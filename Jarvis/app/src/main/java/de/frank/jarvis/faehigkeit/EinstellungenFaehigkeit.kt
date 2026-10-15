package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.tts.TtsCatalog
import de.frank.jarvis.tts.TtsProvider
import java.util.Locale

/**
 * Jarvis stellt sich selbst ein: „Schalte die Update-Ansagen aus“, „Lies schneller vor“, „Was hast du beim Vorlesen
 * für Einstellungen?“. Grundlage ist der [EinstellungsKatalog]: Jeder neue Schalter in den Einstellungen gehört dort
 * hinein, dann kann Jarvis ihn beschreiben, lesen und ändern.
 */
class EinstellungenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "einstellungen"
    override val name = "Einstellungen"
    override val beschreibung = "Die eigenen Einstellungen von Jarvis beschreiben, lesen und ändern."
    override val hinweise =
        "Fragt Frank, was Jarvis für Einstellungen hat (allgemein oder zu einem Bereich wie Vorlesen), nimm einstellungen mit aktion lesen " +
            "und beschreibe die Schalter verständlich mit ihrem Stand, ohne die Kennungen vorzulesen. Will er etwas ein-, aus- oder umstellen, " +
            "nimm aktion aendern mit der Kennung aus der Liste; bist du unsicher, welche gemeint ist, lies erst. Bestätige danach kurz den neuen Stand."

    override fun stoerung(): String? = null

    private val katalog by lazy { EinstellungsKatalog(context) }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "einstellungen",
            titel = "Jarvis-Einstellungen",
            beschreibung = "Jarvis: beschreibt, liest und ändert die Einstellungen der Jarvis-App (Vorlesen, Update-Ansagen, Spracheingabe, Hey Jarvis, " +
                "Losfahr-Meldung, Tagesauswertung, Timer, Modell, Darstellung, System). aktion lesen ohne bereich = alle Bereiche mit ihren Schaltern, Stand und möglichen Werten; " +
                "mit bereich nur diesen. aktion aendern setzt einen Schalter: kennung aus der Liste, wert wie dort angegeben (an/aus, Zahl oder eine der Auswahlen). " +
                "Mehrere Änderungen auf einmal über aenderungen. Beispiele: „Schalte die Update-Ansagen aus“ → aendern, kennung ansage_vorlesen, wert aus. " +
                "„Was hast du beim Vorlesen für Einstellungen?“ → lesen, bereich Vorlesen. „Lies schneller vor“ → aendern, kennung vorlese_tempo, wert Zügig. " +
                "Zugangsschlüssel und Passwörter gehen hier bewusst nicht.",
            schema = schema(
                "aktion" to text("lesen oder aendern.", listOf("lesen", "aendern")),
                "bereich" to text("Beim Lesen: nur diesen Bereich, z. B. Vorlesen, Update-Ansagen, Timer. Weglassen = alle."),
                "kennung" to text("Beim Ändern: die Kennung des Schalters aus der Liste, z. B. ansage_vorlesen."),
                "wert" to text("Beim Ändern: der neue Wert, z. B. an, aus, 60 oder Zügig."),
                "aenderungen" to textListe("Mehrere Änderungen auf einmal, je Eintrag „kennung=wert“, z. B. „ansage_lautstaerke=40“."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a ->
            when (a.optString("aktion")) {
                "lesen" -> Ergebnis(katalog.beschreibe(a.optString("bereich").trim()))
                "aendern" -> {
                    val paare = buildList {
                        if (a.gesetzt("kennung")) add(a.optString("kennung").trim() to a.optString("wert").trim())
                        a.optJSONArray("aenderungen")?.let { liste ->
                            for (i in 0 until liste.length()) liste.optString(i).split('=', limit = 2).takeIf { it.size == 2 }?.let { add(it[0].trim() to it[1].trim()) }
                        }
                    }
                    if (paare.isEmpty()) Ergebnis("Was soll ich ändern? Nenne kennung und wert.", fehler = true)
                    else {
                        val antworten = paare.map { (k, w) -> katalog.setze(k, w) }
                        Ergebnis(antworten.joinToString("\n") { it.second }, fehler = antworten.all { !it.first })
                    }
                }
                else -> Ergebnis("aktion muss lesen oder aendern sein.", fehler = true)
            }
        },
    )
}

/**
 * Alle Einstellungen, die Jarvis selbst beschreiben und umstellen darf. NEUER SCHALTER IN DER APP → HIER EINTRAGEN,
 * mit verständlicher Beschreibung und denselben Folgeaktionen wie in der Oberfläche (Dienst abgleichen, Zeitplan neu stellen …).
 * Nicht hinein gehören Zugangsschlüssel, Passwörter und Adressen von Servern.
 */
class EinstellungsKatalog(context: Context) {
    private val app = context.applicationContext
    private val e get() = Einstellungen.get(app)
    private val s get() = SecureSettings.get(app)

    class Eintrag(
        val kennung: String,
        val bereich: String,
        val titel: String,
        val beschreibung: String,
        /** Mögliche Werte in Worten, z. B. „an oder aus“. */
        val werte: String,
        val lesen: () -> String,
        /** Wirft [IllegalArgumentException] mit verständlichem Text, wenn der Wert nicht passt. */
        val setzen: (String) -> Unit,
    )

    private fun schalter(kennung: String, bereich: String, titel: String, beschreibung: String, lies: () -> Boolean, setze: (Boolean) -> Unit) =
        Eintrag(kennung, bereich, titel, beschreibung, "an oder aus", { if (lies()) "an" else "aus" }) { setze(jaNein(it)) }

    private fun zahl(kennung: String, bereich: String, titel: String, beschreibung: String, min: Int, max: Int, einheit: String, lies: () -> Int, setze: (Int) -> Unit) =
        Eintrag(kennung, bereich, titel, beschreibung, "$min bis $max $einheit", { "${lies()} $einheit" }) { w ->
            val zahl = Regex("-?\\d+").find(w)?.value?.toIntOrNull() ?: when (norm(w)) { "maximal", "max", "voll" -> max; "minimal", "min" -> min; else -> null }
                ?: throw IllegalArgumentException("„$w“ ist keine Zahl. Erlaubt: $min bis $max $einheit.")
            if (zahl !in min..max) throw IllegalArgumentException("$zahl liegt außerhalb von $min bis $max $einheit.")
            setze(zahl)
        }

    /** Uhrzeit HH:MM; versteht auch „17.10“, „17:10 Uhr“ oder „5“. */
    private fun uhrzeit(kennung: String, bereich: String, titel: String, beschreibung: String, lies: () -> String, setze: (String) -> Unit) =
        Eintrag(kennung, bereich, titel, beschreibung, "eine Uhrzeit HH:MM", { lies() + " Uhr" }) { w ->
            val minuten = Mitdenken.uhrzeitMinuten(w) ?: throw IllegalArgumentException("„$w“ ist keine Uhrzeit. Bitte als HH:MM, z. B. 17:10.")
            setze(String.format(Locale.GERMAN, "%02d:%02d", minuten / 60, minuten % 60))
        }

    /** [optionen]: gespeicherter Wert zu Anzeige. Erkannt wird beides, ohne Groß- und Kleinschreibung. */
    private fun auswahl(kennung: String, bereich: String, titel: String, beschreibung: String, optionen: () -> List<Pair<String, String>>, lies: () -> String, setze: (String) -> Unit) =
        Eintrag(kennung, bereich, titel, beschreibung, "", { val w = lies(); optionen().firstOrNull { it.first == w }?.second ?: w }) { w ->
            val o = optionen()
            val treffer = o.firstOrNull { norm(it.first) == norm(w) || norm(it.second) == norm(w) }
                ?: o.firstOrNull { norm(it.second).startsWith(norm(w)) || norm(it.second).substringBefore(" (") == norm(w) }
                ?: throw IllegalArgumentException("„$w“ gibt es hier nicht. Möglich: ${o.joinToString(", ") { it.second }}.")
            setze(treffer.first)
        }.let { roh -> Eintrag(roh.kennung, roh.bereich, roh.titel, roh.beschreibung, "eine von: " + optionen().joinToString(", ") { it.second }, roh.lesen, roh.setzen) }

    private fun norm(t: String) = t.trim().lowercase(Locale.GERMAN)

    private fun jaNein(w: String): Boolean = when (norm(w)) {
        "an", "ein", "einschalten", "anschalten", "true", "ja", "1", "on", "aktiv", "aktivieren" -> true
        "aus", "ausschalten", "abschalten", "false", "nein", "0", "off", "inaktiv", "deaktivieren" -> false
        else -> throw IllegalArgumentException("„$w“ verstehe ich nicht, bitte an oder aus.")
    }

    // ---- Stimmen ----
    private fun alleStimmen(): List<Pair<String, String>> =
        TtsCatalog.edgeVoices.map { it.id to "${it.name} (${TtsProvider.EDGE.kurz})" } +
            TtsCatalog.googleVoices.map { it.id to "${it.name} (${TtsProvider.GOOGLE_CLOUD.kurz})" } +
            TtsCatalog.qwenVoices.map { it.id to "${it.name} (${TtsProvider.QWEN.kurz})" }

    private fun jarvisStimme(): String = when (TtsCatalog.providers.firstOrNull { it.id == s.ttsProvider }) {
        TtsProvider.GOOGLE_CLOUD -> s.googleTtsVoice
        TtsProvider.QWEN -> s.qwenStandardVoice
        TtsProvider.QWEN_CLONE -> "eigene"
        else -> s.edgeTtsVoice
    }

    private fun setzeJarvisStimme(id: String) {
        when (TtsCatalog.anbieterZu(id)) {
            TtsProvider.EDGE -> { s.edgeTtsVoice = id; s.ttsProvider = TtsProvider.EDGE.id }
            TtsProvider.GOOGLE_CLOUD -> { s.googleTtsVoice = id; s.ttsProvider = TtsProvider.GOOGLE_CLOUD.id }
            TtsProvider.QWEN -> { s.qwenStandardVoice = id; s.ttsProvider = TtsProvider.QWEN.id }
            else -> throw IllegalArgumentException("Diese Stimme kenne ich nicht.")
        }
    }

    private val tempi = listOf("0.9" to "Ruhig", "1.0" to "Normal", "1.1" to "Zügig", "1.2" to "Schnell")
    private fun tempoText(t: Float) = tempi.minByOrNull { kotlin.math.abs(it.first.toFloat() - t) }!!.first

    /** Nach einer Änderung an der Losfahr-Meldung neu planen, wie in der Oberfläche. */
    private fun abfahrtNeu() { e.abfahrtZustand = ""; de.frank.jarvis.fahrt.Abfahrt.stelle(app) }
    private fun zeitplanNeu() = de.frank.jarvis.auswertung.Zeitplan.stelle(app)

    val eintraege: List<Eintrag> by lazy {
        listOf(
            // ---- Vorlesen ----
            auswahl("vorlese_anbieter", "Vorlesen", "Anbieter der Stimme", "Welcher Dienst Jarvis' Stimme erzeugt.",
                { TtsCatalog.providers.map { it.id to it.label } }, { s.ttsProvider }) { s.ttsProvider = it },
            auswahl("vorlese_stimme", "Vorlesen", "Stimme", "Jarvis' Vorlese-Stimme; stellt den passenden Anbieter mit um.",
                { alleStimmen() + ("eigene" to "Meine Stimme (geklont)") }, { jarvisStimme() }) { if (it == "eigene") s.ttsProvider = TtsProvider.QWEN_CLONE.id else setzeJarvisStimme(it) },
            auswahl("vorlese_tempo", "Vorlesen", "Sprechtempo", "Wie schnell Jarvis vorliest.", { tempi }, { tempoText(s.ttsSpeechRate) }) { s.ttsSpeechRate = it.toFloat() },
            schalter("immer_deutsch", "Vorlesen", "Immer deutsch aussprechen", "Englisch aussehende Wörter trotzdem deutsch aussprechen.", { s.immerDeutschVorlesen }) { s.immerDeutschVorlesen = it },
            schalter("antworten_vorlesen", "Vorlesen", "Antworten auf Gesprochenes vorlesen", "Liest die Antwort vor, wenn die Frage gesprochen wurde.", { e.antwortenVorlesen }) { e.antwortenVorlesen = it },
            schalter("ergebnisse_vorlesen", "Vorlesen", "Fertige Agenten-Ergebnisse vorlesen", "Liest Ergebnisse von Agenten von selbst vor, sobald sie fertig sind.", { e.ergebnisseVorlesen }) { e.ergebnisseVorlesen = it },

            // ---- Update-Ansagen ----
            schalter("ansage_vorlesen", "Update-Ansagen", "Update-Ansagen vorlesen", "Sagt an, wenn in UpdateStation ein neues Update bereitliegt. Aus = nur die stille Benachrichtigung.", { e.ansageVorlesen }) { e.ansageVorlesen = it },
            auswahl("ansage_stimme", "Update-Ansagen", "Stimme der Ansagen", "Eigene Stimme nur für die Update-Ansagen.",
                { listOf("" to "Wie Jarvis") + alleStimmen() }, { e.ansageStimme }) { e.ansageStimme = it },
            auswahl("ansage_tempo", "Update-Ansagen", "Tempo der Ansagen", "Sprechtempo nur für die Update-Ansagen.",
                { listOf("0" to "Wie Jarvis") + tempi }, { if (e.ansageTempo <= 0f) "0" else tempoText(e.ansageTempo) }) { e.ansageTempo = it.toFloat() },
            zahl("ansage_lautstaerke", "Update-Ansagen", "Lautstärke der Ansagen", "Lautstärke nur für die Ansage, danach gilt wieder die alte.", 10, 100, "Prozent", { e.ansageLautstaerke }) { e.ansageLautstaerke = it },

            // ---- Spracheingabe ----
            schalter("filter_stille", "Spracheingabe", "Filter: Stille vorab", "Wirft Aufnahmen ohne Sprache weg, bevor sie umgeschrieben werden.", { s.filterStilleVorabAn }) { s.filterStilleVorabAn = it },
            schalter("filter_segmente", "Spracheingabe", "Filter: Segment-Werte", "Verwirft Textstücke, die das Erkennungsmodell selbst für unsicher hält.", { s.filterSegmentmetrikenAn }) { s.filterSegmentmetrikenAn = it },
            schalter("filter_zeitstempel", "Spracheingabe", "Filter: Zeitstempel", "Verwirft Text, der außerhalb der gesprochenen Zeit liegt.", { s.filterZeitstempelAn }) { s.filterZeitstempelAn = it },
            schalter("filter_floskeln", "Spracheingabe", "Filter: Floskeln", "Entfernt typische erfundene Floskeln wie „Untertitel von …“.", { s.filterFloskelnAn }) { s.filterFloskelnAn = it },

            // ---- Hey Jarvis ----
            schalter("weckwort_an", "Hey Jarvis", "Auf „Hey Jarvis“ lauschen", "Dauerhaft auf das Weckwort hören, auch bei gesperrtem Handy.", { e.weckwortAn }) {
                e.weckwortAn = it; de.frank.jarvis.weckwort.WeckwortDienst.abgleichen(app)
            },
            auswahl("weckwort_empfindlichkeit", "Hey Jarvis", "Empfindlichkeit", "Wie leicht das Weckwort anspringt.",
                { listOf("niedrig" to "Niedrig", "normal" to "Normal", "hoch" to "Hoch") }, { e.weckwortEmpfindlichkeit }) {
                e.weckwortEmpfindlichkeit = it
                de.frank.jarvis.weckwort.Weckwort.schwelle = e.weckwortSchwelle
                if (e.weckwortArt == "phrase") de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(app)
            },
            auswahl("weckwort_art", "Hey Jarvis", "Weckwort", "„Hey Jarvis“ oder die eigene Phrase „${e.weckwortPhrase}“.",
                { listOf("jarvis" to "Hey Jarvis", "phrase" to "Eigene Phrase") }, { e.weckwortArt }) {
                if (e.weckwortArt != it) { e.weckwortArt = it; de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(app) }
            },

            // ---- Losfahr-Meldung ----
            schalter("abfahrt_an", "Losfahr-Meldung", "Losfahr-Meldung", "Meldet an Arbeitstagen rechtzeitig vor der nötigen Abfahrt.", { e.abfahrtAn }) { e.abfahrtAn = it; abfahrtNeu() },
            schalter("abfahrt_vorlesen", "Losfahr-Meldung", "Losfahr-Meldung vorlesen", "Liest die Meldung zusätzlich vor.", { e.abfahrtVorlesen }) { e.abfahrtVorlesen = it },
            zahl("abfahrt_lautstaerke", "Losfahr-Meldung", "Lautstärke der Losfahr-Meldung", "Lautstärke der vorgelesenen Meldung.", 10, 100, "Prozent", { e.abfahrtLautstaerke }) { e.abfahrtLautstaerke = it },
            zahl("abfahrt_vorlauf", "Losfahr-Meldung", "Vorlauf", "So viele Minuten vor der nötigen Abfahrt kommt die Meldung.", 0, 60, "Minuten", { e.abfahrtVorlauf }) { e.abfahrtVorlauf = it; abfahrtNeu() },
            uhrzeit("ankunft_nacht", "Losfahr-Meldung", "Ankunft vor dem Nachtdienst", "Wann Frank vor dem Nachtdienst auf Arbeit sein will; danach richtet sich die Losfahr-Meldung.", { e.ankunftNacht }) { e.ankunftNacht = it; abfahrtNeu() },
            uhrzeit("ankunft_tag", "Losfahr-Meldung", "Ankunft vor dem Tagdienst", "Wann Frank vor dem Tagdienst auf Arbeit sein will; danach richtet sich die Losfahr-Meldung.", { e.ankunftTag }) { e.ankunftTag = it; abfahrtNeu() },

            // ---- Tagesauswertung ----
            schalter("auswertung_an", "Tagesauswertung", "Tagesauswertung", "Erstellt regelmäßig die Tagesauswertung.", { e.auswertungAn }) { e.auswertungAn = it; zeitplanNeu() },
            zahl("auswertung_abstand", "Tagesauswertung", "Abstand", "Alle wie viele Stunden die Auswertung läuft.", 1, 12, "Stunden", { e.auswertungAbstand }) { e.auswertungAbstand = it; zeitplanNeu() },
            schalter("auswertung_schlafpause", "Tagesauswertung", "Schlafpause", "Ruht, solange Frank laut Dienstplan schläft.", { e.auswertungSchlafpause }) { e.auswertungSchlafpause = it; zeitplanNeu() },
            schalter("auswertung_abgleich", "Tagesauswertung", "Biodaten vorher abgleichen", "Holt vor jedem Lauf frische Daten von Whoop, Oura und Waage.", { e.auswertungAbgleich }) { e.auswertungAbgleich = it },
            schalter("auswertung_deutung", "Tagesauswertung", "Deutung schreiben", "Das Modell schreibt eine Deutung; aus = nur Daten abgleichen.", { e.auswertungDeutung }) { e.auswertungDeutung = it },
            schalter("auswertung_lernen", "Tagesauswertung", "Aus dem Tagebuch lernen", "Verdichtet alte Monate und notiert neue Tatsachen über Frank.", { e.auswertungLernen }) { e.auswertungLernen = it },

            // ---- Timer ----
            schalter("timer_wecker", "Timer", "Timer klingelt wie ein Wecker", "An = klingelt bis zum Ausschalten, aus = kurze Meldung mit Ton.", { e.timerWecker }) { e.timerWecker = it },
            schalter("timer_vibration", "Timer", "Vibration", "Vibriert beim Klingeln.", { e.timerVibration }) { e.timerVibration = it },
            zahl("timer_lautstaerke", "Timer", "Lautstärke des Wecktons", "In Prozent der Wecker-Lautstärke des Handys.", 10, 100, "Prozent", { e.timerLautstaerke }) { e.timerLautstaerke = it },
            zahl("timer_schlummern", "Timer", "Schlummern", "Minuten für „Schlummern“.", 1, 30, "Minuten", { e.timerSchlummern }) { e.timerSchlummern = it },
            zahl("timer_klingel_max", "Timer", "Höchstens klingeln", "Danach bleibt eine stille Meldung; 0 = bis zum Ausschalten.", 0, 60, "Minuten", { e.timerKlingelMax }) { e.timerKlingelMax = it },

            // ---- Modell ----
            auswahl("modell", "Modell", "Modell", "Das Sprachmodell, mit dem Jarvis denkt.",
                { CodexModel.entries.map { it.name to it.label } }, { e.modell.name }) { w ->
                val m = CodexModel.valueOf(w); e.modell = m; e.denkstufe = m.normalizeEffort(e.denkstufe)
            },
            auswahl("denkstufe", "Modell", "Denkstufe", "Wie gründlich das Modell nachdenkt.",
                { e.modell.supportedEfforts.map { it.name to it.label } }, { e.denkstufe.name }) { e.denkstufe = ReasoningEffort.valueOf(it) },

            // ---- Darstellung ----
            auswahl("modus", "Darstellung", "Hell oder dunkel", "Helles oder dunkles Design.",
                { listOf("system" to "Automatisch", "hell" to "Hell", "dunkel" to "Dunkel") }, { e.modus }) { e.modus = it },
            auswahl("design", "Darstellung", "Farbdesign", "Blau (Jarvis) oder Gold (wie Geniale Ideen).",
                { listOf("blau" to "Blau", "gold" to "Gold") }, { e.design }) { e.design = it },
            auswahl("ablage_ansicht", "Darstellung", "Ansicht der Ablage", "Wie die Ablage ihre Dateien zeigt.",
                { listOf("gross" to "Große Kacheln", "mittel" to "Mittlere Kacheln", "klein" to "Kleine Kacheln", "details" to "Details", "liste" to "Liste") }, { e.ablageAnsicht }) { e.ablageAnsicht = it },

            // ---- System ----
            schalter("dienst_an", "System", "Bereitschaft im Hintergrund", "Jarvis läuft im Hintergrund (Plugin, Signale, Weckwort). Ausschalten nur in der App, sonst wäre Jarvis danach nicht mehr erreichbar.", { e.dienstAn }) {
                if (!it) throw IllegalArgumentException("Die Bereitschaft schalte ich nicht selbst ab, sonst wäre ich danach nicht mehr erreichbar. Das geht in der App unter Einstellungen → System.")
                e.dienstAn = true; de.frank.jarvis.dienst.JarvisDienst.abgleichen(app)
            },
            schalter("app_sperre", "System", "App-Sperre", "App nur mit Fingerabdruck öffnen. Ausschalten nur in der App selbst.", { e.appSperre }) {
                if (!it) throw IllegalArgumentException("Die App-Sperre schalte ich aus Sicherheitsgründen nicht per Zuruf ab. Das geht in der App unter Einstellungen → System.")
                e.appSperre = true
            },
        )
    }

    fun bereiche(): List<String> = eintraege.map { it.bereich }.distinct()

    fun beschreibe(bereich: String): String {
        val auswahl = if (bereich.isBlank()) eintraege else eintraege.filter { norm(it.bereich) == norm(bereich) || norm(it.bereich).contains(norm(bereich)) || norm(bereich).contains(norm(it.bereich)) }
        if (auswahl.isEmpty()) return "Den Bereich „$bereich“ kenne ich nicht. Es gibt: ${bereiche().joinToString(", ")}."
        return auswahl.groupBy { it.bereich }.entries.joinToString("\n\n") { (b, liste) ->
            "## $b\n" + liste.joinToString("\n") { "- ${it.titel} [${it.kennung}]: ${runCatching { it.lesen() }.getOrDefault("?")} — ${it.beschreibung} Werte: ${it.werte}." }
        }
    }

    /** first = geklappt, second = Antwort in einem Satz. */
    fun setze(kennung: String, wert: String): Pair<Boolean, String> {
        val k = norm(kennung)
        val eintrag = eintraege.firstOrNull { it.kennung == k } ?: eintraege.firstOrNull { norm(it.titel) == k }
            ?: return false to "Eine Einstellung „$kennung“ gibt es nicht. Lies erst die Liste mit aktion lesen."
        if (wert.isBlank()) return false to "Für „${eintrag.titel}“ fehlt der Wert. Möglich: ${eintrag.werte}."
        return try {
            eintrag.setzen(wert)
            true to "${eintrag.titel}: jetzt ${eintrag.lesen()}."
        } catch (f: IllegalArgumentException) {
            false to "${eintrag.titel}: ${f.message}"
        }
    }
}
