package de.frank.jarvis.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Anhang
import de.frank.jarvis.ablage.Art
import de.frank.jarvis.ablage.Dateityp
import de.frank.jarvis.ablage.Eintrag
import de.frank.jarvis.ablage.Kategorie
import de.frank.jarvis.ablage.Medien
import de.frank.jarvis.ablage.UebertragungsZustand
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.faehigkeit.Ablage
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Sortierung(val anzeige: String) { NEUESTE("Neueste zuerst"), AELTESTE("Älteste zuerst"), NAME("Name A–Z"), GROESSE("Größte zuerst") }

/**
 * Die Ablage von Jarvis: Texte und Dateien suchen, filtern, ansehen, abspielen, herunterladen, teilen und löschen.
 * Antippen öffnet die Vorschau, langes Drücken wählt mehrere Einträge zum gemeinsamen Teilen oder Herunterladen aus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AblageBildschirm(vm: AppViewModel, activity: ComponentActivity) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val speicher = remember { AblageZentrale.speicher(context) }
    val stand by speicher.stand.collectAsState()
    val laeufe by Agenten.laeufe.collectAsState()
    val uebertragungen by AblageZentrale.uebertragungen(context).liste.collectAsState()
    val arbeiten by AblageZentrale.arbeiten.collectAsState()
    val bereich = rememberCoroutineScope()
    val aktionen = rememberAblageAktionen(vm)
    val alle by ladeImHintergrund(emptyList<Eintrag>(), stand, laeufe.size) { speicher.eintraege() }

    var suche by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortierung by rememberSaveable { mutableStateOf(Sortierung.NEUESTE.name) }
    var auswahl by rememberSaveable { mutableStateOf(emptyList<String>()) }
    // Offene Ansicht: Eintrag-ID, dazu optional die Anhang-ID.
    var offenerEintrag by rememberSaveable { mutableStateOf<String?>(null) }
    var offenerAnhang by rememberSaveable { mutableStateOf<String?>(null) }
    var loeschFrage by remember { mutableStateOf<Pair<Eintrag, Anhang?>?>(null) }

    // Volltextsuche im Text der Einträge: einmal im Hintergrund gelesen (gekürzt), nicht bei jedem Tastendruck.
    val texte by ladeImHintergrund(emptyMap<String, String>(), alle) { alle.associate { it.id to speicher.text(it, 200_000).lowercase(Locale.GERMAN) } }
    val kat = filter?.let { k -> Kategorie.entries.firstOrNull { it.name == k } }
    val sicht = remember(alle, suche, kat, sortierung, texte) {
        val s = suche.trim().lowercase(Locale.GERMAN)
        alle.filter { e ->
            (kat == null || kat in e.kategorien) && (s.isEmpty() || s in e.titel.lowercase(Locale.GERMAN) || (texte[e.id]?.contains(s) == true) ||
                e.anhaenge.any { s in it.originalName.lowercase(Locale.GERMAN) || s in it.beschreibung.lowercase(Locale.GERMAN) })
        }.let { l ->
            when (Sortierung.valueOf(sortierung)) {
                Sortierung.NEUESTE -> l.sortedByDescending { it.geaendert }
                Sortierung.AELTESTE -> l.sortedBy { it.geaendert }
                Sortierung.NAME -> l.sortedBy { it.titel.lowercase(Locale.GERMAN) }
                Sortierung.GROESSE -> l.sortedByDescending { groesse(speicher, it) }
            }
        }
    }

    // Löschen in einem Reiter trifft nur die Dateien dieses Reiters; was der Eintrag sonst enthält, bleibt in den anderen Reitern.
    fun nurReiter(e: Eintrag): List<Anhang>? = kat?.let { k -> e.anhaenge.filter { it.art.kategorie == k }.takeIf { it.isNotEmpty() && it.size < e.anhaenge.size } }

    fun loesche(e: Eintrag, a: Anhang?) {
        val teil = if (a == null) nurReiter(e) else null
        bereich.launch {
            withContext(Dispatchers.IO) {
                when {
                    a != null -> speicher.loescheAnhang(e.id, a.id)
                    teil != null -> teil.forEach { speicher.loescheAnhang(e.id, it.id) }
                    else -> speicher.loescheEintrag(e.id)
                }
            }
            vm.meldung = if (a != null) "„${a.originalName}“ gelöscht." else if (teil != null) "${teil.size} Datei(en) aus „${e.titel}“ gelöscht." else "„${e.titel}“ gelöscht."
            if (a == null) { offenerEintrag = null; offenerAnhang = null } else offenerAnhang = null
        }
    }

    loeschFrage?.let { (e, a) ->
        val teil = if (a == null) nurReiter(e) else null
        BestaetigeLoeschen(
            if (a != null) "„${a.originalName}“ endgültig löschen?"
            else if (teil != null) "${teil.size} Datei(en) aus „${e.titel}“ unter ${kat?.anzeige} endgültig löschen? Die übrigen Dateien des Eintrags bleiben."
            else "„${e.titel}“" + (if (e.hatDateien) " mit ${e.anhaenge.size} Datei(en)" else "") + " endgültig löschen?",
            { loesche(e, a); loeschFrage = null }, { loeschFrage = null })
    }

    val eintrag = offenerEintrag?.let { id -> alle.firstOrNull { it.id == id } }
    if (offenerEintrag != null && eintrag != null) {
        val anhang = offenerAnhang?.let { id -> eintrag.anhaenge.firstOrNull { it.id == id } }
        // Ein Eintrag mit genau einer Datei öffnet direkt die Vorschau, sein Text steht eingeklappt darunter; „Zurück“ führt dann zur Liste.
        val direkt = eintrag.anhaenge.size == 1
        BackHandler { if (anhang != null && !direkt) offenerAnhang = null else { offenerEintrag = null; offenerAnhang = null } }
        if (anhang != null || direkt) {
            val a = anhang ?: eintrag.anhaenge[0]
            AnhangVorschau(eintrag, a, aktionen, zurueck = { if (anhang != null && !direkt) offenerAnhang = null else { offenerEintrag = null; offenerAnhang = null } },
                loeschen = { loeschFrage = eintrag to (if (direkt) null else a) }, mitText = direkt)
        } else {
            EintragAnsicht(vm, eintrag, aktionen, zurueck = { offenerEintrag = null }, oeffne = { offenerAnhang = it.id }, loeschen = { loeschFrage = eintrag to it })
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        if (auswahl.isNotEmpty()) {
            val gewaehlt = alle.filter { it.id in auswahl }
            BackHandler { auswahl = emptyList() }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp).glas(f, erhoeht = 0.6f, toenung = f.primaer).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rundknopf(Icons.Rounded.Close, "Auswahl beenden") { auswahl = emptyList() }
                Text("${gewaehlt.size} ausgewählt", Modifier.weight(1f), color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Rundknopf(Icons.Rounded.Download, "Ausgewählte herunterladen") { aktionen.herunterladen(gewaehlt.flatMap(aktionen::freigaben)); auswahl = emptyList() }
                Rundknopf(Icons.Rounded.Share, "Ausgewählte teilen") { aktionen.teilen(gewaehlt.flatMap(aktionen::freigaben), gewaehlt.singleOrNull()?.titel); auswahl = emptyList() }
            }
        } else {
            Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 4.dp)) {
                Text("Ablage", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Texte, Bilder, Dokumente und Medien von Jarvis", color = f.textLeise, fontSize = 14.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Eingabe(suche, { suche = it }, "Suchen in Titeln, Texten, Dateien", Modifier.fillMaxWidth())
                if (suche.isEmpty()) Icon(Icons.Rounded.Search, null, tint = f.textSchwach, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(20.dp))
            }
            var sortMenue by remember { mutableStateOf(false) }
            Box {
                Rundknopf(Icons.AutoMirrored.Rounded.Sort, "Sortieren: " + Sortierung.valueOf(sortierung).anzeige) { sortMenue = true }
                DropdownMenu(sortMenue, { sortMenue = false }) {
                    Sortierung.entries.forEach { s ->
                        DropdownMenuItem({ Text(s.anzeige, fontWeight = if (s.name == sortierung) FontWeight.Bold else FontWeight.Normal) }, { sortierung = s.name; sortMenue = false })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Alle", filter == null) { filter = null }
            Kategorie.entries.forEach { k -> Chip(k.anzeige, filter == k.name, icon = kategorieSymbol(k)) { filter = if (filter == k.name) null else k.name } }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(laeufe, key = { "lauf" + it.seit }) { lauf ->
                ArbeitZeile("${lauf.agent} arbeitet", lauf.schritt.ifEmpty { lauf.auftrag }, null)
            }
            items(arbeiten, key = { "arbeit" + it.kennung }) { a ->
                if (a.fehler == null) ArbeitZeile("${a.art}: „${a.eintragTitel}“", "läuft seit ${(System.currentTimeMillis() - a.seit) / 1000} s – noch nicht gespeichert", null)
                else FehlerZeile("${a.art} fehlgeschlagen: „${a.eintragTitel}“", a.fehler, null) { AblageZentrale.verwerfeArbeit(a.kennung) }
            }
            items(uebertragungen, key = { "ueb" + it.id }) { u ->
                if (u.zustand == UebertragungsZustand.LAEUFT) {
                    ArbeitZeile("Übertragung: ${u.name}", "von ${u.host} · " + (u.prozent?.let { "$it % von ${Dateityp.groesse(u.gesamt ?: 0)}" } ?: "${Dateityp.groesse(u.geladen)} geladen"), u.prozent)
                } else {
                    FehlerZeile("Übertragung fehlgeschlagen: ${u.name}", u.fehler ?: "", {
                        bereich.launch { runCatching { AblageZentrale.starteDownload(context, u.id, u.name, u.eintragTitel) } }
                    }) { AblageZentrale.uebertragungen(context).verwerfen(u.id) }
                }
            }
            if (sicht.isEmpty() && laeufe.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Kern(96.dp, f.primaer, aktiv = false)
                    Text(if (alle.isEmpty()) "Noch nichts abgelegt" else "Nichts gefunden", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                    Text(if (alle.isEmpty()) "Sag in ChatGPT zum Beispiel: „Jarvis, lass recherchieren, wie …“ oder „Erstelle eine Infografik und speichere sie in deiner Ablage.“ Das Ergebnis erscheint hier."
                        else "Andere Suche oder anderen Filter probieren.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            items(sicht, key = { it.id }) { e ->
                val gewaehlt = e.id in auswahl
                EintragZeile(
                    e, gewaehlt, aktionen,
                    tippen = { if (auswahl.isNotEmpty()) auswahl = if (gewaehlt) auswahl - e.id else auswahl + e.id else { offenerEintrag = e.id; offenerAnhang = null } },
                    lange = { auswahl = if (gewaehlt) auswahl - e.id else auswahl + e.id },
                    loeschen = { loeschFrage = e to null },
                )
            }
        }
    }
}

private fun groesse(s: de.frank.jarvis.ablage.AblageSpeicher, e: Eintrag): Long = e.anhaenge.sumOf { it.groesse } + (s.textDatei(e)?.length() ?: 0)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EintragZeile(e: Eintrag, gewaehlt: Boolean, aktionen: AblageAktionen, tippen: () -> Unit, lange: () -> Unit, loeschen: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val speicher = remember { AblageZentrale.speicher(context) }
    var menue by remember { mutableStateOf(false) }
    val haupt = e.anhaenge.firstOrNull { it.art == Art.BILD || it.art == Art.ANIMATION } ?: e.anhaenge.firstOrNull { it.art == Art.VIDEO || it.art == Art.PDF } ?: e.anhaenge.firstOrNull()
    val art = if (haupt == null) "Text" else if (e.anhaenge.size == 1) haupt.art.anzeige + " · " + haupt.endung.uppercase() else "${e.anhaenge.size} Dateien"
    val meta = "$art · ${Ablage.datum(e.geaendert)} · ${Dateityp.groesse(groesse(speicher, e))}"
    Row(
        Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f, toenung = if (gewaehlt) f.primaer else null)
            .combinedClickable(onClick = tippen, onLongClick = lange, onClickLabel = "Öffnen", onLongClickLabel = "Auswählen")
            .semantics { stateDescription = if (gewaehlt) "ausgewählt" else "" }
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp)) {
            Vorschaubild(e, haupt)
            if (gewaehlt) Icon(Icons.Rounded.CheckCircle, null, tint = f.primaer, modifier = Modifier.align(Alignment.TopEnd).size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Text(e.titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta, color = f.textSchwach, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            if (e.anhaenge.size > 1) Text(e.anhaenge.groupingBy { it.art.anzeige }.eachCount().entries.joinToString(" · ") { "${it.value}× ${it.key}" },
                color = f.textLeise, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val alleDateien = remember(e) { aktionen.freigaben(e) }
        Rundknopf(Icons.Rounded.Download, "„${e.titel}“ herunterladen") { aktionen.herunterladen(alleDateien) }
        Box(Modifier.padding(start = 4.dp)) { Rundknopf(Icons.Rounded.Share, "„${e.titel}“ teilen") { aktionen.teilen(alleDateien, e.titel) } }
        Box {
            Box(Modifier.size(48.dp).antippen { menue = true }.semantics { contentDescription = "Weitere Aktionen für „${e.titel}“" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, null, tint = f.textLeise)
            }
            DropdownMenu(menue, { menue = false }) {
                alleDateien.singleOrNull()?.let { einzige ->
                    DropdownMenuItem({ Text("Mit anderer App öffnen") }, { menue = false; aktionen.oeffnen(einzige) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) })
                    DropdownMenuItem({ Text("Speichern unter …") }, { menue = false; aktionen.speichernUnter(einzige) }, leadingIcon = { Icon(Icons.Rounded.SaveAs, null) })
                }
                DropdownMenuItem({ Text("Auswählen") }, { menue = false; lange() }, leadingIcon = { Icon(Icons.Rounded.CheckCircle, null) })
                DropdownMenuItem({ Text("Löschen", color = f.gefahr) }, { menue = false; loeschen() }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = f.gefahr) })
            }
        }
    }
}

/** Vorschaubild aus dem Speicher (einmal erzeugt, bleibt erhalten), sonst ein eindeutiges Dateitypsymbol. */
@Composable
private fun Vorschaubild(e: Eintrag, a: Anhang?) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val bild by ladeImHintergrund<ImageBitmap?>(null, a?.id, a?.vorschau) {
        if (a == null) null else runCatching {
            val s = AblageZentrale.speicher(context)
            val ziel = s.vorschauDatei(a)
            val datei = if (ziel.isFile) ziel else Medien.vorschaubild(s.datei(a), a.art, ziel)?.also { s.setzeVorschau(e.id, a.id, it) }
            datei?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }.getOrNull()
    }
    val form = RoundedCornerShape(14.dp)
    val b = bild
    if (b != null) {
        Box(Modifier.size(56.dp).clip(form)) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (a?.art == Art.VIDEO) Icon(Icons.Rounded.VideoFile, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(16.dp))
        }
    } else {
        Box(Modifier.size(56.dp).glas(f, 14.dp, erhoeht = 0.4f, toenung = f.primaer), contentAlignment = Alignment.Center) {
            Icon(a?.let { dateiSymbol(it.art) } ?: Icons.AutoMirrored.Rounded.Article, null, tint = f.primaer, modifier = Modifier.size(28.dp))
        }
    }
}

/** Ein Eintrag mit Text und/oder mehreren Dateien: Text lesen (wie bisher, mit Vorlesen) und jede Datei einzeln öffnen. */
@Composable
private fun EintragAnsicht(vm: AppViewModel, e: Eintrag, aktionen: AblageAktionen, zurueck: () -> Unit, oeffne: (Anhang) -> Unit, loeschen: (Anhang?) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val speicher = remember { AblageZentrale.speicher(context) }
    val textDatei = remember(e) { speicher.textDatei(e) }
    val text by ladeImHintergrund("", textDatei, e.geaendert) { textDatei?.let { runCatching { it.readText() }.getOrDefault("") } ?: "" }
    val alle = remember(e) { aktionen.freigaben(e) }
    Column(Modifier.fillMaxSize()) {
        VorschauKopf(e.titel, "${Ablage.datum(e.geaendert)} · " + (if (e.hatText) "Text" else "") + (if (e.hatText && e.hatDateien) " + " else "") +
            (if (e.hatDateien) "${e.anhaenge.size} Datei(en)" else ""), alle.singleOrNull(), aktionen, zurueck, { loeschen(null) },
            zusatz = {
                if (e.hatText && textDatei != null) Vorleseknopf(vm, "ablage:" + textDatei.name, e.titel, text)
                if (alle.size > 1) {
                    Rundknopf(Icons.Rounded.Download, "Alle Dateien herunterladen") { aktionen.herunterladen(alle) }
                    Rundknopf(Icons.Rounded.Share, "Alle Dateien teilen") { aktionen.teilen(alle, e.titel) }
                }
            })
        if (e.hatDateien) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                e.anhaenge.forEach { a -> AnhangZeile(e, a, aktionen, { oeffne(a) }) { loeschen(a) } }
            }
        }
        if (e.hatText) {
            if (e.hatDateien) Begleittext(text)
            else Box(Modifier.weight(1f)) { LeseText(text, "md", markdown = true, gekuerzt = false, gesamt = textDatei?.length() ?: 0) }
        }
    }
}

@Composable
private fun AnhangZeile(e: Eintrag, a: Anhang, aktionen: AblageAktionen, oeffne: () -> Unit, loeschen: () -> Unit) {
    val f = LocalFarben.current
    var menue by remember { mutableStateOf(false) }
    val freigabe = remember(a.id) { aktionen.freigabe(a) }
    Row(Modifier.fillMaxWidth().glas(f, erhoeht = 0.5f).antippen(aktion = oeffne).semantics { contentDescription = "${a.originalName}, ${a.art.anzeige}, ${Dateityp.groesse(a.groesse)}. Antippen zum Öffnen." }
        .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp)) { Vorschaubild(e, a) }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(a.originalName, color = f.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${a.art.anzeige} · ${Dateityp.groesse(a.groesse)}", color = f.textSchwach, fontSize = 12.sp)
        }
        Rundknopf(Icons.Rounded.Download, "„${a.originalName}“ herunterladen") { aktionen.herunterladen(listOf(freigabe)) }
        Box(Modifier.padding(start = 4.dp)) { Rundknopf(Icons.Rounded.Share, "„${a.originalName}“ teilen") { aktionen.teilen(listOf(freigabe), a.originalName) } }
        Box {
            Box(Modifier.size(48.dp).antippen { menue = true }.semantics { contentDescription = "Weitere Aktionen für „${a.originalName}“" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, null, tint = f.textLeise)
            }
            DropdownMenu(menue, { menue = false }) {
                DropdownMenuItem({ Text("Mit anderer App öffnen") }, { menue = false; aktionen.oeffnen(freigabe) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) })
                DropdownMenuItem({ Text("Speichern unter …") }, { menue = false; aktionen.speichernUnter(freigabe) }, leadingIcon = { Icon(Icons.Rounded.SaveAs, null) })
                DropdownMenuItem({ Text("Löschen", color = f.gefahr) }, { menue = false; loeschen() }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = f.gefahr) })
            }
        }
    }
}

@Composable
private fun ArbeitZeile(titel: String, text: String, prozent: Int?) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f, toenung = f.primaer).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kern(34.dp, f.primaer, aktiv = true)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text, color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (prozent != null) LinearProgressIndicator(progress = { prozent / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp), color = f.primaer, trackColor = f.flaecheStark)
    }
}

@Composable
private fun FehlerZeile(titel: String, text: String, wiederholen: (() -> Unit)?, verwerfen: () -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f, toenung = f.gefahr).padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text, color = f.textLeise, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (wiederholen != null) Rundknopf(Icons.Rounded.Refresh, "Erneut versuchen", aktion = wiederholen)
        Box(Modifier.padding(start = 4.dp)) { Rundknopf(Icons.Rounded.Close, "Verwerfen", aktion = verwerfen) }
    }
}

@Composable
private fun BestaetigeLoeschen(frage: String, ja: () -> Unit, nein: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = nein,
        confirmButton = { androidx.compose.material3.TextButton(ja) { Text("Endgültig löschen", color = LocalFarben.current.gefahr) } },
        dismissButton = { androidx.compose.material3.TextButton(nein) { Text("Behalten") } },
        text = { Text(frage) },
    )
}

fun dateiSymbol(art: Art): ImageVector = when (art) {
    Art.TEXT -> Icons.Rounded.Description
    Art.MARKDOWN -> Icons.AutoMirrored.Rounded.Article
    Art.CODE -> Icons.Rounded.Code
    Art.DATEN -> Icons.Rounded.DataObject
    Art.HTML -> Icons.Rounded.Language
    Art.TABELLE_TEXT, Art.TABELLE -> Icons.Rounded.TableChart
    Art.BILD, Art.SVG -> Icons.Rounded.Image
    Art.ANIMATION -> Icons.Rounded.Gif
    Art.PDF -> Icons.Rounded.PictureAsPdf
    Art.DOKUMENT -> Icons.Rounded.Description
    Art.PRAESENTATION -> Icons.Rounded.Slideshow
    Art.AUDIO -> Icons.Rounded.AudioFile
    Art.VIDEO -> Icons.Rounded.VideoFile
    Art.ARCHIV -> Icons.Rounded.FolderZip
    Art.SONSTIGE -> Icons.Rounded.InsertDriveFile
}

private fun kategorieSymbol(k: Kategorie): ImageVector = when (k) {
    Kategorie.TEXTE -> Icons.AutoMirrored.Rounded.Article
    Kategorie.BILDER -> Icons.Rounded.Image
    Kategorie.DOKUMENTE -> Icons.Rounded.PictureAsPdf
    Kategorie.TABELLEN -> Icons.Rounded.TableChart
    Kategorie.PRAESENTATIONEN -> Icons.Rounded.Slideshow
    Kategorie.AUDIO -> Icons.Rounded.AudioFile
    Kategorie.VIDEO -> Icons.Rounded.VideoFile
    Kategorie.SONSTIGE -> Icons.Rounded.InsertDriveFile
}

/** Kleiner runder Glasknopf mit Symbol, für die Kopfzeilen (48 dp Tippfläche, Beschriftung für Bildschirmleser). */
@Composable
fun Rundknopf(icon: ImageVector, beschreibung: String, farbe: androidx.compose.ui.graphics.Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(48.dp).padding(2.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen(aktion = aktion).semantics { contentDescription = beschreibung }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = farbe ?: f.textLeise, modifier = Modifier.size(20.dp))
    }
}
