package com.crumbcodex.crk.domain.usecase

import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieFilter

enum class CookieSort(val label: String) { RARITY("Rarity"), NAME("Name"), POSITION("Position") }

/** Pure filtering/sorting logic for the Cookie-Dex. */
class FilterCookiesUseCase {
    operator fun invoke(cookies: List<Cookie>, filter: CookieFilter, sort: CookieSort = CookieSort.RARITY): List<Cookie> {
        val tokens = filter.query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return cookies.asSequence()
            .filter { filter.rarities.isEmpty() || it.rarity in filter.rarities }
            .filter { filter.classes.isEmpty() || it.cookieClass in filter.classes }
            .filter { filter.positions.isEmpty() || it.position in filter.positions }
            .filter { filter.elements.isEmpty() || it.elements.any { e -> e in filter.elements } }
            .filter { !filter.ownedOnly || it.isOwned }
            .filter { cookie ->
                if (tokens.isEmpty()) return@filter true
                val haystack = buildString {
                    append(cookie.name.lowercase()); append(' ')
                    append(cookie.title.lowercase()); append(' ')
                    append(cookie.rarity.label.lowercase()); append(' ')
                    append(cookie.cookieClass.label.lowercase()); append(' ')
                    append(cookie.position.label.lowercase()); append(' ')
                    cookie.elements.forEach { append(it.label.lowercase()); append(' ') }
                    append(cookie.skill.name.lowercase())
                }
                tokens.all { it in haystack }
            }
            .sortedWith(
                when (sort) {
                    CookieSort.RARITY -> compareBy<Cookie>({ it.rarity.sortOrder }, { it.name })
                    CookieSort.NAME -> compareBy { it.name }
                    CookieSort.POSITION -> compareBy<Cookie>({ it.position.sortOrder }, { it.rarity.sortOrder }, { it.name })
                },
            )
            .toList()
    }
}
