package com.bestjournal.app.data.remote.groq

import android.content.SharedPreferences

/**
 * Whisper model choice + personal vocabulary for every Groq transcription.
 * Logic mirrors TerminalVoiceOverlay-Windows (GroqModelSetting, PersonalVocabulary,
 * GroqWhisperClient.BuildVocabularyPrompt / IsPromptEcho).
 */
object GroqWhisperSettings {
    const val MODEL_TURBO = "whisper-large-v3-turbo"
    const val MODEL_LARGE_V3 = "whisper-large-v3"

    const val PREF_WHISPER_MODEL = "groq_whisper_model"
    const val PREF_VOCABULARY_ENABLED = "groq_vocabulary_enabled"
    const val PREF_VOCABULARY = "groq_vocabulary"

    const val STANDARD_WOERTERBUCH =
        "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, " +
            "Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, " +
            "Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

    private const val MAX_WORDS = 100
    private const val MAX_PROMPT_CHARS = 350
    private const val ECHO_MARKER = "Begriffe"
    private const val BLOCK_DURATION_MS = 30 * 60 * 1000L

    // After a "model blocked for the organization" answer use Turbo directly for 30 minutes.
    @Volatile private var blockedUntilMs = 0L

    fun storedModel(prefs: SharedPreferences): String =
        if (prefs.getString(PREF_WHISPER_MODEL, MODEL_TURBO) == MODEL_LARGE_V3) MODEL_LARGE_V3
        else MODEL_TURBO

    fun saveModel(prefs: SharedPreferences, model: String) {
        prefs.edit()
            .putString(PREF_WHISPER_MODEL, if (model == MODEL_LARGE_V3) MODEL_LARGE_V3 else MODEL_TURBO)
            .apply()
        blockedUntilMs = 0L
    }

    /** Model for this recording; large-v3 is temporarily replaced by Turbo after a block. */
    fun resolveModel(prefs: SharedPreferences): String {
        val model = storedModel(prefs)
        return if (model == MODEL_LARGE_V3 && System.currentTimeMillis() < blockedUntilMs) MODEL_TURBO
        else model
    }

    fun markBlocked() {
        blockedUntilMs = System.currentTimeMillis() + BLOCK_DURATION_MS
    }

    /** Groq answer "model blocked for the organization" (usually 403, sometimes 400). */
    fun isBlockedError(body: String?): Boolean =
        body != null &&
            (body.contains("model_permission_blocked_org") ||
                body.contains("blocked at the organization level"))

    fun isVocabularyEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_VOCABULARY_ENABLED, true)

    fun setVocabularyEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(PREF_VOCABULARY_ENABLED, enabled).apply()
    }

    fun vocabularyText(prefs: SharedPreferences): String =
        prefs.getString(PREF_VOCABULARY, null) ?: STANDARD_WOERTERBUCH

    fun saveVocabularyText(prefs: SharedPreferences, text: String) {
        prefs.edit().putString(PREF_VOCABULARY, text).apply()
    }

    fun parseBegriffe(raw: String): List<String> {
        val seen = HashSet<String>()
        return raw.split('\n', '\r', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .filter { seen.add(it.lowercase()) }
            .take(MAX_WORDS)
    }

    /** Vocabulary as a natural sentence; Whisper adopts the spellings from it. */
    fun buildPrompt(words: List<String>): String? {
        if (words.isEmpty()) return null
        val sb = StringBuilder("$ECHO_MARKER: ")
        var added = 0
        for (w in words) {
            // Skip entries that no longer fit instead of stopping: the remaining terms still count.
            if (sb.length + w.length + 2 > MAX_PROMPT_CHARS) continue
            if (added++ > 0) sb.append(", ")
            sb.append(w)
        }
        return if (added == 0) null else sb.append('.').toString()
    }

    /** Prompt for this recording, or null when the vocabulary is switched off or empty. */
    fun promptFor(prefs: SharedPreferences): String? =
        if (!isVocabularyEnabled(prefs)) null else buildPrompt(parseBegriffe(vocabularyText(prefs)))

    /**
     * Whisper likes to output the prompt itself on silence/murmur. Echo = the text repeats the
     * prompt from its beginning ("Begriffe: …"), fully or partly. Dictation consisting only of
     * vocabulary words does not start with the marker and is kept.
     */
    fun isPromptEcho(text: String, prompt: String?): Boolean {
        if (prompt.isNullOrEmpty() || text.isBlank()) return false
        fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
        val t = norm(text)
        val p = norm(prompt)
        val marker = norm(ECHO_MARKER)
        return t.startsWith(marker) && p.contains(t.substring(marker.length))
    }
}
