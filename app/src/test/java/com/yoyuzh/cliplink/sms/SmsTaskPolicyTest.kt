package com.yoyuzh.cliplink.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsTaskPolicyTest {
    @Test
    fun realSmsSendingIsDisabledForFirstVersion() {
        val policy = SmsTaskPolicy()

        assertFalse(policy.canSendRealSms(userAuthorized = true))
        assertTrue(policy.disabledReason().contains("disabled in version one"))
    }
}
