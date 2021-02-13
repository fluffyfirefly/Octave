package gg.octave.bot.commands.music.playlists

import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import gg.octave.bot.Launcher
import gg.octave.bot.db.music.CustomPlaylist
import gg.octave.bot.entities.framework.Usages
import gg.octave.bot.music.LoadResultHandler
import gg.octave.bot.music.utils.TrackContext
import gg.octave.bot.utils.Page
import gg.octave.bot.utils.extensions.*
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.annotations.Tentative
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.EmbedBuilder
import java.net.URL
import java.util.function.Consumer

class Playlists : Cog {
    @Command(aliases = ["pl", "cpl"], description = "Manage your custom playlists.")
    fun playlists(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @SubCommand(description = "Lists all of your custom playlists.")
    fun list(ctx: Context, page: Int = 1) {
        val allPlaylists = ctx.db.getCustomPlaylistsAsList(ctx.author.id).takeIf { it.isNotEmpty() }
            ?: return ctx.send {
                setColor(0x9571D3)
                setTitle("No Playlists :(")
                setDescription("That's OK! You can create a new one with `${ctx.trigger}playlists create <name>`\n*Without the `<>` of course.*")
            }

        val octavePlaylists = allPlaylists.filter { !it.isImported }
        val memerPlaylists = allPlaylists.filter { it.isImported }

        val octavePage = Page.paginate(octavePlaylists, page, "", ::formatPlaylist)
        val memerPage = Page.paginate(memerPlaylists, page, "", ::formatPlaylist)

        val showing = octavePage.elementCount + memerPage.elementCount
        val total = octavePlaylists.size + memerPlaylists.size

        ctx.send {
            setColor(0x9571D3)
            setTitle("Your Playlists")

            addField("Playlists (${octavePage.maxPages.plural("page")})", octavePage.content, true)
            if (memerPage.content.isNotEmpty()) {
                addField("Imported (${memerPage.maxPages.plural("page")})", memerPage.content, true)
            }

            setFooter("Showing $showing of $total playlists • Page ${octavePage.page} • Specify a page to go to.")
        }
    }

    @SubCommand(aliases = ["new", "add", "+"], description = "Create a new custom playlist.")
    fun create(ctx: Context, @Greedy name: String) {
        if (!checkQuota(ctx)) {
            return
        }

        val existingPlaylist = ctx.db.getCustomPlaylist(ctx.author.id, name)

        if (existingPlaylist != null) {
            return ctx.send("You already have a playlist with this name.")
        }

        CustomPlaylist.createWith(ctx.author.id, name)
            .save()

        ctx.send {
            setColor(0x9571D3)
            setTitle("Your Playlists")
            setDescription("Your shiny new playlist has been created.")
        }
    }

    @SubCommand(aliases = ["del", "remove", "-"], description = "Delete one of your custom playlists.")
    fun delete(ctx: Context, @Greedy name: String) {
        val existingPlaylist = ctx.db.getCustomPlaylistByNameOrId(ctx.author.id, name)
            ?: return ctx.send {
                setColor(0x9571D3)
                setTitle("Your Playlists")
                setDescription("No custom playlists found with that name.\nTo prevent accidental deletion, you need to enter the full playlist name.")
            }

        existingPlaylist.delete()

        ctx.send {
            setColor(0x9571D3)
            setTitle("Your Playlists")
            setDescription("Your custom playlist has been removed.")
        }
    }

    @SubCommand(aliases = ["manage"], description = "Edit an existing playlist (move/remove/...)")
    fun edit(ctx: Context, @Greedy name: String) {
        val existingPlaylist = ctx.db.findCustomPlaylist(ctx.author.id, name)
            ?: return ctx.send("You don't have any playlists with that name.")

        ctx.messageChannel.sendMessage(EmbedBuilder().apply {
            setColor(0x9571D3)
            setDescription("Loading playlist...")
        }.build()).queue({
            PlaylistManager(existingPlaylist, ctx, it)
        }, {
            ctx.send("Failed to load playlist: `${it.localizedMessage}`")
        })
    }

    @SubCommand(description = "Share a custom playlist).")
    fun share(ctx: Context, @Greedy name: String) {
        val existingPlaylist = ctx.db.findCustomPlaylist(ctx.author.id, name)
            ?: return ctx.send("You don't have any playlists with that name.")

        if (!existingPlaylist.isExposed) {
            return ctx.send {
                setColor(0x9571D3)
                setTitle("Whoops...")
                setDescription("This playlist is not marked as public. If you wish to share this playlist, you will need to do the following:\n" +
                    "**1.** `${ctx.trigger}playlists edit ${existingPlaylist.name}`\n" +
                    "**2.** `privacy public`\n" +
                    "**3.** `save`\n" +
                    "**4.** `${ctx.trigger}playlists share ${existingPlaylist.name}`")
            }
        }

        ctx.send {
            setColor(0x9571D3)
            setTitle("Sharing playlist \"${existingPlaylist.name}\"")
            setDescription(
                "Your unique playlist ID is `${existingPlaylist.id}`.\n" +
                    "Share this with your friends - it will allow them to play your playlist and even clone it!\n\n" +
                    "Playing a shared playlist -> `${ctx.trigger}playlists play ${existingPlaylist.id}`\n" +
                    "Cloning a shared playlist -> `${ctx.trigger}playlists clone ${existingPlaylist.id}`"
            )
            addField(
                "Changed your mind?",
                "Simply head over to the playlist editor (`${ctx.trigger}playlists edit ${existingPlaylist.name}`) and " +
                    "set the privacy to private!",
                false
            )
            setFooter("Sharing is caring 👏")
        }
    }

    @SubCommand(description = "Import a playlist from YouTube/SoundCloud/...")
    fun import(ctx: Context, url: URL, @Greedy name: String?) {
        if (!checkQuota(ctx)) {
            return
        }

        val loader = FunctionalResultHandler(
            Consumer { ctx.send("This is not a playlist.") },
            Consumer {
                val importName = name
                    ?: it.name
                    ?: return@Consumer ctx.send("The playlist does not have a name. You need to specify one instead.")
                // Last bit shouldn't happen but better safe than sorry.

                val existing = ctx.db.getCustomPlaylist(ctx.author.id, importName)

                if (existing != null) {
                    return@Consumer ctx.send("A playlist with that name already exists. Specify a different one.")
                    // Maybe we could append tracks to a playlist here? TODO, or, INVESTIGATE
                }

                val playlist = CustomPlaylist.createWith(ctx.author.id, importName)
                playlist.setTracks(it.tracks)
                playlist.save()

                ctx.send {
                    setColor(0x9571D3)
                    setTitle("Playlist Imported")
                    setDescription("The playlist `${it.name}` has been imported as `$importName` successfully!")
                }
            },
            Runnable { ctx.send("The URL doesn't lead to a valid playlist.") },
            Consumer { ctx.send("Failed to load the media resource.\n`${it.localizedMessage}`") }
        )

        Launcher.players.playerManager.loadItem(url.toString(), loader)
    }

    @SubCommand(aliases = ["copy", "yoink", "steal"], description = "Clone a public playlist to your library!")
    fun clone(ctx: Context, playlistId: String, @Greedy rename: String?) {
        if (!checkQuota(ctx)) {
            return
        }

        val playlist = ctx.db.getCustomPlaylistById(playlistId)
            ?: return ctx.send("No playlists found with that ID.")

        if (playlist.author == ctx.author.id) {
            return ctx.send {
                setColor(0x9571D3)
                setTitle("Whoops...")
                setDescription("You can't clone your own playlist.")
            }
        }

        if (!playlist.isExposed) {
            return ctx.send {
                setColor(0x9571D3)
                setTitle("Whoops...")
                setDescription("The playlist you're trying to clone is set to private.")
                setFooter("🥺 lemme clone your playlist bro")
            }
        }

        if (ctx.db.getCustomPlaylist(ctx.author.id, rename ?: playlist.name) != null) {
            return ctx.send {
                setColor(0x9571D3)
                setTitle("Whoops...")
                setDescription("You already have a playlist with the same name as the one you're cloning.\n" +
                    "Re-run the command but specify a new name.\nExample: `${ctx.trigger}playlists clone $playlistId My New Playlist Except It's Stolen`")
            }
        }

        val clonedPlaylist = CustomPlaylist.createWith(ctx.author.id, rename ?: playlist.name)
        clonedPlaylist.setTracks(playlist.decodedTracks)
        clonedPlaylist.save()

        ctx.send {
            setColor(0x9571D3)
            setTitle("Playlist Cloned")
            setDescription("Successfully cloned `${playlist.name}` to your library.")
        }
    }

    @SubCommand(aliases = ["play"], description = "Loads a custom playlist for playing.")
    @Usages("On Repeat", "yes Top Songs 2020")
    fun load(ctx: Context, @Tentative shuffle: Boolean = false, @Greedy name: String) {
        when {
            ctx.voiceChannel == null -> {
                return ctx.send {
                    setColor(0x9571D3)
                    setTitle("Your Playlists")
                    setDescription("You need to be in a voice channel to load a playlist.")
                }
            }
            ctx.guild!!.selfMember.voiceState?.channel != null && ctx.voiceChannel != ctx.guild!!.selfMember.voiceState?.channel -> {
                return ctx.send {
                    setColor(0x9571D3)
                    setTitle("Your Playlists")
                    setDescription("The bot is already playing music in another channel.")
                }
            }
        }

        var existingPlaylist = ctx.db.findCustomPlaylist(ctx.author.id, name)

        if (existingPlaylist == null && name.length == 5) {
            existingPlaylist = ctx.db.getCustomPlaylistById(name)
        }

        if (existingPlaylist == null) {
            return ctx.send {
                setColor(0x9571D3)
                setTitle("Unknown Playlist")
                setDescription("There were no playlists found with that name/ID.")
            }
        }

        if (!existingPlaylist.isExposed && existingPlaylist.author != ctx.author.id) {
            return ctx.send {
                setColor(0x9571D3)
                setTitle("Whoops...")
                setDescription("The playlist you're trying to play is set to private.")
                setFooter("🥺 lemme play your playlist bro")
            }
        }

        val manager = Launcher.players.get(ctx.guild!!)
        val lrh = LoadResultHandler(null, ctx, manager, TrackContext(ctx.author.idLong, ctx.textChannel!!.idLong), false, shuffle, null)
        lrh.playlistLoaded(existingPlaylist.toBasicAudioPlaylist())
    }

    // fun share(ctx: Context, @Greedy name: String)
    // fun privacy(ctx: Context, setting: ..., @Greedy name: String) // Changes whether a playlist can be viewed by other users.
    // fun snoop(ctx: Context, user: User) // snoop on other user's custom playlists.

    // Perhaps track how many times a playlist has been cloned, and show it in a leaderboard thing.
    //  - "Top cloned playlists"

    // method to remove multiple tracks from playlist

    private fun checkQuota(ctx: Context): Boolean {
        val quota = ctx.premiumUser.remainingCustomPlaylistQuota

        if (quota <= 0) {
            ctx.send {
                setColor(0x9571D3)
                setTitle("Your Playlists")
                setDescription("You don't have any remaining custom playlist slots.\n[Donate for more slots](http://patreon.com/octavebot)")
            }
            return false
        }

        return true
    }

    private fun formatPlaylist(index: Int, playlist: CustomPlaylist): String {
        return buildString {
            append("`")
            append(index + 1)
            append(".` ")
            append(playlist.name)
            append(" - `")
            append(playlist.id)
            append("`")

            if (playlist.isExposed) {
                append(" \\🔓 ")
            }

            append("\n")
        }
    }
}
