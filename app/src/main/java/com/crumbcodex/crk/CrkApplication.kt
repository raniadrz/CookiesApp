package com.crumbcodex.crk

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.crumbcodex.crk.data.seed.DataUpdateWorker
import com.crumbcodex.crk.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SeedState {
    data object Loading : SeedState
    data object Ready : SeedState
    data class Failed(val message: String) : SeedState
}

class CrkApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _seedState = MutableStateFlow<SeedState>(SeedState.Loading)
    val seedState: StateFlow<SeedState> = _seedState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        reseed()
        DataUpdateWorker.schedule(this)
    }

    /** Cookie artwork is fetched at runtime (never bundled) and cached on disk for offline use. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("cookie_art")).maxSizeBytes(150L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()

    fun reseed() {
        _seedState.value = SeedState.Loading
        appScope.launch {
            _seedState.value = runCatching { container.seedRepository.ensureSeeded() }
                .fold(
                    onSuccess = {
                        // Bundled data is in place; pull newer data over the air at most once a day.
                        launch {
                            container.dataUpdates.refreshStatus()
                            container.dataUpdates.checkIfDue()
                        }
                        SeedState.Ready
                    },
                    onFailure = {
                        Log.e("CrkApplication", "Seeding failed", it)
                        SeedState.Failed(it.message ?: it::class.java.simpleName)
                    },
                )
        }
    }
}
