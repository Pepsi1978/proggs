package de.frank.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import de.frank.jarvis.ablage.AblageQuellen
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
    var nachQuelle by remember { mutableStateOf(vm.einstellungen.promptsNachQuelle) }
    var quellenWert by remember { mutableStateOf(vm.einstellungen.ablageQuellen) }
    var sortMenue by remember { mutableStateOf(false) }
    // Auch per Sprache umstellbar, während der Reiter offen ist: Die Anzeige folgt den Einstellungen.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            vm.einstellungen.promptsNachQuelle.let { if (it != nachQuelle) nachQuelle = it }
            vm.einstellungen.ablageQuellen.let { if (it != quellenWert) quellenWert = it }
        }
    }
    var quellenOffen by remember { mutableStateOf(false) }
    val sortiert = remember(reihenfolge, nachId, nachQuelle, quellenWert) {
        val eigene = reihenfolge.mapNotNull(nachId::get)
        // Sortiert umstellen ändert nur die Anzeige: Die Nummern und die eigene Reihenfolge bleiben.
        if (nachQuelle) AblageQuellen.ordnung(quellenWert).let { o -> eigene.sortedBy { AblageQuellen.rang(o, it.quelleName) } } else eigene
    }
    if (quellenOffen) QuellenReihenfolge(AblageQuellen.ordnung(quellenWert), { n -> quellenWert = AblageQuellen.alsWert(n); vm.einstellungen.ablageQuellen = quellenWert; nachQuelle = true; vm.einstellungen.promptsNachQuelle = true }) { quellenOffen = false }

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
        item(key = "sortieren") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    Rundknopf(Icons.AutoMirrored.Rounded.Sort, if (nachQuelle) "Sortieren: nach Quelle" else "Sortieren: eigene Reihenfolge", farbe = f.primaer) { sortMenue = true }
                    Menue(sortMenue, { sortMenue = false }) {
                        androidx.compose.material3.DropdownMenuItem({ Text("Eigene Reihenfolge", color = if (!nachQuelle) f.primaer else f.text) }, { nachQuelle = false; vm.einstellungen.promptsNachQuelle = false; sortMenue = false })
                        androidx.compose.material3.DropdownMenuItem({ Text("Nach Quelle", color = if (nachQuelle) f.primaer else f.text) }, { nachQuelle = true; vm.einstellungen.promptsNachQuelle = true; sortMenue = false })
                        androidx.compose.material3.DropdownMenuItem({ Text("Reihenfolge der Quellen …") }, { sortMenue = false; quellenOffen = true })
                    }
                }
            }
        }
        if (defekt) item(key = "defekt") {
            Text("Die Prompt-Datei ist beschädigt und wurde als prompts.json.defekt gesichert. Jarvis speichert dort nichts Neues, bis sie repariert ist; Details unter Aktivität.",
                color = f.gefahr, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
        } else if (liste?.isEmpty() == true) item(key = "leer") {
            Text("Noch keine Prompts. Tippe auf +, oder sag Jarvis: „Speicher folgendes als Prompt ab: …“", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
        itemsIndexed(sortiert, key = { _, p -> p.erstellt }) { i, p ->
            // Die Nummer ist der Platz: oben immer Prompt 1, auch während des Ziehens. Nach Quelle sortiert bleibt jeder Prompt bei seiner festen Nummer.
            Row(reorderItem(zustand, p.erstellt, reduziert).fillMaxWidth().glas(f, erhoeht = 0.7f), verticalAlignment = Alignment.CenterVertically) {
                // Nur der Textteil öffnet den Prompt; der Griff bleibt frei, sonst öffnete ein abgebrochenes Ziehen den Dialog.
                Column(Modifier.weight(1f).antippen { offen = p.erstellt }.padding(start = 16.dp, top = 16.dp, bottom = 16.dp)) {
                    // Hinter der Nummer die KI-Überschrift (höchstens zwei Zeilen), damit man den Inhalt ohne Lesen erkennt.
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = f.primaer)) { append("Prompt ${if (nachQuelle) p.nummer else i + 1}") }
                        if (p.titel.isNotEmpty()) { append("  ·  "); append(p.titel) }
                    }, color = f.text, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(p.text, color = f.text, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Text(p.quelleName, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                // Griff rechts, gut mit dem Daumen erreichbar: lange drücken und ziehen.
                // Nach Quelle sortiert gibt es nichts zu ziehen: Die Reihenfolge ergibt sich dann aus der Quelle.
                if (!nachQuelle) Box(reorderHandle(zustand, p.erstellt).size(48.dp, 64.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DragIndicator, "Zum Verschieben lange drücken und ziehen", tint = f.textLeise)
                }
            }
        }
        item(key = "plus") {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).knopf3d(f.primaer, f.tertiaer, 28.dp, f.dunkel).antippen { vm.promptNeu() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "Neuer Prompt", tint = f.aufPrimaer, modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    liste?.firstOrNull { it.erstellt == offen }?.let { p ->
        var loeschen by remember(p.erstellt) { mutableStateOf(false) }
        Dialog({ offen = null }, DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp).fillMaxWidth().deckend(f).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prompt ${p.nummer}", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(40.dp).antippen { offen = null }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
                }
                if (p.titel.isNotEmpty()) Text(p.titel, color = f.primaer, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                Text("Von ${p.quelleName}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                // Der Text bekommt nur den Platz, den Kopf und Knöpfe übrig lassen: Sonst schöbe er sie im Querformat aus dem Fenster.
                Text(p.text, color = f.text, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.weight(1f, fill = false).padding(vertical = 10.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()))
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
                        vm.kopiere(de.frank.jarvis.faehigkeit.DiktatTrenner.ohneSchluss(p.text), "Prompt ${p.nummer}", melden = false); kopiert = true
                    }
                    Symbol(Icons.Rounded.Share, "Senden an …", f.text, zeige) {
                        teilen(vm, context, de.frank.jarvis.faehigkeit.DiktatTrenner.ohneSchluss(p.text), "Prompt ${p.nummer} senden an")
                    }
                    Spacer(Modifier.weight(1f))
                    // Rechts, gut mit dem Daumen erreichbar, als einziges Symbol plastisch hervorgehoben.
                    Box(Modifier.size(60.dp).knopf3d(f.primaer, f.tertiaer, 30.dp, f.dunkel)
                        .antippen(lange = { zeige("Senden") }, langeText = "Senden") { offen = null; vm.promptSenden(p) }, contentAlignment = Alignment.Center) {
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

/**
 * Frage nach dem Senden eines Prompts. Eine Ebene über der ganzen App (in [JarvisApp]), weil das Senden die Ablage verlässt.
 * Kein Dialogfenster: Das nähme der Eingabeblase den Fokus und läge über Rückfragen und Meldungen.
 * Zurück oder Tippen daneben gilt als Nein: Der Prompt bleibt.
 */
@Composable
fun PromptLoeschenFrage(vm: AppViewModel) {
    vm.promptLoeschenFrage ?: return
    val f = LocalFarben.current
    BackHandler { vm.promptLoeschenAntwort(false) }
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
        .pointerInput(Unit) { detectTapGestures { vm.promptLoeschenAntwort(false) } }
        .windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(16.dp).fillMaxWidth().deckend(f).pointerInput(Unit) { detectTapGestures { } }.padding(18.dp)) {
            Text("Soll der Prompt gelöscht werden?", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Der Text in der Eingabe bleibt erhalten.", color = f.textLeise, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Knopf("Ja", Modifier.weight(1f), Icons.Rounded.Delete, farbe = f.gefahr) { vm.promptLoeschenAntwort(true) }
                Knopf("Nein", Modifier.weight(1f), haupt = false) { vm.promptLoeschenAntwort(false) }
            }
        }
    }
}

/**
 * Eingabe eines neuen oder zu bearbeitenden Prompts als ganzer Bildschirm: eintippen oder einsprechen, KI-Knopf formuliert in
 * gutes Deutsch (rückgängig machbar), Diskette speichert. Eine Ebene über der ganzen App (in [JarvisApp]), kein Dialogfenster:
 * Das legte Android bei randloser App zu hoch an, die Knöpfe unten lagen außerhalb des Bildschirms (Almanach jetpack-compose 8.9).
 */
@Composable
fun PromptEbene(vm: AppViewModel) {
    if (!vm.promptNeuOffen && vm.promptBearbeitet == null) return
    val f = LocalFarben.current
    val ausgang = remember { vm.promptText }
    var verwerfen by remember { mutableStateOf(false) }
    // Das Plus beginnt immer leer: Ungespeicherter Text eines neuen Prompts ginge beim Schließen verloren, auch ein laufendes Diktat.
    val ungespeichert = if (vm.promptBearbeitet != null) vm.promptText != ausgang else vm.promptText.isNotBlank() || vm.nimmtAuf || vm.schreibtMit
    val versuchen = { if (ungespeichert && !verwerfen) verwerfen = true else vm.promptEingabeSchliessen() }
    BackHandler(onBack = versuchen)
    Column(Modifier.fillMaxSize().background(f.hgOben)
        // Berührungen neben den Bedienelementen gehen nicht an die Ablage darunter.
        .pointerInput(Unit) { detectTapGestures { } }
        .windowInsetsPadding(WindowInsets.safeDrawing).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(vm.promptBearbeitet?.let { "Prompt ${it.nummer} bearbeiten" } ?: "Neuer Prompt", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.size(40.dp).antippen(aktion = versuchen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
        }
        // Mit Schreibmarke am Ende, auch nach Diktat und KI: So bleibt der neue Text im Blick. Das Feld füllt den Platz und scrollt in sich.
        var feld by remember { mutableStateOf(TextFieldValue(vm.promptText, TextRange(vm.promptText.length))) }
        LaunchedEffect(vm.promptText) { if (feld.text != vm.promptText) feld = TextFieldValue(vm.promptText, TextRange(vm.promptText.length)) }
        val fokus = remember { FocusRequester() }
        LaunchedEffect(Unit) { fokus.requestFocus() }
        // Nur eigene Textänderungen ins ViewModel: Ein Tastatur-Update mit dem alten Text überschriebe sonst die gerade gesetzte KI-Fassung.
        BasicTextField(feld, { if (it.text != feld.text) vm.promptText = it.text; feld = it },
            Modifier.padding(vertical = 12.dp).weight(1f).fillMaxWidth().glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).focusRequester(fokus),
            textStyle = TextStyle(color = f.text, fontSize = 16.sp, lineHeight = 23.sp), cursorBrush = SolidColor(f.primaer),
            decorationBox = { inhalt ->
                Box(Modifier.padding(16.dp)) {
                    if (feld.text.isEmpty()) Text(if (vm.nimmtAuf) "Ich höre zu … tippe aufs Mikrofon zum Übernehmen" else if (vm.schreibtMit) "Schreibe mit …" else "Prompt eintippen oder einsprechen", color = f.textSchwach, fontSize = 16.sp)
                    inhalt()
                }
            })
        if (vm.promptFormuliert) Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Kern(28.dp, f.primaer, aktiv = true)
            Text("Jarvis formuliert …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
        }
        if (verwerfen) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Knopf(if (vm.promptBearbeitet != null) "Änderungen verwerfen" else "Entwurf verwerfen", Modifier.weight(1f), Icons.Rounded.Delete, farbe = f.gefahr) { vm.promptEingabeSchliessen() }
            Knopf("Weiter bearbeiten", Modifier.weight(1f), haupt = false) { verwerfen = false }
        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Rund(if (vm.nimmtAuf) Icons.Rounded.Stop else Icons.Rounded.Mic, if (vm.nimmtAuf) "Aufnahme beenden" else "Einsprechen", if (vm.nimmtAuf) f.gefahr else f.primaer) {
                vm.promptMikrofonTippen()
            }
            if (vm.promptVorKi != null) Knopf("Rückgängig", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Undo, haupt = false) { vm.promptKiRueckgaengig() }
            else Knopf("KI", Modifier.weight(1f), Icons.Rounded.AutoAwesome, haupt = false) { vm.promptFormulieren() }
            Rund(Icons.Rounded.Save, "Speichern", f.primaer) { vm.promptSpeichern { vm.promptEingabeSchliessen() } }
        }
        // Ohne Tastatur rund einen Zentimeter Abstand nach unten; mit Tastatur zählt jede Zeile fürs Textfeld.
        if (WindowInsets.ime.getBottom(LocalDensity.current) == 0) Spacer(Modifier.height(63.dp))
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
