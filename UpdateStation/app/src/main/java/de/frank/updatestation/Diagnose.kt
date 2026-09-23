package de.frank.updatestation

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Feste Phasen des Diagnoseprotokolls. */
enum class Phase { SCAN, SYNC, DOWNLOAD, PRUEFUNG, INSTALLATION, BENACHRICHTIGUNG, WORKER }

/**
 * Dauerhaftes, begrenztes Diagnoseprotokoll (filesDir/diagnose.log), das Prozessneustarts
 * überlebt und sich in den Einstellungen ansehen und teilen lässt.
 *
 * Datensparsam: jede Zeile besteht nur aus Zeit, fester [Phase], festem Code und Feldern, deren
 * Werte Zahlen, Enums oder auf [A-Za-z0-9._-] gekürzte Namen sind – nie Tokens, URLs, Drive-IDs,
 * Content-URIs oder rohe Exception-Meldungen. Jede Datei-Operation ist fehlertolerant: das
 * Protokoll kann Scan oder Installation nie abbrechen.
 */
object Diagnose {
    private const val DATEI = "diagnose.log"
    private const val MAX_ZEILEN = 300
    private const val MAX_BYTES = 64 * 1024

    /** Name/Kennung für das Protokoll: nur unbedenkliche Zeichen, höchstens 48 Stück. */
    fun name(s: String?): String = s.orEmpty().filter { it.isLetterOrDigit() && it.code < 128 || it in "._-" }.take(48).ifEmpty { "-" }

    /** Fehlerklasse ohne Meldungstext. */
    fun klasse(e: Throwable): String = name(e.javaClass.simpleName)

    fun ereignis(context: Context, phase: Phase, code: String, vararg felder: Pair<String, Any?>) {
        val werte = felder.joinToString(" ") { (k, v) ->
            val wert = when (v) {
                null -> "-"
                is Number, is Boolean -> v.toString()
                is Enum<*> -> v.name
                else -> name(v.toString())
            }
            "${name(k)}=$wert"
        }
        val zeit = SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY).format(Date())
        val zeile = "$zeit ${phase.name} ${name(code)}" + if (werte.isEmpty()) "" else " $werte"
        synchronized(this) {
            runCatching {
                val datei = File(context.filesDir, DATEI)
                datei.appendText(zeile + "\n")
                kuerze(datei)
            }.onFailure { Log.w(TAG, "Diagnose nicht schreibbar: ${klasse(it)}") }
        }
    }

    /** Hält Zeilen- UND Bytegrenze ein; schreibt über eine Temp-Datei, bei Lesefehlern neu beginnen. */
    private fun kuerze(datei: File) {
        if (datei.length() <= MAX_BYTES / 4 && datei.length() < MAX_ZEILEN * 20L) return
        val zeilen = runCatching { datei.readLines() }.getOrElse { datei.delete(); return }
        if (zeilen.size <= MAX_ZEILEN && datei.length() <= MAX_BYTES) return
        val behalten = ArrayDeque(zeilen.takeLast(MAX_ZEILEN))
        var bytes = behalten.sumOf { it.toByteArray().size + 1 }
        while (bytes > MAX_BYTES && behalten.isNotEmpty()) bytes -= behalten.removeFirst().toByteArray().size + 1
        val temp = File(datei.parentFile, "$DATEI.tmp")
        temp.writeText(behalten.joinToString("\n", postfix = if (behalten.isEmpty()) "" else "\n"))
        if (!temp.renameTo(datei)) { datei.delete(); temp.renameTo(datei) }
    }

    /** Die letzten Einträge, neueste zuerst; leer, wenn nichts lesbar ist. */
    fun lesen(context: Context, anzahl: Int = MAX_ZEILEN): List<String> = synchronized(this) {
        runCatching { File(context.filesDir, DATEI).takeIf { it.exists() }?.readLines().orEmpty() }
            .getOrDefault(emptyList()).takeLast(anzahl).reversed()
    }

    /** Text zum Teilen: Kopfzeile mit App-Version, dann alle Einträge in zeitlicher Reihenfolge. */
    fun text(context: Context): String =
        "UpdateStation ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) – Diagnose\n" +
            lesen(context).reversed().joinToString("\n").ifEmpty { "(keine Einträge)" }
}

/**
 * Sichtbare Hinweise für die App, abgeleitet aus dem gespeicherten Zustand – auch dann sichtbar,
 * wenn die zugehörige Benachrichtigung nicht gezeigt werden durfte.
 */
object Hinweise {
    fun aktuell(context: Context): List<Pair<String, String>> {
        val einst = Einstellungen(context)
        val liste = mutableListOf<Pair<String, String>>()
        if (einst.pruefFehler) {
            liste += "Hintergrundprüfung scheitert" to "Die automatische Prüfung ist mehrmals fehlgeschlagen. Details unter Einstellungen → Diagnose."
        }
        if (einst.quelleLeer) {
            liste += "Keine Update-Projekte mehr gefunden" to "Der Update-Ordner ist seit längerer Zeit leer. UpdateStation sucht bei jeder Prüfung weiter."
        }
        einst.haengendeProjekte(PruefWorker.MAX_NACHPRUEFUNGEN).filter { it != Einstellungen.LEER }.forEach { projekt ->
            liste += "Update-Ordner „$projekt“ nicht stimmig" to "Seit etwa 30 Minuten passen APK und update.json nicht zusammen. Veröffentlichung am PC prüfen."
        }
        return liste
    }
}
