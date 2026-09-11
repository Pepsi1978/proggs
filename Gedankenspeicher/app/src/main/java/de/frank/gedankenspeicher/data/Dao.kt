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

    /**
     * Zuletzt **geänderte** oben.
     *
     * Nicht die zuletzt geöffnete: sonst sortierte sich die Liste schon um, wenn man eine
     * Notiz nur aufschlägt und wieder zumacht.
     */
    @Query("SELECT * FROM sitzung ORDER BY zuletztGeaendert DESC, erstelltAm DESC")
    fun alle(): Flow<List<Sitzung>>

    @Query("SELECT * FROM sitzung WHERE id = :id")
    suspend fun eine(id: Long): Sitzung?

    /** Die zuletzt benutzte sichtbare Sitzung: nicht im Papierkorb, nicht geschützt. */
    @Query(
        "SELECT * FROM sitzung WHERE geloeschtAm IS NULL AND geschuetzt = 0 " +
            "ORDER BY zuletztGeoeffnet DESC LIMIT 1",
    )
    suspend fun zuletztGeoeffnete(): Sitzung?

    @Query("SELECT COUNT(*) FROM sitzung WHERE geloeschtAm IS NULL AND geschuetzt = 0")
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

    /** Jede echte Änderung am Inhalt hebt die Sitzung in der Liste nach oben. */
    @Query("UPDATE sitzung SET zuletztGeaendert = :zeit WHERE id = :id")
    suspend fun merkeAenderung(id: Long, zeit: Long)

    @Query("UPDATE sitzung SET titel = :titel, titelVonHand = :vonHand WHERE id = :id")
    suspend fun setzeTitel(id: Long, titel: String, vonHand: Boolean)

    @Query("UPDATE sitzung SET titel = :titel WHERE id = :id AND titelVonHand = 0 AND titel = 'Neue Sitzung' AND (:notizId IS NULL OR EXISTS (SELECT 1 FROM notiz WHERE id = :notizId AND sitzungId = :id AND text = :notiztext))")
    suspend fun setzeKiTitel(id: Long, titel: String, notizId: Long?, notiztext: String)

    @Query("UPDATE sitzung SET favorit = CASE favorit WHEN 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun favoritUmschalten(id: Long)

    @Query("UPDATE sitzung SET geschuetzt = :geschuetzt WHERE id = :id")
    suspend fun setzeSchutz(id: Long, geschuetzt: Boolean)

    @Query("UPDATE sitzung SET geloeschtAm = :zeit WHERE id = :id")
    suspend fun setzePapierkorb(id: Long, zeit: Long?)

    @Query("UPDATE sitzung SET ordnerId = :ordnerId WHERE id = :id")
    suspend fun setzeOrdner(id: Long, ordnerId: Long?)

    @Query("DELETE FROM sitzung WHERE geloeschtAm IS NOT NULL")
    suspend fun leerePapierkorb()
}

@Dao
interface OrdnerDao {

    @Query("SELECT * FROM ordner ORDER BY name COLLATE NOCASE")
    fun alle(): Flow<List<Ordner>>

    @Insert
    suspend fun einfuegen(ordner: Ordner): Long

    @Update
    suspend fun aendern(ordner: Ordner)

    @Query("DELETE FROM ordner WHERE id = :id")
    suspend fun loeschen(id: Long)

    /** Beim Löschen eines Ordners bleiben die Sitzungen erhalten und landen ausserhalb. */
    @Query("UPDATE sitzung SET ordnerId = NULL WHERE ordnerId = :id")
    suspend fun loeseSitzungen(id: Long)

    /** Zuordnungen und Kategorie verschwinden atomar, die Sitzungen selbst bleiben erhalten. */
    @Transaction
    suspend fun loescheMitZuordnungen(id: Long) {
        loeseSitzungen(id)
        loeschen(id)
    }
}

@Dao
interface NotizDao {

    @Query("SELECT * FROM notiz WHERE sitzungId = :sitzungId ORDER BY erstelltAm ASC")
    fun ausSitzung(sitzungId: Long): Flow<List<Notiz>>

    /** Alle Notizen, laufend — die Quelle für die Aktivitätszeiten der Seitenleiste. */
    @Query("SELECT * FROM notiz")
    fun alleLaufend(): Flow<List<Notiz>>

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
    @Query(
        "SELECT * FROM notiz WHERE zustand = 'FERTIG' AND ueberschrift IS NULL " +
            "AND ueberschriftVonHand = 0 AND length(trim(text)) > 0 ORDER BY erstelltAm ASC LIMIT 20",
    )
    suspend fun ohneUeberschrift(): List<Notiz>

    /** Alle Notizen einer Sitzung, einmalig — zum Wegräumen ihrer Dateien vor dem Löschen. */
    @Query("SELECT * FROM notiz WHERE sitzungId = :sitzungId")
    suspend fun alleAusSitzung(sitzungId: Long): List<Notiz>

    /** Alle Notizen aus Sitzungen im Papierkorb — zum Wegräumen ihrer Dateien vor dem Leeren. */
    @Query("SELECT * FROM notiz WHERE sitzungId IN (SELECT id FROM sitzung WHERE geloeschtAm IS NOT NULL)")
    suspend fun imPapierkorb(): List<Notiz>

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

    @Query("UPDATE notiz SET zustand = CASE WHEN audioPfad IS NULL OR versucheTranskription >= 3 THEN 'TRANSKRIPTION_FEHLGESCHLAGEN' ELSE 'WARTET_AUF_TRANSKRIPTION' END WHERE zustand = 'TRANSKRIBIERT_GERADE'")
    suspend fun repariereUnterbrocheneTranskriptionen()
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

    @Query("UPDATE auswertungsprofil SET name = :name, anweisung = :anweisung, istAktiv = CASE WHEN :leer THEN 0 ELSE istAktiv END WHERE nummer = :nummer")
    suspend fun aendereText(nummer: Int, name: String, anweisung: String, leer: Boolean)

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

    @Query("UPDATE auswertungsprofil SET istAktiv = 1 WHERE nummer = :nummer AND trim(anweisung) != ''")
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
        WHERE (lower(replace(replace(replace(replace(n.text, 'Ä', 'ä'), 'Ö', 'ö'), 'Ü', 'ü'), 'ẞ', 'ß')) LIKE '%' || :begriff || '%' ESCAPE '\'
           OR lower(replace(replace(replace(replace(COALESCE(n.ueberschrift, ''), 'Ä', 'ä'), 'Ö', 'ö'), 'Ü', 'ü'), 'ẞ', 'ß')) LIKE '%' || :begriff || '%' ESCAPE '\')
          AND s.geloeschtAm IS NULL
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
        WHERE lower(replace(replace(replace(replace(a.text, 'Ä', 'ä'), 'Ö', 'ö'), 'Ü', 'ü'), 'ẞ', 'ß')) LIKE '%' || :begriff || '%' ESCAPE '\'
          AND s.geloeschtAm IS NULL
        ORDER BY a.erstelltAm DESC
        LIMIT 200
        """,
    )
    suspend fun inAntworten(begriff: String): List<Suchtreffer>
}
