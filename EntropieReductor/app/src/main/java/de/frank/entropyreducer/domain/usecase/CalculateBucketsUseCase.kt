package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.model.TimeBucket
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Bestimmt fuer einen Eintrag den passenden TimeBucket (HEUTE / MORGEN / DIESE_WOCHE /
 * DIESEN_MONAT / SPAETER) basierend auf Schichtdienst-Kalender und geschaetzter Dauer.
 *
 * Heuristik:
 *  - Wenn der Eintrag schon einen expliziten Bucket hat (Spec §10.3 — manuelle Korrektur),
 *    wird er respektiert.
 *  - Sonst: Wenn die geschaetzte Dauer in das verfuegbare Fenster des heutigen Tages passt,
 *    -> HEUTE. Falls nicht, Tage durchlaufen bis ein Tag passt; Buckets nach Distanz:
 *      <= morgen -> MORGEN, <= 7 Tage -> DIESE_WOCHE, <= 31 Tage -> DIESEN_MONAT, sonst SPAETER.
 *  - Wenn keine Kalenderdaten existieren: Default-Bucket aus Eintrag-Schwere.
 */
class CalculateBucketsUseCase @Inject constructor() {

    fun bucketFor(
        entry: EntropyEntryEntity,
        calendarByDate: Map<String, CalendarDayEntity>,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        manualOverride: TimeBucket? = null,
    ): TimeBucket {
        manualOverride?.let { return it }

        val durationMin = entry.estimatedDurationMinutes ?: defaultDurationFor(entry.severity)

        // Suche den ersten Tag (heute, morgen, ...) dessen verfuegbares Zeitfenster ausreicht.
        repeat(31) { offset ->
            val candidate = today.plusDays(offset.toLong())
            val day = calendarByDate[candidate.toString()]
            val available = day?.availableMinutesEstimate ?: defaultMinutesFor(day?.shiftCode)
            if (available >= durationMin) {
                return when (offset) {
                    0 -> TimeBucket.HEUTE
                    1 -> TimeBucket.MORGEN
                    in 2..7 -> TimeBucket.DIESE_WOCHE
                    in 8..31 -> TimeBucket.DIESEN_MONAT
                    else -> TimeBucket.SPAETER
                }
            }
        }
        return TimeBucket.SPAETER
    }

    /**
     * Ueberprueft, ob der vom KI vergebene Bucket angesichts des aktualisierten Kalenders
     * korrigiert werden sollte (z.B. wenn HEUTE inzwischen Tagdienst ist und der Eintrag
     * 90 Minuten braucht).
     *
     * Liefert null, wenn keine Korrektur noetig ist.
     */
    fun suggestRebucket(
        entry: EntropyEntryEntity,
        calendarByDate: Map<String, CalendarDayEntity>,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): TimeBucket? {
        val computed = bucketFor(entry, calendarByDate, today, manualOverride = null)
        return computed.takeIf { it != entry.timeBucket }
    }

    private fun defaultDurationFor(severity: Int): Int = when {
        severity >= 8 -> 60
        severity >= 5 -> 30
        else -> 15
    }

    private fun defaultMinutesFor(shift: ShiftCode?): Int = when (shift) {
        ShiftCode.FREI -> 480
        ShiftCode.URLAUB -> 600
        ShiftCode.NACHTDIENST -> 75
        ShiftCode.TAGDIENST -> 45
        ShiftCode.UNBEKANNT, null -> 240
    }
}
