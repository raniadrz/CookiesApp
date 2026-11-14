package com.crumbcodex.crk.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.repository.GameDataRepository
import com.crumbcodex.crk.domain.repository.TeamRepository
import com.crumbcodex.crk.domain.usecase.TeamSynergy
import com.crumbcodex.crk.domain.usecase.TeamSynergyCalculator
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeamSummary(val team: Team, val members: List<Cookie?>, val synergy: TeamSynergy)

data class TeamListUiState(
    val loading: Boolean = true,
    val teams: List<TeamSummary> = emptyList(),
    val modeFilter: GameMode? = null,
)

class TeamListViewModel(
    private val teamRepository: TeamRepository,
    cookieRepository: CookieRepository,
    gameDataRepository: GameDataRepository,
    private val synergy: TeamSynergyCalculator,
) : ViewModel() {

    private val modeFilter = MutableStateFlow<GameMode?>(null)

    val state: StateFlow<TeamListUiState> = combine(
        teamRepository.observeTeams(),
        cookieRepository.observeCookies(),
        gameDataRepository.observeTreasures(),
        modeFilter,
    ) { teams, cookies, treasures, filter ->
        val byId = cookies.associateBy { it.id }
        TeamListUiState(
            loading = false,
            teams = teams.filter { filter == null || it.mode == filter }.map { t ->
                val members = t.slots.map { id -> id?.let(byId::get) }
                TeamSummary(t, members, synergy(t.mode, members, treasures))
            },
            modeFilter = filter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamListUiState())

    fun setFilter(mode: GameMode?) { modeFilter.value = mode }
    fun delete(id: Long) { viewModelScope.launch { teamRepository.delete(id) } }

    companion object {
        val Factory = containerFactory {
            TeamListViewModel(it.teamRepository, it.cookieRepository, it.gameDataRepository, it.teamSynergy)
        }
    }
}
