package com.bestjournal.app

import android.app.Application
import android.app.LocaleManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.ReminderReceiver
import com.bestjournal.app.util.WeeklyReviewReceiver
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BestJournalApp : Application() {

    @Inject lateinit var prefs: android.content.SharedPreferences

    override fun onCreate() {
        super.onCreate()
        // Enforce: app language ALWAYS follows device language.
        // Android 13+ supports per-app locale overrides (via Settings → App → Language,
        // adb, or earlier app versions). Any such override would make a Polish user see
        // German/English instead of Polish. Clear overrides unconditionally on every
        // start so the system locale wins.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            if (localeManager != null && !localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
            }
        }
        FirebaseApp.initializeApp(this)

        // DSGVO: Apply analytics opt-in state BEFORE any other Firebase call.
        // Default is false — so on first launch analytics stays off until the
        // user accepts on the consent screen (ConsentScreen sets PREF_ANALYTICS_ENABLED=true).
        val analyticsEnabled = prefs.getBoolean(Constants.PREF_ANALYTICS_ENABLED, false)
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(analyticsEnabled)

        // First-launch theme enforcement: guarantee light mode on fresh install.
        // We detect "first launch" via PREF_CONSENT_SHOWN — if consent has not been
        // shown yet, the user has never interacted with this install. Write the
        // theme preferences explicitly so no leftover state (e.g. from Android
        // auto-backup or device-level dark mode defaults) can flip the UI to dark.
        if (!prefs.contains(Constants.PREF_CONSENT_SHOWN)) {
            prefs
                .edit()
                .putBoolean(Constants.PREF_DARK_THEME, false)
                .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                .apply()
        }
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
