package de.frank.wecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.Instant

class AlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    private val store = AlarmStore.get(context)
    fun allowed() = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    private fun operation(id: String, snooze: Boolean, at: Long = 0): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, AlarmReceiver::class.java)
            .setData(Uri.parse("wecker://alarm/$id/${if (snooze) "snooze" else "regular"}"))
            .putExtra("id", id).putExtra("snooze", snooze).putExtra("at", at),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    fun schedule(alarm: Alarm) {
        cancel(alarm.id)
        require(allowed()) { "Die Freigabe für genaue Weckzeiten fehlt." }
        if (alarm.enabled && alarm.nextAt > 0) set(alarm.id, false, alarm.nextAt)
        if (alarm.snoozeUntil > 0) set(alarm.id, true, alarm.snoozeUntil)
    }
    private fun set(id: String, snooze: Boolean, at: Long) {
        val show = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(at.coerceAtLeast(System.currentTimeMillis() + 1000), show), operation(id, snooze, at))
    }
    fun cancel(id: String) { manager.cancel(operation(id, false)); manager.cancel(operation(id, true)) }
    fun restore(clockChanged: Boolean = false) {
        if (!allowed()) return
        store.all().forEach { alarm ->
            val now = System.currentTimeMillis()
            var updated = alarm
            if (alarm.enabled && (clockChanged || alarm.nextAt == 0L)) {
                updated = try { updated.copy(nextAt = AlarmTime.next(alarm)) }
                catch (_: IllegalArgumentException) { updated.copy(enabled = false, nextAt = 0) }
            }
            else if (alarm.enabled && alarm.nextAt < now) {
                updated = if (now - alarm.nextAt < 12 * 60 * 60_000L) updated.copy(nextAt = now + 2000)
                else if (!alarm.repeats) updated.copy(enabled = false, nextAt = 0)
                else updated.copy(nextAt = AlarmTime.next(alarm))
            }
            if (alarm.snoozeUntil in 1 until now) updated = updated.copy(snoozeUntil = now + 2000)
            if (alarm.id in store.ringing() && AlarmService.state.value.alarm == null) updated = updated.copy(snoozeUntil = now + 2000)
            if (updated != alarm) store.put(updated)
            schedule(updated)
        }
    }
    fun save(alarm: Alarm) {
        alarm.validate()
        require(allowed() || !alarm.enabled) { "Erlaube zuerst genaue Weckzeiten in den Einstellungen." }
        val updated = alarm.copy(nextAt = if (alarm.enabled) AlarmTime.next(alarm) else 0, snoozeUntil = 0, snoozes = 0)
        store.put(updated)
        if (allowed()) schedule(updated) else cancel(updated.id)
    }
    fun accept(id: String, snooze: Boolean, at: Long): Boolean {
        val alarm = store.get(id) ?: return false
        if (snooze) {
            if (alarm.snoozeUntil == 0L || alarm.snoozeUntil != at) return false
            store.put(alarm.copy(snoozeUntil = 0))
        } else {
            if (!alarm.enabled || alarm.nextAt != at) return false
            val next = alarm.copy(enabled = alarm.repeats,
                nextAt = if (alarm.repeats) AlarmTime.next(alarm, Instant.ofEpochMilli(maxOf(at, System.currentTimeMillis()) + 1000)) else 0,
                snoozes = 0)
            store.put(next)
        }
        store.get(id)?.let(::schedule)
        return true
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        if (!AlarmScheduler(context).accept(id, intent.getBooleanExtra("snooze", false), intent.getLongExtra("at", 0))) return
        val store = AlarmStore.get(context)
        store.ringing(store.ringing() + id)
        ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java).setAction("RING"))
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler(context).restore(intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED)
    }
}
