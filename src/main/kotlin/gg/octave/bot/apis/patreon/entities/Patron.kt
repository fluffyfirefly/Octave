package gg.octave.bot.apis.patreon.entities

import gg.octave.bot.apis.patreon.PatreonUser
import gg.octave.bot.apis.patreon.PatronTier
import org.json.JSONObject

class Patron(
    val id: Int,
    val fullName: String,
    val lastChargeDate: String?,
    val lastChargeStatus: LastChargeStatus,
    val patronStatus: PatronStatus,
    val entitledAmountCents: Int,
    val lifetimeSupportCents: Int,
    val entitledTiers: List<PatronTier>,
    val discordUserId: Long?
) {
    val isDeclined = patronStatus == PatronStatus.DECLINED_PATRON
    val highestPledgeAmount = entitledTiers.firstOrNull()?.tierAmountCents ?: entitledAmountCents

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PatreonUser) return false

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode()
    }

    companion object {
        fun from(data: JSONObject, included: JSONObject): Patron {
            val id = included.getString("id").toInt()
            val memberAttr = data.getJSONObject("attributes")
            val includedAttr = included.getJSONObject("attributes")

            val fullName = memberAttr.getString("full_name")
            val lastChargeDate = memberAttr.optString("last_charge_date")
            val lastChargeStatus = LastChargeStatus.from(memberAttr.optString("last_charge_status"))
            val patronStatus = PatronStatus.from(memberAttr.optString("patron_status"))
            val entitledAmountCents = memberAttr.getInt("currently_entitled_amount_cents")
            val lifetimeSupportCents = memberAttr.getInt("lifetime_support_cents")

            val tiers = data.getJSONObject("relationships").getJSONObject("currently_entitled_tiers").getJSONArray("data")
            val entitledTiers = tiers.map { it as JSONObject }
                .filter { it.getString("type") == "tier" }
                .map { PatronTier.from(it.getString("id").toInt()) }
                .sortedByDescending { it.tierAmountCents }

            val social = if (!includedAttr.isEmpty) includedAttr.getJSONObject("social_connections") else null
            val discordUserId = if (social?.isNull("discord") == false) social.getJSONObject("discord").getString("user_id").toLong() else null

            return Patron(id, fullName, lastChargeDate, lastChargeStatus, patronStatus, entitledAmountCents, lifetimeSupportCents, entitledTiers, discordUserId)
        }
    }
}
