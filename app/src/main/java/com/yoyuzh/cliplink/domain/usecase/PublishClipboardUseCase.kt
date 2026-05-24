package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.domain.error.DomainError
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PublishClipboardUseCase @Inject constructor(
    private val repository: ClipboardRepository,
    private val settingsStore: AppSettingsStore,
    private val credentialStore: DeviceCredentialStore
) {
    suspend operator fun invoke(recordId: String): Result<ClipboardRecord> {
        val settings = settingsStore.settings.first()
        if (!settings.manualPublishEnabled) {
            return Result.failure(IllegalStateException("Manual publish is disabled"))
        }
        if (settings.serverUrl.isBlank() || settings.deviceId.isBlank()) {
            return Result.failure(IllegalStateException("Device not registered"))
        }
        val credentials = credentialStore.credentials.first()
        if (credentials.token.isBlank() || credentials.serverUrl != settings.serverUrl) {
            return Result.failure(IllegalStateException("Device token is not available"))
        }
        val result = repository.publish(recordId, settings.serverUrl, credentials.token, settings.deviceId)
        if (result.exceptionOrNull() is DomainError.Unauthorized) {
            credentialStore.clearToken()
        }
        return result
    }
}
