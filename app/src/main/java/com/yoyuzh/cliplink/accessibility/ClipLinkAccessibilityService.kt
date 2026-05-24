package com.yoyuzh.cliplink.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.worker.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClipLinkAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsStore: AppSettingsStore
    @Inject lateinit var workScheduler: WorkScheduler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val tracker = FocusContextTracker()
    private var router = AccessibilityEventRouter(
        denylist = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin"
        )
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Load excluded apps from settings and update denylist
        serviceScope.launch {
            val settings = settingsStore.settings.first()
            router = AccessibilityEventRouter(
                denylist = settings.excludedApps + setOf(
                    "com.android.systemui",
                    "com.google.android.inputmethod.latin"
                )
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val snapshot = event.toSnapshot()

        // Route event — router already filters password fields and denylisted apps
        val context = router.route(snapshot) ?: return
        tracker.update(context)

        // On context switch to an editable field in an allowed app, schedule a conservative sync
        if (context.isEditable) {
            serviceScope.launch {
                workScheduler.triggerImmediateSync()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
