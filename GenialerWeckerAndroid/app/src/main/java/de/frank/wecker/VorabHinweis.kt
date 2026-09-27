package de.frank.wecker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.genialerwecker.app.R

/**
 * Eine Stunde vor dem Wecken ein stiller Hinweis mit „Diesmal auslassen“ – so lässt sich ein Wecker überspringen,
 * ohne die App zu öffnen. Völlig getrennt vom Weckpfad: ungenaue Planung, jeder Fehler wird geschluckt.
 */
object VorabHinweis {
    const val ACTION_ZEIGEN = "de.frank.wecker.VORAB_ZEIGEN"
    const val ACTION_AUSLASSEN = "de.frank.wecker.VORAB_AUSLASSEN"
    private const val CHANNEL = "alarm_vorab_v1"
    private const val NOTIFICATION = 6000
    private const val VORLAUF_MS = 60 * 60_000L
    private const val PREF = "vorab_hinweis"

    fun aktiv(context: Context): Boolean = runCatching {
        context.getSharedPreferences("wecker_einstellungen", Context.MODE_PRIVATE).getBoolean(PREF, true)
    }.getOrDefault(true)

    fun setAktiv(context: Context, an: Boolean) {
        runCatching { context.getSharedPreferences("wecker_einstellungen", Context.MODE_PRIVATE).edit().putBoolean(PREF, an).apply() }
        AlarmStore.get(context).all().forEach { planen(context, it.id, it) }
    }

    private fun intent(context: Context, action: String, id: String, at: Long) = Intent(context, AlarmReceiver::class.java)
        .setAction(action).setData(Uri.parse("wecker://vorab/$id/$action")).putExtra("id", id).putExtra("at", at)

    /** Plant (oder entfernt) den Hinweis passend zum aktuellen Stand des Weckers. */
    fun planen(context: Context, id: String, alarm: Alarm?) = runCatching {
        val manager = context.getSystemService(AlarmManager::class.java)
        val op = PendingIntent.getBroadcast(context, 0, intent(context, ACTION_ZEIGEN, id, 0),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.cancel(op)
        if (alarm == null || !aktiv(context) || !alarm.enabled || alarm.nextAt <= 0) return@runCatching
        val zeit = alarm.nextAt - VORLAUF_MS
        if (zeit <= System.currentTimeMillis()) return@runCatching
        val mitZeit = PendingIntent.getBroadcast(context, 0, intent(context, ACTION_ZEIGEN, id, alarm.nextAt),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, mitZeit)
    }.onFailure { Log.w("WeckerVorab", "Vorab-Hinweis nicht geplant", it) }

    fun entfernen(context: Context, id: String) {
        runCatching { context.getSystemService(NotificationManager::class.java).cancel(id, NOTIFICATION) }
    }

    /** Vom Empfänger: zeigen (nur wenn der Termin noch derselbe ist) oder auslassen. */
    fun empfangen(context: Context, intent: Intent) = runCatching {
        val id = intent.getStringExtra("id") ?: return@runCatching
        val alarm = AlarmStore.get(context).get(id)
        when (intent.action) {
            ACTION_ZEIGEN -> if (alarm != null && alarm.enabled && alarm.nextAt == intent.getLongExtra("at", -1)) zeigen(context, alarm)
            ACTION_AUSLASSEN -> {
                entfernen(context, id)
                if (alarm == null) return@runCatching
                val scheduler = AlarmScheduler(context)
                if (alarm.repeats) scheduler.skipNext(id) else scheduler.setEnabled(id, false)
            }
        }
    }.onFailure { Log.w("WeckerVorab", "Vorab-Hinweis fehlgeschlagen", it) }

    private fun zeigen(context: Context, alarm: Alarm) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Wecker in einer Stunde", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Stiller Hinweis eine Stunde vor dem Wecken, mit „Diesmal auslassen“."
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
        val auslassen = PendingIntent.getBroadcast(context, 1, intent(context, ACTION_AUSLASSEN, alarm.id, alarm.nextAt),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(alarm.id, NOTIFICATION, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle("Wecker um ${formatClock(alarm.nextAt)}")
            .setContentText(alarm.name.ifBlank { "Dein Wecker" } + " · in einer Stunde")
            .setColor(0xFFB8860B.toInt()).setSilent(true).setAutoCancel(true).setContentIntent(open)
            .setTimeoutAfter(VORLAUF_MS)
            .addAction(0, if (alarm.repeats) "Diesmal auslassen" else "Ausschalten", auslassen).build())
    }
}
