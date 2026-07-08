package de.frank.entropyreducer.data.remote.brain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.HabitDao
import de.frank.entropyreducer.data.local.dao.IdeaDao
import de.frank.entropyreducer.data.local.dao.IdeaWithFollowups
import de.frank.entropyreducer.data.local.dao.MentalSentenceDao
import de.frank.entropyreducer.data.local.entities.HabitEntity
import de.frank.entropyreducer.data.local.entities.IdeaEntity
import de.frank.entropyreducer.data.local.entities.MentalEntity
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.safety.PhoneContentGuard
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.presentation.tagebuch.TagebuchEntry
import de.frank.entropyreducer.presentation.tagebuch.addTagebuchEntry
import de.frank.entropyreducer.presentation.tagebuch.deleteTagebuchEntry
import de.frank.entropyreducer.presentation.tagebuch.tagebuchEntriesFlow
import de.frank.entropyreducer.presentation.thesen.ThesenEntry
import de.frank.entropyreducer.presentation.thesen.addThesenEntry
import de.frank.entropyreducer.presentation.thesen.deleteThesenEntry
import de.frank.entropyreducer.presentation.thesen.thesenEntriesFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SecondBrainIdeaSyncState(
    val syncing: Boolean = false,
    val lastMessage: String = "Noch nicht synchronisiert.",
    val syncedCount: Int = 0,
    val lastSyncedAtMs: Long = 0L,
)

data class SecondBrainArea(
    val key: String,
    val label: String,
    val category: String,
)

data class SecondBrainSyncRow(
    val id: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val title: String,
    val bodyLabel: String,
    val body: String,
    val summary: String? = null,
)

private data class SecondBrainSyncTarget(
    val area: SecondBrainArea,
    val observeRows: () -> Flow<List<SecondBrainSyncRow>>,
    val loadRows: suspend () -> List<SecondBrainSyncRow>,
    val insertFromBrain: suspend (SecondBrainCategoryItem) -> SecondBrainSyncRow?,
    val deleteById: suspend (String) -> Unit,
)

@Singleton
class SecondBrainIdeaConnector @Inject constructor(
    private val ideaDao: IdeaDao,
    private val habitDao: HabitDao,
    private val mentalDao: MentalSentenceDao,
    private val journalMirrorDao: JournalMirrorDao,
    private val settings: AppSettings,
    private val secrets: EncryptedSecretsStore,
    private val api: SecondBrainApi,
    @ApplicationContext private val appContext: Context,
) {
    private val started = AtomicBoolean(false)
    private val _state = MutableStateFlow(SecondBrainIdeaSyncState())
    val state: StateFlow<SecondBrainIdeaSyncState> = _state.asStateFlow()

    val areas: List<SecondBrainArea> = listOf(
        AREAS_IDEAS,
        AREAS_HABITS,
        AREAS_MENTAL,
        AREAS_ENTROPY,
        AREAS_THESES,
        AREAS_JOURNAL,
    )

    private val targets: List<SecondBrainSyncTarget> by lazy { buildTargets() }

    fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=connectorStart expected=observer_registered actual=observer_registered ok=true")
        targets.forEach { target ->
            scope.launch(Dispatchers.IO) {
                combine(
                    settings.secondBrainConnectorEnabledFlow(target.area.key),
                    target.observeRows(),
                ) { enabled, rows -> enabled to rows }
                    .collect { (enabled, rows) ->
                        if (enabled) {
                            Diag.d(
                                DiagnosticArea.SECOND_BRAIN,
                                TAG,
                                "CHECKPOINT step=observerEmission area=${target.area.key} enabled=true rows=${rows.size}",
                            )
                            syncRows(target, rows, reason = "Automatischer ${target.area.label}-Sync")
                        } else {
                            Diag.d(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=observerEmission area=${target.area.key} enabled=false")
                        }
                    }
            }
        }
    }

    fun syncAllNow(scope: CoroutineScope, areaKey: String? = null) {
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=manualSyncRequested expected=sync_start actual=sync_start ok=true area=${areaKey ?: "all"}")
        scope.launch(Dispatchers.IO) {
            for (target in selectedTargets(areaKey)) {
                if (!settings.secondBrainConnectorEnabledFlow(target.area.key).first()) continue
                syncRows(target, target.loadRows(), reason = "Manueller ${target.area.label}-Sync")
            }
        }
    }

    fun retryPendingUploads(scope: CoroutineScope, areaKey: String? = null) {
        scope.launch(Dispatchers.IO) {
            if (secrets.secondBrainApiKey.orEmpty().isBlank()) return@launch
            repeat(PUSH_RETRIES) { attempt ->
                var hadNetworkError = false
                for (target in selectedTargets(areaKey)) {
                    if (!settings.secondBrainConnectorEnabledFlow(target.area.key).first()) continue
                    hadNetworkError = syncRows(target, target.loadRows(), reason = "Nachhol-Upload ${target.area.label}") || hadNetworkError
                }
                if (!hadNetworkError) return@launch
                if (attempt < PUSH_RETRIES - 1) delay(PUSH_RETRY_DELAY_MS)
            }
        }
    }

    fun resyncAll(scope: CoroutineScope, areaKey: String? = null) {
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncRequested expected=resync_start actual=resync_start ok=true area=${areaKey ?: "all"}")
        scope.launch(Dispatchers.IO) {
            val key = secrets.secondBrainApiKey.orEmpty().trim()
            if (key.isBlank()) {
                _state.value = _state.value.copy(lastMessage = "Second-Brain-API-Key fehlt.")
                return@launch
            }
            val auth = "Bearer $key"
            for (target in selectedTargets(areaKey)) {
                if (!settings.secondBrainConnectorEnabledFlow(target.area.key).first()) continue
                _state.value = _state.value.copy(syncing = true, lastMessage = "Bereinige ${target.area.category} im Second Brain …")
                var removed = 0
                try {
                    val existing = api.byCategory(auth, target.area.category)
                    for (item in existing.items) {
                        val t = item.title ?: continue
                        try {
                            api.forget(auth, t)
                            removed++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncForget area=${target.area.key} actual=error ok=false title=\"$t\" message=${e.message ?: e::class.java.simpleName}")
                        }
                    }
                    Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncCleared area=${target.area.key} expected=category_emptied actual=removed=$removed ok=true")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value = _state.value.copy(syncing = false, lastMessage = "Bereinigung fehlgeschlagen: ${e.message}")
                    Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncCleared area=${target.area.key} actual=error ok=false message=${e.message ?: e::class.java.simpleName}", e)
                    continue
                }
                settings.clearAllSecondBrainSync(target.area.key)
                syncRows(target, target.loadRows(), reason = "Vollständiger Neu-Sync ${target.area.label}")
            }
        }
    }

    suspend fun testConnection(apiKeyOverride: String? = null): Boolean = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.trim().orEmpty().ifBlank { secrets.secondBrainApiKey.orEmpty() }
        if (key.isBlank()) {
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=healthCheck expected=api_key_present actual=missing ok=false")
            _state.value = _state.value.copy(lastMessage = "Second-Brain-API-Key fehlt.")
            return@withContext false
        }
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=healthCheck expected=request_sent actual=request_sent ok=true")
        try {
            val health = api.health("Bearer $key")
            val ok = health.ready || health.status == "ok"
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=healthCheckResponse expected=reachable actual=status=${health.status},ready=${health.ready},version=${health.version ?: "unbekannt"} ok=$ok")
            _state.value = _state.value.copy(
                lastMessage = if (health.ready) "Second Brain verbunden (${health.version ?: health.status})."
                else "Second Brain antwortet, lädt aber noch (${health.status}).",
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=healthCheckResponse expected=reachable actual=error ok=false message=${e.message ?: e::class.java.simpleName}", e)
            _state.value = _state.value.copy(lastMessage = "Second Brain nicht erreichbar: ${e.message}")
            false
        }
    }

    fun pullFromBrain(scope: CoroutineScope, areaKey: String? = null) {
        scope.launch(Dispatchers.IO) {
            repeat(4) {
                val ok = selectedTargets(areaKey).all { pullOnce(it) }
                if (ok) return@launch
                delay(3000)
            }
        }
    }

    private suspend fun pullOnce(target: SecondBrainSyncTarget): Boolean {
        if (!settings.secondBrainConnectorEnabledFlow(target.area.key).first()) return true
        val key = secrets.secondBrainApiKey.orEmpty().trim()
        if (key.isBlank()) return true
        val auth = "Bearer $key"
        val brain = try {
            api.byCategory(auth, target.area.category)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullUnreachable area=${target.area.key} actual=error ok=false message=${e.message ?: e::class.java.simpleName}")
            return false
        }
        val brainTitles = brain.items.mapNotNull { it.title?.trim() }.filter { it.isNotEmpty() }.toSet()
        val appRows = target.loadRows()
        val appTitles = appRows.map { it.title.trim() }.toSet()
        val uploadedMap = settings.readSecondBrainTitles(target.area.key)
        val knownTitles = uploadedMap.values.map { it.trim() }.toSet()
        var imported = 0
        for (item in brain.items) {
            val title = item.title?.trim().orEmpty()
            if (title.isBlank()) continue
            if (PhoneContentGuard.isSecondBrainWorkArtifact(title, item.text)) {
                Diag.w(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=pullSkippedPhoneArtifact area=${target.area.key} expected=not_on_phone actual=skipped ok=true title=\"$title\"",
                )
                continue
            }
            if (title in appTitles) continue
            if (title in knownTitles) continue
            val row = target.insertFromBrain(item) ?: continue
            settings.markSecondBrainSynced(target.area.key, row.id, syncStamp(row))
            settings.setSecondBrainTitle(target.area.key, row.id, row.title)
            imported++
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullImported area=${target.area.key} expected=row_created actual=created ok=true title=\"${row.title}\" id=${row.id}")
        }
        var deletedInApp = 0
        val ready = try {
            api.health(auth).ready
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        if (ready) {
            for (row in appRows) {
                val syncedTitle = (uploadedMap[row.id] ?: continue).trim()
                if (syncedTitle.isNotEmpty() && syncedTitle !in brainTitles) {
                    target.deleteById(row.id)
                    settings.clearSecondBrainSync(target.area.key, row.id)
                    deletedInApp++
                    Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullDeletedInApp area=${target.area.key} expected=row_removed actual=removed ok=true title=\"$syncedTitle\" id=${row.id}")
                }
            }
        }
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullComplete area=${target.area.key} expected=brain_scanned actual=brain=${brain.items.size},imported=$imported,deletedInApp=$deletedInApp,ready=$ready ok=true")
        if (imported > 0 || deletedInApp > 0) {
            _state.value = _state.value.copy(
                lastMessage = "${target.area.label}: $imported neu, $deletedInApp entfernt.",
                lastSyncedAtMs = System.currentTimeMillis(),
            )
        }
        return true
    }

    private suspend fun syncRows(target: SecondBrainSyncTarget, rows: List<SecondBrainSyncRow>, reason: String): Boolean {
        val key = secrets.secondBrainApiKey.orEmpty().trim()
        if (key.isBlank()) {
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=syncPreflight area=${target.area.key} expected=api_key_present actual=missing ok=false reason=$reason rows=${rows.size}")
            _state.value = _state.value.copy(lastMessage = "Connector aktiv, aber API-Key fehlt.")
            return false
        }
        val auth = "Bearer $key"
        val skippedArtifacts = rows.count { row -> PhoneContentGuard.isSecondBrainWorkArtifact(row.title, row.body) }
        val sorted = rows
            .filterNot { row -> PhoneContentGuard.isSecondBrainWorkArtifact(row.title, row.body) }
            .sortedBy { it.createdAtMs }
        if (skippedArtifacts > 0) {
            Diag.w(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=syncSkippedPhoneArtifacts area=${target.area.key} expected=not_synced actual=skipped=$skippedArtifacts ok=true reason=$reason",
            )
        }
        val currentIds = sorted.map { it.id }.toSet()
        val uploadedTitles = settings.readSecondBrainTitles(target.area.key)

        val deletedIds = uploadedTitles.keys - currentIds
        var deleted = 0
        var deletionNetworkError = false
        for (id in deletedIds) {
            val title = uploadedTitles[id] ?: continue
            try {
                val resp = api.forget(auth, title)
                settings.clearSecondBrainSync(target.area.key, id)
                deleted++
                Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetRow area=${target.area.key} expected=deleted actual=deleted=${resp.deleted} ok=${resp.ok} id=$id title=\"$title\"")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deletionNetworkError = true
                Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetRow area=${target.area.key} expected=deleted actual=error ok=false id=$id title=\"$title\" message=${e.message ?: e::class.java.simpleName}", e)
            }
        }

        val knownStamps = settings.readSecondBrainSyncStamps(target.area.key).toMutableSet()
        val pending = sorted.filter { row -> syncStamp(row) !in knownStamps }
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=syncPreflight area=${target.area.key} expected=pending_calculated actual=total=${sorted.size},pending=${pending.size},deleted=$deleted,known=${knownStamps.size} ok=true reason=$reason")
        if (pending.isEmpty()) {
            _state.value = _state.value.copy(
                lastMessage = if (deleted > 0) "$deleted ${target.area.label}-Eintrag(e) im Second Brain gelöscht, Rest aktuell."
                else "${target.area.label} ist im Second Brain aktuell.",
            )
            return deletionNetworkError
        }

        _state.value = _state.value.copy(syncing = true, lastMessage = "$reason: ${pending.size} offen.")
        var synced = 0
        for (row in pending) {
            val stamp = syncStamp(row)
            val title = brainTitle(row)
            val text = row.toBrainText()
            val previousTitle = uploadedTitles[row.id]
            if (previousTitle != null && previousTitle != title) {
                try {
                    api.forget(auth, previousTitle)
                    Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetOldTitle area=${target.area.key} expected=deleted actual=done ok=true id=${row.id} old=\"$previousTitle\" new=\"$title\"")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetOldTitle area=${target.area.key} expected=deleted actual=error ok=false id=${row.id} old=\"$previousTitle\" message=${e.message ?: e::class.java.simpleName}")
                }
            }
            val request = SecondBrainStoreRequest(title = title, category = target.area.category, text = text)
            try {
                val response = api.store(
                    authorization = auth,
                    idempotencyKey = "entropy-${target.area.key}-${row.id}-$stamp",
                    request = request,
                )
                settings.markSecondBrainSynced(target.area.key, row.id, stamp)
                settings.setSecondBrainTitle(target.area.key, row.id, title)
                knownStamps.removeAll { it.startsWith("${row.id}:") }
                knownStamps += stamp
                synced++
                Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=storeResponse area=${target.area.key} expected=ok actual=ok=${response.ok},docId=${response.docId ?: "leer"},replaced=${response.replaced} ok=${response.ok} id=${row.id}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: e::class.java.simpleName
                Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=storeResponse area=${target.area.key} expected=ok actual=error ok=false id=${row.id} title=\"$title\" synced_before_error=$synced message=$msg", e)
                _state.value = _state.value.copy(syncing = false, lastMessage = "Sync gestoppt bei '$title': $msg", syncedCount = synced)
                return true
            }
        }
        _state.value = _state.value.copy(
            syncing = false,
            lastMessage = "$reason abgeschlossen: $synced gespeichert" + (if (deleted > 0) ", $deleted gelöscht." else "."),
            syncedCount = synced,
            lastSyncedAtMs = System.currentTimeMillis(),
        )
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=syncComplete area=${target.area.key} expected=all_pending_synced actual=synced=$synced,deleted=$deleted,pending=${pending.size} ok=${synced == pending.size} reason=$reason")
        return deletionNetworkError
    }

    private fun buildTargets(): List<SecondBrainSyncTarget> = listOf(
        SecondBrainSyncTarget(
            area = AREAS_IDEAS,
            observeRows = { ideaDao.getAllIdeasWithFollowups().mapRows { it.toIdeaRows() } },
            loadRows = { loadIdeaRows() },
            insertFromBrain = { item -> insertIdeaFromBrain(item) },
            deleteById = { id -> ideaDao.deleteIdeaById(id) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_HABITS,
            observeRows = { habitDao.getAll().mapRows { rows -> rows.map { it.toSyncRow() } } },
            loadRows = { habitDao.getAllForBackup().map { it.toSyncRow() } },
            insertFromBrain = { item -> insertHabitFromBrain(item) },
            deleteById = { id -> habitDao.deleteById(id) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_MENTAL,
            observeRows = { mentalDao.getAll().mapRows { rows -> rows.map { it.toSyncRow() } } },
            loadRows = { mentalDao.getAllForBackup().map { it.toSyncRow() } },
            insertFromBrain = { item -> insertMentalFromBrain(item) },
            deleteById = { id -> mentalDao.deleteById(id) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_ENTROPY,
            observeRows = { tagebuchEntriesFlow(appContext).mapRows { rows -> rows.map { it.toEntropySyncRow() } } },
            loadRows = { tagebuchEntriesFlow(appContext).first().map { it.toEntropySyncRow() } },
            insertFromBrain = { item -> insertEntropyJournalFromBrain(item) },
            deleteById = { id -> deleteTagebuchEntry(appContext, id, propagate = false) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_THESES,
            observeRows = { thesenEntriesFlow(appContext).mapRows { rows -> rows.map { it.toSyncRow() } } },
            loadRows = { thesenEntriesFlow(appContext).first().map { it.toSyncRow() } },
            insertFromBrain = { item -> insertThesisFromBrain(item) },
            deleteById = { id -> deleteThesenEntry(appContext, id, propagate = false) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_JOURNAL,
            observeRows = { journalMirrorDao.observeEntries().mapRows { rows -> rows.map { it.toSyncRow() } } },
            loadRows = { journalMirrorDao.observeEntries().first().map { it.toSyncRow() } },
            insertFromBrain = { item -> insertJournalFromBrain(item) },
            deleteById = { id -> journalMirrorDao.deleteEntriesByIds(listOf(id.toLong())) },
        ),
    )

    private fun selectedTargets(areaKey: String?): List<SecondBrainSyncTarget> =
        if (areaKey == null) targets else targets.filter { it.area.key == areaKey }

    private suspend fun loadIdeaRows(): List<SecondBrainSyncRow> =
        ideaDao.getAllIdeasForBackup().map { idea ->
            IdeaWithFollowups(idea, ideaDao.getFollowupsForIdea(idea.id))
        }.toIdeaRows()

    private fun List<IdeaWithFollowups>.toIdeaRows(): List<SecondBrainSyncRow> = map { row ->
        val ideaText = row.idea.improvedText?.takeIf { it.isNotBlank() }?.trim() ?: row.idea.text.trim()
        SecondBrainSyncRow(
            id = row.idea.id,
            createdAtMs = row.idea.timestampMs,
            updatedAtMs = maxOf(row.idea.updatedAt ?: row.idea.timestampMs, row.followups.maxOfOrNull { it.createdAtMs } ?: 0L),
            title = row.idea.title.trim().ifBlank { "Idee ${row.idea.id.take(8)}" },
            bodyLabel = "Idee",
            body = ideaText,
            summary = row.idea.summary?.takeIf { it.isNotBlank() }?.trim(),
        )
    }

    private suspend fun insertIdeaFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val title = item.title?.trim().orEmpty()
        val ts = parseIsoToMs(item.createdAt)
        val newId = UUID.randomUUID().toString()
        val entity = IdeaEntity(
            id = newId,
            timestampMs = ts,
            title = title,
            text = item.text.trim(),
            summary = null,
            improvedText = null,
            isImproved = false,
            updatedAt = null,
        )
        ideaDao.upsertIdea(entity)
        return listOf(IdeaWithFollowups(entity, emptyList())).toIdeaRows().first()
    }

    private fun HabitEntity.toSyncRow(): SecondBrainSyncRow = SecondBrainSyncRow(
        id = id,
        createdAtMs = updatedAt.takeIf { it > 0L } ?: 0L,
        updatedAtMs = updatedAt.takeIf { it > 0L } ?: 0L,
        title = text.trim().shortTitle("Gewohnheit", id),
        bodyLabel = "Gewohnheit",
        body = text.trim(),
    )

    private suspend fun insertHabitFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val nextPosition = habitDao.maxPosition() + 1
        val entity = HabitEntity(
            id = UUID.randomUUID().toString(),
            text = item.text.trim(),
            updatedAt = ts,
            position = nextPosition,
        )
        habitDao.upsert(entity)
        return entity.toSyncRow()
    }

    private fun MentalEntity.toSyncRow(): SecondBrainSyncRow = SecondBrainSyncRow(
        id = id,
        createdAtMs = updatedAt.takeIf { it > 0L } ?: 0L,
        updatedAtMs = updatedAt.takeIf { it > 0L } ?: 0L,
        title = text.trim().shortTitle("Mental", id),
        bodyLabel = "Mental",
        body = text.trim(),
    )

    private suspend fun insertMentalFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow? {
        if (PhoneContentGuard.isSecondBrainWorkArtifact(item.title, item.text)) return null
        val text = item.text.trim()
        if (text.isBlank()) return null
        val ts = parseIsoToMs(item.createdAt)
        val nextPosition = mentalDao.maxPosition() + 1
        val entity = MentalEntity(
            id = UUID.randomUUID().toString(),
            text = text,
            updatedAt = ts,
            position = nextPosition,
        )
        mentalDao.upsert(entity)
        return entity.toSyncRow()
    }

    private fun TagebuchEntry.toEntropySyncRow(): SecondBrainSyncRow = SecondBrainSyncRow(
        id = id,
        createdAtMs = timestampMs,
        updatedAtMs = updatedAt.takeIf { it > 0L } ?: timestampMs,
        title = title.trim().ifBlank { text.trim().shortTitle("Entropie", id) },
        bodyLabel = "Entropie",
        body = (improvedText?.takeIf { isImproved && it.isNotBlank() } ?: text).trim(),
        summary = summary?.takeIf { it.isNotBlank() }?.trim(),
    )

    private suspend fun insertEntropyJournalFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val title = item.title?.trim().orEmpty().ifBlank { "Entropie ${formatTs(ts)}" }
        val text = item.text.trim()
        val entry = TagebuchEntry(
            id = UUID.randomUUID().toString(),
            timestampMs = ts,
            title = title,
            updatedAt = ts,
            text = text,
        )
        addTagebuchEntry(appContext, entry)
        return entry.toEntropySyncRow()
    }

    private fun ThesenEntry.toSyncRow(): SecondBrainSyncRow = SecondBrainSyncRow(
        id = id,
        createdAtMs = timestampMs,
        updatedAtMs = maxOf(updatedAt.takeIf { it > 0L } ?: timestampMs, followups.maxOfOrNull { it.createdAtMs } ?: 0L),
        title = title.trim().ifBlank { text.trim().shortTitle("These", id) },
        bodyLabel = "These",
        body = improvedText?.takeIf { it.isNotBlank() }?.trim() ?: text.trim(),
        summary = summary?.takeIf { it.isNotBlank() }?.trim(),
    )

    private suspend fun insertThesisFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val title = item.title?.trim().orEmpty().ifBlank { "These ${formatTs(ts)}" }
        val entry = ThesenEntry.create(item.text.trim()).copy(
            id = UUID.randomUUID().toString(),
            timestampMs = ts,
            title = title,
            updatedAt = ts,
        )
        addThesenEntry(appContext, entry)
        return entry.toSyncRow()
    }

    private suspend fun insertJournalFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val entity = JournalMirrorEntryEntity(
            sourceId = -System.nanoTime(),
            timestamp = ts,
            title = item.title?.trim(),
            displayText = item.text.trim(),
            rawText = item.text.trim(),
            improvedText = null,
            isImproved = false,
            summary = null,
        )
        journalMirrorDao.upsertEntries(listOf(entity))
        return entity.toSyncRow()
    }

    private fun JournalMirrorEntryEntity.toSyncRow(): SecondBrainSyncRow = SecondBrainSyncRow(
        id = sourceId.toString(),
        createdAtMs = timestamp,
        updatedAtMs = timestamp,
        title = title?.trim().takeUnless { it.isNullOrBlank() } ?: displayText.trim().shortTitle("Tagebucheintrag", sourceId.toString()),
        bodyLabel = "Tagebucheintrag",
        body = (improvedText?.takeIf { isImproved && it.isNotBlank() } ?: displayText.ifBlank { rawText }).trim(),
        summary = summary?.takeIf { it.isNotBlank() }?.trim(),
    )

    private fun String.shortTitle(prefix: String, id: String): String {
        val firstLine = lineSequence().firstOrNull().orEmpty().trim()
        val base = if (firstLine.length <= 80) firstLine else firstLine.take(80).trim() + "…"
        return base.ifBlank { "$prefix ${id.take(8)}" }
    }

    private fun syncStamp(row: SecondBrainSyncRow): String = "${row.id}:${maxOf(row.createdAtMs, row.updatedAtMs)}"

    private fun brainTitle(row: SecondBrainSyncRow): String = row.title.trim().ifBlank { "${row.bodyLabel} ${row.id.take(8)}" }

    private fun SecondBrainSyncRow.toBrainText(): String = buildString {
        appendLine("Erstellt am: ${formatTs(createdAtMs)}")
        appendLine("Aktualisiert am: ${formatTs(updatedAtMs)}")
        appendLine()
        appendLine("$bodyLabel: ${body.trim()}")
        if (!summary.isNullOrBlank()) {
            appendLine()
            appendLine("Zusammenfassung: ${summary.trim()}")
        }
    }.trim()

    private fun parseIsoToMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(iso).toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
    }

    private fun formatTs(ms: Long): String =
        DATE_TEXT.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    private fun <T> Flow<T>.mapRows(mapper: suspend (T) -> List<SecondBrainSyncRow>): Flow<List<SecondBrainSyncRow>> =
        map { mapper(it) }

    private companion object {
        const val TAG = "SecondBrainSync"
        val AREAS_IDEAS = SecondBrainArea("ideas", "Ideen", "Ideen")
        val AREAS_HABITS = SecondBrainArea("habits", "Gewohnheiten", "Gewohnheiten")
        val AREAS_MENTAL = SecondBrainArea("mental", "Mental", "Mental")
        val AREAS_ENTROPY = SecondBrainArea("entropy", "Entropie", "Entropie")
        val AREAS_THESES = SecondBrainArea("theses", "Thesen", "Thesen")
        val AREAS_JOURNAL = SecondBrainArea("journal", "Tagebucheinträge", "Tagebucheinträge")
        const val PUSH_RETRIES = 4
        const val PUSH_RETRY_DELAY_MS = 3000L
        val DATE_TEXT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)
    }
}
