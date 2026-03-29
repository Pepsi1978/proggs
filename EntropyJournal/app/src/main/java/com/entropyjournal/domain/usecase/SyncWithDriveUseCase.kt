package com.entropyjournal.domain.usecase

import android.content.Context
import com.entropyjournal.data.local.AppDatabase
import com.entropyjournal.data.remote.googledrive.DriveBackupManager
import com.entropyjournal.data.remote.googledrive.DriveRestoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SyncWithDriveUseCase @Inject constructor(
    private val driveBackupManager: DriveBackupManager,
    private val driveRestoreManager: DriveRestoreManager,
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context
) {
    suspend fun backup(): Result<Unit> {
        // Checkpoint WAL to ensure all data is in the main db file
        appDatabase.query("PRAGMA wal_checkpoint(FULL)", null)

        val dbFile = context.getDatabasePath("entropy_journal_db")
        if (!dbFile.exists()) return Result.failure(Exception("Datenbank nicht gefunden"))
        return driveBackupManager.backup(dbFile)
    }

    suspend fun restore(): Result<Unit> {
        val dbFile = context.getDatabasePath("entropy_journal_db")

        // Close the database so we can replace the file
        appDatabase.close()

        // Delete WAL and SHM files that would conflict with the restored database
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()

        // Download the backup from Google Drive
        val result = driveRestoreManager.restore(dbFile)

        // Database will be reopened automatically by Room on next access
        return result
    }

    suspend fun hasBackup(): Boolean {
        return driveRestoreManager.hasBackup()
    }
}
