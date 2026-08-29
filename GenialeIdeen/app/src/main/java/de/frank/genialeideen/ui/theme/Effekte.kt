package de.frank.genialeideen.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Die wiederkehrenden Effekte aus Baustein N — einmal gebaut, überall benutzt.
 *
 * Die Beleuchtung sitzt in der ganzen App **oben links**: Lichtkanten oben und links,
 * Schatten unten und rechts (N.3). Wer davon abweicht, zerstört den räumlichen Eindruck.
 */

/** Hellt eine Farbe auf, ohne den Farbton zu verlieren. */
fun Color.heller(anteil: Float): Color = Color(
    red = red + (1f - red) * anteil,
    green = green + (1f - green) * anteil,
    blue = blue + (1f - blue) * anteil,
    alpha = alpha,
)

/** Dunkelt eine Farbe ab. */
fun Color.dunkler(anteil: Float): Color = Color(
    red = red * (1f - anteil),
    green = green * (1f - anteil),
    blue = blue * (1f - anteil),
    alpha = alpha,
)

fun Color.mischeMit(andere: Color, anteil: Float): Color = Color(
    red = red * (1 - anteil) + andere.red * anteil,
    green = green * (1 - anteil) + andere.green * anteil,
    blue = blue * (1 - anteil) + andere.blue * anteil,
    alpha = alpha,
)

/**
 * Zwei bis drei Schatten übereinander statt einem — Kontaktschatten dicht am Element,
 * weicher Umgebungsschatten dahinter, beide in der Elementfarbe statt in Schwarz (N.4).
 */
fun Modifier.tiefenSchatten(
    farbe: Color,
    hoehe: Dp,
    form: Shape,
    gedrueckt: Boolean = false,
): Modifier {
    val faktor = if (gedrueckt) 0.25f else 1f
    return this
        .shadow(
            elevation = hoehe * faktor,
            shape = form,
            ambientColor = farbe.copy(alpha = Hoehe.UMGEBUNG_ALPHA),
            spotColor = farbe.copy(alpha = 0.55f),
        )
        .shadow(
            elevation = Hoehe.kontakt * faktor,
            shape = form,
            ambientColor = Color.Black.copy(alpha = Hoehe.KONTAKT_ALPHA),
            spotColor = Color.Black.copy(alpha = Hoehe.KONTAKT_ALPHA),
        )
}

/** Der senkrechte Körperverlauf eines plastischen Elements: oben hell, unten dunkel (N.2). */
fun koerperVerlauf(grund: Color, gedrueckt: Boolean = false): Brush {
    val hell = grund.heller(0.18f)
    val dunkel = grund.dunkler(0.22f)
    return if (gedrueckt) {
        Brush.verticalGradient(listOf(dunkel, grund, hell))
    } else {
        Brush.verticalGradient(listOf(hell, grund, dunkel))
    }
}

/** Lichtkante oben, dunklere Kante unten — der Unterschied zwischen Rechteck und Knopf (N.2). */
fun lichtKante(gedrueckt: Boolean = false, staerke: Float = 0.45f): Brush = Brush.verticalGradient(
    if (gedrueckt) {
        listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.White.copy(alpha = staerke))
    } else {
        listOf(Color.White.copy(alpha = staerke), Color.Transparent, Color.Black.copy(alpha = 0.28f))
    },
)

/** Der flache Glanzbogen im oberen Drittel (N.2, Schicht 4). */
fun glanzLicht(deckung: Float = 0.40f): Brush = Brush.radialGradient(
    colors = listOf(Color.White.copy(alpha = deckung), Color.Transparent),
    center = Offset(0.35f, -0.35f),
    radius = 1.1f,
)

/**
 * Atmender goldener Schein — für den wichtigsten Knopf eines Bildschirms und aktive Karten
 * (N.7, Alpha 0,25 bis 0,45 im 2,5-Sekunden-Rhythmus).
 */
fun Modifier.pulsierenderSchein(
    farbe: Color,
    form: Shape,
    aktiv: Boolean = true,
    hoehe: Dp = 18.dp,
): Modifier = composed {
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "schein")
    val atem by uebergang.animateFloat(
        initialValue = 0.25f,
        targetValue = if (aktiv && !reduziert) 0.45f else 0.25f,
        animationSpec = infiniteRepeatable(tween(Motion.ATEM_MS), RepeatMode.Reverse),
        label = "atemwert",
    )
    shadow(
        elevation = hoehe,
        shape = form,
        ambientColor = farbe.copy(alpha = atem),
        spotColor = farbe.copy(alpha = atem),
    )
}

/** Schwebt langsam 2 bis 3 dp auf und ab — für den Aktionsknopf und Abzeichen (N.7). */
fun Modifier.schwebend(hub: Float = 3f, aktiv: Boolean = true): Modifier = composed {
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "schweben")
    val phase by uebergang.animateFloat(
        initialValue = 0f,
        targetValue = if (aktiv && !reduziert) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(tween(Motion.SCHWEBEN_MS), RepeatMode.Restart),
        label = "phase",
    )
    graphicsLayer { translationY = sin(phase) * hub * density }
}

/**
 * Kurzes seitliches Wackeln bei einem Fehler — drei Ausschläge in 320 ms (N.7).
 * [ausloeser] anzustossen genügt; jeder neue Wert lässt es erneut wackeln.
 */
fun Modifier.wackelnBeiFehler(ausloeser: Any?): Modifier = composed {
    val reduziert = LocalBewegungReduziert.current
    var versatz by remember { mutableFloatStateOf(0f) }
    val animiert by animateFloatAsState(versatz, tween(0), label = "wackelwert")
    LaunchedEffect(ausloeser) {
        if (ausloeser == null || reduziert) return@LaunchedEffect
        val schritte = listOf(10f, -8f, 6f, -4f, 2f, 0f)
        schritte.forEach { wert ->
            versatz = wert
            kotlinx.coroutines.delay(Motion.WACKELN_MS / schritte.size.toLong())
        }
    }
    graphicsLayer { translationX = animiert * density }
}

/**
 * Kipp-Effekt auf Berührung: Die Karte neigt sich zum Finger, höchstens 6 Grad,
 * und federt danach zurück (N.3).
 */
/**
 * Achtung: Der Kipp-Effekt greift Zieh-Gesten ab und darf deshalb **nie** an eine Karte
 * innerhalb einer Scroll-Liste. Dort stiehlt er dem Scrollen die Finger und die Liste hakt.
 */
fun Modifier.kippKarte(aktiv: Boolean = true, maxGrad: Float = 6f): Modifier = composed {
    val reduziert = LocalBewegungReduziert.current
    var kippX by remember { mutableFloatStateOf(0f) }
    var kippY by remember { mutableFloatStateOf(0f) }
    val weichX by animateFloatAsState(kippX, Motion.zustand(reduziert), label = "kippX")
    val weichY by animateFloatAsState(kippY, Motion.zustand(reduziert), label = "kippY")
    if (!aktiv || reduziert) {
        this
    } else {
        this
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { kippX = 0f; kippY = 0f },
                    onDragCancel = { kippX = 0f; kippY = 0f },
                ) { aenderung, _ ->
                    val mitteX = size.width / 2f
                    val mitteY = size.height / 2f
                    kippY = ((aenderung.position.x - mitteX) / mitteX).coerceIn(-1f, 1f) * maxGrad
                    kippX = -((aenderung.position.y - mitteY) / mitteY).coerceIn(-1f, 1f) * maxGrad
                }
            }
            .graphicsLayer {
                rotationX = weichX
                rotationY = weichY
                cameraDistance = 12f * density
            }
    }
}

/**
 * Milchglas mit Körnung (N.4). `Modifier.blur` wirkt erst ab Android 12 — darunter bleibt
 * die halbtransparente Fläche mit Verlauf, nie ein leeres Bild.
 */
fun Modifier.milchglas(
    flaeche: Color,
    form: Shape,
    deckung: Float = 0.62f,
    kante: Boolean = true,
): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            listOf(
                flaeche.copy(alpha = deckung + 0.08f),
                flaeche.copy(alpha = deckung),
            ),
        ),
        shape = form,
    )
    .background(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
            start = Offset.Zero,
            end = Offset(220f, 220f),
        ),
        shape = form,
    )
    .koernung()
    // Randlos, wo die Fläche bis an den Bildschirmrand läuft: Sonst stünde die helle Kante
    // im Dunkelmodus als weisser Strich ganz oben und an der Seite.
    .then(if (kante) Modifier.border(1.dp, lichtKante(staerke = 0.30f), form) else Modifier)

/**
 * Sehr feine Rausch-Textur über dem Glas — nimmt dem Verlauf die Plastikwirkung (N.4).
 * Bewusst gezeichnet statt als Bild: eine Textur im Projekt wäre schwerer und sähe bei
 * jeder Bildschirmdichte anders aus.
 */
fun Modifier.koernung(deckung: Float = 0.04f): Modifier = drawWithCache {
    // Die Punkte werden **einmal je Grösse** berechnet und danach in einem einzigen
    // drawPoints-Aufruf gezeichnet. Vorher lief die Doppelschleife mit tausenden
    // Einzelkreisen bei *jedem* Bild — das war die teuerste Stelle der ganzen Oberfläche.
    val schritt = 3f
    val punkte = ArrayList<Offset>(((size.width / schritt) * (size.height / schritt) / 3).toInt().coerceAtLeast(16))
    var y = 0f
    var zaehler = 0
    while (y < size.height) {
        var x = (zaehler % 2) * schritt / 2f
        while (x < size.width) {
            // Ein billiger, aber gleichmässig streuender Pseudozufall — kein Random je Bild,
            // sonst flimmert die Körnung bei jeder Neuzeichnung.
            val zufall = abs(sin(x * 12.9898f + y * 78.233f) * 43758.547f) % 1f
            if (zufall > 0.75f) punkte += Offset(x, y)
            x += schritt
        }
        y += schritt
        zaehler++
    }
    val farbe = Color.White.copy(alpha = deckung * 0.85f)
    onDrawWithContent {
        drawContent()
        if (punkte.isNotEmpty()) {
            drawPoints(
                points = punkte,
                pointMode = PointMode.Points,
                color = farbe,
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Wandernder Glanz über wichtige Karten und Überschriften (N.7). */
@Composable
fun wanderndesGlanzlicht(breite: Float = 900f): Brush {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "glanz")
    val versatz by uebergang.animateFloat(
        initialValue = -breite,
        targetValue = if (reduziert) -breite else breite,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = Motion.GLANZ_MS
                -breite at 0
                breite at Motion.GLANZ_MS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "glanzversatz",
    )
    return Brush.linearGradient(
        colors = listOf(gold.primaer, gold.primaer.heller(0.55f), gold.akzentWarm, gold.primaer),
        start = Offset(versatz, 0f),
        end = Offset(versatz + breite / 2f, 120f),
    )
}
