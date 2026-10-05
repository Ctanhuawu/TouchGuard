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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.R
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.components.material.MaterialDropdownPreference
import com.ccwait.touchguard.ui.components.material.MaterialPreferenceCard
import com.ccwait.touchguard.ui.components.material.MaterialPreferenceItem
import com.ccwait.touchguard.ui.components.material.MaterialSectionTitle
import com.ccwait.touchguard.ui.components.material.MaterialSwitchPreference

/**
 * 设置页面 Material 3 纯净实现 (SettingsMaterial)
 * 1:1 对齐 Material You 规范设计，零 Miuix 依赖
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMaterial(
    bottomInnerPadding: Dp,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onResetLock: () -> Unit,
    onVibrate: (Long) -> Unit
) {
    val context = LocalContext.current
    var checkUpdate by rememberSaveable { mutableStateOf(true) }
    val uiModeIndex = if (themeMode == AppThemeMode.Miuix) 0 else 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
                    // Card 1: 更新检查
                    MaterialSectionTitle(text = stringResource(R.string.settings_check_update_title))
                    MaterialPreferenceCard {
                        MaterialSwitchPreference(
                            title = stringResource(R.string.settings_check_update_title),
                            summary = stringResource(R.string.settings_check_update_summary),
                            leadingIcon = Icons.Rounded.SystemUpdate,
                            checked = checkUpdate,
                            onCheckedChange = {
                                checkUpdate = it
                                onVibrate(20)
                            }
                        )
                    }

                    // Card 2: 界面与个性化
                    MaterialSectionTitle(text = stringResource(R.string.settings_style_title))
                    MaterialPreferenceCard {
                        MaterialDropdownPreference(
                            title = stringResource(R.string.settings_style_title),
                            summary = if (uiModeIndex == 0) stringResource(R.string.settings_style_miuix_summary) else stringResource(R.string.settings_style_m3_summary),
                            leadingIcon = Icons.Rounded.DisplaySettings,
                            items = listOf("Miuix", "Material 3"),
                            selectedIndex = uiModeIndex,
                            showDivider = true,
                            onSelectedIndexChange = {
                                onThemeModeChange(if (it == 0) AppThemeMode.Miuix else AppThemeMode.Material3)
                                onVibrate(20)
                            }
                        )

                        val languageOptions = com.ccwait.touchguard.model.AppLanguage.entries
                        val currentLanguageIndex = languageOptions.indexOf(AppPreferences.appLanguage).coerceAtLeast(0)
                        MaterialDropdownPreference(
                            title = stringResource(R.string.settings_language_title),
                            summary = AppPreferences.appLanguage.displayName,
                            leadingIcon = Icons.Rounded.Language,
                            items = languageOptions.map { it.displayName },
                            selectedIndex = currentLanguageIndex,
                            showDivider = true,
                            onSelectedIndexChange = { index ->
                                val selected = languageOptions.getOrNull(index) ?: com.ccwait.touchguard.model.AppLanguage.FOLLOW_SYSTEM
                                AppPreferences.updateAppLanguage(selected)
                                com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(selected)
                                onVibrate(20)
                            }
                        )

                        val navigator = com.ccwait.touchguard.ui.navigation.LocalNavigator.current
                        MaterialPreferenceItem(
                            title = stringResource(R.string.settings_theme),
                            summary = stringResource(R.string.settings_theme_summary),
                            leadingIcon = Icons.Rounded.Palette,
                            showDivider = true,
                            onClick = {
                                navigator.push(com.ccwait.touchguard.ui.navigation.Route.ColorPalette)
                                onVibrate(20)
                            }
                        )

                        MaterialSwitchPreference(
                            title = stringResource(R.string.settings_floating_bar_title),
                            summary = stringResource(R.string.settings_floating_bar_summary),
                            leadingIcon = Icons.Rounded.CallToAction,
                            checked = AppPreferences.isFloatingBottomBarEnabled,
                            showDivider = AppPreferences.isFloatingBottomBarEnabled,
                            onCheckedChange = {
                                AppPreferences.updateFloatingBottomBar(it)
                                onVibrate(25)
                            }
                        )

                        if (AppPreferences.isFloatingBottomBarEnabled) {
                            MaterialSwitchPreference(
                                title = stringResource(R.string.settings_blur_title),
                                summary = stringResource(R.string.settings_blur_summary),
                                leadingIcon = Icons.Rounded.WaterDrop,
                                checked = AppPreferences.isFloatingBottomBarBlurEnabled,
                                showDivider = true,
                                onCheckedChange = {
                                    AppPreferences.updateFloatingBottomBarBlur(it)
                                    onVibrate(20)
                                }
                            )
                        }

                        MaterialSwitchPreference(
                            title = stringResource(R.string.settings_badge_title),
                            summary = stringResource(R.string.settings_badge_summary),
                            leadingIcon = Icons.Rounded.Pin,
                            checked = AppPreferences.isNavigationBadgeEnabled,
                            onCheckedChange = {
                                AppPreferences.updateNavigationBadge(it)
                                onVibrate(20)
                            }
                        )
                    }

                    // Card 3: 运行与后台保活
                    MaterialSectionTitle(text = stringResource(R.string.settings_fgs_title))
                    MaterialPreferenceCard {
                        MaterialSwitchPreference(
                            title = stringResource(R.string.settings_fgs_title),
                            summary = stringResource(R.string.settings_fgs_summary),
                            leadingIcon = Icons.Rounded.Description,
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
                    MaterialSectionTitle(text = stringResource(R.string.settings_developer_title))
                    MaterialPreferenceCard {
                        MaterialPreferenceItem(
                            title = stringResource(R.string.settings_reset_lock_title),
                            summary = stringResource(R.string.settings_reset_lock_summary),
                            leadingIcon = Icons.Rounded.RestartAlt,
                            showDivider = true,
                            onClick = {
                                onResetLock()
                                onVibrate(30)
                            }
                        )

                        val clearToast = stringResource(R.string.settings_clear_logs_toast)
                        MaterialPreferenceItem(
                            title = stringResource(R.string.settings_clear_logs_title),
                            summary = stringResource(R.string.settings_clear_logs_summary),
                            leadingIcon = Icons.Outlined.DeleteOutline,
                            showDivider = true,
                            onClick = {
                                AppLogManager.clear()
                                onVibrate(20)
                                Toast.makeText(context, clearToast, Toast.LENGTH_SHORT).show()
                            }
                        )

                        MaterialPreferenceItem(
                            title = stringResource(R.string.settings_about_app_title),
                            summary = stringResource(R.string.settings_about_app_summary, BuildConfig.VERSION_NAME, "Ctanhuawu"),
                            leadingIcon = Icons.Rounded.Info,
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
