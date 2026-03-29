package com.entropyjournal.data.remote.googledrive

import android.accounts.Account
import android.content.Context
import android.content.SharedPreferences
import com.entropyjournal.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
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

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences
) {
    private suspend fun getDriveService(): Drive = withContext(Dispatchers.IO) {
        val accountEmail = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null)
            ?: throw IllegalStateException("Nicht angemeldet — bitte zuerst mit Google anmelden")

        if (accountEmail.isBlank()) {
            throw IllegalStateException("E-Mail-Adresse ist leer — bitte abmelden und neu anmelden")
        }

        val account = Account(accountEmail, "com.google")
        val scope = "oauth2:${DriveScopes.DRIVE_APPDATA}"
        val token = GoogleAuthUtil.getToken(context, account, scope)

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance()
        ) { request ->
            request.headers.authorization = "Bearer $token"
        }
            .setApplicationName("Journal")
            .build()
    }

    suspend fun backup(databaseFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService()

            val existingFiles = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '${Constants.DRIVE_BACKUP_FILENAME}'")
                .setFields("files(id)")
                .execute()

            existingFiles.files?.forEach { file ->
                driveService.files().delete(file.id).execute()
            }

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = Constants.DRIVE_BACKUP_FILENAME
                parents = listOf("appDataFolder")
            }

            val mediaContent = FileContent("application/octet-stream", databaseFile)
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            encryptedPrefs.edit()
                .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                .apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
