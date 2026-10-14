package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.gedaechtnis.Gespraechsgedaechtnis
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Das tiefe Gedächtnis: Das Live-Gedächtnis hält die letzten 10 Tage vollständig und Älteres verdichtet. Für gezielte
 * Fragen („was habe ich im Frühjahr über … geschrieben?“, „wann hatten wir über … gesprochen?“) sucht
 * gedaechtnis_suchen über alles, was Jarvis je gesehen hat: Tagebuch (jeder Eintrag), verdichtete Monate, Gespräche
 * mit Jarvis, Gesprächsnotizen, Gemerktes, Ideen (auch umgesetzte), Ablage und das Archiv der Tagesauswertungen.
 * Alles lokal auf dem Handy, deshalb schnell.
 */
class GedaechtnisFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "gedaechtnis"
    override val name = "Gedächtnis-Suche"
    override val beschreibung = "Gezielt im ganzen Gedächtnis von Jarvis suchen (Tagebuch, Gespräche, Ideen, Ablage, frühere Tagesauswertungen) und das Logbuch der App lesen."
    override val hinweise =
        "Das Gesamtbild enthält die letzten 10 Tage vollständig und Älteres nur verdichtet. Fragt Frank gezielt nach etwas Älterem oder einem Thema über längere Zeit, " +
            "suche mit gedaechtnis_suchen (ein bis drei treffende Wörter, bei Bedarf mit Zeitraum oder nur in bestimmten Quellen) und lies Treffer danach bei Bedarf im Volltext " +
            "mit tagebuch_lesen, ideen_lesen oder ablage_lesen. Sag ehrlich, wenn nichts gefunden wurde."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "gedaechtnis_suchen",
            titel = "Im Gedächtnis suchen",
            beschreibung = "Jarvis: sucht gezielt im gesamten Gedächtnis von Jarvis, auch weit zurück: Tagebuch (alle Einträge und verdichtete Monate), Gespräche mit Jarvis, " +
                "Gesprächsnotizen, Gemerktes (Regeln, Ziele, Wissen über Frank), Geniale Ideen (auch umgesetzte), Ablage und frühere Tagesauswertungen. Liefert je Treffer Datum und Ausschnitt, neueste zuerst. " +
                "Nutze es für Fragen nach Älterem oder nach einem Thema („Wann war ich zuletzt angeln?“, „Was hatte ich zu meinem Schwindel geschrieben?“, „Haben wir schon mal über den RAV4 gesprochen?“).",
            schema = schema(
                "suche" to text("Ein bis drei Wörter, die alle vorkommen müssen (Groß- und Kleinschreibung egal). In Anführungszeichen = genau diese Wortgruppe."),
                "quellen" to textListe("Nur diese Quellen: tagebuch, gespraeche, notizen, gemerkt, ideen, ablage, auswertungen. Weglassen = alle."),
                "von" to text("Optional: frühester Tag JJJJ-MM-TT."),
                "bis" to text("Optional: spätester Tag JJJJ-MM-TT."),
                "hoechstens" to zahl("Höchstzahl Treffer je Quelle (Vorgabe 8, höchstens 30)."),
                pflicht = listOf("suche"),
            ),
            nurLesen = true,
        ) { a -> withContext(Dispatchers.IO) { suche(a) } },
        Werkzeug(
            name = "logbuch_lesen",
            titel = "Logbuch lesen",
            beschreibung = "Jarvis: liest das Logbuch der Jarvis-App: was Jarvis getan hat, Fehler, Abstürze, wie lange Abläufe dauerten (Antworten, Werkzeuge, Gedächtnis), Verbindung und Sprache. " +
                "Mit Zusammenfassung (Anzahl je Art, häufigste Fehler, langsamste Abläufe). Nutze es bei „Warum hat das nicht geklappt?“, „Was ist schiefgelaufen?“ oder um Jarvis zu verbessern.",
            schema = schema(
                "tage" to zahl("Wie viele Tage zurück (Vorgabe 2, höchstens 14)."),
                "art" to text("Nur diese Art.", listOf("aktion", "fehler", "dauer", "gedaechtnis", "netz", "sprache", "system")),
                "suche" to text("Nur Einträge mit diesem Wort."),
                "hoechstens" to zahl("Höchstzahl der gezeigten Einträge (Vorgabe 80)."),
            ),
            nurLesen = true,
        ) { a ->
            withContext(Dispatchers.IO) {
                Ergebnis(Logbuch.auszug(a.optInt("tage", 2).coerceIn(1, 14), a.optString("art").ifBlank { null }, a.optString("suche").ifBlank { null }, a.optInt("hoechstens", 80).coerceIn(5, 300)))
            }
        },
    )

    private suspend fun suche(a: JSONObject): Ergebnis {
        val roh = a.optString("suche").trim()
        if (roh.isEmpty()) return Ergebnis("Es fehlt ein Suchwort.", fehler = true)
        val woerter = if (roh.length > 2 && roh.startsWith("\"") && roh.endsWith("\"")) listOf(roh.trim('"'))
        else roh.split(Regex("\\s+")).map { it.trim(',', '.', '?', '!', '„', '“', '"') }.filter { it.length >= 2 }.take(4)
        if (woerter.isEmpty()) return Ergebnis("Das Suchwort ist zu kurz.", fehler = true)
        val quellen = a.optJSONArray("quellen")?.let { q -> (0 until q.length()).map { q.optString(it).lowercase(Locale.GERMAN) }.toSet() }.orEmpty()
        fun will(q: String) = quellen.isEmpty() || q in quellen
        val von = a.optString("von").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val bis = a.optString("bis").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val n = a.optInt("hoechstens", 8).coerceIn(1, 30)
        fun passt(text: String) = woerter.all { text.contains(it, ignoreCase = true) }
        fun imZeitraum(tag: LocalDate) = (von == null || !tag.isBefore(von)) && (bis == null || !tag.isAfter(bis))
        val beginn = System.currentTimeMillis()
        val bloecke = mutableListOf<String>()
        val alle = Register.alle(context)

        if (will("tagebuch")) alle.filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.let { t ->
            val treffer = t.suche(woerter, von, bis, n)
            val monate = if (von == null && bis == null) t.sucheMonate(woerter).take(4) else emptyList()
            if (treffer.isNotEmpty() || monate.isNotEmpty()) bloecke += "TAGEBUCH:\n" +
                treffer.joinToString("\n") { (tag, ausschnitt) -> "- " + t.lang(tag) + ": " + ausschnitt } +
                (if (monate.isNotEmpty()) (if (treffer.isNotEmpty()) "\n" else "") + monate.joinToString("\n") { (m, ausschnitt) -> "- Monat $m (verdichtet): $ausschnitt" } else "")
        }
        if (will("gespraeche")) {
            val ordner = File(context.filesDir, "gespraeche")
            val treffer = ordner.listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedByDescending { it.lastModified() }.asSequence()
                .mapNotNull { f -> Gespraeche.laden(context, f.name.removeSuffix(".json")) }
                .filter { (s, _) -> imZeitraum(tag(s.geaendert)) }
                .mapNotNull { (s, nachrichten) ->
                    val passend = nachrichten.filter { it.art != "fehler" && passt(it.text) }
                    if (passend.isEmpty() && !passt(s.titel)) null
                    else "- „${s.titel}“ (${zeit(s.geaendert)}): " + passend.take(2).joinToString(" | ") { (if (it.vonMir) "Frank: " else "Jarvis: ") + TagebuchFaehigkeit.ausschnitt(it.text, woerter.first(), 160) }
                }.take(n).toList()
            if (treffer.isNotEmpty()) bloecke += "GESPRÄCHE MIT JARVIS:\n" + treffer.joinToString("\n")
        }
        if (will("notizen")) {
            val treffer = Gespraechsgedaechtnis.alleNotizen(context).filter { passt(it.text) }.sortedByDescending { it.erstellt }.take(n)
            if (treffer.isNotEmpty()) bloecke += "GESPRÄCHSNOTIZEN:\n" + treffer.joinToString("\n") { "- ${zeit(it.erstellt)}: ${it.text}" + (it.bis?.let { b -> " (bis $b)" } ?: "") }
        }
        if (will("gemerkt")) {
            val treffer = merkKontext(context).lines().filter { it.startsWith("[") && passt(it) }.take(n)
            if (treffer.isNotEmpty()) bloecke += "GEMERKT (Regeln, Ziele, Wissen über Frank):\n" + treffer.joinToString("\n") { "- $it" }
        }
        if (will("ideen")) {
            val lesen = alle.flatMap { it.werkzeuge }.firstOrNull { it.name == "ideen_lesen" }
            val antwort = runCatching { lesen?.ausfuehren(JSONObject().put("status", "alle").put("suche", woerter.maxBy { it.length }).put("limit", 100)) }.getOrNull()
            val treffer = antwort?.takeIf { !it.fehler }?.text?.lines()?.filter { it.startsWith("- ") && passt(it) }?.take(n).orEmpty()
            if (treffer.isNotEmpty()) bloecke += "GENIALE IDEEN:\n" + treffer.joinToString("\n") { it.take(400) }
        }
        if (will("ablage")) {
            val speicher = AblageZentrale.speicher(context)
            val treffer = speicher.eintraege().asSequence().filter { imZeitraum(tag(it.geaendert)) }.mapNotNull { e ->
                val text = runCatching { speicher.textDatei(e)?.readText() }.getOrNull().orEmpty()
                if (passt(e.titel + " " + e.beschreibung + " " + text)) "- „${e.titel}“ (${zeit(e.geaendert)}): " + TagebuchFaehigkeit.ausschnitt(text.ifEmpty { e.beschreibung }, woerter.first(), 160) else null
            }.take(n).toList()
            if (treffer.isNotEmpty()) bloecke += "ABLAGE:\n" + treffer.joinToString("\n")
        }
        if (will("auswertungen")) {
            val treffer = Tagesauswertung.archiv(context).asSequence().filter { imZeitraum(it.first) }.mapNotNull { (tag, datei) ->
                datei.readText().let { t -> if (passt(t)) "- " + tag.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)) + ": " + TagebuchFaehigkeit.ausschnitt(t, woerter.first(), 180) else null }
            }.take(n).toList()
            if (treffer.isNotEmpty()) bloecke += "FRÜHERE TAGESAUSWERTUNGEN:\n" + treffer.joinToString("\n")
        }
        Logbuch.gedaechtnis("Gedächtnis-Suche", "${woerter.size} Wort/Wörter, ${bloecke.size} Quelle(n) mit Treffern", System.currentTimeMillis() - beginn)
        if (bloecke.isEmpty()) return Ergebnis("Im Gedächtnis von Jarvis nichts zu „$roh“ gefunden" + (if (quellen.isNotEmpty()) " (gesucht in: ${quellen.joinToString(", ")})" else "") +
            ". Andere Wörter oder ein Wortstamm (zum Beispiel „angel“ statt „angeln“) finden oft mehr.")
        return Ergebnis(("Treffer zu „$roh“ im Gedächtnis von Jarvis (neueste zuerst):\n\n" + bloecke.joinToString("\n\n")).let { if (it.length > 14_000) it.take(14_000) + "\n… (mehr mit engerem Zeitraum oder weiteren Wörtern)" else it })
    }

    private fun tag(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    private fun zeit(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.GERMAN))

    @Suppress("unused")
    private val lagebild = Lagebild
}
