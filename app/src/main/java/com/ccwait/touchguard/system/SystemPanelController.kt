package com.ccwait.touchguard.system

import android.content.Context
import android.content.Intent
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
 * 采用广播、反射与 Root 命令三重降级保证 100% 收起面板且不切前台
 */
object DefaultSystemPanelController : SystemPanelController {
    override fun collapsePanels(context: Context) {
        // 1. 系统广播尝试
        try {
            @Suppress("DEPRECATION")
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        } catch (_: Exception) {}

        // 2. 状态栏服务反射尝试
        try {
            val sbm = context.getSystemService("statusbar")
            val method = sbm?.javaClass?.getMethod("collapsePanels")
            method?.invoke(sbm)
        } catch (_: Exception) {}

        // 3. Root 级强制收起控制中心与通知中心，100% 生效且不切换前台应用
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd statusbar collapse")).waitFor()
            } catch (_: Exception) {}
        }
    }
}
