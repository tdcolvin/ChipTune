package com.example.chiptune

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

data class Note(
    val freq: Float,
    val len: Float,
    val stacatto: Boolean = false
)
const val NOTE_A1 = 55.00f
const val NOTE_AS1 = 58.27f
const val NOTE_B1 = 61.74f

const val NOTE_C2 = 65.41f
const val NOTE_CS2 = 69.30f
const val NOTE_D2 = 73.42f
const val NOTE_DS2 = 77.78f
const val NOTE_E2 = 82.41f
const val NOTE_F2 = 87.31f
const val NOTE_FS2 = 92.50f
const val NOTE_G2 = 98.00f
const val NOTE_GS2 = 103.83f
const val NOTE_A2 = 110.00f
const val NOTE_AS2 = 116.54f
const val NOTE_B2 = 123.47f

const val NOTE_C3 = 130.81f
const val NOTE_CS3 = 138.59f
const val NOTE_D3 = 146.83f
const val NOTE_DS3 = 155.56f
const val NOTE_E3 = 164.81f
const val NOTE_F3 = 174.61f
const val NOTE_FS3 = 185.00f
const val NOTE_G3 = 196.00f
const val NOTE_GS3 = 207.65f
const val NOTE_A3 = 220.00f
const val NOTE_AS3 = 233.08f
const val NOTE_B3 = 246.94f

const val NOTE_C4 = 261.63f // Middle C
const val NOTE_CS4 = 277.18f
const val NOTE_D4 = 293.66f
const val NOTE_DS4 = 311.13f
const val NOTE_E4 = 329.63f
const val NOTE_F4 = 349.23f
const val NOTE_FS4 = 369.99f
const val NOTE_G4 = 392.00f
const val NOTE_GS4 = 415.30f
const val NOTE_A4 = 440.00f // Concert Pitch
const val NOTE_AS4 = 466.16f
const val NOTE_B4 = 493.88f

const val NOTE_C5 = 523.25f
const val NOTE_CS5 = 554.37f
const val NOTE_D5 = 587.33f
const val NOTE_DS5 = 622.25f
const val NOTE_E5 = 659.25f
const val NOTE_F5 = 698.46f
const val NOTE_FS5 = 739.99f
const val NOTE_G5 = 783.99f
const val NOTE_GS5 = 830.61f
const val NOTE_A5 = 880.00f
const val NOTE_AS5 = 932.33f
const val NOTE_B5 = 987.77f

const val NOTE_C6 = 1046.50f

const val REST    = 0.00f

enum class SynthType {
    Sine,
    Square,
    Fm2op,
    Sawtooth,
    Opl2
}

data class WaveformData(
    val mixed: FloatArray = FloatArray(0),
    val channelWaveforms: Map<String, FloatArray> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaveformData) return false
        if (!mixed.contentEquals(other.mixed)) return false
        if (channelWaveforms.size != other.channelWaveforms.size) return false
        for ((k, v) in channelWaveforms) {
            val otherV = other.channelWaveforms[k] ?: return false
            if (!v.contentEquals(otherV)) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = mixed.contentHashCode()
        result = 31 * result + channelWaveforms.hashCode()
        return result
    }
}

class ChiptuneSynthesizer {
    public val currentWaveform = MutableSharedFlow<FloatArray>(replay = 0, extraBufferCapacity = 1)
    public val waveformData = MutableSharedFlow<WaveformData>(replay = 0, extraBufferCapacity = 1)
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private var masterSampleIndex = 0L
    private val scope = CoroutineScope(Dispatchers.Default)

    val channels = mutableListOf<AudioChannel>()

    // Mixer volume configuration per channel
    private val channelVolumes = mapOf(
        "Lead" to 0.35f,
        "Bass" to 0.40f,
        "Percussion" to 0.25f
    )

    private val drumPattern = listOf(
        DrumType.Kick, DrumType.HiHat, DrumType.Snare, DrumType.HiHat,
        DrumType.Kick, DrumType.HiHat, DrumType.Snare, DrumType.HiHat,
        DrumType.Kick, DrumType.HiHat, DrumType.Snare, DrumType.HiHat,
        DrumType.Kick, DrumType.Kick, DrumType.Snare, DrumType.HiHat
    )

    private fun generateAudio() {
        synthesisJob = scope.launch {
            val floatBuffer = FloatArray(1024)
            val masterFloatBuffer = FloatArray(1024)

            while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                floatBuffer.fill(0f)
                masterFloatBuffer.fill(0f)

                val currentChannels = channels.toList()
                val channelWavesMap = mutableMapOf<String, FloatArray>()

                for (ch in currentChannels) {
                    val chFloatBuffer = FloatArray(floatBuffer.size)
                    if (!ch.isMuted) {
                        ch.renderBlock(chFloatBuffer, masterSampleIndex, chFloatBuffer.size, sampleRate)
                    }

                    // 100% full-scale waveform for individual channel visualiser
                    val chWaveform = FloatArray(chFloatBuffer.size)
                    for (i in chFloatBuffer.indices) {
                        chWaveform[i] = chFloatBuffer[i].coerceIn(-1.0f, 1.0f)
                    }
                    channelWavesMap[ch.name] = chWaveform

                    // Scale by channel volume in mixer when mixing into master audio
                    if (!ch.isMuted) {
                        val vol = channelVolumes[ch.name] ?: 0.3f
                        for (i in floatBuffer.indices) {
                            floatBuffer[i] += chFloatBuffer[i] * vol
                        }
                    }
                }

                // Headroom scaling and output preparation
                val masterGain = 0.6f
                for (i in floatBuffer.indices) {
                    val mixedSignal = (floatBuffer[i] * masterGain).coerceIn(-1.0f, 1.0f)
                    masterFloatBuffer[i] = mixedSignal
                }

                val mixedWave = masterFloatBuffer.copyOf()
                currentWaveform.tryEmit(mixedWave)
                waveformData.tryEmit(WaveformData(mixed = mixedWave, channelWaveforms = channelWavesMap))

                audioTrack?.write(masterFloatBuffer, 0, masterFloatBuffer.size, AudioTrack.WRITE_BLOCKING)

                masterSampleIndex += floatBuffer.size
            }
        }
    }

    private fun initAudioTrack() {
        if (audioTrack != null) return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        audioTrack = AudioTrack.Builder()
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

        audioTrack?.play()
    }

    /**
     * Plays a sequence through once and stops automatically.
     */
    fun playOnce(
        sequence: List<Note>,
        synthType: SynthType = SynthType.Square,
        bpm: Double = 180.0,
        onComplete: (() -> Unit)? = null
    ) {
        stop()
        masterSampleIndex = 0L
        channels.clear()

        val totalBeats = sequence.sumOf { it.len.toDouble() }
        val samplesPerBeat = sampleRate * (60.0 / bpm)
        val totalSamples = (totalBeats * samplesPerBeat).toLong()

        channels.add(
            SequencedToneChannel(
                name = "Lead",
                synthType = synthType,
                sequence = sequence,
                bpm = bpm,
                dutyCycle = 0.5,
                loop = false
            )
        )

        initAudioTrack()

        synthesisJob = scope.launch {
            val floatBuffer = FloatArray(1024)
            val masterFloatBuffer = FloatArray(1024)

            while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING && masterSampleIndex < totalSamples) {
                floatBuffer.fill(0f)
                masterFloatBuffer.fill(0f)

                val currentChannels = channels.toList()
                val channelWavesMap = mutableMapOf<String, FloatArray>()

                for (ch in currentChannels) {
                    val chFloatBuffer = FloatArray(floatBuffer.size)
                    if (!ch.isMuted) {
                        ch.renderBlock(chFloatBuffer, masterSampleIndex, chFloatBuffer.size, sampleRate)
                    }

                    val chWaveform = FloatArray(chFloatBuffer.size)
                    for (i in chFloatBuffer.indices) {
                        chWaveform[i] = chFloatBuffer[i].coerceIn(-1.0f, 1.0f)
                    }
                    channelWavesMap[ch.name] = chWaveform

                    if (!ch.isMuted) {
                        val vol = channelVolumes[ch.name] ?: 0.5f
                        for (i in floatBuffer.indices) {
                            floatBuffer[i] += chFloatBuffer[i] * vol
                        }
                    }
                }

                val masterGain = 0.6f
                for (i in floatBuffer.indices) {
                    val mixedSignal = (floatBuffer[i] * masterGain).coerceIn(-1.0f, 1.0f)
                    masterFloatBuffer[i] = mixedSignal
                }

                val mixedWave = masterFloatBuffer.copyOf()
                currentWaveform.tryEmit(mixedWave)
                waveformData.tryEmit(WaveformData(mixed = mixedWave, channelWaveforms = channelWavesMap))

                audioTrack?.write(masterFloatBuffer, 0, masterFloatBuffer.size, AudioTrack.WRITE_BLOCKING)

                masterSampleIndex += floatBuffer.size
            }

            stop()
            onComplete?.invoke()
        }
    }

    /**
     * Starts synthesis for preview buttons.
     */
    fun start(
        synthType: SynthType,
        leadSequence: List<Note> = emptyList(),
        bassSequence: List<Note> = emptyList()
    ) {
        stop()
        masterSampleIndex = 0L
        channels.clear()
        if (leadSequence.isNotEmpty()) {
            channels.add(
                SequencedToneChannel(
                    name = "Lead",
                    synthType = synthType,
                    sequence = leadSequence,
                    dutyCycle = 0.5
                )
            )
        }
        if (bassSequence.isNotEmpty()) {
            channels.add(
                SequencedToneChannel(
                    name = "Bass",
                    synthType = synthType,
                    sequence = bassSequence,
                    dutyCycle = 0.25
                )
            )
        }
        channels.add(
            NoiseChannel(
                name = "Percussion",
                pattern = drumPattern
            )
        )
        initAudioTrack()
        generateAudio()
    }

    /**
     * Seeks playback to a specific timeline sample position.
     */
    fun seekToSample(sampleIndex: Long) {
        masterSampleIndex = sampleIndex
    }

    /**
     * Seeks playback to a specific timeline offset in seconds.
     */
    fun seekSeconds(seconds: Double) {
        masterSampleIndex = (seconds * sampleRate).toLong()
    }

    fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
        masterSampleIndex = 0L
        channels.forEach { it.reset() }
    }
}
