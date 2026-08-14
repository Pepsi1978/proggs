package de.frank.stacklabor.ui.bausteine

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift

/**
 * Eine Karte, wie sie im ganzen Entwurf vorkommt.
 *
 * Gemessen: Fläche `--flaeche`, Radius 12 dp, Rand 1 dp `--rand`,
 * Schatten `0 2px 6px rgba(15,23,42,.10)`. Die Glanzkante (M-16) wandert alle 8 Sekunden
 * darüber und macht aus der Fläche Material.
 */
@Composable
fun Karte(
    modifier: Modifier = Modifier,
    form: Shape = Formen.karte,
    mitGlanz: Boolean = true,
    inhalt: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(form)
            .background(Farben.flaeche)
            .border(1.dp, Farben.rand, form)
            .then(if (mitGlanz) Modifier.glanzkante() else Modifier),
        content = inhalt,
    )
}

/**
 * Der Kopfbereich von B-01 mit dem wandernden Farbverlauf.
 *
 * Gemessen: `linear-gradient(110deg, #4F46E5, #0EA5E9, #4F46E5)`, `@keyframes wandern`,
 * **30 s linear endlos**. Das ist die einzige Dauerbewegung im oberen Bildschirmdrittel —
 * langsam genug, dass sie nicht vom Inhalt ablenkt, aber sichtbar genug, dass die App lebt.
 */
@Composable
fun Modifier.wandernderVerlauf(): Modifier {
    val reduziert = LocalBewegungReduziert.current
    val a = Farben.akzent
    val b = Farben.akzent2
    if (reduziert) {
        return this.drawBehind {
            drawRect(
                Brush.linearGradient(
                    listOf(a.copy(alpha = 0.10f), b.copy(alpha = 0.10f)),
                    start = Offset(0f, 0f), end = Offset(size.width, size.height),
                ),
            )
        }
    }
    val takt = rememberInfiniteTransition(label = "wandern")
    val lauf by takt.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(Dauer.WANDERN, easing = Kurven.linear), RepeatMode.Reverse,
        ),
        label = "wandernLauf",
    )
    return this.drawBehind {
        val breite = size.width
        val versatz = -breite + lauf * 2f * breite
        drawRect(
            Brush.linearGradient(
                colorStops = arrayOf(
                    0f to a.copy(alpha = 0.14f),
                    0.5f to b.copy(alpha = 0.14f),
                    1f to a.copy(alpha = 0.14f),
                ),
                start = Offset(versatz, 0f),
                end = Offset(versatz + breite * 2f, size.height),
            ),
        )
    }
}

/**
 * Eine Glasfläche — Kopfleiste und Sockel.
 *
 * Der Weichzeichner liegt **nur** auf festen Flächen, nie über der scrollenden Liste:
 * Das ist der einzige Effekt, der auf 120 Hz messbar ins Stocken führt.
 */
@Composable
fun Modifier.glasflaeche(): Modifier = this.background(Farben.glas)

/** Die Verlaufskante über dem Sockel, damit die Liste sichtbar darunter verschwindet (4 dp). */
@Composable
fun VerlaufsKante(modifier: Modifier = Modifier) {
    val farbe = Farben.text.copy(alpha = 0.10f)
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, farbe)))
            },
    )
}

/** Ein Symbolknopf in voller Tippfläche (44 dp), wie überall im Entwurf. */
@Composable
fun SymbolKnopf(
    symbol: ImageVector,
    beschreibung: String,
    modifier: Modifier = Modifier,
    groesse: Dp = 22.dp,
    farbe: Color = Farben.text,
    aufKlick: () -> Unit,
) {
    IconButton(onClick = aufKlick, modifier = modifier.size(Masse.tippflaeche)) {
        Icon(symbol, contentDescription = beschreibung, tint = farbe, modifier = Modifier.size(groesse))
    }
}

/**
 * Ein Chip, wie ihn die Sortierleiste und der Dosis-Umschalter benutzen.
 * Aktiv: Fläche `--akz`, Text weiß. Inaktiv: nur ein 1-dp-Rand.
 */
@Composable
fun Chip(
    beschriftung: String,
    aktiv: Boolean,
    modifier: Modifier = Modifier,
    aufKlick: () -> Unit,
) {
    val flaeche by animateColorAsState(
        if (aktiv) Farben.akzent else Color.Transparent,
        tween(Dauer.ZUSTAND, easing = Kurven.zustand), label = "chipFlaeche",
    )
    val schrift by animateColorAsState(
        if (aktiv) (if (Farben.istHell) Color.White else Farben.grund) else Farben.textSchwach,
        tween(Dauer.ZUSTAND, easing = Kurven.zustand), label = "chipSchrift",
    )
    Box(
        modifier
            .height(28.dp)
            .clip(Formen.chip)
            .background(flaeche)
            .border(if (aktiv) 0.dp else 1.dp, if (aktiv) Color.Transparent else Farben.rand, Formen.chip)
            .clickable(onClick = aufKlick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(beschriftung, style = Schrift.grundMittel, color = schrift)
    }
}

/** Der schwebende Plus-Knopf: 57 dp, Radius 28, atmet mit 3,2 s (M-16). */
@Composable
fun PlusKnopf(
    beschreibung: String,
    modifier: Modifier = Modifier,
    aufKlick: () -> Unit,
) {
    val reduziert = LocalBewegungReduziert.current
    val atem = if (reduziert) 1f else {
        val takt = rememberInfiniteTransition(label = "fab")
        takt.animateFloat(
            initialValue = 1f, targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                tween(Dauer.FAB_ATEM / 2, easing = Kurven.atem), RepeatMode.Reverse,
            ),
            label = "fabAtem",
        ).value
    }
    Box(
        modifier
            .size(Masse.plusKnopf)
            .graphicsLayer { scaleX = atem; scaleY = atem }
            .clip(RoundedCornerShape(28.dp))
            .background(Farben.akzent)
            .clickable(onClick = aufKlick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = beschreibung,
            tint = if (Farben.istHell) Color.White else Farben.grund,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Die Reihe der vier Zählpunkte — grün, gelb, rot, grau. */
@Composable
fun RowScope.Zaehlpunkte(gruen: Int, gelb: Int, rot: Int, grau: Int) {
    ZaehlPunkt(Farben.gruen, gruen)
    Spacer(Modifier.width(10.dp))
    ZaehlPunkt(Farben.gelb, gelb)
    Spacer(Modifier.width(10.dp))
    ZaehlPunkt(Farben.rot, rot)
    if (grau > 0) {
        Spacer(Modifier.width(10.dp))
        ZaehlPunkt(Farben.grau, grau)
    }
}
