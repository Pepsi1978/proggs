package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.remote.drive.BackupPayload
import de.frank.entropyreducer.data.remote.drive.DriveRestoreManager
import de.frank.entropyreducer.data.remote.drive.DriveSession
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.drive.toEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * High-Level UseCase für manuelles Backup + Restore. Die "automatische"
 * Variante (jede Änderung triggert ein Backup) laeuft über den
 * [SyncCoordinator]; dieser UseCase ist für explizite Aktionen aus den
 * Einstellungen.
 */
class SyncEntriesUseCase @Inject constructor(
    private val coordinator: SyncCoordinator,
    private val restoreManager: DriveRestoreManager,
    private val driveSession: DriveSession,
    private val entryRepo: EntryRepository,
    private val secrets: EncryptedSecretsStore,
    private val json: Json,
) {

    /** Manuell ein Sofort-Backup ausloesen. */
    fun backupNow() = coordinator.requestImmediate()

    /**
     * Vom Drive heruntergeladene Eintraege mit dem lokalen Bestand mergen.
     *
     * Strategie: Last-Write-Wins per `updatedAt`. Wenn ein Eintrag lokal nicht
     * existiert, wird er eingefuegt. Wenn er existiert und das Drive-Backup
     * juenger ist, wird er ueberschrieben.
     *
     * Returns: Anzahl neu eingefuegter + ueberschriebener Eintraege.
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
        for (backupEntry in payload.entries) {
            val incoming = backupEntry.toEntity()
            val existing = entryRepo.get(incoming.id)
            when {
                existing == null -> {
                    entryRepo.upsert(incoming)
                    inserted++
                }
                incoming.updatedAt > existing.updatedAt -> {
                    entryRepo.upsert(incoming)
                    updated++
                }
                else -> Unit
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
