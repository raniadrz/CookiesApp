package com.crumbcodex.crk.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.crumbcodex.crk.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id")
    fun observe(id: Long): Flow<TeamEntity?>

    /** Returns the new row id on insert, or -1 when an existing row was updated. */
    @Upsert
    suspend fun upsert(team: TeamEntity): Long

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun delete(id: Long)
}
