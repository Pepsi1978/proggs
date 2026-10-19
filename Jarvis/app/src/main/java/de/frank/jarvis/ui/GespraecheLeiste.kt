package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Sitzung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Seitenleiste mit allen gespeicherten Gesprächen, wie in Geniale Ideen: von links nach rechts wischen öffnet sie,
 * zurückwischen oder die Zurück-Geste schließt sie. Tippen öffnet ein Gespräch, lange drücken benennt um oder löscht.
 */
@Composable
fun GespraecheSchublade(vm: AppViewModel, schublade: DrawerState, wischen: Boolean = true, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    val raum = rememberCoroutineScope()
    BackHandler(enabled = schublade.isOpen) { raum.launch { schublade.close() } }
    // Reiter oben: Gespräche, Programmierung oder Projekte. Beim Wechsel bleibt alles erhalten; geschlossen springt die Leiste
    // zurück auf die Liste, zu der das offene Gespräch gehört, damit Wischen zum Gesprächswechsel sie zeigt.
    var reiter by rememberSaveable { mutableStateOf(LeistenReiter.GESPRAECHE) }
    // Das geöffnete Projekt im Reiter „Projekte“; null = Liste der Projekte.
    var projektOffen by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(schublade.isOpen) { if (schublade.isOpen) vm.sitzungenLaden() }
    // Auch ein neues, noch ungespeichertes Gespräch aus „Neuer Programmierauftrag“: Die Leiste bleibt dann auf „Programmierung“.
    val offenProgrammierung = vm.sitzungen.firstOrNull { it.id == vm.sitzungId }?.programmierung == true || (vm.sitzungId == null && vm.sitzungProgrammierung)
    // Auch mit sitzungProjekt als Schlüssel: Ein gewähltes Gespräch lädt erst nach dem Schließen fertig.
    LaunchedEffect(schublade.isOpen, vm.sitzungProjekt, offenProgrammierung) {
        if (!schublade.isOpen) { reiter = reiterFuer(offenProgrammierung, vm.sitzungProjekt); projektOffen = vm.sitzungProjekt.ifEmpty { null } }
    }
    // Zurück in der App (etwa nach einem Gespräch über die Sprachblase): eine geschlossene Leiste, die mitten in ihrer
    // Animation angehalten wurde, ganz hinausschieben, damit kein Streifen am Rand stehen bleibt.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (schublade.currentValue == DrawerValue.Closed && schublade.targetValue == DrawerValue.Closed) raum.launch { schublade.snapTo(DrawerValue.Closed) }
    }

    ModalNavigationDrawer(
        drawerState = schublade,
        gesturesEnabled = schublade.isOpen && !schublade.isAnimationRunning,
        scrimColor = Color.Black.copy(alpha = if (f.dunkel) 0.55f else 0.32f),
        // Waagerechtes Wischen irgendwo im Gespräch öffnet die Leiste (nicht nur am Rand); senkrechtes Scrollen bleibt der Liste.
        // Erst ein deutlicher Wisch zählt: mindestens 56 dp oder 8 % der Fensterbreite (auf dem Fold innen breiter), klar
        // waagerecht. Antippen mit etwas Zittern und schräges Scrollen öffnen die Leiste nicht mehr.
        modifier = Modifier.pointerInput(schublade, wischen) {
            if (!wischen) return@pointerInput
            val schlupf = viewConfiguration.touchSlop
            val schwelle = maxOf(schlupf * 3f, 56.dp.toPx(), size.width * 0.08f)
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (schublade.isAnimationRunning) return@awaitEachGesture
                val schliessen = schublade.isOpen
                var weg = Offset.Zero
                var erkannt = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (erkannt) {
                        change.consume()
                        if (!change.pressed) break
                        continue
                    }
                    if (!change.pressed || change.isConsumed) break
                    weg += change.position - change.previousPosition
                    // Senkrecht oder steiler als etwa 37 Grad: das ist Scrollen, keine Leistengeste.
                    if (abs(weg.y) > schlupf && abs(weg.y) > abs(weg.x) * 0.75f) break
                    // Falsche Richtung früh aufgeben, damit Wischen in der Gegenrichtung frei bleibt.
                    if (abs(weg.x) > schlupf && ((schliessen && weg.x >= 0f) || (!schliessen && weg.x <= 0f))) break
                    if (abs(weg.x) >= schwelle && abs(weg.x) >= abs(weg.y) * 2f) {
                        if (schublade.isAnimationRunning) break
                        erkannt = true
                        change.consume()
                        raum.launch { if (schliessen) schublade.close() else schublade.open() }
                    } else {
                        // Bereits von einer scrollenden Liste übernommene Gesten nicht stehlen.
                        val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                        if (finalEvent.changes.any { it.id == down.id && it.isConsumed }) break
                    }
                }
            }
        },
        drawerContent = {
            Leiste(
                vm, schublade.isOpen, reiter, { reiter = it }, projektOffen, { projektOffen = it },
                aufNeu = { vm.neuesGespraech(); raum.launch { schublade.close() } },
                aufNeuerAuftrag = { vm.neuesGespraech(); vm.programmiererEinsetzen(); raum.launch { schublade.close() } },
                aufNeuImProjekt = { vm.neuesGespraechImProjekt(it); raum.launch { schublade.close() } },
                aufWahl = { vm.sitzungOeffnen(it); raum.launch { schublade.close() } },
            )
        },
        content = inhalt,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Leiste(
    vm: AppViewModel, offen: Boolean, reiter: LeistenReiter, reiterWaehlen: (LeistenReiter) -> Unit, projektOffen: String?, projektOeffnen: (String?) -> Unit,
    aufNeu: () -> Unit, aufNeuerAuftrag: () -> Unit, aufNeuImProjekt: (String) -> Unit, aufWahl: (String) -> Unit,
) {
    val projekte = reiter == LeistenReiter.PROJEKTE
    val f = LocalFarben.current
    var bearbeiten by remember { mutableStateOf<Sitzung?>(null) }
    var umbenennen by remember { mutableStateOf<Sitzung?>(null) }
    var loeschen by remember { mutableStateOf<Sitzung?>(null) }
    var projektNeu by remember { mutableStateOf(false) }
    val projektIds = remember(vm.projekte) { vm.projekte.map { it.id }.toSet() }
    val listen = remember(vm.sitzungen, projektIds) { aufteilen(vm.sitzungen, projektIds) }
    val normale = listen.gespraeche
    val anzahlJe = remember(vm.sitzungen) { vm.sitzungen.groupingBy { it.projekt }.eachCount() }
    val gruppen = remember(normale) { gruppiert(normale) }
    val programmierGruppen = remember(listen.programmierung) { gruppiert(listen.programmierung) }
    val projekt = if (projekte) vm.projekte.firstOrNull { it.id == projektOffen } else null
    val imProjekt = remember(vm.sitzungen, projekt?.id) { projekt?.let { p -> vm.sitzungen.filter { it.projekt == p.id } }.orEmpty() }
    // Zurück aus einem Projekt führt zur Projektliste, erst danach schließt die Leiste.
    BackHandler(enabled = offen && projekt != null) { projektOeffnen(null) }
    // Welche Gespräche gerade arbeiten: Programmier-Sitzungen (Claude Code) und Jarvis' eigene Antworten.
    val programmiert by de.frank.jarvis.faehigkeit.ProgrammierChat.aktivitaet.collectAsState()
    val arbeitende = remember(programmiert) { programmiert.filterValues { it.arbeitet }.keys }
    // Ein Takt für alle Claude-Symbole in der Liste, nur solange eines zu sehen ist.
    var bild by remember { mutableStateOf(0) }
    val mitEintraegen = !projekte || projekt != null
    LaunchedEffect(mitEintraegen && arbeitende.isNotEmpty()) {
        while (mitEintraegen && arbeitende.isNotEmpty()) {
            kotlinx.coroutines.delay(120)
            bild = (bild + 1) % SPINNER.size
        }
    }

    // Im Stil des gewählten Designs (Blau oder Gold, hell oder dunkel): derselbe Verlauf wie der App-Hintergrund, leicht getönt.
    // Breiter als die Vorgabe, damit neben den Überschriften Platz für die Arbeitssymbole bleibt.
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.9f),
        drawerContainerColor = f.hgUnten,
        drawerContentColor = f.text,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(lerp(f.hgOben, f.primaer, if (f.dunkel) 0.10f else 0.06f), f.hgUnten, lerp(f.hgUnten, f.primaer, if (f.dunkel) 0.06f else 0.04f))))
                .statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp),
        ) {
            // Drei Reiter: auf schmalen Bildschirmen (Fold zugeklappt) Symbol über dem Namen, damit „Programmierung“ ganz hineinpasst.
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp)) {
                val schmal = maxWidth < 420.dp
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Reiter("Gespräche", Icons.Rounded.Forum, gewaehlt = reiter == LeistenReiter.GESPRAECHE, schmal, Modifier.weight(1f)) { reiterWaehlen(LeistenReiter.GESPRAECHE) }
                    Reiter("Programmierung", Icons.Rounded.Code, gewaehlt = reiter == LeistenReiter.PROGRAMMIERUNG, schmal, Modifier.weight(1.35f)) { reiterWaehlen(LeistenReiter.PROGRAMMIERUNG) }
                    Reiter("Projekte", Icons.Rounded.Folder, gewaehlt = projekte, schmal, Modifier.weight(1f)) { reiterWaehlen(LeistenReiter.PROJEKTE) }
                }
            }
            if (projekte && projekt != null) {
                // Ein Projekt: seine eigenen Gespräche, getrennt von allen übrigen.
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).antippen { projektOeffnen(null) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück zu den Projekten", tint = f.text, modifier = Modifier.size(22.dp))
                    }
                    Icon(Icons.Rounded.Folder, null, tint = f.primaer, modifier = Modifier.padding(start = 4.dp).size(22.dp))
                    Text(projekt.name, Modifier.weight(1f).padding(start = 8.dp), color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Knopf("Neues Gespräch im Projekt", Modifier.fillMaxWidth(), icon = Icons.Rounded.AddComment) { aufNeuImProjekt(projekt.id) }
                if (imProjekt.isEmpty()) {
                    Text(
                        "Noch keine Gespräche in diesem Projekt. Starte es mit „Neues Gespräch im Projekt“.",
                        Modifier.padding(horizontal = 6.dp, vertical = 20.dp), color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(imProjekt, key = { it.id }) { s -> SitzungEintrag(vm, s, arbeitende, programmiert, SPINNER[bild], { aufWahl(s.id) }, { bearbeiten = s }) }
                }
                if (imProjekt.isNotEmpty()) Text("Lange drücken: umbenennen oder löschen", Modifier.padding(6.dp, 8.dp), color = f.textSchwach, fontSize = 12.sp)
            } else if (projekte) {
                // Eigener Bereich, getrennt von den Gesprächen: oben immer „Neues Projekt“, darunter die Projekte.
                Knopf("Neues Projekt", Modifier.fillMaxWidth(), icon = Icons.Rounded.CreateNewFolder) { projektNeu = true }
                if (vm.projekte.isEmpty()) {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Rounded.Folder, null, tint = f.primaer.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
                        Text("Noch keine Projekte", Modifier.padding(top = 12.dp), color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Leg mit „Neues Projekt“ eins an. Seine Gespräche bleiben hier, getrennt von deinen übrigen Gesprächen.", Modifier.padding(top = 6.dp), color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(vm.projekte, key = { it.id }) { p ->
                            val anzahl = anzahlJe[p.id] ?: 0
                            val aktiv = p.id == vm.sitzungProjekt
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .then(if (aktiv) Modifier.glas(f, 14.dp, erhoeht = 0.4f, toenung = f.primaer) else Modifier)
                                    .antippen { projektOeffnen(p.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Folder, null, tint = f.primaer, modifier = Modifier.size(22.dp))
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(p.name, color = if (aktiv) f.primaer else f.text, fontSize = 15.sp, fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                                    Text(if (anzahl == 1) "1 Gespräch" else "$anzahl Gespräche", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                // „Gespräche“ oder „Programmierung“: alle Programmieraufträge mit ihren Gesprächen, alte wie neue; sie stehen nicht unter „Gespräche“.
                val auftraege = reiter == LeistenReiter.PROGRAMMIERUNG
                val liste = if (auftraege) listen.programmierung else normale
                if (auftraege) Knopf("Neuer Programmierauftrag", Modifier.fillMaxWidth(), icon = Icons.Rounded.Code, aktion = aufNeuerAuftrag)
                else Knopf("Neues Gespräch", Modifier.fillMaxWidth(), icon = Icons.Rounded.AddComment, aktion = aufNeu)
                if (liste.isEmpty()) {
                    Text(
                        if (auftraege) "Noch keine Programmieraufträge. Jeder Auftrag an den Programmierer landet mit seinem Gespräch von selbst hier."
                        else "Noch keine gespeicherten Gespräche. Jedes Gespräch mit Jarvis landet von selbst hier, auch die mit „Hey Jarvis“.",
                        Modifier.padding(horizontal = 6.dp, vertical = 20.dp), color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                }
                // Je Reiter eine eigene Liste: Die Scrollposition des einen gilt nicht für den anderen.
                key(reiter) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (if (auftraege) programmierGruppen else gruppen).forEach { (gruppe, eintraege) ->
                            item(key = "g:$gruppe") {
                                Text(gruppe.uppercase(), Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp), color = f.textSchwach, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                            }
                            items(eintraege, key = { it.id }) { s -> SitzungEintrag(vm, s, arbeitende, programmiert, SPINNER[bild], { aufWahl(s.id) }, { bearbeiten = s }) }
                        }
                    }
                }
                if (!auftraege || liste.isNotEmpty()) Text("Lange drücken: umbenennen oder löschen", Modifier.padding(6.dp, 8.dp), color = f.textSchwach, fontSize = 12.sp)
            }
        }
    }

    bearbeiten?.let { s ->
        AlertDialog(
            onDismissRequest = { bearbeiten = null },
            containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
            title = { Text(s.titel.ifBlank { "Gespräch" }, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = { umbenennen = s; bearbeiten = null }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, null, tint = f.primaer)
                        Text("Umbenennen", Modifier.padding(start = 12.dp), color = f.text, fontSize = 16.sp)
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = { loeschen = s; bearbeiten = null }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Delete, null, tint = f.gefahr)
                        Text("Löschen", Modifier.padding(start = 12.dp), color = f.gefahr, fontSize = 16.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { bearbeiten = null }) { Text("Schließen") } },
        )
    }

    if (projektNeu) {
        var name by remember { mutableStateOf("") }
        // Tastatur „Senden“ und „Anlegen“ im selben Augenblick legen nur ein Projekt an.
        var gesendet by remember { mutableStateOf(false) }
        val anlegen = { if (name.isNotBlank() && !gesendet) { gesendet = true; vm.projektAnlegen(name) { reiterWaehlen(LeistenReiter.PROJEKTE); projektOeffnen(it.id) }; projektNeu = false } }
        AlertDialog(
            onDismissRequest = { projektNeu = false },
            containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
            title = { Text("Neues Projekt") },
            text = { Eingabe(name, { name = it }, "Name des Projekts", Modifier.fillMaxWidth(), senden = anlegen) },
            confirmButton = { TextButton(onClick = anlegen, enabled = name.isNotBlank()) { Text("Anlegen") } },
            dismissButton = { TextButton(onClick = { projektNeu = false }) { Text("Abbrechen") } },
        )
    }

    umbenennen?.let { s ->
        var name by remember(s.id) { mutableStateOf(s.titel) }
        AlertDialog(
            onDismissRequest = { umbenennen = null },
            containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
            title = { Text("Gespräch umbenennen") },
            text = { Eingabe(name, { name = it }, "Name des Gesprächs", Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.sitzungUmbenennen(s.id, name); umbenennen = null }, enabled = name.isNotBlank()) { Text("Speichern") } },
            dismissButton = { TextButton(onClick = { umbenennen = null }) { Text("Abbrechen") } },
        )
    }

    loeschen?.let { s ->
        AlertDialog(
            onDismissRequest = { loeschen = null },
            containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
            title = { Text("Gespräch löschen?") },
            text = { Text("„${s.titel.ifBlank { "Gespräch" }}“ wird endgültig gelöscht.") },
            confirmButton = { TextButton(onClick = { vm.sitzungLoeschen(s.id); loeschen = null }) { Text("Löschen", color = f.gefahr) } },
            dismissButton = { TextButton(onClick = { loeschen = null }) { Text("Abbrechen") } },
        )
    }
}

/** Ein Gespräch in der Leiste, unter „Gespräche“ wie in einem Projekt: Titel, Arbeitssymbole und Stand. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SitzungEintrag(
    vm: AppViewModel, s: Sitzung, arbeitende: Set<String>, programmiert: Map<String, de.frank.jarvis.faehigkeit.ProgrammierChat.Aktivitaet>, zeichen: String,
    aufWahl: () -> Unit, langDruck: () -> Unit,
) {
    val f = LocalFarben.current
    val gewaehlt = s.id == vm.sitzungId
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (gewaehlt) Modifier.glas(f, 14.dp, erhoeht = 0.4f, toenung = f.primaer) else Modifier)
            .combinedClickable(onClick = aufWahl, onLongClick = langDruck, onClickLabel = "Öffnen", onLongClickLabel = "Umbenennen oder löschen")
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        val codet = s.id in arbeitende
        val denkt = vm.laufende.containsKey(s.id)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.titel.ifBlank { "Gespräch" }, Modifier.weight(1f), color = if (gewaehlt) f.primaer else f.text, fontSize = 15.sp, fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
            // Jarvis antwortet oder recherchiert selbst: sein pulsierender Leuchtkern.
            if (denkt) Kern(22.dp, f.primaer, aktiv = true, modifier = Modifier.padding(start = 8.dp))
            // Eine Programmier-Sitzung (Claude Code) arbeitet: das laufende Claude-Symbol in Orange.
            if (codet) Text(zeichen, Modifier.padding(start = 8.dp).width(24.dp), color = CLAUDE_ORANGE, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Text(
            when {
                codet && denkt -> "Programmiert · Jarvis arbeitet …"
                codet -> "Programmiert …" + (programmiert[s.id]?.app?.takeIf { it.isNotEmpty() }?.let { " ($it)" } ?: "")
                denkt -> "Jarvis arbeitet …"
                else -> zeitKurz(s.geaendert) + " · " + if (s.anzahl == 1) "1 Nachricht" else "${s.anzahl} Nachrichten"
            },
            color = if (codet) CLAUDE_ORANGE else if (denkt) f.primaer else f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ein Reiter oben in der Leiste: der gewählte als getönte Glasfläche in der Akzentfarbe, die anderen schlicht und leiser. [schmal]: Symbol über dem Namen. */
@Composable
private fun Reiter(text: String, icon: ImageVector, gewaehlt: Boolean, schmal: Boolean, modifier: Modifier = Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    val flaeche = modifier.antippen(rolle = Role.Tab, aktion = aktion)
        .semantics { selected = gewaehlt }
        .clip(RoundedCornerShape(14.dp))
        .then(if (gewaehlt) Modifier.glas(f, 14.dp, erhoeht = 0.4f, toenung = f.primaer) else Modifier)
    val farbe = if (gewaehlt) f.text else f.textLeise
    val gewicht = if (gewaehlt) FontWeight.Bold else FontWeight.Medium
    if (schmal) {
        Column(flaeche.padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (gewaehlt) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
            Text(text, Modifier.padding(top = 2.dp), color = farbe, fontSize = 13.sp, fontWeight = gewicht, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Row(flaeche.padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (gewaehlt) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
            Text(text, Modifier.padding(start = 6.dp), color = farbe, fontSize = 16.sp, fontWeight = gewicht, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Die drei Bereiche der Gesprächsleiste. */
internal enum class LeistenReiter { GESPRAECHE, PROGRAMMIERUNG, PROJEKTE }

/** Der Reiter, zu dem das offene Gespräch gehört: Programmiergespräche unter „Programmierung“, sonst sein Projekt oder „Gespräche“. */
internal fun reiterFuer(programmierung: Boolean, projekt: String): LeistenReiter = when {
    programmierung -> LeistenReiter.PROGRAMMIERUNG
    projekt.isNotEmpty() -> LeistenReiter.PROJEKTE
    else -> LeistenReiter.GESPRAECHE
}

internal data class LeistenListen(val gespraeche: List<Sitzung>, val programmierung: List<Sitzung>)

/**
 * Verteilt die Gespräche auf die Reiter, jedes in genau einen der beiden: Programmiergespräche nur unter „Programmierung“
 * (auch die eines Projekts, die zusätzlich in ihrem Projekt stehen), alle übrigen unter „Gespräche“. Gespräche eines Projekts
 * stehen nur in dessen Bereich; eines mit unbekanntem Projekt (Datei verloren) bleibt unter „Gespräche“, damit es nirgends verschwindet.
 */
internal fun aufteilen(sitzungen: List<Sitzung>, projektIds: Set<String>) = LeistenListen(
    gespraeche = sitzungen.filter { !it.programmierung && (it.projekt.isEmpty() || it.projekt !in projektIds) },
    programmierung = sitzungen.filter { it.programmierung },
)

/** Heute, Gestern, Letzte 7 Tage, Diesen Monat, Älter: so findet Frank ein Gespräch nach dem Gefühl für die Zeit. */
private fun gruppiert(sitzungen: List<Sitzung>): List<Pair<String, List<Sitzung>>> {
    val heute = java.time.LocalDate.now()
    val zone = java.time.ZoneId.systemDefault()
    return sitzungen.groupBy { s ->
        val tag = java.time.Instant.ofEpochMilli(s.geaendert).atZone(zone).toLocalDate()
        when {
            tag == heute -> "Heute"
            tag == heute.minusDays(1) -> "Gestern"
            tag.isAfter(heute.minusDays(7)) -> "Letzte 7 Tage"
            tag.isAfter(heute.minusDays(30)) -> "Letzte 30 Tage"
            else -> "Älter"
        }
    }.toList()
}

/**
 * Schublade für den Jarvis-Reiter, über Reiterwechsel hinweg dieselbe. Neu angelegt, sobald sich die Fensterbreite ändert (Start aus der Sprachblase, Fold auf- oder zuklappen): Material3 1.3
 * verankert die Leiste sonst nach der alten Breite und lässt sie ein Stück im Bild oder halb geöffnet stehen.
 */
@Composable
fun rememberGespraecheSchublade(fensterBreite: Int): DrawerState = remember(fensterBreite) { DrawerState(DrawerValue.Closed) }
