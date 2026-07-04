@file:OptIn(ExperimentalMaterial3Api::class)

package com.swm.navi_overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot() } }
    }
}

private fun openOverlaySettings(ctx: android.content.Context) {
    try {
        ctx.startActivity(android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${ctx.packageName}")
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        ctx.startActivity(android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${ctx.packageName}")
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun requestNotificationPermission(activity: MainActivity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
        activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
}

enum class Screen { MAIN, WHITELIST }

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.MAIN) }
    when (screen) {
        Screen.MAIN -> SettingsScreen(onWhitelist = { screen = Screen.WHITELIST })
        Screen.WHITELIST -> WhitelistScreen(onBack = { screen = Screen.MAIN })
    }
}

// ── 主设置页 ──────────────────────────────────────────────────────

@Composable
fun SettingsScreen(onWhitelist: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var overlay by remember { mutableStateOf<Boolean?>(null) }
    var a11yOn by remember { mutableStateOf<Boolean?>(null) }
    var a11yLive by remember { mutableStateOf(false) }
    var navMode by remember { mutableStateOf<String?>(null) }
    var navHeight by remember { mutableStateOf(0f) }
    val brand = remember { ManufacturerUtils.detect() }
    var wlCount by remember { mutableStateOf(OverlayPrefs.getWhitelist(ctx).size) }

    var height by remember { mutableFloatStateOf(OverlayPrefs.getHeightDp(ctx)) }
    var showHints by remember { mutableStateOf(OverlayPrefs.getShowHints(ctx)) }
    var notice by remember { mutableStateOf(OverlayPrefs.isNoticeEnabled(ctx)) }
    var running by remember { mutableStateOf(OverlayPrefs.isRunning(ctx)) }
    var paused by remember { mutableStateOf(OverlayPrefs.isPaused(ctx)) }

    // 手势模式高度超出手势条的警告
    var showHeightWarn by remember { mutableStateOf(false) }
    var pendingHeight by remember { mutableFloatStateOf(0f) }
    var skipWarn by remember { mutableStateOf(OverlayPrefs.getSkipHeightWarning(ctx)) }

    var hornR by remember { mutableFloatStateOf(OverlayPrefs.getHornRadius(ctx)) }
    var screenR by remember { mutableFloatStateOf(0f) }
    var cornerSource by remember { mutableStateOf("") }
    var barColor by remember { mutableIntStateOf(OverlayPrefs.getBarColor(ctx)) }
    var statusBarH by remember { mutableFloatStateOf(NavUtils.getStatusBarHeightDp(ctx)) }

    fun refresh() {
        overlay = android.provider.Settings.canDrawOverlays(ctx)
        a11yOn = A11yUtils.isEnabled(ctx)
        a11yLive = OverlayA11yService.isLive()
        navMode = NavUtils.detectNavMode(ctx)
        navHeight = NavUtils.getNavigationBarHeightDp(ctx)
        wlCount = OverlayPrefs.getWhitelist(ctx).size
        running = OverlayPrefs.isRunning(ctx)
        paused = OverlayPrefs.isPaused(ctx)
        hornR = OverlayPrefs.getHornRadius(ctx)
        val cornerInfo = NavUtils.getScreenCornerInfo(ctx)
        screenR = cornerInfo.radiusDp
        cornerSource = cornerInfo.source
        barColor = OverlayPrefs.getBarColor(ctx)
        statusBarH = NavUtils.getStatusBarHeightDp(ctx)
        if (NavUtils.detectNavMode(ctx) == "three_button" && !OverlayPrefs.getShowHintsEverSet(ctx)) {
            showHints = true
            OverlayPrefs.setShowHints(ctx, true)
            OverlayPrefs.setShowHintsEverSet(ctx, true)
            OverlayA11yService.instance?.refreshBar()
        }
        // 首次加载：手势模式默认高度 = 手势条高度，避免误遮挡
        if (!OverlayPrefs.getHeightEverSet(ctx)) {
            val nH = NavUtils.getNavigationBarHeightDp(ctx)
            val mode = NavUtils.detectNavMode(ctx)
            val safe = if (mode == "gesture" && nH in 12f..48f) nH else 42f
            if (java.lang.Math.abs(height - safe) > 0.5f) {
                height = safe
                OverlayPrefs.setHeightDp(ctx, safe)
                OverlayA11yService.instance?.refreshBar()
            }
        }
    }

    // 加载
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text("底部黑条") },
                actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "刷新") } })
        },
        floatingActionButton = {
            if (running) FloatingActionButton(onClick = {
                scope.launch {
                    val n = !paused
                    OverlayPrefs.setPaused(ctx, n)
                    OverlayA11yService.instance?.onPausedChanged(n)
                    NoticeHelper.refreshIfEnabled(ctx)
                    paused = n
                }
            }) { Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, "暂停/恢复") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {

            // ── 权限 ──
            PermissionCard(overlay, a11yOn, a11yLive, navMode, navHeight,
                { openOverlaySettings(ctx) },
                { A11yUtils.openSettings(ctx) })
            Spacer(Modifier.height(12.dp))

            // ── 高度 ──
            HeightCard(
                height = height,
                navMode = navMode,
                navHeight = navHeight,
                statusBarHeight = statusBarH,
                onChanged = { v ->
                    height = v; OverlayPrefs.setHeightDp(ctx, v)
                    OverlayA11yService.instance?.refreshBar()
                },
                onFinished = {
                    OverlayPrefs.setHeightEverSet(ctx, true)
                    if (navMode == "gesture" && height > navHeight + 1f && !skipWarn) {
                        pendingHeight = height; showHeightWarn = true
                    }
                },
                onMatchStatusBar = {
                    height = statusBarH
                    OverlayPrefs.setHeightDp(ctx, statusBarH)
                    OverlayPrefs.setHeightEverSet(ctx, true)
                    OverlayA11yService.instance?.refreshBar()
                }
            )
            Spacer(Modifier.height(12.dp))

            // ── 牛角弧 ──
            HornCard(
                radius = hornR,
                screenRadius = screenR,
                source = cornerSource,
                onChanged = { v ->
                    hornR = v; OverlayPrefs.setHornRadius(ctx, v)
                    OverlayA11yService.instance?.refreshBar()
                },
                onAuto = {
                    val info = NavUtils.getScreenCornerInfo(ctx)
                    if (info.radiusDp > 0f) {
                        hornR = info.radiusDp; OverlayPrefs.setHornRadius(ctx, info.radiusDp)
                        screenR = info.radiusDp; cornerSource = info.source
                        OverlayA11yService.instance?.refreshBar()
                    } else {
                        scope.launch { snackbar.showSnackbar("未能自动检测，请手动调整") }
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            // ── 颜色 ──
            ColorCard(
                color = barColor,
                onChanged = { c ->
                    barColor = c; OverlayPrefs.setBarColor(ctx, c)
                    OverlayA11yService.instance?.refreshBar()
                }
            )
            Spacer(Modifier.height(12.dp))

            // ── 定位点 ──
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (navMode == "three_button") Icons.Default.MoreHoriz else Icons.Default.TouchApp, null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("显示按键定位点", fontWeight = FontWeight.Bold)
                        Text(if (navMode == "three_button") "在黑条上显示三个半透明圆点" else "手势导航，可手动开启",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = showHints, onCheckedChange = { v ->
                        showHints = v; OverlayPrefs.setShowHints(ctx, v)
                        OverlayA11yService.instance?.refreshBar()
                    })
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── 常驻通知 ──
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("常驻通知栏", fontWeight = FontWeight.Bold)
                        Text("默认关闭，开启后显示暂停/恢复按钮", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = notice, onCheckedChange = { v ->
                        scope.launch {
                            notice = v; OverlayPrefs.setNoticeEnabled(ctx, v)
                            if (v) { NoticeHelper.ensureChannel(ctx); NoticeHelper.show(ctx) }
                            else NoticeHelper.cancel(ctx)
                        }
                    })
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── 白名单 ──
            Card(onClick = onWhitelist) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlaylistAddCheck, null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("白名单 · 不显示黑条的应用", fontWeight = FontWeight.Bold)
                        Text("${wlCount} 个应用", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── 后台保活 ──
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryStd, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("后台保活引导 · ${brand.brand}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(brand.steps)
                    if (brand.extraTip != null) {
                        Spacer(Modifier.height(4.dp)); Text(brand.extraTip, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("贴士：无障碍服务系统优先级高，通常不会被杀。此引导仅为激进省电策略的厂商兜底。",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── 说明 ──
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("工作原理", fontWeight = FontWeight.Bold)
                        Text("无障碍服务在屏幕底部绘制纯黑遮罩，触摸穿透。竖屏生效，横屏自动隐藏。白名单/暂停时不显示。开机自动启动。",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── 控制按钮 ──
            if (running) {
                Button(onClick = {
                    scope.launch {
                        OverlayPrefs.setRunning(ctx, false)
                        OverlayA11yService.instance?.onRunningChanged(false)
                        NoticeHelper.refreshIfEnabled(ctx)
                        running = false; paused = false
                    }
                }, modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("停止") }
            } else {
                Button(onClick = {
                    scope.launch {
                        if (!android.provider.Settings.canDrawOverlays(ctx)) {
                            snackbar.showSnackbar("请先开启悬浮窗权限"); openOverlaySettings(ctx); return@launch
                        }
                        if (!A11yUtils.isEnabled(ctx)) {
                            snackbar.showSnackbar("请先开启无障碍服务"); A11yUtils.openSettings(ctx); return@launch
                        }
                        OverlayPrefs.setRunning(ctx, true)
                        OverlayA11yService.instance?.onRunningChanged(true)
                        NoticeHelper.refreshIfEnabled(ctx)
                        running = true; paused = false
                    }
                }, modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("启动黑条") }
            }
            if (running) {
                Spacer(Modifier.height(8.dp))
                Text(
                    when { paused -> "已暂停"; a11yLive -> "运行中 · 底部黑条生效"; else -> "等待无障碍服务…" },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
        // 手势模式高度超出手势条 → 确认弹窗
        if (showHeightWarn) {
            AlertDialog(
                onDismissRequest = { showHeightWarn = false },
                title = { Text("确认高度") },
                text = {
                    Column {
                        Text("当前为手势导航，手势条高度约 ${navHeight.toInt()} dp。" +
                            "黑条高度超过手势条可能会遮挡应用底部导航栏按钮。")
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = skipWarn, onCheckedChange = { skipWarn = it })
                            Text("不再询问", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        OverlayPrefs.setSkipHeightWarning(ctx, skipWarn)
                        showHeightWarn = false
                    }) { Text("确定（使用 ${pendingHeight.toInt()} dp）") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        height = navHeight.coerceAtLeast(16f)
                        OverlayPrefs.setHeightDp(ctx, height)
                        OverlayA11yService.instance?.refreshBar()
                        showHeightWarn = false
                    }) { Text("取消") }
                }
            )
        }
    }
}

// ── 权限卡片 ──────────────────────────────────────────────────────

@Composable
fun PermissionCard(
    overlay: Boolean?, a11y: Boolean?, a11yLive: Boolean,
    navMode: String?, navHeight: Float,
    onOverlay: () -> Unit, onA11y: () -> Unit
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            if (navMode != null) {
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (navMode == "three_button") Icons.Default.Menu else Icons.Default.TouchApp, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("检测：${if (navMode == "three_button") "三键导航" else "手势导航"}（${String.format("%.0f dp", navHeight)}）")
                }
            }
            PermRow(Icons.Default.Accessible, "无障碍服务",
                if (a11yLive) "已开启 · 运行中" else "必需 · 保活与开机自启", a11y,
                if (a11yLive) "在线" else "未运行", onA11y)
            HorizontalDivider()
            PermRow(Icons.Default.Layers, "显示在其他应用上层", "必需 · 绘制悬浮窗", overlay, null, onOverlay)
        }
    }
}

@Composable
fun PermRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String,
            state: Boolean?, extra: String?, onClick: () -> Unit) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title); Text(sub, style = MaterialTheme.typography.bodySmall) }
        extra?.let { Text(it, style = MaterialTheme.typography.labelSmall,
            color = if (it == "在线") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(4.dp)) }
        if (state == false) TextButton(onClick = onClick) { Text("去开启") }
        Spacer(Modifier.width(4.dp))
        when (state) {
            true -> Icon(Icons.Default.CheckCircle, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            false -> Icon(Icons.Default.ErrorOutline, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
            null -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

// ── 高度滑块 ──────────────────────────────────────────────────────

@Composable
fun HeightCard(
    height: Float,
    navMode: String?,
    navHeight: Float,
    statusBarHeight: Float,
    onChanged: (Float) -> Unit,
    onFinished: () -> Unit,
    onMatchStatusBar: () -> Unit
) {
    val isGesture = navMode == "gesture"
    val barDp = navHeight.toInt()
    Card {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text("黑条高度", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${height.toInt()} dp")
            }
            Slider(
                value = height, onValueChange = onChanged,
                onValueChangeFinished = onFinished,
                valueRange = 16f..96f, steps = 79
            )
            Text(
                if (isGesture) "手势导航 · 手势条约 $barDp dp，超出可能遮挡底部按钮"
                else "三键导航 · 建议 42~48 dp，不会遮挡应用内容",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                Text(
                    "状态栏高度：${statusBarHeight.toInt()} dp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.weight(1f))
                if (statusBarHeight > 0f) {
                    TextButton(
                        onClick = onMatchStatusBar,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("设为同步") }
                }
            }
        }
    }
}

// ── 牛角弧卡片 ───────────────────────────────────────────────────

@Composable
fun HornCard(
    radius: Float,
    screenRadius: Float,
    source: String,
    onChanged: (Float) -> Unit,
    onAuto: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("牛角弧半径", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(if (radius <= 0f) "关闭" else "${radius.toInt()} dp")
            }
            Slider(
                value = radius, onValueChange = onChanged,
                valueRange = 0f..60f, steps = 59
            )
            Column {
                Text(
                    if (radius <= 0f) "关闭 = 纯遮罩，无弧形装饰"
                    else "已开启，上方左右延伸弧形与屏幕 R 角呼应",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "检测结果：${if (screenRadius > 0f) "${screenRadius.toInt()} dp" else "无"} ($source)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onAuto) {
                    Text(if (screenRadius > 0f) "应用检测值 (${screenRadius.toInt()}dp)" else "尝试自动检测")
                }
            }
        }
    }
}

// ── 颜色选择卡片 ──────────────────────────────────────────────────

@Composable
fun ColorCard(color: Int, onChanged: (Int) -> Unit) {
    val presets = remember {
        listOf(
            0xFF000000.toInt(), // 纯黑
            0xFF2C2C2C.toInt(), // 炭灰
            0xFF1A237E.toInt(), // 靛蓝
            0xFF0D3818.toInt(), // 墨绿
            0xFF3E2723.toInt(), // 深咖
            0xFF311B92.toInt()  // 深紫
        )
    }
    var hexInput by remember(color) { mutableStateOf(String.format("%06X", color and 0xFFFFFF)) }
    var showPicker by remember { mutableStateOf(false) }

    val cCompose = androidx.compose.ui.graphics.Color(color)

    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("遮罩颜色", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(String.format("#%06X", color and 0xFFFFFF),
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                // 点击预览色块 → 弹出颜色选择器
                Box(
                    Modifier.size(36.dp)
                        .background(cCompose)
                        .clickable { showPicker = true }
                )
            }
            Spacer(Modifier.height(12.dp))
            // 预设色块
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                presets.forEach { c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(40.dp)
                                .background(androidx.compose.ui.graphics.Color(c))
                                .clickable {
                                    onChanged(c)
                                    hexInput = String.format("%06X", c and 0xFFFFFF)
                                }
                        )
                        if (c == color) Text("✓", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 十六进制输入
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { v ->
                        val clean = v.filter { it.isLetterOrDigit() }.take(6)
                        hexInput = clean.uppercase()
                        val n = clean.toLongOrNull(16)
                        if (n != null && clean.length == 6) {
                            onChanged(0xFF000000.toInt() or n.toInt())
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showPicker = true }) { Text("更多...") }
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initialColor = color,
            onConfirm = { c ->
                onChanged(c)
                hexInput = String.format("%06X", c and 0xFFFFFF)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

// ── RGB 颜色选择器弹窗 ────────────────────────────────────────────

@Composable
fun ColorPickerDialog(initialColor: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var r by remember { mutableIntStateOf((initialColor shr 16) and 0xFF) }
    var g by remember { mutableIntStateOf((initialColor shr 8) and 0xFF) }
    var b by remember { mutableIntStateOf(initialColor and 0xFF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色") },
        text = {
            Column {
                // 预览
                Box(
                    Modifier.fillMaxWidth().height(56.dp)
                        .background(androidx.compose.ui.graphics.Color(red = r, green = g, blue = b))
                )
                Spacer(Modifier.height(16.dp))
                // R 滑块
                Text("红  $r", style = MaterialTheme.typography.bodySmall)
                Slider(value = r.toFloat(), onValueChange = { r = it.toInt() },
                    valueRange = 0f..255f, colors = SliderDefaults.colors())
                // G 滑块
                Text("绿  $g", style = MaterialTheme.typography.bodySmall)
                Slider(value = g.toFloat(), onValueChange = { g = it.toInt() },
                    valueRange = 0f..255f)
                // B 滑块
                Text("蓝  $b", style = MaterialTheme.typography.bodySmall)
                Slider(value = b.toFloat(), onValueChange = { b = it.toInt() },
                    valueRange = 0f..255f)
                Spacer(Modifier.height(8.dp))
                Text(String.format("#%02X%02X%02X", r, g, b),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
