package de.frank.kompass.data

import de.frank.kompass.data.local.ChatNachrichtEntity
import de.frank.kompass.data.local.ChatSitzungEntity
import de.frank.kompass.data.local.EintragEntity
import de.frank.kompass.data.local.FrageEntity

/**
 * Die Kompass-Seite der Sicherung.
 *
 * Das Dateiformat — Kopf, Prüfsumme, Fußzeile, satzweises Streamen — liegt seit der
 * Herauslösung im Modul **M1.1 Sicherung** (`de.frank.module.sicherung`). Hier stehen nur noch
 * die Verträge, über die das Repository seine Sätze hinein- und herausreicht; die
 * Serialisierung selbst macht `de.frank.module.sicherung.KompassSicherungsInhalt`.
 *
 * Die Trennung folgt der Regel des Moduls: Der Umschlag gehört dem Modul, der Inhalt der App.
 * Eine fremde App kann das Modul deshalb benutzen, ohne die Tabellen von Kompass zu kennen.
 */
object Sicherung {

    /** So viele Sätze werden je Abfrage aus der Datenbank geholt. */
    const val SEITE = 200

    /**
     * Woher die Sätze kommen. Jede Seite wird geholt, geschrieben und wieder freigegeben.
     *
     * Weitergereicht wird die Kennung des zuletzt gelesenen Satzes, nicht die Zahl der schon
     * gelesenen: Kommt während des Laufs etwas dazu, verschiebt das sonst das Fenster.
     */
    interface Quelle {
        suspend fun eintraegeSeite(bereiche: List<String>, nachId: String): List<EintragEntity>
        suspend fun fragenSeite(nachId: Long): List<FrageEntity>
        suspend fun sitzungen(): List<ChatSitzungEntity>
        suspend fun nachrichten(sitzungId: Long): List<ChatNachrichtEntity>
    }

    /** Wohin die gelesenen Sätze gehen. Jeder kommt einzeln und wird sofort verarbeitet. */
    interface Senke {
        suspend fun eintrag(werte: Map<String, String>)
        suspend fun frage(eintragId: String, frage: String, antwort: String, erstelltAm: Long)
        suspend fun sitzung(titel: String, erstelltAm: Long, nachrichten: List<Triple<String, String, Long>>)
    }
}

/** Wie viele Sätze je Art gesichert würden — für die Anzeige vor dem Sichern. */
data class SicherungsAnzahl(
    val eintraege: Int = 0,
    val fragen: Int = 0,
    val sitzungen: Int = 0,
    val nachrichten: Int = 0,
)
