package com.nems.app.di

import android.content.Context
import androidx.room.Room
import com.nems.app.data.local.NmsDatabase
import com.nems.app.util.Constants
import com.nems.app.data.local.dao.StackSectionDao
import com.nems.app.data.local.dao.SupplementDao
import com.nems.app.data.local.dao.SupplementEntryDao
import com.nems.app.data.repository.SupplementRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NmsDatabase =
        Room.databaseBuilder(context, NmsDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSupplementDao(db: NmsDatabase): SupplementDao = db.supplementDao()

    @Provides
    fun provideStackSectionDao(db: NmsDatabase): StackSectionDao = db.stackSectionDao()

    @Provides
    fun provideSupplementEntryDao(db: NmsDatabase): SupplementEntryDao = db.supplementEntryDao()

    @Provides
    @Singleton
    fun provideSupplementRepository(
        supplementDao: SupplementDao,
        stackSectionDao: StackSectionDao,
        supplementEntryDao: SupplementEntryDao,
    ): SupplementRepository = SupplementRepository(supplementDao, stackSectionDao, supplementEntryDao)
}
