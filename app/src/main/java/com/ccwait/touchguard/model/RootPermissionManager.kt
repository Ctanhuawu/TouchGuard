package com.ccwait.touchguard.model

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class RootStatus {
    CHECKING,       // 正在检测 Root 环境与授权
    AUTHORIZED,     // 已获取完整 Root 提权
    UNAUTHORIZED,   // 未获取 Root 授权 / 授权丢失
    NO_ROOT_DEVICE  // 系统未检测到 Root 环境 (无 su 二进制)
}

object RootPermissionManager {
    var status by mutableStateOf(RootStatus.CHECKING)
        private set

    var lastCheckedTime by mutableStateOf(0L)
        private set

    private val COMMON_SU_PATHS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su",
        "/data/adb/magisk/su"
    )

    fun isSuBinaryPresent(): Boolean {
        for (path in COMMON_SU_PATHS) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {}
        }
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exit = process.waitFor()
            if (exit == 0) return true
        } catch (_: Exception) {}
        return false
    }

    suspend fun checkPermission(forceRequest: Boolean = false): RootStatus = withContext(Dispatchers.IO) {
        val hasSuByPath = isSuBinaryPresent()

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readLine() ?: ""

            val timeoutSec = if (forceRequest) 8L else 3L
            val finished = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(timeoutSec, TimeUnit.SECONDS)
            } else {
                process.waitFor()
                true
            }

            if (finished && process.exitValue() == 0 && output.contains("uid=0")) {
                status = RootStatus.AUTHORIZED
            } else {
                try {
                    process.destroy()
                } catch (_: Exception) {}
                status = RootStatus.UNAUTHORIZED
            }
        } catch (e: Exception) {
            val isNotFound = e.message?.contains("error=2") == true ||
                             e.message?.contains("No such file") == true
            status = if (isNotFound && !hasSuByPath) RootStatus.NO_ROOT_DEVICE else RootStatus.UNAUTHORIZED
        }

        lastCheckedTime = System.currentTimeMillis()
        status
    }
}
