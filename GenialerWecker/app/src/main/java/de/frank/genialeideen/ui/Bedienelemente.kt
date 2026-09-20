package de.frank.genialeideen.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.gerichteterReflex
import de.frank.genialeideen.ui.theme.glanzBogen
import de.frank.genialeideen.ui.theme.innenSchatten
import de.frank.genialeideen.ui.theme.materialKante
import de.frank.genialeideen.ui.theme.nut
import de.frank.genialeideen.ui.theme.tiefenVerlauf
import de.frank.wecker.design.LocalDesignTokens
import de.frank.wecker.design.LocalMaterial
import de.frank.wecker.design.Material

/**
 * Die gemeinsamen Bedienelemente mit echter Materialität — Eingabefeld, Regler, Schalter, Chip.
 *
 * ## Warum es diese Datei gibt
 *
 * Knöpfe hatten mit [Knopf3D] längst ihre Plastik; alles andere war rohes Material3 mit
 * richtigen Farben, aber ohne jede Tiefe. Und weil sich „erhaben" nur gegen etwas Vertieftes
 * lesen lässt, wirkte dadurch auch der Knopf flacher, als er ist. Regler, Felder und Chips
 * stehen in jedem Menü und jedem Untermenü — hier liegt der größte Hebel.
 *
 * ## Die Regel dahinter (Ebene.VERTIEFT aus `Design.kt`)
 *
 * Alle vier Elemente sitzen auf derselben Ebene: **unter** der Kartenoberfläche. Das entsteht
 * nicht durch einen Schatten, sondern durch die umgekehrte Beleuchtung — dunkel oben, hell
 * unten, dazu ein Innenschatten. Ausgenommen sind die beiden Teile, die von Natur aus obenauf
 * liegen: der Reglerknauf und der Schalterknauf, und der gewählte Chip.
 *
 * ## Woher die Werte kommen
 *
 * Ausschließlich aus [LocalMaterial] und [LocalGold]. In dieser Datei steht **kein einziger
 * `when (design)`** — die vier Handschriften entstehen allein aus den Materialwerten. Einzige
 * erlaubte Abfrage ist `material.nut`, weil die Fräsnut eine zeichnende Eigenheit ist und kein
 * Zahlenwert. Jede Überlagerung ist Weiß- oder Schwarz-Alpha beziehungsweise eine Palettenfarbe
 * mit Alpha; `heller()`, `dunkler()` und `koerperVerlauf()` sind hier bewusst **nicht** benutzt,
 * weil sie den Farbton verschieben würden und Schlichts Goldwerte unantastbar sind.
 *
 * ## Zwei Fallen, die hier vermieden sind
 *
 * 1. **Kein Blur.** `Modifier.blur` verwischt nur den eigenen Inhalt, nie den Hintergrund — für
 *    einen Innenschatten ist er nutzlos. Die Tiefe kommt aus [innenSchatten], das mit
 *    abgestuften Konturstrichen arbeitet und deshalb ohne SDK-Abfrage auf minSdk 26 läuft.
 * 2. **Höchstens ein `shadow` je Element.** Diese Elemente stehen in scrollenden Listen;
 *    [de.frank.genialeideen.ui.theme.tiefenSchatten] legt zwei Schattenschichten übereinander
 *    und ist dafür zu teuer. Vertiefte Flächen bekommen gar keinen — was einsinkt, wirft keinen
 *    Schatten nach außen.
 *
 * Alle Zeichenschichten laufen über `onDrawBehind` in [drawWithCache]: Die Pinsel entstehen
 * einmal je Größe, und Text und Symbole bleiben scharf, weil nichts über ihnen liegt.
 */

// --- Die geteilten Schichtfolgen --------------------------------------------------------------

/**
 * Die vertiefte Fläche — die Grundlage von Eingabefeld, Reglerbahn, Schalterbahn und
 * ungewähltem Chip.
 *
 * Reihenfolge von unten nach oben, und die ist nicht beliebig:
 * 1. `clip` — alles Folgende zeichnet mit `drawRect` über die volle Fläche und würde sonst
 *    über die gerundeten Ecken hinauslaufen.
 * 2. **deckende** Grundfarbe — Pflicht. Unter einer halbdurchsichtigen Fläche scheint der
 *    Untergrund durch und aus der Tiefe wird grauer Matsch.
 * 3. Tiefenverlauf, umgekehrt: dunkel oben, hell unten. Das ist der eigentliche Kippschalter
 *    zwischen „erhaben" und „eingelassen".
 * 4. Innenschatten, oben kräftiger als unten.
 * 5. die Nut, wenn das Design sie führt.
 * 6. die Materialkante zuletzt, sonst verschluckt sie die darüberliegende Schicht.
 */
private fun Modifier.vertieftesMaterial(
    form: Shape,
    grund: Color,
    material: Material,
    tiefe: Dp,
): Modifier = this
    .clip(form)
    .background(grund)
    .tiefenVerlauf(material.tiefenOben, material.tiefenUnten, gedrueckt = true)
    .innenSchatten(form, material.innenSchattenAlpha, tiefe = tiefe)
    .then(if (material.nut) Modifier.nut(form, alpha = material.innenSchattenAlpha) else Modifier)
    .border(
        1.dp,
        materialKante(
            material.kanteLichtFarbe, material.kanteLichtAlpha, material.kanteSchattenAlpha,
            gedrueckt = true,
        ),
        form,
    )

/**
 * Die erhabene Fläche — Reglerknauf, Schalterknauf, gewählter Chip.
 *
 * Ohne Schatten: Den setzt der Aufrufer **vor** dem `clip`, damit es bei genau einem bleibt.
 * Der gerichtete Reflex ist das, was hier Glas von bloßer Farbe unterscheidet; seine Werte
 * (Winkel, Länge, Farbe) sind der Punkt, an dem sich die vier Designs am stärksten trennen.
 */
private fun Modifier.erhabenesMaterial(
    form: Shape,
    grund: Color,
    material: Material,
    /**
     * Deckung des Glanzbogens, 0 bedeutet keinen. Er gehört als Parameter hierher und nicht
     * hinter den Aufruf: Modifier zeichnen in der Reihenfolge der Kette, ein nachgestelltes
     * `glanzBogen()` läge also **über** der Materialkante und würde sie aufhellen. In
     * [Knopf3D] steht er aus demselben Grund vor dem `border`.
     */
    glanz: Float = 0f,
): Modifier = this
    .clip(form)
    .background(grund)
    .tiefenVerlauf(material.tiefenOben, material.tiefenUnten)
    .gerichteterReflex(
        material.reflexFarbe, material.reflexAlpha, material.reflexWinkelGrad, material.reflexLaenge,
    )
    .then(if (glanz > 0f) Modifier.glanzBogen(deckung = glanz) else Modifier)
    .then(if (material.nut) Modifier.nut(form, alpha = material.innenSchattenAlpha) else Modifier)
    .border(
        1.dp,
        materialKante(material.kanteLichtFarbe, material.kanteLichtAlpha, material.kanteSchattenAlpha),
        form,
    )

/**
 * Der Eckenradius kleiner Bedienelemente, nach oben auf die halbe Höhe begrenzt.
 *
 * Morgenruhe und Traumraum führen `chipRadius = 999.dp`, also die Kapselform. Ungedeckelt
 * ergäbe das auf einer 12 dp hohen Reglerbahn nichts Sinnvolles; gedeckelt wird daraus genau
 * die Kapsel und bei Orbits 4 dp die kantige Rille. Derselbe Ausdruck, vier Ergebnisse — das
 * ist der Grund, warum hier keine Design-Abfrage steht.
 */
private fun kleinerRadius(radius: Dp, hoehe: Dp): Dp = minOf(radius, hoehe / 2)

// --- 1. Eingabefeld ---------------------------------------------------------------------------

/**
 * Ein [OutlinedTextField], das in der Fläche liegt statt darauf.
 *
 * **Die 8-sp-Falle:** `OutlinedTextField` legt sich bei vorhandener Beschriftung selbst ein
 * `padding(top = OutlinedTextFieldTopPadding)` an — im Material3-Artefakt nachgesehen, es ist
 * `TypeScaleTokens.BodySmallLineHeight / 2`, also **8.sp** und damit von der Schriftgröße des
 * Geräts abhängig, nicht von der Pixeldichte allein. In diesem Streifen schwebt die obere
 * Hälfte der Beschriftung; der gezeichnete Rahmen beginnt erst darunter. Eine Hüllfläche über
 * die vollen Maße stünde deshalb als sichtbarer Sims über dem Rahmen. Die vertiefte Fläche
 * bekommt den Streifen oben abgezogen und deckt sich dadurch genau mit dem Rahmen.
 *
 * **Warum das Feld seinen Rahmen behält:** Material3 schneidet für die schwebende Beschriftung
 * eine Lücke in den Rahmen. Ein selbst gezeichneter Rahmen hätte diese Lücke nicht, und die
 * Beschriftung läge quer über der Linie. Also zeichnet das Feld den Rahmen weiter — nur eben
 * in `rahmen` und bei Fokus in `primaer`, was zugleich die geforderte Lichtkante bei Fokus ist.
 * Der Container ist in allen Zuständen durchsichtig, damit die vertiefte Fläche durchkommt.
 *
 * Der [modifier] des Aufrufers gehört an das Textfeld, nicht an die Hülle: Sonst liefe ein
 * `heightIn(min = 160.dp)` gegen die Hülle und das Feld selbst bliebe einzeilig.
 */
@Composable
fun Eingabefeld(
    wert: String,
    aufAenderung: (String) -> Unit,
    beschriftung: String,
    modifier: Modifier = Modifier,
    einzeilig: Boolean = true,
    aktiviert: Boolean = true,
    istFehler: Boolean = false,
    sichtWandlung: VisualTransformation = VisualTransformation.None,
    tastaturOptionen: KeyboardOptions = KeyboardOptions.Default,
    tastaturAktionen: KeyboardActions = KeyboardActions.Default,
) {
    val gold = LocalGold.current
    val material = LocalMaterial.current
    val tokens = LocalDesignTokens.current
    // Wie in `formenFuer`: Die Kapselform wird für Eingabefelder gedeckelt — ein mehrzeiliges
    // Textfeld als Kapsel wäre weder schön noch lesbar.
    val form = RoundedCornerShape(if (tokens.chipRadius > 16.dp) 12.dp else tokens.chipRadius)
    val absatzOben = with(LocalDensity.current) { 8.sp.toDp() }

    Box {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = absatzOben)
                // Etwas flacher als das Material vorgibt: Die untere Hälfte der Beschriftung
                // sitzt genau auf dem kräftigsten Streifen des Innenschattens und würde sonst
                // an Kontrast verlieren.
                .vertieftesMaterial(form, gold.eingabefeld, material, tiefe = 4.dp),
        )
        OutlinedTextField(
            value = wert,
            onValueChange = aufAenderung,
            modifier = modifier,
            enabled = aktiviert,
            label = { Text(beschriftung) },
            isError = istFehler,
            visualTransformation = sichtWandlung,
            keyboardOptions = tastaturOptionen,
            keyboardActions = tastaturAktionen,
            singleLine = einzeilig,
            shape = form,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = gold.textPrimaer,
                unfocusedTextColor = gold.textPrimaer,
                disabledTextColor = gold.textGedaempft,
                // Durchsichtig in jedem Zustand: Die Fläche kommt aus der Hülle darunter.
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                cursorColor = gold.primaer,
                focusedBorderColor = gold.primaer,
                unfocusedBorderColor = gold.rahmen,
                disabledBorderColor = gold.rahmen,
                focusedLabelColor = gold.primaer,
                unfocusedLabelColor = gold.textGedaempft,
                disabledLabelColor = gold.textGedaempft,
                focusedPlaceholderColor = gold.textGedaempft,
                unfocusedPlaceholderColor = gold.textGedaempft,
            ),
        )
    }
}

// --- 2. Regler3D ------------------------------------------------------------------------------

/** Höhe der Reglerbahn. Schmal genug, dass der Knauf deutlich darüber steht. */
private val BahnHoehe = 12.dp

/** Durchmesser des Reglerknaufs. */
private val KnaufGroesse = 22.dp

/**
 * Ein [Slider] mit eigener Bahn und eigenem Knauf: Die Bahn ist eine eingelassene Rille, der
 * gefüllte Teil liegt in `primaer` darin, der Knauf steht erhaben darüber.
 *
 * Die Überladung mit den Steckplätzen `track` und `thumb` ist im Artefakt von Material3 1.3.1
 * nachgeschlagen; sie trägt `@ExperimentalMaterial3Api`, daher das [OptIn]. Die Reihenfolge der
 * Angaben ist `value, onValueChange, modifier, enabled, onValueChangeFinished, colors,
 * interactionSource, steps, thumb, track, valueRange` — hier durchweg benannt übergeben, damit
 * ein Versionswechsel nicht still die Bedeutung vertauscht.
 *
 * **Wie der Anteil zustande kommt:** `SliderState.coercedValueAsFraction` ist bibliotheksintern
 * und von außen nicht lesbar, deshalb wird der Anteil aus `value` und `valueRange` selbst
 * gerechnet. Das deckt sich mit der Platzierung durch den Slider: Die Bahn bekommt genau die
 * Breite des Laufwegs (Gesamtbreite minus Knaufbreite), der Knauf sitzt bei
 * `Anteil × Bahnbreite`. Gelesen wird `value` bewusst erst **in** `onDrawBehind` — dadurch
 * löst das Ziehen nur ein Neuzeichnen aus, keine Neuzusammensetzung der Bahn.
 *
 * **Keine Teilstriche.** Die Aufrufstelle für den Vorlauf der Schlafenszeit-Erinnerung führt
 * bis zu 119 Stufen; ausgezeichnet wären das 119 Punkte auf einer Fingerbreite. Die Stufen
 * wirken weiter auf das Einrasten, sie werden nur nicht gezeichnet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Regler3D(
    wert: Float,
    aufAenderung: (Float) -> Unit,
    modifier: Modifier = Modifier,
    bereich: ClosedFloatingPointRange<Float> = 0f..1f,
    stufen: Int = 0,
    aktiviert: Boolean = true,
    aufAenderungFertig: (() -> Unit)? = null,
) {
    val gold = LocalGold.current
    val material = LocalMaterial.current
    val tokens = LocalDesignTokens.current
    val reduziert = LocalBewegungReduziert.current
    val quelle = remember { MutableInteractionSource() }

    val bahnRadius = kleinerRadius(tokens.chipRadius, BahnHoehe)
    val bahnForm = RoundedCornerShape(bahnRadius)
    // Der gefüllte Teil bleibt reines `primaer`; ausgegraut wird allein über Alpha, damit auch
    // im gesperrten Zustand kein Ton verschoben wird. Bahn und Knauf teilen sich die Farbe.
    val aktivFarbe = if (aktiviert) gold.primaer else gold.primaer.copy(alpha = 0.38f)
    val schattenFarbe = material.schattenFarbe ?: Color.Black

    Slider(
        value = wert,
        onValueChange = aufAenderung,
        modifier = modifier,
        enabled = aktiviert,
        onValueChangeFinished = aufAenderungFertig,
        interactionSource = quelle,
        steps = stufen,
        thumb = {
            // Beim Ziehen sinkt der Knauf näher an die Bahn: dieselbe Sprache wie beim
            // gedrückten Knopf, nur ohne dessen zweite Schattenschicht.
            val gezogen by quelle.collectIsDraggedAsState()
            val gedrueckt by quelle.collectIsPressedAsState()
            val nah = (gezogen || gedrueckt) && aktiviert
            val schattenHoehe by animateDpAsState(
                targetValue = when {
                    !aktiviert -> 0.dp
                    nah -> 2.dp
                    else -> 6.dp
                },
                animationSpec = Motion.mikro(reduziert),
                label = "knaufhoehe",
            )
            Box(
                Modifier
                    .size(KnaufGroesse)
                    // Der einzige Schatten dieses Elements — und er steht vor dem `clip`,
                    // sonst schnitte ihn die eigene Form weg.
                    .shadow(
                        elevation = schattenHoehe,
                        shape = CircleShape,
                        ambientColor = schattenFarbe,
                        spotColor = schattenFarbe,
                    )
                    .erhabenesMaterial(CircleShape, aktivFarbe, material),
            )
        },
        track = { zustand ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BahnHoehe)
                    .vertieftesMaterial(bahnForm, gold.eingabefeld, material, tiefe = 3.dp)
                    .drawWithCache {
                        // Beide Pinsel entstehen einmal je Größe, nicht je Bild. Der Glanz über
                        // dem gefüllten Teil ist reines Weiß-/Schwarz-Alpha — `primaer` selbst
                        // bleibt unangetastet.
                        val glanz = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = material.tiefenOben),
                                Color.Transparent,
                                Color.Black.copy(alpha = material.tiefenUnten),
                            ),
                        )
                        // Derselbe Radius wie die Rille, nicht fest die halbe Höhe: In Orbit
                        // ist die Bahn kantig, und eine kapselförmige Füllung darin sähe
                        // aufgeklebt aus.
                        val ecke = CornerRadius(bahnRadius.toPx(), bahnRadius.toPx())
                        val anfang = zustand.valueRange.start
                        val spanne = (zustand.valueRange.endInclusive - anfang).takeIf { it != 0f } ?: 1f
                        onDrawBehind {
                            val anteil = ((zustand.value - anfang) / spanne).coerceIn(0f, 1f)
                            val breite = size.width * anteil
                            if (breite <= 0f) return@onDrawBehind
                            // Von rechts füllen, wo von rechts nach links gelesen wird. Der
                            // Slider spiegelt seine eigene Platzierung, die Bahn muss mitziehen.
                            val links = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - breite
                            val masse = Size(breite, size.height)
                            drawRoundRect(aktivFarbe, Offset(links, 0f), masse, ecke)
                            drawRoundRect(glanz, Offset(links, 0f), masse, ecke)
                        }
                    },
            )
        },
        valueRange = bereich,
    )
}

// --- 3. Schalter3D ----------------------------------------------------------------------------

private val SchalterBreite = 52.dp
private val SchalterHoehe = 30.dp
private val SchalterKnauf = 24.dp
private val SchalterRand = 3.dp

/**
 * Ein eigener Schalter: Bahn eingelassen, Knauf erhaben mit Glanzbogen.
 *
 * Der Schalter aus Material3 lässt sich in der Tiefe nicht anpassen — er nimmt Farben entgegen,
 * aber keine Zeichenschichten, und seine Bahn bleibt dadurch eine flache Kapsel. Deshalb hier
 * neu gebaut, mit denselben Farbrollen, die [de.frank.genialeideen.ui.theme.SchalterFarben]
 * schon benutzt: `primaer`/`aufPrimaer` im eingeschalteten, `eingabefeld`/`textPrimaer` im
 * ausgeschalteten Zustand. Keine neue Farbe, nur eine andere Tiefe.
 *
 * **Knauf und Bahn sind Geschwister, nicht Kind und Elternteil.** Läge der Knauf in der
 * beschnittenen Bahn, schnitte deren `clip` seinen Schatten weg und das Erhabene bliebe
 * flach — genau der Fehler, den diese Datei beheben soll.
 *
 * [aufWechsel] darf `null` sein: Dann sitzt die Rolle bei der umgebenden Zeile, wie es `Toggle`
 * mit `Switch(value, null)` seit jeher tut. Nur wenn der Schalter selbst bedient wird, setzt er
 * `Role.Switch` — sonst meldete TalkBack denselben Schalter zweimal.
 */
@Composable
fun Schalter3D(
    an: Boolean,
    aufWechsel: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    aktiviert: Boolean = true,
) {
    val gold = LocalGold.current
    val material = LocalMaterial.current
    val tokens = LocalDesignTokens.current
    val reduziert = LocalBewegungReduziert.current
    val quelle = remember { MutableInteractionSource() }

    val bahnForm = RoundedCornerShape(kleinerRadius(tokens.chipRadius, SchalterHoehe))
    val bahnFarbe = when {
        !aktiviert -> gold.eingabefeld
        an -> gold.primaer
        else -> gold.eingabefeld
    }
    val knaufFarbe = when {
        !aktiviert -> gold.textGedaempft
        an -> gold.aufPrimaer
        else -> gold.textPrimaer
    }
    val schattenFarbe = material.schattenFarbe ?: Color.Black

    // Bei reduzierter Bewegung liefert `Motion.mikro` `tween(0)`: Der Knauf springt sofort auf
    // seinen Platz. Der Endzustand bleibt also in jedem Fall ablesbar, es fehlt nur der Weg.
    val versatz by animateDpAsState(
        targetValue = if (an) SchalterBreite - SchalterKnauf - SchalterRand else SchalterRand,
        animationSpec = Motion.mikro(reduziert),
        label = "schalterknauf",
    )

    Box(
        modifier
            // Die Tippfläche zuerst, damit sie die vollen 48 dp umfasst und nicht nur den
            // 52 × 30 dp großen Körper.
            .then(
                if (aufWechsel != null) {
                    Modifier.toggleable(
                        value = an,
                        interactionSource = quelle,
                        // Ohne eigene Rückmeldung: Der voreingestellte Wellenschlag liefe über
                        // das 52 × 48 dp große Rechteck der Tippfläche und stünde als eckiger
                        // Fleck um den runden Knauf. Die Rückmeldung ist der wandernde Knauf
                        // selbst — dieselbe Entscheidung wie bei [Knopf3D].
                        indication = null,
                        enabled = aktiviert,
                        role = Role.Switch,
                        onValueChange = aufWechsel,
                    )
                } else {
                    Modifier
                },
            )
            .minimumInteractiveComponentSize()
            .size(SchalterBreite, SchalterHoehe),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .vertieftesMaterial(bahnForm, bahnFarbe, material, tiefe = 5.dp),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                // Als Lambda, damit der animierte Wert im Layout gelesen wird und nicht bei
                // jedem Schritt die ganze Zeile neu zusammensetzt.
                .offset { IntOffset(versatz.roundToPx(), 0) }
                .size(SchalterKnauf)
                .then(
                    if (aktiviert) {
                        Modifier.shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = schattenFarbe,
                            spotColor = schattenFarbe,
                        )
                    } else {
                        Modifier
                    },
                )
                // Der Glanzbogen ist beim Schalter das Merkmal, das den Knauf von einem bloß
                // farbigen Kreis unterscheidet — er liegt unter der Kante, nicht darüber.
                .erhabenesMaterial(CircleShape, knaufFarbe, material, glanz = 0.28f),
        )
    }
}

// --- 4. Chip3D --------------------------------------------------------------------------------

/**
 * Ein Auswahlchip, der seine Wahl über die Tiefe zeigt: gewählt = erhaben wie ein kleiner Knopf,
 * nicht gewählt = eingelassen mit Innenschatten.
 *
 * Das ist der Zustand, den Material3 nur über einen Farbwechsel andeutet — und der damit in den
 * flacheren Designs kaum zu sehen war. Über die Tiefe steht er auch dann, wenn die beiden
 * Farben dicht beieinander liegen.
 *
 * **Reihenfolge der Modifier, und warum sie so ist:** Erst `selectable` (volle 48 dp Tippfläche),
 * dann `minimumInteractiveComponentSize`, **erst danach** Schatten, `clip` und Fläche. Läge die
 * Mindestgröße weiter innen — wie bei [StillerKnopf] —, bliese sie den sichtbaren Körper auf
 * 48 dp auf und jede Chip-Reihe würde spürbar höher. So bleibt der Körper bei rund 32 dp, genau
 * wie beim bisherigen `FilterChip`, und die Abstände der `FlowRow` stimmen unverändert.
 *
 * `Role.Checkbox` ist bewusst gewählt: Material3 meldet seinen `FilterChip` genauso, und wer die
 * App mit TalkBack bedient, soll nach der Umstellung nichts anderes hören als vorher.
 */
@Composable
fun Chip3D(
    gewaehlt: Boolean,
    aufTipp: () -> Unit,
    beschriftung: String,
    modifier: Modifier = Modifier,
    aktiviert: Boolean = true,
) {
    val gold = LocalGold.current
    val material = LocalMaterial.current
    val tokens = LocalDesignTokens.current
    val reduziert = LocalBewegungReduziert.current
    val quelle = remember { MutableInteractionSource() }
    val gedrueckt by quelle.collectIsPressedAsState()

    val form = RoundedCornerShape(tokens.chipRadius)
    val schattenFarbe = material.schattenFarbe ?: Color.Black
    val grund = when {
        !aktiviert -> gold.eingabefeld
        gewaehlt -> gold.primaer
        else -> gold.eingabefeld
    }
    val schrift = when {
        !aktiviert -> gold.textGedaempft
        gewaehlt -> gold.aufPrimaer
        else -> gold.textPrimaer
    }

    val skalierung by animateFloatAsState(
        targetValue = if (gedrueckt && aktiviert && !reduziert) 0.96f else 1f,
        animationSpec = Motion.mikro(reduziert),
        label = "chipdruck",
    )

    Box(
        modifier
            .selectable(
                selected = gewaehlt,
                enabled = aktiviert,
                role = Role.Checkbox,
                interactionSource = quelle,
                indication = null,
                onClick = aufTipp,
            )
            .minimumInteractiveComponentSize()
            .graphicsLayer { scaleX = skalierung; scaleY = skalierung }
            .then(
                if (gewaehlt && aktiviert) {
                    // Der gewählte Chip ist das einzige Element hier, das nach außen Schatten
                    // wirft — und auch er nur mit einer Schicht.
                    Modifier.shadow(
                        elevation = 5.dp,
                        shape = form,
                        ambientColor = schattenFarbe,
                        spotColor = schattenFarbe,
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (gewaehlt) {
                    Modifier.erhabenesMaterial(form, grund, material)
                } else {
                    Modifier.vertieftesMaterial(form, grund, material, tiefe = 4.dp)
                },
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = beschriftung,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (gewaehlt) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = schrift,
        )
    }
}
