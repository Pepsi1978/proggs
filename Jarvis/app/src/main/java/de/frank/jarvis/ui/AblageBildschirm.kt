package de.frank.jarvis.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.faehigkeit.Ablage
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import java.io.File

/** Die Ablage von Jarvis: Recherchen und Ausarbeitungen lesen, teilen und löschen. */
@Composable
fun AblageBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val laeufe by Agenten.laeufe.collectAsState()
    // Jede Aktivität (ein Agent wird fertig, etwas wird gelöscht) kann den Bestand ändern: dann neu einlesen.
    val protokoll by Protokoll.eintraege.collectAsState()
    var stand by remember { mutableStateOf(0) }
    val dateien = remember(protokoll.size, stand, laeufe.size) { Ablage.alle(activity) }
    var offen by rememberSaveable { mutableStateOf<String?>(null) }
    var loeschen by remember { mutableStateOf(false) }
    val datei: File? = offen?.let { name -> dateien.firstOrNull { it.name == name } }

    if (datei != null) {
        BackHandler { offen = null; loeschen = false }
        val text = remember(datei.name, datei.lastModified()) { runCatching { datei.readText() }.getOrDefault("") }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rundknopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { offen = null; loeschen = false }
                Text(datei.nameWithoutExtension, Modifier.weight(1f), color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Vorleseknopf(vm, "ablage:" + datei.name, datei.nameWithoutExtension, text)
                Rundknopf(Icons.Rounded.Share, "Teilen") {
                    runCatching {
                        activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, datei.nameWithoutExtension).putExtra(Intent.EXTRA_TEXT, text), "Teilen"))
                    }
                }
                Rundknopf(Icons.Rounded.Delete, "Löschen", farbe = f.gefahr) { loeschen = true }
            }
            if (loeschen) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Knopf("Endgültig löschen", Modifier.weight(1f), farbe = f.gefahr) { datei.delete(); offen = null; loeschen = false; stand++ }
                    Knopf("Behalten", Modifier.weight(1f), haupt = false) { loeschen = false }
                }
            }
            SelectionContainer(Modifier.weight(1f)) {
                Text(text, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), color = f.text, fontSize = 16.sp, lineHeight = 23.sp)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp)) {
            Text("Ablage", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Recherchen und Ausarbeitungen von Jarvis", color = f.textLeise, fontSize = 14.sp)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(laeufe, key = { "lauf" + it.seit }) { lauf ->
                Row(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f, toenung = f.primaer).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Kern(34.dp, f.primaer, aktiv = true)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("${lauf.agent} arbeitet", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(lauf.schritt.ifEmpty { lauf.auftrag }, color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (dateien.isEmpty() && laeufe.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Kern(96.dp, f.primaer, aktiv = false)
                    Text("Noch nichts abgelegt", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                    Text("Sag in ChatGPT zum Beispiel: „Jarvis, lass recherchieren, wie …“ Das Ergebnis erscheint hier.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            items(dateien, key = { it.name }) { d ->
                Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f).antippen { offen = d.name }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(d.nameWithoutExtension, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(Ablage.datum(d) + " · " + (d.length() / 1000 + 1) + " kB", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

/** Kleiner runder Glasknopf mit Symbol, für die Kopfzeilen. */
@Composable
fun Rundknopf(icon: androidx.compose.ui.graphics.vector.ImageVector, beschreibung: String, farbe: androidx.compose.ui.graphics.Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = farbe ?: f.textLeise, modifier = Modifier.size(20.dp))
    }
}
