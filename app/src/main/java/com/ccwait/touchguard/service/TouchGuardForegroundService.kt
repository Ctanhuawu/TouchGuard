package com.ccwait.touchguard.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.notification.TouchGuardNotificationManager
import com.ccwait.touchguard.strategy.GlobalScreenPolicyManager
import com.ccwait.touchguard.strategy.TouchLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TouchGuardForegroundService : Service() {

    private var mediaSession: MediaSession? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                if (AppPreferences.isAutoUnlockOnScreenOffEnabled && TouchLockManager.isTouchLocked) {
                    AppLogManager.addLog("熄屏", "后台服务检测到熄屏 (SCREEN_OFF)，执行安全自动解除")
                    CoroutineScope(Dispatchers.Main).launch {
                        TouchLockManager.unlock(this@TouchGuardForegroundService, source = "熄屏自动解除")
                    }
                }
            }
        }
    }

    private val lockListener: (Boolean) -> Unit = { isLocked ->
        if (isLocked) {
            startKeyIntercept()
        } else {
            stopKeyIntercept()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TouchGuardNotificationManager.init(this)
        TouchLockManager.addLockStateListener(lockListener)
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
        if (TouchLockManager.isTouchLocked) {
            startKeyIntercept()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = TouchGuardNotificationManager.buildNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    TouchGuardNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    TouchGuardNotificationManager.NOTIFICATION_ID,
                    notification,
                    0
                )
            }
        } else {
            startForeground(TouchGuardNotificationManager.NOTIFICATION_ID, notification)
        }

        if (TouchLockManager.isTouchLocked) {
            startKeyIntercept()
        } else {
            stopKeyIntercept()
        }

        return START_STICKY
    }

    private fun startKeyIntercept() {
        if (mediaSession != null) return
        try {
            val session = MediaSession(this, "TouchGuardKeyWatcher").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
                    override fun onAdjustVolume(direction: Int) {
                        if (!TouchLockManager.isTouchLocked) return
                        android.util.Log.d("TouchGuard", "MediaSession onAdjustVolume: direction=$direction")
                        PhysicalKeyUnlockHandler.onVolumeAdjust(this@TouchGuardForegroundService, direction)
                    }
                }
                setPlaybackToRemote(volumeProvider)
                isActive = true
            }
            mediaSession = session
            android.util.Log.d("TouchGuard", "MediaSession global key interceptor activated")
        } catch (e: Exception) {
            android.util.Log.e("TouchGuard", "Failed to activate MediaSession", e)
        }
    }

    private fun stopKeyIntercept() {
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {}
        mediaSession = null
    }

    override fun onDestroy() {
        TouchLockManager.removeLockStateListener(lockListener)
        stopKeyIntercept()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        GlobalScreenPolicyManager.applyPolicies(this, false)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TouchGuardForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TouchGuardForegroundService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
