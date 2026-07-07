package com.entropyjournal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.entropyjournal.util.ReminderReceiver
import com.entropyjournal.util.WeeklyReviewReceiver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EntropyJournalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val reminderChannel =
                NotificationChannel(
                        ReminderReceiver.CHANNEL_ID,
                        "Tägliche Erinnerung",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply { description = "Erinnert dich täglich ans Tagebuchschreiben" }
            manager.createNotificationChannel(reminderChannel)

            val weeklyChannel =
                NotificationChannel(
                        WeeklyReviewReceiver.CHANNEL_ID,
                        "Wöchentlicher Rückblick",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply {
                        description = "Wöchentliche Benachrichtigung für deinen Dashboard-Rückblick"
                    }
            manager.createNotificationChannel(weeklyChannel)

            NotificationChannel(
                    "monthly_review",
                    "Monatsrückblick",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .also {
                    it.description = "Benachrichtigung wenn dein Monatsrückblick fertig ist"
                    manager.createNotificationChannel(it)
                }
            NotificationChannel(
                    "yearly_review",
                    "Jahresrückblick",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .also {
                    it.description = "Benachrichtigung wenn dein Jahresrückblick fertig ist"
                    manager.createNotificationChannel(it)
                }
        }
    }
}
