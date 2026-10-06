package com.ccwait.touchguard.strategy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.model.PhysicalKeyUnlockHandler
import com.ccwait.touchguard.system.AudioVolumeHelper

/**
 * 全屏透明触控拦截视图
 *
 * 核心特性：
 * 1. 拥有 1/255 极微透明度缓冲并重写 onDraw，保证 SurfaceFlinger 为其分配 InputChannel，避免空缓冲穿透；
 * 2. 吞噬全屏触控 MotionEvent；
 * 3. 截获音量按键传递至 PhysicalKeyUnlockHandler，并在配置允许时通过 AudioVolumeHelper 放行真实音量调节；
 * 4. 动态设置手势排除矩形（SystemGestureExclusionRects）与沉浸式全屏模式。
 */
@SuppressLint("ViewConstructor")
internal class TouchLockOverlayView(context: Context) : FrameLayout(context) {

    init {
        // 使用 alpha=1 (0x01000000) 微弱颜色，视觉完全不可见，但能防止系统将完全透明纯0优化为无缓冲(rect=0,0)，
        // 从而确保 SurfaceFlinger 为该图层分配实体渲染缓冲，并向 InputDispatcher 注册完整全屏触控通道
        setBackgroundColor(Color.argb(1, 0, 0, 0))
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        fitsSystemWindows = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.argb(1, 0, 0, 0))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyImmersiveMode()
    }

    fun applyImmersiveMode() {
        if (!AppPreferences.isHideSystemBarsEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController?.show(WindowInsets.Type.systemBars())
            }
            @Suppress("DEPRECATION")
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        @Suppress("DEPRECATION")
        systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets.CONSUMED
        } else {
            @Suppress("DEPRECATION")
            insets.consumeSystemWindowInsets()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 排除边缘系统手势拦截（单侧高度限制在 Android 规范的 200dp 内，防止系统丢弃请求）
            val density = context.resources.displayMetrics.density
            val maxExclusionPx = (200 * density).toInt()
            val edgeWidth = (60 * density).toInt().coerceAtMost(width / 4)
            val edgeHeight = (60 * density).toInt().coerceAtMost(height / 4)

            val leftRect = Rect(0, (height - maxExclusionPx).coerceAtLeast(0) / 2, edgeWidth, ((height + maxExclusionPx) / 2).coerceAtMost(height))
            val rightRect = Rect(width - edgeWidth, (height - maxExclusionPx).coerceAtLeast(0) / 2, width, ((height + maxExclusionPx) / 2).coerceAtMost(height))
            val topRect = Rect(0, 0, width, edgeHeight)
            val bottomRect = Rect(0, height - edgeHeight, width, height)
            try {
                systemGestureExclusionRects = listOf(leftRect, rightRect, topRect, bottomRect)
            } catch (_: Exception) {}
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus && TouchLockManager.isTouchLocked) {
            postDelayed({
                if (TouchLockManager.isTouchLocked) {
                    TouchLockManager.collapsePanels(context)
                    applyImmersiveMode()
                    requestFocus()
                }
            }, 50)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!TouchLockManager.isTouchLocked) return super.dispatchKeyEvent(event)

        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        PhysicalKeyUnlockHandler.onKeyDown(context, keyCode)
                    }
                    if (AppPreferences.isAllowVolumeKeysEnabled) {
                        AudioVolumeHelper.adjustVolumeByKey(context, keyCode)
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    PhysicalKeyUnlockHandler.onKeyUp(keyCode)
                    return true
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = TouchLockManager.isTouchLocked

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!TouchLockManager.isTouchLocked) {
            return false
        }
        val density = context.resources.displayMetrics.density
        if ((ev.y < 120 * density || ev.y > height - 100 * density) && ev.action == MotionEvent.ACTION_DOWN) {
            TouchLockManager.collapsePanels(context)
        }
        return true
    }
}
