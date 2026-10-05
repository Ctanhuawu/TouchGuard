package com.ccwait.touchguard.ui.screens.colorpalette

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.ui.components.material.MaterialDropdownPreference
import com.ccwait.touchguard.ui.components.material.MaterialPreferenceCard
import com.ccwait.touchguard.ui.components.material.MaterialSectionTitle
import com.ccwait.touchguard.ui.components.material.MaterialSwitchPreference
import com.ccwait.touchguard.ui.theme.ColorMode
import com.ccwait.touchguard.ui.theme.keyColorOptions
import com.ccwait.touchguard.ui.theme.rememberKernelSUColorScheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * 主题与调色板屏幕 Material 3 纯净实现 (1:1 对齐 SukiSU-Ultra ColorPaletteScreenMaterial.kt)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteScreenMaterial(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentColorMode = AppPreferences.colorMode
    val isDark = currentColorMode.isDark || (currentColorMode.isSystem && isSystemInDarkTheme())
    val isAmoled = currentColorMode.isAmoled
    val currentKeyColor = AppPreferences.keyColor
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

    val styleItems = remember { PaletteStyle.entries.map { it.name } }
    val specItems = remember { listOf(ColorSpec.SpecVersion.SPEC_2025.name, ColorSpec.SpecVersion.SPEC_2021.name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_close)
                        )
                    }
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
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. 实时预览小手机看板
                ThemePreviewCardMaterial(
                    keyColor = currentKeyColor,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    paletteStyle = currentStyle,
                    colorSpec = currentSpec
                )
            }

            // 2. 种子色圆环横向滚动选择器
            item {
                MaterialSectionTitle(text = stringResource(R.string.settings_key_color))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ColorCircleChip(
                            color = null,
                            isSelected = currentKeyColor == 0,
                            onClick = { AppPreferences.updateKeyColor(0) }
                        )
                    }

                    items(keyColorOptions) { colorInt ->
                        ColorCircleChip(
                            color = Color(colorInt),
                            isSelected = currentKeyColor == colorInt,
                            onClick = { AppPreferences.updateKeyColor(colorInt) }
                        )
                    }
                }
            }

            // 3. 色彩模式分段选择 (系统 / 浅色 / 深色 / 纯黑AMOLED)
            item {
                MaterialSectionTitle(text = stringResource(R.string.settings_theme))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modeList = listOf(
                        ColorMode.MONET_SYSTEM to stringResource(R.string.settings_theme_mode_system),
                        ColorMode.MONET_LIGHT to stringResource(R.string.settings_theme_mode_light),
                        ColorMode.MONET_DARK to stringResource(R.string.settings_theme_mode_dark),
                        ColorMode.DARK_AMOLED to stringResource(R.string.settings_theme_mode_amoled),
                    )

                    modeList.forEach { (mode, label) ->
                        val isSelected = when (mode) {
                            ColorMode.MONET_SYSTEM -> currentColorMode == ColorMode.MONET_SYSTEM || currentColorMode == ColorMode.SYSTEM
                            ColorMode.MONET_LIGHT -> currentColorMode == ColorMode.MONET_LIGHT || currentColorMode == ColorMode.LIGHT
                            ColorMode.MONET_DARK -> currentColorMode == ColorMode.MONET_DARK || currentColorMode == ColorMode.DARK
                            ColorMode.DARK_AMOLED -> currentColorMode == ColorMode.DARK_AMOLED
                            else -> false
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { AppPreferences.updateColorMode(mode) },
                            label = { Text(text = label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
            }

            // 4. 调色风格与色彩规范
            item {
                MaterialSectionTitle(text = stringResource(R.string.settings_color_style))
                MaterialPreferenceCard {
                    MaterialDropdownPreference(
                        title = stringResource(R.string.settings_color_style),
                        items = styleItems,
                        selectedIndex = styleItems.indexOf(currentStyle.name).coerceAtLeast(0),
                        leadingIcon = Icons.Rounded.Style,
                        showDivider = true,
                        onSelectedIndexChange = { index ->
                            AppPreferences.updateColorStyle(styleItems[index])
                        }
                    )

                    MaterialDropdownPreference(
                        title = stringResource(R.string.settings_color_spec),
                        items = specItems,
                        selectedIndex = specItems.indexOf(currentSpec.name).coerceAtLeast(0),
                        onSelectedIndexChange = { index ->
                            AppPreferences.updateColorSpec(specItems[index])
                        }
                    )
                }
            }

            // 5. 交互外观选项
            item {
                MaterialSectionTitle(text = stringResource(R.string.settings_floating_bar_title))
                MaterialPreferenceCard {
                    MaterialSwitchPreference(
                        title = stringResource(R.string.settings_enable_blur),
                        summary = stringResource(R.string.settings_enable_blur_summary),
                        leadingIcon = Icons.Rounded.WaterDrop,
                        checked = AppPreferences.isFloatingBottomBarBlurEnabled,
                        showDivider = true,
                        onCheckedChange = { AppPreferences.updateFloatingBottomBarBlur(it) }
                    )

                    MaterialSwitchPreference(
                        title = stringResource(R.string.settings_floating_bar_title),
                        summary = stringResource(R.string.settings_floating_bar_summary),
                        leadingIcon = Icons.Rounded.CallToAction,
                        checked = AppPreferences.isFloatingBottomBarEnabled,
                        showDivider = true,
                        onCheckedChange = { AppPreferences.updateFloatingBottomBar(it) }
                    )

                    MaterialSwitchPreference(
                        title = stringResource(R.string.settings_badge_title),
                        summary = stringResource(R.string.settings_badge_summary),
                        leadingIcon = Icons.Rounded.Pin,
                        checked = AppPreferences.isNavigationBadgeEnabled,
                        showDivider = true,
                        onCheckedChange = { AppPreferences.updateNavigationBadge(it) }
                    )

                    MaterialSwitchPreference(
                        title = stringResource(R.string.settings_enable_predictive_back),
                        summary = stringResource(R.string.settings_enable_predictive_back_summary),
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        checked = AppPreferences.isPredictiveBackEnabled,
                        onCheckedChange = { AppPreferences.updatePredictiveBack(it, context) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ColorCircleChip(
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayColor = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(displayColor)
            .border(
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, Color.Transparent),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = if (color == null) MaterialTheme.colorScheme.onPrimary else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMaterial(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .aspectRatio(screenRatio)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primaryContainer)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceContainerHigh)
                )
            }
        }
    }
}
