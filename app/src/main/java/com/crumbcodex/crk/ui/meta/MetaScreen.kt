package com.crumbcodex.crk.ui.meta

import androidx.compose.foundation.background
import com.crumbcodex.crk.domain.repository.DataStatus
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crumbcodex.crk.domain.model.Coupon
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.ui.components.CookieAvatar
import com.crumbcodex.crk.ui.components.EmptyState
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.components.TagPill
import com.crumbcodex.crk.ui.dex.MascotArt
import com.crumbcodex.crk.ui.navigation.ThemeMenuButton
import com.crumbcodex.crk.ui.theme.StatusBad
import com.crumbcodex.crk.ui.theme.StatusGood
import com.crumbcodex.crk.ui.theme.StatusWarn
import com.crumbcodex.crk.ui.util.icon
import com.crumbcodex.crk.ui.util.tierColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
fun MetaScreen(onCookieClick: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val couponsVm: CouponsViewModel = viewModel(factory = CouponsViewModel.Factory)
    val dataVm: DataUpdateViewModel = viewModel(factory = DataUpdateViewModel.Factory)
    val dataStatus by dataVm.status.collectAsStateWithLifecycle()
    LaunchedEffect(dataVm) { dataVm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Meta & Codes", style = MaterialTheme.typography.headlineSmall) },
                    actions = {
                        if (dataStatus.checking) {
                            CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = dataVm::check) { Icon(Icons.Rounded.Sync, "Check for data updates") }
                        }
                        ThemeMenuButton()
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tier lists") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Coupon codes") })
                }
            }
        },
        floatingActionButton = {
            if (tab == 1) FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, "Add coupon code") }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (tab == 0) TierListTab(onCookieClick, dataStatus, dataVm::check) else CouponsTab(couponsVm, snackbar)
        }
    }

    if (showAdd) AddCouponDialog(onDismiss = { showAdd = false }, onAdd = couponsVm::add)
}

@Composable
private fun TierListTab(
    onCookieClick: (String) -> Unit,
    dataStatus: DataStatus,
    onCheckUpdates: () -> Unit,
    viewModel: TierListViewModel = viewModel(factory = TierListViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GameMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = state.mode == m,
                        onClick = { viewModel.setMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, GameMode.entries.size),
                        icon = { Icon(m.icon, null, Modifier.size(18.dp)) },
                    ) { Text(m.shortLabel) }
                }
            }
        }
        item { DataStatusCard(dataStatus, onCheckUpdates) }
        item {
            GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.tertiary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Editorial snapshot. Rankings shift with every balance patch; new data arrives with the data updates above.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (!state.loading && state.groups.isEmpty()) {
            item { EmptyState("No tier list yet", "No rankings recorded for ${state.mode.label}.", art = { MascotArt() }) }
        }
        items(state.groups, key = { "${state.mode}-${it.tier}" }) { group ->
            GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Row {
                    Box(
                        Modifier.width(64.dp).heightIn(min = 110.dp).background(tierColor(group.tier).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(group.tier, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                    }
                    FlowRow(
                        Modifier.weight(1f).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        group.entries.forEach { e ->
                            Column(
                                Modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).clickable { onCookieClick(e.cookie.id) }.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CookieAvatar(e.cookie, size = 56.dp)
                                Text(
                                    e.cookie.name.removeSuffix(" Cookie"), style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                )
                                if (e.note.isNotBlank()) {
                                    Text(
                                        e.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponsTab(viewModel: CouponsViewModel, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Coupon?>(null) }

    fun copy(code: String) {
        clipboard.setText(AnnotatedString(code))
        scope.launch { snackbar.showSnackbar("Copied $code") }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.secondary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Redeem, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Redeem a code", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Codes are redeemed on the official coupon page with your DevPlay account or in-game User ID (Settings → Info).",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { uriHandler.openUri(CouponsViewModel.REDEEM_URL) }) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open redemption page")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.coupons.size} code(s)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = state.showExpired,
                    onClick = viewModel::toggleShowExpired,
                    label = { Text(if (state.showExpired) "Showing expired" else "Show expired (${state.hiddenExpired})") },
                )
            }
        }
        if (!state.loading && state.coupons.isEmpty()) {
            item {
                EmptyState(
                    "No active codes",
                    "Add codes you find on official socials with the + button — they're saved on your device.",
                    art = { MascotArt() },
                )
            }
        }
        items(state.coupons, key = { it.code }) { coupon ->
            CouponCard(
                coupon = coupon,
                now = state.now,
                onCopy = { copy(coupon.code) },
                onRedeem = { copy(coupon.code); uriHandler.openUri(CouponsViewModel.REDEEM_URL) },
                onToggleRedeemed = { viewModel.setRedeemed(coupon.code, !coupon.isRedeemed) },
                onDelete = { pendingDelete = coupon },
            )
        }
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${c.code}?") },
            confirmButton = { TextButton(onClick = { viewModel.delete(c.code); pendingDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

@Composable
private fun CouponCard(
    coupon: Coupon,
    now: Long,
    onCopy: () -> Unit,
    onRedeem: () -> Unit,
    onToggleRedeemed: () -> Unit,
    onDelete: () -> Unit,
) {
    val expired = coupon.isExpired(now)
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalOffer, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                coupon.code,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clickable(onClick = onCopy),
            )
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, "Copy code") }
        }
        if (coupon.rewards.isNotBlank()) Text(coupon.rewards, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val exp = coupon.expiresAt
            when {
                expired -> TagPill("Expired", StatusBad)
                exp == null -> TagPill("No known expiry", StatusGood)
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(exp - now)
                    TagPill(
                        if (days < 1) "Expires today" else "Expires ${dateFmt.format(Instant.ofEpochMilli(exp))} ($days d)",
                        if (days <= 3) StatusWarn else StatusGood,
                    )
                }
            }
            if (coupon.isVerified) TagPill("Verified", StatusGood, icon = Icons.Rounded.Verified)
            else TagPill(if (coupon.isUserAdded) "Added by you" else "Unverified", StatusWarn)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = coupon.isRedeemed, onCheckedChange = { onToggleRedeemed() })
            Text("Redeemed", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete code") }
            FilledTonalButton(onClick = onRedeem, enabled = !expired) { Text("Copy & redeem") }
        }
    }
}

@Composable
private fun AddCouponDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> String?) {
    var code by rememberSaveable { mutableStateOf("") }
    var rewards by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add coupon code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it.uppercase(); error = null }, label = { Text("Code") }, singleLine = true)
                OutlinedTextField(rewards, { rewards = it }, label = { Text("Rewards (optional)") }, singleLine = true)
                OutlinedTextField(expiry, { expiry = it; error = null }, label = { Text("Expires (YYYY-MM-DD, optional)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = onAdd(code, rewards, expiry)
                if (result == null) onDismiss() else error = result
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DataStatusCard(status: DataStatus, onCheck: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudSync, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Game data", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Cookies v${status.cookiesVersion ?: "–"} · Tiers & drops v${status.gameDataVersion ?: "–"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    status.lastCheckedAt?.let { "Last checked ${relativeTime(it)} · checks automatically once a day" }
                        ?: "Not checked online yet · checks automatically once a day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onCheck, enabled = !status.checking) {
                Text(if (status.checking) "Checking…" else "Check now")
            }
        }
    }
}

private fun relativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        else -> "${minutes / (60 * 24)} d ago"
    }
}
