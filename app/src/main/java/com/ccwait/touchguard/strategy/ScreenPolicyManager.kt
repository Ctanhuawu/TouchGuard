package com.ccwait.touchguard.strategy

import android.content.Context

/**
 * 屏幕保持策略管理器规范
 */
interface ScreenPolicyManager {
    fun applyPolicies(context: Context, isLocked: Boolean)
}

/**
 * 屏幕常亮 WakeLock 锁管理器规范
 */
interface WakeLockController {
    fun acquire(context: Context)
    fun release()
}

/**
 * 全局屏幕保持与指示悬浮窗控制器规范
 */
interface ScreenOverlayController {
    fun attach(context: Context)
    fun detach()
}
