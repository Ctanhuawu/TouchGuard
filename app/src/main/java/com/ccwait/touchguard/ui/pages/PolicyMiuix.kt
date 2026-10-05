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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
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
                                title = stringResource(R.string.policy_hide_system_bars_title),
                                summary = stringResource(R.string.policy_hide_system_bars_desc),
                                checked = AppPreferences.isHideSystemBarsEnabled,
                                onCheckedChange = {
                                    AppPreferences.setHideSystemBars(it)
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

                            KeyPressWindowPreferenceMiuix(
                                windowMs = AppPreferences.keyPressWindowMs,
                                onWindowChange = { AppPreferences.updateKeyPressWindowMs(it) },
                                onVibrate = onVibrate
                            )
                        }

                        Spacer(modifier = Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyPressWindowPreferenceMiuix(
    windowMs: Int,
    onWindowChange: (Int) -> Unit,
    onVibrate: (Long) -> Unit
) {
    var textValue by remember(windowMs) { mutableStateOf(windowMs.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.policy_window_title),
                        fontSize = MiuixTheme.textStyles.headline1.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val isDefault = windowMs == AppPreferences.DEFAULT_KEY_PRESS_WINDOW_MS
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isDefault) Color.Transparent else colorScheme.primary.copy(alpha = 0.12f))
                            .clickable {
                                onWindowChange(AppPreferences.DEFAULT_KEY_PRESS_WINDOW_MS)
                                textValue = AppPreferences.DEFAULT_KEY_PRESS_WINDOW_MS.toString()
                                onVibrate(25)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.policy_window_reset),
                            tint = if (isDefault) colorScheme.onSurfaceVariantSummary.copy(alpha = 0.35f) else colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.policy_window_desc),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 数字输入框 (加 "ms" 后缀)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        if (newText.length <= 4 && newText.all { it.isDigit() }) {
                            textValue = newText
                            val num = newText.toIntOrNull()
                            if (num != null && num in 300..3000) {
                                onWindowChange(num)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val num = textValue.toIntOrNull()?.coerceIn(300, 3000) ?: 1000
                            textValue = num.toString()
                            onWindowChange(num)
                        }
                    ),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .widthIn(min = 16.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                val num = textValue.toIntOrNull()?.coerceIn(300, 3000) ?: 1000
                                textValue = num.toString()
                                onWindowChange(num)
                            }
                        }
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariantSummary
                )
            }
        }

        // 拖动条 (Slider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "300ms",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary
            )
            Slider(
                value = windowMs.toFloat(),
                onValueChange = { floatVal ->
                    val intVal = ((floatVal.roundToInt() + 25) / 50) * 50
                    if (intVal != windowMs) {
                        onWindowChange(intVal)
                        onVibrate(15)
                    }
                },
                valueRange = 300f..3000f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "3000ms",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
