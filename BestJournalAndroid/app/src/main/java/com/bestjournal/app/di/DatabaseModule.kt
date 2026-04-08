package com.bestjournal.app.di

import android.content.Context
import com.bestjournal.app.data.local.AppDatabase
import com.bestjournal.app.data.local.dao.AdviceDashboardDao
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.local.dao.RetrospectiveDao
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

    @Provides
    fun provideRetrospectiveDao(database: AppDatabase): RetrospectiveDao {
        return database.retrospectiveDao()
    }
}
