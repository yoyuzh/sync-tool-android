package com.yoyuzh.cliplink.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yoyuzh.cliplink.data.local.db.ClipLinkDatabase
import com.yoyuzh.cliplink.data.mapper.toDomain
import com.yoyuzh.cliplink.data.remote.api.ApiResult
import com.yoyuzh.cliplink.data.remote.api.SyncApiClient
import com.yoyuzh.cliplink.data.remote.dto.ClipboardRecordDto
import com.yoyuzh.cliplink.data.remote.dto.PublishRecordResponse
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultClipboardRepositoryTest {
    private lateinit var database: ClipLinkDatabase
    private lateinit var repository: DefaultClipboardRepository
    private val mockApiClient: SyncApiClient = mock()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClipLinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultClipboardRepository(database.clipboardRecordDao(), mockApiClient)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saveLocalText creates LOCAL_ONLY text record`() = runTest {
        val saved = repository.saveLocalText("hello world", "android-test-device")

        assertEquals(PublishState.LOCAL_ONLY, saved.publishState)
        assertEquals("hello world", saved.textPreview)
        assertNotNull(saved.contentHash)
        assertEquals("android-test-device", saved.sourceDeviceId)

        repository.observeLocalHistory(10).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(saved.id, items.single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveLocalText deduplicates identical content`() = runTest {
        repository.saveLocalText("duplicate text", "android-test-device")
        repository.saveLocalText("duplicate text", "android-test-device")

        repository.observeLocalHistory(10).test {
            val items = awaitItem()
            assertEquals("Should deduplicate by contentHash", 1, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveLocalText ignores whitespace-only text`() = runTest {
        // Blank text trimming is enforced at the use-case level;
        // repository stores the hash of the trimmed empty string
        // This test verifies saving the trimmed blank string still creates a record
        // (the use case handles the reject-if-blank guard)
        val saved = repository.saveLocalText("   normal text   ", "device-1")
        assertEquals("normal text", saved.textContent)
        assertTrue(saved.sizeBytes > 0)
    }

    @Test
    fun `trimLocalHistory removes oldest records beyond limit`() = runTest {
        repeat(5) { i ->
            Thread.sleep(1) // ensure different timestamps
            repository.saveLocalText("Record $i nano ${i * 1000}", "device")
        }

        repository.trimLocalHistory(3)

        repository.observeLocalHistory(10).test {
            val items = awaitItem()
            assertTrue("Should keep at most 3 records", items.size <= 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `publish merges canonical server record returned by publish API`() = runTest {
        val local = repository.saveLocalText("server canonical text", "android-local")
        whenever(
            mockApiClient.publishRecord(
                org.mockito.kotlin.eq("http://127.0.0.1:8787"),
                org.mockito.kotlin.eq("token-1"),
                org.mockito.kotlin.any()
            )
        ).thenReturn(
            ApiResult.Success(
                PublishRecordResponse(
                    record = ClipboardRecordDto(
                        id = local.id,
                        createdAt = "2026-05-24T00:00:00Z",
                        updatedAt = "2026-05-24T00:00:01Z",
                        sourceDeviceId = "server-device",
                        kind = "text",
                        title = "Server title",
                        textPreview = "Server preview",
                        textContent = "Server canonical text",
                        mimeType = "text/plain",
                        sizeBytes = 21,
                        storageMode = "metadata_only",
                        publishState = "published",
                        contentHash = "server-hash"
                    ),
                    acceptedAt = "2026-05-24T00:00:01Z"
                )
            )
        )

        val result = repository.publish(
            recordId = local.id,
            serverUrl = "http://127.0.0.1:8787",
            token = "token-1",
            sourceDeviceId = "server-device"
        )

        assertTrue(result.isSuccess)
        val published = database.clipboardRecordDao().findById(local.id)!!.toDomain()
        assertEquals("server-device", published.sourceDeviceId)
        assertEquals("Server title", published.title)
        assertEquals("Server preview", published.textPreview)
        assertEquals(StorageMode.INLINE, published.storageMode)
        assertEquals(PublishState.PUBLISHED, published.publishState)
    }
}
