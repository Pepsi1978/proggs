package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.atem.AtemFarben
import de.frank.jarvis.atem.AtemProfil
import de.frank.jarvis.atem.AtemProfile
import de.frank.jarvis.atem.AtemSitzung
import de.frank.jarvis.atem.AtemStimme
import de.frank.jarvis.atem.AtemSatzVerlauf
import de.frank.jarvis.atem.SatzArt
import de.frank.jarvis.atem.dauerText
import de.frank.jarvis.atem.zehntelText
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Welches Werkzeug im Reiter „Tools“ offen ist. */
object ToolsAnsicht {
    var tool by mutableStateOf<String?>(null)
    var bearbeiten by mutableStateOf<AtemProfil?>(null)
    var neu by mutableStateOf(false)
    internal val hintergrund = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

/** Längste Phase: zwei Minuten. */
private const val MAX_PHASE = 1200
private val GRUEN = Color(AtemFarben.EIN)
private val GELB = Color(AtemFarben.HALTEN)
private val ORANGE = Color(AtemFarben.AUS)

private data class Tool(val id: String, val titel: String, val text: String, val icon: ImageVector, val farben: List<Color>)

private val TOOLS = listOf(
    Tool("atem", "Atemübungen", "Geführtes Atmen mit Animation, Stimme und eigenen Sätzen", Icons.Rounded.Air, listOf(GRUEN, Color(0xFF4FC8FF))),
)

@Composable
fun ToolsBildschirm(vm: AppViewModel) {
    val b = ToolsAnsicht.bearbeiten
    when {
        b != null -> AtemEditor(vm, b, ToolsAnsicht.neu)
        ToolsAnsicht.tool == "atem" -> AtemUebersicht()
        else -> ToolsUebersicht()
    }
}

@Composable
private fun ToolsUebersicht() {
    val f = LocalFarben.current
    Column(Modifier.fillMaxSize()) {
        Text("Tools", Modifier.padding(start = 20.dp, top = 8.dp), color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("In Jarvis eingebaute Werkzeuge", Modifier.padding(start = 20.dp, bottom = 8.dp), color = f.textLeise, fontSize = 14.sp)
        LazyVerticalGrid(GridCells.Adaptive(170.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(TOOLS, key = { it.id }) { t ->
                Column(Modifier.fillMaxWidth().glas(f, 24.dp, erhoeht = 0.9f).antippen { ToolsAnsicht.tool = t.id }.semantics { contentDescription = t.titel }.padding(16.dp)) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(t.farben)), contentAlignment = Alignment.Center) {
                        Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Text(t.titel, Modifier.padding(top = 12.dp), color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(t.text, Modifier.padding(top = 4.dp), color = f.textLeise, fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Atemübungen: Übersicht

@Composable
private fun AtemUebersicht() {
    val f = LocalFarben.current
    val context = LocalContext.current
    val profile by AtemProfile.stand.collectAsState()
    LaunchedEffect(Unit) { AtemProfile.sicher(context) }
    BackHandler { ToolsAnsicht.tool = null }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).antippen { ToolsAnsicht.tool = null }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
                }
                Text("Atemübungen", Modifier.weight(1f), color = f.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            val speicherHinweis by AtemProfile.hinweis.collectAsState()
            speicherHinweis?.let { Text(it, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = f.gefahr, fontSize = 13.sp) }
            if (profile.isEmpty()) {
                Text("Noch kein Atemprofil. Tippe auf Plus, um dein erstes Muster anzulegen.", Modifier.padding(24.dp), color = f.textLeise, fontSize = 15.sp)
            }
            // Langer Druck auf eine Karte zieht sie an eine neue Stelle; während des Ziehens eigene Reihenfolge, beim Loslassen einmal gespeichert.
            val haptik = LocalHapticFeedback.current
            val bereich = rememberCoroutineScope()
            var lokal by remember { mutableStateOf<List<AtemProfil>?>(null) }
            val gezeigt = lokal ?: profile
            val listenStand = rememberLazyListState()
            val ordnen = rememberReorderableLazyListState(listenStand) { von, nach ->
                val l = (lokal ?: profile).toMutableList()
                val a = l.indexOfFirst { it.id == von.key }
                val b = l.indexOfFirst { it.id == nach.key }
                if (a < 0 || b < 0) return@rememberReorderableLazyListState
                l.add(b, l.removeAt(a))
                lokal = l
                haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            LazyColumn(state = listenStand, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(gezeigt, key = { it.id }) { p ->
                    ReorderableItem(ordnen, key = p.id) { zieht ->
                        val gross by animateFloatAsState(if (zieht) 1.035f else 1f, label = "gross")
                        AtemKarte(
                            p,
                            Modifier.graphicsLayer { scaleX = gross; scaleY = gross }.longPressDraggableHandle(
                                onDragStarted = { haptik.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragStopped = {
                                    val l = lokal
                                    if (l != null) bereich.launch {
                                        runCatching { AtemProfile.reihenfolge(context.applicationContext, l.map { it.id }) }
                                        lokal = null
                                    }
                                },
                            ),
                        )
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(20.dp).size(62.dp).shadow(10.dp, CircleShape).clip(CircleShape)
                .background(Brush.linearGradient(listOf(GRUEN, Color(0xFF2EC4B6))))
                .antippen { ToolsAnsicht.neu = true; ToolsAnsicht.bearbeiten = AtemProfil(name = "", farbe = AtemFarben.KARTEN[profile.size % AtemFarben.KARTEN.size]) }
                .semantics { contentDescription = "Neues Atemprofil" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
private fun AtemKarte(p: AtemProfil, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val farbe = Color(p.farbe)
    val form = RoundedCornerShape(26.dp)
    Row(
        modifier.fillMaxWidth().clip(form)
            .background(Brush.linearGradient(listOf(farbe.copy(alpha = if (f.dunkel) 0.40f else 0.32f), farbe.copy(alpha = 0.08f))))
            .border(1.dp, farbe.copy(alpha = 0.55f), form)
            .antippen { ToolsAnsicht.neu = false; ToolsAnsicht.bearbeiten = p }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(farbe))
                Text(p.name.ifBlank { "Ohne Namen" }, Modifier.padding(start = 8.dp), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Das Muster als Zahlen und als Band in den Phasenfarben, anteilig zur Dauer.
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MusterWert("Ein", p.ein, GRUEN); MusterWert("Halten", p.halt1, GELB); MusterWert("Aus", p.aus, ORANGE); MusterWert("Halten", p.halt2, GELB)
            }
            MusterBand(p, Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp))
            Text(
                "${p.zyklen} Zyklen · ${dauerText(p.dauerMs)} min · " + "%.1f".format(600f / p.zyklusZehntel.coerceAtLeast(1)).replace('.', ',') + " Atemzüge/min",
                Modifier.padding(top = 8.dp), color = f.textLeise, fontSize = 13.sp,
            )
            val extras = listOfNotNull("Stimme".takeIf { p.vorlesen }, "Atemgeräusch".takeIf { p.geraeusch }, "Sätze".takeIf { p.saetzeAn && p.alleSaetze.isNotEmpty() }, "Intro".takeIf { p.intro })
            if (extras.isNotEmpty()) Text(extras.joinToString(" · "), Modifier.padding(top = 2.dp), color = f.textSchwach, fontSize = 12.sp)
        }
        Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(f.flaecheStark).antippen { ToolsAnsicht.neu = false; ToolsAnsicht.bearbeiten = p }.semantics { contentDescription = "Einstellungen von ${p.name}" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Settings, null, tint = f.textLeise, modifier = Modifier.size(20.dp))
            }
            Box(
                Modifier.size(58.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Brush.linearGradient(listOf(farbe, farbe.copy(alpha = 0.7f))))
                    .antippen { AtemSitzung.starten(context, p) }.semantics { contentDescription = "${p.name} starten" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        }
    }
}

@Composable
private fun MusterWert(titel: String, z: Int, farbe: Color) {
    val f = LocalFarben.current
    Column {
        Text(zehntelText(z) + " s", color = if (z > 0) f.text else f.textSchwach, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(titel, color = if (z > 0) farbe else f.textSchwach, fontSize = 11.sp)
    }
}

@Composable
private fun MusterBand(p: AtemProfil, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(3.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(p.ein to GRUEN, p.halt1 to GELB, p.aus to ORANGE, p.halt2 to GELB).filter { it.first > 0 }.forEach { (z, c) ->
            Box(Modifier.weight(z.toFloat()).fillMaxHeight().background(c))
        }
    }
}

// ---------------------------------------------------------------- Atemübungen: Profil anlegen und ändern

@Composable
private fun AtemEditor(vm: AppViewModel, start: AtemProfil, neu: Boolean) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val bereich = rememberCoroutineScope()
    var p by remember(start.id) { mutableStateOf(start) }
    var loeschFrage by remember { mutableStateOf(false) }
    BackHandler { ToolsAnsicht.bearbeiten = null }

    fun speichern() {
        val fertig = p.copy(name = p.name.trim().ifBlank { "Atemprofil" }, zyklen = p.zyklen.coerceAtLeast(1))
        if (fertig.ein <= 0 || fertig.aus <= 0) { vm.meldung = "Einatmen und Ausatmen brauchen je mindestens 0,1 Sekunden."; return }
        bereich.launch {
            val ok = runCatching { AtemProfile.speichern(context, fertig) }.isSuccess
            if (!ok) { vm.meldung = "Das Profil ließ sich nicht speichern (Speicher voll?)."; return@launch }
            ToolsAnsicht.bearbeiten = null
            // Stimme gleich vorbereiten, damit der Start sofort klappt.
            ToolsAnsicht.hintergrund.launch { AtemStimme.vorbereiten(context.applicationContext, fertig) }
        }
    }

    if (loeschFrage) AlertDialog(
        onDismissRequest = { loeschFrage = false },
        title = { Text("„${p.name}“ löschen?") },
        text = { Text("Das Atemprofil wird endgültig entfernt.") },
        confirmButton = { TextButton({ loeschFrage = false; bereich.launch { if (runCatching { AtemProfile.loeschen(context, p.id) }.isSuccess) ToolsAnsicht.bearbeiten = null else vm.meldung = "Das Profil ließ sich nicht löschen." } }) { Text("Löschen", color = f.gefahr) } },
        dismissButton = { TextButton({ loeschFrage = false }) { Text("Abbrechen") } },
    )

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).antippen { ToolsAnsicht.bearbeiten = null }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
            }
            Text(if (neu) "Neues Atemprofil" else "Profil bearbeiten", Modifier.weight(1f), color = f.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(p.farbe)).antippen { speichern() }.semantics { contentDescription = "Speichern" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Abschnitt("Name") {
                Eingabe(p.name, { p = p.copy(name = it) }, "z. B. Baroreflex")
                Text("Farbe der Karte", Modifier.padding(top = 14.dp, bottom = 8.dp), color = f.textLeise, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AtemFarben.KARTEN.forEach { c ->
                        val gewaehlt = c == p.farbe
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(Color(c))
                                .then(if (gewaehlt) Modifier.border(3.dp, f.text, CircleShape) else Modifier)
                                .antippen { p = p.copy(farbe = c) },
                        )
                    }
                }
            }
            Abschnitt("Atemmuster") {
                ZeitZeile("Einatmen", GRUEN, p.ein, 1) { p = p.copy(ein = it) }
                ZeitZeile("Halten", GELB, p.halt1, 0) { p = p.copy(halt1 = it) }
                ZeitZeile("Ausatmen", ORANGE, p.aus, 1) { p = p.copy(aus = it) }
                ZeitZeile("Halten", GELB, p.halt2, 0) { p = p.copy(halt2 = it) }
                MusterBand(p, Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp))
            }
            Abschnitt("Dauer") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zyklen", Modifier.weight(1f), color = f.text, fontSize = 16.sp)
                    Stepper(p.zyklen.toString(), { p = p.copy(zyklen = (p.zyklen - 1).coerceAtLeast(1)) }, { p = p.copy(zyklen = (p.zyklen + 1).coerceAtMost(999)) }) { t ->
                        t.filter { it.isDigit() }.take(3).toIntOrNull()?.let { p = p.copy(zyklen = it.coerceIn(1, 999)) }
                    }
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                    Text(dauerText(p.dauerMs), color = Color(p.farbe), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(" min Gesamtdauer", Modifier.padding(bottom = 6.dp), color = f.textLeise, fontSize = 14.sp)
                }
                Text(
                    "Ein Zyklus ${zehntelText(p.zyklusZehntel)} s · " + "%.1f".format(600f / p.zyklusZehntel.coerceAtLeast(1)).replace('.', ',') + " Atemzüge pro Minute" +
                        (if (p.intro || p.outro) " · zuzüglich Intro und Outro" else ""),
                    color = f.textSchwach, fontSize = 12.sp,
                )
            }
            Abschnitt("Ablauf") {
                SchalterZeile("Vorlesen", "Einatmen, Halten und Ausatmen werden angesagt", p.vorlesen) { p = p.copy(vorlesen = it) }
                SchalterZeile("Atemgeräusch", "Sanftes Ein- und Ausatmen im Takt", p.geraeusch) { p = p.copy(geraeusch = it) }
                SchalterZeile("Intro", "„Mach es dir bequem …“ vor dem Start", p.intro) { p = p.copy(intro = it) }
                SchalterZeile("Outro", "Ruhiger Ausklang am Ende", p.outro) { p = p.copy(outro = it) }
            }
            Abschnitt("Sätze") {
                SchalterZeile("Sätze vorlesen", "Eigene Sätze mittig in der Ein-, Halte- und Ausatemphase; je Zeile ein Satz, mehrere wechseln sich ab; leeres Feld = kein Satz", p.saetzeAn) { p = p.copy(saetzeAn = it) }
                if (p.saetzeAn) {
                    SatzFeld(vm, "Beim Einatmen", GRUEN, SatzArt.EIN, p.satzEin, p.ein, AtemStimme.EINATMEN, p.vorlesen) { p = p.copy(satzEin = it) }
                    // Haltephasen nur, wenn sie eine Dauer haben; leer = es wird nichts vorgelesen.
                    if (p.halt1 > 0) SatzFeld(vm, "Beim Halten nach dem Einatmen", GELB, SatzArt.HALTEN, p.satzHalt1, p.halt1, AtemStimme.HALTEN, p.vorlesen) { p = p.copy(satzHalt1 = it) }
                    SatzFeld(vm, "Beim Ausatmen", ORANGE, SatzArt.AUS, p.satzAus, p.aus, AtemStimme.AUSATMEN, p.vorlesen) { p = p.copy(satzAus = it) }
                    if (p.halt2 > 0) SatzFeld(vm, "Beim Halten nach dem Ausatmen", GELB, SatzArt.HALTEN, p.satzHalt2, p.halt2, AtemStimme.HALTEN, p.vorlesen) { p = p.copy(satzHalt2 = it) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!neu) Knopf("Löschen", Modifier.weight(1f), icon = Icons.Rounded.Delete, haupt = false) { loeschFrage = true }
                Knopf("Speichern", Modifier.weight(1f), icon = Icons.Rounded.Check, farbe = Color(p.farbe)) { speichern() }
            }
        }
    }
}

@Composable
private fun Abschnitt(titel: String, inhalt: @Composable ColumnScope.() -> Unit) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().glas(f, 22.dp, erhoeht = 0.6f).padding(16.dp)) {
        Text(titel.uppercase(), color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, modifier = Modifier.padding(bottom = 10.dp))
        inhalt()
    }
}

@Composable
private fun Eingabe(wert: String, aendern: (String) -> Unit, platzhalter: String, einzeilig: Boolean = true) {
    val f = LocalFarben.current
    Box(Modifier.fillMaxWidth().glas(f, 14.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (wert.isEmpty()) Text(platzhalter, color = f.textSchwach, fontSize = 16.sp)
        BasicTextField(wert, aendern, Modifier.fillMaxWidth(), singleLine = einzeilig, minLines = if (einzeilig) 1 else 2,
            textStyle = TextStyle(color = f.text, fontSize = 16.sp), cursorBrush = SolidColor(f.primaer))
    }
}

@Composable
private fun ZeitZeile(titel: String, farbe: Color, wert: Int, minimum: Int, setze: (Int) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(farbe))
        Text(titel, Modifier.weight(1f).padding(start = 10.dp), color = f.text, fontSize = 16.sp)
        Stepper(zehntelText(wert), { setze((wert - 1).coerceAtLeast(minimum)) }, { setze((wert + 1).coerceAtMost(MAX_PHASE)) }, einheit = "s", dezimal = true) { t ->
            // Werte unter dem Minimum (etwa die „0“ auf dem Weg zu „0,5“) nicht sofort anheben, sonst springt das Feld beim Tippen.
            t.replace(',', '.').toFloatOrNull()?.let { Math.round(it * 10) }?.takeIf { it >= minimum }?.let { setze(it.coerceAtMost(MAX_PHASE)) }
        }
    }
}

/** Minus, Wert zum Eintippen, Plus. Gedrückt halten wiederholt den Schritt immer schneller. */
@Composable
private fun Stepper(anzeige: String, minus: () -> Unit, plus: () -> Unit, einheit: String = "", dezimal: Boolean = false, getippt: (String) -> Unit) {
    val f = LocalFarben.current
    var text by remember { mutableStateOf(anzeige) }
    var fokus by remember { mutableStateOf(false) }
    LaunchedEffect(anzeige) { if (!fokus || text.replace(',', '.').toFloatOrNull() != anzeige.replace(',', '.').toFloatOrNull()) text = anzeige }
    Row(verticalAlignment = Alignment.CenterVertically) {
        WiederholKnopf(Icons.Rounded.Remove, "Weniger", minus)
        Box(Modifier.padding(horizontal = 6.dp).width(72.dp).glas(f, 12.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            BasicTextField(
                text, { t -> text = t; getippt(t) },
                Modifier.fillMaxWidth().onFokus { fokus = it },
                singleLine = true,
                textStyle = TextStyle(color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = if (dezimal) KeyboardType.Decimal else KeyboardType.Number),
                cursorBrush = SolidColor(f.primaer),
            )
        }
        WiederholKnopf(Icons.Rounded.Add, "Mehr", plus)
        if (einheit.isNotEmpty()) Text(einheit, Modifier.padding(start = 6.dp), color = f.textLeise, fontSize = 14.sp)
    }
}

private fun Modifier.onFokus(melde: (Boolean) -> Unit): Modifier = onFocusChanged { melde(it.isFocused) }

@Composable
private fun WiederholKnopf(icon: ImageVector, beschreibung: String, schritt: () -> Unit) {
    val f = LocalFarben.current
    val aktuell by rememberUpdatedState(schritt)
    val bereich = rememberCoroutineScope()
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(f.flaecheStark).semantics { contentDescription = beschreibung }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val lauf = bereich.launch {
                        aktuell(); delay(420)
                        var pause = 110L
                        while (true) { aktuell(); delay(pause); pause = (pause - 6).coerceAtLeast(35) }
                    }
                    tryAwaitRelease(); lauf.cancel()
                })
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = f.text, modifier = Modifier.size(20.dp)) }
}

/**
 * Rundes Mikrofon unter einem Satzfeld: Tippen startet die Aufnahme, nochmal Tippen übernimmt das Gesagte als neue Zeile (neuer Satz).
 * Dieselbe Transkription wie überall in Jarvis; die Mitschrift kommt über das ViewModel, auch wenn sie erst später ankommt.
 */
@Composable
private fun SatzMikrofon(vm: AppViewModel, feld: String, wert: String, setze: (String) -> Unit, farbe: Color, eingesprochen: (String) -> Unit = {}) {
    val f = LocalFarben.current
    val jetzt by rememberUpdatedState(wert)
    val aktuellSetzen by rememberUpdatedState(setze)
    val aktuellGesprochen by rememberUpdatedState(eingesprochen)
    val meins = vm.antwortDiktatFeld == feld
    val diktat = vm.antwortDiktate[feld]
    LaunchedEffect(diktat) {
        val neu = diktat?.trim() ?: return@LaunchedEffect
        vm.antwortDiktate.remove(feld)
        if (neu.isNotEmpty()) { aktuellSetzen(jetzt.trimEnd().let { if (it.isEmpty()) neu else it + "\n" + neu }); aktuellGesprochen(neu) }
    }
    // Editor geschlossen, während die Aufnahme lief: die Aufnahme beenden.
    DisposableEffect(feld) { onDispose { if (vm.antwortDiktatFeld == feld) vm.antwortAufnahmeBeenden() } }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val aufnahme = meins && vm.nimmtAuf
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(if (aufnahme) f.gefahr else farbe.copy(alpha = 0.9f))
                .antippen { vm.antwortMikrofonTippen(feld) }
                .semantics { contentDescription = if (aufnahme) "Aufnahme beenden und übernehmen" else "Satz einsprechen" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (aufnahme) Icons.Rounded.Stop else Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        Text(
            when {
                aufnahme -> "Ich höre zu … tippe zum Übernehmen"
                meins && vm.schreibtMit -> "Schreibe mit …"
                meins -> "Mikrofon startet …"
                else -> "Satz einsprechen"
            },
            Modifier.padding(start = 10.dp), color = if (aufnahme) f.gefahr else f.textLeise, fontSize = 14.sp,
        )
    }
}

@Composable
private fun SchalterZeile(titel: String, text: String, an: Boolean, setze: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).antippen { setze(!an) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(titel, color = f.text, fontSize = 16.sp)
            Text(text, color = f.textSchwach, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Switch(an, setze, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = f.primaer, checkedBorderColor = f.primaer,
            uncheckedThumbColor = f.textLeise, uncheckedTrackColor = f.flaeche, uncheckedBorderColor = f.rand,
        ))
    }
}

/**
 * Sätze einer Phase mit Zeitschätzung: Passt jeder Satz nach dem Stichwort mittig in die Phase, mit gleich viel Ruhe davor und danach?
 * Erst geschätzt nach der Länge, nach „Genau messen“ mit der echten Stimme.
 */
@Composable
private fun SatzFeld(vm: AppViewModel, titel: String, farbe: Color, art: SatzArt, wert: String, phaseZehntel: Int, stichwort: String, vorlesen: Boolean, setze: (String) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val bereich = rememberCoroutineScope()
    var messen by remember { mutableIntStateOf(0) }
    var misstGerade by remember { mutableStateOf(false) }
    val zeilen = wert.lines().map { it.trim() }.filter { it.isNotEmpty() }
    // Gemessene Längen (falls schon erzeugt), abseits des Hauptfadens.
    var gemessen by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var cue by remember { mutableLongStateOf(0L) }
    LaunchedEffect(zeilen, messen, vorlesen) {
        delay(300)
        withContext(Dispatchers.IO) {
            gemessen = zeilen.mapNotNull { z -> AtemStimme.vorhanden(context, z)?.let { z to it.ms } }.toMap()
            cue = if (vorlesen) AtemStimme.vorhanden(context, stichwort)?.ms ?: AtemStimme.schaetzeMs(context, stichwort) else 0L
        }
    }
    Row(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(farbe))
        Text(titel, Modifier.padding(start = 8.dp).weight(1f), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("Phase ${zehntelText(phaseZehntel)} s", color = f.textLeise, fontSize = 13.sp)
    }
    SatzAuswahl(art, wert, farbe, setze)
    SatzMikrofon(vm, "atem:$titel", wert, setze, farbe) { gesagt ->
        ToolsAnsicht.hintergrund.launch { runCatching { AtemSatzVerlauf.eingesprochen(context.applicationContext, art, gesagt) } }
    }
    val phase = phaseZehntel * 100L
    zeilen.forEachIndexed { i, z ->
        val echt = gemessen[z]
        val s = echt ?: AtemStimme.schaetzeMs(context, z)
        // Dieselben Regeln wie in der Übung (AtemStimme.stichwortPasst/satzRuhe).
        val wirksam = if (AtemStimme.stichwortPasst(cue, phase)) cue else 0L
        val ruhe = AtemStimme.satzRuhe(phase, wirksam, s)
        val passt = ruhe != null
        val sek = { ms: Long -> "%.1f".format(ms / 1000f).replace('.', ',') }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
            Icon(if (passt) Icons.Rounded.Check else Icons.Rounded.GraphicEq, null, tint = if (passt) f.erfolg else f.gefahr, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(
                (if (zeilen.size > 1) "Satz ${i + 1}: " else "") +
                    (if (echt != null) "${sek(s)} s gesprochen" else "≈ ${sek(s)} s geschätzt") +
                    (if (ruhe != null) " · je ${sek(ruhe)} s Ruhe davor und danach" else " · zu lang, frei sind nur ${sek((phase - wirksam).coerceAtLeast(0))} s"),
                Modifier.padding(start = 6.dp), color = if (passt) f.textLeise else f.gefahr, fontSize = 12.sp, lineHeight = 15.sp,
            )
        }
    }
    if (zeilen.isNotEmpty()) {
        Text(
            if (misstGerade) "Misst mit der Jarvis-Stimme …" else "Genau messen",
            Modifier.padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).antippen {
                if (misstGerade) return@antippen
                misstGerade = true
                bereich.launch {
                    (listOf(stichwort) + zeilen).forEach { AtemStimme.hole(context, it) }
                    misstGerade = false; messen++
                }
            }.padding(horizontal = 8.dp, vertical = 6.dp),
            color = farbe, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Ein Eintrag im Satz-Menü: aktuell im Feld gesetzt und/oder zuletzt eingesprochen. */
private data class SatzEintrag(val text: String, val gesetzt: Boolean, val gesprochen: Boolean)

/**
 * Satzfeld als Auswahlmenü: Eintippen bleibt möglich, der Pfeil rechts öffnet die Liste aller je gespeicherten Sätze aller Phasen
 * (profilübergreifend, die dieser Phase zuerst). Antippen eines Eintrags oder vom Pfeil auf einen Eintrag ziehen und loslassen setzt ihn ins Feld.
 * Öffnen und Schließen allein ändern nichts.
 */
@Composable
private fun SatzAuswahl(art: SatzArt, wert: String, farbe: Color, setze: (String) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val haptik = LocalHapticFeedback.current
    val verlauf by AtemSatzVerlauf.stand.collectAsState()
    val profile by AtemProfile.stand.collectAsState()
    LaunchedEffect(Unit) { runCatching { AtemSatzVerlauf.sicher(context.applicationContext) } }
    var offen by remember { mutableStateOf(false) }
    var ueber by remember { mutableStateOf<Int?>(null) }
    var loeschSatz by remember { mutableStateOf<String?>(null) }
    loeschSatz?.let { satz ->
        AlertDialog(
            onDismissRequest = { loeschSatz = null },
            title = { Text("Satz löschen?") },
            text = { Text("„$satz“ verschwindet aus der Auswahl aller Phasen. Profile, die ihn verwenden, behalten ihn.") },
            confirmButton = {
                TextButton({
                    loeschSatz = null
                    ToolsAnsicht.hintergrund.launch { runCatching { AtemSatzVerlauf.loeschen(context.applicationContext, satz) } }
                }) { Text("Löschen", color = f.gefahr) }
            },
            dismissButton = { TextButton({ loeschSatz = null }) { Text("Abbrechen") } },
        )
    }
    val zeilenOrte = remember { HashMap<Int, Rect>() }
    val griffOrt = remember { floatArrayOf(0f, 0f) }

    val gesprochen = verlauf.eingesprochen[art]
    val eintraege = remember(wert, verlauf, profile, art) {
        val gesetzt = wert.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        // Alle Sätze aus allen Phasen: zuerst die dieser Phase, danach die der übrigen Phasen.
        val andere = SatzArt.entries.filter { it != art }
        (gesetzt + listOfNotNull(gesprochen) + verlauf.saetze[art].orEmpty() + profile.flatMap { it.saetze(art) } +
            andere.flatMap { a -> verlauf.saetze[a].orEmpty() + profile.flatMap { it.saetze(a) } } + andere.mapNotNull { verlauf.eingesprochen[it] })
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            // Gelöschte Sätze ausblenden; was gerade im Feld steht, bleibt immer sichtbar.
            .filter { it in gesetzt || it !in verlauf.geloescht }
            .map { SatzEintrag(it, it in gesetzt, it == gesprochen) }
    }
    val aktuelleEintraege by rememberUpdatedState(eintraege)
    val aktuellSetzen by rememberUpdatedState(setze)
    fun waehle(i: Int) {
        aktuelleEintraege.getOrNull(i)?.let { aktuellSetzen(it.text) }
        offen = false; ueber = null
    }
    LaunchedEffect(offen) { if (!offen) { zeilenOrte.clear(); ueber = null } }
    val drehung by animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().glas(f, 14.dp, erhoeht = 0.3f, fuellung = f.flaecheStark)
                .then(if (offen) Modifier.border(1.5.dp, farbe.copy(alpha = 0.8f), RoundedCornerShape(14.dp)) else Modifier)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).padding(vertical = 6.dp)) {
                if (wert.isEmpty()) Text("z. B. Heute ist der erste Tag vom Rest meines Lebens.", color = f.textSchwach, fontSize = 16.sp)
                BasicTextField(wert, setze, Modifier.fillMaxWidth(), singleLine = false, minLines = 2,
                    textStyle = TextStyle(color = f.text, fontSize = 16.sp), cursorBrush = SolidColor(f.primaer))
            }
            // Griff: Antippen öffnet bzw. schließt, gedrückt halten und auf einen Satz ziehen wählt ihn beim Loslassen.
            Box(
                Modifier.padding(start = 6.dp).size(44.dp).clip(CircleShape)
                    .background(if (offen) farbe else farbe.copy(alpha = 0.22f))
                    .onGloballyPositioned { val o = it.positionInRoot(); griffOrt[0] = o.x; griffOrt[1] = o.y }
                    .semantics {
                        contentDescription = if (offen) "Satzliste schließen" else "Gespeicherte Sätze zeigen"
                        role = Role.Button
                        onClick { offen = !offen; true }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val runter = awaitFirstDown()
                            runter.consume()
                            val warOffen = offen
                            // Trefferflächen aus einem früheren Öffnen (Schließ-Animation) nicht mehr gelten lassen.
                            if (!warOffen) zeilenOrte.clear()
                            offen = true
                            var bewegt = false
                            var ziel: Int? = null
                            while (true) {
                                val ereignis = awaitPointerEvent()
                                val c = ereignis.changes.firstOrNull { it.id == runter.id } ?: break
                                if (!c.pressed) { c.consume(); break }
                                if ((c.position - runter.position).getDistance() > viewConfiguration.touchSlop) bewegt = true
                                c.consume()
                                if (bewegt) {
                                    val ort = Offset(griffOrt[0] + c.position.x, griffOrt[1] + c.position.y)
                                    val neu = zeilenOrte.entries.firstOrNull { it.key < aktuelleEintraege.size && it.value.contains(ort) }?.key
                                    if (neu != ziel && neu != null) haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    ziel = neu; ueber = neu
                                }
                            }
                            ueber = null
                            when {
                                bewegt && ziel != null -> waehle(ziel)
                                !bewegt && warOffen -> offen = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ExpandMore, null, tint = if (offen) Color.White else farbe, modifier = Modifier.size(28.dp).rotate(drehung)) }
        }
        AnimatedVisibility(offen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier.padding(top = 8.dp).fillMaxWidth().glas(f, 18.dp, erhoeht = 1f, toenung = farbe.copy(alpha = 0.06f)).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (eintraege.isEmpty()) "Noch keine gespeicherten Sätze. Tippe einen ein oder sprich ihn ein."
                    else "${eintraege.size} ${if (eintraege.size == 1) "Satz" else "Sätze"} · antippen oder vom Pfeil hierher ziehen · lange drücken zum Löschen",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = f.textLeise, fontSize = 12.sp,
                )
                eintraege.forEachIndexed { i, e ->
                    val markiert = e.gesetzt || e.gesprochen
                    val form = RoundedCornerShape(12.dp)
                    Row(
                        Modifier.fillMaxWidth()
                            .onGloballyPositioned { if (offen) zeilenOrte[i] = it.boundsInRoot() }
                            .clip(form)
                            .background(
                                when {
                                    ueber == i -> farbe.copy(alpha = 0.32f)
                                    markiert -> farbe.copy(alpha = if (f.dunkel) 0.16f else 0.12f)
                                    else -> Color.Transparent
                                },
                            )
                            .then(if (ueber == i) Modifier.border(1.5.dp, farbe, form) else Modifier)
                            .antippen(lange = { loeschSatz = e.text }, langeText = "Satz löschen") { waehle(i) }
                            .semantics { contentDescription = e.text + (if (e.gesetzt) ", aktuell" else "") + (if (e.gesprochen) ", zuletzt eingesprochen" else "") }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(width = 4.dp, height = 28.dp).clip(RoundedCornerShape(2.dp)).background(if (markiert) farbe else f.rand))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(e.text, color = f.text, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = if (markiert) FontWeight.SemiBold else FontWeight.Normal, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (markiert) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (e.gesetzt) SatzMarke("Aktuell", Icons.Rounded.Check, farbe)
                                if (e.gesprochen) SatzMarke("Zuletzt eingesprochen", Icons.Rounded.KeyboardVoice, farbe)
                            }
                        }
                        if (e.gesetzt) Icon(Icons.Rounded.Check, null, tint = farbe, modifier = Modifier.padding(start = 8.dp).size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SatzMarke(text: String, icon: ImageVector, farbe: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(farbe.copy(alpha = 0.22f)).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = farbe, modifier = Modifier.size(12.dp))
        Text(text, Modifier.padding(start = 4.dp), color = farbe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
