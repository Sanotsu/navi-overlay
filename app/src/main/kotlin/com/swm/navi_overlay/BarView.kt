package com.swm.navi_overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * 底部遮罩条视图。
 *
 * 形状：顶部左右两端有弧形"牛角"向上延伸（弧半径 = hornRadiusDp），
 * 中间为直线，模拟手机物理 R 角的视觉效果。
 *
 * hornRadiusDp = 0 时退化为纯矩形。
 */
class BarView(
    context: Context,
    showHints: Boolean = false,
    hornRadiusDp: Float = 0f,
    initialColor: Int = Color.BLACK
) : View(context) {

    private val fillPaint = Paint().apply { color = initialColor }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
    }

    var showHints: Boolean = showHints
        set(value) { if (field != value) { field = value; invalidate() } }

    var hornRadiusDp: Float = hornRadiusDp
        set(value) {
            if (field != value) {
                field = value
                rebuildPath()
                updateBackground()
                invalidate()
            }
        }

    var barColor: Int = initialColor
        set(value) {
            if (field != value) {
                field = value
                fillPaint.color = value
                updateBackground()
                invalidate()
            }
        }

    private val path = Path()

    init {
        updateBackground()
    }

    /** 牛角=0 → 不透明背景；牛角>0 → 透明背景（Path 决定填充区域）。 */
    private fun updateBackground() {
        setBackgroundColor(if (hornRadiusDp <= 0f) barColor else Color.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPath()
    }

    private fun rebuildPath() {
        path.reset()
        val w = width.toFloat()
        val h = height.toFloat()
        val r = hornRadiusDp * resources.displayMetrics.density
        if (r <= 0f || w <= 0f || h <= 0f) return

        // 左牛角：圆心 (r, 0) ——在屏幕内侧，与物理 R 角同心
        // 弧从 (0,0) 逆时针到 (r,r)，弧线凸向左下方（贴着屏幕边缘）
        val leftRect = RectF(0f, -r, 2 * r, r) // center (r, 0)
        path.moveTo(0f, 0f)
        path.arcTo(leftRect, 180f, -90f, false)

        // 直线顶边
        path.lineTo(w - r, r)

        // 右牛角：圆心 (w-r, 0) ——在屏幕内侧
        // 弧从 (w-r,r) 逆时针到 (w,0)
        val rightRect = RectF(w - 2 * r, -r, w, r) // center (w-r, 0)
        path.arcTo(rightRect, 90f, -90f, false)

        // 右边 → 底边 → 闭合
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        val r = hornRadiusDp * resources.displayMetrics.density
        if (r > 0f) {
            canvas.clipPath(path)
        }
        canvas.drawColor(barColor)

        if (!showHints) return
        val w = width.toFloat()
        val bodyTop = if (r > 0f) r else 0f
        val hintCenterY = (bodyTop + height) / 2f
        val dotR = ((height - bodyTop) * 0.18f).coerceAtMost(dp(4f))
        for (p in floatArrayOf(0.25f, 0.5f, 0.75f)) {
            canvas.drawCircle(w * p, hintCenterY, dotR, hintPaint)
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
