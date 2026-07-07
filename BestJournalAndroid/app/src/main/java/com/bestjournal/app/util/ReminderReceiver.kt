package com.bestjournal.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bestjournal.app.MainActivity
import com.bestjournal.app.R
import com.google.firebase.analytics.FirebaseAnalytics

class ReminderReceiver : BroadcastReceiver() {

    private fun getMessages(context: Context) =
        listOf(
            context.getString(R.string.notif_reminder_body_0),
            context.getString(R.string.notif_reminder_body_1),
            context.getString(R.string.notif_reminder_body_2),
            context.getString(R.string.notif_reminder_body_3),
            context.getString(R.string.notif_reminder_body_4),
        )

    override fun onReceive(context: Context, intent: Intent) {

        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_reminder", true)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val body = getMessages(context).random()

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notif_reminder_title))
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)

        FirebaseAnalytics.getInstance(context).logEvent("reminder_notification_shown", null)

        // Reschedule the next alarm 24 hours later so exact alarms keep firing daily
        // (setExactAndAllowWhileIdle does not repeat automatically unlike setRepeating)
        rescheduleNext(context)
    }

    private fun rescheduleNext(context: Context) {
        try {
            val prefs = EncryptedPrefsProvider.get(context)
            val reminderManager = DailyReminderManager(context, prefs)
            if (reminderManager.isReminderEnabled()) {
                reminderManager.scheduleReminder(
                    reminderManager.getReminderHour(),
                    reminderManager.getReminderMinute(),
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ReminderReceiver", "Failed to reschedule next alarm", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        private const val NOTIFICATION_ID = 2001
    }
}
