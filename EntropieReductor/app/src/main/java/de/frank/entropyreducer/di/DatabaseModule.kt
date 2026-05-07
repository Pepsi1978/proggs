package de.frank.entropyreducer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.frank.entropyreducer.data.local.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DB_NAME)
            // Stufe 1: keine Migrationen — Schema noch in Entwicklung.
            // Bei Schema-Aenderungen waehrend Stufe 1 wird die DB zurueckgesetzt.
            // dropAllTables = true ist explizit gewollt waehrend MVP.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideEntropyEntryDao(db: AppDatabase) = db.entropyEntryDao()
    @Provides fun provideSavedPromptDao(db: AppDatabase) = db.savedPromptDao()
    @Provides fun provideMemoryDao(db: AppDatabase) = db.memoryDao()
    @Provides fun provideScientistSessionDao(db: AppDatabase) = db.scientistSessionDao()
    @Provides fun provideScientistMessageDao(db: AppDatabase) = db.scientistMessageDao()
    @Provides fun provideHypothesisDao(db: AppDatabase) = db.hypothesisDao()
    @Provides fun provideInsightDao(db: AppDatabase) = db.insightDao()
    @Provides fun provideBiomarkerSnapshotDao(db: AppDatabase) = db.biomarkerSnapshotDao()
    @Provides fun provideSupplementLogDao(db: AppDatabase) = db.supplementLogDao()
    @Provides fun provideCalendarDayDao(db: AppDatabase) = db.calendarDayDao()
    @Provides fun provideCalendarEventDao(db: AppDatabase) = db.calendarEventDao()
    @Provides fun provideKiTriggerDao(db: AppDatabase) = db.kiTriggerDao()
    @Provides fun provideGenieCodexDao(db: AppDatabase) = db.genieCodexDao()
}
