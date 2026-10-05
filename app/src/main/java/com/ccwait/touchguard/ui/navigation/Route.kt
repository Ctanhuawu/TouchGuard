package com.ccwait.touchguard.ui.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 导航路由定义 (对齐 SukiSU-Ultra Routes.kt)
 */
sealed interface Route : NavKey, Parcelable {
    @Parcelize
    data object Main : Route

    @Parcelize
    data object ColorPalette : Route
}
