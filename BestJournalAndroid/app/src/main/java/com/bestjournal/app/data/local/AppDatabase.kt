package com.bestjournal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bestjournal.app.data.local.dao.AdviceDashboardDao
import com.bestjournal.app.data.local.dao.EntryPhotoDao
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.local.entity.AdviceBlockEntity
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import com.bestjournal.app.data.local.entity.JournalEntryEntity

@Database(
    entities = [JournalEntryEntity::class, AdviceBlockEntity::class, EntryPhotoEntity::class],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun journalEntryDao(): JournalEntryDao

    abstract fun adviceDashboardDao(): AdviceDashboardDao

    abstract fun entryPhotoDao(): EntryPhotoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE journal_entries ADD COLUMN summary TEXT DEFAULT NULL")
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE journal_entries ADD COLUMN title TEXT DEFAULT NULL")
                }
            }

        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE advice_blocks ADD COLUMN topActionsJson TEXT NOT NULL DEFAULT '[]'"
                    )
                }
            }

        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS retrospective_summaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        periodLabel TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        summaryText TEXT NOT NULL,
                        periodIndex INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                    )
                }
            }

        private val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS entry_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY (entryId) REFERENCES journal_entries(id) ON DELETE CASCADE
                    )"""
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_entry_photos_entryId ON entry_photos(entryId)"
                    )
                }
            }

        private val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE entry_photos ADD COLUMN isVideo INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        private val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Retrospective summaries moved to separate database (retrospective_db)
                    db.execSQL("DROP TABLE IF EXISTS retrospective_summaries")
                }
            }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "entropy_journal_db",
                            )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8,
                            )
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
