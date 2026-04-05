package com.bestjournal.app.util

object Constants {
    // Groq API (for premium transcription via Firebase Remote Config)
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    const val GROQ_TRANSCRIPTION_MODEL = "whisper-large-v3-turbo"
    const val GROQ_LANGUAGE = "de"
    const val REMOTE_CONFIG_GROQ_KEY = "groq_api_key"

    // Audio recording
    const val MAX_RECORDING_DURATION_MINUTES = 15
    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_CHANNELS = 1

    // Sync
    const val SYNC_DEBOUNCE_MS = 30_000L
    const val ANALYSIS_DEBOUNCE_MS = 60_000L
    const val DRIVE_BACKUP_FILENAME = "bestjournal_backup.db"

    // SharedPreferences keys
    const val PREF_TEXT_IMPROVEMENT_DEFAULT = "text_improvement_default"
    const val PREF_MAX_RECORDING_DURATION = "max_recording_duration"
    const val PREF_AUTO_UPDATE_DASHBOARD = "auto_update_dashboard"
    const val PREF_MANUAL_REFRESHES_LEFT = "manual_refreshes_left"
    const val MAX_REFRESHES_PER_ENTRY = 5
    const val PREF_GOOGLE_ACCOUNT_NAME = "google_account_name"
    const val PREF_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
    const val PREF_GOOGLE_AVATAR_URL = "google_avatar_url"
    const val PREF_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val PREF_DARK_THEME = "dark_theme"
    const val PREF_THEME_FOLLOW_SYSTEM = "theme_follow_system"
    const val PREF_THEME_FOLLOW_SUN = "theme_follow_sun"
    const val PREF_LATITUDE = "location_latitude"
    const val PREF_LONGITUDE = "location_longitude"
    const val PREF_BIOMETRIC_LOCK = "biometric_lock"
    const val PREF_DASHBOARD_UPDATING = "dashboard_updating"
    const val PREF_DASHBOARD_LAST_UPDATED = "dashboard_last_updated"

    // Google OAuth
    const val GOOGLE_WEB_CLIENT_ID =
        "314424175660-12m82j2jgvmc0tcgl61igghp67nnpmn1.apps.googleusercontent.com"

    // Encrypted SharedPreferences file name
    const val ENCRYPTED_PREFS_NAME = "entropy_journal_secure_prefs"

    // Dashboard-Szenario
    const val PREF_DASHBOARD_SCENARIO = "dashboard_scenario"
    const val PREF_CUSTOM_PROMPT = "custom_dashboard_prompt"
    const val DASHBOARD_SCENARIO_COUNT = 5

    // Freemium
    const val TRIAL_USAGE_DAYS = 7
    const val FREE_WEEKLY_DASHBOARD_LIMIT = 5
    const val FREE_WEEKLY_TEXT_LIMIT = 5
    const val MAX_ENTRIES_FREE_ANALYSIS = 20
    const val MAX_ENTRIES_SUBSCRIBED_ANALYSIS = 40
    const val MAX_ENTRIES_TRIAL_ANALYSIS = 20

    // Trial daily limits (4-tier: Premium→Lite→Cooldown→Hard)
    const val TRIAL_PREMIUM_LIMIT = 20 // 1-20: Flash 2.5
    const val TRIAL_COOLDOWN_AT = 81 // 21-80: Lite, 81st: 30-min cooldown
    const val TRIAL_HARD_LIMIT = 101 // 101+: blocked for the day

    // Subscriber daily limits (4-tier, higher than trial)
    const val SUB_PREMIUM_LIMIT = 30 // 1-30: Flash 2.5
    const val SUB_COOLDOWN_AT = 101 // 31-100: Lite, 101st: 30-min cooldown
    const val SUB_HARD_LIMIT = 151 // 151+: blocked for the day

    // Cooldown duration (shared)
    const val COOLDOWN_MINUTES = 30

    // Spam protection
    const val SPAM_HOURLY_AI_LIMIT = 30

    // Subscription pricing (display only — actual prices set in Google Play Console)
    const val MONTHLY_PRICE_DISPLAY = "3,99\u00A0\u20AC"
    const val YEARLY_PRICE_DISPLAY = "29,99\u00A0\u20AC"
    const val YEARLY_MONTHLY_EQUIVALENT = "2,50\u00A0\u20AC"
}
