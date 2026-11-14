package com.crumbcodex.crk.domain.model

data class Skill(
    val name: String,
    val description: String,
    val cooldownSeconds: Int?,
)

data class MagicCandyInfo(
    val kind: CandyKind,
    val priority: Priority,
    val summary: String,
    val effects: List<String>,
    val ingredients: List<String>,
)

data class ToppingSlot(val type: ToppingType, val count: Int)

data class TartRecommendation(val name: String, val stat: Stat, val note: String)

data class SubstatTarget(
    val stat: Stat,
    val min: Double,
    val max: Double?,
    val priority: Int,
    val note: String?,
) {
    val targetLabel: String
        get() {
            val unit = if (stat.isPercent) "%" else ""
            val lo = min.formatStat()
            return if (max == null) "$lo$unit+" else "$lo–${max.formatStat()}$unit"
        }
}

data class BeascuitRecommendation(
    val grade: BeascuitGrade,
    val element: Element?,
    val attunements: List<Stat>,
    val note: String,
)

data class Build(
    val mode: GameMode,
    val title: String,
    val summary: String,
    val toppings: List<ToppingSlot>,
    val tart: TartRecommendation?,
    val substats: List<SubstatTarget>,
    val beascuit: BeascuitRecommendation,
    val notes: List<String>,
)

data class Cookie(
    val id: String,
    val name: String,
    val title: String,
    val rarity: Rarity,
    val cookieClass: CookieClass,
    val position: Position,
    val elements: List<Element>,
    val palette: List<Long>,
    val imageUrl: String?,
    val skill: Skill,
    val magicCandy: MagicCandyInfo,
    val builds: List<Build>,
    val isOwned: Boolean = false,
) {
    fun buildFor(mode: GameMode): Build? = builds.firstOrNull { it.mode == mode }
}

data class OwnedCookie(
    val cookieId: String,
    val level: Int,
    val stars: Int,
    val ascension: Int,
    val power: Long,
    val skillLevel: Int,
    val candyLevel: Int,
    val toppings: List<ToppingSlot>,
    val tart: String,
    val beascuitGrade: BeascuitGrade?,
    val beascuitElement: Element?,
    val currentStats: Map<Stat, Double>,
    val checklist: Map<String, Boolean>,
    val notes: String,
    val updatedAt: Long,
) {
    companion object {
        fun newFor(cookieId: String, now: Long) = OwnedCookie(
            cookieId = cookieId, level = 1, stars = 0, ascension = 0, power = 0,
            skillLevel = 1, candyLevel = 0, toppings = emptyList(), tart = "",
            beascuitGrade = null, beascuitElement = null, currentStats = emptyMap(),
            checklist = emptyMap(), notes = "", updatedAt = now,
        )
        const val MAX_LEVEL = 90
        const val MAX_STARS = 5
        const val MAX_ASCENSION = 5
        const val MAX_SKILL_LEVEL = 90
        const val MAX_CANDY_LEVEL = 30
    }
}

data class OwnedCookieWithInfo(val owned: OwnedCookie, val cookie: Cookie)

data class Team(
    val id: Long,
    val name: String,
    val mode: GameMode,
    val slots: List<String?>,
    val treasureIds: List<String>,
    val notes: String,
    val updatedAt: Long,
) {
    companion object {
        const val SIZE = 5
        const val MAX_TREASURES = 3
        fun empty(mode: GameMode) = Team(0, "", mode, List(SIZE) { null }, emptyList(), "", 0)
    }
}

data class Treasure(
    val id: String,
    val name: String,
    val effect: String,
    val tags: List<SynergyTag>,
    val modes: List<GameMode>,
)

data class DropSource(
    val name: String,
    val type: DropSourceType,
    val detail: String,
    val efficiency: Efficiency,
)

data class DropItem(
    val id: String,
    val category: DropCategory,
    val name: String,
    val element: Element?,
    val description: String,
    val sources: List<DropSource>,
)

data class TierEntry(
    val mode: GameMode,
    val tier: String,
    val rank: Int,
    val note: String,
    val cookie: Cookie,
)

data class Coupon(
    val code: String,
    val rewards: String,
    val expiresAt: Long?,
    val isVerified: Boolean,
    val isUserAdded: Boolean,
    val isRedeemed: Boolean,
    val addedAt: Long,
) {
    fun isExpired(now: Long): Boolean = expiresAt != null && expiresAt < now
}

data class CookieFilter(
    val query: String = "",
    val rarities: Set<Rarity> = emptySet(),
    val classes: Set<CookieClass> = emptySet(),
    val positions: Set<Position> = emptySet(),
    val elements: Set<Element> = emptySet(),
    val ownedOnly: Boolean = false,
) {
    val activeCount: Int
        get() = rarities.size + classes.size + positions.size + elements.size + if (ownedOnly) 1 else 0
    val isEmpty: Boolean get() = query.isBlank() && activeCount == 0
}

fun Double.formatStat(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else "%.1f".format(this)
