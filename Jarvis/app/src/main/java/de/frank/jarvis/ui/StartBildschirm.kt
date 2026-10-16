package de.frank.jarvis.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbUpOffAlt
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.tunnel.TunnelStufe
import de.frank.jarvis.tunnel.TunnelZustand
import de.frank.jarvis.ui.theme.Chip
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Die Schritte, deren Stand erst die Prüfung der angebundenen Apps zeigt. */
private val APP_SCHRITTE = setOf("Geniale Aufgaben", "Kalender", "Geniale Ideen", "Tagebuch", "Genialer Wecker", "Entropie Reductor")

/** Ein Schritt der Einrichtung: erledigt oder mit einem Knopf, der genau dorthin führt. */
private class Schritt(val titel: String, val text: String, val fertig: Boolean, val knopf: String, val aktion: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartBildschirm(vm: AppViewModel, tunnel: TunnelZustand, activity: ComponentActivity, schublade: androidx.compose.material3.DrawerState) {
    val f = LocalFarben.current
    // Vom Protokoll zählt hier nur, ob das Plugin schon benutzt wurde: Nicht jede neue Zeile soll den Bildschirm neu aufbauen.
    val pluginGenutzt by remember { Protokoll.eintraege.map { l -> l.any { it.quelle == Quelle.CHATGPT } }.distinctUntilChanged() }
        .collectAsState(initial = remember { Protokoll.eintraege.value.any { it.quelle == Quelle.CHATGPT } })
    val auswertung by Tagesauswertung.stand.collectAsState()
    var auswertungOffen by rememberSaveable { mutableStateOf(false) }
    val liste = rememberLazyListState()

    val schritte = listOf(
        Schritt("Mit ChatGPT verbinden", "Gibt Jarvis sein eigenes Denken. Einmal anmelden, fertig.", vm.kiVerbunden, "Verbinden") { vm.reiter = Reiter.EINSTELLUNGEN; vm.kiVerbinden(activity) },
        Schritt("Geniale Aufgaben", vm.aufgabenStoerung ?: "Angebunden: lesen und schreiben.", vm.aufgabenGeprueft && vm.aufgabenStoerung == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Kalender", vm.stoerungen["kalender"]?.let { "Damit Jarvis Termine und Dienstplan kennt, braucht er den Zugriff auf deinen Kalender." } ?: "Angebunden: Termine und Dienstplan lesen.", vm.aufgabenGeprueft && vm.stoerungen["kalender"] == null, "Erlauben") { vm.kalenderAnfragen() },
        Schritt("Geniale Ideen", vm.stoerungen["ideen"] ?: "Angebunden: lesen und schreiben.", vm.aufgabenGeprueft && vm.stoerungen["ideen"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Tagebuch", vm.stoerungen["tagebuch"] ?: "Angebunden: Einträge aus Drive lesen.", vm.aufgabenGeprueft && vm.stoerungen["tagebuch"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Genialer Wecker", vm.stoerungen["wecker"] ?: "Angebunden: Wecker lesen und stellen.", vm.aufgabenGeprueft && vm.stoerungen["wecker"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Entropie Reductor", vm.stoerungen["biomarker"] ?: "Angebunden: Biomarker lesen.", vm.aufgabenGeprueft && vm.stoerungen["biomarker"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Epigenetik", vm.stoerungen["epigenetik"] ?: "Angebunden: Auswertungen lesen und Recherchen starten.", vm.aufgabenGeprueft && vm.stoerungen["epigenetik"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt(
            "Verbindung zum Server",
            if (tunnel.stufe == TunnelStufe.FEHLER) tunnel.meldung else "Jarvis verbindet sich von selbst mit deinem Server und ist dann für ChatGPT erreichbar.",
            tunnel.stufe == TunnelStufe.ONLINE, "Ansehen",
        ) { vm.reiter = Reiter.EINSTELLUNGEN },
        Schritt("Plugin in ChatGPT eintragen", "Adresse kopieren und in ChatGPT als eigenen Konnektor „Jarvis“ anlegen.", pluginGenutzt, "Anleitung") { vm.reiter = Reiter.EINSTELLUNGEN },
        Schritt(
            "Hey Jarvis",
            when {
                !vm.weckwortMikro -> "Damit Jarvis auf „Hey Jarvis“ hört, braucht er das Mikrofon."
                else -> "Damit Jarvis nach „Hey Jarvis“ von selbst aufgeht, auch bei gesperrtem Handy: „Über anderen Apps einblenden“ erlauben."
            },
            !vm.einstellungen.weckwortAn || (vm.weckwortMikro && vm.ueberlagernErlaubt), "Erlauben",
        ) { vm.weckwortEinrichten(activity) },
        Schritt("Im Hintergrund wach bleiben", "Damit Jarvis auch bei gesperrtem Handy antwortet.", vm.akkuFrei, "Erlauben") {
            runCatching { activity.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${activity.packageName}"))) }
        },
    )
    // Solange die Prüfung der angebundenen Apps läuft, zählen sie nicht als offen: sonst blitzt die Karte beim Start kurz auf.
    val offen = schritte.count { !it.fertig && (vm.aufgabenGeprueft || it.titel !in APP_SCHRITTE) }

    LaunchedEffect(vm.gespraech.size, vm.denkt) {
        // Nur im laufenden Gespräch ans Ende springen; beim Start bleibt die Einrichtung oben sichtbar.
        val letzter = liste.layoutInfo.totalItemsCount - 1
        if (vm.gespraech.isNotEmpty() && letzter >= 0) liste.animateScrollToItem(letzter)
    }

    // Die Seitenleiste selbst liegt in JarvisApp: So reicht sie von ganz oben bis ganz unten, über die Reiter-Leiste.
    val raum = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Kopf(vm, tunnel, auswertung.laeuft) { raum.launch { schublade.open() } }
        LaeuftGerade(vm)
        // Die Stoppuhr erscheint nur, wenn Frank sie über Jarvis startet, und bleibt bis zum Zurücksetzen.
        StoppuhrKarte(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = liste,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (offen > 0) item(key = "einrichtung") {
                Karte(toenung = f.primaer) {
                    Text("Einrichtung", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Noch $offen von ${schritte.size} Schritten, dann hört Jarvis auf dein Wort.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    schritte.forEachIndexed { i, s -> SchrittZeile(i + 1, s) }
                }
            }
            if (vm.gespraech.isEmpty()) item(key = "auswertung") {
                Karte {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tagesauswertung", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val neueste = auswertung.neueste
                            Text(
                                when {
                                    auswertung.laeuft -> auswertung.schritt.ifEmpty { "Wird erstellt" } + " …"
                                    neueste == null -> "Noch keine. Nächste: ${vm.naechsteAuswertung}"
                                    else -> "Stand " + zeitKurz(neueste.zeit) + " Uhr · nächste ${vm.naechsteAuswertung}"
                                },
                                color = if (auswertung.laeuft) f.primaer else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        auswertung.neueste?.let { Vorleseknopf(vm, "auswertung", "Tagesauswertung", if (it.mitKi) it.text else it.text + "\n\n" + it.daten) }
                    }
                    auswertung.neueste?.let { neueste ->
                        Text(
                            if (neueste.mitKi) neueste.text else neueste.text + "\n\n" + neueste.daten,
                            Modifier.padding(top = 12.dp).fillMaxWidth().antippen(haptik = false) { auswertungOffen = !auswertungOffen },
                            color = f.text, fontSize = 15.sp, lineHeight = 21.sp,
                            maxLines = if (auswertungOffen) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                        )
                        Text(if (auswertungOffen) "Weniger zeigen" else "Ganz lesen", Modifier.padding(top = 6.dp).antippen { auswertungOffen = !auswertungOffen }, color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (vm.gespraech.isEmpty()) item(key = "gedaechtnis") {
                val lagebild by de.frank.jarvis.gedaechtnis.Lagebild.stand.collectAsState()
                Karte(Modifier.antippen { GedaechtnisAnsicht.offen = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Live-Gedächtnis", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val neu = lagebild.neuigkeiten.count { System.currentTimeMillis() - it.zeit < 24 * 3_600_000L }
                            val fehler = lagebild.abschnitte.count { it.value.fehler.isNotEmpty() }
                            Text(
                                "${lagebild.abschnitte.size} Bereiche aus allen Apps bereit" + (if (neu > 0) " · $neu Neuigkeiten heute" else "") +
                                    (if (fehler > 0) " · $fehler gestört" else "") + (if (lagebild.laeuft.isNotEmpty()) " · liest gerade …" else ""),
                                color = if (fehler > 0) f.gefahr else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Text("Ansehen", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (vm.gespraech.isEmpty()) item(key = "vorschlaege") {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("Frag mich etwas", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    Text("Hier schreibst du direkt mit Jarvis. Nach rechts wischen zeigt deine gespeicherten Gespräche. In ChatGPT sagst du einfach „Jarvis, …“.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Was steht heute an?", "Wie sind meine Biowerte heute?", "Wann habe ich wieder frei?", "Welche Aufgaben sind überfällig?").forEach { frage ->
                            Chip(frage, aktiv = false) { vm.sende(frage) }
                        }
                    }
                }
            }
            items(vm.gespraech.size) { i -> Blase(vm, i, vm.gespraech[i]) }
            item(key = "programmiert") { ProgrammierAktivitaet(vm) }
            if (vm.denkt) item(key = "denkt") {
                Row(Modifier.padding(start = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Kern(30.dp, f.primaer, aktiv = true)
                    Text(vm.schritt.ifEmpty { "Jarvis denkt" } + " …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
        // Die Eingabeblase wie in der ChatGPT-App: Text, Plus-Menü, Modell, Diktat und Live-Gespräch.
        EingabeBlase(vm)
    }
}

@Composable
private fun Kopf(vm: AppViewModel, tunnel: TunnelZustand, wertetAus: Boolean, leisteOeffnen: () -> Unit) {
    val f = LocalFarben.current
    val (zeile, farbe) = when (tunnel.stufe) {
        TunnelStufe.ONLINE -> "Online" to f.erfolg
        TunnelStufe.VERBINDET -> tunnel.meldung to f.primaer
        TunnelStufe.NICHT_EINGERICHTET -> "Server noch nicht eingerichtet" to f.textLeise
        TunnelStufe.FEHLER -> tunnel.meldung to f.gefahr
        TunnelStufe.AUS -> "Ausgeschaltet" to f.textLeise
    }
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        // Gespräche: dasselbe wie von links nach rechts wischen.
        Rundknopf(Icons.Rounded.Menu, "Gespräche", farbe = f.primaer) { leisteOeffnen() }
        Spacer(Modifier.width(6.dp))
        Kern(56.dp, if (tunnel.stufe == TunnelStufe.FEHLER) f.gefahr else f.primaer, aktiv = vm.denkt || tunnel.stufe == TunnelStufe.VERBINDET)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Jarvis", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(zeile, color = farbe, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (vm.gespraech.isNotEmpty()) {
            Rundknopf(Icons.Rounded.AddComment, "Neues Gespräch", farbe = f.primaer) { vm.neuesGespraech() }
            Spacer(Modifier.width(8.dp))
        }
        // Tagesauswertung jetzt neu erstellen: frische Daten aller Apps holen und alles neu auswerten.
        if (wertetAus) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Kern(34.dp, f.primaer, aktiv = true) }
        else Rundknopf(Icons.Rounded.Refresh, "Tagesauswertung aktualisieren", farbe = f.primaer) { vm.auswertungJetzt() }
        Spacer(Modifier.width(8.dp))
        // Design umschalten: Blau ↔ Gold wie Geniale Ideen; hell oder dunkel bleibt beim Knopf daneben.
        Rundknopf(Icons.Rounded.Palette, "Design wechseln", farbe = f.primaer) {
            val ids = de.frank.jarvis.ui.theme.DESIGNS
            val i = ids.indexOfFirst { it.first == vm.einstellungen.design }
            val neu = ids[(i + 1) % ids.size]
            vm.einstellungen.design = neu.first
            vm.meldung = "Design: " + neu.second
        }
        Spacer(Modifier.width(8.dp))
        // Hell → Automatisch (wie das System) → Dunkel → Hell, wie in Geniale Aufgaben. Das Symbol zeigt den aktuellen Modus.
        val modus = vm.einstellungen.modus
        Rundknopf(
            when (modus) { "hell" -> Icons.Rounded.LightMode; "dunkel" -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto },
            when (modus) { "hell" -> "Hell (weiter zu Automatisch)"; "dunkel" -> "Dunkel (weiter zu Hell)"; else -> "Automatisch (weiter zu Dunkel)" },
            farbe = f.primaer,
        ) {
            val neu = when (modus) { "hell" -> "system"; "system" -> "dunkel"; else -> "hell" }
            vm.einstellungen.modus = neu
            vm.meldung = when (neu) { "hell" -> "Hell"; "dunkel" -> "Dunkel"; else -> "Automatisch wie das System" }
        }
    }
}

@Composable
private fun SchrittZeile(nummer: Int, s: Schritt) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).then(if (s.fertig) Modifier.knopf3d(f.erfolg, f.erfolg, 15.dp, f.dunkel) else Modifier.glas(f, 15.dp, erhoeht = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            if (s.fertig) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            else Text("$nummer", color = f.textLeise, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(s.titel, color = if (s.fertig) f.textLeise else f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (!s.fertig) Text(s.text, color = f.textLeise, fontSize = 13.sp, lineHeight = 17.sp)
        }
        if (!s.fertig) Knopf(s.knopf, Modifier.height(40.dp), haupt = false, aktion = s.aktion)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Blase(vm: AppViewModel, nummer: Int, n: Nachricht) {
    if (n.art == "rueckfrage") { Rueckfrage(vm, n); return }
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (n.vonMir) Alignment.End else Alignment.Start) {
        if (n.anhaenge.isNotEmpty()) FlowRow(Modifier.padding(start = 44.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            n.anhaenge.forEach { AnhangKachel(de.frank.jarvis.data.Anhang(it), 96.dp) }
        }
        if (n.vonMir) {
            // Franks Nachricht: dezente Blase rechts, wie in der ChatGPT-App.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Markierbar(hell = false) {
                    Text(
                        n.text,
                        Modifier.weight(1f, fill = false).widthIn(max = 460.dp).padding(start = 48.dp)
                            .background(f.flaecheStark, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                        color = f.text, fontSize = 16.sp, lineHeight = 22.sp,
                    )
                }
            }
            Kopierknopf(vm, n.text, "Frage")
        } else {
            // Jarvis' Antwort: Fließtext über die ganze Breite, ohne Blase, gut gegliedert.
            Markierbar(hell = false) {
                val form = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp, top = 2.dp)
                when (n.art) {
                    "programmieren" -> ProgrammierText(n.text, form, farbe = f.text)
                    "fehler", "abgebrochen" -> Text(n.text, form, color = f.gefahr, fontSize = 15.sp, lineHeight = 21.sp)
                    else -> AntwortText(n.text, form)
                }
            }
            AntwortLeiste(vm, nummer, n)
        }
    }
}

/**
 * Die Leiste unter einer Antwort wie in der ChatGPT-App: Kopieren, Daumen hoch, Daumen runter, Vorlesen, Teilen,
 * in neuem Chat fortsetzen und erneut versuchen. Ein Daumen runter fragt kurz nach dem Grund (ein Tipp, kein Zwang).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AntwortLeiste(vm: AppViewModel, nummer: Int, n: Nachricht) {
    val f = LocalFarben.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var grundFragen by remember { mutableStateOf(false) }
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Kopierknopf(vm, n.text, "Antwort")
        LeistenKnopf(if (n.bewertung > 0) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt, "Gute Antwort", if (n.bewertung > 0) f.primaer else f.textLeise) {
            vm.bewerten(nummer, if (n.bewertung > 0) 0 else 1)
        }
        LeistenKnopf(if (n.bewertung < 0) Icons.Rounded.ThumbDown else Icons.Rounded.ThumbDownOffAlt, "Schlechte Antwort", if (n.bewertung < 0) f.primaer else f.textLeise) {
            if (n.bewertung < 0) vm.bewerten(nummer, 0) else grundFragen = true
        }
        Vorleseknopf(vm, "gespraech:$nummer", "Jarvis", n.text, groesse = 36.dp)
        LeistenKnopf(Icons.Rounded.Share, "Teilen", f.textLeise) {
            vm.sperrAufschubBis = System.currentTimeMillis() + 120_000
            runCatching {
                context.startActivity(android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, n.text), "Antwort teilen",
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        LeistenKnopf(Icons.AutoMirrored.Rounded.CallSplit, "In neuem Chat fortsetzen", f.textLeise) { vm.abzweigen(nummer) }
        LeistenKnopf(Icons.Rounded.Refresh, "Erneut versuchen", f.textLeise) { vm.erneutVersuchen(nummer) }
    }
    if (grundFragen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { grundFragen = false }, containerColor = f.flaecheStark) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Was hat nicht gepasst?", color = f.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Ein Tipp genügt. Daraus lernt Jarvis, wie du Antworten lieber hättest.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                de.frank.jarvis.data.Stil.GRUENDE.forEach { (id, name) -> Chip(name, aktiv = false) { grundFragen = false; vm.bewerten(nummer, -1, id) } }
                Chip("Überspringen", aktiv = false) { grundFragen = false; vm.bewerten(nummer, -1) }
            }
        }
    }
}

@Composable
private fun LeistenKnopf(icon: androidx.compose.ui.graphics.vector.ImageVector, beschreibung: String, farbe: Color, aktion: () -> Unit) {
    Box(Modifier.size(36.dp).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = farbe, modifier = Modifier.size(19.dp))
    }
}

/** Textauswahl mit Griffen und Markierung, die auf jedem Blasengrund sichtbar bleiben. */
@Composable
private fun Markierbar(hell: Boolean, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    val farbe = if (hell) f.aufPrimaer else f.primaer
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.text.selection.LocalTextSelectionColors provides
            androidx.compose.foundation.text.selection.TextSelectionColors(handleColor = farbe, backgroundColor = farbe.copy(alpha = 0.35f)),
    ) { androidx.compose.foundation.text.selection.SelectionContainer { inhalt() } }
}

/** Dezentes Kopiersymbol unter einer Nachricht: kopiert genau ihren ganzen Text und zeigt kurz einen Haken. */
@Composable
private fun Kopierknopf(vm: AppViewModel, text: String, was: String) {
    val f = LocalFarben.current
    var kopiert by remember(text) { mutableStateOf(false) }
    LaunchedEffect(kopiert) { if (kopiert) { kotlinx.coroutines.delay(1_500); kopiert = false } }
    Box(
        Modifier.size(36.dp).antippen { vm.kopiere(text, was); kopiert = true },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (kopiert) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            if (kopiert) "$was kopiert" else "$was kopieren",
            tint = if (kopiert) f.erfolg else f.textLeise, modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Arbeitet die Programmier-Sitzung dieses Gesprächs gerade, steht unter der letzten Nachricht ein sich drehendes, pulsierendes
 * Symbol wie im Terminal, dazu der letzte Schritt und wie lange die letzte Aktivität her ist. Kommt lange nichts mehr (über
 * drei Minuten), wird die Zeit rot: Dann lohnt ein Blick in die Sitzung. Endet der Zug oder wartet sie auf Frank, verschwindet es.
 */
@Composable
private fun ProgrammierAktivitaet(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by de.frank.jarvis.faehigkeit.ProgrammierChat.aktivitaet.collectAsState()
    val a = alle[vm.sitzungId ?: return] ?: return
    if (!a.arbeitet) return
    // Wie das Arbeitssymbol von Claude Code: die Zeichen laufen hin und zurück, alle 120 ms eins weiter, in Claude-Orange.
    var bild by remember { mutableStateOf(0) }
    var jetzt by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(120)
            bild = (bild + 1) % SPINNER.size
            jetzt = System.currentTimeMillis()
        }
    }
    // Ein neues Wort je neuem Schritt, wie Claude Code je Zug („Drizzling…“, „Cooking…“).
    val wort = remember(a.schritt) { SPINNER_WOERTER.random() }
    val sekunden = ((jetzt - a.zuletzt) / 1_000).coerceAtLeast(0)
    val seit = if (sekunden < 90) "${sekunden}s" else "${sekunden / 60} min"
    // Zwei Zeilen, damit auf dem zugeklappten Handy nichts abgeschnitten wird: oben Wort und Laufzeit des Auftrags,
    // darunter (eingerückt, bis drei Zeilen) App und letzte Aktivität.
    Column(Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(SPINNER[bild], color = CLAUDE_ORANGE, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
            Text("$wort…", color = CLAUDE_ORANGE, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            if (a.start > 0) Text(" · " + laufzeit((jetzt - a.start) / 1_000), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "(${a.app} · letzte Aktivität vor $seit)",
            color = if (sekunden > 180) f.gefahr else f.textLeise, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 28.dp, top = 1.dp),
        )
    }
}

/** Laufzeit wie im Terminal: „42 s“, „4 min 12 s“, „1 h 3 min“. */
private fun laufzeit(sekunden: Long): String {
    val s = sekunden.coerceAtLeast(0)
    return when {
        s < 60 -> "$s s"
        s < 3_600 -> "${s / 60} min ${s % 60} s"
        else -> "${s / 3_600} h ${s % 3_600 / 60} min"
    }
}

internal val CLAUDE_ORANGE = Color(0xFFD97757)
/** Die Bilder des Claude-Code-Arbeitssymbols, alle 120 ms eins weiter; auch die Seitenleiste zeigt es an arbeitenden Programmier-Gesprächen. */
internal val SPINNER = listOf("·", "✢", "✳", "✶", "✻", "✽", "✻", "✶", "✳", "✢")
private val SPINNER_WOERTER = listOf(
    "Drizzling", "Cooking", "Brewing", "Baking", "Clauding", "Cogitating", "Percolating", "Simmering", "Marinating", "Noodling",
    "Pondering", "Ruminating", "Conjuring", "Crafting", "Forging", "Hatching", "Mulling", "Musing", "Stewing", "Synthesizing",
    "Transmuting", "Vibing", "Puttering", "Schlepping", "Reticulating", "Finagling", "Moseying", "Churning", "Crunching", "Computing",
)

/**
 * Rückfrage einer Programmier-Sitzung (AskUserQuestion oder freie Frage am Zugende): je Frage die Optionen als Knöpfe
 * A, B, C, D (bei Mehrfachauswahl zum Umschalten), dazu ein Feld für eine eigene Antwort. Nach dem Senden steht die
 * Antwort darunter, die Knöpfe verschwinden. Frank kann auch einfach sprechen; Jarvis ordnet es mit programmier_antwort zu.
 */
@Composable
private fun Rueckfrage(vm: AppViewModel, n: Nachricht) {
    val f = LocalFarben.current
    val k = remember(n.kontext) { runCatching { org.json.JSONObject(n.kontext) }.getOrNull() }
    Column(Modifier.fillMaxWidth().widthIn(max = 520.dp).glas(f, 20.dp, erhoeht = 0.6f, fuellung = f.flaecheStark).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Markierbar(hell = false) { ProgrammierText(n.text, farbe = f.text, fontWeight = FontWeight.SemiBold) }
        Kopierknopf(vm, n.text, "Frage")
        if (k == null) return@Column
        if (k.optBoolean("beantwortet")) {
            Text("✓ Beantwortet: " + k.optString("antwort"), color = f.erfolg, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            return@Column
        }
        val fragen = k.optJSONArray("fragen") ?: org.json.JSONArray()
        val auswahl = remember(n.kontext) { androidx.compose.runtime.mutableStateMapOf<Int, Set<String>>() }
        var frei by remember(n.kontext) { mutableStateOf("") }
        if (!k.optBoolean("frei")) for (i in 0 until fragen.length()) {
            val frage = fragen.optJSONObject(i) ?: continue
            val mehrfach = frage.optBoolean("multiSelect")
            if (fragen.length() > 1 || frage.optString("header").isNotEmpty()) Text(
                listOf(frage.optString("header"), frage.optString("question")).filter { it.isNotEmpty() }.joinToString(": ") + if (mehrfach) " (mehrere möglich)" else "",
                color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp),
            )
            val optionen = frage.optJSONArray("options") ?: org.json.JSONArray()
            for (j in 0 until optionen.length()) {
                val option = optionen.optJSONObject(j) ?: continue
                val label = option.optString("label")
                val gewaehlt = label in auswahl[i].orEmpty()
                Knopf("${'A' + j} · $label", Modifier.padding(top = 6.dp).fillMaxWidth(), haupt = gewaehlt) {
                    val bisher = auswahl[i].orEmpty()
                    auswahl[i] = if (mehrfach) (if (gewaehlt) bisher - label else bisher + label) else setOf(label)
                }
                option.optString("description").takeIf { it.isNotEmpty() }?.let {
                    Text(it, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
            }
        }
        Eingabe(frei, { frei = it }, if (k.optBoolean("frei")) "Deine Antwort" else "Eigene Antwort (Sonstiges)", Modifier.padding(top = 10.dp).fillMaxWidth(), einzeilig = false)
        val bereit = frei.isNotBlank() || auswahl.values.any { it.isNotEmpty() }
        if (bereit) Knopf("Antwort senden", Modifier.padding(top = 10.dp).fillMaxWidth()) {
            val antworten = (0 until fragen.length()).map { i -> auswahl[i].orEmpty().joinToString(", ").ifEmpty { frei.trim() } }
            vm.programmierAntwort(k.optString("frage_id"), if (k.optBoolean("frei")) emptyList() else antworten, frei.trim())
        }
    }
}

/**
 * Alles, was gerade läuft (Timer mit Restzeit, arbeitende Agenten), steht immer oben auf der Startseite,
 * auch während eines Gesprächs. Ohne Laufendes bleibt der Platz frei.
 */
@Composable
private fun LaeuftGerade(vm: AppViewModel) {
    val f = LocalFarben.current
    val timer by de.frank.jarvis.timer.Timer.laufend.collectAsState()
    val agenten by de.frank.jarvis.agent.Agenten.laeufe.collectAsState()
    KlingelndeTimer(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    if (timer.isEmpty() && agenten.isEmpty()) return
    var jetzt by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer.isNotEmpty()) {
        while (timer.isNotEmpty()) {
            jetzt = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000 - jetzt % 1_000)
        }
    }
    val kontext = androidx.compose.ui.platform.LocalContext.current
    Karte(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), toenung = f.primaer) {
        Text("Läuft gerade", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        timer.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = f.primaer, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(de.frank.jarvis.timer.Timer.restUhr(t.ende - jetzt), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        t.name + " · " + de.frank.jarvis.timer.Timer.dauerText(t.dauerMs) + " · klingelt " +
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMAN).format(java.util.Date(t.ende)) + " Uhr",
                        color = f.textLeise, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Rundknopf(Icons.Rounded.Close, "Timer stoppen", farbe = f.gefahr) {
                    de.frank.jarvis.timer.Timer.stoppen(kontext, t.id)
                    vm.meldung = "Timer gestoppt"
                }
            }
        }
        agenten.forEach { l ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Kern(22.dp, f.primaer, aktiv = true)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("Agent " + l.agent, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(l.schritt.ifEmpty { l.auftrag }, color = f.textLeise, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Timer, die gerade wie ein Wecker klingeln: groß, mit „Aus“ und „Schlummern“. Auch auf dem Sperr-Bildschirm von Jarvis. */
@Composable
fun KlingelndeTimer(modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val klingelt by de.frank.jarvis.timer.Timer.klingelt.collectAsState()
    if (klingelt.isEmpty()) return
    val kontext = androidx.compose.ui.platform.LocalContext.current
    val schlummern = de.frank.jarvis.data.Einstellungen.get(kontext).timerSchlummern
    Karte(modifier, toenung = f.gefahr) {
        klingelt.forEachIndexed { i, t ->
            Row(Modifier.fillMaxWidth().padding(top = if (i > 0) 14.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = f.gefahr, modifier = Modifier.size(26.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(t.name + " ist abgelaufen", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Nach " + de.frank.jarvis.timer.Timer.dauerText(t.dauerMs), color = f.textLeise, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Knopf("Aus", Modifier.weight(1f), farbe = f.gefahr) { de.frank.jarvis.timer.Timer.klingelnAus(kontext, t.id) }
                Knopf("Schlummern $schlummern min", Modifier.weight(1f), haupt = false) { de.frank.jarvis.timer.Timer.schlummern(kontext, t.id) }
            }
        }
    }
}
