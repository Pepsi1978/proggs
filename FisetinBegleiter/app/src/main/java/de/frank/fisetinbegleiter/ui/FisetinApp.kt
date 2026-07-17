package de.frank.fisetinbegleiter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.fisetinbegleiter.ui.theme.AppThemeMode
import de.frank.fisetinbegleiter.ui.theme.AppThemeVariant
import de.frank.fisetinbegleiter.ui.theme.LocalAppColors

private val FbEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val CssEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private enum class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Heute", Icons.Outlined.Home),
    TIMELINE("timeline", "Ablauf", Icons.Outlined.Checklist),
    STACK("stack", "Stack", Icons.Outlined.Layers),
    HISTORY("history", "Verlauf", Icons.Outlined.History),
    MORE("more", "System", Icons.Outlined.Tune),
}

@Composable
fun FisetinApp(
    themeVariant: AppThemeVariant,
    themeMode: AppThemeMode,
    onThemeVariantChange: (AppThemeVariant) -> Unit,
    onThemeModeToggle: () -> Unit,
    externalDestination: String?,
    onDestinationConsumed: () -> Unit,
    onExport: (String) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    showFirstStartNotice: Boolean,
    onNoticeConfirmed: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val backgroundStart by animateColorAsState(colors.bg0, tween(500), label = "background-start")
    val backgroundEnd by animateColorAsState(colors.bg1, tween(500), label = "background-end")
    var screen by remember { mutableStateOf(MainScreen.TODAY) }
    var noticeVisible by remember(showFirstStartNotice) { mutableStateOf(showFirstStartNotice) }

    LaunchedEffect(externalDestination) {
        externalDestination?.let { destination ->
            screen = MainScreen.entries.firstOrNull { it.route == destination } ?: MainScreen.TODAY
            onDestinationConsumed()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .angledGradient(listOf(backgroundStart, backgroundEnd), angleDegrees = 170f)
            .clipToBounds(),
    ) {
        val useRail = maxWidth >= 720.dp
        AppGlow(Modifier.align(Alignment.TopEnd))
        Row(Modifier.fillMaxSize()) {
            if (useRail) {
                FloatingNavigationRail(
                    selected = screen,
                    onSelected = { screen = it },
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    AppHeader(
                        subtitle = headerSubtitle(state),
                        themeMode = themeMode,
                        onThemeModeToggle = onThemeModeToggle,
                    )
                    val screenOffset = with(LocalDensity.current) { 14.dp.roundToPx() }
                    AnimatedContent(
                        targetState = screen,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (
                                fadeIn(tween(400, easing = FbEasing)) +
                                    slideInVertically(
                                        animationSpec = tween(400, easing = FbEasing),
                                        initialOffsetY = { screenOffset },
                                    )
                                ) togetherWith fadeOut(tween(160))
                        },
                        label = "main-screen",
                    ) { destination ->
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 18.dp,
                                top = 6.dp,
                                end = 18.dp,
                                bottom = if (useRail) 18.dp else 130.dp,
                            )
                        when (destination) {
                            MainScreen.TODAY -> TodayScreen(
                                state = state,
                                onCreateCure = viewModel::createCure,
                                onDrinkNow = viewModel::drinkNow,
                                onCancelCure = viewModel::cancelAllActiveCures,
                                onGoTimeline = { screen = MainScreen.TIMELINE },
                                onGoStack = { screen = MainScreen.STACK },
                                onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                                exactAlarmsAvailable = viewModel.canScheduleExactAlarms(),
                                modifier = contentModifier,
                            )
                            MainScreen.TIMELINE -> TimelineScreen(
                                state = state,
                                onMealDone = viewModel::markMeal,
                                onSpermidinDone = viewModel::markSpermidin,
                                onCompleteDay = viewModel::completeDay,
                                modifier = contentModifier,
                            )
                            MainScreen.STACK -> StackScreen(state, contentModifier)
                            MainScreen.HISTORY -> HistoryScreen(
                                state = state,
                                onUpdateNote = viewModel::updateNote,
                                onCancelCure = viewModel::cancelAllActiveCures,
                                onExport = { onExport(viewModel.exportText()) },
                                modifier = contentModifier,
                            )
                            MainScreen.MORE -> MoreScreen(
                                state = state,
                                themeVariant = themeVariant,
                                onThemeVariantChange = onThemeVariantChange,
                                onSaveProtocol = viewModel::saveProtocol,
                                onSaveIngredient = viewModel::saveIngredient,
                                onDeleteIngredient = viewModel::deleteIngredient,
                                onOpenBatterySettings = onOpenBatterySettings,
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                if (!useRail) {
                    FloatingBottomNavigation(
                        selected = screen,
                        onSelected = { screen = it },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    if (noticeVisible) {
        FirstStartNotice(
            onConfirm = {
                noticeVisible = false
                onNoticeConfirmed()
            },
        )
    }
}

@Composable
private fun AppGlow(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "app-glow")
    val floatOffset by transition.animateFloat(
        initialValue = -10f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = CssEaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "app-glow-offset",
    )
    Canvas(
        modifier = modifier
            .size(300.dp)
            .offset(x = 80.dp, y = (-90f + floatOffset).dp)
            .blur(50.dp),
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to colors.glow,
                    0.7f to Color.Transparent,
                    1f to Color.Transparent,
                ),
            ),
        )
    }
}

@Composable
private fun AppHeader(
    subtitle: String,
    themeMode: AppThemeMode,
    onThemeModeToggle: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val logoShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 9.dp,
                        shape = logoShape,
                        ambientColor = colors.glow,
                        spotColor = colors.glow,
                    )
                    .clip(logoShape)
                    .background(Brush.linearGradient(listOf(colors.accent, colors.accent2))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.onAccent,
                )
            }
            Column {
                Text(
                    text = "Fisetin-Begleiter",
                    color = colors.text,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = subtitle,
                    color = colors.sub,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.line, CircleShape)
                .clickable(onClick = onThemeModeToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (themeMode == AppThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (themeMode == AppThemeMode.DARK) "Helles Design" else "Dunkles Design",
                    modifier = Modifier.size(18.dp),
                    tint = colors.text,
                )
            }
        }
    }
}

@Composable
private fun FloatingBottomNavigation(
    selected: MainScreen,
    onSelected: (MainScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            .shadow(
                elevation = colors.shadow.blurRadius * 0.5f,
                shape = RoundedCornerShape(26.dp),
                ambientColor = colors.shadow.color,
                spotColor = colors.shadow.color,
            )
            .clip(RoundedCornerShape(26.dp))
            .background(colors.navBg)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MainScreen.entries.forEach { destination ->
            BottomNavigationItem(
                destination = destination,
                selected = destination == selected,
                onClick = { onSelected(destination) },
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItem(
    destination: MainScreen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val itemBackground by animateColorAsState(
        targetValue = if (selected) colors.accentSoft else Color.Transparent,
        animationSpec = tween(300),
        label = "navigation-background",
    )
    val itemColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.sub,
        animationSpec = tween(300),
        label = "navigation-content",
    )
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 9.dp,
                        shape = shape,
                        ambientColor = colors.glow,
                        spotColor = colors.glow,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(itemBackground)
            .clickable(onClick = onClick)
            .padding(top = 9.dp, bottom = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            modifier = Modifier.size(20.dp),
            tint = itemColor,
        )
        Text(
            text = destination.label,
            color = itemColor,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun FloatingNavigationRail(
    selected: MainScreen,
    onSelected: (MainScreen) -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            .width(82.dp)
            .fillMaxHeight()
            .shadow(
                elevation = colors.shadow.blurRadius * 0.5f,
                shape = RoundedCornerShape(26.dp),
                ambientColor = colors.shadow.color,
                spotColor = colors.shadow.color,
            )
            .clip(RoundedCornerShape(26.dp))
            .background(colors.navBg)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(26.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MainScreen.entries.forEach { destination ->
            val isSelected = destination == selected
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) colors.accentSoft else Color.Transparent)
                    .clickable { onSelected(destination) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) colors.accent else colors.sub,
                )
                Text(
                    text = destination.label,
                    color = if (isSelected) colors.accent else colors.sub,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FirstStartNotice(onConfirm: () -> Unit) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73080614))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350, easing = FbEasing)) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(350, easing = FbEasing),
                ),
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    Text(
                        text = "Wichtiger Hinweis",
                        color = colors.text,
                        fontSize = 18.sp,
                        lineHeight = 25.2.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Diese App bildet nur dein persönliches, selbst festgelegtes Protokoll ab. " +
                            "Sie ist keine medizinische Beratung. Bei Unsicherheiten oder Wechselwirkungen ärztlichen Rat einholen.",
                        modifier = Modifier.padding(top = 10.dp),
                        color = colors.sub,
                        fontSize = 13.5.sp,
                        lineHeight = 20.925.sp,
                    )
                    Text(
                        text = "Venlafaxin wird durch die Kur nicht verändert und nie ausgesetzt.",
                        modifier = Modifier.padding(top = 10.dp),
                        color = colors.text,
                        fontSize = 13.5.sp,
                        lineHeight = 20.25.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AccentGradientButton(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    ) {
                        Text(
                            text = "Verstanden",
                            fontSize = 14.5.sp,
                            lineHeight = 20.3.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun headerSubtitle(state: MainUiState): String {
    val cure = state.activeCure?.cure ?: return "Bereit für die nächste Kur"
    val day = state.currentDay ?: return "Bereit für die nächste Kur"
    val prefix = "Tag ${day.dayNumber} von ${cure.plannedDurationDays}"
    return when {
        day.t0 == null -> "$prefix · wartet auf den Drink"
        state.blockActive -> "$prefix · Sperrfenster läuft"
        else -> "$prefix · alles erlaubt"
    }
}
