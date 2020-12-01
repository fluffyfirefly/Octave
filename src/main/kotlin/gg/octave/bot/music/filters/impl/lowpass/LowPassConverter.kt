package gg.octave.bot.music.filters.impl.lowpass

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter

open class LowPassConverter(private val downstream: FloatPcmAudioFilter, channelCount: Int) : FloatPcmAudioFilter {
    var smoothingFactor = 20f

    private val values = FloatArray(channelCount)
    private var initialized = false

    override fun process(input: Array<out FloatArray>, offset: Int, length: Int) {
        if (!initialized) {
            for (c in input.indices) {
                values[c] = input[c][offset]
            }

            initialized = true
        }

        for (c in input.indices) {
            var value = values[c]

            for (i in offset until offset + length) {
                val currentValue = input[c][i]
                value += (currentValue - value) / smoothingFactor
                input[c][i] = value
            }

            values[c] = value
        }

        downstream.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        initialized = false
    }

    override fun flush() {

    }

    override fun close() {

    }
}
