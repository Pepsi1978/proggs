@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.genialeideen.ui.GoldKarte
import de.frank.genialeideen.ui.theme.IdeenSchriftFest
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion

/*
 * Warum ein eigener Dialograhmen statt des Material3-`AlertDialog`:
 *
 * `AlertDialog` zieht seine Containerfarbe aus der Farbrolle `surfaceContainerHigh`. Diese Rolle
 * wird in `GenialeIdeenTheme` nicht gesetzt — deshalb erscheint jeder Dialog im Material-Baseline-
 * Grau statt in der Farbe des gewählten Designs, und mit der festen 28-dp-Rundung statt in der Form
 * des Designs. Beides lässt sich nur durch einen eigenen Rahmen beheben; jeder Dialog trägt damit
 * dieselbe Fläche und dieselbe Kantenform wie die Karten daneben.
 *
 * Gebaut auf `androidx.compose.ui.window.Dialog` — bewusst nicht auf dem experimentellen
 * `BasicAlertDialog`, dessen Zuschnitt sich zwischen Material3-Versionen noch ändert.
 */

/** Anteil der Fensterhöhe, den ein Dialog höchstens einnimmt. Der Rest bleibt sichtbar frei. */
private const val HOEHENANTEIL = 0.82f

/**
 * Luft zwischen Dialogkante und Systemleiste — oben wie unten. Bewusst knapp: Jedes Millimeter
 * hier fehlt dem Inhalt, und der Monatskalender in Picker.kt braucht seine 512 dp am Stück.
 */
private val RAND_LUFT = 8.dp

/** Breitengrenze: auf dem Telefon füllt der Dialog die Zeile, auf dem aufgeklappten Foldable nicht. */
internal val DIALOG_MAX_BREITE = 560.dp

/**
 * Der übliche waagerechte Außenabstand des Rahmens — Rand zum Bildschirmrand, kein Innenabstand.
 *
 * Steht hier als einzige Quelle, weil Picker.kt mit derselben Zahl rechnet und sie für seine beiden
 * Dialoge auch unterschreiten darf (siehe [DesignDialog] `randSeitlich`). Zwei gespiegelte Konstanten
 * in zwei Dateien liefen sonst irgendwann auseinander.
 */
internal val DIALOG_RAND_SEITLICH = 24.dp

/**
 * Die Höhe, auf die sich ein Dialograhmen deckelt — zwei Grenzen, die kleinere gewinnt:
 *
 * 1. [anteil] der Fensterhöhe. Das ist die Wunschhöhe: Bei der Vorgabe 0,82 bleibt sichtbar Rand
 *    frei, der Dialog wirkt als Dialog. Ein Aufrufer, der den Platz wirklich braucht, hebt den
 *    Anteil an (die Picker in Picker.kt fordern mit 1,0 alles an, was Grenze 2 hergibt).
 * 2. Die **harte** Obergrenze: Fensterhöhe − Systemleisten − 2 × [RAND_LUFT]. Sie greift im Querformat
 *    und auf kleinen Schirmen und verhindert, dass der Rahmen über den Bildschirm hinauswächst.
 *
 * Warum die Systemleisten überhaupt abgezogen werden: Die App läuft mit `enableEdgeToEdge` und
 * targetSdk 36. Ab API 35 schließt `Configuration.screenHeightDp` die Systemleisten **nicht mehr**
 * aus (Android-15-Verhaltensänderung; Google verweist für Maße seither auf `WindowMetrics`) — der
 * Wert ist die volle Fensterhöhe. Auf älteren Geräten zieht dieser Aufruf sie ein zweites Mal ab;
 * das kostet etwas Höhe, kann aber nie über den Rand laufen. Die Richtung ist bewusst so gewählt.
 *
 * **Diese Funktion gehört in die Komposition des App-Fensters, nicht in die eines `Dialog`.**
 * Der Dialog trägt `decorFitsSystemWindows = true`, sein Fenster ist also bereits um die
 * Systemleisten eingerückt und meldet innen 0 dp. Stünde der Aufruf dort, läse er andere Werte als
 * `dialogInhaltsHoehe()` in Picker.kt, das ihn aus dem App-Fenster heraus aufruft — Rahmen und
 * Schwellenrechnung lägen auseinander.
 */
@Composable
internal fun dialogHoechstHoehe(anteil: Float = HOEHENANTEIL): Dp {
    val dichte = LocalDensity.current
    val leisten = WindowInsets.systemBars
    val leistenHoehe = with(dichte) { (leisten.getTop(this) + leisten.getBottom(this)).toDp() }
    val fenster = LocalConfiguration.current.screenHeightDp.dp
    val obergrenze = (fenster - leistenHoehe - RAND_LUFT * 2).coerceAtLeast(0.dp)
    return minOf(fenster * anteil, obergrenze)
}

/**
 * Dialograhmen in der Farbwelt und Form des gewählten Designs.
 * [inhalt] ist optional; [bestaetigung] und [abbruch] sind die Knopfzeilen.
 *
 * [hoehenAnteil] ist der Anteil der Fensterhöhe, den der Rahmen anfordert. Ohne Angabe bleibt es
 * beim gewohnten [HOEHENANTEIL]; Aufrufer mit einem Inhalt, der nicht schrumpfen kann, dürfen mehr
 * verlangen. Die harte Obergrenze nahe der Fensterhöhe gilt weiterhin — siehe
 * [dialogHoechstHoehe]. Der Parameter steht **vor** [inhalt], damit ein nachgestelltes
 * Inhalts-Lambda (wie in [DesignTextDialog]) weiterhin auf [inhalt] fällt.
 *
 * [randSeitlich] ist der Abstand zum Bildschirmrand, [inhaltRandSeitlich] überschreibt — nur für die
 * Inhaltsspalte — den designabhängigen Innenabstand. Beide haben Standardwerte; für alle
 * bestehenden Aufrufer ändert sich damit nichts. Gebraucht werden sie von den Pickern in Picker.kt:
 * Der Monatskalender von Material 3 ist mit 360 dp breiter, als ein 360-dp-Telefon nach Abzug des
 * gewohnten Rahmens (2 × 24 dp Rand + 2 × 20 dp innen = 88 dp) übrig lässt, und er schrumpft nicht.
 * Er gibt deshalb genau so viel Rahmen ab, wie er für seine Breite braucht — mehr nicht.
 *
 * Wichtig: Titelzeile und Knopfzeile behalten ihren gewohnten Innenabstand. Nur so bleibt die
 * Höhenrechnung in `dialogInhaltsHoehe()` (Picker.kt) gültig, die genau diese beiden Zeilen abzieht.
 *
 * Die Scrollhoheit bleibt beim Aufrufer: [inhalt] bekommt eine nach oben begrenzte Spalte, darf
 * darin selbst eine `LazyColumn` aufspannen. Der Rahmen legt **kein** eigenes `verticalScroll`
 * darüber — sonst stünde gleichachsiges Scrollen ineinander und die Messung liefe in
 * „infinity constraints" (siehe Bug-Almanach jetpack-compose §6.1).
 */
@Composable
fun DesignDialog(
    titel: String,
    aufSchliessen: () -> Unit,
    bestaetigung: @Composable () -> Unit,
    abbruch: (@Composable () -> Unit)? = null,
    hoehenAnteil: Float = HOEHENANTEIL,
    randSeitlich: Dp = DIALOG_RAND_SEITLICH,
    inhaltRandSeitlich: Dp? = null,
    inhalt: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val tokens = LocalDesignTokens.current
    val gestalt = LocalGestalt.current
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    // Relativ zur Fensterhöhe statt als feste Zahl: auf dem kleinen Außenschirm des Foldables
    // bleibt derselbe Anteil frei wie auf dem aufgeklappten Gerät.
    //
    // Der Aufruf steht hier oben mit Absicht — außerhalb des `Dialog`-Lambdas darunter. Im
    // Dialogfenster selbst melden die Systemleisten 0 dp; die Zahl wäre eine andere als die, mit
    // der Picker.kt seine Schwellen rechnet. Nicht nach unten verschieben.
    val maxHoehe = dialogHoechstHoehe(hoehenAnteil)

    Dialog(
        onDismissRequest = aufSchliessen,
        // Die Plattformbreite würde den Dialog auf dem aufgeklappten Foldable in einen schmalen
        // Streifen zwängen; die Breite wird deshalb unten selbst begrenzt.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Kurzer Auftritt: leicht heranwachsen und einblenden, danach Ruhe. Bei reduzierter
        // Bewegung steht der Dialog sofort da — kein erster Bildpunkt bei 0,92 und Deckung 0.
        var sichtbar by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { sichtbar = true }
        val offen = sichtbar || reduziert
        val tempo = tween<Float>(if (reduziert) 0 else Motion.ZUSTAND_MS)
        val skalierung by animateFloatAsState(if (offen) 1f else 0.92f, tempo, label = "dialogskalierung")
        val deckung by animateFloatAsState(if (offen) 1f else 0f, tempo, label = "dialogdeckung")

        // Reihenfolge ist wichtig: Der Außenabstand steht zuerst (er ist Rand, nicht Innenabstand),
        // danach begrenzt `widthIn` die Restbreite — erst dann darf `fillMaxWidth` sie ausfüllen.
        // Umgekehrt käme `fillMaxWidth` mit einer festen Breite an und `widthIn` bliebe wirkungslos.
        val rahmen = Modifier
            .padding(horizontal = randSeitlich)
            .widthIn(max = DIALOG_MAX_BREITE)
            .fillMaxWidth()
            .heightIn(max = maxHoehe)
            .graphicsLayer {
                scaleX = skalierung
                scaleY = skalierung
                alpha = deckung
            }

        val koerper: @Composable () -> Unit = {
            DialogKoerper(
                titel = titel,
                bestaetigung = bestaetigung,
                abbruch = abbruch,
                inhaltRandSeitlich = inhaltRandSeitlich,
                inhalt = inhalt,
            )
        }

        // Das Dialogfenster selbst ist durchsichtig. Deckend wird die Fläche durch die Gestalt:
        // `GoldKarte` füllt mit einem Verlauf aus `gold.flaecheErhoeht`, `Flaeche` füllt flach mit
        // derselben Farbe. Alle Paletten (Farben.kt und Paletten.kt) führen `flaeche` und
        // `flaecheErhoeht` als volldeckende 0xFF-Farben, und der weiche Palettenwechsel in
        // `GenialeIdeenTheme` behält den Alphawert bei — hinter dem Inhalt scheint also nichts durch.
        if (tokens.plastisch) {
            // Schlicht behält den gewohnten Gold-Look mit Verlauf, Lichtkante und Tiefenschatten.
            GoldKarte(modifier = rahmen, erhoeht = true, inhalt = koerper)
        } else {
            // Die deckende Grundfarbe liegt hier ausdrücklich in der Eckenform des Designs
            // (`karteRadius`) unter der Gestalt — damit hängt die Deckung nicht davon ab, womit
            // eine Gestalt ihre Fläche intern füllt. Beim plastischen Schlicht bleibt das weg:
            // darüber läge es unter dem Tiefenschatten, darunter würde es den Goldverlauf zudecken.
            gestalt.Flaeche(
                modifier = rahmen.background(gold.flaecheErhoeht, RoundedCornerShape(tokens.karteRadius)),
                erhoeht = true,
                inhalt = koerper,
            )
        }
    }
}

/** Kurzform für reine Text-Dialoge. */
@Composable
fun DesignTextDialog(
    titel: String,
    text: String,
    aufSchliessen: () -> Unit,
    bestaetigung: @Composable () -> Unit,
    abbruch: (@Composable () -> Unit)? = null,
) {
    val gold = LocalGold.current
    DesignDialog(
        titel = titel,
        aufSchliessen = aufSchliessen,
        bestaetigung = bestaetigung,
        abbruch = abbruch,
    ) {
        // Nur hier wird gescrollt: Der Rahmen kennt den Inhalt, es ist genau ein Text und kein
        // eigener Scroller des Aufrufers — verschachteltes gleichachsiges Scrollen kann nicht
        // entstehen. Ein langer Text läuft damit innerhalb der Maximalhöhe durch.
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
            // Bewusst der kräftige Textton statt des gedämpften: Im Dialog steht die Erklärung
            // allein und muss auch bei großer Systemschrift gut lesbar bleiben.
            color = gold.textPrimaer,
        )
    }
}

/**
 * Titel, Inhalt und Knopfzeile innerhalb der Designfläche.
 *
 * Der Aufbau folgt bewusst dem, was der Material3-`AlertDialog` intern tut: Titel und Knopfzeile
 * werden zuerst gemessen, der Inhalt bekommt mit `weight(1f, fill = false)` nur den Rest und
 * schrumpft auf seine eigene Höhe. Ohne dieses Gewicht würde eine lange Liste die ganze Höhe
 * schlucken und die Knöpfe unten aus dem Dialog schieben.
 */
@Composable
private fun DialogKoerper(
    titel: String,
    bestaetigung: @Composable () -> Unit,
    abbruch: (@Composable () -> Unit)?,
    inhaltRandSeitlich: Dp?,
    inhalt: (@Composable ColumnScope.() -> Unit)?,
) {
    val gold = LocalGold.current
    val design = LocalDesignTokens.current.design
    val orbit = design == Design.ORBIT
    // Orbit arbeitet wie seine Module mit knappem Rand; die anderen Designs geben dem Text Luft.
    val innen = if (orbit) 14.dp else 20.dp

    Column(Modifier.fillMaxWidth()) {
        DialogTitel(titel = titel, design = design, innen = innen)
        // Die Trennlinie unter der Kopfzeile ist das Erkennungsmerkmal der Orbit-Instrumententafel.
        if (orbit) HorizontalDivider(color = gold.rahmen)

        if (inhalt != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    // Nur der Inhalt darf seinen seitlichen Abstand überschreiben; der senkrechte
                    // bleibt, damit die Höhenrechnung in Picker.kt weiterhin stimmt.
                    .padding(horizontal = inhaltRandSeitlich ?: innen, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = inhalt,
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // FlowRow statt Row: Bei großer Systemschrift rutscht die Bestätigung in eine eigene Zeile,
        // statt abgeschnitten zu werden. Der Abbruch steht links, die Bestätigung ganz rechts.
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = innen, end = innen, top = 4.dp, bottom = innen),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            abbruch?.invoke()
            bestaetigung()
        }
    }
}

/**
 * Die Titelzeile in der Typografie des jeweiligen Designs — dieselbe Unterscheidung, die
 * `Section` und `DesignKopfleiste` in WeckerUi.kt treffen, damit der Dialog nicht aus der Reihe
 * fällt. Für TalkBack ist der Titel als Überschrift ausgezeichnet.
 */
@Composable
private fun DialogTitel(titel: String, design: Design, innen: androidx.compose.ui.unit.Dp) {
    val gold = LocalGold.current
    val orbit = design == Design.ORBIT
    val (stil: TextStyle, farbe: Color) = when (design) {
        // Orbit: Versalien in der Festbreitenschrift mit Sperrung, genau wie die Kopfleiste.
        Design.ORBIT ->
            MaterialTheme.typography.labelLarge.copy(fontFamily = IdeenSchriftFest, letterSpacing = 1.5.sp) to gold.primaer
        // Traumraum: großer, weicher Titel im kräftigen Textton.
        Design.TRAUMRAUM ->
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) to gold.textPrimaer
        // Morgenruhe: ruhig und ohne Farbakzent. Bewusst kräftiger als der Abschnittstitel in
        // `Section` — dort steht die Überschrift außerhalb der Karte, hier trägt sie den Dialog.
        Design.MORGENRUHE ->
            MaterialTheme.typography.titleMedium to gold.textPrimaer
        // Schlicht: unverändert der bisherige Abschnittstitel in Gold.
        Design.SCHLICHT ->
            MaterialTheme.typography.titleMedium to gold.primaer
    }
    Text(
        text = if (orbit) titel.uppercase(java.util.Locale.GERMAN) else titel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = innen, end = innen, top = if (orbit) 12.dp else innen, bottom = if (orbit) 12.dp else 0.dp)
            .semantics { heading() },
        style = stil,
        color = farbe,
    )
}
