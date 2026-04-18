package com.bestjournal.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bestjournal.app.MainActivity
import com.bestjournal.app.R

class YearlyReviewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val openAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", 0)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                3001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(context, "yearly_review")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notif_yearly_title))
                .setContentText(context.getString(R.string.notif_yearly_body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            2004,
            notification,
        )

        // Re-schedule for next year (fromUserToggle=false to preserve enabled state)
        try {
            val prefs = EncryptedPrefsProvider.get(context)
            DailyReminderManager(context, prefs).scheduleYearlyReview(fromUserToggle = false)
        } catch (e: Exception) {
            android.util.Log.e("YearlyReview", "Re-schedule failed: ${e.message}")
        }
    }
}
