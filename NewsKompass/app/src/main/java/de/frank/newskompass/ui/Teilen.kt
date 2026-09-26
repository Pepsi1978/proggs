package de.frank.newskompass.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.observability.KompassLog
import java.io.File

/**
 * Teilt eine Meldung über den Android-Teilen-Dialog, etwa an WhatsApp.
 *
 * Das Bild wird dafür in `cache/teilen/` kopiert und nur über eine `content://`-Adresse des
 * FileProviders mit einmaliger Leseberechtigung herausgegeben — die gespeicherten Ausgaben und
 * ihre Bilder bleiben unberührt und von außen unsichtbar.
 */
object Teilen {

    private const val ORDNER = "teilen"

    /** So lange darf die Empfänger-App die Kopie noch lesen, dann räumt der nächste Aufruf sie weg. */
    private const val HALTBAR_MS = 30 * 60_000L

    /** Mehr Kopien bleiben nie liegen, auch wenn viele Meldungen kurz hintereinander geteilt werden. */
    private const val HOECHSTENS = 5

    /** Bild mit Überschrift, erstem Absatz und bester Quelle — kurz genug für eine Bildunterschrift. */
    fun mitBild(context: Context, meldung: Meldung, bild: File) {
        val uri = runCatching {
            val kopie = kopiere(context, bild, meldung)
            FileProvider.getUriForFile(context, "${context.packageName}.teilen", kopie)
        }.getOrElse {
            KompassLog.warn("Teilen", "mitBild", "Bild ließ sich nicht freigeben, teile nur Text", mapOf("grund" to it.message))
            return alsText(context, meldung)
        }
        val absicht = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, kurzText(meldung))
            putExtra(Intent.EXTRA_SUBJECT, meldung.titel)
            clipData = ClipData.newUri(context.contentResolver, meldung.titel, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        starte(context, absicht)
    }

    /** Der ganze Beitrag als Text — geht immer, auch ohne Bild. */
    fun alsText(context: Context, meldung: Meldung) {
        val absicht = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, vollText(meldung))
            putExtra(Intent.EXTRA_SUBJECT, meldung.titel)
        }
        starte(context, absicht)
    }

    /** Beim App-Start: alte Kopien weg, falls ein früherer Lauf nicht mehr zum Aufräumen kam. */
    fun raeumeAuf(context: Context) = raeumeAuf(File(context.cacheDir, ORDNER), behalten = Int.MAX_VALUE)

    private fun starte(context: Context, absicht: Intent) {
        val auswahl = Intent.createChooser(absicht, "Meldung teilen")
        if (context !is android.app.Activity) auswahl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(auswahl) }
            .onFailure { KompassLog.warn("Teilen", "starte", "Teilen-Dialog ließ sich nicht öffnen", mapOf("grund" to it.message)) }
    }

    private fun kopiere(context: Context, bild: File, meldung: Meldung): File {
        val ordner = File(context.cacheDir, ORDNER).apply { mkdirs() }
        raeumeAuf(ordner, behalten = HOECHSTENS - 1)
        val name = meldung.titel.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(50).ifBlank { "Meldung" }
        val ziel = File(ordner, "NewsKompass-$name-${System.currentTimeMillis() % 100_000}.jpg")
        bild.copyTo(ziel, overwrite = true)
        return ziel
    }

    /** Löscht Kopien, die älter als [HALTBAR_MS] sind, und darüber hinaus alle bis auf die [behalten] neuesten. */
    private fun raeumeAuf(ordner: File, behalten: Int) {
        val dateien = ordner.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        val grenze = System.currentTimeMillis() - HALTBAR_MS
        dateien.forEachIndexed { index, datei ->
            if (index >= behalten || datei.lastModified() < grenze) datei.delete()
        }
    }

    private fun kurzText(m: Meldung): String = buildString {
        append(m.titel)
        m.absaetze.firstOrNull()?.let { append("\n\n").append(it) }
        m.quellen.firstOrNull()?.let { append("\n\n").append(it) }
    }

    private fun vollText(m: Meldung): String = buildString {
        append(m.titel)
        if (m.wann.isNotBlank()) append("\n").append(m.wann.replaceFirstChar { it.uppercase() })
        m.absaetze.forEach { append("\n\n").append(it) }
        val quellen = m.quellen.distinct().take(3)
        if (quellen.isNotEmpty()) {
            append("\n\nQuellen:")
            quellen.forEach { append("\n").append(it) }
        }
    }
}
