package com.ccwait.touchguard.system

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ccwait.touchguard.AppPreferences

/**
 * 触觉震动反馈服务规范
 */
interface HapticFeedbackService {
    fun vibrate(context: Context, durationMs: Long)
}

/**
 * 系统默认触觉反馈实现，支持 Android 12+ VibratorManager 与低版本平滑兼容
 */
object DefaultHapticFeedbackService : HapticFeedbackService {
    override fun vibrate(context: Context, durationMs: Long) {
        if (!AppPreferences.isHapticFeedbackEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
