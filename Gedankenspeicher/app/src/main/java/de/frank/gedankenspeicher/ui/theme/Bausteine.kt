package de.frank.gedankenspeicher.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * **Die Tiefe aus `02-UI-SPEC.md` §5.**
 *
 * Der Entwurf lebt davon, dass Karten schweben und Leisten aus Glas sind. Beides steht hier
 * als eigener Modifier, nicht in jedem Bildschirm neu: sonst wäre der Schatten der einen
 * Karte irgendwann ein anderer als der der nächsten, und die Tiefe zerfiele.
 */

/** Eine schwebende Karte: Schatten, leiser Verlauf, Rand (`.notizkarte`). */
fun Modifier.schwebendeKarte(
    farben: Farbrollen,
    radius: Dp = 20.dp,
    randfarbe: Color? = null,
    randstaerke: Dp = 1.dp,
    grundfarbe: Color? = null,
): Modifier {
    val form: Shape = RoundedCornerShape(radius)
    val grund = grundfarbe ?: farben.hintergrundErhoben
    val schattenfarbe = if (farben.istDunkel) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.08f)
    return this
        .shadow(
            elevation = 6.dp,
            shape = form,
            ambientColor = schattenfarbe,
            spotColor = schattenfarbe,
        )
        .background(
            // 145°-Verlauf von der Grundfläche zu einer um 4 % helleren — das ist die
            // "leise" Tiefe des Specs: sichtbar an der Kante, unsichtbar unter dem Text.
            brush = Brush.linearGradient(
                colors = listOf(grund, lerp(grund, Color.White, 0.04f)),
                start = Offset.Zero,
                end = Offset(320f, 460f),
            ),
            shape = form,
        )
        .border(randstaerke, randfarbe ?: farben.rand, form)
}

/**
 * Eine Glasleiste (`header.kopfleiste`, `footer.fussleiste`).
 *
 * Ohne echte Rückwärts-Unschärfe: `Modifier.blur` verwischt den eigenen Inhalt, nicht das
 * Dahinterliegende, und `RenderEffect` mit Backdrop gibt es erst ab Android 12 zuverlässig.
 * Die Leiste zeichnet deshalb den halbdurchsichtigen Glaston — der Verlauf darunter scheint
 * hindurch, und das ist der Eindruck, um den es geht.
 */
fun Modifier.glasleiste(
    farben: Farbrollen,
    form: Shape = RoundedCornerShape(0.dp),
): Modifier = this.background(farben.hintergrundGlas, form)

/**
 * Die Aura des Aufnahmeknopfs — ein Radialverlauf, der nach außen ausläuft.
 *
 * [staerke] steuert Größe und Deckkraft: 0f im Ruhezustand, im Aufnahmezustand der gemessene
 * Pegel (M-05). Damit wird aus der Zierde eine Anzeige.
 */
fun Modifier.aura(farbe: Color, staerke: Float): Modifier = this.drawBehind {
    if (staerke <= 0f) return@drawBehind
    val halbmesser = size.minDimension / 2f * (1f + staerke * 0.45f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                farbe.copy(alpha = 0.50f * staerke),
                farbe.copy(alpha = 0.12f * staerke),
                Color.Transparent,
            ),
            center = center,
            radius = halbmesser,
        ),
        radius = halbmesser,
        center = center,
    )
}

/**
 * **M-07 — das wandernde Leuchten.**
 *
 * Ein Streifen von 40 % Breite zieht in 2400 ms von −40 % nach 140 % über die Fläche. Bei
 * reduzierter Bewegung wird daraus eine ruhige Fläche: die Information "hier arbeitet
 * gerade jemand" bleibt, die Bewegung geht.
 */
@Composable
fun Modifier.wanderndesLeuchten(farbe: Color, aktiv: Boolean): Modifier {
    if (!aktiv) return this
    if (bewegungReduziert()) return this.background(farbe)
    val takt = rememberInfiniteTransition(label = "leuchten")
    val stelle by takt.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(Dauern.WANDERN, easing = Kurven.wandern),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stelle",
    )
    return this.drawBehind {
        val breite = size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, farbe, Color.Transparent),
                startX = (stelle - 0.2f) * breite,
                endX = (stelle + 0.2f) * breite,
            ),
        )
    }
}

/**
 * Der Schimmer auf Platzhaltern (`03-MOTION-SPEC.md` §7). Dieselbe Bewegung wie M-07, nur
 * über eine leere Fläche statt über eine Karte.
 */
@Composable
fun Modifier.schimmer(farben: Farbrollen, form: Shape = RoundedCornerShape(20.dp)): Modifier =
    this.background(farben.hintergrundErhoben, form).wanderndesLeuchten(farben.akzentGedeckt, aktiv = true)

/**
 * **Der Druckzustand** (`03-MOTION-SPEC.md` §5): einsinken beim Drücken, zurück beim
 * Loslassen — in 120 ms.
 */
@Composable
fun Modifier.sinktEin(
    quelle: MutableInteractionSource,
    auf: Float = 0.94f,
): Modifier {
    val gedrueckt by quelle.collectIsPressedAsState()
    val ziel = if (gedrueckt) auf else 1f
    val wert by androidx.compose.animation.core.animateFloatAsState(
        targetValue = ziel,
        animationSpec = tween(dauer(Dauern.KURZ), easing = Kurven.kurz),
        label = "druck",
    )
    return this.scale(wert)
}

/** Kurzform für einen Interaktionsquell, den man nur für den Druckzustand braucht. */
@Composable
fun merkeDruck(): MutableInteractionSource = remember { MutableInteractionSource() }

/** Ein Kasten, der bei reduzierter Bewegung nichts pulsieren lässt (M-05). */
@Composable
fun PulsierenderRing(
    aktiv: Boolean,
    pegel: Float,
    farbe: Color,
    inhalt: @Composable BoxScope.() -> Unit,
) {
    val reduziert = bewegungReduziert()
    val staerke = when {
        !aktiv -> 0f
        reduziert -> 1f // statischer Ring in voller Deckkraft (§8, Punkt 1)
        else -> (0.12f + pegel * 0.88f).coerceIn(0.12f, 1f)
    }
    val takt = rememberInfiniteTransition(label = "puls")
    val atem by takt.animateFloat(
        initialValue = 1f,
        targetValue = if (aktiv && !reduziert) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Dauern.PULS, easing = Kurven.puls),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atem",
    )
    Box(
        modifier = Modifier
            .scale(if (aktiv && !reduziert) atem else 1f)
            .aura(farbe, staerke),
        content = inhalt,
    )
}
