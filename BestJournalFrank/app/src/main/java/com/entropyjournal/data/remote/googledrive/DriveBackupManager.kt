package com.entropyjournal.data.remote.googledrive

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.entropyjournal.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
                Result.failure(NeedConsentException(e.intent ?: Intent()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun backupFile(localFile: File, remoteName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()
                uploadFile(driveService, localFile, remoteName)
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("DriveBackup", "File backup FAILED ($remoteName): ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Batch upload with delta sync + parallel uploads.
     * - Single OAuth token / Drive service for all files
     * - Lists existing files once to skip already-uploaded
     * - 3 parallel uploads via Semaphore
     */
    suspend fun backupFiles(
        files: List<Pair<File, String>>,
        onProgress: (uploaded: Int, total: Int) -> Unit,
    ): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService()

                // List all existing photo files on Drive in one call
                val existingNames = mutableSetOf<String>()
                var pageToken: String? = null
                do {
                    val request =
                        driveService
                            .files()
                            .list()
                            .setSpaces("appDataFolder")
                            .setQ("name contains 'photo_'")
                            .setFields("nextPageToken, files(name)")
                            .setPageSize(1000)
                    if (pageToken != null) request.pageToken = pageToken
                    val result = request.execute()
                    result.files?.forEach { existingNames.add(it.name) }
                    pageToken = result.nextPageToken
                } while (pageToken != null)

                // Filter to only new files
                val newFiles = files.filter { (_, remoteName) -> remoteName !in existingNames }
                val alreadyUploaded = files.size - newFiles.size
                android.util.Log.d(
                    "DriveBackup",
                    "Delta sync: ${files.size} total, $alreadyUploaded already on Drive, ${newFiles.size} to upload",
                )

                if (newFiles.isEmpty()) {
                    onProgress(files.size, files.size)
                    return@withContext Result.success(0)
                }

                onProgress(alreadyUploaded, files.size)

                // Parallel upload with Semaphore(3)
                val semaphore = Semaphore(3)
                val uploaded = AtomicInteger(alreadyUploaded)
                val errors = AtomicInteger(0)

                coroutineScope {
                    newFiles
                        .map { (localFile, remoteName) ->
                            launch {
                                semaphore.withPermit {
                                    try {
                                        val fileMetadata =
                                            com.google.api.services.drive.model.File().apply {
                                                name = remoteName
                                                parents = listOf("appDataFolder")
                                            }
                                        val mediaContent =
                                            FileContent("application/octet-stream", localFile)
                                        driveService
                                            .files()
                                            .create(fileMetadata, mediaContent)
                                            .setFields("id")
                                            .execute()
                                        val current = uploaded.incrementAndGet()
                                        onProgress(current, files.size)
                                        android.util.Log.d(
                                            "DriveBackup",
                                            "Uploaded ($current/${files.size}): $remoteName (${localFile.length()} bytes)",
                                        )
                                    } catch (e: Exception) {
                                        errors.incrementAndGet()
                                        android.util.Log.e(
                                            "DriveBackup",
                                            "Upload failed ($remoteName): ${e.message}",
                                        )
                                    }
                                }
                            }
                        }
                        .joinAll()
                }

                val errorCount = errors.get()
                if (errorCount > 0) {
                    android.util.Log.w(
                        "DriveBackup",
                        "$errorCount of ${newFiles.size} uploads failed",
                    )
                }
                Result.success(newFiles.size - errorCount)
            } catch (e: UserRecoverableAuthException) {
                Result.failure(NeedConsentException(e.intent ?: Intent()))
            } catch (e: Exception) {
                android.util.Log.e("DriveBackup", "Batch upload FAILED: ${e.message}", e)
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
