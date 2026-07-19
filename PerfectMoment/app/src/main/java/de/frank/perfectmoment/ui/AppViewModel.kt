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
import de.frank.perfectmoment.auth.AuthErrorKind
import de.frank.perfectmoment.auth.CodexAuthException
import de.frank.perfectmoment.auth.CodexModel
import de.frank.perfectmoment.auth.DeviceAuthInfo
import de.frank.perfectmoment.auth.ReasoningEffort
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var hooksInitialized = false
    private var pendingRecordingTarget: RecordingTarget? = null
    private var transcriptionJob: Job? = null
    private var activeTranscriber: GroqTranscriber? = null
    private var recordingGeneration = 0L
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

    var ttsProvider by mutableStateOf(settings.ttsProvider)
        private set
    var edgeVoice by mutableStateOf(settings.edgeTtsVoice)
        private set
    var googleVoice by mutableStateOf(settings.googleTtsVoice)
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
    var pendingSessionTopic by mutableStateOf("")
        private set
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
    val activeSkill: SkillEntity?
        get() = skills.firstOrNull { it.id == activeSkillId } ?: skills.firstOrNull()
    val selectedVoice: String
        get() = if (ttsProvider == TtsProvider.EDGE.id) edgeVoice else googleVoice

    init {
        if (ttsProvider == TtsProvider.GOOGLE_CLOUD.id && googleApiKey.isBlank()) {
            ttsProvider = TtsProvider.EDGE.id
            settings.ttsProvider = TtsProvider.EDGE.id
        }
        viewModelScope.launch {
            contentRepository.observeHooks().collect { next ->
                hooks = next
                if (!hooksInitialized) {
                    hooksInitialized = true
                    next.firstOrNull()?.let {
                        selectedHookId = it.id
                        topic = it.text
                    }
                }
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
            beginAiSession("")
        } else {
            sheet = null
        }
    }

    fun selectHook(hook: HookEntity) {
        selectedHookId = hook.id
        topic = hook.text
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
        sheet = null
    }

    fun setTheme(value: String) {
        settings.theme = value
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
        pendingSessionTopic = topic.trim()
        introText = ""
        introVisible = true
        sessionError = null
        screen = AppScreen.SESSION
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
        sheet = null
        introVisible = false
        sessionError = null
        val topicValue = pendingSessionTopic
        viewModelScope.launch {
            runCatching { sessionController.startNewSession(topicValue, context.trim()) }
                .onFailure(::handleSessionFailure)
        }
    }

    fun retrySession() {
        beginAiSession(introText)
    }

    fun toggleSpeaker() = sessionController.toggleSpeaker()

    fun stopSession() {
        cancelVoiceInput()
        sessionController.stopAndClear()
        introVisible = false
        screen = AppScreen.START
    }

    fun finishEndedSession() {
        sessionController.dismissEnded()
        screen = AppScreen.START
    }

    fun openHistoryDetail(id: Long) {
        viewModelScope.launch {
            historyDetail = sessionRepository.getSession(id)
            randomReplay = false
            screen = AppScreen.HISTORY_DETAIL
        }
    }

    fun toggleRandomReplay() {
        randomReplay = !randomReplay
    }

    fun replayHistory() {
        val id = historyDetail?.session?.id ?: return
        sessionError = null
        screen = AppScreen.SESSION
        viewModelScope.launch {
            runCatching { sessionController.replaySession(id, randomReplay) }
                .onFailure { sessionError = it.message ?: "Die Sitzung konnte nicht abgespielt werden." }
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { sessionRepository.deleteSession(id) }
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
        val reordered = hooks.toMutableList().apply { add(to, removeAt(from)) }
        viewModelScope.launch { contentRepository.reorderHooks(reordered.map(HookEntity::id)) }
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
