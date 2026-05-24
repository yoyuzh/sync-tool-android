package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.domain.error.DomainError
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fetches remote history from the server and merges it into local Room DB.
 * Returns the count of records merged.
 */
class FetchRemoteHistoryUseCase @Inject constructor(
    private val repository: ClipboardRepository,
    private val settingsStore: AppSettingsStore,
    private val credentialStore: DeviceCredentialStore
) {
    suspend operator fun invoke(days: Int = 7): Result<Int> {
        val settings = settingsStore.settings.first()
        if (settings.serverUrl.isBlank() || settings.deviceId.isBlank()) {
            return Result.failure(IllegalStateException("Device not registered"))
        }
        val credentials = credentialStore.credentials.first()
        if (credentials.token.isBlank() || credentials.serverUrl != settings.serverUrl) {
            return Result.failure(IllegalStateException("Device token is not available"))
        }
        val result = repository.fetchAndMergeHistory(credentials.token, settings.serverUrl, days)
        if (result.exceptionOrNull() is DomainError.Unauthorized) {
            credentialStore.clearToken()
        }
        return result
    }
}
