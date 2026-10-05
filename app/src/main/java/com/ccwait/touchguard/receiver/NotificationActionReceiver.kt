package com.ccwait.touchguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.system.DefaultSystemPanelController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        android.util.Log.d("TouchGuard", "NotificationActionReceiver onReceive: action=$action")
        val pendingResult = goAsync()

        DefaultSystemPanelController.collapsePanels(context)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (action) {
                    ACTION_LOCK -> {
                        delay(100)
                        val res = TouchLockManager.lock(context.applicationContext, source = "通知中心")
                        android.util.Log.d("TouchGuard", "ACTION_LOCK result: $res, isTouchLocked=${TouchLockManager.isTouchLocked}")
                    }
                    ACTION_UNLOCK -> {
                        val res = TouchLockManager.unlock(context.applicationContext, source = "通知中心")
                        android.util.Log.d("TouchGuard", "ACTION_UNLOCK result: $res, isTouchLocked=${TouchLockManager.isTouchLocked}")
                    }
                    ACTION_TOGGLE -> {
                        if (TouchLockManager.isTouchLocked) {
                            val res = TouchLockManager.unlock(context.applicationContext, source = "通知中心")
                            android.util.Log.d("TouchGuard", "ACTION_TOGGLE (unlock) result: $res")
                        } else {
                            delay(100)
                            val res = TouchLockManager.lock(context.applicationContext, source = "通知中心")
                            android.util.Log.d("TouchGuard", "ACTION_TOGGLE (lock) result: $res")
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOCK = "com.ccwait.touchguard.ACTION_LOCK"
        const val ACTION_UNLOCK = "com.ccwait.touchguard.ACTION_UNLOCK"
        const val ACTION_TOGGLE = "com.ccwait.touchguard.ACTION_TOGGLE"
    }
}
