package com.yoyuzh.cliplink.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.yoyuzh.cliplink.data.local.db.ClipLinkDatabase
import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardRecordDaoTest {
    private lateinit var database: ClipLinkDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClipLinkDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeRecentReturnsNewestFirst() = runTest {
        val dao = database.clipboardRecordDao()
        dao.upsert(record("old", 1_000))
        dao.upsert(record("new", 2_000))

        dao.observeRecent(2).test {
            val rows = awaitItem()
            assertEquals(listOf("new", "old"), rows.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun record(id: String, createdAt: Long) = ClipboardRecordEntity(
        id = id,
        createdAtMillis = createdAt,
        updatedAtMillis = createdAt,
        sourceDeviceId = "android-local",
        kind = "TEXT",
        title = id,
        textPreview = id,
        textContent = id,
        mimeType = "text/plain",
        sizeBytes = id.length.toLong(),
        storageMode = "INLINE",
        publishState = "LOCAL_ONLY",
        contentHash = id
    )
}
