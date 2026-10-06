package com.ccwait.touchguard.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * 悬浮窗与 AppOps 权限统一管理助手
 *
 * 职责单一：集中管理全屏悬浮窗（SYSTEM_ALERT_WINDOW）与厂商扩展权限的检测、
 * Shizuku 特权静默授权、Root 兜底授权与系统设置引导跳转。
 */
object OverlayPermissionHelper {

    private const val TAG = "OverlayPermissionHelper"

    // AppOps 常量
    private const val OP_SYSTEM_ALERT_WINDOW = 24
    private const val MODE_ALLOWED = 0

    // HyperOS / MIUI 扩展权限 Ops
    private const val OP_MIUI_ALERT_WINDOW = 10021 // 悬浮窗
    private const val OP_MIUI_BACKGROUND_START = 10020 // 后台弹窗
    private const val OP_MIUI_SHOW_WHEN_LOCKED = 10008 // 锁屏显示

    /**
     * 判断当前是否已具备悬浮窗权限
     */
    fun hasPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 尝试确保悬浮窗权限生效（优先通过已授权的 Shizuku 或 Root 静默提权）
     */
    fun ensurePermission(context: Context): Boolean {
        if (hasPermission(context)) return true

        // 1. 通过 Shizuku 的 IAppOpsService Binder 直接授权 (最快)
        grantViaShizukuBinder(context)
        if (hasPermission(context)) return true

        // 2. 通过 Shizuku 进程执行 appops 命令行兜底
        grantViaShizukuShell(context)
        if (hasPermission(context)) return true

        // 3. 通过 Root (su) 执行 appops 命令行兜底
        grantViaRoot(context)

        // 等待并轮询确认生效（适配部分厂商系统的 IPC 延迟）
        for (i in 1..4) {
            if (hasPermission(context)) return true
            try { Thread.sleep(40) } catch (_: Exception) {}
        }

        return hasPermission(context)
    }

    /**
     * 打开系统「显示在其他应用上层」设置页
     */
    fun requestPermission(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay permission settings", e)
        }
    }

    private fun grantViaShizukuBinder(context: Context) {
        runCatching {
            val appOpsBinder = SystemServiceHelper.getSystemService("appops") ?: return
            val wrappedBinder = ShizukuBinderWrapper(appOpsBinder)
            val stubClass = Class.forName("com.android.internal.app.IAppOpsService\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val service = asInterface.invoke(null, wrappedBinder) ?: return
            val setMode = service.javaClass.getMethod(
                "setMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val uid = context.applicationInfo.uid
            val pkg = context.packageName

            setMode.invoke(service, OP_SYSTEM_ALERT_WINDOW, uid, pkg, MODE_ALLOWED)
            runCatching { setMode.invoke(service, OP_MIUI_ALERT_WINDOW, uid, pkg, MODE_ALLOWED) }
            runCatching { setMode.invoke(service, OP_MIUI_BACKGROUND_START, uid, pkg, MODE_ALLOWED) }
            runCatching { setMode.invoke(service, OP_MIUI_SHOW_WHEN_LOCKED, uid, pkg, MODE_ALLOWED) }
        }.onFailure {
            Log.w(TAG, "grantViaShizukuBinder failed: ${it.message}")
        }
    }

    private fun grantViaShizukuShell(context: Context) {
        runCatching {
            val binder = Shizuku.getBinder() ?: return
            val service = IShizukuService.Stub.asInterface(binder) ?: return
            val pkg = context.packageName
            val cmds = arrayOf(
                "sh", "-c",
                "appops set $pkg SYSTEM_ALERT_WINDOW allow; " +
                "appops set $pkg $OP_MIUI_ALERT_WINDOW allow; " +
                "appops set $pkg $OP_MIUI_BACKGROUND_START allow; " +
                "appops set $pkg $OP_MIUI_SHOW_WHEN_LOCKED allow"
            )
            val p = service.newProcess(cmds, null, null)
            p.waitFor()
        }.onFailure {
            Log.w(TAG, "grantViaShizukuShell failed: ${it.message}")
        }
    }

    private fun grantViaRoot(context: Context) {
        runCatching {
            val pkg = context.packageName
            val p = Runtime.getRuntime().exec(
                arrayOf("su", "-c", "appops set $pkg SYSTEM_ALERT_WINDOW allow")
            )
            p.waitFor()
        }.onFailure {
            Log.w(TAG, "grantViaRoot failed: ${it.message}")
        }
    }
}
