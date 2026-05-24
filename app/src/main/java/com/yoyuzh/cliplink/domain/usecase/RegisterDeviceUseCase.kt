package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Registers this Android device with the sync server.
 * On success, stores the deviceId and token via AppSettingsStore.
 */
class RegisterDeviceUseCase @Inject constructor(
    private val repository: ClipboardRepository,
    private val settingsStore: AppSettingsStore,
    private val credentialStore: DeviceCredentialStore
) {
    suspend operator fun invoke(): Result<Unit> {
        val settings = settingsStore.settings.first()
        if (settings.serverUrl.isBlank()) {
            return Result.failure(IllegalStateException("Server URL is not configured"))
        }

        return repository.registerDevice(settings).onSuccess { (deviceId, token) ->
            settingsStore.updateDeviceId(deviceId)
            credentialStore.saveToken(settings.serverUrl, token)
        }.map { }
    }
}
