package com.bestjournal.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.bestjournal.app.MainActivity
import com.bestjournal.app.R

class ReminderReceiver : BroadcastReceiver() {

    private val messages = listOf(
        "Was bewegt dich heute?",
        "Ein Moment f\u00fcr dich \u2014 halte ihn fest",
        "Dein zuk\u00fcnftiges Ich wird dir danken",
        "Nur ein Gedanke reicht \u2014 leg los",
        "Was war heute dein Highlight?",
    )

    override fun onReceive(context: Context, intent: Intent) {

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_reminder", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = messages.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Zeit f\u00fcr dein Tagebuch")
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)

        FirebaseAnalytics.getInstance(context).logEvent("reminder_notification_shown", null)
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        private const val NOTIFICATION_ID = 2001
    }
}
