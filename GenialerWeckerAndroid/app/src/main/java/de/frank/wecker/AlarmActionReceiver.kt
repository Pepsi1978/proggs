package de.frank.wecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Nimmt Schlummern und Ausschalten aus der Benachrichtigung (auch aus der kleinen schwebenden Nachricht) entgegen
 * und reicht sie unverändert an den Weckdienst weiter. Ein Broadcast wird aus der Systemleiste zuverlässiger
 * zugestellt als ein direkter Dienststart; der Dienst prüft Wecker und Klingel-Id wie beim Weckbildschirm.
 */
class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val forward = Intent(context, AlarmService::class.java).setAction(intent.action).setData(intent.data)
            .putExtra("id", intent.getStringExtra("id")).putExtra("ring", intent.getLongExtra("ring", 0))
        runCatching { context.startService(forward) }
            .onFailure { Log.e("WeckerAction", "Aktion ${intent.action} konnte den Weckdienst nicht erreichen", it) }
    }
}
