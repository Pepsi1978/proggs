package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.tts.TtsCatalog
import de.frank.jarvis.tts.TtsProvider
import java.util.Locale
import kotlinx.coroutines.launch

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
            beschreibung = "Jarvis: beschreibt, liest und ändert die Einstellungen der Jarvis-App (Benachrichtigungen je Meldung mit Ton, Wecker, Vorlesen, Stimme und Lautstärke, Ruhezeit, Vorlesen, Update-Ansagen, Spracheingabe, Hey Jarvis, " +
                "Wetter-Ort, Losfahr-Meldung, Tagesauswertung, Timer, Modell, Darstellung, System). aktion lesen ohne bereich = alle Bereiche mit ihren Schaltern, Stand und möglichen Werten; " +
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

    /**
     * Wie in der Oberfläche: Koordinaten zum Ortsnamen suchen (Open-Meteo), speichern und die Vorhersage neu holen.
     * Die Suche läuft vor dem Speichern: Scheitert sie (kein Netz, unbekannter Ort), bleibt der alte Ort.
     * Werkzeuge laufen nie auf dem Hauptfaden, der Netzaufruf darf hier blockieren.
     */
    private fun setzeWetterOrt(name: String) {
        val gefunden = (try {
            val adresse = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=de&name=" + java.net.URLEncoder.encode(name.trim(), "UTF-8")
            ORTSSUCHE.newCall(okhttp3.Request.Builder().url(adresse).build()).execute().use { a ->
                if (!a.isSuccessful) throw IllegalStateException("Open-Meteo antwortet mit ${a.code}")
                org.json.JSONObject(a.body?.string().orEmpty()).optJSONArray("results")?.optJSONObject(0)
            }
        } catch (f: Exception) {
            throw IllegalArgumentException("Die Ortssuche geht gerade nicht (${f.message ?: f.javaClass.simpleName}); der alte Ort bleibt.")
        }) ?: throw IllegalArgumentException("Den Ort „${name.trim()}“ habe ich nicht gefunden.")
        e.wetterBreite = gefunden.optDouble("latitude")
        e.wetterLaenge = gefunden.optDouble("longitude")
        e.wetterOrt = listOf(gefunden.optString("name"), gefunden.optString("admin1")).filter { it.isNotBlank() }.distinct().joinToString(", ")
        // Folgeaktion im Hintergrund; wetter_lesen holt bei geändertem Ort ohnehin frisch.
        NEBENHER.launch { runCatching { Register.alle(app).filterIsInstance<WetterFaehigkeit>().firstOrNull()?.synchronisiere() } }
    }

    /** Je Meldungsart die Schalter, die sie kann; Felder mit alter Kennung (ansage_…, abfahrt_…, ergebnisse_vorlesen) stehen schon einzeln da. */
    private fun meldungsEintraege(): List<Eintrag> = de.frank.jarvis.meldung.Meldungsarten.alle.flatMap { art ->
        val p = de.frank.jarvis.meldung.MeldungsProfil(e, art)
        val b = "Benachrichtigungen: ${art.titel}"
        fun neu(feld: String) = feld !in art.alt
        buildList {
            if (art.schaltbar && neu("an")) add(schalter(art.schluessel("an"), b, "${art.titel} anzeigen", art.wozu, { p.an }) {
                p.an = it
                if (art == de.frank.jarvis.meldung.Meldungsarten.STOPPUHR) de.frank.jarvis.timer.Stoppuhr.meldungErneuern(app)
            })
            if (art.minimierbar) add(schalter(art.schluessel("minimiert"), b, "${art.titel} minimiert", "Klein bei den stillen Meldungen, ohne Symbol in der Statusleiste.", { p.minimiert }) {
                p.minimiert = it
                if (art == de.frank.jarvis.meldung.Meldungsarten.BEREITSCHAFT) de.frank.jarvis.dienst.JarvisDienst.hinweisErneuern(app)
                else de.frank.jarvis.weckwort.WeckwortDienst.hinweisErneuern(app)
            })
            if (art.ton) add(schalter(art.schluessel("ton"), b, "${art.titel} mit Ton", "Aus = die Meldung kommt lautlos.", { p.ton }) { p.ton = it })
            if (art.wecker) add(schalter(art.schluessel("wecker"), b, "${art.titel} wie ein Wecker", "Klingelt mit dem Weckton, auch bei „Nicht stören“, bis die Meldung angetippt oder weggewischt wird.", { p.wecker }) { p.wecker = it })
            if (art.vorlesen && neu("vorlesen")) add(schalter(art.schluessel("vorlesen"), b, "${art.titel} vorlesen", "Liest die Meldung zusätzlich vor; ein Live-Gespräch wartet sie ab.", { p.vorlesen }) { p.vorlesen = it })
            if ((art.vorlesen || art.sprichtImmer) && neu("stimme")) add(auswahl(art.schluessel("stimme"), b, "Stimme für ${art.titel}", "Eigene Vorlese-Stimme nur für diese Meldung.",
                { listOf("" to "Wie Jarvis") + alleStimmen() }, { p.stimme }) { p.stimme = it })
            if ((art.vorlesen || art.sprichtImmer) && neu("tempo")) add(auswahl(art.schluessel("tempo"), b, "Tempo für ${art.titel}", "Sprechtempo nur für diese Meldung.",
                { listOf("0" to "Wie Jarvis") + tempi }, { if (p.tempo <= 0f) "0" else tempoText(p.tempo) }) { p.tempo = it.toFloat() })
            if ((art.vorlesen || art.sprichtImmer) && neu("lautstaerke")) add(zahl(art.schluessel("lautstaerke"), b, "Lautstärke für ${art.titel}", "Lautstärke beim Vorlesen; 0 = so laut, wie das Handy steht.", 0, 100, "Prozent", { p.lautstaerke }) { p.lautstaerke = it })
        }
    }

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
            schalter("diktat_antwort_vorlesen", "Vorlesen", "Antwort nach Diktat vorlesen", "Liest die erste Antwort vor, wenn die Frage mit dem Mikrofon in der Eingabeblase eingesprochen und dann mit dem Pfeil gesendet wurde.", { e.diktatAntwortVorlesen }) { e.diktatAntwortVorlesen = it },
            schalter("ergebnisse_vorlesen", "Benachrichtigungen: Agent fertig", "Fertige Agenten-Ergebnisse vorlesen", "Liest eine kurze, vorlesetaugliche Zusammenfassung des Agenten-Ergebnisses von selbst vor, sobald er fertig ist; der ganze Bericht bleibt in der Ablage.", { e.ergebnisseVorlesen }) { e.ergebnisseVorlesen = it },

            // ---- Update-Ansagen ----
            schalter("ansage_vorlesen", "Benachrichtigungen: Update-Ansagen", "Update-Ansagen vorlesen", "Sagt an, wenn in UpdateStation ein neues Update bereitliegt. Aus = nur die stille Benachrichtigung.", { e.ansageVorlesen }) { e.ansageVorlesen = it },
            auswahl("ansage_stimme", "Benachrichtigungen: Update-Ansagen", "Stimme der Ansagen", "Eigene Stimme nur für die Update-Ansagen.",
                { listOf("" to "Wie Jarvis") + alleStimmen() }, { e.ansageStimme }) { e.ansageStimme = it },
            auswahl("ansage_tempo", "Benachrichtigungen: Update-Ansagen", "Tempo der Ansagen", "Sprechtempo nur für die Update-Ansagen.",
                { listOf("0" to "Wie Jarvis") + tempi }, { if (e.ansageTempo <= 0f) "0" else tempoText(e.ansageTempo) }) { e.ansageTempo = it.toFloat() },
            zahl("ansage_lautstaerke", "Benachrichtigungen: Update-Ansagen", "Lautstärke der Ansagen", "Lautstärke nur für die Ansage, danach gilt wieder die alte; 0 = so laut, wie das Handy steht.", 0, 100, "Prozent", { e.ansageLautstaerke }) { e.ansageLautstaerke = it },

            // ---- Spracheingabe ----
            schalter("filter_stille", "Spracheingabe", "Filter: Stille vorab", "Wirft Aufnahmen ohne Sprache weg, bevor sie umgeschrieben werden.", { s.filterStilleVorabAn }) { s.filterStilleVorabAn = it },
            schalter("filter_segmente", "Spracheingabe", "Filter: Segment-Werte", "Verwirft Textstücke, die das Erkennungsmodell selbst für unsicher hält.", { s.filterSegmentmetrikenAn }) { s.filterSegmentmetrikenAn = it },
            schalter("filter_zeitstempel", "Spracheingabe", "Filter: Zeitstempel", "Verwirft Text, der außerhalb der gesprochenen Zeit liegt.", { s.filterZeitstempelAn }) { s.filterZeitstempelAn = it },
            schalter("filter_floskeln", "Spracheingabe", "Filter: Floskeln", "Entfernt typische erfundene Floskeln wie „Untertitel von …“.", { s.filterFloskelnAn }) { s.filterFloskelnAn = it },

            // ---- Hey Jarvis ----
            schalter("weckwort_an", "Hey Jarvis", "Auf „Hey Jarvis“ lauschen", "Dauerhaft auf das Weckwort hören, auch bei gesperrtem Handy.", { e.weckwortAn }) {
                e.weckwortAn = it; de.frank.jarvis.weckwort.WeckwortDienst.abgleichen(app)
            },
            auswahl("weckwort_empfindlichkeit", "Hey Jarvis", "Empfindlichkeit", "Wie leicht das Weckwort anspringt und wie großzügig die Stimmerkennung urteilt.",
                { listOf("niedrig" to "Niedrig", "normal" to "Normal", "hoch" to "Hoch", "sehr_hoch" to "Sehr hoch") }, { e.weckwortEmpfindlichkeit }) {
                e.weckwortEmpfindlichkeit = it
                de.frank.jarvis.weckwort.WeckwortDienst.empfindlichkeitUebernehmen(app)
            },
            auswahl("weckwort_art", "Hey Jarvis", "Weckwort", "„Hey Jarvis“ oder die eigene Phrase „${e.weckwortPhrase}“.",
                { listOf("jarvis" to "Hey Jarvis", "phrase" to "Eigene Phrase") }, { e.weckwortArt }) {
                if (e.weckwortArt != it) { e.weckwortArt = it; de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(app) }
            },
            // Wie „Übernehmen“ in der Oberfläche: prüfen, speichern, auf die eigene Phrase umschalten, Weckwort neu laden.
            Eintrag("weckwort_phrase", "Hey Jarvis", "Eigene Phrase", "Das eigene Weckwort, englisch ausgesprochen; drei Wörter mit verschiedenen Anfangslauten erkennt es am sichersten. Setzen schaltet auf die eigene Phrase um.",
                "zwei bis drei englische Wörter, nur A bis Z, z. B. OKAY COMPUTER JARVIS", { e.weckwortPhrase }) { w ->
                val phrase = de.frank.jarvis.weckwort.PhrasenTokens.normiert(w)
                if (phrase.split(' ').size < 2) throw IllegalArgumentException("Bitte mindestens zwei Wörter, besser drei.")
                val zerlegt = try {
                    de.frank.jarvis.weckwort.PhrasenTokens.zerlege(app, phrase)
                } catch (f: Exception) {
                    throw IllegalArgumentException("Das Phrasen-Modell ließ sich nicht laden (${f.message ?: f.javaClass.simpleName}); die alte Phrase bleibt.")
                }
                if (zerlegt == null) throw IllegalArgumentException("Das Modell kennt nur englische Buchstaben A bis Z, ohne Umlaute und Ziffern.")
                e.weckwortPhrase = phrase
                e.weckwortArt = "phrase"
                de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(app)
            },

            // ---- Kalender und Wetter ----
            Eintrag("wetter_ort", "Kalender und Wetter", "Ort für die Wettervorhersage", "Für welchen Ort Jarvis das Wetter vorhersagt; der Name wird gesucht wie in der App.",
                "ein Ortsname, z. B. Potsdam", { e.wetterOrt }) { setzeWetterOrt(it) },

            // ---- Losfahr-Meldung ----
            schalter("abfahrt_an", "Benachrichtigungen: Losfahren", "Losfahr-Meldung", "Meldet an Arbeitstagen rechtzeitig vor der nötigen Abfahrt.", { e.abfahrtAn }) { e.abfahrtAn = it; abfahrtNeu() },
            schalter("abfahrt_vorlesen", "Benachrichtigungen: Losfahren", "Losfahr-Meldung vorlesen", "Liest die Meldung zusätzlich vor.", { e.abfahrtVorlesen }) { e.abfahrtVorlesen = it },
            zahl("abfahrt_lautstaerke", "Benachrichtigungen: Losfahren", "Lautstärke der Losfahr-Meldung", "Lautstärke der vorgelesenen Meldung; 0 = so laut, wie das Handy steht.", 0, 100, "Prozent", { e.abfahrtLautstaerke }) { e.abfahrtLautstaerke = it },
            zahl("abfahrt_vorlauf", "Benachrichtigungen: Losfahren", "Vorlauf", "So viele Minuten vor der nötigen Abfahrt kommt die Meldung.", 0, 60, "Minuten", { e.abfahrtVorlauf }) { e.abfahrtVorlauf = it; abfahrtNeu() },
            uhrzeit("ankunft_nacht", "Losfahr-Meldung", "Ankunft vor dem Nachtdienst", "Wann Frank vor dem Nachtdienst auf Arbeit sein will; danach richtet sich die Losfahr-Meldung.", { e.ankunftNacht }) { e.ankunftNacht = it; abfahrtNeu() },
            uhrzeit("ankunft_tag", "Losfahr-Meldung", "Ankunft vor dem Tagdienst", "Wann Frank vor dem Tagdienst auf Arbeit sein will; danach richtet sich die Losfahr-Meldung.", { e.ankunftTag }) { e.ankunftTag = it; abfahrtNeu() },

            // ---- Tagesauswertung ----
            schalter("auswertung_an", "Tagesauswertung", "Tagesauswertung", "Erstellt regelmäßig die Tagesauswertung.", { e.auswertungAn }) { e.auswertungAn = it; zeitplanNeu() },
            zahl("auswertung_abstand", "Tagesauswertung", "Abstand", "Alle wie viele Stunden die Auswertung läuft.", 1, 12, "Stunden", { e.auswertungAbstand }) { e.auswertungAbstand = it; zeitplanNeu() },
            schalter("auswertung_schlafpause", "Tagesauswertung", "Schlafpause", "Ruht, solange Frank laut Dienstplan schläft.", { e.auswertungSchlafpause }) { e.auswertungSchlafpause = it; zeitplanNeu() },
            schalter("auswertung_deutung", "Tagesauswertung", "Deutung schreiben", "Das Modell schreibt eine Deutung; aus = nur Daten abgleichen.", { e.auswertungDeutung }) { e.auswertungDeutung = it },
            schalter("auswertung_lernen", "Tagesauswertung", "Aus dem Tagebuch lernen", "Verdichtet alte Monate und notiert neue Tatsachen über Frank.", { e.auswertungLernen }) { e.auswertungLernen = it },

            // ---- Timer ----
            schalter("timer_wecker", "Benachrichtigungen: Timer", "Timer klingelt wie ein Wecker", "An = klingelt bis zum Ausschalten, aus = kurze Meldung mit Ton.", { e.timerWecker }) { e.timerWecker = it },
            schalter("timer_vibration", "Benachrichtigungen: Timer", "Vibration", "Vibriert beim Klingeln.", { e.timerVibration }) { e.timerVibration = it },
            zahl("timer_lautstaerke", "Benachrichtigungen: Timer", "Lautstärke des Wecktons", "In Prozent der Wecker-Lautstärke des Handys.", 10, 100, "Prozent", { e.timerLautstaerke }) { e.timerLautstaerke = it },
            zahl("timer_schlummern", "Benachrichtigungen: Timer", "Schlummern", "Minuten für „Schlummern“.", 1, 30, "Minuten", { e.timerSchlummern }) { e.timerSchlummern = it },
            zahl("timer_klingel_max", "Benachrichtigungen: Timer", "Höchstens klingeln", "Danach bleibt eine stille Meldung; 0 = bis zum Ausschalten.", 0, 60, "Minuten", { e.timerKlingelMax }) { e.timerKlingelMax = it },

            // ---- Benachrichtigungen: je Meldungsart aus derselben Liste wie der Bildschirm; Felder mit alten Kennungen stehen oben ----
            *meldungsEintraege().toTypedArray(),
            schalter("meldung_ruhe_an", "Benachrichtigungen", "Ruhezeit", "In der Ruhezeit kommen Agenten, Ablage, Programmieraufträge und Update-Ansagen lautlos, ohne Vorlesen und Wecker.", { e.meldungRuheAn }) { e.meldungRuheAn = it },
            uhrzeit("meldung_ruhe_von", "Benachrichtigungen", "Ruhezeit beginnt", "Ab dieser Uhrzeit gilt die Ruhezeit.", { e.meldungRuheVon }) { e.meldungRuheVon = it },
            uhrzeit("meldung_ruhe_bis", "Benachrichtigungen", "Ruhezeit endet", "Bis zu dieser Uhrzeit gilt die Ruhezeit.", { e.meldungRuheBis }) { e.meldungRuheBis = it },
            schalter("meldung_inhalt_verbergen", "Benachrichtigungen", "Inhalt auf dem Sperrbildschirm verbergen", "Auf dem Sperrbildschirm steht nur „Jarvis“, nicht der Text der Meldung.", { e.meldungInhaltVerbergen }) { e.meldungInhaltVerbergen = it },

            // ---- Modell ----
            auswahl("modell", "Modell", "Modell", "Das Sprachmodell, mit dem Jarvis denkt.",
                { CodexModel.entries.map { it.name to it.label } }, { e.modell.name }) { w ->
                val m = CodexModel.valueOf(w); e.modell = m; e.denkstufe = m.normalizeEffort(e.denkstufe)
            },
            auswahl("denkstufe", "Modell", "Denkstufe", "Wie gründlich das Modell nachdenkt.",
                { e.modell.supportedEfforts.map { it.name to it.label } }, { e.denkstufe.name }) { e.denkstufe = ReasoningEffort.valueOf(it) },
            auswahl("schrittlimit", "Modell", "Schrittlimit pro Anfrage", "Höchstens so viele Bearbeitungsrunden samt Werkzeugaufrufen je Anfrage; Unbegrenzt hebt nur dieses Limit auf (Zeitlimit und Abbrechen gelten weiter) und kann mehr kosten und länger dauern.",
                { de.frank.jarvis.data.Einstellungen.SCHRITTLIMITS.map { it.toString() to (if (it == 0) "Unbegrenzt" else "$it Schritte") } }, { e.schrittlimit.toString() }) { e.schrittlimit = it.toInt() },

            // ---- Live-Gespräch ----
            // Alle Einstellungen aus derselben Liste wie der Einstellungs-Bildschirm, samt Preis in der Beschreibung.
            *de.frank.jarvis.live.LiveOptionen.alle.map { o ->
                auswahl("live_" + o.id, o.bereich, o.titel, o.beschreibung,
                    { o.werte(de.frank.jarvis.live.LiveOptionen.Stand(s)).map { w -> w.id to (w.titel + if (w.preis.isNotEmpty()) " (${w.preis})" else "") } },
                    { de.frank.jarvis.live.LiveOptionen.Stand(s)[o.id] }) { de.frank.jarvis.live.LiveOptionen.Stand(s).setze(o.id, it) }
            }.toTypedArray(),

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

    private companion object {
        val ORTSSUCHE: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS).readTimeout(15, java.util.concurrent.TimeUnit.SECONDS).build()
        /** Für Folgeaktionen, auf die die Antwort nicht warten muss. */
        val NEBENHER = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
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
        } catch (f: Exception) {
            // Wert geprüft und gespeichert, aber die Folgeaktion (Dienst, Zeitplan, Weckwort …) scheiterte: das melden,
            // statt die Änderung als fehlgeschlagen auszugeben und die weiteren Änderungen abzubrechen.
            de.frank.jarvis.observability.Logbuch.fehler("Einstellung $k", f)
            true to "${eintrag.titel}: gesetzt (jetzt ${runCatching { eintrag.lesen() }.getOrDefault("?")}), Folgeaktion fehlgeschlagen: ${f.message ?: f.javaClass.simpleName}"
        }
    }
}
