package de.frank.aufgaben.erinnerung

import android.app.AlarmManager
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
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.widget.HeuteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
                stelle(app, zeit, pi)
            } else {
                alarmManager(app).cancel(pi)
            }
        }
        planeMitternacht(app)
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

    fun schlummern(context: Context, id: Long, minuten: Int) {
        stelle(context, System.currentTimeMillis() + minuten * 60_000L, ausloeser(context, id, (id * 2 + 1).toInt()))
    }

    const val AKTION_MITTERNACHT = "de.frank.aufgaben.MITTERNACHT"
    const val AKTION_ERLEDIGT = "de.frank.aufgaben.ERLEDIGT"
    const val AKTION_SCHLUMMERN = "de.frank.aufgaben.SCHLUMMERN"
    internal const val ID = EXTRA_ID
}

class ErinnerungsEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Planer.ID, -1)
        if (id < 0) return
        val ergebnis = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val a = AufgabenRepository.get(context).eine(id)
                if (a == null || a.erledigt) { ergebnis.finish(); return@launch }
                val e = Einstellungen.get(context)
                Planer.kanalAnlegen(context)
                val zeit = a.minuten?.let { Tage.zeit(it) }.orEmpty()
                val wann = if (a.vorlauf > 0 && a.minuten != null) "in ${a.vorlauf} Minuten · $zeit Uhr" else "Jetzt · $zeit Uhr"
                val oeffnen = PendingIntent.getActivity(
                    context, id.toInt(),
                    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_AUFGABE, id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                fun aktion(aktion: String, code: Int) = PendingIntent.getBroadcast(
                    context, code, Intent(context, AktionsEmpfaenger::class.java).setAction(aktion).putExtra(Planer.ID, id),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val n = NotificationCompat.Builder(context, Planer.KANAL)
                    .setSmallIcon(R.drawable.ic_stat_aufgabe)
                    .setContentTitle(a.titel)
                    .setContentText(wann)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(if (a.text.isNotBlank()) "$wann\n${a.text}" else wann))
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(oeffnen)
                    .setAutoCancel(true)
                    .addAction(0, "Erledigt", aktion(Planer.AKTION_ERLEDIGT, (id * 4 + 2).toInt()))
                    .addAction(0, "In 10 Min.", aktion(Planer.AKTION_SCHLUMMERN, (id * 4 + 3).toInt()))
                    .build()
                runCatching { NotificationManagerCompat.from(context).notify(id.toInt(), n) }
                if (e.vibration) runCatching {
                    context.getSystemService(Vibrator::class.java)
                        ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 260), -1))
                }
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toene.spiele(context, e.ton, e.lautstaerke) { ergebnis.finish() }
                }
            } catch (_: Exception) {
                ergebnis.finish()
            }
        }
    }
}

class AktionsEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Planer.ID, -1)
        if (id < 0) return
        NotificationManagerCompat.from(context).cancel(id.toInt())
        val ergebnis = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Planer.AKTION_ERLEDIGT -> AufgabenRepository.get(context).let { r -> r.eine(id)?.let { r.setzeErledigt(it, true) } }
                    Planer.AKTION_SCHLUMMERN -> Planer.schlummern(context, id, 10)
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
