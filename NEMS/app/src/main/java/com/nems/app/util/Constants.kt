package com.nems.app.util

object Constants {
    const val DATABASE_NAME = "nems_database"
    const val DRIVE_BACKUP_FILENAME = "nems_backup.db"

    // Preferences keys
    const val PREF_GOOGLE_ACCOUNT_NAME = "google_account_name"
    const val PREF_GOOGLE_ACCOUNT_EMAIL = "google_account_email"
    const val PREF_GOOGLE_AVATAR_URL = "google_avatar_url"
    const val PREF_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val PREF_RESTORE_PENDING = "restore_pending"
    const val PREF_DARK_THEME = "dark_theme"

    // TODO: Replace with your Google Cloud OAuth Web Client ID
    // Create at: https://console.cloud.google.com/apis/credentials
    // Type: Web application (NOT Android)
    const val GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
}
