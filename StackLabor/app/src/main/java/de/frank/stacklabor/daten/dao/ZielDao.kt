package de.frank.stacklabor.daten.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.frank.stacklabor.daten.entitaeten.StackE
import de.frank.stacklabor.daten.entitaeten.StackZielE
import de.frank.stacklabor.daten.entitaeten.ZielE
import de.frank.stacklabor.daten.entitaeten.ZielMitAnzahl
import de.frank.stacklabor.daten.entitaeten.ZielMitRang
import kotlinx.coroutines.flow.Flow

/** Ziel-Katalog (F-08) und die stack-eigene Zuordnung samt Rang (F-09, F-10). */
@Dao
interface ZielDao {

    // ── Katalog lesen ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM ziel ORDER BY text COLLATE NOCASE ASC")
    fun alleZiele(): Flow<List<ZielE>>

    @Query("SELECT * FROM ziel WHERE id = :zielId")
    fun ziel(zielId: String): Flow<ZielE?>

    @Query("SELECT * FROM ziel WHERE id = :zielId")
    suspend fun zielEinmalig(zielId: String): ZielE?

    /** Ziel-Liste auf B-03 mit "in N Stacks verwendet" (F-08). */
    @Query(
        """
        SELECT z.*, (
            SELECT COUNT(*) FROM stack_ziel sz WHERE sz.zielId = z.id
        ) AS anzahlStacks
        FROM ziel z
        ORDER BY z.text COLLATE NOCASE ASC
        """
    )
    fun alleZieleMitAnzahl(): Flow<List<ZielMitAnzahl>>

    /** Fuer die Warnung beim Loeschen eines verwendeten Ziels (F-08). */
    @Query(
        """
        SELECT s.* FROM stack s
        WHERE s.id IN (SELECT sz.stackId FROM stack_ziel sz WHERE sz.zielId = :zielId)
        ORDER BY s.sortierung ASC
        """
    )
    suspend fun stacksMitZielEinmalig(zielId: String): List<StackE>

    // ── Zuordnung lesen ─────────────────────────────────────────────────────────

    /** Die Ziele dieses Stacks in ihrer Rangfolge (B-04, B-12, F-14). */
    @Query(
        """
        SELECT z.*, sz.rang AS rang
        FROM ziel z
        JOIN stack_ziel sz ON sz.zielId = z.id
        WHERE sz.stackId = :stackId
        ORDER BY sz.rang ASC
        """
    )
    fun zieleDesStacks(stackId: String): Flow<List<ZielMitRang>>

    @Query(
        """
        SELECT z.*, sz.rang AS rang
        FROM ziel z
        JOIN stack_ziel sz ON sz.zielId = z.id
        WHERE sz.stackId = :stackId
        ORDER BY sz.rang ASC
        """
    )
    suspend fun zieleDesStacksEinmalig(stackId: String): List<ZielMitRang>

    @Query("SELECT * FROM stack_ziel WHERE stackId = :stackId ORDER BY rang ASC")
    fun zuordnungen(stackId: String): Flow<List<StackZielE>>

    /** Alle Zuordnungen aller Stacks — die Sammelampeln auf B-01 brauchen die Raenge am Stueck. */
    @Query("SELECT * FROM stack_ziel ORDER BY stackId ASC, rang ASC")
    fun alleZuordnungen(): Flow<List<StackZielE>>

    @Query("SELECT COALESCE(MAX(rang), -1) + 1 FROM stack_ziel WHERE stackId = :stackId")
    suspend fun naechsterRang(stackId: String): Int

    // ── Katalog schreiben ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenZiel(ziel: ZielE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenZiele(ziele: List<ZielE>)

    @Update
    suspend fun aktualisierenZiel(ziel: ZielE)

    @Delete
    suspend fun loeschenZiel(ziel: ZielE)

    @Query("DELETE FROM ziel WHERE id = :zielId")
    suspend fun loeschenZiel(zielId: String)

    @Query("DELETE FROM bewertungszelle WHERE zielId = :zielId")
    suspend fun loeschenZellenFuerZiel(zielId: String)

    /**
     * Loescht ein Ziel samt seiner Bewertungszellen (F-08, Fehlerfall).
     * Die Zuordnungen fallen per CASCADE mit.
     */
    @Transaction
    suspend fun loeschenZielMitZellen(zielId: String) {
        loeschenZellenFuerZiel(zielId)
        loeschenZiel(zielId)
    }

    // ── Zuordnung schreiben ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setzeZuordnung(zuordnung: StackZielE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setzeZuordnungen(zuordnungen: List<StackZielE>)

    @Query("DELETE FROM stack_ziel WHERE stackId = :stackId AND zielId = :zielId")
    suspend fun entferneZuordnung(stackId: String, zielId: String)

    /** Neu angehaktes Ziel bekommt den letzten Rang (F-09). */
    @Transaction
    suspend fun waehleZiel(stackId: String, zielId: String) {
        setzeZuordnung(StackZielE(stackId = stackId, zielId = zielId, rang = naechsterRang(stackId)))
    }

    @Query("UPDATE stack_ziel SET rang = :rang WHERE stackId = :stackId AND zielId = :zielId")
    suspend fun setzeRang(stackId: String, zielId: String, rang: Int)

    /**
     * Batch fuer das Priorisieren auf B-12 (F-10): die uebergebene Liste ist die neue Rangfolge
     * dieses Stacks. Raenge anderer Stacks bleiben unberuehrt.
     */
    @Transaction
    suspend fun setzeRaenge(stackId: String, zielIdsInReihenfolge: List<String>) {
        zielIdsInReihenfolge.forEachIndexed { position, zielId -> setzeRang(stackId, zielId, position) }
    }
}
