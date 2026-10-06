package com.ccwait.touchguard.strategy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.R
import com.ccwait.touchguard.service.TouchGuardAccessibilityService
import com.ccwait.touchguard.system.OverlayPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    override fun getBadgeText(context: Context): String = when (readiness) {
        StrategyReadiness.READY -> context.getString(R.string.strat_accessibility_title)
        StrategyReadiness.PERMISSION_MISSING -> context.getString(R.string.badge_permission_needed)
        StrategyReadiness.UNSUPPORTED -> context.getString(R.string.badge_unsupported)
        StrategyReadiness.CHECKING -> context.getString(R.string.badge_checking)
    }

    override val badgeText: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "无障碍"
            StrategyReadiness.PERMISSION_MISSING -> "需授权"
            StrategyReadiness.UNSUPPORTED -> "不支持"
            StrategyReadiness.CHECKING -> "检测中"
        }

    override fun getStatusSummary(context: Context): String = when (readiness) {
        StrategyReadiness.READY -> context.getString(R.string.status_version_beta, BuildConfig.VERSION_NAME)
        StrategyReadiness.PERMISSION_MISSING -> context.getString(R.string.status_acc_request_desc)
        StrategyReadiness.UNSUPPORTED -> context.getString(R.string.status_acc_unsupported)
        StrategyReadiness.CHECKING -> context.getString(R.string.status_acc_checking)
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
                    context.getString(R.string.toast_acc_service_guide),
                    Toast.LENGTH_LONG
                ).show()
            }
            return false
        } catch (_: Exception) {
            // 降级引导悬浮窗权限
            if (!com.ccwait.touchguard.system.OverlayPermissionHelper.hasPermission(context)) {
                com.ccwait.touchguard.system.OverlayPermissionHelper.requestPermission(context)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_overlay_perm_guide), Toast.LENGTH_SHORT).show()
                }
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
            val hasOverlayPerm = com.ccwait.touchguard.system.OverlayPermissionHelper.hasPermission(context)

            if (accService == null && !hasOverlayPerm) {
                return@withContext Result.failure(Exception(context.getString(R.string.err_overlay_perm_required)))
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

            @Suppress("DEPRECATION")
            if (AppPreferences.isLockScreenOverlayEnabled) {
                flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
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
                    view.visibility = View.GONE
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
        if (view != null) {
            try {
                view.visibility = View.GONE
            } catch (_: Exception) {}
            if (wm != null) {
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
    }

    override fun reassert(context: Context) {
        val isSystemLocked = TouchLockManager.isTouchLocked
        if (!_isLocked && !isSystemLocked) return
        val appContext = context.applicationContext
        val wm = windowManager ?: (appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return
        val currentView = overlayView

        val action = Runnable {
            if (currentView == null || !currentView.isAttachedToWindow || !_isLocked) {
                // 视图未附加或被系统回收，重新执行 lock
                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                    lock(context)
                }
                return@Runnable
            }

            try {
                currentView.visibility = View.VISIBLE
                currentView.bringToFront()
                currentView.requestFocus()
                (currentView as? TouchLockOverlayView)?.applyImmersiveMode()
                currentView.layoutParams?.let { lp ->
                    if (lp is WindowManager.LayoutParams) {
                        @Suppress("DEPRECATION")
                        if (AppPreferences.isLockScreenOverlayEnabled) {
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        } else {
                            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED.inv()
                        }
                    }
                    wm.updateViewLayout(currentView, lp)
                }
                com.ccwait.touchguard.model.AppLogManager.addLog("遮罩", "全屏触控拦截遮罩已加固并重置焦点")
            } catch (e: Exception) {
                com.ccwait.touchguard.model.AppLogManager.addLog("遮罩", "遮罩刷新异常，重新加载: ${e.message}", isWarning = true)
                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                    lock(context)
                }
            }
        }

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            action.run()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post(action)
        }
    }
}
