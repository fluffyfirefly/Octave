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

package gg.octave.bot.commands.music.dj

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import gg.octave.bot.Launcher
import gg.octave.bot.commands.music.embedTitle
import gg.octave.bot.commands.music.embedUri
import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.entities.framework.Usages
import gg.octave.bot.utils.extensions.manager
import gg.octave.bot.utils.extensions.removeAll
import gg.octave.bot.utils.extensions.removeAt
import gg.octave.bot.utils.truncate
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Matcher
import java.util.regex.Pattern

class Remove : MusicCog {
    override fun sameChannel() = true

    private val rangePattern = Pattern.compile("(\\d+)?\\s*?(?:\\.\\.|-)\\s*(\\d+)?")
    private val multipleIndexPattern = Pattern.compile("\\d+(?:\\s+)?")

    @DJ
    @Usages("first", "last", "all", "1..5", "1", "1 7 12 3", "rickroll")
    @Command(aliases = ["removesong", "rm", "rem"], description = "Remove a song from the queue.")
    fun remove(ctx: Context, @Greedy which: String?) {
        val queue = ctx.manager.queue

        if (queue.isEmpty()) {
            return ctx.send("The queue is empty.")
        }

        val track = when (which) {
            null -> return ctx.send("You need to specify what to remove. (`first`/`last`/`all`/`1..3`)")
            "first" -> queue.remove() //Remove head
            "last" -> queue.removeAt(queue.size - 1)
            "all" -> {
                queue.clear()
                return ctx.send("Cleared the music queue.")
            }
            else -> {
                val rangeMatcher = rangePattern.matcher(which)
                val multipleIndexMatcher = multipleIndexPattern.matcher(which)

                return when {
                    rangeMatcher.find() -> removeRange(ctx, rangeMatcher)
                    multipleIndexMatcher.find() -> removeMany(ctx, which.split("\\s+".toRegex()))
                    else -> removeByTitle(ctx, which)
                }
            }
        }

        val decodedTrack = Launcher.players.playerManager.decodeAudioTrack(track)
        ctx.send {
            setColor(0x9570D3)
            setTitle("Track Removed")
            setDescription("Removed __[${decodedTrack.info.embedTitle}](${decodedTrack.info.embedUri})__ from the queue.")
        }
    }

    fun removeRange(ctx: Context, matcher: Matcher) {
        val queue = ctx.manager.queue

        if (matcher.group(1) == null && matcher.group(2) == null) {
            return ctx.send("You must specify start range and/or end range.")
        }

        val start = matcher.group(1).let {
            if (it == null) 1
            else it.toIntOrNull()?.coerceAtLeast(1)
                ?: return ctx.send("Invalid start of range")
        }

        val end = matcher.group(2).let {
            if (it == null) queue.size
            else it.toIntOrNull()?.coerceAtMost(queue.size)
                ?: return ctx.send("Invalid end of range")
        }

        val range = (end downTo start).map { it - 1 }
        queue.removeAll(range)

        return ctx.send("Removed tracks `${range.size}` from the queue.")
    }

    fun removeMany(ctx: Context, ind: List<String>) {
        val queue = ctx.manager.queue
        val invalidIndexes = ind.filter {
            val int = it.toIntOrNull()
            return@filter int == null || int < 1 || int > queue.size
        }

        if (invalidIndexes.isNotEmpty()) {
            return ctx.send {
                setColor(0x9570D3)
                setTitle("Invalid Track Indexes")
                setDescription(
                    "The following track numbers are invalid: ${invalidIndexes.joinToString("`, `", prefix = "`", postfix = "`")}\n" +
                        "You must specify the position (or multiple positions) of tracks in the queue you want to remove.\n\n" +
                        "Alternatively, you can try `remove 1`, `remove 1..${queue.size}`, `remove first` or `remove last`."
                )
            }
        }

        val indexes = ind.map { it.toInt() - 1 }.sortedByDescending { it }
        val removed = queue.removeAll(indexes)

        return ctx.send("Removed `$removed` tracks from the queue.")
    }

    fun removeByTitle(ctx: Context, title: String) {
        if (title.length < 3) {
            return ctx.send {
                setColor(0x9570D3)
                setDescription("Your query needs to be at least 3 characters long.\nRefine your query and try again.")
            }
        }

        val queue = ctx.manager.queue
        val toRemove = queue.mapIndexedNotNull { index, track -> checkTitle(index, track, title) }

        when (toRemove.size) {
            0 -> ctx.send {
                setColor(0x9570D3)
                setTitle("No Tracks Found")
                setDescription("Your search query didn't match any tracks in the queue.\nRefine your search and try again?")
            }
            1 -> {
                val (index, track) = toRemove.first()
                queue.removeAt(index)
                ctx.send {
                    setColor(0x9570D3)
                    setTitle("Track Removed")
                    setDescription("Removed `${track.info.embedTitle}` from the queue.")
                }
            }
            else -> {
                val tracks = toRemove.truncate(5) { (_, t) -> t.info.embedTitle }.joinToString("\n")

                ctx.messageChannel
                    .sendMessage(
                        EmbedBuilder().apply {
                            setColor(0x9570D3)
                            setTitle("Remove Tracks")
                            setDescription("The query you provided is ambiguous.\n" +
                                "Do you want to remove the following tracks?:\n```\n$tracks```\n" +
                                "Send `y` to remove **`${toRemove.size}`** tracks\n" +
                                "Send `n` to preserve the tracks.")
                        }.build()
                    )
                    .submit()
                    .thenCompose { _ ->
                        ctx.commandClient.waitFor(MessageReceivedEvent::class.java, {
                            it.author.idLong == ctx.author.idLong && it.channel.idLong == ctx.messageChannel.idLong
                                && (it.message.contentRaw.toLowerCase() == "y" || it.message.contentRaw.toLowerCase() == "n")
                        }, 15000)
                    }
                    .thenAccept {
                        if (it.message.contentRaw.toLowerCase() == "y") {
                            queue.removeAll(toRemove.map { (index, _) -> index })
                            ctx.send {
                                setColor(0x9570D3)
                                setTitle("Tracks Removed")
                                setDescription("Successfully removed **`${toRemove.size}`** tracks from the queue.")
                            }
                        } else {
                            ctx.send {
                                setColor(0x9570D3)
                                setDescription("Removal cancelled. The tracks will remain in the queue.")
                            }
                        }
                    }
            }
        }
    }

    private fun checkTitle(index: Int, encodedTrack: String, title: String): Pair<Int, AudioTrack>? {
        val decoded = Launcher.players.playerManager.decodeAudioTrack(encodedTrack)

        if (decoded.info.title == title || decoded.info.title.toLowerCase().contains(title)) {
            return Pair(index, decoded)
        }

        return null
    }
}
