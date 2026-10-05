package com.ccwait.touchguard.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.ColorMode
import com.ccwait.touchguard.DarkModeOption
import com.ccwait.touchguard.ThemePalette
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页面 (SettingsPage)
 * 1:1 对齐 SukiSU-Ultra (SettingsMiuix.kt) 设计：
 * Card 1: 更新检查
 * Card 2: 界面与个性化 (Miuix/Material, 语言, 主题调色板, 悬浮底栏开关, 实时模糊, 角标)
 * Card 3: 运行与后台保活
 * Card 4: 维护与关于 (重载触控, 清空日志, 关于)
 */
import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

import com.ccwait.touchguard.ui.LocalAppThemeMode
import com.ccwait.touchguard.ui.adaptive.AdaptiveArrowPreference
import com.ccwait.touchguard.ui.adaptive.AdaptiveCard
import com.ccwait.touchguard.ui.adaptive.AdaptiveDropdownPreference
import com.ccwait.touchguard.ui.adaptive.AdaptiveScaffold
import com.ccwait.touchguard.ui.adaptive.AdaptiveSwitchPreference
import com.ccwait.touchguard.ui.adaptive.AdaptiveTopAppBar

@Composable
fun SettingsPage(
    bottomInnerPadding: Dp,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onResetLock: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(true)
    val blurActive = backdrop != null
    val isMiuix = LocalAppThemeMode.current == AppThemeMode.Miuix
    val barColor = if (blurActive) Color.Transparent else if (isMiuix) colorScheme.surface else androidx.compose.material3.MaterialTheme.colorScheme.surface
    val iconTint = if (isMiuix) colorScheme.onBackground else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    // 局部设置状态
    var checkUpdate by rememberSaveable { mutableStateOf(true) }
    val uiModeIndex = if (AppPreferences.themeMode == AppThemeMode.Miuix) 0 else 1

    AdaptiveScaffold(
        topBar = {
            if (isMiuix) {
                BlurredBar(backdrop) {
                    AdaptiveTopAppBar(
                        barColor = barColor,
                        title = stringResource(R.string.settings_title),
                        scrollBehavior = scrollBehavior
                    )
                }
            } else {
                AdaptiveTopAppBar(
                    barColor = barColor,
                    title = stringResource(R.string.settings_title)
                )
            }
        },
    ) { innerPadding ->
        val lazyModifier = Modifier
            .fillMaxHeight()
            .then(if (isMiuix) Modifier.scrollEndHaptic().overScrollVertical().nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
            .padding(horizontal = 12.dp)

        val boxModifier = if (isMiuix && backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

        Box(modifier = boxModifier) {
            LazyColumn(
                modifier = lazyModifier,
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    // Card 1: 更新检查
                    AdaptiveCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        AdaptiveSwitchPreference(
                            title = stringResource(R.string.settings_check_update_title),
                            summary = stringResource(R.string.settings_check_update_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.SystemUpdate,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_check_update_title),
                                    tint = iconTint
                                )
                            },
                            checked = checkUpdate,
                            onCheckedChange = {
                                checkUpdate = it
                                onVibrate(20)
                            }
                        )
                    }

                    // Card 2: 界面与个性化
                    AdaptiveCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        AdaptiveDropdownPreference(
                            title = stringResource(R.string.settings_style_title),
                            summary = if (uiModeIndex == 0) stringResource(R.string.settings_style_miuix_summary) else stringResource(R.string.settings_style_m3_summary),
                            items = listOf("Miuix", "Material 3"),
                            startAction = {
                                Icon(
                                    Icons.Rounded.DisplaySettings,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_style_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = uiModeIndex,
                            showDivider = true,
                            onSelectedIndexChange = {
                                onThemeModeChange(if (it == 0) AppThemeMode.Miuix else AppThemeMode.Material3)
                                onVibrate(20)
                            }
                        )
                        val languageOptions = com.ccwait.touchguard.model.AppLanguage.entries
                        val currentLanguageIndex = languageOptions.indexOf(AppPreferences.appLanguage).coerceAtLeast(0)
                        AdaptiveDropdownPreference(
                            title = stringResource(R.string.settings_language_title),
                            summary = AppPreferences.appLanguage.displayName,
                            items = languageOptions.map { it.displayName },
                            startAction = {
                                Icon(
                                    Icons.Rounded.Language,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_language_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = currentLanguageIndex,
                            showDivider = true,
                            onSelectedIndexChange = { index ->
                                val selected = languageOptions.getOrNull(index) ?: com.ccwait.touchguard.model.AppLanguage.FOLLOW_SYSTEM
                                AppPreferences.updateAppLanguage(selected)
                                com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(selected)
                                onVibrate(20)
                            }
                        )
                        val darkModeOptions = DarkModeOption.entries
                        val currentDarkModeIndex = darkModeOptions.indexOf(AppPreferences.darkMode).coerceAtLeast(0)
                        AdaptiveDropdownPreference(
                            title = stringResource(R.string.settings_dark_mode_title),
                            summary = stringResource(R.string.settings_dark_mode_summary, stringResource(AppPreferences.darkMode.labelRes)),
                            items = darkModeOptions.map { stringResource(it.labelRes) },
                            startAction = {
                                Icon(
                                    Icons.Rounded.DarkMode,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_dark_mode_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = currentDarkModeIndex,
                            showDivider = true,
                            onSelectedIndexChange = { index ->
                                val selected = darkModeOptions.getOrNull(index) ?: DarkModeOption.SYSTEM
                                AppPreferences.updateDarkMode(selected)
                                onVibrate(20)
                            }
                        )
                        val paletteOptions = ThemePalette.entries
                        val currentPaletteIndex = paletteOptions.indexOf(AppPreferences.themePalette).coerceAtLeast(0)
                        AdaptiveDropdownPreference(
                            title = stringResource(R.string.settings_palette_title),
                            summary = "${stringResource(AppPreferences.themePalette.labelRes)} · ${stringResource(AppPreferences.themePalette.descRes)}",
                            items = paletteOptions.map { stringResource(it.labelRes) },
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_palette_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = currentPaletteIndex,
                            showDivider = true,
                            onSelectedIndexChange = { index ->
                                val selected = paletteOptions.getOrNull(index) ?: ThemePalette.MONET
                                AppPreferences.updateThemePalette(selected)
                                onVibrate(20)
                            }
                        )
                        AdaptiveSwitchPreference(
                            title = stringResource(R.string.settings_floating_bar_title),
                            summary = stringResource(R.string.settings_floating_bar_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.CallToAction,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_floating_bar_title),
                                    tint = iconTint
                                )
                            },
                            checked = AppPreferences.isFloatingBottomBarEnabled,
                            showDivider = AppPreferences.isFloatingBottomBarEnabled,
                            onCheckedChange = {
                                AppPreferences.updateFloatingBottomBar(it)
                                onVibrate(25)
                            }
                        )
                        if (AppPreferences.isFloatingBottomBarEnabled) {
                            AdaptiveSwitchPreference(
                                title = stringResource(R.string.settings_blur_title),
                                summary = stringResource(R.string.settings_blur_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.WaterDrop,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_blur_title),
                                        tint = iconTint
                                    )
                                },
                                checked = AppPreferences.isFloatingBottomBarBlurEnabled,
                                showDivider = true,
                                onCheckedChange = {
                                    AppPreferences.updateFloatingBottomBarBlur(it)
                                    onVibrate(20)
                                }
                            )
                        }
                        AdaptiveSwitchPreference(
                            title = stringResource(R.string.settings_badge_title),
                            summary = stringResource(R.string.settings_badge_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Pin,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_badge_title),
                                    tint = iconTint
                                )
                            },
                            checked = AppPreferences.isNavigationBadgeEnabled,
                            onCheckedChange = {
                                AppPreferences.updateNavigationBadge(it)
                                onVibrate(20)
                            }
                        )
                    }

                    // Card 3: 运行与后台保活
                    AdaptiveCard(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        AdaptiveSwitchPreference(
                            title = stringResource(R.string.settings_fgs_title),
                            summary = stringResource(R.string.settings_fgs_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Description,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_fgs_title),
                                    tint = iconTint
                                )
                            },
                            checked = AppPreferences.isKeepAliveEnabled,
                            onCheckedChange = {
                                AppPreferences.setKeepAlive(it)
                                if (it) {
                                    com.ccwait.touchguard.service.TouchGuardForegroundService.start(context)
                                } else {
                                    com.ccwait.touchguard.service.TouchGuardForegroundService.stop(context)
                                    com.ccwait.touchguard.notification.TouchGuardNotificationManager.updateNotification(context)
                                }
                                onVibrate(20)
                            }
                        )
                    }

                    // Card 4: 维护与关于
                    AdaptiveCard(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        AdaptiveArrowPreference(
                            title = stringResource(R.string.settings_reset_lock_title),
                            summary = stringResource(R.string.settings_reset_lock_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.RestartAlt,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_reset_lock_title),
                                    tint = iconTint
                                )
                            },
                            showDivider = true,
                            onClick = {
                                onResetLock()
                                onVibrate(30)
                            }
                        )
                        val clearToast = stringResource(R.string.settings_clear_logs_toast)
                        AdaptiveArrowPreference(
                            title = stringResource(R.string.settings_clear_logs_title),
                            summary = stringResource(R.string.settings_clear_logs_summary),
                            startAction = {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_clear_logs_title),
                                    tint = iconTint
                                )
                            },
                            showDivider = true,
                            onClick = {
                                AppLogManager.clear()
                                onVibrate(20)
                                Toast.makeText(context, clearToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                        AdaptiveArrowPreference(
                            title = stringResource(R.string.settings_about_app_title),
                            summary = stringResource(R.string.settings_about_app_summary, BuildConfig.VERSION_NAME, "Ctanhuawu"),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Info,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_about_app_title),
                                    tint = iconTint
                                )
                            },
                            onClick = {
                                onVibrate(20)
                                Toast.makeText(context, "TouchGuard v${BuildConfig.VERSION_NAME} · 开发者: Ctanhuawu", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

