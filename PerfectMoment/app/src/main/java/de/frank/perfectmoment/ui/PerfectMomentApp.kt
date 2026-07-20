package de.frank.perfectmoment.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.perfectmoment.auth.CodexModel
import de.frank.perfectmoment.auth.ReasoningEffort
import de.frank.perfectmoment.session.SessionRuntime
import de.frank.perfectmoment.session.SessionState
import de.frank.perfectmoment.tts.TtsProvider
import de.frank.perfectmoment.ui.theme.BreathingBackground
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.LocalReducedMotion

@Composable
fun PerfectMomentApp(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
    connectChatGpt: () -> Unit,
    copyDeviceCode: (String) -> Unit,
    openDevicePage: (String) -> Unit,
    toggleAppLock: (Boolean) -> Unit,
) {
    val runtime by viewModel.sessionRuntime.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val reduced = LocalReducedMotion.current
    val duration = if (reduced) 200 else 450
    val screenOffset = with(LocalDensity.current) { 14.dp.roundToPx() }
    val canCancelSessionPreparation = viewModel.screen == AppScreen.SESSION &&
        viewModel.sheet == null &&
        sessionState == null &&
        viewModel.sessionError == null

    BackHandler(enabled = viewModel.screen != AppScreen.START || viewModel.sheet != null) {
        when {
            canCancelSessionPreparation -> viewModel.stopSession()
            viewModel.screen != AppScreen.SESSION || viewModel.sheet != null -> viewModel.back()
        }
    }
    BreathingBackground(session = viewModel.screen == AppScreen.SESSION) {
        AnimatedContent(
            targetState = viewModel.screen,
            transitionSpec = {
                val enter = if (targetState == AppScreen.SESSION) {
                    slideInVertically(tween(if (reduced) 200 else 700, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))) { it }
                } else {
                    fadeIn(tween(duration)) + slideInVertically(
                        tween(duration, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                    ) { screenOffset }
                }
                enter togetherWith fadeOut(tween(duration / 2))
            },
            label = "Navigation",
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                AppScreen.START -> StartScreen(
                    viewModel,
                    microphonePermissionGranted,
                    requestMicrophonePermission,
                )
                AppScreen.SESSION -> SessionScreen(viewModel, runtime, sessionState)
                AppScreen.HISTORY -> HistoryScreen(viewModel)
                AppScreen.HISTORY_DETAIL -> HistoryDetailScreen(viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel, appLockEnabled, toggleAppLock)
                AppScreen.HOOKS -> HooksScreen(viewModel)
                AppScreen.HOOK_EDITOR -> HookEditorScreen(viewModel)
                AppScreen.SKILLS -> SkillsScreen(viewModel)
                AppScreen.SKILL_EDITOR -> SkillEditorScreen(viewModel)
                AppScreen.VOICE -> VoiceScreen(viewModel)
                AppScreen.CHAT_GPT -> ChatGptScreen(
                    viewModel,
                    connectChatGpt,
                    copyDeviceCode,
                    openDevicePage,
                )
                AppScreen.RAW_DATA -> RawDataScreen(viewModel)
            }
        }
        viewModel.sheet?.let { sheet ->
            AppBottomSheet(
                viewModel = viewModel,
                sheet = sheet,
                microphonePermissionGranted = microphonePermissionGranted,
                requestMicrophonePermission = requestMicrophonePermission,
            )
        }
        viewModel.message?.let { message ->
            MessageOverlay(message = message, onDismiss = viewModel::clearMessage)
        }
    }
}

@Composable
private fun AppBottomSheet(
    viewModel: AppViewModel,
    sheet: AppSheet,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    val colors = LocalPmColors.current
    val visibility = remember(sheet) {
        MutableTransitionState(false).apply { targetState = true }
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .pmClickable(onClick = viewModel::closeSheet),
        )
        AnimatedVisibility(
            visibleState = visibility,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                tween(
                    if (LocalReducedMotion.current) 200 else 400,
                    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                ),
            ) { it },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(colors.surface2, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 44.dp),
            ) {
                when (sheet) {
                    AppSheet.PAUSES -> {
                        SliderBlock("Pause zwischen Wiederholungen", viewModel.pauseRep, 1..30, viewModel::updatePauseRep, "s")
                        Spacer(Modifier.height(24.dp))
                        SliderBlock("Pause bis zur nächsten Frage", viewModel.pauseNext, 1..60, viewModel::updatePauseNext, "s")
                    }
                    AppSheet.REPETITIONS -> SliderBlock(
                        "Wiederholungen pro Frage",
                        viewModel.repetitions,
                        1..10,
                        viewModel::updateRepetitions,
                        "×",
                    )
                    AppSheet.DURATION -> DurationSheet(viewModel)
                    AppSheet.PROVIDER -> OptionSheet(
                        "Anbieter",
                        TtsProvider.entries.map { provider ->
                            val label = if (provider == TtsProvider.EDGE) "Microsoft Edge" else "Google Chirp 3 HD"
                            OptionRow(label, viewModel.ttsProvider == provider.id) { viewModel.setProvider(provider) }
                        },
                    )
                    AppSheet.MODEL -> OptionSheet(
                        "Modell",
                        CodexModel.entries.map { value ->
                            OptionRow(value.label, viewModel.model == value) { viewModel.updateModel(value) }
                        },
                    )
                    AppSheet.REASONING -> OptionSheet(
                        "Denkstärke",
                        ReasoningEffort.entries.map { value ->
                            OptionRow(value.label, viewModel.reasoning == value) { viewModel.updateReasoning(value) }
                        },
                    )
                    AppSheet.INTRO -> IntroAnswerSheet(
                        viewModel,
                        microphonePermissionGranted,
                        requestMicrophonePermission,
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderBlock(
    label: String,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
    suffix: String,
) {
    val colors = LocalPmColors.current
    SectionLabel(label)
    Text(
        "$value $suffix",
        color = colors.goldHi,
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
    )
    Slider(
        value = value.toFloat(),
        onValueChange = { onValue(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = range.last - range.first - 1,
        colors = SliderDefaults.colors(
            thumbColor = colors.goldHi,
            activeTrackColor = colors.gold,
            inactiveTrackColor = colors.surface,
        ),
    )
}

@Composable
private fun DurationSheet(viewModel: AppViewModel) {
    SectionLabel("Sitzungsdauer", Modifier.padding(bottom = 16.dp))
    listOf(10, 20, 30, 45, 60, 90, 120).chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { duration ->
                Segment(
                    text = duration.toString(),
                    selected = duration == viewModel.durationMinutes,
                    onClick = { viewModel.setDuration(duration) },
                    modifier = Modifier.weight(1f).height(52.dp),
                )
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

private data class OptionRow(val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun OptionSheet(title: String, rows: List<OptionRow>) {
    val colors = LocalPmColors.current
    SectionLabel(title, Modifier.padding(start = 8.dp, bottom = 12.dp))
    rows.forEachIndexed { index, row ->
        Row(
            Modifier.fillMaxWidth().height(58.dp).pmClickable(onClick = row.onClick).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.label, color = colors.text1, fontFamily = Inter, fontSize = 16.sp, modifier = Modifier.weight(1f))
            CheckMark(row.selected)
        }
        if (index != rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surface))
    }
}

@Composable
private fun IntroAnswerSheet(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    SectionLabel("Deine Antwort")
    PmTextArea(
        value = viewModel.introText,
        onValueChange = viewModel::updateIntroText,
        placeholder = viewModel.introQuestion,
        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 330.dp).padding(top = 12.dp),
    )
    Spacer(Modifier.height(8.dp))
    RecorderControl(
        state = if (viewModel.recordingTarget == RecordingTarget.INTRO) viewModel.recordingState else RecordingState.IDLE,
        message = viewModel.recordingMessage,
        onClick = {
            viewModel.onMicTapped(
                RecordingTarget.INTRO,
                microphonePermissionGranted,
                requestMicrophonePermission,
            )
        },
    )
    Spacer(Modifier.height(12.dp))
    PrimaryButton(
        "Beginnen",
        onClick = { viewModel.beginAiSession() },
        enabled = viewModel.introText.isNotBlank(),
    )
}

@Composable
private fun MessageOverlay(message: String, onDismiss: () -> Unit) {
    val colors = LocalPmColors.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).pmClickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        PmCard(Modifier.fillMaxWidth().padding(horizontal = 36.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = colors.text1, fontFamily = Inter, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Schließen", onDismiss, height = 48, textSize = 14)
            }
        }
    }
}
