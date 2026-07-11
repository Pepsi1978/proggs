package de.frank.cortex

import android.app.Application
import de.frank.cortex.observability.CortexCrashHandler
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.WireGuardManager

class CortexApp : Application() {

    companion object {
        // App-Context fuer Komponenten ohne eigenen Context (z.B. Mp3Player fuer cacheDir).
        @Volatile
        lateinit var appContext: android.content.Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Observability zuerst
        CortexLog.init(this)
        CortexLog.info("CortexApp", "onCreate", "Cortex gestartet", mapOf("version" to BuildConfig.VERSION_NAME))

        // Globaler Crash-Fänger
        Thread.setDefaultUncaughtExceptionHandler(
            CortexCrashHandler(Thread.getDefaultUncaughtExceptionHandler())
        )

        // SettingsStore initialisieren
        de.frank.cortex.data.SettingsStore.init(this)
        de.frank.cortex.data.TtsUsageStore.init(this)

        // Lokale Chat-Sessions initialisieren
        de.frank.cortex.data.ChatSessionStore.init(this)

        // WireGuard initialisieren
        WireGuardManager.init(this)
        WireGuardManager.loadSavedConfig()
    }
}
