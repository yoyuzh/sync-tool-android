package com.yoyuzh.cliplink.data.mapper

import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardRecordMapperTest {

    private fun sampleEntity() = ClipboardRecordEntity(
        id = "test-id-1",
        createdAtMillis = 1_000_000L,
        updatedAtMillis = 1_000_100L,
        sourceDeviceId = "android-abc",
        kind = "TEXT",
        title = "Hello world",
        textPreview = "Hello world preview",
        textContent = "Hello world full content",
        mimeType = "text/plain",
        sizeBytes = 24L,
        storageMode = "INLINE",
        publishState = "LOCAL_ONLY",
        contentHash = "abc123"
    )

    @Test
    fun `entity toDomain maps all fields correctly`() {
        val entity = sampleEntity()
        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.createdAtMillis, domain.createdAtMillis)
        assertEquals(entity.updatedAtMillis, domain.updatedAtMillis)
        assertEquals(entity.sourceDeviceId, domain.sourceDeviceId)
        assertEquals(ClipboardKind.TEXT, domain.kind)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.textPreview, domain.textPreview)
        assertEquals(entity.textContent, domain.textContent)
        assertEquals(entity.mimeType, domain.mimeType)
        assertEquals(entity.sizeBytes, domain.sizeBytes)
        assertEquals(StorageMode.INLINE, domain.storageMode)
        assertEquals(PublishState.LOCAL_ONLY, domain.publishState)
        assertEquals(entity.contentHash, domain.contentHash)
    }

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val domain = sampleEntity().toDomain()
        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.createdAtMillis, entity.createdAtMillis)
        assertEquals(domain.updatedAtMillis, entity.updatedAtMillis)
        assertEquals(domain.sourceDeviceId, entity.sourceDeviceId)
        assertEquals("TEXT", entity.kind)
        assertEquals(domain.title, entity.title)
        assertEquals(domain.textPreview, entity.textPreview)
        assertEquals(domain.textContent, entity.textContent)
        assertEquals(domain.mimeType, entity.mimeType)
        assertEquals(domain.sizeBytes, entity.sizeBytes)
        assertEquals("INLINE", entity.storageMode)
        assertEquals("LOCAL_ONLY", entity.publishState)
        assertEquals(domain.contentHash, entity.contentHash)
    }

    @Test
    fun `entity with null optional fields maps to domain without crash`() {
        val entity = sampleEntity().copy(
            textPreview = null,
            textContent = null,
            mimeType = null,
            contentHash = null
        )
        val domain = entity.toDomain()
        assertNull(domain.textPreview)
        assertNull(domain.textContent)
        assertNull(domain.mimeType)
        assertNull(domain.contentHash)
    }

    @Test
    fun `roundtrip entity to domain and back preserves data`() {
        val original = sampleEntity()
        val roundtripped = original.toDomain().toEntity()

        assertEquals(original.id, roundtripped.id)
        assertEquals(original.createdAtMillis, roundtripped.createdAtMillis)
        assertEquals(original.updatedAtMillis, roundtripped.updatedAtMillis)
        assertEquals(original.kind, roundtripped.kind)
        assertEquals(original.publishState, roundtripped.publishState)
        assertEquals(original.storageMode, roundtripped.storageMode)
        assertEquals(original.contentHash, roundtripped.contentHash)
    }
}
