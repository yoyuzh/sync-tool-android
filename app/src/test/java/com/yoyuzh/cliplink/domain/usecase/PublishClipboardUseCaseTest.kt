package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentialStore
import com.yoyuzh.cliplink.data.settings.DeviceCredentials
import com.yoyuzh.cliplink.domain.model.AppSettings
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PublishClipboardUseCaseTest {

    private lateinit var repository: ClipboardRepository
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var credentialStore: DeviceCredentialStore
    private lateinit var useCase: PublishClipboardUseCase

    private val localRecord = ClipboardRecord(
        id = "local-record-1",
        createdAtMillis = 1_000_000L,
        updatedAtMillis = 1_000_000L,
        sourceDeviceId = "android-device-1",
        kind = ClipboardKind.TEXT,
        title = "Hello",
        textPreview = "Hello world",
        textContent = "Hello world",
        mimeType = "text/plain",
        sizeBytes = 11L,
        storageMode = StorageMode.INLINE,
        publishState = PublishState.LOCAL_ONLY,
        contentHash = "abc"
    )

    @Before
    fun setUp() {
        repository = mock()
        settingsStore = mock()
        credentialStore = mock()
        whenever(settingsStore.settings).doReturn(
            flowOf(
                AppSettings(
                    serverUrl = "http://127.0.0.1:8787",
                    deviceId = "device-1",
                    manualPublishEnabled = true
                )
            )
        )
        whenever(credentialStore.credentials).doReturn(
            flowOf(DeviceCredentials(token = "token-1", serverUrl = "http://127.0.0.1:8787"))
        )
        useCase = PublishClipboardUseCase(repository, settingsStore, credentialStore)
    }

    @Test
    fun `invoke calls repository publish and returns success`() = runTest {
        val published = localRecord.copy(publishState = PublishState.PUBLISHED)
        whenever(repository.publish("local-record-1", "http://127.0.0.1:8787", "token-1", "device-1"))
            .doReturn(Result.success(published))

        val result = useCase("local-record-1")

        assertTrue(result.isSuccess)
        assertEquals(PublishState.PUBLISHED, result.getOrNull()?.publishState)
        verify(repository).publish("local-record-1", "http://127.0.0.1:8787", "token-1", "device-1")
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        whenever(repository.publish("local-record-1", "http://127.0.0.1:8787", "token-1", "device-1"))
            .doReturn(Result.failure(Exception("Network error")))

        val result = useCase("local-record-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke does not publish when manual publish is disabled`() = runTest {
        whenever(settingsStore.settings).doReturn(
            flowOf(
                AppSettings(
                    serverUrl = "http://127.0.0.1:8787",
                    deviceId = "device-1",
                    manualPublishEnabled = false
                )
            )
        )

        val result = useCase("local-record-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke clears token when repository reports unauthorized`() = runTest {
        whenever(repository.publish("local-record-1", "http://127.0.0.1:8787", "token-1", "device-1"))
            .doReturn(Result.failure(com.yoyuzh.cliplink.domain.error.DomainError.Unauthorized))

        val result = useCase("local-record-1")

        assertTrue(result.isFailure)
        verify(credentialStore).clearToken()
    }
}
