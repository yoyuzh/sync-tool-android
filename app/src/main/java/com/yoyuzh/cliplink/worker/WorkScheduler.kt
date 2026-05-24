package com.yoyuzh.cliplink.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    /** Enqueue periodic background history sync (15-minute interval). */
    fun scheduleHistorySync() {
        workManager.enqueueUniquePeriodicWork(
            HISTORY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            buildHistorySyncRequest()
        )
    }

    /**
     * Trigger a one-time immediate sync (e.g. after publish, settings change,
     * manual refresh, or foreground session reconnect).
     */
    fun triggerImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncHistoryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val HISTORY_SYNC_WORK_NAME = "cliplink-history-sync"
        const val ONE_TIME_SYNC_WORK_NAME = "cliplink-history-sync-immediate"

        fun buildHistorySyncRequest() = PeriodicWorkRequestBuilder<SyncHistoryWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
    }
}
