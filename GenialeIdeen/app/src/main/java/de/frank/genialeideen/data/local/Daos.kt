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

    @Query("UPDATE ideen SET kategorieId = :kategorieId, geaendertAm = :jetzt WHERE id = :id")
    suspend fun setzeKategorie(id: Long, kategorieId: Long?, jetzt: Long = System.currentTimeMillis())

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

    @Query("SELECT * FROM nachrichten WHERE ideeId = :ideeId ORDER BY zeitpunkt ASC, id ASC")
    suspend fun fuerIdeeEinmal(ideeId: Long): List<NachrichtEntity>

    @Query("DELETE FROM nachrichten WHERE id = :id")
    suspend fun loesche(id: Long)

    @Query("DELETE FROM nachrichten WHERE id IN (:ids)")
    suspend fun loescheMehrere(ids: List<Long>)

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

@Dao
interface KategorienDao {
    @Query("SELECT * FROM kategorien ORDER BY art ASC, reihenfolge ASC, name ASC")
    fun alle(): Flow<List<KategorieEntity>>

    @Query("SELECT * FROM kategorien ORDER BY art ASC, reihenfolge ASC, name ASC")
    suspend fun alleEinmal(): List<KategorieEntity>

    @Query("SELECT * FROM kategorien WHERE name = :name COLLATE NOCASE AND art = :art LIMIT 1")
    suspend fun nachNameUndArt(name: String, art: Kategorieart): KategorieEntity?

    @Query("SELECT * FROM kategorien WHERE id = :id")
    suspend fun nachId(id: Long): KategorieEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun einfuegen(kategorie: KategorieEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun einfuegenAlle(kategorien: List<KategorieEntity>)

    @Query("SELECT COUNT(*) FROM kategorien WHERE art = :art")
    suspend fun anzahl(art: Kategorieart): Int

    @Query("UPDATE kategorien SET name = :name WHERE id = :id")
    suspend fun benenneUm(id: Long, name: String)

    @Query("UPDATE ideen SET kategorieId = NULL, geaendertAm = :jetzt WHERE kategorieId = :id")
    suspend fun loeseIdeen(id: Long, jetzt: Long = System.currentTimeMillis())

    @Query("DELETE FROM kategorien WHERE id = :id")
    suspend fun loesche(id: Long)

    /** Ideen und Kategorie werden in einem Schritt getrennt; die Ideen selbst bleiben erhalten. */
    @Transaction
    suspend fun loescheMitZuordnungen(id: Long) {
        loeseIdeen(id)
        loesche(id)
    }
}
