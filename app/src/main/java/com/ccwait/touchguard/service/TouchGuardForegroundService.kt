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
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (AppPreferences.isAutoUnlockOnScreenOffEnabled && TouchLockManager.isTouchLocked) {
                        AppLogManager.addLog("熄屏", "后台服务检测到熄屏 (SCREEN_OFF)，执行安全自动解除")
                        CoroutineScope(Dispatchers.Main).launch {
                            TouchLockManager.unlock(this@TouchGuardForegroundService, source = "熄屏自动解除")
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    // 当没有开启熄屏自动解除且当前处于锁定中时：
                    // 屏幕唤醒或用户解锁锁屏后，系统可能重置了系统栏状态或使顶层悬浮窗失去焦点。
                    // 必须立即重新加固触控拦截与系统手势管控！
                    if (!AppPreferences.isAutoUnlockOnScreenOffEnabled && TouchLockManager.isTouchLocked) {
                        val eventName = if (intent.action == Intent.ACTION_USER_PRESENT) "解锁进入系统" else "屏幕点亮"
                        AppLogManager.addLog("唤醒加固", "检测到$eventName，立即重新激活全屏触控拦截与状态栏冻结")
                        CoroutineScope(Dispatchers.Main).launch {
                            TouchLockManager.reassert(this@TouchGuardForegroundService)
                        }
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
        instance = this
        TouchGuardNotificationManager.init(this)
        TouchLockManager.addLockStateListener(lockListener)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
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
        if (AppPreferences.isAllowVolumeKeysEnabled) {
            stopKeyIntercept()
            return
        }
        if (mediaSession != null) return
        try {
            val session = MediaSession(this, "TouchGuardKeyWatcher").apply {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
                }
                val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
                    override fun onAdjustVolume(direction: Int) {
                        if (!TouchLockManager.isTouchLocked) return
                        android.util.Log.d("TouchGuard", "MediaSession onAdjustVolume: direction=$direction")
                        PhysicalKeyUnlockHandler.onVolumeAdjust(this@TouchGuardForegroundService, direction)
                        if (AppPreferences.isAllowVolumeKeysEnabled) {
                            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                            val adjustDir = if (direction > 0) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER
                            audioManager?.adjustSuggestedStreamVolume(adjustDir, android.media.AudioManager.USE_DEFAULT_STREAM_TYPE, android.media.AudioManager.FLAG_SHOW_UI)
                        }
                    }
                }
                setPlaybackToRemote(volumeProvider)
                val playbackState = android.media.session.PlaybackState.Builder()
                    .setState(android.media.session.PlaybackState.STATE_PLAYING, 0L, 1.0f)
                    .setActions(
                        android.media.session.PlaybackState.ACTION_PLAY or
                        android.media.session.PlaybackState.ACTION_PAUSE or
                        android.media.session.PlaybackState.ACTION_STOP
                    )
                    .build()
                setPlaybackState(playbackState)
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

    fun updateKeyInterceptState() {
        if (AppPreferences.isAllowVolumeKeysEnabled) {
            stopKeyIntercept()
        } else if (TouchLockManager.isTouchLocked) {
            startKeyIntercept()
        }
    }

    override fun onDestroy() {
        instance = null
        TouchLockManager.removeLockStateListener(lockListener)
        stopKeyIntercept()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        GlobalScreenPolicyManager.applyPolicies(this, false)
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: TouchGuardForegroundService? = null
            private set

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
