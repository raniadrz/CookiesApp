package com.crumbcodex.crk.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.Treasure
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.repository.GameDataRepository
import com.crumbcodex.crk.domain.repository.TeamRepository
import com.crumbcodex.crk.domain.usecase.SuggestTeamUseCase
import com.crumbcodex.crk.domain.usecase.TeamSuggestion
import com.crumbcodex.crk.domain.usecase.TeamSynergy
import com.crumbcodex.crk.domain.usecase.TeamSynergyCalculator
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamEditorUiState(
    val loading: Boolean = true,
    val team: Team,
    val cookies: List<Cookie> = emptyList(),
    val treasures: List<Treasure> = emptyList(),
    val synergy: TeamSynergy? = null,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val suggesting: Boolean = false,
    val lastSuggestion: TeamSuggestion? = null,
) {
    private val byId get() = cookies.associateBy { it.id }
    val members: List<Cookie?> get() = team.slots.map { id -> id?.let { byId[it] } }
    val isNew: Boolean get() = team.id == 0L
}

class TeamEditorViewModel(
    teamId: Long,
    initialMode: GameMode,
    private val teamRepository: TeamRepository,
    cookieRepository: CookieRepository,
    private val gameDataRepository: GameDataRepository,
    private val calculator: TeamSynergyCalculator,
    private val collectionRepository: CollectionRepository,
    private val suggestTeam: SuggestTeamUseCase,
    autoSuggest: Boolean = false,
) : ViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _state = MutableStateFlow(TeamEditorUiState(team = Team.empty(initialMode)))
    val state: StateFlow<TeamEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (teamId != 0L) {
                teamRepository.observeTeam(teamId).first()?.let { t -> _state.update { it.copy(team = t, saved = true) } }
            }
            combine(cookieRepository.observeCookies(), gameDataRepository.observeTreasures()) { c, t -> c to t }
                .collect { (cookies, treasures) ->
                    val first = _state.value.loading
                    _state.update { it.copy(loading = false, cookies = cookies, treasures = treasures).recompute() }
                    if (first && autoSuggest && teamId == 0L) autoBuild(ownedOnly = true)
                }
        }
    }

    /**
     * Fills all 5 slots with the strongest lineup for the current mode, built from the user's collection
     * ([ownedOnly]) or from every cookie. Picks treasures for the new lineup too.
     */
    fun autoBuild(ownedOnly: Boolean) {
        val current = _state.value
        if (current.cookies.isEmpty() || current.suggesting) return
        _state.update { it.copy(suggesting = true) }
        viewModelScope.launch {
            val mode = current.team.mode
            val owned = collectionRepository.observeOwned().first()
            val tiers = gameDataRepository.observeTierList(mode).first()
            val suggestion = suggestTeam(mode, current.cookies, owned, tiers, current.treasures, ownedOnly)
            if (suggestion == null) {
                _state.update { it.copy(suggesting = false) }
                _messages.send("Not enough cookies to build a team")
                return@launch
            }
            editTeam { t ->
                t.copy(
                    slots = suggestion.members.map { it.cookie.id },
                    treasureIds = suggestion.synergy.recommendedTreasures.map { it.id }.take(Team.MAX_TREASURES),
                    name = t.name.ifBlank { "Suggested ${mode.shortLabel} team" },
                )
            }
            _state.update { it.copy(suggesting = false, lastSuggestion = suggestion) }
            _messages.send(
                when {
                    !suggestion.usedUnowned -> "Built from your collection · synergy ${suggestion.synergy.score}"
                    owned.isEmpty() -> "Your collection is empty, so this uses any cookie · synergy ${suggestion.synergy.score}"
                    else -> "You own fewer than 5 suitable cookies, so some slots use cookies you don't have"
                },
            )
        }
    }

    private fun TeamEditorUiState.recompute() = copy(synergy = calculator(team.mode, members, treasures))

    private fun editTeam(block: (Team) -> Team) =
        _state.update { it.copy(team = block(it.team), dirty = true).recompute() }

    fun setName(name: String) = editTeam { it.copy(name = name.take(40)) }
    fun setMode(mode: GameMode) = editTeam { it.copy(mode = mode) }
    fun setNotes(notes: String) = editTeam { it.copy(notes = notes.take(1000)) }

    /** Places [cookieId] in [slot]. If it is already elsewhere in the team, the two slots swap. */
    fun assign(slot: Int, cookieId: String) = editTeam { t ->
        val slots = t.slots.toMutableList()
        val existing = slots.indexOf(cookieId)
        if (existing >= 0 && existing != slot) slots[existing] = slots[slot]
        slots[slot] = cookieId
        t.copy(slots = slots)
    }

    fun clear(slot: Int) = editTeam { t -> t.copy(slots = t.slots.toMutableList().also { it[slot] = null }) }

    /** Orders the lineup Front → Middle → Rear, empty slots last. */
    fun sortByPosition() {
        val byId = _state.value.cookies.associateBy { it.id }
        editTeam { t ->
            val filled = t.slots.filterNotNull().sortedWith(
                compareBy({ byId[it]?.position?.sortOrder ?: 9 }, { byId[it]?.name ?: it }),
            )
            t.copy(slots = filled + List(Team.SIZE - filled.size) { null })
        }
    }

    fun toggleTreasure(id: String) = editTeam { t ->
        when {
            id in t.treasureIds -> t.copy(treasureIds = t.treasureIds - id)
            t.treasureIds.size >= Team.MAX_TREASURES -> t
            else -> t.copy(treasureIds = t.treasureIds + id)
        }
    }

    fun applyRecommendedTreasures() {
        val rec = _state.value.synergy?.recommendedTreasures.orEmpty().map { it.id }
        if (rec.isNotEmpty()) editTeam { it.copy(treasureIds = rec.take(Team.MAX_TREASURES)) }
    }

    fun save(onDone: () -> Unit = {}) {
        val current = _state.value
        val team = current.team.let { if (it.name.isBlank()) it.copy(name = "${it.mode.shortLabel} team") else it }
        viewModelScope.launch {
            val id = teamRepository.save(team)
            _state.update { it.copy(team = team.copy(id = id), dirty = false, saved = true).recompute() }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _state.value.team.id
        viewModelScope.launch {
            if (id != 0L) teamRepository.delete(id)
            onDone()
        }
    }

    companion object {
        fun factory(teamId: Long, mode: GameMode, autoSuggest: Boolean = false): ViewModelProvider.Factory = containerFactory {
            TeamEditorViewModel(
                teamId, mode, it.teamRepository, it.cookieRepository, it.gameDataRepository, it.teamSynergy,
                it.collectionRepository, it.suggestTeam, autoSuggest,
            )
        }
    }
}
