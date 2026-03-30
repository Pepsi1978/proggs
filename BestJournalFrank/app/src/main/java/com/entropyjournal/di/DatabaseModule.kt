package com.entropyjournal.di

import android.content.Context
import com.entropyjournal.data.local.AppDatabase
import com.entropyjournal.data.local.dao.AdviceDashboardDao
import com.entropyjournal.data.local.dao.JournalEntryDao
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
    fun provideJournalEntryDao(database: AppDatabase): JournalEntryDao {
        return database.journalEntryDao()
    }

    @Provides
    fun provideAdviceDashboardDao(database: AppDatabase): AdviceDashboardDao {
        return database.adviceDashboardDao()
    }
}
