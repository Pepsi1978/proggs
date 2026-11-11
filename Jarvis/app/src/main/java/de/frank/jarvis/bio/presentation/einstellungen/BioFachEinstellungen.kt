package de.frank.jarvis.bio.presentation.einstellungen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.jarvis.bio.presentation.components.GlassCard
import de.frank.jarvis.bio.presentation.theme.LocalCosmos
import de.frank.jarvis.faehigkeit.EinstellungsKatalog

/**
 * Die fachlichen Biomarker-Einstellungen (Quellen an oder pausiert, Abruf beim Öffnen und vor der Auswertung, Wartezeit,
 * Vergleichszeitraum, Warnung bei alten Werten, ausgelassene Messgrößen) und die Zahl der automatischen Tagesauswertungen.
 * Gebaut direkt aus dem [EinstellungsKatalog]: Oberfläche und Sprache lesen und setzen dieselben Werte mit denselben Folgen.
 */
@Composable
fun BioFachEinstellungen(modifier: Modifier) {
    val app = LocalContext.current.applicationContext
    val cosmos = LocalCosmos.current
    val katalog = remember { EinstellungsKatalog(app) }
    val eintraege = remember {
        katalog.eintraege.filter { it.bereich == "Biomarker" || it.kennung in setOf("auswertung_pro_tag", "auswertung_frueheste", "auswertung_abstand") }
    }
    // Nach jeder Änderung neu lesen.
    var stand by remember { mutableIntStateOf(0) }
    var meldung by remember { mutableStateOf("") }
    fun setze(kennung: String, wert: String) {
        meldung = katalog.setze(kennung, wert).second
        stand++
    }
    GlassCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Abruf und Auswertung", color = cosmos.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Frische Daten holt Jarvis nur beim Öffnen dieses Bereichs, auf ausdrücklichen Wunsch und vor jeder Tagesauswertung. Alles hier lässt sich auch per Sprache ändern.",
                color = cosmos.textSecondary, style = MaterialTheme.typography.bodySmall,
            )
            eintraege.forEach { e ->
                @Suppress("UNUSED_EXPRESSION") stand
                val wert = e.lesen()
                Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.titel, color = cosmos.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(e.beschreibung, color = cosmos.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        when {
                            e.werte == "an oder aus" -> Switch(checked = wert == "an", onCheckedChange = { setze(e.kennung, if (it) "an" else "aus") })
                            Regex("^-?\\d+ bis -?\\d+").containsMatchIn(e.werte) -> {
                                val zahl = Regex("-?\\d+").find(wert)?.value?.toIntOrNull() ?: 0
                                val schritt = when (e.kennung) { "bio_wartezeit" -> 30; "bio_vergleich_tage" -> 7; else -> 1 }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { setze(e.kennung, (zahl - schritt).toString()) }) { Text("–") }
                                    Text(wert, color = cosmos.textPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                                    OutlinedButton(onClick = { setze(e.kennung, (zahl + schritt).toString()) }) { Text("+") }
                                }
                            }
                            else -> Unit
                        }
                    }
                    if (e.werte != "an oder aus" && !Regex("^-?\\d+ bis -?\\d+").containsMatchIn(e.werte)) {
                        var text by remember(e.kennung, stand) { mutableStateOf(wert) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(text, { text = it }, Modifier.weight(1f), singleLine = true)
                            OutlinedButton(onClick = { setze(e.kennung, text) }, Modifier.padding(start = 8.dp)) { Text("Übernehmen") }
                        }
                    }
                }
            }
            if (meldung.isNotEmpty()) Text(meldung, color = cosmos.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
