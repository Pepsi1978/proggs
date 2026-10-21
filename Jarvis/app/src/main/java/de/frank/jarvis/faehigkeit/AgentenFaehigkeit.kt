package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.agent.Annahme
import de.frank.jarvis.agent.Vorgaenge
import de.frank.jarvis.agent.Vorgang
import de.frank.jarvis.agent.VorgangsBuch
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.dienst.JarvisDienst
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Die Agenten von Jarvis und sein Aktivitäts-Protokoll. Steht im Plugin und im eigenen Chat (auch „Hey Jarvis“), damit
 * eine große Recherche überall an einen Agenten geht statt im kurzen Gespräch zu scheitern. Agenten selbst und die
 * Tagesauswertung bekommen diese Werkzeuge nicht (siehe JarvisAgent.versuche): Agenten starten keine Agenten.
 */
class AgentenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "agenten"
    override val name = "Agenten und Protokoll"
    override val beschreibung = "Agenten für längere Arbeiten beauftragen und verwalten; das Aktivitäts-Protokoll von Jarvis lesen (auch Fehler)."
    override val hinweise =
        "Verlangt Frank ausdrücklich eine Recherche, eine Ausarbeitung oder einen Agenten, startest du mit agent_starten " +
            "einen Agenten (meist Recherche). Normale Fragen nach Aktuellem beantwortest du dagegen selbst mit der direkten Websuche (siehe WEBSUCHE STATT AGENT); " +
            "Internetbedarf, Unsicherheit oder längere Bearbeitung allein sind kein Grund für einen Agenten. Er arbeitet bis zu 25 Minuten im Hintergrund und meldet sich per Benachrichtigung; sein vollständiger Bericht kommt als schön formatierter Text ins Gespräch zurück, " +
            "im Jarvis-Chat direkt, in ChatGPT über vorgaenge (gib ihn dort ausführlich als normale, gegliederte Antwort wieder, nicht nur als Zusammenfassung). " +
            "Das ist die Vorgabe: Chattext, kein PDF, keine Datei. Ein PDF nur mit als_pdf=true, und das nur, wenn Frank ausdrücklich ein PDF verlangt; „kein PDF“, „nicht als PDF“, „nur Text“ heißt als_pdf weglassen. " +
            "In die Ablage legt er nur mit ablegen=true, und das nur, wenn Frank ausdrücklich die Ablage verlangt " +
            "(„… und leg das in der Ablage ab“); Recherche, Agent oder PDF allein sind kein Speicherwunsch. Format und Ablage sind unabhängig: „als Text in die Ablage“ = ablegen ohne als_pdf, „als PDF“ = als_pdf ohne ablegen. " +
            "Will Frank ein fertiges Ergebnis später behalten: in_ablage_uebernehmen mit vorgang. " +
            "Gib ihm den vollständigen Auftrag mit; eine beauftragte gründliche Recherche versuchst du nicht selbst im Gespräch. " +
            "agent_starten liefert eine Vorgangskennung; angenommen heißt nicht fertig. Stand und Ergebnis über vorgaenge, nach dem Mitteilen mit aktion mitgeteilt bestätigen. " +
            "Fragt Frank, warum etwas nicht geklappt hat, oder meldet ein Werkzeug einen Fehler, lies mit protokoll_lesen nach, was Jarvis zuletzt getan hat und woran es scheiterte."

    override fun stoerung(): String? = null

    private val app = context.applicationContext

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
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
                beschreibung = "Jarvis: beauftragt einen Agenten von Jarvis mit einer längeren Arbeit, NUR wenn Frank das ausdrücklich verlangt, zum Beispiel eine gründliche Recherche („Jarvis, recherchiere ausführlich, wie man … am besten baut“), " +
                    "einem gestalteten PDF-Dokument mit Illustrationen (nur auf ausdrücklichen Wunsch), einer Machbarkeitsprüfung einer Idee, oder den Agenten Code-Analyse mit der Auswertung einer App in Franks Repo (er liest den Code und liefert den Stand samt fertigem Auftrag für ein Programmier-Werkzeug; er ändert nichts). " +
                    "Nicht für normale Fragen nach Aktuellem (Verfügbarkeit, Preise, Neuigkeiten): die beantwortest du selbst mit web_suche. " +
                    "Der Agent arbeitet bis zu 25 Minuten selbstständig im Hintergrund auf Franks Handy (eigenes Modell, Internet-Suche, Bilderzeugung, Zugriff auf Ideen, " +
                    "Aufgaben, Kalender, Biomarker), liefert seinen vollständigen Bericht als formatierten Text ins Gespräch (Jarvis-Chat direkt, ChatGPT über vorgaenge) und meldet sich per Benachrichtigung. " +
                    "Ein PDF entsteht NUR mit als_pdf=true, in die Ablage kommt das Ergebnis NUR mit ablegen=true; beides unabhängig voneinander. Dieser Aufruf kehrt sofort zurück " +
                    "und liefert eine Vorgangskennung (zum Beispiel V7K2P): Der Auftrag ist dann ANGENOMMEN, nicht fertig. Sag Frank das kurz und rede normal weiter. " +
                    "Stand und Ergebnis holst du später mit vorgaenge (auch in einem neuen Gespräch); ein wiederholter Aufruf mit demselben Auftrag startet nichts doppelt. Mit per_mail wird der Bericht zusätzlich an Frank gemailt.",
                schema = schema(
                    "agent" to text("Name des Agenten, zum Beispiel Recherche oder Machbarkeit (siehe agenten)."),
                    "auftrag" to text("Der vollständige Auftrag mit allem Wissen aus dem Gespräch: Ziel, Rahmen, was Frank wichtig ist, gewünschte Form des Ergebnisses. Ob ein PDF entsteht, steuert allein als_pdf, nicht dieser Text."),
                    "als_pdf" to schalter("true = zusätzlich ein professionell gestaltetes PDF-Dokument (mit Bildern und Infografiken) erzeugen. NUR wenn Frank ausdrücklich ein PDF verlangt („mach mir das als PDF“); " +
                        "verneint („kein PDF“, „nicht als PDF“, „nur Text“) oder ohne Formatwunsch weglassen: Dann kommt der Bericht nur als Chattext. Ein PDF-Wunsch allein ist kein Ablagewunsch."),
                    "ablegen" to schalter("true = das Ergebnis dauerhaft in der Ablage speichern. NUR wenn Frank das ausdrücklich verlangt (zum Beispiel „und leg das in der Ablage ab“); sonst weglassen."),
                    "per_mail" to schalter("true = den fertigen Bericht zusätzlich per E-Mail an Frank schicken."),
                    "vorlesen" to schalter("true = Jarvis liest eine Zusammenfassung des fertigen Berichts auf dem Handy laut vor, sobald er da ist. Im Sprachgespräch unnötig: Dort liest Jarvis das Ergebnis ohnehin vollständig vor."),
                    pflicht = listOf("agent", "auftrag"),
                ),
                nurLesen = false,
            ) { a ->
                val plan = Agenten.finde(app, a.optString("agent"))
                val auftrag = a.optString("auftrag")
                when {
                    plan == null -> Ergebnis("Diesen Agenten gibt es nicht. Vorhanden: " + Agenten.alle(app).joinToString { it.name } + ". Mit agenten (aktion=anlegen) lässt sich ein neuer bauen.", fehler = true)
                    auftrag.isBlank() -> Ergebnis("Der Auftrag ist leer.", fehler = true)
                    // Franks Vorgabe: Ein Agent (allen voran Recherche) startet nur auf seinen ausdrücklichen Wunsch. Geprüft an seinen eigenen Worten,
                    // nicht an der Deutung des Modells; ohne sie (ChatGPT über MCP, Live ohne Mitschrift) gilt die Regel nur über die Anweisungen.
                    !kotlinx.coroutines.currentCoroutineContext()[ProgrammierChat.FranksWorte].let { SuchRegel.darfStarten(plan.name, it?.aktuell, it?.vorher, it?.angebot) } -> {
                        de.frank.jarvis.observability.Logbuch.system("agent_starten abgelehnt", "Recherche ohne ausdrücklichen Wunsch")
                        Ergebnis(SuchRegel.NICHT_GESTARTET, fehler = true)
                    }
                    !CodexAuthManager(app).isConnected -> Ergebnis("Jarvis ist nicht mit seinem Modell verbunden. Frank muss sich in Jarvis unter Einstellungen bei ChatGPT anmelden.", fehler = true)
                    // Unter der Sperre des Vorgangsbuchs: erst nach Doppeln suchen, dann synchron reservieren. Der Lauf beginnt erst
                    // asynchron im Dienst; ein paralleler oder wiederholter Aufruf (Verbindungsabbruch) sähe sonst noch einen freien Platz.
                    // Im Sprachgespräch begonnen (Gesprächsmodus mit Vorlesen oder Live): Der Vorgang erbt das von selbst, unabhängig vom Parameter vorlesen.
                    else -> when (val annahme = Vorgaenge.buch(app).annehmen(plan.name, auftrag, a.optBoolean("ablegen"), herkunftsGespraech(), a.optBoolean("als_pdf"),
                        sprache = kotlinx.coroutines.currentCoroutineContext()[de.frank.jarvis.speech.SprachUrsprung]?.modus?.kennung.orEmpty()) { Agenten.reserviere(plan.name, auftrag) }) {
                        is Annahme.Doppelt -> Ergebnis("Dieser Auftrag ist schon angenommen, es wurde nichts doppelt gestartet: ${Vorgaenge.zeile(annahme.vorgang)}\n" +
                            "Sag Frank den Stand in einem Satz. Das Ergebnis holst du mit vorgaenge (aktion lesen, id ${annahme.vorgang.id}), sobald es fertig ist.")
                        Annahme.KeinPlatz -> Ergebnis("Nicht angenommen: Es arbeiten schon ${Agenten.HOECHSTENS} Agenten. Bitte warten, bis einer fertig ist (Stand mit vorgaenge).", fehler = true)
                        is Annahme.Neu -> {
                            val id = annahme.vorgang.id
                            if (!JarvisDienst.agentStarten(app, plan.name, auftrag, a.optBoolean("per_mail"), a.optBoolean("vorlesen"), id)) {
                                Agenten.gibFrei(plan.name, auftrag)
                                Vorgaenge.buch(app).beende(id, Vorgang.FEHLGESCHLAGEN, null, "Der Hintergrunddienst ließ sich nicht starten")
                                Ergebnis("Nicht gestartet: Der Hintergrunddienst von Jarvis ließ sich nicht starten (Vorgang $id fehlgeschlagen). Frank soll Jarvis einmal öffnen.", fehler = true)
                            } else Ergebnis("ANGENOMMEN, noch nicht fertig: Vorgang $id, Agent ${plan.name}. Er arbeitet im Hintergrund, meist fünf bis zwanzig Minuten. " +
                                (if (annahme.vorgang.gespraech.isNotEmpty()) "Das Ergebnis erscheint in diesem Jarvis-Gespräch" else "Das Ergebnis holst du mit vorgaenge") +
                                (if (a.optBoolean("als_pdf")) " als Text und gestaltetes PDF" else " als formatierter Text (kein PDF)") +
                                (if (a.optBoolean("ablegen")) " und wird in der Ablage gespeichert" else ", es wird NICHT in der Ablage gespeichert") +
                                (if (a.optBoolean("per_mail")) " und kommt per E-Mail" else "") + "; Frank bekommt eine Benachrichtigung" +
                                (if (annahme.vorgang.sprache.isNotEmpty()) " und Jarvis liest das Ergebnis von selbst vollständig vor, sobald es da ist, auch wenn das Sprachgespräch bis dahin endet. " else ". ") +
                                "Sag Frank in einem Satz, dass der Auftrag angenommen ist, und rede normal weiter. Später: vorgaenge (aktion lesen, id $id).")
                        }
                    }
                }
            },
            Werkzeug(
                name = "vorgaenge",
                titel = "Hintergrundaufträge: Stand und Ergebnisse",
                beschreibung = "Jarvis: Stand und Ergebnisse von Franks Hintergrundaufträgen (Agenten aus agent_starten und Programmieraufträge), auch aus früheren Gesprächen und nach einem Neustart. " +
                    "Ohne aktion (oder aktion=liste): kurze Liste der laufenden und der fertigen, die Frank noch nicht mitgeteilt wurden, je mit Kennung und Stand " +
                    "(angenommen, läuft, Rückfrage, fertig, fehlgeschlagen, abgebrochen); mit alle=true auch schon Mitgeteiltes der letzten 7 Tage. " +
                    "aktion=lesen mit id: Details und das Ergebnis (Bericht aus der Ablage, gekürzt). aktion=mitgeteilt mit ids: bestätigt, dass du Frank die Ergebnisse in diesem Gespräch gesagt hast; " +
                    "erst dann verschwinden sie aus der Liste. Rufe es, wenn der Startkontext offene oder fertige Vorgänge zeigt oder ein Werkzeug-Ergebnis „VORGANG FERTIG“ meldet. " +
                    "Ein Abruf startet und beendet nie etwas; abbrechen geht nur mit vorgang_abbrechen.",
                schema = schema(
                    "aktion" to text("liste (Vorgabe), lesen oder mitgeteilt.", listOf("liste", "lesen", "mitgeteilt")),
                    "id" to text("Bei lesen: die Vorgangskennung, zum Beispiel V7K2P."),
                    "ids" to textListe("Bei mitgeteilt: die Kennungen, deren Ergebnis du Frank gesagt hast."),
                    "alle" to schalter("Bei liste: true = auch schon Mitgeteiltes der letzten 7 Tage."),
                ),
                // Ändert nur die Buchführung (abgerufen, mitgeteilt), nie einen Lauf: als lesend markiert, damit ChatGPT es im Gespräch ohne Rückfrage nutzt.
                nurLesen = true,
            ) { a -> if (a.optString("aktion").equals("abbrechen", ignoreCase = true)) Ergebnis("Abbrechen geht nur mit vorgang_abbrechen.", fehler = true) else vorgaenge(a) },
            Werkzeug(
                name = "vorgang_abbrechen",
                titel = "Hintergrundauftrag abbrechen",
                beschreibung = "Jarvis: bricht einen laufenden oder noch nicht angelaufenen Agenten-Auftrag ab (Kennung aus agent_starten oder vorgaenge). Nur wenn Frank das ausdrücklich will. " +
                    "Was der Agent bis dahin erzeugt hat, bleibt erhalten (in der Ablage nur, wenn Frank sie für diesen Auftrag verlangt hatte).",
                schema = schema("id" to text("Die Vorgangskennung, zum Beispiel V7K2P."), pflicht = listOf("id")),
                nurLesen = false,
                loeschend = true,
            ) { a -> vorgaenge(a.put("aktion", "abbrechen")) },
            Werkzeug(
                name = "protokoll_lesen",
                titel = "Protokoll lesen",
                beschreibung = "Jarvis: liest das Aktivitäts-Protokoll von Jarvis (dasselbe wie in der App unter Aktivität): was Jarvis, ChatGPT und die Agenten zuletzt getan haben, " +
                    "mit Uhrzeit, Werkzeug, Ergebnis und Fehlern. Nutze es, wenn Frank fragt, warum etwas nicht geklappt hat, was ein Agent gemacht hat oder was zuletzt passiert ist.",
                schema = schema(
                    "anzahl" to zahl("Wie viele Einträge, neueste zuerst; Vorgabe 40, höchstens 200."),
                    "nur_fehler" to schalter("true = nur fehlgeschlagene Einträge."),
                    "suche" to text("Optional: nur Einträge, deren Werkzeug oder Text dieses Wort enthält (zum Beispiel „Agent“, „Bild“, „PDF“)."),
                ),
                nurLesen = true,
            ) { a ->
                val anzahl = a.optInt("anzahl", 40).coerceIn(1, 200)
                val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
                val treffer = Protokoll.eintraege.value
                    .filter { !a.optBoolean("nur_fehler") || !it.ok }
                    .filter { suche.isEmpty() || suche in it.werkzeug.lowercase(Locale.GERMAN) || suche in it.text.lowercase(Locale.GERMAN) }
                    .take(anzahl)
                if (treffer.isEmpty()) Ergebnis("Im Protokoll steht dazu nichts (es hält die letzten 200 Einträge).")
                else Ergebnis(buildString {
                    append("Protokoll von Jarvis, neueste zuerst (").append(treffer.size).append(" Einträge):\n")
                    treffer.forEach { e ->
                        append(ZEIT.format(Instant.ofEpochMilli(e.zeit).atZone(ZoneId.systemDefault()))).append(" · ").append(e.quelle.anzeige).append(" · ").append(e.werkzeug)
                            .append(if (e.ok) ": " else ": FEHLER: ").append(e.text.replace('\n', ' ')).append('\n')
                    }
                }.trim())
            },
        )
    }

    /** Das Jarvis-Gespräch, aus dem gerade beauftragt wird (wie bei Programmieraufträgen). Leer bei Aufrufen aus ChatGPT. */
    private suspend fun herkunftsGespraech(): String {
        val ctx = kotlinx.coroutines.currentCoroutineContext()
        return ctx[ProgrammierChat.GespraechKennung]?.id ?: if (ctx[ProgrammierChat.AusChatGpt] != null) "" else ProgrammierChat.aktivesGespraech.orEmpty()
    }

    private fun vorgaenge(a: org.json.JSONObject): Ergebnis {
        val buch = Vorgaenge.buch(app)
        return when (a.optString("aktion", "liste").lowercase()) {
            "lesen" -> {
                val v = buch.finde(a.optString("id")) ?: return Ergebnis("Einen Vorgang „${a.optString("id")}“ gibt es nicht (oder er ist älter als ${VorgangsBuch.TAGE} Tage). Liste mit vorgaenge.", fehler = true)
                if (!v.offen) buch.abgerufen(listOf(v.id))
                val bericht = when {
                    v.ergebnis.isNotEmpty() -> Ablage.finde(app, v.ergebnis)?.let { runCatching { it.readText() }.getOrNull() }?.let { text ->
                        "\n\nERGEBNIS (Bericht aus der Ablage" + (if (text.length > BERICHT) ", gekürzt; vollständig mit ablage_lesen titel „${v.ergebnis}“" else "") + "):\n" + text.take(BERICHT)
                    } ?: "\n\nDer Ablage-Eintrag „${v.ergebnis}“ ist nicht mehr da."
                    v.vorlaeufig.isNotEmpty() -> {
                        val vs = de.frank.jarvis.ablage.Vorlaeufig.speicher(app)
                        vs.findeGenau(v.vorlaeufig)?.let { e ->
                            val text = vs.text(e)
                            "\n\nERGEBNIS (vorläufig, NICHT in der Ablage" + (if (text.length > BERICHT) ", gekürzt; vollständig mit ablage_lesen titel „${v.vorlaeufig}“" else "") + "):\n" + text.take(BERICHT) +
                                (if (e.hatDateien) "\n\nDATEIEN dazu: " + e.anhaenge.joinToString { it.originalName } + " (zeigen mit ablage_zeigen titel „${v.vorlaeufig}“)." else "") +
                                "\nIn die Ablage nur, wenn Frank es will: in_ablage_uebernehmen mit vorgang ${v.id}."
                        } ?: "\n\nDas vorläufige Ergebnis ist nicht mehr da (vorläufige Ergebnisse bleiben ${de.frank.jarvis.ablage.Vorlaeufig.AUFBEWAHRUNG_TAGE} Tage)."
                    }
                    else -> ""
                }
                Ergebnis(Vorgaenge.zeile(buch.finde(v.id) ?: v) + bericht + when {
                    v.offen -> "\n\nNoch nicht fertig. Nicht wiederholt abfragen; der Stand kommt mit dem nächsten passenden Werkzeug-Ergebnis."
                    else -> "\n\nSobald du Frank das Ergebnis gesagt hast: vorgaenge mit aktion mitgeteilt und ids [${v.id}]."
                })
            }
            "mitgeteilt" -> {
                val liste = a.optJSONArray("ids")
                val ids = (0 until (liste?.length() ?: 0)).map { liste!!.optString(it).trim() }.filter { it.isNotEmpty() } + listOfNotNull(a.optString("id").trim().takeIf { it.isNotEmpty() })
                if (ids.isEmpty()) return Ergebnis("Es fehlen die Kennungen (ids).", fehler = true)
                val bestaetigt = buch.mitgeteilt(ids)
                val rest = ids.filter { id -> bestaetigt.none { it.id.equals(id, ignoreCase = true) } }
                Ergebnis((if (bestaetigt.isEmpty()) "Nichts bestätigt." else "Als mitgeteilt vermerkt: " + bestaetigt.joinToString { it.id } + ".") +
                    (if (rest.isEmpty()) "" else " Nicht bestätigt (unbekannt oder noch offen): " + rest.joinToString() + "."), fehler = bestaetigt.isEmpty())
            }
            "abbrechen" -> {
                val v = buch.finde(a.optString("id")) ?: return Ergebnis("Einen Vorgang „${a.optString("id")}“ gibt es nicht.", fehler = true)
                when {
                    !v.offen -> Ergebnis("Vorgang ${v.id} ist schon beendet: ${Vorgang.standText(v.stand)}.")
                    Agenten.brichAb(v.id) -> Ergebnis("Vorgang ${v.id} wird abgebrochen.")
                    else -> {
                        // Noch nicht angelaufen: als abgebrochen eintragen. Der Dienst startet ihn dann nicht mehr, und ein Lauf,
                        // der gerade erst anläuft, sieht den Eintrag in fuehreAus und hört sofort auf.
                        buch.beende(v.id, Vorgang.ABGEBROCHEN, null, "von Frank abgebrochen, bevor er anlief")
                        Agenten.gibFrei(v.agent, v.auftrag)
                        // Lief er inzwischen doch an, auch den Lauf selbst beenden.
                        Agenten.brichAb(v.id)
                        Ergebnis("Vorgang ${v.id} ist abgebrochen.")
                    }
                }
            }
            else -> Ergebnis(vorgangsBlock(app, alle = a.optBoolean("alle")).ifEmpty { "Keine laufenden und keine neuen fertigen Hintergrundaufträge." })
        }
    }

    private companion object {
        val ZEIT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M. HH:mm:ss", Locale.GERMAN)
        /** So viel vom Bericht kommt bei vorgaenge lesen mit; der Rest über ablage_lesen. */
        const val BERICHT = 20_000
    }
}

/**
 * Die Hintergrundaufträge als kurzer Block (für vorgaenge und den Startkontext): Agenten-Vorgänge, die laufen oder fertig,
 * aber noch nicht mitgeteilt sind, dazu offene und in den letzten 24 Stunden beendete Programmieraufträge. Leer = nichts.
 */
fun vorgangsBlock(context: Context, alle: Boolean = false): String {
    val app = context.applicationContext
    val agenten = Vorgaenge.buch(app).liste(alle).map { Vorgaenge.zeile(it) }
    val programmieren = runCatching { Register.alle(app).filterIsInstance<ProgrammierFaehigkeit>().firstOrNull()?.vorgangsZeilen(alle) }.getOrNull().orEmpty()
    if (agenten.isEmpty() && programmieren.isEmpty()) return ""
    return buildString {
        append("HINTERGRUNDAUFTRÄGE (Kennung in eckigen Klammern; angenommen heißt nicht fertig):\n")
        (agenten + programmieren).forEach { append("- ").append(it).append('\n') }
        if (agenten.any { "noch nicht mitgeteilt" in it || "nicht als mitgeteilt" in it }) append("Fertige Ergebnisse: vorgaenge aktion lesen mit id; danach aktion mitgeteilt.\n")
        // Laufende Programmieraufträge sind kein Ziel für neue Wünsche: Ein Nachtrag nur, wenn Frank ihn ausdrücklich dorthin verlangt.
        if (programmieren.isNotEmpty()) append("Neue Programmierwünsche sind neue Aufträge; Nachträge an diese Aufträge nur, wenn Frank sie ausdrücklich in eine von ihm bezeichnete Sitzung verlangt.\n")
    }.trim()
}
