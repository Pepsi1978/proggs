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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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

    /**
     * Das freigestellte Objekt steht für sich — wie beim Orbit-Motiv: kein Beschnitt, keine Platte,
     * kein Rahmen. Vorher lag hier das Bild mit seinem eingebrannten cremefarbenen Hintergrund in
     * einer gerahmten Kachel; im Dunkelmodus stand deshalb eine helle Fläche mitten im dunklen Raum,
     * und nach einem Palettenwechsel hätte sie erst recht nicht mehr gepasst.
     */
    @Composable override fun Motiv(modifier: Modifier) {
        // Das Bild stammt noch aus der abgelösten Salbei-/Terrakotta-Welt. Bis es in Leinen und
        // Tinte neu erzeugt ist, nimmt eine Entsättigung ihm die Buntheit: Terrakotta wird
        // Messingbraun, Salbei ein ruhiges Graugrün. Das ist eine Tonwertkorrektur, keine
        // flache Einfärbung — Schattierung und Materialwirkung bleiben vollständig erhalten.
        Image(
            painter = painterResource(R.drawable.design_bett_frei),
            contentDescription = null,
            modifier = if (modifier == Modifier) Modifier.size(120.dp) else modifier,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.55f) }),
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
                        // Gedämpft von 16 auf 9 Prozent: Oben lagen drei warme Schichten
                        // übereinander — dieser Schein, die helle Kuppel und der orangefarbene
                        // Hauptknopf. Eine davon muss zurücktreten, sonst glüht der ganze Kopf.
                        colors = listOf(gold.primaer.copy(alpha = .09f), Color.Transparent),
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

    /**
     * Ebenfalls freigestellt. Der runde Beschnitt entfällt damit: Er hatte nur den Zweck, den
     * deckenden pflaumefarbenen Bildhintergrund zu einer Scheibe zu machen. Auf warmem Schwarz
     * stünde diese Scheibe als Fremdkörper.
     */
    @Composable override fun Motiv(modifier: Modifier) {
        // Der Mond auf dem Kissen ist noch im alten Rosé gemalt — auf der warmen Glutkuppel wäre
        // das genau der Rückfall in die abgelöste Pflaume-/Rosé-Welt. Der multiplizierende Filter
        // nimmt den Rotstich heraus; weil er die vorhandenen Helligkeiten multipliziert statt sie
        // zu ersetzen, bleibt die Plastik vollständig erhalten. Ein neu erzeugtes Bild wäre die
        // saubere Lösung.
        //
        // Die Tönung ist modusabhängig, weil dieselbe Farbe auf den beiden Kuppeln sehr
        // unterschiedlich wirkt. Gemessen gegen die jeweilige Kuppelfarbe, mit dem Mittelwert
        // der deckenden Bildpunkte (#B68D7F):
        //   dunkle Kuppel #342519 — #FFC898 ergibt 3,73:1, dunklere Tönungen nur 2,1 bis 2,5:1
        //   helle Kuppel  #EFD5C0 — #FFC898 ergibt magere 2,81:1, #C87A45 dagegen 5,02:1
        // Im Dunkeln bleibt es deshalb beim warmen Bernstein; im Hellen tritt eine tiefere
        // Tönung an seine Stelle, damit sich das Kissen von der Pfirsichfläche absetzt.
        val toenung = if (LocalGold.current.istDunkel) Color(0xFFFFC898) else Color(0xFFC87A45)
        Image(
            painter = painterResource(R.drawable.design_kissen_frei),
            contentDescription = null,
            modifier = if (modifier == Modifier) Modifier.size(128.dp) else modifier,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(toenung, BlendMode.Modulate),
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

    /**
     * Das freigestellte Objekt steht für sich: keine Platte, kein Rahmen, kein Beschnitt. Die Größe
     * gibt der Aufrufer vor, damit es genau den Platz des früheren Rings einnimmt. Der dunkle Sockel
     * gehört zum Objekt und bleibt; die Fläche ringsum ist durchsichtig und trägt jede Oberfläche.
     */
    @Composable override fun Motiv(modifier: Modifier) {
        Image(
            painter = painterResource(R.drawable.design_sternbahn_frei),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }

    override val zeigtRestzeitRing = true

    @Composable override fun trennfarbe(): Color = LocalGold.current.rahmen
}

