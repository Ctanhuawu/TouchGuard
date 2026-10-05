package com.ccwait.touchguard.system.adaptation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.ccwait.touchguard.service.TouchGuardAccessibilityService
import com.ccwait.touchguard.ui.TransparentCollapseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系统控制面板与通知中心折叠策略接口
 */
interface PanelCollapseStrategy {
    val name: String
    fun collapse(context: Context): Boolean
}

/**
 * 策略 1: 无障碍服务全局折叠策略（免 Root 核心）
 * 原理：通过 Android 12+ (API 31+) 官方标准 GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE 接口，
 * 无侵入、零动画打扰且绝不派发全局返回键误退前台应用。
 */
class AccessibilityCollapseStrategy : PanelCollapseStrategy {
    override val name: String = "AccessibilityService"

    override fun collapse(context: Context): Boolean {
        return try {
            TouchGuardAccessibilityService.collapsePanels()
        } catch (e: Exception) {
            android.util.Log.w("PanelCollapse", "AccessibilityCollapseStrategy failed", e)
            false
        }
    }
}

/**
 * 策略 2: Root 级底层系统命令折叠策略
 * 原理：通过 su 执行 Android 内置 statusbar 命令强制折叠通知中心与控制中心
 */
class RootCollapseStrategy : PanelCollapseStrategy {
    override val name: String = "RootCommand"

    override fun collapse(context: Context): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd statusbar collapse")).waitFor()
            } catch (_: Exception) {}
        }
        return true
    }
}

/**
 * 策略 3: 系统广播折叠策略（仅用于 Android 11 及以下历史版本降级兼容）
 */
class LegacyBroadcastCollapseStrategy : PanelCollapseStrategy {
    override val name: String = "LegacyBroadcast"

    override fun collapse(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) 限制非系统应用发送 ACTION_CLOSE_SYSTEM_DIALOGS，跳过避免无谓异常
            return false
        }
        return try {
            @Suppress("DEPRECATION")
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Quick Settings 磁贴专属折叠助手
 * 针对 TileService 上下文，利用官方 startActivityAndCollapse API 驱动 SystemUI 折叠
 */
object TileCollapseHelper {
    fun collapse(tileService: TileService): Boolean {
        return try {
            val intent = Intent(tileService, TransparentCollapseActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    tileService,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                tileService.startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                tileService.startActivityAndCollapse(intent)
            }
            true
        } catch (e: Exception) {
            android.util.Log.w("TileCollapseHelper", "startActivityAndCollapse failed", e)
            false
        }
    }
}
