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
     * Haupt-Datenbank (Aufgaben, Biomarker, Kalender, Memory, Insights etc.).
     * Stage 1: destructive fallback ist explizit gewollt — Schema ist noch in Bewegung,
     * Datenverlust hier ist akzeptabel weil die meisten Tabellen wieder befuellt werden
     * (Whoop-Sync, Calendar-Sync, KI-Vorschlaege).
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DB_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    /**
     * Forscher-Datenbank (Frank-Wunsch 2026-05-09): SEPARATE DB-Datei fuer
     * scientist_sessions, scientist_messages, hypotheses, hypothesis_messages.
     * Frank's eigentliches Tagebuch — diese Daten muessen ueber App-Updates und
     * Schema-Aenderungen der Haupt-DB persistent bleiben.
     *
     * KEIN destructive fallback hier: Wenn das Schema sich aendert, MUSS eine echte
     * Migration geschrieben werden, sonst crasht die App beim Start. Das ist Poka-Yoke
     * Stufe 3 (Eliminierung): Datenverlust durch versehentlichen Reset ist
     * konzeptionell unmoeglich gemacht.
     *
     * Bei zukuenftigem Schema-Bump:
     *   .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)
     * hinzufuegen, jede Migration als val MIGRATION_X_Y = object : Migration(X, Y).
     */
    @Provides
    @Singleton
    fun provideScientistDatabase(@ApplicationContext ctx: Context): ScientistDatabase =
        Room.databaseBuilder(ctx, ScientistDatabase::class.java, ScientistDatabase.DB_NAME)
            .build()

    @Provides fun provideEntropyEntryDao(db: AppDatabase) = db.entropyEntryDao()
    @Provides fun provideSavedPromptDao(db: AppDatabase) = db.savedPromptDao()
    @Provides fun provideMemoryDao(db: AppDatabase) = db.memoryDao()
    @Provides fun provideInsightDao(db: AppDatabase) = db.insightDao()
    @Provides fun provideBiomarkerSnapshotDao(db: AppDatabase) = db.biomarkerSnapshotDao()
    @Provides fun provideSupplementLogDao(db: AppDatabase) = db.supplementLogDao()
    @Provides fun provideCalendarDayDao(db: AppDatabase) = db.calendarDayDao()
    @Provides fun provideCalendarEventDao(db: AppDatabase) = db.calendarEventDao()
    @Provides fun provideKiTriggerDao(db: AppDatabase) = db.kiTriggerDao()
    @Provides fun provideGenieCodexDao(db: AppDatabase) = db.genieCodexDao()
    @Provides fun provideWhoopWorkoutDao(db: AppDatabase) = db.whoopWorkoutDao()

    // Forscher-DAOs jetzt aus der ScientistDatabase — eigene Persistenz-Domaene.
    @Provides fun provideScientistSessionDao(db: ScientistDatabase) = db.scientistSessionDao()
    @Provides fun provideScientistMessageDao(db: ScientistDatabase) = db.scientistMessageDao()
    @Provides fun provideHypothesisDao(db: ScientistDatabase) = db.hypothesisDao()
    @Provides fun provideHypothesisMessageDao(db: ScientistDatabase) = db.hypothesisMessageDao()
}
