package com.ccwait.touchguard.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.DarkModeOption
import com.ccwait.touchguard.R
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
 * 设置页面 Miuix 纯净实现 (SettingsMiuix)
 * 1:1 对齐 SukiSU-Ultra (SettingsMiuix.kt) 规范设计，零 Material 依赖
 */
@Composable
fun SettingsMiuix(
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
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val iconTint = colorScheme.onBackground

    var checkUpdate by rememberSaveable { mutableStateOf(true) }
    val uiModeIndex = if (themeMode == AppThemeMode.Miuix) 0 else 1

    val styleItems = remember { listOf("Miuix", "Material 3") }
    val languageOptions = remember { com.ccwait.touchguard.model.AppLanguage.entries }
    val languageItems = remember { languageOptions.map { it.displayName } }
    val darkModeOptions = remember { DarkModeOption.entries }
    val darkModeItems = darkModeOptions.map { stringResource(it.labelRes) }
    val paletteOptions = remember { ThemePalette.entries }
    val paletteItems = paletteOptions.map { stringResource(it.labelRes) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings_title),
                    scrollBehavior = scrollBehavior
                )
            }
        },
    ) { innerPadding ->
        val lazyModifier = Modifier
            .fillMaxHeight()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp)

        val boxModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

        Box(modifier = boxModifier) {
            LazyColumn(
                modifier = lazyModifier,
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    // Card 1: 更新检查
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
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
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_style_title),
                            summary = if (uiModeIndex == 0) stringResource(R.string.settings_style_miuix_summary) else stringResource(R.string.settings_style_m3_summary),
                            items = styleItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.DisplaySettings,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_style_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = uiModeIndex,
                            onSelectedIndexChange = {
                                onThemeModeChange(if (it == 0) AppThemeMode.Miuix else AppThemeMode.Material3)
                                onVibrate(20)
                            }
                        )

                        val currentLanguageIndex = languageOptions.indexOf(AppPreferences.appLanguage).coerceAtLeast(0)
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_language_title),
                            summary = AppPreferences.appLanguage.displayName,
                            items = languageItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Language,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_language_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = currentLanguageIndex,
                            onSelectedIndexChange = { index ->
                                val selected = languageOptions.getOrNull(index) ?: com.ccwait.touchguard.model.AppLanguage.FOLLOW_SYSTEM
                                AppPreferences.updateAppLanguage(selected)
                                com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(selected)
                                onVibrate(20)
                            }
                        )

                        val currentDarkModeIndex = darkModeOptions.indexOf(AppPreferences.darkMode).coerceAtLeast(0)
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_dark_mode_title),
                            summary = stringResource(R.string.settings_dark_mode_summary, stringResource(AppPreferences.darkMode.labelRes)),
                            items = darkModeItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.DarkMode,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_dark_mode_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = currentDarkModeIndex,
                            onSelectedIndexChange = { index ->
                                val selected = darkModeOptions.getOrNull(index) ?: DarkModeOption.SYSTEM
                                AppPreferences.updateDarkMode(selected)
                                onVibrate(20)
                            }
                        )

                        val currentPaletteIndex = paletteOptions.indexOf(AppPreferences.themePalette).coerceAtLeast(0)
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_palette_title),
                            summary = "${stringResource(AppPreferences.themePalette.labelRes)} · ${stringResource(AppPreferences.themePalette.descRes)}",
                            items = paletteItems,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(R.string.settings_palette_title),
                                    tint = iconTint
                                )
                            },
                            selectedIndex = currentPaletteIndex,
                            onSelectedIndexChange = { index ->
                                val selected = paletteOptions.getOrNull(index) ?: ThemePalette.MONET
                                AppPreferences.updateThemePalette(selected)
                                onVibrate(20)
                            }
                        )

                        SwitchPreference(
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
                            onCheckedChange = {
                                AppPreferences.updateFloatingBottomBar(it)
                                onVibrate(25)
                            }
                        )

                        if (AppPreferences.isFloatingBottomBarEnabled) {
                            SwitchPreference(
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
                                onCheckedChange = {
                                    AppPreferences.updateFloatingBottomBarBlur(it)
                                    onVibrate(20)
                                }
                            )
                        }

                        SwitchPreference(
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
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
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
                    Card(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        ArrowPreference(
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
                            onClick = {
                                onResetLock()
                                onVibrate(30)
                            }
                        )
                        val clearToast = stringResource(R.string.settings_clear_logs_toast)
                        ArrowPreference(
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
                            onClick = {
                                AppLogManager.clear()
                                onVibrate(20)
                                Toast.makeText(context, clearToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                        ArrowPreference(
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
