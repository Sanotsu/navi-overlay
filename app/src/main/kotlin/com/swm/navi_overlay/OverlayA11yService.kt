package com.swm.navi_overlay

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.accessibility.AccessibilityEvent
import android.view.WindowManager

/**
 * 核心服务：基于无障碍服务（AccessibilityService）。
 *
 * 为什么用无障碍服务而不是前台服务：
 * - 系统级保活：用户开启后系统不会因后台清理杀死它，开机自动启动
 * - 无需常驻通知：默认无通知，体验干净
 * - 实时前台检测：onAccessibilityEvent 比 UsageStats 轮询更准更快
 *
 * 黑条显示规则：
 * - 暂停 → 隐藏
 * - 横屏 → 隐藏（横屏下顶部刘海在侧边，底部黑条不对称）
 * - 前台 App 在白名单 → 隐藏
 * - running=false → 隐藏（用户点了"停止"）
 * - 其他 → 显示
 */
class OverlayA11yService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var barView: BarView? = null
    private var barShown = false

    private var orientation = Configuration.ORIENTATION_PORTRAIT
    private var currentForeground: String? = null
    private var heightDp = OverlayPrefs.DEFAULT_HEIGHT_DP
    private var showHints = false
    private var isPaused = false
    private var running = false

    companion object {
        @Volatile
        var instance: OverlayA11yService? = null
            private set

        /** 当前无障碍服务是否已运行（用于 UI 判断服务是否在线） */
        fun isLive(): Boolean = instance != null

        private val FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        reloadConfig()
        updateVisibility()
    }

    /** 从 prefs 重新读取全部配置。 */
    fun reloadConfig() {
        heightDp = OverlayPrefs.getHeightDp(this)
        showHints = OverlayPrefs.getShowHints(this)
        isPaused = OverlayPrefs.isPaused(this)
        running = OverlayPrefs.isRunning(this)
        updateVisibility()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != currentForeground) {
                currentForeground = pkg
                updateVisibility()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation != orientation) {
            orientation = newConfig.orientation
            updateVisibility()
        }
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        removeBar()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        removeBar()
        super.onDestroy()
    }

    /** 决定黑条显示/隐藏的核心逻辑。 */
    private fun updateVisibility() {
        val whitelist = OverlayPrefs.getWhitelist(this)
        val inWhitelist = currentForeground != null && currentForeground in whitelist
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val shouldShow = running && !isPaused && !isLandscape && !inWhitelist
        if (shouldShow && !barShown) addBar()
        else if (!shouldShow && barShown) removeBar()
    }

    private fun addBar() {
        if (barShown || barView != null) return

        val density = resources.displayMetrics.density
        val barHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, heightDp, resources.displayMetrics
        ).toInt().coerceAtLeast(1)

        val hornRadius = OverlayPrefs.getHornRadius(this)
        val hornPx = (hornRadius * density).toInt()
        // View 总高 = 黑条高度 + 牛角高度（牛角在 View 顶部向上延伸）
        val totalHeightPx = barHeightPx + hornPx

        val color = OverlayPrefs.getBarColor(this)
        // 牛角 > 0 需要透明背景（RGBA_8888）；= 0 用 RGBX_8888 保证纯黑
        val format = if (hornPx > 0) PixelFormat.RGBA_8888 else PixelFormat.RGBX_8888

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            totalHeightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            FLAGS,
            format
        ).apply {
            gravity = Gravity.BOTTOM
            title = "navi_overlay_blackbar"
            alpha = 1.0f
            dimAmount = 0.0f
        }

        val view = BarView(
            context = this,
            showHints = this@OverlayA11yService.showHints,
            hornRadiusDp = hornRadius,
            initialColor = color
        )
        try {
            windowManager.addView(view, params)
            barView = view
            barShown = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeBar() {
        barView?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (_: Exception) {
            }
        }
        barView = null
        barShown = false
    }

    // ── 供 Flutter 通过 MainActivity 调用的接口 ──

    /** 高度或 hint 变化后重新绘制。 */
    fun refreshBar() {
        heightDp = OverlayPrefs.getHeightDp(this)
        showHints = OverlayPrefs.getShowHints(this)
        if (barShown) {
            removeBar()
            updateVisibility()
        }
    }

    fun onRunningChanged(v: Boolean) {
        running = v
        OverlayPrefs.setRunning(this, v)
        updateVisibility()
    }

    fun onPausedChanged(v: Boolean) {
        isPaused = v
        OverlayPrefs.setPaused(this, v)
        updateVisibility()
    }
}
