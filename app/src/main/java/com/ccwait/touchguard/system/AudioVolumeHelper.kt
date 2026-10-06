package com.ccwait.touchguard.system

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

/**
 * 音量调节统一助手
 * 集中管理系统音量流类型判定与按键调节逻辑，避免视图与后台服务重复实现
 */
object AudioVolumeHelper {

    /**
     * 根据按键键值调节音量
     */
    fun adjustVolumeByKey(context: Context, keyCode: Int) {
        val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AudioManager.ADJUST_RAISE
        } else {
            AudioManager.ADJUST_LOWER
        }
        adjustVolume(context, direction)
    }

    /**
     * 根据方向调节音量 (direction > 0 调大, direction < 0 调小)
     */
    fun adjustVolume(context: Context, direction: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val adjustDir = if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            val streamType = if (audioManager.mode == AudioManager.MODE_IN_CALL ||
                audioManager.mode == AudioManager.MODE_IN_COMMUNICATION) {
                AudioManager.STREAM_VOICE_CALL
            } else {
                AudioManager.STREAM_MUSIC
            }
            audioManager.adjustStreamVolume(
                streamType,
                adjustDir,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } catch (_: Exception) {}
    }
}
