package de.frank.karteikartenlernen.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.frank.karteikartenlernen.AppViewModel
import de.frank.karteikartenlernen.model.AppTab
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.ui.theme.KarteikartenTheme
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette
import de.frank.karteikartenlernen.text.researchAnswerForSpeech
import de.frank.karteikartenlernen.text.completedResearchSpeechSegments

@Composable
fun KarteikartenApp(
    state: AppUiState,
    viewModel: AppViewModel,
    onMicClick: () -> Unit,
    onSpeak: (String) -> Unit,
    onSpeakWithContinuation: (String, String) -> Unit,
    onContinueSpeaking: () -> Unit,
    onStartSpeechQueue: () -> Unit,
    onEnqueueSpeech: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onLogin: () -> Unit,
    onOpenAuthPage: () -> Unit,
    onDarkChanged: (Boolean) -> Unit,
) {
    var learningNarrationEnabled by remember { mutableStateOf(false) }
    var activeReadAloudTarget by remember { mutableStateOf<String?>(null) }
    var currentResearchSpokenSegments by remember { mutableIntStateOf(0) }
    val currentResearchTarget = "research:current"
    val profile = if (state.settings.dark) state.settings.darkProfile else state.settings.lightProfile
    KarteikartenTheme(state.settings.dark, profile) {
        LaunchedEffect(state.settings.dark) { onDarkChanged(state.settings.dark) }
        LaunchedEffect(activeReadAloudTarget, state.answer, state.generationPhase) {
            if (activeReadAloudTarget != currentResearchTarget) return@LaunchedEffect
            if (state.generationPhase == de.frank.karteikartenlernen.model.GenerationPhase.ANSWER && state.answer.isNullOrEmpty()) {
                currentResearchSpokenSegments = 0
                onStartSpeechQueue()
                return@LaunchedEffect
            }
            val segments = completedResearchSpeechSegments(
                answer = state.answer.orEmpty(),
                final = state.generationPhase != de.frank.karteikartenlernen.model.GenerationPhase.ANSWER,
            )
            if (segments.size < currentResearchSpokenSegments) currentResearchSpokenSegments = 0
            segments.drop(currentResearchSpokenSegments).forEach(onEnqueueSpeech)
            currentResearchSpokenSegments = segments.size
        }
        BackHandler(
            enabled = state.learning != null || state.showOAuth || state.showModelSheet || state.showCrossSheet || state.selectedSession != null,
        ) {
            when {
                state.showOAuth -> viewModel.showOAuth(false)
                state.showModelSheet -> viewModel.showModelSheet(false)
                state.showCrossSheet -> viewModel.closeCross()
                state.learning != null -> {
                    learningNarrationEnabled = false
                    onStopSpeaking()
                    viewModel.closeLearning()
                }
                state.selectedSession != null -> viewModel.closeSession()
            }
        }
        val c = LocalAppPalette.current
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.background1, c.background0)))) {
            AuroraBackground()
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (state.tab) {
                            AppTab.RESEARCH -> ResearchScreen(
                                state = state,
                                onMicClick = onMicClick,
                                onInput = viewModel::updateInput,
                                onImprove = viewModel::improve,
                                onUndo = viewModel::undo,
                                onSend = viewModel::send,
                                readAloudEnabled = activeReadAloudTarget == currentResearchTarget,
                                onToggleReadAloud = {
                                    if (activeReadAloudTarget == currentResearchTarget) {
                                        activeReadAloudTarget = null
                                        currentResearchSpokenSegments = 0
                                        onStopSpeaking()
                                    } else if (!state.answer.isNullOrBlank()) {
                                        activeReadAloudTarget = currentResearchTarget
                                        currentResearchSpokenSegments = 0
                                        onStartSpeechQueue()
                                    }
                                },
                                onLearn = { viewModel.startLearning(state.cards) },
                                onReset = viewModel::resetResearch,
                            )
                            AppTab.PROFILES -> ProfilesScreen(state, viewModel::updateSearch, viewModel::openSession)
                            AppTab.SETTINGS -> SettingsScreen(
                                state = state,
                                onModel = viewModel::chooseModel,
                                onReasoning = viewModel::chooseReasoning,
                                onSettings = viewModel::updateSettings,
                                onLogin = { viewModel.showOAuth(true) },
                                onLogout = viewModel::logout,
                                onSpeakTest = { onSpeak("Guten Tag, so klingt diese Stimme beim Vorlesen deiner Karten.") },
                                onTestSound = viewModel::testSound,
                            )
                            AppTab.LEARN -> Unit
                        }
                    }
                    if (state.tab == AppTab.RESEARCH) {
                        ModelStatusPill(
                            model = state.model,
                            reasoning = state.reasoning,
                            onClick = { viewModel.showModelSheet(true) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 10.dp),
                        )
                    }
                }
                MainBottomBar(state.tab, viewModel::selectTab)
            }
            if (state.selectedSession != null) {
                val profileReadAloudTarget = "profile:${state.selectedSession.id}"
                SessionDetail(
                    state = state,
                    onClose = viewModel::closeSession,
                    onResearchTab = viewModel::setDetailResearch,
                    onLearn = viewModel::startLearning,
                    onDelete = viewModel::deleteCard,
                    onDeleteSession = viewModel::deleteSelectedSession,
                    onReset = viewModel::resetLearningStatus,
                    readAloudEnabled = activeReadAloudTarget == profileReadAloudTarget,
                    onToggleReadAloud = {
                        if (activeReadAloudTarget == profileReadAloudTarget) {
                            activeReadAloudTarget = null
                            onStopSpeaking()
                        } else if (state.detailAnswer.isNotBlank()) {
                            activeReadAloudTarget = profileReadAloudTarget
                            onSpeak(researchAnswerForSpeech(state.detailAnswer))
                        }
                    },
                )
            }
            if (state.learning != null) {
                LearningOverlay(
                    state = state,
                    onClose = {
                        learningNarrationEnabled = false
                        onStopSpeaking()
                        viewModel.closeLearning()
                    },
                    onFlip = viewModel::flipCard,
                    onRate = viewModel::rateCard,
                    onRestart = viewModel::restartLearning,
                    onSpeak = onSpeak,
                    onSpeakWithContinuation = onSpeakWithContinuation,
                    onContinueSpeaking = onContinueSpeaking,
                    narrationEnabled = learningNarrationEnabled,
                    onNarrationEnabledChange = { learningNarrationEnabled = it },
                    onStopSpeaking = onStopSpeaking,
                )
            }
            CrossSheet(state, viewModel::decideCross, viewModel::closeCross)
            ModelSheet(state, { viewModel.showModelSheet(false) }, viewModel::chooseModel, viewModel::chooseReasoning)
            OAuthDialog(state, { viewModel.showOAuth(false) }, onLogin, onOpenAuthPage)
        }
    }
}
