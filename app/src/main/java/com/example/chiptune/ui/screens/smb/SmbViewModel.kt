package com.example.chiptune.ui.screens.smb

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.ChiptuneSynthesizer
import com.example.chiptune.NOTE_A4
import com.example.chiptune.NOTE_A5
import com.example.chiptune.NOTE_AS4
import com.example.chiptune.NOTE_B4
import com.example.chiptune.NOTE_C5
import com.example.chiptune.NOTE_D5
import com.example.chiptune.NOTE_E4
import com.example.chiptune.NOTE_E5
import com.example.chiptune.NOTE_F5
import com.example.chiptune.NOTE_G4
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

class SmbViewModel : ViewModel() {

    private val synth = ChiptuneSynthesizer()

    val smbMelody = listOf(
        // Bar 1
        Note(NOTE_E5, 0.5f),
        Note(NOTE_E5, 0.5f),
        Note(REST, 0.5f),
        Note(NOTE_E5, 0.5f),
        Note(REST, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f, stacatto = true),
        Note(NOTE_G5, 1.0f, stacatto = true),
        Note(REST, 1.0f),
        Note(NOTE_G4, 1.0f, stacatto = true),
        Note(REST, 1.0f),

        // Bar 2
        Note(NOTE_C5, 1f, stacatto = true),
        Note(REST, 0.5f),
        Note(NOTE_G4, 1f, stacatto = true),
        Note(REST, 0.5f),
        Note(NOTE_E4, 1f, stacatto = true),
        Note(REST, 0.5f),

        Note(NOTE_A4, 1f, stacatto = true),
        Note(NOTE_B4, 1f, stacatto = true),
        Note(NOTE_AS4, 0.5f),
        Note(NOTE_A4, 1f, stacatto = true),

        Note(NOTE_G4, 2.0f/3.0f, stacatto = true),
        Note(NOTE_E5, 2.0f/3.0f, stacatto = true),
        Note(NOTE_G5, 2.0f/3.0f, stacatto = true),
        Note(NOTE_A5, 1f, stacatto = true),
        Note(NOTE_F5, 0.5f),
        Note(NOTE_G5, 0.5f),

        Note(REST, 0.5f),
        Note(NOTE_E5, 1.0f, stacatto = true),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_B4, 1.0f, stacatto = true),
        Note(REST, 0.5f),

        Note(NOTE_C5, 1f, stacatto = true),
        Note(REST, 0.5f),
        Note(NOTE_G4, 1f, stacatto = true),
        Note(REST, 0.5f),
        Note(NOTE_E4, 1f, stacatto = true),
        Note(REST, 0.5f),

        Note(NOTE_A4, 1f, stacatto = true),
        Note(NOTE_B4, 1f, stacatto = true),
        Note(NOTE_AS4, 0.5f),
        Note(NOTE_A4, 1f, stacatto = true),

        Note(NOTE_G4, 2.0f/3.0f, stacatto = true),
        Note(NOTE_E5, 2.0f/3.0f, stacatto = true),
        Note(NOTE_G5, 2.0f/3.0f, stacatto = true),
        Note(NOTE_A5, 1f, stacatto = true),
        Note(NOTE_F5, 0.5f),
        Note(NOTE_G5, 0.5f),

        Note(REST, 0.5f),
        Note(NOTE_E5, 1.0f, stacatto = true),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_B4, 1.0f, stacatto = true),
        Note(REST, 0.5f),    )

    private val sampleRate = 44100

    private val _isPlayingMelody = MutableStateFlow(false)
    val isPlayingMelody: StateFlow<Boolean> = _isPlayingMelody.asStateFlow()

    private val _activeNote = MutableStateFlow<PianoNote?>(null)
    val activeNote: StateFlow<PianoNote?> = _activeNote.asStateFlow()

    private val _liveWaveform = MutableStateFlow(FloatArray(1024))

    private val _waveformData = MutableStateFlow(WaveformData())
    val waveformData: StateFlow<WaveformData> = _waveformData.asStateFlow()

    private val activeNotesStack = mutableListOf<PianoNote>()

    @Volatile
    private var currentFrequency = 0f

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

            var n = 0L
            var renderedGain = 0f

            while (isLiveEngineRunning && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val freq = currentFrequency
                val baseAmp = 0.5f
                val fs = sampleRate.toDouble()

                val goalGain = if (freq > 0f && !_isPlayingMelody.value) baseAmp else 0f

                for (i in 0 until bufferChunkSize) {
                    if (renderedGain < goalGain) {
                        renderedGain = (renderedGain + 0.002f).coerceAtMost(goalGain)
                    } else if (renderedGain > goalGain) {
                        renderedGain = (renderedGain - 0.002f).coerceAtLeast(goalGain)
                    }

                    if (renderedGain > 0f && freq > 0f) {
                        val t = n / fs
                        val phase = (t * freq) % 1.0
                        val sampleValue = if (phase < 0.5) renderedGain.toDouble() else -renderedGain.toDouble()
                        floatBuffer[i] = sampleValue.toFloat()
                    } else {
                        floatBuffer[i] = 0f
                    }

                    n++
                }

                if (renderedGain > 0f && !_isPlayingMelody.value) {
                    val waveCopy = floatBuffer.copyOf()
                    _liveWaveform.value = waveCopy
                    _waveformData.value = WaveformData(mixed = waveCopy)
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
        synchronized(activeNotesStack) {
            activeNotesStack.remove(note)
            activeNotesStack.add(note)
            updateActiveNote()
        }
    }

    fun stopNote(note: PianoNote) {
        synchronized(activeNotesStack) {
            activeNotesStack.remove(note)
            updateActiveNote()
        }
    }

    private fun updateActiveNote() {
        val topNote = activeNotesStack.lastOrNull()
        _activeNote.value = topNote
        currentFrequency = topNote?.frequency ?: 0f
        if (topNote == null && !_isPlayingMelody.value) {
            generatePreviewWaveform()
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

        synchronized(activeNotesStack) {
            activeNotesStack.clear()
            _activeNote.value = null
            currentFrequency = 0f
        }

        _isPlayingMelody.value = true

        synth.playOnce(
            sequence = smbMelody,
            synthType = SynthType.Square,
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
        val amp = 0.5f

        for (i in 0 until bufferSize) {
            val t = i / sampleRate.toDouble()
            val phase = (t * refFreq) % 1.0
            val sampleValue = if (phase < 0.5) amp else -amp
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
