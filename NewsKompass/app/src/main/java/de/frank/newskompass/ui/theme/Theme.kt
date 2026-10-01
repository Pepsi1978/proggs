package de.frank.newskompass.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.newskompass.data.model.FarbDesign

private val Hell = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF1E1B6B),
    secondary = Color(0xFFDB2777),
    secondaryContainer = Color(0xFFFFD9E6),
    tertiary = Color(0xFF0891B2),
    background = Color(0xFFF4F2FB),
    onBackground = Color(0xFF15142B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15142B),
    surfaceVariant = Color(0xFFEDEAF7),
    onSurfaceVariant = Color(0xFF55536E),
    surfaceContainer = Color(0xFFF9F8FE),
    surfaceContainerHigh = Color(0xFFF0EEF9),
    outline = Color(0xFFCAC6DC),
    outlineVariant = Color(0xFFE3E0EF),
)

private val Dunkel = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B6B),
    primaryContainer = Color(0xFF2E2A7A),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFF9A8D4),
    secondaryContainer = Color(0xFF5B1438),
    tertiary = Color(0xFF67E8F9),
    background = Color(0xFF0D0F1C),
    onBackground = Color(0xFFE8E6F5),
    surface = Color(0xFF161932),
    onSurface = Color(0xFFE8E6F5),
    surfaceVariant = Color(0xFF22254A),
    onSurfaceVariant = Color(0xFFB3B0CC),
    surfaceContainer = Color(0xFF1A1D3A),
    surfaceContainerHigh = Color(0xFF232748),
    outline = Color(0xFF4A4D72),
    outlineVariant = Color(0xFF2C2F55),
)

/** Orange-Schwarz, hell: warmes Creme mit kräftigem Orange und fast schwarzer Schrift. */
private val OrangeHell = lightColorScheme(
    primary = Color(0xFFE8650A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0C7),
    onPrimaryContainer = Color(0xFF3A1A04),
    secondary = Color(0xFFC2410C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8BE),
    onSecondaryContainer = Color(0xFF3A1606),
    tertiary = Color(0xFF8A3D06),
    tertiaryContainer = Color(0xFFFFE9D2),
    onTertiaryContainer = Color(0xFF2A1A0E),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF1A120C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A120C),
    surfaceVariant = Color(0xFFFCEBDD),
    onSurfaceVariant = Color(0xFF6E5343),
    surfaceContainer = Color(0xFFFFF3E8),
    surfaceContainerHigh = Color(0xFFFCEBDD),
    surfaceContainerHighest = Color(0xFFF8E2D0),
    outline = Color(0xFFE3C2A8),
    outlineVariant = Color(0xFFF2DCCB),
    inverseSurface = Color(0xFF1A120C),
    inverseOnSurface = Color(0xFFFFF1E6),
)

/** Orange-Schwarz, dunkel: tiefes Schwarz, leuchtendes Orange. */
private val OrangeDunkel = darkColorScheme(
    primary = Color(0xFFFF8A1F),
    onPrimary = Color(0xFF1A0E05),
    primaryContainer = Color(0xFF3A2414),
    onPrimaryContainer = Color(0xFFFFD9B8),
    secondary = Color(0xFFFFA64D),
    onSecondary = Color(0xFF1A0E05),
    secondaryContainer = Color(0xFF2E1A0C),
    onSecondaryContainer = Color(0xFFFFE0C2),
    tertiary = Color(0xFFFFB86B),
    tertiaryContainer = Color(0xFF33200F),
    onTertiaryContainer = Color(0xFFFFE3C8),
    background = Color(0xFF050404),
    onBackground = Color(0xFFF7F1EC),
    surface = Color(0xFF110D0A),
    onSurface = Color(0xFFF7F1EC),
    surfaceVariant = Color(0xFF1E1610),
    onSurfaceVariant = Color(0xFFC9B8AA),
    surfaceContainer = Color(0xFF0E0B09),
    surfaceContainerHigh = Color(0xFF17120E),
    surfaceContainerHighest = Color(0xFF211912),
    outline = Color(0xFF5A4232),
    outlineVariant = Color(0xFF2A2018),
    inverseSurface = Color(0xFFF7F1EC),
    inverseOnSurface = Color(0xFF1A120C),
)

/** Farben, die nicht im Material-Schema stehen: Kopf, Mikrofon-Knopf und die Verläufe der Themenblöcke. */
@androidx.compose.runtime.Immutable
data class KompassFarben(
    val kopfVerlauf: List<Color>,
    val knopfVerlauf: List<Color>,
    val blockVerlaeufe: List<List<Color>>,
)

private val KompassBloecke = listOf(
    listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
    listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6)),
    listOf(Color(0xFFF97316), Color(0xFFE11D48)),
    listOf(Color(0xFF10B981), Color(0xFF65A30D)),
    listOf(Color(0xFFEAB308), Color(0xFFF97316)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
)

/** Alles in der Orange-Familie — vom hellen Bernstein bis zum tiefen Kupfer, damit die Blöcke trotzdem unterscheidbar bleiben. */
private val OrangeBloecke = listOf(
    listOf(Color(0xFFFF8A1F), Color(0xFFE5520A)),
    listOf(Color(0xFFFFB020), Color(0xFFF07A0C)),
    listOf(Color(0xFFE5520A), Color(0xFFB8330F)),
    listOf(Color(0xFFF59E0B), Color(0xFFD9620A)),
    listOf(Color(0xFFFF7043), Color(0xFFC2410C)),
    listOf(Color(0xFFD9822B), Color(0xFF8A3D06)),
)

fun kompassFarben(design: FarbDesign, dunkel: Boolean): KompassFarben = when (design) {
    FarbDesign.KOMPASS -> KompassFarben(
        kopfVerlauf = if (dunkel) listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF831843))
        else listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)),
        knopfVerlauf = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)),
        blockVerlaeufe = KompassBloecke,
    )
    FarbDesign.ORANGE -> KompassFarben(
        // Dunkel: aus tiefem Schwarz in glühendes Orange; hell: kräftiges Orange bis Bernstein.
        kopfVerlauf = if (dunkel) listOf(Color(0xFF000000), Color(0xFF2A1406), Color(0xFFB8520F))
        else listOf(Color(0xFFC2410C), Color(0xFFEE6A0C), Color(0xFFFFA43D)),
        knopfVerlauf = listOf(Color(0xFFFF8A1F), Color(0xFFE5520A), Color(0xFFB8330F)),
        blockVerlaeufe = OrangeBloecke,
    )
}

val LocalKompassFarben = staticCompositionLocalOf { kompassFarben(FarbDesign.KOMPASS, false) }

/** Jeder Themenblock bekommt seinen eigenen Farbverlauf — so erkennt man den Block sofort. */
@Composable
fun blockVerlauf(index: Int): Brush {
    val verlaeufe = LocalKompassFarben.current.blockVerlaeufe
    return Brush.linearGradient(verlaeufe[index.mod(verlaeufe.size)])
}

@Composable
fun blockFarbe(index: Int): Color {
    val verlaeufe = LocalKompassFarben.current.blockVerlaeufe
    return verlaeufe[index.mod(verlaeufe.size)][0]
}

val LocalIstDunkel = staticCompositionLocalOf { false }

private val Schrift = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
)

@Composable
fun NewsTheme(dunkel: Boolean, design: FarbDesign = FarbDesign.KOMPASS, inhalt: @Composable () -> Unit) {
    val schema = when (design) {
        FarbDesign.KOMPASS -> if (dunkel) Dunkel else Hell
        FarbDesign.ORANGE -> if (dunkel) OrangeDunkel else OrangeHell
    }
    CompositionLocalProvider(LocalIstDunkel provides dunkel, LocalKompassFarben provides kompassFarben(design, dunkel)) {
        MaterialTheme(colorScheme = schema, typography = Schrift, content = inhalt)
    }
}
