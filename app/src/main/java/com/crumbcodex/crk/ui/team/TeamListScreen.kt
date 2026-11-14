package com.crumbcodex.crk.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.EmptyState
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.TagPill
import com.crumbcodex.crk.ui.dex.MascotArt
import com.crumbcodex.crk.ui.navigation.ThemeMenuButton
import com.crumbcodex.crk.ui.util.icon

@Composable
fun TeamListScreen(
    onOpenTeam: (Long) -> Unit,
    onNewTeam: (GameMode) -> Unit,
    viewModel: TeamListViewModel = viewModel(factory = TeamListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Team Builder", style = MaterialTheme.typography.headlineSmall) },
                actions = { ThemeMenuButton() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { state.modeFilter?.let(onNewTeam) ?: run { newMenu = true } },
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("New team") },
                )
                DropdownMenu(expanded = newMenu, onDismissRequest = { newMenu = false }) {
                    GameMode.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.label) },
                            leadingIcon = { Icon(m.icon, null) },
                            onClick = { newMenu = false; onNewTeam(m) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = state.modeFilter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") }) }
                items(GameMode.entries.size) { i ->
                    val m = GameMode.entries[i]
                    FilterChip(
                        selected = state.modeFilter == m,
                        onClick = { viewModel.setFilter(m) },
                        label = { Text(m.shortLabel) },
                        leadingIcon = { Icon(m.icon, null, Modifier.size(18.dp)) },
                    )
                }
            }
            if (!state.loading && state.teams.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "No teams yet",
                        message = "Build a 5-cookie lineup for Arena, World Exploration or Guild Boss and get an instant synergy check.",
                        art = { MascotArt() },
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("${state.teams.size} team(s)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.teams, key = { it.team.id }) { summary -> TeamCard(summary, onClick = { onOpenTeam(summary.team.id) }) }
                }
            }
        }
    }
}

@Composable
private fun TeamCard(summary: TeamSummary, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(summary.team.name.ifBlank { "Untitled team" }, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                TagPill(summary.team.mode.label, MaterialTheme.colorScheme.primary, icon = summary.team.mode.icon)
            }
            ScoreBadge(summary.synergy.score)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            summary.members.forEach { c ->
                if (c != null) CookieAvatar(c, size = 52.dp, showElement = false)
                else Box(
                    Modifier.size(52.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.outline) }
            }
        }
        Spacer(Modifier.height(8.dp))
        val s = summary.synergy
        Text(
            "Front ${s.front} · Mid ${s.middle} · Rear ${s.rear} · Healers ${s.healers} · Supports ${s.supports}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ScoreBadge(score: Int, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val color = when {
        score >= 80 -> com.crumbcodex.crk.ui.theme.StatusGood
        score >= 50 -> com.crumbcodex.crk.ui.theme.StatusWarn
        else -> com.crumbcodex.crk.ui.theme.StatusBad
    }
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
