package com.crumbcodex.crk.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowWidthSizeClass
import com.crumbcodex.crk.SeedState
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.ui.collection.CollectionScreen
import com.crumbcodex.crk.ui.collection.OwnedEditorScreen
import com.crumbcodex.crk.ui.components.AppBackground
import com.crumbcodex.crk.ui.components.GlassCard
import com.crumbcodex.crk.ui.detail.CookieDetailScreen
import com.crumbcodex.crk.ui.dex.DexListDetail
import com.crumbcodex.crk.ui.dex.DexScreen
import com.crumbcodex.crk.ui.farming.FarmingScreen
import com.crumbcodex.crk.ui.meta.MetaScreen
import com.crumbcodex.crk.ui.team.TeamEditorScreen
import com.crumbcodex.crk.ui.team.TeamListScreen

@Composable
fun CrkApp(
    seedState: SeedState,
    onRetrySeed: () -> Unit,
    openCoupons: Boolean = false,
    onOpenCouponsHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val expanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    LaunchedEffect(openCoupons) {
        if (openCoupons) navController.navigateTopLevel(TopLevelDestination.META)
    }

    AppBackground {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { dest ->
                    val selected = destination?.hierarchy?.any { it.hasRoute(dest.routeClass) } == true ||
                        (dest == TopLevelDestination.DEX && destination?.hasRoute(CookieDetailRoute::class) == true) ||
                        (dest == TopLevelDestination.COLLECTION && destination?.hasRoute(OwnedEditorRoute::class) == true) ||
                        (dest == TopLevelDestination.TEAMS && destination?.hasRoute(TeamEditorRoute::class) == true)
                    item(
                        selected = selected,
                        onClick = { navController.navigateTopLevel(dest) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            },
            containerColor = Color.Transparent,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                navigationRailContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            ),
        ) {
            Column(Modifier.fillMaxSize()) {
                SeedBanner(seedState, onRetrySeed)
                NavHost(navController, startDestination = DexRoute, modifier = Modifier.weight(1f)) {
                    composable<DexRoute> {
                        if (expanded) {
                            DexListDetail(
                                onOpenTracker = { navController.navigate(OwnedEditorRoute(it)) },
                            )
                        } else {
                            DexScreen(onCookieClick = { navController.navigate(CookieDetailRoute(it)) })
                        }
                    }
                    composable<CookieDetailRoute> { entry ->
                        val route = entry.toRoute<CookieDetailRoute>()
                        CookieDetailScreen(
                            cookieId = route.cookieId,
                            onBack = { navController.popBackStack() },
                            onOpenTracker = { navController.navigate(OwnedEditorRoute(it)) },
                        )
                    }
                    composable<CollectionRoute> {
                        CollectionScreen(
                            onOpen = { navController.navigate(OwnedEditorRoute(it)) },
                            onBrowse = { navController.navigateTopLevel(TopLevelDestination.DEX) },
                        )
                    }
                    composable<OwnedEditorRoute> { entry ->
                        val route = entry.toRoute<OwnedEditorRoute>()
                        OwnedEditorScreen(
                            cookieId = route.cookieId,
                            onBack = { navController.popBackStack() },
                            onOpenGuide = { navController.navigate(CookieDetailRoute(it)) },
                        )
                    }
                    composable<TeamsRoute> {
                        TeamListScreen(
                            onOpenTeam = { navController.navigate(TeamEditorRoute(teamId = it)) },
                            onNewTeam = { mode -> navController.navigate(TeamEditorRoute(teamId = 0L, mode = mode.name)) },
                            onSuggestTeam = { mode -> navController.navigate(TeamEditorRoute(teamId = 0L, mode = mode.name, suggest = true)) },
                        )
                    }
                    composable<TeamEditorRoute> { entry ->
                        val route = entry.toRoute<TeamEditorRoute>()
                        TeamEditorScreen(
                            teamId = route.teamId,
                            initialMode = runCatching { GameMode.valueOf(route.mode) }.getOrDefault(GameMode.ARENA),
                            autoSuggest = route.suggest,
                            onBack = { navController.popBackStack() },
                            onOpenCookie = { navController.navigate(CookieDetailRoute(it)) },
                        )
                    }
                    composable<FarmingRoute> { FarmingScreen() }
                    composable<MetaRoute> {
                        MetaScreen(
                            onCookieClick = { navController.navigate(CookieDetailRoute(it)) },
                            showCoupons = openCoupons,
                            onShowCouponsHandled = onOpenCouponsHandled,
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(dest: TopLevelDestination) {
    navigate(dest.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun SeedBanner(state: SeedState, onRetry: () -> Unit) {
    when (state) {
        SeedState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth().statusBarsPadding())
        is SeedState.Failed -> GlassCard(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            tint = MaterialTheme.colorScheme.error,
        ) {
            Row {
                Text(
                    "Couldn't load game data: ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
        SeedState.Ready -> Unit
    }
}
