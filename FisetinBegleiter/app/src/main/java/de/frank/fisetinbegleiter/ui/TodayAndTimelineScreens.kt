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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawBehind
import de.frank.fisetinbegleiter.alarm.hourLabel
import de.frank.fisetinbegleiter.data.StackCategory
import de.frank.fisetinbegleiter.domain.plannedT0
import de.frank.fisetinbegleiter.domain.prePhaseStartAt
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
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import de.frank.fisetinbegleiter.data.CureEntity
import de.frank.fisetinbegleiter.data.CureStatus
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
    notificationsAvailable: Boolean,
    onOpenNotificationSettings: () -> Unit,
    exactAlarmsAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var showDrink by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }

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
        contentPadding = PaddingValues(top = 4.dp, bottom = LocalScreenBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle(todaySectionTitle(state.activeCure?.cure), "Dein persönliches Protokoll, Schritt für Schritt.") }
        item { TodayHero(state) }

        if (active && day?.t0 != null) {
            item { NextStepCard(state, onGoTimeline) }
            item {
                DesignOutlineButton(
                    text = "Stack: Was darf ich jetzt nehmen?",
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
            item {
                DesignOutlineButton(
                    text = "Stack: Was darf ich jetzt nehmen?",
                    onClick = onGoStack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                DesignOutlineButton(
                    text = "Ganzen Ablauf ansehen",
                    onClick = onGoTimeline,
                    modifier = Modifier.fillMaxWidth(),
                )
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
        if (!notificationsAvailable) {
            item {
                StatusNotice(
                    title = "Benachrichtigungen freigeben",
                    text = "Android blockiert derzeit die Kur-Erinnerungen. Aktiviere den Kanal „Kur-Schritte“ in den App-Einstellungen.",
                    containerColor = colors.warnBg,
                    contentColor = colors.warn,
                    icon = { Icon(Icons.Outlined.WarningAmber, null, tint = colors.warn, modifier = Modifier.size(20.dp)) },
                    trailing = {
                        SmallActionButton("Öffnen", onOpenNotificationSettings)
                    },
                )
            }
        }
        item {
            StatusNotice(
                title = "Antioxidantien-Regel",
                text = "Vorstufe: ab ${hourLabel(state.protocol.preBlockMinutes)} vor dem Drink kein Vitamin C, Vitamin E, Curcumin, Q10 oder Astaxanthin. " +
                    "Sperre: bis ${hourLabel(state.protocol.antioxidantBlockMinutes)} nach T0. Am Kurmorgen nüchtern bleiben, nur Wasser.",
                containerColor = colors.badBg,
                contentColor = colors.bad,
                icon = { Icon(Icons.Outlined.WarningAmber, null, tint = colors.bad, modifier = Modifier.size(20.dp)) },
            )
        }
        if (active) {
            item {
                DesignOutlineButton(
                    text = if (state.completedDayCount > 0) {
                        "Kur nach ${dayLabel(state.completedDayCount)} beenden"
                    } else {
                        "Kur abbrechen und zurücksetzen"
                    },
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
            preBlockMinutes = state.protocol.preBlockMinutes,
            onDismiss = { showSetup = false },
            onConfirm = { duration, startDate, startMinute ->
                onCreateCure(duration, DRINK_LIQUID, DRINK_FAT, startDate, startMinute)
                showSetup = false
            },
        )
    }
    if (showDrink) {
        DrinkSheet(
            ingredients = state.ingredients.filter { it.phase == IngredientPhase.DRINK },
            onDismiss = { showDrink = false },
            onDrinkNow = {
                onDrinkNow()
                showDrink = false
            },
        )
    }
    if (showCancelConfirmation) {
        val completedDays = state.completedDayCount
        FbPopDialog(onDismiss = { showCancelConfirmation = false }) {
            Text(
                if (completedDays > 0) "Kur nach Tag $completedDays beenden?" else "Vollständig zurücksetzen?",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (completedDays > 0) {
                    if (completedDays == 1) {
                        "Der vollständige Tag bleibt als Kur im Verlauf gespeichert. Der offene Folgetag und seine Alarme werden entfernt."
                    } else {
                        "Die $completedDays vollständigen Tage bleiben als Kur im Verlauf gespeichert. Der offene Folgetag und seine Alarme werden entfernt."
                    }
                } else {
                    "Der aktive Ablauf, alle zugehörigen Schritte, Alarme und der Eintrag im Verlauf werden vollständig gelöscht."
                },
                modifier = Modifier.padding(top = 10.dp),
                color = colors.sub,
                fontSize = 13.5.sp,
                lineHeight = 20.925.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DesignOutlineButton("Behalten", { showCancelConfirmation = false }, Modifier.weight(1f), verticalPadding = 13.dp)
                FlatDesignButton(
                    text = if (completedDays > 0) "Als Kur speichern" else "Löschen",
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

/** Der Fisetin-Drink wird immer mit Wasser und Olivenöl zubereitet. */
internal const val DRINK_LIQUID = "Wasser"
internal const val DRINK_FAT = "Olivenöl"

internal fun todaySectionTitle(cure: CureEntity?, today: LocalDate = LocalDate.now()): String {
    if (cure?.status != CureStatus.GEPLANT) return "Heute"
    val startDate = LocalDate.ofEpochDay(cure.startEpochDay)
    if (startDate == today) return "Heute"
    val startMinute = cure.preferredStartMinuteOfDay.coerceIn(0, 1_439)
    val startTime = "%02d:%02d".format(startMinute / 60, startMinute % 60)
    return "Geplante Kur am ${startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} um $startTime Uhr"
}

@Composable
private fun TodayHero(state: MainUiState) {
    val colors = LocalAppColors.current
    val active = state.activeCure != null
    val day = state.currentDay
    val cure = state.activeCure?.cure
    val drinkAt = state.plannedDrinkAt
    val preStart = state.prePhaseStart
    val drinkDue = day?.t0 == null && drinkAt != null && state.now >= drinkAt
    val inPrePhase = day?.t0 == null && state.prePhaseActive && !drinkDue
    val (title, text) = when {
        !active -> "Keine Kur aktiv" to "Lege die Kur in einen Freiblock: nüchterner Morgen, Mahlzeiten und das 4-Stunden-Fenster brauchen einen freien Tag."
        drinkDue -> "Zeit für den Fisetin-Drink" to "Geplant war ${formatTime(drinkAt!!)}. Nüchtern trinken und T0 direkt danach bestätigen."
        inPrePhase && drinkAt != null -> "Vorstufe läuft" to "Drink um ${formatTime(drinkAt)} · noch ${formatCountdown(drinkAt, state.now)}. Ab jetzt keine starken Antioxidantien mehr – der Stack zeigt, was tabu ist."
        day?.t0 == null && preStart != null && drinkAt != null -> "Kur geplant" to "Vorstufe ab ${formatDayTime(preStart, state.now)}, Drink ${formatDayTime(drinkAt, state.now)}. Bis zur Vorstufe nimmst du alles wie gewohnt."
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
                drinkDue -> HeroIcon(colors.accentSoft, colors.accent, Icons.Outlined.WaterDrop, pulse = true)
                inPrePhase -> HeroIcon(colors.warnBg, colors.warn, Icons.Outlined.Schedule, pulse = true)
                active && day?.t0 == null && preStart != null -> HeroIcon(colors.accentSoft, colors.accent, Icons.Outlined.Schedule)
                active && day?.t0 == null -> HeroIcon(colors.accentSoft, colors.accent, Icons.Outlined.WaterDrop, pulse = true)
                active -> HeroIcon(colors.okBg, colors.ok, Icons.Outlined.Check)
                else -> HeroIcon(colors.chip, colors.sub, Icons.Outlined.Add)
            }
            Text(
                title,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp),
                color = if (inPrePhase) colors.warn else colors.text,
                fontSize = 19.sp,
                lineHeight = 26.6.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text,
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 320.dp),
                color = colors.sub,
                fontSize = 13.5.sp,
                lineHeight = 20.25.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (active && day != null && cure != null) {
                val percent = (((day.dayNumber - 1f) / cure.plannedDurationDays) * 100).roundToInt()
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
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
            .clickable(onClick = onGoTimeline)
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
    preBlockMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, LocalDate, Int) -> Unit,
) {
    var duration by rememberSaveable { mutableIntStateOf(defaultDuration.coerceIn(1, 3)) }
    var startEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var startMinute by rememberSaveable { mutableIntStateOf(defaultStartMinute.coerceIn(0, 1_425)) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val startDate = startEpochDay?.let(LocalDate::ofEpochDay)
    val now = System.currentTimeMillis()
    val plannedStart = startDate?.let { plannedStartAt(it, startMinute) }
    val validStart = plannedStart?.let { it > now } == true

    fun selectDate(selected: LocalDate) {
        startEpochDay = selected.toEpochDay()
        if (selected == LocalDate.now()) {
            val current = LocalTime.now()
            val nowMinute = current.hour * 60 + current.minute
            if (startMinute <= nowMinute) startMinute = ((nowMinute / 15 + 1) * 15).coerceAtMost(1_425)
        }
    }

    FbBottomSheet(
        onDismiss = onDismiss,
        footer = {
            DesignOutlineButton("Abbrechen", onDismiss, Modifier.weight(1f), verticalPadding = 14.dp)
            AccentGradientButton(
                onClick = {
                    val selectedDate = startDate
                    if (selectedDate != null && plannedStartAt(selectedDate, startMinute) > System.currentTimeMillis()) {
                        onConfirm(duration, selectedDate, startMinute)
                    }
                },
                enabled = validStart,
                modifier = Modifier.weight(1.4f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(14.dp),
            ) { Text("Kur anlegen", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        },
    ) {
        Text("Kur einrichten", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        SheetLabel("Dauer")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { value ->
                SelectionButton(dayLabel(value), duration == value, { duration = value }, Modifier.weight(1f))
            }
        }
        SheetLabel("Drink")
        Text(
            "Fisetin-Drink immer mit lauwarmem Wasser und Olivenöl.",
            color = colors.sub,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        SheetLabel("Kurtag")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionButton("Heute", startDate == today, { selectDate(today) }, Modifier.weight(1f))
            SelectionButton("Morgen", startDate == tomorrow, { selectDate(tomorrow) }, Modifier.weight(1f))
            val otherDate = startDate?.takeIf { it != today && it != tomorrow }
            SelectionButton(
                otherDate?.format(DateTimeFormatter.ofPattern("dd.MM.")) ?: "Datum …",
                otherDate != null,
                { showDatePicker = true },
                Modifier.weight(1f),
            )
        }
        if (startDate != null) {
            SheetLabel("Uhrzeit für den Drink")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SquareControl("−1 h") { startMinute = (startMinute - 60).coerceAtLeast(0) }
                SquareControl("−15") { startMinute = (startMinute - 15).coerceAtLeast(0) }
                Text(
                    "%02d:%02d".format(startMinute / 60, startMinute % 60),
                    modifier = Modifier.weight(1f),
                    color = colors.text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                SquareControl("+15") { startMinute = (startMinute + 15).coerceAtMost(1_425) }
                SquareControl("+1 h") { startMinute = (startMinute + 60).coerceAtMost(1_425) }
            }
            if (validStart && plannedStart != null) {
                val preStart = plannedStart - preBlockMinutes * 60_000L
                StatusNotice(
                    title = if (preStart <= now) "Vorstufe beginnt sofort" else "Vorstufe ab ${formatDayTime(preStart, now)}",
                    text = "Ab dann kein Vitamin C, Vitamin E, Curcumin, Q10 oder Astaxanthin mehr. Am Kurmorgen nüchtern bleiben, nur Wasser. Drink ${formatDayTime(plannedStart, now)}.",
                    containerColor = colors.warnBg,
                    contentColor = colors.warn,
                    icon = { Icon(Icons.Outlined.Schedule, null, tint = colors.warn, modifier = Modifier.size(20.dp)) },
                )
            } else {
                Text("Die Uhrzeit muss in der Zukunft liegen.", color = colors.bad, fontSize = 12.5.sp, lineHeight = 18.75.sp)
            }
        } else {
            Text("Wähle den Kurtag – danach die Uhrzeit für den Drink.", color = colors.sub, fontSize = 12.5.sp, lineHeight = 18.75.sp)
        }
        Text("T0 wird erst gesetzt, wenn du „Drink getrunken – jetzt“ bestätigst.", color = colors.sub, fontSize = 12.5.sp, lineHeight = 18.75.sp)
    }

    if (showDatePicker) {
        val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val calendarSurface = colors.bg0
        val calendarColorScheme = MaterialTheme.colorScheme.copy(
            background = calendarSurface,
            surface = calendarSurface,
            surfaceContainer = calendarSurface,
            surfaceContainerHigh = calendarSurface,
        )
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialDisplayedMonthMillis = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: todayUtcMillis,
            selectableDates = remember(todayUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMillis
                }
            },
        )
        MaterialTheme(colorScheme = calendarColorScheme) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        enabled = datePickerState.selectedDateMillis != null,
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                                showDatePicker = false
                            }
                        },
                    ) { Text("Übernehmen") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
                },
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = calendarSurface,
                        titleContentColor = colors.sub,
                        headlineContentColor = colors.text,
                        weekdayContentColor = colors.sub,
                        subheadContentColor = colors.text,
                        navigationContentColor = colors.text,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DrinkSheet(
    ingredients: List<IngredientEntity>,
    onDismiss: () -> Unit,
    onDrinkNow: () -> Unit,
) {
    val colors = LocalAppColors.current
    val checked = remember(ingredients) { mutableStateMapOf<Long, Boolean>().apply { ingredients.forEach { put(it.id, false) } } }
    val complete = ingredients.isNotEmpty() && ingredients.all { checked[it.id] == true }
    FbBottomSheet(
        onDismiss = onDismiss,
        footer = {
            DesignOutlineButton("Abbrechen", onDismiss, Modifier.weight(1f), verticalPadding = 14.dp)
            AccentGradientButton(
                onClick = onDrinkNow,
                enabled = complete,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(14.dp),
            ) { Text("Drink getrunken – jetzt", fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        },
    ) {
        Text("Drink-Checkliste", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Mit Wasser und Olivenöl · alles abhaken, dann trinken.", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        ingredients.forEach { ingredient ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.chip)
                    .clickable { checked[ingredient.id] = checked[ingredient.id] != true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesignCheckbox(checked[ingredient.id] == true) { checked[ingredient.id] = it }
                Column(Modifier.weight(1f)) {
                    Text(ingredient.name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(ingredient.amount, ingredient.note).filter { it.isNotBlank() }.joinToString(" · "),
                        color = colors.sub,
                        fontSize = 12.sp,
                        lineHeight = 16.8.sp,
                    )
                }
            }
        }
        Text(
            if (complete) "Alles intensiv mixen oder shaken und sofort trinken." else "Hake alle Zutaten ab, dann kannst du T0 setzen.",
            modifier = Modifier.fillMaxWidth(),
            color = if (complete) colors.text else colors.sub,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private enum class StepState { DONE, ACTIVE, OPEN }

@Composable
fun TimelineScreen(
    state: MainUiState,
    onDrinkNow: () -> Unit,
    onMealDone: () -> Unit,
    onSpermidinDone: () -> Unit,
    onCompleteDay: () -> Unit,
    onFinishCureAfterDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val day = state.currentDay
    val cure = state.activeCure?.cure
    var showCompletionChoice by rememberSaveable(day?.id) { mutableStateOf(false) }
    var showDrink by rememberSaveable { mutableStateOf(false) }
    val hardItems = remember(state.stackItems) {
        state.stackItems.filter { it.category == StackCategory.HARTE_SPERRE }.map { it.name.substringBefore(" (") }
    }
    val drinkItems = state.ingredients.filter { it.phase == IngredientPhase.DRINK }.map { "${it.name} · ${it.amount}" }
    val spermidinItems = state.ingredients.filter { it.phase == IngredientPhase.SPERMIDIN }.map { "${it.name} · ${it.amount}" }
    val prePhaseNote = "Keine starken Antioxidantien. Am Kurmorgen nüchtern bleiben, nur Wasser. Medikamente wie gewohnt."
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = LocalScreenBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle(
                "Tagesablauf",
                if (day?.t0 == null) "Geplante Zeiten – nach dem Drink richtet sich alles nach T0." else "Alle Uhrzeiten werden ab T0 berechnet.",
            )
        }
        if (cure == null || day == null) {
            item {
                GlassCard(shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(18.dp)) {
                    Text("Keine Kur geplant", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Lege auf dem Heute-Bildschirm eine neue Kur an. Danach siehst du hier den ganzen Ablauf – von der Vorstufe bis zum Ende des Sperrfensters.",
                        modifier = Modifier.padding(top = 3.dp),
                        color = colors.sub,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
        } else if (day.t0 == null) {
            val drinkAt = state.plannedDrinkAt ?: plannedT0(cure, day)
            val preStart = prePhaseStartAt(drinkAt, state.protocol)
            val planned = timeline(drinkAt, state.protocol)
            val preActive = state.now >= preStart
            item {
                Column {
                    TimelineStep(
                        title = "Vorstufe",
                        time = if (preActive) "läuft seit ${formatDayTime(preStart, state.now)}" else "ab ${formatDayTime(preStart, state.now)}",
                        state = if (preActive) StepState.ACTIVE else StepState.OPEN,
                        note = prePhaseNote,
                        ingredients = hardItems.map { "kein $it" },
                        warningChips = true,
                    )
                    TimelineStep(
                        title = "Fisetin-Drink · T0",
                        time = "geplant ${formatDayTime(drinkAt, state.now)}",
                        state = if (state.now >= drinkAt) StepState.ACTIVE else StepState.OPEN,
                        actionLabel = "Checkliste",
                        action = { showDrink = true },
                        ingredients = drinkItems,
                    )
                    TimelineStep("Fettige Mahlzeit", "ca. bis ${formatTime(planned.mealDeadline)}", StepState.OPEN)
                    TimelineStep("Spermidin + leichte Mahlzeit", "ca. ${clock(planned.spermidinStart)}–${formatTime(planned.spermidinEnd)}", StepState.OPEN, ingredients = spermidinItems)
                    TimelineStep("Sperrfenster vorbei", "ca. ${formatTime(planned.blockEnd)}", StepState.OPEN, last = true)
                }
            }
        } else {
            val times = timeline(day.t0, state.protocol)
            val mealDone = day.mealDoneAt != null
            val spermidinDone = day.spermidinDoneAt != null
            item {
                Column {
                    TimelineStep("Vorstufe", "erledigt", StepState.DONE)
                    TimelineStep("Fisetin-Drink · T0", "${formatTime(times.t0)} · T0 gesetzt", StepState.DONE, ingredients = drinkItems)
                    TimelineStep(
                        title = "Fettige Mahlzeit",
                        time = "bis ${formatTime(times.mealDeadline)}",
                        state = when {
                            mealDone -> StepState.DONE
                            state.now < times.mealDeadline -> StepState.ACTIVE
                            else -> StepState.OPEN
                        },
                        action = if (mealDone) null else onMealDone,
                    )
                    TimelineStep(
                        title = "Spermidin + leichte Mahlzeit",
                        time = "${clock(times.spermidinStart)}–${formatTime(times.spermidinEnd)}",
                        state = when {
                            spermidinDone -> StepState.DONE
                            state.now >= times.spermidinStart -> StepState.ACTIVE
                            else -> StepState.OPEN
                        },
                        action = if (spermidinDone) null else onSpermidinDone,
                        ingredients = spermidinItems,
                    )
                    TimelineStep(
                        title = "Sperrfenster vorbei",
                        time = formatTime(times.blockEnd),
                        state = if (state.blockActive) StepState.ACTIVE else StepState.DONE,
                        last = true,
                    )
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
                val hasAnotherDay = day.dayNumber < cure.plannedDurationDays
                AccentGradientButton(
                    onClick = { if (hasAnotherDay) showCompletionChoice = true else onCompleteDay() },
                    enabled = state.canCompleteDay,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (day.dayNumber == cure.plannedDurationDays) "Kur abschließen" else "Tag ${day.dayNumber} abschließen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

    if (showDrink) {
        DrinkSheet(
            ingredients = state.ingredients.filter { it.phase == IngredientPhase.DRINK },
            onDismiss = { showDrink = false },
            onDrinkNow = {
                onDrinkNow()
                showDrink = false
            },
        )
    }
    if (showCompletionChoice && day != null) {
        FbPopDialog(onDismiss = { showCompletionChoice = false }) {
            Text("Tag ${day.dayNumber} vollständig", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Möchtest du die Kur jetzt nach ${dayLabel(day.dayNumber)} abschließen oder mit Tag ${day.dayNumber + 1} fortfahren?",
                modifier = Modifier.padding(top = 10.dp),
                color = colors.sub,
                fontSize = 13.5.sp,
                lineHeight = 20.925.sp,
            )
            AccentGradientButton(
                onClick = {
                    onCompleteDay()
                    showCompletionChoice = false
                },
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            ) {
                Text("Weiter mit Tag ${day.dayNumber + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            DesignOutlineButton(
                text = "Kur nach ${dayLabel(day.dayNumber)} beenden",
                onClick = {
                    onFinishCureAfterDay()
                    showCompletionChoice = false
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalPadding = 13.dp,
            )
        }
    }
}

private fun dayLabel(days: Int): String = if (days == 1) "1 Tag" else "$days Tage"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineStep(
    title: String,
    time: String,
    state: StepState,
    actionLabel: String = "Erledigt",
    action: (() -> Unit)? = null,
    ingredients: List<String> = emptyList(),
    last: Boolean = false,
    note: String? = null,
    warningChips: Boolean = false,
) {
    val colors = LocalAppColors.current
    val lineColor = colors.line
    val (circleBackground, circleBorder, iconTint) = when (state) {
        StepState.DONE -> Triple(colors.okBg, colors.line, colors.ok)
        StepState.ACTIVE -> Triple(colors.accentSoft, colors.accent, colors.accent)
        StepState.OPEN -> Triple(colors.chip, colors.line, colors.sub)
    }
    // Verbindungslinie wird gezeichnet statt per IntrinsicSize gemessen: FlowRow-Inhalte
    // wurden mit Intrinsic-Messung abgeschnitten.
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                if (!last) {
                    val x = 18.dp.toPx()
                    drawLine(lineColor, Offset(x, 36.dp.toPx()), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                }
            }
            .padding(bottom = if (last) 0.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(circleBackground).border(if (state == StepState.ACTIVE) 2.dp else 1.dp, circleBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when (state) {
                StepState.DONE -> Icons.Outlined.Check
                StepState.ACTIVE -> Icons.Outlined.Schedule
                StepState.OPEN -> Icons.Outlined.RadioButtonUnchecked
            }
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        GlassCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = colors.text, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        time,
                        color = if (state == StepState.ACTIVE) colors.accent else colors.sub,
                        fontSize = 12.5.sp,
                        fontWeight = if (state == StepState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                action?.let { SmallActionButton(actionLabel, it) }
            }
            note?.let {
                Text(it, modifier = Modifier.padding(top = 6.dp), color = colors.sub, fontSize = 12.5.sp, lineHeight = 18.sp)
            }
            if (ingredients.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ingredients.forEach { IngredientChip(it, warning = warningChips) }
                }
            }
        }
    }
}

@Composable
private fun IngredientChip(text: String, warning: Boolean = false) {
    val colors = LocalAppColors.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (warning) colors.badBg else colors.chip)
            .border(1.dp, if (warning) colors.badBg else colors.line, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        color = if (warning) colors.bad else colors.sub,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun clock(timestamp: Long): String = formatTime(timestamp).removeSuffix(" Uhr")

private val SheetScrim = Color(0x73080614)

/**
 * Eigenes Bottom-Sheet: randlos im Dialog-Fenster, Inhalt scrollt, die Aktionsleiste
 * bleibt fest über Navigationsleiste und Tastatur sichtbar.
 */
@Composable
internal fun FbBottomSheet(
    onDismiss: () -> Unit,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(SheetScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 24.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val sheetMaxHeight = maxHeight
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, easing = FbMotionEasing)) + slideInVertically(tween(300, easing = FbMotionEasing)) { it / 3 },
            ) {
                val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                Column(
                    Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                        .clip(shape)
                        .background(colors.card)
                        .border(BorderStroke(1.dp, colors.cardBorder), shape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
                ) {
                    Box(
                        Modifier
                            .padding(top = 12.dp, bottom = 4.dp)
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(colors.line),
                    )
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = if (footer == null) 24.dp else 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content,
                    )
                    if (footer != null) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = footer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FbPopDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(SheetScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, easing = FbMotionEasing)) + scaleIn(
                    animationSpec = tween(300, easing = FbMotionEasing),
                    initialScale = 0.94f,
                ),
            ) {
                GlassCard(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
                        content = content,
                    )
                }
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
internal fun SmallActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(12.dp)).background(colors.accentSoft).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.accent.copy(alpha = if (enabled) 1f else 0.45f), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
        Text(text, color = if (selected) colors.accent else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
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
        Modifier.size(width = 48.dp, height = 46.dp).clip(RoundedCornerShape(14.dp)).background(colors.chip).border(1.dp, colors.line, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
