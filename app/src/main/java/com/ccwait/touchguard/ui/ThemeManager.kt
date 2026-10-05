package com.ccwait.touchguard.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.ColorMode
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

import com.ccwait.touchguard.DarkModeOption
import com.ccwait.touchguard.ThemePalette

enum class AppThemeMode {
    Material3,
    Miuix
}

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.Miuix }

@Composable
fun isInDarkTheme(): Boolean {
    return AppPreferences.isDark(isSystemInDarkTheme())
}

/**
 * 完整对齐 SukiSU-Ultra 的 Miuix + Monet 动态取色主题引擎
 */
@Composable
fun AppThemeContainer(
    themeMode: AppThemeMode = AppPreferences.themeMode,
    darkMode: DarkModeOption = AppPreferences.darkMode,
    palette: ThemePalette = AppPreferences.themePalette,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = AppPreferences.isDark(systemDarkTheme)

    // Monet 动态提取系统种子色作为关键色，或使用指定强调色
    val resolvedKeyColor: Color? = when {
        palette == ThemePalette.MONET ->
            if (darkTheme) dynamicDarkColorScheme(context).primary
            else dynamicLightColorScheme(context).primary
        palette.keyColorHex != null ->
            Color(palette.keyColorHex)
        else -> null
    }

    val controller = remember(darkMode, palette, darkTheme, resolvedKeyColor) {
        val mode = when {
            palette == ThemePalette.CLASSIC -> when (darkMode) {
                DarkModeOption.SYSTEM -> ColorSchemeMode.System
                DarkModeOption.LIGHT -> ColorSchemeMode.Light
                DarkModeOption.DARK -> ColorSchemeMode.Dark
            }
            else -> when (darkMode) {
                DarkModeOption.SYSTEM -> ColorSchemeMode.MonetSystem
                DarkModeOption.LIGHT -> ColorSchemeMode.MonetLight
                DarkModeOption.DARK -> ColorSchemeMode.MonetDark
            }
        }
        ThemeController(
            colorSchemeMode = mode,
            keyColor = resolvedKeyColor,
            isDark = darkTheme,
        )
    }

    val m3ColorScheme = remember(darkTheme, palette, resolvedKeyColor) {
        val hasMonet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        if (palette == ThemePalette.MONET && hasMonet) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (resolvedKeyColor != null) {
            if (darkTheme) {
                darkColorScheme(
                    primary = resolvedKeyColor,
                    primaryContainer = resolvedKeyColor.copy(alpha = 0.35f),
                    secondary = resolvedKeyColor,
                    secondaryContainer = resolvedKeyColor.copy(alpha = 0.22f),
                )
            } else {
                lightColorScheme(
                    primary = resolvedKeyColor,
                    primaryContainer = resolvedKeyColor.copy(alpha = 0.18f),
                    secondary = resolvedKeyColor,
                    secondaryContainer = resolvedKeyColor.copy(alpha = 0.12f),
                )
            }
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
        if (themeMode == AppThemeMode.Miuix) {
            MiuixTheme(
                controller = controller,
                content = {
                    val view = LocalView.current
                    if (!view.isInEditMode) {
                        SideEffect {
                            val window = (context as? Activity)?.window ?: return@SideEffect
                            WindowInsetsControllerCompat(window, window.decorView).apply {
                                isAppearanceLightStatusBars = !darkTheme
                                isAppearanceLightNavigationBars = !darkTheme
                            }
                        }
                    }
                    CompositionLocalProvider(
                        LocalContentColor provides MiuixTheme.colorScheme.onBackground,
                    ) {
                        content()
                    }
                }
            )
        } else {
            MaterialTheme(
                colorScheme = m3ColorScheme,
                content = {
                    val view = LocalView.current
                    if (!view.isInEditMode) {
                        SideEffect {
                            val window = (context as? Activity)?.window ?: return@SideEffect
                            WindowInsetsControllerCompat(window, window.decorView).apply {
                                isAppearanceLightStatusBars = !darkTheme
                                isAppearanceLightNavigationBars = !darkTheme
                            }
                        }
                    }
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        content = content
                    )
                }
            )
        }
    }
}
