package com.yoyuzh.cliplink.worker

import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class WorkSchedulerTest {
    @Test
    fun historySyncRequestUsesConservativePeriodicConstraints() {
        val request: PeriodicWorkRequest = WorkScheduler.buildHistorySyncRequest()

        assertTrue(request.workSpec.intervalDuration >= TimeUnit.MINUTES.toMillis(15))
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals("cliplink-history-sync", WorkScheduler.HISTORY_SYNC_WORK_NAME)
    }
}
