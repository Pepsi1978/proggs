package de.frank.stacklabor.daten.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.frank.stacklabor.daten.entitaeten.EintragMitMittel
import de.frank.stacklabor.daten.entitaeten.StackE
import de.frank.stacklabor.daten.entitaeten.StackEintragE
import kotlinx.coroutines.flow.Flow

/** Stacks (F-01) und ihre Eintraege (F-02 bis F-07, F-28). */
@Dao
interface StackDao {

    // ── Stacks lesen ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM stack ORDER BY sortierung ASC, name ASC")
    fun alleStacks(): Flow<List<StackE>>

    @Query("SELECT * FROM stack WHERE id = :stackId")
    fun stack(stackId: String): Flow<StackE?>

    @Query("SELECT * FROM stack WHERE id = :stackId")
    suspend fun stackEinmalig(stackId: String): StackE?

    /** Suche in der Stack-Liste (F-24) — ohne Ruecksicht auf Gross- und Kleinschreibung. */
    @Query(
        """
        SELECT * FROM stack
        WHERE :suchtext = '' OR name LIKE '%' || :suchtext || '%' COLLATE NOCASE
        ORDER BY sortierung ASC, name ASC
        """
    )
    fun sucheStacks(suchtext: String): Flow<List<StackE>>

    /** Naechste freie Sortierung — neue Stacks kommen ans Ende. */
    @Query("SELECT COALESCE(MAX(sortierung), -1) + 1 FROM stack")
    suspend fun naechsteSortierung(): Int

    /** Anzahl der Mittel in einem Stack — fuer die Rueckfrage beim Loeschen (F-01). */
    @Query("SELECT COUNT(*) FROM stack_eintrag WHERE stackId = :stackId")
    fun anzahlEintraege(stackId: String): Flow<Int>

    // ── Stacks schreiben ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenStack(stack: StackE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenStacks(stacks: List<StackE>)

    @Update
    suspend fun aktualisierenStack(stack: StackE)

    /** Loescht den Stack; Eintraege, Zielzuordnungen, Fragen und Bewertungen fallen per CASCADE mit. */
    @Delete
    suspend fun loeschenStack(stack: StackE)

    @Query("DELETE FROM stack WHERE id = :stackId")
    suspend fun loeschenStack(stackId: String)

    @Query("UPDATE stack SET sortierung = :sortierung WHERE id = :stackId")
    suspend fun setzeSortierung(stackId: String, sortierung: Int)

    /** Batch fuer Drag & Drop auf B-01: die Listenposition wird zur Sortierung. */
    @Transaction
    suspend fun setzeSortierungen(stackIdsInReihenfolge: List<String>) {
        stackIdsInReihenfolge.forEachIndexed { position, stackId -> setzeSortierung(stackId, position) }
    }

    // ── Eintraege lesen ─────────────────────────────────────────────────────────

    /** Die Liste auf B-02 in gespeicherter Einnahme-Reihenfolge, samt Katalog-Stammdaten. */
    @Transaction
    @Query(
        """
        SELECT * FROM stack_eintrag
        WHERE stackId = :stackId
        ORDER BY reihenfolge ASC
        """
    )
    fun eintraegeMitMittel(stackId: String): Flow<List<EintragMitMittel>>

    /** Alle Eintraege aller Stacks — Grundlage der Tagesgesamtdosis (F-13). */
    @Transaction
    @Query("SELECT * FROM stack_eintrag ORDER BY stackId ASC, reihenfolge ASC")
    fun alleEintraegeMitMittel(): Flow<List<EintragMitMittel>>

    /** Nur die aktiven Eintraege — deaktivierte gehen weder in die Anfrage noch in die Summe ein. */
    @Transaction
    @Query("SELECT * FROM stack_eintrag WHERE aktiv = 1 ORDER BY stackId ASC, reihenfolge ASC")
    fun alleAktivenEintraegeMitMittel(): Flow<List<EintragMitMittel>>

    /** Aktive Eintraege eines Stacks — das, was bei F-12 mitgeschickt wird. */
    @Transaction
    @Query("SELECT * FROM stack_eintrag WHERE stackId = :stackId AND aktiv = 1 ORDER BY reihenfolge ASC")
    suspend fun aktiveEintraegeMitMittelEinmalig(stackId: String): List<EintragMitMittel>

    @Query("SELECT * FROM stack_eintrag WHERE id = :eintragId")
    fun eintrag(eintragId: String): Flow<StackEintragE?>

    @Query("SELECT * FROM stack_eintrag WHERE id = :eintragId")
    suspend fun eintragEinmalig(eintragId: String): StackEintragE?

    /** Prueft, ob ein Mittel schon im Stack liegt (F-02: nur einmal je Stack). */
    @Query("SELECT * FROM stack_eintrag WHERE stackId = :stackId AND mittelId = :mittelId")
    suspend fun eintragFuerMittel(stackId: String, mittelId: String): StackEintragE?

    /** Mitglieder einer Kombi-Gruppe (F-28). */
    @Transaction
    @Query(
        """
        SELECT * FROM stack_eintrag
        WHERE stackId = :stackId AND gruppeId = :gruppeId
        ORDER BY reihenfolge ASC
        """
    )
    fun gruppenMitglieder(stackId: String, gruppeId: String): Flow<List<EintragMitMittel>>

    /** Eintraege mit noch offenem Hinweis aus der Konkurrenzpruefung (F-02, geht nie verloren). */
    @Query("SELECT * FROM stack_eintrag WHERE stackId = :stackId AND offenerHinweis <> ''")
    fun eintraegeMitOffenemHinweis(stackId: String): Flow<List<StackEintragE>>

    /** Fuer den Punkt auf der Stack-Karte (B-01): gibt es irgendwo noch einen offenen Hinweis? */
    @Query("SELECT DISTINCT stackId FROM stack_eintrag WHERE offenerHinweis <> ''")
    fun stackIdsMitOffenemHinweis(): Flow<List<String>>

    @Query("SELECT COALESCE(MAX(reihenfolge), -1) + 1 FROM stack_eintrag WHERE stackId = :stackId")
    suspend fun naechsteReihenfolge(stackId: String): Int

    // ── Eintraege schreiben ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenEintrag(eintrag: StackEintragE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenEintraege(eintraege: List<StackEintragE>)

    @Update
    suspend fun aktualisierenEintrag(eintrag: StackEintragE)

    @Delete
    suspend fun loeschenEintrag(eintrag: StackEintragE)

    @Query("DELETE FROM stack_eintrag WHERE id = :eintragId")
    suspend fun loeschenEintrag(eintragId: String)

    /** Entfernt eine ganze Kombi-Gruppe auf einmal (F-04, Rueckfrage bei Gruppen). */
    @Query("DELETE FROM stack_eintrag WHERE stackId = :stackId AND gruppeId = :gruppeId")
    suspend fun loeschenGruppe(stackId: String, gruppeId: String)

    /** Das Haekchen (F-05) — dauerhaft gespeichert, macht die Bewertung nicht veraltet. */
    @Query("UPDATE stack_eintrag SET aktiv = :aktiv WHERE id = :eintragId")
    suspend fun setzeAktiv(eintragId: String, aktiv: Boolean)

    /** Gruppen-Haekchen: schaltet alle Mitglieder gemeinsam (F-05, F-28). */
    @Query("UPDATE stack_eintrag SET aktiv = :aktiv WHERE stackId = :stackId AND gruppeId = :gruppeId")
    suspend fun setzeAktivFuerGruppe(stackId: String, gruppeId: String, aktiv: Boolean)

    @Query("UPDATE stack_eintrag SET reihenfolge = :reihenfolge WHERE id = :eintragId")
    suspend fun setzeReihenfolge(eintragId: String, reihenfolge: Int)

    /**
     * Batch fuer Drag & Drop (F-07): die uebergebene Liste ist die neue Einnahme-Reihenfolge.
     * In einer Transaktion, damit die Liste nie halb umnummeriert beobachtet wird.
     */
    @Transaction
    suspend fun setzeReihenfolgen(eintragIdsInReihenfolge: List<String>) {
        eintragIdsInReihenfolge.forEachIndexed { position, eintragId ->
            setzeReihenfolge(eintragId, position)
        }
    }

    /** Kombi-Gruppe bilden oder aufloesen (F-28); null loest die Zugehoerigkeit auf. */
    @Query("UPDATE stack_eintrag SET gruppeId = :gruppeId WHERE id = :eintragId")
    suspend fun setzeGruppe(eintragId: String, gruppeId: String?)

    @Transaction
    suspend fun setzeGruppeFuer(eintragIds: List<String>, gruppeId: String?) {
        eintragIds.forEach { eintragId -> setzeGruppe(eintragId, gruppeId) }
    }

    @Query("UPDATE stack_eintrag SET offenerHinweis = :hinweis WHERE id = :eintragId")
    suspend fun setzeOffenenHinweis(eintragId: String, hinweis: String)

    @Query("UPDATE stack_eintrag SET offenerHinweis = '' WHERE stackId = :stackId")
    suspend fun leereOffeneHinweise(stackId: String)
}
