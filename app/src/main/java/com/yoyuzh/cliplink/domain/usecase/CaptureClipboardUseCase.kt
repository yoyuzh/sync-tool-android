package com.yoyuzh.cliplink.domain.usecase

import android.content.ClipboardManager
import android.content.Context
import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reads current clipboard text while the app is in the foreground
 * (or via a user-triggered action), hashes it, deduplicates, and
 * saves it as a LOCAL_ONLY record.
 *
 * Returns failure when clipboard is empty or whitespace-only.
 */
class CaptureClipboardUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ClipboardRepository,
    private val settingsStore: AppSettingsStore
) {
    suspend operator fun invoke(): Result<ClipboardRecord> {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return Result.failure(IllegalStateException("ClipboardManager unavailable"))

        val text = manager.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
        if (text.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Clipboard is empty or whitespace-only"))
        }

        val settings = settingsStore.settings.first()
        val deviceId = settings.deviceId.ifBlank { "android-local" }

        return runCatching { repository.saveLocalText(text, deviceId) }
    }
}
