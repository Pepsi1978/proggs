package com.nems.app.data.repository

import com.nems.app.data.local.dao.DailyCompletionStat
import com.nems.app.data.local.dao.MissedSupplement
import com.nems.app.data.local.dao.StackSectionDao
import com.nems.app.data.local.dao.SupplementDao
import com.nems.app.data.local.dao.SupplementEntryDao
import com.nems.app.data.local.entity.StackSectionEntity
import com.nems.app.data.local.entity.SupplementEntity
import com.nems.app.data.local.entity.SupplementEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class SupplementRepository @Inject constructor(
    private val supplementDao: SupplementDao,
    private val stackSectionDao: StackSectionDao,
    private val entryDao: SupplementEntryDao,
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getAllActiveSuplements(): Flow<List<SupplementEntity>> = supplementDao.getAllActive()

    fun getAllSupplements(): Flow<List<SupplementEntity>> = supplementDao.getAll()

    fun getAllSections(): Flow<List<StackSectionEntity>> = stackSectionDao.getAll()

    fun getSupplementsBySection(sectionId: String): Flow<List<SupplementEntity>> =
        supplementDao.getBySectionId(sectionId)

    fun getEntriesForDate(date: LocalDate): Flow<List<SupplementEntryEntity>> =
        entryDao.getEntriesForDate(date.format(dateFormatter))

    fun getDailyCompletionStats(): Flow<List<DailyCompletionStat>> =
        entryDao.getDailyCompletionStats()

    fun getCompletionStatsForRange(start: LocalDate, end: LocalDate): Flow<List<DailyCompletionStat>> =
        entryDao.getCompletionStatsForRange(start.format(dateFormatter), end.format(dateFormatter))

    fun getMostMissed(start: LocalDate, end: LocalDate): Flow<List<MissedSupplement>> =
        entryDao.getMostMissedSupplements(start.format(dateFormatter), end.format(dateFormatter))

    suspend fun toggleEntry(entryId: String, taken: Boolean) {
        val timestamp = if (taken) LocalDateTime.now().format(dateTimeFormatter) else null
        entryDao.updateTakenStatus(entryId, taken, timestamp)
    }

    suspend fun toggleSectionComplete(date: LocalDate, sectionId: String) {
        val dateStr = date.format(dateFormatter)
        val untaken = entryDao.countUntakenInSection(dateStr, sectionId)
        if (untaken > 0) {
            entryDao.markSectionComplete(dateStr, sectionId, LocalDateTime.now().format(dateTimeFormatter))
        } else {
            entryDao.markSectionIncomplete(dateStr, sectionId)
        }
    }

    suspend fun toggleDayComplete(date: LocalDate) {
        val dateStr = date.format(dateFormatter)
        val untaken = entryDao.countUntakenForDate(dateStr)
        if (untaken > 0) {
            entryDao.markDayComplete(dateStr, LocalDateTime.now().format(dateTimeFormatter))
        } else {
            entryDao.markDayIncomplete(dateStr)
        }
    }

    suspend fun generateDailyEntries(
        date: LocalDate,
        supplements: List<SupplementEntity>,
        sections: List<StackSectionEntity>,
        isDienstTag: Boolean = false,
    ) {
        val dateStr = date.format(dateFormatter)
        if (entryDao.countEntriesForDate(dateStr) > 0) return

        val epochDay = date.toEpochDay()
        val activeSupplements = supplements.filter { it.isActive }

        // Build a map of alternating pairs: supplementId -> partner supplement
        // so we can determine which one wins for this day.
        val alternatingPairs: Map<String, SupplementEntity> = activeSupplements
            .filter { it.alternatesWith != null }
            .associateBy { it.id }

        // Track IDs already decided (suppressed) to avoid adding the losing half of a pair.
        val suppressedIds = mutableSetOf<String>()

        // Pre-process alternating pairs: for each pair, suppress the one that should NOT show today.
        val processedPairs = mutableSetOf<String>() // track pair IDs to avoid double-processing
        for (supplement in activeSupplements) {
            val partnerId = supplement.alternatesWith ?: continue
            if (supplement.id in processedPairs || partnerId in processedPairs) continue

            val partner = alternatingPairs[partnerId] ?: continue

            // The supplement with the LOWER sortOrder shows on EVEN epochDays.
            val (evenSupplement, oddSupplement) = if (supplement.sortOrder <= partner.sortOrder) {
                supplement to partner
            } else {
                partner to supplement
            }

            val isEvenDay = Math.abs(epochDay) % 2 == 0L
            val suppressedId = if (isEvenDay) oddSupplement.id else evenSupplement.id
            suppressedIds.add(suppressedId)

            processedPairs.add(supplement.id)
            processedPairs.add(partnerId)
        }

        val entries = activeSupplements
            .filter { it.id !in suppressedIds }
            .mapNotNull { supplement ->
                if (!isSupplementDueOnDate(supplement, date, epochDay)) return@mapNotNull null

                SupplementEntryEntity(
                    id = UUID.randomUUID().toString(),
                    date = dateStr,
                    supplementId = supplement.id,
                    stackSectionId = supplement.defaultStackSectionId,
                    taken = false,
                    takenTimestamp = null,
                    notes = null,
                )
            }

        entryDao.insertAll(entries)
    }

    // DECISION: alternating supplements are handled before this function is called (in generateDailyEntries),
    // so here we only handle the non-alternation frequency check.
    // Math.abs(epochDay) prevents wrong modulo results for negative epoch days (dates before 1970-01-01).
    fun isSupplementDueOnDate(supplement: SupplementEntity, date: LocalDate, epochDay: Long): Boolean {
        // Alternating logic is resolved upstream in generateDailyEntries via sortOrder.
        // Here we just return true so the upstream suppression logic takes effect.
        if (supplement.alternatesWith != null) return true

        val absDay = Math.abs(epochDay)
        return when (supplement.frequency) {
            "taeglich" -> true
            "alle 2 Tage" -> absDay % 2 == 0L
            "alle 3 Tage" -> absDay % 3 == 0L
            "alle 4 Tage" -> absDay % 4 == 0L
            "alle 5 Tage" -> absDay % 5 == 0L
            "alle 6 Tage" -> absDay % 6 == 0L
            "alle 7 Tage" -> absDay % 7 == 0L
            else -> true
        }
    }

    suspend fun insertSeedSections(sections: List<StackSectionEntity>) {
        if (stackSectionDao.count() == 0) {
            stackSectionDao.insertAll(sections)
        }
    }

    suspend fun insertSeedSupplements(supplements: List<SupplementEntity>) {
        if (supplementDao.count() == 0) {
            supplementDao.insertAll(supplements)
        }
    }

    suspend fun updateSupplement(supplement: SupplementEntity) = supplementDao.update(supplement)

    suspend fun deleteSupplement(supplement: SupplementEntity) = supplementDao.delete(supplement)

    suspend fun insertSupplement(supplement: SupplementEntity) = supplementDao.insert(supplement)

    suspend fun updateSection(section: StackSectionEntity) = stackSectionDao.update(section)
}
