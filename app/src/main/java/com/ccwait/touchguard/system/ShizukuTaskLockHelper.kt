package com.ccwait.touchguard.system

import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

/**
 * Shizuku 特权级状态栏与系统手势管控助手
 *
 * 利用 com.android.shell (UID 2000) 权限通过 IStatusBarService Binder 接口：
 * 1. 禁用状态栏与控制中心下拉；
 * 2. 禁用全面屏手势（返回、桌面、多任务）与物理导航键；
 * 3. 完美保留时间、电量、WiFi等状态栏系统信息显示；
 * 4. 自动静默授予悬浮窗权限 (SYSTEM_ALERT_WINDOW)。
 */
object ShizukuTaskLockHelper {

    private const val TAG = "TouchGuard"

    // 状态栏锁定标志常量 (来自 AOSP StatusBarManager)
    private const val DISABLE_EXPAND = 0x00010000
    private const val DISABLE_HOME = 0x00200000
    private const val DISABLE_BACK = 0x00400000
    private const val DISABLE_RECENT = 0x01000000
    private const val DISABLE_SEARCH = 0x02000000

    private const val DISABLE2_QUICK_SETTINGS = 0x00000001
    private const val DISABLE2_NOTIFICATION_SHADE = 0x00000004

    // 锁定时禁用的标志组合（禁用下拉、桌面手势、返回手势、多任务手势，但不禁用系统信息与时钟）
    private const val LOCK_FLAG1 = DISABLE_EXPAND or DISABLE_HOME or DISABLE_RECENT or DISABLE_BACK or DISABLE_SEARCH
    private const val LOCK_FLAG2 = DISABLE2_QUICK_SETTINGS or DISABLE2_NOTIFICATION_SHADE

    private val statusBarToken: IBinder = Binder()

    @Volatile
    private var cachedStatusBarService: Any? = null
    @Volatile
    private var cachedDisableMethod: Method? = null
    @Volatile
    private var cachedDisable2Method: Method? = null
    @Volatile
    private var isMethodsCached = false

    private fun getStatusBarService(): Any? {
        val current = cachedStatusBarService
        if (current != null) return current

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("")
            }
        }

        return try {
            val binderRaw = SystemServiceHelper.getSystemService("statusbar") ?: return null
            val wrappedBinder = ShizukuBinderWrapper(binderRaw)
            val stubClass = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val service = asInterface.invoke(null, wrappedBinder)
            cachedStatusBarService = service
            service
        } catch (e: Throwable) {
            Log.e(TAG, "getStatusBarService failed", e)
            null
        }
    }

    private fun ensureMethods(service: Any) {
        if (isMethodsCached) return
        try {
            val methods = service.javaClass.methods
            cachedDisableMethod = methods.firstOrNull { it.name == "disable" && it.parameterTypes.size == 3 }
            cachedDisable2Method = methods.firstOrNull { it.name == "disable2" && it.parameterTypes.size == 3 }
            isMethodsCached = true
        } catch (e: Throwable) {
            Log.e(TAG, "Cache disable methods failed", e)
        }
    }

    /**
     * 通过 IStatusBarService 精确冻结/恢复状态栏下拉与底层系统手势
     *
     * @param disabled true: 禁用下拉、返回手势、桌面手势、多任务手势（保留时间、电量等状态栏系统信息显示）
     *                 false: 完全恢复所有状态栏下拉与手势操作
     */
    fun setSystemBarsAndGesturesDisabled(context: Context, disabled: Boolean): Boolean {
        for (attempt in 1..2) {
            try {
                val service = getStatusBarService() ?: return false
                ensureMethods(service)

                val flag1 = if (disabled) LOCK_FLAG1 else 0
                val flag2 = if (disabled) LOCK_FLAG2 else 0

                cachedDisableMethod?.invoke(service, flag1, statusBarToken, context.packageName)
                cachedDisable2Method?.invoke(service, flag2, statusBarToken, context.packageName)

                Log.d(TAG, "setSystemBarsAndGesturesDisabled: disabled=$disabled, flag1=$flag1, flag2=$flag2")
                return true
            } catch (e: Throwable) {
                Log.e(TAG, "setSystemBarsAndGesturesDisabled failed (attempt $attempt)", e)
                // 异常时重置缓存，以便重试时重新获取服务引用
                cachedStatusBarService = null
                isMethodsCached = false
            }
        }
        return false
    }

    /**
     * 兼容接口：通过 IStatusBarService 冻结状态栏下拉
     */
    fun setStatusBarExpandDisabled(context: Context, disabled: Boolean): Boolean {
        return setSystemBarsAndGesturesDisabled(context, disabled)
    }

    @Volatile
    private var cachedNewProcessMethod: Method? = null
    @Volatile
    private var isNewProcessCached = false

    private fun getNewProcessMethod(): Method? {
        if (isNewProcessCached) return cachedNewProcessMethod
        return try {
            val method = Shizuku::class.java.declaredMethods.firstOrNull {
                it.name == "newProcess" && it.parameterTypes.size == 3
            }?.apply { isAccessible = true }
            cachedNewProcessMethod = method
            isNewProcessCached = true
            method
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to resolve Shizuku.newProcess", e)
            null
        }
    }

    /**
     * 通过 Shizuku 静默为 TouchGuard 授予悬浮窗权限 (SYSTEM_ALERT_WINDOW)
     * 同步注入 HyperOS / MIUI 锁屏显示(10008)与悬浮窗(10021)专属权限
     */
    fun ensureOverlayPermission(context: Context): Boolean {
        if (Settings.canDrawOverlays(context)) return true
        return try {
            val method = getNewProcessMethod() ?: return false
            val cmds = arrayOf(
                "sh", "-c",
                "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow; " +
                "appops set ${context.packageName} 10021 allow; " +
                "appops set ${context.packageName} 10020 allow; " +
                "appops set ${context.packageName} 10008 allow"
            )
            val p = method.invoke(null, cmds, null, null) as? Process
            p?.waitFor()
            Settings.canDrawOverlays(context)
        } catch (e: Throwable) {
            Log.e(TAG, "ensureOverlayPermission failed", e)
            false
        }
    }
}
