package de.frank.epigenetik.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Ein Mittel, das auf einen Schalter wirkt – für Atlas und Rückwärtssuche. */
data class SchalterTreffer(
    val rechercheId: Long,
    val name: String,
    val kategorie: String,
    val potenzial: Int,
    val imStack: Boolean,
    val schalter: String,
    val richtung: String,
    val staerke: Int,
    val evidenz: String,
    val mechanismus: String,
)

@Dao
interface RechercheDao {
    @Query("SELECT * FROM recherche ORDER BY aktualisiert DESC")
    fun alle(): Flow<List<Recherche>>

    @Query("SELECT * FROM recherche ORDER BY aktualisiert DESC")
    suspend fun alleJetzt(): List<Recherche>

    @Query("SELECT * FROM recherche WHERE id = :id")
    fun beobachte(id: Long): Flow<Recherche?>

    @Query("SELECT * FROM recherche WHERE id = :id")
    suspend fun hole(id: Long): Recherche?

    @Query("SELECT * FROM recherche WHERE typ = :typ AND schluessel = :schluessel LIMIT 1")
    suspend fun finde(typ: String, schluessel: String): Recherche?

    @Query("SELECT * FROM recherche WHERE status IN ('WARTET','LAEUFT')")
    suspend fun offene(): List<Recherche>

    @Insert
    suspend fun einfuegen(r: Recherche): Long

    @Update
    suspend fun aendern(r: Recherche)

    @Query("DELETE FROM recherche WHERE id = :id")
    suspend fun loeschen(id: Long)

    @Query("UPDATE recherche SET status = :status, fehler = :fehler, aktualisiert = :zeit WHERE id = :id")
    suspend fun setzeStatus(id: Long, status: String, fehler: String = "", zeit: Long = System.currentTimeMillis())

    @Query("UPDATE recherche SET gelesen = 1 WHERE id = :id")
    suspend fun gelesen(id: Long)

    @Query("SELECT * FROM schalter_wirkung")
    fun alleWirkungen(): Flow<List<SchalterWirkung>>

    @Query("SELECT * FROM schalter_wirkung WHERE rechercheId = :id ORDER BY staerke DESC")
    suspend fun wirkungenVon(id: Long): List<SchalterWirkung>

    @Query(
        """SELECT w.rechercheId, r.name, r.kategorie, r.potenzial, r.imStack, w.schalter, w.richtung, w.staerke, w.evidenz, w.mechanismus
           FROM schalter_wirkung w JOIN recherche r ON r.id = w.rechercheId
           WHERE w.schalter = :schalter AND r.status = 'FERTIG' ORDER BY w.staerke DESC""",
    )
    suspend fun mittelFuer(schalter: String): List<SchalterTreffer>

    @Query("DELETE FROM schalter_wirkung WHERE rechercheId = :id")
    suspend fun wirkungenLoeschen(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun wirkungenEinfuegen(w: List<SchalterWirkung>)

    @Transaction
    suspend fun ergebnisSpeichern(r: Recherche, wirkungen: List<SchalterWirkung>) {
        aendern(r)
        wirkungenLoeschen(r.id)
        if (wirkungen.isNotEmpty()) wirkungenEinfuegen(wirkungen)
    }

    @Transaction
    suspend fun allesLoeschen(id: Long) {
        wirkungenLoeschen(id)
        loeschen(id)
    }
}

@Database(entities = [Recherche::class, SchalterWirkung::class], version = 1, exportSchema = true)
abstract class Datenbank : RoomDatabase() {
    abstract fun dao(): RechercheDao

    companion object {
        @Volatile private var instanz: Datenbank? = null
        fun get(context: Context): Datenbank = instanz ?: synchronized(this) {
            instanz ?: Room.databaseBuilder(context.applicationContext, Datenbank::class.java, "epigenetik.db")
                .build().also { instanz = it }
        }
    }
}
