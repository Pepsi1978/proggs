package de.frank.updatestation

import android.app.Application

class UpdateStationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Benachrichtigungen.kanaeleAnlegen(this)
        PruefWorker.plane(this)
        Pruefer.bewerteGespeichert(this)
        // Plant das Wiedereinschalten von Debugging über WLAN, sobald WLAN verbunden ist
        // (höchstens einmal pro WLAN-Verbindung; KEEP verhindert Stapeln).
        WlanDebugReceiver.planen(this)
    }
}
