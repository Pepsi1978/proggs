package de.frank.aufgaben.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AufgabenDao {
    @Query("SELECT * FROM aufgaben ORDER BY erstellt DESC")
    fun alle(): Flow<List<Aufgabe>>

    @Query("SELECT * FROM aufgaben")
    suspend fun alleEinmal(): List<Aufgabe>

    @Query("SELECT * FROM aufgaben WHERE id = :id")
    suspend fun eine(id: Long): Aufgabe?

    /** Ein Tag; mit [mitUeberfaelligen] = 1 zusätzlich alles Offene aus vergangenen Tagen. */
    @Query("SELECT * FROM aufgaben WHERE tag = :tag OR (:mitUeberfaelligen = 1 AND tag < :tag AND erledigt = 0)")
    suspend fun fuerTag(tag: Long, mitUeberfaelligen: Int): List<Aufgabe>

    @Insert
    suspend fun neu(aufgabe: Aufgabe): Long

    @Update
    suspend fun aendere(aufgabe: Aufgabe)

    @Delete
    suspend fun loesche(aufgabe: Aufgabe)
}

@Database(entities = [Aufgabe::class], version = 1, exportSchema = true)
abstract class AufgabenDatenbank : RoomDatabase() {
    abstract fun dao(): AufgabenDao

    companion object {
        @Volatile private var instanz: AufgabenDatenbank? = null

        fun get(context: Context): AufgabenDatenbank = instanz ?: synchronized(this) {
            instanz ?: Room.databaseBuilder(context.applicationContext, AufgabenDatenbank::class.java, "aufgaben.db")
                .build().also { instanz = it }
        }
    }
}
