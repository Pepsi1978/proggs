package de.frank.cortex.audio

/**
 * Whisper-Modellwahl und persönliches Wörterbuch für die Groq-Transkription.
 * Vorbild: TerminalVoiceOverlay (GroqModelSetting, PersonalVocabulary, BuildVocabularyPrompt, IsPromptEcho).
 */
object WhisperWoerterbuch {
    const val MODELL_TURBO = "whisper-large-v3-turbo"
    const val MODELL_LARGE_V3 = "whisper-large-v3"

    /** Vorbelegung des persönlichen Wörterbuchs (Stand TerminalVoiceOverlay). */
    const val STANDARD_WOERTERBUCH =
        "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

    private const val MAX_PROMPT_ZEICHEN = 350
    private const val MAX_BEGRIFFE = 100
    private const val ECHO_MARKER = "Begriffe"

    /** Nach einer large-v3-Sperre so lange direkt Turbo nehmen, statt jedes Diktat doppelt hochzuladen. */
    private const val SPERRE_MS = 30 * 60 * 1000L

    @Volatile
    private var gesperrtBisMs = 0L

    /** Gewähltes Modell für diese Aufnahme; large-v3 wird nach einer Sperre vorübergehend durch Turbo ersetzt. */
    fun aufloesen(modell: String): String =
        if (modell == MODELL_LARGE_V3 && System.currentTimeMillis() < gesperrtBisMs) MODELL_TURBO
        else if (modell == MODELL_LARGE_V3) MODELL_LARGE_V3 else MODELL_TURBO

    fun sperreLargeV3() {
        gesperrtBisMs = System.currentTimeMillis() + SPERRE_MS
    }

    /** Groq-Antwort „Modell für die Organisation gesperrt“. */
    fun istModellGesperrt(body: String): Boolean =
        body.contains("model_permission_blocked_org") || body.contains("blocked at the organization level")

    /** Begriffe aus dem Wörterbuch-Text: getrennt an Zeilenumbruch, Komma, Semikolon; ohne Leeres, #-Kommentare und Duplikate. */
    fun begriffe(text: String): List<String> =
        text.split('\n', '\r', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .distinctBy { it.lowercase() }
            .take(MAX_BEGRIFFE)

    /** Wörterbuch als natürlicher Satz („Begriffe: A, B.“); ein zu langer Begriff wird übersprungen. Keine Begriffe → null. */
    fun prompt(begriffe: List<String>): String? {
        if (begriffe.isEmpty()) return null
        val sb = StringBuilder("$ECHO_MARKER: ")
        var hinzu = 0
        for (w in begriffe) {
            if (sb.length + w.length + 2 > MAX_PROMPT_ZEICHEN) continue
            if (hinzu++ > 0) sb.append(", ")
            sb.append(w)
        }
        return if (hinzu == 0) null else sb.append('.').toString()
    }

    /**
     * Echo = Whisper wiederholt den Prompt ab seinem Anfang („Begriffe: …“), ganz oder teilweise.
     * Ein Diktat, das nur Wörterbuch-Wörter enthält, beginnt nicht mit dem Marker und bleibt erhalten.
     */
    fun istPromptEcho(text: String, prompt: String?): Boolean {
        if (prompt.isNullOrEmpty() || text.isBlank()) return false
        fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
        val t = norm(text)
        val marker = norm(ECHO_MARKER)
        return t.startsWith(marker) && norm(prompt).contains(t.substring(marker.length))
    }
}
