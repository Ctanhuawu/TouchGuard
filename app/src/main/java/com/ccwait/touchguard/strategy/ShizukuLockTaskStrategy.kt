package com.ccwait.touchguard.strategy

import android.content.Context
import android.widget.Toast
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.ShizukuPermissionManager
import com.ccwait.touchguard.model.ShizukuStatus
import com.ccwait.touchguard.system.ShizukuTaskLockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shizuku 屏幕固定 (LockTask) 策略方案
 *
 * 核心机制：
 * 1. 利用 Shizuku (UID 2000 / MANAGE_ACTIVITY_TASKS) 特权，
 *    通过 IActivityTaskManager.startSystemLockTaskMode 自动将当前前台应用固定在屏幕上；
 * 2. 系统底层自动封死顶部状态栏下拉与底部全面屏手势（小白条退出）；
 * 3. 辅以 WindowOverlayStrategy (TYPE_ACCESSIBILITY_OVERLAY) 吞噬屏幕内所有点击滑动，
 *    实现“系统底层防逃逸 + 视图图层防乱点”的双重绝对锁定；
 * 4. 解锁时调用 stopSystemLockTaskMode 自动平滑退出固定模式。
 */
class ShizukuLockTaskStrategy : TouchLockStrategy {
    override val type: StrategyType = StrategyType.SHIZUKU_PINNING

    @Volatile
    private var _isLocked: Boolean = false
    override val isLocked: Boolean
        get() = _isLocked

    private val overlayStrategy = WindowOverlayStrategy()
    private var pinnedTaskId: Int? = null

    override val readiness: StrategyReadiness
        get() = when (ShizukuPermissionManager.status) {
            ShizukuStatus.AUTHORIZED -> StrategyReadiness.READY
            ShizukuStatus.UNAUTHORIZED -> StrategyReadiness.PERMISSION_MISSING
            ShizukuStatus.NOT_RUNNING -> StrategyReadiness.UNSUPPORTED
            ShizukuStatus.CHECKING -> StrategyReadiness.CHECKING
        }

    override val badgeText: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "Shizuku"
            StrategyReadiness.PERMISSION_MISSING -> "未授权"
            StrategyReadiness.UNSUPPORTED -> "未运行"
            StrategyReadiness.CHECKING -> "检测中"
        }

    override val statusSummary: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "v${BuildConfig.VERSION_NAME} · Beta版"
            StrategyReadiness.PERMISSION_MISSING -> "点击申请 Shizuku 授权 · 需启动配对"
            StrategyReadiness.UNSUPPORTED -> "未检测到 Shizuku 服务 · 点击打开/下载"
            StrategyReadiness.CHECKING -> "正在检测 Shizuku 授权..."
        }

    override suspend fun checkReadiness(context: Context, forceRequest: Boolean): StrategyReadiness {
        ShizukuPermissionManager.checkPermission(forceRequest)
        return readiness
    }

    override suspend fun requestPermission(context: Context): Boolean {
        if (!ShizukuPermissionManager.isRunning()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Shizuku 服务未运行，正在尝试启动 Shizuku...", Toast.LENGTH_SHORT).show()
                ShizukuPermissionManager.launchShizukuApp(context)
            }
            return false
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "正在向 Shizuku 请求授权，请在弹窗中允许...", Toast.LENGTH_SHORT).show()
        }
        val isGranted = ShizukuPermissionManager.requestPermission()
        withContext(Dispatchers.Main) {
            if (isGranted) {
                Toast.makeText(context, "✅ Shizuku 授权成功！系统固定策略已就绪", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "❌ 未获得 Shizuku 授权，请在 Shizuku 应用中允许", Toast.LENGTH_LONG).show()
            }
        }
        return isGranted
    }

    override suspend fun prepare(context: Context): Boolean {
        return overlayStrategy.prepare(context)
    }

    override suspend fun lock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (readiness != StrategyReadiness.READY) {
                checkReadiness(context, forceRequest = true)
            }

            // 1. 获取当前正在前台播放或运行的目标应用 Task ID
            val targetTaskId = ShizukuTaskLockHelper.getForegroundTaskId(context)
            pinnedTaskId = targetTaskId

            // 2. 触发系统级屏幕固定（打入 LockTask 模式，封死状态栏与手势）
            var pinSuccess = false
            if (targetTaskId != null && targetTaskId > 0) {
                pinSuccess = ShizukuTaskLockHelper.startLockTask(targetTaskId)
                if (pinSuccess) {
                    AppLogManager.addLog("Shizuku", "已自动固定当前任务 (TaskID: $targetTaskId)，手势退出已锁死")
                } else {
                    AppLogManager.addLog("Shizuku", "任务固定请求未响应，降级使用状态栏与视图遮罩", isWarning = true)
                }
            } else {
                AppLogManager.addLog("Shizuku", "未捕获到前台任务 ID，降级启用视图遮罩", isWarning = true)
            }

            // 3. 辅助冻结状态栏下拉（双保险）
            ShizukuTaskLockHelper.setStatusBarExpandDisabled(context, true)

            // 4. 覆盖全屏透明无障碍遮罩，吞噬应用内所有触控操作
            val overlayRes = overlayStrategy.lock(context)
            if (overlayRes.isFailure) {
                // 如果遮罩加载失败，记录日志但不中断主流程
                AppLogManager.addLog("Shizuku", "悬浮视图加载提示: ${overlayRes.exceptionOrNull()?.message}", isWarning = true)
            }

            _isLocked = true
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _isLocked = false
            Result.failure(e)
        }
    }

    override suspend fun unlock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 移除全屏透明触控遮罩
            overlayStrategy.unlock(context)

            // 2. 恢复状态栏下拉
            ShizukuTaskLockHelper.setStatusBarExpandDisabled(context, false)

            // 3. 退出系统屏幕固定模式
            ShizukuTaskLockHelper.stopLockTask()
            pinnedTaskId = null

            _isLocked = false
            AppLogManager.addLog("Shizuku", "屏幕固定模式已安全退出，手势与多任务已恢复正常", isSuccess = true)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _isLocked = false
            Result.failure(e)
        }
    }

    override fun release(context: Context) {
        try {
            overlayStrategy.release(context)
            ShizukuTaskLockHelper.setStatusBarExpandDisabled(context, false)
            ShizukuTaskLockHelper.stopLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pinnedTaskId = null
        _isLocked = false
    }
}
