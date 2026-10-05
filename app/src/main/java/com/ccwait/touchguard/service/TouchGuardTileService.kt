package com.ccwait.touchguard.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.system.DefaultSystemPanelController
import com.ccwait.touchguard.ui.TransparentCollapseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TouchGuardTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileInternal()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileInternal()
    }

    override fun onClick() {
        super.onClick()
        val isLocked = TouchLockManager.isTouchLocked
        if (!isLocked) {
            collapseFromTile()
            DefaultSystemPanelController.collapsePanels(applicationContext)
        }
        CoroutineScope(Dispatchers.Main).launch {
            if (isLocked) {
                TouchLockManager.unlock(applicationContext, source = "控制中心")
            } else {
                delay(120)
                TouchLockManager.lock(applicationContext, source = "控制中心")
            }
            updateTileInternal()
        }
    }

    private fun collapseFromTile() {
        try {
            val intent = Intent(this, TransparentCollapseActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            android.util.Log.w("TouchGuard", "collapseFromTile failed", e)
        }
    }

    private fun updateTileInternal() {
        val tile = qsTile ?: return
        val isLocked = TouchLockManager.isTouchLocked

        tile.state = if (isLocked) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_touch_lock)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isLocked) {
                "${getString(R.string.home_status_locked)} · ${getString(AppPreferences.unlockMechanism.shortNameRes)}"
            } else {
                getString(R.string.home_btn_lock)
            }
        }

        val iconRes = if (isLocked) R.drawable.ic_qs_touch_lock else R.drawable.ic_qs_touch_unlock
        tile.icon = Icon.createWithResource(this, iconRes)
        tile.updateTile()
    }

    companion object {
        fun updateTileState(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, TouchGuardTileService::class.java)
                )
            } catch (_: Exception) {}
        }

        fun requestAddTile(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val sbm = context.getSystemService(android.app.StatusBarManager::class.java)
                    sbm?.requestAddTileService(
                        ComponentName(context, TouchGuardTileService::class.java),
                        context.getString(R.string.tile_touch_lock),
                        Icon.createWithResource(context, R.drawable.ic_qs_touch_lock),
                        context.mainExecutor
                    ) { result ->
                        android.util.Log.d("TouchGuard", "requestAddTile result: $result")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TouchGuard", "Failed to requestAddTile", e)
                }
            }
        }
    }
}
