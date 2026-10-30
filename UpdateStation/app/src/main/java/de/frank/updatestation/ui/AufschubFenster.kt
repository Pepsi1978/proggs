package de.frank.updatestation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Fragt vor jeder Jarvis-Installation, wann sie starten soll; [onWahl] bekommt die Minuten, 0 = sofort. */
@Composable
fun AufschubFenster(onWahl: (Int) -> Unit, onSchliessen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSchliessen,
        confirmButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Eine neue Jarvis-Version kann installiert werden. Jetzt installieren?")
                (0..5).chunked(2).forEach { reihe ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reihe.forEach { minuten ->
                            val breite = Modifier.weight(1f)
                            val rand = PaddingValues(horizontal = 4.dp)
                            if (minuten == 0) {
                                Button({ onWahl(0) }, breite, contentPadding = rand) { Knopftext("Sofort") }
                            } else {
                                val text = if (minuten == 1) "In 1 Minute" else "In $minuten Minuten"
                                OutlinedButton({ onWahl(minuten) }, breite, contentPadding = rand) { Knopftext(text) }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun Knopftext(text: String) = Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
