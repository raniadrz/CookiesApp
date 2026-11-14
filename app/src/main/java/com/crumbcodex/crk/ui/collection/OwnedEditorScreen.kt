package com.crumbcodex.crk.ui.collection

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.ToppingType
import com.crumbcodex.crk.domain.usecase.CheckStatus
import com.crumbcodex.crk.domain.usecase.ComparisonItem
import com.crumbcodex.crk.ui.components.BeascuitArt
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.NumberStepper
import com.crumbcodex.crk.ui.components.SectionHeader
import com.crumbcodex.crk.ui.components.StarRow
import com.crumbcodex.crk.ui.components.StatProgress
import com.crumbcodex.crk.ui.components.ToppingArt
import com.crumbcodex.crk.ui.theme.StatusBad
import com.crumbcodex.crk.ui.theme.StatusGood
import com.crumbcodex.crk.ui.theme.StatusWarn
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon
import kotlinx.coroutines.launch

@Composable
fun OwnedEditorScreen(
    cookieId: String,
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
) {
    val viewModel: OwnedEditorViewModel = viewModel(key = "editor-$cookieId", factory = OwnedEditorViewModel.factory(cookieId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                EditorEvent.Saved -> scope.launch { snackbar.showSnackbar("Build saved") }
                EditorEvent.Removed -> onBack()
            }
        }
    }

    val back: () -> Unit = { if (state.dirty) confirmDiscard = true else onBack() }
    androidx.activity.compose.BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Build tracker", style = MaterialTheme.typography.titleLarge)
                        Text(state.cookie?.name.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { onOpenGuide(cookieId) }) { Icon(Icons.AutoMirrored.Rounded.MenuBook, "Open build guide") }
                    if (state.isPersisted) IconButton(onClick = { confirmRemove = true }) { Icon(Icons.Rounded.Delete, "Remove from collection") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (state.dirty || !state.isPersisted) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.save() },
                    icon = { Icon(Icons.Rounded.Save, null) },
                    text = { Text(if (state.isPersisted) "Save changes" else "Save to collection") },
                )
            }
        },
    ) { padding ->
        val cookie = state.cookie
        val draft = state.draft
        if (cookie == null || draft == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CookieAvatar(cookie, size = 72.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cookie.name, style = MaterialTheme.typography.titleLarge)
                            Text("${cookie.rarity.label} · ${cookie.cookieClass.label} · ${cookie.position.label}", style = MaterialTheme.typography.bodySmall)
                            if (state.dirty) Text("Unsaved changes", style = MaterialTheme.typography.labelMedium, color = StatusWarn)
                        }
                    }
                }
            }
            item { ProgressSection(state, viewModel) }
            item { ComparisonSection(state, viewModel) }
            item { ToppingSection(state, viewModel) }
            item { BeascuitSection(draft, viewModel) }
            item { StatsSection(state, viewModel) }
            item { ChecklistSection(draft, viewModel) }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    SectionHeader("Notes", icon = Icons.Rounded.EditNote)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = viewModel::setNotes,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Rotation, team ideas, what to farm next…") },
                    )
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove from collection?") },
            text = { Text("Your saved level, toppings, stats and notes for this cookie will be deleted.") },
            confirmButton = { TextButton(onClick = { confirmRemove = false; viewModel.removeFromCollection() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved edits to this build.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Discard") } },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false; viewModel.save(onSaved = onBack) }) { Text("Save & exit") }
            },
        )
    }
}

@Composable
private fun ProgressSection(state: EditorUiState, vm: OwnedEditorViewModel) {
    val draft = state.draft ?: return
    val cookie = state.cookie ?: return
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Progress", icon = Icons.AutoMirrored.Rounded.TrendingUp)
        Spacer(Modifier.height(8.dp))
        NumberStepper("Level", draft.level, 1..OwnedCookie.MAX_LEVEL, vm::setLevel)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Stars", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            StarRow(draft.stars, OwnedCookie.MAX_STARS, size = 30, onChange = vm::setStars)
        }
        NumberStepper("Ascension", draft.ascension, 0..OwnedCookie.MAX_ASCENSION, vm::setAscension)
        NumberStepper("Skill level", draft.skillLevel, 1..OwnedCookie.MAX_SKILL_LEVEL, vm::setSkillLevel)
        if (cookie.magicCandy.kind != CandyKind.NONE) {
            NumberStepper("${cookie.magicCandy.kind.label} level", draft.candyLevel, 0..OwnedCookie.MAX_CANDY_LEVEL, vm::setCandyLevel)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.powerInput,
            onValueChange = vm::setPower,
            label = { Text("Cookie power") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ComparisonSection(state: EditorUiState, vm: OwnedEditorViewModel) {
    val cookie = state.cookie ?: return
    GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primary) {
        SectionHeader("In-game setting guide comparison", icon = Icons.Rounded.Insights, subtitle = "Your setup vs. recommended benchmarks")
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GameMode.entries.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = state.compareMode == mode,
                    onClick = { vm.setCompareMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(i, GameMode.entries.size),
                    enabled = cookie.buildFor(mode) != null,
                    icon = {},
                ) { Text(mode.shortLabel) }
            }
        }
        Spacer(Modifier.height(12.dp))
        val comparison = state.comparison
        if (comparison == null) {
            Text("No recommended build for this mode.", style = MaterialTheme.typography.bodyMedium)
            return@GlassCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { comparison.overall },
                    modifier = Modifier.size(64.dp),
                    color = StatusGood,
                    trackColor = StatusGood.copy(alpha = 0.18f),
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round,
                )
                Text("${(comparison.overall * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("${comparison.met} of ${comparison.total} benchmarks met", style = MaterialTheme.typography.titleSmall)
                Text("Enter your in-game stats below to update.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        comparison.items.forEach { ComparisonRow(it) }
    }
}

@Composable
private fun ComparisonRow(item: ComparisonItem) {
    val (icon, color) = when (item.status) {
        CheckStatus.MET -> Icons.Rounded.CheckCircle to StatusGood
        CheckStatus.PARTIAL -> Icons.Rounded.Error to StatusWarn
        CheckStatus.MISSING -> Icons.Rounded.RadioButtonUnchecked to StatusBad
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = item.status.name, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(item.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("${item.current}  →  ${item.target}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.widthIn(max = 220.dp))
        }
        Spacer(Modifier.height(4.dp))
        StatProgress(item.progress, color)
    }
}

@Composable
private fun ToppingSection(state: EditorUiState, vm: OwnedEditorViewModel) {
    val draft = state.draft ?: return
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            "Equipped toppings",
            icon = Icons.Rounded.Widgets,
            subtitle = "${state.toppingTotal}/5 pieces",
            action = { TextButton(onClick = vm::applyRecommendedToppings) { Text("Use guide") } },
        )
        Spacer(Modifier.height(8.dp))
        ToppingType.entries.forEach { type ->
            val count = draft.toppings.firstOrNull { it.type == type }?.count ?: 0
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                ToppingArt(type, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    NumberStepper(
                        label = type.label,
                        value = count,
                        range = 0..(count + 5 - state.toppingTotal).coerceAtMost(5),
                        onChange = { vm.setToppingCount(type, it) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.tart,
            onValueChange = vm::setTart,
            label = { Text("Topping Tart") },
            placeholder = { Text("e.g. Raspberry Tart (ATK)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BeascuitSection(draft: OwnedCookie, vm: OwnedEditorViewModel) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Beascuit", icon = Icons.Rounded.Pets)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            BeascuitArt(draft.beascuitElement?.color ?: MaterialTheme.colorScheme.outline, draft.beascuitGrade ?: BeascuitGrade.COMMON, size = 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Grade", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BeascuitGrade.entries.forEach { g ->
                        FilterChip(
                            selected = draft.beascuitGrade == g,
                            onClick = { vm.setBeascuitGrade(if (draft.beascuitGrade == g) null else g) },
                            label = { Text(g.label.removeSuffix(" Beascuit")) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Element", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Element.entries.forEach { e ->
                FilterChip(
                    selected = draft.beascuitElement == e,
                    onClick = { vm.setBeascuitElement(if (draft.beascuitElement == e) null else e) },
                    label = { Text(e.label) },
                    leadingIcon = { Icon(e.icon, null, Modifier.size(16.dp), tint = e.color) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = e.color.copy(alpha = 0.25f)),
                )
            }
        }
    }
}

@Composable
private fun StatsSection(state: EditorUiState, vm: OwnedEditorViewModel) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Current stats", icon = Icons.Rounded.Insights, subtitle = "Copy totals from the in-game stat screen")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.trackedStats.forEach { stat ->
                OutlinedTextField(
                    value = state.statInputs[stat].orEmpty(),
                    onValueChange = { vm.setStatInput(stat, it) },
                    label = { Text(stat.label) },
                    suffix = { if (stat.isPercent) Text("%") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Composable
private fun ChecklistSection(draft: OwnedCookie, vm: OwnedEditorViewModel) {
    var newItem by rememberSaveable { mutableStateOf("") }
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("My checklist", icon = Icons.Rounded.Checklist, subtitle = "Custom goals, e.g. \"Reroll 2nd Chocolate for CD\"")
        Spacer(Modifier.height(6.dp))
        draft.checklist.forEach { (label, done) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = done, onCheckedChange = { vm.toggleChecklistItem(label) })
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.removeChecklistItem(label) }) { Icon(Icons.Rounded.Close, "Remove item") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                placeholder = { Text("Add a goal") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.addChecklistItem(newItem); newItem = "" }),
            )
            IconButton(onClick = { vm.addChecklistItem(newItem); newItem = "" }, enabled = newItem.isNotBlank()) {
                Icon(Icons.Rounded.Add, "Add goal")
            }
        }
    }
}
