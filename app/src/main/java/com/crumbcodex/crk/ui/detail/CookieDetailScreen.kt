package com.crumbcodex.crk.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.Build
import com.crumbcodex.crk.domain.model.CandyKind
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.usecase.BuildComparison
import com.crumbcodex.crk.ui.components.BeascuitArt
import com.crumbcodex.crk.ui.components.BulletList
import com.crumbcodex.crk.ui.components.CandyArt
import com.crumbcodex.crk.ui.components.ClassBadge
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.ElementBadge
import com.crumbcodex.crk.ui.components.EmptyState
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.PositionBadge
import com.crumbcodex.crk.ui.components.RarityBadge
import com.crumbcodex.crk.ui.components.SectionHeader
import com.crumbcodex.crk.ui.components.StarRow
import com.crumbcodex.crk.ui.components.StatProgress
import com.crumbcodex.crk.ui.components.TagPill
import com.crumbcodex.crk.ui.components.ToppingArt
import com.crumbcodex.crk.ui.components.drawSparkle
import com.crumbcodex.crk.ui.dex.MascotArt
import com.crumbcodex.crk.ui.theme.RosePink
import com.crumbcodex.crk.ui.theme.StatusGood
import com.crumbcodex.crk.ui.util.color
import com.crumbcodex.crk.ui.util.icon
import com.crumbcodex.crk.ui.util.toColors
import kotlin.random.Random

/**
 * @param onBack null when shown as the detail pane of the expanded list-detail layout.
 */
@Composable
fun CookieDetailScreen(
    cookieId: String,
    onBack: (() -> Unit)?,
    onOpenTracker: (String) -> Unit,
) {
    val viewModel: CookieDetailViewModel = viewModel(key = "detail-$cookieId", factory = CookieDetailViewModel.factory(cookieId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cookie = state.cookie

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(cookie?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    if (cookie != null) IconButton(onClick = viewModel::toggleOwned) {
                        Icon(
                            if (state.owned != null) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (state.owned != null) "Remove from collection" else "Add to collection",
                            tint = if (state.owned != null) RosePink else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        if (cookie == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!state.loading) EmptyState("Cookie not found", "This cookie isn't in the current data set.", art = { MascotArt() })
            }
            return@Scaffold
        }
        val build = cookie.buildFor(state.mode)
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { HeroCard(cookie) }
            item { SkillCard(cookie) }
            item {
                ModeTabs(state.mode, cookie, viewModel::selectMode)
            }
            if (build == null) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("No ${state.mode.label} build recorded for this cookie yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                item { BuildSummaryCard(build) }
                item { ToppingsCard(build) }
                item { SubstatsCard(build) }
                item { BeascuitCard(build) }
                if (build.notes.isNotEmpty()) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            SectionHeader("Play tips", icon = Icons.Rounded.Lightbulb)
                            Spacer(Modifier.height(10.dp))
                            BulletList(build.notes)
                        }
                    }
                }
            }
            item { CandyCard(cookie) }
            item {
                MySetupCard(
                    owned = state.owned,
                    comparison = state.comparison,
                    mode = state.mode,
                    onAdd = viewModel::toggleOwned,
                    onOpenTracker = { onOpenTracker(cookie.id) },
                )
            }
        }
    }
}

@Composable
private fun HeroCard(cookie: Cookie) {
    val palette = remember(cookie.id) { cookie.palette.toColors() }
    val elementColor = cookie.elements.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.first().copy(alpha = 0.55f),
                        elementColor.copy(alpha = 0.35f),
                        palette.last().copy(alpha = 0.5f),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val rnd = Random(cookie.id.hashCode())
            repeat(14) {
                drawSparkle(
                    Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height),
                    6f + rnd.nextFloat() * 12f,
                    Color.White.copy(alpha = 0.35f + rnd.nextFloat() * 0.4f),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CookieAvatar(cookie, size = 148.dp)
            Spacer(Modifier.height(12.dp))
            Text(cookie.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (cookie.title.isNotBlank()) {
                Text(cookie.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RarityBadge(cookie.rarity)
                ClassBadge(cookie.cookieClass)
                PositionBadge(cookie.position)
                cookie.elements.forEach { ElementBadge(it) }
            }
        }
    }
}

@Composable
private fun SkillCard(cookie: Cookie) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            cookie.skill.name,
            icon = Icons.Rounded.AutoAwesome,
            subtitle = "Skill",
            action = {
                cookie.skill.cooldownSeconds?.let { TagPill("${it}s CD", MaterialTheme.colorScheme.primary, icon = Icons.Rounded.Timer) }
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(cookie.skill.description, style = MaterialTheme.typography.bodyMedium)
        cookie.wikiUrl?.let { url ->
            val uriHandler = LocalUriHandler.current
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { uriHandler.openUri(url) }) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open on wiki")
            }
            Text(
                "Data: Cookie Run: Kingdom Wiki (Fandom), CC BY-SA. Artwork © Devsisters, loaded from the wiki CDN.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ModeTabs(selected: GameMode, cookie: Cookie, onSelect: (GameMode) -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        PrimaryTabRow(selectedTabIndex = selected.ordinal, containerColor = Color.Transparent) {
            GameMode.entries.forEach { mode ->
                val has = cookie.buildFor(mode) != null
                Tab(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    text = { Text(mode.shortLabel) },
                    icon = { Icon(mode.icon, null) },
                    enabled = true,
                    unselectedContentColor = if (has) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun BuildSummaryCard(build: Build) {
    GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primary) {
        Text(build.mode.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(build.title, style = MaterialTheme.typography.titleLarge)
        if (build.summary.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(build.summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ToppingsCard(build: Build) {
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Toppings", icon = Icons.Rounded.Widgets, subtitle = build.toppings.joinToString(" + ") { "${it.count}× ${it.type.label}" })
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            build.toppings.forEach { slot ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    ToppingArt(slot.type, size = 56.dp, count = slot.count)
                    Spacer(Modifier.height(4.dp))
                    Text(slot.type.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    Text("Main: ${slot.type.mainStat.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        build.tart?.let { tart ->
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Cake, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Topping Tart: ${tart.name}", style = MaterialTheme.typography.titleSmall)
                    Text("Stat: ${tart.stat.label}" + if (tart.note.isNotBlank()) " — ${tart.note}" else "", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SubstatsCard(build: Build) {
    if (build.substats.isEmpty()) return
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Priority substats", icon = Icons.Rounded.QueryStats, subtitle = "Target thresholds across all 5 toppings")
        Spacer(Modifier.height(10.dp))
        build.substats.sortedBy { it.priority }.forEach { sub ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${sub.priority}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(sub.stat.label, style = MaterialTheme.typography.titleSmall)
                    sub.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                TagPill(sub.targetLabel, MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun BeascuitCard(build: Build) {
    val rec = build.beascuit
    GlassCard(Modifier.fillMaxWidth()) {
        SectionHeader("Beascuit", icon = Icons.Rounded.Pets)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BeascuitArt(rec.element?.color ?: MaterialTheme.colorScheme.primary, rec.grade, size = 72.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(rec.grade.label, style = MaterialTheme.typography.titleMedium)
                rec.element?.let { ElementBadge(it) }
                if (rec.attunements.isNotEmpty()) {
                    Text("Attunement priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        rec.attunements.forEachIndexed { i, s -> TagPill("${i + 1}. ${s.label}", MaterialTheme.colorScheme.secondary) }
                    }
                }
            }
        }
        if (rec.note.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(rec.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CandyCard(cookie: Cookie) {
    val candy = cookie.magicCandy
    val tint = cookie.elements.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CandyArt(candy.kind, tint, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(candy.kind.label, style = MaterialTheme.typography.titleMedium)
                Text("Magic Candy / Crystal Jam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (candy.kind != CandyKind.NONE) TagPill("Priority: ${candy.priority.label}", candy.priority.color)
        }
        if (candy.summary.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(candy.summary, style = MaterialTheme.typography.bodyMedium)
        }
        if (candy.effects.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            BulletList(candy.effects)
        }
        if (candy.ingredients.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Upgrade materials", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                candy.ingredients.forEach { TagPill(it, tint) }
            }
        }
    }
}

@Composable
private fun MySetupCard(
    owned: OwnedCookie?,
    comparison: BuildComparison?,
    mode: GameMode,
    onAdd: () -> Unit,
    onOpenTracker: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.secondary) {
        SectionHeader("My setup vs. guide", icon = Icons.Rounded.Checklist, subtitle = mode.label)
        Spacer(Modifier.height(10.dp))
        if (owned == null) {
            Text("Add this cookie to your collection to track level, toppings, Beascuit and compare your stats against these benchmarks.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onAdd) { Text("Add to collection") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lv.${owned.level} · Power ${owned.power}", style = MaterialTheme.typography.titleSmall)
                    StarRow(owned.stars, OwnedCookie.MAX_STARS, size = 16)
                }
                comparison?.let { Text("${it.met}/${it.total} met", style = MaterialTheme.typography.titleMedium, color = StatusGood) }
            }
            comparison?.let {
                Spacer(Modifier.height(8.dp))
                StatProgress(it.overall, StatusGood)
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(onClick = onOpenTracker) { Text("Open build tracker") }
        }
    }
}
