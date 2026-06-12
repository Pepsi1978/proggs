package de.frank.entropyreducer.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Status-Balken "Zustand jetzt" — Spec §4.1. Tap oeffnet ein Detail-Sheet mit Aufschluesselung der
 * drei Komponenten.
 */
@Composable
fun StatusBar(
    modifier: Modifier = Modifier,
    percent: Int,
    label: String = "Zustand jetzt",
    breakdown: StatusBreakdown? = null,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val animated by
        animateFloatAsState(
            targetValue = (percent.coerceIn(0, 100) / 100f),
            animationSpec = tween(durationMillis = 600),
            label = "statusBar",
        )
    val cosmos = LocalCosmos.current

    val gradient =
        Brush.horizontalGradient(
            0f to CosmosColors.StatusRed,
            0.33f to CosmosColors.StatusYellow,
            0.66f to CosmosColors.StatusLightGreen,
            1f to CosmosColors.StatusGreen,
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(enabled = breakdown != null) { sheetOpen = true }
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier =
                Modifier.weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cosmos.glassBg)
        ) {
            Box(
                modifier =
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(animated)
                        .clip(RoundedCornerShape(50))
                        .background(gradient)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$percent %",
            color = LocalCosmos.current.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }

    if (sheetOpen && breakdown != null) {
        StatusDetailSheet(breakdown = breakdown, onDismiss = { sheetOpen = false })
    }
}

@Composable
private fun StatusDetailSheet(breakdown: StatusBreakdown, onDismiss: () -> Unit) {
    val cosmos = LocalCosmos.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Zustand jetzt — Aufschlüsselung",
                style = MaterialTheme.typography.titleLarge,
                color = cosmos.textPrimary,
            )
            Text(
                text = "Gesamtwert: ${breakdown.total} %",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = LocalCosmos.current.accent,
            )

            ComponentRow(
                label = "Biomarker (40 %)",
                value = breakdown.biomarkerScore?.let { "$it %" } ?: "Whoop nicht verbunden",
                detail =
                    buildString {
                            breakdown.recoveryScore?.let { append("Recovery $it %  ") }
                            breakdown.hrvMs?.let { append("HRV ${"%.1f".format(it)} ms  ") }
                            breakdown.sleepPerformance?.let { append("Sleep $it %") }
                        }
                        .trim(),
            )
            ComponentRow(
                label = "Aufgaben-Reduktion (35 %)",
                value = "${breakdown.tasksScore} %",
                detail =
                    "${breakdown.openEntryCount} offen, ${breakdown.resolvedTodayCount} heute reduziert",
            )
            ComponentRow(
                label = "Kontext (25 %)",
                value = "${breakdown.contextScore} %",
                detail = "Heute: ${shiftLabel(breakdown.shiftCode)}",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    "Berechnungslogik gemäß Spec §4.1. Der Wert wird alle paar Minuten " +
                        "neu berechnet sowie sofort nach jeder Status-Änderung eines Eintrags.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

@Composable
private fun ComponentRow(label: String, value: String, detail: String) {
    val cosmos = LocalCosmos.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = cosmos.textPrimary, style = MaterialTheme.typography.titleMedium)
            Text(value, color = LocalCosmos.current.accent, fontWeight = FontWeight.SemiBold)
        }
        if (detail.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(detail, color = cosmos.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun shiftLabel(shift: ShiftCode): String =
    when (shift) {
        ShiftCode.TAGDIENST -> "Tagdienst"
        ShiftCode.NACHTDIENST -> "Nachtdienst"
        ShiftCode.FREI -> "Frei"
        ShiftCode.URLAUB -> "Urlaub"
        ShiftCode.UNBEKANNT -> "Unbekannt"
    }

/** Variante als Ring für Recovery-Score (Dashboard 4). */
@Composable
fun RingPlaceholder(score: Int, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Box(
        modifier = modifier.clip(RoundedCornerShape(50)).background(cosmos.glassBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$score",
            color = LocalCosmos.current.ok,
            style = MaterialTheme.typography.displayLarge,
        )
    }
}
