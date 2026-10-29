package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an „Geniale Ideen“ (de.frank.genialeideen) über deren Jarvis-Brücke (Signatur-Erlaubnis):
 * Ideen lesen (auch wie die Reiter und die Seitenleiste der App), neue anlegen (die App glättet Text und Titel mit ihrer KI),
 * ändern, annehmen, als umgesetzt markieren, löschen; Kurzfassung lesen, mit KI neu erzeugen und zurücknehmen.
 */
class IdeenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "ideen"
    override val name = "Geniale Ideen"
    override val beschreibung = "Ideen lesen (Reiter und Seitenleiste), neue speichern (mit KI-Glättung), ändern, annehmen, als umgesetzt markieren, " +
        "Kurzfassung mit KI verbessern oder zurücknehmen und löschen."
    override val hinweise =
        "Geniale Ideen ist Franks Ideensammlung. Eine Idee hat einen kurzen Titel (höchstens drei Wörter, bildet die App selbst), einen Text, " +
            "optional Kategorien, eine Kurzfassung (höchstens 35 Wörter, über der ausführlichen Fassung; sie wird vorgelesen) " +
            "und den Status offen, angenommen (von Frank für gut befunden, noch nicht umgesetzt) oder umgesetzt. Halbfertiges liegt als Entwurf. " +
            "Die App hat die Reiter Angenommen, Offen und Umgesetzt und in der Seitenleiste zusätzlich Entwürfe und Alle Ideen; ideen_lesen mit uebersicht nennt deren Zahlen. " +
            "„Mit KI verbessern“ und „Rückgängig“ der App gehen über idee_speichern mit ki_aktion. Neue Ideen übergibst du in Franks eigenen Worten; die App glättet den Text selbst " +
            "und behält das Original. Ideen sind etwas anderes als Aufgaben: Eine Idee ist ein Einfall zum Aufheben, eine Aufgabe etwas zu Erledigendes. " +
            "Sagt Frank „geniale Idee“ oder „Idee“, gehört es hierher. Vor dem Löschen kurz bestätigen lassen."

    /** Schützt vor doppelt angelegten Ideen, wenn ChatGPT einen Aufruf nach einer Zeitüberschreitung wiederholt. */
    private var letzteAnlage: Triple<String, Long, String>? = null
    /** Antwort, wenn ein Speichern ins Zeitlimit lief und unklar ist, ob die Idee angekommen ist. */
    private val UNKLAR = "Womöglich trotzdem gespeichert. Vor einem neuen Versuch mit ideen_lesen prüfen, nicht noch einmal speichern."
    private val anlageSperre = Mutex()

    override fun stoerung(): String? = rufeDirekt("kategorien", JSONObject()).optString("fehler").takeIf { it.isNotEmpty() }

    /**
     * Alle offenen Ideen als kompakter Text, für die Tagesdatenbank. Höchstens 14.000 Zeichen, gekürzt wird zeilenweise:
     * Zuerst werden die Texte kürzer, reicht das nicht, fallen die hinteren Ideen weg, und der Text sagt, wie viele.
     */
    suspend fun ueberblick(): String = rufe("lesen", JSONObject().put("status", "offen").put("limit", 200)).let { antwort ->
        antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let { "NICHT VERFÜGBAR: $it" } ?: run {
            val voll = liste(antwort, 160)
            if (voll.length <= UEBERBLICK_ZEICHEN) return@run voll
            val knapp = liste(antwort, 80)
            if (knapp.length <= UEBERBLICK_ZEICHEN) return@run knapp
            val zeilen = knapp.lines()
            val behalten = StringBuilder()
            var weggelassen = 0
            zeilen.forEachIndexed { n, zeile ->
                // Kopfzeile immer; danach nur, solange Platz für den Hinweis am Ende bleibt.
                if (n == 0 || (weggelassen == 0 && behalten.length + zeile.length + 1 <= UEBERBLICK_ZEICHEN - 120)) behalten.append(zeile).append('\n') else weggelassen++
            }
            behalten.append("… und ").append(weggelassen).append(" weitere offene Idee(n), vollständig über ideen_lesen.").toString()
        }
    }

    /**
     * Die offenen Ideen fürs Live-Gedächtnis: die 25 neuesten mit Text, alle älteren nur mit Titel und Datum.
     * So kennt Jarvis jede Idee, und der Text wächst mit Hunderten Ideen kaum. Volltext über ideen_lesen mit id.
     */
    suspend fun gedaechtnis(): String {
        val antwort = rufe("lesen", JSONObject().put("status", "offen").put("limit", 500))
        antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let { return "NICHT VERFÜGBAR: $it" }
        val ideen = antwort.optJSONArray("ideen") ?: JSONArray()
        if (ideen.length() == 0) return "Keine offenen Ideen."
        val liste = (0 until ideen.length()).map { ideen.getJSONObject(it) }.sortedByDescending { it.optLong("id") }
        return buildString {
            append(antwort.optInt("anzahl", liste.size)).append(" offene Idee(n), neueste zuerst:\n")
            liste.forEachIndexed { n, i ->
                append("- ").append(kopf(i))
                if (n < 25) append(": ").append(i.optString("text").replace(Regex("\\s+"), " ").let { if (it.length > 260) it.take(260) + " …" else it })
                append('\n')
            }
        }.trim().let { if (it.length > 9_000) it.take(9_000).substringBeforeLast('\n') + "\n… (weitere mit ideen_lesen)" else it }
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            w("ideen_lesen").als(
                beschreibung = "Jarvis: liest Franks geniale Ideen aus der App Geniale Ideen. Nutze es für „Welche Ideen habe ich?“, „Was war meine Idee zu …?“, " +
                    "„Welche Ideen sind umgesetzt?“, „Welche Ideen habe ich angenommen?“, „Was steht in den Entwürfen?“. In der Liste sind lange Texte gekürzt; " +
                    "mit id kommt eine einzelne Idee vollständig mit Kurzfassung, samt dem bisherigen Gespräch dazu. " +
                    "Reiter und Seitenleiste der App: Angenommene = status angenommen, Offene = nur_offen, Umgesetzte = umgesetzt, Entwürfe = entwurf, Alle Ideen = alle mit entwuerfe; " +
                    "uebersicht nennt die Zahl je Reiter.",
                schema = w("ideen_lesen").schema.mit("id" to zahl("id einer Idee aus der Liste: dann nur diese, im Volltext.")),
            ) { a -> (if (a.idGesetzt()) w("idee_lesen") else w("ideen_lesen")).ausfuehren(a) },
            Werkzeug(
                name = "idee_speichern",
                titel = "Idee speichern oder ändern",
                beschreibung = "Jarvis: speichert eine neue geniale Idee in der App Geniale Ideen oder ändert eine vorhandene. " +
                    "NEU (ohne id und ohne suche): bei „Jarvis, ich habe eine Idee …“, „speichere als geniale Idee …“. Übergib den Inhalt vollständig in Franks Worten; die App glättet den Text sprachlich und bildet den Titel selbst. " +
                    "ÄNDERN (mit id oder suche): neuer Text oder Titel, andere Kategorie, oder status angenommen, umgesetzt bzw. wieder offen. " +
                    "KI (mit id oder suche und ki_aktion): verbessern wie der Knopf „Mit KI verbessern“ (ausführliche Fassung und Kurzfassung neu), " +
                    "kurzfassung (nur die Kurzfassung neu, Text bleibt), rueckgaengig (holt die Fassung vor der letzten Verbesserung zurück).",
                schema = schema(
                    "id" to zahl("Nur beim Ändern: id der Idee aus ideen_lesen."),
                    "suche" to text("Nur beim Ändern, statt id: Wort aus Titel oder Text der Idee."),
                    "text" to text("Die Idee vollständig, so wie Frank sie gesagt hat, ohne die Anrede an Jarvis. Pflicht bei einer neuen Idee."),
                    "titel" to text("Nur wenn Frank ausdrücklich einen Titel nennt (höchstens drei Wörter)."),
                    "kategorie" to text("Nur wenn Frank eine Kategorie nennt. Unbekannte Namen werden neu angelegt; ohne = Kategorie entfernen."),
                    "status" to text("Nur beim Ändern: offen, angenommen oder umgesetzt.", listOf("offen", "angenommen", "umgesetzt")),
                    "verbessern" to schalter("Nur bei einer neuen Idee: Text von der KI glätten lassen (Vorgabe: an)."),
                    "ki_aktion" to text(
                        "Nur bei einer vorhandenen Idee: verbessern (beide Fassungen mit KI verbessern), kurzfassung (nur die Kurzfassung neu erzeugen) " +
                            "oder rueckgaengig (vorige Fassung zurückholen). Dauert bis etwa eine Minute.",
                        listOf("verbessern", "kurzfassung", "rueckgaengig"),
                    ),
                ),
                nurLesen = false,
            ) { a ->
                // „Markiere die Idee X als umgesetzt“ kommt oft ohne id und suche, nur mit Titel oder Text der Idee.
                // Das ist eine Änderung, keine neue Idee: sonst entstünde ein Doppel oder der Fehler „braucht einen Text“.
                val nurAenderung = a.gesetzt("status") || (!a.gesetzt("text") && (a.gesetzt("titel") || a.has("kategorie")))
                when {
                    a.gesetzt("ki_aktion") -> {
                        val ohneId = !a.idGesetzt() && !a.gesetzt("suche")
                        val suche = a.optString("titel").trim().ifEmpty { a.optString("text").trim() }
                        if (ohneId && suche.isEmpty()) Ergebnis("Für ki_aktion fehlt die id oder ein Suchwort (suche) für die Idee. Hole die id mit ideen_lesen.", fehler = true)
                        else {
                            // Ohne id dienen Titel oder Text als Suchwort; Status und Kategorie im selben Aufruf gelten trotzdem.
                            val ziel = if (ohneId) JSONObject(a.toString()).put("suche", suche).apply { remove("titel"); remove("text") } else a
                            mitIdee(ziel) { id ->
                                val aenderung = ziel.gesetzt("status") || ziel.gesetzt("text") || ziel.gesetzt("titel") || ziel.has("kategorie")
                                val vorher = if (!aenderung) null else w("idee_aendern").ausfuehren(
                                    JSONObject(ziel.toString()).put("id", id).apply { remove("suche"); remove("ki_aktion"); remove("verbessern") },
                                )
                                if (vorher?.fehler == true) vorher
                                else kiAktion(id, a.optString("ki_aktion").trim().lowercase()).let { ki -> if (vorher == null) ki else Ergebnis(vorher.text + "\n" + ki.text, fehler = ki.fehler) }
                            }
                        }
                    }
                    a.idGesetzt() || a.gesetzt("suche") -> w("idee_aendern").ausfuehren(a)
                    nurAenderung -> {
                        val suche = a.optString("titel").trim().ifEmpty { a.optString("text").trim() }
                        if (suche.isEmpty()) Ergebnis("Zum Ändern fehlt die id oder ein Suchwort (suche) für die Idee. Hole die id mit ideen_lesen.", fehler = true)
                        else w("idee_aendern").ausfuehren(JSONObject(a.toString()).put("suche", suche).apply { remove("titel"); remove("text") })
                    }
                    else -> w("idee_anlegen").ausfuehren(a)
                }
            },
            w("idee_loeschen"),
        )
    }

    private fun w(name: String): Werkzeug = einzeln.first { it.name == name }

    private val einzeln: List<Werkzeug> = listOf(
        Werkzeug(
            name = "ideen_lesen",
            titel = "Ideen lesen",
            beschreibung = "Jarvis: liest Franks geniale Ideen aus der App Geniale Ideen. Nutze es für „Welche Ideen habe ich?“, „Was war meine Idee zu …?“, " +
                "„Welche Ideen sind umgesetzt?“. Lange Texte sind gekürzt; den vollen Text einer Idee liefert idee_lesen.",
            schema = schema(
                "status" to text(
                    "offen (Vorgabe: offene und angenommene, also alles noch nicht Umgesetzte), angenommen, nur_offen (wie der Reiter Offen, ohne angenommene), " +
                        "umgesetzt, entwurf oder alle (ohne Entwürfe).",
                    listOf("offen", "angenommen", "nur_offen", "umgesetzt", "entwurf", "alle"),
                ),
                "suche" to text("Suchwort oder Wortgruppe; durchsucht Titel und Text."),
                "kategorie" to text("Nur Ideen dieser Kategorie (Name oder Teil davon)."),
                "entwuerfe" to schalter("Entwürfe mitnehmen (Vorgabe: aus). Mit status alle wie die Seitenleiste „Alle Ideen“."),
                "uebersicht" to schalter("Zahl der Ideen je Reiter und Seitenleiste (Angenommen, Offen, Umgesetzt, Entwürfe, Alle). Mit status zusätzlich diese Liste."),
            ),
            nurLesen = true,
        ) { a ->
            if (!a.optBoolean("uebersicht")) return@Werkzeug rufe("lesen", a).fehlerOder(::liste)
            val zahlen = rufe("uebersicht", JSONObject())
            zahlen.optString("fehler").takeIf { it.isNotEmpty() }?.let { return@Werkzeug Ergebnis(it, fehler = true) }
            val kopf = "Reiter und Seitenleiste in Geniale Ideen: Angenommene Ideen ${zahlen.optInt("angenommen")}, Offene Ideen ${zahlen.optInt("offen")}, " +
                "Umgesetzte Ideen ${zahlen.optInt("umgesetzt")}, Entwürfe ${zahlen.optInt("entwuerfe")}, Alle Ideen ${zahlen.optInt("alle")}."
            if (!a.gesetzt("status") && !a.gesetzt("suche") && !a.gesetzt("kategorie")) return@Werkzeug Ergebnis(kopf)
            rufe("lesen", a).fehlerOder { kopf + "\n\n" + liste(it) }
        },
        Werkzeug(
            name = "idee_lesen",
            titel = "Idee im Volltext",
            beschreibung = "Jarvis: liest eine einzelne Idee aus Geniale Ideen vollständig, samt dem bisherigen Gespräch dazu. Über id ODER Suchwort.",
            schema = schema("id" to zahl("id der Idee aus ideen_lesen."), "suche" to text("Statt id: Wort aus Titel oder Text.")),
            nurLesen = true,
        ) { a ->
            mitIdee(a) { id ->
                rufe("idee", JSONObject().put("id", id)).fehlerOder { antwort ->
                    val i = antwort.getJSONObject("idee")
                    buildString {
                        append(kopf(i)).append('\n')
                        i.optString("kurzfassung").takeIf { it.isNotBlank() }?.let { append("Kurzfassung: ").append(it).append("\n\n") }
                        append(i.optString("text"))
                        if (i.optBoolean("rueckgaengig_moeglich")) append("\n\n(Die Fassung vor der letzten KI-Verbesserung lässt sich mit idee_speichern, ki_aktion rueckgaengig zurückholen.)")
                        val gespraech = antwort.optJSONArray("gespraech") ?: JSONArray()
                        if (gespraech.length() > 0) {
                            append("\n\nBisheriges Gespräch zur Idee:\n")
                            for (n in 0 until gespraech.length()) gespraech.getJSONObject(n).let { append(if (it.optString("rolle") == "user") "Frank: " else "KI: ").append(it.optString("text")).append('\n') }
                        }
                    }.trim()
                }
            }
        },
        Werkzeug(
            name = "idee_anlegen",
            titel = "Idee speichern",
            beschreibung = "Jarvis: speichert eine neue geniale Idee in der App Geniale Ideen. Nutze es bei „Jarvis, ich habe eine Idee …“, „speichere als geniale Idee …“, " +
                "„notiere die Idee …“. Übergib den Inhalt vollständig in Franks Worten; die App glättet den Text sprachlich und bildet den Titel selbst.",
            schema = schema(
                "text" to text("Die Idee vollständig, so wie Frank sie gesagt hat, ohne die Anrede an Jarvis."),
                "titel" to text("Nur wenn Frank ausdrücklich einen Titel nennt (höchstens drei Wörter). Sonst weglassen."),
                "kategorie" to text("Nur wenn Frank eine Kategorie nennt. Unbekannte Namen werden als neue Kategorie angelegt."),
                "verbessern" to schalter("Text von der KI glätten lassen (Vorgabe: an). Aus = wörtlich speichern."),
                pflicht = listOf("text"),
            ),
            nurLesen = false,
        ) { a ->
            val schluessel = a.optString("text").trim().lowercase()
            // Gesperrt, weil der Tunnel Aufrufe parallel abarbeitet: Ein wiederholter Aufruf wartet, bis der erste fertig ist.
            anlageSperre.withLock {
                letzteAnlage?.let { (alt, zeit, antwort) ->
                    if (alt == schluessel && System.currentTimeMillis() - zeit < 90_000)
                        return@Werkzeug if (antwort == UNKLAR) Ergebnis(antwort, fehler = true) else Ergebnis("$antwort (War bereits gespeichert, nicht doppelt angelegt.)")
                }
                val antwort = rufe("anlegen", a)
                // Zeitlimit: Die Idee kam womöglich trotzdem an. Schlüssel merken, damit ein Wiederholungsversuch nicht doppelt speichert.
                if (antwort.optBoolean(AppBruecke.ZEITLIMIT)) {
                    letzteAnlage = Triple(schluessel, System.currentTimeMillis(), UNKLAR)
                    return@Werkzeug Ergebnis(antwort.optString("fehler") + " " + UNKLAR, fehler = true)
                }
                antwort.fehlerOder { "Idee gespeichert: „${it.getJSONObject("idee").optString("titel")}“ (id ${it.getJSONObject("idee").optLong("id")}). ${it.optString("hinweis")}" }.also {
                    if (!it.fehler) letzteAnlage = Triple(schluessel, System.currentTimeMillis(), it.text)
                }
            }
        },
        Werkzeug(
            name = "idee_aendern",
            titel = "Idee ändern",
            beschreibung = "Jarvis: ändert eine Idee in Geniale Ideen: neuer Text oder Titel, andere Kategorie, oder Status angenommen, umgesetzt bzw. wieder offen. Über id ODER Suchwort.",
            schema = schema(
                "id" to zahl("id der Idee aus ideen_lesen."),
                "suche" to text("Statt id: Wort aus Titel oder Text."),
                "text" to text("Neuer vollständiger Text."),
                "titel" to text("Neuer Titel."),
                "kategorie" to text("Neue Kategorie; ohne = Kategorie entfernen."),
                "status" to text("offen, angenommen oder umgesetzt.", listOf("offen", "angenommen", "umgesetzt")),
            ),
            nurLesen = false,
        ) { a ->
            mitIdee(a) { id -> rufe("aendern", JSONObject(a.toString()).put("id", id).apply { remove("suche") }).fehlerOder { "Geändert: " + kopf(it.getJSONObject("idee")) } }
        },
        Werkzeug(
            name = "idee_loeschen",
            titel = "Idee löschen",
            beschreibung = "Jarvis: löscht eine Idee endgültig aus Geniale Ideen. Nur aufrufen, wenn Frank das Löschen eindeutig verlangt hat. Über id ODER Suchwort.",
            schema = schema("id" to zahl("id der Idee aus ideen_lesen."), "suche" to text("Statt id: Wort aus Titel oder Text.")),
            nurLesen = false,
            loeschend = true,
        ) { a -> mitIdee(a) { id -> rufe("loeschen", JSONObject().put("id", id)).fehlerOder { "Gelöscht: „${it.getJSONObject("geloescht").optString("titel")}“." } } },
    )

    /** „Mit KI verbessern“, nur die Kurzfassung neu oder „Rückgängig“, wie in der Einzelansicht der App. */
    private suspend fun kiAktion(id: Long, aktion: String): Ergebnis {
        val antwort = when (aktion) {
            "verbessern" -> rufe("verbessern", JSONObject().put("id", id).put("warte_ms", KI_WARTE_MS))
            "kurzfassung" -> rufe("verbessern", JSONObject().put("id", id).put("nur_kurzfassung", true).put("warte_ms", KI_WARTE_MS))
            "rueckgaengig" -> rufe("rueckgaengig", JSONObject().put("id", id))
            else -> return Ergebnis("Unbekannte ki_aktion „$aktion“: verbessern, kurzfassung oder rueckgaengig.", fehler = true)
        }
        if (antwort.optBoolean("laeuft")) return Ergebnis(antwort.optString("hinweis"))
        return antwort.fehlerOder { r ->
            val i = r.getJSONObject("idee")
            buildString {
                append(r.optString("hinweis")).append(' ').append(kopf(i)).append('\n')
                append("Kurzfassung: ").append(i.optString("kurzfassung").ifBlank { "(noch keine)" })
                if (aktion != "kurzfassung") append("\n\nAusführliche Fassung:\n").append(i.optString("text"))
            }
        }
    }

    private suspend fun mitIdee(a: JSONObject, dann: suspend (Long) -> Ergebnis): Ergebnis {
        // id 0 heißt bei vielen Modellen „nicht angegeben“: dann gilt das Suchwort.
        if (a.idGesetzt()) return dann(a.optLong("id"))
        val suche = a.optString("suche").trim()
        if (suche.isEmpty()) return Ergebnis("Es fehlt die id oder ein Suchwort für die Idee.", fehler = true)
        var treffer = rufe("lesen", JSONObject().put("status", "alle").put("suche", suche))
        treffer.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis(it, fehler = true) }
        // Erst ohne Entwürfe wie bisher; findet sich nichts, auch unter den Entwürfen suchen.
        if ((treffer.optJSONArray("ideen")?.length() ?: 0) == 0) {
            treffer = rufe("lesen", JSONObject().put("status", "alle").put("entwuerfe", true).put("suche", suche))
            treffer.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis(it, fehler = true) }
        }
        val ideen = treffer.optJSONArray("ideen") ?: JSONArray()
        return when (ideen.length()) {
            0 -> Ergebnis("Keine Idee gefunden, die zu „$suche“ passt.", fehler = true)
            1 -> dann(ideen.getJSONObject(0).getLong("id"))
            else -> Ergebnis("Mehrere Ideen passen zu „$suche“. Frage Frank, welche gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + liste(treffer), fehler = true)
        }
    }

    private fun kopf(i: JSONObject): String = buildString {
        append("[id ").append(i.optLong("id")).append("] ").append(i.optString("titel")).append(" (").append(i.optString("status"))
        val kategorien = i.optJSONArray("kategorien")
        if (kategorien != null && kategorien.length() > 0) append(", ").append((0 until kategorien.length()).joinToString("/") { kategorien.optString(it) })
        append(", angelegt ").append(i.optString("angelegt")).append(")")
    }

    private fun liste(antwort: JSONObject, laenge: Int = Int.MAX_VALUE): String {
        val ideen = antwort.optJSONArray("ideen") ?: JSONArray()
        if (ideen.length() == 0) return "Keine Ideen gefunden."
        return buildString {
            append(antwort.optInt("anzahl")).append(" Idee(n)")
            if (ideen.length() < antwort.optInt("anzahl")) append(", gezeigt die ersten ").append(ideen.length())
            append(":\n")
            for (n in 0 until ideen.length()) {
                val i = ideen.getJSONObject(n)
                append("- ").append(kopf(i)).append(": ").append(i.optString("text").replace('\n', ' ').let { if (it.length > laenge) it.take(laenge) + " …" else it })
                // Nur in kurzen Werkzeug-Listen; lange Listen, Überblick und Gedächtnis bleiben knapp (Kurzfassung dann mit id).
                if (laenge == Int.MAX_VALUE && ideen.length() <= KURZFASSUNG_IN_LISTE) i.optString("kurzfassung").takeIf { it.isNotBlank() }?.let { append(" | Kurzfassung: ").append(it) }
                append('\n')
            }
        }.trim()
    }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = AppBruecke.unterbrechbar { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject =
        AppBruecke.rufe(context, "Geniale Ideen", BRUECKE, methode, anfrage, lesend = methode in LESEND)

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private inline fun JSONObject.fehlerOder(gut: (JSONObject) -> String): Ergebnis =
        optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(gut(this))

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.genialeideen.jarvis")
        private val LESEND = setOf("kategorien", "lesen", "idee", "uebersicht")
        /** Die KI braucht für beide Fassungen oft länger als die 25 s eines Schreibaufrufs; unter der Relay-Grenze von 75 s. */
        private const val KI_WARTE_MS = 50_000L
        private const val KURZFASSUNG_IN_LISTE = 25
        private const val UEBERBLICK_ZEICHEN = 14_000
    }
}
