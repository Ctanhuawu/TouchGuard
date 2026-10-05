package com.ccwait.touchguard.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

enum class ShizukuStatus {
    CHECKING,
    AUTHORIZED,
    UNAUTHORIZED,
    NOT_RUNNING
}

object ShizukuPermissionManager {
    const val REQUEST_CODE = 1001
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    var status by mutableStateOf(ShizukuStatus.CHECKING)
        private set

    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        isInitialized = true
        try {
            Shizuku.addBinderReceivedListenerSticky {
                checkPermissionSync()
            }
            Shizuku.addBinderDeadListener {
                status = ShizukuStatus.NOT_RUNNING
            }
        } catch (_: Throwable) {
        }
    }

    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun checkPermissionSync(): ShizukuStatus {
        if (!isRunning()) {
            status = ShizukuStatus.NOT_RUNNING
            return status
        }
        status = try {
            if (Shizuku.isPreV11()) {
                ShizukuStatus.AUTHORIZED
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuStatus.AUTHORIZED
            } else {
                ShizukuStatus.UNAUTHORIZED
            }
        } catch (_: Throwable) {
            ShizukuStatus.UNAUTHORIZED
        }
        return status
    }

    suspend fun checkPermission(forceRequest: Boolean = false): ShizukuStatus = withContext(Dispatchers.IO) {
        init()
        if (!isRunning()) {
            status = ShizukuStatus.NOT_RUNNING
            return@withContext status
        }
        val current = checkPermissionSync()
        if (current == ShizukuStatus.UNAUTHORIZED && forceRequest) {
            requestPermission()
        }
        status
    }

    suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isRunning()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        if (checkPermissionSync() == ShizukuStatus.AUTHORIZED) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        var resumed = false
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == REQUEST_CODE) {
                    try {
                        Shizuku.removeRequestPermissionResultListener(this)
                    } catch (_: Throwable) {}
                    val isGranted = grantResult == PackageManager.PERMISSION_GRANTED
                    status = if (isGranted) ShizukuStatus.AUTHORIZED else ShizukuStatus.UNAUTHORIZED
                    if (!resumed) {
                        resumed = true
                        continuation.resume(isGranted)
                    }
                }
            }
        }
        try {
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Throwable) {
            try {
                Shizuku.removeRequestPermissionResultListener(listener)
            } catch (_: Throwable) {}
            if (!resumed) {
                resumed = true
                continuation.resume(false)
            }
        }
    }

    fun launchShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }
}
