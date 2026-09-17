package de.frank.wecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import de.frank.genialeideen.R

/**
 * Stille Anzeige während einer Schlummerpause mit „Jetzt beenden“. Wer schon wach ist, beendet die
 * Pause, ohne den Wecker oder seine Wiederholung auszuschalten. Fehler hier berühren nie das Wecken.
 */
object SnoozeNotice {
    const val ACTION_END = "de.frank.wecker.END_SNOOZE"
    private const val CHANNEL = "alarm_snooze_v1"
    private const val NOTIFICATION = 5000

    fun show(context: Context, alarm: Alarm) = runCatching {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Schlummerpause", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Zeigt eine laufende Schlummerpause und beendet sie auf Wunsch."
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
        val end = PendingIntent.getBroadcast(context, NOTIFICATION,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_END).setData(android.net.Uri.parse("wecker://snooze-end/${alarm.id}")).putExtra("id", alarm.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(alarm.id, NOTIFICATION, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle("${alarm.name} schlummert")
            .setContentText("Klingelt wieder um ${formatClock(alarm.snoozeUntil)}")
            .setWhen(alarm.snoozeUntil).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
            .setOngoing(true).setSilent(true).setContentIntent(open)
            .addAction(0, "Jetzt beenden", end).build())
    }

    fun cancel(context: Context, id: String) {
        runCatching { context.getSystemService(NotificationManager::class.java).cancel(id, NOTIFICATION) }
    }
}

fun formatClock(time: Long): String = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault())
    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
