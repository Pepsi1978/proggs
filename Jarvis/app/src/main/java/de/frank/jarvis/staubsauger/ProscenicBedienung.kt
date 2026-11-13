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
import de.frank.jarvis.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Bedienungshilfe „Jarvis Staubsauger“: bedient die Proscenic-App (M7 Pro) wie ein Finger, damit Jarvis den Saugroboter
 * per Sprache oder Startkarte starten kann, ohne ihn aus der Proscenic-App zu lösen. Sie tut von selbst nichts; sie merkt
 * sich nur, welche startbare App zuletzt vorn war, um nach dem Tippen dorthin zurückzukehren.
 */
class ProscenicBedienung : AccessibilityService() {
    /** Paket → hat einen Startbildschirm-Eintrag (Tastaturen, Overlays und Systemfenster haben keinen). */
    private val startbar = HashMap<String, Boolean>()

    override fun onServiceConnected() { instanz = this }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paket = event.packageName?.toString() ?: return
        if (paket == ProscenicSteuerung.PAKET) return
        if (startbar.getOrPut(paket) { packageManager.getLaunchIntentForPackage(paket) != null }) zuletztVorn = paket
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean { if (instanz === this) instanz = null; return super.onUnbind(intent) }
    override fun onDestroy() { if (instanz === this) instanz = null; super.onDestroy() }

    companion object {
        @Volatile var instanz: ProscenicBedienung? = null
        @Volatile var zuletztVorn: String? = null

        /** Ist die Bedienungshilfe in den Android-Einstellungen eingeschaltet? */
        fun eingeschaltet(context: Context): Boolean {
            val eigen = ComponentName(context, ProscenicBedienung::class.java)
            val liste = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return liste.split(':').any { ComponentName.unflattenFromString(it) == eigen }
        }
    }
}

/** Was eine Aktion am Saugroboter ergab. */
data class SaugerAusgang(val text: String, val fehler: Boolean = false)

/** Zuletzt gelesener Stand des M7 Pro (für die Startkarte), auch über einen Neustart von Jarvis hinweg. */
object SaugerStand {
    var akku by mutableStateOf<Int?>(null)
    var status by mutableStateOf<String?>(null)
    /** Saugstufe laut Proscenic-App, etwa „Leistungsstarker Modus“. */
    var modus by mutableStateOf<String?>(null)
    /** Sekunden seit 1970, wann Akku oder Zustand zuletzt wirklich abgelesen wurden. */
    var gelesenUm by mutableStateOf<Long?>(null)
    var laeuft by mutableStateOf(false)
    var meldung by mutableStateOf<SaugerAusgang?>(null)
    private var geladen = false

    @Synchronized
    fun laden(context: Context) {
        if (geladen) return
        geladen = true
        val p = context.getSharedPreferences("staubsauger", Context.MODE_PRIVATE)
        akku = p.getInt("akku", -1).takeIf { it >= 0 }
        status = p.getString("status", null)
        modus = p.getString("modus", null)
        gelesenUm = p.getLong("gelesen_s", 0L).takeIf { it > 0 }
    }

    /** Merkt nur, was wirklich abgelesen wurde; ohne Wert bleibt auch die Zeit beim alten Stand. */
    @Synchronized
    fun merken(context: Context, akku: Int?, status: String?, modus: String? = null) {
        if (akku == null && status.isNullOrBlank()) return
        if (akku != null) this.akku = akku
        if (!status.isNullOrBlank()) this.status = status
        if (!modus.isNullOrBlank()) this.modus = modus
        gelesenUm = System.currentTimeMillis() / 1000
        context.getSharedPreferences("staubsauger", Context.MODE_PRIVATE).edit()
            .putInt("akku", this.akku ?: -1).putString("status", this.status).putString("modus", this.modus).putLong("gelesen_s", gelesenUm ?: 0L).apply()
    }
}

/**
 * Tippt in der Proscenic-App. Ablauf je Aktion: App öffnen, Gerätebildschirm des M7 Pro abwarten (aus der Geräteliste
 * „M7 Pro“ antippen, aus Unterseiten zurückgehen), Knopf drücken, Stand ablesen, zur vorher offenen App zurück.
 * Kennungen und Beschriftungen stammen aus Proscenic 1.5.5 auf Deutsch (ausgelesen am 13.11.2026). Erkennt Jarvis eine
 * Beschriftung nicht, tippt es lieber nicht und sagt, was dasteht.
 */
object ProscenicSteuerung {
    const val PAKET = "com.proscenic.robot"

    enum class Aktion(val wort: String) {
        REINIGEN("reinigen"), TIEFENREINIGUNG("tiefenreinigung"), STOPPEN("stoppen"), STATION("station"),
        POSITIONIEREN("positionieren"), STAUBSAMMLUNG("staubsammlung"), WISCHEN("wischen"), STATUS("status");

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
    private const val MEHR_WISCHEN = "ll_y_word_2"
    private const val MODUS = "tv_clean_mode"
    /** Bestätigungsknöpfe einer Rückfrage; bewusst ohne „Start“, das auch als Beschriftung anderer Knöpfe vorkommt. */
    private val BESTAETIGEN = listOf("Bestätigen", "OK", "Ja", "Fortfahren")

    /** Zustand des Reinigen-Knopfs nach seiner Beschriftung. */
    private enum class Knopf { BEREIT, REINIGT, PAUSIERT, UNBEKANNT }

    private fun knopfZustand(text: String): Knopf {
        val t = text.lowercase()
        return when {
            t.contains("paus") || t.contains("stopp") || t.contains("anhalten") -> Knopf.REINIGT
            t.contains("fortsetz") || t.contains("weiter") -> Knopf.PAUSIERT
            t.contains("reinig") || t.contains("start") -> Knopf.BEREIT
            else -> Knopf.UNBEKANNT
        }
    }

    private val sperre = Mutex()

    suspend fun ausfuehren(context: Context, aktion: Aktion): SaugerAusgang = sperre.withLock {
        SaugerStand.laden(context)
        SaugerStand.laeuft = true
        SaugerStand.meldung = null
        val ausgang = try {
            withContext(Dispatchers.Default) { ablauf(context.applicationContext, aktion) }
        } catch (e: CancellationException) {
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
                "Die Bedienungshilfe „Jarvis Staubsauger“ ist aus. Schalte sie in den Android-Einstellungen unter Bedienungshilfen, " +
                    "Installierte Apps ein. Ist der Schalter gesperrt, erst in den App-Infos von Jarvis oben rechts „Eingeschränkte Einstellungen zulassen“ tippen.",
                fehler = true,
            )
        val km = context.getSystemService(KeyguardManager::class.java)
        val pm = context.getSystemService(PowerManager::class.java)
        if (km?.isKeyguardLocked == true || pm?.isInteractive == false)
            return SaugerAusgang("Das Gerät ist gesperrt. Entsperre es kurz, dann kann ich die Proscenic-App bedienen.", fehler = true)
        val start = context.packageManager.getLaunchIntentForPackage(PAKET)
            ?: return SaugerAusgang("Die Proscenic-App ist auf diesem Gerät nicht installiert.", fehler = true)

        val vorher = when {
            MainActivity.sichtbar -> context.packageName
            else -> dienst.rootInActiveWindow?.packageName?.toString()
                ?.takeIf { it != PAKET && context.packageManager.getLaunchIntentForPackage(it) != null }
                ?: ProscenicBedienung.zuletztVorn
        }
        dienst.startActivity(start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            if (!hauptbildschirm(dienst)) return SaugerAusgang("Der Bildschirm des M7 Pro in der Proscenic-App ließ sich nicht öffnen.", fehler = true)
            val vorZustand = text(dienst, ZUSTAND).orEmpty()
            val knopfText = text(dienst, TEXT_REINIGEN).orEmpty()
            val knopf = knopfZustand(knopfText)
            val antwort: String = when (aktion) {
                Aktion.STATUS -> ""
                Aktion.REINIGEN -> when (knopf) {
                    Knopf.REINIGT -> "Der M7 Pro reinigt schon."
                    Knopf.BEREIT -> { klick(dienst, KNOPF_REINIGEN); "Reinigung gestartet." }
                    Knopf.PAUSIERT -> { klick(dienst, KNOPF_REINIGEN); "Reinigung fortgesetzt." }
                    Knopf.UNBEKANNT -> return unbekannt(knopfText)
                }
                Aktion.STOPPEN -> when (knopf) {
                    Knopf.REINIGT -> { klick(dienst, KNOPF_REINIGEN); "Reinigung angehalten." }
                    Knopf.BEREIT, Knopf.PAUSIERT -> "Der M7 Pro reinigt gerade nicht."
                    Knopf.UNBEKANNT -> return unbekannt(knopfText)
                }
                Aktion.STATION -> {
                    val z = vorZustand.lowercase()
                    when {
                        z.contains("rück") || z.contains("zurück") -> "Der M7 Pro fährt schon zur Ladestation."
                        z.contains("ladevorgang") || z.contains("lädt") || z.contains("geladen") || z.contains("aufladen") -> "Der M7 Pro steht schon in der Ladestation."
                        else -> { klick(dienst, KNOPF_STATION); "Der M7 Pro fährt zurück zur Ladestation." }
                    }
                }
                Aktion.TIEFENREINIGUNG ->
                    if (knopf == Knopf.REINIGT) "Der M7 Pro reinigt gerade schon; halte ihn erst an, dann starte ich die Tiefenreinigung."
                    else mehr(dienst, MEHR_TIEFENREINIGUNG, "Tiefenreinigung ausgelöst.")
                Aktion.POSITIONIEREN -> mehr(dienst, MEHR_POSITIONIEREN, "Gerät positionieren ausgelöst; der M7 Pro meldet sich.")
                Aktion.STAUBSAMMLUNG ->
                    if (knopf == Knopf.REINIGT) "Der M7 Pro reinigt gerade; die Staubsammlung geht erst in der Station."
                    else mehr(dienst, MEHR_STAUBSAMMLUNG, "Staubsammlung ausgelöst.")
                Aktion.WISCHEN -> mehr(dienst, MEHR_WISCHEN, "Y-förmiges Wischen ausgelöst.")
            }
            if (aktion != Aktion.STATUS) delay(3500)
            // Ein noch offenes Mehr-Menü schließen, damit Zustand und Akku lesbar sind.
            if (finde(dienst, MEHR_TIEFENREINIGUNG) != null) { dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); delay(800) }
            val zustand = text(dienst, ZUSTAND)
            val akku = text(dienst, AKKU)?.let { Regex("\\d{1,3}").find(it)?.value?.toIntOrNull() }?.takeIf { it in 0..100 }
            val modus = text(dienst, MODUS)
            SaugerStand.merken(context, akku, zustand, modus)
            val stand = listOfNotNull(zustand?.let { "Zustand: $it." }, akku?.let { "Akku $it Prozent." }, modus?.let { "Saugstufe: $it." }).joinToString(" ")
            return SaugerAusgang(listOf(antwort, stand).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Kein Stand lesbar." })
        } finally {
            zurueck(dienst, context, vorher)
        }
    }

    private fun unbekannt(knopfText: String) = SaugerAusgang(
        "Ich erkenne den Zustand in der Proscenic-App nicht (der Knopf zeigt „${knopfText.ifBlank { "nichts" }}“) und tippe lieber nichts an.",
        fehler = true,
    )

    /** Öffnet das Mehr-Menü, tippt den Eintrag [id] und bestätigt eine Rückfrage. */
    private suspend fun mehr(dienst: AccessibilityService, id: String, erfolg: String): String {
        if (finde(dienst, id) == null) {
            klick(dienst, KNOPF_MEHR)
            if (!warte { finde(dienst, id) != null }) throw IllegalStateException("das Menü „Mehr“ öffnete sich nicht")
        }
        klick(dienst, id)
        bestaetigen(dienst)
        return erfolg
    }

    /**
     * Wartet bis zu 15 Sekunden auf den Gerätebildschirm des M7 Pro. Aus der Geräteliste tippt es „M7 Pro“ an,
     * von einer Unterseite (Karte, Einstellungen) geht es höchstens viermal zurück.
     */
    private suspend fun hauptbildschirm(dienst: AccessibilityService): Boolean {
        val ende = System.currentTimeMillis() + 15_000
        var letzterVersuch = System.currentTimeMillis()
        var zurueckGegangen = 0
        while (System.currentTimeMillis() < ende) {
            val wurzeln = wurzeln(dienst)
            if (wurzeln.isNotEmpty()) {
                if (finde(dienst, KNOPF_REINIGEN) != null) { delay(700); return true }
                if (System.currentTimeMillis() - letzterVersuch > 2500) {
                    letzterVersuch = System.currentTimeMillis()
                    val eintrag = wurzeln.firstNotNullOfOrNull { w -> w.findAccessibilityNodeInfosByText("M7 Pro").firstOrNull { it.isVisibleToUser && klickbar(it) } }
                    if (eintrag != null) tippe(dienst, eintrag)
                    else if (zurueckGegangen < 4) { zurueckGegangen++; dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }
                }
            }
            delay(400)
        }
        return false
    }

    /** Tippt eine Bestätigung, falls die App nachfragt (bis 2,5 Sekunden); true, wenn eine getippt wurde. */
    private suspend fun bestaetigen(dienst: AccessibilityService): Boolean {
        val ende = System.currentTimeMillis() + 2500
        while (System.currentTimeMillis() < ende) {
            delay(500)
            for (w in wurzeln(dienst)) for (wort in BESTAETIGEN) {
                val knoten = w.findAccessibilityNodeInfosByText(wort).firstOrNull { n ->
                    n.isVisibleToUser && n.text?.toString()?.trim()?.equals(wort, ignoreCase = true) == true
                }
                if (knoten != null) { tippe(dienst, knoten); return true }
            }
        }
        return false
    }

    private suspend fun warte(sekunden: Int = 5, bedingung: () -> Boolean): Boolean {
        val ende = System.currentTimeMillis() + sekunden * 1000L
        while (System.currentTimeMillis() < ende) { if (bedingung()) return true; delay(300) }
        return false
    }

    /** Alle Fenster der Proscenic-App, das oberste zuerst (Menüs öffnen als eigenes Pop-up-Fenster). */
    private fun wurzeln(dienst: AccessibilityService): List<AccessibilityNodeInfo> {
        val fenster = runCatching { dienst.windows }.getOrDefault(emptyList())
            .sortedByDescending { it.layer }.mapNotNull { it.root }
        val alle = if (fenster.isNotEmpty()) fenster else listOfNotNull(dienst.rootInActiveWindow)
        return alle.filter { it.packageName?.toString() == PAKET }
    }

    private fun finde(dienst: AccessibilityService, id: String): AccessibilityNodeInfo? =
        wurzeln(dienst).firstNotNullOfOrNull { w -> w.findAccessibilityNodeInfosByViewId("$PAKET:id/$id").firstOrNull { it.isVisibleToUser } }

    /** Frisch gelesener Text (refresh umgeht den Zwischenspeicher, der ohne Inhalts-Ereignisse veraltet). */
    private fun text(dienst: AccessibilityService, id: String): String? =
        finde(dienst, id)?.also { it.refresh() }?.text?.toString()?.trim()?.ifBlank { null }

    private fun klickbar(knoten: AccessibilityNodeInfo): Boolean {
        var k: AccessibilityNodeInfo? = knoten
        while (k != null) { if (k.isClickable) return true; k = k.parent }
        return false
    }

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
        if (intent != null && runCatching { dienst.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
}
