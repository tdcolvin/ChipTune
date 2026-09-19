package com.example.chiptune.ui.screens.sine

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

class SineViewModel : ViewModel() {

    private val sampleRate = 44100

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _frequency = MutableStateFlow(440f) // Default 440 Hz (Concert A)
    val frequency: StateFlow<Float> = _frequency.asStateFlow()

    private val _amplitude = MutableStateFlow(0.5f) // Default 50% amplitude
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveform = MutableStateFlow(FloatArray(1024))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null

    @Volatile
    private var currentFrequency = 440f

    @Volatile
    private var currentAmplitude = 0.5f

    init {
        generatePreviewWaveform()
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
            val bufferChunkSize = track.bufferSizeInFrames
            val floatBuffer = FloatArray(bufferChunkSize)

            // 'n' tracks the absolute sample index since playback started
            var n = 0L

            while (_isPlaying.value && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val f = currentFrequency
                val A = currentAmplitude
                val fs = sampleRate

                for (i in 0 until bufferChunkSize) {
                    val x_n = A * sin(2.0 * Math.PI * f * (n / fs))
                    floatBuffer[i] = x_n.toFloat()
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
        val bufferChunkSize = 1024
        val shortBuffer = FloatArray(bufferChunkSize)
        val twoPi = 2.0 * Math.PI

        for (i in 0 until bufferChunkSize) {
            val phase = (twoPi * currentFrequency * i / sampleRate) % twoPi
            shortBuffer[i] = sin(phase.toFloat()) * currentAmplitude
        }
        _waveform.value = shortBuffer
    }

    override fun onCleared() {
        stop()
    }
}
