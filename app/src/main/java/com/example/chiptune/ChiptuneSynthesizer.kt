package com.example.chiptune

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.sin

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
private val scope = CoroutineScope(Dispatchers.Default)

// Sequence translated from the spreadsheet transcription (1 step = 1 Eighth Note)
private val leadSequence = floatArrayOf(
    // Measure 1
    NOTE_B4, NOTE_B4, NOTE_B4, NOTE_B4, // B (Quarter - 4 steps)
    NOTE_B4, NOTE_B4, NOTE_B4,         // B (Dotted Eighth - 3 steps)
    NOTE_E5,                           // E (Sixteenth - 1 step)
    NOTE_DS5, NOTE_DS5,                 // D# (Eighth - 2 steps)
    NOTE_B4, NOTE_B4,                   // B (Eighth - 2 steps)
    NOTE_B4, NOTE_B4, NOTE_B4, NOTE_B4, // B (Quarter - 4 steps)

    // Measure 2
    NOTE_E5, NOTE_E5, NOTE_E5, NOTE_E5, // E (Quarter - 4 steps)
    NOTE_E5, NOTE_E5, NOTE_E5,         // E (Dotted Eighth - 3 steps)
    NOTE_FS5,                          // F# (Sixteenth - 1 step)
    NOTE_G5, NOTE_G5,                   // G (Eighth - 2 steps)
    NOTE_G5, NOTE_G5,                   // G (Eighth - 2 steps)
    NOTE_FS5, NOTE_FS5,                 // F# (Eighth - 2 steps)
    NOTE_G5, NOTE_G5,                   // G (Eighth - 2 steps)

    // Measure 3
    NOTE_FS5, NOTE_FS5,                 // F# (Eighth - 2 steps)
    NOTE_E5, NOTE_E5,                   // E (Eighth - 2 steps)
    NOTE_FS5, NOTE_FS5, NOTE_FS5, NOTE_FS5,
    NOTE_FS5, NOTE_FS5, NOTE_FS5, NOTE_FS5  // F# (Half note - 8 steps)
)

    val leadSeq = listOf(
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

    private fun generateAudio(synthType: SynthType) {
        synthesisJob = scope.launch {
            val buffer = ShortArray(1024)
            var sampleIndex = 0L

            val bpm = 150.0

            val samplesPerWholeNote = (sampleRate * (60.0 / bpm))

            var currentNoteIndex = 0
            var noteSampleCounter = 0L

            // OPL2 specific state
            var phaseModulator = 0.0
            var phaseCarrier = 0.0
            var oplEnvelope = 1.0
            val twoPi = 2.0 * Math.PI

            while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                for (i in buffer.indices) {
                    val currentNote = leadSeq[currentNoteIndex]
                    val totalNoteSamples = (samplesPerWholeNote * currentNote.len).toLong()

                    if (noteSampleCounter >= totalNoteSamples) {
                        noteSampleCounter = 0L
                        currentNoteIndex = (currentNoteIndex + 1) % leadSeq.size
                        
                        // Reset OPL2 state for new note
                        phaseModulator = 0.0
                        phaseCarrier = 0.0
                        oplEnvelope = 1.0
                    }

                    val updatedNote = leadSeq[currentNoteIndex]
                    val leadFreq = updatedNote.freq
                    val t = sampleIndex / sampleRate.toDouble()

                    val mixedSignal = if (synthType == SynthType.Opl2) {
                        if (leadFreq > 0f) {
                            val incCarrier = twoPi * leadFreq / sampleRate
                            val incModulator = twoPi * (leadFreq * 3.5) / sampleRate

                            val modIndex = 2.5 * oplEnvelope
                            // OPL2 Half-sine for modulator
                            val modOut = if (phaseModulator % (2.0 * Math.PI) < Math.PI) sin(phaseModulator) else 0.0
                            val finalModOut = modOut * modIndex
                            
                            val carrierOut = sin(phaseCarrier + finalModOut) * oplEnvelope
                            
                            oplEnvelope *= 0.99992
                            
                            phaseModulator = (phaseModulator + incModulator) % twoPi
                            phaseCarrier = (phaseCarrier + incCarrier) % twoPi
                            
                            carrierOut * 0.7
                        } else {
                            0.0
                        }
                    } else {
                        // Dynamically assign gate properties based on staccato status
                        val gateRatio = if (updatedNote.stacatto) 0.5 else 0.85

                        val gateEnvelope = if (noteSampleCounter < totalNoteSamples * gateRatio) {
                            1.0 // Note is active
                        } else {
                            if (updatedNote.stacatto) {
                                0.0 // Staccato: cut off immediately
                            } else {
                                // Standard note: quick linear ramp down to prevent audio pops
                                val remainingSamples = totalNoteSamples - noteSampleCounter
                                val gateWindow = totalNoteSamples * (1.0 - gateRatio)
                                if (gateWindow > 0) {
                                    (remainingSamples / gateWindow).coerceIn(0.0, 1.0)
                                } else {
                                    0.0
                                }
                            }
                        }

                        // Waveform Synthesis Switchboard
                        val leadSignal = if (leadFreq > 0f) {
                            val rawSine = sin(2 * Math.PI * leadFreq * t)

                            when (synthType) {
                                SynthType.Sine -> rawSine
                                SynthType.Square -> if (rawSine >= 0.0) 1.0 else -1.0
                                SynthType.Fm2op -> {
                                    val modulatorFreq = leadFreq * 2.0
                                    val modulationIndex = 2.2
                                    val modulator = sin(2 * Math.PI * modulatorFreq * t)
                                    sin(2 * Math.PI * leadFreq * t + (modulator * modulationIndex))
                                }
                                SynthType.Sawtooth -> {
                                    val period = 1.0 / leadFreq
                                    val progress = (t % period) / period
                                    2.0 * progress - 1.0
                                }
                                else -> 0.0
                            }
                        } else {
                            0.0
                        }
                        leadSignal * 0.30 * gateEnvelope
                    }

                    // 8-bit DAC Bit-crush step for true vintage hardware grime
                    val amplitude = (mixedSignal * Short.MAX_VALUE).toInt()
                    val bitCrushed = (amplitude shr 8) shl 8

                    buffer[i] = bitCrushed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    // Advance counters
                    sampleIndex++
                    noteSampleCounter++
                }

                currentWaveform.tryEmit(buffer.copyOf())
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }
    fun start(
        synthType: SynthType
    ) {
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
        generateAudio(synthType = synthType)
    }
    /*
    private fun generateAudio() {
        synthesisJob = scope.launch {
            val buffer = ShortArray(1024)
            var sampleIndex = 0L

            // Brisk, bouncy reggae-infused tempo (125 BPM)
            val samplesPerStep = (sampleRate * 60) / (125 * 4)

            while (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                for (i in buffer.indices) {
                    val currentStep = ((sampleIndex / samplesPerStep) % leadSequence.size).toInt()

                    val leadFreq = leadSequence[currentStep]
                    val bassFreq = bassSequence[currentStep]
                    val t = sampleIndex / sampleRate.toDouble()

                    // Channel 1: Lead Melody (Triangle/Saw hybrid wave for an Amiga-style reed texture)
                    val leadSignal = if (leadFreq > 0f) {
                        val phase = (t * leadFreq) % 1.0
                        // Mathematical ramp-up then sharp drop
                        if (phase < 0.8) (phase / 0.8) * 2.0 - 1.0 else 1.0 - ((phase - 0.8) / 0.2) * 2.0
                    } else {
                        0.0
                    }

                    // Channel 2: Bouncing Bassline (Sharp 10% Duty Cycle Pulse Wave for a SID-chip punch)
                    val bassSignal = if (bassFreq > 0f) {
                        val phase = (t * bassFreq) % 1.0
                        if (phase < 0.10) 1.0 else -1.0
                    } else {
                        0.0
                    }

                    // Mix channels together while leaving some headroom
                    val mixedSignal = (leadSignal * 0.2) + (bassSignal * 0.25)

                    // Down-quantize to 8-bit to strip out modern smooth resolution
                    val amplitude = (mixedSignal * Short.MAX_VALUE).toInt()
                    val bitCrushed = (amplitude shr 8) shl 8

                    buffer[i] = bitCrushed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    sampleIndex++
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }
     */

    fun stop() {
        synthesisJob?.cancel()
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
    }
}