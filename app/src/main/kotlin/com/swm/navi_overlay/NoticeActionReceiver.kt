package com.swm.navi_overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 处理常驻通知上的「暂停/恢复」「停止」按钮点击。
 *
 * 通过静态 instance 直接操作无障碍服务；若服务未在线（用户未开启无障碍），
 * 仅更新 prefs，服务下次启动时会读取最新状态。
 */
class NoticeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_PAUSE -> {
                val current = OverlayPrefs.isPaused(context)
                val next = !current
                OverlayPrefs.setPaused(context, next)
                OverlayA11yService.instance?.onPausedChanged(next)
                NoticeHelper.refreshIfEnabled(context)
            }
            ACTION_STOP -> {
                OverlayPrefs.setRunning(context, false)
                OverlayA11yService.instance?.onRunningChanged(false)
                NoticeHelper.refreshIfEnabled(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_PAUSE = "com.swm.navi_overlay.NOTICE_TOGGLE_PAUSE"
        const val ACTION_STOP = "com.swm.navi_overlay.NOTICE_STOP"
    }
}
