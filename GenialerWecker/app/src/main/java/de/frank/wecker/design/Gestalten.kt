package de.frank.wecker.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.R
import de.frank.genialeideen.ui.GoldKarte
import de.frank.wecker.SichtbarerHintergrund
import de.frank.genialeideen.ui.theme.LocalGold

/**
 * Schlicht — die bisherige Optik, unverändert. Es werden dieselben Bausteine aufgerufen wie zuvor:
 * [SichtbarerHintergrund] und [GoldKarte] mit ihrem plastischen Verlauf, Schein und Lichtkante.
 * Hier wird nichts nachgebaut, damit das gewohnte Erscheinungsbild exakt erhalten bleibt.
 */
object SchlichtGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) = SichtbarerHintergrund()

    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) =
        GoldKarte(modifier = modifier, erhoeht = erhoeht, inhalt = inhalt)

    @Composable override fun Motiv(modifier: Modifier) { /* Schlicht kommt ohne Bildmotiv aus. */ }

    override val zeigtRestzeitRing = true

    @Composable override fun trennfarbe(): Color = LocalGold.current.textGedaempft.copy(alpha = .3f)
}

/**
 * Morgenruhe — ruhige, weiche Flächen ohne Glanz. Der Hintergrund ist ein sanfter Verlauf statt des
 * schwebenden Goldlichts, Karten sind flach mit klarer Kante. Das Bettmotiv gehört sichtbar dazu.
 */
object MorgenruheGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) {
        val gold = LocalGold.current
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(gold.hintergrund, gold.flaecheErhoeht.copy(alpha = .55f), gold.hintergrund),
                ),
            ),
        )
    }

    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) {
        val gold = LocalGold.current
        val form = RoundedCornerShape(22.dp)
        Box(
            modifier
                .clip(form)
                .background(if (erhoeht) gold.flaecheErhoeht else gold.flaeche)
                .border(1.dp, gold.rahmen, form),
        ) { inhalt() }
    }

    @Composable override fun Motiv(modifier: Modifier) {
        val gold = LocalGold.current
        Image(
            painter = painterResource(R.drawable.design_bett),
            contentDescription = null,
            modifier = modifier.size(132.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, gold.rahmen, RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
        )
    }

    override val zeigtRestzeitRing = false

    @Composable override fun trennfarbe(): Color = LocalGold.current.rahmen
}

/**
 * Traumraum — skulpturale, stark gerundete Flächen mit weichem Schein von oben. Das Kissenmotiv
 * sitzt als rundes Objekt im Kopfbereich.
 */
object TraumraumGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) {
        val gold = LocalGold.current
        Box(
            Modifier.fillMaxSize()
                .background(gold.hintergrund)
                .background(
                    Brush.radialGradient(
                        colors = listOf(gold.primaer.copy(alpha = .16f), Color.Transparent),
                        center = Offset(0.5f, 0f),
                        radius = 1400f,
                    ),
                ),
        )
    }

    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) {
        val gold = LocalGold.current
        val form = RoundedCornerShape(32.dp)
        Box(
            modifier
                .clip(form)
                .background(if (erhoeht) gold.flaecheErhoeht else gold.flaeche)
                .border(1.dp, gold.rahmen, form),
        ) { inhalt() }
    }

    @Composable override fun Motiv(modifier: Modifier) {
        Image(
            painter = painterResource(R.drawable.design_kissen),
            contentDescription = null,
            modifier = modifier.size(148.dp).clip(RoundedCornerShape(50)),
            contentScale = ContentScale.Crop,
        )
    }

    override val zeigtRestzeitRing = true

    @Composable override fun trennfarbe(): Color = LocalGold.current.rahmen
}

/**
 * Orbit — Instrumententafel: kantige Module, klare Kanten, kein Verlauf. Die Sternbahn ist das
 * einzige Bildelement und bleibt ein kleines Feld.
 */
object OrbitGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) {
        Box(Modifier.fillMaxSize().background(LocalGold.current.hintergrund))
    }

    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) {
        val gold = LocalGold.current
        val form = RoundedCornerShape(6.dp)
        Box(
            modifier
                .clip(form)
                .background(if (erhoeht) gold.flaecheErhoeht else gold.flaeche)
                .border(1.dp, gold.rahmen, form),
        ) { inhalt() }
    }

    @Composable override fun Motiv(modifier: Modifier) {
        val gold = LocalGold.current
        Image(
            painter = painterResource(R.drawable.design_sternbahn),
            contentDescription = null,
            modifier = modifier.size(116.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, gold.rahmen, RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
    }

    override val zeigtRestzeitRing = true

    @Composable override fun trennfarbe(): Color = LocalGold.current.rahmen
}

