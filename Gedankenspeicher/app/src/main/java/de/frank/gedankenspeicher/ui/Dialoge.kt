package de.frank.gedankenspeicher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Masse
import de.frank.gedankenspeicher.ui.theme.Schriften

/**
 * **Die Rückfrage vor etwas Endgültigem.**
 *
 * Löschen ist in dieser App endgültig — es gibt keinen Papierkorb. Deshalb steht vor jedem
 * Löschen eine Frage, und die Bestätigung trägt die Fehlerfarbe: sie soll nicht aus Versehen
 * getroffen werden.
 */
@Composable
fun Rueckfrage(
    titel: String,
    text: String,
    bestaetigung: String,
    beiJa: () -> Unit,
    beiNein: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    Dialog(onDismissRequest = beiNein) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.gruppeRadius))
                .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
                .padding(20.dp),
        ) {
            Text(titel, style = schrift.kartenUeberschrift, color = farben.textStark)
            Spacer(Modifier.height(8.dp))
            Text(text, style = schrift.einstellungErklaerung, color = farben.textSchwach)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = beiNein) {
                    Text("Abbrechen", style = schrift.knopf, color = farben.textMittel)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = beiJa) {
                    Text(bestaetigung, style = schrift.knopf, color = farben.fehler)
                }
            }
        }
    }
}

/** Ein Dialog mit einem Textfeld — zum Umbenennen einer Sitzung (F-12). */
@Composable
fun Eingabefrage(
    titel: String,
    wert: String,
    beiAenderung: (String) -> Unit,
    beiJa: () -> Unit,
    beiNein: () -> Unit,
) {
    val farben = Farben
    val schrift = Schriften
    Dialog(onDismissRequest = beiNein) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(farben.hintergrundErhoben, RoundedCornerShape(Masse.gruppeRadius))
                .border(1.dp, farben.rand, RoundedCornerShape(Masse.gruppeRadius))
                .padding(20.dp),
        ) {
            Text(titel, style = schrift.kartenUeberschrift, color = farben.textStark)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(farben.hintergrund, RoundedCornerShape(Masse.profilRadius))
                    .border(1.dp, farben.rand, RoundedCornerShape(Masse.profilRadius))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = wert,
                    onValueChange = beiAenderung,
                    textStyle = schrift.eingabefeld.copy(color = farben.textStark),
                    cursorBrush = SolidColor(farben.akzent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = beiNein) {
                    Text("Abbrechen", style = schrift.knopf, color = farben.textMittel)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = beiJa, enabled = wert.isNotBlank()) {
                    Text(
                        "Speichern",
                        style = schrift.knopf,
                        color = if (wert.isNotBlank()) farben.akzent else farben.textSchwach,
                    )
                }
            }
        }
    }
}
