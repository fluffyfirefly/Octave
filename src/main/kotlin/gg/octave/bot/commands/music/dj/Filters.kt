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

import gg.octave.bot.entities.framework.DJ
import gg.octave.bot.entities.framework.DonorOnly
import gg.octave.bot.entities.framework.MusicCog
import gg.octave.bot.entities.framework.Usages
import gg.octave.bot.music.MusicManagerV2
import gg.octave.bot.music.filters.EqualizerFilter
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.manager
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.SubCommand

class Filters : MusicCog {
    override fun sameChannel() = true
    override fun requirePlayingTrack() = true
    override fun requirePlayer() = true

    @DJ
    @DonorOnly
    @Command(aliases = ["filters", "fx", "effects"], description = "Apply audio filters to the music such as speed and pitch")
    fun filter(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @Usages("depth 0.5", "frequency 6")
    @SubCommand(aliases = ["t", "tr", "trem"], description = "Applies a \"wavy\" effect.")
    fun tremolo(ctx: Context, type: String, value: Double) = modifyTremolo(ctx, type, value, ctx.manager)

    @Usages("depth 0.5", "frequency 7", "strength 2")
    @SubCommand(aliases = ["v", "vi", "vibr"], description = "Applies a \"wobble\" effect.")
    fun vibrato(ctx: Context, type: String, value: Double) = modifyVibrato(ctx, type, value, ctx.manager)

    @Usages("speed 1.5", "pitch 0.7", "rate 1.25")
    @SubCommand(aliases = ["ts", "ti", "time"], description = "Adjust the pitch, rate, and speed.")
    fun timescale(ctx: Context, type: String, value: Double) = modifyTimescale(ctx, type, value, ctx.manager)

    @Usages("width 100", "level 1", "band 200")
    @SubCommand(aliases = ["k", "ka", "kara"], description = "Vocal filtering adjustments so you can singalong.")
    fun karaoke(ctx: Context, type: String?, value: Float?) = modifyKaraoke(ctx, type, value, ctx.manager)

    @SubCommand(aliases = ["s", "st", "stat"], description = "View the current status of filters.")
    fun status(ctx: Context) {
        val dspFilter = ctx.manager.dspFilter
        val enabledFilters = dspFilter.getEnabledFilters(true).filter { it !is EqualizerFilter }

        ctx.send {
            setColor(0x9570D3)
            setTitle("Enabled Music Effects")

            if (enabledFilters.isEmpty()) {
                setDescription("No effects are enabled. You can access available effects with `${ctx.trigger}fx`.")
            } else {
                for (filter in enabledFilters) {
                    addField(filter.name, filter.formatParameters(dspFilter), true)
                }
            }
        }
    }

    @SubCommand(description = "Clear all filters.")
    fun clear(ctx: Context) {
        ctx.manager.dspFilter.clearFilters()
        ctx.send("Cleared all filters.")
    }

    private fun modifyTimescale(ctx: Context, type: String, amount: Double, manager: MusicManagerV2) {
        val value = amount.coerceIn(0.1, 3.0)

        when (type) {
            "pitch", "p" -> manager.dspFilter.tsPitch = value
            "speed", "s" -> manager.dspFilter.tsSpeed = value
            "rate", "r" -> manager.dspFilter.tsRate = value
            else -> return ctx.send("Invalid choice `$type`, pick one of `pitch`/`speed`/`rate`.")
        }

        ctx.send("Timescale `${type.toLowerCase()}` set to `$value`")
    }

    private fun modifyTremolo(ctx: Context, type: String, amount: Double, manager: MusicManagerV2) {
        when (type) {
            "depth", "d" -> {
                val depth = amount.coerceIn(0.0, 1.0)
                manager.dspFilter.tDepth = depth.toFloat()
                ctx.send("Tremolo `depth` set to `$depth`")
            }
            "frequency", "freq", "f" -> {
                val frequency = amount.coerceAtLeast(0.1)
                manager.dspFilter.tFrequency = frequency.toFloat()
                ctx.send("Tremolo `frequency` set to `$frequency`")
            }
            else -> ctx.send("Invalid choice `$type`, pick one of `depth`/`frequency`.")
        }
    }

    private fun modifyVibrato(ctx: Context, type: String, amount: Double, manager: MusicManagerV2) {
        when (type) {
            "depth", "d" -> {
                val depth = amount.coerceIn(0.0, 1.0)
                manager.dspFilter.vDepth = depth.toFloat()
                ctx.send("Vibrato `depth` set to `$depth`")
            }
            "frequency", "freq", "f" -> {
                val frequency = amount.coerceIn(0.1, 14.0)
                manager.dspFilter.vFrequency = frequency.toFloat()
                ctx.send("Vibrato `frequency` set to `$frequency`")
            }
            "strength", "s" -> {
                val strength = amount.toInt().coerceIn(1, 3)
                manager.dspFilter.vStrength = strength
                ctx.send("Vibrato `strength` set to $strength")
            }
            else -> ctx.send("Invalid choice `$type`, pick one of `depth`/`frequency`.")
        }
    }

    private fun modifyKaraoke(ctx: Context, type: String?, amount: Float?, manager: MusicManagerV2) {
        if (type != null && (type == "level" || type == "band" || type == "width") && amount == null) {
            return ctx.send("You must specify a valid number for `amount`.")
        }

        when (type) {
            "level", "lvl", "l" -> {
                val level = amount!!.coerceAtLeast(0.0f)
                manager.dspFilter.kLevel = level
                return ctx.send("Karaoke `$type` set to `$level`")
            }
            "band", "b" -> manager.dspFilter.kFilterBand = amount!!
            "width", "w" -> manager.dspFilter.kFilterWidth = amount!!
//            "reset", "r" -> {
//                manager.dspFilter.apply {
//                    kLevel = 0.0f
//                    kFilterBand = 220f
//                    kFilterWidth = 100f
//                }
//                return ctx.send("Karaoke filter reset.")
//            }
            else -> return ctx.send("Invalid choice, `type` must be `level`/`band`/`width`.")
        }

        ctx.send("Karaoke `$type` set to `$amount`")
    }
}