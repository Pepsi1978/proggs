package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.SavedPromptDao
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Frank-Bugfix 2026-05-22: Mutationen triggern jetzt sofort einen
 * Drive-Backup-Sync — sonst gehen frisch gespeicherte Prompts beim
 * Wechsel auf das zweite Geraet verloren (Backup wurde nur ueber
 * andere Pfade getriggert, nicht zuverlaessig pro Prompt-Aenderung).
 */
@Singleton
class PromptRepository
@Inject
constructor(
    private val dao: SavedPromptDao,
    private val syncCoordinator: Lazy<SyncCoordinator>,
) {
    fun getAll(): Flow<List<SavedPromptEntity>> = dao.getAll()

    fun getActive(): Flow<List<SavedPromptEntity>> = dao.getActive()

    fun getActiveByCategory(
        cat: de.frank.entropyreducer.domain.model.PromptCategory
    ): Flow<List<SavedPromptEntity>> = dao.getActiveByCategory(cat)

    suspend fun getById(id: String): SavedPromptEntity? = dao.getById(id)

    suspend fun upsert(prompt: SavedPromptEntity) {
        dao.upsert(prompt)
        syncCoordinator.get().requestSync("Agentic-AI: Prompt geaendert")
    }

    suspend fun delete(prompt: SavedPromptEntity) {
        dao.delete(prompt)
        syncCoordinator.get().requestSync("Agentic-AI: Prompt geaendert")
    }

    suspend fun count(): Int = dao.count()

    // Agentic-AI: schmale Updates (Frank-Wunsch 2026-05-21)

    suspend fun updateModel(id: String, model: String) {
        dao.updateModel(id, model, System.currentTimeMillis())
        syncCoordinator.get().requestSync("Agentic-AI: Prompt geaendert")
    }

    suspend fun updateTokenLimit(id: String, limit: Int?) {
        dao.updateTokenLimit(id, limit, System.currentTimeMillis())
        syncCoordinator.get().requestSync("Agentic-AI: Prompt geaendert")
    }

    suspend fun updateTrustMode(id: String, trust: Boolean) {
        dao.updateTrustMode(id, trust, System.currentTimeMillis())
        syncCoordinator.get().requestSync("Agentic-AI: Prompt geaendert")
    }
}
