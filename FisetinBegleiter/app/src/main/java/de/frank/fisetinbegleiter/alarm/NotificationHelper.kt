package de.frank.fisetinbegleiter.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.frank.fisetinbegleiter.MainActivity
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity

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

    fun show(
        context: Context,
        type: AlarmType,
        dayId: Long,
        targetDay: Int,
        protocol: ProtocolTemplateEntity = ProtocolTemplateEntity(),
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val (title, text, destination) = notificationContent(type, targetDay, protocol)

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

    fun areNotificationsAvailable(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_STEPS)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun notificationSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun cancelDay(context: Context, dayId: Long) {
        val manager = NotificationManagerCompat.from(context)
        AlarmType.entries.forEach { type ->
            manager.cancel(((dayId % 100_000) * 10 + type.ordinal).toInt())
        }
    }
}
