package gg.octave.bot.entities.framework.parsers

import gg.octave.bot.entities.framework.UserOrId
import me.devoxin.flight.api.Context
import me.devoxin.flight.internal.parsers.Parser
import java.util.*
import java.util.regex.Pattern

class UserOrIdParser : Parser<UserOrId> {
    private val extendedMemberParser = ExtendedMemberParser()

    override fun parse(ctx: Context, param: String): Optional<UserOrId> {
        val firstParse = extendedMemberParser.parse(ctx, param)
        val matcher = snowflakeMatch.matcher(param)

        return when {
            firstParse.isPresent -> firstParse.get().let { Optional.of(UserOrId(it.user, it.idLong)) }
            matcher.matches() -> Optional.of(UserOrId(null, (matcher.group("sid") ?: matcher.group("id")).toLong()))
            else -> Optional.ofNullable(null)
        }
    }

    companion object {
        private val snowflakeMatch = Pattern.compile("^(?:<@!?(?<sid>[0-9]{17,21})>|(?<id>[0-9]{17,21}))$")
    }
}
