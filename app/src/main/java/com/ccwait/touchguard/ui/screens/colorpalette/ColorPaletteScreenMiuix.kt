package com.ccwait.touchguard.ui.screens.colorpalette

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.ui.theme.ColorMode
import com.ccwait.touchguard.ui.theme.keyColorOptions
import com.ccwait.touchguard.ui.util.BlurredBar
import com.ccwait.touchguard.ui.util.rememberBlurBackdrop
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 主题与调色板屏幕 Miuix 纯净实现 (1:1 对齐 SukiSU-Ultra ColorPaletteScreenMiuix.kt)
 */
@Composable
fun ColorPaletteScreenMiuix(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(AppPreferences.isFloatingBottomBarBlurEnabled)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    val currentColorMode = AppPreferences.colorMode
    val isDark = currentColorMode.isDark || (currentColorMode.isSystem && isSystemInDarkTheme())
    val keyColor = AppPreferences.keyColor
    val miuixMonet = AppPreferences.miuixMonet
    val currentStyle = try {
        PaletteStyle.valueOf(AppPreferences.colorStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val currentSpec = try {
        ColorSpec.SpecVersion.valueOf(AppPreferences.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.SPEC_2025
    }

    val themeTabItems = listOf(
        stringResource(R.string.settings_theme_mode_system),
        stringResource(R.string.settings_theme_mode_light),
        stringResource(R.string.settings_theme_mode_dark),
    )

    val currentTabIndex = when {
        currentColorMode == ColorMode.LIGHT || currentColorMode == ColorMode.MONET_LIGHT -> 1
        currentColorMode.isDark -> 2
        else -> 0
    }

    val colorItems = listOf(
        stringResource(R.string.settings_key_color_default),
        stringResource(R.string.color_red),
        stringResource(R.string.color_pink),
        stringResource(R.string.color_purple),
        stringResource(R.string.color_deep_purple),
        stringResource(R.string.color_indigo),
        stringResource(R.string.color_blue),
        stringResource(R.string.color_cyan),
        stringResource(R.string.color_teal),
        stringResource(R.string.color_green),
        stringResource(R.string.color_yellow),
        stringResource(R.string.color_amber),
        stringResource(R.string.color_orange),
        stringResource(R.string.color_brown),
        stringResource(R.string.color_blue_grey),
        stringResource(R.string.color_sakura),
    )
    val colorValues = remember { listOf(0) + keyColorOptions }

    val styleItems = remember { PaletteStyle.entries.map { it.name } }
    val specItems = remember { listOf(ColorSpec.SpecVersion.SPEC_2025.name, ColorSpec.SpecVersion.SPEC_2021.name) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings_theme),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { innerPadding ->
        val boxModifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

        Box(modifier = boxModifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. 核心主题实时预览卡片 (1:1 对齐 SukiSU-Ultra ThemePreviewCardMiuix)
                    ThemePreviewCardMiuix(
                        keyColor = keyColor,
                        isDark = isDark,
                        miuixMonet = miuixMonet,
                        enableFloatingBottomBar = AppPreferences.isFloatingBottomBarEnabled,
                        enableFloatingBottomBarBlur = AppPreferences.isFloatingBottomBarBlurEnabled,
                        paletteStyle = currentStyle,
                        colorSpec = currentSpec
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. 主题色彩模式分段切换 (跟随系统 / 浅色 / 深色)
                    TabRow(
                        tabs = themeTabItems,
                        selectedTabIndex = currentTabIndex,
                        onTabSelected = { index ->
                            val newMode = when (index) {
                                1 -> if (miuixMonet) ColorMode.MONET_LIGHT else ColorMode.LIGHT
                                2 -> if (miuixMonet) ColorMode.MONET_DARK else ColorMode.DARK
                                else -> if (miuixMonet) ColorMode.MONET_SYSTEM else ColorMode.SYSTEM
                            }
                            AppPreferences.updateColorMode(newMode)
                        }
                    )

                    // 3. 动态色彩与自定义种子强调色卡片
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_monet),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = miuixMonet,
                            onCheckedChange = { enabled ->
                                AppPreferences.updateMiuixMonet(enabled)
                                val updatedMode = if (enabled) currentColorMode.toMonetMode() else currentColorMode.toNonMonetMode()
                                AppPreferences.updateColorMode(ColorMode.fromValue(updatedMode))
                            }
                        )

                        AnimatedVisibility(visible = miuixMonet) {
                            Column {
                                OverlayDropdownPreference(
                                    title = stringResource(R.string.settings_key_color),
                                    items = colorItems,
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.Colorize,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = null,
                                            tint = colorScheme.onBackground
                                        )
                                    },
                                    selectedIndex = colorValues.indexOf(keyColor).takeIf { it >= 0 } ?: 0,
                                    onSelectedIndexChange = { index ->
                                        AppPreferences.updateKeyColor(colorValues[index])
                                    }
                                )

                                AnimatedVisibility(visible = keyColor != 0) {
                                    Column {
                                        OverlayDropdownPreference(
                                            title = stringResource(R.string.settings_color_style),
                                            items = styleItems,
                                            startAction = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Style,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    contentDescription = null,
                                                    tint = colorScheme.onBackground
                                                )
                                            },
                                            selectedIndex = styleItems.indexOf(currentStyle.name).coerceAtLeast(0),
                                            onSelectedIndexChange = { index ->
                                                AppPreferences.updateColorStyle(styleItems[index])
                                            }
                                        )

                                        OverlayDropdownPreference(
                                            title = stringResource(R.string.settings_color_spec),
                                            items = specItems,
                                            selectedIndex = specItems.indexOf(currentSpec.name).coerceAtLeast(0),
                                            onSelectedIndexChange = { index ->
                                                AppPreferences.updateColorSpec(specItems[index])
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. 界面与交互外观增强
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 24.dp)
                            .fillMaxWidth()
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_enable_blur),
                            summary = stringResource(R.string.settings_enable_blur_summary),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.WaterDrop,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = AppPreferences.isFloatingBottomBarBlurEnabled,
                            onCheckedChange = {
                                AppPreferences.updateFloatingBottomBarBlur(it)
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.settings_floating_bar_title),
                            summary = stringResource(R.string.settings_floating_bar_summary),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.CallToAction,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = AppPreferences.isFloatingBottomBarEnabled,
                            onCheckedChange = {
                                AppPreferences.updateFloatingBottomBar(it)
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.settings_badge_title),
                            summary = stringResource(R.string.settings_badge_summary),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Pin,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = AppPreferences.isNavigationBadgeEnabled,
                            onCheckedChange = {
                                AppPreferences.updateNavigationBadge(it)
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.settings_enable_predictive_back),
                            summary = stringResource(R.string.settings_enable_predictive_back_summary),
                            startAction = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = null,
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = AppPreferences.isPredictiveBackEnabled,
                            onCheckedChange = {
                                AppPreferences.updatePredictiveBack(it, context)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * SukiSU-Ultra 1:1 动态实时主题预览卡片
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    enableFloatingBottomBar: Boolean = false,
    enableFloatingBottomBarBlur: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else colorSpec
    val dynamicCs = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = effectiveStyle,
        specVersion = effectiveSpec,
    )

    val bgColor = if (miuixMonet) dynamicCs.background else colorScheme.surface
    val textColor = if (miuixMonet) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (miuixMonet) dynamicCs.primary else colorScheme.primary
    val navSelectedColor = colorScheme.onSurfaceContainer
    val navUnselectedColor = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .aspectRatio(screenRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 11.sp,
                        color = textColor
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentCardColor)
                )

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                    }
                }

                if (enableFloatingBottomBar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.6f)
                                    else navBarColor
                                )
                                .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(13.dp))
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (it == 0) iconColor else textColor)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(textColor.copy(alpha = 0.1f))
                        )
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .fillMaxWidth()
                                .background(navBarColor)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (it == 0) navSelectedColor else navUnselectedColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
