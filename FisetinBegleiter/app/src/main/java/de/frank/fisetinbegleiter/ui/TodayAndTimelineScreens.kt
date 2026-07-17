package de.frank.fisetinbegleiter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.fisetinbegleiter.data.AppCureState
import de.frank.fisetinbegleiter.data.IngredientEntity
import de.frank.fisetinbegleiter.data.IngredientPhase
import de.frank.fisetinbegleiter.domain.timeline
import de.frank.fisetinbegleiter.domain.plannedStartAt
import de.frank.fisetinbegleiter.ui.theme.LocalAppColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal val FbMotionEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun TodayScreen(
    state: MainUiState,
    onCreateCure: (Int, String, String, LocalDate, Int) -> Unit,
    onDrinkNow: () -> Unit,
    onCancelCure: () -> Unit,
    onGoTimeline: () -> Unit,
    onGoStack: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    exactAlarmsAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var showSetup by remember { mutableStateOf(false) }
    var showDrink by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }

    if (!state.initialized) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.accent)
        }
        return
    }

    val active = state.activeCure != null
    val day = state.currentDay
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("Heute", "Dein persönliches Protokoll, Schritt für Schritt.") }
        item { TodayHero(state) }

        if (active && day?.t0 != null) {
            item { NextStepCard(state, onGoTimeline) }
            item {
                DesignOutlineButton(
                    text = "Angepassten Morgen-Stack ansehen",
                    onClick = onGoStack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else if (active) {
            item {
                AccentGradientButton(
                    onClick = { showDrink = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Drink-Checkliste öffnen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            item {
                AccentGradientButton(
                    onClick = { showSetup = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Neue Kur starten", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!exactAlarmsAvailable) {
            item {
                StatusNotice(
                    title = "Exakte Alarme freigeben",
                    text = "Android erlaubt derzeit nur angenäherte Erinnerungen. Gib exakte Alarme für punktgenaue Timer frei.",
                    containerColor = colors.warnBg,
                    contentColor = colors.warn,
                    icon = { Icon(Icons.Outlined.Schedule, null, tint = colors.warn, modifier = Modifier.size(20.dp)) },
                    trailing = {
                        SmallActionButton("Öffnen", onOpenExactAlarmSettings)
                    },
                )
            }
        }
        item {
            StatusNotice(
                title = "Harte Regel",
                text = "Kein Vitamin C oder andere starke Antioxidantien zeitgleich oder in den ersten 4 Stunden nach T0.",
                containerColor = colors.badBg,
                contentColor = colors.bad,
                icon = { Icon(Icons.Outlined.WarningAmber, null, tint = colors.bad, modifier = Modifier.size(20.dp)) },
            )
        }
        if (active) {
            item {
                DesignOutlineButton(
                    text = "Kur abbrechen und zurücksetzen",
                    onClick = { showCancelConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = colors.bad,
                    borderColor = colors.badBg,
                    verticalPadding = 13.dp,
                )
            }
        }
    }

    if (showSetup) {
        SetupSheet(
            defaultDuration = state.protocol.standardDurationDays,
            defaultStartMinute = state.protocol.preferredStartMinuteOfDay,
            onDismiss = { showSetup = false },
            onConfirm = { duration, liquid, fat, startDate, startMinute ->
                onCreateCure(duration, liquid, fat, startDate, startMinute)
                showSetup = false
            },
        )
    }
    if (showDrink) {
        DrinkSheet(
            ingredients = state.ingredients.filter { it.phase == IngredientPhase.DRINK },
            carrierLiquid = state.activeCure?.cure?.carrierLiquid.orEmpty(),
            fatCarrier = state.activeCure?.cure?.fatCarrier.orEmpty(),
            onDismiss = { showDrink = false },
            onDrinkNow = {
                onDrinkNow()
                showDrink = false
            },
        )
    }
    if (showCancelConfirmation) {
        FbPopDialog(onDismiss = { showCancelConfirmation = false }) {
            Text("Vollständig zurücksetzen?", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Der aktive Ablauf, alle zugehörigen Schritte, Alarme und der Eintrag im Verlauf werden vollständig gelöscht.",
                modifier = Modifier.padding(top = 10.dp),
                color = colors.sub,
                fontSize = 13.5.sp,
                lineHeight = 20.925.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DesignOutlineButton("Behalten", { showCancelConfirmation = false }, Modifier.weight(1f), verticalPadding = 13.dp)
                FlatDesignButton(
                    text = "Löschen",
                    onClick = {
                        onCancelCure()
                        showCancelConfirmation = false
                    },
                    modifier = Modifier.weight(1f),
                    background = colors.badBg,
                    contentColor = colors.bad,
                )
            }
        }
    }
}

@Composable
private fun TodayHero(state: MainUiState) {
    val colors = LocalAppColors.current
    val active = state.activeCure != null
    val day = state.currentDay
    val cure = state.activeCure?.cure
    val (title, text) = when {
        !active -> "Keine Kur aktiv" to "Lege die Kur in einen Freiblock: nüchterner Morgen, Mahlzeiten und das 4-Stunden-Fenster brauchen einen freien Tag."
        day?.t0 == null -> "Nächster Schritt: Fisetin-Drink" to "Morgens nüchtern. Öffne die Checkliste und setze T0 erst direkt nach dem Trinken."
        state.blockActive -> "Sperrfenster aktiv" to "Antioxidantien sind ab ${formatTime(state.blockEnd!!)} wieder erlaubt."
        else -> "Alles im erlaubten Bereich" to "Grünes Licht: Aktuell ist keine Antioxidantien-Sperre aktiv."
    }

    Box(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .align(Alignment.TopCenter)
                .width(240.dp)
                .height(140.dp)
                .blur(30.dp),
        ) {
            drawOval(Brush.radialGradient(listOf(colors.glow, Color.Transparent)))
        }
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        ) {
            when {
                state.blockActive && day?.t0 != null -> CountdownRing(day.t0, state.blockEnd!!, state.now)
                active && day?.t0 == null -> HeroIcon(colors.accentSoft, colors.accent, Icons.Outlined.WaterDrop, pulse = true)
                active -> HeroIcon(colors.okBg, colors.ok, Icons.Outlined.Check)
                else -> HeroIcon(colors.chip, colors.sub, Icons.Outlined.Add)
            }
            Text(
                title,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
                color = colors.text,
                fontSize = 19.sp,
                lineHeight = 26.6.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text,
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 290.dp),
                color = colors.sub,
                fontSize = 13.5.sp,
                lineHeight = 20.25.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (active && day != null && cure != null) {
                val percent = (((day.dayNumber - 1f) / cure.plannedDurationDays) * 100).roundToInt()
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Tag ${day.dayNumber} von ${cure.plannedDurationDays}", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("$percent %", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(cure.plannedDurationDays) { index ->
                        val brush = when {
                            index + 1 < day.dayNumber -> Brush.horizontalGradient(listOf(colors.ok, colors.ok))
                            index + 1 == day.dayNumber -> Brush.horizontalGradient(listOf(colors.accent, colors.accent2))
                            else -> Brush.horizontalGradient(listOf(colors.chip, colors.chip))
                        }
                        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(99.dp)).background(brush))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.HeroIcon(background: Color, foreground: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, pulse: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "hero-icon-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_200), RepeatMode.Reverse),
        label = "hero-icon-alpha",
    )
    val alpha = if (pulse) pulseAlpha else 1f
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .size(96.dp)
            .shadow(if (pulse) 15.dp else 0.dp, CircleShape, ambientColor = foreground, spotColor = foreground)
            .alpha(alpha)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = foreground, modifier = Modifier.size(if (icon == Icons.Outlined.Check) 42.dp else 40.dp))
    }
}

@Composable
private fun ColumnScope.CountdownRing(t0: Long, blockEnd: Long, now: Long) {
    val colors = LocalAppColors.current
    val duration = (blockEnd - t0).coerceAtLeast(1L)
    val progress = ((now - t0).toFloat() / duration).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1_000, easing = LinearEasing),
        label = "countdown-ring",
    )
    Box(Modifier.align(Alignment.CenterHorizontally).size(152.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .shadow(4.dp, CircleShape, ambientColor = colors.glow, spotColor = colors.glow),
        ) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            val radius = 64.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)
            drawCircle(colors.chip, radius = radius, style = Stroke(width = 10.dp.toPx()))
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(colors.accent, colors.accent2),
                    start = topLeft,
                    end = Offset(topLeft.x + arcSize.width, topLeft.y + arcSize.height),
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatRingCountdown(blockEnd, now), color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text("VERBLEIBEND", color = colors.sub, fontSize = 10.sp, letterSpacing = 1.6.sp)
            Text("frei ab ${formatTime(blockEnd)}", modifier = Modifier.padding(top = 4.dp), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatRingCountdown(target: Long, now: Long): String {
    val total = ((target - now).coerceAtLeast(0L) / 1_000)
    return "%d:%02d:%02d".format(total / 3_600, (total % 3_600) / 60, total % 60)
}

@Composable
private fun NextStepCard(state: MainUiState, onGoTimeline: () -> Unit) {
    val colors = LocalAppColors.current
    val day = state.currentDay ?: return
    val times = timeline(day.t0 ?: return, state.protocol)
    val (title, text) = when {
        day.mealDoneAt == null && state.now < times.mealDeadline -> "Fettige Mahlzeit" to "Bis ${formatTime(times.mealDeadline)}, noch ${formatCountdown(times.mealDeadline, state.now)}."
        day.spermidinDoneAt == null && state.now < times.spermidinStart -> "Spermidin-Fenster folgt" to "Öffnet um ${formatTime(times.spermidinStart)}."
        day.spermidinDoneAt == null && state.now < times.spermidinEnd -> "Spermidin-Schritt jetzt" to "6 mg Spermidin, optional Piperin und eine leichte Mahlzeit."
        state.blockActive -> "Sperrfenster läuft" to "Antioxidantien ab ${formatTime(times.blockEnd)} wieder erlaubt."
        state.canCompleteDay -> "Tag bereit zum Abschluss" to "Alle erforderlichen Schritte sind erledigt."
        else -> "Ablauf prüfen" to "Offene Schritte im Tagesablauf ansehen."
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(colors.shadow.blurRadius * 0.5f, shape, ambientColor = colors.shadow.color, spotColor = colors.shadow.color)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.accentSoft, Color.Transparent)))
            .border(1.dp, colors.cardBorder, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(colors.accent, colors.accent2))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text(text, color = colors.sub, fontSize = 12.5.sp, lineHeight = 18.125.sp)
        }
        SmallActionButton("Öffnen", onGoTimeline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupSheet(
    defaultDuration: Int,
    defaultStartMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, LocalDate, Int) -> Unit,
) {
    var duration by remember { mutableIntStateOf(defaultDuration.coerceIn(2, 3)) }
    var liquid by remember { mutableStateOf("Wasser") }
    var fat by remember { mutableStateOf("MCT-Öl") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var startMinute by remember { mutableIntStateOf(defaultStartMinute.coerceIn(0, 1_425)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val plannedStart = startDate?.let { plannedStartAt(it, startMinute) }
    val validStart = plannedStart?.let { it > System.currentTimeMillis() } == true
    FbBottomSheet(onDismiss) {
        Text("Kur einrichten", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        SheetLabel("Dauer")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3).forEach { value ->
                SelectionButton("$value Tage", duration == value, { duration = value }, Modifier.weight(1f))
            }
        }
        SheetLabel("Trägerflüssigkeit")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Wasser", "Grüner Tee").forEach { value ->
                SelectionButton(value, liquid == value, { liquid = value }, Modifier.weight(1f))
            }
        }
        SheetLabel("Fettträger")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MCT-Öl", "Olivenöl").forEach { value ->
                SelectionButton(value, fat == value, { fat = value }, Modifier.weight(1f))
            }
        }
        SheetLabel("Geplantes Datum / Startzeit")
        DesignOutlineButton(
            text = startDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Datum auswählen",
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        if (startDate != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SquareControl("−15", {
                    startMinute = (startMinute - 15).coerceAtLeast(0)
                    startError = false
                })
                Text(
                    "%02d:%02d".format(startMinute / 60, startMinute % 60),
                    modifier = Modifier.weight(1f),
                    color = colors.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                SquareControl("+15", {
                    startMinute = (startMinute + 15).coerceAtMost(1_425)
                    startError = false
                })
            }
            Text(
                if (validStart && !startError) {
                    "Start-Erinnerung am ${startDate!!.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} um %02d:%02d Uhr.".format(startMinute / 60, startMinute % 60)
                } else {
                    "Datum und Startzeit müssen in der Zukunft liegen."
                },
                color = if (validStart && !startError) colors.sub else colors.bad,
                fontSize = 12.5.sp,
                lineHeight = 18.75.sp,
            )
        } else {
            Text("Wähle zuerst den Kurtag und danach die Startzeit.", color = colors.sub, fontSize = 12.5.sp, lineHeight = 18.75.sp)
        }
        Text("T0 wird erst gesetzt, wenn du „Drink getrunken – jetzt“ bestätigst.", color = colors.sub, fontSize = 12.5.sp, lineHeight = 18.75.sp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DesignOutlineButton("Abbrechen", onDismiss, Modifier.weight(1f), verticalPadding = 14.dp)
            AccentGradientButton(
                onClick = {
                    val selectedDate = startDate
                    if (selectedDate != null && plannedStartAt(selectedDate, startMinute) > System.currentTimeMillis()) {
                        onConfirm(duration, liquid, fat, selectedDate, startMinute)
                    } else {
                        startError = true
                    }
                },
                enabled = validStart,
                modifier = Modifier.weight(1.4f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(14.dp),
            ) { Text("Kur anlegen", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialDisplayedMonthMillis = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: todayUtcMillis,
            selectableDates = remember(todayUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMillis
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        val selected = datePickerState.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        if (selected != null) {
                            startDate = selected
                            if (selected == LocalDate.now()) {
                                val current = LocalTime.now()
                                val nextQuarter = ((current.hour * 60 + current.minute) / 15 + 1) * 15
                                if (startMinute <= current.hour * 60 + current.minute) {
                                    startMinute = nextQuarter.coerceAtMost(1_425)
                                }
                            }
                            startError = false
                            showDatePicker = false
                        }
                    },
                ) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DrinkSheet(
    ingredients: List<IngredientEntity>,
    carrierLiquid: String,
    fatCarrier: String,
    onDismiss: () -> Unit,
    onDrinkNow: () -> Unit,
) {
    val colors = LocalAppColors.current
    val checked = remember(ingredients) { mutableStateMapOf<Long, Boolean>().apply { ingredients.forEach { put(it.id, false) } } }
    val complete = ingredients.isNotEmpty() && ingredients.all { checked[it.id] == true }
    FbBottomSheet(onDismiss) {
        Text("Drink-Checkliste", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Gewählt: $carrierLiquid · $fatCarrier", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        ingredients.forEach { ingredient ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.chip)
                    .clickable { checked[ingredient.id] = checked[ingredient.id] != true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                DesignCheckbox(checked[ingredient.id] == true) { checked[ingredient.id] = it }
                Column {
                    Row {
                        Text(ingredient.name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(" · ${ingredient.amount}", color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(ingredient.note, color = colors.sub, fontSize = 12.sp, lineHeight = 16.8.sp)
                }
            }
        }
        Text("Alles intensiv mixen oder shaken und sofort trinken.", modifier = Modifier.fillMaxWidth(), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DesignOutlineButton("Abbrechen", onDismiss, Modifier.weight(1f), verticalPadding = 14.dp)
            AccentGradientButton(
                onClick = onDrinkNow,
                enabled = complete,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(14.dp),
            ) { Text("Drink getrunken – jetzt", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun TimelineScreen(
    state: MainUiState,
    onMealDone: () -> Unit,
    onSpermidinDone: () -> Unit,
    onCompleteDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val day = state.currentDay
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("Tagesablauf", "Alle Uhrzeiten werden ab T0 berechnet.") }
        if (day?.t0 == null) {
            item {
                GlassCard(shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(18.dp)) {
                    Text("Noch kein T0", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Trinke zuerst den Fisetin-Drink über den Heute-Bildschirm.", modifier = Modifier.padding(top = 3.dp), color = colors.sub, fontSize = 13.sp)
                }
            }
        } else {
            val times = timeline(day.t0, state.protocol)
            val drinkItems = state.ingredients.filter { it.phase == IngredientPhase.DRINK }.map { "${it.name} · ${it.amount}" }
            val mealItems = listOfNotNull(state.activeCure?.cure?.fatCarrier?.takeIf { it.isNotBlank() })
            val spermidinItems = state.ingredients.filter { it.phase == IngredientPhase.SPERMIDIN }.map { "${it.name} · ${it.amount}" }
            item {
                Column {
                    TimelineStep("Fisetin-Drink", "${formatTime(times.t0)} · T0 gesetzt", true, null, drinkItems, false)
                    TimelineStep("Fettige Mahlzeit", "bis ${formatTime(times.mealDeadline)}", day.mealDoneAt != null, if (day.mealDoneAt == null) onMealDone else null, mealItems, false)
                    TimelineStep("Spermidin + leichte Mahlzeit", "${clock(times.spermidinStart)}–${formatTime(times.spermidinEnd)}", day.spermidinDoneAt != null, if (day.spermidinDoneAt == null) onSpermidinDone else null, spermidinItems, false)
                    TimelineStep("Sperrfenster vorbei", formatTime(times.blockEnd), !state.blockActive, null, emptyList(), true)
                }
            }
            if (state.blockActive) {
                item {
                    StatusNotice(
                        "Sperrfenster aktiv",
                        "Noch ${formatCountdown(times.blockEnd, state.now)}.",
                        colors.warnBg,
                        colors.warn,
                        icon = { Icon(Icons.Outlined.Schedule, null, tint = colors.warn, modifier = Modifier.size(20.dp)) },
                    )
                }
            }
            item {
                AccentGradientButton(onClick = onCompleteDay, enabled = state.canCompleteDay, modifier = Modifier.fillMaxWidth()) {
                    Text(if (day.dayNumber == state.activeCure?.cure?.plannedDurationDays) "Kur abschließen" else "Tag ${day.dayNumber} abschließen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!state.canCompleteDay) {
                item {
                    Text(
                        "Der Abschluss wird freigeschaltet, sobald Mahlzeit, Spermidin-Schritt und Sperrfenster erledigt sind.",
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.sub,
                        fontSize = 12.5.sp,
                        lineHeight = 18.75.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineStep(
    title: String,
    time: String,
    done: Boolean,
    action: (() -> Unit)?,
    ingredients: List<String>,
    last: Boolean,
) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.width(36.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(if (done) colors.okBg else colors.chip).border(1.dp, colors.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (done) Icons.Outlined.Check else Icons.Outlined.RadioButtonUnchecked, null, tint = if (done) colors.ok else colors.sub, modifier = Modifier.size(17.dp))
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(colors.line))
        }
        GlassCard(
            modifier = Modifier.weight(1f).padding(bottom = if (last) 0.dp else 10.dp),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = colors.text, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Text(time, color = colors.sub, fontSize = 12.5.sp)
                    if (ingredients.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            ingredients.forEach { IngredientChip(it) }
                        }
                    }
                }
                action?.let { SmallActionButton("Erledigt", it) }
            }
        }
    }
}

@Composable
private fun IngredientChip(text: String) {
    val colors = LocalAppColors.current
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colors.chip).border(1.dp, colors.line, RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
        color = colors.sub,
        fontSize = 10.5.sp,
        lineHeight = 14.7.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun clock(timestamp: Long): String = formatTime(timestamp).removeSuffix(" Uhr")

@Composable
internal fun FbBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color(0x73080614))
                .navigationBarsPadding()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, easing = FbMotionEasing)) + slideInVertically(tween(400, easing = FbMotionEasing)) { it },
            ) {
                val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.94f)
                        .clip(shape)
                        .background(colors.card)
                        .border(BorderStroke(1.dp, colors.cardBorder), shape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 38.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(99.dp)).background(colors.line))
                    content()
                }
            }
        }
    }
}

@Composable
internal fun FbPopDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0x73080614)).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350, easing = FbMotionEasing)) + scaleIn(
                    animationSpec = tween(350, easing = FbMotionEasing),
                    initialScale = 0.92f,
                ),
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun DesignOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalAppColors.current.text,
    borderColor: Color = LocalAppColors.current.line,
    verticalPadding: androidx.compose.ui.unit.Dp = 14.dp,
    enabled: Boolean = true,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .clip(shape)
            .background(colors.chip)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun FlatDesignButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, background: Color, contentColor: Color, enabled: Boolean = true) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(background).clickable(enabled = enabled, onClick = onClick).padding(13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun SmallActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = LocalAppColors.current
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(colors.accentSoft).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.accent.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SelectionButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.clip(shape).background(if (selected) colors.accentSoft else colors.chip).border(1.dp, if (selected) colors.accent else colors.line, shape).clickable(onClick = onClick).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) colors.accent else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun DesignCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(if (checked) colors.accent else Color.Transparent).border(2.dp, if (checked) colors.accent else colors.sub, RoundedCornerShape(8.dp)).clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Outlined.Check, null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SheetLabel(text: String) {
    val colors = LocalAppColors.current
    Text(text.uppercase(), color = colors.sub, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}

@Composable
private fun SquareControl(text: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.chip).border(1.dp, colors.line, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
