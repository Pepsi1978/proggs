// ──────────────────────────────────────────────────────────────────────
// Modul M1.1 — Sicherung · Stand v3
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

    /** Der Produktname, wie er im Kopf der Datei steht. */
    val produkt: String

    /**
     * Frühere Produktnamen dieser App.
     *
     * Eine Umbenennung darf keine Sicherung entwerten: Eine unter altem Namen geschriebene
     * Datei gehört weiterhin zu dieser App und muss einspielbar bleiben.
     */
    val fruehereNamen: List<String> get() = emptyList()

    /**
     * Frühere Namen von Kopf-Feldern dieser App: alter Name → heutiger Name.
     *
     * Eine App, die vor dem Modul schon gesichert hat, hat ihren Kopf womöglich anders benannt —
     * etwa `schemaVersion` statt `schema`. Ohne diese Zuordnung liefe ein solches Feld in die
     * Nutzlast, der Kopf bliebe leer und die Datei würde als „ohne Schema-Angabe" abgelehnt: Alle
     * bis dahin geschriebenen Sicherungen wären auf einen Schlag wertlos.
     *
     * **Das Modul kennt die Vergangenheit keiner App** — es nimmt hier nur entgegen, was die
     * jeweilige App über ihre eigene mitteilt. Vorbelegt mit „nichts umzubenennen".
     */
    val kopfAliase: Map<String, String> get() = emptyMap()

    /**
     * Die Fassung des Datenmodells dieser App. Steht im Kopf der Datei, damit beim Einspielen
     * erkennbar ist, aus welcher Zeit sie stammt. Eine Datei aus einer neueren Fassung wird
     * abgelehnt — sie könnte Felder mitbringen, die hier niemand einordnen kann.
     */
    val datenmodellVersion: Int

    /**
     * Schreibt die Nutzlast — die benannten Felder zwischen Kopf und Fußzeile.
     *
     * Das Modul hat das umgebende Objekt geöffnet und den Kopf geschrieben. Hier kommen die
     * eigenen Felder der App hinein, etwa `eintraege`, `fragen`, `sitzungen`. **Die Feldnamen
     * bestimmt die App** — dadurch bleibt eine bestehende Datei Byte für Byte wie bisher.
     *
     * Jeder geschriebene Wert gehört über [pruefsumme] mitgerechnet, sonst schlägt die
     * Vollständigkeitsprüfung beim Einspielen fehl.
     */
    suspend fun schreibeNutzlast(
        schreiber: JsonWriter,
        umfang: Set<SicherungsTeil>,
        pruefsumme: Inhaltspruefsumme,
    ): Nutzlastzahlen

    /**
     * Liest ein Nutzlast-Feld, das das Modul nicht kennt.
     *
     * Kommt `null` zurück, überspringt das Modul das Feld — so bleibt eine Datei aus einer
     * neueren Fassung lesbar, statt an einem unbekannten Feld zu scheitern.
     *
     * Ist [einspielen] falsch, wird nur gezählt und geprüft (die Vorschau). Sonst werden die
     * Sätze angelegt — **ergänzend**: Was schon da ist, bleibt unverändert. Diese Entscheidung
     * gehört der App, weil nur sie weiß, wann zwei Sätze derselbe sind.
     */
    suspend fun liesNutzlast(
        feld: String,
        leser: JsonReader,
        pruefsumme: Inhaltspruefsumme,
        einspielen: Boolean,
    ): Nutzlastzahlen?

    /**
     * Wie viele Sätze [umfang] umfassen würde — **ohne** zu schreiben.
     *
     * Damit zeigt die Oberfläche vorab an, was die nächste Sicherung enthält. Eine Zählung ist
     * billig, ein Probelauf wäre es nicht.
     */
    suspend fun zaehle(umfang: Set<SicherungsTeil>): Nutzlastzahlen

    /**
     * Wie groß die Datei daraus etwa wird, in Bytes.
     *
     * Ein Erfahrungswert je Satzart, keine Rechnung. Die Angabe soll die Größenordnung zeigen —
     * „ein paar Kilobyte" gegen „über ein Megabyte" —, nicht auf das Byte genau sein. Wie
     * schwer ein Satz wiegt, weiß nur die App.
     */
    fun schaetzeGroesse(zahlen: Nutzlastzahlen): Long

    /** Ein Satz für die Oberfläche: was in dieser Datei steckt. */
    fun fasseZusammen(vorschau: SicherungsVorschau): String
}

/**
 * Was beim Schreiben oder Lesen der Nutzlast zusammengekommen ist.
 *
 * Beides sind Zuordnungen statt fester Felder, weil die Schlüssel von App zu App verschieden
 * sind. In der Datei stehen sie unverändert unter `anzahl` und `jeBereich` — genau wie bisher.
 */
data class Nutzlastzahlen(
    val anzahl: Map<String, Int> = emptyMap(),
    val jeBereich: Map<String, Int> = emptyMap(),
) {
    operator fun plus(weitere: Nutzlastzahlen) = Nutzlastzahlen(
        anzahl = verschmelze(anzahl, weitere.anzahl),
        jeBereich = verschmelze(jeBereich, weitere.jeBereich),
    )

    private fun verschmelze(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> =
        buildMap {
            putAll(a)
            b.forEach { (schluessel, wert) -> put(schluessel, (get(schluessel) ?: 0) + wert) }
        }
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
