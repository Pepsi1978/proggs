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
import androidx.compose.ui.graphics.graphicsLayer
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
import de.frank.aufgaben.ui.szenen.an
import de.frank.aufgaben.ui.szenen.weich
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.glas

/** Ablauf je Design: Zykluslänge, Standbild für reduzierte Bewegung, Phasen mit Startzeit. Die Zeiten müssen zu den Szenen passen. */
private class Ablauf(val zyklus: Float, val standbild: Float, val phasen: List<Pair<Float, String>>)

private fun ablauf(d: Design) = when (d) {
    Design.ORANGE -> Ablauf(40f, 24f, listOf(0f to "Ankommen", 6.4f to "Arbeiten", 17f to "Katzenpause", 31f to "Feierabend"))
    Design.AURORA -> Ablauf(36f, 22f, listOf(0f to "Nachdenken", 5.2f to "Skizzieren", 20f to "Abhaken", 25.4f to "Geschafft"))
    Design.GARTEN -> Ablauf(38f, 21f, listOf(0f to "Losgehen", 3f to "Gießen", 17.8f to "Wachsen", 23.4f to "Ernten"))
    Design.KOSMOS -> Ablauf(36f, 19f, listOf(0f to "Checkliste", 2.6f to "Systeme prüfen", 12.6f to "Countdown", 18f to "Start!"))
}

/** Sanftes Einblenden am Anfang und Ausblenden am Ende jedes Durchlaufs, damit der Neustart nicht springt. */
private const val EINBLENDEN = 1.4f
private const val AUSBLENDEN = 1.6f

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
        Canvas(
            Modifier.fillMaxWidth().height(172.dp).graphicsLayer {
                if (!bewegung) return@graphicsLayer
                val t = (zeit.value % ms) / 1000f
                val ein = weich(an(t, 0f, EINBLENDEN))
                val aus = weich(an(t, a.zyklus - AUSBLENDEN, AUSBLENDEN))
                // Taucht leicht von unten auf, schwebt am Ende leicht nach oben weg; dazu ein Hauch Zoom.
                alpha = ein * (1f - aus)
                val k = 0.965f + 0.035f * ein - 0.025f * aus
                scaleX = k; scaleY = k
                translationY = (1f - ein) * 8.dp.toPx() - aus * 6.dp.toPx()
            },
        ) {
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
