package de.frank.cortex

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import de.frank.cortex.observability.CortexCrashHandler
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CortexApp : Application() {

    companion object {
        // App-Context fuer Komponenten ohne eigenen Context (z.B. Mp3Player fuer cacheDir).
        @Volatile
        lateinit var appContext: android.content.Context
            private set
    }

    // Prozessweiter Scope fuer Arbeit, die eine Activity ueberleben muss (VPN-Disconnect).
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

        // "App im Hintergrund => Tunnel trennen" lebt HIER statt in der MainActivity: Der
        // ProcessLifecycle-onStop kommt erst ~700 ms nach Activity.onStop — wird die Activity
        // gefinished (Back-Taste auf Android 8-11), war ihr Observer laengst entfernt und ihr
        // lifecycleScope gecancelt: der Tunnel blieb dauerhaft verbunden. Der Application-
        // Observer und appScope leben so lange wie der Prozess selbst.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // Eigener Auth-Prompt (Keyguard-Activity auf API 26-28) ist KEIN Verlassen der
                // App: Reset+Disconnect wuerden den gerade laufenden Entsperr-Vorgang zunichte
                // machen (PIN-Endlosschleife).
                if (AppLockState.authInProgress) {
                    CortexLog.info("CortexApp", "onStop", "Hintergrund-Wechsel waehrend Auth-Prompt — Reset/Disconnect uebersprungen")
                    return
                }
                AppLockState.authenticatedThisForeground = false
                AppLockState.appUnlocked = false
                AppLockState.foregroundAuthHandled = false
                appScope.launch { WireGuardManager.disconnect() }
            }
        })
    }
}
