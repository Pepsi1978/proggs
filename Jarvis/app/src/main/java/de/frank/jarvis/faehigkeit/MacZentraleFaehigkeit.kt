package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.ui.MacUpdateZentrale
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Mac-Zentrale → Programmier-CLI per Sprache: Schalter „Automatisches Aufräumen“ und Intervall (1–24 ganze Stunden) lesen und ändern.
 * Gespeichert wird am Mac (Mac-Brücke, cli-aufraeumen.json); Handy und Tablet zeigen denselben Stand. Das Aufräumen selbst startet nur
 * auf der Karte nach Franks Ja.
 */
class MacZentraleFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "mac_zentrale"
    override val name = "Mac-Zentrale"
    override val beschreibung = "Automatisches Aufräumen der Programmier-CLI-Sitzungen auf Franks MacBook: Schalter und Intervall."
    override val hinweise = "„Schalte das automatische CLI-Aufräumen ein“, „Räum alle 6 Stunden auf“ → cli_aufraeumen_einstellen. " +
        "Das Intervall gibt es nur in ganzen Stunden von 1 bis 24; die Grenze von mehr als einer Stunde Inaktivität ändert sich nie."

    override fun stoerung(): String? = null

    private fun stand(): String {
        val c = MacUpdateZentrale.lage.value.cli ?: return "Vom MacBook liegt noch kein Stand vor."
        val an = c.optBoolean("automatik")
        return "Automatisches Aufräumen " + (if (an) "an, alle ${c.optInt("stunden")} Stunden" else "aus (Intervall ${c.optInt("stunden")} Stunden)") +
            ". " + (c.optJSONObject("letzter")?.optString("text")?.let { "Letztes Aufräumen: $it" } ?: "")
    }

    override val werkzeuge = listOf(
        Werkzeug(
            name = "cli_aufraeumen_einstellen",
            titel = "CLI-Aufräumen einstellen",
            beschreibung = "Liest oder ändert am MacBook das automatische Aufräumen der Programmier-CLI-Sitzungen (beendet nur Sitzungen, die " +
                "eindeutig seit mehr als einer Stunde inaktiv sind). automatik true/false schaltet es, stunden (ganze Zahl 1–24) legt fest, wie oft. " +
                "Ohne Angaben nur lesen.",
            schema = JSONObject().put("type", "object").put("properties", JSONObject()
                .put("automatik", JSONObject().put("type", "boolean"))
                .put("stunden", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 24))),
            nurLesen = false,
            taetigkeit = "stellt das CLI-Aufräumen am Mac ein",
        ) { a ->
            MacUpdateZentrale.holen(context)
            val zusatz = JSONObject()
            if (a.has("automatik")) {
                val wert = a.opt("automatik")
                if (wert !is Boolean) return@Werkzeug Ergebnis("automatik muss true oder false sein.", fehler = true)
                zusatz.put("automatik", wert)
            }
            if (a.has("stunden")) {
                val wert = a.opt("stunden")
                val zahl = (wert as? Number)?.toDouble()
                if (zahl == null || zahl % 1.0 != 0.0 || zahl < 1 || zahl > 24)
                    return@Werkzeug Ergebnis("Das Intervall gibt es nur in ganzen Stunden von 1 bis 24. Nichts geändert.", fehler = true)
                zusatz.put("stunden", zahl.toInt())
            }
            if (zusatz.length() == 0) return@Werkzeug Ergebnis(stand())
            val grund = MacUpdateZentrale.auftrag(context, "cli_einstellung", zusatz)
            if (grund != null) return@Werkzeug Ergebnis(grund.replace("Es wurde nichts gestartet.", "Nichts geändert."), fehler = true)
            // Der Mac speichert und meldet den neuen Stand; erst der bestätigt die Änderung.
            repeat(5) {
                delay(1_500)
                MacUpdateZentrale.holen(context)
                val c = MacUpdateZentrale.lage.value.cli
                val passt = c != null && (!zusatz.has("automatik") || c.optBoolean("automatik") == zusatz.getBoolean("automatik")) &&
                    (!zusatz.has("stunden") || c.optInt("stunden") == zusatz.getInt("stunden"))
                if (passt) return@Werkzeug Ergebnis("Gespeichert. " + stand())
            }
            Ergebnis("Der Auftrag ging an den Mac, die Bestätigung des neuen Stands fehlt noch. " + stand())
        },
    )
}
