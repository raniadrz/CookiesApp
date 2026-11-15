package com.crumbcodex.crk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.crumbcodex.crk.data.local.entity.OwnedCookieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM owned_cookies ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<OwnedCookieEntity>>

    @Query("SELECT * FROM owned_cookies WHERE cookieId = :cookieId")
    fun observe(cookieId: String): Flow<OwnedCookieEntity?>

    @Query("SELECT * FROM owned_cookies WHERE cookieId = :cookieId")
    suspend fun get(cookieId: String): OwnedCookieEntity?

    @Query("SELECT * FROM owned_cookies")
    suspend fun getAll(): List<OwnedCookieEntity>

    @Upsert
    suspend fun upsert(entity: OwnedCookieEntity)

    @Query("DELETE FROM owned_cookies WHERE cookieId = :cookieId")
    suspend fun delete(cookieId: String)
}
