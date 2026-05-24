package com.yoyuzh.cliplink.domain.usecase

import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import javax.inject.Inject

/**
 * Captures the current clipboard, then immediately publishes it.
 * Mirrors Electron's global-shortcut "publish current clipboard" behavior.
 */
class PublishCurrentClipboardUseCase @Inject constructor(
    private val captureClipboard: CaptureClipboardUseCase,
    private val publishClipboard: PublishClipboardUseCase
) {
    suspend operator fun invoke(): Result<ClipboardRecord> {
        val captured = captureClipboard()
        if (captured.isFailure) return captured

        val record = captured.getOrThrow()
        return publishClipboard(record.id)
    }
}
