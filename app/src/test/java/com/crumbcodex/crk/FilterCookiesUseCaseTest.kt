package com.crumbcodex.crk

import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.CookieFilter
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.usecase.CookieSort
import com.crumbcodex.crk.domain.usecase.FilterCookiesUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterCookiesUseCaseTest {
    private val filter = FilterCookiesUseCase()
    private val all = listOf(
        cookie("alpha", CookieClass.MAGIC, Position.MIDDLE, Rarity.BEAST, listOf(Element.DARKNESS), owned = true),
        cookie("bravo", CookieClass.CHARGE, Position.FRONT, Rarity.ANCIENT, listOf(Element.FIRE)),
        cookie("charlie", CookieClass.HEALING, Position.REAR, Rarity.EPIC, listOf(Element.LIGHT, Element.WATER)),
    )

    @Test fun `empty filter returns all sorted by rarity`() {
        assertEquals(listOf("alpha", "bravo", "charlie"), filter(all, CookieFilter()).map { it.id })
    }

    @Test fun `query matches name and attributes`() {
        assertEquals(listOf("bravo"), filter(all, CookieFilter(query = "charge")).map { it.id })
        assertEquals(listOf("charlie"), filter(all, CookieFilter(query = "char rear")).map { it.id })
    }

    @Test fun `element filter matches any element`() {
        assertEquals(listOf("charlie"), filter(all, CookieFilter(elements = setOf(Element.WATER))).map { it.id })
    }

    @Test fun `filters combine with AND across groups and OR within`() {
        val f = CookieFilter(rarities = setOf(Rarity.BEAST, Rarity.ANCIENT), positions = setOf(Position.FRONT))
        assertEquals(listOf("bravo"), filter(all, f).map { it.id })
    }

    @Test fun `owned only`() {
        assertEquals(listOf("alpha"), filter(all, CookieFilter(ownedOnly = true)).map { it.id })
    }

    @Test fun `position sort`() {
        assertEquals(listOf("bravo", "alpha", "charlie"), filter(all, CookieFilter(), CookieSort.POSITION).map { it.id })
    }
}
