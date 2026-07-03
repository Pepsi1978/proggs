package de.frank.entropyreducer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogDatabase
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.ScientistDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Haupt-Datenbank (Aufgaben, Biomarker, Kalender, Memory, Insights etc.).
     *
     * Migrations-Politik (verschaerft 2026-07-03, #47447 — Room-Almanach M1/M2):
     * Der fruehere pauschale destructive fallback war eine tickende Datenverlust-Bombe —
     * ein einziger Versions-Bump ohne Migration haette ALLE lokalen Daten still verworfen.
     * Jetzt gilt: destruktiv NUR noch fuer Uralt-Staende < 10 (fuer die nie Migrationen
     * geschrieben wurden); ab Version 10 crasht ein fehlender Migrationspfad LAUT beim
     * Start (Poka-Yoke wie bei der ScientistDatabase) — dann MUSS eine echte
     * Migration/@AutoMigration ergaenzt werden, nie der Fallback zurueckkommen.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DB_NAME)
            // Frank-Wunsch 2026-05-09: saubere Migration fuer Amazfit-Tabellen
            // (kein Datenverlust an bestehenden Whoop-Daten). destructiveFallback
            // bleibt als Sicherheitsnetz fuer alle anderen unerwarteten Schemas.
            .addMigrations(
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23,
                AppDatabase.MIGRATION_23_24,
                AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_25_26,
                AppDatabase.MIGRATION_26_27,
                AppDatabase.MIGRATION_27_28,
                AppDatabase.MIGRATION_28_29,
                AppDatabase.MIGRATION_29_30,
                AppDatabase.MIGRATION_30_31,
                AppDatabase.MIGRATION_31_32,
                AppDatabase.MIGRATION_32_33,
                AppDatabase.MIGRATION_33_34,
                AppDatabase.MIGRATION_34_35,
            )
            // Destruktiv NUR von Uralt-Versionen 1-9 (keine Migrationen vorhanden, kein
            // Geraet mehr auf diesem Stand). Ab Version 10: NIE destruktiv — fehlende
            // Migration = lauter Start-Crash statt stillem Verlust aller Daten.
            .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            .build()

    /**
     * Forscher-Datenbank (Frank-Wunsch 2026-05-09): SEPARATE DB-Datei fuer scientist_sessions,
     * scientist_messages, hypotheses, hypothesis_messages. Frank's eigentliches Tagebuch — diese
     * Daten muessen ueber App-Updates und Schema-Aenderungen der Haupt-DB persistent bleiben.
     *
     * KEIN destructive fallback hier: Wenn das Schema sich aendert, MUSS eine echte Migration
     * geschrieben werden, sonst crasht die App beim Start. Das ist Poka-Yoke Stufe 3
     * (Eliminierung): Datenverlust durch versehentlichen Reset ist konzeptionell unmoeglich
     * gemacht.
     *
     * Bei zukuenftigem Schema-Bump: .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...) hinzufuegen,
     * jede Migration als val MIGRATION_X_Y = object : Migration(X, Y).
     */
    @Provides
    @Singleton
    fun provideScientistDatabase(@ApplicationContext ctx: Context): ScientistDatabase =
        Room.databaseBuilder(ctx, ScientistDatabase::class.java, ScientistDatabase.DB_NAME)
            .addMigrations(ScientistDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideEntropyEntryDao(db: AppDatabase) = db.entropyEntryDao()

    @Provides fun provideSavedPromptDao(db: AppDatabase) = db.savedPromptDao()

    @Provides fun provideBiomarkerSnapshotDao(db: AppDatabase) = db.biomarkerSnapshotDao()

    @Provides fun provideSupplementLogDao(db: AppDatabase) = db.supplementLogDao()

    @Provides fun provideCalendarDayDao(db: AppDatabase) = db.calendarDayDao()

    @Provides fun provideCalendarEventDao(db: AppDatabase) = db.calendarEventDao()

    @Provides fun provideKiTriggerDao(db: AppDatabase) = db.kiTriggerDao()

    @Provides fun provideGenieCodexDao(db: AppDatabase) = db.genieCodexDao()

    @Provides fun provideWhoopWorkoutDao(db: AppDatabase) = db.whoopWorkoutDao()

    @Provides fun provideAmazfitDailyDao(db: AppDatabase) = db.amazfitDailyDao()

    @Provides fun provideAmazfitWorkoutDao(db: AppDatabase) = db.amazfitWorkoutDao()

    @Provides fun provideOuraReadinessDao(db: AppDatabase) = db.ouraReadinessDao()

    @Provides fun provideOuraDailySleepDao(db: AppDatabase) = db.ouraDailySleepDao()

    @Provides fun provideOuraActivityDao(db: AppDatabase) = db.ouraActivityDao()

    @Provides fun provideOuraResilienceDao(db: AppDatabase) = db.ouraResilienceDao()

    @Provides fun provideOuraSleepDetailDao(db: AppDatabase) = db.ouraSleepDetailDao()

    @Provides fun provideOuraPersonalInfoDao(db: AppDatabase) = db.ouraPersonalInfoDao()

    @Provides fun provideHealthConnectValueDao(db: AppDatabase) = db.healthConnectValueDao()

    @Provides fun provideEntropyEntryFollowupDao(db: AppDatabase) = db.entropyEntryFollowupDao()

    // Agentic-AI DAOs (Frank-Wunsch 2026-05-21: Prompts als Agenten)
    @Provides fun providePromptExecutionDao(db: AppDatabase) = db.promptExecutionDao()

    @Provides fun providePromptExecutionStepDao(db: AppDatabase) = db.promptExecutionStepDao()

    @Provides fun providePromptToolPermissionDao(db: AppDatabase) = db.promptToolPermissionDao()

    @Provides fun provideTokenUsageDailyDao(db: AppDatabase) = db.tokenUsageDailyDao()

    @Provides fun providePromptTriggerDao(db: AppDatabase) = db.promptTriggerDao()

    /** Wiederkehrende Aufgaben (Sprint 2, Frank-Wunsch 2026-05-22). */
    @Provides fun provideRecurringTemplateDao(db: AppDatabase) = db.recurringTemplateDao()

    /** Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19). */
    @Provides fun providePriorityMemoryDao(db: AppDatabase) = db.priorityMemoryDao()

    /** ID-Architektur Etappe 2 (Frank-Wunsch 2026-06-19): Ideen + Aufgaben-Vorschlaege. */
    @Provides fun provideIdeaDao(db: AppDatabase) = db.ideaDao()

    @Provides fun provideTaskSuggestionDao(db: AppDatabase) = db.taskSuggestionDao()

    /** ID-Architektur Etappe 3 (Frank-Wunsch 2026-06-19): Gewohnheiten + Gewohnheits-Vorschlaege. */
    @Provides fun provideHabitDao(db: AppDatabase) = db.habitDao()

    @Provides fun provideHabitSuggestionDao(db: AppDatabase) = db.habitSuggestionDao()

    /** ID-Architektur Etappe 4 (Frank-Wunsch 2026-06-19): Mental-Board-Saetze. */
    @Provides fun provideMentalSentenceDao(db: AppDatabase) = db.mentalSentenceDao()

    // Frank-Wunsch 2026-05-09 (Abend): Insights und Memories leben jetzt in
    // ScientistDatabase — schema-stabil und ins Drive-Backup mitgesichert.
    @Provides fun provideMemoryDao(db: ScientistDatabase) = db.memoryDao()

    @Provides fun provideInsightDao(db: ScientistDatabase) = db.insightDao()

    // Forscher-DAOs jetzt aus der ScientistDatabase — eigene Persistenz-Domaene.
    @Provides fun provideScientistSessionDao(db: ScientistDatabase) = db.scientistSessionDao()

    @Provides fun provideScientistMessageDao(db: ScientistDatabase) = db.scientistMessageDao()

    @Provides fun provideHypothesisDao(db: ScientistDatabase) = db.hypothesisDao()

    @Provides fun provideHypothesisMessageDao(db: ScientistDatabase) = db.hypothesisMessageDao()

    /**
     * Diagnose-Log-DB (Frank-Wunsch 2026-05-23): EIGENE DB-Datei nur fuer das interne
     * Fehler-/Erfolgs-Logging der API-Bereiche. destructiveFallback ist hier voellig
     * unkritisch — Logs sind Wegwerf-Daten, ein Schema-Reset darf sie loeschen.
     */
    @Provides
    @Singleton
    fun provideDiagnosticLogDatabase(@ApplicationContext ctx: Context): DiagnosticLogDatabase =
        Room.databaseBuilder(ctx, DiagnosticLogDatabase::class.java, DiagnosticLogDatabase.DB_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideDiagnosticLogDao(db: DiagnosticLogDatabase) = db.diagnosticLogDao()

    /**
     * Spiegel-DB der BestJournal-Frank-Tagebucheintraege (Frank-Wunsch 2026-05-24).
     * Eigene DB-Datei, NICHT im Drive-Backup. destructiveFallback unkritisch (reine Kopie,
     * wird bei jedem App-Start neu synchronisiert).
     */
    @Provides
    @Singleton
    fun provideJournalMirrorDatabase(
        @ApplicationContext ctx: Context,
    ): de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase =
        Room.databaseBuilder(
                ctx,
                de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase::class.java,
                de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase.DB_NAME,
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideJournalMirrorDao(
        db: de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase,
    ) = db.journalMirrorDao()
}
