package com.ccwait.touchguard.strategy

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.notification.TouchGuardNotificationManager
import com.ccwait.touchguard.service.TouchGuardTileService
import com.ccwait.touchguard.system.DefaultHapticFeedbackService
import com.ccwait.touchguard.system.DefaultSystemPanelController
import com.ccwait.touchguard.system.HapticFeedbackService
import com.ccwait.touchguard.system.SystemPanelController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TouchLockManager : TouchLockCoordinator {
    override val currentStrategyType: StrategyType
        get() = AppPreferences.activeStrategyType

    override var isTouchLocked: Boolean by mutableStateOf(false)
        private set

    var onLockStateChanged: ((Boolean) -> Unit)? = null

    private val lockStateListeners = mutableListOf<(Boolean) -> Unit>()

    override fun addLockStateListener(listener: (Boolean) -> Unit) {
        if (!lockStateListeners.contains(listener)) {
            lockStateListeners.add(listener)
        }
    }

    override fun removeLockStateListener(listener: (Boolean) -> Unit) {
        lockStateListeners.remove(listener)
    }

    private fun notifyLockStateChanged(locked: Boolean) {
        onLockStateChanged?.invoke(locked)
        lockStateListeners.toList().forEach { it.invoke(locked) }
    }

    private val strategies = mapOf<StrategyType, TouchLockStrategy>(
        StrategyType.ROOT_EVIOCGRAB to KernelEvgrabStrategy(),
        StrategyType.ACCESSIBILITY_OVERLAY to WindowOverlayStrategy()
    )

    override val currentStrategy: TouchLockStrategy
        get() = strategies[currentStrategyType] ?: strategies[StrategyType.ROOT_EVIOCGRAB]!!

    fun getStrategy(type: StrategyType): TouchLockStrategy =
        strategies[type] ?: strategies[StrategyType.ROOT_EVIOCGRAB]!!

    override val currentReadiness: StrategyReadiness
        get() = currentStrategy.readiness

    fun getAllStrategies(): List<TouchLockStrategy> = strategies.values.toList()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        AppPreferences.init(context)
        TouchGuardNotificationManager.init(context)
        CoroutineScope(Dispatchers.IO).launch {
            checkAllReadiness(context, forceRequest = false)
        }
    }

    suspend fun checkAllReadiness(context: Context, forceRequest: Boolean = false) = withContext(Dispatchers.IO) {
        strategies.values.forEach { it.checkReadiness(context, forceRequest) }
    }

    suspend fun prepareAll(context: Context) = withContext(Dispatchers.IO) {
        strategies.values.forEach { it.prepare(context) }
    }

    override fun selectStrategy(context: Context, type: StrategyType): Boolean {
        if (isTouchLocked) return false
        AppPreferences.updateActiveStrategy(type)
        CoroutineScope(Dispatchers.IO).launch {
            currentStrategy.checkReadiness(context, forceRequest = false)
        }
        TouchGuardNotificationManager.updateNotification(context)
        TouchGuardTileService.updateTileState(context)
        return true
    }

    fun collapsePanels(context: Context) {
        DefaultSystemPanelController.collapsePanels(context)
    }

    override suspend fun lock(context: Context, source: String): Result<Unit> {
        AppPreferences.init(context)
        init(context)
        // 若非应用内触发（如通知栏/控制中心触发），自动收回下拉面板以防锁定后遮挡前台画面
        if (source != "应用界面") {
            collapsePanels(context)
        }
        // 保证前台常驻服务已启动以承载息屏监听、WakeLock 与全局按键
        com.ccwait.touchguard.service.TouchGuardForegroundService.start(context)
        // 重置物理按键恢复处理器的防误触与点击计数状态
        PhysicalKeyUnlockHandler.reset()

        val strategy = currentStrategy
        val result = strategy.lock(context)
        withContext(Dispatchers.Main) {
            if (result.isSuccess) {
                isTouchLocked = true
                GlobalScreenPolicyManager.applyPolicies(context, true)
                vibratePhone(context, 150)
                AppLogManager.addLog(
                    source,
                    "屏幕触控已锁定【${strategy.type.title}】",
                    isWarning = true
                )
                val tip = AppPreferences.unlockMechanism.promptTip
                Toast.makeText(
                    context,
                    "屏幕触控已锁定【${strategy.type.title}】！$tip",
                    Toast.LENGTH_LONG
                ).show()
                notifyLockStateChanged(true)
            } else {
                vibratePhone(context, 40)
                val errMsg = result.exceptionOrNull()?.message ?: "未知异常"
                AppLogManager.addLog(source, "触控锁定失败: $errMsg", isWarning = true)
                Toast.makeText(context, "锁定失败: $errMsg", Toast.LENGTH_LONG).show()
                strategy.checkReadiness(context, forceRequest = false)
            }
            TouchGuardNotificationManager.updateNotification(context)
            TouchGuardTileService.updateTileState(context)
        }
        return result
    }

    override suspend fun unlock(context: Context, source: String): Result<Unit> {
        if (!isTouchLocked) return Result.success(Unit)
        AppPreferences.init(context)
        init(context)
        PhysicalKeyUnlockHandler.reset()
        var lastError: Throwable? = null
        strategies.values.forEach { s ->
            if (s.isLocked) {
                val res = s.unlock(context)
                if (res.isFailure) {
                    lastError = res.exceptionOrNull()
                }
            }
        }
        withContext(Dispatchers.Main) {
            isTouchLocked = false
            GlobalScreenPolicyManager.applyPolicies(context, false)
            if (lastError == null) {
                vibratePhone(context, 80)
                AppLogManager.addLog(source, "触控已恢复正常，释放底层驱动", isSuccess = true)
                Toast.makeText(context, "✅ 触控已恢复正常！", Toast.LENGTH_SHORT).show()
                notifyLockStateChanged(false)
            } else {
                vibratePhone(context, 40)
                val errMsg = lastError.message ?: "未知异常"
                AppLogManager.addLog(source, "恢复异常: $errMsg", isWarning = true)
                Toast.makeText(context, "恢复异常: $errMsg", Toast.LENGTH_SHORT).show()
                notifyLockStateChanged(false)
            }
            TouchGuardNotificationManager.updateNotification(context)
            TouchGuardTileService.updateTileState(context)

            if (!AppPreferences.isKeepAliveEnabled) {
                com.ccwait.touchguard.service.TouchGuardForegroundService.stop(context)
            }
        }
        return if (lastError != null) Result.failure(lastError) else Result.success(Unit)
    }

    override fun releaseAll(context: Context) {
        strategies.values.forEach { it.release(context) }
        isTouchLocked = false
        GlobalScreenPolicyManager.applyPolicies(context, false)
        notifyLockStateChanged(false)
        TouchGuardNotificationManager.updateNotification(context)
        TouchGuardTileService.updateTileState(context)
    }

    fun vibratePhone(context: Context, durationMs: Long) {
        DefaultHapticFeedbackService.vibrate(context, durationMs)
    }
}
