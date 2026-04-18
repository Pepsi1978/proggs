package com.bestjournal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bestjournal.app.data.local.dao.RetrospectiveDao
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity

@Database(entities = [RetrospectiveSummaryEntity::class], version = 2, exportSchema = true)
abstract class RetrospectiveDatabase : RoomDatabase() {

    abstract fun retrospectiveDao(): RetrospectiveDao

    companion object {
        @Volatile private var INSTANCE: RetrospectiveDatabase? = null

        fun getDatabase(context: Context): RetrospectiveDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                                context.applicationContext,
                                RetrospectiveDatabase::class.java,
                                "retrospective_db",
                            )
                            // Retrospective data is regenerable from journal entries — destructive
                            // migration is safe
                            .fallbackToDestructiveMigration()
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
