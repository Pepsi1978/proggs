package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.local.entity.JournalEntryEntity
import com.bestjournal.app.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val journalEntryDao: JournalEntryDao
) {
    fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalEntryDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getEntryById(id: Long): JournalEntry? {
        return journalEntryDao.getById(id)?.toDomain()
    }

    fun searchEntries(query: String): Flow<List<JournalEntry>> {
        return journalEntryDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun filterByCategory(category: String): Flow<List<JournalEntry>> {
        return journalEntryDao.filterByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun filterByTimeRange(startTime: Long, endTime: Long): Flow<List<JournalEntry>> {
        return journalEntryDao.filterByTimeRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveEntry(entry: JournalEntry): Long {
        return journalEntryDao.insert(entry.toEntity())
    }

    suspend fun updateEntry(entry: JournalEntry) {
        journalEntryDao.update(entry.toEntity())
    }

    suspend fun deleteEntry(entry: JournalEntry) {
        journalEntryDao.delete(entry.toEntity())
    }

    suspend fun getEntryCount(): Int {
        return journalEntryDao.getEntryCount()
    }

    suspend fun markAsSynced(ids: List<Long>) {
        journalEntryDao.markAsSynced(ids)
    }

    suspend fun getUnsyncedEntries(): List<JournalEntry> {
        return journalEntryDao.getUnsyncedEntries().map { it.toDomain() }
    }
}

private fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    timestamp = timestamp,
    rawText = rawText,
    improvedText = improvedText,
    isImproved = isImproved,
    displayText = displayText,
    audioDurationSeconds = audioDurationSeconds,
    moodTag = moodTag,
    entropyScore = entropyScore,
    adviceCategoryTags = adviceCategoryTags,
    summary = summary,
    title = title,
    isSynced = isSynced
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    timestamp = timestamp,
    rawText = rawText,
    improvedText = improvedText,
    isImproved = isImproved,
    displayText = displayText,
    audioDurationSeconds = audioDurationSeconds,
    moodTag = moodTag,
    entropyScore = entropyScore,
    adviceCategoryTags = adviceCategoryTags,
    summary = summary,
    title = title,
    isSynced = isSynced
)
