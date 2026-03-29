package com.entropyjournal.util

object Constants {
    // API Base URLs
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"

    // Groq
    const val GROQ_TRANSCRIPTION_MODEL = "whisper-large-v3-turbo"
    const val GROQ_LANGUAGE = "de"

    // Audio recording
    const val DEFAULT_MAX_RECORDING_DURATION_MINUTES = 5
    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_CHANNELS = 1

    // Sync
    const val SYNC_DEBOUNCE_MS = 30_000L
    const val ANALYSIS_DEBOUNCE_MS = 60_000L
    const val DRIVE_BACKUP_FILENAME = "entropy_journal_backup.db"

    // SharedPreferences keys
    const val PREF_GROQ_API_KEY = "groq_api_key"
    const val PREF_GEMINI_API_KEY = "gemini_api_key"
    const val PREF_GEMINI_MODEL = "gemini_model"
    const val PREF_TEXT_IMPROVEMENT_DEFAULT = "text_improvement_default"
    const val PREF_MAX_RECORDING_DURATION = "max_recording_duration"
    const val PREF_AUTO_UPDATE_DASHBOARD = "auto_update_dashboard"
    const val PREF_GOOGLE_ACCOUNT_NAME = "google_account_name"
    const val PREF_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
    const val PREF_GOOGLE_AVATAR_URL = "google_avatar_url"
    const val PREF_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val PREF_DARK_THEME = "dark_theme"

    // Google OAuth
    const val GOOGLE_WEB_CLIENT_ID = "674560807048-l6ktqsucjr4ld91srdc6assgfiks19mh.apps.googleusercontent.com"

    // Encrypted SharedPreferences file name
    const val ENCRYPTED_PREFS_NAME = "entropy_journal_secure_prefs"

    // Gemini Flash models: sorted strongest→weakest, prices per 1M tokens (input/output)
    data class GeminiModel(
        val id: String,
        val displayName: String,
        val price: String
    )

    val GEMINI_FLASH_MODELS = listOf(
        GeminiModel("gemini-3-flash-preview", "Gemini 3 Flash", "\$0.50 / \$3.00"),
        GeminiModel("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "\$0.25 / \$1.50"),
        GeminiModel("gemini-2.5-flash", "Gemini 2.5 Flash", "\$0.30 / \$2.50"),
        GeminiModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", "\$0.10 / \$0.40"),
        GeminiModel("gemini-2.0-flash", "Gemini 2.0 Flash", "\$0.10 / \$0.40"),
    )

    val DEFAULT_GEMINI_MODEL = GEMINI_FLASH_MODELS.first().id
}
