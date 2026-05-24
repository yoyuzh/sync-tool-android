package com.yoyuzh.cliplink.accessibility

class FocusContextTracker {
    var current: FocusContext? = null
        private set

    fun update(context: FocusContext) {
        current = context
    }
}
