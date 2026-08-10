package de.frank.experimente.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer

/**
 * Die untere Leiste — sie **schwebt**: 12 dp Abstand zu den Seiten und nach unten, Höhe 64,
 * Radius 24, 1 dp Rand, Fläche *fläche*. So ist sie im Entwurf gemessen.
 *
 * Das aktive Feld trägt eine Pille (Radius 20, Fläche *Aktion gedeckt*, Schrift und Symbol in
 * *Aktion*). Der Wechsel läuft über M-50/M-51 (240 ms, `cubic-bezier(.2,0,0,1)`) — dadurch
 * wandert das aktive Feld mit, wenn gewischt wird (F-27 Schritt 2).
 */
@Composable
fun UntereLeiste(
    jetzt: Ziel,
    beiWahl: (Ziel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalFarben.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(24.dp))
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Feld(Ziel.HEUTE, "Heute", jetzt, beiWahl, Modifier.weight(1f))
        Feld(Ziel.ZIELE, "Ziele", jetzt, beiWahl, Modifier.weight(1f))
        Feld(Ziel.MERKLISTE, "Merkliste", jetzt, beiWahl, Modifier.weight(1f))
        Feld(Ziel.ERKENNTNISSE, "Erkenntnisse", jetzt, beiWahl, Modifier.weight(1f))
        Feld(Ziel.LOGBUCH, "Logbuch", jetzt, beiWahl, Modifier.weight(1f))
    }
}

@Composable
private fun Feld(
    ziel: Ziel,
    beschriftung: String,
    jetzt: Ziel,
    beiWahl: (Ziel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalFarben.current
    val aktiv = ziel == jetzt
    val lang = dauer(Bewegung.LANG)

    val flaeche by animateColorAsState(
        targetValue = if (aktiv) farben.aktionGedeckt else Color.Transparent,
        animationSpec = tween(lang, easing = Bewegung.ruhig),
        label = "feldflaeche",
    )
    val vordergrund by animateColorAsState(
        targetValue = if (aktiv) farben.aktion else farben.gedaempft,
        animationSpec = tween(lang, easing = Bewegung.ruhig),
        label = "feldfarbe",
    )
    val randfarbe by animateColorAsState(
        targetValue = if (aktiv) farben.aktion else Color.Transparent,
        animationSpec = tween(lang, easing = Bewegung.ruhig),
        label = "feldrand",
    )

    Column(
        modifier
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(flaeche)
            .border(1.dp, randfarbe, RoundedCornerShape(20.dp))
            .clickable { beiWahl(ziel) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = symbolFuer(ziel),
            contentDescription = beschriftung,
            tint = vordergrund,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = beschriftung,
            style = LocalSchriften.current.stufe.copy(fontSize = 12.sp),
            color = vordergrund,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * Die Symbole der Leiste stammen aus der Messung — nicht aus einer Standardsammlung, weil
 * ein gleichnamiges Material-Symbol andere Proportionen und Strichstärken hat.
 */
private fun symbolFuer(ziel: Ziel) = when (ziel) {
    Ziel.HEUTE -> Symbole.Heute2
    Ziel.ZIELE -> Symbole.Hauptnavigation
    Ziel.MERKLISTE -> Symbole.Hauptnavigation2
    Ziel.ERKENNTNISSE -> Symbole.Hauptnavigation3
    else -> Symbole.Hauptnavigation4
}
