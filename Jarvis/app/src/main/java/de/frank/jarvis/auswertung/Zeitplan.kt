package de.frank.jarvis.auswertung

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.observability.Logbuch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Die Tagesauswertungs-Synchronisation: Jarvis schreibt die Tagesauswertung zu jeder vollen Stunde neu (oder im
 * eingestellten Abstand). Solange Frank laut Dienstplan schläft, ruht sie; die erste volle Stunde nach dem Schlaf
 * läuft immer. Es ist immer genau ein Wecker gestellt: der nächste. Nach jedem Lauf, beim Start der App, nach dem
 * Einschalten des Handys und nach einer Zeit- oder Zeitzonenänderung wird neu gestellt. Der Wecker ist exakt und
 * geht auch im Stromsparmodus des Geräts.
 *
 * Fällt das Modell bei einem Lauf aus, stellt ein zweiter Wecker höchstens zwei Wiederholungen nach je 15 Minuten.
 */
object Zeitplan {
    private const val TAG = "JarvisZeitplan"
    const val AKTION = "de.frank.jarvis.TAGESAUSWERTUNG"
    const val AKTION_WIEDERHOLUNG = "de.frank.jarvis.TAGESAUSWERTUNG_WIEDERHOLUNG"
    /** Anlass der Wiederholung nach einem Modellausfall; daran erkennt [nachModellausfall] den Zähler. */
    const val ANLASS_WIEDERHOLUNG = "neuer Versuch, das Modell war nicht erreichbar"
    private const val WIEDERHOLUNGEN = 2
    private const val WIEDERHOLEN_NACH_MS = 15 * 60_000L

    /** Wählbare Abstände in Stunden. */
    val ABSTAENDE = listOf(1, 2, 3, 4, 6)

    /** Der zuletzt gestellte nächste Lauf: Die Oberfläche liest ihn, statt den Kalender neu abzufragen. */
    private val _naechsterLauf = MutableStateFlow<LocalDateTime?>(null)
    val naechsterLauf: StateFlow<LocalDateTime?> = _naechsterLauf.asStateFlow()

    /** Zwischenspeicher je Minute und Einstellung: Prüfer (Kalender-Abfrage) und nächster Lauf. */
    private data class Schluessel(val minute: Long, val an: Boolean, val abstand: Int, val schlafpause: Boolean, val proTag: Int, val frueheste: Int)
    @Volatile private var prueferSpeicher: Pair<Schluessel, (LocalDateTime) -> Boolean>? = null
    @Volatile private var naechsterSpeicher: Pair<Pair<Schluessel, LocalDateTime>, LocalDateTime?>? = null

    private fun schluessel(context: Context): Schluessel {
        val e = Einstellungen.get(context)
        return Schluessel(System.currentTimeMillis() / 60_000L, e.auswertungAn, e.auswertungAbstand, e.auswertungSchlafpause, e.auswertungProTag, e.auswertungFrueheste)
    }

    /** Vergisst die zwischengespeicherten Rechnungen, etwa nach einer Änderung der Einstellungen oder der Uhrzeit. */
    fun vergiss() {
        prueferSpeicher = null
        naechsterSpeicher = null
    }

    /** Prüft für eine volle Stunde, ob dann ein Lauf ansteht. Die Kalender-Abfrage wird höchstens einmal je Minute gemacht. */
    private fun pruefer(context: Context): (LocalDateTime) -> Boolean {
        val k = schluessel(context)
        prueferSpeicher?.let { (alt, p) -> if (alt == k) return p }
        val schlaeft: (LocalDateTime) -> Boolean = (if (k.schlafpause) Register.alle(context).filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.schlafzeiten() else null) ?: { false }
        val abstand = k.abstand
        // Nach dem Schlaf zählt die erste Stunde auch dann, wenn sie nicht in den Abstand fällt (15 Uhr nach dem Nachtdienst).
        val kandidat: (LocalDateTime) -> Boolean = { t -> !schlaeft(t) && (t.hour % abstand == 0 || schlaeft(t.minusHours(1))) }
        // Höchstens [proTag] Läufe je Kalendertag (Franks Vorgabe vom 11.11.2026: zwei), der Reihe nach und je mindestens
        // [abstand] Stunden nach dem vorigen angenommenen Lauf desselben Tages: sonst ergäben „erste Stunde nach dem Schlaf“
        // und das Raster zwei Läufe direkt hintereinander (11 und 12 Uhr). Hier gerechnet, damit der nächste Lauf und der
        // letzte fällige (veraltet, Nachholen) dasselbe sagen.
        val tage = java.util.concurrent.ConcurrentHashMap<java.time.LocalDate, Set<Int>>()
        fun angenommen(tag: java.time.LocalDate): Set<Int> = tage.getOrPut(tag) {
            if (k.proTag == 0) return@getOrPut (0..23).filter { kandidat(tag.atTime(it, 0)) }.toSet()
            // Gezählt wird ab dem Aufwachen: Der erste Lauf ist die erste wache Stunde nach dem Schlaf laut Dienstplan, sonst die
            // erste wache Stunde ab [Schluessel.frueheste] (freier Tag ohne Schlafzeit). Jeder weitere folgt [abstand] Stunden später
            // (schläft Frank dann, die nächste wache Stunde). Tagdienst (Schlaf 20–4): 4 und 10 Uhr; Nachtdienst (Schlaf 6–15):
            // 15 und 21 Uhr; freier Tag: 10 und 16 Uhr.
            val wach: (Int) -> Boolean = { h -> !schlaeft(tag.atTime(h, 0)) }
            val stunden = mutableListOf<Int>()
            var h = (0..23).firstOrNull { wach(it) && (schlaeft(tag.atTime(it, 0).minusHours(1)) || it >= k.frueheste) } ?: return@getOrPut emptySet()
            while (h <= 23 && stunden.size < k.proTag) {
                stunden += h
                h += abstand
                while (h <= 23 && !wach(h)) h++
            }
            stunden.toSet()
        }
        val p: (LocalDateTime) -> Boolean = { t -> t.minute == 0 && t.hour in angenommen(t.toLocalDate()) }
        prueferSpeicher = k to p
        return p
    }

    /** Nächster Lauf ab jetzt, oder null, wenn die Synchronisation aus ist. Je Minute zwischengespeichert. */
    fun naechster(context: Context, ab: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val k = schluessel(context)
        if (!k.an) return null
        val start = ab.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        naechsterSpeicher?.let { (schl, wert) -> if (schl.first == k && schl.second == start) return wert }
        val faellig = pruefer(context)
        val wert = (0L until 48L).map { start.plusHours(it) }.firstOrNull(faellig) ?: start
        naechsterSpeicher = (k to start) to wert
        return wert
    }

    /** Der letzte geplante Lauf, der schon hätte stattfinden sollen (höchstens einen Tag zurück). */
    fun letzterFaelliger(context: Context, jetzt: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!Einstellungen.get(context).auswertungAn) return null
        val faellig = pruefer(context)
        val start = jetzt.truncatedTo(ChronoUnit.HOURS)
        return (0L until 24L).map { start.minusHours(it) }.firstOrNull(faellig)
    }

    fun stelle(context: Context) {
        val app = context.applicationContext
        // Aufrufer stellen nach Änderungen an Einstellungen, Uhrzeit oder Dienstplan neu: dann frisch rechnen.
        vergiss()
        val wecker = app.getSystemService(AlarmManager::class.java)
        val absicht = PendingIntent.getBroadcast(app, 7, Intent(app, AuswertungsEmpfaenger::class.java).setAction(AKTION), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        wecker.cancel(absicht)
        val wann = naechster(app)
        _naechsterLauf.value = wann
        // Für die Prüfung: die geplanten Läufe der nächsten 16 Tage (ein Schichtzyklus), nur im Logcat.
        runCatching {
            val p = pruefer(app)
            val heute = java.time.LocalDate.now()
            Log.i(TAG, "Plan 16 Tage: " + (0L until 16L).joinToString(" | ") { d -> val tag = heute.plusDays(d); "$tag " + (0..23).filter { p(tag.atTime(it, 0)) }.joinToString(",") })
        }
        if (wann == null) return
        val millis = wann.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) {
                wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, absicht)
            } else {
                // Ohne Erlaubnis für exakte Wecker: ungefähr zur Zeit, das System darf um Minuten verschieben.
                wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, absicht)
            }
            Log.i(TAG, "Nächste Tagesauswertung: $wann")
        }.onFailure { Log.w(TAG, "Wecker ließ sich nicht stellen", it); Logbuch.fehler("Zeitplan: Wecker ließ sich nicht stellen", it) }
    }

    private fun wiederholungsAbsicht(app: Context): PendingIntent =
        PendingIntent.getBroadcast(app, 8, Intent(app, AuswertungsEmpfaenger::class.java).setAction(AKTION_WIEDERHOLUNG), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /**
     * Das Modell war bei einem Lauf nicht erreichbar: in 15 Minuten erneut versuchen, höchstens [WIEDERHOLUNGEN]-mal je
     * ausgefallenem Lauf. Ein Lauf mit anderem Anlass beginnt die Zählung von vorn.
     */
    fun nachModellausfall(context: Context, anlass: String) {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        val bisher = if (anlass == ANLASS_WIEDERHOLUNG) e.auswertungWiederholungen else 0
        if (bisher >= WIEDERHOLUNGEN) {
            if (e.auswertungWiederholungen != bisher) e.auswertungWiederholungen = bisher
            Logbuch.system("Zeitplan: keine weitere Wiederholung", "Modell nach $bisher Versuchen weiter nicht erreichbar")
            return
        }
        e.auswertungWiederholungen = bisher + 1
        val wecker = app.getSystemService(AlarmManager::class.java)
        val millis = System.currentTimeMillis() + WIEDERHOLEN_NACH_MS
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, wiederholungsAbsicht(app))
            else wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, wiederholungsAbsicht(app))
            Logbuch.system("Zeitplan: Wiederholung gestellt", "Versuch ${bisher + 1} von $WIEDERHOLUNGEN in 15 Minuten")
        }.onFailure { Logbuch.fehler("Zeitplan: Wiederholung ließ sich nicht stellen", it) }
    }

    /** Eine Deutung ist gelungen: offene Wiederholung absagen und den Zähler zurücksetzen. */
    fun deutungGelungen(context: Context) {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        if (e.auswertungWiederholungen == 0) return
        e.auswertungWiederholungen = 0
        runCatching { app.getSystemService(AlarmManager::class.java).cancel(wiederholungsAbsicht(app)) }
    }
}

/** Der Wecker ist da: den Dienst mit der Auswertung beauftragen. Der Dienst stellt danach den nächsten Wecker. */
class AuswertungsEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Zeitplan.AKTION -> JarvisDienst.auswerten(context, "automatische Synchronisation")
            Zeitplan.AKTION_WIEDERHOLUNG -> JarvisDienst.auswerten(context, Zeitplan.ANLASS_WIEDERHOLUNG)
        }
    }
}
