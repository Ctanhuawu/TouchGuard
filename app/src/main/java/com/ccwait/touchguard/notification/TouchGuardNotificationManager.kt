package com.ccwait.touchguard.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.MainActivity
import com.ccwait.touchguard.R
import com.ccwait.touchguard.receiver.NotificationActionReceiver
import com.ccwait.touchguard.strategy.TouchLockManager

object TouchGuardNotificationManager {
    const val CHANNEL_ID = "touchguard_status_channel"
    const val NOTIFICATION_ID = 1001

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TouchGuard 触控守护"
            val descriptionText = "常驻快捷控制与触控锁定状态提示"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context): Notification {
        init(context)
        val isLocked = TouchLockManager.isTouchLocked
        val strategy = TouchLockManager.currentStrategyType
        val unlockMech = AppPreferences.unlockMechanism

        // 点击通知卡片主体：直接执行快捷锁定 / 解除，无需弹窗打扰当前前台画面！
        val toggleAction = if (isLocked) NotificationActionReceiver.ACTION_UNLOCK else NotificationActionReceiver.ACTION_LOCK
        val toggleIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = toggleAction
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            if (isLocked) 1001 else 1002,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 操作按钮：打开 TouchGuard 应用主页
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("tab", 0)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_touchguard)
            // 核心交互：轻触通知卡片任意区域直接触发锁定/解除
            .setContentIntent(togglePendingIntent)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isLocked) {
            // 锁定状态下的通知：醒目提示，附带解除与打开主页按钮
            builder
                .setContentTitle("⚠️ 屏幕触控已锁定")
                .setContentText("轻触直接解除锁定 · ${strategy.title} · ${unlockMech.promptTip}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .addAction(
                    R.drawable.ic_qs_touch_unlock,
                    "🔓 立即解除",
                    togglePendingIntent
                )
                .addAction(
                    R.drawable.ic_stat_touchguard,
                    "📱 打开主页",
                    openAppPendingIntent
                )
        } else {
            // 就绪状态下的通知：轻量常驻，轻触卡片或操作按钮均可一键锁定
            builder
                .setContentTitle("TouchGuard · 轻触立即锁定")
                .setContentText("轻触直接锁定触控 · 引擎: ${strategy.title}")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(AppPreferences.isKeepAliveEnabled)
                .addAction(
                    R.drawable.ic_qs_touch_lock,
                    "🔒 立即锁定",
                    togglePendingIntent
                )
                .addAction(
                    R.drawable.ic_stat_touchguard,
                    "📱 打开主页",
                    openAppPendingIntent
                )
        }

        return builder.build()
    }

    fun updateNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        android.util.Log.d("TouchGuard", "updateNotification: isLocked=${TouchLockManager.isTouchLocked}, isKeepAlive=${AppPreferences.isKeepAliveEnabled}")
        if (TouchLockManager.isTouchLocked || AppPreferences.isKeepAliveEnabled) {
            try {
                val noti = buildNotification(context)
                notificationManager.notify(NOTIFICATION_ID, noti)
                android.util.Log.d("TouchGuard", "notificationManager.notify SUCCESS, id=$NOTIFICATION_ID")
            } catch (e: Exception) {
                android.util.Log.e("TouchGuard", "notificationManager.notify FAILED", e)
            }
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}
