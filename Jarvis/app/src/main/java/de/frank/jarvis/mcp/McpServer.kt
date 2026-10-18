package de.frank.jarvis.mcp

import android.content.Context
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.agent.Gehirn
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.auswertung.Zeitplan
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.schalter
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.AblageKarte
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.ProgrammierChat
import de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit
import de.frank.jarvis.faehigkeit.Merkdatei
import de.frank.jarvis.faehigkeit.merkKontextOhneProgrammierregeln
import de.frank.jarvis.faehigkeit.ohneNull
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.Werkzeug
import de.frank.jarvis.faehigkeit.schema
import de.frank.jarvis.faehigkeit.text
import de.frank.jarvis.faehigkeit.textListe
import de.frank.jarvis.faehigkeit.vorgangsBlock
import de.frank.jarvis.agent.Vorgaenge
import de.frank.jarvis.gedaechtnis.Bereich
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.observability.Logbuch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Der MCP-Server von Jarvis (Model Context Protocol, Streamable HTTP, zustandslos, nur JSON-Antworten).
 * ChatGPT ruft ihn über den Tunnel auf. Er kennt genau vier Dinge: initialize, ping, tools/list, tools/call.
 * Es gibt bewusst keine Sitzungen — ChatGPT beginnt oft bei jedem Aufruf neu.
 */
class McpServer(context: Context) {
    private val app = context.applicationContext

    /** Werkzeuge von Jarvis selbst, zusätzlich zu denen der angebundenen Apps. */
    private val eigene: List<Werkzeug> = listOf(
        Werkzeug(
            name = "jarvis_kontext",
            titel = "Startkontext und Gesamtbild von Frank holen",
            beschreibung = "Jarvis: Franks persönlicher Kontext aus Jarvis' Live-Gedächtnis, live aus allen angebundenen Apps, jeder Bereich mit Datenstand. " +
                "DAS ERSTE WERKZEUG IN JEDEM GESPRÄCH: Rufe es auf, bevor du Franks erste Frage beantwortest, auch wenn sie einfach wirkt, und noch einmal nach etwa einer Stunde. " +
                "Vorgabe umfang=kurz (schnell, für Sprachgespräche): Integrationsregeln v${Integration.VERSION}, Fähigkeiten, offene und neu fertige Hintergrundaufträge, Franks Regeln, Ziele, Entscheidungen und Wissen über ihn, " +
                "Neues der letzten 24 Stunden, Dienst und Schlafzeiten, Aufgaben, Termine, Wecker, Tagesfazit, Biodaten, Wetter und eine Liste der übrigen Bereiche. " +
                "bereiche=[…] lädt einzelne Bereiche vollständig nach (zum Beispiel tagebuch, gespraeche, ideen, trainings, rueckblick, stacks, ablage, mail, wissen). " +
                "seit=<STAND-MARKE eines früheren Abrufs> liefert nur, was sich seither geändert hat. umfang=voll liefert das ganze Gesamtbild (groß, für Auswertungen und Rückblicke). " +
                "Älteres und frühere Entscheidungen findest du mit gedaechtnis_suchen. Regeln in Kürze: ${Integration.KURZ}",
            schema = schema(
                "umfang" to text("kurz (Vorgabe) oder voll.", listOf("kurz", "voll")),
                "bereiche" to textListe("Nur diese Bereiche vollständig und frisch: " + Bereich.entries.joinToString { it.kennung } + "."),
                "seit" to text("STAND-MARKE aus einem früheren Abruf: dann nur Neuigkeiten und geänderte Bereiche seither."),
            ),
            nurLesen = true,
        ) { a ->
            // Der Zeitpunkt gilt erst, wenn der Kontext wirklich da ist; der Beginn zählt, damit Änderungen währenddessen als neuer gelten.
            val beginn = System.currentTimeMillis()
            val gewaehlt = a.optJSONArray("bereiche")?.let { l -> (0 until l.length()).map { l.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
            val seit = a.optString("seit").trim().toLongOrNull()?.takeIf { it in 1 until beginn }
            val marke = "STAND-MARKE: $beginn (für jarvis_kontext seit=$beginn)"
            when {
                gewaehlt.isNotEmpty() -> Ergebnis(Lagebild.bereiche(app, gewaehlt) + "\n\n" + marke)
                seit != null -> Ergebnis(Lagebild.seit(app, seit) + "\n\n" + vorgangsBlock(app).let { if (it.isEmpty()) "" else "$it\n\n" } + marke)
                    // Die Neuigkeiten sind damit geliefert: nicht noch einmal an das nächste Ergebnis hängen, keine falsche GESAMTBILD-Mahnung.
                    .also { Gehirn.zuletztNachgereicht = beginn }
                else -> {
                    val voll = a.optString("umfang").equals("voll", ignoreCase = true)
                    val jetzt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
                    val kopf = "JARVIS-${if (voll) "GESAMTBILD" else "STARTKONTEXT (kurz)"} · Jarvis ${BuildConfig.VERSION_NAME} · Integration v${Integration.VERSION} · $jetzt · $marke\n\n" +
                        Integration.REGELN + "\n\n" + faehigkeiten() + "\n\n" + vorgangsBlock(app).let { if (it.isEmpty()) "HINTERGRUNDAUFTRÄGE: keine offenen und keine neuen Ergebnisse.\n\n" else "$it\n\n" }
                    val text = kopf + if (voll) Gehirn.kontext(app) else Lagebild.kurz(app)
                    Gehirn.zuletztGeholt = beginn
                    Gehirn.zuletztNachgereicht = beginn
                    Ergebnis(text)
                }
            }
        },
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
            // Aufgaben (heute, morgen) und der nächste Lauf werden gleich nebeneinander gelesen, während die Auswertung geladen wird.
            val lesen = Register.werkzeuge(app).firstOrNull { it.name == "aufgaben_lesen" }
            val aufgabenHeute = nebenher.async { runCatching { lesen?.ausfuehren(JSONObject().put("bereich", "heute"))?.text }.getOrNull() }
            val aufgabenMorgen = nebenher.async { runCatching { lesen?.ausfuehren(JSONObject().put("bereich", "morgen"))?.text }.getOrNull() }
            val naechsterLauf = nebenher.async { runCatching { Zeitplan.naechster(app) }.getOrNull() }
            // Die neueste liegt im Speicher (lade/erstelle/datenbericht halten sie aktuell): kein Lesen von der Platte.
            var neueste = Tagesauswertung.stand.value.neueste ?: Tagesauswertung.neueste(app)
            var hinweis = ""
            if (neueste != null && Tagesauswertung.veraltet(app) && !Tagesauswertung.stand.value.laeuft) {
                // Ein geplanter Lauf fehlt: sofort frische Daten liefern und die volle Auswertung nachziehen.
                neueste = Tagesauswertung.datenbericht(app, "beim Abruf erstellt, weil der geplante Lauf fehlte")
                JarvisDienst.auswerten(app, "nachgeholt beim Abruf")
                hinweis = "HINWEIS: Die letzte gedeutete Auswertung war veraltet. Dies sind frische Daten ohne Deutung; die vollständige Auswertung ist in ein bis drei Minuten abrufbar.\n"
            }
            val aufgaben = buildString {
                append("--- AUFGABEN (frisch gelesen um ").append(java.time.LocalTime.now().toString().take(5)).append(" Uhr) ---\n")
                // Eine hängende Aufgaben-App hält die Antwort höchstens kurz auf.
                append("Heute und überfällig: ").append(withTimeoutOrNull(QUELLE_MS) { aufgabenHeute.await() } ?: "nicht lesbar").append("\n")
                append("Morgen: ").append(withTimeoutOrNull(QUELLE_MS) { aufgabenMorgen.await() } ?: "nicht lesbar").append("\n")
            }
            val naechster = withTimeoutOrNull(QUELLE_MS) { naechsterLauf.await() }?.let { "Nächste automatische Auswertung: ${it.toLocalTime().toString().take(5)} Uhr." } ?: "Die automatische Synchronisation ist ausgeschaltet."
            if (neueste == null) {
                JarvisDienst.auswerten(app, "erster Abruf")
                Ergebnis("Es gibt noch keine Tagesauswertung. Jarvis erstellt gerade die erste, das dauert ein bis drei Minuten; bitte danach noch einmal abrufen. $naechster\n\n$aufgaben")
            } else {
                val daten = a.optBoolean("mit_daten", false)
                // Die Aufgaben im Datenanhang sind der alte Stand: weglassen, die frischen stehen darunter.
                val fassung = neueste.copy(daten = Tagesauswertung.abschnitte(neueste.daten, ohne = setOf("OFFENE AUFGABEN")))
                // Was sich seit dem Schreiben der Auswertung in den Apps geändert hat, steht gleich dabei.
                val seither = Lagebild.neuigkeitenSeit(neueste.zeit).let { if (it.isEmpty()) "" else "--- SEIT DIESER AUSWERTUNG NEU (live aus Franks Apps) ---\n$it\n\n" }
                Ergebnis(hinweis + Tagesauswertung.alsText(fassung, daten) + "\n" + seither + aufgaben + naechster)
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
            val neueste = Tagesauswertung.stand.value.neueste ?: Tagesauswertung.neueste(app)
            if (neueste == null) { JarvisDienst.auswerten(app, "erster Abruf"); Ergebnis("Die Tagesdatenbank wird gerade zum ersten Mal gefüllt. In ein bis drei Minuten erneut abrufen.") }
            else {
                val alt = Tagesauswertung.veraltet(app)
                if (alt && !Tagesauswertung.stand.value.laeuft) JarvisDienst.auswerten(app, "nachgeholt bei tagesdaten_lesen")
                // Von einem anderen Tag: „heute“ in den Überschriften meint jenen Tag, nicht heute.
                val datum = neueste.zeitpunkt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM, HH:mm 'Uhr'", Locale.GERMAN))
                val achtung = if (neueste.zeitpunkt.toLocalDate() == LocalDate.now()) "" else "ACHTUNG: Diese Daten sind von $datum; „heute“ und „morgen“ in den Überschriften beziehen sich auf jenen Tag.\n"
                val hinweis = if (alt) "HINWEIS: Diese Daten sind älter als der letzte geplante Lauf; neue werden gerade gesammelt (in ein bis drei Minuten erneut abrufen). Aktuelles steht im Gesamtbild (jarvis_kontext).\n" else ""
                Ergebnis((achtung + hinweis).let { if (it.isEmpty()) it else it + "\n" } + Tagesauswertung.tagesdaten(neueste, a.optString("bereich", "alle")))
            }
        },
        Werkzeug(
            name = "jarvis_status",
            titel = "Jarvis-Status",
            beschreibung = "Jarvis: sagt, ob Jarvis auf Franks Handy erreichbar ist, welche Apps angebunden sind, welches Datum und welche Uhrzeit dort gerade gelten " +
                "und ob Franks Windows-PC und sein Mac an und für Programmieraufträge erreichbar sind (genauer: pc_status). " +
                "Nutze es bei „Jarvis, bist du da?“ oder wenn du das heutige Datum auf dem Handy brauchst. Mit funktionen=true kommt zusätzlich die vollständige, tatsächliche " +
                "Liste aller Werkzeuge von Jarvis (Grundlage zum Beispiel für eine Übersicht oder Infografik über Jarvis' Funktionen). Für Fragen, was Jarvis kann oder was fehlt, " +
                "ist funktionen_lesen die erste Quelle: die gepflegte Funktionsübersicht mit Grenzen und Versionsstand.",
            schema = schema("funktionen" to schalter("true = alle Werkzeuge mit Kurzbeschreibung auflisten.")),
            nurLesen = true,
        ) { it ->
            val jetzt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
            // Alle Apps gleichzeitig fragen, jede mit eigenem Zeitlimit: Eine kalte oder hängende App hält die anderen nicht auf.
            val faehigkeiten = Register.alle(app)
            val zustaende = faehigkeiten.map { f ->
                f.name to nebenher.async { runCatching { f.stoerung()?.let { "gestört ($it)" } ?: "bereit" }.getOrElse { "gestört (${it.message ?: it.javaClass.simpleName})" } }
            }
            val dienstHeute = nebenher.async { runCatching { faehigkeiten.filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.heuteKurz() }.getOrNull().orEmpty() }
            val apps = zustaende.map { (name, zustand) ->
                name + ": " + (withTimeoutOrNull(QUELLE_MS) { zustand.await() }
                    ?: "keine Antwort (Zeitlimit)".also { Logbuch.system("jarvis_status: $name antwortet nicht", "Zeitlimit ${QUELLE_MS / 1000} s") })
            }.joinToString("; ")
            val dienst = withTimeoutOrNull(QUELLE_MS) { dienstHeute.await() }.orEmpty()
            val programmieren = faehigkeiten.filterIsInstance<ProgrammierFaehigkeit>().firstOrNull()
                ?.let { p -> withTimeoutOrNull(QUELLE_MS) { runCatching { p.zustand() }.getOrNull() } }.orEmpty()
            val funktionen = if (!it.optBoolean("funktionen")) "" else buildString {
                append("\n\nFUNKTIONEN VON JARVIS (Version ").append(BuildConfig.VERSION_NAME).append("):\n")
                Register.alle(app).forEach { f ->
                    append(f.name).append(if (f.imPlugin) "" else " (nur in Jarvis selbst und für seine Agenten)").append(": ").append(f.beschreibung).append('\n')
                    f.werkzeuge.forEach { w -> append("  - ").append(w.titel).append('\n') }
                }
                append("Jarvis selbst: ").append(eigene.joinToString(", ") { w -> w.titel }).append('\n')
            }
            Ergebnis("Jarvis ist bereit (Version ${BuildConfig.VERSION_NAME}, Integration v${Integration.VERSION}). Auf dem Handy ist es $jetzt. " +(if (dienst.isEmpty()) "" else "Dienst heute: $dienst. ") + "Angebundene Apps: $apps." +
                (if (programmieren.isEmpty()) "" else " $programmieren") + funktionen)
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
                .put("serverInfo", JSONObject().put("name", "Jarvis").put("title", "Jarvis").put("version", "${BuildConfig.VERSION_NAME} (Integration v${Integration.VERSION})"))
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
        val beginn = System.currentTimeMillis()
        // JSON-null zählt als nicht angegeben (sonst läse das Werkzeug den Text „null“).
        val sauber = argumente.ohneNull()
        val ausfuehren: suspend () -> Ergebnis = {
            runCatching { werkzeug.ausfuehren(sauber) }
                .getOrElse { if (it is kotlinx.coroutines.CancellationException) throw it; Logbuch.fehler("Plugin-Werkzeug $name", it); Ergebnis("Werkzeug fehlgeschlagen: ${it.message ?: it.javaClass.simpleName}", fehler = true) }
        }
        // Höchstens [JE_WERKZEUG] gleichzeitige Läufe je Werkzeug: Hängt eine Partner-App, bindet nicht jede Wiederholung einen weiteren Faden.
        val platz = plaetze.getOrPut(name) { Semaphore(JE_WERKZEUG) }
        if (!platz.tryAcquire()) {
            Logbuch.system("Plugin: $name ausgelastet", "$JE_WERKZEUG Aufrufe laufen noch")
            return inhalt("Das Werkzeug $name arbeitet noch an $JE_WERKZEUG früheren Aufrufen (eine Partner-App antwortet nicht). " +
                "Bitte gleich noch einmal versuchen; schreibende Aufrufe zuerst mit dem passenden Lese-Werkzeug prüfen.", fehler = true)
        }
        // Schreibende Aufrufe, die doppelt Schaden anrichten, laufen durch den Wiederholschutz (Wiederholung nach Verbindungsabbruch).
        // AusChatGpt: Werkzeuge erkennen daran den Weg (ein Programmierauftrag bekommt dann seine eigene Session).
        // Abgekoppelt mit Zeitlimit: Der Faden des Tunnels kommt spätestens nach [WERKZEUG_MS] frei, auch wenn das Werkzeug hängt;
        // es läuft dann im Hintergrund weiter (eine Wiederholung wartet im Wiederholschutz auf genau diesen Lauf).
        val lauf = nebenher.async(ProgrammierChat.AusChatGpt()) {
            try { if (name in GESCHUETZT) schutz.fuehreAus(Wiederholschutz.schluessel(name, sauber), ausfuehren) else ausfuehren() to false }
            finally { platz.release() }
        }
        val (r, wiederholt) = runCatching {
            runBlocking {
                withTimeoutOrNull(WERKZEUG_MS) { lauf.await() } ?: run {
                    Logbuch.system("Plugin: $name Zeitlimit", "läuft nach ${WERKZEUG_MS / 1000} s im Hintergrund weiter")
                    Ergebnis("Das Werkzeug $name ist nach ${WERKZEUG_MS / 1000} Sekunden noch nicht fertig und arbeitet im Hintergrund weiter. " +
                        "Prüfe das Ergebnis gleich mit dem passenden Lese-Werkzeug; schreibende Aufrufe nicht blind wiederholen.", fehler = true) to false
                }
            }
        }
            .getOrElse { Logbuch.fehler("Plugin-Werkzeug $name", it); Ergebnis("Werkzeug fehlgeschlagen: ${it.message ?: it.javaClass.simpleName}", fehler = true) to false }
        Logbuch.dauer("Plugin: $name", System.currentTimeMillis() - beginn, (if (wiederholt) "Wiederholung, nicht erneut ausgeführt; " else "") + if (r.fehler) "Fehler: " + r.text.take(200) else "${r.text.length} Zeichen")
        if (wiederholt) return inhalt("WIEDERHOLUNG ERKANNT: Derselbe Aufruf wurde gerade schon ausgeführt, es ist nichts doppelt passiert. Ergebnis des ersten Aufrufs:\n" + r.text, r.fehler)
            .apply { r.struktur?.let { put("structuredContent", it) }; r.meta?.let { put("_meta", it) } }
        // Synchron und vor der Auffrischung: Die Gespräche im Live-Gedächtnis lesen das Protokoll und sollen diese Aktion schon sehen.
        // Teuer ist das nicht, melde() ändert nur den Speicher und schreibt die Datei gebündelt auf dem eigenen Faden.
        runCatching { Protokoll.melde(Quelle.CHATGPT, werkzeug.titel, r.text, !r.fehler) }
        // Eine Schreibaktion landet sofort im Live-Gedächtnis (der betroffene Bereich wird neu gelesen).
        if (!werkzeug.nurLesen && !r.fehler) Lagebild.nachAktion(app, name, "ChatGPT")
        // Fertige Hintergrundaufträge, die Frank noch nicht erfahren hat: als kurze Zeile an passende Ergebnisse (nicht konsumierend;
        // erst vorgaenge mit aktion mitgeteilt nimmt sie heraus). Je Vorgang höchstens alle fünf Minuten, damit es nicht an jedem Ergebnis klebt.
        // Nicht an Werkzeuge, die Programmier-Sitzungen (Claude Code) benutzen: Sonst „verbraucht“ eine fremde Sitzung die Meldung fürs Gespräch.
        val vorgangFertig = if (name in OHNE_VORGANGSMELDUNG || name.startsWith("programmier")) "" else fertigeVorgaenge()
        // Inzwischen fertig Gewordenes reicht Jarvis nach (ChatGPT lässt sich nicht von hier aus ansprechen). Jede Meldung gibt es nur
        // einmal; deshalb nur an Ergebnisse, die zur Ablage gehören oder ein Gespräch beginnen. Sonst verbrauchte sie ein fremdes
        // Gespräch (z. B. eine Programmiersitzung, die nur die Tagesauswertung liest), und das anstoßende erführe nichts.
        val fertig = if (name == "jarvis_kontext" || name == "bild_erzeugen" || name.startsWith("ablage_")) AblageZentrale.fertigFuerPlugin() else emptyList()
        val nachtrag = if (fertig.isEmpty()) "" else "\n\nINZWISCHEN FERTIG (sag es Frank jetzt von dir aus):\n" + fertig.joinToString("\n") { "- $it" }
        // Die Anleitung liest ein Programm nur beim Verbinden. Damit Franks Regeln, Ziele und das Wissen über ihn trotzdem immer und sofort gelten, reicht Jarvis sie mit den Ergebnissen nach.
        // Jarvis kann Gespräche nicht unterscheiden (der Relay reicht nur den Rumpf weiter). Deshalb:
        // - eine kurze Erinnerung an jarvis_kontext an jedem Ergebnis (ein neues Gespräch innerhalb der Stunde bekäme sonst nie eine),
        // - Gedächtnis und Neuigkeiten genau einmal nach einer Änderung, sonst höchstens einmal je Stunde (nicht an jedem weiteren Ergebnis).
        val jetzt = System.currentTimeMillis()
        // Fortschrittsmeldungen kommen von Programmier-Sitzungen, nicht aus einem Gespräch mit Frank: ihnen nichts anhängen.
        val eigenes = r.fehler || name == "jarvis_kontext" || name in GEDAECHTNIS || name == "programmier_fortschritt"
        val bekannt = maxOf(Gehirn.zuletztGeholt, Gehirn.zuletztNachgereicht)
        val stundeUm = jetzt - Gehirn.zuletztGeholt >= 60 * 60_000L && jetzt - Gehirn.zuletztNachgereicht >= 60 * 60_000L
        val gedaechtnis = if (eigenes || !(stundeUm || Merkdatei.zuletztGeaendert > bekannt)) "" else merkKontextOhneProgrammierregeln(app, ohneSymbiose = true).let { if (it.isEmpty()) "" else "\n\n$it" }
        // Was sich seit dem letzten Überblick in Franks Apps geändert hat (eigene Aktionen über ChatGPT kennt das Gespräch schon).
        val neues = if (eigenes || Gehirn.zuletztGeholt == 0L) "" else Lagebild.neuigkeitenSeit(bekannt, ohneAnlass = "ChatGPT", hoechstens = 12).let {
            if (it.isEmpty()) "" else "\n\nNEU IN FRANKS APPS SEIT DEINEM LETZTEN ÜBERBLICK (beziehe es ein, wo es zur Frage passt):\n$it"
        }
        if (gedaechtnis.isNotEmpty() || neues.isNotEmpty()) Gehirn.zuletztNachgereicht = jetzt
        val erinnerung = when {
            eigenes -> ""
            stundeUm -> "\n\nGESAMTBILD FEHLT: Hast du in diesem Gespräch jarvis_kontext noch nicht aufgerufen, hole es jetzt nach, bevor du Frank antwortest (Live-Gedächtnis aus allen Apps in einem Aufruf)."
            else -> "\n\n(Falls in diesem Gespräch noch nicht geschehen: zuerst jarvis_kontext aufrufen.)"
        }
        return inhalt(r.text + nachtrag + vorgangFertig + gedaechtnis + neues + erinnerung, r.fehler).apply { r.struktur?.let { put("structuredContent", it) }; r.meta?.let { put("_meta", it) } }
    }

    /** Die Zeile „VORGANG FERTIG“ für fertige, noch nicht mitgeteilte Hintergrundaufträge; leer, wenn keiner fällig ist. */
    private fun fertigeVorgaenge(): String {
        val jetzt = System.currentTimeMillis()
        val faellig = runCatching { Vorgaenge.buch(app).liste().filter { !it.offen && it.neu } }.getOrDefault(emptyList())
            .filter { v -> synchronized(gezeigt) { (gezeigt[v.id] ?: 0L).let { jetzt - it >= 5 * 60_000L }.also { if (it) gezeigt[v.id] = jetzt } } }
        if (faellig.isEmpty()) return ""
        return "\n\nVORGANG FERTIG (Hintergrundauftrag, gehört vielleicht zu einem früheren Gespräch; sprichst du gerade mit Frank, sag es ihm an passender Stelle, " +
            "Details mit vorgaenge aktion lesen; aktion mitgeteilt erst, nachdem du es ihm wirklich gesagt hast):\n" +
            faellig.joinToString("\n") { "- " + Vorgaenge.zeile(it) }
    }

    /** Was Jarvis kann, je angebundene App eine Zeile mit den Werkzeugnamen (für den Startkontext). */
    private fun faehigkeiten(): String = buildString {
        append("FÄHIGKEITEN (Werkzeuge je Bereich; Störungen stehen bei jarvis_status):\n")
        Register.alle(app).filter { it.imPlugin && it.werkzeuge.isNotEmpty() }.forEach { f -> append("- ").append(f.name).append(": ").append(f.werkzeuge.joinToString(", ") { it.name }).append('\n') }
        append("- Jarvis selbst: ").append(eigene.joinToString(", ") { it.name })
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
        // Die versionierten Grundregeln stehen vorn: Manche Programme kürzen die Anleitung, der Anfang kommt immer an.
        append(Integration.REGELN).append('\n')
        append("GESAMTBILD: jarvis_kontext liefert in einem Aufruf Jarvis' Live-Gedächtnis aus allen angebundenen Apps, jeden Bereich mit Datenstand ")
        append("(kurz als Vorgabe; einzelne Bereiche mit bereiche nachladen, nur Geändertes mit seit, alles mit umfang voll). Betrachte jede Frage vor diesem Hintergrund und beziehe von dir aus ein, was die Antwort besser macht; was nicht dazugehört, lässt du weg. ")
        append("Für Älteres oder ein bestimmtes Thema suchst du gezielt mit gedaechtnis_suchen. Hast du selbst etwas geändert oder geht es um einen Messwert von gerade eben, lies die App frisch.\n")
        // Ohne Programmierregeln: Die Anleitung lesen auch Programmier-Sitzungen (Claude Code über denselben Relay); die Regeln sind reiner Jarvis-Kontext.
        append(merkKontextOhneProgrammierregeln(app))
        append("PROGRAMMIERREGELN: Franks Programmierregeln stehen bewusst nicht hier. Sie sind nur für dich (Jarvis bzw. ChatGPT als Jarvis' Gesprächspartner): Lies sie mit jarvis_gemerktes_lesen, bevor du einen Programmierauftrag formulierst, und richte den Auftrag danach aus, ohne ihren Text an den Programmierer zu schicken. ")
        append("Bist du selbst eine Programmier-Sitzung, gehen sie dich nichts an.\n")
        Register.alle(app).filter { it.imPlugin }.forEach { append(it.name).append(": ").append(it.hinweise).append('\n') }
        append("MITDENKEN: Bei jeder Bitte und Frage von Frank prüfst du, ob sein Tag sie berührt – Dienst und Schlafzeiten, Termine, Wetter, Erholung, offene Aufgaben. ")
        append("Das Gesamtbild sagt dir das meiste davon; die schreibenden Werkzeuge hängen zusätzlich einen Abschnitt MITGEDACHT an, und für Fragen zu einem Zeitpunkt nimmst du wetter_lesen mit datum und uhrzeit. ")
        append("Nenne Frank von dir aus, was sein Vorhaben beeinflusst (zum Beispiel „da schläfst du schon, morgen ist Tagdienst“ oder „um die Zeit soll es stark regnen“), kurz und nur wenn es wirklich zählt.\n")
        append("Welches Werkzeug wofür: Überblick und Hintergrund = jarvis_kontext. tagesauswertung_lesen nur, wenn Frank die Auswertung selbst hören oder neu erstellen lassen will. ")
        append("tagesdaten_lesen für einzelne Rohwerte der letzten Synchronisation. Die einzelnen Apps für Aktuelles, Details und Änderungen.\n")
        append("Ablage nur auf ausdrücklichen Wunsch: Antworten und Recherchen gibst du im Gespräch; ablage_schreiben, agent_starten mit ablegen und bild_erzeugen mit ablegen nur, wenn Frank das Ablegen verlangt. ")
        append("Ein späterer Wunsch („leg das doch ab“) geht mit in_ablage_uebernehmen. ")
        append("Dateien: Melde eine Datei erst als gespeichert, wenn ablage_datei_speichern oder bild_erzeugen „Gespeichert“ zurückgibt; „Erzeugt … VORLÄUFIG“ heißt fertig, aber NICHT in der Ablage (zeigen mit ablage_zeigen). Bei „läuft noch“ ist sie noch nicht da: ")
        append("Rufe dann dasselbe Werkzeug sofort mit denselben Angaben noch einmal auf (das wartet auf die laufende Arbeit, es entsteht nichts doppelt), wiederhole das bis „Gespeichert“, „Erzeugt“ oder ein Fehler kommt, ")
        append("und sag Frank von dir aus, dass es fertig ist und ob es in der Ablage liegt. Er soll nicht nachfragen müssen. ")
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
        /** Höchste Wartezeit auf eine einzelne Quelle (Partner-App, Kalender), bevor sie als „keine Antwort“ gilt. */
        private const val QUELLE_MS = 5_000L
        /**
         * Hier laufen Quellen nebeneinander. Bewusst abgekoppelt: Ein hängender Aufruf an eine Partner-App lässt sich nicht
         * abbrechen; so kommt die Antwort trotzdem nach dem Zeitlimit frei, und der Aufruf läuft im Hintergrund aus.
         */
        private val nebenher = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        /** Die Werkzeuge des Gedächtnisses geben ihre Einträge selbst zurück; ihnen liegt der Kontext nicht noch einmal bei. */
        private val GEDAECHTNIS = setOf("jarvis_merken", "jarvis_vergessen", "jarvis_gemerktes_lesen")
        /**
         * Schreibende Werkzeuge, bei denen eine Wiederholung nach Verbindungsabbruch Schaden anrichtet (doppelt angelegt,
         * doppelt verschickt, doppelt gestartet). Nicht hierher: Werkzeuge, die zum Nachfragen bewusst wiederholt werden
         * (bild_erzeugen, ablage_datei_speichern), und Schalter wie Timer und Stoppuhr.
         */
        private val GESCHUETZT = setOf(
            "aufgabe_anlegen", "aufgabe_aendern", "aufgabe_loeschen", "kalender_eintragen", "kalender_loeschen", "idee_speichern", "idee_loeschen",
            "mail_senden", "agent_starten", "programmieren", "programmier_nachtrag", "ablage_schreiben", "in_ablage_uebernehmen", "jarvis_merken", "jarvis_vergessen", "wecker_stellen", "wecker_loeschen",
            "stack_speichern", "mittel_speichern", "stack_mittel_setzen",
        )
        private val schutz = Wiederholschutz()
        /** Längste Wartezeit des Tunnel-Fadens auf ein Werkzeug; großzügig über der längsten legitimen Dauer (der Relay gibt nach 75 s auf). */
        private const val WERKZEUG_MS = 120_000L
        /** So viele Läufe desselben Werkzeugs dürfen gleichzeitig offen sein. */
        private const val JE_WERKZEUG = 4
        private val plaetze = java.util.concurrent.ConcurrentHashMap<String, Semaphore>()
        /** Werkzeuge ohne „VORGANG FERTIG“: eigene Übersichten und die Werkzeuge der Programmier-Sitzungen. */
        private val OHNE_VORGANGSMELDUNG = setOf("jarvis_kontext", "vorgaenge", "vorgang_abbrechen", "pc_status", "repo_lesen", "tagesauswertung_lesen", "funktionen_lesen")
        /** Wann eine Vorgangsmeldung zuletzt an einem Ergebnis hing (Kennung → Zeit). */
        private val gezeigt = HashMap<String, Long>()
        /** Unterstützte Protokollstände, neuester zuerst. Wünscht der Client einen davon, bekommt er ihn. */
        val VERSIONEN = listOf("2025-06-18", "2025-11-25", "2025-03-26", "2024-11-05")
    }
}
