package gg.octave.bot.apis.ksoft

import gg.octave.bot.utils.RequestUtil
import gg.octave.bot.utils.extensions.url
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class KSoftClient(private val token: String?) {
    fun lyricsSearch(query: String, textOnly: Boolean = false, limit: Int = 1): CompletableFuture<List<Lyrics>> {
        val queryParams = mapOf(
            "q" to query,
            "text_only" to textOnly,
            "limit" to limit,
            "clean_up" to "true"
        )
        return request("/lyrics/search", queryParams)
            .thenApply {
                if (!it.has("total") || it.getInt("total") == 0) {
                    throw KsoftException("No results found.")
                }

                val entries = it.getJSONArray("data")
                return@thenApply entries.map { e -> Lyrics.build(e as JSONObject) }
            }
    }

    private fun request(path: String, query: Map<String, Any>): CompletableFuture<JSONObject> {
        if (token.isNullOrEmpty()) {
            return CompletableFuture.failedFuture(IllegalStateException("KSoft API not configured!"))
        }

        return RequestUtil.jsonObject {
            url("$BASE_API_URL$path") {
                for ((k, v) in query) {
                    this.addQueryParameter(k, v.toString())
                }
            }
            header("Authorization", "Bearer $token")
        }
    }

    companion object {
        private const val BASE_API_URL = "https://api.ksoft.si"
    }
}
