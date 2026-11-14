package com.crumbcodex.crk.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookieWithInfo
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.usecase.CompareBuildUseCase
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class CollectionSort(val label: String) { POWER("Power"), LEVEL("Level"), RECENT("Recently updated"), READINESS("Build readiness") }

data class CollectionEntry(
    val info: OwnedCookieWithInfo,
    /** Build readiness 0..1 per mode that has a recommended build. */
    val readiness: Map<GameMode, Float>,
)

data class CollectionUiState(
    val loading: Boolean = true,
    val entries: List<CollectionEntry> = emptyList(),
    val totalCookies: Int = 0,
    val totalPower: Long = 0,
    val averageLevel: Int = 0,
    val sort: CollectionSort = CollectionSort.POWER,
)

class CollectionViewModel(
    cookieRepository: CookieRepository,
    collectionRepository: CollectionRepository,
    private val compareBuild: CompareBuildUseCase,
) : ViewModel() {

    private val sort = MutableStateFlow(CollectionSort.POWER)

    val state: StateFlow<CollectionUiState> = combine(
        cookieRepository.observeCookies(),
        collectionRepository.observeOwned(),
        sort,
    ) { cookies, owned, s ->
        val byId = cookies.associateBy { it.id }
        val entries = owned.mapNotNull { o ->
            val cookie = byId[o.cookieId] ?: return@mapNotNull null
            val readiness = cookie.builds.associate { b -> b.mode to compareBuild(cookie, b, o).overall }
            CollectionEntry(OwnedCookieWithInfo(o, cookie), readiness)
        }
        val sorted = when (s) {
            CollectionSort.POWER -> entries.sortedByDescending { it.info.owned.power }
            CollectionSort.LEVEL -> entries.sortedByDescending { it.info.owned.level }
            CollectionSort.RECENT -> entries.sortedByDescending { it.info.owned.updatedAt }
            CollectionSort.READINESS -> entries.sortedByDescending { e -> e.readiness.values.maxOrNull() ?: 0f }
        }
        CollectionUiState(
            loading = false,
            entries = sorted,
            totalCookies = cookies.size,
            totalPower = entries.sumOf { it.info.owned.power },
            averageLevel = if (entries.isEmpty()) 0 else entries.map { it.info.owned.level }.average().toInt(),
            sort = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())

    fun setSort(s: CollectionSort) { sort.value = s }

    companion object {
        val Factory = containerFactory { CollectionViewModel(it.cookieRepository, it.collectionRepository, it.compareBuild) }
    }
}
