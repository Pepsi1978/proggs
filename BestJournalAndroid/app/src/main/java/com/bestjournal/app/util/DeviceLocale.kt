package com.bestjournal.app.util

import java.util.Locale

/**
 * Central source for device language and locale settings.
 * Used across the app for AI prompts, transcription, and date formatting.
 *
 * Single source of truth — if we ever add an in-app language override,
 * only this file needs to change.
 */
object DeviceLocale {
    /**
     * Language name in English for AI prompts (e.g., "German", "Japanese", "English").
     * Gemini understands English language names reliably across all models.
     * Falls back to "German" if the locale returns an empty name.
     */
    val promptLanguage: String
        get() = Locale.getDefault().getDisplayLanguage(Locale.ENGLISH).ifBlank { "English" }

    /**
     * ISO 639-1 language code for speech recognition APIs (e.g., "de", "ja", "en").
     * Used by Whisper (local + Groq) to set the transcription target language.
     * Falls back to "en" (English) if the locale returns an empty code.
     */
    val languageCode: String
        get() = Locale.getDefault().language.ifBlank { "en" }

    /**
     * Device locale for date/number formatting.
     * Ensures dates, month names, and numbers match user expectations.
     */
    val locale: Locale
        get() = Locale.getDefault()
}
