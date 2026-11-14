package com.crumbcodex.crk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crumbcodex.crk.data.local.entity.CookieBuildEntity
import com.crumbcodex.crk.data.local.entity.CookieEntity
import com.crumbcodex.crk.data.local.entity.CookieWithBuilds
import kotlinx.coroutines.flow.Flow

@Dao
interface CookieDao {
    @Transaction
    @Query("SELECT * FROM cookies ORDER BY name")
    fun observeAll(): Flow<List<CookieWithBuilds>>

    @Transaction
    @Query("SELECT * FROM cookies WHERE id = :id")
    fun observe(id: String): Flow<CookieWithBuilds?>

    @Query("SELECT COUNT(*) FROM cookies")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCookies(cookies: List<CookieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuilds(builds: List<CookieBuildEntity>)

    @Query("DELETE FROM cookie_builds")
    suspend fun clearBuilds()

    @Query("DELETE FROM cookies WHERE id NOT IN (:keepIds)")
    suspend fun deleteCookiesNotIn(keepIds: List<String>)
}
