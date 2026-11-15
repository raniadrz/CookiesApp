package com.crumbcodex.crk.domain.repository

import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.Coupon
import com.crumbcodex.crk.domain.model.DropItem
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.TierEntry
import com.crumbcodex.crk.domain.model.Treasure
import kotlinx.coroutines.flow.Flow

interface CookieRepository {
    fun observeCookies(): Flow<List<Cookie>>
    fun observeCookie(id: String): Flow<Cookie?>
}

interface CollectionRepository {
    fun observeOwned(): Flow<List<OwnedCookie>>
    fun observeOwned(cookieId: String): Flow<OwnedCookie?>
    suspend fun setOwned(cookieId: String, owned: Boolean)
    suspend fun save(owned: OwnedCookie)
}

interface TeamRepository {
    fun observeTeams(): Flow<List<Team>>
    fun observeTeam(id: Long): Flow<Team?>
    suspend fun save(team: Team): Long
    suspend fun delete(id: Long)
}

interface GameDataRepository {
    fun observeTreasures(): Flow<List<Treasure>>
    fun observeDrops(): Flow<List<DropItem>>
    fun observeTierList(mode: GameMode): Flow<List<TierEntry>>
}

interface CouponRepository {
    fun observeCoupons(): Flow<List<Coupon>>
    suspend fun add(code: String, rewards: String, expiresAt: Long?)
    suspend fun setRedeemed(code: String, redeemed: Boolean)
    suspend fun delete(code: String)
}

interface SeedRepository {
    /** Imports bundled JSON seed data if its version is newer than what is stored. */
    suspend fun ensureSeeded()
}

/** Outcome of an over-the-air data check. */
sealed interface UpdateResult {
    data class Updated(val cookiesVersion: Int?, val gameDataVersion: Int?, val newCoupons: List<String> = emptyList()) : UpdateResult
    data object UpToDate : UpdateResult
    data class Failed(val message: String) : UpdateResult
}

data class DataStatus(
    val cookiesVersion: Int? = null,
    val gameDataVersion: Int? = null,
    val lastCheckedAt: Long? = null,
    val checking: Boolean = false,
)

/** Downloads newer seed JSON from the published data URL, so data updates don't need a new APK. */
interface DataUpdateRepository {
    val status: kotlinx.coroutines.flow.StateFlow<DataStatus>
    suspend fun refreshStatus()
    suspend fun checkForUpdates(): UpdateResult
    /** Runs [checkForUpdates] if the last check is older than the automatic interval. */
    suspend fun checkIfDue()
}

data class ImportSummary(val cookies: Int, val teamsAdded: Int, val teamsSkipped: Int, val coupons: Int)

/** Export / import of all user data (collection, teams, user coupons and redeemed marks). */
interface BackupRepository {
    suspend fun exportJson(): String
    /** Merges a backup into the current data; never deletes anything. */
    suspend fun importJson(raw: String): ImportSummary
}
