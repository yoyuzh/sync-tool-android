package com.yoyuzh.cliplink.accessibility

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventRouterTest {
    @Test
    fun debouncesRepeatedPackageClassWithinWindow() {
        val router = AccessibilityEventRouter(debounceMillis = 200)
        val first = snapshot(timestampMillis = 1_000)
        val repeated = snapshot(timestampMillis = 1_100)

        assertEquals("com.example", router.route(first)?.packageName)
        assertNull(router.route(repeated))
    }

    @Test
    fun rejectsPasswordFields() {
        val router = AccessibilityEventRouter()

        assertNull(router.route(snapshot(isPassword = true)))
    }

    @Test
    fun acceptsEditableFocusEventOutsideDenylist() {
        val router = AccessibilityEventRouter(denylist = setOf("blocked.app"))
        val context = router.route(snapshot(isEditable = true))

        assertTrue(context?.isEditable == true)
    }

    private fun snapshot(
        timestampMillis: Long = 1_000,
        isPassword: Boolean = false,
        isEditable: Boolean = false
    ) = AccessibilityEventSnapshot(
        packageName = "com.example",
        className = "android.widget.EditText",
        eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED,
        timestampMillis = timestampMillis,
        isEditable = isEditable,
        isPassword = isPassword
    )
}
