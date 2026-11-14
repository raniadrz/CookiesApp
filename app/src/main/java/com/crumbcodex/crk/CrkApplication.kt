package com.crumbcodex.crk

import android.app.Application
import android.util.Log
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

class CrkApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _seedState = MutableStateFlow<SeedState>(SeedState.Loading)
    val seedState: StateFlow<SeedState> = _seedState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        reseed()
    }

    fun reseed() {
        _seedState.value = SeedState.Loading
        appScope.launch {
            _seedState.value = runCatching { container.seedRepository.ensureSeeded() }
                .fold(
                    onSuccess = { SeedState.Ready },
                    onFailure = {
                        Log.e("CrkApplication", "Seeding failed", it)
                        SeedState.Failed(it.message ?: it::class.java.simpleName)
                    },
                )
        }
    }
}
