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
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.glas

/** Ablauf je Design: Zykluslänge, Standbild für reduzierte Bewegung, Phasen mit Startzeit. */
private class Ablauf(val zyklus: Float, val standbild: Float, val phasen: List<Pair<Float, String>>)

private fun ablauf(d: Design) = when (d) {
    Design.ORANGE -> Ablauf(26f, 17f, listOf(0f to "Ankommen", 5f to "Arbeiten", 15f to "Katzenpause", 21f to "Feierabend"))
    Design.AURORA -> Ablauf(24f, 18f, listOf(0f to "Nachdenken", 3f to "Skizzieren", 15f to "Abhaken", 19.3f to "Geschafft"))
    Design.GARTEN -> Ablauf(26f, 19f, listOf(0f to "Losgehen", 2.6f to "Gießen", 15f to "Wachsen", 18f to "Ernten"))
    Design.KOSMOS -> Ablauf(24f, 15.5f, listOf(0f to "Checkliste", 2f to "Systeme prüfen", 10f to "Countdown", 14f to "Start!"))
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
