package de.frank.aufgaben.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
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
    Canvas(modifier) {
        val t = zeit.value / 1000f
        drawRect(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten)))
        val w = size.width
        val h = size.height
        val a = if (f.dunkel) 0.55f else 0.65f
        fun wolke(farbe: Color, cx: Float, cy: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = a), farbe.copy(alpha = 0f)), Offset(cx, cy), r), r, Offset(cx, cy))
        wolke(f.blob1, w * (0.2f + 0.12f * sin(t * 0.13f)), h * (0.12f + 0.06f * cos(t * 0.11f)), w * 0.75f)
        wolke(f.blob2, w * (0.9f + 0.1f * cos(t * 0.09f)), h * (0.42f + 0.08f * sin(t * 0.07f)), w * 0.7f)
        wolke(f.blob3, w * (0.25f + 0.15f * sin(t * 0.06f + 1f)), h * (0.85f + 0.05f * cos(t * 0.1f)), w * 0.8f)
        when (f.design) {
            Design.KOSMOS, Design.AURORA -> if (f.dunkel || f.design == Design.KOSMOS) sterne.forEachIndexed { i, (x, y, s) ->
                val funkeln = 0.35f + 0.65f * ((sin(t * (0.6f + s) + i) + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = (if (f.dunkel) 0.8f else 0.5f) * funkeln * s), 0.6f + s * 1.8f, Offset(x * w, y * h))
            }
            Design.GARTEN -> sterne.take(26).forEachIndexed { i, (x, y, s) ->
                val yy = ((y * h - t * (8f + s * 14f)) % h + h) % h
                val xx = x * w + sin(t * 0.5f + i) * 14f
                drawCircle((if (i % 3 == 0) f.sekundaer else f.primaer).copy(alpha = 0.28f), 2f + s * 3f, Offset(xx, yy))
            }
            Design.GLUT -> sterne.take(18).forEachIndexed { i, (x, y, s) ->
                val yy = ((y * h - t * (5f + s * 8f)) % h + h) % h
                drawCircle(Brush.radialGradient(listOf(f.blob3.copy(alpha = 0.35f), Color.Transparent), Offset(x * w, yy), 18f + s * 30f), 18f + s * 30f, Offset(x * w, yy))
            }
        }
    }
}
