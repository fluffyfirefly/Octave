package gg.octave.bot.apis.patreon.entities

enum class LastChargeStatus {
    DECLINED,
    PAID,
    UNKNOWN;

    companion object {
        fun from(value: String): LastChargeStatus {
            return when (value) {
                "Declined" -> DECLINED
                "Paid" -> PAID
                else -> UNKNOWN
            }
        }
    }
}
