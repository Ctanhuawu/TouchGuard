package com.ccwait.touchguard

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.model.AppLanguage
import com.ccwait.touchguard.model.ScreenOrientationLock
import com.ccwait.touchguard.model.UnlockMechanism
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.ui.AppThemeMode
import com.ccwait.touchguard.ui.theme.AppSettings
import com.ccwait.touchguard.ui.theme.ColorMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

object AppPreferences {
    private const val PREFS_NAME = "touchguard_config"
    private const val KEY_ACTIVE_STRATEGY = "active_strategy_id"

    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_SCREEN_ORIENTATION = "screen_orientation"
    private const val KEY_BRIGHTNESS_LOCK = "brightness_lock"
    private const val KEY_HIDE_SYSTEM_BARS = "hide_system_bars"
    private const val KEY_UNLOCK_MECHANISM = "unlock_mechanism"
    private const val KEY_FLOATING_INDICATOR = "floating_indicator"
    private const val KEY_AUTO_UNLOCK_SCREEN_OFF = "auto_unlock_screen_off"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_FOREGROUND_SERVICE = "foreground_service"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COLOR_MODE = "theme_color_mode"
    private const val KEY_KEY_COLOR = "theme_key_color"
    private const val KEY_COLOR_STYLE = "theme_color_style"
    private const val KEY_COLOR_SPEC = "theme_color_spec"
    private const val KEY_MIUIX_MONET = "theme_miuix_monet"
    private const val KEY_FLOATING_BOTTOM_BAR = "floating_bottom_bar"
    private const val KEY_FLOATING_BOTTOM_BAR_BLUR = "floating_bottom_bar_blur"
    private const val KEY_NAVIGATION_BADGE = "navigation_badge"
    private const val KEY_PREDICTIVE_BACK = "enable_predictive_back"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_CHECK_UPDATE = "check_update"
    private const val KEY_UPDATE_CHANNEL = "update_channel"
    private const val KEY_KEY_PRESS_WINDOW_MS = "key_press_window_ms"
    const val DEFAULT_KEY_PRESS_WINDOW_MS = 1000

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
    var isHideSystemBarsEnabled by mutableStateOf(false)
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
    var colorMode by mutableStateOf(ColorMode.MONET_SYSTEM)
        private set
    var keyColor by mutableIntStateOf(0)
        private set
    var colorStyle by mutableStateOf(PaletteStyle.TonalSpot.name)
        private set
    var colorSpec by mutableStateOf(ColorSpec.SpecVersion.SPEC_2025.name)
        private set
    var miuixMonet by mutableStateOf(true)
        private set
    var isFloatingBottomBarEnabled by mutableStateOf(false)
        private set
    var isFloatingBottomBarBlurEnabled by mutableStateOf(true)
        private set
    var isNavigationBadgeEnabled by mutableStateOf(true)
        private set
    var isPredictiveBackEnabled by mutableStateOf(true)
        private set
    var isCheckUpdateEnabled by mutableStateOf(true)
        private set
    var updateChannel by mutableStateOf(com.ccwait.touchguard.model.UpdateChannel.BETA)
        private set
    var keyPressWindowMs by mutableIntStateOf(DEFAULT_KEY_PRESS_WINDOW_MS)
        private set

    fun getAppSettings(): AppSettings {
        val palette = try {
            PaletteStyle.valueOf(colorStyle)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val spec = try {
            ColorSpec.SpecVersion.valueOf(colorSpec)
        } catch (_: Exception) {
            ColorSpec.SpecVersion.SPEC_2025
        }
        return AppSettings(
            colorMode = colorMode,
            keyColor = keyColor,
            paletteStyle = palette,
            colorSpec = spec,
            miuixMonet = miuixMonet
        )
    }

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        isKeepScreenOnEnabled = sp.getBoolean(KEY_KEEP_SCREEN_ON, true)
        val orientationId = sp.getString(KEY_SCREEN_ORIENTATION, ScreenOrientationLock.FOLLOW_SYSTEM.id)
        screenOrientationLock = ScreenOrientationLock.fromId(orientationId)
        isBrightnessLockEnabled = sp.getBoolean(KEY_BRIGHTNESS_LOCK, false)
        isHideSystemBarsEnabled = sp.getBoolean(KEY_HIDE_SYSTEM_BARS, false)
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
        isPredictiveBackEnabled = sp.getBoolean(KEY_PREDICTIVE_BACK, true)
        isCheckUpdateEnabled = sp.getBoolean(KEY_CHECK_UPDATE, true)
        val channelId = sp.getString(KEY_UPDATE_CHANNEL, com.ccwait.touchguard.model.UpdateChannel.BETA.id)
        updateChannel = com.ccwait.touchguard.model.UpdateChannel.fromId(channelId)
        keyPressWindowMs = sp.getInt(KEY_KEY_PRESS_WINDOW_MS, DEFAULT_KEY_PRESS_WINDOW_MS)

        val colorModeValue = sp.getInt(KEY_COLOR_MODE, ColorMode.MONET_SYSTEM.value)
        colorMode = ColorMode.fromValue(colorModeValue)
        keyColor = sp.getInt(KEY_KEY_COLOR, 0)
        colorStyle = sp.getString(KEY_COLOR_STYLE, PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        colorSpec = sp.getString(KEY_COLOR_SPEC, ColorSpec.SpecVersion.SPEC_2025.name) ?: ColorSpec.SpecVersion.SPEC_2025.name
        miuixMonet = sp.getBoolean(KEY_MIUIX_MONET, true)

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

    fun setHideSystemBars(value: Boolean) {
        isHideSystemBarsEnabled = value
        prefs?.edit()?.putBoolean(KEY_HIDE_SYSTEM_BARS, value)?.apply()
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

    fun updateColorMode(value: ColorMode) {
        colorMode = value
        prefs?.edit()?.putInt(KEY_COLOR_MODE, value.value)?.apply()
    }

    fun updateKeyColor(value: Int) {
        keyColor = value
        prefs?.edit()?.putInt(KEY_KEY_COLOR, value)?.apply()
    }

    fun updateColorStyle(value: String) {
        colorStyle = value
        prefs?.edit()?.putString(KEY_COLOR_STYLE, value)?.apply()
    }

    fun updateColorSpec(value: String) {
        colorSpec = value
        prefs?.edit()?.putString(KEY_COLOR_SPEC, value)?.apply()
    }

    fun updateMiuixMonet(value: Boolean) {
        miuixMonet = value
        prefs?.edit()?.putBoolean(KEY_MIUIX_MONET, value)?.apply()
    }

    fun isDark(systemDark: Boolean): Boolean = when {
        colorMode.isDark -> true
        colorMode == ColorMode.LIGHT || colorMode == ColorMode.MONET_LIGHT -> false
        else -> systemDark
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

    fun updateCheckUpdate(value: Boolean) {
        isCheckUpdateEnabled = value
        prefs?.edit()?.putBoolean(KEY_CHECK_UPDATE, value)?.apply()
    }

    fun updateUpdateChannel(value: com.ccwait.touchguard.model.UpdateChannel) {
        updateChannel = value
        prefs?.edit()?.putString(KEY_UPDATE_CHANNEL, value.id)?.apply()
    }

    fun updateKeyPressWindowMs(value: Int) {
        val clamped = value.coerceIn(300, 3000)
        keyPressWindowMs = clamped
        prefs?.edit()?.putInt(KEY_KEY_PRESS_WINDOW_MS, clamped)?.apply()
    }

    fun resetKeyPressWindowMs() {
        updateKeyPressWindowMs(DEFAULT_KEY_PRESS_WINDOW_MS)
    }

    fun updatePredictiveBack(value: Boolean, context: Context? = null) {
        isPredictiveBackEnabled = value
        prefs?.edit()?.putBoolean(KEY_PREDICTIVE_BACK, value)?.apply()
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                val method = ApplicationInfo::class.java.getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(context.applicationInfo, value)
            }
        }
    }

    fun setKeepAlive(value: Boolean) {
        isKeepAliveEnabled = value
        setForegroundService(value)
    }
}
