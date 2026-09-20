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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
    /** Ohne Angabe die Standardform des gewählten Designs; Schlicht bleibt bei 16 dp. */
    form: Shape? = null,
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
    val tokens = de.frank.wecker.design.LocalDesignTokens.current
    val form = form ?: RoundedCornerShape(if (tokens.plastisch) 16.dp else tokens.chipRadius)
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
            .graphicsLayer { scaleX = skalierung; scaleY = skalierung }
            .then(
                if (hauptKnopf && aktiviert) {
                    Modifier.pulsierenderSchein(grund, form)
                } else {
                    Modifier
                },
            )
            .then(if (tokens.plastisch) Modifier.tiefenSchatten(
                farbe = if (aktiviert) grund else Color.Black,
                hoehe = schattenHoehe,
                form = form,
                gedrueckt = gedrueckt,
            ) else Modifier)
            .clip(form)
            .then(if (tokens.plastisch)
                Modifier.background(koerperVerlauf(koerper, gedrueckt = gedrueckt && aktiviert))
                    .background(if (gedrueckt) Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)) else glanzLicht())
                    .border(1.dp, lichtKante(gedrueckt = gedrueckt && aktiviert), form)
                else Modifier.background(koerper))
            .then(
                if (aktiviert) {
                    Modifier.clickable(interactionSource = quelle, indication = null,
                        role = androidx.compose.ui.semantics.Role.Button) {
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
    /**
     * Sprechender Name für Knöpfe, deren Aufschrift allein nichts sagt — etwa ein bloßes „＋".
     * [Knopf3D] konnte das schon, [GoldKnopf] reichte es bisher nicht durch.
     */
    beschreibung: String? = null,
) {
    val gold = LocalGold.current
    Knopf3D(
        aufTipp = aufTipp,
        modifier = modifier,
        aktiviert = aktiviert && !laedt,
        hauptKnopf = hauptKnopf,
        beschreibung = beschreibung,
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
    // Nicht plastische Designs (Morgenruhe, Traumraum, Orbit) übernehmen ihre eigene Kantenform;
    // Schlicht behält den bisherigen Radius von 12 dp und den erhabenen Körper.
    val tokens = de.frank.wecker.design.LocalDesignTokens.current
    val form = RoundedCornerShape(if (tokens.plastisch) 12.dp else tokens.chipRadius)
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
            .graphicsLayer { scaleX = skalierung; scaleY = skalierung }
            // One consistent raised 3D look for every secondary button (opaque body, so the shadow
            // never shows through); only pressing sinks it in. `hervorgehoben` stays for API compatibility.
            .then(if (tokens.plastisch) Modifier.tiefenSchatten(Color.Black, 4.dp, form, gedrueckt = gedrueckt) else Modifier)
            .clip(form)
            .then(if (tokens.plastisch) Modifier.background(koerperVerlauf(gold.flaecheErhoeht, gedrueckt = gedrueckt))
                else Modifier.background(gold.flaecheErhoeht))
            .then(if (tokens.plastisch) Modifier.border(1.dp, lichtKante(gedrueckt = gedrueckt, staerke = 0.35f), form)
                else Modifier.border(1.dp, gold.rahmen, form))
            // Rolle und Mindestgröße: Ohne `Role.Button` sagt TalkBack nur den Text an, nie
            // „Schaltfläche"; ohne die Mindestgröße bleibt der Körper bei rund 34 dp unter dem
            // Richtwert von 48 dp für Tippflächen.
            .clickable(interactionSource = quelle, indication = null,
                role = androidx.compose.ui.semantics.Role.Button) {
                haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                aufTipp()
            }
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = gold.textPrimaer,
        )
    }
}
