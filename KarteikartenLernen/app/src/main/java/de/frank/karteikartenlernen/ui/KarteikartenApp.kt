package de.frank.karteikartenlernen.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import de.frank.karteikartenlernen.AppViewModel
import de.frank.karteikartenlernen.model.AppTab
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.ui.theme.KarteikartenTheme
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette

@Composable
fun KarteikartenApp(
    state: AppUiState,
    viewModel: AppViewModel,
    onMicClick: () -> Unit,
    onSpeak: (String) -> Unit,
    onLogin: () -> Unit,
    onDarkChanged: (Boolean) -> Unit,
) {
    val profile = if (state.settings.dark) state.settings.darkProfile else state.settings.lightProfile
    KarteikartenTheme(state.settings.dark, profile) {
        LaunchedEffect(state.settings.dark) { onDarkChanged(state.settings.dark) }
        BackHandler(
            enabled = state.learning != null || state.showOAuth || state.showModelSheet || state.showCrossSheet || state.selectedSession != null,
        ) {
            when {
                state.showOAuth -> viewModel.showOAuth(false)
                state.showModelSheet -> viewModel.showModelSheet(false)
                state.showCrossSheet -> viewModel.closeCross()
                state.learning != null -> viewModel.closeLearning()
                state.selectedSession != null -> viewModel.closeSession()
            }
        }
        val c = LocalAppPalette.current
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.background1, c.background0)))) {
            AuroraBackground()
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (state.tab) {
                        AppTab.RESEARCH -> ResearchScreen(
                            state = state,
                            onModelClick = { viewModel.showModelSheet(true) },
                            onMicClick = onMicClick,
                            onInput = viewModel::updateInput,
                            onImprove = viewModel::improve,
                            onUndo = viewModel::undo,
                            onSend = viewModel::send,
                            onSpeak = onSpeak,
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
                MainBottomBar(state.tab, viewModel::selectTab)
            }
            if (state.selectedSession != null) {
                SessionDetail(
                    state = state,
                    onClose = viewModel::closeSession,
                    onResearchTab = viewModel::setDetailResearch,
                    onLearn = viewModel::startLearning,
                    onDelete = viewModel::deleteCard,
                    onReset = viewModel::resetLearningStatus,
                    onSpeak = onSpeak,
                )
            }
            if (state.learning != null) {
                LearningOverlay(
                    state = state,
                    onClose = viewModel::closeLearning,
                    onFlip = viewModel::flipCard,
                    onRate = viewModel::rateCard,
                    onRestart = viewModel::restartLearning,
                    onSpeak = onSpeak,
                )
            }
            CrossSheet(state, viewModel::decideCross, viewModel::closeCross)
            ModelSheet(state, { viewModel.showModelSheet(false) }, viewModel::chooseModel, viewModel::chooseReasoning)
            OAuthDialog(state, { viewModel.showOAuth(false) }, onLogin)
        }
    }
}
