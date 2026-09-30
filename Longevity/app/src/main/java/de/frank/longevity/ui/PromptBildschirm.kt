package de.frank.longevity.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.ui.theme.Chip
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.glas

/**
 * Der komplette Aktualisierungs-Prompt zum Bearbeiten. Export/Import als Markdown-Datei über die
 * Android-Dateiauswahl – z. B. in den Update-Ordner auf Google Drive, um ihn am PC mit einer KI zu überarbeiten.
 */
@Composable
fun PromptBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val gespeichert = remember(vm.promptStand) { vm.wirksamerPrompt() }
    var text by remember(vm.promptStand) { mutableStateOf(gespeichert) }
    var stand by remember(vm.promptStand) { mutableStateOf(gespeichert) }
    val geaendert = text != stand

    val exportieren = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) vm.promptExportieren(uri, text)
    }
    // "*/*": Google Drive meldet .md oft als application/octet-stream – ein text/*-Filter würde die Datei ausgrauen.
    val importieren = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.promptImportieren(uri)
    }

    fun speichern() { vm.promptSpeichern(text); stand = text }
    fun schliessen() { if (geaendert) speichern(); vm.zurueck() }
    BackHandler { schliessen() }

    Column(Modifier.fillMaxSize().imePadding()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { schliessen() }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Aktualisierungs-Prompt", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (geaendert) "Ungespeicherte Änderungen" else if (vm.eigenerPrompt) "Eigener Prompt aktiv" else "Standard-Prompt",
                    color = if (geaendert) f.gefahr else f.textLeise, fontSize = 12.sp,
                )
            }
        }
        Box(Modifier.padding(horizontal = 16.dp)) { ChipReihe {
            Chip("Speichern", geaendert, icon = Icons.Rounded.Save) { speichern() }
            Chip("Exportieren", false, icon = Icons.Rounded.FileUpload) { exportieren.launch("Longevity-Aktualisierungsprompt.md") }
            Chip("Importieren", false, icon = Icons.Rounded.FileDownload) { importieren.launch(arrayOf("*/*")) }
            Chip("Standard", false, icon = Icons.Rounded.RestartAlt) { vm.promptStandard() }
        } }
        Text(
            "Abschnitte beginnen mit „## Name“ – die Namen bitte nicht ändern. Platzhalter wie {{LISTE}} füllt die App. " +
                "Die Erklärung oben im Text beschreibt alles genau.",
            color = f.textSchwach, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                .glas(f, 16.dp, 0.6f, f.flaecheStark).verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            BasicTextField(
                text, { text = it },
                textStyle = TextStyle(color = f.text, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(f.primaer),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.navigationBarsPadding().padding(bottom = 12.dp))
    }
}
