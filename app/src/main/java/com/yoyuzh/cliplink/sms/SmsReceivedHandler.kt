package com.yoyuzh.cliplink.sms

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import com.yoyuzh.cliplink.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReceivedHandler @Inject constructor(
    private val repository: ClipboardRepository,
    private val settingsStore: AppSettingsStore
) {
    suspend fun handle(messages: List<InboundSmsMessage>): ClipboardRecord? {
        val parts = messages
            .sortedBy { it.receivedAtMillis }
            .filter { it.body.isNotBlank() }
        if (parts.isEmpty()) {
            return null
        }

        val sender = maskSender(parts.first().sender)
        val body = parts.joinToString(separator = "") { it.body }.trim()
        if (body.isBlank()) {
            return null
        }

        val settings = settingsStore.settings.first()
        val deviceId = settings.deviceId.ifBlank { "android-sms" }
        return repository.saveLocalText("短信来自 $sender\n$body", deviceId)
    }

    private fun maskSender(sender: String): String {
        val digits = sender.filter { it.isDigit() }
        if (digits.length <= 4) {
            return sender.ifBlank { "未知号码" }
        }
        return "${sender.takeWhile { it == '+' }}***${digits.takeLast(4)}"
    }
}
