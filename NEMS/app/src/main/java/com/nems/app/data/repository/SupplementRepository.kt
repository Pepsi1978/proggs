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

    suspend fun markSectionComplete(date: LocalDate, sectionId: String) {
        entryDao.markSectionComplete(
            date.format(dateFormatter),
            sectionId,
            LocalDateTime.now().format(dateTimeFormatter),
        )
    }

    suspend fun markDayComplete(date: LocalDate) {
        entryDao.markDayComplete(
            date.format(dateFormatter),
            LocalDateTime.now().format(dateTimeFormatter),
        )
    }

    suspend fun generateEntriesForDate(date: LocalDate) {
        val dateStr = date.format(dateFormatter)
        if (entryDao.countEntriesForDate(dateStr) > 0) return

        val sections = stackSectionDao.getAll() // This is Flow, need suspend version
        // We need a non-flow version for this. Let's use a direct query approach.
    }

    suspend fun generateDailyEntries(
        date: LocalDate,
        supplements: List<SupplementEntity>,
        sections: List<StackSectionEntity>,
        isDienstTag: Boolean = false,
    ) {
        val dateStr = date.format(dateFormatter)
        if (entryDao.countEntriesForDate(dateStr) > 0) return

        val dayOfYear = date.toEpochDay()
        val entries = supplements.filter { it.isActive }.mapNotNull { supplement ->
            if (!isSupplementDueOnDate(supplement, date, dayOfYear)) return@mapNotNull null

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

    fun isSupplementDueOnDate(supplement: SupplementEntity, date: LocalDate, dayOfYear: Long): Boolean {
        if (supplement.alternatesWith != null) {
            val isEvenDay = dayOfYear % 2 == 0L
            // For alternating pairs, one shows on even days, the other on odd days
            // Convention: the supplement with the lower sort order shows on even days
            return isEvenDay
        }
        return when (supplement.frequency) {
            "taeglich" -> true
            "alle 2 Tage" -> dayOfYear % 2 == 0L
            "alle 3 Tage" -> dayOfYear % 3 == 0L
            "alle 4 Tage" -> dayOfYear % 4 == 0L
            "alle 5 Tage" -> dayOfYear % 5 == 0L
            "alle 6 Tage" -> dayOfYear % 6 == 0L
            "alle 7 Tage" -> dayOfYear % 7 == 0L
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
