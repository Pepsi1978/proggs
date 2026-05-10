package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Oura-Ring-Karten fuer den Biomarker-Bildschirm (Frank-Wunsch 2026-05-10).
 * Frank hat sechs Endpunkte ausgewaehlt: daily_readiness, daily_sleep,
 * daily_activity, daily_resilience, sleep (Detail), personal_info. Personal
 * Info wird hier nicht als eigene Karte angezeigt sondern subtil in der
 * Readiness-Karte eingebaut (Quelle-Label). Fuenf Karten:
 *
 *  1. OuraReadinessCard   — Readiness-Score + Hauttemperatur-Abweichung
 *  2. OuraSleepScoreCard  — Sleep-Score + Top-Contributors
 *  3. OuraActivityCard    — Activity-Score + Schritte + Kalorien
 *  4. OuraResilienceCard  — Resilience-Level + Sub-Faktoren
 *  5. OuraSleepDetailCard — Schlaf-Phasen-Verteilung der letzten Nacht
 *
 * Designprinzip: gleiche GlassCard-Optik wie Whoop/Amazfit, aber alle Texte
 * mit "Oura Ring"-Label damit die Quelle klar ist. Farb-Akzent: Success-Gruen
 * (Tier 3 — gehoert konzeptionell zur Readiness-Familie).
 */

private val OuraAccent: Color = CosmosColors.Success

@Composable
private fun OuraSourceLabel(text: String = "Oura Ring") {
    val cosmos = LocalCosmos.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OuraAccent.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = OuraAccent,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Karte 1 — Readiness-Score plus Hauttemperatur-Abweichung. Score-Skala
 * 0-100. Farbskala: gruen ab 80, gelb 60-80, orange 40-60, rot unter 40.
 */
@Composable
internal fun OuraReadinessCard(
    readiness: OuraReadinessEntity?,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = readiness?.score
    val color = scoreColor(score)
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Readiness",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                OuraSourceLabel()
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score?.toString() ?: "—",
                    color = color,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (score != null) " /100" else "",
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            val tempDelta = readiness?.temperatureDeviation
            Text(
                text = if (tempDelta != null) {
                    val sign = if (tempDelta >= 0) "+" else ""
                    "Hauttemperatur: $sign${"%.2f".format(tempDelta)} °C vom Mittel"
                } else {
                    "Noch keine Daten"
                },
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            if (readiness != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Wie erholt dein Koerper heute ist (HRV, Ruhepuls, Schlaf, Aktivitaet).",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

/**
 * Karte 2 — Tages-Sleep-Score plus die drei staerksten Contributors
 * (Tiefschlaf, REM, Effizienz).
 */
@Composable
internal fun OuraSleepScoreCard(
    sleep: OuraDailySleepEntity?,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = sleep?.score
    val color = scoreColor(score)
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Schlaf-Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                OuraSourceLabel()
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score?.toString() ?: "—",
                    color = color,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (score != null) " /100" else "",
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (sleep != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContributorChip("Tief", sleep.deepSleepScore)
                    ContributorChip("REM", sleep.remSleepScore)
                    ContributorChip("Effizienz", sleep.efficiencyScore)
                }
            } else {
                Text(
                    "Noch keine Daten",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

/**
 * Karte 3 — Activity-Score plus Schrittzahl und aktive Kalorien.
 */
@Composable
internal fun OuraActivityCard(
    activity: OuraActivityEntity?,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = activity?.score
    val color = scoreColor(score)
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aktivitaet",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                OuraSourceLabel()
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score?.toString() ?: "—",
                    color = color,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (score != null) " /100" else "",
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (activity != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("Schritte", activity.steps?.let { "%,d".format(it).replace(',', '.') } ?: "—")
                    StatChip("Aktive kcal", activity.activeCalories?.toString() ?: "—")
                }
            } else {
                Text(
                    "Noch keine Daten",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

/**
 * Karte 4 — Resilience-Level (Text statt Zahl). Levels: limited, adequate,
 * solid, strong, exceptional. Wird ins Deutsche uebersetzt fuer das UI.
 */
@Composable
internal fun OuraResilienceCard(
    resilience: OuraResilienceEntity?,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val level = resilience?.level
    val germanLabel = when (level) {
        "limited" -> "Eingeschraenkt"
        "adequate" -> "Ausreichend"
        "solid" -> "Solide"
        "strong" -> "Stark"
        "exceptional" -> "Aussergewoehnlich"
        else -> "—"
    }
    val color = when (level) {
        "exceptional" -> CosmosColors.Success
        "strong" -> CosmosColors.Success
        "solid" -> CosmosColors.AccentPrimary
        "adequate" -> CosmosColors.Warning
        "limited" -> CosmosColors.Critical
        else -> cosmos.textSecondary
    }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Resilienz",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                OuraSourceLabel()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = germanLabel,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Wie gut dein Koerper langfristig mit Stress umgeht.",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/**
 * Karte 5 — Schlaf-Phasen-Detail der letzten Nacht. Zeigt eine horizontale
 * Stapel-Bar mit Tief / REM / Leicht / Wach plus Minuten pro Phase.
 */
@Composable
internal fun OuraSleepDetailCard(
    sleepDetails: List<OuraSleepDetailEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // Bei Naps gibt es mehrere Sessions pro Tag — wir nehmen die laengste
    // (vermutlich die Hauptnacht).
    val main = sleepDetails.maxByOrNull { it.totalSleepSeconds ?: 0 }
    val deep = main?.deepSeconds ?: 0
    val rem = main?.remSeconds ?: 0
    val light = main?.lightSeconds ?: 0
    val awake = main?.awakeSeconds ?: 0
    val totalInBed = deep + rem + light + awake
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Schlaf-Phasen",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                OuraSourceLabel()
            }
            Spacer(Modifier.height(10.dp))
            if (totalInBed > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(cosmos.glassBorder.copy(alpha = 0.3f)),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SleepPhaseSegment(deep, totalInBed, Color(0xFF6366F1))
                        SleepPhaseSegment(rem, totalInBed, CosmosColors.AccentSecondary)
                        SleepPhaseSegment(light, totalInBed, CosmosColors.AccentPrimary)
                        SleepPhaseSegment(awake, totalInBed, CosmosColors.Warning)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PhaseLegend("Tief", deep / 60, Color(0xFF6366F1))
                    PhaseLegend("REM", rem / 60, CosmosColors.AccentSecondary)
                    PhaseLegend("Leicht", light / 60, CosmosColors.AccentPrimary)
                    PhaseLegend("Wach", awake / 60, CosmosColors.Warning)
                }
                main?.averageHrv?.let { hrv ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Durchschnittliche HRV im Schlaf: $hrv ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                Text(
                    "Noch keine Schlafphasen-Daten",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SleepPhaseSegment(
    seconds: Int,
    total: Int,
    color: Color,
) {
    if (seconds <= 0) return
    Box(
        modifier = Modifier
            .weight(seconds.toFloat() / total.toFloat())
            .background(color),
    )
}

@Composable
private fun PhaseLegend(label: String, minutes: Int, color: Color) {
    val cosmos = LocalCosmos.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textPrimary)
        Text("${minutes} min", style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
    }
}

@Composable
private fun ContributorChip(label: String, score: Int?) {
    val cosmos = LocalCosmos.current
    val color = scoreColor(score)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            "$label ${score ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    val cosmos = LocalCosmos.current
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, color = cosmos.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
    }
}

private fun scoreColor(score: Int?): Color = when {
    score == null -> CosmosColors.AccentPrimary
    score >= 85 -> CosmosColors.Success
    score >= 70 -> CosmosColors.AccentPrimary
    score >= 50 -> CosmosColors.Warning
    else -> CosmosColors.Critical
}
