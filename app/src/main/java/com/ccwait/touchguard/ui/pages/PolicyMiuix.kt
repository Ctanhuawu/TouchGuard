package com.ccwait.touchguard.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.model.ScreenOrientationLock
import com.ccwait.touchguard.model.UnlockMechanism
import com.ccwait.touchguard.strategy.StrategyReadiness
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 页面 2: 锁定策略 Miuix 纯净实现 (PolicyMiuix)
 * 1:1 对齐 SukiSU-Ultra 规范：所有设置项均由 Miuix 原生 Preference 组件承载，零 Material 依赖
 */
@Composable
fun PolicyMiuix(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit,
    onScreenHoldStateUpdate: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.policy_title),
                    scrollBehavior = scrollBehavior
                )
            }
        },
    ) { innerPadding ->
        val lazyModifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp)

        val boxModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

        Box(modifier = boxModifier) {
            LazyColumn(
                modifier = lazyModifier,
                contentPadding = innerPadding,
                overscrollEffect = null
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. 核心控制分组
                        SmallTitle(
                            text = stringResource(R.string.policy_core_controls),
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            val currentStrategy = TouchLockManager.currentStrategy
                            val readiness = currentStrategy.readiness

                            val strategySummary = if (readiness == StrategyReadiness.READY) {
                                stringResource(currentStrategy.type.descRes)
                            } else {
                                currentStrategy.statusSummary
                            }

                            val strategyItems = StrategyType.entries.map { stringResource(it.titleRes) }
                            val currentStrategyIndex = StrategyType.entries.indexOf(TouchLockManager.currentStrategyType).coerceAtLeast(0)

                            OverlayDropdownPreference(
                                title = stringResource(R.string.policy_strategy_dropdown_title),
                                summary = strategySummary,
                                items = strategyItems,
                                selectedIndex = currentStrategyIndex,
                                onSelectedIndexChange = { index ->
                                    if (TouchLockManager.isTouchLocked) {
                                        Toast.makeText(context, context.getString(R.string.policy_toast_unlock_first), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val selected = StrategyType.entries.getOrNull(index) ?: StrategyType.ROOT_EVIOCGRAB
                                        TouchLockManager.selectStrategy(context, selected)
                                        onVibrate(25)
                                    }
                                }
                            )

                            val unlockMech = AppPreferences.unlockMechanism
                            val unlockItems = UnlockMechanism.entries.map { stringResource(it.titleRes) }
                            val currentUnlockIndex = UnlockMechanism.entries.indexOf(unlockMech).coerceAtLeast(0)

                            OverlayDropdownPreference(
                                title = stringResource(R.string.policy_unlock_dropdown_title),
                                summary = stringResource(unlockMech.descRes),
                                items = unlockItems,
                                selectedIndex = currentUnlockIndex,
                                onSelectedIndexChange = { index ->
                                    val selected = UnlockMechanism.entries.getOrNull(index) ?: UnlockMechanism.DOUBLE_VOLUME_DOWN
                                    AppPreferences.updateUnlockMechanism(selected)
                                    onVibrate(25)
                                    if (TouchLockManager.isTouchLocked) {
                                        onScreenHoldStateUpdate()
                                    }
                                }
                            )
                        }

                        // 2. 画面与屏幕保持
                        SmallTitle(
                            text = stringResource(R.string.policy_display_retention),
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = stringResource(R.string.policy_keep_screen_on_title),
                                summary = stringResource(R.string.policy_keep_screen_on_desc),
                                checked = AppPreferences.isKeepScreenOnEnabled,
                                onCheckedChange = {
                                    AppPreferences.setKeepScreenOn(it)
                                    onVibrate(25)
                                    if (TouchLockManager.isTouchLocked) {
                                        onScreenHoldStateUpdate()
                                    }
                                }
                            )

                            val screenOrientationLock = AppPreferences.screenOrientationLock
                            val orientationItems = ScreenOrientationLock.entries.map { stringResource(it.titleRes) }
                            val currentOrientationIndex = ScreenOrientationLock.entries.indexOf(screenOrientationLock).coerceAtLeast(0)

                            OverlayDropdownPreference(
                                title = stringResource(R.string.policy_orientation_title),
                                summary = stringResource(screenOrientationLock.descRes),
                                items = orientationItems,
                                selectedIndex = currentOrientationIndex,
                                onSelectedIndexChange = { index ->
                                    val selected = ScreenOrientationLock.entries.getOrNull(index) ?: ScreenOrientationLock.FOLLOW_SYSTEM
                                    AppPreferences.setScreenOrientation(selected)
                                    onVibrate(25)
                                    if (TouchLockManager.isTouchLocked) {
                                        onScreenHoldStateUpdate()
                                    }
                                }
                            )

                            SwitchPreference(
                                title = stringResource(R.string.policy_brightness_lock_title),
                                summary = stringResource(R.string.policy_brightness_lock_desc),
                                checked = AppPreferences.isBrightnessLockEnabled,
                                onCheckedChange = {
                                    AppPreferences.setBrightnessLock(it)
                                    onVibrate(25)
                                    if (TouchLockManager.isTouchLocked) {
                                        onScreenHoldStateUpdate()
                                    }
                                }
                            )

                            SwitchPreference(
                                title = stringResource(R.string.policy_capsule_title),
                                summary = stringResource(R.string.policy_capsule_desc),
                                checked = AppPreferences.isFloatingIndicatorEnabled,
                                onCheckedChange = {
                                    AppPreferences.setFloatingIndicator(it)
                                    onVibrate(25)
                                    if (TouchLockManager.isTouchLocked) {
                                        onScreenHoldStateUpdate()
                                    }
                                }
                            )
                        }

                        // 3. 自动化与安全解除
                        SmallTitle(
                            text = stringResource(R.string.policy_automation_safety),
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = stringResource(R.string.policy_auto_unlock_title),
                                summary = stringResource(R.string.policy_auto_unlock_desc),
                                checked = AppPreferences.isAutoUnlockOnScreenOffEnabled,
                                onCheckedChange = {
                                    AppPreferences.setAutoUnlockOnScreenOff(it)
                                    onVibrate(25)
                                }
                            )

                            SwitchPreference(
                                title = stringResource(R.string.policy_haptic_title),
                                summary = stringResource(R.string.policy_haptic_desc),
                                checked = AppPreferences.isHapticFeedbackEnabled,
                                onCheckedChange = {
                                    AppPreferences.setHapticFeedback(it)
                                    onVibrate(30)
                                }
                            )

                            SwitchPreference(
                                title = stringResource(R.string.policy_window_title),
                                summary = stringResource(R.string.policy_window_desc),
                                checked = true,
                                onCheckedChange = {}
                            )
                        }

                        Spacer(modifier = Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}
