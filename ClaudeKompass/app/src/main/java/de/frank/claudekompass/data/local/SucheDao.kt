package de.frank.claudekompass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Ein Treffer, so wie ihn die Ergebnisliste braucht. */
data class SuchTreffer(
    val quelleId: String,
    val quelleArt: String,
    val bereich: String,
    val titel: String,
)

@Dao
interface SucheDao {

    /**
     * Sucht in der normalisierten Spalte.
     *
     * Die Anfrage wird vom Aufrufer normalisiert und um ein Sternchen ergänzt, damit auch
     * Wortanfänge treffen (`komp*` findet `kompaktieren`). FTS4 kennt kein `LIKE '%…%'` —
     * eine Suche mitten im Wort ist damit bewusst nicht möglich, dafür bleibt sie auch bei
     * vielen tausend Einträgen schnell.
     */
    @Query(
        """
        SELECT quelleId, quelleArt, bereich, titel
        FROM suche_fts
        WHERE suchtext MATCH :anfrage
        LIMIT :grenze
        """,
    )
    suspend fun suche(anfrage: String, grenze: Int = 120): List<SuchTreffer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun indiziere(eintraege: List<SucheFtsEntity>)

    @Query("DELETE FROM suche_fts WHERE quelleArt = :art")
    suspend fun leereArt(art: String)

    @Query("DELETE FROM suche_fts WHERE quelleId = :quelleId AND quelleArt = :art")
    suspend fun entferne(quelleId: String, art: String)

    @Query("SELECT COUNT(*) FROM suche_fts")
    suspend fun anzahl(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun merkeAnfrage(eintrag: SuchVerlaufEntity)

    @Query("SELECT * FROM such_verlauf ORDER BY zuletztAm DESC LIMIT 10")
    fun beobachteVerlauf(): Flow<List<SuchVerlaufEntity>>

    @Query("DELETE FROM such_verlauf")
    suspend fun leereVerlauf()

    @Query("DELETE FROM such_verlauf WHERE anfrage = :anfrage")
    suspend fun loescheAnfrage(anfrage: String)

    /** Hält den Verlauf bei zehn Einträgen; ältere fallen hinten heraus. */
    @Query(
        """
        DELETE FROM such_verlauf WHERE anfrage NOT IN (
            SELECT anfrage FROM such_verlauf ORDER BY zuletztAm DESC LIMIT 10
        )
        """,
    )
    suspend fun kuerzeVerlauf()
}
