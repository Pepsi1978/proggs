package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Sitzung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.glas
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Seitenleiste mit allen gespeicherten Gesprächen, wie in Geniale Ideen: von links nach rechts wischen öffnet sie,
 * zurückwischen oder die Zurück-Geste schließt sie. Tippen öffnet ein Gespräch, lange drücken benennt um oder löscht.
 */
@Composable
fun GespraecheSchublade(vm: AppViewModel, schublade: DrawerState, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    val raum = rememberCoroutineScope()
    BackHandler(enabled = schublade.isOpen) { raum.launch { schublade.close() } }
    LaunchedEffect(schublade.isOpen) { if (schublade.isOpen) vm.sitzungenLaden() }

    ModalNavigationDrawer(
        drawerState = schublade,
        gesturesEnabled = schublade.isOpen && !schublade.isAnimationRunning,
        scrimColor = Color.Black.copy(alpha = if (f.dunkel) 0.55f else 0.32f),
        // Waagerechtes Wischen irgendwo im Gespräch öffnet die Leiste (nicht nur am Rand); senkrechtes Scrollen bleibt der Liste.
        modifier = Modifier.pointerInput(schublade) {
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
                    if (abs(weg.y) > viewConfiguration.touchSlop && abs(weg.y) >= abs(weg.x)) break
                    if (abs(weg.x) > viewConfiguration.touchSlop) {
                        if ((schliessen && weg.x >= 0f) || (!schliessen && weg.x <= 0f)) break
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
                vm,
                aufNeu = { vm.neuesGespraech(); raum.launch { schublade.close() } },
                aufWahl = { vm.sitzungOeffnen(it); raum.launch { schublade.close() } },
            )
        },
        content = inhalt,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Leiste(vm: AppViewModel, aufNeu: () -> Unit, aufWahl: (String) -> Unit) {
    val f = LocalFarben.current
    var bearbeiten by remember { mutableStateOf<Sitzung?>(null) }
    var umbenennen by remember { mutableStateOf<Sitzung?>(null) }
    var loeschen by remember { mutableStateOf<Sitzung?>(null) }
    val gruppen = remember(vm.sitzungen) { gruppiert(vm.sitzungen) }

    ModalDrawerSheet(
        drawerContainerColor = if (f.dunkel) Color(0xFA0A1424) else Color(0xFAF2F7FD),
        drawerContentColor = f.text,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Forum, null, tint = f.primaer, modifier = Modifier.size(24.dp))
                Text("Gespräche", Modifier.padding(start = 10.dp), color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Knopf("Neues Gespräch", Modifier.fillMaxWidth(), icon = Icons.Rounded.AddComment, aktion = aufNeu)
            if (vm.sitzungen.isEmpty()) {
                Text(
                    "Noch keine gespeicherten Gespräche. Jedes Gespräch mit Jarvis landet von selbst hier, auch die mit „Hey Jarvis“.",
                    Modifier.padding(horizontal = 6.dp, vertical = 20.dp), color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                gruppen.forEach { (gruppe, eintraege) ->
                    item(key = "g:$gruppe") {
                        Text(gruppe.uppercase(), Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp), color = f.textSchwach, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                    }
                    items(eintraege, key = { it.id }) { s ->
                        val gewaehlt = s.id == vm.sitzungId
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .then(if (gewaehlt) Modifier.glas(f, 14.dp, erhoeht = 0.4f, toenung = f.primaer) else Modifier)
                                .combinedClickable(onClick = { aufWahl(s.id) }, onLongClick = { bearbeiten = s }, onClickLabel = "Öffnen", onLongClickLabel = "Umbenennen oder löschen")
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(s.titel.ifBlank { "Gespräch" }, color = if (gewaehlt) f.primaer else f.text, fontSize = 15.sp, fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                            Text(zeitKurz(s.geaendert) + " · " + if (s.anzahl == 1) "1 Nachricht" else "${s.anzahl} Nachrichten", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            Text("Lange drücken: umbenennen oder löschen", Modifier.padding(6.dp, 8.dp), color = f.textSchwach, fontSize = 12.sp)
        }
    }

    bearbeiten?.let { s ->
        AlertDialog(
            onDismissRequest = { bearbeiten = null },
            title = { Text(s.titel.ifBlank { "Gespräch" }, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).combinedClickable(onClick = { umbenennen = s; bearbeiten = null }).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, null, tint = f.primaer)
                        Text("Umbenennen", Modifier.padding(start = 12.dp), fontSize = 16.sp)
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

    umbenennen?.let { s ->
        var name by remember(s.id) { mutableStateOf(s.titel) }
        AlertDialog(
            onDismissRequest = { umbenennen = null },
            title = { Text("Gespräch umbenennen") },
            text = { Eingabe(name, { name = it }, "Name des Gesprächs", Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.sitzungUmbenennen(s.id, name); umbenennen = null }, enabled = name.isNotBlank()) { Text("Speichern") } },
            dismissButton = { TextButton(onClick = { umbenennen = null }) { Text("Abbrechen") } },
        )
    }

    loeschen?.let { s ->
        AlertDialog(
            onDismissRequest = { loeschen = null },
            title = { Text("Gespräch löschen?") },
            text = { Text("„${s.titel.ifBlank { "Gespräch" }}“ wird endgültig gelöscht.") },
            confirmButton = { TextButton(onClick = { vm.sitzungLoeschen(s.id); loeschen = null }) { Text("Löschen", color = f.gefahr) } },
            dismissButton = { TextButton(onClick = { loeschen = null }) { Text("Abbrechen") } },
        )
    }
}

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

/** Schublade für den Jarvis-Reiter, über Reiterwechsel hinweg dieselbe. */
@Composable
fun rememberGespraecheSchublade(): DrawerState = rememberDrawerState(DrawerValue.Closed)
