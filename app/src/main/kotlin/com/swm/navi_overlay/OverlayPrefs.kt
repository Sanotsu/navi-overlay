package com.swm.navi_overlay

import android.content.Context
import android.content.SharedPreferences

object OverlayPrefs {
    private const val FILE = "overlay_prefs"
    private const val KEY_HEIGHT_DP = "height_dp"
    private const val KEY_WHITELIST = "whitelist"
    private const val KEY_PAUSED = "paused"
    private const val KEY_SHOW_HINTS = "show_hints"
    private const val KEY_SHOW_HINTS_EVER_SET = "show_hints_ever_set"
    private const val KEY_RUNNING = "running"
    private const val KEY_NOTICE_ENABLED = "notice_enabled"
    private const val KEY_HEIGHT_EVER_SET = "height_ever_set"
    private const val KEY_SKIP_HEIGHT_WARNING = "skip_height_warning"
    private const val KEY_CORNER_RADIUS = "corner_radius"
    private const val KEY_HORN_RADIUS = "horn_radius"
    private const val KEY_BAR_COLOR = "bar_color"

    // 信息显示模块（第一梯队）
    private const val KEY_SHOW_CLOCK = "show_clock"
    private const val KEY_SHOW_BATTERY = "show_battery"
    private const val KEY_SHOW_CUSTOM_TEXT = "show_custom_text"
    private const val KEY_CUSTOM_TEXT = "custom_text"
    private const val KEY_LOW_BATTERY_WARNING = "low_battery_warning"
    private const val KEY_LOW_BATTERY_THRESHOLD = "low_battery_threshold"
    private const val KEY_BREATH_ANIMATION = "breath_animation"

    // 信息显示模块（第二梯队：系统指标）
    private const val KEY_SHOW_DATE = "show_date"
    private const val KEY_SHOW_BATTERY_TEMP = "show_battery_temp"
    private const val KEY_SHOW_BATTERY_VOLTAGE = "show_battery_voltage"
    private const val KEY_SHOW_CPU_TEMP = "show_cpu_temp"
    private const val KEY_SHOW_CPU_USAGE = "show_cpu_usage"
    private const val KEY_SHOW_RAM_USAGE = "show_ram_usage"
    private const val KEY_SHOW_STORAGE = "show_storage"

    const val DEFAULT_HEIGHT_DP = 42f
    const val DEFAULT_BAR_COLOR = 0xFF000000.toInt()
    const val DEFAULT_LOW_BATTERY_THRESHOLD = 20
    const val DEFAULT_CUSTOM_TEXT = "navi-overlay"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getHeightDp(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_HEIGHT_DP, DEFAULT_HEIGHT_DP)
    fun setHeightDp(ctx: Context, value: Float) {
        prefs(ctx).edit().putFloat(KEY_HEIGHT_DP, value).apply()
    }

    fun getHeightEverSet(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_HEIGHT_EVER_SET, false)
    fun setHeightEverSet(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_HEIGHT_EVER_SET, value).apply()
    }

    fun getSkipHeightWarning(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SKIP_HEIGHT_WARNING, false)
    fun setSkipHeightWarning(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SKIP_HEIGHT_WARNING, value).apply()
    }

    fun getWhitelist(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
    fun setWhitelist(ctx: Context, packages: Set<String>) {
        prefs(ctx).edit().putStringSet(KEY_WHITELIST, packages).apply()
    }

    fun isPaused(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_PAUSED, false)
    fun setPaused(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_PAUSED, value).apply()
    }

    fun getShowHints(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_SHOW_HINTS, false)
    fun setShowHints(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_HINTS, value).apply()
    }

    fun getShowHintsEverSet(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_HINTS_EVER_SET, false)
    fun setShowHintsEverSet(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_HINTS_EVER_SET, value).apply()
    }

    fun isRunning(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_RUNNING, false)
    fun setRunning(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_RUNNING, value).apply()
    }

    fun isNoticeEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NOTICE_ENABLED, false)
    fun setNoticeEnabled(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_NOTICE_ENABLED, value).apply()
    }
    fun getCornerRadius(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_CORNER_RADIUS, 0f)

    fun setCornerRadius(ctx: Context, value: Float) {
        prefs(ctx).edit().putFloat(KEY_CORNER_RADIUS, value).apply()
    }

    fun getHornRadius(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_HORN_RADIUS, 0f)

    fun setHornRadius(ctx: Context, value: Float) {
        prefs(ctx).edit().putFloat(KEY_HORN_RADIUS, value).apply()
    }

    fun getBarColor(ctx: Context): Int =
        prefs(ctx).getInt(KEY_BAR_COLOR, DEFAULT_BAR_COLOR)

    fun setBarColor(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_BAR_COLOR, value).apply()
    }

    // ── 信息显示模块 ──

    fun getShowClock(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_CLOCK, false)
    fun setShowClock(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_CLOCK, value).apply()
    }

    fun getShowBattery(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_BATTERY, false)
    fun setShowBattery(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_BATTERY, value).apply()
    }

    fun getShowCustomText(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_CUSTOM_TEXT, false)
    fun setShowCustomText(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_CUSTOM_TEXT, value).apply()
    }

    fun getCustomText(ctx: Context): String =
        prefs(ctx).getString(KEY_CUSTOM_TEXT, DEFAULT_CUSTOM_TEXT) ?: DEFAULT_CUSTOM_TEXT
    fun setCustomText(ctx: Context, value: String) {
        prefs(ctx).edit().putString(KEY_CUSTOM_TEXT, value).apply()
    }

    fun getLowBatteryWarning(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_LOW_BATTERY_WARNING, true)
    fun setLowBatteryWarning(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LOW_BATTERY_WARNING, value).apply()
    }

    fun getLowBatteryThreshold(ctx: Context): Int =
        prefs(ctx).getInt(KEY_LOW_BATTERY_THRESHOLD, DEFAULT_LOW_BATTERY_THRESHOLD)
    fun setLowBatteryThreshold(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_LOW_BATTERY_THRESHOLD, value).apply()
    }

    fun getBreathAnimation(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BREATH_ANIMATION, false)
    fun setBreathAnimation(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_BREATH_ANIMATION, value).apply()
    }

    // ── 第二梯队：系统指标开关 ──

    fun getShowDate(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_DATE, false)
    fun setShowDate(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_DATE, value).apply()
    }

    fun getShowBatteryTemp(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_BATTERY_TEMP, false)
    fun setShowBatteryTemp(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_BATTERY_TEMP, value).apply()
    }

    fun getShowBatteryVoltage(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_BATTERY_VOLTAGE, false)
    fun setShowBatteryVoltage(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_BATTERY_VOLTAGE, value).apply()
    }

    fun getShowCpuTemp(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_CPU_TEMP, false)
    fun setShowCpuTemp(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_CPU_TEMP, value).apply()
    }

    fun getShowCpuUsage(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_CPU_USAGE, false)
    fun setShowCpuUsage(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_CPU_USAGE, value).apply()
    }

    fun getShowRamUsage(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_RAM_USAGE, false)
    fun setShowRamUsage(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_RAM_USAGE, value).apply()
    }

    fun getShowStorage(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SHOW_STORAGE, false)
    fun setShowStorage(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_SHOW_STORAGE, value).apply()
    }
}
