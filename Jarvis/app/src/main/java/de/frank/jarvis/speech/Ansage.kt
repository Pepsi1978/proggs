package de.frank.jarvis.speech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Kurze Ansage mit der Stimme von Jarvis, auch bei gesperrtem Handy: hält das Gerät wach, bis die Stimme
 * fertig ist (sie wird erst aus dem Netz geholt), und spricht mit eigener Lautstärke.
 */
object Ansage {
    private const val TAG = "JarvisAnsage"

    // Mehrere Ansagen können sich überlappen: Die Ausgangslautstärke merkt sich nur die erste, zurück stellt erst die letzte.
    private val sperre = Any()
    private class LautstaerkeStand(val ausgang: Int, var zuletztGesetzt: Int, var aktiv: Int = 0)
    private val lautstaerken = mutableMapOf<Int, LautstaerkeStand>()

    /** Auch lokal gespeicherte Erinnerungen nutzen denselben Schutz; Wecker sprechen auf ihrem eigenen Tonweg. */
    suspend fun mitLautstaerke(context: Context, prozent: Int, stream: Int = AudioManager.STREAM_MUSIC, sprechen: suspend () -> Unit) {
        if (prozent <= 0) { sprechen(); return }
        val ton = context.getSystemService(AudioManager::class.java)
        val maximum = ton.getStreamMaxVolume(stream)
        val laut = (maximum * prozent.coerceIn(1, 100) / 100.0).roundToInt().coerceIn(1, maximum.coerceAtLeast(1))
        synchronized(sperre) {
            val stand = lautstaerken.getOrPut(stream) { LautstaerkeStand(ton.getStreamVolume(stream), laut) }
            stand.aktiv++
            runCatching { ton.setStreamVolume(stream, laut, 0); stand.zuletztGesetzt = ton.getStreamVolume(stream) }
                .onFailure { Log.w(TAG, "Lautstärke ließ sich nicht setzen", it) }
        }
        try {
            sprechen()
        } finally {
            synchronized(sperre) {
                val stand = lautstaerken.getValue(stream)
                if (--stand.aktiv == 0) {
                    lautstaerken.remove(stream)
                    // Eine Änderung mit den Lautstärketasten während des Vorlesens hat Vorrang.
                    runCatching { if (ton.getStreamVolume(stream) == stand.zuletztGesetzt) ton.setStreamVolume(stream, stand.ausgang, 0) }
                        .onFailure { Log.w(TAG, "Lautstärke ließ sich nicht zurückstellen", it) }
                }
            }
        }
    }

    /** [art] wird Teil der Quelle (z. B. „abfahrt“); [lautstaerke] in Prozent der Medienlautstärke, 0 = so laut, wie das Handy steht. */
    fun sprich(context: Context, art: String, titel: String, text: String, lautstaerke: Int, wahl: StimmWahl? = null) {
        CoroutineScope(Dispatchers.Main).launch { sprichUndWarte(context, art, titel, text, lautstaerke, wahl) }
    }

    /** Wie [sprich], kehrt aber erst zurück, wenn die Stimme fertig ist (oder ein anderes Vorlesen übernommen hat). */
    suspend fun sprichUndWarte(context: Context, art: String, titel: String, text: String, lautstaerke: Int, wahl: StimmWahl? = null) {
        val app = context.applicationContext
        withContext(Dispatchers.Main) {
            val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:$art-stimme").apply { setReferenceCounted(false) }
            // Lange Texte (Agenten-Zusammenfassung) brauchen länger als zwei Minuten: Grenze nach der Textlänge, gut 12 Zeichen je Sekunde.
            val hoechstens = (60_000L + text.length * 80L).coerceIn(110_000L, 10 * 60_000L)
            runCatching { wach.acquire(hoechstens + 10_000L) }
            var quelle = ""
            try {
                mitLautstaerke(app, lautstaerke) {
                    try {
                        val vorleser = Vorleser.hole(app, SecureSettings(app))
                        // Das Vorlesen steht wegen einer Rückfrage: Die Ansage kommt obendrauf und nimmt ihm nicht seine Stelle.
                        if (vorleser.einschieben(art)) withTimeoutOrNull(hoechstens) { vorleser.einschub(text, wahl) }
                        else {
                            // Eigene Quelle je Meldung: Dieselbe Quelle ein zweites Mal würde das Vorlesen anhalten statt starten.
                            quelle = "$art:" + System.currentTimeMillis()
                            vorleser.sprich(quelle, titel, text, wahl = wahl)
                            // Steht sie wegen einer Rückfrage, bleiben Lautstärke und Wachhaltung bis zu ihrem Ende.
                            vorleser.warteAufEnde(quelle, hoechstens) { runCatching { wach.acquire(hoechstens + 10_000L) } }
                        }
                    } catch (abbruch: kotlinx.coroutines.CancellationException) {
                        // Erst verstummen, dann stellt mitLautstaerke den Tonweg zurück.
                        runCatching { Vorleser.hole(app, SecureSettings(app)).let { v -> if (quelle.isNotEmpty() && v.stand.value.quelle == quelle) v.stopp() } }
                        throw abbruch
                    }
                }
            } catch (abbruch: kotlinx.coroutines.CancellationException) {
                throw abbruch
            } catch (f: Exception) {
                Log.w(TAG, "Vorlesen nicht möglich", f)
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
            }
        }
    }

    /** Ansage anderer Apps mit den eigenen Einstellungen (Stimme, Tempo, Lautstärke). */
    fun ansage(context: Context, titel: String, text: String) {
        val p = de.frank.jarvis.meldung.Meldungen.profil(context, de.frank.jarvis.meldung.Meldungsarten.ANSAGE)
        sprich(context, "ansage", titel, text, p.lautstaerke, p.stimmWahl)
    }
}

/**
 * Nimmt Ansagen anderer eigener Apps an (z. B. UpdateStation: „Ein Update von … ist bereit“). Geschützt durch die
 * Signatur-Erlaubnis BRUECKE: Nur Apps mit Franks Schlüssel dürfen Jarvis sprechen lassen.
 */
class AnsageEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AKTION) return
        val text = intent.getStringExtra(EXTRA_TEXT)?.take(500)?.takeIf { it.isNotBlank() } ?: return
        val titel = intent.getStringExtra(EXTRA_TITEL)?.take(80) ?: "Jarvis"
        Logbuch.sprache("Ansage von $titel", text.take(120))
        val art = de.frank.jarvis.meldung.Meldungsarten.ANSAGE
        if (!de.frank.jarvis.meldung.Meldungen.an(context, art)) return
        if (de.frank.jarvis.meldung.Meldungen.inRuhe(context, art)) return
        // Wie jede Meldung: Ein laufendes Live-Gespräch wartet sie ab (höchstens 15 Minuten), mit Stimme und Lautstärke der Ansagen.
        de.frank.jarvis.meldung.Meldungen.vorlesenSpaeter(context, art, titel, text, schluessel = "$titel:$text")
    }

    companion object {
        const val AKTION = "de.frank.jarvis.ANSAGE"
        const val EXTRA_TITEL = "titel"
        const val EXTRA_TEXT = "text"
    }
}
