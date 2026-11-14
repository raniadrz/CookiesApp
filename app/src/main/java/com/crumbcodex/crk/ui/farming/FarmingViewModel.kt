package com.crumbcodex.crk.ui.farming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.DropCategory
import com.crumbcodex.crk.domain.model.DropItem
import com.crumbcodex.crk.domain.model.DropSourceType
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.repository.GameDataRepository
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class FarmingFilter(
    val query: String = "",
    val category: DropCategory? = null,
    val element: Element? = null,
    val sourceType: DropSourceType? = null,
)

data class FarmingUiState(
    val loading: Boolean = true,
    val items: List<DropItem> = emptyList(),
    val filter: FarmingFilter = FarmingFilter(),
    val availableElements: List<Element> = emptyList(),
)

class FarmingViewModel(gameDataRepository: GameDataRepository) : ViewModel() {
    private val filter = MutableStateFlow(FarmingFilter())

    val state: StateFlow<FarmingUiState> = combine(gameDataRepository.observeDrops(), filter) { drops, f ->
        val q = f.query.trim()
        FarmingUiState(
            loading = false,
            filter = f,
            availableElements = drops.mapNotNull { it.element }.distinct().sortedBy { it.ordinal },
            items = drops.filter { d ->
                (f.category == null || d.category == f.category) &&
                    (f.element == null || d.element == f.element) &&
                    (f.sourceType == null || d.sources.any { it.type == f.sourceType }) &&
                    (q.isEmpty() || d.name.contains(q, true) || d.description.contains(q, true) ||
                        d.sources.any { it.name.contains(q, true) || it.detail.contains(q, true) })
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmingUiState())

    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setCategory(c: DropCategory?) = filter.update { it.copy(category = c) }
    fun setElement(e: Element?) = filter.update { it.copy(element = if (it.element == e) null else e) }
    fun setSourceType(t: DropSourceType?) = filter.update { it.copy(sourceType = if (it.sourceType == t) null else t) }

    companion object {
        val Factory = containerFactory { FarmingViewModel(it.gameDataRepository) }
    }
}
