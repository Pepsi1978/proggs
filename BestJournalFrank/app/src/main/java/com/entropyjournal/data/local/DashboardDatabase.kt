package com.entropyjournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.entropyjournal.data.local.dao.AdviceDashboardDao
import com.entropyjournal.data.local.entity.AdviceBlockEntity

@Database(entities = [AdviceBlockEntity::class], version = 1, exportSchema = true)
abstract class DashboardDatabase : RoomDatabase() {

    abstract fun adviceDashboardDao(): AdviceDashboardDao

    companion object {
        @Volatile private var INSTANCE: DashboardDatabase? = null

        fun getDatabase(context: Context): DashboardDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                                context.applicationContext,
                                DashboardDatabase::class.java,
                                "dashboard_db",
                            )
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
