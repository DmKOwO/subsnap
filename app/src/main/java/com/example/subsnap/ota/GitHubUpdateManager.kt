package com.example.subsnap.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.subsnap.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GitHubUpdateManager(private val context: Context) {

    sealed interface DownloadState {
        object Idle : DownloadState
        data class Downloading(val progressPercent: Int) : DownloadState
        data class ReadyToInstall(val apkFile: File) : DownloadState
        data class Error(val message: String) : DownloadState
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    suspend fun checkForUpdates(repoSlug: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val cleanSlug = repoSlug.trim().trim('/')
        if (cleanSlug.isBlank() || !cleanSlug.contains('/')) {
            return@withContext Result.failure(
                IllegalArgumentException("Укажите репозиторий GitHub в формате 'owner/repo' (например: dmk/subsnap)")
            )
        }

        val apiUrl = "https://api.github.com/repos/$cleanSlug/releases/latest"
        try {
            val url = URL(apiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SubSnap-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = conn.responseCode
            if (responseCode == 404) {
                return@withContext Result.failure(
                    Exception("Релизов в репозитории $cleanSlug пока не найдено (404)")
                )
            } else if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    Exception("Ошибка GitHub API ($responseCode)")
                )
            }

            val responseBody = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val releaseObj = JSONObject(responseBody)

            val tagName = releaseObj.optString("tag_name", "").trim()
            val releaseNotes = releaseObj.optString("body", "")
            val htmlUrl = releaseObj.optString("html_url", "https://github.com/$cleanSlug/releases")
            val publishedAt = releaseObj.optString("published_at", "")

            // Find .apk in assets
            var apkDownloadUrl: String? = null
            val assetsArray = releaseObj.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url").ifBlank { null }
                        break
                    }
                }
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val isAvailable = isNewerVersion(tagName, currentVersion)

            val info = AppUpdateInfo(
                tagName = tagName,
                versionName = tagName.removePrefix("v").removePrefix("V"),
                changelog = releaseNotes,
                apkDownloadUrl = apkDownloadUrl,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt,
                isUpdateAvailable = isAvailable,
                currentVersion = currentVersion
            )

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check updates from GitHub", e)
            Result.failure(e)
        }
    }

    suspend fun downloadApk(downloadUrl: String): Result<File> = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(0)
        try {
            val destinationDir = File(context.filesDir, "updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(destinationDir, "subsnap_update.apk")
            if (apkFile.exists()) apkFile.delete()

            var currentUrl = downloadUrl
            var redirectConn: HttpURLConnection? = null
            var redirectCount = 0
            while (redirectCount < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "SubSnap-Android-App")
                    connectTimeout = 30000
                    readTimeout = 30000
                }
                val code = conn.responseCode
                if (code in listOf(HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    }
                }
                redirectConn = conn
                break
            }

            val finalConn = redirectConn ?: throw IllegalStateException("Слишком много перенаправлений при скачивании APK")
            val totalBytes = finalConn.contentLengthLong
            var downloadedBytes = 0L

            finalConn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                            _downloadState.value = DownloadState.Downloading(percent)
                        }
                    }
                    output.flush()
                }
            }

            _downloadState.value = DownloadState.ReadyToInstall(apkFile)
            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update APK", e)
            _downloadState.value = DownloadState.Error(e.message ?: "Ошибка скачивания")
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("Файл APK обновления не найден"))
            }

            // Check permission to install unknown apps on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(
                        SecurityException("Разрешите установку обновлений из этого источника и повторите попытку.")
                    )
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
            Result.failure(e)
        }
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    companion object {
        private const val TAG = "GitHubUpdateManager"

        @Volatile
        private var instance: GitHubUpdateManager? = null

        fun getInstance(context: Context): GitHubUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: GitHubUpdateManager(context.applicationContext).also { instance = it }
            }
        }

        fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
            try {
                val cleanRemote = remoteTag.removePrefix("v").removePrefix("V").trim()
                val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

                if (cleanRemote == cleanCurrent) return false

                val remoteParts = cleanRemote.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
                val currentParts = cleanCurrent.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

                val maxLen = maxOf(remoteParts.size, currentParts.size)
                for (i in 0 until maxLen) {
                    val r = remoteParts.getOrElse(i) { 0 }
                    val c = currentParts.getOrElse(i) { 0 }
                    if (r > c) return true
                    if (r < c) return false
                }
                return false
            } catch (e: Exception) {
                return false
            }
        }
    }
}
