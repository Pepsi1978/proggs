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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.frank.aufgaben.MainActivity
import de.frank.aufgaben.R
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Einstellungen

/**
 * Fokus-Timer im Hintergrund: Das Ende ist ein exakter Alarm, der auch feuert, wenn die App im Hintergrund
 * liegt (z. B. beim Lesen in Kindle) oder Android sie beendet hat. Während der Timer läuft, zeigt eine stille
 * Benachrichtigung den Countdown. Am Ende spielt der [ErinnerungsDienst] den Erinnerungston und liest
 * [TEXT] einmal vor. Ende, Aufgabe und Gesamtlänge liegen in eigenen Einstellungen, damit die App den Timer
 * nach einem Neustart des Prozesses wieder aufnehmen kann.
 */
object Fokus {
    /** Kennung der Ende-Benachrichtigung im Erinnerungsdienst (kollidiert nicht mit Aufgaben-IDs im Alltag). */
    const val ID = 1_000_000_011L
    const val TEXT = "Deine Fokuszeit ist vorbei. Gut gemacht! Gönn dir jetzt eine kurze Pause."
    const val AKTION_ENDE = "de.frank.aufgaben.FOKUS_ENDE"
    private const val KANAL_LAUF = "fokus_lauf_v1"
    private const val LAUF_NID = 1_000_000_013
    private const val ALARM_CODE = 1_000_000_015
    private const val PREFS = "fokus"

    data class Stand(val ende: Long, val aufgabe: Long?, val gesamt: Long)

    /** Die Ansage als Aufgabe, damit der Erinnerungsdienst sie wie jede andere abspielt. */
    fun aufgabe(): Aufgabe = Aufgabe(id = ID, titel = "Fokuszeit vorbei", text = TEXT, vorlesen = true)

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun ausloeser(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, ALARM_CODE,
        Intent(context, FokusEmpfaenger::class.java).setAction(AKTION_ENDE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun oeffnen(context: Context, code: Int, extra: Long? = null): PendingIntent = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java).apply { extra?.let { putExtra(MainActivity.EXTRA_AUFGABE, it) } }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Startet oder verschiebt den Hintergrund-Alarm für das Ende um [ende] und zeigt den Countdown. */
    fun planen(context: Context, ende: Long, aufgabe: Long?, gesamt: Long, titel: String?) {
        val app = context.applicationContext
        prefs(app).edit().putLong("ende", ende).putLong("aufgabe", aufgabe ?: -1L).putLong("gesamt", gesamt).apply()
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = ausloeser(app)
        val exakt = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (exakt) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ende, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ende, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ende, pi)
        }
        laufAnzeigen(app, ende, titel)
        Ansage.fokusVorbereiten(app)
    }

    /** Pause, Beenden oder Ende: kein Alarm, kein Countdown, nichts mehr zum Wiederaufnehmen. */
    fun abbrechen(context: Context) {
        val app = context.applicationContext
        prefs(app).edit().clear().apply()
        app.getSystemService(AlarmManager::class.java).cancel(ausloeser(app))
        NotificationManagerCompat.from(app).cancel(LAUF_NID)
    }

    /** Ein laufender Fokus aus einem früheren Prozess, falls sein Ende noch in der Zukunft liegt. */
    fun gespeichert(context: Context): Stand? {
        val p = prefs(context)
        val ende = p.getLong("ende", 0L)
        if (ende <= System.currentTimeMillis()) return null
        return Stand(ende, p.getLong("aufgabe", -1L).takeIf { it >= 0 }, p.getLong("gesamt", 0L))
    }

    private fun laufAnzeigen(app: Context, ende: Long, titel: String?) {
        val nm = app.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(KANAL_LAUF) == null) {
            nm.createNotificationChannel(
                NotificationChannel(KANAL_LAUF, "Fokus-Timer läuft", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Zeigt die restliche Fokuszeit, während du in anderen Apps bist"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            )
        }
        val uhr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(java.util.Date(ende))
        val n = NotificationCompat.Builder(app, KANAL_LAUF)
            .setSmallIcon(R.drawable.ic_stat_aufgabe)
            .setContentTitle(titel?.let { "Fokus: $it" } ?: "Fokus läuft")
            .setContentText("Bis $uhr Uhr")
            .setWhen(ende)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(oeffnen(app, LAUF_NID))
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(LAUF_NID, n) }
    }

    /** Der Alarm ist da: Countdown weg, Ton und Ansage über den Erinnerungsdienst, sonst wenigstens Ton. */
    fun enden(context: Context) {
        val app = context.applicationContext
        prefs(app).edit().clear().apply()
        NotificationManagerCompat.from(app).cancel(LAUF_NID)
        Planer.kanalAnlegen(app)
        try {
            ErinnerungsDienst.starteFokus(app)
        } catch (x: Exception) {
            android.util.Log.w("Fokus", "Dienst nicht gestartet: ${x.message}")
            einfach(app)
        }
    }

    /** Rückfall ohne Dienst: Benachrichtigung und Ton, ohne Vorlesen. */
    fun einfach(context: Context) {
        runCatching { NotificationManagerCompat.from(context).notify(ID.toInt(), benachrichtigung(context, imDienst = false)) }
        val e = Einstellungen.get(context)
        Toene.spiele(context, e.ton, e.lautstaerke)
    }

    /** Die Benachrichtigung am Ende; Antippen öffnet die App und beendet das Vorlesen. */
    fun benachrichtigung(context: Context, imDienst: Boolean): Notification {
        fun aus(code: Int) = PendingIntent.getBroadcast(
            context, code,
            Intent(context, AktionsEmpfaenger::class.java).setAction(Planer.AKTION_AUS).putExtra(Planer.ID, ID),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, Planer.KANAL)
            .setSmallIcon(R.drawable.ic_stat_aufgabe)
            .setContentTitle("Fokuszeit vorbei")
            .setContentText("Gut gemacht! Gönn dir jetzt eine kurze Pause.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(oeffnen(context, ALARM_CODE + 1, ID))
            .setAutoCancel(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply { if (imDienst) addAction(0, "Ausschalten", aus(ALARM_CODE + 2)) }
            .setDeleteIntent(aus(ALARM_CODE + 3))
            .build()
    }
}

class FokusEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Fokus.AKTION_ENDE) Fokus.enden(context)
    }
}
