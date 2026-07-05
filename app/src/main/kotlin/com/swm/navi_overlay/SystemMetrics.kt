package com.swm.navi_overlay

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

/**
 * 系统性能指标采集（全部零权限）。
 *
 * 数据源说明：
 * - CPU 温度：/sys/class/thermal 下的 thermal_zoneN/temp（多数设备返回毫摄氏度，需 /1000）
 * - CPU 负载：/sys/devices/system/cpu/cpuN/cpufreq 下 scaling_cur_freq / cpuinfo_max_freq
 *   ⚠ Android 10+ SELinux 禁止应用读 /proc/stat 与 /proc/loadavg，无法计算真实 CPU 时间占比；
 *   退而求其次用频率加权比作负载代理指标（boost 到最高频=高负载，降到最低频=低负载）。
 *   注意：频率不能完全等价于使用率（轻负载也可能因 boost 策略跑在最高频），仅作粗略参考。
 * - 内存使用率：[ActivityManager.MemoryInfo]
 * - 可用存储：[StatFs]
 */
object SystemMetrics {

    /** 失败标记（调用方据此跳过绘制）。 */
    const val INVALID: Float = -1f

    private const val TAG = "navi_metrics"

    private const val THERMAL_DIR = "/sys/class/thermal"
    private const val CPU_DIR = "/sys/devices/system/cpu"

    // ── CPU 温度 ──────────────────────────────────────────────────

    /**
     * 读取 CPU 温度（°C）。
     *
     * 遍历 `/sys/class/thermal/thermal_zone*`：
     * 1. 优先取 type 字段含 cpu/xclipse/silicon 等关键字的区域，返回其中**最高**温度
     *    （多核多 zone 时取最高更接近用户感知的"CPU 温度"）
     * 2. 若没有 CPU 标识的 zone，回退到第一个有效温度
     *
     * 部分设备 /sys 节点不可读，会返回 [INVALID]。
     */
    fun cpuTempCelsius(): Float {
        val dir = File(THERMAL_DIR)
        if (!dir.isDirectory) return INVALID
        val zones = dir.listFiles { f -> f.name.startsWith("thermal_zone") }
            ?: return INVALID

        var cpuTemp: Float = INVALID
        var otherTemp: Float = INVALID
        for (zone in zones) {
            val type = try {
                File(zone, "type").readText().trim()
            } catch (_: Exception) {
                continue
            }
            val tempStr = try {
                File(zone, "temp").readText().trim()
            } catch (_: Exception) {
                continue
            }
            val raw = tempStr.toFloatOrNull() ?: continue
            // 多数设备：毫摄氏度（41000 = 41°C）；少数设备：直接摄氏度
            val tempC = if (raw > 1000f) raw / 1000f else raw
            val isCpu = type.contains("cpu", ignoreCase = true) ||
                type.contains("xclipse", ignoreCase = true) ||
                type.contains("apc", ignoreCase = true) ||
                type.contains("silicon", ignoreCase = true) ||
                type.contains("soc", ignoreCase = true)
            if (isCpu) {
                if (cpuTemp == INVALID || tempC > cpuTemp) cpuTemp = tempC
            } else if (otherTemp == INVALID) {
                otherTemp = tempC
            }
        }
        return if (cpuTemp != INVALID) cpuTemp else otherTemp
    }

    // ── CPU 负载（基于频率的代理指标） ────────────────────────────

    /**
     * 估算 CPU 负载（0..100）。
     *
     * **背景**：Android 10+ SELinux 禁止应用读 /proc/stat，传统时间差分法不可用。
     *
     * **算法**：遍历所有在线核心，读取 scaling_cur_freq / cpuinfo_max_freq，
     * 用频率加权比作为负载代理：
     * - load = Σ(cur_freq) / Σ(max_freq) × 100
     *
     * **局限**：
     * - 频率不能完全等价于使用率（部分调度策略会在低负载时 boost 到最高频）
     * - 离线核心无 cpufreq 目录，自动跳过
     *
     * @return 负载百分比；无任何核心可读时返回 -1
     */
    fun cpuLoadPercent(): Int {
        val cpuDir = File(CPU_DIR)
        if (!cpuDir.isDirectory) {
            Log.w(TAG, "cpuLoad: $CPU_DIR not a directory")
            return -1
        }
        // 筛选 cpuN 目录（N 为数字），跳过 cpuidle / cpufreq 等系统节点
        val cores = cpuDir.listFiles { f ->
            f.isDirectory && f.name.matches(Regex("cpu\\d+"))
        } ?: return -1

        var totalCur = 0L
        var totalMax = 0L
        var onlineCount = 0
        for (core in cores) {
            val cur = try {
                File(core, "cpufreq/scaling_cur_freq").readText().trim().toLong()
            } catch (_: Exception) { 0L }
            val max = try {
                File(core, "cpufreq/cpuinfo_max_freq").readText().trim().toLong()
            } catch (_: Exception) { 0L }
            if (cur > 0 && max > 0) {
                totalCur += cur
                totalMax += max
                onlineCount++
            }
        }
        if (totalMax <= 0 || onlineCount == 0) {
            Log.w(TAG, "cpuLoad: no readable cpufreq cores (checked ${cores.size} entries)")
            return -1
        }
        val load = (totalCur * 100 / totalMax).toInt().coerceIn(0, 100)
        Log.d(TAG, "cpuLoad=${load}% (totalCur=$totalCur totalMax=$totalMax onlineCores=$onlineCount)")
        return load
    }

    // ── 内存 ──────────────────────────────────────────────────────

    /** 内存使用率（0..100）。失败返回 -1。 */
    fun ramUsagePercent(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return -1
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        if (mi.totalMem <= 0) return -1
        val used = mi.totalMem - mi.availMem
        return (used * 100 / mi.totalMem).toInt().coerceIn(0, 100)
    }

    // ── 存储 ──────────────────────────────────────────────────────

    /** 主存储可用空间（GB，十进制）。失败返回 [INVALID]。 */
    fun availableStorageGB(): Float {
        val path = Environment.getExternalStorageDirectory().absolutePath
        val stat = try {
            StatFs(path)
        } catch (_: Exception) {
            return INVALID
        }
        val availBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availBytes / 1_000_000_000f
    }
}
