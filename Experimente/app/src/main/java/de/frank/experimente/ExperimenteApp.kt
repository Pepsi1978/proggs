package de.frank.experimente

import android.app.Application
import de.frank.experimente.data.settings.Einstellungen

class ExperimenteApp : Application() {

    lateinit var einstellungen: Einstellungen
        private set

    override fun onCreate() {
        super.onCreate()
        einstellungen = Einstellungen(this)
    }
}
