package com.ccwait.touchguard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.ui.theme.AppSettings
import com.ccwait.touchguard.ui.theme.LocalColorMode
import com.ccwait.touchguard.ui.theme.LocalEnableBlur
import com.ccwait.touchguard.ui.theme.LocalEnableFloatingBottomBar
import com.ccwait.touchguard.ui.theme.LocalEnableFloatingBottomBarBlur
import com.ccwait.touchguard.ui.theme.LocalEnableNavigationBadge
import com.ccwait.touchguard.ui.theme.MaterialAppTheme
import com.ccwait.touchguard.ui.theme.MiuixAppTheme

enum class AppThemeMode {
    Material3,
    Miuix
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.Miuix }

/**
 * 完整对齐 SukiSU-Ultra 规范的主题承载容器 (KernelSUTheme 对齐版)
 */
@Composable
fun AppThemeContainer(
    themeMode: AppThemeMode = AppPreferences.themeMode,
    appSettings: AppSettings = AppPreferences.getAppSettings(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppThemeMode provides themeMode,
        LocalColorMode provides appSettings.colorMode.value,
        LocalEnableBlur provides AppPreferences.isFloatingBottomBarBlurEnabled,
        LocalEnableFloatingBottomBar provides AppPreferences.isFloatingBottomBarEnabled,
        LocalEnableFloatingBottomBarBlur provides AppPreferences.isFloatingBottomBarBlurEnabled,
        LocalEnableNavigationBadge provides AppPreferences.isNavigationBadgeEnabled,
    ) {
        when (themeMode) {
            AppThemeMode.Miuix -> MiuixAppTheme(
                appSettings = appSettings,
                content = content
            )
            AppThemeMode.Material3 -> MaterialAppTheme(
                appSettings = appSettings,
                content = content
            )
        }
    }
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun isInDarkTheme(): Boolean = com.ccwait.touchguard.ui.theme.isInDarkTheme()

