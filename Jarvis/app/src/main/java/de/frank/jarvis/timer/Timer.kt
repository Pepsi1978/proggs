package de.frank.jarvis.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Ein laufender Timer. [ende] ist die Wanduhrzeit in Millisekunden, zu der er klingelt. */
data class LaufenderTimer(val id: Int, val name: String, val dauerMs: Long, val ende: Long)

/**
 * Timer von Jarvis: „Starte einen Timer für 33 Minuten“. Jeder Timer ist ein exakter Wecker im AlarmManager
 * (klingelt auf die Sekunde, auch im Ruhezustand und bei gesperrtem Handy) und überlebt einen Neustart der App,
 * weil die Liste gespeichert ist. Die Startseite zeigt alle laufenden Timer mit Restzeit.
 */
object Timer {
    private const val TAG = "JarvisTimer"
    private const val SPEICHER = "timer"
    const val AKTION_ABGELAUFEN = "de.frank.jarvis.TIMER_ABGELAUFEN"
    const val AKTION_STOPPEN = "de.frank.jarvis.TIMER_STOPPEN"
    const val AKTION_SCHLUMMERN = "de.frank.jarvis.TIMER_SCHLUMMERN"
    const val AKTION_ZEIGEN = "de.frank.jarvis.TIMER_ZEIGEN"
    const val EXTRA_ID = "id"
    private const val KANAL = "jarvis-timer"
    /** Erst so lange nach dem Ende gilt ein Timer beim App-Start als verpasst; Jüngeres klingelt noch über seinen Wecker. */
    private const val SPAET_MS = 60_000L
    /** Ohne eigenen Ton: Der Klingel-Dienst spielt den Weckton selbst; auch für stille Meldungen. */
    internal const val KANAL_STILL = "jarvis-timer-wecker"

    private val _laufend = MutableStateFlow<List<LaufenderTimer>>(emptyList())
    val laufend: StateFlow<List<LaufenderTimer>> = _laufend.asStateFlow()
    @Volatile private var geladen = false

    /** Timer, die gerade wie ein Wecker klingeln (bis Frank sie ausschaltet). */
    private val _klingelt = MutableStateFlow<List<LaufenderTimer>>(emptyList())
    val klingelt: StateFlow<List<LaufenderTimer>> = _klingelt.asStateFlow()

    /** Beim App-Start: Liste laden, Abgelaufenes melden, alle Wecker neu stellen (etwa nach einem Neustart des Handys). */
    @Synchronized fun starte(context: Context) {
        val app = context.applicationContext
        kanalAnlegen(app)
        lade(app)
        val jetzt = System.currentTimeMillis()
        // Nur lange Verpasstes ohne Klingel-Dienst melden: Es ist ohnehin zu spät, und nach einem Neustart darf kein Klingel-Dienst starten.
        // Eben erst Abgelaufenes nicht: Meist hat gerade sein Wecker diesen Prozess gestartet, und der Broadcast kommt gleich
        // hinterher. Sein Wecker wird unten neu gestellt (ein vergangener Zeitpunkt löst sofort aus), so klingelt er richtig;
        // kommt der Broadcast doppelt, findet der zweite den Timer nicht mehr und bleibt still.
        _laufend.value.filter { jetzt - it.ende > SPAET_MS }.forEach { abgelaufen(app, it.id, spaet = true) }
        _laufend.value.forEach { stelleWecker(app, it) }
    }

    @Synchronized fun starten(context: Context, dauerMs: Long, name: String): LaufenderTimer {
        val app = context.applicationContext
        lade(app)
        val id = (_laufend.value.maxOfOrNull { it.id } ?: 0) + 1
        val t = LaufenderTimer(id, name.ifBlank { "Timer" }, dauerMs, System.currentTimeMillis() + dauerMs)
        _laufend.value = (_laufend.value + t).sortedBy { it.ende }
        speichere(app)
        stelleWecker(app, t)
        return t
    }

    /** Hält einen Timer an und entfernt ihn. true = es gab ihn. */
    @Synchronized fun stoppen(context: Context, id: Int): Boolean {
        val app = context.applicationContext
        lade(app)
        val gab = _laufend.value.any { it.id == id }
        _laufend.value = _laufend.value.filterNot { it.id == id }
        speichere(app)
        app.getSystemService(AlarmManager::class.java).cancel(absicht(app, id))
        app.getSystemService(NotificationManager::class.java).cancel(meldungsId(id))
        return gab
    }

    fun alle(context: Context): List<LaufenderTimer> { lade(context.applicationContext); return _laufend.value }

    /**
     * Der Wecker hat ausgelöst. Im Wecker-Modus (Einstellung) klingelt der Klingel-Dienst in einer Schleife, bis Frank
     * in der Meldung oder in der App „Aus“ tippt; sonst eine Meldung, die klingelt, bis sie weggewischt wird.
     */
    @Synchronized fun abgelaufen(context: Context, id: Int, spaet: Boolean = false) {
        val app = context.applicationContext
        lade(app)
        // Unbekannt: schon gemeldet (doppelter Broadcast nach dem Neustellen in [starte]) oder inzwischen gestoppt.
        // Keine Meldung, sonst überschriebe sie die Meldung oder das Klingeln des echten Ablaufs.
        val t = _laufend.value.firstOrNull { it.id == id } ?: return
        _laufend.value = _laufend.value.filterNot { it.id == id }
        speichere(app)
        kanalAnlegen(app)
        // Auf Wunsch zusätzlich vorgelesen (Einstellungen → Benachrichtigungen → Timer abgelaufen).
        if (!spaet && de.frank.jarvis.meldung.Meldungen.vorlesenAn(app, de.frank.jarvis.meldung.Meldungsarten.TIMER))
            de.frank.jarvis.meldung.Meldungen.vorlesenSpaeter(app, de.frank.jarvis.meldung.Meldungsarten.TIMER, t.name, "${t.name} ist abgelaufen.", schluessel = "${t.id}")
        if (!spaet && de.frank.jarvis.data.Einstellungen.get(app).timerWecker) {
            _klingelt.value = (_klingelt.value.filterNot { it.id == id } + t)
            val gestartet = runCatching { androidx.core.content.ContextCompat.startForegroundService(app, Intent(app, TimerKlingelDienst::class.java)) }
                .onFailure { Log.w(TAG, "Klingel-Dienst ließ sich nicht starten", it) }.isSuccess
            if (gestartet) return
            _klingelt.value = _klingelt.value.filterNot { it.id == id }
        }
        meldung(app, id, t, laut = true)
    }

    /** Einfache Meldung. [laut] = klingelt, bis sie weggewischt wird; sonst still (etwa nach der Höchstdauer). */
    internal fun meldung(app: Context, id: Int, t: LaufenderTimer?, laut: Boolean) {
        val name = t?.name ?: "Timer"
        val unveraenderlich = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val aus = PendingIntent.getBroadcast(app, 9_000 + id, Intent(app, TimerEmpfaenger::class.java).setAction(AKTION_STOPPEN).putExtra(EXTRA_ID, id), unveraenderlich)
        val meldung = NotificationCompat.Builder(app, if (laut) KANAL else KANAL_STILL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle("$name ist abgelaufen").setContentText(t?.let { "Nach " + dauerText(it.dauerMs) } ?: "Zeit ist um")
            .setPriority(if (laut) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true).setOngoing(false)
            .setContentIntent(PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), unveraenderlich))
            .addAction(R.drawable.ic_stat_jarvis, "Aus", aus)
            .setDeleteIntent(aus)
            .build()
            .apply { if (laut) flags = flags or android.app.Notification.FLAG_INSISTENT }
        runCatching { app.getSystemService(NotificationManager::class.java).notify(meldungsId(id), meldung) }.onFailure { Log.w(TAG, "Meldung nicht möglich", it) }
    }

    /** Schaltet das Klingeln aus: [id] = nur diesen Timer, null = alle klingelnden. */
    fun klingelnAus(context: Context, id: Int? = null) {
        // Ausgeschaltet heißt erledigt: ein noch wartendes „… ist abgelaufen“ (etwa nach einem Gespräch) nicht mehr sprechen.
        de.frank.jarvis.meldung.Meldungen.vorlesenAbbrechen(de.frank.jarvis.meldung.Meldungsarten.TIMER, id?.toString())
        _klingelt.value = if (id == null) emptyList() else _klingelt.value.filterNot { it.id == id }
        TimerKlingelDienst.abgleichen()
    }

    /** Schlummern: Klingeln aus und denselben Timer mit der eingestellten Schlummerzeit neu starten. */
    fun schlummern(context: Context, id: Int? = null) {
        val app = context.applicationContext
        val t = _klingelt.value.firstOrNull { id == null || it.id == id } ?: return
        klingelnAus(app, t.id)
        starten(app, de.frank.jarvis.data.Einstellungen.get(app).timerSchlummern * 60_000L, t.name)
    }

    fun meldungAus(context: Context, id: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(meldungsId(id))
    }

    /** „1 Stunde 20 Minuten“, „33 Minuten“, „45 Sekunden“. */
    fun dauerText(ms: Long): String {
        val s = (ms + 500) / 1000
        val h = s / 3600; val m = (s % 3600) / 60; val sek = s % 60
        return listOfNotNull(
            h.takeIf { it > 0 }?.let { if (it == 1L) "1 Stunde" else "$it Stunden" },
            m.takeIf { it > 0 }?.let { if (it == 1L) "1 Minute" else "$it Minuten" },
            sek.takeIf { it > 0 && h == 0L }?.let { if (it == 1L) "1 Sekunde" else "$it Sekunden" },
        ).joinToString(" ").ifEmpty { "0 Sekunden" }
    }

    /** Restzeit als Uhr: 1:05:09 oder 32:59. */
    fun restUhr(ms: Long): String {
        val s = ((ms.coerceAtLeast(0) + 999) / 1000)
        val h = s / 3600; val m = (s % 3600) / 60; val sek = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sek) else "%d:%02d".format(m, sek)
    }

    private fun meldungsId(id: Int) = 7_000 + id

    private fun absicht(app: Context, id: Int) = PendingIntent.getBroadcast(
        app, 7_000 + id, Intent(app, TimerEmpfaenger::class.java).setAction(AKTION_ABGELAUFEN).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun stelleWecker(app: Context, t: LaufenderTimer) {
        val wecker = app.getSystemService(AlarmManager::class.java)
        runCatching {
            // setAlarmClock ist der genaueste Weg: kein Aufschub durch Doze, und das Handy zeigt den Wecker an.
            if (Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()) {
                val zeigen = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                wecker.setAlarmClock(AlarmManager.AlarmClockInfo(t.ende, zeigen), absicht(app, t.id))
            } else wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.ende, absicht(app, t.id))
        }.onFailure { Log.w(TAG, "Timer-Wecker ließ sich nicht stellen", it) }
    }

    internal fun kanalAnlegen(app: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(KANAL_STILL, "Timer-Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Zeigt den klingelnden Timer mit „Aus“ und „Schlummern“. Den Ton spielt Jarvis selbst."
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
            },
        )
        val ton = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(KANAL, "Timer", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Klingelt, wenn ein Timer abgelaufen ist."
                setSound(ton, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600)
                setBypassDnd(true)
            },
        )
    }

    @Synchronized private fun lade(app: Context) {
        if (geladen) return
        val roh = app.getSharedPreferences(SPEICHER, Context.MODE_PRIVATE).getString("liste", null)
        _laufend.value = runCatching {
            val a = JSONArray(roh ?: "[]")
            (0 until a.length()).map { a.getJSONObject(it).let { o -> LaufenderTimer(o.getInt("id"), o.optString("name", "Timer"), o.getLong("dauer"), o.getLong("ende")) } }
        }.getOrDefault(emptyList()).sortedBy { it.ende }
        geladen = true
    }

    @Synchronized private fun speichere(app: Context) {
        val a = JSONArray()
        _laufend.value.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("dauer", it.dauerMs).put("ende", it.ende)) }
        app.getSharedPreferences(SPEICHER, Context.MODE_PRIVATE).edit().putString("liste", a.toString()).apply()
    }
}

class TimerEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Timer.EXTRA_ID, -1)
        when (intent.action) {
            Timer.AKTION_ABGELAUFEN -> Timer.abgelaufen(context, id)
            Timer.AKTION_STOPPEN -> if (id < 0) Timer.klingelnAus(context) else { Timer.meldungAus(context, id); Timer.klingelnAus(context, id) }
            Timer.AKTION_SCHLUMMERN -> Timer.schlummern(context, id.takeIf { it >= 0 })
            Timer.AKTION_ZEIGEN -> TimerKlingelDienst.abgleichen()
        }
    }
}
