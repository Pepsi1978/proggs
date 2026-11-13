package de.frank.jarvis.staubsauger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Bedienungshilfe „Jarvis Staubsauger“: bedient die Proscenic-App (M7 Pro) wie ein Finger, damit Jarvis den Saugroboter
 * per Sprache oder Startkarte starten kann, ohne ihn aus der Proscenic-App zu lösen. Sie tut von selbst nichts; sie merkt
 * sich nur, welche App vorn war, um nach dem Tippen dorthin zurückzukehren.
 */
class ProscenicBedienung : AccessibilityService() {
    override fun onServiceConnected() { instanz = this }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paket = event.packageName?.toString() ?: return
        if (paket != ProscenicSteuerung.PAKET && paket != "com.android.systemui") zuletztVorn = paket
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean { if (instanz === this) instanz = null; return super.onUnbind(intent) }
    override fun onDestroy() { if (instanz === this) instanz = null; super.onDestroy() }

    companion object {
        @Volatile var instanz: ProscenicBedienung? = null
        @Volatile var zuletztVorn: String? = null

        /** Ist die Bedienungshilfe in den Android-Einstellungen eingeschaltet? */
        fun eingeschaltet(context: Context): Boolean {
            val eigen = ComponentName(context, ProscenicBedienung::class.java).flattenToString()
            val liste = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return liste.split(':').any { it.equals(eigen, ignoreCase = true) || it.equals(eigen.replace("/de.frank.jarvis.", "/."), ignoreCase = true) }
        }
    }
}

/** Was eine Aktion am Saugroboter ergab. */
data class SaugerAusgang(val text: String, val fehler: Boolean = false)

/** Zuletzt gelesener Stand des M7 Pro (für die Startkarte), auch über einen Neustart von Jarvis hinweg. */
object SaugerStand {
    var akku by mutableStateOf<Int?>(null)
    var status by mutableStateOf<String?>(null)
    var gelesenUm by mutableStateOf<Long?>(null)
    var laeuft by mutableStateOf(false)
    var meldung by mutableStateOf<SaugerAusgang?>(null)
    private var geladen = false

    fun laden(context: Context) {
        if (geladen) return
        geladen = true
        val p = context.getSharedPreferences("staubsauger", Context.MODE_PRIVATE)
        akku = p.getInt("akku", -1).takeIf { it >= 0 }
        status = p.getString("status", null)
        gelesenUm = p.getLong("gelesen_um", 0L).takeIf { it > 0 }
    }

    fun merken(context: Context, akku: Int?, status: String?) {
        if (akku != null) this.akku = akku
        if (!status.isNullOrBlank()) this.status = status
        gelesenUm = System.currentTimeMillis()
        context.getSharedPreferences("staubsauger", Context.MODE_PRIVATE).edit()
            .putInt("akku", this.akku ?: -1).putString("status", this.status).putLong("gelesen_um", gelesenUm ?: 0L).apply()
    }
}

/**
 * Tippt in der Proscenic-App. Ablauf je Aktion: App öffnen, Gerätebildschirm des M7 Pro abwarten (aus der Geräteliste
 * notfalls „M7 Pro“ antippen), Knopf drücken, Stand ablesen, zur vorher offenen App zurück. Die Kennungen der Knöpfe
 * stammen aus Proscenic 1.5.5 (ausgelesen am 13.11.2026).
 */
object ProscenicSteuerung {
    const val PAKET = "com.proscenic.robot"

    enum class Aktion(val wort: String) {
        REINIGEN("reinigen"), TIEFENREINIGUNG("tiefenreinigung"), STOPPEN("stoppen"), STATION("station"),
        POSITIONIEREN("positionieren"), STAUBSAMMLUNG("staubsammlung"), STATUS("status");

        companion object { fun aus(wort: String): Aktion? = entries.firstOrNull { it.wort == wort.trim().lowercase() } }
    }

    private const val KNOPF_REINIGEN = "ll_clean_openorstop"
    private const val TEXT_REINIGEN = "tv_clean_openorstop"
    private const val KNOPF_MEHR = "ll_more_setting"
    private const val KNOPF_STATION = "ll_charge_state"
    private const val ZUSTAND = "tv_act_clear_state"
    private const val AKKU = "tv_act_clear_elec"
    private const val MEHR_TIEFENREINIGUNG = "ll_depthTotalClean"
    private const val MEHR_POSITIONIEREN = "ll_pop_window_more_location_reboot"
    private const val MEHR_STAUBSAMMLUNG = "ll_startDustCenter"
    private val BESTAETIGEN = listOf("Bestätigen", "OK", "Ok", "Ja", "Starten", "Start", "Sicher", "Confirm")

    private val sperre = Mutex()

    suspend fun ausfuehren(context: Context, aktion: Aktion): SaugerAusgang = sperre.withLock {
        SaugerStand.laden(context)
        SaugerStand.laeuft = true
        val ausgang = try {
            withContext(Dispatchers.Default) { ablauf(context.applicationContext, aktion) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            SaugerAusgang("Die Proscenic-App ließ sich nicht bedienen: ${e.message ?: e.javaClass.simpleName}.", fehler = true)
        } finally {
            SaugerStand.laeuft = false
        }
        SaugerStand.meldung = ausgang
        ausgang
    }

    private suspend fun ablauf(context: Context, aktion: Aktion): SaugerAusgang {
        val dienst = ProscenicBedienung.instanz
            ?: return SaugerAusgang(
                "Die Bedienungshilfe „Jarvis Staubsauger“ ist aus. Schalte sie in den Android-Einstellungen unter Bedienungshilfen, Installierte Apps ein.",
                fehler = true,
            )
        val km = context.getSystemService(KeyguardManager::class.java)
        val pm = context.getSystemService(PowerManager::class.java)
        if (km?.isKeyguardLocked == true || pm?.isInteractive == false)
            return SaugerAusgang("Das Gerät ist gesperrt. Entsperre es kurz, dann kann ich die Proscenic-App bedienen.", fehler = true)
        val start = context.packageManager.getLaunchIntentForPackage(PAKET)
            ?: return SaugerAusgang("Die Proscenic-App ist auf diesem Gerät nicht installiert.", fehler = true)

        val vorher = dienst.rootInActiveWindow?.packageName?.toString()?.takeIf { it != PAKET && it != "com.android.systemui" }
            ?: ProscenicBedienung.zuletztVorn
        dienst.startActivity(start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        try {
            if (!hauptbildschirm(dienst)) return SaugerAusgang("Der Bildschirm des M7 Pro in der Proscenic-App ließ sich nicht öffnen.", fehler = true)
            val vorZustand = text(dienst, ZUSTAND)
            val knopf = text(dienst, TEXT_REINIGEN).orEmpty()
            val reinigtGerade = knopf.isNotBlank() && !knopf.contains("reinig", ignoreCase = true)
            val antwort: String = when (aktion) {
                Aktion.STATUS -> ""
                Aktion.REINIGEN -> {
                    if (reinigtGerade) "Der M7 Pro reinigt schon."
                    else { klick(dienst, KNOPF_REINIGEN); bestaetigen(dienst); "Reinigung gestartet." }
                }
                Aktion.STOPPEN -> {
                    if (!reinigtGerade) "Der M7 Pro reinigt gerade nicht."
                    else { klick(dienst, KNOPF_REINIGEN); "Reinigung angehalten." }
                }
                Aktion.STATION -> {
                    if (vorZustand.orEmpty().contains("lade", ignoreCase = true)) "Der M7 Pro steht schon in der Ladestation."
                    else { klick(dienst, KNOPF_STATION); bestaetigen(dienst); "Der M7 Pro fährt zurück zur Ladestation." }
                }
                Aktion.TIEFENREINIGUNG -> mehr(dienst, MEHR_TIEFENREINIGUNG, "Tiefenreinigung gestartet.")
                Aktion.POSITIONIEREN -> mehr(dienst, MEHR_POSITIONIEREN, "Der M7 Pro meldet sich, damit du ihn findest.")
                Aktion.STAUBSAMMLUNG -> mehr(dienst, MEHR_STAUBSAMMLUNG, "Staubsammlung gestartet.")
            }
            if (aktion != Aktion.STATUS) delay(3500)
            // Ein noch offenes Mehr-Menü schließen, damit Zustand und Akku lesbar sind.
            if (finde(dienst, MEHR_TIEFENREINIGUNG) != null) { dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); delay(800) }
            val zustand = text(dienst, ZUSTAND)
            val akku = text(dienst, AKKU)?.filter { it.isDigit() }?.toIntOrNull()
            SaugerStand.merken(context, akku, zustand)
            val stand = listOfNotNull(zustand?.let { "Zustand: $it" }, akku?.let { "Akku $it Prozent" }).joinToString(", ")
            return SaugerAusgang(listOf(antwort, stand).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Kein Stand lesbar." })
        } finally {
            zurueck(dienst, context, vorher)
        }
    }

    /** Öffnet das Mehr-Menü, tippt den Eintrag [id] und bestätigt eine Rückfrage. */
    private suspend fun mehr(dienst: AccessibilityService, id: String, erfolg: String): String {
        if (finde(dienst, id) == null) {
            klick(dienst, KNOPF_MEHR)
            warte { finde(dienst, id) != null } || throw IllegalStateException("das Menü „Mehr“ öffnete sich nicht")
        }
        klick(dienst, id)
        bestaetigen(dienst)
        return erfolg
    }

    /** Wartet bis zu 15 Sekunden auf den Gerätebildschirm des M7 Pro; aus der Geräteliste tippt es „M7 Pro“ an. */
    private suspend fun hauptbildschirm(dienst: AccessibilityService): Boolean {
        val ende = System.currentTimeMillis() + 15_000
        var getippt = 0L
        while (System.currentTimeMillis() < ende) {
            val wurzel = dienst.rootInActiveWindow
            if (wurzel?.packageName == PAKET) {
                if (finde(dienst, KNOPF_REINIGEN) != null) { delay(700); return true }
                if (System.currentTimeMillis() - getippt > 3000) {
                    wurzel.findAccessibilityNodeInfosByText("M7 Pro").firstOrNull { it.isVisibleToUser }?.let { tippe(dienst, it); getippt = System.currentTimeMillis() }
                }
            }
            delay(400)
        }
        return false
    }

    /** Tippt eine Bestätigung, falls die App nachfragt (bis 2,5 Sekunden). */
    private suspend fun bestaetigen(dienst: AccessibilityService) {
        val ende = System.currentTimeMillis() + 2500
        while (System.currentTimeMillis() < ende) {
            delay(500)
            val wurzel = dienst.rootInActiveWindow ?: continue
            if (wurzel.packageName != PAKET) continue
            for (wort in BESTAETIGEN) {
                val knoten = wurzel.findAccessibilityNodeInfosByText(wort).firstOrNull { n ->
                    n.isVisibleToUser && n.text?.toString()?.trim()?.equals(wort, ignoreCase = true) == true
                }
                if (knoten != null) { tippe(dienst, knoten); return }
            }
        }
    }

    private suspend fun warte(sekunden: Int = 5, bedingung: () -> Boolean): Boolean {
        val ende = System.currentTimeMillis() + sekunden * 1000L
        while (System.currentTimeMillis() < ende) { if (bedingung()) return true; delay(300) }
        return false
    }

    private fun finde(dienst: AccessibilityService, id: String): AccessibilityNodeInfo? =
        dienst.rootInActiveWindow?.takeIf { it.packageName == PAKET }
            ?.findAccessibilityNodeInfosByViewId("$PAKET:id/$id")?.firstOrNull { it.isVisibleToUser }

    private fun text(dienst: AccessibilityService, id: String): String? = finde(dienst, id)?.text?.toString()?.trim()?.ifBlank { null }

    private suspend fun klick(dienst: AccessibilityService, id: String) {
        val knoten = finde(dienst, id) ?: throw IllegalStateException("der Knopf „$id“ fehlt in der Proscenic-App")
        tippe(dienst, knoten)
        delay(900)
    }

    /** Tippt über die Bedienungshilfe; reagiert der Knoten nicht, tippt eine Geste auf seine Mitte. */
    private fun tippe(dienst: AccessibilityService, knoten: AccessibilityNodeInfo) {
        var k: AccessibilityNodeInfo? = knoten
        while (k != null && !k.isClickable) k = k.parent
        if (k?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
        val r = Rect().also { knoten.getBoundsInScreen(it) }
        val pfad = Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }
        dienst.dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(pfad, 0, 60)).build(), null, null)
    }

    /** Zurück zur App, die vorher vorn war; ohne bekannte App auf den Startbildschirm. */
    private fun zurueck(dienst: AccessibilityService, context: Context, vorher: String?) {
        val intent = vorher?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (intent != null) runCatching { dienst.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onSuccess { return }
        dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
}
