package de.frank.entropyreducer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.ScientistDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Haupt-Datenbank (Aufgaben, Biomarker, Kalender, Memory, Insights etc.). Stage 1: destructive
     * fallback ist explizit gewollt — Schema ist noch in Bewegung, Datenverlust hier ist akzeptabel
     * weil die meisten Tabellen wieder befuellt werden (Whoop-Sync, Calendar-Sync, KI-Vorschlaege).
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
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
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

    // Frank-Wunsch 2026-05-09 (Abend): Insights und Memories leben jetzt in
    // ScientistDatabase — schema-stabil und ins Drive-Backup mitgesichert.
    @Provides fun provideMemoryDao(db: ScientistDatabase) = db.memoryDao()

    @Provides fun provideInsightDao(db: ScientistDatabase) = db.insightDao()

    // Forscher-DAOs jetzt aus der ScientistDatabase — eigene Persistenz-Domaene.
    @Provides fun provideScientistSessionDao(db: ScientistDatabase) = db.scientistSessionDao()

    @Provides fun provideScientistMessageDao(db: ScientistDatabase) = db.scientistMessageDao()

    @Provides fun provideHypothesisDao(db: ScientistDatabase) = db.hypothesisDao()

    @Provides fun provideHypothesisMessageDao(db: ScientistDatabase) = db.hypothesisMessageDao()
}
