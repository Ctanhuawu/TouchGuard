package com.ccwait.touchguard.system.adaptation

import android.os.Build

/**
 * 设备与厂商魔改系统适配助手
 * 集中管理针对特定 OEM 厂商（如小米/HyperOS/MIUI）或 Android 版本的系统级兼容策略与常数
 */
object VendorDeviceHelper {

    /**
     * 控制中心与通知栏展开后，SystemUI 开始收起滑移动画所需的平滑延时（毫秒）
     * 作用：防止 Layer 31 (TYPE_ACCESSIBILITY_OVERLAY) 顶层拦截窗口在折叠开始瞬间夺焦，
     * 从而导致 HyperOS / MIUI 等厂商魔改 SystemUI 的收起动画被异常阻断卡死在半空
     */
    const val SYSTEMUI_COLLAPSE_ANIMATION_DELAY_MS = 120L

    /**
     * 物理按键（如音量键）真实点击最小机械间隔（毫秒）
     * 作用：滤除机械按键微动抖动以及系统并发重复派发
     */
    const val PHYSICAL_KEY_DEBOUNCE_MS = 100L

    /**
     * 是否运行于小米 / HyperOS / MIUI 系统环境
     */
    val isXiaomiOrHyperOS: Boolean by lazy {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                manufacturer.contains("redmi") || brand.contains("redmi") ||
                manufacturer.contains("poco") || brand.contains("poco")
    }

    /**
     * 获取设备运行环境摘要
     */
    fun getDeviceSummary(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
    }

    /**
     * 安全读取系统属性 (SystemProperties)
     */
    fun getSystemProperty(key: String): String {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, key, "") as? String)?.trim().orEmpty()
        }.getOrDefault("")
    }

    /**
     * 获取厂商操作系统（OS）名称与版本，如 HyperOS、MIUI、ColorOS、OriginOS、HarmonyOS、One UI 等
     */
    fun getOsVersion(): String {
        // 1. 小米 / 澎湃 OS (HyperOS) & MIUI
        val miuiVersionCode = getSystemProperty("ro.miui.ui.version.name")
        val hyperOsVersion = getSystemProperty("ro.mi.os.version.name")
        val miuiIncremental = getSystemProperty("ro.build.version.incremental")

        if (hyperOsVersion.isNotEmpty()) {
            return "Xiaomi HyperOS $hyperOsVersion"
        } else if (miuiVersionCode.equals("V816", ignoreCase = true) || miuiIncremental.startsWith("OS", ignoreCase = true)) {
            val displayVer = if (miuiIncremental.startsWith("OS", ignoreCase = true)) miuiIncremental else "1.0 ($miuiVersionCode)"
            return "Xiaomi HyperOS $displayVer"
        } else if (miuiVersionCode.isNotEmpty()) {
            val cleanCode = miuiVersionCode.removePrefix("V").removePrefix("v")
            val major = if (cleanCode.length >= 2) cleanCode.substring(0, 2) else cleanCode
            return if (miuiIncremental.isNotBlank()) "MIUI $major ($miuiIncremental)" else "MIUI $major"
        }

        // 2. 华为 HarmonyOS / EMUI
        val harmonyVersion = getSystemProperty("hw_sc.build.platform.version").ifEmpty {
            getSystemProperty("ro.build.version.harmony")
        }
        if (harmonyVersion.isNotEmpty()) {
            return "HarmonyOS $harmonyVersion"
        }
        val emuiVersion = getSystemProperty("ro.build.version.emui")
        if (emuiVersion.isNotEmpty()) {
            return emuiVersion.replace("EmotionUI_", "EMUI ")
        }

        // 3. 荣耀 MagicOS
        val magicVersion = getSystemProperty("ro.build.version.magic")
        if (magicVersion.isNotEmpty()) {
            return "MagicOS $magicVersion"
        }

        // 4. OPPO / 一加 ColorOS & OxygenOS
        val oplusDisplay = getSystemProperty("ro.oplus.display.os.version")
        val colorOsVersion = getSystemProperty("ro.build.version.opporom").ifEmpty {
            getSystemProperty("ro.build.version.oplusrom")
        }
        if (oplusDisplay.isNotEmpty()) {
            return oplusDisplay
        } else if (colorOsVersion.isNotEmpty()) {
            return "ColorOS $colorOsVersion"
        }

        // 5. vivo / iQOO OriginOS / Funtouch OS
        val vivoDisplay = getSystemProperty("ro.vivo.os.build.display.id")
        val vivoOsVersion = getSystemProperty("ro.vivo.os.version")
        if (vivoDisplay.isNotEmpty()) {
            return vivoDisplay
        } else if (vivoOsVersion.isNotEmpty()) {
            return "OriginOS $vivoOsVersion"
        }

        // 6. 魅族 Flyme
        val displayId = Build.DISPLAY.orEmpty()
        if (displayId.contains("Flyme", ignoreCase = true)) {
            return displayId
        }

        // 7. 三星 One UI
        val oneUiVersion = runCatching {
            val semPlatform = Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null)
            val major = (semPlatform - 90000) / 10000
            val minor = ((semPlatform - 90000) % 10000) / 100
            "One UI $major.$minor"
        }.getOrNull()
        if (!oneUiVersion.isNullOrEmpty()) {
            return oneUiVersion
        }

        // 8. 类原生/开源第三方 ROM (LineageOS 等)
        val lineageVersion = getSystemProperty("ro.lineage.version")
        if (lineageVersion.isNotEmpty()) {
            return "LineageOS $lineageVersion"
        }

        // 9. 兜底通用展示
        return if (displayId.isNotBlank() && !displayId.equals(Build.ID, ignoreCase = true)) {
            "${Build.MANUFACTURER} ($displayId)"
        } else {
            "Android ${Build.VERSION.RELEASE}"
        }
    }
}
