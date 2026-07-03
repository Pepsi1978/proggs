package de.frank.entropyreducer.presentation.dashboard1

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.audio.AudioRecorder
import de.frank.entropyreducer.data.audio.RecordingService
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.KiQuestionRepository
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.domain.usecase.CalculateBucketsUseCase
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
import de.frank.entropyreducer.domain.usecase.TranscribeAudioUseCase
import de.frank.entropyreducer.presentation.components.MicState
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * State der Aufgaben-Ansicht. @Immutable garantiert Compose dass equals()
 * ausreichend ist — verhindert unnoetige Recompositions wenn der gleiche
 * State-Wert erneut uebergeben wird (Frank-Wunsch 2026-05-09 Performance).
 */
@androidx.compose.runtime.Immutable
data class TasksUiState(
    val entriesByBucket: Map<TimeBucket, List<EntropyEntryEntity>> = emptyMap(),
    val activeCategories: Set<EntropyCategory> = emptySet(),
    val statusPercent: Int = 50,
    val statusBreakdown: StatusBreakdown? = null,
    val openCount: Int = 0,
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val recentlyCreatedId: String? = null,
    val kiQuestion: KiQuestion? = null,
    /** Eintrag der aktuell im Detail-Bottom-Sheet angezeigt wird (null = geschlossen). */
    val detailEntry: EntropyEntryEntity? = null,
    /** Erledigt-Bucket — die letzten REDUZIERT/ARCHIVIERT-Eintraege, sortiert nach
     *  resolvedAt absteigend. Werden separat unter den aktiven Bucket-Sektionen
     *  als ausgegrauter Block angezeigt. */
    val resolvedEntries: List<EntropyEntryEntity> = emptyList(),
    /** Proaktiver Forscher: Eintrag für den gerade nach der Loesungsmethode
     *  gefragt wird. null = kein Dialog sichtbar (Frank-Wunsch 2026-05-08). */
    val pendingMethodFor: EntropyEntryEntity? = null,
    /** Status des Drive-Backup-Sync — fuer die kleine Backup-Statuszeile direkt
     *  unter dem Titel "Entropie Reduktor" (Frank-Wunsch 2026-05-09: "ich will
     *  sehen ob mein neuer Eintrag im Backup ist"). */
    val syncStatus: SyncStatus = SyncStatus.Idle,
    /** Zeitpunkt des letzten erfolgreichen Drive-Backup-Uploads (epoch ms).
     *  0L = noch kein Backup gelaufen / Backup nicht aktiv. */
    val lastBackupAtMs: Long = 0L,
    /** Ist Drive-Backup aktiviert? Wenn false wird die Statuszeile ausgeblendet. */
    val driveBackupEnabled: Boolean = false,
    /**
     * Status der automatischen Re-Bewertung aller offenen Aufgaben
     * (Frank-Wunsch 2026-05-09 nach Aenderung der priorityScore-Doktrin).
     * null = nicht aktiv. Pair = (fertig, gesamt) — zeigt einen kleinen
     * Banner unter dem Titel "X von Y Aufgaben neu bewertet".
     */
    val rescoreProgress: RescoreProgress? = null,
)

@androidx.compose.runtime.Immutable
data class RescoreProgress(
    val done: Int,
    val total: Int,
    val failed: Int = 0,
)

private data class UiOnlyState(
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val recentlyCreatedId: String? = null,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    application: Application,
    private val entries: EntryRepository,
    private val recorder: AudioRecorder,
    private val transcribe: TranscribeAudioUseCase,
    private val process: ProcessEntryUseCase,
    private val improveText: de.frank.entropyreducer.domain.usecase.ImproveTextUseCase,
    private val statusObserver: StatusObserver,
    private val kiQuestions: KiQuestionRepository,
    private val generateKiQuestion: de.frank.entropyreducer.domain.kiquestion.GenerateKiQuestionUseCase,
    private val memories: de.frank.entropyreducer.data.repository.MemoryRepository,
    @Suppress("unused") private val bucketingUseCase: CalculateBucketsUseCase,
    private val balanceBuckets: de.frank.entropyreducer.domain.usecase.BalanceBucketsUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val secrets: EncryptedSecretsStore,
    private val settings: de.frank.entropyreducer.data.settings.AppSettings,
    // Agentic Auto-Suggestion: Beim manuellen Refresh auch Aufgaben- und
    // Gewohnheitsvorschläge aus Ideen neu generieren (Frank-Wunsch 2026-06-18).
    private val generateSuggestions: de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase,
    // Frank-Wunsch 2026-06-19: fuer die Rueckwaerts-Propagierung der Prio einer Loop-Instanz
    // ins zugehoerige Loop-Template (Heute-Aenderung -> Loop-Bereich konsistent).
    private val recurringTemplates: RecurringTemplateRepository,
    // Frank-Wunsch 2026-06-19: Prioritaets-Gedaechtnis (lernt manuell gesetzte Prioritaeten,
    // damit die KI bei neuen, sehr aehnlichen Aufgaben dieselbe Prio vorschlaegt).
    private val priorityMemoryRepository:
        de.frank.entropyreducer.data.repository.PriorityMemoryRepository,
) : AndroidViewModel(application) {

    private val activeCategoriesFlow = MutableStateFlow<Set<EntropyCategory>>(emptySet())
    private val uiOnlyFlow = MutableStateFlow(UiOnlyState())
    private val detailEntryIdFlow = MutableStateFlow<String?>(null)
    private val pendingMethodForFlow = MutableStateFlow<EntropyEntryEntity?>(null)
    private val rescoreProgressFlow = MutableStateFlow<RescoreProgress?>(null)

    val state: StateFlow<TasksUiState> = combine(
        entries.getActive(),
        activeCategoriesFlow,
        uiOnlyFlow,
        combine(statusObserver.observe(), kiQuestions.currentQuestion, syncCoordinator.status) { b, q, s -> Triple(b, q, s) },
        combine(detailEntryIdFlow, pendingMethodForFlow, rescoreProgressFlow) { d, p, r -> Triple(d, p, r) },
    ) { list, cats, ui, statusTriple, detailTriple ->
        val breakdown = statusTriple.first
        val question = statusTriple.second
        val syncStatus = statusTriple.third
        val detailId = detailTriple.first
        val pendingMethod = detailTriple.second
        val rescore = detailTriple.third
        val filtered = if (cats.isEmpty()) list else list.filter { it.category in cats }
        // Aktive Eintraege (OFFEN + IN_ARBEIT) → Bucket-Gruppierung.
        // PERFORMANCE 2026-05-09: Sortierung nach priorityScore wird HIER vorberechnet
        // statt in der LazyColumn-Lambda. Verhindert dass `sortedByDescending` bei
        // jedem State-Update (Status-Tick alle 5 min, DB-Aenderung) neu laeuft —
        // das war eine der Hauptursachen fuer Scroll-Ruckler im Aufgaben-Bereich.
        val activeList = filtered.filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
        // Frank-Wunsch 2026-05-31: nach EFFEKTIVER Prioritaet sortieren
        // (manualPriorityScore ?: priorityScore). So wandert eine manuell auf 100%
        // gesetzte Aufgabe sofort nach oben — die Liste re-sortiert sich automatisch,
        // weil der DB-Flow nach dem Update neu emittiert.
        val grouped = activeList
            .groupBy { it.timeBucket }
            .mapValues { (_, entries) ->
                entries.sortedByDescending { it.manualPriorityScore ?: it.priorityScore }
            }
        // Erledigt-Bucket (REDUZIERT) — sortiert nach resolvedAt absteigend (neueste zuerst).
        val resolvedList = filtered
            .filter { it.status == EntryStatus.REDUZIERT }
            .sortedByDescending { it.resolvedAt ?: it.updatedAt }
            .take(20)
        val openCount = list.count { it.status == EntryStatus.OFFEN }
        val detail = detailId?.let { id -> list.firstOrNull { it.id == id } }
        TasksUiState(
            entriesByBucket = grouped,
            activeCategories = cats,
            statusPercent = breakdown.total,
            statusBreakdown = breakdown,
            openCount = openCount,
            micState = ui.micState,
            processingMessage = ui.processingMessage,
            errorMessage = ui.errorMessage,
            recentlyCreatedId = ui.recentlyCreatedId,
            kiQuestion = question,
            detailEntry = detail,
            resolvedEntries = resolvedList,
            pendingMethodFor = pendingMethod,
            syncStatus = syncStatus,
            // Live-Wert wenn gerade synchronisiert wurde (atEpochMs aus Synced),
            // sonst der zuletzt persistierte Wert aus den Secrets. SharedPreferences
            // ist nicht reaktiv, aber bei jedem syncStatus-Update wird der Block
            // neu ausgewertet — das reicht weil DriveBackupManager den Secrets-Wert
            // VOR dem Status-Update auf Synced setzt.
            lastBackupAtMs = (syncStatus as? SyncStatus.Synced)?.atEpochMs
                ?: secrets.driveLastBackupEpochMs,
            driveBackupEnabled = secrets.driveBackupEnabled && secrets.driveAccountEmail != null,
            rescoreProgress = rescore,
        )
    }
        // Performance-Fix Loop 3.1: Der combine-Block macht filter/groupBy/
        // mapValues/sortedByDescending PLUS drei synchrone EncryptedSharedPreferences-
        // Reads (secrets.driveLastBackupEpochMs, .driveBackupEnabled,
        // .driveAccountEmail). Ohne flowOn lief das auf Main bei jedem combine-Tick
        // (entries-Update, Status-Tick alle 5 Min, KI-Frage, Sync-Status).
        // EncryptedSharedPreferences cached die entschluesselten Werte intern,
        // aber jeder getString geht durch ConcurrentHashMap-Lookups + AES-SIV-
        // Key-Decode. Mehrfach pro combine-Tick auf Main = Tipp-Latenz.
        // .flowOn(Default) verlagert die Berechnung off-main, TasksUiState
        // (@Immutable) wird thread-safe an Main propagiert.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), TasksUiState())

    init {
        // KI-Frage des Moments dynamisch generieren beim ViewModel-Start
        // (Frank-Wunsch 2026-05-08: keine vordefinierten Standardfragen,
        // immer durch Gemini aus dem aktuellen Kontext gebildet).
        refreshKiQuestion()
        // Tag-Rollover: MORGEN-Eintraege die gestern gesetzt wurden werden heute
        // automatisch zu HEUTE (Frank-Wunsch 2026-05-09).
        rolloverManualBucketsForNewDay()
        // Auto-Verteilung: Wenn die KI alle Eintraege in HEUTE eingeordnet hat,
        // verteilen wir sie auf MORGEN/FREIBLOCK/SPAETER damit Frank ALLE
        // Aufgaben sieht — nicht nur die top 5 in HEUTE (Frank-Reklamation
        // 2026-05-09: 12 Aufgaben da, nur 5 sichtbar).
        autoBalanceBuckets()
        // Auto-Archivierung: erledigte Eintraege aelter als 14 Tage werden
        // archiviert und verschwinden aus der Aufgabenliste. Sie sind weiter
        // ueber den Settings-Archiv-Bereich erreichbar (Frank-Wunsch 2026-05-09).
        autoArchiveOldResolved()
        // Day-Change-Watcher: Prueft minuetlich ob die lokale Datums-Komponente
        // sich geaendert hat (Mitternacht in Lokalzeit ueberschritten). Wenn ja
        // → MORGEN-Eintraege werden zu HEUTE, autoBalance verteilt Ueberschuss.
        // Frank-Wunsch 2026-05-09: "ab 0 Uhr meiner Zeitzone hier sollen diese
        // Aufgaben dann in heute drin stehen". Laeuft im Hintergrund solange
        // der ViewModel lebt — bei App-Killing uebernimmt der init-Block beim
        // naechsten Oeffnen den Rollover.
        startDayChangeWatcher()
        // Auto-Re-Score (Frank-Wunsch 2026-05-09): wenn die priorityScore-
        // Doktrin sich geaendert hat (neue 5-Farben-Entropie-Reduktions-Skala),
        // werden alle offenen Eintraege EINMALIG mit der neuen Logik bewertet.
        // Marker liegt in EncryptedSecretsStore.lastRescoreVersionCode — sobald
        // er den Ziel-VersionCode erreicht hat, laeuft der Auto-Re-Score nicht
        // mehr. Frank kann jederzeit manuell rescoreAllOpenEntries() ausloesen.
        maybeAutoRescoreOnDoctrineChange()
        // Frank-Wunsch 2026-05-31: einmalige KI-Kuerzung aller alten, zu langen Titel
        // (>3 Woerter) auf max. 3 Woerter. Neue Aufgaben sind ohnehin schon begrenzt.
        maybeShortenLongTitles()
        // Auto-Refresh beim App-Start (Frank-Wunsch 2026-05-22): wenn Frank die App
        // oeffnet sollen Nachtraege/Zeitanpassungen/Prio gepueft werden. Throttle 6h
        // damit kurze Cold-Starts hintereinander keine Gemini-Quota verbrennen.
        maybeAutoRefreshOnStartup()
    }

    /**
     * Manueller Refresh-Trigger (Frank-Wunsch 2026-05-22): laeuft wenn Frank den
     * Refresh-Button oben im Aufgabenreiter tippt. Macht den gleichen Job wie der
     * App-Start-Refresh — Rollover, Bucket-Balance, Auto-Archiv und Rescore aller
     * offenen Aufgaben. Im Gegensatz zum Auto-Refresh hat dieser keinen Throttle.
     */
    fun refreshAll() {
        rolloverManualBucketsForNewDay()
        autoArchiveOldResolved()
        autoBalanceBuckets()
        rescoreAllOpenEntries()
        // Agentic Auto-Suggestion: Beim manuellen Refresh — EIN kombinierter Aufruf.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val context = getApplication<Application>()
                val ideas = de.frank.entropyreducer.presentation.ideen.ideenEntriesFlow(context).first()
                if (ideas.isEmpty()) return@launch

                val kiStore = context.kiTaskSuggestionStore
                val habitStore = context.gewohnheitSuggestionStore

                val taskProcessedIds = loadProcessedIds(kiStore, TASK_PROCESSED_KEY)
                val habitProcessedIds = loadProcessedIds(habitStore, HABIT_PROCESSED_KEY)
                val allProcessedIds = taskProcessedIds + habitProcessedIds

                val (result, updatedProcessedIds) = generateSuggestions
                    .generateSuggestions(ideas, allProcessedIds)
                    .getOrThrow()

                if (result.tasks.isNotEmpty()) {
                    // Ab ID-Architektur Etappe 2c in Room (task_suggestions) statt kiStore-JSON.
                    storeKiTaskSuggestions(context, result.tasks)
                }
                if (result.habits.isNotEmpty()) {
                    // Ab ID-Architektur Etappe 3c/3d in Room (habit_suggestions) mit Herkunft.
                    storeHabitSuggestions(context, result.habits)
                }
                saveProcessedIds(kiStore, TASK_PROCESSED_KEY, updatedProcessedIds)
                saveProcessedIds(habitStore, HABIT_PROCESSED_KEY, updatedProcessedIds)
            }
        }
    }

    /**
     * "Neue Aufgaben hinzufuegen?"-Button (Frank-Wunsch 2026-05-23).
     * HEUTE wird nicht mehr automatisch nachgefuellt — erst wenn ALLE
     * HEUTE-Aufgaben erledigt sind, blendet der TasksScreen diesen Button ein.
     * Beim Tippen holen wir die [count] hoechstpriorisierten offenen Aufgaben aus
     * den aelteren Bereichen (MORGEN/FREIBLOCK/SPAETER) nach HEUTE. Wir setzen
     * manualBucket=HEUTE damit der Eintrag dort sicher liegen bleibt (kein
     * Auto-Rebalance schiebt ihn weg, kein erneuter Rollover greift).
     */
    fun refillHeute(count: Int = 5) {
        viewModelScope.launch {
            val active = entries.getActive().first().filter {
                it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT
            }
            // Nur auffuellen wenn HEUTE wirklich leer ist (Button-Bedingung).
            val heuteOpen = active.count { it.timeBucket == TimeBucket.HEUTE }
            if (heuteOpen > 0) return@launch
            val candidates = active
                .filter { it.timeBucket != TimeBucket.HEUTE }
                .sortedByDescending { it.manualPriorityScore ?: it.priorityScore }
                .take(count)
            if (candidates.isEmpty()) return@launch
            val now = System.currentTimeMillis()
            candidates.forEach { e ->
                de.frank.entropyreducer.data.diagnostics.Diag.i(
                    de.frank.entropyreducer.data.diagnostics.DiagnosticArea.APP,
                    "TasksVM",
                    "REFILL-HEUTE '${e.title.take(24)}' ${e.timeBucket}->HEUTE (HEUTE war leer)",
                )
                entries.update(
                    e.copy(
                        manualBucket = TimeBucket.HEUTE,
                        manualBucketSetAt = now,
                        timeBucket = TimeBucket.HEUTE,
                        updatedAt = now,
                    ),
                )
            }
            // Restliche Buckets neu verteilen (MORGEN/FREIBLOCK/SPAETER), damit die
            // Caps stimmen nachdem die Eintraege HEUTE zugewiesen wurden.
            autoBalanceBuckets()
        }
    }

    /**
     * Throttled Auto-Refresh beim App-Start. Laeuft hoechstens einmal pro 6h
     * — Frank kann zwischendurch jederzeit manuell ueber den Refresh-Button
     * triggern.
     */
    private fun maybeAutoRefreshOnStartup() {
        val now = System.currentTimeMillis()
        val last = secrets.lastStartupRefreshAtMs
        val sixHoursMs = 6 * 60 * 60 * 1000L
        if (now - last < sixHoursMs) return
        if (secrets.geminiApiKey.isNullOrBlank()) return
        secrets.lastStartupRefreshAtMs = now
        // Buckets + Archiv laufen schon ueber init() — wir muessen nur den
        // Rescore-Lauf zusaetzlich anstossen.
        rescoreAllOpenEntries()
    }

    /**
     * Triggert beim ViewModel-Start einmalig die Re-Bewertung aller offenen
     * Aufgaben, wenn die priorityScore-Doktrin sich geaendert hat (Frank-Wunsch
     * 2026-05-09 — neue 5-Farben-Skala basiert auf Entropie-Reduktion).
     * Ueberspringt den Lauf still wenn:
     *   - keine offenen Aufgaben da
     *   - kein Gemini-Key gesetzt (wird beim naechsten Start mit Key getriggert)
     *   - lastRescoreVersionCode bereits >= Ziel-VersionCode
     */
    private fun maybeAutoRescoreOnDoctrineChange() {
        if (secrets.lastRescoreVersionCode >= RESCORE_DOCTRINE_VERSION) return
        if (secrets.geminiApiKey.isNullOrBlank()) return
        viewModelScope.launch {
            val openCount = entries.getActive().first()
                .count { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
            if (openCount == 0) {
                // Keine Eintraege da — Marker trotzdem setzen damit der Trigger
                // beim naechsten Start nicht erneut feuert.
                secrets.lastRescoreVersionCode = RESCORE_DOCTRINE_VERSION
                return@launch
            }
            Diag.i(DiagnosticArea.TASKS, TAG, "Auto-Rescore startet — $openCount offene Eintraege werden mit neuer Doktrin neu bewertet")
            rescoreAllOpenEntries(autoTriggered = true)
        }
    }

    /**
     * Frank-Wunsch 2026-05-31: einmalige KI-Kuerzung aller bestehenden Aufgaben-Titel
     * mit mehr als 3 Woertern. Neue Titel werden ohnehin schon beim Erfassen auf 3
     * Woerter begrenzt (ProcessEntryUseCase.limitToThreeWords) — diese Migration holt
     * die alten Aufgaben nach. Die KI fasst jeden Titel in max. 3 praegnanten Woertern
     * neu zusammen (Frank-Wahl: schoener als die mechanische Erste-3-Woerter-Kappung).
     *
     * Laeuft genau EINMAL pro Installation (Flag in AppSettings). Ueberspringt still wenn:
     *   - bereits gelaufen
     *   - kein Gemini-Key gesetzt (wird beim naechsten Start mit Key getriggert)
     *   - keine zu langen Titel vorhanden (Marker wird trotzdem gesetzt)
     * Ein fehlgeschlagener Titel bricht die Schleife NICHT ab — er bleibt unveraendert.
     * Das gemeinsame DB-Update sorgt dafuer dass die kurzen Titel automatisch auch im
     * Home-Screen-Widget erscheinen (1:1, Frank-Wunsch).
     */
    private fun maybeShortenLongTitles() {
        viewModelScope.launch {
            if (settings.isTitleShortenV1Done()) return@launch
            if (secrets.geminiApiKey.isNullOrBlank()) return@launch
            val targets = entries.getActive().first()
                .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
                .filter { wordCount(it.title) > 3 }
            if (targets.isEmpty()) {
                settings.setTitleShortenV1Done(true)
                return@launch
            }
            Diag.i(DiagnosticArea.TASKS, TAG, "Titel-Kuerzung startet — ${targets.size} lange Titel werden per KI gekuerzt")
            var done = 0
            var failed = 0
            for (entry in targets) {
                process.shortenTitle(entry).onSuccess { newTitle ->
                    if (newTitle.isNotBlank() && newTitle != entry.title) {
                        entries.update(entry.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
                    }
                    done++
                }.onFailure { ex ->
                    failed++
                    Diag.w(DiagnosticArea.TASKS, TAG, "Titel-Kuerzung fuer ${entry.id} fehlgeschlagen: ${ex.message}")
                }
                // Sanfte Pause zwischen Calls — schont die Gemini-Quota bei vielen Aufgaben.
                delay(200L)
            }
            // Marker setzen — auch wenn ein paar fehlgeschlagen sind (laeuft nicht ewig).
            settings.setTitleShortenV1Done(true)
            Diag.i(DiagnosticArea.TASKS, TAG, "Titel-Kuerzung fertig: $done erfolgreich, $failed fehlgeschlagen, total ${targets.size}")
            // Widget aktualisieren, damit die gekuerzten Titel sofort 1:1 erscheinen.
            runCatching {
                de.frank.entropyreducer.presentation.widget.WidgetUpdater.updateAll(getApplication())
            }
        }
    }

    /** Zaehlt die Woerter eines Titels (fuer die 3-Woerter-Regel). */
    private fun wordCount(title: String): Int =
        title.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    /**
     * Bewertet alle offenen Eintraege (OFFEN + IN_ARBEIT) mit der aktuellen
     * priorityScore-Doktrin neu. Sequentiell mit kurzer Pause zwischen den
     * Gemini-Calls damit kein Rate-Limit ausgeloest wird. Status-Updates landen
     * im rescoreProgressFlow → der TasksScreen zeigt einen Banner "X von Y
     * Aufgaben neu bewertet".
     *
     * Robustes Error-Handling: Ein fehlgeschlagener Eintrag bricht die Schleife
     * NICHT ab — Frank soll am Ende eine Liste haben in der die meisten neu
     * bewertet sind. Fehlgeschlagene werden in failed gezaehlt.
     */
    fun rescoreAllOpenEntries(autoTriggered: Boolean = false) {
        viewModelScope.launch {
            val targets = entries.getActive().first()
                .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
            if (targets.isEmpty()) {
                rescoreProgressFlow.value = null
                return@launch
            }
            rescoreProgressFlow.value = RescoreProgress(done = 0, total = targets.size, failed = 0)
            var done = 0
            var failed = 0
            for (entry in targets) {
                process.rescoreExisting(entry).onFailure { ex ->
                    failed++
                    Diag.w(DiagnosticArea.TASKS, TAG, "Rescore fuer ${entry.id} fehlgeschlagen: ${ex.message}")
                }.onSuccess {
                    done++
                }
                rescoreProgressFlow.value = RescoreProgress(done = done, total = targets.size, failed = failed)
                // Sanfte Pause zwischen Calls — Gemini-API verkraftet schnelle Bursts,
                // aber bei 50+ Eintraegen schont das die Quota und reduziert Hitze.
                delay(200L)
            }
            // Marker setzen — auch wenn ein paar Eintraege fehlgeschlagen sind.
            // Wenn Frank nochmal will, kann er den Knopf in den Settings nutzen.
            if (autoTriggered) {
                secrets.lastRescoreVersionCode = RESCORE_DOCTRINE_VERSION
            }
            Diag.i(DiagnosticArea.TASKS, TAG, "Rescore fertig: $done erfolgreich, $failed fehlgeschlagen, total ${targets.size}")
            // Banner noch 3 Sekunden stehen lassen damit Frank das Ergebnis sieht.
            delay(3_000L)
            rescoreProgressFlow.value = null
            // Nach Re-Score Buckets neu verteilen — die neuen priorityScores
            // koennten die Bucket-Verteilung beeinflussen.
            autoBalanceBuckets()
        }
    }

    /**
     * Periodischer Watcher: prueft minuetlich ob die lokale Datums-Komponente
     * sich geaendert hat. Wenn ja → MORGEN→HEUTE-Rollover ausloesen.
     * 1 Minute ist granular genug damit ein Tag-Wechsel um 0:00 spaetestens
     * um 0:01 erkannt wird, ohne dass der Watcher merklich Strom kostet
     * (60 Tick-Operationen pro Stunde, jede macht nur ein LocalDate-Vergleich).
     */
    private fun startDayChangeWatcher() {
        viewModelScope.launch {
            var lastCheckedDate = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            while (isActive) {
                delay(60_000L)
                val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                if (today != lastCheckedDate) {
                    lastCheckedDate = today
                    rolloverManualBucketsForNewDay()
                    // Auch alte erledigte Eintraege archivieren — der 14-Tage-
                    // Cutoff bewegt sich mit dem Datum mit.
                    autoArchiveOldResolved()
                }
            }
        }
    }

    /**
     * Verschiebt REDUZIERT-Eintraege deren resolvedAt mehr als 14 Tage alt ist
     * auf Status ARCHIVIERT (Frank-Wunsch 2026-05-09). Damit verschwinden sie
     * aus dem Aufgabenboard, bleiben aber im Settings-Archiv erreichbar — der
     * Forscher kann sie weiterhin als Kontext nutzen um Muster zu erkennen.
     */
    private fun autoArchiveOldResolved() {
        viewModelScope.launch {
            val cutoffMs = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
            val candidates = entries.getResolvedBefore(cutoffMs)
            if (candidates.isEmpty()) return@launch
            val now = System.currentTimeMillis()
            candidates.forEach { e ->
                entries.update(
                    e.copy(
                        status = EntryStatus.ARCHIVIERT,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    /**
     * Strikte Bucket-Verteilung mit harten Limits (Frank-Spezifikation 2026-05-09):
     *   - HEUTE: max 5 Eintraege
     *   - MORGEN: max 5 Eintraege
     *   - FREIBLOCK: max 10 Eintraege
     *   - SPAETER: unbegrenzt
     *
     * Algorithmus:
     * 1) Manuelle Eintraege belegen Slots in ihrem gewuenschten Bucket zuerst —
     *    sortiert nach priorityScore desc. Wenn ein manueller Bucket-Wunsch das
     *    Limit sprengt, fallen die ueberzaehligen ueber den KI-Pool weiter unten
     *    in den naechsten Bucket (sie behalten ihr manualBucket-Flag im UI als
     *    Hinweis "Frank wollte das hier — hat aber wegen Vollheit nicht gepasst").
     * 2) Restliche freie Slots werden mit KI-Eintraegen (manualBucket=null) nach
     *    priorityScore desc aufgefuellt.
     * 3) Ueberschuss landet in SPAETER (kein Limit).
     *
     * Idempotent: schreibt nur wenn sich timeBucket aendert.
     */
    private fun autoBalanceBuckets() {
        viewModelScope.launch {
            balanceBuckets()
        }
    }

    /**
     * Wenn Frank gestern eine Aufgabe manuell auf MORGEN gesetzt hat, ist sie heute
     * "morgen" -> wird zu HEUTE. Pruefung: manualBucketSetAt liegt vor Mitternacht
     * heute. FREIBLOCK und SPAETER bleiben unveraendert (kein Datums-Rollover).
     */
    private fun rolloverManualBucketsForNewDay() {
        viewModelScope.launch {
            val midnightTodayMs = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val active = entries.getActive().first()
            val toRollover = active.filter {
                it.manualBucket == TimeBucket.MORGEN &&
                    (it.manualBucketSetAt ?: Long.MAX_VALUE) < midnightTodayMs &&
                    (it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT)
            }
            val now = System.currentTimeMillis()
            toRollover.forEach { e ->
                de.frank.entropyreducer.data.diagnostics.Diag.i(
                    de.frank.entropyreducer.data.diagnostics.DiagnosticArea.APP,
                    "TasksVM",
                    "ROLLOVER '${e.title.take(24)}' MORGEN->HEUTE (setAt=${e.manualBucketSetAt} < midnight=$midnightTodayMs)",
                )
                entries.update(
                    e.copy(
                        manualBucket = TimeBucket.HEUTE,
                        manualBucketSetAt = now,
                        timeBucket = TimeBucket.HEUTE,
                        updatedAt = now,
                    ),
                )
            }
            if (toRollover.isNotEmpty()) {
                // Nach Rollover Auto-Verteilung neu aufrufen — gerollte Eintraege
                // koennten HEUTE-Limit sprengen, autoBalanceBuckets schiebt
                // schwaechste KI-Eintraege automatisch in MORGEN/FREIBLOCK/SPAETER.
                autoBalanceBuckets()
            }
        }
    }

    /**
     * Setzt einen manuellen Bucket fuer einen Eintrag (Frank-Wunsch 2026-05-09).
     * Ueberschreibt die KI-Zuordnung. Bei HEUTE wird das 5er-Limit eingehalten:
     * der Eintrag mit niedrigster Prio (ausgenommen der gerade gesetzte) wandert
     * automatisch nach MORGEN.
     */
    fun setManualBucket(entryId: String, bucket: TimeBucket) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            de.frank.entropyreducer.data.diagnostics.Diag.i(
                de.frank.entropyreducer.data.diagnostics.DiagnosticArea.APP,
                "TasksVM",
                "setManualBucket '${entry.title.take(24)}' alt mb=${entry.manualBucket}/tb=${entry.timeBucket} -> $bucket",
            )
            val now = System.currentTimeMillis()
            entries.update(
                entry.copy(
                    manualBucket = bucket,
                    manualBucketSetAt = now,
                    timeBucket = bucket,
                    updatedAt = now,
                ),
            )
            // Nach manueller Zuordnung auto-balance: andere KI-Eintraege werden
            // ggf. neu verteilt damit Buckets ihre Limits einhalten und alle
            // Eintraege sichtbar bleiben.
            autoBalanceBuckets()
        }
    }

    /**
     * Setzt manualBucket zurueck — KI uebernimmt die Bucket-Zuordnung wieder.
     * timeBucket bleibt bis zum naechsten Process/Rebucket auf dem aktuellen Wert.
     */
    fun clearManualBucket(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            entries.update(
                entry.copy(
                    manualBucket = null,
                    manualBucketSetAt = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            // KI uebernimmt — Eintrag wird ggf. in einen anderen Bucket
            // einsortiert basierend auf priorityScore und freien Slots.
            autoBalanceBuckets()
        }
    }


    /** Generiert die KI-Frage neu durch Gemini-API mit allen offenen Eintraegen
     *  als Kontext. Fallback auf statisch wenn kein API-Key vorhanden.
     *  Frank-Wunsch 2026-05-08: bei manuellem Refresh soll eine ANDERE Frage
     *  als die letzte entstehen — die letzte Frage wird als "vermeide diese
     *  wiederholungen"-Hint mitgeschickt, plus Frage wird sofort genullt damit
     *  der alte Text nicht stehen bleibt waehrend die neue Frage generiert wird. */
    fun refreshKiQuestion() {
        viewModelScope.launch {
            val previousText = kiQuestions.currentQuestion.first()?.text
            kiQuestions.setCurrent(null)
            try {
                val question = generateKiQuestion(previousText)
                kiQuestions.setCurrent(question)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Diag.e(DiagnosticArea.TASKS, "TasksViewModel", "refreshKiQuestion failed", t)
            }
        }
    }

    fun openEntryDetail(id: String) { detailEntryIdFlow.value = id }
    fun closeEntryDetail() { detailEntryIdFlow.value = null }

    /** Loescht einen Eintrag (nicht nur archivieren — komplett weg). Aus dem Detail-Sheet. */
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            val entry = entries.get(id) ?: return@launch
            entries.delete(entry)
            detailEntryIdFlow.value = null
        }
    }

    /** Status setzen aus dem Detail-Sheet (4-Buttons: OFFEN / IN_ARBEIT / REDUZIERT / ARCHIVIERT). */
    fun setEntryStatus(id: String, status: EntryStatus) {
        viewModelScope.launch {
            val entry = entries.get(id) ?: return@launch
            val now = System.currentTimeMillis()
            val updated = entry.copy(
                status = status,
                resolvedAt = if (status == EntryStatus.REDUZIERT || status == EntryStatus.ARCHIVIERT) now else null,
                updatedAt = now,
            )
            entries.update(updated)
            // Proaktiver Forscher (Frank-Wunsch 2026-05-08): wenn der Eintrag
            // gerade auf REDUZIERT geht, fragen wir wie er geloest wurde —
            // damit das InsightBoard "bestätigte Methoden" lernen kann.
            if (status == EntryStatus.REDUZIERT && entry.status != EntryStatus.REDUZIERT) {
                pendingMethodForFlow.value = updated
            }
        }
    }

    fun snoozeKiQuestion() = viewModelScope.launch { kiQuestions.snoozeFor24Hours() }

    /**
     * Antwort auf die KI-Frage des Moments verarbeiten (Frank-Wunsch 2026-05-08).
     * 1) Antwort durch ProcessEntryUseCase als neuen Eintrag werten.
     * 2) Dedup-Check: wenn ein bestehender offener Eintrag mit aehnlichem Titel
     *    existiert, wird der neue Eintrag wieder geloescht und stattdessen der
     *    bestehende mit aiNotes ergaenzt + priorityScore erhoeht (Frank's
     *    "Laufeinheit"-Beispiel: keine Doppel-Eintraege wenn die Antwort eine
     *    schon priorisierte Aufgabe nennt).
     * 3) Frage wird aus dem Repository entfernt — sie ist beantwortet, soll
     *    nicht stehen bleiben. Beim naechsten refresh kommt eine neue Frage.
     */
    fun submitKiQuestionAnswer(answer: String) {
        if (answer.isBlank()) return
        viewModelScope.launch {
            // Frage + Antwort fuer Forscher und Gedaechtnis speichern (Frank-Wunsch
            // 2026-05-09: Antworten muessen verteilt werden, nicht nur fuer den
            // einzelnen Eintrag-Update). Memory-Source MANUELL damit es als
            // bestaetigtes Wissen gilt; confidence 70 (hoch weil Frank's eigene
            // Aussage). Inhalt im Format "F: <Frage> | A: <Antwort>" — die KI
            // kann das im Briefing/Review/Forscher als Kontext nutzen.
            val currentQuestion = kiQuestions.currentQuestion.first()
            val questionText = currentQuestion?.text.orEmpty()
            kiQuestions.setCurrent(null)
            try {
                memories.upsert(
                    de.frank.entropyreducer.data.local.entities.MemoryEntryEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        content = if (questionText.isNotBlank()) {
                            "KI-Frage des Moments — Frage: \"$questionText\" — Antwort von Frank: \"${answer.trim()}\""
                        } else {
                            "Frank's Antwort auf KI-Frage: \"${answer.trim()}\""
                        },
                        source = de.frank.entropyreducer.domain.model.MemorySource.MANUELL,
                        isActive = true,
                        confidence = 70,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Diag.e(DiagnosticArea.TASKS, "TasksViewModel", "Memory speichern fehlgeschlagen", t)
            }
            uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Antwort wird verarbeitet …")
            val newResult = process(answer.trim(), EntrySource.NUTZER_TEXT)
            newResult.onSuccess { newEntry ->
                val current = entries.getActive().first()
                val similar = current.firstOrNull { it.id != newEntry.id && titlesAreSimilar(it.title, newEntry.title) }
                if (similar != null) {
                    val notes = (similar.aiNotes?.takeIf { it.isNotBlank() }?.let { "$it\n\n" } ?: "") +
                        "Frank's Antwort auf KI-Frage: ${answer.trim()}"
                    entries.update(
                        similar.copy(
                            aiNotes = notes,
                            priorityScore = (similar.priorityScore + 5.0).coerceAtMost(100.0),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    entries.delete(newEntry)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        processingMessage = null,
                        recentlyCreatedId = similar.id,
                    )
                } else {
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        processingMessage = null,
                        recentlyCreatedId = newEntry.id,
                    )
                }
                // Naechste Frage dynamisch generieren (basiert auf neuem Stand).
                refreshKiQuestion()
                // Nach neuem Eintrag Buckets neu verteilen damit HEUTE-Limit haelt.
                autoBalanceBuckets()
            }.onFailure { ex ->
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    processingMessage = null,
                    errorMessage = ex.message ?: "Antwort konnte nicht verarbeitet werden",
                )
            }
        }
    }

    /** Heuristischer Dedup: wenn der Title eines bestehenden Eintrags eine
     *  signifikante Substring-Ueberlappung mit dem neuen Title hat (>=60% der
     *  kuerzeren Variante), gelten sie als gleich. Faengt "Laufeinheit im Freien"
     *  vs "Laufen draußen" nicht ab — dafuer braeuchte es einen Embedding-Match.
     *  Fuer den haeufigen Fall "exact answer" reicht dieser einfache Check. */
    private fun titlesAreSimilar(a: String, b: String): Boolean {
        val na = a.lowercase().trim()
        val nb = b.lowercase().trim()
        if (na == nb) return true
        if (na.isBlank() || nb.isBlank()) return false
        val shorter = if (na.length < nb.length) na else nb
        val longer = if (na.length < nb.length) nb else na
        return longer.contains(shorter) && shorter.length >= 4
    }

    /**
     * Markiert einen Eintrag als REDUZIERT (= erledigt) — wird vom Haken-Button
     * auf der Eintrag-Card aufgerufen. resolvedAt wird auf jetzt gesetzt damit
     * die Analyse "heute reduziert" zaehlen kann.
     */
    fun markEntryResolved(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            val now = System.currentTimeMillis()
            val updated = entry.copy(
                status = EntryStatus.REDUZIERT,
                resolvedAt = now,
                updatedAt = now,
            )
            entries.update(updated)
            // Frank-Wunsch 2026-05-31: Klick aufs Haekchen hakt die Aufgabe DIREKT ab.
            // Es erscheint KEIN "Wie hast du das geloest?"-Fenster mehr. Das Erledigen
            // ist die einzige Aufgabe des Haekchens; rueckgaengig geht ueber die
            // Undo-Snackbar (siehe TasksScreen.onResolve).
        }
    }

    /**
     * Speichert die vom Forscher abgefragte Loesungsmethode in den ai_notes des
     * Eintrags. Format: 'Methode: <text>' an den vorhandenen aiNotes angehaengt.
     * Damit kann die KI im Briefing/Review/Insight-Board diese Methode
     * wiederfinden und als "bestätigte Methode" einordnen.
     */
    fun submitMethod(notes: String) {
        val current = pendingMethodForFlow.value ?: return
        if (notes.isBlank()) {
            pendingMethodForFlow.value = null
            return
        }
        viewModelScope.launch {
            val existing = current.aiNotes?.takeIf { it.isNotBlank() }
            val combined = if (existing == null) {
                "Methode: ${notes.trim()}"
            } else {
                "$existing\n\nMethode: ${notes.trim()}"
            }
            entries.update(
                current.copy(
                    aiNotes = combined,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            pendingMethodForFlow.value = null
        }
    }

    /**
     * Aufgaben-Nachtrag (Frank-Wunsch 2026-05-08): wenn Frank im Detail-Sheet
     * einen Nachtrag einspricht, wird der Eintrag durch ProcessEntryUseCase
     * neu bewertet — Title + Beschreibung + Tags + priorityScore + timeBucket
     * werden auf Basis von "Originaltext PLUS Nachtrag" neu generiert. So landet
     * die Aufgabe automatisch an der richtigen Prio-Stelle (auch wenn der
     * Nachtrag z.B. eine viel laengere Dauer impliziert).
     */
    fun addFollowupAndReprocess(entryId: String, followup: String) {
        if (followup.isBlank()) return
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            val combined = if (current.description.isNotBlank()) {
                "${current.description}\n\nNachtrag: ${followup.trim()}"
            } else {
                "Nachtrag: ${followup.trim()}"
            }
            uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Bewerte mit Nachtrag neu …")
            process("${current.title}. $combined", EntrySource.NUTZER_TEXT)
                .onSuccess {
                    // Den NEUEN Eintrag aus process() wieder mergen mit dem alten:
                    // wir wollen den alten id behalten, aber die neuen Felder uebernehmen.
                    // Pragmatisch: den alten Eintrag mit Nachtrag-text + neuen
                    // priority/category/bucket aktualisieren, neuen Eintrag wieder
                    // loeschen damit kein Doppel.
                    val newEntry = it
                    entries.update(
                        current.copy(
                            description = combined,
                            priorityScore = newEntry.priorityScore,
                            timeBucket = newEntry.timeBucket,
                            severity = newEntry.severity,
                            estimatedDurationMinutes = newEntry.estimatedDurationMinutes,
                            category = newEntry.category,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    entries.delete(newEntry)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = null)
                    autoBalanceBuckets()
                }
                .onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        processingMessage = null,
                        errorMessage = ex.message ?: "Nachtrag-Bewertung fehlgeschlagen",
                    )
                }
        }
    }

    fun dismissMethodPrompt() {
        pendingMethodForFlow.value = null
    }

    /**
     * Setzt einen erledigten Eintrag wieder auf OFFEN — für Undo-Snackbar oder
     * wenn der Benutzer sich vertippt hat.
     */
    fun reopenEntry(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            entries.update(
                entry.copy(
                    status = EntryStatus.OFFEN,
                    resolvedAt = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun toggleCategory(cat: EntropyCategory) {
        val current = activeCategoriesFlow.value.toMutableSet()
        if (cat in current) current.remove(cat) else current.add(cat)
        activeCategoriesFlow.value = current
    }

    fun clearCategoryFilter() { activeCategoriesFlow.value = emptySet() }

    /**
     * Sprint 6 (Frank-Wunsch 2026-05-22 abend): Mic-Sheet liefert fertigen Text
     * (egal ob aufgenommen oder geschrieben), wir jagen ihn nur noch durch
     * ProcessEntryUseCase. Ersetzt den alten Direct-Mic-Pfad — der bleibt nur
     * noch als Fallback fuer das Widget.
     */
    /**
     * Frank-Wunsch 2026-05-31: verbessert einen diktierten Text per Gemini
     * (Grammatik/Rechtschreibung, gleiche Laenge, Inhalt bleibt). Wird vom
     * Review-Fenster aufgerufen. Gibt bei Erfolg den verbesserten Text zurueck,
     * bei Fehler ein Result.failure (Aufrufer behaelt dann das Original).
     */
    suspend fun improveTranscript(text: String): Result<String> = improveText(text)

    /**
     * @param manualTitle optionaler, manuell eingetippter Titel aus dem
     *   Review-Fenster. Leer/null → KI erstellt den Titel.
     */
    fun processCapturedText(text: String, source: EntrySource, manualTitle: String? = null) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            uiOnlyFlow.value = uiOnlyFlow.value.copy(
                micState = MicState.PROCESSING,
                processingMessage = "Verarbeite …",
            )
            process(trimmed, source, manualTitle)
                .onSuccess { e ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        processingMessage = null,
                        recentlyCreatedId = e.id,
                    )
                    autoBalanceBuckets()
                }
                .onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        processingMessage = null,
                        errorMessage = ex.message ?: "KI-Verarbeitung fehlgeschlagen",
                    )
                }
        }
    }

    /**
     * Frank-Wunsch 2026-05-31: setzt die manuelle Prioritaet einer Aufgabe (0..100,
     * vom Schieberegler in 5er-Schritten). Hat Vorrang vor der KI-Prioritaet und
     * bleibt auch bei spaeteren Rescores bestehen. Die Kachel faerbt sich sofort
     * nach dem neuen Wert, da die Liste reaktiv aus der DB liest.
     */
    fun setManualPriority(entryId: String, score: Double) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            val now = System.currentTimeMillis()
            val clamped = score.coerceIn(0.0, 100.0)
            entries.update(
                entry.copy(
                    manualPriorityScore = clamped,
                    // Frank-Wunsch 2026-06-19: Marker setzen, dass die Prio AN DIESER INSTANZ
                    // manuell gesetzt wurde. Die Loop-Pflege (GenerateRecurringInstancesUseCase)
                    // ueberschreibt sie dann nicht mehr mit der Template-Prio (manuell > KI/Loop).
                    manualPriorityScoreSetAt = now,
                    updatedAt = now,
                )
            )
            // Frank-Wunsch 2026-06-19: Prio ins Prioritaets-Gedaechtnis lernen (nur wenn aktiviert).
            // Gilt fuer normale UND Loop-Aufgaben. Spaeter gleicht die KI neue Aufgaben dagegen ab.
            // BUGFIX 2026-06-19: in runCatching gekapselt — ein Fehler im optionalen Gedaechtnis-Lernen
            // darf NIEMALS die nachgelagerte Loop-Propagierung (isRec-Block) oder das Speichern der
            // manuellen Prio abbrechen. Fehler wird geloggt (nicht still verschluckt), Kern laeuft weiter.
            runCatching {
                if (settings.priorityMemoryEnabledFlow.first()) {
                    priorityMemoryRepository.learnFromManualPriority(entry, clamped, now)
                    Diag.i(
                        DiagnosticArea.APP,
                        "PrioMemory",
                        "gelernt: '${entry.title.take(32)}' -> ${clamped.toInt()}",
                    )
                }
            }
                .onFailure { e ->
                    Diag.i(
                        DiagnosticArea.APP,
                        "PrioMemory",
                        "Gedaechtnis-Lernen fehlgeschlagen (ignoriert, Kern laeuft weiter): ${e.message}",
                    )
                }
            // Frank-Wunsch 2026-06-19 (Bugfix): Ist die geaenderte Aufgabe eine LOOP-INSTANZ, muss
            // die Prio-Aenderung RUECKWAERTS ins Loop-Template (Loop-Bereich) UND in alle offenen
            // Geschwister-Instanzen propagieren — sonst stehen Heute (z.B. 60) und Loop (z.B. 30)
            // dauerhaft auseinander. Die Prio einer Loop-Aufgabe ist EIN Wert, egal wo man ihn aendert.
            val isRec = entry.source == EntrySource.RECURRING_TEMPLATE || entry.id.startsWith("rec-")
            if (isRec) {
                // observeAll (statt getActive), damit auch ein inaktives Template noch gefunden wird.
                // Zuordnung ueber den ID-Praefix (Template-IDs koennen selbst Bindestriche enthalten).
                val template = recurringTemplates.observeAll().first()
                    .firstOrNull { entry.id.startsWith("rec-${it.id}-") }
                if (template != null) {
                    recurringTemplates.upsert(
                        template.copy(priorityScore = clamped.toInt(), updatedAt = now),
                    )
                    Diag.i(
                        DiagnosticArea.APP, "TasksVM",
                        "setManualPriority '${entry.title.take(24)}' = $clamped -> Loop-Template " +
                            "'${template.id}' rueckwaerts auf ${clamped.toInt()} gesetzt",
                    )
                    // Offene Geschwister-Instanzen desselben Templates mitziehen (Konsistenz im Reiter).
                    entries.getActive().first()
                        .filter {
                            it.id != entry.id &&
                                it.source == EntrySource.RECURRING_TEMPLATE &&
                                it.id.startsWith("rec-${template.id}-") &&
                                it.status == EntryStatus.OFFEN
                        }
                        .forEach {
                            entries.update(
                                it.copy(
                                    priorityScore = clamped,
                                    manualPriorityScore = clamped,
                                    manualPriorityScoreSetAt = now,
                                    updatedAt = now,
                                ),
                            )
                        }
                }
            }
        }
    }

    fun onMicClick() {
        val app = getApplication<Application>()
        when (uiOnlyFlow.value.micState) {
            MicState.IDLE -> {
                runCatching {
                    recorder.start()
                    RecordingService.start(app)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.RECORDING,
                        processingMessage = "Aufnahme laeuft …",
                    )
                }.onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        errorMessage = ex.message ?: "Aufnahme konnte nicht gestartet werden",
                    )
                }
            }
            MicState.RECORDING -> {
                val file = recorder.stop()
                RecordingService.stop(app)
                if (file == null || !file.exists() || file.length() == 0L) {
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(micState = MicState.IDLE, processingMessage = null)
                    return
                }
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    micState = MicState.PROCESSING,
                    processingMessage = "Transkribiere …",
                )
                viewModelScope.launch {
                    transcribe(file).onSuccess { transcript ->
                        if (transcript.isBlank()) {
                            uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                micState = MicState.IDLE, processingMessage = null,
                            )
                            return@onSuccess
                        }
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Verarbeite …")
                        process(transcript, EntrySource.NUTZER_MIC)
                            .onSuccess { e ->
                                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                    micState = MicState.IDLE,
                                    processingMessage = null,
                                    recentlyCreatedId = e.id,
                                )
                                autoBalanceBuckets()
                            }
                            .onFailure { ex ->
                                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                    micState = MicState.IDLE,
                                    processingMessage = null,
                                    errorMessage = ex.message ?: "KI-Verarbeitung fehlgeschlagen",
                                )
                            }
                    }.onFailure { ex ->
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                            errorMessage = ex.message ?: "Transkription fehlgeschlagen",
                        )
                    }
                }
            }
            MicState.PROCESSING -> Unit
        }
    }

    fun dismissError() {
        uiOnlyFlow.value = uiOnlyFlow.value.copy(errorMessage = null)
    }

    fun changeStatus(entry: EntropyEntryEntity, newStatus: EntryStatus) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            entries.update(
                entry.copy(
                    status = newStatus,
                    updatedAt = now,
                    resolvedAt = if (newStatus == EntryStatus.REDUZIERT) now else entry.resolvedAt,
                )
            )
        }
    }

    fun delete(entry: EntropyEntryEntity) {
        viewModelScope.launch { entries.delete(entry) }
    }

    override fun onCleared() {
        super.onCleared()
        if (recorder.isRecording()) recorder.discard()
    }

    companion object {
        private const val TAG = "TasksViewModel"
        /**
         * versionCode bei dem die priorityScore-Doktrin zuletzt geaendert wurde.
         * Wenn lastRescoreVersionCode (in EncryptedSecretsStore) kleiner ist,
         * triggert der ViewModel-Init einen einmaligen Auto-Re-Score aller
         * offenen Aufgaben. Beim naechsten Doktrin-Update bitte hier die neue
         * versionCode-Nummer eintragen. Historie:
         *   29 (0.6.0): Erste 5-Farben-Skala mit Entropie-Reduktion-Definition
         *   31 (0.6.2): Vollausbau mit Entropie-Definition, Wellbeing-First-
         *               Prinzip, drei Score-Achsen, Frank-spezifischen Beispielen
         *               pro Stufe, Aufwand-Nutzen-Hebel und 5-10%-Quote fuer Rot.
         */
        private const val RESCORE_DOCTRINE_VERSION = 31
        private val TASK_PROCESSED_KEY = stringPreferencesKey("processed_idea_ids")
        private val HABIT_PROCESSED_KEY = stringPreferencesKey("habit_processed_idea_ids")
    }
}

// ============================================================================
// Private Helper fuer den Suggestion-Import im refreshAll()
// ============================================================================

private suspend fun storeKiTaskSuggestions(
    context: android.content.Context,
    newSuggestions: List<de.frank.entropyreducer.domain.usecase.AutoTaskSuggestion>,
) {
    // ID-Architektur Etappe 2c (Frank-Wunsch 2026-06-19): Aufgaben-Vorschlaege liegen in Room
    // (task_suggestions) statt im DataStore-JSON. Etappe 2d: Herkunft der Quell-Idee mitschreiben.
    val nowMs = System.currentTimeMillis()
    de.frank.entropyreducer.data.local
        .taskSuggestionDaoFrom(context)
        .upsertAll(
            newSuggestions.mapIndexed { index, s ->
                de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity(
                    id = s.id,
                    title = s.title,
                    description = s.description,
                    createdAt = nowMs + index,
                    originId = s.originId,
                    originType = s.originType,
                    rootId = s.rootId,
                )
            }
        )
}

private suspend fun loadProcessedIds(
    store: DataStore<Preferences>,
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
): Set<String> {
    return store.data.first().let { prefs ->
        val raw = prefs[key] ?: return@let emptySet()
        runCatching {
            val arr = JSONArray(raw)
            buildSet(arr.length()) {
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                }
            }
        }.getOrDefault(emptySet())
    }
}

private suspend fun saveProcessedIds(
    store: DataStore<Preferences>,
    key: androidx.datastore.preferences.core.Preferences.Key<String>,
    ids: Set<String>,
) {
    store.edit { prefs ->
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs[key] = arr.toString()
    }
}

private suspend fun storeHabitSuggestions(
    context: android.content.Context,
    newSuggestions: List<de.frank.entropyreducer.domain.usecase.AutoHabitSuggestion>,
) {
    // ID-Architektur Etappe 3c/3d (Frank-Wunsch 2026-06-19): Gewohnheits-Vorschlaege in Room
    // (habit_suggestions) statt im DataStore-JSON, mit Herkunft der Quell-Idee.
    val nowMs = System.currentTimeMillis()
    de.frank.entropyreducer.data.local
        .habitSuggestionDaoFrom(context)
        .upsertAll(
            newSuggestions.mapIndexed { index, s ->
                de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity(
                    id = s.id,
                    text = s.text,
                    createdAt = nowMs + index,
                    originId = s.originId,
                    originType = s.originType,
                    rootId = s.rootId,
                )
            }
        )
}
