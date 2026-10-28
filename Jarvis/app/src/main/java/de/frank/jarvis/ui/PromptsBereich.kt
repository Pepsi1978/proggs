package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.ablage.Prompt
import de.frank.jarvis.ablage.Prompts
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Filterwert des Reiters „Prompts“ in der Ablage (kein Ablage-Kategorie-Name). */
const val PROMPTS_REITER = "PROMPTS"

/**
 * Reiter „Prompts“ der Ablage: nummerierte, vorformulierte Aufträge an Jarvis. Nach dem letzten steht „+“ (eintippen oder
 * einsprechen, KI bringt den Text in gutes Deutsch, Diskette speichert). Antippen öffnet den Prompt; „Senden“ startet
 * eine neue Programmier-Session mit dem Prompt in der Eingabeblase, „Bearbeiten“ öffnet ihn in derselben Eingabe.
 */
@Composable
fun PromptsBereich(vm: AppViewModel, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val bereich = rememberCoroutineScope()
    val stand by Prompts.stand.collectAsState()
    val liste by ladeImHintergrund<List<Prompt>?>(null, stand, behalten = true) { Prompts.alle(context) }
    var offen by rememberSaveable { mutableStateOf<Int?>(null) }
    var neu by rememberSaveable { mutableStateOf(false) }
    val defekt by ladeImHintergrund(false, stand) { withContext(Dispatchers.IO) { Prompts.defekt(context) } }

    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (defekt) item(key = "defekt") {
            Text("Die Prompt-Datei ist beschädigt und wurde als prompts.json.defekt gesichert. Jarvis speichert dort nichts Neues, bis sie repariert ist; Details unter Aktivität.",
                color = f.gefahr, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
        } else if (liste?.isEmpty() == true) item(key = "leer") {
            Text("Noch keine Prompts. Tippe auf +, oder sag Jarvis: „Speicher folgendes als Prompt ab: …“", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
        items(liste.orEmpty(), key = { it.nummer }) { p ->
            Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f).antippen { offen = p.nummer }.padding(16.dp)) {
                Text("Prompt ${p.nummer}", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(p.text, color = f.text, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item(key = "plus") {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).knopf3d(f.primaer, f.tertiaer, 28.dp, f.dunkel).antippen { neu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "Neuer Prompt", tint = f.aufPrimaer, modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (neu || vm.promptBearbeitet != null) PromptEingabe(vm, schliessen = { vm.promptEingabeSchliessen(); neu = false }) { vm.promptSpeichern { neu = false } }

    liste?.firstOrNull { it.nummer == offen }?.let { p ->
        var loeschen by remember(p.nummer) { mutableStateOf(false) }
        Dialog({ offen = null }, DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.padding(16.dp).fillMaxWidth().glas(f, fuellung = f.flaecheStark).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prompt ${p.nummer}", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp).antippen { offen = null }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
                }
                Text(p.text, color = f.text, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(vertical = 10.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (loeschen) {
                        Knopf("Wirklich löschen", Modifier.weight(1f), Icons.Rounded.Delete, farbe = f.gefahr) {
                            bereich.launch {
                                val weg = withContext(Dispatchers.IO) { Prompts.loesche(context, p.nummer) }
                                vm.meldung = if (weg != null) "Prompt ${p.nummer} gelöscht." else "Prompt ${p.nummer} ließ sich nicht löschen."
                                if (weg != null) offen = null
                            }
                        }
                        Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { loeschen = false }
                    } else {
                        Knopf("Löschen", Modifier.weight(1f), Icons.Rounded.Delete, haupt = false, farbe = f.gefahr) { loeschen = true }
                        Knopf("Bearbeiten", Modifier.weight(1f), Icons.Rounded.Edit, haupt = false) { offen = null; vm.promptBearbeiten(p) }
                        // Rechts, gut mit dem Daumen erreichbar.
                        Knopf("Senden", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Send) { offen = null; vm.promptSenden(p.text) }
                    }
                }
            }
        }
    }
}

/** Eingabe eines neuen oder zu bearbeitenden Prompts: eintippen oder einsprechen, KI-Knopf formuliert in gutes Deutsch (rückgängig machbar), Diskette speichert. */
@Composable
private fun PromptEingabe(vm: AppViewModel, schliessen: () -> Unit, speichern: () -> Unit) {
    val f = LocalFarben.current
    Dialog(schliessen, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(16.dp).fillMaxWidth().glas(f, fuellung = f.flaecheStark).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.promptBearbeitet?.let { "Prompt $it bearbeiten" } ?: "Neuer Prompt", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp).antippen(aktion = schliessen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
            }
            Eingabe(vm.promptEntwurf, { vm.promptEntwurf = it }, if (vm.nimmtAuf) "Ich höre zu … tippe aufs Mikrofon zum Übernehmen" else if (vm.schreibtMit) "Schreibe mit …" else "Prompt eintippen oder einsprechen",
                Modifier.fillMaxWidth().padding(vertical = 12.dp), einzeilig = false)
            if (vm.promptFormuliert) Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Kern(28.dp, f.primaer, aktiv = true)
                Text("Jarvis formuliert …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Rund(if (vm.nimmtAuf) Icons.Rounded.Stop else Icons.Rounded.Mic, if (vm.nimmtAuf) "Aufnahme beenden" else "Einsprechen", if (vm.nimmtAuf) f.gefahr else f.primaer) {
                    vm.promptMikrofonTippen()
                }
                if (vm.promptVorKi != null) Knopf("Rückgängig", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Undo, haupt = false) { vm.promptKiRueckgaengig() }
                else Knopf("KI", Modifier.weight(1f), Icons.Rounded.AutoAwesome, haupt = false) { vm.promptFormulieren() }
                Rund(Icons.Rounded.Save, "Speichern", f.primaer) { speichern() }
            }
        }
    }
}

@Composable
private fun Rund(icon: androidx.compose.ui.graphics.vector.ImageVector, beschreibung: String, farbe: androidx.compose.ui.graphics.Color, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(52.dp).knopf3d(farbe, if (farbe == f.primaer) f.tertiaer else farbe, 26.dp, f.dunkel).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = f.aufPrimaer, modifier = Modifier.size(24.dp))
    }
}
