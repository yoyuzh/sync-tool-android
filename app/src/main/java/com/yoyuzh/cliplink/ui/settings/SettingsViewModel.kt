package com.yoyuzh.cliplink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.domain.model.AppSettings
import com.yoyuzh.cliplink.domain.usecase.RegisterDeviceUseCase
import com.yoyuzh.cliplink.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isRegistering: Boolean = false,
    val registrationResult: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    private val isRegistering = MutableStateFlow(false)
    private val registrationResult = MutableStateFlow<String?>(null)

    val settingsFlow: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.settings,
        isRegistering,
        registrationResult
    ) { settings, registering, result ->
        SettingsUiState(settings, registering, result)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateServerUrl(url: String) {
        viewModelScope.launch { settingsStore.updateServerUrl(url) }
    }

    fun updateDeviceName(name: String) {
        viewModelScope.launch { settingsStore.updateDeviceName(name) }
    }

    fun updateManualPublishEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateManualPublishEnabled(enabled) }
    }

    fun updateNotificationPreviewEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateNotificationPreviewEnabled(enabled) }
    }

    fun updateMaxLocalHistoryItems(count: Int) {
        viewModelScope.launch { settingsStore.updateMaxLocalHistoryItems(count) }
    }

    fun updateSyncWindowDays(days: Int) {
        viewModelScope.launch { settingsStore.updateSyncWindowDays(days) }
    }

    fun registerDevice() {
        viewModelScope.launch {
            registerWithCurrentSettings()
        }
    }

    fun saveAndRegisterDevice(serverUrl: String, deviceName: String) {
        viewModelScope.launch {
            settingsStore.updateServerUrl(normalizeServerUrl(serverUrl))
            settingsStore.updateDeviceName(deviceName.trim().ifBlank { "My Android" })
            registerWithCurrentSettings()
        }
    }

    private suspend fun registerWithCurrentSettings() {
            isRegistering.value = true
            registrationResult.value = null
            val result = registerDeviceUseCase()
            isRegistering.value = false
            registrationResult.value = if (result.isSuccess) {
                workScheduler.triggerImmediateSync()
                "注册成功"
            } else {
                "注册失败: ${result.exceptionOrNull()?.message}"
            }
    }

    private fun normalizeServerUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return "http://$trimmed"
    }
}
