package com.crumbcodex.crk.data.seed

import android.content.Context
import android.util.Log
import com.crumbcodex.crk.domain.repository.DataStatus
import com.crumbcodex.crk.domain.repository.DataUpdateRepository
import com.crumbcodex.crk.domain.repository.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

class DataUpdateManager(
    context: Context,
    private val seeder: DatabaseSeeder,
    private val remote: RemoteSeedSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : DataUpdateRepository {

    private val prefs = context.getSharedPreferences("crumb_codex_sync", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _status = MutableStateFlow(DataStatus(lastCheckedAt = prefs.getLong(KEY_LAST_CHECK, 0L).takeIf { it > 0 }))
    override val status: StateFlow<DataStatus> = _status.asStateFlow()

    override suspend fun refreshStatus() {
        val (c, g) = seeder.storedVersions()
        _status.update { it.copy(cookiesVersion = c, gameDataVersion = g) }
    }

    override suspend fun checkForUpdates(): UpdateResult {
        if (!mutex.tryLock()) return UpdateResult.Failed("A check is already running")
        _status.update { it.copy(checking = true) }
        return try {
            val (cookiesRaw, gameRaw) = withContext(Dispatchers.IO) {
                remote.fetch(DatabaseSeeder.COOKIES_FILE) to remote.fetch(DatabaseSeeder.GAME_DATA_FILE)
            }
            val outcome = seeder.sync(cookiesRaw, gameRaw)
            val now = clock()
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
            _status.update { it.copy(lastCheckedAt = now) }
            refreshStatus()
            if (outcome.importedCookies || outcome.importedGameData) {
                UpdateResult.Updated(
                    cookiesVersion = outcome.cookiesVersion.takeIf { outcome.importedCookies },
                    gameDataVersion = outcome.gameDataVersion.takeIf { outcome.importedGameData },
                )
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: SerializationException) {
            Log.w(TAG, "Remote data invalid", e)
            UpdateResult.Failed("Downloaded data was invalid, so nothing was changed")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Remote data rejected", e)
            UpdateResult.Failed("Downloaded data failed validation, so nothing was changed")
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed", e)
            UpdateResult.Failed("Couldn't reach the data server (${e.message ?: e::class.java.simpleName})")
        } finally {
            _status.update { it.copy(checking = false) }
            mutex.unlock()
        }
    }

    override suspend fun checkIfDue() {
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (clock() - last >= AUTO_INTERVAL_MS) checkForUpdates()
    }

    private companion object {
        const val TAG = "DataUpdateManager"
        const val KEY_LAST_CHECK = "last_check"
        const val AUTO_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
