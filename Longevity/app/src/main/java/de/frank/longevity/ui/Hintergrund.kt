package de.frank.longevity.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import de.frank.longevity.ui.theme.Design
import de.frank.longevity.ui.theme.LocalBewegung
import de.frank.longevity.ui.theme.LocalFarben
import kotlin.math.cos
import kotlin.math.sin

/** Ob der Bildschirm vorne ist — Animationen laufen nur dann. */
@Composable
fun rememberVorne(): Boolean {
    val owner = LocalLifecycleOwner.current
    var vorne by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, _ -> vorne = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    return vorne
}

/**
 * Laufende Zeit in Millisekunden, ~30 Bilder/s. Gelesen wird sie nur im Zeichenblock — neu
 * gezeichnet wird so allein diese Ebene, nie die ganze Oberfläche.
 */
@Composable
fun rememberSzenenZeit(): State<Long> {
    val zeit = remember { mutableLongStateOf(0L) }
    val aktiv = rememberVorne() && LocalBewegung.current
    if (aktiv) LaunchedEffect(Unit) {
        var start = -1L
        var letzte = 0L
        while (true) {
            withFrameMillis { t ->
                if (start < 0) start = t - zeit.longValue
                if (t - letzte >= 32) { zeit.longValue = t - start; letzte = t }
            }
        }
    }
    return zeit
}

/** Bewegter Hintergrund: weiche Farbwolken plus die Partikel des Designs. */
@Composable
fun Hintergrund(modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val zeit = rememberSzenenZeit()
    val sterne = remember { List(70) { Triple(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat()) } }
    // Eigene Ebene: das 30-Bilder/s-Neuzeichnen bleibt auf den Hintergrund beschränkt.
    Box(modifier.graphicsLayer().drawWithCache {
        val hintergrund = Brush.verticalGradient(listOf(f.hgOben, f.hgUnten))
        val wolkenRadien = floatArrayOf(size.width * 0.75f, size.width * 0.7f, size.width * 0.8f)
        val wolken = listOf(f.blob1, f.blob2, f.blob3).mapIndexed { i, farbe ->
            Brush.radialGradient(
                listOf(farbe.copy(alpha = if (f.dunkel) 0.55f else 0.65f), farbe.copy(alpha = 0f)),
                Offset.Zero, wolkenRadien[i],
            )
        }
        // Verlauf und Radius bleiben gleich. Nur Position und Deckkraft bewegen sich pro Bild.
        val glanzRadien = if (f.design == Design.MORGENROT) List(if (f.dunkel) 10 else 28) { i ->
            val s = sterne[i].third
            if (f.dunkel) 10f + s * 14f else 12f + s * 18f
        } else emptyList()
        val glanz = glanzRadien.mapIndexed { i, r ->
            val farbe = if (f.dunkel || i % 3 != 0) f.tertiaer else f.primaer
            Brush.radialGradient(
                listOf(farbe.copy(alpha = if (f.dunkel) 0.45f else 0.32f), farbe.copy(alpha = 0f)),
                Offset.Zero, r,
            )
        }
        onDrawBehind {
            val t = zeit.value / 1000f
            drawRect(hintergrund)
            val w = size.width
            val h = size.height
            fun wolke(i: Int, cx: Float, cy: Float) = translate(cx, cy) {
                drawCircle(wolken[i], wolkenRadien[i], Offset.Zero)
            }
            wolke(0, w * (0.2f + 0.12f * sin(t * 0.13f)), h * (0.12f + 0.06f * cos(t * 0.11f)))
            wolke(1, w * (0.9f + 0.1f * cos(t * 0.09f)), h * (0.42f + 0.08f * sin(t * 0.07f)))
            wolke(2, w * (0.25f + 0.15f * sin(t * 0.06f + 1f)), h * (0.85f + 0.05f * cos(t * 0.1f)))
            // Helle Partikelfarbe: im Dunkeln fast weiß, im Hellen ein kräftiger Ton des Designs.
            val licht = if (f.dunkel) f.text else f.primaer
            when (f.design) {
                Design.MORGENROT -> if (f.dunkel) {
                    // Nachts: funkelnde Sterne und ein paar warme Glühwürmchen.
                    for (i in sterne.indices) {
                        val (x, y, s) = sterne[i]
                        val funkeln = 0.35f + 0.65f * ((sin(t * (0.6f + s) + i) + 1f) / 2f)
                        drawCircle(licht.copy(alpha = 0.75f * funkeln * s), 0.6f + s * 1.6f, Offset(x * w, y * h))
                    }
                    for (i in 0 until 10) {
                        val (x, y, s) = sterne[i]
                        val yy = ((y * h - t * (6f + s * 8f)) % h + h) % h
                        val xx = x * w + sin(t * 0.7f + i * 1.3f) * 20f
                        val flackern = 0.5f + 0.5f * sin(t * (1.5f + s * 2f) + i)
                        translate(xx, yy) { drawCircle(glanz[i], glanzRadien[i], Offset.Zero, alpha = flackern) }
                        drawCircle(f.tertiaer.copy(alpha = 0.8f * flackern), 1.6f + s * 1.4f, Offset(xx, yy))
                    }
                } else {
                    // Tagsüber: sanft aufsteigende warme Lichtpunkte.
                    for (i in 0 until 28) {
                        val (x, y, s) = sterne[i]
                        val yy = ((y * h - t * (7f + s * 12f)) % h + h) % h
                        val xx = x * w + sin(t * 0.45f + i * 1.7f) * 16f
                        val flackern = 0.55f + 0.45f * sin(t * (1.1f + s) + i)
                        val farbe = if (i % 3 == 0) f.primaer else f.tertiaer
                        translate(xx, yy) { drawCircle(glanz[i], glanzRadien[i], Offset.Zero, alpha = flackern) }
                        drawCircle(farbe.copy(alpha = 0.55f * flackern), 1.8f + s * 1.8f, Offset(xx, yy))
                    }
                }
                Design.LEBENSBAUM -> {
                    // Langsam fallende, sich drehende Blätter …
                    val spanne = h + 60f
                    for (i in 0 until 18) {
                        val (x, y, s) = sterne[i]
                        val yy = ((y * spanne + t * (10f + s * 12f)) % spanne) - 30f
                        val xx = x * w + sin(t * 0.6f + i * 1.1f) * (18f + s * 16f)
                        val lw = 12f + s * 12f
                        val farbe = when (i % 3) { 0 -> f.primaer; 1 -> f.sekundaer; else -> f.blob1 }
                        rotate(t * (25f + s * 40f) * (if (i % 2 == 0) 1f else -1f) + i * 37f, Offset(xx, yy)) {
                            drawOval(farbe.copy(alpha = if (f.dunkel) 0.32f else 0.38f), Offset(xx - lw / 2f, yy - lw / 4f), Size(lw, lw / 2f))
                            drawLine(farbe.copy(alpha = 0.45f), Offset(xx - lw / 2f, yy), Offset(xx + lw / 2f, yy), 1f)
                        }
                    }
                    // … und schwebende Pollen.
                    for (i in 18 until 40) {
                        val (x, y, s) = sterne[i]
                        val yy = ((y * h - t * (3f + s * 5f)) % h + h) % h
                        val xx = x * w + sin(t * 0.35f + i) * 24f
                        val funkeln = 0.4f + 0.6f * ((sin(t * (0.8f + s) + i) + 1f) / 2f)
                        drawCircle(f.sekundaer.copy(alpha = 0.4f * funkeln), 1.2f + s * 2f, Offset(xx, yy))
                    }
                }
                Design.ATEM -> {
                    // Aufsteigende Luftblasen, die leicht wabern.
                    val spanne = h + 60f
                    for (i in 0 until 24) {
                        val (x, y, s) = sterne[i]
                        val yy = spanne - ((y * spanne + t * (12f + s * 22f)) % spanne) - 30f
                        val xx = x * w + sin(t * (0.9f + s) + i * 1.9f) * (8f + s * 14f)
                        val r = 4f + s * 12f
                        val wabern = 0.08f * sin(t * 2.6f + i)
                        val bw = r * 2f * (1f + wabern)
                        val bh = r * 2f * (1f - wabern)
                        val farbe = if (i % 2 == 0) f.primaer else f.sekundaer
                        drawOval(farbe.copy(alpha = if (f.dunkel) 0.10f else 0.12f), Offset(xx - bw / 2f, yy - bh / 2f), Size(bw, bh))
                        drawOval(farbe.copy(alpha = 0.38f), Offset(xx - bw / 2f, yy - bh / 2f), Size(bw, bh), style = Stroke(1.4f))
                        drawCircle(licht.copy(alpha = if (f.dunkel) 0.55f else 0.35f), r * 0.22f, Offset(xx - r * 0.35f, yy - r * 0.35f))
                    }
                }
                Design.HELIX -> {
                    // Funkelnde Sterne …
                    for (i in sterne.indices) {
                        val (x, y, s) = sterne[i]
                        val funkeln = 0.3f + 0.7f * ((sin(t * (0.7f + s * 1.3f) + i * 2.1f) + 1f) / 2f)
                        drawCircle(licht.copy(alpha = (if (f.dunkel) 0.75f else 0.35f) * funkeln * s), 0.6f + s * 1.6f, Offset(x * w, y * h))
                    }
                    // … plus feine schwebende Partikel.
                    for (i in 0 until 26) {
                        val (x, y, s) = sterne[i]
                        val xx = ((x * w + t * (4f + s * 7f)) % w + w) % w
                        val yy = y * h + sin(t * 0.5f + i * 1.4f) * 20f
                        val farbe = if (i % 2 == 0) f.tertiaer else f.sekundaer
                        drawCircle(farbe.copy(alpha = if (f.dunkel) 0.45f else 0.35f), 1f + s * 1.3f, Offset(xx, yy))
                    }
                }
            }
        }
    })
}
