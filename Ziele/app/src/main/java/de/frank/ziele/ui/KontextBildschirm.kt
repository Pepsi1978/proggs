package de.frank.ziele.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.glas
import kotlinx.coroutines.delay

/** Die Kontextdatei: was die KI über Frank wissen soll. Einsprechen hängt einen datierten Absatz an, Tippen ändert direkt. */
@Composable
fun KontextBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val gespeichert by vm.kontext.collectAsState()
    var text by remember { mutableStateOf(gespeichert.orEmpty()) }
    var getippt by remember { mutableStateOf(false) }
    // Stand der Datei, auf dem das Tippen begann: Kommt währenddessen etwas hinten dazu, bleibt es beim Speichern erhalten.
    var basis by remember { mutableStateOf(gespeichert.orEmpty()) }
    fun speichern() { vm.kontextSpeichern(basis, text); getippt = false }
    // Neues aus dem Diktat oder von Jarvis übernehmen, solange gerade nicht getippt wird.
    LaunchedEffect(gespeichert) { if (!getippt) { text = gespeichert.orEmpty(); basis = text } }
    // Getipptes nach einer kurzen Pause speichern, beim Verlassen sofort.
    LaunchedEffect(text, getippt) { if (getippt) { delay(700); speichern() } }
    val aktuell by rememberUpdatedState(::speichern)
    val offen by rememberUpdatedState(getippt)
    DisposableEffect(Unit) { onDispose { if (offen) aktuell() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
            Text("Kontextdatei", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).weight(1f))
            RundKnopf(if (vm.sprichtId == "kontext") Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, "Vorlesen") { vm.kontextVorlesen() }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().glas(f, erhoeht = 1.2f, toenung = f.primaer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MikroKnopf(vm.aufnahme, 60.dp) { vm.mikroTippen(DiktatZiel.KONTEXT) }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    when (vm.aufnahme) {
                        Aufnahme.LAEUFT -> { Text("Ich höre zu …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold); Aufnahmezeit(vm.aufnahmeStart) }
                        Aufnahme.VERARBEITET -> Text("Schreibe mit …", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Aufnahme.AUS -> {
                            Text("Kontext einsprechen", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Erzähl, was die KI über dich wissen soll: Alltag, Dienstplan, Gesundheit, Vorlieben, was dir schwerfällt.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            Text(
                "Jede Auswertung liest diese Datei mit" + (if (vm.einstellungen.jarvisKontext) ", dazu Jarvis' Live-Gedächtnis" else "") +
                    ". Eingesprochenes kommt mit Datum unten dazu; du kannst alles direkt ändern.",
                color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Box(Modifier.fillMaxWidth().heightIn(min = 260.dp).glas(f, 16.dp, 0.6f, f.flaecheStark).padding(14.dp)) {
                if (text.isEmpty()) Text("Noch leer. Sprich oben einfach los oder tippe hier.", color = f.textSchwach, fontSize = 15.sp)
                BasicTextField(
                    text, { text = it; getippt = true },
                    textStyle = TextStyle(color = f.text, fontSize = 15.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(f.primaer),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
