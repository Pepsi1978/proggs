package com.entropyjournal.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.data.local.AppDatabase
import com.entropyjournal.data.remote.googledrive.DriveBackupManager
import com.entropyjournal.data.remote.googledrive.DriveRestoreManager
import com.entropyjournal.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class SyncWithDriveUseCase
@Inject
constructor(
    private val driveBackupManager: DriveBackupManager,
    private val driveRestoreManager: DriveRestoreManager,
    private val database: AppDatabase,
    private val encryptedPrefs: SharedPreferences,
    @ApplicationContext private val context: Context,
) {
    private val dbName = "entropy_journal_db"

    suspend fun backup(): Result<Unit> {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return Result.failure(Exception("Datenbank nicht gefunden"))

        try {
            val roomDb = database.openHelper.writableDatabase
            val cursor = roomDb.query("PRAGMA wal_checkpoint(TRUNCATE)")
            if (cursor.moveToFirst()) {
                val busy = cursor.getInt(0)
                val logFrames = cursor.getInt(1)
                val checkpointed = cursor.getInt(2)
                Log.d("SyncDebug", "WAL checkpoint: busy=$busy, log=$logFrames, checkpointed=$checkpointed")
                if (busy != 0) {
                    cursor.close()
                    Log.e("SyncDebug", "WAL checkpoint busy — aborting backup to prevent data loss")
                    return Result.failure(Exception("WAL checkpoint busy"))
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("SyncDebug", "WAL checkpoint failed — aborting backup: ${e.message}")
            return Result.failure(Exception("WAL checkpoint failed: ${e.message}"))
        }

        val entryCount =
            try {
                val db =
                    android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbFile.path,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                    )
                val c = db.rawQuery("SELECT COUNT(*) FROM journal_entries", null)
                c.moveToFirst()
                val count = c.getInt(0)
                c.close()
                db.close()
                count
            } catch (_: Exception) {
                -1
            }

        Log.d("SyncDebug", "Backup: $entryCount Eintraege, Datei: ${dbFile.length()} Bytes")

        SyncProgressHolder.setUploading(0, 1)
        val dbResult = driveBackupManager.backup(dbFile)
        if (dbResult.isFailure) {
            SyncProgressHolder.setError()
            return dbResult
        }

        val backupPhotos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_PHOTOS, true)
        val backupVideos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_VIDEOS, true)

        Log.d("SyncDebug", "Photo backup: photos=$backupPhotos, videos=$backupVideos")

        if (backupPhotos || backupVideos) {
            val photosDir = File(context.filesDir, "photos")
            val allFiles = photosDir.listFiles() ?: emptyArray()
            val filesToUpload =
                allFiles
                    .filter { file ->
                        val isVideo =
                            file.extension.lowercase() in listOf("mp4", "3gp", "mkv", "webm", "avi")
                        val isPhoto =
                            file.extension.lowercase() in
                                listOf("jpg", "jpeg", "png", "webp", "gif")
                        (isPhoto && backupPhotos) || (isVideo && backupVideos)
                    }
                    .map { file -> file to "photo_${file.name}" }
            Log.d("SyncDebug", "Photos dir: ${filesToUpload.size} files to check")
            SyncProgressHolder.setUploading(0, filesToUpload.size)

            val batchResult =
                driveBackupManager.backupFiles(filesToUpload) { current, total ->
                    SyncProgressHolder.setUploading(current, total)
                }
            if (batchResult.isFailure) {
                Log.e(
                    "SyncDebug",
                    "Photo batch upload failed: ${batchResult.exceptionOrNull()?.message}",
                )
            } else {
                Log.d(
                    "SyncDebug",
                    "Photo backup done: ${batchResult.getOrDefault(0)} new files uploaded",
                )
            }

            // Cleanup: DATABASE is the single source of truth.
            // Only files referenced in entry_photos may exist on Drive.
            // Everything else is an orphan and gets deleted.
            try {
                val dbPaths = database.entryPhotoDao().getAllFilePaths()
                val referencedDriveNames =
                    dbPaths.map { path -> "photo_${File(path).name}" }.toSet()
                Log.d(
                    "SyncDebug",
                    "DB references ${referencedDriveNames.size} files — cleaning Drive orphans",
                )
                val deletedFromDrive =
                    driveBackupManager.cleanupOrphanedDriveFiles(referencedDriveNames)
                if (deletedFromDrive > 0) {
                    Log.d("SyncDebug", "Cleaned up $deletedFromDrive orphaned files from Drive")
                }

                // Also clean local orphans: files in photos/ not referenced in DB
                val referencedLocalNames = dbPaths.map { File(it).name }.toSet()
                var localOrphans = 0
                allFiles.forEach { file ->
                    if (file.name !in referencedLocalNames) {
                        file.delete()
                        localOrphans++
                        Log.d("SyncDebug", "Deleted local orphan: ${file.name}")
                    }
                }
                if (localOrphans > 0) {
                    Log.d("SyncDebug", "Cleaned up $localOrphans orphaned local files")
                }
            } catch (e: Exception) {
                Log.e("SyncDebug", "Cleanup failed: ${e.message}", e)
            }
        }

        driveBackupManager.endSession()
        SyncProgressHolder.setSynced()
        return Result.success(Unit)
    }

    /** Fast restore: DB only + path rewrite. App restarts immediately after this. */
    suspend fun restore(): Result<Unit> {
        val dbFile = context.getDatabasePath(dbName)

        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        dbFile.delete()

        val dbResult = driveRestoreManager.restore(dbFile)
        if (dbResult.isFailure) return dbResult

        // Clear old photos — they belong to the previous account/device
        val photosDir = File(context.filesDir, "photos")
        if (photosDir.exists()) {
            val deleted = photosDir.listFiles()?.size ?: 0
            photosDir.deleteRecursively()
            Log.d("SyncDebug", "Cleared $deleted old photos from local dir")
        }
        photosDir.mkdirs()

        // Rewrite photo paths in the restored DB to match this device's filesDir
        try {
            val localPhotosPath = photosDir.absolutePath
            val db =
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
                )
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM entry_photos", null)
            val photoCount = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
            countCursor.close()
            Log.d(
                "SyncDebug",
                "DB references $photoCount photos/videos — downloading after restart",
            )

            if (photoCount > 0) {
                db.execSQL(
                    """
                    UPDATE entry_photos
                    SET filePath = ? || '/' ||
                        substr(filePath, instr(filePath, '/photos/') + 8)
                    WHERE filePath LIKE '%/photos/%'
                    """
                        .trimIndent(),
                    arrayOf(localPhotosPath),
                )
                Log.d("SyncDebug", "Rewrote photo paths to: $localPhotosPath/")
            }
            db.close()
        } catch (e: Exception) {
            Log.e("SyncDebug", "Path rewrite failed: ${e.message}", e)
        }

        // Clear retrospective summaries — they belong to the old data and will be regenerated
        context.deleteDatabase("retrospective_db")
        Log.d("SyncDebug", "Cleared retrospective_db for fresh generation after restore")

        // Clear dashboard blocks — they belong to the old data and will be regenerated
        // from journal entries using the current profile prompt
        context.deleteDatabase("dashboard_db")
        Log.d("SyncDebug", "Cleared dashboard_db for fresh generation after restore")

        // Restore TTS favorites from Drive if available
        try {
            val favoritesFile = File(context.cacheDir, "tts_favorites_restore.tmp")
            val restoreResult = driveRestoreManager.restoreFile("tts_favorites.txt", favoritesFile)
            if (restoreResult.isSuccess && favoritesFile.exists()) {
                val favoritesContent = favoritesFile.readText().trim()
                if (favoritesContent.isNotEmpty()) {
                    encryptedPrefs.edit()
                        .putString(Constants.PREF_TTS_FAVORITES, favoritesContent)
                        .commit()
                    Log.d("SyncDebug", "Restored TTS favorites from Drive: $favoritesContent")
                }
                favoritesFile.delete()
            } else {
                Log.d("SyncDebug", "No TTS favorites backup found on Drive — skipping restore")
            }
        } catch (e: Exception) {
            Log.e("SyncDebug", "TTS favorites restore failed (non-critical): ${e.message}")
        }

        // Photos are downloaded AFTER app restart via downloadMissingPhotos()
        return Result.success(Unit)
    }

    /** Immediately backs up TTS voice favorites to Drive as a plain text file. */
    suspend fun backupFavorites(favoritesString: String): Result<Unit> {
        return try {
            val tmpFile = File(context.cacheDir, "tts_favorites_backup.tmp")
            tmpFile.writeText(favoritesString)
            val result = driveBackupManager.backupFile(tmpFile, "tts_favorites.txt")
            tmpFile.delete()
            if (result.isSuccess) {
                Log.d("SyncDebug", "TTS favorites backed up to Drive: $favoritesString")
            } else {
                Log.e("SyncDebug", "TTS favorites backup failed: ${result.exceptionOrNull()?.message}")
            }
            result
        } catch (e: Exception) {
            Log.e("SyncDebug", "TTS favorites backup exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Downloads photos/videos from Drive that are referenced in the DB but missing on disk. Called
     * in the background after app restart, so the user sees entries immediately.
     */
    suspend fun downloadMissingPhotos(): Int {
        val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
        val dbFile = context.getDatabasePath(dbName)

        // Check which files are missing
        val missingFiles = mutableListOf<String>()
        try {
            val db =
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                )
            val cursor = db.rawQuery("SELECT filePath FROM entry_photos", null)
            while (cursor.moveToNext()) {
                val path = cursor.getString(0)
                if (!File(path).exists()) {
                    missingFiles.add(path)
                }
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            Log.e("SyncDebug", "Missing file check failed: ${e.message}")
            return 0
        }

        if (missingFiles.isEmpty()) {
            Log.d("SyncDebug", "All photos already on disk — nothing to download")
            return 0
        }

        Log.d("SyncDebug", "Downloading ${missingFiles.size} missing photos from Drive...")
        SyncProgressHolder.setDownloading(0, missingFiles.size)

        // Download all photos from Drive
        val restoredCount =
            try {
                driveRestoreManager.restoreAllPhotos(photosDir) { current, total ->
                    SyncProgressHolder.setDownloading(current, total)
                }
            } catch (e: Exception) {
                Log.e("SyncDebug", "Photo download failed: ${e.message}", e)
                SyncProgressHolder.setError()
                0
            }

        Log.d("SyncDebug", "Downloaded $restoredCount photos from Drive")

        // Clean up orphaned entries (files still missing after download = not on Drive)
        try {
            val db =
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
                )
            val cursor = db.rawQuery("SELECT id, filePath FROM entry_photos", null)
            val orphanIds = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val path = cursor.getString(1)
                if (!File(path).exists()) {
                    orphanIds.add(id)
                    Log.w("SyncDebug", "Orphaned photo (not on Drive): $path")
                }
            }
            cursor.close()
            if (orphanIds.isNotEmpty()) {
                val idList = orphanIds.joinToString(",")
                db.execSQL("DELETE FROM entry_photos WHERE id IN ($idList)")
                Log.d("SyncDebug", "Removed ${orphanIds.size} orphaned photo entries from DB")
            }
            db.close()
        } catch (e: Exception) {
            Log.e("SyncDebug", "Orphan cleanup failed: ${e.message}", e)
        }

        // Trigger Room's invalidation tracker so the UI refreshes with the new photos
        if (restoredCount > 0) {
            try {
                database.openHelper.writableDatabase.execSQL(
                    "UPDATE entry_photos SET filePath = filePath WHERE 1=1"
                )
                Log.d("SyncDebug", "Triggered UI refresh for downloaded photos")
            } catch (e: Exception) {
                Log.e("SyncDebug", "UI refresh trigger failed: ${e.message}")
            }
        }

        // Clear restore-pending flag — retrospectives may now generate
        encryptedPrefs.edit().putBoolean(Constants.PREF_RESTORE_PENDING, false).apply()
        Log.d("SyncDebug", "Restore complete — cleared PREF_RESTORE_PENDING")

        SyncProgressHolder.setSynced()
        return restoredCount
    }

    suspend fun hasBackup(): Boolean {
        return driveRestoreManager.hasBackup()
    }

    suspend fun mergeFromDrive(): Int {
        val lastLocalSync = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L)
        val driveModifiedTime = driveRestoreManager.getBackupModifiedTime()
        if (driveModifiedTime == null || driveModifiedTime <= lastLocalSync + 60_000) {
            Log.d("SyncDebug", "Auto-merge: Drive not newer or unavailable")
            return 0
        }
        Log.d("SyncDebug", "Auto-merge: Drive backup is newer — checking for new entries")
        val tempFile = File(context.cacheDir, "drive_merge_temp.db")
        try {
            val result = driveRestoreManager.restore(tempFile)
            if (result.isFailure) { tempFile.delete(); return 0 }
            val remoteDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                tempFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            val remoteHasFollowUpText = remoteDb.hasColumn("journal_entries", "followUpText")
            val localTimestamps = mutableSetOf<Long>()
            val localDb = database.openHelper.readableDatabase
            val tsCursor = localDb.query("SELECT timestamp FROM journal_entries")
            while (tsCursor.moveToNext()) { localTimestamps.add(tsCursor.getLong(0)) }
            tsCursor.close()
            val remoteCursor = remoteDb.rawQuery(
                "SELECT timestamp, rawText, improvedText, isImproved, displayText, " +
                    "audioDurationSeconds, moodTag, entropyScore, adviceCategoryTags, " +
                    "summary, title" +
                    if (remoteHasFollowUpText) ", followUpText, isSynced " else ", isSynced " +
                    "FROM journal_entries",
                null,
            )
            val writableDb = database.openHelper.writableDatabase
            var imported = 0
            while (remoteCursor.moveToNext()) {
                val ts = remoteCursor.getLong(0)
                if (ts !in localTimestamps) {
                    val values = android.content.ContentValues().apply {
                        put("timestamp", ts)
                        put("rawText", remoteCursor.getString(1))
                        if (!remoteCursor.isNull(2)) put("improvedText", remoteCursor.getString(2))
                        put("isImproved", remoteCursor.getInt(3))
                        put("displayText", remoteCursor.getString(4))
                        put("audioDurationSeconds", remoteCursor.getInt(5))
                        if (!remoteCursor.isNull(6)) put("moodTag", remoteCursor.getString(6))
                        if (!remoteCursor.isNull(7)) put("entropyScore", remoteCursor.getFloat(7))
                        if (!remoteCursor.isNull(8)) put("adviceCategoryTags", remoteCursor.getString(8))
                        if (!remoteCursor.isNull(9)) put("summary", remoteCursor.getString(9))
                        if (!remoteCursor.isNull(10)) put("title", remoteCursor.getString(10))
                        if (remoteHasFollowUpText && !remoteCursor.isNull(11)) {
                            put("followUpText", remoteCursor.getString(11))
                        }
                        put("isSynced", remoteCursor.getInt(if (remoteHasFollowUpText) 12 else 11))
                    }
                    writableDb.insert("journal_entries", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, values)
                    imported++
                }
            }
            remoteCursor.close(); remoteDb.close(); tempFile.delete()
            if (imported > 0) {
                writableDb.execSQL("UPDATE journal_entries SET isSynced = isSynced WHERE 1=0")
                Log.d("SyncDebug", "Auto-merged $imported entries from Drive")
            }
            encryptedPrefs.edit().putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis()).apply()
            return imported
        } catch (e: Exception) {
            Log.e("SyncDebug", "Auto-merge failed: ${e.message}", e)
            tempFile.delete(); return 0
        }
    }

    private fun android.database.sqlite.SQLiteDatabase.hasColumn(
        tableName: String,
        columnName: String,
    ): Boolean =
        rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) {
                    return@use true
                }
            }
            false
        }

    private fun zipPhotos(
        photosDir: File,
        zipFile: File,
        includePhotos: Boolean,
        includeVideos: Boolean,
    ) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            photosDir.listFiles()?.forEach { file ->
                val isVideo =
                    file.extension.lowercase() in listOf("mp4", "3gp", "mkv", "webm", "avi")
                val isPhoto =
                    file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                if ((isPhoto && includePhotos) || (isVideo && includeVideos)) {
                    zos.putNextEntry(ZipEntry(file.name))
                    FileInputStream(file).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun unzipPhotos(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(targetDir, entry.name)
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
