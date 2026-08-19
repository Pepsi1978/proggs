package de.frank.gedankenspeicher.ui.verlauf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import de.frank.gedankenspeicher.data.KiAntwort
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.blattgrund

/**
 * **Eine Auswertung nachträglich bearbeiten.**
 *
 * Alle drei Texte der Karte stehen hier offen: die Rückfrage der KI, die eigene Antwort
 * darauf und die Auswertung selbst. Die Auswertung liegt dabei so im Feld, wie die KI sie
 * geschrieben hat — mit Strichen, Rauten und `<svg>`-Blöcken. Das ist Absicht: nur an
 * dieser Rohform lässt sich eine Tabellenzeile ändern oder eine Zeichnung entfernen. Beim
 * Speichern wird sie wieder als Reichtext gezeichnet.
 *
 * Der Zeitstempel bleibt unangetastet — er sagt, wann die Auswertung entstand, nicht wann
 * zuletzt daran getippt wurde.
 */
@Composable
fun AntwortBearbeitenBlatt(
    antwort: KiAntwort,
    beiAbbrechen: () -> Unit,
    beiSpeichern: (KiAntwort) -> Unit,
) {
    val farben = Farben
    val schrift = Schriften

    var rueckfrage by remember(antwort.id) { mutableStateOf(antwort.rueckfrage) }
    var meine by remember(antwort.id) { mutableStateOf(antwort.antwortDesNutzers) }
    var text by remember(antwort.id) { mutableStateOf(antwort.text) }

    val geaendert = rueckfrage != antwort.rueckfrage ||
        meine != antwort.antwortDesNutzers ||
        text != antwort.text

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                farben.blattgrund,
                RoundedCornerShape(topStart = Masse.blattRadius, topEnd = Masse.blattRadius),
            )
            .border(
                1.dp,
                farben.rand,
                RoundedCornerShape(topStart = Masse.blattRadius, topEnd = Masse.blattRadius),
            )
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = Masse.seitenrand),
    ) {
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(32.dp).height(4.dp).background(farben.rand, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Abbrechen",
                style = schrift.knopf,
                color = farben.textMittel,
                modifier = Modifier.clickable(onClick = beiAbbrechen).padding(vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text("Auswertung bearbeiten", style = schrift.kartenUeberschrift, color = farben.textStark)
            Spacer(Modifier.weight(1f))
            Text(
                "Speichern",
                style = schrift.knopf,
                // Ist nichts geändert, ist Speichern ausgegraut und wirkungslos.
                color = if (geaendert) farben.akzent else farben.textSchwach,
                modifier = Modifier
                    .clickable(enabled = geaendert) {
                        beiSpeichern(
                            antwort.copy(
                                rueckfrage = rueckfrage.trim(),
                                antwortDesNutzers = meine.trim(),
                                text = text.trim(),
                            ),
                        )
                    }
                    .padding(vertical = 8.dp),
            )
        }

        Column(
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))
            Beschriftung("Die Rückfrage der KI")
            Feld(wert = rueckfrage, beiAenderung = { rueckfrage = it }, mindesthoehe = 60.dp)

            Spacer(Modifier.height(14.dp))
            Beschriftung("Deine Antwort darauf")
            Feld(wert = meine, beiAenderung = { meine = it }, mindesthoehe = 60.dp)

            Spacer(Modifier.height(14.dp))
            Beschriftung("Die Auswertung")
            Feld(wert = text, beiAenderung = { text = it }, mindesthoehe = 220.dp)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Beschriftung(text: String) = Text(
    text,
    style = Schriften.einstellung,
    color = Farben.textMittel,
    modifier = Modifier.padding(bottom = 6.dp),
)

@Composable
private fun Feld(wert: String, beiAenderung: (String) -> Unit, mindesthoehe: androidx.compose.ui.unit.Dp) {
    val farben = Farben
    val schrift = Schriften
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = mindesthoehe)
            .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.profilRadius))
            .border(1.dp, farben.rand, RoundedCornerShape(Masse.profilRadius))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = wert,
            onValueChange = beiAenderung,
            textStyle = schrift.eingabefeld.copy(color = farben.textStark),
            cursorBrush = SolidColor(farben.akzent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
