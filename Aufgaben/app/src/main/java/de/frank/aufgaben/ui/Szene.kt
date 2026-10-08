package de.frank.aufgaben.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.ui.szenen.szeneAurora
import de.frank.aufgaben.ui.szenen.szeneGarten
import de.frank.aufgaben.ui.szenen.szeneKosmos
import de.frank.aufgaben.ui.szenen.szeneOrange
import de.frank.aufgaben.ui.szenen.AURORA_ZYKLUS
import de.frank.aufgaben.ui.szenen.GARTEN_ZYKLUS
import de.frank.aufgaben.ui.szenen.KOSMOS_ZYKLUS
import de.frank.aufgaben.ui.szenen.ORANGE_ZYKLUS
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.glas

/**
 * Ablauf je Design: Zykluslänge, Standbild für reduzierte Bewegung, Phasen mit Startzeit. Die Zeiten müssen zu den
 * Szenen passen. Jede Szene ist eine Endlosschleife ohne Schnitt: Sie endet im Dunkeln mit leerem Bild, genau so, wie
 * sie beginnt.
 */
private class Ablauf(val zyklus: Float, val standbild: Float, val phasen: List<Pair<Float, String>>)

private fun ablauf(d: Design) = when (d) {
    Design.ORANGE -> Ablauf(ORANGE_ZYKLUS, 27f, listOf(0f to "Ankommen", 8.8f to "Arbeiten", 20f to "Katzenpause", 34f to "Feierabend"))
    Design.AURORA -> Ablauf(AURORA_ZYKLUS, 28f, listOf(0f to "Nachdenken", 11.2f to "Skizzieren", 26f to "Abhaken", 31.4f to "Geschafft"))
    Design.GARTEN -> Ablauf(GARTEN_ZYKLUS, 24f, listOf(0f to "Losgehen", 6f to "Gießen", 20.8f to "Wachsen", 26.4f to "Ernten"))
    Design.KOSMOS -> Ablauf(KOSMOS_ZYKLUS, 22f, listOf(0f to "Checkliste", 5.6f to "Systeme prüfen", 15.6f to "Countdown", 21f to "Start!"))
}

/** Die animierte Szene oben: für jedes Design eine eigene Geschichte mit eigenem Blickwinkel. */
@Composable
fun AufgabenSzene(modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") erledigtAnteil: Float) {
    val f = LocalFarben.current
    val bewegung = LocalBewegung.current
    val zeit = rememberSzenenZeit()
    val schrift = rememberTextMeasurer()
    val a = remember(f.design) { ablauf(f.design) }
    val ms = (a.zyklus * 1000).toLong()
    val aktivePhase by remember(a) {
        derivedStateOf {
            val t = if (!bewegung) a.standbild else (zeit.value % ms) / 1000f
            a.phasen.indexOfLast { t >= it.first }.coerceAtLeast(0)
        }
    }
    Column(modifier.glas(f, erhoeht = 1.2f).padding(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(172.dp)) {
            val t = if (!bewegung) a.standbild else (zeit.value % ms) / 1000f
            clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(f.radius.toPx() * 0.7f))) }) {
                when (f.design) {
                    Design.ORANGE -> szeneOrange(t, f)
                    Design.AURORA -> szeneAurora(t, f)
                    Design.GARTEN -> szeneGarten(t, f)
                    Design.KOSMOS -> szeneKosmos(t, f, schrift)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            a.phasen.forEachIndexed { i, (_, name) ->
                val farbe by animateColorAsState(if (i == aktivePhase) f.primaer else f.textSchwach, label = "phase")
                Text(name, color = farbe, fontSize = 12.sp, fontWeight = if (i == aktivePhase) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}
