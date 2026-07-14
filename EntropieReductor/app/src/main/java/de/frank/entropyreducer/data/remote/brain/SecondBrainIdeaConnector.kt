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
import de.frank.entropyreducer.presentation.tagebuch.TagebuchArea
import de.frank.entropyreducer.presentation.tagebuch.addTagebuchEntry
import de.frank.entropyreducer.presentation.tagebuch.deleteTagebuchEntry
import de.frank.entropyreducer.presentation.tagebuch.tagebuchEntriesFlow
import de.frank.entropyreducer.presentation.tagebuch.tagebuchEntriesFlowOrNull
import de.frank.entropyreducer.presentation.thesen.ThesenEntry
import de.frank.entropyreducer.presentation.thesen.addThesenEntry
import de.frank.entropyreducer.presentation.thesen.deleteThesenEntry
import de.frank.entropyreducer.presentation.thesen.thesenEntriesFlow
import de.frank.entropyreducer.presentation.thesen.thesenEntriesFlowOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

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
    val contentRevision: Int? = null,
    // Nachtraege/Followups fuers Second Brain (Fix 2026-07-14): frueher fehlten sie im
    // hochgeladenen Text, obwohl ihr Hinzufuegen einen Re-Upload triggerte.
    val followups: List<String> = emptyList(),
)

private data class SecondBrainSyncTarget(
    val area: SecondBrainArea,
    // Fix 2026-07-14: null = Parse-/Lesefehler der Quelle (NICHT "leer") — der Sync
    // ueberspringt dann, statt jeden Eintrag faelschlich als geloescht zu behandeln.
    val observeRows: () -> Flow<List<SecondBrainSyncRow>?>,
    val loadRows: suspend () -> List<SecondBrainSyncRow>?,
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
    // Kurz-Timeout-Client (~3s) nur fuer den Erreichbarkeits-Check vor Sync-Laeufen (#13).
    @Named("secondBrainQuick") private val quickApi: SecondBrainApi,
    @ApplicationContext private val appContext: Context,
) {
    @Volatile private var observerJob: Job? = null
    private val operationMutex = Mutex()
    private val _state = MutableStateFlow(SecondBrainIdeaSyncState())
    val state: StateFlow<SecondBrainIdeaSyncState> = _state.asStateFlow()

    val areas: List<SecondBrainArea> = listOf(
        AREAS_IDEAS,
        AREAS_HABITS,
        AREAS_MENTAL,
        AREAS_ENTROPY,
        AREAS_LEARNING,
        AREAS_THESES,
        AREAS_JOURNAL,
    )

    private val targets: List<SecondBrainSyncTarget> by lazy { buildTargets() }

    @Synchronized
    fun start(scope: CoroutineScope) {
        if (observerJob?.isActive == true) return
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=connectorStart expected=observer_registered actual=observer_registered ok=true")
        observerJob = scope.launch(Dispatchers.IO) {
            supervisorScope {
                targets.forEach { target ->
                    launch {
                        while (true) {
                            try {
                                combine(
                                    settings.secondBrainConnectorEnabledFlow(target.area.key),
                                    target.observeRows(),
                                ) { enabled, rows -> enabled to rows }
                                    .collect { (enabled, rows) ->
                                        when {
                                            enabled && rows != null -> {
                                                Diag.d(
                                                    DiagnosticArea.SECOND_BRAIN,
                                                    TAG,
                                                    "CHECKPOINT step=observerEmission area=${target.area.key} enabled=true rows=${rows.size}",
                                                )
                                                syncRowsWithRetry(
                                                    target,
                                                    rows,
                                                    reason = "Automatischer ${target.area.label}-Sync",
                                                )
                                            }
                                            enabled -> {
                                                // rows == null: Parse-/Lesefehler der Quelle — NICHT als
                                                // "alles geloescht" behandeln, sonst wuerde der Sync jeden
                                                // Eintrag im Second Brain loeschen. Lauf ueberspringen.
                                                Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=observerSkippedParseError area=${target.area.key} expected=skip_on_source_error actual=skipped ok=true")
                                            }
                                            else -> {
                                                Diag.d(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=observerEmission area=${target.area.key} enabled=false")
                                            }
                                        }
                                    }
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } catch (t: Throwable) {
                                Diag.e(
                                    DiagnosticArea.SECOND_BRAIN,
                                    TAG,
                                    "Observer fuer ${target.area.key} fehlgeschlagen; Neustart in ${PUSH_RETRY_DELAY_MS}ms",
                                    t,
                                )
                                delay(PUSH_RETRY_DELAY_MS)
                            }
                        }
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
                // null = Quelle nicht lesbar (Parse-Fehler) → Lauf ueberspringen, nichts loeschen.
                val rows = target.loadRows() ?: continue
                syncRows(target, rows, reason = "Manueller ${target.area.label}-Sync")
            }
        }
    }

    fun retryPendingUploads(scope: CoroutineScope, areaKey: String? = null) {
        scope.launch(Dispatchers.IO) { retryPendingUploadsNow(areaKey) }
    }

    /** Retry-Pfad fuer Speichern und Lifecycle-Flush; kehrt erst nach Erfolg/Retry-Ende zurueck. */
    suspend fun retryPendingUploadsNow(areaKey: String? = null) {
        if (secrets.secondBrainApiKey.orEmpty().isBlank()) return
        for (target in selectedTargets(areaKey)) {
            if (!settings.secondBrainConnectorEnabledFlow(target.area.key).first()) continue
            // null = Quelle nicht lesbar → nichts nachschieben/loeschen.
            val rows = target.loadRows() ?: continue
            syncRowsWithRetry(
                target,
                rows,
                reason = "Nachhol-Upload ${target.area.label}",
            )
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
                operationMutex.withLock {
                    _state.value = _state.value.copy(syncing = true, lastMessage = "Bereinige ${target.area.category} im Second Brain …")
                    var removed = 0
                    try {
                        val existing = api.byCategory(auth, target.area.category)
                        check(existing.ok && existing.ready) { "Second Brain ist noch nicht vollständig bereit" }
                        // Lokales Titel-Meta (id -> Brain-Titel) fuer (a) Zugehoerigkeits-Check und
                        // (b) sofortiges Clearen pro geloeschtem Item.
                        val metaTitles = settings.readSecondBrainTitles(target.area.key)
                        val titleToId = metaTitles.entries.associate { (id, title) -> title.trim() to id }
                        // Fix 2026-07-14 (#1): Loesch+Meta-Clear-Sequenz gegen Abbruch/Netzfehler
                        // schuetzen — sonst zeigt das Meta noch Titel, die im Brain fehlen, und der
                        // naechste Pull loescht die LOKALEN Eintraege. Pro geloeschtem Item wird das
                        // Meta SOFORT geleert; der gesamte Block laeuft NonCancellable.
                        withContext(NonCancellable) {
                            for (item in existing.items) {
                                val t = item.title ?: continue
                                // Fix 2026-07-14 (#5): nur eigene Eintraege loeschen — (a) im lokalen
                                // Titel-Meta ODER (b) mit App-Marker; Fremdes (source=librarian) bleibt.
                                val belongsToApp =
                                    titleToId.containsKey(t.trim()) ||
                                        parseAppBrainTitle(t, target.area.key).rowId != null ||
                                        item.source.equals("entropyreductor", ignoreCase = true)
                                if (!belongsToApp) continue
                                val response = api.forget(auth, t)
                                check(response.ok) { "Löschen von '$t' wurde nicht bestätigt" }
                                titleToId[t.trim()]?.let { settings.clearSecondBrainSync(target.area.key, it) }
                                removed++
                            }
                            // Rest-Meta ohne Brain-Match ebenfalls leeren.
                            settings.clearAllSecondBrainSync(target.area.key)
                        }
                        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncCleared area=${target.area.key} expected=category_emptied actual=removed=$removed ok=true")
                        val freshRows = target.loadRows()
                        if (freshRows != null) {
                            syncRowsUnlocked(target, freshRows, reason = "Vollständiger Neu-Sync ${target.area.label}")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(syncing = false, lastMessage = "Bereinigung fehlgeschlagen: ${e.message}")
                        Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncCleared area=${target.area.key} actual=error ok=false message=${e.message ?: e::class.java.simpleName}", e)
                    }
                }
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
            val authenticated = api.list("Bearer $key", limit = 0)
            val ok = authenticated.ok && authenticated.ready
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=healthCheckResponse expected=reachable_and_authenticated actual=status=${health.status},ready=${authenticated.ready},version=${health.version ?: "unbekannt"} ok=$ok")
            _state.value = _state.value.copy(
                lastMessage = if (ok) "Second Brain verbunden (${health.version ?: health.status})."
                else "Second Brain antwortet, lädt aber noch (${health.status}).",
            )
            ok
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
                val ok = selectedTargets(areaKey).map { pullOnce(it) }.all { it }
                if (ok) return@launch
                delay(3000)
            }
        }
    }

    private suspend fun pullOnce(target: SecondBrainSyncTarget): Boolean = operationMutex.withLock {
        pullOnceUnlocked(target)
    }

    private suspend fun pullOnceUnlocked(target: SecondBrainSyncTarget): Boolean {
        if (!settings.secondBrainConnectorEnabledFlow(target.area.key).first()) {
            return true
        }
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
        if (!brain.ok || !brain.ready) return false
        val brainTitles = brain.items.mapNotNull { it.title?.trim() }.filter { it.isNotEmpty() }.toSet()
        // null = Quelle nicht lesbar (Parse-Fehler) → Pull ueberspringen, KEINE lokalen Loeschungen.
        val appRows = target.loadRows() ?: run {
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullSkippedSourceError area=${target.area.key} expected=skip_on_source_error actual=skipped ok=true")
            return true
        }
        // Fix 2026-07-14 (#8): waehrend des Pulls frisch importierte Titel mitfuehren, damit ein
        // zweiter Brain-Eintrag mit gleichem Titel im SELBEN Lauf nicht doppelt importiert wird.
        val appTitles = appRows.map { it.title.trim().lowercase(Locale.ROOT) }.toMutableSet()
        val uploadedMap = settings.readSecondBrainTitles(target.area.key)
        val knownTitles = uploadedMap.values.map { it.trim() }.toMutableSet()
        var imported = 0
        val importedRowIds = mutableSetOf<String>()
        for (item in brain.items) {
            val title = item.title?.trim().orEmpty()
            if (title.isBlank()) continue
            val parsedTitle = parseAppBrainTitle(title, target.area.key)
            if (target.area.key != AREAS_IDEAS.key && item.source.equals("librarian", ignoreCase = true)) {
                Diag.i(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=pullSkippedLibrarian area=${target.area.key} expected=no_agent_phone_write actual=skipped ok=true title=\"$title\"",
                )
                continue
            }
            if (PhoneContentGuard.isSecondBrainWorkArtifact(title, item.text)) {
                Diag.w(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=pullSkippedPhoneArtifact area=${target.area.key} expected=not_on_phone actual=skipped ok=true title=\"$title\"",
                )
                continue
            }
            if (parsedTitle.displayTitle.lowercase(Locale.ROOT) in appTitles) {
                continue
            }
            if (title in knownTitles) {
                continue
            }
            val row = target.insertFromBrain(
                item.copy(title = parsedTitle.displayTitle, appRowId = parsedTitle.rowId)
            ) ?: continue
            settings.markSecondBrainSynced(target.area.key, row.id, syncStamp(row), title)
            importedRowIds += row.id
            // Frisch importierte Titel als bekannt markieren (Fix #8 gegen Duplikat-Import).
            appTitles += parsedTitle.displayTitle.lowercase(Locale.ROOT)
            knownTitles += title
            imported++
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullImported area=${target.area.key} expected=row_created actual=created ok=true title=\"${row.title}\" id=${row.id}")
        }
        var deletedInApp = 0
        val ready = brain.ready
        if (ready) {
            // Fix 2026-07-14 (#2): Lösch-Propagation nur mit Plausibilitätsschwelle. ok=true+ready=true
            // mit leerer/kollabierter Server-Liste (neu aufgesetzt/umbenannt) darf NICHT die lokalen
            // Eintraege loeschen.
            val deletionCandidates = appRows.filter { row ->
                row.id !in importedRowIds &&
                    (uploadedMap[row.id]?.trim()?.let { it.isNotEmpty() && it !in brainTitles } == true)
            }
            val knownSyncedCount = appRows.count { row -> uploadedMap[row.id]?.trim()?.isNotEmpty() == true }
            val brainEmpty = brainTitles.isEmpty()
            // >50% der bekannten synchronisierten Eintraege auf einmal → verdaechtig.
            val tooMany = knownSyncedCount > 0 && deletionCandidates.size * 2 > knownSyncedCount
            when {
                deletionCandidates.isEmpty() -> Unit
                brainEmpty && knownSyncedCount > 0 -> {
                    Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullDeletionSuppressedEmptyServer area=${target.area.key} expected=no_mass_delete actual=suppressed ok=true candidates=${deletionCandidates.size} known=$knownSyncedCount")
                    _state.value = _state.value.copy(lastMessage = "${target.area.label}: Server-Liste leer — lokale Löschungen zur Sicherheit übersprungen.")
                }
                tooMany -> {
                    Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullDeletionSuppressedThreshold area=${target.area.key} expected=no_mass_delete actual=suppressed ok=true candidates=${deletionCandidates.size} known=$knownSyncedCount")
                    _state.value = _state.value.copy(lastMessage = "${target.area.label}: Ungewöhnlich viele Löschungen (${deletionCandidates.size}/${knownSyncedCount}) übersprungen.")
                }
                else -> {
                    for (row in deletionCandidates) {
                        val syncedTitle = (uploadedMap[row.id] ?: continue).trim()
                        target.deleteById(row.id)
                        settings.clearSecondBrainSync(target.area.key, row.id)
                        deletedInApp++
                        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=pullDeletedInApp area=${target.area.key} expected=row_removed actual=removed ok=true title=\"$syncedTitle\" id=${row.id}")
                    }
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

    private suspend fun syncRows(target: SecondBrainSyncTarget, rows: List<SecondBrainSyncRow>, reason: String): Boolean =
        operationMutex.withLock { syncRowsUnlocked(target, rows, reason) }

    private suspend fun syncRowsUnlocked(target: SecondBrainSyncTarget, rows: List<SecondBrainSyncRow>, reason: String): Boolean {
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
        var deletionFailed = false
        for (id in deletedIds) {
            val title = uploadedTitles[id] ?: continue
            try {
                val resp = api.forget(auth, title)
                check(resp.ok) { "Second Brain hat die Loeschung nicht bestaetigt" }
                settings.clearSecondBrainSync(target.area.key, id)
                deleted++
                Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetRow area=${target.area.key} expected=deleted actual=deleted=${resp.deleted} ok=${resp.ok} id=$id title=\"$title\"")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deletionFailed = true
                deletionNetworkError = deletionNetworkError || isRetryable(e)
                Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetRow area=${target.area.key} expected=deleted actual=error ok=false id=$id title=\"$title\" message=${e.message ?: e::class.java.simpleName}", e)
            }
        }

        val knownStamps = settings.readSecondBrainSyncStamps(target.area.key).toMutableSet()
        val pending = sorted.filter { row -> syncStamp(row) !in knownStamps }
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=syncPreflight area=${target.area.key} expected=pending_calculated actual=total=${sorted.size},pending=${pending.size},deleted=$deleted,known=${knownStamps.size} ok=true reason=$reason")
        if (pending.isEmpty()) {
            _state.value = _state.value.copy(
                syncing = false,
                lastMessage = when {
                    deletionFailed -> "${target.area.label}: Mindestens eine Loeschung ist fehlgeschlagen."
                    deleted > 0 -> "$deleted ${target.area.label}-Eintrag(e) im Second Brain gelöscht, Rest aktuell."
                    else -> "${target.area.label} ist im Second Brain aktuell."
                },
            )
            return deletionNetworkError
        }

        _state.value = _state.value.copy(syncing = true, lastMessage = "$reason: ${pending.size} offen.")
        var synced = 0
        // Fix 2026-07-14 (#4): nicht-retrybare Fehler (400/413/422/ok=false) blockieren nicht mehr
        // die restlichen Zeilen (kein Head-of-line-Blocking); der "Gift-Eintrag" wird uebersprungen.
        var rowFailures = 0
        for (row in pending) {
            val stamp = syncStamp(row)
            val title = brainTitle(target, row)
            val text = row.toBrainText()
            val previousTitle = uploadedTitles[row.id]
            if (previousTitle != null && previousTitle != title) {
                try {
                    val forgetResponse = api.forget(auth, previousTitle)
                    check(forgetResponse.ok) {
                        "Second Brain hat die alte Titel-Loeschung nicht bestaetigt"
                    }
                    Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetOldTitle area=${target.area.key} expected=deleted actual=done ok=true id=${row.id} old=\"$previousTitle\" new=\"$title\"")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isRetryable(e)) {
                        Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetOldTitle area=${target.area.key} expected=deleted actual=error_retryable ok=false id=${row.id} old=\"$previousTitle\" message=${e.message ?: e::class.java.simpleName}")
                        _state.value = _state.value.copy(
                            syncing = false,
                            lastMessage =
                                "Alter Second-Brain-Titel konnte nicht geloescht werden: " +
                                    (e.message ?: e::class.java.simpleName),
                            syncedCount = synced,
                        )
                        return true
                    }
                    // nicht-retrybar → Zeile ueberspringen, restliche weiter verarbeiten.
                    rowFailures++
                    Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=forgetOldTitle area=${target.area.key} expected=deleted actual=non_retryable_skipped ok=false id=${row.id} old=\"$previousTitle\" message=${e.message ?: e::class.java.simpleName}")
                    continue
                }
            }
            val request = SecondBrainStoreRequest(title = title, category = target.area.category, text = text)
            try {
                val response = api.store(
                    authorization = auth,
                    idempotencyKey = "entropy-${target.area.key}-${row.id}-$stamp",
                    request = request,
                )
                if (!response.ok) {
                    // ok=false ist nicht retrybar → Gift-Zeile ueberspringen (kein Head-of-line-Blocking).
                    rowFailures++
                    Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=storeResponse area=${target.area.key} expected=ok actual=ok_false_skipped ok=false id=${row.id} title=\"$title\"")
                    continue
                }
                settings.markSecondBrainSynced(target.area.key, row.id, stamp, title)
                knownStamps.removeAll { it.startsWith("${row.id}:") }
                knownStamps += stamp
                synced++
                Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=storeResponse area=${target.area.key} expected=ok actual=ok=${response.ok},docId=${response.docId ?: "leer"},replaced=${response.replaced} ok=${response.ok} id=${row.id}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: e::class.java.simpleName
                if (isRetryable(e)) {
                    Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=storeResponse area=${target.area.key} expected=ok actual=error_retryable ok=false id=${row.id} title=\"$title\" synced_before_error=$synced message=$msg", e)
                    _state.value = _state.value.copy(syncing = false, lastMessage = "Sync gestoppt bei '$title': $msg", syncedCount = synced)
                    return true
                }
                // nicht-retrybar (400/413/422 …) → Zeile ueberspringen, weiter.
                rowFailures++
                Diag.e(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=storeResponse area=${target.area.key} expected=ok actual=non_retryable_skipped ok=false id=${row.id} title=\"$title\" message=$msg", e)
                continue
            }
        }
        _state.value = _state.value.copy(
            syncing = false,
            lastMessage = buildString {
                append("$reason abgeschlossen: $synced gespeichert")
                if (deleted > 0) append(", $deleted gelöscht")
                if (rowFailures > 0) append(", $rowFailures fehlgeschlagen")
                if (deletionFailed) append(", mind. eine Löschung fehlgeschlagen")
                append(".")
            },
            syncedCount = synced,
            lastSyncedAtMs =
                if (deletionFailed) _state.value.lastSyncedAtMs else System.currentTimeMillis(),
        )
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=syncComplete area=${target.area.key} expected=all_pending_synced actual=synced=$synced,failed=$rowFailures,deleted=$deleted,pending=${pending.size} ok=${rowFailures == 0 && synced == pending.size} reason=$reason")
        return deletionNetworkError
    }

    private suspend fun syncRowsWithRetry(
        target: SecondBrainSyncTarget,
        initialRows: List<SecondBrainSyncRow>,
        reason: String,
    ) {
        // Fix 2026-07-14 (#13a): Erreichbarkeits-Kurzcheck VOR dem Mutex. Ohne Tunnel wuerde sonst
        // jeder onStart/onStop-Lauf 4×(Timeout+delay) unter Lock haengen und die Kette blockieren.
        if (!isBrainReachable()) {
            _state.value = _state.value.copy(syncing = false, lastMessage = "Server nicht erreichbar (VPN?).")
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=syncUnreachable area=${target.area.key} expected=reachable actual=unreachable ok=false reason=$reason")
            return
        }
        var rows = initialRows
        repeat(PUSH_RETRIES) { attempt ->
            // Fix 2026-07-14 (#13b): Mutex nur um den eigentlichen Sync — delay/loadRows laufen
            // ausserhalb, damit die Retry-Kette den Lock nicht ueber die Wartezeit haelt.
            val hadNetworkError = operationMutex.withLock { syncRowsUnlocked(target, rows, reason) }
            if (!hadNetworkError) return
            if (attempt < PUSH_RETRIES - 1) {
                delay(PUSH_RETRY_DELAY_MS)
                rows = target.loadRows() ?: rows
            }
        }
    }

    /** Kurzer Erreichbarkeits-Check (~3s) — verhindert lange Timeout-Ketten ohne VPN/Tunnel. */
    private suspend fun isBrainReachable(): Boolean {
        val key = secrets.secondBrainApiKey.orEmpty().trim()
        if (key.isBlank()) return true // fehlenden Key meldet syncRowsUnlocked selbst
        return try {
            quickApi.health("Bearer $key")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=reachabilityCheck expected=reachable actual=unreachable ok=false message=${e.message ?: e::class.java.simpleName}")
            false
        }
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
            // OrNull-Flow (#3): bei kaputtem JSON emittiert die Quelle null → Sync ueberspringt.
            observeRows = { tagebuchEntriesFlowOrNull(appContext, TagebuchArea.ENTROPY).mapRowsOrNull { rows -> rows.map { it.toEntropySyncRow() } } },
            loadRows = { tagebuchEntriesFlowOrNull(appContext, TagebuchArea.ENTROPY).first()?.map { it.toEntropySyncRow() } },
            insertFromBrain = { item -> insertEntropyJournalFromBrain(item) },
            deleteById = { id -> deleteTagebuchEntry(appContext, id, propagate = false) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_LEARNING,
            observeRows = { tagebuchEntriesFlowOrNull(appContext, TagebuchArea.LEARNING).mapRowsOrNull { rows -> rows.map { it.toLearningSyncRow() } } },
            loadRows = { tagebuchEntriesFlowOrNull(appContext, TagebuchArea.LEARNING).first()?.map { it.toLearningSyncRow() } },
            insertFromBrain = { item -> insertLearningFromBrain(item) },
            deleteById = { id -> deleteTagebuchEntry(appContext, id, propagate = false) },
        ),
        SecondBrainSyncTarget(
            area = AREAS_THESES,
            observeRows = { thesenEntriesFlowOrNull(appContext).mapRowsOrNull { rows -> rows.map { it.toSyncRow() } } },
            loadRows = { thesenEntriesFlowOrNull(appContext).first()?.map { it.toSyncRow() } },
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
        val ideaText = preferredSecondBrainText(row.idea.text, row.idea.improvedText)
        SecondBrainSyncRow(
            id = row.idea.id,
            createdAtMs = row.idea.timestampMs,
            updatedAtMs = maxOf(row.idea.updatedAt ?: row.idea.timestampMs, row.followups.maxOfOrNull { it.createdAtMs } ?: 0L),
            title = row.idea.title.trim().ifBlank { "Idee ${row.idea.id.take(8)}" },
            bodyLabel = "Idee",
            body = ideaText,
            summary = row.idea.summary?.takeIf { it.isNotBlank() }?.trim(),
            contentRevision = ideaText.hashCode(),
            // Fix 2026-07-14 (#11): Nachtraege mit ins Second Brain — frueher triggerte ein Nachtrag
            // nur einen Re-Upload, ohne selbst im Brain-Text zu landen.
            followups = row.followups.sortedBy { it.createdAtMs }
                .map { preferredSecondBrainText(it.text, it.improvedText) }
                .filter { it.isNotBlank() },
        )
    }

    private suspend fun insertIdeaFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val title = item.title?.trim().orEmpty()
        val ts = parseIsoToMs(item.createdAt)
        val newId = item.appRowId ?: UUID.randomUUID().toString()
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

    private suspend fun insertHabitFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow? {
        // Fix 2026-07-14 (#9): Blank-Check wie bei der Mental-Variante — kein leerer Import.
        val text = item.text.trim()
        if (text.isBlank()) return null
        val ts = parseIsoToMs(item.createdAt)
        val nextPosition = habitDao.maxPosition() + 1
        val entity = HabitEntity(
            id = item.appRowId ?: UUID.randomUUID().toString(),
            text = text,
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
            id = item.appRowId ?: UUID.randomUUID().toString(),
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
            id = item.appRowId ?: UUID.randomUUID().toString(),
            timestampMs = ts,
            title = title,
            updatedAt = ts,
            text = text,
        )
        addTagebuchEntry(appContext, entry)
        return entry.toEntropySyncRow()
    }

    private fun TagebuchEntry.toLearningSyncRow(): SecondBrainSyncRow {
        val learningText = preferredSecondBrainText(text, improvedText)
        return SecondBrainSyncRow(
            id = id,
            createdAtMs = timestampMs,
            updatedAtMs = updatedAt.takeIf { it > 0L } ?: timestampMs,
            title = title.trim().ifBlank { text.trim().shortTitle("Lernen", id) },
            bodyLabel = "Lernen",
            body = learningText,
            summary = summary?.takeIf { it.isNotBlank() }?.trim(),
            contentRevision = learningText.hashCode(),
        )
    }

    private suspend fun insertLearningFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val parsedTitle = parseLearningBrainTitle(item.title)
        val id = item.appRowId ?: parsedTitle.rowId ?: UUID.randomUUID().toString()
        val title = parsedTitle.displayTitle.ifBlank { "Lernen ${formatTs(ts)}" }
        val entry = TagebuchEntry(
            id = id,
            timestampMs = ts,
            title = title,
            updatedAt = ts,
            text = item.text.trim(),
            area = TagebuchArea.LEARNING,
        )
        deleteTagebuchEntry(appContext, id, propagate = false)
        addTagebuchEntry(appContext, entry)
        return entry.toLearningSyncRow()
    }

    private fun ThesenEntry.toSyncRow(): SecondBrainSyncRow = SecondBrainSyncRow(
        id = id,
        createdAtMs = timestampMs,
        updatedAtMs = maxOf(updatedAt.takeIf { it > 0L } ?: timestampMs, followups.maxOfOrNull { it.createdAtMs } ?: 0L),
        title = title.trim().ifBlank { text.trim().shortTitle("These", id) },
        bodyLabel = "These",
        body = improvedText?.takeIf { it.isNotBlank() }?.trim() ?: text.trim(),
        summary = summary?.takeIf { it.isNotBlank() }?.trim(),
        // Fix 2026-07-14 (#11): Nachtraege mit ins Second Brain aufnehmen.
        followups = followups.sortedBy { it.createdAtMs }
            .map { preferredSecondBrainText(it.text, it.improvedText) }
            .filter { it.isNotBlank() },
    )

    private suspend fun insertThesisFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val title = item.title?.trim().orEmpty().ifBlank { "These ${formatTs(ts)}" }
        val entry = ThesenEntry.create(item.text.trim()).copy(
            id = item.appRowId ?: UUID.randomUUID().toString(),
            timestampMs = ts,
            title = title,
            updatedAt = ts,
        )
        addThesenEntry(appContext, entry)
        return entry.toSyncRow()
    }

    private suspend fun insertJournalFromBrain(item: SecondBrainCategoryItem): SecondBrainSyncRow {
        val ts = parseIsoToMs(item.createdAt)
        val stableRemoteId = item.appRowId?.toLongOrNull()
            ?: stableNegativeId(item.docId ?: item.title.orEmpty())
        val entity = JournalMirrorEntryEntity(
            sourceId = stableRemoteId,
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

    private fun stableNegativeId(value: String): Long {
        val hash = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val positive = ByteBuffer.wrap(hash).long and Long.MAX_VALUE
        return -positive.coerceAtLeast(1L)
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

    private fun syncStamp(row: SecondBrainSyncRow): String = secondBrainSyncStamp(row)

    private fun brainTitle(target: SecondBrainSyncTarget, row: SecondBrainSyncRow): String {
        val displayTitle = row.title.trim().ifBlank { "${row.bodyLabel} ${row.id.take(8)}" }
        return appBrainTitle(displayTitle, target.area.key, row.id)
    }

    private fun SecondBrainSyncRow.toBrainText(): String = buildString {
        appendLine("Erstellt am: ${formatTs(createdAtMs)}")
        appendLine("Aktualisiert am: ${formatTs(updatedAtMs)}")
        appendLine()
        appendLine("$bodyLabel: ${body.trim()}")
        // Fix 2026-07-14 (#11): Nachtraege konsistent zum Eintrags-Format in den Body aufnehmen.
        if (followups.isNotEmpty()) {
            appendLine()
            appendLine("Nachträge:")
            followups.forEach { followup ->
                if (followup.isNotBlank()) appendLine("- ${followup.trim()}")
            }
        }
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

    /** Wie [mapRows], aber null (Parse-/Lesefehler) bleibt null — der Sync ueberspringt dann (#3). */
    private fun <T> Flow<List<T>?>.mapRowsOrNull(
        mapper: suspend (List<T>) -> List<SecondBrainSyncRow>,
    ): Flow<List<SecondBrainSyncRow>?> = map { list -> list?.let { mapper(it) } }

    private fun isRetryable(error: Exception): Boolean =
        error is IOException || (error is HttpException && (error.code() == 429 || error.code() in 500..599))

    private companion object {
        const val TAG = "SecondBrainSync"
        val AREAS_IDEAS = SecondBrainArea("ideas", "Ideen", "Ideen")
        val AREAS_HABITS = SecondBrainArea("habits", "Gewohnheiten", "Gewohnheiten")
        val AREAS_MENTAL = SecondBrainArea("mental", "Mental", "Mental")
        val AREAS_ENTROPY = SecondBrainArea("entropy", "Entropie", "Entropie")
        val AREAS_LEARNING = SecondBrainArea("learning", "Lernen", "Lernen")
        val AREAS_THESES = SecondBrainArea("theses", "Thesen", "Thesen")
        val AREAS_JOURNAL = SecondBrainArea("journal", "Tagebucheinträge", "Tagebucheinträge")
        const val PUSH_RETRIES = 4
        const val PUSH_RETRY_DELAY_MS = 3000L
        val DATE_TEXT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)
    }
}

internal fun learningBrainTitle(displayTitle: String, rowId: String): String =
    "$displayTitle$LEARNING_TITLE_MARKER$rowId"

internal fun appBrainTitle(displayTitle: String, areaKey: String, rowId: String): String =
    "$displayTitle$APP_TITLE_MARKER$areaKey · $rowId"

internal fun parseAppBrainTitle(title: String?, expectedAreaKey: String): LearningBrainTitleParts {
    val clean = title?.trim().orEmpty()
    val markerIndex = clean.lastIndexOf(APP_TITLE_MARKER)
    if (markerIndex >= 0) {
        val suffix = clean.substring(markerIndex + APP_TITLE_MARKER.length)
        val separator = suffix.indexOf(" · ")
        if (separator > 0 && suffix.substring(0, separator) == expectedAreaKey) {
            val candidateId = suffix.substring(separator + 3).trim()
            if (candidateId.isNotBlank()) {
                return LearningBrainTitleParts(clean.substring(0, markerIndex).trim(), candidateId)
            }
        }
    }
    return if (expectedAreaKey == "learning") parseLearningBrainTitle(clean)
    else LearningBrainTitleParts(clean, null)
}

internal data class LearningBrainTitleParts(
    val displayTitle: String,
    val rowId: String?,
)

internal fun parseLearningBrainTitle(title: String?): LearningBrainTitleParts {
    val clean = title?.trim().orEmpty()
    val markerIndex = clean.lastIndexOf(LEARNING_TITLE_MARKER)
    if (markerIndex < 0) return LearningBrainTitleParts(clean, null)
    val candidateId = clean.substring(markerIndex + LEARNING_TITLE_MARKER.length).trim()
    val validId = runCatching { UUID.fromString(candidateId).toString() }.getOrNull()
        ?: return LearningBrainTitleParts(clean, null)
    return LearningBrainTitleParts(clean.substring(0, markerIndex).trim(), validId)
}

private const val LEARNING_TITLE_MARKER = " · Lernen · "
private const val APP_TITLE_MARKER = " · EntropieReductor:"

/** Fuer Lernen und Ideen verlaesst nur die beste vorhandene Textfassung das Handy. */
internal fun preferredSecondBrainText(originalText: String, improvedText: String?): String =
    improvedText?.takeIf { it.isNotBlank() }?.trim() ?: originalText.trim()

internal fun secondBrainSyncStamp(row: SecondBrainSyncRow): String = buildString {
    append(row.id)
    append(':')
    append(maxOf(row.createdAtMs, row.updatedAtMs))
    row.contentRevision?.let {
        append(':')
        append(it)
    }
    // Fix 2026-07-14 (#11): Followups in den Stamp — nur wenn vorhanden, damit Eintraege OHNE
    // Nachtraege denselben Stamp wie bisher behalten (kein unnoetiger Massen-Re-Upload).
    if (row.followups.isNotEmpty()) {
        append(':')
        append(row.followups.joinToString("\u0001"))
    }
    append(':')
    val digest = MessageDigest.getInstance("SHA-256").digest(
        listOf(row.title, row.bodyLabel, row.body, row.summary.orEmpty()).joinToString("\u0000").toByteArray(Charsets.UTF_8),
    )
    append(digest.joinToString("") { "%02x".format(it) })
}
