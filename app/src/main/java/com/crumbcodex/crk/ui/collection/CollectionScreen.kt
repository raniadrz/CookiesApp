package com.crumbcodex.crk.ui.collection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import java.time.LocalDate
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.EmptyState
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.RarityBadge
import com.crumbcodex.crk.ui.components.StarRow
import com.crumbcodex.crk.ui.components.StatProgress
import com.crumbcodex.crk.ui.dex.MascotArt
import com.crumbcodex.crk.ui.navigation.ThemeMenuButton
import com.crumbcodex.crk.ui.theme.StatusBad
import com.crumbcodex.crk.ui.theme.StatusGood
import com.crumbcodex.crk.ui.theme.StatusWarn
import java.text.NumberFormat

@Composable
fun CollectionScreen(
    onOpen: (String) -> Unit,
    onBrowse: () -> Unit,
    viewModel: CollectionViewModel = viewModel(factory = CollectionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortOpen by rememberSaveable { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val backupVm: BackupViewModel = viewModel(factory = BackupViewModel.Factory)
    val backupBusy by backupVm.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resolver = LocalContext.current.contentResolver
    LaunchedEffect(backupVm) { backupVm.messages.collect { snackbar.showSnackbar(it) } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) backupVm.export(resolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) backupVm.import(resolver, uri)
    }
    val nf = NumberFormat.getIntegerInstance()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("My Collection", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    Box {
                        TextButton(onClick = { sortOpen = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, null)
                            Spacer(Modifier.width(4.dp))
                            Text(state.sort.label)
                        }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            CollectionSort.entries.forEach {
                                DropdownMenuItem(text = { Text(it.label) }, onClick = { viewModel.setSort(it); sortOpen = false })
                            }
                        }
                    }
                    ThemeMenuButton()
                    Box {
                        if (backupBusy) {
                            CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { moreOpen = true }) { Icon(Icons.Rounded.MoreVert, "Backup options") }
                        }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Export backup") },
                                leadingIcon = { Icon(Icons.Rounded.Upload, null) },
                                onClick = {
                                    moreOpen = false
                                    exportLauncher.launch("crumb-codex-backup-${LocalDate.now()}.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Import backup") },
                                leadingIcon = { Icon(Icons.Rounded.Download, null) },
                                onClick = {
                                    moreOpen = false
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        if (!state.loading && state.entries.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Your jar is empty",
                    message = "Tap the heart on any cookie in the Cookie-Dex to start tracking levels, toppings and Beascuits. " +
                        "Moving from another phone? Use ⋮ → Import backup.",
                    art = { MascotArt() },
                    actionLabel = "Browse Cookie-Dex",
                    onAction = onBrowse,
                )
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.primary) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryStat("Owned", "${state.entries.size}/${state.totalCookies}")
                        SummaryStat("Total power", nf.format(state.totalPower))
                        SummaryStat("Avg. level", "${state.averageLevel}")
                    }
                }
            }
            items(state.entries, key = { it.info.cookie.id }) { entry ->
                OwnedCard(entry, onClick = { onOpen(entry.info.cookie.id) }, nf = nf)
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OwnedCard(entry: CollectionEntry, onClick: () -> Unit, nf: NumberFormat) {
    val cookie = entry.info.cookie
    val owned = entry.info.owned
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CookieAvatar(cookie, size = 68.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cookie.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lv.${owned.level}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    StarRow(owned.stars, OwnedCookie.MAX_STARS, size = 14)
                    if (owned.ascension > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("A${owned.ascension}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text("Power ${nf.format(owned.power)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RarityBadge(cookie.rarity)
        }
        if (entry.readiness.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Build readiness", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            entry.readiness.forEach { (mode, p) ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.shortLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
                    val c = when {
                        p >= 0.85f -> StatusGood
                        p >= 0.4f -> StatusWarn
                        else -> StatusBad
                    }
                    StatProgress(p, c, Modifier.weight(1f))
                    Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp).width(40.dp))
                }
            }
        }
    }
}
