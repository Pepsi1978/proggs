package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.veraenderung.Veraenderung
import de.frank.jarvis.veraenderung.Veraenderungen

/**
 * Franks Veränderungen (eigener Reiter „Veränderungen“): wichtige Veränderungen im Leben mit Einzeile und genauer Beschreibung
 * samt Grund. Die aktuellen stehen im Live-Gedächtnis, damit jede Antwort sie berücksichtigt.
 */
class VeraenderungenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "veraenderungen"
    override val name = "Veränderungen"
    override val beschreibung = "Franks Veränderungen im Leben (Reiter „Veränderungen“): festhalten, abhaken, zurückholen, löschen."
    override val hinweise =
        "Sagt Frank „nimm unter Veränderungen auf …“, „halte als Veränderung fest …“ oder beschreibt er eine Veränderung, die er ab jetzt in seinem Leben umsetzt, " +
            "nimm veraenderung mit aktion anlegen, nicht jarvis_merken, ziel_speichern oder idee_speichern. titel und beschreibung formulierst du selbst. " +
            "Die aktuellen Veränderungen berücksichtigst du in allen Antworten. Erzählt Frank (auch im Tagebuch), wie es damit läuft, schätze ein, wie weit er ist, " +
            "und schlag passend den nächsten sinnvollen Schritt vor. Antworte nach dem Anlegen in einem kurzen Satz."

    override fun stoerung(): String? = null

    private fun zeile(v: Veraenderung, mitBeschreibung: Boolean) =
        "- ${v.titel} (seit ${Veraenderungen.datum(v.erstellt)}" + (if (v.erledigt) ", erledigt ${Veraenderungen.datum(v.erledigtAm)}" else "") + ")" +
            (if (mitBeschreibung && v.beschreibung.isNotBlank()) ": ${v.beschreibung}" else "") + " [id ${v.id}]"

    private fun finde(text: String): Pair<Veraenderung?, Ergebnis?> {
        val t = Veraenderungen.suche(context, text)
        return when {
            t.size == 1 -> t[0] to null
            t.isEmpty() -> null to Ergebnis("Keine Veränderung passt zu „$text“.", fehler = true)
            else -> null to Ergebnis("Mehrere Veränderungen passen zu „$text“: ${t.joinToString { "„${it.titel}“ [id ${it.id}]" }}. Frag Frank, welche gemeint ist, oder nimm die id.", fehler = true)
        }
    }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "veraenderungen_lesen",
            titel = "Veränderungen lesen",
            beschreibung = "Jarvis: liest Franks Veränderungen mit Beschreibung und Grund (aktuelle; mit erledigte auch die erledigten). Das [id …] nicht vorlesen.",
            schema = schema("erledigte" to schalter("Auch erledigte zeigen (Vorgabe: nein).")),
            nurLesen = true,
            taetigkeit = "schaut in die Veränderungen",
        ) { a ->
            val s = Veraenderungen.sicher(context)
            val aktuell = s.filter { !it.erledigt }
            val erledigt = s.filter { it.erledigt }
            Ergebnis(
                "AKTUELL:\n" + (aktuell.joinToString("\n") { zeile(it, true) }.ifEmpty { "(keine)" }) +
                    if (a.optBoolean("erledigte", false)) "\n\nERLEDIGT:\n" + (erledigt.joinToString("\n") { zeile(it, true) }.ifEmpty { "(keine)" }) else "\n\n${erledigt.size} erledigt."
            )
        },
        Werkzeug(
            name = "veraenderung",
            titel = "Veränderung",
            beschreibung = "Jarvis: hält eine Veränderung fest oder ändert sie. „Nimm unter Veränderungen auf, dass ich ab sofort mit dem Sonnenaufgang aufstehe, weil …“ → anlegen mit " +
                "titel (Einzeile, höchstens 7 Wörter, etwa „Mit dem Sonnenaufgang aufstehen“) und beschreibung (genau, was sich ändert und warum, in ganzen Sätzen über Frank in der dritten Person, nur aus Franks Worten, nichts erfinden). " +
                "„Die Veränderung … ist umgesetzt“ oder „verfolge ich nicht mehr“ → erledigt. „Hol … zurück“ → zurueck. „Lösch die Veränderung …“ → loeschen (nur auf ausdrücklichen Wunsch). " +
                "aendern: neuer titel oder beschreibung.",
            schema = schema(
                "aktion" to text("anlegen, erledigt, zurueck, aendern oder loeschen.", listOf("anlegen", "erledigt", "zurueck", "aendern", "loeschen")),
                "titel" to text("Beim Anlegen und Ändern: kurze Einzeile, höchstens 7 Wörter."),
                "beschreibung" to text("Beim Anlegen und Ändern: genaue Beschreibung der Veränderung samt Grund."),
                "veraenderung" to text("Bei erledigt, zurueck, aendern, loeschen: die vorhandene Veränderung (Titel oder id)."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a ->
            when (val aktion = a.optString("aktion")) {
                "anlegen" -> {
                    val titel = a.feld("titel")
                    if (titel.isEmpty()) Ergebnis("Bitte einen titel (Einzeile) mitgeben.", fehler = true)
                    else {
                        val v = Veraenderungen.anlegen(context, titel, a.feld("beschreibung"))
                        if (v == null) Ergebnis("Die Veränderung ließ sich nicht speichern.", fehler = true)
                        else Ergebnis("Veränderung „${v.titel}“ steht unter Veränderungen (aktuell). [id ${v.id}]")
                    }
                }
                "erledigt", "zurueck", "aendern", "loeschen" -> {
                    val (v, fehler) = finde(a.optString("veraenderung"))
                    val nichtDa = Ergebnis("Die Veränderung ließ sich nicht ändern: Sie wurde inzwischen gelöscht, oder das Speichern scheiterte.", fehler = true)
                    fehler ?: when (aktion) {
                        "erledigt" -> Veraenderungen.erledigt(context, v!!.id, true)?.let { Ergebnis("„${it.titel}“ ist erledigt und liegt unter „Erledigt“.") } ?: nichtDa
                        "zurueck" -> Veraenderungen.erledigt(context, v!!.id, false)?.let { Ergebnis("„${it.titel}“ ist wieder aktuell.") } ?: nichtDa
                        "loeschen" -> Veraenderungen.loeschen(context, v!!.id)?.let { Ergebnis("„${it.titel}“ ist gelöscht.") } ?: nichtDa
                        else -> if (a.feld("titel").isEmpty() && a.feld("beschreibung").isEmpty()) Ergebnis("Zum Ändern einen neuen titel oder eine neue beschreibung mitgeben.", fehler = true)
                        else Veraenderungen.aendern(context, v!!.id) {
                            it.copy(titel = a.feld("titel").ifEmpty { it.titel }, beschreibung = a.feld("beschreibung").ifEmpty { it.beschreibung })
                        }?.let { Ergebnis("„${it.titel}“ ist geändert.") } ?: nichtDa
                    }
                }
                else -> Ergebnis("Unbekannte aktion „$aktion“.", fehler = true)
            }
        },
    )

    /** Textfeld ohne JSON-null (optString gäbe sonst „null“). */
    private fun org.json.JSONObject.feld(n: String): String = if (has(n) && !isNull(n)) optString(n).trim() else ""

    /** Live-Gedächtnis: aktuelle Veränderungen mit Grund, dazu die Zahl der erledigten. */
    fun gedaechtnis(): String {
        val s = Veraenderungen.sicher(context)
        val aktuell = s.filter { !it.erledigt }
        val erledigt = s.filter { it.erledigt }.sortedByDescending { it.erledigtAm }
        if (s.isEmpty()) return "Noch keine Veränderungen festgehalten."
        return (if (aktuell.isEmpty()) "Keine aktuellen Veränderungen." else aktuell.joinToString("\n") { zeile(it, true).substringBeforeLast(" [id ") }) +
            (if (erledigt.isEmpty()) "" else "\nErledigt (umgesetzt oder nicht mehr verfolgt): " + erledigt.take(10).joinToString("; ") { it.titel } + if (erledigt.size > 10) " …" else "")
    }
}
