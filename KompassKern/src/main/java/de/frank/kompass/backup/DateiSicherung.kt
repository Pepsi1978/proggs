package de.frank.kompass.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import de.frank.kompass.AppProfil
import de.frank.kompass.observability.KompassLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Eine Sicherungsdatei im gewählten Ordner. */
data class Sicherungsdatei(val uri: Uri, val name: String, val geaendertAm: Long)

/**
 * Sicherung als Datei in einen selbst gewählten Ordner — in der Praxis ein Google-Drive-Ordner,
 * weil die Drive-App sich Android als Speicherort anbietet.
 *
 * Der Weg braucht kein Cloud-Projekt, keine Client-ID und keine Anmeldung: Android fragt einmal
 * nach dem Ordner, die App behält ihn dauerhaft und schreibt danach ohne Rückfrage dorthin. Auf
 * einem zweiten Gerät wird derselbe Ordner gewählt und die Sicherung eingelesen.
 *
 * Der frühere Weg über die Drive-API scheiterte an dem, was er zusätzlich verlangte: eine in der
 * Google-Cloud-Console registrierte OAuth-Client-ID. Ohne sie antwortet Google mit
 * „Fehler 888 / unregistered on API console“ — daran lässt sich in der App nichts ändern.
 *
 * Es liegen immer höchstens [BEHALTEN] Sicherungen im Ordner: die aktuelle und die davor.
 * Ältere räumt jede Sicherung selbst weg, damit sich dort nichts ansammelt.
 */
class DateiSicherung(private val context: Context) {

    private val prefs
        get() = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Der gemerkte Sicherungsordner, oder null solange keiner gewählt wurde. */
    val ordner: Uri?
        get() = prefs.getString(KEY_ORDNER, null)?.let(Uri::parse)

    /** Ordner merken und die Schreibberechtigung über Neustarts hinweg behalten. */
    fun merkeOrdner(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit().putString(KEY_ORDNER, uri.toString()).apply()
    }

    fun vergissOrdner() {
        ordner?.let { alt ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    alt,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs.edit().remove(KEY_ORDNER).apply()
    }

    /** Der Name des Ordners, wie ihn der Dateiwähler zeigt — für die Anzeige in den Einstellungen. */
    fun ordnerName(): String? {
        val baum = ordner ?: return null
        val kennung = runCatching { DocumentsContract.getTreeDocumentId(baum) }.getOrNull()
            ?: return baum.lastPathSegment
        val dokument = DocumentsContract.buildDocumentUriUsingTree(baum, kennung)
        return spalte(dokument, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            ?: kennung.substringAfterLast(':').substringAfterLast('/')
    }

    /**
     * Schreibt eine neue Sicherung und räumt danach auf: Es bleiben nur die aktuelle und die
     * eine davor stehen.
     */
    suspend fun schreibe(json: String): Sicherungsdatei = withContext(Dispatchers.IO) {
        val baum = ordner ?: error("Es ist noch kein Sicherungsordner gewählt.")
        val bisherige = listeAuf(baum)
        val name = neuerName(bisherige)
        val datei = DocumentsContract.createDocument(
            context.contentResolver,
            DocumentsContract.buildDocumentUriUsingTree(baum, DocumentsContract.getTreeDocumentId(baum)),
            "application/json",
            name,
        ) ?: error("Im Sicherungsordner liess sich keine Datei anlegen. Wähl ihn neu aus.")

        check(bisherige.none {
            DocumentsContract.getDocumentId(it.uri) == DocumentsContract.getDocumentId(datei)
        }) { "Der Speicheranbieter hat keine neue Datei angelegt. Die bestehende Sicherung bleibt unverändert." }

        try {
            context.contentResolver.openOutputStream(datei, "wt")?.use { strom ->
                strom.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Die Sicherung ließ sich nicht schreiben. Wähl den Ordner neu aus.")
        } catch (fehler: Exception) {
            runCatching {
                check(DocumentsContract.deleteDocument(context.contentResolver, datei))
            }.onFailure {
                KompassLog.warn("DateiSicherung", "schreibe", "Unvollständige Datei blieb liegen",
                    mapOf("art" to it.javaClass.simpleName))
            }
            throw fehler
        }

        // Die Sicherung steht — ab hier zählt sie, auch wenn das Aufräumen danach hakt.
        BackupStatus.markBackedUp(context)
        raeumeAuf(bisherige)
        Sicherungsdatei(datei, name, System.currentTimeMillis())
    }

    /** Alle Sicherungen im Ordner, die jüngste zuerst. */
    suspend fun sicherungen(): List<Sicherungsdatei> = withContext(Dispatchers.IO) {
        val baum = ordner ?: return@withContext emptyList()
        listeAuf(baum)
    }

    suspend fun lies(quelle: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(quelle)?.use { strom ->
            strom.readBytes().toString(Charsets.UTF_8)
        } ?: error("Die Sicherung konnte nicht gelesen werden. Wähl den Ordner neu aus.")
    }

    private fun kinderUri(baum: Uri): Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
        baum,
        DocumentsContract.getTreeDocumentId(baum),
    )

    /** Nur die eigenen Sicherungen zählen — fremde Dateien im Ordner bleiben unangetastet. */
    private fun listeAuf(baum: Uri): List<Sicherungsdatei> {
        val gefunden = mutableListOf<Sicherungsdatei>()
        context.contentResolver.query(
                kinderUri(baum),
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,
                ),
                null,
                null,
                null,
            )?.use { zeiger ->
                while (zeiger.moveToNext()) {
                    val name = zeiger.getString(1) ?: continue
                    if (!name.startsWith(PRAEFIX) || !name.endsWith(".json")) continue
                    // Eine leer gebliebene Datei (Schreiben abgebrochen) ist keine Sicherung —
                    // sonst gälte sie als „die davor“ und die gute würde weggeräumt.
                    if (!zeiger.isNull(3) && zeiger.getLong(3) == 0L) continue
                    gefunden += Sicherungsdatei(
                        uri = DocumentsContract.buildDocumentUriUsingTree(baum, zeiger.getString(0)),
                        name = name,
                        geaendertAm = zeiger.getLong(2),
                    )
                }
            } ?: error("Der Sicherungsordner konnte nicht aufgelistet werden. Bestehende Sicherungen bleiben erhalten.")
        // Alte Namen enthalten zusätzlich "sicherung-". Nur den Zeitstempel vergleichen,
        // sonst gewinnt die alte Datei alphabetisch gegen jede neue Sicherung. Verglichen wird
        // der echte Zeitpunkt: Neue Namen stehen in UTC (mit "Z"), alte in Ortszeit — sonst
        // bekäme nach der Umstellung auf Winterzeit die neuere Sicherung den kleineren Namen.
        return gefunden.sortedWith(
            compareByDescending<Sicherungsdatei> { zeitpunktAusNamen(it.name) ?: it.geaendertAm }
                .thenByDescending { it.geaendertAm },
        )
    }

    private fun zeitpunktAusNamen(name: String): Long? {
        val treffer = ZEIT_IM_NAMEN.find(name) ?: return null
        val ziffern = treffer.groupValues[1].replace("-", "").padEnd(17, '0')
        val format = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.GERMANY).apply {
            if (treffer.groupValues[2] == "Z") timeZone = UTC
            isLenient = false
        }
        return runCatching { format.parse(ziffern)?.time }.getOrNull()
    }

    private fun raeumeAuf(bisherige: List<Sicherungsdatei>) {
        // Die frisch geschriebene zählt schon als eine der beiden — vom Rest bleibt genau eine.
        val zuLoeschen = bisherige.drop(BEHALTEN - 1)
        zuLoeschen.forEach { datei ->
            runCatching {
                check(DocumentsContract.deleteDocument(context.contentResolver, datei.uri)) {
                    "Die ältere Sicherung konnte nicht gelöscht werden. Die neue Datei ist gespeichert, aber es liegen noch mehr als zwei Sicherungen im Ordner."
                }
            }
                .onFailure {
                    // Die neue Sicherung ist geschrieben — eine liegen gebliebene alte ist kein Fehlschlag.
                    KompassLog.warn(
                        "DateiSicherung",
                        "raeumeAuf",
                        "Alte Sicherung blieb liegen",
                        mapOf("art" to it.javaClass.simpleName),
                    )
                }
        }
        if (zuLoeschen.isNotEmpty()) {
            KompassLog.info(
                "DateiSicherung",
                "raeumeAuf",
                "Alte Sicherungen entfernt",
                mapOf("anzahl" to zuLoeschen.size),
            )
        }
    }

    private fun spalte(dokument: Uri, name: String): String? = runCatching {
        context.contentResolver.query(dokument, arrayOf(name), null, null, null)?.use { zeiger ->
            if (zeiger.moveToFirst()) zeiger.getString(0) else null
        }
    }.getOrNull()

    private fun neuerName(bisherige: List<Sicherungsdatei>): String {
        // UTC mit "Z": Zeitumstellung und Zeitzonenwechsel bringen die Reihenfolge nicht durcheinander.
        val zeit = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.GERMANY).apply { timeZone = UTC }.format(Date())
        val basis = "$PRAEFIX${zeit}Z"
        val namen = bisherige.map { it.name }.toSet()
        var name = "$basis.json"
        var nummer = 2
        while (name in namen) name = "$basis (${nummer++}).json"
        return name
    }

    companion object {
        /** Die aktuelle Sicherung und die eine davor — mehr sammelt sich nie an. */
        const val BEHALTEN = 2

        private const val PREFS = "kompass_backup_status"
        private const val KEY_ORDNER = "sicherungs_ordner"
        private val PRAEFIX = "${AppProfil.DATEI_PRAEFIX}-"
        private val ZEIT_IM_NAMEN = Regex("(\\d{4}-\\d{2}-\\d{2}-\\d{4}(?:\\d{2}-\\d{3})?)(Z?)")
        private val UTC = java.util.TimeZone.getTimeZone("UTC")
    }
}
