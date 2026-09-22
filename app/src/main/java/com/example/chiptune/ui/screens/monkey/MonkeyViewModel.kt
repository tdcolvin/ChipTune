package com.example.chiptune.ui.screens.monkey

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.NOTE_A5
import com.example.chiptune.NOTE_B4
import com.example.chiptune.NOTE_C5
import com.example.chiptune.NOTE_D5
import com.example.chiptune.NOTE_E5
import com.example.chiptune.NOTE_FS5
import com.example.chiptune.NOTE_G5
import com.example.chiptune.Note
import com.example.chiptune.REST
import com.example.chiptune.SynthType
import com.example.chiptune.WaveformData
import com.example.chiptune.ui.components.PianoNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.exp
import kotlin.math.sin

class MonkeyViewModel : ViewModel() {

    private class ActiveVoice(
        val freq: Float,
        var sampleCounter: Long = 0L
    )

    private val synth = ChiptuneSynthesizer()

    val monkeyIslandTheme = listOf(
        Note(NOTE_E5, 0.5f),
        Note(NOTE_E5, 0.25f),
        Note(NOTE_G5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_E5, 0.25f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_E5, 1.0f),

        Note(REST, 0.5f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_D5, 0.25f),
        Note(NOTE_C5, 0.25f),
        Note(NOTE_B4, 0.25f),
        Note(NOTE_D5, 0.25f),
        Note(NOTE_C5, 0.5f, stacatto = true),
        Note(NOTE_C5, 0.5f, stacatto = true),

        Note(NOTE_B4, 1f),
        Note(REST, 0.5f),
        Note(NOTE_E5, 0.5f, stacatto = true),
        Note(NOTE_E5, 0.75f),
        Note(NOTE_G5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_E5, 0.25f),
        Note(NOTE_D5, 0.5f),

        Note(NOTE_E5, 1.0f),
        Note(REST, 0.5f),
        Note(REST, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_G5, 0.5f, stacatto = true),
        Note(NOTE_G5, 0.5f, stacatto = true),
        Note(NOTE_A5, 1f),

        Note(NOTE_FS5, 0.75f),
        Note(NOTE_G5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_E5, 0.25f),
        Note(NOTE_D5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_G5, 0.5f, stacatto = true),
        Note(NOTE_G5, 0.5f, stacatto = true),
        Note(NOTE_FS5, 1f),

        Note(NOTE_E5, 0.75f),
        Note(NOTE_G5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_E5, 0.25f),
        Note(NOTE_D5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_G5, 0.5f, stacatto = true),
        Note(NOTE_G5, 0.5f, stacatto = true),
        Note(NOTE_FS5, 1f),

        Note(NOTE_E5, 0.75f),
        Note(NOTE_G5, 0.25f),
        Note(NOTE_FS5, 0.25f),
        Note(NOTE_E5, 0.25f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_E5, 0.5f, stacatto = true),
        Note(NOTE_E5, 0.5f, stacatto = true),
        Note(NOTE_E5, 1f),

        Note(REST, 0.5f),
        Note(NOTE_E5, 0.5f),
        Note(NOTE_D5, 0.25f),
        Note(NOTE_C5, 0.25f),
        Note(NOTE_B4, 0.25f),
        Note(NOTE_D5, 0.25f),
        Note(NOTE_C5, 0.5f, stacatto = true),
        Note(NOTE_C5, 0.5f),

        Note(NOTE_B4, 1f),
        Note(REST, 2f),
    )

    private val sampleRate = 44100

    private val _isPlayingMelody = MutableStateFlow(false)
    val isPlayingMelody: StateFlow<Boolean> = _isPlayingMelody.asStateFlow()

    private val _selectedSynthType = MutableStateFlow(SynthType.Opl2)
    val selectedSynthType: StateFlow<SynthType> = _selectedSynthType.asStateFlow()

    private val _activeNote = MutableStateFlow<PianoNote?>(null)
    val activeNote: StateFlow<PianoNote?> = _activeNote.asStateFlow()

    private val _isVoicesPlaying = MutableStateFlow(false)
    val isVoicesPlaying: StateFlow<Boolean> = _isVoicesPlaying.asStateFlow()

    private val _liveWaveform = MutableStateFlow(FloatArray(1024))

    private val _waveformData = MutableStateFlow(WaveformData())
    val waveformData: StateFlow<WaveformData> = _waveformData.asStateFlow()

    private val activeKeysHeld = Collections.synchronizedList(mutableListOf<PianoNote>())
    private val playingVoices = ConcurrentLinkedQueue<ActiveVoice>()

    @Volatile
    private var isLiveEngineRunning = true

    private var liveAudioTrack: AudioTrack? = null
    private var liveAudioJob: Job? = null

    init {
        viewModelScope.launch {
            synth.waveformData.collect { synthWave ->
                if (_isPlayingMelody.value) {
                    _waveformData.value = synthWave
                }
            }
        }
        startLiveAudioEngine()
        generatePreviewWaveform()
    }

    fun setSynthType(synthType: SynthType) {
        if (_selectedSynthType.value == synthType) return
        _selectedSynthType.value = synthType
        if (_isPlayingMelody.value) {
            stopMelody()
            playMelody()
        } else {
            generatePreviewWaveform()
        }
    }

    private fun startLiveAudioEngine() {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        liveAudioTrack = track
        track.play()

        liveAudioJob = viewModelScope.launch(Dispatchers.Default) {
            val bufferChunkSize = track.bufferSizeInFrames.coerceAtLeast(512)
            val floatBuffer = FloatArray(bufferChunkSize)
            val twoPi = 2.0 * Math.PI
            var wasVoicesPlaying = false

            while (isLiveEngineRunning && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                floatBuffer.fill(0f)

                if (!_isPlayingMelody.value && playingVoices.isNotEmpty()) {
                    wasVoicesPlaying = true
                    val currentSynth = _selectedSynthType.value
                    val finishedVoices = mutableListOf<ActiveVoice>()

                    for (voice in playingVoices) {
                        val freq = voice.freq
                        var sc = voice.sampleCounter

                        for (i in 0 until bufferChunkSize) {
                            val sampleValue = when (currentSynth) {
                                SynthType.Opl2 -> {
                                    val incCarrier = twoPi * freq / sampleRate
                                    val incModulator = twoPi * (freq * 3.5) / sampleRate

                                    val oplEnvelope = exp(-0.000080003 * sc)
                                    if (oplEnvelope < 0.0005) {
                                        finishedVoices.add(voice)
                                        break
                                    }
                                    val modIndex = 2.5 * oplEnvelope

                                    val phaseModulator = (incModulator * sc) % twoPi
                                    val phaseCarrier = (incCarrier * sc) % twoPi

                                    val modOut = if (phaseModulator < Math.PI) sin(phaseModulator) else 0.0
                                    val finalModOut = modOut * modIndex

                                    val carrierOut = sin(phaseCarrier + finalModOut) * oplEnvelope
                                    carrierOut * 0.35
                                }
                                SynthType.Fm2op -> {
                                    val oplEnvelope = exp(-0.000080003 * sc)
                                    if (oplEnvelope < 0.0005) {
                                        finishedVoices.add(voice)
                                        break
                                    }
                                    val t = sc / sampleRate.toDouble()
                                    val modulatorFreq = freq * 2.0
                                    val modulationIndex = 2.2 * oplEnvelope
                                    val modulator = sin(twoPi * modulatorFreq * t)
                                    val carrierOut = sin(twoPi * freq * t + (modulator * modulationIndex)) * oplEnvelope
                                    carrierOut * 0.35
                                }
                                else -> 0.0
                            }

                            floatBuffer[i] += sampleValue.toFloat()
                            sc++
                        }
                        voice.sampleCounter = sc
                    }

                    if (finishedVoices.isNotEmpty()) {
                        playingVoices.removeAll(finishedVoices.toSet())
                    }

                    if (playingVoices.isEmpty()) {
                        _isVoicesPlaying.value = false
                    }

                    for (i in 0 until bufferChunkSize) {
                        floatBuffer[i] = floatBuffer[i].coerceIn(-1.0f, 1.0f)
                    }

                    val waveCopy = floatBuffer.copyOf()
                    _liveWaveform.value = waveCopy
                    _waveformData.value = WaveformData(mixed = waveCopy)
                } else if (!_isPlayingMelody.value) {
                    if (wasVoicesPlaying) {
                        wasVoicesPlaying = false
                        _isVoicesPlaying.value = false
                        generatePreviewWaveform()
                    }
                }

                try {
                    track.write(floatBuffer, 0, bufferChunkSize, AudioTrack.WRITE_BLOCKING)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun playNote(note: PianoNote) {
        if (_isPlayingMelody.value) {
            stopMelody()
        }
        synchronized(activeKeysHeld) {
            activeKeysHeld.remove(note)
            activeKeysHeld.add(note)
            _activeNote.value = note
        }
        playingVoices.add(ActiveVoice(freq = note.frequency))
        _isVoicesPlaying.value = true
    }

    fun stopNote(note: PianoNote) {
        synchronized(activeKeysHeld) {
            activeKeysHeld.remove(note)
            _activeNote.value = activeKeysHeld.lastOrNull()
        }
    }

    fun togglePlayMelody() {
        if (_isPlayingMelody.value) {
            stopMelody()
        } else {
            playMelody()
        }
    }

    fun playMelody() {
        if (_isPlayingMelody.value) return

        playingVoices.clear()
        _isVoicesPlaying.value = false
        synchronized(activeKeysHeld) {
            activeKeysHeld.clear()
            _activeNote.value = null
        }

        _isPlayingMelody.value = true

        synth.playOnce(
            sequence = monkeyIslandTheme,
            synthType = _selectedSynthType.value,
            bpm = 100.0,
            onComplete = {
                _isPlayingMelody.value = false
                generatePreviewWaveform()
            }
        )
    }

    fun stopMelody() {
        synth.stop()
        _isPlayingMelody.value = false
        generatePreviewWaveform()
    }

    private fun generatePreviewWaveform() {
        val bufferSize = 1024
        val previewBuffer = FloatArray(bufferSize)
        val refFreq = 440f
        val twoPi = 2.0 * Math.PI
        val currentSynth = _selectedSynthType.value

        for (i in 0 until bufferSize) {
            val sampleValue = when (currentSynth) {
                SynthType.Opl2 -> {
                    val incCarrier = twoPi * refFreq / sampleRate
                    val incModulator = twoPi * (refFreq * 3.5) / sampleRate
                    val oplEnvelope = exp(-0.000080003 * i)
                    val modIndex = 2.5 * oplEnvelope

                    val phaseModulator = (incModulator * i) % twoPi
                    val phaseCarrier = (incCarrier * i) % twoPi

                    val modOut = if (phaseModulator < Math.PI) sin(phaseModulator) else 0.0
                    val finalModOut = modOut * modIndex

                    val carrierOut = sin(phaseCarrier + finalModOut) * oplEnvelope
                    carrierOut * 0.7 * 0.5
                }
                SynthType.Fm2op -> {
                    val t = i / sampleRate.toDouble()
                    val modulatorFreq = refFreq * 2.0
                    val modulationIndex = 2.2
                    val modulator = sin(twoPi * modulatorFreq * t)
                    val carrierOut = sin(twoPi * refFreq * t + (modulator * modulationIndex))
                    carrierOut * 0.5
                }
                else -> 0.0
            }
            previewBuffer[i] = sampleValue.toFloat()
        }
        _liveWaveform.value = previewBuffer
        _waveformData.value = WaveformData(mixed = previewBuffer)
    }

    override fun onCleared() {
        isLiveEngineRunning = false
        liveAudioJob?.cancel()
        liveAudioJob = null

        val trackToRelease = liveAudioTrack
        liveAudioTrack = null
        trackToRelease?.let { track ->
            try {
                if (track.state == AudioTrack.STATE_INITIALIZED && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.release()
                } catch (_: Exception) {
                }
            }
        }
        synth.stop()
    }
}
