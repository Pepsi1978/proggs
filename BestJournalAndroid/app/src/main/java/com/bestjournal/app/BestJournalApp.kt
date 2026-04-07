package com.bestjournal.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.bestjournal.app.util.ReminderReceiver
import com.bestjournal.app.util.WeeklyReviewReceiver
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BestJournalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Debug builds use DebugAppCheckProvider (no Play Integrity needed).
        // Release builds use Play Integrity for production App Check.
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val dailyChannel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "T\u00e4gliche Erinnerung",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Erinnert dich t\u00e4glich ans Tagebuchschreiben"
            }
            manager.createNotificationChannel(dailyChannel)

            val weeklyChannel = NotificationChannel(
                WeeklyReviewReceiver.CHANNEL_ID,
                "W\u00f6chentlicher R\u00fcckblick",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Dein Wochenr\u00fcckblick jeden Sonntag um 19:00 Uhr"
            }
            manager.createNotificationChannel(weeklyChannel)
        }
    }
}
