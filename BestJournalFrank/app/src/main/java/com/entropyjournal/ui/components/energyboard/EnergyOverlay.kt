package com.entropyjournal.ui.components.energyboard

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Ueber der LazyColumn liegende Effekt-Schicht (V4).
 * Nutzt AGSL-RuntimeShader fuer Card-Border-Bloom und FBM-Lightning-Hauptlinie.
 * Fraktale Lichtenberg-Baeume werden via CPU-Generator erzeugt und mit 3-Schichten-Glow
 * gezeichnet. AGSL braucht API 33+ — auf aelteren Geraeten fallen die Shader-Effekte aus,
 * der Rest funktioniert weiter.
 */
@Composable
fun EnergyOverlay(
    listState: LazyListState,
    controller: EnergyController,
    isDarkTheme: Boolean,
    railLeftPaddingDp: Float = 28f,
    cardLeftDp: Float = 48f,
    cardRightInsetDp: Float = 16f,
    modifier: Modifier = Modifier,
) {
    val core = if (isDarkTheme) EnergyTheme.Core else EnergyTheme.CoreLight
    val gIn = if (isDarkTheme) EnergyTheme.GlowInner else EnergyTheme.GlowInnerLight
    val gMid = if (isDarkTheme) EnergyTheme.GlowMid else EnergyTheme.GlowMidLight
    val gOut = if (isDarkTheme) EnergyTheme.GlowOuter else EnergyTheme.GlowOuterLight
    val packetPeak = if (isDarkTheme) EnergyTheme.PacketPeak else EnergyTheme.PacketPeakLight
    val accent = if (isDarkTheme) EnergyTheme.Accent else EnergyTheme.AccentLight

    val agslAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // Shader nur einmal anlegen — RuntimeShader ist ein teures Objekt
    val borderShader = if (agslAvailable) {
        remember { RuntimeShader(EnergyShaders.CARD_BORDER_GLOW) }
    } else null
    val lightningShader = if (agslAvailable) {
        remember { RuntimeShader(EnergyShaders.FBM_LIGHTNING) }
    } else null

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
                        borderShader = borderShader,
                        lightningShader = lightningShader,
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
    borderShader: RuntimeShader?,
    lightningShader: RuntimeShader?,
) {
    val info = listState.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty()) return

    val viewportStart = info.viewportStartOffset.toFloat()
    val nanos = controller.phaseNanos.longValue
    val tSec = nanos / 1_000_000_000f

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

    val viewportMid = canvasSize.height / 2f
    val active = entries.minByOrNull { (it.centerY - viewportMid).absoluteValue } ?: entries.first()
    val cardRight = canvasSize.width - cardRightInset
    val activeBounds = Rect(
        left = cardLeft,
        top = active.topY + 6f,
        right = cardRight,
        bottom = active.bottomY - 6f,
    )
    controller.activeCardBounds.value = activeBounds
    controller.focusY.value = active.centerY

    val topY = entries.first().centerY
    val bottomY = entries.last().centerY

    // === 1. AGSL FBM-Lightning fuer die Hauptlinie (volumetrisch, mit Sub-Aesten) ===
    if (lightningShader != null) {
        drawAgslLightningRail(
            shader = lightningShader,
            railX = railX,
            topY = topY,
            bottomY = bottomY,
            tSec = tSec,
            coreColor = coreColor,
            glowInner = glowInner,
        )
    } else {
        // Fallback fuer API < 33
        drawElectricLineFallback(railX, topY, bottomY, tSec, glowOuter, glowMid, glowInner, coreColor)
    }

    // === 2. Lichtenberg-Baeume um aktive Karte (CPU-generiert, 3-Schichten-Glow) ===
    drawLichtenbergTrees(controller, glowInner, accentColor, coreColor, packetPeak)

    // === 3. AGSL Card-Border-Glow ===
    if (borderShader != null) {
        drawAgslCardBorder(
            shader = borderShader,
            bounds = activeBounds,
            tSec = tSec,
            glowInner = glowInner,
            coreColor = coreColor,
        )
    } else {
        drawCardBorderFallback(activeBounds, tSec, coreColor, glowInner, glowMid, glowOuter)
    }

    // === 4. Quer-Blitze (gerichtet) ===
    drawArcs(controller, glowInner, coreColor, accentColor)

    // === 5. Wandernde Energie-Pakete ===
    drawPackets(railX, topY, bottomY, nanos, packetPeak, coreColor)

    // === 6. Funken ===
    drawSparks(controller, tSec, coreColor, glowInner, packetPeak)

    // === 7. Power-Nodes (Punkte mit Strahlenkranz) ===
    entries.forEach { entry ->
        val isActive = entry === active
        val distToFocus = (entry.centerY - active.centerY).absoluteValue
        val focusBoost = focusGlow(distToFocus)
        val basePulse = organicPulse(tSec + entry.centerY * 0.001f)
        val crackle = crackleNoise(tSec, entry.centerY)
        val intensity = ((if (isActive) 0.85f else 0.4f) + focusBoost * 0.4f) *
            (0.7f + basePulse * 0.3f) * crackle
        drawPowerNode(railX, entry.centerY, intensity, isActive, tSec, coreColor, glowInner, glowMid, accentColor)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawAgslLightningRail(
    shader: RuntimeShader,
    railX: Float,
    topY: Float,
    bottomY: Float,
    tSec: Float,
    coreColor: Color,
    glowInner: Color,
) {
    val railWidth = 80f  // Region-Breite fuer den Shader
    val railTopLeft = Offset(railX - railWidth / 2f, topY - 20f)
    val railSize = Size(railWidth, (bottomY - topY) + 40f)

    shader.setFloatUniform("resolution", railSize.width, railSize.height)
    shader.setFloatUniform("time", tSec)
    shader.setFloatUniform("intensity", 1.0f + crackleNoise(tSec, 0f) * 0.3f)
    shader.setColorUniform(
        "plasmaColor",
        android.graphics.Color.argb(
            255,
            (glowInner.red * 255).toInt(),
            (glowInner.green * 255).toInt(),
            (glowInner.blue * 255).toInt(),
        )
    )
    shader.setColorUniform(
        "hotCore",
        android.graphics.Color.argb(
            255,
            (coreColor.red * 255).toInt(),
            (coreColor.green * 255).toInt(),
            (coreColor.blue * 255).toInt(),
        )
    )

    drawRect(
        brush = ShaderBrush(shader),
        topLeft = railTopLeft,
        size = railSize,
        blendMode = BlendMode.Plus,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawAgslCardBorder(
    shader: RuntimeShader,
    bounds: Rect,
    tSec: Float,
    glowInner: Color,
    coreColor: Color,
) {
    val glowRadius = 50f
    val expandedBounds = Rect(
        left = bounds.left - glowRadius,
        top = bounds.top - glowRadius,
        right = bounds.right + glowRadius,
        bottom = bounds.bottom + glowRadius,
    )

    shader.setFloatUniform("size", expandedBounds.width, expandedBounds.height)
    shader.setFloatUniform("cornerRadius", 16f)
    shader.setFloatUniform("time", tSec)
    shader.setFloatUniform("glowRadius", glowRadius)
    shader.setColorUniform(
        "glowColor",
        android.graphics.Color.argb(
            255,
            (glowInner.red * 255).toInt(),
            (glowInner.green * 255).toInt(),
            (glowInner.blue * 255).toInt(),
        )
    )
    shader.setColorUniform(
        "innerGlowColor",
        android.graphics.Color.argb(
            255,
            (coreColor.red * 255).toInt(),
            (coreColor.green * 255).toInt(),
            (coreColor.blue * 255).toInt(),
        )
    )

    drawRect(
        brush = ShaderBrush(shader),
        topLeft = expandedBounds.topLeft,
        size = expandedBounds.size,
        blendMode = BlendMode.Plus,
    )
}

/** Fraktale Lichtenberg-Baeume mit 3-Schichten-Glow je Segment. */
private fun DrawScope.drawLichtenbergTrees(
    controller: EnergyController,
    glowInner: Color,
    accentColor: Color,
    coreColor: Color,
    packetPeak: Color,
) {
    val nowMs = controller.phaseNanos.longValue / 1_000_000L
    controller.lichtenbergs.forEach { tree ->
        val ageMs = nowMs - tree.bornAtMs
        if (ageMs < 0L || ageMs > EnergyTheme.LICHTENBERG_LIFETIME_MS) return@forEach
        val progress = ageMs.toFloat() / EnergyTheme.LICHTENBERG_LIFETIME_MS.toFloat()
        // Schnelles Aufflackern, dann Fadeout
        val baseAlpha = if (progress < 0.15f) progress / 0.15f else (1f - progress) / 0.85f
        val a = baseAlpha.coerceIn(0f, 1f)

        tree.segments.forEach { seg ->
            val genFactor = 0.5f.pow(seg.generation.toFloat())
            val segIntensity = a * seg.intensity

            val baseWidth = 5f * genFactor
            // Schicht 1: weiter Halo (transparent, dick)
            drawLine(
                color = accentColor.copy(alpha = segIntensity * 0.25f),
                start = seg.start, end = seg.end,
                strokeWidth = baseWidth * 5f,
                blendMode = BlendMode.Plus,
            )
            // Schicht 2: mittlerer Cyan-Glow
            drawLine(
                color = glowInner.copy(alpha = segIntensity * 0.6f),
                start = seg.start, end = seg.end,
                strokeWidth = baseWidth * 2.5f,
                blendMode = BlendMode.Plus,
            )
            // Schicht 3: heller innerer Glow
            drawLine(
                color = packetPeak.copy(alpha = segIntensity * 0.85f),
                start = seg.start, end = seg.end,
                strokeWidth = baseWidth * 1.2f,
                blendMode = BlendMode.Plus,
            )
            // Schicht 4: scharfer weisser Kern
            drawLine(
                color = coreColor.copy(alpha = segIntensity),
                start = seg.start, end = seg.end,
                strokeWidth = baseWidth.coerceAtLeast(0.8f),
            )
        }
    }
}

private fun DrawScope.drawElectricLineFallback(
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

private fun DrawScope.drawCardBorderFallback(
    bounds: Rect,
    tSec: Float,
    coreColor: Color,
    glowInner: Color,
    glowMid: Color,
    glowOuter: Color,
) {
    val intensity = 0.7f + organicPulse(tSec) * 0.3f
    val crackle = crackleNoise(tSec, bounds.top)
    val cr = CornerRadius(16f, 16f)

    drawRoundRect(color = glowOuter, topLeft = Offset(bounds.left - 10f, bounds.top - 10f),
        size = Size(bounds.width + 20f, bounds.height + 20f),
        cornerRadius = CornerRadius(20f, 20f),
        style = Stroke(width = 22f), blendMode = BlendMode.Plus,
        alpha = (intensity * crackle).coerceIn(0f, 1f))
    drawRoundRect(color = glowMid.copy(alpha = 0.55f), topLeft = bounds.topLeft, size = bounds.size,
        cornerRadius = cr, style = Stroke(width = 12f), blendMode = BlendMode.Plus,
        alpha = (intensity * crackle).coerceIn(0f, 1f))
    drawRoundRect(color = glowInner.copy(alpha = 0.85f), topLeft = bounds.topLeft, size = bounds.size,
        cornerRadius = cr, style = Stroke(width = 5f), blendMode = BlendMode.Plus,
        alpha = (intensity * crackle).coerceIn(0f, 1f))
    drawRoundRect(color = coreColor, topLeft = bounds.topLeft, size = bounds.size,
        cornerRadius = cr, style = Stroke(width = 1.6f),
        alpha = (intensity * 0.85f * crackle + 0.15f).coerceIn(0f, 1f))
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

    val brush = Brush.radialGradient(
        0.00f to glowInner.copy(alpha = 0.6f * intensity),
        0.40f to glowMid.copy(alpha = 0.35f * intensity),
        1.00f to Color.Transparent,
        center = Offset(railX, y),
        radius = outerR,
    )
    drawCircle(brush = brush, radius = outerR, center = Offset(railX, y), blendMode = BlendMode.Plus)

    drawCircle(
        color = glowInner.copy(alpha = 0.65f * intensity),
        radius = innerR,
        center = Offset(railX, y),
        blendMode = BlendMode.Plus,
    )

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
