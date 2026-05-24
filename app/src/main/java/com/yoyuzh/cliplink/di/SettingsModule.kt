package com.yoyuzh.cliplink.di

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Note: AppSettingsStore is itself @Singleton and @Inject constructor,
 * so Hilt can provide it directly. This module is a placeholder for
 * future settings bindings or migration of other settings-related providers.
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule
// AppSettingsStore is auto-provided via @Singleton @Inject constructor
