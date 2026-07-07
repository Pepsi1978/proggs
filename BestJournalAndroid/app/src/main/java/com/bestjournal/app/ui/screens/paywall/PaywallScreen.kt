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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.AiLimitsDisclaimerRow
import com.bestjournal.app.ui.components.AiLimitsFreeNote
import com.bestjournal.app.ui.components.PulsingOrb
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.util.Constants
import kotlinx.coroutines.delay

// Benefits list is built inside the composable to support stringResource
// See benefitsList below

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
    // D1-Fix (2026-05-31): opens the Terms of Use (which contain the §355 BGB
    // withdrawal instruction) so the consumer can read it BEFORE purchasing.
    onOpenTerms: () -> Unit = {},
) {
    val monthlyPrice by viewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearlyPrice by viewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetimePrice by viewModel.lifetimePrice.collectAsStateWithLifecycle()
    val personalizedHeadline by viewModel.personalizedHeadline.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Loop-7 fix (Frank, 2026-04-30): close the paywall automatically as
    // soon as Google confirms the purchase. The previous behaviour left
    // the upsell card on screen so the user had to tap the close button
    // and could even re-enter the offer flow on a freshly purchased plan.
    androidx.compose.runtime.LaunchedEffect(subscriptionState) {
        if (subscriptionState is com.bestjournal.app.billing.SubscriptionState.Subscribed) {
            onDismiss()
        }
    }

    val benefitsList =
        listOf(
            stringResource(R.string.paywall_feature_patterns),
            stringResource(R.string.paywall_feature_improve),
            stringResource(R.string.paywall_feature_dashboard),
            stringResource(R.string.paywall_feature_retro),
            stringResource(R.string.paywall_feature_profiles),
            stringResource(R.string.paywall_feature_followups),
            stringResource(R.string.paywall_feature_pdf),
            stringResource(R.string.paywall_feature_noads),
        )

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

    val launchPurchase: (String) -> Unit = { plan ->
        activity?.let { act ->
            val launched =
                when (plan) {
                    "yearly" -> viewModel.launchPurchaseFlow(act, isYearly = true)
                    "monthly" -> viewModel.launchPurchaseFlow(act, isYearly = false)
                    "lifetime" -> viewModel.launchPurchaseFlow(act, isLifetime = true)
                    else -> false
                }
            if (!launched) {
                val toastRes =
                    if (plan == "lifetime") {
                        R.string.paywall_purchase_loading_toast
                    } else {
                        R.string.paywall_sub_loading_toast
                    }
                Toast.makeText(act, context.getString(toastRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Calculate half monthly price for exit-intent 50% offer (currency-neutral)
    val halfMonthlyPrice =
        remember(displayMonthlyPrice) { formatDerivedPrice(displayMonthlyPrice, 0.5) }

    // Calculate yearly savings vs monthly (currency-neutral parsing)
    val savingsPercent =
        remember(displayMonthlyPrice, displayYearlyPrice) {
            val monthly = parseLocalizedPrice(displayMonthlyPrice)
            val yearly = parseLocalizedPrice(displayYearlyPrice)
            if (monthly != null && yearly != null && monthly > 0) {
                ((monthly * 12 - yearly) / (monthly * 12) * 100).toInt()
            } else {
                37 // fallback
            }
        }

    // Calculate daily price from yearly for price reframing (currency-neutral)
    val dailyPrice =
        remember(displayYearlyPrice) { formatDerivedPrice(displayYearlyPrice, 1.0 / 365.0) }

    // Track paywall_shown + personalization on first composition
    LaunchedEffect(Unit) {
        viewModel.analyticsTracker.trackPaywallShown(viewModel.source)
        viewModel.analyticsTracker.trackPaywallPersonalized(viewModel.matchedGoalType)
    }

    // Staggered benefit entrance animation
    var visibleBenefits by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(300) // wait for screen transition to settle
        for (i in benefitsList.indices) {
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

    // Track trial_timeline_viewed when all benefitsList have animated in
    LaunchedEffect(visibleBenefits >= benefitsList.size) {
        if (visibleBenefits >= benefitsList.size) {
            viewModel.analyticsTracker.trackTrialTimelineViewed()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Scrollable content ──
            Column(
                modifier =
                    Modifier.fillMaxSize()
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
                    benefitsList.forEachIndexed { index, benefit ->
                        AnimatedVisibility(
                            visible = index < visibleBenefits,
                            enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 },
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

                Spacer(modifier = Modifier.height(12.dp))
                AnimatedVisibility(
                    visible = visibleBenefits >= benefitsList.size,
                    enter = fadeIn(tween(300)),
                ) {
                    AiLimitsDisclaimerRow()
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
                            text = stringResource(R.string.paywall_prices_loading),
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
                                Toast.makeText(
                                        act,
                                        context.getString(R.string.paywall_sub_loading_toast),
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(ctaScale),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.action_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                // ── Trial Timeline ──
                AnimatedVisibility(
                    visible = visibleBenefits >= benefitsList.size,
                    enter = fadeIn(tween(300)),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            // Connecting line at circle center height
                            HorizontalDivider(
                                modifier =
                                    Modifier.fillMaxWidth()
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
                                            Modifier.size(32.dp)
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
                                        text = stringResource(R.string.paywall_today),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = stringResource(R.string.paywall_full_access),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier =
                                            Modifier.size(32.dp)
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
                                        text = stringResource(R.string.paywall_day7),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = stringResource(R.string.paywall_reminder),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier =
                                            Modifier.size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.15f
                                                    )
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
                                        text = stringResource(R.string.paywall_day8),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = stringResource(R.string.paywall_first_payment),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.paywall_yearly_note, displayYearlyPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Yearly subscription (highlighted — best value) ──
                Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.analyticsTracker.trackYearlyCtaClicked()
                            launchPurchase("yearly")
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.paywall_from_per_day,
                                        displayYearlyPrice,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text =
                                    stringResource(
                                        R.string.paywall_instead_per_month,
                                        savingsPercent,
                                    ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // ── "Beliebteste Wahl" badge ──
                    Surface(
                        modifier =
                            Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = (-11).dp),
                        shape = RoundedCornerShape(11.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp,
                    ) {
                        Text(
                            text = stringResource(R.string.paywall_most_popular),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }

                    // Track badge visibility
                    LaunchedEffect(Unit) { viewModel.analyticsTracker.trackYearlyBadgeViewed() }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Monthly subscription ──
                OutlinedButton(
                    onClick = {
                        viewModel.analyticsTracker.trackMonthlyCtaClicked()
                        launchPurchase("monthly")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.paywall_monthly_plan, displayMonthlyPrice),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Text(
                    text = stringResource(R.string.paywall_monthly_note, displayMonthlyPrice),
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
                        text = stringResource(R.string.paywall_or),
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
                        launchPurchase("lifetime")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border =
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        NeonAmber.copy(alpha = 0.5f),
                                        NeonAmber.copy(alpha = 0.15f),
                                    )
                            ),
                        ),
                    shadowElevation = 2.dp,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier.size(40.dp)
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
                                text = stringResource(R.string.paywall_lifetime_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.paywall_lifetime_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // 'Kein Abo, keine Verlaengerung' direkt unter
                            // dem Desc-Satz — innerhalb der Lifetime-Blase,
                            // nicht mehr separat zentriert darunter.
                            Text(
                                text = stringResource(R.string.paywall_lifetime_note),
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

                Spacer(modifier = Modifier.height(16.dp))

                // ── Free tier note (two lines for readability) — whole block clickable ──
                AiLimitsFreeNote()

                // ── D1-Fix (2026-05-31): legal hint + link to Terms (with §355 BGB
                // withdrawal instruction). Google Play Billing handles the §312j
                // "pay now" confirmation; this makes the withdrawal info reachable
                // BEFORE the purchase, closing the only remaining gap. ──
                // D1-Fix (2026-05-31): only the "Terms of Use" part is rendered as a
                // visible link (dark blue + underline); the rest stays plain hint text.
                // %1$s in paywall_legal_hint marks where the link word goes (we split it
                // ourselves, so the string is formatted="false" — no String.format).
                val termsLinkColor =
                    if (isDarkTheme) Color(0xFF82B1FF) else Color(0xFF1565C0)
                val legalHint = stringResource(R.string.paywall_legal_hint)
                val legalLinkText = stringResource(R.string.paywall_legal_hint_link)
                val legalAnnotated = buildAnnotatedString {
                    val parts = legalHint.split("%1\$s", limit = 2)
                    append(parts[0])
                    withStyle(
                        SpanStyle(
                            color = termsLinkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                    ) {
                        append(legalLinkText)
                    }
                    if (parts.size > 1) append(parts[1])
                }
                TextButton(
                    onClick = onOpenTerms,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = legalAnnotated,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Exit-intent dialog ──
            // Always shows 50% monthly discount for 2 months + 2 bonus trial days.
            if (showExitDialog) {
                LaunchedEffect(Unit) { viewModel.analyticsTracker.trackExitIntentShown() }

                val exitCtaTransition = rememberInfiniteTransition(label = "exitCta")
                val exitCtaScale by
                    exitCtaTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.04f,
                        animationSpec =
                            infiniteRepeatable(
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

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier =
                                Modifier.widthIn(max = 420.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .offset(y = (-60).dp),
                            shape = RoundedCornerShape(28.dp),
                            color = gradientTop,
                            shadowElevation = 24.dp,
                        ) {
                            Box {
                                Box(
                                    modifier =
                                        Modifier.matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(gradientTop, gradientBottom)
                                                )
                                            )
                                )

                                Column(
                                    modifier =
                                        Modifier.fillMaxWidth()
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
                                        text = stringResource(R.string.paywall_exit_wait),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.paywall_exit_special_offer),
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
                                            modifier =
                                                Modifier.padding(
                                                    vertical = 20.dp,
                                                    horizontal = 24.dp,
                                                ),
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
                                                text =
                                                    stringResource(
                                                        R.string.paywall_exit_discount,
                                                        Constants.EXIT_INTENT_DISCOUNT_MONTHS,
                                                    ),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text =
                                                    stringResource(
                                                        R.string.paywall_exit_price_comparison,
                                                        halfMonthlyPrice,
                                                        displayMonthlyPrice,
                                                    ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color =
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                        .copy(alpha = 0.7f),
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text =
                                                    stringResource(
                                                        R.string.paywall_exit_bonus_days,
                                                        Constants.EXIT_INTENT_TRIAL_BONUS_DAYS,
                                                    ),
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
                                                val launched =
                                                    viewModel.launchPurchaseFlow(
                                                        act,
                                                        isYearly = false,
                                                        usePromoOffer = true,
                                                    )
                                                if (launched) {
                                                    // Trial extension happens after Google confirms
                                                    // the purchase
                                                    viewModel.onExitIntentPurchaseStarted()
                                                } else {
                                                    Toast.makeText(
                                                            act,
                                                            context.getString(
                                                                R.string.paywall_sub_loading_toast
                                                            ),
                                                            Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                                }
                                            }
                                            showExitDialog = false
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .height(56.dp)
                                                .scale(exitCtaScale),
                                        shape = RoundedCornerShape(16.dp),
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                    ) {
                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.paywall_exit_start_discount
                                                ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    TextButton(
                                        onClick = {
                                            viewModel.analyticsTracker.trackExitIntentRejected()
                                            onDismiss()
                                        }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_no_thanks),
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
                    Modifier.align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 12.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Parses a localized price string from Google Play into a Double. Correctly handles all
 * decimal/thousands separator styles: "3,99" (comma decimal), "3.99" (dot decimal), "3,499.00"
 * (thousands+dot), "1.234,56" (thousands+comma), "580" (no decimals).
 */
private fun parseLocalizedPrice(formattedPrice: String): Double? {
    val numStr = formattedPrice.replace("[^0-9,.]".toRegex(), "")
    if (numStr.isBlank()) return null
    val lastComma = numStr.lastIndexOf(',')
    val lastDot = numStr.lastIndexOf('.')
    val usesCommaDecimal = lastComma > lastDot && numStr.length - lastComma <= 3
    val normalized =
        if (usesCommaDecimal) {
            numStr.replace(".", "").replace(",", ".")
        } else {
            numStr.replace(",", "")
        }
    return normalized.toDoubleOrNull()
}

/**
 * Calculates a derived price (e.g. 50% off, daily equivalent) while preserving the original
 * currency symbol and decimal format from Google Play's formattedPrice. Works with all locales:
 * "3,99 €", "$3.99", "₹329.00", "¥580", "R$ 19,90".
 */
private fun formatDerivedPrice(originalPrice: String, factor: Double): String {
    if (originalPrice.isBlank()) return ""

    val numStr = originalPrice.replace("[^0-9,.]".toRegex(), "")
    if (numStr.isBlank()) return originalPrice

    val amount = parseLocalizedPrice(originalPrice) ?: return originalPrice
    val newAmount = amount * factor

    val lastComma = numStr.lastIndexOf(',')
    val lastDot = numStr.lastIndexOf('.')
    val usesCommaDecimal = lastComma > lastDot && numStr.length - lastComma <= 3
    val hasDecimals = (usesCommaDecimal && lastComma >= 0) || (!usesCommaDecimal && lastDot >= 0)

    val formatted =
        if (hasDecimals) {
            val f = String.format(java.util.Locale.US, "%.2f", newAmount)
            if (usesCommaDecimal) f.replace(".", ",") else f
        } else {
            newAmount.toInt().toString()
        }

    // Also match spaces (regular, non-breaking, narrow non-breaking) used as thousands separators
    return originalPrice.replaceFirst(Regex("[0-9][0-9., \u00A0\u202F]*[0-9]|[0-9]+"), formatted)
}
