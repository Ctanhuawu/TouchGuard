package com.ccwait.touchguard.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val isManualCheck: Boolean = false,
    val isDebugAsset: Boolean = BuildConfig.DEBUG
)

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val REPO_OWNER = "Ctanhuawu"
    private const val REPO_NAME = "TouchGuard"

    var activeUpdate by mutableStateOf<UpdateInfo?>(null)
        private set

    var isChecking by mutableStateOf(false)
        private set

    fun dismiss() {
        activeUpdate = null
    }

    private var downloadReceiver: BroadcastReceiver? = null

    fun openDownload(context: Context, updateInfo: UpdateInfo) {
        startSystemDownload(context, updateInfo)
    }

    private fun startSystemDownload(context: Context, updateInfo: UpdateInfo) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                openBrowserDownload(context, updateInfo)
                return
            }

            val downloadUri = Uri.parse(updateInfo.downloadUrl)
            val fileName = if (updateInfo.isDebugAsset) {
                "TouchGuard-v${updateInfo.versionName}-debug.apk"
            } else {
                "TouchGuard-v${updateInfo.versionName}.apk"
            }
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val appTitle = if (updateInfo.isDebugAsset) {
                "${context.getString(R.string.app_name)} (Debug) v${updateInfo.versionName}"
            } else {
                "${context.getString(R.string.app_name)} v${updateInfo.versionName}"
            }

            val request = DownloadManager.Request(downloadUri).apply {
                setTitle(appTitle)
                setDescription(context.getString(R.string.update_downloading))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/vnd.android.package-archive")
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, context.getString(R.string.update_downloading), Toast.LENGTH_SHORT).show()

            registerDownloadReceiver(context.applicationContext, downloadId, destinationFile)
        } catch (e: Exception) {
            AppLogManager.addLog("更新", "系统下载器启动失败，降级调用浏览器: ${e.message}", isWarning = true)
            Toast.makeText(context, context.getString(R.string.update_download_failed), Toast.LENGTH_SHORT).show()
            openBrowserDownload(context, updateInfo)
        }
    }

    private fun registerDownloadReceiver(appContext: Context, targetDownloadId: Long, targetApkFile: File) {
        downloadReceiver?.let {
            runCatching { appContext.unregisterReceiver(it) }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == targetDownloadId) {
                        runCatching { appContext.unregisterReceiver(this) }
                        downloadReceiver = null

                        if (targetApkFile.exists() && targetApkFile.length() > 0) {
                            Toast.makeText(appContext, appContext.getString(R.string.update_install_hint), Toast.LENGTH_SHORT).show()
                            installApk(appContext, targetApkFile)
                        }
                    }
                }
            }
        }
        downloadReceiver = receiver

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun installApk(context: Context, apkFile: File) {
        runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        }.onFailure { e ->
            AppLogManager.addLog("更新", "拉起安装器失败: ${e.message}", isWarning = true)
        }
    }

    fun openBrowserDownload(context: Context, updateInfo: UpdateInfo) {
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

            val assetsArray = releaseObj.optJSONArray("assets")
            val (downloadUrl, isDebugAsset) = findMatchingAsset(assetsArray, isDebug = BuildConfig.DEBUG)
                ?: (buildDownloadUrl(tagName, isDebug = BuildConfig.DEBUG) to BuildConfig.DEBUG)

            return UpdateInfo(
                tagName = tagName,
                versionName = tagName.removePrefix("v").removePrefix("V"),
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releasePageUrl = releasePageUrl,
                isManualCheck = isManual,
                isDebugAsset = isDebugAsset
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
                    val downloadUrl = buildDownloadUrl(tagName, isDebug = BuildConfig.DEBUG)
                    return UpdateInfo(
                        tagName = tagName,
                        versionName = tagName.removePrefix("v").removePrefix("V"),
                        releaseNotes = "",
                        downloadUrl = downloadUrl,
                        releasePageUrl = location,
                        isManualCheck = isManual,
                        isDebugAsset = BuildConfig.DEBUG
                    )
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun findMatchingAsset(assetsArray: JSONArray?, isDebug: Boolean): Pair<String, Boolean>? {
        if (assetsArray == null || assetsArray.length() == 0) return null

        val apkAssets = mutableListOf<Pair<String, String>>()
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.optJSONObject(i) ?: continue
            val name = asset.optString("name", "")
            val downloadUrl = asset.optString("browser_download_url", "")
            if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                apkAssets.add(name to downloadUrl)
            }
        }

        if (apkAssets.isEmpty()) return null

        return if (isDebug) {
            val debugAsset = apkAssets.firstOrNull { it.first.contains("debug", ignoreCase = true) }
            if (debugAsset != null) {
                debugAsset.second to true
            } else {
                apkAssets.first().second to false
            }
        } else {
            val releaseAsset = apkAssets.firstOrNull { it.first.contains("release", ignoreCase = true) }
                ?: apkAssets.firstOrNull { !it.first.contains("debug", ignoreCase = true) }
            if (releaseAsset != null) {
                releaseAsset.second to false
            } else {
                apkAssets.first().second to false
            }
        }
    }

    private fun buildDownloadUrl(tagName: String, isDebug: Boolean = BuildConfig.DEBUG): String {
        val apkName = if (isDebug) "TouchGuard-debug.apk" else "TouchGuard-release.apk"
        return "https://github.com/$REPO_OWNER/$REPO_NAME/releases/download/$tagName/$apkName"
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
