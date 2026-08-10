package de.frank.experimente.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.frank.experimente.data.settings.Einstellungen

/**
 * Nach einem Neustart des Geräts sind alle Weckzeiten weg — hier werden sie neu gesetzt (§6).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, absicht: Intent) {
        if (absicht.action != Intent.ACTION_BOOT_COMPLETED) return
        Erinnerungen.setzeAlle(ctx, Einstellungen(ctx))
    }
}
