package com.yoyuzh.cliplink.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

data class AccessibilityEventSnapshot(
    val packageName: String,
    val className: String,
    val eventType: Int,
    val timestampMillis: Long,
    val isEditable: Boolean,
    val isPassword: Boolean
)

class AccessibilityEventRouter(
    private val denylist: Set<String> = emptySet(),
    private val debounceMillis: Long = 200
) {
    private val acceptedTypes = setOf(
        AccessibilityEvent.TYPE_VIEW_FOCUSED,
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    )
    private var lastAccepted: AccessibilityEventSnapshot? = null

    fun route(snapshot: AccessibilityEventSnapshot): FocusContext? {
        if (snapshot.eventType !in acceptedTypes) return null
        if (snapshot.packageName in denylist) return null
        if (snapshot.isPassword) return null

        val previous = lastAccepted
        val isRepeated = previous?.packageName == snapshot.packageName &&
            previous.className == snapshot.className &&
            snapshot.timestampMillis - previous.timestampMillis < debounceMillis
        if (isRepeated) return null

        lastAccepted = snapshot
        return FocusContext(
            packageName = snapshot.packageName,
            className = snapshot.className,
            eventType = snapshot.eventType,
            timestampMillis = snapshot.timestampMillis,
            isEditable = snapshot.isEditable
        )
    }
}

fun AccessibilityEvent.toSnapshot(): AccessibilityEventSnapshot {
    val sourceNode = source
    return AccessibilityEventSnapshot(
        packageName = packageName?.toString().orEmpty(),
        className = className?.toString().orEmpty(),
        eventType = eventType,
        timestampMillis = eventTime,
        isEditable = sourceNode?.isEditable == true,
        isPassword = sourceNode?.isPassword == true || itemIsPassword(sourceNode)
    )
}

private fun itemIsPassword(node: AccessibilityNodeInfo?): Boolean {
    return node?.inputType?.let { inputType ->
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
    } ?: false
}
