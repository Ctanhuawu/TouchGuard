package com.ccwait.touchguard.strategy

import android.content.Context

/**
 * 触控锁定与调度协调器规范
 * 负责解耦业务调用方与底层锁定方案执行器
 */
interface TouchLockCoordinator {
    val currentStrategyType: StrategyType
    val currentStrategy: TouchLockStrategy
    val currentReadiness: StrategyReadiness
    val isTouchLocked: Boolean

    fun addLockStateListener(listener: (Boolean) -> Unit)
    fun removeLockStateListener(listener: (Boolean) -> Unit)
    fun selectStrategy(context: Context, type: StrategyType): Boolean
    suspend fun lock(context: Context, source: String = "应用界面"): Result<Unit>
    suspend fun unlock(context: Context, source: String = "应用界面"): Result<Unit>
    fun releaseAll(context: Context)
}
