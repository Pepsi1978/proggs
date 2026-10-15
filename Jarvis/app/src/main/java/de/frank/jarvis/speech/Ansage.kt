package de.frank.jarvis.speech

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Kurze Ansage mit der Stimme von Jarvis, auch bei gesperrtem Handy: hält das Gerät wach, bis die Stimme
 * fertig ist (sie wird erst aus dem Netz geholt), und spricht mit eigener Lautstärke.
 */
object Ansage {
    private const val TAG = "JarvisAnsage"

    /** [art] wird Teil der Quelle (z. B. „abfahrt“); [lautstaerke] in Prozent der Medienlautstärke. */
    fun sprich(context: Context, art: String, titel: String, text: String, lautstaerke: Int) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Main).launch {
            val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:$art-stimme")
            runCatching { wach.acquire(120_000L) }
            val ton = app.getSystemService(AudioManager::class.java)
            val vorher = ton.getStreamVolume(AudioManager.STREAM_MUSIC)
            val hoechste = ton.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val laut = (hoechste * lautstaerke / 100.0).roundToInt().coerceIn(1, hoechste)
            try {
                // Die Meldung hat ihre eigene Lautstärke, unabhängig davon, wie leise das Handy gerade steht.
                runCatching { ton.setStreamVolume(AudioManager.STREAM_MUSIC, laut, 0) }.onFailure { Log.w(TAG, "Lautstärke ließ sich nicht setzen", it) }
                // Eigene Quelle je Meldung: Dieselbe Quelle ein zweites Mal würde das Vorlesen anhalten statt starten.
                val quelle = "$art:" + System.currentTimeMillis()
                val vorleser = Vorleser.hole(app, SecureSettings(app))
                vorleser.sprich(quelle, titel, text)
                withTimeoutOrNull(110_000L) { vorleser.stand.first { it.quelle != quelle || it.zustand == VorleseZustand.AUS } }
            } catch (f: Exception) {
                Log.w(TAG, "Vorlesen nicht möglich", f)
            } finally {
                // Zurück auf den Stand von vorher, außer Frank hat inzwischen selbst nachgeregelt.
                runCatching { if (ton.getStreamVolume(AudioManager.STREAM_MUSIC) == laut) ton.setStreamVolume(AudioManager.STREAM_MUSIC, vorher, 0) }
                runCatching { if (wach.isHeld) wach.release() }
            }
        }
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
        Ansage.sprich(context, "ansage", titel, text, Einstellungen.get(context).abfahrtLautstaerke)
    }

    companion object {
        const val AKTION = "de.frank.jarvis.ANSAGE"
        const val EXTRA_TITEL = "titel"
        const val EXTRA_TEXT = "text"
    }
}
