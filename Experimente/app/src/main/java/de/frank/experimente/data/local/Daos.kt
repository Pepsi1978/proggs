package de.frank.experimente.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SelbstbildDao {
    @Query("SELECT * FROM selbstbild WHERE id = 1")
    fun beobachte(): Flow<SelfImage?>

    @Query("SELECT * FROM selbstbild WHERE id = 1")
    suspend fun lies(): SelfImage?

    @Upsert
    suspend fun schreibe(satz: SelfImage)
}

@Dao
interface ZieleDao {
    @Query("SELECT * FROM ziele ORDER BY createdAt ASC")
    fun beobachte(): Flow<List<Goal>>

    @Query("SELECT * FROM ziele ORDER BY createdAt ASC")
    suspend fun alle(): List<Goal>

    @Insert
    suspend fun lege(ziel: Goal): Long

    @Update
    suspend fun aendere(ziel: Goal)

    @Delete
    suspend fun loesche(ziel: Goal)
}

@Dao
interface LogbuchDao {
    /** Reiter *Letzte 15 Tage*: die Tage mit ausführlichem Text. */
    @Query("SELECT * FROM logtage WHERE detailText IS NOT NULL ORDER BY date DESC")
    fun beobachteAusfuehrlich(): Flow<List<LogDay>>

    /** Reiter *Langzeit*: die verdichteten Tage, dauerhaft. */
    @Query("SELECT * FROM logtage WHERE compactText IS NOT NULL ORDER BY date DESC")
    fun beobachteVerdichtet(): Flow<List<LogDay>>

    @Query("SELECT * FROM logtage WHERE date = :tag")
    suspend fun tag(tag: LocalDate): LogDay?

    @Query("SELECT * FROM logtage WHERE detailText IS NOT NULL ORDER BY date DESC")
    suspend fun alleAusfuehrlich(): List<LogDay>

    @Query("SELECT * FROM logtage WHERE compactText IS NOT NULL ORDER BY date DESC")
    suspend fun alleVerdichtet(): List<LogDay>

    /** F-15: alles, was älter als 15 Tage ist und noch ausführlich dasteht. */
    @Query("SELECT * FROM logtage WHERE detailText IS NOT NULL AND date < :grenze ORDER BY date ASC")
    suspend fun zuVerdichten(grenze: LocalDate): List<LogDay>

    @Upsert
    suspend fun schreibe(tag: LogDay)

    @Delete
    suspend fun loesche(tag: LogDay)
}

@Dao
interface VorschlaegeDao {
    /** Die heutigen, noch nicht verworfenen fünf. */
    @Query("SELECT * FROM vorschlaege WHERE date = :tag AND discardedAt IS NULL ORDER BY id ASC")
    fun beobachte(tag: LocalDate): Flow<List<Suggestion>>

    @Query("SELECT * FROM vorschlaege WHERE date = :tag AND discardedAt IS NULL ORDER BY id ASC")
    suspend fun aktuelle(tag: LocalDate): List<Suggestion>

    /** F-04: alle an diesem Tag verworfenen Titel bleiben bis Mitternacht ausgeschlossen. */
    @Query("SELECT title FROM vorschlaege WHERE date = :tag AND discardedAt IS NOT NULL")
    suspend fun verworfeneTitel(tag: LocalDate): List<String>

    @Query("SELECT * FROM vorschlaege WHERE id = :id")
    suspend fun einer(id: Long): Suggestion?

    @Insert
    suspend fun lege(vorschlaege: List<Suggestion>)

    @Query("UPDATE vorschlaege SET discardedAt = :jetzt WHERE date = :tag AND discardedAt IS NULL")
    suspend fun verwerfeAlle(tag: LocalDate, jetzt: java.time.Instant)
}

@Dao
interface ExperimenteDao {
    /**
     * Abschnitt „Läuft“ des Monitors — höchstens drei, das jüngst gestartete oben (F-34).
     * Laufende ordnen sich nach ihrem Startzeitpunkt und sind nicht verschiebbar (F-38).
     */
    @Query("SELECT * FROM experimente WHERE state = 'LAEUFT' ORDER BY startedAt DESC, id DESC")
    fun beobachteLaufende(): Flow<List<Experiment>>

    @Query("SELECT * FROM experimente WHERE state = 'LAEUFT' ORDER BY startedAt DESC, id DESC")
    suspend fun laufende(): List<Experiment>

    /** Abschnitt „Steht an“ — beliebig viele, in Franks eigener Reihenfolge (F-38). */
    @Query("SELECT * FROM experimente WHERE state = 'ANSTEHEND' ORDER BY order_index ASC, addedAt DESC")
    fun beobachteAnstehende(): Flow<List<Experiment>>

    @Query("SELECT * FROM experimente WHERE state = 'ANSTEHEND' ORDER BY order_index ASC, addedAt DESC")
    suspend fun anstehende(): List<Experiment>

    /** Die Grenze von drei gilt fürs Laufen, nicht fürs Vormerken (F-34, F-37). */
    @Query("SELECT COUNT(*) FROM experimente WHERE state = 'LAEUFT'")
    suspend fun anzahlLaufende(): Int

    /** F-36 — ein Vorschlag lässt sich nicht doppelt übernehmen. */
    @Query("SELECT COUNT(*) FROM experimente WHERE title = :titel AND state IN ('ANSTEHEND', 'LAEUFT')")
    suspend fun zaehleImMonitor(titel: String): Int

    /** F-35 — neue Karten kommen **oben** im Abschnitt „Steht an“ an. */
    @Query("SELECT MIN(order_index) FROM experimente WHERE state = 'ANSTEHEND'")
    suspend fun kleinsterRang(): Int?

    @Query("SELECT * FROM experimente WHERE id = :id")
    suspend fun einer(id: Long): Experiment?

    @Query("SELECT * FROM experimente WHERE state IN ('ABGESCHLOSSEN', 'NICHT_UMGESETZT') ORDER BY closedAt DESC")
    suspend fun abgeschlossene(): List<Experiment>

    @Insert
    suspend fun lege(experiment: Experiment): Long

    @Update
    suspend fun aendere(experiment: Experiment)

    /** F-38 — die neue Reihenfolge aller betroffenen Sätze. */
    @Query("UPDATE experimente SET order_index = :rang WHERE id = :id")
    suspend fun setzeRang(id: Long, rang: Int)

    /** F-39, Weg „Löschen“ — endgültig, mitsamt seinen Aufgaben. */
    @Delete
    suspend fun loesche(experiment: Experiment)
}

@Dao
interface AufgabenDao {
    @Query("SELECT * FROM aufgaben WHERE experimentId = :experimentId AND dayIndex = :tagNr ORDER BY order_index ASC")
    suspend fun tagesaufgaben(experimentId: Long, tagNr: Int): List<Task>

    @Query("SELECT * FROM aufgaben WHERE experimentId IN (:experimentIds) ORDER BY experimentId ASC, order_index ASC")
    fun beobachte(experimentIds: List<Long>): Flow<List<Task>>

    @Insert
    suspend fun lege(aufgaben: List<Task>)

    /** F-39 — mit dem Experiment gehen auch seine Aufgaben. */
    @Query("DELETE FROM aufgaben WHERE experimentId = :experimentId")
    suspend fun loescheZu(experimentId: Long)

    /** F-37 Schritt 1 — beim Start werden die Aufgaben auf die Tage verteilt. */
    @Query("SELECT * FROM aufgaben WHERE experimentId = :experimentId ORDER BY dayIndex ASC, order_index ASC")
    suspend fun alleZu(experimentId: Long): List<Task>

    @Query("UPDATE aufgaben SET doneAt = :zeitpunkt WHERE id = :id")
    suspend fun setzeHaken(id: Long, zeitpunkt: java.time.Instant?)

    @Query("SELECT * FROM aufgaben WHERE id = :id")
    suspend fun eine(id: Long): Task?
}

@Dao
interface AuswertungenDao {
    @Query("SELECT * FROM auswertungen WHERE experimentId = :experimentId ORDER BY date ASC")
    suspend fun zumExperiment(experimentId: Long): List<Evaluation>

    @Query("SELECT * FROM auswertungen WHERE experimentId = :experimentId AND date = :tag")
    suspend fun anTag(experimentId: Long, tag: LocalDate): Evaluation?

    @Query("SELECT * FROM auswertungen WHERE date = :tag")
    fun beobachteTag(tag: LocalDate): Flow<List<Evaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun lege(auswertung: Evaluation): Long

    @Update
    suspend fun aendere(auswertung: Evaluation)
}

@Dao
interface GespraechDao {
    @Query("SELECT * FROM gespraech WHERE experimentId = :experimentId ORDER BY createdAt ASC, id ASC")
    fun beobachte(experimentId: Long): Flow<List<ChatTurn>>

    @Query("SELECT * FROM gespraech WHERE experimentId = :experimentId ORDER BY createdAt ASC, id ASC")
    suspend fun faden(experimentId: Long): List<ChatTurn>

    @Insert
    suspend fun lege(runde: ChatTurn): Long
}

@Dao
interface MerklisteDao {
    @Query("SELECT * FROM merkliste ORDER BY createdAt DESC")
    fun beobachte(): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM merkliste ORDER BY createdAt DESC")
    suspend fun alle(): List<WatchlistItem>

    @Query("SELECT COUNT(*) FROM merkliste WHERE title = :titel")
    suspend fun zaehleMitTitel(titel: String): Int

    @Insert
    suspend fun lege(eintrag: WatchlistItem): Long

    @Delete
    suspend fun loesche(eintrag: WatchlistItem)

    @Query("DELETE FROM merkliste WHERE title = :titel")
    suspend fun loescheMitTitel(titel: String)
}

@Dao
interface ErkenntnisseDao {
    @Query("SELECT * FROM erkenntnisse ORDER BY updatedAt DESC")
    fun beobachte(): Flow<List<Insight>>

    @Query("SELECT * FROM erkenntnisse ORDER BY updatedAt DESC")
    suspend fun alle(): List<Insight>

    @Insert
    suspend fun lege(erkenntnis: Insight): Long

    @Update
    suspend fun aendere(erkenntnis: Insight)

    @Delete
    suspend fun loesche(erkenntnis: Insight)
}

@Dao
interface LageDao {
    @Query("SELECT * FROM lage WHERE date = :tag")
    fun beobachte(tag: LocalDate): Flow<Lage?>

    @Query("SELECT * FROM lage WHERE date = :tag")
    suspend fun anTag(tag: LocalDate): Lage?

    @Upsert
    suspend fun schreibe(lage: Lage)
}
