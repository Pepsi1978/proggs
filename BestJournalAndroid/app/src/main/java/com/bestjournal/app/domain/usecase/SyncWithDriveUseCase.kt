package com.bestjournal.app.domain.usecase

import android.content.Context
import android.util.Log
import com.bestjournal.app.data.local.AppDatabase
import com.bestjournal.app.data.remote.googledrive.DriveBackupManager
import com.bestjournal.app.data.remote.googledrive.DriveRestoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SyncWithDriveUseCase
@Inject
constructor(
    private val driveBackupManager: DriveBackupManager,
    private val driveRestoreManager: DriveRestoreManager,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context,
) {
    private val dbName = "entropy_journal_db"

    suspend fun backup(): Result<Unit> {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return Result.failure(Exception("Datenbank nicht gefunden"))

        // Force WAL checkpoint via Room's own connection — do NOT close Room (breaks saves!)
        try {
            val roomDb = database.openHelper.writableDatabase
            val cursor = roomDb.query("PRAGMA wal_checkpoint(TRUNCATE)")
            if (cursor.moveToFirst()) {
                Log.d("SyncDebug", "WAL checkpoint: busy=${cursor.getInt(0)}, log=${cursor.getInt(1)}, checkpointed=${cursor.getInt(2)}")
            }
            cursor.close()
        } catch (e: Exception) {
            Log.w("SyncDebug", "WAL checkpoint failed: ${e.message}")
        }

        // Count entries for logging
        val entryCount = try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            val c = db.rawQuery("SELECT COUNT(*) FROM journal_entries", null)
            c.moveToFirst()
            val count = c.getInt(0)
            c.close()
            db.close()
            count
        } catch (_: Exception) { -1 }

        Log.d("SyncDebug", "Backup: $entryCount Eintraege, Datei: ${dbFile.length()} Bytes")

        return driveBackupManager.backup(dbFile)
    }

    suspend fun restore(): Result<Unit> {
        val dbFile = context.getDatabasePath(dbName)

        // Delete WAL and SHM files that would conflict with the restored database
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        dbFile.delete()

        // Download the backup from Google Drive
        return driveRestoreManager.restore(dbFile)
    }

    suspend fun hasBackup(): Boolean {
        return driveRestoreManager.hasBackup()
    }
}
