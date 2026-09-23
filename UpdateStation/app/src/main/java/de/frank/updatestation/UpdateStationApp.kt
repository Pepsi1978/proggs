package de.frank.updatestation

import android.app.Application

class UpdateStationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Benachrichtigungen.kanaeleAnlegen(this)
        PruefWorker.plane(this)
        Pruefer.bewerteGespeichert(this)
        // Läuft bei jedem Prozessstart (auch durch den PruefWorker) und holt ein
        // abgeschaltetes Debugging über WLAN zurück, z. B. nach einem Netzwechsel.
        WlanDebugReceiver.einschalten(this)
    }
}
