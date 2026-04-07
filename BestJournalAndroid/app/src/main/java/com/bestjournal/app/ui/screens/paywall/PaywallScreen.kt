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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.bestjournal.app.ui.components.PulsingOrb
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonViolet
import com.bestjournal.app.ui.theme.WarmCopper
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
    val activity = LocalContext.current as? Activity

    val displayMonthlyPrice = monthlyPrice.ifEmpty { Constants.MONTHLY_PRICE_DISPLAY }
    val displayYearlyPrice = yearlyPrice.ifEmpty { Constants.YEARLY_PRICE_DISPLAY }

    // Dark mode: warm orange orb, Light mode: cool cyan orb
    val isDarkTheme = LocalIsDarkTheme.current
    val orbPrimaryColor = if (isDarkTheme) WarmCopper else NeonCyan
    val orbSecondaryColor = if (isDarkTheme) NeonAmber else NeonViolet

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

    // Track paywall_shown on first composition
    LaunchedEffect(Unit) {
        viewModel.analyticsTracker.trackPaywallShown(viewModel.source)
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

                // ── Emotional headline ──
                Text(
                    text = "Entdecke dich selbst\nJeden Tag ein St\u00fcck mehr",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Subtitle ──
                Text(
                    text = "Dein pers\u00f6nlicher KI-Begleiter ohne Grenzen",
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
                        text = "7 Tage kostenlos testen",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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

            // ── Fixed close button (stays visible while scrolling) ──
            IconButton(
                onClick = {
                    viewModel.analyticsTracker.trackPaywallDismissed()
                    onDismiss()
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
