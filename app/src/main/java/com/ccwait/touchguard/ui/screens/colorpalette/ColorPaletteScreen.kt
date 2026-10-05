package com.ccwait.touchguard.ui.screens.colorpalette

import androidx.compose.runtime.Composable
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode
import com.ccwait.touchguard.ui.navigation.LocalNavigator

/**
 * 主题与调色板设置屏幕分发层 (ColorPaletteScreen)
 * 1:1 对齐 SukiSU-Ultra 规范：根据当前主题模式分发至 Miuix / Material 纯净实现
 */
@Composable
fun ColorPaletteScreen() {
    val navigator = LocalNavigator.current
    when (LocalAppThemeMode.current) {
        AppThemeMode.Miuix -> ColorPaletteScreenMiuix(onBack = { navigator.pop() })
        AppThemeMode.Material3 -> ColorPaletteScreenMaterial(onBack = { navigator.pop() })
    }
}
