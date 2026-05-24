package com.yoyuzh.cliplink.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardRecordDao {
    @Query("SELECT * FROM clipboard_records ORDER BY createdAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ClipboardRecordEntity>>

    @Query(
        "SELECT * FROM clipboard_records WHERE createdAtMillis >= :sinceMillis ORDER BY createdAtMillis DESC LIMIT :limit"
    )
    fun observeRecentSince(sinceMillis: Long, limit: Int): Flow<List<ClipboardRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ClipboardRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<ClipboardRecordEntity>)

    @Query("SELECT * FROM clipboard_records WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ClipboardRecordEntity?

    @Query("SELECT * FROM clipboard_records WHERE contentHash = :hash LIMIT 1")
    suspend fun findByContentHash(hash: String): ClipboardRecordEntity?

    @Query("DELETE FROM clipboard_records WHERE id NOT IN (SELECT id FROM clipboard_records ORDER BY createdAtMillis DESC LIMIT :keepCount)")
    suspend fun trimToLimit(keepCount: Int)

    @Query("UPDATE clipboard_records SET publishState = :state, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun updatePublishState(id: String, state: String, updatedAt: Long)
}
