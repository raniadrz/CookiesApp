package com.crumbcodex.crk.data.repository

import com.crumbcodex.crk.data.local.dao.CollectionDao
import com.crumbcodex.crk.data.local.dao.CookieDao
import com.crumbcodex.crk.data.local.dao.CouponDao
import com.crumbcodex.crk.data.local.dao.GameDataDao
import com.crumbcodex.crk.data.local.dao.TeamDao
import com.crumbcodex.crk.data.local.entity.CouponEntity
import com.crumbcodex.crk.data.mapper.Mappers
import com.crumbcodex.crk.domain.model.Cookie
import com.crumbcodex.crk.domain.model.Coupon
import com.crumbcodex.crk.domain.model.DropItem
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.OwnedCookie
import com.crumbcodex.crk.domain.model.Team
import com.crumbcodex.crk.domain.model.TierEntry
import com.crumbcodex.crk.domain.model.Treasure
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.repository.CouponRepository
import com.crumbcodex.crk.domain.repository.GameDataRepository
import com.crumbcodex.crk.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CookieRepositoryImpl(
    private val cookieDao: CookieDao,
    private val collectionDao: CollectionDao,
    private val mappers: Mappers,
) : CookieRepository {

    private val ownedIds: Flow<Set<String>> =
        collectionDao.observeAll().map { list -> list.mapTo(HashSet()) { it.cookieId } }.distinctUntilChanged()

    override fun observeCookies(): Flow<List<Cookie>> =
        combine(cookieDao.observeAll(), ownedIds) { rows, owned ->
            rows.map { mappers.toDomain(it, it.cookie.id in owned) }
        }

    override fun observeCookie(id: String): Flow<Cookie?> =
        combine(cookieDao.observe(id), ownedIds) { row, owned ->
            row?.let { mappers.toDomain(it, id in owned) }
        }
}

class CollectionRepositoryImpl(
    private val dao: CollectionDao,
    private val mappers: Mappers,
    private val clock: () -> Long = System::currentTimeMillis,
) : CollectionRepository {
    override fun observeOwned(): Flow<List<OwnedCookie>> = dao.observeAll().map { it.map { mappers.toDomain(it) } }

    override fun observeOwned(cookieId: String): Flow<OwnedCookie?> =
        dao.observe(cookieId).map { it?.let { mappers.toDomain(it) } }

    override suspend fun setOwned(cookieId: String, owned: Boolean) {
        if (owned) {
            if (dao.get(cookieId) == null) dao.upsert(mappers.toEntity(OwnedCookie.newFor(cookieId, clock())))
        } else {
            dao.delete(cookieId)
        }
    }

    override suspend fun save(owned: OwnedCookie) {
        dao.upsert(mappers.toEntity(owned.copy(updatedAt = clock())))
    }
}

class TeamRepositoryImpl(
    private val dao: TeamDao,
    private val mappers: Mappers,
    private val clock: () -> Long = System::currentTimeMillis,
) : TeamRepository {
    override fun observeTeams(): Flow<List<Team>> = dao.observeAll().map { it.map { mappers.toDomain(it) } }
    override fun observeTeam(id: Long): Flow<Team?> = dao.observe(id).map { it?.let { mappers.toDomain(it) } }

    override suspend fun save(team: Team): Long {
        val rowId = dao.upsert(mappers.toEntity(team.copy(updatedAt = clock())))
        return if (team.id != 0L) team.id else rowId
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}

class GameDataRepositoryImpl(
    private val dao: GameDataDao,
    private val cookieRepository: CookieRepository,
    private val mappers: Mappers,
) : GameDataRepository {
    override fun observeTreasures(): Flow<List<Treasure>> = dao.observeTreasures().map { it.map { mappers.toDomain(it) } }
    override fun observeDrops(): Flow<List<DropItem>> = dao.observeDrops().map { it.map { mappers.toDomain(it) } }

    override fun observeTierList(mode: GameMode): Flow<List<TierEntry>> =
        combine(dao.observeTiers(mode.name), cookieRepository.observeCookies()) { tiers, cookies ->
            val byId = cookies.associateBy { it.id }
            tiers.mapNotNull { t ->
                byId[t.cookieId]?.let { TierEntry(mode, t.tier, t.rank, t.note, it) }
            }
        }
}

class CouponRepositoryImpl(
    private val dao: CouponDao,
    private val mappers: Mappers,
    private val clock: () -> Long = System::currentTimeMillis,
) : CouponRepository {
    override fun observeCoupons(): Flow<List<Coupon>> = dao.observeAll().map { it.map { mappers.toDomain(it) } }

    override suspend fun add(code: String, rewards: String, expiresAt: Long?) {
        val clean = code.trim().uppercase()
        require(clean.isNotEmpty()) { "Code must not be empty" }
        dao.insert(
            CouponEntity(
                code = clean,
                rewards = rewards.trim(),
                expiresAt = expiresAt,
                verified = false,
                userAdded = true,
                redeemed = false,
                addedAt = clock(),
            ),
        )
    }

    override suspend fun setRedeemed(code: String, redeemed: Boolean) = dao.setRedeemed(code, redeemed)
    override suspend fun delete(code: String) = dao.delete(code)
}
