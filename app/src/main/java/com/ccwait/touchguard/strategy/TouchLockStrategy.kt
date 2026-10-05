package com.ccwait.touchguard.strategy

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

enum class StrategyReadiness {
    READY,              // 环境与权限完全就绪，可随时锁定
    PERMISSION_MISSING, // 缺少必要权限（如未获 Root 授权，或未开启悬浮窗）
    UNSUPPORTED,        // 硬件或系统环境不支持（如设备无 Root）
    CHECKING            // 正在检测中
}

enum class StrategyType(
    val id: String,
    val title: String,
    val description: String,
    val technicalSpec: String,
    val tag: String,
    val requiresRoot: Boolean,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    @StringRes val specRes: Int,
    @StringRes val tagRes: Int
) {
    ROOT_EVIOCGRAB(
        id = "root_eviocgrab",
        title = "Root",
        description = "底层硬件独占，彻底阻断触控",
        technicalSpec = "Linux EVIOCGRAB 独占",
        tag = "Root",
        requiresRoot = true,
        titleRes = R.string.strat_root_title,
        descRes = R.string.strat_root_desc,
        specRes = R.string.strat_root_spec,
        tagRes = R.string.tag_root
    ),
    ACCESSIBILITY_OVERLAY(
        id = "accessibility_overlay",
        title = "无障碍",
        description = "悬浮遮罩拦截，无需 Root 权限",
        technicalSpec = "无障碍顶层遮罩",
        tag = "免 Root",
        requiresRoot = false,
        titleRes = R.string.strat_accessibility_title,
        descRes = R.string.strat_accessibility_desc,
        specRes = R.string.strat_accessibility_spec,
        tagRes = R.string.tag_no_root
    ),
    SHIZUKU_PINNING(
        id = "shizuku_pinning",
        title = "Shizuku",
        description = "特权锁定手势与下拉，保留状态栏",
        technicalSpec = "IStatusBarService 特权接管",
        tag = "免 Root",
        requiresRoot = false,
        titleRes = R.string.strat_shizuku_title,
        descRes = R.string.strat_shizuku_desc,
        specRes = R.string.strat_shizuku_spec,
        tagRes = R.string.tag_shizuku
    );

    @Composable
    fun localizedTitle(): String = stringResource(titleRes)

    @Composable
    fun localizedDescription(): String = stringResource(descRes)

    @Composable
    fun localizedTechnicalSpec(): String = stringResource(specRes)

    @Composable
    fun localizedTag(): String = stringResource(tagRes)

    fun getTitle(context: Context): String = context.getString(titleRes)
    fun getDescription(context: Context): String = context.getString(descRes)
    fun getTechnicalSpec(context: Context): String = context.getString(specRes)
    fun getTag(context: Context): String = context.getString(tagRes)

    companion object {
        fun fromId(id: String?): StrategyType {
            return entries.firstOrNull { it.id == id } ?: ROOT_EVIOCGRAB
        }
    }
}

/**
 * 屏幕/触控锁定策略规范化接口
 * 所有具体策略（Root/悬浮窗等）统一实现此规范
 */
interface TouchLockStrategy {
    val type: StrategyType
    val isLocked: Boolean

    /**
     * 当前策略就绪状态
     */
    val readiness: StrategyReadiness

    /**
     * 状态徽章文本 (如 "Root", "未授权", "免 Root", "需授权", "无 Root", "检测中")
     */
    val badgeText: String

    /**
     * 状态摘要或引导说明
     */
    val statusSummary: String

    /**
     * 检测并刷新策略环境与权限状态
     */
    suspend fun checkReadiness(context: Context, forceRequest: Boolean = false): StrategyReadiness

    /**
     * 规范化的权限申请或修复接口
     * UI 统一通过此方法发起授权或设置跳转，无须针对各方案硬编码判断
     */
    suspend fun requestPermission(context: Context): Boolean

    /**
     * 准备底层驱动/资源
     */
    suspend fun prepare(context: Context): Boolean = true

    /**
     * 执行触控锁定
     */
    suspend fun lock(context: Context): Result<Unit>

    /**
     * 解除触控锁定
     */
    suspend fun unlock(context: Context): Result<Unit>

    /**
     * 释放所有底层句柄
     */
    fun release(context: Context) {}
}
