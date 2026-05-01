package com.entropyjournal.ui.components.energyboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Ueber der LazyColumn liegende Effekt-Schicht.
 * V3: zeichnet zusaetzlich um die aktive Karte einen leuchtenden Rahmen, einen leichten
 * Cyan-Wash innen, und Lichtenberg-Verzweigungen die von der Stromlinie um die Karte herum zucken.
 *
 * KEINE Touch-Events — Scrollen bleibt frei.
 */
@Composable
fun EnergyOverlay(
    listState: LazyListState,
    controller: EnergyController,
    isDarkTheme: Boolean,
    railLeftPaddingDp: Float = 28f,
    cardLeftDp: Float = 48f,        // 16dp LazyColumn-Pad + 24dp Rail-Spacer + 8dp Inner-Spacer
    cardRightInsetDp: Float = 16f,  // LazyColumn rechts-Pad
    modifier: Modifier = Modifier,
) {
    val core = if (isDarkTheme) EnergyTheme.Core else EnergyTheme.CoreLight
    val gIn = if (isDarkTheme) EnergyTheme.GlowInner else EnergyTheme.GlowInnerLight
    val gMid = if (isDarkTheme) EnergyTheme.GlowMid else EnergyTheme.GlowMidLight
    val gOut = if (isDarkTheme) EnergyTheme.GlowOuter else EnergyTheme.GlowOuterLight
    val packetPeak = if (isDarkTheme) EnergyTheme.PacketPeak else EnergyTheme.PacketPeakLight
    val accent = if (isDarkTheme) EnergyTheme.Accent else EnergyTheme.AccentLight
    val cardWash = if (isDarkTheme) EnergyTheme.CardWash else EnergyTheme.CardWashLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val railX = with(this) { railLeftPaddingDp.dp.toPx() }
                val cardLeft = with(this) { cardLeftDp.dp.toPx() }
                val cardRightInset = with(this) { cardRightInsetDp.dp.toPx() }
                onDrawBehind {
                    drawEnergy(
                        canvasSize = size,
                        railX = railX,
                        cardLeft = cardLeft,
                        cardRightInset = cardRightInset,
                        listState = listState,
                        controller = controller,
                        coreColor = core,
                        glowInner = gIn,
                        glowMid = gMid,
                        glowOuter = gOut,
                        packetPeak = packetPeak,
                        accentColor = accent,
                        cardWash = cardWash,
                    )
                }
            },
    )
}

private fun DrawScope.drawEnergy(
    canvasSize: Size,
    railX: Float,
    cardLeft: Float,
    cardRightInset: Float,
    listState: LazyListState,
    controller: EnergyController,
    coreColor: Color,
    glowInner: Color,
    glowMid: Color,
    glowOuter: Color,
    packetPeak: Color,
    accentColor: Color,
    cardWash: Color,
) {
    val info = listState.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty()) return

    val viewportStart = info.viewportStartOffset.toFloat()
    val nanos = controller.phaseNanos.longValue
    val tSec = nanos / 1_000_000_000f

    // Eintragspunkte (Header-Items < 60px werden gefiltert) — wir merken uns auch die Item-Bounds
    data class EntryItem(val centerY: Float, val topY: Float, val bottomY: Float, val originalIndex: Int)
    val entries = items
        .filter { it.size > 60 }
        .map {
            val top = (it.offset - viewportStart).toFloat()
            val bottom = top + it.size
            EntryItem(centerY = top + it.size / 2f, topY = top, bottomY = bottom, originalIndex = it.index)
        }
        .filter { it.bottomY >= 0f && it.topY <= canvasSize.height }

    val pointYs = entries.map { it.centerY }
    controller.spawnPoints.value = pointYs
    controller.railX.value = railX

    if (entries.isEmpty()) {
        controller.activeCardBounds.value = null
        drawArcs(controller, glowInner, coreColor, accentColor)
        drawSparks(controller, tSec, coreColor, glowInner, packetPeak)
        return
    }

    // Aktive Karte = Eintrag dessen Mitte am naechsten zur Viewport-Mitte ist
    val viewportMid = canvasSize.height / 2f
    val active = entries.minByOrNull { (it.centerY - viewportMid).absoluteValue } ?: entries.first()
    val cardRight = canvasSize.width - cardRightInset
    val activeBounds = Rect(
        left = cardLeft,
        top = active.topY + 6f,        // entspricht dem padding vertical=6.dp der GlassCard
        right = cardRight,
        bottom = active.bottomY - 6f,
    )
    controller.activeCardBounds.value = activeBounds
    controller.focusY.value = active.centerY

    // === Card-Wash: leichte Cyan-Toenung der aktiven Karte (sehr transparent) ===
    drawCardWash(activeBounds, cardWash, tSec)

    // === Hauptlinie ===
    drawElectricLine(
        railX = railX,
        topY = entries.first().centerY,
        bottomY = entries.last().centerY,
        tSec = tSec,
        glowOuter = glowOuter,
        glowMid = glowMid,
        glowInner = glowInner,
        coreColor = coreColor,
    )

    // === Lichtenberg um aktive Karte ===
    drawLichtenbergs(controller, glowInner, accentColor, coreColor)

    // === Quer-Blitze (gerichtet) ===
    drawArcs(controller, glowInner, coreColor, accentColor)

    // === Wandernde Energie-Pakete ===
    drawPackets(railX, entries.first().centerY, entries.last().centerY, nanos, packetPeak, coreColor)

    // === Funken ===
    drawSparks(controller, tSec, coreColor, glowInner, packetPeak)

    // === Pulsierende Punkte mit Strahlenkranz ===
    entries.forEach { entry ->
        val isActive = entry === active
        val distToFocus = (entry.centerY - active.centerY).absoluteValue
        val focusBoost = focusGlow(distToFocus)
        val basePulse = organicPulse(tSec + entry.centerY * 0.001f)
        val crackle = crackleNoise(tSec, entry.centerY)
        val intensity = ((if (isActive) 0.85f else 0.4f) + focusBoost * 0.4f) * (0.7f + basePulse * 0.3f) * crackle
        drawPowerNode(railX, entry.centerY, intensity, isActive, tSec, coreColor, glowInner, glowMid, accentColor)
    }

    // === Leuchtender Rahmen um aktive Karte ===
    val frameIntensity = 0.7f + organicPulse(tSec) * 0.3f
    drawActiveCardFrame(activeBounds, frameIntensity, tSec, coreColor, glowInner, glowMid, glowOuter)
}

/** Sanft animierter Wash innerhalb der aktiven Karte — gibt ihr eine elektrische Toenung. */
private fun DrawScope.drawCardWash(bounds: Rect, washColor: Color, tSec: Float) {
    val pulse = 0.7f + organicPulse(tSec * 1.4f) * 0.3f
    drawRect(
        color = washColor,
        topLeft = bounds.topLeft,
        size = bounds.size,
        alpha = pulse,
        blendMode = BlendMode.Plus,
    )
    // Zusaetzlich ein RadialGradient von der linken Mitte (wo die Stromlinie reinfliesst)
    val brush = Brush.radialGradient(
        0.0f to washColor.copy(alpha = (washColor.alpha * 2.0f).coerceAtMost(1f)),
        0.6f to washColor.copy(alpha = washColor.alpha * 0.4f),
        1.0f to Color.Transparent,
        center = Offset(bounds.left, bounds.center.y),
        radius = bounds.width * 0.7f,
    )
    drawRect(
        brush = brush,
        topLeft = bounds.topLeft,
        size = bounds.size,
        alpha = pulse,
        blendMode = BlendMode.Plus,
    )
}

/**
 * Mehrschicht-Glow-Rahmen um die aktive Karte (wie ein elektrischer Bilderrahmen).
 * Mit knisterndem Alpha und leicht jitternder Strichbreite.
 */
private fun DrawScope.drawActiveCardFrame(
    bounds: Rect,
    intensity: Float,
    tSec: Float,
    coreColor: Color,
    glowInner: Color,
    glowMid: Color,
    glowOuter: Color,
) {
    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    val crackle = crackleNoise(tSec, bounds.top)

    // Aussen — sehr weicher Halo
    drawRoundRect(
        color = glowOuter,
        topLeft = Offset(bounds.left - 10f, bounds.top - 10f),
        size = Size(bounds.width + 20f, bounds.height + 20f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
        style = Stroke(width = 22f),
        blendMode = BlendMode.Plus,
        alpha = (intensity * crackle).coerceIn(0f, 1f),
    )
    // Mitte
    drawRoundRect(
        color = glowMid.copy(alpha = 0.55f),
        topLeft = bounds.topLeft,
        size = bounds.size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 12f),
        blendMode = BlendMode.Plus,
        alpha = (intensity * crackle).coerceIn(0f, 1f),
    )
    // Innen — heller Cyan
    drawRoundRect(
        color = glowInner.copy(alpha = 0.85f),
        topLeft = bounds.topLeft,
        size = bounds.size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 5f),
        blendMode = BlendMode.Plus,
        alpha = (intensity * crackle).coerceIn(0f, 1f),
    )
    // Harter weisser Kern
    drawRoundRect(
        color = coreColor,
        topLeft = bounds.topLeft,
        size = bounds.size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.6f),
        alpha = (intensity * 0.85f * crackle + 0.15f).coerceIn(0f, 1f),
    )
}

/** Lichtenberg-Verzweigungen rund um die aktive Karte. */
private fun DrawScope.drawLichtenbergs(
    controller: EnergyController,
    glowInner: Color,
    accentColor: Color,
    coreColor: Color,
) {
    val nowMs = controller.phaseNanos.longValue / 1_000_000L
    controller.lichtenbergs.forEach { lb ->
        val ageMs = nowMs - lb.bornAtMs
        if (ageMs < 0L || ageMs > EnergyTheme.LICHTENBERG_LIFETIME_MS) return@forEach
        val progress = ageMs.toFloat() / EnergyTheme.LICHTENBERG_LIFETIME_MS.toFloat()
        // Schnelles Aufflackern, dann Fadeout
        val alpha = if (progress < 0.15f) progress / 0.15f else (1f - progress) / 0.85f
        val a = alpha.coerceIn(0f, 1f)

        val path = Path()
        lb.points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        // Aussen Violett-Halo
        drawPath(
            path = path,
            color = accentColor.copy(alpha = a * 0.45f),
            style = Stroke(width = 7f),
            blendMode = BlendMode.Plus,
        )
        // Cyan-Glow
        drawPath(
            path = path,
            color = glowInner.copy(alpha = a * 0.85f),
            style = Stroke(width = 3.2f),
            blendMode = BlendMode.Plus,
        )
        // Heller Kern
        drawPath(
            path = path,
            color = coreColor.copy(alpha = a),
            style = Stroke(width = 1.2f),
        )
    }
}

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
    path.moveTo(railX, topY)
    for (i in 1..seg) {
        val y = topY + (length * i / seg)
        val n1 = sin(tSec * 17f + i * 1.7f)
        val n2 = sin(tSec * 11f + i * 3.1f)
        val offset = (n1 * 0.6f + n2 * 0.4f) * jitter
        val x = railX + offset
        path.lineTo(x, y)
    }

    val crackle = crackleNoise(tSec, 0f)

    drawPath(path, color = glowOuter, style = Stroke(width = 26f), blendMode = BlendMode.Plus, alpha = (0.85f * crackle).coerceIn(0f, 1f))
    drawPath(path, color = glowMid.copy(alpha = 0.65f), style = Stroke(width = 14f), blendMode = BlendMode.Plus, alpha = crackle.coerceIn(0f, 1f))
    drawPath(path, color = glowInner.copy(alpha = 0.85f), style = Stroke(width = 5f), blendMode = BlendMode.Plus, alpha = crackle.coerceIn(0f, 1f))
    drawPath(path, color = coreColor, style = Stroke(width = 1.6f), alpha = (0.85f * crackle + 0.15f).coerceIn(0f, 1f))
}

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
        drawPath(path, color = accentColor.copy(alpha = alpha * 0.5f), style = Stroke(width = 9f), blendMode = BlendMode.Plus)
        drawPath(path, color = glowInner.copy(alpha = alpha * 0.85f), style = Stroke(width = 4f), blendMode = BlendMode.Plus)
        drawPath(path, color = coreColor.copy(alpha = alpha), style = Stroke(width = 1.5f))
    }
}

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
        drawCircle(brush = brush, radius = 22f, center = Offset(railX, y), blendMode = BlendMode.Plus)
        drawCircle(color = coreColor, radius = 2.8f, center = Offset(railX, y))
    }
}

/**
 * Power-Node — Punkt auf der Schiene mit deutlichem Glow + Strahlenkranz der zuckt.
 * Aktive Punkte sind staerker und haben groessere Strahlen.
 */
private fun DrawScope.drawPowerNode(
    railX: Float,
    y: Float,
    intensity: Float,
    isActive: Boolean,
    tSec: Float,
    coreColor: Color,
    glowInner: Color,
    glowMid: Color,
    accentColor: Color,
) {
    val outerR = EnergyTheme.DOT_OUTER_RADIUS_PX * (if (isActive) 1.3f else 1f)
    val innerR = EnergyTheme.DOT_INNER_RADIUS_PX * (if (isActive) 1.25f else 1f)
    val coreR = EnergyTheme.DOT_CORE_RADIUS_PX * (if (isActive) 1.15f else 1f)

    // Aeusserer Glow (RadialGradient)
    val brush = Brush.radialGradient(
        0.00f to glowInner.copy(alpha = 0.6f * intensity),
        0.40f to glowMid.copy(alpha = 0.35f * intensity),
        1.00f to Color.Transparent,
        center = Offset(railX, y),
        radius = outerR,
    )
    drawCircle(brush = brush, radius = outerR, center = Offset(railX, y), blendMode = BlendMode.Plus)

    // Mittlerer Cyan-Halo
    drawCircle(
        color = glowInner.copy(alpha = 0.65f * intensity),
        radius = innerR,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )

    // Strahlenkranz: kleine Strahlen die nach aussen zucken (rotieren ueber Zeit)
    val rays = EnergyTheme.DOT_RAY_COUNT
    val rayLen = EnergyTheme.DOT_RAY_LENGTH_PX * (if (isActive) 1.4f else 0.85f) * intensity
    val rayJitter = sin(tSec * 5f + y * 0.05f) * 0.3f
    repeat(rays) { i ->
        val angle = (i.toFloat() / rays) * 2f * Math.PI.toFloat() + tSec * 1.2f + rayJitter
        val rx = railX + cos(angle) * coreR
        val ry = y + sin(angle) * coreR
        val ex = railX + cos(angle) * (coreR + rayLen)
        val ey = y + sin(angle) * (coreR + rayLen)
        drawLine(
            color = (if (isActive) glowInner else accentColor).copy(alpha = 0.6f * intensity),
            start = Offset(rx, ry),
            end = Offset(ex, ey),
            strokeWidth = 1.6f,
            blendMode = BlendMode.Plus,
        )
    }

    // Solider Kern
    drawCircle(
        color = coreColor.copy(alpha = (0.6f + intensity * 0.4f).coerceIn(0f, 1f)),
        radius = max(2.5f, coreR),
        center = Offset(railX, y),
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

        drawLine(
            color = glowColor.copy(alpha = alpha * 0.6f),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 5f * alpha,
            blendMode = BlendMode.Plus,
        )
        drawLine(
            color = packetPeak.copy(alpha = alpha * 0.85f),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 2.5f,
            blendMode = BlendMode.Plus,
        )
        drawLine(
            color = coreColor.copy(alpha = alpha),
            start = Offset(tailX, tailY),
            end = Offset(x, y),
            strokeWidth = 1.2f,
        )
    }
}
