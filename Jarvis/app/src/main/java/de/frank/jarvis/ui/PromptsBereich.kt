package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.ablage.Prompt
import de.frank.jarvis.ablage.Prompts
import de.frank.jarvis.ui.theme.LocalBewegung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.module.draganddrop.ReorderAutoScroll
import de.frank.module.draganddrop.rememberReorderState
import de.frank.module.draganddrop.reorderHandle
import de.frank.module.draganddrop.reorderItem
import de.frank.module.draganddrop.reorderViewport
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    // Gemerkt wird der Anlagezeitpunkt, nicht die Nummer: eine gelöschte Nummer kann an einen neuen Prompt gehen.
    var offen by rememberSaveable { mutableStateOf<Long?>(null) }
    var neu by rememberSaveable { mutableStateOf(false) }
    val defekt by ladeImHintergrund(false, stand) { withContext(Dispatchers.IO) { Prompts.defekt(context) } }
    // Auch nach dem Verbinden des Modells: vorher kann keine Überschrift entstehen.
    LaunchedEffect(liste, vm.modellBereit) { liste?.let { vm.promptTitelNachziehen(it) } }

    // Ziehen zum Umsortieren (Modul M1.2): Die Reihenfolge lebt während der Geste lokal, damit ein Neuladen sie nicht zurücksetzt.
    val listState = rememberLazyListState()
    val zustand = rememberReorderState(listState, PROMPTS_REITER)
    val reduziert = !LocalBewegung.current
    ReorderAutoScroll(zustand)
    var reihenfolge by remember { mutableStateOf(liste.orEmpty().map { it.erstellt }) }
    LaunchedEffect(liste) {
        val ids = liste.orEmpty().map { it.erstellt }
        reihenfolge = if (zustand.draggedId == null) ids else {
            val vorhanden = ids.toSet()
            val lokal = reihenfolge.toSet()
            reihenfolge.filter { it in vorhanden } + ids.filter { it !in lokal }
        }
    }
    val nachId = remember(liste) { liste.orEmpty().associateBy { it.erstellt } }
    val sortiert = remember(reihenfolge, nachId) { reihenfolge.mapNotNull(nachId::get) }

    LazyColumn(
        modifier.fillMaxWidth().reorderViewport(
            state = zustand,
            order = { reihenfolge },
            onMove = { von, nach -> reihenfolge = reihenfolge.toMutableList().apply { add(nach, removeAt(von)) } },
            onDrop = {
                val neu = reihenfolge
                bereich.launch {
                    if (withContext(Dispatchers.IO) { Prompts.ordne(context, neu) }) return@launch
                    // Nicht gespeichert: zurück zur Reihenfolge der Datei, damit Anzeige und Nummern übereinstimmen.
                    reihenfolge = liste.orEmpty().map { it.erstellt }
                    vm.meldung = "Die neue Reihenfolge ließ sich nicht speichern."
                }
            },
            reducedMotion = reduziert,
        ),
        state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (defekt) item(key = "defekt") {
            Text("Die Prompt-Datei ist beschädigt und wurde als prompts.json.defekt gesichert. Jarvis speichert dort nichts Neues, bis sie repariert ist; Details unter Aktivität.",
                color = f.gefahr, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
        } else if (liste?.isEmpty() == true) item(key = "leer") {
            Text("Noch keine Prompts. Tippe auf +, oder sag Jarvis: „Speicher folgendes als Prompt ab: …“", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
        itemsIndexed(sortiert, key = { _, p -> p.erstellt }) { i, p ->
            // Die Nummer ist der Platz: oben immer Prompt 1, auch während des Ziehens.
            Row(reorderItem(zustand, p.erstellt, reduziert).fillMaxWidth().glas(f, erhoeht = 0.7f), verticalAlignment = Alignment.CenterVertically) {
                // Nur der Textteil öffnet den Prompt; der Griff bleibt frei, sonst öffnete ein abgebrochenes Ziehen den Dialog.
                Column(Modifier.weight(1f).antippen { offen = p.erstellt }.padding(start = 16.dp, top = 16.dp, bottom = 16.dp)) {
                    // Hinter der Nummer die KI-Überschrift (höchstens zwei Zeilen), damit man den Inhalt ohne Lesen erkennt.
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = f.primaer)) { append("Prompt ${i + 1}") }
                        if (p.titel.isNotEmpty()) { append("  ·  "); append(p.titel) }
                    }, color = f.text, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(p.text, color = f.text, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                // Griff rechts, gut mit dem Daumen erreichbar: lange drücken und ziehen.
                Box(reorderHandle(zustand, p.erstellt).size(48.dp, 64.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DragIndicator, "Zum Verschieben lange drücken und ziehen", tint = f.textLeise)
                }
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

    liste?.firstOrNull { it.erstellt == offen }?.let { p ->
        var loeschen by remember(p.erstellt) { mutableStateOf(false) }
        Dialog({ offen = null }, DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.padding(16.dp).fillMaxWidth().deckend(f).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prompt ${p.nummer}", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp).antippen { offen = null }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
                }
                if (p.titel.isNotEmpty()) Text(p.titel, color = f.primaer, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                Text(p.text, color = f.text, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(vertical = 10.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()))
                var hinweis by remember(p.erstellt) { mutableStateOf<String?>(null) }
                // Zählt jedes lange Drücken, damit derselbe Hinweis erneut die volle Zeit stehen bleibt.
                var hinweisNr by remember(p.erstellt) { mutableStateOf(0) }
                val zeige: (String) -> Unit = { hinweis = it; hinweisNr++ }
                var kopiert by remember(p.erstellt) { mutableStateOf(false) }
                LaunchedEffect(hinweisNr) { if (hinweis != null) { delay(1_800); hinweis = null } }
                LaunchedEffect(kopiert) { if (kopiert) { delay(1_500); kopiert = false } }
                // Beschreibung beim langen Drücken bzw. „Kopiert“ als kurze Rückmeldung über den Symbolen.
                Text(hinweis ?: if (kopiert) "Kopiert" else " ", color = if (hinweis == null && kopiert) f.erfolg else f.textLeise, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), textAlign = TextAlign.End)
                if (!loeschen) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Symbol(Icons.Rounded.Delete, "Löschen", f.gefahr, zeige) { loeschen = true }
                    Symbol(Icons.Rounded.Edit, "Bearbeiten", f.text, zeige) { offen = null; vm.promptBearbeiten(p) }
                    Symbol(if (kopiert) Icons.Rounded.Check else Icons.Rounded.ContentCopy, "Kopieren", if (kopiert) f.erfolg else f.text, zeige) {
                        vm.kopiere(p.text, "Prompt ${p.nummer}", melden = false); kopiert = true
                    }
                    Symbol(Icons.Rounded.Share, "Senden an …", f.text, zeige) {
                        teilen(vm, context, p.text, "Prompt ${p.nummer} senden an")
                    }
                    Spacer(Modifier.weight(1f))
                    // Rechts, gut mit dem Daumen erreichbar, als einziges Symbol plastisch hervorgehoben.
                    Box(Modifier.size(60.dp).knopf3d(f.primaer, f.tertiaer, 30.dp, f.dunkel)
                        .antippen(lange = { zeige("Senden") }, langeText = "Senden") { offen = null; vm.promptSenden(p.text) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.Send, "Senden", tint = f.aufPrimaer, modifier = Modifier.size(28.dp))
                    }
                }
                if (loeschen) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Knopf("Wirklich löschen", Modifier.weight(1f), Icons.Rounded.Delete, farbe = f.gefahr) {
                        bereich.launch {
                            val weg = withContext(Dispatchers.IO) { Prompts.loesche(context, p.nummer, p.erstellt) }
                            vm.meldung = if (weg != null) "Prompt ${p.nummer} gelöscht." else "Prompt ${p.nummer} ließ sich nicht löschen."
                            if (weg != null) offen = null
                        }
                    }
                    Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { loeschen = false }
                }
            }
        }
    }
}

/** Eingabe eines neuen oder zu bearbeitenden Prompts als ganzer Bildschirm: eintippen oder einsprechen, KI-Knopf formuliert in gutes Deutsch (rückgängig machbar), Diskette speichert. */
@Composable
private fun PromptEingabe(vm: AppViewModel, schliessen: () -> Unit, speichern: () -> Unit) {
    val f = LocalFarben.current
    Dialog(schliessen, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(f.hgOben).systemBarsPadding().imePadding().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vm.promptBearbeitet?.let { "Prompt ${it.nummer} bearbeiten" } ?: "Neuer Prompt", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(40.dp).antippen(aktion = schliessen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
            }
            // Das Feld nimmt den ganzen freien Platz ein und scrollt langen Text in sich.
            Box(Modifier.padding(vertical = 12.dp).weight(1f).fillMaxWidth().glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(16.dp)) {
                if (vm.promptText.isEmpty()) Text(if (vm.nimmtAuf) "Ich höre zu … tippe aufs Mikrofon zum Übernehmen" else if (vm.schreibtMit) "Schreibe mit …" else "Prompt eintippen oder einsprechen", color = f.textSchwach, fontSize = 16.sp)
                BasicTextField(vm.promptText, { vm.promptText = it }, Modifier.fillMaxSize(),
                    textStyle = TextStyle(color = f.text, fontSize = 16.sp, lineHeight = 23.sp), cursorBrush = SolidColor(f.primaer))
            }
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

/** Glasfläche der Prompt-Fenster auf deckendem Grund: dahinter scheint nichts durch. */
private fun Modifier.deckend(f: de.frank.jarvis.ui.theme.Farben): Modifier =
    background(f.hgUnten, RoundedCornerShape(f.radius)).glas(f, fuellung = f.flaecheStark)

/** Rundes Symbol unter einem Prompt; langes Drücken zeigt über [hinweis] kurz, was es tut. */
@Composable
private fun Symbol(icon: androidx.compose.ui.graphics.vector.ImageVector, beschreibung: String, farbe: androidx.compose.ui.graphics.Color, hinweis: (String) -> Unit, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(52.dp).glas(f, 26.dp, erhoeht = 0.5f).antippen(lange = { hinweis(beschreibung) }, langeText = beschreibung, aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = farbe, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Rund(icon: androidx.compose.ui.graphics.vector.ImageVector, beschreibung: String, farbe: androidx.compose.ui.graphics.Color, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(52.dp).knopf3d(farbe, if (farbe == f.primaer) f.tertiaer else farbe, 26.dp, f.dunkel).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = f.aufPrimaer, modifier = Modifier.size(24.dp))
    }
}
