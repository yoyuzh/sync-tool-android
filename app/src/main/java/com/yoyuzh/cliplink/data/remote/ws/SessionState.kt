package com.yoyuzh.cliplink.data.remote.ws

sealed class SessionState {
    object Disconnected : SessionState()
    object Connecting : SessionState()
    data class Online(
        val onlineDeviceCount: Int = 0,
        val connectedAtMillis: Long = System.currentTimeMillis()
    ) : SessionState()
    data class Error(val message: String) : SessionState()

    val isOnline get() = this is Online
    val label get() = when (this) {
        is Disconnected -> "已断开"
        is Connecting -> "连接中..."
        is Online -> "已连接"
        is Error -> "连接失败"
    }
}
