package de.frank.genialeideen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IdeeEntity::class, IdeeFts::class, NachrichtEntity::class, SuchanfrageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class GenialeIdeenDatabase : RoomDatabase() {
    abstract fun ideenDao(): IdeenDao
    abstract fun nachrichtenDao(): NachrichtenDao
    abstract fun suchverlaufDao(): SuchverlaufDao

    companion object {
        @Volatile private var instanz: GenialeIdeenDatabase? = null

        fun getInstance(context: Context): GenialeIdeenDatabase =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    GenialeIdeenDatabase::class.java,
                    "geniale_ideen.db",
                ).build().also { instanz = it }
            }
    }
}
