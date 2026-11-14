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

/**
 * Imports assets/seed/cookies.json and assets/seed/game_data.json into Room.
 * Static data is fully replaced whenever the combined seed version increases; user tables
 * (owned cookies, teams, user-added coupons) are never touched.
 */
class DatabaseSeeder(
    private val context: Context,
    private val db: CrkDatabase,
    private val parser: SeedDataParser,
    private val mappers: Mappers,
    private val clock: () -> Long = System::currentTimeMillis,
) : SeedRepository {

    private val mutex = Mutex()

    override suspend fun ensureSeeded() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val cookiesFile = parser.parseCookies(readAsset(COOKIES_ASSET))
            val gameFile = parser.parseGameData(
                readAsset(GAME_DATA_ASSET),
                cookiesFile.cookies.map { it.id }.toSet(),
            )
            val combinedVersion = cookiesFile.version * 10_000 + gameFile.version
            val dao = db.gameDataDao()
            val stored = dao.seedVersion(SEED_KEY)
            if (stored != null && stored >= combinedVersion && db.cookieDao().count() > 0) return@withContext

            val now = clock()
            db.withTransaction {
                val cookieDao = db.cookieDao()
                cookieDao.clearBuilds()
                cookieDao.deleteCookiesNotIn(cookiesFile.cookies.map { it.id })
                cookieDao.insertCookies(cookiesFile.cookies.map { mappers.toEntity(it) })
                cookieDao.insertBuilds(cookiesFile.cookies.flatMap { c -> c.builds.map { mappers.toEntity(c.id, it) } })

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
                dao.setSeedVersion(SeedMetaEntity(SEED_KEY, combinedVersion))
            }
            Log.i(TAG, "Seeded ${cookiesFile.cookies.size} cookies (seed v$combinedVersion)")
        }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        private const val TAG = "DatabaseSeeder"
        const val COOKIES_ASSET = "seed/cookies.json"
        const val GAME_DATA_ASSET = "seed/game_data.json"
        private const val SEED_KEY = "static_data"
    }
}
