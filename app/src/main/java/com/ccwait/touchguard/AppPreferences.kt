package com.ccwait.touchguard

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.model.AppLanguage
import com.ccwait.touchguard.model.ScreenOrientationLock
import com.ccwait.touchguard.model.UnlockMechanism
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.ui.AppThemeMode

object AppPreferences {
    private const val PREFS_NAME = "touchguard_config"
    private const val KEY_ACTIVE_STRATEGY = "active_strategy_id"

    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_SCREEN_ORIENTATION = "screen_orientation"
    private const val KEY_BRIGHTNESS_LOCK = "brightness_lock"
    private const val KEY_UNLOCK_MECHANISM = "unlock_mechanism"
    private const val KEY_FLOATING_INDICATOR = "floating_indicator"
    private const val KEY_AUTO_UNLOCK_SCREEN_OFF = "auto_unlock_screen_off"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_FOREGROUND_SERVICE = "foreground_service"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COLOR_MODE = "color_mode"
    private const val KEY_DARK_MODE = "dark_mode_option"
    private const val KEY_THEME_PALETTE = "theme_palette"
    private const val KEY_FLOATING_BOTTOM_BAR = "floating_bottom_bar"
    private const val KEY_FLOATING_BOTTOM_BAR_BLUR = "floating_bottom_bar_blur"
    private const val KEY_NAVIGATION_BADGE = "navigation_badge"
    private const val KEY_APP_LANGUAGE = "app_language"

    private var prefs: SharedPreferences? = null

    // Compose 可观察全局持久化状态
    var activeStrategyType by mutableStateOf(StrategyType.ROOT_EVIOCGRAB)
        private set
    var appLanguage by mutableStateOf(AppLanguage.FOLLOW_SYSTEM)
        private set
    var isKeepScreenOnEnabled by mutableStateOf(true)
        private set
    var screenOrientationLock by mutableStateOf(ScreenOrientationLock.FOLLOW_SYSTEM)
        private set
    var isBrightnessLockEnabled by mutableStateOf(false)
        private set
    var unlockMechanism by mutableStateOf(UnlockMechanism.DOUBLE_VOLUME_DOWN)
        private set
    var isFloatingIndicatorEnabled by mutableStateOf(true)
        private set
    var isAutoUnlockOnScreenOffEnabled by mutableStateOf(true)
        private set
    var isHapticFeedbackEnabled by mutableStateOf(true)
        private set
    var isAutoStartEnabled by mutableStateOf(true)
        private set
    var isForegroundServiceEnabled by mutableStateOf(true)
        private set
    var isKeepAliveEnabled by mutableStateOf(true)
        private set
    var themeMode by mutableStateOf(AppThemeMode.Miuix)
        private set
    var darkMode by mutableStateOf(DarkModeOption.SYSTEM)
        private set
    var themePalette by mutableStateOf(ThemePalette.MONET)
        private set
    val colorMode: ColorMode
        get() = when (themePalette) {
            ThemePalette.CLASSIC -> when (darkMode) {
                DarkModeOption.SYSTEM -> ColorMode.SYSTEM
                DarkModeOption.LIGHT -> ColorMode.LIGHT
                DarkModeOption.DARK -> ColorMode.DARK
            }
            else -> when (darkMode) {
                DarkModeOption.SYSTEM -> ColorMode.MONET_SYSTEM
                DarkModeOption.LIGHT -> ColorMode.MONET_LIGHT
                DarkModeOption.DARK -> ColorMode.MONET_DARK
            }
        }
    var isFloatingBottomBarEnabled by mutableStateOf(false)
        private set
    var isFloatingBottomBarBlurEnabled by mutableStateOf(true)
        private set
    var isNavigationBadgeEnabled by mutableStateOf(true)
        private set

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        isKeepScreenOnEnabled = sp.getBoolean(KEY_KEEP_SCREEN_ON, true)
        val orientationId = sp.getString(KEY_SCREEN_ORIENTATION, ScreenOrientationLock.FOLLOW_SYSTEM.id)
        screenOrientationLock = ScreenOrientationLock.fromId(orientationId)
        isBrightnessLockEnabled = sp.getBoolean(KEY_BRIGHTNESS_LOCK, false)
        val unlockId = sp.getString(KEY_UNLOCK_MECHANISM, UnlockMechanism.DOUBLE_VOLUME_DOWN.id)
        unlockMechanism = UnlockMechanism.fromId(unlockId)
        isFloatingIndicatorEnabled = sp.getBoolean(KEY_FLOATING_INDICATOR, true)
        isAutoUnlockOnScreenOffEnabled = sp.getBoolean(KEY_AUTO_UNLOCK_SCREEN_OFF, true)
        isHapticFeedbackEnabled = sp.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        isAutoStartEnabled = sp.getBoolean(KEY_AUTO_START, true)
        isForegroundServiceEnabled = sp.getBoolean(KEY_FOREGROUND_SERVICE, true)
        isKeepAliveEnabled = sp.getBoolean(KEY_FOREGROUND_SERVICE, true)
        isFloatingBottomBarEnabled = sp.getBoolean(KEY_FLOATING_BOTTOM_BAR, false)
        isFloatingBottomBarBlurEnabled = sp.getBoolean(KEY_FLOATING_BOTTOM_BAR_BLUR, true)
        isNavigationBadgeEnabled = sp.getBoolean(KEY_NAVIGATION_BADGE, true)

        val darkModeId = sp.getString(KEY_DARK_MODE, null)
        if (darkModeId != null) {
            darkMode = DarkModeOption.fromId(darkModeId)
        } else {
            val legacyColorMode = sp.getInt(KEY_COLOR_MODE, ColorMode.MONET_SYSTEM.value)
            val old = ColorMode.fromValue(legacyColorMode)
            darkMode = when {
                old == ColorMode.DARK || old == ColorMode.MONET_DARK -> DarkModeOption.DARK
                old == ColorMode.LIGHT || old == ColorMode.MONET_LIGHT -> DarkModeOption.LIGHT
                else -> DarkModeOption.SYSTEM
            }
        }

        val paletteId = sp.getString(KEY_THEME_PALETTE, null)
        if (paletteId != null) {
            themePalette = ThemePalette.fromId(paletteId)
        } else {
            val legacyColorMode = sp.getInt(KEY_COLOR_MODE, ColorMode.MONET_SYSTEM.value)
            val old = ColorMode.fromValue(legacyColorMode)
            themePalette = if (old.isMonet) ThemePalette.MONET else ThemePalette.CLASSIC
        }

        val hasUserSelectedTheme = sp.getBoolean("has_user_selected_theme", false)
        val themeName = if (hasUserSelectedTheme) sp.getString(KEY_THEME_MODE, AppThemeMode.Miuix.name) else AppThemeMode.Miuix.name
        themeMode = try {
            AppThemeMode.valueOf(themeName ?: AppThemeMode.Miuix.name)
        } catch (_: Exception) {
            AppThemeMode.Miuix
        }

        val langId = sp.getString(KEY_APP_LANGUAGE, AppLanguage.FOLLOW_SYSTEM.id)
        appLanguage = AppLanguage.fromId(langId)

        val stratId = sp.getString(KEY_ACTIVE_STRATEGY, null)
            ?: context.getSharedPreferences("touchguard_strategy_prefs", Context.MODE_PRIVATE).getString("active_strategy_id", StrategyType.ROOT_EVIOCGRAB.id)
        activeStrategyType = StrategyType.fromId(stratId)
    }

    fun updateActiveStrategy(value: StrategyType) {
        activeStrategyType = value
        prefs?.edit()?.putString(KEY_ACTIVE_STRATEGY, value.id)?.apply()
    }

    fun updateAppLanguage(value: AppLanguage) {
        appLanguage = value
        prefs?.edit()?.putString(KEY_APP_LANGUAGE, value.id)?.apply()
    }

    fun setKeepScreenOn(value: Boolean) {
        isKeepScreenOnEnabled = value
        prefs?.edit()?.putBoolean(KEY_KEEP_SCREEN_ON, value)?.apply()
    }

    fun setScreenOrientation(value: ScreenOrientationLock) {
        screenOrientationLock = value
        prefs?.edit()?.putString(KEY_SCREEN_ORIENTATION, value.id)?.apply()
    }

    fun setBrightnessLock(value: Boolean) {
        isBrightnessLockEnabled = value
        prefs?.edit()?.putBoolean(KEY_BRIGHTNESS_LOCK, value)?.apply()
    }

    fun updateUnlockMechanism(value: UnlockMechanism) {
        unlockMechanism = value
        prefs?.edit()?.putString(KEY_UNLOCK_MECHANISM, value.id)?.apply()
    }

    fun setFloatingIndicator(value: Boolean) {
        isFloatingIndicatorEnabled = value
        prefs?.edit()?.putBoolean(KEY_FLOATING_INDICATOR, value)?.apply()
    }

    fun setAutoUnlockOnScreenOff(value: Boolean) {
        isAutoUnlockOnScreenOffEnabled = value
        prefs?.edit()?.putBoolean(KEY_AUTO_UNLOCK_SCREEN_OFF, value)?.apply()
    }

    fun setHapticFeedback(value: Boolean) {
        isHapticFeedbackEnabled = value
        prefs?.edit()?.putBoolean(KEY_HAPTIC_FEEDBACK, value)?.apply()
    }

    fun setAutoStart(value: Boolean) {
        isAutoStartEnabled = value
        prefs?.edit()?.putBoolean(KEY_AUTO_START, value)?.apply()
    }

    fun setForegroundService(value: Boolean) {
        isForegroundServiceEnabled = value
        prefs?.edit()?.putBoolean(KEY_FOREGROUND_SERVICE, value)?.apply()
    }

    fun setTheme(value: AppThemeMode) {
        themeMode = value
        prefs?.edit()
            ?.putString(KEY_THEME_MODE, value.name)
            ?.putBoolean("has_user_selected_theme", true)
            ?.apply()
    }

    fun updateDarkMode(value: DarkModeOption) {
        darkMode = value
        prefs?.edit()?.putString(KEY_DARK_MODE, value.id)?.apply()
    }

    fun updateThemePalette(value: ThemePalette) {
        themePalette = value
        prefs?.edit()?.putString(KEY_THEME_PALETTE, value.id)?.apply()
    }

    fun isDark(systemDark: Boolean): Boolean = when (darkMode) {
        DarkModeOption.DARK -> true
        DarkModeOption.LIGHT -> false
        DarkModeOption.SYSTEM -> systemDark
    }

    fun updateColorMode(value: ColorMode) {
        darkMode = when {
            value.isDark -> DarkModeOption.DARK
            value == ColorMode.LIGHT || value == ColorMode.MONET_LIGHT -> DarkModeOption.LIGHT
            else -> DarkModeOption.SYSTEM
        }
        themePalette = if (value.isMonet) ThemePalette.MONET else ThemePalette.CLASSIC
        prefs?.edit()
            ?.putString(KEY_DARK_MODE, darkMode.id)
            ?.putString(KEY_THEME_PALETTE, themePalette.id)
            ?.putInt(KEY_COLOR_MODE, value.value)
            ?.apply()
    }

    fun updateFloatingBottomBar(value: Boolean) {
        isFloatingBottomBarEnabled = value
        prefs?.edit()?.putBoolean(KEY_FLOATING_BOTTOM_BAR, value)?.apply()
    }

    fun updateFloatingBottomBarBlur(value: Boolean) {
        isFloatingBottomBarBlurEnabled = value
        prefs?.edit()?.putBoolean(KEY_FLOATING_BOTTOM_BAR_BLUR, value)?.apply()
    }

    fun updateNavigationBadge(value: Boolean) {
        isNavigationBadgeEnabled = value
        prefs?.edit()?.putBoolean(KEY_NAVIGATION_BADGE, value)?.apply()
    }

    fun setKeepAlive(value: Boolean) {
        isKeepAliveEnabled = value
        setForegroundService(value)
    }
}

enum class DarkModeOption(
    val id: String,
    val label: String,
    @StringRes val labelRes: Int
) {
    SYSTEM("system", "跟随系统", R.string.dark_mode_system),
    LIGHT("light", "浅色模式", R.string.dark_mode_light),
    DARK("dark", "深色模式", R.string.dark_mode_dark);

    val isDark: Boolean get() = this == DARK
    val isSystem: Boolean get() = this == SYSTEM

    companion object {
        fun fromId(id: String?): DarkModeOption = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class ThemePalette(
    val id: String,
    val label: String,
    val description: String,
    val keyColorHex: Long?,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int
) {
    MONET("monet", "Monet 动态取色", "提取系统壁纸色调，自适应强调色", null, R.string.palette_monet_title, R.string.palette_monet_desc),
    CLASSIC("classic", "经典 HyperOS 蓝", "MIUI/HyperOS 原生经典蓝色基调", null, R.string.palette_classic_title, R.string.palette_classic_desc),
    XIAOMI_ORANGE("orange", "小米经典橙", "小米标志性活力橙色强调", 0xFFFF6900, R.string.palette_orange_title, R.string.palette_orange_desc),
    SUKI_CYAN("cyan", "Suki 原生青", "SukiSU 质感薄荷青强调色", 0xFF009688, R.string.palette_cyan_title, R.string.palette_cyan_desc),
    NATURE_GREEN("green", "生机绿", "清新自然绿色基调", 0xFF4CAF50, R.string.palette_green_title, R.string.palette_green_desc);

    companion object {
        fun fromId(id: String?): ThemePalette = entries.firstOrNull { it.id == id } ?: MONET
    }
}

enum class ColorMode(val value: Int, val label: String) {
    MONET_SYSTEM(0, "Monet (跟随系统)"),
    MONET_LIGHT(1, "Monet (浅色模式)"),
    MONET_DARK(2, "Monet (深色模式)"),
    SYSTEM(3, "标准 (跟随系统)"),
    LIGHT(4, "标准 (浅色模式)"),
    DARK(5, "标准 (深色模式)");

    val isDark: Boolean get() = this == DARK || this == MONET_DARK
    val isSystem: Boolean get() = this == SYSTEM || this == MONET_SYSTEM
    val isMonet: Boolean get() = this == MONET_SYSTEM || this == MONET_LIGHT || this == MONET_DARK

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: MONET_SYSTEM
    }
}

