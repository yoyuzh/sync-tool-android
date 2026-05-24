package com.yoyuzh.cliplink.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yoyuzh.cliplink.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cliplink_settings")

@Singleton
class AppSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialStore: DeviceCredentialStore
) {
    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val MANUAL_PUBLISH_ENABLED = booleanPreferencesKey("manual_publish_enabled")
        val NOTIFICATION_PREVIEW_ENABLED = booleanPreferencesKey("notification_preview_enabled")
        val MAX_LOCAL_HISTORY_ITEMS = intPreferencesKey("max_local_history_items")
        val SYNC_WINDOW_DAYS = intPreferencesKey("sync_window_days")
        val ACCESSIBILITY_ENABLED_OBSERVED = booleanPreferencesKey("accessibility_enabled_observed")
        val EXCLUDED_APPS = stringSetPreferencesKey("excluded_apps")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serverUrl = prefs[Keys.SERVER_URL] ?: "",
            deviceName = prefs[Keys.DEVICE_NAME] ?: "My Android",
            deviceId = prefs[Keys.DEVICE_ID] ?: "",
            manualPublishEnabled = prefs[Keys.MANUAL_PUBLISH_ENABLED] ?: true,
            notificationPreviewEnabled = prefs[Keys.NOTIFICATION_PREVIEW_ENABLED] ?: false,
            maxLocalHistoryItems = prefs[Keys.MAX_LOCAL_HISTORY_ITEMS] ?: 200,
            syncWindowDays = prefs[Keys.SYNC_WINDOW_DAYS] ?: 7,
            accessibilityEnabledObserved = prefs[Keys.ACCESSIBILITY_ENABLED_OBSERVED] ?: false,
            excludedApps = prefs[Keys.EXCLUDED_APPS] ?: emptySet()
        )
    }

    suspend fun updateServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            val previous = prefs[Keys.SERVER_URL].orEmpty()
            val normalized = url.trim()
            prefs[Keys.SERVER_URL] = normalized
            if (previous != normalized) {
                prefs.remove(Keys.DEVICE_ID)
            }
        }
        credentialStore.clearToken()
    }

    suspend fun updateDeviceName(name: String) {
        context.dataStore.edit { it[Keys.DEVICE_NAME] = name }
    }

    suspend fun updateDeviceId(id: String) {
        context.dataStore.edit { it[Keys.DEVICE_ID] = id }
    }

    suspend fun ensureDeviceId(): String {
        var id = ""
        context.dataStore.edit { prefs ->
            id = prefs[Keys.DEVICE_ID] ?: ""
            if (id.isBlank()) {
                id = "android-${UUID.randomUUID()}"
                prefs[Keys.DEVICE_ID] = id
            }
        }
        return id
    }

    suspend fun updateManualPublishEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MANUAL_PUBLISH_ENABLED] = enabled }
    }

    suspend fun updateNotificationPreviewEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_PREVIEW_ENABLED] = enabled }
    }

    suspend fun updateMaxLocalHistoryItems(count: Int) {
        context.dataStore.edit { it[Keys.MAX_LOCAL_HISTORY_ITEMS] = count }
    }

    suspend fun updateSyncWindowDays(days: Int) {
        context.dataStore.edit { it[Keys.SYNC_WINDOW_DAYS] = days }
    }

    suspend fun updateAccessibilityEnabledObserved(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ACCESSIBILITY_ENABLED_OBSERVED] = enabled }
    }

    suspend fun updateExcludedApps(apps: Set<String>) {
        context.dataStore.edit { it[Keys.EXCLUDED_APPS] = apps }
    }
}
