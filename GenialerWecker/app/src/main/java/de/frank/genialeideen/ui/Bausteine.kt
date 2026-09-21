package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.LocalSemantisch
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.Semantisch
import de.frank.genialeideen.ui.theme.kippKarte
import de.frank.genialeideen.ui.theme.lichtKante
import de.frank.genialeideen.ui.theme.milchglas
import de.frank.genialeideen.ui.theme.mischeMit
import de.frank.genialeideen.ui.theme.tiefenSchatten
import de.frank.genialeideen.ui.theme.vignette
import de.frank.genialeideen.ui.theme.wackelnBeiFehler
import de.frank.genialeideen.ui.theme.wanderndesGlanzlicht
import de.frank.wecker.design.Ebene
import de.frank.wecker.design.LocalMaterial
import de.frank.wecker.design.MaterialFlaeche
import kotlin.math.cos
import kotlin.math.sin

/**
 * Die Kopfleiste aus Baustein C: links der Theme-Knopf, rechts daneben das Zahnrad.
 * Die Reihenfolge ist fest. Der Theme-Knopf kennt genau zwei Zustände — hell und dunkel.
 * Die Leiste liegt auf Milchglas (N.4).
 */
@Composable
fun IdeenKopfleiste(
    titel: String,
    themeWahl: String,
    modifier: Modifier = Modifier,
    aufThemeTipp: (() -> Unit)? = null,
    aufSuche: (() -> Unit)? = null,
    /** Null lässt das Zahnrad weg — in den Einstellungen selbst hat es nichts zu suchen. */
    aufEinstellungen: (() -> Unit)? = null,
    voran: (@Composable () -> Unit)? = null,
    aufDesignTipp: (() -> Unit)? = null,
) {
    val gold = LocalGold.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Geprüft gegen die Schichtreihenfolge des Materialsystems: `milchglas` legt
            // Grundfläche, dann den gerichteten Reflex, dann die Körnung und erst zuletzt die
            // Kante. Alle drei zeichnen mit `onDrawBehind`, also **hinter** Titel und Symbolen —
            // die Körnung nimmt der Schrift seit der Korrektur in `Effekte.kt` keine Schärfe
            // mehr. Die Kante bleibt hier bewusst aus (`kante = false`), weil die Leiste bis an
            // den Bildschirmrand läuft und sonst ein heller Strich oben und seitlich stünde.
            .milchglas(gold.flaeche, RoundedCornerShape(0.dp), deckung = 0.55f, kante = false)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        voran?.invoke()
        // Der Titel misst sich gegen den Platz, der nach den Knöpfen übrig bleibt. Das Glanzlicht
        // liegt jetzt auf dem Titelfeld statt auf dem Text — geometrisch dasselbe (beide füllen
        // die gewichtete Spalte), aber es trägt auch eine zweite Zeile mit.
        KopfTitel(
            titel = titel,
            stil = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            farbe = gold.primaer,
            modifier = Modifier.weight(1f).wanderndesGlanzlicht(),
        )
        if (aufSuche != null) {
            KopfKnopf(beschreibung = "Alle Ideen durchsuchen", aufTipp = aufSuche) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (aufDesignTipp != null) {
            KopfKnopf(beschreibung = "Design wechseln", aufTipp = aufDesignTipp) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = gold.primaer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        if (aufThemeTipp != null) {
            KopfKnopf(
                beschreibung = when (themeWahl) {
                    "dark" -> "Dunkler Modus, tippen für automatisch"
                    "system" -> "Automatisch wie das Handy, tippen für hell"
                    else -> "Heller Modus, tippen für dunkel"
                },
                aufTipp = aufThemeTipp,
            ) {
                if (themeWahl == "system") de.frank.wecker.design.ModusAutomatikSymbol(gold.primaer)
                else Icon(
                    imageVector = if (themeWahl == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (aufEinstellungen != null) {
            KopfKnopf(beschreibung = "Einstellungen öffnen", aufTipp = aufEinstellungen) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Die Überschrift einer Kopfleiste — die einzige Stelle, an der alle vier Designs ihren Titel
 * setzen. Sie richtet sich nach dem Platz, der nach den Aktionsknöpfen übrig bleibt, statt ihn
 * stillschweigend abzuschneiden.
 *
 * **Die Engstelle, vorgerechnet für 320 dp Bildschirmbreite im Editor (Schlicht):**
 * 32 dp Außenabstand + 48 dp Zurück + 8 dp Abstand + 40 dp Hell/Dunkel + 8 dp + 40 dp Zahnrad
 * = 176 dp gehen an Rand und Knöpfe, für den Titel bleiben **144 dp**. „Wecker bearbeiten“
 * braucht bei 22 sp rund 195 dp — es passte dort noch nie. Bisher fehlte das Ende wortlos, ohne
 * Auslassungszeichen. Zum Vergleich: 360 dp → 184, 393 dp → 217, 412 dp → 236 dp; Orbit hat
 * wegen 14 dp Rand 4 dp mehr (148), Traumraum wegen 18 dp 4 dp weniger (140); auf der
 * Weckerliste fehlt der Zurück-Knopf, dort sind es 56 dp mehr.
 *
 * **Die Leiter, in dieser Reihenfolge:**
 *  1. Wunschgröße, eine Zeile. Passt der Titel, bleibt alles exakt wie bisher — „Genialer
 *     Wecker" und „Einstellungen“ sehen unverändert aus.
 *  2. Bis auf 85 % verkleinern, wenn das die eine Zeile rettet. Ein kaum sichtbarer
 *     Größenunterschied stört weniger als ein Umbruch.
 *  3. Umbrechen auf [maxZeilen] Zeilen in voller Wunschgröße. Hier landet „Wecker bearbeiten“
 *     bei 320 dp: „Wecker“ (≈70 dp) und „bearbeiten“ (≈115 dp) passen beide in 144 dp, die
 *     Schrift bleibt bei 22 sp. Die Leiste wächst dafür um eine Zeilenhöhe.
 *  4. [maxZeilen] Zeilen und verkleinern bis [kleinste]. Das ist die Notbremse für große
 *     Systemschrift: Bei Schriftfaktor 1,5 braucht „bearbeiten“ bei 22 sp schon 172 dp, die
 *     Suche landet bei rund 18 sp — immer noch deutlich größer als die Normalschrift.
 *
 * Reicht auch die kleinste Stufe nicht, kürzt Compose mit **sichtbarem** Auslassungszeichen —
 * `overflow = Ellipsis` liegt immer an, nicht nur in der letzten Stufe. Mehr als [maxZeilen]
 * Zeilen gibt es nie; damit ist die Höhe der Leiste auch bei doppelter Systemschrift auf rund
 * zwei Zeilen gedeckelt und frisst nie die halbe Seite.
 *
 * Die Knöpfe liegen außerhalb dieser Messung und behalten ihre feste Größe: Was hier schrumpft,
 * ist immer der Titel, nie die Bedienbarkeit.
 *
 * @param stil der vollständige Stil des Designs — er muss die Schriftfamilie enthalten, denn
 *   mit genau diesem Stil wird auch gemessen (Orbit misst also seine feste Schrift).
 * @param modifier gehört auf das Titelfeld, üblicherweise `Modifier.weight(1f)`.
 * @param kleinste die absolute Untergrenze in sp. Sie ist ein sp-Wert und wächst deshalb mit der
 *   Systemschrift mit — verkleinert wird nur gegen die Breite, nie gegen die Einstellung.
 */
@Composable
fun KopfTitel(
    titel: String,
    stil: TextStyle,
    farbe: Color,
    modifier: Modifier = Modifier,
    maxZeilen: Int = 2,
    kleinste: TextUnit = 15.sp,
) {
    BoxWithConstraints(modifier) {
        // Die tatsächlich zugeteilte Breite in Pixeln, nicht der Umweg über dp: Row verteilt
        // erst die festen Knöpfe, der Rest landet hier. dp → px → dp kann um ein Pixel danebenliegen,
        // und dann sagt die Messung „passt“, während die Anzeige ein Zeichen frisst.
        val stilJetzt = passenderTitelStil(titel, stil, constraints.maxWidth, maxZeilen, kleinste)
        Text(
            text = titel,
            modifier = Modifier.semantics { heading() },
            style = stilJetzt,
            color = farbe,
            maxLines = maxZeilen,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Sucht den Stil, mit dem [titel] in [breitePx] Pixel passt — nach dem Verfahren, das der
 * Weckbildschirm für seine große Uhr benutzt: mit `rememberTextMeasurer` wirklich messen statt
 * Zeichen zu zählen. Gemessen wird mit demselben Stil, mit dem danach gezeichnet wird, sonst
 * stimmt das Ergebnis für kursive, gesperrte oder feste Schriften nicht.
 */
@Composable
private fun passenderTitelStil(
    titel: String,
    stil: TextStyle,
    breitePx: Int,
    maxZeilen: Int,
    kleinste: TextUnit,
): TextStyle {
    val messer = rememberTextMeasurer()
    val dichte = LocalDensity.current
    return remember(messer, titel, stil, breitePx, maxZeilen, kleinste, dichte.density, dichte.fontScale) {
        // Ohne bekannte Breite (erster Durchlauf, oder die Knöpfe belegen alles) bleibt es bei
        // der Wunschgröße; gekürzt wird dann sichtbar durch die Ellipsis.
        if (breitePx <= 0 || breitePx == Constraints.Infinity) return@remember stil
        // Nur eine in sp angegebene Größe lässt sich gegen die Breite herunterrechnen. Alle vier
        // Designs geben eine an (titleLarge 22 sp, Orbit labelLarge 14 sp); käme je ein Stil ohne
        // Größe oder in em herein, bliebe er unangetastet — die Ellipsis greift trotzdem.
        if (!stil.fontSize.isSp) return@remember stil
        val basis = stil.fontSize.value
        if (basis <= 0f) return@remember stil
        val untergrenze = kleinste.value.coerceAtMost(basis)

        fun stilBei(sp: Float): TextStyle = if (sp >= basis) {
            stil
        } else {
            stil.copy(
                fontSize = sp.sp,
                // Die Zeilenhöhe schrumpft mit. Material3 setzt sie fest (titleLarge: 28 sp);
                // zwei kleine Zeilen stünden sonst mit einem Loch dazwischen.
                lineHeight = if (stil.lineHeight.isSp) (stil.lineHeight.value * sp / basis).sp else stil.lineHeight,
            )
        }

        fun passt(sp: Float, zeilen: Int): Boolean = !messer.measure(
            text = titel,
            style = stilBei(sp),
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            maxLines = zeilen,
            constraints = Constraints(maxWidth = breitePx),
        ).hasVisualOverflow

        // Halbierung zwischen einer passenden Untergrenze und einer zu großen Obergrenze.
        // Sechs Schritte treffen den Wert auf unter 0,2 sp genau — feiner sieht niemand.
        fun groessteGroesse(unten: Float, oben: Float, zeilen: Int): Float {
            var passend = unten
            var zuGross = oben
            repeat(6) {
                val mitte = (passend + zuGross) / 2f
                if (passt(mitte, zeilen)) passend = mitte else zuGross = mitte
            }
            return passend
        }

        // Stufe 1: Wunschgröße, eine Zeile — der Normalfall, hier ändert sich nichts.
        if (passt(basis, 1)) return@remember stil
        // Stufe 2: leicht verkleinern rettet manchmal die eine Zeile.
        val schonend = (basis * 0.85f).coerceAtLeast(untergrenze)
        if (schonend < basis && passt(schonend, 1)) {
            return@remember stilBei(groessteGroesse(schonend, basis, 1))
        }
        // Stufe 3: umbrechen, Schriftgröße bleibt.
        if (maxZeilen > 1 && passt(basis, maxZeilen)) return@remember stil
        // Stufe 4: umbrechen UND verkleinern.
        if (maxZeilen > 1 && passt(untergrenze, maxZeilen)) {
            return@remember stilBei(groessteGroesse(untergrenze, basis, maxZeilen))
        }
        // Selbst die kleinste Stufe reicht nicht: dann kürzt die Ellipsis — sichtbar.
        stilBei(untergrenze)
    }
}

@Composable
fun KopfKnopf(
    beschreibung: String,
    aufTipp: () -> Unit,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    // Die Eckenform kommt aus dem Design, nicht aus einem festen Wert: In Morgenruhe und
    // Traumraum ist der Kopfknopf dadurch rund wie jeder andere kleine Knopf dort, in Orbit
    // kantig. Die Pillenform wird gedeckelt, damit aus dem Quadrat kein Kreis wird.
    val tokens = de.frank.wecker.design.LocalDesignTokens.current
    Knopf3D(
        aufTipp = aufTipp,
        modifier = Modifier.size(40.dp),
        grundfarbe = gold.primaer.copy(alpha = 0.16f).compositeUeber(gold.flaeche),
        form = RoundedCornerShape(if (tokens.chipRadius > 20.dp) 20.dp else tokens.chipRadius),
        hoehe = Hoehe.karte,
        innenAbstandWaagerecht = 0.dp,
        innenAbstandSenkrecht = 0.dp,
        beschreibung = beschreibung,
        inhalt = inhalt,
    )
}

/** Legt eine halbtransparente Farbe über eine deckende — ergibt eine deckende Mischfarbe. */
private fun Color.compositeUeber(unten: Color): Color =
    unten.mischeMit(copy(alpha = 1f), alpha)

/** Jeder Knopf sinkt beim Drücken kurz ein und federt aus (Baustein N.2). */
@Composable
fun Modifier.druckEffekt(aufTipp: () -> Unit): Modifier {
    val reduziert = LocalBewegungReduziert.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()
    val faktor by animateFloatAsState(
        targetValue = if (gedrueckt && !reduziert) 0.94f else 1f,
        animationSpec = Motion.mikro(reduziert),
        label = "druck",
    )
    return this
        .graphicsLayer { scaleX = faktor; scaleY = faktor }
        .clickable(interactionSource = quelle, indication = null, onClick = aufTipp)
}

/** Goldener Schein hinter aktiven Elementen — mehrschichtig nach N.4. */
fun Modifier.goldSchein(farbe: Color, hoehe: Dp = 12.dp, radius: Dp = 20.dp): Modifier =
    this.tiefenSchatten(farbe, hoehe, RoundedCornerShape(radius))

/**
 * Karte mit Verlauf, Lichtkante und mehrschichtigem Schatten (N.3, N.4).
 * Eine plane Farbfüllung ohne alles gibt es in dieser App nicht.
 *
 * Sie ist seit dem Materialumbau nur noch der vertraute Name für [MaterialFlaeche] auf der
 * Karten- beziehungsweise Hero-Ebene. Das ist Absicht: Schlichts Karten kommen über
 * `Flaeche(erhoeht)` herein, der Schlicht-Dialog dagegen ruft [GoldKarte] unmittelbar auf —
 * über eine gemeinsame Umsetzung können beide nicht mehr auseinanderlaufen.
 *
 * **Was sich dabei geändert hat, und warum kein Goldwert wandert:**
 *  - Der Körperverlauf rechnete `heller(a)`/`dunkler(a)` auf die Flächenfarbe. Das ist
 *    rechnerisch identisch mit Weiß- beziehungsweise Schwarz-Alpha über derselben Farbe
 *    (`c + (1−c)·a` und `c·(1−a)`) — genau das tut jetzt `tiefenVerlauf` mit Schlichts Werten.
 *  - Der warme Schein lief über `radius = 700f`, einen festen Pixelwert: auf einer kleinen Karte
 *    ein Schein über die ganze Fläche, auf einer breiten Hero-Karte ein Fleck in der Ecke. An
 *    seiner Stelle steht der gerichtete Reflex, dessen Länge ein Anteil der Fläche ist und der
 *    deshalb auf jeder Kartengröße gleich wirkt.
 *  - Die Kante ist von Weiß auf Schlichts Primärgold gewechselt — die „goldene Kante" seiner
 *    Handschrift. Ein Palettenton verschiebt sich auch dadurch nicht: Sie liegt als Farbe mit
 *    Alpha über der unveränderten Grundfläche.
 */
@Composable
fun GoldKarte(
    modifier: Modifier = Modifier,
    erhoeht: Boolean = false,
    kippbar: Boolean = false,
    inhalt: @Composable () -> Unit,
) = MaterialFlaeche(
    modifier = modifier.then(if (kippbar) Modifier.kippKarte() else Modifier),
    ebene = if (erhoeht) Ebene.HERO else Ebene.KARTE,
    inhalt = inhalt,
)

/** Ein Element blendet gestaffelt auf, gleitet hoch und schwingt leicht ein (N.7). */
@Composable
fun GestaffeltEinblenden(
    sichtbar: Boolean,
    index: Int,
    inhalt: @Composable () -> Unit,
) {
    val reduziert = LocalBewegungReduziert.current
    AnimatedVisibility(
        visible = sichtbar,
        enter = if (reduziert) {
            fadeIn(tween(0))
        } else {
            fadeIn(tween(Motion.ZUSTAND_MS, delayMillis = index * Motion.STAFFEL_MS)) +
                slideInVertically(
                    animationSpec = tween(Motion.ZUSTAND_MS, delayMillis = index * Motion.STAFFEL_MS),
                    initialOffsetY = { Motion.STAFFEL_HUB_DP * 3 },
                ) +
                scaleIn(
                    animationSpec = tween(Motion.ZUSTAND_MS, delayMillis = index * Motion.STAFFEL_MS),
                    initialScale = 0.96f,
                )
        },
        exit = fadeOut(tween(Motion.MIKRO_MS)),
    ) { inhalt() }
}

/** Platzhalter-Gerüst mit goldenem Laufband, solange geladen wird (Baustein L, N.7). */
@Composable
fun SchimmerGeruest(zeilen: Int = 3, modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "schimmer")
    val versatz by uebergang.animateFloat(
        initialValue = 0f,
        targetValue = if (reduziert) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(Motion.SCHIMMER_MS), RepeatMode.Restart),
        label = "versatz",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(zeilen) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .tiefenSchatten(gold.primaer, Hoehe.karte, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .drawBehind {
                        drawRect(Brush.linearGradient(
                            colors = listOf(
                                gold.flaeche,
                                gold.flaeche.mischeMit(gold.primaer, 0.14f),
                                gold.flaeche,
                            ),
                            start = Offset(versatz * 1200f - 400f + index * 40f, 0f),
                            end = Offset(versatz * 1200f + index * 40f, 300f),
                        ))
                    }
                    .border(1.dp, lichtKante(staerke = 0.20f), RoundedCornerShape(20.dp)),
            )
        }
    }
}

/**
 * Die bewegte Hintergrund-Ebene (N.3): zwei goldene Scheine, die langsam wandern.
 * Auf Leer- und Ladebildschirmen Pflicht — die App wirkt lebendig, auch wenn nichts passiert.
 *
 * Dazu die Vignette des Materials: Sie dunkelt die Ecken der Seite leicht ab und gibt ihr damit
 * eine Wölbung, auf der die Karten erst aufliegen. Ohne sie schwebt jede Karte über einer
 * gleichmäßigen Farbfläche, und genau das lässt eine Oberfläche flach wirken.
 */
@Composable
fun BewegterHintergrund(modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    // Bei reduzierter Bewegung läuft gar keine Animation: Eine Endlos-Animation mit
    // gleichem Start- und Zielwert zeichnet den Vollbild-Hintergrund trotzdem jedes Bild neu.
    val phaseState = if (reduziert) {
        null
    } else {
        val uebergang = rememberInfiniteTransition(label = "hintergrund")
        val wert = uebergang.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(Motion.HINTERGRUND_MS), RepeatMode.Restart),
            label = "phase",
        )
        wert
    }
    // Eigene Zeichenebene: Der wandernde Schein zieht so nur sich selbst neu, nicht die
    // Liste darüber. Die Vignette steht bewusst **vor** dieser Ebene: Sie liegt damit unter den
    // Scheinen und wird nicht bei jedem Bild der Endlos-Animation mitgerechnet.
    Canvas(
        modifier = modifier.fillMaxSize()
            .vignette(LocalMaterial.current.vignetteAlpha)
            .graphicsLayer(),
    ) {
        val phase = phaseState?.value ?: 0f
        val breite = size.width
        val hoehe = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gold.primaer.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(
                    breite * (0.30f + 0.18f * cos(phase)),
                    hoehe * (0.24f + 0.10f * sin(phase)),
                ),
                radius = breite * 0.75f,
            ),
            radius = breite * 0.75f,
            center = Offset(
                breite * (0.30f + 0.18f * cos(phase)),
                hoehe * (0.24f + 0.10f * sin(phase)),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gold.akzentWarm.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(
                    breite * (0.76f - 0.16f * sin(phase)),
                    hoehe * (0.78f + 0.09f * cos(phase)),
                ),
                radius = breite * 0.62f,
            ),
            radius = breite * 0.62f,
            center = Offset(
                breite * (0.76f - 0.16f * sin(phase)),
                hoehe * (0.78f + 0.09f * cos(phase)),
            ),
        )
    }
}

/**
 * Ein Leerzustand mit Symbol, einem Satz und dem Knopf, der ihn füllt (Baustein L).
 * Das Symbol atmet leise, damit auch der leere Bildschirm lebt (N.7).
 */
@Composable
fun Leerzustand(
    symbol: String,
    ueberschrift: String,
    satz: String,
    knopfText: String? = null,
    aufKnopf: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "leer")
    val atem by uebergang.animateFloat(
        initialValue = 1f,
        targetValue = if (reduziert) 1f else 1.06f,
        animationSpec = infiniteRepeatable(tween(Motion.ATEM_MS), RepeatMode.Reverse),
        label = "atemwert",
    )
    val dreh by uebergang.animateFloat(
        initialValue = -3f,
        targetValue = if (reduziert) -3f else 3f,
        animationSpec = infiniteRepeatable(tween(Motion.SCHWEBEN_MS), RepeatMode.Reverse),
        label = "drehwert",
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer { scaleX = atem; scaleY = atem }
                // Die Form folgt dem Design: im kantigen Orbit ist das Symbolfeld kein
                // Rundling, im weich gerundeten Traumraum keine Ecke.
                .clip(RoundedCornerShape(de.frank.wecker.design.LocalDesignTokens.current.karteRadius))
                .background(
                    Brush.radialGradient(
                        listOf(gold.primaer.copy(alpha = 0.26f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, fontSize = 44.sp, modifier = Modifier.graphicsLayer { rotationZ = dreh })
        }
        Spacer(Modifier.height(20.dp))
        Text(
            ueberschrift,
            style = MaterialTheme.typography.titleMedium,
            color = gold.textPrimaer,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            satz,
            style = MaterialTheme.typography.bodyMedium,
            color = gold.textGedaempft,
            textAlign = TextAlign.Center,
        )
        if (knopfText != null && aufKnopf != null) {
            Spacer(Modifier.height(20.dp))
            GoldKnopf(text = knopfText, aufTipp = aufKnopf, hauptKnopf = true)
        }
    }
}

/** Kurze Bestätigungen und echte Probleme als Streifen — kein Toast für Wichtiges. */
@Composable
fun MeldungsStreifen(
    meldung: Meldung,
    aufSchliessen: () -> Unit,
    aufEinstellungen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val farbe = if (meldung.istFehler) LocalSemantisch.current.fehler else LocalSemantisch.current.erfolg
    val form = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // Ein Fehler wackelt kurz — nie nur stummer roter Text (N.7).
            .wackelnBeiFehler(if (meldung.istFehler) meldung.text else null)
            .tiefenSchatten(farbe, Hoehe.schwebendeLeiste, form)
            .clip(form)
            .milchglas(gold.flaecheErhoeht, form, deckung = 0.92f)
            .border(1.dp, farbe.copy(alpha = 0.45f), form)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(farbe),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            meldung.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = gold.textPrimaer,
        )
        meldung.wiederholen?.let { wiederholen ->
            Spacer(Modifier.width(8.dp))
            StillerKnopf("Wiederholen", { wiederholen(); aufSchliessen() }, hervorgehoben = true)
        }
        if (meldung.zuEinstellungen) {
            Spacer(Modifier.width(8.dp))
            StillerKnopf("Einstellungen", { aufEinstellungen(); aufSchliessen() }, hervorgehoben = true)
        }
        Spacer(Modifier.width(8.dp))
        StillerKnopf("Weg", aufSchliessen)
    }
}

/** Auf dem Cover-Display des Fold ist alles einspaltig, aufgeklappt wird es zweispaltig. */
@Composable
fun istBreit(): Boolean = LocalConfiguration.current.screenWidthDp >= 600
