package de.frank.entropyreducer.data.remote.drive

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
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
 * Public State des Sync-Vorgangs — beobachtet von der UI für Status-Anzeigen.
 */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Pending : SyncStatus     // Änderung erfasst, Debounce laeuft
    data object Running : SyncStatus     // Upload ist gerade aktiv
    data class Synced(val atEpochMs: Long) : SyncStatus
    data class Failed(val reason: String) : SyncStatus
}

/**
 * Verhindert Job-Stacking: jede Änderung am Datenbestand führt zu einem
 * Sync-Trigger. Ein laufender Sync wird NICHT abgebrochen, ein bereits
 * geplanter (Pending) Sync wird hingegen mit dem neueren Trigger zusammengelegt.
 *
 * Konzepte:
 *  - Ein Mutex schuetzt den Upload selbst — nie zwei gleichzeitig.
 *  - Ein Debounce-Window von 1500ms sammelt schnelle, aufeinanderfolgende
 *    Aenderungen (Stop-Recording + KI-Antwort + Status-Update) zu einem
 *    einzigen Upload zusammen.
 *  - Nach erfolgreichem Upload prüfen wir, ob waehrenddessen eine neue
 *    Änderung kam. Wenn ja: noch ein Upload, weil der erste den neuen
 *    Stand noch nicht hatte.
 *
 * Frank-Wunsch 2026-05-09 (Abend): BackupPayload v2 enthaelt jetzt nicht nur
 * Aufgaben sondern AUCH Insights, Memories, Hypothesen, Forscher-Sessions und
 * Forscher-Messages. Vollstaendige Wiederherstellbarkeit nach Reinstall.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val secrets: EncryptedSecretsStore,
    private val backupManager: DriveBackupManager,
    private val entryRepoLazy: Lazy<EntryRepository>,
    private val insightDaoLazy: Lazy<InsightDao>,
    private val memoryDaoLazy: Lazy<MemoryDao>,
    private val hypothesisDaoLazy: Lazy<HypothesisDao>,
    private val scientistSessionDaoLazy: Lazy<ScientistSessionDao>,
    private val scientistMessageDaoLazy: Lazy<ScientistMessageDao>,
    private val hypothesisMessageDaoLazy: Lazy<HypothesisMessageDao>,
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
            // Frank-Wunsch 2026-05-09 (Abend): Vollstaendiger Snapshot ALLER
            // wichtigen Wissens-Daten. Aufgaben (entries) plus alles was in
            // ScientistDatabase persistent ist — Insights, Memories, Hypothesen,
            // Forscher-Sessions, Forscher- und Hypothese-Messages.
            val entries = entryRepoLazy.get().getActive().first().map { it.toBackup() }
            val insights = insightDaoLazy.get().getByConfidenceDesc().first().map { it.toBackup() }
            val memories = memoryDaoLazy.get().getAll().first().map { it.toBackup() }
            val hypotheses = hypothesisDaoLazy.get().getAll().first().map { it.toBackup() }
            val sessions = scientistSessionDaoLazy.get().getAll().first().map { it.toBackup() }
            val sessMessages = scientistMessageDaoLazy.get().getAll().map { it.toBackup() }
            val hypMessages = hypothesisMessageDaoLazy.get().getAllOnce().map { it.toBackup() }

            val payload = BackupPayload(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                entries = entries,
                insights = insights,
                memories = memories,
                hypotheses = hypotheses,
                scientistSessions = sessions,
                scientistMessages = sessMessages,
                hypothesisMessages = hypMessages,
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
