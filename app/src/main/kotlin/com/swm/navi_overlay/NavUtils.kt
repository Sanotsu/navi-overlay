package com.swm.navi_overlay

import android.content.Context
import android.os.Build
import android.view.RoundedCorner

object NavUtils {

    fun getNavigationBarHeightDp(ctx: Context): Float {
        val resId = ctx.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resId > 0) {
            val px = ctx.resources.getDimensionPixelSize(resId)
            return px / ctx.resources.displayMetrics.density
        }
        return 0f
    }

    /** 获取状态栏高度（dp）。 */
    fun getStatusBarHeightDp(ctx: Context): Float {
        val resId = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) {
            val px = ctx.resources.getDimensionPixelSize(resId)
            return px / ctx.resources.displayMetrics.density
        }
        return 0f
    }

    fun detectNavMode(ctx: Context): String =
        if (getNavigationBarHeightDp(ctx) >= 32f) "three_button" else "gesture"

    /**
     * 屏幕圆角信息。
     * @param radiusDp 圆角半径（dp），0 = 无圆角或未检测到
     * @param source 检测来源（Display API / 系统资源 / 未检测到）
     */
    data class CornerInfo(val radiusDp: Float, val source: String)

    /**
     * 检测屏幕物理圆角半径。
     * 优先级：Display.getRoundedCorner (API 33+) > 系统资源 > 0
     */
    fun getScreenCornerInfo(ctx: Context): CornerInfo {
        // 1. API 33+: Display.getRoundedCorner —— 最准确，Service 可直接用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.display?.let { display ->
                val corner = display.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
                if (corner != null && corner.radius > 0) {
                    val radiusDp = corner.radius / ctx.resources.displayMetrics.density
                    return CornerInfo(radiusDp, "Display API")
                }
            }
        }
        // 2. 系统内部资源（各 ROM 命名不统一，逐个尝试）
        for (name in listOf(
            "rounded_corner_radius",
            "rounded_corner_radius_bottom",
            "config_roundedCornerRadius"
        )) {
            val id = ctx.resources.getIdentifier(name, "dimen", "android")
            if (id > 0) {
                val px = ctx.resources.getDimension(id)
                if (px > 0f) {
                    return CornerInfo(px / ctx.resources.displayMetrics.density, "系统资源($name)")
                }
            }
        }
        return CornerInfo(0f, "未检测到")
    }

    /** 兼容旧调用 */
    fun getScreenCornerRadiusDp(ctx: Context): Float =
        getScreenCornerInfo(ctx).radiusDp
}
