package com.example.chiptune.ui.screens.waves

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class WaveformType(
    val displayName: String,
    val formulaTitle: String,
    val formulaExpression: String,
    val description: String
) {
    SINE(
        displayName = "Sine",
        formulaTitle = "Sine Wave Formula",
        formulaExpression = "y(t) = A × sin(2π × f × t)",
        description = "Pure fundamental frequency without overtones or harmonics."
    ),
    SQUARE(
        displayName = "Square",
        formulaTitle = "Square Wave Formula (50% Duty Cycle)",
        formulaExpression = "y(t) = if ((t × f) mod 1.0 < 0.5) A else -A",
        description = "Classic 8-bit retro gaming wave containing odd harmonics."
    ),
    SAWTOOTH(
        displayName = "Sawtooth",
        formulaTitle = "Sawtooth Wave Formula",
        formulaExpression = "y(t) = A × (2.0 × ((t × f) mod 1.0) - 1.0)",
        description = "Bright, buzzy tone rich in both even and odd harmonics."
    )
}

data class PianoNote(
    val name: String,
    val frequency: Float,
    val isBlack: Boolean
)

// 1-Octave scale from C4 (261.63 Hz) to C5 (523.25 Hz)
val OCTAVE_NOTES = listOf(
    PianoNote("C4", 261.63f, isBlack = false),
    PianoNote("C#4", 277.18f, isBlack = true),
    PianoNote("D4", 293.66f, isBlack = false),
    PianoNote("D#4", 311.13f, isBlack = true),
    PianoNote("E4", 329.63f, isBlack = false),
    PianoNote("F4", 349.23f, isBlack = false),
    PianoNote("F#4", 369.99f, isBlack = true),
    PianoNote("G4", 392.00f, isBlack = false),
    PianoNote("G#4", 415.30f, isBlack = true),
    PianoNote("A4", 440.00f, isBlack = false),
    PianoNote("A#4", 466.16f, isBlack = true),
    PianoNote("B4", 493.88f, isBlack = false),
    PianoNote("C5", 523.25f, isBlack = false)
)

class WavesViewModel : ViewModel() {

    private val sampleRate = 44100

    private val _selectedWaveform = MutableStateFlow(WaveformType.SINE)
    val selectedWaveform: StateFlow<WaveformType> = _selectedWaveform.asStateFlow()

    private val _activeNote = MutableStateFlow<PianoNote?>(null)
    val activeNote: StateFlow<PianoNote?> = _activeNote.asStateFlow()

    private val _amplitude = MutableStateFlow(0.5f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(1024))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    // Single-note priority stack for monophonic playback (no chords allowed)
    private val activeNotesStack = mutableListOf<PianoNote>()

    @Volatile
    private var currentWaveform = WaveformType.SINE

    @Volatile
    private var currentFrequency = 0f

    @Volatile
    private var targetAmplitude = 0.5f

    @Volatile
    private var isEngineRunning = true

    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null

    init {
        startAudioEngine()
        generatePreviewWaveform()
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

            var n = 0L // Absolute sample counter
            var renderedGain = 0f

            while (isEngineRunning && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val wave = currentWaveform
                val freq = currentFrequency
                val baseAmp = targetAmplitude
                val fs = sampleRate.toDouble()
                val twoPi = 2.0 * Math.PI

                // Smoothly fade gain to avoid click pops on note trigger/release
                val goalGain = if (freq > 0f) baseAmp else 0f

                for (i in 0 until bufferChunkSize) {
                    if (renderedGain < goalGain) {
                        renderedGain = (renderedGain + 0.002f).coerceAtMost(goalGain)
                    } else if (renderedGain > goalGain) {
                        renderedGain = (renderedGain - 0.002f).coerceAtLeast(goalGain)
                    }

                    if (renderedGain > 0f && freq > 0f) {
                        val t = n / fs // Elapsed time in seconds

                        // Monophonic Waveform Formulae:
                        val sampleValue: Double = when (wave) {
                            WaveformType.SINE -> {
                                // Sine Wave Formula: y(t) = A * sin(2 * π * f * t)
                                renderedGain * sin(twoPi * freq * t)
                            }

                            WaveformType.SQUARE -> {
                                // Square Wave Formula (50% duty cycle):
                                // phase = (t * f) mod 1.0
                                // y(t) = if (phase < 0.5) A else -A
                                val phase = (t * freq) % 1.0
                                if (phase < 0.5) renderedGain.toDouble() else -renderedGain.toDouble()
                            }

                            WaveformType.SAWTOOTH -> {
                                // Sawtooth Wave Formula:
                                // phase = (t * f) mod 1.0
                                // y(t) = A * (2.0 * phase - 1.0)
                                val phase = (t * freq) % 1.0
                                renderedGain * (2.0 * phase - 1.0)
                            }
                        }

                        floatBuffer[i] = sampleValue.toFloat()
                    } else {
                        floatBuffer[i] = 0f
                    }

                    n++
                }

                if (renderedGain > 0f) {
                    _waveform.value = floatBuffer.copyOf()
                }

                track.write(floatBuffer, 0, bufferChunkSize, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    /**
     * Triggered when a key on the keyboard is held down.
     * Monophonic synth rule: replaces current note with this new single note.
     */
    fun playNote(note: PianoNote) {
        synchronized(activeNotesStack) {
            activeNotesStack.remove(note)
            activeNotesStack.add(note)
            updateActiveNote()
        }
    }

    /**
     * Triggered when a key on the keyboard is released.
     */
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
        if (topNote == null) {
            generatePreviewWaveform()
        }
    }

    fun setWaveform(waveform: WaveformType) {
        _selectedWaveform.value = waveform
        currentWaveform = waveform
        if (_activeNote.value == null) {
            generatePreviewWaveform()
        }
    }

    fun setAmplitude(amp: Float) {
        _amplitude.value = amp
        targetAmplitude = amp
        if (_activeNote.value == null) {
            generatePreviewWaveform()
        }
    }

    private fun generatePreviewWaveform() {
        val bufferSize = 1024
        val previewBuffer = FloatArray(bufferSize)
        val refFreq = 440f // A4 reference note for idle visualization
        val amp = _amplitude.value
        val twoPi = 2.0 * Math.PI
        val wave = currentWaveform

        for (i in 0 until bufferSize) {
            val t = i / sampleRate.toDouble()
            val sampleValue = when (wave) {
                WaveformType.SINE -> {
                    amp * sin(twoPi * refFreq * t)
                }
                WaveformType.SQUARE -> {
                    val phase = (t * refFreq) % 1.0
                    if (phase < 0.5) amp.toDouble() else -amp.toDouble()
                }
                WaveformType.SAWTOOTH -> {
                    val phase = (t * refFreq) % 1.0
                    amp * (2.0 * phase - 1.0)
                }
            }
            previewBuffer[i] = sampleValue.toFloat()
        }
        _waveform.value = previewBuffer
    }

    override fun onCleared() {
        isEngineRunning = false
        audioJob?.cancel()
        audioJob = null

        val trackToRelease = audioTrack
        audioTrack = null
        trackToRelease?.let { track ->
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
            } catch (_: Exception) {
            } finally {
                track.release()
            }
        }
    }
}
