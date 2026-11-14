package com.crumbcodex.crk.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * JSON seed schema. To add a cookie, append an entry to assets/seed/cookies.json and bump
 * the top-level "version" — the app re-imports static data on next launch while keeping
 * all user data (collection, teams, coupons) intact.
 */

@Serializable
data class CookieSeedFile(
    val version: Int,
    val cookies: List<CookieDto>,
)

@Serializable
data class CookieDto(
    val id: String,
    val name: String,
    val title: String = "",
    val rarity: String,
    @SerialName("class") val cookieClass: String,
    val position: String,
    val elements: List<String> = emptyList(),
    val palette: List<String> = listOf("#C9B8FF", "#FFC2D4"),
    val imageUrl: String? = null,
    val skill: SkillDto,
    val magicCandy: MagicCandyDto = MagicCandyDto(),
    val builds: List<BuildDto> = emptyList(),
)

@Serializable
data class SkillDto(
    val name: String,
    val description: String,
    val cooldown: Int? = null,
)

@Serializable
data class MagicCandyDto(
    val kind: String = "NONE",
    val priority: String = "SKIP",
    val summary: String = "",
    val effects: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
)

@Serializable
data class BuildDto(
    val mode: String,
    val title: String,
    val summary: String = "",
    val toppings: List<ToppingSlotDto>,
    val tart: TartDto? = null,
    val substats: List<SubstatDto> = emptyList(),
    val beascuit: BeascuitDto,
    val notes: List<String> = emptyList(),
)

@Serializable
data class ToppingSlotDto(val type: String, val count: Int)

@Serializable
data class TartDto(val name: String, val stat: String, val note: String = "")

@Serializable
data class SubstatDto(
    val stat: String,
    val min: Double,
    val max: Double? = null,
    val priority: Int = 1,
    val note: String? = null,
)

@Serializable
data class BeascuitDto(
    val grade: String = "LEGENDARY",
    val element: String? = null,
    val attunements: List<String> = emptyList(),
    val note: String = "",
)

@Serializable
data class GameDataSeedFile(
    val version: Int,
    val treasures: List<TreasureDto> = emptyList(),
    val drops: List<DropItemDto> = emptyList(),
    val tierLists: Map<String, List<TierGroupDto>> = emptyMap(),
    val coupons: List<CouponDto> = emptyList(),
)

@Serializable
data class TreasureDto(
    val id: String,
    val name: String,
    val effect: String,
    val tags: List<String> = emptyList(),
    val modes: List<String> = emptyList(),
)

@Serializable
data class DropItemDto(
    val id: String,
    val category: String,
    val name: String,
    val element: String? = null,
    val description: String = "",
    val sources: List<DropSourceDto>,
)

@Serializable
data class DropSourceDto(
    val name: String,
    val type: String,
    val detail: String = "",
    val efficiency: String = "OK",
)

@Serializable
data class TierGroupDto(
    val tier: String,
    val cookies: List<String>,
    val notes: Map<String, String> = emptyMap(),
)

@Serializable
data class CouponDto(
    val code: String,
    val rewards: String,
    /** ISO date "yyyy-MM-dd" (inclusive, end of day UTC) or null for no known expiry. */
    val expires: String? = null,
    val verified: Boolean = false,
)
