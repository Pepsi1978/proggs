package de.frank.denknotiz.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun entryType(value: String): EntryType = EntryType.valueOf(value)
    @TypeConverter fun entryType(value: EntryType): String = value.name
    @TypeConverter fun snapshotStatus(value: String): SnapshotStatus = SnapshotStatus.valueOf(value)
    @TypeConverter fun snapshotStatus(value: SnapshotStatus): String = value.name
}

@Database(
    entities = [SessionEntity::class, EntryEntity::class, EvaluationSnapshotEntity::class, ContextBoundaryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DenknotizDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun entryDao(): EntryDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun boundaryDao(): BoundaryDao

    companion object {
        fun create(context: Context): DenknotizDatabase = Room.databaseBuilder(
            context.applicationContext,
            DenknotizDatabase::class.java,
            "denknotiz.db",
        ).build()
    }
}
