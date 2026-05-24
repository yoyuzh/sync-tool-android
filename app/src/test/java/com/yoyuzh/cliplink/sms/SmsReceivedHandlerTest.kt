package com.yoyuzh.cliplink.sms

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.domain.model.AppSettings
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SmsReceivedHandlerTest {
    @Test
    fun `received sms is saved as local text history without exposing full sender`() = runTest {
        val repository = FakeClipboardRepository()
        val settingsStore = mock<AppSettingsStore>()
        whenever(settingsStore.settings).thenReturn(flowOf(AppSettings(deviceId = "android-device-1")))
        val handler = SmsReceivedHandler(repository, settingsStore)

        val record = handler.handle(
            listOf(
                InboundSmsMessage(
                    sender = "+8613812345678",
                    body = "【ClipLink】验证码 482913，5 分钟内有效。",
                    receivedAtMillis = 1000L
                )
            )
        )

        assertEquals("android-device-1", record?.sourceDeviceId)
        assertEquals(PublishState.LOCAL_ONLY, record?.publishState)
        assertEquals("短信来自 +***5678\n【ClipLink】验证码 482913，5 分钟内有效。", repository.savedText)
    }

    @Test
    fun `blank sms bodies are ignored`() = runTest {
        val repository = FakeClipboardRepository()
        val settingsStore = mock<AppSettingsStore>()
        whenever(settingsStore.settings).thenReturn(flowOf(AppSettings(deviceId = "android-device-1")))
        val handler = SmsReceivedHandler(repository, settingsStore)

        val record = handler.handle(
            listOf(InboundSmsMessage(sender = "+10086", body = "   ", receivedAtMillis = 1000L))
        )

        assertNull(record)
        assertNull(repository.savedText)
    }
}

private class FakeClipboardRepository : ClipboardRepository {
    var savedText: String? = null

    override fun observeLocalHistory(limit: Int): Flow<List<ClipboardRecord>> = flowOf(emptyList())

    override fun observeLocalHistorySince(sinceMillis: Long, limit: Int): Flow<List<ClipboardRecord>> =
        flowOf(emptyList())

    override suspend fun saveLocalText(text: String, deviceId: String): ClipboardRecord {
        savedText = text
        return ClipboardRecord(
            id = "sms-local-1",
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L,
            sourceDeviceId = deviceId,
            kind = ClipboardKind.TEXT,
            title = text.take(50),
            textPreview = text.take(200),
            textContent = text,
            mimeType = "text/plain",
            sizeBytes = text.toByteArray().size.toLong(),
            storageMode = StorageMode.INLINE,
            publishState = PublishState.LOCAL_ONLY,
            contentHash = "sms-hash"
        )
    }

    override suspend fun trimLocalHistory(maxItems: Int) = Unit

    override suspend fun publish(
        recordId: String,
        serverUrl: String,
        token: String,
        sourceDeviceId: String
    ): Result<ClipboardRecord> = Result.failure(UnsupportedOperationException())

    override suspend fun registerDevice(settings: AppSettings): Result<Pair<String, String>> =
        Result.failure(UnsupportedOperationException())

    override suspend fun fetchAndMergeHistory(token: String, serverUrl: String, days: Int): Result<Int> =
        Result.failure(UnsupportedOperationException())

    override suspend fun sync(): Result<Unit> = Result.success(Unit)
}
