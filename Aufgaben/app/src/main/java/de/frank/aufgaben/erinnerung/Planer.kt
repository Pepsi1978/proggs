package de.frank.aufgaben.erinnerung

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.frank.aufgaben.MainActivity
import de.frank.aufgaben.R
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.widget.HeuteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plant Erinnerungen für Aufgaben in Heute und Morgen mit Uhrzeit. Einmal-Alarme, die bei jeder
 * Änderung, jedem App-Start, nach dem Neustart und um Mitternacht neu berechnet werden.
 */
object Planer {
    const val KANAL = "erinnerungen_v1"
    private const val EXTRA_ID = "id"
    private const val MITTERNACHT_CODE = 1_000_000_001

    fun kanalAnlegen(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KANAL) != null) return
        // Ohne Kanalton: Den Ton spielt die App selbst, mit der eingestellten Lautstärke.
        val kanal = NotificationChannel(KANAL, "Aufgaben-Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Erinnert an Aufgaben mit Uhrzeit"
            setSound(null, null)
            enableVibration(false)
            enableLights(true)
        }
        nm.createNotificationChannel(kanal)
    }

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun ausloeser(context: Context, id: Long, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, ErinnerungsEmpfaenger::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun stelle(context: Context, zeit: Long, pi: PendingIntent) {
        val am = alarmManager(context)
        val exakt = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exakt) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, pi)
    }

    fun storniere(context: Context, id: Long) {
        alarmManager(context).cancel(ausloeser(context, id, (id * 2).toInt()))
    }

    suspend fun planeAlle(context: Context) {
        val app = context.applicationContext
        val heute = Tage.heute()
        val jetzt = System.currentTimeMillis()
        AufgabenRepository.get(app).alleEinmal().forEach { a ->
            val pi = ausloeser(app, a.id, (a.id * 2).toInt())
            val tag = a.tag
            val min = a.minuten
            val zeit = if (tag != null && min != null) Tage.millis(tag, min) - a.vorlauf * 60_000L else 0L
            if (!a.erledigt && a.erinnerung && tag != null && min != null && tag in heute..heute + 1 && zeit > jetzt) {
                if (a.alsWecker) stelleWecker(app, zeit, pi, a.id) else stelle(app, zeit, pi)
            } else {
                alarmManager(app).cancel(pi)
            }
        }
        planeMitternacht(app)
        // Fehlende Sprachfassungen nachholen (z. B. nach Stimmwechsel oder wenn beim Speichern kein Netz war).
        Ansage.anstossen(app)
    }

    /**
     * Wecker-Erinnerungen als echter Wecker (setAlarmClock): pünktlich auch im Ruhezustand, ohne
     * Drosselung, und der Start des Vorlese-Dienstes ist sicher erlaubt.
     */
    private fun stelleWecker(context: Context, zeit: Long, pi: PendingIntent, id: Long) {
        val zeigen = PendingIntent.getActivity(
            context, (id * 4 + 1).toInt(),
            // Eigene Aktion, damit sich dieser PendingIntent nie mit dem Öffnen-Intent einer anderen Aufgabe deckt.
            Intent(context, MainActivity::class.java).setAction("de.frank.aufgaben.WECKER_ZEIGEN").putExtra(MainActivity.EXTRA_AUFGABE, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            alarmManager(context).setAlarmClock(AlarmManager.AlarmClockInfo(zeit, zeigen), pi)
        } catch (_: SecurityException) {
            stelle(context, zeit, pi)
        }
    }

    /** Um 0 Uhr wird aus Morgen Heute: Widget, Erinnerungen und Anzeige ziehen dann nach. */
    fun planeMitternacht(context: Context) {
        val pi = PendingIntent.getBroadcast(
            context, MITTERNACHT_CODE,
            Intent(context, SystemEmpfaenger::class.java).setAction(AKTION_MITTERNACHT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Tage.naechsteMitternacht() + 2_000, pi)
    }

    fun schlummern(context: Context, id: Long, minuten: Int, alsWecker: Boolean = false) {
        val zeit = System.currentTimeMillis() + minuten * 60_000L
        val pi = ausloeser(context, id, (id * 2 + 1).toInt())
        if (alsWecker) stelleWecker(context, zeit, pi, id) else stelle(context, zeit, pi)
    }

    /** Text unter dem Titel: „Jetzt · 09:00 Uhr“ bzw. „in 10 Minuten · 09:00 Uhr“. */
    fun wann(a: Aufgabe): String {
        val zeit = a.minuten?.let { Tage.zeit(it) }.orEmpty()
        return if (a.vorlauf > 0 && a.minuten != null) "in ${a.vorlauf} Minuten · $zeit Uhr" else "Jetzt · $zeit Uhr"
    }

    /**
     * Die Benachrichtigung einer Erinnerung. [imDienst]: läuft gerade mit Vorlesen/Wecker; im
     * Wecker-Modus bleibt sie stehen, bis man ausschaltet. Jeder Weg, der sie entfernt, stoppt den Dienst.
     */
    fun benachrichtigung(context: Context, a: Aufgabe, wann: String, imDienst: Boolean): Notification {
        val id = a.id
        val wecker = imDienst && a.alsWecker
        val oeffnen = PendingIntent.getActivity(
            context, id.toInt(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_AUFGABE, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun aktion(aktion: String, code: Int) = PendingIntent.getBroadcast(
            context, code, Intent(context, AktionsEmpfaenger::class.java).setAction(aktion).putExtra(ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val zeile = if (wecker) "⏰ $wann · läuft, bis du ausschaltest" else wann
        return NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_stat_aufgabe)
            .setContentTitle(a.titel)
            .setContentText(zeile)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (a.text.isNotBlank()) "$zeile\n${a.text}" else zeile))
            .setCategory(if (wecker) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(oeffnen)
            .setAutoCancel(!wecker)
            .setOngoing(wecker)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply { if (imDienst) addAction(0, "Ausschalten", aktion(AKTION_AUS, (id * 4).toInt())) }
            .addAction(0, "Erledigt", aktion(AKTION_ERLEDIGT, (id * 4 + 2).toInt()))
            .addAction(0, "In 10 Min.", aktion(AKTION_SCHLUMMERN, (id * 4 + 3).toInt()))
            // Wegwischen beendet ein laufendes Vorlesen ebenfalls.
            .setDeleteIntent(aktion(AKTION_AUS, (id * 4 + 1).toInt()))
            .build()
    }

    /** Kurzer Platzhalter, bis die Aufgabe aus der Datenbank geladen ist (Android verlangt sofort eine). */
    fun platzhalter(context: Context): Notification =
        NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_stat_aufgabe)
            .setContentTitle("Erinnerung")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    /** Bisheriger Weg ohne Dienst: Benachrichtigung plus kurzer Ton (Rückfall). */
    fun einfacheErinnerung(context: Context, id: Long, fertig: () -> Unit = {}) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val a = AufgabenRepository.get(context).eine(id)
                if (a == null || a.erledigt) { fertig(); return@launch }
                val e = Einstellungen.get(context)
                kanalAnlegen(context)
                runCatching { NotificationManagerCompat.from(context).notify(id.toInt(), benachrichtigung(context, a, wann(a), imDienst = false)) }
                if (e.vibration) runCatching {
                    context.getSystemService(Vibrator::class.java)
                        ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 260), -1))
                }
                withContext(Dispatchers.Main) { Toene.spiele(context, e.ton, e.lautstaerke) { fertig() } }
            } catch (_: Exception) {
                fertig()
            }
        }
    }

    const val AKTION_MITTERNACHT = "de.frank.aufgaben.MITTERNACHT"
    const val AKTION_ERLEDIGT = "de.frank.aufgaben.ERLEDIGT"
    const val AKTION_SCHLUMMERN = "de.frank.aufgaben.SCHLUMMERN"
    const val AKTION_AUS = "de.frank.aufgaben.AUSSCHALTEN"
    internal const val ID = EXTRA_ID
}

class ErinnerungsEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Planer.ID, -1)
        if (id < 0) return
        val ergebnis = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val a = runCatching { AufgabenRepository.get(context).eine(id) }.getOrNull()
            if (a == null || a.erledigt) { ergebnis.finish(); return@launch }
            Planer.kanalAnlegen(context)
            val e = Einstellungen.get(context)
            // Vorlesen, Wecker und eigene MP3s (länger als ein kurzer Ton) laufen im Vordergrund-Dienst.
            if (a.vorlesen || a.alsWecker || e.ton.startsWith("datei:")) {
                val gestartet = try {
                    ErinnerungsDienst.starte(context, id, Planer.wann(a))
                    true
                } catch (x: Exception) {
                    // z. B. ForegroundServiceStartNotAllowedException: dann wie bisher, nur ohne Vorlesen.
                    android.util.Log.w("Erinnerung", "Dienst nicht gestartet: ${x.message}")
                    false
                }
                if (gestartet) { ergebnis.finish(); return@launch }
            }
            Planer.einfacheErinnerung(context, id) { ergebnis.finish() }
        }
    }
}

class AktionsEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Planer.ID, -1)
        if (id < 0) return
        // Jede Aktion (auch Wegwischen) beendet ein laufendes Vorlesen bzw. den Wecker.
        ErinnerungsDienst.stoppe(context, id)
        NotificationManagerCompat.from(context).cancel(id.toInt())
        if (intent.action == Planer.AKTION_AUS) return
        val ergebnis = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Planer.AKTION_ERLEDIGT -> AufgabenRepository.get(context).let { r -> r.eine(id)?.let { r.setzeErledigt(it, true) } }
                    Planer.AKTION_SCHLUMMERN -> {
                        val wecker = AufgabenRepository.get(context).eine(id)?.alsWecker == true
                        Planer.schlummern(context, id, 10, wecker)
                    }
                }
            } finally {
                ergebnis.finish()
            }
        }
    }
}

/** Neustart, App-Update, Zeit- und Datumswechsel, Mitternacht: alles neu planen. */
class SystemEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ergebnis = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Planer.planeAlle(context)
                HeuteWidget.aktualisiere(context)
            } finally {
                ergebnis.finish()
            }
        }
    }
}
