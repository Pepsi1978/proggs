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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.R
import de.frank.wecker.SichtbarerHintergrund
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.gerichteterReflex
import de.frank.genialeideen.ui.theme.innenSchatten
import de.frank.genialeideen.ui.theme.koernung
import de.frank.genialeideen.ui.theme.materialKante
import de.frank.genialeideen.ui.theme.nut
import de.frank.genialeideen.ui.theme.tiefenSchatten
import de.frank.genialeideen.ui.theme.tiefenVerlauf
import de.frank.genialeideen.ui.theme.vignette

/**
 * Die eine Stelle, an der aus [Material] eine Oberfläche wird — für **alle vier** Designs
 * dieselbe. Es gibt hier bewusst keinen Zweig je Design: Die Handschrift steckt vollständig in
 * den Werten aus [LocalMaterial] und in den Radien aus [LocalDesignTokens]. Die einzige
 * Ausnahme ist [Material.nut], weil eine gefräste Innenlinie keine Zahl, sondern eine Schicht ist.
 *
 * **Die Schichtreihenfolge, von unten nach oben** — sie ist nicht beliebig:
 *  1. Außenschatten, **vor** dem Beschnitt; danach schnitte ihn die Form weg.
 *  2. `clip` auf die Form des Designs.
 *  3. Die deckende Palettenfarbe. Pflicht: Jede Schicht darüber ist Weiß- oder Schwarz-Alpha
 *     und bräuchte sonst einen Untergrund, den es nicht gibt (Dialoge stünden durchsichtig da).
 *  4. Der senkrechte Tiefenverlauf.
 *  5. Der gerichtete Reflex — der Unterschied zwischen Glas und bloßer Farbe.
 *  6. Die Körnung, nur auf großen Flächen (Hero, Dialog) und nie in einer Liste.
 *  7. Der Innenschatten, nur bei [Ebene.VERTIEFT].
 *  8. Die Nut, nur wo das Material sie führt.
 *  9. Die Kante **zuletzt**; läge sie früher, überzöge der Reflex sie und sie verschwände.
 * 10. Der Inhalt. Alle Schichten zeichnen mit `onDrawBehind`, deshalb bleibt er scharf.
 *
 * **Leistung:** In scrollenden Listen liegt höchstens **ein** Schatten pro Element und **keine**
 * Körnung. [Ebene.KARTE] ist die Kartenebene der Weckerliste und bekommt deshalb **immer** nur
 * den einfachen Schatten — unabhängig von [inListe], denn die Aufrufstellen in der Liste kommen
 * über die alte Brücke herein und können [inListe] gar nicht setzen.
 */
@Composable
internal fun MaterialFlaeche(
    modifier: Modifier = Modifier,
    ebene: Ebene,
    inListe: Boolean = false,
    /** Ohne Angabe die zur Ebene passende Eckenform des Designs. */
    form: Shape? = null,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    val material = LocalMaterial.current
    val tokens = LocalDesignTokens.current

    val koerperForm = form ?: RoundedCornerShape(
        when (ebene) {
            Ebene.VERTIEFT -> tokens.chipRadius
            Ebene.KNOPF -> tokens.knopfRadius
            else -> tokens.karteRadius
        },
    )
    // Genau die Zuordnung, die vorher fest im Code stand: erhöht → flaecheErhoeht, sonst flaeche.
    // Dadurch verschiebt der Umbau keinen einzigen Farbwert einer bestehenden Aufrufstelle.
    val grund = when (ebene) {
        Ebene.HINTERGRUND -> gold.hintergrund
        Ebene.VERTIEFT -> gold.eingabefeld
        Ebene.ABSCHNITT, Ebene.KARTE -> gold.flaeche
        Ebene.HERO, Ebene.DIALOG, Ebene.KNOPF -> gold.flaecheErhoeht
    }
    val hoehe = when (ebene) {
        Ebene.HINTERGRUND, Ebene.VERTIEFT -> 0.dp
        Ebene.ABSCHNITT -> Hoehe.kontakt
        Ebene.KARTE, Ebene.KNOPF -> Hoehe.karte
        Ebene.HERO -> Hoehe.karteErhoeht
        Ebene.DIALOG -> Hoehe.dialog
    }
    val vertieft = ebene == Ebene.VERTIEFT
    val grossflaeche = ebene == Ebene.HERO || ebene == Ebene.DIALOG
    // Der doppelte Schatten (Kontakt- plus Umgebungsschatten) kostet eine zweite Schattenebene
    // und bleibt deshalb den wenigen großen Flächen vorbehalten, die nie in einer Liste stehen.
    val doppelterSchatten = grossflaeche && !inListe
    val schattenFarbe = material.schattenFarbe ?: Color.Black
    val koernt = grossflaeche && !inListe && material.koernungAlpha > 0f

    Box(
        modifier
            .then(
                when {
                    hoehe <= 0.dp -> Modifier
                    doppelterSchatten -> Modifier.tiefenSchatten(schattenFarbe, hoehe, koerperForm)
                    // Derselbe Umgebungsschatten wie in `tiefenSchatten`, nur ohne den zweiten
                    // Kontaktschatten — beide Wege sehen dadurch nach demselben Material aus.
                    else -> Modifier.shadow(
                        elevation = hoehe,
                        shape = koerperForm,
                        ambientColor = schattenFarbe.copy(alpha = Hoehe.UMGEBUNG_ALPHA),
                        spotColor = schattenFarbe.copy(alpha = 0.55f),
                    )
                },
            )
            .clip(koerperForm)
            .background(grund)
            .tiefenVerlauf(material.tiefenOben, material.tiefenUnten, gedrueckt = vertieft)
            .gerichteterReflex(
                farbe = material.reflexFarbe,
                alpha = material.reflexAlpha,
                winkelGrad = material.reflexWinkelGrad,
                laenge = material.reflexLaenge,
            )
            // Abgefragt statt mit 0 aufgerufen: `koernung` rechnet seine Punktwolke auch bei
            // Deckung 0 aus. Orbit führt bewusst keine Körnung und darf sie nicht bezahlen.
            .then(if (koernt) Modifier.koernung(material.koernungAlpha) else Modifier)
            .then(if (vertieft) Modifier.innenSchatten(koerperForm, material.innenSchattenAlpha) else Modifier)
            .then(if (material.nut) Modifier.nut(koerperForm) else Modifier)
            .then(
                // Der Seitenhintergrund bekommt keine Kante; ein 1-dp-Strich rings um den
                // ganzen Bildschirm wäre ein Rahmen, kein Material.
                if (ebene == Ebene.HINTERGRUND) {
                    Modifier
                } else {
                    // Der deckende Umriss **unter** der Materialkante. Ohne ihn hatte eine weiße
                    // Karte auf hellem Leinen gar keine Begrenzung: Die Materialkante legt nur
                    // Weiß- und Schwarz-Alpha auf, und Weiß auf Weiß ergibt rechnerisch null
                    // Unterschied — unabhängig davon, wie hoch das Alpha steht. Genau dafür ist
                    // `kanteGrund` gedacht; es war gesetzt, wurde aber nie gezeichnet.
                    (material.kanteGrund?.let { grundfarbe ->
                        Modifier.border(1.dp, grundfarbe, koerperForm)
                    } ?: Modifier).border(
                        width = 1.dp,
                        brush = materialKante(
                            lichtFarbe = material.kanteLichtFarbe,
                            lichtAlpha = material.kanteLichtAlpha,
                            schattenAlpha = material.kanteSchattenAlpha,
                            gedrueckt = vertieft,
                        ),
                        shape = koerperForm,
                    )
                },
            ),
    ) { inhalt() }
}

/**
 * Die Fläche, die ihre **Tiefenebene** nennt — die Fassung, die jede neue Aufrufstelle nehmen soll.
 *
 * Sie steht als Erweiterung neben der Schnittstellenmethode, nicht in ihr: Ihr Körper ist für
 * alle vier Gestalten derselbe (er liest Material und Formen aus den CompositionLocals), eine
 * überschreibbare Methode je Gestalt brächte hier also nur vier gleiche Rümpfe.
 *
 * Die alte Fassung `Flaeche(modifier, erhoeht, inhalt)` bleibt als **Brücke** bestehen und bildet
 * `erhoeht = true` auf [Ebene.HERO] und alles andere auf [Ebene.KARTE] ab. Die vorhandenen
 * Aufrufstellen ändern sich dadurch nicht und wählen weiterhin genau die Ebene, die sie meinen.
 *
 * @param inListe für Elemente in einer scrollenden Liste: höchstens ein Schatten, keine Körnung.
 */
@Composable
fun WeckerGestalt.Flaeche(
    modifier: Modifier = Modifier,
    ebene: Ebene,
    inListe: Boolean = false,
    inhalt: @Composable () -> Unit,
) = MaterialFlaeche(modifier = modifier, ebene = ebene, inListe = inListe, inhalt = inhalt)

/** Die Brücke von der alten Ja/Nein-Angabe auf die Tiefenebene. */
private fun ebeneFuer(erhoeht: Boolean): Ebene = if (erhoeht) Ebene.HERO else Ebene.KARTE

/**
 * Schlicht — geschliffenes Glas mit goldener Kante. Der Hintergrund bleibt das gewohnte
 * schwebende Goldlicht ([SichtbarerHintergrund]); die Vignette dazu sitzt in dessen Zeichenebene.
 *
 * Die Fläche geht seit dem Materialumbau denselben Weg wie die anderen drei Designs. Die
 * Goldwerte der Palette bleiben dabei unangetastet: Der frühere Körperverlauf rechnete
 * `heller(a)`/`dunkler(a)` — das ist rechnerisch dasselbe wie Weiß- beziehungsweise
 * Schwarz-Alpha über der Grundfarbe, und nahezu dieselben Werte führt [materialFuer] für
 * Schlicht (dunkel 0,06/0,10 deckungsgleich, hell 0,03/0,05 gegenüber vorher 0,02/0,04).
 * Sichtbar anders sind zwei Dinge, beide gewollt: Die Kante ist jetzt golden statt weiß, und der
 * frühere Schein aus einem festen 700-Pixel-Radius ist der gerichtete Schliff geworden.
 */
object SchlichtGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) = SichtbarerHintergrund()

    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) =
        MaterialFlaeche(modifier = modifier, ebene = ebeneFuer(erhoeht), inhalt = inhalt)

    @Composable override fun Motiv(modifier: Modifier) { /* Schlicht kommt ohne Bildmotiv aus. */ }

    override val zeigtRestzeitRing = true

    @Composable override fun trennfarbe(): Color = LocalGold.current.textGedaempft.copy(alpha = .3f)
}

/**
 * Morgenruhe — mattes Opalglas auf Leinen. Der Hintergrund ist ein sanfter Verlauf statt des
 * schwebenden Goldlichts, dazu eine sehr zurückhaltende Vignette. Das Bettmotiv gehört sichtbar dazu.
 */
object MorgenruheGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) {
        val gold = LocalGold.current
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(gold.hintergrund, gold.flaecheErhoeht.copy(alpha = .55f), gold.hintergrund),
                    ),
                )
                // Die Wölbung, auf der die Flächen erst aufliegen. Zuletzt, damit sie über dem
                // Verlauf liegt und nicht unter ihm verschwindet.
                .vignette(LocalMaterial.current.vignetteAlpha),
        )
    }

    /**
     * Kein eigener Zeichencode mehr: Das breite, weiche Licht von oben (90 Grad, 70 Prozent
     * Länge), die weiße Kante und der gedämpfte statt schwarze Schatten stehen als Werte in
     * [materialFuer] — hier bleibt nur die Ebene übrig.
     */
    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) =
        MaterialFlaeche(modifier = modifier, ebene = ebeneFuer(erhoeht), inhalt = inhalt)

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
 * Traumraum — warmes Rauchglas mit orangefarbener Lichtkante. Stark gerundete Flächen, weicher
 * Schein von oben, die tiefste Vignette der vier Designs. Das Kissenmotiv sitzt im Kopfbereich.
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
                )
                // Die Glutkuppel bleibt oben, die Ecken sinken weg — daher der höchste Wert
                // der vier Designs (28 Prozent im Dunkeln).
                .vignette(LocalMaterial.current.vignetteAlpha),
        )
    }

    /**
     * Das flache Streiflicht (30 Grad, halbe Länge), im Dunkeln in Glut-Orange statt Weiß, und
     * die kräftigste Schattenkante der vier Designs stehen in [materialFuer].
     */
    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) =
        MaterialFlaeche(modifier = modifier, ebene = ebeneFuer(erhoeht), inhalt = inhalt)

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
 * Orbit — kühles Instrumentenglas: kantige Module, hartes schmales Lichtband, eingefräste Nut.
 * Als einziges Design ohne Körnung und mit schwarzem statt farbigem Schatten — gebürstetes
 * Metall reflektiert, es streut nicht.
 */
object OrbitGestalt : WeckerGestalt {
    @Composable override fun Hintergrund(modifier: Modifier) {
        Box(
            Modifier.fillMaxSize()
                .background(LocalGold.current.hintergrund)
                .vignette(LocalMaterial.current.vignetteAlpha),
        )
    }

    /**
     * Die Nut ist die einzige Schicht, die ein Material ein- oder ausschaltet statt sie nur zu
     * gewichten — und Orbit ist das einzige Design, das sie führt ([Material.nut]).
     */
    @Composable override fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit) =
        MaterialFlaeche(modifier = modifier, ebene = ebeneFuer(erhoeht), inhalt = inhalt)

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
