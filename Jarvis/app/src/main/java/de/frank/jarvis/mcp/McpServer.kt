package de.frank.jarvis.mcp

import android.content.Context
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.faehigkeit.zahl
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.auswertung.Zeitplan
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.schalter
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
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
    private val agent by lazy { JarvisAgent(app) }

    /** Werkzeuge von Jarvis selbst, zusätzlich zu denen der angebundenen Apps. */
    private val eigene: List<Werkzeug> = listOf(
        Werkzeug(
            name = "tagesauswertung_lesen",
            titel = "Tagesauswertung lesen",
            beschreibung = "Jarvis: liefert Franks fertige Tagesauswertung. Jarvis erstellt sie mehrmals täglich selbst im Hintergrund: was für ein Tag heute ist " +
                "(Arbeitstag oder frei, Dienst, Schlaf- und freie Zeitfenster, Termine), die Biodaten des Tages im Vergleich zu 7 Tagen, dem letzten Monat und allen bisherigen Tagen, " +
                "Trainings, eine Einschätzung, Empfehlungen und einen Ausblick auf die nächsten Tage. Die offenen Aufgaben werden bei jedem Abruf frisch angehängt. " +
                "DAS ERSTE WERKZEUG für „Wie ist meine Tagesauswertung?“, „Wie sieht mein Tag aus?“, „Guten Morgen Jarvis“, „Was steht an und wie geht es mir?“. " +
                "Ein Aufruf genügt; rufe danach Kalender, Biomarker oder Aufgaben nur noch für Nachfragen auf, die die Auswertung nicht beantwortet.",
            schema = schema("mit_daten" to schalter("true = zusätzlich der vollständige Datenanhang mit allen Einzelwerten (Vorgabe: false, die Auswertung genügt meist).")),
            nurLesen = true,
        ) { a ->
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
            val naechster = Zeitplan.naechster(app)?.let { "Nächste automatische Auswertung: ${it.toLocalTime().toString().take(5)} Uhr." } ?: "Die automatische Auswertung ist ausgeschaltet."
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
            name = "agenten_liste",
            titel = "Agenten ansehen",
            beschreibung = "Jarvis: nennt die Agenten, die Jarvis einsetzen kann (zum Beispiel Recherche, Machbarkeit und selbst angelegte), und welche gerade arbeiten.",
            schema = schema(),
            nurLesen = true,
        ) {
            val laufend = Agenten.laeufe.value
            Ergebnis(buildString {
                append("Agenten von Jarvis:\n")
                Agenten.alle(app).forEach { append("- ").append(it.name).append(if (it.vorgegeben) " (eingebaut)" else " (selbst angelegt)").append(": ").append(it.rolle.take(220)).append('\n') }
                if (laufend.isEmpty()) append("Gerade arbeitet kein Agent.")
                else laufend.forEach { append("Arbeitet gerade: ").append(it.agent).append(" seit ").append((System.currentTimeMillis() - it.seit) / 60_000).append(" Minuten an „").append(it.auftrag.take(100)).append("“").append(if (it.schritt.isEmpty()) "" else ", aktueller Schritt: ${it.schritt}").append('\n') }
            }.trim())
        },
        Werkzeug(
            name = "agent_starten",
            titel = "Agenten beauftragen",
            beschreibung = "Jarvis: beauftragt einen Agenten von Jarvis mit einer längeren Arbeit, zum Beispiel einer Internet-Recherche („Jarvis, recherchiere, wie man … am besten baut“) " +
                "oder einer Machbarkeitsprüfung einer Idee. Der Agent arbeitet mehrere Minuten selbstständig im Hintergrund auf Franks Handy (eigenes Modell, Internet-Suche, Zugriff auf Ideen, " +
                "Aufgaben, Kalender, Biomarker), legt den Bericht in die Ablage und meldet sich per Benachrichtigung. Dieser Aufruf kehrt sofort zurück. " +
                "Das Ergebnis holst du später mit ablage_liste und ablage_lesen. Mit per_mail wird der Bericht zusätzlich an Frank gemailt.",
            schema = schema(
                "agent" to text("Name des Agenten, zum Beispiel Recherche oder Machbarkeit (siehe agenten_liste)."),
                "auftrag" to text("Der vollständige Auftrag mit allem Wissen aus dem Gespräch: Ziel, Rahmen, was Frank wichtig ist, gewünschte Form des Ergebnisses."),
                "per_mail" to schalter("true = den fertigen Bericht zusätzlich per E-Mail an Frank schicken."),
                "vorlesen" to schalter("true = Jarvis liest den fertigen Bericht auf dem Handy laut vor, sobald er da ist."),
                pflicht = listOf("agent", "auftrag"),
            ),
            nurLesen = false,
        ) { a ->
            val plan = Agenten.finde(app, a.optString("agent"))
            when {
                plan == null -> Ergebnis("Diesen Agenten gibt es nicht. Vorhanden: " + Agenten.alle(app).joinToString { it.name } + ". Mit agent_anlegen lässt sich ein neuer bauen.", fehler = true)
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
            name = "agent_anlegen",
            titel = "Agenten bauen",
            beschreibung = "Jarvis: baut einen neuen Agenten oder ändert einen vorhandenen. Ein Agent ist ein Name plus eine Rolle: die Anweisung, wie er an Aufträge herangeht, " +
                "worauf er achtet und wie sein Ergebnis aussieht. Nutze es, wenn Frank sich einen Spezialisten wünscht (zum Beispiel „Trainingsplaner“, „Einkaufsvergleich“, „App-Architekt“). " +
                "Formuliere die Rolle selbst ausführlich aus Franks Wunsch.",
            schema = schema(
                "name" to text("Kurzer Name, ein bis zwei Wörter."),
                "rolle" to text("Die Rolle in fünf bis zehn Sätzen: Aufgabe, Vorgehen in Schritten, worauf zu achten ist, Form des Ergebnisses."),
                pflicht = listOf("name", "rolle"),
            ),
            nurLesen = false,
        ) { a ->
            if (a.optString("name").isBlank() || a.optString("rolle").length < 40) Ergebnis("Name und eine ausführliche Rolle sind nötig.", fehler = true)
            else Agenten.speichere(app, a.optString("name"), a.optString("rolle")).let { Ergebnis("Agent „${it.name}“ ist angelegt und kann mit agent_starten beauftragt werden.") }
        },
        Werkzeug(
            name = "agent_loeschen",
            titel = "Agenten löschen",
            beschreibung = "Jarvis: löscht einen selbst angelegten Agenten. Die eingebauten Agenten bleiben.",
            schema = schema("name" to text("Name des Agenten."), pflicht = listOf("name")),
            nurLesen = false,
            loeschend = true,
        ) { a -> if (Agenten.loesche(app, a.optString("name"))) Ergebnis("Agent „${a.optString("name")}“ gelöscht.") else Ergebnis("Kein selbst angelegter Agent mit diesem Namen.", fehler = true) },
        Werkzeug(
            name = "tagesauswertung_erstellen",
            titel = "Tagesauswertung neu erstellen",
            beschreibung = "Jarvis: stößt jetzt eine neue Tagesauswertung an (frische Biodaten holen, alles neu auswerten). Das dauert ein bis drei Minuten und läuft im Hintergrund; " +
                "das Ergebnis holst du danach mit tagesauswertung_lesen. Nur aufrufen, wenn Frank ausdrücklich eine neue oder aktualisierte Auswertung verlangt.",
            schema = schema(),
            nurLesen = false,
        ) {
            if (Tagesauswertung.stand.value.laeuft) Ergebnis("Eine Tagesauswertung läuft bereits. In ein bis zwei Minuten mit tagesauswertung_lesen abrufen.")
            else {
                JarvisDienst.auswerten(app, "auf Wunsch über ChatGPT")
                Ergebnis("Die neue Tagesauswertung wird jetzt erstellt. In ein bis drei Minuten mit tagesauswertung_lesen abrufen.")
            }
        },
        Werkzeug(
            name = "jarvis_status",
            titel = "Jarvis-Status",
            beschreibung = "Jarvis: sagt, ob Jarvis auf Franks Handy erreichbar ist, welche Apps angebunden sind und welches Datum und welche Uhrzeit dort gerade gelten. " +
                "Nutze es bei „Jarvis, bist du da?“ oder wenn du das heutige Datum auf dem Handy brauchst.",
            schema = schema(),
            nurLesen = true,
        ) {
            val jetzt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
            val apps = Register.alle(app).joinToString("; ") { f -> f.name + ": " + (f.stoerung()?.let { "gestört ($it)" } ?: "bereit") }
            val dienst = Register.alle(app).filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.heuteKurz().orEmpty()
            Ergebnis("Jarvis ist bereit. Auf dem Handy ist es $jetzt. " + (if (dienst.isEmpty()) "" else "Dienst heute: $dienst. ") + "Angebundene Apps: $apps.")
        },
        Werkzeug(
            name = "jarvis_auftrag",
            titel = "Auftrag an Jarvis",
            beschreibung = "Jarvis: übergibt einen frei formulierten Auftrag an Jarvis auf Franks Handy, der ihn selbstständig mit allen angebundenen Apps erledigt " +
                "und in einem Satz antwortet. Nutze dieses Werkzeug NUR, wenn keines der anderen Werkzeuge direkt passt — etwa für Aufträge über mehrere Schritte " +
                "(„verschiebe alles von heute Nachmittag auf morgen“, „räum meine überfälligen Aufgaben auf“). Für eine einzelne Aufgabe nimm das direkte Werkzeug, das ist schneller.",
            schema = schema("auftrag" to text("Der vollständige Auftrag in Franks Worten, mit allen Angaben aus dem Gespräch."), pflicht = listOf("auftrag")),
            nurLesen = false,
        ) { a ->
            val auftrag = a.optString("auftrag").trim()
            if (auftrag.isEmpty()) Ergebnis("Der Auftrag ist leer.", fehler = true)
            // Knappes Zeitfenster: ChatGPT wartet nicht beliebig lange auf ein Werkzeug.
            else Ergebnis(agent.frage(auftrag, zeitlimitMs = 45_000, maxSchritte = 6))
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
                .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
                .put("serverInfo", JSONObject().put("name", "Jarvis").put("title", "Jarvis").put("version", BuildConfig.VERSION_NAME))
                .put("instructions", anleitung()))
            "ping" -> ergebnis(id, JSONObject())
            "tools/list" -> ergebnis(id, JSONObject().put("tools", JSONArray(alleWerkzeuge().map(::beschreibe))))
            "tools/call" -> ergebnis(id, rufe(parameter.optString("name"), parameter.optJSONObject("arguments") ?: JSONObject()))
            "resources/list" -> ergebnis(id, JSONObject().put("resources", JSONArray()))
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
        return inhalt(r.text, r.fehler)
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

    private fun anleitung(): String = buildString {
        append("Dies ist Jarvis, Franks persönlicher Assistent auf seinem Handy. Sagt Frank „Jarvis“ oder geht es um seine Aufgaben, Termine ")
        append("oder Erinnerungen, nutze diese Werkzeuge. Antworte danach kurz in einem Satz, was erledigt wurde, ohne ids vorzulesen. ")
        append("Fehlt eine nötige Angabe oder ist sie mehrdeutig, frage kurz nach, statt zu raten.\n")
        Register.alle(app).filter { it.imPlugin }.forEach { append(it.name).append(": ").append(it.hinweise).append('\n') }
        append("Tagesdatenbank: Jarvis hält die Daten aller Apps mehrmals täglich fertig vor. Für einen Überblick genügt tagesauswertung_lesen oder tagesdaten_lesen; ")
        append("die einzelnen Apps fragst du nur für Aktuelles (Aufgaben) oder Details ab.\n")
        append("Agenten: Für Recherchen und längere Ausarbeitungen startest du mit agent_starten einen Agenten von Jarvis. Er arbeitet Minuten im Hintergrund und legt das Ergebnis in die Ablage.\n")
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
