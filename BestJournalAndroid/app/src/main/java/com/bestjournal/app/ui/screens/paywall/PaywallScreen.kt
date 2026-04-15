package com.bestjournal.app.ui.screens.paywall

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bestjournal.app.ui.components.PulsingOrb
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.util.Constants
import kotlinx.coroutines.delay

private val benefits =
    listOf(
        "Verstehe verborgene Muster in deinem Denken",
        "Unbegrenzte KI-Textverbesserung f\u00fcr jeden Eintrag",
        "5 intelligente Analyse-Profile f\u00fcr verschiedene Perspektiven",
        "Automatische Dashboard-Updates nach jedem Eintrag",
        "Wochen-, Monats- und Jahresr\u00fcckblicke mit KI",
        "Tagebucheintr\u00e4ge mit Fotos als PDF exportieren",
        "Spreche oder schreibe ungest\u00f6rt, ohne Werbung",
    )

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
) {
    val monthlyPrice by viewModel.monthlyPrice.collectAsState()
    val yearlyPrice by viewModel.yearlyPrice.collectAsState()
    val lifetimePrice by viewModel.lifetimePrice.collectAsState()
    val personalizedHeadline by viewModel.personalizedHeadline.collectAsState()
    val activity = LocalContext.current as? Activity

    val pricesLoaded = monthlyPrice.isNotEmpty()
    val displayMonthlyPrice = monthlyPrice.ifEmpty { "\u2026" }
    val displayYearlyPrice = yearlyPrice.ifEmpty { "\u2026" }
    val displayLifetimePrice = lifetimePrice.ifEmpty { "\u2026" }

    // Use theme-aware colors so the orb matches the current color scheme (incl. Dynamic Color)
    val isDarkTheme = LocalIsDarkTheme.current
    val orbPrimaryColor = MaterialTheme.colorScheme.primary
    val orbSecondaryColor = MaterialTheme.colorScheme.tertiary

    // Exit-intent dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Calculate half monthly price for exit-intent 50% offer (currency-neutral)
    val halfMonthlyPrice = remember(displayMonthlyPrice) {
        formatDerivedPrice(displayMonthlyPrice, 0.5)
    }

    // Calculate yearly savings vs monthly
    val savingsPercent = remember(displayMonthlyPrice, displayYearlyPrice) {
        val monthly = displayMonthlyPrice.replace("[^0-9,.]".toRegex(), "")
            .replace(",", ".").toDoubleOrNull()
        val yearly = displayYearlyPrice.replace("[^0-9,.]".toRegex(), "")
            .replace(",", ".").toDoubleOrNull()
        if (monthly != null && yearly != null && monthly > 0) {
            ((monthly * 12 - yearly) / (monthly * 12) * 100).toInt()
        } else {
            37 // fallback
        }
    }

    // Calculate daily price from yearly for price reframing (currency-neutral)
    val dailyPrice = remember(displayYearlyPrice) {
        formatDerivedPrice(displayYearlyPrice, 1.0 / 365.0)
    }

    // Track paywall_shown + personalization on first composition
    LaunchedEffect(Unit) {
        viewModel.analyticsTracker.trackPaywallShown(viewModel.source)
        viewModel.analyticsTracker.trackPaywallPersonalized(viewModel.matchedGoalType)
    }

    // Staggered benefit entrance animation
    var visibleBenefits by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(300) // wait for screen transition to settle
        for (i in benefits.indices) {
            delay(80)
            visibleBenefits = i + 1
        }
    }

    // Subtle breathing animation on primary CTA to draw the eye
    val infiniteTransition = rememberInfiniteTransition(label = "cta")
    val ctaScale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.02f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "ctaBreathing",
        )

    // Track trial_timeline_viewed when all benefits have animated in
    LaunchedEffect(visibleBenefits >= benefits.size) {
        if (visibleBenefits >= benefits.size) {
            viewModel.analyticsTracker.trackTrialTimelineViewed()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Scrollable content ──
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(top = 56.dp) // room for fixed X button
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Hero: Animated PulsingOrb (orange in dark, cyan in light) ──
                PulsingOrb(
                    size = 140.dp,
                    entropyLevel = 0.7f,
                    color = orbPrimaryColor,
                    secondaryColor = orbSecondaryColor,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Personalized headline (adapts to onboarding goals) ──
                Text(
                    text = personalizedHeadline.first,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Personalized subtitle ──
                Text(
                    text = personalizedHeadline.second,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Benefits with staggered entrance ──
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    benefits.forEachIndexed { index, benefit ->
                        AnimatedVisibility(
                            visible = index < visibleBenefits,
                            enter =
                                fadeIn(tween(300)) +
                                    slideInHorizontally(tween(300)) { -it / 4 },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── "Prices loading" hint — only visible until Google Play responds ──
                if (!pricesLoaded) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preise werden geladen\u2026",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ── Primary CTA: yearly with free trial ──
                Button(
                    onClick = {
                        viewModel.analyticsTracker.trackTrialCtaClicked()
                        activity?.let { act ->
                            if (!viewModel.launchPurchaseFlow(act, isYearly = true)) {
                                Toast.makeText(act, "Abo wird geladen, bitte versuche es gleich nochmal.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(ctaScale),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text(
                        text = "Weiter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                // ── Trial Timeline ──
                AnimatedVisibility(
                    visible = visibleBenefits >= benefits.size,
                    enter = fadeIn(tween(300)),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        ) {
                            // Connecting line at circle center height
                            HorizontalDivider(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 36.dp)
                                        .align(Alignment.TopCenter)
                                        .padding(top = 16.dp),
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(NeonEmerald.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = NeonEmerald,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Heute",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Voller Zugang",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(NeonAmber.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Notifications,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = NeonAmber,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tag 7",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Erinnerung",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.CreditCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tag 8",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = "Erste Zahlung",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Danach $displayYearlyPrice pro Jahr\nIn der Testphase jederzeit k\u00fcndbar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Yearly subscription (highlighted — best value) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.analyticsTracker.trackYearlyCtaClicked()
                            activity?.let { act ->
                                if (!viewModel.launchPurchaseFlow(act, isYearly = true)) {
                                    Toast.makeText(act, "Abo wird geladen, bitte versuche es gleich nochmal.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Ab $dailyPrice pro Tag",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "statt $displayMonthlyPrice pro Monat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // ── "Beliebteste Wahl" badge ──
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-16).dp, y = (-11).dp),
                        shape = RoundedCornerShape(11.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp,
                    ) {
                        Text(
                            text = "Beliebteste Wahl",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }

                    // Track badge visibility
                    LaunchedEffect(Unit) {
                        viewModel.analyticsTracker.trackYearlyBadgeViewed()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Monthly subscription ──
                OutlinedButton(
                    onClick = {
                        viewModel.analyticsTracker.trackMonthlyCtaClicked()
                        activity?.let { act ->
                            if (!viewModel.launchPurchaseFlow(act, isYearly = false)) {
                                Toast.makeText(act, "Abo wird geladen, bitte versuche es gleich nochmal.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "Monatsabo, $displayMonthlyPrice pro Monat",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Spare $savingsPercent% gegen\u00fcber dem Monatsabo, jederzeit k\u00fcndbar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Divider with "oder" separator ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    Text(
                        text = "oder",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Lifetime one-time purchase (premium card with amber accent) ──
                Surface(
                    onClick = {
                        viewModel.analyticsTracker.trackLifetimeCtaClicked()
                        activity?.let { act ->
                            if (!viewModel.launchPurchaseFlow(act, isLifetime = true)) {
                                Toast.makeText(
                                    act,
                                    "Kaufvorgang wird geladen, bitte versuche es gleich nochmal.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                NeonAmber.copy(alpha = 0.5f),
                                NeonAmber.copy(alpha = 0.15f),
                            ),
                        ),
                    ),
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonAmber.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.AllInclusive,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = NeonAmber,
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Einmalkauf",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Einmal zahlen, alles f\u00fcr immer nutzen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = displayLifetimePrice,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Kein Abo, keine Verl\u00e4ngerung",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Free tier note (two lines for readability) ──
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Die App funktioniert auch ohne Abo,",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "mit eingeschr\u00e4nkten Limits.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Exit-intent dialog ──
            // Always shows 50% monthly discount for 2 months + 2 bonus trial days.
            if (showExitDialog) {
                LaunchedEffect(Unit) {
                    viewModel.analyticsTracker.trackExitIntentShown()
                }

                val exitCtaTransition = rememberInfiniteTransition(label = "exitCta")
                val exitCtaScale by exitCtaTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "exitCtaBreathing",
                )

                Dialog(
                    onDismissRequest = {
                        viewModel.analyticsTracker.trackExitIntentRejected()
                        onDismiss()
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    val gradientTop = if (isDarkTheme) Color(0xFF2A2622) else Color(0xFFFFF8F0)
                    val gradientBottom = if (isDarkTheme) Color(0xFF222926) else Color(0xFFF0F5F2)

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .offset(y = (-60).dp),
                            shape = RoundedCornerShape(28.dp),
                            color = gradientTop,
                            shadowElevation = 24.dp,
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(gradientTop, gradientBottom),
                                            ),
                                        ),
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    PulsingOrb(
                                        size = 56.dp,
                                        entropyLevel = 0.5f,
                                        color = orbPrimaryColor,
                                        secondaryColor = orbSecondaryColor,
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Warte kurz!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Wir haben ein besonderes\nAngebot f\u00fcr dich:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // ── 50% discount for 2 months + 2 extra trial days ──
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shadowElevation = 8.dp,
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(vertical = 20.dp, horizontal = 24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = "50%",
                                                style = MaterialTheme.typography.displaySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Rabatt f\u00fcr ${Constants.EXIT_INTENT_DISCOUNT_MONTHS} Monate",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Nur $halfMonthlyPrice statt $displayMonthlyPrice pro Monat",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "+ ${Constants.EXIT_INTENT_TRIAL_BONUS_DAYS} Tage extra Testzeit",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeonEmerald,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // ── CTA: 50% monthly promo + trial extension ──
                                    Button(
                                        onClick = {
                                            viewModel.analyticsTracker.trackExitIntentAccepted()
                                            activity?.let { act ->
                                                val launched = viewModel.launchPurchaseFlow(
                                                    act,
                                                    isYearly = false,
                                                    usePromoOffer = true,
                                                )
                                                if (launched) {
                                                    // Only extend trial when billing flow actually started
                                                    viewModel.extendTrial()
                                                } else {
                                                    Toast.makeText(act, "Abo wird geladen, bitte versuche es gleich nochmal.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            showExitDialog = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .scale(exitCtaScale),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    ) {
                                        Text(
                                            text = "Monatsabo mit 50% Rabatt starten",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    TextButton(onClick = {
                                        viewModel.analyticsTracker.trackExitIntentRejected()
                                        onDismiss()
                                    }) {
                                        Text(
                                            text = "Nein danke",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Fixed close button (shows exit-intent dialog on first tap) ──
            IconButton(
                onClick = {
                    viewModel.analyticsTracker.trackPaywallDismissed()
                    showExitDialog = true
                },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 12.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Schlie\u00dfen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Calculates a derived price (e.g. 50% off, daily equivalent) while preserving
 * the original currency symbol and decimal format from Google Play's formattedPrice.
 * Works with all locales: "3,99 €", "$3.99", "₹329.00", "¥580", "R$ 19,90".
 */
private fun formatDerivedPrice(originalPrice: String, factor: Double): String {
    if (originalPrice.isBlank()) return ""

    // Extract numeric portion
    val numStr = originalPrice.replace("[^0-9,.]".toRegex(), "")
    if (numStr.isBlank()) return originalPrice

    // Detect if comma is decimal separator (European: last separator with <=2 digits after)
    val lastComma = numStr.lastIndexOf(',')
    val lastDot = numStr.lastIndexOf('.')
    val usesCommaDecimal = lastComma > lastDot && numStr.length - lastComma <= 3

    // Normalize to parseable double
    val normalized = if (usesCommaDecimal) {
        numStr.replace(".", "").replace(",", ".")
    } else {
        numStr.replace(",", "")
    }
    val amount = normalized.toDoubleOrNull() ?: return originalPrice
    val newAmount = amount * factor

    // Detect if original has decimal places
    val hasDecimals = (usesCommaDecimal && lastComma >= 0) || (!usesCommaDecimal && lastDot >= 0)

    // Format with same decimal style
    val formatted = if (hasDecimals) {
        val f = String.format("%.2f", newAmount)
        if (usesCommaDecimal) f.replace(".", ",") else f
    } else {
        newAmount.toInt().toString()
    }

    // Replace the numeric part in the original string, keeping currency symbol in place
    return originalPrice.replaceFirst(Regex("[0-9][0-9.,]*[0-9]|[0-9]+"), formatted)
}
