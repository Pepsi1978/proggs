package de.frank.wecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

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
        // Check the permission first so a refusal never removes alarms that are already planned.
        require(allowed()) { "Die Freigabe für genaue Weckzeiten fehlt." }
        if (alarm.id == failScheduleForTestId) throw IllegalStateException("Testfehler bei der Folgeplanung")
        cancel(alarm.id)
        // An overdue occurrence that is still ringing is advanced when the ringing ends, never re-fired here.
        val overdueRinging = alarm.nextAt <= System.currentTimeMillis() && alarm.id in store.ringing()
        if (alarm.enabled && alarm.nextAt > 0 && !overdueRinging) set(alarm.id, false, alarm.nextAt)
        if (alarm.snoozeUntil > 0) set(alarm.id, true, alarm.snoozeUntil)
    }
    /** Plant ohne Exception nach außen; Fehler werden sichtbar am Wecker vermerkt. */
    fun scheduleSafely(alarm: Alarm): Boolean = try {
        schedule(alarm)
        if (store.issues.value[alarm.id]?.startsWith(PLAN_ISSUE) == true) store.issue(alarm.id, null)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Planung fehlgeschlagen für ${alarm.id}", e)
        store.issue(alarm.id, "$PLAN_ISSUE ${e.message ?: e.javaClass.simpleName}. Neuer Versuch nach dem nächsten Klingeln oder App-Start.")
        false
    }
    private fun set(id: String, snooze: Boolean, at: Long) {
        val show = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(at.coerceAtLeast(System.currentTimeMillis() + 1000), show), operation(id, snooze, at))
    }
    fun cancel(id: String) { manager.cancel(operation(id, false)); manager.cancel(operation(id, true)) }
    fun restore(clockChanged: Boolean = false, onlyIds: Set<String>? = null) {
        if (!allowed()) return
        val all = store.ringingEntries().filter { onlyIds == null || it.id in onlyIds }
        val ringing = all.filterNot(AlarmService::wasStopped)
        // An occurrence stopped in this process whose removal could not be saved must never be re-fired.
        if (ringing.size != all.size) runCatching { store.writeRinging(store.ringingEntries().filterNot(AlarmService::wasStopped)) }
        val idle = AlarmService.state.value.alarm == null
        store.all().filter { onlyIds == null || it.id in onlyIds }.forEach { alarm ->
            try { restoreOne(alarm, ringing.find { it.id == alarm.id }, idle, clockChanged, System.currentTimeMillis()) }
            catch (e: Exception) { Log.w(TAG, "Wiederherstellung fehlgeschlagen für ${alarm.id}", e) }
        }
    }
    internal fun restoreOne(alarm: Alarm, entry: RingEntry?, idle: Boolean, clockChanged: Boolean, now: Long) {
        var updated = alarm
        var ringingEntry = entry
        if (entry != null && idle) {
            when (AlarmClaim.recover(entry, now)) {
                AlarmClaim.Recovery.REFIRE -> {
                    // Exactly one path re-fires an interrupted ringing: the snooze slot. nextAt stays untouched.
                    if (updated.snoozeUntil <= now) updated = updated.copy(snoozeUntil = now + 2000)
                    if (entry.since == 0L) store.writeRinging(store.ringingEntries().map { if (it.id == entry.id) it.copy(since = now) else it })
                }
                AlarmClaim.Recovery.DROP_STALE -> {
                    store.writeRinging(store.ringingEntries().filterNot { it.id == entry.id })
                    ringingEntry = null
                    val (settled, error) = AlarmClaim.settle(updated, now)
                    updated = settled
                    store.issue(alarm.id, error.ifBlank { "Ein Klingeln vom ${formatMissed(entry.since)} wurde nicht beendet und nicht nachgeholt." })
                }
            }
        }
        if (ringingEntry == null) {
            if (updated.enabled && (clockChanged || updated.nextAt == 0L)) {
                updated = try { updated.copy(nextAt = AlarmTime.next(updated)) }
                catch (_: IllegalArgumentException) { updated.copy(enabled = false, nextAt = 0) }
            }
            else if (updated.enabled && updated.nextAt < now) {
                updated = if (now - updated.nextAt < AlarmClaim.STALE_MS) updated.copy(nextAt = now + 2000)
                else if (!updated.repeats) updated.copy(enabled = false, nextAt = 0)
                else updated.copy(nextAt = AlarmTime.next(updated))
            }
            if (updated.snoozeUntil in 1 until now) updated =
                if (now - updated.snoozeUntil < AlarmClaim.STALE_MS) updated.copy(snoozeUntil = now + 2000)
                else updated.copy(snoozeUntil = 0).also { store.issue(alarm.id, "Eine Schlummerpause vom ${formatMissed(alarm.snoozeUntil)} wurde verpasst und nicht nachgeholt.") }
        }
        if (updated != alarm) store.put(updated)
        scheduleSafely(updated)
    }
    private fun formatMissed(at: Long) = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM. HH:mm"))
    fun save(alarm: Alarm, create: Boolean = false) {
        alarm.validate()
        require(allowed() || !alarm.enabled) { "Erlaube zuerst genaue Weckzeiten in den Einstellungen." }
        val updated = alarm.copy(nextAt = if (alarm.enabled) AlarmTime.next(alarm) else 0, snoozeUntil = 0, snoozes = 0)
        if (create) store.insertNew(updated) else store.put(updated)
        if (allowed()) schedule(updated) else cancel(updated.id)
        store.issue(updated.id, null)
    }

    /**
     * Nimmt ein ausgelöstes Vorkommen an. Liefert den Folgezustand zum Planen, oder null, wenn nichts
     * zu klingeln ist. [Claimed.persisted] ist false, wenn nur aus dem Speicher geklingelt werden kann.
     */
    fun claim(id: String, snooze: Boolean, at: Long): Claimed? {
        val now = System.currentTimeMillis()
        return try {
            when (val decision = store.claim(id) { alarm, ringing -> AlarmClaim.decide(alarm, snooze, at, ringing, now) }) {
                is AlarmClaim.Accepted -> Claimed(decision.next, persisted = true)
                AlarmClaim.Rejected -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Weckannahme konnte nicht gespeichert werden; es wird trotzdem geklingelt", e)
            val alarm = store.get(id) ?: return null
            val valid = if (snooze) alarm.snoozeUntil == at else alarm.enabled && alarm.nextAt == at
            if (valid) Claimed(null, persisted = false) else null
        }
    }
    data class Claimed(val next: Alarm?, val persisted: Boolean)

    /** Nach dem Ende eines Klingelns: nicht weitergerücktes Vorkommen nachziehen und Planung genau einmal erneut versuchen. */
    fun settleAfterRing(id: String) {
        val alarm = store.get(id) ?: return
        val (settled, error) = AlarmClaim.settle(alarm, System.currentTimeMillis())
        try { if (settled != alarm) store.put(settled) }
        catch (e: Exception) { store.issue(id, "Der Folgetermin konnte nicht gespeichert werden: ${e.message}"); return }
        scheduleSafely(settled)
        if (error.isNotBlank()) store.issue(id, error)
    }

    companion object {
        private const val TAG = "WeckerScheduler"
        const val PLAN_ISSUE = "Folgetermin nicht geplant:"
        @androidx.annotation.VisibleForTesting @Volatile var failScheduleForTestId: String? = null
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val scheduler = AlarmScheduler(context)
        val claimed = scheduler.claim(id, intent.getBooleanExtra("snooze", false), intent.getLongExtra("at", 0)) ?: return
        // Ringing is requested before any follow-up planning, so a planning failure can never silence this alarm.
        AlarmRinging.start(context, id)
        if (claimed.persisted) claimed.next?.let(scheduler::scheduleSafely)
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler(context).restore(intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED)
    }
}
