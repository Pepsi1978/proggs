package de.frank.experimente.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Formen
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.lichtsaum
import de.frank.experimente.ui.theme.unterkante

/**
 * Die wiederkehrenden Bauteile — mit den Werten, die im Design gemessen wurden:
 * Kartenradius 20 dp, Rand 1 dp, Innenabstand 21 dp, Knopfhöhe 48 dp, Feldradius 14 dp,
 * Seitenrand 20 dp.
 */

/** Eine Karte: Fläche *erhöht*, 1 dp Rand, Radius 20, Lichtsaum oben. */
@Composable
fun Karte(
    modifier: Modifier = Modifier,
    beiKlick: (() -> Unit)? = null,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val farben = LocalFarben.current
    val quelle = remember0()
    val gedrueckt by quelle.collectIsPressedAsState()
    // M-01: die Karte sinkt beim Drücken auf 98 % ein, 120 ms.
    val faktor by animateFloatAsState(
        targetValue = if (gedrueckt && beiKlick != null) 0.98f else 1f,
        animationSpec = tween(dauer(Bewegung.KURZ), easing = Bewegung.einsinken),
        label = "einsinken",
    )
    Column(
        modifier
            .fillMaxWidth()
            .scale(faktor)
            .clip(Formen.karte)
            .background(farben.erhoeht)
            .border(1.dp, farben.rand, Formen.karte)
            .lichtsaum(farben.text, 0.12f)
            .then(
                if (beiKlick == null) Modifier
                else Modifier.clickable(interactionSource = quelle, indication = null, onClick = beiKlick)
            )
            .padding(21.dp),
        content = inhalt,
    )
}

@Composable
private fun remember0() = androidx.compose.runtime.remember { MutableInteractionSource() }

/** Der Bildschirmtitel in der oberen Leiste: Fraunces 28/34, Höhe der Leiste 64 dp. */
@Composable
fun Kopfzeile(
    titel: String,
    modifier: Modifier = Modifier,
    links: (@Composable RowScope.() -> Unit)? = null,
    rechts: (@Composable RowScope.() -> Unit)? = null,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            // Der Entwurf zieht eine feine Linie unter die obere Leiste — sie trennt
            // Kopfzeile und Inhalt, ohne eine Fläche zu setzen.
            .unterkante(farben.randWeich)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        links?.invoke(this)
        Text(
            text = titel,
            style = schriften.bildschirmtitel,
            color = farben.text,
            modifier = Modifier.weight(1f),
        )
        rechts?.invoke(this)
    }
}

/** Ein rundes Symbolfeld, 48 × 48, vollrund, 1 dp Rand — wie in der oberen Leiste gemessen. */
@Composable
fun Rundknopf(
    symbol: ImageVector,
    beschriftung: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    farbe: Color? = null,
) {
    val farben = LocalFarben.current
    Box(
        modifier
            .size(48.dp)
            .clip(Formen.vollrund)
            .background(farben.flaeche)
            .border(1.dp, farben.rand, Formen.vollrund)
            .clickable(onClick = beiKlick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = symbol,
            contentDescription = beschriftung,
            tint = farbe ?: farben.text,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Ein Textknopf, Höhe 48, Radius 14. Trägt er Gewicht (`betont`), bekommt er die gedeckte
 * Aktionsfläche und den Aktionsrand — genau wie „Lieber tippen“ im Entwurf.
 */
@Composable
fun Textknopf(
    text: String,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    betont: Boolean = true,
    aktiv: Boolean = true,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    // M-12/M-50: Flächen- und Randwechsel 200–240 ms.
    val flaeche by animateColorAsState(
        targetValue = if (betont) farben.aktionGedeckt else Color.Transparent,
        animationSpec = tween(dauer(Bewegung.MITTEL), easing = Bewegung.ease),
        label = "knopfflaeche",
    )
    Box(
        modifier
            .height(48.dp)
            .clip(Formen.eingabefeld)
            .background(flaeche)
            .border(1.dp, if (betont) farben.aktion.copy(alpha = 0.55f) else farben.rand, Formen.eingabefeld)
            .clickable(enabled = aktiv, onClick = beiKlick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = schriften.knopf,
            color = if (aktiv) farben.aktion else farben.blass,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Das Stufen-Etikett auf einer Karte: vollrund, JetBrains Mono 12/16, Laufweite 0,4. */
@Composable
fun Etikett(text: String, modifier: Modifier = Modifier, farbe: Color? = null) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val grund = farbe ?: farben.aktionGedeckt
    Box(
        modifier
            .clip(Formen.vollrund)
            .background(grund)
            .border(1.dp, farben.rand, Formen.vollrund)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(text = text, style = schriften.stufe, color = farben.gedaempft)
    }
}

/** Eine Zwischenüberschrift: Inter 13/17, Laufweite 0,6, in Großbuchstaben. */
@Composable
fun Zwischenueberschrift(text: String, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    Text(
        text = text.uppercase(),
        style = LocalSchriften.current.zwischenueberschrift,
        color = farben.gedaempft,
        modifier = modifier,
    )
}

/** Ein Eingabefeld: Radius 14, 1 dp Rand, Fläche *erhöht*, Innenabstand 16. */
@Composable
fun Eingabefeld(
    text: String,
    beiAenderung: (String) -> Unit,
    platzhalter: String,
    modifier: Modifier = Modifier,
    zeilen: Int = 6,
) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Box(
        modifier
            .fillMaxWidth()
            .clip(Formen.eingabefeld)
            .background(farben.erhoeht)
            .border(1.dp, farben.rand, Formen.eingabefeld)
            .padding(16.dp),
    ) {
        if (text.isEmpty()) {
            Text(platzhalter, style = schriften.fliesstext, color = farben.blass)
        }
        BasicTextField(
            value = text,
            onValueChange = beiAenderung,
            textStyle = schriften.fliesstext.copy(color = farben.text),
            cursorBrush = SolidColor(farben.aktion),
            minLines = zeilen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Der Wartezustand der KI (M-09): Text unter einem laufenden Balken, 20 dp Abstand. */
@Composable
fun Wartezustand(text: String, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(farben.aktionGedeckt),
        ) {
            val breite by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(1_200, easing = Bewegung.ruhig),
                label = "warten",
            )
            Box(
                Modifier
                    .fillMaxWidth(breite)
                    .height(3.dp)
                    .background(farben.aktion),
            )
        }
        Text(
            text = text,
            style = LocalSchriften.current.fliesstextKlein,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/** Die Fehlerkarte: Text in *Warnung*, der Knopf 12 dp darunter. */
@Composable
fun Fehlerkarte(text: String, beiWiederholen: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    Karte(modifier) {
        Text(text = text, style = LocalSchriften.current.fliesstext, color = farben.warnung)
        if (beiWiederholen != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Textknopf("Nochmal versuchen", beiWiederholen)
            }
        }
    }
}
