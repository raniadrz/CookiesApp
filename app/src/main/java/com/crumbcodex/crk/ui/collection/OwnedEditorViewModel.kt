package com.crumbcodex.crk.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Stat
import com.crumbcodex.crk.domain.model.ToppingSlot
import com.crumbcodex.crk.domain.model.ToppingType
import com.crumbcodex.crk.domain.model.formatStat
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.usecase.BuildComparison
import com.crumbcodex.crk.domain.usecase.CompareBuildUseCase
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val loading: Boolean = true,
    val cookie: Cookie? = null,
    val isPersisted: Boolean = false,
    val draft: OwnedCookie? = null,
    val dirty: Boolean = false,
    val compareMode: GameMode = GameMode.ARENA,
    val comparison: BuildComparison? = null,
    val statInputs: Map<Stat, String> = emptyMap(),
    val powerInput: String = "",
    val trackedStats: List<Stat> = emptyList(),
) {
    val toppingTotal: Int get() = draft?.toppings?.sumOf { it.count } ?: 0
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Removed : EditorEvent
}

class OwnedEditorViewModel(
    private val cookieId: String,
    cookieRepository: CookieRepository,
    private val collectionRepository: CollectionRepository,
    private val compareBuild: CompareBuildUseCase,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(cookieRepository.observeCookie(cookieId), collectionRepository.observeOwned(cookieId)) { c, o -> c to o }
                .collect { (cookie, owned) ->
                    _state.update { s ->
                        val firstLoad = s.draft == null
                        val draft = if (firstLoad || !s.dirty) owned ?: s.draft ?: OwnedCookie.newFor(cookieId, clock()) else s.draft
                        val tracked = cookie?.let { trackedStatsFor(it) } ?: DEFAULT_STATS
                        val mode = if (firstLoad) cookie?.builds?.firstOrNull()?.mode ?: GameMode.ARENA else s.compareMode
                        s.copy(
                            loading = false,
                            cookie = cookie,
                            isPersisted = owned != null,
                            draft = draft,
                            compareMode = mode,
                            trackedStats = tracked,
                            statInputs = if (firstLoad || !s.dirty) draft!!.currentStats.mapValues { it.value.formatStat() } else s.statInputs,
                            powerInput = if (firstLoad || !s.dirty) draft!!.power.takeIf { it > 0 }?.toString().orEmpty() else s.powerInput,
                        ).withComparison()
                    }
                }
        }
    }

    private fun EditorUiState.withComparison(): EditorUiState {
        val c = cookie ?: return copy(comparison = null)
        val d = draft ?: return copy(comparison = null)
        val build = c.buildFor(compareMode) ?: return copy(comparison = null)
        return copy(comparison = compareBuild(c, build, d))
    }

    private fun edit(block: (OwnedCookie) -> OwnedCookie) {
        _state.update { s -> s.draft?.let { s.copy(draft = block(it), dirty = true).withComparison() } ?: s }
    }

    fun setLevel(v: Int) = edit { it.copy(level = v.coerceIn(1, OwnedCookie.MAX_LEVEL)) }
    fun setStars(v: Int) = edit { it.copy(stars = v.coerceIn(0, OwnedCookie.MAX_STARS)) }
    fun setAscension(v: Int) = edit { it.copy(ascension = v.coerceIn(0, OwnedCookie.MAX_ASCENSION)) }
    fun setSkillLevel(v: Int) = edit { it.copy(skillLevel = v.coerceIn(1, OwnedCookie.MAX_SKILL_LEVEL)) }
    fun setCandyLevel(v: Int) = edit { it.copy(candyLevel = v.coerceIn(0, OwnedCookie.MAX_CANDY_LEVEL)) }
    fun setTart(v: String) = edit { it.copy(tart = v.take(60)) }
    fun setNotes(v: String) = edit { it.copy(notes = v.take(2000)) }
    fun setBeascuitGrade(g: BeascuitGrade?) = edit { it.copy(beascuitGrade = g) }
    fun setBeascuitElement(e: Element?) = edit { it.copy(beascuitElement = e) }

    fun setPower(text: String) {
        val digits = text.filter { it.isDigit() }.take(9)
        _state.update { it.copy(powerInput = digits) }
        edit { it.copy(power = digits.toLongOrNull() ?: 0L) }
    }

    /** Sets the number of pieces of [type]; the total across all types is capped at 5. */
    fun setToppingCount(type: ToppingType, count: Int) = edit { o ->
        val others = o.toppings.filter { it.type != type }
        val max = 5 - others.sumOf { it.count }
        val c = count.coerceIn(0, max)
        val list = if (c == 0) others else others + ToppingSlot(type, c)
        o.copy(toppings = list.sortedBy { it.type.ordinal })
    }

    fun applyRecommendedToppings() {
        val build = _state.value.cookie?.buildFor(_state.value.compareMode) ?: return
        edit { it.copy(toppings = build.toppings) }
    }

    fun setStatInput(stat: Stat, text: String) {
        val clean = text.replace(',', '.').filter { it.isDigit() || it == '.' }.take(7)
        _state.update { it.copy(statInputs = it.statInputs + (stat to clean)) }
        edit { o ->
            val value = clean.toDoubleOrNull()
            o.copy(currentStats = if (value == null) o.currentStats - stat else o.currentStats + (stat to value))
        }
    }

    fun setCompareMode(m: GameMode) = _state.update { it.copy(compareMode = m).withComparison() }

    fun addChecklistItem(label: String) {
        val clean = label.trim().take(80)
        if (clean.isEmpty()) return
        edit { it.copy(checklist = it.checklist + (clean to false)) }
    }

    fun toggleChecklistItem(label: String) = edit { o ->
        o.copy(checklist = o.checklist + (label to !(o.checklist[label] ?: false)))
    }

    fun removeChecklistItem(label: String) = edit { it.copy(checklist = it.checklist - label) }

    /** Persists the draft; [onSaved] runs after the write completes (e.g. to navigate away safely). */
    fun save(onSaved: (() -> Unit)? = null) {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            collectionRepository.save(draft)
            _state.update { it.copy(dirty = false, isPersisted = true) }
            if (onSaved != null) onSaved() else _events.send(EditorEvent.Saved)
        }
    }

    fun removeFromCollection() {
        viewModelScope.launch {
            collectionRepository.setOwned(cookieId, false)
            _events.send(EditorEvent.Removed)
        }
    }

    companion object {
        val DEFAULT_STATS = listOf(Stat.ATK, Stat.CRIT, Stat.COOLDOWN, Stat.DMG_RESIST, Stat.ATK_SPD, Stat.HP, Stat.DEF)

        fun trackedStatsFor(cookie: Cookie): List<Stat> {
            val fromBuilds = cookie.builds.flatMap { b -> b.substats.sortedBy { it.priority }.map { it.stat } }
            return (fromBuilds + DEFAULT_STATS).distinct()
        }

        fun factory(cookieId: String): ViewModelProvider.Factory = containerFactory {
            OwnedEditorViewModel(cookieId, it.cookieRepository, it.collectionRepository, it.compareBuild)
        }
    }
}
