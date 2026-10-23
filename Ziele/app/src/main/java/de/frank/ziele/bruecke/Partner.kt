package de.frank.ziele.bruecke

import android.content.Context
import android.net.Uri
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import org.json.JSONArray
import org.json.JSONObject

/** Geniale Aufgaben (de.frank.aufgaben): Vorschläge übernehmen und ihren Stand abgleichen, über deren Jarvis-Brücke. */
object GenialeAufgaben {
    private val ADRESSE: Uri = Uri.parse("content://de.frank.aufgaben.jarvis")
    const val PAKET = "de.frank.aufgaben"

    class Fehler(text: String) : Exception(text)

    /** Legt den Vorschlag als Aufgabe an und liefert deren id. Notwendige mit Priorität Mittel, unterstützende Gering. */
    suspend fun uebernehmen(context: Context, ziel: Ziel, v: Vorschlag): Long {
        val text = buildString {
            if (v.text.isNotBlank()) append(v.text.trim())
            if (v.erfolg.isNotBlank()) append(if (isEmpty()) "" else "\n\n").append("Erledigt, wenn: ").append(v.erfolg.trim())
            append(if (isEmpty()) "" else "\n\n").append("Für dein Ziel „").append(ziel.titel).append("“")
            if (v.begruendung.isNotBlank()) append(": ").append(v.begruendung.trim())
        }
        val anfrage = JSONObject()
            .put("titel", v.titel)
            .put("text", text)
            .put("prioritaet", if (v.gruppe == Kategorie.NOTWENDIG) (if (v.wichtigkeit >= 4) "hoch" else "mittel") else "gering")
            .put("dauer", v.minuten.coerceIn(5, 24 * 60))
            .put("vorlesen", true)
        val antwort = AppBruecke.rufe(context, "Geniale Aufgaben", ADRESSE, "anlegen", anfrage, lesend = false)
        antwort.optString("fehler").takeIf { it.isNotBlank() }?.let { throw Fehler(it) }
        return antwort.optJSONObject("aufgabe")?.optLong("id", -1)?.takeIf { it >= 0 } ?: throw Fehler("Geniale Aufgaben hat keine Aufgabe zurückgemeldet.")
    }

    /** Stand aller Aufgaben: id → erledigt. Fehlt eine id, wurde die Aufgabe dort gelöscht. null = nicht erreichbar. */
    suspend fun stand(context: Context): Map<Long, Boolean>? {
        val antwort = AppBruecke.rufe(context, "Geniale Aufgaben", ADRESSE, "lesen", JSONObject().put("bereich", "alle").put("limit", 200), lesend = true)
        if (antwort.has("fehler")) return null
        val liste = antwort.optJSONArray("aufgaben") ?: JSONArray()
        return (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }.associate { it.optLong("id") to it.optBoolean("erledigt") }
    }
}

/** Jarvis (de.frank.jarvis): sein Live-Gedächtnis als Kontext für die Auswertung. */
object Jarvis {
    private val ADRESSE: Uri = Uri.parse("content://de.frank.jarvis.ziele")
    const val PAKET = "de.frank.jarvis"

    /** Kurzer Startkontext aus allen angebundenen Apps (Regeln, Ziele, Wissen über Frank, Tageslage). null = nicht erreichbar. */
    suspend fun gedaechtnis(context: Context): String? {
        val antwort = AppBruecke.rufe(context, "Jarvis", ADRESSE, "kontext", JSONObject().put("warte_ms", 4_000), lesend = true)
        return antwort.optString("kontext").takeIf { it.isNotBlank() && !antwort.has("fehler") }
    }

    /** Schlüssel aus Jarvis übernehmen (Groq, Anthropic, Vorlese-Stimmen), damit Frank nichts doppelt eintippen muss. */
    suspend fun zugang(context: Context): JSONObject {
        val antwort = AppBruecke.rufe(context, "Jarvis", ADRESSE, "zugang", JSONObject(), lesend = true)
        antwort.optString("fehler").takeIf { it.isNotBlank() }?.let { throw GenialeAufgaben.Fehler(it) }
        return antwort
    }
}
