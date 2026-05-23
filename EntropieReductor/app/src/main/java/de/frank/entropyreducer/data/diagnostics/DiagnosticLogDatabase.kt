package de.frank.entropyreducer.data.diagnostics

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Eigenstaendige Datenbank NUR fuer das interne Diagnose-Logging (Frank-Wunsch 2026-05-23).
 *
 * Bewusst getrennt von AppDatabase und ScientistDatabase:
 *  - Keine Migration der Haupt-DB noetig — kein Risiko fuer echte Aufgaben-/Biomarker-Daten.
 *  - Logs sind geraetelokale Diagnose und gehoeren NICHT ins Drive-Backup (BackupPayload).
 *  - destructiveFallback ist hier voellig unkritisch: Logs sind Wegwerf-Daten. Bei einem
 *    Schema-Bump duerfen die alten Eintraege verschwinden.
 */
@Database(
    entities = [DiagnosticLogEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class DiagnosticLogDatabase : RoomDatabase() {
    abstract fun diagnosticLogDao(): DiagnosticLogDao

    companion object {
        const val DB_NAME = "entropy_reducer_diagnostics.db"
    }
}
