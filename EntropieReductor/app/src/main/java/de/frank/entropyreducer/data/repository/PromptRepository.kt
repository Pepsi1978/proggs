package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PromptRepository @Inject constructor(private val dao: SavedPromptDao) {
    fun getAll(): Flow<List<SavedPromptEntity>> = dao.getAll()

    fun getActive(): Flow<List<SavedPromptEntity>> = dao.getActive()

    fun getActiveByCategory(
        cat: de.frank.entropyreducer.domain.model.PromptCategory
    ): Flow<List<SavedPromptEntity>> = dao.getActiveByCategory(cat)

    suspend fun upsert(prompt: SavedPromptEntity) = dao.upsert(prompt)

    suspend fun delete(prompt: SavedPromptEntity) = dao.delete(prompt)

    suspend fun count(): Int = dao.count()
}
