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
    // M2-Schutz (Frank-Bugfix 2026-06-19): Read-before-write braucht den aktuellen
    // Remote-Stand. Lazy gegen mögliche Init-Zyklen (beide @Singleton).
    private val restoreManagerLazy: Lazy<DriveRestoreManager>,
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
    // Schema v11 (Frank-Bugfix 2026-05-22): Tagliche T-Rex-3-Werte (PAI,
    // BioCharge, Hauttemperatur, SpO2, Stress, Schritte, HRV, Schlaf) ins
    // Haupt-Backup. Bisher GAR NICHT gesichert — bei Reinstall verloren.
    private val amazfitDailyDaoLazy:
        Lazy<de.frank.entropyreducer.data.local.dao.AmazfitDailyDao>,
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
    private val promptRepoLazy: Lazy<de.frank.entropyreducer.data.repository.PromptRepository>,
    // Frank-Wunsch 2026-05-21: Agentic-AI Permissions + Triggers ins Backup
    private val promptToolPermissionRepoLazy:
        Lazy<de.frank.entropyreducer.data.repository.PromptToolPermissionRepository>,
    private val promptTriggerRepoLazy:
        Lazy<de.frank.entropyreducer.data.repository.PromptTriggerRepository>,
    // Sprint 2.8 (Frank-Wunsch 2026-05-22): Wiederkehrende Aufgaben-Vorlagen
    // ins Backup-Payload. Ohne diese Lazy waeren alle RRULE-Vorlagen nach
    // adb uninstall verloren.
    private val recurringTemplateRepoLazy:
        Lazy<de.frank.entropyreducer.data.repository.RecurringTemplateRepository>,
    // Frank-Wunsch 2026-06-19: Prioritaets-Gedaechtnis ins Backup-Payload.
    private val priorityMemoryRepoLazy:
        Lazy<de.frank.entropyreducer.data.repository.PriorityMemoryRepository>,
    private val json: Json,
    // Performance 2026-05-23 (Vorschlag 5): nur fuer Timing-Messungen (Payload-Aufbau, Upload).
    private val diagnostics: de.frank.entropyreducer.data.diagnostics.DiagnosticLogger,
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
     *
     * Diagnose-Sonde (Frank-Wunsch 2026-06-19): jeder Backup-Trigger loggt mit [reason], WER aus
     * welchem Grund ein Backup ausgeloest hat — so ist im Log der komplette Backup-Fluss
     * (Mutation -> Trigger -> Debounce -> Upload) sichtbar.
     */
    fun requestSync(reason: String = "unbekannt", debounceMs: Long = DEBOUNCE_MS) {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return

        // Wenn gerade ein Upload laeuft: nur Flag setzen, kein neuer Job. Der dirtyDuringUpload-
        // Mechanismus startet danach automatisch einen weiteren Upload — das deckt Franks
        // "falls nach den 5s noch weitere Werte kommen, danach nochmal sichern" ab.
        if (uploadMutex.isLocked) {
            dirtyDuringUpload = true
            diagnostics.info(
                de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                "Backup-Trigger '$reason' waehrend laufendem Upload -> als dirty markiert (Re-Run danach)",
            )
            return
        }

        // Vorhandener Pending-Job wird durch neuen ersetzt — Coalescing. debounceMs erlaubt einen
        // laengeren Sammel-Fenster (Frank-Wunsch 2026-06-19: Biomarker 5s, weil viele APIs
        // ueber mehrere Sekunden nachladen) statt des Standard-1500ms.
        pendingJob?.cancel()
        _status.value = SyncStatus.Pending
        diagnostics.info(
            de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
            "Backup-Trigger: $reason (Upload in ${debounceMs}ms)",
        )
        pendingJob = scope.launch {
            delay(debounceMs)
            performUpload()
        }
    }

    /** Sofortiger Upload ohne Debounce (z. B. beim Tippen auf "Jetzt sichern"). */
    fun requestImmediate(reason: String = "Sofort-Backup") {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return
        diagnostics.info(
            de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
            "Sofort-Backup-Trigger: $reason",
        )
        pendingJob?.cancel()
        scope.launch { performUpload() }
    }

    /**
     * Frank-Wunsch 2026-05-23: Blockierender Backup-Upload fuer den App-Start. Im Gegensatz zu
     * [requestSync] (debounced, fire-and-forget) WARTET diese Methode bis der komplette Upload
     * (Haupt + Workouts + Health) durch ist. So kann der Start-Ablauf sicherstellen, dass das
     * Drive-Backup KOMPLETT abgeschlossen ist, BEVOR die anderen API-Syncs (Whoop/Oura/
     * Kalender) starten. Ohne verbundenes Drive-Konto kehrt sie sofort zurueck.
     */
    suspend fun syncNowAndWait() {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return
        pendingJob?.cancel()
        performUpload()
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
            val buildStartMs = System.currentTimeMillis()
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
                    updatedAt = e.updatedAt,
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
            // Performance 2026-05-23 (Loop 2): Prompt-Liste EINMAL holen und sowohl fuer das
            // Prompt-Backup (savedPromptBackups) als auch fuer Permissions/Trigger weiter unten
            // wiederverwenden. Frueher wurde promptRepoLazy.get().getAll().first() ZWEIMAL
            // aufgerufen — zwei DB-Queries + zwei Flow-Collections fuer denselben Snapshot.
            // Verhaltensgleich, sogar konsistenter (beide Ableitungen aus genau einem Snapshot).
            val allPrompts = promptRepoLazy.get().getAll().first()
            val savedPromptBackups =
                allPrompts.map { p ->
                    BackupSavedPrompt(
                        id = p.id,
                        name = p.name,
                        content = p.content,
                        isActive = p.isActive,
                        createdAt = p.createdAt,
                        updatedAt = p.updatedAt,
                        category = p.category.name,
                        model = p.model,
                        tokenLimitPerDay = p.tokenLimitPerDay,
                        trustModeDefault = p.trustModeDefault,
                    )
                }
            val thesenList =
                de.frank.entropyreducer.presentation.thesen.thesenEntriesFlow(appContext).first()
            val thesenBackups = thesenList.map { e ->
                BackupThesenEntry(
                    id = e.id,
                    timestampMs = e.timestampMs,
                    updatedAt = e.updatedAt,
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

            // Agentic-AI v9: Permissions + Triggers ueber alle Prompts (allPrompts oben einmalig
            // geholt — wird hier wiederverwendet statt erneut aus der DB zu lesen).
            val permissionBackups = mutableListOf<BackupPromptToolPermission>()
            val triggerBackups = mutableListOf<BackupPromptTrigger>()
            for (p in allPrompts) {
                promptToolPermissionRepoLazy.get().getForPromptSnapshot(p.id).forEach { perm ->
                    permissionBackups.add(
                        BackupPromptToolPermission(
                            id = perm.id,
                            promptId = perm.promptId,
                            toolName = perm.toolName,
                            granted = perm.granted,
                            trustMode = perm.trustMode,
                        )
                    )
                }
                promptTriggerRepoLazy.get().getForPrompt(p.id).first().forEach { trig ->
                    triggerBackups.add(
                        BackupPromptTrigger(
                            id = trig.id,
                            promptId = trig.promptId,
                            triggerType = trig.triggerType.name,
                            cronExpression = trig.cronExpression,
                            eventCondition = trig.eventCondition,
                            chainAfterPromptId = trig.chainAfterPromptId,
                            isActive = trig.isActive,
                        )
                    )
                }
            }

            // Sprint 2.8: alle Vorlagen wiederkehrender Aufgaben ins Backup.
            val recurringTemplateBackups =
                recurringTemplateRepoLazy.get().getAllForBackup().map { it.toBackup() }

            // Frank-Wunsch 2026-06-19: Prioritaets-Gedaechtnis ins Backup.
            val priorityMemoryBackups =
                priorityMemoryRepoLazy.get().getAllForBackup().map { it.toBackup() }

            // Schema v11 (Frank-Bugfix 2026-05-22): Amazfit-Daily-Werte ins
            // Haupt-Backup. Volumen klein (~ein Eintrag pro Tag, max 730 Tage
            // Retention = ~365 KB). Idempotenter Restore via PrimaryKey=date.
            val amazfitDailyBackups =
                amazfitDailyDaoLazy.get().getAll().first().map { it.toBackup() }

            // Frank-Wunsch 2026-06-09: Mentalboard-Eintraege (Aufgaben-Reiter "Mental") ins
            // Haupt-Backup. v19 (2026-06-20): zusaetzlich die Herkunft (originId/originType/rootId)
            // mitsichern — direkt aus der mental_sentences-Tabelle statt ueber das herkunftslose
            // Mental-UI-Modell (Zukunftssicherung, analog zu den Gewohnheiten; aktuell entsteht Mental
            // nicht aus Ideen, aber falls doch, bleibt die Kette cross-device intakt).
            val mentalBackups =
                de.frank.entropyreducer.data.local.mentalSentenceDaoFrom(appContext).getAllForBackup().map {
                    BackupMental(
                        id = it.id,
                        text = it.text,
                        updatedAt = it.updatedAt,
                        originId = it.originId,
                        originType = it.originType,
                        rootId = it.rootId,
                    )
                }

            // Frank-Wunsch 2026-06-10: Ideen-Eintraege (Aufgaben-Reiter "Ideen", 1:1-Klon des
            // Tagebuch-Bereichs) ins Haupt-Backup. DataStore-basiert wie Tagebuch/Thesen/Mental.
            val ideenBackups =
                de.frank.entropyreducer.presentation.ideen.ideenEntriesFlow(appContext).first().map {
                    e ->
                    BackupIdeenEntry(
                        id = e.id,
                        timestampMs = e.timestampMs,
                        updatedAt = e.updatedAt,
                        title = e.title,
                        text = e.text,
                        summary = e.summary,
                        followups =
                            e.followups.map { f ->
                                BackupIdeenFollowup(
                                    id = f.id,
                                    createdAtMs = f.createdAtMs,
                                    text = f.text,
                                )
                            },
                    )
                }

            // Frank-Bugfix 2026-06-19: Gewohnheit-Eintraege (Aufgaben-Reiter "Gewohnheit") ins
            // Haupt-Backup. Bisher GAR NICHT gesichert — bei jedem Update/Reinstall verloren.
            // v19 (2026-06-20, Direktive #3 robust): direkt aus der habits-Tabelle MIT Herkunft
            // (originId/originType/rootId) sichern statt ueber das herkunftslose Mental-UI-Modell.
            // Sonst geht die Kette Idee -> Gewohnheits-Vorschlag -> Gewohnheit beim Drive-Backup
            // verloren und eine angenommene Gewohnheit wuerde auf einem 2. Geraet erneut vorgeschlagen
            // (gleicher Bug wie bei den Aufgaben, nur fuer Gewohnheiten).
            val gewohnheitBackups =
                de.frank.entropyreducer.data.local.habitDaoFrom(appContext).getAllForBackup().map {
                    BackupMental(
                        id = it.id,
                        text = it.text,
                        updatedAt = it.updatedAt,
                        originId = it.originId,
                        originType = it.originType,
                        rootId = it.rootId,
                    )
                }

            // Frank-Wunsch 2026-06-19 (Schema v15): offene KI-Vorschlaege mitsichern, damit sie bei
            // Update/Reinstall/Geraetewechsel nicht verloren gehen.
            val taskSuggestionBackups =
                de.frank.entropyreducer.data.taskSuggestionsForBackup(appContext).first()
            val gewohnheitSuggestionBackups =
                de.frank.entropyreducer.data.gewohnheitSuggestionsForBackup(appContext).first()
            // Sync-Etappe 1.2 (Frank-Wunsch 2026-06-19): Loesch-Protokoll mitsichern (Tombstones),
            // damit Loeschungen 1:1 auf andere Geraete propagieren.
            val tombstoneBackups = de.frank.entropyreducer.data.tombstonesForBackup(appContext)

            val localPayload =
                BackupPayload(
                    // v18 (2026-06-20): Vorschlags-Herkunft (originId/originType/rootId) im Backup —
                    // haelt den Ketten-Dedup nach Drive-Sync/Restore geraeteuebergreifend wirksam.
                    // v19 (2026-06-20): zusaetzlich die Herkunft der ANGENOMMENEN Aufgaben (entropy_entries)
                    // mitsichern -> die Kette bricht beim Annehmen eines Vorschlags auch cross-device nicht.
                    version = 19,
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
                    savedPrompts = savedPromptBackups,
                    promptToolPermissions = permissionBackups,
                    promptTriggers = triggerBackups,
                    recurringTemplates = recurringTemplateBackups,
                    priorityMemories = priorityMemoryBackups,
                    amazfitDaily = amazfitDailyBackups,
                    mentals = mentalBackups,
                    ideenEntries = ideenBackups,
                    gewohnheiten = gewohnheitBackups,
                    taskSuggestions = taskSuggestionBackups,
                    gewohnheitSuggestions = gewohnheitSuggestionBackups,
                    tombstones = tombstoneBackups,
                )
            // Performance 2026-05-23 (Vorschlag 5, vom Benutzer freigegeben): Misst wie lange der
            // Aufbau des Haupt-Payloads (alle DAO-Reads + Mapping oben) dauert, damit per
            // Diagnose-Log (adb pull) sichtbar wird ob er den Start spuerbar bremst. Reine
            // Messung — beeinflusst weder Inhalt noch Ablauf des Backups.
            val payloadBuildMs = System.currentTimeMillis() - buildStartMs
            diagnostics.info(
                de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                "Payload-Aufbau in ${payloadBuildMs}ms " +
                    "(Aufgaben=${entries.size}, Insights=${insights.size}, Memories=${memories.size}, " +
                    "Prompts=${allPrompts.size}, Amazfit-Daily=${amazfitDailyBackups.size}, " +
                    "Mental=${mentalBackups.size}, Ideen=${ideenBackups.size}, " +
                    "Gewohnheit=${gewohnheitBackups.size}, " +
                    "Aufgabenvorschlaege=${taskSuggestionBackups.size}, " +
                    "Gewohnheitsvorschlaege=${gewohnheitSuggestionBackups.size})",
            )

            // M2-Schutz (Read-before-write, Frank-Bugfix 2026-06-19 — Almanach M2 /
            // Best-Practices §4 Multi-Device): Bevor das Haupt-Backup hochgeladen wird, das
            // aktuelle Remote-Backup lesen und eine additive DataStore-Liste (Mental, Ideen,
            // Gewohnheit, Tagebuch, Thesen) NUR DANN aus dem Remote retten, wenn sie lokal
            // KOMPLETT LEER ist. Verhindert, dass ein frisch installiertes / nach Update
            // geleertes Geraet ein volles Remote-Backup leer ueberschreibt (M2-Datenverlust).
            // Bewusst kein voller Union (Loeschungen wuerden sonst zurueckkehren — siehe
            // mergeRemoteAdditiveLists). Safe-Degradation (Direktive #3): Remote nicht
            // lesbar/leer -> localPayload unveraendert (Verhalten wie vor dem Fix).
            val payload = mergeRemoteAdditiveLists(localPayload)

            // Frank-Bugfix 2026-05-16 (Iteration 2): Defense-in-Depth gegen OOM
            // beim Serialize. Falls jemals ein Backup-Payload zu gross wird
            // (z.B. nach einem Bulk-Import) fangen wir den OOM ab und melden
            // SyncStatus.Failed — die App crasht NICHT mehr in der Endlos-
            // schleife wenn ein einziger Upload-Versuch fehlschlaegt.
            // Performance 2026-05-23 (Vorschlag 2, vom Benutzer freigegeben): Das Haupt-Backup nur
            // hochladen wenn sich der INHALT geaendert hat — analog zum Workouts-Backup. Der
            // Fingerprint ist der inhaltsbasierte hashCode des Payloads, aber mit exportedAt = 0:
            // der Zeitstempel aendert sich sonst bei jedem Lauf und wuerde den Hash immer
            // verschieden machen. BackupPayload ist eine data class, daher deckt hashCode() alle
            // Inhalts-Listen ab.
            //
            // Safe-Degradation (Direktive #3): sollte der Hash aus irgendeinem Grund instabil sein
            // (ein verschachtelter Typ ohne content-hashCode, eine Query ohne ORDER BY), flackert
            // er nur -> es wird wie bisher IMMER hochgeladen. Es kann nie FAELSCHLICH geskippt
            // werden ohne dass beim naechsten echten Inhaltswechsel wieder hochgeladen wird
            // (requestSync laeuft bei jeder Mutation + bei jedem Start).
            val mainFingerprint = payload.copy(exportedAt = 0L).hashCode()
            val lastMainFingerprint = appSettingsLazy.get().mainBackupFingerprintFlow.first()
            val mainOk =
                if (mainFingerprint == lastMainFingerprint && lastMainFingerprint != 0) {
                    // Inhalt unveraendert -> teuren Serialize+Upload des Haupt-Backups ueberspringen.
                    // mainOk = true, damit Workouts-/Health-Backup (eigene Skip-Logik) weiterlaufen.
                    diagnostics.info(
                        de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                        "Haupt-Backup unveraendert -> Upload uebersprungen",
                    )
                    _status.value = SyncStatus.Synced(System.currentTimeMillis())
                    true
                } else {
                    try {
                        val uploadStartMs = System.currentTimeMillis()
                        val text = json.encodeToString(BackupPayload.serializer(), payload)
                        val result = backupManager.upload(text)
                        if (result.isSuccess) {
                            // Fingerprint erst NACH erfolgreichem Upload speichern, damit ein
                            // fehlgeschlagener Upload beim naechsten Mal erneut versucht wird.
                            appSettingsLazy.get().setMainBackupFingerprint(mainFingerprint)
                            diagnostics.info(
                                de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                                "Haupt-Upload OK in ${System.currentTimeMillis() - uploadStartMs}ms " +
                                    "(${text.length} Zeichen)",
                            )
                        }
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
                }

            // Frank-Wunsch 2026-05-19: Sport-Trainings in EIGENER Drive-Datei
            // hochladen — inkl. ALLER Streams (GPS, Puls, Pace, Splits). Damit
            // ist der Trainingsverlauf nach `adb uninstall` automatisch wieder da.
            // Nach 2-Jahres-Retention (~104 Workouts) liegt das Volumen sicher
            // unter 15 MB.
            if (mainOk) {
                try {
                    // Frank-Wunsch 2026-05-23: Das Workouts-Backup ist mit ~6 MB die mit Abstand
                    // groesste Datei und bremst den App-Start am staerksten. Es wird nur noch
                    // hochgeladen, wenn sich seit dem letzten Workouts-Backup wirklich etwas
                    // geaendert hat. Fingerprint = Hash ueber trackId + createdAt + manuelle Edits
                    // aller Workouts — aendert sich bei neuem, geloeschtem oder editiertem Training.
                    //
                    // Performance 2026-05-23 (Loop 1, Direktive #3): Fingerprint aus der schlanken
                    // 3-Spalten-Projektion getFingerprintRows() berechnen, NICHT mehr ueber
                    // observeAll() (das laedt alle Stream-Felder, ~6 MB bis ~130 MB, nur um sie
                    // nach dem Hash sofort zu verwerfen). Reihenfolge (startMs DESC) und die
                    // joinToString-Form sind identisch -> der Hash und damit die Skip-/Upload-
                    // Entscheidung bleiben bit-genau gleich. Die vollen Entities werden erst im
                    // else-Zweig geladen, also nur wenn tatsaechlich hochgeladen wird.
                    val fingerprint =
                        amazfitWorkoutDaoLazy
                            .get()
                            .getFingerprintRows()
                            .joinToString("|") {
                                "${it.trackId}:${it.createdAt}:${it.manualOverridesMs ?: 0L}"
                            }
                            .hashCode()
                    val lastFingerprint = appSettingsLazy.get().workoutsBackupFingerprintFlow.first()
                    if (fingerprint == lastFingerprint && lastFingerprint != 0) {
                        // Keine Aenderung an den Trainings — teuren 6-MB-Upload ueberspringen.
                        // Skip-Pfad laedt jetzt KEINE Stream-Daten mehr in den RAM.
                        _status.value = SyncStatus.Synced(System.currentTimeMillis())
                    } else {
                        // Nur bei echter Aenderung die vollen Entities (inkl. Streams) laden.
                        val workoutEntities = amazfitWorkoutDaoLazy.get().observeAll().first()
                        // M2-Schutz (Frank-Wunsch 2026-06-19, Almanach M2): Wenn lokal KEINE
                        // Trainings vorhanden sind (frisch installiert / vor dem ersten Trainings-
                        // Polar-Sync), aber das Remote-Workouts-Backup welche hat, NICHT mit leer
                        // ueberschreiben. Trainings sind zwar re-syncbar, aber manuelle Edits +
                        // Detail-Streams (GPS/Puls/Pace) sollen nicht durch ein leeres Geraet
                        // verloren gehen. Beim naechsten echten Sync laedt das Geraet die Trainings
                        // ohnehin neu und ueberschreibt dann mit vollem Stand.
                        if (workoutEntities.isEmpty() && remoteWorkoutsHasData()) {
                            diagnostics.info(
                                de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                                "M2-Schutz Workouts: lokal 0 Trainings, Remote-Backup voll -> Upload uebersprungen",
                            )
                            _status.value = SyncStatus.Synced(System.currentTimeMillis())
                        } else {
                            val workoutBackups = workoutEntities.map { it.toBackup() }
                            val workoutsPayload =
                                WorkoutsBackupPayload(
                                    version = 1,
                                    exportedAt = System.currentTimeMillis(),
                                    workouts = workoutBackups,
                                )
                            val workoutsText =
                                json.encodeToString(
                                    WorkoutsBackupPayload.serializer(),
                                    workoutsPayload,
                                )
                            backupManager
                                .uploadWorkouts(workoutsText)
                                .onSuccess {
                                    // Fingerprint erst NACH erfolgreichem Upload speichern, damit ein
                                    // fehlgeschlagener Upload beim naechsten Mal erneut versucht wird.
                                    appSettingsLazy.get().setWorkoutsBackupFingerprint(fingerprint)
                                    _status.value = SyncStatus.Synced(System.currentTimeMillis())
                                }
                                .onFailure { ex ->
                                    _status.value =
                                        SyncStatus.Failed(
                                            "Workouts-Backup fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}"
                                        )
                                }
                        }
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
                    // Performance 2026-05-24 (vom Benutzer freigegeben): Health-Backup analog zum
                    // Haupt-/Workouts-Backup nur hochladen wenn sich der Inhalt geaendert hat.
                    // Fingerprint = inhaltsbasierter hashCode OHNE exportedAt. Safe-Degradation:
                    // instabiler Hash -> flackert nur -> wird wie bisher immer hochgeladen.
                    val healthFingerprint = healthPayload.copy(exportedAt = 0L).hashCode()
                    val lastHealthFingerprint =
                        appSettingsLazy.get().healthBackupFingerprintFlow.first()
                    if (healthFingerprint == lastHealthFingerprint && lastHealthFingerprint != 0) {
                        diagnostics.info(
                            de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                            "Health-Backup unveraendert -> Upload uebersprungen",
                        )
                        _status.value = SyncStatus.Synced(System.currentTimeMillis())
                    } else {
                        val healthText =
                            json.encodeToString(HealthBackupPayload.serializer(), healthPayload)
                        backupManager
                            .uploadHealth(healthText)
                            .onSuccess {
                                // Fingerprint erst nach Erfolg speichern -> fehlgeschlagener
                                // Upload wird beim naechsten Mal erneut versucht.
                                appSettingsLazy.get().setHealthBackupFingerprint(healthFingerprint)
                                _status.value = SyncStatus.Synced(System.currentTimeMillis())
                            }
                            .onFailure { ex ->
                                _status.value =
                                    SyncStatus.Failed(
                                        "Health-Backup fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}"
                                    )
                            }
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
            requestSync("Re-Run: Aenderung kam waehrend laufendem Upload")
        }
    }

    /**
     * M2-Schutz (Read-before-write): Liest das aktuelle Remote-Haupt-Backup und rettet die
     * additiven DataStore-Listen (Mental, Ideen, Gewohnheit, Tagebuch, Thesen) NUR DANN aus dem
     * Remote, wenn die lokale Liste KOMPLETT LEER ist und das Remote sie hat. So kann ein Geraet
     * mit lokal leeren Quellen (frisch installiert / Update mit geleertem DataStore) ein volles
     * Remote-Backup nicht mehr leer ueberschreiben (Almanach M2 / Best-Practices §4).
     *
     * WICHTIG — funktionserhaltend (Direktive #3): Bewusst KEIN voller Union per ID. Diese Listen
     * haben kein Deletion-Tracking (siehe Memory reference_drive_restore_revives_deletions); ein
     * Union wuerde eine bewusste Loeschung einzelner Eintraege aus dem Remote wieder zurueckholen
     * und damit das Loeschen kaputtmachen. Eine TEILWEISE Schrumpfung (Nutzer loescht einzelne
     * Eintraege) bleibt deshalb unangetastet; nur der klare Totalverlust-Fall (lokal == leer) wird
     * gerettet. Den Multi-Device-Merge (verschiedene Geraete, verschiedene Eintraege) erledigt der
     * additive Restore beim Start (restoreFromDrive laeuft vor dem Upload).
     *
     * Aufgaben/Insights/Memories (updatedAt/Last-Write-Wins) bleiben generell aussen vor.
     *
     * Safe-Degradation (Direktive #3): Remote nicht vorhanden/lesbar -> localPayload unveraendert
     * (Verhalten exakt wie vor dem Fix, kein Datenverlust durch den Schutz selbst).
     */
    private suspend fun mergeRemoteAdditiveLists(local: BackupPayload): BackupPayload {
        val remoteRaw = restoreManagerLazy.get().fetchLatest().getOrNull() ?: return local
        val remote =
            runCatching { json.decodeFromString(BackupPayload.serializer(), remoteRaw) }
                .getOrElse {
                    diagnostics.info(
                        de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                        "M2-Schutz: Remote-Backup nicht lesbar -> lokaler Stand bleibt",
                    )
                    return local
                }
        val merged =
            local.copy(
                mentals = rescueIfLocalEmpty(local.mentals, remote.mentals),
                ideenEntries = rescueIfLocalEmpty(local.ideenEntries, remote.ideenEntries),
                gewohnheiten = rescueIfLocalEmpty(local.gewohnheiten, remote.gewohnheiten),
                tagebuchEntries = rescueIfLocalEmpty(local.tagebuchEntries, remote.tagebuchEntries),
                thesenEntries = rescueIfLocalEmpty(local.thesenEntries, remote.thesenEntries),
                taskSuggestions = rescueIfLocalEmpty(local.taskSuggestions, remote.taskSuggestions),
                gewohnheitSuggestions =
                    rescueIfLocalEmpty(local.gewohnheitSuggestions, remote.gewohnheitSuggestions),
                // Sync-Etappe 1.2: Tombstones der anderen Geraete NICHT verlieren — echter Union
                // (neuester Loeschzeitpunkt pro Eintrag), damit das hochgeladene Backup die
                // Loeschungen ALLER Geraete enthaelt (nicht nur die eigenen). Sonst wuerde ein
                // Upload ohne vorherigen Restore fremde Loeschungen im Backup ueberschreiben.
                tombstones =
                    de.frank.entropyreducer.data.unionTombstones(local.tombstones, remote.tombstones),
            )
        val rescued =
            (merged.mentals.size - local.mentals.size) +
                (merged.ideenEntries.size - local.ideenEntries.size) +
                (merged.gewohnheiten.size - local.gewohnheiten.size) +
                (merged.tagebuchEntries.size - local.tagebuchEntries.size) +
                (merged.thesenEntries.size - local.thesenEntries.size) +
                (merged.taskSuggestions.size - local.taskSuggestions.size) +
                (merged.gewohnheitSuggestions.size - local.gewohnheitSuggestions.size)
        if (rescued > 0) {
            diagnostics.info(
                de.frank.entropyreducer.data.diagnostics.DiagnosticArea.DRIVE_BACKUP,
                "M2-Schutz: $rescued Remote-Eintraege vor leerem Ueberschreiben gerettet " +
                    "(Mental ${local.mentals.size}->${merged.mentals.size}, " +
                    "Ideen ${local.ideenEntries.size}->${merged.ideenEntries.size}, " +
                    "Gewohnheit ${local.gewohnheiten.size}->${merged.gewohnheiten.size}, " +
                    "Tagebuch ${local.tagebuchEntries.size}->${merged.tagebuchEntries.size}, " +
                    "Thesen ${local.thesenEntries.size}->${merged.thesenEntries.size})",
            )
        }
        return merged
    }

    /**
     * Uebernimmt das Remote NUR, wenn die lokale Liste leer ist und das Remote nicht — der klare
     * Totalverlust-Fall. Sonst bleibt die lokale Liste unveraendert (Loeschungen/Edits gewinnen).
     */
    private fun <T> rescueIfLocalEmpty(local: List<T>, remote: List<T>): List<T> =
        if (local.isEmpty() && remote.isNotEmpty()) remote else local

    /**
     * M2-Schutz fuer das separate Workouts-Backup: true, wenn das Remote-Workouts-Backup mindestens
     * ein Training enthaelt. Safe-Degradation: nicht lesbar/nicht vorhanden -> false (dann laeuft
     * der normale Upload, wie vor dem Fix).
     */
    private suspend fun remoteWorkoutsHasData(): Boolean {
        val raw = restoreManagerLazy.get().fetchWorkouts().getOrNull() ?: return false
        return runCatching {
                json.decodeFromString(WorkoutsBackupPayload.serializer(), raw).workouts.isNotEmpty()
            }
            .getOrDefault(false)
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L

        /**
         * Frank-Wunsch 2026-06-19: laengeres Debounce-Fenster fuer den Biomarker-Bereich (5 s).
         * Dort laden mehrere APIs (Whoop/Oura/Health Connect) ueber mehrere Sekunden nach;
         * mit 5 s werden sie zu EINEM Upload gesammelt. Kommen nach dem Upload noch weitere Werte,
         * sorgt dirtyDuringUpload fuer einen zweiten Lauf.
         */
        const val BIOMARKER_DEBOUNCE_MS = 5000L
    }
}
