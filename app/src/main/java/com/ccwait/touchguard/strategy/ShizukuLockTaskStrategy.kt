package com.ccwait.touchguard.strategy

import android.content.Context
import android.widget.Toast
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.R
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.ShizukuPermissionManager
import com.ccwait.touchguard.model.ShizukuStatus
import com.ccwait.touchguard.system.ShizukuTaskLockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shizuku 特权级状态栏与系统手势管控策略方案
 */
class ShizukuLockTaskStrategy : TouchLockStrategy {
    override val type: StrategyType = StrategyType.SHIZUKU_PINNING

    @Volatile
    private var _isLocked: Boolean = false
    override val isLocked: Boolean
        get() = _isLocked

    private val overlayStrategy = WindowOverlayStrategy()

    override val readiness: StrategyReadiness
        get() = when (ShizukuPermissionManager.status) {
            ShizukuStatus.AUTHORIZED -> StrategyReadiness.READY
            ShizukuStatus.UNAUTHORIZED -> StrategyReadiness.PERMISSION_MISSING
            ShizukuStatus.NOT_RUNNING -> StrategyReadiness.UNSUPPORTED
            ShizukuStatus.CHECKING -> StrategyReadiness.CHECKING
        }

    override fun getBadgeText(context: Context): String = when (readiness) {
        StrategyReadiness.READY -> context.getString(R.string.badge_ready_shizuku)
        StrategyReadiness.PERMISSION_MISSING -> context.getString(R.string.badge_permission_missing)
        StrategyReadiness.UNSUPPORTED -> context.getString(R.string.badge_not_running)
        StrategyReadiness.CHECKING -> context.getString(R.string.badge_checking)
    }

    override val badgeText: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "Shizuku"
            StrategyReadiness.PERMISSION_MISSING -> "未授权"
            StrategyReadiness.UNSUPPORTED -> "未运行"
            StrategyReadiness.CHECKING -> "检测中"
        }

    override fun getStatusSummary(context: Context): String = when (readiness) {
        StrategyReadiness.READY -> context.getString(R.string.status_version_beta, BuildConfig.VERSION_NAME)
        StrategyReadiness.PERMISSION_MISSING -> context.getString(R.string.status_shizuku_request_desc)
        StrategyReadiness.UNSUPPORTED -> context.getString(R.string.status_shizuku_not_running)
        StrategyReadiness.CHECKING -> context.getString(R.string.status_shizuku_checking)
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
                Toast.makeText(context, context.getString(R.string.toast_shizuku_not_running), Toast.LENGTH_SHORT).show()
                ShizukuPermissionManager.launchShizukuApp(context)
            }
            return false
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.toast_shizuku_requesting), Toast.LENGTH_SHORT).show()
        }
        val isGranted = ShizukuPermissionManager.requestPermission()
        withContext(Dispatchers.Main) {
            if (isGranted) {
                Toast.makeText(context, context.getString(R.string.toast_shizuku_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.toast_shizuku_denied), Toast.LENGTH_LONG).show()
            }
        }
        return isGranted
    }

    override suspend fun prepare(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (readiness == StrategyReadiness.READY) {
            com.ccwait.touchguard.system.OverlayPermissionHelper.ensurePermission(context)
        }
        overlayStrategy.prepare(context)
    }

    override suspend fun lock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (readiness != StrategyReadiness.READY) {
                checkReadiness(context, forceRequest = true)
            }

            // 1. 静默确保悬浮窗权限 (SYSTEM_ALERT_WINDOW) 已获得
            val hasOverlay = com.ccwait.touchguard.system.OverlayPermissionHelper.ensurePermission(context)
            if (hasOverlay) {
                AppLogManager.addLog("Shizuku", "已自动通过 Shizuku 授权悬浮窗权限")
            } else {
                AppLogManager.addLog("Shizuku", "静默授予悬浮窗权限未完成，尝试加载遮罩", isWarning = true)
            }

            // 2. 覆盖全屏透明遮罩，吞噬应用内所有触控操作
            val overlayRes = overlayStrategy.lock(context)
            if (overlayRes.isFailure) {
                val defaultOverlayErr = context.getString(R.string.toast_unknown_error)
                val err = overlayRes.exceptionOrNull()?.message ?: defaultOverlayErr
                AppLogManager.addLog("Shizuku", "全屏遮罩加载失败: $err", isWarning = true)
                // 遮罩加载失败时绝不判定锁定成功，避免无拦截空跑！
                _isLocked = false
                return@withContext Result.failure(Exception(context.getString(R.string.err_shizuku_overlay_failed, err)))
            }

            // 3. 遮罩激活成功后，再冻结顶部状态栏下拉与底层全面屏手势
            val barSuccess = ShizukuTaskLockHelper.setSystemBarsAndGesturesDisabled(context, true)
            if (barSuccess) {
                AppLogManager.addLog("Shizuku", "已冻结顶部状态栏下拉与全面屏导航手势")
            } else {
                AppLogManager.addLog("Shizuku", "状态栏/手势冻结未响应，已依靠全屏遮罩完成拦截", isWarning = true)
            }

            _isLocked = true
            AppLogManager.addLog("Shizuku", "全屏防误触触控拦截已激活")
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            // 发生异常时回滚状态
            overlayStrategy.unlock(context)
            ShizukuTaskLockHelper.setSystemBarsAndGesturesDisabled(context, false)
            _isLocked = false
            Result.failure(e)
        }
    }

    override suspend fun unlock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 移除全屏透明触控遮罩
            overlayStrategy.unlock(context)

            // 2. 恢复状态栏下拉与全面屏导航手势
            ShizukuTaskLockHelper.setSystemBarsAndGesturesDisabled(context, false)

            _isLocked = false
            AppLogManager.addLog("Shizuku", "状态栏与系统手势已完全恢复正常", isSuccess = true)
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
            ShizukuTaskLockHelper.setSystemBarsAndGesturesDisabled(context, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLocked = false
    }

    override fun reassert(context: Context) {
        if (!_isLocked) return
        try {
            // 重新冻结状态栏与全面屏手势（防止解锁锁屏后系统清理了 disable 状态）
            val barSuccess = ShizukuTaskLockHelper.setSystemBarsAndGesturesDisabled(context, true)
            if (barSuccess) {
                AppLogManager.addLog("Shizuku", "已重新加固状态栏与全面屏手势冻结")
            } else {
                AppLogManager.addLog("Shizuku", "状态栏重加固未响应", isWarning = true)
            }
            // 重新加固全屏触控遮罩置顶与焦点
            overlayStrategy.reassert(context)
        } catch (e: Exception) {
            AppLogManager.addLog("Shizuku", "重加固失败: ${e.message}", isWarning = true)
        }
    }
}
