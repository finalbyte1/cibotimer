package com.cibotimer.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import com.cibotimer.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
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
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
    data class ReadyToInstall(val apkFile: File? = null, val apkUri: Uri? = null, val info: UpdateInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateManager(private val context: Context) {

    val isPlayStoreBuild: Boolean = BuildConfig.IS_PLAY_STORE_BUILD
    val enableLiveUpdate: Boolean = BuildConfig.ENABLE_LIVE_UPDATE

    private val prefs = context.getSharedPreferences("cibotimer_update_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    // In-App Updates Google Play
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context) }
    private var cachedAppUpdateInfo: AppUpdateInfo? = null
    private var currentUpdateInfo: UpdateInfo? = null
    private var isListenerRegistered = false

    private var attachedActivity: WeakReference<Activity>? = null
    private var attachedLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    companion object {
        const val OFFICIAL_SERVER_URL = "https://cibotimer.duckdns.org"
        const val IN_APP_UPDATE_REQUEST_CODE = 9001
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        val status = state.installStatus()
        Log.d("UpdateManager", "Google Play InstallState: status=$status, bytes=${state.bytesDownloaded()}/${state.totalBytesToDownload()}")
        when (status) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                val percent = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                val info = currentUpdateInfo ?: UpdateInfo(
                    versionName = "Google Play",
                    versionCode = cachedAppUpdateInfo?.availableVersionCode() ?: 0,
                    downloadUrl = "",
                    apkName = "Google Play",
                    size = if (totalBytes > 0) formatBytes(totalBytes) else ""
                )
                _updateState.value = UpdateState.Downloading(percent, info)
            }
            InstallStatus.DOWNLOADED -> {
                Log.d("UpdateManager", "Google Play Update DOWNLOADED - pronto per l'installazione")
                val info = currentUpdateInfo ?: UpdateInfo(
                    versionName = "Google Play",
                    versionCode = cachedAppUpdateInfo?.availableVersionCode() ?: 0,
                    downloadUrl = "",
                    apkName = "Google Play",
                    size = ""
                )
                _updateState.value = UpdateState.ReadyToInstall(
                    apkFile = null,
                    apkUri = null,
                    info = info
                )
            }
            InstallStatus.FAILED -> {
                Log.e("UpdateManager", "Google Play install fallito: ${state.installErrorCode()}")
                _updateState.value = UpdateState.Error("Download Google Play fallito (codice: ${state.installErrorCode()})")
            }
            InstallStatus.CANCELED -> {
                Log.d("UpdateManager", "Google Play update annullato dall'utente")
                _updateState.value = UpdateState.Idle
            }
            else -> {}
        }
    }

    private fun ensureListenerRegistered() {
        if (isPlayStoreBuild && !isListenerRegistered) {
            try {
                appUpdateManager.registerListener(installStateUpdatedListener)
                isListenerRegistered = true
                Log.d("UpdateManager", "InstallStateUpdatedListener registrato")
            } catch (e: Exception) {
                Log.w("UpdateManager", "Impossibile registrare InstallStateUpdatedListener: ${e.message}")
            }
        }
    }

    fun attachActivity(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>? = null) {
        attachedActivity = WeakReference(activity)
        attachedLauncher = launcher
        ensureListenerRegistered()
    }

    fun detachActivity() {
        attachedActivity = null
        attachedLauncher = null
    }

    fun cleanup() {
        if (isListenerRegistered) {
            try {
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            } catch (e: Exception) {
                Log.w("UpdateManager", "Errore deregistrazione listener: ${e.message}")
            }
            isListenerRegistered = false
        }
    }

    var serverUrl: String
        get() {
            val saved = prefs.getString("server_url", OFFICIAL_SERVER_URL)
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

    /**
     * Verifica la presenza di aggiornamenti:
     * - Se enableLiveUpdate è FALSE (isPlayStoreBuild=true), cerca su Google Play Store tramite In-App Updates API.
     * - Se enableLiveUpdate è TRUE (isPlayStoreBuild=false), cerca sul server Live Update HTTPS DuckDNS.
     */
    fun checkForUpdates(manual: Boolean = false) {
        if (isPlayStoreBuild) {
            checkForGooglePlayUpdates(manual)
        } else {
            checkForLiveServerUpdates(manual)
        }
    }

    /**
     * Verifica aggiornamenti tramite Google Play In-App Updates API.
     */
    private fun checkForGooglePlayUpdates(manual: Boolean) {
        scope.launch {
            try {
                _updateState.value = UpdateState.Checking
                ensureListenerRegistered()

                val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    cachedAppUpdateInfo = appUpdateInfo
                    val availability = appUpdateInfo.updateAvailability()
                    val installStatus = appUpdateInfo.installStatus()
                    Log.d("UpdateManager", "Google Play updateAvailability: $availability, installStatus: $installStatus")

                    if (installStatus == InstallStatus.DOWNLOADED) {
                        val info = UpdateInfo(
                            versionName = "Google Play (${appUpdateInfo.availableVersionCode()})",
                            versionCode = appUpdateInfo.availableVersionCode(),
                            downloadUrl = "",
                            apkName = "Google Play",
                            size = ""
                        )
                        currentUpdateInfo = info
                        _updateState.value = UpdateState.ReadyToInstall(null, null, info)
                        return@addOnSuccessListener
                    }

                    if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                        val targetVersionCode = appUpdateInfo.availableVersionCode()
                        val totalBytes = appUpdateInfo.totalBytesToDownload()
                        val sizeStr = if (totalBytes > 0) formatBytes(totalBytes) else ""
                        val info = UpdateInfo(
                            versionName = "Google Play (Build $targetVersionCode)",
                            versionCode = targetVersionCode,
                            downloadUrl = "https://play.google.com/store/apps/details?id=${context.packageName}",
                            apkName = "Google Play",
                            size = sizeStr,
                            releaseNotes = context.getString(com.cibotimer.R.string.update_play_store_available_desc)
                        )
                        currentUpdateInfo = info
                        _updateState.value = UpdateState.Available(info)
                    } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        val info = currentUpdateInfo ?: UpdateInfo(
                            versionName = "Google Play (${appUpdateInfo.availableVersionCode()})",
                            versionCode = appUpdateInfo.availableVersionCode(),
                            downloadUrl = "",
                            apkName = "Google Play",
                            size = ""
                        )
                        _updateState.value = UpdateState.Downloading(50, info)
                    } else if (availability == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                        _updateState.value = UpdateState.UpToDate
                    } else {
                        if (manual) {
                            _updateState.value = UpdateState.Error(
                                context.getString(com.cibotimer.R.string.update_play_store_check_error, "non disponibile")
                            )
                        } else {
                            _updateState.value = UpdateState.Idle
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.w("UpdateManager", "Verifica Google Play fallita: ${e.message}", e)
                    if (manual) {
                        _updateState.value = UpdateState.Error(
                            context.getString(
                                com.cibotimer.R.string.update_play_store_check_error,
                                e.localizedMessage ?: "connessione fallita"
                            )
                        )
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Errore inatteso verifica Google Play: ${e.message}", e)
                if (manual) {
                    _updateState.value = UpdateState.Error("Errore Google Play: ${e.localizedMessage}")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    /**
     * Verifica aggiornamenti tramite server Live Update HTTP/HTTPS esterno (DuckDNS).
     */
    private fun checkForLiveServerUpdates(manual: Boolean) {
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
                        currentUpdateInfo = updateInfo
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

    /**
     * Avvia il download e l'installazione:
     * - Se isPlayStoreBuild è true, avvia il flusso Google Play In-App Updates (senza uscire dall'app).
     * - Se isPlayStoreBuild è false, scarica il pacchetto APK dal server HTTPS e avvia l'intent di installazione.
     */
    fun downloadAndInstall(info: UpdateInfo) {
        if (isPlayStoreBuild) {
            startPlayStoreInAppUpdate()
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

    /**
     * Avvia il flusso ufficiale Google Play In-App Updates.
     * Tenta prima Flexible Update (download in background mentre si continua ad usare l'app),
     * oppure Immediate Update. Se non disponibile, effettua il fallback su openPlayStore().
     */
    fun startPlayStoreInAppUpdate(explicitActivity: Activity? = null) {
        ensureListenerRegistered()
        val cached = cachedAppUpdateInfo
        if (cached != null) {
            launchPlayStoreUpdateFlow(cached, explicitActivity)
        } else {
            _updateState.value = UpdateState.Checking
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                cachedAppUpdateInfo = info
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    launchPlayStoreUpdateFlow(info, explicitActivity)
                } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    val updateInfo = currentUpdateInfo ?: UpdateInfo(
                        versionName = "Google Play (${info.availableVersionCode()})",
                        versionCode = info.availableVersionCode(),
                        downloadUrl = "",
                        apkName = "Google Play",
                        size = ""
                    )
                    _updateState.value = UpdateState.ReadyToInstall(null, null, updateInfo)
                } else {
                    openPlayStore()
                    dismissBanner()
                }
            }.addOnFailureListener { e ->
                Log.w("UpdateManager", "Fallback su Play Store per failure info: ${e.message}")
                openPlayStore()
                dismissBanner()
            }
        }
    }

    private fun launchPlayStoreUpdateFlow(appUpdateInfo: AppUpdateInfo, explicitActivity: Activity? = null) {
        val canFlexible = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        val canImmediate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

        if (!canFlexible && !canImmediate) {
            Log.w("UpdateManager", "Né Flexible né Immediate consentiti, fallback su Google Play Store")
            openPlayStore()
            dismissBanner()
            return
        }

        // Preferisci il tipo Flexible in modo che l'utente possa continuare a usare l'app
        val updateType = if (canFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
        val options = AppUpdateOptions.newBuilder(updateType).build()

        val launcher = attachedLauncher
        val act = explicitActivity ?: attachedActivity?.get()

        var launched = false
        if (launcher != null) {
            try {
                launched = appUpdateManager.startUpdateFlowForResult(appUpdateInfo, launcher, options)
                Log.d("UpdateManager", "startUpdateFlowForResult con ActivityResultLauncher: $launched")
            } catch (e: Exception) {
                Log.e("UpdateManager", "Errore startUpdateFlowForResult con launcher: ${e.message}", e)
            }
        }

        if (!launched && act != null) {
            try {
                launched = appUpdateManager.startUpdateFlowForResult(appUpdateInfo, act, options, IN_APP_UPDATE_REQUEST_CODE)
                Log.d("UpdateManager", "startUpdateFlowForResult con Activity: $launched")
            } catch (e: Exception) {
                Log.e("UpdateManager", "Errore startUpdateFlowForResult con activity: ${e.message}", e)
            }
        }

        if (!launched) {
            Log.w("UpdateManager", "Impossibile avviare il flusso in-app, apertura Play Store")
            openPlayStore()
        }
        dismissBanner()
    }

    /**
     * Notifica della risposta dell'activity result del flusso di aggiornamento Google Play.
     */
    fun onInAppUpdateActivityResult(resultCode: Int) {
        Log.d("UpdateManager", "onInAppUpdateActivityResult: resultCode=$resultCode")
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d("UpdateManager", "Flusso In-App Update avviato con successo dall'utente")
            }
            Activity.RESULT_CANCELED -> {
                Log.d("UpdateManager", "Flusso In-App Update annullato dall'utente")
                _updateState.value = UpdateState.Idle
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                Log.e("UpdateManager", "Flusso In-App Update fallito")
                _updateState.value = UpdateState.Error(
                    context.getString(com.cibotimer.R.string.update_play_store_flow_failed)
                )
            }
        }
    }

    /**
     * Da invocare su onResume dell'Activity principale:
     * - Controlla se un aggiornamento Flexible è stato già scaricato ed è pronto per l'installazione.
     * - Se un aggiornamento Immediate era già stato avviato, riprende la visualizzazione.
     */
    fun onResume(activity: Activity? = null) {
        if (activity != null) {
            attachedActivity = WeakReference(activity)
        }
        if (isPlayStoreBuild) {
            ensureListenerRegistered()
            try {
                appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                    cachedAppUpdateInfo = appUpdateInfo
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        Log.d("UpdateManager", "onResume: In-App update già scaricato, pronto per installazione")
                        val info = currentUpdateInfo ?: UpdateInfo(
                            versionName = "Google Play (${appUpdateInfo.availableVersionCode()})",
                            versionCode = appUpdateInfo.availableVersionCode(),
                            downloadUrl = "",
                            apkName = "Google Play",
                            size = ""
                        )
                        _updateState.value = UpdateState.ReadyToInstall(null, null, info)
                    } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        Log.d("UpdateManager", "onResume: In-App update in corso, ripresa flusso")
                        val act = attachedActivity?.get() ?: activity
                        if (act != null) {
                            try {
                                appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    act,
                                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                                    IN_APP_UPDATE_REQUEST_CODE
                                )
                            } catch (e: Exception) {
                                Log.w("UpdateManager", "Impossibile riprendere immediate update: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("UpdateManager", "Errore verifica onResume: ${e.message}")
            }
        }
    }

    /**
     * Completa l'installazione dell'aggiornamento Google Play scaricato tramite Flexible Update.
     */
    fun completeInAppUpdate() {
        try {
            Log.d("UpdateManager", "Chiamata completeUpdate() su Google Play AppUpdateManager")
            appUpdateManager.completeUpdate().addOnFailureListener { e ->
                Log.e("UpdateManager", "completeUpdate fallito: ${e.message}", e)
                _updateState.value = UpdateState.Error("Impossibile completare l'installazione: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Eccezione in completeInAppUpdate: ${e.message}", e)
            _updateState.value = UpdateState.Error("Errore completamento aggiornamento: ${e.localizedMessage}")
        }
    }

    /**
     * Esegue l'installazione:
     * - Se isPlayStoreBuild è true, chiama completeInAppUpdate() per riavviare l'app e completare l'aggiornamento.
     * - Se isPlayStoreBuild è false, avvia l'installazione APK classica con FileProvider.
     */
    fun triggerInstall(apkUri: Uri? = null) {
        if (isPlayStoreBuild) {
            completeInAppUpdate()
            return
        }

        if (apkUri == null) {
            Log.e("UpdateManager", "triggerInstall chiamato con URI nullo per build non-Play Store")
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

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
