package de.frank.ziele.ki

import de.frank.ziele.text.Quellen
import android.content.Context
import android.util.Log
import de.frank.ziele.auth.ChatTurn
import de.frank.ziele.auth.CodexAuthManager
import de.frank.ziele.auth.CodexModel
import de.frank.ziele.auth.ReasoningEffort
import de.frank.ziele.bruecke.Jarvis
import de.frank.ziele.data.Abgelehnt
import de.frank.ziele.data.Aufwand
import de.frank.ziele.data.Einstellungen
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.Kontext
import de.frank.ziele.data.Rueckmeldung
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import de.frank.ziele.data.ZieleRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Der Aufgaben-Agent: recherchiert zuerst, wie ein Ziel am besten erreicht wird, und schlägt darauf aufbauend
 * notwendige und unterstützende Aufgaben vor, zugeschnitten auf Frank (Kontextdatei + Jarvis' Live-Gedächtnis).
 *
 * Läuft im Prozess-Bereich, nicht im ViewModel: Verlässt Frank den Bildschirm, rechnet die Auswertung weiter
 * und das Ergebnis landet trotzdem in der Datenbank. Auch Jarvis stößt sie über die Brücke an.
 */
object Auswertung {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val laeufe = mutableMapOf<Long, Job>()

    /** Ziel-id → Schritt, der gerade läuft (für die Fortschrittsanzeige). */
    private val _stand = MutableStateFlow<Map<Long, String>>(emptyMap())
    val stand: StateFlow<Map<Long, String>> = _stand.asStateFlow()

    /** Ziel-id → letzte Fehlermeldung (verschwindet beim nächsten Start). */
    private val _fehler = MutableStateFlow<Map<Long, String>>(emptyMap())
    val fehler: StateFlow<Map<Long, String>> = _fehler.asStateFlow()

    /** Ziel-id → Art des laufenden Laufs (das Gespräch zeigt „denkt nach“ nur bei einer Rückmeldung). */
    private val _modus = MutableStateFlow<Map<Long, Modus>>(emptyMap())
    val modus: StateFlow<Map<Long, Modus>> = _modus.asStateFlow()

    fun laeuft(zielId: Long): Boolean = _stand.value.containsKey(zielId)

    /**
     * VOLL = komplette neue Auswertung (offene Vorschläge werden ersetzt).
     * NACHFUELLEN = ergänzende Recherche aus anderer Richtung, angehängt an die bestehende, füllt die freien Plätze.
     * RUECKMELDUNG = antwortet auf Franks Rückmeldung im Gespräch und ergänzt bei Bedarf Vorschläge.
     * Nachfüllen und Rückmeldung fügen nur hinzu, sie ersetzen nichts.
     */
    enum class Modus { VOLL, NACHFUELLEN, RUECKMELDUNG }

    /**
     * Startet die Auswertung; läuft schon eine für dieses Ziel, passiert nichts.
     * [fertig] bekommt den Fehlertext (null = Erfolg) und eine Erfolgsmeldung.
     */
    fun starte(context: Context, zielId: Long, wunsch: String = "", modus: Modus = Modus.VOLL, fertig: (String?, String) -> Unit = { _, _ -> }): Boolean {
        val app = context.applicationContext
        if (!synchronized(this) { anlegen(app, zielId, wunsch, modus, fertig) }) return false
        // Außerhalb der Sperre (Systemaufruf) und erst nach dem Eintrag in „stand“: Der Dienst beendet sich, sobald keine Auswertung mehr läuft.
        AuswertungsDienst.starte(app)
        return true
    }

    private fun anlegen(app: Context, zielId: Long, wunsch: String, modus: Modus, fertig: (String?, String) -> Unit): Boolean {
        if (laeufe[zielId]?.isActive == true) return false
        _fehler.update { it - zielId }
        schritt(zielId, "Bereite vor …")
        _modus.update { it + (zielId to modus) }
        laeufe[zielId] = bereich.launch {
            var erfolg = ""
            val ergebnis = try {
                erfolg = rechne(app, zielId, wunsch, modus)
                null
            } catch (c: CancellationException) {
                "Abgebrochen."
            } catch (e: Throwable) {
                Log.w("ZieleAuswertung", "Auswertung $zielId ($modus)", e)
                (e.message ?: e.javaClass.simpleName).also { m -> _fehler.update { it + (zielId to m) } }
            } finally {
                _stand.update { it - zielId }
                _modus.update { it - zielId }
                synchronized(this@Auswertung) { laeufe.remove(zielId) }
            }
            // Erst die Meldung im Hintergrund, dann der Hinweis in der App; ein Fehler im Hinweis darf nichts mitreißen.
            if (ergebnis != "Abgebrochen.") runCatching {
                val titel = ZieleRepository.get(app).ziel(zielId)?.titel ?: "Ziel"
                AuswertungsDienst.fertig(app, zielId, titel, ergebnis ?: erfolg, ergebnis != null)
            }
            runCatching { fertig(ergebnis, erfolg) }.onFailure { Log.w("ZieleAuswertung", "Rückmeldung $zielId", it) }
        }
        return true
    }

    fun abbrechen(zielId: Long) { synchronized(this) { laeufe[zielId] }?.cancel() }

    private fun schritt(zielId: Long, text: String) = _stand.update { it + (zielId to text) }

    /** Rechnet einen Lauf im gewählten [modus] und liefert die Erfolgsmeldung für Frank. */
    private suspend fun rechne(app: Context, zielId: Long, wunsch: String, modus: Modus): String {
        val repo = ZieleRepository.get(app)
        val e = Einstellungen.get(app)
        val auth = CodexAuthManager(app)
        val modell = e.modell
        if (!auth.kannDenken(modell)) {
            throw IllegalStateException(if (modell.istAnthropic) "Für ${modell.label} fehlt der Anthropic-API-Schlüssel (Einstellungen → KI)." else "Bitte zuerst in den Einstellungen mit ChatGPT verbinden.")
        }
        val ziel = repo.ziel(zielId) ?: throw IllegalStateException("Das Ziel gibt es nicht mehr.")
        val bisher = repo.vorschlaege(zielId)
        val abgelehnt = repo.abgelehnt(zielId)
        val gespraech = repo.rueckmeldungen(zielId)
        // Noch nicht umgesetzte Rückmeldungen fließen in diesen Lauf ein und gelten danach als bedacht.
        val offeneRueckmeldungen = gespraech.filter { it.vonFrank && !it.verarbeitet }.map { it.id }

        // Bei der vollen Auswertung bleiben nur übernommene und ausgeblendete Vorschläge, die übrigen werden ersetzt;
        // Nachfüllen und Rückmeldung behalten alles und ergänzen nur.
        val behalten = if (modus == Modus.VOLL) bisher.filter { it.aufgabeId != null || it.verworfen || it.angenommen } else bisher
        // Freie Plätze je Kategorie: 10 minus offene, sichtbare Vorschläge (erledigte und ausgeblendete belegen keinen Platz).
        val freiJe = freiePlaetze(behalten)
        // Alles belegt: keine Recherche, offene Rückmeldungen bleiben offen und fließen in den nächsten Lauf mit freien Plätzen.
        if (modus != Modus.RUECKMELDUNG && freiJe.values.sum() <= 0) {
            return "Alle $PLAETZE_JE_KATEGORIE notwendigen und $PLAETZE_JE_KATEGORIE unterstützenden Plätze sind belegt. Lehne Vorschläge ab oder blende sie aus, dann füllt „Neue Aufgaben“ genau diese Lücken." +
                if (offeneRueckmeldungen.isNotEmpty()) " Deine offene Rückmeldung kommt dabei zuerst dran." else ""
        }

        val persoenlich = e.kontextNutzen
        if (persoenlich) schritt(zielId, if (e.jarvisKontext) "Lese deinen Kontext und Jarvis' Live-Gedächtnis …" else "Lese deinen Kontext …")
        val kontextDatei = if (persoenlich) Kontext.lies(app).trim() else ""
        val gedaechtnis = if (persoenlich && e.jarvisKontext) runCatching { Jarvis.gedaechtnis(app) }.getOrNull()?.take(24_000) else null

        val ueberFrank = buildString {
            append("HEUTE: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))).append("\n\n")
            if (!persoenlich) append(
                "OHNE PERSÖNLICHEN KONTEXT: Frank möchte diesmal allgemeine Empfehlungen, die für jeden Menschen mit diesem Ziel gelten. " +
                    "Stimme nichts auf seine persönliche Lage, seinen Alltag oder seine Daten ab.\n\n",
            ) else if (kontextDatei.isNotEmpty()) append("KONTEXTDATEI (von Frank selbst eingesprochen):\n").append(kontextDatei.takeLast(12_000)).append("\n\n")
            if (gedaechtnis != null) append("JARVIS-LIVE-GEDÄCHTNIS (Franks persönlicher Assistent, aus allen seinen Apps):\n").append(gedaechtnis).append("\n\n")
            if (persoenlich && kontextDatei.isEmpty() && gedaechtnis == null) append("(Kein weiterer Kontext über Frank vorhanden.)\n\n")
        }
        val zielText = zielBeschreibung(ziel)
        val vorlieben = vorliebenBlock(zielId, if (persoenlich) repo.alleVorschlaegeEinmal() else bisher)
        val sperre = sperrBlock(abgelehnt)
        val rueckmeldungen = gespraechBlock(gespraech)

        // ---- Schritt 1: Recherche (voll) bzw. Ergänzungsrecherche (nachfüllen); die Rückmeldung baut auf der vorhandenen auf ----
        val bericht = when (modus) {
            Modus.VOLL -> {
                schritt(zielId, if (e.recherche) "Recherchiere, wie man das Ziel am besten erreicht …" else "Denke über den besten Weg nach …")
                auth.streamChat(
                    instructions = RECHERCHE_ANWEISUNG + if (e.recherche) RECHERCHE_MIT_WEB else RECHERCHE_OHNE_WEB,
                    turns = listOf(ChatTurn("user", "$zielText\n\n$ueberFrank$vorlieben$rueckmeldungen" + if (wunsch.isNotBlank()) "BESONDERER WUNSCH FÜR DIESE AUSWERTUNG: $wunsch\n" else "")),
                    model = modell,
                    reasoningEffort = e.denkstufe,
                    websuche = e.recherche,
                ).trim().also { if (it.isBlank()) throw IllegalStateException("Die Recherche hat keinen Bericht geliefert.") }
            }
            Modus.NACHFUELLEN -> {
                schritt(zielId, if (e.recherche) "Recherchiere weiter im Internet, aus anderer Richtung …" else "Denke über weitere Wege zum Ziel nach …")
                val alt = Quellen.teilen(ziel.bericht).text
                val ergaenzung = auth.streamChat(
                    instructions = ERGAENZUNG_ANWEISUNG + if (e.recherche) RECHERCHE_MIT_WEB else RECHERCHE_OHNE_WEB,
                    turns = listOf(ChatTurn("user", buildString {
                        append(zielText).append("\n\n")
                        // Die gesamte bisherige Recherche samt aller Ergänzungen, damit nichts doppelt recherchiert wird.
                        append("BISHERIGE RECHERCHE (bitte nicht wiederholen, sondern ergänzen):\n").append(alt.ifBlank { "(noch keine)" }.takeLast(RECHERCHE_MAX)).append("\n\n")
                        if (ziel.strategie.isNotBlank()) append("BISHERIGER WEG: ").append(ziel.strategie).append("\n\n")
                        append(ueberFrank).append(vorlieben).append(rueckmeldungen).append(sperre)
                        if (behalten.isNotEmpty()) append("SCHON VORHANDENE AUFGABEN:\n").append(behalten.joinToString("\n") { "- " + it.titel }).append("\n\n")
                        if (wunsch.isNotBlank()) append("BESONDERER WUNSCH: ").append(wunsch).append('\n')
                    })),
                    model = modell,
                    reasoningEffort = e.denkstufe,
                    websuche = e.recherche,
                ).trim()
                if (ergaenzung.isBlank()) throw IllegalStateException("Die ergänzende Recherche hat nichts geliefert.")
                berichtAnfuegen(ziel.bericht, ergaenzung)
            }
            Modus.RUECKMELDUNG -> ziel.bericht
        }

        // ---- Schritt 2: Aufgaben ableiten ----
        schritt(zielId, if (modus == Modus.RUECKMELDUNG) "Denke über deine Rückmeldung nach …" else "Leite die wichtigsten Aufgaben ab …")
        // Etwas mehr erbitten als frei ist: Das Aussieben streicht oft einige, danach zählen nur die besten je Kategorie.
        val anzahl = mengeText(freiJe.mapValues { (_, n) -> if (n > 0) n + PUFFER else 0 })
        val planAnfrage = buildString {
            append(zielText).append("\n\n")
            if (bericht.isNotBlank()) Quellen.teilen(bericht).text.let { t ->
                append("RECHERCHE-BERICHT:\n").append(if (modus == Modus.VOLL) t.take(RECHERCHE_MAX) else t.takeLast(RECHERCHE_MAX)).append("\n\n")
            }
            if (modus != Modus.VOLL && ziel.strategie.isNotBlank()) append("BISHERIGER WEG: ").append(ziel.strategie).append("\n\n")
            append(ueberFrank).append(vorlieben)
            if (behalten.isNotEmpty()) {
                append("SCHON VORHANDEN (nicht noch einmal vorschlagen; du darfst sie als Voraussetzung über ihren Schlüssel nennen; ")
                append("ihr Wortlaut kann sprachlich missglückt sein, nimm ihn nicht als Stilvorbild):\n")
                behalten.forEach { v ->
                    append("- [").append(v.schluessel).append("] ").append(v.titel)
                    append(when { v.verworfen -> " (von Frank verworfen, nichts Ähnliches mehr vorschlagen)"; v.erledigt -> " (erledigt)"; v.aufgabeId != null -> " (übernommen, in Arbeit)"; v.angenommen -> " (von Frank angenommen: gefällt ihm, will er machen)"; else -> " (offener Vorschlag)" })
                    append('\n')
                }
                append('\n')
            }
            append(sperre)
            append(rueckmeldungen)
            if (wunsch.isNotBlank()) append("BESONDERER WUNSCH: ").append(wunsch).append("\n\n")
            when (modus) {
                Modus.VOLL -> append("Liefere $anzahl neue Vorschläge.")
                Modus.NACHFUELLEN -> append(
                    "Frank hat Vorschläge abgelehnt oder ausgeblendet; fülle die freien Plätze mit $anzahl neuen Vorschlägen, " +
                        "vor allem aus den neuen Erkenntnissen der Ergänzung: andere Wege zum Ziel, die bisher fehlen. " +
                        "Greife dabei auch sinnvolle Anregungen aus FRANKS RÜCKMELDUNGEN (oben) auf, für die es noch keine Aufgabe gibt. " +
                        (if (offeneRueckmeldungen.isNotEmpty()) RUECKMELDUNG_VORRANG else "") +
                        "Jeder Vorschlag muss inhaltlich eigenständig sein: nichts, was einer abgelehnten oder vorhandenen Aufgabe gleicht, " +
                        "ähnelt, sie umformuliert oder sinngleich ersetzt.",
                )
                Modus.RUECKMELDUNG -> append("Antworte auf Franks neue Rückmeldung im Gespräch oben und setze ihre sinnvollen Anregungen in Aufgaben um.")
            }
        }
        val anweisung = if (modus == Modus.RUECKMELDUNG) PLAN_ANWEISUNG + RUECKMELDUNG_ZUSATZ else PLAN_ANWEISUNG
        val antwort = auth.streamChat(
            instructions = anweisung,
            turns = listOf(ChatTurn("user", planAnfrage)),
            model = modell,
            reasoningEffort = e.denkstufe,
        )
        val plan = jsonAus(antwort) ?: throw IllegalStateException("Die KI hat keinen lesbaren Aufgabenplan geliefert. Bitte noch einmal versuchen.")

        val vorhanden = behalten.map { it.titel to it.text }
        val kandidaten = kandidatenAus(plan)
        if (kandidaten.isNotEmpty()) schritt(zielId, "Prüfe gegen Abgelehntes und Vorhandenes …")
        var objekte = aussieben(auth, modell, kandidaten, abgelehnt, vorhanden)
        if (modus != Modus.RUECKMELDUNG) {
            objekte = auswahl(objekte, freiJe)
            // Fehlen nach dem Aussieben noch Vorschläge, einmal gezielt nachfordern (ohne neue Websuche).
            val fehlt = Kategorie.entries.associateWith { k -> (freiJe[k] ?: 0) - objekte.count { Kategorie.von(it.optString("kategorie")) == k } }
            if (fehlt.values.any { it > 0 }) {
                schritt(zielId, "Ergänze die noch freien Plätze …")
                val schonGewaehlt = objekte.map { titelAus(it.optString("titel")) to it.optString("text").trim() }
                val nachschlag = runCatching {
                    jsonAus(auth.streamChat(
                        instructions = anweisung,
                        turns = listOf(ChatTurn("user", buildString {
                            append(planAnfrage).append("\n\n")
                            if (schonGewaehlt.isNotEmpty()) {
                                append("BEREITS NEU GEWÄHLT (ebenfalls nicht wiederholen, nichts Ähnliches):\n")
                                schonGewaehlt.forEach { (t, _) -> append("- ").append(t).append('\n') }
                                append('\n')
                            }
                            append("Es fehlen noch Vorschläge. Liefere jetzt ").append(mengeText(fehlt.mapValues { (_, n) -> if (n > 0) n + 2 else 0 }))
                            append(" weitere, inhaltlich eigenständige Vorschläge.")
                        })),
                        model = modell,
                        reasoningEffort = e.denkstufe,
                    ))?.let(::kandidatenAus).orEmpty()
                }.onFailure { if (it is CancellationException) throw it; Log.w("ZieleAuswertung", "Nachschlag", it) }.getOrDefault(emptyList())
                if (nachschlag.isNotEmpty()) {
                    val dazu = auswahl(aussieben(auth, modell, nachschlag, abgelehnt, vorhanden + schonGewaehlt), fehlt)
                    objekte = objekte + dazu
                }
            }
        }
        // Während des Laufs (mehrere Minuten) kann Frank weitere Vorschläge abgelehnt haben: auch die gelten.
        val spaeter = repo.abgelehnt(zielId).filter { n -> abgelehnt.none { it.id == n.id } }
        if (spaeter.isNotEmpty() && objekte.isNotEmpty()) objekte = aussieben(auth, modell, objekte, spaeter, emptyList())

        schritt(zielId, "Speichere die Vorschläge …")
        // Schlüssel abgelehnter Vorschläge bleiben belegt: Alte Voraussetzungen sollen nicht auf einen neuen Vorschlag zeigen.
        val belegt = (behalten.map { it.schluessel } + abgelehnt.map { it.schluessel }).toMutableSet()
        // Vergibt die KI einen schon belegten Schlüssel neu, wird er umbenannt; Voraussetzungen unter den neuen Vorschlägen
        // meinen dann den neuen Vorschlag und ziehen mit (der erste Vorschlag mit diesem Schlüssel gewinnt).
        val umbenannt = mutableMapOf<String, String>()
        val schluesselJe = objekte.map { o ->
            val gewuenscht = o.optString("schluessel").trim().ifEmpty { "v${belegt.size + 1}" }
            var schluessel = gewuenscht
            while (schluessel in belegt) schluessel += "b"
            belegt += schluessel
            if (schluessel != gewuenscht) umbenannt.putIfAbsent(gewuenscht, schluessel)
            schluessel
        }
        val neu = objekte.mapIndexed { i, o ->
            val titel = titelAus(o.optString("titel"))
            val schluessel = schluesselJe[i]
            val vor = o.optJSONArray("voraussetzungen")?.let { a -> (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() } }.orEmpty()
                .map { umbenannt[it] ?: it }.filter { it != schluessel }
            Vorschlag(
                zielId = zielId,
                schluessel = schluessel,
                titel = titel.take(80),
                text = o.optString("text").trim(),
                begruendung = o.optString("begruendung").trim(),
                kategorie = Kategorie.von(o.optString("kategorie")).name,
                wichtigkeit = o.optInt("wichtigkeit", 3).coerceIn(1, 5),
                aufwand = Aufwand.von(o.optString("aufwand")).name,
                minuten = o.optInt("minuten", 60).coerceIn(5, 100 * 60),
                voraussetzungenJson = Vorschlag.liste(vor),
                phase = o.optInt("phase", 2).coerceIn(1, 3),
                erfolg = o.optString("erfolg").trim(),
                wiederkehrend = o.optBoolean("wiederkehrend", false),
            )
        }

        val ergebnis = when (modus) {
            Modus.VOLL -> {
                if (neu.isEmpty() && freiJe.values.sum() > 0) throw IllegalStateException("Die KI hat keine Aufgaben vorgeschlagen. Bitte noch einmal auswerten.")
                // Unter der Sperre des Übernehmens; ist das Ziel inzwischen gelöscht, nichts schreiben (sonst verwaiste Vorschläge).
                val frisch = Abgleich.gesperrt {
                    repo.ziel(zielId)?.also { repo.vorschlaegeErsetzen(zielId, ohneAbgelehnte(repo, zielId, neu)) }
                } ?: return "Das Ziel gibt es nicht mehr."
                repo.speichere(
                    frisch.copy(
                        bericht = bericht,
                        strategie = plan.optString("strategie").trim().ifEmpty { frisch.strategie },
                        symbol = plan.optString("symbol").trim().takeIf { it.isNotEmpty() && it.length <= 8 && frisch.symbol == "🎯" } ?: frisch.symbol,
                        ausgewertetAm = System.currentTimeMillis(),
                        auswertungModell = modell.label,
                    ),
                )
                "Auswertung fertig ✨"
            }
            Modus.NACHFUELLEN -> {
                var gespeichert = emptyList<Vorschlag>()
                val frisch = Abgleich.gesperrt {
                    repo.ziel(zielId)?.also { gespeichert = ohneAbgelehnte(repo, zielId, neu); if (gespeichert.isNotEmpty()) repo.neueVorschlaege(gespeichert) }
                } ?: return "Das Ziel gibt es nicht mehr."
                // Die Ergänzung bleibt in der Recherche, auch wenn alle Kandidaten durchs Raster fielen.
                repo.speichere(frisch.copy(bericht = bericht))
                val n = gespeichert.count { it.kategorie == Kategorie.NOTWENDIG.name }
                val u = gespeichert.size - n
                if (gespeichert.isEmpty()) "Die Recherche ist ergänzt, aber es kam keine neue Aufgabe durch, die nicht schon da oder abgelehnt ist."
                else "$n notwendige und $u unterstützende Aufgaben ergänzt, die Recherche ist erweitert ✨"
            }
            Modus.RUECKMELDUNG -> {
                val text = plan.optString("antwort").trim().ifEmpty { if (neu.isEmpty()) "Verstanden, ich habe es mir gemerkt." else "Dazu habe ich dir neue Vorschläge ergänzt." }
                Abgleich.gesperrt {
                    repo.ziel(zielId)?.also { frisch ->
                        if (neu.isNotEmpty()) repo.neueVorschlaege(ohneAbgelehnte(repo, zielId, neu))
                        repo.neueRueckmeldung(Rueckmeldung(zielId = zielId, vonFrank = false, text = text))
                        plan.optString("strategie").trim().takeIf { it.isNotEmpty() && it != frisch.strategie }?.let { repo.speichere(frisch.copy(strategie = it)) }
                    }
                } ?: return "Das Ziel gibt es nicht mehr."
                if (neu.isEmpty()) "Antwort ist da" else "Antwort ist da, ${if (neu.size == 1) "ein neuer Vorschlag" else "${neu.size} neue Vorschläge"} ✨"
            }
        }
        // Die Anregungen dieser Rückmeldungen sind jetzt bedacht: Sie heben das Platzlimit nicht mehr auf und werden nicht erneut umgesetzt.
        repo.rueckmeldungenVerarbeitet(offeneRueckmeldungen)
        return ergebnis
    }

    /** Feste Plätze je Ziel und Kategorie: 10 notwendige und 10 unterstützende Vorschläge. */
    const val PLAETZE_JE_KATEGORIE = 10
    /** So viele Kandidaten mehr als freie Plätze erbittet der Plan, damit nach dem Aussieben genug übrig bleiben. */
    private const val PUFFER = 3
    /** Obergrenze für den Recherchetext in einer Anfrage (die ganze Recherche samt Ergänzungen passt in aller Regel hinein). */
    private const val RECHERCHE_MAX = 60_000

    /** Freie Plätze je Kategorie: offene, sichtbare Vorschläge belegen einen Platz, erledigte und ausgeblendete nicht. */
    fun freiePlaetze(vorschlaege: List<Vorschlag>): Map<Kategorie, Int> = Kategorie.entries.associateWith { k ->
        (PLAETZE_JE_KATEGORIE - vorschlaege.count { !it.verworfen && !it.erledigt && Kategorie.von(it.kategorie) == k }).coerceAtLeast(0)
    }

    private fun mengeText(je: Map<Kategorie, Int>): String = Kategorie.entries.joinToString(" und ") { k ->
        "genau ${je[k] ?: 0} ${if (k == Kategorie.NOTWENDIG) "NOTWENDIGE" else "UNTERSTUETZENDE"}"
    }

    private fun kandidatenAus(plan: JSONObject): List<JSONObject> {
        val roh = plan.optJSONArray("vorschlaege") ?: JSONArray()
        return (0 until roh.length()).mapNotNull { roh.optJSONObject(it) }.filter { titelAus(it.optString("titel")).isNotEmpty() }
    }

    /** Je Kategorie die wichtigsten Kandidaten, höchstens so viele, wie Plätze frei sind; Aufgaben zu Franks Rückmeldungen zuerst. */
    private fun auswahl(kandidaten: List<JSONObject>, frei: Map<Kategorie, Int>): List<JSONObject> =
        Kategorie.entries.flatMap { k ->
            kandidaten.filter { Kategorie.von(it.optString("kategorie")) == k }
                .sortedWith(compareByDescending<JSONObject> { it.optBoolean("rueckmeldung", false) }.thenByDescending { it.optInt("wichtigkeit", 3) })
                .take(frei[k] ?: 0)
        }

    /**
     * Angenommene Aufgaben zeigen, was Frank gefällt: die dieses Ziels zuerst, danach (mit persönlichem Kontext) die der übrigen Ziele.
     * Daraus soll der Agent Art, Umfang und Stil ableiten, die Frank mag, ohne sie zu wiederholen.
     */
    private fun vorliebenBlock(zielId: Long, vorschlaege: List<Vorschlag>): String {
        val angenommen = vorschlaege.filter { it.angenommen && !it.verworfen }.sortedWith(compareBy({ it.zielId != zielId }, { -(it.angenommenAm ?: 0L) })).take(40)
        if (angenommen.isEmpty()) return ""
        return buildString {
            append("VON FRANK ANGENOMMENE AUFGABEN (diese Vorschläge gefallen ihm, er will sie behalten und machen; erkenne daraus seine Vorlieben ")
            append("bei Art, Umfang, Aufwand und Stil und richte neue Vorschläge danach aus, ohne diese zu wiederholen):\n")
            angenommen.forEach { v ->
                append("- ").append(v.titel)
                if (v.zielId != zielId) append(" (anderes Ziel)")
                if (v.text.isNotBlank()) append(": ").append(v.text.take(160))
                append('\n')
            }
            append('\n')
        }
    }

    private fun sperrBlock(abgelehnt: List<Abgelehnt>): String = if (abgelehnt.isEmpty()) "" else buildString {
        append("VON FRANK ABGELEHNT (diese Aufgaben will er nicht. Schlage sie nie wieder vor, auch nicht umformuliert, ")
        append("in Teilen, mit anderer Zahl, anderem Ort oder anderer Häufigkeit, und auch nichts sinngleich Ähnliches):\n")
        abgelehnt.forEach { a -> append("- ").append(a.titel); if (a.text.isNotBlank()) append(": ").append(a.text.take(200)); append('\n') }
        append('\n')
    }

    private fun gespraechBlock(gespraech: List<Rueckmeldung>): String = if (gespraech.isEmpty()) "" else buildString {
        append("FRANKS RÜCKMELDUNGEN ZUR AUSWERTUNG (Gespräch, ältestes zuerst; jede Rückmeldung von Frank zählt: seine Anregungen, Wünsche und Einwände; ")
        append("NEU = Anregungen daraus sind noch nicht in Aufgaben umgesetzt):\n")
        // Franks Beiträge ausführlich, die Antworten des Agenten gekürzt (sie erklären, was schon abgelehnt oder umgesetzt ist);
        // wird das Gespräch lang, nur das Neueste, damit die Anfrage begrenzt bleibt.
        val zeilen = gespraech.map {
            if (it.vonFrank) (if (it.verarbeitet) "Frank: " else "Frank (NEU): ") + it.text.trim().take(2_000) else "Agent: " + it.text.trim().take(500)
        }
        var laenge = 0
        val neueste = zeilen.asReversed().takeWhile { laenge += it.length; laenge < 14_000 }.ifEmpty { listOf(zeilen.last()) }
        neueste.asReversed().forEach { append(it).append('\n') }
        append('\n')
    }

    /**
     * Siebt Kandidaten aus, die abgelehnten oder vorhandenen Aufgaben gleichen oder ähneln: erst ohne KI über den
     * bereinigten Titel, dann ein kurzer Prüfpass der KI, der auch Umformulierungen erkennt. Scheitert der Prüfpass,
     * bleibt es beim ersten Sieb, statt den ganzen Lauf zu verlieren.
     */
    private suspend fun aussieben(auth: CodexAuthManager, modell: CodexModel, kandidaten: List<JSONObject>, abgelehnt: List<Abgelehnt>, vorhanden: List<Pair<String, String>>): List<JSONObject> {
        val gesehen = (abgelehnt.map { normTitel(it.titel) } + vorhanden.map { normTitel(it.first) }).toMutableSet()
        val vorgefiltert = kandidaten.filter { gesehen.add(normTitel(titelAus(it.optString("titel")))) }
        if (vorgefiltert.isEmpty() || (abgelehnt.isEmpty() && vorhanden.isEmpty())) return vorgefiltert
        val anfrage = buildString {
            if (abgelehnt.isNotEmpty()) {
                append("ABGELEHNT:\n")
                abgelehnt.forEach { a -> append("- ").append(a.titel); if (a.text.isNotBlank()) append(": ").append(a.text.take(200)); append('\n') }
                append('\n')
            }
            if (vorhanden.isNotEmpty()) {
                append("VORHANDEN:\n")
                vorhanden.forEach { (titel, text) -> append("- ").append(titel); if (text.isNotBlank()) append(": ").append(text.take(160)); append('\n') }
                append('\n')
            }
            append("KANDIDATEN:\n")
            vorgefiltert.forEachIndexed { i, o -> append(i + 1).append(". ").append(titelAus(o.optString("titel"))).append(": ").append(o.optString("text").trim().take(240)).append('\n') }
        }
        val streichen = runCatching {
            val o = jsonAus(auth.streamChat(
                instructions = RICHTER_ANWEISUNG,
                turns = listOf(ChatTurn("user", anfrage)),
                model = modell,
                reasoningEffort = ReasoningEffort.LOW,
            )) ?: return@runCatching emptySet()
            val a = o.optJSONArray("streichen") ?: JSONArray()
            (0 until a.length()).map { a.optInt(it, -1) - 1 }.toSet()
        }.onFailure { if (it is CancellationException) throw it; Log.w("ZieleAuswertung", "Prüfpass", it) }.getOrDefault(emptySet())
        return vorgefiltert.filterIndexed { i, _ -> i !in streichen }
    }

    private val NICHT_WORT = Regex("[^\\p{L}\\p{N}]+")
    private fun normTitel(t: String) = t.lowercase(Locale.GERMAN).replace(NICHT_WORT, " ").trim()

    /** Letztes Sieb unter der Sperre: nichts speichern, dessen Titel inzwischen abgelehnt ist. */
    private suspend fun ohneAbgelehnte(repo: ZieleRepository, zielId: Long, neu: List<Vorschlag>): List<Vorschlag> {
        val gesperrt = repo.abgelehnt(zielId).map { normTitel(it.titel) }.toSet()
        return neu.filter { normTitel(it.titel) !in gesperrt }
    }

    /**
     * Hängt eine Ergänzungsrecherche an den bisherigen Bericht: beide Texte hintereinander, darunter ein einziger
     * Abschnitt QUELLEN mit den Quellen beider (doppelte nur einmal), damit [Quellen.teilen] beides sauber trennt.
     */
    internal fun berichtAnfuegen(alt: String, ergaenzung: String): String {
        val a = Quellen.teilen(alt)
        val b = Quellen.teilen(ergaenzung)
        val datum = LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN))
        val kopf = "ERGÄNZUNG VOM ${datum.uppercase(Locale.GERMAN)}"
        val text = if (a.text.isBlank()) b.text else a.text + "\n\n" + kopf + "\n" + b.text
        val quellen = (a.quellen + b.quellen).distinctBy { it.url.ifEmpty { "ohne:" + it.titel.lowercase(Locale.GERMAN) } }
        if (quellen.isEmpty()) return text
        return text + "\n\nQUELLEN\n" + quellen.joinToString("\n") { if (it.url.isEmpty()) it.titel else "${it.titel} – ${it.url}" }
    }

    fun zielBeschreibung(z: Ziel): String = buildString {
        append("ZIEL: ").append(z.titel).append('\n')
        if (z.beschreibung.isNotBlank()) append("GENAUER: ").append(z.beschreibung.trim()).append('\n')
        if (z.warum.isNotBlank()) append("WARUM ES FRANK WICHTIG IST: ").append(z.warum.trim()).append('\n')
        z.frist?.let { append("ZIELDATUM: ").append(LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN))).append('\n') }
    }

    /** Sucht das JSON-Objekt in der Antwort, auch wenn das Modell Codeblöcke oder Text drumherum schreibt. */
    internal fun jsonAus(antwort: String): JSONObject? {
        val ohneZaun = antwort.replace(Regex("```(?:json)?"), "")
        val anfang = ohneZaun.indexOf('{')
        val ende = ohneZaun.lastIndexOf('}')
        if (anfang < 0 || ende <= anfang) return null
        return runCatching { JSONObject(ohneZaun.substring(anfang, ende + 1)) }.getOrNull()
    }

    /**
     * Säubert einen KI-Titel: Anführungszeichen und einen einzelnen Schlusspunkt weg (Auslassungspunkte bleiben).
     * Sicherheitsnetz: „Kläre du deinen …“ → „Kläre deinen …“ — nur wenn das erste Wort kein Bindewort ist
     * („Wenn du …“, „Bevor du …“ brauchen das „du“).
     */
    internal fun titelAus(roh: String): String {
        var t = roh.trim().trim('„', '“', '"').trim()
        DU_NACH_VERB.find(t)?.let { m -> if (m.groupValues[1].lowercase(Locale.GERMAN) !in BINDEWOERTER) t = m.groupValues[1] + " " + t.substring(m.range.last + 1) }
        if (t.endsWith('.') && !t.endsWith("..")) t = t.dropLast(1)
        return t.trim()
    }

    private val DU_NACH_VERB = Regex("^(\\p{L}+)\\s+du\\s+")
    private val BINDEWOERTER = setOf(
        "wenn", "bevor", "sobald", "falls", "ob", "als", "damit", "während", "nachdem", "weil", "dass", "wie", "was", "wo",
        "bis", "solange", "sodass", "obwohl", "indem", "sofern", "seit", "seitdem", "wann", "warum", "womit", "wodurch",
    )

    private const val RECHERCHE_ANWEISUNG =
        "Du bist ein erstklassiger Coach und Rechercheur. Frank hat ein persönliches Ziel. Finde gründlich heraus, wie man genau dieses Ziel " +
            "am besten erreicht, und schneide alles auf Frank zu (Kontext unten: sein Alltag, seine Arbeit im Schichtdienst, seine Gesundheit, " +
            "seine anderen Ziele und Gewohnheiten). Schreibe auf Deutsch, du sprichst Frank mit „du“ an.\n\n" +
            "Gliedere den Bericht so (Überschriften als eigene Zeile, keine Markdown-Zeichen wie # oder **):\n" +
            "KERN: zwei bis vier Sätze, worauf es bei diesem Ziel wirklich ankommt.\n" +
            "WAS NACHWEISLICH WIRKT: die wirksamsten Hebel, möglichst belegt (Studien, Fachleute, bewährte Methoden), je mit einem Satz warum.\n" +
            "DER WEG: sinnvolle Etappen und Meilensteine bis zum Ziel, mit realistischen Zeiträumen.\n" +
            "FÜR DICH ZUGESCHNITTEN: was in Franks Lage besonders zählt, was ihm leichtfällt, was ihn bremsen könnte.\n" +
            "STOLPERSTEINE: typische Fehler und wie man sie vermeidet.\n" +
            "WORAN DU FORTSCHRITT MERKST: messbare Zeichen.\n\n" +
            "Erfinde nichts. Trenne Belegtes von Erfahrungswissen. Bleib konkret und praktisch; keine Floskeln."

    private const val RECHERCHE_MIT_WEB =
        "\n\nRecherchiere mit der Websuche, gern mehrfach und aus verschiedenen Blickwinkeln, bevor du schreibst. " +
            "Im Text selbst stehen keine Quellenangaben: keine Links, keine Webadressen, keine Seitennamen in Klammern. " +
            "Nenne die wichtigsten Quellen ausschließlich ganz am Ende unter der Zeile QUELLEN, je Zeile „Kurzer Titel – Adresse“, höchstens acht."

    private const val RECHERCHE_OHNE_WEB = "\n\nStütze dich auf dein Fachwissen; eine Websuche steht dafür nicht zur Verfügung."

    private const val ERGAENZUNG_ANWEISUNG =
        "Du bist ein erstklassiger Coach und Rechercheur. Zu Franks persönlichem Ziel gibt es schon eine Recherche (unten). " +
            "Deine Aufgabe ist eine Ergänzung: Stelle dir weitere Fragen, die die bisherige Recherche nicht beantwortet, und betrachte das Ziel " +
            "aus anderen Richtungen. Wie kann man es sonst noch erreichen? Welche anderen Methoden, Hilfsmittel, Angebote, Menschen oder " +
            "Gewohnheiten führen ebenfalls dorthin? Was fehlt bisher? Beziehe das Vorhandene ein und baue darauf auf, wiederhole es aber nicht. " +
            "Franks Rückmeldungen zählen: Gehe seinen sinnvollen Anregungen gezielt nach; Richtungen, die er abgelehnt hat, verfolgst du nicht weiter. " +
            "Schneide alles auf Frank zu. Schreibe auf Deutsch, du sprichst Frank mit „du“ an.\n\n" +
            "Gliedere die Ergänzung so (Überschriften als eigene Zeile, keine Markdown-Zeichen wie # oder **):\n" +
            "WEITERE FRAGEN: die Fragen, denen du nachgegangen bist, je mit der kurzen Antwort.\n" +
            "ANDERE WEGE ZUM ZIEL: alternative Ansätze, je mit einem Satz, warum und für wen sie wirken.\n" +
            "NEUE ERKENNTNISSE: was die bisherige Recherche ergänzt oder korrigiert.\n" +
            "FÜR DICH ZUGESCHNITTEN: was davon in Franks Lage besonders passt.\n\n" +
            "Erfinde nichts. Trenne Belegtes von Erfahrungswissen. Bleib konkret und praktisch; keine Floskeln."

    private const val RUECKMELDUNG_ZUSATZ =
        "\n\nSONDERFALL GESPRÄCH: Frank hat auf die Auswertung oder die Aufgaben reagiert (unter FRANKS RÜCKMELDUNGEN; es zählen seine als NEU markierten Beiträge, die älteren sind Hintergrund). " +
            "Nimm seine Rückmeldung ernst. Hat er recht, gib ihm recht und passe den Weg an; hast du gute Gründe dagegen, erkläre sie kurz und freundlich. " +
            "Gehe jede Anregung und jeden neuen Punkt seiner als NEU markierten Rückmeldungen einzeln durch: Ist er für das Ziel sinnvoll und gibt es dafür noch keine " +
            "vorhandene Aufgabe, mache daraus eine oder mehrere konkrete, erledigbare Aufgaben (NOTWENDIG oder UNTERSTUETZEND, mit ehrlicher Wichtigkeit) " +
            "und sag in \"begruendung\" kurz, dass sie seine Rückmeldung aufgreift. Spricht er einen neuen Punkt an, gehört dazu in aller Regel mindestens eine Aufgabe. " +
            "Nicht sinnvolle Anregungen (bringen das Ziel nicht voran, schaden eher, gleichen Abgelehntem) setzt du nicht um und erklärst in der Antwort kurz, warum. " +
            "Ist die Rückmeldung nur Zustimmung, Dank oder eine Frage ohne neuen Punkt, bleibt die Liste leer. " +
            "Gefällt ihm eine vorhandene Aufgabe nicht, schlage, wenn es passt, eine bessere Alternative vor und sag ihm, dass er die alte mit Ablehnen herausnehmen kann. " +
            "Ergänze das JSON um das Feld \"antwort\": zwei bis fünf Sätze an Frank in der Du-Form, gut vorlesbar, ohne Aufzählungszeichen; nenne darin, welche neuen Aufgaben du vorschlägst. " +
            "\"strategie\" füllst du nur, wenn sich der beste Weg durch die Rückmeldung ändert, sonst leer; \"symbol\" bleibt leer."

    private const val RUECKMELDUNG_VORRANG =
        "Vorrang haben Franks als NEU markierte Rückmeldungen: Ihre Anregungen sind noch nicht umgesetzt. Ist eine davon für das Ziel sinnvoll " +
            "und greift sie noch keine vorhandene Aufgabe auf, mache daraus einen der Vorschläge, setze bei ihm zusätzlich \"rueckmeldung\": true " +
            "und sag in \"begruendung\" kurz, dass er seine Rückmeldung aufgreift. Nicht sinnvolle Anregungen und was der Agent im Gespräch schon " +
            "begründet abgelehnt hat, setzt du nicht um. "

    private const val RICHTER_ANWEISUNG =
        "Du prüfst neue Aufgabenvorschläge (KANDIDATEN) gegen zwei Listen. Streiche jeden Kandidaten, der einer ABGELEHNTEN Aufgabe gleicht oder ähnelt: " +
            "dieselbe Handlung oder Absicht, nur anders formuliert, ein Teil davon, eine Variante mit anderer Zahl, anderem Ort, anderer Häufigkeit " +
            "oder anderem Anbieter. Streiche auch jeden Kandidaten, der eine VORHANDENE Aufgabe im Kern wiederholt. " +
            "Behalte alles, was wirklich etwas anderes ist. Im Zweifel bei ABGELEHNT: streichen. " +
            "Antworte NUR mit JSON: {\"streichen\": [Nummern der zu streichenden Kandidaten]}"

    private const val PLAN_ANWEISUNG =
        "Du bist Franks Aufgaben-Agent. Aus dem Recherche-Bericht und dem Kontext über Frank leitest du konkrete Aufgaben ab, " +
            "die ihn seinem Ziel nachweislich näherbringen. Jede Aufgabe muss in jedem Fall zur Zielerreichung beitragen; nichts Allgemeines, nichts Beliebiges. " +
            "Zuschnitt auf Frank: berücksichtige seinen Kontext (Dienstplan, Gesundheit, Vorlieben, vorhandene Ziele), damit die Aufgaben in sein Leben passen.\n\n" +
            "Zwei Gruppen:\n" +
            "- NOTWENDIG: wirklich wichtig, ohne diese Aufgaben wird das Ziel nicht erreicht.\n" +
            "- UNTERSTUETZEND: nicht unbedingt nötig, aber hilfreich (leichter, schneller, sicherer, angenehmer).\n\n" +
            "SPRACHE (sehr wichtig, Frank liest und hört jeden Vorschlag): einwandfreies, natürliches Deutsch, wie es ein guter Muttersprachler schreibt, " +
            "grammatisch korrekt und auf Anhieb verständlich, ohne Fachjargon und ohne Wortneuschöpfungen.\n" +
            "- Titel: eine klare, erledigbare Handlung als Aufforderung in der Du-Form, aber OHNE das Wort „du“ (Imperativ: „Kläre“, „Übe“, „Lies“, „Melde dich“). " +
            "Etwa drei bis acht Wörter, höchstens 60 Zeichen, ohne Punkt am Ende; lieber ein Wort mehr als unverständlich. " +
            "Der Titel muss für sich allein verständlich sein: Was genau ist zu tun?\n" +
            "  Gut (nur Stilbeispiele, nicht übernehmen): „Melde dich beim Lauftreff Köpenick an“, „Lege jeden Monat 100 Euro zurück“, " +
            "„Lass beim Hausarzt dein Blutbild prüfen“, „Schreib abends drei schöne Momente auf“.\n" +
            "  Falsch: „Kläre du deinen Trainingsstand“ (überflüssiges „du“, unklar), „Übe du zehn Minuten ruhig atmen“ (falsche Wortstellung), " +
            "„Ziehe du etwas Wochenbilanzen“ (kein Deutsch), „Kläre den finanziellen Bedarf“ (Amtsdeutsch, unklar, was zu tun ist).\n" +
            "- text, begruendung und erfolg: ganze, vollständige Sätze in der Du-Form, gut vorlesbar.\n" +
            "Prüfe jeden Titel und Satz vor der Ausgabe: Würde ein Deutschlehrer ihn so stehen lassen? Wenn nicht, formuliere ihn neu.\n\n" +
            "Wiederkehrende Gewohnheiten (z. B. „Laufe dreimal pro Woche 30 Minuten“) sind erlaubt und als wiederkehrend markiert.\n\n" +
            "Antworte NUR mit einem JSON-Objekt, ohne Text davor oder danach, genau in dieser Form:\n" +
            "{\n" +
            "  \"symbol\": \"ein passendes Emoji für das Ziel\",\n" +
            "  \"strategie\": \"ein bis drei Sätze: der beste Weg zum Ziel, für Frank zugeschnitten, gut vorlesbar\",\n" +
            "  \"vorschlaege\": [\n" +
            "    {\n" +
            "      \"schluessel\": \"v1\",\n" +
            "      \"titel\": \"kurze, verständliche Aufforderung ohne das Wort du, passend zu diesem Ziel\",\n" +
            "      \"text\": \"ein bis drei Sätze: was genau zu tun ist, konkret\",\n" +
            "      \"begruendung\": \"ein Satz: warum das auf das Ziel einzahlt\",\n" +
            "      \"kategorie\": \"NOTWENDIG oder UNTERSTUETZEND\",\n" +
            "      \"wichtigkeit\": 1 bis 5 (5 = entscheidend für das Ziel),\n" +
            "      \"aufwand\": \"KLEIN, MITTEL oder GROSS\",\n" +
            "      \"minuten\": geschätzte Minuten für die Aufgabe (bei wiederkehrenden: pro Durchgang),\n" +
            "      \"phase\": 1 (sofort), 2 (bald) oder 3 (später),\n" +
            "      \"voraussetzungen\": [\"Schlüssel der Aufgaben, die vorher erledigt sein müssen\"],\n" +
            "      \"erfolg\": \"woran Frank merkt, dass die Aufgabe erledigt ist\",\n" +
            "      \"wiederkehrend\": true oder false\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Schlüssel fortlaufend v1, v2, … (außer sie sind unter SCHON VORHANDEN belegt). Voraussetzungen ohne Kreise. " +
            "Halte dich genau an die verlangte Anzahl je Gruppe (Feld kategorie); ist für eine Gruppe 0 verlangt, lieferst du dafür nichts. " +
            "Jeder Vorschlag ist inhaltlich eigenständig, keine zwei meinen dasselbe. Sortiere nichts, das macht die App."
}
