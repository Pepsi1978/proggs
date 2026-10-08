package de.frank.modellkompass.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.frank.modellkompass.ui.theme.LocalBewegung
import de.frank.modellkompass.ui.theme.LocalFarben
import de.frank.modellkompass.ui.theme.antippen
import de.frank.modellkompass.ui.theme.glas
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

/** Bewegter Hintergrund: weiche Farbwolken und langsam aufsteigende Glutpunkte. */
@Composable
fun Hintergrund(modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val zeit = rememberSzenenZeit()
    val punkte = remember { List(18) { Triple(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat()) } }
    Canvas(modifier) {
        val t = zeit.value / 1000f
        drawRect(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten)))
        val w = size.width
        val h = size.height
        val a = if (f.dunkel) 0.32f else 0.5f
        fun wolke(farbe: Color, cx: Float, cy: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = a), farbe.copy(alpha = 0f)), Offset(cx, cy), r), r, Offset(cx, cy))
        wolke(f.blob1, w * (0.2f + 0.12f * sin(t * 0.13f)), h * (0.12f + 0.06f * cos(t * 0.11f)), w * 0.75f)
        wolke(f.blob2, w * (0.9f + 0.1f * cos(t * 0.09f)), h * (0.42f + 0.08f * sin(t * 0.07f)), w * 0.7f)
        wolke(f.blob3, w * (0.25f + 0.15f * sin(t * 0.06f + 1f)), h * (0.85f + 0.05f * cos(t * 0.1f)), w * 0.8f)
        punkte.forEach { (x, y, s) ->
            val yy = ((y * h - t * (5f + s * 8f)) % h + h) % h
            val r = 18f + s * 30f
            drawCircle(Brush.radialGradient(listOf(f.primaer.copy(alpha = if (f.dunkel) 0.16f else 0.12f), Color.Transparent), Offset(x * w, yy), r), r, Offset(x * w, yy))
        }
    }
}

/** Blendet eine Fläche beim Erscheinen von unten her räumlich ein. */
fun Modifier.einblenden(verzoegerung: Int = 0): Modifier = composed {
    val bewegung = LocalBewegung.current
    val a = remember { Animatable(if (bewegung) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (bewegung) { kotlinx.coroutines.delay(verzoegerung.toLong()); a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 180f)) }
    }
    graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 60f
        rotationX = (1f - v) * 18f
        cameraDistance = 16f * density
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

@Composable
fun RundKnopf(icon: ImageVector, beschreibung: String, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.padding(4.dp).size(44.dp).glas(f, 99.dp, 0.8f).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = f.text, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun Block(titel: String, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().einblenden(100).glas(f, erhoeht = 1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titel, color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        inhalt()
    }
}

@Composable
fun ChipReihe(inhalt: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { inhalt() }
}

@Composable
fun Schalter(text: String, an: Boolean, aendern: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().antippen(haptik = false) { aendern(!an) }, verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(an, aendern, colors = SwitchDefaults.colors(checkedTrackColor = f.primaer))
    }
}

@Composable
fun Feld(titel: String, wert: String, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    var text by remember { mutableStateOf(wert) }
    Column {
        Text(titel, color = f.textLeise, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).glas(f, 12.dp, 0f, f.flaecheStark).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                text, { text = it; aendern(it) }, singleLine = true,
                textStyle = TextStyle(color = f.text, fontSize = 15.sp),
                cursorBrush = SolidColor(f.primaer),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
