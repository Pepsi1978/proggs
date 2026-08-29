package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.Semantisch

/**
 * Die Kopfleiste aus Baustein C: links der Theme-Knopf, rechts daneben das Zahnrad.
 * Die Reihenfolge ist fest.
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
                brush = Brush.linearGradient(
                    listOf(gold.primaer, gold.akzentWarm, gold.primaer),
                ),
            ),
            maxLines = 1,
        )
        if (aufThemeTipp != null) {
            KopfKnopf(
                beschreibung = when (themeWahl) {
                    "light" -> "Heller Modus, tippen für dunkel"
                    "dark" -> "Dunkler Modus, tippen für Systemvorgabe"
                    else -> "Systemvorgabe, tippen für hell"
                },
                aufTipp = aufThemeTipp,
            ) {
                Icon(
                    imageVector = when (themeWahl) {
                        "light" -> Icons.Default.LightMode
                        "dark" -> Icons.Default.DarkMode
                        else -> Icons.Default.BrightnessAuto
                    },
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
    Box(
        modifier = Modifier
            .size(40.dp)
            .druckEffekt(aufTipp)
            .clip(RoundedCornerShape(12.dp))
            .background(gold.primaer.copy(alpha = 0.10f))
            .border(1.dp, gold.primaer.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .semantics { contentDescription = beschreibung },
        contentAlignment = Alignment.Center,
    ) { inhalt() }
}

/** Jeder Knopf sinkt beim Drücken kurz ein und federt aus (Baustein N.2). */
@Composable
fun Modifier.druckEffekt(aufTipp: () -> Unit): Modifier {
    val reduziert = LocalBewegungReduziert.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()
    val faktor by animateFloatAsState(
        targetValue = if (gedrueckt && !reduziert) 0.96f else 1f,
        animationSpec = Motion.mikro(reduziert),
        label = "druck",
    )
    return this
        .scale(faktor)
        .clickable(interactionSource = quelle, indication = null, onClick = aufTipp)
}

/** Goldener Schein hinter aktiven Elementen. */
fun Modifier.goldSchein(farbe: Color, hoehe: Dp = 12.dp, radius: Dp = 20.dp): Modifier =
    this.shadow(
        elevation = hoehe,
        shape = RoundedCornerShape(radius),
        ambientColor = farbe,
        spotColor = farbe,
    )

/** Karte mit Verlauf, Lichtkante und Tiefe statt Einfarbfläche (Baustein N.2). */
@Composable
fun GoldKarte(
    modifier: Modifier = Modifier,
    erhoeht: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    val flaeche = if (erhoeht) gold.flaecheErhoeht else gold.flaeche
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        flaeche,
                        if (gold.istDunkel) flaeche.mischeMit(gold.primaer, 0.05f) else flaeche,
                    ),
                    start = Offset.Zero,
                    end = Offset(0f, Float.POSITIVE_INFINITY),
                    tileMode = TileMode.Clamp,
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        gold.primaer.copy(alpha = 0.28f),
                        gold.rahmen.copy(alpha = 0.6f),
                    ),
                ),
                shape = RoundedCornerShape(20.dp),
            ),
    ) { inhalt() }
}

private fun Color.mischeMit(andere: Color, anteil: Float): Color = Color(
    red = red * (1 - anteil) + andere.red * anteil,
    green = green * (1 - anteil) + andere.green * anteil,
    blue = blue * (1 - anteil) + andere.blue * anteil,
    alpha = alpha,
)

/** Ein Element blendet gestaffelt auf und gleitet leicht hoch (Baustein N.2). */
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
                )
        },
        exit = fadeOut(tween(Motion.MIKRO_MS)),
    ) { inhalt() }
}

/** Platzhalter-Gerüst mit Schimmer, solange geladen wird (Baustein L und N.2). */
@Composable
fun SchimmerGeruest(zeilen: Int = 3, modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "schimmer")
    val versatz by uebergang.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "versatz",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(zeilen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (reduziert) {
                            Brush.linearGradient(listOf(gold.flaeche, gold.flaeche))
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    gold.flaeche,
                                    gold.flaecheErhoeht,
                                    gold.flaeche,
                                ),
                                start = Offset(versatz * 900f - 300f, 0f),
                                end = Offset(versatz * 900f, 300f),
                            )
                        },
                    ),
            )
        }
    }
}

/**
 * Ein Leerzustand mit Symbol, einem Satz und dem Knopf, der ihn füllt (Baustein L).
 * „Noch nichts angelegt" und „Suche ohne Treffer" sind bewusst verschiedene Bildschirme.
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
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.radialGradient(
                        listOf(gold.primaer.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, fontSize = 40.sp)
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
            GoldKnopf(text = knopfText, aufTipp = aufKnopf)
        }
    }
}

@Composable
fun GoldKnopf(
    text: String,
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
    aktiviert: Boolean = true,
    laedt: Boolean = false,
) {
    val gold = LocalGold.current
    Box(
        modifier = modifier
            .then(if (aktiviert && !laedt) Modifier.druckEffekt(aufTipp) else Modifier)
            .alpha(if (aktiviert && !laedt) 1f else 0.5f)
            .goldSchein(gold.primaer.copy(alpha = 0.6f), hoehe = if (aktiviert) 10.dp else 0.dp, radius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(gold.primaer, gold.primaerGedaempft)))
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (laedt) "Einen Moment …" else text,
            color = gold.aufPrimaer,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gold.flaecheErhoeht)
            .border(1.dp, farbe.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
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
            TextButton(onClick = { wiederholen(); aufSchliessen() }) {
                Text("Wiederholen", color = gold.primaer)
            }
        }
        if (meldung.zuEinstellungen) {
            TextButton(onClick = { aufEinstellungen(); aufSchliessen() }) {
                Text("Einstellungen", color = gold.primaer)
            }
        }
        TextButton(onClick = aufSchliessen) {
            Text("Weg", color = gold.textGedaempft)
        }
    }
}

/** Auf dem Cover-Display des Fold ist alles einspaltig, aufgeklappt wird es zweispaltig. */
@Composable
fun istBreit(): Boolean = LocalConfiguration.current.screenWidthDp >= 600
