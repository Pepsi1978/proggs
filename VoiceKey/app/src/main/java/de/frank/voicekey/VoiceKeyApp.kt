package de.frank.voicekey

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import de.frank.voicekey.obs.Obs

class VoiceKeyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Obs.init(this, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        // Globaler Fehler-Faenger: nichts stirbt still (observability-first §2.2).
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Obs.fatal("VoiceKeyApp", "uncaught", "Unbehandelter Crash in ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                    CHANNEL_SERVICE,
                    "Wake-Word-Dienst",
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply { description = "Dauerhafte Status-Anzeige, solange der Dienst lauscht" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_HINTS, "Hinweise", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Tipp nach Neustart und wichtige Hinweise" }
        )
    }

    companion object {
        const val CHANNEL_SERVICE = "wakeword_service"
        const val CHANNEL_HINTS = "hints"
    }
}
