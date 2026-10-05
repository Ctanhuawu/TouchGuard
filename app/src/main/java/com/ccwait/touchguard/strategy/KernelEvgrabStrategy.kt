package com.ccwait.touchguard.strategy

import android.content.Context
import android.widget.Toast
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.model.RootPermissionManager
import com.ccwait.touchguard.model.RootStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class KernelEvgrabStrategy : TouchLockStrategy {
    override val type: StrategyType = StrategyType.ROOT_EVIOCGRAB

    @Volatile
    private var _isLocked: Boolean = false
    override val isLocked: Boolean
        get() = _isLocked

    private var grabProcess: Process? = null
    private var listenerJob: Job? = null

    override val readiness: StrategyReadiness
        get() = when (RootPermissionManager.status) {
            RootStatus.AUTHORIZED -> StrategyReadiness.READY
            RootStatus.UNAUTHORIZED -> StrategyReadiness.PERMISSION_MISSING
            RootStatus.NO_ROOT_DEVICE -> StrategyReadiness.UNSUPPORTED
            RootStatus.CHECKING -> StrategyReadiness.CHECKING
        }

    override val badgeText: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "Root"
            StrategyReadiness.PERMISSION_MISSING -> "未授权"
            StrategyReadiness.UNSUPPORTED -> "无 Root"
            StrategyReadiness.CHECKING -> "检测中"
        }

    override val statusSummary: String
        get() = when (readiness) {
            StrategyReadiness.READY -> "v${BuildConfig.VERSION_NAME} · 稳定版"
            StrategyReadiness.PERMISSION_MISSING -> "点击申请 Root 授权 · 驱动未就绪"
            StrategyReadiness.UNSUPPORTED -> "未检测到 Root 环境 · 点击切换为免 Root"
            StrategyReadiness.CHECKING -> "正在检测底层 Root 授权..."
        }

    override suspend fun checkReadiness(context: Context, forceRequest: Boolean): StrategyReadiness {
        RootPermissionManager.checkPermission(forceRequest)
        return readiness
    }

    override suspend fun requestPermission(context: Context): Boolean {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "正在申请 Root 授权，请在弹窗中允许...", Toast.LENGTH_SHORT).show()
        }
        val res = RootPermissionManager.checkPermission(forceRequest = true)
        val isOk = res == RootStatus.AUTHORIZED
        withContext(Dispatchers.Main) {
            if (isOk) {
                Toast.makeText(context, "✅ Root 授权成功！驱动环境已就绪", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "❌ 未获得 Root 授权，请在授权管理器中允许", Toast.LENGTH_LONG).show()
            }
        }
        return isOk
    }

    override suspend fun prepare(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(context.filesDir, "evgrab")
            context.assets.open("evgrab").use { input ->
                val assetBytes = input.readBytes()
                if (!targetFile.exists() || targetFile.length() != assetBytes.size.toLong()) {
                    targetFile.writeBytes(assetBytes)
                    targetFile.setExecutable(true, false)
                }
            }
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow"))
            } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun lock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prepare(context)
            // 先清理可能残留的旧守护进程
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "killall -9 evgrab 2>/dev/null")).waitFor()
            } catch (_: Exception) {}

            val binaryPath = File(context.filesDir, "evgrab").absolutePath
            Runtime.getRuntime().exec(arrayOf("chmod", "777", binaryPath)).waitFor()

            val mechId = AppPreferences.unlockMechanism.id
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "$binaryPath auto $mechId"))
            grabProcess = process

            val reader = process.inputStream.bufferedReader()
            val line = reader.readLine()

            if (line != null && line.contains("GRAB_SUCCESS")) {
                _isLocked = true
                android.util.Log.d("TouchGuard", "evgrab ready: $line")
                AppLogManager.addLog("驱动", "触控硬件独占已就绪 ($line)")

                // 启动后台协程持续监听 evgrab 的底层实体按键输出
                listenerJob?.cancel()
                listenerJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var logLine: String?
                        while (reader.readLine().also { logLine = it } != null) {
                            val msg = logLine?.trim() ?: continue
                            android.util.Log.d("TouchGuard", "evgrab event: $msg")
                            if (msg.startsWith("STEP:")) {
                                withContext(Dispatchers.Main) {
                                    val parts = msg.split(":")
                                    if (parts.size >= 3) {
                                        val current = parts[1].toIntOrNull() ?: 1
                                        val total = parts[2].toIntOrNull() ?: 2
                                        PhysicalKeyUnlockHandler.onEvgrabStep(context, current, total, isWrong = false)
                                    } else if (msg == "STEP:WRONG") {
                                        PhysicalKeyUnlockHandler.onEvgrabStep(context, 0, 0, isWrong = true)
                                    }
                                }
                            } else if (msg.startsWith("UNLOCKED")) {
                                withContext(Dispatchers.Main) {
                                    PhysicalKeyUnlockHandler.onEvgrabUnlocked(context)
                                }
                                break
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        try {
                            reader.close()
                        } catch (_: Exception) {}
                        if (_isLocked) {
                            withContext(Dispatchers.Main) {
                                TouchLockManager.unlock(context, source = "驱动退出")
                            }
                        }
                    }
                }

                Result.success(Unit)
            } else {
                unlock(context)
                Result.failure(Exception("Root 权限未授予或 /dev/input/event4 驱动节点异常"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            unlock(context)
            Result.failure(e)
        }
    }

    override suspend fun unlock(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLocked = false
            listenerJob?.cancel()
            listenerJob = null
            try {
                grabProcess?.outputStream?.write("QUIT\n".toByteArray())
                grabProcess?.outputStream?.flush()
            } catch (_: Exception) {}
            try {
                grabProcess?.outputStream?.close()
            } catch (_: Exception) {}
            grabProcess?.destroy()
            grabProcess = null
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "killall -9 evgrab 2>/dev/null")).waitFor()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _isLocked = false
            Result.failure(e)
        }
    }

    override fun release(context: Context) {
        _isLocked = false
        listenerJob?.cancel()
        listenerJob = null
        try {
            grabProcess?.outputStream?.write("QUIT\n".toByteArray())
            grabProcess?.outputStream?.flush()
        } catch (_: Exception) {}
        try {
            grabProcess?.outputStream?.close()
        } catch (_: Exception) {}
        grabProcess?.destroy()
        grabProcess = null
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "killall -9 evgrab 2>/dev/null"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
