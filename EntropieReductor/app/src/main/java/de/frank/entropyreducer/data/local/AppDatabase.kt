package de.frank.entropyreducer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.frank.entropyreducer.data.local.dao.AmazfitDailyDao
import de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.GenieCodexDao
import de.frank.entropyreducer.data.local.dao.KiTriggerDao
import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.dao.WhoopWorkoutDao
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
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
        AmazfitDailyEntity::class,
        AmazfitWorkoutEntity::class,
    ],
    version = 13,
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
    abstract fun amazfitDailyDao(): AmazfitDailyDao
    abstract fun amazfitWorkoutDao(): AmazfitWorkoutDao

    companion object {
        const val DB_NAME = "entropy_reducer.db"

        /**
         * Migration 10 -> 11: Zwei neue Tabellen fuer die Amazfit T-Rex 3.
         * Frank-Wunsch 2026-05-09: Sport-Daten + PAI/BioCharge/Hauttemperatur
         * von der T-Rex 3 ohne Datenverlust an den bestehenden Whoop-Tabellen.
         *
         * - amazfit_daily: tagliche Werte (PAI, BioCharge, Hauttemperatur,
         *   SpO2, Stress, Atmung, Schritte, Kalorien, HRV)
         * - amazfit_workouts: Sport-Sessions (GPS-Track, Pulsverlauf, Splits,
         *   Pace, Geschwindigkeit, VO2Max, Trainingseffekt, Hauttemperatur)
         */
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS amazfit_daily (
                        date TEXT NOT NULL PRIMARY KEY,
                        capturedAt INTEGER NOT NULL,
                        paiScore INTEGER,
                        bioChargeScore INTEGER,
                        skinTempCelsius REAL,
                        spo2Percent REAL,
                        stressScore INTEGER,
                        respiratoryRate REAL,
                        steps INTEGER,
                        distanceMeters REAL,
                        activeCalories REAL,
                        activeMinutes INTEGER,
                        restingHeartRate INTEGER,
                        averageHeartRate INTEGER,
                        hrvMs REAL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS amazfit_workouts (
                        trackId TEXT NOT NULL PRIMARY KEY,
                        dateKey TEXT NOT NULL,
                        startMs INTEGER NOT NULL,
                        endMs INTEGER NOT NULL,
                        durationSeconds INTEGER,
                        sportType INTEGER,
                        sportName TEXT,
                        distanceMeters REAL,
                        avgPaceSecPerKm REAL,
                        maxPaceSecPerKm REAL,
                        avgSpeedKmh REAL,
                        maxSpeedKmh REAL,
                        calories REAL,
                        avgHeartRate INTEGER,
                        maxHeartRate INTEGER,
                        gpsTrackJson TEXT,
                        heartRateSeriesJson TEXT,
                        paceSeriesJson TEXT,
                        splitsJson TEXT,
                        altitudeGainMeters REAL,
                        altitudeLossMeters REAL,
                        trainingEffectAerobic REAL,
                        trainingEffectAnaerobic REAL,
                        vo2Max REAL,
                        cadence INTEGER,
                        strideLengthCm INTEGER,
                        recoveryTimeHours INTEGER,
                        skinTempCelsius REAL,
                        swolf INTEGER,
                        poolLaps INTEGER,
                        poolLengthMeters REAL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Migration 11 -> 12: Schlaf-Felder in amazfit_daily ergaenzen.
         * Frank-Wunsch 2026-05-09: Schlaf-Phasen aus dem Zepp-Summary mit anzeigen
         * (slp_dp, slp_lt, slp_wk, slp_to). Plus Sleep-Score und Sleep-REM.
         */
        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE amazfit_daily ADD COLUMN sleepTotalMinutes INTEGER")
                db.execSQL("ALTER TABLE amazfit_daily ADD COLUMN sleepDeepMinutes INTEGER")
                db.execSQL("ALTER TABLE amazfit_daily ADD COLUMN sleepLightMinutes INTEGER")
                db.execSQL("ALTER TABLE amazfit_daily ADD COLUMN sleepWakeMinutes INTEGER")
                db.execSQL("ALTER TABLE amazfit_daily ADD COLUMN sleepRemMinutes INTEGER")
                db.execSQL("ALTER TABLE amazfit_daily ADD COLUMN sleepScore INTEGER")
            }
        }

        /**
         * Migration 12 -> 13: amazfit_workouts.source + amazfit_workouts.city.
         * Frank-Wunsch 2026-05-09: source ist Pflicht-Parameter fuer den
         * Workout-Detail-Endpoint (GPS-Track + Pulsverlauf).
         */
        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN source TEXT")
                db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN city TEXT")
            }
        }
    }
}
