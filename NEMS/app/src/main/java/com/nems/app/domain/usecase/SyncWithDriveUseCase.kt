package com.nems.app.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nems.app.data.local.NmsDatabase
import com.nems.app.data.remote.DriveBackupManager
import com.nems.app.data.remote.DriveRestoreManager
import com.nems.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SyncWithDriveUseCase @Inject constructor(
    private val driveBackupManager: DriveBackupManager,
    private val driveRestoreManager: DriveRestoreManager,
    private val database: NmsDatabase,
    private val encryptedPrefs: SharedPreferences,
    @ApplicationContext private val context: Context,
) {
    suspend fun backup(): Result<Unit> {
        val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
        if (!dbFile.exists()) return Result.failure(Exception("Datenbank nicht gefunden"))

        // WAL checkpoint: flush write-ahead log into main DB file
        try {
            val roomDb = database.openHelper.writableDatabase
            val cursor = roomDb.query("PRAGMA wal_checkpoint(TRUNCATE)")
            if (cursor.moveToFirst()) {
                Log.d("NmsSync", "WAL checkpoint: busy=${cursor.getInt(0)}, log=${cursor.getInt(1)}, checkpointed=${cursor.getInt(2)}")
            }
            cursor.close()
        } catch (e: Exception) {
            Log.w("NmsSync", "WAL checkpoint failed: ${e.message}")
        }

        Log.d("NmsSync", "Backing up database: ${dbFile.length()} bytes")
        return driveBackupManager.backup(dbFile)
    }

    suspend fun restore(): Result<Unit> {
        val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)

        // Delete WAL and SHM files
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        dbFile.delete()

        val result = driveRestoreManager.restore(dbFile)
        if (result.isFailure) return result

        Log.d("NmsSync", "Restore successful. Restarting app...")

        // Restart app to reload database
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)

        return Result.success(Unit) // unreachable but needed for compiler
    }

    suspend fun hasBackup(): Boolean = driveRestoreManager.hasBackup()
}
