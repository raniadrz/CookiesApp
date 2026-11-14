package com.crumbcodex.crk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crumbcodex.crk.data.local.entity.CouponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons ORDER BY redeemed ASC, (expiresAt IS NULL) ASC, expiresAt ASC, addedAt DESC")
    fun observeAll(): Flow<List<CouponEntity>>

    /** Seed coupons never overwrite a user's own changes. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(items: List<CouponEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CouponEntity)

    @Query("UPDATE coupons SET redeemed = :redeemed WHERE code = :code")
    suspend fun setRedeemed(code: String, redeemed: Boolean)

    @Query("DELETE FROM coupons WHERE code = :code")
    suspend fun delete(code: String)
}
