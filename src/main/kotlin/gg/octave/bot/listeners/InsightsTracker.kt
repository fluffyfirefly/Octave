package gg.octave.bot.listeners

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener

class InsightsTracker : EventListener {
    override fun onEvent(event: GenericEvent) {

    }

    // Event List:
    //  VoiceConnect
    //   - User join, check playing. Check deafen status

    //  VoiceDeafen/Undeafen/GuildX
    //   - User

    //  VoiceMove
    //   - User

    //  VoiceLeave
    //   - User
}
