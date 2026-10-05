package com.ccwait.touchguard.model

import androidx.annotation.StringRes
import com.ccwait.touchguard.R

/**
 * 应用更新渠道枚举
 */
enum class UpdateChannel(
    val id: String,
    @StringRes val titleRes: Int,
    val apiUrl: String
) {
    STABLE("stable", R.string.channel_stable, "https://api.github.com/repos/Ctanhuawu/TouchGuard/releases/latest"),
    BETA("beta", R.string.channel_beta, "https://api.github.com/repos/Ctanhuawu/TouchGuard/releases"),
    MIRROR("mirror", R.string.channel_mirror, "https://ghproxy.net/https://api.github.com/repos/Ctanhuawu/TouchGuard/releases/latest");

    companion object {
        fun fromId(id: String?): UpdateChannel {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STABLE
        }
    }
}
