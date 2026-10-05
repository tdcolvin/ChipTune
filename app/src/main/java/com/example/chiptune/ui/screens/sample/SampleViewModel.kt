package com.example.chiptune.ui.screens.sample

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.NOTE_C4
import com.example.chiptune.NOTE_E4
import com.example.chiptune.NOTE_G4
import com.example.chiptune.R
import com.example.chiptune.ui.components.OCTAVE_NOTES
import com.example.chiptune.ui.components.PianoNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

class SampleViewModel(application: Application) : AndroidViewModel(application) {

    private class SampleVoice(
        val note: PianoNote,
        val speed: Float,
        var position: Double = 0.0,
        var isReleased: Boolean = false,
        var releaseGain: Float = 1.0f
    )

    private val sampleRate = 44100
    private val baseFreq = NOTE_C4 // 261.63 Hz (Middle C)

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    private val _activeNote = MutableStateFlow<PianoNote?>(null)
    val activeNote: StateFlow<PianoNote?> = _activeNote.asStateFlow()

    private val _selectedOctaveOffset = MutableStateFlow(0) // -1 (Octave 3), 0 (Octave 4), +1 (Octave 5)
    val selectedOctaveOffset: StateFlow<Int> = _selectedOctaveOffset.asStateFlow()

    private val _currentNotesList = MutableStateFlow<List<PianoNote>>(OCTAVE_NOTES)
    val currentNotesList: StateFlow<List<PianoNote>> = _currentNotesList.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(1024))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private val _isPlayingDemo = MutableStateFlow(false)
    val isPlayingDemo: StateFlow<Boolean> = _isPlayingDemo.asStateFlow()

    private val activeKeysHeld = mutableListOf<PianoNote>()
    private val playingVoices = ConcurrentLinkedQueue<SampleVoice>()

    private var pianoSamples: FloatArray = FloatArray(0)

    @Volatile
    private var isEngineRunning = true

    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null
    private var demoJob: Job? = null

    init {
        loadPianoSamples()
        updateNotesList()
        startAudioEngine()
    }

    private fun loadPianoSamples() {
        try {
            val inputStream = getApplication<Application>().resources.openRawResource(R.raw.piano)
            val bytes = inputStream.use { it.readBytes() }

            if (bytes.size >= 4 && bytes.size % 4 == 0) {
                val floatBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                val floats = FloatArray(floatBuffer.remaining())
                floatBuffer.get(floats)

                val hasNaNs = floats.any { it.isNaN() || it.isInfinite() }
                val maxVal = floats.maxOfOrNull { abs(it) } ?: 0f

                if (!hasNaNs && maxVal > 0f && maxVal <= 2.0f) {
                    pianoSamples = floats
                    _sampleCount.value = floats.size
                    return
                }
            }

            val floats = FloatArray(bytes.size)
            for (i in bytes.indices) {
                floats[i] = bytes[i].toFloat() / 128.0f
            }
            pianoSamples = floats
            _sampleCount.value = floats.size
        } catch (_: Exception) {
            pianoSamples = FloatArray(0)
            _sampleCount.value = 0
        }
    }

    fun setOctaveOffset(offset: Int) {
        if (_selectedOctaveOffset.value == offset) return
        _selectedOctaveOffset.value = offset
        updateNotesList()
    }

    private fun updateNotesList() {
        val multiplier = when (_selectedOctaveOffset.value) {
            -1 -> 0.5f // Octave 3 (C3..C4)
            1 -> 2.0f  // Octave 5 (C5..C6)
            else -> 1.0f // Octave 4 (C4..C5)
        }

        val octaveLabel = when (_selectedOctaveOffset.value) {
            -1 -> 3
            1 -> 5
            else -> 4
        }

        _currentNotesList.value = OCTAVE_NOTES.map { note ->
            val newNoteName = note.name.replace("4", octaveLabel.toString()).replace("5", (octaveLabel + 1).toString())
            PianoNote(
                name = newNoteName,
                frequency = note.frequency * multiplier,
                isBlack = note.isBlack
            )
        }
    }

    private fun startAudioEngine() {
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

        audioTrack = track
        track.play()

        audioJob = viewModelScope.launch(Dispatchers.Default) {
            val bufferChunkSize = track.bufferSizeInFrames.coerceAtLeast(512)
            val floatBuffer = FloatArray(bufferChunkSize)

            while (isEngineRunning && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                floatBuffer.fill(0f)

                if (playingVoices.isNotEmpty() && pianoSamples.isNotEmpty()) {
                    val samples = pianoSamples
                    val finishedVoices = mutableListOf<SampleVoice>()

                    for (voice in playingVoices) {
                        var pos = voice.position
                        val speed = voice.speed.toDouble()

                        for (i in 0 until bufferChunkSize) {
                            if (voice.isReleased) {
                                voice.releaseGain -= 0.003f
                                if (voice.releaseGain <= 0f) {
                                    finishedVoices.add(voice)
                                    break
                                }
                            }

                            val idx0 = pos.toInt()
                            if (idx0 >= samples.size - 1) {
                                finishedVoices.add(voice)
                                break
                            }

                            val idx1 = idx0 + 1
                            val frac = (pos - idx0).toFloat()
                            val s0 = samples[idx0]
                            val s1 = if (idx1 < samples.size) samples[idx1] else 0f
                            val interpolatedSample = s0 + frac * (s1 - s0)

                            val gain = 0.6f * voice.releaseGain
                            floatBuffer[i] += interpolatedSample * gain
                            pos += speed
                        }

                        voice.position = pos
                    }

                    if (finishedVoices.isNotEmpty()) {
                        playingVoices.removeAll(finishedVoices.toSet())
                    }

                    for (i in 0 until bufferChunkSize) {
                        floatBuffer[i] = floatBuffer[i].coerceIn(-1.0f, 1.0f)
                    }

                    val waveCopy = floatBuffer.copyOf()
                    _waveform.value = waveCopy
                } else {
                    _waveform.value = floatBuffer.copyOf()
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
        if (_isPlayingDemo.value) {
            stopDemo()
        }
        synchronized(activeKeysHeld) {
            activeKeysHeld.remove(note)
            activeKeysHeld.add(note)
            _activeNote.value = note
        }
        val speed = note.frequency / baseFreq
        playingVoices.add(SampleVoice(note = note, speed = speed))
    }

    fun stopNote(note: PianoNote) {
        synchronized(activeKeysHeld) {
            activeKeysHeld.remove(note)
            _activeNote.value = activeKeysHeld.lastOrNull()
        }
        for (voice in playingVoices) {
            if (voice.note == note) {
                voice.isReleased = true
            }
        }
    }

    fun toggleDemo() {
        if (_isPlayingDemo.value) {
            stopDemo()
        } else {
            playDemo()
        }
    }

    private fun playDemo() {
        if (_isPlayingDemo.value) return
        _isPlayingDemo.value = true

        val notesList = currentNotesList.value
        val cNote = notesList.firstOrNull { it.name.startsWith("C") && !it.isBlack } ?: notesList[0]
        val eNote = notesList.firstOrNull { it.name.startsWith("E") && !it.isBlack } ?: notesList[2]
        val gNote = notesList.firstOrNull { it.name.startsWith("G") && !it.isBlack } ?: notesList[4]
        val bNote = notesList.firstOrNull { it.name.startsWith("B") && !it.isBlack } ?: notesList[6]
        val c5Note = notesList.lastOrNull { !it.isBlack } ?: notesList.last()

        val arpeggio = listOf(cNote, eNote, gNote, bNote, c5Note, bNote, gNote, eNote)

        demoJob = viewModelScope.launch {
            for (note in arpeggio) {
                if (!_isPlayingDemo.value) break
                playNote(note)
                delay(300)
                stopNote(note)
                delay(50)
            }
            _isPlayingDemo.value = false
        }
    }

    private fun stopDemo() {
        _isPlayingDemo.value = false
        demoJob?.cancel()
        demoJob = null
        playingVoices.forEach { it.isReleased = true }
    }



    override fun onCleared() {
        isEngineRunning = false
        demoJob?.cancel()
        audioJob?.cancel()
        audioJob = null

        val trackToRelease = audioTrack
        audioTrack = null
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
    }
}
