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
    const val DEFAULT_HEIGHT_DP = 42f
    const val DEFAULT_BAR_COLOR = 0xFF000000.toInt()

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
}
