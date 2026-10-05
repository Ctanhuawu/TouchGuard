package com.ccwait.touchguard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults.flingBehavior
import androidx.compose.foundation.pager.PagerDefaults.pageNestedScrollConnection
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.components.LocalMainPagerState
import com.ccwait.touchguard.ui.components.MainPagerState
import com.ccwait.touchguard.ui.components.SukiNavigationBar
import com.ccwait.touchguard.ui.components.rememberMainPagerState
import com.ccwait.touchguard.ui.pages.HomePage
import com.ccwait.touchguard.ui.pages.LogsPage
import com.ccwait.touchguard.ui.pages.PolicyPage
import com.ccwait.touchguard.ui.pages.SettingsPage
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride

/**
 * 应用主交互容器组件 (MainScreen)
 * 职责：
 * 1. 承载 HorizontalPager 4 大主页面调度
 * 2. 集成 SukiNavigationBar 底部导航与毛玻璃背景联动
 * 3. 隔离 Activity 生命周期与 UI 树架构
 */
@Composable
fun MainScreen(
    initialTab: Int = 0,
    mainPagerState: MainPagerState = rememberMainPagerState(
        pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 3), pageCount = { 4 })
    ),
    onLockToggle: () -> Unit,
    onScreenHoldStateUpdate: () -> Unit,
    onVibrate: (Long) -> Unit,
    onResetLock: () -> Unit
) {
    val themeMode = AppPreferences.themeMode

    val navigator = com.ccwait.touchguard.ui.navigation.LocalNavigator.current
    val isBackEnabled by remember {
        derivedStateOf {
            navigator.current() is com.ccwait.touchguard.ui.navigation.Route.Main &&
                navigator.backStackSize() == 1 &&
                mainPagerState.selectedPage != 0
        }
    }
    BackHandler(enabled = isBackEnabled) {
        mainPagerState.animateToPage(0)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    val navDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalNavigationEventDispatcherOwner provides navDispatcherOwner,
    ) {
        val surfaceColor = MiuixTheme.colorScheme.surface
                val blurBackdrop = rememberBlurBackdrop(true)
                val backdrop = rememberLayerBackdrop {
                    drawRect(surfaceColor)
                    drawContent()
                }

                val bottomBar = @Composable {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SukiNavigationBar(
                            blurBackdrop = blurBackdrop,
                            backdrop = backdrop,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                val pagerContent = @Composable { bottomInnerPadding: Dp ->
                    val pagerBoxModifier = if (themeMode == AppThemeMode.Miuix && blurBackdrop != null) {
                        Modifier.layerBackdrop(blurBackdrop)
                    } else {
                        Modifier
                    }
                    Box(modifier = pagerBoxModifier) {
                        HorizontalPager(
                            state = mainPagerState.pagerState,
                            beyondViewportPageCount = 1,
                            overscrollEffect = null,
                            userScrollEnabled = true,
                            pageNestedScrollConnection = pageNestedScrollConnection(
                                state = mainPagerState.pagerState,
                                orientation = Orientation.Horizontal,
                            ),
                            flingBehavior = flingBehavior(
                                state = mainPagerState.pagerState,
                                snapAnimationSpec = PagerNavigationSpringSpec,
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .pagerGestureOverride(
                                    pagerState = mainPagerState.pagerState,
                                    mode = PagerInterceptionMode.Native,
                                    enabled = true,
                                )
                                .then(
                                    if (themeMode == AppThemeMode.Miuix && AppPreferences.isFloatingBottomBarEnabled && AppPreferences.isFloatingBottomBarBlurEnabled)
                                        Modifier.layerBackdrop(backdrop)
                                    else Modifier
                                )
                        ) { page ->
                            when (page) {
                                0 -> HomePage(
                                    bottomInnerPadding = bottomInnerPadding,
                                    onLockToggle = onLockToggle
                                )
                                1 -> PolicyPage(
                                    bottomInnerPadding = bottomInnerPadding,
                                    onLockToggle = onLockToggle,
                                    onScreenHoldStateUpdate = onScreenHoldStateUpdate,
                                    onVibrate = onVibrate
                                )
                                2 -> LogsPage(bottomInnerPadding = bottomInnerPadding)
                                3 -> SettingsPage(
                                    bottomInnerPadding = bottomInnerPadding,
                                    themeMode = themeMode,
                                    onThemeModeChange = { AppPreferences.setTheme(it) },
                                    onResetLock = onResetLock,
                                    onVibrate = onVibrate
                                )
                            }
                        }
                    }
                }

                if (themeMode == AppThemeMode.Miuix) {
                    Scaffold(
                        bottomBar = bottomBar
                    ) { innerPadding ->
                        pagerContent(innerPadding.calculateBottomPadding())
                    }
                } else {
                    androidx.compose.material3.Scaffold(
                        bottomBar = bottomBar,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) { innerPadding ->
                        pagerContent(innerPadding.calculateBottomPadding())
                    }
                }
    }
}
