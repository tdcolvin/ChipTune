package com.example.chiptune

import kotlin.math.exp

enum class PercussionSound(val displayName: String, val description: String) {
    HIHAT_PATTERN("16-Step Hi-Hat Beat", "Full 16-step repeating percussion beat with open/closed hi-hats and crash cymbal."),
    CLOSED_HIHAT("Closed Hi-Hat", "Short noise burst with fast decay (~50ms) and high cutoff."),
    OPEN_HIHAT("Open Hi-Hat", "Sustained noise burst with medium decay (~220ms)."),
    CRASH_CYMBAL("Crash Cymbal", "Long metallic noise burst with slow decay (~550ms).")
}

class PercussionBeatChannel(
    override val name: String = "Percussion",
    override var isMuted: Boolean = false
) : AudioChannel {

    var selectedSound: PercussionSound = PercussionSound.HIHAT_PATTERN
    var bpm: Double = 120.0
    var isPlaying: Boolean = false

    val pattern16Step = intArrayOf(
        3, 0, 1, 0,  1, 0, 1, 0,
        1, 0, 1, 0,  1, 0, 1, 2
    )

    private var lfsr: Int = 0x5555
    private var filterState: Float = 0f

    @Volatile
    var currentStep: Int = 0
        private set

    override fun reset() {
        lfsr = 0x5555
        filterState = 0f
        currentStep = 0
    }

    override fun renderBlock(outBuffer: FloatArray, startSampleIndex: Long, numSamples: Int, sampleRate: Int) {
        if (isMuted || !isPlaying) return

        val samplesPerBeat = sampleRate * (60.0 / bpm)
        val samplesPerStep = (samplesPerBeat / 4.0).toLong().coerceAtLeast(1L)
        val totalPatternSamples = samplesPerStep * 16

        if (totalPatternSamples <= 0L) return

        for (i in 0 until numSamples) {
            val currentSample = startSampleIndex + i
            val loopSample = (currentSample % totalPatternSamples + totalPatternSamples) % totalPatternSamples

            val stepIdx = (loopSample / samplesPerStep).toInt().coerceIn(0, 15)
            val stepSampleCounter = loopSample % samplesPerStep
            currentStep = stepIdx

            val soundTypeOnStep = if (selectedSound == PercussionSound.HIHAT_PATTERN) {
                pattern16Step[stepIdx]
            } else {
                if (stepIdx % 4 == 0) {
                    when (selectedSound) {
                        PercussionSound.CLOSED_HIHAT -> 1
                        PercussionSound.OPEN_HIHAT -> 2
                        PercussionSound.CRASH_CYMBAL -> 3
                        PercussionSound.HIHAT_PATTERN -> 1
                    }
                } else 0
            }

            if (soundTypeOnStep == 0) continue

            val tStep = stepSampleCounter / sampleRate.toDouble()

            val (decayTime, cutoff) = when (soundTypeOnStep) {
                1 -> Pair(0.05f, 6000f)
                2 -> Pair(0.22f, 4500f)
                3 -> Pair(0.55f, 3500f)
                else -> Pair(0.05f, 5000f)
            }

            val maxSamplesForSound = (decayTime * sampleRate).toLong()
            if (stepSampleCounter >= maxSamplesForSound) continue

            val bit = ((lfsr shr 0) xor (lfsr shr 1)) and 1
            lfsr = (lfsr shr 1) or (bit shl 14)
            val noise = if ((lfsr and 1) != 0) 1.0f else -1.0f

            val dt = 1.0 / sampleRate
            val rc = 1.0 / (2.0 * Math.PI * cutoff)
            val alpha = (dt / (rc + dt)).toFloat().coerceIn(0f, 1f)
            filterState += alpha * (noise - filterState)

            val metallicNoise = noise - (filterState * 0.4f)
            val env = exp(-tStep * (4.0 / decayTime)).toFloat()

            val signal = metallicNoise * env * 0.4f
            outBuffer[i] += signal.coerceIn(-1.0f, 1.0f)
        }
    }
}
