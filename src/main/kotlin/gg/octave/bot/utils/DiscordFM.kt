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

package gg.octave.bot.utils

import org.apache.commons.io.IOUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class DiscordFM {
    private val cache: HashMap<String, List<String>>

    init {
        val cls = this::class.java
        val dfmPlaylists = Reflections("dfm", ResourcesScanner())
            .getResources(".*\\.txt".toPattern())
            .map { it.drop(4).dropLast(4) }

        cache = HashMap(dfmPlaylists.size)

        for (playlist in dfmPlaylists) {
            cls.getResourceAsStream("/dfm/$playlist.txt").use {
                if (it == null) {
                    return@use log.warn("Playlist {} does not exist, skipping...", playlist)
                }

                try {
                    val collect = IOUtils.toString(it, Charsets.UTF_8)
                        .split('\n')
                        .map { s -> s.split(' ')[0] }
                        .filter { s -> s.startsWith("https://") }

                    cache[playlist] = collect
                    log.info("Added {} tracks from playlist {}", collect.size, playlist)
                } catch (e: IOException) {
                    log.error("Failed to load playlist {}", playlist, e)
                }
            }
        }
    }

    fun libraries() = cache.keys.toSet()

    fun getRandomSong(library: String): String? {
        return cache[library]?.random()?.trim { it <= ' ' } //?: "https://www.youtube.com/watch?v=D7npse9n-Yw"
    }

    companion object {
        private val log = LoggerFactory.getLogger(DiscordFM::class.java)
        /*val LIBRARIES = arrayOf(
            "chill corner", "christmas", "classical", "coffee house jazz",
            "country countdown", "electro hub", "electro swing", "funk",
            "halloween", "hip hop", "japanese lounge", "korean madness",
            "metal mix", "purely pop", "retro renegade", "rock n roll"
        )*/
    }
}
