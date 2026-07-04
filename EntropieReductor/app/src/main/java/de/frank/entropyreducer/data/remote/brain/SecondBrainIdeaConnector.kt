package de.frank.entropyreducer.data.remote.brain

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.IdeaDao
import de.frank.entropyreducer.data.local.dao.IdeaWithFollowups
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SecondBrainIdeaSyncState(
    val syncing: Boolean = false,
    val lastMessage: String = "Noch nicht synchronisiert.",
    val syncedCount: Int = 0,
    val lastSyncedAtMs: Long = 0L,
)

@Singleton
class SecondBrainIdeaConnector @Inject constructor(
    private val ideaDao: IdeaDao,
    private val settings: AppSettings,
    private val secrets: EncryptedSecretsStore,
    private val api: SecondBrainApi,
) {
    private val started = AtomicBoolean(false)
    private val _state = MutableStateFlow(SecondBrainIdeaSyncState())
    val state: StateFlow<SecondBrainIdeaSyncState> = _state.asStateFlow()

    fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=connectorStart expected=observer_registered actual=observer_registered ok=true")
        scope.launch(Dispatchers.IO) {
            combine(
                settings.secondBrainIdeasConnectorEnabledFlow,
                ideaDao.getAllIdeasWithFollowups(),
            ) { enabled, ideas -> enabled to ideas }
                .collect { (enabled, ideas) ->
                    if (enabled) {
                        Diag.d(
                            DiagnosticArea.SECOND_BRAIN,
                            TAG,
                            "CHECKPOINT step=observerEmission enabled=true ideas=${ideas.size}",
                        )
                        syncRows(ideas, reason = "Automatischer Ideen-Sync")
                    } else {
                        Diag.d(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=observerEmission enabled=false")
                    }
                }
        }
    }

    fun syncAllNow(scope: CoroutineScope) {
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=manualSyncRequested expected=sync_start actual=sync_start ok=true")
        scope.launch(Dispatchers.IO) {
            syncRows(ideaDao.getAllIdeasForBackup().map { idea ->
                IdeaWithFollowups(idea, ideaDao.getFollowupsForIdea(idea.id))
            }, reason = "Manueller Ideen-Sync")
        }
    }

    suspend fun testConnection(apiKeyOverride: String? = null): Boolean = withContext(Dispatchers.IO) {
        val key = apiKeyOverride?.trim().orEmpty().ifBlank { secrets.secondBrainApiKey.orEmpty() }
        if (key.isBlank()) {
            Diag.w(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=healthCheck expected=api_key_present actual=missing ok=false",
            )
            _state.value = _state.value.copy(lastMessage = "Second-Brain-API-Key fehlt.")
            return@withContext false
        }
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=healthCheck expected=request_sent actual=request_sent ok=true")
        try {
            val health = api.health("Bearer $key")
            val ok = health.ready || health.status == "ok"
            Diag.i(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=healthCheckResponse expected=reachable actual=status=${health.status},ready=${health.ready},version=${health.version ?: "unbekannt"} ok=$ok",
            )
            _state.value = _state.value.copy(
                lastMessage = if (health.ready) {
                    "Second Brain verbunden (${health.version ?: health.status})."
                } else {
                    "Second Brain antwortet, lädt aber noch (${health.status})."
                }
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Diag.e(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=healthCheckResponse expected=reachable actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
                e,
            )
            _state.value = _state.value.copy(lastMessage = "Second Brain nicht erreichbar: ${e.message}")
            false
        }
    }

    private suspend fun syncRows(rows: List<IdeaWithFollowups>, reason: String) {
        val key = secrets.secondBrainApiKey.orEmpty().trim()
        if (key.isBlank()) {
            Diag.w(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=syncPreflight expected=api_key_present actual=missing ok=false reason=$reason rows=${rows.size}",
            )
            _state.value = _state.value.copy(lastMessage = "Connector aktiv, aber API-Key fehlt.")
            return
        }
        val sorted = rows.sortedBy { it.idea.timestampMs }
        val knownStamps = settings.readSecondBrainIdeaSyncStamps().toMutableSet()
        val pending = sorted.filter { row -> syncStamp(row) !in knownStamps }
        Diag.i(
            DiagnosticArea.SECOND_BRAIN,
            TAG,
            "CHECKPOINT step=syncPreflight expected=pending_calculated actual=total=${sorted.size},pending=${pending.size},known=${knownStamps.size} ok=true reason=$reason",
        )
        if (pending.isEmpty()) {
            _state.value = _state.value.copy(lastMessage = "Alle Ideen sind im Second Brain aktuell.")
            return
        }

        _state.value = _state.value.copy(syncing = true, lastMessage = "$reason: ${pending.size} Idee(n) offen.")
        var synced = 0
        for (row in pending) {
            val stamp = syncStamp(row)
            val title = stableBrainTitle(row)
            val text = row.toBrainText()
            val request = SecondBrainStoreRequest(
                title = title,
                category = "Ideen",
                text = text,
            )
            Diag.d(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=storeIdeaRequest id=${row.idea.id} title=\"$title\" chars=${text.length} followups=${row.followups.size} stamp=$stamp",
            )
            try {
                val response = api.store(
                    authorization = "Bearer $key",
                    idempotencyKey = "entropy-idea-${row.idea.id}-$stamp",
                    request = request,
                )
                settings.markSecondBrainIdeaSynced(row.idea.id, stamp)
                knownStamps.removeAll { it.startsWith("${row.idea.id}:") }
                knownStamps += stamp
                synced++
                Diag.i(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=storeIdeaResponse expected=ok actual=ok=${response.ok},docId=${response.docId ?: "leer"},replaced=${response.replaced} ok=${response.ok} id=${row.idea.id}",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: e::class.java.simpleName
                Diag.e(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=storeIdeaResponse expected=ok actual=error ok=false id=${row.idea.id} title=\"$title\" synced_before_error=$synced message=$msg",
                    e,
                )
                _state.value = _state.value.copy(
                    syncing = false,
                    lastMessage = "Sync gestoppt bei '${row.idea.title}': $msg",
                    syncedCount = synced,
                )
                return
            }
        }
        _state.value = _state.value.copy(
            syncing = false,
            lastMessage = "$reason abgeschlossen: $synced Idee(n) gespeichert.",
            syncedCount = synced,
            lastSyncedAtMs = System.currentTimeMillis(),
        )
        Diag.i(
            DiagnosticArea.SECOND_BRAIN,
            TAG,
            "CHECKPOINT step=syncComplete expected=all_pending_synced actual=synced=$synced,pending=${pending.size} ok=${synced == pending.size} reason=$reason",
        )
    }

    private fun syncStamp(row: IdeaWithFollowups): String {
        val ideaVersion = row.idea.updatedAt ?: row.idea.timestampMs
        val followupVersion = row.followups.maxOfOrNull { it.createdAtMs } ?: 0L
        return "${row.idea.id}:${maxOf(ideaVersion, followupVersion)}"
    }

    private fun stableBrainTitle(row: IdeaWithFollowups): String {
        val created = DATE_TITLE.format(Instant.ofEpochMilli(row.idea.timestampMs).atZone(ZoneId.systemDefault()))
        return "EntropieReductor Idee $created ${row.idea.id.take(8)}"
    }

    private fun IdeaWithFollowups.toBrainText(): String = buildString {
        appendLine("# ${idea.title.ifBlank { "Idee" }}")
        appendLine()
        appendLine("Quelle: EntropieReductor / Aufgaben / Ideen")
        appendLine("Kategorie: Ideen")
        appendLine("ID: ${idea.id}")
        appendLine("Erstellt: ${formatTs(idea.timestampMs)}")
        appendLine("Aktualisiert: ${formatTs(idea.updatedAt ?: idea.timestampMs)}")
        appendLine()
        appendLine("## Text")
        appendLine(idea.text.trim())
        if (!idea.improvedText.isNullOrBlank()) {
            appendLine()
            appendLine("## Verbesserte Fassung")
            appendLine(idea.improvedText.trim())
        }
        if (!idea.summary.isNullOrBlank()) {
            appendLine()
            appendLine("## Zusammenfassung")
            appendLine(idea.summary.trim())
        }
        if (followups.isNotEmpty()) {
            appendLine()
            appendLine("## Nachträge")
            followups.sortedBy { it.createdAtMs }.forEachIndexed { index, followup ->
                appendLine("${index + 1}. ${formatTs(followup.createdAtMs)}")
                appendLine(followup.text.trim())
                if (!followup.improvedText.isNullOrBlank()) {
                    appendLine("Verbessert: ${followup.improvedText.trim()}")
                }
            }
        }
    }.trim()

    private fun formatTs(ms: Long): String =
        DATE_TEXT.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    private companion object {
        const val TAG = "SecondBrainIdeas"
        val DATE_TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm", Locale.GERMANY)
        val DATE_TEXT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)
    }
}
