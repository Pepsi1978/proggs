package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import de.frank.jarvis.news.NewsGedaechtnis
import de.frank.jarvis.news.NewsMeldung
import de.frank.jarvis.news.NewsRegeln
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an „News-Kompass“ (de.frank.newskompass) über dessen Jarvis-Brücke, nur lesend: alle Rubriken (Themen) mit ihren
 * Meldungen, je Tag nur, was Frank noch nicht genannt wurde ([NewsGedaechtnis], über Handy und Tablet hinweg), und einzelne
 * Meldungen im Volltext. Spielstände und Ergebnisse von Borussia Dortmund bleiben ausgeblendet, bis Frank ausdrücklich fragt.
 */
class NewsFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "news"
    override val name = "News-Kompass"
    override val beschreibung = "Nachrichten aus News-Kompass lesen: alle Rubriken (z. B. KI-News, Fußball), je Tag nur noch nicht Genanntes, einzelne Meldungen im Volltext."
    override val hinweise =
        "News-Kompass ist Franks Nachrichten-App: Sie sammelt zu jeder Rubrik (frei formuliertes Thema, z. B. KI-News, Fußball) automatisch zu festen Zeiten " +
            "(KI-News um 5 und 17 Uhr) die Neuigkeiten. Fragt Frank nach News oder Nachrichten („Was gibt es für News?“, „Was gibt es heute an KI-News?“, " +
            "„Gibt es was Neues vom Fußball?“), liest du ZUERST news_lesen (ohne Rubrik = alle, sonst mit rubrik), auch bei jeder erneuten Frage, statt aus dem Gedächtnis zu antworten. " +
            "Nur wenn Frank ausdrücklich das Internet oder die Websuche für allgemeine Nachrichten verlangt („Schau bitte im Internet nach allgemeinen Nachrichten“), " +
            "suchst du direkt mit der Websuche, ohne News-Kompass. Nenne die neuen Schlagzeilen kurz und verständlich (im Sprachgespräch knapp vorlesen), ohne ids, Quellen oder Adressen; " +
            "Jarvis merkt sich, was du Frank genannt hast, und lässt es am selben Tag weg (auch über Rubriken, Gespräche, Handy und Tablet hinweg). " +
            "Rückfragen zu einer genannten Meldung („Erzähl mir mehr über die Anthropic-Meldung“): news_meldung mit ihrer id aus dem Gespräch (sonst mit suche), dann die Details sinngemäß. " +
            "Bei weiteren Nachfragen erst prüfen, ob die Antwort in der Meldung steht oder du ein Detail ausgelassen hast; geht die Frage darüber hinaus oder will Frank eine Internetsuche dazu, " +
            "suche gezielt zur konkreten Meldung mit der direkten Websuche (Titel und Kernbegriffe) und antworte im selben Gespräch; keinen Agenten. " +
            "Borussia Dortmund: Spielstände und Ergebnisse nie ungefragt nennen, auch nicht über eine Schlagzeile oder die Websuche (Frank schaut zeitversetzt)."

    override fun stoerung(): String? = AppBruecke.rufe(context, name, BRUECKE, "lesen", JSONObject().put("tage", 1), lesend = true)
        .optString("fehler").takeIf { it.isNotEmpty() }

    override val werkzeuge: List<Werkzeug> by lazy { listOf(lesen, meldung) }

    private val lesen = Werkzeug(
        name = "news_lesen",
        titel = "Nachrichten lesen",
        beschreibung = "Jarvis: liest die Nachrichten aus Franks App News-Kompass, alle Rubriken (z. B. KI-News, Fußball) oder eine. Nutze es bei JEDER Frage nach News, " +
            "Nachrichten oder Neuigkeiten („Was gibt es für News?“, „Was gibt es heute an KI-News?“, „Was gibt es Neues vom Fußball?“), auch wiederholt: " +
            "Es liest jedes Mal frisch und liefert nur Meldungen, die Frank heute noch nicht genannt wurden, samt Stand der App (aktuell, älterer Stand, nicht erreichbar). " +
            "Nicht bei ausdrücklichem Wunsch nach einer Internetsuche für allgemeine Nachrichten (dann direkt die Websuche).",
        schema = schema(
            "rubrik" to text("Nur diese Rubrik, so wie Frank sie nennt (z. B. KI, Fußball). Weglassen = alle Rubriken."),
            "auch_genannte" to schalter("Auch heute schon Genanntes zeigen (nur wenn Frank alles noch einmal hören will). Vorgabe: aus."),
            "tage" to zahl("Nur wenn Frank ältere Meldungen will: so viele Tage zurück (1–10). Weglassen = aktueller Bestand (heutige Ausgaben, je Rubrik mindestens die neueste)."),
            "mitgeteilt" to textListe("Nur für ChatGPT und andere Wege außerhalb der Jarvis-App: ids der Meldungen, die du Frank seit dem letzten Abruf tatsächlich genannt hast."),
        ),
        nurLesen = true,
        taetigkeit = "liest die Nachrichten in News-Kompass",
    ) { a -> leseNews(a) }

    private val meldung = Werkzeug(
        name = "news_meldung",
        titel = "Nachricht im Detail",
        beschreibung = "Jarvis: liest eine einzelne Meldung aus News-Kompass vollständig (alle Absätze, Zeitangabe, Quellen für eine gezielte Websuche). " +
            "Für Rückfragen zu einer genannten Meldung („Berichte mir genauer über die Anthropic-Meldung“) und für den Wunsch, eine Meldung noch einmal zu hören. " +
            "Über id aus news_lesen ODER suche (Wörter aus Titel oder Inhalt).",
        schema = schema(
            "id" to text("id der Meldung aus news_lesen."),
            "suche" to text("Statt id: Stichwörter aus Titel oder Inhalt, z. B. „Anthropic Börse“."),
            "ergebnis" to schalter("Nur wenn Frank ausdrücklich nach Spielstand oder Ergebnis von Borussia Dortmund fragt: dann nichts ausblenden."),
        ),
        nurLesen = true,
        taetigkeit = "liest eine Meldung in News-Kompass",
    ) { a -> leseMeldung(a) }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject =
        AppBruecke.unterbrechbar { AppBruecke.rufe(context, name, BRUECKE, methode, anfrage, lesend = true) }

    private fun meldungen(antwort: JSONObject): List<NewsMeldung> {
        val arr = antwort.optJSONArray("meldungen") ?: JSONArray()
        return (0 until arr.length()).map { arr.getJSONObject(it) }.map {
            NewsMeldung(it.optString("id"), it.optString("rubrik"), it.optString("titel"), it.optString("kurz"), it.optString("wann"),
                it.optString("quelle"), it.optBoolean("update"), it.optLong("ausgabe_um"))
        }
    }

    /**
     * Der aktuelle Bestand: alle Ausgaben von heute und je Rubrik zusätzlich die neueste (etwa die von gestern Abend vor dem ersten Lauf).
     * Ältere Tage nur, wenn Frank ausdrücklich weiter zurück will (tage).
     */
    private fun aktuell(liste: List<NewsMeldung>): List<NewsMeldung> {
        val heute = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val neueste = liste.groupBy { it.rubrik }.mapValues { (_, l) -> l.maxOf { it.ausgabeUm } }
        return liste.filter { it.ausgabeUm >= heute || it.ausgabeUm == neueste[it.rubrik] }
    }

    /** Dortmund-Meldung mit Spielausgang: Schlagzeile und Kurztext verraten nichts. */
    private fun entschaerft(m: NewsMeldung): NewsMeldung = if (!NewsRegeln.spoiler(m)) m else
        m.copy(titel = "Borussia Dortmund: Meldung zum Spiel (Ergebnis ausgeblendet)", kurz = NewsRegeln.ohneErgebnis(m.kurz))

    private suspend fun leseNews(a: JSONObject): Ergebnis {
        // ChatGPT meldet nach: Was es Frank zuvor genannt hat, zählt ab jetzt als genannt.
        a.optJSONArray("mitgeteilt")?.let { ids ->
            val gemeldet = (0 until ids.length()).map { ids.optString(it) }.filter { it.isNotBlank() }.toSet()
            if (gemeldet.isNotEmpty()) {
                val alle = rufe("lesen", JSONObject().put("tage", 10))
                if (!alle.has("fehler")) NewsGedaechtnis.merke(context, meldungen(alle).filter { it.id in gemeldet }.map(::entschaerft))
            }
        }
        val anfrage = JSONObject().put("tage", a.optInt("tage", 2).coerceIn(1, 10))
        a.optString("rubrik").trim().takeIf { it.isNotEmpty() }?.let { anfrage.put("rubrik", it) }
        var antwort = rufe("lesen", anfrage)
        // Rubriken, die seltener als täglich sammeln: ohne Lauf in den letzten zwei Tagen zehn Tage zurück, damit auch ihre neueste Ausgabe zählt.
        val ohneLauf = antwort.optJSONArray("rubriken")?.let { r -> (0 until r.length()).any { r.getJSONObject(it).optLong("letzter_lauf") == 0L } } == true
        if (ohneLauf && a.optInt("tage", 0) <= 0) rufe("lesen", JSONObject(anfrage.toString()).put("tage", 10)).takeIf { !it.has("fehler") }?.let { antwort = it }
        antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let {
            return Ergebnis("NEWS-KOMPASS NICHT ERREICHBAR: $it Sag Frank klar, dass News-Kompass gerade nicht gelesen werden konnte (das ist nicht „keine neuen Meldungen“).", fehler = true)
        }
        val jetzt = antwort.optLong("jetzt", System.currentTimeMillis())
        val alle = meldungen(antwort).let { if (a.optInt("tage", 0) > 0) it else aktuell(it) }.map(::entschaerft)
        val genannt = NewsGedaechtnis.genannt(context)
        val neu = if (a.optBoolean("auch_genannte")) alle else NewsRegeln.neu(alle, genannt)
        val weggelassen = alle.size - neu.size
        NewsGedaechtnis.angeboten(neu)

        val rubriken = antwort.optJSONArray("rubriken") ?: JSONArray()
        val stand = StringBuilder()
        val veraltet = mutableListOf<String>()
        for (i in 0 until rubriken.length()) {
            val r = rubriken.getJSONObject(i)
            val letzter = r.optLong("letzter_lauf")
            val faellig = r.optLong("faellig_seit")
            val zeiten = (r.optJSONArray("uhrzeiten") ?: JSONArray()).let { z -> (0 until z.length()).joinToString(" und ") { z.optString(it) } }
            stand.append("- ").append(r.optString("name").ifBlank { r.optString("titel") })
            if (zeiten.isNotEmpty()) stand.append(" (sammelt um ").append(zeiten).append(" Uhr)")
            stand.append(": letzter Stand ").append(if (letzter > 0) zeit(letzter) else "keiner in den letzten 10 Tagen")
            if (r.optBoolean("nichts_neues")) stand.append(", beim letzten Lauf nichts Neues gefunden")
            r.optString("fehler").takeIf { it.isNotBlank() }?.let { stand.append(", letzter Lauf gescheitert: ").append(it.take(160)) }
            // Ein fälliger Termin ohne Ausgabe danach (mehr als 20 Minuten her): älterer Stand, nicht „nichts Neues“.
            if (faellig > 0 && letzter + 15 * 60_000L < faellig && jetzt - faellig > 20 * 60_000L) {
                stand.append(", ÄLTERER STAND: der Lauf von ").append(zeit(faellig)).append(" fehlt")
                veraltet += r.optString("name").ifBlank { r.optString("titel") }
            }
            stand.append('\n')
        }
        if (antwort.optBoolean("lauf_wartet")) stand.append("- Ein Lauf steht gerade noch aus oder läuft.\n")
        if (!antwort.optBoolean("zeitplan_aktiv", true)) stand.append("- Der automatische Zeitplan ist in News-Kompass ausgeschaltet.\n")

        val text = buildString {
            append("NEWS-KOMPASS, gelesen ").append(zeit(jetzt))
            a.optString("rubrik").trim().takeIf { it.isNotEmpty() }?.let { append(", Rubrik „").append(it).append("“") }
            append(".\nStand der Rubriken:\n").append(stand)
            if (veraltet.isNotEmpty()) append("Sag Frank, dass für ").append(veraltet.joinToString(", ")).append(" nur ein älterer Stand vorliegt.\n")
            append('\n')
            if (alle.isEmpty()) {
                append("KEINE MELDUNGEN im Bestand").append(if (a.gesetzt("rubrik")) " dieser Rubrik" else "").append(" für den gewählten Zeitraum. ")
                if (a.gesetzt("rubrik")) append("Prüfe an den Rubriken oben, ob Frank eine andere meinte. ")
            } else if (neu.isEmpty()) {
                append("KEINE NEUEN MELDUNGEN: Alle ").append(alle.size).append(" Meldung(en) wurden Frank heute schon genannt. Sag ihm kurz, dass seit dem letzten Mal nichts Neues dazugekommen ist")
                if (veraltet.isNotEmpty()) append(", und dass der Stand älter ist")
                append(". Einzelne Meldungen kann er trotzdem noch einmal hören (news_meldung).")
            } else {
                append(if (a.optBoolean("auch_genannte")) "ALLE MELDUNGEN" else "NEU FÜR FRANK (heute noch nicht genannt)").append(": ").append(neu.size).append(" Meldung(en)\n")
                neu.groupBy { it.rubrik }.forEach { (rubrik, liste) ->
                    append(rubrik).append(":\n")
                    liste.forEach { m ->
                        append("- [id ").append(m.id).append("] ").append(m.titel)
                        if (m.wann.isNotBlank()) append(" (").append(m.wann).append(")")
                        if (m.update) append(" [Folgemeldung]")
                        if (m.kurz.isNotBlank()) append(": ").append(m.kurz.replace(Regex("\\s+"), " ").take(180))
                        append('\n')
                    }
                }
                if (weggelassen > 0) append("Heute schon genannt und weggelassen: ").append(weggelassen).append(".\n")
                append("\nSO ANTWORTEN: die Schlagzeilen kurz und verständlich, nach Rubrik, ohne ids, Quellen und Adressen; im Sprachgespräch knapp. " +
                    "Was du nennst, gilt als genannt; Ausgelassenes kommt beim nächsten Mal wieder. Details zu einer Meldung mit news_meldung und ihrer id. " +
                    "Dortmunder Spielstände oder Ergebnisse nie ungefragt.")
            }
        }
        return Ergebnis(text.trim())
    }

    private suspend fun leseMeldung(a: JSONObject): Ergebnis {
        var id = a.optString("id").trim().removePrefix("id ").trim()
        val suche = a.optString("suche").trim()
        // Ohne id oder mit einer id vom anderen Gerät: über die Wörter finden.
        if (id.isEmpty() || suche.isNotEmpty()) {
            val stichworte = suche.ifEmpty { id }
            if (stichworte.isEmpty()) return Ergebnis("Es fehlt die id oder ein Suchwort für die Meldung.", fehler = true)
            if (!id.contains('/') || suche.isNotEmpty()) {
                val alle = rufe("lesen", JSONObject().put("tage", 10))
                alle.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis("NEWS-KOMPASS NICHT ERREICHBAR: $it", fehler = true) }
                val gesucht = NewsRegeln.woerter(stichworte)
                val treffer = meldungen(alle).map { m -> m to NewsRegeln.woerter(m.titel + " " + m.kurz + " " + m.rubrik).count { w -> gesucht.any { w.startsWith(it.take(5)) } } }
                    .filter { it.second > 0 }.sortedByDescending { it.second }
                if (treffer.isEmpty()) return Ergebnis("Keine Meldung in News-Kompass passt zu „$stichworte“.", fehler = true)
                if (treffer.size > 1 && treffer[0].second == treffer[1].second && treffer[0].second < gesucht.size)
                    return Ergebnis("Mehrere Meldungen passen. Frage Frank, welche gemeint ist, oder nimm die id:\n" +
                        treffer.take(5).joinToString("\n") { "- [id ${it.first.id}] ${entschaerft(it.first).titel}" }, fehler = true)
                id = treffer[0].first.id
            }
        }
        val antwort = rufe("meldung", JSONObject().put("id", id))
        antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis("News-Kompass: $it Versuche es mit suche (Stichwörter aus dem Titel).", fehler = true) }
        val m = antwort.getJSONObject("meldung")
        val absaetze = (m.optJSONArray("absaetze") ?: JSONArray()).let { arr -> (0 until arr.length()).map { arr.optString(it) } }
        val quellen = (m.optJSONArray("quellen") ?: JSONArray()).let { arr -> (0 until arr.length()).map { arr.optString(it) } }
        val roh = NewsMeldung(id, m.optString("rubrik"), m.optString("titel"), absaetze.firstOrNull().orEmpty(), m.optString("wann"), quellen.firstOrNull().orEmpty(), m.optBoolean("update"), m.optLong("ausgabe_um"))
        val schutz = !a.optBoolean("ergebnis") && NewsRegeln.dortmundSpoiler(roh.titel + " " + absaetze.joinToString(" "))
        // Wer gezielt nachfragt, hat die Meldung gehört; die Antwort darauf markiert sie als genannt.
        NewsGedaechtnis.angeboten(listOf(entschaerft(roh)))
        val text = buildString {
            append("MELDUNG aus News-Kompass (").append(roh.rubrik).append(", Ausgabe ").append(zeit(roh.ausgabeUm)).append(")\n")
            append(if (schutz) entschaerft(roh).titel else roh.titel)
            if (roh.wann.isNotBlank()) append(" (").append(roh.wann).append(")")
            if (roh.update) append(" [Folgemeldung]")
            append("\n\n")
            absaetze.forEach { append(if (schutz) NewsRegeln.ohneErgebnis(it) else it).append("\n\n") }
            if (schutz) append("(Spielstand und Ergebnis von Borussia Dortmund ausgeblendet: Frank schaut zeitversetzt. Nur auf seine ausdrückliche Frage mit ergebnis.)\n\n")
            if (quellen.isNotEmpty()) append("Quellen (nur für dich, etwa für eine gezielte Websuche zu dieser Meldung; Frank nicht nennen):\n").append(quellen.take(6).joinToString("\n")).append('\n')
            append("\nGeht Franks Frage über diese Meldung hinaus oder will er eine Internetsuche dazu: gezielt mit der direkten Websuche zu genau dieser Meldung suchen, kein Agent.")
        }
        return Ergebnis(text.trim())
    }

    private fun zeit(ms: Long): String {
        if (ms <= 0) return "unbekannt"
        val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        val heute = LocalDate.now()
        val uhr = z.format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)) + " Uhr"
        return when (z.toLocalDate()) {
            heute -> "heute $uhr"
            heute.minusDays(1) -> "gestern $uhr"
            else -> z.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMANY)) + ", $uhr"
        }
    }

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.newskompass.jarvis")
    }
}
