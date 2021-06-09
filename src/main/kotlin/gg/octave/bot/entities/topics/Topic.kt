package gg.octave.bot.entities.topics

abstract class Topic(val name: String, val description: String) {
    val subtopics = mutableListOf<Subtopic>()

    fun subtopic(brief: String, description: () -> String) {
        subtopics.add(Subtopic(brief, description()))
    }

    inner class Subtopic(val brief: String, val details: String)
}
