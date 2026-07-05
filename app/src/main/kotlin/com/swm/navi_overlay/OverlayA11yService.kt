package com.swm.navi_overlay

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.accessibility.AccessibilityEvent
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
 *
 * 信息显示模块的数据采集：
 * - 电量 / 充电 / 电池温度 / 电池电压：监听 ACTION_BATTERY_CHANGED 粘性广播，零权限
 * - 时钟 / 日期：主线程 Handler，对齐到下一分钟边界刷新（约 60s 一次）
 * - CPU 温度 / CPU 使用率 / 内存 / 存储：主线程 Handler，每 [METRICS_POLL_INTERVAL_MS] 轮询一次
 *
 * 上述状态由服务集中持有，View 重建（refreshBar）时通过 pushRuntimeState 回填。
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

    // 信息显示运行时状态
    private var lastBatteryPercent: Int = -1
    private var lastIsCharging: Boolean = false
    private var lastBatteryTempCelsius: Float = SystemMetrics.INVALID
    private var lastBatteryVoltageVolts: Float = SystemMetrics.INVALID
    private var lastCpuTempCelsius: Float = SystemMetrics.INVALID
    private var lastCpuUsagePercent: Int = -1
    private var lastRamUsagePercent: Int = -1
    private var lastStorageGB: Float = SystemMetrics.INVALID
    private var currentTimeText: String = ""
    private var currentDateText: String = ""

    private var batteryReceiver: BroadcastReceiver? = null
    private var clockHandler: Handler? = null
    private var clockRunnable: Runnable? = null
    private var metricsHandler: Handler? = null
    private var metricsRunnable: Runnable? = null

    companion object {
        @Volatile
        var instance: OverlayA11yService? = null
            private set

        fun isLive(): Boolean = instance != null

        /** CPU 温度/使用率/内存/存储 轮询间隔（毫秒）。 */
        private const val METRICS_POLL_INTERVAL_MS = 3000L

        private const val TAG = "navi_metrics"

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
        registerBatteryReceiver()
        startClockTick()
        updateVisibility()
    }

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
        unregisterBatteryReceiver()
        stopClockTick()
        super.onDestroy()
    }

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
        val totalHeightPx = barHeightPx + hornPx

        val color = OverlayPrefs.getBarColor(this)
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
            showHints = showHints,
            hornRadiusDp = hornRadius,
            initialColor = color,
            showClock = OverlayPrefs.getShowClock(this),
            showDate = OverlayPrefs.getShowDate(this),
            showBattery = OverlayPrefs.getShowBattery(this),
            showBatteryTemp = OverlayPrefs.getShowBatteryTemp(this),
            showBatteryVoltage = OverlayPrefs.getShowBatteryVoltage(this),
            showCpuTemp = OverlayPrefs.getShowCpuTemp(this),
            showCpuUsage = OverlayPrefs.getShowCpuUsage(this),
            showRamUsage = OverlayPrefs.getShowRamUsage(this),
            showStorage = OverlayPrefs.getShowStorage(this),
            showCustomText = OverlayPrefs.getShowCustomText(this),
            customText = OverlayPrefs.getCustomText(this),
            lowBatteryWarning = OverlayPrefs.getLowBatteryWarning(this),
            lowBatteryThreshold = OverlayPrefs.getLowBatteryThreshold(this),
            breathAnimation = OverlayPrefs.getBreathAnimation(this)
        )
        try {
            windowManager.addView(view, params)
            barView = view
            barShown = true
            pushRuntimeState()
            // 按需启动系统指标轮询（避免无开关时空转 I/O）
            val needPoll = needsMetricsPolling()
            Log.d(TAG, "addBar: needsMetricsPolling=$needPoll (cpuTemp=${OverlayPrefs.getShowCpuTemp(this)} cpuUsage=${OverlayPrefs.getShowCpuUsage(this)} ram=${OverlayPrefs.getShowRamUsage(this)} storage=${OverlayPrefs.getShowStorage(this)})")
            if (needPoll) startMetricsPolling()
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
        stopMetricsPolling()
    }

    /** 当前是否启用了任意需要轮询的指标。 */
    private fun needsMetricsPolling(): Boolean {
        return OverlayPrefs.getShowCpuTemp(this) ||
            OverlayPrefs.getShowCpuUsage(this) ||
            OverlayPrefs.getShowRamUsage(this) ||
            OverlayPrefs.getShowStorage(this)
    }

    /** 把当前缓存的所有运行时状态推送到 BarView（addBar 后立即显示）。 */
    private fun pushRuntimeState() {
        val v = barView ?: return
        v.currentTimeText = currentTimeText
        v.dateText = currentDateText
        v.batteryPercent = lastBatteryPercent
        v.isCharging = lastIsCharging
        v.batteryTempCelsius = lastBatteryTempCelsius
        v.batteryVoltageVolts = lastBatteryVoltageVolts
        v.cpuTempCelsius = lastCpuTempCelsius
        v.cpuUsagePercent = lastCpuUsagePercent
        v.ramUsagePercent = lastRamUsagePercent
        v.storageAvailGB = lastStorageGB
        v.updateBreathAnimator()
        v.invalidate()
    }

    // ── 电量广播（含温度/电压） ──────────────────────────────────────

    private fun registerBatteryReceiver() {
        if (batteryReceiver != null) return
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateBatteryFromIntent(intent)
            }
        }
        // ACTION_BATTERY_CHANGED 是粘性广播，注册后会立即收到最近一次状态
        // targetSdk >= 34 必须显式声明 RECEIVER_NOT_EXPORTED（系统广播仍可正常接收）
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterBatteryReceiver() {
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        batteryReceiver = null
    }

    private fun updateBatteryFromIntent(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        // EXTRA_TEMPERATURE: 摄氏度 ×10（320 = 32.0°C）
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val tempC = if (tempRaw > 0) tempRaw / 10f else SystemMetrics.INVALID
        // EXTRA_VOLTAGE: 毫伏（4200 = 4.2V）
        val voltRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val volt = if (voltRaw > 0) voltRaw / 1000f else SystemMetrics.INVALID

        if (percent != lastBatteryPercent || charging != lastIsCharging ||
            tempC != lastBatteryTempCelsius || volt != lastBatteryVoltageVolts
        ) {
            lastBatteryPercent = percent
            lastIsCharging = charging
            lastBatteryTempCelsius = tempC
            lastBatteryVoltageVolts = volt
            barView?.let { v ->
                v.batteryPercent = percent
                v.isCharging = charging
                v.batteryTempCelsius = tempC
                v.batteryVoltageVolts = volt
                v.updateBreathAnimator()
            }
        }
    }

    // ── 时钟 + 日期 ────────────────────────────────────────────────

    private fun startClockTick() {
        if (clockHandler != null) return
        clockHandler = Handler(Looper.getMainLooper())
        clockRunnable = object : Runnable {
            override fun run() {
                refreshClockAndDate()
                // 对齐到下一分钟边界（+500ms 容错，避免系统调度误差导致跳秒）
                val now = System.currentTimeMillis()
                val nextMinute = 60_000L - (now % 60_000L) + 500L
                clockHandler?.postDelayed(this, nextMinute)
            }
        }
        clockRunnable?.run()
    }

    private fun stopClockTick() {
        clockRunnable?.let { clockHandler?.removeCallbacks(it) }
        clockRunnable = null
        clockHandler = null
    }

    private fun refreshClockAndDate() {
        val is24Hour = DateFormat.is24HourFormat(this)
        val timePattern = if (is24Hour) "HH:mm" else "h:mm"
        val now = Date()
        val locale = Locale.getDefault()

        val time = SimpleDateFormat(timePattern, locale).format(now)
        if (time != currentTimeText) {
            currentTimeText = time
            barView?.currentTimeText = time
        }
        // 日期/星期："M/d E"（中文→"12/5 周日"，英文→"12/5 Sun"）
        val date = SimpleDateFormat("M/d E", locale).format(now)
        if (date != currentDateText) {
            currentDateText = date
            barView?.dateText = date
        }
    }

    // ── 系统指标轮询（CPU 温度/使用率/内存/存储） ───────────────────

    private fun startMetricsPolling() {
        if (metricsHandler != null) return
        Log.d(TAG, "metrics polling STARTED (interval=${METRICS_POLL_INTERVAL_MS}ms)")
        metricsHandler = Handler(Looper.getMainLooper())
        metricsRunnable = object : Runnable {
            override fun run() {
                refreshPolledMetrics()
                metricsHandler?.postDelayed(this, METRICS_POLL_INTERVAL_MS)
            }
        }
        metricsRunnable?.run()
    }

    private fun stopMetricsPolling() {
        if (metricsHandler != null) Log.d(TAG, "metrics polling STOPPED")
        metricsRunnable?.let { metricsHandler?.removeCallbacks(it) }
        metricsRunnable = null
        metricsHandler = null
    }

    private fun refreshPolledMetrics() {
        val cpuTemp = SystemMetrics.cpuTempCelsius()
        val cpuLoad = SystemMetrics.cpuLoadPercent()
        val ramPercent = SystemMetrics.ramUsagePercent(this)
        val storageGB = SystemMetrics.availableStorageGB()

        Log.d(TAG, "poll result: cpuTemp=$cpuTemp cpuLoad=$cpuLoad ram=$ramPercent storage=$storageGB")

        var changed = false
        if (cpuTemp != SystemMetrics.INVALID && cpuTemp != lastCpuTempCelsius) {
            lastCpuTempCelsius = cpuTemp; changed = true
        }
        if (cpuLoad >= 0 && cpuLoad != lastCpuUsagePercent) {
            lastCpuUsagePercent = cpuLoad; changed = true
        }
        if (ramPercent >= 0 && ramPercent != lastRamUsagePercent) {
            lastRamUsagePercent = ramPercent; changed = true
        }
        if (storageGB != SystemMetrics.INVALID && storageGB != lastStorageGB) {
            lastStorageGB = storageGB; changed = true
        }
        if (!changed) {
            Log.d(TAG, "poll: no change, skip push")
            return
        }

        barView?.let { v ->
            if (cpuTemp != SystemMetrics.INVALID) v.cpuTempCelsius = cpuTemp
            if (cpuLoad >= 0) v.cpuUsagePercent = cpuLoad
            if (ramPercent >= 0) v.ramUsagePercent = ramPercent
            if (storageGB != SystemMetrics.INVALID) v.storageAvailGB = storageGB
            Log.d(TAG, "pushed to BarView: cpuLoad=${if (cpuLoad >= 0) "$cpuLoad%" else "skip"}")
        } ?: Log.d(TAG, "barView is null, cannot push")
    }

    // ── 供 MainActivity 调用的接口 ──

    /** 高度或 hint 变化后重新绘制。 */
    fun refreshBar() {
        heightDp = OverlayPrefs.getHeightDp(this)
        showHints = OverlayPrefs.getShowHints(this)
        if (barShown) {
            removeBar()
            updateVisibility()
        } else if (running) {
            // 黑条未显示但启用了某项轮询指标时，无需启动；显示时 addBar 会自动启动
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
