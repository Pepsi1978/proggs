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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

/** Breitengrenze: auf dem Telefon füllt der Dialog die Zeile, auf dem aufgeklappten Foldable nicht. */
private val MAX_BREITE = 560.dp

/**
 * Dialograhmen in der Farbwelt und Form des gewählten Designs.
 * [inhalt] ist optional; [bestaetigung] und [abbruch] sind die Knopfzeilen.
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
    inhalt: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val tokens = LocalDesignTokens.current
    val gestalt = LocalGestalt.current
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    // Relativ zur Fensterhöhe statt als feste Zahl: auf dem kleinen Außenschirm des Foldables
    // bleibt derselbe Anteil frei wie auf dem aufgeklappten Gerät.
    val maxHoehe = (LocalConfiguration.current.screenHeightDp * HOEHENANTEIL).dp

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
            .padding(horizontal = 24.dp)
            .widthIn(max = MAX_BREITE)
            .fillMaxWidth()
            .heightIn(max = maxHoehe)
            .graphicsLayer {
                scaleX = skalierung
                scaleY = skalierung
                alpha = deckung
            }

        val koerper: @Composable () -> Unit = {
            DialogKoerper(titel = titel, bestaetigung = bestaetigung, abbruch = abbruch, inhalt = inhalt)
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
                    .padding(horizontal = innen, vertical = 12.dp),
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
