package de.frank.genialeideen

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.InvalidationTracker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenCrashHandler
import de.frank.genialeideen.observability.IdeenLog

class GenialeIdeenApplication : Application() {
    lateinit var container: AppContainer
        private set

    /**
     * Fällt erst, wenn [container] steht.
     *
     * Startet Jarvis die App kalt über die Brücke, veröffentlicht Android die ContentProvider vor
     * [onCreate] — der Binder-Aufruf kommt dann parallel an und träfe auf den leeren Container.
     */
    private val bereit = CountDownLatch(1)

    /** Wartet höchstens [ms] Millisekunden auf den Container; `null`, wenn die App bis dahin nicht bereit ist. */
    fun warteAufContainer(ms: Long = 15_000): AppContainer? =
        if (bereit.await(ms, TimeUnit.MILLISECONDS)) container else null

    override fun onCreate() {
        super.onCreate()
        IdeenLog.start(this)
        IdeenCrashHandler.installiere(this)
        container = AppContainer(this)
        bereit.countDown()
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
