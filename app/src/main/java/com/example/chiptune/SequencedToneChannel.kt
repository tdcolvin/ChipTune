package com.example.chiptune

import kotlin.math.sin

class SequencedToneChannel(
    override val name: String,
    var synthType: SynthType,
    val sequence: List<Note>,
    val bpm: Double = 150.0,
    override var isMuted: Boolean = false,
    val dutyCycle: Double = 0.5,
    val loop: Boolean = true
) : AudioChannel {

    override fun reset() {
        // Deterministic channels need no internal state resets
    }

    override fun renderBlock(outBuffer: FloatArray, startSampleIndex: Long, numSamples: Int, sampleRate: Int) {
        if (isMuted || sequence.isEmpty()) return

        val samplesPerWholeNote = sampleRate * (60.0 / bpm)
        val twoPi = 2.0 * Math.PI

        var totalSequenceSamples = 0L
        val noteStartSamples = LongArray(sequence.size)
        val noteTotalSamples = LongArray(sequence.size)
        for (idx in sequence.indices) {
            val noteLenSamples = (samplesPerWholeNote * sequence[idx].len).toLong().coerceAtLeast(1L)
            noteStartSamples[idx] = totalSequenceSamples
            noteTotalSamples[idx] = noteLenSamples
            totalSequenceSamples += noteLenSamples
        }

        if (totalSequenceSamples <= 0L) return

        var activeNoteIdx = 0

        for (i in 0 until numSamples) {
            val currentSample = startSampleIndex + i
            if (!loop && currentSample >= totalSequenceSamples) {
                continue
            }

            val loopSample = if (loop) {
                (currentSample % totalSequenceSamples + totalSequenceSamples) % totalSequenceSamples
            } else {
                currentSample
            }

            if (loopSample < noteStartSamples[activeNoteIdx] || loopSample >= noteStartSamples[activeNoteIdx] + noteTotalSamples[activeNoteIdx]) {
                activeNoteIdx = sequence.indices.firstOrNull { idx ->
                    loopSample >= noteStartSamples[idx] && loopSample < noteStartSamples[idx] + noteTotalSamples[idx]
                } ?: 0
            }

            val currentNote = sequence[activeNoteIdx]
            val noteSampleCounter = loopSample - noteStartSamples[activeNoteIdx]
            val totalNoteSamples = noteTotalSamples[activeNoteIdx]
            val freq = currentNote.freq

            val signal = if (freq <= 0f) {
                0.0
            } else {
                val t = currentSample / sampleRate.toDouble()
                val gateRatio = if (currentNote.stacatto) 0.5 else 0.85

                val gateEnvelope = if (noteSampleCounter < totalNoteSamples * gateRatio) {
                    1.0
                } else {
                    if (currentNote.stacatto) {
                        0.0
                    } else {
                        val remainingSamples = totalNoteSamples - noteSampleCounter
                        val gateWindow = totalNoteSamples * (1.0 - gateRatio)
                        if (gateWindow > 0) {
                            (remainingSamples / gateWindow).coerceIn(0.0, 1.0)
                        } else {
                            0.0
                        }
                    }
                }

                val patch = synthType.patch
                val rawSignal = if (patch != null) {
                    patch.renderSample(freq.toDouble(), noteSampleCounter, sampleRate, masterGain = 0.7)
                } else {
                    when (synthType) {
                        SynthType.Sine -> sin(twoPi * freq * t)
                        SynthType.Square -> {
                            val phase = (t * freq) % 1.0
                            if (phase < dutyCycle) 1.0 else -1.0
                        }
                        SynthType.Sawtooth -> {
                            val period = 1.0 / freq
                            val progress = (t % period) / period
                            2.0 * progress - 1.0
                        }
                        else -> 0.0
                    }
                }

                rawSignal * gateEnvelope
            }

            outBuffer[i] += signal.toFloat()
        }
    }
}
