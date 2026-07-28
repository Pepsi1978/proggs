package de.frank.perfectmoment.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.perfectmoment.auth.QuestionPerspective
import de.frank.perfectmoment.data.local.HookEntity
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.local.SkillEntity
import de.frank.perfectmoment.session.Phase
import de.frank.perfectmoment.session.SessionRuntime
import de.frank.perfectmoment.session.SessionState
import de.frank.perfectmoment.tts.TtsCatalog
import de.frank.perfectmoment.tts.TtsProvider
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.JetBrainsMono
import de.frank.perfectmoment.ui.theme.LocalMotionActive
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.LocalReducedMotion
import de.frank.perfectmoment.ui.theme.Newsreader
import de.frank.perfectmoment.ui.theme.PmTextStyles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun PmTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    singleLine: Boolean = false,
    textSize: Int = if (mono) 13 else 15,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    radius: Int = if (singleLine) 16 else 20,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val shape = RoundedCornerShape(radius.dp)
    var focused by remember { mutableStateOf(false) }
    val focusAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (reduced) 200 else 280),
        label = "Feldfokus",
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.gold),
        textStyle = TextStyle(
            color = colors.text1,
            fontFamily = if (mono) JetBrainsMono else Inter,
            fontSize = textSize.sp,
            lineHeight = if (mono) (textSize * 1.6f).sp else (textSize * 1.55f).sp,
            fontWeight = if (singleLine) FontWeight.Medium else FontWeight.Normal,
        ),
        // Focus ring of the design: 0 0 0 2px accent 52% plus a soft 0 0 30px accent 14% halo.
        modifier = modifier
            .shadow(
                elevation = (15f * focusAlpha).dp,
                shape = shape,
                ambientColor = colors.gold.copy(alpha = 0.14f),
                spotColor = colors.gold.copy(alpha = 0.14f),
            )
            .pmGlassSurface(colors, radius)
            .border(2.dp, colors.gold.copy(alpha = 0.52f * focusAlpha), shape)
            .onFocusChanged { focused = it.isFocused }
            .padding(contentPadding),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize()) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = colors.text3,
                        fontFamily = if (mono) JetBrainsMono else Inter,
                        fontSize = textSize.sp,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
fun StartScreen(
    viewModel: AppViewModel,
    microphonePermissionGranted: Boolean,
    requestMicrophonePermission: () -> Unit,
) {
    val colors = LocalPmColors.current
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val darkThemeActive = theme == "dark" || theme == "system" && isSystemInDarkTheme()
    val density = LocalDensity.current
    // The bottom bar floats above the scrolling content. Reserve exactly its height, otherwise the
    // locked-state hint ("Bitte zuerst ein Thema wählen") overlaps the recorder caption.
    var bottomBarHeight by remember { mutableStateOf(108.dp) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottomBarHeight),
        ) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).pmHeaderSurface(colors).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldWordmark("Perfect Moment", Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).drawWithCache {
                        val inset = 4.dp.toPx()
                        val visualSize = Size(44.dp.toPx(), 44.dp.toPx())
                        val corner = CornerRadius(16.dp.toPx())
                        onDrawBehind {
                            drawRoundRect(colors.surface, Offset(-inset, -inset), visualSize, corner)
                            drawRoundRect(
                                colors.gold.copy(alpha = 0.18f),
                                Offset(-inset, -inset),
                                visualSize,
                                corner,
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                    }
                        .pmClickable { viewModel.setTheme(if (darkThemeActive) "light" else "dark") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (darkThemeActive) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        if (darkThemeActive) "Zum hellen Erscheinungsbild wechseln" else "Zum dunklen Erscheinungsbild wechseln",
                        tint = colors.gold,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(18.dp))
                Icon(
                    Icons.Outlined.History,
                    "Verlauf",
                    tint = colors.goldDim,
                    modifier = Modifier.size(24.dp).pmClickable { viewModel.navigate(AppScreen.HISTORY) },
                )
                Spacer(Modifier.width(18.dp))
                Icon(
                    Icons.Outlined.Settings,
                    "Einstellungen",
                    tint = colors.goldDim,
                    modifier = Modifier.size(24.dp).pmClickable { viewModel.navigate(AppScreen.SETTINGS) },
                )
            }
            LazyRow(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(188.dp),
            ) {
                items(viewModel.hooks, key = HookEntity::id) { hook ->
                    val selected = hook.id == viewModel.selectedHookId
                    val shape = RoundedCornerShape(32.dp)
                    val iconColor = hookDisplayColor(hook, colors.dark)
                    Column(
                        // .pm-start__hook — rises and glows on touch
                        Modifier.size(168.dp)
                            .pmClickable(shape = shape, lift = true, pressBorder = colors.gold) {
                                viewModel.selectHook(hook)
                            }
                            .then(
                                if (selected) Modifier.shadow(
                                    14.dp,
                                    shape,
                                    ambientColor = colors.gold.copy(alpha = 0.24f),
                                    spotColor = colors.gold.copy(alpha = 0.24f),
                                ) else Modifier,
                            )
                            .pmGlassSurface(colors, 32, if (selected) colors.surface2 else colors.surface)
                            .border(3.dp, if (selected) colors.goldHi else Color.Transparent, shape)
                            .padding(22.dp),
                    ) {
                        Box(
                            Modifier.size(44.dp).shadow(
                                8.dp,
                                CircleShape,
                                ambientColor = iconColor.copy(alpha = 0.14f),
                                spotColor = iconColor.copy(alpha = 0.14f),
                            ).clip(CircleShape).drawWithCache {
                                val disc = Brush.radialGradient(
                                    0f to Color.White.copy(alpha = 0.18f),
                                    0.48f to iconColor.copy(alpha = 0.15f),
                                    1f to iconColor.copy(alpha = 0.15f),
                                    center = Offset(size.width * 0.34f, size.height * 0.28f),
                                    radius = size.maxDimension,
                                )
                                onDrawBehind { drawCircle(disc) }
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                hookDisplayIcon(hook),
                                null,
                                tint = iconColor,
                                modifier = Modifier.size(27.dp).scale(if (selected) 1.08f else 1f),
                            )
                        }
                        Text(
                            hook.text,
                            color = if (selected) colors.text1 else colors.text2,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            lineHeight = 22.4.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
            SectionLabel("Oder eigene Frage", Modifier.padding(start = 24.dp, top = 14.dp, bottom = 10.dp))
            PmTextArea(
                value = viewModel.topic,
                onValueChange = viewModel::updateTopic,
                placeholder = "Was möchtest du hören?",
                // Startet zweizeilig (2 × 23.25dp Zeilenhöhe + 2 × 18dp Innenabstand) und
                // wächst mit dem Text bis 180dp.
                modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp, max = 180.dp).padding(horizontal = 24.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                radius = 32,
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ParameterCard("Pause", "${viewModel.pauseRep} s", { viewModel.openSheet(AppSheet.PAUSES) }, Modifier.weight(1f), radius = 32, horizontalPadding = 4)
                ParameterCard("Wiederholungen", "${viewModel.repetitions}×", { viewModel.openSheet(AppSheet.REPETITIONS) }, Modifier.weight(1.4f), radius = 32, horizontalPadding = 4)
                ParameterCard("Dauer", formatSessionDuration(viewModel.durationMinutes), { viewModel.openSheet(AppSheet.DURATION) }, Modifier.weight(1f), radius = 32, horizontalPadding = 4)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 38.dp, bottom = 38.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Spacer(Modifier.weight(3f))
                RecorderControl(
                    state = if (viewModel.recordingTarget == RecordingTarget.START) viewModel.recordingState else RecordingState.IDLE,
                    message = viewModel.recordingMessage,
                    onClick = {
                        viewModel.onMicTapped(
                            RecordingTarget.START,
                            microphonePermissionGranted,
                            requestMicrophonePermission,
                        )
                    },
                    scale = 4f / 3f,
                )
                Spacer(Modifier.weight(1f))
            }
        }
        val connected = viewModel.chatGptState == ChatGptState.CONNECTED
        val unlocked = viewModel.topic.isNotBlank() && connected
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } }
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, colors.background, colors.background)),
                ).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Nur der ChatGPT-Hinweis bleibt — er führt zur Lösung. Dass ein Thema fehlt,
            // zeigt der gesperrte Knopf bereits.
            if (!connected) {
                Text(
                    "Bitte zuerst mit ChatGPT verbinden",
                    color = colors.text2,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                        .pmClickable { viewModel.navigate(AppScreen.CHAT_GPT) },
                )
            }
            PrimaryButton(
                "Sitzung beginnen",
                viewModel::startSessionIntro,
                enabled = unlocked,
                height = 60,
                textSize = 17,
            )
        }
    }
}

@Composable
fun SessionScreen(
    viewModel: AppViewModel,
    runtime: SessionRuntime?,
    state: SessionState?,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    var dimmed by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var showStopDialog by remember { mutableStateOf(false) }
    var resumeAfterStopDialog by remember { mutableStateOf(false) }
    val endRing = remember { Animatable(0f) }
    val endExit = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dimAlpha by animateFloatAsState(
        if (dimmed) 0.88f else 0f,
        tween(if (reduced) 200 else if (dimmed) 4_000 else 300),
        label = "Bildschirmdimmung",
    )
    LaunchedEffect(interactionTick, state?.phase, viewModel.introVisible) {
        if (!viewModel.introVisible && state?.phase != Phase.ENDED) {
            delay(30_000)
            dimmed = true
        }
    }
    LaunchedEffect(state?.phase) {
        if (state?.phase == Phase.ENDED) {
            endRing.snapTo(0f)
            endExit.snapTo(0f)
            endRing.animateTo(
                1f,
                tween(if (reduced) 200 else 2_000, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            )
            delay(6_000)
            endExit.animateTo(
                1f,
                tween(if (reduced) 200 else 700, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            )
            viewModel.finishEndedSession()
        }
    }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                var wasPressed = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val pressed = event.changes.any { it.pressed }
                    if (pressed && !wasPressed) interactionTick++
                    wasPressed = pressed
                }
            }
        },
    ) {
        // Stop dialog open: freeze the animations behind it so the blurred layer can be cached.
        CompositionLocalProvider(LocalMotionActive provides !showStopDialog) {
            SessionQuestions(
                runtime = runtime,
                state = state,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onTogglePause = viewModel::togglePause,
                onStop = {
                    resumeAfterStopDialog = state?.paused == false
                    if (resumeAfterStopDialog) viewModel.togglePause()
                    showStopDialog = true
                },
                modifier = if (showStopDialog) Modifier.pmScrimBackdrop() else Modifier,
            )
        }
        if (viewModel.introVisible) SessionIntroOverlay(viewModel)
        if (!viewModel.introVisible && state == null && viewModel.sessionError == null) {
            Box(Modifier.fillMaxSize()) {
                PreparationBackButton(
                    onClick = viewModel::stopSession,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 8.dp),
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoadingDots()
                    Text(
                        "Fragen werden vorbereitet…",
                        color = colors.text2,
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
            }
        }
        viewModel.sessionError?.let { SessionErrorOverlay(viewModel, it) }
        if (state?.phase == Phase.ENDED) {
            Box(
                Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.75f))
                    .graphicsLayer { translationY = with(density) { 915.dp.toPx() } * endExit.value }
                    .pmClickable { viewModel.finishEndedSession() },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Box(
                        Modifier.size(64.dp).graphicsLayer {
                            scaleX = endRing.value
                            scaleY = endRing.value
                            alpha = 1f - endRing.value * 0.15f
                        }.shadow(
                            18.dp,
                            CircleShape,
                            ambientColor = colors.gold.copy(alpha = 0.32f),
                            spotColor = colors.gold.copy(alpha = 0.32f),
                        ).background(colors.gold, CircleShape),
                    )
                    Text(
                        "„Der perfekte Moment ist hier.“",
                        color = colors.goldHi,
                        fontFamily = Newsreader,
                        fontWeight = FontWeight.Light,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                    Text("Antippen, um zurückzukehren", color = colors.text3, fontFamily = Inter, fontSize = 13.sp)
                }
            }
        }
        if (dimAlpha > 0.001f && state?.phase != Phase.ENDED) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha))
                    .pmClickable {
                        dimmed = false
                        interactionTick++
                    },
            )
        }
        if (showStopDialog) {
            SessionStopDialog(
                onDismiss = {
                    showStopDialog = false
                    if (resumeAfterStopDialog) viewModel.togglePause()
                },
                onEnd = viewModel::stopSession,
                onSave = viewModel::saveSession,
            )
        }
    }
}

@Composable
private fun SessionStopDialog(
    onDismiss: () -> Unit,
    onEnd: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = LocalPmColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f))
                    .pmClickable(onClick = onDismiss),
            )
            PmCard(
                Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 24.dp).shadow(
                    24.dp,
                    RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.38f),
                    spotColor = Color.Black.copy(alpha = 0.38f),
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(28.dp)) {
                    Text(
                        "Sitzung stoppen?",
                        color = colors.text1,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    )
                    Text(
                        "Möchten Sie die Sitzung komplett beenden oder an diesem Punkt speichern?",
                        color = colors.text2,
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton("Beenden", colors.text2, onEnd, Modifier.weight(1f), height = 48)
                        PrimaryButton("Speichern", onSave, Modifier.weight(1f), height = 48)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionQuestions(
    runtime: SessionRuntime?,
    state: SessionState?,
    onToggleSpeaker: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val listState = rememberLazyListState()
    val questions = state?.questions.orEmpty()
    val current = state?.currentIndex ?: 0
    LaunchedEffect(current, questions.size) {
        if (questions.isNotEmpty() && current in questions.indices) {
            delay(if (listState.isScrollInProgress) 5_000 else 100)
            val viewportOffset = (listState.layoutInfo.viewportSize.height * 0.4f).roundToInt()
            listState.animateScrollToItem(current, scrollOffset = -viewportOffset)
        }
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 96.dp, bottom = 240.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(questions, key = { _, question -> question.id.takeIf { it != 0L } ?: question.hashCode() }) { index, question ->
                val active = index == current
                val past = index < current
                val animationDuration = if (reduced) 200 else 700
                val spec = tween<Float>(animationDuration, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                // Only the question that is leaving and the one arriving actually cross-fade
                // ("die alte schrumpft und verblasst, die neue waechst und leuchtet auf").
                // Every other row already sits at its target, so it needs no running animation.
                val crossfading = index >= current - 1 && index <= current + 1
                val targetAlpha = if (active) 1f else if (past) 0.3f else 0.55f
                val targetSize = if (active) 32f else 20f
                val targetColor = if (active) colors.goldHi else if (past) colors.text3 else colors.text2
                val targetEmojiScale = if (active) 1.15f else 1f
                val alphaState: State<Float>
                val emojiScaleState: State<Float>
                val questionSize: Float
                val questionColor: Color
                if (crossfading) {
                    alphaState = animateFloatAsState(targetAlpha, spec, label = "Frage Deckkraft")
                    emojiScaleState = animateFloatAsState(targetEmojiScale, spec, label = "Emoji Größe")
                    // Font size and colour drive text layout, so these two do recompose the row —
                    // but only for the up to three rows that are actually cross-fading.
                    questionSize = animateFloatAsState(targetSize, spec, label = "Frage Größe").value
                    questionColor = animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = tween(animationDuration, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                        label = "Frage Farbe",
                    ).value
                } else {
                    alphaState = remember(targetAlpha) { mutableFloatStateOf(targetAlpha) }
                    emojiScaleState = remember(targetEmojiScale) { mutableFloatStateOf(targetEmojiScale) }
                    questionSize = targetSize
                    questionColor = targetColor
                }
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        question.emoji,
                        fontSize = 24.sp,
                        lineHeight = 31.2.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = alphaState.value
                            val scale = emojiScaleState.value
                            scaleX = scale
                            scaleY = scale
                        },
                    )
                    Text(
                        question.text,
                        color = questionColor,
                        style = PmTextStyles.question.copy(
                            fontSize = questionSize.sp,
                            lineHeight = (questionSize * 1.55f).sp,
                        ),
                        modifier = Modifier.weight(1f).graphicsLayer { alpha = alphaState.value },
                    )
                }
            }
            if (state?.refillInFlight == true) {
                item { Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) { LoadingDots() } }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(56.dp).align(Alignment.TopCenter)
                .pmHeaderSurface(colors)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                runtime?.topic.orEmpty(),
                color = colors.text2,
                fontFamily = Inter,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val waiting = state?.offline == true || state?.phase == Phase.WAITING_NETWORK
            Box(
                Modifier.size(8.dp).then(
                    if (waiting) Modifier.border(1.dp, colors.goldDim, CircleShape)
                    else Modifier.background(colors.goldDim, CircleShape),
                ),
            )
            Spacer(Modifier.width(12.dp))
            SpeakerButton(state?.speakerOn == true, state != null && state.paused != true, onToggleSpeaker)
        }
        if (state != null) {
            Row(
                Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-24).dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionProgress(state, runtime)
                val controlShape = RoundedCornerShape(14.dp)
                Box(
                    // .pm-session-stop-button:active — amber outline
                    Modifier.size(48.dp)
                        .pmClickable(shape = controlShape, pressBorder = colors.amber, onClick = onTogglePause)
                        .background(colors.surface2, controlShape)
                        .border(1.dp, colors.goldDim.copy(alpha = 0.42f), controlShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        if (state.paused) "Sitzung fortsetzen" else "Sitzung pausieren",
                        tint = colors.gold,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    Modifier.size(48.dp)
                        .pmClickable(shape = controlShape, pressBorder = colors.amber, onClick = onStop)
                        .background(colors.surface2, controlShape)
                        .border(1.dp, colors.goldDim.copy(alpha = 0.42f), controlShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Stop, "Sitzung stoppen", tint = colors.text2, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PreparationBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPmColors.current
    Row(
        modifier = modifier.pmGlassSurface(colors, 14, colors.surface2)
            .pmClickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = colors.gold, modifier = Modifier.size(18.dp))
        Text("Zurück", color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun SessionProgress(state: SessionState?, runtime: SessionRuntime?) {
    if (state == null || runtime == null) return
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    val progress = remember { Animatable(0f) }
    val speaking = state.phase == Phase.SPEAKING
    // The amber pulse only exists while the ring stands still and speech is running.
    val speakingPulseState: State<Float> = if (speaking && !reduced && motionActive) {
        val pulseTransition = rememberInfiniteTransition(label = "Fortschrittsring")
        pulseTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.50f,
            animationSpec = infiniteRepeatable(
                tween(1_200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                RepeatMode.Reverse,
            ),
            label = "Bernsteinpuls",
        )
    } else {
        remember { mutableFloatStateOf(0.25f) }
    }
    LaunchedEffect(state.phase, state.currentIndex, state.currentRep, state.paused) {
        if (state.paused) return@LaunchedEffect
        progress.snapTo(if (state.phase == Phase.SPEAKING) 1f else 0f)
        val pause = when (state.phase) {
            Phase.PAUSE_REP -> runtime.config.pauseRepMs
            Phase.PAUSE_NEXT -> runtime.config.pauseNextMs
            else -> 0L
        }
        if (pause > 0) progress.animateTo(1f, tween(if (reduced) 200 else pause.toInt()))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).background(colors.surface2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Spacer(
                Modifier.matchParentSize().drawWithCache {
                    val stroke = Stroke(2.5.dp.toPx())
                    val roundStroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                    val track = colors.goldDim.copy(alpha = 0.28f)
                    onDrawBehind {
                        drawCircle(track, style = stroke)
                        drawArc(
                            color = if (speaking) colors.amber.copy(alpha = speakingPulseState.value) else colors.gold,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.value,
                            useCenter = false,
                            style = roundStroke,
                        )
                    }
                },
            )
            val totalSeconds = state.remainingMs.coerceAtLeast(0L) / 1_000L
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L),
                    color = colors.text2,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
                if (runtime.config.isEndless) {
                    Text("∞", color = colors.gold, fontFamily = Inter, fontSize = 11.sp, lineHeight = 11.sp)
                }
            }
        }
        Text(
            "Frage ${state.currentIndex + 1} · Wiederholung ${state.currentRep} von ${runtime.config.repsPerQuestion}",
            color = colors.text3,
            fontFamily = Inter,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (state.offline) {
            Text(
                "Keine Verbindung — die Sitzung wartet.",
                color = colors.text3,
                fontFamily = Inter,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun SessionIntroOverlay(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().background(colors.background)) {
        PreparationBackButton(
            onClick = viewModel::stopSession,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 8.dp),
        )
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            SectionLabel("Die KI fragt zuerst")
            Text(
                "„${viewModel.introQuestion}“",
                color = colors.goldHi,
                style = TextStyle(
                    fontFamily = Newsreader,
                    fontWeight = FontWeight.Light,
                    fontSize = 28.sp,
                    lineHeight = 42.sp,
                    shadow = Shadow(
                        colors.gold.copy(alpha = 0.10f),
                        Offset(0f, with(density) { 4.dp.toPx() }),
                        with(density) { 24.dp.toPx() },
                    ),
                ),
                modifier = Modifier.padding(top = 22.dp, bottom = 22.dp),
            )
            PrimaryButton("Antworten & beginnen", viewModel::openIntroSheet)
            Text(
                "Danach beginnt die Routine des Skills.",
                color = colors.text3,
                fontFamily = Inter,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun SessionErrorOverlay(viewModel: AppViewModel, error: String) {
    val colors = LocalPmColors.current
    Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error, color = colors.text2, fontFamily = Inter, fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Erneut versuchen", viewModel::retrySession)
            Spacer(Modifier.height(12.dp))
            OutlineButton("Zu ChatGPT", colors.gold, { viewModel.navigate(AppScreen.CHAT_GPT) })
            Spacer(Modifier.height(12.dp))
            OutlineButton("Zurück", colors.text2, viewModel::stopSession)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    var pendingDelete by remember { mutableStateOf<SessionEntity?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Verlauf",
            viewModel::back,
            action = {
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = "Verlauf sortieren: ${viewModel.historySort.label}" }
                            .pmClickable { sortMenuExpanded = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(24.dp)) {
                            listOf(22.dp, 16.dp, 10.dp).forEachIndexed { index, width ->
                                val lineWidth = width.toPx()
                                val y = size.height * (index + 1) / 4f
                                drawLine(
                                    color = colors.gold,
                                    start = Offset((size.width - lineWidth) / 2f, y),
                                    end = Offset((size.width + lineWidth) / 2f, y),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, colors.goldDim.copy(alpha = 0.26f)),
                        modifier = Modifier.width(220.dp),
                    ) {
                        HistorySort.entries.forEach { sort ->
                            val selected = sort == viewModel.historySort
                            DropdownMenuItem(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .background(
                                        if (selected) colors.surface2 else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    ),
                                text = {
                                    Text(
                                        sort.label,
                                        color = if (selected) colors.goldHi else colors.text1,
                                        fontFamily = Inter,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                trailingIcon = {
                                    if (selected) {
                                        Icon(Icons.Outlined.Check, null, tint = colors.gold)
                                    }
                                },
                                onClick = {
                                    viewModel.updateHistorySort(sort)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            },
        )
        if (viewModel.sessions.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OrbitRing(
                    Modifier.size(88.dp).border(1.5.dp, colors.goldDim.copy(alpha = 0.30f), CircleShape),
                ) {
                    Box(Modifier.size(12.dp).background(colors.goldDim, CircleShape))
                }
                Text("Noch keine Sitzungen.", color = colors.text3, fontFamily = Inter, fontSize = 15.sp, modifier = Modifier.padding(top = 20.dp))
            }
        } else {
            val sortedSessions = viewModel.sortedSessions
            val topSessions = sortedSessions.take(3)
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "top-three") {
                    val topShape = RoundedCornerShape(24.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surface2, topShape)
                            .border(1.5.dp, colors.goldDim.copy(alpha = 0.50f), topShape)
                            .padding(12.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(30.dp).background(colors.gold, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    null,
                                    tint = colors.background,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(
                                    "DEINE TOP 3",
                                    color = colors.goldHi,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    "Aktuell: ${viewModel.historySort.label}",
                                    color = colors.text2,
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        topSessions.forEachIndexed { index, session ->
                            DismissibleHistoryRow(
                                session = session,
                                rank = index + 1,
                                onDeleteRequest = { pendingDelete = session },
                                onClick = { viewModel.openHistoryDetail(session.id) },
                            )
                            if (index < topSessions.lastIndex) Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                items(sortedSessions.drop(3), key = SessionEntity::id) { session ->
                    DismissibleHistoryRow(
                        session = session,
                        onDeleteRequest = { pendingDelete = session },
                        onClick = { viewModel.openHistoryDetail(session.id) },
                    )
                }
                item {
                    Text(
                        "Nach links wischen zeigt „Löschen“",
                        color = colors.text3,
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
    pendingDelete?.let { session ->
        Dialog(onDismissRequest = { pendingDelete = null }) {
            PmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text(
                        "Eintrag wirklich löschen?",
                        color = colors.text1,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    )
                    Text(
                        "Möchten Sie diesen Eintrag wirklich löschen?",
                        color = colors.text2,
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton("Nein", colors.text2, { pendingDelete = null }, Modifier.weight(1f), height = 48)
                        PrimaryButton(
                            "Ja",
                            onClick = {
                                viewModel.deleteSession(session.id)
                                pendingDelete = null
                            },
                            modifier = Modifier.weight(1f),
                            height = 48,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleHistoryRow(
    session: SessionEntity,
    rank: Int? = null,
    onDeleteRequest: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalPmColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(colors.warning, RoundedCornerShape(20.dp)).padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Outlined.Delete, "Löschen", tint = colors.background) }
        },
    ) { HistoryRow(session, rank, onClick) }
}

@Composable
private fun HistoryRow(session: SessionEntity, rank: Int? = null, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    PmCard(
        Modifier.fillMaxWidth()
            .pmClickable(shape = RoundedCornerShape(32.dp), lift = true, onClick = onClick),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (rank != null) {
                val rankColor = when (rank) {
                    1 -> Color(0xFF5B8DEF)
                    2 -> colors.success
                    else -> Color(0xFFF08A36)
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .background(rankColor, CircleShape)
                        .border(1.5.dp, colors.background.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        rank.toString(),
                        color = if (colors.dark) colors.background else colors.text1,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    session.topic,
                    color = colors.text1,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 21.6.sp,
                )
                Text(
                    "${formatSessionDate(session.startedAt)} · ${formatSessionDuration(session.durationMin)} · ${session.questionCount} Fragen · ${voiceDisplayName(session.voiceName)}",
                    color = colors.text2,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun HistoryDetailScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    val density = LocalDensity.current
    val detail = viewModel.historyDetail
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", viewModel::back)
        if (detail == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)) {
                item {
                    val resumable = detail.session.resumeQuestionIndex != null
                    PrimaryButton(
                        if (resumable) "Sitzung fortsetzen" else "Erneut abspielen",
                        if (resumable) viewModel::resumeHistory else viewModel::replayHistory,
                    )
                    if (resumable) {
                        Spacer(Modifier.height(10.dp))
                        OutlineButton("Von vorne abspielen", colors.gold, viewModel::replayHistory)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ParameterCard("Pause", "${viewModel.pauseRep} s", { viewModel.openSheet(AppSheet.PAUSES) }, Modifier.weight(1f), compact = true)
                        ParameterCard("Wiederholungen", "${viewModel.repetitions}×", { viewModel.openSheet(AppSheet.REPETITIONS) }, Modifier.weight(1.2f), compact = true)
                        ParameterCard("Dauer", formatSessionDuration(viewModel.durationMinutes), { viewModel.openSheet(AppSheet.DURATION) }, Modifier.weight(1f), compact = true)
                    }
                    PmCard(
                        Modifier.fillMaxWidth().padding(top = 12.dp)
                            .pmClickable(shape = RoundedCornerShape(32.dp), lift = true) {
                                viewModel.toggleRandomReplay()
                            },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Zufällige Reihenfolge", color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text("Fragen werden gemischt abgespielt", color = colors.text3, fontFamily = Inter, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            PmSwitch(viewModel.randomReplay) { viewModel.toggleRandomReplay() }
                        }
                    }
                    PmCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        SelectionContainer {
                            Text(
                                detail.session.topic,
                                color = colors.text1,
                                fontFamily = Inter,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                items(detail.questions, key = { it.id }) { question ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        Text(question.emoji, fontSize = 20.sp, lineHeight = 28.sp)
                        Text(
                            question.text,
                            color = colors.text2,
                            style = PmTextStyles.question.copy(
                                shadow = Shadow(
                                    colors.gold.copy(alpha = 0.10f),
                                    Offset(0f, with(density) { 4.dp.toPx() }),
                                    with(density) { 24.dp.toPx() },
                                ),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun formatSessionDate(timestamp: Long): String =
    SimpleDateFormat("dd. MMM, HH:mm", Locale.GERMAN).format(Date(timestamp))

private fun formatSessionDuration(minutes: Int): String = if (minutes == 0) "Endlos" else "$minutes min"

private fun voiceDisplayName(id: String): String = id.substringAfterLast('-').removeSuffix("Neural")

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    appLockEnabled: Boolean,
    toggleAppLock: (Boolean) -> Unit,
) {
    val colors = LocalPmColors.current
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Einstellungen", viewModel::back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            SettingsSection("Ablauf") {
                SettingRow("Pause zwischen Wiederholungen", "${viewModel.pauseRep} s", onClick = { viewModel.openSheet(AppSheet.PAUSES) }, showChevron = true)
                SettingRow("Pause bis zur nächsten Frage", "${viewModel.pauseNext} s", onClick = { viewModel.openSheet(AppSheet.PAUSES) }, showChevron = true)
                SettingRow("Wiederholungen pro Frage", "${viewModel.repetitions}×", onClick = { viewModel.openSheet(AppSheet.REPETITIONS) }, showChevron = true)
                SettingRow("Sitzungsdauer", formatSessionDuration(viewModel.durationMinutes), onClick = { viewModel.openSheet(AppSheet.DURATION) }, divider = false, showChevron = true)
            }
            SettingsSection("Inhalt") {
                SettingRow(
                    "Gesprächsaufhänger",
                    viewModel.hooks.size.toString(),
                    onClick = { viewModel.navigate(AppScreen.HOOKS) },
                    showChevron = true,
                )
                SettingRow(
                    "Skills",
                    viewModel.activeSkill?.name ?: "",
                    onClick = { viewModel.navigate(AppScreen.SKILLS) },
                    showChevron = true,
                )
                QuestionPerspectiveSetting(viewModel)
            }
            SettingsSection("Stimme") {
                SettingRow(
                    "Anbieter",
                    if (viewModel.ttsProvider == TtsProvider.EDGE.id) "Microsoft Edge" else "Google Chirp 3 HD",
                    onClick = { viewModel.openSheet(AppSheet.PROVIDER) },
                    showChevron = true,
                )
                SettingRow(
                    "Stimme",
                    voiceDisplayName(viewModel.selectedVoice),
                    onClick = { viewModel.navigate(AppScreen.VOICE) },
                    showChevron = true,
                )
                VoiceSpeedSlider(viewModel.ttsSpeechRate, viewModel::updateTtsSpeechRate)
                SecureKeyRow(
                    label = "Google-API",
                    supporting = "TTS · Chirp 3 HD",
                    value = viewModel.googleApiKey,
                    visible = viewModel.showGoogleKey,
                    onValueChange = viewModel::updateGoogleApiKey,
                    onToggleVisibility = viewModel::toggleGoogleKeyVisibility,
                )
                SecureKeyRow(
                    label = "Groq-API",
                    supporting = "Whisper Large v3 · Spracheingabe",
                    value = viewModel.groqApiKey,
                    visible = viewModel.showGroqKey,
                    onValueChange = viewModel::updateGroqApiKey,
                    onToggleVisibility = viewModel::toggleGroqKeyVisibility,
                    divider = false,
                )
            }
            SettingsSection("KI-Verbindung") {
                SettingRow(
                    "Mit ChatGPT verbinden",
                    if (viewModel.chatGptState == ChatGptState.CONNECTED) viewModel.connectedEmail ?: "Verbunden" else "Nicht verbunden",
                    onClick = { viewModel.navigate(AppScreen.CHAT_GPT) },
                    showChevron = true,
                    statusColor = colors.success.takeIf { viewModel.chatGptState == ChatGptState.CONNECTED },
                    valueColor = if (viewModel.chatGptState == ChatGptState.CONNECTED) colors.text2 else colors.text3,
                    valueFontSize = if (viewModel.chatGptState == ChatGptState.CONNECTED) 13 else 14,
                )
                SettingRow("Modell", viewModel.model.label, onClick = { viewModel.openSheet(AppSheet.MODEL) }, showChevron = true)
                SettingRow("Denkstärke", viewModel.reasoning.label, onClick = { viewModel.openSheet(AppSheet.REASONING) }, divider = false, showChevron = true)
            }
            SettingsSection("Sicherheit") {
                SettingRow(
                    label = "App mit Fingerabdruck sperren",
                    supporting = "Nur starker biometrischer Fingerabdruck",
                    trailing = { PmSwitch(appLockEnabled, toggleAppLock) },
                    divider = false,
                )
            }
            SettingsSection("Darstellung") {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Segment("Hell", theme == "light", { viewModel.setTheme("light") }, Modifier.weight(1f))
                    Segment("Dunkel", theme == "dark", { viewModel.setTheme("dark") }, Modifier.weight(1f))
                    Segment("Wie System", theme == "system", { viewModel.setTheme("system") }, Modifier.weight(1f))
                }
            }
            SettingsSection("Sicherung") {
                SettingRow(
                    "Jetzt sichern",
                    value = if (viewModel.backupBusy) "…" else null,
                    supporting = if (viewModel.backupTargetChosen) {
                        viewModel.backupState
                    } else {
                        "Ort einmal wählen — z.B. Google Drive"
                    },
                    onClick = viewModel::saveToFile,
                    showChevron = !viewModel.backupBusy,
                    statusColor = if (viewModel.backupTargetChosen) colors.success else null,
                )
                SettingRow(
                    "Wiederherstellen",
                    supporting = "Sicherungsdatei auswählen und einspielen",
                    onClick = viewModel::restoreFromFileRequested,
                    showChevron = !viewModel.backupBusy,
                    divider = viewModel.backupTargetChosen,
                )
                if (viewModel.backupTargetChosen) {
                    SettingRow(
                        "Anderen Ort wählen",
                        onClick = viewModel::forgetBackupTarget,
                        divider = false,
                    )
                }
            }
            SettingsSection("Über") {
                Text(
                    "V ${viewModel.versionName} (${viewModel.versionStand})",
                    color = colors.text2,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun QuestionPerspectiveSetting(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Frageperspektive", color = colors.text1, fontFamily = Inter, fontSize = 15.sp)
        Text(
            "Wie die KI ihre Fragen formuliert",
            color = colors.text3,
            fontFamily = Inter,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segment(
                "Ich-Person",
                viewModel.questionPerspective == QuestionPerspective.FIRST_PERSON,
                { viewModel.updateQuestionPerspective(QuestionPerspective.FIRST_PERSON) },
                Modifier.weight(1f),
            )
            Segment(
                "Du-Person",
                viewModel.questionPerspective == QuestionPerspective.SECOND_PERSON,
                { viewModel.updateQuestionPerspective(QuestionPerspective.SECOND_PERSON) },
                Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSpeedSlider(value: Float, onValueChange: (Float) -> Unit) {
    val colors = LocalPmColors.current
    val displayValue = String.format(Locale.GERMAN, "%.2f", value).trimEnd('0').trimEnd(',')
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Geschwindigkeit", color = colors.text1, fontFamily = Inter, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                "$displayValue×",
                color = colors.gold,
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.7f..1.3f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = colors.goldHi,
                activeTrackColor = colors.gold,
                inactiveTrackColor = colors.surface2,
            ),
            thumb = { PmSliderThumb() },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
    Box(
        Modifier.fillMaxWidth().height(1.dp).background(
            Brush.horizontalGradient(
                listOf(Color.Transparent, colors.gold.copy(alpha = 0.11f), colors.surface2, Color.Transparent),
            ),
        ),
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        SectionLabel(title, Modifier.padding(bottom = 10.dp), decorated = true)
        PmCard(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

@Composable
private fun SecureKeyRow(
    label: String,
    supporting: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    divider: Boolean = true,
) {
    val colors = LocalPmColors.current
    Box {
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = colors.text1, fontFamily = Inter, fontSize = 15.sp)
                Text(supporting, color = colors.text3, fontFamily = Inter, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                cursorBrush = SolidColor(colors.gold),
                textStyle = TextStyle(
                    color = colors.text2,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    textAlign = TextAlign.End,
                ),
                modifier = Modifier.width(126.dp),
            )
            Icon(
                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                if (visible) "Schlüssel verbergen" else "Schlüssel anzeigen",
                tint = colors.goldDim,
                modifier = Modifier.padding(start = 10.dp).size(20.dp).pmClickable(onClick = onToggleVisibility),
            )
        }
        if (divider) {
            Box(
                Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, colors.gold.copy(alpha = 0.11f), colors.surface2, Color.Transparent),
                    ),
                ).align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun HooksScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    var draggedHookId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Gesprächsaufhänger",
            viewModel::back,
            action = {
                Icon(
                    Icons.Outlined.Add,
                    "Aufhänger hinzufügen",
                    tint = colors.gold,
                    modifier = Modifier.size(26.dp).pmClickable { viewModel.openHookEditor(null) },
                )
            },
            titleSize = 24,
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(viewModel.hooks, key = HookEntity::id) { hook ->
                var dragY by remember(hook.id) { mutableStateOf(0f) }
                val iconColor = hookDisplayColor(hook, colors.dark)
                val isDragging = draggedHookId == hook.id
                val cardAlpha by animateFloatAsState(
                    if (draggedHookId == null || isDragging) 1f else 0.45f,
                    label = "Aufhänger Deckkraft",
                )
                val cardScale by animateFloatAsState(
                    if (isDragging) 1.035f else 1f,
                    label = "Aufhänger Größe",
                )
                PmCard(
                    Modifier.fillMaxWidth().height(68.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragY else 0f
                            alpha = cardAlpha
                            scaleX = cardScale
                            scaleY = cardScale
                        }
                        // .pm-hooks-card
                        .pmClickable(shape = RoundedCornerShape(32.dp), lift = true) {
                            viewModel.openHookEditor(hook)
                        },
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).background(iconColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(hookDisplayIcon(hook), null, tint = iconColor, modifier = Modifier.size(23.dp))
                        }
                        Text(
                            hook.text,
                            color = colors.text1,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                        )
                        Icon(
                            Icons.Outlined.DragIndicator,
                            "Zum Sortieren halten und ziehen",
                            tint = colors.text3,
                            modifier = Modifier.size(24.dp).pointerInput(hook.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedHookId = hook.id },
                                    onDragEnd = {
                                        dragY = 0f
                                        draggedHookId = null
                                        viewModel.persistHookOrder()
                                    },
                                    onDragCancel = {
                                        dragY = 0f
                                        draggedHookId = null
                                        viewModel.persistHookOrder()
                                    },
                                ) { change, amount ->
                                    change.consume()
                                    dragY += amount.y
                                    val itemStep = 78.dp.toPx()
                                    var currentIndex = viewModel.hooks.indexOfFirst { it.id == hook.id }
                                    while (dragY > itemStep / 2f && currentIndex < viewModel.hooks.lastIndex) {
                                        viewModel.moveHook(currentIndex, currentIndex + 1)
                                        dragY -= itemStep
                                        currentIndex++
                                    }
                                    while (dragY < -itemStep / 2f && currentIndex > 0) {
                                        viewModel.moveHook(currentIndex, currentIndex - 1)
                                        dragY += itemStep
                                        currentIndex--
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun hookVisualIndex(hook: HookEntity): Int =
    ((hook.id.takeIf { it > 0L } ?: hook.sortIndex.toLong()) % 6L).toInt()

private fun hookDisplayIcon(hook: HookEntity): ImageVector = when (hookVisualIndex(hook)) {
    0 -> Icons.Outlined.SelfImprovement
    1 -> Icons.Outlined.FavoriteBorder
    2 -> Icons.Outlined.Psychology
    3 -> Icons.Outlined.Explore
    4 -> Icons.Outlined.Lightbulb
    else -> Icons.Outlined.AutoAwesome
}

private fun hookDisplayColor(hook: HookEntity, dark: Boolean): Color = when (hookVisualIndex(hook)) {
    0 -> if (dark) Color(0xFFFF9E80) else Color(0xFFB94B32)
    1 -> if (dark) Color(0xFFC4A7E7) else Color(0xFF7954A1)
    2 -> if (dark) Color(0xFF75C8B2) else Color(0xFF2A7D69)
    3 -> if (dark) Color(0xFF8AB4F8) else Color(0xFF3F6FA8)
    4 -> if (dark) Color(0xFFFFC66D) else Color(0xFFA66B00)
    else -> if (dark) Color(0xFFF28BA8) else Color(0xFFA54462)
}

@Composable
fun HookEditorScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Aufhänger bearbeiten", viewModel::back, titleSize = 24)
        Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PmTextArea(
                value = viewModel.hookEditorEmoji,
                onValueChange = viewModel::updateHookEmoji,
                placeholder = "✨",
                singleLine = true,
                textSize = 40,
                modifier = Modifier.size(88.dp),
                radius = 20,
            )
            Text("Antippen öffnet die Emoji-Tastatur", color = colors.text3, fontFamily = Inter, fontSize = 12.sp)
            PmTextArea(
                value = viewModel.hookEditorText,
                onValueChange = viewModel::updateHookText,
                placeholder = "Text des Aufhängers",
                modifier = Modifier.fillMaxWidth().height(140.dp),
                textSize = 16,
                radius = 26,
            )
        }
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Speichern", viewModel::saveHook)
            OutlineButton("Löschen", colors.warning, viewModel::deleteHook)
        }
    }
}

@Composable
fun SkillsScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Skills",
            viewModel::back,
            action = {
                Icon(
                    Icons.Outlined.Add,
                    "Skill hinzufügen",
                    tint = colors.gold,
                    modifier = Modifier.size(26.dp).pmClickable { viewModel.openSkillEditor(null) },
                )
            },
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(viewModel.skills, key = SkillEntity::id) { skill ->
                val selected = viewModel.activeSkill?.id == skill.id
                val shape = RoundedCornerShape(32.dp)
                PmCard(
                    // .pm-skills-card
                    Modifier.fillMaxWidth()
                        .pmClickable(role = Role.RadioButton, shape = shape, lift = true) {
                            viewModel.selectSkill(skill)
                        }
                        .then(
                            if (selected) Modifier.shadow(
                                12.dp,
                                shape,
                                ambientColor = colors.gold.copy(alpha = 0.24f),
                                spotColor = colors.gold.copy(alpha = 0.24f),
                            ).border(3.dp, colors.goldHi, shape) else Modifier,
                        ).semantics { this.selected = selected },
                    color = if (selected) colors.surface2 else colors.surface,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(skill.name, color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            Text(
                                skill.text.replace('\n', ' ').take(60) + if (skill.text.length > 60) "…" else "",
                                color = colors.text3,
                                fontFamily = Inter,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        }
                        Icon(
                            Icons.Outlined.Edit,
                            "Skill bearbeiten",
                            tint = colors.text3,
                            modifier = Modifier.size(20.dp).pmClickable { viewModel.openSkillEditor(skill) },
                        )
                        Spacer(Modifier.width(14.dp))
                        CheckMark(selected)
                    }
                }
            }
        }
    }
}

@Composable
fun SkillEditorScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    var skillTextFocused by remember { mutableStateOf(false) }
    val skillTextEditing = skillTextFocused && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(Modifier.fillMaxSize()) {
        if (!skillTextEditing) {
            ScreenHeader("Skill bearbeiten", viewModel::back, titleSize = 24)
        }
        Column(
            Modifier.fillMaxSize().padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = if (skillTextEditing) 8.dp else 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!skillTextEditing) {
                PmTextArea(
                    value = viewModel.skillEditorName,
                    onValueChange = viewModel::updateSkillName,
                    placeholder = "Name des Skills",
                    singleLine = true,
                    textSize = 16,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    radius = 18,
                )
            }
            PmTextArea(
                value = viewModel.skillEditorText,
                onValueChange = viewModel::updateSkillText,
                placeholder = "Vollständiger Skill-Text",
                mono = true,
                modifier = Modifier.fillMaxWidth().weight(1f).onFocusChanged {
                    skillTextFocused = it.isFocused
                },
                radius = 22,
            )
            if (!skillTextEditing) {
                PmCard(Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().pmClickable { viewModel.toggleOperatingMode() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Betriebsmodus (automatischer Anhang)", color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text("Wird unter jeden Skill gehängt und steuert das Frage-Format.", color = colors.text3, fontFamily = Inter, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Text(if (viewModel.operatingModeOpen) "−" else "+", color = colors.goldDim, fontSize = 22.sp)
                        }
                        if (viewModel.operatingModeOpen) {
                            PmTextArea(
                                value = viewModel.operatingModeText,
                                onValueChange = viewModel::updateOperatingMode,
                                placeholder = "Betriebsmodus",
                                mono = true,
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                textSize = 12,
                                radius = 22,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                PrimaryButton("Speichern", viewModel::saveSkill)
                OutlineButton("Löschen", colors.warning, viewModel::deleteSkill)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    val isGoogle = viewModel.voiceTab == TtsProvider.GOOGLE_CLOUD
    val noKey = isGoogle && viewModel.googleApiKey.isBlank()
    val voices = if (isGoogle) TtsCatalog.googleVoices else TtsCatalog.edgeVoices
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Stimme", viewModel::back)
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segment("Microsoft Edge", !isGoogle, { viewModel.updateVoiceTab(TtsProvider.EDGE) }, Modifier.weight(1f))
            Segment("Google Chirp 3 HD", isGoogle, { viewModel.updateVoiceTab(TtsProvider.GOOGLE_CLOUD) }, Modifier.weight(1f))
        }
        if (noKey) {
            PmCard(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                    .pmClickable { viewModel.navigate(AppScreen.SETTINGS) },
                radius = 20,
                color = colors.surface2,
            ) {
                Text(
                    "Bitte zuerst einen Google-API-Schlüssel hinterlegen. Zu den Einstellungen",
                    color = colors.text2,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.alpha(if (noKey) 0.35f else 1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(voices, key = { it.id }) { voice ->
                val selected = if (isGoogle) viewModel.googleVoice == voice.id else viewModel.edgeVoice == voice.id
                val starred = voice.name in setOf("Seraphina", "Florian") || voice.id in viewModel.favoriteVoiceIds
                val shape = RoundedCornerShape(20.dp)
                PmCard(
                    // .werft-voice-screen__voice — amber outline on touch
                    Modifier.fillMaxWidth().height(60.dp)
                        .pmCombinedClickable(
                            enabled = !noKey,
                            shape = shape,
                            lift = true,
                            pressBorder = colors.amber,
                            onClick = { viewModel.selectVoice(voice) },
                            onLongClick = { viewModel.toggleFavoriteVoice(voice) },
                        )
                        .then(
                            if (selected) Modifier.shadow(
                                12.dp,
                                shape,
                                ambientColor = colors.gold.copy(alpha = 0.24f),
                                spotColor = colors.gold.copy(alpha = 0.24f),
                            ).border(3.dp, colors.goldHi, shape) else Modifier,
                        ).semantics { this.selected = selected },
                    radius = 20,
                    color = if (selected) colors.surface2 else colors.surface,
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (starred) {
                            Text("★", color = colors.gold, fontSize = 13.sp, modifier = Modifier.padding(end = 7.dp))
                        }
                        Text(voice.name, color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            when {
                                voice.name == "Seraphina" -> "weiblich · mehrsprachig"
                                voice.name == "Florian" -> "männlich · mehrsprachig"
                                voice.gender.name == "FEMALE" -> "weiblich"
                                else -> "männlich"
                            },
                            color = colors.text3,
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        Box(
                            Modifier.size(36.dp).background(
                                if (viewModel.playingVoiceId == voice.id) colors.amber else colors.surface2,
                                CircleShape,
                            ).pmClickable(enabled = !noKey) { viewModel.previewVoice(voice) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (viewModel.playingVoiceId == voice.id) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                                "Stimme Probehören",
                                tint = if (viewModel.playingVoiceId == voice.id) colors.background else colors.gold,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.pmClickable(enabled = !noKey) { viewModel.selectVoice(voice) }) { CheckMark(selected) }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatGptScreen(
    viewModel: AppViewModel,
    connectChatGpt: () -> Unit,
    copyDeviceCode: (String) -> Unit,
    openDevicePage: (String) -> Unit,
) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Mit ChatGPT verbinden", viewModel::back, titleSize = 24)
        when (viewModel.chatGptState) {
            ChatGptState.DISCONNECTED -> Column(
                Modifier.fillMaxSize().padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OrbitRing(Modifier.size(96.dp).border(1.5.dp, colors.gold.copy(alpha = 0.36f), CircleShape)) {
                    Box(Modifier.size(14.dp).background(colors.goldHi, CircleShape))
                }
                Text(
                    "Perfect Moment braucht deinen ChatGPT-Zugang, um Fragen zu erzeugen.",
                    color = colors.text2,
                    fontFamily = Inter,
                    fontSize = 15.sp,
                    lineHeight = 23.25.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 26.dp),
                )
                PrimaryButton("Verbinden", connectChatGpt, Modifier.width(240.dp))
                viewModel.chatGptError?.let {
                    Text(it, color = colors.warning, fontFamily = Inter, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
                }
            }
            ChatGptState.CODE, ChatGptState.EXPIRED -> DeviceCodeState(viewModel, connectChatGpt, copyDeviceCode, openDevicePage)
            ChatGptState.CONNECTED -> Column(
                Modifier.fillMaxSize().padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(colors.success.copy(alpha = 0.5f), Color.Transparent),
                                center = center,
                                radius = size.minDimension / 2f,
                            ),
                        )
                    }
                    Box(Modifier.size(12.dp).background(colors.success, CircleShape))
                }
                Text(
                    viewModel.connectedEmail ?: "ChatGPT verbunden",
                    color = colors.text1,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    viewModel.connectedSince?.let { "Verbunden seit $it" } ?: "Verbunden",
                    color = colors.text2,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                OutlineButton("Verbindung trennen", colors.warning, viewModel::disconnectChatGpt, Modifier.width(240.dp), height = 50)
            }
        }
    }
}

@Composable
private fun DeviceCodeState(
    viewModel: AppViewModel,
    connectChatGpt: () -> Unit,
    copyDeviceCode: (String) -> Unit,
    openDevicePage: (String) -> Unit,
) {
    val colors = LocalPmColors.current
    val density = LocalDensity.current
    val info = viewModel.deviceAuthInfo
    val expired = viewModel.chatGptState == ChatGptState.EXPIRED
    val code = info?.userCode.orEmpty()
    val codeCharacters = code.filter(Char::isLetterOrDigit).take(8)
    val codeGroups = listOf(
        codeCharacters.take(4).padEnd(4, '–'),
        codeCharacters.drop(4).take(4).padEnd(4, '–'),
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            codeGroups.forEachIndexed { index, group ->
                Text(
                    group,
                    color = if (expired) colors.text3 else colors.goldHi,
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 40.sp,
                        letterSpacing = 4.sp,
                        shadow = Shadow(
                            colors.goldHi.copy(alpha = if (expired) 0f else 0.28f),
                            blurRadius = with(density) { 28.dp.toPx() },
                        ),
                    ),
                    textDecoration = if (expired) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    maxLines = 1,
                )
                if (index == 0) Box(Modifier.width(16.dp).height(1.5.dp).background(colors.goldDim))
            }
        }
        Text("Gib diesen Code auf der geöffneten Seite ein.", color = colors.text2, fontFamily = Inter, fontSize = 15.sp, modifier = Modifier.padding(top = 18.dp))
        Text(
            "auth.openai.com/codex/device",
            color = colors.text3,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallPill("Kopieren") { copyDeviceCode(info?.userCode.orEmpty()) }
            SmallPill("Seite erneut öffnen") { openDevicePage(info?.verificationUri ?: "https://auth.openai.com/codex/device") }
        }
        if (expired) {
            PrimaryButton("Neuen Code holen", connectChatGpt, Modifier.width(220.dp).padding(top = 24.dp), height = 48, textSize = 14)
        } else {
            Row(Modifier.padding(top = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                WaitingSpinner()
                Text("Warte auf Bestätigung…", color = colors.text2, fontFamily = Inter, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun WaitingSpinner() {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    // pm-fx-spin 2200ms linear infinite
    val rotationState: State<Float> = if (reduced || !motionActive) {
        remember { mutableFloatStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "Anmeldung")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Restart),
            label = "Anmeldedrehung",
        )
    }
    Canvas(Modifier.size(20.dp)) {
        drawArc(
            color = colors.gold,
            startAngle = rotationState.value,
            sweepAngle = 250f,
            useCenter = false,
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun SmallPill(text: String, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    Box(
        Modifier.height(48.dp)
            .pmClickable(role = Role.Button, shape = RoundedCornerShape(24.dp), onClick = onClick)
            .background(colors.surface2, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = colors.text1, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp) }
}

@Composable
fun RawDataScreen(viewModel: AppViewModel) {
    val colors = LocalPmColors.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rohdaten", viewModel::back, horizontalPadding = 20)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)) {
            item { RawHeading("Sitzungen (${viewModel.rawSessions.size})") }
            viewModel.rawSessions.forEach { detail ->
                item(key = "session-${detail.session.id}") {
                    RawBlock(
                        "Session #${detail.session.id}\n" +
                            "Thema: ${detail.session.topic}\n" +
                            "Start: ${formatSessionDate(detail.session.startedAt)}\n" +
                            "Dauer: ${formatSessionDuration(detail.session.durationMin)}\n" +
                            "Anbieter: ${detail.session.providerId}\n" +
                            "Stimme: ${detail.session.voiceName}\n" +
                            "Pausen: ${detail.session.pauseRep}/${detail.session.pauseNext} s\n" +
                            "Wiederholungen: ${detail.session.reps}\n" +
                            detail.questions.joinToString("\n") { "${it.orderIndex}: ${it.emoji} ${it.text}" },
                    )
                }
            }
            item { RawHeading("Skills (${viewModel.skills.size})") }
            items(viewModel.skills, key = { "raw-skill-${it.id}" }) { RawBlock("#${it.id} ${it.name}\n${it.text}") }
            item { RawHeading("Aufhänger (${viewModel.hooks.size})") }
            items(viewModel.hooks, key = { "raw-hook-${it.id}" }) { RawBlock("#${it.id} [${it.sortIndex}] ${it.emoji} ${it.text}") }
        }
    }
}

@Composable
private fun RawHeading(text: String) {
    SectionLabel(text, Modifier.padding(top = 12.dp, bottom = 10.dp))
}

@Composable
private fun RawBlock(text: String) {
    val colors = LocalPmColors.current
    PmCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), radius = 22) {
        Text(text, color = colors.text2, style = PmTextStyles.mono, modifier = Modifier.padding(16.dp))
    }
}
