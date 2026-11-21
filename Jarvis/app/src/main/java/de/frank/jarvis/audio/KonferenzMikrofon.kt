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
 * es frei, sobald die Aufnahme endet. Die Antwort kommt so wieder in voller Qualität über den Lautsprecher.
 * Mit „Weckwort über den Anker“ hält der Weckwort-Dienst die Strecke unter [WECKWORT], solange er lauscht; nach dem Treffer
 * übernimmt das Gespräch sie ([uebergeben]) oder sie wird frei, das Medien-Profil kommt zurück.
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

    /** Besitzer-Nummer des Weckwort-Dienstes (Gespräche zählen ab 1). */
    const val WECKWORT = -1
    /** Hat der Weckwort-Dienst das Mikrofon für das Gespräch nach dem Treffer stehen lassen (Übergabe ohne Umschalten)? */
    @Volatile private var uebergabeSeit = 0L

    /** Wann die Strecke zuletzt freigegeben wurde (elapsedRealtime). */
    @Volatile private var freiGegebenUm = 0L

    /** Nach der Freigabe braucht das Medien-Profil etwa eine Sekunde: so lange warten, damit ein Ton nicht verschluckt wird. */
    suspend fun medienBereit() {
        val rest = 1_000L - (SystemClock.elapsedRealtime() - freiGegebenUm)
        if (freiGegebenUm > 0L && rest > 0) delay(rest)
    }

    /** Ist der Anker gerade als Freisprechgerät (Telefon-Profil) erreichbar? */
    fun verfuegbar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        // Kopfhörer verbunden: Mikrofon und Ton gehören ihnen.
        if (AnkerVerbindung.fremdesAudioGeraet(context.applicationContext) != null) return false
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return false
        return runCatching { am.availableCommunicationDevices.any(::istGeraet) }.getOrDefault(false)
    }

    private fun istGeraet(d: AudioDeviceInfo) =
        d.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && d.productName?.toString()?.contains(GERAET, ignoreCase = true) == true

    /**
     * Vor der Aufnahme: Mikrofon des Konferenz-Lautsprechers öffnen und warten, bis Android wirklich umgeschaltet hat.
     * false = nicht verbunden, ausgeschaltet oder Umschalten gescheitert; dann nimmt das Handy-Mikrofon auf.
     */
    suspend fun holen(context: Context, gespraech: Int): Boolean {
        if (Build.VERSION.SDK_INT < 31 || !Einstellungen.get(context).konferenzMikrofonAn || gescheitertIn == gespraech) return false
        // Kopfhörer verbunden: über deren Mikrofon (Androids Wahl), nie über den Anker.
        if (AnkerVerbindung.fremdesAudioGeraet(context.applicationContext) != null) return false
        // Übergabe vom Weckwort: Die Freisprech-Strecke steht schon, nur den Besitz übernehmen (kein Umschalten, kein Warten).
        if (aktiv && besitzer == WECKWORT && kanalSteht(context)) {
            besitzer = gespraech
            uebergabeSeit = 0L
            Logbuch.system("Konferenz-Mikrofon", "vom Weckwort übernommen")
            return true
        }
        return oeffnen(context, gespraech) { gescheitertIn = gespraech }
    }

    /** Für den Weckwort-Dienst: Freisprech-Strecke zum Anker öffnen; false = nicht möglich, dann lauscht das Handy. */
    suspend fun holenFuerWeckwort(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return false
        if (AnkerVerbindung.fremdesAudioGeraet(context.applicationContext) != null) return false
        // Hält oder öffnet gerade ein Gespräch das Mikrofon, nicht dazwischenfunken.
        if ((aktiv && besitzer != WECKWORT) || oeffnetGespraech != 0) return false
        if (aktiv && besitzer == WECKWORT && kanalSteht(context)) { uebergabeSeit = 0L; return true }
        return oeffnen(context, WECKWORT) {}
    }

    /**
     * Treffer über den Anker und das Gespräch hört ebenfalls über ihn zu: Strecke stehen lassen, das Gespräch übernimmt sie.
     * Holt es sie nicht binnen 20 Sekunden ab, gibt Jarvis sie frei (Medien-Profil).
     */
    fun uebergeben(context: Context) {
        if (besitzer != WECKWORT || !aktiv) return
        val seit = SystemClock.elapsedRealtime()
        uebergabeSeit = seit
        val app = context.applicationContext
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (uebergabeSeit == seit && besitzer == WECKWORT) { uebergabeSeit = 0L; freigeben(app, WECKWORT) }
        }, 20_000L)
    }

    /** Gespräch, das die Strecke gerade öffnet (0 = keins): Das Weckwort funkt dann nicht dazwischen. */
    @Volatile private var oeffnetGespraech = 0
    /** Fehlschläge des Weckworts nur einmal je Serie ins Logbuch. */
    @Volatile private var weckwortFehlerGemeldet = false

    private fun fehlerMelden(gespraech: Int, text: String) {
        if (gespraech == WECKWORT) { if (weckwortFehlerGemeldet) return; weckwortFehlerGemeldet = true }
        Logbuch.system("Konferenz-Mikrofon", text)
    }

    /** Der Weckwort-Dienst schaltet die Strecke gerade auf den Anker um. */
    @Volatile private var oeffnetWeckwort = false

    /** Schaltet gerade jemand auf den Freisprech-Kanal um? Ein Ton wüsste dann noch nicht, über welchen Weg er läuft. */
    val umschaltungLaeuft: Boolean get() = oeffnetGespraech != 0 || oeffnetWeckwort

    private suspend fun oeffnen(context: Context, gespraech: Int, gescheitert: () -> Unit): Boolean {
        if (gespraech != WECKWORT) oeffnetGespraech = gespraech else oeffnetWeckwort = true
        try { return oeffnenIntern(context, gespraech, gescheitert) } finally {
            if (gespraech != WECKWORT && oeffnetGespraech == gespraech) oeffnetGespraech = 0
            if (gespraech == WECKWORT) oeffnetWeckwort = false
        }
    }

    private suspend fun oeffnenIntern(context: Context, gespraech: Int, gescheitert: () -> Unit): Boolean {
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return false
        val geraet = am.availableCommunicationDevices.firstOrNull(::istGeraet) ?: run {
            if (gespraech == WECKWORT) fehlerMelden(gespraech, "Anker noch nicht als Freisprechgerät bereit, Handy-Mikrofon")
            return false
        }
        val beginn = SystemClock.elapsedRealtime()
        // Bis der Freisprech-Kanal steht, dauert es bis etwa eine Sekunde; Android meldet es über diesen Zuhörer.
        val umgeschaltet = CompletableDeferred<Unit>()
        val zuhoerer = AudioManager.OnCommunicationDeviceChangedListener { d -> if (d?.id == geraet.id) umgeschaltet.complete(Unit) }
        runCatching { am.addOnCommunicationDeviceChangedListener(context.mainExecutor, zuhoerer) }
        besitzer = gespraech
        try {
            if (am.communicationDevice?.id == geraet.id) umgeschaltet.complete(Unit)
            if (!runCatching { am.setCommunicationDevice(geraet) }.getOrDefault(false)) {
                fehlerMelden(gespraech, "Umschalten auf ${geraet.productName} abgelehnt, Handy-Mikrofon")
                gescheitert()
                return false
            }
            if (withTimeoutOrNull(WARTE_MS) { umgeschaltet.await() } == null) {
                fehlerMelden(gespraech, "Keine Bestätigung nach ${WARTE_MS / 1000} s, Handy-Mikrofon")
                gescheitert()
                if (besitzer == gespraech) runCatching { am.clearCommunicationDevice() }
                return false
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Abgebrochen, während der Kanal umschaltete (neues „Hey Jarvis“, Sessionwechsel): Sonst bliebe der Anker im Telefon-Profil,
            // denn freigeben() greift erst, wenn der Kanal als aktiv gilt.
            if (besitzer == gespraech) runCatching { am.clearCommunicationDevice() }
            throw e
        } finally {
            runCatching { am.removeOnCommunicationDeviceChangedListener(zuhoerer) }
        }
        // Ein neueres Gespräch hat übernommen, während dieses wartete: Ihm gehört das Mikrofon jetzt.
        if (besitzer != gespraech) return false
        // Kurz nachlaufen lassen: Der Kanal meldet sich, bevor der erste Ton sicher durchkommt.
        delay(150)
        eingang = am.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull(::istGeraet)
        aktiv = true
        if (gespraech == WECKWORT) weckwortFehlerGemeldet = false
        Logbuch.dauer(if (gespraech == WECKWORT) "Konferenz-Mikrofon an (Weckwort)" else "Konferenz-Mikrofon an", SystemClock.elapsedRealtime() - beginn, "${geraet.productName}${if (eingang == null) ", Eingang nicht gefunden" else ""}")
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
        // Aufräumen ohne Nummer (Ende des ViewModels) lässt die Strecke des Weckworts stehen.
        if (gespraech == null && besitzer == WECKWORT && aktiv) return
        uebergabeSeit = 0L
        aktiv = false
        eingang = null
        freiGegebenUm = SystemClock.elapsedRealtime()
        if (Build.VERSION.SDK_INT < 31) return
        val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
        if (am.communicationDevice?.let(::istGeraet) == true) runCatching { am.clearCommunicationDevice() }
    }
}
