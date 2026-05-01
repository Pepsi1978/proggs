package com.entropyjournal.ui.components.energyboard

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * Ueber der LazyColumn liegende Effekt-Schicht. Liest die Punkt-Positionen direkt
 * aus [LazyListState.layoutInfo] im Draw-Lambda — keine Recomposition durch Scrollen.
 *
 * @param railLeftPaddingDp Abstand vom linken Bildschirmrand bis zur Mitte der Stromlinie.
 *   Muss exakt mit dem horizontalen Padding der LazyColumn ueberein-stimmen, damit die
 *   Linie durch die Mitte der Timeline-Punkte laeuft. Standard 28dp = 16dp Padding der
 *   LazyColumn + 12dp Mitte des 24dp-Rails im TimelineItem.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> controller.touchPos.value = offset },
                    onDrag = { change, _ -> controller.touchPos.value = change.position },
                    onDragEnd = { controller.touchPos.value = null },
                    onDragCancel = { controller.touchPos.value = null },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        controller.touchPos.value = offset
                        try {
                            tryAwaitRelease()
                        } finally {
                            controller.touchPos.value = null
                        }
                    },
                )
            }
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
) {
    val info = listState.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty()) return

    val viewportStart = info.viewportStartOffset.toFloat()
    val nanos = controller.phaseNanos.longValue
    val tSec = nanos / 1_000_000_000f

    // Punkt-Y-Positionen aller sichtbaren Eintraege auf der Schiene (Mitte des Items).
    // Header-Items (kleine Zeilen unter ~60px) werden ausgefiltert, die Stromlinie
    // soll nur durch echte Eintraege fliessen.
    val pointYs = items
        .filter { it.size > 60 }
        .map { (it.offset - viewportStart) + it.size / 2f }
        .filter { it >= 0f && it <= canvasSize.height }

    if (pointYs.size < 2) {
        drawSparks(controller, tSec, coreColor, glowInner)
        return
    }

    // Fokus-Y bestimmen: Touch hat Vorrang, sonst Mittelpunkt des Viewports — auf den
    // naechstgelegenen tatsaechlichen Eintragspunkt rasten.
    val viewportMid = canvasSize.height / 2f
    val rawFocus = controller.touchPos.value?.y ?: viewportMid
    val snappedFocus = pointYs.minByOrNull { (it - rawFocus).absoluteValue } ?: viewportMid
    controller.focusY.value = snappedFocus

    val topY = pointYs.first()
    val bottomY = pointYs.last()

    // 3-Schichten-Glow auf der gesamten Stromlinie zwischen erstem und letztem Punkt
    drawGlowLine(railX, topY, bottomY, glowOuter, strokeWidth = 28f)
    drawGlowLine(railX, topY, bottomY, glowMid, strokeWidth = 14f)
    drawGlowLine(railX, topY, bottomY, glowInner, strokeWidth = 6f)

    // Wanderndes Energie-Paket: bewegt sich zyklisch vom obersten zum untersten Punkt
    val packetPhase = ((nanos / 1_000_000L) % EnergyTheme.PACKET_DURATION_MS) /
        EnergyTheme.PACKET_DURATION_MS.toFloat()
    val packetY = topY + (bottomY - topY) * packetPhase
    drawPacketGlow(railX, packetY, packetPeak, coreColor)

    // Funken
    drawSparks(controller, tSec, coreColor, glowInner)

    // Pulsierende Punkte
    pointYs.forEach { y ->
        val distToFocus = (y - snappedFocus).absoluteValue
        val focusBoost = focusGlow(distToFocus)
        val basePulse = organicPulse(tSec + y * 0.001f)
        val intensity = (0.35f + focusBoost * 0.65f) * (0.7f + basePulse * 0.3f)
        drawDot(railX, y, intensity, coreColor, glowInner, glowMid)
    }

    // Fokus-Halo
    val fIntensity = 0.7f + organicPulse(tSec) * 0.3f
    drawFocusHalo(railX, snappedFocus, fIntensity, glowInner, glowMid, glowOuter)
}

private fun DrawScope.drawGlowLine(
    railX: Float,
    topY: Float,
    bottomY: Float,
    color: Color,
    strokeWidth: Float,
) {
    drawLine(
        color = color,
        start = Offset(railX, topY),
        end = Offset(railX, bottomY),
        strokeWidth = strokeWidth,
        blendMode = BlendMode.Plus,
    )
}

private fun DrawScope.drawPacketGlow(
    railX: Float,
    y: Float,
    packetPeak: Color,
    coreColor: Color,
) {
    val brush = Brush.radialGradient(
        0.00f to coreColor,
        0.10f to packetPeak.copy(alpha = 0.95f),
        0.40f to packetPeak.copy(alpha = 0.45f),
        0.80f to packetPeak.copy(alpha = 0.10f),
        1.00f to Color.Transparent,
        center = Offset(railX, y),
        radius = 36f,
    )
    drawCircle(
        brush = brush,
        radius = 36f,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = coreColor,
        radius = 4.5f,
        center = Offset(railX, y),
    )
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
        radius = 18f * intensity,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = glowInner.copy(alpha = 0.65f * intensity),
        radius = 9f * intensity,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        color = coreColor.copy(alpha = (0.6f + intensity * 0.4f).coerceIn(0f, 1f)),
        radius = max(3f, 4.5f * intensity.coerceAtLeast(0.6f)),
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

        drawLine(
            color = glowColor.copy(alpha = alpha * 0.5f),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 4f * alpha,
            blendMode = BlendMode.Plus,
        )
        drawLine(
            color = coreColor.copy(alpha = alpha),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 1.5f,
        )
    }
}
