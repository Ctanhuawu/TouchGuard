package com.ccwait.touchguard.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode

/**
 * 设置页面 (SettingsPage)
 * SukiSU-Ultra 架构模式：纯分发器，依据当前主题模式独立分发至 Miuix / Material 3 纯净实现
 */
@Composable
fun SettingsPage(
    bottomInnerPadding: Dp,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onVibrate: (Long) -> Unit
) {
    when (LocalAppThemeMode.current) {
        AppThemeMode.Miuix -> SettingsMiuix(
            bottomInnerPadding = bottomInnerPadding,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onVibrate = onVibrate
        )
        AppThemeMode.Material3 -> SettingsMaterial(
            bottomInnerPadding = bottomInnerPadding,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onVibrate = onVibrate
        )
    }
}
