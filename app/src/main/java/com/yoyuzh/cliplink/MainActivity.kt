package com.yoyuzh.cliplink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yoyuzh.cliplink.data.remote.ws.SyncWebSocketClient
import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.notification.ClipLinkNotificationManager
import com.yoyuzh.cliplink.ui.ClipLinkApp
import com.yoyuzh.cliplink.worker.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var wsClient: SyncWebSocketClient
    @Inject lateinit var settingsStore: AppSettingsStore
    @Inject lateinit var credentialStore: DeviceCredentialStore
    @Inject lateinit var notificationManager: ClipLinkNotificationManager

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure notification channels exist
        notificationManager.ensureChannels()

        // Schedule periodic background sync
        workScheduler.scheduleHistorySync()

        setContent { ClipLinkApp() }
    }

    override fun onStart() {
        super.onStart()
        // Connect WebSocket when app comes to foreground
        activityScope.launch {
            val settings = settingsStore.settings.first()
            if (settings.serverUrl.isNotBlank() && settings.deviceId.isNotBlank()) {
                val credentials = credentialStore.credentials.first()
                if (credentials.token.isNotBlank() && credentials.serverUrl == settings.serverUrl) {
                    wsClient.connect(settings.serverUrl, credentials.token)
                    workScheduler.triggerImmediateSync()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Disconnect WebSocket when app goes to background
        wsClient.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
