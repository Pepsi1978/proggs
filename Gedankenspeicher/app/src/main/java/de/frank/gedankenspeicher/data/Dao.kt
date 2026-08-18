package de.frank.gedankenspeicher.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SitzungDao {

    /** Zuletzt benutzte oben (`01-FUNKTIONS-SPEC.md` F-12, Regeln). */
    @Query("SELECT * FROM sitzung ORDER BY zuletztGeoeffnet DESC")
    fun alle(): Flow<List<Sitzung>>

    @Query("SELECT * FROM sitzung WHERE id = :id")
    suspend fun eine(id: Long): Sitzung?

    @Query("SELECT * FROM sitzung ORDER BY zuletztGeoeffnet DESC LIMIT 1")
    suspend fun zuletztGeoeffnete(): Sitzung?

    @Query("SELECT COUNT(*) FROM sitzung")
    suspend fun anzahl(): Int

    @Query("SELECT COUNT(*) FROM notiz WHERE sitzungId = :sitzungId")
    fun notizzahl(sitzungId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notiz WHERE sitzungId = :sitzungId")
    suspend fun notizzahlJetzt(sitzungId: Long): Int

    @Query("SELECT MAX(erstelltAm) FROM notiz WHERE sitzungId = :sitzungId")
    fun letzteNotizzeit(sitzungId: Long): Flow<Long?>

    @Insert
    suspend fun einfuegen(sitzung: Sitzung): Long

    @Update
    suspend fun aendern(sitzung: Sitzung)

    @Delete
    suspend fun loeschen(sitzung: Sitzung)

    @Query("UPDATE sitzung SET zuletztGeoeffnet = :zeit WHERE id = :id")
    suspend fun merkeOeffnung(id: Long, zeit: Long)

    @Query("UPDATE sitzung SET titel = :titel, titelVonHand = :vonHand WHERE id = :id")
    suspend fun setzeTitel(id: Long, titel: String, vonHand: Boolean)
}

@Dao
interface NotizDao {

    @Query("SELECT * FROM notiz WHERE sitzungId = :sitzungId ORDER BY erstelltAm ASC")
    fun ausSitzung(sitzungId: Long): Flow<List<Notiz>>

    @Query("SELECT * FROM notiz WHERE id = :id")
    suspend fun eine(id: Long): Notiz?

    /**
     * Alle Notizen einer Sitzung, die **nach** dem angegebenen Zeitpunkt entstanden sind —
     * das ist der Kontext für die nächste Auswertung (F-09, Schritt 1).
     */
    @Query(
        "SELECT * FROM notiz WHERE sitzungId = :sitzungId AND erstelltAm > :seit " +
            "AND zustand = 'FERTIG' ORDER BY erstelltAm ASC",
    )
    suspend fun seit(sitzungId: Long, seit: Long): List<Notiz>

    @Query("SELECT * FROM notiz WHERE sitzungId = :sitzungId AND zustand = 'FERTIG' ORDER BY erstelltAm ASC")
    suspend fun alleFertigen(sitzungId: Long): List<Notiz>

    /** Die wartenden Aufnahmen, in der Reihenfolge ihrer Entstehung (F-04, Schritt 2). */
    @Query(
        "SELECT * FROM notiz WHERE zustand = 'WARTET_AUF_TRANSKRIPTION' " +
            "AND versucheTranskription < :hoechstversuche ORDER BY erstelltAm ASC",
    )
    suspend fun wartende(hoechstversuche: Int): List<Notiz>

    /** Was am fehlenden Groq-Schlüssel gescheitert ist — sobald er da ist, läuft es nach. */
    @Query("SELECT * FROM notiz WHERE zustand = 'KEIN_SCHLUESSEL' ORDER BY erstelltAm ASC")
    suspend fun ohneSchluessel(): List<Notiz>

    /** Notizen ohne Überschrift, die eine bekommen sollen (F-05, Fehlerfall: beim nächsten Start). */
    @Query("SELECT * FROM notiz WHERE zustand = 'FERTIG' AND ueberschrift IS NULL ORDER BY erstelltAm ASC LIMIT 20")
    suspend fun ohneUeberschrift(): List<Notiz>

    @Insert
    suspend fun einfuegen(notiz: Notiz): Long

    @Update
    suspend fun aendern(notiz: Notiz)

    @Delete
    suspend fun loeschen(notiz: Notiz)

    @Query("SELECT * FROM notiz WHERE zustand = 'AUFNEHMEND'")
    suspend fun angefangene(): List<Notiz>

    @Query("DELETE FROM notiz WHERE zustand = 'AUFNEHMEND'")
    suspend fun raeumeAngefangeneWeg()
}

@Dao
interface KiAntwortDao {

    @Query("SELECT * FROM ki_antwort WHERE sitzungId = :sitzungId ORDER BY erstelltAm ASC")
    fun ausSitzung(sitzungId: Long): Flow<List<KiAntwort>>

    @Query("SELECT * FROM ki_antwort WHERE sitzungId = :sitzungId ORDER BY erstelltAm ASC")
    suspend fun alleEinmal(sitzungId: Long): List<KiAntwort>

    /** Der Zeitpunkt der letzten Auswertung — die Grenze für die nächste (F-09). */
    @Query("SELECT MAX(erstelltAm) FROM ki_antwort WHERE sitzungId = :sitzungId")
    suspend fun letzteZeit(sitzungId: Long): Long?

    @Insert
    suspend fun einfuegen(antwort: KiAntwort): Long

    @Update
    suspend fun aendern(antwort: KiAntwort)

    @Delete
    suspend fun loeschen(antwort: KiAntwort)
}

@Dao
interface ProfilDao {

    @Query("SELECT * FROM auswertungsprofil ORDER BY nummer ASC")
    fun alle(): Flow<List<Auswertungsprofil>>

    @Query("SELECT * FROM auswertungsprofil WHERE istAktiv = 1 LIMIT 1")
    suspend fun aktives(): Auswertungsprofil?

    @Query("SELECT * FROM auswertungsprofil WHERE istAktiv = 1 LIMIT 1")
    fun aktivesLaufend(): Flow<Auswertungsprofil?>

    @Query("SELECT COUNT(*) FROM auswertungsprofil")
    suspend fun anzahl(): Int

    @Insert
    suspend fun einfuegenAlle(profile: List<Auswertungsprofil>)

    @Update
    suspend fun aendern(profil: Auswertungsprofil)

    /**
     * Setzt das Häkchen auf genau ein Profil.
     *
     * Beides in einer Transaktion, weil sonst für einen Augenblick **kein** Profil aktiv wäre —
     * und genau in diesem Augenblick könnte eine Auswertung starten und ohne Anweisung laufen.
     */
    @Transaction
    suspend fun setzeAktiv(nummer: Int) {
        alleAbwaehlen()
        waehleAus(nummer)
    }

    @Query("UPDATE auswertungsprofil SET istAktiv = 0")
    suspend fun alleAbwaehlen()

    @Query("UPDATE auswertungsprofil SET istAktiv = 1 WHERE nummer = :nummer")
    suspend fun waehleAus(nummer: Int)
}

@Dao
interface SucheDao {

    /**
     * Volltextsuche über Notizen (F-14). Groß- und Kleinschreibung spielen keine Rolle:
     * SQLite vergleicht `LIKE` bei ASCII ohnehin ohne Rücksicht darauf, und für Umlaute
     * setzt [Repository] den Suchbegriff zusätzlich klein.
     */
    @Query(
        """
        SELECT n.sitzungId AS sitzungId, s.titel AS sitzungstitel, n.id AS notizId,
               n.ueberschrift AS ueberschrift, n.text AS text, n.erstelltAm AS erstelltAm,
               0 AS istKiAntwort
        FROM notiz n JOIN sitzung s ON s.id = n.sitzungId
        WHERE lower(n.text) LIKE '%' || :begriff || '%'
           OR lower(COALESCE(n.ueberschrift, '')) LIKE '%' || :begriff || '%'
        ORDER BY n.erstelltAm DESC
        LIMIT 200
        """,
    )
    suspend fun inNotizen(begriff: String): List<Suchtreffer>

    @Query(
        """
        SELECT a.sitzungId AS sitzungId, s.titel AS sitzungstitel, a.id AS notizId,
               a.rueckfrage AS ueberschrift, a.text AS text, a.erstelltAm AS erstelltAm,
               1 AS istKiAntwort
        FROM ki_antwort a JOIN sitzung s ON s.id = a.sitzungId
        WHERE lower(a.text) LIKE '%' || :begriff || '%'
        ORDER BY a.erstelltAm DESC
        LIMIT 200
        """,
    )
    suspend fun inAntworten(begriff: String): List<Suchtreffer>
}
