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

    suspend fun getById(id: String): SavedPromptEntity? = dao.getById(id)

    suspend fun upsert(prompt: SavedPromptEntity) = dao.upsert(prompt)

    suspend fun delete(prompt: SavedPromptEntity) = dao.delete(prompt)

    suspend fun count(): Int = dao.count()

    // Agentic-AI: schmale Updates (Frank-Wunsch 2026-05-21)

    suspend fun updateModel(id: String, model: String) {
        dao.updateModel(id, model, System.currentTimeMillis())
    }

    suspend fun updateTokenLimit(id: String, limit: Int?) {
        dao.updateTokenLimit(id, limit, System.currentTimeMillis())
    }

    suspend fun updateTrustMode(id: String, trust: Boolean) {
        dao.updateTrustMode(id, trust, System.currentTimeMillis())
    }
}
