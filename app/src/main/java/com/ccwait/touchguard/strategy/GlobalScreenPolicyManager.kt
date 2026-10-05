package com.ccwait.touchguard.strategy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.model.ScreenOrientationLock

/**
 * 屏幕 WakeLock 控制器实现，负责安全持有与释放 SCREEN_BRIGHT_WAKE_LOCK
 */
class DefaultWakeLockController : WakeLockController {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun acquire(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (wakeLock == null || wakeLock?.isHeld == false) {
                @Suppress("DEPRECATION")
                wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                    "TouchGuard:GlobalKeepScreenOn"
                ).apply {
                    setReferenceCounted(false)
                    acquire(12 * 60 * 60 * 1000L) // 最大12小时保护超时
                }
                android.util.Log.d("WakeLockController", "Global WakeLock acquired (SCREEN_BRIGHT_WAKE_LOCK)")
            }
        } catch (e: Exception) {
            android.util.Log.w("WakeLockController", "Failed to acquire global WakeLock", e)
        }
    }

    override fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                android.util.Log.d("WakeLockController", "Global WakeLock released")
            }
        } catch (_: Exception) {}
        wakeLock = null
    }
}

/**
 * 全局屏幕保持与指示悬浮窗控制器实现
 */
class DefaultScreenOverlayController : ScreenOverlayController {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun attach(context: Context) {
        ensureOverlayPermission(context)
        if (!Settings.canDrawOverlays(context)) {
            android.util.Log.w("ScreenOverlayController", "No overlay permission, skip global window policy")
            return
        }

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val showPill = AppPreferences.isFloatingIndicatorEnabled
            val orientation = AppPreferences.screenOrientationLock
            val lockBrightness = AppPreferences.isBrightnessLockEnabled
            val keepScreenOn = AppPreferences.isKeepScreenOnEnabled
            val density = context.resources.displayMetrics.density

            val container = FrameLayout(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = false
                isFocusable = false
            }

            if (showPill) {
                val promptPill = TextView(context).apply {
                    val tip = context.getString(AppPreferences.unlockMechanism.promptTipRes)
                    text = "🔒 " + context.getString(R.string.capsule_press_tip, tip)
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    val padH = (16 * density).toInt()
                    val padV = (8 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    background = GradientDrawable().apply {
                        setColor(Color.argb(220, 24, 24, 27))
                        cornerRadius = 24 * density
                        setStroke((0.8f * density).toInt(), Color.argb(50, 255, 255, 255))
                    }
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        topMargin = (50 * density).toInt()
                    }
                    layoutParams = params
                }
                container.addView(promptPill)
            }

            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            if (keepScreenOn) {
                flags = flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
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
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (orientation != ScreenOrientationLock.FOLLOW_SYSTEM) {
                    screenOrientation = orientation.orientationValue
                }
                if (lockBrightness) {
                    try {
                        val curBrightness = Settings.System.getInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS
                        ) / 255f
                        screenBrightness = curBrightness.coerceIn(0.01f, 1.0f)
                    } catch (_: Exception) {}
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            detach()
            wm.addView(container, layoutParams)
            overlayView = container
            android.util.Log.d("ScreenOverlayController", "Global policy overlay attached (pill=$showPill, orientation=${orientation.title})")
        } catch (e: Exception) {
            android.util.Log.e("ScreenOverlayController", "Failed to attach global policy overlay", e)
        }
    }

    override fun detach() {
        try {
            overlayView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            }
        } catch (_: Exception) {}
        overlayView = null
    }

    private fun ensureOverlayPermission(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            try {
                Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                ).waitFor()
            } catch (_: Exception) {}
        }
    }
}

/**
 * 全局屏幕与画面保持策略管理器 (GlobalScreenPolicyManager)
 * 实现 ScreenPolicyManager 规范，门面整合 WakeLock 与悬浮保持控制器
 */
object GlobalScreenPolicyManager : ScreenPolicyManager {
    private val wakeLockController: WakeLockController = DefaultWakeLockController()
    private val overlayController: ScreenOverlayController = DefaultScreenOverlayController()

    @Synchronized
    override fun applyPolicies(context: Context, isLocked: Boolean) {
        val appContext = context.applicationContext
        AppPreferences.init(appContext)

        if (isLocked) {
            if (AppPreferences.isKeepScreenOnEnabled) {
                wakeLockController.acquire(appContext)
            } else {
                wakeLockController.release()
            }
            overlayController.attach(appContext)
        } else {
            wakeLockController.release()
            overlayController.detach()
        }
    }
}
