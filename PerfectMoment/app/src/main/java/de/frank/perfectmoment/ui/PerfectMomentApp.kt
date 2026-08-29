package de.frank.perfectmoment.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.perfectmoment.auth.CodexModel
import de.frank.perfectmoment.auth.ReasoningEffort
import de.frank.perfectmoment.data.settings.MAX_PAUSE_SECONDS
import de.frank.perfectmoment.data.settings.MAX_REPETITIONS_PER_QUESTION
import de.frank.perfectmoment.data.settings.MIN_PAUSE_SECONDS
import de.frank.perfectmoment.session.SessionRuntime
import de.frank.perfectmoment.session.SessionState
import de.frank.perfectmoment.tts.TtsCatalog
import de.frank.perfectmoment.tts.TtsProvider
import de.frank.perfectmoment.ui.theme.BreathingBackground
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.LocalMotionActive
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.LocalReducedMotion
import kotlin.math.roundToInt

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
    val duration = 650
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
    // While a sheet or message covers the screen, the content below stops animating. The blurred
    // layer then stays static and can be cached instead of re-rendering offscreen every frame.
    val overlayOpen = viewModel.sheet != null || viewModel.message != null
    BreathingBackground(session = viewModel.screen == AppScreen.SESSION) {
        CompositionLocalProvider(LocalMotionActive provides !overlayOpen) {
            AnimatedContent(
                targetState = viewModel.screen,
                transitionSpec = {
                    if (reduced) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val enter = if (targetState == AppScreen.SESSION) {
                            slideInVertically(tween(700, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))) { it }
                        } else {
                            fadeIn(tween(duration)) + slideInVertically(
                                tween(duration, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                            ) { screenOffset } + scaleIn(
                                animationSpec = tween(duration, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                                initialScale = 0.985f,
                            )
                        }
                        enter togetherWith fadeOut(tween(duration / 2))
                    }
                },
                label = "Navigation",
                modifier = Modifier.fillMaxSize().then(
                    if (overlayOpen) Modifier.pmScrimBackdrop() else Modifier,
                ),
            ) { screen ->
                // pm-fx-screen-in also softens the arriving screen from blur(4px) to blur(0).
                // Read inside graphicsLayer so it stays a layer property, never a recomposition.
                val enterBlur = transition.animateFloat(
                    transitionSpec = {
                        tween(duration, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
                    },
                    label = "Eintrittsunschärfe",
                ) { state -> if (reduced || state == EnterExitState.Visible) 0f else 4f }
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val radius = enterBlur.value
                        renderEffect = if (radius > 0.05f) {
                            BlurEffect(radius.dp.toPx(), radius.dp.toPx(), TileMode.Decal)
                        } else {
                            null
                        }
                    },
                ) {
                when (screen) {
                    AppScreen.START -> StartScreen(
                        viewModel,
                        microphonePermissionGranted,
                        requestMicrophonePermission,
                    )
                    AppScreen.SESSION -> SessionScreen(viewModel, runtime, sessionState)
                    AppScreen.HISTORY -> HistoryScreen(viewModel)
                    AppScreen.HISTORY_DETAIL -> HistoryDetailScreen(viewModel)
                    AppScreen.READING_DETAIL -> ReadingDetailScreen(
                        viewModel,
                        microphonePermissionGranted,
                        requestMicrophonePermission,
                    )
                    AppScreen.SETTINGS -> SettingsScreen(viewModel, appLockEnabled, toggleAppLock)
                    AppScreen.HOOKS -> HooksScreen(viewModel)
                    AppScreen.HOOK_EDITOR -> HookEditorScreen(
                        viewModel,
                        microphonePermissionGranted,
                        requestMicrophonePermission,
                    )
                    AppScreen.SKILLS -> SkillsScreen(viewModel)
                    AppScreen.SKILL_EDITOR -> SkillEditorScreen(viewModel)
                    AppScreen.VOICE -> VoiceScreen(viewModel)
                    AppScreen.MY_VOICES -> MyVoicesScreen(viewModel)
                    AppScreen.VOICE_RECORDER -> VoiceRecorderScreen(
                        viewModel,
                        microphonePermissionGranted,
                        requestMicrophonePermission,
                    )
                    AppScreen.CHAT_GPT -> ChatGptScreen(
                        viewModel,
                        connectChatGpt,
                        copyDeviceCode,
                        openDevicePage,
                    )
                    AppScreen.RAW_DATA -> RawDataScreen(viewModel)
                }
                }
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
    val reduced = LocalReducedMotion.current
    val optionSheet = sheet == AppSheet.PROVIDER || sheet == AppSheet.MODEL || sheet == AppSheet.REASONING
    val panelShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (optionSheet) 0.48f else 0.55f))
                .pmClickable(onClick = viewModel::closeSheet),
        )
        AnimatedVisibility(
            visibleState = visibility,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                tween(if (reduced) 200 else 400, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            ) { it },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .shadow(
                        18.dp,
                        panelShape,
                        ambientColor = Color.Black.copy(alpha = 0.34f),
                        spotColor = Color.Black.copy(alpha = 0.34f),
                    )
                    .clip(panelShape)
                    .background(colors.surface2)
                    .drawWithCache {
                        val sheen = Brush.linearGradient(
                            listOf(
                                if (colors.dark) colors.goldHi.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.48f),
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        )
                        onDrawBehind {
                            drawRect(sheen)
                            drawLine(
                                colors.goldHi.copy(alpha = 0.24f),
                                Offset(0f, 0.5.dp.toPx()),
                                Offset(size.width, 0.5.dp.toPx()),
                                1.dp.toPx(),
                            )
                        }
                    }
                    .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 44.dp),
            ) {
                when (sheet) {
                    AppSheet.PAUSES -> {
                        SliderBlock(
                            "Pause zwischen Wiederholungen",
                            viewModel.pauseRep,
                            MIN_PAUSE_SECONDS..MAX_PAUSE_SECONDS,
                            viewModel::updatePauseRep,
                            "s",
                            stepSize = 2,
                        )
                        Spacer(Modifier.height(24.dp))
                        SliderBlock(
                            "Pause bis zur nächsten Frage",
                            viewModel.pauseNext,
                            MIN_PAUSE_SECONDS..MAX_PAUSE_SECONDS,
                            viewModel::updatePauseNext,
                            "s",
                            stepSize = 2,
                        )
                    }
                    AppSheet.REPETITIONS -> SliderBlock(
                        "Wiederholungen pro Frage",
                        viewModel.repetitions,
                        1..MAX_REPETITIONS_PER_QUESTION,
                        viewModel::updateRepetitions,
                        "×",
                    )
                    AppSheet.DURATION -> DurationSheet(viewModel)
                    AppSheet.PROVIDER -> OptionSheet(
                        "Anbieter",
                        TtsProvider.entries.map { provider ->
                            OptionRow(provider.label, viewModel.ttsProvider == provider.id) {
                                viewModel.setProvider(provider)
                            }
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
                    AppSheet.HOOK_ICON -> HookIconSheet(viewModel)
                    AppSheet.HISTORY_VOICE -> HistoryVoiceSheet(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderBlock(
    label: String,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
    suffix: String,
    stepSize: Int = 1,
) {
    val colors = LocalPmColors.current
    val maxStepIndex = (range.last - range.first) / stepSize
    fun snap(rawValue: Float): Int =
        (range.first + ((rawValue - range.first) / stepSize).roundToInt() * stepSize)
            .coerceIn(range)
    val snappedValue = snap(value.toFloat())
    SectionLabel(label, decorated = true)
    Text(
        "$snappedValue $suffix",
        color = colors.goldHi,
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
    )
    Slider(
        value = snappedValue.toFloat(),
        onValueChange = { onValue(snap(it)) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (maxStepIndex - 1).coerceAtLeast(0),
        colors = SliderDefaults.colors(
            thumbColor = colors.goldHi,
            activeTrackColor = colors.gold,
            inactiveTrackColor = colors.surface,
        ),
        thumb = { PmSliderThumb() },
    )
}

@Composable
private fun DurationSheet(viewModel: AppViewModel) {
    SectionLabel("Sitzungsdauer", Modifier.padding(bottom = 16.dp), decorated = true)
    listOf(10, 20, 30, 45, 60, 90, 120, 0).chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { duration ->
                Segment(
                    text = if (duration == 0) "Endlos" else duration.toString(),
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

/**
 * The symbols a conversation hook can be given, laid out as a grid.
 *
 * Built from rows instead of a LazyVerticalGrid on purpose: the sheet is itself a Column, and a
 * lazy grid inside it would be asked to measure against infinite height.
 */
@Composable
private fun HookIconSheet(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    val chosen = HookIcons.find(viewModel.hookEditorEmoji)?.key
    SectionLabel("Symbol wählen", Modifier.padding(start = 8.dp, bottom = 14.dp), decorated = true)
    Column(
        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HookIcons.all.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { entry ->
                    val tint = HookIcons.hueColor(entry.hue, colors.dark)
                    val selected = entry.key == chosen
                    Box(
                        Modifier.weight(1f).height(60.dp)
                            .pmClickable(shape = RoundedCornerShape(20.dp), lift = true) {
                                viewModel.pickHookIcon(entry)
                            }
                            .background(
                                if (selected) colors.surface2 else colors.surface,
                                RoundedCornerShape(20.dp),
                            )
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) colors.goldHi else colors.gold.copy(alpha = 0.12f),
                                RoundedCornerShape(20.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(38.dp).background(tint.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(entry.icon, entry.key, tint = tint, modifier = Modifier.size(23.dp))
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * The voice one history entry always plays with.
 *
 * Every voice the app knows appears here, own clones first, so a sleep routine can be tied to a
 * soft voice while the settings keep serving everything else.
 */
@Composable
private fun HistoryVoiceSheet(viewModel: AppViewModel) {
    val session = viewModel.historyDetail?.session
    val chosenProvider = session?.voiceProviderOverride.orEmpty()
    val chosenVoice = session?.voiceOverride.orEmpty()
    val follows = chosenProvider.isBlank() || chosenVoice.isBlank()
    val rows = buildList {
        add(OptionRow(AppViewModel.WITH_SETTINGS_VOICE, follows) { viewModel.clearHistoryVoice() })
        viewModel.qwenVoices.forEach { voice ->
            add(
                OptionRow(
                    "${viewModel.voiceTitle(voice)}  ·  Meine Stimme",
                    !follows && chosenProvider == TtsProvider.QWEN_CLONE.id && chosenVoice == voice.id,
                ) { viewModel.selectHistoryVoice(TtsProvider.QWEN_CLONE, voice.id) },
            )
        }
        TtsCatalog.edgeVoices.forEach { voice ->
            add(
                OptionRow(
                    "${voice.name}  ·  Microsoft Edge",
                    !follows && chosenProvider == TtsProvider.EDGE.id && chosenVoice == voice.id,
                ) { viewModel.selectHistoryVoice(TtsProvider.EDGE, voice.id) },
            )
        }
        TtsCatalog.googleVoices.forEach { voice ->
            add(
                OptionRow(
                    "${voice.name}  ·  Google Chirp 3 HD",
                    !follows && chosenProvider == TtsProvider.GOOGLE_CLOUD.id && chosenVoice == voice.id,
                ) { viewModel.selectHistoryVoice(TtsProvider.GOOGLE_CLOUD, voice.id) },
            )
        }
    }
    SectionLabel("Stimme für diesen Verlauf", Modifier.padding(start = 8.dp, bottom = 12.dp), decorated = true)
    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        OptionRows(rows)
    }
}

private data class OptionRow(val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun OptionSheet(title: String, rows: List<OptionRow>) {
    SectionLabel(title, Modifier.padding(start = 8.dp, bottom = 12.dp), decorated = true)
    OptionRows(rows)
}

@Composable
private fun OptionRows(rows: List<OptionRow>) {
    val colors = LocalPmColors.current
    rows.forEachIndexed { index, row ->
        Row(
            Modifier.fillMaxWidth().height(58.dp).drawWithCache {
                onDrawBehind {
                    if (row.selected) drawRect(colors.amber, size = Size(3.dp.toPx(), size.height))
                }
            }.pmClickable(onClick = row.onClick).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.label, color = colors.text1, fontFamily = Inter, fontSize = 16.sp, modifier = Modifier.weight(1f))
            CheckMark(row.selected)
        }
        if (index != rows.lastIndex) {
            Box(
                Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, colors.gold.copy(alpha = 0.11f), colors.surface, Color.Transparent),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun IntroAnswerSheet(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    SectionLabel("Deine Antwort", decorated = true)
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
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .pmClickable(onClick = onDismiss),
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
