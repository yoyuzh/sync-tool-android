package com.yoyuzh.cliplink.domain.repository

import com.yoyuzh.cliplink.domain.model.AppSettings
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import kotlinx.coroutines.flow.Flow

interface ClipboardRepository {
    // Local history
    fun observeLocalHistory(limit: Int): Flow<List<ClipboardRecord>>
    fun observeLocalHistorySince(sinceMillis: Long, limit: Int): Flow<List<ClipboardRecord>>

    // Local write
    suspend fun saveLocalText(text: String, deviceId: String): ClipboardRecord
    suspend fun trimLocalHistory(maxItems: Int)

    // Publish
    suspend fun publish(recordId: String, serverUrl: String, token: String, sourceDeviceId: String): Result<ClipboardRecord>

    // Remote sync
    suspend fun registerDevice(settings: AppSettings): Result<Pair<String, String>> // deviceId, token
    suspend fun fetchAndMergeHistory(token: String, serverUrl: String, days: Int): Result<Int>
    suspend fun sync(): Result<Unit>
}
