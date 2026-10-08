package de.frank.jarvis

import android.app.Application
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.dienst.JarvisDienst

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        JarvisDienst.kanalAnlegen(this)
        Protokoll.lade(this)
    }
}
