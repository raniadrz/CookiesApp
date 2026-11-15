package com.crumbcodex.crk.data.backup

import androidx.room.withTransaction
import com.crumbcodex.crk.data.local.CrkDatabase
import com.crumbcodex.crk.domain.repository.BackupRepository
import com.crumbcodex.crk.domain.repository.ImportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepositoryImpl(
    private val db: CrkDatabase,
    private val codec: BackupCodec = BackupCodec(),
    private val clock: () -> Long = System::currentTimeMillis,
) : BackupRepository {

    override suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        codec.encode(
            BackupFile(
                exportedAt = clock(),
                owned = db.collectionDao().getAll(),
                teams = db.teamDao().getAll(),
                // Seed coupons are re-delivered by data updates; keep only what the user added or redeemed.
                coupons = db.couponDao().getAll().filter { it.userAdded || it.redeemed },
            ),
        )
    }

    override suspend fun importJson(raw: String): ImportSummary = withContext(Dispatchers.IO) {
        val file = codec.decode(raw) // throws BackupFormatException before anything is written
        db.withTransaction {
            file.owned.forEach { db.collectionDao().upsert(it) }

            val existing = db.teamDao().getAll().map { Triple(it.name, it.mode, it.slots) }.toMutableSet()
            var added = 0
            var skipped = 0
            file.teams.forEach { t ->
                val key = Triple(t.name, t.mode, t.slots)
                if (key in existing) {
                    skipped++
                } else {
                    db.teamDao().upsert(t.copy(id = 0))
                    existing += key
                    added++
                }
            }

            val codes = db.couponDao().allCodes().toSet()
            file.coupons.forEach { c ->
                when {
                    c.code !in codes -> db.couponDao().insert(c)
                    c.redeemed -> db.couponDao().setRedeemed(c.code, true)
                }
            }
            ImportSummary(file.owned.size, added, skipped, file.coupons.size)
        }
    }
}
