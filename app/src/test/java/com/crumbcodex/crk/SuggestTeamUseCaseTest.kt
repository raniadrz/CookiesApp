package com.crumbcodex.crk

import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.model.TierEntry
import com.crumbcodex.crk.domain.usecase.SuggestTeamUseCase
import com.crumbcodex.crk.domain.usecase.TeamSynergyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestTeamUseCaseTest {
    private val suggest = SuggestTeamUseCase(TeamSynergyCalculator())

    private val pool = listOf(
        cookie("tank", CookieClass.DEFENSE, Position.FRONT),
        cookie("charger", CookieClass.CHARGE, Position.FRONT),
        cookie("bruiser", CookieClass.CHARGE, Position.FRONT),
        cookie("mage", CookieClass.MAGIC, Position.MIDDLE),
        cookie("assassin", CookieClass.AMBUSH, Position.MIDDLE),
        cookie("archer", CookieClass.RANGED, Position.REAR),
        cookie("healer", CookieClass.HEALING, Position.REAR),
        cookie("healer2", CookieClass.HEALING, Position.REAR),
        cookie("weak", CookieClass.MAGIC, Position.MIDDLE, Rarity.COMMON),
    )
    private fun owned(vararg ids: String) = ids.map { OwnedCookie.newFor(it, 0) }
    private fun tier(id: String, t: String, mode: GameMode = GameMode.ARENA) =
        TierEntry(mode, t, 0, "", pool.first { it.id == id })

    @Test fun `builds a valid team from owned cookies`() {
        val s = suggest(GameMode.ARENA, pool, owned(*pool.map { it.id }.toTypedArray()), emptyList(), emptyList())!!
        assertEquals(5, s.members.size)
        assertEquals(5, s.members.map { it.cookie.id }.toSet().size)
        assertTrue(s.synergy.front >= 1)
        assertTrue(s.synergy.front <= 3)
        assertEquals(1, s.synergy.healers)
        assertTrue(s.synergy.isValid)
        assertFalse(s.usedUnowned)
    }

    @Test fun `members are ordered front to rear`() {
        val s = suggest(GameMode.ARENA, pool, owned(*pool.map { it.id }.toTypedArray()), emptyList(), emptyList())!!
        val order = s.members.map { it.cookie.position.sortOrder }
        assertEquals(order.sorted(), order)
    }

    @Test fun `prefers higher tier cookies`() {
        val tiers = listOf(tier("assassin", "S+"), tier("mage", "C"), tier("weak", "S"))
        val s = suggest(GameMode.ARENA, pool, owned(*pool.map { it.id }.toTypedArray()), tiers, emptyList())!!
        val ids = s.members.map { it.cookie.id }
        assertTrue("assassin" in ids)
        assertTrue("weak" in ids)
        assertTrue(s.members.first { it.cookie.id == "assassin" }.reason.contains("S+ tier"))
    }

    @Test fun `fills with unowned cookies when collection is too small`() {
        val s = suggest(GameMode.WORLD_EXPLORATION, pool, owned("tank", "mage"), emptyList(), emptyList())!!
        assertEquals(5, s.members.size)
        assertTrue(s.usedUnowned)
        assertTrue(s.members.map { it.cookie.id }.containsAll(listOf("tank", "mage")))
    }

    @Test fun `returns null when fewer than five cookies exist`() {
        assertNull(suggest(GameMode.ARENA, pool.take(3), owned(), emptyList(), emptyList()))
    }
}
