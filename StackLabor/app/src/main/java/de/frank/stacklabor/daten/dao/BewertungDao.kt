package de.frank.stacklabor.daten.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.frank.stacklabor.daten.entitaeten.BewertungE
import de.frank.stacklabor.daten.entitaeten.BewertungMitInhalt
import de.frank.stacklabor.daten.entitaeten.BewertungszelleE
import de.frank.stacklabor.daten.entitaeten.FrageAntwortE
import de.frank.stacklabor.daten.entitaeten.KonkurrenzE
import kotlinx.coroutines.flow.Flow

/** Auswertungslaeufe (F-12, F-13), Ampel-Grundlage (F-14), Veraltet-Pruefung (F-23), Historie (F-29). */
@Dao
interface BewertungDao {

    // ── Juengster gueltiger Lauf ────────────────────────────────────────────────

    /** Die juengste Stack-Bewertung — Grundlage der Ampeln und der Veraltet-Pruefung (F-23). */
    @Query(
        """
        SELECT * FROM bewertung
        WHERE bereich = 'STACK' AND stackId = :stackId
        ORDER BY zeitpunkt DESC
        LIMIT 1
        """
    )
    fun juengsteBewertung(stackId: String): Flow<BewertungE?>

    @Query(
        """
        SELECT * FROM bewertung
        WHERE bereich = 'STACK' AND stackId = :stackId
        ORDER BY zeitpunkt DESC
        LIMIT 1
        """
    )
    suspend fun juengsteBewertungEinmalig(stackId: String): BewertungE?

    /** Derselbe Lauf mit Zellen, Konkurrenzen und Antworten (B-02, B-06, B-07). */
    @Transaction
    @Query(
        """
        SELECT * FROM bewertung
        WHERE bereich = 'STACK' AND stackId = :stackId
        ORDER BY zeitpunkt DESC
        LIMIT 1
        """
    )
    fun juengsteBewertungMitInhalt(stackId: String): Flow<BewertungMitInhalt?>

    /** Der juengste Tageslauf (F-13, B-09). */
    @Transaction
    @Query("SELECT * FROM bewertung WHERE bereich = 'TAG' ORDER BY zeitpunkt DESC LIMIT 1")
    fun juengsteTagesbewertungMitInhalt(): Flow<BewertungMitInhalt?>

    /** Je Stack der juengste Lauf — fuer die Sammelampeln auf B-01. */
    @Query(
        """
        SELECT b.* FROM bewertung b
        WHERE b.bereich = 'STACK' AND b.stackId IS NOT NULL
          AND b.zeitpunkt = (
              SELECT MAX(z.zeitpunkt) FROM bewertung z
              WHERE z.bereich = 'STACK' AND z.stackId = b.stackId
          )
        """
    )
    fun juengsteBewertungJeStack(): Flow<List<BewertungE>>

    /** Die Zellen genau dieser juengsten Laeufe — B-01 rechnet damit alle Sammelampeln (F-14). */
    @Query(
        """
        SELECT c.* FROM bewertungszelle c
        WHERE c.bewertungId IN (
            SELECT b.id FROM bewertung b
            WHERE b.bereich = 'STACK' AND b.stackId IS NOT NULL
              AND b.zeitpunkt = (
                  SELECT MAX(z.zeitpunkt) FROM bewertung z
                  WHERE z.bereich = 'STACK' AND z.stackId = b.stackId
              )
        )
        """
    )
    fun zellenDerJuengstenBewertungen(): Flow<List<BewertungszelleE>>

    // ── Aufschluesselung (F-15) ─────────────────────────────────────────────────

    @Query("SELECT * FROM bewertungszelle WHERE bewertungId = :bewertungId")
    fun zellen(bewertungId: String): Flow<List<BewertungszelleE>>

    /** Mittel → Ziele: alle Urteile zu einem Mittel in diesem Lauf. */
    @Query("SELECT * FROM bewertungszelle WHERE bewertungId = :bewertungId AND mittelId = :mittelId")
    fun zellenFuerMittel(bewertungId: String, mittelId: String): Flow<List<BewertungszelleE>>

    /** Ziel → Mittel: alle Urteile zu einem Ziel in diesem Lauf. */
    @Query("SELECT * FROM bewertungszelle WHERE bewertungId = :bewertungId AND zielId = :zielId")
    fun zellenFuerZiel(bewertungId: String, zielId: String): Flow<List<BewertungszelleE>>

    @Query("SELECT * FROM konkurrenz WHERE bewertungId = :bewertungId ORDER BY schwere DESC")
    fun konkurrenzen(bewertungId: String): Flow<List<KonkurrenzE>>

    @Query("SELECT * FROM frage_antwort WHERE bewertungId = :bewertungId")
    fun antworten(bewertungId: String): Flow<List<FrageAntwortE>>

    // ── Historie (F-29) ─────────────────────────────────────────────────────────

    /** Die letzten fuenf Laeufe dieses Stacks, juengster zuerst (B-15). */
    @Query(
        """
        SELECT * FROM bewertung
        WHERE bereich = 'STACK' AND stackId = :stackId
        ORDER BY zeitpunkt DESC
        LIMIT :anzahl
        """
    )
    fun historie(stackId: String, anzahl: Int): Flow<List<BewertungE>>

    /** Zwei Laeufe zum Vergleichen — mit Zellen, damit die Unterschiede je Ziel benennbar sind. */
    @Transaction
    @Query("SELECT * FROM bewertung WHERE id = :bewertungId")
    fun bewertungMitInhalt(bewertungId: String): Flow<BewertungMitInhalt?>

    @Transaction
    @Query("SELECT * FROM bewertung WHERE id = :bewertungId")
    suspend fun bewertungMitInhaltEinmalig(bewertungId: String): BewertungMitInhalt?

    // ── Schreiben ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenBewertung(bewertung: BewertungE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenZellen(zellen: List<BewertungszelleE>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenKonkurrenzen(konkurrenzen: List<KonkurrenzE>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenAntworten(antworten: List<FrageAntwortE>)

    /** Behaelt je Stack nur die juengsten Laeufe, der aelteste faellt heraus (F-29). */
    @Query(
        """
        DELETE FROM bewertung
        WHERE bereich = 'STACK' AND stackId = :stackId AND id NOT IN (
            SELECT id FROM bewertung
            WHERE bereich = 'STACK' AND stackId = :stackId
            ORDER BY zeitpunkt DESC
            LIMIT :behalten
        )
        """
    )
    suspend fun entferneAlteLaeufe(stackId: String, behalten: Int)

    /** Gleiches Fenster fuer die Tageslaeufe (F-13). */
    @Query(
        """
        DELETE FROM bewertung
        WHERE bereich = 'TAG' AND id NOT IN (
            SELECT id FROM bewertung WHERE bereich = 'TAG' ORDER BY zeitpunkt DESC LIMIT :behalten
        )
        """
    )
    suspend fun entferneAlteTageslaeufe(behalten: Int)

    /**
     * Speichert einen vollstaendigen Lauf in einem Zug (F-12 Schritt 6, F-13) und schneidet die
     * Historie auf fuenf Laeufe zurueck. In einer Transaktion, damit nie ein halber Lauf sichtbar
     * wird und die Ampeln zwischendurch nicht auf unvollstaendige Zellen rechnen.
     */
    @Transaction
    suspend fun speichereLauf(
        bewertung: BewertungE,
        zellen: List<BewertungszelleE>,
        konkurrenzen: List<KonkurrenzE>,
        antworten: List<FrageAntwortE>,
    ) {
        einfuegenBewertung(bewertung)
        if (zellen.isNotEmpty()) einfuegenZellen(zellen)
        if (konkurrenzen.isNotEmpty()) einfuegenKonkurrenzen(konkurrenzen)
        if (antworten.isNotEmpty()) einfuegenAntworten(antworten)
        val stackId = bewertung.stackId
        if (stackId != null) {
            entferneAlteLaeufe(stackId, HISTORIE_TIEFE)
        } else {
            entferneAlteTageslaeufe(HISTORIE_TIEFE)
        }
    }

    @Query("DELETE FROM bewertung WHERE id = :bewertungId")
    suspend fun loeschenBewertung(bewertungId: String)

    /**
     * Entfernt die Zellen eines Mittels in allen Laeufen dieses Stacks — wird beim Entfernen des
     * Mittels aus dem Stack gebraucht (F-04).
     */
    @Query(
        """
        DELETE FROM bewertungszelle
        WHERE mittelId = :mittelId AND bewertungId IN (
            SELECT id FROM bewertung WHERE stackId = :stackId
        )
        """
    )
    suspend fun loeschenZellenFuerMittelImStack(stackId: String, mittelId: String)

    companion object {
        /** Je Stack werden fuenf Laeufe aufbewahrt (F-29). */
        const val HISTORIE_TIEFE: Int = 5
    }
}
