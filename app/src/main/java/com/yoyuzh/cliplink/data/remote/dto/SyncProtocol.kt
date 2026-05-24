package com.yoyuzh.cliplink.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import java.time.Instant
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Protocol constants
// ---------------------------------------------------------------------------

const val SYNC_PROTOCOL_VERSION = 1
const val API_V1_PREFIX = "/api/v1"

// ---------------------------------------------------------------------------
// Device capabilities
// ---------------------------------------------------------------------------

object DeviceCapability {
    const val CLIPBOARD_READ_TEXT = "clipboard.read.text"
    const val CLIPBOARD_WRITE_TEXT = "clipboard.write.text"
    const val HISTORY_QUERY = "history.query"
    const val RECORD_PUBLISH = "record.publish"
}

// ---------------------------------------------------------------------------
// HTTP DTOs
// ---------------------------------------------------------------------------

@Serializable
data class RegisterDeviceRequest(
    val deviceName: String,
    val deviceType: String = "android",
    val capabilities: List<String> = listOf(
        DeviceCapability.CLIPBOARD_READ_TEXT,
        DeviceCapability.CLIPBOARD_WRITE_TEXT,
        DeviceCapability.HISTORY_QUERY,
        DeviceCapability.RECORD_PUBLISH
    )
)

@Serializable
data class RegisterDeviceResponse(
    val device: DeviceSessionDto,
    val token: String,
    val protocolVersion: Int
)

@Serializable
data class DeviceSessionDto(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val capabilities: List<String>,
    val online: Boolean = false,
    val lastSeenAt: String? = null
)

@Serializable
data class HistoryResponse(
    val records: List<ClipboardRecordDto>,
    val nextCursor: String? = null,
    val serverTime: String
)

@Serializable
data class ClipboardRecordDto(
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val sourceDeviceId: String,
    val kind: String,
    val title: String,
    val textPreview: String? = null,
    val textContent: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long,
    val storageMode: String,
    val publishState: String,
    val contentHash: String? = null
)

@Serializable
data class PublishRecordRequest(
    val record: ClipboardRecordDraftDto,
    val clientRequestId: String
) {
    companion object {
        fun fromDomain(
            record: ClipboardRecord,
            clientRequestId: String,
            sourceDeviceId: String = record.sourceDeviceId
        ) = PublishRecordRequest(
            record = ClipboardRecordDraftDto(
                id = record.id,
                createdAt = millisToIso(record.createdAtMillis),
                sourceDeviceId = sourceDeviceId,
                kind = record.kind.toProtocolKind(),
                title = record.title,
                textPreview = record.textPreview,
                textContent = record.textContent,
                mimeType = record.mimeType,
                sizeBytes = record.sizeBytes,
                storageMode = record.storageMode.toProtocolStorageMode(),
                contentHash = record.contentHash
            ),
            clientRequestId = clientRequestId
        )
    }
}

@Serializable
data class ClipboardRecordDraftDto(
    val id: String,
    val createdAt: String,
    val sourceDeviceId: String,
    val kind: String,
    val title: String,
    val textPreview: String? = null,
    val textContent: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long,
    val storageMode: String,
    val contentHash: String? = null
)

@Serializable
data class PublishRecordResponse(
    val record: ClipboardRecordDto,
    val acceptedAt: String
)

@Serializable
data class ApiErrorResponse(
    val error: ApiError
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: JsonElement? = null
)

@Serializable
data class RecordResponse(
    val record: ClipboardRecordDto
)

@Serializable
data class PresenceSnapshotPayload(
    val devices: List<DeviceSessionDto>
)

@Serializable
data class PresenceChangedPayload(
    val device: DeviceSessionDto,
    val online: Boolean
)

@Serializable
data class RecordMessagePayload(
    val record: ClipboardRecordDto
)

@Serializable
data class ServerErrorPayload(
    val code: String,
    val message: String,
    val retryable: Boolean,
    val details: JsonElement? = null
)

// ---------------------------------------------------------------------------
// WebSocket message envelope
// ---------------------------------------------------------------------------

@Serializable
data class SyncMessage(
    val protocolVersion: Int,
    val type: String,
    val messageId: String,
    val sentAt: String,
    val requestId: String? = null,
    val payload: JsonElement? = null
)

// WebSocket message type constants
object MessageType {
    // Client → Server
    const val CLIENT_PING = "client.ping"
    const val RECORD_ACK = "record.ack"

    // Server → Client
    const val SERVER_HELLO = "server.hello"
    const val SERVER_PONG = "server.pong"
    const val SERVER_ERROR = "server.error"
    const val PRESENCE_SNAPSHOT = "presence.snapshot"
    const val PRESENCE_CHANGED = "presence.changed"
    const val RECORD_PUBLISHED = "record.published"
    const val RECORD_UPDATED = "record.updated"
}

// ---------------------------------------------------------------------------
// Domain error codes (matching server error codes)
// ---------------------------------------------------------------------------

object ApiErrorCode {
    const val UNAUTHORIZED = "unauthorized"
    const val VALIDATION_FAILED = "validation_failed"
    const val RECORD_NOT_FOUND = "record_not_found"
    const val BLOB_TOO_LARGE = "blob_too_large"
    const val CONFLICT = "conflict"
    const val INTERNAL_ERROR = "internal_error"
}

fun ClipboardKind.toProtocolKind(): String = when (this) {
    ClipboardKind.TEXT -> "text"
    ClipboardKind.IMAGE -> "image"
    ClipboardKind.DOCUMENT -> "document"
}

fun StorageMode.toProtocolStorageMode(): String = when (this) {
    StorageMode.INLINE -> "metadata_only"
    StorageMode.BLOB -> "source_file"
    StorageMode.METADATA_ONLY -> "metadata_only"
}

fun String.toDomainStorageMode(): StorageMode = when (lowercase()) {
    "source_file" -> StorageMode.BLOB
    "metadata_only" -> StorageMode.INLINE
    else -> StorageMode.INLINE
}

fun ClipboardRecordDto.toDomainStorageMode(): StorageMode = storageMode.toDomainStorageMode()

fun String.toDomainPublishState(): PublishState = when (lowercase()) {
    "local" -> PublishState.LOCAL_ONLY
    "published", "broadcast" -> PublishState.PUBLISHED
    else -> PublishState.PUBLISHED
}

fun ClipboardRecordDto.toDomainPublishState(): PublishState = publishState.toDomainPublishState()

fun String.toDomainKind(): ClipboardKind = when (lowercase()) {
    "image" -> ClipboardKind.IMAGE
    "document" -> ClipboardKind.DOCUMENT
    else -> ClipboardKind.TEXT
}

fun millisToIso(millis: Long): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(millis))

fun parseProtocolMillis(iso: String): Long = try {
    Instant.parse(iso).toEpochMilli()
} catch (e: Exception) {
    System.currentTimeMillis()
}
