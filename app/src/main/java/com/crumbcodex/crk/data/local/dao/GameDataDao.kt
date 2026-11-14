package com.crumbcodex.crk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crumbcodex.crk.data.local.entity.DropItemEntity
import com.crumbcodex.crk.data.local.entity.SeedMetaEntity
import com.crumbcodex.crk.data.local.entity.TierEntryEntity
import com.crumbcodex.crk.data.local.entity.TreasureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDataDao {
    @Query("SELECT * FROM treasures ORDER BY name")
    fun observeTreasures(): Flow<List<TreasureEntity>>

    @Query("SELECT * FROM drop_items ORDER BY category, name")
    fun observeDrops(): Flow<List<DropItemEntity>>

    @Query("SELECT * FROM tier_entries WHERE mode = :mode ORDER BY rank")
    fun observeTiers(mode: String): Flow<List<TierEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreasures(items: List<TreasureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrops(items: List<DropItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiers(items: List<TierEntryEntity>)

    @Query("DELETE FROM treasures") suspend fun clearTreasures()
    @Query("DELETE FROM drop_items") suspend fun clearDrops()
    @Query("DELETE FROM tier_entries") suspend fun clearTiers()

    @Query("SELECT version FROM seed_meta WHERE `key` = :key")
    suspend fun seedVersion(key: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSeedVersion(meta: SeedMetaEntity)
}
