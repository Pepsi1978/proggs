package de.frank.jarvis.weckwort

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import de.frank.jarvis.observability.Logbuch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Diagnose für „reagiert nicht auf leise Rufe“: schreibt für eine Weile den unverstärkten Ton des Weckworts
 * (`files/weckwort_mitschnitt.pcm`, 16 kHz, 16 Bit, mono) und je 80-ms-Block eine Zeile, was Stille-Filter und Spotter taten
 * (`files/weckwort_mitschnitt.csv`). Nur über adb: `adb shell am broadcast -a de.frank.jarvis.WECKWORT_MITSCHNITT -p de.frank.jarvis --ei sekunden 90`.
 * Läuft nur im Weckwort-Faden. Eine Anforderung beginnt die Dateien neu; spätere Durchgänge (nach einem Gespräch, Vorlesen) hängen an,
 * je mit einer Zeile „neu“ in der CSV.
 */
class WeckwortMitschnitt private constructor(private val ton: BufferedOutputStream, private val csv: java.io.BufferedWriter, private val bis: Long, private val start: Int) {
    private var block = 0
    private val bytes = ByteArray(WeckwortErkenner.BLOCK * 2)

    /** Hängt einen Block an; false = die Zeit ist um, der Mitschnitt ist geschlossen. */
    fun ton(b: ShortArray): Boolean {
        if (SystemClock.elapsedRealtime() > bis) { schliessen(); Logbuch.system("Weckwort-Mitschnitt", "Durchgang fertig, $block Blöcke"); return false }
        for (i in b.indices) { val w = b[i].toInt(); bytes[2 * i] = w.toByte(); bytes[2 * i + 1] = (w shr 8).toByte() }
        runCatching { ton.write(bytes, 0, b.size * 2) }
        block++
        return true
    }

    fun zeile(rms: Float, boden: Float, laut: Boolean, stimme: Boolean, uebersprungen: Boolean, verstaerkung: Float?, anker: Boolean, treffer: Boolean, nachgeholt: Int = 0) {
        runCatching { csv.write("${start + block - 1};${"%.0f".format(rms)};${"%.0f".format(boden)};${if (laut) 1 else 0};${if (stimme) 1 else 0};${if (uebersprungen) 1 else 0};${verstaerkung?.let { "%.2f".format(it) } ?: ""};${if (anker) 1 else 0};${if (treffer) 1 else 0};$nachgeholt\n") }
    }

    fun schliessen() {
        runCatching { ton.close() }
        runCatching { csv.close() }
        bloeckeBisher = start + block
    }

    companion object {
        @Volatile private var bisZeit = 0L
        /** Neu angefordert: Der erste Durchgang danach legt die Dateien neu an. */
        @Volatile private var neuAnlegen = false
        /** Blöcke in den Dateien aus früheren Durchgängen derselben Anforderung (Blocknummer = Lage im Ton). */
        @Volatile private var bloeckeBisher = 0

        /** Neuer Mitschnitt, wenn einer angefordert ist und noch läuft; sonst null. */
        fun oeffnen(context: Context): WeckwortMitschnitt? {
            val bis = bisZeit
            if (SystemClock.elapsedRealtime() > bis) return null
            return runCatching {
                val dir = context.applicationContext.filesDir
                val neu = neuAnlegen
                neuAnlegen = false
                if (neu) bloeckeBisher = 0
                val csv = java.io.BufferedWriter(java.io.FileWriter(File(dir, "weckwort_mitschnitt.csv"), !neu))
                csv.write(if (neu) "block;rms;boden;laut;stimme;uebersprungen;verstaerkung;anker;treffer;nachgeholt\n" else "neu\n")
                WeckwortMitschnitt(BufferedOutputStream(FileOutputStream(File(dir, "weckwort_mitschnitt.pcm"), !neu)), csv, bis, bloeckeBisher)
            }.getOrNull()
        }

        fun anfordern(sekunden: Int) {
            neuAnlegen = true
            bisZeit = SystemClock.elapsedRealtime() + sekunden.coerceIn(5, 600) * 1000L
        }
    }

    /** Empfänger nur für Shell und System (Manifest: Erlaubnis DUMP). */
    class Empfaenger : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val s = intent.getIntExtra("sekunden", 90)
            anfordern(s)
            Logbuch.system("Weckwort-Mitschnitt", "angefordert für $s s")
            // Neuer Durchgang, damit der Mitschnitt sofort beginnt.
            WeckwortDienst.neuLaden(context)
        }
    }
}
