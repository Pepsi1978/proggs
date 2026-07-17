package de.frank.fisetinbegleiter.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.frank.fisetinbegleiter.FisetinApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in supportedActions) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as FisetinApplication
                val protocol = app.repository.getProtocolSnapshot()
                app.repository.getActiveCuresSnapshot().forEach { cure ->
                    app.alarmScheduler.restoreCure(cure, protocol)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
