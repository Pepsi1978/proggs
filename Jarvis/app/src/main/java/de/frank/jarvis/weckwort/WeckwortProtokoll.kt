package de.frank.jarvis.weckwort

import android.content.Context
import java.io.File
import org.json.JSONObject

/**
 * Dauerhaftes Protokoll jedes Weckwort-Treffers, damit sich die Strenge nach ein paar Tagen an echten Werten
 * nachjustieren lässt (Logcat ist dafür zu flüchtig). Eine JSON-Zeile je Ereignis in `files/weckwort_protokoll.jsonl`:
 *
 * - `treffer`: Weckwort erkannt; mit Modellwert (nur „Hey Jarvis“), Ähnlichkeit, Schwelle und nächstem Fehlalarm
 *   aus dem [Stimmprofil] und dem Urteil (`gerufen` oder `abgewiesen`).
 * - `ausgang`: was nach dem Rufen geschah: `frage` (Frank hat gesprochen) oder `nichts` (Fehlalarm).
 *
 * Lesen am PC (Debug-Build): `adb shell run-as de.frank.jarvis cat files/weckwort_protokoll.jsonl`.
 */
object WeckwortProtokoll {
    private const val DATEI = "weckwort_protokoll.jsonl"
    private const val MAX_BYTES = 512 * 1024L
    private const val BEHALTEN = 1500

    fun treffer(
        context: Context, art: String, modellwert: Float?, pruefung: Stimmprofil.Pruefung?, urteil: String,
        stimme: Stimmprofil.StimmPruefung? = null, mitStimme: Boolean = false, variante: String? = null,
    ) {
        val j = JSONObject().put("ereignis", "treffer").put("art", art).put("urteil", urteil)
        modellwert?.let { j.put("modell", runde(it)) }
        // Welche Aussprache-Variante der Phrase gegriffen hat (Tokens), zeigt, wie Frank sie wirklich spricht.
        variante?.takeIf { it.isNotEmpty() }?.let { j.put("variante", it) }
        if (pruefung != null) j.put("aehnlich", runde(pruefung.aehnlich)).put("schwelle", runde(pruefung.schwelle)).put("fehlalarm", runde(pruefung.fremd))
        else j.put("profil", false)
        // Stimmerkennung: Ähnlichkeit des Stimm-Fingerabdrucks zu Franks Mitte und ihre Schwelle (sie entscheidet).
        if (stimme != null) j.put("stimme", runde(stimme.aehnlich)).put("stimmSchwelle", runde(stimme.schwelle))
        else if (mitStimme) j.put("stimme", "zu wenig Sprache")
        schreibe(context, j)
    }

    fun ausgang(context: Context, ausgang: String) = schreibe(context, JSONObject().put("ereignis", "ausgang").put("ausgang", ausgang))

    @Synchronized
    private fun schreibe(context: Context, j: JSONObject) {
        runCatching {
            val d = File(context.applicationContext.filesDir, DATEI)
            j.put("zeit", java.time.LocalDateTime.now().withNano(0).toString())
            d.appendText(j.toString() + "\n")
            // Nicht endlos wachsen: die neuesten Einträge behalten.
            if (d.length() > MAX_BYTES) d.writeText(d.readLines().takeLast(BEHALTEN).joinToString("\n", postfix = "\n"))
        }
    }

    private fun runde(f: Float) = Math.round(f * 1000) / 1000.0
}
