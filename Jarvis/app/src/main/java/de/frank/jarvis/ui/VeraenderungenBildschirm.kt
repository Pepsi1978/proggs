package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.veraenderung.Veraenderung
import de.frank.jarvis.veraenderung.Veraenderungen
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas

/**
 * Reiter „Veränderungen“: Franks wichtige Veränderungen im Leben. Oben zwei Reiter: „Aktuell“ (angenommen, wird gerade umgesetzt;
 * Standard beim Öffnen) und „Offen“ (neu eingesprochen, steht noch an; Detail mit „Annehmen“). In „Aktuell“ (Einzeile, Kreis hakt ab), unten aufklappbar
 * „Erledigt“ (Zurückholen). Lange drücken und ziehen ordnet „Aktuell“ um (geräteübergreifend gespeichert). Antippen öffnet die genaue Beschreibung mit Papierkorb. Neue Einträge kommen nur per
 * Sprache an Jarvis (Werkzeug `veraenderung`).
 */
@Composable
fun VeraenderungenBildschirm() {
    val context = LocalContext.current
    val alle by Veraenderungen.stand.collectAsState()
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Veraenderungen.sicher(context) } }
    var offenId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Beim Öffnen des Bereichs immer „Aktuell“; der Reiter bleibt erhalten, solange eine Veränderung im Detail offen ist.
    var zeigtOffene by rememberSaveable { mutableStateOf(false) }
    val offen = offenId?.let { id -> alle.firstOrNull { it.id == id } }
    // Auf dem anderen Gerät gelöscht, während der Eintrag hier offen war: zurück zur Liste.
    LaunchedEffect(offenId, offen) { if (offenId != null && offen == null && alle.isNotEmpty()) offenId = null }
    Column(Modifier.fillMaxSize()) {
        // Zurück aus dem Detail in den Reiter, in dem die Veränderung jetzt steht (nach „Annehmen“ oder Zurücklegen nach „Offen“).
        if (offen != null) Detail(offen) { if (!offen.erledigt && zeigtOffene == offen.angenommen) zeigtOffene = !offen.angenommen; offenId = null }
        // Das Gedächtnis hängt nicht am gezeigten Reiter: Jedes Verschieben zwischen „Aktuell“ und „Offen“ schreibt und frischt es auf.
        else Uebersicht(alle, zeigtOffene, { zeigtOffene = it }) { offenId = it }
    }
}

/** Zwei Reiter wie in Geniale Ideen: links „Aktuell“, daneben „Offen“, je mit Anzahl. */
@Composable
private fun ReiterWahl(zeigtOffene: Boolean, aktuell: Int, offene: Int, waehlen: (Boolean) -> Unit) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(form).background(f.flaeche).border(1.dp, f.primaer.copy(alpha = 0.25f), form).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to "Aktuell ($aktuell)", true to "Offen ($offene)").forEach { (istOffen, text) ->
            val gewaehlt = istOffen == zeigtOffene
            val farbe by animateColorAsState(if (gewaehlt) f.primaer else Color.Transparent, label = "reiter")
            Box(Modifier.weight(1f).semantics { role = Role.Tab; selected = gewaehlt }.clip(RoundedCornerShape(12.dp)).background(farbe).antippen { waehlen(istOffen) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(text, color = if (gewaehlt) f.aufPrimaer else f.textLeise, fontSize = 15.sp, fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Uebersicht(alle: List<Veraenderung>, zeigtOffene: Boolean, reiterWaehlen: (Boolean) -> Unit, oeffnen: (Long) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val haptik = LocalHapticFeedback.current
    val angenommen = alle.filter { it.laeuft }
    val offene = alle.filter { it.stehtAn }
    val gruppe = if (zeigtOffene) offene else angenommen
    // Erledigte erscheinen in dem Reiter, aus dem sie kamen; Zurückholen bringt sie genau dorthin.
    val erledigt = alle.filter { it.erledigt && it.angenommen != zeigtOffene }.sortedByDescending { it.erledigtAm }
    Text("Veränderungen", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    ReiterWahl(zeigtOffene, angenommen.size, offene.size, reiterWaehlen)
    // Je Reiter eine eigene Liste: Beim Wechsel erscheinen die Karten sofort samt Glasschatten, ohne Einblend-Animation,
    // deren Alpha-Ebene den Schatten außerhalb der Karte bis zum Ende abschneiden würde.
    androidx.compose.runtime.key(zeigtOffene) {
        // Während des Ziehens eigene Reihenfolge, beim Loslassen einmal gespeichert; danach wieder der Stand aus der Datei.
        var lokal by remember { mutableStateOf<List<Veraenderung>?>(null) }
        val gezeigt = lokal ?: gruppe
        val listenStand = rememberLazyListState()
        val ordnen = rememberReorderableLazyListState(listenStand) { von, nach ->
            val vonId = von.key as? Long ?: return@rememberReorderableLazyListState
            val nachId = nach.key as? Long ?: return@rememberReorderableLazyListState
            val l = (lokal ?: gruppe).toMutableList()
            val a = l.indexOfFirst { it.id == vonId }
            val b = l.indexOfFirst { it.id == nachId }
            if (a < 0 || b < 0) return@rememberReorderableLazyListState
            l.add(b, l.removeAt(a))
            lokal = l
            haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listenStand, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (gruppe.isEmpty() && zeigtOffene) item(key = "leer_offen") {
                Karte {
                    Text("Keine offene Veränderung", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Was du Jarvis als Veränderung einsprichst, landet zuerst hier. Öffne sie und tippe auf „Annehmen“, sobald du sie umsetzt.",
                        color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            else if (gruppe.isEmpty()) item(key = "leer") {
                Karte {
                    Text("Keine laufende Veränderung", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sag Jarvis, was du anders machen willst und warum, etwa: „Hey Jarvis, nimm unter Veränderungen auf, dass ich ab sofort mit dem Sonnenaufgang aufstehe.“ Sie landet unter „Offen“; mit „Annehmen“ kommt sie hierher.",
                        color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            items(gezeigt, key = { it.id }) { v ->
                ReorderableItem(ordnen, key = v.id) { zieht ->
                    val gross by animateFloatAsState(if (zieht) 1.035f else 1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "gross")
                    val hoch by animateFloatAsState(if (zieht) 1.5f else 0.7f, spring(stiffness = Spring.StiffnessMediumLow), label = "hoch")
                    Zeile(
                        v,
                        Modifier
                            .graphicsLayer { scaleX = gross; scaleY = gross }
                            .longPressDraggableHandle(
                                onDragStarted = { haptik.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragStopped = {
                                    lokal?.let { l -> Veraenderungen.reihenfolge(context, l.map { it.id }) }
                                    lokal = null
                                },
                            ),
                        erhoeht = hoch,
                        oeffnen = { oeffnen(v.id) },
                        gezogen = { ordnen.isAnyItemDragging },
                    ) { Veraenderungen.erledigt(context, v.id, true) }
                }
            }
            if (erledigt.isNotEmpty()) item(key = "erledigt") {
                Bereich("Erledigt", kopf = { Text(erledigt.size.toString(), Modifier.padding(end = 8.dp), color = f.textLeise, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        erledigt.forEach { v ->
                            Row(Modifier.fillMaxWidth().antippen { oeffnen(v.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = f.erfolg, modifier = Modifier.size(22.dp))
                                Text(v.titel, Modifier.weight(1f).padding(start = 12.dp), color = f.textLeise, fontSize = 15.sp, textDecoration = TextDecoration.LineThrough, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Box(Modifier.size(42.dp).antippen { Veraenderungen.erledigt(context, v.id, false) }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, if (zeigtOffene) "In Offen zurückholen" else "In Aktuell zurückholen", tint = f.primaer, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Zeile(v: Veraenderung, modifier: Modifier = Modifier, erhoeht: Float = 0.7f, oeffnen: () -> Unit, gezogen: () -> Boolean, abhaken: () -> Unit) {
    val f = LocalFarben.current
    // Ein langer Druck ohne Bewegung (Ziehen begonnen, nicht bewegt) öffnet und hakt nichts ab.
    Row(modifier.fillMaxWidth().glas(f, 18.dp, erhoeht = erhoeht).antippen(unterdruecken = gezogen, aktion = oeffnen).padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).antippen(unterdruecken = gezogen, aktion = abhaken), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.RadioButtonUnchecked, "Als erledigt abhaken", tint = f.primaer, modifier = Modifier.size(24.dp))
        }
        Text(v.titel, Modifier.weight(1f).padding(vertical = 8.dp), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Detail(v: Veraenderung, zurueck: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    var loeschenFrage by remember { mutableStateOf(false) }
    BackHandler(onBack = zurueck)
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).antippen(aktion = zurueck), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
        }
        Text("Veränderung", Modifier.weight(1f).padding(start = 2.dp), color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Rundknopf(Icons.Rounded.Delete, "Löschen", farbe = f.gefahr) { loeschenFrage = true }
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Karte {
            Text(v.titel, color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Text(
                "Seit ${Veraenderungen.datum(v.erstellt)}" + when { v.erledigt -> " · erledigt am ${Veraenderungen.datum(v.erledigtAm)}"; v.angenommen -> " · aktuell"; else -> " · offen" },
                color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Text(v.beschreibung.ifBlank { "Keine Beschreibung." }, color = f.text, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 12.dp))
        }
        if (v.erledigt) Knopf(if (v.angenommen) "In Aktuell zurückholen" else "In Offen zurückholen", Modifier.fillMaxWidth(), Icons.AutoMirrored.Rounded.Undo) { Veraenderungen.erledigt(context, v.id, false) }
        // Schalter zwischen den Reitern: offen = „Annehmen“ (normal), aktuell = „Angenommen“ (grün mit Haken, Tipp legt zurück nach „Offen“).
        // Er steht in beiden Zuständen an derselben Stelle: Ein doppelter Tipp schaltet nur hin und zurück, trifft nie „Als erledigt abhaken“.
        else {
            if (v.angenommen) Knopf("Angenommen", Modifier.fillMaxWidth(), Icons.Rounded.CheckCircle, farbe = f.erfolg) { Veraenderungen.nachOffen(context, v.id) }
            else Knopf("Annehmen", Modifier.fillMaxWidth(), Icons.Rounded.TaskAlt, haupt = false) { Veraenderungen.annehmen(context, v.id) }
            Knopf("Als erledigt abhaken", Modifier.fillMaxWidth(), Icons.Rounded.RadioButtonUnchecked, haupt = false) { Veraenderungen.erledigt(context, v.id, true); zurueck() }
        }
    }
    if (loeschenFrage) AlertDialog(
        onDismissRequest = { loeschenFrage = false }, containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Veränderung löschen?") },
        text = { Text("„${v.titel}“ endgültig löschen, auch auf dem anderen Gerät.") },
        confirmButton = { TextButton({ loeschenFrage = false; Veraenderungen.loeschen(context, v.id); zurueck() }) { Text("Löschen", color = f.gefahr) } },
        dismissButton = { TextButton({ loeschenFrage = false }) { Text("Abbrechen", color = f.text) } },
    )
}
