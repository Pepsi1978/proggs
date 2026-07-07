package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
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
import de.frank.entropyreducer.data.remote.drive.WorkoutsBackupPayload
import de.frank.entropyreducer.data.remote.drive.toEntity
import de.frank.entropyreducer.data.repository.BiomarkerCardOrderRepository
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

// Live-Sonde (Frank-Wunsch 2026-06-20): jede Restore-Entscheidung fuer Tagebuch/Ideen/Thesen mit
// Grund in logcat -> `adb logcat -s ERESyncEntry` (analog EREVorschlagHeal). android.util.Log direkt,
// weil Diag bei aktivem Logger nicht in logcat schreibt.
private const val SYNC_ENTRY_TAG = "ERESyncEntry"

/**
 * High-Level UseCase für manuelles Backup + Restore. Die "automatische" Variante (jede Änderung
 * triggert ein Backup) laeuft über den [SyncCoordinator]; dieser UseCase ist für explizite Aktionen
 * aus den Einstellungen.
 *
 * Frank-Wunsch 2026-05-09 (Abend): Restore zieht jetzt vollstaendig — nicht nur Aufgaben (v1)
 * sondern auch Insights, Memories, Hypothesen, Forscher-Sessions und Forscher-/Hypothese-Messages
 * (v2). Bei v1-Backups bleiben die neuen Listen leer und nur Aufgaben kommen zurueck — keine
 * Fehler, abwaerts-kompatibel.
 */
class SyncEntriesUseCase
@Inject
constructor(
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
    private val cardOrderRepo: BiomarkerCardOrderRepository,
    private val healthConnectValueDao: de.frank.entropyreducer.data.local.dao.HealthConnectValueDao,
    private val amazfitWorkoutDao: de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao,
    // Schema v11 (Frank-Bugfix 2026-05-22): Restore der T-Rex-3-Daily-Werte
    // (PAI, BioCharge, Hauttemperatur, SpO2, Stress, Schritte, HRV, Schlaf).
    private val amazfitDailyDao: de.frank.entropyreducer.data.local.dao.AmazfitDailyDao,
    // Frank-Wunsch 2026-05-19 (Erweiterung): Whoop + Oura im Health-Backup.
    private val biomarkerSnapshotDao: de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao,
    private val whoopWorkoutDao: de.frank.entropyreducer.data.local.dao.WhoopWorkoutDao,
    private val ouraReadinessDao: de.frank.entropyreducer.data.local.dao.OuraReadinessDao,
    private val ouraDailySleepDao: de.frank.entropyreducer.data.local.dao.OuraDailySleepDao,
    private val ouraActivityDao: de.frank.entropyreducer.data.local.dao.OuraActivityDao,
    private val ouraResilienceDao: de.frank.entropyreducer.data.local.dao.OuraResilienceDao,
    private val ouraSleepDetailDao: de.frank.entropyreducer.data.local.dao.OuraSleepDetailDao,
    private val ouraPersonalInfoDao: de.frank.entropyreducer.data.local.dao.OuraPersonalInfoDao,
    private val secrets: EncryptedSecretsStore,
    private val appSettings: de.frank.entropyreducer.data.settings.AppSettings,
    // Frank-Wunsch 2026-05-20: Profil + Tagebuch + Entropie-Followups beim Restore
    // wiederherstellen.
    private val entropyEntryFollowupDao:
        de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao,
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext: android.content.Context,
    private val promptRepo: de.frank.entropyreducer.data.repository.PromptRepository,
    // Frank-Wunsch 2026-05-21: Agentic-AI v9 — Permissions + Triggers restorebar
    private val promptToolPermissionRepo:
        de.frank.entropyreducer.data.repository.PromptToolPermissionRepository,
    private val promptTriggerRepo:
        de.frank.entropyreducer.data.repository.PromptTriggerRepository,
    // Sprint 2.8 (Frank-Wunsch 2026-05-22): Wiederkehrende Aufgaben-Vorlagen restorebar.
    private val recurringTemplateRepo:
        de.frank.entropyreducer.data.repository.RecurringTemplateRepository,
    // Frank-Wunsch 2026-06-19: Prioritaets-Gedaechtnis restorebar.
    private val priorityMemoryRepo:
        de.frank.entropyreducer.data.repository.PriorityMemoryRepository,
    private val json: Json,
) {

    /** Manuell ein Sofort-Backup ausloesen. */
    fun backupNow() = coordinator.requestImmediate()

    /**
     * Vom Drive heruntergeladene Daten mit dem lokalen Bestand mergen.
     *
     * Strategien pro Entity-Typ:
     * - Aufgaben + Insights + Memories: Last-Write-Wins per `updatedAt`. Wenn lokal nicht
     *   vorhanden, einfuegen. Wenn lokal vorhanden und Backup juenger, ueberschreiben.
     * - Hypothesen + Sessions + Messages: Existenz-basiert. Wenn lokal nicht vorhanden, einfuegen.
     *   Wenn lokal vorhanden, NICHT ueberschreiben (Messages aendern sich nach Erstellung nicht;
     *   Hypothesen und Sessions haben kein updatedAt-Feld — konservative Strategie um lokale
     *   Aenderungen nicht zu zerstoeren).
     *
     * Returns: aggregierte Counts ueber alle Entity-Typen.
     */
    suspend fun restoreFromDrive(): Result<RestoreOutcome> {
        val downloadResult = restoreManager.fetchLatest()
        val raw =
            downloadResult.getOrElse {
                return Result.failure(it)
            } ?: return Result.success(RestoreOutcome.NoBackup)

        val payload =
            runCatching { json.decodeFromString(BackupPayload.serializer(), raw) }
                .getOrElse {
                    return Result.failure(it)
                }

        // Diagnose-Sonde (Frank-Bugfix 2026-06-19): zeigt im Log GENAU, was das vom Drive
        // geladene Haupt-Backup an Aufgabenreiter-Daten enthaelt — damit live nachvollziehbar
        // ist, ob Mental/Ideen/Gewohnheit ueberhaupt im Backup ankommen (Multi-Device-Overwrite
        // M2 vs. leeres Backup) oder ob der Restore sie nicht einspielt. Reine Messung.
        Diag.i(
            DiagnosticArea.DRIVE_BACKUP,
            "SyncEntries",
            "Restore-Payload v${payload.version}: entries=${payload.entries.size}, " +
                "mentals=${payload.mentals.size}, ideen=${payload.ideenEntries.size}, " +
                "gewohnheiten=${payload.gewohnheiten.size}, tagebuch=${payload.tagebuchEntries.size}, " +
                "thesen=${payload.thesenEntries.size}, " +
                "aufgabenvorschlaege=${payload.taskSuggestions.size}, " +
                "gewohnheitsvorschlaege=${payload.gewohnheitSuggestions.size}, " +
                "tombstones=${payload.tombstones.size}",
        )

        var inserted = 0
        var updated = 0
        var deleted = 0

        // Live-Logik-Sonde (Frank-Wunsch 2026-07-03, #47447): Bestand der Kind-Datensaetze
        // VOR dem Merge festhalten. Der Abschluss-CHECKPOINT prueft die Invariante
        // "Followups schrumpfen nie staerker, als Tombstone-Loeschungen erklaeren" —
        // Fruehwarnung fuer stillen Multi-Device-Kind-Datenverlust (z.B. REPLACE+CASCADE).
        val followupsVorher = entropyEntryFollowupDao.getAllForBackup().size
        var followupTombstoneDeletes = 0

        // --- Loesch-Protokoll (Tombstones, Sync-Etappe 1.2) ---
        // Remote-Tombstones in den lokalen Store mergen (neuester Loeschzeitpunkt pro Eintrag
        // gewinnt) und die Gesamtliste fuer das Anwenden je Typ holen.
        val allTombstones =
            de.frank.entropyreducer.data.mergeRemoteTombstones(appContext, payload.tombstones)
        val aufgabeDeletedAt =
            allTombstones
                .filter { it.type == de.frank.entropyreducer.data.TombstoneType.AUFGABE }
                .associate { it.id to it.deletedAt }

        // --- Aufgaben (immer dabei, auch bei v1-Backups) ---
        for (backupEntry in payload.entries) {
            val incoming = backupEntry.toEntity()
            // delete-wins-only-if-newer: eine geloeschte Aufgabe NICHT aus dem Backup wieder
            // einspielen, wenn der Tombstone neuer ist als die Backup-Version dieser Aufgabe.
            val tombstoneAt = aufgabeDeletedAt[incoming.id]
            if (tombstoneAt != null && tombstoneAt > incoming.updatedAt) continue
            val existing = entryRepo.get(incoming.id)
            when {
                existing == null -> {
                    entryRepo.upsert(incoming)
                    inserted++
                }
                incoming.updatedAt > existing.updatedAt -> {
                    // DIAGNOSE-SONDE (2026-06-19, Bucket-Rollback-Bug): wenn der Restore eine
                    // Bucket-Zuordnung ueberschreibt, GENAU festhalten was vorher/nachher galt.
                    if (incoming.manualBucket != existing.manualBucket ||
                        incoming.timeBucket != existing.timeBucket
                    ) {
                        Diag.i(
                            DiagnosticArea.DRIVE_BACKUP,
                            "SyncEntries",
                            "RESTORE ueberschreibt Bucket '${existing.title.take(24)}': " +
                                "lokal mb=${existing.manualBucket}/tb=${existing.timeBucket}/upd=${existing.updatedAt} " +
                                "-> Backup mb=${incoming.manualBucket}/tb=${incoming.timeBucket}/upd=${incoming.updatedAt}",
                        )
                    }
                    entryRepo.upsert(incoming)
                    updated++
                }
                else -> Unit
            }
        }

        // Tombstones auf LOKAL noch vorhandene Aufgaben anwenden: was ein anderes Geraet geloescht
        // hat (Tombstone neuer als die lokale Version), hier ebenfalls loeschen. deleteByIdForRestore
        // schreibt KEINEN neuen Tombstone und loest keinen Sync aus (der Tombstone ist schon da).
        for ((id, deletedAt) in aufgabeDeletedAt) {
            val local = entryRepo.get(id) ?: continue
            if (deletedAt > local.updatedAt) {
                entryRepo.deleteByIdForRestore(id)
                deleted++
            }
        }

        // --- Insights (v2+, bei v1 leere Liste) ---
        // Direktive 3 Loop-3-Fix (war L3-3-Bug): existing-Set EINMAL vor dem
        // Loop laden statt pro Eintrag. Bei N=100 Insights ergab das vorher
        // 100 vollstaendige DB-Scans. ANR-Risiko entfaellt damit.
        val existingInsightsMap =
            insightDao.getByConfidenceDesc().first().associateBy { it.id }
        for (b in payload.insights) {
            val incoming = b.toEntity()
            val existing = existingInsightsMap[incoming.id]
            when {
                existing == null -> {
                    insightDao.upsert(incoming)
                    inserted++
                }
                incoming.updatedAt > existing.updatedAt -> {
                    insightDao.upsert(incoming)
                    updated++
                }
                else -> Unit
            }
        }

        // --- Memories (v2+) ---
        // Loop-3-Fix L3-3: O(n)-Map vorab statt n×Full-Scan
        val existingMemoriesMap = memoryDao.getAll().first().associateBy { it.id }
        for (b in payload.memories) {
            val incoming = b.toEntity()
            val existing = existingMemoriesMap[incoming.id]
            when {
                existing == null -> {
                    memoryDao.upsert(incoming)
                    inserted++
                }
                incoming.updatedAt > existing.updatedAt -> {
                    memoryDao.upsert(incoming)
                    updated++
                }
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

        // --- Biomarker-Card-Order (v3+, Frank-Wunsch 2026-05-10) ---
        // Drag&Drop-Reihenfolge des Biomarker-Screens wiederherstellen. Leere
        // Liste = User hatte beim Backup nichts verschoben — dann nichts machen
        // (die lokale Reihenfolge bleibt unangetastet). saveOrder() filtert
        // ungueltige IDs (aus aelteren App-Versionen) automatisch heraus.
        if (payload.biomarkerCardOrder.isNotEmpty()) {
            cardOrderRepo.saveOrder(payload.biomarkerCardOrder)
            updated++
        }

        // --- Health-Connect-Werte-Cache (v4+, Frank-Wunsch 2026-05-10 abend) ---
        // Cross-Device-Sync: was auf dem Fold 6 in HC steht, landet ueber das
        // Backup auch hier. Insert ist via PrimaryKey (metric, timestampMs)
        // idempotent — bestehende Werte werden ueberschrieben aber das ist OK
        // weil HC-Werte immutable sind (selber Timestamp = selber Messwert).
        if (payload.healthConnectValues.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val entities =
                payload.healthConnectValues.map {
                    de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity(
                        metric = it.metric,
                        timestampMs = it.timestampMs,
                        value = it.value,
                        createdAt = now,
                    )
                }
            healthConnectValueDao.upsertAll(entities)
            inserted += entities.size
        }

        // --- Amazfit-Workouts inkl. Detail-Streams (v5+, Frank-Wunsch 2026-05-11) ---
        // Cross-Device-Restore: Trainings vom Fold 6 landen ueber das Backup
        // direkt auf dem S23 Ultra — KEIN Zepp-API-Call noetig, kein Re-Login,
        // kein Kicken aus der Zepp-Handy-App. Detail-Bewahrung wie beim Sync-Fix:
        // Wenn das lokale Geraet bereits Detail-Felder hat die im Backup leer
        // sind, bleiben die lokalen Daten erhalten. So gewinnt der NEUERE Stand
        // immer — egal ob er aus dem Backup oder vom lokalen Sync kam.
        //
        // Frank-Wunsch 2026-05-16: Nach erfolgreicher Cleanup-Migration (Flag
        // workoutCleanupV1Done) den Workout-Block ueberspringen — sonst wuerden
        // alte Drive-Backups mit 391 Workouts den geleerten Stand sofort wieder
        // ueberschreiben. Wenn Frank's Polar-Integration spaeter Workouts
        // schreibt, ueberschreibt der naechste regulaere Sync das Drive-Backup
        // mit dem Polar-Stand — der Restore wird dann nur Polar-Workouts mergen.
        val workoutCleanupDone = appSettings.isWorkoutCleanupV1Done()
        if (workoutCleanupDone && payload.amazfitWorkouts.isNotEmpty()) {
            Diag.i(DiagnosticArea.DRIVE_BACKUP, 
                "SyncEntries",
                "Restore: ueberspringe ${payload.amazfitWorkouts.size} Workouts aus Backup (Cleanup-Migration aktiv)",
            )
        }
        if (!workoutCleanupDone && payload.amazfitWorkouts.isNotEmpty()) {
            // Frank-Bugfix 2026-07-04: manuell geloeschte Trainings nicht aus dem Backup zurueckholen.
            val mainDeletedStarts = appSettings.getDeletedWorkoutStarts()
            val mainDeleteToleranceMs = 5L * 60L * 1000L
            val merged =
                payload.amazfitWorkouts.map { backupWorkout ->
                    val freshFromBackup = backupWorkout.toEntity()
                    val existing = amazfitWorkoutDao.getById(freshFromBackup.trackId)
                    if (existing == null) {
                        freshFromBackup
                    } else {
                        freshFromBackup.copy(
                            gpsTrackJson = freshFromBackup.gpsTrackJson ?: existing.gpsTrackJson,
                            heartRateSeriesJson =
                                freshFromBackup.heartRateSeriesJson ?: existing.heartRateSeriesJson,
                            paceSeriesJson =
                                freshFromBackup.paceSeriesJson ?: existing.paceSeriesJson,
                            paceStreamJson =
                                freshFromBackup.paceStreamJson ?: existing.paceStreamJson,
                            splitsJson = freshFromBackup.splitsJson ?: existing.splitsJson,
                        )
                    }
                }
                    .filterNot { entity ->
                        mainDeletedStarts.any { kotlin.math.abs(it - entity.startMs) <= mainDeleteToleranceMs }
                    }
            amazfitWorkoutDao.upsertAll(merged)
            inserted += merged.size
        }

        // Frank-Wunsch 2026-05-19: Separate Workouts-Datei (entropy_reducer_workouts_v1.json)
        // restoren — enthaelt im Gegensatz zum Hauptbackup auch GPS-Track,
        // Pulsverlauf, Pace und Splits. Nur einlesen wenn vorhanden und nicht
        // durch die Cleanup-Migration unterdrueckt.
        val workoutsInserted = restoreWorkoutsBackup(skipDueToCleanup = workoutCleanupDone)
        inserted += workoutsInserted

        // --- Amazfit-Daily-Werte (v11+, Frank-Bugfix 2026-05-22) ---
        // PAI/BioCharge/Hauttemperatur/SpO2/Stress/Schritte/HRV/Schlaf werden
        // jetzt im Backup gesichert. Restore via upsertAll — PrimaryKey ist
        // `date` (YYYY-MM-DD), bei Konflikt gewinnt der eingelesene Wert.
        // Bei v1-v10-Backups ist die Liste leer (Default emptyList) -> kein
        // Datenverlust auf der lokalen Seite.
        if (payload.amazfitDaily.isNotEmpty()) {
            val dailyEntities = payload.amazfitDaily.map { it.toEntity() }
            amazfitDailyDao.upsertAll(dailyEntities)
            inserted += dailyEntities.size
            Diag.i(DiagnosticArea.DRIVE_BACKUP, 
                "SyncEntries",
                "Restore: ${dailyEntities.size} Amazfit-Daily-Eintraege wiederhergestellt",
            )
        }

        // Frank-Wunsch 2026-05-19 (Erweiterung): Whoop + Oura-Backup wiederherstellen.
        // Eigene Datei `entropy_reducer_health_v1.json`. Nicht-Existenz ist OK
        // (Erst-Restore vor erstem Health-Upload).
        val healthInserted = restoreHealthBackup()
        inserted += healthInserted

        // --- Profil-Text (v6+, Frank-Wunsch 2026-05-20) ---
        // Profil ueberschreibt nur wenn lokal leer ODER Backup neuer wirkt.
        // Hier konservativ: Backup gewinnt nur wenn lokal leer — sonst koennte ein
        // alter Backup-Stand frische lokale Edits ueberschreiben.
        if (payload.profileText.isNotBlank()) {
            val localProfile = appSettings.profileTextFlow.first()
            if (localProfile.isBlank()) {
                appSettings.setProfileText(payload.profileText)
                updated++
            }
        }

        // --- Tagebuch-Eintraege (v6+) ---
        // DataStore-basiert. Frank-Wunsch 2026-06-20: Tombstones anwenden, sonst kommt ein auf einem
        // 2. Geraet geloeschter Tagebuch-Eintrag ueber den additiven Restore wieder.
        run {
            val tagebuchDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.TAGEBUCH }
                    .associate { it.id to it.deletedAt }
            if (payload.tagebuchEntries.isNotEmpty() || tagebuchDeletedAt.isNotEmpty()) {
                val existingMap =
                    de.frank.entropyreducer.presentation.tagebuch
                        .tagebuchEntriesFlow(appContext)
                        .first()
                        .associateBy { it.id }
                val incomingById = payload.tagebuchEntries.associateBy { it.id }
                // Backup-DTO -> UI-Modell. updatedAt-Baseline = timestampMs, falls das Backup noch
                // kein updatedAt trug (altes Backup vor Phase B -> Default 0L).
                fun toIncoming(b: de.frank.entropyreducer.data.remote.drive.BackupTagebuchEntry) =
                    de.frank.entropyreducer.presentation.tagebuch.TagebuchEntry(
                        id = b.id,
                        timestampMs = b.timestampMs,
                        title = b.title,
                        text = b.text,
                        summary = b.summary,
                        followups =
                            b.followups.map { f ->
                                de.frank.entropyreducer.presentation.tagebuch.TagebuchFollowup(
                                    id = f.id,
                                    createdAtMs = f.createdAtMs,
                                    text = f.text,
                                )
                            },
                        updatedAt = if (b.updatedAt > 0L) b.updatedAt else b.timestampMs,
                    )
                // 1. Bestehende: Tombstone (delete-wins-only-if-newer) ODER LWW-Update (incoming neuer).
                //    DataStore-Update = loeschen (propagate=false) + neu hinzufuegen; timestampMs bleibt,
                //    daher aendert sich die Sortierung (sortedByDescending timestampMs) nicht.
                for (ex in existingMap.values) {
                    val ts = tagebuchDeletedAt[ex.id]
                    if (ts != null && ts > ex.updatedAt) {
                        de.frank.entropyreducer.presentation.tagebuch.deleteTagebuchEntry(
                            appContext, ex.id, propagate = false)
                        deleted++
                        android.util.Log.i(SYNC_ENTRY_TAG, "Tagebuch ${ex.id} geloescht (Tombstone ts=$ts > updatedAt=${ex.updatedAt})")
                        continue
                    }
                    val b = incomingById[ex.id] ?: continue
                    val inc = toIncoming(b)
                    if (inc.updatedAt > ex.updatedAt) {
                        de.frank.entropyreducer.presentation.tagebuch.deleteTagebuchEntry(
                            appContext, ex.id, propagate = false)
                        de.frank.entropyreducer.presentation.tagebuch.addTagebuchEntry(appContext, inc)
                        updated++
                        android.util.Log.i(SYNC_ENTRY_TAG, "Tagebuch ${ex.id} aktualisiert (LWW: inc=${inc.updatedAt} > ex=${ex.updatedAt})")
                    }
                }
                // 2. Neue (im Backup, nicht lokal) — ausser frisch getombstonet (Loeschung neuer).
                for (b in payload.tagebuchEntries) {
                    if (existingMap.containsKey(b.id)) continue
                    val inc = toIncoming(b)
                    val ts = tagebuchDeletedAt[b.id]
                    if (ts != null && ts > inc.updatedAt) continue
                    de.frank.entropyreducer.presentation.tagebuch.addTagebuchEntry(appContext, inc)
                    inserted++
                    android.util.Log.i(SYNC_ENTRY_TAG, "Tagebuch ${b.id} neu eingespielt")
                }
            }
        }

        // --- Mentalboard (v12+) ---
        // Frank-Wunsch 2026-06-20: IMMER aufrufen + Mental-Tombstones durchreichen (Loeschung
        // propagiert, restoreMentals raeumt lokal getombstonete weg — auch bei leerem incoming).
        run {
            val mentalDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.MENTAL }
                    .associate { it.id to it.deletedAt }
            inserted +=
                de.frank.entropyreducer.presentation.mental.restoreMentals(
                    appContext,
                    // v19 (2026-06-20): Backup-DTO MIT Herkunft direkt durchreichen.
                    payload.mentals,
                    mentalDeletedAt,
                )
        }

        // --- Ideen-Eintraege (v13+, Frank-Wunsch 2026-06-10) ---
        // DataStore-basiert wie Tagebuch. Frank-Wunsch 2026-06-20: Tombstones anwenden (Loeschung
        // propagiert; sonst kommt eine auf einem 2. Geraet geloeschte Idee ueber den Restore wieder).
        run {
            val ideeDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.IDEE }
                    .associate { it.id to it.deletedAt }
            if (payload.ideenEntries.isNotEmpty() || ideeDeletedAt.isNotEmpty()) {
                val existingIdeenMap =
                    de.frank.entropyreducer.presentation.ideen
                        .ideenEntriesFlow(appContext)
                        .first()
                        .associateBy { it.id }
                val incomingIdeenById = payload.ideenEntries.associateBy { it.id }
                // updatedAt-Baseline = timestampMs (Ideen-updatedAt ist nullable: Bestand/altes Backup).
                fun toIncoming(b: de.frank.entropyreducer.data.remote.drive.BackupIdeenEntry) =
                    de.frank.entropyreducer.presentation.ideen.IdeenEntry(
                        id = b.id,
                        timestampMs = b.timestampMs,
                        title = b.title,
                        text = b.text,
                        summary = b.summary,
                        followups =
                            b.followups.map { f ->
                                de.frank.entropyreducer.presentation.ideen.IdeenFollowup(
                                    id = f.id,
                                    createdAtMs = f.createdAtMs,
                                    text = f.text,
                                )
                            },
                        updatedAt = b.updatedAt ?: b.timestampMs,
                    )
                // 1. Bestehende: Tombstone (delete-wins-only-if-newer) ODER LWW-Update (incoming neuer).
                //    Update via replaceIdeenEntryFromSync -> erhaelt die Room-Herkunft (originId/originType/rootId),
                //    die das Backup-DTO NICHT traegt (sonst gleiche Falle wie der v19-Gewohnheits-Fix).
                for (ex in existingIdeenMap.values) {
                    val exUpdatedAt = ex.updatedAt ?: ex.timestampMs
                    val ts = ideeDeletedAt[ex.id]
                    if (ts != null && ts > exUpdatedAt) {
                        de.frank.entropyreducer.presentation.ideen.deleteIdeenEntry(
                            appContext, ex.id, propagate = false)
                        deleted++
                        android.util.Log.i(SYNC_ENTRY_TAG, "Idee ${ex.id} geloescht (Tombstone ts=$ts > updatedAt=$exUpdatedAt)")
                        continue
                    }
                    val b = incomingIdeenById[ex.id] ?: continue
                    val inc = toIncoming(b)
                    if ((inc.updatedAt ?: inc.timestampMs) > exUpdatedAt) {
                        de.frank.entropyreducer.presentation.ideen.replaceIdeenEntryFromSync(appContext, inc)
                        updated++
                        android.util.Log.i(SYNC_ENTRY_TAG, "Idee ${ex.id} aktualisiert (LWW: inc=${inc.updatedAt ?: inc.timestampMs} > ex=$exUpdatedAt)")
                    }
                }
                // 2. Neue (im Backup, nicht lokal) — ausser frisch getombstonet (Loeschung neuer).
                for (b in payload.ideenEntries) {
                    if (existingIdeenMap.containsKey(b.id)) continue
                    val inc = toIncoming(b)
                    val ts = ideeDeletedAt[b.id]
                    if (ts != null && ts > (inc.updatedAt ?: inc.timestampMs)) continue
                    de.frank.entropyreducer.presentation.ideen.addIdeenEntry(appContext, inc)
                    inserted++
                    android.util.Log.i(SYNC_ENTRY_TAG, "Idee ${b.id} neu eingespielt")
                }
            }
        }

        // --- Gewohnheit-Eintraege (v14+, Frank-Bugfix 2026-06-19) ---
        // DataStore-basiert wie Mental. restoreGewohnheiten ergaenzt nur fehlende IDs —
        // lokale Reihenfolge/Edits gewinnen (konservativ, exakt wie Mental/Tagebuch).
        // Bisher GAR NICHT restored, obwohl restoreGewohnheiten() bereits existierte.
        run {
            val gewohnheitDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.GEWOHNHEIT }
                    .associate { it.id to it.deletedAt }
            if (payload.gewohnheiten.isNotEmpty() || gewohnheitDeletedAt.isNotEmpty()) {
                inserted +=
                    de.frank.entropyreducer.presentation.mental.restoreGewohnheiten(
                        appContext,
                        // v19 (2026-06-20, Direktive #3 robust): das Backup-DTO MIT Herkunft direkt
                        // durchreichen statt es ins herkunftslose Mental-UI-Modell zu wandeln.
                        payload.gewohnheiten,
                        gewohnheitDeletedAt,
                    )
            }
        }

        // --- KI-Vorschlaege (v15+, Frank-Wunsch 2026-06-19) ---
        // Offene Aufgaben- und Gewohnheitsvorschlaege wiederherstellen. Existenz-Strategie:
        // nur fehlende IDs ergaenzen, lokale gewinnen (regenerierbar, aber sollen nicht verloren
        // gehen). Bei v1-v14-Backups sind die Listen leer -> kein Effekt.
        // Vorschlags-Restore (Frank-Wunsch 2026-06-20): IMMER aufrufen — auch bei leerem incoming —,
        // damit der Heal lokale Alt-Vorschlaege aufraeumt (per Tombstone geloescht ODER Idee schon
        // angenommen). Tombstones propagieren die Loeschung von einem 2. Geraet (delete-wins).
        run {
            // Bugfix 2026-06-20: per Tombstone GELOESCHTE Ideen -> ihre abgeleiteten Vorschlaege sind
            // verwaist und duerfen nicht (wieder) eingespielt werden. Greift auch, wenn die abgeleitete
            // Gewohnheit/Aufgabe VOR der Idee geloescht wurde (dann ist countByRootId=0 und nur der
            // Idee-Tombstone zeigt die Loeschung noch an).
            val ideaDeletedIds =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.IDEE }
                    .map { it.id }
                    .toSet()
            val taskSuggestionDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.TASK_SUGGESTION }
                    .associate { it.id to it.deletedAt }
            inserted +=
                de.frank.entropyreducer.data.restoreTaskSuggestions(
                    appContext,
                    payload.taskSuggestions,
                    taskSuggestionDeletedAt,
                    ideaDeletedIds,
                )
            val habitSuggestionDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.HABIT_SUGGESTION }
                    .associate { it.id to it.deletedAt }
            inserted +=
                de.frank.entropyreducer.data.restoreGewohnheitSuggestions(
                    appContext,
                    payload.gewohnheitSuggestions,
                    habitSuggestionDeletedAt,
                    ideaDeletedIds,
                )
        }

        // --- Aufgaben-Nachtraege (v6+) ---
        // Existenz-basiert: id ist UUID, Doppelung quasi unmoeglich. Wenn lokal vorhanden,
        // gewinnt der lokale Stand (Inline-Edits seit letztem Backup).
        run {
            val followupDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.FOLLOWUP }
                    .associate { it.id to it.deletedAt }
            // LWW + Tombstone (Sync-Etappe 1.5): frueher Existenz-Strategie -> Nachtrag-Edits/
            // -Loeschungen propagierten nie. Jetzt per updatedAt + delete-wins-only-if-newer.
            for (b in payload.entropyEntryFollowups) {
                val incoming = b.toEntity()
                val ts = followupDeletedAt[b.id]
                if (ts != null && ts > incoming.updatedAt) continue
                val existing = entropyEntryFollowupDao.getById(b.id)
                when {
                    existing == null -> {
                        entropyEntryFollowupDao.upsert(incoming)
                        inserted++
                    }
                    incoming.updatedAt > existing.updatedAt -> {
                        entropyEntryFollowupDao.upsert(incoming)
                        updated++
                    }
                    else -> Unit
                }
            }
            for ((id, ts) in followupDeletedAt) {
                val local = entropyEntryFollowupDao.getById(id) ?: continue
                if (ts > local.updatedAt) {
                    entropyEntryFollowupDao.deleteById(id)
                    deleted++
                    followupTombstoneDeletes++
                }
            }
        }

        // --- Saved Prompts (v8+, Frank-Wunsch 2026-05-20) ---
        // Existenz-Strategie pro id. Bei unbekannter Kategorie fallback auf AUFGABEN.
        // v9 (Frank 2026-05-21): zusaetzliche Felder model/tokenLimitPerDay/trustModeDefault
        // werden uebernommen — bei alten v8-Backups defaulten sie automatisch.
        if (payload.savedPrompts.isNotEmpty()) {
            val existingPromptIds = promptRepo.getAll().first().map { it.id }.toHashSet()
            for (b in payload.savedPrompts) {
                if (b.id in existingPromptIds) continue
                val cat =
                    runCatching {
                            de.frank.entropyreducer.domain.model.PromptCategory.valueOf(b.category)
                        }
                        .getOrDefault(de.frank.entropyreducer.domain.model.PromptCategory.AUFGABEN)
                promptRepo.upsert(
                    de.frank.entropyreducer.data.local.entities.SavedPromptEntity(
                        id = b.id,
                        name = b.name,
                        content = b.content,
                        isActive = b.isActive,
                        createdAt = b.createdAt,
                        updatedAt = b.updatedAt,
                        category = cat,
                        model = b.model,
                        tokenLimitPerDay = b.tokenLimitPerDay,
                        trustModeDefault = b.trustModeDefault,
                    )
                )
                inserted++
            }
        }

        // --- Prompt-Tool-Permissions (v9+, Frank-Wunsch 2026-05-21) ---
        // Existenz-Strategie pro id. CASCADE-FK: nur einspielen wenn der zugehoerige
        // Prompt schon existiert (sonst FK-Constraint-Verletzung).
        if (payload.promptToolPermissions.isNotEmpty()) {
            val existingPromptIds = promptRepo.getAll().first().map { it.id }.toHashSet()
            for (b in payload.promptToolPermissions) {
                if (b.promptId !in existingPromptIds) continue
                val existing = promptToolPermissionRepo.getOne(b.promptId, b.toolName)
                if (existing != null) continue
                promptToolPermissionRepo.upsert(
                    de.frank.entropyreducer.data.local.entities.PromptToolPermissionEntity(
                        id = b.id,
                        promptId = b.promptId,
                        toolName = b.toolName,
                        granted = b.granted,
                        trustMode = b.trustMode,
                    )
                )
                inserted++
            }
        }

        // --- Prompt-Triggers (v9+, Frank-Wunsch 2026-05-21) ---
        // nextScheduledAt wird vom TriggerScheduler beim naechsten Worker-Lauf neu
        // berechnet — hier nur die statische Konfiguration einspielen.
        if (payload.promptTriggers.isNotEmpty()) {
            val existingPromptIds = promptRepo.getAll().first().map { it.id }.toHashSet()
            for (b in payload.promptTriggers) {
                if (b.promptId !in existingPromptIds) continue
                if (promptTriggerRepo.getById(b.id) != null) continue
                val triggerType =
                    runCatching {
                            de.frank.entropyreducer.domain.model.TriggerType.valueOf(b.triggerType)
                        }
                        .getOrDefault(de.frank.entropyreducer.domain.model.TriggerType.MANUAL)
                val nextAt =
                    if (triggerType == de.frank.entropyreducer.domain.model.TriggerType.CRON &&
                            b.cronExpression != null
                    )
                        de.frank.entropyreducer.domain.agentic.trigger.SimpleCronParser.nextFireAt(
                            b.cronExpression
                        )
                    else null
                promptTriggerRepo.upsert(
                    de.frank.entropyreducer.data.local.entities.PromptTriggerEntity(
                        id = b.id,
                        promptId = b.promptId,
                        triggerType = triggerType,
                        cronExpression = b.cronExpression,
                        eventCondition = b.eventCondition,
                        chainAfterPromptId = b.chainAfterPromptId,
                        isActive = b.isActive,
                        nextScheduledAt = nextAt,
                    )
                )
                inserted++
            }
        }

        // --- Wiederkehrende Aufgaben-Vorlagen (v10+; Sync-Etappe 1.3: Last-Write-Wins + Tombstone) ---
        // Frueher reine Existenz-Strategie ("id existiert -> ueberspringen") -> Edits an einer schon
        // bekannten Vorlage (Intervall, Aktivieren/Deaktivieren, Titel, Prioritaet, Ziel-Bucket)
        // propagierten NIE auf das Zweitgeraet. Jetzt LWW per updatedAt + Tombstone-Loeschung.
        run {
            val loopDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.LOOP_TEMPLATE }
                    .associate { it.id to it.deletedAt }
            val existingTemplates = recurringTemplateRepo.getAllForBackup().associateBy { it.id }
            for (b in payload.recurringTemplates) {
                val incoming = b.toEntity()
                // delete-wins-only-if-newer: geloeschte Vorlage nicht aus dem Backup wiederbeleben.
                val tombstoneAt = loopDeletedAt[b.id]
                if (tombstoneAt != null && tombstoneAt > incoming.updatedAt) continue
                val existing = existingTemplates[b.id]
                when {
                    existing == null -> {
                        recurringTemplateRepo.upsert(incoming)
                        inserted++
                    }
                    incoming.updatedAt > existing.updatedAt -> {
                        recurringTemplateRepo.upsert(incoming)
                        updated++
                    }
                    else -> Unit
                }
            }
            // Tombstones auf lokal noch vorhandene Vorlagen anwenden (frischer Stand nach den Upserts).
            for ((id, deletedAt) in loopDeletedAt) {
                val local = recurringTemplateRepo.getById(id) ?: continue
                if (deletedAt > local.updatedAt) {
                    recurringTemplateRepo.deleteByIdForRestore(id)
                    deleted++
                }
            }
        }

        // --- Prioritaets-Gedaechtnis (v17+; Frank-Wunsch 2026-06-19: LWW + Tombstone) ---
        run {
            val memDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.PRIORITY_MEMORY }
                    .associate { it.id to it.deletedAt }
            val existingMemories = priorityMemoryRepo.getAllForBackup().associateBy { it.id }
            for (b in payload.priorityMemories) {
                val incoming = b.toEntity()
                // delete-wins-only-if-newer: geloeschten Eintrag nicht aus dem Backup wiederbeleben.
                val tombstoneAt = memDeletedAt[b.id]
                if (tombstoneAt != null && tombstoneAt > incoming.updatedAt) continue
                val existing = existingMemories[b.id]
                when {
                    existing == null -> {
                        priorityMemoryRepo.upsert(incoming)
                        inserted++
                    }
                    incoming.updatedAt > existing.updatedAt -> {
                        priorityMemoryRepo.upsert(incoming)
                        updated++
                    }
                    else -> Unit
                }
            }
            // Tombstones auf lokal noch vorhandene Eintraege anwenden (frischer Stand nach den Upserts).
            for ((id, deletedAt) in memDeletedAt) {
                val local = priorityMemoryRepo.getById(id) ?: continue
                if (deletedAt > local.updatedAt) {
                    priorityMemoryRepo.deleteByIdForRestore(id)
                    deleted++
                }
            }
        }

        // --- Thesen-Eintraege (v7+, Frank-Wunsch 2026-05-20) ---
        // Frank-Wunsch 2026-06-20: Tombstones anwenden (Loeschung propagiert; sonst kommt eine auf
        // einem 2. Geraet geloeschte These ueber den additiven Restore wieder).
        run {
            val theseDeletedAt =
                allTombstones
                    .filter { it.type == de.frank.entropyreducer.data.TombstoneType.THESE }
                    .associate { it.id to it.deletedAt }
            if (payload.thesenEntries.isNotEmpty() || theseDeletedAt.isNotEmpty()) {
                val existingThesenMap =
                    de.frank.entropyreducer.presentation.thesen
                        .thesenEntriesFlow(appContext)
                        .first()
                        .associateBy { it.id }
                val incomingThesenById = payload.thesenEntries.associateBy { it.id }
                fun toIncoming(b: de.frank.entropyreducer.data.remote.drive.BackupThesenEntry) =
                    de.frank.entropyreducer.presentation.thesen.ThesenEntry(
                        id = b.id,
                        timestampMs = b.timestampMs,
                        title = b.title,
                        text = b.text,
                        summary = b.summary,
                        followups =
                            b.followups.map { f ->
                                de.frank.entropyreducer.presentation.thesen.ThesenFollowup(
                                    id = f.id,
                                    createdAtMs = f.createdAtMs,
                                    text = f.text,
                                )
                            },
                        updatedAt = if (b.updatedAt > 0L) b.updatedAt else b.timestampMs,
                    )
                // 1. Bestehende: Tombstone (delete-wins-only-if-newer) ODER LWW-Update (incoming neuer).
                for (ex in existingThesenMap.values) {
                    val ts = theseDeletedAt[ex.id]
                    if (ts != null && ts > ex.updatedAt) {
                        de.frank.entropyreducer.presentation.thesen.deleteThesenEntry(
                            appContext, ex.id, propagate = false)
                        deleted++
                        android.util.Log.i(SYNC_ENTRY_TAG, "These ${ex.id} geloescht (Tombstone ts=$ts > updatedAt=${ex.updatedAt})")
                        continue
                    }
                    val b = incomingThesenById[ex.id] ?: continue
                    val inc = toIncoming(b)
                    if (inc.updatedAt > ex.updatedAt) {
                        de.frank.entropyreducer.presentation.thesen.deleteThesenEntry(
                            appContext, ex.id, propagate = false)
                        de.frank.entropyreducer.presentation.thesen.addThesenEntry(appContext, inc)
                        updated++
                        android.util.Log.i(SYNC_ENTRY_TAG, "These ${ex.id} aktualisiert (LWW: inc=${inc.updatedAt} > ex=${ex.updatedAt})")
                    }
                }
                // 2. Neue (im Backup, nicht lokal) — ausser frisch getombstonet (Loeschung neuer).
                for (b in payload.thesenEntries) {
                    if (existingThesenMap.containsKey(b.id)) continue
                    val inc = toIncoming(b)
                    val ts = theseDeletedAt[b.id]
                    if (ts != null && ts > inc.updatedAt) continue
                    de.frank.entropyreducer.presentation.thesen.addThesenEntry(appContext, inc)
                    inserted++
                    android.util.Log.i(SYNC_ENTRY_TAG, "These ${b.id} neu eingespielt")
                }
            }
        }

        // Diagnose-Sonde (Frank-Wunsch 2026-06-19): Abschluss-Bilanz. Zusammen mit der
        // "Restore-Payload"-Zeile oben (was im Backup ankam) ist damit sichtbar, was beim Start
        // tatsaechlich NEU eingespielt wurde — die Differenz war lokal bereits vorhanden.
        Diag.i(
            DiagnosticArea.DRIVE_BACKUP,
            "SyncEntries",
            "Restore abgeschlossen: $inserted Eintraege NEU eingespielt, $updated aktualisiert, " +
                "$deleted via Tombstone geloescht (Tombstones gesamt: ${allTombstones.size})",
        )

        // Live-Logik-Sonden (Frank-Wunsch 2026-07-03, #47447): der LWW-Merge bestaetigt sich
        // selbst (kind CHECKPOINT, gleicher Stil wie HabitRoomMigrator). Grep-Hebel:
        // adb logcat | grep "CHECKPOINT sync=" — ok=false ist ein Multi-Device-Verlust-Alarm.
        Diag.i(
            DiagnosticArea.DRIVE_BACKUP,
            "SyncEntries",
            "CHECKPOINT sync=lww_merge inserted=$inserted updated=$updated deleted=$deleted ok=true",
        )
        val followupsNachher = entropyEntryFollowupDao.getAllForBackup().size
        val followupsMinErwartet = followupsVorher - followupTombstoneDeletes
        val followupsOk = followupsNachher >= followupsMinErwartet
        if (followupsOk) {
            Diag.i(
                DiagnosticArea.DRIVE_BACKUP,
                "SyncEntries",
                "CHECKPOINT sync=followup_bestand erwartetMin=$followupsMinErwartet " +
                    "tatsaechlich=$followupsNachher (vorher=$followupsVorher, " +
                    "tombstoneLoeschungen=$followupTombstoneDeletes) ok=true",
            )
        } else {
            Diag.e(
                DiagnosticArea.DRIVE_BACKUP,
                "SyncEntries",
                "CHECKPOINT sync=followup_bestand erwartetMin=$followupsMinErwartet " +
                    "tatsaechlich=$followupsNachher (vorher=$followupsVorher, " +
                    "tombstoneLoeschungen=$followupTombstoneDeletes) ok=false — Kind-Datensaetze " +
                    "staerker geschrumpft als Tombstones erklaeren (moeglicher CASCADE-/Merge-Verlust)!",
            )
        }
        driveSession.end()
        return Result.success(RestoreOutcome.Merged(inserted = inserted, updated = updated))
    }

    /**
     * Frank-Wunsch 2026-05-19: Liest die separate Workouts-Backup-Datei aus dem Drive-
     * appDataFolder und schreibt sie in die DB. Streams aus dem Backup gewinnen, fehlende Streams
     * werden mit dem lokalen Stand gefuellt (kein Datenverlust).
     *
     * Frank-Bugfix 2026-05-22: `skipDueToCleanup`-Parameter wird ignoriert.
     * Begruendung: das `workouts_v1.json` auf Drive wird vom alten Geraet NACH der
     * Cleanup-Migration mit dem korrekten Stand neu hochgeladen. Es gibt keinen
     * Pre-Cleanup-Stand mehr darin — Skippen wuerde den Trainings-Backup
     * unnoetig wegwerfen. Vorher fuehrte das dazu dass Frank nach `adb uninstall`
     * keine Trainings zurueckbekam, weil die Cleanup-Migration auf dem neuen
     * Geraet schon gelaufen war bevor er Drive verbunden hat.
     */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun restoreWorkoutsBackup(skipDueToCleanup: Boolean): Int {
        val raw = restoreManager.fetchWorkouts().getOrNull() ?: return 0
        val workoutsPayload =
            runCatching { json.decodeFromString(WorkoutsBackupPayload.serializer(), raw) }
                .getOrElse {
                    Diag.w(DiagnosticArea.DRIVE_BACKUP, "SyncEntries", "Workouts-Backup nicht lesbar", it)
                    return 0
                }
        if (workoutsPayload.workouts.isEmpty()) return 0
        // Frank-Bugfix 2026-07-04: manuell geloeschte Trainings NICHT aus dem Backup wiederherstellen
        // (sonst kommt ein geloeschtes Training beim naechsten Restore zurueck).
        val deletedStarts = appSettings.getDeletedWorkoutStarts()
        val restoreToleranceMs = 5L * 60L * 1000L
        val merged =
            workoutsPayload.workouts.map { backupWorkout ->
                val freshFromBackup = backupWorkout.toEntity()
                val existing = amazfitWorkoutDao.getById(freshFromBackup.trackId)
                // Frank-Wunsch 2026-05-25 (Restore-Haertung): Eine lokal MANUELL
                // editierte Version (manualOverridesMs gesetzt) darf NICHT von einem
                // aelteren Backup ueberschrieben werden — sonst kommen z.B. alte
                // Bewegungszeiten zurueck, nachdem Frank die verstrichene Zeit
                // korrigiert hat. Hat das Backup einen NEUEREN manuellen Edit (z.B.
                // von einem anderen Geraet), gewinnt das Backup. Streams (GPS/Puls/
                // Pace/Splits) sind kein manuell editierbares Feld und werden in
                // beiden Faellen von der jeweils anderen Seite ergaenzt wenn sie dort
                // fehlen. Nicht-manuelle Workouts verhalten sich exakt wie bisher.
                val localMs = existing?.manualOverridesMs
                val backupMs = freshFromBackup.manualOverridesMs
                val localManualWins =
                    existing != null && localMs != null && (backupMs == null || localMs >= backupMs)
                when {
                    existing == null -> freshFromBackup
                    localManualWins ->
                        existing.copy(
                            gpsTrackJson = existing.gpsTrackJson ?: freshFromBackup.gpsTrackJson,
                            heartRateSeriesJson =
                                existing.heartRateSeriesJson ?: freshFromBackup.heartRateSeriesJson,
                            paceSeriesJson = existing.paceSeriesJson ?: freshFromBackup.paceSeriesJson,
                            paceStreamJson = existing.paceStreamJson ?: freshFromBackup.paceStreamJson,
                            splitsJson = existing.splitsJson ?: freshFromBackup.splitsJson,
                        )
                    else ->
                        freshFromBackup.copy(
                            gpsTrackJson = freshFromBackup.gpsTrackJson ?: existing.gpsTrackJson,
                            heartRateSeriesJson =
                                freshFromBackup.heartRateSeriesJson ?: existing.heartRateSeriesJson,
                            paceSeriesJson = freshFromBackup.paceSeriesJson ?: existing.paceSeriesJson,
                            paceStreamJson = freshFromBackup.paceStreamJson ?: existing.paceStreamJson,
                            splitsJson = freshFromBackup.splitsJson ?: existing.splitsJson,
                        )
                }
            }
                .filterNot { entity ->
                    deletedStarts.any { kotlin.math.abs(it - entity.startMs) <= restoreToleranceMs }
                }
        amazfitWorkoutDao.upsertAll(merged)
        Diag.i(DiagnosticArea.DRIVE_BACKUP,
            "SyncEntries",
            "Restore: ${merged.size} Workouts aus Backup wiederhergestellt (geloeschte via Tombstone uebersprungen)",
        )
        return merged.size
    }

    /**
     * Frank-Wunsch 2026-05-19 (Erweiterung): Liest `entropy_reducer_health_v1.json` und schreibt
     * Whoop + Oura-Daten in die DB. Strategie: idempotenter Upsert pro Tabelle (Primaerschluessel
     * sind id/day) — lokale Daten werden ueberschrieben falls im Backup derselbe Eintrag existiert.
     * Wenn das Backup leer ist oder noch nicht existiert, passiert nichts (kein Datenverlust).
     */
    private suspend fun restoreHealthBackup(): Int {
        val raw = restoreManager.fetchHealth().getOrNull() ?: return 0
        val payload =
            runCatching {
                    json.decodeFromString(
                        de.frank.entropyreducer.data.remote.drive.HealthBackupPayload.serializer(),
                        raw,
                    )
                }
                .getOrElse {
                    Diag.w(DiagnosticArea.DRIVE_BACKUP, "SyncEntries", "Health-Backup nicht lesbar", it)
                    return 0
                }
        var count = 0
        // Whoop Daily Recovery — Performance-Fix 2026-07-03 (#47449): Batch-Upsert in EINER
        // Transaktion statt einer Einzeltransaktion pro Snapshot (vorher ~300 pro App-Start).
        if (payload.whoopSnapshots.isNotEmpty()) {
            val snaps = payload.whoopSnapshots.map { it.toEntity() }
            biomarkerSnapshotDao.upsertAll(snaps)
            count += snaps.size
        }
        // Whoop Workouts
        if (payload.whoopWorkouts.isNotEmpty()) {
            val list = payload.whoopWorkouts.map { it.toEntity() }
            whoopWorkoutDao.upsertAll(list)
            count += list.size
        }
        // Oura 6 Tabellen
        if (payload.ouraReadiness.isNotEmpty()) {
            val list = payload.ouraReadiness.map { it.toEntity() }
            ouraReadinessDao.upsertAll(list)
            count += list.size
        }
        if (payload.ouraDailySleep.isNotEmpty()) {
            val list = payload.ouraDailySleep.map { it.toEntity() }
            ouraDailySleepDao.upsertAll(list)
            count += list.size
        }
        if (payload.ouraActivity.isNotEmpty()) {
            val list = payload.ouraActivity.map { it.toEntity() }
            ouraActivityDao.upsertAll(list)
            count += list.size
        }
        if (payload.ouraResilience.isNotEmpty()) {
            val list = payload.ouraResilience.map { it.toEntity() }
            ouraResilienceDao.upsertAll(list)
            count += list.size
        }
        if (payload.ouraSleepDetail.isNotEmpty()) {
            val list = payload.ouraSleepDetail.map { it.toEntity() }
            ouraSleepDetailDao.upsertAll(list)
            count += list.size
        }
        payload.ouraPersonalInfo?.let {
            ouraPersonalInfoDao.upsert(it.toEntity())
            count++
        }
        Diag.i(DiagnosticArea.DRIVE_BACKUP, "SyncEntries", "Restore: $count Whoop+Oura-Eintraege wiederhergestellt")
        return count
    }

    /** Hat das Drive-Konto bereits ein Backup? */
    suspend fun hasRemoteBackup(): Boolean = restoreManager.hasBackup()

    sealed interface RestoreOutcome {
        data object NoBackup : RestoreOutcome

        data class Merged(val inserted: Int, val updated: Int) : RestoreOutcome
    }
}
