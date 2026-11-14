package com.crumbcodex.crk.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.usecase.BuildComparison
import com.crumbcodex.crk.domain.usecase.CompareBuildUseCase
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val cookie: Cookie? = null,
    val owned: OwnedCookie? = null,
    val mode: GameMode = GameMode.ARENA,
    val comparison: BuildComparison? = null,
)

class CookieDetailViewModel(
    private val cookieId: String,
    cookieRepository: CookieRepository,
    private val collectionRepository: CollectionRepository,
    private val compareBuild: CompareBuildUseCase,
) : ViewModel() {

    private val mode = MutableStateFlow(GameMode.ARENA)

    val state: StateFlow<DetailUiState> = combine(
        cookieRepository.observeCookie(cookieId),
        collectionRepository.observeOwned(cookieId),
        mode,
    ) { cookie, owned, m ->
        val build = cookie?.buildFor(m)
        DetailUiState(
            loading = false,
            cookie = cookie,
            owned = owned,
            mode = m,
            comparison = if (cookie != null && build != null && owned != null) compareBuild(cookie, build, owned) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun selectMode(m: GameMode) { mode.value = m }

    fun toggleOwned() {
        val owned = state.value.owned != null
        viewModelScope.launch { collectionRepository.setOwned(cookieId, !owned) }
    }

    companion object {
        fun factory(cookieId: String): ViewModelProvider.Factory = containerFactory {
            CookieDetailViewModel(cookieId, it.cookieRepository, it.collectionRepository, it.compareBuild)
        }
    }
}
