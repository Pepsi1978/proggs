package de.frank.entropyreducer.presentation.dashboard4

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import de.frank.entropyreducer.presentation.components.charts.MiniBarsCanvas
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.CosmosColors

/**
 * Oura-Ring-Karten fuer den Biomarker-Bildschirm.
 *
 * Etappe E (Frank-Vorgabe 2026-05-10): radikal aufgeraeumt. Pro Karte:
 * - Score (groß) plus Plus/Minus-Trend zum 30-Tage-Durchschnitt
 * - 7-Tage-Mini-Balkenchart mit Wert IN jedem Balken
 * - Klick fuehrt zum Detail-Screen mit 30-Tage-Verlauf (Etappe F)
 *
 * RAUS aus den Karten:
 * - Hauttemperatur-Abweichung (Readiness)
 * - Tief/REM/Effizienz-Contributors (Sleep-Score)
 * - Sub-Faktor-Bars (Resilienz)
 * - 3-Tage-Werte-Liste auf allen Karten
 * - "Letzte 7 Tage"-Beschriftung
 * - Diagnose-Texte ("Wie erholt dein Koerper heute ist...")
 *
 * Aktivitaet behaelt Schritte und Aktive Kalorien als Chips, weil Frank diese explizit drinhaben
 * wollte.
 */
private val OuraAccent: Color
    @Composable get() = CosmosColors.Success

@Composable
private fun OuraSourceLabel(text: String = "Oura Ring") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
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
    selectedDate: java.time.LocalDate,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val today = java.time.LocalDate.now()
    val effective =
        remember(readiness, history, selectedDate) {
            readiness ?: if (selectedDate == today) history.maxByOrNull { it.day } else null
        }
    val score = effective?.score

    val derived =
        remember(history) {
            ouraScoreDerived(history.mapNotNull { it.score?.toDouble() })
        }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Readiness",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Oura Ring",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                val headerColor = score?.let { scoreBarColor(it.toDouble(), derived.avgAll) } ?: cosmos.accent
                Text(
                    text = score?.let { "$it" } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            OuraScoreBars(values = derived.last30, avg = derived.avgAll)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Durchschnitt: ${derived.avgAll?.let { "%.1f".format(it) } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    OuraTrendBadge(delta = derived.deltaVsAvg)
                }
            }
            Text(
                text = "Tippen fuer komplette Historie",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary.copy(alpha = 0.6f),
            )
        }
    }
}

/** Sleep-Score 0-100 ohne Contributors. */
@Composable
internal fun OuraSleepScoreCard(
    sleep: OuraDailySleepEntity?,
    history: List<OuraDailySleepEntity>,
    selectedDate: java.time.LocalDate,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val today = java.time.LocalDate.now()
    val effective =
        remember(sleep, history, selectedDate) {
            sleep ?: if (selectedDate == today) history.maxByOrNull { it.day } else null
        }
    val score = effective?.score

    val derived =
        remember(history) {
            ouraScoreDerived(history.mapNotNull { it.score?.toDouble() })
        }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schlaf-Score",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Oura Ring",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                val headerColor = score?.let { scoreBarColor(it.toDouble(), derived.avgAll) } ?: cosmos.accent
                Text(
                    text = score?.let { "$it" } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            OuraScoreBars(values = derived.last30, avg = derived.avgAll)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Durchschnitt: ${derived.avgAll?.let { "%.1f".format(it) } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    OuraTrendBadge(delta = derived.deltaVsAvg)
                }
            }
            Text(
                text = "Tippen fuer komplette Historie",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * "Stand: gestern (12.05.)" — Hinweis dass der angezeigte Wert NICHT vom ausgewaehlten Tag stammt
 * sondern aus der Historie kommt. Verhindert Verwirrung bei Sync-Delay (Oura: 40-50 Min nach
 * Ring-Sync).
 */
@Composable
private fun StaleDataLabel(entryDay: String?, selectedDate: java.time.LocalDate) {
    if (entryDay == null) return
    val cosmos = LocalCosmos.current
    val entryDate = runCatching { java.time.LocalDate.parse(entryDay) }.getOrNull() ?: return
    if (entryDate == selectedDate) return
    val today = java.time.LocalDate.now()
    val label =
        when (entryDate) {
            today -> "Stand: heute"
            today.minusDays(1) -> "Stand: gestern"
            today.minusDays(2) -> "Stand: vorgestern"
            else ->
                "Stand: ${entryDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM."))}"
        }
    Spacer(Modifier.height(2.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
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
    // Performance-Audit (Frank-Wunsch 2026-05-18): siehe OuraSleepScoreCard.
    val aggregates =
        remember(history) {
            val last30AsDouble = history.takeLast(30).mapNotNull { it.score?.toDouble() }
            val last7AsDouble = history.takeLast(7).mapNotNull { it.score?.toDouble() }
            last30AsDouble to last7AsDouble
        }
    val last30Doubles = aggregates.first
    val last7Doubles = aggregates.second
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            CardHeader(title = "Aktivität", color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            ScoreWithTrend(score = score?.toDouble(), color = color, last30 = last30Doubles)
            if (activity != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip(
                        "Schritte",
                        activity.steps?.let { "%,d".format(it).replace(',', '.') } ?: "—",
                    )
                    StatChip("Aktive kcal", activity.activeCalories?.toString() ?: "—")
                }
            }
            Spacer(Modifier.height(10.dp))
            HistoryMiniChartWithLabels(
                values = last7Doubles,
                maxValue = 100.0,
                formatLabel = { "%.0f".format(it) },
                colorFor = { scoreColor(it.toInt()) },
            )
        }
    }
}

/**
 * Resilienz mit Level (deutsch). KEINE Sub-Faktoren mehr (Frank-Vorgabe). Plus/Minus auf Rang-Basis
 * ueber die letzten 30 Tage.
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
    // Performance-Audit (Frank-Wunsch 2026-05-18): rank-Listen aus history nur
    // bei history-Aenderung berechnen, nicht pro Recomposition. Verhindert
    // O(N) bei jedem Scroll-Frame.
    val rankAggregates =
        remember(history) {
            val last30 = history.takeLast(30).mapNotNull { levelToRank(it.level) }
            val last7 = history.takeLast(7).mapNotNull { levelToRank(it.level) }
            last30 to last7
        }
    val last30Ranks = rankAggregates.first
    val currentRank = levelToRank(level)

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
            val rankValues = rankAggregates.second
            if (rankValues.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HistoryMiniChartWithLabels(
                    values = rankValues,
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
                    // Rang-basierte Farbgebung pro Balken: 0=rot, 1=gelb,
                    // 2=blau, 3-4=gruen — wie die levelToColor-Skala.
                    colorFor = { rank ->
                        when (rank.toInt()) {
                            0 -> CosmosColors.Critical
                            1 -> CosmosColors.Warning
                            2 -> CosmosColors.AccentPrimary
                            3,
                            4 -> CosmosColors.Success
                            else -> color
                        }
                    },
                )
            }
        }
    }
}

/**
 * Schlaf-Phasen-Karte — bleibt fuer Backward-Compat im when, aber nicht mehr in DEFAULT_ORDER.
 * Frank vertraut nur Whoop fuer Schlafphasen.
 */
@Composable
internal fun OuraSleepDetailCard(sleepDetails: List<OuraSleepDetailEntity>, onClick: () -> Unit) {
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
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(cosmos.glassBorder.copy(alpha = 0.3f))
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

/**
 * Score in grosser Zahl, rechtsbuendig. Frank-Wunsch 2026-05-18: nur die Zahl ohne "/100", der Wert
 * steht rechts. Hinter dem Wert die Abweichung zum 30-Tage-Mittel als reiner Text ohne
 * Hintergrundfeld — Plus in Gruen, Minus in Rot.
 */
@Composable
private fun ScoreWithTrend(score: Double?, color: Color, last30: List<Double>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = score?.toInt()?.toString() ?: "—",
            color = color,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        if (score != null && last30.size >= 2) {
            val avg = last30.average()
            val delta = score - avg
            val deltaColor = if (delta >= 0) CosmosColors.Success else CosmosColors.Critical
            Text(
                text = "%+.1f".format(delta),
                color = deltaColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
            )
        }
    }
}

/**
 * Plus/Minus-Badge mit Farbe. Frank-Vorgabe 2026-05-10: ohne zusaetzlichen Beschreibungstext
 * darunter — die '30-Tage-Durchschnitt: 75'-Zeile am Ende der Karte erklaert sich selbst, also
 * keine Doppel-Information. Plus (gruen) wenn Wert ueber 30-Tage-Mittel, Minus (rot) wenn drunter,
 * blau bei kaum Aenderung (innerhalb +/-0.5).
 */
@Composable
private fun TrendBadge(delta: Double, formatter: (Double) -> String) {
    val color =
        when {
            delta > 0.5 -> CosmosColors.Success
            delta < -0.5 -> CosmosColors.Critical
            else -> CosmosColors.AccentPrimary
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
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
}

/**
 * Mini-Balkenchart der letzten Werte mit Beschriftung UNTER jedem Balken.
 *
 * Frank-Vorgabe 2026-05-10: jeder Balken bekommt seine EIGENE Farbe abhaengig vom Wert dieses
 * Balkens. Bei den Score-Karten (Readiness, Schlaf-Score, Aktivität) heisst das: ein Balken mit 85
 * ist gruen, einer mit 70 gelb, einer mit 55 rot — der ganze Verlauf zeigt also auf einen Blick wo
 * gute und wo schlechte Tage waren. `colorFor` mappt einen einzelnen Balkenwert auf seine Farbe.
 * Bei Resilienz/Levels wird einfach immer dieselbe Farbe zurueckgegeben.
 *
 * Auch der Wert-Text unter dem Balken nimmt diese Farbe damit Balken und Zahl zusammenpassen.
 */
@Composable
private fun HistoryMiniChartWithLabels(
    values: List<Double>,
    maxValue: Double,
    formatLabel: (Double) -> String,
    colorFor: (Double) -> Color,
) {
    if (values.isEmpty()) return
    val padded = values.takeLast(7)
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Wenn weniger als 7 Werte vorhanden sind, links auffuellen.
        repeat(7 - padded.size) { Box(modifier = Modifier.weight(1f).height(54.dp)) }
        padded.forEach { v ->
            val barColor = colorFor(v)
            val frac = (v / maxValue).coerceIn(0.0, 1.0).toFloat()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height((40.dp * frac).coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(barColor.copy(alpha = 0.85f))
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatLabel(v),
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
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
    Box(modifier = Modifier.weight(seconds.toFloat() / total.toFloat()).background(color))
}

@Composable
private fun StatChip(label: String, value: String) {
    val cosmos = LocalCosmos.current
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, color = cosmos.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
    }
}

/**
 * Kleine Zeile direkt unter dem Mini-Verlaufschart die den 30-Tage-Durchschnitt der Werte anzeigt —
 * Frank-Vorgabe 2026-05-10: 'unter den Balken bitte da noch den Durchschnittwert anzeigen lassen
 * fuer 30 Tage' und die Zahl 'als schwarze Zahl, egal was der Durchschnitt'. Schwarz bedeutet hier
 * cosmos.textPrimary (also der App-Standardtext, im Dark-Mode hell statt schwarz — der hoechste
 * Kontrast zum Karten-Hintergrund). Frank's Begruendung war dass keine Farbinterpretation noetig
 * sein soll — die Zahl steht einfach da als Zahl.
 */
@Composable
private fun ThirtyDayAverageLabel(values: List<Double>) {
    val cosmos = LocalCosmos.current
    if (values.size < 2) return
    val avg = values.average()
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "30-Tage-Durchschnitt: ",
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
        )
        Text(
            text = "%.0f".format(avg),
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* --------------------- Reine Funktionen (kein @Composable) --------------------- */

/**
 * Farb-Skala fuer Score-Karten (Readiness, Schlaf-Score, Aktivität). Frank-Vorgabe 2026-05-11: drei
 * Stufen mit verschobenen Schwellen.
 * - 75 oder mehr: gruen (Success)
 * - 50 bis 74: gelb (Warning)
 * - unter 50: rot (Critical) Resilienz nutzt eine eigene Logik (levelToColor), keine Score-Skala.
 */
internal fun scoreColor(score: Int?): Color =
    when {
        score == null -> CosmosColors.AccentPrimary
        score >= 75 -> CosmosColors.Success
        score >= 50 -> CosmosColors.Warning
        else -> CosmosColors.Critical
    }

internal fun levelToGerman(level: String?): String =
    when (level) {
        "limited" -> "Eingeschränkt"
        "adequate" -> "Ausreichend"
        "solid" -> "Solide"
        "strong" -> "Stark"
        "exceptional" -> "Außergewöhnlich"
        else -> "—"
    }

internal fun levelToColor(level: String?, fallback: Color): Color =
    when (level) {
        "exceptional",
        "strong" -> CosmosColors.Success
        "solid" -> CosmosColors.AccentPrimary
        "adequate" -> CosmosColors.Warning
        "limited" -> CosmosColors.Critical
        else -> fallback
    }

internal fun levelToRank(level: String?): Double? =
    when (level) {
        "limited" -> 0.0
        "adequate" -> 1.0
        "solid" -> 2.0
        "strong" -> 3.0
        "exceptional" -> 4.0
        else -> null
    }

/* ------------------------- Oura Score Balken-System ------------------------- */

private data class OuraScoreDerived(
    val last30: List<Double>,
    val avgAll: Double?,
    val deltaVsAvg: Double?,
)

private fun ouraScoreDerived(allScores: List<Double>): OuraScoreDerived {
    val last7 = allScores.takeLast(7)
    val avgAll = allScores.takeIf { it.isNotEmpty() }?.average()
    val current = allScores.lastOrNull()
    val delta = if (current != null && avgAll != null) current - avgAll else null
    return OuraScoreDerived(
        last30 = last7,
        avgAll = avgAll,
        deltaVsAvg = delta,
    )
}

/**
 * Farbe relativ zum persoenlichen Durchschnitt:
 * - ueber Durchschnitt            -> Gruen (Success, wie Resilienz)
 * - bis 10 unter Durchschnitt     -> Gelb
 * - mehr als 10 unter Durchschnitt -> Rot
 */
private fun scoreBarColor(score: Double, avg: Double?): Color {
    val avgSafe = avg ?: return CosmosColors.Warning
    return when {
        score >= avgSafe -> CosmosColors.Success
        score >= avgSafe - 10.0 -> CosmosColors.Warning
        else -> CosmosColors.Critical
    }
}

@Composable
private fun OuraScoreBars(values: List<Double>, avg: Double?) {
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 3.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 100.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { scoreBarColor(it, avg) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Daten",
    )
}

@Composable
private fun OuraTrendBadge(delta: Double) {
    val color =
        when {
            delta > 0.5 -> LocalCosmos.current.ok
            delta < -0.5 -> LocalCosmos.current.crit
            else -> LocalCosmos.current.accent
        }
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "%+.1f".format(delta),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
