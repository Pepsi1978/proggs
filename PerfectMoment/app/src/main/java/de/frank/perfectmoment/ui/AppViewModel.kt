package de.frank.perfectmoment.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.frank.perfectmoment.BuildConfig
import de.frank.perfectmoment.audio.GroqTranscriber
import de.frank.perfectmoment.audio.MicRecorder
import de.frank.perfectmoment.backup.BackupRepository
import de.frank.perfectmoment.backup.BackupStatus
import de.frank.perfectmoment.backup.DriveAuth
import de.frank.perfectmoment.auth.AuthErrorKind
import de.frank.perfectmoment.auth.CodexAuthException
import de.frank.perfectmoment.auth.CodexModel
import de.frank.perfectmoment.auth.DeviceAuthInfo
import de.frank.perfectmoment.auth.IntroQuestionPolicy
import de.frank.perfectmoment.auth.QuestionPerspective
import de.frank.perfectmoment.auth.ReasoningEffort
import de.frank.perfectmoment.auth.SessionPromptDecision
import de.frank.perfectmoment.data.local.HookEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.SessionWithQuestions
import de.frank.perfectmoment.data.local.SkillEntity
import de.frank.perfectmoment.di.AppContainer
import de.frank.perfectmoment.session.Phase
import de.frank.perfectmoment.session.SessionController
import de.frank.perfectmoment.session.SessionRuntime
import de.frank.perfectmoment.session.SessionState
import de.frank.perfectmoment.tts.TtsCatalog
import de.frank.perfectmoment.tts.TtsManager
import de.frank.perfectmoment.tts.TtsProvider
import de.frank.perfectmoment.tts.TtsVoice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    START,
    SESSION,
    HISTORY,
    HISTORY_DETAIL,
    SETTINGS,
    HOOKS,
    HOOK_EDITOR,
    SKILLS,
    SKILL_EDITOR,
    VOICE,
    CHAT_GPT,
    RAW_DATA,
}

enum class AppSheet { PAUSES, REPETITIONS, DURATION, PROVIDER, MODEL, REASONING, INTRO }
enum class RecordingState { IDLE, RECORDING, PROCESSING }
enum class RecordingTarget { START, INTRO }
enum class ChatGptState { DISCONNECTED, CODE, EXPIRED, CONNECTED }
enum class HistorySort(val label: String) {
    MOST_USED("Am häufigsten"),
    NEWEST("Neueste"),
    OLDEST("Älteste"),
    A_TO_Z("A–Z"),
}

internal fun sortHistorySessions(
    sessions: List<SessionEntity>,
    sort: HistorySort,
): List<SessionEntity> {
    val lastPlayed = sessions.maxWithOrNull(compareBy<SessionEntity> { it.lastPlayedAt }.thenBy { it.id })
        ?: return emptyList()
    val remainingComparator = when (sort) {
        HistorySort.MOST_USED -> compareByDescending<SessionEntity> { it.playCount }
            .thenByDescending { it.lastPlayedAt }
            .thenByDescending { it.startedAt }
        HistorySort.NEWEST -> compareByDescending<SessionEntity> { it.startedAt }
        HistorySort.OLDEST -> compareBy<SessionEntity> { it.startedAt }
        HistorySort.A_TO_Z -> compareBy<SessionEntity> { it.topic.lowercase(Locale.GERMAN) }
            .thenByDescending { it.startedAt }
    }
    return listOf(lastPlayed) + sessions.filterNot { it.id == lastPlayed.id }.sortedWith(remainingComparator)
}

/** Woher die zuletzt gestartete Sitzung kam — Grundlage für „Erneut versuchen". */
private sealed interface SessionStart {
    data object New : SessionStart
    data class Replay(val sessionId: Long, val shuffle: Boolean) : SessionStart
    data class Resume(val sessionId: Long, val useChangedSettings: Boolean) : SessionStart
}

class AppViewModel(
    context: Context,
    private val container: AppContainer,
) : ViewModel() {
    private val appContext = context.applicationContext
    private val settings = container.settings
    private val contentRepository = container.contentRepository
    private val sessionRepository = container.sessionRepository
    private val authManager = container.codexAuthManager
    private val sessionController: SessionController = container.sessionController
    private val micRecorder = MicRecorder(appContext)
    private val previewTts = TtsManager(appContext)
    private var pendingRecordingTarget: RecordingTarget? = null
    private var transcriptionJob: Job? = null
    private var activeTranscriber: GroqTranscriber? = null
    private var recordingGeneration = 0L
    private var sessionStartJob: Job? = null
    private var sessionStartGeneration = 0L
    private var hadActiveSession = sessionController.runtime.value != null

    val sessionRuntime: StateFlow<SessionRuntime?> = sessionController.runtime
    val sessionState: StateFlow<SessionState?> = sessionController.state
    val theme: StateFlow<String> = settings.themeFlow
    val appLockEnabled: StateFlow<Boolean> = settings.appLockEnabledFlow

    var screen by mutableStateOf(
        if (sessionController.runtime.value != null) AppScreen.SESSION else AppScreen.START,
    )
        private set
    var sheet by mutableStateOf<AppSheet?>(null)
        private set

    var hooks by mutableStateOf<List<HookEntity>>(emptyList())
        private set
    var skills by mutableStateOf<List<SkillEntity>>(emptyList())
        private set
    var activeSkillId by mutableStateOf(settings.activeSkillId)
        private set
    var sessions by mutableStateOf<List<SessionEntity>>(emptyList())
        private set
    var historySort by mutableStateOf(HistorySort.MOST_USED)
        private set
    var rawSessions by mutableStateOf<List<SessionWithQuestions>>(emptyList())
        private set

    var selectedHookId by mutableStateOf<Long?>(null)
        private set
    var topic by mutableStateOf("")
        private set
    var recordingState by mutableStateOf(RecordingState.IDLE)
        private set
    var recordingTarget by mutableStateOf(RecordingTarget.START)
        private set
    var recordingMessage by mutableStateOf<String?>(null)
        private set

    var pauseRep by mutableStateOf(settings.pauseRepSeconds)
        private set
    var pauseNext by mutableStateOf(settings.pauseNextSeconds)
        private set
    var repetitions by mutableStateOf(settings.repsPerQuestion)
        private set
    var durationMinutes by mutableStateOf(settings.sessionDurationMin)
        private set
    var questionPerspective by mutableStateOf(QuestionPerspective.fromId(settings.questionPerspective))
        private set

    var ttsProvider by mutableStateOf(settings.ttsProvider)
        private set
    var edgeVoice by mutableStateOf(settings.edgeTtsVoice)
        private set
    var googleVoice by mutableStateOf(settings.googleTtsVoice)
        private set
    var ttsSpeechRate by mutableStateOf(settings.ttsSpeechRate)
        private set
    var googleApiKey by mutableStateOf(settings.googleTtsApiKey)
        private set
    var groqApiKey by mutableStateOf(settings.groqApiKey)
        private set
    var showGoogleKey by mutableStateOf(false)
        private set
    var showGroqKey by mutableStateOf(false)
        private set
    var voiceTab by mutableStateOf(TtsProvider.EDGE)
        private set
    var playingVoiceId by mutableStateOf<String?>(null)
        private set
    var favoriteVoiceIds by mutableStateOf(settings.favoriteTtsVoices)
        private set

    var model by mutableStateOf(CodexModel.fromLabel(settings.model))
        private set
    var reasoning by mutableStateOf(ReasoningEffort.fromLabel(settings.reasoning))
        private set
    var chatGptState by mutableStateOf(
        if (authManager.isConnected) ChatGptState.CONNECTED else ChatGptState.DISCONNECTED,
    )
        private set
    var deviceAuthInfo by mutableStateOf<DeviceAuthInfo?>(null)
        private set
    var chatGptError by mutableStateOf<String?>(null)
        private set
    val connectedEmail: String?
        get() = authManager.email
    val connectedSince: String?
        get() = settings.chatGptConnectedAt.takeIf { it > 0L }?.let {
            SimpleDateFormat("d. MMMM yyyy", Locale.GERMAN).format(Date(it))
        }

    var introVisible by mutableStateOf(false)
        private set
    var introText by mutableStateOf("")
        private set
    var introQuestion by mutableStateOf("")
        private set
    var pendingSessionTopic by mutableStateOf("")
        private set
    private var pendingSessionDecision: SessionPromptDecision? = null

    /** Merkt, wie die aktuelle Sitzung gestartet wurde, damit „Erneut versuchen" denselben Weg wiederholt. */
    private var lastSessionStart: SessionStart = SessionStart.New
    var sessionError by mutableStateOf<String?>(null)
        private set
    var randomReplay by mutableStateOf(false)
        private set
    var historyDetail by mutableStateOf<SessionWithQuestions?>(null)
        private set

    var hookEditorId by mutableStateOf<Long?>(null)
        private set
    var hookEditorEmoji by mutableStateOf("")
        private set
    var hookEditorText by mutableStateOf("")
        private set
    var skillEditorId by mutableStateOf<Long?>(null)
        private set
    var skillEditorName by mutableStateOf("")
        private set
    var skillEditorText by mutableStateOf("")
        private set
    var operatingModeText by mutableStateOf(settings.operatingModeText)
        private set
    var operatingModeOpen by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    val versionName: String get() = BuildConfig.VERSION_NAME
    val versionStand: String get() = BuildConfig.VERSION_BUMPED_AT
    val packageName: String get() = BuildConfig.APPLICATION_ID

    /** Zustand der Google-Drive-Sicherung für den Einstellungs-Bildschirm. */
    var driveConnected by mutableStateOf(false)
        private set
    var backupBusy by mutableStateOf(false)
        private set
    var backupState by mutableStateOf(BackupStatus.describe(appContext))
        private set

    /** Wird gesetzt, wenn Google die Freigabe abfragen will — die Activity zeigt sie an. */
    var backupConsent by mutableStateOf<android.content.IntentSender?>(null)
        private set

    fun refreshBackupState() {
        viewModelScope.launch {
            driveConnected = container.backupRepository.isConnected()
            backupState = BackupStatus.describe(appContext)
        }
    }

    fun backupNow() = runBackupAction { repository ->
        val summary = repository.backupNow()
        "Gesichert: ${summary.hooks} Aufhänger, ${summary.skills} Skills, ${summary.sessions} Sitzungen"
    }

    fun restoreNow() = runBackupAction { repository ->
        val summary = repository.restoreNow()
        if (summary.sessions == 0 && summary.skills == 0 && summary.hooks == 0) {
            "Die Sicherung war leer."
        } else {
            "Zurückgeholt: ${summary.hooks} Aufhänger, ${summary.skills} Skills, ${summary.sessions} neue Sitzungen"
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            container.backupRepository.disconnect()
            driveConnected = false
            message = "Die Verbindung zu Google Drive wurde getrennt."
        }
    }

    /**
     * Nach dem Freigabe-Dialog wird die auslösende Aktion immer erneut versucht — Google meldet ein
     * erteiltes Recht nicht verlässlich über den Rückgabewert zurück. Ob es geklappt hat, zeigt erst
     * der zweite Versuch: Kommt dann wieder eine Freigabe-Anfrage, wurde wirklich abgelehnt.
     */
    fun consentHandled(@Suppress("UNUSED_PARAMETER") approved: Boolean) {
        backupConsent = null
        val pending = pendingBackupAction
        pendingBackupAction = null
        if (pending == null) {
            refreshBackupState()
            return
        }
        consentAlreadyAsked = true
        runBackupAction(pending)
    }

    private var pendingBackupAction: (suspend (BackupRepository) -> String)? = null
    private var consentAlreadyAsked = false

    private fun runBackupAction(action: suspend (BackupRepository) -> String) {
        if (backupBusy) return
        viewModelScope.launch {
            backupBusy = true
            try {
                message = action(container.backupRepository)
                driveConnected = true
                consentAlreadyAsked = false
                backupState = BackupStatus.describe(appContext)
            } catch (consent: DriveAuth.NeedsConsent) {
                if (consentAlreadyAsked) {
                    // Zweite Anfrage trotz gezeigtem Dialog: der Zugriff wurde nicht erteilt.
                    consentAlreadyAsked = false
                    message = "Ohne die Freigabe kann nicht mit Google Drive gesichert werden."
                } else {
                    pendingBackupAction = action
                    backupConsent = consent.pendingIntent.intentSender
                }
            } catch (error: Exception) {
                consentAlreadyAsked = false
                message = error.message ?: "Die Sicherung hat nicht geklappt."
            } finally {
                backupBusy = false
            }
        }
    }
    val activeSkill: SkillEntity?
        get() = skills.firstOrNull { it.id == activeSkillId } ?: skills.firstOrNull()
    val selectedVoice: String
        get() = if (ttsProvider == TtsProvider.EDGE.id) edgeVoice else googleVoice
    val sortedSessions: List<SessionEntity>
        get() = sortHistorySessions(sessions, historySort)

    init {
        if (ttsProvider == TtsProvider.GOOGLE_CLOUD.id && googleApiKey.isBlank()) {
            ttsProvider = TtsProvider.EDGE.id
            settings.ttsProvider = TtsProvider.EDGE.id
        }
        viewModelScope.launch {
            contentRepository.observeHooks().collect { next ->
                hooks = next
            }
        }
        viewModelScope.launch {
            contentRepository.observeSkills().collect { next ->
                skills = next
                if (next.isNotEmpty() && next.none { it.id == activeSkillId }) {
                    activeSkillId = next.first().id
                    settings.activeSkillId = activeSkillId
                }
            }
        }
        viewModelScope.launch {
            sessionRepository.observeSessions().collect { sessions = it }
        }
        viewModelScope.launch {
            sessionController.runtime.collect { runtime ->
                if (runtime != null) {
                    hadActiveSession = true
                } else if (hadActiveSession && screen == AppScreen.SESSION && !introVisible) {
                    hadActiveSession = false
                    screen = AppScreen.START
                }
            }
        }
    }

    fun navigate(target: AppScreen) {
        cancelVoiceInput()
        if (target != AppScreen.VOICE) stopVoicePreview()
        sheet = null
        screen = target
        if (target == AppScreen.RAW_DATA) refreshRawData()
    }

    fun back() {
        cancelVoiceInput()
        if (screen == AppScreen.VOICE) stopVoicePreview()
        if (sheet != null) {
            closeSheet()
            return
        }
        screen = when (screen) {
            AppScreen.HISTORY_DETAIL -> AppScreen.HISTORY
            AppScreen.HOOKS, AppScreen.SKILLS, AppScreen.VOICE, AppScreen.CHAT_GPT,
            AppScreen.RAW_DATA,
            -> AppScreen.SETTINGS
            AppScreen.HOOK_EDITOR -> AppScreen.HOOKS
            AppScreen.SKILL_EDITOR -> AppScreen.SKILLS
            AppScreen.HISTORY, AppScreen.SETTINGS -> AppScreen.START
            AppScreen.SESSION -> AppScreen.SESSION
            AppScreen.START -> AppScreen.START
        }
    }

    fun openSheet(value: AppSheet) {
        cancelVoiceInput()
        sheet = value
    }

    fun closeSheet() {
        if (sheet == AppSheet.INTRO) {
            cancelVoiceInput()
            sheet = null
        } else {
            sheet = null
        }
    }

    fun selectHook(hook: HookEntity) {
        if (selectedHookId == hook.id) {
            selectedHookId = null
            topic = ""
        } else {
            selectedHookId = hook.id
            topic = hook.text
        }
        recordingMessage = null
    }

    fun updateTopic(value: String) {
        topic = value
        selectedHookId = null
        recordingMessage = null
    }

    fun updatePauseRep(value: Int) {
        pauseRep = value.coerceIn(1, 30)
        settings.pauseRepSeconds = pauseRep
    }

    fun updatePauseNext(value: Int) {
        pauseNext = value.coerceIn(1, 60)
        settings.pauseNextSeconds = pauseNext
    }

    fun updateRepetitions(value: Int) {
        repetitions = value.coerceIn(1, 10)
        settings.repsPerQuestion = repetitions
    }

    fun setDuration(value: Int) {
        durationMinutes = value
        settings.sessionDurationMin = value
        viewModelScope.launch {
            delay(100)
            if (sheet == AppSheet.DURATION) sheet = null
        }
    }

    fun setTheme(value: String) {
        settings.theme = value
    }

    fun updateQuestionPerspective(value: QuestionPerspective) {
        questionPerspective = value
        settings.questionPerspective = value.id
    }

    fun setProvider(value: TtsProvider) {
        if (value == TtsProvider.GOOGLE_CLOUD && googleApiKey.isBlank()) {
            message = "Bitte zuerst einen Google-API-Schlüssel hinterlegen."
            return
        }
        ttsProvider = value.id
        settings.ttsProvider = value.id
        sheet = null
    }

    fun updateModel(value: CodexModel) {
        model = value
        settings.model = value.apiId
        sheet = null
    }

    fun updateReasoning(value: ReasoningEffort) {
        reasoning = value
        settings.reasoning = value.apiValue
        sheet = null
    }

    fun updateGoogleApiKey(value: String) {
        googleApiKey = value
        settings.googleTtsApiKey = value
        if (value.isBlank() && ttsProvider == TtsProvider.GOOGLE_CLOUD.id) {
            ttsProvider = TtsProvider.EDGE.id
            settings.ttsProvider = TtsProvider.EDGE.id
            message = "Google TTS wurde deaktiviert, weil kein API-Schlüssel hinterlegt ist."
        }
    }

    fun updateGroqApiKey(value: String) {
        groqApiKey = value
        settings.groqApiKey = value
    }

    fun updateTtsSpeechRate(value: Float) {
        ttsSpeechRate = (value * 20f).roundToInt().div(20f).coerceIn(0.7f, 1.3f)
        settings.ttsSpeechRate = ttsSpeechRate
    }

    fun toggleGoogleKeyVisibility() {
        showGoogleKey = !showGoogleKey
    }

    fun toggleGroqKeyVisibility() {
        showGroqKey = !showGroqKey
    }

    fun updateVoiceTab(provider: TtsProvider) {
        stopVoicePreview()
        voiceTab = provider
    }

    fun selectVoice(voice: TtsVoice) {
        if (voiceTab == TtsProvider.EDGE) {
            edgeVoice = voice.id
            settings.edgeTtsVoice = voice.id
        } else if (googleApiKey.isNotBlank()) {
            googleVoice = voice.id
            settings.googleTtsVoice = voice.id
        }
    }

    fun toggleFavoriteVoice(voice: TtsVoice) {
        favoriteVoiceIds = if (voice.id in favoriteVoiceIds) {
            favoriteVoiceIds - voice.id
        } else {
            favoriteVoiceIds + voice.id
        }
        settings.favoriteTtsVoices = favoriteVoiceIds
    }

    fun previewVoice(voice: TtsVoice) {
        if (voiceTab == TtsProvider.GOOGLE_CLOUD && googleApiKey.isBlank()) return
        if (playingVoiceId == voice.id) {
            stopVoicePreview()
            return
        }
        playingVoiceId = voice.id
        previewTts.speak(
            text = "Wie fühlt es sich an, dass es dir gut geht?",
            onStart = { },
            onComplete = ::finishVoicePreview,
            onError = {
                finishVoicePreview()
                message = it.message ?: "Die Stimme konnte nicht abgespielt werden."
            },
            providerOverride = voiceTab,
            voiceOverride = voice.id,
        )
    }

    private fun stopVoicePreview() {
        previewTts.stop()
        finishVoicePreview()
    }

    private fun finishVoicePreview() {
        playingVoiceId = null
    }

    fun startSessionIntro() {
        if (topic.isBlank() || !authManager.isConnected) return
        cancelVoiceInput()
        lastSessionStart = SessionStart.New
        pendingSessionTopic = topic.trim()
        introText = ""
        introQuestion = ""
        pendingSessionDecision = null
        introVisible = false
        sessionError = null
        screen = AppScreen.SESSION
        classifyAndStartPendingSession()
    }

    fun openIntroSheet() {
        sheet = AppSheet.INTRO
    }

    fun updateIntroText(value: String) {
        introText = value
        recordingMessage = null
    }

    fun beginAiSession(context: String = introText) {
        cancelVoiceInput()
        sessionStartJob?.cancel()
        authManager.cancelQuestionGeneration()
        lastSessionStart = SessionStart.New
        sheet = null
        introVisible = false
        sessionError = null
        val decision = pendingSessionDecision ?: SessionPromptDecision(false, "")
        val prompt = IntroQuestionPolicy.resolve(pendingSessionTopic, context, decision.requiresAnswer)
        val generation = ++sessionStartGeneration
        sessionStartJob = viewModelScope.launch {
            try {
                sessionController.startNewSession(prompt.topic, prompt.introContext, decision.question)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == sessionStartGeneration) handleSessionFailure(error)
            } finally {
                if (generation == sessionStartGeneration) sessionStartJob = null
            }
        }
    }

    /**
     * Wiederholt genau den Start, der fehlgeschlagen ist. Ohne diese Unterscheidung würde der
     * Wiederholversuch einer Verlaufs-Sitzung im Startpfad für neue Sitzungen landen und dort
     * ohne Thema scheitern.
     */
    fun retrySession() {
        when (val start = lastSessionStart) {
            is SessionStart.Replay -> startReplay(start.sessionId, start.shuffle)
            is SessionStart.Resume -> startResume(start.sessionId, start.useChangedSettings)
            SessionStart.New -> when {
                pendingSessionTopic.isBlank() -> {
                    sessionError = null
                    screen = AppScreen.START
                }
                pendingSessionDecision == null -> classifyAndStartPendingSession()
                else -> beginAiSession(introText)
            }
        }
    }

    fun toggleSpeaker() = sessionController.toggleSpeaker()
    fun togglePause() = sessionController.togglePause()

    fun stopSession() {
        cancelVoiceInput()
        sessionStartGeneration++
        sessionStartJob?.cancel()
        sessionStartJob = null
        authManager.cancelQuestionGeneration()
        sessionController.stopAndClear()
        introVisible = false
        introQuestion = ""
        pendingSessionDecision = null
        sessionError = null
        screen = AppScreen.START
    }

    fun saveSession() {
        cancelVoiceInput()
        sessionStartGeneration++
        sessionStartJob?.cancel()
        sessionStartJob = null
        authManager.cancelQuestionGeneration()
        viewModelScope.launch {
            try {
                sessionController.saveAndClear()
                introVisible = false
                introQuestion = ""
                pendingSessionDecision = null
                sessionError = null
                screen = AppScreen.START
            } catch (error: Throwable) {
                message = error.message ?: "Der Fortsetzungspunkt konnte nicht gespeichert werden."
            }
        }
    }

    fun finishEndedSession() {
        sessionController.dismissEnded()
        screen = AppScreen.START
    }

    fun openHistoryDetail(id: Long) {
        viewModelScope.launch {
            historyDetail = sessionRepository.getSession(id)
            historyDetail?.session?.let { session ->
                pauseRep = session.pauseRep
                pauseNext = session.pauseNext
                repetitions = session.reps
                durationMinutes = session.durationMin
                settings.pauseRepSeconds = pauseRep
                settings.pauseNextSeconds = pauseNext
                settings.repsPerQuestion = repetitions
                settings.sessionDurationMin = durationMinutes
            }
            randomReplay = false
            screen = AppScreen.HISTORY_DETAIL
        }
    }

    fun toggleRandomReplay() {
        randomReplay = !randomReplay
    }

    fun replayHistory() {
        val id = historyDetail?.session?.id ?: return
        startReplay(id, randomReplay)
    }

    fun resumeHistory() {
        val session = historyDetail?.session ?: return
        val useChangedSettings = pauseRep != session.pauseRep ||
            pauseNext != session.pauseNext ||
            repetitions != session.reps ||
            durationMinutes != session.durationMin
        startResume(session.id, useChangedSettings)
    }

    private fun startReplay(sessionId: Long, shuffle: Boolean) {
        lastSessionStart = SessionStart.Replay(sessionId, shuffle)
        val generation = beginSessionStart()
        sessionStartJob = viewModelScope.launch {
            try {
                if (!sessionController.replaySession(sessionId, shuffle)) {
                    message = "Die Fragen laufen im gespeicherten Wortlaut. " +
                        "OpenAI war für die Perspektivumstellung gerade nicht erreichbar."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == sessionStartGeneration) handleSessionFailure(error)
            } finally {
                if (generation == sessionStartGeneration) sessionStartJob = null
            }
        }
    }

    private fun startResume(sessionId: Long, useChangedSettings: Boolean) {
        lastSessionStart = SessionStart.Resume(sessionId, useChangedSettings)
        val generation = beginSessionStart()
        sessionStartJob = viewModelScope.launch {
            try {
                sessionController.resumeSession(sessionId, useChangedSettings)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == sessionStartGeneration) handleSessionFailure(error)
            } finally {
                if (generation == sessionStartGeneration) sessionStartJob = null
            }
        }
    }

    private fun beginSessionStart(): Long {
        sessionError = null
        screen = AppScreen.SESSION
        sessionStartJob?.cancel()
        authManager.cancelQuestionGeneration()
        return ++sessionStartGeneration
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { sessionRepository.deleteSession(id) }
    }

    fun updateHistorySort(value: HistorySort) {
        historySort = value
    }

    fun openHookEditor(hook: HookEntity?) {
        hookEditorId = hook?.id
        hookEditorEmoji = hook?.emoji.orEmpty()
        hookEditorText = hook?.text.orEmpty()
        screen = AppScreen.HOOK_EDITOR
    }

    fun updateHookEmoji(value: String) {
        hookEditorEmoji = value
    }

    fun updateHookText(value: String) {
        hookEditorText = value
    }

    fun saveHook() {
        if (hookEditorEmoji.isBlank() || hookEditorText.isBlank()) {
            message = "Emoji und Aufhängertext dürfen nicht leer sein."
            return
        }
        viewModelScope.launch {
            contentRepository.upsertHook(
                HookEntity(
                    id = hookEditorId ?: 0,
                    emoji = hookEditorEmoji.trim(),
                    text = hookEditorText.trim(),
                    sortIndex = hooks.firstOrNull { it.id == hookEditorId }?.sortIndex
                        ?: (hooks.maxOfOrNull(HookEntity::sortIndex)?.plus(1) ?: 0),
                ),
            )
            screen = AppScreen.HOOKS
        }
    }

    fun deleteHook() {
        val hook = hooks.firstOrNull { it.id == hookEditorId } ?: run {
            screen = AppScreen.HOOKS
            return
        }
        viewModelScope.launch {
            contentRepository.deleteHook(hook)
            if (selectedHookId == hook.id) {
                selectedHookId = null
                topic = ""
            }
            screen = AppScreen.HOOKS
        }
    }

    fun moveHook(from: Int, to: Int) {
        if (from !in hooks.indices || to !in hooks.indices || from == to) return
        hooks = hooks.toMutableList().apply { add(to, removeAt(from)) }
            .mapIndexed { index, hook -> hook.copy(sortIndex = index) }
    }

    fun persistHookOrder() {
        val hookIds = hooks.map(HookEntity::id)
        viewModelScope.launch { contentRepository.reorderHooks(hookIds) }
    }

    fun openSkillEditor(skill: SkillEntity?) {
        skillEditorId = skill?.id
        skillEditorName = skill?.name.orEmpty()
        skillEditorText = skill?.text.orEmpty()
        operatingModeText = settings.operatingModeText
        operatingModeOpen = false
        screen = AppScreen.SKILL_EDITOR
    }

    fun selectSkill(skill: SkillEntity) {
        activeSkillId = skill.id
        settings.activeSkillId = skill.id
    }

    fun updateSkillName(value: String) {
        skillEditorName = value
    }

    fun updateSkillText(value: String) {
        skillEditorText = value
    }

    fun toggleOperatingMode() {
        operatingModeOpen = !operatingModeOpen
    }

    fun updateOperatingMode(value: String) {
        operatingModeText = value
    }

    fun saveSkill() {
        if (skillEditorName.isBlank() || skillEditorText.isBlank() || operatingModeText.isBlank()) {
            message = "Name, Skill-Text und Betriebsmodus dürfen nicht leer sein."
            return
        }
        viewModelScope.launch {
            val id = contentRepository.upsertSkill(
                SkillEntity(
                    id = skillEditorId ?: 0,
                    name = skillEditorName.trim(),
                    text = skillEditorText.trim(),
                    createdAt = skills.firstOrNull { it.id == skillEditorId }?.createdAt
                        ?: System.currentTimeMillis(),
                ),
            )
            settings.operatingModeText = operatingModeText.trim()
            if (skillEditorId == null) {
                activeSkillId = id
                settings.activeSkillId = id
            }
            screen = AppScreen.SKILLS
        }
    }

    fun deleteSkill() {
        val skill = skills.firstOrNull { it.id == skillEditorId } ?: run {
            screen = AppScreen.SKILLS
            return
        }
        if (skills.size == 1) {
            message = "Mindestens ein Skill muss bestehen bleiben."
            return
        }
        viewModelScope.launch {
            contentRepository.deleteSkill(skill)
            if (activeSkillId == skill.id) {
                activeSkillId = skills.first { it.id != skill.id }.id
                settings.activeSkillId = activeSkillId
            }
            screen = AppScreen.SKILLS
        }
    }

    fun connectChatGpt(activity: ComponentActivity) {
        chatGptError = null
        viewModelScope.launch {
            runCatching {
                authManager.login(activity) { info ->
                    deviceAuthInfo = info
                    chatGptState = ChatGptState.CODE
                }
            }.onSuccess {
                chatGptState = ChatGptState.CONNECTED
                deviceAuthInfo = null
                settings.chatGptConnectedAt = System.currentTimeMillis()
            }.onFailure { error ->
                chatGptError = error.message
                chatGptState = if (
                    error is CodexAuthException &&
                    error.message.orEmpty().contains("abgelaufen", ignoreCase = true)
                ) {
                    ChatGptState.EXPIRED
                } else {
                    ChatGptState.DISCONNECTED
                }
            }
        }
    }

    fun disconnectChatGpt() {
        authManager.cancelLogin()
        authManager.logout()
        chatGptState = ChatGptState.DISCONNECTED
        deviceAuthInfo = null
        chatGptError = null
        settings.chatGptConnectedAt = 0L
    }

    fun onMicTapped(
        target: RecordingTarget,
        permissionGranted: Boolean,
        requestPermission: () -> Unit,
    ) {
        when (recordingState) {
            RecordingState.IDLE -> {
                if (groqApiKey.isBlank()) {
                    recordingMessage = "Bitte zuerst einen Groq-API-Schlüssel hinterlegen."
                    return
                }
                if (permissionGranted) {
                    startRecording(target)
                } else {
                    pendingRecordingTarget = target
                    requestPermission()
                }
            }
            RecordingState.RECORDING -> stopAndTranscribe()
            RecordingState.PROCESSING -> Unit
        }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        val target = pendingRecordingTarget ?: return
        pendingRecordingTarget = null
        if (granted && isRecordingTargetVisible(target)) {
            startRecording(target)
        } else if (!granted) {
            recordingMessage = "Die Mikrofonberechtigung wurde nicht erteilt."
        }
    }

    private fun startRecording(target: RecordingTarget) {
        if (!isRecordingTargetVisible(target)) return
        recordingGeneration++
        recordingTarget = target
        recordingMessage = null
        if (micRecorder.start(viewModelScope)) {
            recordingState = RecordingState.RECORDING
        } else {
            recordingMessage = "Die Aufnahme konnte nicht gestartet werden."
        }
    }

    private fun stopAndTranscribe() {
        recordingState = RecordingState.PROCESSING
        val target = recordingTarget
        val generation = recordingGeneration
        transcriptionJob = viewModelScope.launch {
            var transcriber: GroqTranscriber? = null
            try {
                val wav = micRecorder.stop()
                if (generation != recordingGeneration) return@launch
                if (wav == null) {
                    recordingMessage = "Ich habe nichts verstanden."
                    return@launch
                }
                transcriber = GroqTranscriber(settings.groqApiKey)
                activeTranscriber = transcriber
                val transcript = transcriber.transcribe(wav)
                if (generation != recordingGeneration) return@launch
                if (transcript.isBlank()) {
                    recordingMessage = "Ich habe nichts verstanden."
                } else if (target == RecordingTarget.START) {
                    updateTopic(appendDictation(topic, transcript))
                } else {
                    updateIntroText(appendDictation(introText, transcript))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == recordingGeneration) {
                    recordingMessage = error.message ?: "Die Spracheingabe ist fehlgeschlagen."
                }
            } finally {
                transcriber?.shutdown()
                if (activeTranscriber === transcriber) activeTranscriber = null
                if (generation == recordingGeneration) {
                    transcriptionJob = null
                    recordingState = RecordingState.IDLE
                }
            }
        }
    }

    private fun isRecordingTargetVisible(target: RecordingTarget): Boolean = when (target) {
        RecordingTarget.START -> screen == AppScreen.START && sheet == null
        RecordingTarget.INTRO -> screen == AppScreen.SESSION && introVisible && sheet == AppSheet.INTRO
    }

    private fun cancelVoiceInput() {
        recordingGeneration++
        pendingRecordingTarget = null
        transcriptionJob?.cancel()
        transcriptionJob = null
        activeTranscriber?.shutdown()
        activeTranscriber = null
        micRecorder.release()
        recordingState = RecordingState.IDLE
        recordingMessage = null
    }

    private fun handleSessionFailure(error: Throwable) {
        val authError = generateSequence(error) { it.cause }
            .filterIsInstance<CodexAuthException>()
            .firstOrNull { it.kind == AuthErrorKind.REAUTH }
        if (authError != null) {
            authManager.cancelLogin()
            authManager.logout()
            settings.chatGptConnectedAt = 0L
            chatGptState = ChatGptState.DISCONNECTED
            deviceAuthInfo = null
            chatGptError = authError.message
        }
        sessionError = error.message ?: "Die Fragen konnten nicht erzeugt werden."
    }

    private fun classifyAndStartPendingSession() {
        if (pendingSessionTopic.isBlank()) {
            sessionError = null
            screen = AppScreen.START
            return
        }
        sessionStartJob?.cancel()
        authManager.cancelQuestionGeneration()
        sessionError = null
        val generation = ++sessionStartGeneration
        sessionStartJob = viewModelScope.launch {
            try {
                val decision = authManager.classifySessionPrompt(pendingSessionTopic, model, reasoning)
                if (generation != sessionStartGeneration) return@launch
                pendingSessionDecision = decision
                introQuestion = decision.question
                if (decision.requiresAnswer) {
                    introVisible = true
                } else {
                    val prompt = IntroQuestionPolicy.resolve(pendingSessionTopic, "", false)
                    sessionController.startNewSession(prompt.topic, prompt.introContext)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (generation == sessionStartGeneration) handleSessionFailure(error)
            } finally {
                if (generation == sessionStartGeneration) sessionStartJob = null
            }
        }
    }

    fun clearMessage() {
        message = null
    }

    fun showMessage(value: String) {
        message = value
    }

    fun disableAppLock() = container.appLockManager.disable()

    private fun refreshRawData() {
        viewModelScope.launch {
            rawSessions = sessions.mapNotNull { sessionRepository.getSession(it.id) }
        }
    }

    override fun onCleared() {
        cancelVoiceInput()
        sessionStartGeneration++
        sessionStartJob?.cancel()
        sessionStartJob = null
        authManager.cancelQuestionGeneration()
        previewTts.shutdown()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context, container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(context, container) as T
            }
    }
}
