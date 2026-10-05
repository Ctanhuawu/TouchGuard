package com.ccwait.touchguard

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.system.DefaultHapticFeedbackService
import com.ccwait.touchguard.system.DefaultSystemPanelController
import com.ccwait.touchguard.ui.AppThemeContainer
import com.ccwait.touchguard.ui.components.rememberMainPagerState
import com.ccwait.touchguard.ui.navigation.LocalNavigator
import com.ccwait.touchguard.ui.navigation.Route
import com.ccwait.touchguard.ui.navigation.rememberNavigator
import com.ccwait.touchguard.ui.screens.MainScreen
import com.ccwait.touchguard.ui.screens.colorpalette.ColorPaletteScreen
import com.ccwait.touchguard.ui.theme.ColorMode
import com.ccwait.touchguard.ui.util.ProvideAppLanguage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius

class MainActivity : ComponentActivity() {

    private val latestIntent = mutableStateOf<Intent?>(null)
    private val intentSequence = androidx.compose.runtime.mutableLongStateOf(0L)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntent.value = intent
        intentSequence.longValue = System.currentTimeMillis()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化持久化配置与方案管理器
        AppPreferences.init(this)
        com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(AppPreferences.appLanguage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                val method = ApplicationInfo::class.java.getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(applicationInfo, AppPreferences.isPredictiveBackEnabled)
            }
        }
        TouchLockManager.init(this)

        TouchLockManager.onLockStateChanged = { locked ->
            applyScreenHoldState(locked)
        }

        if (AppPreferences.isKeepAliveEnabled) {
            com.ccwait.touchguard.service.TouchGuardForegroundService.start(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (intent?.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            DefaultSystemPanelController.collapsePanels(this@MainActivity)
        }

        val reqStrategy = intent?.getStringExtra("set_strategy")
        if (reqStrategy != null) {
            TouchLockManager.selectStrategy(this@MainActivity, StrategyType.fromId(reqStrategy))
        }
        val reqFloating = intent?.getStringExtra("set_floating_bar")
        if (reqFloating != null) {
            AppPreferences.updateFloatingBottomBar(reqFloating.toBoolean())
        }
        val reqColorMode = intent?.getStringExtra("set_color_mode")
        if (reqColorMode != null) {
            ColorMode.entries.firstOrNull { it.name.equals(reqColorMode, ignoreCase = true) }?.let {
                AppPreferences.updateColorMode(it)
            }
        }
        val reqLang = intent?.getStringExtra("set_language")
        if (reqLang != null) {
            val lang = com.ccwait.touchguard.model.AppLanguage.fromId(reqLang)
            AppPreferences.updateAppLanguage(lang)
            com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(lang)
        }

        lifecycleScope.launch {
            TouchLockManager.checkAllReadiness(this@MainActivity, forceRequest = false)
            TouchLockManager.prepareAll(this@MainActivity)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    android.os.PowerManager.ON_AFTER_RELEASE,
                "TouchGuard:WakeScreen"
            )
            wl.acquire(3000)
            wl.release()
        } catch (_: Exception) {}

        applyScreenHoldState(TouchLockManager.isTouchLocked)

        setContent {
            val appSettings = AppPreferences.getAppSettings()
            val themeMode = AppPreferences.themeMode
            val isDark = AppPreferences.isDark(isSystemInDarkTheme())

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose { }
            }

            val navigator = rememberNavigator(Route.Main)
            val navDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalNavigationEventDispatcherOwner provides navDispatcherOwner,
            ) {
                AppThemeContainer(
                    themeMode = themeMode,
                    appSettings = appSettings
                ) {
                    ProvideAppLanguage {
                        val initialTab = intent?.getIntExtra("tab", 0)?.coerceIn(0, 3) ?: 0
                        val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 4 })
                        val mainPagerState = rememberMainPagerState(pagerState = pagerState)

                        LaunchedEffect(intentSequence.longValue) {
                            val targetIntent = latestIntent.value
                            if (targetIntent != null) {
                                if (targetIntent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
                                    DefaultSystemPanelController.collapsePanels(this@MainActivity)
                                    mainPagerState.animateToPage(0)
                                }
                                val reqStrat = targetIntent.getStringExtra("set_strategy")
                                if (reqStrat != null) {
                                    TouchLockManager.selectStrategy(this@MainActivity, StrategyType.fromId(reqStrat))
                                }
                                val reqFloat = targetIntent.getStringExtra("set_floating_bar")
                                if (reqFloat != null) {
                                    AppPreferences.updateFloatingBottomBar(reqFloat.toBoolean())
                                }
                                val reqColor = targetIntent.getStringExtra("set_color_mode")
                                if (reqColor != null) {
                                    ColorMode.entries.firstOrNull { it.name.equals(reqColor, ignoreCase = true) }?.let {
                                        AppPreferences.updateColorMode(it)
                                    }
                                }
                                val reqLang = targetIntent.getStringExtra("set_language")
                                if (reqLang != null) {
                                    val lang = com.ccwait.touchguard.model.AppLanguage.fromId(reqLang)
                                    AppPreferences.updateAppLanguage(lang)
                                    com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(lang)
                                }
                                val targetTab = targetIntent.getIntExtra("tab", -1)
                                if (targetTab in 0..3) {
                                    mainPagerState.animateToPage(targetTab)
                                }
                            }
                        }

                        val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                        val swipeDismiss = if (isRtl) top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection.RightToLeft else top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection.LeftToRight

                        NavDisplay(
                            backStack = navigator.backStack,
                            effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
                            onBack = { navigator.pop() }
                        ) {
                            entry<Route.Main> {
                                MainScreen(
                                    initialTab = initialTab,
                                    mainPagerState = mainPagerState,
                                    onLockToggle = {
                                        if (TouchLockManager.isTouchLocked) unlockTouch() else lockTouch()
                                    },
                                    onScreenHoldStateUpdate = {
                                        applyScreenHoldState(true)
                                        com.ccwait.touchguard.strategy.GlobalScreenPolicyManager.applyPolicies(this@MainActivity, true)
                                    },
                                    onVibrate = { duration ->
                                        vibratePhone(duration)
                                    },
                                    onResetLock = {
                                        unlockTouch()
                                        AppLogManager.addLog("重置", "已手动重载并释放所有触控锁", isSuccess = true)
                                        Toast.makeText(this@MainActivity, "已重置并释放所有触控锁", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            entry<Route.ColorPalette>(swipeDismiss = swipeDismiss) {
                                ColorPaletteScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyScreenHoldState(locked: Boolean) {
        runOnUiThread {
            if (locked) {
                // 1. 保持屏幕常亮
                if (AppPreferences.isKeepScreenOnEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                // 2. 屏幕方向锁定
                requestedOrientation = AppPreferences.screenOrientationLock.orientationValue

                // 3. 锁定当前屏幕亮度
                if (AppPreferences.isBrightnessLockEnabled) {
                    try {
                        val curBrightness = android.provider.Settings.System.getInt(
                            contentResolver,
                            android.provider.Settings.System.SCREEN_BRIGHTNESS
                        ) / 255f
                        val lp = window.attributes
                        lp.screenBrightness = curBrightness.coerceIn(0.01f, 1.0f)
                        window.attributes = lp
                    } catch (_: Exception) {}
                }

                // 4. 隐藏通知栏与小白条
                try {
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                    if (AppPreferences.isHideSystemBarsEnabled) {
                        insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    }
                } catch (_: Throwable) {}
            } else {
                // 恢复默认状态
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp

                try {
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                } catch (_: Throwable) {}
            }
        }
    }

    private fun lockTouch() {
        lifecycleScope.launch {
            TouchLockManager.lock(this@MainActivity, source = "应用界面")
        }
    }

    private fun unlockTouch() {
        lifecycleScope.launch {
            TouchLockManager.unlock(this@MainActivity, source = "应用界面")
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (TouchLockManager.isTouchLocked) {
            val keyCode = event.keyCode
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        // 仅当 repeatCount == 0 时才视为一次真正的按键按下
                        // 如果 repeatCount > 0 说明是用户长按产生的系统自动重复事件，坚决忽略！
                        if (event.repeatCount == 0) {
                            PhysicalKeyUnlockHandler.onKeyDown(this, keyCode)
                        }
                        return true // 消费按键，防止系统音量面板弹出
                    }
                    KeyEvent.ACTION_UP -> {
                        PhysicalKeyUnlockHandler.onKeyUp(keyCode)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun vibratePhone(durationMs: Long) {
        DefaultHapticFeedbackService.vibrate(this, durationMs)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            TouchLockManager.checkAllReadiness(this@MainActivity, forceRequest = false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TouchLockManager.onLockStateChanged = null
        if (!TouchLockManager.isTouchLocked) {
            applyScreenHoldState(false)
        }
    }
}
