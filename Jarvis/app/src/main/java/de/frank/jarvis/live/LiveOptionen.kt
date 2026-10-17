package de.frank.jarvis.live

import de.frank.jarvis.data.settings.SecureSettings
import org.json.JSONObject
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
            val w = migriert(id, roh(id)).ifBlank { o.standard }
            val erlaubt = o.werte(this).map { it.id }
            return if (w in erlaubt || erlaubt.isEmpty()) w else if (o.standard in erlaubt) o.standard else erlaubt.first()
        }
        fun setze(id: String, wert: String) = s.setzeLiveOption(id, wert)

        /**
         * Alte Werte auf die heutigen Stufen: Die Realtime-Höchstlänge kannte früher 256, 512, 1024 und „Ohne Grenze“.
         * Alles bis 1024 wird 1.000, „Ohne Grenze“ wird 4.000; der neue Wert wird einmal gespeichert.
         */
        private fun migriert(id: String, w: String): String {
            if (id != "rt_max" || w.isBlank() || ueber.containsKey(id)) return w
            val neu = when (val n = w.toIntOrNull()) {
                null -> if (w == "inf") "4000" else w
                in Int.MIN_VALUE..1024 -> "1000"
                in 1025..4096 -> if (w in setOf("1000", "2000", "3000", "4000")) w else ((n + 500) / 1000 * 1000).coerceIn(1000, 4000).toString()
                else -> "4000"
            }
            if (neu != w) s.setzeLiveOption(id, neu)
            return neu
        }
        val modell get() = get("modell")
        val istLive get() = modell.startsWith("gpt-live")
        val istGemini get() = modell.startsWith("gemini")
        val istRealtime get() = !istLive && !istGemini
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
    /**
     * Gemini-Live-Modelle (Developer API, Stand 17.10.2026, ai.google.dev/gemini-api/docs/pricing): Audio- und Textpreise je
     * 1 Mio. Tokens, Denk-Tokens wie Text-Ausgabe. Kein Cache: Gemini rechnet bei jeder Antwort den ganzen Verlauf neu als Eingabe.
     * Mit kostenlosem Schlüssel (Projekt ohne Abrechnung) kostet alles nichts; Google nutzt die Daten dann zum Training.
     */
    class GeminiPreis(val audioEin: Double, val textEin: Double, val audioAus: Double, val textAus: Double)
    val GEMINI = linkedMapOf(
        "gemini-3.8-live" to GeminiPreis(3.00, 0.75, 12.00, 4.50),
        "gemini-3.8-live-extended-thinking" to GeminiPreis(3.00, 0.75, 12.00, 4.50),
        "gemini-3.1-flash-live-preview" to GeminiPreis(3.00, 0.75, 12.00, 4.50),
    )

    /** Kosten einer Antwort aus `usageMetadata` (Eingabe und Ausgabe nach Audio und Text getrennt); null = Modell ohne Preis. */
    fun geminiKosten(modell: String, u: JSONObject): Double? {
        val p = GEMINI[modell] ?: return null
        fun anteil(feld: String, art: String): Int {
            val a = u.optJSONArray(feld) ?: return 0
            return (0 until a.length()).sumOf { i -> a.optJSONObject(i)?.takeIf { it.optString("modality") == art }?.optInt("tokenCount") ?: 0 }
        }
        val ein = u.optInt("promptTokenCount")
        val aus = u.optInt("responseTokenCount").let { if (it == 0) u.optInt("candidatesTokenCount") else it }
        val audioEin = anteil("promptTokensDetails", "AUDIO")
        val audioAus = anteil("responseTokensDetails", "AUDIO").let { if (it == 0) anteil("candidatesTokensDetails", "AUDIO") else it }
        val denken = u.optInt("thoughtsTokenCount")
        // Jarvis' Werkzeug-Ergebnisse meldet Gemini getrennt; sie sind Text-Eingabe.
        val werkzeug = u.optInt("toolUsePromptTokenCount")
        // Fehlen die Einzelheiten, zählt alles als Audio: lieber etwas zu hoch als zu niedrig geschätzt.
        val audioE = if (audioEin == 0 && ein > 0 && u.optJSONArray("promptTokensDetails") == null) ein else audioEin
        val audioA = if (audioAus == 0 && aus > 0 && u.optJSONArray("responseTokensDetails") == null && u.optJSONArray("candidatesTokensDetails") == null) aus else audioAus
        return (audioE * p.audioEin + ((ein - audioE).coerceAtLeast(0) + werkzeug) * p.textEin + audioA * p.audioAus + ((aus - audioA).coerceAtLeast(0) + denken) * p.textAus) / 1_000_000
    }

    /** Denkstufe je Modell: 3.8 Live hat keine (thinkingConfig muss fehlen), Extended Thinking low bis high, 3.1 Flash minimal bis high. */
    private fun geminiStufen(modell: String): List<String> = when (modell) {
        "gemini-3.8-live-extended-thinking" -> listOf("low", "medium", "high")
        "gemini-3.1-flash-live-preview" -> listOf("minimal", "low", "medium", "high")
        else -> emptyList()
    }
    fun geminiDenken(st: Stand): JSONObject? =
        // Googles Roh-WebSocket-Beispiel schreibt die Stufe groß ("LOW"), nur das SDK nimmt sie klein.
        if (geminiStufen(st.modell).isEmpty()) null else JSONObject().put("thinkingLevel", st["g_denken"].uppercase(Locale.ROOT))

    /** Ab so vielen Tokens kürzt Gemini den Verlauf (Sliding Window); null = Googles Vorgabe. */
    fun geminiKuerzenAb(st: Stand): Int? = st["g_kuerzen"].toIntOrNull()

    /** Werkzeugaufrufe: Extended Thinking nur nebenbei (NON_BLOCKING), 3.1 Flash nur wartend, 3.8 Live nach Einstellung. */
    fun geminiNebenbei(st: Stand) = when (st.modell) {
        "gemini-3.8-live-extended-thinking" -> true
        "gemini-3.1-flash-live-preview" -> false
        else -> st["g_werkzeug"] == "nebenbei"
    }

    /** Die 30 Stimmen der Gemini Live API (Name, Googles Charakterwort, Geschlecht nach den gleichnamigen Chirp-3-HD-Stimmen). */
    private val GEMINI_STIMMEN = listOf(
        "Charon" to "sachlich, männlich", "Puck" to "munter, männlich", "Kore" to "bestimmt, weiblich", "Zephyr" to "hell, weiblich",
        "Fenrir" to "aufgeweckt, männlich", "Leda" to "jugendlich, weiblich", "Orus" to "bestimmt, männlich", "Aoede" to "locker, weiblich",
        "Callirrhoe" to "gelassen, weiblich", "Autonoe" to "klar, weiblich", "Enceladus" to "hauchig, männlich", "Iapetus" to "deutlich, männlich",
        "Umbriel" to "gelassen, männlich", "Algieba" to "weich, männlich", "Despina" to "weich, weiblich", "Erinome" to "deutlich, weiblich",
        "Algenib" to "rau, männlich", "Rasalgethi" to "informativ, männlich", "Laomedeia" to "beschwingt, weiblich", "Achernar" to "sanft, weiblich",
        "Alnilam" to "fest, männlich", "Schedar" to "ausgeglichen, männlich", "Gacrux" to "reif, weiblich", "Pulcherrima" to "direkt, weiblich",
        "Achird" to "freundlich, männlich", "Zubenelgenubi" to "ungezwungen, männlich", "Vindemiatrix" to "behutsam, weiblich", "Sadachbia" to "lebhaft, männlich",
        "Sadaltager" to "kenntnisreich, männlich", "Sulafat" to "warm, weiblich",
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
    private const val GEMINI_PREIS_TEXT = "Audio 3 / 12 $, Text 0,75 / 4,50 $ je 1 Mio. Tokens (ein / aus), etwa 0,005 $ je Minute hören und 0,018 $ je Minute sprechen; mit kostenlosem Schlüssel 0 $"

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
                    Wahl("gemini-3.8-live", "Gemini 3.8 Live", "Googles aktuelles Live-Modell (seit 15.09.2026): hört und spricht gleichzeitig, denkt mit fester, kurzer Verzögerung selbst. " +
                        "Braucht einen Gemini-Schlüssel aus AI Studio.", GEMINI_PREIS_TEXT),
                    Wahl("gemini-3.8-live-extended-thinking", "Gemini 3.8 Live Extended Thinking", "Wie 3.8 Live, denkt aber im Hintergrund gründlicher nach (Stufe einstellbar) " +
                        "und überbrückt die Zeit mit kurzen Sätzen. Werkzeuge laufen nebenher.", GEMINI_PREIS_TEXT + "; Denk-Tokens wie Text-Ausgabe"),
                    Wahl("gemini-3.1-flash-live-preview", "Gemini 3.1 Flash Live", "Das Flash-Live-Modell (Vorschau, Google nennt eine Abschaltung frühestens am 17.11.2026). " +
                        "Denkstufe Minimal bis Hoch; „Flash Live High“ = Denkstufe Hoch.", GEMINI_PREIS_TEXT),
                )
            }),

        // ---- Nur Gemini ----
        Option("g_schluessel", "Live-Gespräch", "Gemini-Schlüssel",
            "Welcher deiner Gemini-Schlüssel das Gespräch bezahlt. Ob ein Schlüssel kostenlos oder bezahlt ist, hängt an seinem Projekt in AI Studio (mit oder ohne Abrechnung).",
            "auto", {
                listOf(
                    Wahl("auto", "Erst kostenlos, dann bezahlt", "Nutzt den kostenlosen Schlüssel; ist sein Kontingent erschöpft, spricht Jarvis mit dem bezahlten weiter. Empfehlung.", "meist 0 $"),
                    Wahl("frei", "Nur kostenlos", "Endet, wenn das kostenlose Kontingent erschöpft ist. Google nutzt die Gespräche zum Training.", "0 $"),
                    Wahl("bezahlt", "Nur bezahlt", "Immer der Schlüssel mit Abrechnung; Google nutzt die Gespräche nicht zum Training.", GEMINI_PREIS_TEXT),
                )
            }, gilt = { it.istGemini }),
        Option("g_denken", "Live-Gespräch", "Denkstufe (Gemini)",
            "Wie gründlich Gemini vor der Antwort nachdenkt. Mehr Denken heißt längere Pausen und mehr Ausgabe-Tokens. 3.8 Live hat keine Stufe, es denkt immer kurz.",
            "low", { st ->
                geminiStufen(st.modell).map { e ->
                    when (e) {
                        "minimal" -> Wahl("minimal", "Minimal", "Antwortet am schnellsten, Googles Standard bei Flash Live.", "kaum Denk-Tokens")
                        "low" -> Wahl("low", "Niedrig", "Kurz nachdenken, gut fürs Gespräch. Empfehlung.", "wenige Denk-Tokens")
                        "medium" -> Wahl("medium", "Mittel", "Merklich längere Pausen.", "mehr Denk-Tokens")
                        else -> Wahl("high", "Hoch", "Für schwierige Fragen; deutlich längere Pausen. Bei Flash Live ist das „Flash Live High“.", "die meisten Denk-Tokens")
                    }
                }
            }, gilt = { it.istGemini && geminiStufen(it.modell).isNotEmpty() }),
        Option("g_werkzeug", "Live-Gespräch", "Jarvis fragen (Gemini 3.8 Live)",
            "Ob Gemini weiterredet, während Jarvis nachschlägt. Extended Thinking macht das immer, Flash Live nie.",
            "warten", {
                listOf(
                    Wahl("warten", "Warten, bis Jarvis antwortet", "Gemini sagt kurz „Moment“ und antwortet mit dem Ergebnis. Am verlässlichsten. Empfehlung."),
                    Wahl("nebenbei", "Nebenbei weiterreden", "Gemini bleibt ansprechbar und baut das Ergebnis ein, sobald es da ist."),
                )
            }, gilt = { it.modell == "gemini-3.8-live" }),
        Option("g_melden", "Live-Gespräch", "Ergebnis von Jarvis einbringen (Gemini)",
            "Wann Gemini ein Ergebnis von Jarvis vorträgt, das nebenbei kam.",
            "WHEN_IDLE", {
                listOf(
                    Wahl("WHEN_IDLE", "Wenn gerade niemand spricht", "Unterbricht nichts. Empfehlung."),
                    Wahl("INTERRUPT", "Sofort", "Gemini bricht ab, was es gerade sagt, und meldet das Ergebnis."),
                )
            }, gilt = { it.modell == "gemini-3.8-live" && it["g_werkzeug"] == "nebenbei" }),
        Option("g_start", "Live-Gespräch", "Wann du anfängst (Gemini)",
            "Wie schnell Gemini merkt, dass du sprichst. Empfindlich reagiert früh, kann aber auf Geräusche anspringen.",
            "niedrig", { listOf(Wahl("niedrig", "Gelassen", "Springt auf Geräusche seltener an. Empfehlung am Lautsprecher und im Auto."), Wahl("hoch", "Empfindlich", "Reagiert schon auf leises Ansprechen.")) },
            gilt = { it.istGemini }),
        Option("g_ende", "Live-Gespräch", "Wann du fertig bist (Gemini)",
            "Wie schnell Gemini nach deinem Satzende antwortet.",
            "niedrig", { listOf(Wahl("niedrig", "Geduldig", "Lässt dir Denkpausen. Empfehlung."), Wahl("hoch", "Flott", "Antwortet schnell, fällt dir eher ins Wort.")) },
            gilt = { it.istGemini }),
        Option("g_stille", "Live-Gespräch", "Pause bis zur Antwort (Gemini)",
            "So lange musst du schweigen, bis Gemini deinen Satz als beendet ansieht.",
            "800", { listOf(Wahl("300", "0,3 s"), Wahl("500", "0,5 s"), Wahl("800", "0,8 s", "Empfehlung."), Wahl("1200", "1,2 s"), Wahl("2000", "2 s")) },
            gilt = { it.istGemini }),
        Option("g_mitschrift", "Live-Gespräch", "Mitschrift deiner Sprache (Gemini)",
            "Ob deine gesprochenen Sätze im Chat erscheinen. Gemini schreibt selbst mit, ohne Zusatzpreis.",
            "an", { AN_AUS }, gilt = { it.istGemini }),
        Option("g_kuerzen", "Live-Gespräch", "Gedächtnis im langen Gespräch (Gemini)",
            "Gemini rechnet bei jeder Antwort den ganzen bisherigen Verlauf neu als Eingabe ab, ohne Cache. Kürzen hält die Kosten flach und erlaubt Gespräche über 15 Minuten.",
            "16000", {
                listOf(
                    Wahl("8000", "Kürzen ab 8.000 Tokens", "Am günstigsten, vergisst nach gut 5 Minuten Gespräch.", "höchstens ca. 0,024 $ Eingabe je Antwort"),
                    Wahl("16000", "Kürzen ab 16.000 Tokens", "Etwa 10 Minuten Gedächtnis. Empfehlung.", "höchstens ca. 0,048 $ Eingabe je Antwort"),
                    Wahl("32000", "Kürzen ab 32.000 Tokens", "Etwa 20 Minuten Gedächtnis.", "höchstens ca. 0,096 $ Eingabe je Antwort"),
                    Wahl("auto", "Googles Vorgabe", "Kürzt erst, wenn der Kontext fast voll ist.", "kann je Antwort deutlich teurer werden"),
                )
            }, gilt = { it.istGemini }),
        Option("g_suche", "Live-Gespräch", "Google-Suche (Gemini)",
            "Gemini darf selbst bei Google suchen. Jarvis kann über seine eigene Recherche ohnehin nachschlagen.",
            "aus", { listOf(Wahl("aus", "Aus", "Empfehlung."), Wahl("an", "An", "", "Suchanfragen laut Google-Preisliste, im kostenlosen Kontingent begrenzt")) },
            gilt = { it.istGemini }),
        Option("g_temperatur", "Live-Gespräch", "Kreativität (Gemini)",
            "Wie frei Gemini formuliert. Google empfiehlt für die 3er-Modelle die Vorgabe.",
            "standard", { listOf(Wahl("standard", "Vorgabe", "Empfehlung."), Wahl("0.7", "Ruhiger"), Wahl("1.3", "Lebhafter")) },
            gilt = { it.istGemini }),
        Option("g_max", "Live-Gespräch", "Höchstlänge einer Antwort (Gemini)",
            "Deckel je Antwort; gesprochene Ausgabe ist der teuerste Posten (etwa 25 Tokens je Sekunde).",
            "0", { listOf(Wahl("1000", "1.000 Tokens", "etwa 40 Sekunden Sprache"), Wahl("2000", "2.000 Tokens", "etwa 80 Sekunden Sprache"),
                Wahl("4000", "4.000 Tokens", "etwa 2,5 Minuten Sprache"), Wahl("0", "Ohne Grenze", "Empfehlung bei Denkstufen: Denk-Tokens zählen mit.")) },
            gilt = { it.istGemini }),

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
            gilt = { it.istRealtime && it.modell in setOf("gpt-realtime-2.1", "gpt-realtime-2.1-mini", "gpt-realtime-2") }),
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
            }, gilt = { it.istRealtime }),
        Option("unterbrechen", "Live-Gespräch", "Dazwischenreden (Realtime und Gemini)",
            "Ob Jarvis sofort verstummt, wenn du anfängst zu sprechen.", "an", { AN_AUS }, gilt = { !it.istLive }),
        Option("rauschfilter", "Live-Gespräch", "Rauschfilter (Realtime)",
            "Filtert Geräusche vor der Erkennung. Fern passt zum Lautsprecherbetrieb des Handys.",
            "far_field", { listOf(Wahl("far_field", "Fern (Lautsprecher)", "Empfehlung."), Wahl("near_field", "Nah (Headset)"), Wahl("aus", "Aus")) },
            gilt = { it.istRealtime }),
        Option("tempo", "Live-Gespräch", "Sprechtempo (Realtime)",
            "Wie schnell Jarvis spricht.", "1.0",
            { listOf("0.8", "0.9", "1.0", "1.1", "1.25", "1.5").map { Wahl(it, it.replace('.', ',') + "×", if (it == "1.0") "Normal" else "") } },
            gilt = { it.istRealtime }),
        Option("mitschrift", "Live-Gespräch", "Mitschrift deiner Sprache (Realtime)",
            "Welches Modell deine gesprochenen Sätze für den Chatverlauf mitschreibt. GPT-Live schreibt selbst mit.",
            "gpt-transcribe", {
                listOf(
                    Wahl("gpt-transcribe", "GPT Transcribe", "Genau, Empfehlung.", "0,0045 $ pro Minute"),
                    Wahl("gpt-live-transcribe", "GPT Live Transcribe", "Schneller beim Mitschreiben.", "0,017 $ pro Minute"),
                    Wahl("gpt-4o-mini-transcribe", "GPT-4o mini Transcribe (alt)", "Wird am 26.02.2027 abgeschaltet.", "ca. 0,003 $ pro Minute"),
                    Wahl("aus", "Aus", "Deine Sätze erscheinen dann nicht im Chat.", "0 $"),
                )
            }, gilt = { it.istRealtime }),
        Option("kuerzen", "Live-Gespräch", "Gedächtnis im langen Gespräch (Realtime)",
            "Realtime schickt den ganzen Verlauf bei jeder Antwort mit; das treibt die Kosten. Kürzen hält sie niedrig.",
            "0.8", {
                listOf(
                    Wahl("0.8", "Kürzen auf 80 %", "Empfehlung: günstiger, der Cache greift besser."),
                    Wahl("0.6", "Kürzen auf 60 %", "Noch günstiger, vergisst früher."),
                    Wahl("auto", "Automatisch", "OpenAI kürzt erst, wenn der Kontext voll ist."),
                    Wahl("disabled", "Nie kürzen", "Fehler, sobald der Kontext voll ist."),
                )
            }, gilt = { it.istRealtime }),
        Option("rt_max", "Live-Gespräch", "Höchstlänge einer Antwort (Realtime)",
            "Deckel je gesprochener Antwort; gesprochene Ausgabe ist der teuerste Posten.", "4000",
            { listOf(Wahl("1000", "1.000 Tokens", "etwa 50 Sekunden Sprache"), Wahl("2000", "2.000 Tokens", "etwa 1,5 Minuten Sprache"),
                Wahl("3000", "3.000 Tokens", "etwa 2,5 Minuten Sprache"), Wahl("4000", "4.000 Tokens", "etwa 3 Minuten Sprache")) },
            gilt = { it.istRealtime }),

        // ---- Beide ----
        Option("audioweg", "Live-Gespräch", "Tonweg (Bluetooth)",
            "Wie Jarvis' Stimme im Auto oder Kopfhörer ankommt. Bluetooth kann nicht beides zugleich: Medienqualität (A2DP) gibt es nur mit dem Handy-Mikrofon; " +
                "wer das Mikrofon des Autos oder Headsets nutzt, bekommt das Telefonprofil (HFP) mit Telefonklang. Gilt ab dem nächsten Gespräch.",
            "medien", {
                listOf(
                    Wahl("medien", "Medienton, Handy-Mikrofon", "Stimme in voller Medienqualität über Bluetooth-Medien oder Kopfhörer, kein Telefonmodus. " +
                        "Gesprochen wird ins Handy. Ohne Bluetooth und Kopfhörer ebenfalls Medienton über den Handy-Lautsprecher. Empfehlung."),
                    Wahl("freisprechen", "Freisprechen, Auto-Mikrofon", "Telefonprofil wie bei einem Anruf: Das Mikrofon im Auto oder Headset hört, die Stimme klingt " +
                        "aber nach Telefon. Ohne Headset oder Freisprechanlage trotzdem Medienton am Handy-Lautsprecher. Rückfallweg, falls der Medienton nicht klappt."),
                )
            }),
        Option("echo_sperre", "Live-Gespräch", "Echosperre im Medienton",
            "Im Medienton hört das Handy-Mikrofon die Lautsprecher mit. Ob das Handy dieses Echo herausrechnet, hängt vom Gerät ab; sonst unterbricht Jarvis sich selbst.",
            "an", {
                listOf(
                    Wahl("an", "An", "Während Jarvis spricht, ist das Mikrofon zu. Kein Selbstunterbrechen; statt Dazwischenreden tippst du auf die orange Blase. Empfehlung im Auto."),
                    Wahl("aus", "Aus", "Mikrofon immer offen, Dazwischenreden geht. Gut mit Kopfhörern; im Auto nur, wenn Jarvis sich nicht selbst unterbricht."),
                )
            }),
        Option("stimme", "Live-Gespräch", "Stimme",
            "Mit welcher Stimme Jarvis im Live-Gespräch spricht. Deutsch steuert die Anweisung; probiere aus, welche Stimme am besten Deutsch spricht. Gilt ab dem nächsten Gespräch.",
            "marin", { st ->
                if (st.istGemini) GEMINI_STIMMEN.map { Wahl(it.first, it.first + " · " + it.second) }
                else if (st.istLive) LIVE_STIMMEN.map { Wahl(it.first, it.second) }
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
        if (st.istGemini) {
            val p = GEMINI[st.modell] ?: return ""
            // 25 Tokens je Sekunde Audio: 5 Minuten du = 7.500 Tokens ein, 5 Minuten Jarvis = 7.500 Tokens aus, 10 Wortwechsel.
            // Gemini liest bei jeder Antwort den Verlauf neu (bis zur Kürzungsgrenze); die Anweisung (~2.000 Tokens) jedes Mal.
            val grenze = st["g_kuerzen"].toIntOrNull() ?: 100_000
            val verlauf = (1..10).sumOf { minOf(it * 1_500, grenze) }
            val denk = (EFFORT_TOKENS[st["g_denken"]] ?: 0) * 10 * if (geminiStufen(st.modell).isEmpty()) 0 else 1
            val fest = (7_500 * p.audioAus + 20_000 * p.textEin + denk * p.textAus) / 1_000_000
            val gering = fest + 7_500 * p.audioEin / 1_000_000
            val voll = fest + verlauf * p.audioEin / 1_000_000
            val frei = when (st["g_schluessel"]) { "bezahlt" -> ""; "frei" -> " Mit dem kostenlosen Schlüssel: 0 $."; else -> " Solange das kostenlose Kontingent reicht: 0 $." }
            return "Geschätzt für 10 Minuten: " + usd(gering) + " bis " + usd(voll) + " mit bezahltem Schlüssel (je nachdem, wie viel Verlauf Gemini neu liest)." + frei
        }
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
        Profil("gemini38", "Gemini 3.8 Live", "Google: hört und spricht gleichzeitig, mit kostenlosem Kontingent. Braucht einen Gemini-Schlüssel.",
            mapOf("modell" to "gemini-3.8-live", "g_werkzeug" to "warten", "g_kuerzen" to "16000", "g_schluessel" to "auto")),
        Profil("gemini38x", "Gemini 3.8 Live Extended Thinking", "Google: denkt im Hintergrund gründlicher nach, Stufe Niedrig.",
            mapOf("modell" to "gemini-3.8-live-extended-thinking", "g_denken" to "low", "g_kuerzen" to "16000", "g_schluessel" to "auto")),
        Profil("gemini31", "Gemini 3.1 Flash Live", "Google Flash Live mit minimaler Denkstufe: am schnellsten. Vorschau, endet frühestens am 17.11.2026.",
            mapOf("modell" to "gemini-3.1-flash-live-preview", "g_denken" to "minimal", "g_kuerzen" to "16000", "g_schluessel" to "auto")),
        Profil("gemini31hoch", "Gemini 3.1 Flash Live High", "Flash Live mit hoher Denkstufe: gründlicher, längere Pausen.",
            mapOf("modell" to "gemini-3.1-flash-live-preview", "g_denken" to "high", "g_kuerzen" to "16000", "g_schluessel" to "auto")),
    )

    /** Welches Profil gerade genau eingestellt ist (null = eigene Mischung). */
    fun aktivesProfil(st: Stand): Profil? = PROFILE.firstOrNull { p -> p.werte.all { (k, v) -> st[k] == v } }

    /** Ein Satz, was gerade eingestellt ist, für die Übersicht. */
    fun kurzfassung(st: Stand): String {
        val m = alle.first { it.id == "modell" }.werte(st).first { it.id == st.modell }.titel
        if (st.istGemini) return m + (if (geminiStufen(st.modell).isEmpty()) "" else " · Denkstufe " + alle.first { it.id == "g_denken" }.werte(st).first { it.id == st["g_denken"] }.titel) +
            " · Stimme " + st["stimme"] + " · " + alle.first { it.id == "g_schluessel" }.werte(st).first { it.id == st["g_schluessel"] }.titel
        if (!st.istLive) return "$m · Denkstufe " + (alle.first { it.id == "rt_effort" }.takeIf { it.gilt(st) }?.let { o -> o.werte(st).first { it.id == st["rt_effort"] }.titel } ?: "–") +
            " · Stimme " + st["stimme"]
        if (!st.denktBeiOpenAi) return "$m · Jarvis denkt selbst · Stimme " + st["stimme"]
        val b = BACKEND_NAMEN.getValue(st["backend"])
        val e = EFFORTS.getValue(st["backend_effort"]).titel
        val t = alle.first { it.id == "tarif" }.werte(st).first { it.id == st["tarif"] }.titel
        return "$m · denkt mit $b, Stufe $e, Tarif $t · Stimme " + st["stimme"]
    }
}
