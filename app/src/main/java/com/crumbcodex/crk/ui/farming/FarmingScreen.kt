package com.crumbcodex.crk.ui.farming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.BeascuitGrade
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.DropCategory
import com.crumbcodex.crk.domain.model.DropItem
import com.crumbcodex.crk.domain.model.DropSource
import com.crumbcodex.crk.domain.model.DropSourceType
import com.crumbcodex.crk.domain.model.ToppingType
import com.crumbcodex.crk.ui.components.BeascuitArt
import com.crumbcodex.crk.ui.components.CandyArt
import com.crumbcodex.crk.ui.components.EmptyState
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.TagPill
import com.crumbcodex.crk.ui.components.ToppingArt
import com.crumbcodex.crk.ui.dex.MascotArt
import com.crumbcodex.crk.ui.navigation.ThemeMenuButton
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon

@Composable
fun FarmingScreen(viewModel: FarmingViewModel = viewModel(factory = FarmingViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Drop Finder", style = MaterialTheme.typography.headlineSmall)
                        Text("Beascuits & upgrade materials", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { ThemeMenuButton() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search item or stage…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (state.filter.query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Rounded.Close, "Clear") } },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
            LazyRow(
                Modifier.padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = state.filter.category == null, onClick = { viewModel.setCategory(null) }, label = { Text("All") }) }
                items(DropCategory.entries) { c ->
                    FilterChip(selected = state.filter.category == c, onClick = { viewModel.setCategory(c) }, label = { Text(c.label) })
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(DropSourceType.entries) { t ->
                    FilterChip(
                        selected = state.filter.sourceType == t,
                        onClick = { viewModel.setSourceType(t) },
                        label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon, null, Modifier.size(16.dp)) },
                    )
                }
            }
            if (state.availableElements.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.availableElements) { e ->
                        FilterChip(
                            selected = state.filter.element == e,
                            onClick = { viewModel.setElement(e) },
                            label = { Text(e.label) },
                            leadingIcon = { Icon(e.icon, null, Modifier.size(16.dp), tint = e.color) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = e.color.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
            if (!state.loading && state.items.isEmpty()) {
                EmptyState("Nothing found", "No drop source matches these filters.", art = { MascotArt() })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SourceLegend() }
                    items(state.items, key = { it.id }) { DropCard(it) }
                }
            }
        }
    }
}

@Composable
private fun SourceLegend() {
    GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.tertiary) {
        Text("Where things drop", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Beast-Yeast stages and Hard Mode are stamina-based farming; Monster Menace and Beast Raid are " +
                "rotating events with ticket limits. \"Best\" marks the most efficient source per item.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DropCard(item: DropItem) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val tint = item.element?.color ?: MaterialTheme.colorScheme.primary
    GlassCard(Modifier.fillMaxWidth().animateContentSize(), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DropArt(item, tint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagPill(item.category.label, MaterialTheme.colorScheme.primary)
                    item.element?.let { TagPill(it.label, it.color, icon = it.icon) }
                }
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (expanded) "Collapse" else "Expand")
        }
        if (item.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val best = item.sources.firstOrNull()
        if (!expanded && best != null) {
            Spacer(Modifier.height(8.dp))
            SourceRow(best)
            if (item.sources.size > 1) Text("+${item.sources.size - 1} more source(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.sources.forEach { SourceRow(it) }
            }
        }
    }
}

@Composable
private fun DropArt(item: DropItem, tint: Color) {
    when (item.category) {
        DropCategory.BEASCUIT -> BeascuitArt(tint, if (item.name.contains("Legendary", true)) BeascuitGrade.LEGENDARY else BeascuitGrade.EPIC, size = 56.dp)
        DropCategory.MAGIC_CANDY -> CandyArt(CandyKind.MAGIC_CANDY, tint, size = 56.dp)
        DropCategory.CRYSTAL_JAM -> CandyArt(CandyKind.CRYSTAL_JAM, tint, size = 56.dp)
        DropCategory.TOPPING -> ToppingArt(
            ToppingType.entries.firstOrNull { item.name.contains(it.label, true) } ?: ToppingType.SEARING_RASPBERRY,
            size = 56.dp,
        )
    }
}

@Composable
private fun SourceRow(source: DropSource) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(source.efficiency.color.copy(alpha = 0.10f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(source.efficiency.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(source.type.icon, null, Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(source.name, style = MaterialTheme.typography.titleSmall)
            Text(
                source.type.label + if (source.detail.isNotBlank()) " · ${source.detail}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TagPill(source.efficiency.label, source.efficiency.color)
    }
}
