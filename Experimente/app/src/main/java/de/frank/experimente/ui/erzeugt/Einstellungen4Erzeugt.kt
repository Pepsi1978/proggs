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
 * Quelle: `4-einstellungen.json` aus `Specs/<App>/v2/messung/`.
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
fun Einstellungen4Erzeugt(
    modifier: Modifier = Modifier,
    beiFunktion: (String) -> Unit = {},
    beiNavigation: (String) -> Unit = {},
) {
    // Der Entwurf ist 1822 dp hoch, das Sichtfenster nur 751 dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(1822f.dp)) {
        // werft-b08__topbar
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 64f.dp).shadow(elevation = 10f.dp, clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).background(Color(0xFF151210).copy(alpha = 0.82f))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.08f))) }
        // werft-b08
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 751f.dp).background(Brush.radialGradient(listOf(Color(0xFFC4623C).copy(alpha = 0.18f), Color(0xFF000000).copy(alpha = 0f), Color(0xFF6F8F6A).copy(alpha = 0.12f), Color(0xFF000000).copy(alpha = 0f)))))
        // werft-b08__title
        Box(Modifier.offset(x = 20f.dp, y = 14.5f.dp).defaultMinSize(minWidth = 163.39f.dp, minHeight = 34f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Einstellungen",
                style = TextStyle(
                    fontSize = 28f.sp, lineHeight = 34f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                    fontFamily = Fraunces,
                ),
            )
        }
        // werft-b08__section
        Box(Modifier.offset(x = 16f.dp, y = 80f.dp).size(width = 443f.dp, height = 365f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.07f), spotColor = Color(0xFFC4623C).copy(alpha = 0.07f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 101f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__select
        Box(Modifier.offset(x = 206f.dp, y = 102f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-22") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "GPT 5.6 Terra",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 112.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Modell",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 161f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__select
        Box(Modifier.offset(x = 206f.dp, y = 162f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-22") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Hoch",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 172.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Effort",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__subgroup-title
        Box(Modifier.offset(x = 37f.dp, y = 229f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Logbuch",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 262f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__select
        Box(Modifier.offset(x = 206f.dp, y = 263f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-22") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "GPT 5.6 Luna",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 273.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Modell",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 322f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__select
        Box(Modifier.offset(x = 206f.dp, y = 323f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-22") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Mittel",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 333.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Effort",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__hint
        Box(Modifier.offset(x = 37f.dp, y = 382f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 42f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Das Logbuch darf ein anderes Modell benutzen als die Experimente.",
                style = TextStyle(
                    fontSize = 14f.sp, lineHeight = 21f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__section
        Box(Modifier.offset(x = 16f.dp, y = 477f.dp).size(width = 443f.dp, height = 419f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.07f), spotColor = Color(0xFFC4623C).copy(alpha = 0.07f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__section-title
        Box(Modifier.offset(x = 37f.dp, y = 498f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 17f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Stimme".uppercase(),
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 17f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 527f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__select
        Box(Modifier.offset(x = 206f.dp, y = 528f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-23") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Google Chirp 3 HD",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 538.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Anbieter",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 587f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__select
        Box(Modifier.offset(x = 206f.dp, y = 588f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-23") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "de-DE-Chirp3-HD-Kore",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 598.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Stimme",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__range
        Box(Modifier.offset(x = 205f.dp, y = 767f.dp).size(width = 177f.dp, height = 48f.dp).background(Color(0xFF3B3B3B)).clickable { beiFunktion("F-23") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "1",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 778.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Sprechgeschwindigkeit",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__range-value
        Box(Modifier.offset(x = 394f.dp, y = 782f.dp).defaultMinSize(minWidth = 44f.dp, minHeight = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "1,00",
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        // werft-b08__action
        Box(Modifier.offset(x = 37f.dp, y = 827f.dp).size(width = 124.27f.dp, height = 48f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF201B17).copy(alpha = 0.78f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(14f.dp)).clickable { beiFunktion("F-23") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // span
        Box(Modifier.offset(x = 54f.dp, y = 842f.dp).defaultMinSize(minWidth = 90.27f.dp, minHeight = 17f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Probe hören",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFC4623C),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__section
        Box(Modifier.offset(x = 16f.dp, y = 928f.dp).size(width = 443f.dp, height = 299f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.07f), spotColor = Color(0xFFC4623C).copy(alpha = 0.07f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__section-title
        Box(Modifier.offset(x = 37f.dp, y = 949f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 17f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Zugänge".uppercase(),
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 17f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__action
        Box(Modifier.offset(x = 330.34f.dp, y = 978f.dp).size(width = 107.66f.dp, height = 48f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF201B17).copy(alpha = 0.78f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(14f.dp)).clickable { beiFunktion("F-24") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__access-name
        Box(Modifier.offset(x = 37f.dp, y = 989.5f.dp).defaultMinSize(minWidth = 96f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Codex",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__access-state
        Box(Modifier.offset(x = 145f.dp, y = 991.5f.dp).defaultMinSize(minWidth = 173.34f.dp, minHeight = 21f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "nicht angemeldet",
                style = TextStyle(
                    fontSize = 14f.sp, lineHeight = 21f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 347.34f.dp, y = 993f.dp).defaultMinSize(minWidth = 73.66f.dp, minHeight = 17f.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Anmelden",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFC4623C),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 1038f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__password
        Box(Modifier.offset(x = 206f.dp, y = 1039f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-24") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "••••••••••••",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 1049.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Groq",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 1098f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__password
        Box(Modifier.offset(x = 206f.dp, y = 1099f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-24") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "••••••••••••",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 1109.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Google Cloud",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__control-shell
        Box(Modifier.offset(x = 205f.dp, y = 1158f.dp).size(width = 233f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__password
        Box(Modifier.offset(x = 206f.dp, y = 1159f.dp).size(width = 231f.dp, height = 46f.dp).background(Color(0xFF2A231D)).clickable { beiFunktion("F-24") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "••••••••••••",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__label
        Box(Modifier.offset(x = 37f.dp, y = 1169.5f.dp).defaultMinSize(minWidth = 156f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Alibaba",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__section
        Box(Modifier.offset(x = 16f.dp, y = 1259f.dp).size(width = 443f.dp, height = 183f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.07f), spotColor = Color(0xFFC4623C).copy(alpha = 0.07f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__section-title
        Box(Modifier.offset(x = 37f.dp, y = 1280f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 17f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Erinnerungen".uppercase(),
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 17f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__reminder-row
        Box(Modifier.offset(x = 37f.dp, y = 1309f.dp).size(width = 401f.dp, height = 50f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__time
        Box(Modifier.offset(x = 261f.dp, y = 1310f.dp).size(width = 104f.dp, height = 48f.dp).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D)).border(1f.dp, Color(0xFF38302A), RoundedCornerShape(14f.dp)).clickable { beiFunktion("F-25") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "08:00",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        // input
        Box(Modifier.offset(x = 377f.dp, y = 1310f.dp).size(width = 48f.dp, height = 48f.dp).clickable { beiFunktion("F-25") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "on",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__switch-track
        Box(Modifier.offset(x = 377f.dp, y = 1320f.dp).size(width = 48f.dp, height = 28f.dp).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF2A231D)).border(1f.dp, Color(0xFF38302A), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__reminder-name
        Box(Modifier.offset(x = 50f.dp, y = 1321.5f.dp).defaultMinSize(minWidth = 199f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Morgens",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__reminder-row
        Box(Modifier.offset(x = 37f.dp, y = 1371f.dp).size(width = 401f.dp, height = 50f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__time
        Box(Modifier.offset(x = 261f.dp, y = 1372f.dp).size(width = 104f.dp, height = 48f.dp).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D)).border(1f.dp, Color(0xFF38302A), RoundedCornerShape(14f.dp)).clickable { beiFunktion("F-25") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "20:30",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        // input
        Box(Modifier.offset(x = 377f.dp, y = 1372f.dp).size(width = 48f.dp, height = 48f.dp).clickable { beiFunktion("F-25") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "on",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__switch-track
        Box(Modifier.offset(x = 377f.dp, y = 1382f.dp).size(width = 48f.dp, height = 28f.dp).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF2A231D)).border(1f.dp, Color(0xFF38302A), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b08__reminder-name
        Box(Modifier.offset(x = 50f.dp, y = 1383.5f.dp).defaultMinSize(minWidth = 199f.dp, minHeight = 25f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Abends",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 25f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__section
        Box(Modifier.offset(x = 16f.dp, y = 1474f.dp).size(width = 443f.dp, height = 127f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.07f), spotColor = Color(0xFFC4623C).copy(alpha = 0.07f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__section-title
        Box(Modifier.offset(x = 37f.dp, y = 1495f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 17f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Erscheinung".uppercase(),
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 17f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__appearance
        Box(Modifier.offset(x = 37f.dp, y = 1524f.dp).size(width = 401f.dp, height = 56f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // input
        Box(Modifier.offset(x = 43f.dp, y = 1529f.dp).size(width = 129.66f.dp, height = 44f.dp).clickable { beiFunktion("F-26") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "light",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 43f.dp, y = 1529f.dp).defaultMinSize(minWidth = 129.66f.dp, minHeight = 44f.dp).clip(RoundedCornerShape(percent = 50)), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Hell",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 20f.sp,
                    fontWeight = FontWeight(500), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 172.66f.dp, y = 1529f.dp).size(width = 129.67f.dp, height = 44f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.2f), spotColor = Color(0xFFC4623C).copy(alpha = 0.2f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF3A231A)), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Dunkel",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 20f.sp,
                    fontWeight = FontWeight(500), color = Color(0xFFC4623C),
                    fontFamily = Inter,
                ),
            )
        }
        // input
        Box(Modifier.offset(x = 172.66f.dp, y = 1529f.dp).size(width = 129.67f.dp, height = 44f.dp).clickable { beiFunktion("F-26") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "dark",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // input
        Box(Modifier.offset(x = 302.33f.dp, y = 1529f.dp).size(width = 129.67f.dp, height = 44f.dp).clickable { beiFunktion("F-26") }, contentAlignment = Alignment.CenterStart) {
            Text(
                text = "system",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 302.33f.dp, y = 1529f.dp).defaultMinSize(minWidth = 129.67f.dp, minHeight = 44f.dp).clip(RoundedCornerShape(percent = 50)), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Wie das System",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 20f.sp,
                    fontWeight = FontWeight(500), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__section
        Box(Modifier.offset(x = 16f.dp, y = 1633f.dp).size(width = 443f.dp, height = 119f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.07f), spotColor = Color(0xFFC4623C).copy(alpha = 0.07f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b08__section-title
        Box(Modifier.offset(x = 37f.dp, y = 1654f.dp).defaultMinSize(minWidth = 401f.dp, minHeight = 17f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Über mich".uppercase(),
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 17f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__nav-row
        Box(Modifier.offset(x = 37f.dp, y = 1683f.dp).size(width = 401f.dp, height = 48f.dp).shadow(elevation = 12f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.14f), spotColor = Color(0xFF000000).copy(alpha = 0.14f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.05f), spotColor = Color(0xFFC4623C).copy(alpha = 0.05f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF2A231D).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.88f), RoundedCornerShape(14f.dp)).clickable { beiNavigation("B-09") }) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // svg
        Box(Modifier.offset(x = 401f.dp, y = 1695f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(960f, 960f,
                    "M480-160 160-480l320-320 57 56-224 224h487v80H313l224 224-57 56Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // span
        Box(Modifier.offset(x = 54f.dp, y = 1698f.dp).defaultMinSize(minWidth = 69.38f.dp, minHeight = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Selbstbild",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFF4EEE7),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b08__version
        Box(Modifier.offset(x = 16f.dp, y = 1784f.dp).defaultMinSize(minWidth = 443f.dp, minHeight = 38f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Version",
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        }
    }
}
