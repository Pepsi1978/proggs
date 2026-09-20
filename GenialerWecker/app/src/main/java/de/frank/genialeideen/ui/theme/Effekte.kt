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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.addOutline
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

/**
 * Der flache Glanzbogen im oberen Drittel (N.2, Schicht 4).
 *
 * **Achtung, hier lag ein Fehler:** `Brush.radialGradient` erwartet `center` und `radius` in
 * **Pixeln**, nicht in Anteilen der Fläche. Die frühere Fassung setzte `center = Offset(0.35f,
 * -0.35f)` und `radius = 1.1f` — also einen Radius von gut einem Pixel dicht an der linken
 * oberen Ecke. Der Glanz war damit auf keinem Gerät zu sehen, und weil [glanzLicht] in jedem
 * plastischen Knopf steckt, fehlte der gesamten App ihre oberste Lichtschicht.
 *
 * Als [Modifier] statt als [Brush], weil die Größe erst beim Zeichnen feststeht.
 */
fun Modifier.glanzBogen(deckung: Float = 0.40f): Modifier = drawWithCache {
    val pinsel = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = deckung), Color.Transparent),
        center = Offset(size.width * 0.35f, -size.height * 0.35f),
        radius = size.width * 1.1f,
    )
    onDrawBehind { drawRect(pinsel) }
}

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
    // Bei reduzierter Bewegung lagen Start- und Zielwert zwar beide auf 0,25 — die Endlos-
    // Animation lief aber trotzdem und rechnete Bild für Bild denselben Wert aus. Auf dem
    // feststehenden Kopf ist das ein Dauerticker ohne jede sichtbare Wirkung. Jetzt entsteht
    // sie gar nicht erst, wenn nichts zu atmen ist.
    val atem = if (aktiv && !reduziert) {
        val uebergang = rememberInfiniteTransition(label = "schein")
        uebergang.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(Motion.ATEM_MS), RepeatMode.Reverse),
            label = "atemwert",
        ).value
    } else {
        0.25f
    }
    if (hoehe > 0.dp) {
        graphicsLayer {
            shadowElevation = hoehe.toPx()
            shape = form
            clip = true
            ambientShadowColor = farbe.copy(alpha = atem)
            spotShadowColor = farbe.copy(alpha = atem)
        }
    } else {
        this
    }
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
    /**
     * Stärke der Körnung; 0 lässt sie ganz weg. Orbit schließt Körnung in seinem Material
     * ausdrücklich aus — vorher war sie hier fest verdrahtet und kam über die Kopfleiste
     * trotzdem an.
     */
    koernungAlpha: Float = 0.04f,
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
    // Der Reflex lief früher über feste 220 Pixel — auf einer breiten Leiste ein Fleck in der
    // Ecke, auf einem schmalen Gerät die halbe Fläche. Jetzt folgt er der tatsächlichen Größe.
    .gerichteterReflex(Color.White, 0.12f, winkelGrad = 45f, laenge = 0.6f)
    .then(if (koernungAlpha > 0f) Modifier.koernung(koernungAlpha) else Modifier)
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
    // Compose drawPoints(List<Offset>) kopiert auf Android bei jedem Draw alle Punkte
    // in ein neues FloatArray. Rohkoordinaten und Paint stattdessen einmal vorbereiten.
    val koordinaten = FloatArray(punkte.size * 2)
    punkte.forEachIndexed { index, punkt ->
        koordinaten[index * 2] = punkt.x
        koordinaten[index * 2 + 1] = punkt.y
    }
    val stift = Paint().apply {
        color = Color.White.copy(alpha = deckung * 0.85f)
        strokeWidth = 1.2f
        strokeCap = StrokeCap.Round
    }
    // Hinter den Inhalt, nicht darüber: Vorher lag die Körnung als letzte Schicht auf Text und
    // Symbolen und nahm ihnen die Schärfe. Sie gehört unter den Inhalt, wie jede Materialschicht.
    onDrawBehind {
        if (koordinaten.isNotEmpty()) {
            drawIntoCanvas { canvas ->
                canvas.drawRawPoints(androidx.compose.ui.graphics.PointMode.Points, koordinaten, stift)
            }
        }
    }
}

/** Wandernder Glanz über wichtige Karten und Überschriften (N.7). */
fun Modifier.wanderndesGlanzlicht(breite: Float = 900f): Modifier = composed {
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
    val farben = remember(gold) {
        listOf(gold.primaer, gold.primaer.heller(0.55f), gold.akzentWarm, gold.primaer)
    }
    // Nur die Schriftmaske neu einfärben. Ein animierter Brush im TextStyle invalidiert
    // dagegen pro Frame die gesamte Kopfleiste inklusive Glastextur und Textlayout.
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.linearGradient(
                    colors = farben,
                    start = Offset(versatz, 0f),
                    end = Offset(versatz + breite / 2f, 120f),
                ),
                blendMode = BlendMode.SrcIn,
            )
        }
}

// --- Die gemeinsame Tiefensprache ------------------------------------------------------------
//
// Alle folgenden Bausteine sind bewusst **designneutral**: Sie bekommen ihre Werte übergeben und
// legen nur Weiß- beziehungsweise Schwarz-Alpha über eine bereits deckende Grundfläche. Dadurch
// verschiebt keiner von ihnen einen Farbton — Schlichts Goldwerte bleiben unangetastet, und jedes
// Design behält seine Handschrift über die Werte, nicht über eigenen Zeichencode.
//
// Alle zeichnen mit `onDrawBehind`, damit Text und Symbole darüber scharf bleiben, und bereiten
// ihre Pinsel in `drawWithCache` genau einmal je Größe vor statt bei jeder Neuzeichnung.
//
// **Wichtig für Aufrufer:** [tiefenVerlauf], [gerichteterReflex] und [vignette] füllen das volle
// Rechteck der Fläche. Sie gehören deshalb **hinter** ein `Modifier.clip(form)`, sonst malen sie
// über abgerundete Ecken hinaus. Die Schichtreihenfolge im Materialsystem sieht genau das vor:
// erst `clip`, dann Grundfarbe, dann diese Schichten, zuletzt die Kante. [innenSchatten] und
// [nut] schneiden sich dagegen selbst an der übergebenen Form ab.

/**
 * Der senkrechte Tiefenverlauf einer Fläche: oben etwas Licht, unten etwas Schatten.
 * [gedrueckt] dreht ihn um — daraus liest das Auge „eingedrückt" statt „erhaben".
 */
fun Modifier.tiefenVerlauf(oben: Float, unten: Float, gedrueckt: Boolean = false): Modifier =
    drawWithCache {
        val farben = listOf(
            Color.White.copy(alpha = oben),
            Color.Transparent,
            Color.Black.copy(alpha = unten),
        )
        val pinsel = Brush.verticalGradient(if (gedrueckt) farben.asReversed() else farben)
        onDrawBehind { drawRect(pinsel) }
    }

/**
 * Ein gerichteter Reflex über die Fläche — das, was Glas von Farbe unterscheidet.
 *
 * [winkelGrad] zählt von der Waagerechten, [laenge] ist ein Anteil der Fläche. Beides relativ,
 * damit derselbe Reflex auf einer Kopfleiste und auf einem kleinen Chip gleich wirkt.
 */
fun Modifier.gerichteterReflex(
    farbe: Color,
    alpha: Float,
    winkelGrad: Float,
    laenge: Float,
): Modifier = drawWithCache {
    val bogen = Math.toRadians(winkelGrad.toDouble())
    val ende = Offset(
        (size.width * laenge * kotlin.math.cos(bogen)).toFloat(),
        (size.height * laenge * kotlin.math.sin(bogen)).toFloat(),
    )
    val pinsel = Brush.linearGradient(
        colors = listOf(farbe.copy(alpha = alpha), farbe.copy(alpha = 0f)),
        start = Offset.Zero,
        end = ende,
    )
    onDrawBehind { drawRect(pinsel) }
}

/**
 * Ein innerer Schatten ohne Weichzeichner — damit Eingabefelder, Reglerbahnen und gedrückte
 * Knöpfe sichtbar **unter** der Oberfläche liegen.
 *
 * Compose kennt bis einschließlich der hier verwendeten Fassung keinen inneren Schatten, und
 * `BlurMaskFilter` ist mit Hardwarebeschleunigung unterhalb von API 28 nicht verlässlich. Deshalb
 * vier Konturstriche mit abnehmender Deckung, innen an der Form abgeschnitten: kein Offscreen-
 * Puffer, keine Versionsabhängigkeit, und der Inhalt darüber bleibt scharf.
 */
fun Modifier.innenSchatten(
    form: Shape,
    alpha: Float,
    tiefe: Dp = 6.dp,
    farbe: Color = Color.Black,
): Modifier = drawWithCache {
    val umriss = form.createOutline(size, layoutDirection, this)
    val pfad = Path().apply { addOutline(umriss) }
    val tiefePx = tiefe.toPx()
    // Pinsel und Strichbreiten werden **einmal je Größe** angelegt, nicht bei jedem Bild. Vorher
    // entstanden hier pro Zeichenvorgang vier Verlaufspinsel samt Farblisten und vier
    // Stroke-Objekte — bei einem Element, das in jeder Liste mehrfach vorkommt, ist das
    // vermeidbarer Müll. (Kein gemessener Bildratenwert, nur die eingesparte Zuteilung.)
    val ringe = listOf(0.25f to alpha, 0.5f to alpha * 0.6f, 0.75f to alpha * 0.35f, 1f to alpha * 0.15f)
        .map { (anteil, a) ->
            // Oben kräftiger als unten: Das Licht kommt in der ganzen App von oben links.
            Brush.verticalGradient(listOf(farbe.copy(alpha = a), farbe.copy(alpha = a * 0.15f))) to
                Stroke(width = tiefePx * anteil * 2f)
        }
    onDrawBehind {
        clipPath(pfad) {
            ringe.forEach { (pinsel, strich) -> drawPath(pfad, brush = pinsel, style = strich) }
        }
    }
}

/**
 * Die Materialkante: Licht oben, Schatten unten, in der Farbe des jeweiligen Designs.
 * Sie gehört als **letzte** Schicht über Verlauf und Reflex, sonst verschluckt der Reflex sie.
 */
fun materialKante(
    lichtFarbe: Color,
    lichtAlpha: Float,
    schattenAlpha: Float,
    gedrueckt: Boolean = false,
): Brush = Brush.verticalGradient(
    if (gedrueckt) {
        listOf(Color.Black.copy(alpha = schattenAlpha), Color.Transparent, lichtFarbe.copy(alpha = lichtAlpha))
    } else {
        listOf(lichtFarbe.copy(alpha = lichtAlpha), Color.Transparent, Color.Black.copy(alpha = schattenAlpha))
    },
)

/**
 * Eine feine dunkle Innenlinie dicht hinter der Kante — die Fräsnut einer Instrumententafel.
 * Nur Orbit trägt sie; sie ist das, was dort „aus dem Vollen gefräst" statt „gedruckt" aussehen lässt.
 */
fun Modifier.nut(form: Shape, alpha: Float = 0.25f, einzug: Dp = 2.dp): Modifier = drawWithCache {
    val einzugPx = einzug.toPx()
    val innen = androidx.compose.ui.geometry.Size(
        (size.width - einzugPx * 2).coerceAtLeast(0f),
        (size.height - einzugPx * 2).coerceAtLeast(0f),
    )
    val pfad = Path().apply { addOutline(form.createOutline(innen, layoutDirection, this@drawWithCache)) }
    onDrawBehind {
        translate(einzugPx, einzugPx) {
            drawPath(pfad, Color.Black.copy(alpha = alpha), style = Stroke(width = 1.dp.toPx()))
        }
    }
}

/**
 * Eine Vignette für den Seitenhintergrund: zu den Ecken hin etwas dunkler.
 * Sie kostet nichts und gibt der ganzen Seite eine Wölbung, auf der die Karten erst aufliegen.
 */
fun Modifier.vignette(alpha: Float): Modifier = drawWithCache {
    val pinsel = Brush.radialGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = alpha)),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = kotlin.math.max(size.width, size.height) * 0.75f,
    )
    onDrawBehind { drawRect(pinsel) }
}
