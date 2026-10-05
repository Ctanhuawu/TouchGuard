package com.ccwait.touchguard.model

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

enum class ScreenOrientationLock(
    val id: String,
    val title: String,
    val description: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val orientationValue: Int
) {
    FOLLOW_SYSTEM(
        id = "follow_system",
        title = "跟随系统方向",
        description = "不干预屏幕旋转，跟随系统重力感应自适应",
        titleRes = R.string.orient_follow_title,
        descRes = R.string.orient_follow_desc,
        orientationValue = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    ),
    LOCK_CURRENT(
        id = "lock_current",
        title = "锁定当前方向",
        description = "锁定时固定当前机身朝向，防止翻转屏幕时画面旋转",
        titleRes = R.string.orient_current_title,
        descRes = R.string.orient_current_desc,
        orientationValue = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    ),
    FORCE_PORTRAIT(
        id = "force_portrait",
        title = "强制锁定竖屏",
        description = "锁定期间强制保持常规垂直方向显示",
        titleRes = R.string.orient_portrait_title,
        descRes = R.string.orient_portrait_desc,
        orientationValue = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    ),
    FORCE_LANDSCAPE(
        id = "force_landscape",
        title = "强制锁定横屏",
        description = "锁定期间强制保持水平宽屏显示，适合看剧与游戏",
        titleRes = R.string.orient_landscape_title,
        descRes = R.string.orient_landscape_desc,
        orientationValue = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    );

    val label: String get() = title

    @Composable
    fun localizedTitle(): String = stringResource(titleRes)

    @Composable
    fun localizedDescription(): String = stringResource(descRes)

    fun getTitle(context: Context): String = context.getString(titleRes)
    fun getDescription(context: Context): String = context.getString(descRes)

    companion object {
        fun fromId(id: String?): ScreenOrientationLock {
            return entries.firstOrNull { it.id == id } ?: FOLLOW_SYSTEM
        }
    }
}
