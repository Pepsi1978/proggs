package de.frank.experimente.ui.erzeugt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import de.frank.experimente.ui.theme.Fraunces
import de.frank.experimente.ui.theme.Inter
import de.frank.experimente.ui.theme.JetBrainsMono
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ERZEUGT — nicht von Hand aendern.
 *
 * Quelle: `3-auswertung.json` aus `Specs/<App>/v2/messung/`.
 * Neu erzeugen: `design-umsetzer/references/bildschirm-erzeugen.ps1`.
 *
 * Jedes Element steht an seiner gemessenen Stelle, mit seinen gemessenen Werten. Wer hier
 * etwas verschiebt, verschiebt es gegen die Vorlage — dann stimmt die Vorlage nicht mehr.
 */
private fun ausPfaden(breite: Float, hoehe: Float, vararg pfade: String): ImageVector =
    ImageVector.Builder(
        name = "symbol", defaultWidth = breite.dp, defaultHeight = hoehe.dp,
        viewportWidth = breite, viewportHeight = hoehe,
    ).apply {
        pfade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()

@Composable
fun Auswertung3Erzeugt(
    modifier: Modifier = Modifier,
    beiFunktion: (String) -> Unit = {},
    beiNavigation: (String) -> Unit = {},
) {
    // Der Entwurf ist 865 dp hoch, das Sichtfenster nur 751 dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(865f.dp)) {
        // werft-b03
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 751f.dp).background(Brush.radialGradient(listOf(Color(0xFFC4623C).copy(alpha = 0.18f), Color(0xFF000000).copy(alpha = 0f), Color(0xFF6F8F6A).copy(alpha = 0.12f), Color(0xFF000000).copy(alpha = 0f)))))
        // werft-b03__topbar
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 64f.dp).shadow(elevation = 10f.dp, clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).background(Color(0xFF151210).copy(alpha = 0.82f))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.08f))) }
        // werft-b03__back
        Box(Modifier.offset(x = 20f.dp, y = 7.5f.dp).size(width = 48f.dp, height = 48f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF201B17).copy(alpha = 0.78f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(percent = 50)).clickable { beiNavigation("B-01") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b03__title
        Box(Modifier.offset(x = 80f.dp, y = 14.5f.dp).defaultMinSize(minWidth = 235.14f.dp, minHeight = 34f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Wie ist es gelaufen?",
                style = TextStyle(
                    fontSize = 28f.sp, lineHeight = 34f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                    fontFamily = Fraunces,
                ),
            )
        }
        // svg
        Box(Modifier.offset(x = 32f.dp, y = 19.5f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.42-1.41L7.83 13H20v-2z",
                ),
                contentDescription = null, tint = Color(0xFFF4EEE7),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // werft-b03__experiment-title
        Box(Modifier.offset(x = 20f.dp, y = 96f.dp).defaultMinSize(minWidth = 435f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "⟨Experimenttitel⟩",
                style = TextStyle(
                    fontSize = 19f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                    fontFamily = Fraunces,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 20f.dp, y = 125f.dp).defaultMinSize(minWidth = 78.63f.dp, minHeight = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Tag 2 von 3",
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 118.63f.dp, y = 125f.dp).defaultMinSize(minWidth = 114.36f.dp, minHeight = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "3 von 5 erledigt",
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        // werft-b03__mic
        Box(Modifier.offset(x = 193.5f.dp, y = 175f.dp).size(width = 88f.dp, height = 88f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.3f), spotColor = Color(0xFFC4623C).copy(alpha = 0.3f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.26f), spotColor = Color(0xFF000000).copy(alpha = 0.26f)).clip(RoundedCornerShape(percent = 50)).background(Brush.linearGradient(0f to Color(0xFFC9704D), 0.58f to Color(0xFFC4623C), 1f to Color(0xFFA55232))).clickable { beiFunktion("F-10") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.28f))) }
        // svg
        Box(Modifier.offset(x = 223.5f.dp, y = 205f.dp).size(width = 28f.dp, height = 28f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M12 14q1.25 0 2.125-.875T15 11V5q0-1.25-.875-2.125T12 2 9.875 2.875 9 5v6q0 1.25.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16q2.075 0 3.538-1.463T17 11h2q0 2.6-1.7 4.575T13 17.925V21h-2Z",
                ),
                contentDescription = null, tint = Color(0xFFF4EEE7),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // werft-b03__button
        Box(Modifier.offset(x = 188.27f.dp, y = 769f.dp).defaultMinSize(minWidth = 98.47f.dp, minHeight = 48f.dp).clip(RoundedCornerShape(14f.dp)), contentAlignment = Alignment.Center) {
            Text(
                text = "Überspringen",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        }
    }
}
