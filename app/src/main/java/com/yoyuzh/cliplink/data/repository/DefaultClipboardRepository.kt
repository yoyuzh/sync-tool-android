package com.yoyuzh.cliplink.data.repository

import com.yoyuzh.cliplink.data.local.dao.ClipboardRecordDao
import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity
import com.yoyuzh.cliplink.data.mapper.toDomain
import com.yoyuzh.cliplink.data.mapper.toEntity
import com.yoyuzh.cliplink.data.remote.api.ApiResult
import com.yoyuzh.cliplink.data.remote.api.SyncApiClient
import com.yoyuzh.cliplink.data.remote.dto.ClipboardRecordDto
import com.yoyuzh.cliplink.data.remote.dto.ClipboardRecordDraftDto
import com.yoyuzh.cliplink.data.remote.dto.PublishRecordRequest
import com.yoyuzh.cliplink.data.remote.dto.RegisterDeviceRequest
import com.yoyuzh.cliplink.data.remote.dto.PublishRecordRequest.Companion.fromDomain
import com.yoyuzh.cliplink.data.remote.dto.parseProtocolMillis
import com.yoyuzh.cliplink.data.remote.dto.toDomainKind
import com.yoyuzh.cliplink.data.remote.dto.toDomainPublishState
import com.yoyuzh.cliplink.data.remote.dto.toDomainStorageMode
import com.yoyuzh.cliplink.domain.error.DomainError
import com.yoyuzh.cliplink.domain.model.AppSettings
import com.yoyuzh.cliplink.domain.model.ClipboardKind
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.model.PublishState
import com.yoyuzh.cliplink.domain.model.StorageMode
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class DefaultClipboardRepository @Inject constructor(
    private val dao: ClipboardRecordDao,
    private val apiClient: SyncApiClient
) : ClipboardRepository {

    override fun observeLocalHistory(limit: Int): Flow<List<ClipboardRecord>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeLocalHistorySince(sinceMillis: Long, limit: Int): Flow<List<ClipboardRecord>> =
        dao.observeRecentSince(sinceMillis, limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveLocalText(text: String, deviceId: String): ClipboardRecord =
        withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            val now = System.currentTimeMillis()
            val hash = sha256Hex(trimmed)

            // Deduplicate by content hash
            dao.findByContentHash(hash)?.let { existing ->
                return@withContext existing.toDomain()
            }

            val record = ClipboardRecord(
                id = "local-${UUID.randomUUID()}",
                createdAtMillis = now,
                updatedAtMillis = now,
                sourceDeviceId = deviceId,
                kind = ClipboardKind.TEXT,
                title = trimmed.take(50).ifBlank { "Empty text" },
                textPreview = trimmed.take(200),
                textContent = trimmed,
                mimeType = "text/plain",
                sizeBytes = trimmed.toByteArray(Charsets.UTF_8).size.toLong(),
                storageMode = StorageMode.INLINE,
                publishState = PublishState.LOCAL_ONLY,
                contentHash = hash
            )
            dao.upsert(record.toEntity())
            record
        }

    override suspend fun trimLocalHistory(maxItems: Int) = withContext(Dispatchers.IO) {
        dao.trimToLimit(maxItems)
    }

    override suspend fun publish(
        recordId: String,
        serverUrl: String,
        token: String,
        sourceDeviceId: String
    ): Result<ClipboardRecord> = withContext(Dispatchers.IO) {
        val entity = dao.findById(recordId)
            ?: return@withContext Result.failure(IllegalArgumentException("Record not found: $recordId"))
        if (serverUrl.isBlank() || token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Device is not registered"))
        }

        val record = entity.toDomain()
        if (record.textContent.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Record has no text content"))
        }

        // Mark as pending
        val now = System.currentTimeMillis()
        dao.updatePublishState(recordId, PublishState.PENDING.name, now)

        val clientRequestId = UUID.randomUUID().toString()
        val result = apiClient.publishRecord(
            serverUrl = serverUrl,
            token = token,
            request = PublishRecordRequest.fromDomain(
                record = record,
                clientRequestId = clientRequestId,
                sourceDeviceId = sourceDeviceId
            )
        )

        return@withContext when (result) {
            is ApiResult.Success -> {
                val published = result.data.record.toEntity()
                dao.upsert(published)
                Result.success(published.toDomain())
            }
            is ApiResult.Failure -> {
                dao.updatePublishState(recordId, PublishState.FAILED.name, System.currentTimeMillis())
                Result.failure(result.error.toDomainThrowable("Publish failed"))
            }
        }
    }

    override suspend fun registerDevice(settings: AppSettings): Result<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val request = RegisterDeviceRequest(deviceName = settings.deviceName)
            return@withContext when (val result = apiClient.registerDevice(settings.serverUrl, request)) {
                is ApiResult.Success -> {
                    val response = result.data
                    Result.success(Pair(response.device.deviceId, response.token))
                }
                is ApiResult.Failure -> Result.failure(result.error.toDomainThrowable("Registration failed"))
            }
        }

    override suspend fun fetchAndMergeHistory(
        token: String,
        serverUrl: String,
        days: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (serverUrl.isBlank() || token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Device is not registered"))
        }
        return@withContext when (
            val result = apiClient.fetchHistory(
                serverUrl = serverUrl,
                token = token,
                days = days,
                limit = 100
            )
        ) {
            is ApiResult.Success -> {
                val remoteRecords = result.data.records
                val entities = remoteRecords.map { dto -> dto.toEntity() }
                dao.upsertAll(entities)
                Result.success(entities.size)
            }
            is ApiResult.Failure -> Result.failure(result.error.toDomainThrowable("Fetch history failed"))
        }
    }

    override suspend fun sync(): Result<Unit> = Result.success(Unit)

    // ---------------------------------------------------------------------------

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun ApiResult.ApiError.toDomainThrowable(message: String): Throwable = when (this) {
        ApiResult.ApiError.UNAUTHORIZED -> DomainError.Unauthorized
        ApiResult.ApiError.NOT_FOUND -> DomainError.RecordNotFound
        ApiResult.ApiError.CONFLICT -> DomainError.Conflict
        ApiResult.ApiError.VALIDATION_FAILED -> DomainError.ValidationFailed
        ApiResult.ApiError.NETWORK_ERROR -> DomainError.NetworkError
        ApiResult.ApiError.INTERNAL_ERROR -> DomainError.ServerError("internal_error", message)
        ApiResult.ApiError.UNKNOWN -> DomainError.Unknown(null)
    }

}

private fun ClipboardRecordDto.toEntity() = ClipboardRecordEntity(
    id = id,
    createdAtMillis = parseProtocolMillis(createdAt),
    updatedAtMillis = parseProtocolMillis(updatedAt),
    sourceDeviceId = sourceDeviceId,
    kind = kind.toDomainKind().name,
    title = title,
    textPreview = textPreview,
    textContent = textContent,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    storageMode = toDomainStorageMode().name,
    publishState = toDomainPublishState().name,
    contentHash = contentHash
)
