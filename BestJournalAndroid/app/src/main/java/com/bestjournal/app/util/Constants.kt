package com.bestjournal.app.util

object Constants {
    // Audio recording
    const val DEFAULT_MAX_RECORDING_DURATION_MINUTES = 5
    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_CHANNELS = 1

    // Sync
    const val SYNC_DEBOUNCE_MS = 30_000L
    const val ANALYSIS_DEBOUNCE_MS = 60_000L
    const val DRIVE_BACKUP_FILENAME = "entropy_journal_backup.db"

    // SharedPreferences keys
    const val PREF_TEXT_IMPROVEMENT_DEFAULT = "text_improvement_default"
    const val PREF_MAX_RECORDING_DURATION = "max_recording_duration"
    const val PREF_AUTO_UPDATE_DASHBOARD = "auto_update_dashboard"
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

    // Google OAuth
    const val GOOGLE_WEB_CLIENT_ID = "674560807048-l6ktqsucjr4ld91srdc6assgfiks19mh.apps.googleusercontent.com"

    // Encrypted SharedPreferences file name
    const val ENCRYPTED_PREFS_NAME = "entropy_journal_secure_prefs"

    // Freemium
    const val TRIAL_USAGE_DAYS = 7
    const val HONEYMOON_USAGE_DAYS = 3
    const val FREE_WEEKLY_AI_LIMIT = 3
    const val MAX_ENTRIES_FREE_ANALYSIS = 10
    const val MAX_ENTRIES_SUBSCRIBED_ANALYSIS = 30
}
