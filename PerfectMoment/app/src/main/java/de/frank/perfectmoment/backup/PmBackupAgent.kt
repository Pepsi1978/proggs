package de.frank.perfectmoment.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import de.frank.perfectmoment.data.local.PerfectMomentDatabase

/**
 * Makes sure the database on disk is complete before Google copies it.
 *
 * Room writes through a write-ahead log, so recent sessions, hooks and skills can still sit in
 * `perfect_moment.db-wal` while the main file looks older than it is. Checkpointing folds the log
 * into the main file first, so the backup is consistent no matter when the system decides to run it.
 *
 * Only full backup is used ([android:fullBackupOnly]), so the key/value callbacks stay empty.
 */
class PmBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        runCatching {
            PerfectMomentDatabase.getInstance(applicationContext)
                .openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)")
                .close()
        }
        super.onFullBackup(data)
        // Only stamped once the copy actually went through, so a dead backup stays visible.
        BackupStatus.markBackedUp(applicationContext)
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit
}
