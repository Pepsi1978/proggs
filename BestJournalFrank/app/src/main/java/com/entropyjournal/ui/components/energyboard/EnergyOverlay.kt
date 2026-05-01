package com.entropyjournal.ui.components.energyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sin

/**
 * Ueber der LazyColumn liegende Effekt-Schicht. Liest Punkt-Positionen direkt aus
 * [LazyListState.layoutInfo] im Draw-Lambda — keine Recomposition durch Scrollen.
 *
 * V2-WICHTIG: Dieser Composable greift KEINE Touch-Events ab. Die LazyColumn behaelt
 * volles Scroll-Verhalten. Visuelle Reaktion auf Scrollen entsteht implizit dadurch,
 * dass der Fokus-Punkt immer das mittigste sichtbare Item ist — beim Scrollen wandert
 * also der Strom-Brennpunkt automatisch mit.
 */
@Composable
fun EnergyOverlay(
    listState: LazyListState,
    controller: EnergyController,
    isDarkTheme: Boolean,
    railLeftPaddingDp: Float = 28f,
    modifier: Modifier = Modifier,
) {
    val core = if (isDarkTheme) EnergyTheme.Core else EnergyTheme.CoreLight
    val gIn = if (isDarkTheme) EnergyTheme.GlowInner else EnergyTheme.GlowInnerLight
    val gMid = if (isDarkTheme) EnergyTheme.GlowMid else EnergyTheme.GlowMidLight
    val gOut = if (isDarkTheme) EnergyTheme.GlowOuter else EnergyTheme.GlowOuterLight
    val packetPeak = if (isDarkTheme) EnergyTheme.PacketPeak else EnergyTheme.PacketPeakLight
    val accent = if (isDarkTheme) EnergyTheme.Accent else EnergyTheme.AccentLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val railX = with(this) { railLeftPaddingDp.dp.toPx() }
                onDrawBehind {
                    drawEnergy(
                        canvasSize = size,
                        railX = railX,
                        listState = listState,
                        controller = controller,
                        coreColor = core,
                        glowInner = gIn,
                        glowMid = gMid,
                        glowOuter = gOut,
                        packetPeak = packetPeak,
                        accentColor = accent,
                    )
                }
            },
    )
}

private fun DrawScope.drawEnergy(
    canvasSize: Size,
    railX: Float,
    listState: LazyListState,
    controller: EnergyController,
    coreColor: Color,
    glowInner: Color,
    glowMid: Color,
    glowOuter: Color,
    packetPeak: Color,
    accentColor: Color,
) {
    val info = listState.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty()) return

    val viewportStart = info.viewportStartOffset.toFloat()
    val nanos = controller.phaseNanos.longValue
    val tSec = nanos / 1_000_000_000f

    // Punkt-Y-Positionen aller sichtbaren Eintraege (Header-Items < 60px werden gefiltert)
    val pointYs = items
        .filter { it.size > 60 }
        .map { (it.offset - viewportStart) + it.size / 2f }
        .filter { it >= 0f && it <= canvasSize.height }

    // Spawn-Daten an Controller weitergeben (er liest sie im Frame-Loop)
    controller.spawnPoints.value = pointYs
    controller.railX.value = railX

    if (pointYs.size < 2) {
        // Nur Funken/Quer-Blitze die noch leben weiter zeichnen
        drawArcs(controller, glowInner, coreColor, accentColor)
        drawSparks(controller, tSec, coreColor, glowInner, packetPeak)
        return
    }

    // Fokus-Punkt = mittigstes sichtbares Item — Scrollen verschiebt ihn automatisch
    val viewportMid = canvasSize.height / 2f
    val snappedFocus = pointYs.minByOrNull { (it - viewportMid).absoluteValue } ?: viewportMid
    controller.focusY.value = snappedFocus

    val topY = pointYs.first()
    val bottomY = pointYs.last()

    // === Knisternde Hauptlinie ===
    // Statt einer geraden Linie zeichnen wir eine zickzackige, jitternde Polylinie mit
    // mehreren Glow-Schichten. Das Jitter veraendert sich pro Frame, was das "Knistern"
    // erzeugt.
    drawElectricLine(
        railX = railX,
        topY = topY,
        bottomY = bottomY,
        tSec = tSec,
        glowOuter = glowOuter,
        glowMid = glowMid,
        glowInner = glowInner,
        coreColor = coreColor,
    )

    // === Quer-Blitze ===
    drawArcs(controller, glowInner, coreColor, accentColor)

    // === Wandernde Energie-Pakete (mehrere versetzt, statt eine Kugel) ===
    drawPackets(railX, topY, bottomY, nanos, packetPeak, coreColor)

    // === Funken ===
    drawSparks(controller, tSec, coreColor, glowInner, packetPeak)

    // === Pulsierende Punkte ===
    pointYs.forEach { y ->
        val distToFocus = (y - snappedFocus).absoluteValue
        val focusBoost = focusGlow(distToFocus)
        val basePulse = organicPulse(tSec + y * 0.001f)
        val crackle = crackleNoise(tSec, y)
        val intensity = (0.4f + focusBoost * 0.6f) * (0.7f + basePulse * 0.3f) * crackle
        drawDot(railX, y, intensity, coreColor, glowInner, glowMid)
    }

    // === Fokus-Halo ===
    val fIntensity = 0.7f + organicPulse(tSec) * 0.3f
    drawFocusHalo(railX, snappedFocus, fIntensity, glowInner, glowMid, glowOuter)
}

/**
 * Zeichnet die Hauptlinie als zickzackige Polylinie mit zeit-abhaengigem Jitter pro Segment.
 * Drei Schichten von aussen nach innen + Knister-Alpha.
 */
private fun DrawScope.drawElectricLine(
    railX: Float,
    topY: Float,
    bottomY: Float,
    tSec: Float,
    glowOuter: Color,
    glowMid: Color,
    glowInner: Color,
    coreColor: Color,
) {
    val length = bottomY - topY
    if (length <= 0f) return

    val seg = EnergyTheme.LINE_SEGMENT_COUNT
    val jitter = EnergyTheme.LINE_JITTER_PX

    val path = Path()
    var prevX = railX
    var prevY = topY
    path.moveTo(prevX, prevY)
    for (i in 1..seg) {
        val y = topY + (length * i / seg)
        // Pseudo-Random-Jitter abhaengig von Zeit und Segment-Index — flackert pro Frame
        val n1 = sin(tSec * 17f + i * 1.7f)
        val n2 = sin(tSec * 11f + i * 3.1f)
        val offset = (n1 * 0.6f + n2 * 0.4f) * jitter
        val x = railX + offset
        path.lineTo(x, y)
        prevX = x; prevY = y
    }

    // Knister-Alpha — die ganze Linie flackert in der Helligkeit
    val crackle = crackleNoise(tSec, 0f)

    // Schicht 1: aussen, breit, weich
    drawPath(
        path = path,
        color = glowOuter,
        style = Stroke(width = 26f),
        blendMode = BlendMode.Plus,
        alpha = (0.85f * crackle).coerceIn(0f, 1f),
    )
    // Schicht 2: mittel
    drawPath(
        path = path,
        color = glowMid.copy(alpha = 0.65f),
        style = Stroke(width = 14f),
        blendMode = BlendMode.Plus,
        alpha = crackle.coerceIn(0f, 1f),
    )
    // Schicht 3: hell, schmal
    drawPath(
        path = path,
        color = glowInner.copy(alpha = 0.85f),
        style = Stroke(width = 5f),
        blendMode = BlendMode.Plus,
        alpha = crackle.coerceIn(0f, 1f),
    )
    // Schicht 4: harter weisser Kern
    drawPath(
        path = path,
        color = coreColor,
        style = Stroke(width = 1.6f),
        alpha = (0.85f * crackle + 0.15f).coerceIn(0f, 1f),
    )
}

/** Quer-Blitze — gezackte Mini-Polylinien die seitlich aus der Schiene knistern. */
private fun DrawScope.drawArcs(
    controller: EnergyController,
    glowInner: Color,
    coreColor: Color,
    accentColor: Color,
) {
    val nowMs = controller.phaseNanos.longValue / 1_000_000L
    controller.arcs.forEach { arc ->
        val ageMs = nowMs - arc.bornAtMs
        if (ageMs < 0L || ageMs > EnergyTheme.ARC_LIFETIME_MS) return@forEach
        val progress = ageMs.toFloat() / EnergyTheme.ARC_LIFETIME_MS.toFloat()
        val alpha = (1f - progress).coerceIn(0f, 1f)

        val path = Path()
        arc.points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.first, p.second) else path.lineTo(p.first, p.second)
        }
        // Aeusserer Glow
        drawPath(
            path = path,
            color = accentColor.copy(alpha = alpha * 0.5f),
            style = Stroke(width = 9f),
            blendMode = BlendMode.Plus,
        )
        // Mittlerer Glow
        drawPath(
            path = path,
            color = glowInner.copy(alpha = alpha * 0.85f),
            style = Stroke(width = 4f),
            blendMode = BlendMode.Plus,
        )
        // Kern
        drawPath(
            path = path,
            color = coreColor.copy(alpha = alpha),
            style = Stroke(width = 1.5f),
        )
    }
}

/**
 * Mehrere wandernde Energie-Pakete — drei versetzt um 1/3 Phasendauer.
 * Sehen aus wie schnelle Lichtimpulse die der Linie entlang sausen.
 */
private fun DrawScope.drawPackets(
    railX: Float,
    topY: Float,
    bottomY: Float,
    nanos: Long,
    packetPeak: Color,
    coreColor: Color,
) {
    val length = bottomY - topY
    if (length <= 0f) return

    val durMs = EnergyTheme.PACKET_DURATION_MS
    repeat(3) { i ->
        val phaseShift = i / 3f
        val rawPhase = ((nanos / 1_000_000L) % durMs).toFloat() / durMs.toFloat()
        val phase = ((rawPhase + phaseShift) % 1f)
        val y = topY + length * phase
        val brush = Brush.radialGradient(
            0.00f to coreColor,
            0.10f to packetPeak.copy(alpha = 0.95f),
            0.40f to packetPeak.copy(alpha = 0.40f),
            0.80f to packetPeak.copy(alpha = 0.10f),
            1.00f to Color.Transparent,
            center = Offset(railX, y),
            radius = 22f,
        )
        drawCircle(
            brush = brush,
            radius = 22f,
            center = Offset(railX, y),
            blendMode = BlendMode.Plus,
        )
        drawCircle(
            color = coreColor,
            radius = 2.8f,
            center = Offset(railX, y),
        )
    }
}

private fun DrawScope.drawDot(
    railX: Float,
    y: Float,
    intensity: Float,
    coreColor: Color,
    glowInner: Color,
    glowMid: Color,
) {
    drawCircle(
        color = glowMid.copy(alpha = 0.35f * intensity),
        radius = 16f * intensity,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = glowInner.copy(alpha = 0.65f * intensity),
        radius = 8f * intensity,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = coreColor.copy(alpha = (0.6f + intensity * 0.4f).coerceIn(0f, 1f)),
        radius = max(2.5f, 4f * intensity.coerceAtLeast(0.6f)),
        center = Offset(railX, y),
    )
}

private fun DrawScope.drawFocusHalo(
    railX: Float,
    y: Float,
    intensity: Float,
    glowInner: Color,
    glowMid: Color,
    glowOuter: Color,
) {
    drawCircle(
        color = glowOuter,
        radius = 56f,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = glowMid.copy(alpha = 0.30f * intensity),
        radius = 36f,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = glowInner.copy(alpha = 0.45f * intensity),
        radius = 18f,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
}

private fun DrawScope.drawSparks(
    controller: EnergyController,
    tSec: Float,
    coreColor: Color,
    glowColor: Color,
    packetPeak: Color,
) {
    val nowMs = (tSec * 1000f).toLong()
    controller.sparks.forEach { spark ->
        val ageMs = nowMs - spark.bornAtMs
        if (ageMs < 0L || ageMs > spark.lifetimeMs) return@forEach
        val progress = ageMs.toFloat() / spark.lifetimeMs.toFloat()
        val alpha = (1f - progress).coerceIn(0f, 1f)

        val frames = ageMs / 16.6f
        val x = spark.startX + spark.velX * frames
        val y = spark.startY + spark.velY * frames + 0.04f * frames * frames

        val tailX = x - spark.velX * 2f
        val tailY = y - spark.velY * 2f

        // Aeusserer Glow
        drawLine(
            color = glowColor.copy(alpha = alpha * 0.6f),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 5f * alpha,
            blendMode = BlendMode.Plus,
        )
        // Innerer hellerer Glow
        drawLine(
            color = packetPeak.copy(alpha = alpha * 0.85f),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 2.5f,
            blendMode = BlendMode.Plus,
        )
        // Heller Kern
        drawLine(
            color = coreColor.copy(alpha = alpha),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 1.2f,
        )
    }
}
