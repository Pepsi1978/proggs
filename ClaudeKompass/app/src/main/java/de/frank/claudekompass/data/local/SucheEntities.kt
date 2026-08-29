package de.frank.claudekompass.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Der Volltext-Index über alles, was die App gespeichert hat (Baustein K).
 *
 * Bewusst eine eigenständige FTS4-Tabelle statt einer `contentEntity`-Spiegelung: Die
 * gespiegelte Form verlangt einen ganzzahligen Primärschlüssel in der Quelltabelle, und die
 * Einträge werden über eine sprechende Zeichenkette identifiziert (`slash:/compact`). Außerdem
 * fließen hier drei verschiedene Quellen zusammen — Einträge, eigene Fragen und Gespräche —,
 * die es als eine gespiegelte Tabelle gar nicht geben könnte.
 *
 * [suchtext] ist die normalisierte Fassung (klein, Umlaute aufgelöst). Gesucht wird gezielt in
 * dieser Spalte, damit „Uber", „über" und „über" dasselbe finden. Für die Anzeige der
 * Fundstelle wird der Originaltext über [quelleId] nachgeladen.
 */
@Fts4
@Entity(tableName = "suche_fts")
data class SucheFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long = 0,
    /** Kennung in der Quelltabelle: Eintrags-ID, Frage-ID oder Nachrichten-ID. */
    val quelleId: String,
    /** `eintrag`, `frage` oder `chat` — bestimmt, wohin ein Treffer springt. */
    val quelleArt: String,
    /** `slash`, `config`, `praxis` oder `chat`; für die Gruppierung der Trefferliste. */
    val bereich: String,
    /** Was in der Trefferzeile fett steht. */
    val titel: String,
    /** Normalisierter Text; hierin wird gesucht. */
    val suchtext: String,
)

/** Die letzten Suchanfragen, damit man sie nicht erneut tippen muss (Baustein K). */
@Entity(tableName = "such_verlauf")
data class SuchVerlaufEntity(
    @PrimaryKey val anfrage: String,
    val zuletztAm: Long = System.currentTimeMillis(),
)
