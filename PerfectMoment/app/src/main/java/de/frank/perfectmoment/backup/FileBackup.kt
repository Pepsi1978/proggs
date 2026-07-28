package de.frank.perfectmoment.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sicherung als Datei an einen selbst gewählten Ort — in der Praxis der Google-Drive-Ordner, weil
 * die Drive-App sich Android als Speicherort anbietet.
 *
 * Der Weg braucht kein Cloud-Projekt, keine Client-ID und keine Freigabe: Android fragt einmal nach
 * dem Ort, die App behält ihn dauerhaft und schreibt danach ohne Rückfrage dorthin. Auf einem
 * zweiten Gerät wird dieselbe Datei ausgewählt und eingelesen.
 */
class FileBackup(private val context: Context) {

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Der gemerkte Sicherungsort, oder null solange keiner gewählt wurde. */
    val target: Uri?
        get() = prefs.getString(KEY_TARGET, null)?.let(Uri::parse)

    /** Ort merken und die Leseberechtigung über Neustarts hinweg behalten. */
    fun rememberTarget(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        prefs.edit().putString(KEY_TARGET, uri.toString()).apply()
    }

    fun forgetTarget() = prefs.edit().remove(KEY_TARGET).apply()

    /** Name mit Datum, damit mehrere Sicherungen nebeneinander erkennbar bleiben. */
    fun suggestedName(): String =
        "perfectmoment-${SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())}.json"

    suspend fun write(uri: Uri, json: String) = withContext(Dispatchers.IO) {
        // "wt" schneidet die Datei ab; ohne das bliebe bei kürzerem Inhalt alter Text stehen.
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(json.toByteArray())
        } ?: throw IllegalStateException("Der Sicherungsort ist nicht mehr erreichbar.")
        BackupStatus.markBackedUp(context)
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().decodeToString()
        } ?: throw IllegalStateException("Die Sicherung konnte nicht gelesen werden.")
    }

    private companion object {
        const val PREFS = "pm_backup_status"
        const val KEY_TARGET = "sicherungs_ziel"
    }
}
