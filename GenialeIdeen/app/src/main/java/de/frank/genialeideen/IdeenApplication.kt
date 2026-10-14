package de.frank.genialeideen

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.InvalidationTracker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import de.frank.genialeideen.bridge.JarvisGlaettung
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
        // Vor der Freigabe: Ein Schreibaufruf der Brücke beim Kaltstart soll schon gemeldet werden.
        meldeAenderungenAnJarvis()
        bereit.countDown()
        // Von Jarvis angelegte Ideen, deren KI-Glättung ein beendeter Prozess unterbrochen hat.
        JarvisGlaettung.nachholen(container, this)
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

    /** Eigener Faden für das Signal an Jarvis — hält den Hauptfaden frei. */
    private val jarvisTakt = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var jarvisSignal: ScheduledFuture<*>? = null

    /**
     * Jede Änderung an Ideen, Kategorien oder Gesprächen meldet sich bei Jarvis, damit sein
     * Gedächtnis sofort nachzieht.
     *
     * Wie bei der Sicherung über den Änderungsmelder von Room: So zählen Oberfläche, Glättung,
     * Einspielen einer Sicherung und Schreibaufrufe über die Brücke gleichermaßen. Lesen über die
     * Brücke ändert nichts und löst daher kein Signal aus. Mehrere Änderungen binnen 700 ms
     * ergeben ein Signal; ohne installiertes Jarvis geht es ins Leere.
     */
    private fun meldeAenderungenAnJarvis() {
        val bruecke = Uri.parse("content://$packageName.jarvis")
        container.database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("ideen", "kategorien", "nachrichten") {
                override fun onInvalidated(tables: Set<String>) {
                    jarvisSignal?.cancel(false)
                    jarvisSignal = jarvisTakt.schedule(
                        { runCatching { contentResolver.notifyChange(bruecke, null) } },
                        700,
                        TimeUnit.MILLISECONDS,
                    )
                }
            },
        )
    }
}
