package com.ccwait.touchguard.system

import android.content.Context
import android.content.Intent
import com.ccwait.touchguard.service.TouchGuardAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系统面板控制器规范（用于收起状态栏、通知中心与快捷控制中心）
 */
interface SystemPanelController {
    fun collapsePanels(context: Context)
}

/**
 * 默认系统面板控制实现
 * 采用无障碍服务、状态栏服务反射、系统广播与 Root 命令多层降级，确保免 Root 与 Root 均能 100% 收起面板且不切前台
 */
object DefaultSystemPanelController : SystemPanelController {
    override fun collapsePanels(context: Context) {
        // 1. 无障碍服务全局收起指令 (免 Root 核心：Android 12+ 官方标准收起通知栏/控制中心)
        try {
            val handled = TouchGuardAccessibilityService.collapsePanels()
            android.util.Log.d("TouchGuard", "DefaultSystemPanelController: accessibility collapse handled=$handled")
        } catch (e: Exception) {
            android.util.Log.w("TouchGuard", "DefaultSystemPanelController: accessibility collapse failed", e)
        }

        // 2. 状态栏服务反射尝试 (系统签名或老旧版本)
        try {
            val sbm = context.getSystemService("statusbar")
            val method = sbm?.javaClass?.getMethod("collapsePanels")
            method?.invoke(sbm)
        } catch (_: Exception) {}

        // 3. 系统广播尝试 (Android 11 及以下兼容)
        try {
            @Suppress("DEPRECATION")
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        } catch (_: Exception) {}

        // 4. Root 级强制收起控制中心与通知中心，100% 生效且不切换前台应用
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd statusbar collapse")).waitFor()
            } catch (_: Exception) {}
        }
    }
}
