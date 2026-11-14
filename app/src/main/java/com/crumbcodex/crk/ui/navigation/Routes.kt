package com.crumbcodex.crk.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object DexRoute
@Serializable data class CookieDetailRoute(val cookieId: String)
@Serializable data object CollectionRoute
@Serializable data class OwnedEditorRoute(val cookieId: String)
@Serializable data object TeamsRoute
@Serializable data class TeamEditorRoute(val teamId: Long = 0L, val mode: String = "ARENA")
@Serializable data object FarmingRoute
@Serializable data object MetaRoute

enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val routeClass: KClass<*>,
) {
    DEX("Cookies", Icons.Rounded.Cookie, DexRoute, DexRoute::class),
    COLLECTION("Collection", Icons.Rounded.Inventory2, CollectionRoute, CollectionRoute::class),
    TEAMS("Teams", Icons.Rounded.Groups, TeamsRoute, TeamsRoute::class),
    FARMING("Farming", Icons.Rounded.Map, FarmingRoute, FarmingRoute::class),
    META("Meta", Icons.Rounded.Leaderboard, MetaRoute, MetaRoute::class),
}
