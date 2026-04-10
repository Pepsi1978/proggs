package com.bestjournal.app.data.remote.googledrive

import android.accounts.Account
import android.content.Context
import android.content.SharedPreferences
import com.bestjournal.app.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DriveRestoreManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
) {
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
                android.util.Log.e(
                    "DriveRestore",
                    "getDriveService FAILED: ${e.javaClass.simpleName}: ${e.message}",
                    e,
                )
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
                val driveService = getDriveService()
                if (driveService == null) {
                    android.util.Log.e(
                        "DriveRestore",
                        "restoreAllPhotos: DriveService is null — not signed in or token error",
                    )
                    return@withContext 0
                }

                // First pass: collect all files to download (for total count)
                val filesToDownload = mutableListOf<com.google.api.services.drive.model.File>()
                var pageToken: String? = null
                do {
                    val request =
                        driveService
                            .files()
                            .list()
                            .setSpaces("appDataFolder")
                            .setQ("name contains 'photo_'")
                            .setFields("nextPageToken, files(id, name)")
                            .setPageSize(100)
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

                val total = filesToDownload.size
                android.util.Log.d("DriveRestore", "Files to download: $total")
                onProgress?.invoke(0, total)

                // Second pass: download files with progress
                var count = 0
                for (driveFile in filesToDownload) {
                    val localName = driveFile.name.removePrefix("photo_")
                    val localFile = File(targetDir, localName)
                    try {
                        FileOutputStream(localFile).use { os ->
                            driveService.files().get(driveFile.id).executeMediaAndDownloadTo(os)
                        }
                        count++
                        onProgress?.invoke(count, total)
                        android.util.Log.d(
                            "DriveRestore",
                            "Restored ($count/$total): $localName (${localFile.length()} bytes)",
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("DriveRestore", "Failed: $localName: ${e.message}")
                    }
                }

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
