package com.example.chiptune.ui.screens.explosion

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chiptune.ExplosionChannel
import com.example.chiptune.PercussionBeatChannel
import com.example.chiptune.PercussionSound
import com.example.chiptune.WaveformData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ExplosionPreset(
    val title: String,
    val cutoffHz: Float,
    val dynamicSweep: Boolean,
    val decaySec: Float,
    val filterEnabled: Boolean,
    val description: String,
) {
    SPACE_INVADERS(
        title = "Space Invaders",
        cutoffHz = 280f,
        dynamicSweep = true,
        decaySec = 1.2f,
        filterEnabled = true,
        description = "Iconic ship destruction sound with dynamic sweep down to low bass rumble.",
    ),
    ASTEROIDS(
        title = "Asteroids Boom",
        cutoffHz = 150f,
        dynamicSweep = true,
        decaySec = 1.5f,
        filterEnabled = true,
        description = "Deep sub-bass explosion rumble from blowing up large asteroids.",
    ),
    RAW_HISS(
        title = "Raw Noise Hiss",
        cutoffHz = 8000f,
        dynamicSweep = false,
        decaySec = 1.0f,
        filterEnabled = false,
        description = "Unfiltered white noise static/hiss without low-pass filtering.",
    ),
    CANNON_SHOT(
        title = "Short Cannon",
        cutoffHz = 500f,
        dynamicSweep = false,
        decaySec = 0.35f,
        filterEnabled = true,
        description = "Punchy, short explosion blast.",
    )
}

class ExplosionViewModel : ViewModel() {

    private val sampleRate = 44100

    val explosionChannel = ExplosionChannel()
    val percussionChannel = PercussionBeatChannel()

    private val _filterEnabled = MutableStateFlow(value = true)
    val filterEnabled: StateFlow<Boolean> = _filterEnabled.asStateFlow()

    private val _cutoffFrequency = MutableStateFlow(300f)
    val cutoffFrequency: StateFlow<Float> = _cutoffFrequency.asStateFlow()

    private val _dynamicSweep = MutableStateFlow(value = true)
    val dynamicSweep: StateFlow<Boolean> = _dynamicSweep.asStateFlow()

    private val _decayDuration = MutableStateFlow(1.2f)
    val decayDuration: StateFlow<Float> = _decayDuration.asStateFlow()

    private val _loopExplosion = MutableStateFlow(value = false)
    val loopExplosion: StateFlow<Boolean> = _loopExplosion.asStateFlow()

    private val _isPlayingPercussion = MutableStateFlow(value = false)
    val isPlayingPercussion: StateFlow<Boolean> = _isPlayingPercussion.asStateFlow()

    private val _percussionSound = MutableStateFlow(PercussionSound.HIHAT_PATTERN)
    val percussionSound: StateFlow<PercussionSound> = _percussionSound.asStateFlow()

    private val _percussionBpm = MutableStateFlow(120.0)
    val percussionBpm: StateFlow<Double> = _percussionBpm.asStateFlow()

    private val _currentPercussionStep = MutableStateFlow(0)
    val currentPercussionStep: StateFlow<Int> = _currentPercussionStep.asStateFlow()

    private val _isExplosionMuted = MutableStateFlow(value = false)
    val isExplosionMuted: StateFlow<Boolean> = _isExplosionMuted.asStateFlow()

    private val _isPercussionMuted = MutableStateFlow(value = false)
    val isPercussionMuted: StateFlow<Boolean> = _isPercussionMuted.asStateFlow()

    private val _waveformData = MutableStateFlow(WaveformData())
    val waveformData: StateFlow<WaveformData> = _waveformData.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null
    private var masterSampleIndex = 0L

    init {
        startAudioTrack()
    }

    private fun startAudioTrack() {
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
            val floatBufferExplosion = FloatArray(bufferChunkSize)
            val floatBufferPercussion = FloatArray(bufferChunkSize)
            val masterBuffer = FloatArray(bufferChunkSize)

            while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                floatBufferExplosion.fill(0f)
                floatBufferPercussion.fill(0f)
                masterBuffer.fill(0f)

                // Render Explosion Channel
                explosionChannel.renderBlock(
                    floatBufferExplosion,
                    masterSampleIndex,
                    bufferChunkSize,
                    sampleRate
                )

                // Render Percussion Channel
                percussionChannel.renderBlock(
                    floatBufferPercussion,
                    masterSampleIndex,
                    bufferChunkSize,
                    sampleRate
                )

                _currentPercussionStep.value = percussionChannel.currentStep

                // Store channel waveforms for individual visualisers
                val waveMap = mapOf(
                    "Explosion" to floatBufferExplosion.copyOf(),
                    "Percussion" to floatBufferPercussion.copyOf()
                )

                // Mix into master output
                for (i in 0 until bufferChunkSize) {
                    val mixed = (floatBufferExplosion[i] * 0.7f) + (floatBufferPercussion[i] * 0.5f)
                    masterBuffer[i] = mixed.coerceIn(-1.0f, 1.0f)
                }

                _waveformData.value = WaveformData(
                    mixed = masterBuffer.copyOf(),
                    channelWaveforms = waveMap
                )

                track.write(masterBuffer, 0, bufferChunkSize, AudioTrack.WRITE_BLOCKING)
                masterSampleIndex += bufferChunkSize
            }
        }
    }

    fun triggerExplosion() {
        explosionChannel.trigger(masterSampleIndex)
    }

    fun setFilterEnabled(enabled: Boolean) {
        _filterEnabled.value = enabled
        explosionChannel.filterEnabled = enabled
    }

    fun setCutoffFrequency(freq: Float) {
        _cutoffFrequency.value = freq
        explosionChannel.cutoffFrequency = freq
    }

    fun setDynamicSweep(sweep: Boolean) {
        _dynamicSweep.value = sweep
        explosionChannel.dynamicSweep = sweep
    }

    fun setDecayDuration(duration: Float) {
        _decayDuration.value = duration
        explosionChannel.decayDuration = duration
    }

    fun setLoopExplosion(loop: Boolean) {
        _loopExplosion.value = loop
        explosionChannel.loop = loop
        if (loop) {
            explosionChannel.trigger(masterSampleIndex)
        } else {
            explosionChannel.stop()
        }
    }

    fun applyPreset(preset: ExplosionPreset) {
        setFilterEnabled(preset.filterEnabled)
        setCutoffFrequency(preset.cutoffHz)
        setDynamicSweep(preset.dynamicSweep)
        setDecayDuration(preset.decaySec)
        triggerExplosion()
    }

    fun togglePercussionPlay() {
        val playing = !_isPlayingPercussion.value
        _isPlayingPercussion.value = playing
        percussionChannel.isPlaying = playing
    }

    fun setPercussionSound(sound: PercussionSound) {
        _percussionSound.value = sound
        percussionChannel.selectedSound = sound
    }

    fun setPercussionBpm(bpm: Double) {
        _percussionBpm.value = bpm
        percussionChannel.bpm = bpm
    }

    fun toggleExplosionMute() {
        val muted = !_isExplosionMuted.value
        _isExplosionMuted.value = muted
        explosionChannel.isMuted = muted
    }

    fun togglePercussionMute() {
        val muted = !_isPercussionMuted.value
        _isPercussionMuted.value = muted
        percussionChannel.isMuted = muted
    }

    override fun onCleared() {
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
