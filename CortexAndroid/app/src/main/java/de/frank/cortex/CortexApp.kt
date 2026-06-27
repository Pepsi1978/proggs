package de.frank.cortex

import android.app.Application
import de.frank.cortex.observability.CortexCrashHandler
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.WireGuardManager

class CortexApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Observability zuerst
        CortexLog.init(this)
        CortexLog.info("CortexApp", "onCreate", "Cortex gestartet", mapOf("version" to BuildConfig.VERSION_NAME))

        // Globaler Crash-Fänger
        Thread.setDefaultUncaughtExceptionHandler(
            CortexCrashHandler(Thread.getDefaultUncaughtExceptionHandler())
        )

        // SettingsStore initialisieren
        de.frank.cortex.data.SettingsStore.init(this)

        // WireGuard initialisieren
        WireGuardManager.init(this)
        WireGuardManager.loadSavedConfig()
    }
}
