package de.frank.perfectmoment.backup

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Remembers when Google last actually copied the data.
 *
 * A backup switch that merely says "on" is worthless — it can be enabled while nothing has ever
 * been written. So the agent stamps the time of every completed run and the settings screen shows
 * it, which makes a dead backup visible instead of silently reassuring.
 *
 * Kept in plain preferences on purpose: this is a local fact about this device and must not travel
 * into the backup itself.
 */
object BackupStatus {

    private const val PREFS = "pm_backup_status"
    private const val KEY_LAST_BACKUP = "zuletzt_gesichert"

    private val format = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)

    fun markBackedUp(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP, System.currentTimeMillis())
            .apply()
    }

    private fun lastBackupAt(context: Context): Long =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP, 0L)

    /** Never claims a backup happened when it did not. */
    fun describe(context: Context): String {
        val last = lastBackupAt(context)
        return if (last == 0L) {
            "Noch nicht gesichert — läuft beim Laden im WLAN"
        } else {
            "Zuletzt: ${format.format(Date(last))} Uhr"
        }
    }

    fun hasBackup(context: Context): Boolean = lastBackupAt(context) > 0L
}
