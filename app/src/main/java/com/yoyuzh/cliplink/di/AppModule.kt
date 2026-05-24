package com.yoyuzh.cliplink.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.yoyuzh.cliplink.data.local.dao.ClipboardRecordDao
import com.yoyuzh.cliplink.data.local.db.ClipLinkDatabase
import com.yoyuzh.cliplink.data.repository.DefaultClipboardRepository
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindClipboardRepository(
        repository: DefaultClipboardRepository
    ): ClipboardRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ClipLinkDatabase {
        return Room.databaseBuilder(
            context,
            ClipLinkDatabase::class.java,
            "cliplink.db"
        )
            .addMigrations(ClipLinkDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideClipboardRecordDao(database: ClipLinkDatabase): ClipboardRecordDao {
        return database.clipboardRecordDao()
    }

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
