# ── Compose ──
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Kotlin ──
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── R8 full mode ──
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ── 保持无障碍服务（系统反射调用）──
-keep class com.swm.navi_overlay.OverlayA11yService { *; }

# ── 保持 BroadcastReceiver ──
-keep class com.swm.navi_overlay.NoticeActionReceiver { *; }
-keep class com.swm.navi_overlay.BootReceiver { *; }

# ── 保持 Activity（Manifest 引用）──
-keep class com.swm.navi_overlay.MainActivity { *; }

# ── 保持 SharedPreferences key 不被混淆 ──
-keepclassmembers class com.swm.navi_overlay.OverlayPrefs { *; }
-keepclassmembers class com.swm.navi_overlay.OverlayPrefsKt { *; }
