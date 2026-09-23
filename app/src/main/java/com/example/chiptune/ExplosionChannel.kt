package com.example.chiptune

import kotlin.math.exp

class ExplosionChannel(
    override val name: String = "Explosion",
    override var isMuted: Boolean = false
) : AudioChannel {

    var filterEnabled: Boolean = true
    var cutoffFrequency: Float = 300f
    var dynamicSweep: Boolean = true
    var sweepStartFrequency: Float = 2500f
    var decayDuration: Float = 1.2f
    var loop: Boolean = false

    @Volatile
    private var triggerSampleIndex: Long = -1L
    private var filterState: Float = 0f
    private var lfsr: Int = 0x7FFF

    fun trigger(currentSampleIndex: Long = 0L) {
        triggerSampleIndex = currentSampleIndex
        filterState = 0f
        lfsr = 0x7FFF
    }

    fun stop() {
        triggerSampleIndex = -1L
        filterState = 0f
    }

    override fun reset() {
        triggerSampleIndex = -1L
        filterState = 0f
        lfsr = 0x7FFF
    }

    override fun renderBlock(outBuffer: FloatArray, startSampleIndex: Long, numSamples: Int, sampleRate: Int) {
        if (isMuted) return

        val totalDecaySamples = (decayDuration * sampleRate).toLong().coerceAtLeast(1L)

        for (i in 0 until numSamples) {
            val currentSample = startSampleIndex + i

            if (triggerSampleIndex < 0L && loop) {
                triggerSampleIndex = currentSample
            }

            if (triggerSampleIndex < 0L) continue

            var sampleOffset = currentSample - triggerSampleIndex

            if (loop && sampleOffset >= totalDecaySamples) {
                triggerSampleIndex = currentSample
                sampleOffset = 0L
                filterState = 0f
            }

            if (sampleOffset < 0L || sampleOffset >= totalDecaySamples) {
                continue
            }

            val t = sampleOffset / sampleRate.toDouble()

            // 1. Generate White Noise (15-bit LFSR pseudo-random noise)
            val bit = ((lfsr shr 0) xor (lfsr shr 1)) and 1
            lfsr = (lfsr shr 1) or (bit shl 14)
            val whiteNoise = if ((lfsr and 1) != 0) 1.0f else -1.0f

            // 2. Calculate Low Pass Filter Cutoff Frequency & Smoothing Factor alpha
            val currentCutoff = if (dynamicSweep && filterEnabled) {
                val sweepRate = 4.0 / decayDuration
                val envSweep = exp(-t * sweepRate)
                (cutoffFrequency + (sweepStartFrequency - cutoffFrequency) * envSweep).toFloat()
            } else {
                cutoffFrequency
            }

            val outputSample = if (filterEnabled) {
                val dt = 1.0 / sampleRate
                val rc = 1.0 / (2.0 * Math.PI * currentCutoff.coerceIn(20f, 20000f))
                val alpha = (dt / (rc + dt)).toFloat().coerceIn(0f, 1f)

                filterState += alpha * (whiteNoise - filterState)
                filterState
            } else {
                // Raw unfiltered white noise (harsh hiss)
                whiteNoise
            }

            // 3. Apply Volume Decay Envelope
            val envDecay = exp(-t * (3.0 / decayDuration)).toFloat()

            val signal = outputSample * envDecay
            outBuffer[i] += signal.coerceIn(-1.0f, 1.0f)
        }
    }
}
