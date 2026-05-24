package com.yoyuzh.cliplink.sms

class SmsTaskPolicy {
    fun canSendRealSms(userAuthorized: Boolean): Boolean = false

    fun disabledReason(): String {
        return "SMS sending is disabled in version one and requires explicit authorization before implementation."
    }
}
