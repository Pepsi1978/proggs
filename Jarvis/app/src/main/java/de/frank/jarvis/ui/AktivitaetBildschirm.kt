package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.data.Eintrag
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
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
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/** Alles, was Jarvis getan hat — egal ob ChatGPT oder der eigene Chat es ausgelöst hat. */
@Composable
fun AktivitaetBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val eintraege by Protokoll.eintraege.collectAsState()
    // Leeren ist endgültig: erst nach einer Rückfrage, damit ein versehentlicher Tipp nichts löscht.
    // rememberSaveable: Auf- oder Zuklappen des Fold schließt die Rückfrage nicht. Abbrechen, Zurück und Tippen daneben löschen nichts.
    var leerenFragen by rememberSaveable { mutableStateOf(false) }
    if (leerenFragen) androidx.compose.material3.AlertDialog(
        onDismissRequest = { leerenFragen = false },
        containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Alle Aktivitäten löschen?") },
        text = { Text("Möchtest du wirklich alle Aufzeichnungen im Bereich Aktivität löschen?") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { leerenFragen = false; vm.protokollLeeren() }) { Text("Alle löschen", color = f.gefahr) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { leerenFragen = false }) { Text("Abbrechen") } },
    )
    // Angetippter Eintrag, gemerkt über seine Zeit: Die Liste zeigt nur eine Vorschau, das Vollbild den ganzen Text.
    var offen by rememberSaveable { mutableStateOf<Long?>(null) }
    eintraege.firstOrNull { it.zeit == offen }?.let { e -> EintragVollbild(e) { offen = null } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aktivität", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Was Jarvis für dich erledigt hat", color = f.textLeise, fontSize = 14.sp)
            }
            if (eintraege.isNotEmpty()) {
                Box(Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen { leerenFragen = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DeleteSweep, "Verlauf leeren", tint = f.textLeise, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (eintraege.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Kern(96.dp, f.primaer, aktiv = false)
                Text("Noch nichts passiert", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                Text("Sobald ChatGPT oder du Jarvis etwas auftragt, steht es hier.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            return
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(eintraege) { e ->
                val farbe = when {
                    !e.ok -> f.gefahr
                    e.quelle == Quelle.CHATGPT -> f.erfolg
                    else -> f.primaer
                }
                Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f, toenung = farbe).antippen { offen = e.zeit }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).glas(f, 4.dp, 0f, farbe, rand = false))
                        Text(e.werkzeug, Modifier.weight(1f).padding(start = 8.dp), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.quelle.anzeige + " · " + zeitKurz(e.zeit), color = f.textSchwach, fontSize = 12.sp)
                    }
                    Text(e.text, color = if (e.ok) f.textLeise else f.gefahr, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Ein Eintrag im Vollbild, ungekürzt und scrollbar; Zurück oder der Pfeil führt zur Liste. */
@Composable
private fun EintragVollbild(e: Eintrag, zurueck: () -> Unit) {
    val f = LocalFarben.current
    Dialog(zurueck, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten))).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Rundknopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", aktion = zurueck)
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(e.werkzeug, color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(e.quelle.anzeige + " · " + zeitLang(e.zeit) + if (e.ok) "" else " · Fehler", color = if (e.ok) f.textSchwach else f.gefahr, fontSize = 13.sp)
                }
            }
            SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
                Text(
                    e.text, color = if (e.ok) f.text else f.gefahr, fontSize = 16.sp, lineHeight = 23.sp,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

private fun zeitLang(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm:ss 'Uhr'", java.util.Locale.GERMAN))
