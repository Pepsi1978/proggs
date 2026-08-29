package de.frank.genialeideen.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeenDao {
    @Query("SELECT * FROM ideen ORDER BY reihenfolge ASC, angelegtAm DESC")
    fun alle(): Flow<List<IdeeEntity>>

    @Query("SELECT * FROM ideen WHERE id = :id")
    fun beobachte(id: Long): Flow<IdeeEntity?>

    @Query("SELECT * FROM ideen ORDER BY reihenfolge ASC, angelegtAm DESC")
    suspend fun alleEinmal(): List<IdeeEntity>

    @Query("SELECT * FROM ideen WHERE id = :id")
    suspend fun lade(id: Long): IdeeEntity?

    @Query("SELECT COALESCE(MIN(reihenfolge), 0) - 1 FROM ideen WHERE status = :status")
    suspend fun naechsteReihenfolgeOben(status: String): Int

    @Insert
    suspend fun einfuegen(idee: IdeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenAlle(ideen: List<IdeeEntity>)

    @Update
    suspend fun aktualisieren(idee: IdeeEntity)

    @Delete
    suspend fun loeschen(idee: IdeeEntity)

    @Query("DELETE FROM ideen")
    suspend fun alleLoeschen()

    @Query("UPDATE ideen SET reihenfolge = :reihenfolge, geaendertAm = :jetzt WHERE id = :id")
    suspend fun setzeReihenfolge(id: Long, reihenfolge: Int, jetzt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun schreibeReihenfolge(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setzeReihenfolge(id, index) }
    }

    @Query(
        """
        SELECT ideen.* FROM ideen
        JOIN ideen_fts ON ideen.rowid = ideen_fts.rowid
        WHERE ideen_fts MATCH :abfrage
        ORDER BY ideen.geaendertAm DESC
        """,
    )
    suspend fun suche(abfrage: String): List<IdeeEntity>
}

@Dao
interface NachrichtenDao {
    @Query("SELECT * FROM nachrichten WHERE ideeId = :ideeId ORDER BY zeitpunkt ASC, id ASC")
    fun fuerIdee(ideeId: Long): Flow<List<NachrichtEntity>>

    @Query("SELECT * FROM nachrichten ORDER BY zeitpunkt ASC")
    suspend fun alle(): List<NachrichtEntity>

    @Insert
    suspend fun einfuegen(nachricht: NachrichtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenAlle(nachrichten: List<NachrichtEntity>)

    @Update
    suspend fun aktualisieren(nachricht: NachrichtEntity)

    @Query("DELETE FROM nachrichten WHERE ideeId = :ideeId")
    suspend fun loescheFuerIdee(ideeId: Long)

    @Query("DELETE FROM nachrichten")
    suspend fun alleLoeschen()
}

@Dao
interface SuchverlaufDao {
    @Query("SELECT * FROM suchverlauf ORDER BY zeitpunkt DESC LIMIT 10")
    fun letzte(): Flow<List<SuchanfrageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun merken(anfrage: SuchanfrageEntity)

    @Query("DELETE FROM suchverlauf")
    suspend fun leeren()
}
