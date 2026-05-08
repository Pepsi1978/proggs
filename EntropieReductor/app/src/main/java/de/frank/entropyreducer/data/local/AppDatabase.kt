package de.frank.entropyreducer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.GenieCodexDao
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.dao.KiTriggerDao
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity

@Database(
    entities = [
        EntropyEntryEntity::class,
        SavedPromptEntity::class,
        MemoryEntryEntity::class,
        ScientistSessionEntity::class,
        ScientistMessageEntity::class,
        HypothesisEntity::class,
        HypothesisMessageEntity::class,
        InsightEntity::class,
        BiomarkerSnapshotEntity::class,
        SupplementLogEntity::class,
        CalendarDayEntity::class,
        CalendarEventEntity::class,
        KiTriggerEntity::class,
        GenieCodexVersionEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(EntropyTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entropyEntryDao(): EntropyEntryDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun memoryDao(): MemoryDao
    abstract fun scientistSessionDao(): ScientistSessionDao
    abstract fun scientistMessageDao(): ScientistMessageDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun hypothesisMessageDao(): HypothesisMessageDao
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
