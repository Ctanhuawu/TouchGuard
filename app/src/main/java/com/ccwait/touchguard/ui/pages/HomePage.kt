package com.ccwait.touchguard.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode

/**
 * 页面 1: 主页 (HomePage)
 * SukiSU-Ultra 架构模式：纯分发器，依据当前主题模式独立分发至 Miuix / Material 3 纯净实现
 */
@Composable
fun HomePage(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit = {}
) {
    when (LocalAppThemeMode.current) {
        AppThemeMode.Miuix -> HomeMiuix(
            bottomInnerPadding = bottomInnerPadding,
            onLockToggle = onLockToggle
        )
        AppThemeMode.Material3 -> HomeMaterial(
            bottomInnerPadding = bottomInnerPadding,
            onLockToggle = onLockToggle
        )
    }
}
