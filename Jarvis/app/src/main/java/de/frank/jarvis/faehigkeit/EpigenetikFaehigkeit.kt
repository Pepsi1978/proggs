package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/**
 * Anbindung an Franks App „Epigenetik“ (de.frank.epigenetik) über deren Jarvis-Brücke (Signatur-Erlaubnis).
 *
 * Die App recherchiert mit ChatGPT und Websuche, welche epigenetischen Wirkungen Nahrungsergänzungsmittel,
 * Nahrung und Verhalten haben (Gene, Enzyme, Signalwege aus einem festen Schalter-Vokabular), und umgekehrt,
 * was eine gewünschte Wirkung oder ein Ziel erreicht. Vorhandene Auswertungen liest Jarvis sofort, statt selbst
 * zu recherchieren; fehlt etwas, stößt er die Recherche in der App an (1–4 Minuten).
 */
class EpigenetikFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "epigenetik"
    override val name = "Epigenetik"
    override val beschreibung = "Epigenetische Wirkungen von Nahrungsergänzungsmitteln, Nahrung und Verhalten lesen und recherchieren lassen; Wirkungen, Ziele und Franks Stack."
    override val hinweise =
        "Franks App Epigenetik ist seine Wissensbasis dazu, welche Gene, Enzyme und Signalwege (Sirtuine, NAD+, AMPK, mTOR, NRF2, NF-κB, DNA-Methylierung, " +
            "Histone, Autophagie …) Nahrungsergänzungsmittel, Lebensmittel und Verhaltensweisen an- oder abschalten. Fragt Frank nach epigenetischen Wirkungen " +
            "(„Welche epigenetischen Wirkungen hat Ashwagandha?“, „Was aktiviert Sirtuine?“, „Was hilft epigenetisch bei Krebsprävention?“), nimm IMMER zuerst " +
            "epigenetik_lesen: Liegt die Auswertung vor, kommt sie sofort und vollständig – dann nicht selbst im Internet suchen, sondern darauf aufbauen. " +
            "Fehlt sie, startet epigenetik_lesen die Recherche in der App und wartet bis zu 50 Sekunden; läuft sie danach noch, sag Frank, dass die App weiter " +
            "recherchiert (1–4 Minuten), und lies später mit epigenetik_lesen nach. Für „Welche meiner Mittel wirken auf NRF2?“ ohne neue Recherche: epigenetik_schalter. " +
            "Für den Überblick, Franks Stack und die Stack-Analyse: epigenetik_uebersicht. Erkläre die Ergebnisse verständlich, nenne Evidenzstufe und Dosis, " +
            "unterscheide Zellkultur und Tierversuch von Humanstudien und gib keine Heilversprechen."

    override fun stoerung(): String? = rufeDirekt("info", JSONObject()).optString("fehler").takeIf { it.isNotEmpty() }

    override val werkzeuge = listOf(
        Werkzeug(
            name = "epigenetik_lesen",
            titel = "Epigenetik: Auswertung lesen",
            beschreibung = "Jarvis: liest aus Franks App Epigenetik die vollständige Auswertung zu einem Nahrungsergänzungsmittel, Lebensmittel oder Verhalten " +
                "(epigenetische Schalter mit Richtung, Stärke, Evidenz und Mechanismus, Dosierung, Einnahme, Risiken, Synergien, Fazit, Quellen), " +
                "zu einer Wirkung („Sirtuine aktivieren“: beste Supplements, Nahrung, Verhalten), zu einem Ziel („Krebsprävention“) oder zu einer früheren Frage. " +
                "Namen dürfen ungenau oder falsch geschrieben sein. Fehlt die Auswertung, startet es sie in der App und wartet bis zu 50 Sekunden auf das Ergebnis.",
            schema = schema(
                "name" to text("Mittel, Wirkung, Ziel oder Frage, z. B. „Ashwagandha“, „Nikotinamid-Ribosid“, „Sirtuine aktivieren“, „Langlebigkeit“."),
                "art" to text("Was gemeint ist; weglassen = die App erkennt es selbst.", listOf("mittel", "wirkung", "ziel", "frage")),
                "kategorie" to text("Nur bei Mitteln: Art des Mittels für eine neue Recherche.", listOf("supplement", "nahrung", "verhalten")),
                "kurz" to schalter("true = Kurzfassung (Top-Schalter, Dosis, Fazit) für den Sprachmodus. Vorgabe: vollständig."),
                "recherchieren_wenn_fehlt" to schalter("Vorgabe true: fehlt die Auswertung, startet die App die Recherche. false = nur nachsehen."),
                pflicht = listOf("name"),
            ),
            nurLesen = true,
        ) { a ->
            val anfrage = JSONObject().put("name", a.optString("name"))
                .put("recherchieren_wenn_fehlt", if (a.has("recherchieren_wenn_fehlt")) a.optBoolean("recherchieren_wenn_fehlt") else true)
                .put("kurz", a.optBoolean("kurz"))
                .put("warte_ms", WARTEN_MS)
            if (a.gesetzt("art")) anfrage.put("typ", a.optString("art"))
            if (a.gesetzt("kategorie")) anfrage.put("kategorie", a.optString("kategorie"))
            ergebnis(rufe("lesen", anfrage))
        },
        Werkzeug(
            name = "epigenetik_recherchieren",
            titel = "Epigenetik: neu recherchieren",
            beschreibung = "Jarvis: lässt die App Epigenetik etwas neu oder erneut recherchieren – ein Mittel, eine Wirkung, ein Ziel oder eine Frage zur Epigenetik. " +
                "Nur wenn Frank ausdrücklich eine neue oder aktualisierte Recherche will oder eine freie Frage stellt; zum Nachsehen epigenetik_lesen nehmen. " +
                "Wartet bis zu 50 Sekunden, sonst läuft die Recherche in der App weiter (1–4 Minuten).",
            schema = schema(
                "name" to text("Mittel, Wirkung, Ziel oder die Frage im Wortlaut."),
                "art" to text("Was recherchiert wird.", listOf("mittel", "wirkung", "ziel", "frage")),
                "kategorie" to text("Nur bei Mitteln.", listOf("supplement", "nahrung", "verhalten")),
                "neu" to schalter("true = auch wenn schon vorhanden, neu recherchieren (Vorgabe true)."),
                pflicht = listOf("name"),
            ),
            nurLesen = false,
        ) { a ->
            val anfrage = JSONObject().put("name", a.optString("name")).put("neu", if (a.has("neu")) a.optBoolean("neu") else true).put("warte_ms", WARTEN_MS)
            if (a.gesetzt("art")) anfrage.put("typ", a.optString("art"))
            if (a.gesetzt("kategorie")) anfrage.put("kategorie", a.optString("kategorie"))
            ergebnis(rufe("recherchieren", anfrage))
        },
        Werkzeug(
            name = "epigenetik_schalter",
            titel = "Epigenetik: Mittel zu einem Schalter",
            beschreibung = "Jarvis: zeigt ohne neue Recherche, welche bereits recherchierten Mittel aus Franks App Epigenetik auf einen epigenetischen Schalter wirken " +
                "(z. B. SIRT1, NAD, AMPK, MTOR, NRF2, NFKB, DNMT, HDAC, AUTOPHAGIE, P53, BDNF, EPIUHR), mit Richtung, Stärke und Evidenz, und ob sie in Franks Stack sind.",
            schema = schema("schalter" to text("Schalter-ID oder Name, z. B. „SIRT1“, „NRF2“, „Autophagie“."), pflicht = listOf("schalter")),
            nurLesen = true,
        ) { a ->
            val o = rufe("schalter", JSONObject().put("schalter", a.optString("schalter")))
            o.optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(buildString {
                append(o.optString("name")).append(" (").append(o.optString("schalter")).append("), günstig: ").append(o.optString("guenstig")).append(". ").append(o.optString("info")).append('\n')
                val l = o.optJSONArray("mittel")
                if (l == null || l.length() == 0) append(o.optString("hinweis"))
                else for (i in 0 until l.length()) {
                    val m = l.getJSONObject(i)
                    append("- ").append(m.optString("name")).append(" (").append(m.optString("kategorie")).append("): ").append(m.optString("richtung"))
                        .append(", Stärke ").append(m.optInt("staerke")).append(", ").append(m.optString("evidenz"))
                    if (m.optBoolean("im_stack")) append(", im Stack")
                    m.optString("mechanismus").takeIf { it.isNotBlank() }?.let { append(" – ").append(it) }
                    append('\n')
                }
            }.trim())
        },
        Werkzeug(
            name = "epigenetik_uebersicht",
            titel = "Epigenetik: Überblick und Stack",
            beschreibung = "Jarvis: listet, was in Franks App Epigenetik schon recherchiert ist (Mittel mit epigenetischem Potenzial 0–100, Wirkungen, Ziele, Fragen) " +
                "oder – mit bereich „stack“ – die Mittel, die Frank täglich nimmt, samt der letzten KI-Analyse des Stacks (Synergien, Lücken, Risiken, Tagesplan).",
            schema = schema("bereich" to text("Was gezeigt wird; Vorgabe: alles.", listOf("alles", "mittel", "wirkung", "ziel", "frage", "stack"))),
            nurLesen = true,
        ) { a ->
            val bereich = a.optString("bereich", "alles").ifBlank { "alles" }
            if (bereich == "stack") {
                val o = rufe("stack", JSONObject())
                o.optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(buildString {
                    val l = o.optJSONArray("mittel")
                    append("STACK (").append(l?.length() ?: 0).append(" Mittel):\n")
                    if (l != null) for (i in 0 until l.length()) l.getJSONObject(i).let { append("- ").append(it.optString("name")).append(it.optString("kurz").let { k -> if (k.isBlank()) "" else ": $k" }).append('\n') }
                    append("\nLETZTE STACK-ANALYSE:\n").append(o.optString("analyse"))
                }.trim())
            } else {
                val anfrage = JSONObject()
                if (bereich != "alles") anfrage.put("typ", bereich)
                val o = rufe("liste", anfrage)
                o.optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(buildString {
                    val l = o.optJSONArray("eintraege")
                    append(o.optInt("anzahl")).append(" Einträge:\n")
                    if (l != null) for (i in 0 until l.length()) {
                        val e = l.getJSONObject(i)
                        append("- ").append(e.optString("name")).append(" [").append(e.optString("typ"))
                        e.optString("kategorie").takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
                        if (e.has("potenzial")) append(", Potenzial ").append(e.optInt("potenzial"))
                        if (e.optString("status") != "fertig") append(", ").append(e.optString("status"))
                        if (e.optBoolean("im_stack")) append(", im Stack")
                        append("]")
                        e.optString("kurz").takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                        append('\n')
                    }
                }.trim())
            }
        },
    )

    /** Kurzer Überblick für das Gedächtnis oder andere Fähigkeiten. */
    suspend fun ueberblick(): String = rufe("liste", JSONObject()).let { o ->
        o.optString("fehler").takeIf { it.isNotEmpty() } ?: (0 until (o.optJSONArray("eintraege")?.length() ?: 0)).joinToString("\n") {
            val e = o.getJSONArray("eintraege").getJSONObject(it)
            "- ${e.optString("name")} (${e.optString("typ")}${if (e.has("potenzial")) ", Potenzial ${e.optInt("potenzial")}" else ""})"
        }
    }

    private fun ergebnis(o: JSONObject): Ergebnis {
        o.optString("fehler").takeIf { it.isNotEmpty() && !o.has("gefunden") }?.let { return Ergebnis(it, fehler = true) }
        return when {
            o.optString("status") == "laeuft" -> Ergebnis(
                "Die App Epigenetik recherchiert „${o.optString("name")}“ (${o.optString("typ")}) gerade im Hintergrund – meist 1–4 Minuten. " +
                    "Sag Frank Bescheid und lies später mit epigenetik_lesen nach (id ${o.optLong("id")}).",
            )
            o.has("gefunden") && !o.optBoolean("gefunden") -> Ergebnis(
                "„${o.optString("name")}“ ist in der App Epigenetik noch nicht recherchiert. Vorhanden: " +
                    (o.optJSONArray("vorhanden")?.let { l -> (0 until l.length()).joinToString { l.optString(it) } }?.ifBlank { "noch nichts" } ?: "noch nichts"),
            )
            o.optString("status") == "fehler" -> Ergebnis("Die Recherche zu „${o.optString("name")}“ ist gescheitert: ${o.optString("fehler")}", fehler = true)
            else -> Ergebnis(buildString {
                append("AUS DER APP EPIGENETIK (Stand ").append(o.optString("stand")).append(")")
                if (o.optBoolean("schon_vorhanden")) append(", war schon recherchiert")
                append(":\n").append(o.optString("bericht"))
            })
        }
    }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = AppBruecke.unterbrechbar { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject =
        AppBruecke.rufe(context, "Epigenetik", BRUECKE, methode, anfrage, lesend = methode in LESEND)

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.epigenetik.jarvis")
        private val LESEND = setOf("info", "lesen", "status", "liste", "schalter", "stack")
        /** Bleibt mit Reserve unter dem 75-Sekunden-Limit des Relays. */
        private const val WARTEN_MS = 50_000L
    }
}
