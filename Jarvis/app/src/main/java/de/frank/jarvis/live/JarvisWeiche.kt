package de.frank.jarvis.live

/**
 * Gemini Live mit „Jarvis direkt“: Die App, nicht das Sprachmodell, sorgt dafür, dass jeder abgeschlossene Beitrag von
 * Frank an Jarvis geht und Gemini nur Jarvis' Antworten spricht.
 *
 * Warum nicht per Anweisung allein: `gemini-3.8-live` hat Proactive Audio fest an (es darf Beiträge, die es für unwichtig
 * hält, unbeantwortet lassen), und die Live-API kennt keinen erzwungenen Werkzeugaufruf (kein `toolConfig` im Setup).
 *
 * - Inhalt: Franks erkannte Worte aus der endgültigen Eingabe-Mitschrift (`inputTranscription`), der Reihe nach gesammelt,
 *   nicht Geminis Umformulierung im Werkzeugaufruf. Vorläufige Mitschrift zählt nur als „Frank spricht noch“.
 * - Abschluss eines Beitrags: Werkzeugaufruf, eigene Ausgabe oder Zugende von Gemini, oder [ruheMs] ohne neue Mitschrift
 *   (Proactive Audio schweigt). Danach noch [NACHLAUF_MS], weil die Mitschrift dem Werkzeugaufruf hinterherlaufen kann.
 * - Tonsperre ([tonFrei]): Gemini-Ton und -Mitschrift zählen nur, solange Gemini eine Antwort von Jarvis spricht (und bei der
 *   Begrüßung). Jarvis' Antwort darf Gemini dabei natürlich ausformulieren (Anweisung), prüfbar ist das vor dem Abspielen nicht.
 * - Neutrales Signal: Eine eigene Äußerung von Gemini bei gesperrtem Ton wird gepuffert und nur hörbar, wenn ihre Mitschrift
 *   aus [SIGNAL_WORTE] besteht (etwa „Okay, Moment.“), Jarvis noch arbeitet oder gleich gefragt wird und Frank nicht weiterspricht.
 *   Höchstens eins je Antwort, nie zweimal dasselbe hintereinander; ohne Mitschrift, zu lang oder inhaltlich bleibt es stumm.
 * - Immer nur eine Anfrage an Jarvis. Spricht Frank weiter, bevor die Antwort gesprochen ist, wird sie nicht mehr vorgelesen,
 *   sondern mit dem neuen Beitrag an Jarvis gegeben; so geht keine Bestätigung verloren und nichts Überholtes wird gesagt.
 * - Zurück an Gemini als Werkzeug-Ergebnis, sonst als markierte Eingabe ([MARKE]). Ruft Gemini daraufhin noch einmal
 *   jarvis_fragen auf, antwortet die Weiche aus dem Speicher, ohne Jarvis erneut zu fragen (keine doppelten Aktionen).
 *
 * Reine Logik ohne Android, damit sie ohne Gerät prüfbar ist. Alle Aufrufe laufen unter der Sperre dieses Objekts; die
 * zurückgegebenen [Aktion]en führt [LiveGespraech] danach aus.
 */
class JarvisWeiche(
    /** So lange ohne neue Mitschrift, dann gilt Franks Beitrag als abgeschlossen, auch wenn Gemini nicht reagiert. */
    private val ruheMs: Long,
) {
    sealed interface Aktion {
        /** Franks gesammelte Mitschrift jetzt in den Chat, vor der Übergabe. */
        object NutzerFertig : Aktion
        /** Übergabe an Jarvis: [auftrag] = Franks Worte, [hinweis] = Begleitumstände für Jarvis (nicht für Befehlserkennung). */
        data class AnJarvis(val kennung: String, val auftrag: String, val hinweis: String) : Aktion
        /** Werkzeug-Ergebnis an Gemini: der erste offene Aufruf bekommt [text], weitere nur einen Verweis darauf. */
        data class Werkzeugantwort(val ids: List<String>, val text: String) : Aktion
        /** Jarvis' Antwort als neue, markierte Eingabe an Gemini, weil kein Werkzeugaufruf mehr offen ist. */
        data class Einspielen(val text: String) : Aktion
        /** Nur in den Chat: Gemini hat Jarvis' Antwort nicht vorgelesen. */
        data class Anzeigen(val text: String) : Aktion
        /** Geminis gepuffertes neutrales Signal ([text]) jetzt abspielen; nicht in den Chat. */
        class Signal(val ton: List<ByteArray>, val text: String) : Aktion
        data class Fehler(val text: String) : Aktion
        data class Protokoll(val text: String) : Aktion
    }

    private enum class Tor { BEGRUESSUNG, ZU, WARTET, OFFEN }

    /** BEGRUESSUNG und OFFEN: Gemini darf hörbar sprechen. WARTET: eine abgelöste eigene Antwort von Gemini läuft noch aus. */
    @Volatile private var tor = Tor.BEGRUESSUNG
    val tonFrei: Boolean get() = tor == Tor.BEGRUESSUNG || tor == Tor.OFFEN

    private class Uebergabe(val kennung: String, val seitMs: Long)
    /** Ein Werkzeugaufruf ohne Mitschrift: wartet kurz auf sie, sonst gilt Geminis Fassung. */
    private class OhneMitschrift(val auftrag: String, val seitMs: Long, val begruessung: Boolean)

    private val beitrag = StringBuilder()
    private var letzteMitschriftMs = 0L
    /** Frank spricht laut vorläufiger Mitschrift; verlängert die Ruhezeit, liefert aber keinen Inhalt. */
    private var letzteSpracheMs = 0L
    private var abschlussFaellig = false
    /** Geminis Fassung, falls Franks Mitschrift ausbleibt (nur dann verwendet). */
    private var ersatzAuftrag = ""
    private var ohneMitschrift: OhneMitschrift? = null
    /** Gemini erzeugt gerade (Ausgabe seit dem letzten Zugende). */
    private var erzeugt = false
    private val offeneAufrufe = mutableListOf<String>()
    @Volatile private var laufend: Uebergabe? = null
    /** Abgeschlossene Beiträge, die warten, bis Jarvis' laufende Antwort da ist (zusammengefasst). */
    private var wartend: String? = null
    /** Antworten von Jarvis, die Frank nicht gehört hat, weil er weitersprach. */
    private var ungehoert: String? = null
    private var unterbrochen = false
    private var letzteAntwort = ""
    private var echos = 0
    /** Seit der letzten Antwort an Gemini kam Mitschrift von Frank. */
    private var nutzerSeitAbgabe = false
    private var abgabeMs = 0L
    private var wartetSeitMs = 0L
    private var tonSeitOffen = false
    /** Eigene Äußerung von Gemini bei gesperrtem Ton; [endeMs] = Werkzeugaufruf oder Zugende, danach entscheidet [pruefen]. */
    private class Gepuffert(val seitMs: Long) {
        val ton = mutableListOf<ByteArray>()
        var bytes = 0
        val text = StringBuilder()
        var endeMs = 0L
    }
    private var gepuffert: Gepuffert? = null
    /** In diesem Antwortzyklus schon ein Signal gespielt oder verworfen: Mehr als eins wäre eine Warteschleife. */
    private var signalGenutzt = false
    private var letztesSignal = ""
    /** Übergaben nacheinander nur aus Geminis Fassung (keine Mitschrift): begrenzt, damit Gemini keine Schleife anstößt. */
    private var ohneMitschriftFolge = 0
    private var nr = 0

    /** Jarvis arbeitet oder ein Beitrag wartet auf ihn: „Jarvis schaut nach …“ bleibt stehen. */
    val arbeitet: Boolean get() = laufend != null || wartend != null

    /** Ein Stück endgültiger Eingabe-Mitschrift. */
    @Synchronized fun nutzerText(text: String, jetzt: Long): List<Aktion> {
        if (text.isBlank()) return pruefen(jetzt)
        beitrag.append(text)
        letzteMitschriftMs = jetzt
        letzteSpracheMs = jetzt
        nutzerSeitAbgabe = true
        ohneMitschriftFolge = 0
        if (tor == Tor.BEGRUESSUNG) tor = Tor.ZU
        val aus = signalVerwerfenWennNeu(jetzt)
        // Frank spricht weiter, bevor Jarvis' Antwort hörbar ist: Sie ist überholt, bleibt stumm und geht mit dem neuen Beitrag an Jarvis.
        if ((tor == Tor.OFFEN || tor == Tor.WARTET) && !tonSeitOffen) {
            tor = Tor.ZU
            ungehoert = ungehoert?.let { "$it\n$letzteAntwort" } ?: letzteAntwort
        }
        return aus + pruefen(jetzt)
    }

    /** Vorläufige Mitschrift: Frank spricht noch. Kein Inhalt, kein Abschluss. */
    @Synchronized fun nutzerSpricht(jetzt: Long): List<Aktion> { letzteSpracheMs = jetzt; return signalVerwerfenWennNeu(jetzt) }

    /** Frank spricht neu (nicht bloß nachlaufende Mitschrift von vorher): Ein gepuffertes Signal ist überholt. */
    private fun signalVerwerfenWennNeu(jetzt: Long): List<Aktion> {
        val g = gepuffert ?: return emptyList()
        if (jetzt - g.seitMs <= NACHLAUF_MS) return emptyList()
        return signalVerwerfen("Frank spricht weiter", verbraucht = false)
    }

    /** [verbraucht]: Gemini hatte seine Gelegenheit (inhaltlich, zu lang, wiederholt); sonst darf später noch ein Signal kommen. */
    private fun signalVerwerfen(grund: String, verbraucht: Boolean = true): List<Aktion> {
        val g = gepuffert ?: return emptyList()
        gepuffert = null
        if (verbraucht) signalGenutzt = true
        return listOf(Aktion.Protokoll("Weiche: eigene Äußerung von Gemini stumm ($grund): „${g.text.toString().trim().take(80)}“"))
    }

    /** Geminis eigener Ton bei gesperrtem Ton ([tonFrei] false): gepuffert, falls er ein neutrales Signal sein kann. */
    @Synchronized fun eigenerTon(pcm: ByteArray, jetzt: Long) {
        val g = puffer(jetzt) ?: return
        if (g.endeMs > 0) return
        g.bytes += pcm.size
        // Zu lang für ein Signal: Der Inhalt bleibt stumm, egal was die Mitschrift sagt.
        if (g.bytes > SIGNAL_MAX_BYTES) g.ton.clear() else g.ton += pcm
    }

    /** Geminis Ausgabe-Mitschrift bei gesperrtem Ton: entscheidet, ob der gepufferte Ton ein neutrales Signal ist. */
    @Synchronized fun eigeneMitschrift(text: String, jetzt: Long) { puffer(jetzt)?.text?.append(text) }

    private fun puffer(jetzt: Long): Gepuffert? {
        gepuffert?.let { return it }
        // Nur, wenn es etwas zu überbrücken gibt: Franks Beitrag ist da oder Jarvis arbeitet.
        if (tor != Tor.ZU || signalGenutzt || (beitrag.isEmpty() && !arbeitet)) return null
        return Gepuffert(jetzt).also { gepuffert = it }
    }

    /**
     * Gemini gibt Ton oder Ausgabe-Mitschrift aus. Nur echter Ton ([ton]) zählt als „Antwort wird gesprochen“: Bei Googles
     * offenem Fehler (Mitschrift ohne Ton) soll der Wächter melden, dass nichts zu hören war.
     */
    @Synchronized fun modellAusgabe(jetzt: Long, ton: Boolean = true): List<Aktion> {
        erzeugt = true
        val aus = mutableListOf<Aktion>()
        if (ton && tor == Tor.OFFEN && !tonSeitOffen) {
            tonSeitOffen = true
            aus += Aktion.Protokoll("Weiche: Gemini spricht Jarvis' Antwort, ${jetzt - abgabeMs} ms nach der Abgabe")
        }
        // Gemini antwortet selbst: Franks Zug ist zu Ende, sein Beitrag geht jetzt an Jarvis.
        if (tor == Tor.ZU && beitrag.isNotEmpty()) abschlussFaellig = true
        return aus + pruefen(jetzt)
    }

    @Synchronized fun werkzeugAufruf(id: String, auftrag: String, jetzt: Long): List<Aktion> {
        // Werkzeuge warten (BLOCKING): Gemini erzeugt nichts, bis das Ergebnis da ist.
        erzeugt = false
        val aus = mutableListOf<Aktion>()
        // BLOCKING: Was Gemini vor dem Aufruf sagte („Moment“), ist abgeschlossen; Jarvis' Antwort folgt im selben Zug.
        gepuffert?.let { if (it.endeMs == 0L) it.endeMs = jetzt }
        when {
            beitrag.isNotEmpty() -> { offeneAufrufe += id; abschlussFaellig = true; ersatzAuftrag = auftrag }
            // Derselbe Zug, den die Weiche schon übergeben hat (etwa nach der Ruhezeit): Antwort kommt über diesen Aufruf.
            laufend != null || wartend != null -> offeneAufrufe += id
            // Gemini gibt die eben eingespielte Antwort weiter: aus dem Speicher beantworten, Jarvis nicht noch einmal fragen.
            (tor == Tor.OFFEN || tor == Tor.WARTET) && !nutzerSeitAbgabe && letzteAntwort.isNotEmpty() -> {
                echos++
                if (echos <= ECHO_MAX) {
                    aus += Aktion.Protokoll("Weiche: Gemini fragt Jarvis erneut ohne neuen Beitrag ($echos), Antwort aus dem Speicher")
                    aus += Aktion.Werkzeugantwort(listOf(id), letzteAntwort)
                    tor = Tor.OFFEN
                } else {
                    aus += Aktion.Werkzeugantwort(listOf(id), "Das ist bereits Jarvis' Antwort. Sag jetzt nichts mehr.")
                    aus += Aktion.Anzeigen(letzteAntwort)
                    aus += Aktion.Fehler("Gemini hat Jarvis' Antwort nicht vorgelesen; sie steht im Chat.")
                    tor = Tor.ZU
                }
            }
            else -> {
                // Die Mitschrift läuft dem Aufruf oft hinterher: kurz auf sie warten.
                offeneAufrufe += id
                ohneMitschrift = OhneMitschrift(auftrag, jetzt, tor == Tor.BEGRUESSUNG)
            }
        }
        return aus + pruefen(jetzt)
    }

    @Synchronized fun werkzeugAbgebrochen(ids: Collection<String>) { offeneAufrufe.removeAll(ids.toSet()) }

    /** Neue Verbindung (goAway, Schlüsselwechsel): Aufrufe der alten Verbindung kann niemand mehr beantworten. */
    @Synchronized fun verbindungNeu() { offeneAufrufe.clear(); ohneMitschrift = null }

    @Synchronized fun zugEnde(jetzt: Long): List<Aktion> {
        erzeugt = false
        val aus = mutableListOf<Aktion>()
        when (tor) {
            Tor.BEGRUESSUNG -> tor = Tor.ZU
            // Die abgelöste eigene Antwort von Gemini ist zu Ende: Ab jetzt kommt Jarvis' Antwort.
            Tor.WARTET -> { tor = Tor.OFFEN; abgabeMs = jetzt }
            // Ohne Ton seit dem Öffnen ist es das Zugende von etwas Älterem; der Wächter in [pruefen] fängt Schweigen ab.
            Tor.OFFEN -> if (tonSeitOffen) { tor = Tor.ZU; aus += Aktion.Protokoll("Weiche: Jarvis' Antwort fertig gesprochen") }
            Tor.ZU -> {
                if (beitrag.isNotEmpty()) abschlussFaellig = true
                gepuffert?.let { if (it.endeMs == 0L) it.endeMs = jetzt }
            }
        }
        return aus + pruefen(jetzt)
    }

    /** Gemini meldet `interrupted`: Frank redet dazwischen oder die eingespielte Antwort hat Geminis eigene abgelöst. */
    @Synchronized fun unterbrochen(jetzt: Long): List<Aktion> {
        erzeugt = false
        val aus = signalVerwerfen("unterbrochen", verbraucht = false)
        when (tor) {
            Tor.BEGRUESSUNG -> tor = Tor.ZU
            Tor.WARTET -> { tor = Tor.OFFEN; abgabeMs = jetzt }
            Tor.OFFEN -> if (tonSeitOffen) { tor = Tor.ZU; unterbrochen = true }
            Tor.ZU -> {}
        }
        return aus + pruefen(jetzt)
    }

    /** Frank tippt die Blase an, während Gemini Jarvis' Antwort spricht. */
    @Synchronized fun angetippt() { if (tor == Tor.OFFEN && tonSeitOffen) unterbrochen = true }

    @Synchronized fun jarvisAntwort(kennung: String, text: String, jetzt: Long): List<Aktion> {
        val l = laufend
        if (l == null || l.kennung != kennung) return listOf(Aktion.Protokoll("Weiche: verspätete Antwort zu $kennung verworfen"))
        laufend = null
        val antwort = text.trim().ifEmpty { "Jarvis hat keine Antwort geliefert." }
        val aus = mutableListOf<Aktion>(Aktion.Protokoll("Weiche: Antwort von Jarvis nach ${jetzt - l.seitMs} ms, ${antwort.length} Zeichen"))
        if (wartend != null || beitrag.isNotEmpty()) {
            // Frank hat inzwischen weitergesprochen: nicht vorlesen, sondern mit seinem neuen Beitrag an Jarvis geben.
            ungehoert = ungehoert?.let { "$it\n$antwort" } ?: antwort
            aus += Aktion.Protokoll("Weiche: Frank sprach weiter, Antwort geht mit dem nächsten Beitrag an Jarvis")
        } else {
            aus += abgeben(antwort, jetzt)
        }
        return aus + pruefen(jetzt)
    }

    /** Regelmäßig (alle 100 ms) und nach jedem Ereignis: Ruhezeit, Mitschrift-Warten und Wächter. */
    @Synchronized fun takt(jetzt: Long): List<Aktion> = pruefen(jetzt)

    private fun pruefen(jetzt: Long): List<Aktion> {
        val aus = mutableListOf<Aktion>()
        if (tor == Tor.WARTET && jetzt - wartetSeitMs >= WARTEN_MAX_MS) { tor = Tor.OFFEN; abgabeMs = jetzt }
        if (tor == Tor.OFFEN && !tonSeitOffen && abgabeMs > 0 && letzteAntwort.isNotEmpty() && jetzt - abgabeMs >= STUMM_MAX_MS) {
            aus += Aktion.Anzeigen(letzteAntwort)
            aus += Aktion.Fehler("Gemini hat Jarvis' Antwort nicht vorgelesen; sie steht im Chat.")
            tor = Tor.ZU
        }
        laufend?.let { l ->
            if (jetzt - l.seitMs >= JARVIS_MAX_MS) aus += jarvisAntwort(l.kennung,
                "Jarvis hat nicht rechtzeitig geantwortet. Ich kann dazu gerade nichts sagen; frag bitte gleich noch einmal.", jetzt)
        }
        ohneMitschrift?.let { o ->
            if (beitrag.isNotEmpty()) { ohneMitschrift = null; abschlussFaellig = true }
            else if (jetzt - o.seitMs >= MITSCHRIFT_WARTEN_MS) {
                ohneMitschrift = null
                if (o.begruessung || o.auftrag.isBlank() || ohneMitschriftFolge >= OHNE_MITSCHRIFT_MAX) {
                    if (ohneMitschriftFolge >= OHNE_MITSCHRIFT_MAX && !o.begruessung)
                        aus += Aktion.Fehler("Gemini liefert keine Mitschrift deiner Worte; bitte das Live-Gespräch neu starten.")
                    aus += Aktion.Werkzeugantwort(offeneAufrufe.toList(), "Frank hat noch nichts gesagt. Sag nichts weiter.")
                    offeneAufrufe.clear()
                } else {
                    ohneMitschriftFolge++
                    // Keine Mitschrift von Frank: bestmöglich mit Geminis Fassung, deutlich gekennzeichnet.
                    beitrag.append(o.auftrag)
                    ersatzAuftrag = MITSCHRIFT_FEHLT
                    abschlussFaellig = true
                    letzteMitschriftMs = 0L
                }
            }
        }
        val ruhig = jetzt - maxOf(letzteMitschriftMs, letzteSpracheMs)
        if (beitrag.isNotEmpty() && (abschlussFaellig || ruhig >= ruheMs) && jetzt - letzteMitschriftMs >= NACHLAUF_MS) {
            val text = beitrag.toString().trim()
            val ersatz = ersatzAuftrag
            beitrag.setLength(0)
            abschlussFaellig = false
            ersatzAuftrag = ""
            aus += Aktion.NutzerFertig
            aus += Aktion.Protokoll("Weiche: Beitrag abgeschlossen (${text.length} Zeichen, ${jetzt - letzteMitschriftMs} ms nach dem letzten Wort)")
            val worte = if (ersatz == MITSCHRIFT_FEHLT) "$MITSCHRIFT_FEHLT$text" else text
            wartend = wartend?.let { "$it\n$worte" } ?: worte
        }
        val w = wartend
        if (laufend == null && w != null) {
            wartend = null
            aus += uebergeben(w, jetzt)
        }
        aus += signalPruefen(jetzt)
        return aus
    }

    /** Nach dem Ende der eigenen Äußerung (plus Nachlauf für die Mitschrift): abspielen, wenn sie ein neutrales Signal ist. */
    private fun signalPruefen(jetzt: Long): List<Aktion> {
        val g = gepuffert ?: return emptyList()
        if (g.endeMs == 0L) return if (jetzt - g.seitMs >= SIGNAL_MAX_MS) signalVerwerfen("kein Ende") else emptyList()
        if (jetzt - g.endeMs < SIGNAL_NACHLAUF_MS) return emptyList()
        val text = g.text.toString().trim()
        val grund = when {
            g.bytes > SIGNAL_MAX_BYTES -> "zu lang"
            !neutralesSignal(text) -> "kein neutrales Signal"
            signalSchluessel(text) == letztesSignal -> "wiederholt"
            g.ton.isEmpty() -> "kein Ton"
            tor != Tor.ZU || (!arbeitet && beitrag.isEmpty()) -> "nichts zu überbrücken"
            else -> null
        }
        if (grund != null) return signalVerwerfen(grund)
        gepuffert = null
        signalGenutzt = true
        letztesSignal = signalSchluessel(text)
        return listOf(Aktion.Signal(g.ton.toList(), text), Aktion.Protokoll("Weiche: neutrales Signal von Gemini hörbar: „$text“"))
    }

    private fun uebergeben(worte: String, jetzt: Long): Aktion {
        val kennung = "weiche-" + (++nr)
        val hinweis = buildString {
            append("(Live-Gespräch: Franks Worte aus der automatischen Spracherkennung, Hörfehler möglich. Gemini spricht deine Antwort " +
                "und formuliert sie dabei natürlich aus: Liefere den Kern klar und sprechbar.)")
            if (unterbrochen) append("\n(Frank hat deine vorige gesprochene Antwort unterbrochen.)")
            ungehoert?.let { append("\n(Deine vorige Antwort hat Frank nicht gehört, weil er weitersprach: „$it“. Beziehe sie ein, soweit sie noch passt, vor allem Bestätigungen erledigter Aktionen.)") }
        }
        unterbrochen = false
        ungehoert = null
        laufend = Uebergabe(kennung, jetzt)
        return Aktion.AnJarvis(kennung, worte, hinweis)
    }

    private fun abgeben(antwort: String, jetzt: Long): List<Aktion> {
        // Die Antwort ist da: Ein noch nicht gespieltes Signal käme zu spät.
        val aus = signalVerwerfen("Antwort von Jarvis ist da", verbraucht = false)
        signalGenutzt = false
        letzteAntwort = antwort
        echos = 0
        nutzerSeitAbgabe = false
        tonSeitOffen = false
        abgabeMs = jetzt
        if (offeneAufrufe.isNotEmpty()) {
            val ids = offeneAufrufe.toList()
            offeneAufrufe.clear()
            tor = Tor.OFFEN
            return aus + Aktion.Werkzeugantwort(ids, antwort)
        }
        // Läuft noch eine (stumme) eigene Antwort von Gemini, bricht die Eingabe sie ab; erst danach wird der Ton frei.
        if (erzeugt) { tor = Tor.WARTET; wartetSeitMs = jetzt } else tor = Tor.OFFEN
        return aus + Aktion.Einspielen(MARKE + antwort)
    }

    companion object {
        /** Kennzeichnet eine eingespielte Antwort von Jarvis; die Anweisung sagt Gemini, was damit zu tun ist. */
        const val MARKE = "[JARVIS-ANTWORT] "
        const val MITSCHRIFT_FEHLT = "(Keine Mitschrift von Franks Worten; sinngemäß vom Sprachmodell:) "
        /** Mitschrift kommt nach dem Werkzeugaufruf oder nach Geminis erster Ausgabe noch so lange nach. */
        const val NACHLAUF_MS = 350L
        /** So lange wartet ein Werkzeugaufruf ohne Mitschrift auf sie. */
        const val MITSCHRIFT_WARTEN_MS = 1_500L
        /** Höchstens so lange läuft eine abgelöste eigene Antwort von Gemini aus, dann wird der Ton frei. */
        const val WARTEN_MAX_MS = 1_500L
        /** Kommt so lange nach der Abgabe kein Ton, hat Gemini Jarvis' Antwort nicht gesprochen. */
        const val STUMM_MAX_MS = 10_000L
        /** Über dem Zeitlimit des Agenten (60 bzw. 120 s): Danach meldet die Weiche den Ausfall selbst. */
        const val JARVIS_MAX_MS = 150_000L
        const val ECHO_MAX = 2
        const val OHNE_MITSCHRIFT_MAX = 2

        /** Neutrale Signale, die Gemini sagen darf, während Jarvis arbeitet; die Anweisung nennt genau diese. */
        val SIGNALE = listOf("Mhm", "Okay", "Alles klar", "Moment", "Einen Moment", "Augenblick", "Ich schau nach", "Lass mich kurz nachsehen")
        /**
         * Wörter, aus denen ein hörbares Signal bestehen darf (Mitschrift kann abweichen: „Okay, einen kleinen Moment“).
         * Bewusst ohne Antwortwörter wie „ja“, „nein“, „genau“, „stimmt“, „klar“, „gut“ und ohne „erledigt“, „gemacht“, „fertig“:
         * Die beantworten eine Frage oder bestätigen eine Aktion. „Alles klar“ zählt nur als ganze Wendung.
         */
        val SIGNAL_WORTE = setOf("mhm", "mm", "hm", "hmm", "okay", "ok", "oke", "moment", "einen", "kleinen", "augenblick", "sekunde",
            "ich", "schau", "schaue", "seh", "sehe", "mal", "kurz", "nach", "lass", "mich", "nachsehen", "nachschauen", "verstehe", "aha", "ah", "oh", "so")
        const val SIGNAL_MAX_WORTE = 6
        /** Etwa 3 s Ton (PCM16 mono, 24 kHz); länger ist kein Signal mehr. */
        const val SIGNAL_MAX_BYTES = 144_000
        /** Nach Werkzeugaufruf oder Zugende so lange auf die nachlaufende Ausgabe-Mitschrift warten. */
        const val SIGNAL_NACHLAUF_MS = 400L
        /** Endet eine eigene Äußerung so lange nicht, bleibt sie stumm. */
        const val SIGNAL_MAX_MS = 6_000L

        fun neutralesSignal(text: String): Boolean {
            val t = text.lowercase(java.util.Locale.ROOT)
            // Nur Buchstaben und Satzzeichen: Ziffern und Zeichen („42“, „14:30“, „5 %“) sind Inhalt und machen es stumm.
            if (t.any { !it.isLetter() && !it.isWhitespace() && it !in SIGNAL_SATZZEICHEN }) return false
            val worte = Regex("alles\\s+klar|\\p{L}+").findAll(t).map { it.value }.toList()
            return worte.isNotEmpty() && worte.size <= SIGNAL_MAX_WORTE && worte.all { it in SIGNAL_WORTE || it.split(Regex("\\s+")) == listOf("alles", "klar") }
        }

        private const val SIGNAL_SATZZEICHEN = ".,!?…-–—'’\"„“‚‘"

        private fun signalSchluessel(text: String) = text.lowercase(java.util.Locale.ROOT).filter { it.isLetter() }
    }
}
