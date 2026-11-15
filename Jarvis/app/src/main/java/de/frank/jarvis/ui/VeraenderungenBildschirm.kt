package de.frank.jarvis.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.veraenderung.Veraenderung
import de.frank.jarvis.veraenderung.Veraenderungen
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/**
 * Reiter „Veränderungen“: Franks wichtige Veränderungen im Leben. Oben „Aktuell“ (Einzeile, Kreis hakt ab), unten aufklappbar
 * „Erledigt“ (Zurückholen). Antippen öffnet die genaue Beschreibung mit Papierkorb. Neue Einträge kommen nur per
 * Sprache an Jarvis (Werkzeug `veraenderung`).
 */
@Composable
fun VeraenderungenBildschirm() {
    val context = LocalContext.current
    val alle by Veraenderungen.stand.collectAsState()
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Veraenderungen.sicher(context) } }
    var offenId by rememberSaveable { mutableStateOf<Long?>(null) }
    val offen = offenId?.let { id -> alle.firstOrNull { it.id == id } }
    // Auf dem anderen Gerät gelöscht, während der Eintrag hier offen war: zurück zur Liste.
    LaunchedEffect(offenId, offen) { if (offenId != null && offen == null && alle.isNotEmpty()) offenId = null }
    Column(Modifier.fillMaxSize()) {
        if (offen != null) Detail(offen) { offenId = null }
        else Uebersicht(alle) { offenId = it }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Uebersicht(alle: List<Veraenderung>, oeffnen: (Long) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val aktuell = alle.filter { !it.erledigt }
    val erledigt = alle.filter { it.erledigt }.sortedByDescending { it.erledigtAm }
    Text("Veränderungen", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "kopf_aktuell") { Abschnitt("Aktuell") }
        if (aktuell.isEmpty()) item(key = "leer") {
            Karte {
                Text("Noch keine Veränderung", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Sag Jarvis, was du ab jetzt anders machst und warum, etwa: „Hey Jarvis, nimm unter Veränderungen auf, dass ich ab sofort mit dem Sonnenaufgang aufstehe.“",
                    color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        items(aktuell, key = { it.id }) { v -> Zeile(v, oeffnen = { oeffnen(v.id) }) { Veraenderungen.erledigt(context, v.id, true) } }
        if (erledigt.isNotEmpty()) item(key = "erledigt") {
            Bereich("Erledigt", kopf = { Text(erledigt.size.toString(), Modifier.padding(end = 8.dp), color = f.textLeise, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    erledigt.forEach { v ->
                        Row(Modifier.fillMaxWidth().antippen { oeffnen(v.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = f.erfolg, modifier = Modifier.size(22.dp))
                            Text(v.titel, Modifier.weight(1f).padding(start = 12.dp), color = f.textLeise, fontSize = 15.sp, textDecoration = TextDecoration.LineThrough, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.size(42.dp).antippen { Veraenderungen.erledigt(context, v.id, false) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, "In Aktuell zurückholen", tint = f.primaer, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Zeile(v: Veraenderung, oeffnen: () -> Unit, abhaken: () -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().glas(f, 18.dp, erhoeht = 0.7f).antippen(aktion = oeffnen).padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).antippen(aktion = abhaken), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.RadioButtonUnchecked, "Als erledigt abhaken", tint = f.primaer, modifier = Modifier.size(24.dp))
        }
        Text(v.titel, Modifier.weight(1f).padding(vertical = 8.dp), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Detail(v: Veraenderung, zurueck: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    var loeschenFrage by remember { mutableStateOf(false) }
    BackHandler(onBack = zurueck)
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).antippen(aktion = zurueck), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
        }
        Text("Veränderung", Modifier.weight(1f).padding(start = 2.dp), color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Rundknopf(Icons.Rounded.Delete, "Löschen", farbe = f.gefahr) { loeschenFrage = true }
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Karte {
            Text(v.titel, color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Text(
                "Seit ${Veraenderungen.datum(v.erstellt)}" + if (v.erledigt) " · erledigt am ${Veraenderungen.datum(v.erledigtAm)}" else " · aktuell",
                color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Text(v.beschreibung.ifBlank { "Keine Beschreibung." }, color = f.text, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 12.dp))
        }
        if (v.erledigt) Knopf("In Aktuell zurückholen", Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.Undo) { Veraenderungen.erledigt(context, v.id, false) }
        else Knopf("Als erledigt abhaken", Modifier.fillMaxWidth(), Icons.Rounded.CheckCircle) { Veraenderungen.erledigt(context, v.id, true); zurueck() }
    }
    if (loeschenFrage) AlertDialog(
        onDismissRequest = { loeschenFrage = false }, containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Veränderung löschen?") },
        text = { Text("„${v.titel}“ endgültig löschen, auch auf dem anderen Gerät.") },
        confirmButton = { TextButton({ loeschenFrage = false; Veraenderungen.loeschen(context, v.id); zurueck() }) { Text("Löschen", color = f.gefahr) } },
        dismissButton = { TextButton({ loeschenFrage = false }) { Text("Abbrechen", color = f.text) } },
    )
}
