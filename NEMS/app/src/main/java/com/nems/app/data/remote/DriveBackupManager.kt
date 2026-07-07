package com.nems.app.data.remote

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.nems.app.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class NeedConsentException(val consentIntent: Intent) :
    Exception("Drive-Zugriff muss erlaubt werden")

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
) {
    private suspend fun getDriveService(): Drive = withContext(Dispatchers.IO) {
        val accountEmail = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null)
            ?: throw IllegalStateException("Nicht angemeldet")

        val account = Account(accountEmail, "com.google")
        val scope = "oauth2:${DriveScopes.DRIVE_APPDATA}"
        val token = GoogleAuthUtil.getToken(context, account, scope)

        Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance()) { request ->
            request.headers.authorization = "Bearer $token"
        }.setApplicationName("NEMS").build()
    }

    suspend fun backup(databaseFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()

            val existing = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '${Constants.DRIVE_BACKUP_FILENAME}'")
                .setFields("files(id)")
                .execute()

            val mediaContent = FileContent("application/octet-stream", databaseFile)
            val existingId = existing.files?.firstOrNull()?.id

            if (existingId != null) {
                driveService.files().update(existingId, null, mediaContent).execute()
                existing.files?.drop(1)?.forEach {
                    driveService.files().delete(it.id).execute()
                }
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = Constants.DRIVE_BACKUP_FILENAME
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            }

            encryptedPrefs.edit()
                .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                .apply()

            Result.success(Unit)
        } catch (e: UserRecoverableAuthException) {
            Result.failure(NeedConsentException(e.intent ?: Intent()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
