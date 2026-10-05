package com.ccwait.touchguard.system

import android.content.Context
import com.ccwait.touchguard.system.adaptation.AccessibilityCollapseStrategy
import com.ccwait.touchguard.system.adaptation.LegacyBroadcastCollapseStrategy
import com.ccwait.touchguard.system.adaptation.PanelCollapseStrategy
import com.ccwait.touchguard.system.adaptation.RootCollapseStrategy

/**
 * 系统面板控制器规范（用于收起状态栏、通知中心与快捷控制中心）
 */
interface SystemPanelController {
    fun collapsePanels(context: Context)
}

/**
 * 默认系统面板控制实现
 * 采用模块化策略链（无障碍服务、Root 命令、老旧系统广播降级），确保免 Root 与 Root 均能平滑收起面板且不切前台
 */
object DefaultSystemPanelController : SystemPanelController {
    private val strategies: List<PanelCollapseStrategy> = listOf(
        AccessibilityCollapseStrategy(),
        RootCollapseStrategy(),
        LegacyBroadcastCollapseStrategy()
    )

    override fun collapsePanels(context: Context) {
        for (strategy in strategies) {
            try {
                strategy.collapse(context)
            } catch (e: Exception) {
                android.util.Log.w("TouchGuard", "PanelCollapseStrategy ${strategy.name} failed", e)
            }
        }
    }
}
