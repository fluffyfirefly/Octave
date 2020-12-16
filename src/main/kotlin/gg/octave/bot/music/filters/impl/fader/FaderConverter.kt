/*
 * MIT License
 *
 * Copyright (c) 2020 Melms Media LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package gg.octave.bot.music.filters.impl.fader

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import kotlin.math.cos
import kotlin.math.sin

open class FaderConverter(private val downstream: FloatPcmAudioFilter) : FloatPcmAudioFilter {
    var x = 0.0
    val delta = 0.01

    override fun process(input: Array<out FloatArray>, offset: Int, length: Int) {
        for (c in input.indices) { // channel
            val isL = c % 2 == 0

            for (i in offset until offset + length) {
                val sample = input[c][i]

                if (isL) {
                    input[c][i] = ((sin(x) + 1) * sample).toFloat()
                } else {
                    input[c][i] = ((cos(x + Math.PI / 2) + 1) * sample).toFloat()
                }
            }
        }

        x += delta
        downstream.process(input, offset, length)
    }

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {

    }

    override fun flush() {

    }

    override fun close() {

    }
}
