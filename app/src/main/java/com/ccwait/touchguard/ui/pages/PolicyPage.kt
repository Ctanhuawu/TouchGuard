package com.ccwait.touchguard.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.model.ScreenOrientationLock
import com.ccwait.touchguard.model.UnlockMechanism
import com.ccwait.touchguard.strategy.TouchLockManager
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 页面 2: 锁定策略 (PolicyPage)
 * 1:1 对齐 SukiSU-Ultra 规范：所有锁定控制与屏幕保持统一由 Miuix 架构承载
 */
import android.widget.Toast
import com.ccwait.touchguard.strategy.StrategyReadiness
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.LocalAppThemeMode
import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop

import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

import com.ccwait.touchguard.ui.adaptive.AdaptiveCard
import com.ccwait.touchguard.ui.adaptive.AdaptiveDropdownPreference
import com.ccwait.touchguard.ui.adaptive.AdaptiveScaffold
import com.ccwait.touchguard.ui.adaptive.AdaptiveSectionTitle
import com.ccwait.touchguard.ui.adaptive.AdaptiveSwitchPreference
import com.ccwait.touchguard.ui.adaptive.AdaptiveTopAppBar

@Composable
fun PolicyPage(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit,
    onScreenHoldStateUpdate: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)
    val blurActive = backdrop != null
    val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix
    val barColor = if (blurActive) Color.Transparent else if (isMiuix) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
    val dividerColor = if (isMiuix) MiuixTheme.colorScheme.dividerLine else MaterialTheme.colorScheme.outlineVariant
    val summaryTextColor = if (isMiuix) MiuixTheme.colorScheme.onSurfaceVariantSummary else MaterialTheme.colorScheme.onSurfaceVariant

    AdaptiveScaffold(
        topBar = {
            if (isMiuix) {
                BlurredBar(backdrop) {
                    AdaptiveTopAppBar(
                        barColor = barColor,
                        title = stringResource(R.string.policy_title),
                        scrollBehavior = scrollBehavior
                    )
                }
            } else {
                AdaptiveTopAppBar(
                    barColor = barColor,
                    title = stringResource(R.string.policy_title)
                )
            }
        },
    ) { innerPadding ->
        val lazyModifier = Modifier
            .fillMaxSize()
            .then(if (isMiuix) Modifier.scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
            .padding(horizontal = 12.dp)

        val boxModifier = if (isMiuix && backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

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
                AdaptiveSectionTitle(
                    text = stringResource(R.string.policy_core_controls),
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                )
                AdaptiveCard(modifier = Modifier.fillMaxWidth()) {
                    val currentStrategy = TouchLockManager.currentStrategy
                    val readiness = currentStrategy.readiness

                    val strategySummary = if (readiness == StrategyReadiness.READY) {
                        stringResource(currentStrategy.type.descRes)
                    } else {
                        currentStrategy.statusSummary
                    }

                    val strategyItems = StrategyType.entries.map { stringResource(it.titleRes) }
                    val currentStrategyIndex = StrategyType.entries.indexOf(TouchLockManager.currentStrategyType).coerceAtLeast(0)

                    AdaptiveDropdownPreference(
                        title = stringResource(R.string.policy_strategy_dropdown_title),
                        summary = strategySummary,
                        items = strategyItems,
                        showDivider = true,
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

                    AdaptiveDropdownPreference(
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
                AdaptiveSectionTitle(
                    text = stringResource(R.string.policy_display_retention),
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                )
                AdaptiveCard(modifier = Modifier.fillMaxWidth()) {
                    AdaptiveSwitchPreference(
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

                    AdaptiveDropdownPreference(
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

                    AdaptiveSwitchPreference(
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

                    AdaptiveSwitchPreference(
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
                AdaptiveSectionTitle(
                    text = stringResource(R.string.policy_automation_safety),
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                )
                AdaptiveCard(modifier = Modifier.fillMaxWidth()) {
                    AdaptiveSwitchPreference(
                        title = stringResource(R.string.policy_auto_unlock_title),
                        summary = stringResource(R.string.policy_auto_unlock_desc),
                        checked = AppPreferences.isAutoUnlockOnScreenOffEnabled,
                        onCheckedChange = {
                            AppPreferences.setAutoUnlockOnScreenOff(it)
                            onVibrate(25)
                        }
                    )

                    AdaptiveSwitchPreference(
                        title = stringResource(R.string.policy_haptic_title),
                        summary = stringResource(R.string.policy_haptic_desc),
                        checked = AppPreferences.isHapticFeedbackEnabled,
                        onCheckedChange = {
                            AppPreferences.setHapticFeedback(it)
                            onVibrate(30)
                        }
                    )

                    AdaptiveSwitchPreference(
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



