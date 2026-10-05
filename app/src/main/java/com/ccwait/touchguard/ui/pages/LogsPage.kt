package com.ccwait.touchguard.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode

/**
 * 页面 3: 日志 (LogsPage)
 * SukiSU-Ultra 架构模式：纯分发器，依据当前主题模式独立分发至 Miuix / Material 3 纯净实现
 */
@Composable
fun LogsPage(bottomInnerPadding: Dp) {
    when (LocalAppThemeMode.current) {
        AppThemeMode.Miuix -> LogsMiuix(bottomInnerPadding = bottomInnerPadding)
        AppThemeMode.Material3 -> LogsMaterial(bottomInnerPadding = bottomInnerPadding)
    }
}
