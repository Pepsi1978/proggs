package de.frank.updatestation

import android.app.Application

class UpdateStationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Benachrichtigungen.kanaeleAnlegen(this)
        PruefWorker.plane(this)
        Pruefer.bewerteGespeichert(this)
    }
}
