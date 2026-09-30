package de.frank.longevity.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FaktorDao {
    @Query("SELECT * FROM faktoren ORDER BY rang ASC, id ASC")
    fun alle(): Flow<List<Faktor>>

    @Query("SELECT * FROM faktoren ORDER BY rang ASC, id ASC")
    suspend fun alleJetzt(): List<Faktor>

    @Query("SELECT * FROM faktoren WHERE id = :id")
    suspend fun einer(id: Long): Faktor?

    @Query("SELECT COUNT(*) FROM faktoren")
    suspend fun anzahl(): Int

    @Insert
    suspend fun neu(f: Faktor): Long

    @Insert
    suspend fun neu(f: List<Faktor>)

    @Update
    suspend fun speichere(f: Faktor)

    @Update
    suspend fun speichere(f: List<Faktor>)

    @Delete
    suspend fun loesche(f: Faktor)

    @Transaction
    suspend fun ersetzeAlle(liste: List<Faktor>) { speichere(liste) }
}

/** v2: Ziel als erreicht markierbar. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE faktoren ADD COLUMN zielErreicht INTEGER NOT NULL DEFAULT 0")
    }
}

/** v3: Quellen, Stand-Datum und Hinweise der Gutachterin (alle nullable, damit der Altbestand unverändert bleibt). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE faktoren ADD COLUMN quellenJson TEXT")
        db.execSQL("ALTER TABLE faktoren ADD COLUMN standVom INTEGER")
        db.execSQL("ALTER TABLE faktoren ADD COLUMN hinweis TEXT")
        db.execSQL("ALTER TABLE faktoren ADD COLUMN zusammenMit INTEGER")
    }
}

@Database(entities = [Faktor::class], version = 3, exportSchema = true)
abstract class Datenbank : RoomDatabase() {
    abstract fun faktoren(): FaktorDao

    companion object {
        @Volatile private var instanz: Datenbank? = null
        fun get(context: Context): Datenbank = instanz ?: synchronized(this) {
            instanz ?: Room.databaseBuilder(context.applicationContext, Datenbank::class.java, "longevity.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instanz = it }
        }
    }
}
