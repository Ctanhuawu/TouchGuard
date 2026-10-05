package com.ccwait.touchguard.strategy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.BuildConfig
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
            StrategyReadiness.READY -> "免 Root"
            StrategyReadiness.PERMISSION_MISSING -> "需授权"
            StrategyReadiness.UNSUPPORTED -> "不支持"
            StrategyReadiness.CHECKING -> "检测中"
        }

    override val statusSummary: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "v${BuildConfig.VERSION_NAME} · Beta版"
            StrategyReadiness.PERMISSION_MISSING -> "点击前往系统设置开启悬浮窗"
            StrategyReadiness.UNSUPPORTED -> "系统不支持悬浮窗拦截"
            StrategyReadiness.CHECKING -> "正在检测悬浮窗权限..."
        }

    override suspend fun checkReadiness(context: Context, forceRequest: Boolean): StrategyReadiness {
        val hasPerm = Settings.canDrawOverlays(context)
        readiness = if (hasPerm) StrategyReadiness.READY else StrategyReadiness.PERMISSION_MISSING
        return readiness
    }

    override suspend fun requestPermission(context: Context): Boolean {
        if (Settings.canDrawOverlays(context)) {
            readiness = StrategyReadiness.READY
            return true
        }
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "请在设置中开启「显示在其他应用上层」权限", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {}
        return false
    }

    override suspend fun prepare(context: Context): Boolean {
        checkReadiness(context)
        return readiness == StrategyReadiness.READY
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override suspend fun lock(context: Context): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (!Settings.canDrawOverlays(context)) {
                return@withContext Result.failure(Exception("请授予 TouchGuard「悬浮窗/显示在其他应用上层」权限"))
            }

            if (overlayView != null) {
                unlock(context)
            }

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val frameLayout = FrameLayout(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                isFocusable = false
                setOnTouchListener { _, _ -> true } // 吞噬并消费一切触控手势
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            wm.addView(frameLayout, layoutParams)
            overlayView = frameLayout
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
}
