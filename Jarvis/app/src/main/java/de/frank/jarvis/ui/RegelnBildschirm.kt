package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.knopf3d

/**
 * Franks Regeln im Vollbild: ansehen, ändern, löschen, neue eintippen oder einsprechen. Die KI-Korrektur
 * formuliert nur die zuletzt hinzugefügte Regel um; „Rückgängig“ nimmt die letzte Änderung zurück.
 */
@Composable
fun RegelnBildschirm(vm: AppViewModel, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    BackHandler { vm.regelnSchliessen() }
    var eingabe by rememberSaveable { mutableStateOf("") }
    var bearbeite by rememberSaveable { mutableStateOf<Int?>(null) }
    var entwurf by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).antippen { vm.regelnSchliessen() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
            }
            Text("Franks Regeln", Modifier.weight(1f).padding(start = 2.dp), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (vm.regelRueckgaengigMoeglich) Knopf("Rückgängig", icon = Icons.AutoMirrored.Rounded.Undo, haupt = false) { bearbeite = null; vm.regelRueckgaengig() }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "kopf") {
                Text(
                    "Diese Regeln beachtet Jarvis bei jeder Antwort, bei seinen Agenten und in der Tagesauswertung. Er lernt sie auch selbst, wenn du ihm sagst, wie du etwas künftig haben möchtest.",
                    color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            if (vm.regeln.isEmpty()) item(key = "leer") {
                Karte { Text("Noch keine Regeln. Tippe unten eine ein oder sprich sie mit dem Mikrofon ein.", color = f.textLeise, fontSize = 15.sp) }
            }
            items(vm.regeln, key = { it.id }) { regel ->
                val letzte = regel.id == vm.letzteRegel
                Karte(toenung = if (letzte) f.primaer else null) {
                    if (bearbeite == regel.id) {
                        Eingabe(entwurf, { entwurf = it }, "Regel", Modifier.fillMaxWidth(), einzeilig = false)
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Knopf("Speichern", Modifier.weight(1f), Icons.Rounded.Check) { vm.regelAendern(regel.id, entwurf); bearbeite = null }
                            Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { bearbeite = null }
                        }
                    } else {
                        Text(regel.text, color = f.text, fontSize = 16.sp, lineHeight = 23.sp)
                        Text((if (letzte) "Zuletzt hinzugefügt · " else "") + "Stand " + regel.stand, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Knopf("Ändern", Modifier.weight(1f), Icons.Rounded.Edit, haupt = false) { entwurf = regel.text; bearbeite = regel.id }
                            Knopf("Löschen", Modifier.weight(1f), Icons.Rounded.Delete, haupt = false, farbe = f.gefahr) { vm.regelLoeschen(regel.id) }
                        }
                        // Nur die zuletzt hinzugefügte Regel bekommt die KI-Korrektur; alle anderen bleiben, wie sie sind.
                        if (letzte) {
                            if (vm.regelKorrigiert) {
                                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Kern(30.dp, f.primaer, aktiv = true)
                                    Text("Jarvis formuliert die Regel …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                                }
                            } else {
                                Knopf("KI-Korrektur", Modifier.padding(top = 8.dp).fillMaxWidth(), Icons.Rounded.AutoAwesome) { vm.regelKorrektur() }
                            }
                            Text("Formuliert nur diese eine Regel in klares Deutsch um, so dass Jarvis sie sicher versteht. Mit „Rückgängig“ oben holst du die alte Fassung zurück.", color = f.textSchwach, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
            val hinzufuegen = { if (eingabe.isNotBlank()) { vm.regelNeu(eingabe); eingabe = "" } }
            Eingabe(eingabe, { eingabe = it }, if (vm.nimmtAuf) "Ich höre zu … tippe zum Speichern" else if (vm.schreibtMit) "Schreibe mit …" else "Neue Regel eintippen oder einsprechen", Modifier.weight(1f), einzeilig = false, senden = hinzufuegen)
            Spacer(Modifier.width(10.dp))
            // Leeres Feld: der Knopf ist das Mikrofon. Mit Text: als Regel speichern.
            val alsMikro = eingabe.isBlank()
            Box(
                Modifier.size(52.dp).knopf3d(if (vm.nimmtAuf) f.gefahr else f.primaer, if (vm.nimmtAuf) f.gefahr else f.tertiaer, 26.dp, f.dunkel)
                    .antippen { if (alsMikro || vm.nimmtAuf) vm.regelMikrofonTippen() else hinzufuegen() },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    vm.schreibtMit -> Kern(34.dp, f.aufPrimaer, aktiv = true)
                    vm.nimmtAuf -> Icon(Icons.Rounded.Stop, "Aufnahme beenden und als Regel speichern", tint = f.aufPrimaer, modifier = Modifier.size(22.dp))
                    alsMikro -> Icon(Icons.Rounded.Mic, "Regel einsprechen", tint = f.aufPrimaer, modifier = Modifier.size(24.dp))
                    else -> Icon(Icons.AutoMirrored.Rounded.Send, "Regel speichern", tint = f.aufPrimaer, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
