package de.frank.fisetinbegleiter.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.frank.fisetinbegleiter.MainActivity

object NotificationHelper {
    private const val CHANNEL_STEPS = "kur_schritte"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_STEPS,
            "Kur-Schritte",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Punktgenaue Erinnerungen für das persönliche Fisetin-Protokoll"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, type: AlarmType, dayId: Long, targetDay: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val (title, text, destination) = when (type) {
            AlarmType.MEAL_WARNING -> Triple(
                "Fettige Mahlzeit",
                "In 5 Minuten läuft das 30-Minuten-Fenster für deine fettige Mahlzeit ab.",
                "timeline",
            )
            AlarmType.SPERMIDIN_OPEN -> Triple(
                "Spermidin-Fenster offen",
                "Fenster offen: Spermidin (6 mg) + optional Piperin (10–15 mg) + leichte Mahlzeit. Zeit bis 4 h.",
                "timeline",
            )
            AlarmType.SPERMIDIN_REMINDER -> Triple(
                "Spermidin-Erinnerung",
                "Erinnerung: Spermidin-Schritt in 15 Minuten fällig, falls noch nicht erledigt.",
                "timeline",
            )
            AlarmType.BLOCK_ENDED -> Triple(
                "Antioxidantien wieder erlaubt",
                "Antioxidantien-Fenster vorbei – Vitamin C und deine restlichen Antioxidantien sind jetzt wieder erlaubt.",
                "stack",
            )
            AlarmType.NEXT_DAY -> Triple(
                "Tag $targetDay deiner Fisetin-Kur",
                "Nüchtern starten, wenn du bereit bist.",
                "today",
            )
            AlarmType.CURE_ENDED -> Triple(
                "Kur abgeschlossen",
                "Kur abgeschlossen ($targetDay Tage). Der Eintrag ist im Protokoll gespeichert.",
                "history",
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, destination)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ((dayId % 100_000) * 10 + type.ordinal).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_STEPS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(((dayId % 100_000) * 10 + type.ordinal).toInt(), notification)
    }
}
