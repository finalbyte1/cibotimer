package com.cibotimer.data.remote

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleDriveSyncClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Carica il backup JSON su Google Drive utilizzando le API REST v3.
     */
    suspend fun uploadBackupToDrive(
        accessToken: String,
        backupJsonString: String,
        fileName: String = "cibotimer_backup_latest.json"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Access token mancante per Google Drive."))
        }

        try {
            // Cerca prima se esiste già un file con questo nome
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='$fileName'+and+trashed=false&fields=files(id,name)"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            var existingFileId: String? = null
            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val regex = Regex(""""id"\s*:\s*"([^"]+)"""")
                    existingFileId = regex.find(body)?.groupValues?.get(1)
                }
            }

            if (existingFileId != null) {
                // Aggiorna file esistente (PATCH)
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val body = backupJsonString.toRequestBody("application/json".toMediaType())
                val patchRequest = Request.Builder()
                    .url(updateUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .patch(body)
                    .build()

                client.newCall(patchRequest).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IOException("Errore aggiornamento Google Drive: HTTP ${resp.code}"))
                    }
                    Result.success(existingFileId!!)
                }
            } else {
                // Crea nuovo file con Multipart Upload
                val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val metadataJson = """{"name": "$fileName", "mimeType": "application/json"}"""

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addFormDataPart("file", fileName, backupJsonString.toRequestBody("application/json".toMediaType()))
                    .build()

                val createRequest = Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .post(multipartBody)
                    .build()

                client.newCall(createRequest).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IOException("Errore caricamento su Google Drive: HTTP ${resp.code} - $respBody"))
                    }
                    val fileIdRegex = Regex(""""id"\s*:\s*"([^"]+)"""")
                    val newFileId = fileIdRegex.find(respBody)?.groupValues?.get(1) ?: "uploaded"
                    Result.success(newFileId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scarica l'ultimo backup da Google Drive tramite le API REST v3.
     */
    suspend fun downloadBackupFromDrive(
        accessToken: String,
        fileName: String = "cibotimer_backup_latest.json"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Access token mancante per Google Drive."))
        }

        try {
            // Cerca il file (prova prima con cibotimer, poi fallback su bodyfast)
            fun searchForFile(name: String): String? {
                val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='$name'+and+trashed=false&fields=files(id,name)&orderBy=modifiedTime desc"
                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                return client.newCall(searchRequest).execute().use { response ->
                    if (!response.isSuccessful) null
                    else {
                        val body = response.body?.string().orEmpty()
                        val regex = Regex(""""id"\s*:\s*"([^"]+)"""")
                        regex.find(body)?.groupValues?.get(1)
                    }
                }
            }

            val targetFileId = searchForFile(fileName)
                ?: if (fileName == "cibotimer_backup_latest.json") searchForFile("bodyfast_backup_latest.json") else null

            if (targetFileId == null) {
                return@withContext Result.failure(NoSuchElementException("Nessun file di backup trovato su Google Drive con nome $fileName."))
            }

            // Download file media
            val downloadUrl = "https://www.googleapis.com/drive/v3/files/$targetFileId?alt=media"
            val downloadRequest = Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(downloadRequest).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IOException("Download del backup fallito: HTTP ${resp.code}"))
                }
                val jsonString = resp.body?.string().orEmpty()
                Result.success(jsonString)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Salva una copia del backup nella cartella cache locale per condivisione/export.
     */
    fun saveLocalBackupFile(context: Context, backupJson: String): File {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(backupDir, "cibotimer_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { out ->
            out.write(backupJson.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Legge un backup JSON da un Uri (Storage Access Framework).
     */
    fun readBackupFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IOException("Impossibile aprire il file selezionato.")
    }

    /**
     * Scrive il backup su un Uri (Storage Access Framework).
     */
    fun writeBackupToUri(context: Context, uri: Uri, backupJson: String) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(backupJson)
            }
        } ?: throw IOException("Impossibile salvare il file di backup.")
    }
}
