package com.swm.navi_overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * 可选的常驻通知栏入口。
 *
 * 注意：这是「快捷入口」性质的普通通知，**不依赖前台服务保活**。
 * 保活由无障碍服务负责；通知只是给用户一个可见的暂停/恢复入口。
 */
object NoticeHelper {

    const val CHANNEL_ID = "BlackBarNotice"
    const val NOTICE_ID = 2

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "黑条快捷入口",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "可选的常驻通知，提供暂停/恢复快捷开关"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /** 根据当前状态构建通知。 */
    fun build(context: Context): Notification {
        ensureChannel(context)
        val running = OverlayPrefs.isRunning(context)
        val paused = OverlayPrefs.isPaused(context)

        // 暂停/恢复 Action
        val toggleIntent = Intent(context, NoticeActionReceiver::class.java).apply {
            action = NoticeActionReceiver.ACTION_TOGGLE_PAUSE
        }
        val togglePi = PendingIntent.getBroadcast(
            context, 1, toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 停止 Action
        val stopIntent = Intent(context, NoticeActionReceiver::class.java).apply {
            action = NoticeActionReceiver.ACTION_STOP
        }
        val stopPi = PendingIntent.getBroadcast(
            context, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when {
            !running -> "底部黑条（未启动）"
            paused -> "底部黑条 · 已暂停"
            else -> "底部黑条运行中"
        }

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setOngoing(true)
            .setShowWhen(false)
            .setColorized(true)
            .setColor(0xFF1F1F1F.toInt())

        // 打开 App
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }

        if (running) {
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(
                        context,
                        if (paused) android.R.drawable.ic_media_play
                        else android.R.drawable.ic_media_pause
                    ),
                    if (paused) "恢复" else "暂停",
                    togglePi
                ).build()
            )
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(
                        context, android.R.drawable.ic_menu_close_clear_cancel
                    ),
                    "停止",
                    stopPi
                ).build()
            )
        }
        return builder.build()
    }

    fun show(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTICE_ID, build(context))
    }

    fun cancel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTICE_ID)
    }

    /** 状态变化后刷新通知（仅在用户开启了常驻通知时生效）。 */
    fun refreshIfEnabled(context: Context) {
        if (OverlayPrefs.isNoticeEnabled(context)) show(context)
    }
}
