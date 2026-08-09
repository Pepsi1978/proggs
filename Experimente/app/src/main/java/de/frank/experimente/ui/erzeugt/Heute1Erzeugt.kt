package de.frank.experimente.ui.erzeugt

import androidx.compose.foundation.background
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
 * Quelle: `1-heute.json` aus `Specs/<App>/v2/messung/`.
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
fun Heute1Erzeugt(modifier: Modifier = Modifier) {
    // Der Entwurf ist 2146 dp hoch, das Sichtfenster nur 751 dp.
    // Ohne Scroll-Bereich waere alles darunter unerreichbar.
    Box(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(2146f.dp)) {
        // werft-b01
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 751f.dp).background(Brush.radialGradient(listOf(Color(0xFFC4623C).copy(alpha = 0.18f), Color(0xFF000000).copy(alpha = 0f), Color(0xFF6F8F6A).copy(alpha = 0.12f), Color(0xFF000000).copy(alpha = 0f)))))
        // werft-b01__topbar
        Box(Modifier.offset(x = 0f.dp, y = 0f.dp).size(width = 475f.dp, height = 64f.dp).shadow(elevation = 10f.dp, clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).background(Color(0xFF151210).copy(alpha = 0.82f))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.08f))) }
        // werft-b01__theme-control
        Box(Modifier.offset(x = 355f.dp, y = 7.5f.dp).size(width = 48f.dp, height = 48f.dp).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF201B17).copy(alpha = 0.84f)).border(1f.dp, Color(0xFF38302A), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b01__icon-action
        Box(Modifier.offset(x = 407f.dp, y = 7.5f.dp).size(width = 48f.dp, height = 48f.dp).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(percent = 50)).background(Color(0xFF201B17).copy(alpha = 0.78f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.12f))) }
        // werft-b01__title
        Box(Modifier.offset(x = 20f.dp, y = 14.5f.dp).size(width = 71.53f.dp, height = 34f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Heute",
                style = TextStyle(
                    fontSize = 28f.sp, lineHeight = 34f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                    fontFamily = Fraunces,
                ),
            )
        }
        // svg
        Box(Modifier.offset(x = 419f.dp, y = 19.5f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7.13 7.13 0 0 0-1.62-.94L14.4 2.81A.48.48 0 0 0 13.92 2h-3.84a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.72 8.47a.48.48 0 0 0 .12.61l2.03 1.58c-.05.31-.09.65-.09.98s.03.66.08.98l-2.02 1.57a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.38.31.59.22l2.39-.96c.49.38 1.03.7 1.62.94l.36 2.54c.04.23.24.41.48.41h3.84c.24 0 .44-.18.48-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.09.47 0 .59-.22l1.92-3.32a.49.49 0 0 0-.12-.61l-2.02-1.57ZM12 15.5A3.5 3.5 0 1 1 12 8a3.5 3.5 0 0 1 0 7.5Z",
                ),
                contentDescription = null, tint = Color(0xFFF4EEE7),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 369.75f.dp, y = 22.25f.dp).size(width = 20.5f.dp, height = 20.5f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 369.75f.dp, y = 22.25f.dp).size(width = 20.5f.dp, height = 20.5f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M8.25 18 12 6l3.75 12M9.7 13.5h4.6M5.5 7.5 3.75 5.75M4.5 12H2M5.5 16.5l-1.75 1.75M18.5 7.5l1.75-1.75M19.5 12H22M18.5 16.5l1.75 1.75",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 370f.dp, y = 22.5f.dp).size(width = 20f.dp, height = 20f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M11 1h2v3h-2V1Zm0 19h2v3h-2v-3ZM3.515 4.929l1.414-1.414L7.05 5.636 5.636 7.05 3.515 4.929Zm13.435 13.435 1.414-1.414 2.121 2.121-1.414 1.414-2.121-2.121ZM1 11h3v2H1v-2Zm19 0h3v2h-3v-2ZM3.515 19.071l2.121-2.121 1.414 1.414-2.121 2.121-1.414-1.414ZM16.95 5.636l2.121-2.121 1.414 1.414-2.121 2.121-1.414-1.414ZM12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // werft-b01__date
        Box(Modifier.offset(x = 20f.dp, y = 84f.dp).size(width = 435f.dp, height = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "SONNTAG, 9. AUGUST 2026",
                style = TextStyle(
                    fontSize = 13f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = JetBrainsMono,
                ),
            )
        }
        // werft-b01__intro
        Box(Modifier.offset(x = 20f.dp, y = 134f.dp).size(width = 435f.dp, height = 99f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF201B17).copy(alpha = 0.82f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(20f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b01__section-title
        Box(Modifier.offset(x = 41f.dp, y = 155f.dp).size(width = 393f.dp, height = 28f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Wie ist deine Lage heute?",
                style = TextStyle(
                    fontSize = 22f.sp, lineHeight = 28f.sp,
                    fontWeight = FontWeight(600), color = Color(0xFFF4EEE7),
                    fontFamily = Fraunces,
                ),
            )
        }
        // werft-b01__subtitle
        Box(Modifier.offset(x = 41f.dp, y = 191f.dp).size(width = 393f.dp, height = 21f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Was für ein Tag ist das? Was liegt vor dir?",
                style = TextStyle(
                    fontSize = 14f.sp, lineHeight = 21f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b01__recording-ring
        Box(Modifier.offset(x = 185.5f.dp, y = 257f.dp).size(width = 104f.dp, height = 104f.dp).clip(RoundedCornerShape(percent = 50)).border(2f.dp, Color(0xFFC4623C), RoundedCornerShape(percent = 50)))
        // werft-b01__mic
        Box(Modifier.offset(x = 193.5f.dp, y = 265f.dp).size(width = 88f.dp, height = 88f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.3f), spotColor = Color(0xFFC4623C).copy(alpha = 0.3f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.26f), spotColor = Color(0xFF000000).copy(alpha = 0.26f)).clip(RoundedCornerShape(percent = 50)).background(Brush.linearGradient(0f to Color(0xFFC9704D), 0.58f to Color(0xFFC4623C), 1f to Color(0xFFA55232))).border(1f.dp, Color(0xFFF4EEE7).copy(alpha = 0.28f), RoundedCornerShape(percent = 50))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.28f))) }
        // svg
        Box(Modifier.offset(x = 223.5f.dp, y = 295f.dp).size(width = 28f.dp, height = 28f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M12 14q-1.25 0-2.125-.875T9 11V5q0-1.25.875-2.125T12 2q1.25 0 2.125.875T15 5v6q0 1.25-.875 2.125T12 14Zm-1 7v-3.075q-2.6-.35-4.3-2.325T5 11h2q0 2.075 1.463 3.538T12 16q2.075 0 3.538-1.462T17 11h2q0 2.6-1.7 4.575T13 17.925V21Z",
                ),
                contentDescription = null, tint = Color(0xFF151210),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // werft-b01__text-action
        Box(Modifier.offset(x = 172.94f.dp, y = 391f.dp).size(width = 129.13f.dp, height = 48f.dp).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.16f), spotColor = Color(0xFF000000).copy(alpha = 0.16f)).shadow(elevation = 8f.dp, shape = RoundedCornerShape(14f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(14f.dp)).background(Color(0xFF3A231A).copy(alpha = 0.72f)).border(1f.dp, Color(0xFFC4623C).copy(alpha = 0.24f), RoundedCornerShape(14f.dp)), contentAlignment = Alignment.Center) {
            Text(
                text = "Lieber tippen",
                style = TextStyle(
                    fontSize = 16f.sp, lineHeight = 24f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFC4623C),
                    fontFamily = Inter,
                ),
            )
        }
        // werft-b01__bottomnav
        Box(Modifier.offset(x = 12f.dp, y = 675f.dp).size(width = 451f.dp, height = 64f.dp).shadow(elevation = 16f.dp, shape = RoundedCornerShape(24f.dp), clip = false, ambientColor = Color(0xFF000000).copy(alpha = 0.18f), spotColor = Color(0xFF000000).copy(alpha = 0.18f)).shadow(elevation = 12f.dp, shape = RoundedCornerShape(24f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.06f), spotColor = Color(0xFFC4623C).copy(alpha = 0.06f)).clip(RoundedCornerShape(24f.dp)).background(Color(0xFF201B17).copy(alpha = 0.88f)).border(1f.dp, Color(0xFF38302A).copy(alpha = 0.84f), RoundedCornerShape(24f.dp))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFF4EEE7).copy(alpha = 0.1f))) }
        // werft-b01__nav-item
        Box(Modifier.offset(x = 17f.dp, y = 680f.dp).size(width = 81.8f.dp, height = 54f.dp).shadow(elevation = 10f.dp, shape = RoundedCornerShape(20f.dp), clip = false, ambientColor = Color(0xFFC4623C).copy(alpha = 0.18f), spotColor = Color(0xFFC4623C).copy(alpha = 0.18f)).clip(RoundedCornerShape(20f.dp)).background(Color(0xFF3A231A))) { Box(Modifier.fillMaxWidth().height(1f.dp).background(Color(0xFFC4623C).copy(alpha = 0.18f))) }
        // svg
        Box(Modifier.offset(x = 45.89f.dp, y = 685f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M5 22q-.825 0-1.412-.587T3 20V6q0-.825.588-1.412T5 4h1V2h2v2h8V2h2v2h1q.825 0 1.413.588T21 6v14q0 .825-.587 1.413T19 22H5Zm0-2h14V10H5v10Zm0-12h14V6H5v2Zm7 6q-.825 0-1.412-.587T10 12q0-.825.588-1.412T12 10q.825 0 1.413.588T14 12q0 .825-.587 1.413T12 14Z",
                ),
                contentDescription = null, tint = Color(0xFFC4623C),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 135.69f.dp, y = 685f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M5 21V4h9l.4 2H20v10h-7l-.4-2H7v7H5Zm2-9h7.25l.4 2H18V8h-5.25l-.4-2H7v6Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 225.48f.dp, y = 685f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M7 3h10q.825 0 1.413.588T19 5v16l-7-3-7 3V5q0-.825.588-1.412T7 3Zm0 3v11.95l5-2.15 5 2.15V5H7Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 315.28f.dp, y = 685f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M9 21q-.425 0-.712-.288T8 20v-1h8v1q0 .425-.288.713T15 21H9Zm-1-4v-2.2q-1.4-.85-2.2-2.225T5 9.5q0-2.725 1.888-4.612T11.5 3q2.725 0 4.613 1.888T18 9.5q0 1.7-.8 3.075T15 14.8V17H8Zm2-2h3v-1.3l.95-.55q.95-.55 1.5-1.5T16 9.5q0-1.875-1.312-3.187T11.5 5Q9.625 5 8.313 6.313T7 9.5q0 1.2.55 2.15t1.5 1.5l.95.55V15Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // svg
        Box(Modifier.offset(x = 405.09f.dp, y = 685f.dp).size(width = 24f.dp, height = 24f.dp)) {
            Icon(
                imageVector = ausPfaden(24f, 24f,
                    "M13 3q3.325 0 5.663 2.338T21 11q0 3.325-2.337 5.663T13 19q-2.075 0-3.85-1T6.2 15.25L4 17.45V11H2q0-4.575 3.213-7.787T13 0v3Zm0 2Q9.675 5 7.338 7.338T5 13h1.8l-.05 1.65 1.1-1.1.7 1.1Q9.35 15.8 10.5 16.4T13 17q2.5 0 4.25-1.75T19 11q0-2.5-1.75-4.25T13 5Zm-1 2v5l4.25 2.5 1-1.65-3.25-1.9V7h-2Z",
                ),
                contentDescription = null, tint = Color(0xFFA99C8F),
                modifier = Modifier.fillMaxSize(),
            )
        }
        // span
        Box(Modifier.offset(x = 41.88f.dp, y = 711f.dp).size(width = 32.03f.dp, height = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Heute",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFC4623C),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 134.69f.dp, y = 711f.dp).size(width = 26.02f.dp, height = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Ziele",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 213.48f.dp, y = 711f.dp).size(width = 48.02f.dp, height = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Merkliste",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 292.59f.dp, y = 711f.dp).size(width = 69.38f.dp, height = 18f.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "Erkenntnisse",
                style = TextStyle(
                    fontSize = 12f.sp, lineHeight = 18f.sp,
                    fontWeight = FontWeight(400), color = Color(0xFFA99C8F),
                    fontFamily = Inter,
                ),
            )
        }
        // span
        Box(Modifier.offset(x = 394.06f.dp, y = 711f.dp).size(width = 46.05f.dp, height = 18f.dp), contentAlignment = Alignment.CenterStart) {
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
