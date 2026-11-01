package de.frank.ziele.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.glas

@Composable
fun LeitsatzBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text("Leitsatz", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
            TextButton(onClick = vm::leitsatzSpeichern, enabled = vm.aufnahme == Aufnahme.AUS && !vm.leitsatzSpeichert) {
                Text(if (vm.leitsatzSpeichert) "Speichert …" else "Speichern", color = f.primaer)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().glas(f, erhoeht = 1f, toenung = f.primaer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MikroKnopf(vm.aufnahme, 60.dp) { if (!vm.leitsatzSpeichert) vm.mikroTippen(DiktatZiel.LEITSATZ) }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(when (vm.aufnahme) { Aufnahme.AUS -> "Leitsatz einsprechen"; Aufnahme.LAEUFT -> "Ich höre zu …"; Aufnahme.VERARBEITET -> "Schreibe mit …" }, color = f.text, fontSize = 16.sp)
                    if (vm.aufnahme == Aufnahme.LAEUFT) Aufnahmezeit(vm.aufnahmeStart)
                }
            }
            Text("Deine eigenen Worte bleiben unverändert. Nach dem Speichern erscheinen sie automatisch in Perfect Moment unter Leitsätze.", color = f.textLeise, fontSize = 13.sp)
            Box(Modifier.fillMaxWidth().heightIn(min = 180.dp).glas(f, 16.dp, 0.6f, f.flaecheStark).padding(16.dp)) {
                if (vm.leitsatzEntwurf.isEmpty()) Text("Dein Leitsatz …", color = f.textSchwach, fontSize = 16.sp)
                BasicTextField(vm.leitsatzEntwurf, { vm.leitsatzEntwurf = it }, enabled = !vm.leitsatzSpeichert && vm.aufnahme == Aufnahme.AUS,
                    textStyle = TextStyle(color = f.text, fontSize = 16.sp, lineHeight = 24.sp), cursorBrush = SolidColor(f.primaer), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
