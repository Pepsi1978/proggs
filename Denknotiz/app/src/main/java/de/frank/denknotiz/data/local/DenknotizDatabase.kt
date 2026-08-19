package de.frank.denknotiz.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun entryType(value: String): EntryType = EntryType.valueOf(value)
    @TypeConverter fun entryType(value: EntryType): String = value.name
    @TypeConverter fun snapshotStatus(value: String): SnapshotStatus = SnapshotStatus.valueOf(value)
    @TypeConverter fun snapshotStatus(value: SnapshotStatus): String = value.name
}

@Database(
    entities = [SessionEntity::class, EntryEntity::class, EvaluationSnapshotEntity::class, ContextBoundaryEntity::class, FolderEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DenknotizDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun entryDao(): EntryDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun boundaryDao(): BoundaryDao
    abstract fun folderDao(): FolderDao

    companion object {
        fun create(context: Context): DenknotizDatabase = Room.databaseBuilder(
            context.applicationContext,
            DenknotizDatabase::class.java,
            "denknotiz.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE evaluation_snapshots ADD COLUMN sourceNotesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE evaluation_snapshots ADD COLUMN profileInstruction TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN secured INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE sessions ADD COLUMN folderId TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS folders (id TEXT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
}
