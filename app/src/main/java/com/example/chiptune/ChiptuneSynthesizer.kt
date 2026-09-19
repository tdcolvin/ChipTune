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
private const val NOTE_A1 = 55.00f
private const val NOTE_AS1 = 58.27f
private const val NOTE_B1 = 61.74f

private const val NOTE_C2 = 65.41f
private const val NOTE_CS2 = 69.30f
private const val NOTE_D2 = 73.42f
private const val NOTE_DS2 = 77.78f
private const val NOTE_E2 = 82.41f
private const val NOTE_F2 = 87.31f
private const val NOTE_FS2 = 92.50f
private const val NOTE_G2 = 98.00f
private const val NOTE_GS2 = 103.83f
private const val NOTE_A2 = 110.00f
private const val NOTE_AS2 = 116.54f
private const val NOTE_B2 = 123.47f

private const val NOTE_C3 = 130.81f
private const val NOTE_CS3 = 138.59f
private const val NOTE_D3 = 146.83f
private const val NOTE_DS3 = 155.56f
private const val NOTE_E3 = 164.81f
private const val NOTE_F3 = 174.61f
private const val NOTE_FS3 = 185.00f
private const val NOTE_G3 = 196.00f
private const val NOTE_GS3 = 207.65f
private const val NOTE_A3 = 220.00f
private const val NOTE_AS3 = 233.08f
private const val NOTE_B3 = 246.94f

private const val NOTE_C4 = 261.63f // Middle C
private const val NOTE_CS4 = 277.18f
private const val NOTE_D4 = 293.66f
private const val NOTE_DS4 = 311.13f
private const val NOTE_E4 = 329.63f
private const val NOTE_F4 = 349.23f
private const val NOTE_FS4 = 369.99f
private const val NOTE_G4 = 392.00f
private const val NOTE_GS4 = 415.30f
private const val NOTE_A4 = 440.00f // Concert Pitch
private const val NOTE_AS4 = 466.16f
private const val NOTE_B4 = 493.88f

private const val NOTE_C5 = 523.25f
private const val NOTE_CS5 = 554.37f
private const val NOTE_D5 = 587.33f
private const val NOTE_DS5 = 622.25f
private const val NOTE_E5 = 659.25f
private const val NOTE_F5 = 698.46f
private const val NOTE_FS5 = 739.99f
private const val NOTE_G5 = 783.99f
private const val NOTE_GS5 = 830.61f
private const val NOTE_A5 = 880.00f
private const val NOTE_AS5 = 932.33f
private const val NOTE_B5 = 987.77f

private const val NOTE_C6 = 1046.50f

private const val REST    = 0.00f

// The classic 32-step Tetris opening melody loop
/*
private val leadSequenceTetris = floatArrayOf(
    NOTE_E5, REST,    NOTE_B4, NOTE_C5, NOTE_D5, REST,    NOTE_C5, NOTE_B4,
    NOTE_A4, REST,    NOTE_A4, NOTE_C5, NOTE_E5, REST,    NOTE_D5, NOTE_C5,
    NOTE_B4, REST,    REST,    NOTE_C5, NOTE_D5, REST,    NOTE_E5, REST,
    NOTE_C5, REST,    NOTE_A4, REST,    NOTE_A4, REST,    REST,    REST
)

// The iconic bouncing bassline that gives Tetris its momentum
private val bassSequenceTetris = floatArrayOf(
    NOTE_A3, NOTE_E4, NOTE_A3, NOTE_E4, NOTE_D4, NOTE_F4, NOTE_D4, NOTE_F4,
    NOTE_C4, NOTE_E4, NOTE_C4, NOTE_E4, NOTE_E3, NOTE_B4, NOTE_E3, NOTE_B4,
    NOTE_A3, NOTE_E4, NOTE_A3, NOTE_E4, NOTE_D4, NOTE_F4, NOTE_D4, NOTE_F4,
    NOTE_C4, NOTE_E4, NOTE_A3, NOTE_E4, NOTE_A3, NOTE_E3, NOTE_A3, REST
)

private const val NOTE_D3 = 146.83f
private const val NOTE_G3 = 196.00f
private const val NOTE_Bb3 = 233.08f
private const val NOTE_Bb4 = 466.16f


// The iconic opening woodwind / synth lead hook
private val leadSequence = floatArrayOf(
    NOTE_D4, REST,    NOTE_F4, NOTE_G4, NOTE_A4, REST,    NOTE_Bb4, NOTE_A4,
    NOTE_G4, REST,    NOTE_F4, NOTE_G4, NOTE_A4, REST,    REST,     REST,
    NOTE_D4, REST,    NOTE_F4, NOTE_G4, NOTE_A4, REST,    NOTE_C5,  NOTE_A4,
    NOTE_G4, REST,    NOTE_F4, NOTE_E4, NOTE_D4, REST,    REST,     REST
)

// The classic reggae-style syncopated bassline
private val bassSequence = floatArrayOf(
    NOTE_D3, REST,    REST,    NOTE_D3, NOTE_G3, REST,    REST,     NOTE_G3,
    NOTE_A3, REST,    REST,    NOTE_A3, NOTE_D3, REST,    REST,     REST,
    NOTE_D3, REST,    REST,    NOTE_D3, NOTE_F3, REST,    REST,     NOTE_F3, // F3 is 174.61f
    NOTE_C4, REST,    NOTE_Bb3,REST,    NOTE_D3, REST,    REST,     REST
)*/

enum class SynthType {
    Sine,
    Square,
    Fm2op,
    Sawtooth,
    Opl2
}

class ChiptuneSynthesizer {
    public val currentWaveform = MutableSharedFlow<ShortArray>(replay = 0, extraBufferCapacity = 1)
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private var masterSampleIndex = 0L
    private val scope = CoroutineScope(Dispatchers.Default)

    val channels = mutableListOf<AudioChannel>()

    private val tetrisBass = listOf(
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),
        Note(NOTE_E3, 0.5f),
        Note(NOTE_E4, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),

        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),

        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),
        Note(NOTE_D3, 0.5f),
        Note(NOTE_D4, 0.5f),

        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),
        Note(NOTE_C3, 0.5f),
        Note(NOTE_C4, 0.5f),

        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),
        Note(NOTE_B2, 0.5f),
        Note(NOTE_B3, 0.5f),

        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
        Note(NOTE_A2, 0.5f),
        Note(NOTE_A3, 0.5f),
    )
    private val tetris = listOf(
        // Measure 1
        Note(NOTE_E5, 1.0f),
        Note(NOTE_B4, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_B4, 0.5f),

        // Measure 2
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 0.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_C5, 0.5f),

        // Measure 3
        Note(NOTE_B4, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_E5, 1.0f),

        // Measure 4
        Note(NOTE_C5, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(REST, 1.0f),

        // Measure 5
        Note(REST, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_F5, 0.5f),
        Note(NOTE_A5, 1.0f),
        Note(NOTE_G5, 0.5f),
        Note(NOTE_F5, 0.5f),

        // Measure 6
        Note(NOTE_E5, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_E5, 1.0f),
        Note(NOTE_D5, 0.5f),
        Note(NOTE_C5, 0.5f),

        // Measure 7
        Note(NOTE_B4, 1.5f),
        Note(NOTE_C5, 0.5f),
        Note(NOTE_D5, 1.0f),
        Note(NOTE_E5, 1.0f),

        // Measure 8
        Note(NOTE_C5, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(NOTE_A4, 1.0f),
        Note(REST, 1.0f),
    )

    private val leadSeq2 = listOf(
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

    private val drumPattern = listOf(
        DrumType.Kick, DrumType.HiHat, DrumType.Snare, DrumType.HiHat,
        DrumType.Kick, DrumType.HiHat, DrumType.Snare, DrumType.HiHat,
        DrumType.Kick, DrumType.HiHat, DrumType.Snare, DrumType.HiHat,
        DrumType.Kick, DrumType.Kick, DrumType.Snare, DrumType.HiHat
    )

    private fun generateAudio() {
        synthesisJob = scope.launch {
            val floatBuffer = FloatArray(1024)
            val shortBuffer = ShortArray(1024)

            while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                floatBuffer.fill(0f)

                // Render block for each non-muted channel (0 CPU cost for muted channels!)
                for (ch in channels) {
                    if (!ch.isMuted) {
                        ch.renderBlock(floatBuffer, masterSampleIndex, floatBuffer.size, sampleRate)
                    }
                }

                // Headroom scaling and 8-bit DAC bit-crush quantization
                val masterGain = 0.6f
                for (i in floatBuffer.indices) {
                    val mixedSignal = (floatBuffer[i] * masterGain).coerceIn(-1.0f, 1.0f)

                    val amplitude = (mixedSignal * Short.MAX_VALUE).toInt()
                    val bitCrushed = (amplitude shr 8) shl 8

                    shortBuffer[i] = bitCrushed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                currentWaveform.tryEmit(shortBuffer.copyOf())
                audioTrack?.write(shortBuffer, 0, shortBuffer.size)

                masterSampleIndex += floatBuffer.size
            }
        }
    }

    private fun initAudioTrack() {
        if (audioTrack != null) return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
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
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
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
     * Starts synthesis for preview buttons.
     */
    fun start(synthType: SynthType) {
        stop()
        masterSampleIndex = 0L
        channels.clear()
        channels.add(
            SequencedToneChannel(
                name = "Lead",
                synthType = synthType,
                sequence = tetris,
                dutyCycle = 0.5,
                volume = 0.35f
            )
        )
        channels.add(
            SequencedToneChannel(
                name = "Bass",
                synthType = synthType,
                sequence = tetrisBass,
                dutyCycle = 0.25,
                volume = 0.40f
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
