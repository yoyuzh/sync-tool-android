package com.yoyuzh.cliplink.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.yoyuzh.cliplink.MainActivity
import com.yoyuzh.cliplink.domain.model.ClipboardRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipLinkNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            "剪贴板同步",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "来自其他设备的剪贴板记录通知"
        }
        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            "发布状态",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "发布成功或失败的状态提示"
        }
        notificationManager.createNotificationChannel(syncChannel)
        notificationManager.createNotificationChannel(statusChannel)
    }

    /** Notify when a remote record is received via WebSocket. */
    fun notifyRemoteRecord(record: ClipboardRecord, showPreview: Boolean) {
        ensureChannels()

        val contentText = if (showPreview) {
            record.textPreview?.take(100) ?: "新记录"
        } else {
            "收到来自 ${record.sourceDeviceId} 的新剪贴板记录"
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_RECORD_ID, record.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, record.id.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("新剪贴板记录")
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFY_ID_REMOTE_RECORD_BASE + record.id.hashCode(), notification)
    }

    /** Notify publish success. */
    fun notifyPublishSuccess(recordTitle: String) {
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle("发布成功")
            .setContentText(recordTitle.take(60))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFY_ID_PUBLISH_SUCCESS, notification)
    }

    /** Notify publish failure. */
    fun notifyPublishFailure(recordTitle: String, reason: String) {
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("发布失败")
            .setContentText("${recordTitle.take(40)} — $reason")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFY_ID_PUBLISH_FAILURE, notification)
    }

    /** Notify sync finished summary. */
    fun notifySyncFinished(count: Int) {
        if (count == 0) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("同步完成")
            .setContentText("已同步 $count 条记录")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFY_ID_SYNC_FINISHED, notification)
    }

    companion object {
        const val CHANNEL_SYNC = "cliplink-sync"
        const val CHANNEL_STATUS = "cliplink-status"
        const val EXTRA_RECORD_ID = "cliplink_record_id"

        private const val NOTIFY_ID_PUBLISH_SUCCESS = 1001
        private const val NOTIFY_ID_PUBLISH_FAILURE = 1002
        private const val NOTIFY_ID_SYNC_FINISHED = 1003
        private const val NOTIFY_ID_REMOTE_RECORD_BASE = 2000
    }
}
