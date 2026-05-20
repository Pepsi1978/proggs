package de.frank.entropyreducer.data.remote.drive

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.repository.BiomarkerCardOrderRepository
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import javax.inject.Singleton
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

/** Public State des Sync-Vorgangs — beobachtet von der UI für Status-Anzeigen. */
sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Pending : SyncStatus // Änderung erfasst, Debounce laeuft

    data object Running : SyncStatus // Upload ist gerade aktiv

    data class Synced(val atEpochMs: Long) : SyncStatus

    data class Failed(val reason: String) : SyncStatus
}

/**
 * Verhindert Job-Stacking: jede Änderung am Datenbestand führt zu einem Sync-Trigger. Ein laufender
 * Sync wird NICHT abgebrochen, ein bereits geplanter (Pending) Sync wird hingegen mit dem neueren
 * Trigger zusammengelegt.
 *
 * Konzepte:
 * - Ein Mutex schuetzt den Upload selbst — nie zwei gleichzeitig.
 * - Ein Debounce-Window von 1500ms sammelt schnelle, aufeinanderfolgende Aenderungen
 *   (Stop-Recording + KI-Antwort + Status-Update) zu einem einzigen Upload zusammen.
 * - Nach erfolgreichem Upload prüfen wir, ob waehrenddessen eine neue Änderung kam. Wenn ja: noch
 *   ein Upload, weil der erste den neuen Stand noch nicht hatte.
 *
 * Frank-Wunsch 2026-05-09 (Abend): BackupPayload v2 enthaelt jetzt nicht nur Aufgaben sondern AUCH
 * Insights, Memories, Hypothesen, Forscher-Sessions und Forscher-Messages. Vollstaendige
 * Wiederherstellbarkeit nach Reinstall.
 */
@Singleton
class SyncCoordinator
@Inject
constructor(
    private val secrets: EncryptedSecretsStore,
    private val backupManager: DriveBackupManager,
    private val entryRepoLazy: Lazy<EntryRepository>,
    private val insightDaoLazy: Lazy<InsightDao>,
    private val memoryDaoLazy: Lazy<MemoryDao>,
    private val hypothesisDaoLazy: Lazy<HypothesisDao>,
    private val scientistSessionDaoLazy: Lazy<ScientistSessionDao>,
    private val scientistMessageDaoLazy: Lazy<ScientistMessageDao>,
    private val hypothesisMessageDaoLazy: Lazy<HypothesisMessageDao>,
    private val cardOrderRepoLazy: Lazy<BiomarkerCardOrderRepository>,
    private val healthConnectValueDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.HealthConnectValueDao>,
    private val amazfitWorkoutDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao>,
    // Frank-Wunsch 2026-05-19: Whoop + Oura ins separate Health-Backup
    // (entropy_reducer_health_v1.json). Whoop liefert nur ~90 Tage rueckwirkend,
    // Oura ~6 Monate — ohne Backup gehen aeltere Recovery- und Schlaf-Daten
    // nach Reinstall verloren.
    private val biomarkerSnapshotDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao>,
    private val whoopWorkoutDaoLazy: Lazy<de.frank.entropyreducer.data.local.dao.WhoopWorkoutDao>,
    private val ouraReadinessDaoLazy: Lazy<de.frank.entropyreducer.data.local.dao.OuraReadinessDao>,
    private val ouraDailySleepDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.OuraDailySleepDao>,
    private val ouraActivityDaoLazy: Lazy<de.frank.entropyreducer.data.local.dao.OuraActivityDao>,
    private val ouraResilienceDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.OuraResilienceDao>,
    private val ouraSleepDetailDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.OuraSleepDetailDao>,
    private val ouraPersonalInfoDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.OuraPersonalInfoDao>,
    // Frank-Wunsch 2026-05-20: Profil + Tagebuch + Entropie-Followups ins Backup.
    private val appSettingsLazy: Lazy<de.frank.entropyreducer.data.settings.AppSettings>,
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context,
    private val entropyEntryFollowupDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao>,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadMutex = Mutex()
    private var pendingJob: Job? = null
    private var dirtyDuringUpload = false

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /**
     * Wird vom Repository nach jeder Mutation aufgerufen. Wenn Backup deaktiviert oder kein Account
     * verbunden, wird der Trigger ignoriert — ohne Fehler.
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
            // Frank-Wunsch 2026-05-19: ALLE Eintraege sichern, auch archivierte
            // (Bereich "Entropie"). getActive() filtert ARCHIVIERT raus — dadurch
            // gingen archivierte Eintraege bei Reinstall verloren.
            val entries = entryRepoLazy.get().getAllForBackup().map { it.toBackup() }
            val insights = insightDaoLazy.get().getByConfidenceDesc().first().map { it.toBackup() }
            val memories = memoryDaoLazy.get().getAll().first().map { it.toBackup() }
            val hypotheses = hypothesisDaoLazy.get().getAll().first().map { it.toBackup() }
            val sessions = scientistSessionDaoLazy.get().getAll().first().map { it.toBackup() }
            val sessMessages = scientistMessageDaoLazy.get().getAll().map { it.toBackup() }
            val hypMessages = hypothesisMessageDaoLazy.get().getAllOnce().map { it.toBackup() }
            // Frank-Wunsch 2026-05-10: Drag&Drop-Reihenfolge der Biomarker-Karten
            // mit ins Backup. Wir nehmen die ROHE gespeicherte Liste — nicht die
            // gemergte — damit nur User-Anpassungen gesichert werden, nicht die
            // automatisch angehaengte DEFAULT_ORDER. Leere Liste = User hat nichts
            // verschoben, beim Restore passiert dann auch nichts.
            val cardOrder = cardOrderRepoLazy.get().rawSavedOrder.first()
            // Frank-Wunsch 2026-05-10 abend: Cross-Device-HC-Cache mit ins Backup.
            val hcValues =
                healthConnectValueDaoLazy.get().getAll().map {
                    BackupHealthConnectValue(
                        metric = it.metric,
                        timestampMs = it.timestampMs,
                        value = it.value,
                    )
                }
            // Frank-Wunsch 2026-05-19: Workouts kommen ab jetzt aus der SEPARATEN
            // Datei `entropy_reducer_workouts_v1.json` — Haupt-Backup haelt die Liste
            // nur noch leer fuer Backwards-Compat (alte v5-Restores funktionieren).
            // Slim-Workouts (ohne Streams) sind im Hauptbackup nicht mehr noetig.
            // Frank-Wunsch 2026-05-20: Profil-Text, Tagebuch-Eintraege und
            // Aufgaben-Nachtraege gehoeren auch ins Backup. Ohne diese drei waren
            // sie bei Reinstall verloren — obwohl sie zur persoenlichen Wissens-
            // domaene gehoeren (Profil-Text fliesst in jeden KI-Aufruf, Tagebuch
            // ist Frank's Entropie-Tagebuch, Followups sind Detailkontext fuer
            // Aufgaben).
            val profileText = appSettingsLazy.get().profileTextFlow.first()
            val tagebuchList =
                de.frank.entropyreducer.presentation.tagebuch
                    .tagebuchEntriesFlow(appContext)
                    .first()
            val tagebuchBackups = tagebuchList.map { e ->
                BackupTagebuchEntry(
                    id = e.id,
                    timestampMs = e.timestampMs,
                    title = e.title,
                    text = e.text,
                    summary = e.summary,
                    followups =
                        e.followups.map { f ->
                            BackupTagebuchFollowup(
                                id = f.id,
                                createdAtMs = f.createdAtMs,
                                text = f.text,
                            )
                        },
                )
            }
            val entropyFollowupBackups =
                entropyEntryFollowupDaoLazy.get().getAllForBackup().map { it.toBackup() }
            val thesenList =
                de.frank.entropyreducer.presentation.thesen.thesenEntriesFlow(appContext).first()
            val thesenBackups = thesenList.map { e ->
                BackupThesenEntry(
                    id = e.id,
                    timestampMs = e.timestampMs,
                    title = e.title,
                    text = e.text,
                    summary = e.summary,
                    followups =
                        e.followups.map { f ->
                            BackupThesenFollowup(
                                id = f.id,
                                createdAtMs = f.createdAtMs,
                                text = f.text,
                            )
                        },
                )
            }

            val payload =
                BackupPayload(
                    version = 7,
                    exportedAt = System.currentTimeMillis(),
                    entries = entries,
                    insights = insights,
                    memories = memories,
                    hypotheses = hypotheses,
                    scientistSessions = sessions,
                    scientistMessages = sessMessages,
                    hypothesisMessages = hypMessages,
                    biomarkerCardOrder = cardOrder,
                    healthConnectValues = hcValues,
                    amazfitWorkouts = emptyList(),
                    profileText = profileText,
                    tagebuchEntries = tagebuchBackups,
                    entropyEntryFollowups = entropyFollowupBackups,
                    thesenEntries = thesenBackups,
                )
            // Frank-Bugfix 2026-05-16 (Iteration 2): Defense-in-Depth gegen OOM
            // beim Serialize. Falls jemals ein Backup-Payload zu gross wird
            // (z.B. nach einem Bulk-Import) fangen wir den OOM ab und melden
            // SyncStatus.Failed — die App crasht NICHT mehr in der Endlos-
            // schleife wenn ein einziger Upload-Versuch fehlschlaegt.
            val mainOk =
                try {
                    val text = json.encodeToString(BackupPayload.serializer(), payload)
                    val result = backupManager.upload(text)
                    result.isSuccess
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    _status.value = SyncStatus.Failed("Backup zu gross fuer Upload (OOM)")
                    false
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    _status.value = SyncStatus.Failed(t.message ?: "Backup fehlgeschlagen")
                    false
                }

            // Frank-Wunsch 2026-05-19: Sport-Trainings in EIGENER Drive-Datei
            // hochladen — inkl. ALLER Streams (GPS, Puls, Pace, Splits). Damit
            // ist der Trainingsverlauf nach `adb uninstall` automatisch wieder da.
            // Nach 2-Jahres-Retention (~104 Workouts) liegt das Volumen sicher
            // unter 15 MB.
            if (mainOk) {
                try {
                    val workoutEntities = amazfitWorkoutDaoLazy.get().observeAll().first()
                    val workoutBackups = workoutEntities.map { it.toBackup() }
                    val workoutsPayload =
                        WorkoutsBackupPayload(
                            version = 1,
                            exportedAt = System.currentTimeMillis(),
                            workouts = workoutBackups,
                        )
                    val workoutsText =
                        json.encodeToString(WorkoutsBackupPayload.serializer(), workoutsPayload)
                    backupManager
                        .uploadWorkouts(workoutsText)
                        .onSuccess { _status.value = SyncStatus.Synced(System.currentTimeMillis()) }
                        .onFailure { ex ->
                            _status.value =
                                SyncStatus.Failed(
                                    "Workouts-Backup fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}"
                                )
                        }
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    _status.value = SyncStatus.Failed("Workouts-Backup zu gross (OOM)")
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    _status.value =
                        SyncStatus.Failed(
                            "Workouts-Backup fehlgeschlagen: ${t.message ?: t.javaClass.simpleName}"
                        )
                }
            }

            // Frank-Wunsch 2026-05-19 (Erweiterung): Whoop + Oura ins EIGENE Drive-
            // Backup-File `entropy_reducer_health_v1.json` hochladen. Volumen klein
            // (keine GPS-Streams) — pro Tag 2 KB Whoop-Recovery + 4 KB Oura.
            // 2 Jahre = ca. 4 MB Total.
            if (mainOk) {
                try {
                    val whoopSnapshots =
                        biomarkerSnapshotDaoLazy.get().getAll().first().map { it.toBackup() }
                    val whoopWorkouts =
                        whoopWorkoutDaoLazy.get().observeAll().first().map { it.toBackup() }
                    val ouraReadiness =
                        ouraReadinessDaoLazy.get().getAll().first().map { it.toBackup() }
                    val ouraDailySleep =
                        ouraDailySleepDaoLazy.get().getAll().first().map { it.toBackup() }
                    val ouraActivity =
                        ouraActivityDaoLazy.get().getAll().first().map { it.toBackup() }
                    val ouraResilience =
                        ouraResilienceDaoLazy.get().getAll().first().map { it.toBackup() }
                    val ouraSleepDetail =
                        ouraSleepDetailDaoLazy.get().getAll().first().map { it.toBackup() }
                    val ouraPersonalInfo = ouraPersonalInfoDaoLazy.get().get()?.toBackup()
                    val healthPayload =
                        HealthBackupPayload(
                            version = 1,
                            exportedAt = System.currentTimeMillis(),
                            whoopSnapshots = whoopSnapshots,
                            whoopWorkouts = whoopWorkouts,
                            ouraReadiness = ouraReadiness,
                            ouraDailySleep = ouraDailySleep,
                            ouraActivity = ouraActivity,
                            ouraResilience = ouraResilience,
                            ouraSleepDetail = ouraSleepDetail,
                            ouraPersonalInfo = ouraPersonalInfo,
                        )
                    val healthText =
                        json.encodeToString(HealthBackupPayload.serializer(), healthPayload)
                    backupManager
                        .uploadHealth(healthText)
                        .onSuccess { _status.value = SyncStatus.Synced(System.currentTimeMillis()) }
                        .onFailure { ex ->
                            _status.value =
                                SyncStatus.Failed(
                                    "Health-Backup fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}"
                                )
                        }
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    _status.value = SyncStatus.Failed("Health-Backup zu gross (OOM)")
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    _status.value =
                        SyncStatus.Failed(
                            "Health-Backup fehlgeschlagen: ${t.message ?: t.javaClass.simpleName}"
                        )
                }
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
