package com.yoyuzh.cliplink.domain.usecase

import app.cash.turbine.test
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
import org.junit.Test

class GetLocalHistoryUseCaseTest {
    @Test
    fun emitsRecordsInRepositoryOrder() = runTest {
        val records = listOf(record("1", "first"), record("2", "second"))
        val useCase = GetLocalHistoryUseCase(FakeClipboardRepository(records))

        useCase(limit = 2).test {
            assertEquals(records, awaitItem())
            awaitComplete()
        }
    }

    private fun record(id: String, title: String) = ClipboardRecord(
        id = id,
        createdAtMillis = id.toLong(),
        updatedAtMillis = id.toLong(),
        sourceDeviceId = "android-local",
        kind = ClipboardKind.TEXT,
        title = title,
        textPreview = title,
        textContent = title,
        mimeType = "text/plain",
        sizeBytes = title.length.toLong(),
        storageMode = StorageMode.INLINE,
        publishState = PublishState.LOCAL_ONLY,
        contentHash = null
    )

    private class FakeClipboardRepository(
        private val records: List<ClipboardRecord>
    ) : ClipboardRepository {
        override fun observeLocalHistory(limit: Int): Flow<List<ClipboardRecord>> =
            flowOf(records.take(limit))

        override fun observeLocalHistorySince(sinceMillis: Long, limit: Int): Flow<List<ClipboardRecord>> =
            flowOf(records.take(limit))

        override suspend fun saveLocalText(text: String, deviceId: String): ClipboardRecord =
            records.first()

        override suspend fun trimLocalHistory(maxItems: Int) = Unit

        override suspend fun publish(
            recordId: String,
            serverUrl: String,
            token: String,
            sourceDeviceId: String
        ): Result<ClipboardRecord> =
            Result.success(records.first())

        override suspend fun registerDevice(settings: AppSettings): Result<Pair<String, String>> =
            Result.success(Pair("device-1", "token-1"))

        override suspend fun fetchAndMergeHistory(token: String, serverUrl: String, days: Int): Result<Int> =
            Result.success(0)

        override suspend fun sync(): Result<Unit> = Result.success(Unit)
    }
}
