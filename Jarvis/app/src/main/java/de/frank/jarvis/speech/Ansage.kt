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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Kurze Ansage mit der Stimme von Jarvis, auch bei gesperrtem Handy: hält das Gerät wach, bis die Stimme
 * fertig ist (sie wird erst aus dem Netz geholt), und spricht mit eigener Lautstärke.
 */
object Ansage {
    private const val TAG = "JarvisAnsage"

    // Verschachtelte Ansagen: Nach der jüngsten gilt wieder die Wahl der vorherigen, am Ende die Handy-Lautstärke.
    private val sperre = Any()
    private class LautstaerkeWahl(var laut: Int)
    private class LautstaerkeStand(var ausgang: Int, var zuletztGesetzt: Int) {
        val wahlen = mutableListOf<LautstaerkeWahl>()
        /** Eine Änderung mit den Lautstärketasten gilt auch nach der nächsten eingeschobenen Ansage. */
        fun manuell(jetzt: Int) {
            if (jetzt == zuletztGesetzt) return
            ausgang = jetzt
            wahlen.forEach { it.laut = jetzt }
            zuletztGesetzt = jetzt
        }
    }
    private val lautstaerken = mutableMapOf<Int, LautstaerkeStand>()

    /** Auch lokal gespeicherte Erinnerungen nutzen denselben Schutz; Wecker sprechen auf ihrem eigenen Tonweg. */
    suspend fun mitLautstaerke(context: Context, prozent: Int, stream: Int = AudioManager.STREAM_MUSIC, sprechen: suspend () -> Unit) {
        currentCoroutineContext().ensureActive()
        val ton = context.getSystemService(AudioManager::class.java)
        // Ein nicht lesbarer Tonweg darf die Erinnerung nicht ganz verstummen lassen.
        val wahl = runCatching {
            synchronized(sperre) {
                val jetzt = ton.getStreamVolume(stream)
                val maximum = ton.getStreamMaxVolume(stream)
                val minimum = if (android.os.Build.VERSION.SDK_INT >= 28) ton.getStreamMinVolume(stream) else 0
                val stand = lautstaerken.getOrPut(stream) { LautstaerkeStand(jetzt, jetzt) }
                stand.manuell(jetzt)
                val laut = if (prozent <= 0) stand.ausgang else
                    (maximum * prozent.coerceIn(1, 100) / 100.0).roundToInt().coerceIn(minOf(maxOf(1, minimum), maximum), maximum)
                LautstaerkeWahl(laut).also {
                    stand.wahlen.add(it)
                    setzeLautstaerke(ton, stream, stand, laut)
                }
            }
        }.onFailure { Log.w(TAG, "Tonweg nicht lesbar, Vorlesen mit Handy-Lautstärke", it) }.getOrNull()
        if (wahl == null) {
            sprechen()
            return
        }
        try {
            sprechen()
        } finally {
            synchronized(sperre) {
                val stand = lautstaerken.getValue(stream)
                val warLetzte = stand.wahlen.lastOrNull() === wahl
                stand.wahlen.remove(wahl)
                try {
                    stand.manuell(ton.getStreamVolume(stream))
                    if (warLetzte) setzeLautstaerke(ton, stream, stand, stand.wahlen.lastOrNull()?.laut ?: stand.ausgang)
                } catch (f: Exception) {
                    Log.w(TAG, "Lautstärke ließ sich nicht zurückstellen", f)
                } finally {
                    if (stand.wahlen.isEmpty()) lautstaerken.remove(stream)
                }
            }
        }
    }

    private fun setzeLautstaerke(ton: AudioManager, stream: Int, stand: LautstaerkeStand, laut: Int) {
        if (laut == stand.zuletztGesetzt) return
        runCatching { ton.setStreamVolume(stream, laut, 0); stand.zuletztGesetzt = ton.getStreamVolume(stream) }
            .onFailure { Log.w(TAG, "Lautstärke ließ sich nicht setzen", it) }
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
                            val ende = vorleser.warteAufEnde(quelle, hoechstens) { runCatching { wach.acquire(hoechstens + 10_000L) } }
                            // Die Frist darf nicht enden, während die eigene Stimme danach ohne Lautstärkeschutz weiterläuft.
                            if (ende == null && vorleser.stand.value.quelle == quelle) vorleser.stopp()
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
        de.frank.jarvis.meldung.Meldungen.ereignis(context, de.frank.jarvis.meldung.Meldungsarten.ANSAGE, 4_493, titel, text)
    }
}

/**
 * Nimmt Ansagen anderer eigener Apps an (z. B. UpdateStation: „Ein Update von … ist bereit“). Nur Apps mit Franks Schlüssel
 * dürfen Jarvis sprechen lassen. Geprüft wird im Code über die mitgeschickte Absender-Kennung (Absender setzt
 * `BroadcastOptions.setShareIdentityEnabled`, ab Android 14), nicht über die geteilte Erlaubnis BRUECKE (Almanach §2.12).
 */
class AnsageEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AKTION) return
        val absender = if (android.os.Build.VERSION.SDK_INT >= 34) sentFromUid else -1
        if (absender == -1 || !de.frank.jarvis.faehigkeit.BrueckenSchutz.eigeneSignatur(context, absender)) {
            Logbuch.fehler("Ansage abgelehnt", null, "Absender ohne Franks Schlüssel oder ohne Kennung (uid $absender)")
            return
        }
        val text = intent.getStringExtra(EXTRA_TEXT)?.take(500)?.takeIf { it.isNotBlank() } ?: return
        val titel = intent.getStringExtra(EXTRA_TITEL)?.take(80) ?: "Jarvis"
        Logbuch.sprache("Ansage von $titel", text.take(120))
        val art = de.frank.jarvis.meldung.Meldungsarten.ANSAGE
        if (!de.frank.jarvis.meldung.Meldungen.an(context, art)) return
        if (de.frank.jarvis.meldung.Meldungen.inRuhe(context, art)) return
        // Wie jede Meldung: Ein laufendes Live-Gespräch wartet sie ab (höchstens 15 Minuten), mit Stimme und Lautstärke der Ansagen.
        Ansage.ansage(context, titel, text)
    }

    companion object {
        const val AKTION = "de.frank.jarvis.ANSAGE"
        const val EXTRA_TITEL = "titel"
        const val EXTRA_TEXT = "text"
    }
}
