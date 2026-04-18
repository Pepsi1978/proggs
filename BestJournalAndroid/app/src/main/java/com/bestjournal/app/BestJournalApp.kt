package com.bestjournal.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.bestjournal.app.util.ReminderReceiver
import com.bestjournal.app.util.WeeklyReviewReceiver
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BestJournalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Debug builds use DebugAppCheckProvider (no Play Integrity needed).
        // Release builds use Play Integrity for production App Check.
        // DebugAppCheckProviderFactory is only available as debugImplementation,
        // so reflection is used to avoid a compile error in release builds.
        val factory: AppCheckProviderFactory =
            if (BuildConfig.DEBUG) {
                try {
                    val clazz =
                        Class.forName(
                            "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
                        )
                    @Suppress("UNCHECKED_CAST")
                    clazz.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
                } catch (e: Exception) {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                }
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val dailyChannel =
                NotificationChannel(
                        ReminderReceiver.CHANNEL_ID,
                        getString(R.string.notif_channel_daily_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply { description = getString(R.string.notif_channel_daily_desc) }
            manager.createNotificationChannel(dailyChannel)

            val weeklyChannel =
                NotificationChannel(
                        WeeklyReviewReceiver.CHANNEL_ID,
                        getString(R.string.notif_channel_weekly_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply { description = getString(R.string.notif_channel_weekly_desc) }
            manager.createNotificationChannel(weeklyChannel)

            NotificationChannel(
                    "monthly_review",
                    getString(R.string.notif_channel_monthly_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .also {
                    it.description = getString(R.string.notif_channel_monthly_desc)
                    manager.createNotificationChannel(it)
                }
            NotificationChannel(
                    "yearly_review",
                    getString(R.string.notif_channel_yearly_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                .also {
                    it.description = getString(R.string.notif_channel_yearly_desc)
                    manager.createNotificationChannel(it)
                }
        }
    }
}
