package de.frank.jarvis.live

import de.frank.jarvis.data.settings.SecureSettings
import java.util.Locale

/**
 * Alle Einstellungen des Live-Gesprächs an einer Stelle: Einstellungs-Bildschirm, Jarvis' Einstellungs-Katalog und der
 * Aufbau der Sitzung lesen dieselbe Liste. Preise und Werte nach der OpenAI-Doku, Stand 16.10.2026
 * (developers.openai.com/api/docs/pricing, models/gpt-live-1, guides/live-delegation, guides/realtime-costs).
 *
 * Zwei Arten von Live-Gespräch: GPT-Live (`gpt-live-1`, eigener Endpunkt, 0,05 $ pro Minute plus Denk-Modell) und die
 * Realtime-API (`gpt-realtime-*`, Abrechnung nach Tokens). Viele Optionen gibt es nur bei einer der beiden.
 */
object LiveOptionen {

    /** Ein Wert einer Option: Kennung, Anzeige, ein Satz Erklärung und der Preis (leer, wenn er nichts kostet). */
    class Wahl(val id: String, val titel: String, val beschreibung: String = "", val preis: String = "")

    class Option(
        val id: String,
        val bereich: String,
        val titel: String,
        val beschreibung: String,
        val standard: String,
        /** Die Werte; dürfen von den übrigen Einstellungen abhängen (Stimmen je Modell, Denkstufen je Denk-Modell). */
        val werte: (Stand) -> List<Wahl>,
        /** Ob die Option bei den aktuellen Einstellungen gilt (sonst ausgeblendet). */
        val gilt: (Stand) -> Boolean = { true },
    )

    /** Die gespeicherten Einstellungen; ein fehlender oder nicht mehr erlaubter Wert fällt auf den Standard zurück. */
    class Stand(private val s: SecureSettings, private val ueber: Map<String, String> = emptyMap()) {
        fun roh(id: String): String = ueber[id] ?: s.liveOption(id)
        operator fun get(id: String): String {
            val o = alle.first { it.id == id }
            val w = roh(id).ifBlank { o.standard }
            val erlaubt = o.werte(this).map { it.id }
            return if (w in erlaubt || erlaubt.isEmpty()) w else if (o.standard in erlaubt) o.standard else erlaubt.first()
        }
        fun setze(id: String, wert: String) = s.setzeLiveOption(id, wert)
        val modell get() = get("modell")
        val istLive get() = modell.startsWith("gpt-live")
        val denktBeiOpenAi get() = istLive && get("denken") == "openai"
    }

    // ---- Preise (USD) ----

    /** Preise je 1 Mio. Tokens: Eingabe, zwischengespeichert, Ausgabe. */
    class Preis(val ein: Double, val cache: Double, val aus: Double)

    /** Denk-Modelle für GPT-Live (Responses-Delegation), Standard-Tarif. */
    val BACKENDS = linkedMapOf(
        "gpt-6-luna" to Preis(0.10, 0.01, 0.50),
        "gpt-5.6-luna" to Preis(0.20, 0.02, 1.20),
        "gpt-6-sol" to Preis(2.00, 0.20, 10.00),
        "gpt-6.1-sol" to Preis(2.00, 0.10, 10.00),
        "gpt-5.6-terra" to Preis(2.00, 0.20, 12.00),
        "gpt-5.6-sol" to Preis(4.00, 0.40, 20.00),
        "gpt-6-astra" to Preis(10.00, 1.00, 50.00),
    )
    private val BACKEND_NAMEN = mapOf(
        "gpt-6-luna" to "GPT-6 Luna", "gpt-5.6-luna" to "GPT-5.6 Luna", "gpt-6-sol" to "GPT-6 Sol", "gpt-6.1-sol" to "GPT-6.1 Sol",
        "gpt-5.6-terra" to "GPT-5.6 Terra", "gpt-5.6-sol" to "GPT-5.6 Sol", "gpt-6-astra" to "GPT-6 Astra",
    )
    private val BACKEND_TEXT = mapOf(
        "gpt-6-luna" to "Schnell und sehr günstig, von OpenAI als Start empfohlen. Für Gespräche fast immer genug.",
        "gpt-5.6-luna" to "Ältere kleine Variante, etwas teurer als 6 Luna.",
        "gpt-6-sol" to "Kräftiger für schwierige Fragen, rund 20-mal teurer als Luna.",
        "gpt-6.1-sol" to "Neueste Sol-Fassung, kein Effort „none“, auch mit Ultrafast.",
        "gpt-5.6-terra" to "Mittelklasse der 5.6-Familie.",
        "gpt-5.6-sol" to "Starkes 5.6-Modell, Aktionspreis bis mindestens 21.11.2026.",
        "gpt-6-astra" to "Das stärkste Modell, sehr teuer. Kein Effort „none“, auch mit Ultrafast.",
    )

    /** Realtime-Modelle: Audio-Preise (Eingabe, zwischengespeichert, Ausgabe) und Text-Preise. */
    class RealtimePreis(val audio: Preis, val text: Preis)
    val REALTIME = linkedMapOf(
        "gpt-realtime-2.1" to RealtimePreis(Preis(32.0, 0.40, 64.0), Preis(4.0, 0.40, 24.0)),
        "gpt-realtime-2.1-mini" to RealtimePreis(Preis(10.0, 0.30, 20.0), Preis(0.60, 0.06, 2.40)),
        "gpt-realtime-2" to RealtimePreis(Preis(32.0, 0.40, 64.0), Preis(4.0, 0.40, 24.0)),
        "gpt-realtime-1.5" to RealtimePreis(Preis(32.0, 0.40, 64.0), Preis(4.0, 0.40, 16.0)),
        "gpt-realtime" to RealtimePreis(Preis(32.0, 0.40, 64.0), Preis(4.0, 0.40, 16.0)),
        "gpt-realtime-mini" to RealtimePreis(Preis(10.0, 0.30, 20.0), Preis(0.60, 0.06, 2.40)),
    )
    const val LIVE_PRO_MINUTE = 0.05
    val MITSCHRIFT_PRO_MINUTE = mapOf("gpt-transcribe" to 0.0045, "gpt-live-transcribe" to 0.017, "gpt-4o-mini-transcribe" to 0.003, "aus" to 0.0)
    val TARIF_FAKTOR = mapOf("auto" to 1.0, "default" to 1.0, "flex" to 0.5, "priority" to 2.0, "ultrafast" to 6.0)

    private val EFFORTS = mapOf(
        "none" to Wahl("none", "Keine", "Antwortet sofort ohne Nachdenken, am schnellsten. OpenAI empfiehlt das für Sprache."),
        "minimal" to Wahl("minimal", "Minimal", "Ganz kurzes Nachdenken."),
        "low" to Wahl("low", "Niedrig", "Kurz nachdenken, gut für Werkzeuge mit Fokus auf Tempo. Empfehlung für Jarvis."),
        "medium" to Wahl("medium", "Mittel", "Standard der Modelle, merklich langsamer im Gespräch."),
        "high" to Wahl("high", "Hoch", "Für schwere Fragen; deutlich längere Pausen und mehr Kosten."),
        "xhigh" to Wahl("xhigh", "Sehr hoch", "Nur wenn es wirklich nötig ist; lange Denkpausen."),
        "max" to Wahl("max", "Maximal", "Das Gründlichste. Neu, wird eventuell noch abgelehnt."),
    )
    private fun backendEfforts(modell: String): List<String> = when (modell) {
        "gpt-6.1-sol", "gpt-6-astra" -> listOf("low", "medium", "high", "xhigh", "max")
        else -> listOf("none", "low", "medium", "high", "xhigh", "max")
    }
    /** Denkstufe kostet Ausgabe-Tokens: grobe Zusatzmenge je Antwort für die Schätzung. */
    private val EFFORT_TOKENS = mapOf("none" to 0, "minimal" to 100, "low" to 300, "medium" to 1000, "high" to 3000, "xhigh" to 8000, "max" to 15000)

    private fun usd(v: Double): String = when {
        v == 0.0 -> "0 $"
        v < 0.01 -> String.format(Locale.GERMAN, "%.4f $", v)
        v < 1 -> String.format(Locale.GERMAN, "%.3f $", v)
        else -> String.format(Locale.GERMAN, "%.2f $", v)
    }
    private fun mio(p: Preis) = "${zahl(p.ein)} / ${zahl(p.cache)} / ${zahl(p.aus)} $ je 1 Mio. Tokens (ein / Cache / aus)"
    private fun zahl(v: Double) = if (v == Math.floor(v)) v.toInt().toString() else String.format(Locale.GERMAN, "%s", v.toString().replace('.', ','))

    /** Stimmen von GPT-Live laut API-Referenz; die beschriebenen haben einen englischen oder portugiesischen Akzent. */
    private val LIVE_STIMMEN = listOf(
        "marin" to "Marin (Standard)", "cedar" to "Cedar", "alloy" to "Alloy", "ash" to "Ash", "ballad" to "Ballad", "coral" to "Coral",
        "echo" to "Echo", "sage" to "Sage", "shimmer" to "Shimmer", "verse" to "Verse", "beacon" to "Beacon (en-PH)", "bossa" to "Bossa (pt-BR)",
        "brise" to "Brise", "cinder" to "Cinder (en-US Süd)", "delta" to "Delta (en-US Süd)", "flitz" to "Flitz", "gleam" to "Gleam (en-US)",
        "harema" to "Harema", "juni" to "Juni", "meridian" to "Meridian (en-US)", "nira" to "Nira", "noeul" to "Noeul", "nuri" to "Nuri",
        "quartz" to "Quartz (en-AU)", "ripple" to "Ripple (en-AU)", "shida" to "Shida", "sillage" to "Sillage", "stone" to "Stone (en-IE)",
        "tempo" to "Tempo (pt-BR)", "vesper" to "Vesper (en-GB)", "willow" to "Willow (en-IE)",
    )

    private val AN_AUS = listOf(Wahl("an", "An"), Wahl("aus", "Aus"))

    val alle: List<Option> = listOf(
        Option("modell", "Live-Gespräch", "Live-Modell",
            "Womit das Live-Gespräch läuft. GPT-Live hört und spricht gleichzeitig und lässt ein Denk-Modell arbeiten; die Realtime-Modelle machen alles selbst. Ein Wechsel gilt ab dem nächsten Gespräch.",
            "gpt-live-1", {
                listOf(
                    Wahl("gpt-live-1", "GPT-Live 1 (neuestes)", "Das neue Sprachmodell von OpenAI (seit 10.09.2026). Hört dauernd zu, man kann jederzeit dazwischenreden. Denken übernimmt das gewählte Denk-Modell.",
                        "0,05 $ pro Minute, sekundengenau, auch bei Stille; plus Denk-Modell"),
                    Wahl("gpt-realtime-2.1", "Realtime 2.1", "Bestes Realtime-Modell mit eigener Denkstufe und Werkzeugen. Kosten wachsen mit der Gesprächslänge.", "Audio " + mio(REALTIME.getValue("gpt-realtime-2.1").audio)),
                    Wahl("gpt-realtime-2.1-mini", "Realtime 2.1 mini", "Schneller und rund dreimal günstiger, folgt Anweisungen und Werkzeugen etwas schlechter.", "Audio " + mio(REALTIME.getValue("gpt-realtime-2.1-mini").audio)),
                    Wahl("gpt-realtime-2", "Realtime 2", "Vorgänger von 2.1, gleicher Preis.", "Audio " + mio(REALTIME.getValue("gpt-realtime-2").audio)),
                    Wahl("gpt-realtime-1.5", "Realtime 1.5", "Ältere Generation ohne Denkstufe, kleiner Kontext (32.000 Tokens).", "Audio " + mio(REALTIME.getValue("gpt-realtime-1.5").audio)),
                    Wahl("gpt-realtime", "Realtime (alt)", "Wird am 20.01.2027 abgeschaltet.", "Audio " + mio(REALTIME.getValue("gpt-realtime").audio)),
                    Wahl("gpt-realtime-mini", "Realtime mini (alt)", "Wird am 20.01.2027 abgeschaltet.", "Audio " + mio(REALTIME.getValue("gpt-realtime-mini").audio)),
                )
            }),

        // ---- Nur GPT-Live ----
        Option("denken", "Live-Gespräch", "Wer denkt (GPT-Live)",
            "GPT-Live spricht nur; nachdenken und nachschlagen lässt es ein Denk-Modell. Beide Wege fragen für deine Daten den normalen Jarvis mit allen Werkzeugen.",
            "openai", {
                listOf(
                    Wahl("openai", "Denk-Modell bei OpenAI", "Ein OpenAI-Modell entscheidet und ruft bei Bedarf Jarvis mit allen Werkzeugen. Am zuverlässigsten. Empfehlung.", "Denk-Modell nach Tokens, über deinen API-Schlüssel"),
                    Wahl("jarvis", "Jarvis direkt (experimentell)", "Jarvis antwortet selbst mit dem Modell aus deinem ChatGPT-Abo. Kein Denk-Modell bei OpenAI, aber Jarvis muss den Auftrag aus der Mitschrift erraten.", "nur 0,05 $ pro Minute"),
                )
            }, gilt = { it.istLive }),
        Option("backend", "Live-Gespräch", "Denk-Modell",
            "Das Modell bei OpenAI, das für GPT-Live nachdenkt und Jarvis fragt. Für Sprache reicht meist das günstigste.",
            "gpt-6-luna", { BACKENDS.map { (id, p) -> Wahl(id, BACKEND_NAMEN.getValue(id), BACKEND_TEXT.getValue(id), mio(p)) } },
            gilt = { it.denktBeiOpenAi }),
        Option("backend_effort", "Live-Gespräch", "Denkstufe des Denk-Modells",
            "Wie lange das Denk-Modell nachdenkt. Denk-Tokens werden wie Ausgabe bezahlt, und jede Stufe macht die Pausen im Gespräch länger.",
            "low", { st -> backendEfforts(st["backend"]).map { e -> EFFORTS.getValue(e).let { w -> Wahl(w.id, w.titel, w.beschreibung, preisEffort(st, e)) } } },
            gilt = { it.denktBeiOpenAi }),
        Option("tarif", "Live-Gespräch", "Tempo-Tarif",
            "Wie schnell OpenAI das Denk-Modell rechnet. Schneller kostet ein Vielfaches.",
            "default", { st ->
                buildList {
                    add(Wahl("default", "Standard", "Normales Tempo, normaler Preis.", "× 1"))
                    add(Wahl("auto", "Projekt-Einstellung", "Folgt dem, was im OpenAI-Projekt eingestellt ist.", "meist × 1"))
                    add(Wahl("priority", "Fast", "Bis zu 2,5-mal schneller, kürzere Denkpausen.", "× 2"))
                    add(Wahl("flex", "Flex", "Halber Preis, aber langsamer; für Gespräche eher ungeeignet.", "× 0,5"))
                    if (st["backend"] in setOf("gpt-6.1-sol", "gpt-6-astra")) add(Wahl("ultrafast", "Ultrafast", "Bis zu 14-mal schneller, nur bei 6.1 Sol und Astra.", "× 6"))
                }
            }, gilt = { it.denktBeiOpenAi }),
        Option("ausfuehrlich", "Live-Gespräch", "Ausführlichkeit des Denk-Modells",
            "Wie viel Text das Denk-Modell an GPT-Live zurückgibt. Weniger ist schneller und billiger; gesprochen wird ohnehin knapp.",
            "low", { listOf(Wahl("low", "Knapp", "Kurze Antworten, Empfehlung.", "am günstigsten"), Wahl("medium", "Mittel", "Etwas mehr Inhalt."), Wahl("high", "Ausführlich", "Lange Antworten.", "mehr Ausgabe-Tokens")) },
            gilt = { it.denktBeiOpenAi }),
        Option("backend_max", "Live-Gespräch", "Höchstlänge einer Denk-Antwort",
            "Deckel für Text und Denk-Tokens je Antwort des Denk-Modells. Schützt vor teuren Ausreißern.",
            "2048", { listOf(Wahl("512", "512 Tokens", "Sehr kurz; bei hoher Denkstufe kann die Antwort abbrechen."), Wahl("1024", "1.024 Tokens"), Wahl("2048", "2.048 Tokens", "Empfehlung."), Wahl("4096", "4.096 Tokens"), Wahl("0", "Ohne Grenze", "", "kein Kostendeckel")) },
            gilt = { it.denktBeiOpenAi }),
        Option("websuche", "Live-Gespräch", "Websuche des Denk-Modells",
            "Das Denk-Modell darf selbst im Internet suchen. Jarvis kann das über seine eigene Recherche ohnehin.",
            "aus", { listOf(Wahl("aus", "Aus", "Empfehlung."), Wahl("an", "An", "", "plus Websuche-Gebühr laut OpenAI-Preisliste")) },
            gilt = { it.denktBeiOpenAi }),
        Option("speichern", "Live-Gespräch", "Gespräch bei OpenAI speichern",
            "OpenAI hebt Mitschnitt und Verlauf 30 Tage auf (zum Nachhören und Fortsetzen). Braucht eine Freigabe im OpenAI-Projekt.",
            "aus", { AN_AUS }, gilt = { it.istLive }),

        // ---- Nur Realtime ----
        Option("rt_effort", "Live-Gespräch", "Denkstufe (Realtime)",
            "Wie lange das Realtime-Modell vor der Antwort nachdenkt. Minimal antwortet nach etwa 1 Sekunde, Hoch nach etwa 2,3 Sekunden.",
            "low", { listOf("minimal", "low", "medium", "high", "xhigh").map { EFFORTS.getValue(it) } },
            gilt = { !it.istLive && it.modell in setOf("gpt-realtime-2.1", "gpt-realtime-2.1-mini", "gpt-realtime-2") }),
        Option("erkennung", "Live-Gespräch", "Wann du fertig bist (Realtime)",
            "Wie das Modell erkennt, dass du ausgeredet hast. „Nach Sinn“ hört auf den Satz, „Nach Stille“ nur auf die Pause.",
            "semantic_auto", {
                listOf(
                    Wahl("semantic_auto", "Nach Sinn, automatisch", "Natürlichstes Verhalten, Empfehlung."),
                    Wahl("semantic_low", "Nach Sinn, geduldig", "Wartet bis zu 8 Sekunden, gut bei Denkpausen."),
                    Wahl("semantic_high", "Nach Sinn, flott", "Antwortet nach spätestens 2 Sekunden."),
                    Wahl("server_500", "Nach Stille, 0,5 s", "Antwortet nach einer halben Sekunde Pause."),
                    Wahl("server_800", "Nach Stille, 0,8 s", "Etwas geduldiger."),
                    Wahl("server_1200", "Nach Stille, 1,2 s", "Fällt dir kaum ins Wort."),
                )
            }, gilt = { !it.istLive }),
        Option("unterbrechen", "Live-Gespräch", "Dazwischenreden (Realtime)",
            "Ob Jarvis sofort verstummt, wenn du anfängst zu sprechen.", "an", { AN_AUS }, gilt = { !it.istLive }),
        Option("rauschfilter", "Live-Gespräch", "Rauschfilter (Realtime)",
            "Filtert Geräusche vor der Erkennung. Fern passt zum Lautsprecherbetrieb des Handys.",
            "far_field", { listOf(Wahl("far_field", "Fern (Lautsprecher)", "Empfehlung."), Wahl("near_field", "Nah (Headset)"), Wahl("aus", "Aus")) },
            gilt = { !it.istLive }),
        Option("tempo", "Live-Gespräch", "Sprechtempo (Realtime)",
            "Wie schnell Jarvis spricht.", "1.0",
            { listOf("0.8", "0.9", "1.0", "1.1", "1.25", "1.5").map { Wahl(it, it.replace('.', ',') + "×", if (it == "1.0") "Normal" else "") } },
            gilt = { !it.istLive }),
        Option("mitschrift", "Live-Gespräch", "Mitschrift deiner Sprache (Realtime)",
            "Welches Modell deine gesprochenen Sätze für den Chatverlauf mitschreibt. GPT-Live schreibt selbst mit.",
            "gpt-transcribe", {
                listOf(
                    Wahl("gpt-transcribe", "GPT Transcribe", "Genau, Empfehlung.", "0,0045 $ pro Minute"),
                    Wahl("gpt-live-transcribe", "GPT Live Transcribe", "Schneller beim Mitschreiben.", "0,017 $ pro Minute"),
                    Wahl("gpt-4o-mini-transcribe", "GPT-4o mini Transcribe (alt)", "Wird am 26.02.2027 abgeschaltet.", "ca. 0,003 $ pro Minute"),
                    Wahl("aus", "Aus", "Deine Sätze erscheinen dann nicht im Chat.", "0 $"),
                )
            }, gilt = { !it.istLive }),
        Option("kuerzen", "Live-Gespräch", "Gedächtnis im langen Gespräch (Realtime)",
            "Realtime schickt den ganzen Verlauf bei jeder Antwort mit; das treibt die Kosten. Kürzen hält sie niedrig.",
            "0.8", {
                listOf(
                    Wahl("0.8", "Kürzen auf 80 %", "Empfehlung: günstiger, der Cache greift besser."),
                    Wahl("0.6", "Kürzen auf 60 %", "Noch günstiger, vergisst früher."),
                    Wahl("auto", "Automatisch", "OpenAI kürzt erst, wenn der Kontext voll ist."),
                    Wahl("disabled", "Nie kürzen", "Fehler, sobald der Kontext voll ist."),
                )
            }, gilt = { !it.istLive }),
        Option("rt_max", "Live-Gespräch", "Höchstlänge einer Antwort (Realtime)",
            "Deckel je gesprochener Antwort; gesprochene Ausgabe ist der teuerste Posten.", "inf",
            { listOf(Wahl("inf", "Ohne Grenze"), Wahl("1024", "1.024 Tokens", "etwa 50 Sekunden Sprache"), Wahl("512", "512 Tokens", "etwa 25 Sekunden Sprache"), Wahl("256", "256 Tokens", "etwa 12 Sekunden Sprache")) },
            gilt = { !it.istLive }),

        // ---- Beide ----
        Option("stimme", "Live-Gespräch", "Stimme",
            "Mit welcher Stimme Jarvis im Live-Gespräch spricht. Deutsch steuert die Anweisung; probiere aus, welche Stimme am besten Deutsch spricht. Gilt ab dem nächsten Gespräch.",
            "marin", { st ->
                if (st.istLive) LIVE_STIMMEN.map { Wahl(it.first, it.second) }
                else LiveGespraech.STIMMEN.map { Wahl(it.first, it.second, if (it.first == "marin" || it.first == "cedar") "Von OpenAI empfohlen." else "") }
            }),
        Option("stille_ende", "Live-Gespräch", "Bei Stille beenden",
            "Beendet das Live-Gespräch, wenn so lange niemand spricht. GPT-Live kostet auch bei Stille weiter.",
            "5", { listOf(Wahl("2", "Nach 2 Minuten"), Wahl("5", "Nach 5 Minuten", "Empfehlung."), Wahl("10", "Nach 10 Minuten"), Wahl("0", "Nie", "", "kostet im Leerlauf weiter")) }),
        Option("verlauf", "Live-Gespräch", "Bisheriges Gespräch mitgeben",
            "Wie viele Nachrichten aus dem offenen Chat das Live-Gespräch am Anfang kennt.",
            "6", { listOf(Wahl("0", "Keine"), Wahl("6", "Die letzten 6", "Empfehlung."), Wahl("12", "Die letzten 12", "", "etwas mehr Eingabe-Tokens")) }),
    )

    private fun preisEffort(st: Stand, effort: String): String {
        val p = BACKENDS[st["backend"]] ?: return ""
        val extra = EFFORT_TOKENS[effort] ?: 0
        if (extra == 0) return "keine Denk-Tokens"
        return "ca. +" + usd(extra * p.aus / 1_000_000 * (TARIF_FAKTOR[st["tarif"]] ?: 1.0)) + " je Antwort"
    }

    /**
     * Geschätzte Kosten für 10 Minuten Gespräch (5 Minuten du, 5 Minuten Jarvis, 10 Wortwechsel), nach dem Rechenweg
     * aus der OpenAI-Kostenhilfe. Realtime als Spanne zwischen gut und gar nicht zwischengespeichert.
     */
    fun schaetzung(st: Stand): String {
        if (st.istLive) {
            val sprache = 10 * LIVE_PRO_MINUTE
            if (!st.denktBeiOpenAi) return "Geschätzt für 10 Minuten: " + usd(sprache) + " (nur Sprache; Jarvis denkt über dein Abo)."
            val p = BACKENDS.getValue(st["backend"])
            val f = TARIF_FAKTOR[st["tarif"]] ?: 1.0
            val denk = (EFFORT_TOKENS[st["backend_effort"]] ?: 0) * 10
            val leicht = (40_000 * p.ein + (5_000 + denk) * p.aus) / 1_000_000 * f
            val schwer = (200_000 * p.ein + (30_000 + denk) * p.aus) / 1_000_000 * f
            return "Geschätzt für 10 Minuten: " + usd(sprache + leicht) + " bis " + usd(sprache + schwer) + " (0,50 $ Sprache + Denk-Modell)."
        }
        val r = REALTIME[st.modell] ?: return ""
        val neu = (3_000 * r.audio.ein + 6_000 * r.audio.aus) / 1_000_000
        val anteil = when (st["kuerzen"]) { "0.6" -> 0.6; "0.8" -> 0.8; else -> 1.0 }
        val historieVoll = 85_500 * anteil * r.audio.ein / 1_000_000
        val historieCache = 85_500 * anteil * r.audio.cache / 1_000_000
        val anweisung = 20_000 * r.text.ein / 1_000_000
        val mit = 5 * (MITSCHRIFT_PRO_MINUTE[st["mitschrift"]] ?: 0.0)
        return "Geschätzt für 10 Minuten: " + usd(neu + historieCache + anweisung / 10 + mit) + " bis " + usd(neu + historieVoll + anweisung + mit) +
            " (je nachdem, wie gut der Cache greift; Denk-Tokens kommen dazu)."
    }

    fun usdText(v: Double) = usd(v)

    /** Fertige Zusammenstellungen für die Schnellwahl; alles Übrige bleibt, wie es ist. */
    class Profil(val id: String, val titel: String, val beschreibung: String, val werte: Map<String, String>)

    val PROFILE = listOf(
        Profil("sparsam", "Sparsam", "GPT-Live mit dem günstigsten Denk-Modell ohne Nachdenken. Am schnellsten.",
            mapOf("modell" to "gpt-live-1", "denken" to "openai", "backend" to "gpt-6-luna", "backend_effort" to "none", "tarif" to "default", "ausfuehrlich" to "low", "websuche" to "aus")),
        Profil("ausgewogen", "Ausgewogen", "GPT-Live mit Luna und kurzem Nachdenken. Empfehlung für den Alltag.",
            mapOf("modell" to "gpt-live-1", "denken" to "openai", "backend" to "gpt-6-luna", "backend_effort" to "low", "tarif" to "default", "ausfuehrlich" to "low", "websuche" to "aus")),
        Profil("klug", "Klug", "GPT-Live mit GPT-6.1 Sol im Fast-Tarif: denkt gründlicher, kostet deutlich mehr.",
            mapOf("modell" to "gpt-live-1", "denken" to "openai", "backend" to "gpt-6.1-sol", "backend_effort" to "medium", "tarif" to "priority", "ausfuehrlich" to "low")),
        Profil("realtime", "Realtime 2.1", "Ein Modell macht alles selbst, mit eigener Denkstufe. Kosten wachsen mit der Gesprächslänge.",
            mapOf("modell" to "gpt-realtime-2.1", "rt_effort" to "low", "erkennung" to "semantic_auto", "kuerzen" to "0.8", "mitschrift" to "gpt-transcribe")),
    )

    /** Welches Profil gerade genau eingestellt ist (null = eigene Mischung). */
    fun aktivesProfil(st: Stand): Profil? = PROFILE.firstOrNull { p -> p.werte.all { (k, v) -> st[k] == v } }

    /** Ein Satz, was gerade eingestellt ist, für die Übersicht. */
    fun kurzfassung(st: Stand): String {
        val m = alle.first { it.id == "modell" }.werte(st).first { it.id == st.modell }.titel
        if (!st.istLive) return "$m · Denkstufe " + (alle.first { it.id == "rt_effort" }.takeIf { it.gilt(st) }?.let { o -> o.werte(st).first { it.id == st["rt_effort"] }.titel } ?: "–") +
            " · Stimme " + st["stimme"]
        if (!st.denktBeiOpenAi) return "$m · Jarvis denkt selbst · Stimme " + st["stimme"]
        val b = BACKEND_NAMEN.getValue(st["backend"])
        val e = EFFORTS.getValue(st["backend_effort"]).titel
        val t = alle.first { it.id == "tarif" }.werte(st).first { it.id == st["tarif"] }.titel
        return "$m · denkt mit $b, Stufe $e, Tarif $t · Stimme " + st["stimme"]
    }
}
