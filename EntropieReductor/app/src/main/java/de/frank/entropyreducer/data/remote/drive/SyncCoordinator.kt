package de.frank.entropyreducer.data.remote.drive

import dagger.Lazy
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public State des Sync-Vorgangs — beobachtet von der UI fuer Status-Anzeigen.
 */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Pending : SyncStatus     // Aenderung erfasst, Debounce laeuft
    data object Running : SyncStatus     // Upload ist gerade aktiv
    data class Synced(val atEpochMs: Long) : SyncStatus
    data class Failed(val reason: String) : SyncStatus
}

/**
 * Verhindert Job-Stacking: jede Aenderung am Datenbestand fuehrt zu einem
 * Sync-Trigger. Ein laufender Sync wird NICHT abgebrochen, ein bereits
 * geplanter (Pending) Sync wird hingegen mit dem neueren Trigger zusammengelegt.
 *
 * Konzepte:
 *  - Ein Mutex schuetzt den Upload selbst — nie zwei gleichzeitig.
 *  - Ein Debounce-Window von 1500ms sammelt schnelle, aufeinanderfolgende
 *    Aenderungen (Stop-Recording + KI-Antwort + Status-Update) zu einem
 *    einzigen Upload zusammen.
 *  - Nach erfolgreichem Upload pruefen wir, ob waehrenddessen eine neue
 *    Aenderung kam. Wenn ja: noch ein Upload, weil der erste den neuen
 *    Stand noch nicht hatte.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val secrets: EncryptedSecretsStore,
    private val backupManager: DriveBackupManager,
    private val entryRepoLazy: Lazy<EntryRepository>,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadMutex = Mutex()
    private var pendingJob: Job? = null
    private var dirtyDuringUpload = false

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Wird vom Repository nach jeder Mutation aufgerufen. Wenn Backup deaktiviert
     * oder kein Account verbunden, wird der Trigger ignoriert — ohne Fehler.
     */
    fun requestSync() {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return

        // Wenn gerade ein Upload laeuft: nur Flag setzen, kein neuer Job.
        if (uploadMutex.isLocked) {
            dirtyDuringUpload = true
            return
        }

        // Vorhandener Pending-Job wird durch neuen ersetzt — Coalescing.
        pendingJob?.cancel()
        _status.value = SyncStatus.Pending
        pendingJob = scope.launch {
            delay(DEBOUNCE_MS)
            performUpload()
        }
    }

    /** Sofortiger Upload ohne Debounce (z. B. beim Tippen auf "Jetzt sichern"). */
    fun requestImmediate() {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return
        pendingJob?.cancel()
        scope.launch { performUpload() }
    }

    private suspend fun performUpload() {
        uploadMutex.withLock {
            _status.value = SyncStatus.Running
            dirtyDuringUpload = false
            val entries = entryRepoLazy.get().getActive().first().map { it.toBackup() }
            val payload = BackupPayload(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                entries = entries,
            )
            val text = json.encodeToString(BackupPayload.serializer(), payload)
            backupManager.upload(text)
                .onSuccess { _status.value = SyncStatus.Synced(System.currentTimeMillis()) }
                .onFailure { ex ->
                    _status.value = SyncStatus.Failed(ex.message ?: "Backup fehlgeschlagen")
                }
        }
        // Wenn waehrend des Uploads neue Aenderungen kamen: noch einen Run.
        if (dirtyDuringUpload) {
            requestSync()
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L
    }
}
