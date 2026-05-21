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
        ],
    version = 22,
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
    }
}
