package de.frank.jarvis.mcp

import android.content.Context
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.agent.Gehirn
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.faehigkeit.zahl
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.auswertung.Zeitplan
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.schalter
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.AblageKarte
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.merkKontext
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.Werkzeug
import de.frank.jarvis.faehigkeit.schema
import de.frank.jarvis.faehigkeit.text
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Der MCP-Server von Jarvis (Model Context Protocol, Streamable HTTP, zustandslos, nur JSON-Antworten).
 * ChatGPT ruft ihn über den Tunnel auf. Er kennt genau vier Dinge: initialize, ping, tools/list, tools/call.
 * Es gibt bewusst keine Sitzungen — ChatGPT beginnt oft bei jedem Aufruf neu.
 */
class McpServer(context: Context) {
    private val app = context.applicationContext
    private val agent by lazy { JarvisAgent(app) }  // nur noch für die Prüfung, ob das Modell verbunden ist

    /** Werkzeuge von Jarvis selbst, zusätzlich zu denen der angebundenen Apps. */
    private val eigene: List<Werkzeug> = listOf(
        Werkzeug(
            name = "jarvis_kontext",
            titel = "Gesamtbild von Frank holen",
            beschreibung = "Jarvis: liefert in EINEM Aufruf das Gesamtbild, mit dem Jarvis jede Frage von Frank betrachtet: Franks Regeln, was Jarvis über Frank weiß, " +
                "den Rahmen der nächsten Tage (Dienst, Schlaf- und freie Zeiten), die Termine von heute und morgen, die offenen Aufgaben und die aktuelle Tagesauswertung " +
                "(Biodaten, Wetter, Trainings, Tagebuch und Ideen schon gedeutet). " +
                "DAS ERSTE WERKZEUG IN JEDEM GESPRÄCH: Rufe es auf, bevor du Franks erste Frage beantwortest, auch wenn sie einfach wirkt, und noch einmal, wenn das Gespräch länger als eine Stunde läuft. " +
                "Betrachte danach jede Frage vor diesem Hintergrund und beziehe von dir aus ein, was die Antwort besser macht. Die einzelnen Apps fragst du nur noch für Details oder Änderungen ab.",
            schema = schema(),
            nurLesen = true,
        ) { _ -> Gehirn.zuletztGeholt = System.currentTimeMillis(); Ergebnis(Gehirn.kontext(app)) },
        Werkzeug(
            name = "tagesauswertung_lesen",
            titel = "Tagesauswertung lesen",
            beschreibung = "Jarvis: liefert Franks fertige Tagesauswertung. Jarvis schreibt sie selbst im Hintergrund neu, in der Regel jede Stunde (außer wenn Frank schläft), aus allen angebundenen Apps: " +
                "RÜCKBLICK (letzte Tage: Dienste, Schlaf und Erholung, Trainings, Tagebuch), AKTUELL (was für ein Tag heute ist, Termine, Biodaten im Vergleich und bezogen auf den Dienst, " +
                "Wetter, Aufgaben, Einschätzung, Empfehlungen, passende Ideen) und AUSBLICK (nächste Tage). Die offenen Aufgaben werden bei jedem Abruf zusätzlich frisch angehängt. " +
                "Nutze es, wenn Frank die Auswertung selbst hören will („Wie ist meine Tagesauswertung?“, „Guten Morgen Jarvis“), eine neue anstoßen möchte oder du den Datenanhang brauchst. " +
                "Als Hintergrund für andere Fragen steckt sie schon in jarvis_kontext.",
            schema = schema(
                "mit_daten" to schalter("true = zusätzlich der vollständige Datenanhang mit allen Einzelwerten (Vorgabe: false, die Auswertung genügt meist)."),
                "neu_erstellen" to schalter("true = jetzt eine neue Auswertung anstoßen (frische Daten aller Apps, ein bis drei Minuten im Hintergrund) und danach erneut abrufen. Nur wenn Frank ausdrücklich eine neue oder aktualisierte Auswertung verlangt."),
            ),
            nurLesen = true,
        ) { a ->
            if (a.optBoolean("neu_erstellen")) {
                return@Werkzeug if (Tagesauswertung.stand.value.laeuft) Ergebnis("Eine Tagesauswertung läuft bereits. In ein bis zwei Minuten erneut abrufen (ohne neu_erstellen).")
                else { JarvisDienst.auswerten(app, "auf Wunsch über ChatGPT"); Ergebnis("Die neue Tagesauswertung wird jetzt erstellt. In ein bis drei Minuten erneut abrufen (ohne neu_erstellen).") }
            }
            var neueste = Tagesauswertung.neueste(app)
            var hinweis = ""
            if (neueste != null && Tagesauswertung.veraltet(app) && !Tagesauswertung.stand.value.laeuft) {
                // Ein geplanter Lauf fehlt: sofort frische Daten liefern und die volle Auswertung nachziehen.
                neueste = Tagesauswertung.datenbericht(app, "beim Abruf erstellt, weil der geplante Lauf fehlte")
                JarvisDienst.auswerten(app, "nachgeholt beim Abruf")
                hinweis = "HINWEIS: Die letzte gedeutete Auswertung war veraltet. Dies sind frische Daten ohne Deutung; die vollständige Auswertung ist in ein bis drei Minuten abrufbar.\n"
            }
            val aufgaben = buildString {
                append("--- AUFGABEN (frisch gelesen um ").append(java.time.LocalTime.now().toString().take(5)).append(" Uhr) ---\n")
                val lesen = Register.werkzeuge(app).firstOrNull { it.name == "aufgaben_lesen" }
                for (bereich in listOf("heute", "morgen")) {
                    append(if (bereich == "heute") "Heute und überfällig: " else "Morgen: ")
                    append(runCatching { lesen?.ausfuehren(JSONObject().put("bereich", bereich))?.text }.getOrNull() ?: "nicht lesbar").append("\n")
                }
            }
            val naechster = Zeitplan.naechster(app)?.let { "Nächste automatische Auswertung: ${it.toLocalTime().toString().take(5)} Uhr." } ?: "Die automatische Synchronisation ist ausgeschaltet."
            if (neueste == null) {
                JarvisDienst.auswerten(app, "erster Abruf")
                Ergebnis("Es gibt noch keine Tagesauswertung. Jarvis erstellt gerade die erste, das dauert ein bis drei Minuten; bitte danach noch einmal abrufen. $naechster\n\n$aufgaben")
            } else {
                val daten = a.optBoolean("mit_daten", false)
                // Die Aufgaben im Datenanhang sind der alte Stand: weglassen, die frischen stehen darunter.
                val fassung = neueste.copy(daten = neueste.daten.substringBefore("== OFFENE AUFGABEN").trim())
                Ergebnis(hinweis + Tagesauswertung.alsText(fassung, daten) + "\n" + aufgaben + naechster)
            }
        },
        Werkzeug(
            name = "tagesdaten_lesen",
            titel = "Tagesdatenbank lesen",
            beschreibung = "Jarvis: liest die Tagesdatenbank, also die Daten aller angebundenen Apps so, wie Jarvis sie bei der letzten Synchronisierung abgelegt hat: " +
                "Rahmen der nächsten Tage (Dienst, Schlaf- und freie Zeiten), Termine, Biodaten des Tages, Biodaten im Vergleich, Trainings und alle offenen genialen Ideen. " +
                "Ein Aufruf statt vieler einzelner. Nutze es, wenn du Rohdaten zum Nachdenken oder für Nachfragen brauchst. Aufgaben sind nicht enthalten, die liest du mit aufgaben_lesen frisch.",
            schema = schema("bereich" to text("Ein Bereich oder alle (Vorgabe).", listOf("alle") + Tagesauswertung.BEREICHE.keys)),
            nurLesen = true,
        ) { a ->
            val neueste = Tagesauswertung.neueste(app)
            if (neueste == null) { JarvisDienst.auswerten(app, "erster Abruf"); Ergebnis("Die Tagesdatenbank wird gerade zum ersten Mal gefüllt. In ein bis drei Minuten erneut abrufen.") }
            else Ergebnis(Tagesauswertung.tagesdaten(neueste, a.optString("bereich", "alle")))
        },
        Werkzeug(
            name = "agenten",
            titel = "Agenten ansehen und verwalten",
            beschreibung = "Jarvis: die Agenten von Jarvis. Ohne aktion (oder aktion=liste) kommt die Liste der vorhandenen Agenten (zum Beispiel Recherche, Machbarkeit und selbst angelegte) und wer gerade arbeitet. " +
                "Mit aktion=anlegen baust du einen neuen Agenten oder änderst einen: ein Name plus eine Rolle, also die Anweisung, wie er an Aufträge herangeht und wie sein Ergebnis aussieht – " +
                "wenn Frank sich einen Spezialisten wünscht (zum Beispiel „Trainingsplaner“, „App-Architekt“); formuliere die Rolle selbst ausführlich. Mit aktion=loeschen entfernst du einen selbst angelegten Agenten. " +
                "Beauftragt wird ein Agent mit agent_starten.",
            schema = schema(
                "aktion" to text("liste (Vorgabe), anlegen oder loeschen.", listOf("liste", "anlegen", "loeschen")),
                "name" to text("Bei anlegen und loeschen: Name des Agenten, ein bis zwei Wörter."),
                "rolle" to text("Bei anlegen: die Rolle in fünf bis zehn Sätzen: Aufgabe, Vorgehen in Schritten, worauf zu achten ist, Form des Ergebnisses."),
            ),
            nurLesen = false,
        ) { a ->
            when (a.optString("aktion", "liste").lowercase()) {
                "anlegen" ->
                    if (a.optString("name").isBlank() || a.optString("rolle").length < 40) Ergebnis("Name und eine ausführliche Rolle sind nötig.", fehler = true)
                    else Agenten.speichere(app, a.optString("name"), a.optString("rolle")).let { Ergebnis("Agent „${it.name}“ ist angelegt und kann mit agent_starten beauftragt werden.") }
                "loeschen" -> if (Agenten.loesche(app, a.optString("name"))) Ergebnis("Agent „${a.optString("name")}“ gelöscht.") else Ergebnis("Kein selbst angelegter Agent mit diesem Namen.", fehler = true)
                else -> {
                    val laufend = Agenten.laeufe.value
                    Ergebnis(buildString {
                        append("Agenten von Jarvis:\n")
                        Agenten.alle(app).forEach { append("- ").append(it.name).append(if (it.vorgegeben) " (eingebaut)" else " (selbst angelegt)").append(": ").append(it.rolle.take(220)).append('\n') }
                        if (laufend.isEmpty()) append("Gerade arbeitet kein Agent.")
                        else laufend.forEach { append("Arbeitet gerade: ").append(it.agent).append(" seit ").append((System.currentTimeMillis() - it.seit) / 60_000).append(" Minuten an „").append(it.auftrag.take(100)).append("“").append(if (it.schritt.isEmpty()) "" else ", aktueller Schritt: ${it.schritt}").append('\n') }
                    }.trim())
                }
            }
        },
        Werkzeug(
            name = "agent_starten",
            titel = "Agenten beauftragen",
            beschreibung = "Jarvis: beauftragt einen Agenten von Jarvis mit einer längeren Arbeit, zum Beispiel einer Internet-Recherche („Jarvis, recherchiere, wie man … am besten baut“) " +
                "oder einer Machbarkeitsprüfung einer Idee, oder den Agenten Code-Analyse mit der Auswertung einer App in Franks Repo (er liest den Code und liefert den Stand samt fertigem Auftrag für ein Programmier-Werkzeug; er ändert nichts). Der Agent arbeitet mehrere Minuten selbstständig im Hintergrund auf Franks Handy (eigenes Modell, Internet-Suche, Zugriff auf Ideen, " +
                "Aufgaben, Kalender, Biomarker), legt den Bericht in die Ablage und meldet sich per Benachrichtigung. Dieser Aufruf kehrt sofort zurück. " +
                "Das Ergebnis holst du später mit ablage_lesen (ohne titel die Liste, mit titel der Bericht). Mit per_mail wird der Bericht zusätzlich an Frank gemailt.",
            schema = schema(
                "agent" to text("Name des Agenten, zum Beispiel Recherche oder Machbarkeit (siehe agenten)."),
                "auftrag" to text("Der vollständige Auftrag mit allem Wissen aus dem Gespräch: Ziel, Rahmen, was Frank wichtig ist, gewünschte Form des Ergebnisses."),
                "per_mail" to schalter("true = den fertigen Bericht zusätzlich per E-Mail an Frank schicken."),
                "vorlesen" to schalter("true = Jarvis liest den fertigen Bericht auf dem Handy laut vor, sobald er da ist."),
                pflicht = listOf("agent", "auftrag"),
            ),
            nurLesen = false,
        ) { a ->
            val plan = Agenten.finde(app, a.optString("agent"))
            when {
                plan == null -> Ergebnis("Diesen Agenten gibt es nicht. Vorhanden: " + Agenten.alle(app).joinToString { it.name } + ". Mit agenten (aktion=anlegen) lässt sich ein neuer bauen.", fehler = true)
                a.optString("auftrag").isBlank() -> Ergebnis("Der Auftrag ist leer.", fehler = true)
                !agent.verbunden -> Ergebnis("Jarvis ist nicht mit seinem Modell verbunden. Frank muss sich in Jarvis unter Einstellungen bei ChatGPT anmelden.", fehler = true)
                Agenten.laeufe.value.size >= 2 -> Ergebnis("Es arbeiten schon zwei Agenten. Bitte warten, bis einer fertig ist.", fehler = true)
                else -> {
                    JarvisDienst.agentStarten(app, plan.name, a.optString("auftrag"), a.optBoolean("per_mail"), a.optBoolean("vorlesen"))
                    Ergebnis("Der Agent ${plan.name} arbeitet jetzt. Das dauert meist drei bis zehn Minuten. Der Bericht landet in der Ablage" + (if (a.optBoolean("per_mail")) " und kommt per E-Mail" else "") + "; Frank bekommt eine Benachrichtigung.")
                }
            }
        },
        Werkzeug(
            name = "jarvis_status",
            titel = "Jarvis-Status",
            beschreibung = "Jarvis: sagt, ob Jarvis auf Franks Handy erreichbar ist, welche Apps angebunden sind und welches Datum und welche Uhrzeit dort gerade gelten. " +
                "Nutze es bei „Jarvis, bist du da?“ oder wenn du das heutige Datum auf dem Handy brauchst. Mit funktionen=true kommt zusätzlich die vollständige, tatsächliche " +
                "Liste aller Werkzeuge von Jarvis (Grundlage zum Beispiel für eine Übersicht oder Infografik über Jarvis' Funktionen).",
            schema = schema("funktionen" to schalter("true = alle Werkzeuge mit Kurzbeschreibung auflisten.")),
            nurLesen = true,
        ) { it ->
            val jetzt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
            val apps = Register.alle(app).joinToString("; ") { f -> f.name + ": " + (f.stoerung()?.let { "gestört ($it)" } ?: "bereit") }
            val dienst = Register.alle(app).filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.heuteKurz().orEmpty()
            val funktionen = if (!it.optBoolean("funktionen")) "" else buildString {
                append("\n\nFUNKTIONEN VON JARVIS (Version ").append(BuildConfig.VERSION_NAME).append("):\n")
                Register.alle(app).forEach { f ->
                    append(f.name).append(if (f.imPlugin) "" else " (nur in Jarvis selbst und für seine Agenten)").append(": ").append(f.beschreibung).append('\n')
                    f.werkzeuge.forEach { w -> append("  - ").append(w.titel).append('\n') }
                }
                append("Jarvis selbst: ").append(eigene.joinToString(", ") { w -> w.titel }).append('\n')
            }
            Ergebnis("Jarvis ist bereit. Auf dem Handy ist es $jetzt. " + (if (dienst.isEmpty()) "" else "Dienst heute: $dienst. ") + "Angebundene Apps: $apps." + funktionen)
        },
    )

    private fun alleWerkzeuge(): List<Werkzeug> = Register.pluginWerkzeuge(app) + eigene

    /**
     * Verarbeitet einen HTTP-Rumpf. Rückgabe: Antwort-JSON oder null, wenn nichts zu antworten ist
     * (reine Benachrichtigung → HTTP 202).
     */
    fun verarbeite(rumpf: String): String? {
        val anfang = rumpf.trimStart()
        if (anfang.startsWith("[")) {
            val stapel = runCatching { JSONArray(anfang) }.getOrElse { return fehler(JSONObject.NULL, -32700, "Ungültiges JSON").toString() }
            val antworten = (0 until stapel.length()).mapNotNull { i -> stapel.optJSONObject(i)?.let(::eine) }
            return if (antworten.isEmpty()) null else JSONArray(antworten).toString()
        }
        val anfrage = runCatching { JSONObject(anfang) }.getOrElse { return fehler(JSONObject.NULL, -32700, "Ungültiges JSON").toString() }
        return eine(anfrage)?.toString()
    }

    private fun eine(anfrage: JSONObject): JSONObject? {
        val methode = anfrage.optString("method")
        // Ohne id ist es eine Benachrichtigung (z. B. notifications/initialized): keine Antwort.
        if (!anfrage.has("id") || anfrage.isNull("id")) return null
        val id = anfrage.get("id")
        val parameter = anfrage.optJSONObject("params") ?: JSONObject()
        return when (methode) {
            "initialize" -> ergebnis(id, JSONObject()
                .put("protocolVersion", parameter.optString("protocolVersion").takeIf { it in VERSIONEN } ?: VERSIONEN.first())
                .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)).put("resources", JSONObject()))
                .put("serverInfo", JSONObject().put("name", "Jarvis").put("title", "Jarvis").put("version", BuildConfig.VERSION_NAME))
                .put("instructions", anleitung()))
            "ping" -> ergebnis(id, JSONObject())
            "tools/list" -> ergebnis(id, JSONObject().put("tools", JSONArray(alleWerkzeuge().map(::beschreibe))))
            "tools/call" -> ergebnis(id, rufe(parameter.optString("name"), parameter.optJSONObject("arguments") ?: JSONObject()))
            // Einzige Ressource: die Karte, mit der ChatGPT Dateien aus der Ablage anzeigt.
            "resources/list" -> ergebnis(id, AblageKarte.liste())
            "resources/read" -> AblageKarte.lies(parameter.optString("uri"))?.let { ergebnis(id, it) } ?: fehler(id, -32002, "Ressource nicht gefunden")
            "resources/templates/list" -> ergebnis(id, JSONObject().put("resourceTemplates", JSONArray()))
            "prompts/list" -> ergebnis(id, JSONObject().put("prompts", JSONArray()))
            else -> fehler(id, -32601, "Unbekannte Methode: $methode")
        }
    }

    private fun rufe(name: String, argumente: JSONObject): JSONObject {
        val werkzeug = alleWerkzeuge().firstOrNull { it.name == name }
            ?: return inhalt("Das Werkzeug $name gibt es nicht.", fehler = true)
        val r = runCatching { runBlocking { werkzeug.ausfuehren(argumente) } }
            .getOrElse { Ergebnis("Werkzeug fehlgeschlagen: ${it.message ?: it.javaClass.simpleName}", fehler = true) }
        Protokoll.melde(Quelle.CHATGPT, werkzeug.titel, r.text, !r.fehler)
        // Inzwischen fertig Gewordenes reicht Jarvis mit dem nächsten Ergebnis nach (ChatGPT lässt sich nicht von hier aus ansprechen).
        val fertig = AblageZentrale.fertigFuerPlugin()
        val nachtrag = if (fertig.isEmpty()) "" else "\n\nINZWISCHEN FERTIG (sag es Frank jetzt von dir aus):\n" + fertig.joinToString("\n") { "- $it" }
        // Die Anleitung liest ein Programm nur beim Verbinden. Damit Franks Regeln und das Wissen über ihn immer und sofort gelten, liegen sie jedem Ergebnis bei.
        // Hat das Programm das Gesamtbild länger nicht geholt (neues Gespräch), erinnert Jarvis daran.
        val eigenes = r.fehler || name == "jarvis_kontext" || name.startsWith("regel") || name.startsWith("frank_info")
        val erinnerung = if (eigenes || System.currentTimeMillis() - Gehirn.zuletztGeholt < 60 * 60_000L) "" else
            "\n\nGESAMTBILD FEHLT: Hast du in diesem Gespräch jarvis_kontext noch nicht aufgerufen, hole es jetzt nach, bevor du Frank antwortest (Dienst, Termine, Aufgaben, Tagesauswertung in einem Aufruf)."
        val regeln = if (eigenes) "" else merkKontext(app).let { if (it.isEmpty()) "" else "\n\n$it" } + erinnerung
        return inhalt(r.text + nachtrag + regeln, r.fehler).apply { r.struktur?.let { put("structuredContent", it) }; r.meta?.let { put("_meta", it) } }
    }

    private fun beschreibe(w: Werkzeug): JSONObject = JSONObject()
        .put("name", w.name)
        .put("title", w.titel)
        .put("description", w.beschreibung)
        .put("inputSchema", w.schema)
        .put("annotations", JSONObject()
            .put("title", w.titel)
            .put("readOnlyHint", w.nurLesen)
            .put("destructiveHint", w.loeschend)
            .put("idempotentHint", w.nurLesen)
            .put("openWorldHint", false))
        .apply { w.meta?.let { put("_meta", it) } }

    private fun anleitung(): String = buildString {
        append("Dies ist Jarvis, Franks persönlicher Assistent auf seinem Handy. Sagt Frank „Jarvis“ oder geht es um seine Aufgaben, Termine ")
        append("oder Erinnerungen, nutze diese Werkzeuge. Antworte danach kurz in einem Satz, was erledigt wurde, ohne ids vorzulesen. ")
        append("Fehlt eine nötige Angabe oder ist sie mehrdeutig, frage kurz nach, statt zu raten.\n")
        append("GESAMTBILD: Rufe in jedem Gespräch zuerst jarvis_kontext auf, bevor du Franks erste Frage beantwortest. Du bekommst in einem Aufruf alles, was Jarvis weiß: ")
        append("Regeln, Wissen über Frank, Dienst und Schlafzeiten, Termine, Aufgaben und die aktuelle Tagesauswertung. Betrachte jede Frage vor diesem Hintergrund ")
        append("und beziehe von dir aus ein, was die Antwort besser macht; was nicht dazugehört, lässt du weg.\n")
        append(merkKontext(app))
        Register.alle(app).filter { it.imPlugin }.forEach { append(it.name).append(": ").append(it.hinweise).append('\n') }
        append("MITDENKEN: Bei jeder Bitte und Frage von Frank prüfst du, ob sein Tag sie berührt – Dienst und Schlafzeiten, Termine, Wetter, Erholung, offene Aufgaben. ")
        append("Die schreibenden Werkzeuge hängen dazu einen Abschnitt MITGEDACHT an; für Fragen zu einem Zeitpunkt nimm wetter_lesen mit datum und uhrzeit, für den Überblick tagesauswertung_lesen. ")
        append("Nenne Frank von dir aus, was sein Vorhaben beeinflusst (zum Beispiel „da schläfst du schon, morgen ist Tagdienst“ oder „um die Zeit soll es stark regnen“), kurz und nur wenn es wirklich zählt.\n")
        append("Tagesdatenbank: Jarvis hält die Daten aller Apps mehrmals täglich fertig vor. Für einen Überblick genügt tagesauswertung_lesen oder tagesdaten_lesen; ")
        append("die einzelnen Apps fragst du nur für Aktuelles (Aufgaben) oder Details ab.\n")
        append("Agenten: Für Recherchen und längere Ausarbeitungen startest du mit agent_starten einen Agenten von Jarvis. Er arbeitet Minuten im Hintergrund und legt das Ergebnis in die Ablage.\n")
        append("Dateien: Melde eine Datei erst als gespeichert, wenn ablage_datei_speichern oder bild_erzeugen „Gespeichert“ zurückgibt. Bei „läuft noch“ ist sie noch nicht da: ")
        append("Rufe dann dasselbe Werkzeug sofort mit denselben Angaben noch einmal auf (das wartet auf die laufende Arbeit, es entsteht nichts doppelt), wiederhole das bis „Gespeichert“ oder ein Fehler kommt, ")
        append("und sag Frank von dir aus, dass es in der Ablage liegt. Er soll nicht nachfragen müssen. ")
        append("Dateien aus diesem Gespräch (hochgeladen oder von dir erzeugt) übergibst du in ablage_datei_speichern als chatgpt_dateien; Pfade wie sandbox: oder /mnt/data erreicht das Handy nicht. ")
        append("Geht keine Übergabe, sag ehrlich, dass die Datei nicht übertragen werden kann.\n")
    }

    private fun inhalt(text: String, fehler: Boolean) = JSONObject()
        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
        .put("isError", fehler)

    private fun ergebnis(id: Any, ergebnis: JSONObject) = JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", ergebnis)
    private fun fehler(id: Any, code: Int, text: String) = JSONObject().put("jsonrpc", "2.0").put("id", id)
        .put("error", JSONObject().put("code", code).put("message", text))

    companion object {
        /** Unterstützte Protokollstände, neuester zuerst. Wünscht der Client einen davon, bekommt er ihn. */
        val VERSIONEN = listOf("2025-06-18", "2025-11-25", "2025-03-26", "2024-11-05")
    }
}
