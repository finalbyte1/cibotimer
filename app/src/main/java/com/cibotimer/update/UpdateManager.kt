package com.cibotimer.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.cibotimer.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val apkName: String,
    val size: String,
    val releaseNotes: String = ""
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progressPercent: Int, val info: UpdateInfo) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val apkUri: Uri, val info: UpdateInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateManager(private val context: Context) {

    val isPlayStoreBuild: Boolean = BuildConfig.IS_PLAY_STORE_BUILD

    private val prefs = context.getSharedPreferences("cibotimer_update_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val OFFICIAL_SERVER_URL = "https://cibotimer.duckdns.org"
    }

    var serverUrl: String
        get() {
            val saved = prefs.getString("server_url", OFFICIAL_SERVER_URL)
            // L'utente richiede espressamente di usare SEMPRE https://cibotimer.duckdns.org
            return if (saved.isNullOrBlank() || !saved.startsWith("https://") || !saved.contains("cibotimer.duckdns.org")) {
                OFFICIAL_SERVER_URL
            } else {
                saved
            }
        }
        set(value) {
            val url = if (value.isBlank() || !value.startsWith("https://") || !value.contains("cibotimer.duckdns.org")) {
                OFFICIAL_SERVER_URL
            } else {
                value.trim().removeSuffix("/")
            }
            prefs.edit().putString("server_url", url).apply()
        }

    var autoCheckEnabled: Boolean
        get() = prefs.getBoolean("auto_check", true)
        set(value) = prefs.edit().putBoolean("auto_check", value).apply()

    private fun fetchVersionJson(urlStr: String): JSONObject? {
        return try {
            Log.d("UpdateManager", "Tentativo controllo da: $urlStr")
            val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "CiboTimer-Android/${BuildConfig.VERSION_NAME}")
            }
            val code = connection.responseCode
            Log.d("UpdateManager", "Response code: $code per $urlStr")
            if (code == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("UpdateManager", "Tentativo fallito per $urlStr: ${e.message}", e)
            null
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        scope.launch {
            try {
                _updateState.value = UpdateState.Checking
                val baseUrl = serverUrl.removeSuffix("/")

                Log.d("UpdateManager", "Controllo aggiornamenti HTTPS da: $baseUrl/api/version")
                val json = fetchVersionJson("$baseUrl/api/version")

                if (json != null) {
                    val remoteVersionCode = json.optInt("versionCode", 0)
                    val remoteVersionName = json.optString("versionName", json.optString("name", "Nuova Versione"))
                    val downloadUrl = json.optString("downloadUrl", "$baseUrl/download")
                    val apkName = json.optString("name", "CiboTimer.apk")
                    val size = json.optString("size", "")
                    val releaseNotes = json.optString("releaseNotes", "Nuova versione con miglioramenti e correzioni.")

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    Log.d("UpdateManager", "Server VersionCode: $remoteVersionCode, Locale: $currentVersionCode")

                    if (remoteVersionCode > currentVersionCode) {
                        val updateInfo = UpdateInfo(
                            versionName = remoteVersionName,
                            versionCode = remoteVersionCode,
                            downloadUrl = downloadUrl,
                            apkName = apkName,
                            size = size,
                            releaseNotes = releaseNotes
                        )
                        _updateState.value = UpdateState.Available(updateInfo)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                } else {
                    if (manual) {
                        _updateState.value = UpdateState.Error("Server non raggiungibile via HTTPS ($baseUrl)")
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.w("UpdateManager", "Errore durante controllo aggiornamenti: ${e.message}")
                if (manual) {
                    _updateState.value = UpdateState.Error("Impossibile connettersi al server (${e.localizedMessage ?: "timeout"})")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    private fun downloadFile(urlStr: String, destination: File, onProgress: (Int) -> Unit): Boolean {
        return try {
            val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 30000
                requestMethod = "GET"
            }
            if (connection.responseCode != 200) return false
            val totalLength = connection.contentLength
            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalLength > 0) {
                            val percent = ((downloaded * 100) / totalLength).toInt()
                            onProgress(percent)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w("UpdateManager", "Download fallito da $urlStr: ${e.message}")
            false
        }
    }

    fun openPlayStore() {
        val packageName = context.packageName
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    fun downloadAndInstall(info: UpdateInfo) {
        if (isPlayStoreBuild) {
            openPlayStore()
            dismissBanner()
            return
        }

        scope.launch {
            try {
                _updateState.value = UpdateState.Downloading(0, info)

                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updatesDir, "CiboTimer-update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                // Assicura che il download avvenga sempre via HTTPS su cibotimer.duckdns.org
                val secureDownloadUrl = if (info.downloadUrl.startsWith("https://")) {
                    info.downloadUrl
                } else {
                    "${OFFICIAL_SERVER_URL}/download"
                }

                Log.d("UpdateManager", "Avvio download APK HTTPS da: $secureDownloadUrl")
                val success = downloadFile(secureDownloadUrl, apkFile) { percent ->
                    _updateState.value = UpdateState.Downloading(percent, info)
                }

                if (!success) {
                    throw Exception("Download non riuscito tramite HTTPS ($secureDownloadUrl)")
                }

                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                _updateState.value = UpdateState.ReadyToInstall(apkFile, apkUri, info)

                withContext(Dispatchers.Main) {
                    triggerInstall(apkUri)
                }

            } catch (e: Exception) {
                Log.e("UpdateManager", "Errore download APK: ${e.message}", e)
                _updateState.value = UpdateState.Error("Errore download: ${e.localizedMessage}")
            }
        }
    }

    fun triggerInstall(apkUri: Uri) {
        if (isPlayStoreBuild) {
            openPlayStore()
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Errore avvio installazione: ${e.message}", e)
            _updateState.value = UpdateState.Error("Impossibile avviare l'installazione: ${e.localizedMessage}")
        }
    }

    fun dismissBanner() {
        _updateState.value = UpdateState.Idle
    }
}
