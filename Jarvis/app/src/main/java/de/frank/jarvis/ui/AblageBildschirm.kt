package de.frank.jarvis.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.List
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Anhang
import de.frank.jarvis.ablage.Art
import de.frank.jarvis.ablage.Auszug
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ansichten wie im Windows-Explorer: große, mittlere und kleine Kacheln, Details und eine knappe Liste. */
private enum class Ansicht(val anzeige: String, val spalten: Int) {
    GROSS("Große Kacheln", 2), MITTEL("Mittlere Kacheln", 3), KLEIN("Kleine Kacheln", 5), DETAILS("Details", 1), LISTE("Liste", 1);
    val kacheln get() = spalten > 1
}

private enum class Sortierung(val anzeige: String) { NEUESTE("Neueste zuerst"), AELTESTE("Älteste zuerst"), NAME("Name A–Z"), GROESSE("Größte zuerst") }

/**
 * Die Ablage von Jarvis: Texte und Dateien suchen, filtern, ansehen, abspielen, herunterladen, teilen und löschen.
 * Antippen öffnet die Vorschau, langes Drücken wählt mehrere Einträge zum gemeinsamen Teilen oder Herunterladen aus.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    // null = noch nicht geladen (kein Leer-Hinweis); bei Änderungen bleibt die vorige Liste stehen, bis die neue da ist.
    val geladen by ladeImHintergrund<List<Eintrag>?>(null, stand, laeufe.size, behalten = true) { speicher.eintraege() }
    val alle = geladen.orEmpty()

    var suche by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortierung by rememberSaveable { mutableStateOf(Sortierung.NEUESTE.name) }
    var auswahl by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val ansicht = Ansicht.entries.firstOrNull { it.name.lowercase() == vm.einstellungen.ablageAnsicht } ?: Ansicht.MITTEL
    // Offene Ansicht: Eintrag-ID, dazu optional die Anhang-ID.
    var offenerEintrag by rememberSaveable { mutableStateOf<String?>(null) }
    var offenerAnhang by rememberSaveable { mutableStateOf<String?>(null) }
    var loeschFrage by remember { mutableStateOf<Pair<Eintrag, Anhang?>?>(null) }

    // Volltextsuche im Text der Einträge: nur gelesen, solange gesucht wird, einmal im Hintergrund (gekürzt), nicht je Tastendruck.
    val suchAktiv = suche.isNotBlank()
    val texte by ladeImHintergrund(emptyMap<String, String>(), alle, suchAktiv) {
        if (!suchAktiv) emptyMap() else alle.associate { it.id to speicher.text(it, 200_000).lowercase(Locale.GERMAN) }
    }
    val kat = filter?.let { k -> Kategorie.entries.firstOrNull { it.name == k } }
    // Filtern und Sortieren abseits des Hauptfadens, kurz entprellt; bis zum neuen Ergebnis bleibt die vorige Liste stehen.
    // null = erste Berechnung läuft noch: Erst danach darf „Nichts gefunden“ oder „Noch nichts abgelegt“ erscheinen.
    var sicht by remember { mutableStateOf<List<Eintrag>?>(null) }
    LaunchedEffect(geladen, suche, kat, sortierung, texte) {
        if (geladen == null) return@LaunchedEffect
        if (suche.isNotBlank()) delay(150)
        val s = suche.trim().lowercase(Locale.GERMAN)
        sicht = withContext(Dispatchers.Default) {
            alle.filter { e ->
                (kat == null || kat in e.kategorien) && (s.isEmpty() || s in e.titel.lowercase(Locale.GERMAN) || (texte[e.id]?.contains(s) == true) ||
                    e.anhaenge.any { s in it.originalName.lowercase(Locale.GERMAN) || s in it.beschreibung.lowercase(Locale.GERMAN) })
            }.let { l ->
                when (Sortierung.valueOf(sortierung)) {
                    Sortierung.NEUESTE -> l.sortedByDescending { it.geaendert }
                    Sortierung.AELTESTE -> l.sortedBy { it.geaendert }
                    Sortierung.NAME -> l.sortedBy { it.titel.lowercase(Locale.GERMAN) }
                    // Größe je Eintrag einmal ermitteln, nicht bei jedem Vergleich (Dateisystem-Abfrage).
                    Sortierung.GROESSE -> withContext(Dispatchers.IO) { l.map { it to groesse(speicher, it) } }.sortedByDescending { it.second }.map { it.first }
                }
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
            Text("Ablage", Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 4.dp), color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Eingabe(suche, { suche = it }, "Suchen in Titeln, Texten, Dateien", Modifier.fillMaxWidth())
                if (suche.isEmpty()) Icon(Icons.Rounded.Search, null, tint = f.textSchwach, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(20.dp))
            }
            var sortMenue by remember { mutableStateOf(false) }
            Box {
                Rundknopf(Icons.AutoMirrored.Rounded.Sort, "Sortieren: " + Sortierung.valueOf(sortierung).anzeige) { sortMenue = true }
                Menue(sortMenue, { sortMenue = false }) {
                    Sortierung.entries.forEach { s ->
                        DropdownMenuItem({ Text(s.anzeige, fontWeight = if (s.name == sortierung) FontWeight.Bold else FontWeight.Normal) }, { sortierung = s.name; sortMenue = false })
                    }
                }
            }
            var ansichtMenue by remember { mutableStateOf(false) }
            Box {
                Rundknopf(ansichtSymbol(ansicht), "Ansicht: " + ansicht.anzeige) { ansichtMenue = true }
                Menue(ansichtMenue, { ansichtMenue = false }) {
                    Ansicht.entries.forEach { a ->
                        DropdownMenuItem({ Text(a.anzeige, fontWeight = if (a == ansicht) FontWeight.Bold else FontWeight.Normal) }, { vm.einstellungen.ablageAnsicht = a.name.lowercase(); ansichtMenue = false },
                            leadingIcon = { Icon(ansichtSymbol(a), null) })
                    }
                }
            }
        }
        // Alle Bereiche auf einen Blick, umbrechend statt seitlich zu wischen.
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Alle", filter == null) { filter = null }
            Kategorie.entries.forEach { k -> Chip(k.anzeige, filter == k.name, icon = kategorieSymbol(k)) { filter = if (filter == k.name) null else k.name } }
        }
        val ganz: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.(Any?) -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
        LazyVerticalGrid(
            GridCells.Fixed(ansicht.spalten),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (ansicht == Ansicht.LISTE) 6.dp else if (ansicht == Ansicht.KLEIN) 6.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (ansicht == Ansicht.KLEIN) 6.dp else 10.dp),
        ) {
            items(laeufe, key = { "lauf" + it.seit }, span = ganz) { lauf ->
                ArbeitZeile("${lauf.agent} arbeitet", lauf.schritt.ifEmpty { lauf.auftrag }, null)
            }
            items(arbeiten, key = { "arbeit" + it.kennung }, span = ganz) { a ->
                if (a.fehler == null) ArbeitZeile("${a.art}: „${a.eintragTitel}“", "läuft seit ${(System.currentTimeMillis() - a.seit) / 1000} s – noch nicht gespeichert", null)
                else FehlerZeile("${a.art} fehlgeschlagen: „${a.eintragTitel}“", a.fehler, null) { AblageZentrale.verwerfeArbeit(a.kennung) }
            }
            items(uebertragungen, key = { "ueb" + it.id }, span = ganz) { u ->
                if (u.zustand == UebertragungsZustand.LAEUFT) {
                    ArbeitZeile("Übertragung: ${u.name}", "von ${u.host} · " + (u.prozent?.let { "$it % von ${Dateityp.groesse(u.gesamt ?: 0)}" } ?: "${Dateityp.groesse(u.geladen)} geladen"), u.prozent)
                } else {
                    FehlerZeile("Übertragung fehlgeschlagen: ${u.name}", u.fehler ?: "", {
                        bereich.launch { runCatching { AblageZentrale.starteDownload(context, u.id, u.name, u.eintragTitel) } }
                    }) { AblageZentrale.uebertragungen(context).verwerfen(u.id) }
                }
            }
            if (sicht?.isEmpty() == true && laeufe.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Kern(96.dp, f.primaer, aktiv = false)
                    Text(if (alle.isEmpty()) "Noch nichts abgelegt" else "Nichts gefunden", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                    Text(if (alle.isEmpty()) "Sag in ChatGPT zum Beispiel: „Jarvis, lass recherchieren, wie …“ oder „Erstelle eine Infografik und speichere sie in deiner Ablage.“ Das Ergebnis erscheint hier."
                        else "Andere Suche oder anderen Filter probieren.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            items(sicht.orEmpty(), key = { it.id }) { e ->
                val gewaehlt = e.id in auswahl
                val tippen = { if (auswahl.isNotEmpty()) auswahl = if (gewaehlt) auswahl - e.id else auswahl + e.id else { offenerEintrag = e.id; offenerAnhang = null } }
                val lange = { auswahl = if (gewaehlt) auswahl - e.id else auswahl + e.id }
                when {
                    ansicht.kacheln -> EintragKachel(e, gewaehlt, ansicht, aktionen, tippen, lange) { loeschFrage = e to null }
                    ansicht == Ansicht.LISTE -> EintragListe(e, gewaehlt, aktionen, tippen, lange) { loeschFrage = e to null }
                    else -> EintragZeile(e, gewaehlt, aktionen, tippen = tippen, lange = lange, loeschen = { loeschFrage = e to null })
                }
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
    val haupt = hauptAnhang(e)
    val art = if (haupt == null) "Text" else if (e.anhaenge.size == 1) haupt.art.anzeige + " · " + haupt.endung.uppercase() else "${e.anhaenge.size} Dateien"
    // Die Textgröße kommt aus dem Dateisystem: im Hintergrund, bis dahin die Summe der Anhänge.
    val g by ladeImHintergrund(e.anhaenge.sumOf { it.groesse }, e.id, e.geaendert) { groesse(speicher, e) }
    val meta = "$art · ${Ablage.datum(e.geaendert)} · ${Dateityp.groesse(g)}"
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
        EintragMenue(e, aktionen, lange, loeschen)
    }
}

/** Die drei Punkte mit allen Aktionen eines Eintrags; in den Kacheln und der Liste stehen dort auch Herunterladen und Teilen. */
@Composable
private fun EintragMenue(e: Eintrag, aktionen: AblageAktionen, lange: () -> Unit, loeschen: () -> Unit, mitTeilen: Boolean = false, groesse: Dp = 48.dp, aufBild: Boolean = false) {
    val f = LocalFarben.current
    var menue by remember { mutableStateOf(false) }
    val alleDateien = remember(e) { aktionen.freigaben(e) }
    Box {
        Box(
            Modifier.size(groesse).then(if (aufBild) Modifier.padding(4.dp).glas(f, groesse / 2, erhoeht = 0.4f, fuellung = f.flaecheStark) else Modifier)
                .antippen { menue = true }.semantics { contentDescription = "Weitere Aktionen für „${e.titel}“" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreVert, null, tint = if (aufBild) f.text else f.textLeise, modifier = Modifier.size(if (groesse < 40.dp) 18.dp else 24.dp))
        }
        Menue(menue, { menue = false }) {
            if (mitTeilen) {
                DropdownMenuItem({ Text("Herunterladen") }, { menue = false; aktionen.herunterladen(alleDateien) }, leadingIcon = { Icon(Icons.Rounded.Download, null) })
                DropdownMenuItem({ Text("Teilen") }, { menue = false; aktionen.teilen(alleDateien, e.titel) }, leadingIcon = { Icon(Icons.Rounded.Share, null) })
            }
            alleDateien.singleOrNull()?.let { einzige ->
                DropdownMenuItem({ Text("Mit anderer App öffnen") }, { menue = false; aktionen.oeffnen(einzige) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) })
                DropdownMenuItem({ Text("Speichern unter …") }, { menue = false; aktionen.speichernUnter(einzige) }, leadingIcon = { Icon(Icons.Rounded.SaveAs, null) })
            }
            DropdownMenuItem({ Text("Auswählen") }, { menue = false; lange() }, leadingIcon = { Icon(Icons.Rounded.CheckCircle, null) })
            DropdownMenuItem({ Text("Löschen", color = f.gefahr) }, { menue = false; loeschen() }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = f.gefahr) })
        }
    }
}

private fun hauptAnhang(e: Eintrag): Anhang? =
    e.anhaenge.firstOrNull { it.art == Art.BILD || it.art == Art.ANIMATION } ?: e.anhaenge.firstOrNull { it.art == Art.VIDEO || it.art == Art.PDF } ?: e.anhaenge.firstOrNull()

/** Kachel wie in einer Fotogalerie: das Bild selbst, quadratisch; darunter der Titel (bei kleinen Kacheln ohne). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EintragKachel(e: Eintrag, gewaehlt: Boolean, ansicht: Ansicht, aktionen: AblageAktionen, tippen: () -> Unit, lange: () -> Unit, loeschen: () -> Unit) {
    val f = LocalFarben.current
    val haupt = hauptAnhang(e)
    val radius = if (ansicht == Ansicht.KLEIN) 10.dp else 16.dp
    val form = RoundedCornerShape(radius)
    Column(
        Modifier.fillMaxWidth().combinedClickable(onClick = tippen, onLongClick = lange, onClickLabel = "Öffnen", onLongClickLabel = "Auswählen")
            .semantics { contentDescription = e.titel; stateDescription = if (gewaehlt) "ausgewählt" else "" },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(form)) {
            Vorschaubild(e, haupt, Modifier.fillMaxSize(), radius = radius, pixel = if (ansicht == Ansicht.GROSS) 900 else if (ansicht == Ansicht.MITTEL) 500 else 0,
                symbolGroesse = if (ansicht == Ansicht.KLEIN) 24.dp else 40.dp)
            if (e.anhaenge.size > 1) Text("${e.anhaenge.size}", Modifier.align(Alignment.BottomStart).padding(6.dp).glas(f, 10.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(horizontal = 7.dp, vertical = 2.dp),
                color = f.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (gewaehlt) Box(Modifier.matchParentSize().glas(f, 0.dp, erhoeht = 0f, toenung = f.primaer, rand = false))
            if (gewaehlt) Icon(Icons.Rounded.CheckCircle, null, tint = f.primaer, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp))
            if (ansicht != Ansicht.KLEIN) Box(Modifier.align(Alignment.TopEnd)) { EintragMenue(e, aktionen, lange, loeschen, mitTeilen = true, groesse = 36.dp, aufBild = true) }
        }
        if (ansicht != Ansicht.KLEIN) {
            Text(e.titel, Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp), color = f.text, fontSize = if (ansicht == Ansicht.GROSS) 14.sp else 12.sp,
                fontWeight = FontWeight.Medium, maxLines = if (ansicht == Ansicht.GROSS) 2 else 1, overflow = TextOverflow.Ellipsis)
            if (ansicht == Ansicht.GROSS) Text(Ablage.datum(e.geaendert), Modifier.padding(horizontal = 2.dp), color = f.textSchwach, fontSize = 11.sp, maxLines = 1)
        }
    }
}

/** Knappe Liste: kleines Symbol, Titel und Datum in einer Zeile. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EintragListe(e: Eintrag, gewaehlt: Boolean, aktionen: AblageAktionen, tippen: () -> Unit, lange: () -> Unit, loeschen: () -> Unit) {
    val f = LocalFarben.current
    val haupt = hauptAnhang(e)
    Row(
        Modifier.fillMaxWidth().glas(f, 14.dp, erhoeht = 0.4f, toenung = if (gewaehlt) f.primaer else null)
            .combinedClickable(onClick = tippen, onLongClick = lange, onClickLabel = "Öffnen", onLongClickLabel = "Auswählen")
            .semantics { stateDescription = if (gewaehlt) "ausgewählt" else "" }
            .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Vorschaubild(e, haupt, Modifier.size(32.dp), radius = 8.dp, symbolGroesse = 18.dp)
        Text(e.titel, Modifier.weight(1f).padding(start = 10.dp), color = f.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(Ablage.datum(e.geaendert), Modifier.padding(start = 8.dp), color = f.textSchwach, fontSize = 12.sp, maxLines = 1)
        EintragMenue(e, aktionen, lange, loeschen, mitTeilen = true, groesse = 40.dp)
    }
}

private fun ansichtSymbol(a: Ansicht): ImageVector = when (a) {
    Ansicht.GROSS -> Icons.Rounded.GridView
    Ansicht.MITTEL -> Icons.Rounded.ViewModule
    Ansicht.KLEIN -> Icons.Rounded.Apps
    Ansicht.DETAILS -> Icons.Rounded.ViewList
    Ansicht.LISTE -> Icons.AutoMirrored.Rounded.List
}

/**
 * Vorschaubild aus dem Speicher (einmal erzeugt, bleibt erhalten), sonst ein eindeutiges Dateitypsymbol.
 * [pixel] > 0: Bilder für große Kacheln direkt aus der Datei in dieser Größe, damit sie scharf sind.
 */
@Composable
private fun Vorschaubild(e: Eintrag, a: Anhang?, modifier: Modifier = Modifier.size(56.dp), radius: Dp = 14.dp, pixel: Int = 0, symbolGroesse: Dp = 28.dp) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val bild by ladeImHintergrund<ImageBitmap?>(null, a?.id, a?.vorschau, pixel) {
        if (a == null) null else runCatching {
            val s = AblageZentrale.speicher(context)
            if (pixel > 0 && a.art == Art.BILD) Medien.bildVerkleinert(s.datei(a), pixel)?.asImageBitmap()?.let { return@runCatching it }
            val ziel = s.vorschauDatei(a)
            val datei = if (ziel.isFile) ziel else Medien.vorschaubild(s.datei(a), a.art, ziel)?.also { s.setzeVorschau(e.id, a.id, it) }
            datei?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }.getOrNull()
    }
    val b = bild
    val form = RoundedCornerShape(radius)
    if (b != null) {
        Box(modifier.clip(form)) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (a?.art == Art.VIDEO) Icon(Icons.Rounded.VideoFile, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(16.dp))
        }
    } else {
        Box(modifier.glas(f, radius, erhoeht = 0.4f, toenung = f.primaer), contentAlignment = Alignment.Center) {
            Icon(a?.let { dateiSymbol(it.art) } ?: Icons.AutoMirrored.Rounded.Article, null, tint = f.primaer, modifier = Modifier.size(symbolGroesse))
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
    // Begrenzt gelesen (wie die Dateivorschau): Ein riesiger Bericht friert die Ansicht sonst ein.
    val gelesen by ladeImHintergrund<Auszug.TextVorschau?>(null, textDatei, e.geaendert) { textDatei?.let { runCatching { Auszug.lies(it) }.getOrNull() } }
    val text = gelesen?.text.orEmpty()
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
            if (e.hatDateien) { var offen by rememberSaveable(e.id) { mutableStateOf(false) }; Begleittext(text, offen, { offen = !offen }) }
            else Box(Modifier.weight(1f)) { LeseText(text, "md", markdown = true, gekuerzt = gelesen?.gekuerzt == true, gesamt = textDatei?.length() ?: 0) }
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
        Vorschaubild(e, a, Modifier.size(48.dp))
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
            Menue(menue, { menue = false }) {
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

/** Aufklappmenü in den Farben der App (helle bzw. blaue Glasfläche mit Rand) statt der dunklen Standardfläche. */
@Composable
fun Menue(offen: Boolean, schliessen: () -> Unit, inhalt: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val f = LocalFarben.current
    androidx.compose.material3.DropdownMenu(
        offen, schliessen, shape = RoundedCornerShape(f.radius), containerColor = if (f.dunkel) androidx.compose.ui.graphics.Color(0xFF14294A) else androidx.compose.ui.graphics.Color(0xFFFAFDFF),
        tonalElevation = 0.dp, shadowElevation = 10.dp, border = androidx.compose.foundation.BorderStroke(1.dp, f.rand), content = inhalt,
    )
}

/** Kleiner runder Glasknopf mit Symbol, für die Kopfzeilen (48 dp Tippfläche, Beschriftung für Bildschirmleser). */
@Composable
fun Rundknopf(icon: ImageVector, beschreibung: String, farbe: androidx.compose.ui.graphics.Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(48.dp).padding(2.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen(aktion = aktion).semantics { contentDescription = beschreibung }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = farbe ?: f.textLeise, modifier = Modifier.size(20.dp))
    }
}
