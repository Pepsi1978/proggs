package de.frank.claudekompass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EintragDao {

    /**
     * Alle sichtbaren Einträge eines Bereichs, alphabetisch.
     *
     * `sortierName` statt `name`: sonst stünden alle Slash-Befehle unter „/" und die
     * Sortierung wäre case-abhängig (`Bash` vor `agent`).
     */
    @Query("SELECT * FROM eintraege WHERE bereich = :bereich AND entfernt = 0 ORDER BY sortierName ASC")
    fun beobachteAktive(bereich: String): Flow<List<EintragEntity>>

    @Query("SELECT * FROM eintraege WHERE bereich = :bereich AND entfernt = 1 ORDER BY sortierName ASC")
    fun beobachteEntfernte(bereich: String): Flow<List<EintragEntity>>

    @Query("SELECT * FROM eintraege WHERE id = :id")
    suspend fun lade(id: String): EintragEntity?

    @Query("SELECT * FROM eintraege WHERE bereich = :bereich ORDER BY sortierName ASC")
    suspend fun ladeAlle(bereich: String): List<EintragEntity>

    @Query("SELECT * FROM eintraege ORDER BY bereich, sortierName ASC")
    suspend fun ladeKomplett(): List<EintragEntity>

    @Query("SELECT COUNT(*) FROM eintraege")
    suspend fun anzahl(): Int

    /**
     * Alle Einträge, die noch keine deutsche Erklärung haben.
     *
     * Das ist die Warteschlange des Aktualisierens: Neue Einträge kommen zuerst nur mit ihrem
     * englischen Text herein, die Erklärung wird danach einzeln nachgezogen. Bricht der Lauf
     * dazwischen ab, findet der nächste Lauf hier genau die Reste — statt alles noch einmal
     * von vorn erklären zu lassen.
     */
    @Query("SELECT * FROM eintraege WHERE entfernt = 0 AND TRIM(erklaerung) = '' ORDER BY bereich, sortierName ASC")
    suspend fun ladeUnerklaerte(): List<EintragEntity>

    @Query("SELECT COUNT(*) FROM eintraege WHERE entfernt = 0 AND TRIM(erklaerung) = ''")
    suspend fun anzahlUnerklaerte(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setze(eintraege: List<EintragEntity>)

    /**
     * Löscht Einträge endgültig.
     *
     * Gedacht für Namen, die ein früherer, fehlerhafter Lauf erfunden hat. Sie als „entfernt"
     * zu führen wäre falsch: Sie gab es in Claude Code nie, und im Klapp-Bereich „Entfernte
     * Einträge" würden sie dauerhaft Unsinn behaupten.
     */
    @Query("DELETE FROM eintraege WHERE id IN (:ids)")
    suspend fun loesche(ids: List<String>)

    @Update
    suspend fun aktualisiere(eintrag: EintragEntity)

    /**
     * Löscht die Neu-Markierung aller Einträge, die NICHT aus dem laufenden Durchgang stammen.
     *
     * Genau das macht die farbliche Hervorhebung sitzungsbezogen: was beim letzten Mal neu war,
     * gehört jetzt zum Bestand und wird nicht mehr hervorgehoben.
     */
    @Query("UPDATE eintraege SET neuImLauf = 0 WHERE neuImLauf != :laufId")
    suspend fun entferneAlteNeuMarkierungen(laufId: Long)
}

@Dao
interface ErklaerungDao {

    @Insert
    suspend fun sichere(eintrag: ErklaerungHistorieEntity): Long

    /** Die jeweils zuletzt gesicherte Fassung — der Zurück-Pfeil holt genau diese. */
    @Query("SELECT * FROM erklaerung_historie WHERE eintragId = :eintragId ORDER BY id DESC LIMIT 1")
    suspend fun letzte(eintragId: String): ErklaerungHistorieEntity?

    @Query("DELETE FROM erklaerung_historie WHERE id = :id")
    suspend fun loesche(id: Long)

    @Query("SELECT COUNT(*) FROM erklaerung_historie WHERE eintragId = :eintragId")
    fun beobachteAnzahl(eintragId: String): Flow<Int>

    @Query("SELECT eintragId, COUNT(*) AS anzahl FROM erklaerung_historie GROUP BY eintragId")
    fun beobachteAlleAnzahlen(): Flow<List<HistorieZaehler>>
}

data class HistorieZaehler(val eintragId: String, val anzahl: Int)

@Dao
interface FrageDao {

    @Query("SELECT * FROM fragen WHERE eintragId = :eintragId ORDER BY id ASC")
    fun beobachte(eintragId: String): Flow<List<FrageEntity>>

    @Query("SELECT * FROM fragen ORDER BY eintragId, id ASC")
    fun beobachteAlle(): Flow<List<FrageEntity>>

    @Insert
    suspend fun fuegeEin(frage: FrageEntity): Long

    @Update
    suspend fun aktualisiere(frage: FrageEntity)

    @Query("SELECT * FROM fragen WHERE id = :id")
    suspend fun lade(id: Long): FrageEntity?

    @Query("DELETE FROM fragen WHERE id = :id")
    suspend fun loesche(id: Long)
}

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_sitzungen ORDER BY zuletztAm DESC")
    fun beobachteSitzungen(): Flow<List<ChatSitzungEntity>>

    @Query("SELECT * FROM chat_nachrichten WHERE sitzungId = :sitzungId ORDER BY id ASC")
    fun beobachteNachrichten(sitzungId: Long): Flow<List<ChatNachrichtEntity>>

    @Query("SELECT * FROM chat_nachrichten WHERE sitzungId = :sitzungId ORDER BY id ASC")
    suspend fun ladeNachrichten(sitzungId: Long): List<ChatNachrichtEntity>

    /** Alle Nachrichten aller Gespräche — für den Neuaufbau des Suchindex. */
    @Query("SELECT * FROM chat_nachrichten ORDER BY id ASC")
    suspend fun ladeAlleNachrichten(): List<ChatNachrichtEntity>

    @Insert
    suspend fun lege(sitzung: ChatSitzungEntity): Long

    @Update
    suspend fun aktualisiere(sitzung: ChatSitzungEntity)

    @Query("SELECT * FROM chat_sitzungen WHERE id = :id")
    suspend fun ladeSitzung(id: Long): ChatSitzungEntity?

    @Query("DELETE FROM chat_sitzungen WHERE id = :id")
    suspend fun loescheSitzung(id: Long)

    @Insert
    suspend fun fuegeEin(nachricht: ChatNachrichtEntity): Long

    @Update
    suspend fun aktualisiere(nachricht: ChatNachrichtEntity)

    @Query("SELECT * FROM chat_nachrichten WHERE id = :id")
    suspend fun ladeNachricht(id: Long): ChatNachrichtEntity?

    @Transaction
    suspend fun beruehre(sitzungId: Long) {
        val sitzung = ladeSitzung(sitzungId) ?: return
        aktualisiere(sitzung.copy(zuletztAm = System.currentTimeMillis()))
    }
}

@Dao
interface AktualisierungDao {

    @Insert
    suspend fun starte(lauf: AktualisierungEntity): Long

    @Update
    suspend fun aktualisiere(lauf: AktualisierungEntity)

    @Query("SELECT * FROM aktualisierungen WHERE id = :id")
    suspend fun lade(id: Long): AktualisierungEntity?

    /** Der letzte abgeschlossene Lauf liefert die Kopfzeile „Aktualisiert für Version …". */
    @Query("SELECT * FROM aktualisierungen WHERE status = 'fertig' ORDER BY id DESC LIMIT 1")
    fun beobachteLetztenErfolg(): Flow<AktualisierungEntity?>

    @Query("SELECT * FROM aktualisierungen ORDER BY id DESC LIMIT 20")
    fun beobachteVerlauf(): Flow<List<AktualisierungEntity>>
}
