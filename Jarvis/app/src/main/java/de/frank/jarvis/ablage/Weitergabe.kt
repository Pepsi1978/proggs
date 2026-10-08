package de.frank.jarvis.ablage

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/** Eine Datei, wie sie das Handy verlässt: der echte Inhalt, der ursprüngliche Name und der passende MIME-Typ. */
data class Freigabe(val datei: File, val name: String, val mime: String)

/**
 * Download, „Speichern unter“, Teilen und „Mit anderer App öffnen“. Alles über Androids vorgesehene Wege:
 * MediaStore für den Download-Ordner (ohne Speicherberechtigung), der Systemdialog für „Speichern unter“ und
 * Inhalts-Adressen (FileProvider) mit befristeter Leseberechtigung zum Teilen und Öffnen. Exportiert wird immer
 * die Originaldatei, Byte für Byte.
 */
object Weitergabe {
    const val AUTHORITY = "de.frank.jarvis.dateien"
    const val ORDNER = "Jarvis"

    fun uri(context: Context, f: Freigabe): Uri = FileProvider.getUriForFile(context, AUTHORITY, f.datei, f.name)

    /** Ob der Download-Ordner ohne Systemdialog erreichbar ist (ab Android 10). Darunter bleibt „Speichern unter“. */
    val downloadOhneDialog: Boolean get() = Build.VERSION.SDK_INT >= 29

    /**
     * Kopiert die Datei nach „Download/Jarvis/“. Rückgabe: der tatsächlich vergebene Name (Android hängt bei
     * gleichem Namen selbst eine Nummer an). Wirft bei Fehlern; eine halbe Kopie wird wieder entfernt.
     */
    fun herunterladen(context: Context, f: Freigabe): String {
        if (Build.VERSION.SDK_INT < 29) throw IllegalStateException("Auf diesem Android bitte „Speichern unter“ verwenden.")
        val aufloeser = context.contentResolver
        val werte = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            put(MediaStore.MediaColumns.MIME_TYPE, f.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + ORDNER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val ziel = aufloeser.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, werte) ?: throw IllegalStateException("Der Download-Ordner ist nicht erreichbar.")
        try {
            kopiere(context, f.datei, ziel)
            aufloeser.update(ziel, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            runCatching { aufloeser.delete(ziel, null, null) }
            throw e
        }
        return runCatching {
            aufloeser.query(ziel, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() ?: f.name
    }

    /** Schreibt die Datei an die im Systemdialog „Speichern unter“ gewählte Stelle und prüft die Größe. */
    fun speichernUnter(context: Context, datei: File, ziel: Uri) = kopiere(context, datei, ziel)

    private fun kopiere(context: Context, datei: File, ziel: Uri) {
        val geschrieben = (context.contentResolver.openOutputStream(ziel, "wt") ?: context.contentResolver.openOutputStream(ziel)
            ?: throw IllegalStateException("Das Ziel lässt sich nicht beschreiben.")).use { aus ->
            datei.inputStream().use { it.copyTo(aus, 64 * 1024) }
        }
        // Größe am Ziel nachprüfen, soweit der Anbieter sie nennt (Google Drive meldet sie oft erst später).
        val groesse = runCatching {
            context.contentResolver.query(ziel, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }
        }.getOrNull()
        if (groesse != null && groesse > 0 && groesse != datei.length()) throw IllegalStateException("Die Kopie ist unvollständig ($groesse von ${datei.length()} Bytes).")
        if (geschrieben != datei.length()) throw IllegalStateException("Die Kopie ist unvollständig.")
    }

    /**
     * Öffnet die Android-Teilauswahl mit den echten Dateien als Anhang (eine oder mehrere). Das ist noch kein
     * Versand: Ob und wohin geschickt wird, entscheidet Frank in der gewählten App.
     */
    fun teilen(context: Context, dateien: List<Freigabe>, betreff: String? = null) {
        require(dateien.isNotEmpty())
        val uris = dateien.map { uri(context, it) }
        val typ = gemeinsamerTyp(dateien.map { it.mime })
        val absicht = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }.apply {
            type = typ
            betreff?.let { putExtra(Intent.EXTRA_SUBJECT, it); putExtra(Intent.EXTRA_TITLE, it) }
            clipData = ClipData.newUri(context.contentResolver, dateien[0].name, uris[0]).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(absicht, if (uris.size == 1) "„${dateien[0].name}“ teilen" else "${uris.size} Dateien teilen").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** Übergibt die Datei an eine passende installierte App. false = keine App kann diesen Typ öffnen. */
    fun oeffnen(context: Context, f: Freigabe): Boolean {
        val absicht = Intent(Intent.ACTION_VIEW).setDataAndType(uri(context, f), f.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) absicht.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(absicht)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun gemeinsamerTyp(mimes: List<String>): String {
        val eindeutig = mimes.distinct()
        if (eindeutig.size == 1) return eindeutig[0]
        val oben = eindeutig.map { it.substringBefore('/') }.distinct()
        return if (oben.size == 1) oben[0] + "/*" else "*/*"
    }
}
