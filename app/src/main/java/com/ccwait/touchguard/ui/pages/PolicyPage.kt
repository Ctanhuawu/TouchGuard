package com.ccwait.touchguard.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode

/**
 * 页面 2: 锁定策略 (PolicyPage)
 * SukiSU-Ultra 架构模式：纯分发器，依据当前主题模式独立分发至 Miuix / Material 3 纯净实现
 */
@Composable
fun PolicyPage(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit,
    onScreenHoldStateUpdate: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    when (LocalAppThemeMode.current) {
        AppThemeMode.Miuix -> PolicyMiuix(
            bottomInnerPadding = bottomInnerPadding,
            onLockToggle = onLockToggle,
            onScreenHoldStateUpdate = onScreenHoldStateUpdate,
            onVibrate = onVibrate
        )
        AppThemeMode.Material3 -> PolicyMaterial(
            bottomInnerPadding = bottomInnerPadding,
            onLockToggle = onLockToggle,
            onScreenHoldStateUpdate = onScreenHoldStateUpdate,
            onVibrate = onVibrate
        )
    }
}
