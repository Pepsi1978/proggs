package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.PromptToolPermissionDao
import de.frank.entropyreducer.data.local.entities.PromptToolPermissionEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository fuer Tool-Freischaltungen pro Prompt. Wird vom PermissionGate gelesen
 * (vor jedem Tool-Aufruf) und vom Prompt-Editor-UI geschrieben (Frank schaltet ein
 * Write-Tool fuer einen bestimmten Prompt frei).
 *
 * Read-Tools brauchen keinen Eintrag — sie sind per Default fuer alle Prompts
 * erlaubt (kategorie-uebergreifend, Frank-Entscheidung 2026-05-21). Erst Write-Tools
 * brauchen ein explizites granted=true.
 *
 * trustMode=true bedeutet: das Tool darf ohne UI-Confirm-Dialog ausgefuehrt werden
 * (z.B. fuer Auto-Trigger im Hintergrund, wo kein Dialog gezeigt werden kann).
 */
@Singleton
class PromptToolPermissionRepository
@Inject
constructor(
    private val dao: PromptToolPermissionDao,
    // Frank-Bugfix 2026-05-22: Jede Mutation triggert Drive-Backup-Sync.
    private val syncCoordinator: Lazy<SyncCoordinator>,
) {

    fun getForPrompt(promptId: String): Flow<List<PromptToolPermissionEntity>> =
        dao.getByPrompt(promptId)

    suspend fun getForPromptSnapshot(promptId: String): List<PromptToolPermissionEntity> =
        dao.getByPromptSnapshot(promptId)

    suspend fun getOne(promptId: String, toolName: String): PromptToolPermissionEntity? =
        dao.getOne(promptId, toolName)

    /**
     * Vom PermissionGate genutzt: "Darf dieser Prompt dieses Write-Tool aufrufen?"
     * Liefert false wenn kein Eintrag existiert (Default-Deny fuer Writes).
     */
    suspend fun isWriteToolGranted(promptId: String, toolName: String): Boolean =
        dao.getOne(promptId, toolName)?.granted == true

    /**
     * Vom ConfirmationGate genutzt: "Soll der Confirm-Dialog uebersprungen werden?"
     * Nur true wenn ausdrueklich granted UND trustMode aktiviert.
     */
    suspend fun isInTrustMode(promptId: String, toolName: String): Boolean {
        val perm = dao.getOne(promptId, toolName) ?: return false
        return perm.granted && perm.trustMode
    }

    suspend fun upsert(permission: PromptToolPermissionEntity) {
        dao.upsert(permission)
        syncCoordinator.get().requestSync("Agentic-AI: Tool-Permission geaendert")
    }

    suspend fun update(permission: PromptToolPermissionEntity) {
        dao.update(permission)
        syncCoordinator.get().requestSync("Agentic-AI: Tool-Permission geaendert")
    }

    suspend fun delete(permission: PromptToolPermissionEntity) {
        dao.delete(permission)
        syncCoordinator.get().requestSync("Agentic-AI: Tool-Permission geaendert")
    }

    suspend fun deleteAllForPrompt(promptId: String) {
        dao.deleteByPrompt(promptId)
        syncCoordinator.get().requestSync("Agentic-AI: Tool-Permission geaendert")
    }

    /** Convenience: alle Permissions eines Prompts auf einmal setzen (vom Permission-Editor-UI). */
    suspend fun setAll(promptId: String, permissions: List<PromptToolPermissionEntity>) {
        permissions.forEach { dao.upsert(it) }
        // Nur EIN Sync-Trigger fuer die ganze Batch (debounce coalesced ohnehin).
        syncCoordinator.get().requestSync("Agentic-AI: Tool-Permission geaendert")
    }
}
