package de.frank.jarvis.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.listen.Eintrag
import de.frank.jarvis.listen.Gruppierung
import de.frank.jarvis.listen.Liste
import de.frank.jarvis.listen.ListenArt
import de.frank.jarvis.listen.ListenWidget
import de.frank.jarvis.listen.ListenWidgetEmpfaenger
import de.frank.jarvis.listen.Listen
import de.frank.jarvis.listen.Prioritaet
import de.frank.jarvis.listen.Sortierung
import de.frank.jarvis.listen.Zeile
import de.frank.jarvis.ui.theme.LocalBewegung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import de.frank.module.draganddrop.ReorderAutoScroll
import de.frank.module.draganddrop.rememberReorderState
import de.frank.module.draganddrop.reorderHandle
import de.frank.module.draganddrop.reorderItem
import de.frank.module.draganddrop.reorderViewport

/** Ob die Listen im Vollbild offen sind und welche Liste (null = Übersicht). Geöffnet über die Leiste oder das Widget. */
object ListenAnsicht {
    var offen by mutableStateOf(false)
        private set
    var liste by mutableStateOf<Long?>(null)
    /** Aus dem Widget mit „+“: gleich das Eingabefeld öffnen. */
    var eintippen by mutableStateOf(false)

    fun oeffnen(liste: Long? = null, eintippen: Boolean = false) {
        this.liste = liste?.takeIf { it != 0L }
        this.eintippen = eintippen && this.liste != null
        offen = true
    }

    fun schliessen() { offen = false; liste = null; eintippen = false }
}

/** Farben der Listen (Punkt in der Übersicht, Akzent im Kopf). */
private val LISTEN_FARBEN = listOf(Color(0xFF4FC8FF), Color(0xFF4FE0A0), Color(0xFFFFB547), Color(0xFFFF6B6B), Color(0xFFB98CFF), Color(0xFFFF8AD8), Color(0xFF8ADFFF), Color(0xFFE3B341))
/** Prioritäten wie im Widget: keine, niedrig (blau), mittel (gelb), hoch (rot). */
private fun prioFarbe(p: Int, f: de.frank.jarvis.ui.theme.Farben): Color = if (p <= 0) f.textLeise else Color(ListenWidget.PRIO[p.coerceAtMost(3)])

@Composable
fun ListenBildschirm(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val stand by Listen.stand.collectAsState()
    LaunchedEffect(Unit) { Listen.sicher(context) }
    val offen = ListenAnsicht.liste?.let { id -> stand.listen.firstOrNull { it.id == id } }
    // Liste auf dem anderen Gerät gelöscht, während sie hier offen war: zurück zur Übersicht.
    LaunchedEffect(offen, ListenAnsicht.liste, stand.listen.size) { if (ListenAnsicht.liste != null && offen == null && stand.listen.isNotEmpty()) ListenAnsicht.liste = null }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            if (offen == null) Uebersicht() else ListeAnsehen(offen)
        }
    }
}

// ---------------------------------------------------------------- Übersicht

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Uebersicht() {
    val f = LocalFarben.current
    val context = LocalContext.current
    val stand by Listen.stand.collectAsState()
    var neu by remember { mutableStateOf(false) }
    BackHandler { ListenAnsicht.schliessen() }
    Kopf("Listen", zurueck = { ListenAnsicht.schliessen() }) {
        Rundknopf(Icons.Rounded.Widgets, "Widget auf den Startbildschirm") { widgetAnheften(context) }
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (stand.listen.isEmpty()) item(key = "leer") {
            Karte {
                Text("Noch keine Listen", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Lege unten eine an oder sag einfach: „Hey Jarvis, füge Zahnpasta meiner Einkaufsliste hinzu.“", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        items(stand.listen, key = { it.id }) { l ->
            val offenZahl = stand.offen(l.id)
            val gesamt = stand.eintraegeVon(l.id).size
            Row(
                Modifier.fillMaxWidth().glas(f).antippen { ListenAnsicht.liste = l.id }.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).background(LISTEN_FARBEN[l.farbe.mod(LISTEN_FARBEN.size)].copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(ListenArt.von(l.art).symbol, fontSize = 20.sp)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(l.name, color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ListenArt.von(l.art).anzeige} · " + (if (gesamt == 0) "leer" else "$offenZahl offen von $gesamt"), color = f.textLeise, fontSize = 13.sp)
                }
                Text(offenZahl.toString(), color = LISTEN_FARBEN[l.farbe.mod(LISTEN_FARBEN.size)], fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Knopf("Neue Liste", Modifier.weight(1f), Icons.Rounded.Add) { neu = true }
    }
    if (neu) ListeDialog(null) { neu = false }
}

private fun widgetAnheften(context: android.content.Context) {
    val m = AppWidgetManager.getInstance(context)
    if (android.os.Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported) m.requestPinAppWidget(ComponentName(context, ListenWidgetEmpfaenger::class.java), null, null)
}

@Composable
private fun Kopf(titel: String, zurueck: () -> Unit, rechts: @Composable () -> Unit = {}) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).antippen(aktion = zurueck), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
        }
        Text(titel, Modifier.weight(1f).padding(start = 2.dp), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        rechts()
    }
}

// ---------------------------------------------------------------- Eine Liste

/** Ein ListenBlock der Liste: ein Gruppenkopf oder ein Eintrag der obersten Ebene mit seinen Unterpunkten (wird als Ganzes verschoben). */
private data class ListenBlock(val schluessel: Long, val zeilen: List<Zeile>, val verschiebbar: Boolean)

private fun bloecke(zeilen: List<Zeile>, manuell: Boolean): List<ListenBlock> {
    val aus = ArrayList<ListenBlock>()
    var erledigtTeil = false
    zeilen.forEachIndexed { i, z ->
        when {
            z.kopf != null -> { erledigtTeil = z.kopf == "Erledigt"; aus += ListenBlock(-1L - i, listOf(z), false) }
            z.tiefe == 0 -> aus += ListenBlock(z.eintrag!!.id, listOf(z), manuell && !erledigtTeil && !z.eintrag.erledigt)
            aus.isNotEmpty() -> aus[aus.lastIndex] = aus.last().copy(zeilen = aus.last().zeilen + z)
        }
    }
    return aus
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ListeAnsehen(l: Liste) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val stand by Listen.stand.collectAsState()
    BackHandler { ListenAnsicht.liste = null }
    var menue by remember { mutableStateOf(false) }
    var sortMenue by remember { mutableStateOf(false) }
    var einstellen by remember { mutableStateOf(false) }
    var loeschenFrage by remember { mutableStateOf(false) }
    var bearbeite by remember { mutableStateOf<Long?>(null) }
    var unter by remember { mutableStateOf<Eintrag?>(null) }
    var eingabe by rememberSaveable(l.id) { mutableStateOf("") }
    val fokus = remember { FocusRequester() }

    val manuell = l.sortierung == Sortierung.MANUELL.kennung && l.gruppierung == Gruppierung.KEINE.kennung
    val zeilen = remember(stand, l) { Listen.ansicht(stand, l) }
    var reihe by remember(zeilen, manuell) { mutableStateOf(bloecke(zeilen, manuell)) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState, l.id)
    ReorderAutoScroll(reorder)
    LaunchedEffect(ListenAnsicht.eintippen) { if (ListenAnsicht.eintippen) { runCatching { fokus.requestFocus() }; ListenAnsicht.eintippen = false } }

    Kopf("${ListenArt.von(l.art).symbol} ${l.name}", zurueck = { ListenAnsicht.liste = null }) {
        Box {
            Rundknopf(Icons.AutoMirrored.Rounded.Sort, "Sortieren und gruppieren") { sortMenue = true }
            Menue(sortMenue, { sortMenue = false }) {
                Text("Sortieren", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                Sortierung.entries.forEach { s ->
                    Wahl(s.anzeige, l.sortierung == s.kennung) { Listen.listeAendern(context, l.id) { it.copy(sortierung = s.kennung) }; sortMenue = false }
                }
                HorizontalDivider(color = f.rand)
                Text("Gruppieren", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                Gruppierung.entries.forEach { g ->
                    Wahl(g.anzeige, l.gruppierung == g.kennung) { Listen.listeAendern(context, l.id) { it.copy(gruppierung = g.kennung) }; sortMenue = false }
                }
            }
        }
        Box {
            Rundknopf(Icons.Rounded.MoreVert, "Mehr") { menue = true }
            Menue(menue, { menue = false }) {
                Wahl(if (l.erledigteZeigen) "Erledigte ausblenden" else "Erledigte zeigen", false) { Listen.listeAendern(context, l.id) { it.copy(erledigteZeigen = !it.erledigteZeigen) }; menue = false }
                Wahl("Erledigte löschen", false) { Listen.erledigteLoeschen(context, l.id); menue = false }
                Wahl("Liste bearbeiten", false) { einstellen = true; menue = false }
                Wahl("Widget auf den Startbildschirm", false) { widgetAnheften(context); menue = false }
                Wahl("Liste löschen", false, farbe = f.gefahr) { loeschenFrage = true; menue = false }
            }
        }
    }

    val reduziert = !LocalBewegung.current
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth().reorderViewport(reorder, { reihe.map { it.schluessel } }, { von, nach ->
            val r = reihe.toMutableList()
            // Nur zwischen offenen Einträgen verschieben, nie über Gruppenköpfe oder in „Erledigt“.
            if (r.getOrNull(nach)?.verschiebbar == true) { r.add(nach, r.removeAt(von)); reihe = r }
        }, { Listen.ordnen(context, reihe.filter { it.verschiebbar }.map { it.schluessel }) }, reduziert),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (reihe.isEmpty()) item(key = -999_999L) {
            Karte {
                Text("Die Liste ist leer", color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Tippe unten etwas ein, etwa „Milch !hoch #kühl“ (Priorität mit !, Tag mit #), oder sag es Jarvis.", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        items(reihe, key = { it.schluessel }) { b ->
            val kopf = b.zeilen.first().kopf
            if (kopf != null) {
                Text("$kopf  ${b.zeilen.first().kopfAnzahl}", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 8.dp))
            } else {
                Column(reorderItem(reorder, b.schluessel, reduziert).fillMaxWidth().glas(f, 18.dp, erhoeht = if (reorder.isDragging(b.schluessel)) 1.6f else 0.7f).padding(vertical = 4.dp)) {
                    b.zeilen.forEach { z ->
                        EintragZeile(z, griff = if (b.verschiebbar && z.tiefe == 0) reorderHandle(reorder, b.schluessel) else null) { bearbeite = z.eintrag!!.id }
                    }
                }
            }
        }
    }

    unter?.let { u ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Unterpunkt von „${u.titel}“", Modifier.weight(1f), color = f.primaer, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Aufheben", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.antippen { unter = null }.padding(6.dp))
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
        val hinzufuegen = {
            if (eingabe.isNotBlank()) {
                val s = Listen.schnell(eingabe)
                Listen.hinzufuegen(context, l.id, s.titel, unter?.id ?: 0L, s.prioritaet, tags = s.tags)
                eingabe = ""
            }
        }
        Eingabe(eingabe, { eingabe = it }, if (unter != null) "Unterpunkt eintragen …" else "Neuer Eintrag … (!hoch, #tag)", Modifier.weight(1f).focusRequester(fokus), senden = hinzufuegen)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(52.dp).knopf3d(f.primaer, f.tertiaer, 26.dp, f.dunkel).antippen(aktion = hinzufuegen), contentAlignment = Alignment.Center) {
            Icon(if (eingabe.isBlank()) Icons.Rounded.Add else Icons.AutoMirrored.Rounded.Send, "Hinzufügen", tint = f.aufPrimaer, modifier = Modifier.size(24.dp))
        }
    }

    bearbeite?.let { id ->
        val e = stand.eintraege.firstOrNull { it.id == id }
        if (e == null) bearbeite = null
        else EintragDialog(e, l, stand.listen, beiUnterpunkt = { unter = e; bearbeite = null; runCatching { fokus.requestFocus() } }) { bearbeite = null }
    }
    if (einstellen) ListeDialog(l) { einstellen = false }
    if (loeschenFrage) AlertDialog(
        onDismissRequest = { loeschenFrage = false }, containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Liste löschen?") },
        text = { Text("„${l.name}“ mit allen ${stand.eintraegeVon(l.id).size} Einträgen löschen, auch auf dem anderen Gerät.") },
        confirmButton = { TextButton({ Listen.listeLoeschen(context, l.id); loeschenFrage = false; ListenAnsicht.liste = null }) { Text("Löschen", color = f.gefahr) } },
        dismissButton = { TextButton({ loeschenFrage = false }) { Text("Abbrechen", color = f.text) } },
    )
}

@Composable
private fun Wahl(text: String, gewaehlt: Boolean, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    DropdownMenuItem(
        { Text(text, color = farbe ?: if (gewaehlt) f.primaer else f.text, fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Normal) }, aktion,
        trailingIcon = if (gewaehlt) { { Icon(Icons.Rounded.CheckCircle, null, tint = f.primaer, modifier = Modifier.size(18.dp)) } } else null,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EintragZeile(z: Zeile, griff: Modifier?, antippen: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val e = z.eintrag ?: return
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).antippen(haptik = false, aktion = antippen).padding(start = (8 + 24 * z.tiefe).dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).antippen { Listen.abhaken(context, e.id, !e.erledigt) }, contentAlignment = Alignment.Center) {
            Icon(if (e.erledigt) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (e.erledigt) "Wieder öffnen" else "Abhaken",
                tint = if (e.erledigt) f.erfolg else prioFarbe(e.prioritaet, f), modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text(e.titel, color = if (e.erledigt) f.textSchwach else f.text, fontSize = 16.sp, lineHeight = 21.sp,
                textDecoration = if (e.erledigt) TextDecoration.LineThrough else null)
            val info = buildList {
                if (z.kinderGesamt > 0) add("${z.kinderErledigt}/${z.kinderGesamt}")
                if (e.faellig.isNotEmpty()) add(datumKurz(e.faellig))
                e.tags.forEach { add("#$it") }
                if (e.abschnitt.isNotEmpty()) add(e.abschnitt)
                if (e.notiz.isNotEmpty()) add(e.notiz.lineSequence().first().take(60))
            }
            if (info.isNotEmpty()) Text(info.joinToString("  ·  "), color = f.textLeise, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (e.angeheftet) Icon(Icons.Rounded.PushPin, "Angeheftet", tint = f.primaer, modifier = Modifier.padding(end = 4.dp).size(16.dp))
        if (e.prioritaet > 0 && !e.erledigt) Icon(Icons.Rounded.Flag, "Priorität ${Prioritaet.name(e.prioritaet)}", tint = prioFarbe(e.prioritaet, f), modifier = Modifier.padding(end = 4.dp).size(18.dp))
        if (griff != null) Box(griff.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DragIndicator, "Gedrückt halten und ziehen", tint = f.textSchwach, modifier = Modifier.size(22.dp))
        }
    }
}

private fun datumKurz(iso: String): String = runCatching {
    val d = java.time.LocalDate.parse(iso)
    val heute = java.time.LocalDate.now()
    when (d) { heute -> "heute"; heute.plusDays(1) -> "morgen"; heute.minusDays(1) -> "gestern"; else -> "%d.%d.".format(d.dayOfMonth, d.monthValue) }
}.getOrDefault(iso)

// ---------------------------------------------------------------- Dialoge

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chip(text: String, gewaehlt: Boolean, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    val c = farbe ?: f.primaer
    Box(
        Modifier.padding(end = 6.dp, bottom = 6.dp).background(if (gewaehlt) c.copy(alpha = 0.25f) else f.flaeche, RoundedCornerShape(14.dp)).antippen(aktion = aktion)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(text, color = if (gewaehlt) c else f.text, fontSize = 14.sp, fontWeight = if (gewaehlt) FontWeight.SemiBold else FontWeight.Normal) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EintragDialog(e: Eintrag, l: Liste, listen: List<Liste>, beiUnterpunkt: () -> Unit, schliessen: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    var titel by remember(e.id) { mutableStateOf(e.titel) }
    var notiz by remember(e.id) { mutableStateOf(e.notiz) }
    var prio by remember(e.id) { mutableStateOf(e.prioritaet) }
    var faellig by remember(e.id) { mutableStateOf(e.faellig) }
    var tags by remember(e.id) { mutableStateOf(e.tags.joinToString(" ") { "#$it" }) }
    var abschnitt by remember(e.id) { mutableStateOf(e.abschnitt) }
    var angeheftet by remember(e.id) { mutableStateOf(e.angeheftet) }
    var verschieben by remember { mutableStateOf(false) }
    fun speichern() {
        val neuTags = tags.split(Regex("[\\s,]+")).map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
        if (abschnitt.isNotBlank() && abschnitt.trim() !in l.abschnitte) Listen.listeAendern(context, l.id) { it.copy(abschnitte = it.abschnitte + abschnitt.trim()) }
        Listen.aendern(context, e.id) { it.copy(titel = titel.trim().ifEmpty { it.titel }, notiz = notiz.trim(), prioritaet = prio, faellig = faellig, tags = neuTags,
            abschnitt = if (e.eltern == 0L) abschnitt.trim() else it.abschnitt, angeheftet = angeheftet) }
        schliessen()
    }
    AlertDialog(
        onDismissRequest = { speichern() }, containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Eintrag") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Eingabe(titel, { titel = it }, "Titel", Modifier.fillMaxWidth()) }
                item { Eingabe(notiz, { notiz = it }, "Notiz", Modifier.fillMaxWidth(), einzeilig = false) }
                item {
                    Text("Priorität", color = f.textLeise, fontSize = 12.sp)
                    FlowRow(Modifier.padding(top = 4.dp)) {
                        (0..3).forEach { p -> Chip(Prioritaet.name(p).replaceFirstChar { it.uppercase() }, prio == p, if (p > 0) prioFarbe(p, f) else null) { prio = p } }
                    }
                }
                item {
                    Text("Fällig", color = f.textLeise, fontSize = 12.sp)
                    val heute = java.time.LocalDate.now()
                    FlowRow(Modifier.padding(top = 4.dp)) {
                        Chip("Ohne", faellig.isEmpty()) { faellig = "" }
                        Chip("Heute", faellig == heute.toString()) { faellig = heute.toString() }
                        Chip("Morgen", faellig == heute.plusDays(1).toString()) { faellig = heute.plusDays(1).toString() }
                        Chip("In 7 Tagen", faellig == heute.plusDays(7).toString()) { faellig = heute.plusDays(7).toString() }
                    }
                    if (faellig.isNotEmpty()) Text("Fällig ${datumKurz(faellig)} ($faellig)", color = f.textLeise, fontSize = 12.sp)
                }
                item { Eingabe(tags, { tags = it }, "Tags (#kühl #drogerie)", Modifier.fillMaxWidth()) }
                if (e.eltern == 0L) item {
                    Eingabe(abschnitt, { abschnitt = it }, "Abschnitt (etwa Obst, Drogerie)", Modifier.fillMaxWidth())
                    if (l.abschnitte.isNotEmpty()) FlowRow(Modifier.padding(top = 6.dp)) {
                        l.abschnitte.forEach { a -> Chip(a, abschnitt == a) { abschnitt = if (abschnitt == a) "" else a } }
                    }
                }
                item { FlowRow { Chip(if (angeheftet) "📌 Angeheftet" else "Oben anheften", angeheftet) { angeheftet = !angeheftet } } }
                item {
                    FlowRow {
                        Chip("+ Unterpunkt", false) { speichern(); beiUnterpunkt() }
                        if (e.eltern != 0L) Chip("Auf oberste Ebene", false) { Listen.einhaengen(context, e.id, 0L); schliessen() }
                        if (listen.size > 1) Chip("Verschieben …", verschieben) { verschieben = !verschieben }
                    }
                    if (verschieben) FlowRow(Modifier.padding(top = 4.dp)) {
                        listen.filter { it.id != l.id }.forEach { z -> Chip("${ListenArt.von(z.art).symbol} ${z.name}", false) { Listen.verschieben(context, e.id, z.id); schliessen() } }
                    }
                }
            }
        },
        confirmButton = { TextButton({ speichern() }) { Text("Fertig", color = f.primaer) } },
        dismissButton = {
            TextButton({ Listen.loeschen(context, e.id); schliessen() }) {
                Icon(Icons.Rounded.Delete, null, tint = f.gefahr, modifier = Modifier.size(18.dp)); Text(" Löschen", color = f.gefahr)
            }
        },
    )
}

/** Neue Liste anlegen ([l] = null) oder Name, Art, Farbe und Abschnitte einer Liste ändern. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeDialog(l: Liste?, schliessen: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    var name by remember { mutableStateOf(l?.name.orEmpty()) }
    var art by remember { mutableStateOf(l?.art ?: ListenArt.EINKAUF.kennung) }
    var farbe by remember { mutableStateOf(l?.farbe ?: (Listen.stand.value.listen.size % LISTEN_FARBEN.size)) }
    var abschnitte by remember { mutableStateOf(l?.abschnitte?.joinToString(", ").orEmpty()) }
    AlertDialog(
        onDismissRequest = schliessen, containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text(if (l == null) "Neue Liste" else "Liste bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eingabe(name, { name = it }, "Name (etwa Einkaufsliste)", Modifier.fillMaxWidth())
                Text("Art", color = f.textLeise, fontSize = 12.sp)
                FlowRow { ListenArt.entries.forEach { a -> Chip("${a.symbol} ${a.anzeige}", art == a.kennung) { art = a.kennung } } }
                Text("Farbe", color = f.textLeise, fontSize = 12.sp)
                FlowRow {
                    LISTEN_FARBEN.forEachIndexed { i, c ->
                        Box(Modifier.padding(end = 8.dp, bottom = 6.dp).size(if (farbe == i) 34.dp else 28.dp).background(c, CircleShape).antippen { farbe = i })
                    }
                }
                Eingabe(abschnitte, { abschnitte = it }, "Abschnitte, mit Komma (Obst, Kühlregal …)", Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton({
                val n = name.trim()
                val ab = abschnitte.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                if (n.isNotEmpty()) {
                    if (l == null) {
                        val neu = Listen.listeAnlegen(context, n, art, farbe)
                        if (ab.isNotEmpty()) Listen.listeAendern(context, neu.id) { it.copy(abschnitte = ab) }
                        ListenAnsicht.liste = neu.id
                    } else Listen.listeAendern(context, l.id) { it.copy(name = n, art = art, farbe = farbe, abschnitte = ab) }
                    schliessen()
                }
            }) { Text(if (l == null) "Anlegen" else "Speichern", color = f.primaer) }
        },
        dismissButton = { TextButton(schliessen) { Text("Abbrechen", color = f.text) } },
    )
}
