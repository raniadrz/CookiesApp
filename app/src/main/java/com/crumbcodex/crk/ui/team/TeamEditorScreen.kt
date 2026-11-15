package com.crumbcodex.crk.ui.team

import androidx.activity.compose.BackHandler
import com.crumbcodex.crk.domain.usecase.TeamSuggestion
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.Treasure
import com.crumbcodex.crk.domain.usecase.Severity
import com.crumbcodex.crk.domain.usecase.TeamSynergy
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.SectionHeader
import com.crumbcodex.crk.ui.components.TagPill
import com.crumbcodex.crk.ui.components.TreasureArt
import com.crumbcodex.crk.ui.theme.StatusBad
import com.crumbcodex.crk.ui.theme.StatusGood
import com.crumbcodex.crk.ui.theme.StatusWarn
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon
import com.crumbcodex.crk.ui.util.toggle

@Composable
fun TeamEditorScreen(
    teamId: Long,
    initialMode: GameMode,
    onBack: () -> Unit,
    onOpenCookie: (String) -> Unit,
    autoSuggest: Boolean = false,
) {
    val viewModel: TeamEditorViewModel = viewModel(
        key = "team-$teamId-$initialMode-$autoSuggest",
        factory = TeamEditorViewModel.factory(teamId, initialMode, autoSuggest),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var autoMenu by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    var pickerSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New team" else "Edit team") },
                navigationIcon = {
                    IconButton(onClick = { if (state.dirty) confirmDiscard = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        if (state.suggesting) {
                            CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { autoMenu = true }) { Icon(Icons.Rounded.AutoAwesome, "Auto-build team") }
                        }
                        DropdownMenu(expanded = autoMenu, onDismissRequest = { autoMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Auto-build from my cookies") },
                                leadingIcon = { Icon(Icons.Rounded.Favorite, null) },
                                onClick = { autoMenu = false; viewModel.autoBuild(ownedOnly = true) },
                            )
                            DropdownMenuItem(
                                text = { Text("Auto-build from any cookie") },
                                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null) },
                                onClick = { autoMenu = false; viewModel.autoBuild(ownedOnly = false) },
                            )
                        }
                    }
                    if (!state.isNew) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete team") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (state.dirty || state.isNew) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.save() },
                    icon = { Icon(Icons.Rounded.Save, null) },
                    text = { Text("Save team") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.team.name,
                        onValueChange = viewModel::setName,
                        label = { Text("Team name") },
                        placeholder = { Text("${state.team.mode.shortLabel} team") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        GameMode.entries.forEachIndexed { i, mode ->
                            SegmentedButton(
                                selected = state.team.mode == mode,
                                onClick = { viewModel.setMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(i, GameMode.entries.size),
                                icon = { Icon(mode.icon, null, Modifier.size(18.dp)) },
                            ) { Text(mode.shortLabel) }
                        }
                    }
                }
            }
            item {
                LineupCard(
                    members = state.members,
                    onSlotClick = { pickerSlot = it },
                    onClear = viewModel::clear,
                    onSort = viewModel::sortByPosition,
                    onOpenCookie = onOpenCookie,
                )
            }
            state.lastSuggestion?.let { s -> item { SuggestionCard(s) } }
            item { FormationCard(state.members) }
            state.synergy?.let { syn ->
                item { SynergyCard(syn) }
                item {
                    TreasureCard(
                        treasures = state.treasures,
                        selected = state.team.treasureIds,
                        recommended = syn.recommendedTreasures,
                        onToggle = viewModel::toggleTreasure,
                        onApplyRecommended = viewModel::applyRecommendedTreasures,
                    )
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.team.notes,
                        onValueChange = viewModel::setNotes,
                        label = { Text("Notes") },
                        placeholder = { Text("Skill order, counters, Beascuit swaps…") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    pickerSlot?.let { slot ->
        CookiePickerSheet(
            cookies = state.cookies,
            inTeam = state.team.slots.filterNotNull().toSet(),
            onPick = { viewModel.assign(slot, it); pickerSlot = null },
            onDismiss = { pickerSlot = null },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this team?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("This team has unsaved changes.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false; viewModel.save(onBack) }) { Text("Save & exit") } },
        )
    }
}

@Composable
private fun LineupCard(
    members: List<Cookie?>,
    onSlotClick: (Int) -> Unit,
    onClear: (Int) -> Unit,
    onSort: () -> Unit,
    onOpenCookie: (String) -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            "Lineup",
            icon = Icons.Rounded.ViewColumn,
            subtitle = "${members.count { it != null }}/${Team.SIZE} cookies · tap a slot to pick",
            action = { TextButton(onClick = onSort) { Icon(Icons.Rounded.SwapVert, null); Text("Sort") } },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            members.forEachIndexed { i, cookie ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { onSlotClick(i) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        if (cookie != null) {
                            CookieAvatar(cookie, size = 58.dp)
                            Box(
                                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface).clickable { onClear(i) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Close, "Remove from slot", Modifier.size(14.dp)) }
                        } else {
                            Box(
                                Modifier.size(58.dp).clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Add, "Add cookie", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        cookie?.name?.removeSuffix(" Cookie") ?: "Slot ${i + 1}",
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = if (cookie != null) Modifier.clickable { onOpenCookie(cookie.id) } else Modifier,
                    )
                    cookie?.let { Text(it.position.label, style = MaterialTheme.typography.labelSmall, color = it.position.color) }
                }
            }
        }
    }
}

/** Battlefield preview: three lanes, cookies placed by their position. */
@Composable
private fun FormationCard(members: List<Cookie?>) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Formation", icon = Icons.Rounded.Insights, subtitle = "Rear ← Middle ← Front (enemy side →)")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Position.REAR, Position.MIDDLE, Position.FRONT).forEach { pos ->
                val lane = members.filterNotNull().filter { it.position == pos }
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(pos.color.copy(alpha = 0.12f)).padding(8.dp)
                        .heightIn(min = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("${pos.label} (${lane.size})", style = MaterialTheme.typography.labelLarge, color = pos.color, fontWeight = FontWeight.Bold)
                    lane.forEach { CookieAvatar(it, size = 40.dp, showElement = false) }
                    if (lane.isEmpty()) Text("—", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun SynergyCard(syn: TeamSynergy) {
    GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreBadge(syn.score, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Synergy summary", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (syn.isValid) "Lineup is valid" else "Fix the errors below",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (syn.isValid) StatusGood else StatusBad,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Frontline", syn.front, Position.FRONT.color)
            StatTile("Middle", syn.middle, Position.MIDDLE.color)
            StatTile("Rear", syn.rear, Position.REAR.color)
            StatTile("Healers", syn.healers, StatusGood)
            StatTile("Supports", syn.supports, StatusWarn)
            StatTile("Defense", syn.defenders, MaterialTheme.colorScheme.tertiary)
            StatTile("DPS", syn.damageDealers, StatusBad)
        }
        if (syn.elements.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Elemental coverage", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                syn.elements.forEach { (e, n) -> TagPill("${e.label} ×$n", e.color, icon = e.icon) }
            }
        }
        Spacer(Modifier.height(12.dp))
        syn.messages.forEach { msg ->
            val (icon, color) = when (msg.severity) {
                Severity.ERROR -> Icons.Rounded.Error to StatusBad
                Severity.WARNING -> Icons.Rounded.Warning to StatusWarn
                Severity.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.primary
            }
            Row(Modifier.padding(vertical = 3.dp)) {
                Icon(icon, msg.severity.name, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(msg.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, color: Color) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TreasureCard(
    treasures: List<Treasure>,
    selected: List<String>,
    recommended: List<Treasure>,
    onToggle: (String) -> Unit,
    onApplyRecommended: () -> Unit,
) {
    val recIds = recommended.map { it.id }.toSet()
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            "Treasures",
            icon = Icons.Rounded.Diamond,
            subtitle = "${selected.size}/${Team.MAX_TREASURES} equipped",
            action = { if (recommended.isNotEmpty()) TextButton(onClick = onApplyRecommended) { Icon(Icons.Rounded.AutoAwesome, null); Text(" Suggest") } },
        )
        Spacer(Modifier.height(8.dp))
        treasures.sortedByDescending { it.id in recIds }.forEach { t ->
            val isSelected = t.id in selected
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onToggle(t.id) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TreasureArt(t.tags.firstOrNull(), size = 44.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                        if (t.id in recIds) { Spacer(Modifier.width(6.dp)); TagPill("Suggested", StatusGood) }
                    }
                    Text(t.effect, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isSelected) Icon(Icons.Rounded.Diamond, "Equipped", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CookiePickerSheet(
    cookies: List<Cookie>,
    inTeam: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var positions by remember { mutableStateOf(emptySet<Position>()) }
    var ownedOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(cookies, query, positions, ownedOnly) {
        cookies.filter { c ->
            (query.isBlank() || c.name.contains(query, ignoreCase = true)) &&
                (positions.isEmpty() || c.position in positions) &&
                (!ownedOnly || c.isOwned)
        }.sortedWith(compareBy({ it.position.sortOrder }, { it.rarity.sortOrder }, { it.name }))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text("Choose a cookie", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Search") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            )
            LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = ownedOnly, onClick = { ownedOnly = !ownedOnly }, label = { Text("Owned") }) }
                items(Position.entries.size) { i ->
                    val p = Position.entries[i]
                    FilterChip(selected = p in positions, onClick = { positions = positions.toggle(p) }, label = { Text(p.label) })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(filtered, key = { it.id }) { c ->
                    Column(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (c.id in inTeam) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onPick(c.id) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CookieAvatar(c, size = 60.dp)
                        Text(c.name.removeSuffix(" Cookie"), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (c.id in inTeam) "In team" else c.position.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (c.id in inTeam) MaterialTheme.colorScheme.primary else c.position.color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: TeamSuggestion) {
    GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.tertiary) {
        SectionHeader(
            "Why this lineup",
            icon = Icons.Rounded.AutoAwesome,
            subtitle = if (suggestion.usedUnowned) "Includes cookies you don't own yet" else "Built from your collection",
        )
        Spacer(Modifier.height(8.dp))
        suggestion.members.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CookieAvatar(m.cookie, size = 36.dp, showElement = false)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.cookie.name, style = MaterialTheme.typography.titleSmall)
                    Text(m.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
