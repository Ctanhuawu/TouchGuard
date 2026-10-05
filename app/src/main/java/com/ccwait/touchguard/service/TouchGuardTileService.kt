package com.ccwait.touchguard.service

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.R
import com.ccwait.touchguard.strategy.TouchLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            TouchLockManager.collapsePanels(applicationContext)
        }
        CoroutineScope(Dispatchers.Main).launch {
            if (isLocked) {
                TouchLockManager.unlock(applicationContext, source = "控制中心")
            } else {
                TouchLockManager.lock(applicationContext, source = "控制中心")
            }
            updateTileInternal()
        }
    }

    private fun updateTileInternal() {
        val tile = qsTile ?: return
        val isLocked = TouchLockManager.isTouchLocked

        tile.state = if (isLocked) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_touch_lock)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isLocked) {
                "已锁定 · ${AppPreferences.unlockMechanism.shortName}"
            } else {
                "未锁定 · 点击锁定"
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
