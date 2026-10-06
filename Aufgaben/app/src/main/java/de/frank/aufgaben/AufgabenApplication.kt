package de.frank.aufgaben

import android.app.Application
import de.frank.aufgaben.erinnerung.Lautsprecher
import de.frank.aufgaben.erinnerung.Planer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AufgabenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Planer.kanalAnlegen(this)
        // Hat ein Absturz die für eine Erinnerung angehobene Wecker-Lautstärke stehen lassen: zurückstellen.
        Lautsprecher.zuruecksetzen(this)
        // Bei jedem Start neu planen: Ab Android 15 löscht ein Stopp erzwungenes Beenden alle Alarme.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { runCatching { Planer.planeAlle(this@AufgabenApplication) } }
    }
}
