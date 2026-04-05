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

        // Close Room's connection so WAL checkpoint can fully flush all data
        database.close()

        // Force WAL checkpoint — moves ALL pending writes from WAL into the main .db file
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )
        val checkpointCursor = db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)
        if (checkpointCursor.moveToFirst()) {
            val busy = checkpointCursor.getInt(0)
            val log = checkpointCursor.getInt(1)
            val checkpointed = checkpointCursor.getInt(2)
            Log.d("SyncDebug", "WAL checkpoint: busy=$busy, log=$log, checkpointed=$checkpointed")
        }
        checkpointCursor.close()

        // Count entries to verify backup completeness
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM journal_entries", null)
        countCursor.moveToFirst()
        val entryCount = countCursor.getInt(0)
        countCursor.close()
        db.close()

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
