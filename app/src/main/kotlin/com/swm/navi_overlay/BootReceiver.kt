package com.swm.navi_overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机自启兜底。
 *
 * 注意：无障碍服务在用户开启后，系统会在开机后自动启动，本身不需要 BootReceiver。
 * 这里仅作为额外保险——如果用户曾点过「启动黑条」（running=true），
 * 但无障碍服务因故未自启，这里会刷新一次通知状态作为提醒。
 *
 * 实际上 BootReceiver 无法直接拉起无障碍服务（系统管理），
 * 真正的自启依赖无障碍服务本身的系统机制。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return
        // 若用户配置过运行 + 开启了常驻通知，重新显示通知作为可见提醒
        if (OverlayPrefs.isRunning(context) && OverlayPrefs.isNoticeEnabled(context)) {
            NoticeHelper.show(context)
        }
    }
}
