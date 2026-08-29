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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.Semantisch
import de.frank.genialeideen.ui.theme.dunkler
import de.frank.genialeideen.ui.theme.kippKarte
import de.frank.genialeideen.ui.theme.heller
import de.frank.genialeideen.ui.theme.lichtKante
import de.frank.genialeideen.ui.theme.milchglas
import de.frank.genialeideen.ui.theme.mischeMit
import de.frank.genialeideen.ui.theme.tiefenSchatten
import de.frank.genialeideen.ui.theme.wackelnBeiFehler
import de.frank.genialeideen.ui.theme.wanderndesGlanzlicht
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
    aufEinstellungen: () -> Unit,
    voran: (@Composable () -> Unit)? = null,
) {
    val gold = LocalGold.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .milchglas(gold.flaeche, RoundedCornerShape(0.dp), deckung = 0.55f, kante = false)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        voran?.invoke()
        Text(
            text = titel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                // Verlaufsschrift mit wanderndem Glanz (N.6, N.7).
                brush = wanderndesGlanzlicht(),
            ),
            maxLines = 1,
        )
        if (aufThemeTipp != null) {
            KopfKnopf(
                beschreibung = if (themeWahl == "dark") {
                    "Dunkler Modus, tippen für hell"
                } else {
                    "Heller Modus, tippen für dunkel"
                },
                aufTipp = aufThemeTipp,
            ) {
                Icon(
                    imageVector = if (themeWahl == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
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

@Composable
fun KopfKnopf(
    beschreibung: String,
    aufTipp: () -> Unit,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    Knopf3D(
        aufTipp = aufTipp,
        modifier = Modifier.size(40.dp),
        grundfarbe = gold.primaer.copy(alpha = 0.16f).compositeUeber(gold.flaeche),
        form = RoundedCornerShape(12.dp),
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
        .scale(faktor)
        .clickable(interactionSource = quelle, indication = null, onClick = aufTipp)
}

/** Goldener Schein hinter aktiven Elementen — mehrschichtig nach N.4. */
fun Modifier.goldSchein(farbe: Color, hoehe: Dp = 12.dp, radius: Dp = 20.dp): Modifier =
    this.tiefenSchatten(farbe, hoehe, RoundedCornerShape(radius))

/**
 * Karte mit Verlauf, Lichtkante und mehrschichtigem Schatten (N.3, N.4).
 * Eine plane Farbfüllung ohne alles gibt es in dieser App nicht.
 */
@Composable
fun GoldKarte(
    modifier: Modifier = Modifier,
    erhoeht: Boolean = false,
    kippbar: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    val form = RoundedCornerShape(20.dp)
    val flaeche = if (erhoeht) gold.flaecheErhoeht else gold.flaeche
    Box(
        modifier = modifier
            .then(if (kippbar) Modifier.kippKarte() else Modifier)
            .tiefenSchatten(
                farbe = gold.primaer,
                hoehe = if (erhoeht) Hoehe.karteErhoeht else Hoehe.karte,
                form = form,
            )
            .clip(form)
            .background(
                Brush.verticalGradient(
                    listOf(
                        flaeche.heller(if (gold.istDunkel) 0.06f else 0.02f),
                        flaeche,
                        flaeche.dunkler(if (gold.istDunkel) 0.10f else 0.04f),
                    ),
                ),
            )
            // Ein warmer Schein aus der Lichtrichtung oben links.
            .background(
                Brush.radialGradient(
                    colors = listOf(gold.primaer.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 700f,
                ),
            )
            .border(1.dp, lichtKante(staerke = if (gold.istDunkel) 0.16f else 0.55f), form),
    ) { inhalt() }
}

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
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                gold.flaeche,
                                gold.flaeche.mischeMit(gold.primaer, 0.14f),
                                gold.flaeche,
                            ),
                            start = Offset(versatz * 1200f - 400f + index * 40f, 0f),
                            end = Offset(versatz * 1200f + index * 40f, 300f),
                        ),
                    )
                    .border(1.dp, lichtKante(staerke = 0.20f), RoundedCornerShape(20.dp)),
            )
        }
    }
}

/**
 * Die bewegte Hintergrund-Ebene (N.3): zwei goldene Scheine, die langsam wandern.
 * Auf Leer- und Ladebildschirmen Pflicht — die App wirkt lebendig, auch wenn nichts passiert.
 */
@Composable
fun BewegterHintergrund(modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "hintergrund")
    val phase by uebergang.animateFloat(
        initialValue = 0f,
        targetValue = if (reduziert) 0f else (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(Motion.HINTERGRUND_MS), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
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
                .scale(atem)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        listOf(gold.primaer.copy(alpha = 0.26f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, fontSize = 44.sp, modifier = Modifier.rotate(dreh))
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
    val farbe = if (meldung.istFehler) Semantisch.fehler else Semantisch.erfolg
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
