package com.crumbcodex.crk.ui.dex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.CookieFilter
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.usecase.CookieSort
import com.crumbcodex.crk.domain.usecase.FilterCookiesUseCase
import com.crumbcodex.crk.ui.util.containerFactory
import com.crumbcodex.crk.ui.util.toggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DexUiState(
    val loading: Boolean = true,
    val cookies: List<Cookie> = emptyList(),
    val total: Int = 0,
    val ownedCount: Int = 0,
    val filter: CookieFilter = CookieFilter(),
    val sort: CookieSort = CookieSort.RARITY,
)

class DexViewModel(
    cookieRepository: CookieRepository,
    private val collectionRepository: CollectionRepository,
    private val filterCookies: FilterCookiesUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(CookieFilter())
    private val sort = MutableStateFlow(CookieSort.RARITY)

    val state: StateFlow<DexUiState> =
        combine(cookieRepository.observeCookies(), filter, sort) { cookies, f, s ->
            DexUiState(
                loading = false,
                cookies = filterCookies(cookies, f, s),
                total = cookies.size,
                ownedCount = cookies.count { it.isOwned },
                filter = f,
                sort = s,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DexUiState())

    fun onQueryChange(q: String) = filter.update { it.copy(query = q) }
    fun toggleRarity(r: Rarity) = filter.update { it.copy(rarities = it.rarities.toggle(r)) }
    fun toggleClass(c: CookieClass) = filter.update { it.copy(classes = it.classes.toggle(c)) }
    fun togglePosition(p: Position) = filter.update { it.copy(positions = it.positions.toggle(p)) }
    fun toggleElement(e: Element) = filter.update { it.copy(elements = it.elements.toggle(e)) }
    fun toggleOwnedOnly() = filter.update { it.copy(ownedOnly = !it.ownedOnly) }
    fun clearFilters() = filter.update { CookieFilter(query = it.query) }
    fun setSort(s: CookieSort) { sort.value = s }

    fun setOwned(cookieId: String, owned: Boolean) {
        viewModelScope.launch { collectionRepository.setOwned(cookieId, owned) }
    }

    companion object {
        val Factory = containerFactory { DexViewModel(it.cookieRepository, it.collectionRepository, it.filterCookies) }
    }
}
