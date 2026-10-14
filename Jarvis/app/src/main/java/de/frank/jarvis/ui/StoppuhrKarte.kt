package de.frank.jarvis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.timer.Stoppuhr
import de.frank.jarvis.ui.theme.LocalFarben
import kotlin.math.cos
import kotlin.math.sin

/**
 * Die Stoppuhr auf der Startseite: Zifferblatt mit umlaufendem Sekundenring, große Zeit mit Hundertsteln,
 * darunter Start/Anhalten/Weiter und Zurücksetzen. Steht da, solange die Stoppuhr läuft oder angehalten ist
 * (bis Frank zurücksetzt), oder wenn er sie über den Knopf oben geöffnet hat.
 */
@Composable
fun StoppuhrKarte(offen: Boolean, schliessen: () -> Unit, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val kontext = LocalContext.current
    val s by Stoppuhr.stand.collectAsState()
    if (!s.aktiv && !offen) return
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Läuft sie, wird jedes Bild neu berechnet (flüssige Hundertstel); angehalten steht alles still.
    LaunchedEffect(s.laeuft) {
        while (s.laeuft) { androidx.compose.runtime.withFrameMillis { }; jetzt = System.currentTimeMillis() }
        jetzt = System.currentTimeMillis()
    }
    val ms = s.gesamt(jetzt)
    Karte(modifier, toenung = f.primaer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stoppuhr", Modifier.weight(1f), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (!s.aktiv) Rundknopf(Icons.Rounded.Close, "Stoppuhr schließen") { schliessen() }
        }
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
            val ring = f.primaer
            val spur = f.textSchwach.copy(alpha = 0.25f)
            val anteil = (ms % 60_000) / 60_000f
            Canvas(Modifier.size(190.dp)) {
                val strich = 10.dp.toPx()
                val r = size.minDimension / 2 - strich
                val links = Offset(center.x - r, center.y - r)
                // Skala: 60 Striche, alle fünf Sekunden länger.
                for (i in 0 until 60) {
                    val w = Math.toRadians(i * 6.0 - 90).toFloat()
                    val lang = i % 5 == 0
                    val innen = r - strich - (if (lang) 9.dp.toPx() else 5.dp.toPx())
                    val aussen = r - strich - 2.dp.toPx()
                    drawLine(if (lang) ring.copy(alpha = 0.7f) else spur, Offset(center.x + innen * cos(w), center.y + innen * sin(w)),
                        Offset(center.x + aussen * cos(w), center.y + aussen * sin(w)), if (lang) 2.dp.toPx() else 1.dp.toPx(), StrokeCap.Round)
                }
                drawArc(spur, 0f, 360f, false, links, Size(r * 2, r * 2), style = Stroke(strich))
                drawArc(ring, -90f, 360f * anteil, false, links, Size(r * 2, r * 2), style = Stroke(strich, cap = StrokeCap.Round))
                // Leuchtpunkt an der Spitze des Rings.
                val w = Math.toRadians(360.0 * anteil - 90).toFloat()
                drawCircle(ring, strich * 0.85f, Offset(center.x + r * cos(w), center.y + r * sin(w)))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Stoppuhr.anzeige(ms, hundertstel = false), color = f.text, fontSize = if (ms >= 3_600_000) 30.sp else 38.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(",%02d".format((ms % 1000) / 10), color = f.primaer, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            }
        }
        Text(
            when {
                !s.aktiv -> "Bereit"
                s.laeuft -> "Läuft seit " + uhr(s.gestartetUm) + " Uhr"
                else -> "Angehalten · gestartet um " + uhr(s.gestartetUm) + " Uhr"
            },
            Modifier.fillMaxWidth().padding(top = 4.dp), color = f.textLeise, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                !s.aktiv -> Knopf("Start", Modifier.weight(1f), Icons.Rounded.PlayArrow) { Stoppuhr.starten(kontext) }
                s.laeuft -> Knopf("Anhalten", Modifier.weight(1f), Icons.Rounded.Pause) { Stoppuhr.anhalten(kontext) }
                else -> Knopf("Weiter", Modifier.weight(1f), Icons.Rounded.PlayArrow) { Stoppuhr.fortsetzen(kontext) }
            }
            if (s.aktiv) Knopf("Zurücksetzen", Modifier.weight(1f), Icons.Rounded.RestartAlt, haupt = false) { Stoppuhr.zuruecksetzen(kontext); schliessen() }
        }
    }
}

private fun uhr(ms: Long?): String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.GERMAN).format(java.util.Date(ms ?: System.currentTimeMillis()))
