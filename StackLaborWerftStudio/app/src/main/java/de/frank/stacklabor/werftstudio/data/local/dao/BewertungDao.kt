package de.frank.stacklabor.werftstudio.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungszelleEntity
import de.frank.stacklabor.werftstudio.data.local.entity.FrageAntwortEntity
import de.frank.stacklabor.werftstudio.data.local.entity.KonkurrenzEntity
import de.frank.stacklabor.werftstudio.data.local.relation.BewertungMitDetailsEntity
import de.frank.stacklabor.werftstudio.domain.model.BewertungsBereich
import kotlinx.coroutines.flow.Flow

@Dao
interface BewertungDao {
    @Transaction
    @Query("SELECT * FROM bewertung WHERE bereich = :bereich AND stackId IS :stackId ORDER BY zeitpunktEpochMillis DESC, id DESC LIMIT 1")
    fun beobachteNeueste(bereich: BewertungsBereich, stackId: String?): Flow<BewertungMitDetailsEntity?>

    @Transaction
    @Query("SELECT * FROM bewertung WHERE bereich = :bereich AND stackId IS :stackId ORDER BY zeitpunktEpochMillis DESC, id DESC LIMIT 1")
    suspend fun holeNeueste(bereich: BewertungsBereich, stackId: String?): BewertungMitDetailsEntity?

    @Transaction
    @Query("SELECT * FROM bewertung WHERE bereich = :bereich AND stackId IS :stackId ORDER BY zeitpunktEpochMillis DESC, id DESC LIMIT :limit")
    fun beobachteHistorie(
        bereich: BewertungsBereich,
        stackId: String?,
        limit: Int,
    ): Flow<List<BewertungMitDetailsEntity>>

    @Query("SELECT * FROM bewertung ORDER BY zeitpunktEpochMillis, id")
    suspend fun holeAlleBewertungen(): List<BewertungEntity>

    @Query("SELECT * FROM bewertungszelle ORDER BY bewertungId, mittelId, zielId")
    suspend fun holeAlleZellen(): List<BewertungszelleEntity>

    @Query("SELECT * FROM konkurrenz ORDER BY bewertungId, mittelAId, mittelBId, art")
    suspend fun holeAlleKonkurrenzen(): List<KonkurrenzEntity>

    @Query("SELECT * FROM frage_antwort ORDER BY bewertungId, frageId")
    suspend fun holeAlleAntworten(): List<FrageAntwortEntity>

    @Upsert suspend fun speichereBewertung(bewertung: BewertungEntity)
    @Upsert suspend fun speichereBewertungen(bewertungen: List<BewertungEntity>)
    @Upsert suspend fun speichereZellen(zellen: List<BewertungszelleEntity>)
    @Upsert suspend fun speichereKonkurrenzen(konkurrenzen: List<KonkurrenzEntity>)
    @Upsert suspend fun speichereAntworten(antworten: List<FrageAntwortEntity>)

    @Query("DELETE FROM bewertungszelle WHERE bewertungId = :bewertungId")
    suspend fun loescheZellen(bewertungId: String)

    @Query("DELETE FROM konkurrenz WHERE bewertungId = :bewertungId")
    suspend fun loescheKonkurrenzen(bewertungId: String)

    @Query("DELETE FROM frage_antwort WHERE bewertungId = :bewertungId")
    suspend fun loescheAntworten(bewertungId: String)

    @Query(
        """
        DELETE FROM bewertung
        WHERE bereich = :bereich
          AND stackId IS :stackId
          AND id NOT IN (
              SELECT id FROM bewertung
              WHERE bereich = :bereich AND stackId IS :stackId
              ORDER BY zeitpunktEpochMillis DESC, id DESC
              LIMIT :limit
          )
        """,
    )
    suspend fun begrenzeHistorie(bereich: BewertungsBereich, stackId: String?, limit: Int)

    @Query(
        """
        DELETE FROM bewertungszelle
        WHERE mittelId = :mittelId
          AND bewertungId IN (SELECT id FROM bewertung WHERE stackId = :stackId)
        """,
    )
    suspend fun loescheZellenFuerStackMittel(stackId: String, mittelId: String)
}
