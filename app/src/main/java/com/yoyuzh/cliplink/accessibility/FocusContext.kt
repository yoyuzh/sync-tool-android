package com.yoyuzh.cliplink.accessibility

data class FocusContext(
    val packageName: String,
    val className: String,
    val eventType: Int,
    val timestampMillis: Long,
    val isEditable: Boolean
)
