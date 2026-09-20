package de.frank.genialeideen.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.genialeideen.R

val LocalGold = staticCompositionLocalOf { HelleGoldPalette }

/**
 * Die semantischen Farben in der Fassung, die auf dem aktuellen Untergrund lesbar ist.
 * Immer hierüber lesen statt [Semantisch] direkt — sonst steht ein Warnhinweis im hellen
 * Modus mit gemessenen 1,79:1 praktisch unsichtbar auf der Karte.
 */
val LocalSemantisch = staticCompositionLocalOf { semantischeFarben(dunkel = false) }

/**
 * Alle drei Schriftdateien sind **variable** Schriften mit einer `wght`-Achse — nachgemessen in
 * den `fvar`-Tabellen: Inter 100–900, Newsreader 200–800, JetBrains Mono 100–800.
 *
 * Registriert war bisher je **eine** feste Instanz (Inter bei Normal, Mono bei Medium,
 * Newsreader bei SemiBold). Damit lief jede Gewichtsangabe der Designs ins Leere: Android
 * synthetisiert nur Fett, und das auch nur ab 600 und bei genügend Abstand. Morgenruhes leichte
 * Uhr blieb Regular, Traumraums kräftige Uhr wurde ein gleichmäßig verdicktes Faux-Bold, Orbits
 * halbfette Ziffern blieben Medium. Zwei der vier Designs unterschieden sich dadurch in der
 * Kopfleiste praktisch nur noch durch die Farbe.
 *
 * Jetzt trägt jede Stufe ihre echte Achsenposition. [achse] hält Gewichtsangabe und
 * Achsenwert zusammen, damit beide nicht auseinanderlaufen können.
 */
@OptIn(ExperimentalTextApi::class)
private fun achse(resId: Int, gewicht: FontWeight) = Font(
    resId = resId,
    weight = gewicht,
    variationSettings = FontVariation.Settings(FontVariation.weight(gewicht.weight)),
)

/** Die Grotesk der Oberfläche — Inter deckt 100 bis 900 ab. */
val IdeenSchrift = FontFamily(
    achse(R.font.inter, FontWeight.Light),
    achse(R.font.inter, FontWeight.Normal),
    achse(R.font.inter, FontWeight.Medium),
    achse(R.font.inter, FontWeight.SemiBold),
    achse(R.font.inter, FontWeight.Bold),
)

/** Die Serifenschrift der Schlicht-Titel — Newsreader beginnt erst bei 200. */
val IdeenSchriftBetont = FontFamily(
    achse(R.font.newsreader, FontWeight.Light),
    achse(R.font.newsreader, FontWeight.Normal),
    achse(R.font.newsreader, FontWeight.Medium),
    achse(R.font.newsreader, FontWeight.SemiBold),
    achse(R.font.newsreader, FontWeight.Bold),
)

/**
 * Die Schrift für die Ideen-Überschriften: Inter, nicht die Serifenschrift. Serifen werden
 * künstlich fett schnell matschig, die Grotesk bleibt auch bei Black klar lesbar.
 */
val IdeenSchriftDick = IdeenSchrift

/** Festbreitenschrift für Anmeldecode und die Orbit-Zahlen — Mono deckt 100 bis 800 ab. */
val IdeenSchriftFest = FontFamily(
    achse(R.font.jetbrains_mono, FontWeight.Light),
    achse(R.font.jetbrains_mono, FontWeight.Normal),
    achse(R.font.jetbrains_mono, FontWeight.Medium),
    achse(R.font.jetbrains_mono, FontWeight.SemiBold),
    achse(R.font.jetbrains_mono, FontWeight.Bold),
)

/**
 * [titelSchrift] ersetzt die Schrift für Display-, Headline- und Title-Stile; ohne Angabe gilt
 * unverändert [IdeenSchriftBetont]. Fließtext und Beschriftungen bleiben in jedem Fall [IdeenSchrift].
 */
private fun typografie(skalierung: Float, titelSchrift: FontFamily = IdeenSchriftBetont) = Typography().let { standard ->
    Typography(
        displayLarge = standard.displayLarge.skaliert(skalierung, titelSchrift),
        displayMedium = standard.displayMedium.skaliert(skalierung, titelSchrift),
        displaySmall = standard.displaySmall.skaliert(skalierung, titelSchrift),
        headlineLarge = standard.headlineLarge.skaliert(skalierung, titelSchrift),
        headlineMedium = standard.headlineMedium.skaliert(skalierung, titelSchrift),
        headlineSmall = standard.headlineSmall.skaliert(skalierung, titelSchrift),
        titleLarge = standard.titleLarge.skaliert(skalierung, titelSchrift),
        titleMedium = standard.titleMedium.skaliert(skalierung, titelSchrift),
        titleSmall = standard.titleSmall.skaliert(skalierung, IdeenSchrift),
        bodyLarge = standard.bodyLarge.skaliert(skalierung, IdeenSchrift),
        bodyMedium = standard.bodyMedium.skaliert(skalierung, IdeenSchrift),
        bodySmall = standard.bodySmall.skaliert(skalierung, IdeenSchrift),
        labelLarge = standard.labelLarge.skaliert(skalierung, IdeenSchrift),
        labelMedium = standard.labelMedium.skaliert(skalierung, IdeenSchrift),
        labelSmall = standard.labelSmall.skaliert(skalierung, IdeenSchrift),
    )
}

private fun TextStyle.skaliert(faktor: Float, familie: FontFamily): TextStyle =
    copy(fontFamily = familie, fontSize = (fontSize.value * faktor).sp)

/**
 * @param themeWahl `light` oder `dark`. Es gibt bewusst **keinen** Automatik-Modus:
 *   Die App folgt der Systemvorgabe nicht (Baustein A).
 *
 * Der Wechsel läuft weich: Jede Farbe wandert in den neuen Wert, statt hart umzuspringen
 * (Baustein N.5).
 */
@Composable
fun GenialeIdeenTheme(
    themeWahl: String,
    schriftSkalierung: Float = 1f,
    /** Abweichende Farbwelt eines gewählten Designs; ohne Vorgabe gilt unverändert Gold. */
    paletteVorgabe: GoldPalette? = null,
    /** Abweichende Schrift für Titel und große Zahlen; ohne Vorgabe die bisherige Serifenschrift. */
    titelSchrift: FontFamily? = null,
    content: @Composable () -> Unit,
) {
    val dunkel = themeWahl == "dark"
    val ziel = paletteVorgabe ?: if (dunkel) DunkleGoldPalette else HelleGoldPalette
    val context = LocalContext.current
    val reduziert = Motion.bewegungReduziert(context)
    val dauer = if (reduziert) 0 else Motion.THEME_WECHSEL_MS

    @Composable
    fun weich(farbe: Color, name: String) =
        animateColorAsState(farbe, tween(dauer), label = name).value

    val palette = GoldPalette(
        hintergrund = weich(ziel.hintergrund, "hintergrund"),
        flaeche = weich(ziel.flaeche, "flaeche"),
        flaecheErhoeht = weich(ziel.flaecheErhoeht, "flaecheErhoeht"),
        primaer = weich(ziel.primaer, "primaer"),
        primaerGedaempft = weich(ziel.primaerGedaempft, "primaerGedaempft"),
        aufPrimaer = weich(ziel.aufPrimaer, "aufPrimaer"),
        akzentWarm = weich(ziel.akzentWarm, "akzentWarm"),
        textPrimaer = weich(ziel.textPrimaer, "textPrimaer"),
        textGedaempft = weich(ziel.textGedaempft, "textGedaempft"),
        rahmen = weich(ziel.rahmen, "rahmen"),
        eingabefeld = weich(ziel.eingabefeld, "eingabefeld"),
        istDunkel = ziel.istDunkel,
        // Die Hero-Rollen müssen mitkopiert werden, sonst fielen sie beim Themenwechsel auf die
        // Vorgaben zurück und der Hero verlöre genau die Fläche, die ihn abhebt. Sie werden
        // ebenso weich überblendet wie alles andere; `null` bleibt `null`.
        heroFlaeche = ziel.heroFlaeche?.let { weich(it, "heroFlaeche") },
        heroFlaecheUnten = ziel.heroFlaecheUnten?.let { weich(it, "heroFlaecheUnten") },
        heroRahmen = ziel.heroRahmen?.let { weich(it, "heroRahmen") },
        heroText = ziel.heroText?.let { weich(it, "heroText") },
        heroTextGedaempft = ziel.heroTextGedaempft?.let { weich(it, "heroTextGedaempft") },
        heroPrimaer = ziel.heroPrimaer?.let { weich(it, "heroPrimaer") },
    )

    // Material3 zieht sich viele Flächen NICHT aus `surface`, sondern aus den Container-Rollen:
    // `AlertDialog` etwa aus surfaceContainerHigh, Menüs und Blätter aus surfaceContainer.
    // Wurden die nicht gesetzt, gewann die Material-Grundpalette — dann stand ein graulila
    // Standarddialog mitten in einer goldenen, blauen oder orangefarbenen App. Deshalb ist hier
    // jede Rolle aus der eigenen Palette abgeleitet, statt nur die Handvoll Hauptfarben zu setzen.
    val schema = remember(dunkel, palette) {
        val flaecheHoch = palette.flaecheErhoeht
        val behaelterPrimaer = if (dunkel) palette.primaer.mischeMit(palette.flaeche, 0.74f)
            else palette.primaer.mischeMit(Color.White, 0.82f)
        val aufBehaelterPrimaer = if (dunkel) palette.primaer.heller(0.30f) else palette.primaer.dunkler(0.28f)
        val behaelterZweit = if (dunkel) palette.primaerGedaempft.mischeMit(palette.flaeche, 0.76f)
            else palette.primaerGedaempft.mischeMit(Color.White, 0.84f)
        val aufBehaelterZweit = if (dunkel) palette.primaerGedaempft.heller(0.34f) else palette.primaerGedaempft.dunkler(0.30f)
        val behaelterDritt = if (dunkel) palette.akzentWarm.mischeMit(palette.flaeche, 0.76f)
            else palette.akzentWarm.mischeMit(Color.White, 0.84f)
        val aufBehaelterDritt = if (dunkel) palette.akzentWarm.heller(0.30f) else palette.akzentWarm.dunkler(0.28f)
        val fehler = if (dunkel) Semantisch.fehler else Semantisch.fehlerHell
        val behaelterFehler = if (dunkel) fehler.mischeMit(palette.flaeche, 0.76f) else fehler.mischeMit(Color.White, 0.86f)
        val aufBehaelterFehler = if (dunkel) fehler.heller(0.30f) else fehler.dunkler(0.24f)
        if (dunkel) {
            darkColorScheme(
                primary = palette.primaer,
                onPrimary = palette.aufPrimaer,
                primaryContainer = behaelterPrimaer,
                onPrimaryContainer = aufBehaelterPrimaer,
                inversePrimary = palette.primaer.dunkler(0.35f),
                secondary = palette.primaerGedaempft,
                onSecondary = palette.aufPrimaer,
                secondaryContainer = behaelterZweit,
                onSecondaryContainer = aufBehaelterZweit,
                tertiary = palette.akzentWarm,
                onTertiary = palette.aufPrimaer,
                tertiaryContainer = behaelterDritt,
                onTertiaryContainer = aufBehaelterDritt,
                background = palette.hintergrund,
                onBackground = palette.textPrimaer,
                surface = palette.flaeche,
                onSurface = palette.textPrimaer,
                surfaceVariant = palette.flaecheErhoeht,
                onSurfaceVariant = palette.textGedaempft,
                surfaceTint = palette.primaer,
                inverseSurface = palette.textPrimaer,
                inverseOnSurface = palette.flaeche,
                error = fehler,
                onError = Color(0xFF2A0606),
                errorContainer = behaelterFehler,
                onErrorContainer = aufBehaelterFehler,
                // Unverändert palette.rahmen wie vor der Umstellung: In Schlicht darf sich keine
                // einzige Farbe verschieben, und `outline` färbt dort jeden Eingabefeldrand.
                outline = palette.rahmen,
                // `outlineVariant` ist die *leisere* der beiden Rollen — Trennlinien und
                // Chip-Ränder ziehen daraus. Vorher lag hier der hellere, also auffälligere
                // Ton; im Dunkeln heißt leiser, näher an die Fläche zu rücken.
                outlineVariant = palette.rahmen.dunkler(0.22f),
                scrim = Color.Black,
                // Die fünf Container-Stufen im Dunkeln: von tiefer als der Hintergrund bis über
                // die erhöhte Fläche. Dialoge landen auf `surfaceContainerHigh`.
                surfaceBright = flaecheHoch.heller(0.14f),
                surfaceDim = palette.hintergrund.dunkler(0.10f),
                surfaceContainerLowest = palette.hintergrund.dunkler(0.28f),
                surfaceContainerLow = palette.hintergrund,
                surfaceContainer = palette.flaeche,
                surfaceContainerHigh = flaecheHoch,
                surfaceContainerHighest = flaecheHoch.heller(0.10f),
            )
        } else {
            lightColorScheme(
                primary = palette.primaer,
                onPrimary = palette.aufPrimaer,
                primaryContainer = behaelterPrimaer,
                onPrimaryContainer = aufBehaelterPrimaer,
                inversePrimary = palette.primaer.heller(0.45f),
                secondary = palette.primaerGedaempft,
                onSecondary = palette.aufPrimaer,
                secondaryContainer = behaelterZweit,
                onSecondaryContainer = aufBehaelterZweit,
                tertiary = palette.akzentWarm,
                onTertiary = Color.White,
                tertiaryContainer = behaelterDritt,
                onTertiaryContainer = aufBehaelterDritt,
                background = palette.hintergrund,
                onBackground = palette.textPrimaer,
                surface = palette.flaeche,
                onSurface = palette.textPrimaer,
                surfaceVariant = palette.flaecheErhoeht,
                onSurfaceVariant = palette.textGedaempft,
                surfaceTint = palette.primaer,
                inverseSurface = palette.textPrimaer,
                inverseOnSurface = palette.flaeche,
                error = fehler,
                onError = Color.White,
                errorContainer = behaelterFehler,
                onErrorContainer = aufBehaelterFehler,
                outline = palette.rahmen,
                outlineVariant = palette.rahmen.heller(0.24f),
                scrim = Color.Black,
                // Im Hellen ist die weiße Karte die hellste Stufe; der Seitenhintergrund liegt darunter.
                surfaceBright = palette.flaeche,
                surfaceDim = palette.hintergrund.dunkler(0.08f),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = palette.flaeche,
                surfaceContainer = palette.hintergrund,
                surfaceContainerHigh = flaecheHoch,
                surfaceContainerHighest = flaecheHoch.dunkler(0.05f),
            )
        }
    }
    val schrift = remember(schriftSkalierung, titelSchrift) { typografie(schriftSkalierung, titelSchrift ?: IdeenSchriftBetont) }

    val tokens = de.frank.wecker.design.LocalDesignTokens.current
    val formen = remember(tokens) { formenFuer(tokens.chipRadius, tokens.karteRadius) }

    CompositionLocalProvider(
        LocalGold provides palette,
        LocalSemantisch provides semantischeFarben(ziel.istDunkel),
        LocalBewegungReduziert provides reduziert,
        LocalContentColor provides palette.textPrimaer,
    ) {
        MaterialTheme(
            colorScheme = schema,
            typography = schrift,
            shapes = formen,
            content = content,
        )
    }
}

/**
 * Schalterfarben für beide Modi: Der ausgeschaltete Zustand bleibt auf der Karte
 * klar erkennbar — deutlich abgesetzte Schiene, kräftiger Rand, hoher Daumen-Kontrast.
 */
@Composable
fun SchalterFarben(): SwitchColors {
    val gold = LocalGold.current
    return SwitchColors(
        checkedThumbColor = gold.aufPrimaer,
        checkedTrackColor = gold.primaer,
        checkedBorderColor = gold.primaerGedaempft,
        checkedIconColor = gold.aufPrimaer,
        uncheckedThumbColor = gold.textPrimaer,
        // Schlicht behält die abgestimmten Brauntöne; andere Designs nehmen ihre eigene Palette,
        // damit die Schiene nicht warm aus einer kühlen Oberfläche sticht.
        uncheckedTrackColor = if (de.frank.wecker.design.LocalDesignTokens.current.plastisch)
            (if (gold.istDunkel) Color(0xFF3B352B) else Color(0xFFE8E0CE)) else gold.flaecheErhoeht,
        uncheckedBorderColor = if (de.frank.wecker.design.LocalDesignTokens.current.plastisch)
            (if (gold.istDunkel) Color(0xFF8F8168) else gold.primaerGedaempft) else gold.rahmen,
        uncheckedIconColor = gold.textPrimaer,
        disabledCheckedThumbColor = gold.textGedaempft,
        disabledCheckedTrackColor = gold.flaecheErhoeht,
        disabledCheckedBorderColor = gold.rahmen,
        disabledCheckedIconColor = gold.textGedaempft,
        disabledUncheckedThumbColor = gold.textGedaempft,
        disabledUncheckedTrackColor = gold.flaecheErhoeht,
        disabledUncheckedBorderColor = gold.rahmen,
        disabledUncheckedIconColor = gold.textGedaempft,
    )
}

/**
 * Die Eckenformen des gewählten Designs als Material-Formenstaffel. Vorher zog jede
 * Material-Komponente ihre eigene Rundung aus der Grundvorlage — ein 28 dp runder Dialog stand
 * dann im kantigen Orbit, ein 4 dp kantiges Eingabefeld im weich gerundeten Traumraum.
 *
 * Die Pillenform (999 dp) gilt bewusst nur für kleine Bedienelemente und wird für Eingabefelder
 * und Menüs gedeckelt: Ein mehrzeiliges Textfeld als Kapsel wäre weder schön noch lesbar.
 */
internal fun formenFuer(chipRadius: Dp, karteRadius: Dp): Shapes {
    val klein = if (chipRadius > 16.dp) 12.dp else chipRadius
    val mittelklein = if (chipRadius > 16.dp) 16.dp else chipRadius
    return Shapes(
        extraSmall = RoundedCornerShape(klein),
        small = RoundedCornerShape(mittelklein),
        medium = RoundedCornerShape(karteRadius),
        large = RoundedCornerShape(karteRadius),
        extraLarge = RoundedCornerShape(karteRadius),
    )
}
