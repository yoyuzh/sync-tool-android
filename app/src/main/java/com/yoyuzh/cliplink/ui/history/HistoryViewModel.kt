package com.yoyuzh.cliplink.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoyuzh.cliplink.data.remote.ws.SessionState
import com.yoyuzh.cliplink.data.remote.ws.SyncWebSocketClient
import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.usecase.CaptureClipboardUseCase
import com.yoyuzh.cliplink.domain.usecase.FetchRemoteHistoryUseCase
import com.yoyuzh.cliplink.domain.usecase.GetLocalHistoryUseCase
import com.yoyuzh.cliplink.domain.usecase.PublishClipboardUseCase
import com.yoyuzh.cliplink.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val query: String = "",
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val selectedDayRange: Int = 7,
    val records: List<ClipboardRecord> = emptyList(),
    val sessionState: SessionState = SessionState.Disconnected,
    val captureResult: CaptureResult? = null
)

enum class HistoryFilter(val label: String) {
    ALL("全部"),
    LOCAL("本地"),
    SYNCED("已同步"),
    FILES("文件"),
    IMAGES("图片"),
    FAILED("失败")
}

sealed class CaptureResult {
    object Success : CaptureResult()
    data class Failure(val message: String) : CaptureResult()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getLocalHistory: GetLocalHistoryUseCase,
    private val publishClipboard: PublishClipboardUseCase,
    private val captureClipboard: CaptureClipboardUseCase,
    private val fetchRemoteHistory: FetchRemoteHistoryUseCase,
    private val wsClient: SyncWebSocketClient,
    private val settingsStore: AppSettingsStore,
    private val credentialStore: DeviceCredentialStore,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    private val selectedDayRange = MutableStateFlow(7)
    private val captureResult = MutableStateFlow<CaptureResult?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        getLocalHistory(200),
        query,
        selectedFilter,
        selectedDayRange,
        combine(wsClient.sessionState, captureResult) { session, capture -> Pair(session, capture) }
    ) { records, currentQuery, filter, dayRange, (session, capture) ->
        val filtered = records
            .filter { record ->
                currentQuery.isBlank() ||
                    record.title.contains(currentQuery, ignoreCase = true) ||
                    record.textPreview.orEmpty().contains(currentQuery, ignoreCase = true) ||
                    record.sourceDeviceId.contains(currentQuery, ignoreCase = true)
            }
            .filter { record ->
                when (filter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.LOCAL -> record.publishState == PublishState.LOCAL_ONLY
                    HistoryFilter.SYNCED -> record.publishState == PublishState.PUBLISHED
                    HistoryFilter.FILES -> record.kind == ClipboardKind.DOCUMENT
                    HistoryFilter.IMAGES -> record.kind == ClipboardKind.IMAGE
                    HistoryFilter.FAILED -> record.publishState == PublishState.FAILED
                }
            }
        HistoryUiState(currentQuery, filter, dayRange, filtered, session, capture)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            if (settings.serverUrl.isNotBlank() && settings.deviceId.isNotBlank()) {
                workScheduler.scheduleHistorySync()
                workScheduler.triggerImmediateSync()
            }
        }
    }

    fun updateQuery(value: String) { query.value = value }

    fun selectFilter(filter: HistoryFilter) { selectedFilter.value = filter }

    fun selectDayRange(days: Int) {
        selectedDayRange.value = days
        viewModelScope.launch {
            fetchRemoteHistory(days)
        }
    }

    fun publish(recordId: String) {
        viewModelScope.launch {
            publishClipboard(recordId)
            workScheduler.triggerImmediateSync()
        }
    }

    fun capture() {
        viewModelScope.launch {
            val result = captureClipboard()
            captureResult.value = if (result.isSuccess) {
                CaptureResult.Success
            } else {
                CaptureResult.Failure(result.exceptionOrNull()?.message ?: "捕获失败")
            }
        }
    }

    fun reconnect() {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            if (settings.serverUrl.isNotBlank()) {
                val credentials = credentialStore.credentials.first()
                wsClient.disconnect()
                if (credentials.token.isNotBlank() && credentials.serverUrl == settings.serverUrl) {
                    wsClient.connect(settings.serverUrl, credentials.token)
                    workScheduler.triggerImmediateSync()
                }
            }
        }
    }

    fun dismissCaptureResult() { captureResult.value = null }
}
