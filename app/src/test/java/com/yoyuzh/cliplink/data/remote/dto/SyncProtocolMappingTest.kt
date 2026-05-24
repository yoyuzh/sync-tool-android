package com.yoyuzh.cliplink.data.remote.dto

import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncProtocolMappingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `local inline text record is published as server metadata_only storage mode`() {
        val request = PublishRecordRequest.fromDomain(
            record = record(storageMode = StorageMode.INLINE),
            clientRequestId = "request-1"
        )

        val encoded = json.encodeToString(PublishRecordRequest.serializer(), request)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertEquals("metadata_only", root["record"]!!.jsonObject["storageMode"]!!.jsonPrimitive.content)
    }

    @Test
    fun `publish request can override source device id with registered server device id`() {
        val request = PublishRecordRequest.fromDomain(
            record = record(storageMode = StorageMode.INLINE).copy(sourceDeviceId = "android-local-before-registration"),
            clientRequestId = "request-1",
            sourceDeviceId = "server-device-1"
        )

        val encoded = json.encodeToString(PublishRecordRequest.serializer(), request)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertEquals("server-device-1", root["record"]!!.jsonObject["sourceDeviceId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `server record storage and publish states map back to Android domain values`() {
        val dto = ClipboardRecordDto(
            id = "server-record-1",
            createdAt = "2026-05-24T00:00:00Z",
            updatedAt = "2026-05-24T00:01:00Z",
            sourceDeviceId = "desktop-1",
            kind = "text",
            title = "Hello",
            textPreview = "Hello",
            textContent = "Hello",
            mimeType = "text/plain",
            sizeBytes = 5,
            storageMode = "metadata_only",
            publishState = "published",
            contentHash = "hash"
        )

        assertEquals(StorageMode.INLINE, dto.toDomainStorageMode())
        assertEquals(PublishState.PUBLISHED, dto.toDomainPublishState())
    }

    private fun record(storageMode: StorageMode) = ClipboardRecord(
        id = "local-record-1",
        createdAtMillis = 1_000_000L,
        updatedAtMillis = 1_000_000L,
        sourceDeviceId = "android-1",
        kind = ClipboardKind.TEXT,
        title = "Hello",
        textPreview = "Hello",
        textContent = "Hello",
        mimeType = "text/plain",
        sizeBytes = 5,
        storageMode = storageMode,
        publishState = PublishState.LOCAL_ONLY,
        contentHash = "hash"
    )
}
