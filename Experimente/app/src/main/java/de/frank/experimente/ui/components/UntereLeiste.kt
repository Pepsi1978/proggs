package de.frank.experimente.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Farben
import de.frank.experimente.ui.theme.Leistensymbole
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.glas
import de.frank.experimente.ui.theme.glasschatten

/**
 * Die untere Leiste — **sechs** Felder statt fünf, in dieser festen Reihenfolge:
 * **Monitor · Heute · Ziele · Merkliste · Erkenntnisse · Logbuch.**
 *
 * Gemessen aus dem Entwurf: sie schwebt mit 12 dp Abstand nach links, rechts und unten, ist
 * 64 dp hoch, hat Radius 24 dp, ist eine Glasfläche (`E-03`) mit 1 dp Rand
 * `color-mix(Rand 70%, transparent)` und liegt auf dem Schatten `0 16px 32px rgba(0,0,0,.28)`.
 *
 * Je Feld: Symbol 22 dp über der Beschriftung in Inter 11/14, 3 dp Abstand dazwischen. Das
 * aktive Feld färbt Symbol **und** Schrift in *Aktion* — die Farbe trägt die Information
 * nicht allein, denn die Beschriftung steht ohnehin da.
 *
 * Um das aktive Feld liegt zusätzlich ein **Kästchen**, das beim Wechsel sichtbar zum
 * nächsten Feld hinüberwandert (`zeichneKaestchen`).
 *
 * Sie ist auf B-10, B-01, B-04, B-05, B-06 und B-07 identisch; F-27 (Wischen) läuft über
 * alle sechs in genau dieser Reihenfolge. Gezeichnet wird sie **einmal** von `Navigation`
 * über dem Bildschirmwechsel — sie steht fest, während der Bildschirm darunter tauscht.
 */
@Composable
fun UntereLeiste(
    jetzt: Ziel,
    beiWahl: (Ziel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val form = RoundedCornerShape(24.dp)

    // Das Kästchen um das aktive Feld. Es springt nicht, es **wandert**: sein Platz ist eine
    // Kommazahl zwischen 0 und 5, und die läuft in der Dauer eines Bildschirmwechsels
    // hinüber. Beim Wischen von *Monitor* nach *Heute* sieht man es also fahren.
    val platz by animateFloatAsState(
        targetValue = Ziel.hauptreihe.indexOf(jetzt).coerceAtLeast(0).toFloat(),
        animationSpec = tween(dauer(Bewegung.WECHSEL, stufe), easing = Bewegung.ruhig),
        label = "kaestchen",
    )
    val kastenform = RoundedCornerShape(18.dp)

    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .height(64.dp)
            // Der Schatten des Entwurfs — außen gezeichnet und unter der Leiste ausgestanzt,
            // damit er nicht durch das Glas scheint (siehe `glasschatten`).
            .glasschatten(form, versatzY = 16.dp, weichheit = 32.dp, farbe = Color.Black.copy(alpha = 0.28f))
            .clip(form)
            .glas(farben, stufe)
            .drawBehind { zeichneKaestchen(platz, farben, kastenform) }
            .border(1.dp, farben.rand.copy(alpha = 0.7f), form),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ziel.hauptreihe.forEach { ziel ->
            Feld(ziel, jetzt, beiWahl, Modifier.weight(1f))
        }
    }
}

/**
 * Zeichnet das Kästchen an seinem — auch zwischen zwei Feldern gültigen — Platz.
 *
 * Es ist so breit wie ein Feld minus 8 dp Luft und 52 dp hoch, getönt in *Aktion* mit einem
 * feinen Rand derselben Farbe. Damit trägt nicht mehr die Farbe allein, welches Feld gerade
 * gilt, sondern auch eine Fläche — das hilft bei Farbsehschwäche.
 */
private fun DrawScope.zeichneKaestchen(
    platz: Float,
    farben: Farben,
    form: RoundedCornerShape,
) {
    val felder = Ziel.hauptreihe.size
    val feldbreite = size.width / felder
    val breite = feldbreite - 8.dp.toPx()
    val hoehe = 52.dp.toPx()
    val links = platz * feldbreite + (feldbreite - breite) / 2f
    val oben = (size.height - hoehe) / 2f
    val ecke = CornerRadius(form.topStart.toPx(Size(breite, hoehe), this))
    drawRoundRect(
        color = farben.aktion.copy(alpha = 0.12f),
        topLeft = Offset(links, oben),
        size = Size(breite, hoehe),
        cornerRadius = ecke,
    )
    drawRoundRect(
        color = farben.aktion.copy(alpha = 0.38f),
        topLeft = Offset(links, oben),
        size = Size(breite, hoehe),
        cornerRadius = ecke,
        style = Stroke(1.dp.toPx()),
    )
}

@Composable
private fun Feld(
    ziel: Ziel,
    jetzt: Ziel,
    beiWahl: (Ziel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val aktiv = ziel == jetzt

    // M-94 — das Leistenfeld leuchtet auf: 240 ms, `cubic-bezier(.2, 0, 0, 1)`.
    val vordergrund by animateColorAsState(
        targetValue = if (aktiv) farben.aktion else farben.gedaempft,
        animationSpec = tween(dauer(Bewegung.LEUCHTEN, stufe), easing = Bewegung.ruhig),
        label = "leistenfarbe",
    )

    val quelle = merkeDruck()
    Column(
        modifier
            .height(64.dp)
            .federdruck(quelle)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = quelle, indication = null) { beiWahl(ziel) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = symbolFuer(ziel),
            contentDescription = ziel.beschriftung,
            tint = vordergrund,
            modifier = Modifier.size(22.dp),
        )
        Box(Modifier.height(3.dp))
        Text(
            text = ziel.beschriftung,
            style = LocalSchriften.current.leiste,
            color = vordergrund,
            maxLines = 1,
        )
    }
}

/** Die Symbole der sechs Felder, in der Reihenfolge des Entwurfs. */
private fun symbolFuer(ziel: Ziel): ImageVector = when (ziel) {
    Ziel.MONITOR -> Leistensymbole.Monitor
    Ziel.HEUTE -> Leistensymbole.Heute
    Ziel.ZIELE -> Leistensymbole.Ziele
    Ziel.MERKLISTE -> Leistensymbole.Merkliste
    Ziel.ERKENNTNISSE -> Leistensymbole.Erkenntnisse
    else -> Leistensymbole.Logbuch
}
