package de.frank.ziele.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.ziele.data.Kategorie
import de.frank.ziele.text.Quellen
import de.frank.ziele.data.Sortierung
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import de.frank.ziele.data.ZielStatus
import de.frank.ziele.data.sortiert
import de.frank.ziele.ui.theme.Chip
import de.frank.ziele.ui.theme.LocalFarben
import de.frank.ziele.ui.theme.antippen
import de.frank.ziele.ui.theme.glas
import de.frank.ziele.ui.theme.knopf3d
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailBildschirm(vm: AppViewModel, id: Long) {
    val f = LocalFarben.current
    val ziele by vm.ziele.collectAsState()
    val alle by vm.vorschlaege.collectAsState()
    val stand by vm.auswertungStand.collectAsState()
    val fehler by vm.auswertungFehler.collectAsState()
    val z = ziele.firstOrNull { it.id == id } ?: run { Box(Modifier.fillMaxSize()); return }
    val vorschlaege = alle.filter { it.zielId == id }
    val sichtbar = vorschlaege.filter { !it.verworfen }
    val sortierung = vm.einstellungen.sortierung
    val notwendig = sichtbar.filter { it.gruppe == Kategorie.NOTWENDIG }.sortiert(sortierung)
    val hilfe = sichtbar.filter { it.gruppe == Kategorie.UNTERSTUETZEND }.sortiert(sortierung)
    val verworfen = vorschlaege.filter { it.verworfen }
    val schluesselTitel = vorschlaege.associate { it.schluessel to it.titel }
    val schritt = stand[id]
    var loeschenFragen by remember { mutableStateOf(false) }
    var verworfeneZeigen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RundKnopf(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") { vm.zurueck() }
                Spacer(Modifier.weight(1f))
                RundKnopf(if (vm.sprichtId == "plan_${z.id}") Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, "Plan vorlesen") { vm.planVorlesen(z) }
                RundKnopf(Icons.Rounded.Edit, "Bearbeiten") { vm.bearbeiten(z) }
                RundKnopf(Icons.Rounded.Close, "Löschen", farbe = f.gefahr) { loeschenFragen = true }
            }
        }
        item { Kopf(vm, z, notwendig) }
        item { AuswertenKnopf(vm, z, schritt, fehler[id]) }
        if (z.strategie.isNotBlank()) item {
            Column(Modifier.fillMaxWidth().glas(f, erhoeht = 1f, toenung = f.primaer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DEIN WEG", color = f.primaer, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(z.strategie, color = f.text, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
        if (z.bericht.isNotBlank()) item { Bericht(vm, z) }
        if (sichtbar.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SORTIEREN NACH", color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    ChipReihe { Sortierung.entries.forEach { s -> Chip(s.anzeige, sortierung == s) { vm.sortieren(s) } } }
                    Text(sortierung.erklaerung, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
            item { GruppenKopf(vm, z, Kategorie.NOTWENDIG, notwendig) }
            items(notwendig, key = { it.id }) { v -> VorschlagKarte(vm, v, schluesselTitel, sortierung, notwendig.indexOf(v) + 1) }
            if (hilfe.isNotEmpty()) {
                item { GruppenKopf(vm, z, Kategorie.UNTERSTUETZEND, hilfe) }
                items(hilfe, key = { it.id }) { v -> VorschlagKarte(vm, v, schluesselTitel, sortierung, hilfe.indexOf(v) + 1) }
            }
            if (verworfen.isNotEmpty()) item {
                Row(Modifier.fillMaxWidth().antippen(haptik = false) { verworfeneZeigen = !verworfeneZeigen }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ausgeblendet (${verworfen.size})", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(if (verworfeneZeigen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = f.textSchwach)
                }
            }
            if (verworfeneZeigen) items(verworfen, key = { it.id }) { v ->
                Row(Modifier.fillMaxWidth().glas(f, 14.dp, 0.3f).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(v.titel, color = f.textSchwach, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.Undo, "Wieder einblenden", tint = f.primaer, modifier = Modifier.size(22.dp).antippen { vm.verwerfen(v, false) })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (z.zustand) {
                    ZielStatus.AKTIV -> {
                        Chip("Erreicht 🏆", false) { vm.statusSetzen(z, ZielStatus.ERREICHT) }
                        Chip("Pausieren", false) { vm.statusSetzen(z, ZielStatus.PAUSIERT) }
                    }
                    else -> Chip("Wieder aktiv", false) { vm.statusSetzen(z, ZielStatus.AKTIV) }
                }
                if (sichtbar.any { it.aufgabeId != null }) Chip("Geniale Aufgaben", false, icon = Icons.AutoMirrored.Rounded.OpenInNew) { vm.aufgabenOeffnen() }
            }
        }
    }
    if (loeschenFragen) AlertDialog(
        onDismissRequest = { loeschenFragen = false },
        containerColor = f.hgUnten, titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Ziel löschen?") },
        text = { Text("„${z.titel}“ und alle Vorschläge werden gelöscht. Aufgaben, die schon in Geniale Aufgaben stehen, bleiben dort.") },
        confirmButton = { TextButton(onClick = { loeschenFragen = false; vm.loeschen(z) }) { Text("Löschen", color = f.gefahr) } },
        dismissButton = { TextButton(onClick = { loeschenFragen = false }) { Text("Abbrechen", color = f.primaer) } },
    )
}

@Composable
private fun Kopf(vm: AppViewModel, z: Ziel, notwendig: List<Vorschlag>) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().glas(f, erhoeht = 1.4f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).knopf3d(f.primaer, f.tertiaer, 20.dp, f.dunkel), contentAlignment = Alignment.Center) { Text(z.symbol, fontSize = 32.sp) }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(z.titel, color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Etikett(z.zustand.emoji + " " + z.zustand.anzeige, f.primaer)
                    z.frist?.let { Etikett(fristText(it, vm.heute), if (it < vm.heute && z.zustand == ZielStatus.AKTIV) f.gefahr else f.primaer) }
                }
            }
            if (notwendig.isNotEmpty()) Fortschrittsring(notwendig.count { it.erledigt } / notwendig.size.toFloat(), 58.dp)
        }
        if (z.beschreibung.isNotBlank()) Text(z.beschreibung, color = f.text, fontSize = 15.sp, lineHeight = 21.sp)
        if (z.warum.isNotBlank()) Row {
            Text("Warum: ", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(z.warum, color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun AuswertenKnopf(vm: AppViewModel, z: Ziel, schritt: String?, fehler: String?) {
    val f = LocalFarben.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (schritt != null) {
            Row(
                Modifier.fillMaxWidth().glas(f, 18.dp, 1f, f.primaer.copy(alpha = 0.10f)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = f.primaer, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text("Auswertung läuft", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(schritt, color = f.textLeise, fontSize = 13.sp)
                }
                Text("Abbrechen", color = f.gefahr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.antippen { vm.auswertungAbbrechen(z.id) })
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(58.dp).knopf3d(f.primaer, f.tertiaer, 18.dp, f.dunkel).antippen { vm.auswerten(z.id) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = f.aufPrimaer)
                Spacer(Modifier.width(10.dp))
                Text(if (z.ausgewertetAm == null) "Auswerten: Weg und Aufgaben finden" else "Neu auswerten", color = f.aufPrimaer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            z.ausgewertetAm?.let {
                Text(
                    "Zuletzt ausgewertet am " + SimpleDateFormat("d. MMMM yyyy, HH:mm", Locale.GERMAN).format(Date(it)) + " Uhr" +
                        (if (z.auswertungModell.isNotBlank()) " · ${z.auswertungModell}" else "") +
                        ". Übernommene und ausgeblendete Vorschläge bleiben beim Neu-Auswerten erhalten.",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        fehler?.let { Text(it, color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
    }
}

@Composable
private fun Bericht(vm: AppViewModel, z: Ziel) {
    val f = LocalFarben.current
    var offen by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.8f).animateContentSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().antippen(haptik = false) { offen = !offen }, verticalAlignment = Alignment.CenterVertically) {
            Text("RECHERCHE", color = f.primaer, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Icon(if (vm.sprichtId == "bericht_${z.id}") Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp, "Recherche vorlesen", tint = f.primaer,
                modifier = Modifier.size(22.dp).antippen { vm.berichtVorlesen(z) })
            Spacer(Modifier.width(12.dp))
            Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (offen) "Zuklappen" else "Aufklappen", tint = f.textLeise)
        }
        val recherche = remember(z.bericht) { Quellen.teilen(z.bericht) }
        val zeilen = recherche.text.lines()
        val gezeigt = if (offen) zeilen else zeilen.filter { it.isNotBlank() }.take(4)
        gezeigt.forEachIndexed { i, zeile ->
            // Markdown räumt Quellen.teilen schon ab; Spiegelstriche werden hier zu Punkten.
            val inhalt = zeile.trim()
            val punkt = SPIEGELSTRICH.find(inhalt)
            val ueberschrift = punkt == null && inhalt.isNotEmpty() && inhalt.length < 60 && inhalt == inhalt.uppercase() && inhalt.any { it.isLetter() }
            when {
                inhalt.isEmpty() -> if (i > 0 && gezeigt[i - 1].isNotBlank()) Spacer(Modifier.height(2.dp))
                punkt != null -> Row(Modifier.padding(start = 4.dp)) {
                    Text("•", color = f.primaer, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(14.dp))
                    Text(inhalt.substring(punkt.range.last + 1), color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
                }
                ueberschrift -> Text(inhalt.trimEnd(':'), color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp))
                else -> Text(inhalt, color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        if (!offen) Text("Ganze Recherche lesen", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.antippen { offen = true })
        if (offen && recherche.quellen.isNotEmpty()) QuellenAbschnitt(recherche.quellen)
    }
}

private val SPIEGELSTRICH = Regex("""^[-*•]\s+""")

/** Die Belege der Recherche unter einem Strich: kurzer Titel, darunter der Seitenname, antippen öffnet die Seite. */
@Composable
private fun QuellenAbschnitt(quellen: List<Quellen.Quelle>) {
    val f = LocalFarben.current
    val uri = LocalUriHandler.current
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(f.rand))
    Text("QUELLEN", color = f.textLeise, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 4.dp))
    quellen.forEach { q ->
        Row(
            Modifier.fillMaxWidth().then(if (q.url.isEmpty()) Modifier else Modifier.antippen(haptik = false) { runCatching { uri.openUri(q.url) } }).padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(q.titel, color = if (q.url.isEmpty()) f.textLeise else f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (q.seite != q.titel) Text(q.seite, color = f.textSchwach, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (q.url.isNotEmpty()) Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Quelle öffnen", tint = f.textSchwach, modifier = Modifier.padding(start = 8.dp).size(16.dp))
        }
    }
}

@Composable
private fun GruppenKopf(vm: AppViewModel, z: Ziel, k: Kategorie, liste: List<Vorschlag>) {
    val f = LocalFarben.current
    val farbe = if (k == Kategorie.NOTWENDIG) f.primaer else f.leise
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).knopf3d(farbe, farbe, 99.dp, f.dunkel))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("${k.anzeige} · ${liste.count { it.erledigt }}/${liste.size} erledigt", color = f.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(k.beschreibung, color = f.textLeise, fontSize = 12.sp)
        }
        if (k == Kategorie.NOTWENDIG && liste.any { it.aufgabeId == null }) {
            Row(
                Modifier.glas(f, 99.dp, 0.6f, f.flaecheStark).antippen { vm.alleNotwendigenUebernehmen(z.id) }.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.DoneAll, null, tint = f.primaer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Alle", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VorschlagKarte(vm: AppViewModel, v: Vorschlag, titelVon: Map<String, String>, sortierung: Sortierung, nummer: Int) {
    val f = LocalFarben.current
    var offen by rememberSaveable(v.id) { mutableStateOf(false) }
    val farbe = if (v.gruppe == Kategorie.NOTWENDIG) f.primaer else f.leise
    Column(
        Modifier.fillMaxWidth()
            .glas(f, 18.dp, if (v.erledigt) 0.3f else 0.9f, toenung = if (v.erledigt) f.erfolg else farbe).animateContentSize()
            .antippen(haptik = false) { offen = !offen }.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sortierung == Sortierung.REIHENFOLGE) Text("$nummer. ", color = farbe, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(v.titel, color = if (v.erledigt) f.textLeise else f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                    Wichtigkeit(v.wichtigkeit, farbe)
                    Etikett(v.aufwandKlasse.anzeige + " · " + dauerText(v.minuten))
                    Etikett(when (v.phase) { 1 -> "sofort"; 2 -> "bald"; else -> "später" })
                    if (v.wiederkehrend) Icon(Icons.Rounded.Repeat, "wiederkehrend", tint = f.textLeise, modifier = Modifier.size(15.dp))
                }
            }
            PlusKnopf(vm, v)
        }
        AnimatedVisibility(offen) {
            Column(Modifier.padding(top = 10.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (v.text.isNotBlank()) Text(v.text, color = f.text, fontSize = 14.sp, lineHeight = 20.sp)
                if (v.begruendung.isNotBlank()) Zeile("Warum", v.begruendung)
                if (v.erfolg.isNotBlank()) Zeile("Erledigt, wenn", v.erfolg)
                val vor = v.voraussetzungen.mapNotNull { titelVon[it] }
                if (vor.isNotEmpty()) Zeile("Vorher", vor.joinToString(" · "))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (v.aufgabeId == null) Text("Ausblenden", color = f.textSchwach, fontSize = 13.sp, modifier = Modifier.antippen { vm.verwerfen(v) })
                    Text(if (vm.sprichtId == "v_${v.id}") "Stopp" else "Vorlesen", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.antippen { vm.vorlesen("v_${v.id}", listOf(v.titel + ".", v.text, v.begruendung).filter { it.isNotBlank() }.joinToString(" ")) })
                }
            }
        }
    }
}

@Composable
private fun Zeile(titel: String, text: String) {
    val f = LocalFarben.current
    Row {
        Text("$titel: ", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/** Fünf goldene Punkte für die Wichtigkeit. */
@Composable
private fun Wichtigkeit(wert: Int, farbe: Color) {
    val f = LocalFarben.current
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) Box(Modifier.size(7.dp).glas(f, 99.dp, 0f, if (i <= wert) farbe else f.textSchwach.copy(alpha = 0.25f), rand = false))
    }
}

/** Plus: übernimmt in Geniale Aufgaben. Danach Haken (übernommen) bzw. gefüllter Haken (dort erledigt). */
@Composable
private fun PlusKnopf(vm: AppViewModel, v: Vorschlag) {
    val f = LocalFarben.current
    val laeuft = v.id in vm.uebernimmt
    Box(Modifier.padding(start = 6.dp).size(46.dp), contentAlignment = Alignment.Center) {
        when {
            laeuft -> CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            v.erledigt -> Box(Modifier.size(40.dp).knopf3d(f.erfolg, f.erfolg, 99.dp, f.dunkel), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.TaskAlt, "In Geniale Aufgaben erledigt", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            v.aufgabeId != null -> Box(Modifier.size(40.dp).glas(f, 99.dp, 0.4f, f.primaer.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, "Steht in Geniale Aufgaben", tint = f.primaer, modifier = Modifier.size(22.dp))
            }
            else -> Box(Modifier.size(42.dp).knopf3d(f.primaer, f.tertiaer, 99.dp, f.dunkel).antippen { vm.uebernehmen(v) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "In Geniale Aufgaben übernehmen", tint = f.aufPrimaer, modifier = Modifier.size(26.dp))
            }
        }
    }
}

private fun dauerText(min: Int): String = when {
    min < 60 -> "$min Min."
    min % 60 == 0 && min < 24 * 60 -> "${min / 60} Std."
    min < 24 * 60 -> "%.1f Std.".format(Locale.GERMAN, min / 60.0)
    else -> "${(min / 60.0 / 8).toInt().coerceAtLeast(1)} Tage"
}
