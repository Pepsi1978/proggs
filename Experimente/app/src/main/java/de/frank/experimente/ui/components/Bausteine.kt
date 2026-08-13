package de.frank.experimente.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Effektstufe
import de.frank.experimente.ui.theme.Farben
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.aktionsVerlauf
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.glas
import de.frank.experimente.ui.theme.gesperrterVerlauf
import de.frank.experimente.ui.theme.lichtsaum
import de.frank.experimente.ui.theme.mische
import de.frank.experimente.ui.theme.schimmer
import de.frank.experimente.ui.theme.unterkante
import de.frank.experimente.ui.theme.warteStreifen

/**
 * Die wiederkehrenden Bauteile — alle Werte wörtlich aus
 * `Experimente Fold-Aussendisplay.dc.html`.
 *
 * Kartenradius 20 dp · Rand 1 dp · Innenabstand 18 dp · Knopfhöhe 48 dp ·
 * Feldradius 14 dp · Seitenrand 16 dp · Kopfleiste 64 dp.
 */

// ---------------------------------------------------------------------------------------
// E-07 — Federphysik
// ---------------------------------------------------------------------------------------

/**
 * **E-07** — jeder Druck sinkt ein und schwingt zurück, statt linear zu skalieren:
 * `spring(dampingRatio = 0.55f, stiffness = 380f)`. Rückfallebene auf der Stufe *Aus*:
 * `tween(120)`.
 */
@Composable
fun Modifier.federdruck(quelle: MutableInteractionSource, aktiv: Boolean = true): Modifier {
    val stufe = LocalEffektstufe.current
    val gedrueckt by quelle.collectIsPressedAsState()
    val ziel = if (gedrueckt && aktiv) 0.96f else 1f
    val faktor by if (stufe.federphysik) {
        animateFloatAsState(
            targetValue = ziel,
            animationSpec = spring(
                dampingRatio = Bewegung.DRUCK_DAEMPFUNG,
                stiffness = Bewegung.DRUCK_STEIFE,
                visibilityThreshold = Spring.DefaultDisplacementThreshold,
            ),
            label = "federdruck",
        )
    } else {
        animateFloatAsState(
            targetValue = ziel,
            animationSpec = tween(dauer(Bewegung.KURZ), easing = Bewegung.einsinken),
            label = "einsinken",
        )
    }
    return this.scale(faktor)
}

@Composable
fun merkeDruck(): MutableInteractionSource = remember { MutableInteractionSource() }

// ---------------------------------------------------------------------------------------
// Kopfleiste
// ---------------------------------------------------------------------------------------

/**
 * Die Kopfleiste: 64 dp hoch, Glasfläche (`E-03`), Innenabstand 16 dp — oder 8 dp, wenn
 * links ein Zurück-Knopf steht. Der Monitor zieht zusätzlich eine feine Unterkante.
 */
@Composable
fun Kopfzeile(
    modifier: Modifier = Modifier,
    innenAbstand: Dp = 16.dp,
    mitUnterkante: Boolean = false,
    links: (@Composable RowScope.() -> Unit)? = null,
    inhalt: @Composable RowScope.() -> Unit,
) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .glas(farben, stufe)
            .then(
                // `border-bottom: 1px solid color-mix(in srgb, var(--rand) 60%, transparent)`
                if (mitUnterkante) Modifier.unterkante(farben.rand.copy(alpha = 0.6f)) else Modifier
            )
            .padding(horizontal = innenAbstand),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        links?.invoke(this)
        inhalt()
    }
}

/** Der Bildschirmtitel: Fraunces 28/34, Gewicht 600. */
@Composable
fun RowScope.Titel(text: String, klein: Boolean = false) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Text(
        text = text,
        style = if (klein) schriften.bildschirmtitelKlein else schriften.bildschirmtitel,
        color = farben.text,
    )
}

/**
 * Ein rundes Feld in der Kopfleiste: 48 × 48 dp, vollrund, **ohne** Fläche und **ohne**
 * Rand — im Entwurf steht dort nur das Symbol auf dem Glas.
 */
@Composable
fun Rundknopf(
    symbol: ImageVector,
    beschriftung: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    farbe: Color? = null,
    groesse: Dp = 24.dp,
) {
    val farben = LocalFarben.current
    val quelle = merkeDruck()
    Box(
        modifier
            .size(48.dp)
            .federdruck(quelle)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(interactionSource = quelle, indication = null, onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = symbol,
            contentDescription = beschriftung,
            tint = farbe ?: farben.text,
            modifier = Modifier.size(groesse),
        )
    }
}

// ---------------------------------------------------------------------------------------
// Karten
// ---------------------------------------------------------------------------------------

/**
 * Eine Karte: Fläche *Fläche*, 1 dp Rand *Rand*, Radius 20 dp, Innenabstand 18 dp,
 * darüber der Lichtsaum (`E-04`, `border-top: 1px color-mix(Text 12%)`).
 */
@Composable
fun Karte(
    modifier: Modifier = Modifier,
    innen: Dp = 18.dp,
    beiKlick: (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val farben = LocalFarben.current
    val quelle = merkeDruck()
    Column(
        modifier
            .fillMaxWidth()
            .federdruck(quelle, aktiv = beiKlick != null)
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
            .lichtsaum(farben.text, 0.12f)
            .then(
                if (beiKlick == null) Modifier
                else Modifier.clickable(interactionSource = quelle, indication = null, onClick = beiKlick)
            )
            .padding(innen),
        content = inhalt,
    )
}

/**
 * Ein leerer Zustand: `padding:20px; border:1px dashed Rand; border-radius:20px`,
 * Inter 16/25 in *Gedämpft*. Die Sätze stehen wörtlich in UI-Spec §8.
 */
@Composable
fun LeererZustand(text: String, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    Box(
        modifier
            .fillMaxWidth()
            .gestrichelterRand(farben.rand, 20.dp)
            .padding(20.dp),
    ) {
        Text(text, style = LocalSchriften.current.fliesstext, color = farben.gedaempft)
    }
}

/** `border: 1px dashed` — Compose hat dafür kein fertiges Bauteil, also gezeichnet. */
private fun Modifier.gestrichelterRand(farbe: Color, radius: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = farbe,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx(), radius.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
            ),
        ),
    )
}

// ---------------------------------------------------------------------------------------
// Etiketten
// ---------------------------------------------------------------------------------------

/** Welches der drei Etiketten des Entwurfs gemeint ist. */
enum class Etikettart { STUFE, ANGABE, HERKUNFT }

/**
 * Ein Etikett auf einer Karte: Höhe 24 dp, Innenabstand 10 dp, vollrund,
 * JetBrains Mono 12/16 mit Laufweite 0,4.
 *
 * - `STUFE` — Fläche *Aktion gedeckt*, Schrift *Aktion*
 * - `ANGABE` — Fläche `color-mix(Text 8%)`, Schrift *Gedämpft*
 * - `HERKUNFT` — nur 1 dp Rand *Rand*, Schrift *Gedämpft*
 */
@Composable
fun Etikett(
    text: String,
    art: Etikettart = Etikettart.ANGABE,
    modifier: Modifier = Modifier,
    /**
     * Macht das Etikett antippbar — die Tagesangabe „Tag 4 von 5" öffnet damit die
     * Dauer-Änderung. Ein antippbares Etikett trägt einen Rand in *Aktion*, damit es sich
     * sichtbar von den bloßen Angaben daneben unterscheidet.
     */
    beiKlick: (() -> Unit)? = null,
    beschriftung: String? = null,
) {
    val farben = LocalFarben.current
    val form = RoundedCornerShape(percent = 50)
    val flaeche = when (art) {
        Etikettart.STUFE -> farben.aktionGedeckt
        Etikettart.ANGABE -> farben.text.copy(alpha = 0.08f)
        Etikettart.HERKUNFT -> Color.Transparent
    }
    Box(
        modifier
            // Antippbare Etiketten sind höher: 24 dp wären als Tippfläche zu klein.
            .height(if (beiKlick == null) 24.dp else 32.dp)
            .clip(form)
            .background(flaeche)
            .then(
                when {
                    beiKlick != null -> Modifier.border(1.dp, farben.aktion.copy(alpha = 0.55f), form)
                    art == Etikettart.HERKUNFT -> Modifier.border(1.dp, farben.rand, form)
                    else -> Modifier
                },
            )
            .then(
                if (beiKlick == null) Modifier
                else Modifier
                    .clickable(onClick = beiKlick)
                    .semantics { contentDescription = beschriftung ?: text },
            )
            .padding(horizontal = if (beiKlick == null) 10.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalSchriften.current.stufe,
            color = when {
                art == Etikettart.STUFE -> farben.aktion
                beiKlick != null -> farben.text
                else -> farben.gedaempft
            },
            maxLines = 1,
        )
    }
}

/** Eine Zwischenüberschrift: Inter 13/17, Gewicht 600, Laufweite 0,6, Großbuchstaben. */
@Composable
fun Zwischenueberschrift(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = LocalSchriften.current.zwischenueberschrift,
        color = LocalFarben.current.gedaempft,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------------------
// Knöpfe
// ---------------------------------------------------------------------------------------

/**
 * Der betonte Knopf: Höhe 48 dp, vollrund, Verlauf `linear-gradient(145deg, …)`,
 * Schrift *Auf Aktion*, darunter der Schein `0 8px 20px color-mix(Aktion 30%)`.
 *
 * Gesperrt (drei laufen schon) wird er flach — `linear-gradient(Erhöht, Erhöht)` mit
 * Schrift in *Blass* und ohne Schein. Der Grund steht daneben als Satz (A-26).
 */
@Composable
fun KnopfBetont(
    text: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    aktiv: Boolean = true,
    hoehe: Dp = 48.dp,
    gross: Boolean = false,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current
    val quelle = merkeDruck()
    val form = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .height(hoehe)
            .federdruck(quelle, aktiv)
            .then(
                // E-05: `box-shadow: 0 8px 20px color-mix(in srgb, var(--aktion) 30%, transparent)`
                if (aktiv && stufe.schein) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = form,
                        clip = false,
                        ambientColor = farben.aktion.copy(alpha = 0.30f),
                        spotColor = farben.aktion.copy(alpha = 0.30f),
                    )
                } else Modifier
            )
            .clip(form)
            .background(if (aktiv) aktionsVerlauf(farben) else gesperrterVerlauf(farben))
            .clickable(interactionSource = quelle, indication = null, enabled = aktiv, onClick = beiKlick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (gross) schriften.knopf else schriften.knopfKlein,
            color = if (aktiv) farben.aufAktion else farben.blass,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Der umrandete Knopf: Höhe 48 dp, vollrund, 1 dp Rand *Rand*, Schrift *Text*. */
@Composable
fun KnopfUmrandet(
    text: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    farbe: Color? = null,
    randfarbe: Color? = null,
) {
    val farben = LocalFarben.current
    val quelle = merkeDruck()
    val form = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .height(48.dp)
            .federdruck(quelle)
            .clip(form)
            .border(1.dp, randfarbe ?: farben.rand, form)
            .clickable(interactionSource = quelle, indication = null, onClick = beiKlick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalSchriften.current.knopfKlein,
            color = farbe ?: farben.text,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * Der schlichte Textknopf: mindestens 48 dp hoch, ohne Fläche, Schrift in *Aktion*.
 * Radius 14 dp — der Entwurf setzt ihn dort, wo der Knopf keine Pille ist.
 */
@Composable
fun Textknopf(
    text: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    symbol: ImageVector? = null,
    farbe: Color? = null,
    aktiv: Boolean = true,
) {
    val farben = LocalFarben.current
    val quelle = merkeDruck()
    Row(
        modifier
            .heightIn(min = 48.dp)
            .federdruck(quelle, aktiv)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = quelle, indication = null, enabled = aktiv, onClick = beiKlick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (symbol != null) {
            Icon(symbol, null, tint = farbe ?: farben.aktion, modifier = Modifier.size(22.dp))
        }
        Text(
            text = text,
            style = LocalSchriften.current.knopf,
            color = if (aktiv) (farbe ?: farben.aktion) else farben.blass,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Eingabe
// ---------------------------------------------------------------------------------------

/** Ein Eingabefeld: Radius 14 dp, 1 dp Rand, Fläche *Erhöht*, Innenabstand 18 dp. */
@Composable
fun Eingabefeld(
    text: String,
    beiAenderung: (String) -> Unit,
    platzhalter: String,
    modifier: Modifier = Modifier,
    mindesthoehe: Dp = 150.dp,
    innen: Dp = 18.dp,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Box(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = mindesthoehe)
            .clip(RoundedCornerShape(14.dp))
            .background(farben.erhoeht)
            .border(1.dp, farben.rand, RoundedCornerShape(14.dp))
            .padding(innen),
    ) {
        if (text.isEmpty()) {
            Text(platzhalter, style = schriften.fliesstext, color = farben.blass)
        }
        BasicTextField(
            value = text,
            onValueChange = beiAenderung,
            textStyle = schriften.fliesstext.copy(color = farben.text),
            cursorBrush = SolidColor(farben.aktion),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------------------
// Zwischenzustände
// ---------------------------------------------------------------------------------------

/**
 * **M-09 · E-14 — der Wartezustand der KI.** Eine Karte mit einem 2 dp hohen Lichtstreifen
 * darüber, der in 1800 ms durchzieht, und dem Satz darunter (Inter 16/25, *Gedämpft*).
 */
@Composable
fun Wartekarte(text: String, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(farben.erhoeht)
                .warteStreifen(farben, stufe),
        )
        Text(
            text = text,
            style = LocalSchriften.current.fliesstext,
            color = farben.gedaempft,
            modifier = Modifier.padding(20.dp),
        )
    }
}

/**
 * **E-13 · M-89 — das Schimmer-Skelett.** Beim Laden stehen Kartenumrisse mit wanderndem
 * Lichtstreifen: 118 dp hoch, Radius 20 dp, Fläche *Fläche*, 1 dp Rand *Rand weich*.
 */
@Composable
fun Skelett(modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    Box(
        modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.randWeich, RoundedCornerShape(20.dp))
            .schimmer(farben, stufe),
    )
}

/**
 * Die Störungsmeldung: `color-mix(Warnung 16%, Fläche)` mit 1 dp Rand
 * `color-mix(Warnung 46%)`, Radius 14 dp, Symbol 24 dp, Knopf „Nochmal“ rechts.
 * Sie liegt über dem Inhalt, direkt unter der Kopfleiste.
 */
@Composable
fun Stoerung(text: String, beiNochmal: () -> Unit, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(mische(farben.flaeche, farben.warnung, 0.16f))
            .border(1.dp, farben.warnung.copy(alpha = 0.46f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            de.frank.experimente.ui.theme.Symbole.Hinweis,
            contentDescription = null,
            tint = farben.warnung,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = LocalSchriften.current.knopfKlein.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(400)),
            color = farben.text,
            modifier = Modifier.weight(1f),
        )
        Textknopf("Nochmal", beiNochmal)
    }
}

/**
 * Der kurze Hinweis, der von unten einblendet („Steht jetzt unter ‚Steht an‘.“):
 * Fläche *Erhöht*, 1 dp Rand *Rand*, Radius 14 dp, Inter 14/21.
 */
@Composable
fun Hinweiszeile(text: String, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(farben.erhoeht)
            .border(1.dp, farben.rand, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text, style = LocalSchriften.current.fliesstextKlein, color = farben.text)
    }
}

// ---------------------------------------------------------------------------------------
// Fortschrittsring und Plus-Knopf
// ---------------------------------------------------------------------------------------

/**
 * **M-87 — der Fortschrittsring** auf einer Laufkarte: 56 dp, Ring 4 dp, Radius 24 dp,
 * Spur in *Rand weich*, Bogen in *Aktion* mit rundem Ende, Beginn oben (−90°). Er füllt
 * sich in 600 ms mit der Hauskurve.
 *
 * Er zeigt den Stand der **heutigen** Aufgaben und nichts darüber hinaus — keine Serie,
 * keine Quote, keinen Vergleich mit gestern. Der Stand steht als Zahl in der Mitte, damit
 * kein Effekt Information allein trägt.
 */
@Composable
fun Fortschrittsring(anteil: Float, beschriftung: String, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val gefuellt by animateFloatAsState(
        targetValue = anteil.coerceIn(0f, 1f),
        animationSpec = tween(dauer(Bewegung.RING, stufe), easing = Bewegung.ruhig),
        label = "ring",
    )
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strich = 4.dp.toPx()
            val einzug = strich / 2f
            drawArc(
                color = farben.randWeich,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(einzug, einzug),
                size = androidx.compose.ui.geometry.Size(size.width - strich, size.height - strich),
                style = Stroke(width = strich),
            )
            if (gefuellt > 0f) {
                drawArc(
                    color = farben.aktion,
                    startAngle = -90f,
                    sweepAngle = 360f * gefuellt,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(einzug, einzug),
                    size = androidx.compose.ui.geometry.Size(size.width - strich, size.height - strich),
                    style = Stroke(width = strich, cap = StrokeCap.Round),
                )
            }
        }
        Text(beschriftung, style = LocalSchriften.current.ringstand, color = farben.text, maxLines = 1)
    }
}

/**
 * **M-79 — der schwebende Plus-Knopf**, 64 dp, vollrund, mit dem Aktionsverlauf.
 * Er atmet in 3200 ms mit `cubic-bezier(0.42, 0, 0.58, 1)`: `scale(1 → 1.05)`, der Schein
 * wächst von `0 10px 26px Aktion 34%` auf `0 14px 40px Aktion 52%`.
 */
@Composable
fun Plusknopf(beschriftung: String, beiKlick: () -> Unit, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val takt = rememberInfiniteTransition(label = "atmen")
    val weite by takt.animateFloat(
        initialValue = 1f,
        targetValue = if (stufe.dauerbewegung) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Bewegung.PLUSATEM, easing = Bewegung.atmen),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atemweite",
    )
    val quelle = merkeDruck()
    Box(
        modifier
            .size(64.dp)
            .scale(weite)
            .federdruck(quelle)
            .clip(RoundedCornerShape(percent = 50))
            .background(aktionsVerlauf(farben))
            .clickable(interactionSource = quelle, indication = null, onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            de.frank.experimente.ui.theme.Symbole.Plus,
            contentDescription = beschriftung,
            tint = farben.aufAktion,
            modifier = Modifier.size(28.dp),
        )
    }
}

/** Ein Umschalter aus Pillen (Effekte, Erscheinung, Reiter, Morgen/Abend). */
@Composable
fun Pillenwahl(
    eintraege: List<String>,
    gewaehlt: Int,
    beiWahl: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hoehe: Dp = 44.dp,
) {
    val farben = LocalFarben.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(farben.text.copy(alpha = 0.08f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        eintraege.forEachIndexed { nr, name ->
            val aktiv = nr == gewaehlt
            Box(
                Modifier
                    .weight(1f)
                    .height(hoehe)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (aktiv) farben.flaeche else Color.Transparent)
                    .clickable { beiWahl(nr) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name,
                    style = LocalSchriften.current.knopfKlein,
                    color = if (aktiv) farben.aktion else farben.gedaempft,
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Dauer eines Experiments
// ---------------------------------------------------------------------------------------

/**
 * Die Wahl der Tagesanzahl — beim Anlegen, beim Verlängern und beim nachträglichen Ändern.
 *
 * Die Dauer kam bisher ausschließlich aus der KI-Schätzung und ließ sich danach nirgends
 * berichtigen: aus „die nächsten sechs, sieben Tage" wurden zwei, und dabei blieb es. Diese
 * Zeile gehört Frank.
 *
 * Minus und Plus decken den Alltag ab, die schnellen Sprünge darunter die üblichen Längen.
 */
@Composable
fun Tagewahl(
    tage: Int,
    beiAenderung: (Int) -> Unit,
    modifier: Modifier = Modifier,
    kleinstes: Int = 1,
    groesstes: Int = 60,
    beschriftung: String = "Dauer",
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    Column(modifier.fillMaxWidth()) {
        Text(
            text = beschriftung,
            style = schriften.feldbeschriftung,
            color = farben.gedaempft,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Schritt("−", "Ein Tag weniger", tage > kleinstes) {
                beiAenderung((tage - 1).coerceAtLeast(kleinstes))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$tage",
                    style = schriften.zahl,
                    color = farben.aktion,
                )
                Text(
                    text = if (tage == 1) "Tag" else "Tage",
                    style = schriften.stufe,
                    color = farben.gedaempft,
                )
            }
            Schritt("+", "Ein Tag mehr", tage < groesstes) {
                beiAenderung((tage + 1).coerceAtMost(groesstes))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 3, 7, 14, 30).filter { it in kleinstes..groesstes }.forEach { wert ->
                Sprung(wert, wert == tage, Modifier.weight(1f)) { beiAenderung(wert) }
            }
        }
    }
}

/** Ein Schritt-Knopf der Tagewahl: 48 dp, damit er sicher zu treffen ist. */
@Composable
private fun Schritt(zeichen: String, beschriftung: String, aktiv: Boolean, beiKlick: () -> Unit) {
    val farben = LocalFarben.current
    val quelle = merkeDruck()
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (aktiv) farben.erhoeht else farben.erhoeht.copy(alpha = 0.4f))
            .border(1.dp, farben.rand, RoundedCornerShape(percent = 50))
            .federdruck(quelle, aktiv)
            .clickable(interactionSource = quelle, indication = null, enabled = aktiv, onClick = beiKlick)
            .semantics { contentDescription = beschriftung },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = zeichen,
            style = LocalSchriften.current.knopf,
            color = if (aktiv) farben.text else farben.blass,
        )
    }
}

/** Eine der üblichen Längen zum Antippen. */
@Composable
private fun Sprung(wert: Int, aktiv: Boolean, modifier: Modifier = Modifier, beiKlick: () -> Unit) {
    val farben = LocalFarben.current
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (aktiv) farben.aktionGedeckt else Color.Transparent)
            .border(1.dp, if (aktiv) farben.aktion else farben.rand, RoundedCornerShape(percent = 50))
            .clickable(onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$wert",
            style = LocalSchriften.current.stufe,
            color = if (aktiv) farben.aktion else farben.gedaempft,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Vorlesen
// ---------------------------------------------------------------------------------------

/**
 * Der Lautsprecher, der einen Text vorlesen lässt (F-12).
 *
 * Vorlesen gab es bisher an genau **einer** Stelle: für die frische Einschätzung auf B-03.
 * Gesprächsrunden, Erkenntnisse, Logbuch-Tage und frühere Auswertungen waren nur lesbar,
 * obwohl die App auf Sprache gebaut ist.
 *
 * Er trägt die Form der übrigen Rundknöpfe: 44 dp Tippfläche, Symbol 22 dp, Federdruck. Der
 * gerade sprechende Knopf färbt sich in *Aktion* und legt die Fläche *Aktion gedeckt*
 * darunter — die Farbe trägt die Information nicht allein, die Fläche zeigt es mit.
 */
@Composable
fun Vorleseknopf(
    spricht: Boolean,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    beschriftung: String = "Vorlesen",
    groesse: Dp = 44.dp,
) {
    val farben = LocalFarben.current
    val quelle = merkeDruck()
    Box(
        modifier
            .size(groesse)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (spricht) farben.aktionGedeckt else Color.Transparent)
            .federdruck(quelle)
            .clickable(interactionSource = quelle, indication = null, onClick = beiKlick)
            .semantics { contentDescription = if (spricht) "$beschriftung anhalten" else beschriftung },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (spricht) Symbole.Stopp else Symbole.Vorlesen,
            contentDescription = null,
            tint = if (spricht) farben.aktion else farben.gedaempft,
            modifier = Modifier.size(groesse * 0.5f),
        )
    }
}
