package com.crumbcodex.crk.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Entity(tableName = "cookies")
data class CookieEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val rarity: String,
    val cookieClass: String,
    val position: String,
    val elements: List<String>,
    val palette: List<String>,
    val imageUrl: String?,
    val wikiUrl: String?,
    val skillName: String,
    val skillDescription: String,
    val skillCooldown: Int?,
    /** JSON of [com.crumbcodex.crk.data.seed.MagicCandyDto]. */
    val magicCandyJson: String,
)

@Entity(
    tableName = "cookie_builds",
    foreignKeys = [
        ForeignKey(
            entity = CookieEntity::class,
            parentColumns = ["id"],
            childColumns = ["cookieId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cookieId")],
)
data class CookieBuildEntity(
    @PrimaryKey val id: String,
    val cookieId: String,
    val mode: String,
    val title: String,
    val summary: String,
    val toppingsJson: String,
    val tartJson: String?,
    val substatsJson: String,
    val beascuitJson: String,
    val notes: List<String>,
)

data class CookieWithBuilds(
    @Embedded val cookie: CookieEntity,
    @Relation(parentColumn = "id", entityColumn = "cookieId")
    val builds: List<CookieBuildEntity>,
)

/** User data — intentionally no FK so it survives seed refreshes. */
@Serializable
@Entity(tableName = "owned_cookies")
data class OwnedCookieEntity(
    @PrimaryKey val cookieId: String,
    val level: Int,
    val stars: Int,
    val ascension: Int,
    val power: Long,
    val skillLevel: Int,
    val candyLevel: Int,
    val toppingsJson: String,
    val tart: String,
    val beascuitGrade: String?,
    val beascuitElement: String?,
    val currentStatsJson: String,
    val checklistJson: String,
    val notes: String,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mode: String,
    /** Exactly 5 entries; empty string = empty slot. */
    val slots: List<String>,
    val treasureIds: List<String>,
    val notes: String,
    val updatedAt: Long,
)

@Entity(tableName = "treasures")
data class TreasureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val effect: String,
    val tags: List<String>,
    val modes: List<String>,
)

@Entity(tableName = "drop_items")
data class DropItemEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val element: String?,
    val description: String,
    val sourcesJson: String,
)

@Entity(tableName = "tier_entries", primaryKeys = ["mode", "cookieId"])
data class TierEntryEntity(
    val mode: String,
    val cookieId: String,
    val tier: String,
    val rank: Int,
    val note: String,
)

@Serializable
@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val code: String,
    val rewards: String,
    val expiresAt: Long?,
    val verified: Boolean,
    val userAdded: Boolean,
    val redeemed: Boolean,
    val addedAt: Long,
)

@Entity(tableName = "seed_meta")
data class SeedMetaEntity(
    @PrimaryKey val key: String,
    val version: Int,
)
