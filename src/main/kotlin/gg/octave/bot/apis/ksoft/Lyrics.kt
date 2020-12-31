package gg.octave.bot.apis.ksoft

import org.json.JSONObject

data class Lyrics(val artist: String, val name: String, val lyrics: String, val score: Double) {
    companion object {
        fun build(obj: JSONObject): Lyrics {
            val artist = obj.getString("artist")
            val track = obj.getString("name")
            val lyrics = obj.getString("lyrics")
            val score = obj.getDouble("search_score")

            return Lyrics(artist, track, lyrics, score)
        }
    }
}
