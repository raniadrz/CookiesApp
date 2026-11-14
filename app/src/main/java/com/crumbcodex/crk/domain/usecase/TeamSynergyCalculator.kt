package com.crumbcodex.crk.domain.usecase

import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.SynergyTag
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.Treasure

enum class Severity { INFO, WARNING, ERROR }

data class SynergyMessage(val severity: Severity, val text: String)

data class TeamSynergy(
    val filledSlots: Int,
    val front: Int,
    val middle: Int,
    val rear: Int,
    val healers: Int,
    val supports: Int,
    val defenders: Int,
    val damageDealers: Int,
    val elements: Map<Element, Int>,
    val dominantElement: Element?,
    val neededTags: List<SynergyTag>,
    val recommendedTreasures: List<Treasure>,
    val messages: List<SynergyMessage>,
    val score: Int,
) {
    val isValid: Boolean get() = messages.none { it.severity == Severity.ERROR }
}

/**
 * Analyses a 5-cookie team: position balance, role coverage, elemental coverage and
 * treasure suggestions. Pure Kotlin — no Android dependencies, fully unit-tested.
 */
class TeamSynergyCalculator {

    operator fun invoke(
        mode: GameMode,
        members: List<Cookie?>,
        treasures: List<Treasure>,
    ): TeamSynergy {
        val cookies = members.filterNotNull()
        val filled = cookies.size
        val front = cookies.count { it.position == Position.FRONT }
        val middle = cookies.count { it.position == Position.MIDDLE }
        val rear = cookies.count { it.position == Position.REAR }
        val healers = cookies.count { it.cookieClass == CookieClass.HEALING }
        val supports = cookies.count { it.cookieClass == CookieClass.SUPPORT }
        val defenders = cookies.count { it.cookieClass == CookieClass.DEFENSE }
        val dps = cookies.count { it.cookieClass.isDamageDealer }
        val elements = cookies.flatMap { it.elements }.groupingBy { it }.eachCount()
            .toList().sortedByDescending { it.second }.toMap()
        val dominant = elements.entries.firstOrNull()?.takeIf { it.value >= 2 }?.key

        val messages = mutableListOf<SynergyMessage>()
        var score = 100

        val duplicates = cookies.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        if (duplicates.isNotEmpty()) {
            messages += SynergyMessage(Severity.ERROR, "The same cookie can only be deployed once per team.")
            score -= 30
        }
        if (filled < Team.SIZE) {
            messages += SynergyMessage(Severity.WARNING, "${Team.SIZE - filled} empty slot(s) — fill all ${Team.SIZE} for full power.")
            score -= (Team.SIZE - filled) * 12
        }
        if (filled > 0 && front == 0) {
            messages += SynergyMessage(Severity.ERROR, "No Front cookie: your middle/rear line will take all the damage.")
            score -= 20
        }
        if (front >= 4) {
            messages += SynergyMessage(Severity.WARNING, "Very heavy frontline ($front) — damage output may be too low.")
            score -= 10
        }
        if (rear == 0 && filled >= 3) {
            messages += SynergyMessage(Severity.INFO, "No Rear cookie — consider a healer or ranged damage dealer in the back.")
            score -= 4
        }
        if (healers == 0 && filled > 0 && mode != GameMode.GUILD_BOSS) {
            val sev = if (mode == GameMode.WORLD_EXPLORATION) Severity.WARNING else Severity.INFO
            messages += SynergyMessage(sev, "No Healing cookie — long fights will be hard to sustain.")
            score -= if (sev == Severity.WARNING) 15 else 6
        }
        if (healers >= 3) {
            messages += SynergyMessage(Severity.WARNING, "$healers healers is overkill; swap one for damage.")
            score -= 10
        }
        if (filled >= 3 && dps < 2) {
            messages += SynergyMessage(Severity.WARNING, "Only $dps damage dealer(s) — fights may time out.")
            score -= 15
        }
        if (mode == GameMode.GUILD_BOSS && dominant != null && (elements[dominant] ?: 0) >= 3) {
            messages += SynergyMessage(Severity.INFO, "Elemental stack: ${dominant.label} ×${elements[dominant]} — pair with ${dominant.label} DMG Beascuits.")
            score += 5
        }
        if (mode == GameMode.ARENA && supports + healers == 0 && filled >= 4) {
            messages += SynergyMessage(Severity.INFO, "Pure-damage Arena team: strong burst but fragile against control comps.")
        }
        if (filled == Team.SIZE && messages.none { it.severity != Severity.INFO }) {
            messages += SynergyMessage(Severity.INFO, "Balanced composition — looks battle-ready!")
        }

        val needed = neededTags(mode, healers, front, defenders, dps)
        val recommended = treasures
            .map { it to scoreTreasure(it, mode, needed) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Treasure, Int>> { it.second }.thenBy { it.first.name })
            .take(Team.MAX_TREASURES)
            .map { it.first }

        return TeamSynergy(
            filledSlots = filled,
            front = front, middle = middle, rear = rear,
            healers = healers, supports = supports, defenders = defenders, damageDealers = dps,
            elements = elements, dominantElement = dominant,
            neededTags = needed,
            recommendedTreasures = recommended,
            messages = messages,
            score = score.coerceIn(0, 100),
        )
    }

    private fun neededTags(mode: GameMode, healers: Int, front: Int, defenders: Int, dps: Int): List<SynergyTag> {
        val tags = linkedSetOf<SynergyTag>()
        when (mode) {
            GameMode.ARENA -> { tags += SynergyTag.COOLDOWN; tags += SynergyTag.REVIVE }
            GameMode.WORLD_EXPLORATION -> { tags += SynergyTag.SUSTAIN; tags += SynergyTag.COOLDOWN }
            GameMode.GUILD_BOSS -> { tags += SynergyTag.BURST; tags += SynergyTag.CRIT; tags += SynergyTag.COOLDOWN }
        }
        if (healers == 0) tags += SynergyTag.SUSTAIN
        if (front == 0 || defenders == 0) { tags += SynergyTag.TANK; tags += SynergyTag.SHIELD }
        if (dps >= 3) { tags += SynergyTag.BURST; tags += SynergyTag.CRIT }
        return tags.toList()
    }

    private fun scoreTreasure(t: Treasure, mode: GameMode, needed: List<SynergyTag>): Int {
        var s = 0
        if (mode in t.modes) s += 3
        t.tags.forEach { tag ->
            val idx = needed.indexOf(tag)
            if (idx >= 0) s += (needed.size - idx) + 2
        }
        return if (t.tags.none { it in needed }) 0 else s
    }
}
