package com.bestjournal.app.ui.screens.paywall

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
        "Schreibe ungest\u00f6rt, ohne Limits und ohne Werbung",
    )

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
) {
    val monthlyPrice by viewModel.monthlyPrice.collectAsState()
    val yearlyPrice by viewModel.yearlyPrice.collectAsState()
    val personalizedHeadline by viewModel.personalizedHeadline.collectAsState()
    val activity = LocalContext.current as? Activity

    val displayMonthlyPrice = monthlyPrice.ifEmpty { Constants.MONTHLY_PRICE_DISPLAY }
    val displayYearlyPrice = yearlyPrice.ifEmpty { Constants.YEARLY_PRICE_DISPLAY }

    // Use theme-aware colors so the orb matches the current color scheme (incl. Dynamic Color)
    val isDarkTheme = LocalIsDarkTheme.current
    val orbPrimaryColor = MaterialTheme.colorScheme.primary
    val orbSecondaryColor = MaterialTheme.colorScheme.tertiary

    // Exit-intent dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Calculate half monthly price for exit-intent 50% offer
    val halfMonthlyPrice = remember(displayMonthlyPrice) {
        val price = displayMonthlyPrice.replace("[^0-9,.]".toRegex(), "")
            .replace(",", ".").toDoubleOrNull()
        if (price != null) {
            String.format("%.2f\u00A0\u20AC", price / 2).replace(".", ",")
        } else {
            "2,50\u00A0\u20AC"
        }
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
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        ) {
                            // Connecting line at icon center height
                            HorizontalDivider(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .align(Alignment.TopCenter)
                                        .padding(top = 9.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = NeonEmerald,
                                    )
                                    Text(
                                        text = "Heute",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = "Voller Zugang",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = NeonAmber,
                                    )
                                    Text(
                                        text = "Tag 6",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = "Erinnerung",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "Tag 7",
                                        style = MaterialTheme.typography.bodySmall,
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "Monatsabo, $displayMonthlyPrice pro Monat",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Yearly subscription (direct) ──
                OutlinedButton(
                    onClick = {
                        viewModel.analyticsTracker.trackYearlyCtaClicked()
                        activity?.let { act ->
                            if (!viewModel.launchPurchaseFlow(act, isYearly = true)) {
                                Toast.makeText(act, "Abo wird geladen, bitte versuche es gleich nochmal.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "Jahresabo, $displayYearlyPrice pro Jahr",
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

                Spacer(modifier = Modifier.height(8.dp))

                // ── No thanks ──
                TextButton(
                    onClick = {
                        viewModel.analyticsTracker.trackNoThanksClicked()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Weiter ohne Premium",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Exit-intent dialog ──
            if (showExitDialog) {
                LaunchedEffect(Unit) {
                    viewModel.analyticsTracker.trackExitIntentShown()
                }
                Dialog(onDismissRequest = {
                    viewModel.analyticsTracker.trackExitIntentRejected()
                    onDismiss()
                }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "Warte kurz!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Wir m\u00f6chten dir den Einstieg leicht machen:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── 50% discount highlight ──
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "50%",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Rabatt im ersten Monat",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            text = "Nur $halfMonthlyPrice statt $displayMonthlyPrice",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "+ 3 zus\u00e4tzliche Test-Tage, komplett kostenlos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = {
                                    viewModel.analyticsTracker.trackExitIntentRejected()
                                    onDismiss()
                                }) {
                                    Text(
                                        text = "Nein danke",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.analyticsTracker.trackExitIntentAccepted()
                                        activity?.let { act ->
                                            if (!viewModel.launchPurchaseFlow(act, isYearly = true)) {
                                                Toast.makeText(act, "Abo wird geladen, bitte versuche es gleich nochmal.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        showExitDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text(
                                        text = "10 Tage kostenlos testen",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
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
