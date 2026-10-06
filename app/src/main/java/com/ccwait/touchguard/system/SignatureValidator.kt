package com.ccwait.touchguard.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.ccwait.touchguard.BuildConfig
import java.security.MessageDigest

/**
 * 应用签名校验与防篡改检测器
 * 用于检测当前 APK 是否为官方 Release 密钥签署，识别第三方二次打包并做出告警提示
 */
object SignatureValidator {

    /**
     * 官方 Release 密钥 SHA-256 证书指纹 (全小写、无冒号十六进制字符串)
     * 对应 touchguard-release.jks
     */
    const val OFFICIAL_RELEASE_SHA256 = "d27a9b9b5075754b34033482d3a120702d412d193a428992d318f914c6518a53"

    /**
     * 获取当前安装包实际签署的证书 SHA-256 摘要
     */
    fun getCurrentSignatureSha256(context: Context): String {
        return runCatching {
            val pm = context.packageManager
            val packageName = context.packageName
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    val signers = signingInfo.apkContentsSigners
                    if (!signers.isNullOrEmpty()) {
                        signers.first().toByteArray()
                    } else {
                        signingInfo.signingCertificateHistory?.firstOrNull()?.toByteArray()
                    }
                } else null
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            }

            if (certBytes == null || certBytes.isEmpty()) return ""

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(certBytes)
            digest.joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }

    /**
     * 判定当前应用是否为第三方重新打包/非官方签名的 Release 版本
     * 注意：开发调试构建 (BuildConfig.DEBUG == true) 视为安全调试模式，不触发篡改告警
     */
    fun isTamperedOrCustom(context: Context): Boolean {
        if (BuildConfig.DEBUG) return false

        val currentSha256 = getCurrentSignatureSha256(context)
        if (currentSha256.isBlank()) return false

        return !currentSha256.equals(OFFICIAL_RELEASE_SHA256, ignoreCase = true)
    }
}
