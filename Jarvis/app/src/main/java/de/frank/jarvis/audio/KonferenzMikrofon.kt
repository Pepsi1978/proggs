package de.frank.jarvis.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import de.frank.module.transkriptionsmodul.MicRecorder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mikrofone des Konferenz-Lautsprechers (Anker PowerConf) nur fürs Zuhören im Gespräch nach dem Weckwort.
 *
 * Bluetooth kann nicht zugleich in Musikqualität ausgeben und das Mikrofon des Geräts liefern: Ist dessen Mikrofon offen
 * (Freisprechen, SCO), läuft jeder Ton in Telefonqualität. Deshalb öffnet Jarvis es nur, solange er Frank zuhört, und gibt
 * es frei, sobald die Aufnahme endet. Die Antwort kommt so wieder in voller Qualität über den Lautsprecher. Das Weckwort
 * selbst hört weiter das Handy, sonst stünde der Lautsprecher dauerhaft auf Telefonqualität.
 *
 * Nur für Geräte, deren Name [GERAET] enthält; ohne dieses Gerät bleibt alles beim Handy-Mikrofon.
 */
object KonferenzMikrofon {
    /** Teil des Gerätenamens, wie Android ihn meldet („Anker PowerConf“). */
    const val GERAET = "PowerConf"
    private const val WARTE_MS = 3_000L

    /** Hält Jarvis gerade das Mikrofon des Konferenz-Lautsprechers? */
    @Volatile var aktiv = false; private set

    /** Der Konferenz-Lautsprecher als Freisprech-Gerät, falls verbunden. */
    private fun finde(am: AudioManager): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < 31) return null
        return am.availableCommunicationDevices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && it.productName?.toString()?.contains(GERAET, ignoreCase = true) == true
        }
    }

    /**
     * Vor der Aufnahme: Mikrofon des Konferenz-Lautsprechers öffnen und warten, bis Android wirklich umgeschaltet hat.
     * false = nicht verbunden, ausgeschaltet oder Umschalten gescheitert; dann nimmt das Handy-Mikrofon auf.
     */
    suspend fun holen(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31 || !Einstellungen.get(context).konferenzMikrofonAn) return false
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return false
        val geraet = finde(am) ?: return false
        val beginn = SystemClock.elapsedRealtime()
        // Bis der Freisprech-Kanal steht, dauert es bis etwa eine Sekunde; Android meldet es über diesen Zuhörer.
        val umgeschaltet = CompletableDeferred<Unit>()
        val zuhoerer = AudioManager.OnCommunicationDeviceChangedListener { d -> if (d?.id == geraet.id) umgeschaltet.complete(Unit) }
        runCatching { am.addOnCommunicationDeviceChangedListener(context.mainExecutor, zuhoerer) }
        try {
            if (am.communicationDevice?.id == geraet.id) umgeschaltet.complete(Unit)
            val ok = runCatching { am.setCommunicationDevice(geraet) }.getOrDefault(false)
            if (!ok) {
                Logbuch.system("Konferenz-Mikrofon", "Umschalten auf ${geraet.productName} abgelehnt, Handy-Mikrofon")
                return false
            }
            aktiv = true
            if (withTimeoutOrNull(WARTE_MS) { umgeschaltet.await() } == null) {
                Logbuch.system("Konferenz-Mikrofon", "Keine Bestätigung nach ${WARTE_MS / 1000} s, Aufnahme startet trotzdem")
            }
        } finally {
            runCatching { am.removeOnCommunicationDeviceChangedListener(zuhoerer) }
        }
        // Kurz nachlaufen lassen: Der Kanal meldet sich, bevor der erste Ton sicher durchkommt.
        delay(150)
        val eingang = am.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && it.productName?.toString()?.contains(GERAET, ignoreCase = true) == true
        }
        MicRecorder.bevorzugtesGeraet = eingang
        Logbuch.dauer("Konferenz-Mikrofon an", SystemClock.elapsedRealtime() - beginn, "${geraet.productName}${if (eingang == null) ", Eingang nicht gefunden" else ""}")
        return true
    }

    /** Nach der Aufnahme: Mikrofon freigeben, der Lautsprecher spielt wieder in voller Qualität. Mehrfacher Aufruf schadet nicht. */
    fun freigeben(context: Context) {
        if (!aktiv) return
        aktiv = false
        MicRecorder.bevorzugtesGeraet = null
        if (Build.VERSION.SDK_INT < 31) return
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
        runCatching { am.clearCommunicationDevice() }
    }
}
