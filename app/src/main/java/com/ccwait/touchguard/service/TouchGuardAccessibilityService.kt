package com.ccwait.touchguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.strategy.TouchLockManager

/**
 * TouchGuard 系统无障碍服务
 *
 * 核心能力：
 * 1. 具备系统级最高渲染层级（TYPE_ACCESSIBILITY_OVERLAY - Layer 31），
 *    超越状态栏（Layer 24）与导航栏（Layer 23），彻底解决免 Root 悬浮窗方案
 *    无法遮挡标题栏/状态栏下拉与全面屏边缘手势的问题。
 * 2. 具备 flagRequestFilterKeyEvents，直接在系统层级监听并过滤物理按键，
 *    无论前台为何种应用均能稳定触发应急解锁。
 */
class TouchGuardAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        android.util.Log.d("TouchGuard", "TouchGuardAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 无需捕获窗口内容，纯净拦截
    }

    override fun onInterrupt() {
        android.util.Log.d("TouchGuard", "TouchGuardAccessibilityService interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        android.util.Log.d("TouchGuard", "TouchGuardAccessibilityService destroyed")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (TouchLockManager.isTouchLocked) {
            val keyCode = event.keyCode
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            PhysicalKeyUnlockHandler.onKeyDown(this, keyCode)
                        }
                        if (AppPreferences.isAllowVolumeKeysEnabled) {
                            return false
                        }
                        return true
                    }
                    KeyEvent.ACTION_UP -> {
                        PhysicalKeyUnlockHandler.onKeyUp(keyCode)
                        if (AppPreferences.isAllowVolumeKeysEnabled) {
                            return false
                        }
                        return true
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true // 拦截全局返回键
            }
        }
        return super.onKeyEvent(event)
    }

    /**
     * 免 Root 核心：通过无障碍服务的系统权限收起通知栏与控制中心
     */
    fun dismissNotificationShade(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val handled = performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                android.util.Log.d("TouchGuard", "dismissNotificationShade: GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE handled=$handled")
                return handled
            } catch (e: Exception) {
                android.util.Log.w("TouchGuard", "GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE failed", e)
            }
        }
        return false
    }

    companion object {
        @Volatile
        var instance: TouchGuardAccessibilityService? = null
            private set

        val isEnabled: Boolean
            get() = instance != null

        fun collapsePanels(): Boolean {
            val service = instance ?: return false
            return service.dismissNotificationShade()
        }

        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            if (isEnabled) return true
            return try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServices)
                val expected = "${context.packageName}/${TouchGuardAccessibilityService::class.java.name}"
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(expected, ignoreCase = true) || componentName.contains(context.packageName)) {
                        return true
                    }
                }
                false
            } catch (_: Exception) {
                false
            }
        }
    }
}
