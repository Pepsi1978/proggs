package de.frank.ziele.data

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZieleDao {
    @Query("SELECT * FROM ziele ORDER BY rang, erstellt")
    fun alleZiele(): Flow<List<Ziel>>

    @Query("SELECT * FROM ziele ORDER BY rang, erstellt")
    suspend fun alleZieleEinmal(): List<Ziel>

    @Query("SELECT * FROM ziele WHERE id = :id")
    suspend fun ziel(id: Long): Ziel?

    @Insert
    suspend fun neu(z: Ziel): Long

    @Update
    suspend fun speichere(z: Ziel)

    @Delete
    suspend fun loesche(z: Ziel)

    @Query("SELECT * FROM vorschlaege ORDER BY zielId, id")
    fun alleVorschlaege(): Flow<List<Vorschlag>>

    @Query("SELECT * FROM vorschlaege WHERE zielId = :zielId ORDER BY id")
    suspend fun vorschlaege(zielId: Long): List<Vorschlag>

    @Query("SELECT * FROM vorschlaege ORDER BY zielId, id")
    suspend fun alleVorschlaegeEinmal(): List<Vorschlag>

    @Query("SELECT * FROM vorschlaege WHERE id = :id")
    suspend fun vorschlag(id: Long): Vorschlag?

    @Insert
    suspend fun neueVorschlaege(v: List<Vorschlag>)

    @Insert
    suspend fun neuerVorschlag(v: Vorschlag): Long

    @Update
    suspend fun speichere(v: Vorschlag)

    @Query("DELETE FROM vorschlaege WHERE zielId = :zielId")
    suspend fun vorschlaegeLoeschen(zielId: Long)

    @Query("DELETE FROM vorschlaege WHERE zielId = :zielId AND aufgabeId IS NULL AND verworfen = 0")
    suspend fun offeneVorschlaegeLoeschen(zielId: Long)

    /** Neue Auswertung: übernommene und verworfene Vorschläge bleiben, die übrigen werden ersetzt. */
    @Transaction
    suspend fun vorschlaegeErsetzen(zielId: Long, neu: List<Vorschlag>) {
        offeneVorschlaegeLoeschen(zielId)
        neueVorschlaege(neu)
    }
}

@Database(entities = [Ziel::class, Vorschlag::class], version = 1, exportSchema = false)
abstract class ZieleDatenbank : RoomDatabase() {
    abstract fun dao(): ZieleDao
}

/** Einziger Zugang zu den Daten. Jede Änderung meldet Jarvis und dem Wecker ein Signal, damit sie frisch lesen. */
class ZieleRepository private constructor(private val app: Context) {
    private val dao = Room.databaseBuilder(app, ZieleDatenbank::class.java, "ziele.db").build().dao()

    val ziele: Flow<List<Ziel>> = dao.alleZiele()
    val vorschlaege: Flow<List<Vorschlag>> = dao.alleVorschlaege()

    suspend fun zieleEinmal() = dao.alleZieleEinmal()
    suspend fun ziel(id: Long) = dao.ziel(id)
    suspend fun vorschlaege(zielId: Long) = dao.vorschlaege(zielId)
    suspend fun alleVorschlaegeEinmal() = dao.alleVorschlaegeEinmal()
    suspend fun vorschlag(id: Long) = dao.vorschlag(id)

    suspend fun neu(z: Ziel): Long {
        val rang = (dao.alleZieleEinmal().minOfOrNull { it.rang } ?: 1) - 1
        return dao.neu(z.copy(rang = rang)).also { signal() }
    }

    suspend fun speichere(z: Ziel) { dao.speichere(z.copy(geaendert = System.currentTimeMillis())); signal() }

    suspend fun loesche(z: Ziel) { dao.vorschlaegeLoeschen(z.id); dao.loesche(z); signal() }

    suspend fun wiederherstellen(z: Ziel, v: List<Vorschlag>) { dao.neu(z); dao.neueVorschlaege(v); signal() }

    suspend fun speichere(v: Vorschlag) { dao.speichere(v); signal() }

    suspend fun neuerVorschlag(v: Vorschlag): Long = dao.neuerVorschlag(v).also { signal() }

    suspend fun vorschlaegeErsetzen(zielId: Long, neu: List<Vorschlag>) { dao.vorschlaegeErsetzen(zielId, neu); signal() }

    suspend fun reihenfolge(ids: List<Long>) {
        val alle = dao.alleZieleEinmal().associateBy { it.id }
        ids.forEachIndexed { i, id -> alle[id]?.let { if (it.rang != i) dao.speichere(it.copy(rang = i)) } }
        signal()
    }

    private fun signal() {
        runCatching { app.contentResolver.notifyChange(Uri.parse("content://${app.packageName}.jarvis"), null) }
        runCatching { app.contentResolver.notifyChange(Uri.parse("content://${app.packageName}.wecker"), null) }
    }

    companion object {
        @Volatile private var instanz: ZieleRepository? = null
        fun get(context: Context): ZieleRepository = instanz ?: synchronized(this) {
            instanz ?: ZieleRepository(context.applicationContext).also { instanz = it }
        }
    }
}
