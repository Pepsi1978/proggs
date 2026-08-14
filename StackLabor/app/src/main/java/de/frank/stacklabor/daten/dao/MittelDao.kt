package de.frank.stacklabor.daten.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.frank.stacklabor.daten.entitaeten.MittelE
import de.frank.stacklabor.daten.entitaeten.MittelMitAnzahl
import de.frank.stacklabor.daten.entitaeten.StackE
import kotlinx.coroutines.flow.Flow

/** Der Katalog (F-30) — Stammdaten, Verwendungszaehlung und Zusammenfuehren. */
@Dao
interface MittelDao {

    // ── Lesen ───────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM mittel ORDER BY name COLLATE NOCASE ASC")
    fun alleMittel(): Flow<List<MittelE>>

    @Query("SELECT * FROM mittel WHERE id = :mittelId")
    fun mittel(mittelId: String): Flow<MittelE?>

    @Query("SELECT * FROM mittel WHERE id = :mittelId")
    suspend fun mittelEinmalig(mittelId: String): MittelE?

    /** Suche im Katalog (F-24), ohne Ruecksicht auf Gross- und Kleinschreibung. */
    @Query(
        """
        SELECT * FROM mittel
        WHERE :suchtext = '' OR name LIKE '%' || :suchtext || '%' COLLATE NOCASE
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun sucheMittel(suchtext: String): Flow<List<MittelE>>

    /** Katalog-Liste auf B-14 mit "in N Stacks" (F-30). */
    @Query(
        """
        SELECT m.*, (
            SELECT COUNT(DISTINCT e.stackId) FROM stack_eintrag e WHERE e.mittelId = m.id
        ) AS anzahlStacks
        FROM mittel m
        ORDER BY m.name COLLATE NOCASE ASC
        """
    )
    fun alleMittelMitAnzahl(): Flow<List<MittelMitAnzahl>>

    /** Gleiche Liste, aber gefiltert — B-14 hat eine Suchzeile ueber demselben Bestand. */
    @Query(
        """
        SELECT m.*, (
            SELECT COUNT(DISTINCT e.stackId) FROM stack_eintrag e WHERE e.mittelId = m.id
        ) AS anzahlStacks
        FROM mittel m
        WHERE :suchtext = '' OR m.name LIKE '%' || :suchtext || '%' COLLATE NOCASE
        ORDER BY m.name COLLATE NOCASE ASC
        """
    )
    fun sucheMittelMitAnzahl(suchtext: String): Flow<List<MittelMitAnzahl>>

    /** "gilt in N Stacks" im Kopf von B-05 (F-03). */
    @Query("SELECT COUNT(DISTINCT stackId) FROM stack_eintrag WHERE mittelId = :mittelId")
    fun anzahlStacksFuerMittel(mittelId: String): Flow<Int>

    /** Fuer die Warnung beim Loeschen: welche Stacks betrifft es? */
    @Query(
        """
        SELECT s.* FROM stack s
        WHERE s.id IN (SELECT e.stackId FROM stack_eintrag e WHERE e.mittelId = :mittelId)
        ORDER BY s.sortierung ASC
        """
    )
    fun stacksMitMittel(mittelId: String): Flow<List<StackE>>

    @Query(
        """
        SELECT s.* FROM stack s
        WHERE s.id IN (SELECT e.stackId FROM stack_eintrag e WHERE e.mittelId = :mittelId)
        ORDER BY s.sortierung ASC
        """
    )
    suspend fun stacksMitMittelEinmalig(mittelId: String): List<StackE>

    // ── Schreiben ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenMittel(mittel: MittelE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenMittel(mittel: List<MittelE>)

    @Update
    suspend fun aktualisierenMittel(mittel: MittelE)

    @Delete
    suspend fun loeschenMittel(mittel: MittelE)

    @Query("DELETE FROM mittel WHERE id = :mittelId")
    suspend fun loeschenMittel(mittelId: String)

    // ── Zusammenfuehren (F-30) ──────────────────────────────────────────────────

    /** Stacks, in denen beide Mittel liegen — dort wuerde das Umhaengen ein Dublett erzeugen. */
    @Query(
        """
        SELECT stackId FROM stack_eintrag WHERE mittelId = :verlierendeId
          AND stackId IN (SELECT stackId FROM stack_eintrag WHERE mittelId = :bleibendeId)
        """
    )
    suspend fun stacksMitBeidenMitteln(bleibendeId: String, verlierendeId: String): List<String>

    @Query("DELETE FROM stack_eintrag WHERE mittelId = :mittelId AND stackId IN (:stackIds)")
    suspend fun loeschenEintraegeVonMittelInStacks(mittelId: String, stackIds: List<String>)

    @Query("UPDATE stack_eintrag SET mittelId = :bleibendeId WHERE mittelId = :verlierendeId")
    suspend fun haengeEintraegeUm(bleibendeId: String, verlierendeId: String)

    @Query("UPDATE bewertungszelle SET mittelId = :bleibendeId WHERE mittelId = :verlierendeId")
    suspend fun haengeBewertungszellenUm(bleibendeId: String, verlierendeId: String)

    /**
     * Fuehrt zwei Katalog-Eintraege zusammen (F-30): alle Stack-Eintraege wandern auf das
     * bleibende Mittel, die Stack-Dosis bleibt dabei unveraendert.
     *
     * Liegen beide Mittel im selben Stack, wuerde das Umhaengen die Regel "ein Mittel nur einmal
     * je Stack" verletzen — dort wird der Eintrag des verlierenden Mittels vorher entfernt und
     * die bereits gepflegte Dosis des bleibenden behalten.
     */
    @Transaction
    suspend fun zusammenfuehren(bleibendeId: String, verlierendeId: String) {
        if (bleibendeId == verlierendeId) return
        val doppelteStacks = stacksMitBeidenMitteln(bleibendeId, verlierendeId)
        if (doppelteStacks.isNotEmpty()) {
            loeschenEintraegeVonMittelInStacks(verlierendeId, doppelteStacks)
        }
        haengeEintraegeUm(bleibendeId, verlierendeId)
        haengeBewertungszellenUm(bleibendeId, verlierendeId)
        loeschenMittel(verlierendeId)
    }
}
