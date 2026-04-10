package com.entropyjournal.di

import android.content.Context
import com.entropyjournal.data.local.AppDatabase
import com.entropyjournal.data.local.RetrospectiveDatabase
import com.entropyjournal.data.local.dao.AdviceDashboardDao
import com.entropyjournal.data.local.dao.EntryPhotoDao
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.local.dao.RetrospectiveDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideRetrospectiveDatabase(@ApplicationContext context: Context): RetrospectiveDatabase {
        return RetrospectiveDatabase.getDatabase(context)
    }

    @Provides
    fun provideJournalEntryDao(database: AppDatabase): JournalEntryDao {
        return database.journalEntryDao()
    }

    @Provides
    fun provideAdviceDashboardDao(database: AppDatabase): AdviceDashboardDao {
        return database.adviceDashboardDao()
    }

    @Provides
    fun provideRetrospectiveDao(database: RetrospectiveDatabase): RetrospectiveDao {
        return database.retrospectiveDao()
    }

    @Provides
    fun provideEntryPhotoDao(database: AppDatabase): EntryPhotoDao {
        return database.entryPhotoDao()
    }
}
