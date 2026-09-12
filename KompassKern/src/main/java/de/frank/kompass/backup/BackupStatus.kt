package de.frank.kompass.backup

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Merkt sich, wann zuletzt wirklich eine Sicherung geschrieben wurde.
 *
 * Ein Sicherungsschalter, der nur „an" sagt, ist wertlos — er kann eingeschaltet sein, ohne dass
 * je etwas geschrieben wurde. Deshalb wird der Zeitpunkt jedes abgeschlossenen Laufs gestempelt
 * und im Einstellungsbildschirm angezeigt. Damit wird eine tote Sicherung sichtbar, statt still
 * zu beruhigen.
 *
 * Bewusst in der offenen Ablage: Das ist eine örtliche Tatsache über dieses Gerät und hat in der
 * Sicherungsdatei selbst nichts verloren.
 */
object BackupStatus {

    private const val PREFS = "kompass_backup_status"
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

    /** Behauptet nie eine Sicherung, die es nicht gab. */
    fun describe(context: Context): String {
        val last = lastBackupAt(context)
        return if (last == 0L) {
            "Noch nicht gesichert"
        } else {
            "Zuletzt: ${format.format(Date(last))} Uhr"
        }
    }

    fun hasBackup(context: Context): Boolean = lastBackupAt(context) > 0L
}
