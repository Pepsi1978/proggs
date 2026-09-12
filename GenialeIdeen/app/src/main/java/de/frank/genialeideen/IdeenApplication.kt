package de.frank.genialeideen

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.InvalidationTracker
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenCrashHandler
import de.frank.genialeideen.observability.IdeenLog

class GenialeIdeenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        IdeenLog.start(this)
        IdeenCrashHandler.installiere(this)
        container = AppContainer(this)
        container.appLockManager.start()
        meldeAenderungenAnDieSicherung()
        // Damit beim Verlassen der App sofort gesichert wird, statt die Änderung zu verlieren.
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.autoSicherung)
    }

    /**
     * Jede Änderung am Bestand meldet sich bei der selbsttätigen Sicherung an.
     *
     * Über den Änderungsmelder von Room statt über zwanzig Aufrufstellen: Eine vergessene Stelle
     * fiele niemandem auf — die Sicherung liefe weiter, nur eben ohne die neuen Sätze.
     */
    private fun meldeAenderungenAnDieSicherung() {
        container.database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("ideen", "kategorien", "nachrichten") {
                override fun onInvalidated(tables: Set<String>) {
                    container.autoSicherung.melde("Bestand geändert")
                }
            },
        )
    }
}
