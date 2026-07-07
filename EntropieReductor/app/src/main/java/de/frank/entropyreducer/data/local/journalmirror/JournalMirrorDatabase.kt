package de.frank.entropyreducer.data.local.journalmirror

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Eigenstaendige, NICHT ins Drive-Backup aufgenommene Spiegel-DB der
 * BestJournal-Frank-Tagebucheintraege (Frank-Wunsch 2026-05-24). Reine Kopie —
 * destructiveFallback ist unkritisch, da bei jedem App-Start neu synchronisiert wird.
 */
@Database(
    entities = [JournalMirrorEntryEntity::class, JournalMirrorFollowupEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class JournalMirrorDatabase : RoomDatabase() {
    abstract fun journalMirrorDao(): JournalMirrorDao

    companion object {
        const val DB_NAME = "journal_mirror_db"
    }
}
