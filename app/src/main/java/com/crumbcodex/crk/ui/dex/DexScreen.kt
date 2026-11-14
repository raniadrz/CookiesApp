package com.crumbcodex.crk.ui.dex

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.CookieClass
import com.crumbcodex.crk.domain.model.Element
import com.crumbcodex.crk.domain.model.Position
import com.crumbcodex.crk.domain.model.Rarity
import com.crumbcodex.crk.domain.usecase.CookieSort
import com.crumbcodex.crk.ui.components.ClassBadge
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.EmptyState
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.PositionBadge
import com.crumbcodex.crk.ui.components.RarityBadge
import com.crumbcodex.crk.ui.components.TagPill
import com.crumbcodex.crk.ui.components.drawCookieMascot
import com.crumbcodex.crk.ui.detail.CookieDetailScreen
import com.crumbcodex.crk.ui.navigation.ThemeMenuButton
import com.crumbcodex.crk.ui.theme.Lavender
import com.crumbcodex.crk.ui.theme.RosePink
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon

/** Compact/medium layout: grid only, detail opens as its own destination. */
@Composable
fun DexScreen(
    onCookieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    viewModel: DexViewModel = viewModel(factory = DexViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cookie-Dex", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${state.ownedCount}/${state.total} owned",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = { ThemeMenuButton() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchAndFilterBar(
                state = state,
                onQuery = viewModel::onQueryChange,
                onOpenFilters = { showFilters = true },
                onSort = viewModel::setSort,
                onToggleOwned = viewModel::toggleOwnedOnly,
            )
            ActiveFilterChips(state, viewModel)
            if (!state.loading && state.cookies.isEmpty()) {
                EmptyState(
                    title = if (state.total == 0) "Baking the roster…" else "No cookies match",
                    message = if (state.total == 0) "Game data is being imported." else "Try a different search or clear some filters.",
                    art = { MascotArt() },
                    actionLabel = if (state.filter.isEmpty) null else "Clear filters",
                    onAction = { viewModel.clearFilters(); viewModel.onQueryChange("") },
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 164.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${state.cookies.size} cookies",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.cookies, key = { it.id }) { cookie ->
                        CookieCard(
                            cookie = cookie,
                            selected = cookie.id == selectedId,
                            onClick = { onCookieClick(cookie.id) },
                            onToggleOwned = { viewModel.setOwned(cookie.id, !cookie.isOwned) },
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(state = state, viewModel = viewModel, onDismiss = { showFilters = false })
    }
}

/** Expanded layout (tablets / unfolded foldables): grid on the left, detail pane on the right. */
@Composable
fun DexListDetail(onOpenTracker: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize()) {
        DexScreen(
            onCookieClick = { selected = it },
            selectedId = selected,
            modifier = Modifier.weight(0.45f).fillMaxHeight(),
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Box(Modifier.weight(0.55f).fillMaxHeight()) {
            AnimatedContent(targetState = selected, label = "detailPane") { id ->
                if (id == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            title = "Pick a cookie",
                            message = "Select any cookie to see its PvP, PvE and Guild Boss builds side by side.",
                            art = { MascotArt() },
                        )
                    }
                } else {
                    CookieDetailScreen(cookieId = id, onBack = null, onOpenTracker = onOpenTracker)
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    state: DexUiState,
    onQuery: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onSort: (CookieSort) -> Unit,
    onToggleOwned: () -> Unit,
) {
    var sortOpen by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search name, class, element, skill…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.filter.query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Clear search") }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                unfocusedBorderColor = Color.Transparent,
            ),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(badge = { if (state.filter.activeCount > 0) Badge { Text("${state.filter.activeCount}") } }) {
                FilterChip(
                    selected = state.filter.activeCount > 0,
                    onClick = onOpenFilters,
                    label = { Text("Filters") },
                    leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) },
                )
            }
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = state.filter.ownedOnly,
                onClick = onToggleOwned,
                label = { Text("Owned") },
                leadingIcon = { Icon(if (state.filter.ownedOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, Modifier.size(18.dp)) },
            )
            Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick = { sortOpen = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(state.sort.label)
                }
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    CookieSort.entries.forEach {
                        DropdownMenuItem(text = { Text("Sort by ${it.label}") }, onClick = { onSort(it); sortOpen = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChips(state: DexUiState, vm: DexViewModel) {
    val f = state.filter
    if (f.activeCount == 0) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(f.rarities.toList()) { r -> RemovableChip(r.label) { vm.toggleRarity(r) } }
        items(f.classes.toList()) { c -> RemovableChip(c.label) { vm.toggleClass(c) } }
        items(f.positions.toList()) { p -> RemovableChip(p.label) { vm.togglePosition(p) } }
        items(f.elements.toList()) { e -> RemovableChip(e.label) { vm.toggleElement(e) } }
        item { TextButton(onClick = vm::clearFilters) { Text("Clear all") } }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Rounded.Close, "Remove $label", Modifier.size(16.dp)) },
    )
}

@Composable
private fun FilterSheet(state: DexUiState, viewModel: DexViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::clearFilters) { Text("Reset") }
            }
            FilterGroup("Rarity", Rarity.entries, state.filter.rarities, { it.label }, { it.color }, null, viewModel::toggleRarity)
            FilterGroup("Class", CookieClass.entries, state.filter.classes, { it.label }, { it.color }, { it.icon }, viewModel::toggleClass)
            FilterGroup("Position", Position.entries, state.filter.positions, { it.label }, { it.color }, null, viewModel::togglePosition)
            FilterGroup("Element", Element.entries, state.filter.elements, { it.label }, { it.color }, { it.icon }, viewModel::toggleElement)
            Text(
                "${state.cookies.size} of ${state.total} cookies match",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun <T> FilterGroup(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    color: (T) -> Color,
    icon: ((T) -> androidx.compose.ui.graphics.vector.ImageVector)?,
    onToggle: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isSelected = option in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(option) },
                    label = { Text(label(option)) },
                    leadingIcon = if (icon != null) {
                        { Icon(icon(option), null, Modifier.size(18.dp), tint = color(option)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color(option).copy(alpha = 0.25f)),
                )
            }
        }
    }
}

@Composable
fun CookieCard(
    cookie: Cookie,
    onClick: () -> Unit,
    onToggleOwned: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        tint = if (selected) MaterialTheme.colorScheme.primary else null,
        contentPadding = PaddingValues(12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            CookieAvatar(cookie, size = 84.dp, modifier = Modifier.align(Alignment.Center).padding(top = 4.dp))
            IconButton(onClick = onToggleOwned, modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                Icon(
                    if (cookie.isOwned) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (cookie.isOwned) "Remove from collection" else "Add to collection",
                    tint = if (cookie.isOwned) RosePink else MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            cookie.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Text(
            cookie.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RarityBadge(cookie.rarity)
            ClassBadge(cookie.cookieClass)
            PositionBadge(cookie.position)
            cookie.elements.forEach { TagPill(it.label, it.color, icon = it.icon) }
        }
    }
}

/** Decorative mascot used for empty states. */
@Composable
fun MascotArt(size: androidx.compose.ui.unit.Dp = 120.dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        drawCookieMascot(listOf(Lavender, RosePink), Rarity.EPIC, Lavender, 42)
    }
}
