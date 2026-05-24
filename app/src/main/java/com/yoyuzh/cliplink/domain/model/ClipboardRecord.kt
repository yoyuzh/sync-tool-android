package com.yoyuzh.cliplink.domain.model

data class ClipboardRecord(
    val id: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceDeviceId: String,
    val kind: ClipboardKind,
    val title: String,
    val textPreview: String?,
    val textContent: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val storageMode: StorageMode,
    val publishState: PublishState,
    val contentHash: String?
)
