package com.ccwait.touchguard.model

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GuardLogItem(
    val timestamp: String,
    val tag: String,
    val message: String,
    val isWarning: Boolean = false,
    val isSuccess: Boolean = false
)

object AppLogManager {
    val logs = mutableStateListOf<GuardLogItem>()

    init {
        addLog("系统", "TouchGuard 核心守护服务已加载", isSuccess = true)
        addLog("驱动", "底层输入节点动态探测机制就绪 (Touch + Pen)")
        addLog("安全", "物理按键应急恢复机制就绪 (阈值 1000ms)")
    }

    fun addLog(tag: String, message: String, isWarning: Boolean = false, isSuccess: Boolean = false) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        android.util.Log.d("TouchGuard", "AppLog: [$tag] $message (warn=$isWarning, succ=$isSuccess)")
        logs.add(0, GuardLogItem(time, tag, message, isWarning, isSuccess))
        if (logs.size > 100) {
            logs.removeAt(logs.lastIndex)
        }
    }

    fun clear() {
        logs.clear()
        addLog("系统", "日志记录已清空")
    }
}
