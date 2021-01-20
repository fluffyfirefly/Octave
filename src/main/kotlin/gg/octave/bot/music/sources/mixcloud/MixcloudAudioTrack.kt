package gg.octave.bot.music.sources.mixcloud

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import java.net.URI

class MixcloudAudioTrack(trackInfo: AudioTrackInfo, private val sourceManager: MixcloudAudioSourceManager) : DelegatedAudioTrack(trackInfo) {
    override fun makeClone(): AudioTrack {
        return MixcloudAudioTrack(trackInfo, sourceManager)
    }

    override fun getSourceManager(): AudioSourceManager {
        return sourceManager
    }

    @Throws(Exception::class)
    override fun process(localExecutor: LocalAudioTrackExecutor) {
        sourceManager.httpInterfaceManager.`interface`.use {
            processStatic(localExecutor, it)
        }
    }

    @Throws(Exception::class)
    private fun processStatic(localExecutor: LocalAudioTrackExecutor, httpInterface: HttpInterface) {
        val playbackUrl = getPlaybackUrl()

        PersistentHttpStream(httpInterface, URI(playbackUrl), Long.MAX_VALUE).use { stream ->
            processDelegate(MpegAudioTrack(trackInfo, stream), localExecutor)
        }
    }

    private fun getPlaybackUrl(): String {
        val json = sourceManager.extractTrackInfoGraphQl(trackInfo.author, trackInfo.identifier)
            ?: throw FriendlyException("This track is unplayable", FriendlyException.Severity.SUSPICIOUS, null)

        val streamInfo = json.get("streamInfo")
        val mp4Url = streamInfo.get("url").text()

        return Utils.decryptUrl(MixcloudAudioSourceManager.DECRYPTION_KEY, mp4Url)
    }
}
