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

package gg.octave.bot.commands.music

import com.jagrosh.jdautilities.paginator
import gg.octave.bot.utils.RequestUtil
import gg.octave.bot.utils.extensions.launcher
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.utils.TextSplitter
import org.json.JSONException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class Lyrics : Cog {
    @Command(description = "Shows the lyrics of the current song")
    fun lyrics(ctx: Context) {
        val manager = ctx.launcher.players.getExisting(ctx.guild)
            ?: return ctx.send("There's no player to be seen here.")

        val audioTrack = manager.player.playingTrack
            ?: return ctx.send("There's no song playing currently.")

        val title = audioTrack.info.title
        sendLyricsFor(ctx, title)
    }

    @SubCommand(description = "Search for specific song lyrics")
    fun search(ctx: Context, @Greedy content: String) = sendLyricsFor(ctx, content)

    private fun sendLyricsFor(ctx: Context, title: String) {
        val encodedTitle = URLEncoder.encode(title, Charsets.UTF_8)

        RequestUtil.jsonObject {
            url("https://lyrics.tsu.sh/v1/?q=$encodedTitle")
            header("User-Agent", "Octave (DiscordBot, https://github.com/Stardust-Discord/Octave)")
        }.thenAccept {
            if (!it.isNull("error")) {
                return@thenAccept ctx.send("No lyrics found for `${ctx.cleanContent(title)}`. Try another song?")
            }

            val lyrics = URLDecoder.decode(it.getString("content"), Charsets.UTF_8)
                .replace("\n{2,}".toRegex(), "\n\n")
                .replace("&amp;", "&")
                .replace("([(\\[])([^\\n]*?)\\n+([^\\n)\\]]+)\\n+([)\\]])".toRegex(), "$1$2$3$4")
            val pages = TextSplitter.split(lyrics, 1000)

            val songObject = it.getJSONObject("song")
            val fullTitle = songObject.getString("full_title")

            ctx.textChannel?.let { tx ->
                ctx.launcher.eventWaiter.paginator {
                    setUser(ctx.author)
                    setTitle("Lyrics for $fullTitle")
                    setEmptyMessage("There should be something here 👀")
                    setItemsPerPage(1)
                    setTimeout(1, TimeUnit.MINUTES)
                    finally { message -> message?.delete()?.queue() }

                    for (page in pages) {
                        entry { page }
                    }
                }.display(tx)
            }
        }.exceptionally {
            when (it) {
                is JSONException -> ctx.send("An invalid response was returned by the API. Try again?")
                else -> ctx.send(it.localizedMessage)
            }
            return@exceptionally null
        }
    }
}