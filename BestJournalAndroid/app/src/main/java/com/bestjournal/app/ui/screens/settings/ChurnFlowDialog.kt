package com.bestjournal.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bestjournal.app.R
import com.bestjournal.app.billing.SubscriptionType
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    promoInfo: SettingsViewModel.PromoInfo? = null,
    autoRenewing: Boolean = true,
    expiryTime: String? = null,
    // Loop-9 (Frank, 2026-04-30): suppress the "25 % sparen" retention
    // step when the user has already accepted that very offer in a
    // previous churn cycle. Re-offering "2,99 € statt 2,99 €" reads as
    // a bug to the user and takes them back to a screen they have no
    // sensible action on.
    isAlreadyOnRetentionPlan: Boolean = false,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedReason by remember { mutableStateOf<String?>(null) }
    // Loop-11 (Frank, 2026-04-30): free-text reason for "Anderer Grund".
    // Held at the dialog level so it survives across step transitions
    // (going back from "Bist du sicher?" to step 1 keeps what the user
    // already typed).
    var customReasonText by remember { mutableStateOf("") }
    var offerShownTracked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { analyticsTracker.trackChurnFlowOpened() }

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
                        slideInHorizontally(tween(350)) { -it / 2 } +
                            fadeIn(tween(350)) togetherWith
                            slideOutHorizontally(tween(350)) { it / 2 } + fadeOut(tween(250))
                    }
                },
                label = "churn_step",
            ) { step ->
                when (step) {
                    0 ->
                        StepOverview(
                            subscriptionType = subscriptionType,
                            currentPrice = currentPrice,
                            onCancel = onDismiss,
                            onCancelSubscription = { currentStep = 1 },
                            promoInfo = promoInfo,
                            autoRenewing = autoRenewing,
                            expiryTime = expiryTime,
                        )
                    1 -> {
                        val reasonOtherLabel =
                            stringResource(R.string.churn_reason_other)
                        StepReason(
                            selectedReason = selectedReason,
                            onReasonSelected = { selectedReason = it },
                            onNext = {
                                selectedReason?.let { reason ->
                                    // Loop-11: when the user picks
                                    // "Anderer Grund", forward the
                                    // free-text contents alongside the
                                    // category so the Firebase analytics
                                    // event captures the actual cause
                                    // rather than the literal string
                                    // "Anderer Grund".
                                    val trackedReason =
                                        if (reason == reasonOtherLabel &&
                                            customReasonText.isNotBlank()
                                        ) {
                                            "$reason: ${customReasonText.trim()}"
                                        } else {
                                            reason
                                        }
                                    analyticsTracker.trackChurnReasonSelected(trackedReason)
                                    // Loop-9: skip the "25 % sparen" step
                                    // when the user is already on the
                                    // retention plan — go straight to
                                    // the final cancel confirmation.
                                    currentStep = if (isAlreadyOnRetentionPlan) 3 else 2
                                }
                            },
                            onCancel = { currentStep = 0 },
                            customReasonText = customReasonText,
                            onCustomReasonChanged = { customReasonText = it },
                        )
                    }
                    2 ->
                        StepRetentionOffer(
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
                                // Loop-9 audit (Frank, 2026-04-30): pause is
                                // not the same as accepting the retention
                                // offer — Google Play handles it as a
                                // payment-suspension on the SAME plan.
                                // Tracking it as "offer accepted" was a
                                // straight-up mislabel and skewed the churn
                                // analytics. Use the dedicated event.
                                analyticsTracker.trackChurnPaused()
                                saveChurnOfferAccepted(context)
                                try {
                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://play.google.com/store/account/subscriptions"
                                            ),
                                        )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                                onOfferAccepted()
                            },
                            onDecline = { currentStep = 3 },
                        )
                    3 ->
                        StepConfirm(
                            // Loop-9: when the user is already on the
                            // retention plan, "Doch lieber bleiben"
                            // closes the dialog completely. They have
                            // no sensible target screen — stepping back
                            // to the retention offer (2,99 € statt
                            // 2,99 €) makes no sense, and stepping back
                            // to the reason picker makes them fight the
                            // flow they no longer want to be in.
                            onGoBack = {
                                if (isAlreadyOnRetentionPlan) {
                                    onDismiss()
                                } else {
                                    currentStep = 2
                                }
                            },
                            onConfirmCancel = {
                                analyticsTracker.trackChurnConfirmed()
                                try {
                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://play.google.com/store/account/subscriptions"
                                            ),
                                        )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
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
    promoInfo: SettingsViewModel.PromoInfo? = null,
    autoRenewing: Boolean = true,
    expiryTime: String? = null,
) {
    val isYearly = subscriptionType == SubscriptionType.YEARLY
    val planName =
        if (isYearly) stringResource(R.string.churn_plan_yearly)
        else stringResource(R.string.churn_plan_monthly)
    val periodLabel =
        if (isYearly) stringResource(R.string.churn_per_year)
        else stringResource(R.string.churn_per_month)
    // Loop-7: format the cloud's ISO-8601 expiryTime into a localised date+time
    // string so the user sees "30.04.2026, 18:13" instead of an opaque code.
    // Returns null if the timestamp is missing or unparseable so the caller
    // can hide the row gracefully.
    val expiryFormatted = remember(expiryTime) { formatExpiryDateTime(expiryTime) }
    val phaseEndFormatted = remember(promoInfo?.phaseEndDate) {
        formatExpiryDateTime(promoInfo?.phaseEndDate)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Premium icon
        Box(
            modifier =
                Modifier.size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonEmerald.copy(alpha = 0.15f), NeonAmber.copy(alpha = 0.1f))
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
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(20.dp)
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
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
                val displayedPrice = promoInfo?.currentPrice ?: currentPrice
                Text(
                    "$displayedPrice $periodLabel",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Loop-7: promo notice now reads directly from Google's offerPhase
            // signal — "Sonderpreis aktiv. Naechster Abrechnungstermin am
            // [Datum] zu [Sonderpreis], danach [Standardpreis]". No counter,
            // no guessing — exactly what Google reports right now.
            if (promoInfo != null && phaseEndFormatted != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        stringResource(R.string.churn_promo_active_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.churn_promo_phase_ends,
                            phaseEndFormatted,
                            promoInfo.baseAfterwardsPrice,
                        ),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NeonAmber,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    stringResource(R.string.churn_renewal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
                if (autoRenewing) {
                    // Loop-7: show the exact next-renewal date when we have it
                    // ("Verlaengert sich am 30.04.2026, 18:13"). Falls back to
                    // the plain "Automatisch" label if the cloud has not yet
                    // delivered the expiryTime.
                    Text(
                        text = if (expiryFormatted != null) {
                            stringResource(R.string.churn_renews_on, expiryFormatted)
                        } else {
                            stringResource(R.string.churn_automatic)
                        },
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = NeonEmerald,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Cancelled — show the exact end-of-service date.
                    Text(
                        text = if (expiryFormatted != null) {
                            stringResource(R.string.churn_cancelled_ends_on, expiryFormatted)
                        } else {
                            stringResource(R.string.churn_cancelled)
                        },
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
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
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                stringResource(R.string.action_done),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cancel link — intentionally subtle, but readable (alpha 0.8f per Frank-Wahl Option [3])
        Text(
            text = stringResource(R.string.churn_cancel_sub),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.clickable { onCancelSubscription() }.padding(vertical = 4.dp),
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
    customReasonText: String,
    onCustomReasonChanged: (String) -> Unit,
) {
    val reasonTooExpensive = stringResource(R.string.churn_reason_too_expensive)
    val reasonUnused = stringResource(R.string.churn_reason_unused)
    val reasonFeatures = stringResource(R.string.churn_reason_features)
    val reasonOther = stringResource(R.string.churn_reason_other)
    val reasons = listOf(reasonTooExpensive, reasonUnused, reasonFeatures, reasonOther)
    // Loop-11 (Frank, 2026-04-30): "Anderer Grund" without a text input
    // is just a black hole — the analytics dashboard would show 80% of
    // churn reasons as the literal string "Anderer Grund" with no clue
    // about the actual cause. Adding a free-text field surfaces it.
    val isOther = selectedReason == reasonOther

    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
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
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected)
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.25f
                                        )
                                    )
                                else Modifier
                            )
                            .clickable { onReasonSelected(reason) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onReasonSelected(reason) },
                        colors =
                            RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Loop-11: free-text input for "Anderer Grund". Animated visibility
        // so the layout stays compact when the user picks one of the
        // pre-defined reasons.
        if (isOther) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = customReasonText,
                onValueChange = onCustomReasonChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(R.string.churn_reason_other_placeholder))
                },
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Loop-11: continue is only enabled once the user has provided
        // enough information — pre-defined reason picks are fine on
        // their own, "Anderer Grund" needs at least a few characters of
        // free text so the analytics event has something useful in it.
        val canContinue =
            selectedReason != null && (!isOther || customReasonText.trim().length >= 3)

        Button(
            onClick = onNext,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                stringResource(R.string.action_continue),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.action_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
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
    // Use the live Play price; if it is missing, show a neutral placeholder
    // instead of a hardcoded EUR amount (matching the main paywall).
    val displayRetentionPrice = retentionPrice ?: "…"

    val isYearly = subscriptionType == SubscriptionType.YEARLY
    val periodLabel =
        if (isYearly) stringResource(R.string.churn_per_year)
        else stringResource(R.string.churn_per_month)

    // Breathing animation on the CTA button
    val infiniteTransition = rememberInfiniteTransition(label = "retention")
    val ctaScale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "retentionCta",
        )

    // Glow animation on the discount badge
    val glowAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glowPulse",
        )

    val accentColor = NeonEmerald
    val warmGold = NeonAmber

    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Animated gift icon with glow ──
        Box(contentAlignment = Alignment.Center) {
            // Glow ring
            Box(
                modifier =
                    Modifier.size(88.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = glowAlpha * 0.15f))
            )
            // Inner circle
            Box(
                modifier =
                    Modifier.size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.2f), warmGold.copy(alpha = 0.15f))
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
                modifier =
                    Modifier.fillMaxWidth()
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
                            brush =
                                Brush.linearGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.5f),
                                        warmGold.copy(alpha = 0.3f),
                                    )
                                ),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(24.dp)
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
                            text =
                                stringResource(
                                    R.string.churn_discount_badge,
                                    Constants.RETENTION_DISCOUNT_PERCENT,
                                ),
                            style =
                                MaterialTheme.typography.labelLarge.copy(
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
                        style =
                            MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
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
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough
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
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Benefits reminder
                    val keepBenefits =
                        listOf(
                            stringResource(R.string.churn_offer_feature_ai),
                            stringResource(R.string.churn_offer_feature_perspectives),
                            stringResource(R.string.churn_offer_feature_voice),
                        )
                    keepBenefits.forEach { benefit ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
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
            modifier = Modifier.fillMaxWidth().height(54.dp).scale(ctaScale),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.churn_accept_offer),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }

        // ── Secondary: Switch to yearly (only for monthly subscribers) ──
        if (
            subscriptionType == SubscriptionType.MONTHLY &&
                selectedReason == stringResource(R.string.churn_reason_too_expensive)
        ) {
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
private fun StepConfirm(onGoBack: () -> Unit, onConfirmCancel: () -> Unit) {
    // Loop-7 (Frank, 2026-04-30): replace the abrupt red warning icon with
    // a softly pulsing red heart — visually says "stay with us" instead
    // of "danger ahead". The colour stays a saturated red because that is
    // the universal love/affection cue; the breathing scale keeps the
    // moment warm without demanding attention.
    val infiniteTransition = rememberInfiniteTransition(label = "stayHeart")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "stayHeartScale",
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = NeonRed,
                modifier = Modifier.size(40.dp).scale(heartScale),
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
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 28.dp,
                vertical = 10.dp,
            ),
        ) {
            Text(
                stringResource(R.string.churn_stay),
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onConfirmCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 28.dp,
                vertical = 10.dp,
            ),
        ) {
            Text(
                stringResource(R.string.action_go_google_play),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

// ── Helper ──────────────────────────────────────────────────────────────

private fun saveChurnOfferAccepted(context: Context) {
    try {
        val prefs = com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
        prefs
            .edit()
            .putBoolean(Constants.PREF_CHURN_OFFER_ACCEPTED, true)
            .putLong(Constants.PREF_CHURN_OFFER_TIMESTAMP, System.currentTimeMillis())
            .apply()
    } catch (_: Exception) {
        // Non-critical: offer acceptance is best-effort
    }
}

/**
 * Loop-7: parse the cloud function's ISO-8601 expiryTime (UTC, e.g.
 * "2026-04-30T18:13:54.483Z") and format it in the user's local timezone
 * as "30.04.2026, 18:13". Returns null when the input is missing or
 * unparseable so the UI can fall back to a plain text label gracefully.
 */
private fun formatExpiryDateTime(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date = parser.parse(iso) ?: return null
        val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault()
        formatter.format(date)
    } catch (_: Exception) {
        // Some payloads may use the second-precision form ("...:54Z") without
        // milliseconds — try a fallback parser before giving up.
        try {
            val fallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            fallback.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date = fallback.parse(iso) ?: return null
            val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault()
            formatter.format(date)
        } catch (_: Exception) {
            null
        }
    }
}
