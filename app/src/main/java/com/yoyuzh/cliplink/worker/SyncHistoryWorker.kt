package com.yoyuzh.cliplink.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import com.yoyuzh.cliplink.domain.usecase.FetchRemoteHistoryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncHistoryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ClipboardRepository,
    private val settingsStore: AppSettingsStore,
    private val fetchRemoteHistory: FetchRemoteHistoryUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsStore.settings.first()

        // Nothing to sync if server is not configured
        if (settings.serverUrl.isBlank() || settings.deviceId.isBlank()) {
            return Result.success()
        }

        // Fetch and merge remote history
        val historyResult = fetchRemoteHistory(days = settings.syncWindowDays)
        if (historyResult.isFailure) {
            return Result.retry()
        }

        // Trim local history to configured max
        repository.trimLocalHistory(settings.maxLocalHistoryItems)

        return Result.success()
    }
}
