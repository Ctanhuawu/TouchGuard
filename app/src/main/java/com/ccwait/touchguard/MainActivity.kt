package com.ccwait.touchguard

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
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
import com.ccwait.touchguard.strategy.GlobalScreenPolicyManager
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

    /**
     * 处理外部再次拉起时的 Intent 传参（如快捷设置图块）
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntent.value = intent
        intentSequence.longValue = System.currentTimeMillis()
    }

    /**
     * 初始化入口：加载配置、初始化策略引擎、注册常驻服务并渲染 UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("")
            }
        }

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

        handleIntentParameters(intent)

        lifecycleScope.launch {
            TouchLockManager.checkAllReadiness(this@MainActivity, forceRequest = false)
            TouchLockManager.prepareAll(this@MainActivity)
        }

        if (AppPreferences.isCheckUpdateEnabled) {
            com.ccwait.touchguard.update.UpdateManager.checkUpdate(this, isManual = false)
        }

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
                            val targetIntent = latestIntent.value ?: return@LaunchedEffect
                            handleIntentParameters(targetIntent)
                            if (targetIntent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
                                mainPagerState.animateToPage(0)
                            }
                            val targetTab = targetIntent.getIntExtra("tab", -1)
                            if (targetTab in 0..3) {
                                mainPagerState.animateToPage(targetTab)
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
                                    }
                                )
                            }
                            entry<Route.ColorPalette>(swipeDismiss = swipeDismiss) {
                                ColorPaletteScreen()
                            }
                        }

                        val activeUpdate = com.ccwait.touchguard.update.UpdateManager.activeUpdate
                        if (activeUpdate != null) {
                            com.ccwait.touchguard.ui.components.UpdateDialog(
                                updateInfo = activeUpdate,
                                onDismiss = { com.ccwait.touchguard.update.UpdateManager.dismiss() }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析外部 Intent 参数（如切换锁定策略、底部栏显示、主题色、语言等）
     */
    private fun handleIntentParameters(intent: Intent?) {
        val targetIntent = intent ?: return
        if (targetIntent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            DefaultSystemPanelController.collapsePanels(this)
        }
        targetIntent.getStringExtra("set_strategy")?.let { stratId ->
            TouchLockManager.selectStrategy(this, StrategyType.fromId(stratId))
        }
        targetIntent.getStringExtra("set_floating_bar")?.let { floatingStr ->
            AppPreferences.updateFloatingBottomBar(floatingStr.toBoolean())
        }
        targetIntent.getStringExtra("set_color_mode")?.let { colorModeStr ->
            ColorMode.entries.firstOrNull { it.name.equals(colorModeStr, ignoreCase = true) }?.let {
                AppPreferences.updateColorMode(it)
            }
        }
        targetIntent.getStringExtra("set_language")?.let { langStr ->
            val lang = com.ccwait.touchguard.model.AppLanguage.fromId(langStr)
            AppPreferences.updateAppLanguage(lang)
            com.ccwait.touchguard.ui.util.LocalizationManager.updateLocaleOnly(lang)
        }
    }

    /**
     * 应用 Activity 维度的屏幕保持状态（常亮、屏幕方向、亮度锁定及系统栏隐藏）
     */
    private fun applyScreenHoldState(locked: Boolean) {
        GlobalScreenPolicyManager.applyToActivity(this, locked)
    }

    /**
     * 主界面锁定按钮响应：启动协程执行底层触控锁定
     */
    private fun lockTouch() {
        lifecycleScope.launch {
            TouchLockManager.lock(this@MainActivity, source = "应用界面")
        }
    }

    /**
     * 主界面解除按钮响应：启动协程恢复底层触控
     */
    private fun unlockTouch() {
        lifecycleScope.launch {
            TouchLockManager.unlock(this@MainActivity, source = "应用界面")
        }
    }

    /**
     * 前台按键分发：拦截音量键转交物理按键解锁器处理，并根据配置放行音量调节
     */
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
                        if (AppPreferences.isAllowVolumeKeysEnabled) {
                            return super.dispatchKeyEvent(event)
                        }
                        return true // 消费按键，防止系统音量面板弹出
                    }
                    KeyEvent.ACTION_UP -> {
                        PhysicalKeyUnlockHandler.onKeyUp(keyCode)
                        if (AppPreferences.isAllowVolumeKeysEnabled) {
                            return super.dispatchKeyEvent(event)
                        }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * 执行按键与状态切换的振动反馈
     */
    private fun vibratePhone(durationMs: Long) {
        DefaultHapticFeedbackService.vibrate(this, durationMs)
    }

    /**
     * 页面重回前台：若处于锁定中重新加固置顶，并刷新各方案就绪状态
     */
    override fun onResume() {
        super.onResume()
        if (TouchLockManager.isTouchLocked) {
            TouchLockManager.reassert(this)
        }
        lifecycleScope.launch {
            TouchLockManager.checkAllReadiness(this@MainActivity, forceRequest = false)
        }
    }

    /**
     * 页面销毁：清理锁定状态回调并重置屏幕保持属性
     */
    override fun onDestroy() {
        super.onDestroy()
        TouchLockManager.onLockStateChanged = null
        if (!TouchLockManager.isTouchLocked) {
            applyScreenHoldState(false)
        }
    }
}
