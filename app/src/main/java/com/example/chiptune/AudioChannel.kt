package com.example.chiptune

interface AudioChannel {
    val name: String
    var isMuted: Boolean
    var volume: Float

    /**
     * Renders [numSamples] audio frames into [outBuffer] starting at master timeline position [startSampleIndex].
     * The implementation adds its sample values (-1.0f to 1.0f) to the existing contents of [outBuffer].
     */
    fun renderBlock(outBuffer: FloatArray, startSampleIndex: Long, numSamples: Int, sampleRate: Int)

    /**
     * Resets any non-deterministic state if needed.
     */
    fun reset()
}
