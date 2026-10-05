package com.ccwait.touchguard.system

import android.content.ComponentName
import android.content.Context
import android.os.Binder
import android.os.IBinder
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Shizuku 底层任务锁定与状态栏管控助手
 * 利用 com.android.shell (UID 2000) 权限通过 Binder 调用 ActivityTaskManager 特权接口
 */
object ShizukuTaskLockHelper {

    private fun getAtmService(): Any? {
        val binderRaw = try {
            SystemServiceHelper.getSystemService("activity_task")
                ?: SystemServiceHelper.getSystemService("activity")
        } catch (_: Throwable) {
            null
        } ?: return null

        val wrappedBinder = ShizukuBinderWrapper(binderRaw)
        return try {
            val stubClass = Class.forName("android.app.IActivityTaskManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            asInterface.invoke(null, wrappedBinder)
        } catch (_: Throwable) {
            try {
                val stubClass = Class.forName("android.app.IActivityManager\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                asInterface.invoke(null, wrappedBinder)
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * 获取当前处于前台运行的任务 Task ID
     * 自动排除 TouchGuard 自身界面，确保锁定的是用户正在使用的目标应用
     */
    fun getForegroundTaskId(context: Context): Int? {
        val service = getAtmService() ?: return null
        val methods = service.javaClass.methods
        val getTasksMethod = methods.firstOrNull { it.name == "getTasks" } ?: return null

        val taskList = try {
            when (getTasksMethod.parameterTypes.size) {
                4 -> getTasksMethod.invoke(service, 5, false, false, 0)
                3 -> getTasksMethod.invoke(service, 5, false, false)
                2 -> getTasksMethod.invoke(service, 5, false)
                1 -> getTasksMethod.invoke(service, 5)
                else -> null
            } as? List<*>
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } ?: return null

        var fallbackTaskId: Int? = null

        for (item in taskList) {
            if (item == null) continue
            try {
                val taskIdField = item.javaClass.fields.firstOrNull { it.name == "taskId" }
                    ?: item.javaClass.declaredFields.firstOrNull { it.name == "taskId" }?.apply { isAccessible = true }
                val tid = taskIdField?.getInt(item) ?: continue

                if (fallbackTaskId == null) {
                    fallbackTaskId = tid
                }

                // 检查 topActivity / baseActivity 是否为当前应用
                val topActivityField = item.javaClass.fields.firstOrNull { it.name == "topActivity" }
                    ?: item.javaClass.declaredFields.firstOrNull { it.name == "topActivity" }?.apply { isAccessible = true }
                val component = topActivityField?.get(item) as? ComponentName

                if (component != null && component.packageName != context.packageName) {
                    return tid
                }
            } catch (_: Throwable) {}
        }

        return fallbackTaskId
    }

    /**
     * 调用系统特权接口将目标 Task 固定在屏幕上
     * 触发后系统将封锁所有手势退出与状态栏
     */
    fun startLockTask(taskId: Int): Boolean {
        val service = getAtmService() ?: return false
        val methods = service.javaClass.methods
        val startMethod = methods.firstOrNull { it.name == "startSystemLockTaskMode" } ?: return false
        return try {
            startMethod.invoke(service, taskId)
            android.util.Log.d("TouchGuard", "Shizuku startSystemLockTaskMode success: taskId=$taskId")
            true
        } catch (e: Throwable) {
            android.util.Log.e("TouchGuard", "Shizuku startSystemLockTaskMode failed", e)
            false
        }
    }

    /**
     * 解除屏幕固定模式
     */
    fun stopLockTask(): Boolean {
        val service = getAtmService() ?: return false
        val methods = service.javaClass.methods
        val stopMethod = methods.firstOrNull { it.name == "stopSystemLockTaskMode" } ?: return false
        return try {
            stopMethod.invoke(service)
            android.util.Log.d("TouchGuard", "Shizuku stopSystemLockTaskMode success")
            true
        } catch (e: Throwable) {
            android.util.Log.e("TouchGuard", "Shizuku stopSystemLockTaskMode failed", e)
            false
        }
    }

    private val statusBarToken: IBinder = Binder()

    /**
     * 辅助双保险：通过 IStatusBarService 彻底冻结状态栏下拉
     */
    fun setStatusBarExpandDisabled(context: Context, disabled: Boolean): Boolean {
        return try {
            val binderRaw = SystemServiceHelper.getSystemService("statusbar") ?: return false
            val wrappedBinder = ShizukuBinderWrapper(binderRaw)
            val stubClass = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val service = asInterface.invoke(null, wrappedBinder) ?: return false
            val disableMethod = service.javaClass.methods.firstOrNull { it.name == "disable" && it.parameterTypes.size == 3 }
            val flag = if (disabled) 0x00010000 /* DISABLE_EXPAND */ else 0
            disableMethod?.invoke(service, flag, statusBarToken, context.packageName)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 检查系统屏幕固定开关是否已开启 (Settings.System.LOCK_TO_APP_ENABLED)
     */
    fun isLockToAppEnabled(context: Context): Boolean {
        return try {
            android.provider.Settings.System.getInt(context.contentResolver, "lock_to_app_enabled", 0) != 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 尝试通过 Shizuku 自动开启系统的屏幕固定总开关 (小米 / ColorOS 强启)
     */
    fun ensureLockToAppEnabled(context: Context): Boolean {
        if (isLockToAppEnabled(context)) return true
        return try {
            val method = rikka.shizuku.Shizuku::class.java.declaredMethods.firstOrNull {
                it.name == "newProcess" && it.parameterTypes.size == 3
            }?.apply { isAccessible = true } ?: return false

            // 先给 shell 授权 WRITE_SETTINGS，确保 settings put 能够写入系统表
            val p1 = method.invoke(
                null,
                arrayOf("appops", "set", "com.android.shell", "WRITE_SETTINGS", "allow"),
                null,
                null
            ) as? Process
            p1?.waitFor()

            // 写入系统全局屏幕固定总开关
            val p2 = method.invoke(
                null,
                arrayOf("settings", "put", "system", "lock_to_app_enabled", "1"),
                null,
                null
            ) as? Process
            p2?.waitFor()

            isLockToAppEnabled(context)
        } catch (_: Throwable) {
            false
        }
    }
}

