package com.yoyuzh.cliplink.sms

import com.yoyuzh.cliplink.domain.model.SmsTask

interface SmsTaskRepository {
    suspend fun listRemoteTasks(): List<SmsTask>
    suspend fun markStatus(taskId: String, status: String): Result<Unit>
}
