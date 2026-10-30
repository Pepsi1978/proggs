package de.frank.jarvis.speech

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Stellt das Offline-Modell den anderen Apps bereit (Gegenstück: `ParakeetUeberJarvis.kt` aus Modul M1.4). Die Dateien liegen
 * für andere Apps unerreichbar in Jarvis, und jede App bräuchte sonst 670 MB Speicher und Arbeitsspeicher: Darum schickt eine
 * App ihre Aufnahme als Dateideskriptor (Binder trägt nur rund 1 MB) samt eigener Wortliste, Jarvis rechnet und gibt den Text zurück.
 *
 * Nur Apps mit Franks Signaturschlüssel kommen an diese Schnittstelle (`de.frank.jarvis.permission.BRUECKE`).
 */
class ParakeetBruecke : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val context = context ?: return fehler("Jarvis ist nicht bereit.")
        return runCatching {
            when (method) {
                "status" -> Bundle().apply {
                    putInt("schnittstelle", 1)
                    putBoolean("modell_bereit", ParakeetLokal.bereit(context))
                }
                "vorwaermen" -> {
                    bereich.launch { ParakeetLokal.vorwaermen(context.applicationContext); freigabePlanen() }
                    Bundle()
                }
                "transkribiere" -> Bundle().apply { putString("text", transkribiere(extras)) }
                else -> fehler("Unbekannte Methode: $method")
            }
        }.getOrElse {
            Logbuch.fehler("Parakeet-Brücke $method", it)
            fehler(it.message ?: "Die Mitschrift in Jarvis ist fehlgeschlagen.")
        }
    }

    private fun transkribiere(extras: Bundle?): String {
        val context = requireNotNull(context)
        @Suppress("DEPRECATION")
        val fd = extras?.getParcelable<ParcelFileDescriptor>("audio") ?: throw ParakeetFehler("Keine Aufnahme übergeben.")
        // In einen eigenen Cache kopieren: MediaExtractor und RIFF-Leser brauchen wahlfreien Zugriff, und die App kann ihre Datei danach löschen.
        val datei = File.createTempFile("parakeet", ".audio", context.cacheDir)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(fd).use { ein -> datei.outputStream().use { ein.copyTo(it) } }
            val proben = ParakeetAudio.lies(datei)
            return runBlocking {
                ParakeetLokal.transkribiereProben(
                    context.applicationContext,
                    proben,
                    extras.getString("hotwords").orEmpty(),
                )
            }.also { freigabePlanen() }
        } finally {
            datei.delete()
        }
    }

    /** Nutzt Jarvis selbst Groq, hält das Modell nicht dauerhaft rund 700 MB Speicher, nur weil eine andere App einmal diktiert hat. */
    private fun freigabePlanen() {
        val context = context?.applicationContext ?: return
        freigabe?.cancel()
        freigabe = bereich.launch {
            delay(FREIGABE_NACH_MS)
            if (!SecureSettings(context).use { it.transkriptionLokal }) ParakeetLokal.freigeben()
        }
    }

    private fun fehler(text: String) = Bundle().apply { putString("fehler", text) }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    private companion object {
        const val FREIGABE_NACH_MS = 15 * 60 * 1000L
        val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        @Volatile var freigabe: Job? = null
    }
}
