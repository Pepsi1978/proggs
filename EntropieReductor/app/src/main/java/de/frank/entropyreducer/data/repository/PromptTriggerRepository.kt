package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.PromptTriggerDao
import de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
import de.frank.entropyreducer.domain.model.TriggerType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository fuer Auto-Trigger-Konfigurationen (Stufe 3 — WorkManager-basierte
 * Hintergrund-Ausfuehrung).
 *
 * Konsumenten:
 *
 * 1. **TriggerScheduler (WorkManager-Worker, periodisch alle 15 Min):**
 *    - `getDueCronTriggers(now)` — welche zeitgesteuerten Trigger sind faellig?
 *    - `markFired(triggerId, nextScheduledAt)` — nach Feuer den naechsten Termin setzen
 *
 * 2. **EventDispatcher (bei App-Events wie "neuer Entropie-Eintrag"):**
 *    - `getAllEventTriggers()` — alle Event-Trigger zur Pruefung
 *
 * 3. **Chain-Auto-Run nach Erfolg eines Prompts:**
 *    - `getChainTriggersAfter(promptId)` — wer kettet sich an?
 *
 * 4. **Trigger-Editor-UI:**
 *    - `getForPrompt(promptId)` — bestehende Trigger eines Prompts anzeigen
 *    - `upsert / delete / toggleActive`
 */
@Singleton
class PromptTriggerRepository
@Inject
constructor(private val dao: PromptTriggerDao) {

    fun getForPrompt(promptId: String): Flow<List<PromptTriggerEntity>> = dao.getByPrompt(promptId)

    suspend fun getById(id: String): PromptTriggerEntity? = dao.getById(id)

    /** Alle aktiven Trigger ueber alle Prompts (z.B. fuer Trigger-Uebersichts-Screen). */
    suspend fun getAllActive(): List<PromptTriggerEntity> = dao.getAllActive()

    /** Vom TriggerScheduler genutzt. */
    suspend fun getDueCronTriggers(nowMillis: Long): List<PromptTriggerEntity> =
        dao.getDueCronTriggers(nowMillis)

    /** Vom EventDispatcher genutzt. */
    suspend fun getAllEventTriggers(): List<PromptTriggerEntity> = dao.getAllEventTriggers()

    /** Nach Erfolg eines Prompts: wer kettet sich an? */
    suspend fun getChainTriggersAfter(sourcePromptId: String): List<PromptTriggerEntity> =
        dao.getChainTriggersAfter(sourcePromptId)

    suspend fun getActiveByType(type: TriggerType): List<PromptTriggerEntity> =
        dao.getActiveByType(type)

    suspend fun upsert(trigger: PromptTriggerEntity) = dao.upsert(trigger)

    suspend fun update(trigger: PromptTriggerEntity) = dao.update(trigger)

    suspend fun delete(trigger: PromptTriggerEntity) = dao.delete(trigger)

    suspend fun deleteAllForPrompt(promptId: String) = dao.deleteByPrompt(promptId)

    /** Nach Trigger-Feuer: Zeitpunkt aktualisieren und ggf. nextScheduledAt setzen. */
    suspend fun markFired(triggerId: String, firedAt: Long, nextScheduledAt: Long?) {
        val trigger = dao.getById(triggerId) ?: return
        dao.update(
            trigger.copy(
                lastRunAt = firedAt,
                nextScheduledAt = nextScheduledAt,
            )
        )
    }

    suspend fun setActive(triggerId: String, isActive: Boolean) {
        val trigger = dao.getById(triggerId) ?: return
        dao.update(trigger.copy(isActive = isActive))
    }
}
