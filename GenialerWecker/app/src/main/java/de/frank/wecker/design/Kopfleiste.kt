package de.frank.wecker.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import de.frank.genialeideen.ui.IdeenKopfleiste
import de.frank.genialeideen.ui.KopfKnopf
import de.frank.genialeideen.ui.theme.IdeenSchriftFest
import de.frank.genialeideen.ui.theme.LocalGold

/**
 * Die Kopfleiste in der Handschrift des gewählten Designs.
 *
 * Vorher zweigte nur Orbit ab; Morgenruhe und Traumraum bekamen die volle Gold-Signatur aus
 * [IdeenKopfleiste] — Milchglas, Körnung und ein alle 3,8 Sekunden über den Titel wanderndes
 * Glanzlicht. Das ist genau der plastische Effekt, den diese beiden Designs laut ihrem eigenen
 * Merkmal `plastisch = false` gerade nicht tragen sollen. Jetzt hat jedes Design seine eigene
 * Leiste, mit identischem Inhalt und identischen Rückrufen.
 */
@Composable
fun DesignKopfleiste(
    titel: String,
    themeWahl: String,
    aufThemeTipp: (() -> Unit)? = null,
    aufEinstellungen: (() -> Unit)? = null,
    voran: (@Composable () -> Unit)? = null,
) {
    when (LocalDesignTokens.current.design) {
        // Schlicht behält seine gewohnte Glasleiste samt Glanzlicht — das ist die Signatur
        // des Standarddesigns und bleibt unangetastet.
        Design.SCHLICHT -> IdeenKopfleiste(
            titel = titel, themeWahl = themeWahl, aufThemeTipp = aufThemeTipp,
            aufEinstellungen = aufEinstellungen, voran = voran,
        )
        Design.ORBIT -> OrbitLeiste(titel, themeWahl, aufThemeTipp, aufEinstellungen, voran)
        Design.MORGENRUHE -> RuhigeLeiste(titel, themeWahl, aufThemeTipp, aufEinstellungen, voran)
        Design.TRAUMRAUM -> BogenLeiste(titel, themeWahl, aufThemeTipp, aufEinstellungen, voran)
    }
}

/** Orbit: technische Modulzeile, Versalien in fester Schrift, harte Trennlinie. */
@Composable
private fun OrbitLeiste(
    titel: String, themeWahl: String, aufThemeTipp: (() -> Unit)?,
    aufEinstellungen: (() -> Unit)?, voran: (@Composable () -> Unit)?,
) {
    val gold = LocalGold.current
    Column(Modifier.fillMaxWidth().background(gold.flaeche).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            voran?.invoke()
            // Versalien mit Sperrung brauchen mehr Platz als die sonstige Kopfzeile; zwei Zeilen
            // verhindern, dass „WECKER BEARBEITEN“ bei großer Schrift früh gekürzt wird.
            Text(
                titel.uppercase(java.util.Locale.GERMAN),
                Modifier.weight(1f).semantics { heading() },
                fontFamily = IdeenSchriftFest,
                style = MaterialTheme.typography.labelLarge,
                color = gold.primaer, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            // Dieselben Vektorsymbole wie in den anderen drei Designs. Vorher standen hier die
            // Schriftzeichen ☀ ☾ ⚙ — die fallen je nach Gerät als farbiges Emoji aus der
            // monochromen Instrumentenoptik heraus und sind nicht sicher verfügbar.
            LeistenKnoepfe(themeWahl, aufThemeTipp, aufEinstellungen)
        }
        HorizontalDivider(color = gold.rahmen)
    }
}

/**
 * Morgenruhe: ruhige, flache Leinenleiste. Kein Glas, kein Glanz, keine Dauerbewegung —
 * nur eine feine Kante nach unten, die den Kopf vom Inhalt trennt.
 */
@Composable
private fun RuhigeLeiste(
    titel: String, themeWahl: String, aufThemeTipp: (() -> Unit)?,
    aufEinstellungen: (() -> Unit)?, voran: (@Composable () -> Unit)?,
) {
    val gold = LocalGold.current
    Column(Modifier.fillMaxWidth().background(gold.hintergrund).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            voran?.invoke()
            Text(
                titel,
                Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp,
                ),
                color = gold.textPrimaer, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            LeistenKnoepfe(themeWahl, aufThemeTipp, aufEinstellungen)
        }
        HorizontalDivider(color = gold.rahmen)
    }
}

/**
 * Traumraum: Die Leiste ist der obere Rand des Blatts und endet in derselben weichen Rundung,
 * die auch die Karten tragen. Ein sehr flacher senkrechter Verlauf gibt Tiefe, ohne zu glänzen.
 */
@Composable
private fun BogenLeiste(
    titel: String, themeWahl: String, aufThemeTipp: (() -> Unit)?,
    aufEinstellungen: (() -> Unit)?, voran: (@Composable () -> Unit)?,
) {
    val gold = LocalGold.current
    // Derselbe Radius wie Karten und Kuppel — vorher trafen hier 28 dp auf 46 dp der Kuppel.
    val radius = LocalDesignTokens.current.karteRadius
    val form = RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    Column(
        Modifier.fillMaxWidth().clip(form)
            .background(Brush.verticalGradient(listOf(gold.flaecheErhoeht, gold.flaeche)))
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            voran?.invoke()
            Text(
                titel,
                Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = gold.textPrimaer, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            LeistenKnoepfe(themeWahl, aufThemeTipp, aufEinstellungen)
        }
    }
}

/** Die beiden rechten Knöpfe — in jedem Design dieselbe Reihenfolge und dieselbe Beschreibung. */
@Composable
private fun LeistenKnoepfe(
    themeWahl: String, aufThemeTipp: (() -> Unit)?, aufEinstellungen: (() -> Unit)?,
) {
    val gold = LocalGold.current
    if (aufThemeTipp != null) {
        KopfKnopf(
            beschreibung = if (themeWahl == "dark") "Dunkler Modus, tippen für hell"
            else "Heller Modus, tippen für dunkel",
            aufTipp = aufThemeTipp,
        ) {
            Icon(
                imageVector = if (themeWahl == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null, tint = gold.primaer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
    }
    if (aufEinstellungen != null) {
        KopfKnopf(beschreibung = "Einstellungen öffnen", aufTipp = aufEinstellungen) {
            Icon(
                imageVector = Icons.Default.Settings, contentDescription = null,
                tint = gold.primaer, modifier = Modifier.size(20.dp),
            )
        }
    }
}
