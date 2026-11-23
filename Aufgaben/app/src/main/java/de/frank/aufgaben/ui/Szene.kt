package de.frank.aufgaben.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.frank.aufgaben.ui.szene3d.SZENE_3D_PHASEN
import de.frank.aufgaben.ui.szene3d.Szene3DView
import de.frank.aufgaben.ui.szene3d.palette3DFuer
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.glas

/**
 * Die echtzeit-gerenderte 3D-Szene oben (OpenGL ES 3.0 mit PBR-Beleuchtung, weichen Echtzeitschatten
 * und filmischer 3D-Kamera): Ein Mensch kommt mit seiner Katze durch die schwingende Tür herein,
 * setzt sich an den PC-Arbeitsplatz und beginnt zu arbeiten, trinkt Kaffee, schaltet abends die
 * Schreibtischlampe ein und streichelt die Katze auf dem Schreibtisch. Farbwelt und Atmosphäre passen
 * sich nahtlos jedem gewählten App-Design an.
 */
@Composable
fun AufgabenSzene(modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") erledigtAnteil: Float, pausiert: Boolean = false) {
    val f = LocalFarben.current
    val bewegung = LocalBewegung.current
    val vorne = rememberVorne()
    val effektivPausiert = pausiert || !vorne
    val palette = remember(f.design, f.dunkel) { palette3DFuer(f.design, f.dunkel) }
    val eckenPx = with(LocalDensity.current) { (f.radius * 0.7f).toPx() }
    var aktivePhase by remember { mutableIntStateOf(if (bewegung) 0 else 1) }

    Column(modifier.glas(f, erhoeht = 1.2f).padding(10.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(184.dp),
            factory = { ctx ->
                Szene3DView(ctx).apply {
                    setCornerRadiusPx(eckenPx)
                    setPalette(palette)
                    setZustand(effektivPausiert, bewegung)
                    setOnPhaseChanged { aktivePhase = it }
                }
            },
            update = { view ->
                view.setCornerRadiusPx(eckenPx)
                view.setPalette(palette)
                view.setZustand(effektivPausiert, bewegung)
                view.setOnPhaseChanged { aktivePhase = it }
            },
            onRelease = { view ->
                view.setOnPhaseChanged(null)
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SZENE_3D_PHASEN.forEachIndexed { i, (_, name) ->
                val farbe by animateColorAsState(if (i == aktivePhase) f.primaer else f.textSchwach, label = "phase")
                Text(
                    name,
                    color = farbe,
                    fontSize = 12.sp,
                    fontWeight = if (i == aktivePhase) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
