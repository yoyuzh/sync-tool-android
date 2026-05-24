package com.yoyuzh.cliplink.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCredentials(
    val token: String = "",
    val serverUrl: String = ""
)

@Singleton
class DeviceCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val state = MutableStateFlow(readCredentials())

    val credentials: Flow<DeviceCredentials> = state.asStateFlow()

    suspend fun saveToken(serverUrl: String, token: String) {
        prefs.edit()
            .putString(DEVICE_TOKEN, token)
            .putString(DEVICE_TOKEN_SERVER_URL, serverUrl)
            .apply()
        state.value = DeviceCredentials(token = token, serverUrl = serverUrl)
    }

    suspend fun clearToken() {
        prefs.edit()
            .remove(DEVICE_TOKEN)
            .remove(DEVICE_TOKEN_SERVER_URL)
            .apply()
        state.value = DeviceCredentials()
    }

    private fun readCredentials(): DeviceCredentials {
        return DeviceCredentials(
            token = prefs.getString(DEVICE_TOKEN, null).orEmpty(),
            serverUrl = prefs.getString(DEVICE_TOKEN_SERVER_URL, null).orEmpty()
        )
    }

    companion object {
        private const val PREFS_NAME = "cliplink_credentials_secure"
        private const val DEVICE_TOKEN = "device_token"
        private const val DEVICE_TOKEN_SERVER_URL = "device_token_server_url"
    }
}
