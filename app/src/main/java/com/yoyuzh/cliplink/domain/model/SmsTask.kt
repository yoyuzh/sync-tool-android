package com.yoyuzh.cliplink.domain.model

data class SmsTask(
    val taskId: String,
    val createdAtMillis: Long,
    val targetPhoneNumberMasked: String,
    val messagePreview: String,
    val status: String
)
