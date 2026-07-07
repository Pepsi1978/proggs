package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.dao.EntryFollowUpDao
import com.bestjournal.app.data.local.entity.EntryFollowUpEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class EntryFollowUpRepository
@Inject
constructor(
    private val entryFollowUpDao: EntryFollowUpDao,
) {
    fun observeForEntry(entryId: Long): Flow<List<EntryFollowUpEntity>> {
        return entryFollowUpDao.observeForEntry(entryId)
    }

    suspend fun getForEntryOnce(entryId: Long): List<EntryFollowUpEntity> {
        return entryFollowUpDao.getForEntryOnce(entryId)
    }

    suspend fun getAllOnce(): List<EntryFollowUpEntity> {
        return entryFollowUpDao.getAllOnce()
    }

    suspend fun countForEntry(entryId: Long): Int {
        return entryFollowUpDao.countForEntry(entryId)
    }

    suspend fun saveNewFollowUp(
        entryId: Long,
        rawText: String,
        improvedText: String? = null,
        isImproved: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        val display =
            if (isImproved && !improvedText.isNullOrBlank()) improvedText.trim() else rawText.trim()
        return entryFollowUpDao.insert(
            EntryFollowUpEntity(
                entryId = entryId,
                text = display,
                rawText = rawText.trim(),
                improvedText = improvedText?.trim()?.takeIf { it.isNotBlank() },
                isImproved = isImproved && !improvedText.isNullOrBlank(),
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        )
    }

    suspend fun updateFollowUp(followUp: EntryFollowUpEntity) {
        entryFollowUpDao.update(followUp)
    }

    suspend fun upsertFollowUp(followUp: EntryFollowUpEntity): Long {
        return entryFollowUpDao.insert(followUp)
    }

    suspend fun deleteFollowUp(followUp: EntryFollowUpEntity) {
        entryFollowUpDao.delete(followUp)
    }

    suspend fun deleteForEntry(entryId: Long) {
        entryFollowUpDao.deleteForEntry(entryId)
    }
}
