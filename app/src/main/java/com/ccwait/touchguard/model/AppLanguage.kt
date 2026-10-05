package com.ccwait.touchguard.model

import java.util.Locale

/**
 * 应用多语言定义枚举
 * 设计为开箱即用、高可扩展性：后续新增任何语言（如法语、日语、俄语），
 * 只需在此追加枚举项并在 res/values-<locale>/strings.xml 添加翻译，全局 UI 自动动态适配。
 */
enum class AppLanguage(
    val id: String,
    val displayName: String,
    val locale: Locale?
) {
    FOLLOW_SYSTEM("system", "跟随系统", null),
    ZH_CN("zh_cn", "简体中文", Locale.SIMPLIFIED_CHINESE),
    ZH_TW("zh_tw", "繁體中文", Locale.TRADITIONAL_CHINESE),
    EN("en", "English", Locale.ENGLISH);

    companion object {
        fun fromId(id: String?): AppLanguage {
            val normalized = id?.replace("-", "_")
            return entries.firstOrNull { it.id.equals(normalized, ignoreCase = true) } ?: FOLLOW_SYSTEM
        }
    }
}
