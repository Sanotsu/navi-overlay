package com.swm.navi_overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * 底部遮罩条视图。
 *
 * 形状：顶部左右两端有弧形"牛角"向上延伸（弧半径 = hornRadiusDp），
 * 中间为直线，模拟手机物理 R 角的视觉效果。
 *
 * hornRadiusDp = 0 时退化为纯矩形。
 *
 * 信息显示模块（与顶部状态栏视觉对称）：
 * - **自定义文字模式**（独占）：整行居中显示用户文字，更大字号；与系统信息互斥
 * - **系统信息模式**：左区（时钟 + 日期）+ 右区（电量/温度/电压/CPU/RAM/存储 指标组）
 *
 * 两种模式由 UI 层强制互斥（开启任一会自动关闭另一）；BarView 渲染时若两者同时为真，
 * 自定义文字模式优先（防御性处理）。
 *
 * 不同指标使用不同颜色以视觉区分：
 * - 白色：时钟 / 自定义文字 / 电量
 * - 浅灰：日期
 * - 暖橙：电池温度
 * - 青：电池电压
 * - 蓝：CPU 温度 / CPU 使用率
 * - 绿：RAM
 * - 黄：存储
 * - 红：低电量警告（电量文字 + 整体呼吸叠加）
 *
 * 信息显示优先于按键定位点；当黑条身体高度不足（< 22dp）时自动隐藏文字模块。
 */
class BarView(
    context: Context,
    showHints: Boolean = false,
    hornRadiusDp: Float = 0f,
    initialColor: Int = Color.BLACK,
    showClock: Boolean = false,
    showDate: Boolean = false,
    showBattery: Boolean = false,
    showBatteryTemp: Boolean = false,
    showBatteryVoltage: Boolean = false,
    showCpuTemp: Boolean = false,
    showCpuUsage: Boolean = false,
    showRamUsage: Boolean = false,
    showStorage: Boolean = false,
    showCustomText: Boolean = false,
    customText: String = "",
    lowBatteryWarning: Boolean = true,
    lowBatteryThreshold: Int = 20,
    breathAnimation: Boolean = false
) : View(context) {

    private val fillPaint = Paint().apply { color = initialColor }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY
        isFakeBoldText = true
    }
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 255, 220, 80)
        isFakeBoldText = true
    }
    private val breathPaint = Paint().apply {
        color = Color.argb(255, 229, 57, 53)
    }
    private val boltPath = Path()

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

    // ── 显示开关（创建时传入，refreshBar 会重建 View） ──
    var showClock: Boolean = showClock
        set(value) { if (field != value) { field = value; invalidate() } }
    var showDate: Boolean = showDate
        set(value) { if (field != value) { field = value; invalidate() } }
    var showBattery: Boolean = showBattery
        set(value) { if (field != value) { field = value; updateBreathAnimator(); invalidate() } }
    var showBatteryTemp: Boolean = showBatteryTemp
        set(value) { if (field != value) { field = value; invalidate() } }
    var showBatteryVoltage: Boolean = showBatteryVoltage
        set(value) { if (field != value) { field = value; invalidate() } }
    var showCpuTemp: Boolean = showCpuTemp
        set(value) { if (field != value) { field = value; invalidate() } }
    var showCpuUsage: Boolean = showCpuUsage
        set(value) { if (field != value) { field = value; invalidate() } }
    var showRamUsage: Boolean = showRamUsage
        set(value) { if (field != value) { field = value; invalidate() } }
    var showStorage: Boolean = showStorage
        set(value) { if (field != value) { field = value; invalidate() } }
    var showCustomText: Boolean = showCustomText
        set(value) { if (field != value) { field = value; invalidate() } }
    var customText: String = customText
        set(value) { if (field != value) { field = value; invalidate() } }
    var lowBatteryWarning: Boolean = lowBatteryWarning
        set(value) { if (field != value) { field = value; updateBreathAnimator(); invalidate() } }
    var lowBatteryThreshold: Int = lowBatteryThreshold
        set(value) { if (field != value) { field = value; updateBreathAnimator(); invalidate() } }
    var breathAnimation: Boolean = breathAnimation
        set(value) { if (field != value) { field = value; updateBreathAnimator(); invalidate() } }

    // ── 运行时状态（由服务推送） ──
    var currentTimeText: String = ""
        set(value) { if (field != value) { field = value; invalidate() } }
    var dateText: String = ""
        set(value) { if (field != value) { field = value; invalidate() } }
    var batteryPercent: Int = -1
        set(value) { if (field != value) { field = value; updateBreathAnimator(); invalidate() } }
    var isCharging: Boolean = false
        set(value) { if (field != value) { field = value; updateBreathAnimator(); invalidate() } }
    var batteryTempCelsius: Float = SystemMetrics.INVALID
        set(value) { if (field != value) { field = value; invalidate() } }
    var batteryVoltageVolts: Float = SystemMetrics.INVALID
        set(value) { if (field != value) { field = value; invalidate() } }
    var cpuTempCelsius: Float = SystemMetrics.INVALID
        set(value) { if (field != value) { field = value; invalidate() } }
    var cpuUsagePercent: Int = -1
        set(value) { if (field != value) { field = value; invalidate() } }
    var ramUsagePercent: Int = -1
        set(value) { if (field != value) { field = value; invalidate() } }
    var storageAvailGB: Float = SystemMetrics.INVALID
        set(value) { if (field != value) { field = value; invalidate() } }

    // 呼吸动画当前值（0..1），由 animator 驱动
    private var breathAlpha: Float = 0f
    private var breathAnimator: ValueAnimator? = null

    private val path = Path()

    init {
        updateBackground()
    }

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

        val leftRect = RectF(0f, -r, 2 * r, r)
        path.moveTo(0f, 0f)
        path.arcTo(leftRect, 180f, -90f, false)
        path.lineTo(w - r, r)
        val rightRect = RectF(w - 2 * r, -r, w, r)
        path.arcTo(rightRect, 90f, -90f, false)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
    }

    /** 当前是否处于低电量警告状态。 */
    private val isLowBattery: Boolean
        get() = lowBatteryWarning && batteryPercent in 0..lowBatteryThreshold && !isCharging

    /** 系统信息模式：任一信息开关打开（不含自定义文字，二者互斥）。 */
    private val isInfoActive: Boolean
        get() = showClock || showDate || showBattery || showBatteryTemp ||
            showBatteryVoltage || showCpuTemp || showCpuUsage ||
            showRamUsage || showStorage

    /** 自定义文字模式：开关打开且有内容。与系统信息互斥，渲染时优先级更高。 */
    private val isCustomTextMode: Boolean
        get() = showCustomText && customText.isNotEmpty()

    /** 黑条身体高度是否足够绘制文字。 */
    private fun bodyHeightSufficient(): Boolean {
        val bodyTop = if (hornRadiusDp > 0f) hornRadiusDp * resources.displayMetrics.density else 0f
        return (height - bodyTop) >= dp(22f)
    }

    override fun onDraw(canvas: Canvas) {
        val r = hornRadiusDp * resources.displayMetrics.density
        val bodyTop = if (r > 0f) r else 0f
        if (r > 0f) {
            canvas.clipPath(path)
        }
        canvas.drawColor(barColor)

        // 低电量呼吸叠加层
        if (breathAlpha > 0f) {
            breathPaint.alpha = (60 + breathAlpha * 80).toInt()
            canvas.drawRect(0f, bodyTop, width.toFloat(), height.toFloat(), breathPaint)
        }

        // 自定义文字模式与系统信息模式互斥；二者均优先于按键定位点
        val sufficient = bodyHeightSufficient()
        when {
            isCustomTextMode && sufficient -> drawCustomTextOnly(canvas, bodyTop)
            isInfoActive && sufficient -> drawInfo(canvas, bodyTop)
            showHints -> drawHintDots(canvas, bodyTop)
        }
    }

    private fun drawHintDots(canvas: Canvas, bodyTop: Float) {
        val w = width.toFloat()
        val hintCenterY = (bodyTop + height) / 2f
        val dotR = ((height - bodyTop) * 0.18f).coerceAtMost(dp(4f))
        for (p in floatArrayOf(0.25f, 0.5f, 0.75f)) {
            canvas.drawCircle(w * p, hintCenterY, dotR, hintPaint)
        }
    }

    /**
     * 系统信息模式：三区布局 [左：时钟+日期]    [右：指标组]
     * （无中区——自定义文字与之互斥，不会同时出现）
     */
    private fun drawInfo(canvas: Canvas, bodyTop: Float) {
        val sidePadding = dp(12f)
        val bodyH = height - bodyTop
        val textSize = (bodyH * 0.42f).coerceIn(dp(10f), dp(13f))
        textPaint.textSize = textSize

        val fm = textPaint.fontMetrics
        val baseline = (bodyTop + bodyH / 2f) - (fm.ascent + fm.descent) / 2f

        drawRightZone(canvas, baseline, sidePadding)
        drawLeftZone(canvas, baseline, sidePadding)
    }

    /**
     * 自定义文字模式：独占整行，居中显示。
     * 由于无其他信息竞争空间，可使用更大字号（bodyH × 52%，比信息模式大 ~24%）。
     */
    private fun drawCustomTextOnly(canvas: Canvas, bodyTop: Float) {
        val bodyH = height - bodyTop
        val centerY = bodyTop + bodyH / 2f
        val textSize = (bodyH * 0.52f).coerceIn(dp(12f), dp(16f))
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = COLOR_PRIMARY

        val fm = textPaint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f

        val sidePadding = dp(16f)
        val maxWidth = (width.toFloat() - 2 * sidePadding).coerceAtLeast(dp(40f))
        val display = ellipsizeIfNeeded(customText, maxWidth, textPaint)
        canvas.drawText(display, width / 2f, baseline, textPaint)
    }

    /** 左区：时钟 + 日期。返回绘制内容的右边缘 x。 */
    private fun drawLeftZone(canvas: Canvas, baseline: Float, sidePadding: Float): Float {
        val items = buildList {
            if (showClock && currentTimeText.isNotEmpty()) {
                add(currentTimeText to COLOR_PRIMARY)
            }
            if (showDate && dateText.isNotEmpty()) {
                add(dateText to COLOR_DATE)
            }
        }
        if (items.isEmpty()) return sidePadding

        textPaint.textAlign = Paint.Align.LEFT
        var x = sidePadding
        val gap = dp(8f)
        for ((text, color) in items) {
            textPaint.color = color
            canvas.drawText(text, x, baseline, textPaint)
            x += textPaint.measureText(text) + gap
        }
        return x - gap
    }

    /**
     * 右区：从右到左依次绘制电量组、CPU 组、系统组。
     *
     * 顺序（从右到左）：电量% + 充电闪电 → 电池电压 → 电池温度 → CPU 温度 → CPU 使用率 → RAM → 存储
     * 这样电池组（最常看的）贴近右边缘，与系统状态栏习惯一致。
     *
     * 返回绘制内容的最左 x（中区不能超过此位置）。
     */
    private fun drawRightZone(canvas: Canvas, baseline: Float, sidePadding: Float): Float {
        val items = buildList<Pair<String, Int>> {
            // 电池组
            if (showBatteryVoltage && batteryVoltageVolts >= 0) {
                add(String.format(LocaleCtx(), "%.1fV", batteryVoltageVolts) to COLOR_VOLTAGE)
            }
            if (showBatteryTemp && batteryTempCelsius >= 0) {
                add("${batteryTempCelsius.toInt()}°" to COLOR_BATTERY_TEMP)
            }
            // CPU 组
            if (showCpuTemp && cpuTempCelsius >= 0) {
                add("${cpuTempCelsius.toInt()}°" to COLOR_CPU_TEMP)
            }
            if (showCpuUsage && cpuUsagePercent >= 0) {
                add("${cpuUsagePercent}%" to COLOR_CPU_USAGE)
            }
            // 系统组
            if (showRamUsage && ramUsagePercent >= 0) {
                add("${ramUsagePercent}%" to COLOR_RAM)
            }
            if (showStorage && storageAvailGB >= 0) {
                add(String.format(LocaleCtx(), "%.0fG", storageAvailGB) to COLOR_STORAGE)
            }
        }

        textPaint.textAlign = Paint.Align.RIGHT
        val gap = dp(6f)
        var x = width - sidePadding

        // 先处理电量%（最右，含充电闪电图标）
        if (showBattery && batteryPercent >= 0) {
            val batteryText = "$batteryPercent%"
            textPaint.color = if (isLowBattery) COLOR_LOW_BATTERY else COLOR_PRIMARY
            canvas.drawText(batteryText, x, baseline, textPaint)
            val textW = textPaint.measureText(batteryText)
            if (isCharging) {
                val boltSize = ((height - (if (hornRadiusDp > 0f)
                    hornRadiusDp * resources.displayMetrics.density else 0f)) * 0.6f)
                    .coerceAtLeast(dp(8f))
                val centerY = baseline + (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
                val boltCx = x - textW - dp(5f) - boltSize / 2f
                drawLightningBolt(canvas, boltCx, centerY, boltSize)
                x -= textW + dp(5f) + boltSize + gap
            } else {
                x -= textW + gap
            }
        }

        // 剩余指标（按 items 顺序从右到左追加绘制）
        for ((text, color) in items) {
            textPaint.color = color
            canvas.drawText(text, x, baseline, textPaint)
            x -= textPaint.measureText(text) + gap
        }

        return x + gap // 返回最左边缘（中区右边界）
    }

    /** 绘制简单闪电图标（Path，避免引入矢量资源增大 APK）。size ≈ 图标外接高度。 */
    private fun drawLightningBolt(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val s = size
        boltPath.reset()
        boltPath.moveTo(cx + s * 0.10f, cy - s * 0.50f)
        boltPath.lineTo(cx - s * 0.40f, cy + s * 0.05f)
        boltPath.lineTo(cx - s * 0.05f, cy + s * 0.05f)
        boltPath.lineTo(cx - s * 0.10f, cy + s * 0.50f)
        boltPath.lineTo(cx + s * 0.40f, cy - s * 0.05f)
        boltPath.lineTo(cx + s * 0.05f, cy - s * 0.05f)
        boltPath.close()
        canvas.drawPath(boltPath, boltPaint)
    }

    /** 文字过宽时用省略号截断。 */
    private fun ellipsizeIfNeeded(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisW = paint.measureText(ellipsis)
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + ellipsisW > maxWidth) {
            end--
        }
        return if (end <= 0) ellipsis else text.substring(0, end) + ellipsis
    }

    /** 触发条件满足时启动呼吸动画；否则停止。需在主线程调用。 */
    fun updateBreathAnimator() {
        val shouldAnimate = breathAnimation && isLowBattery
        if (shouldAnimate && breathAnimator == null) {
            breathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 2500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    breathAlpha = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else if (!shouldAnimate && breathAnimator != null) {
            breathAnimator?.cancel()
            breathAnimator = null
            breathAlpha = 0f
        }
    }

    override fun onDetachedFromWindow() {
        breathAnimator?.cancel()
        breathAnimator = null
        breathAlpha = 0f
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun LocaleCtx(): java.util.Locale = java.util.Locale.getDefault()

    companion object {
        // 指标颜色（alpha 225 ≈ 88% 不透明，平衡可读性与不喧宾夺主）
        private val COLOR_PRIMARY = Color.argb(225, 255, 255, 255)      // 白：时钟/电量/自定义
        private val COLOR_DATE = Color.argb(195, 200, 200, 200)         // 浅灰：日期
        private val COLOR_BATTERY_TEMP = Color.argb(225, 255, 167, 38)  // 暖橙：电池温度
        private val COLOR_VOLTAGE = Color.argb(225, 128, 222, 234)      // 青：电池电压
        private val COLOR_CPU_TEMP = Color.argb(225, 100, 181, 246)     // 蓝：CPU 温度
        private val COLOR_CPU_USAGE = Color.argb(225, 149, 117, 205)    // 紫：CPU 使用率
        private val COLOR_RAM = Color.argb(225, 129, 199, 132)          // 绿：内存
        private val COLOR_STORAGE = Color.argb(225, 255, 213, 79)       // 黄：存储
        private val COLOR_LOW_BATTERY = Color.argb(245, 255, 90, 90)    // 红：低电量警告
    }
}
