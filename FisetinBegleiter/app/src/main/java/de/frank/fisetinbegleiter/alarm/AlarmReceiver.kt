package de.frank.fisetinbegleiter.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.frank.fisetinbegleiter.FisetinApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE)?.let(AlarmType::valueOf) ?: return
        val dayId = intent.getLongExtra(EXTRA_DAY_ID, -1L)
        val targetDay = intent.getIntExtra(EXTRA_TARGET_DAY, 0)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as FisetinApplication
                if (type == AlarmType.BLOCK_ENDED && dayId > 0) {
                    app.repository.markBlockEnded(dayId, System.currentTimeMillis())
                }
                NotificationHelper.show(context, type, dayId, targetDay, app.repository.getProtocolSnapshot())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_DAY_ID = "day_id"
        const val EXTRA_TYPE = "alarm_type"
        const val EXTRA_TARGET_DAY = "target_day"
    }
}
