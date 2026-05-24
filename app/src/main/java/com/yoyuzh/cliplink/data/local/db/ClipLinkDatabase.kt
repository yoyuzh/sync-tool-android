package com.yoyuzh.cliplink.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yoyuzh.cliplink.data.local.dao.ClipboardRecordDao
import com.yoyuzh.cliplink.data.local.entity.ClipboardRecordEntity

@Database(
    entities = [ClipboardRecordEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ClipLinkDatabase : RoomDatabase() {
    abstract fun clipboardRecordDao(): ClipboardRecordDao

    companion object {
        /**
         * Migration from v1 (original schema) to v2 (extended schema with
         * updatedAtMillis, textContent, storageMode, contentHash).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clipboard_records ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clipboard_records ADD COLUMN textContent TEXT")
                db.execSQL("ALTER TABLE clipboard_records ADD COLUMN storageMode TEXT NOT NULL DEFAULT 'INLINE'")
                db.execSQL("ALTER TABLE clipboard_records ADD COLUMN contentHash TEXT")
            }
        }
    }
}
