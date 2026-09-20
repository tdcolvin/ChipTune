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
        formulaTitle = "Square Wave Formula",
        formulaExpression = "y(t) = if ((t × f) mod 1.0 < d) A else -A",
        description = "Classic 8-bit retro gaming wave. Duty cycle controls pulse width."
    ),
    SAWTOOTH(
        displayName = "Sawtooth",
        formulaTitle = "Sawtooth Wave Formula",
        formulaExpression = "y(t) = A × (2.0 × ((t × f) mod 1.0) - 1.0)",
        description = "Bright, buzzy tone rich in both even and odd harmonics."
    )
}

class WavesViewModel : ViewModel() {

    private val sampleRate = 44100

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedWaveform = MutableStateFlow(WaveformType.SINE)
    val selectedWaveform: StateFlow<WaveformType> = _selectedWaveform.asStateFlow()

    private val _frequency = MutableStateFlow(440f) // Default 440 Hz
    val frequency: StateFlow<Float> = _frequency.asStateFlow()

    private val _amplitude = MutableStateFlow(0.5f) // Default 50%
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _dutyCycle = MutableStateFlow(0.5f) // Default 50% duty cycle
    val dutyCycle: StateFlow<Float> = _dutyCycle.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(1024))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null

    @Volatile
    private var currentWaveform = WaveformType.SINE

    @Volatile
    private var currentFrequency = 440f

    @Volatile
    private var currentAmplitude = 0.5f

    @Volatile
    private var currentDutyCycle = 0.5f

    init {
        generatePreviewWaveform()
    }

    fun setWaveform(waveform: WaveformType) {
        _selectedWaveform.value = waveform
        currentWaveform = waveform
        if (!_isPlaying.value) {
            generatePreviewWaveform()
        }
    }

    fun setFrequency(freq: Float) {
        _frequency.value = freq
        currentFrequency = freq
        if (!_isPlaying.value) {
            generatePreviewWaveform()
        }
    }

    fun setAmplitude(amp: Float) {
        _amplitude.value = amp
        currentAmplitude = amp
        if (!_isPlaying.value) {
            generatePreviewWaveform()
        }
    }

    fun setDutyCycle(duty: Float) {
        _dutyCycle.value = duty
        currentDutyCycle = duty
        if (!_isPlaying.value) {
            generatePreviewWaveform()
        }
    }

    fun togglePlay() {
        if (_isPlaying.value) {
            stop()
        } else {
            start()
        }
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true

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

            while (_isPlaying.value && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val wave = currentWaveform
                val f = currentFrequency
                val A = currentAmplitude
                val d = currentDutyCycle
                val fs = sampleRate.toDouble()
                val twoPi = 2.0 * Math.PI

                for (i in 0 until bufferChunkSize) {
                    val t = n / fs

                    val sampleValue: Double = when (wave) {
                        WaveformType.SINE -> {
                            A * sin(twoPi * f * t)
                        }
                        WaveformType.SQUARE -> {
                            val phase = (t * f) % 1.0
                            if (phase < d) A.toDouble() else -A.toDouble()
                        }
                        WaveformType.SAWTOOTH -> {
                            val phase = (t * f) % 1.0
                            A * (2.0 * phase - 1.0)
                        }
                    }

                    floatBuffer[i] = sampleValue.toFloat()
                    n++
                }

                _waveform.value = floatBuffer.copyOf()
                track.write(floatBuffer, 0, bufferChunkSize, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
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
        generatePreviewWaveform()
    }

    private fun generatePreviewWaveform() {
        val bufferSize = 1024
        val previewBuffer = FloatArray(bufferSize)
        val f = currentFrequency
        val A = currentAmplitude
        val d = currentDutyCycle
        val twoPi = 2.0 * Math.PI
        val wave = currentWaveform

        for (i in 0 until bufferSize) {
            val t = i / sampleRate.toDouble()
            val sampleValue = when (wave) {
                WaveformType.SINE -> {
                    A * sin(twoPi * f * t)
                }
                WaveformType.SQUARE -> {
                    val phase = (t * f) % 1.0
                    if (phase < d) A.toDouble() else -A.toDouble()
                }
                WaveformType.SAWTOOTH -> {
                    val phase = (t * f) % 1.0
                    A * (2.0 * phase - 1.0)
                }
            }
            previewBuffer[i] = sampleValue.toFloat()
        }
        _waveform.value = previewBuffer
    }

    override fun onCleared() {
        stop()
    }
}
