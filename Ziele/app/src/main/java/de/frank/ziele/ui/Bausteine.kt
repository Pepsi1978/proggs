package de.frank.ziele.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.ui.theme.LocalBewegung
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.antippen
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d
import kotlinx.coroutines.delay

/** Warmer Goldschein im Hintergrund: zwei weiche, langsam atmende Lichtflecken auf dem Grundverlauf. */
@Composable
fun Hintergrund(modifier: Modifier) {
    val f = LocalFarben.current
    val bewegung = LocalBewegung.current
    val atem = if (bewegung) rememberInfiniteTransition(label = "atem").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "t").value else 0.5f
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten)))
        val a = if (f.dunkel) 0.22f else 0.55f
        drawCircle(
            Brush.radialGradient(listOf(f.blob1.copy(alpha = a), Color.Transparent), Offset(size.width * (0.85f - 0.1f * atem), size.height * 0.08f), size.width * 0.75f),
            size.width * 0.75f, Offset(size.width * (0.85f - 0.1f * atem), size.height * 0.08f),
        )
        drawCircle(
            Brush.radialGradient(listOf(f.blob2.copy(alpha = a * 0.6f), Color.Transparent), Offset(size.width * (0.1f + 0.08f * atem), size.height * 0.72f), size.width * 0.8f),
            size.width * 0.8f, Offset(size.width * (0.1f + 0.08f * atem), size.height * 0.72f),
        )
    }
}

@Composable
fun RundKnopf(icon: ImageVector, beschreibung: String, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.padding(4.dp).size(44.dp).glas(f, 99.dp, 0.8f).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = farbe ?: f.text, modifier = Modifier.size(22.dp))
    }
}

/** Hell → Automatisch (nach der Sonne) → Dunkel → Hell, wie in Jarvis. Das Symbol zeigt den aktuellen Modus. */
@Composable
fun ModusKnopf(vm: AppViewModel) {
    val f = LocalFarben.current
    val modus = vm.einstellungen.modus
    RundKnopf(
        when (modus) { "hell" -> Icons.Rounded.LightMode; "dunkel" -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto },
        when (modus) { "hell" -> "Hell (weiter zu Automatisch)"; "dunkel" -> "Dunkel (weiter zu Hell)"; else -> "Automatisch (weiter zu Dunkel)" },
        farbe = f.primaer,
    ) {
        val neu = when (modus) { "hell" -> "system"; "system" -> "dunkel"; else -> "hell" }
        vm.einstellungen.modus = neu
        vm.melde(when (neu) { "hell" -> "Hell"; "dunkel" -> "Dunkel"; else -> "Automatisch nach der Sonne" })
    }
}

@Composable
fun Block(titel: String, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().glas(f, erhoeht = 1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titel.uppercase(), color = f.primaer, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        inhalt()
    }
}

@Composable
fun ChipReihe(inhalt: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { inhalt() }
}

/** Goldener Ring für den Fortschritt (notwendige Aufgaben erledigt), mit Prozent in der Mitte. */
@Composable
fun Fortschrittsring(anteil: Float, groesse: Dp, staerke: Dp = 5.dp, beschriftung: Boolean = true) {
    val f = LocalFarben.current
    val wert by animateFloatAsState(anteil.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(Modifier.size(groesse), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(groesse)) {
            val s = staerke.toPx()
            val bogen = Size(size.width - s, size.height - s)
            val ecke = Offset(s / 2, s / 2)
            drawArc(f.textSchwach.copy(alpha = 0.22f), 0f, 360f, false, ecke, bogen, style = Stroke(s))
            drawArc(Brush.sweepGradient(listOf(f.tertiaer, f.primaer, f.sekundaer, f.tertiaer)), -90f, 360f * wert, false, ecke, bogen, style = Stroke(s, cap = StrokeCap.Round))
        }
        if (beschriftung) Text("${(wert * 100).toInt()}%", color = f.text, fontSize = (groesse.value * 0.24f).sp, fontWeight = FontWeight.Bold)
    }
}

/** Großer runder Mikrofon-Knopf: pulsiert beim Aufnehmen, dreht beim Verarbeiten. */
@Composable
fun MikroKnopf(aufnahme: Aufnahme, groesse: Dp = 64.dp, aktion: () -> Unit) {
    val f = LocalFarben.current
    val puls = if (aufnahme == Aufnahme.LAEUFT && LocalBewegung.current)
        rememberInfiniteTransition(label = "puls").animateFloat(1f, 1.12f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "p").value else 1f
    val farbe = if (aufnahme == Aufnahme.LAEUFT) f.gefahr else f.primaer
    Box(
        Modifier.size(groesse).graphicsLayer { scaleX = puls; scaleY = puls }.knopf3d(farbe, if (aufnahme == Aufnahme.LAEUFT) farbe.copy(alpha = 0.8f) else f.tertiaer, 99.dp, f.dunkel).antippen(aktion = aktion),
        contentAlignment = Alignment.Center,
    ) {
        when (aufnahme) {
            Aufnahme.VERARBEITET -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(groesse * 0.42f))
            Aufnahme.LAEUFT -> Icon(Icons.Rounded.Stop, "Aufnahme beenden", tint = Color.White, modifier = Modifier.size(groesse * 0.46f))
            Aufnahme.AUS -> Icon(Icons.Rounded.Mic, "Einsprechen", tint = f.aufPrimaer, modifier = Modifier.size(groesse * 0.46f))
        }
    }
}

/** Laufende Aufnahmezeit, z. B. „0:12“. */
@Composable
fun Aufnahmezeit(start: Long) {
    val f = LocalFarben.current
    var jetzt by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) { while (true) { jetzt = System.currentTimeMillis(); delay(250) } }
    val s = ((jetzt - start) / 1000).coerceAtLeast(0)
    Text("%d:%02d".format(s / 60, s % 60), color = f.gefahr, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun Meldung(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val text = vm.meldung
    LaunchedEffect(text) { if (text != null) { delay(if (vm.rueckgaengig != null) 5000 else 3000); vm.meldungWeg() } }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Row(
            Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth()
                .glas(f, 18.dp, 2f, f.flaecheStark).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(vm.meldung.orEmpty(), color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            vm.rueckgaengig?.let { r ->
                Text("Rückgängig", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.antippen { r(); vm.meldungWeg() }.padding(start = 12.dp))
            }
        }
    }
}

/** Kleines Etikett (Frist, Aufwand, Phase …). */
@Composable
fun Etikett(text: String, farbe: Color? = null, gefuellt: Boolean = false) {
    val f = LocalFarben.current
    val c = farbe ?: f.textLeise
    Text(
        text,
        color = if (gefuellt) f.aufPrimaer else c,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .then(if (gefuellt) Modifier.knopf3d(c, c.copy(alpha = 0.85f), 99.dp, f.dunkel) else Modifier.glas(f, 99.dp, 0f, c.copy(alpha = 0.12f), rand = false))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** „noch 12 Tage“, „heute“, „seit 3 Tagen vorbei“. */
fun fristText(frist: Long, heute: Long): String {
    val t = frist - heute
    return when {
        t == 0L -> "heute"
        t == 1L -> "morgen"
        t > 1 && t < 60 -> "noch $t Tage"
        t >= 60 && t < 730 -> "noch ${(t / 30.4).toInt()} Monate"
        t >= 730 -> "noch ${(t / 365.25).toInt()} Jahre"
        t == -1L -> "seit gestern vorbei"
        else -> "seit ${-t} Tagen vorbei"
    }
}
