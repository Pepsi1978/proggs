package de.frank.module.transkriptionsmodul

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Einstellungsblock für [ParakeetUeberJarvis]: Modell wählen, Verknüpfung sehen, Wörterbuch schalten und pflegen. */
@Composable
fun ParakeetEinstellungen(modifier: Modifier = Modifier, geaendert: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val p = ParakeetUeberJarvis
    var gewaehlt by remember { mutableStateOf(p.gewaehlt(context)) }
    var woerterbuch by remember { mutableStateOf(p.woerterbuchAn(context)) }
    var gespeichert by remember { mutableStateOf(p.begriffe(context)) }
    var begriffe by remember { mutableStateOf(gespeichert) }
    var verbindung by remember { mutableStateOf<ParakeetUeberJarvis.Verbindung?>(null) }
    LaunchedEffect(gewaehlt) { if (gewaehlt) verbindung = p.pruefeVerbindung(context) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Zeile("Parakeet primeline (offline, über Jarvis)", gewaehlt) {
            gewaehlt = it
            p.gewaehlt(context, it)
            geaendert(it)
        }
        if (gewaehlt) {
            val v = verbindung
            Text(
                when {
                    v == null -> "Verknüpfung wird geprüft …"
                    v.bereit -> "✓ ${v.text}"
                    else -> "✗ ${v.text}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (v == null || v.bereit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Zeile("Wörterbuch nutzen", woerterbuch) {
                woerterbuch = it
                p.woerterbuchAn(context, it)
            }
            Text(
                "Hilft bei Namen und Fachwörtern. Bei zu vielen Fehlern ausschalten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = begriffe,
                onValueChange = { begriffe = it },
                label = { Text("Begriffe (durch Komma oder Zeilenumbruch getrennt)") },
                enabled = woerterbuch,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            if (begriffe != gespeichert) {
                Button(
                    onClick = {
                        p.begriffe(context, begriffe)
                        gespeichert = begriffe
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                ) { Text("Wörterbuch speichern") }
            }
        }
    }
}

@Composable
private fun Zeile(text: String, an: Boolean, wechsel: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = an, onCheckedChange = wechsel)
    }
}
