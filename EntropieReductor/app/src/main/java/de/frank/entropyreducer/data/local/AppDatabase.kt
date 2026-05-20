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
import de.frank.entropyreducer.data.local.dao.HealthConnectValueDao
import de.frank.entropyreducer.data.local.dao.KiTriggerDao
import de.frank.entropyreducer.data.local.dao.OuraActivityDao
import de.frank.entropyreducer.data.local.dao.OuraDailySleepDao
import de.frank.entropyreducer.data.local.dao.OuraPersonalInfoDao
import de.frank.entropyreducer.data.local.dao.OuraReadinessDao
import de.frank.entropyreducer.data.local.dao.OuraResilienceDao
import de.frank.entropyreducer.data.local.dao.OuraSleepDetailDao
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
import de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraPersonalInfoEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity

/**
 * Haupt-Datenbank — enthaelt alle nicht-Forscher-Daten. Frank-Wunsch 2026-05-09: Die
 * rein-Forscher-spezifischen Tabellen (scientist_sessions, scientist_messages, hypotheses,
 * hypothesis_messages) sind in eine separate ScientistDatabase ausgelagert, damit sie ueber
 * Schema-Aenderungen dieser Haupt-DB hinweg persistent bleiben. Siehe ScientistDatabase.kt fuer
 * Details.
 *
 * Insights und Memory bleiben hier — sie werden cross-cutting genutzt (Briefing, Repertoire,
 * Aufgaben-Antworten) und sind beim destruktiven Reset weniger schmerzhaft weil Gemini sie aus den
 * verbleibenden Daten wieder ableiten kann.
 *
 * Version 7: scientist_sessions/scientist_messages/hypotheses/hypothesis_messages sind aus dieser
 * DB entfernt — sie leben jetzt in der Forscher-DB.
 */
@Database(
    entities =
        [
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
            OuraReadinessEntity::class,
            OuraDailySleepEntity::class,
            OuraActivityEntity::class,
            OuraResilienceEntity::class,
            OuraSleepDetailEntity::class,
            OuraPersonalInfoEntity::class,
            HealthConnectValueEntity::class,
            de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity::class,
        ],
    version = 20,
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

    abstract fun ouraReadinessDao(): OuraReadinessDao

    abstract fun ouraDailySleepDao(): OuraDailySleepDao

    abstract fun ouraActivityDao(): OuraActivityDao

    abstract fun ouraResilienceDao(): OuraResilienceDao

    abstract fun ouraSleepDetailDao(): OuraSleepDetailDao

    abstract fun ouraPersonalInfoDao(): OuraPersonalInfoDao

    abstract fun healthConnectValueDao(): HealthConnectValueDao

    abstract fun entropyEntryFollowupDao():
        de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao

    companion object {
        const val DB_NAME = "entropy_reducer.db"

        /**
         * Migration 10 -> 11: Zwei neue Tabellen fuer die Amazfit T-Rex 3. Frank-Wunsch 2026-05-09:
         * Sport-Daten + PAI/BioCharge/Hauttemperatur von der T-Rex 3 ohne Datenverlust an den
         * bestehenden Whoop-Tabellen.
         *
         * - amazfit_daily: tagliche Werte (PAI, BioCharge, Hauttemperatur, SpO2, Stress, Atmung,
         *   Schritte, Kalorien, HRV)
         * - amazfit_workouts: Sport-Sessions (GPS-Track, Pulsverlauf, Splits, Pace,
         *   Geschwindigkeit, VO2Max, Trainingseffekt, Hauttemperatur)
         */
        val MIGRATION_10_11: Migration =
            object : Migration(10, 11) {
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
                        """
                            .trimIndent()
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
                        """
                            .trimIndent()
                    )
                }
            }

        /**
         * Migration 11 -> 12: Schlaf-Felder in amazfit_daily ergaenzen. Frank-Wunsch 2026-05-09:
         * Schlaf-Phasen aus dem Zepp-Summary mit anzeigen (slp_dp, slp_lt, slp_wk, slp_to). Plus
         * Sleep-Score und Sleep-REM.
         */
        val MIGRATION_11_12: Migration =
            object : Migration(11, 12) {
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
         * Migration 12 -> 13: amazfit_workouts.source + amazfit_workouts.city. Frank-Wunsch
         * 2026-05-09: source ist Pflicht-Parameter fuer den Workout-Detail-Endpoint (GPS-Track +
         * Pulsverlauf).
         */
        val MIGRATION_12_13: Migration =
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN source TEXT")
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN city TEXT")
                }
            }

        /**
         * Migration 13 -> 14: paceStreamJson Spalte fuer hochaufgeloeste Pace-Sample-Stream (~3000
         * Werte pro Workout) — Frank-Wunsch 2026-05-09 fuer fluessigen Tempo-Verlauf.
         */
        val MIGRATION_13_14: Migration =
            object : Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN paceStreamJson TEXT")
                }
            }

        /**
         * Migration 14 -> 15: Sechs neue Tabellen fuer den Oura Ring (Frank-Wunsch 2026-05-10).
         * Anbindung als dritte Datenquelle neben Whoop und Amazfit. KEIN ALTER auf bestehende
         * Tabellen — reine Erweiterung, daher risikofrei fuer alle bisherigen Daten.
         *
         * - oura_daily_readiness: Tages-Readiness-Score + Hauttemperatur-Abweichung
         * - oura_daily_sleep: Tages-Sleep-Score mit 7 Contributors
         * - oura_daily_activity: Tages-Activity-Score, Schritte, Kalorien
         * - oura_daily_resilience: Tages-Resilience-Level + Sub-Faktoren
         * - oura_sleep_detail: Detaillierte Schlaf-Sessions mit Phasen alle 5 Min
         * - oura_personal_info: Stammdaten als Single-Row (id = 1)
         */
        val MIGRATION_14_15: Migration =
            object : Migration(14, 15) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS oura_daily_readiness (
                            day TEXT NOT NULL PRIMARY KEY,
                            capturedAt INTEGER NOT NULL,
                            score INTEGER,
                            temperatureDeviation REAL,
                            temperatureTrendDeviation REAL,
                            activityBalance INTEGER,
                            bodyTemperature INTEGER,
                            hrvBalance INTEGER,
                            previousDayActivity INTEGER,
                            previousNight INTEGER,
                            recoveryIndex INTEGER,
                            restingHeartRate INTEGER,
                            sleepBalance INTEGER,
                            createdAt INTEGER NOT NULL
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS oura_daily_sleep (
                            day TEXT NOT NULL PRIMARY KEY,
                            capturedAt INTEGER NOT NULL,
                            score INTEGER,
                            deepSleepScore INTEGER,
                            efficiencyScore INTEGER,
                            latencyScore INTEGER,
                            remSleepScore INTEGER,
                            restfulnessScore INTEGER,
                            timingScore INTEGER,
                            totalSleepScore INTEGER,
                            createdAt INTEGER NOT NULL
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS oura_daily_activity (
                            day TEXT NOT NULL PRIMARY KEY,
                            capturedAt INTEGER NOT NULL,
                            score INTEGER,
                            activeCalories INTEGER,
                            totalCalories INTEGER,
                            targetCalories INTEGER,
                            steps INTEGER,
                            walkingDistanceMeters INTEGER,
                            highActivitySeconds INTEGER,
                            mediumActivitySeconds INTEGER,
                            lowActivitySeconds INTEGER,
                            nonWearSeconds INTEGER,
                            restingSeconds INTEGER,
                            sedentarySeconds INTEGER,
                            inactivityAlerts INTEGER,
                            createdAt INTEGER NOT NULL
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS oura_daily_resilience (
                            day TEXT NOT NULL PRIMARY KEY,
                            capturedAt INTEGER NOT NULL,
                            level TEXT,
                            sleepRecovery REAL,
                            daytimeRecovery REAL,
                            stress REAL,
                            createdAt INTEGER NOT NULL
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS oura_sleep_detail (
                            id TEXT NOT NULL PRIMARY KEY,
                            day TEXT NOT NULL,
                            capturedAt INTEGER NOT NULL,
                            bedtimeStart TEXT,
                            bedtimeEnd TEXT,
                            type TEXT,
                            totalSleepSeconds INTEGER,
                            timeInBedSeconds INTEGER,
                            awakeSeconds INTEGER,
                            lightSeconds INTEGER,
                            deepSeconds INTEGER,
                            remSeconds INTEGER,
                            efficiency INTEGER,
                            latencySeconds INTEGER,
                            restlessPeriods INTEGER,
                            averageBreath REAL,
                            averageHeartRate REAL,
                            averageHrv INTEGER,
                            lowestHeartRate INTEGER,
                            sleepPhase5Min TEXT,
                            createdAt INTEGER NOT NULL
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS oura_personal_info (
                            id INTEGER NOT NULL PRIMARY KEY,
                            ouraUserId TEXT,
                            age INTEGER,
                            weightKg REAL,
                            heightMeters REAL,
                            biologicalSex TEXT,
                            email TEXT,
                            capturedAt INTEGER NOT NULL
                        )
                        """
                            .trimIndent()
                    )
                }
            }

        /**
         * Schema 15 -> 16 (Performance-Audit Loop 1, 2026-05-10): Indizes auf Spalten die in
         * WHERE/ORDER BY oft gefiltert werden, um Full- Table-Scans zu vermeiden.
         * biomarker_snapshots.capturedAt wird in den Range-Queries des Dashboard-4-ViewModels
         * permanent gefiltert; whoop_workouts und amazfit_workouts werden nach startMs und dateKey
         * gefiltert; amazfit_daily nach capturedAt; oura_sleep_detail nach day. Ohne diese Indizes
         * laufen die Queries als Full Scan, was bei 365+ Tagen Daten messbar Recompositions
         * verzoegert.
         *
         * IF NOT EXISTS schuetzt vor doppelter Anlage. Index-Namen folgen dem Room- Konvention
         * "index_<table>_<column>".
         */
        val MIGRATION_15_16: Migration =
            object : Migration(15, 16) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_biomarker_snapshots_capturedAt ON biomarker_snapshots(capturedAt)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_whoop_workouts_startMs ON whoop_workouts(startMs)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_whoop_workouts_dateKey ON whoop_workouts(dateKey)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_amazfit_daily_capturedAt ON amazfit_daily(capturedAt)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_amazfit_workouts_startMs ON amazfit_workouts(startMs)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_amazfit_workouts_dateKey ON amazfit_workouts(dateKey)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_oura_sleep_detail_day ON oura_sleep_detail(day)"
                    )
                }
            }

        /**
         * Schema 17 -> 18 (Frank-Wunsch 2026-05-17): Neue Spalte `manualOverridesMs` in
         * amazfit_workouts. Markiert ob der Benutzer manuelle Edits vorgenommen hat — null = keine.
         * Beim Strava- Sync werden Workouts mit gesetztem manualOverridesMs nur in den
         * Stream-Feldern aktualisiert, nicht in den Summary-Werten.
         */
        val MIGRATION_17_18: Migration =
            object : Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN manualOverridesMs INTEGER")
                }
            }

        /**
         * Schema 18 -> 19 (Frank-Wunsch 2026-05-17, Iteration 2): Neue Spalte
         * `manualOverrideFields` in amazfit_workouts — komma- separierte Liste der Labels (genau
         * wie im StatsGrid) der manuell editierten Felder. Ermoeglicht das Anzeigen eines
         * Schloss-Icons pro Stat-Karte im Detail-Screen statt nur einem globalen Schloss.
         */
        val MIGRATION_18_19: Migration =
            object : Migration(18, 19) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN manualOverrideFields TEXT")
                }
            }

        /**
         * Schema 16 -> 17 (Frank-Wunsch 2026-05-10 abend): Cross-Device-Cache fuer
         * Health-Connect-Werte. Wird beim Backup mit-exportiert und beim Restore auf einem anderen
         * Geraet eingespielt. Damit hat die App ueber Geraete-Wechsel hinweg den vollen HC-Verlauf,
         * auch wenn Zepp auf dem neuen Geraet die alten Werte nicht in HC pushen kann.
         */
        val MIGRATION_16_17: Migration =
            object : Migration(16, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS hc_value_cache (
                            metric TEXT NOT NULL,
                            timestampMs INTEGER NOT NULL,
                            value REAL NOT NULL,
                            createdAt INTEGER NOT NULL,
                            PRIMARY KEY(metric, timestampMs)
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_hc_value_cache_metric ON hc_value_cache(metric)"
                    )
                }
            }

        /**
         * Schema 19 -> 20 (Frank-Wunsch 2026-05-20): Neue Tabelle `entropy_entry_followups` fuer
         * strukturierte Nachtraege pro Aufgabe. Vorher wurden Nachtraege als Text an description
         * angehaengt — das verlor Zeitstempel und Verbessert/Original-Tabs.
         *
         * Foreign Key auf entropy_entries.id mit ON DELETE CASCADE: wenn ein Eintrag geloescht
         * wird, fliegen seine Nachtraege automatisch mit. Indizes auf entryId und (entryId,
         * createdAt) fuer schnelle Queries im Detail-Screen.
         */
        val MIGRATION_19_20: Migration =
            object : Migration(19, 20) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS entropy_entry_followups (
                            id TEXT NOT NULL PRIMARY KEY,
                            entryId TEXT NOT NULL,
                            rawText TEXT NOT NULL,
                            improvedText TEXT,
                            isImproved INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            FOREIGN KEY(entryId) REFERENCES entropy_entries(id) ON DELETE CASCADE
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_entropy_entry_followups_entryId ON entropy_entry_followups(entryId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_entropy_entry_followups_entryId_createdAt ON entropy_entry_followups(entryId, createdAt)"
                    )
                }
            }
    }
}
