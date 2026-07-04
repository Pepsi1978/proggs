package de.frank.entropyreducer.data.remote.brain

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.IdeaDao
import de.frank.entropyreducer.data.local.dao.IdeaWithFollowups
import de.frank.entropyreducer.data.local.entities.IdeaEntity
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    /**
     * Frank-Wunsch 2026-07-04 (Fix „Idee bleibt liegen"): Schiebt ausstehende (noch nicht
     * hochgeladene) Ideen aktiv nach — mit Retry gegen einen wackligen WireGuard-Tunnel, genau wie
     * [pullFromBrain] es beim Runterladen tut. Wird beim App-Vordergrund (MainActivity.onStart) und
     * beim Betreten des Ideen-Reiters aufgerufen.
     *
     * Root Cause davor: Der Push haengt sonst allein am Ideen-Flow-Observer in [start], der bei
     * unveraenderter Liste nicht neu feuert. Schlug der erste Upload unterwegs (ohne Netz) fehl,
     * blieb die Idee liegen, bis die App als Prozess frisch startete ODER sich eine Idee aenderte —
     * genau der beobachtete „ich oeffne die App und es passiert nichts"-Fall. Zusaetzlich hatte der
     * Push (anders als der Pull) keinen Retry gegen den intermittierenden Tunnel.
     *
     * Idempotent (Sync-Stamp + idempotencyKey): gefahrlos bei jedem Vordergrund aufrufbar; ist
     * nichts offen, kehrt [syncRows] sofort zurueck.
     */
    fun retryPendingUploads(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            if (!settings.secondBrainIdeasConnectorEnabledFlow.first()) return@launch
            if (secrets.secondBrainApiKey.orEmpty().isBlank()) return@launch
            repeat(PUSH_RETRIES) { attempt ->
                val rows = ideaDao.getAllIdeasForBackup().map { idea ->
                    IdeaWithFollowups(idea, ideaDao.getFollowupsForIdea(idea.id))
                }
                val hadNetworkError = syncRows(rows, reason = "Nachhol-Upload")
                if (!hadNetworkError) return@launch
                if (attempt < PUSH_RETRIES - 1) delay(PUSH_RETRY_DELAY_MS)
            }
        }
    }

    /**
     * Frank-Wunsch 2026-07-04: Vollstaendiger Neu-Sync. Leert die Brain-Kategorie „Ideen" komplett
     * (alte Formate + Karteileichen bereits geloeschter Ideen), setzt die lokalen Sync-Marken zurueck
     * und laedt danach ALLE aktuellen Ideen frisch im neuen Format hoch. Die App-Ideen bleiben
     * unberuehrt (das Handy ist die Quelle der Wahrheit) — es wird nur das Brain neu geschrieben.
     */
    fun resyncAll(scope: CoroutineScope) {
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=resyncRequested expected=resync_start actual=resync_start ok=true")
        scope.launch(Dispatchers.IO) {
            val key = secrets.secondBrainApiKey.orEmpty().trim()
            if (key.isBlank()) {
                _state.value = _state.value.copy(lastMessage = "Second-Brain-API-Key fehlt.")
                return@launch
            }
            val auth = "Bearer $key"
            _state.value = _state.value.copy(syncing = true, lastMessage = "Bereinige Second Brain …")
            var removed = 0
            try {
                val existing = api.byCategory(auth, "Ideen")
                for (item in existing.items) {
                    val t = item.title ?: continue
                    try {
                        api.forget(auth, t)
                        removed++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Diag.w(
                            DiagnosticArea.SECOND_BRAIN,
                            TAG,
                            "CHECKPOINT step=resyncForget actual=error ok=false title=\"$t\" message=${e.message ?: e::class.java.simpleName}",
                        )
                    }
                }
                Diag.i(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=resyncCleared expected=category_emptied actual=removed=$removed ok=true",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(syncing = false, lastMessage = "Bereinigung fehlgeschlagen: ${e.message}")
                Diag.e(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=resyncCleared actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
                    e,
                )
                return@launch
            }
            // Lokale Marken zuruecksetzen -> jede Idee gilt als „neu" und wird frisch hochgeladen.
            settings.clearAllSecondBrainIdeaSync()
            val rows = ideaDao.getAllIdeasForBackup().map { idea ->
                IdeaWithFollowups(idea, ideaDao.getFollowupsForIdea(idea.id))
            }
            syncRows(rows, reason = "Vollstaendiger Neu-Sync")
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

    /**
     * Frank-Wunsch 2026-07-04 (Etappe 2, Brain->App): Holt Ideen, die im Second Brain (Kategorie
     * „Ideen") NEU angelegt wurden, in die App. Abgleich per Titel; in der App geloeschte Ideen
     * kommen NICHT zurueck (bekannte Titel = Tombstone). Die importierte Idee bekommt den
     * Brain-Zeitstempel und nur den Text (keine verbesserte Version) — die kann in der App ergaenzt
     * werden, wodurch sie automatisch wieder ins Brain hochgeladen wird (App->Brain).
     * Mit Retry, weil der WireGuard-Tunnel nach dem App-Start ein paar Sekunden braucht.
     */
    fun pullFromBrain(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            repeat(4) {
                if (pullOnce()) return@launch
                delay(3000)
            }
        }
    }

    /** Ein Pull-Versuch. true = Brain erreichbar (fertig), false = Netzfehler (Tunnel evtl. noch nicht oben -> Retry). */
    private suspend fun pullOnce(): Boolean {
        if (!settings.secondBrainIdeasConnectorEnabledFlow.first()) return true
        val key = secrets.secondBrainApiKey.orEmpty().trim()
        if (key.isBlank()) return true
        val auth = "Bearer $key"
        val brain = try {
            api.byCategory(auth, "Ideen")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Diag.w(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=pullUnreachable actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
            )
            return false
        }
        val brainTitles = brain.items.mapNotNull { it.title?.trim() }.filter { it.isNotEmpty() }.toSet()
        val appIdeas = ideaDao.getAllIdeasForBackup()
        val appTitles = appIdeas.map { it.title.trim() }.toSet()
        val uploadedMap = settings.readSecondBrainIdeaTitles()
        val knownTitles = uploadedMap.values.map { it.trim() }.toSet()
        var imported = 0
        for (item in brain.items) {
            val title = item.title?.trim().orEmpty()
            if (title.isBlank()) continue
            if (title in appTitles) continue      // schon in der App
            if (title in knownTitles) continue     // Tombstone: in der App geloescht -> nicht zurueckholen
            val ts = parseIsoToMs(item.createdAt)
            val newId = UUID.randomUUID().toString()
            // Reihenfolge: erst als „synchron" markieren, DANN anlegen — sonst wuerde der
            // App->Brain-Flow die frische Idee sofort im App-Format neu hochladen (Ping-Pong).
            settings.markSecondBrainIdeaSynced(newId, "$newId:$ts")
            settings.setSecondBrainIdeaTitle(newId, title)
            ideaDao.upsertIdea(
                IdeaEntity(
                    id = newId,
                    timestampMs = ts,
                    title = title,
                    text = item.text.trim(),
                    summary = null,
                    improvedText = null,
                    isImproved = false,
                    updatedAt = null,
                ),
            )
            imported++
            Diag.i(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=pullImported expected=idea_created actual=created ok=true title=\"$title\" ts=$ts id=$newId",
            )
        }
        // Loeschung Brain->App (Frank-Wunsch 2026-07-04): App-Ideen, die vom Brain kamen (Titel in
        // uploadedMap = synchronisiert) UND in der App existieren, aber im Brain NICHT mehr vorhanden
        // sind -> wurden im Brain geloescht -> auch in der App entfernen. SICHERUNG (Ladefenster-Schutz,
        // brain-api §1.15): NUR loeschen, wenn das Brain sicher vollstaendig geladen ist (health.ready) —
        // sonst wuerde eine kurz unvollstaendige Server-Antwort App-Ideen faelschlich loeschen.
        var deletedInApp = 0
        val ready = try {
            api.health(auth).ready
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        if (ready) {
            for (idea in appIdeas) {
                val syncedTitle = (uploadedMap[idea.id] ?: continue).trim()
                if (syncedTitle.isNotEmpty() && syncedTitle !in brainTitles) {
                    ideaDao.deleteIdeaById(idea.id)
                    settings.clearSecondBrainIdeaSync(idea.id)
                    deletedInApp++
                    Diag.i(
                        DiagnosticArea.SECOND_BRAIN,
                        TAG,
                        "CHECKPOINT step=pullDeletedInApp expected=idea_removed actual=removed ok=true title=\"$syncedTitle\" id=${idea.id}",
                    )
                }
            }
        }
        Diag.i(
            DiagnosticArea.SECOND_BRAIN,
            TAG,
            "CHECKPOINT step=pullComplete expected=brain_scanned actual=brain=${brain.items.size},imported=$imported,deletedInApp=$deletedInApp,ready=$ready ok=true",
        )
        if (imported > 0 || deletedInApp > 0) {
            _state.value = _state.value.copy(
                lastMessage = "Second Brain synchronisiert: $imported neu, $deletedInApp entfernt.",
                lastSyncedAtMs = System.currentTimeMillis(),
            )
        }
        return true
    }

    /** ISO-8601-Zeitstempel des Brains -> Millis; robust gegen die gaengigen Formate. */
    private fun parseIsoToMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(iso).toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
    }

    /**
     * Laedt ausstehende/geaenderte Ideen hoch und entfernt in der App geloeschte per Titel.
     * @return true, wenn ein NETZFEHLER einen Upload ODER eine Loeschung (forget) abbrach (Retry
     *         sinnvoll) — false, wenn fertig, nichts zu tun war oder nur ein nicht-netzbedingter
     *         Grund (fehlender Key) vorlag.
     *         Bestehende Aufrufer (Flow-Observer, syncAllNow, resyncAll) ignorieren den Rueckgabewert.
     */
    private suspend fun syncRows(rows: List<IdeaWithFollowups>, reason: String): Boolean {
        val key = secrets.secondBrainApiKey.orEmpty().trim()
        if (key.isBlank()) {
            Diag.w(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=syncPreflight expected=api_key_present actual=missing ok=false reason=$reason rows=${rows.size}",
            )
            _state.value = _state.value.copy(lastMessage = "Connector aktiv, aber API-Key fehlt.")
            return false
        }
        val auth = "Bearer $key"
        val sorted = rows.sortedBy { it.idea.timestampMs }
        val currentIds = sorted.map { it.idea.id }.toSet()
        val uploadedTitles = settings.readSecondBrainIdeaTitles()

        // 1) Loeschungen (Frank-Wunsch 2026-07-04): Ideen, die frueher hochgeladen wurden, jetzt aber
        //    nicht mehr existieren (in der App geloescht) -> per Titel aus dem Brain entfernen.
        val deletedIds = uploadedTitles.keys - currentIds
        var deleted = 0
        // Fix 2026-07-04: Auch eine fehlgeschlagene Loeschung (forget) soll den Retry ausloesen —
        // sonst wuerde eine offline geloeschte Idee erst beim naechsten App-Start (statt sofort per
        // Retry) aus dem Brain verschwinden. true, sobald ein forget an einem Netzfehler scheitert.
        var deletionNetworkError = false
        for (id in deletedIds) {
            val title = uploadedTitles[id] ?: continue
            try {
                val resp = api.forget(auth, title)
                settings.clearSecondBrainIdeaSync(id)
                deleted++
                Diag.i(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=forgetIdea expected=deleted actual=deleted=${resp.deleted} ok=${resp.ok} id=$id title=\"$title\"",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deletionNetworkError = true
                Diag.e(
                    DiagnosticArea.SECOND_BRAIN,
                    TAG,
                    "CHECKPOINT step=forgetIdea expected=deleted actual=error ok=false id=$id title=\"$title\" message=${e.message ?: e::class.java.simpleName}",
                    e,
                )
            }
        }

        // 2) Uploads: neue/geaenderte Ideen (per Sync-Stamp) hochladen.
        val knownStamps = settings.readSecondBrainIdeaSyncStamps().toMutableSet()
        val pending = sorted.filter { row -> syncStamp(row) !in knownStamps }
        Diag.i(
            DiagnosticArea.SECOND_BRAIN,
            TAG,
            "CHECKPOINT step=syncPreflight expected=pending_calculated actual=total=${sorted.size},pending=${pending.size},deleted=$deleted,known=${knownStamps.size} ok=true reason=$reason",
        )
        if (pending.isEmpty()) {
            _state.value = _state.value.copy(
                lastMessage = if (deleted > 0) "$deleted Idee(n) im Second Brain geloescht, Rest aktuell."
                else "Alle Ideen sind im Second Brain aktuell.",
            )
            return deletionNetworkError
        }

        _state.value = _state.value.copy(syncing = true, lastMessage = "$reason: ${pending.size} Idee(n) offen.")
        var synced = 0
        for (row in pending) {
            val stamp = syncStamp(row)
            val title = brainTitle(row)
            val text = row.toBrainText()
            // Titel-Aenderung: alten Brain-Eintrag (anderer Titel) zuerst entfernen, sonst Leiche.
            val previousTitle = uploadedTitles[row.idea.id]
            if (previousTitle != null && previousTitle != title) {
                try {
                    api.forget(auth, previousTitle)
                    Diag.i(
                        DiagnosticArea.SECOND_BRAIN,
                        TAG,
                        "CHECKPOINT step=forgetOldTitle expected=deleted actual=done ok=true id=${row.idea.id} old=\"$previousTitle\" new=\"$title\"",
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Diag.w(
                        DiagnosticArea.SECOND_BRAIN,
                        TAG,
                        "CHECKPOINT step=forgetOldTitle expected=deleted actual=error ok=false id=${row.idea.id} old=\"$previousTitle\" message=${e.message ?: e::class.java.simpleName}",
                    )
                }
            }
            val request = SecondBrainStoreRequest(
                title = title,
                category = "Ideen",
                text = text,
            )
            Diag.d(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=storeIdeaRequest id=${row.idea.id} title=\"$title\" chars=${text.length} stamp=$stamp",
            )
            try {
                val response = api.store(
                    authorization = auth,
                    idempotencyKey = "entropy-idea-${row.idea.id}-$stamp",
                    request = request,
                )
                settings.markSecondBrainIdeaSynced(row.idea.id, stamp)
                settings.setSecondBrainIdeaTitle(row.idea.id, title)
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
                return true
            }
        }
        _state.value = _state.value.copy(
            syncing = false,
            lastMessage = "$reason abgeschlossen: $synced gespeichert" + (if (deleted > 0) ", $deleted geloescht." else "."),
            syncedCount = synced,
            lastSyncedAtMs = System.currentTimeMillis(),
        )
        Diag.i(
            DiagnosticArea.SECOND_BRAIN,
            TAG,
            "CHECKPOINT step=syncComplete expected=all_pending_synced actual=synced=$synced,deleted=$deleted,pending=${pending.size} ok=${synced == pending.size} reason=$reason",
        )
        return deletionNetworkError
    }

    private fun syncStamp(row: IdeaWithFollowups): String {
        val ideaVersion = row.idea.updatedAt ?: row.idea.timestampMs
        val followupVersion = row.followups.maxOfOrNull { it.createdAtMs } ?: 0L
        return "${row.idea.id}:${maxOf(ideaVersion, followupVersion)}"
    }

    // Frank-Wunsch 2026-07-04: Brain-Titel = NUR der Ideen-Titel (z. B. "Frage Meditation ausprobieren"),
    // nicht mehr die interne ID/Datum. Leerer Titel -> kurzer Fallback, damit der Brain nie einen
    // leeren Titel-Schluessel bekommt.
    private fun brainTitle(row: IdeaWithFollowups): String =
        row.idea.title.trim().ifBlank { "Idee ${row.idea.id.take(8)}" }

    // Frank-Wunsch 2026-07-04: reiner, lesbarer Text ohne Rauten/Markup. Nur Erstell-/Aktualisierungs-
    // datum, dann "Idee:" (verbesserte Fassung, sonst Original) und — falls vorhanden — "Zusammenfassung:".
    // Bewusst KEINE Nachtraege (Followups) im Brain-Text.
    private fun IdeaWithFollowups.toBrainText(): String = buildString {
        appendLine("Erstellt am: ${formatTs(idea.timestampMs)}")
        appendLine("Aktualisiert am: ${formatTs(idea.updatedAt ?: idea.timestampMs)}")
        appendLine()
        val ideaText = idea.improvedText?.takeIf { it.isNotBlank() }?.trim() ?: idea.text.trim()
        appendLine("Idee: $ideaText")
        if (!idea.summary.isNullOrBlank()) {
            appendLine()
            appendLine("Zusammenfassung: ${idea.summary.trim()}")
        }
    }.trim()

    private fun formatTs(ms: Long): String =
        DATE_TEXT.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    private companion object {
        const val TAG = "SecondBrainIdeas"
        // Fix 2026-07-04: Retry-Kadenz fuer den Nachhol-Upload — analog zu pullFromBrain (4x, 3s),
        // faengt den intermittierenden WireGuard-Tunnel ab (mal erreichbar, mal 30s-Timeout).
        const val PUSH_RETRIES = 4
        const val PUSH_RETRY_DELAY_MS = 3000L
        val DATE_TEXT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)
    }
}
