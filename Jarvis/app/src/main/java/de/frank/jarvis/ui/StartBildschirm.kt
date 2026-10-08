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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

/** Ein Schritt der Einrichtung: erledigt oder mit einem Knopf, der genau dorthin führt. */
private class Schritt(val titel: String, val text: String, val fertig: Boolean, val knopf: String, val aktion: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartBildschirm(vm: AppViewModel, tunnel: TunnelZustand, activity: ComponentActivity) {
    val f = LocalFarben.current
    val protokoll by Protokoll.eintraege.collectAsState()
    val auswertung by Tagesauswertung.stand.collectAsState()
    var auswertungOffen by rememberSaveable { mutableStateOf(false) }
    var eingabe by rememberSaveable { mutableStateOf("") }
    val liste = rememberLazyListState()

    val pluginGenutzt = protokoll.any { it.quelle == Quelle.CHATGPT }
    val schritte = listOf(
        Schritt("Mit ChatGPT verbinden", "Gibt Jarvis sein eigenes Denken. Einmal anmelden, fertig.", vm.kiVerbunden, "Verbinden") { vm.reiter = Reiter.EINSTELLUNGEN; vm.kiVerbinden(activity) },
        Schritt("Geniale Aufgaben", vm.aufgabenStoerung ?: "Angebunden: lesen und schreiben.", vm.aufgabenGeprueft && vm.aufgabenStoerung == null, "Prüfen") { vm.lagePruefen() },
        Schritt("Kalender", vm.stoerungen["kalender"]?.let { "Damit Jarvis Termine und Dienstplan kennt, braucht er den Zugriff auf deinen Kalender." } ?: "Angebunden: Termine und Dienstplan lesen.", vm.aufgabenGeprueft && vm.stoerungen["kalender"] == null, "Erlauben") { vm.kalenderAnfragen() },
        Schritt("Entropie Reductor", vm.stoerungen["biomarker"] ?: "Angebunden: Biomarker lesen.", vm.aufgabenGeprueft && vm.stoerungen["biomarker"] == null, "Prüfen") { vm.lagePruefen() },
        Schritt(
            "Verbindung zum Server",
            if (tunnel.stufe == TunnelStufe.FEHLER) tunnel.meldung else "Jarvis verbindet sich von selbst mit deinem Server und ist dann für ChatGPT erreichbar.",
            tunnel.stufe == TunnelStufe.ONLINE, "Ansehen",
        ) { vm.reiter = Reiter.EINSTELLUNGEN },
        Schritt("Plugin in ChatGPT eintragen", "Adresse kopieren und in ChatGPT als eigenen Konnektor „Jarvis“ anlegen.", pluginGenutzt, "Anleitung") { vm.reiter = Reiter.EINSTELLUNGEN },
        Schritt("Im Hintergrund wach bleiben", "Damit Jarvis auch bei gesperrtem Handy antwortet.", vm.akkuFrei, "Erlauben") {
            runCatching { activity.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${activity.packageName}"))) }
        },
    )
    val offen = schritte.count { !it.fertig }

    LaunchedEffect(vm.gespraech.size, vm.denkt) {
        // Nur im laufenden Gespräch ans Ende springen; beim Start bleibt die Einrichtung oben sichtbar.
        val letzter = liste.layoutInfo.totalItemsCount - 1
        if (vm.gespraech.isNotEmpty() && letzter >= 0) liste.animateScrollToItem(letzter)
    }

    Column(Modifier.fillMaxSize()) {
        Kopf(vm, tunnel)
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
                        if (auswertung.laeuft) Kern(34.dp, f.primaer, aktiv = true)
                        else Knopf("Jetzt", Modifier.height(40.dp), haupt = false) { vm.auswertungJetzt() }
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
            if (vm.gespraech.isEmpty()) item(key = "vorschlaege") {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("Frag mich etwas", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    Text("Hier schreibst du direkt mit Jarvis. In ChatGPT sagst du einfach „Jarvis, …“.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Was steht heute an?", "Wie sind meine Biowerte heute?", "Wann habe ich wieder frei?", "Welche Aufgaben sind überfällig?").forEach { frage ->
                            Chip(frage, aktiv = false) { vm.sende(frage) }
                        }
                    }
                }
            }
            items(vm.gespraech) { n -> Blase(n) }
            if (vm.denkt) item(key = "denkt") {
                Row(Modifier.padding(start = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Kern(30.dp, f.primaer, aktiv = true)
                    Text(vm.schritt.ifEmpty { "Jarvis denkt" } + " …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
            Eingabe(eingabe, { eingabe = it }, "Nachricht an Jarvis", Modifier.weight(1f), einzeilig = false, senden = { vm.sende(eingabe); eingabe = "" })
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(52.dp).knopf3d(if (vm.denkt) f.gefahr else f.primaer, if (vm.denkt) f.gefahr else f.tertiaer, 26.dp, f.dunkel)
                    .antippen { if (vm.denkt) vm.abbrechen() else { vm.sende(eingabe); eingabe = "" } },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (vm.denkt) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send, if (vm.denkt) "Abbrechen" else "Senden", tint = f.aufPrimaer, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Kopf(vm: AppViewModel, tunnel: TunnelZustand) {
    val f = LocalFarben.current
    val (zeile, farbe) = when (tunnel.stufe) {
        TunnelStufe.ONLINE -> "Online · bereit für ChatGPT" to f.erfolg
        TunnelStufe.VERBINDET -> tunnel.meldung to f.primaer
        TunnelStufe.NICHT_EINGERICHTET -> "Server noch nicht eingerichtet" to f.textLeise
        TunnelStufe.FEHLER -> tunnel.meldung to f.gefahr
        TunnelStufe.AUS -> "Ausgeschaltet" to f.textLeise
    }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Kern(64.dp, if (tunnel.stufe == TunnelStufe.FEHLER) f.gefahr else f.primaer, aktiv = vm.denkt || tunnel.stufe == TunnelStufe.VERBINDET)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Jarvis", color = f.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(zeile, color = farbe, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (vm.gespraech.isNotEmpty()) {
            Box(Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen { vm.gespraechLeeren() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.DeleteSweep, "Gespräch leeren", tint = f.textLeise, modifier = Modifier.size(20.dp))
            }
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

@Composable
private fun Blase(n: Nachricht) {
    val f = LocalFarben.current
    Box(Modifier.fillMaxWidth(), contentAlignment = if (n.vonMir) Alignment.CenterEnd else Alignment.CenterStart) {
        val grund = if (n.vonMir) Modifier.knopf3d(f.primaer, f.tertiaer, 20.dp, f.dunkel) else Modifier.glas(f, 20.dp, erhoeht = 0.6f, fuellung = f.flaecheStark)
        Text(
            n.text,
            Modifier.widthIn(max = 460.dp).padding(start = if (n.vonMir) 44.dp else 0.dp, end = if (n.vonMir) 0.dp else 44.dp).then(grund).padding(horizontal = 16.dp, vertical = 11.dp),
            color = if (n.vonMir) f.aufPrimaer else f.text, fontSize = 16.sp, lineHeight = 22.sp,
        )
    }
}
