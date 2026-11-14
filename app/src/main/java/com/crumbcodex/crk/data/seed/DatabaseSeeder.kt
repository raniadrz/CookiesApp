package com.crumbcodex.crk.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.crumbcodex.crk.data.local.CrkDatabase
import com.crumbcodex.crk.data.local.entity.CouponEntity
import com.crumbcodex.crk.data.local.entity.SeedMetaEntity
import com.crumbcodex.crk.data.local.entity.TierEntryEntity
import com.crumbcodex.crk.data.mapper.Mappers
import com.crumbcodex.crk.domain.repository.SeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SyncOutcome(
    val importedCookies: Boolean,
    val importedGameData: Boolean,
    val cookiesVersion: Int,
    val gameDataVersion: Int,
)

/**
 * Imports seed JSON (bundled assets or a downloaded copy) into Room. Static tables are replaced in one
 * transaction when a file's version increases; user tables (owned cookies, teams, user-added coupons) are
 * never touched. Both files are parsed and validated before anything is written.
 */
class DatabaseSeeder(
    private val context: Context,
    private val db: CrkDatabase,
    private val parser: SeedDataParser,
    private val mappers: Mappers,
    private val clock: () -> Long = System::currentTimeMillis,
) : SeedRepository {

    private val mutex = Mutex()

    override suspend fun ensureSeeded() {
        val outcome = sync(readAsset(COOKIES_ASSET), readAsset(GAME_DATA_ASSET))
        Log.i(TAG, "Bundled seed check: $outcome")
    }

    /** Validates and imports the given raw JSON files; returns what (if anything) was imported. */
    suspend fun sync(cookiesRaw: String, gameDataRaw: String): SyncOutcome = mutex.withLock {
        withContext(Dispatchers.IO) {
            val cookiesFile = parser.parseCookies(cookiesRaw)
            val gameFile = parser.parseGameData(gameDataRaw, cookiesFile.cookies.map { it.id }.toSet())
            val dao = db.gameDataDao()
            val cookieDao = db.cookieDao()
            val plan = SeedSyncPlanner.plan(
                storedCookies = dao.seedVersion(KEY_COOKIES),
                storedGameData = dao.seedVersion(KEY_GAME_DATA),
                newCookies = cookiesFile.version,
                newGameData = gameFile.version,
                hasCookies = cookieDao.count() > 0,
            )
            val outcome = SyncOutcome(plan.importCookies, plan.importGameData, cookiesFile.version, gameFile.version)
            if (plan.isNoop) return@withContext outcome

            val now = clock()
            db.withTransaction {
                if (plan.importCookies) {
                    cookieDao.clearBuilds()
                    cookieDao.deleteCookiesNotIn(cookiesFile.cookies.map { it.id })
                    cookieDao.insertCookies(cookiesFile.cookies.map { mappers.toEntity(it) })
                    cookieDao.insertBuilds(cookiesFile.cookies.flatMap { c -> c.builds.map { mappers.toEntity(c.id, it) } })
                    dao.setSeedVersion(SeedMetaEntity(KEY_COOKIES, cookiesFile.version))
                }
                if (plan.importGameData) {
                    dao.clearTreasures(); dao.insertTreasures(gameFile.treasures.map { mappers.toEntity(it) })
                    dao.clearDrops(); dao.insertDrops(gameFile.drops.map { mappers.toEntity(it) })
                    dao.clearTiers()
                    dao.insertTiers(
                        gameFile.tierLists.flatMap { (mode, groups) ->
                            var rank = 0
                            groups.flatMap { g ->
                                g.cookies.map { id -> TierEntryEntity(mode, id, g.tier, rank++, g.notes[id].orEmpty()) }
                            }
                        },
                    )
                    db.couponDao().insertIfAbsent(
                        gameFile.coupons.map {
                            CouponEntity(
                                code = it.code.trim().uppercase(),
                                rewards = it.rewards,
                                expiresAt = it.expires?.let { d -> SeedDataParser.parseExpiry(d) },
                                verified = it.verified,
                                userAdded = false,
                                redeemed = false,
                                addedAt = now,
                            )
                        },
                    )
                    dao.setSeedVersion(SeedMetaEntity(KEY_GAME_DATA, gameFile.version))
                }
            }
            outcome
        }
    }

    /** Stored (cookies, game data) versions, or null when not seeded yet. */
    suspend fun storedVersions(): Pair<Int?, Int?> = withContext(Dispatchers.IO) {
        db.gameDataDao().seedVersion(KEY_COOKIES) to db.gameDataDao().seedVersion(KEY_GAME_DATA)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        private const val TAG = "DatabaseSeeder"
        const val COOKIES_ASSET = "seed/cookies.json"
        const val GAME_DATA_ASSET = "seed/game_data.json"
        const val COOKIES_FILE = "cookies.json"
        const val GAME_DATA_FILE = "game_data.json"
        private const val KEY_COOKIES = "cookies"
        private const val KEY_GAME_DATA = "game_data"
    }
}
