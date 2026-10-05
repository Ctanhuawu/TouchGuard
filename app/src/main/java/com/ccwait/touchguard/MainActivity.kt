package com.ccwait.touchguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.lifecycle.lifecycleScope
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.model.RootPermissionManager
import com.ccwait.touchguard.model.UnlockMechanism
import com.ccwait.touchguard.strategy.StrategyType
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.ui.components.rememberMainPagerState
import com.ccwait.touchguard.ui.screens.MainScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 核心交互与恢复计时
    private var keyPressCount = 0
    private var lastKeyPressTime = 0L
    private var lastKeyCode = 0

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
            TouchLockManager.collapsePanels(this@MainActivity)
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

            val initialTab = intent?.getIntExtra("tab", 0)?.coerceIn(0, 3) ?: 0
            val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 4 })
            val mainPagerState = rememberMainPagerState(pagerState = pagerState)

            LaunchedEffect(intentSequence.longValue) {
                val targetIntent = latestIntent.value
                if (targetIntent != null) {
                    if (targetIntent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
                        TouchLockManager.collapsePanels(this@MainActivity)
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
            } else {
                // 恢复默认状态
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    private fun lockTouch() {
        keyPressCount = 0
        lastKeyPressTime = 0
        lastKeyCode = 0
        lifecycleScope.launch {
            TouchLockManager.lock(this@MainActivity, source = "应用界面")
        }
    }

    private fun unlockTouch() {
        keyPressCount = 0
        lastKeyPressTime = 0
        lastKeyCode = 0
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
        if (!AppPreferences.isHapticFeedbackEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
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
