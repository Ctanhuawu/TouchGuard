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
            StrategyReadiness.READY -> {
                if (TouchGuardAccessibilityService.isEnabled) {
                    "v${BuildConfig.VERSION_NAME} · 无障碍顶层拦截 (状态栏与手势已全覆盖)"
                } else {
                    "v${BuildConfig.VERSION_NAME} · 悬浮窗模式 (开启无障碍服务防状态栏下拉)"
                }
            }
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
            val (wm, windowType) = if (accService != null) {
                val serviceWm = accService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                serviceWm to WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                val sysWm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                sysWm to type
            }
            windowManager = wm

            val overlay = TouchLockOverlayView(context)

            @Suppress("DEPRECATION")
            val flags = WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

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
            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            }
            overlayView = null
            _isLocked = false
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
            _isLocked = false
            Result.failure(e)
        }
    }

    override fun release(context: Context) {
        try {
            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        overlayView = null
        _isLocked = false
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

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 排除两侧边缘与底部的系统手势拦截，令全面屏手势全部落入当前 View 吞噬
                val density = context.resources.displayMetrics.density
                val exclusionWidth = (60 * density).toInt().coerceAtMost(width / 4)
                val exclusionBottomHeight = (60 * density).toInt().coerceAtMost(height / 4)
                val leftRect = Rect(0, 0, exclusionWidth, height)
                val rightRect = Rect(width - exclusionWidth, 0, width, height)
                val bottomRect = Rect(0, height - exclusionBottomHeight, width, height)
                val topRect = Rect(0, 0, width, (60 * density).toInt())
                systemGestureExclusionRects = listOf(leftRect, rightRect, bottomRect, topRect)
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
        override fun onTouchEvent(event: MotionEvent): Boolean = true

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (TouchLockManager.isTouchLocked) {
                if (ev.y < 120 * context.resources.displayMetrics.density && ev.action == MotionEvent.ACTION_DOWN) {
                    TouchLockManager.collapsePanels(context)
                }
                return true
            }
            return super.dispatchTouchEvent(ev)
        }
    }
}
