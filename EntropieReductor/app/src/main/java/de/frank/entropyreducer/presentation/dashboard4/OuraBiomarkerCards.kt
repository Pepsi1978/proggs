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
 * Oura-Ring-Karten fuer den Biomarker-Bildschirm.
 *
 * Etappe E (Frank-Vorgabe 2026-05-10): radikal aufgeraeumt. Pro Karte:
 *  - Score (groß) plus Plus/Minus-Trend zum 30-Tage-Durchschnitt
 *  - 7-Tage-Mini-Balkenchart mit Wert IN jedem Balken
 *  - Klick fuehrt zum Detail-Screen mit 30-Tage-Verlauf (Etappe F)
 *
 * RAUS aus den Karten:
 *   - Hauttemperatur-Abweichung (Readiness)
 *   - Tief/REM/Effizienz-Contributors (Sleep-Score)
 *   - Sub-Faktor-Bars (Resilienz)
 *   - 3-Tage-Werte-Liste auf allen Karten
 *   - "Letzte 7 Tage"-Beschriftung
 *   - Diagnose-Texte ("Wie erholt dein Koerper heute ist...")
 *
 * Aktivitaet behaelt Schritte und Aktive Kalorien als Chips, weil Frank
 * diese explizit drinhaben wollte.
 */

private val OuraAccent: Color = CosmosColors.Success

@Composable
private fun OuraSourceLabel(text: String = "Oura Ring") {
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

/** Readiness-Score 0-100 ohne Hauttemperatur-Abweichung. */
@Composable
internal fun OuraReadinessCard(
    readiness: OuraReadinessEntity?,
    history: List<OuraReadinessEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = readiness?.score
    val color = scoreColor(score)
    val last30Scores = history.takeLast(30).mapNotNull { it.score }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            CardHeader(title = "Readiness", color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            ScoreWithTrend(score = score?.toDouble(), color = color, last30 = last30Scores.map { it.toDouble() })
            Spacer(Modifier.height(10.dp))
            HistoryMiniChartWithLabels(
                values = history.takeLast(7).mapNotNull { it.score?.toDouble() },
                accent = color,
                maxValue = 100.0,
                formatLabel = { "%.0f".format(it) },
            )
        }
    }
}

/** Sleep-Score 0-100 ohne Contributors. */
@Composable
internal fun OuraSleepScoreCard(
    sleep: OuraDailySleepEntity?,
    history: List<OuraDailySleepEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = sleep?.score
    val color = scoreColor(score)
    val last30Scores = history.takeLast(30).mapNotNull { it.score }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            CardHeader(title = "Schlaf-Score", color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            ScoreWithTrend(score = score?.toDouble(), color = color, last30 = last30Scores.map { it.toDouble() })
            Spacer(Modifier.height(10.dp))
            HistoryMiniChartWithLabels(
                values = history.takeLast(7).mapNotNull { it.score?.toDouble() },
                accent = color,
                maxValue = 100.0,
                formatLabel = { "%.0f".format(it) },
            )
        }
    }
}

/** Activity-Score plus Schritte/Kalorien (Frank-Vorgabe: bleiben drin). */
@Composable
internal fun OuraActivityCard(
    activity: OuraActivityEntity?,
    history: List<OuraActivityEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = activity?.score
    val color = scoreColor(score)
    val last30Scores = history.takeLast(30).mapNotNull { it.score }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            CardHeader(title = "Aktivität", color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            ScoreWithTrend(score = score?.toDouble(), color = color, last30 = last30Scores.map { it.toDouble() })
            if (activity != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("Schritte", activity.steps?.let { "%,d".format(it).replace(',', '.') } ?: "—")
                    StatChip("Aktive kcal", activity.activeCalories?.toString() ?: "—")
                }
            }
            Spacer(Modifier.height(10.dp))
            HistoryMiniChartWithLabels(
                values = history.takeLast(7).mapNotNull { it.score?.toDouble() },
                accent = color,
                maxValue = 100.0,
                formatLabel = { "%.0f".format(it) },
            )
        }
    }
}

/**
 * Resilienz mit Level (deutsch). KEINE Sub-Faktoren mehr (Frank-Vorgabe).
 * Plus/Minus auf Rang-Basis ueber die letzten 30 Tage.
 */
@Composable
internal fun OuraResilienceCard(
    resilience: OuraResilienceEntity?,
    history: List<OuraResilienceEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val level = resilience?.level
    val germanLabel = levelToGerman(level)
    val color = levelToColor(level, cosmos.textSecondary)
    val rankNumeric: (String?) -> Double? = ::levelToRank
    val last30Ranks = history.takeLast(30).mapNotNull { rankNumeric(it.level) }
    val currentRank = rankNumeric(level)

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            CardHeader(title = "Resilienz", color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = germanLabel,
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (currentRank != null && last30Ranks.size >= 2) {
                    val mean = last30Ranks.average()
                    val delta = currentRank - mean
                    TrendBadge(delta = delta, formatter = { "%+.1f".format(it) })
                }
            }
            // Mini-Balken auf Rang-Basis (0 bis 4) mit gekuerzten Level-Labels
            // unter den Balken — pro Balken steht ein Buchstabe (E/A/S/T/X)
            // damit man den Verlauf sieht ohne dass es ueberladen wirkt.
            val rankValues = history.takeLast(7).mapNotNull { rankNumeric(it.level) }
            if (rankValues.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HistoryMiniChartWithLabels(
                    values = rankValues,
                    accent = color,
                    maxValue = 4.0,
                    formatLabel = { v ->
                        when (v.toInt()) {
                            0 -> "E"
                            1 -> "A"
                            2 -> "S"
                            3 -> "St"
                            4 -> "X"
                            else -> "—"
                        }
                    },
                )
            }
        }
    }
}

/**
 * Schlaf-Phasen-Karte — bleibt fuer Backward-Compat im when, aber nicht mehr
 * in DEFAULT_ORDER. Frank vertraut nur Whoop fuer Schlafphasen.
 */
@Composable
internal fun OuraSleepDetailCard(
    sleepDetails: List<OuraSleepDetailEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val main = sleepDetails.maxByOrNull { it.totalSleepSeconds ?: 0 }
    val deep = main?.deepSeconds ?: 0
    val rem = main?.remSeconds ?: 0
    val light = main?.lightSeconds ?: 0
    val awake = main?.awakeSeconds ?: 0
    val totalInBed = deep + rem + light + awake
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            CardHeader(title = "Schlaf-Phasen (Oura)", color = cosmos.textPrimary)
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

/* --------------------- Wiederverwendbare Helper --------------------- */

@Composable
private fun CardHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.weight(1f),
        )
        OuraSourceLabel()
    }
}

/** Score in grosser Zahl plus Plus/Minus-Badge zum 30-Tage-Durchschnitt. */
@Composable
private fun ScoreWithTrend(
    score: Double?,
    color: Color,
    last30: List<Double>,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = score?.toInt()?.toString() ?: "—",
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
        Spacer(Modifier.weight(1f))
        if (score != null && last30.size >= 2) {
            val avg = last30.average()
            val delta = score - avg
            TrendBadge(delta = delta, formatter = { "%+.1f".format(it) })
        }
    }
}

/**
 * Plus/Minus-Badge mit Farbe und Vergleichstext (besser/schlechter/neutral).
 * Frank-Vorgabe: gruen wenn besser, rot wenn schlechter.
 */
@Composable
private fun TrendBadge(delta: Double, formatter: (Double) -> String) {
    val color = when {
        delta > 0.5 -> CosmosColors.Success
        delta < -0.5 -> CosmosColors.Critical
        else -> CosmosColors.AccentPrimary
    }
    val label = when {
        delta > 0.5 -> "besser"
        delta < -0.5 -> "schlechter"
        else -> "neutral"
    }
    Column(horizontalAlignment = Alignment.End) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = formatter(delta),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "vs. 30-Tage-Mittel",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * Mini-Balkenchart der letzten Werte mit Beschriftung IN/UNTER jedem Balken.
 * Frank-Vorgabe 2026-05-10: Werte in den Balken-Bereich schreiben statt
 * separater 3-Tage-Liste.
 */
@Composable
private fun HistoryMiniChartWithLabels(
    values: List<Double>,
    accent: Color,
    maxValue: Double,
    formatLabel: (Double) -> String,
) {
    val cosmos = LocalCosmos.current
    if (values.isEmpty()) return
    val padded = values.takeLast(7)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Wenn weniger als 7 Werte vorhanden sind, links auffuellen.
        repeat(7 - padded.size) {
            Box(modifier = Modifier.weight(1f).height(54.dp))
        }
        padded.forEach { v ->
            val frac = (v / maxValue).coerceIn(0.0, 1.0).toFloat()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((40.dp * frac).coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(accent.copy(alpha = 0.7f)),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatLabel(v),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                    fontWeight = FontWeight.Medium,
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
private fun StatChip(label: String, value: String) {
    val cosmos = LocalCosmos.current
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, color = cosmos.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
    }
}

/* --------------------- Reine Funktionen (kein @Composable) --------------------- */

internal fun scoreColor(score: Int?): Color = when {
    score == null -> CosmosColors.AccentPrimary
    score >= 85 -> CosmosColors.Success
    score >= 70 -> CosmosColors.AccentPrimary
    score >= 50 -> CosmosColors.Warning
    else -> CosmosColors.Critical
}

internal fun levelToGerman(level: String?): String = when (level) {
    "limited" -> "Eingeschränkt"
    "adequate" -> "Ausreichend"
    "solid" -> "Solide"
    "strong" -> "Stark"
    "exceptional" -> "Außergewöhnlich"
    else -> "—"
}

internal fun levelToColor(level: String?, fallback: Color): Color = when (level) {
    "exceptional", "strong" -> CosmosColors.Success
    "solid" -> CosmosColors.AccentPrimary
    "adequate" -> CosmosColors.Warning
    "limited" -> CosmosColors.Critical
    else -> fallback
}

internal fun levelToRank(level: String?): Double? = when (level) {
    "limited" -> 0.0
    "adequate" -> 1.0
    "solid" -> 2.0
    "strong" -> 3.0
    "exceptional" -> 4.0
    else -> null
}
