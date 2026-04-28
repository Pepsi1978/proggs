package com.bestjournal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bestjournal.app.data.local.dao.EntryFollowUpDao
import com.bestjournal.app.data.local.dao.EntryPhotoDao
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.local.entity.EntryFollowUpEntity
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import com.bestjournal.app.data.local.entity.JournalEntryEntity

@Database(
    entities = [JournalEntryEntity::class, EntryPhotoEntity::class, EntryFollowUpEntity::class],
    version = 12,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun journalEntryDao(): JournalEntryDao

    abstract fun entryPhotoDao(): EntryPhotoDao

    abstract fun entryFollowUpDao(): EntryFollowUpDao

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

        private val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Advice blocks moved to separate database (dashboard_db)
                    // so Google Auto Backup only backs up journal entries, not dashboard.
                    db.execSQL("DROP TABLE IF EXISTS advice_blocks")
                }
            }

        private val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_journal_entries_timestamp ON journal_entries(timestamp)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_journal_entries_isSynced ON journal_entries(isSynced)"
                    )
                }
            }

        // Nachtrag / follow-up support. Adds a denormalised `followUpText` summary
        // column to journal_entries (for fast search + legacy compatibility with
        // Frank's schema) and creates the new `entry_follow_ups` table with all
        // columns present from the very beginning (rawText + improvedText +
        // isImproved included) so we don't need a second migration step later.
        private val MIGRATION_10_11 =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE journal_entries ADD COLUMN followUpText TEXT DEFAULT NULL"
                    )
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS entry_follow_ups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryId INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        rawText TEXT NOT NULL DEFAULT '',
                        improvedText TEXT DEFAULT NULL,
                        isImproved INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (entryId) REFERENCES journal_entries(id) ON DELETE CASCADE
                    )"""
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_entry_follow_ups_entryId ON entry_follow_ups(entryId)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_entry_follow_ups_entryId_createdAt ON entry_follow_ups(entryId, createdAt)"
                    )
                }
            }

        // Drop the unused `entropyScore` column. Room/SQLite on minSdk 26 does not
        // reliably support `ALTER TABLE ... DROP COLUMN`, so we recreate the table
        // (preserving ids so existing entry_photos / entry_follow_ups foreign keys
        // remain valid) and rebuild the indices that MIGRATION_9_10 added.
        private val MIGRATION_11_12 =
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE journal_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        rawText TEXT NOT NULL,
                        improvedText TEXT,
                        isImproved INTEGER NOT NULL,
                        displayText TEXT NOT NULL,
                        audioDurationSeconds INTEGER NOT NULL,
                        moodTag TEXT,
                        adviceCategoryTags TEXT,
                        summary TEXT,
                        title TEXT,
                        followUpText TEXT,
                        isSynced INTEGER NOT NULL
                    )"""
                    )
                    db.execSQL(
                        """INSERT INTO journal_entries_new (
                            id, timestamp, rawText, improvedText, isImproved,
                            displayText, audioDurationSeconds, moodTag,
                            adviceCategoryTags, summary, title, followUpText, isSynced
                        ) SELECT
                            id, timestamp, rawText, improvedText, isImproved,
                            displayText, audioDurationSeconds, moodTag,
                            adviceCategoryTags, summary, title, followUpText, isSynced
                        FROM journal_entries"""
                    )
                    db.execSQL("DROP TABLE journal_entries")
                    db.execSQL("ALTER TABLE journal_entries_new RENAME TO journal_entries")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_journal_entries_timestamp ON journal_entries(timestamp)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_journal_entries_isSynced ON journal_entries(isSynced)"
                    )
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
                                MIGRATION_8_9,
                                MIGRATION_9_10,
                                MIGRATION_10_11,
                                MIGRATION_11_12,
                            )
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
