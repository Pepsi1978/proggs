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
import de.frank.entropyreducer.data.local.dao.PromptExecutionDao
import de.frank.entropyreducer.data.local.dao.PromptExecutionStepDao
import de.frank.entropyreducer.data.local.dao.PromptToolPermissionDao
import de.frank.entropyreducer.data.local.dao.PromptTriggerDao
import de.frank.entropyreducer.data.local.dao.RecurringTemplateDao
import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.dao.TokenUsageDailyDao
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
import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.data.local.entities.PromptToolPermissionEntity
import de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity
import de.frank.entropyreducer.data.local.entities.TokenUsageDailyEntity
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
            // Agentic-AI Tabellen (Frank-Wunsch 2026-05-21)
            PromptExecutionEntity::class,
            PromptExecutionStepEntity::class,
            PromptToolPermissionEntity::class,
            TokenUsageDailyEntity::class,
            PromptTriggerEntity::class,
            // Wiederkehrende Aufgaben (Sprint 2, Frank-Wunsch 2026-05-22)
            RecurringTemplateEntity::class,
            // Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19)
            de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity::class,
            // ID-Architektur Etappe 2 (Frank-Wunsch 2026-06-19): Kern-Kette Aufgaben
            de.frank.entropyreducer.data.local.entities.IdeaEntity::class,
            de.frank.entropyreducer.data.local.entities.IdeaFollowupEntity::class,
            de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity::class,
            // ID-Architektur Etappe 3 (Frank-Wunsch 2026-06-19): Kern-Kette Gewohnheiten
            de.frank.entropyreducer.data.local.entities.HabitEntity::class,
            de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity::class,
            // ID-Architektur Etappe 4 (Frank-Wunsch 2026-06-19): Mental-Board-Saetze
            de.frank.entropyreducer.data.local.entities.MentalEntity::class,
        ],
    version = 36,
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

    // Agentic-AI DAOs (Frank-Wunsch 2026-05-21)
    abstract fun promptExecutionDao(): PromptExecutionDao

    abstract fun promptExecutionStepDao(): PromptExecutionStepDao

    abstract fun promptToolPermissionDao(): PromptToolPermissionDao

    abstract fun tokenUsageDailyDao(): TokenUsageDailyDao

    abstract fun promptTriggerDao(): PromptTriggerDao

    /** Wiederkehrende Aufgaben (Sprint 2, Frank-Wunsch 2026-05-22). */
    abstract fun recurringTemplateDao(): RecurringTemplateDao

    /** Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19). */
    abstract fun priorityMemoryDao(): de.frank.entropyreducer.data.local.dao.PriorityMemoryDao

    /** Ideen + Nachtraege (ID-Architektur Etappe 2, Frank-Wunsch 2026-06-19). */
    abstract fun ideaDao(): de.frank.entropyreducer.data.local.dao.IdeaDao

    /** Aufgaben-Vorschlaege (ID-Architektur Etappe 2, Frank-Wunsch 2026-06-19). */
    abstract fun taskSuggestionDao(): de.frank.entropyreducer.data.local.dao.TaskSuggestionDao

    /** Gewohnheiten (ID-Architektur Etappe 3, Frank-Wunsch 2026-06-19). */
    abstract fun habitDao(): de.frank.entropyreducer.data.local.dao.HabitDao

    /** Gewohnheits-Vorschlaege (ID-Architektur Etappe 3, Frank-Wunsch 2026-06-19). */
    abstract fun habitSuggestionDao(): de.frank.entropyreducer.data.local.dao.HabitSuggestionDao

    /** Mental-Board-Saetze (ID-Architektur Etappe 4, Frank-Wunsch 2026-06-19). */
    abstract fun mentalSentenceDao(): de.frank.entropyreducer.data.local.dao.MentalSentenceDao

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
         * Beim Trainings-Sync werden Workouts mit gesetztem manualOverridesMs nur in den
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

        /**
         * Schema 20 -> 21 (Frank-Wunsch 2026-05-20): Neue Spalte `category` in saved_prompts.
         * Bestehende Prompts werden auf AUFGABEN gesetzt (Standard-Fall vor der Kategorisierung).
         * Pro Bereich der App (Aufgaben, Entropie, Thesen, Analyse, Forscher, Codex) wirken nun nur
         * die Prompts der jeweiligen Kategorie, statt aller aktiven Prompts global.
         */
        val MIGRATION_20_21: Migration =
            object : Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE saved_prompts ADD COLUMN category TEXT NOT NULL DEFAULT 'AUFGABEN'"
                    )
                }
            }

        /**
         * Schema 21 -> 22 (Frank-Wunsch 2026-05-21): Agentic-AI-Prompts. Erweitert saved_prompts
         * um 3 Spalten (model, tokenLimitPerDay, trustModeDefault) und legt 5 neue Tabellen an:
         *
         * - prompt_executions: Audit-Log aller Prompt-Ausfuehrungen mit Snapshot des Prompts
         * - prompt_execution_steps: Feinkoernige Schritte im ReAct-Loop
         * - prompt_tool_permissions: Write-Tool-Freischaltung pro Prompt
         * - token_usage_daily: Tagesaggregierte Tokens fuer Balkendiagramm-Performance
         * - prompt_triggers: Auto-Ausfuehrungs-Konfig (CRON/EVENT/CHAIN, WorkManager-basiert)
         *
         * Foreign-Key-Strategie:
         * - prompt_executions hat KEINEN FK auf saved_prompts.id — Audit ueberlebt Loeschung
         * - alle anderen 4 Tabellen haben FK auf saved_prompts.id mit ON DELETE CASCADE
         * - prompt_execution_steps hat FK auf prompt_executions.id mit ON DELETE CASCADE
         *
         * Sicherheit: alle ADD COLUMNs haben Defaults (kein Datenverlust). Bestehende Prompts
         * bekommen model='gemini-2.5-flash', tokenLimitPerDay=NULL, trustModeDefault=0.
         */
        val MIGRATION_21_22: Migration =
            object : Migration(21, 22) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 1) saved_prompts erweitern (3 neue Spalten mit Defaults — kein Datenverlust)
                    db.execSQL(
                        "ALTER TABLE saved_prompts ADD COLUMN model TEXT NOT NULL DEFAULT 'gemini-2.5-flash'"
                    )
                    db.execSQL("ALTER TABLE saved_prompts ADD COLUMN tokenLimitPerDay INTEGER")
                    db.execSQL(
                        "ALTER TABLE saved_prompts ADD COLUMN trustModeDefault INTEGER NOT NULL DEFAULT 0"
                    )

                    // 2) prompt_executions — Audit-Log (KEIN FK auf saved_prompts!)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS prompt_executions (
                            id TEXT NOT NULL PRIMARY KEY,
                            promptId TEXT NOT NULL,
                            snapshotName TEXT NOT NULL,
                            snapshotContent TEXT NOT NULL,
                            snapshotCategory TEXT NOT NULL,
                            snapshotModel TEXT NOT NULL,
                            userInputContext TEXT,
                            startedAt INTEGER NOT NULL,
                            finishedAt INTEGER,
                            status TEXT NOT NULL,
                            finalAnswer TEXT,
                            errorMessage TEXT,
                            tokensInput INTEGER NOT NULL DEFAULT 0,
                            tokensOutput INTEGER NOT NULL DEFAULT 0,
                            tokensTotal INTEGER NOT NULL DEFAULT 0,
                            toolCallCount INTEGER NOT NULL DEFAULT 0,
                            modelUsed TEXT NOT NULL,
                            triggerSource TEXT NOT NULL DEFAULT 'MANUAL'
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_executions_promptId ON prompt_executions(promptId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_executions_startedAt ON prompt_executions(startedAt)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_executions_status ON prompt_executions(status)"
                    )

                    // 3) prompt_execution_steps — Feinkoernige Schritte (FK CASCADE auf Execution)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS prompt_execution_steps (
                            id TEXT NOT NULL PRIMARY KEY,
                            executionId TEXT NOT NULL,
                            stepIndex INTEGER NOT NULL,
                            stepType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            toolName TEXT,
                            toolArgsJson TEXT,
                            toolResultJson TEXT,
                            llmTextOutput TEXT,
                            confirmDecision TEXT,
                            createdEntityIds TEXT NOT NULL DEFAULT '[]',
                            updatedEntityIds TEXT NOT NULL DEFAULT '[]',
                            deletedEntityIds TEXT NOT NULL DEFAULT '[]',
                            error TEXT,
                            FOREIGN KEY(executionId) REFERENCES prompt_executions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_execution_steps_executionId ON prompt_execution_steps(executionId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_execution_steps_executionId_stepIndex ON prompt_execution_steps(executionId, stepIndex)"
                    )

                    // 4) prompt_tool_permissions — Write-Tool-Freischaltung pro Prompt
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS prompt_tool_permissions (
                            id TEXT NOT NULL PRIMARY KEY,
                            promptId TEXT NOT NULL,
                            toolName TEXT NOT NULL,
                            granted INTEGER NOT NULL DEFAULT 0,
                            trustMode INTEGER NOT NULL DEFAULT 0,
                            FOREIGN KEY(promptId) REFERENCES saved_prompts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_prompt_tool_permissions_promptId_toolName ON prompt_tool_permissions(promptId, toolName)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_tool_permissions_promptId ON prompt_tool_permissions(promptId)"
                    )

                    // 5) token_usage_daily — Tagesaggregation pro Prompt fuer Balkendiagramm
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS token_usage_daily (
                            id TEXT NOT NULL PRIMARY KEY,
                            promptId TEXT NOT NULL,
                            day TEXT NOT NULL,
                            tokensInput INTEGER NOT NULL DEFAULT 0,
                            tokensOutput INTEGER NOT NULL DEFAULT 0,
                            tokensTotal INTEGER NOT NULL DEFAULT 0,
                            runCount INTEGER NOT NULL DEFAULT 0,
                            FOREIGN KEY(promptId) REFERENCES saved_prompts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_token_usage_daily_promptId_day ON token_usage_daily(promptId, day)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_token_usage_daily_day ON token_usage_daily(day)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_token_usage_daily_promptId ON token_usage_daily(promptId)"
                    )

                    // 6) prompt_triggers — Auto-Ausfuehrungs-Konfiguration (Stufe 3)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS prompt_triggers (
                            id TEXT NOT NULL PRIMARY KEY,
                            promptId TEXT NOT NULL,
                            triggerType TEXT NOT NULL,
                            cronExpression TEXT,
                            eventCondition TEXT,
                            chainAfterPromptId TEXT,
                            isActive INTEGER NOT NULL DEFAULT 1,
                            lastRunAt INTEGER,
                            nextScheduledAt INTEGER,
                            FOREIGN KEY(promptId) REFERENCES saved_prompts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """
                            .trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_triggers_promptId ON prompt_triggers(promptId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_triggers_nextScheduledAt ON prompt_triggers(nextScheduledAt)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_triggers_chainAfterPromptId ON prompt_triggers(chainAfterPromptId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_prompt_triggers_isActive ON prompt_triggers(isActive)"
                    )
                }
            }

        /**
         * Schema 22 -> 23 (Frank-Wunsch 2026-05-22 zweite Iteration): Marker ob die
         * estimatedDurationMinutes manuell vom Benutzer gesetzt wurde. Default 0
         * (= false) damit bestehende Eintraege als "KI-Schaetzung" gelten und beim
         * naechsten Rescore aktualisiert werden duerfen.
         */
        val MIGRATION_22_23: Migration =
            object : Migration(22, 23) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE entropy_entries ADD COLUMN durationManuallySet INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        /**
         * Schema 23 -> 24 (Frank-Wunsch 2026-05-22 dritte Iteration): Frist
         * (Deadline) pro Eintrag. Nullable INTEGER (epoch ms). Default NULL =
         * keine Frist.
         */
        val MIGRATION_23_24: Migration =
            object : Migration(23, 24) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE entropy_entries ADD COLUMN dueAtMs INTEGER")
                }
            }

        /**
         * Schema 24 -> 25 (Sprint 2, Frank-Wunsch 2026-05-22): Wiederkehrende Aufgaben.
         * Neue Tabelle recurring_templates mit RFC-5545-RRULE-Feld. Beim App-Start
         * erzeugt GenerateRecurringInstancesUseCase aus aktiven Vorlagen normale
         * EntropyEntries.
         */
        val MIGRATION_24_25: Migration =
            object : Migration(24, 25) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS recurring_templates (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            description TEXT,
                            category TEXT NOT NULL,
                            priorityScore INTEGER NOT NULL,
                            severity INTEGER NOT NULL,
                            estimatedDurationMinutes INTEGER,
                            rrule TEXT NOT NULL,
                            timeOfDayMinutes INTEGER NOT NULL DEFAULT 480,
                            untilEpochMs INTEGER,
                            nextOccurrenceAt INTEGER,
                            lastGeneratedAt INTEGER NOT NULL DEFAULT 0,
                            occurrenceCount INTEGER NOT NULL DEFAULT 0,
                            isActive INTEGER NOT NULL DEFAULT 1,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_recurring_templates_isActive " +
                            "ON recurring_templates(isActive)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_recurring_templates_nextOccurrenceAt " +
                            "ON recurring_templates(nextOccurrenceAt)"
                    )
                }
            }

        /**
         * Schema 25 -> 26 (Frank-Wunsch 2026-05-31): manuelle Prioritaet. Neue Spalte
         * manualPriorityScore (REAL, nullable) — wenn gesetzt, hat sie Vorrang vor der
         * KI-Prioritaet. null = KI bestimmt weiterhin.
         */
        val MIGRATION_25_26: Migration =
            object : Migration(25, 26) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE entropy_entries ADD COLUMN manualPriorityScore REAL")
                }
            }

        /**
         * Schema 26 -> 27 (Frank-Wunsch 2026-05-31): Loop-Vorlagen koennen einen
         * Ziel-Bucket vorgeben. Neue Spalte targetBucket (TEXT, nullable) — speichert
         * TimeBucket.name. null = "KI"/automatisch HEUTE (bisheriges Verhalten).
         */
        val MIGRATION_26_27: Migration =
            object : Migration(26, 27) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE recurring_templates ADD COLUMN targetBucket TEXT")
                }
            }

        /**
         * Schema 27 -> 28 (Frank-Wunsch 2026-06-01): festes Wiederkehr-Intervall in Tagen.
         * Neue Spalte intervalDays (INTEGER, nullable) — null = "KI entscheidet" (bisheriges
         * Verhalten). Wenn gesetzt (z.B. 5), erscheint die naechste Loop-Instanz erst N Tage
         * nach der letzten Erledigung.
         */
        val MIGRATION_27_28: Migration =
            object : Migration(27, 28) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE recurring_templates ADD COLUMN intervalDays INTEGER")
                }
            }

        /**
         * Schema 28 -> 29 (Frank-Wunsch 2026-06-19): Marker fuer manuell an der Instanz
         * gesetzte Prioritaet. Neue Spalte manualPriorityScoreSetAt (INTEGER, nullable) —
         * null = nicht vom Nutzer gesetzt (Loop/Template/KI). Wenn gesetzt, schlaegt die
         * manuelle Prio die Loop-Pflege (analog manualBucketSetAt beim Bucket-Rollback-Fix).
         */
        val MIGRATION_28_29: Migration =
            object : Migration(28, 29) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE entropy_entries ADD COLUMN manualPriorityScoreSetAt INTEGER")
                }
            }

        /**
         * Schema 29 -> 30 (Frank-Wunsch 2026-06-19): Prioritaets-Gedaechtnis. Neue Tabelle
         * priority_memory speichert manuell gesetzte Aufgaben-Prioritaeten, damit die KI bei
         * neuen, sehr aehnlichen Aufgaben dieselbe Prioritaet vorschlagen kann. Keine Fremdschluessel
         * (eigenstaendig, ueberlebt das Loeschen der Ursprungsaufgabe). Index-Namen folgen der
         * Room-Konvention index_<table>_<column>, damit der Schema-Abgleich exakt passt.
         */
        val MIGRATION_29_30: Migration =
            object : Migration(29, 30) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS priority_memory (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            priority REAL NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            sourceEntryId TEXT
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_priority_memory_updatedAt " +
                            "ON priority_memory(updatedAt)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_priority_memory_sourceEntryId " +
                            "ON priority_memory(sourceEntryId)"
                    )
                }
            }

        /**
         * Schema 30 -> 31 (Frank-Wunsch 2026-06-19, ID-Architektur Etappe 1): Herkunfts-Kette.
         * Drei neue nullable Spalten in entropy_entries:
         * - originId   = direkter Vorgaenger (z. B. der Aufgaben-Vorschlag, aus dem die Aufgabe entstand)
         * - originType = Art des Vorgaengers (IDEA / TASK_SUGGESTION / ...)
         * - rootId     = Ur-Eintrag der Kette
         * Rein additiv (nullable, kein Default) -> KEIN Datenverlust. Bestandseintraege bleiben NULL
         * (= Ursprung bzw. vor dem Umbau). Muss exakt zum Entity-Schema passen, sonst greift der
         * destructive fallback (siehe DatabaseModule).
         */
        val MIGRATION_30_31: Migration =
            object : Migration(30, 31) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE entropy_entries ADD COLUMN originId TEXT")
                    db.execSQL("ALTER TABLE entropy_entries ADD COLUMN originType TEXT")
                    db.execSQL("ALTER TABLE entropy_entries ADD COLUMN rootId TEXT")
                }
            }

        /**
         * Schema 31 -> 32 (Frank-Wunsch 2026-06-19, ID-Architektur Etappe 2a): Drei neue Tabellen
         * fuer die Kern-Kette Aufgaben — ideas, idea_followups, task_suggestions. Rein additiv
         * (CREATE TABLE), keine bestehenden Daten betroffen. Werden in 2b mit den Bestandsdaten aus
         * den DataStore-JSONs befuellt.
         *
         * WICHTIG: KEINE SQL-DEFAULT-Klauseln — die Kotlin-Defaults (z. B. isImproved = false)
         * erzeugen KEINEN SQL-Default im Room-Schema. Ein DEFAULT hier wuerde einen Schema-Mismatch
         * (-> destructive fallback) ausloesen. Spalten-/Index-Namen exakt nach den Entity-Definitionen.
         */
        val MIGRATION_31_32: Migration =
            object : Migration(31, 32) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS ideas (
                            id TEXT NOT NULL PRIMARY KEY,
                            timestampMs INTEGER NOT NULL,
                            title TEXT NOT NULL,
                            text TEXT NOT NULL,
                            summary TEXT,
                            improvedText TEXT,
                            isImproved INTEGER NOT NULL,
                            originId TEXT,
                            originType TEXT,
                            rootId TEXT
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_ideas_timestampMs ON ideas(timestampMs)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_ideas_originId ON ideas(originId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_ideas_rootId ON ideas(rootId)")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS idea_followups (
                            id TEXT NOT NULL PRIMARY KEY,
                            ideaId TEXT NOT NULL,
                            createdAtMs INTEGER NOT NULL,
                            text TEXT NOT NULL,
                            improvedText TEXT,
                            isImproved INTEGER NOT NULL,
                            FOREIGN KEY(ideaId) REFERENCES ideas(id) ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_idea_followups_ideaId ON idea_followups(ideaId)")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_idea_followups_ideaId_createdAtMs " +
                            "ON idea_followups(ideaId, createdAtMs)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS task_suggestions (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            originId TEXT,
                            originType TEXT,
                            rootId TEXT
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_task_suggestions_createdAt ON task_suggestions(createdAt)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_task_suggestions_originId ON task_suggestions(originId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_task_suggestions_rootId ON task_suggestions(rootId)")
                }
            }

        /**
         * Migration 32 -> 33 (ID-Architektur Etappe 3, Frank-Wunsch 2026-06-19): Kern-Kette
         * Gewohnheiten. Neue Tabellen `habits` (Gewohnheit-Reiter, mit manueller `position`) und
         * `habit_suggestions` (Gewohnheits-Vorschlaege) — analog ideas/task_suggestions aus 31->32.
         * KEINE SQL-DEFAULT-Klausel fuer Felder mit Kotlin-Default (updatedAt/position) — das
         * Room-Schema erwartet NOT NULL ohne DEFAULT (sonst Integrity-Mismatch -> destructive fallback).
         */
        val MIGRATION_32_33: Migration =
            object : Migration(32, 33) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS habits (
                            id TEXT NOT NULL PRIMARY KEY,
                            text TEXT NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            position INTEGER NOT NULL,
                            originId TEXT,
                            originType TEXT,
                            rootId TEXT
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_position ON habits(position)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_originId ON habits(originId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_rootId ON habits(rootId)")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS habit_suggestions (
                            id TEXT NOT NULL PRIMARY KEY,
                            text TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            originId TEXT,
                            originType TEXT,
                            rootId TEXT
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_suggestions_createdAt ON habit_suggestions(createdAt)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_suggestions_originId ON habit_suggestions(originId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_suggestions_rootId ON habit_suggestions(rootId)")
                }
            }

        /**
         * Migration 33 -> 34 (ID-Architektur Etappe 4, Frank-Wunsch 2026-06-19): Mental-Board-Saetze.
         * Neue Tabelle `mental_sentences` (mit manueller `position`) — analog habits aus 32->33.
         * KEINE SQL-DEFAULT-Klausel fuer Felder mit Kotlin-Default (updatedAt/position).
         */
        val MIGRATION_33_34: Migration =
            object : Migration(33, 34) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS mental_sentences (
                            id TEXT NOT NULL PRIMARY KEY,
                            text TEXT NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            position INTEGER NOT NULL,
                            originId TEXT,
                            originType TEXT,
                            rootId TEXT
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_mental_sentences_position ON mental_sentences(position)")
                }
            }

        /**
         * Migration 34 -> 35 (Frank-Wunsch 2026-06-20): Modifikations-Zeitstempel `updatedAt` fuer
         * die `ideas`-Tabelle (geraeteuebergreifende Edit-Sync per Last-Write-Wins). ADDITIV +
         * NULLABLE (kein NOT NULL / kein SQL-DEFAULT) -> Bestandszeilen bekommen NULL, kein
         * identityHash-Mismatch (M3), kein Datenverlust. Im Sync zaehlt `updatedAt ?: timestampMs`.
         */
        val MIGRATION_34_35: Migration =
            object : Migration(34, 35) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE ideas ADD COLUMN updatedAt INTEGER")
                }
            }

        /**
         * Migration 35 -> 36 (Wetter pro Training, Open-Meteo): drei ADDITIVE, NULLABLE Spalten
         * an `amazfit_workouts` — Lufttemperatur (°C), Wetterlage (Wort) und Abruf-Zeitstempel.
         * Kein NOT NULL / kein SQL-DEFAULT -> Bestandszeilen bekommen NULL, kein identityHash-
         * Mismatch (M3), kein Datenverlust. Befuellt wird spaeter per Open-Meteo-Backfill.
         */
        val MIGRATION_35_36: Migration =
            object : Migration(35, 36) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN weatherTempCelsius INTEGER")
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN weatherCondition TEXT")
                    db.execSQL("ALTER TABLE amazfit_workouts ADD COLUMN weatherFetchedMs INTEGER")
                }
            }
    }
}
