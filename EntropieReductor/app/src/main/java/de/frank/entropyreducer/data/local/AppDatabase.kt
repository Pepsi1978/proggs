package de.frank.entropyreducer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.GenieCodexDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.dao.KiTriggerDao
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity

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
        MemoryEntryEntity::class,
        InsightEntity::class,
        BiomarkerSnapshotEntity::class,
        SupplementLogEntity::class,
        CalendarDayEntity::class,
        CalendarEventEntity::class,
        KiTriggerEntity::class,
        GenieCodexVersionEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(EntropyTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entropyEntryDao(): EntropyEntryDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun memoryDao(): MemoryDao
    abstract fun insightDao(): InsightDao
    abstract fun biomarkerSnapshotDao(): BiomarkerSnapshotDao
    abstract fun supplementLogDao(): SupplementLogDao
    abstract fun calendarDayDao(): CalendarDayDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun kiTriggerDao(): KiTriggerDao
    abstract fun genieCodexDao(): GenieCodexDao

    companion object {
        const val DB_NAME = "entropy_reducer.db"
    }
}
