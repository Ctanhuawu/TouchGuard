package com.ccwait.touchguard.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.model.ScreenOrientationLock
import com.ccwait.touchguard.model.UnlockMechanism
import com.ccwait.touchguard.strategy.StrategyReadiness
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.ui.components.material.MaterialDropdownPreference
import com.ccwait.touchguard.ui.components.material.MaterialPreferenceCard
import com.ccwait.touchguard.ui.components.material.MaterialSectionTitle
import com.ccwait.touchguard.ui.components.material.MaterialSwitchPreference

/**
 * 页面 2: 锁定策略 Material 3 纯净实现 (PolicyMaterial)
 * 1:1 对齐 Material You 规范，纯 M3 组件渲染，零 Miuix 依赖
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyMaterial(
    bottomInnerPadding: Dp,
    onLockToggle: () -> Unit,
    onScreenHoldStateUpdate: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.policy_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. 核心控制分组
                    MaterialSectionTitle(text = stringResource(R.string.policy_core_controls))
                    MaterialPreferenceCard {
                        val currentStrategy = TouchLockManager.currentStrategy
                        val readiness = currentStrategy.readiness

                        val strategySummary = if (readiness == StrategyReadiness.READY) {
                            stringResource(currentStrategy.type.descRes)
                        } else {
                            currentStrategy.statusSummary
                        }

                        val strategyItems = StrategyType.entries.map { stringResource(it.titleRes) }
                        val currentStrategyIndex = StrategyType.entries.indexOf(TouchLockManager.currentStrategyType).coerceAtLeast(0)

                        MaterialDropdownPreference(
                            title = stringResource(R.string.policy_strategy_dropdown_title),
                            summary = strategySummary,
                            items = strategyItems,
                            selectedIndex = currentStrategyIndex,
                            showDivider = true,
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

                        MaterialDropdownPreference(
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
                    MaterialSectionTitle(text = stringResource(R.string.policy_display_retention))
                    MaterialPreferenceCard {
                        MaterialSwitchPreference(
                            title = stringResource(R.string.policy_keep_screen_on_title),
                            summary = stringResource(R.string.policy_keep_screen_on_desc),
                            checked = AppPreferences.isKeepScreenOnEnabled,
                            showDivider = true,
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

                        MaterialDropdownPreference(
                            title = stringResource(R.string.policy_orientation_title),
                            summary = stringResource(screenOrientationLock.descRes),
                            items = orientationItems,
                            selectedIndex = currentOrientationIndex,
                            showDivider = true,
                            onSelectedIndexChange = { index ->
                                val selected = ScreenOrientationLock.entries.getOrNull(index) ?: ScreenOrientationLock.FOLLOW_SYSTEM
                                AppPreferences.setScreenOrientation(selected)
                                onVibrate(25)
                                if (TouchLockManager.isTouchLocked) {
                                    onScreenHoldStateUpdate()
                                }
                            }
                        )

                        MaterialSwitchPreference(
                            title = stringResource(R.string.policy_brightness_lock_title),
                            summary = stringResource(R.string.policy_brightness_lock_desc),
                            checked = AppPreferences.isBrightnessLockEnabled,
                            showDivider = true,
                            onCheckedChange = {
                                AppPreferences.setBrightnessLock(it)
                                onVibrate(25)
                                if (TouchLockManager.isTouchLocked) {
                                    onScreenHoldStateUpdate()
                                }
                            }
                        )

                        MaterialSwitchPreference(
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
                    MaterialSectionTitle(text = stringResource(R.string.policy_automation_safety))
                    MaterialPreferenceCard {
                        MaterialSwitchPreference(
                            title = stringResource(R.string.policy_auto_unlock_title),
                            summary = stringResource(R.string.policy_auto_unlock_desc),
                            checked = AppPreferences.isAutoUnlockOnScreenOffEnabled,
                            showDivider = true,
                            onCheckedChange = {
                                AppPreferences.setAutoUnlockOnScreenOff(it)
                                onVibrate(25)
                            }
                        )

                        MaterialSwitchPreference(
                            title = stringResource(R.string.policy_haptic_title),
                            summary = stringResource(R.string.policy_haptic_desc),
                            checked = AppPreferences.isHapticFeedbackEnabled,
                            showDivider = true,
                            onCheckedChange = {
                                AppPreferences.setHapticFeedback(it)
                                onVibrate(30)
                            }
                        )

                        MaterialSwitchPreference(
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
