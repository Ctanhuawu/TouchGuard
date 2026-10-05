package com.ccwait.touchguard.strategy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.service.TouchGuardAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WindowOverlayStrategy : TouchLockStrategy {
    override val type: StrategyType = StrategyType.ACCESSIBILITY_OVERLAY

    @Volatile
    private var _isLocked: Boolean = false
    override val isLocked: Boolean
        get() = _isLocked

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override var readiness by mutableStateOf(StrategyReadiness.CHECKING)
        private set

    override val badgeText: String
        get() = when (readiness) {
            StrategyReadiness.READY -> {
                if (TouchGuardAccessibilityService.isEnabled) "无障碍增强" else "免 Root"
            }
            StrategyReadiness.PERMISSION_MISSING -> "需授权"
            StrategyReadiness.UNSUPPORTED -> "不支持"
            StrategyReadiness.CHECKING -> "检测中"
        }

    override val statusSummary: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "v${BuildConfig.VERSION_NAME} · Beta版"
            StrategyReadiness.PERMISSION_MISSING -> "点击开启无障碍服务或悬浮窗权限"
            StrategyReadiness.UNSUPPORTED -> "系统不支持悬浮窗或无障碍拦截"
            StrategyReadiness.CHECKING -> "正在检测权限..."
        }

    override suspend fun checkReadiness(context: Context, forceRequest: Boolean): StrategyReadiness {
        val hasAcc = TouchGuardAccessibilityService.isEnabled || TouchGuardAccessibilityService.isAccessibilitySettingsEnabled(context)
        val hasOverlay = Settings.canDrawOverlays(context)
        readiness = if (hasAcc || hasOverlay) StrategyReadiness.READY else StrategyReadiness.PERMISSION_MISSING
        return readiness
    }

    override suspend fun requestPermission(context: Context): Boolean {
        if (TouchGuardAccessibilityService.isEnabled) {
            readiness = StrategyReadiness.READY
            return true
        }

        // 优先引导开启无障碍服务（具备最高层级 TYPE_ACCESSIBILITY_OVERLAY 突破状态栏与手势）
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "请在「已下载的服务」中开启 TouchGuard 无障碍服务（完美覆盖状态栏与全面屏手势）",
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        } catch (_: Exception) {
            // 降级引导悬浮窗权限
            if (!Settings.canDrawOverlays(context)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "请在设置中开启「显示在其他应用上层」权限", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {}
            }
        }
        return false
    }

    override suspend fun prepare(context: Context): Boolean {
        checkReadiness(context)
        return readiness == StrategyReadiness.READY
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override suspend fun lock(context: Context): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val accService = TouchGuardAccessibilityService.instance
            val hasOverlayPerm = Settings.canDrawOverlays(context)

            if (accService == null && !hasOverlayPerm) {
                return@withContext Result.failure(Exception("请开启 TouchGuard 无障碍服务或授予悬浮窗权限"))
            }

            if (overlayView != null) {
                unlock(context)
            }

            // 确定 WindowManager 与窗口层级类型
            // 若无障碍服务已开启，使用 TYPE_ACCESSIBILITY_OVERLAY（Layer 31），直接超越状态栏（Layer 24）与导航栏（Layer 23）
            val appContext = context.applicationContext
            val (wm, windowType) = if (accService != null) {
                val serviceWm = accService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                serviceWm to WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                val sysWm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                sysWm to type
            }
            windowManager = wm

            // 防重入：如果已有旧视图残留，强制先清空
            overlayView?.let { oldView ->
                try { wm.removeViewImmediate(oldView) } catch (_: Exception) {
                    try { wm.removeView(oldView) } catch (_: Exception) {}
                }
            }
            overlayView = null

            val overlay = TouchLockOverlayView(appContext)

            @Suppress("DEPRECATION")
            var flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

            @Suppress("DEPRECATION")
            if (AppPreferences.isHideSystemBarsEnabled) {
                flags = flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    setFitInsetsTypes(0)
                    setFitInsetsSides(0)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            wm.addView(overlay, layoutParams)
            overlay.requestFocus()
            overlayView = overlay
            _isLocked = true
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _isLocked = false
            Result.failure(e)
        }
    }

    override suspend fun unlock(context: Context): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val wm = windowManager ?: (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            overlayView?.let { view ->
                try {
                    wm?.removeViewImmediate(view)
                } catch (_: Exception) {
                    try {
                        wm?.removeView(view)
                    } catch (_: Exception) {}
                }
            }
            overlayView = null
            windowManager = null
            _isLocked = false
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
            windowManager = null
            _isLocked = false
            Result.failure(e)
        }
    }

    override fun release(context: Context) {
        val wm = windowManager ?: (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        val view = overlayView
        overlayView = null
        windowManager = null
        _isLocked = false
        if (view != null && wm != null) {
            try {
                if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                    try { wm.removeViewImmediate(view) } catch (_: Exception) { wm.removeView(view) }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try { wm.removeViewImmediate(view) } catch (_: Exception) { try { wm.removeView(view) } catch (_: Exception) {} }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private class TouchLockOverlayView(context: Context) : FrameLayout(context) {
        init {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            fitsSystemWindows = false
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            applyImmersiveMode()
        }

        private fun applyImmersiveMode() {
            if (!AppPreferences.isHideSystemBarsEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    windowInsetsController?.show(WindowInsets.Type.systemBars())
                }
                @Suppress("DEPRECATION")
                systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            @Suppress("DEPRECATION")
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }

        override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsets.CONSUMED
            } else {
                @Suppress("DEPRECATION")
                insets.consumeSystemWindowInsets()
            }
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 排除边缘系统手势拦截（单侧高度限制在 Android 规范的 200dp 内，防止系统丢弃请求）
                val density = context.resources.displayMetrics.density
                val maxExclusionPx = (200 * density).toInt()
                val edgeWidth = (60 * density).toInt().coerceAtMost(width / 4)
                val edgeHeight = (60 * density).toInt().coerceAtMost(height / 4)

                val leftRect = Rect(0, (height - maxExclusionPx).coerceAtLeast(0) / 2, edgeWidth, ((height + maxExclusionPx) / 2).coerceAtMost(height))
                val rightRect = Rect(width - edgeWidth, (height - maxExclusionPx).coerceAtLeast(0) / 2, width, ((height + maxExclusionPx) / 2).coerceAtMost(height))
                val topRect = Rect(0, 0, width, edgeHeight)
                val bottomRect = Rect(0, height - edgeHeight, width, height)
                try {
                    systemGestureExclusionRects = listOf(leftRect, rightRect, topRect, bottomRect)
                } catch (_: Exception) {}
            }
        }

        override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
            super.onWindowFocusChanged(hasWindowFocus)
            if (!hasWindowFocus && TouchLockManager.isTouchLocked) {
                postDelayed({
                    if (TouchLockManager.isTouchLocked) {
                        TouchLockManager.collapsePanels(context)
                        applyImmersiveMode()
                        requestFocus()
                    }
                }, 50)
            }
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (!TouchLockManager.isTouchLocked) return super.dispatchKeyEvent(event)

            val keyCode = event.keyCode
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            PhysicalKeyUnlockHandler.onKeyDown(context, keyCode)
                        }
                        return true
                    }
                    KeyEvent.ACTION_UP -> {
                        PhysicalKeyUnlockHandler.onKeyUp(keyCode)
                        return true
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean = TouchLockManager.isTouchLocked

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (!TouchLockManager.isTouchLocked) {
                return false
            }
            val density = context.resources.displayMetrics.density
            if ((ev.y < 120 * density || ev.y > height - 100 * density) && ev.action == MotionEvent.ACTION_DOWN) {
                TouchLockManager.collapsePanels(context)
            }
            return true
        }
    }
}
