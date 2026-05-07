package de.frank.entropyreducer.domain.usecase

import android.util.Log
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.domain.model.EntryStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Pruft alle aktiven Eintraege gegen den aktuellen Schichtkalender und korrigiert
 * den TimeBucket falls die Heuristik (CalculateBucketsUseCase) einen besseren
 * Bucket vorschlaegt. Aktiv ausgefuehrt nach erfolgreichem Calendar-Sync.
 *
 * Spec §22 Stufe 2 — Calendar-Sync triggert Bucket-Aktualisierung.
 */
class RebucketEntriesUseCase @Inject constructor(
    private val entryDao: EntropyEntryDao,
    private val calendarDao: CalendarDayDao,
    private val bucketing: CalculateBucketsUseCase,
) {

    suspend operator fun invoke(): Int {
        val today = java.time.LocalDate.now().toString()
        val until = java.time.LocalDate.now().plusDays(31).toString()
        val calendarMap = calendarDao.getRange(today, until).first().associateBy { it.date }

        val active = entryDao.getActive().first()
            .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }

        var updated = 0
        active.forEach { entry ->
            val suggested = bucketing.suggestRebucket(entry, calendarMap)
            if (suggested != null) {
                entryDao.update(entry.copy(timeBucket = suggested, updatedAt = System.currentTimeMillis()))
                updated++
            }
        }
        Log.i(TAG, "Rebucketing: $updated von ${active.size} Eintraegen aktualisiert")
        return updated
    }

    companion object { private const val TAG = "RebucketEntriesUseCase" }
}
