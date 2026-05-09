package de.frank.entropyreducer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.GenieCodexDao
import de.frank.entropyreducer.data.local.dao.KiTriggerDao
import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.dao.WhoopWorkoutDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity

/**
 * Haupt-Datenbank — enthaelt alle nicht-Forscher-Daten. Frank-Wunsch 2026-05-09:
 * Die rein-Forscher-spezifischen Tabellen (scientist_sessions, scientist_messages,
 * hypotheses, hypothesis_messages) sind in eine separate ScientistDatabase ausgelagert,
 * damit sie ueber Schema-Aenderungen dieser Haupt-DB hinweg persistent bleiben.
 * Siehe ScientistDatabase.kt fuer Details.
 *
 * Insights und Memory bleiben hier — sie werden cross-cutting genutzt (Briefing,
 * Repertoire, Aufgaben-Antworten) und sind beim destruktiven Reset weniger schmerzhaft
 * weil Gemini sie aus den verbleibenden Daten wieder ableiten kann.
 *
 * Version 7: scientist_sessions/scientist_messages/hypotheses/hypothesis_messages
 * sind aus dieser DB entfernt — sie leben jetzt in der Forscher-DB.
 */
@Database(
    entities = [
        EntropyEntryEntity::class,
        SavedPromptEntity::class,
        BiomarkerSnapshotEntity::class,
        SupplementLogEntity::class,
        CalendarDayEntity::class,
        CalendarEventEntity::class,
        KiTriggerEntity::class,
        GenieCodexVersionEntity::class,
        WhoopWorkoutEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
// Version 10 (2026-05-09 Abend): InsightEntity und MemoryEntryEntity sind aus
// dieser DB ENTFERNT und in die ScientistDatabase verschoben. Frank-Wunsch:
// schema-stabile Persistenz fuer Insights und Memories plus Drive-Backup.
// Vor diesem Schritt laeuft InitialDataMigrator in EntropyReducerApp.onCreate
// und kopiert vorhandene Daten aus der alten v9-Datei in die ScientistDatabase
// BEVOR Room hier den destructive fallback ausfuehrt.
//
// Version 9 (2026-05-09): InsightEntity erweitert um additionalCategories
// und manualSource (siehe ScientistEntities.kt — Definition jetzt dort).
@TypeConverters(EntropyTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entropyEntryDao(): EntropyEntryDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun biomarkerSnapshotDao(): BiomarkerSnapshotDao
    abstract fun supplementLogDao(): SupplementLogDao
    abstract fun calendarDayDao(): CalendarDayDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun kiTriggerDao(): KiTriggerDao
    abstract fun genieCodexDao(): GenieCodexDao
    abstract fun whoopWorkoutDao(): WhoopWorkoutDao

    companion object {
        const val DB_NAME = "entropy_reducer.db"
    }
}
