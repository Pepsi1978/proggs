package de.frank.jarvis.staubsauger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.frank.jarvis.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
        event ?: return
        val paket = event.packageName?.toString() ?: return
        if (paket == ProscenicSteuerung.PAKET) {
            when (event.eventType) {
                // Toasts der Proscenic-App (etwa „kein aktives WLAN“), damit Jarvis sieht, ob ein Befehl abgewiesen wurde.
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                    if (event.parcelableData == null) event.text.joinToString(" ").trim().takeIf { it.isNotEmpty() }?.let { toast = it; toastUm = System.currentTimeMillis() }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> event.className?.toString()?.let { proscenicKlasse = it }
            }
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (startbar.getOrPut(paket) { packageManager.getLaunchIntentForPackage(paket) != null }) zuletztVorn = paket
    }

    override fun onInterrupt() {}

    private val haupt = Handler(Looper.getMainLooper())
    private var vorhang: View? = null

    /**
     * Deckt den ganzen Bildschirm mit einer Jarvis-Fläche ab, solange Jarvis in der Proscenic-App tippt: Die App muss vorn laufen
     * (im Hintergrund sperrt Android ihr das Netz), Frank soll sie aber nicht sehen. Die Fläche lässt Berührungen durch und nimmt
     * keinen Fokus, die Proscenic-App bleibt also aktiv und bedienbar.
     */
    fun vorhangZeigen(text: String) = haupt.post {
        if (vorhang != null) return@post
        val v = TextView(this).apply {
            setText(text); setTextColor(Color.WHITE); textSize = 18f; gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(14, 17, 22)); setPadding(64, 64, 64, 64)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE,
        ).apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS }
        runCatching { getSystemService(WindowManager::class.java).addView(v, lp); vorhang = v }
    }

    fun vorhangWeg() = haupt.post {
        vorhang?.let { v -> runCatching { getSystemService(WindowManager::class.java).removeView(v) } }
        vorhang = null
    }

    override fun onUnbind(intent: Intent?): Boolean { if (instanz === this) instanz = null; return super.onUnbind(intent) }
    override fun onDestroy() { vorhang?.let { v -> runCatching { getSystemService(WindowManager::class.java).removeView(v) } }; vorhang = null; if (instanz === this) instanz = null; super.onDestroy() }

    companion object {
        @Volatile var instanz: ProscenicBedienung? = null
        @Volatile var zuletztVorn: String? = null
        /** Letzter Toast der Proscenic-App und wann er kam. */
        @Volatile var toast: String? = null
        @Volatile var toastUm: Long = 0L
        /** Klasse des zuletzt geöffneten Proscenic-Fensters (Startbildschirm, Geräteliste, Gerät, Unterseite). */
        @Volatile var proscenicKlasse: String? = null

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
    /** Ist der Roboter mit der Proscenic-Cloud verbunden? null = noch nicht geprüft. Jede Abfrage und jeder Befehl hält das aktuell. */
    var erreichbar by mutableStateOf<Boolean?>(null)
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
 * Tippt in der Proscenic-App. Ablauf je Aktion: App öffnen, geduldig auf den Gerätebildschirm des M7 Pro warten (Startbildschirm
 * und Anmeldung abwarten, in der Geräteliste „M7 Pro“ wiederholt antippen, aus Unterseiten zurückgehen), warten, bis die App den
 * Roboter wieder erreicht, Knopf drücken, die Wirkung prüfen, Stand ablesen, zur vorher offenen App zurück.
 *
 * Warum so geduldig: Android sperrt der Proscenic-App im Hintergrund das Netz; etwa 5 Sekunden nach dem Wechsel trennt sie ihre
 * Verbindung zum Roboter und zeigt „Aktueller WLAN ist nicht verfügbar“. Kommt sie wieder nach vorn, verbindet sie neu, bei
 * langsamem Internet erst nach Sekunden. Ein Tipp davor verpufft.
 * Kennungen und Beschriftungen stammen aus Proscenic 1.5.5 auf Deutsch (ausgelesen am 13. und 14.11.2026). Erkennt Jarvis eine
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
    /** Name des Geräts in der Geräteliste („CleanRobot“ / „M7 Pro“). */
    private const val LISTE_GERAET = "tv_device_name"
    /** Bestätigungsknöpfe einer Rückfrage; bewusst ohne „Start“, das auch als Beschriftung anderer Knöpfe vorkommt. */
    private val BESTAETIGEN = listOf("Bestätigen", "OK", "Ja", "Fortfahren")

    /** Gesamtfrist je Aktion; bleibt unter dem 120-Sekunden-Limit der Werkzeuge, damit Frank eine klare Antwort bekommt. */
    private const val FRIST_MS = 105_000L
    /** So lange darf die App nach dem Öffnen brauchen, bis sie den Roboter wieder erreicht (in zwei Anläufen). */
    private const val VERBINDEN_MS = 50_000L

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

    /** Meldet die App, dass sie den Roboter nicht erreicht („Aktueller WLAN ist nicht verfügbar“, offline …)? */
    private fun offline(text: String?): Boolean {
        val t = text?.lowercase() ?: return false
        return "wlan" in t || "wi-fi" in t || "wifi" in t || "nicht verfügbar" in t || "offline" in t || "netzwerk" in t ||
            "verbindung" in t || "getrennt" in t
    }

    private val sperre = Mutex()

    suspend fun ausfuehren(context: Context, aktion: Aktion): SaugerAusgang = sperre.withLock {
        SaugerStand.laden(context)
        SaugerStand.laeuft = true
        SaugerStand.meldung = null
        val ausgang = try {
            // Unsichtbar über die Proscenic-Cloud, sobald die Anmeldung unter Konten steht.
            if (ProscenicCloud.konfiguriert(context)) ProscenicCloud.ausfuehren(context.applicationContext, aktion)
            else withContext(Dispatchers.Default) { ablauf(context.applicationContext, aktion) }
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

    /** Läuft ein Telefonat oder klingelt es? Dann tippt Jarvis nirgends hin. */
    private fun telefonat(context: Context): Boolean {
        val mode = context.getSystemService(AudioManager::class.java)?.mode ?: return false
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE
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
        if (telefonat(context))
            return SaugerAusgang("Während eines Telefonats bediene ich die Proscenic-App nicht. Sag es mir nach dem Gespräch noch einmal.", fehler = true)
        val start = context.packageManager.getLaunchIntentForPackage(PAKET)
            ?: return SaugerAusgang("Die Proscenic-App ist auf diesem Gerät nicht installiert.", fehler = true)

        val ende = System.currentTimeMillis() + FRIST_MS
        val vorher = when {
            MainActivity.sichtbar -> context.packageName
            else -> dienst.rootInActiveWindow?.packageName?.toString()
                ?.takeIf { it != PAKET && context.packageManager.getLaunchIntentForPackage(it) != null }
                ?: ProscenicBedienung.zuletztVorn
        }
        dienst.vorhangZeigen("")
        try {
            delay(250)
            dienst.startActivity(Intent(start).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            when (val v = bereitMachen(dienst, context, start, ende)) {
                is Bereit.Nein -> return v.ausgang
                Bereit.Ja -> Unit
            }
            val vorZustand = text(dienst, ZUSTAND).orEmpty()
            val knopfText = text(dienst, TEXT_REINIGEN).orEmpty()
            val knopf = knopfZustand(knopfText)
            val antwort: String = when (aktion) {
                Aktion.STATUS -> ""
                Aktion.REINIGEN -> when (knopf) {
                    Knopf.REINIGT -> "Der M7 Pro reinigt schon."
                    Knopf.BEREIT, Knopf.PAUSIERT -> befehl(dienst, context, ende, vorZustand, knopf, wirkung = true, if (knopf == Knopf.BEREIT) "Reinigung gestartet." else "Reinigung fortgesetzt.") {
                        klick(dienst, KNOPF_REINIGEN)
                    }
                    Knopf.UNBEKANNT -> return unbekannt(knopfText)
                }
                Aktion.STOPPEN -> when (knopf) {
                    Knopf.REINIGT -> befehl(dienst, context, ende, vorZustand, knopf, wirkung = true, "Reinigung angehalten.") { klick(dienst, KNOPF_REINIGEN) }
                    Knopf.BEREIT, Knopf.PAUSIERT -> "Der M7 Pro reinigt gerade nicht."
                    Knopf.UNBEKANNT -> return unbekannt(knopfText)
                }
                Aktion.STATION -> {
                    val z = vorZustand.lowercase()
                    when {
                        z.contains("rück") || z.contains("zurück") -> "Der M7 Pro fährt schon zur Ladestation."
                        z.contains("ladevorgang") || z.contains("lädt") || z.contains("geladen") || z.contains("aufladen") -> "Der M7 Pro steht schon in der Ladestation."
                        else -> befehl(dienst, context, ende, vorZustand, knopf, wirkung = true, "Der M7 Pro fährt zurück zur Ladestation.") { klick(dienst, KNOPF_STATION) }
                    }
                }
                Aktion.TIEFENREINIGUNG ->
                    if (knopf == Knopf.REINIGT) "Der M7 Pro reinigt gerade schon; halte ihn erst an, dann starte ich die Tiefenreinigung."
                    else befehl(dienst, context, ende, vorZustand, knopf, wirkung = true, "Tiefenreinigung gestartet.") { mehr(dienst, MEHR_TIEFENREINIGUNG) }
                Aktion.POSITIONIEREN ->
                    befehl(dienst, context, ende, vorZustand, knopf, wirkung = false, "Gerät positionieren an den M7 Pro gesendet; er sollte sich jetzt mit einer Ansage melden.") {
                        mehr(dienst, MEHR_POSITIONIEREN)
                    }
                Aktion.STAUBSAMMLUNG ->
                    if (knopf == Knopf.REINIGT) "Der M7 Pro reinigt gerade; die Staubsammlung geht erst in der Station."
                    else befehl(dienst, context, ende, vorZustand, knopf, wirkung = false, "Staubsammlung an den M7 Pro gesendet.") { mehr(dienst, MEHR_STAUBSAMMLUNG) }
                Aktion.WISCHEN -> befehl(dienst, context, ende, vorZustand, knopf, wirkung = true, "Y-förmiges Wischen gestartet.") { mehr(dienst, MEHR_WISCHEN) }
            }
            menueSchliessen(dienst)
            val zustand = text(dienst, ZUSTAND)
            val akku = text(dienst, AKKU)?.let { Regex("\\d{1,3}").find(it)?.value?.toIntOrNull() }?.takeIf { it in 0..100 }
            val modus = text(dienst, MODUS)
            val ohneNetz = offline(zustand)
            SaugerStand.merken(context, akku, zustand?.takeUnless { ohneNetz }, modus)
            val stand = listOfNotNull(zustand?.let { if (ohneNetz) "Die Proscenic-App meldet: $it." else "Zustand: $it." }, akku?.let { "Akku $it Prozent." }, modus?.let { "Saugstufe: $it." }).joinToString(" ")
            return SaugerAusgang(listOf(antwort, stand).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Kein Stand lesbar." })
        } catch (e: Unbestaetigt) {
            menueSchliessen(dienst)
            return SaugerAusgang(e.message.orEmpty(), fehler = true)
        } finally {
            zurueck(dienst, context, vorher)
            // Kurz warten, damit der Wechsel zurück hinter dem Vorhang passiert.
            withContext(NonCancellable) { delay(700) }
            dienst.vorhangWeg()
        }
    }

    private sealed interface Bereit {
        data object Ja : Bereit
        data class Nein(val ausgang: SaugerAusgang) : Bereit
    }

    /** Ein Befehl, dessen Ankunft am Roboter sich nicht belegen ließ. */
    private class Unbestaetigt(text: String) : Exception(text)

    /**
     * Gerätebildschirm öffnen und warten, bis die App den Roboter erreicht. Meldet sie nach der halben Wartezeit noch „kein WLAN“,
     * geht Jarvis einmal zurück in die Geräteliste und öffnet den M7 Pro neu (das hilft auch beim Antippen von Hand).
     */
    private suspend fun bereitMachen(dienst: AccessibilityService, context: Context, start: Intent, ende: Long): Bereit {
        var neuGeoeffnet = false
        while (true) {
            if (!hauptbildschirm(dienst, context, start, ende))
                return Bereit.Nein(SaugerAusgang(
                    if (telefonat(context)) "Ein Telefonat kam dazwischen; ich habe die Proscenic-App nicht weiter bedient."
                    else "Der Bildschirm des M7 Pro in der Proscenic-App öffnete sich auch nach langem Warten und mehrfachem Antippen nicht. " +
                        "Vermutlich ist das Internet gerade zu langsam für die Anmeldung bei Proscenic.",
                    fehler = true,
                ))
            when (verbunden(dienst, context, minOf(ende, System.currentTimeMillis() + VERBINDEN_MS / 2))) {
                true -> return Bereit.Ja
                false -> {
                    val meldung = text(dienst, ZUSTAND)
                    if (neuGeoeffnet || System.currentTimeMillis() > ende - 30_000 || telefonat(context))
                        return Bereit.Nein(SaugerAusgang(
                            "Die Proscenic-App erreicht den M7 Pro gerade nicht" + (meldung?.let { ", sie meldet „$it“" } ?: "") + ". " +
                                "Ich habe lange gewartet und den Roboter neu geöffnet. Meist ist das Internet zu Hause zu langsam " +
                                "oder der Roboter hat kurz keine Verbindung zum Proscenic-Server. Ich habe nichts angetippt.",
                            fehler = true,
                        ))
                    neuGeoeffnet = true
                    dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    delay(1500)
                }
            }
        }
    }

    /** Wartet (mindestens 2,5 Sekunden, damit ein alter Stand nicht täuscht), bis der Zustand keine Netzwarnung mehr zeigt. */
    private suspend fun verbunden(dienst: AccessibilityService, context: Context, bis: Long): Boolean {
        val frueh = System.currentTimeMillis() + 2500
        var gut = 0
        var ohneKnopfSeit = 0L
        while (System.currentTimeMillis() < bis) {
            if (telefonat(context)) return false
            // Der Gerätebildschirm ist beim Wiederaufbau kurz nicht lesbar; erst nach 6 Sekunden ohne ihn aufgeben.
            val knopfDa = finde(dienst, KNOPF_REINIGEN) != null
            if (knopfDa) ohneKnopfSeit = 0L
            else if (ohneKnopfSeit == 0L) ohneKnopfSeit = System.currentTimeMillis()
            else if (System.currentTimeMillis() - ohneKnopfSeit > 6000) return false
            val z = if (knopfDa) text(dienst, ZUSTAND) else null
            gut = if (!z.isNullOrBlank() && !offline(z)) gut + 1 else 0
            if (gut >= 2 && System.currentTimeMillis() >= frueh) return true
            delay(500)
        }
        return false
    }

    /**
     * Drückt einen Knopf ([tippen]) und prüft danach im Vordergrund, ob der Roboter reagiert. Die App bleibt dabei mindestens
     * 6 Sekunden vorn, damit der Befehl auch bei langsamem Netz rausgeht. Weist die App ihn mit einer Netzwarnung ab, wartet Jarvis
     * auf die Verbindung und tippt einmal neu. Bei [wirkung] muss sich Zustand oder Reinigen-Knopf ändern; sonst einmal nachtippen,
     * danach ehrlich „nicht bestätigt“.
     */
    private suspend fun befehl(
        dienst: AccessibilityService, context: Context, ende: Long, vorZustand: String, vorKnopf: Knopf,
        wirkung: Boolean, erfolg: String, tippen: suspend () -> Unit,
    ): String {
        repeat(2) { versuch ->
            val seit = System.currentTimeMillis()
            tippen()
            bestaetigen(dienst)
            // Das Mehr-Menü bleibt nach dem Tipp offen und verdeckt die Zustandsanzeige; der Befehl ist da schon raus.
            delay(1500)
            menueSchliessen(dienst)
            val bis = minOf(ende - 4000, System.currentTimeMillis() + if (wirkung) 15_000 else 6_000)
            var abgewiesen: String? = null
            while (System.currentTimeMillis() < bis) {
                delay(700)
                val t = ProscenicBedienung.toast?.takeIf { ProscenicBedienung.toastUm >= seit }
                if (t != null && offline(t)) { abgewiesen = t; break }
                if (wirkung) {
                    val z = text(dienst, ZUSTAND).orEmpty()
                    val k = knopfZustand(text(dienst, TEXT_REINIGEN).orEmpty())
                    if (z.isNotBlank() && !offline(z) && (z != vorZustand || k != vorKnopf) && System.currentTimeMillis() - seit >= 3000) return erfolg
                }
            }
            if (abgewiesen == null && !wirkung) {
                val t = ProscenicBedienung.toast?.takeIf { ProscenicBedienung.toastUm >= seit }
                return if (t != null) "$erfolg Die App meldet: $t." else erfolg
            }
            if (versuch == 1 || System.currentTimeMillis() > ende - 25_000) {
                throw Unbestaetigt(
                    if (abgewiesen != null) "Die Proscenic-App hat den Befehl abgewiesen: „$abgewiesen“. Der M7 Pro ist gerade nicht erreichbar, vermutlich wegen des langsamen Internets."
                    else "Ich habe den Befehl zweimal gesendet, aber der M7 Pro hat in der Proscenic-App nicht reagiert (Zustand: ${text(dienst, ZUSTAND) ?: "nicht lesbar"}). " +
                        "Vermutlich kommt er wegen des langsamen Internets nicht an.",
                )
            }
            // Zweiter Versuch: Menü schließen, auf Verbindung warten, neu tippen.
            menueSchliessen(dienst)
            if (!verbunden(dienst, context, minOf(ende - 20_000, System.currentTimeMillis() + 25_000)))
                throw Unbestaetigt("Die Proscenic-App hat die Verbindung zum M7 Pro verloren" + (abgewiesen?.let { " („$it“)" } ?: "") + "; der Befehl kam nicht an.")
        }
        return erfolg
    }

    private fun unbekannt(knopfText: String) = SaugerAusgang(
        "Ich erkenne den Zustand in der Proscenic-App nicht (der Knopf zeigt „${knopfText.ifBlank { "nichts" }}“) und tippe lieber nichts an.",
        fehler = true,
    )

    /** Ein noch offenes Mehr-Menü schließen, damit Zustand und Akku lesbar sind. */
    private suspend fun menueSchliessen(dienst: AccessibilityService) {
        if (finde(dienst, MEHR_TIEFENREINIGUNG) != null) { dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); delay(800) }
    }

    /** Öffnet das Mehr-Menü und tippt den Eintrag [id]. */
    private suspend fun mehr(dienst: AccessibilityService, id: String) {
        if (finde(dienst, id) == null) {
            klick(dienst, KNOPF_MEHR)
            if (!warte(8) { finde(dienst, id) != null }) throw IllegalStateException("das Menü „Mehr“ öffnete sich nicht")
        }
        klick(dienst, id)
    }

    /**
     * Wartet bis zur Frist auf den Gerätebildschirm des M7 Pro. Startbildschirm und Anmeldung wartet es geduldig ab, in der
     * Geräteliste tippt es „M7 Pro“ alle drei Sekunden erneut an (die App reagiert bei langsamem Netz oft erst nach mehreren Tipps),
     * aus einer Unterseite (Karte, Einstellungen) geht es höchstens viermal zurück. Ist die App nicht mehr vorn, holt es sie zurück.
     */
    private suspend fun hauptbildschirm(dienst: AccessibilityService, context: Context, start: Intent, ende: Long): Boolean {
        val bis = ende - 25_000
        var letzterTipp = 0L
        var zurueckGegangen = 0
        var unterseiteSeit = 0L
        var wegSeit = 0L
        var neuGestartet = 0
        while (System.currentTimeMillis() < bis) {
            if (telefonat(context)) return false
            val jetzt = System.currentTimeMillis()
            val wurzeln = wurzeln(dienst)
            if (wurzeln.isEmpty()) {
                // Proscenic ist nicht vorn (etwa nach einem Zurück zu viel): nach 4 Sekunden neu öffnen, höchstens dreimal.
                if (wegSeit == 0L) wegSeit = jetzt
                else if (jetzt - wegSeit > 4000 && neuGestartet < 3) {
                    neuGestartet++; wegSeit = 0L
                    runCatching { dienst.startActivity(Intent(start).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                delay(500); continue
            }
            wegSeit = 0L
            if (finde(dienst, KNOPF_REINIGEN) != null) { delay(700); return true }
            val eintrag = finde(dienst, LISTE_GERAET)
                ?: wurzeln.firstNotNullOfOrNull { w -> w.findAccessibilityNodeInfosByText("M7 Pro").firstOrNull { it.isVisibleToUser && klickbar(it) } }
            val klasse = ProscenicBedienung.proscenicKlasse.orEmpty()
            when {
                eintrag != null -> {
                    unterseiteSeit = 0L
                    if (jetzt - letzterTipp > 3000) { letzterTipp = jetzt; tippe(dienst, eintrag) }
                }
                bestaetigeHinweis(dienst) -> unterseiteSeit = 0L
                // Startbildschirm, Geräteliste beim Laden und Gerätebildschirm beim Aufbau: abwarten, nie zurückgehen.
                klasse.contains("launch", ignoreCase = true) || klasse.endsWith(".activity.MainActivity_") || klasse.contains("RobotMain") ||
                    !klasse.startsWith("com.proscenic") -> unterseiteSeit = 0L
                else -> {
                    if (unterseiteSeit == 0L) unterseiteSeit = jetzt
                    else if (jetzt - unterseiteSeit > 4000 && zurueckGegangen < 4) {
                        zurueckGegangen++; unterseiteSeit = 0L
                        dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }
                }
            }
            delay(500)
        }
        return false
    }

    /** Ein Hinweisfenster der App (etwa zum WLAN) mit „OK“/„Bestätigen“ schließen; true, wenn getippt wurde. */
    private fun bestaetigeHinweis(dienst: AccessibilityService): Boolean {
        for (w in wurzeln(dienst)) for (wort in BESTAETIGEN) {
            val knoten = w.findAccessibilityNodeInfosByText(wort).firstOrNull { n ->
                n.isVisibleToUser && n.text?.toString()?.trim()?.equals(wort, ignoreCase = true) == true
            }
            if (knoten != null) { tippe(dienst, knoten); return true }
        }
        return false
    }

    /** Tippt eine Bestätigung, falls die App nachfragt (bis 2,5 Sekunden); true, wenn eine getippt wurde. */
    private suspend fun bestaetigen(dienst: AccessibilityService): Boolean {
        val ende = System.currentTimeMillis() + 2500
        while (System.currentTimeMillis() < ende) {
            delay(500)
            if (bestaetigeHinweis(dienst)) { delay(500); return true }
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

    /** Zurück zur App, die vorher vorn war; ohne bekannte App auf den Startbildschirm. Während eines Telefonats nichts tun. */
    private fun zurueck(dienst: AccessibilityService, context: Context, vorher: String?) {
        if (telefonat(context) && wurzeln(dienst).isEmpty()) return
        val intent = vorher?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (intent != null && runCatching { dienst.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        dienst.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
}
