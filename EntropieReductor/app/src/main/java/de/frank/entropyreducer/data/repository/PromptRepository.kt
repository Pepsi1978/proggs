package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptRepository @Inject constructor(
    private val dao: SavedPromptDao,
) {
    fun getAll(): Flow<List<SavedPromptEntity>> = dao.getAll()
    fun getActive(): Flow<List<SavedPromptEntity>> = dao.getActive()

    suspend fun upsert(prompt: SavedPromptEntity) = dao.upsert(prompt)
    suspend fun delete(prompt: SavedPromptEntity) = dao.delete(prompt)
    suspend fun count(): Int = dao.count()
}
