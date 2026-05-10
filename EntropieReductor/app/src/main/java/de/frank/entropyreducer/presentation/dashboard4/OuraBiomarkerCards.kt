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
 *
 * Etappe D (2026-05-10): Karten erweitert um Historie-Visualisierung —
 *   - 7-Tage-Mini-Balkenchart der letzten Werte
 *   - Plus/Minus-Indikator zum 30-Tage-Durchschnitt mit Farbe (gruen besser, rot schlechter)
 *   - Letzte 3 Tageswerte als kompakte Sub-Zeile
 * Plus Resilienz erweitert um Sub-Faktoren (Sleep Recovery, Daytime Recovery, Stress).
 * Sleep-Detail-Karte ist aus DEFAULT_ORDER entfernt — Frank traut nur Whoop fuer
 * Schlafphasen. Composable bleibt drin fuer Backward-Compat falls bestehende User
 * die Karte schon hatten.
 *
 * Designprinzip: gleiche GlassCard-Optik wie Whoop/Amazfit, aber alle Texte
 * mit "Oura Ring"-Label damit die Quelle klar ist. Farb-Akzent: Success-Gruen.
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

/**
 * Karte 1 — Readiness-Score plus Hauttemperatur-Abweichung. Score 0-100.
 */
@Composable
internal fun OuraReadinessCard(
    readiness: OuraReadinessEntity?,
    history: List<OuraReadinessEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = readiness?.score
    val color = scoreColor(score)
    val scores = history.mapNotNull { it.score }
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
            ScoreWithTrend(score = score?.toDouble(), color = color, allValues = scores.map { it.toDouble() })
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
            Spacer(Modifier.height(10.dp))
            HistoryMiniChart(values = scores.map { it.toDouble() }, accent = color)
            Spacer(Modifier.height(8.dp))
            LastDaysRow(history.takeLast(3).reversed().map { it.day to it.score?.toString() })
        }
    }
}

/**
 * Karte 2 — Tages-Sleep-Score plus drei staerkste Contributors plus Verlauf.
 */
@Composable
internal fun OuraSleepScoreCard(
    sleep: OuraDailySleepEntity?,
    history: List<OuraDailySleepEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = sleep?.score
    val color = scoreColor(score)
    val scores = history.mapNotNull { it.score }
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
            ScoreWithTrend(score = score?.toDouble(), color = color, allValues = scores.map { it.toDouble() })
            Spacer(Modifier.height(8.dp))
            if (sleep != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContributorChip("Tief", sleep.deepSleepScore)
                    ContributorChip("REM", sleep.remSleepScore)
                    ContributorChip("Effizienz", sleep.efficiencyScore)
                }
            }
            Spacer(Modifier.height(10.dp))
            HistoryMiniChart(values = scores.map { it.toDouble() }, accent = color)
            Spacer(Modifier.height(8.dp))
            LastDaysRow(history.takeLast(3).reversed().map { it.day to it.score?.toString() })
        }
    }
}

/**
 * Karte 3 — Activity-Score plus Schritte/Kalorien plus Verlauf. Frank-Korrektur
 * 2026-05-10: heisst "Aktivität" mit Umlaut, NICHT "Aktivitaet".
 */
@Composable
internal fun OuraActivityCard(
    activity: OuraActivityEntity?,
    history: List<OuraActivityEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val score = activity?.score
    val color = scoreColor(score)
    val scores = history.mapNotNull { it.score }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aktivität",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                OuraSourceLabel()
            }
            Spacer(Modifier.height(8.dp))
            ScoreWithTrend(score = score?.toDouble(), color = color, allValues = scores.map { it.toDouble() })
            Spacer(Modifier.height(8.dp))
            if (activity != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("Schritte", activity.steps?.let { "%,d".format(it).replace(',', '.') } ?: "—")
                    StatChip("Aktive kcal", activity.activeCalories?.toString() ?: "—")
                }
            }
            Spacer(Modifier.height(10.dp))
            HistoryMiniChart(values = scores.map { it.toDouble() }, accent = color)
            Spacer(Modifier.height(8.dp))
            LastDaysRow(history.takeLast(3).reversed().map { it.day to it.score?.toString() })
        }
    }
}

/**
 * Karte 4 — Resilienz mit Level (deutsch) plus drei Sub-Faktoren als Mini-Bars
 * plus Verlauf. Levels werden ins Deutsche uebersetzt.
 *
 * Sub-Faktoren (alle 0-1, hoeher ist mehr):
 *   - Sleep Recovery: wie gut die Naechte bei der Erholung mithalten
 *   - Daytime Recovery: wie gut Pausen am Tag die Erholung unterstuetzen
 *   - Stress: tagsueber gemessene Stress-Belastung (hier: hoeher = mehr Stress)
 */
@Composable
internal fun OuraResilienceCard(
    resilience: OuraResilienceEntity?,
    history: List<OuraResilienceEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val level = resilience?.level
    val germanLabel = when (level) {
        "limited" -> "Eingeschränkt"
        "adequate" -> "Ausreichend"
        "solid" -> "Solide"
        "strong" -> "Stark"
        "exceptional" -> "Außergewöhnlich"
        else -> "—"
    }
    val color = when (level) {
        "exceptional", "strong" -> CosmosColors.Success
        "solid" -> CosmosColors.AccentPrimary
        "adequate" -> CosmosColors.Warning
        "limited" -> CosmosColors.Critical
        else -> cosmos.textSecondary
    }
    // Trend auf Basis des Level-Rangs (0 = limited bis 4 = exceptional).
    val rankNumeric: (String?) -> Double? = { lvl ->
        when (lvl) {
            "limited" -> 0.0
            "adequate" -> 1.0
            "solid" -> 2.0
            "strong" -> 3.0
            "exceptional" -> 4.0
            else -> null
        }
    }
    val rankValues = history.mapNotNull { rankNumeric(it.level) }
    val currentRank = rankNumeric(level)

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = germanLabel,
                    color = color,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (currentRank != null && rankValues.size >= 2) {
                    val mean = rankValues.average()
                    val delta = currentRank - mean
                    TrendBadge(delta = delta, formatter = { "%+.1f".format(it) })
                }
            }
            Spacer(Modifier.height(10.dp))
            // Sub-Faktoren als Mini-Bars (0-1 Skala).
            if (resilience != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SubFactorBar("Schlaf-Erholung", resilience.sleepRecovery, color)
                    SubFactorBar("Tag-Erholung", resilience.daytimeRecovery, color)
                    SubFactorBar("Stress", resilience.stress, CosmosColors.Critical, higherIsWorse = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Verlauf als 7-Tage-Bars auf Rang-Basis (0-4).
            if (rankValues.isNotEmpty()) {
                HistoryMiniChart(values = rankValues, accent = color, maxValue = 4.0)
                Spacer(Modifier.height(8.dp))
                LastDaysRow(
                    history.takeLast(3).reversed().map {
                        it.day to (when (it.level) {
                            "limited" -> "Eingeschr."
                            "adequate" -> "Ausreich."
                            "solid" -> "Solide"
                            "strong" -> "Stark"
                            "exceptional" -> "Außerg."
                            else -> "—"
                        })
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Wie gut dein Körper langfristig mit Stress umgeht.",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/**
 * Karte 5 — Schlaf-Phasen-Detail (NICHT MEHR in DEFAULT_ORDER, Frank-Wunsch
 * 2026-05-10: vertraut nur Whoop fuer Schlafphasen). Composable bleibt drin
 * damit bestehende User die die Karte schon hatten weiterhin etwas sehen.
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

/* ----------------- Wiederverwendbare Helper-Composables ----------------- */

/**
 * Score in grosser Zahl plus Plus/Minus-Badge zum 30-Tage-Durchschnitt.
 * `allValues` = vollstaendige Historie ueber die der Mittelwert gebildet wird.
 */
@Composable
private fun ScoreWithTrend(
    score: Double?,
    color: Color,
    allValues: List<Double>,
) {
    val cosmos = LocalCosmos.current
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
        if (score != null && allValues.size >= 2) {
            val avg = allValues.average()
            val delta = score - avg
            TrendBadge(delta = delta, formatter = { "%+.1f".format(it) })
        }
    }
}

/**
 * Plus/Minus-Badge mit Farbe. Positiv ist gruen (besser), negativ rot
 * (schlechter), nahe Null grau (kein Trend).
 *
 * `formatter` formatiert die Differenz selbst — manche Karten wollen %+.1f,
 * andere %+.2f.
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
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * Mini-Balkenchart der letzten 7 Werte. Hoehe der Balken proportional zum
 * Wert (0..maxValue). Wird unter dem Score auf jeder Karte angezeigt.
 */
@Composable
private fun HistoryMiniChart(
    values: List<Double>,
    accent: Color,
    maxValue: Double = 100.0,
) {
    val cosmos = LocalCosmos.current
    val last7 = values.takeLast(7)
    if (last7.isEmpty()) return
    Column {
        Text(
            text = "Letzte 7 Tage",
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Wenn weniger als 7 Werte vorhanden sind, fuellen wir links mit
            // leeren Plaetzen — so bleibt der juengste Wert immer rechts.
            repeat(7 - last7.size) {
                Box(modifier = Modifier.weight(1f).height(40.dp))
            }
            last7.forEach { v ->
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
                }
            }
        }
    }
}

/**
 * Letzte 3 Tage als kompakte Werte-Liste. Format: "10.05 78  09.05 72  08.05 81".
 * Wenn weniger als 3 Werte: zeigt was da ist.
 */
@Composable
private fun LastDaysRow(entries: List<Pair<String, String?>>) {
    val cosmos = LocalCosmos.current
    if (entries.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.forEach { (day, value) ->
            // YYYY-MM-DD zu DD.MM kuerzen.
            val shortDay = if (day.length == 10) "${day.substring(8, 10)}.${day.substring(5, 7)}" else day
            Column {
                Text(shortDay, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
                Text(
                    value ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Sub-Faktor-Bar fuer die Resilience-Karte. Werte 0..1, Bar fuellt sich von
 * links nach rechts. `higherIsWorse` invertiert die Farb-Logik (Stress: hoch = rot).
 */
@Composable
private fun SubFactorBar(
    label: String,
    value: Double?,
    accent: Color,
    higherIsWorse: Boolean = false,
) {
    val cosmos = LocalCosmos.current
    val v = value?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f
    val color = if (higherIsWorse) {
        when {
            v > 0.7f -> CosmosColors.Critical
            v > 0.4f -> CosmosColors.Warning
            else -> CosmosColors.Success
        }
    } else {
        accent
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary, modifier = Modifier.weight(1f))
            Text(
                value?.let { "%.0f %%".format(it * 100) } ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textPrimary,
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(cosmos.glassBorder.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(v)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
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
