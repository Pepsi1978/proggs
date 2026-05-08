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
 * Bestimmt für einen Eintrag den passenden TimeBucket (HEUTE / MORGEN / FREIBLOCK /
 * SPAETER) basierend auf Schichtdienst-Kalender und geschaetzter Dauer.
 *
 * Frank-Wunsch 2026-05-09: 4-Bucket-Modell statt 5. DIESE_WOCHE und DIESEN_MONAT
 * gibt es nicht mehr — Frank arbeitet im Schichtdienst und plant in Freibloecken,
 * nicht in Wochen oder Monaten. FREIBLOCK = naechster freier Schicht-Block.
 *
 * Heuristik (ohne manuellen Override aus EntryEntity):
 *  - Wenn die geschaetzte Dauer ins Zeitfenster heute passt -> HEUTE.
 *  - Wenn morgen passt -> MORGEN.
 *  - Wenn der naechste FREI-Tag (innerhalb 31 Tage) passt -> FREIBLOCK.
 *  - Sonst -> SPAETER (kein konkretes Datum).
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

        // 1) HEUTE — wenn das Zeitfenster heute reicht.
        if (fitsOnDay(today, calendarByDate, durationMin)) return TimeBucket.HEUTE
        // 2) MORGEN — wenn morgen passt.
        if (fitsOnDay(today.plusDays(1), calendarByDate, durationMin)) return TimeBucket.MORGEN
        // 3) FREIBLOCK — naechster FREI-Tag innerhalb 31 Tage der passt.
        for (offset in 2..31) {
            val candidate = today.plusDays(offset.toLong())
            val day = calendarByDate[candidate.toString()]
            // Bevorzuge echte FREI-Tage (Schichtkalender meldet ShiftCode.FREI).
            val isFreiBlock = day?.shiftCode == ShiftCode.FREI || day?.shiftCode == ShiftCode.URLAUB
            if (isFreiBlock && fitsOnDay(candidate, calendarByDate, durationMin)) {
                return TimeBucket.FREIBLOCK
            }
        }
        // 4) SPAETER — keine passenden Slots gefunden, kein Datum.
        return TimeBucket.SPAETER
    }

    private fun fitsOnDay(
        date: LocalDate,
        calendarByDate: Map<String, CalendarDayEntity>,
        durationMin: Int,
    ): Boolean {
        val day = calendarByDate[date.toString()]
        val available = day?.availableMinutesEstimate ?: defaultMinutesFor(day?.shiftCode)
        return available >= durationMin
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
