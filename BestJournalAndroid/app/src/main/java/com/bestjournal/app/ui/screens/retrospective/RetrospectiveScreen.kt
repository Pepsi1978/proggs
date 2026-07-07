package com.bestjournal.app.ui.screens.retrospective

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bestjournal.app.R
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.PrivacyGateHost
import com.bestjournal.app.ui.components.PrivacyGateState
import com.bestjournal.app.ui.components.SunMoonToggle
import com.bestjournal.app.ui.components.TwinklingStars
import com.bestjournal.app.ui.components.rememberPrivacyGateState
import com.bestjournal.app.ui.theme.FeatureAccentOrange
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.util.DateTimeFormatter as AppDateTimeFormatter
import com.bestjournal.app.util.EdgeTtsPlayer
import com.bestjournal.app.util.PrivacyGateHelper
import com.bestjournal.app.util.rememberHapticAction
import java.util.Calendar

object RetrospectiveColors {
    // Theme-aware: Card-Hintergrund kommt aus MaterialTheme.colorScheme.surface,
    // dadurch tragen die Karten denselben Profil-Hauch wie der Rest der App
    // (siehe profileColorScheme in Theme.kt). 1:1 aus BestJournalFrank.
    private val cardDark: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    private val cardLight: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val weekColors: List<Color>
        @Composable
        get() {
            val c = if (LocalIsDarkTheme.current) cardDark else cardLight
            return List(4) { c }
        }

    val monthDividerColor: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    // Header-Gradient bekommt die Profil-Akzentfarbe oben und blendet dezent in
    // den App-Hintergrund unten — dadurch faerbt sich der Rueckblick bei jedem
    // Profil-Wechsel mit um.
    val headerGradient: List<Color>
        @Composable
        get() {
            val accent = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surface
            val background = MaterialTheme.colorScheme.background
            return if (LocalIsDarkTheme.current) {
                listOf(
                    accent.copy(alpha = 0.35f).compositeOver(background),
                    accent.copy(alpha = 0.22f).compositeOver(background),
                    accent.copy(alpha = 0.10f).compositeOver(background),
                    surface,
                )
            } else {
                listOf(
                    accent.copy(alpha = 0.25f).compositeOver(Color.White),
                    accent.copy(alpha = 0.15f).compositeOver(Color.White),
                    accent.copy(alpha = 0.06f).compositeOver(Color.White),
                    surface,
                )
            }
        }

    val categoryCardColor: Color
        @Composable get() = if (LocalIsDarkTheme.current) cardDark else cardLight

    val categoryIconCircle: Color
        @Composable
        get() =
            MaterialTheme.colorScheme.primary
                .copy(alpha = if (LocalIsDarkTheme.current) 0.18f else 0.14f)
                .compositeOver(MaterialTheme.colorScheme.surface)

    val categoryButtonGradient: List<Color>
        @Composable
        get() {
            val accent = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surface
            return if (LocalIsDarkTheme.current) {
                listOf(accent.copy(alpha = 0.22f).compositeOver(surface), surface)
            } else {
                listOf(accent.copy(alpha = 0.12f).compositeOver(Color.White), Color.White)
            }
        }

    val monthColors: List<Color>
        @Composable get() = List(12) { if (LocalIsDarkTheme.current) cardDark else cardLight }

    val yearColor: Color
        @Composable get() = if (LocalIsDarkTheme.current) cardDark else cardLight

    @Composable fun forWeek(weekOfMonth: Int): Color = weekColors[(weekOfMonth - 1).coerceIn(0, 3)]

    @Composable fun forMonth(month: Int): Color = monthColors[(month - 1).coerceIn(0, 11)]
}

@Composable
fun RetrospectiveScreen(
    viewModel: RetrospectiveViewModel,
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val doHaptic = rememberHapticAction()
    val weekly by viewModel.weeklySummaries.collectAsStateWithLifecycle()
    val monthly by viewModel.monthlySummaries.collectAsStateWithLifecycle()
    val yearly by viewModel.yearlySummaries.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isWaitingForRestore by viewModel.isWaitingForRestore.collectAsStateWithLifecycle()
    val isProfileSwitch by viewModel.isProfileSwitch.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val lockedWeeks by viewModel.lockedWeeks.collectAsStateWithLifecycle()
    val subState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val isPremium = subState is SubscriptionState.Subscribed

    // NK1: Per-service consent gates (EDSA 03/2023, BGH Planet49).
    val geminiGate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.Gemini)
    val edgeTtsGate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.EdgeTts)
    PrivacyGateHost(
        state = geminiGate,
        titleRes = R.string.privacy_gate_gemini_title,
        bodyRes = R.string.privacy_gate_gemini_body,
        acceptRes = R.string.privacy_gate_gemini_accept,
        declineRes = R.string.privacy_gate_gemini_cancel,
    )
    PrivacyGateHost(
        state = edgeTtsGate,
        titleRes = R.string.privacy_gate_tts_title,
        bodyRes = R.string.privacy_gate_tts_body,
        acceptRes = R.string.privacy_gate_tts_accept,
        declineRes = R.string.privacy_gate_tts_cancel,
    )

    // Check profile-change flag on every tab entry — triggers regeneration if user
    // switched dashboard scenario since last visit.
    LaunchedEffect(Unit) { geminiGate.run { viewModel.checkProfileChangeAndRegenerate() } }

    var selectedSummary by remember { mutableStateOf<RetrospectiveSummaryEntity?>(null) }
    var weeklyExpanded by rememberSaveable { mutableStateOf(false) }
    var monthlyExpanded by rememberSaveable { mutableStateOf(false) }
    var yearlyExpanded by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPremiumSheet by remember { mutableStateOf(false) }

    // Info dialog about review benefits
    if (showInfoDialog) {
        ReviewBenefitsDialog(onDismiss = { showInfoDialog = false })
    }

    // Review-specific premium upsell sheet
    if (showPremiumSheet) {
        ReviewPremiumSheet(
            onSubscribe = {
                showPremiumSheet = false
                onNavigateToPaywall("review_locked")
            },
            onDismiss = { showPremiumSheet = false },
        )
    }

    selectedSummary?.let { summary ->
        SummaryDetailDialog(
            summary = summary,
            viewModel = viewModel,
            edgeTtsGate = edgeTtsGate,
            onDismiss = { selectedSummary = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (LocalIsDarkTheme.current) {
            ParticleBackground()
            TwinklingStars()
        }
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed title bar (does not scroll) — same top alignment as Dashboard/Journal/Settings
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.retro_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SunMoonToggle()
                    }
                    IconButton(
                        onClick = {
                            doHaptic(HapticFeedbackType.LongPress)
                            showInfoDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.retro_cd_info),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Last-updated timestamp (same format + string resource as Dashboard)
                val lastUpdated =
                    remember(weekly, monthly, yearly) { viewModel.getLastUpdatedText() }
                if (lastUpdated != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = lastUpdated,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // AI Act Art. 50 inline marker — right-aligned with 8.dp (~2mm) offset
                        // from the edge; longer translations grow leftwards while the trailing
                        // edge stays fixed.
                        com.bestjournal.app.ui.components.AiGeneratedBadgeInline(
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            // Scrollable content
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(colors = RetrospectiveColors.headerGradient)
                            )
                            .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = RetrospectiveColors.monthDividerColor,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.retro_personal_review),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.retro_intro_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.retro_intro_cta),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = RetrospectiveColors.monthDividerColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (isWaitingForRestore || isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        com.bestjournal.app.ui.components.ShimmerLoadingEffect(
                            height = 80.dp,
                            cornerRadius = 16.dp,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Text(
                                stringResource(R.string.retro_please_wait),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                when {
                                    isWaitingForRestore ->
                                        stringResource(R.string.retro_backup_loading)
                                    isProfileSwitch ->
                                        stringResource(R.string.retro_profile_switching)
                                    else -> stringResource(R.string.retro_generating)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (errorMessage != null && !isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.retro_ai_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.Button(
                                onClick = { geminiGate.run { viewModel.retryGeneration() } },
                                colors =
                                    androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = RetrospectiveColors.monthDividerColor
                                    ),
                            ) {
                                Text(stringResource(R.string.action_retry))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text(
                                    stringResource(R.string.action_later),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Wochenrückblick button + expandable entries
                CategoryButton(
                    title = stringResource(R.string.retro_weekly_title),
                    subtitle = stringResource(R.string.retro_weekly_subtitle),
                    icon = Icons.Rounded.CalendarToday,
                    expanded = weeklyExpanded,
                    onClick = { weeklyExpanded = !weeklyExpanded },
                )
                AnimatedVisibility(
                    visible = weeklyExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (weekly.isEmpty() && lockedWeeks.isEmpty()) {
                            EmptyHint(stringResource(R.string.retro_weekly_empty))
                        }

                        // Free weekly reviews
                        if (weekly.isNotEmpty()) {
                            // Wochen werden chronologisch nach (Monat, Jahr) gruppiert.
                            // Pro Monatsgruppe wird eine eigene ContinuousTimelineSection
                            // gerendert, sodass die Linie nicht ueber Monatsgrenzen
                            // hinausgeht — die Linie deckt z.B. nur die 4 April-Wochen ab,
                            // nicht die letzte Maerz-Woche dazu.
                            val weeklyGroups = groupSummariesByMonth(weekly)
                            weeklyGroups.forEachIndexed { groupIndex, group ->
                                if (groupIndex > 0) {
                                    MonthDivider(
                                        label =
                                            AppDateTimeFormatter.formatMonthYear(
                                                group.first().endDate
                                            )
                                    )
                                }
                                ContinuousTimelineSection(
                                    entryCount = group.size,
                                    lineColor =
                                        RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f),
                                    dotColor = RetrospectiveColors.monthDividerColor,
                                ) {
                                    group.forEachIndexed { index, summary ->
                                        if (index > 0) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                        SummaryEntryCard(
                                            summary = summary,
                                            color =
                                                RetrospectiveColors.forWeek(summary.periodIndex),
                                            onClick = { selectedSummary = summary },
                                        )
                                    }
                                }
                            }
                        }

                        // Locked week placeholders (premium-gated) — shown even without free
                        // reviews
                        if (lockedWeeks.isNotEmpty()) {
                            lockedWeeks.forEachIndexed { index, locked ->
                                Spacer(modifier = Modifier.height(10.dp))
                                LockedWeekEntry(
                                    periodLabel = locked.periodLabel,
                                    isLast = index == lockedWeeks.lastIndex,
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        showPremiumSheet = true
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.retro_premium_unlock),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            doHaptic(HapticFeedbackType.LongPress)
                                            showPremiumSheet = true
                                        }
                                        .padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monatsrückblick button + expandable entries
                CategoryButton(
                    title = stringResource(R.string.retro_monthly_title),
                    subtitle = stringResource(R.string.retro_monthly_subtitle),
                    icon = Icons.Rounded.DateRange,
                    expanded = if (isPremium) monthlyExpanded else false,
                    premiumBadge = !isPremium,
                    onClick = {
                        if (isPremium) {
                            monthlyExpanded = !monthlyExpanded
                        } else {
                            doHaptic(HapticFeedbackType.LongPress)
                            showPremiumSheet = true
                        }
                    },
                )
                AnimatedVisibility(
                    visible = monthlyExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (monthly.isEmpty()) {
                            EmptyHint(stringResource(R.string.retro_monthly_empty))
                        } else {
                            // Monate werden chronologisch nach Jahr gruppiert: Eine Linie
                            // laeuft von Januar bis Dezember eines Jahres durch — beim
                            // Jahreswechsel beginnt eine neue Section. Quartalstrenner
                            // bleiben als sichtbare Beschriftung INNERHALB der Section,
                            // die Linie laeuft durch sie hindurch.
                            val monthlyGroups = groupSummariesByYear(monthly)
                            monthlyGroups.forEachIndexed { groupIndex, group ->
                                if (groupIndex > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                ContinuousTimelineSection(
                                    entryCount = group.size,
                                    lineColor =
                                        RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f),
                                    dotColor = RetrospectiveColors.monthDividerColor,
                                ) {
                                    group.forEachIndexed { index, summary ->
                                        if (index > 0) {
                                            val prevQuarter = (group[index - 1].periodIndex - 1) / 3
                                            val curQuarter = (summary.periodIndex - 1) / 3
                                            if (prevQuarter != curQuarter) {
                                                MonthDivider(
                                                    label =
                                                        AppDateTimeFormatter.formatQuarterYear(
                                                            summary.startDate
                                                        )
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }
                                        }
                                        SummaryEntryCard(
                                            summary = summary,
                                            color =
                                                RetrospectiveColors.forMonth(summary.periodIndex),
                                            onClick = { selectedSummary = summary },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Jahresrückblick button + expandable entries
                CategoryButton(
                    title = stringResource(R.string.retro_yearly_title),
                    subtitle = stringResource(R.string.retro_yearly_subtitle),
                    icon = Icons.Rounded.CalendarMonth,
                    expanded = if (isPremium) yearlyExpanded else false,
                    premiumBadge = !isPremium,
                    onClick = {
                        if (isPremium) {
                            yearlyExpanded = !yearlyExpanded
                        } else {
                            doHaptic(HapticFeedbackType.LongPress)
                            showPremiumSheet = true
                        }
                    },
                )
                AnimatedVisibility(
                    visible = yearlyExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (yearly.isEmpty()) {
                            EmptyHint(stringResource(R.string.retro_yearly_empty))
                        } else {
                            ContinuousTimelineSection(
                                entryCount = yearly.size,
                                lineColor =
                                    RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f),
                                dotColor = RetrospectiveColors.monthDividerColor,
                            ) {
                                yearly.forEachIndexed { index, summary ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    SummaryEntryCard(
                                        summary = summary,
                                        color = RetrospectiveColors.yearColor,
                                        onClick = { selectedSummary = summary },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                com.bestjournal.app.ui.components.AiOutputDisclaimer()
            }
        }
    }
}

/**
 * Gruppiert eine chronologisch sortierte Liste von Wochen-Summaries konsekutiv nach (Monat, Jahr).
 * Aufeinanderfolgende Eintraege im selben Monat landen in derselben Untergruppe — sobald der Monat
 * wechselt, beginnt eine neue Gruppe. Wir gehen ueber [endDate] (Sonntag der Woche), damit
 * Cross-Month-Wochen (z.B. 30. Maerz – 5. April) unter dem spaeteren Monat erscheinen.
 */
private fun groupSummariesByMonth(
    items: List<RetrospectiveSummaryEntity>
): List<List<RetrospectiveSummaryEntity>> {
    val result = mutableListOf<MutableList<RetrospectiveSummaryEntity>>()
    items.forEach { summary ->
        val cal = Calendar.getInstance().apply { timeInMillis = summary.endDate }
        val curKey = cal.get(Calendar.MONTH) to cal.get(Calendar.YEAR)
        val lastEntry = result.lastOrNull()?.lastOrNull()
        val sameGroup =
            if (lastEntry == null) false
            else {
                val lastCal = Calendar.getInstance().apply { timeInMillis = lastEntry.endDate }
                val lastKey = lastCal.get(Calendar.MONTH) to lastCal.get(Calendar.YEAR)
                lastKey == curKey
            }
        if (sameGroup) {
            result.last().add(summary)
        } else {
            result.add(mutableListOf(summary))
        }
    }
    return result
}

/**
 * Gruppiert eine chronologisch sortierte Liste von Monats-/Jahres-Summaries konsekutiv nach Jahr
 * (basierend auf [startDate]). Aufeinanderfolgende Eintraege im selben Jahr landen in derselben
 * Untergruppe.
 */
private fun groupSummariesByYear(
    items: List<RetrospectiveSummaryEntity>
): List<List<RetrospectiveSummaryEntity>> {
    val result = mutableListOf<MutableList<RetrospectiveSummaryEntity>>()
    items.forEach { summary ->
        val cal = Calendar.getInstance().apply { timeInMillis = summary.startDate }
        val curYear = cal.get(Calendar.YEAR)
        val lastEntry = result.lastOrNull()?.lastOrNull()
        val sameGroup =
            if (lastEntry == null) false
            else {
                val lastCal = Calendar.getInstance().apply { timeInMillis = lastEntry.startDate }
                lastCal.get(Calendar.YEAR) == curYear
            }
        if (sameGroup) {
            result.last().add(summary)
        } else {
            result.add(mutableListOf(summary))
        }
    }
    return result
}

/**
 * Wickelt eine Liste von Eintragskarten in eine durchgehende Timeline-Rail.
 *
 * Die Rail (links, 24dp breit) zeichnet eine durchgehende vertikale Linie von 10 % bis 90 % der
 * Section-Hoehe und N gleichmaessig verteilte Punkte:
 * - N=1: ein Punkt mittig (50 %)
 * - N=2: 10 % und 90 %
 * - N=3: 10 %, 50 %, 90 %
 * - N=4: 10 %, 36,7 %, 63,3 %, 90 %
 * - allgemein: pos_i = 10 % + i * 80 % / (N-1)
 *
 * Die Karten + Spacer/Divider werden als [content] uebergeben und liegen rechts neben der Rail in
 * einer Column. Damit ist die Linie nicht mehr unterbrochen, auch wenn zwischen den Karten Spacer
 * oder MonthDivider stehen.
 */
@Composable
private fun ContinuousTimelineSection(
    entryCount: Int,
    lineColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
    railWidth: Dp = 24.dp,
    dotSize: Dp = 8.dp,
    lineThickness: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(modifier = Modifier.width(railWidth).fillMaxHeight()) {
            if (entryCount <= 0) return@Canvas
            val cx = size.width / 2f
            val topY = size.height * 0.1f
            val bottomY = size.height * 0.9f
            val strokePx = lineThickness.toPx()
            val dotRadiusPx = dotSize.toPx() / 2f
            if (entryCount > 1) {
                drawLine(
                    color = lineColor,
                    start = Offset(cx, topY),
                    end = Offset(cx, bottomY),
                    strokeWidth = strokePx,
                )
            }
            for (i in 0 until entryCount) {
                val frac = if (entryCount == 1) 0.5f else 0.1f + 0.8f * i / (entryCount - 1)
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(cx, size.height * frac),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun CategoryButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    premiumBadge: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(RetrospectiveColors.categoryButtonGradient))
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(52.dp)
                        .clip(CircleShape)
                        .background(RetrospectiveColors.categoryIconCircle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = if (isDark) Color.White else RetrospectiveColors.monthDividerColor,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                    if (premiumBadge) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = stringResource(R.string.label_premium),
                            modifier = Modifier.size(18.dp),
                            tint = FeatureAccentOrange,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isDark) Color.White.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription =
                    if (expanded) stringResource(R.string.retro_cd_collapse)
                    else stringResource(R.string.retro_cd_expand),
                tint =
                    if (isDark) Color.White.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun SummaryEntryCard(
    summary: RetrospectiveSummaryEntity,
    color: Color,
    onClick: () -> Unit,
) {
    val textColor = if (color.luminance() > 0.4f) Color.Black else Color.White
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = summary.periodLabel,
                style = MaterialTheme.typography.labelMedium,
                color = RetrospectiveColors.monthDividerColor,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonthDivider(label: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(2.5.dp)
                    .background(RetrospectiveColors.monthDividerColor)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = RetrospectiveColors.monthDividerColor,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(2.5.dp)
                    .background(RetrospectiveColors.monthDividerColor)
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    )
}

private fun Modifier.drawVerticalScrollbar(scrollState: ScrollState, color: Color): Modifier =
    drawWithContent {
        drawContent()
        val viewHeight = size.height
        val contentHeight = scrollState.maxValue.toFloat() + viewHeight
        if (contentHeight > viewHeight && scrollState.maxValue > 0) {
            val barWidth = 4.dp.toPx()
            val scrollbarHeight =
                (viewHeight * viewHeight / contentHeight).coerceIn(32.dp.toPx(), viewHeight * 0.15f)
            val scrollbarY =
                scrollState.value.toFloat() / scrollState.maxValue * (viewHeight - scrollbarHeight)
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - barWidth - 2.dp.toPx(), scrollbarY),
                size = Size(barWidth, scrollbarHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }

@Composable
private fun SummaryDetailDialog(
    summary: RetrospectiveSummaryEntity,
    viewModel: RetrospectiveViewModel,
    edgeTtsGate: PrivacyGateState,
    onDismiss: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val doHaptic = rememberHapticAction()
    var isSpeaking by remember { mutableStateOf(false) }
    var isTtsLoading by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }
    val tts = remember { EdgeTtsPlayer(context) }
    val ttsPrefs = remember {
        try {
            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }
    val photos by viewModel.currentPhotos.collectAsStateWithLifecycle()
    val parsed = remember(summary.summaryText) { parseRetrospectiveText(summary.summaryText) }

    // Load photos for this retrospective period when the dialog opens
    LaunchedEffect(summary.startDate, summary.endDate) {
        viewModel.loadPhotosForPeriod(summary.startDate, summary.endDate)
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    Dialog(
        onDismissRequest = {
            tts.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Colored header with gradient in dark mode
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .then(
                                if (isDark)
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            RetrospectiveColors.categoryButtonGradient
                                        )
                                    )
                                else Modifier.background(Color.White)
                            )
                            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 20.dp)
                ) {
                    val detailTextColor = if (isDark) Color.White else Color.Black
                    Column(modifier = Modifier.padding(end = 44.dp)) {
                        Text(
                            text = summary.periodLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = RetrospectiveColors.monthDividerColor,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = detailTextColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = detailTextColor.copy(alpha = 0.8f),
                        )
                    }
                }

                // Body — structured rendering with bullet summary + timeline sections
                val bodyScrollState = rememberScrollState()
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .drawVerticalScrollbar(
                                bodyScrollState,
                                color =
                                    if (isDark) Color.White.copy(alpha = 0.4f)
                                    else Color.Black.copy(alpha = 0.3f),
                            )
                            .verticalScroll(bodyScrollState)
                            .padding(horizontal = 20.dp)
                            .padding(top = 24.dp, bottom = 120.dp)
                ) {
                    // H4 — In-App KI-Kennzeichnung (AI Act Art. 50, Pflicht ab 02.08.2026)
                    com.bestjournal.app.ui.components.AiGeneratedBadge(
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Bullet point summary card
                    if (parsed.bulletPoints.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                                ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.retro_at_a_glance),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = RetrospectiveColors.monthDividerColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                parsed.bulletPoints.forEach { point ->
                                    Row(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Text(
                                            "\u2022 ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = RetrospectiveColors.monthDividerColor,
                                        )
                                        Text(
                                            point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Timeline sections with icons
                    if (parsed.sections.isNotEmpty()) {
                        parsed.sections.forEachIndexed { index, section ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Timeline: icon circle + vertical line
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(44.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(36.dp)
                                                .clip(CircleShape)
                                                .background(RetrospectiveColors.categoryIconCircle),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            section.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = RetrospectiveColors.monthDividerColor,
                                        )
                                    }
                                    if (index < parsed.sections.lastIndex) {
                                        Box(
                                            modifier =
                                                Modifier.width(2.dp)
                                                    .height(20.dp)
                                                    .background(
                                                        RetrospectiveColors.monthDividerColor.copy(
                                                            alpha = 0.3f
                                                        )
                                                    )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Section content
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        section.heading,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RetrospectiveColors.monthDividerColor,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        section.body,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                                    )
                                    if (index < parsed.sections.lastIndex) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback: plain text if parsing found no sections (old format)
                        Text(
                            text = summary.summaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        )
                    }

                    // Photos & Videos section — shown after timeline, before divider
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.retro_photos_videos),
                            style = MaterialTheme.typography.titleSmall,
                            color = RetrospectiveColors.monthDividerColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Performance: LazyRow with stable keys — only visible photos are composed
                        // and load their bitmaps
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(photos, key = { it.filePath }) { photo ->
                                Box {
                                    AsyncImage(
                                        model = photo.filePath,
                                        contentDescription =
                                            if (photo.isVideo)
                                                stringResource(R.string.retro_cd_video)
                                            else stringResource(R.string.retro_cd_photo),
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier.size(120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { fullScreenPhotoPath = photo.filePath },
                                    )
                                    if (photo.isVideo) {
                                        Icon(
                                            Icons.Rounded.PlayCircle,
                                            contentDescription =
                                                stringResource(R.string.retro_play_video),
                                            modifier = Modifier.size(40.dp).align(Alignment.Center),
                                            tint = Color.White.copy(alpha = 0.9f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider + action buttons
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons: Speaker + Share
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                if (isSpeaking || isTtsLoading) {
                                    tts.stop()
                                    isSpeaking = false
                                    isTtsLoading = false
                                } else {
                                    val ttsOn =
                                        ttsPrefs?.getBoolean(
                                            com.bestjournal.app.util.Constants.PREF_TTS_ENABLED,
                                            false,
                                        ) ?: false
                                    if (!ttsOn) {
                                        android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.retro_enable_voices),
                                                android.widget.Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                    } else {
                                        edgeTtsGate.run {
                                            isTtsLoading = true
                                            isSpeaking = true
                                            val speakText =
                                                if (parsed.sections.isNotEmpty())
                                                    parsed.sections.joinToString("\n\n") {
                                                        "${it.heading}.\n${it.body}"
                                                    }
                                                else summary.summaryText
                                            val voice =
                                                ttsPrefs?.getString(
                                                    com.bestjournal.app.util.Constants
                                                        .PREF_EDGE_TTS_VOICE,
                                                    com.bestjournal.app.util.TtsVoiceRegistry
                                                        .getLocaleVoices()
                                                        .defaultVoiceId,
                                                )
                                                    ?: com.bestjournal.app.util.TtsVoiceRegistry
                                                        .getLocaleVoices()
                                                        .defaultVoiceId
                                            tts.speak(
                                                speakText,
                                                voice = voice,
                                                onPlaybackStart = { isTtsLoading = false },
                                            ) {
                                                isSpeaking = false
                                                isTtsLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription =
                                    if (isSpeaking) stringResource(R.string.retro_tts_stop)
                                    else stringResource(R.string.retro_tts_read),
                                tint = FeatureAccentOrange,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                showShareDialog = true
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.retro_cd_share),
                                tint = FeatureAccentOrange,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    // TTS loading indicator
                    if (isTtsLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color =
                                    if (LocalIsDarkTheme.current) Color(0xFF5C7AA3)
                                    else Color(0xFF1976D2),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.retro_tts_wait),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    // Share dialog with checkboxes — like diary entry sharing
    if (showShareDialog) {
        var shareText by remember { mutableStateOf(true) }
        val selectedPhotos = remember { List(photos.size) { true }.toMutableStateList() }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                Text(
                    stringResource(R.string.retro_share_title),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Text checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { shareText = !shareText },
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = shareText,
                            onCheckedChange = { shareText = it },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.retro_share_text_content),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Photo/Video checkboxes
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.retro_photos_videos_colon),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        photos.forEachIndexed { index, photo ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.fillMaxWidth().clickable {
                                        selectedPhotos[index] = !selectedPhotos[index]
                                    },
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = selectedPhotos[index],
                                    onCheckedChange = { selectedPhotos[index] = it },
                                )
                                AsyncImage(
                                    model = photo.filePath,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier.size(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .padding(end = 8.dp),
                                )
                                Text(
                                    if (photo.isVideo) stringResource(R.string.retro_cd_video)
                                    else stringResource(R.string.retro_cd_photo),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val textContent =
                            if (shareText) buildShareText(context, summary, parsed) else null
                        val photoUris =
                            photos
                                .filterIndexed { i, _ ->
                                    i < selectedPhotos.size && selectedPhotos[i]
                                }
                                .map { photo ->
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        java.io.File(photo.filePath),
                                    )
                                }
                        val intent =
                            if (photoUris.isNotEmpty()) {
                                android.content
                                    .Intent(android.content.Intent.ACTION_SEND_MULTIPLE)
                                    .apply {
                                        type = "image/*"
                                        putParcelableArrayListExtra(
                                            android.content.Intent.EXTRA_STREAM,
                                            ArrayList(photoUris),
                                        )
                                        if (textContent != null) {
                                            putExtra(android.content.Intent.EXTRA_TEXT, textContent)
                                        }
                                        addFlags(
                                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                            } else if (textContent != null) {
                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, textContent)
                                }
                            } else null

                        if (intent != null) {
                            context.startActivity(
                                android.content.Intent.createChooser(
                                    intent,
                                    context.getString(R.string.retro_share_title),
                                )
                            )
                        }
                        showShareDialog = false
                    },
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor =
                                if (LocalIsDarkTheme.current) Color(0xFF2C4A6E)
                                else Color(0xFF1976D2)
                        ),
                ) {
                    Text(stringResource(R.string.action_share))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showShareDialog = false }) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    // Full-screen photo/video viewer with pinch-to-zoom and paging
    fullScreenPhotoPath?.let { initialPath ->
        val initialPage = photos.indexOfFirst { it.filePath == initialPath }.coerceAtLeast(0)
        val pagerState =
            androidx.compose.foundation.pager.rememberPagerState(initialPage = initialPage) {
                photos.size
            }
        var currentPageZoomed by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { fullScreenPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !currentPageZoomed,
                ) { page ->
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    LaunchedEffect(scale, pagerState.currentPage) {
                        if (page == pagerState.currentPage) {
                            currentPageZoomed = scale > 1f
                        }
                    }
                    LaunchedEffect(pagerState.currentPage) {
                        if (page != pagerState.currentPage) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }

                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        do {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.count { it.pressed }
                                            if (pressed >= 2) {
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()
                                                scale = (scale * zoom).coerceIn(1f, 5f)
                                                if (scale > 1f) {
                                                    offsetX += pan.x
                                                    offsetY += pan.y
                                                    val mx = 1000f * (scale - 1)
                                                    val my = 1500f * (scale - 1)
                                                    offsetX = offsetX.coerceIn(-mx, mx)
                                                    offsetY = offsetY.coerceIn(-my, my)
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                event.changes.forEach { it.consume() }
                                            } else if (pressed == 1 && scale > 1f) {
                                                val pan = event.calculatePan()
                                                offsetX += pan.x
                                                offsetY += pan.y
                                                val mx = 1000f * (scale - 1)
                                                val my = 1500f * (scale - 1)
                                                offsetX = offsetX.coerceIn(-mx, mx)
                                                offsetY = offsetY.coerceIn(-my, my)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1f) {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            } else {
                                                scale = 2.5f
                                            }
                                        },
                                        onTap = { fullScreenPhotoPath = null },
                                    )
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photos[page].isVideo) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoPath(photos[page].filePath)
                                        setMediaController(
                                            android.widget.MediaController(ctx).also {
                                                it.setAnchorView(this)
                                            }
                                        )
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            AsyncImage(
                                model = java.io.File(photos[page].filePath),
                                contentDescription =
                                    stringResource(R.string.retro_photo_n, page + 1),
                                modifier =
                                    Modifier.fillMaxSize().graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    },
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
                if (photos.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / ${photos.size}",
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                IconButton(
                    onClick = { fullScreenPhotoPath = null },
                    modifier =
                        Modifier.align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        stringResource(R.string.action_close),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

private fun buildShareText(
    context: android.content.Context,
    summary: com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity,
    parsed: ParsedRetrospective,
): String = buildString {
    append(context.getString(R.string.retro_share_header))
    append("\n")
    append(summary.periodLabel)
    append(" \u2014 ")
    append(summary.title)
    append(
        "\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
    )
    if (parsed.bulletPoints.isNotEmpty()) {
        append("\n\n${context.getString(R.string.retro_at_a_glance_colon)}")
        parsed.bulletPoints.forEach { append("\n\u2022 $it") }
    }
    if (parsed.sections.isNotEmpty()) {
        parsed.sections.forEach { section ->
            append("\n\n\u2728 ${section.heading}")
            append("\n${section.body}")
        }
    } else {
        append("\n\n${summary.summaryText}")
    }
}

// ── Locked Week Placeholder ────────────────────────────────────────────────

@Composable
private fun LockedWeekEntry(periodLabel: String, isLast: Boolean, onClick: () -> Unit) {
    val isDark = LocalIsDarkTheme.current
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline dot (lock icon instead of color dot)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.2f)
                            else Color.Black.copy(alpha = 0.15f)
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.retro_cd_locked),
                    modifier = Modifier.size(9.dp),
                    tint =
                        if (isDark) Color.White.copy(alpha = 0.5f)
                        else Color.Black.copy(alpha = 0.4f),
                )
            }
            if (!isLast) {
                Box(
                    modifier =
                        Modifier.width(2.dp)
                            .height(60.dp)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.08f)
                                else Color.Black.copy(alpha = 0.08f)
                            )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Locked card
        Card(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isDark) Color(0xFF181818).copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.6f)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (isDark) Color.White.copy(alpha = 0.4f)
                            else Color.Black.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.retro_unlock_premium_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = FeatureAccentOrange.copy(alpha = 0.8f),
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.retro_cd_premium_required),
                    modifier = Modifier.size(20.dp),
                    tint = FeatureAccentOrange.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Review Benefits Info Dialog ────────────────────────────────────────────

@Composable
private fun ReviewBenefitsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_understood))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.retro_your_ai_reviews),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BenefitSection(
                    title = stringResource(R.string.retro_weekly_title),
                    points =
                        listOf(
                            stringResource(R.string.retro_weekly_p1),
                            stringResource(R.string.retro_weekly_p2),
                            stringResource(R.string.retro_weekly_p3),
                            stringResource(R.string.retro_weekly_p4),
                        ),
                )
                BenefitSection(
                    title = stringResource(R.string.retro_monthly_title),
                    points =
                        listOf(
                            stringResource(R.string.retro_monthly_p1),
                            stringResource(R.string.retro_monthly_p2),
                            stringResource(R.string.retro_monthly_p3),
                        ),
                    isPremium = true,
                )
                BenefitSection(
                    title = stringResource(R.string.retro_yearly_title),
                    points =
                        listOf(
                            stringResource(R.string.retro_yearly_p1),
                            stringResource(R.string.retro_yearly_p2),
                            stringResource(R.string.retro_yearly_p3),
                        ),
                    isPremium = true,
                )
            }
        },
    )
}

@Composable
private fun BenefitSection(title: String, points: List<String>, isPremium: Boolean = false) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isPremium) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = stringResource(R.string.label_premium),
                    modifier = Modifier.size(14.dp),
                    tint = FeatureAccentOrange,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        points.forEach { point ->
            Row(modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Review-specific Premium Upsell Sheet ───────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReviewPremiumSheet(onSubscribe: () -> Unit, onDismiss: () -> Unit) {
    // skipPartiallyExpanded = true → sheet opens fully, not half-way
    val sheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Breathing animation on the CTA button
    val infiniteTransition = rememberInfiniteTransition(label = "reviewCta")
    val ctaScale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "reviewCtaScale",
        )

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.retro_your_reviews),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.retro_premium_reviews_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Review-specific benefits
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReviewBenefitPoint(stringResource(R.string.retro_benefit_weekly))
                ReviewBenefitPoint(stringResource(R.string.retro_benefit_monthly))
                ReviewBenefitPoint(stringResource(R.string.retro_benefit_yearly))
            }

            Spacer(modifier = Modifier.height(28.dp))
            androidx.compose.material3.Button(
                onClick = onSubscribe,
                modifier =
                    Modifier.fillMaxWidth().height(54.dp).graphicsLayer {
                        scaleX = ctaScale
                        scaleY = ctaScale
                    },
                shape = RoundedCornerShape(16.dp),
                colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
            ) {
                Text(
                    stringResource(R.string.retro_start_sub),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.retro_decide_later),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewBenefitPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
            tint = FeatureAccentOrange,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
