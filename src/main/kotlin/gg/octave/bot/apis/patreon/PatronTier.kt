package gg.octave.bot.apis.patreon

enum class PatronTier(val tierId: Int, val tierName: String, val tierAmountCents: Int) {
    UNKNOWN(0, "Unknown", 0),
    /* Legacy Tiers */
    FLUFFY_GNAR_OLD(1763704, "Fluffy Gnar (OLD)", 100),
    DINO_GNAR_OLD(1763714, "Dino Gnar (OLD)", 200),
    DINO_GNAR(2994970, "Dino Gnar", 300),
    HOODIE_GNAR(1782176, "Hoodie Gnar (OLD)", 500),
    BOUNCY_GNAR(1866536, "Bouncy Gnar (OLD)", 1000),

    /* Current */
    DONOR(2994968, "Donor", 200),
    SUPPORTER(2994973, "Supporter", 500),
    HUGE_SUPPORTER(2994974, "Huge Supporter!", 1000),
    SUPREME_SUPPORTER(2994977, "SUPREME Supporter", 2000);

    companion object {
        fun from(tierId: Int): PatronTier = values().firstOrNull { it.tierId == tierId } ?: UNKNOWN
    }
}
