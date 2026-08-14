package de.frank.stacklabor.ui.bausteine

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.logik.Ampel
import de.frank.stacklabor.ui.theme.Bewegungen
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift

/** Die Farbe zu einem Ampelzustand — an einer Stelle, damit sie nirgends abweicht. */
@Composable
fun ampelFarbe(ampel: Ampel): Color = when (ampel) {
    Ampel.GRUEN -> Farben.gruen
    Ampel.GELB -> Farben.gelb
    Ampel.ROT -> Farben.rot
    Ampel.GRAU -> Farben.grau
}

/** Die Textfarbe zu einem Ampelzustand — Gelb braucht auf Weiß einen dunkleren Ton. */
@Composable
fun ampelTextFarbe(ampel: Ampel): Color = when (ampel) {
    Ampel.GRUEN -> Farben.gruen
    Ampel.GELB -> Farben.gelbText
    Ampel.ROT -> Farben.rot
    Ampel.GRAU -> Farben.grau
}

/**
 * Der Ampel-Kantenbalken — 3 dp an der linken Kante, über die volle Höhe.
 *
 * Er ist bewusst kein Punkt: So kann er mit den Löslichkeitspunkten nicht verwechselt
 * werden, und beim Überfliegen einer Liste liest er sich als durchgehende Spur.
 *
 * - **M-07:** Der Farbwechsel überblendet in 320 ms, nie hart.
 * - **M-21:** Steht er auf Rot, atmet eine Aura dahinter (2400 ms) — höchstens drei
 *   gleichzeitig, deshalb entscheidet die aufrufende Liste über `mitAura`.
 */
@Composable
fun AmpelBalken(
    ampel: Ampel,
    modifier: Modifier = Modifier,
    mitAura: Boolean = false,
) {
    val reduziert = LocalBewegungReduziert.current
    val ziel = ampelFarbe(ampel)
    // M-07 bleibt auch bei reduzierter Bewegung erhalten — sie trägt Bedeutung.
    val farbe by animateColorAsState(ziel, Bewegungen.zustand(), label = "ampel")

    val aura = if (mitAura && ampel == Ampel.ROT && !reduziert) {
        val takt = rememberInfiniteTransition(label = "aura")
        takt.animateFloat(
            initialValue = 0.30f, targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                tween(Dauer.AURA / 2, easing = Kurven.atem), RepeatMode.Reverse,
            ),
            label = "auraDeckkraft",
        ).value
    } else 0f

    Box(modifier = modifier.width(Masse.ampelBalken).fillMaxHeight()) {
        if (aura > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(Masse.ampelBalken)
                    .graphicsLayer { alpha = aura; scaleX = 1.9f }
                    .background(farbe, Formen.ampelBalken),
            )
        }
        Box(Modifier.fillMaxHeight().width(Masse.ampelBalken).background(farbe, Formen.ampelBalken))
    }
}

/**
 * Die Löslichkeits-Punkte, genau wie im Grilling festgelegt:
 * grün gefüllt = wasserlöslich · weiß mit Rand = fettlöslich · beide = beides.
 *
 * Der Rand am weißen Punkt ist nicht Zierde: Ohne ihn wäre er auf der weißen Karte der
 * hellen Erscheinung — und die ist der Standard — schlicht unsichtbar.
 */
enum class Loeslichkeit { WASSER, FETT, BEIDES }

@Composable
fun LoeslichkeitsPunkte(loeslichkeit: Loeslichkeit, modifier: Modifier = Modifier) {
    val beschreibung = when (loeslichkeit) {
        Loeslichkeit.WASSER -> "wasserlöslich"
        Loeslichkeit.FETT -> "fettlöslich"
        Loeslichkeit.BEIDES -> "wasser- und fettlöslich"
    }
    Row(
        modifier = modifier.semantics { contentDescription = beschreibung },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loeslichkeit != Loeslichkeit.FETT) {
            Box(
                Modifier
                    .size(Masse.loeslichPunkt)
                    .background(Farben.loeslichWasser, CircleShape),
            )
        }
        if (loeslichkeit == Loeslichkeit.BEIDES) Spacer(Modifier.width(3.dp))
        if (loeslichkeit != Loeslichkeit.WASSER) {
            Box(
                Modifier
                    .size(Masse.loeslichPunkt)
                    .background(Farben.loeslichFettFuellung, CircleShape)
                    .border(Masse.loeslichRand, Farben.loeslichFettRand, CircleShape),
            )
        }
    }
}

/** Ein Zählpunkt mit Zahl — „● 4  ● 0  ● 1  ● 1" auf der Stack-Karte und im Ziel-Streifen. */
@Composable
fun ZaehlPunkt(farbe: Color, anzahl: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(Masse.zaehlPunkt).background(farbe, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(anzahl.toString(), style = Schrift.winzig, color = Farben.textSchwach)
    }
}

/**
 * Das Häkchen (F-05).
 *
 * **Das Kästchen selbst bewegt sich nicht** (Motion-Spec §1) — es wechselt seinen Zustand
 * ohne Sprung oder Skalierung. Die Rückmeldung besteht aus dem Flächenwechsel und einem
 * leichten haptischen Impuls; alles andere würde die Aufmerksamkeit von den Ampeln abziehen.
 */
@Composable
fun Haekchen(
    gesetzt: Boolean,
    aufKlick: () -> Unit,
    modifier: Modifier = Modifier,
    beschreibung: String = if (gesetzt) "aktiv, abschalten" else "abgeschaltet, einschalten",
) {
    val flaeche by animateColorAsState(
        if (gesetzt) Farben.akzent else Color.Transparent,
        Bewegungen.antwort(), label = "haekchenFlaeche",
    )
    Box(
        modifier = modifier
            .size(Masse.tippflaeche)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                onClick = aufKlick,
            )
            .semantics { contentDescription = beschreibung },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Masse.haekchen)
                .clip(Formen.haekchen)
                .background(flaeche)
                .border(
                    width = if (gesetzt) 0.dp else 1.5.dp,
                    color = if (gesetzt) Color.Transparent else Farben.rand,
                    shape = Formen.haekchen,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (gesetzt) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (Farben.istHell) Color.White else Farben.grund,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Die Glanzkante, die alle 8 Sekunden über eine Karte wandert (M-16, `@keyframes glanz`).
 *
 * Sie ist der billigste der gewünschten Effekte und trägt viel: Die Fläche wirkt dadurch
 * wie Material statt wie ein Rechteck. Bei reduzierter Bewegung entfällt sie ersatzlos.
 */
@Composable
fun Modifier.glanzkante(): Modifier {
    if (LocalBewegungReduziert.current) return this
    val takt = rememberInfiniteTransition(label = "glanz")
    val lauf by takt.animateFloat(
        initialValue = -0.6f, targetValue = 3.2f,
        animationSpec = infiniteRepeatable(tween(Dauer.GLANZ, easing = Kurven.linear)),
        label = "glanzLauf",
    )
    val licht = Farben.text.copy(alpha = if (Farben.istHell) 0.05f else 0.10f)
    return this.drawBehind {
        val breite = size.width * 0.35f
        val x = size.width * lauf
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color.Transparent, licht, Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + breite, size.height),
            ),
        )
    }
}

/** Trennlinie zwischen Listeneinträgen — 1 dp, die Farbe der Ränder. */
@Composable
fun Trenner(modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(Farben.rand))
}

/** Blendet einen Eintrag aus, wenn er abgeschaltet ist — 38 % Deckkraft, wie gemessen. */
@Composable
fun Modifier.ausgegraut(aktiv: Boolean): Modifier = this.alpha(if (aktiv) 1f else 0.38f)
