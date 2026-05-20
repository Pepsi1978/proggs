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
import de.frank.entropyreducer.data.remote.drive.WorkoutsBackupPayload
import de.frank.entropyreducer.data.remote.drive.toEntity
import de.frank.entropyreducer.data.repository.BiomarkerCardOrderRepository
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

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

        var inserted = 0
        var updated = 0

        // --- Aufgaben (immer dabei, auch bei v1-Backups) ---
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

        // --- Insights (v2+, bei v1 leere Liste) ---
        for (b in payload.insights) {
            val incoming = b.toEntity()
            val all = insightDao.getByConfidenceDesc().first()
            val existing = all.firstOrNull { it.id == incoming.id }
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
        for (b in payload.memories) {
            val incoming = b.toEntity()
            val all = memoryDao.getAll().first()
            val existing = all.firstOrNull { it.id == incoming.id }
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
            android.util.Log.i(
                "SyncEntries",
                "Restore: ueberspringe ${payload.amazfitWorkouts.size} Workouts aus Backup (Cleanup-Migration aktiv)",
            )
        }
        if (!workoutCleanupDone && payload.amazfitWorkouts.isNotEmpty()) {
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
            amazfitWorkoutDao.upsertAll(merged)
            inserted += merged.size
        }

        // Frank-Wunsch 2026-05-19: Separate Workouts-Datei (entropy_reducer_workouts_v1.json)
        // restoren — enthaelt im Gegensatz zum Hauptbackup auch GPS-Track,
        // Pulsverlauf, Pace und Splits. Nur einlesen wenn vorhanden und nicht
        // durch die Cleanup-Migration unterdrueckt.
        val workoutsInserted = restoreWorkoutsBackup(skipDueToCleanup = workoutCleanupDone)
        inserted += workoutsInserted

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
        // DataStore-basiert, kein DAO. Wir mergen ueber die internen Helper
        // im TagebuchScreen.kt (internal-Sichtbarkeit).
        if (payload.tagebuchEntries.isNotEmpty()) {
            val existingMap =
                de.frank.entropyreducer.presentation.tagebuch
                    .tagebuchEntriesFlow(appContext)
                    .first()
                    .associateBy { it.id }
            for (b in payload.tagebuchEntries) {
                val incoming =
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
                    )
                val existing = existingMap[incoming.id]
                if (existing == null) {
                    de.frank.entropyreducer.presentation.tagebuch.addTagebuchEntry(
                        appContext,
                        incoming,
                    )
                    inserted++
                }
                // Konservativ: vorhandene Eintraege NICHT ueberschreiben — Tagebuch hat kein
                // updatedAt-Feld, und lokale Edits sollen nicht durch alte Backups verloren gehen.
            }
        }

        // --- Aufgaben-Nachtraege (v6+) ---
        // Existenz-basiert: id ist UUID, Doppelung quasi unmoeglich. Wenn lokal vorhanden,
        // gewinnt der lokale Stand (Inline-Edits seit letztem Backup).
        if (payload.entropyEntryFollowups.isNotEmpty()) {
            for (b in payload.entropyEntryFollowups) {
                val existing = entropyEntryFollowupDao.getById(b.id)
                if (existing == null) {
                    entropyEntryFollowupDao.upsert(b.toEntity())
                    inserted++
                }
            }
        }

        // --- Saved Prompts (v8+, Frank-Wunsch 2026-05-20) ---
        // Existenz-Strategie pro id. Bei unbekannter Kategorie fallback auf AUFGABEN
        // (siehe TypeConverter.toPromptCategory).
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
                    )
                )
                inserted++
            }
        }

        // --- Thesen-Eintraege (v7+, Frank-Wunsch 2026-05-20) ---
        // Spiegelt das Tagebuch-Verhalten: Existenz-Strategie, lokale Edits gewinnen.
        if (payload.thesenEntries.isNotEmpty()) {
            val existingThesenMap =
                de.frank.entropyreducer.presentation.thesen
                    .thesenEntriesFlow(appContext)
                    .first()
                    .associateBy { it.id }
            for (b in payload.thesenEntries) {
                val incoming =
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
                    )
                val existing = existingThesenMap[incoming.id]
                if (existing == null) {
                    de.frank.entropyreducer.presentation.thesen.addThesenEntry(appContext, incoming)
                    inserted++
                }
            }
        }

        driveSession.end()
        return Result.success(RestoreOutcome.Merged(inserted = inserted, updated = updated))
    }

    /**
     * Frank-Wunsch 2026-05-19: Liest die separate Workouts-Backup-Datei aus dem Drive-
     * appDataFolder und schreibt sie in die DB. Streams aus dem Backup gewinnen, fehlende Streams
     * werden mit dem lokalen Stand gefuellt (kein Datenverlust).
     */
    private suspend fun restoreWorkoutsBackup(skipDueToCleanup: Boolean): Int {
        if (skipDueToCleanup) return 0
        val raw = restoreManager.fetchWorkouts().getOrNull() ?: return 0
        val workoutsPayload =
            runCatching { json.decodeFromString(WorkoutsBackupPayload.serializer(), raw) }
                .getOrElse {
                    android.util.Log.w("SyncEntries", "Workouts-Backup nicht lesbar", it)
                    return 0
                }
        if (workoutsPayload.workouts.isEmpty()) return 0
        val merged =
            workoutsPayload.workouts.map { backupWorkout ->
                val freshFromBackup = backupWorkout.toEntity()
                val existing = amazfitWorkoutDao.getById(freshFromBackup.trackId)
                if (existing == null) {
                    freshFromBackup
                } else {
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
        amazfitWorkoutDao.upsertAll(merged)
        android.util.Log.i(
            "SyncEntries",
            "Restore: ${merged.size} Workouts aus separatem Backup wiederhergestellt",
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
                    android.util.Log.w("SyncEntries", "Health-Backup nicht lesbar", it)
                    return 0
                }
        var count = 0
        // Whoop Daily Recovery
        payload.whoopSnapshots
            .map { it.toEntity() }
            .forEach { snap ->
                biomarkerSnapshotDao.upsert(snap)
                count++
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
        android.util.Log.i("SyncEntries", "Restore: $count Whoop+Oura-Eintraege wiederhergestellt")
        return count
    }

    /** Hat das Drive-Konto bereits ein Backup? */
    suspend fun hasRemoteBackup(): Boolean = restoreManager.hasBackup()

    sealed interface RestoreOutcome {
        data object NoBackup : RestoreOutcome

        data class Merged(val inserted: Int, val updated: Int) : RestoreOutcome
    }
}
