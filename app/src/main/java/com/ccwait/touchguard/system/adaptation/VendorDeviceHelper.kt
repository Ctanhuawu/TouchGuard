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
}
