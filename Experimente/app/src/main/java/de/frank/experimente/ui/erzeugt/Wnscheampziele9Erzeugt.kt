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
 * Quelle: `9-w-nsche-amp-ziele.json` aus `Specs/<App>/v2/messung/`.
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
fun Wnscheampziele9Erzeugt(
    modifier: Modifier = Modifier,
    beiFunktion: (String) -> Unit = {},
    beiNavigation: (String) -> Unit = {},
) {
    // Der Entwurf ist 751 dp hoch, das Sichtfenster nur 751 dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(751f.dp)) {
        // b04-screen
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 751f.dp).background(Brush.radialGradient(listOf(Color(0xFFC4623C).copy(alpha = 0.18f), Color(0xFF000000).copy(alpha = 0f), Color(0xFF6F8F6A).copy(alpha = 0.12f), Color(0xFF000000).copy(alpha = 0f)))))
        // b04-topbar
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 64f.dp).shadow(elevation = 10f.dp, clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).background(Color(0xFF151210).copy(alpha = 0.82f))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.08f))) }
        // b04-title
        Box(Modifier.offset(x = 20f.dp, y = 14.5f.dp).defaultMinSize(minWidth = 206.89f.dp, minHeight = 34f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Wünsche & Ziele",
                style = TextStyle(
                    fontSize = 28f.sp, lineHeight = 34f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                    fontFamily = Fraunces,
                ),
            )
        }
        // b04-intro
        Box(Modifier.offset(x = 20f.dp, y = 84f.dp).size(width = 435f.dp, height = 84f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp)), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Was möchtest du erreichen? Die Vorschläge tasten dich Schritt für Schritt heran.",
                style = TextStyle(
                    fontSize = 14f.sp, lineHeight = 21f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // b04-empty
        Box(Modifier.offset(x = 20f.dp, y = 200f.dp).size(width = 435f.dp, height = 67f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp)), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Noch keine Ziele. Sprich das erste ein.",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // b04-fab
        Box(Modifier.offset(x = 407f.dp, y = 611f.dp).size(width = 48f.dp, height = 48f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.3f), spotColor = Color(0xFFC4623C).copy(alpha = 0.3f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.26f), spotColor = Color(0xFF000000).copy(alpha = 0.26f)).clip(RoundedCornerShape(percent = 50)).background(Brush.linearGradient(0f to Color(0xFFC9704D), 0.58f to Color(0xFFC4623C), 1f to Color(0xFFA55232))).clickable { beiFunktion("F-20") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.28f))) }
        // svg
        Box(Modifier.offset(x = 419f.dp, y = 623f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M11 19v-6H5v-2h6V5h2v6h6v2h-6v6Z",
                ),
                contentDescription = null, tint = Color(0xFFF4EEE7),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // b04-bottom-nav
        Box(Modifier.offset(x = 12f.dp, y = 675f.dp).size(width = 451f.dp, height = 64f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(24f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(24f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(24f.dp)).background(Color(0xFF201B17).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(24f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // b04-nav-item
        Box(Modifier.offset(x = 106.8f.dp, y = 680f.dp).size(width = 81.8f.dp, height = 54f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.18f), spotColor = Color(0xFFC4623C).copy(alpha = 0.18f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF3A231A)).clickable { beiNavigation("B-04") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFC4623C).copy(alpha = 0.18f))) }
        // b04-nav-label
        Box(Modifier.offset(x = 41.88f.dp, y = 711f.dp).defaultMinSize(minWidth = 32.03f.dp, minHeight = 18f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Heute",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // b04-nav-label
        Box(Modifier.offset(x = 134.69f.dp, y = 711f.dp).defaultMinSize(minWidth = 26.02f.dp, minHeight = 18f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Ziele",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFC4623C),
                    fontFamily = Inter,
                ),
            )
        }
        // b04-nav-label
        Box(Modifier.offset(x = 213.48f.dp, y = 711f.dp).defaultMinSize(minWidth = 48.02f.dp, minHeight = 18f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Merkliste",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // b04-nav-label
        Box(Modifier.offset(x = 292.59f.dp, y = 711f.dp).defaultMinSize(minWidth = 69.38f.dp, minHeight = 18f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Erkenntnisse",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // b04-nav-label
        Box(Modifier.offset(x = 394.06f.dp, y = 711f.dp).defaultMinSize(minWidth = 46.05f.dp, minHeight = 18f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Logbuch",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        }
    }
}
