package de.frank.genialeideen.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.dunkler
import de.frank.genialeideen.ui.theme.glanzLicht
import de.frank.genialeideen.ui.theme.koerperVerlauf
import de.frank.genialeideen.ui.theme.lichtKante
import de.frank.genialeideen.ui.theme.pulsierenderSchein
import de.frank.genialeideen.ui.theme.tiefenSchatten

/**
 * Der plastische 3D-Knopf aus Baustein N.2 — die **einzige** Knopf-Bauart der App.
 *
 * Vier Schichten von unten nach oben: Schlagschatten, Körper mit senkrechtem Verlauf,
 * Lichtkante oben und dunkle Kante unten, Glanzbogen im oberen Drittel. Beim Drücken kippt
 * die Plastik um: Der Verlauf dreht sich, der Schatten schrumpft, der Knopf sinkt ein und
 * federt danach sichtbar aus. Dazu ein kurzer haptischer Impuls.
 *
 * Rohe `Button`- oder `TextButton`-Aufrufe gibt es in dieser App bewusst nirgends.
 */
@Composable
fun Knopf3D(
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
    grundfarbe: Color? = null,
    form: Shape = RoundedCornerShape(16.dp),
    hoehe: Dp = Hoehe.karteErhoeht,
    aktiviert: Boolean = true,
    /** Der wichtigste Knopf eines Bildschirms bekommt zusätzlich den atmenden Schein. */
    hauptKnopf: Boolean = false,
    innenAbstandWaagerecht: Dp = 22.dp,
    innenAbstandSenkrecht: Dp = 13.dp,
    beschreibung: String? = null,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val haptik = LocalHapticFeedback.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()

    val grund = grundfarbe ?: gold.primaer
    // Deaktivierte Knöpfe bleiben plastisch, nur entsättigt — nie ein graues Rechteck.
    val koerper = if (aktiviert) grund else grund.dunkler(0.35f).copy(alpha = 0.55f)

    val skalierung by animateFloatAsState(
        targetValue = if (gedrueckt && aktiviert && !reduziert) 0.96f else 1f,
        animationSpec = Motion.mikro(reduziert),
        label = "knopfdruck",
    )
    val schattenHoehe by animateDpAsState(
        targetValue = if (gedrueckt && aktiviert) 2.dp else hoehe,
        animationSpec = Motion.mikro(reduziert),
        label = "knopfschatten",
    )

    Box(
        modifier = modifier
            .scale(skalierung)
            .then(
                if (hauptKnopf && aktiviert) {
                    Modifier.pulsierenderSchein(grund, form)
                } else {
                    Modifier
                },
            )
            .tiefenSchatten(
                farbe = if (aktiviert) grund else Color.Black,
                hoehe = schattenHoehe,
                form = form,
                gedrueckt = gedrueckt,
            )
            .clip(form)
            .background(koerperVerlauf(koerper, gedrueckt = gedrueckt && aktiviert))
            .background(if (gedrueckt) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) else glanzLicht())
            .border(1.dp, lichtKante(gedrueckt = gedrueckt && aktiviert), form)
            .then(
                if (aktiviert) {
                    Modifier.clickable(interactionSource = quelle, indication = null) {
                        haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        aufTipp()
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (beschreibung != null) {
                    Modifier.semantics { contentDescription = beschreibung }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = innenAbstandWaagerecht, vertical = innenAbstandSenkrecht),
        contentAlignment = Alignment.Center,
    ) { inhalt() }
}

/** Der beschriftete Standardknopf in Gold. */
@Composable
fun GoldKnopf(
    text: String,
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
    aktiviert: Boolean = true,
    laedt: Boolean = false,
    hauptKnopf: Boolean = false,
    symbol: (@Composable () -> Unit)? = null,
) {
    val gold = LocalGold.current
    Knopf3D(
        aufTipp = aufTipp,
        modifier = modifier,
        aktiviert = aktiviert && !laedt,
        hauptKnopf = hauptKnopf,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (symbol != null) {
                symbol()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (laedt) "Einen Moment …" else text,
                color = gold.aufPrimaer,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Ein runder, plastischer Knopf für Symbole. */
@Composable
fun RundKnopf3D(
    beschreibung: String,
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
    groesse: Dp = 46.dp,
    grundfarbe: Color? = null,
    aktiviert: Boolean = true,
    hauptKnopf: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    Knopf3D(
        aufTipp = aufTipp,
        modifier = modifier.size(groesse),
        grundfarbe = grundfarbe,
        form = CircleShape,
        aktiviert = aktiviert,
        hauptKnopf = hauptKnopf,
        innenAbstandWaagerecht = 0.dp,
        innenAbstandSenkrecht = 0.dp,
        beschreibung = beschreibung,
        inhalt = inhalt,
    )
}

/**
 * Ein flacher wirkender Knopf für zweitrangige Handlungen — vertieft statt erhaben,
 * aber weiterhin plastisch (N.2: „nicht gewählt = leicht vertieft").
 */
@Composable
fun StillerKnopf(
    text: String,
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
    hervorgehoben: Boolean = false,
) {
    val gold = LocalGold.current
    val form = RoundedCornerShape(12.dp)
    val reduziert = LocalBewegungReduziert.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()
    val skalierung by animateFloatAsState(
        targetValue = if (gedrueckt && !reduziert) 0.96f else 1f,
        animationSpec = Motion.mikro(reduziert),
        label = "stillerdruck",
    )
    val haptik = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(skalierung)
            .then(
                if (hervorgehoben) {
                    Modifier.tiefenSchatten(gold.primaer, Hoehe.karte, form)
                } else {
                    Modifier
                },
            )
            .clip(form)
            .background(
                if (hervorgehoben) {
                    koerperVerlauf(gold.primaer.copy(alpha = 0.22f))
                } else {
                    // Vertieft: oben dunkler, unten heller — die Umkehrung des Knopfkörpers.
                    Brush.verticalGradient(
                        listOf(
                            gold.flaecheErhoeht.dunkler(0.10f),
                            gold.flaecheErhoeht,
                        ),
                    )
                },
            )
            .border(1.dp, if (hervorgehoben) lichtKante() else lichtKante(gedrueckt = true, staerke = 0.20f), form)
            .clickable(interactionSource = quelle, indication = null) {
                haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                aufTipp()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (hervorgehoben) gold.primaer else gold.textGedaempft,
        )
    }
}
