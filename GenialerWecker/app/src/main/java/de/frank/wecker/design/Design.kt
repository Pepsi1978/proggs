package de.frank.wecker.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Das gewählte Erscheinungsbild. Es ist unabhängig von Hell/Dunkel und von der Ausrichtung:
 * jedes Design hat beide Modi. [SCHLICHT] ist die Vorgabe und bildet die bisherige Gold-/Glasoptik
 * unverändert ab — Bestandsnutzer sehen ohne eigene Wahl genau das, was sie kennen.
 */
enum class Design(val id: String, val anzeige: String, val beschreibung: String) {
    SCHLICHT("schlicht", "Schlicht", "Das gewohnte Gold auf Glas."),
    MORGENRUHE("morgenruhe", "Morgenruhe", "Ruhiger Tagesablauf an einer Achse, Leinen und Tinte."),
    TRAUMRAUM("traumraum", "Traumraum", "Weiche Kuppel und runde Flächen, warmes Schwarz und Glut-Orange."),
    ORBIT("orbit", "Orbit", "Instrumententafel mit Ring und festen Zahlen, Eisblau und Limette.");

    companion object {
        fun von(id: String?): Design = entries.firstOrNull { it.id == id } ?: SCHLICHT
    }
}

/**
 * Form- und Schriftmerkmale eines Designs. Farben liegen weiterhin in der GoldPalette, damit jeder
 * vorhandene Baustein ohne Änderung mitzieht; hier stehen nur die Merkmale, die es dort nicht gibt.
 */
@Immutable
data class DesignTokens(
    val design: Design,
    /** Eckenradius der großen Flächen: von fast kantig (Orbit) bis stark gerundet (Traumraum). */
    val karteRadius: Dp,
    /** Eckenradius kleiner Bedienelemente wie Chips und stiller Knöpfe. */
    val chipRadius: Dp,
    /** Eckenradius der Alarmtasten. */
    val tasteRadius: Dp,
    /**
     * Eckenradius des Hauptknopfes und des stillen Knopfes.
     *
     * Bewusst **getrennt** von [plastisch]: Vorher entschied dieses eine Merkmal zugleich über
     * die Materialtiefe *und* über die Knopfform, und zwar über fest eingetragene 16 bzw. 12 dp
     * in `Knopf3D.kt`. Wer den drei flachen Designs Tiefe geben wollte, hätte ihnen damit
     * automatisch auch Schlichts Rundungen verpasst und ihre Identität eingeebnet.
     */
    val knopfRadius: Dp,
    /**
     * Ob Flächen den plastischen Gold-Look mit Schein und Verlauf tragen.
     *
     * Das betrifft ab jetzt nur noch **Material**, nicht mehr die Form — die kommt aus
     * [knopfRadius], [chipRadius] und [karteRadius].
     */
    val plastisch: Boolean,
)

/**
 * Die Tiefenebenen der App. Sie sind die gemeinsame Grammatik: Jede Fläche sagt, **wo** sie in
 * der Tiefe liegt, und das Material des Designs entscheidet, **wie** das aussieht.
 *
 * Entscheidend ist der Sprung zwischen [VERTIEFT] und [KNOPF]. Erst wenn Eingabefelder,
 * Reglerbahnen und Chips sichtbar unter der Kartenoberfläche liegen, lesen sich Knöpfe von
 * selbst als erhaben — das ist der eigentliche räumliche Eindruck, nicht der Schatten allein.
 */
enum class Ebene { HINTERGRUND, VERTIEFT, ABSCHNITT, KARTE, HERO, DIALOG, KNOPF }

/**
 * Die Materialwerte eines Designs. Alle Überlagerungen sind reines Weiß- oder Schwarz-Alpha
 * beziehungsweise eine ausdrücklich genannte Designfarbe — keine davon verschiebt einen
 * Palettenton. Deshalb bleiben Schlichts Goldwerte über das ganze Materialsystem hinweg
 * unverändert, obwohl sich die Optik deutlich ändert.
 */
@Immutable
data class Material(
    /** Weiß-Alpha oben und Schwarz-Alpha unten im senkrechten Tiefenverlauf. */
    val tiefenOben: Float,
    val tiefenUnten: Float,
    /** Der gerichtete Reflex — das, was Glas von bloßer Farbe unterscheidet. */
    val reflexFarbe: Color,
    val reflexAlpha: Float,
    val reflexWinkelGrad: Float,
    val reflexLaenge: Float,
    /** Die Lichtkante oben; die Schattenkante unten ist immer Schwarz. */
    val kanteLichtFarbe: Color,
    val kanteLichtAlpha: Float,
    val kanteSchattenAlpha: Float,
    /**
     * Ein deckender Umriss unter der Lichtkante, oder `null`.
     *
     * Die Lichtkante zeichnet nur oben und unten; die Seiten bleiben durchsichtig. Bei Designs,
     * deren Flächen sich ohnehin nur schwach vom Untergrund abheben — Morgenruhe auf Leinen,
     * Orbit auf der Tafel — verlöre die Karte dadurch seitlich ihre Begrenzung. Dort liegt
     * deshalb der gewohnte Rahmenstrich darunter. Schlicht trug nie einen und bekommt `null`.
     */
    val kanteGrund: Color?,
    /** Wie tief vertiefte Flächen und gedrückte Knöpfe einsinken. */
    val innenSchattenAlpha: Float,
    /** Feine Körnung; nur auf großen Flächen, nie in Listen. 0 bedeutet keine. */
    val koernungAlpha: Float,
    /** Farbe des Außenschattens; null bedeutet Schwarz. */
    val schattenFarbe: Color?,
    /** Abdunklung der Ecken auf dem Seitenhintergrund. */
    val vignetteAlpha: Float,
    /** Die eingefräste Innenlinie — allein Orbits Handschrift. */
    val nut: Boolean,
)

/**
 * Die Werte je Design und Modus. Die Unterschiede hier sind die vier Handschriften:
 * Schlicht geschliffenes Glas mit diagonalem Schliff und goldener Kante, Morgenruhe mattes
 * Opalglas mit breitem weichem Licht von oben, Traumraum warmes Rauchglas mit flachem
 * orangefarbenem Streiflicht, Orbit kühles Instrumentenglas mit schmalem hartem Band und Nut.
 */
fun materialFuer(design: Design, dunkel: Boolean, primaer: Color, gedaempft: Color, text: Color, rahmen: Color): Material = when (design) {
    Design.SCHLICHT -> Material(
        tiefenOben = if (dunkel) 0.1f else 0.05f,
        tiefenUnten = if (dunkel) 0.18f else 0.08f,
        reflexFarbe = Color.White,
        reflexAlpha = if (dunkel) 0.12f else 0.3f,
        reflexWinkelGrad = 40f, reflexLaenge = 0.55f,
        kanteLichtFarbe = primaer,
        kanteLichtAlpha = if (dunkel) 0.35f else 0.45f,
        kanteSchattenAlpha = if (dunkel) 0.45f else 0.3f,
        // Geschlossener goldener Umriss: Vorher waren die Seiten fast durchsichtig und die Blasen
        // wirkten links und rechts offen.
        kanteGrund = primaer.copy(alpha = if (dunkel) 0.38f else 0.42f),
        innenSchattenAlpha = if (dunkel) 0.4f else 0.24f,
        koernungAlpha = 0.04f,
        schattenFarbe = primaer,
        vignetteAlpha = if (dunkel) 0.22f else 0.06f,
        nut = false,
    )
    Design.MORGENRUHE -> Material(
        tiefenOben = if (dunkel) 0.12f else 0.06f,
        tiefenUnten = if (dunkel) 0.2f else 0.1f,
        reflexFarbe = Color.White,
        reflexAlpha = if (dunkel) 0.12f else 0.22f,
        reflexWinkelGrad = 90f, reflexLaenge = 0.70f,
        kanteLichtFarbe = Color.White,
        kanteLichtAlpha = if (dunkel) 0.28f else 0.35f,
        kanteSchattenAlpha = if (dunkel) 0.5f else 0.28f,
        kanteGrund = rahmen,
        innenSchattenAlpha = if (dunkel) 0.45f else 0.24f,
        koernungAlpha = 0.05f,
        schattenFarbe = gedaempft,
        vignetteAlpha = if (dunkel) 0.12f else 0.04f,
        nut = false,
    )
    Design.TRAUMRAUM -> Material(
        tiefenOben = if (dunkel) 0.1f else 0.06f,
        tiefenUnten = if (dunkel) 0.2f else 0.1f,
        reflexFarbe = if (dunkel) primaer else Color.White,
        reflexAlpha = if (dunkel) 0.14f else 0.2f,
        reflexWinkelGrad = 30f, reflexLaenge = 0.50f,
        kanteLichtFarbe = primaer,
        kanteLichtAlpha = if (dunkel) 0.32f else 0.22f,
        kanteSchattenAlpha = if (dunkel) 0.45f else 0.3f,
        kanteGrund = rahmen,
        innenSchattenAlpha = if (dunkel) 0.45f else 0.24f,
        koernungAlpha = 0.03f,
        schattenFarbe = primaer,
        vignetteAlpha = if (dunkel) 0.28f else 0.06f,
        nut = false,
    )
    Design.ORBIT -> Material(
        tiefenOben = if (dunkel) 0.1f else 0.05f,
        tiefenUnten = if (dunkel) 0.2f else 0.1f,
        reflexFarbe = Color.White,
        reflexAlpha = if (dunkel) 0.16f else 0.25f,
        // Schmales hartes Band statt weicher Fläche: gebürstetes Metall, kein Glas.
        reflexWinkelGrad = 90f, reflexLaenge = 0.14f,
        kanteLichtFarbe = text,
        kanteLichtAlpha = if (dunkel) 0.3f else 0.25f,
        kanteSchattenAlpha = if (dunkel) 0.5f else 0.3f,
        kanteGrund = rahmen,
        innenSchattenAlpha = if (dunkel) 0.45f else 0.26f,
        koernungAlpha = 0f,
        schattenFarbe = null,
        vignetteAlpha = if (dunkel) 0.18f else 0.05f,
        nut = true,
    )
}

val LocalMaterial = staticCompositionLocalOf {
    materialFuer(Design.SCHLICHT, dunkel = false, primaer = Color(0xFF8B6914),
        gedaempft = Color(0xFFA9812A), text = Color(0xFF1B1710), rahmen = Color(0xFFE6DFCF))
}

val LocalDesignTokens = staticCompositionLocalOf { tokensFuer(Design.SCHLICHT) }

/**
 * Die zeichnenden Teile eines Designs. Der geteilte Code berechnet Zustand und Rückrufe genau
 * einmal und übergibt sie hier; die Gestalten sind reine Darsteller ohne Zugriff auf das ViewModel.
 * So bleiben alle Funktionen, Eingaben und Abläufe in jedem Design vollständig erhalten.
 */
interface WeckerGestalt {
    /** Hintergrund hinter allen Bildschirmen. */
    @Composable fun Hintergrund(modifier: Modifier)

    /** Umschließende Fläche für Karten und Abschnitte — trägt Editor und Einstellungen. */
    @Composable fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit)

    /** Das Motiv des Designs neben dem Text; Schlicht zeigt keines. */
    @Composable fun Motiv(modifier: Modifier)

    /** Ob die Weckerliste ihre Kopfkarte mit dem Restzeitring zeigt (Orbit ersetzt sie durch sein Modul). */
    val zeigtRestzeitRing: Boolean

    /** Rahmenfarbe für Trennlinien innerhalb der Flächen. */
    @Composable fun trennfarbe(): Color
}

val LocalGestalt = staticCompositionLocalOf<WeckerGestalt> { SchlichtGestalt }

fun gestaltFuer(design: Design): WeckerGestalt = when (design) {
    Design.SCHLICHT -> SchlichtGestalt
    Design.MORGENRUHE -> MorgenruheGestalt
    Design.TRAUMRAUM -> TraumraumGestalt
    Design.ORBIT -> OrbitGestalt
}

fun tokensFuer(design: Design): DesignTokens = when (design) {
    // Genau die bisherigen Werte: 20 dp Karten, 12 dp Knöpfe, plastisch, keine eigene Zahlenschrift.
    Design.SCHLICHT -> DesignTokens(design, 20.dp, 12.dp, 16.dp, knopfRadius = 16.dp, plastisch = true)
    // Die Pillenform der Knöpfe ist Morgenruhes und Traumraums Handschrift und bleibt, auch
    // wenn beide jetzt echtes Material bekommen.
    Design.MORGENRUHE -> DesignTokens(design, 22.dp, 999.dp, 22.dp, knopfRadius = 999.dp, plastisch = false)
    Design.TRAUMRAUM -> DesignTokens(design, 32.dp, 999.dp, 999.dp, knopfRadius = 999.dp, plastisch = false)
    Design.ORBIT -> DesignTokens(design, 6.dp, 4.dp, 5.dp, knopfRadius = 5.dp, plastisch = false)
}
