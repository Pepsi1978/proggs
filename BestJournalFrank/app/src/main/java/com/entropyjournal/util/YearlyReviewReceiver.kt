package com.entropyjournal.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.entropyjournal.MainActivity
import com.entropyjournal.R

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
                .setContentTitle("Dein Jahresrückblick")
                .setContentText(
                    "Ein ganzes Jahr voller Erinnerungen — schau zurück auf deine Reise."
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            2004,
            notification,
        )

        // Re-schedule for next year
        try {
            val masterKey =
                androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                )
            val prefs =
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    Constants.ENCRYPTED_PREFS_NAME,
                    masterKey,
                    context,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
                        .AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
                        .AES256_GCM,
                )
            DailyReminderManager(context, prefs).scheduleYearlyReview(fromUserToggle = false)
        } catch (e: Exception) {
            android.util.Log.e("YearlyReview", "Re-schedule failed: ${e.message}")
        }
    }
}
