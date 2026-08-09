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
 * Quelle: `5-selbstbild.json` aus `Specs/<App>/v2/messung/`.
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
fun Selbstbild5Erzeugt(modifier: Modifier = Modifier) {
    // Der Entwurf ist 751 dp hoch, das Sichtfenster nur 751 dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(751f.dp)) {
        // b09-screen
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 751f.dp).background(Brush.radialGradient(listOf(Color(0xFFC4623C).copy(alpha = 0.18f), Color(0xFF000000).copy(alpha = 0f), Color(0xFF6F8F6A).copy(alpha = 0.12f), Color(0xFF000000).copy(alpha = 0f)))))
        // b09-topbar
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 64f.dp).shadow(elevation = 10f.dp, clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).background(Color(0xFF151210).copy(alpha = 0.82f))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.08f))) }
        // b09-back
        Box(Modifier.offset(x = 20f.dp, y = 7.5f.dp).size(width = 48f.dp, height = 48f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF201B17).copy(alpha = 0.78f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // b09-title
        Box(Modifier.offset(x = 80f.dp, y = 14.5f.dp).size(width = 118.28f.dp, height = 34f.dp)) {
            Text(
                text = "Selbstbild",
                style = TextStyle(
                    fontSize = 28f.sp, lineHeight = 34f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                ),
            )
        }
        // svg
        Box(Modifier.offset(x = 32f.dp, y = 19.5f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M20 11H7.83l5.59-5.59c.39-.39.39-1.03 0-1.42-.39-.39-1.03-.39-1.42 0l-7.29 7.3c-.39.39-.39 1.02 0 1.41L12 20c.39.39 1.03.39 1.42 0 .39-.39.39-1.03 0-1.42L7.83 13H20c.55 0 1-.45 1-1s-.45-1-1-1z",
                ),
                contentDescription = null, tint = Color(0xFFF4EEE7),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // b09-intro
        Box(Modifier.offset(x = 20f.dp, y = 84f.dp).size(width = 435f.dp, height = 76f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) {
            Text(
                text = "Alles, was die App dauerhaft über dich wissen soll. Je mehr hier steht, desto genauer treffen die Vorschläge.",
                style = TextStyle(
                    fontSize = 14f.sp, lineHeight = 21f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                ),
            )
        }
        // b09-editor
        Box(Modifier.offset(x = 20f.dp, y = 176f.dp).size(width = 435f.dp, height = 447f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 10f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.9f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // b09-mic-button
        Box(Modifier.offset(x = 193.5f.dp, y = 643f.dp).size(width = 88f.dp, height = 88f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.3f), spotColor = Color(0xFFC4623C).copy(alpha = 0.3f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.26f), spotColor = Color(0xFF000000).copy(alpha = 0.26f)).clip(RoundedCornerShape(percent = 50)).background(Brush.linearGradient(0f to Color(0xFFC9704D), 0.58f to Color(0xFFC4623C), 1f to Color(0xFFA55232)))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.28f))) }
        // b09-improve
        Box(Modifier.offset(x = -15.19f.dp, y = 663f.dp).size(width = 188.69f.dp, height = 48f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF201B17).copy(alpha = 0.78f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // svg
        Box(Modifier.offset(x = 225.5f.dp, y = 675f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z",
                ),
                contentDescription = null, tint = Color(0xFFF4EEE7),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // b09-improve-label
        Box(Modifier.offset(x = -2.19f.dp, y = 678f.dp).size(width = 162.69f.dp, height = 17f.dp)) {
            Text(
                text = "Text mit KI verbessern",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 20f.sp,
                    fontWeight = FontWeight(500), color = Color(0xFFC4623C),
                ),
            )
        }
        }
    }
}
