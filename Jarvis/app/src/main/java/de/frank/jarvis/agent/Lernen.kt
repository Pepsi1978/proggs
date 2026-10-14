package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Merkdatei
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.faehigkeit.UeberFrank
import de.frank.jarvis.faehigkeit.Ziele
import de.frank.jarvis.gedaechtnis.Bereich
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Jarvis lernt aus dem Tagebuch, im Anschluss an die Tagesauswertung. Drei kleine Schritte, jeder mit eigenem Zeitlimit,
 * damit ein langsamer Schritt die anderen nicht verhungern lässt:
 * 1. Lernen: Einmal am Tag liest Jarvis die Einträge seit dem zuletzt verarbeiteten Tag und notiert bis zu drei neue
 *    dauerhafte Tatsachen oder Ziele. Das Tagebuch ist Franks eigener Text; nur deshalb darf dieser Lauf Notizen anlegen.
 * 2. Tages-Stichpunkte: Tage, die aus den letzten 10 Tagen herausfallen, bekommen eine Kurzfassung (Kontextregel).
 * 3. Monat verdichten: Ein Monat, der ganz vor den letzten fünf Wochen liegt, wird zu einem kurzen Text – je Lauf einer.
 *    Hat sich ein schon verdichteter Monat geändert (Eintrag nachgetragen oder bearbeitet), wird er neu verdichtet.
 */
object Lernen {
    suspend fun laufe(context: Context) {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        if (!e.auswertungLernen) return
        val agent = JarvisAgent(app)
        if (!agent.verbunden) return
        val tagebuch = Register.alle(app).filterIsInstance<TagebuchFaehigkeit>().firstOrNull() ?: return
        suspend fun frage(anweisung: String, text: String, limitMs: Long): String? = withTimeoutOrNull(limitMs) {
            runCatching { agent.auth.streamChat(anweisung, listOf(ChatTurn("user", text)), e.modell, e.denkstufe).trim() }
                .onFailure { if (it is CancellationException) throw it; Logbuch.fehler("Lernen: Modell", it) }.getOrNull()
        }
        var geaendert = false

        // 1. Lernen zuerst: Es ist das Wichtigste und läuft nur einmal am Tag.
        val heute = LocalDate.now()
        if (e.lernlaufTag != heute.toString()) {
            // Ab dem Tag vor dem zuletzt verarbeiteten, höchstens sieben Tage: Nichts wird übersprungen, nichts dreimal gelernt.
            val bis = e.lernlaufBis.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val ab = (bis?.minusDays(1) ?: heute.minusDays(2)).coerceAtLeast(heute.minusDays(7))
            // Je Tag so viel, dass alle Tage hineinpassen (höchstens acht); was dann noch übersteht, fällt vorn beim Ältesten weg.
            // So sieht das Modell immer die neuesten Tage, und lernlaufBis (der neueste Eintrag) stimmt mit dem Gesehenen überein.
            val jeTag = (EINGABE_ZEICHEN / (ChronoUnit.DAYS.between(ab, heute).toInt() + 1).coerceAtLeast(1)).coerceIn(1_200, 3_000)
            val eintraege = tagebuch.eintraegeAb(ab, jeTag).let { t ->
                if (t.length <= EINGABE_ZEICHEN) t else "### " + t.takeLast(EINGABE_ZEICHEN).substringAfter("\n### ", t.takeLast(EINGABE_ZEICHEN)).removePrefix("### ")
            }
            if (eintraege.isNotBlank()) {
                val bekannt = "ZIELE (art ziel):\n" + Ziele.liste(Ziele.alle(app)).ifEmpty { "(noch nichts)" } + "\n\nINFOS (art info):\n" + UeberFrank.liste(UeberFrank.alle(app)).ifEmpty { "(noch nichts)" }
                val antwort = frage(LERNEN, "SCHON GEMERKT:\n$bekannt\n\nTAGEBUCH SEIT ${ab}:\n$eintraege", 70_000L)
                if (antwort != null) {
                    val gelernt = uebernimm(app, antwort, "Tagebuch")
                    if (gelernt.isNotEmpty()) { Protokoll.melde(Quelle.JARVIS, "Aus dem Tagebuch gelernt", gelernt.joinToString(" ")); geaendert = true }
                    e.lernlaufTag = heute.toString()
                    tagebuch.neuesterEintrag()?.let { e.lernlaufBis = it.toString() }
                }
                // Ohne Antwort bleibt der Tag offen: Der nächste Lauf versucht es noch einmal.
            } else e.lernlaufTag = heute.toString()
        }

        // 2. Tages-Stichpunkte für die Tage, die aus den letzten 10 Tagen herausgefallen sind.
        val offen = tagebuch.tageOhneKurzfassung(14)
        if (offen.isNotEmpty()) {
            val text = offen.joinToString("\n\n") { (tag, inhalt) -> "### $tag\n$inhalt" }
            val antwort = frage(TAGESKURZ, text, 60_000L)
            var anzahl = 0
            val erledigt = mutableSetOf<LocalDate>()
            antwort?.lines()?.forEach { zeile ->
                // Das Modell spiegelt gern das Eingabeformat („### 2026-09-29: …“) oder setzt das Datum fett: beides wegräumen.
                val sauber = zeile.trim().replace("**", "").replace(Regex("^(?:[#\\-*•–]+\\s*|\\d+[.)]\\s+)+"), "").trim()
                TAG.find(sauber)?.let { t ->
                    runCatching { LocalDate.parse(t.groupValues[1]) }.getOrNull()?.takeIf { d -> offen.any { it.first == d } }?.let { d ->
                        val satz = t.groupValues[2].trim().trim('"', '„', '“')
                        if (satz.length >= 10) { tagebuch.speichereKurzfassung(d, satz); erledigt += d; anzahl++ }
                    }
                }
            }
            // Bleibt ein Tag trotz Antwort ohne Kurzfassung, nimmt er den Anfang des Eintrags: Er soll nicht bei jedem Lauf wiederkommen.
            if (antwort != null) offen.filter { (d, _) -> d !in erledigt }.forEach { (d, inhalt) ->
                tagebuch.speichereKurzfassung(d, inhalt.replace(Regex("\\s+"), " ").take(280) + " …"); anzahl++
            }
            if (anzahl > 0) { Logbuch.gedaechtnis("Tagebuch: Tages-Stichpunkte", "$anzahl Tag(e)"); geaendert = true }
        }

        // 3. Einen Monat verdichten (oder einen geänderten neu verdichten).
        tagebuch.unverdichteterMonat()?.let { (monat, eintraege) ->
            val mindestens = minOf(80, eintraege.length / 3)
            val text = frage(VERDICHTEN, "Monat $monat\n\n$eintraege", 75_000L)?.takeIf { it.length >= mindestens }
            val versuche = File(app.filesDir, "tagebuch_verdichtet/$monat.versuche")
            if (text != null) {
                tagebuch.speichereVerdichtung(monat, kuerze(text, MONAT_ZEICHEN), eintraege)
                runCatching { versuche.delete() }
                Protokoll.melde(Quelle.JARVIS, "Tagebuch verdichtet", "Monat $monat liegt jetzt als Kurzfassung im Gedächtnis.")
                geaendert = true
            } else {
                // Ein Monat, der sich nicht verdichten lässt, darf die späteren nicht für immer blockieren: ab dem dritten Fehlschlag eine Notfassung.
                val n = (runCatching { versuche.readText().trim().toInt() }.getOrDefault(0)) + 1
                runCatching { versuche.writeText(n.toString()) }
                if (n >= 3) {
                    val notfassung = eintraege.split("\n\n### ").joinToString(" ") { it.removePrefix("### ").replace(Regex("\\s+"), " ").take(160) + " …" }
                    tagebuch.speichereVerdichtung(monat, kuerze(notfassung, MONAT_ZEICHEN), eintraege)
                    runCatching { versuche.delete() }
                    Protokoll.melde(Quelle.JARVIS, "Tagebuch verdichtet", "Monat $monat ließ sich nicht verdichten; eine Kurzfassung aus den Anfängen der Einträge steht jetzt im Gedächtnis.", ok = false)
                    geaendert = true
                }
            }
        }
        if (geaendert) Lagebild.anstossen(app, listOf(Bereich.TAGEBUCH, Bereich.GEMERKT), "aus dem Tagebuch gelernt")
    }

    /**
     * Übernimmt die Zeilen einer Lern-Antwort. Ziele legt ein Lernlauf nur neu an, er ersetzt nie eines (Ziele setzt Frank).
     * Infos darf er ersetzen; die alte Fassung landet im Verlauf `merk_verlauf.jsonl`. Ist eine id ungültig, wird neu angelegt.
     */
    fun uebernimm(app: Context, antwort: String, quelle: String, hoechstens: Int = 3): List<String> = antwort.lines()
        .map { it.trim().replace(Regex("^(?:[-*•–]\\s*|\\d+[.)]\\s*)"), "").replace("**", "").trim().trimStart('„', '"', '“', '»').trim() }
        .mapNotNull { ZEILE.find(it) }.take(hoechstens).mapNotNull { treffer ->
            val istZiel = treffer.groupValues[1].uppercase() == "ZIEL"
            val datei: Merkdatei = if (istZiel) Ziele else UeberFrank
            val satz = treffer.groupValues[3].trim().trimEnd('“', '”', '"', '«').trim()
            val id = treffer.groupValues[2].toIntOrNull()?.takeIf { it > 0 && !istZiel && datei.alle(app).any { r -> r.id == it } }
            if (satz.length !in 12..300 || !satz.startsWith("Frank") || datei.alle(app).any { normal(it.text) == normal(satz) }) return@mapNotNull null
            val alt = id?.let { i -> datei.alle(app).firstOrNull { it.id == i } }
            val neu = datei.speichere(app, satz, id)
            if (neu == null) { Protokoll.melde(Quelle.JARVIS, if (datei.defekt(app) != null) "Gedächtnis defekt" else "Gedächtnis voll", "Nicht gelernt ($quelle): $satz", ok = false); return@mapNotNull null }
            if (alt != null) runCatching {
                File(app.filesDir, "merk_verlauf.jsonl").appendText(JSONObject().put("zeit", System.currentTimeMillis()).put("datei", if (istZiel) "ziele" else "ueber_frank")
                    .put("id", alt.id).put("alt", alt.text).put("neu", neu.text).put("quelle", quelle).toString() + "\n")
            }
            (if (alt != null) "[${alt.id}] „${alt.text}“ → " else "") + neu.text
        }

    private fun normal(text: String): String = text.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), "").replace(Regex("\\s+"), " ").trim()

    /** Kürzt an der letzten Satzgrenze innerhalb der Grenze. */
    private fun kuerze(text: String, grenze: Int): String = text.trim().let { t ->
        if (t.length <= grenze) t else t.take(grenze).let { k -> (k.lastIndexOfAny(charArrayOf('.', '!', '?')) + 1).takeIf { it > grenze / 2 }?.let { k.substring(0, it) } ?: k }
    }

    private const val MONAT_ZEICHEN = 900
    /** So viel Tagebuch bekommt der tägliche Lernlauf höchstens. */
    private const val EINGABE_ZEICHEN = 12_000

    private val VERDICHTEN =
        "Du verdichtest Franks Tagebuch eines Monats zu einem Gedächtnis für seinen Assistenten Jarvis. Schreibe in höchstens $MONAT_ZEICHEN Zeichen, in ganzen Sätzen und ohne Aufzählungszeichen, " +
            "was in diesem Monat für Frank wichtig war: was er gemacht und erlebt hat, woran er gearbeitet hat, Gesundheit und Training, Menschen, wiederkehrende Themen, Entscheidungen " +
            "und besondere Ereignisse mit Datum. Nur was in den Einträgen steht, sachlich und ohne Wertung. Antworte nur mit diesem Text."

    private const val TAGESKURZ =
        "Du schreibst für Franks Assistenten Jarvis je Tagebucheintrag eine Kurzfassung in Stichpunkten: höchstens 280 Zeichen je Tag, die wichtigsten Punkte durch Semikolons getrennt " +
            "(was Frank gemacht und erlebt hat, Menschen, Gesundheit, Training, Arbeit an Projekten, Entscheidungen), sachlich, nur was im Eintrag steht. " +
            "Antworte je Tag mit genau einer Zeile in der Form JJJJ-MM-TT: Stichpunkt; Stichpunkt; Stichpunkt – sonst nichts."

    /** „ZIEL: Satz“, „INFO: Satz“ oder mit id zum Ersetzen „INFO 4: Satz“. */
    private val ZEILE = Regex("^(ZIEL|INFO)\\s*\\[?(\\d+)?\\]?\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val TAG = Regex("^(\\d{4}-\\d{2}-\\d{2})\\s*:\\s*(.+)$")

    private const val LERNEN =
        "Du pflegst für Franks Assistenten Jarvis das Gedächtnis darüber, wer Frank ist. Du bekommst, was schon gemerkt ist (mit id in eckigen Klammern), und Franks Tagebuch der letzten Tage. " +
            "Finde höchstens drei Dinge, die das Gedächtnis besser machen: ein neues ZIEL (was Frank erreichen will und im Tagebuch eindeutig so sagt), eine neue INFO (dauerhafte Tatsache: " +
            "Vorlieben und Abneigungen, was ihm wichtig ist, Besitz, Gewohnheiten, Menschen in seinem Leben, Gesundheitliches von Dauer) oder eine vorhandene INFO, die das Tagebuch überholt " +
            "oder widerlegt hat. Ziele ersetzt du nie. Nicht: was er an einem einzelnen Tag gemacht hat, Stimmungen, einzelne Termine, Vermutungen. Im Zweifel nichts. " +
            "Antworte je Ding mit einer Zeile ohne Anführungszeichen und ohne Aufzählungszeichen, der Satz in der dritten Person und mit Frank beginnend, zum Beispiel:\n" +
            "ZIEL: Frank will …\nINFO: Frank …\nINFO 7: Frank … (ersetzt den Eintrag mit id 7)\n" +
            "Gibt es nichts, antworte nur mit dem Wort KEINE."
}
