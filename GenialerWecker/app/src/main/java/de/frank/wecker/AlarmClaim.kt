package de.frank.wecker

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Ein angenommenes Klingelvorkommen. [at] ist der geplante Zeitpunkt des Vorkommens, [since] der
 * Annahmezeitpunkt. Einträge älterer Versionen bestehen nur aus der ID; beide Werte sind dann 0
 * und das Alter gilt als unbekannt.
 */
data class RingEntry(val id: String, val at: Long = 0, val since: Long = 0) {
    companion object {
        /** Liest die kommagetrennte Liste und die optionalen Metadaten rückwärtskompatibel. */
        fun parse(ids: String?, meta: String?): List<RingEntry> {
            val json = runCatching { JSONObject(meta ?: "{}") }.getOrElse { JSONObject() }
            return ids.orEmpty().split(',').filter { it.isNotBlank() }.distinct().map { id ->
                json.optJSONObject(id)?.let { RingEntry(id, it.optLong("at"), it.optLong("since")) } ?: RingEntry(id)
            }
        }
        fun ids(entries: List<RingEntry>) = entries.joinToString(",") { it.id }
        fun meta(entries: List<RingEntry>) = JSONObject().apply {
            entries.filter { it.at > 0 || it.since > 0 }.forEach { put(it.id, JSONObject().put("at", it.at).put("since", it.since)) }
        }.toString()
    }
}

/** Reine Zustandsübergänge der Weckannahme. Enthält bewusst keine Android-Aufrufe. */
object AlarmClaim {
    /** Ab diesem bekannten Alter wird ein nie beendetes Klingeln nicht mehr nachgeholt. */
    const val STALE_MS = 12 * 60 * 60_000L

    sealed interface Decision
    data object Rejected : Decision
    /** [next] ist null, wenn der Folgezustand nicht berechnet werden konnte; geklingelt wird trotzdem. */
    data class Accepted(val next: Alarm?, val entry: RingEntry, val error: String = "") : Decision

    fun decide(alarm: Alarm?, snooze: Boolean, at: Long, ringing: List<RingEntry>, now: Long,
        zone: ZoneId = ZoneId.systemDefault()): Decision {
        if (alarm == null || at <= 0) return Rejected
        // A redelivered broadcast for an occurrence that is already ringing must never ring twice.
        if (ringing.any { it.id == alarm.id && it.at == at }) return Rejected
        val since = ringing.find { it.id == alarm.id }?.since?.takeIf { it > 0 } ?: now
        val entry = RingEntry(alarm.id, at, since)
        if (snooze) {
            if (alarm.snoozeUntil != at) return Rejected
            return Accepted(alarm.copy(snoozeUntil = 0), entry)
        }
        if (!alarm.enabled || alarm.nextAt != at) return Rejected
        return try {
            Accepted(alarm.copy(enabled = alarm.repeats, snoozes = 0,
                nextAt = if (alarm.repeats) AlarmTime.next(alarm, Instant.ofEpochMilli(maxOf(at, now) + 1000), zone) else 0), entry)
        } catch (e: RuntimeException) {
            Accepted(null, entry, "Der Folgetermin konnte nicht berechnet werden (${e.message ?: e.javaClass.simpleName}).")
        }
    }

    /**
     * Maps the wall-clock time of an enabled alarm to the current clock and zone. Deliberately without a nextAt guard:
     * callers decide when to recompute (clock change, or nextAt == 0). A passed one-off date disables the alarm as before.
     * The snooze slot is an absolute instant and stays untouched; ringing alarms must not be passed in.
     */
    fun recomputeNextAt(alarm: Alarm, now: Long, zone: ZoneId = ZoneId.systemDefault()): Alarm {
        if (!alarm.enabled) return alarm
        return try { alarm.copy(nextAt = AlarmTime.next(alarm, Instant.ofEpochMilli(now), zone)) }
        catch (_: IllegalArgumentException) { alarm.copy(enabled = false, nextAt = 0) }
    }

    /**
     * Nach dem Ende eines Klingelns: Ein Vorkommen, das bei der Annahme nicht weitergerückt werden
     * konnte, wird jetzt rhythmustreu weitergerückt. Schlummerzähler bleiben unverändert.
     */
    fun settle(alarm: Alarm, now: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Alarm, String> {
        if (!alarm.enabled || alarm.nextAt !in 1..now) return alarm to ""
        if (!alarm.repeats) return alarm.copy(enabled = false, nextAt = 0) to ""
        return try {
            alarm.copy(nextAt = AlarmTime.next(alarm, Instant.ofEpochMilli(maxOf(alarm.nextAt, now) + 1000), zone)) to ""
        } catch (e: RuntimeException) {
            alarm.copy(enabled = false, nextAt = 0) to "Die Wiederholung konnte nicht berechnet werden. Der Wecker wurde ausgeschaltet; bitte neu speichern."
        }
    }

    enum class Recovery { REFIRE, DROP_STALE }

    /** Nur ein bekannt altes Klingeln wird verworfen; unbekanntes Alter wird nie geraten. */
    fun recover(entry: RingEntry, now: Long): Recovery =
        if (entry.since > 0 && now - entry.since > STALE_MS) Recovery.DROP_STALE else Recovery.REFIRE
}
