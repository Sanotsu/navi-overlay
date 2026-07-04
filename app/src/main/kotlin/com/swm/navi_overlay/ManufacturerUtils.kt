package com.swm.navi_overlay

import android.os.Build

/**
 * 厂商识别与后台保活引导。
 * 国产 ROM 后台策略差异大，提供针对性的引导文案，减少用户配置成本。
 */
object ManufacturerUtils {

    data class BatteryGuide(
        val brand: String,
        val steps: String,
        val extraTip: String? = null
    )

    private fun manufacturer(): String =
        (Build.MANUFACTURER ?: "unknown").lowercase()

    fun detect(): BatteryGuide {
        val m = manufacturer()
        return when {
            m.contains("xiaomi") || m.contains("redmi") -> BatteryGuide(
                "小米 / Redmi / 澎湃OS",
                "设置 → 应用设置 → 应用管理 → 底部黑条 → 省电策略 → 无限制",
                "同时在「自启动管理」中允许后台活动"
            )
            m.contains("huawei") || m.contains("honor") -> BatteryGuide(
                "华为 / 荣耀 / 鸿蒙",
                "设置 → 电池 → 启动管理 → 底部黑条 → 手动管理（三个开关全部打开）"
            )
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") -> BatteryGuide(
                "OPPO / 一加 / realme / ColorOS",
                "设置 → 电池 → 应用耗电管理 → 底部黑条 → 允许后台运行 + 允许自启动"
            )
            m.contains("vivo") || m.contains("iqoo") -> BatteryGuide(
                "vivo / iQOO / OriginOS",
                "设置 → 电池 → 后台耗电管理 → 底部黑条 → 允许后台高耗电"
            )
            m.contains("samsung") -> BatteryGuide(
                "三星 / One UI",
                "设置 → 电池和设备维护 → 电池 → 后台使用限制 → 从不休眠应用中添加「底部黑条」"
            )
            m.contains("meizu") -> BatteryGuide(
                "魅族 / Flyme",
                "设置 → 电池 → 后台管理 → 底部黑条 → 允许后台运行"
            )
            m.contains("pixel") || m.contains("google") -> BatteryGuide(
                "Pixel / 原生 Android",
                "原生系统后台策略宽松，通常无需额外配置",
                "若被杀，在「最近任务」里给本应用加锁"
            )
            else -> BatteryGuide(
                "通用 Android",
                "在「最近任务」列表里下拉锁定本应用，或在电池设置中允许后台运行"
            )
        }
    }
}
