package de.frank.stacklabor.ui.bausteine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Schrift

/**
 * Der Hinweis nach der Konkurrenzprüfung (F-02).
 *
 * Er erscheint **nach** dem Hinzufügen, nicht davor — das Eintippen bleibt flüssig, und
 * niemand wartet auf eine Netzantwort. Wird der Stack vorher verlassen, geht der Hinweis
 * nicht verloren: Er steht am Eintrag in der Datenbank und erscheint beim nächsten Öffnen
 * erneut.
 *
 * Es wird **nichts blockiert**. Die KI kann irren; die Entscheidung bleibt beim Benutzer.
 */
@Composable
fun HinweisSchnipsel(
    sichtbar: Boolean,
    mittelName: String,
    text: String,
    aufBehalten: () -> Unit,
    aufEntfernen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduziert = LocalBewegungReduziert.current
    val dauer = if (reduziert) 0 else Dauer.BLATT
    AnimatedVisibility(
        visible = sichtbar,
        enter = slideInVertically(tween(dauer, easing = Kurven.blatt)) { it } + fadeIn(tween(dauer)),
        exit = slideOutVertically(tween(dauer, easing = Kurven.leit)) { it } + fadeOut(tween(dauer)),
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(Formen.karte)
                .background(Farben.flaeche)
                .border(1.dp, Farben.akzent, Formen.karte)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(mittelName, style = Schrift.grundMittel, color = Farben.text, maxLines = 1)
            Text(text, style = Schrift.klein, color = Farben.textSchwach, maxLines = 3)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                KleinKnopf("Doch entfernen", Farben.rot, aufEntfernen)
                KleinKnopf("Behalten", Farben.akzent, aufBehalten)
            }
        }
    }
}

@Composable
private fun KleinKnopf(beschriftung: String, farbe: androidx.compose.ui.graphics.Color, aufKlick: () -> Unit) {
    Box(
        Modifier
            .height(36.dp)
            .clip(Formen.chip)
            .border(1.dp, farbe, Formen.chip)
            .clickable(onClick = aufKlick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(beschriftung, style = Schrift.klein, color = farbe)
    }
}

/**
 * Die Rückgängig-Leiste nach dem Wischen (M-24).
 *
 * Sie steht **sechs Sekunden** — lange genug, um ein Versehen zu bemerken, kurz genug,
 * um nicht im Weg zu sein.
 */
@Composable
fun RueckgaengigLeiste(
    sichtbar: Boolean,
    text: String = "Entfernt",
    aufRueckgaengig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduziert = LocalBewegungReduziert.current
    val dauer = if (reduziert) 0 else Dauer.BLATT
    AnimatedVisibility(
        visible = sichtbar,
        enter = slideInVertically(tween(dauer, easing = Kurven.blatt)) { it } + fadeIn(tween(dauer)),
        exit = slideOutVertically(tween(dauer, easing = Kurven.leit)) { it } + fadeOut(tween(dauer)),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(Formen.knopf)
                .background(Farben.text)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = Schrift.klein, color = Farben.grund, modifier = Modifier.weight(1f))
            Text(
                "RÜCKGÄNGIG",
                style = Schrift.winzigFett,
                color = Farben.grund,
                modifier = Modifier.clickable(onClick = aufRueckgaengig),
            )
        }
    }
}

/**
 * Das Kontextmenü am Mittel (F-31) — neu aus dem Entwurf.
 *
 * Es öffnet beim langen Drücken in der Ansicht **„Löslichkeit"**, wo nicht gezogen werden
 * kann. Damit gibt es in keiner der beiden Ansichten einen toten Zustand: In „Einnahme"
 * nimmt langes Drücken den Eintrag zum Ziehen auf, hier öffnet es dieses Menü.
 */
@Composable
fun MittelKontextmenue(
    sichtbar: Boolean,
    mittelName: String,
    aufBearbeiten: () -> Unit,
    aufGruppeBilden: () -> Unit,
    aufEntfernen: () -> Unit,
    aufSchliessen: () -> Unit,
) {
    if (!sichtbar) return
    androidx.compose.ui.window.Dialog(onDismissRequest = aufSchliessen) {
        Column(
            Modifier
                .clip(Formen.karte)
                .background(Farben.flaeche)
                .border(1.dp, Farben.rand, Formen.karte)
                .padding(vertical = 8.dp),
        ) {
            Text(
                mittelName,
                style = Schrift.rubrik,
                color = Farben.textSchwach,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            MenueZeile("Bearbeiten", aufBearbeiten)
            MenueZeile("Gruppe bilden", aufGruppeBilden)
            MenueZeile("Entfernen", aufEntfernen, Farben.rot)
        }
    }
}

@Composable
private fun MenueZeile(
    beschriftung: String,
    aufKlick: () -> Unit,
    farbe: androidx.compose.ui.graphics.Color = Farben.text,
) {
    Text(
        beschriftung,
        style = Schrift.grund,
        color = farbe,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = aufKlick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
