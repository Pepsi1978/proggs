package de.frank.jarvis.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mikrofone des Konferenz-Lautsprechers (Anker PowerConf) nur fürs Zuhören im Gesprächsmodus (Weckwort und Live-Knopf).
 *
 * Bluetooth kann nicht zugleich in Musikqualität ausgeben und das Mikrofon des Geräts liefern: Ist dessen Mikrofon offen
 * (Freisprechen, SCO), läuft jeder Ton in Telefonqualität. Deshalb öffnet Jarvis es nur, solange er Frank zuhört, und gibt
 * es frei, sobald die Aufnahme endet. Die Antwort kommt so wieder in voller Qualität über den Lautsprecher. Das Weckwort
 * selbst hört weiter das Handy, sonst stünde der Lautsprecher dauerhaft auf Telefonqualität.
 *
 * Nur für Geräte, deren Name [GERAET] enthält; ohne dieses Gerät bleibt alles beim Handy-Mikrofon.
 * Besitz: Jedes Gespräch hält das Mikrofon unter seiner Nummer; ein abgebrochenes altes Gespräch gibt so nie das des neuen frei.
 */
object KonferenzMikrofon {
    /** Teil des Gerätenamens, wie Android ihn meldet („Anker PowerConf“). */
    const val GERAET = "PowerConf"
    private const val WARTE_MS = 3_000L

    /** Hält Jarvis gerade das (bestätigte) Mikrofon des Konferenz-Lautsprechers? */
    @Volatile var aktiv = false; private set
    /** Gespräch, dem das Mikrofon gerade gehört. */
    @Volatile private var besitzer = 0
    /** Gespräch, in dem das Umschalten scheiterte: dort nicht jede Runde erneut drei Sekunden warten. */
    @Volatile private var gescheitertIn = 0
    /** Eingang des Konferenz-Lautsprechers für die laufende Aufnahme; null = Androids Wahl. */
    @Volatile var eingang: AudioDeviceInfo? = null; private set

    private fun istGeraet(d: AudioDeviceInfo) =
        d.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && d.productName?.toString()?.contains(GERAET, ignoreCase = true) == true

    /**
     * Vor der Aufnahme: Mikrofon des Konferenz-Lautsprechers öffnen und warten, bis Android wirklich umgeschaltet hat.
     * false = nicht verbunden, ausgeschaltet oder Umschalten gescheitert; dann nimmt das Handy-Mikrofon auf.
     */
    suspend fun holen(context: Context, gespraech: Int): Boolean {
        if (Build.VERSION.SDK_INT < 31 || !Einstellungen.get(context).konferenzMikrofonAn || gescheitertIn == gespraech) return false
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return false
        val geraet = am.availableCommunicationDevices.firstOrNull(::istGeraet) ?: return false
        val beginn = SystemClock.elapsedRealtime()
        // Bis der Freisprech-Kanal steht, dauert es bis etwa eine Sekunde; Android meldet es über diesen Zuhörer.
        val umgeschaltet = CompletableDeferred<Unit>()
        val zuhoerer = AudioManager.OnCommunicationDeviceChangedListener { d -> if (d?.id == geraet.id) umgeschaltet.complete(Unit) }
        runCatching { am.addOnCommunicationDeviceChangedListener(context.mainExecutor, zuhoerer) }
        besitzer = gespraech
        try {
            if (am.communicationDevice?.id == geraet.id) umgeschaltet.complete(Unit)
            if (!runCatching { am.setCommunicationDevice(geraet) }.getOrDefault(false)) {
                Logbuch.system("Konferenz-Mikrofon", "Umschalten auf ${geraet.productName} abgelehnt, Handy-Mikrofon")
                gescheitertIn = gespraech
                return false
            }
            if (withTimeoutOrNull(WARTE_MS) { umgeschaltet.await() } == null) {
                Logbuch.system("Konferenz-Mikrofon", "Keine Bestätigung nach ${WARTE_MS / 1000} s, in diesem Gespräch Handy-Mikrofon")
                gescheitertIn = gespraech
                if (besitzer == gespraech) runCatching { am.clearCommunicationDevice() }
                return false
            }
        } finally {
            runCatching { am.removeOnCommunicationDeviceChangedListener(zuhoerer) }
        }
        // Ein neueres Gespräch hat übernommen, während dieses wartete: Ihm gehört das Mikrofon jetzt.
        if (besitzer != gespraech) return false
        // Kurz nachlaufen lassen: Der Kanal meldet sich, bevor der erste Ton sicher durchkommt.
        delay(150)
        eingang = am.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull(::istGeraet)
        aktiv = true
        Logbuch.dauer("Konferenz-Mikrofon an", SystemClock.elapsedRealtime() - beginn, "${geraet.productName}${if (eingang == null) ", Eingang nicht gefunden" else ""}")
        return true
    }

    /** Steht der Freisprech-Kanal zum Konferenz-Lautsprecher gerade wirklich (etwa für den Piepton)? */
    fun kanalSteht(context: Context): Boolean {
        if (!aktiv || Build.VERSION.SDK_INT < 31) return false
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return false
        return am.communicationDevice?.let(::istGeraet) == true
    }

    /**
     * Nach der Aufnahme: Mikrofon freigeben, der Lautsprecher spielt wieder in voller Qualität. [gespraech] = nur, wenn es
     * diesem Gespräch gehört; null = in jedem Fall (Aufräumen). Mehrfacher Aufruf schadet nicht.
     */
    fun freigeben(context: Context, gespraech: Int?) {
        if (gespraech != null && gespraech != besitzer) return
        if (!aktiv && gespraech != null) return
        aktiv = false
        eingang = null
        if (Build.VERSION.SDK_INT < 31) return
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
        if (am.communicationDevice?.let(::istGeraet) == true) runCatching { am.clearCommunicationDevice() }
    }
}
