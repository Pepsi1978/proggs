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
        // Separate, caught path first: a reminder problem never fails the alarm, and it also runs without the exact-alarm grant.
        SchlafErinnerung.sync(context, alarm)
        // Check the permission first so a refusal never removes alarms that are already planned.
        require(allowed()) { "Die Freigabe für genaue Weckzeiten fehlt." }
        if (alarm.id == failScheduleForTestId) throw IllegalStateException("Testfehler bei der Folgeplanung")
        cancelRing(alarm.id)
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
    private fun cancelRing(id: String) { manager.cancel(operation(id, false)); manager.cancel(operation(id, true)) }
    /** Cancels ringing, snooze and the planned sleep reminder; visible notifications are not touched. */
    fun cancel(id: String) { cancelRing(id); SchlafErinnerung.entferneWecker(context, id) }
    fun restore(clockChanged: Boolean = false, onlyIds: Set<String>? = null) {
        // Reminders are replanned even without the exact-alarm grant (they fall back to inexact timing).
        if (!allowed()) {
            // Nothing can be scheduled, but after a clock or zone change the stored wall-clock times must still follow,
            // otherwise a later grant would arm the old instant and the sleep reminder would be computed from it.
            if (clockChanged) {
                val ringing = store.ringing().toSet()
                store.all().filter { (onlyIds == null || it.id in onlyIds) && it.id !in ringing }.forEach { alarm ->
                    val updated = AlarmClaim.recomputeNextAt(alarm, System.currentTimeMillis())
                    if (updated == alarm) return@forEach
                    // Compare-and-set under the store lock: a parallel save or toggle is never overwritten, and a ringing
                    // that started after the snapshot above keeps its nextAt (settled when the ringing ends).
                    try { store.update(alarm.id) { current -> if (current == alarm && alarm.id !in store.ringing()) updated else current } }
                    catch (e: Exception) { Log.w(TAG, "Weckzeit nach Uhrwechsel nicht gespeichert für ${alarm.id}", e) }
                }
            }
            SchlafErinnerung.syncAll(context)
            return
        }
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
            // Unchanged condition: recompute on a clock change and also whenever nextAt is missing.
            if (updated.enabled && (clockChanged || updated.nextAt == 0L)) updated = AlarmClaim.recomputeNextAt(updated, now)
            else if (updated.enabled && updated.nextAt < now) {
                updated = if (now - updated.nextAt < AlarmClaim.STALE_MS) updated.copy(nextAt = now + 2000)
                else if (!updated.repeats) updated.copy(enabled = false, nextAt = 0)
                else updated.copy(nextAt = AlarmTime.nextRespectingSkip(updated))
            }
            if (updated.snoozeUntil in 1 until now) updated =
                if (now - updated.snoozeUntil < AlarmClaim.STALE_MS) updated.copy(snoozeUntil = now + 2000)
                else updated.copy(snoozeUntil = 0).also { store.issue(alarm.id, "Eine Schlummerpause vom ${formatMissed(alarm.snoozeUntil)} wurde verpasst und nicht nachgeholt.") }
        }
        if (updated != alarm) store.put(updated)
        scheduleSafely(updated)
        if (updated.snoozeUntil > now && ringingEntry == null) SnoozeNotice.show(context, updated) else SnoozeNotice.cancel(context, updated.id)
    }
    private fun formatMissed(at: Long) = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM. HH:mm"))
    /** Speichert und plant. Liefert false, wenn gespeichert, aber nicht geplant wurde; der Hinweis steht dann am Wecker. */
    fun save(alarm: Alarm, create: Boolean = false): Boolean {
        alarm.validate()
        // A ringing occurrence advances when it ends; an edit now would race its follow-up planning and snooze counter.
        require(alarm.id !in store.ringing()) { "Dieser Wecker klingelt gerade. Beende ihn zuerst." }
        require(allowed() || !alarm.enabled) { "Erlaube zuerst genaue Weckzeiten in den Einstellungen." }
        // Keep audio prepared while the editor was open; a stale draft must not discard it.
        val stored = if (create) null else store.get(alarm.id)
        val base = if (stored != null && stored.sameSpeechAs(alarm)) alarm.copy(prepared = stored.prepared, voiceVariants = stored.voiceVariants,
            preparedAt = stored.preparedAt, preparedSpeed = stored.preparedSpeed, preparedSignature = stored.preparedSignature,
            preparationError = stored.preparationError) else alarm
        // Saving or toggling recomputes from now and clears the skip mark, as before.
        val updated = base.copy(nextAt = if (alarm.enabled) AlarmTime.next(alarm) else 0, snoozeUntil = 0, snoozes = 0, skippedThrough = "")
        if (create) store.insertNew(updated) else store.put(updated)
        SnoozeNotice.cancel(context, updated.id)
        store.issue(updated.id, null)
        if (!allowed()) { cancel(updated.id); return true }
        return scheduleSafely(updated)
    }

    /** Schaltet nur den Aktivierungszustand um, auf dem aktuellen gespeicherten Stand statt auf einer UI-Kopie. */
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        val latest = store.get(id) ?: error("Dieser Wecker existiert nicht mehr.")
        return save(latest.copy(enabled = enabled))
    }

    /** Lässt das nächste Vorkommen aus. Die Schichtfolge bleibt am Startdatum verankert. */
    fun skipNext(id: String): Pair<Alarm, Boolean> {
        val latest = store.get(id) ?: error("Dieser Wecker existiert nicht mehr.")
        require(latest.repeats) { "Einmalige Wecker kannst du ausschalten." }
        require(latest.enabled && latest.nextAt > 0) { "Schalte den Wecker zuerst ein." }
        require(id !in store.ringing()) { "Dieser Wecker klingelt gerade. Beende ihn zuerst." }
        val updated = store.update(id) { current ->
            // Checked again under the store lock: a parallel switch-off or ringing start in between is never skipped over.
            require(current.repeats) { "Einmalige Wecker kannst du ausschalten." }
            require(current.enabled && current.nextAt > 0) { "Schalte den Wecker zuerst ein." }
            require(id !in store.ringing()) { "Dieser Wecker klingelt gerade. Beende ihn zuerst." }
            // Mark the calendar day of the skipped occurrence (all days up to and including it); repeated skips raise the mark.
            val marked = current.copy(skippedThrough = AlarmTime.localDate(current.nextAt).toString())
            marked.copy(nextAt = AlarmTime.nextRespectingSkip(marked, java.time.Instant.ofEpochMilli(maxOf(current.nextAt, System.currentTimeMillis()))))
        }!!
        return updated to scheduleSafely(updated)
    }

    /** Macht ein Auslassen rückgängig: wieder der regulär nächste Termin ab jetzt. */
    fun unskip(id: String): Pair<Alarm, Boolean> {
        require(id !in store.ringing()) { "Dieser Wecker klingelt gerade. Beende ihn zuerst." }
        // Undo removes all skips: clear the mark and return to the regular next occurrence.
        val updated = store.update(id) { current ->
            // Checked again under the store lock, so a ringing that started in between keeps its occurrence.
            require(id !in store.ringing()) { "Dieser Wecker klingelt gerade. Beende ihn zuerst." }
            current.copy(skippedThrough = "").let { cleared -> cleared.copy(nextAt = AlarmTime.next(cleared)) }
        } ?: error("Dieser Wecker existiert nicht mehr.")
        return updated to scheduleSafely(updated)
    }

    /** Beendet eine laufende Schlummerpause, ohne den Wecker selbst oder seine Wiederholung auszuschalten. */
    fun endSnooze(id: String) {
        val updated = store.update(id) { it.copy(snoozeUntil = 0, snoozes = 0) } ?: return
        SnoozeNotice.cancel(context, id)
        scheduleSafely(updated)
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
        if (intent.action == SnoozeNotice.ACTION_END) {
            runCatching { scheduler.endSnooze(id) }.onFailure { Log.e("WeckerScheduler", "Schlummerpause konnte nicht beendet werden", it) }
            return
        }
        val snooze = intent.getBooleanExtra("snooze", false)
        val claimed = scheduler.claim(id, snooze, intent.getLongExtra("at", 0)) ?: return
        if (snooze) SnoozeNotice.cancel(context, id)
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
