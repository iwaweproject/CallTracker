package com.example.calltracker.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodically re-syncs the system call log so the app's history stays current
 * even if the user does not open it. Minimum WorkManager interval is 15 min.
 */
class CallLogSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        CallLogRepository(applicationContext).sync()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CallLogSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "call_log_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
