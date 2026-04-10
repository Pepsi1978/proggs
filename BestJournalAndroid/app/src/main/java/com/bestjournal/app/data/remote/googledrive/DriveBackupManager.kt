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

private const val RESUMABLE_THRESHOLD = 5L * 1024 * 1024 // 5 MB

class NeedConsentException(val consentIntent: Intent) :
    Exception("Drive-Zugriff muss erlaubt werden")

@Singleton
class DriveBackupManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
) {
    @Volatile private var sessionService: Drive? = null

    private suspend fun getSessionDriveService(): Drive {
        sessionService?.let {
            return it
        }
        return getDriveService().also { sessionService = it }
    }

    fun endSession() {
        sessionService = null
    }

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
                val driveService = getSessionDriveService()

                val existing =
                    driveService
                        .files()
                        .list()
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
                    val fileMetadata =
                        com.google.api.services.drive.model.File().apply {
                            name = Constants.DRIVE_BACKUP_FILENAME
                            parents = listOf("appDataFolder")
                        }
                    driveService
                        .files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute()
                }

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
     * - Lists existing files once — skips already-uploaded (name + size match)
     * - Largest files first so big uploads start early and run in parallel
     * - Resumable upload for files > 5 MB (survives connection drops)
     * - Uses update() for changed files (1 API call instead of delete+create = 3)
     * - 5 parallel uploads via Semaphore
     */
    suspend fun backupFiles(
        files: List<Pair<File, String>>,
        onProgress: (uploaded: Int, total: Int) -> Unit,
    ): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getSessionDriveService()

                // List all existing photo files on Drive in one call (name + size + id)
                data class DriveFileInfo(val id: String, val size: Long)
                val existingFiles = mutableMapOf<String, DriveFileInfo>() // name -> info
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
                    if (pageToken != null) request.pageToken = pageToken
                    val result = request.execute()
                    result.files?.forEach { f ->
                        existingFiles[f.name] = DriveFileInfo(f.id, f.getSize() ?: 0L)
                    }
                    pageToken = result.nextPageToken
                } while (pageToken != null)

                // Delta sync: skip files where name AND size match
                val newFiles = files.filter { (localFile, remoteName) ->
                    val info = existingFiles[remoteName]
                    info == null || info.size != localFile.length()
                }
                val alreadyUploaded = files.size - newFiles.size
                android.util.Log.d(
                    "DriveBackup",
                    "Delta sync: ${files.size} total, $alreadyUploaded unchanged, ${newFiles.size} to upload",
                )

                if (newFiles.isEmpty()) {
                    onProgress(files.size, files.size)
                    return@withContext Result.success(0)
                }

                // Sort: largest files first so big uploads start early
                val sorted = newFiles.sortedByDescending { (file, _) -> file.length() }

                onProgress(alreadyUploaded, files.size)

                // Parallel upload with Semaphore(5)
                val semaphore = Semaphore(5)
                val uploaded = AtomicInteger(alreadyUploaded)
                val errors = AtomicInteger(0)

                coroutineScope {
                    sorted
                        .map { (localFile, remoteName) ->
                            launch {
                                semaphore.withPermit {
                                    var success = false
                                    for (attempt in 0..1) { // 1 retry
                                        try {
                                            val mediaContent =
                                                FileContent("application/octet-stream", localFile)
                                            val existingInfo = existingFiles[remoteName]

                                            if (existingInfo != null) {
                                                // Update existing file — 1 API call instead of
                                                // delete+create (3 calls)
                                                val updateRequest =
                                                    driveService
                                                        .files()
                                                        .update(existingInfo.id, null, mediaContent)
                                                if (localFile.length() > RESUMABLE_THRESHOLD) {
                                                    updateRequest.mediaHttpUploader?.apply {
                                                        isDirectUploadEnabled = false
                                                        chunkSize = 5 * 1024 * 1024 // 5 MB chunks
                                                    }
                                                }
                                                updateRequest.execute()
                                            } else {
                                                // Create new file
                                                val fileMetadata =
                                                    com.google.api.services.drive.model
                                                        .File()
                                                        .apply {
                                                            name = remoteName
                                                            parents = listOf("appDataFolder")
                                                        }
                                                val createRequest =
                                                    driveService
                                                        .files()
                                                        .create(fileMetadata, mediaContent)
                                                        .setFields("id")
                                                if (localFile.length() > RESUMABLE_THRESHOLD) {
                                                    createRequest.mediaHttpUploader?.apply {
                                                        isDirectUploadEnabled = false
                                                        chunkSize = 5 * 1024 * 1024 // 5 MB chunks
                                                    }
                                                }
                                                createRequest.execute()
                                            }

                                            val current = uploaded.incrementAndGet()
                                            onProgress(current, files.size)
                                            android.util.Log.d(
                                                "DriveBackup",
                                                "Uploaded ($current/${files.size}): $remoteName (${localFile.length()} bytes)",
                                            )
                                            success = true
                                            break
                                        } catch (e: Exception) {
                                            if (attempt == 0) {
                                                android.util.Log.w(
                                                    "DriveBackup",
                                                    "Upload retry ($remoteName): ${e.message}",
                                                )
                                                kotlinx.coroutines.delay(1000)
                                            } else {
                                                android.util.Log.e(
                                                    "DriveBackup",
                                                    "Upload failed ($remoteName): ${e.message}",
                                                )
                                            }
                                        }
                                    }
                                    if (!success) errors.incrementAndGet()
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

    /**
     * Delete orphaned files from Drive that no longer exist locally. Call after upload to keep
     * Drive in sync with the local photos directory.
     */
    suspend fun cleanupOrphanedDriveFiles(localFileNames: Set<String>): Int =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getSessionDriveService()

                // List all photo/video files on Drive
                val driveFiles = mutableListOf<com.google.api.services.drive.model.File>()
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
                    if (pageToken != null) request.pageToken = pageToken
                    val result = request.execute()
                    result.files?.let { driveFiles.addAll(it) }
                    pageToken = result.nextPageToken
                } while (pageToken != null)

                // Find orphans: files on Drive whose local counterpart no longer exists
                val orphans = driveFiles.filter { it.name !in localFileNames }

                if (orphans.isEmpty()) {
                    android.util.Log.d(
                        "DriveBackup",
                        "Cleanup: no orphaned files on Drive (${driveFiles.size} files OK)",
                    )
                    return@withContext 0
                }

                val totalBytes = orphans.sumOf { it.getSize() ?: 0L }
                android.util.Log.d(
                    "DriveBackup",
                    "Cleanup: deleting ${orphans.size} orphaned files from Drive (${totalBytes / 1024 / 1024} MB)",
                )

                var deleted = 0
                for (orphan in orphans) {
                    try {
                        driveService.files().delete(orphan.id).execute()
                        deleted++
                        android.util.Log.d(
                            "DriveBackup",
                            "Deleted orphan: ${orphan.name} (${(orphan.getSize() ?: 0L) / 1024} KB)",
                        )
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "DriveBackup",
                            "Failed to delete orphan ${orphan.name}: ${e.message}",
                        )
                    }
                }
                android.util.Log.d("DriveBackup", "Cleanup complete: $deleted orphans removed")
                deleted
            } catch (e: Exception) {
                android.util.Log.e("DriveBackup", "Cleanup FAILED: ${e.message}", e)
                0
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
