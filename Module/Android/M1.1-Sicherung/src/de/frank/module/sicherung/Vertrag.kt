// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v1
// Quelle: Module/Android/M1.1-Sicherung/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.1 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sicherung

import android.util.JsonReader
import android.util.JsonWriter

/**
 * Was die App liefern muss, damit das Modul sichern kann.
 *
 * Die Aufteilung ist überall dieselbe: Das Modul besitzt den **Umschlag** — Kopf, Prüfsumme,
 * Fußzeile, das satzweise Schreiben und Lesen, die Ordnerverwaltung, die Autosicherung und die
 * Oberfläche. Die App besitzt den **Inhalt** — welche Teile es gibt und wie ein einzelner Satz
 * aussieht.
 *
 * Ohne diese Grenze müsste das Modul die Tabellen der App kennen, und dann wäre es keines mehr.
 */

/** Ein anhakbarer Teil der Sicherung. Die App zählt auf, was sie zu sichern hat. */
interface SicherungsTeil {
    /** Kurz und dauerhaft — steht so in der Datei und darf sich nie ändern. */
    val id: String

    /** Was im Auswahlmenü steht. */
    val titel: String

    /** Ein Satz darunter, der erklärt, was dieser Teil umfasst. */
    val erklaerung: String
}

/**
 * Der Inhalt der Sicherung — alles, was nur die App weiß.
 *
 * Schreiben und Lesen laufen **satzweise**: Es steht nie der gesamte Bestand gleichzeitig im
 * Speicher. Jeder geschriebene Wert wird zusätzlich in [Inhaltspruefsumme] gegeben, damit eine
 * abgebrochene Übertragung vor dem Einspielen auffällt und nicht mittendrin.
 */
interface SicherungsInhalt {
    /** Alles, was diese App sichern kann. Die Reihenfolge ist die Reihenfolge im Menü. */
    val teile: List<SicherungsTeil>

    /**
     * Die Fassung des Datenmodells dieser App. Steht im Kopf der Datei, damit beim Einspielen
     * erkennbar ist, aus welcher Zeit sie stammt.
     */
    val datenmodellVersion: Int

    /**
     * Schreibt alle Sätze von [teil] nach [ziel] und gibt zurück, wie viele es waren.
     *
     * Das Modul hat den umgebenden JSON-Aufbau bereits geöffnet; hier kommt nur die Nutzlast
     * hinein. Jeder geschriebene Wert gehört über [pruefsumme] mitgerechnet.
     */
    suspend fun schreibe(teil: SicherungsTeil, ziel: JsonWriter, pruefsumme: Inhaltspruefsumme): Int

    /**
     * Liest die Sätze von [teil] aus [quelle] und legt sie an. Zurück kommt die Anzahl.
     *
     * **Ergänzen, nicht überschreiben:** Was schon vorhanden ist, bleibt unverändert. Diese
     * Entscheidung gehört der App, weil nur sie weiß, wann zwei Sätze derselbe sind.
     */
    suspend fun lies(teil: SicherungsTeil, quelle: JsonReader, pruefsumme: Inhaltspruefsumme): Int
}

/**
 * Die Kennung eines Einspielvorgangs.
 *
 * Das Modul reicht sie nur durch — was darin steht, weiß allein die App. Sie braucht sie, um ein
 * Einspielen wieder zurücknehmen zu können, ohne dabei Sätze zu treffen, die schon vorher da
 * waren.
 */
interface Einspielspur

/**
 * Das Zurücknehmen eines Einspielvorgangs.
 *
 * Getrennt von [SicherungsInhalt], weil eine App das Modul auch ohne Rücknahme benutzen kann —
 * dann wird hier nichts geliefert und der entsprechende Knopf bleibt weg.
 */
interface SicherungsRuecknahme {
    suspend fun beginne(): Einspielspur
    suspend fun schliesseAb(spur: Einspielspur)

    /** Nimmt zurück, was unter [spur] eingespielt wurde. Zurück kommt die Zahl der Sätze. */
    suspend fun nimmZurueck(spur: Einspielspur): Int
}

/**
 * Wohin das Modul meldet.
 *
 * Vorbelegt mit [StillesProtokoll], damit eine App ohne eigenes Protokoll nichts liefern muss —
 * ein Modul soll nicht daran scheitern, dass niemand zuhört.
 */
interface SicherungsProtokoll {
    fun info(stelle: String, was: String, meldung: String, felder: Map<String, Any?> = emptyMap()) {}
    fun warn(stelle: String, was: String, meldung: String, felder: Map<String, Any?> = emptyMap()) {}
}

/** Meldet nichts. Der Vorgabewert. */
object StillesProtokoll : SicherungsProtokoll

/**
 * Wie die Sicherungsdateien dieser App heißen.
 *
 * [fruehere] nennt Präfixe aus früheren Fassungen: Dateien, die noch so heißen, werden beim
 * Aufräumen weiter erkannt und nicht als fremd übergangen.
 */
data class SicherungsNamen(
    val dateiPraefix: String,
    val fruehere: List<String> = emptyList(),
    /**
     * Die `SharedPreferences`-Datei, in der der gewählte Ordner steht — und der Schlüssel darin.
     *
     * **Bei einer bestehenden App unverändert übernehmen.** Ändert sich einer der beiden Namen,
     * findet die App den bisher eingestellten Sicherungsordner nicht mehr, und der Benutzer
     * steht ohne Sicherung da, ohne dass ihm etwas angezeigt wird. Nur bei einer App, die das
     * Modul zum ersten Mal bekommt, darf hier frei benannt werden.
     */
    val einstellungenDatei: String = "sicherung_status",
    val ordnerSchluessel: String = "sicherungs_ordner",
)
