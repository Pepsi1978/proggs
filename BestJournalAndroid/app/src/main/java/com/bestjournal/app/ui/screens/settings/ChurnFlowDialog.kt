package com.bestjournal.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bestjournal.app.billing.SubscriptionType
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import androidx.compose.ui.res.stringResource
import com.bestjournal.app.R

@Composable
fun ChurnFlowDialog(
    onDismiss: () -> Unit,
    onOfferAccepted: () -> Unit,
    onCancelConfirmed: () -> Unit,
    onSwitchToYearly: () -> Unit,
    onRetentionAccepted: () -> Unit,
    subscriptionType: SubscriptionType,
    currentPrice: String,
    retentionPrice: String?,
    analyticsTracker: AnalyticsTracker,
    context: Context,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var offerShownTracked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analyticsTracker.trackChurnFlowOpened()
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 2 && !offerShownTracked) {
            analyticsTracker.trackChurnOfferShown()
            offerShownTracked = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(tween(350)) { it / 2 } + fadeIn(tween(350)) togetherWith
                            slideOutHorizontally(tween(350)) { -it / 2 } + fadeOut(tween(250))
                    } else {
                        slideInHorizontally(tween(350)) { -it / 2 } + fadeIn(tween(350)) togetherWith
                            slideOutHorizontally(tween(350)) { it / 2 } + fadeOut(tween(250))
                    }
                },
                label = "churn_step",
            ) { step ->
                when (step) {
                    0 -> StepOverview(
                        subscriptionType = subscriptionType,
                        currentPrice = currentPrice,
                        onCancel = onDismiss,
                        onCancelSubscription = { currentStep = 1 },
                    )
                    1 -> StepReason(
                        selectedReason = selectedReason,
                        onReasonSelected = { selectedReason = it },
                        onNext = {
                            selectedReason?.let { reason ->
                                analyticsTracker.trackChurnReasonSelected(reason)
                                currentStep = 2
                            }
                        },
                        onCancel = { currentStep = 0 },
                    )
                    2 -> StepRetentionOffer(
                        selectedReason = selectedReason ?: "",
                        subscriptionType = subscriptionType,
                        currentPrice = currentPrice,
                        retentionPrice = retentionPrice,
                        onAcceptRetention = {
                            analyticsTracker.trackChurnOfferAccepted()
                            saveChurnOfferAccepted(context)
                            onRetentionAccepted()
                        },
                        onSwitchToYearly = {
                            analyticsTracker.trackChurnOfferAccepted()
                            saveChurnOfferAccepted(context)
                            onSwitchToYearly()
                        },
                        onPauseSubscription = {
                            analyticsTracker.trackChurnOfferAccepted()
                            saveChurnOfferAccepted(context)
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/account/subscriptions"),
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                            onOfferAccepted()
                        },
                        onDecline = { currentStep = 3 },
                    )
                    3 -> StepConfirm(
                        onGoBack = { currentStep = 2 },
                        onConfirmCancel = {
                            analyticsTracker.trackChurnConfirmed()
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/account/subscriptions"),
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                            onCancelConfirmed()
                        },
                    )
                }
            }
        }
    }
}

// ── Step 0: Subscription Overview ────────────────────────────────────────

@Composable
private fun StepOverview(
    subscriptionType: SubscriptionType,
    currentPrice: String,
    onCancel: () -> Unit,
    onCancelSubscription: () -> Unit,
) {
    val isYearly = subscriptionType == SubscriptionType.YEARLY
    val planName = if (isYearly) stringResource(R.string.churn_plan_yearly) else stringResource(R.string.churn_plan_monthly)
    val periodLabel = if (isYearly) stringResource(R.string.churn_per_year) else stringResource(R.string.churn_per_month)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Premium icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            NeonEmerald.copy(alpha = 0.15f),
                            NeonAmber.copy(alpha = 0.1f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            stringResource(R.string.churn_your_sub),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Subscription info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.churn_plan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    planName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.churn_price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$currentPrice $periodLabel",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.churn_renewal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.churn_automatic),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NeonEmerald,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            stringResource(R.string.churn_auto_renew_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Close button (primary — most prominent action)
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.action_done), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cancel link — intentionally very subtle, small, low contrast
        Text(
            text = stringResource(R.string.churn_cancel_sub),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .clickable { onCancelSubscription() }
                .padding(vertical = 4.dp),
        )
    }
}

// ── Step 1: Reason Survey ───────────────────────────────────────────────

@Composable
private fun StepReason(
    selectedReason: String?,
    onReasonSelected: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val reasons = listOf(
        stringResource(R.string.churn_reason_too_expensive),
        stringResource(R.string.churn_reason_unused),
        stringResource(R.string.churn_reason_features),
        stringResource(R.string.churn_reason_other),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.SentimentDissatisfied,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            stringResource(R.string.churn_sorry_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            stringResource(R.string.churn_sorry_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            reasons.forEach { reason ->
                val isSelected = selectedReason == reason
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ) else Modifier
                        )
                        .clickable { onReasonSelected(reason) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onReasonSelected(reason) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            enabled = selectedReason != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Step 1: Beautiful Retention Offer ──────────────────────────────────

@Composable
private fun StepRetentionOffer(
    selectedReason: String,
    subscriptionType: SubscriptionType,
    currentPrice: String,
    retentionPrice: String?,
    onAcceptRetention: () -> Unit,
    onSwitchToYearly: () -> Unit,
    onPauseSubscription: () -> Unit,
    onDecline: () -> Unit,
) {
    // Calculate fallback retention price from current price
    val displayRetentionPrice = retentionPrice ?: run {
        if (subscriptionType == SubscriptionType.YEARLY) Constants.RETENTION_YEARLY_PRICE
        else Constants.RETENTION_MONTHLY_PRICE
    }

    val isYearly = subscriptionType == SubscriptionType.YEARLY
    val periodLabel = if (isYearly) stringResource(R.string.churn_per_year) else stringResource(R.string.churn_per_month)

    // Breathing animation on the CTA button
    val infiniteTransition = rememberInfiniteTransition(label = "retention")
    val ctaScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "retentionCta",
    )

    // Glow animation on the discount badge
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val accentColor = NeonEmerald
    val warmGold = NeonAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Animated gift icon with glow ──
        Box(contentAlignment = Alignment.Center) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = glowAlpha * 0.15f)),
            )
            // Inner circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.2f),
                                warmGold.copy(alpha = 0.15f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocalOffer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            stringResource(R.string.churn_exclusive_offer),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            stringResource(R.string.churn_offer_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Premium retention offer card ──
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            shadowElevation = 12.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.08f),
                                warmGold.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.surface,
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.5f),
                                warmGold.copy(alpha = 0.3f),
                            )
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Discount badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor,
                        shadowElevation = 4.dp,
                    ) {
                        Text(
                            text = stringResource(R.string.churn_discount_badge, Constants.RETENTION_DISCOUNT_PERCENT),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // New price (large)
                    Text(
                        text = displayRetentionPrice,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Old price (strikethrough)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.churn_instead_of),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = currentPrice,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Text(
                            text = " $periodLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Benefits reminder
                    val keepBenefits = listOf(
                        stringResource(R.string.churn_offer_feature_ai),
                        stringResource(R.string.churn_offer_feature_perspectives),
                        stringResource(R.string.churn_offer_feature_voice),
                    )
                    keepBenefits.forEach { benefit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = warmGold,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.churn_offer_auto_renew),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Primary CTA: Accept retention offer ──
        Button(
            onClick = onAcceptRetention,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .scale(ctaScale),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.churn_accept_offer),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }

        // ── Secondary: Switch to yearly (only for monthly subscribers) ──
        if (subscriptionType == SubscriptionType.MONTHLY && selectedReason == "Zu teuer") {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSwitchToYearly, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.churn_switch_yearly),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── Secondary: Pause (for "too seldom" reason) ──
        if (selectedReason == stringResource(R.string.churn_reason_unused)) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onPauseSubscription, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.churn_pause_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.churn_cancel_anyway),
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── Step 2: Final Confirmation ──────────────────────────────────────────

@Composable
private fun StepConfirm(
    onGoBack: () -> Unit,
    onConfirmCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            stringResource(R.string.churn_are_you_sure),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            stringResource(R.string.churn_google_play_redirect),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGoBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.churn_stay), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onConfirmCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_go_google_play), color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Helper ──────────────────────────────────────────────────────────────

private fun saveChurnOfferAccepted(context: Context) {
    try {
        val mk = androidx.security.crypto.MasterKeys.getOrCreate(
            androidx.security.crypto.MasterKeys.AES256_GCM_SPEC,
        )
        val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_PREFS_NAME,
            mk,
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.edit()
            .putBoolean(Constants.PREF_CHURN_OFFER_ACCEPTED, true)
            .putLong(Constants.PREF_CHURN_OFFER_TIMESTAMP, System.currentTimeMillis())
            .apply()
    } catch (_: Exception) {
        // Non-critical: offer acceptance is best-effort
    }
}
