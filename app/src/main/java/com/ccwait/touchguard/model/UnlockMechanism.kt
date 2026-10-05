package com.ccwait.touchguard.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ccwait.touchguard.R

enum class UnlockMechanism(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val promptTip: String,
    @StringRes val titleRes: Int,
    @StringRes val shortNameRes: Int,
    @StringRes val descRes: Int,
    @StringRes val promptTipRes: Int
) {
    DOUBLE_VOLUME_DOWN(
        id = "double_volume_down",
        title = "双击音量下键",
        shortName = "双击音量-",
        description = "连续快速按两次音量减键瞬间解除（推荐）",
        promptTip = "双击音量下键解除",
        titleRes = R.string.unlock_double_vol_down_title,
        shortNameRes = R.string.unlock_double_vol_down_short,
        descRes = R.string.unlock_double_vol_down_desc,
        promptTipRes = R.string.unlock_double_vol_down_tip
    ),
    TRIPLE_VOLUME_DOWN(
        id = "triple_volume_down",
        title = "三击音量下键",
        shortName = "三击音量-",
        description = "连续快速按三次音量减键，极佳防误触",
        promptTip = "连按三次音量下键解除",
        titleRes = R.string.unlock_triple_vol_down_title,
        shortNameRes = R.string.unlock_triple_vol_down_short,
        descRes = R.string.unlock_triple_vol_down_desc,
        promptTipRes = R.string.unlock_triple_vol_down_tip
    ),
    DOUBLE_VOLUME_UP(
        id = "double_volume_up",
        title = "双击音量上键",
        shortName = "双击音量+",
        description = "连续快速按两次音量加键瞬间解除",
        promptTip = "双击音量上键解除",
        titleRes = R.string.unlock_double_vol_up_title,
        shortNameRes = R.string.unlock_double_vol_up_short,
        descRes = R.string.unlock_double_vol_up_desc,
        promptTipRes = R.string.unlock_double_vol_up_tip
    ),
    VOLUME_UP_THEN_DOWN(
        id = "volume_up_then_down",
        title = "音量加 + 音量减",
        shortName = "音量+ 接着 音量-",
        description = "先按一次音量加，紧接着按一次音量减组合解除",
        promptTip = "先按音量+再按音量-解除",
        titleRes = R.string.unlock_vol_up_down_title,
        shortNameRes = R.string.unlock_vol_up_down_short,
        descRes = R.string.unlock_vol_up_down_desc,
        promptTipRes = R.string.unlock_vol_up_down_tip
    ),
    VOLUME_DOWN_THEN_UP(
        id = "volume_down_then_up",
        title = "音量减 + 音量加",
        shortName = "音量- 接着 音量+",
        description = "先按一次音量减，紧接着按一次音量加组合解除",
        promptTip = "先按音量-再按音量+解除",
        titleRes = R.string.unlock_vol_down_up_title,
        shortNameRes = R.string.unlock_vol_down_up_short,
        descRes = R.string.unlock_vol_down_up_desc,
        promptTipRes = R.string.unlock_vol_down_up_tip
    );

    @Composable
    fun localizedTitle(): String = stringResource(titleRes)

    @Composable
    fun localizedShortName(): String = stringResource(shortNameRes)

    @Composable
    fun localizedDescription(): String = stringResource(descRes)

    @Composable
    fun localizedPromptTip(): String = stringResource(promptTipRes)

    fun getTitle(context: Context): String = context.getString(titleRes)
    fun getShortName(context: Context): String = context.getString(shortNameRes)
    fun getDescription(context: Context): String = context.getString(descRes)
    fun getPromptTip(context: Context): String = context.getString(promptTipRes)

    companion object {
        fun fromId(id: String?): UnlockMechanism {
            return entries.firstOrNull { it.id == id } ?: DOUBLE_VOLUME_DOWN
        }
    }
}
