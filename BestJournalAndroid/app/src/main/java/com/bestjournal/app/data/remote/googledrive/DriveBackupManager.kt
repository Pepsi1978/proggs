package com.bestjournal.app.data.remote.googledrive

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.bestjournal.app.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NeedConsentException(val consentIntent: Intent) :
    Exception("Drive-Zugriff muss erlaubt werden")

@Singleton
class DriveBackupManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
) {
    private suspend fun getDriveService(): Drive =
        withContext(Dispatchers.IO) {
            val accountEmail =
                encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null)
                    ?: throw IllegalStateException("Nicht angemeldet")

            val account = Account(accountEmail, "com.google")
            val scope = "oauth2:${DriveScopes.DRIVE_APPDATA}"
            val token = GoogleAuthUtil.getToken(context, account, scope)

            Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance()) { request ->
                    request.headers.authorization = "Bearer $token"
                }
                .setApplicationName("Journal")
                .build()
        }

    suspend fun backup(databaseFile: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                uploadFile(driveService, databaseFile, Constants.DRIVE_BACKUP_FILENAME)
                encryptedPrefs
                    .edit()
                    .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                    .apply()
                Result.success(Unit)
            } catch (e: UserRecoverableAuthException) {
                android.util.Log.e("DriveBackup", "Backup needs consent: ${e.message}")
                Result.failure(NeedConsentException(e.intent ?: Intent()))
            } catch (e: Exception) {
                android.util.Log.e(
                    "DriveBackup",
                    "Backup FAILED: ${e.javaClass.simpleName}: ${e.message}",
                    e,
                )
                Result.failure(e)
            }
        }

    suspend fun backupPhotos(zipFile: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                uploadFile(driveService, zipFile, Constants.DRIVE_PHOTOS_FILENAME)
                android.util.Log.d("DriveBackup", "Photos backup: ${zipFile.length()} bytes")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("DriveBackup", "Photos backup FAILED: ${e.message}", e)
                Result.failure(e)
            }
        }

    private fun uploadFile(driveService: Drive, localFile: File, remoteName: String) {
        val existingFiles =
            driveService
                .files()
                .list()
                .setSpaces("appDataFolder")
                .setQ("name = '$remoteName'")
                .setFields("files(id)")
                .execute()
        existingFiles.files?.forEach { file -> driveService.files().delete(file.id).execute() }

        val fileMetadata =
            com.google.api.services.drive.model.File().apply {
                name = remoteName
                parents = listOf("appDataFolder")
            }
        val mediaContent = FileContent("application/octet-stream", localFile)
        driveService.files().create(fileMetadata, mediaContent).setFields("id").execute()
    }
}
