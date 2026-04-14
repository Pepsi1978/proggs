package com.bestjournal.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.rounded.SentimentDissatisfied
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants

@Composable
fun ChurnFlowDialog(
    onDismiss: () -> Unit,
    onOfferAccepted: () -> Unit,
    onCancelConfirmed: () -> Unit,
    onSwitchToYearly: () -> Unit,
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
        if (currentStep == 1 && !offerShownTracked) {
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
                    0 -> StepReason(
                        selectedReason = selectedReason,
                        onReasonSelected = { selectedReason = it },
                        onNext = {
                            selectedReason?.let { reason ->
                                analyticsTracker.trackChurnReasonSelected(reason)
                                currentStep = 1
                            }
                        },
                        onCancel = onDismiss,
                    )
                    1 -> StepOffer(
                        selectedReason = selectedReason ?: "",
                        onAccept = {
                            analyticsTracker.trackChurnOfferAccepted()
                            saveChurnOfferAccepted(context)
                            if (selectedReason == "Zu teuer") {
                                onSwitchToYearly()
                            } else if (selectedReason == "Nutze es zu selten") {
                                // Open Google Play subscription management for pausing
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/account/subscriptions"),
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) { }
                            }
                            onOfferAccepted()
                        },
                        onDecline = { currentStep = 2 },
                    )
                    2 -> StepConfirm(
                        onGoBack = { currentStep = 1 },
                        onConfirmCancel = {
                            analyticsTracker.trackChurnConfirmed()
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/account/subscriptions"),
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // No browser available — graceful degradation
                            }
                            onCancelConfirmed()
                        },
                    )
                }
            }
        }
    }
}

// ── Step 0: Reason Survey ───────────────────────────────────────────────

@Composable
private fun StepReason(
    selectedReason: String?,
    onReasonSelected: (String) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    val reasons = listOf(
        "Zu teuer",
        "Nutze es zu selten",
        "Nicht die Features die ich brauche",
        "Anderer Grund",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Sad face icon in a soft circle
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
            "Schade, dass du gehst",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Bevor du kündigst, würdest du uns verraten warum? " +
                "Das hilft uns, die App zu verbessern.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Radio button list
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
                            if (isSelected) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onReasonSelected(reason) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onReasonSelected(reason) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary: "Weiter" button
        Button(
            onClick = onNext,
            enabled = selectedReason != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                "Weiter",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary: "Abbrechen" button
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Abbrechen",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── Step 1: Retention Offer ─────────────────────────────────────────────

@Composable
private fun StepOffer(
    selectedReason: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val offerText = when (selectedReason) {
        "Zu teuer" ->
            "Du kannst dein Abo \u00fcber Google Play pausieren oder zum g\u00fcnstigeren " +
                "Jahresabo wechseln, das spart rund 37%."
        "Nutze es zu selten" ->
            "Du kannst dein Abo \u00fcber Google Play pausieren. " +
                "Deine Daten bleiben erhalten und du kannst jederzeit wieder einsteigen."
        else ->
            "Wir arbeiten st\u00e4ndig an Verbesserungen. " +
                "Gib uns noch eine Chance, dich zu \u00fcberzeugen!"
    }

    val discountLabel = when (selectedReason) {
        "Zu teuer" -> "Zum Jahresabo wechseln"
        "Nutze es zu selten" -> "Abo pausieren"
        else -> "Noch bleiben"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Gift icon in a gradient circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            NeonEmerald.copy(alpha = 0.15f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.CardGiftcard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Wir haben ein Angebot für dich",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            offerText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Highlight offer card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            NeonEmerald.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = NeonEmerald.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                discountLabel,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = NeonEmerald,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Exklusiv für dich",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Accept button
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonEmerald,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Angebot annehmen",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Decline button
        TextButton(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Trotzdem kündigen",
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
        // Warning icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
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
            "Bist du sicher?",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Du wirst zu Google Play weitergeleitet, " +
                "um dein Abo zu verwalten. " +
                "Deine Tagebucheinträge bleiben natürlich erhalten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Go back to offer
        Button(
            onClick = onGoBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Doch lieber bleiben",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Confirm cancellation
        TextButton(
            onClick = onConfirmCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Zu Google Play",
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
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
