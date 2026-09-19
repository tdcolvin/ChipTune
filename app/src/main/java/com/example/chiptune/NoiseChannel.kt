package com.example.chiptune

import kotlin.math.exp
import kotlin.math.sin

enum class DrumType {
    None,
    Kick,
    Snare,
    HiHat
}

class NoiseChannel(
    override val name: String = "Percussion",
    val pattern: List<DrumType>,
    val bpm: Double = 150.0,
    val stepLen: Float = 0.25f,
    override var isMuted: Boolean = false
) : AudioChannel {

    override fun reset() {
        // Deterministic channels need no internal state resets
    }

    override fun renderBlock(outBuffer: FloatArray, startSampleIndex: Long, numSamples: Int, sampleRate: Int) {
        if (isMuted || pattern.isEmpty()) return

        val samplesPerWholeNote = sampleRate * (60.0 / bpm)
        val samplesPerStep = (samplesPerWholeNote * stepLen).toLong().coerceAtLeast(1L)
        val totalPatternSamples = samplesPerStep * pattern.size

        if (totalPatternSamples <= 0L) return

        var lfsr = 0x7FFF
        var prevStep = -1

        for (i in 0 until numSamples) {
            val currentSample = startSampleIndex + i
            val loopSample = (currentSample % totalPatternSamples + totalPatternSamples) % totalPatternSamples

            val stepIndex = (loopSample / samplesPerStep).toInt().coerceIn(pattern.indices)
            val drumSampleCounter = loopSample % samplesPerStep

            if (stepIndex != prevStep || drumSampleCounter == 0L) {
                lfsr = (0x7FFF xor (stepIndex * 31337)) and 0x7FFF
                if (lfsr == 0) lfsr = 0x7FFF
                prevStep = stepIndex
            }

            val bit = ((lfsr shr 0) xor (lfsr shr 1)) and 1
            lfsr = (lfsr shr 1) or (bit shl 14)
            val noiseVal = if ((lfsr and 1) != 0) 1.0f else -1.0f

            val tDrum = drumSampleCounter / sampleRate.toDouble()
            val activeDrum = pattern[stepIndex]

            val signal = when (activeDrum) {
                DrumType.Kick -> {
                    val env = exp(-tDrum * 25.0)
                    val freq = 40.0 + 110.0 * env
                    val sine = sin(2.0 * Math.PI * freq * tDrum)
                    val punch = noiseVal * 0.2f * exp(-tDrum * 60.0)
                    (sine * 0.8 + punch) * env
                }
                DrumType.Snare -> {
                    val noiseEnv = exp(-tDrum * 18.0)
                    val toneEnv = exp(-tDrum * 30.0)
                    val tone = sin(2.0 * Math.PI * 180.0 * tDrum) * toneEnv
                    val noise = noiseVal * noiseEnv
                    (tone * 0.4 + noise * 0.6)
                }
                DrumType.HiHat -> {
                    val env = exp(-tDrum * 50.0)
                    val noise = noiseVal * env
                    noise * 0.5
                }
                DrumType.None -> 0.0
            }

            outBuffer[i] += signal.toFloat()
        }
    }
}
