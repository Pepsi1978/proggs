package de.frank.stacklabor.werftstudio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.frank.stacklabor.werftstudio.data.preferences.Absatzpause
import de.frank.stacklabor.werftstudio.data.preferences.AppEinstellungen
import de.frank.stacklabor.werftstudio.data.preferences.Erscheinung
import de.frank.stacklabor.werftstudio.data.preferences.TtsAnbieter
import de.frank.stacklabor.werftstudio.data.repository.ImportModus
import de.frank.stacklabor.werftstudio.data.repository.StackLaborRepository
import de.frank.stacklabor.werftstudio.data.repository.StackSortierstatus
import de.frank.stacklabor.werftstudio.data.transfer.StackExportDaten
import de.frank.stacklabor.werftstudio.data.transfer.StackTextExport
import de.frank.stacklabor.werftstudio.data.transfer.StackZielText
import de.frank.stacklabor.werftstudio.domain.model.Ampel
import de.frank.stacklabor.werftstudio.domain.model.Bewertung
import de.frank.stacklabor.werftstudio.domain.model.BewertungMitDetails
import de.frank.stacklabor.werftstudio.domain.model.Bewertungszelle
import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.EigeneFrage
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Sortieransicht
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackEintragDosis
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Ziel
import de.frank.stacklabor.werftstudio.domain.model.anzeige
import de.frank.stacklabor.werftstudio.domain.model.darreichungsformAnzeige
import de.frank.stacklabor.werftstudio.domain.model.einheitAnzeige
import de.frank.stacklabor.werftstudio.domain.model.frequenzAnzeige
import de.frank.stacklabor.werftstudio.domain.model.mitDeutschenUmlauten
import de.frank.stacklabor.werftstudio.domain.model.toDarreichungsformOrNull
import de.frank.stacklabor.werftstudio.domain.model.toEinheitOrNull
import de.frank.stacklabor.werftstudio.service.auth.CodexAuthState
import de.frank.stacklabor.werftstudio.service.codex.CodexErrorKind
import de.frank.stacklabor.werftstudio.service.codex.CodexException
import de.frank.stacklabor.werftstudio.service.codex.CodexStreamEvent
import de.frank.stacklabor.werftstudio.service.speech.MicRecorder
import de.frank.stacklabor.werftstudio.service.tts.ClonedVoice
import de.frank.stacklabor.werftstudio.service.tts.TtsConfiguration
import de.frank.stacklabor.werftstudio.service.tts.TtsVoices
import de.frank.stacklabor.werftstudio.service.tts.TtsPlaybackState
import de.frank.stacklabor.werftstudio.service.tts.TtsPlaybackStateBus
import de.frank.stacklabor.werftstudio.service.tts.TtsProviderId
import de.frank.stacklabor.werftstudio.service.tts.TtsUsage
import de.frank.stacklabor.werftstudio.service.work.CodexRetryWorker
import de.frank.stacklabor.werftstudio.ui.model.Appearance
import de.frank.stacklabor.werftstudio.ui.model.CodexLoginState
import de.frank.stacklabor.werftstudio.ui.model.CompetitionUi
import de.frank.stacklabor.werftstudio.ui.model.DoseSummaryUi
import de.frank.stacklabor.werftstudio.ui.model.DoseVariant
import de.frank.stacklabor.werftstudio.ui.model.EvaluationRunUi
import de.frank.stacklabor.werftstudio.ui.model.EvaluationState
import de.frank.stacklabor.werftstudio.ui.model.FingerprintPurpose
import de.frank.stacklabor.werftstudio.ui.model.GoalUi
import de.frank.stacklabor.werftstudio.ui.model.GoalInputState
import de.frank.stacklabor.werftstudio.ui.model.HistoryChangeUi
import de.frank.stacklabor.werftstudio.ui.model.MedicineUi
import de.frank.stacklabor.werftstudio.ui.model.MedicineSolubilityAiState
import de.frank.stacklabor.werftstudio.ui.model.PlaybackState
import de.frank.stacklabor.werftstudio.ui.model.QuestionUi
import de.frank.stacklabor.werftstudio.ui.model.RelationshipUi
import de.frank.stacklabor.werftstudio.ui.model.SignalCounts
import de.frank.stacklabor.werftstudio.ui.model.SignalState
import de.frank.stacklabor.werftstudio.ui.model.SortMode
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiEffect
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.model.StackEvaluationEntryUi
import de.frank.stacklabor.werftstudio.ui.model.StackEvaluationGoalUi
import de.frank.stacklabor.werftstudio.ui.model.StackEvaluationGroupUi
import de.frank.stacklabor.werftstudio.ui.model.StackSummaryUi
import de.frank.stacklabor.werftstudio.ui.model.sortiereNachLoeslichkeit
import de.frank.stacklabor.werftstudio.ui.model.toCatalogMedicineUi
import de.frank.stacklabor.werftstudio.ui.model.toQuestionUi
import de.frank.stacklabor.werftstudio.ui.model.toSignalState
import de.frank.stacklabor.werftstudio.ui.model.toSolubility
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StackLaborViewModel(private val container: AppContainer) : ViewModel() {
    private val repository: StackLaborRepository = container.repository
    private val mutableState = MutableStateFlow(
        StackLaborUiState(versionName = BuildConfig.VERSION_NAME, versionBumpedAt = BuildConfig.VERSION_BUMPED_AT),
    )
    val state: StateFlow<StackLaborUiState> = mutableState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        mutableState.value,
    )
    private val effectChannel = Channel<StackLaborUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private val selectedStackId = MutableStateFlow<String?>(null)
    private var currentRoute: StackLaborRoute = StackLaborRoute.Home
    private var settings = AppEinstellungen(bewegungReduziert = false)
    private var stacks: List<Stack> = emptyList()
    private var entries: List<StackEintrag> = emptyList()
    private var goals: List<Ziel> = emptyList()
    private var stackGoals: List<StackZiel> = emptyList()
    private var medicines: List<Mittel> = emptyList()
    private var currentEntries: List<StackEintrag> = emptyList()
    private var currentHistory: List<BewertungMitDetails> = emptyList()
    private var currentEvaluation: BewertungMitDetails? = null
    private var tagEvaluation: BewertungMitDetails? = null
    private var evaluationJob: Job? = null
    private var clonedVoicesJob: Job? = null
    private var goalInputJob: Job? = null
    private var solubilityJob: Job? = null
    private var solubilityGeneration: Long = 0L
    private val micRecorder = MicRecorder(container.applicationContext)
    private var goalDraftBeforeImprovement: String? = null
    private val improvedGoalVersions = mutableListOf<String>()
    private var allStacksDraftBeforeImprovement: String? = null
    private val improvedAllStacksVersions = mutableListOf<String>()
    private var pendingSpeechTarget: SpeechInputTarget? = null
    private var clonedVoices: List<ClonedVoice> = emptyList()
    private val competitionJobs = mutableMapOf<String, Job>()
    private val pendingCompetitionIds = mutableMapOf<String, MutableSet<String>>()
    private var stackDraft = StackDraft()
    private var medicineDraft = MedicineDraft()
    private var undoAction: (suspend () -> Unit)? = null
    private val selectedHistoryIds = linkedSetOf<String>()
    /** Die Startsperre wird pro App-Lauf genau einmal gestellt. */
    private var startsperreGeprueft = false
    /** Welcher Stack auf einen Fingerabdruck wartet, um sein Schloss umzulegen. */
    private var wartenderSchlossStack: String? = null

    init {
        observeGlobalData()
        observeSelectedStack()
        observeTagEvaluation()
        observePlayback()
    }

    fun onEvent(event: StackLaborEvent) {
        when (event) {
            is StackLaborEvent.RouteChanged -> routeChanged(event.route)
            StackLaborEvent.ToggleAppearance -> launchAction {
                container.settings.setzeErscheinung(if (settings.erscheinung == Erscheinung.HELL) Erscheinung.DUNKEL else Erscheinung.HELL)
            }
            is StackLaborEvent.ChangeDoseVariant -> launchAction {
                container.settings.setzeDosisVariante(if (event.variant == DoseVariant.Frei) DosisVariante.FREI else DosisVariante.DIENST)
            }
            is StackLaborEvent.ChangeSortMode -> launchAction {
                val stackId = selectedStackId.value ?: return@launchAction
                // Nochmal auf die bereits aktive Löslichkeit tippen dreht die Richtung um,
                // statt nichts zu tun.
                if (event.mode == SortMode.Loeslichkeit && mutableState.value.sortMode == SortMode.Loeslichkeit) {
                    val fettZuerst = !settings.loeslichkeitFettZuerst
                    container.settings.setzeLoeslichkeitFettZuerst(fettZuerst)
                    message(if (fettZuerst) "Fettlösliche zuerst" else "Wasserlösliche zuerst")
                } else {
                    repository.setzeSortieransicht(stackId, if (event.mode == SortMode.Loeslichkeit) Sortieransicht.LOESLICHKEIT else Sortieransicht.EINNAHME)
                }
            }
            is StackLaborEvent.ChangeSearch -> mutableState.update { it.copy(searchQuery = event.query) }
            is StackLaborEvent.ToggleMedicine -> if (!gesperrtGemeldet(event.stackId)) launchAction {
                currentEntries.firstOrNull { it.stackId == event.stackId && it.mittelId == event.medicineId }?.let {
                    repository.setzeEintragAktiv(it.id, !it.aktiv)
                }
            }
            is StackLaborEvent.RemoveMedicineFromStack -> if (!gesperrtGemeldet(event.stackId)) removeMedicineFromStack(event.stackId, event.medicineId)
            is StackLaborEvent.ReorderMedicine -> if (!gesperrtGemeldet(event.stackId)) reorderMedicine(event.stackId, event.medicineId, event.targetIndex)
            is StackLaborEvent.ApplyMedicineOrder -> if (!gesperrtGemeldet(event.stackId)) applyMedicineOrder(event.stackId, event.medicineIds)
            is StackLaborEvent.ApplyStackOrder -> launchAction { repository.sortiereStacks(event.stackIds) }
            is StackLaborEvent.EditGoal -> launchAction { repository.speichereZiel(Ziel(event.goalId, event.text.trim())) }
            is StackLaborEvent.BeginGoalDraft -> beginGoalDraft(event.text)
            is StackLaborEvent.UpdateGoalDraft -> updateGoalDraft(event.text)
            StackLaborEvent.CancelGoalDraft -> cancelGoalDraft()
            StackLaborEvent.ToggleGoalRecording -> toggleGoalRecording()
            is StackLaborEvent.MicrophonePermissionResult -> microphonePermissionResult(event.granted)
            StackLaborEvent.ImproveGoalDraft -> improveGoalDraft()
            StackLaborEvent.UndoGoalImprovement -> undoGoalImprovement()
            is StackLaborEvent.ToggleGoal -> toggleGoal(event.stackId, event.goalId)
            is StackLaborEvent.ReorderGoal -> reorderGoal(event.stackId, event.goalId, event.targetRank)
            is StackLaborEvent.UpdateStackName -> stackDraft = stackDraft.copy(name = event.value)
            is StackLaborEvent.UpdateStackTime -> stackDraft = stackDraft.copy(time = event.value)
            is StackLaborEvent.UpdateStackNote -> stackDraft = stackDraft.copy(note = event.value)
            is StackLaborEvent.UpdateMedicineField -> {
                val bisherigerName = medicineDraft.name.trim()
                medicineDraft = medicineDraft.with(event.field, event.value)
                if (event.field == "name" && bisherigerName != event.value.trim()) {
                    medicineDraft = medicineDraft.copy(solubilityAiDetermined = false)
                    scheduleSolubilityDetermination(event.value)
                }
            }
            is StackLaborEvent.SelectHistoryRun -> selectHistory(event.runId)
            is StackLaborEvent.SelectSetting -> selectSetting(event.settingId)
            is StackLaborEvent.SelectSettingValue -> selectSettingValue(event.settingId, event.value)
            is StackLaborEvent.SaveApiKey -> saveApiKey(event.keyId, event.value)
            StackLaborEvent.LoadClonedVoices -> loadClonedVoices()
            is StackLaborEvent.RemoveQuestion -> removeQuestion(event.questionId)
            is StackLaborEvent.SaveStack -> if (!gesperrtGemeldet(event.stackId)) saveStack(event.stackId)
            is StackLaborEvent.DeleteStack -> if (!gesperrtGemeldet(event.stackId)) launchAction("Stack gelöscht") { repository.loescheStack(event.stackId) }
            is StackLaborEvent.ToggleStackLock -> toggleStackLock(event.stackId)
            StackLaborEvent.ReportStackLocked -> message("Der Stack ist gesperrt. Öffne zuerst das Schloss.")
            StackLaborEvent.ToggleFingerprint -> toggleFingerprint()
            StackLaborEvent.RetryAppUnlock -> requestFingerprint(
                FingerprintPurpose.AppStart,
                "StackLabor entsperren",
                "Bitte mit dem Fingerabdruck bestätigen.",
            )
            is StackLaborEvent.FingerprintResult -> fingerprintResult(event.purpose, event.granted, event.message)
            is StackLaborEvent.SaveMedicine -> if (!gesperrtGemeldet(event.stackId)) saveMedicine(event.medicineId, event.stackId)
            is StackLaborEvent.DeleteMedicine -> launchAction("Mittel gelöscht") { repository.loescheMittel(event.medicineId, bestaetigt = true) }
            is StackLaborEvent.AddMedicineToStack -> if (!gesperrtGemeldet(event.stackId)) addMedicineToStack(event.stackId, event.medicineId)
            is StackLaborEvent.AddGoal -> launchAction("Ziel gespeichert") {
                repository.speichereZiel(Ziel(UUID.randomUUID().toString(), event.text.trim()))
            }
            is StackLaborEvent.DeleteGoal -> launchAction("Ziel gelöscht") { repository.loescheZiel(event.goalId) }
            is StackLaborEvent.SaveQuestion -> launchAction("Frage gespeichert") {
                repository.speichereFrage(EigeneFrage(event.questionId ?: UUID.randomUUID().toString(), event.stackId, event.text.trim()))
            }
            StackLaborEvent.Undo -> undo()
            StackLaborEvent.EvaluateStack -> evaluate(selectedStackId.value)
            StackLaborEvent.EvaluateAll -> openAllStacksSetup(reset = true)
            is StackLaborEvent.ToggleAllStackSelection -> toggleAllStackSelection(event.stackId)
            is StackLaborEvent.UpdateAllStacksAdditionalInfo -> updateAllStacksAdditionalInfo(event.text)
            StackLaborEvent.ToggleAllStacksRecording -> toggleAllStacksRecording()
            StackLaborEvent.ImproveAllStacksAdditionalInfo -> improveAllStacksAdditionalInfo()
            StackLaborEvent.UndoAllStacksImprovement -> undoAllStacksImprovement()
            StackLaborEvent.ConfirmEvaluateAll -> confirmEvaluateAll()
            StackLaborEvent.DismissAllStacksSetup -> dismissAllStacksSetup()
            StackLaborEvent.RetryEvaluation -> evaluate(selectedStackId.value)
            StackLaborEvent.CancelEvaluation -> {
                evaluationJob?.cancel()
                evaluationJob = null
                mutableState.update {
                    it.copy(
                        evaluationState = if (it.evaluationState == EvaluationState.Running) EvaluationState.Stale else it.evaluationState,
                        allStacksEvaluationState = if (it.allStacksEvaluationState == EvaluationState.Running) EvaluationState.Stale else it.allStacksEvaluationState,
                    )
                }
                message("Auswertung abgebrochen")
            }
            StackLaborEvent.ScheduleRetry -> selectedStackId.value?.let { stackId ->
                CodexRetryWorker.enqueueUnique(containerContext(), stackId)
                message("Erneute Auswertung ist eingeplant")
            }
            StackLaborEvent.ExportData -> launchAction { effectChannel.send(StackLaborUiEffect.CreateExportDocument(repository.exportiere())) }
            is StackLaborEvent.ExportStack -> exportStack(event.stackId)
            StackLaborEvent.ExportAllStacks -> exportAllStacks()
            StackLaborEvent.ImportData -> sendEffect(StackLaborUiEffect.OpenImportDocument)
            is StackLaborEvent.ImportDocument -> launchAction("Daten importiert") { repository.importiere(event.json, ImportModus.ERSETZEN) }
            StackLaborEvent.ImportSeedData -> launchAction("Startbestand eingelesen") { repository.importiereStartbestand() }
            StackLaborEvent.RestoreBackup -> launchAction("Sicherung wiederhergestellt") { repository.stelleRueckfallsicherungWiederHer() }
            StackLaborEvent.OpenCodexPage -> sendEffect(StackLaborUiEffect.OpenUrl(mutableState.value.codexVerificationUrl))
            StackLaborEvent.RetryCodexLogin -> startCodexLogin()
            StackLaborEvent.Play -> playTts()
            is StackLaborEvent.PlayFromParagraph -> playTts(event.index, fortsetzen = false)
            StackLaborEvent.Pause -> sendEffect(StackLaborUiEffect.PauseTts)
            StackLaborEvent.Stop -> sendEffect(StackLaborUiEffect.StopTts)
            is StackLaborEvent.MergeMedicines -> mergeMedicines(event.keepId, event.replaceId)
        }
    }

    private fun observeGlobalData() {
        val data = combine(
            repository.beobachteStacks(),
            repository.beobachteAlleEintraege(),
            repository.beobachteAlleStackZiele(),
        ) { stackList, entryList, stackGoalList -> Triple(stackList, entryList, stackGoalList) }
        val catalog = combine(
            repository.beobachteZiele(),
            repository.beobachteMittel(),
            container.settings.einstellungen,
        ) { goalList, medicineList, appSettings -> Triple(goalList, medicineList, appSettings) }
        viewModelScope.launch {
            combine(data, catalog, container.oauth.state, container.ttsUsage.usage) { database, catalogData, auth, usage ->
                GlobalSnapshot(database, catalogData, auth, usage)
            }.collect { snapshot -> applyGlobal(snapshot) }
        }
    }

    private suspend fun applyGlobal(snapshot: GlobalSnapshot) {
        stacks = snapshot.database.first
        entries = snapshot.database.second
        stackGoals = snapshot.database.third
        goals = snapshot.catalog.first
        medicines = snapshot.catalog.second
        settings = snapshot.catalog.third
        if (selectedStackId.value == null && stacks.isNotEmpty()) selectedStackId.value = stacks.first().id
        val ampeln = stacks.associate { stack -> stack.id to repository.berechneAmpeln(stack.id) }
        val summaries = stacks.map { stack ->
            val result = ampeln.getValue(stack.id)
            val counts = result.zielAmpeln.values.toCounts()
            StackSummaryUi(
                id = stack.id,
                name = stack.name,
                meta = listOf(stack.zeitpunkt, stack.einnahmeHinweis).filter(String::isNotBlank).joinToString(" · "),
                medicineCount = entries.count { it.stackId == stack.id },
                signal = result.sammelAmpel.toSignalState(),
                counts = counts,
                locked = stack.gesperrt,
            )
        }
        val usageByMedicine = entries.groupingBy { it.mittelId }.eachCount()
        val catalogUi = medicines.map { it.toCatalogMedicineUi(usageByMedicine[it.id] ?: 0) }
        val selectedStack = stacks.firstOrNull { it.id == selectedStackId.value }
        val selectedTargets = stackGoals.filter { it.stackId == selectedStack?.id }.associateBy { it.zielId }
        val usageByGoal = stackGoals.groupingBy { it.zielId }.eachCount()
        val selectedGoalSignals = selectedStack?.let { ampeln[it.id]?.zielAmpeln }.orEmpty()
        val selectedEvaluation = currentEvaluation?.takeIf { it.bewertung.stackId == selectedStack?.id }
        val goalCatalogUi = goals.map { goal ->
            val target = selectedTargets[goal.id]
            GoalUi(
                id = goal.id,
                rank = target?.rang ?: 0,
                text = goal.text,
                signal = (selectedGoalSignals[goal.id] ?: Ampel.GRAU).toSignalState(),
                selected = target != null,
                reason = selectedEvaluation?.zellen?.firstOrNull { it.zielId == goal.id }?.grund.orEmpty().mitDeutschenUmlauten(),
                usage = "in ${usageByGoal[goal.id] ?: 0} Stacks verwendet",
            )
        }.sortedWith(compareBy<GoalUi> { if (it.selected) 0 else 1 }.thenBy { if (it.selected) it.rank else Int.MAX_VALUE }.thenBy { it.text })
        mutableState.update { current ->
            current.copy(
                loading = false,
                appearance = if (settings.erscheinung == Erscheinung.DUNKEL) Appearance.Dark else Appearance.Light,
                reducedMotion = settings.bewegungReduziert,
                doseVariant = if (settings.dosisVariante == DosisVariante.FREI) DoseVariant.Frei else DoseVariant.Dienst,
                stacks = summaries,
                stackName = selectedStack?.name ?: current.stackName,
                stackTime = selectedStack?.zeitpunkt ?: current.stackTime,
                stackNote = selectedStack?.einnahmeHinweis ?: current.stackNote,
                stackLocked = selectedStack?.gesperrt ?: current.stackLocked,
                fingerprintEnabled = settings.fingerabdruckAktiv,
                appLocked = if (startsperreGeprueft) current.appLocked else settings.fingerabdruckAktiv,
                catalogMedicines = catalogUi,
                goals = goalCatalogUi,
                dailyDoses = dailyDoses(
                    entries.filter { entry -> current.selectedAllStackIds.isEmpty() || entry.stackId in current.selectedAllStackIds },
                    medicines,
                    stacks.filter { stack -> current.selectedAllStackIds.isEmpty() || stack.id in current.selectedAllStackIds },
                    settings.dosisVariante,
                ),
                allStackGroups = stackEvaluationGroups(
                    current.selectedAllStackIds,
                    stacks,
                    entries,
                    medicines,
                    stackGoals,
                    goals,
                    settings.dosisVariante,
                ),
                ttsProviderLabel = settings.ttsAnbieter.label(),
                ttsVoiceLabel = voiceLabel(settings.ttsStimme, settings.ttsAnbieter),
                ttsVoiceOptions = voiceOptions(settings.ttsAnbieter),
                googleApiKeyValue = settings.googleApiKey,
                qwenApiKeyValue = settings.qwenApiKey,
                groqApiKeyValue = settings.groqApiKey,
                googleApiKeyLabel = keyLabel(settings.googleApiKey, BuildConfig.GOOGLE_TTS_API_KEY),
                qwenApiKeyLabel = keyLabel(settings.qwenApiKey, BuildConfig.QWEN_TTS_API_KEY),
                groqApiKeyLabel = keyLabel(settings.groqApiKey, ""),
                ttsSpeedLabel = speedLabel(settings.ttsTempo),
                ttsPauseLabel = settings.absatzpause.name.lowercase().replaceFirstChar(Char::uppercase),
                ttsTimeoutLabel = "${settings.abschaltzeitMinuten} Min.",
                ttsUsageLabel = "${NumberFormat.getIntegerInstance(Locale.GERMANY).format(snapshot.usage.totalCharacters)} Zeichen",
                codexAccountLabel = when (val auth = snapshot.auth) {
                    CodexAuthState.SignedOut -> "Nicht angemeldet"
                    is CodexAuthState.SignedIn -> auth.account.email ?: auth.account.accountId ?: "Angemeldet"
                },
                codexModelLabel = settings.codexModell.substringAfterLast('-').replaceFirstChar(Char::uppercase),
                codexReasoningLabel = settings.codexDenkstufe.reasoningLabel(),
            )
        }
        if (!startsperreGeprueft) {
            startsperreGeprueft = true
            if (settings.fingerabdruckAktiv) {
                requestFingerprint(
                    FingerprintPurpose.AppStart,
                    "StackLabor entsperren",
                    "Bitte mit dem Fingerabdruck bestätigen.",
                )
            }
        }
        applyTagEvaluation(tagEvaluation)
    }

    private fun observeSelectedStack() {
        viewModelScope.launch {
            selectedStackId.filterNotNull().flatMapLatest { stackId ->
                val content = combine(
                    repository.beobachteEintraege(stackId),
                    repository.beobachteStackZiele(stackId),
                    repository.beobachteFragen(stackId),
                ) { entryList, targetList, questions -> Triple(entryList, targetList, questions) }
                val evaluation = combine(
                    repository.beobachteNeuesteBewertung(stackId),
                    repository.beobachteHistorie(stackId),
                    repository.beobachteSortieransicht(stackId),
                ) { latest, history, sorting -> Triple(latest, history, sorting) }
                combine(content, evaluation, container.settings.einstellungen, repository.beobachteMittel()) { current, evaluated, appSettings, medicineList ->
                    SelectedSnapshot(stackId, current, evaluated, appSettings, medicineList)
                }
            }.collect { applySelected(it) }
        }
    }

    private suspend fun applySelected(snapshot: SelectedSnapshot) {
        currentEntries = snapshot.content.first
        currentHistory = snapshot.evaluation.second
        currentEvaluation = snapshot.evaluation.first
        val stack = stacks.firstOrNull { it.id == snapshot.stackId } ?: return
        val targetById = snapshot.content.second.associateBy { it.zielId }
        val ampeln = repository.berechneAmpeln(snapshot.stackId)
        val medicineById = snapshot.medicines.associateBy { it.id }
        val selectedMedicines = snapshot.content.first.mapNotNull { entry ->
            medicineById[entry.mittelId]?.let { medicine -> medicine.toUi(entry, ampeln.mittelAmpeln[medicine.id] ?: Ampel.GRAU, snapshot.settings.dosisVariante) }
        }.let { list ->
            if (snapshot.evaluation.third.ansicht == Sortieransicht.LOESLICHKEIT) {
                sortiereNachLoeslichkeit(list, snapshot.settings.loeslichkeitFettZuerst)
            } else if (!snapshot.evaluation.third.manuelleReihenfolge) {
                list.sortedBy { it.name.lowercase(Locale.GERMAN) }
            } else list
        }
        val usageByGoal = stackGoals.groupingBy { it.zielId }.eachCount()
        val goalUi = goals.map { goal ->
            val target = targetById[goal.id]
            GoalUi(
                id = goal.id,
                rank = target?.rang ?: 0,
                text = goal.text,
                signal = (ampeln.zielAmpeln[goal.id] ?: Ampel.GRAU).toSignalState(),
                selected = target != null,
                reason = snapshot.evaluation.first?.zellen?.firstOrNull { it.zielId == goal.id }?.grund.orEmpty().mitDeutschenUmlauten(),
                usage = "in ${usageByGoal[goal.id] ?: 0} Stacks verwendet",
            )
        }.sortedWith(compareBy<GoalUi> { if (it.selected) 0 else 1 }.thenBy { if (it.selected) it.rank else Int.MAX_VALUE }.thenBy { it.text })
        val latest = snapshot.evaluation.first
        val stale = latest?.let { repository.istBewertungVeraltet(snapshot.stackId, snapshot.settings.dosisVariante) } ?: false
        val historyUi = snapshot.evaluation.second.map { it.toHistoryUi(it.bewertung.id in selectedHistoryIds) }
        val breakdownSubject = (currentRoute as? StackLaborRoute.Breakdown)?.subjectId
        mutableState.update { current ->
            current.copy(
                selectedStackId = snapshot.stackId,
                stackName = stack.name,
                stackTime = stack.zeitpunkt,
                stackNote = stack.einnahmeHinweis,
                stackLocked = stack.gesperrt,
                sortMode = if (snapshot.evaluation.third.ansicht == Sortieransicht.LOESLICHKEIT) SortMode.Loeslichkeit else SortMode.Einnahme,
                solubilityFatFirst = snapshot.settings.loeslichkeitFettZuerst,
                medicines = selectedMedicines,
                goals = goalUi,
                breakdownItems = breakdownSubject?.let { buildBreakdownItems(it, latest, goalUi, selectedMedicines) }.orEmpty(),
                questions = snapshot.content.third.map { it.toQuestionUi() },
                history = historyUi,
                evaluationState = if (evaluationJob?.isActive == true) EvaluationState.Running else when {
                    latest == null -> EvaluationState.Stale
                    stale -> EvaluationState.Stale
                    else -> EvaluationState.Valid
                },
                evaluationParagraphs = latest?.bewertung?.gesamt?.paragraphs().orEmpty(),
                evaluationMeta = latest?.bewertung?.meta() ?: "Noch nicht ausgewertet",
                latestEvaluationId = latest?.bewertung?.id,
            )
        }
        if (currentRoute is StackLaborRoute.StackEdit && (currentRoute as StackLaborRoute.StackEdit).stackId == snapshot.stackId) {
            stackDraft = StackDraft(stack.name, stack.zeitpunkt, stack.einnahmeHinweis)
        }
    }

    private fun observeTagEvaluation() {
        viewModelScope.launch {
            repository.beobachteNeuesteTagBewertung().collect { latest ->
                tagEvaluation = latest
                applyTagEvaluation(latest)
            }
        }
    }

    private fun applyTagEvaluation(latest: BewertungMitDetails?) {
        val medicineById = medicines.associateBy { it.id }
        mutableState.update { current ->
            current.copy(
                allStacksEvaluationState = when {
                    evaluationJob?.isActive == true && currentRoute == StackLaborRoute.AllStacks -> EvaluationState.Running
                    latest == null -> EvaluationState.Stale
                    else -> EvaluationState.Valid
                },
                allStacksEvaluationId = latest?.bewertung?.id,
                competitions = latest?.konkurrenzen.orEmpty().map { competition ->
                    CompetitionUi(
                        title = listOfNotNull(
                            medicineById[competition.mittelAId]?.name,
                            medicineById[competition.mittelBId]?.name,
                        ).joinToString(" · "),
                        detail = competition.grund.mitDeutschenUmlauten(),
                    )
                },
            )
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            TtsPlaybackStateBus.state.collect { playback ->
                mutableState.update {
                    it.copy(
                        playbackState = when (playback) {
                            TtsPlaybackState.Idle, is TtsPlaybackState.Error -> PlaybackState.Ready
                            is TtsPlaybackState.Paused -> PlaybackState.Paused
                            is TtsPlaybackState.Playing, is TtsPlaybackState.Preparing -> PlaybackState.Playing
                        },
                        spokenParagraphIndex = when (playback) {
                            is TtsPlaybackState.Paused -> playback.paragraphIndex
                            is TtsPlaybackState.Playing -> playback.paragraphIndex
                            is TtsPlaybackState.Preparing -> playback.paragraphIndex
                            else -> null
                        },
                    )
                }
                if (playback is TtsPlaybackState.Error) message(playback.message)
            }
        }
    }

    private fun routeChanged(route: StackLaborRoute) {
        if (route != currentRoute) mutableState.update { it.copy(searchQuery = "") }
        if (currentRoute is StackLaborRoute.GoalCatalog && route !is StackLaborRoute.GoalCatalog) cancelGoalDraft()
        if (currentRoute is StackLaborRoute.MedicineEdit && route !is StackLaborRoute.MedicineEdit) cancelSolubilityDetermination()
        if (currentRoute == StackLaborRoute.AllStacks && route != StackLaborRoute.AllStacks) dismissAllStacksSetup()
        currentRoute = route
        val stackId = route.stackIdOrNull()
        if (stackId != null) selectedStackId.value = stackId
        when (route) {
            is StackLaborRoute.StackEdit -> stackDraft = route.stackId?.let { id ->
                stacks.firstOrNull { it.id == id }?.let { StackDraft(it.name, it.zeitpunkt, it.einnahmeHinweis) }
            } ?: StackDraft()
            is StackLaborRoute.MedicineEdit -> prepareMedicineDraft(route.medicineId, route.stackId)
            is StackLaborRoute.CodexLogin -> if (container.oauth.state.value is CodexAuthState.SignedOut && mutableState.value.codexDeviceCode.isBlank()) startCodexLogin()
            StackLaborRoute.AllStacks -> openAllStacksSetup(reset = true)
            is StackLaborRoute.Evaluation -> (
                currentHistory.firstOrNull { it.bewertung.id == route.evaluationId }
                    ?: tagEvaluation?.takeIf { it.bewertung.id == route.evaluationId }
                )?.let { showEvaluation(it) }
            is StackLaborRoute.Breakdown -> mutableState.update {
                it.copy(breakdownItems = buildBreakdownItems(route.subjectId, currentEvaluation, it.goals, it.medicines))
            }
            // Die eigenen Stimmen kommen aus dem Alibaba-Konto — beim Öffnen einmal nachladen.
            StackLaborRoute.Settings -> if (clonedVoices.isEmpty() && container.qwenSchluesselAktiv().isNotBlank()) loadClonedVoices()
            else -> Unit
        }
    }

    private fun beginGoalDraft(text: String) {
        goalInputJob?.cancel()
        micRecorder.release()
        clearGoalImprovementHistory()
        mutableState.update {
            it.copy(goalDraftActive = true, goalDraftText = text, goalInputState = GoalInputState.Idle)
        }
    }

    private fun updateGoalDraft(text: String) {
        clearGoalImprovementHistory()
        mutableState.update { it.copy(goalDraftText = text) }
    }

    private fun cancelGoalDraft() {
        goalInputJob?.cancel()
        goalInputJob = null
        micRecorder.release()
        clearGoalImprovementHistory()
        mutableState.update { it.copy(goalDraftActive = false, goalDraftText = "", goalInputState = GoalInputState.Idle) }
    }

    private fun toggleGoalRecording() {
        if (!mutableState.value.goalDraftActive) return
        when (mutableState.value.goalInputState) {
            GoalInputState.Idle -> {
                if (container.groqSchluesselAktiv().isBlank()) {
                    message("Bitte zuerst unter Einstellungen einen Groq-API-Schlüssel hinterlegen.")
                } else {
                    pendingSpeechTarget = SpeechInputTarget.Goal
                    sendEffect(StackLaborUiEffect.RequestMicrophonePermission)
                }
            }
            GoalInputState.Recording -> stopAndTranscribeGoal()
            GoalInputState.Transcribing, GoalInputState.Improving -> Unit
        }
    }

    private fun microphonePermissionResult(granted: Boolean) {
        val target = pendingSpeechTarget.also { pendingSpeechTarget = null } ?: return
        if (!granted) {
            return message(
                if (target == SpeechInputTarget.Goal) "Ohne Mikrofonberechtigung kann kein Ziel aufgenommen werden."
                else "Ohne Mikrofonberechtigung können keine Zusatzinformationen aufgenommen werden.",
            )
        }
        val canStart = when (target) {
            SpeechInputTarget.Goal -> mutableState.value.goalDraftActive && mutableState.value.goalInputState == GoalInputState.Idle
            SpeechInputTarget.AllStacks -> mutableState.value.allStacksSetupOpen && mutableState.value.allStacksInputState == GoalInputState.Idle
        }
        if (!canStart) return
        if (!micRecorder.start(viewModelScope)) {
            message("Die Mikrofonaufnahme konnte nicht gestartet werden.")
            return
        }
        mutableState.update {
            when (target) {
                SpeechInputTarget.Goal -> it.copy(goalInputState = GoalInputState.Recording)
                SpeechInputTarget.AllStacks -> it.copy(allStacksInputState = GoalInputState.Recording)
            }
        }
    }

    private fun stopAndTranscribeGoal() {
        if (goalInputJob?.isActive == true) return
        goalInputJob = viewModelScope.launch {
            mutableState.update { it.copy(goalInputState = GoalInputState.Transcribing) }
            try {
                val wav = micRecorder.stop()
                if (wav == null) {
                    message("Ich habe nichts verstanden.")
                    return@launch
                }
                val transcript = container.groqTranscriber.transcribe(container.groqSchluesselAktiv(), wav).trim()
                if (transcript.isBlank()) {
                    message("Ich habe nichts verstanden.")
                } else if (mutableState.value.goalDraftActive) {
                    clearGoalImprovementHistory()
                    mutableState.update { current ->
                        current.copy(
                            goalDraftText = listOf(current.goalDraftText.trim(), transcript)
                                .filter(String::isNotBlank)
                                .joinToString(" "),
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message(error.message ?: "Die Transkription ist fehlgeschlagen.")
            } finally {
                goalInputJob = null
                mutableState.update { it.copy(goalInputState = GoalInputState.Idle) }
            }
        }
    }

    private fun improveGoalDraft() {
        val displayedAtStart = mutableState.value.goalDraftText
        val source = goalDraftBeforeImprovement ?: displayedAtStart.trim()
        if (source.isBlank() || mutableState.value.goalInputState != GoalInputState.Idle) return
        if (container.oauth.state.value is CodexAuthState.SignedOut) {
            return message("Bitte zuerst unter Einstellungen bei Codex anmelden.")
        }
        goalInputJob = viewModelScope.launch {
            mutableState.update { it.copy(goalInputState = GoalInputState.Improving) }
            try {
                val improved = container.goalTextImprover.improve(source, improvedGoalVersions)
                if (mutableState.value.goalDraftActive && mutableState.value.goalDraftText == displayedAtStart) {
                    if (goalDraftBeforeImprovement == null) goalDraftBeforeImprovement = source
                    improvedGoalVersions += improved
                    mutableState.update { current ->
                        if (current.goalDraftActive && current.goalDraftText == displayedAtStart) {
                            current.copy(goalDraftText = improved, canUndoGoalImprovement = true)
                        } else current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message(error.message ?: "Der Zieltext konnte nicht verbessert werden.")
            } finally {
                goalInputJob = null
                mutableState.update { it.copy(goalInputState = GoalInputState.Idle) }
            }
        }
    }

    private fun undoGoalImprovement() {
        val original = goalDraftBeforeImprovement ?: return
        goalDraftBeforeImprovement = null
        mutableState.update { it.copy(goalDraftText = original, canUndoGoalImprovement = false) }
    }

    private fun clearGoalImprovementHistory() {
        goalDraftBeforeImprovement = null
        improvedGoalVersions.clear()
        mutableState.update { it.copy(canUndoGoalImprovement = false) }
    }

    private fun openAllStacksSetup(reset: Boolean) {
        if (stacks.isEmpty()) return message("Keine Stacks vorhanden")
        if (reset) {
            goalInputJob?.cancel()
            micRecorder.release()
            pendingSpeechTarget = null
            allStacksDraftBeforeImprovement = null
            improvedAllStacksVersions.clear()
        }
        mutableState.update { current ->
            val selectedIds = if (reset) stacks.mapTo(linkedSetOf()) { stack -> stack.id } else current.selectedAllStackIds
            current.copy(
                allStacksSetupOpen = true,
                selectedAllStackIds = selectedIds,
                allStacksAdditionalInfo = if (reset) "" else current.allStacksAdditionalInfo,
                allStacksInputState = GoalInputState.Idle,
                canUndoAllStacksImprovement = false,
                allStackGroups = stackEvaluationGroups(selectedIds, stacks, entries, medicines, stackGoals, goals, settings.dosisVariante),
            )
        }
    }

    private fun dismissAllStacksSetup() {
        if (!mutableState.value.allStacksSetupOpen) return
        goalInputJob?.cancel()
        goalInputJob = null
        micRecorder.release()
        if (pendingSpeechTarget == SpeechInputTarget.AllStacks) pendingSpeechTarget = null
        clearAllStacksImprovementHistory()
        mutableState.update { it.copy(allStacksSetupOpen = false, allStacksInputState = GoalInputState.Idle) }
    }

    private fun toggleAllStackSelection(stackId: String) {
        mutableState.update { current ->
            val selected = current.selectedAllStackIds.toMutableSet()
            if (!selected.add(stackId)) selected.remove(stackId)
            current.copy(
                selectedAllStackIds = selected,
                allStackGroups = stackEvaluationGroups(selected, stacks, entries, medicines, stackGoals, goals, settings.dosisVariante),
            )
        }
    }

    private fun updateAllStacksAdditionalInfo(text: String) {
        clearAllStacksImprovementHistory()
        mutableState.update { it.copy(allStacksAdditionalInfo = text) }
    }

    private fun toggleAllStacksRecording() {
        if (!mutableState.value.allStacksSetupOpen) return
        when (mutableState.value.allStacksInputState) {
            GoalInputState.Idle -> {
                if (container.groqSchluesselAktiv().isBlank()) {
                    message("Bitte zuerst unter Einstellungen einen Groq-API-Schlüssel hinterlegen.")
                } else {
                    pendingSpeechTarget = SpeechInputTarget.AllStacks
                    sendEffect(StackLaborUiEffect.RequestMicrophonePermission)
                }
            }
            GoalInputState.Recording -> stopAndTranscribeAllStacksInfo()
            GoalInputState.Transcribing, GoalInputState.Improving -> Unit
        }
    }

    private fun stopAndTranscribeAllStacksInfo() {
        if (goalInputJob?.isActive == true) return
        goalInputJob = viewModelScope.launch {
            mutableState.update { it.copy(allStacksInputState = GoalInputState.Transcribing) }
            try {
                val wav = micRecorder.stop()
                if (wav == null) {
                    message("Ich habe nichts verstanden.")
                    return@launch
                }
                val transcript = container.groqTranscriber.transcribe(container.groqSchluesselAktiv(), wav).trim()
                if (transcript.isBlank()) {
                    message("Ich habe nichts verstanden.")
                } else if (mutableState.value.allStacksSetupOpen) {
                    clearAllStacksImprovementHistory()
                    mutableState.update { current ->
                        current.copy(
                            allStacksAdditionalInfo = listOf(current.allStacksAdditionalInfo.trim(), transcript)
                                .filter(String::isNotBlank)
                                .joinToString(" "),
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message(error.message ?: "Die Transkription ist fehlgeschlagen.")
            } finally {
                goalInputJob = null
                mutableState.update { it.copy(allStacksInputState = GoalInputState.Idle) }
            }
        }
    }

    private fun improveAllStacksAdditionalInfo() {
        val displayedAtStart = mutableState.value.allStacksAdditionalInfo
        val source = allStacksDraftBeforeImprovement ?: displayedAtStart.trim()
        if (source.isBlank() || mutableState.value.allStacksInputState != GoalInputState.Idle) return
        if (container.oauth.state.value is CodexAuthState.SignedOut) {
            return message("Bitte zuerst unter Einstellungen bei Codex anmelden.")
        }
        goalInputJob = viewModelScope.launch {
            mutableState.update { it.copy(allStacksInputState = GoalInputState.Improving) }
            try {
                val improved = container.goalTextImprover.improveAdditionalInformation(source, improvedAllStacksVersions)
                if (mutableState.value.allStacksSetupOpen && mutableState.value.allStacksAdditionalInfo == displayedAtStart) {
                    if (allStacksDraftBeforeImprovement == null) allStacksDraftBeforeImprovement = source
                    improvedAllStacksVersions += improved
                    mutableState.update { current ->
                        if (current.allStacksSetupOpen && current.allStacksAdditionalInfo == displayedAtStart) {
                            current.copy(allStacksAdditionalInfo = improved, canUndoAllStacksImprovement = true)
                        } else current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message(error.message ?: "Die Zusatzinformationen konnten nicht verbessert werden.")
            } finally {
                goalInputJob = null
                mutableState.update { it.copy(allStacksInputState = GoalInputState.Idle) }
            }
        }
    }

    private fun undoAllStacksImprovement() {
        val original = allStacksDraftBeforeImprovement ?: return
        allStacksDraftBeforeImprovement = null
        mutableState.update { it.copy(allStacksAdditionalInfo = original, canUndoAllStacksImprovement = false) }
    }

    private fun clearAllStacksImprovementHistory() {
        allStacksDraftBeforeImprovement = null
        improvedAllStacksVersions.clear()
        mutableState.update { it.copy(canUndoAllStacksImprovement = false) }
    }

    private fun confirmEvaluateAll() {
        val selectedIds = mutableState.value.selectedAllStackIds
        if (selectedIds.isEmpty()) return message("Bitte mindestens einen Stack auswählen.")
        val additionalInformation = mutableState.value.allStacksAdditionalInfo.trim()
        dismissAllStacksSetup()
        mutableState.update {
            it.copy(
                selectedAllStackIds = selectedIds,
                allStacksAdditionalInfo = additionalInformation,
                dailyDoses = dailyDoses(
                    entries.filter { entry -> entry.stackId in selectedIds },
                    medicines,
                    stacks.filter { stack -> stack.id in selectedIds },
                    settings.dosisVariante,
                ),
                allStackGroups = stackEvaluationGroups(selectedIds, stacks, entries, medicines, stackGoals, goals, settings.dosisVariante),
            )
        }
        evaluateAll(selectedIds, additionalInformation)
    }

    private fun buildBreakdownItems(
        subjectId: String,
        evaluation: BewertungMitDetails?,
        goalItems: List<GoalUi>,
        medicineItems: List<MedicineUi>,
    ): List<RelationshipUi> {
        val cells = evaluation?.zellen.orEmpty()
        return if (medicineItems.any { it.id == subjectId }) {
            goalItems.filter { it.selected }.map { goal ->
                val cell = cells.firstOrNull { it.mittelId == subjectId && it.zielId == goal.id }
                RelationshipUi(
                    id = goal.id,
                    rank = goal.rank,
                    title = goal.text,
                    signal = when (cell?.wirkung?.name) {
                        "STUETZT" -> SignalState.Green
                        "STOERT" -> SignalState.Red
                        else -> SignalState.Gray
                    },
                    reason = cell?.grund.orEmpty().mitDeutschenUmlauten(),
                )
            }
        } else {
            medicineItems.mapIndexed { index, medicine ->
                val cell = cells.firstOrNull { it.mittelId == medicine.id && it.zielId == subjectId }
                RelationshipUi(
                    id = medicine.id,
                    rank = index + 1,
                    title = medicine.name,
                    signal = when (cell?.wirkung?.name) {
                        "STUETZT" -> SignalState.Green
                        "STOERT" -> SignalState.Red
                        else -> SignalState.Gray
                    },
                    reason = cell?.grund.orEmpty(),
                )
            }
        }
    }

    private fun toggleGoal(stackId: String, goalId: String) = launchAction {
        val selected = stackGoals.filter { it.stackId == stackId }
        if (selected.any { it.zielId == goalId }) repository.entferneStackZiel(stackId, goalId)
        else repository.setzeStackZiel(StackZiel(stackId, goalId, (selected.maxOfOrNull { it.rang } ?: 0) + 1))
    }

    private fun reorderGoal(stackId: String, goalId: String, targetRank: Int) = launchAction {
        val ordered = stackGoals.filter { it.stackId == stackId }.sortedBy { it.rang }.map { it.zielId }.toMutableList()
        if (ordered.remove(goalId)) {
            ordered.add((targetRank - 1).coerceIn(0, ordered.size), goalId)
            repository.sortiereZiele(stackId, ordered)
        }
    }

    /** Ein gesperrter Stack ist festgeschrieben — jede ändernde Aktion wird hier abgefangen. */
    private fun istGesperrt(stackId: String?): Boolean =
        stackId != null && stacks.firstOrNull { it.id == stackId }?.gesperrt == true

    /** Meldet die Sperre und sagt zugleich, ob die Aktion abgebrochen werden muss. */
    private fun gesperrtGemeldet(stackId: String?): Boolean {
        if (!istGesperrt(stackId)) return false
        message("Der Stack ist gesperrt. Öffne zuerst das Schloss.")
        return true
    }

    private fun toggleStackLock(stackId: String) {
        val stack = stacks.firstOrNull { it.id == stackId } ?: return
        // Ist der Fingerabdruck eingeschaltet, darf das Schloss nur nach einer Prüfung umspringen.
        if (settings.fingerabdruckAktiv) {
            wartenderSchlossStack = stackId
            requestFingerprint(
                FingerprintPurpose.StackLock,
                if (stack.gesperrt) "Stack entsperren" else "Stack sperren",
                "Bitte mit dem Fingerabdruck bestätigen.",
            )
            return
        }
        setzeSchloss(stackId)
    }

    private fun setzeSchloss(stackId: String) = launchAction {
        val stack = stacks.firstOrNull { it.id == stackId } ?: return@launchAction
        repository.setzeStackGesperrt(stackId, !stack.gesperrt)
        message(if (stack.gesperrt) "Stack entsperrt" else "Stack gesperrt")
    }

    private fun toggleFingerprint() {
        val einschalten = !settings.fingerabdruckAktiv
        requestFingerprint(
            if (einschalten) FingerprintPurpose.Enable else FingerprintPurpose.Disable,
            if (einschalten) "Fingerabdruck einrichten" else "Fingerabdruck abschalten",
            "Bitte einmal mit dem Fingerabdruck bestätigen.",
        )
    }

    private fun requestFingerprint(purpose: FingerprintPurpose, title: String, subtitle: String) {
        sendEffect(StackLaborUiEffect.RequestFingerprint(purpose, title, subtitle))
    }

    private fun fingerprintResult(purpose: FingerprintPurpose, granted: Boolean, hinweis: String) {
        if (!granted) {
            wartenderSchlossStack = null
            if (hinweis.isNotBlank()) message(hinweis)
            return
        }
        when (purpose) {
            FingerprintPurpose.AppStart -> mutableState.update { it.copy(appLocked = false) }
            FingerprintPurpose.Enable -> launchAction("Fingerabdruck eingeschaltet") {
                container.settings.setzeFingerabdruckAktiv(true)
            }
            FingerprintPurpose.Disable -> launchAction("Fingerabdruck abgeschaltet") {
                container.settings.setzeFingerabdruckAktiv(false)
            }
            FingerprintPurpose.StackLock -> wartenderSchlossStack?.let { id ->
                wartenderSchlossStack = null
                setzeSchloss(id)
            }
        }
    }

    private fun saveStack(stackId: String?) = launchAction("Stack gespeichert") {
        val id = stackId ?: UUID.randomUUID().toString()
        val position = stacks.firstOrNull { it.id == id }?.sortierung ?: ((stacks.maxOfOrNull { it.sortierung } ?: 0) + 1)
        val gesperrt = stacks.firstOrNull { it.id == id }?.gesperrt ?: false
        repository.speichereStack(Stack(id, stackDraft.name.trim(), stackDraft.time.trim(), stackDraft.note.trim(), position, gesperrt))
        selectedStackId.value = id
    }

    private fun prepareMedicineDraft(medicineId: String?, stackId: String?) {
        val medicine = medicineId?.let { id -> medicines.firstOrNull { it.id == id } }
        val entry = medicineId?.let { id -> currentEntries.firstOrNull { it.stackId == stackId && it.mittelId == id } }
        val free = entry?.dosen?.firstOrNull { it.variante == DosisVariante.FREI }
        val duty = entry?.dosen?.firstOrNull { it.variante == DosisVariante.DIENST }
        medicineDraft = MedicineDraft(
            medicineId = medicine?.id,
            name = medicine?.name.orEmpty(),
            solubility = medicine?.loeslichkeit ?: Loeslichkeit.WASSER,
            form = medicine?.darreichungsform ?: Darreichungsform.KAPSEL,
            formText = medicine?.darreichungsformAnzeige() ?: "Kapsel",
            manufacturer = medicine?.hersteller.orEmpty(),
            diarrheaRisk = medicine?.durchfallrisiko ?: false,
            excipients = medicine?.beistoffe.orEmpty(),
            pieces = free?.stueckzahl ?: BigDecimal.ONE,
            amount = free?.mengeJeStueck,
            unit = free?.einheitAnzeige().orEmpty(),
            secondDose = duty != null,
            frequencyType = entry?.frequenzTyp ?: FrequenzTyp.TAEGLICH,
            everyDays = entry?.alleNTage,
            frequencyText = entry?.frequenzAnzeige() ?: "Täglich",
            together = entry?.gruppeId != null,
            note = entry?.zusatztext.orEmpty(),
            alternates = entry?.alternationsPartnerMittelIds?.joinToString(", ") { id -> medicines.firstOrNull { it.id == id }?.name ?: id }.orEmpty(),
            solubilityAiDetermined = medicine?.loeslichkeitKiErmittelt ?: false,
        )
        mutableState.update { it.copy(medicineName = medicineDraft.name) }
        if (medicineDraft.solubilityAiDetermined && medicineDraft.name.isNotBlank()) {
            mutableState.update {
                it.copy(
                    medicineSolubilityAiState = MedicineSolubilityAiState.Success(
                        medicineDraft.name.trim(),
                        medicineDraft.solubility.toSolubility(),
                    ),
                )
            }
        } else {
            scheduleSolubilityDetermination(medicineDraft.name)
        }
    }

    private fun scheduleSolubilityDetermination(value: String) {
        val name = value.trim()
        solubilityJob?.cancel()
        val generation = ++solubilityGeneration
        if (name.isBlank()) {
            mutableState.update { it.copy(medicineSolubilityAiState = MedicineSolubilityAiState.Idle) }
            return
        }
        mutableState.update { it.copy(medicineSolubilityAiState = MedicineSolubilityAiState.Loading(name)) }
        solubilityJob = viewModelScope.launch {
            delay(700L)
            try {
                val result = container.solubilityClassifier.determine(name)
                if (generation != solubilityGeneration || currentRoute !is StackLaborRoute.MedicineEdit || medicineDraft.name.trim() != name) return@launch
                medicineDraft = medicineDraft.copy(solubility = result, solubilityAiDetermined = true)
                val existing = medicineDraft.medicineId
                    ?.let { id -> medicines.firstOrNull { it.id == id } }
                    ?.takeIf { it.name.trim() == name }
                if (existing != null) {
                    val updated = existing.copy(loeslichkeit = result, loeslichkeitKiErmittelt = true)
                    repository.speichereMittel(updated)
                    medicines = medicines.map { if (it.id == updated.id) updated else it }
                }
                mutableState.update {
                    it.copy(medicineSolubilityAiState = MedicineSolubilityAiState.Success(name, result.toSolubility()))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: CodexException) {
                if (generation != solubilityGeneration) return@launch
                val message = when (error.kind) {
                    CodexErrorKind.REAUTH -> "Bitte zuerst bei Codex anmelden."
                    CodexErrorKind.QUOTA -> "Das Codex-Kontingent ist aktuell erschöpft."
                    CodexErrorKind.NETWORK -> error.message ?: "Codex ist aktuell nicht erreichbar."
                }
                mutableState.update { it.copy(medicineSolubilityAiState = MedicineSolubilityAiState.Error(name, message)) }
            } catch (_: Exception) {
                if (generation == solubilityGeneration) {
                    mutableState.update {
                        it.copy(medicineSolubilityAiState = MedicineSolubilityAiState.Error(name, "Die KI-Bestimmung ist aktuell nicht möglich."))
                    }
                }
            }
        }
    }

    private fun cancelSolubilityDetermination() {
        solubilityGeneration++
        solubilityJob?.cancel()
        solubilityJob = null
        mutableState.update { it.copy(medicineSolubilityAiState = MedicineSolubilityAiState.Idle) }
    }

    private fun saveMedicine(medicineId: String?, stackId: String?) = launchAction("Mittel gespeichert") {
        val id = medicineId ?: UUID.randomUUID().toString()
        repository.speichereMittel(
            Mittel(
                id = id,
                name = medicineDraft.name.trim(),
                loeslichkeit = medicineDraft.solubility,
                darreichungsform = medicineDraft.form,
                darreichungsformText = medicineDraft.formText.toDarreichungsformOrNull()?.anzeige()
                    ?: medicineDraft.formText.trim().mitDeutschenUmlauten().takeIf(String::isNotBlank),
                hersteller = medicineDraft.manufacturer.trim(),
                durchfallrisiko = medicineDraft.diarrheaRisk,
                beistoffe = medicineDraft.excipients.trim(),
                loeslichkeitKiErmittelt = medicineDraft.solubilityAiDetermined,
            ),
        )
        if (stackId != null) {
            val existing = currentEntries.firstOrNull { it.stackId == stackId && it.mittelId == id }
            val entryId = existing?.id ?: UUID.randomUUID().toString()
            val unitText = medicineDraft.unit.trim().takeIf(String::isNotBlank)
            val unit = unitText?.toEinheitOrNull()
            val doses = buildList {
                add(StackEintragDosis(entryId, DosisVariante.FREI, medicineDraft.pieces, medicineDraft.amount, unit, unitText))
                if (medicineDraft.secondDose) add(StackEintragDosis(entryId, DosisVariante.DIENST, medicineDraft.pieces, medicineDraft.amount, unit, unitText))
            }
            val alternationIds = medicineDraft.alternates.split(',').map(String::trim).filter(String::isNotBlank).map { token ->
                medicines.firstOrNull { it.id.equals(token, true) || it.name.equals(token, true) }?.id
                    ?: throw IllegalArgumentException("Alternationspartner nicht gefunden: $token")
            }.distinct()
            repository.speichereEintrag(
                StackEintrag(
                    id = entryId,
                    stackId = stackId,
                    mittelId = id,
                    frequenzTyp = medicineDraft.frequencyType,
                    alleNTage = medicineDraft.everyDays,
                    frequenzText = medicineDraft.frequencyText.trim().takeIf(String::isNotBlank),
                    aktiv = existing?.aktiv ?: true,
                    reihenfolge = existing?.reihenfolge ?: ((currentEntries.maxOfOrNull { it.reihenfolge } ?: 0) + 1),
                    gruppeId = if (medicineDraft.together) existing?.gruppeId ?: UUID.randomUUID().toString() else null,
                    zusatztext = medicineDraft.note,
                    offenerHinweis = existing?.offenerHinweis,
                    dosen = doses,
                    alternationsPartnerMittelIds = alternationIds,
                ),
            )
            if (existing == null) scheduleCompetitionCheck(stackId, id)
        }
    }

    private fun addMedicineToStack(stackId: String, medicineId: String) = launchAction("Mittel zum Stack hinzugefügt") {
        if (entries.any { it.stackId == stackId && it.mittelId == medicineId }) return@launchAction
        val id = UUID.randomUUID().toString()
        repository.speichereEintrag(
            StackEintrag(
                id = id,
                stackId = stackId,
                mittelId = medicineId,
                reihenfolge = entries.filter { it.stackId == stackId }.maxOfOrNull { it.reihenfolge }?.plus(1) ?: 1,
                dosen = listOf(StackEintragDosis(id, DosisVariante.FREI, BigDecimal.ONE, null, null)),
            ),
        )
        scheduleCompetitionCheck(stackId, medicineId)
    }

    private fun scheduleCompetitionCheck(stackId: String, medicineId: String) {
        pendingCompetitionIds.getOrPut(stackId) { linkedSetOf() } += medicineId
        competitionJobs.remove(stackId)?.cancel()
        competitionJobs[stackId] = viewModelScope.launch {
            val pendingEntries = repository.ladeStackInhalt(stackId).eintraege
                .filter { it.mittelId in pendingCompetitionIds[stackId].orEmpty() }
            pendingEntries.forEach { repository.setzeOffenenHinweis(it.id, "Konkurrenzprüfung läuft …") }
            delay(3_000L)
            val checkedIds = pendingCompetitionIds[stackId].orEmpty().toSet()
            try {
                val details = container.evaluator.evaluateStack(stackId, persist = false)
                val ranks = stackGoals.filter { it.stackId == stackId }.associate { it.zielId to it.rang }
                val current = repository.ladeStackInhalt(stackId).eintraege
                current.filter { it.mittelId in checkedIds }.forEach { entry ->
                    val cells = details.zellen.filter { it.mittelId == entry.mittelId }
                    val supports = cells.filter { it.wirkung.name == "STUETZT" }.mapNotNull { ranks[it.zielId] }.sorted()
                    val disrupts = cells.filter { it.wirkung.name == "STOERT" }.mapNotNull { ranks[it.zielId] }.sorted()
                    val competition = details.konkurrenzen.firstOrNull {
                        it.mittelAId == entry.mittelId || it.mittelBId == entry.mittelId
                    }?.grund
                    val hint = buildList {
                        if (supports.isNotEmpty()) add("stützt Ziel ${supports.joinToString(", ")}")
                        if (disrupts.isNotEmpty()) add("stört Ziel ${disrupts.joinToString(", ")}")
                        if (!competition.isNullOrBlank()) add(competition)
                    }.joinToString(" · ").ifBlank { "Keine Konkurrenz zu den gewählten Zielen erkannt" }
                    repository.setzeOffenenHinweis(entry.id, hint)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                repository.ladeStackInhalt(stackId).eintraege
                    .filter { it.mittelId in checkedIds }
                    .forEach { repository.setzeOffenenHinweis(it.id, "Konkurrenzprüfung nicht möglich — beim nächsten Auswerten") }
            } finally {
                pendingCompetitionIds.remove(stackId)
                competitionJobs.remove(stackId)
            }
        }
    }

    private fun removeQuestion(questionId: String) = launchAction("Frage entfernt") {
        val question = mutableState.value.questions.firstOrNull { it.id == questionId }
        val stackId = selectedStackId.value
        repository.loescheFrage(questionId)
        if (question != null && stackId != null) undoAction = { repository.speichereFrage(EigeneFrage(question.id, stackId, question.text)) }
    }

    private fun removeMedicineFromStack(stackId: String, medicineId: String) = launchAction("Mittel entfernt") {
        val entry = currentEntries.firstOrNull { it.stackId == stackId && it.mittelId == medicineId } ?: return@launchAction
        repository.loescheEintrag(entry.id)
        undoAction = { repository.speichereEintrag(entry) }
    }

    private fun reorderMedicine(stackId: String, medicineId: String, targetIndex: Int) = launchAction {
        val orderedEntries = currentEntries.filter { it.stackId == stackId }.sortedBy { it.reihenfolge }
        val dragged = orderedEntries.firstOrNull { it.mittelId == medicineId } ?: return@launchAction
        val moving = if (dragged.gruppeId == null) listOf(dragged) else orderedEntries.filter { it.gruppeId == dragged.gruppeId }
        val remaining = orderedEntries.filterNot { candidate -> moving.any { it.id == candidate.id } }.toMutableList()
        remaining.addAll(targetIndex.coerceIn(0, remaining.size), moving)
        repository.sortiereEintraege(stackId, remaining.map { it.id })
    }

    /**
     * Übernimmt eine komplett gezogene Reihenfolge.
     *
     * Anders als [reorderMedicine] arbeitet das nicht mit einem Zielindex, sondern mit der
     * ganzen Liste — damit stimmt das Ergebnis auch dann, wenn die Anzeige gerade anders
     * sortiert ist als die gespeicherte Einnahme-Reihenfolge. Kombi-Gruppen bleiben fachlich
     * erhalten, beeinflussen aber nicht die vom Nutzer festgelegte Position.
     */
    private fun applyMedicineOrder(stackId: String, medicineIds: List<String>) = launchAction {
        val stackEntries = currentEntries.filter { it.stackId == stackId }
        if (stackEntries.isEmpty()) return@launchAction
        val byMedicine = stackEntries.associateBy { it.mittelId }
        val gezogen = medicineIds.mapNotNull { byMedicine[it] }
        // Was nicht in der Anzeige war (z.B. wegen der Suche) haengt hinten dran.
        val rest = stackEntries.filterNot { entry -> gezogen.any { it.id == entry.id } }.sortedBy { it.reihenfolge }
        repository.sortiereEintraege(stackId, (gezogen + rest).map { it.id })
    }

    private fun mergeMedicines(keepId: String, replaceId: String) = launchAction("Mittel zusammengeführt") {
        val backup = repository.exportiere()
        repository.fuehreMittelZusammen(keepId, replaceId)
        undoAction = { repository.importiere(backup, ImportModus.ERSETZEN) }
    }

    private fun undo() {
        val action = undoAction ?: return message("Nichts zum Rückgängigmachen")
        undoAction = null
        launchAction("Änderung rückgängig gemacht") { action() }
    }

    private fun selectHistory(runId: String) {
        if (!selectedHistoryIds.remove(runId)) {
            if (selectedHistoryIds.size == 2) selectedHistoryIds.remove(selectedHistoryIds.first())
            selectedHistoryIds += runId
        }
        mutableState.update { current ->
            val selected = currentHistory.filter { it.bewertung.id in selectedHistoryIds }
            val comparison = if (selected.size == 2) {
                val left = selected[0].zellen.map { listOf(it.mittelId, it.zielId, it.wirkung.name, it.staerke.toString(), it.grund) }.toSet()
                val right = selected[1].zellen.map { listOf(it.mittelId, it.zielId, it.wirkung.name, it.staerke.toString(), it.grund) }.toSet()
                val changedCells = (left - right).size + (right - left).size
                "$changedCells geänderte Bewertungszuordnungen"
            } else ""
            val changes = if (selected.size == 2) buildHistoryChanges(selected, current.goals) else emptyList()
            current.copy(
                history = current.history.map { it.copy(selectedForComparison = it.id in selectedHistoryIds) },
                historyComparison = comparison,
                historyChanges = changes,
            )
        }
    }

    private fun buildHistoryChanges(selected: List<BewertungMitDetails>, goalItems: List<GoalUi>): List<HistoryChangeUi> {
        val (older, newer) = selected.sortedBy { it.bewertung.zeitpunktEpochMillis }
        val goalIds = (older.zellen.map { it.zielId } + newer.zellen.map { it.zielId }).distinct()
        return goalIds.mapNotNull { goalId ->
            val from = older.zellen.filter { it.zielId == goalId }.historySignal()
            val to = newer.zellen.filter { it.zielId == goalId }.historySignal()
            if (from == to) return@mapNotNull null
            val goal = goalItems.firstOrNull { it.id == goalId }
            HistoryChangeUi(
                title = goal?.let { "Ziel ${it.rank}: ${it.text}" } ?: "Ziel",
                from = from,
                to = to,
            )
        }
    }

    private fun evaluate(stackId: String?) {
        if (stackId == null) return message("Kein Stack ausgewählt")
        // Ohne Ziele wird trotzdem geprüft — dann eben die Zusammenstellung selbst.
        evaluationJob?.cancel()
        evaluationJob = viewModelScope.launch {
            mutableState.update { it.copy(evaluationState = EvaluationState.Running, streamedEvaluationText = "") }
            try {
                val details = container.evaluator.evaluateStack(stackId) { event ->
                    when (event) {
                        is CodexStreamEvent.Stage -> mutableState.update { it.copy(streamedEvaluationText = event.text.mitDeutschenUmlauten()) }
                        is CodexStreamEvent.NarrativeDelta -> mutableState.update {
                            it.copy(streamedEvaluationText = (it.streamedEvaluationText + event.text).mitDeutschenUmlauten())
                        }
                        CodexStreamEvent.NetworkRetry -> mutableState.update { it.copy(streamedEvaluationText = "Netzwerk unterbrochen, neuer Versuch …") }
                        is CodexStreamEvent.RawDelta -> Unit
                    }
                }
                showEvaluation(details)
                mutableState.update { it.copy(evaluationState = EvaluationState.Valid) }
                message("Auswertung gespeichert")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: CodexException) {
                mutableState.update {
                    it.copy(evaluationState = when (error.kind) {
                        CodexErrorKind.REAUTH -> EvaluationState.Reauth
                        CodexErrorKind.QUOTA -> EvaluationState.Quota
                        CodexErrorKind.NETWORK -> EvaluationState.NetworkError
                    })
                }
                message(error.message ?: "Auswertung fehlgeschlagen")
            } catch (error: Exception) {
                mutableState.update { it.copy(evaluationState = EvaluationState.NetworkError) }
                message(error.message ?: "Auswertung fehlgeschlagen")
            } finally {
                evaluationJob = null
            }
        }
    }

    private fun evaluateAll(stackIds: Set<String>, additionalInformation: String) {
        if (stackIds.isEmpty()) return message("Keine Stacks ausgewählt")
        evaluationJob?.cancel()
        evaluationJob = viewModelScope.launch {
            mutableState.update { it.copy(allStacksEvaluationState = EvaluationState.Running, streamedEvaluationText = "") }
            try {
                val details = container.evaluator.evaluateAllStacks(stackIds, additionalInformation) { event ->
                    when (event) {
                        is CodexStreamEvent.Stage -> mutableState.update { it.copy(streamedEvaluationText = event.text.mitDeutschenUmlauten()) }
                        is CodexStreamEvent.NarrativeDelta -> mutableState.update {
                            it.copy(streamedEvaluationText = (it.streamedEvaluationText + event.text).mitDeutschenUmlauten())
                        }
                        CodexStreamEvent.NetworkRetry -> mutableState.update { it.copy(streamedEvaluationText = "Netzwerk unterbrochen, neuer Versuch …") }
                        is CodexStreamEvent.RawDelta -> Unit
                    }
                }
                tagEvaluation = details
                showEvaluation(details)
                applyTagEvaluation(details)
                mutableState.update { it.copy(allStacksEvaluationState = EvaluationState.Valid) }
                message("Alle Stacks wurden gemeinsam ausgewertet")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(allStacksEvaluationState = EvaluationState.NetworkError) }
                message(error.message ?: "Gesamtprüfung fehlgeschlagen")
            } finally {
                evaluationJob = null
            }
        }
    }

    private fun startCodexLogin() {
        viewModelScope.launch {
            mutableState.update { it.copy(codexLoginState = CodexLoginState.Waiting, codexDeviceCode = "") }
            try {
                val session = container.oauth.startDeviceAuthorization()
                mutableState.update { it.copy(codexDeviceCode = session.userCode, codexVerificationUrl = session.verificationUri) }
                container.oauth.finishDeviceAuthorization(session)
                mutableState.update { it.copy(codexLoginState = CodexLoginState.Success) }
                message("Codex-Anmeldung bestätigt")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: CodexException) {
                mutableState.update {
                    it.copy(codexLoginState = if (error.message?.contains("abgelaufen", true) == true) CodexLoginState.Expired else CodexLoginState.Denied)
                }
                message(error.message ?: "Codex-Anmeldung fehlgeschlagen")
            } catch (error: Exception) {
                mutableState.update { it.copy(codexLoginState = CodexLoginState.NetworkError) }
                message(error.message ?: "Codex ist nicht erreichbar")
            }
        }
    }

    private fun selectSetting(id: String) = launchAction {
        when (id) {
            "tts-provider" -> container.settings.setzeTtsAnbieter(settings.ttsAnbieter.next())
            "tts-voice" -> {
                val catalogue = TtsVoices.of(settings.ttsAnbieter.toProviderId())
                if (catalogue.isNotEmpty()) {
                    val next = catalogue.indexOfFirst { it.id == settings.ttsStimme }.plus(1) % catalogue.size
                    container.settings.setzeTtsStimme(catalogue[next].id)
                }
            }
            "tts-speed" -> container.settings.setzeTtsTempo(nextSpeed(settings.ttsTempo))
            "tts-pause" -> container.settings.setzeAbsatzpause(settings.absatzpause.next())
            "tts-timeout" -> container.settings.setzeAbschaltzeitMinuten(when (settings.abschaltzeitMinuten) { 15 -> 30; 30 -> 60; else -> 15 })
            "tts-usage" -> message("Verbrauch ${mutableState.value.ttsUsageLabel}")
            "codex-model" -> container.settings.setzeCodexModell(when (settings.codexModell) { "gpt-5.6-sol" -> "gpt-5.6-terra"; "gpt-5.6-terra" -> "gpt-5.6-luna"; else -> "gpt-5.6-sol" })
            "codex-reasoning" -> container.settings.setzeCodexDenkstufe(when (settings.codexDenkstufe) { "low" -> "medium"; "medium" -> "high"; "high" -> "xhigh"; "xhigh" -> "max"; else -> "low" })
            "reduced-motion" -> container.settings.setzeBewegungReduziert(!settings.bewegungReduziert)
        }
    }

    private fun selectSettingValue(id: String, value: String) = launchAction {
        when (id) {
            "tts-provider" -> {
                val provider = TtsAnbieter.entries.firstOrNull { it.label() == value } ?: TtsAnbieter.EDGE
                container.settings.setzeTtsAnbieter(provider)
                // Die bisherige Stimme gehört zum alten Anbieter — auf dessen erste Stimme wechseln.
                val catalogue = TtsVoices.of(provider.toProviderId())
                container.settings.setzeTtsStimme(
                    when {
                        catalogue.any { it.id == settings.ttsStimme } -> settings.ttsStimme
                        catalogue.isNotEmpty() -> catalogue.first().id
                        else -> container.qwenStimmeAktiv()
                    },
                )
                if (provider == TtsAnbieter.GOOGLE_CLOUD && container.googleSchluesselAktiv().isBlank()) {
                    message("Für Google Chirp 3 HD fehlt noch der API-Schlüssel.")
                }
                if (provider == TtsAnbieter.QWEN_CLONE) {
                    if (container.qwenSchluesselAktiv().isBlank()) message("Für die eigene Stimme fehlt noch der Alibaba-Schlüssel.")
                    else loadClonedVoices()
                }
            }
            "tts-voice" -> {
                val voiceId = TtsVoices.of(settings.ttsAnbieter.toProviderId()).firstOrNull { it.label == value }?.id
                    ?: clonedVoices.firstOrNull { it.voiceLabel() == value }?.id
                if (voiceId != null) container.settings.setzeTtsStimme(voiceId)
                if (settings.ttsAnbieter == TtsAnbieter.QWEN_CLONE && voiceId != null) {
                    container.settings.setzeQwenStimmenId(voiceId)
                }
            }
            "tts-speed" -> container.settings.setzeTtsTempo(value.removeSuffix("×").replace(',', '.').toFloat())
            "tts-pause" -> container.settings.setzeAbsatzpause(when (value) {
                "Mittel" -> Absatzpause.MITTEL
                "Lang" -> Absatzpause.LANG
                else -> Absatzpause.KURZ
            })
            "tts-timeout" -> container.settings.setzeAbschaltzeitMinuten(value.substringBefore(' ').toInt())
            "codex-model" -> container.settings.setzeCodexModell("gpt-5.6-${value.lowercase()}")
            "codex-reasoning" -> container.settings.setzeCodexDenkstufe(when (value) {
                "Niedrig" -> "low"
                "Mittel" -> "medium"
                "Sehr hoch" -> "xhigh"
                "Maximal" -> "max"
                else -> "high"
            })
        }
    }

    private fun exportStack(stackId: String) = launchAction {
        val stack = stacks.firstOrNull { it.id == stackId } ?: return@launchAction message("Der Stack wurde nicht gefunden.")
        val jetzt = System.currentTimeMillis()
        val text = StackTextExport.einzelnerStack(
            daten = exportDaten(stack),
            variante = settings.dosisVariante,
            versionName = BuildConfig.VERSION_NAME,
            zeitpunktEpochMillis = jetzt,
        )
        effectChannel.send(
            StackLaborUiEffect.CreateTextDocument(StackTextExport.dateiname(stack.name, jetzt), text),
        )
    }

    private fun exportAllStacks() = launchAction {
        if (stacks.isEmpty()) return@launchAction message("Es gibt noch keine Stacks zum Exportieren.")
        val jetzt = System.currentTimeMillis()
        val text = StackTextExport.alleStacks(
            daten = stacks.sortedBy { it.sortierung }.map(::exportDaten),
            variante = settings.dosisVariante,
            versionName = BuildConfig.VERSION_NAME,
            zeitpunktEpochMillis = jetzt,
        )
        effectChannel.send(
            StackLaborUiEffect.CreateTextDocument(StackTextExport.dateiname("alle-stacks", jetzt), text),
        )
    }

    /** Sammelt Einträge, Mittel und Ziele eines Stacks aus dem laufenden Datenbestand. */
    private fun exportDaten(stack: Stack): StackExportDaten {
        val zielTexte = goals.associate { it.id to it.text }
        return StackExportDaten(
            stack = stack,
            eintraege = entries.filter { it.stackId == stack.id }.sortedBy { it.reihenfolge },
            mittelNachId = medicines.associateBy { it.id },
            ziele = stackGoals.filter { it.stackId == stack.id }
                .sortedBy { it.rang }
                .mapNotNull { ziel -> zielTexte[ziel.zielId]?.let { StackZielText(ziel.rang, it) } },
        )
    }

    private fun saveApiKey(keyId: String, value: String) = launchAction {
        when (keyId) {
            "google-api-key" -> {
                container.settings.setzeGoogleApiKey(value)
                message(if (value.isBlank()) "Google-Schlüssel gelöscht — es gilt wieder der aus dem SK-Ordner." else "Google-Schlüssel gespeichert")
            }
            "qwen-api-key" -> {
                container.settings.setzeQwenApiKey(value)
                message(if (value.isBlank()) "Alibaba-Schlüssel gelöscht — es gilt wieder der aus dem SK-Ordner." else "Alibaba-Schlüssel gespeichert")
                if (value.isNotBlank()) loadClonedVoices()
            }
            "groq-api-key" -> {
                container.settings.setzeGroqApiKey(value)
                message(if (value.isBlank()) "Groq-Schlüssel gelöscht" else "Groq-Schlüssel gespeichert")
            }
            else -> message("Unbekannter Schlüssel: $keyId")
        }
    }

    /**
     * Holt die im Alibaba-Konto hinterlegten geklonten Stimmen. Ohne diesen Schritt müsste die
     * 46 Zeichen lange Stimm-Kennung von Hand eingetippt werden.
     */
    private fun loadClonedVoices() {
        if (clonedVoicesJob?.isActive == true) return
        val key = container.qwenSchluesselAktiv()
        if (key.isBlank()) {
            mutableState.update { it.copy(clonedVoicesHint = "Zuerst den Alibaba-Schlüssel hinterlegen.") }
            return
        }
        clonedVoicesJob = viewModelScope.launch {
            mutableState.update { it.copy(clonedVoicesLoading = true, clonedVoicesHint = "Stimmen werden geladen …") }
            try {
                clonedVoices = container.qwenVoiceDirectory.list(key)
                mutableState.update { current ->
                    current.copy(
                        clonedVoicesLoading = false,
                        clonedVoicesHint = if (clonedVoices.isEmpty()) {
                            "Im Alibaba-Konto ist noch keine eigene Stimme hinterlegt."
                        } else {
                            "${clonedVoices.size} eigene Stimmen im Konto"
                        },
                        ttsVoiceOptions = voiceOptions(settings.ttsAnbieter),
                        ttsVoiceLabel = voiceLabel(settings.ttsStimme, settings.ttsAnbieter),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(clonedVoicesLoading = false, clonedVoicesHint = error.message ?: "Stimmen konnten nicht geladen werden.")
                }
            } finally {
                clonedVoicesJob = null
            }
        }
    }

    private fun voiceOptions(provider: TtsAnbieter): List<String> = when (provider) {
        TtsAnbieter.QWEN_CLONE -> clonedVoices.map { it.voiceLabel() }
        else -> TtsVoices.of(provider.toProviderId()).map { it.label }
    }

    private fun voiceLabel(voiceId: String, provider: TtsAnbieter): String = when (provider) {
        TtsAnbieter.QWEN_CLONE -> clonedVoices.firstOrNull { it.id == voiceId }?.voiceLabel()
            ?: container.qwenStimmeAktiv().takeIf(String::isNotBlank)?.let { "Eigene Stimme" }
            ?: "Keine Stimme gewählt"
        else -> TtsVoices.byId(voiceId)?.label ?: voiceId.substringAfterLast('-').removeSuffix("Neural")
    }

    /**
     * @param startParagraph ab welchem Absatz gelesen wird — beim Antippen eines Absatzes dessen Nummer.
     * @param fortsetzen ob eine pausierte Wiedergabe einfach weiterlaufen soll.
     */
    private fun playTts(startParagraph: Int = 0, fortsetzen: Boolean = true) {
        val text = mutableState.value.evaluationParagraphs.joinToString("\n\n").trim()
        if (text.isBlank()) return message("Es gibt noch keine Auswertung zum Vorlesen")
        val configuration = TtsConfiguration(
            provider = when (settings.ttsAnbieter) {
                TtsAnbieter.EDGE -> TtsProviderId.EDGE
                TtsAnbieter.GOOGLE_CLOUD -> TtsProviderId.GOOGLE_CLOUD
                TtsAnbieter.QWEN_CLONE -> TtsProviderId.QWEN
            },
            voiceId = settings.ttsStimme,
            speechRate = settings.ttsTempo.coerceIn(0.7f, 1.3f),
            paragraphPauseMillis = when (settings.absatzpause) { Absatzpause.KURZ -> 350L; Absatzpause.MITTEL -> 700L; Absatzpause.LANG -> 1_200L },
            autoStopMillis = settings.abschaltzeitMinuten * 60_000L,
        )
        if (fortsetzen && mutableState.value.playbackState == PlaybackState.Paused) sendEffect(StackLaborUiEffect.ResumeTts)
        else sendEffect(StackLaborUiEffect.PlayTts(text, configuration, startParagraph.coerceAtLeast(0)))
    }

    private fun showEvaluation(details: BewertungMitDetails) {
        mutableState.update {
            it.copy(
                evaluationParagraphs = details.bewertung.gesamt.paragraphs(),
                evaluationMeta = details.bewertung.meta(),
                streamedEvaluationText = details.bewertung.gesamt,
            )
        }
    }

    private fun launchAction(success: String? = null, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                if (success != null) message(success)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message(error.message ?: "Aktion fehlgeschlagen")
            }
        }
    }

    private fun sendEffect(effect: StackLaborUiEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun message(text: String) = sendEffect(StackLaborUiEffect.Message(text))

    private fun containerContext(): android.content.Context = container.applicationContext

    override fun onCleared() {
        micRecorder.release()
        super.onCleared()
    }

    private data class GlobalSnapshot(
        val database: Triple<List<Stack>, List<StackEintrag>, List<StackZiel>>,
        val catalog: Triple<List<Ziel>, List<Mittel>, AppEinstellungen>,
        val auth: CodexAuthState,
        val usage: TtsUsage,
    )

    private data class SelectedSnapshot(
        val stackId: String,
        val content: Triple<List<StackEintrag>, List<StackZiel>, List<EigeneFrage>>,
        val evaluation: Triple<BewertungMitDetails?, List<BewertungMitDetails>, StackSortierstatus>,
        val settings: AppEinstellungen,
        val medicines: List<Mittel>,
    )

    private enum class SpeechInputTarget { Goal, AllStacks }
}

class StackLaborViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(StackLaborViewModel::class.java))
        return StackLaborViewModel(container) as T
    }
}

private data class StackDraft(val name: String = "", val time: String = "", val note: String = "")

private data class MedicineDraft(
    val medicineId: String? = null,
    val name: String = "",
    val solubility: Loeslichkeit = Loeslichkeit.WASSER,
    val form: Darreichungsform = Darreichungsform.KAPSEL,
    val formText: String = "Kapsel",
    val manufacturer: String = "",
    val diarrheaRisk: Boolean = false,
    val excipients: String = "",
    val pieces: BigDecimal = BigDecimal.ONE,
    val amount: BigDecimal? = null,
    val unit: String = "",
    val secondDose: Boolean = false,
    val frequencyType: FrequenzTyp = FrequenzTyp.TAEGLICH,
    val everyDays: Int? = null,
    val frequencyText: String = "Täglich",
    val together: Boolean = false,
    val note: String = "",
    val alternates: String = "",
    val solubilityAiDetermined: Boolean = false,
) {
    fun with(field: String, value: String): MedicineDraft = when (field) {
        "name" -> copy(name = value)
        "solubility" -> copy(solubility = when (value) { "fat" -> Loeslichkeit.FETT; "both" -> Loeslichkeit.BEIDES; else -> Loeslichkeit.WASSER })
        "form" -> value.toDarreichungsformOrNull()?.let { copy(form = it, formText = it.anzeige()) }
            ?: copy(formText = value.mitDeutschenUmlauten())
        "manufacturer" -> copy(manufacturer = value)
        "diarrheaRisk" -> copy(diarrheaRisk = value.toBoolean())
        "excipients" -> copy(excipients = value)
        "pieces" -> copy(pieces = value.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: pieces)
        "amount" -> copy(amount = value.replace(',', '.').toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO })
        "unit" -> copy(unit = value)
        "secondDose" -> copy(secondDose = value.toBoolean())
        "frequency" -> {
            val tage = Regex("\\d+").find(value)?.value?.toIntOrNull()
            when {
                value.contains("alle", true) && tage != null -> copy(frequencyType = FrequenzTyp.ALLE_N_TAGE, everyDays = tage.coerceAtLeast(2), frequencyText = value)
                value.equals("Täglich", true) -> copy(frequencyType = FrequenzTyp.TAEGLICH, everyDays = null, frequencyText = value)
                else -> copy(frequencyText = value)
            }
        }
        "alternates" -> copy(alternates = value)
        "together" -> copy(together = value.toBoolean())
        "ai-note" -> copy(note = value)
        else -> this
    }
}

private fun StackLaborRoute.stackIdOrNull(): String? = when (this) {
    is StackLaborRoute.StackDetail -> stackId
    is StackLaborRoute.StackGoals -> stackId
    is StackLaborRoute.MedicineEdit -> stackId
    is StackLaborRoute.Breakdown -> stackId
    is StackLaborRoute.Questions -> stackId
    is StackLaborRoute.ReorderGoals -> stackId
    is StackLaborRoute.StackEdit -> stackId
    is StackLaborRoute.MedicineCatalog -> stackId
    is StackLaborRoute.History -> stackId
    else -> null
}

private fun Collection<Ampel>.toCounts(): SignalCounts = SignalCounts(count { it == Ampel.GRUEN }, count { it == Ampel.GELB }, count { it == Ampel.ROT })

private fun Mittel.toUi(entry: StackEintrag, signal: Ampel, variant: DosisVariante): MedicineUi {
    val dose = entry.dosen.firstOrNull { it.variante == variant } ?: entry.dosen.firstOrNull()
    val doseText = dose?.let {
        val pieces = it.stueckzahl.stripTrailingZeros().toPlainString()
        val form = darreichungsformAnzeige(it.stueckzahl)
        val amount = it.mengeJeStueck?.multiply(it.stueckzahl)?.stripTrailingZeros()?.toPlainString()
        val unit = it.einheitAnzeige().ifBlank { "Stück" }
        if (amount == null) "$pieces $form" else "$amount $unit · $pieces $form"
    }.orEmpty()
    return MedicineUi(
        id = id,
        name = name,
        dose = doseText,
        reason = entry.offenerHinweis.orEmpty(),
        solubility = loeslichkeit.toSolubility(),
        signal = signal.toSignalState(),
        active = entry.aktiv,
        manufacturer = hersteller,
        form = darreichungsformAnzeige(),
        diarrheaRisk = durchfallrisiko,
        excipients = beistoffe,
        pieces = dose?.stueckzahl?.stripTrailingZeros()?.toPlainString() ?: "1",
        amount = dose?.mengeJeStueck?.stripTrailingZeros()?.toPlainString().orEmpty(),
        unit = dose?.einheitAnzeige().orEmpty(),
        frequency = entry.frequenzAnzeige(),
        secondDose = entry.dosen.any { it.variante == DosisVariante.DIENST },
        together = entry.gruppeId != null,
        note = entry.zusatztext,
        alternates = entry.alternationsPartnerMittelIds.joinToString(", "),
    )
}

private fun dailyDoses(entries: List<StackEintrag>, medicines: List<Mittel>, stacks: List<Stack>, variant: DosisVariante): List<DoseSummaryUi> {
    val medicineById = medicines.associateBy { it.id }
    val stackById = stacks.associateBy { it.id }
    return entries.filter { it.aktiv }.groupBy { it.mittelId }.mapNotNull { (medicineId, grouped) ->
        val medicine = medicineById[medicineId] ?: return@mapNotNull null
        val doses = grouped.mapNotNull { entry -> entry.dosen.firstOrNull { it.variante == variant } ?: entry.dosen.firstOrNull() }
        val unit = doses.map { it.einheitAnzeige() }.filter(String::isNotBlank).distinct().singleOrNull()
        val total = if (unit == null || doses.any { it.mengeJeStueck == null }) null else grouped.sumOf { entry ->
            val dose = entry.dosen.firstOrNull { it.variante == variant } ?: entry.dosen.firstOrNull()
            val amount = dose?.mengeJeStueck?.multiply(dose.stueckzahl) ?: BigDecimal.ZERO
            if (entry.frequenzTyp == FrequenzTyp.ALLE_N_TAGE) {
                amount.divide(BigDecimal.valueOf((entry.alleNTage ?: 1).toLong()), 6, RoundingMode.HALF_UP)
            } else amount
        }
        DoseSummaryUi(
            medicine.name,
            if (total != null && unit != null) "${total.stripTrailingZeros().toPlainString()} $unit"
            else "${doses.sumOf { it.stueckzahl }.stripTrailingZeros().toPlainString()} ${unit ?: "Stück"}",
            grouped.mapNotNull { stackById[it.stackId]?.name }.distinct().joinToString(" · "),
        )
    }.sortedBy { it.name }
}

private fun stackEvaluationGroups(
    selectedStackIds: Set<String>,
    stacks: List<Stack>,
    entries: List<StackEintrag>,
    medicines: List<Mittel>,
    stackGoals: List<StackZiel>,
    goals: List<Ziel>,
    variant: DosisVariante,
): List<StackEvaluationGroupUi> {
    val includedIds = selectedStackIds.ifEmpty { stacks.mapTo(linkedSetOf()) { it.id } }
    val medicineById = medicines.associateBy { it.id }
    val goalById = goals.associateBy { it.id }
    return stacks.filter { it.id in includedIds }.sortedBy { it.sortierung }.mapIndexed { index, stack ->
        StackEvaluationGroupUi(
            id = stack.id,
            sequence = index + 1,
            name = stack.name,
            meta = listOf(stack.zeitpunkt, stack.einnahmeHinweis).filter(String::isNotBlank).joinToString(" · "),
            goals = stackGoals.filter { it.stackId == stack.id }.sortedBy { it.rang }.mapNotNull { assignment ->
                goalById[assignment.zielId]?.let { StackEvaluationGoalUi(assignment.rang, it.text) }
            },
            entries = entries.filter { it.stackId == stack.id }.sortedBy { it.reihenfolge }.mapNotNull { entry ->
                val medicine = medicineById[entry.mittelId] ?: return@mapNotNull null
                val medicineUi = medicine.toUi(entry, Ampel.GRAU, variant)
                StackEvaluationEntryUi(
                    name = medicine.name,
                    dose = medicineUi.dose,
                    details = buildList {
                        add(entry.frequenzAnzeige())
                        add(medicine.darreichungsformText?.trim().orEmpty().ifBlank { medicine.darreichungsformAnzeige() })
                        if (entry.gruppeId != null) add("gemeinsam im Stack")
                        if (entry.zusatztext.isNotBlank()) add(entry.zusatztext)
                    }.filter(String::isNotBlank).joinToString(" · "),
                    active = entry.aktiv,
                )
            },
        )
    }
}

private fun BewertungMitDetails.toHistoryUi(selected: Boolean): EvaluationRunUi = EvaluationRunUi(
    id = bewertung.id,
    date = DateTimeFormatter.ofPattern("dd.MM. · HH:mm", Locale.GERMANY).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(bewertung.zeitpunktEpochMillis)),
    model = "${bewertung.modell.substringAfterLast('-').replaceFirstChar(Char::uppercase)} · ${bewertung.denkstufe}",
    counts = zellen.groupBy { it.zielId }.values.map { cells -> if (cells.any { it.wirkung.name == "STOERT" }) Ampel.ROT else Ampel.GRUEN }.toCounts(),
    selectedForComparison = selected,
)

private fun Bewertung.meta(): String = DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.GERMANY)
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(zeitpunktEpochMillis)) +
    " · ${modell.substringAfterLast('-').replaceFirstChar(Char::uppercase)} · $denkstufe"

private fun String.paragraphs(): List<String> = mitDeutschenUmlauten().split(Regex("\\n\\s*\\n")).map(String::trim).filter(String::isNotBlank)

private fun List<Bewertungszelle>.historySignal(): SignalState = when {
    isEmpty() -> SignalState.Gray
    any { it.wirkung.name == "STOERT" && it.staerke >= 3 } -> SignalState.Red
    any { it.wirkung.name == "STOERT" } -> SignalState.Yellow
    else -> SignalState.Green
}

/** Die Beschriftung einer geklonten Stimme, z.B. `Frank HD · 03.08.2026, 23:21`. */
private fun ClonedVoice.voiceLabel(): String = "$name · $createdAt"

private fun TtsAnbieter.toProviderId(): TtsProviderId = when (this) {
    TtsAnbieter.EDGE -> TtsProviderId.EDGE
    TtsAnbieter.GOOGLE_CLOUD -> TtsProviderId.GOOGLE_CLOUD
    TtsAnbieter.QWEN_CLONE -> TtsProviderId.QWEN
}

/** Tempo in 0,05er-Schritten zwischen 0,70 und 1,20. */
val SPEED_STEPS: List<Float> = generateSequence(0.70f) { it + 0.05f }
    .takeWhile { it < 1.2251f }
    .map { (it * 100f).roundToInt() / 100f }
    .toList()

private fun speedLabel(value: Float): String = String.format(Locale.GERMANY, "%.2f×", value)

private fun nextSpeed(current: Float): Float {
    val index = SPEED_STEPS.indexOfFirst { kotlin.math.abs(it - current) < 0.001f }
    return SPEED_STEPS[(index + 1).mod(SPEED_STEPS.size)]
}

/**
 * Was in der Zeile hinter dem Schlüssel steht: der eigene Eintrag gewinnt, sonst zählt der
 * beim Bauen aus dem SK-Ordner eingebackene.
 */
private fun keyLabel(own: String, baked: String): String = when {
    own.isNotBlank() -> "Hinterlegt (${own.take(4)}…${own.takeLast(3)})"
    baked.isNotBlank() -> "Aus dem SK-Ordner"
    else -> "Nicht hinterlegt"
}

private fun String.reasoningLabel(): String = when (this) {
    "low" -> "Niedrig"
    "medium" -> "Mittel"
    "xhigh" -> "Sehr hoch"
    "max" -> "Maximal"
    else -> "Hoch"
}

fun TtsAnbieter.label(): String = when (this) {
    TtsAnbieter.EDGE -> "Microsoft Edge"
    TtsAnbieter.GOOGLE_CLOUD -> "Google Chirp 3 HD"
    TtsAnbieter.QWEN_CLONE -> "Meine Stimme (Alibaba)"
}
private fun TtsAnbieter.next(): TtsAnbieter = TtsAnbieter.entries[(ordinal + 1) % TtsAnbieter.entries.size]
private fun Absatzpause.next(): Absatzpause = Absatzpause.entries[(ordinal + 1) % Absatzpause.entries.size]
