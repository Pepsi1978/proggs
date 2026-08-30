package de.frank.genialeideen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DatenWandler {
    @TypeConverter fun kategorieartHin(art: Kategorieart): String = art.name

    @TypeConverter fun kategorieartHer(wert: String): Kategorieart =
        runCatching { Kategorieart.valueOf(wert) }.getOrDefault(Kategorieart.MENTAL)
}

@Database(
    entities = [
        IdeeEntity::class,
        IdeeFts::class,
        NachrichtEntity::class,
        SuchanfrageEntity::class,
        KategorieEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(DatenWandler::class)
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

        /** Bestehende Kategorien bleiben erhalten und werden als mental eingeordnet. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kategorien ADD COLUMN art TEXT NOT NULL DEFAULT 'MENTAL'")
                db.execSQL("DROP INDEX IF EXISTS index_kategorien_name")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_kategorien_name_art " +
                        "ON kategorien (name, art)",
                )
            }
        }

        fun getInstance(context: Context): GenialeIdeenDatabase =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    GenialeIdeenDatabase::class.java,
                    "geniale_ideen.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instanz = it }
            }
    }
}
