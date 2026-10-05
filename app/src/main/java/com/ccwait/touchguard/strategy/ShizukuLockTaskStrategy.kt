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
 * Shizuku 特权级状态栏与系统手势管控策略方案
 *
 * 核心机制：
 * 1. 利用 Shizuku (UID 2000 shell) 特权，通过 IStatusBarService 精确禁用：
 *    - 顶部状态栏与控制中心下拉 (DISABLE_EXPAND / DISABLE2_QUICK_SETTINGS)
 *    - 全面屏导航手势（返回、桌面、多任务 DISABLE_BACK / DISABLE_HOME / DISABLE_RECENT）
 *    - 完美保留时间、电量、WiFi等状态栏系统信息显示；
 * 2. 通过 Shizuku 自动静默授权全屏悬浮窗权限 (SYSTEM_ALERT_WINDOW)，
 *    配合 WindowOverlayStrategy 吞噬屏幕内所有触控操作；
 * 3. 规避系统原生 LockTask 强制隐藏状态栏和切断音量键的硬性缺陷，
 *    物理按键（如双击音量下键）应急解锁 100% 灵敏可用。
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

    override suspend fun prepare(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (readiness == StrategyReadiness.READY) {
            ShizukuTaskLockHelper.ensureOverlayPermission(context)
        }
        overlayStrategy.prepare(context)
    }

    override suspend fun lock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (readiness != StrategyReadiness.READY) {
                checkReadiness(context, forceRequest = true)
            }

            // 1. 静默确保悬浮窗权限 (SYSTEM_ALERT_WINDOW) 已获得
            val hasOverlay = ShizukuTaskLockHelper.ensureOverlayPermission(context)
            if (hasOverlay) {
                AppLogManager.addLog("Shizuku", "已自动通过 Shizuku 授权悬浮窗权限")
            }

            // 2. 通过 IStatusBarService 冻结状态栏下拉与底层手势（禁用下拉、返回、多任务、桌面手势，保留时间与电量显示）
            val barSuccess = ShizukuTaskLockHelper.setSystemBarsAndGesturesDisabled(context, true)
            if (barSuccess) {
                AppLogManager.addLog("Shizuku", "已冻结顶部状态栏下拉与全面屏导航手势")
            } else {
                AppLogManager.addLog("Shizuku", "状态栏/手势冻结未响应，降级使用常规遮罩", isWarning = true)
            }

            // 3. 覆盖全屏透明遮罩，吞噬应用内所有触控操作
            val overlayRes = overlayStrategy.lock(context)
            if (overlayRes.isFailure) {
                AppLogManager.addLog("Shizuku", "全屏遮罩加载提示: ${overlayRes.exceptionOrNull()?.message}", isWarning = true)
            } else {
                AppLogManager.addLog("Shizuku", "全屏防误触触控拦截已激活")
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
