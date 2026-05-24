package com.yoyuzh.cliplink.data.mapper

import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode

fun ClipboardRecordEntity.toDomain() = ClipboardRecord(
    id = id,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    sourceDeviceId = sourceDeviceId,
    kind = ClipboardKind.valueOf(kind),
    title = title,
    textPreview = textPreview,
    textContent = textContent,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    storageMode = StorageMode.valueOf(storageMode),
    publishState = PublishState.valueOf(publishState),
    contentHash = contentHash
)

fun ClipboardRecord.toEntity() = ClipboardRecordEntity(
    id = id,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    sourceDeviceId = sourceDeviceId,
    kind = kind.name,
    title = title,
    textPreview = textPreview,
    textContent = textContent,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    storageMode = storageMode.name,
    publishState = publishState.name,
    contentHash = contentHash
)
