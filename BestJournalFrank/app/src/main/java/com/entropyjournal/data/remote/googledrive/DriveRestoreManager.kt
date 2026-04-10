package com.entropyjournal.data.remote.googledrive

import android.accounts.Account
import android.content.Context
import android.content.SharedPreferences
import com.entropyjournal.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Singleton
class DriveRestoreManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
) {
    // Reuse the same authenticated Drive service within a restore session
    // (avoids redundant GoogleAuthUtil.getToken() calls)
    @Volatile private var sessionService: Drive? = null

    private suspend fun getSessionDriveService(): Drive? {
        sessionService?.let {
            return it
        }
        return getDriveService()?.also { sessionService = it }
    }

    /** Clear cached service after a restore session is complete. */
    fun endSession() {
        sessionService = null
    }

    private suspend fun getDriveService(): Drive? =
        withContext(Dispatchers.IO) {
            val accountEmail =
                encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null)
                    ?: return@withContext null

            if (accountEmail.isBlank()) return@withContext null

            try {
                val account = Account(accountEmail, "com.google")
                val scope = "oauth2:${DriveScopes.DRIVE_APPDATA}"
                val token = GoogleAuthUtil.getToken(context, account, scope)

                Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance()) { request ->
                        request.headers.authorization = "Bearer $token"
                    }
                    .setApplicationName("Journal")
                    .build()
            } catch (e: Exception) {
                null
            }
        }

    suspend fun hasBackup(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService() ?: return@withContext false
                val files =
                    driveService
                        .files()
                        .list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '${Constants.DRIVE_BACKUP_FILENAME}'")
                        .setFields("files(id)")
                        .execute()
                !files.files.isNullOrEmpty()
            } catch (e: Exception) {
                false
            }
        }

    suspend fun restore(targetFile: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val driveService =
                    getDriveService()
                        ?: return@withContext Result.failure(
                            IllegalStateException("Nicht angemeldet")
                        )

                val files =
                    driveService
                        .files()
                        .list()
                        .setSpaces("appDataFolder")
                        .setQ("name = '${Constants.DRIVE_BACKUP_FILENAME}'")
                        .setFields("files(id)")
                        .execute()

                val backupFile =
                    files.files?.firstOrNull()
                        ?: return@withContext Result.failure(Exception("Kein Backup gefunden"))

                FileOutputStream(targetFile).use { outputStream ->
                    driveService.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
                }

                Result.success(Unit)
            } catch (e: UserRecoverableAuthException) {
                Result.failure(NeedConsentException(e.intent ?: android.content.Intent()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun restoreAllPhotos(
        targetDir: File,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): Int =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getSessionDriveService()
                if (driveService == null) {
                    android.util.Log.e(
                        "DriveRestore",
                        "restoreAllPhotos: DriveService is null — not signed in or token error",
                    )
                    return@withContext 0
                }

                // First pass: collect all files to download (with size for sorting)
                val filesToDownload = mutableListOf<com.google.api.services.drive.model.File>()
                var pageToken: String? = null
                do {
                    val request =
                        driveService
                            .files()
                            .list()
                            .setSpaces("appDataFolder")
                            .setQ("name contains 'photo_'")
                            .setFields("nextPageToken, files(id, name, size)")
                            .setPageSize(1000)
                    if (pageToken != null) {
                        request.pageToken = pageToken
                    }
                    val result = request.execute()
                    result.files?.forEach { driveFile ->
                        val localName = driveFile.name.removePrefix("photo_")
                        val localFile = File(targetDir, localName)
                        if (!localFile.exists()) {
                            filesToDownload.add(driveFile)
                        } else {
                            android.util.Log.d("DriveRestore", "Skipped (exists): $localName")
                        }
                    }
                    pageToken = result.nextPageToken
                } while (pageToken != null)

                // Sort: largest files first so big downloads start early across all parallel slots
                filesToDownload.sortByDescending { it.getSize() ?: 0L }

                val total = filesToDownload.size
                val totalBytes = filesToDownload.sumOf { it.getSize() ?: 0L }
                android.util.Log.d(
                    "DriveRestore",
                    "Files to download: $total (${totalBytes / 1024 / 1024} MB total)",
                )
                onProgress?.invoke(0, total)

                // Parallel downloads with Semaphore(6)
                val dlSemaphore = Semaphore(6)
                val dlCount = AtomicInteger(0)

                coroutineScope {
                    filesToDownload
                        .map { driveFile ->
                            launch {
                                dlSemaphore.withPermit {
                                    val localName = driveFile.name.removePrefix("photo_")
                                    val localFile = File(targetDir, localName)
                                    var success = false
                                    for (attempt in 0..1) { // 1 retry
                                        try {
                                            java.io
                                                .BufferedOutputStream(
                                                    FileOutputStream(localFile),
                                                    64 * 1024,
                                                )
                                                .use { os ->
                                                    driveService
                                                        .files()
                                                        .get(driveFile.id)
                                                        .executeMediaAndDownloadTo(os)
                                                }
                                            success = true
                                            break
                                        } catch (e: Exception) {
                                            localFile.delete() // clean up partial download
                                            if (attempt == 0) {
                                                android.util.Log.w(
                                                    "DriveRestore",
                                                    "Download retry $localName: ${e.message}",
                                                )
                                                kotlinx.coroutines.delay(1000)
                                            } else {
                                                throw e
                                            }
                                        }
                                    }
                                    if (success) {
                                        val current = dlCount.incrementAndGet()
                                        onProgress?.invoke(current, total)
                                        android.util.Log.d(
                                            "DriveRestore",
                                            "Restored ($current/$total): $localName (${localFile.length()} bytes)",
                                        )
                                    } else {
                                        android.util.Log.e("DriveRestore", "Failed: $localName")
                                    }
                                }
                            }
                        }
                        .joinAll()
                }

                endSession()
                val count = dlCount.get()
                android.util.Log.d(
                    "DriveRestore",
                    "restoreAllPhotos complete: $count files restored",
                )
                count
            } catch (e: Exception) {
                android.util.Log.e("DriveRestore", "restoreAllPhotos FAILED: ${e.message}", e)
                0
            }
        }
}
