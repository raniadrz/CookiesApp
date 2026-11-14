package com.crumbcodex.crk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crumbcodex.crk.data.local.dao.CollectionDao
import com.crumbcodex.crk.data.local.dao.CookieDao
import com.crumbcodex.crk.data.local.dao.CouponDao
import com.crumbcodex.crk.data.local.dao.GameDataDao
import com.crumbcodex.crk.data.local.dao.TeamDao
import com.crumbcodex.crk.data.local.entity.CookieBuildEntity
import com.crumbcodex.crk.data.local.entity.CookieEntity
import com.crumbcodex.crk.data.local.entity.CouponEntity
import com.crumbcodex.crk.data.local.entity.DropItemEntity
import com.crumbcodex.crk.data.local.entity.OwnedCookieEntity
import com.crumbcodex.crk.data.local.entity.SeedMetaEntity
import com.crumbcodex.crk.data.local.entity.TeamEntity
import com.crumbcodex.crk.data.local.entity.TierEntryEntity
import com.crumbcodex.crk.data.local.entity.TreasureEntity

@Database(
    entities = [
        CookieEntity::class,
        CookieBuildEntity::class,
        OwnedCookieEntity::class,
        TeamEntity::class,
        TreasureEntity::class,
        DropItemEntity::class,
        TierEntryEntity::class,
        CouponEntity::class,
        SeedMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CrkDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun collectionDao(): CollectionDao
    abstract fun teamDao(): TeamDao
    abstract fun gameDataDao(): GameDataDao
    abstract fun couponDao(): CouponDao

    companion object {
        private const val NAME = "crumb_codex.db"

        fun create(context: Context): CrkDatabase =
            Room.databaseBuilder(context.applicationContext, CrkDatabase::class.java, NAME)
                .build()
    }
}
