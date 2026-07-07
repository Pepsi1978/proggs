package com.entropyjournal.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.entropyjournal.MainActivity
import com.entropyjournal.R

class WeeklyReviewReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", 0)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                PENDING_REQUEST_CODE,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Dein Wochenrückblick")
                .setContentText(
                    "Schau dir an was dich diese Woche bewegt hat, dein Dashboard hat neue Einsichten."
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "Schau dir an was dich diese Woche bewegt hat, dein Dashboard hat neue Einsichten."
                        )
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "weekly_review"
        private const val NOTIFICATION_ID = 2002
        private const val PENDING_REQUEST_CODE = 1
    }
}
