package de.frank.jarvis

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.dienst.JarvisDienst

/**
 * Hält auch das [de.frank.jarvis.ui.AppViewModel] für den ganzen Prozess: „Hey Jarvis“ aus einer anderen App führt das
 * Gespräch ohne sichtbare Activity (Sprachblase), und öffnet Frank Jarvis danach, sieht er genau dieses Gespräch.
 */
class JarvisApplication : Application(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        // Das Logbuch schreibt ab dem ersten Augenblick mit, auch Abstürze der Oberfläche ohne laufenden Dienst.
        de.frank.jarvis.observability.Logbuch.starte(this)
        JarvisDienst.kanalAnlegen(this)
        de.frank.jarvis.weckwort.WeckwortDienst.kanaeleAnlegen(this)
        Protokoll.lade(this)
        // Laufende Timer nach einem Neustart wieder scharf stellen.
        de.frank.jarvis.timer.Timer.starte(this)
        de.frank.jarvis.timer.Stoppuhr.starte(this)
        // Überholte, unverändert gebliebene Symbiose-Vorgaben einmal ersetzen (Regel 6: Websuche statt Agent); Dateizugriff nicht auf dem Hauptfaden.
        Thread {
            runCatching { de.frank.jarvis.faehigkeit.Symbiose.aktualisiereVorgaben(this) }
                .onSuccess { neu -> if (neu.isNotEmpty()) de.frank.jarvis.observability.Logbuch.gedaechtnis("Symbiose-Vorgabe ersetzt", neu.joinToString { "S${it.id}" }) }
                .onFailure { de.frank.jarvis.observability.Logbuch.fehler("Symbiose-Vorgabe ersetzen", it) }
        }.start()
        // Dienst bei jedem Prozessstart sicherstellen: Samsung überspringt MY_PACKAGE_REPLACED, wenn Jarvis
        // beim Update vorn war. Dann fehlten Tunnel und Update-Signale bis zum nächsten Öffnen.
        JarvisDienst.abgleichen(this)
    }
}
