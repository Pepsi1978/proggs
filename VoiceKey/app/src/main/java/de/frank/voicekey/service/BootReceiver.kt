package de.frank.voicekey.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import de.frank.voicekey.VoiceKeyApp
import de.frank.voicekey.data.WakeWordRepository
import de.frank.voicekey.obs.Obs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Nach einem Geraete-Neustart darf ein Mikrofon-Foreground-Service NICHT direkt aus
 * BOOT_COMPLETED starten (Android 14+, Almanach §5). Stattdessen: Tipp-Notification —
 * ein Tipp oeffnet die App, die den Dienst aus dem Vordergrund startet.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = WakeWordRepository(context.applicationContext).serviceEnabled.first()
                Obs.i("BootReceiver", "onReceive", "Boot erkannt", mapOf("serviceEnabled" to enabled, "action" to intent.action))
                if (enabled) showTapToStartNotification(context)
            } catch (e: Exception) {
                Obs.e("BootReceiver", "onReceive", "Boot-Handling fehlgeschlagen", emptyMap(), e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showTapToStartNotification(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.putExtra(EXTRA_AUTOSTART, true)
        val contentIntent = PendingIntent.getActivity(
            context, 2, launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, VoiceKeyApp.CHANNEL_HINTS)
            .setSmallIcon(de.frank.voicekey.R.drawable.ic_stat_mic)
            .setContentTitle("VoiceKey wieder aktivieren")
            .setContentText("Tippen, um das Wake-Word-Lauschen nach dem Neustart zu starten.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Obs.w("BootReceiver", "showTapToStartNotification", "POST_NOTIFICATIONS fehlt", emptyMap(), e)
        }
    }

    companion object {
        const val EXTRA_AUTOSTART = "autostart_service"
        private const val NOTIFICATION_ID = 1002
    }
}
