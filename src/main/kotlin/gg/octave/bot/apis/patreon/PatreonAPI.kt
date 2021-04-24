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

package gg.octave.bot.apis.patreon

import gg.octave.bot.Launcher
import gg.octave.bot.apis.patreon.entities.Patron
import gg.octave.bot.db.premium.PremiumUser
import gg.octave.bot.utils.RequestUtil
import io.sentry.Sentry
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class PatreonAPI(var accessToken: String?) {
    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        if (accessToken?.isEmpty() == false) {
            log.info("Can't pay? We'll take it away")
            //Scheduler.fixedRateScheduleWithSuppression(executor, 1, 1, TimeUnit.DAYS) { sweep() }
        }
    }

    fun sweep(): CompletableFuture<SweepStats> {
        val storedPledges = Launcher.db.getPremiumUsers()

        return fetchPledges().thenApply { pledges ->
            val total = storedPledges.size
            var changed = 0
            var removed = 0
            var fatal = 0

            for (entry in storedPledges.filterNot(PremiumUser::isOverride)) {
                try {
                    val userId = entry.idLong
                    val pledge = pledges.firstOrNull { it.discordUserId != null && it.discordUserId == userId }

                    if (pledge == null || pledge.isDeclined) {
                        Launcher.shardManager.openPrivateChannel(userId)
                            .flatMap {
                                it.sendMessage("Your pledge was either declined or removed from Patreon. " +
                                    "As a result, your perks have been revoked. If you believe this was in error, " +
                                    "check your payment method. If not, we hope Octave exceeded your expectations, and " +
                                    "we hope to see you again soon!")
                            }.queue()

                        for (guild in entry.premiumGuildsList) {
                            guild.delete()
                        }

                        entry.delete()
                        removed++
                        continue
                    }

                    val pledging = pledge.entitledAmountCents.toDouble() / 100

                    if (pledging < entry.pledgeAmount) { // User is pledging less than what we have stored.
                        entry.setPledgeAmount(pledging).save()

                        val entitledServers = entry.totalPremiumGuildQuota
                        val activatedServers = entry.premiumGuildsList
                        val exceedingLimitBy = activatedServers.size - entitledServers

                        if (exceedingLimitBy > 0) {
                            val remove = (0 until exceedingLimitBy)

                            for (unused in remove) {
                                activatedServers.firstOrNull()?.delete()
                            }
                            // Message about removed guilds? eh
                        }

                        changed++
                    }
                } catch (e: Exception) {
                    Sentry.capture(e)
                    fatal++
                }
            }

            SweepStats(total, changed, removed, fatal)
        }
        // Fetch pledges from database. Iterate through results from api.
        // If no pledge, or pledge is declined; remove from DB. Purge premium servers.
        // If pledgeAmount < stored, remove servers until quota is no longer exceeded.
        // Those pledging more than stored, update amount?
    }

    fun fetchPledges(campaignId: String = "754103") = fetchPledgesOfCampaign0(campaignId)

    private fun fetchPledgesOfCampaign0(campaignId: String, offset: String? = null): CompletableFuture<List<Patron>> {
        val initialUrl = baseUrl.newBuilder().apply {
            addPathSegments("api/oauth2/v2/campaigns/$campaignId/members")
            setQueryParameter("include", "currently_entitled_tiers,user")
            setQueryParameter("fields[member]", "full_name,last_charge_date,last_charge_status,lifetime_support_cents,currently_entitled_amount_cents,patron_status,pledge_relationship_start")
            setQueryParameter("fields[user]", "social_connections")
            setQueryParameter("page[count]", "100")
        }.build()

        return fetchPageOfPledgeRecursive(initialUrl, mutableListOf())
//            .thenCompose {
//                users.addAll(it.pledges)
//
//                if (it.hasMore && offset != it.offset) {
//                    fetchPledgesOfCampaign0(campaignId, it.offset)
//                } else {
//                    CompletableFuture.completedFuture(emptyList())
//                }
//            }
//            .thenAccept { users.addAll(it) }
//            .thenApply { users }
    }

    private fun fetchPageOfPledgeRecursive(url: HttpUrl, cache: MutableList<Patron>): CompletableFuture<List<Patron>> {
        println("fetching...")
        return request { url(url) }.thenApply {
            val nextLink = getNextPage(it)
            val members = it.getJSONArray("data")
            val users = it.getJSONArray("included")
            val patrons = mutableListOf<Patron>()

            for (user in users) {
                val obj = user as JSONObject

                if (obj.getString("type") != "user") {
                    continue
                }

                val userId = obj.getString("id")
                val member = members.firstOrNull { m ->
                    val mObj = m as JSONObject
                    val userData = mObj.getJSONObject("relationships").getJSONObject("user").getJSONObject("data")
                    return@firstOrNull userData.getString("id") == userId
                }

                if (member != null) {
                    patrons.add(Patron.from(member as JSONObject, obj))
                }
            }

            cache.addAll(patrons)
            nextLink
        }.thenCompose {
            when {
                it != null -> fetchPageOfPledgeRecursive(HttpUrl.get(it), cache)
                else -> CompletableFuture.completedFuture(cache)
            }
        }
    }

    private fun fetchPageOfPledge(campaignId: String, offset: String?): CompletableFuture<ResultPage> {
        return get {
            addPathSegments("api/campaigns/$campaignId/pledges")
            setQueryParameter("include", "pledge,patron")
            offset?.let { setQueryParameter("page[cursor]", it) }
        }.thenApply {
            val pledges = it.getJSONArray("data")
            val nextPage = getNextPage(it)
            val users = mutableListOf<PatreonUser>()

            for ((index, obj) in it.getJSONArray("included").withIndex()) {
                obj as JSONObject

                if (obj.getString("type") == "user") {
                    val pledge = pledges.getJSONObject(index)
                    users.add(PatreonUser.fromJsonObject(obj, pledge))
                }
            }

            // users
            ResultPage(listOf(), nextPage)
        }
    }

    private fun getNextPage(json: JSONObject): String? {
        if (json.isNull("links")) {
            return null
        }

        return json.getJSONObject("links")
            .takeIf { it.has("next") }
            ?.getString("next")
            //?.let { parseQueryString(it.getString("next"))["page[cursor]"] }
    }

    private fun parseQueryString(url: String): Map<String, String> {
        return URI(url).query
            .split('&')
            .map { it.split("=") }
            .associateBy({ decode(it[0]) }, { decode(it[1]) })
    }

    private fun decode(s: String) = URLDecoder.decode(s, Charsets.UTF_8)

    private fun get(urlOpts: HttpUrl.Builder.() -> Unit): CompletableFuture<JSONObject> {
        if (accessToken?.isNotEmpty() != true) {
            return CompletableFuture.failedFuture(IllegalStateException("Access token is empty!"))
        }

        val url = baseUrl.newBuilder().apply(urlOpts).build()
        return request { url(url) }
    }

    private fun request(requestOpts: Request.Builder.() -> Unit): CompletableFuture<JSONObject> {
        return RequestUtil.jsonObject({
            apply(requestOpts)
            header("Authorization", "Bearer $accessToken")
        }, true)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PatreonAPI::class.java)
        private val baseUrl = HttpUrl.get("https://www.patreon.com/")
    }
}
