package de.frank.wecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.frank.genialeideen.R

/**
 * Startet den Weckdienst. Lehnt Android den Start ab, folgt eine best-effort Ersatzbenachrichtigung:
 * System-Weckton, höchstens [FALLBACK_TIMEOUT_MS] lang, Vollbild zur AlarmActivity, die den Dienst
 * sichtbar erneut startet. Verweigerte Benachrichtigungen, ein gesperrter Kanal oder Nicht stören
 * können sie verhindern; das ist eine Android-Grenze, keine Garantie.
 */
object AlarmRinging {
    private const val TAG = "WeckerRinging"
    const val FALLBACK_CHANNEL = "alarm_fallback_v1"
    const val FALLBACK_NOTIFICATION = 1002
    const val FALLBACK_TIMEOUT_MS = 10 * 60_000L
    @androidx.annotation.VisibleForTesting @Volatile var failStartForTestId: String? = null

    fun start(context: Context, id: String) {
        try {
            if (id == failStartForTestId) throw IllegalStateException("Testfehler beim Dienststart")
            ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java).setAction("RING").putExtra("id", id))
        } catch (e: Exception) {
            Log.e(TAG, "Weckdienst konnte nicht gestartet werden; Ersatzbenachrichtigung folgt", e)
            AlarmStore.get(context).issue(id, "Android hat den Weckdienst beim letzten Klingeln blockiert (${e.javaClass.simpleName}). Ersatzton wurde versucht.")
            showFallback(context, id)
        }
    }

    fun showFallback(context: Context, id: String) = runCatching {
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        manager.createNotificationChannel(NotificationChannel(FALLBACK_CHANNEL, "Wecker-Ersatzsignal", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Nur wenn Android den normalen Weckdienst blockiert."
            setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true); lockscreenVisibility = Notification.VISIBILITY_PUBLIC; setBypassDnd(true)
        })
        val open = PendingIntent.getActivity(context, 4, Intent(context, AlarmActivity::class.java).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val name = AlarmStore.get(context).get(id)?.name ?: "Genialer Wecker"
        val notification = NotificationCompat.Builder(context, FALLBACK_CHANNEL).setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle(name).setContentText("Tippen, um den Wecker zu öffnen und zu stoppen")
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(open).setFullScreenIntent(open, true)
            .setTimeoutAfter(FALLBACK_TIMEOUT_MS).build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        manager.notify(FALLBACK_NOTIFICATION, notification)
    }.onFailure { Log.e(TAG, "Ersatzbenachrichtigung nicht möglich", it) }

    /** Gekoppelt an den echten Dienst: sobald er klingelt oder stoppt, endet das Ersatzsignal. */
    fun cancelFallback(context: Context) {
        runCatching { context.getSystemService(NotificationManager::class.java).cancel(FALLBACK_NOTIFICATION) }
    }
}
