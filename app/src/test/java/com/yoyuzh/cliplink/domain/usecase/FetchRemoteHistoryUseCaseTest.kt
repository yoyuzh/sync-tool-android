package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentials
import com.yoyuzh.cliplink.domain.error.DomainError
import com.yoyuzh.cliplink.domain.model.AppSettings
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FetchRemoteHistoryUseCaseTest {
    private lateinit var repository: ClipboardRepository
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var credentialStore: DeviceCredentialStore
    private lateinit var useCase: FetchRemoteHistoryUseCase

    @Before
    fun setUp() {
        repository = mock()
        settingsStore = mock()
        credentialStore = mock()
        whenever(settingsStore.settings).doReturn(
            flowOf(AppSettings(serverUrl = "http://127.0.0.1:8787", deviceId = "device-1"))
        )
        whenever(credentialStore.credentials).doReturn(
            flowOf(DeviceCredentials(token = "token-1", serverUrl = "http://127.0.0.1:8787"))
        )
        useCase = FetchRemoteHistoryUseCase(repository, settingsStore, credentialStore)
    }

    @Test
    fun `invoke clears token when repository reports unauthorized`() = runTest {
        whenever(repository.fetchAndMergeHistory("token-1", "http://127.0.0.1:8787", 7))
            .doReturn(Result.failure(DomainError.Unauthorized))

        val result = useCase(7)

        assertTrue(result.isFailure)
        verify(credentialStore).clearToken()
    }
}
