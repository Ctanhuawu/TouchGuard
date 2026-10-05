package com.ccwait.touchguard.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ccwait.touchguard.BuildConfig
import com.ccwait.touchguard.R
import com.ccwait.touchguard.AppPreferences
import com.ccwait.touchguard.model.AppLogManager
import com.ccwait.touchguard.model.UpdateChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val isManualCheck: Boolean = false
)

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val REPO_OWNER = "Ctanhuawu"
    private const val REPO_NAME = "TouchGuard"
    private const val APK_NAME = "TouchGuard-release.apk"

    var activeUpdate by mutableStateOf<UpdateInfo?>(null)
        private set

    var isChecking by mutableStateOf(false)
        private set

    fun dismiss() {
        activeUpdate = null
    }

    fun openDownload(context: Context, updateInfo: UpdateInfo) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { e ->
            AppLogManager.addLog("更新", "无法打开下载链接: ${e.message}", isWarning = true)
            openReleasePage(context, updateInfo)
        }
    }

    fun openReleasePage(context: Context, updateInfo: UpdateInfo) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.releasePageUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { e ->
            AppLogManager.addLog("更新", "无法打开发布页面: ${e.message}", isWarning = true)
            Toast.makeText(context, context.getString(R.string.update_check_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun checkUpdate(context: Context, isManual: Boolean = false) {
        if (isChecking) return

        isChecking = true
        if (isManual) {
            Toast.makeText(context, context.getString(R.string.update_checking), Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = AppPreferences.updateChannel
                val info = fetchLatestRelease(channel, isManual)

                withContext(Dispatchers.Main) {
                    isChecking = false
                    if (info != null && isNewerVersion(info.tagName, BuildConfig.VERSION_NAME)) {
                        activeUpdate = info
                    } else if (isManual) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.update_already_latest, BuildConfig.VERSION_NAME),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                AppLogManager.addLog("更新", "检查更新失败: ${e.message}", isWarning = true)
                withContext(Dispatchers.Main) {
                    isChecking = false
                    if (isManual) {
                        Toast.makeText(context, context.getString(R.string.update_check_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun fetchLatestRelease(channel: UpdateChannel, isManual: Boolean): UpdateInfo? {
        // 方案 1：优先请求 GitHub API 获取完整的 Tag、Changelog 与直链
        val apiResult = runCatching { fetchViaApi(channel, isManual) }.getOrNull()
        if (apiResult != null) return apiResult

        // 方案 2：当 API 遇到 GitHub 未登录访问频次限制 (403 Rate Limit) 时，通过 302 重定向解析 Tag
        return runCatching { fetchViaRedirect(channel, isManual) }.getOrNull()
    }

    private fun fetchViaApi(channel: UpdateChannel, isManual: Boolean): UpdateInfo? {
        val targetUrl = when (channel) {
            UpdateChannel.BETA -> "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
            else -> "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        }

        val url = URL(targetUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "TouchGuard-App/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                AppLogManager.addLog("更新", "GitHub API 返回 HTTP $code", isWarning = true)
                return null
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = if (channel == UpdateChannel.BETA) {
                val array = JSONArray(body)
                if (array.length() == 0) return null
                array.getJSONObject(0)
            } else {
                JSONObject(body)
            }

            val tagName = releaseObj.optString("tag_name").trim()
            if (tagName.isEmpty()) return null

            val releaseNotes = releaseObj.optString("body").trim()
            val releasePageUrl = releaseObj.optString("html_url", "https://github.com/$REPO_OWNER/$REPO_NAME/releases/tag/$tagName")

            val downloadUrl = buildDownloadUrl(tagName, channel)

            return UpdateInfo(
                tagName = tagName,
                versionName = tagName.removePrefix("v").removePrefix("V"),
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releasePageUrl = releasePageUrl,
                isManualCheck = isManual
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchViaRedirect(channel: UpdateChannel, isManual: Boolean): UpdateInfo? {
        val url = URL("https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            instanceFollowRedirects = false
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "TouchGuard-App/${BuildConfig.VERSION_NAME}")
        }

        try {
            val code = conn.responseCode
            val location = conn.getHeaderField("Location")
            if ((code == 301 || code == 302 || code == 307) && !location.isNullOrBlank()) {
                val tagName = location.substringAfterLast("/tag/").substringAfterLast("/")
                if (tagName.isNotBlank()) {
                    val downloadUrl = buildDownloadUrl(tagName, channel)
                    return UpdateInfo(
                        tagName = tagName,
                        versionName = tagName.removePrefix("v").removePrefix("V"),
                        releaseNotes = "发现 TouchGuard 最新发布版本，点击下方按钮即可直接下载 APK 安装包更新。",
                        downloadUrl = downloadUrl,
                        releasePageUrl = location,
                        isManualCheck = isManual
                    )
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun buildDownloadUrl(tagName: String, channel: UpdateChannel): String {
        val rawUrl = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/download/$tagName/$APK_NAME"
        return if (channel == UpdateChannel.MIRROR) {
            "https://ghproxy.net/$rawUrl"
        } else {
            rawUrl
        }
    }

    fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanLocal = localVersion.trim().removePrefix("v").removePrefix("V")

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
