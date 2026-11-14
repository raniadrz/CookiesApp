package com.crumbcodex.crk

import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Priority
import com.crumbcodex.crk.domain.model.Stat
import com.crumbcodex.crk.domain.model.ToppingSlot
import com.crumbcodex.crk.domain.model.ToppingType
import com.crumbcodex.crk.domain.usecase.CheckStatus
import com.crumbcodex.crk.domain.usecase.CompareBuildUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class CompareBuildUseCaseTest {
    private val compare = CompareBuildUseCase()
    private val c = cookie("x", candyPriority = Priority.HIGH, builds = listOf(arenaBuild()))

    @Test fun `fresh cookie meets nothing`() {
        val r = compare(c, arenaBuild(), OwnedCookie.newFor("x", 0))
        assertEquals(0, r.met)
        assertEquals(5, r.total) // toppings, CD, ATK, beascuit, candy
    }

    @Test fun `fully built cookie meets everything`() {
        val owned = OwnedCookie.newFor("x", 0).copy(
            toppings = listOf(ToppingSlot(ToppingType.SWIFT_CHOCOLATE, 5)),
            currentStats = mapOf(Stat.COOLDOWN to 26.0, Stat.ATK to 12.0),
            beascuitGrade = BeascuitGrade.LEGENDARY,
            beascuitElement = Element.FIRE,
            candyLevel = 30,
            checklist = mapOf("Reroll" to true),
        )
        val r = compare(c, arenaBuild(), owned)
        assertEquals(r.total, r.met)
        assertEquals(1f, r.overall, 0.001f)
    }

    @Test fun `partial stats are partial`() {
        val owned = OwnedCookie.newFor("x", 0).copy(
            toppings = listOf(ToppingSlot(ToppingType.SWIFT_CHOCOLATE, 3), ToppingSlot(ToppingType.SEARING_RASPBERRY, 2)),
            currentStats = mapOf(Stat.COOLDOWN to 12.5),
        )
        val r = compare(c, arenaBuild(), owned)
        val toppings = r.items.first { it.label == "Toppings" }
        assertEquals(CheckStatus.PARTIAL, toppings.status)
        assertEquals(0.6f, toppings.progress, 0.001f)
        assertEquals(0.5f, r.items.first { it.label == Stat.COOLDOWN.label }.progress, 0.001f)
    }
}
