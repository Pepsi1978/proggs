package de.frank.jarvis.data

import android.content.Context
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.faehigkeit.StilRegeln
import org.json.JSONObject
import java.io.File

/**
 * Franks Daumen-Bewertungen der Antworten (`files/stil/bewertungen.jsonl`, eine Zeile je Bewertung) und die Auswertung
 * daraus: Alle paar Bewertungen leitet ein Modell Stilregeln ab und schreibt sie in [StilRegeln] („Stil“ im Gedächtnis),
 * wo Frank sie lesen und bearbeiten kann. Gelöscht wird dabei nie etwas; geändert nur, was dem Neuen widerspricht.
 */
object Stil {
    /** Gründe für einen Daumen runter; „falsch“ betrifft den Inhalt und zählt nicht zum Stil. */
    val GRUENDE = listOf("zu_lang" to "Zu lang", "zu_kurz" to "Zu kurz", "ton" to "Ton passt nicht", "unuebersichtlich" to "Unübersichtlich", "falsch" to "Inhaltlich falsch")

    /** Jede Bewertung wird gleich ausgewertet: Der Stil der bewerteten Antwort fließt sofort in die Stil-Datei. */
    const val AB = 1
    private const val HOECHSTENS_REGELN = 20
    /** Eine Regel darf bis zu etwa zehn Zeilen lang sein. */
    private const val REGEL_ZEICHEN = 900

    private fun ordner(context: Context) = File(context.filesDir, "stil").apply { mkdirs() }
    private fun datei(context: Context) = File(ordner(context), "bewertungen.jsonl")
    private fun stand(context: Context) = File(ordner(context), "ausgewertet.txt")

    fun anzahl(context: Context): Int = runCatching { datei(context).readLines().count { it.isNotBlank() } }.getOrDefault(0)
    fun ausgewertetBis(context: Context): Int = runCatching { stand(context).readText().trim().toInt() }.getOrDefault(0)
    fun neueSeitAuswertung(context: Context): Int = (anzahl(context) - ausgewertetBis(context)).coerceAtLeast(0)

    /** [urteil] +1 oder -1; [grund] eine Kennung aus [GRUENDE] oder leer. */
    @Synchronized
    fun bewerten(context: Context, urteil: Int, grund: String, frage: String, antwort: String, modell: CodexModel, denkstufe: ReasoningEffort) {
        val zeile = JSONObject()
            .put("zeit", System.currentTimeMillis())
            .put("urteil", if (urteil > 0) "gut" else "schlecht")
            .put("grund", grund)
            .put("frage", frage.take(1500))
            .put("antwort", antwort.take(4000))
            .put("laenge", antwort.length)
            .put("modell", modell.apiId)
            .put("denkstufe", denkstufe.apiValue)
        datei(context).appendText(zeile.toString() + "\n")
    }

    /**
     * Leitet aus den letzten Bewertungen Stilregeln ab und trägt sie in die Stil-Datei ein. Liefert eine kurze Meldung,
     * was sich geändert hat, oder null, wenn es zu wenige Bewertungen gibt oder nichts herauskam.
     */
    suspend fun auswerten(context: Context, auth: CodexAuthManager, modell: CodexModel): String? {
        val zeilen = runCatching { datei(context).readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())
        if (zeilen.size < AB) return null
        val beispiele = zeilen.takeLast(20).mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
            .filter { it.optString("grund") != "falsch" }
            .joinToString("\n\n") { j ->
                "[" + j.optString("urteil").uppercase() + (j.optString("grund").takeIf { it.isNotEmpty() }?.let { ", Grund: " + GRUENDE.firstOrNull { g -> g.first == it }?.second } ?: "") +
                    ", ${j.optInt("laenge")} Zeichen]\nFRAGE: " + j.optString("frage").take(400) + "\nANTWORT: " + j.optString("antwort").take(1500)
            }
        val vorhanden = StilRegeln.alle(context)
        val auftrag = "Frank hat Antworten seines Assistenten Jarvis mit Daumen hoch (GUT) oder runter (SCHLECHT) bewertet; die neueste Bewertung steht zuletzt. " +
            "Beschreibe kurz und knapp, WIE Jarvis antworten soll, damit Frank es mag, und zwar beides: " +
            "den optischen Stil (Länge, Absätze, Stichpunkte, Überschriften, Fettdruck, Symbole und wo sie stehen, Reihenfolge) und den inhaltlichen Stil " +
            "(Art und Weise: wie direkt, wie ausführlich erklärt, mit Empfehlung oder ohne, Einordnung, Beispiele, Zahlen, Ton). " +
            "Bei GUT: was genau an dieser Antwort gut war, als Regel für ähnliche Fragen. Bei SCHLECHT: was künftig anders sein soll. Keine Fakten über Frank, keine Themen-Inhalte.\n" +
            "Jede Regel ist kurz und knapp, sagt aber genug, dass Jarvis sie ohne weiteres Wissen anwenden kann (höchstens etwa zehn Zeilen, meist deutlich weniger). " +
            "Wenn sinnvoll, nenne, wofür sie gilt („Bei Statusmeldungen …“, „Bei Fragen zu Biowerten …“). Auf Deutsch mit echten Umlauten. " +
            "Gehört das Neue zu einer vorhandenen Regel oder präzisiert sie, ändere diese über aendern, statt eine zweite anzulegen.\n" +
            "VORHANDENE STILREGELN (mit id; teils von Frank selbst geschrieben, die änderst du nur, wenn die Bewertungen klar dagegen sprechen):\n" +
            (if (vorhanden.isEmpty()) "(keine)" else vorhanden.joinToString("\n") { "[${it.id}] ${it.text}" }) + "\n\n" +
            "BEWERTUNGEN:\n$beispiele\n\n" +
            "Antworte mit genau einem JSON-Objekt und sonst nichts: {\"neu\": [\"Regel\", …], \"aendern\": [{\"id\": 1, \"text\": \"neuer Text\"}], \"fazit\": \"ein Satz, was du erkannt hast\"}. " +
            "Höchstens ${HOECHSTENS_REGELN} Regeln insgesamt (vorhandene mitgezählt), nichts doppelt zu Vorhandenem. Gibt es nichts Neues, sind beide Listen leer."
        val roh = kotlinx.coroutines.withTimeoutOrNull(60_000) {
            runCatching { auth.streamChat("Du wertest Bewertungen aus und schreibst knappe Stilregeln.", listOf(ChatTurn("user", auftrag)), modell, ReasoningEffort.LOW) }.getOrNull()
        } ?: return null
        val j = runCatching { JSONObject(roh.substring(roh.indexOf('{'), roh.lastIndexOf('}') + 1)) }.getOrNull() ?: return null
        var neu = 0
        var geaendert = 0
        j.optJSONArray("aendern")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val id = o.optInt("id", -1)
                val text = o.optString("text").trim()
                if (id >= 0 && text.isNotEmpty() && vorhanden.any { it.id == id } && StilRegeln.speichere(context, text.take(REGEL_ZEICHEN), id) != null) geaendert++
            }
        }
        j.optJSONArray("neu")?.let { a ->
            for (i in 0 until a.length()) {
                val text = a.optString(i).trim()
                if (text.isEmpty() || StilRegeln.alle(context).size >= HOECHSTENS_REGELN || StilRegeln.voll(context)) continue
                if (StilRegeln.speichere(context, text.take(REGEL_ZEICHEN)) != null) neu++
            }
        }
        stand(context).writeText(zeilen.size.toString())
        val fazit = j.optString("fazit").trim()
        return when {
            neu == 0 && geaendert == 0 -> "Stil ausgewertet: nichts Neues." + (if (fazit.isNotEmpty()) " $fazit" else "")
            else -> "Stil ausgewertet: $neu neu, $geaendert angepasst." + (if (fazit.isNotEmpty()) " $fazit" else "")
        }
    }
}
