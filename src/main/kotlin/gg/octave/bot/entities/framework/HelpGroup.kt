package gg.octave.bot.entities.framework

// Defines the group that the command will be a part of when listed in the help command
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class HelpGroup(val name: String)
