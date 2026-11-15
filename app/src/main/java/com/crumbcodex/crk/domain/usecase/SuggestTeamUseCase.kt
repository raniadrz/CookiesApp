package com.crumbcodex.crk.domain.usecase

import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.TierEntry
import com.crumbcodex.crk.domain.model.Treasure

data class SuggestedMember(val cookie: Cookie, val reason: String)

data class TeamSuggestion(
    val members: List<SuggestedMember>,
    val synergy: TeamSynergy,
    /** True when the user owns fewer than 5 suitable cookies and unowned ones were used to fill. */
    val usedUnowned: Boolean,
)

/**
 * Builds the strongest 5-cookie lineup for a mode from a candidate pool (normally the cookies the user owns).
 *
 * Each cookie gets a value from its tier in that mode plus the user's recorded power/level. A greedy pass
 * builds a valid core (frontline, healer where it matters), then a local search swaps one member at a time
 * while the combined value + synergy score improves. Pure Kotlin, unit-tested.
 */
class SuggestTeamUseCase(private val synergy: TeamSynergyCalculator) {

    operator fun invoke(
        mode: GameMode,
        allCookies: List<Cookie>,
        owned: List<OwnedCookie>,
        tiers: List<TierEntry>,
        treasures: List<Treasure>,
        ownedOnly: Boolean = true,
    ): TeamSuggestion? {
        val ownedById = owned.associateBy { it.cookieId }
        val tierById = tiers.filter { it.mode == mode }.associate { it.cookie.id to it.tier }
        val maxPower = owned.maxOfOrNull { it.power }?.coerceAtLeast(1) ?: 1

        fun value(c: Cookie): Double {
            val tier = tierById[c.id]
            var v = tier?.let { TIER_VALUE[it.uppercase()] } ?: UNTIERED_VALUE
            // Rarer cookies are usually stronger even when not on the tier list.
            if (tier == null) v += RARITY_BONUS[c.rarity.name] ?: 0.0
            ownedById[c.id]?.let { o ->
                v += 20.0 * o.power / maxPower
                v += 10.0 * o.level / OwnedCookie.MAX_LEVEL
            }
            return v
        }

        val ownedPool = allCookies.filter { it.id in ownedById }
        val pool = if (ownedOnly && ownedPool.size >= Team.SIZE) ownedPool
        else (ownedPool + allCookies.filter { it.id !in ownedById }.sortedByDescending { value(it) }).distinctBy { it.id }
        if (pool.size < Team.SIZE) return null
        val usedUnowned = pool.size > ownedPool.size

        val values = pool.associate { it.id to value(it) }
        val ranked = pool.sortedByDescending { values.getValue(it.id) }

        // Greedy core.
        val team = mutableListOf<Cookie>()
        fun takeBest(filter: (Cookie) -> Boolean) {
            ranked.firstOrNull { it !in team && filter(it) }?.let { team += it }
        }
        takeBest { it.position == Position.FRONT }
        if (mode != GameMode.GUILD_BOSS) takeBest { it.cookieClass == CookieClass.HEALING }
        while (team.size < Team.SIZE) {
            val front = team.count { it.position == Position.FRONT }
            val healers = team.count { it.cookieClass == CookieClass.HEALING }
            val next = ranked.firstOrNull {
                it !in team &&
                    !(it.position == Position.FRONT && front >= MAX_FRONT) &&
                    !(it.cookieClass == CookieClass.HEALING && healers >= MAX_HEALERS)
            } ?: ranked.first { it !in team }
            team += next
        }

        fun objective(t: List<Cookie>): Double =
            t.sumOf { values.getValue(it.id) } + SYNERGY_WEIGHT * synergy(mode, t, treasures).score

        // Local search: best single swap per round.
        var best = objective(team)
        val candidates = ranked.take(CANDIDATE_LIMIT)
        repeat(MAX_ROUNDS) {
            var improved = false
            for (i in team.indices) {
                for (c in candidates) {
                    if (c in team) continue
                    val trial = team.toMutableList().also { it[i] = c }
                    val score = objective(trial)
                    if (score > best + 1e-9) {
                        best = score; team[i] = c; improved = true
                    }
                }
            }
            if (!improved) return@repeat
        }

        val ordered = team.sortedWith(compareBy({ it.position.sortOrder }, { -values.getValue(it.id) }))
        val members = ordered.map { c -> SuggestedMember(c, reasonFor(c, mode, tierById[c.id], ownedById[c.id], team)) }
        return TeamSuggestion(members, synergy(mode, ordered, treasures), usedUnowned)
    }

    private fun reasonFor(c: Cookie, mode: GameMode, tier: String?, owned: OwnedCookie?, team: List<Cookie>): String {
        val parts = mutableListOf<String>()
        if (tier != null) parts += "$tier tier in ${mode.shortLabel}"
        if (c.position == Position.FRONT && team.count { it.position == Position.FRONT } == 1) parts += "holds the frontline"
        if (c.cookieClass == CookieClass.HEALING) parts += "keeps the team alive"
        if (c.cookieClass == CookieClass.SUPPORT) parts += "buffs the team"
        if (owned != null && owned.power > 0) parts += "your power ${owned.power}"
        if (owned == null) parts += "not in your collection yet"
        return parts.ifEmpty { listOf("${c.cookieClass.label} · ${c.position.label}") }.joinToString(" · ")
    }

    private companion object {
        val TIER_VALUE = mapOf("SS" to 110.0, "S+" to 100.0, "S" to 85.0, "A" to 70.0, "B" to 55.0, "C" to 40.0)
        const val UNTIERED_VALUE = 30.0
        val RARITY_BONUS = mapOf("BEAST" to 25.0, "ANCIENT" to 20.0, "LEGENDARY" to 15.0, "DRAGON" to 15.0, "SUPER_EPIC" to 8.0)
        const val SYNERGY_WEIGHT = 1.5
        const val MAX_FRONT = 2
        const val MAX_HEALERS = 1
        const val CANDIDATE_LIMIT = 40
        const val MAX_ROUNDS = 4
    }
}
