package com.ccwait.touchguard.model

import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.strategy.TouchLockManager
import com.ccwait.touchguard.system.DefaultHapticFeedbackService
import com.ccwait.touchguard.system.adaptation.VendorDeviceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PhysicalKeyUnlockHandler {
    private var lastKeyPressTime: Long = 0
    private var keyPressCount: Int = 0
    private var lastKeyCode: Int = 0

    // 按键物理按下状态跟踪（防止未释放就触发后续点击）
    @Volatile
    private var isVolDownPressed: Boolean = false
    @Volatile
    private var isVolUpPressed: Boolean = false

    // 针对 MediaSession 等系统无 UP 事件回调的长按防误触机制
    private var pendingUnlockJob: Job? = null
    private var lastMediaSessionAdjustTime: Long = 0
    private var isMediaSessionLongPressing: Boolean = false

    private const val MIN_CLICK_INTERVAL_MS = VendorDeviceHelper.PHYSICAL_KEY_DEBOUNCE_MS // 两次独立物理点击的最小真实间隔（滤除机械抖动与并发派发）
    private const val MAX_CLICK_INTERVAL_MS = 1000L // 双击超时
    private const val MAX_TRIPLE_INTERVAL_MS = 1200L // 三击超时
    private const val MAX_COMBO_INTERVAL_MS = 1500L // 组合键超时

    fun reset() {
        pendingUnlockJob?.cancel()
        pendingUnlockJob = null
        lastKeyPressTime = 0
        keyPressCount = 0
        lastKeyCode = 0
        isVolDownPressed = false
        isVolUpPressed = false
        isMediaSessionLongPressing = false
    }

    /**
     * 按键抬起通知（来自 Activity.dispatchKeyEvent 等能捕获真实 UP 事件的层）
     */
    fun onKeyUp(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolDownPressed = false
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolUpPressed = false
        }
        isMediaSessionLongPressing = false
    }

    /**
     * 来自 Activity.dispatchKeyEvent 的前台实体按键调用
     */
    fun onKeyDown(context: Context, keyCode: Int): Boolean {
        if (!TouchLockManager.isTouchLocked) return false

        // 状态检查：如果当前按键处于未释放状态（持续按下），说明是长按/自动重复，坚决忽略
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (isVolDownPressed) return true
            isVolDownPressed = true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (isVolUpPressed) return true
            isVolUpPressed = true
        }

        return processKeyAction(context, keyCode, isFromMediaSession = false)
    }

    /**
     * 来自 MediaSession 的后台全局音量键回调（direction < 0 为音量减，direction > 0 为音量加）
     */
    fun onVolumeAdjust(context: Context, direction: Int) {
        if (!TouchLockManager.isTouchLocked) return
        if (direction == 0) {
            pendingUnlockJob?.cancel()
            pendingUnlockJob = null
            return
        }
        val now = System.currentTimeMillis()
        val delta = now - lastMediaSessionAdjustTime
        lastMediaSessionAdjustTime = now

        // 如果处于长按静默期（用户持续按着按键不放），只要连发脉冲间隔小于 400ms 就持续丢弃
        if (isMediaSessionLongPressing) {
            if (delta < 400) {
                return
            } else {
                isMediaSessionLongPressing = false
            }
        }

        // 如果两次 adjust 间隔极短（< 120ms），在 Android 底层这是实体按键长按时的快速连发（auto-repeat）
        if (delta < 120) {
            isMediaSessionLongPressing = true
            pendingUnlockJob?.cancel()
            pendingUnlockJob = null
            return
        }

        val keyCode = if (direction < 0) KeyEvent.KEYCODE_VOLUME_DOWN else KeyEvent.KEYCODE_VOLUME_UP
        processKeyAction(context, keyCode, isFromMediaSession = true)
    }

    private fun processKeyAction(context: Context, keyCode: Int, isFromMediaSession: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val mechanism = AppPreferences.unlockMechanism
        val maxClickInterval = AppPreferences.keyPressWindowMs.toLong()
        val maxTripleInterval = (maxClickInterval * 1.2f).toLong()
        val maxComboInterval = (maxClickInterval * 1.5f).toLong()

        // 防抖：两次按键间隔小于 180ms 判定为按键抖动或并发派发，予以忽略
        val interval = now - lastKeyPressTime
        if (interval < MIN_CLICK_INTERVAL_MS) {
            return true
        }

        when (mechanism) {
            UnlockMechanism.DOUBLE_VOLUME_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if (interval < maxClickInterval) {
                        scheduleOrExecuteUnlock(context, "连续双击音量下键", isFromMediaSession)
                        return true
                    } else {
                        lastKeyPressTime = now
                        DefaultHapticFeedbackService.vibrate(context, 40)
                        Toast.makeText(context, "再按一次【音量减】解除锁定", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            UnlockMechanism.TRIPLE_VOLUME_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if (interval < maxTripleInterval) {
                        keyPressCount++
                        lastKeyPressTime = now
                        if (keyPressCount >= 2) {
                            scheduleOrExecuteUnlock(context, "连续三击音量下键", isFromMediaSession)
                            return true
                        } else {
                            DefaultHapticFeedbackService.vibrate(context, 40)
                            Toast.makeText(context, "还需按 1 次【音量减】解除锁定", Toast.LENGTH_SHORT).show()
                            return true
                        }
                    } else {
                        keyPressCount = 1
                        lastKeyPressTime = now
                        DefaultHapticFeedbackService.vibrate(context, 40)
                        Toast.makeText(context, "还需按 2 次【音量减】解除锁定", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            UnlockMechanism.DOUBLE_VOLUME_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (interval < maxClickInterval) {
                        scheduleOrExecuteUnlock(context, "连续双击音量上键", isFromMediaSession)
                        return true
                    } else {
                        lastKeyPressTime = now
                        DefaultHapticFeedbackService.vibrate(context, 40)
                        Toast.makeText(context, "再按一次【音量加】解除锁定", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            UnlockMechanism.VOLUME_UP_THEN_DOWN -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    lastKeyCode = KeyEvent.KEYCODE_VOLUME_UP
                    lastKeyPressTime = now
                    DefaultHapticFeedbackService.vibrate(context, 40)
                    Toast.makeText(context, "再按一次【音量减】完成组合解除", Toast.LENGTH_SHORT).show()
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if (lastKeyCode == KeyEvent.KEYCODE_VOLUME_UP && interval < maxComboInterval) {
                        scheduleOrExecuteUnlock(context, "「音量加 + 音量减」组合键", isFromMediaSession)
                        return true
                    } else {
                        reset()
                        DefaultHapticFeedbackService.vibrate(context, 40)
                        Toast.makeText(context, "需先按【音量加】再按【音量减】", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            UnlockMechanism.VOLUME_DOWN_THEN_UP -> {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    lastKeyCode = KeyEvent.KEYCODE_VOLUME_DOWN
                    lastKeyPressTime = now
                    DefaultHapticFeedbackService.vibrate(context, 40)
                    Toast.makeText(context, "再按一次【音量加】完成组合解除", Toast.LENGTH_SHORT).show()
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (lastKeyCode == KeyEvent.KEYCODE_VOLUME_DOWN && interval < MAX_COMBO_INTERVAL_MS) {
                        scheduleOrExecuteUnlock(context, "「音量减 + 音量加」组合键", isFromMediaSession)
                        return true
                    } else {
                        reset()
                        DefaultHapticFeedbackService.vibrate(context, 40)
                        Toast.makeText(context, "需先按【音量减】再按【音量加】", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun scheduleOrExecuteUnlock(context: Context, reason: String, isFromMediaSession: Boolean) {
        if (!isFromMediaSession) {
            // 来自前台 Activity（已有 repeatCount == 0 和 ACTION_UP 校验），即时执行解锁
            reset()
            triggerUnlock(context, reason)
        } else {
            // 来自 MediaSession（系统缺乏松手回调）：延迟 220ms 确认
            // 若用户属于长按未放，系统连发脉冲会在 150ms 左右到达并触发取消，绝不误解锁
            pendingUnlockJob?.cancel()
            pendingUnlockJob = CoroutineScope(Dispatchers.Main).launch {
                delay(220)
                pendingUnlockJob = null
                reset()
                triggerUnlock(context, reason)
            }
        }
    }

    /**
     * 来自底层 evgrab 驱动的阶段性提示
     */
    fun onEvgrabStep(context: Context, current: Int, total: Int, isWrong: Boolean) {
        if (!TouchLockManager.isTouchLocked) return
        DefaultHapticFeedbackService.vibrate(context, 40)
        val text = if (isWrong) {
            when (AppPreferences.unlockMechanism) {
                UnlockMechanism.VOLUME_UP_THEN_DOWN -> "需先按【音量加】再按【音量减】"
                UnlockMechanism.VOLUME_DOWN_THEN_UP -> "需先按【音量减】再按【音量加】"
                else -> "按键顺序不符"
            }
        } else {
            val remaining = total - current
            when (AppPreferences.unlockMechanism) {
                UnlockMechanism.DOUBLE_VOLUME_DOWN -> "再按一次【音量减】解除锁定"
                UnlockMechanism.DOUBLE_VOLUME_UP -> "再按一次【音量加】解除锁定"
                UnlockMechanism.TRIPLE_VOLUME_DOWN -> "还需按 $remaining 次【音量减】解除锁定"
                UnlockMechanism.VOLUME_UP_THEN_DOWN -> "再按一次【音量减】完成组合解除"
                UnlockMechanism.VOLUME_DOWN_THEN_UP -> "再按一次【音量加】完成组合解除"
            }
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    /**
     * 来自底层 evgrab 驱动完成物理按键组合解锁
     */
    fun onEvgrabUnlocked(context: Context) {
        reset()
        val mech = AppPreferences.unlockMechanism
        AppLogManager.addLog("按键", "检测到实体按键触发应急解锁 (${mech.title})")
        CoroutineScope(Dispatchers.Main).launch {
            TouchLockManager.unlock(context, source = "物理按键")
        }
    }

    private fun triggerUnlock(context: Context, reason: String) {
        AppLogManager.addLog("按键", "检测到$reason，执行应急恢复")
        CoroutineScope(Dispatchers.Main).launch {
            TouchLockManager.unlock(context, source = "物理按键")
        }
    }
}
