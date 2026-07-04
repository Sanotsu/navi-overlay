@file:OptIn(ExperimentalMaterial3Api::class)

package com.swm.navi_overlay

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── 白名单页 ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var whitelist by remember { mutableStateOf(OverlayPrefs.getWhitelist(ctx)) }
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        allApps = withContext(Dispatchers.IO) { loadApps(ctx) }
        loading = false
    }

    val filtered = remember(allApps, query, showSystem) {
        val q = query.trim().lowercase()
        allApps.filter { a ->
            if (!showSystem && a.isSystem) return@filter false
            if (q.isEmpty()) true
            else a.label.lowercase().contains(q) || a.packageName.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("白名单 · 不显示黑条") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { showSystem = !showSystem }) {
                        Icon(if (showSystem) Icons.Default.VisibilityOff else Icons.Default.Visibility, "系统应用")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            whitelist = emptySet()
                            OverlayPrefs.setWhitelist(ctx, emptySet())
                            OverlayA11yService.instance?.reloadConfig()
                        }
                    }) { Icon(Icons.Default.DeleteForever, "清空") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // 搜索框
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("搜索应用名或包名") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            // 统计
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("已选 ${whitelist.size}", fontWeight = FontWeight.W600)
                Spacer(Modifier.width(8.dp))
                Text("· 显示 ${filtered.size} 个应用", style = MaterialTheme.typography.bodySmall)
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有匹配的应用")
                }
            } else {
                LazyColumn {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = app.packageName in whitelist
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = if (checked) whitelist - app.packageName else whitelist + app.packageName
                                    whitelist = next
                                    OverlayPrefs.setWhitelist(ctx, next)
                                    OverlayA11yService.instance?.reloadConfig()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { v ->
                                val next = if (v) whitelist + app.packageName else whitelist - app.packageName
                                whitelist = next
                                OverlayPrefs.setWhitelist(ctx, next)
                                OverlayA11yService.instance?.reloadConfig()
                            })
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// ─── 数据与工具 ───────────────────────────────────────────────────

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean
)

private fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val all = pm.getInstalledApplications(0)
    return all
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
        .map { app ->
            AppEntry(
                packageName = app.packageName,
                label = try { pm.getApplicationLabel(app).toString() } catch (_: Exception) { app.packageName },
                isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }
        .sortedBy { it.label.lowercase() }
}
