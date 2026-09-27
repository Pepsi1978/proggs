package de.frank.wecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Flüchtige Schlummerpause für das Testwecken. Es wird nichts gespeichert: Nur ein Token im Speicher
 * dieses Prozesses berechtigt die Fortsetzung. Nach Prozessende, Beenden, neuem Test oder einem echten
 * Alarm verfällt der Systemweckruf wirkungslos.
 */
object TestSnooze {
    data class Pending(val id: String, val token: Long, val snoozes: Int)
    @Volatile private var pending: Pending? = null
    private var counter = 0L

    private fun operation(context: Context, token: Long) = PendingIntent.getBroadcast(context, 7,
        Intent(context, TestSnoozeReceiver::class.java).putExtra("token", token),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    @Synchronized fun start(context: Context, id: String, snoozes: Int, at: Long) {
        cancel(context)
        val manager = context.getSystemService(AlarmManager::class.java)
        check(android.os.Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) { "Die Freigabe für genaue Weckzeiten fehlt." }
        val next = Pending(id, System.nanoTime() + ++counter, snoozes)
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation(context, next.token))
        pending = next
    }

    @Synchronized fun cancel(context: Context) {
        if (pending == null) return
        pending = null
        runCatching { context.getSystemService(AlarmManager::class.java).cancel(operation(context, 0)) }
    }

    @Synchronized fun take(token: Long): Pending? = pending?.takeIf { it.token == token }?.also { pending = null }

    @androidx.annotation.VisibleForTesting fun pendingForTest() = pending
}

class TestSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val next = TestSnooze.take(intent.getLongExtra("token", -1)) ?: return
        // App visible: same silent path as the test button. In the background Android needs the alarm notification.
        val visible = androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java).setAction("TEST")
                .putExtra("id", next.id).putExtra("testSnoozes", next.snoozes).putExtra("quiet", visible))
            if (visible) context.startActivity(Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { Log.w("WeckerTest", "Test-Schlummerpause konnte nicht fortgesetzt werden", it) }
    }
}
