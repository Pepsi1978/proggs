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

    suspend fun restoreAllPhotos(targetDir: File): Int =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService() ?: return@withContext 0

                val files =
                    driveService
                        .files()
                        .list()
                        .setSpaces("appDataFolder")
                        .setQ("name contains 'photo_'")
                        .setFields("files(id, name)")
                        .setPageSize(100)
                        .execute()

                var count = 0
                files.files?.forEach { driveFile ->
                    val localName = driveFile.name.removePrefix("photo_")
                    val localFile = File(targetDir, localName)
                    if (!localFile.exists()) {
                        try {
                            FileOutputStream(localFile).use { os ->
                                driveService.files().get(driveFile.id).executeMediaAndDownloadTo(os)
                            }
                            count++
                            android.util.Log.d("DriveRestore", "Restored: $localName (${localFile.length()} bytes)")
                        } catch (e: Exception) {
                            android.util.Log.e("DriveRestore", "Failed: $localName: ${e.message}")
                        }
                    }
                }
                count
            } catch (e: Exception) {
                android.util.Log.e("DriveRestore", "restoreAllPhotos FAILED: ${e.message}", e)
                0
            }
        }
}
