package de.frank.genialeideen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        IdeeEntity::class,
        IdeeFts::class,
        NachrichtEntity::class,
        SuchanfrageEntity::class,
        KategorieEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class GenialeIdeenDatabase : RoomDatabase() {
    abstract fun ideenDao(): IdeenDao
    abstract fun nachrichtenDao(): NachrichtenDao
    abstract fun suchverlaufDao(): SuchverlaufDao
    abstract fun kategorienDao(): KategorienDao

    companion object {
        @Volatile private var instanz: GenialeIdeenDatabase? = null

        /** Kategorien kommen dazu; die Ideen bleiben unangetastet und bekommen nur ein Fach. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ideen ADD COLUMN kategorieId INTEGER DEFAULT NULL")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS kategorien (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "reihenfolge INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_kategorien_name ON kategorien (name)",
                )
            }
        }

        fun getInstance(context: Context): GenialeIdeenDatabase =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    GenialeIdeenDatabase::class.java,
                    "geniale_ideen.db",
                ).addMigrations(MIGRATION_1_2).build().also { instanz = it }
            }
    }
}
