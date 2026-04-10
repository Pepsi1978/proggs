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
                Log.d(
                    "SyncDebug",
                    "WAL checkpoint: busy=${cursor.getInt(0)}, log=${cursor.getInt(1)}, checkpointed=${cursor.getInt(2)}",
                )
            }
            cursor.close()
        } catch (e: Exception) {
            Log.w("SyncDebug", "WAL checkpoint failed: ${e.message}")
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

        // Create a clean copy without retrospective summaries — they are AI-generated
        // and will be regenerated from real entries after restore
        val cleanDbFile = File(context.cacheDir, "backup_clean.db")
        try {
            dbFile.copyTo(cleanDbFile, overwrite = true)
            // Also copy WAL/SHM if they still exist after checkpoint
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.copyTo(File(cleanDbFile.path + "-wal"), overwrite = true)
            if (shmFile.exists()) shmFile.copyTo(File(cleanDbFile.path + "-shm"), overwrite = true)

            val cleanDb =
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    cleanDbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
                )
            cleanDb.execSQL("DELETE FROM retrospective_summaries")
            cleanDb.execSQL("VACUUM")
            cleanDb.close()
            // Clean up copied WAL/SHM after VACUUM
            File(cleanDbFile.path + "-wal").delete()
            File(cleanDbFile.path + "-shm").delete()
            Log.d("SyncDebug", "Created clean backup without retrospective summaries")
        } catch (e: Exception) {
            Log.w("SyncDebug", "Clean copy failed, using original: ${e.message}")
            dbFile.copyTo(cleanDbFile, overwrite = true)
        }

        val dbResult = driveBackupManager.backup(cleanDbFile)
        cleanDbFile.delete()
        if (dbResult.isFailure) return dbResult

        val backupPhotos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_PHOTOS, false)
        val backupVideos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_VIDEOS, false)

        Log.d("SyncDebug", "Photo backup: photos=$backupPhotos, videos=$backupVideos")

        if (backupPhotos || backupVideos) {
            val photosDir = File(context.filesDir, "photos")
            val allFiles = photosDir.listFiles() ?: emptyArray()
            val filesToUpload = allFiles.filter { file ->
                val isVideo =
                    file.extension.lowercase() in listOf("mp4", "3gp", "mkv", "webm", "avi")
                val isPhoto =
                    file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                (isPhoto && backupPhotos) || (isVideo && backupVideos)
            }
            Log.d("SyncDebug", "Photos dir: ${filesToUpload.size} files to upload")
            SyncProgressHolder.setUploading(0, filesToUpload.size)

            var uploaded = 0
            for (file in filesToUpload) {
                try {
                    driveBackupManager.backupFile(file, "photo_${file.name}")
                    uploaded++
                    SyncProgressHolder.setUploading(uploaded, filesToUpload.size)
                    Log.d(
                        "SyncDebug",
                        "Uploaded ($uploaded/${filesToUpload.size}): ${file.name} (${file.length()} bytes)",
                    )
                } catch (e: Exception) {
                    Log.e("SyncDebug", "Upload failed: ${file.name}: ${e.message}")
                }
            }
        }

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
            // Clear retrospective summaries — will be regenerated fresh from real entries
            db.execSQL("DELETE FROM retrospective_summaries")
            Log.d("SyncDebug", "Cleared retrospective summaries for fresh generation")

            db.close()
        } catch (e: Exception) {
            Log.e("SyncDebug", "Path rewrite failed: ${e.message}", e)
        }

        // Photos are downloaded AFTER app restart via downloadMissingPhotos()
        return Result.success(Unit)
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

        SyncProgressHolder.setSynced()
        return restoredCount
    }

    suspend fun hasBackup(): Boolean {
        return driveRestoreManager.hasBackup()
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
