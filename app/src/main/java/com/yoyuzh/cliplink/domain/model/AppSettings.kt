package com.yoyuzh.cliplink.domain.model

data class AppSettings(
    val serverUrl: String = "",
    val deviceName: String = "My Android",
    val deviceId: String = "",
    val manualPublishEnabled: Boolean = true,
    val notificationPreviewEnabled: Boolean = false,
    val maxLocalHistoryItems: Int = 200,
    val syncWindowDays: Int = 7,
    val accessibilityEnabledObserved: Boolean = false,
    val excludedApps: Set<String> = emptySet()
)
