package com.yoyuzh.cliplink.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_records")
data class ClipboardRecordEntity(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceDeviceId: String,
    val kind: String,
    val title: String,
    val textPreview: String?,
    val textContent: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val storageMode: String,
    val publishState: String,
    val contentHash: String?
)
