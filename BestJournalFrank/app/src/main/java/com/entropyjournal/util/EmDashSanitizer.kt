package com.entropyjournal.util

/**
 * Post-processing guard for all AI-generated German text. Even with the
 * NO_EM_DASH_RULE in the system prompt, Gemini occasionally emits em-dashes
 * (U+2014) or en-dashes (U+2013). We strip them here so the user never sees
 * the "LLM tell" in journal improvements, summaries, dashboards or reviews.
 *
 * Replacement strategy:
 *   " — " and " – " with spaces around → ", "   (natural pause)
 *   "—"  or "–"  without spaces       → ", "   (also a list/range separator)
 *
 * Headlines are rendered separately and not run through this helper, so the
 * rare "only in absolute emergency" case in titles is still possible.
 */
fun String.stripEmDashes(): String {
    if (isEmpty()) return this
    return this
        // Spaced dashes → comma + space (most common: "Wort — Wort")
        .replace(Regex("\\s+[—–]\\s+"), ", ")
        // Leading/trailing spaced dash variants
        .replace(Regex("[—–]\\s+"), ", ")
        .replace(Regex("\\s+[—–]"), ", ")
        // Bare dashes (range or joined) → comma + space
        .replace("—", ", ")
        .replace("–", ", ")
}
