package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.remote.drive.BackupPayload
import de.frank.entropyreducer.data.remote.drive.DriveRestoreManager
import de.frank.entropyreducer.data.remote.drive.DriveSession
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.drive.toEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * High-Level UseCase für manuelles Backup + Restore. Die "automatische"
 * Variante (jede Änderung triggert ein Backup) laeuft über den
 * [SyncCoordinator]; dieser UseCase ist für explizite Aktionen aus den
 * Einstellungen.
 *
 * Frank-Wunsch 2026-05-09 (Abend): Restore zieht jetzt vollstaendig — nicht nur
 * Aufgaben (v1) sondern auch Insights, Memories, Hypothesen, Forscher-Sessions
 * und Forscher-/Hypothese-Messages (v2). Bei v1-Backups bleiben die neuen
 * Listen leer und nur Aufgaben kommen zurueck — keine Fehler, abwaerts-kompatibel.
 */
class SyncEntriesUseCase @Inject constructor(
    private val coordinator: SyncCoordinator,
    private val restoreManager: DriveRestoreManager,
    private val driveSession: DriveSession,
    private val entryRepo: EntryRepository,
    private val insightDao: InsightDao,
    private val memoryDao: MemoryDao,
    private val hypothesisDao: HypothesisDao,
    private val scientistSessionDao: ScientistSessionDao,
    private val scientistMessageDao: ScientistMessageDao,
    private val hypothesisMessageDao: HypothesisMessageDao,
    private val secrets: EncryptedSecretsStore,
    private val json: Json,
) {

    /** Manuell ein Sofort-Backup ausloesen. */
    fun backupNow() = coordinator.requestImmediate()

    /**
     * Vom Drive heruntergeladene Daten mit dem lokalen Bestand mergen.
     *
     * Strategien pro Entity-Typ:
     *  - Aufgaben + Insights + Memories: Last-Write-Wins per `updatedAt`.
     *    Wenn lokal nicht vorhanden, einfuegen. Wenn lokal vorhanden und
     *    Backup juenger, ueberschreiben.
     *  - Hypothesen + Sessions + Messages: Existenz-basiert. Wenn lokal nicht
     *    vorhanden, einfuegen. Wenn lokal vorhanden, NICHT ueberschreiben
     *    (Messages aendern sich nach Erstellung nicht; Hypothesen und Sessions
     *    haben kein updatedAt-Feld — konservative Strategie um lokale
     *    Aenderungen nicht zu zerstoeren).
     *
     * Returns: aggregierte Counts ueber alle Entity-Typen.
     */
    suspend fun restoreFromDrive(): Result<RestoreOutcome> {
        val downloadResult = restoreManager.fetchLatest()
        val raw = downloadResult.getOrElse { return Result.failure(it) }
            ?: return Result.success(RestoreOutcome.NoBackup)

        val payload = runCatching {
            json.decodeFromString(BackupPayload.serializer(), raw)
        }.getOrElse { return Result.failure(it) }

        var inserted = 0
        var updated = 0

        // --- Aufgaben (immer dabei, auch bei v1-Backups) ---
        for (backupEntry in payload.entries) {
            val incoming = backupEntry.toEntity()
            val existing = entryRepo.get(incoming.id)
            when {
                existing == null -> { entryRepo.upsert(incoming); inserted++ }
                incoming.updatedAt > existing.updatedAt -> { entryRepo.upsert(incoming); updated++ }
                else -> Unit
            }
        }

        // --- Insights (v2+, bei v1 leere Liste) ---
        for (b in payload.insights) {
            val incoming = b.toEntity()
            val all = insightDao.getByConfidenceDesc().first()
            val existing = all.firstOrNull { it.id == incoming.id }
            when {
                existing == null -> { insightDao.upsert(incoming); inserted++ }
                incoming.updatedAt > existing.updatedAt -> { insightDao.upsert(incoming); updated++ }
                else -> Unit
            }
        }

        // --- Memories (v2+) ---
        for (b in payload.memories) {
            val incoming = b.toEntity()
            val all = memoryDao.getAll().first()
            val existing = all.firstOrNull { it.id == incoming.id }
            when {
                existing == null -> { memoryDao.upsert(incoming); inserted++ }
                incoming.updatedAt > existing.updatedAt -> { memoryDao.upsert(incoming); updated++ }
                else -> Unit
            }
        }

        // --- Hypothesen (v2+, Existenz-Strategie) ---
        for (b in payload.hypotheses) {
            val incoming = b.toEntity()
            val existing = hypothesisDao.getById(incoming.id)
            if (existing == null) {
                hypothesisDao.upsert(incoming)
                inserted++
            }
        }

        // --- Forscher-Sessions (v2+, Existenz-Strategie) ---
        for (b in payload.scientistSessions) {
            val incoming = b.toEntity()
            val existing = scientistSessionDao.getById(incoming.id)
            if (existing == null) {
                scientistSessionDao.upsert(incoming)
                inserted++
            }
        }

        // --- Forscher-Messages (v2+, Existenz-Strategie via getAll-Liste) ---
        if (payload.scientistMessages.isNotEmpty()) {
            val existingIds = scientistMessageDao.getAll().map { it.id }.toHashSet()
            for (b in payload.scientistMessages) {
                if (b.id !in existingIds) {
                    scientistMessageDao.insert(b.toEntity())
                    inserted++
                }
            }
        }

        // --- Hypothese-Messages (v2+) ---
        if (payload.hypothesisMessages.isNotEmpty()) {
            val existingIds = hypothesisMessageDao.getAllOnce().map { it.id }.toHashSet()
            for (b in payload.hypothesisMessages) {
                if (b.id !in existingIds) {
                    hypothesisMessageDao.insert(b.toEntity())
                    inserted++
                }
            }
        }

        driveSession.end()
        return Result.success(RestoreOutcome.Merged(inserted = inserted, updated = updated))
    }

    /** Hat das Drive-Konto bereits ein Backup? */
    suspend fun hasRemoteBackup(): Boolean = restoreManager.hasBackup()

    sealed interface RestoreOutcome {
        data object NoBackup : RestoreOutcome
        data class Merged(val inserted: Int, val updated: Int) : RestoreOutcome
    }
}
