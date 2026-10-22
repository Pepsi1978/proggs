package de.frank.aufgaben.audio

/**
 * Personal vocabulary for Groq Whisper, 1:1 like TerminalVoiceOverlay
 * (PersonalVocabulary.Load, GroqWhisperClient.BuildVocabularyPrompt / IsPromptEcho).
 */
object Woerterbuch {
    const val STANDARD_WOERTERBUCH =
        "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, " +
            "debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, " +
            "Anthropic, bug almanach, Anthrophic"

    private const val MAX_BEGRIFFE = 100
    private const val MAX_PROMPT_ZEICHEN = 350
    private const val ECHO_MARKER = "Begriffe"
    private const val MIN_TEIL_ECHO = 12

    /** Splits at newline, comma and semicolon; drops empty and '#' entries and case-insensitive duplicates. */
    fun begriffe(text: String): List<String> {
        val gesehen = HashSet<String>()
        return text.split('\n', '\r', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && gesehen.add(it.lowercase()) }
            .take(MAX_BEGRIFFE)
    }

    /** Vocabulary as a natural sentence; Whisper copies the spellings from it. */
    fun prompt(begriffe: List<String>): String? {
        if (begriffe.isEmpty()) return null
        val sb = StringBuilder("$ECHO_MARKER: ")
        var added = 0
        for (w in begriffe) {
            // Skip entries that no longer fit instead of stopping: the remaining ones still count.
            if (sb.length + w.length + 2 > MAX_PROMPT_ZEICHEN) continue
            if (added++ > 0) sb.append(", ")
            sb.append(w)
        }
        return if (added == 0) null else sb.append('.').toString()
    }

    /** Prompt for the request, or null when the switch is off or there are no terms. */
    fun prompt(an: Boolean, text: String): String? = if (an) prompt(begriffe(text)) else null

    /**
     * Whisper likes to output the prompt itself on silence/murmur, often only its end
     * ("Claude Code, OpenAI, Anthropic, ..."). Echo = the text starts with the marker ("Begriffe ...")
     * and the rest is contained in the prompt, or the text (at least 12 chars) is a part of the prompt.
     * A real dictation that only repeats vocabulary words in prompt order is just retried once without prompt.
     */
    fun istPromptEcho(text: String, prompt: String?): Boolean {
        if (prompt.isNullOrEmpty() || text.isBlank()) return false
        fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
        val t = norm(text)
        val p = norm(prompt)
        if (t.isEmpty()) return false
        val marker = norm(ECHO_MARKER)
        if (t.startsWith(marker) && p.contains(t.substring(marker.length))) return true
        return t.length >= MIN_TEIL_ECHO && p.contains(t)
    }
}
