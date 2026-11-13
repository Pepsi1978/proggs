package de.frank.jarvis.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.ablage.Ausloesung
import de.frank.jarvis.ablage.Erinnerung
import de.frank.jarvis.ablage.ErinnerungPlaner
import de.frank.jarvis.ablage.ErinnerungStimme
import de.frank.jarvis.ablage.Erinnerungen
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Filterwert des Reiters „Erinnerung“ in der Ablage (kein Ablage-Kategorie-Name). */
const val ERINNERUNG_REITER = "ERINNERUNG"

/** Reiter „Erinnerung“: Unterreiter „Mit Datum“ (nach Fälligkeit, das Nächste oben, das Älteste unten) und „Allgemein“. */
@Composable
fun ErinnerungenBereich(vm: AppViewModel, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val stand by Erinnerungen.stand.collectAsState()
    var allgemein by rememberSaveable { mutableStateOf(false) }
    val datiert by ladeImHintergrund<List<Erinnerung>?>(null, stand, behalten = true) { Erinnerungen.datiert(context) }
    val allg by ladeImHintergrund<List<Erinnerung>?>(null, stand, behalten = true) { Erinnerungen.allgemeine(context) }
    val liste = if (allgemein) allg else datiert
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Mit Datum", !allgemein) { allgemein = false }
            Chip("Allgemein", allgemein) { allgemein = true }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (Erinnerungen.defekt(context)) item(key = "defekt") {
                Text("Die Erinnerungsdatei ist beschädigt und wurde gesichert. Jarvis speichert dort nichts Neues; Details unter Aktivität.", color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
            } else if (liste?.isEmpty() == true) item(key = "leer") {
                Text(if (allgemein) "Keine allgemeinen Erinnerungen. Sag Jarvis: „Erinnere mich später an …“" else "Keine Erinnerungen mit Datum. Tippe auf +, oder sag Jarvis: „Erinnere mich am 12. August daran, …“",
                    color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
            items(liste.orEmpty(), key = { it.id }) { e ->
                val vorbei = !allgemein && (e.erledigt != 0L || (e.termin() ?: 0L) < System.currentTimeMillis())
                Column(Modifier.fillMaxWidth().glas(f, erhoeht = 0.7f).antippen { vm.erinnerungOffen = e.id }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(if (allgemein) (if (e.erledigt != 0L) "Erledigt" else "Ohne festen Termin") else e.datumAnzeige() + " · " + (if (e.wecker) "Wecker" else "Erinnerung") + if (e.erledigt != 0L) " · erledigt" else "",
                        color = if (vorbei) f.textLeise else f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(e.zeile, color = if (vorbei) f.textLeise else f.text, fontSize = 16.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
            }
            item(key = "plus") {
                Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(56.dp).knopf3d(f.primaer, f.tertiaer, 28.dp, f.dunkel).antippen { vm.erinnerungNeu(allgemein) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, "Neue Erinnerung", tint = f.aufPrimaer, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

/** Die vollständige Detailansicht, eine Ebene über der App: Kurzfassung, Inhalt, jeder Erinnerungszeitpunkt samt Bestätigung, Form, Bearbeiten. */
@Composable
fun ErinnerungAnsichtEbene(vm: AppViewModel) {
    val offen = vm.erinnerungOffen ?: return
    val f = LocalFarben.current
    val context = LocalContext.current
    val app = context.applicationContext
    val bereich = rememberCoroutineScope()
    val stand by Erinnerungen.stand.collectAsState()
    val alle by ladeImHintergrund<List<Erinnerung>?>(null, stand, behalten = true) { Erinnerungen.alle(context) }
    LaunchedEffect(alle) { if (alle?.none { it.id == offen } == true) vm.erinnerungOffen = null }
    val e = alle?.firstOrNull { it.id == offen } ?: return
    val schliessen = { vm.erinnerungOffen = null }
    BackHandler(onBack = schliessen)
    var plus by remember(e.id) { mutableStateOf(false) }
    var loeschen by remember(e.id) { mutableStateOf(false) }
    val jetzt = System.currentTimeMillis()
    Box(Modifier.fillMaxSize().background(f.hgOben).pointerInput(Unit) { detectTapGestures { } }.windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (e.allgemein) "Ohne festen Termin" else e.datumAnzeige() + " · " + (if (e.wecker) "Wecker" else "Erinnerung"), Modifier.weight(1f), color = f.primaer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.size(40.dp).antippen { schliessen() }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(e.zeile, color = f.text, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                Text("Kurzfassung", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                Text(e.kurz, color = f.text, fontSize = 16.sp, lineHeight = 22.sp)
                Text("Ausführlich", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                Text(e.text, color = f.text, fontSize = 16.sp, lineHeight = 23.sp)
                if (!e.allgemein) {
                    Text("Form", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Benachrichtigung", !e.wecker) { bereich.launch(Dispatchers.IO) { Erinnerungen.aendere(app, e.id) { it.copy(form = Erinnerung.FORM_MELDUNG) } } }
                        Chip("Wecker", e.wecker) { bereich.launch(Dispatchers.IO) { Erinnerungen.aendere(app, e.id) { it.copy(form = Erinnerung.FORM_WECKER) } } }
                    }
                }
                Text(if (e.allgemein) "Hinweise" else "Erinnerungszeitpunkte", color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                if (e.allgemein && e.hinweisUm == 0L) Text("Jarvis meldet sich von selbst, wenn es zu Dienst, Schlaf und Tag passt.", color = f.textLeise, fontSize = 14.sp)
                e.termine().forEach { t ->
                    val q = Erinnerungen.quittung(app, t.quittung)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.anzeige, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(Erinnerungen.zeitText(t.zeit) + " · " + when {
                                q != null -> "bestätigt auf ${q.von}"
                                t.zeit > jetzt -> "geplant"
                                Erinnerungen.meldbar(t, jetzt) -> "fällig"
                                else -> "verpasst"
                            }, color = if (q != null) f.erfolg else f.textLeise, fontSize = 13.sp)
                        }
                        if (q == null && t.zeit <= jetzt) Knopf("Bestätigen", icon = Icons.Rounded.Check, haupt = false) { bereich.launch(Dispatchers.IO) { ErinnerungPlaner.bestaetige(app, t) } }
                        if (t.ausloesung != null) Box(Modifier.size(40.dp).antippen { bereich.launch(Dispatchers.IO) { Erinnerungen.aendere(app, e.id) { alt -> alt.copy(ausloesungen = alt.ausloesungen.filter { it.id != t.ausloesung.id }) } } }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Zeitpunkt entfernen", tint = f.textLeise, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                if (!e.allgemein) Box(Modifier.padding(top = 6.dp).size(48.dp).knopf3d(f.primaer, f.tertiaer, 24.dp, f.dunkel).antippen { plus = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "Erinnerungszeitpunkt hinzufügen", tint = f.aufPrimaer)
                }
                val bereit by ladeImHintergrund(false, e.kurz, stand) { withContext(Dispatchers.IO) { ErinnerungStimme.bereit(app, e.kurz, e.wecker) } }
                val vorlesen = de.frank.jarvis.meldung.Meldungen.profil(app, de.frank.jarvis.meldung.Meldungen.erinnerungsArt(e.wecker)).vorlesen
                Text(if (!vorlesen) "Vorlesen ist für diese Erinnerungsform ausgeschaltet." else if (bereit) "Sprachausgabe liegt offline bereit." else "Sprachausgabe wird erzeugt, sobald Netz da ist.", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                Text("Von ${de.frank.jarvis.ablage.AblageQuellen.mitZeit(e.quelle.ifBlank { de.frank.jarvis.ablage.AblageQuellen.HANDY }, e.erstellt)}", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            }
            if (loeschen) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Knopf("Wirklich löschen", Modifier.weight(1f), Icons.Rounded.Delete, farbe = f.gefahr) { bereich.launch(Dispatchers.IO) { Erinnerungen.loesche(app, e.id) } }
                Knopf("Abbrechen", Modifier.weight(1f), haupt = false) { loeschen = false }
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Knopf("Löschen", haupt = false, farbe = f.gefahr) { loeschen = true }
                Knopf(if (e.erledigt != 0L) "Wieder offen" else "Erledigt", Modifier.weight(1f), Icons.Rounded.Check, haupt = false) {
                    val neu = if (e.erledigt != 0L) 0L else System.currentTimeMillis()
                    bereich.launch(Dispatchers.IO) { Erinnerungen.aendere(app, e.id) { it.copy(erledigt = neu) } }
                }
                Knopf("Bearbeiten", Modifier.weight(1f), Icons.Rounded.Edit) { vm.erinnerungBearbeiten(e) }
            }
        }
        if (plus) AusloesungWahl(e, { plus = false }) { a ->
            plus = false
            val z = e.zeitVon(a)
            if (z == null || z <= System.currentTimeMillis()) vm.meldung = "Dieser Zeitpunkt ist schon vorbei."
            else if (Erinnerungen.doppelt(e, a)) vm.meldung = "Dieser Zeitpunkt ist schon eingestellt."
            else bereich.launch(Dispatchers.IO) { Erinnerungen.aendere(app, e.id) { it.copy(ausloesungen = it.ausloesungen + a) } }
        }
    }
}

/** Auswahl hinter jedem Plus: wie viele Tage oder Stunden vor dem Termin; bei reinem Datum wahlweise mit eigener Uhrzeit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AusloesungWahl(e: Erinnerung, schliessen: () -> Unit, fertig: (Ausloesung) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current
    var anzahl by remember { mutableStateOf("") }
    var inStunden by remember { mutableStateOf(e.uhrzeit != null) }
    var uhr by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = schliessen)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).pointerInput(Unit) { detectTapGestures { schliessen() } }, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(16.dp).fillMaxWidth().background(f.hgUnten, androidx.compose.foundation.shape.RoundedCornerShape(f.radius)).glas(f, fuellung = f.flaecheStark).pointerInput(Unit) { detectTapGestures { } }.padding(16.dp).imePadding()) {
            Text("Wann erinnern?", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            FlowRow(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val vorgaben = (if (e.uhrzeit != null) listOf("Zum Termin" to 0, "1 Stunde" to 60, "3 Stunden" to 180) else listOf("Am Termintag" to 0)) + listOf("1 Tag" to 1440, "2 Tage" to 2880, "1 Woche" to 10080, "2 Wochen" to 20160, "1 Monat" to 43200)
                vorgaben.forEach { (name, m) -> Chip(name, false) { fertig(Erinnerungen.neueAusloesung(m, uhr)) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Eingabe(anzahl, { w -> anzahl = w.filter { it.isDigit() }.take(4) }, "Anzahl", Modifier.weight(1f))
                if (e.uhrzeit != null) Chip("Stunden", inStunden) { inStunden = true }
                Chip("Tage", !inStunden) { inStunden = false }
            }
            if (e.uhrzeit == null) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Uhrzeit: ${uhr ?: "${e.ohneUhrzeitUm} (Vorgabe)"}", Modifier.weight(1f), color = f.textLeise, fontSize = 14.sp)
                Chip("Wählen", uhr != null) { uhrWaehlen(context, uhr ?: e.ohneUhrzeitUm) { uhr = it } }
            }
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Knopf("Abbrechen", Modifier.weight(1f), haupt = false, aktion = schliessen)
                Knopf("Hinzufügen", Modifier.weight(1f), Icons.Rounded.Add) { anzahl.toIntOrNull()?.let { n -> fertig(Erinnerungen.neueAusloesung(if (inStunden) n * 60 else n * 1440, uhr)) } }
            }
        }
    }
}

internal fun uhrWaehlen(context: android.content.Context, von: String, fertig: (String) -> Unit) {
    val (h, m) = von.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 10) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
    TimePickerDialog(context, { _, a, b -> fertig(String.format(Locale.GERMAN, "%02d:%02d", a, b)) }, h, m, true).show()
}

private fun datumWaehlen(context: android.content.Context, von: String?, fertig: (String) -> Unit) {
    val d = von?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    DatePickerDialog(context, { _, j, m, t -> fertig(LocalDate.of(j, m + 1, t).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

/**
 * Eingabe einer neuen oder zu bearbeitenden Erinnerung als ganzer Bildschirm, wie die Prompt-Eingabe: einsprechen oder eintippen, KI verbessert (und schreibt Kurzfassung und Listenzeile), rückgängig machbar, Diskette speichert.
 * Bleiben Kurzfassung oder Zeile leer, erzeugt Jarvis sie nach dem Speichern.
 */
@Composable
fun ErinnerungEbene(vm: AppViewModel) {
    val d = vm.erinnerungEntwurf ?: return
    val f = LocalFarben.current
    val context = LocalContext.current
    val ausgang = remember { d }
    var verwerfen by remember { mutableStateOf(false) }
    val versuchen = { if ((d != ausgang || vm.nimmtAuf || vm.schreibtMit) && !verwerfen) verwerfen = true else vm.erinnerungEingabeSchliessen() }
    BackHandler(onBack = versuchen)
    val stil = TextStyle(color = f.text, fontSize = 16.sp, lineHeight = 22.sp)
    Column(Modifier.fillMaxSize().background(f.hgOben).pointerInput(Unit) { detectTapGestures { } }.windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (d.id != null) "Erinnerung bearbeiten" else "Neue Erinnerung", Modifier.weight(1f), color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.size(40.dp).antippen(aktion = versuchen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Schließen", tint = f.textLeise) }
        }
        Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Mit Datum", !d.allgemein) { vm.erinnerungEntwurfSetzen(d.copy(allgemein = false)) }
            Chip("Allgemein", d.allgemein) { vm.erinnerungEntwurfSetzen(d.copy(allgemein = true)) }
        }
        if (!d.allgemein) Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(d.datum?.let { t -> runCatching { LocalDate.parse(t).format(DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN)) }.getOrDefault(t) } ?: "Datum wählen", d.datum != null) { datumWaehlen(context, d.datum) { vm.erinnerungEntwurfSetzen(vm.erinnerungEntwurf!!.copy(datum = it)) } }
            Chip(d.uhrzeit?.let { "$it Uhr" } ?: "Ohne Uhrzeit", d.uhrzeit != null) { uhrWaehlen(context, d.uhrzeit ?: vm.einstellungen.erinnerungOhneUhrzeit) { vm.erinnerungEntwurfSetzen(vm.erinnerungEntwurf!!.copy(uhrzeit = it)) } }
            if (d.uhrzeit != null) Box(Modifier.size(36.dp).antippen { vm.erinnerungEntwurfSetzen(d.copy(uhrzeit = null)) }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Uhrzeit entfernen", tint = f.textLeise, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.weight(1f))
            Chip(if (d.form == Erinnerung.FORM_WECKER) "Wecker" else "Benachrichtigung", d.form == Erinnerung.FORM_WECKER) {
                vm.erinnerungEntwurfSetzen(d.copy(form = if (d.form == Erinnerung.FORM_WECKER) Erinnerung.FORM_MELDUNG else Erinnerung.FORM_WECKER))
            }
        }
        BasicTextField(d.text, { vm.erinnerungEntwurfSetzen(vm.erinnerungEntwurf!!.copy(text = it)) },
            Modifier.padding(vertical = 4.dp).weight(1f).fillMaxWidth().glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark),
            textStyle = stil, cursorBrush = SolidColor(f.primaer),
            decorationBox = { inhalt ->
                Box(Modifier.padding(12.dp)) {
                    if (d.text.isEmpty()) Text(if (vm.nimmtAuf) "Ich höre zu … tippe aufs Mikrofon zum Übernehmen" else if (vm.schreibtMit) "Schreibe mit …" else "Woran soll ich dich erinnern? Eintippen oder einsprechen", color = f.textSchwach, fontSize = 16.sp)
                    inhalt()
                }
            })
        Feld(d.kurz, "Kurzfassung zum Vorlesen (etwa 35 Wörter)", Modifier.heightIn(min = 44.dp, max = 96.dp)) { vm.erinnerungEntwurfSetzen(vm.erinnerungEntwurf!!.copy(kurz = it)) }
        Feld(d.zeile, "Listenzeile (etwa sechs Wörter)", Modifier.heightIn(min = 44.dp, max = 64.dp)) { vm.erinnerungEntwurfSetzen(vm.erinnerungEntwurf!!.copy(zeile = it)) }
        if (vm.erinnerungFormuliert) Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Kern(28.dp, f.primaer, aktiv = true)
            Text("Jarvis formuliert …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
        }
        if (verwerfen) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Knopf("Verwerfen", Modifier.weight(1f), Icons.Rounded.Delete, farbe = f.gefahr) { vm.erinnerungEingabeSchliessen() }
            Knopf("Weiter bearbeiten", Modifier.weight(1f), haupt = false) { verwerfen = false }
        } else Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Rund(if (vm.nimmtAuf) Icons.Rounded.Stop else Icons.Rounded.Mic, if (vm.nimmtAuf) "Aufnahme beenden" else "Einsprechen", if (vm.nimmtAuf) f.gefahr else f.primaer) { vm.erinnerungMikrofon() }
            if (vm.erinnerungVorKi != null) Knopf("Rückgängig", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Undo, haupt = false) { vm.erinnerungKiRueckgaengig() }
            else Knopf("KI", Modifier.weight(1f), Icons.Rounded.AutoAwesome, haupt = false) { vm.erinnerungFormulieren() }
            Rund(Icons.Rounded.Save, "Speichern", f.primaer) { vm.erinnerungSpeichern() }
        }
    }
}

@Composable
private fun Feld(wert: String, hinweis: String, modifier: Modifier, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    BasicTextField(wert, aendern, modifier.padding(vertical = 3.dp).fillMaxWidth().glas(f, 12.dp, erhoeht = 0.3f, fuellung = f.flaecheStark),
        textStyle = TextStyle(color = f.text, fontSize = 14.sp, lineHeight = 19.sp), cursorBrush = SolidColor(f.primaer),
        decorationBox = { inhalt -> Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { if (wert.isEmpty()) Text(hinweis, color = f.textSchwach, fontSize = 14.sp); inhalt() } })
}

@Composable
private fun Rund(icon: androidx.compose.ui.graphics.vector.ImageVector, beschreibung: String, farbe: Color, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(52.dp).knopf3d(farbe, if (farbe == f.primaer) f.tertiaer else farbe, 26.dp, f.dunkel).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = f.aufPrimaer, modifier = Modifier.size(24.dp))
    }
}
