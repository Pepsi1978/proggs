package com.entropyjournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.entropyjournal.data.local.dao.RetrospectiveDao
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity

@Database(entities = [RetrospectiveSummaryEntity::class], version = 1, exportSchema = true)
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
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
