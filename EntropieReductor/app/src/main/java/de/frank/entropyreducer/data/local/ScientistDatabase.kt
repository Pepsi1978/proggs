package de.frank.entropyreducer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity

/**
 * Frank-Wunsch 2026-05-09: Forscher-Daten leben in einer EIGENEN Datenbank-Datei,
 * unabhaengig von der Haupt-AppDatabase. So koennen Sessions, Hypothesen und
 * Diskussions-Verlaeufe ueber App-Updates persistent bleiben — auch wenn die
 * Haupt-DB durch Schema-Aenderungen destruktiv resettet wird.
 *
 * Hier nur die rein-Forscher-spezifischen Tabellen. Insights und Memory bleiben
 * absichtlich in AppDatabase, weil sie cross-cutting genutzt werden (Briefing,
 * Repertoire, Aufgaben-Antworten) und bei einem destruktiven Reset von Memory/
 * Insights kommen sie sowieso ueber Gemini wieder zurueck. Sessions/Hypothesen
 * dagegen sind Frank's eigentliches Tagebuch — die muessen ueberleben.
 *
 * Schema-Stabilitaet ist hier hoechste Prioritaet: KEIN destructive fallback in
 * DatabaseModule fuer diese DB. Wenn das Schema sich aendert, MUSS eine echte
 * Migration geschrieben werden — sonst crasht die App und ich werde gezwungen
 * sauber zu arbeiten (Poka-Yoke Stufe 3).
 */
@Database(
    entities = [
        ScientistSessionEntity::class,
        ScientistMessageEntity::class,
        HypothesisEntity::class,
        HypothesisMessageEntity::class,
        // Frank-Wunsch 2026-05-09 (Abend): Insights und Memories aus AppDatabase
        // herausgezogen damit sie schema-stabil persistent werden und ins
        // Drive-Backup mitgesichert werden koennen. Vorher liefen sie in der
        // destructive-fallback AppDatabase und waren bei jedem Schema-Bump weg.
        InsightEntity::class,
        MemoryEntryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(EntropyTypeConverters::class)
abstract class ScientistDatabase : RoomDatabase() {
    abstract fun scientistSessionDao(): ScientistSessionDao
    abstract fun scientistMessageDao(): ScientistMessageDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun hypothesisMessageDao(): HypothesisMessageDao
    abstract fun insightDao(): InsightDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        const val DB_NAME = "entropy_reducer_scientist.db"

        /**
         * Migration 1 → 2 (2026-05-09 Abend): Insights und Memories aus AppDatabase
         * in diese DB verschoben. Die zwei Tabellen werden hier neu angelegt — die
         * alten Daten kommen via [InitialDataMigrator] beim App-Start aus der
         * alten AppDatabase rueber, BEVOR Room die AppDatabase auf Version 10
         * destructive resettet.
         *
         * Die Spalten-Definitionen muessen exakt zu den Entity-Definitionen passen
         * (siehe ScientistEntities.kt fuer InsightEntity, KnowledgeEntities.kt fuer
         * MemoryEntryEntity). Room-Compiler validiert das beim Build.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // insights-Tabelle (siehe InsightEntity in ScientistEntities.kt)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `insights` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `targetCategory` TEXT NOT NULL,
                        `additionalCategories` TEXT NOT NULL,
                        `confidence` INTEGER NOT NULL,
                        `successCount` INTEGER NOT NULL,
                        `attemptCount` INTEGER NOT NULL,
                        `avgBiomarkerImpact` TEXT,
                        `avgFeltImpact` REAL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `sourceHypothesisIds` TEXT NOT NULL,
                        `manualSource` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // memory_entries-Tabelle (siehe MemoryEntryEntity in KnowledgeEntities.kt)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `memory_entries` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `content` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `confidence` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
