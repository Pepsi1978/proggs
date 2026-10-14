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
fun StartBildschirm(vm: AppViewModel, tunnel: TunnelZustand, activity: ComponentActivity) {
    val f = LocalFarben.current
    // Vom Protokoll zählt hier nur, ob das Plugin schon benutzt wurde: Nicht jede neue Zeile soll den Bildschirm neu aufbauen.
    val pluginGenutzt by remember { Protokoll.eintraege.map { l -> l.any { it.quelle == Quelle.CHATGPT } }.distinctUntilChanged() }
        .collectAsState(initial = remember { Protokoll.eintraege.value.any { it.quelle == Quelle.CHATGPT } })
    val auswertung by Tagesauswertung.stand.collectAsState()
    var auswertungOffen by rememberSaveable { mutableStateOf(false) }
    var eingabe by rememberSaveable { mutableStateOf("") }
    val liste = rememberLazyListState()

    val schritte = listOf(
        Schritt("Mit ChatGPT verbinden", "Gibt Jarvis sein eigenes Denken. Einmal anmelden, fertig.", vm.kiVerbunden, "Verbinden") { vm.reiter = Reiter.EINSTELLUNGEN; vm.kiVerbinden(activity) },
        Schritt("Geniale Aufgaben", vm.aufgabenStoerung ?: "Angebunden: lesen und schreiben.", vm.aufgabenGeprueft && vm.aufgabenStoerung == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Kalender", vm.stoerungen["kalender"]?.let { "Damit Jarvis Termine und Dienstplan kennt, braucht er den Zugriff auf deinen Kalender." } ?: "Angebunden: Termine und Dienstplan lesen.", vm.aufgabenGeprueft && vm.stoerungen["kalender"] == null, "Erlauben") { vm.kalenderAnfragen() },
        Schritt("Geniale Ideen", vm.stoerungen["ideen"] ?: "Angebunden: lesen und schreiben.", vm.aufgabenGeprueft && vm.stoerungen["ideen"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Tagebuch", vm.stoerungen["tagebuch"] ?: "Angebunden: Einträge aus Drive lesen.", vm.aufgabenGeprueft && vm.stoerungen["tagebuch"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Genialer Wecker", vm.stoerungen["wecker"] ?: "Angebunden: Wecker lesen und stellen.", vm.aufgabenGeprueft && vm.stoerungen["wecker"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Entropie Reductor", vm.stoerungen["biomarker"] ?: "Angebunden: Biomarker lesen.", vm.aufgabenGeprueft && vm.stoerungen["biomarker"] == null, "Prüfen") { vm.lagePruefen() },
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

    val schublade = rememberGespraecheSchublade()
    val raum = androidx.compose.runtime.rememberCoroutineScope()
    GespraecheSchublade(vm, schublade) {
    Column(Modifier.fillMaxSize()) {
        Kopf(vm, tunnel, auswertung.laeuft) { raum.launch { schublade.open() } }
        LaeuftGerade(vm)
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
            if (vm.denkt) item(key = "denkt") {
                Row(Modifier.padding(start = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Kern(30.dp, f.primaer, aktiv = true)
                    Text(vm.schritt.ifEmpty { "Jarvis denkt" } + " …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
            Eingabe(eingabe, { eingabe = it }, if (vm.nimmtAuf) (if (vm.freihaendig) "Ich höre zu … sprich einfach" else "Ich höre zu … tippe zum Senden") else if (vm.schreibtMit) "Schreibe mit …" else "Nachricht an Jarvis", Modifier.weight(1f), einzeilig = false, senden = { vm.sende(eingabe); eingabe = "" })
            Spacer(Modifier.width(10.dp))
            // Leeres Feld: der Knopf ist das Mikrofon. Mit Text: senden. Während Jarvis arbeitet: abbrechen.
            val alsMikro = eingabe.isBlank() && !vm.denkt
            val rot = vm.denkt || vm.nimmtAuf
            Box(
                Modifier.size(52.dp).knopf3d(if (rot) f.gefahr else f.primaer, if (rot) f.gefahr else f.tertiaer, 26.dp, f.dunkel)
                    .antippen {
                        when {
                            vm.denkt -> vm.abbrechen()
                            alsMikro -> vm.mikrofonTippen()
                            else -> { vm.sende(eingabe); eingabe = "" }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    vm.schreibtMit -> Kern(34.dp, f.aufPrimaer, aktiv = true)
                    vm.denkt || vm.nimmtAuf -> Icon(Icons.Rounded.Stop, if (vm.denkt) "Abbrechen" else "Aufnahme beenden und senden", tint = f.aufPrimaer, modifier = Modifier.size(22.dp))
                    alsMikro -> Icon(Icons.Rounded.Mic, "Sprechen", tint = f.aufPrimaer, modifier = Modifier.size(24.dp))
                    else -> Icon(Icons.AutoMirrored.Rounded.Send, "Senden", tint = f.aufPrimaer, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
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
        // Tagesauswertung jetzt neu erstellen: frische Daten aller Apps holen und alles neu auswerten.
        if (wertetAus) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Kern(34.dp, f.primaer, aktiv = true) }
        else Rundknopf(Icons.Rounded.Refresh, "Tagesauswertung aktualisieren", farbe = f.primaer) { vm.auswertungJetzt() }
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

@Composable
private fun Blase(vm: AppViewModel, nummer: Int, n: Nachricht) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (n.vonMir) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        val grund = if (n.vonMir) Modifier.knopf3d(f.primaer, f.tertiaer, 20.dp, f.dunkel) else Modifier.glas(f, 20.dp, erhoeht = 0.6f, fuellung = f.flaecheStark)
        Text(
            n.text,
            Modifier.weight(1f, fill = false).widthIn(max = 460.dp).padding(start = if (n.vonMir) 44.dp else 0.dp).then(grund).padding(horizontal = 16.dp, vertical = 11.dp),
            color = if (n.vonMir) f.aufPrimaer else f.text, fontSize = 16.sp, lineHeight = 22.sp,
        )
        if (!n.vonMir) {
            Spacer(Modifier.width(6.dp))
            Vorleseknopf(vm, "gespraech:$nummer", "Jarvis", n.text, groesse = 36.dp)
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
