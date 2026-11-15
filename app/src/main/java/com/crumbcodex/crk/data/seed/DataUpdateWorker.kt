package com.crumbcodex.crk.data.seed

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.crumbcodex.crk.CrkApplication
import com.crumbcodex.crk.domain.repository.UpdateResult
import java.util.concurrent.TimeUnit

/** Daily background check for new data / coupon codes, even while the app is closed. */
class DataUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CrkApplication).container
        container.seedRepository.ensureSeeded()
        return when (container.dataUpdates.checkForUpdates()) {
            is UpdateResult.Failed -> if (runAttemptCount < 3) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    companion object {
        private const val NAME = "daily-data-update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DataUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
