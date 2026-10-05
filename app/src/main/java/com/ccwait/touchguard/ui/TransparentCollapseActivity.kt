package com.ccwait.touchguard.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle

/**
 * 专用于从 TileService 触发安全折叠控制中心的零开销透明跳板 Activity
 * 纯透明、无动画、无历史记录、不占用近期任务，在 onCreate 中立即 finish
 */
class TransparentCollapseActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        finish()
    }
}
