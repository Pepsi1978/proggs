package de.frank.experimente.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.frank.experimente.ExperimenteApp

/**
 * Wecker überstehen keinen Neustart des Geräts — nach dem Hochfahren neu setzen.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? ExperimenteApp ?: return
        Erinnerungen.allesNeuSetzen(context, app.einstellungen)
    }
}
