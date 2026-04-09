package com.bestjournal.app.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bestjournal.app.data.local.AppDatabase
import com.bestjournal.app.data.remote.googledrive.DriveBackupManager
import com.bestjournal.app.data.remote.googledrive.DriveRestoreManager
import com.bestjournal.app.util.Constants
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

        val dbResult = driveBackupManager.backup(dbFile)
        if (dbResult.isFailure) return dbResult

        val backupPhotos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_PHOTOS, false)
        val backupVideos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_VIDEOS, false)

        Log.d("SyncDebug", "Photo backup: photos=$backupPhotos, videos=$backupVideos")

        if (backupPhotos || backupVideos) {
            val photosDir = File(context.filesDir, "photos")
            val files = photosDir.listFiles() ?: emptyArray()
            Log.d("SyncDebug", "Photos dir: exists=${photosDir.exists()}, files=${files.size}")

            for (file in files) {
                val isVideo =
                    file.extension.lowercase() in listOf("mp4", "3gp", "mkv", "webm", "avi")
                val isPhoto =
                    file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                if ((isPhoto && backupPhotos) || (isVideo && backupVideos)) {
                    try {
                        driveBackupManager.backupFile(file, "photo_${file.name}")
                        Log.d("SyncDebug", "Uploaded: ${file.name} (${file.length()} bytes)")
                    } catch (e: Exception) {
                        Log.e("SyncDebug", "Upload failed: ${file.name}: ${e.message}")
                    }
                }
            }
        }

        return Result.success(Unit)
    }

    suspend fun restore(): Result<Unit> {
        val dbFile = context.getDatabasePath(dbName)

        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        dbFile.delete()

        val dbResult = driveRestoreManager.restore(dbFile)
        if (dbResult.isFailure) return dbResult

        try {
            val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
            val restoredCount = driveRestoreManager.restoreAllPhotos(photosDir)
            Log.d("SyncDebug", "Photos restored: $restoredCount files")
        } catch (e: Exception) {
            Log.e("SyncDebug", "Photos restore failed: ${e.message}", e)
        }

        return Result.success(Unit)
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
