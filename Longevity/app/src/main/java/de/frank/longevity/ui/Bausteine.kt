package de.frank.longevity.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.data.Evidenz
import de.frank.longevity.ui.theme.Farben
import de.frank.longevity.ui.theme.LocalBewegung
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.glas
import de.frank.longevity.ui.theme.knopf3d

/** Einblenden beim ersten Erscheinen: steigt auf und kippt leicht in 3D nach vorn. */
@Composable
fun Modifier.einblenden(verzoegerung: Int = 0): Modifier {
    val bewegung = LocalBewegung.current
    val a = remember { Animatable(if (bewegung) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (bewegung) { kotlinx.coroutines.delay(verzoegerung.toLong()); a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 180f)) }
    }
    return graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 60f
        rotationX = (1f - v) * 18f
        cameraDistance = 16f * density
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

@Composable
fun RundKnopf(icon: ImageVector, beschreibung: String, aktiv: Boolean = false, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        Modifier.padding(3.dp).size(42.dp)
            .then(if (aktiv) Modifier.knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel) else Modifier.glas(f, 99.dp, 0.8f))
            .antippen(aktion = aktion),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, beschreibung, tint = if (aktiv) Color.White else f.text, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun Block(titel: String, modifier: Modifier = Modifier, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    Column(modifier.fillMaxWidth().einblenden(80).glas(f, erhoeht = 1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titel.uppercase(), color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        inhalt()
    }
}

@Composable
fun ChipReihe(inhalt: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { inhalt() }
}

@Composable
fun AktionsKnopf(icon: ImageVector, text: String, beschaeftigt: Boolean = false, modifier: Modifier = Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    Row(
        modifier.height(44.dp).glas(f, 14.dp, 0.7f, f.flaecheStark).antippen(aktion = aktion).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (beschaeftigt) CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        else Icon(icon, null, tint = f.primaer, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = f.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Großer Hauptknopf mit Verlauf. */
@Composable
fun HauptKnopf(icon: ImageVector, text: String, modifier: Modifier = Modifier, beschaeftigt: Boolean = false, aktion: () -> Unit) {
    val f = LocalFarben.current
    Row(
        modifier.fillMaxWidth().height(56.dp).knopf3d(f.primaer, f.sekundaer, 18.dp, f.dunkel).antippen(aktion = aktion),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (beschaeftigt) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Checkkreis(erledigt: Boolean, farbe: Color, groesse: Int = 24, aktion: () -> Unit) {
    val f = LocalFarben.current
    val haptik = LocalHapticFeedback.current
    val fort by animateFloatAsState(if (erledigt) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "haken")
    Box(
        Modifier.size((groesse + 14).dp).antippen(haptik = false) {
            haptik.performHapticFeedback(HapticFeedbackType.LongPress)
            aktion()
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(groesse.dp).graphicsLayer { scaleX = 1f + 0.15f * fort * (1f - fort) * 4f; scaleY = scaleX }) {
            val r = size.minDimension / 2
            drawCircle(farbe.copy(alpha = 0.12f + 0.88f * fort), r)
            drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f * fort), Color.Transparent)), r * 0.9f)
            drawCircle(farbe, r - 1.5.dp.toPx(), style = Stroke(2.dp.toPx()))
            if (fort > 0f) {
                val p = Path().apply {
                    moveTo(r * 0.55f, r * 1.02f)
                    lineTo(r * 0.88f, r * 1.35f)
                    lineTo(r * (0.88f + 0.62f * fort), r * (1.35f - 0.72f * fort))
                }
                drawPath(p, if (f.dunkel) f.aufPrimaer else Color.White, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

fun Farben.evidenzFarbe(e: Evidenz): Color = when (e) {
    Evidenz.BELEGT -> erfolg
    Evidenz.WAHRSCHEINLICH -> if (dunkel) Color(0xFFFFC857) else Color(0xFFE09A00)
    Evidenz.LOGISCH -> if (dunkel) Color(0xFFB8A6FF) else Color(0xFF7A5CE0)
}

/** Kleines Abzeichen: farbiger Punkt plus Evidenzstufe. */
@Composable
fun EvidenzAbzeichen(e: Evidenz, lang: Boolean = false) {
    val f = LocalFarben.current
    val c = f.evidenzFarbe(e)
    Row(
        Modifier.glas(f, 99.dp, 0f, c.copy(alpha = 0.14f), rand = false).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { i -> Box(Modifier.size(5.dp).glas(f, 99.dp, 0f, if (i < e.staerke) c else c.copy(alpha = 0.25f), rand = false)) }
        }
        Text(if (lang) e.anzeige else e.kurz, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Jahre hübsch: „+4,5 J.“ bzw. „−10 J.“ für Lebenszeit-Räuber. */
fun jahreText(j: Float): String = if (j == 0f) "–" else
    (if (j > 0f) "+" else "−") + (if (kotlin.math.abs(j) >= 10f) "%.0f" else "%.1f").format(java.util.Locale.GERMANY, kotlin.math.abs(j)) + " J."
