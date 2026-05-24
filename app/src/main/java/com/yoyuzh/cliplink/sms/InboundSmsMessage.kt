package com.yoyuzh.cliplink.sms

data class InboundSmsMessage(
    val sender: String,
    val body: String,
    val receivedAtMillis: Long
)
