package com.crumbcodex.crk

import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.SynergyTag
import com.crumbcodex.crk.domain.model.Treasure
import com.crumbcodex.crk.domain.usecase.Severity
import com.crumbcodex.crk.domain.usecase.TeamSynergyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamSynergyCalculatorTest {
    private val calc = TeamSynergyCalculator()
    private val treasures = listOf(
        Treasure("watch", "Watch", "", listOf(SynergyTag.COOLDOWN), GameMode.entries),
        Treasure("scroll", "Scroll", "", listOf(SynergyTag.BURST), listOf(GameMode.GUILD_BOSS)),
        Treasure("monocle", "Monocle", "", listOf(SynergyTag.CRIT), listOf(GameMode.GUILD_BOSS)),
        Treasure("insignia", "Insignia", "", listOf(SynergyTag.TANK, SynergyTag.SHIELD), listOf(GameMode.ARENA)),
    )

    private val balanced = listOf(
        cookie("tank", CookieClass.DEFENSE, Position.FRONT, elements = listOf(Element.EARTH)),
        cookie("charger", CookieClass.CHARGE, Position.FRONT),
        cookie("mage", CookieClass.MAGIC, Position.MIDDLE),
        cookie("archer", CookieClass.RANGED, Position.REAR, elements = listOf(Element.WIND)),
        cookie("healer", CookieClass.HEALING, Position.REAR, elements = listOf(Element.LIGHT)),
    )

    @Test fun `balanced team counts positions and roles`() {
        val s = calc(GameMode.ARENA, balanced, treasures)
        assertEquals(5, s.filledSlots)
        assertEquals(2, s.front); assertEquals(1, s.middle); assertEquals(2, s.rear)
        assertEquals(1, s.healers); assertEquals(1, s.defenders); assertEquals(3, s.damageDealers)
        assertTrue(s.isValid)
        assertEquals(2, s.elements[Element.FIRE])
        assertTrue(s.score >= 90)
    }

    @Test fun `no front cookie is an error`() {
        val team = balanced.filter { it.position != Position.FRONT }
        val s = calc(GameMode.ARENA, team + listOf(null, null), treasures)
        assertFalse(s.isValid)
        assertTrue(s.messages.any { it.severity == Severity.ERROR && "Front" in it.text })
    }

    @Test fun `duplicate cookie is an error`() {
        val team = listOf(balanced[0], balanced[0], balanced[2], balanced[3], balanced[4])
        assertFalse(calc(GameMode.ARENA, team, treasures).isValid)
    }

    @Test fun `empty slots reduce score and warn`() {
        val s = calc(GameMode.ARENA, balanced.take(3) + listOf(null, null), treasures)
        assertTrue(s.score < 90)
        assertTrue(s.messages.any { "empty slot" in it.text })
    }

    @Test fun `guild boss suggests burst and crit treasures`() {
        val s = calc(GameMode.GUILD_BOSS, balanced, treasures)
        val ids = s.recommendedTreasures.map { it.id }
        assertTrue("scroll" in ids)
        assertTrue("monocle" in ids)
        assertTrue(s.recommendedTreasures.size <= 3)
    }

    @Test fun `guild boss detects elemental stack`() {
        val fire = List(5) { i -> cookie("f$i", if (i == 0) CookieClass.DEFENSE else CookieClass.MAGIC, if (i == 0) Position.FRONT else Position.MIDDLE) }
        val s = calc(GameMode.GUILD_BOSS, fire, treasures)
        assertEquals(Element.FIRE, s.dominantElement)
        assertTrue(s.messages.any { "Elemental stack" in it.text })
    }

    @Test fun `pve without healer warns`() {
        val team = balanced.filter { it.cookieClass != CookieClass.HEALING } + null
        val s = calc(GameMode.WORLD_EXPLORATION, team, treasures)
        assertTrue(s.messages.any { it.severity == Severity.WARNING && "Healing" in it.text })
        assertTrue(SynergyTag.SUSTAIN in s.neededTags)
    }
}
