package de.frank.experimente.ui.erzeugt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ERZEUGT — nicht von Hand aendern.
 *
 * Quelle: `6-erkenntnisse.json` aus `Specs/<App>/v2/messung/`.
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
fun Erkenntnisse6Erzeugt(modifier: Modifier = Modifier) {
    // Der Entwurf ist 751 dp hoch, das Sichtfenster nur 751 dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(751f.dp)) {
        // b06-screen
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 751f.dp).background(Brush.radialGradient(listOf(Color(0xFFC4623C).copy(alpha = 0.18f), Color(0xFF000000).copy(alpha = 0f), Color(0xFF6F8F6A).copy(alpha = 0.12f), Color(0xFF000000).copy(alpha = 0f)))))
        // b06-topbar
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 64f.dp).shadow(elevation = 10f.dp, clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).background(Color(0xFF151210).copy(alpha = 0.82f))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.08f))) }
        // b06-title
        Box(Modifier.offset(x = 20f.dp, y = 14.5f.dp).size(width = 157.16f.dp, height = 34f.dp)) {
            Text(
                text = "Erkenntnisse",
                style = TextStyle(
                    fontSize = 28f.sp, lineHeight = 34f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                ),
            )
        }
        // b06-intro
        Box(Modifier.offset(x = 20f.dp, y = 84f.dp).size(width = 435f.dp, height = 63f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) {
            Text(
                text = "Was sich aus deinen Auswertungen ergeben hat.",
                style = TextStyle(
                    fontSize = 14f.sp, lineHeight = 21f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        // b06-empty
        Box(Modifier.offset(x = 20f.dp, y = 159f.dp).size(width = 435f.dp, height = 67f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) {
            Text(
                text = "Noch nichts. Das wächst mit deinen Auswertungen.",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        // b06-bottom-nav
        Box(Modifier.offset(x = 12f.dp, y = 675f.dp).size(width = 451f.dp, height = 64f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(24f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(24f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(24f.dp)).background(Color(0xFF201B17).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(24f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // b06-nav-item
        Box(Modifier.offset(x = 286.39f.dp, y = 680f.dp).size(width = 81.8f.dp, height = 54f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.18f), spotColor = Color(0xFFC4623C).copy(alpha = 0.18f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF3A231A))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFC4623C).copy(alpha = 0.18f))) }
        // b06-nav-label
        Box(Modifier.offset(x = 41.88f.dp, y = 711f.dp).size(width = 32.03f.dp, height = 18f.dp)) {
            Text(
                text = "Heute",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        // b06-nav-label
        Box(Modifier.offset(x = 134.69f.dp, y = 711f.dp).size(width = 26.02f.dp, height = 18f.dp)) {
            Text(
                text = "Ziele",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        // b06-nav-label
        Box(Modifier.offset(x = 213.48f.dp, y = 711f.dp).size(width = 48.02f.dp, height = 18f.dp)) {
            Text(
                text = "Merkliste",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        // b06-nav-label
        Box(Modifier.offset(x = 292.59f.dp, y = 711f.dp).size(width = 69.38f.dp, height = 18f.dp)) {
            Text(
                text = "Erkenntnisse",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFC4623C),
                ),
            )
        }
        // b06-nav-label
        Box(Modifier.offset(x = 394.06f.dp, y = 711f.dp).size(width = 46.05f.dp, height = 18f.dp)) {
            Text(
                text = "Logbuch",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        }
    }
}
