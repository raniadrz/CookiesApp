package com.crumbcodex.crk.di

import android.content.Context
import com.crumbcodex.crk.data.local.CrkDatabase
import com.crumbcodex.crk.data.mapper.Mappers
import com.crumbcodex.crk.data.prefs.ThemePreferences
import com.crumbcodex.crk.data.repository.CollectionRepositoryImpl
import com.crumbcodex.crk.data.repository.CookieRepositoryImpl
import com.crumbcodex.crk.data.repository.CouponRepositoryImpl
import com.crumbcodex.crk.data.repository.GameDataRepositoryImpl
import com.crumbcodex.crk.data.repository.TeamRepositoryImpl
import com.crumbcodex.crk.BuildConfig
import com.crumbcodex.crk.data.seed.DataUpdateManager
import com.crumbcodex.crk.data.seed.DatabaseSeeder
import com.crumbcodex.crk.data.seed.RemoteSeedSource
import com.crumbcodex.crk.data.seed.SeedDataParser
import com.crumbcodex.crk.domain.repository.CollectionRepository
import com.crumbcodex.crk.domain.repository.CookieRepository
import com.crumbcodex.crk.domain.repository.CouponRepository
import com.crumbcodex.crk.domain.repository.DataUpdateRepository
import com.crumbcodex.crk.domain.repository.GameDataRepository
import com.crumbcodex.crk.domain.repository.SeedRepository
import com.crumbcodex.crk.domain.repository.TeamRepository
import com.crumbcodex.crk.domain.usecase.CompareBuildUseCase
import com.crumbcodex.crk.domain.usecase.FilterCookiesUseCase
import com.crumbcodex.crk.domain.usecase.TeamSynergyCalculator
import kotlinx.serialization.json.Json

/** Manual dependency injection: one instance per process, owned by the Application. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val database: CrkDatabase by lazy { CrkDatabase.create(appContext) }
    private val mappers = Mappers(json)

    val themePreferences by lazy { ThemePreferences(appContext) }

    private val seeder: DatabaseSeeder by lazy { DatabaseSeeder(appContext, database, SeedDataParser(), mappers) }
    val seedRepository: SeedRepository get() = seeder
    val dataUpdates: DataUpdateRepository by lazy {
        DataUpdateManager(appContext, seeder, RemoteSeedSource(BuildConfig.SEED_BASE_URL))
    }
    val cookieRepository: CookieRepository by lazy {
        CookieRepositoryImpl(database.cookieDao(), database.collectionDao(), mappers)
    }
    val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(database.collectionDao(), mappers)
    }
    val teamRepository: TeamRepository by lazy { TeamRepositoryImpl(database.teamDao(), mappers) }
    val gameDataRepository: GameDataRepository by lazy {
        GameDataRepositoryImpl(database.gameDataDao(), cookieRepository, mappers)
    }
    val couponRepository: CouponRepository by lazy { CouponRepositoryImpl(database.couponDao(), mappers) }

    val filterCookies = FilterCookiesUseCase()
    val teamSynergy = TeamSynergyCalculator()
    val compareBuild = CompareBuildUseCase()
}
