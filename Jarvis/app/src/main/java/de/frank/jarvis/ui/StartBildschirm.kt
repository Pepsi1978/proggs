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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.isActive
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.key
import de.frank.jarvis.ui.theme.LocalBewegung
import kotlinx.coroutines.delay
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
    var profilWahlOffen by rememberSaveable { mutableStateOf(false) }
    val liste = rememberLazyListState()
    // Rückfragen der Programmier-Sitzungen: Vollbild zum Beantworten, Abgleich mit dem Server.
    RueckfrageFenster(vm)

    val schritte = listOf(
        Schritt("Mit ChatGPT verbinden", "Gibt Jarvis sein eigenes Denken. Einmal anmelden, fertig.", vm.kiVerbunden, "Verbinden") { vm.reiter = Reiter.EINSTELLUNGEN; vm.kiVerbinden() },
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

    // Mitlesen: Nur wer unten steht, folgt neuen Nachrichten. Hat Frank nach oben gescrollt, bleibt seine Lesestelle stehen,
    // während unten weiter Meldungen einlaufen; scrollt er wieder ganz nach unten, folgt die Ansicht erneut. Früher sprang
    // jede neue Programmiermeldung ans Ende, auch mitten im Lesen.
    // Als State-Objekt: Verbindung und Sammler unten hängen am selben Objekt und wechseln mit dem Gespräch mit.
    val folgenZustand = remember(vm.sitzungId) { mutableStateOf(true) }
    var folgen by folgenZustand
    // Nur Franks Finger laufen über nestedScroll, der automatische Sprung ans Ende nicht. So zählt ein Griff nach oben auch
    // dann, wenn er mitten in einen laufenden Sprung fällt (das ließ der alte Merker „automatisch“ durch). Maßgeblich ist,
    // was die Liste selbst nach oben verschoben hat: Wischen in einem Codeblock oder in einem kurzen Chat zählt nicht.
    val nachOben = remember(folgenZustand) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed.y > 0f) folgenZustand.value = false
                return Offset.Zero
            }
        }
    }
    // Mitlesen mittig (Franks Wunsch, gerade auf dem aufgeklappten Fold): Ohne eigenes Scrollen steht das Ende des Gesprächs,
    // also das, was gerade hereinkommt, in der Bildschirmmitte. Dafür bekommt die Liste unten eine halbe Listenhöhe Polster
    // und stapelt kurze Gespräche von unten: Ganz unten angekommen, endet der Text genau in der Mitte, darüber steht der Rest.
    val dichte = androidx.compose.ui.platform.LocalDensity.current
    var listenHoehe by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val schweber = if (vm.liveAktiv) 0.dp else schwebeHoehe(vm.nachtragMoeglich, papierkorb = vm.gespraech.isNotEmpty() || vm.sitzungId != null) + 10.dp
    // Ganz unten angekommen, egal wie: wieder mitlesen.
    LaunchedEffect(liste, folgenZustand) {
        androidx.compose.runtime.snapshotFlow { liste.canScrollForward }.collect { weiter -> if (!weiter) folgenZustand.value = true }
    }
    // Beim Mitlesen bleibt das Ende in der Mitte, auch wenn die letzte Nachricht wächst, darunter der Arbeitsstand läuft oder
    // das Handy auf- oder zugeklappt wird. Greift Frank selbst ein (Finger, laufende Animation), wartet das.
    LaunchedEffect(liste, folgenZustand) {
        androidx.compose.runtime.snapshotFlow { folgenZustand.value && vm.gespraech.isNotEmpty() && !liste.isScrollInProgress && liste.canScrollForward }
            .collect { nachziehen ->
                if (!nachziehen) return@collect
                try { liste.scrollBy(100_000f) } catch (e: kotlinx.coroutines.CancellationException) { if (!isActive) throw e }
            }
    }
    LaunchedEffect(vm.gespraech.size, vm.denkt) {
        // Eigene Nachricht abgeschickt: Frank will die Antwort sehen, also wieder mitlesen.
        if (vm.gespraech.lastOrNull()?.vonMir == true) folgen = true
        // Nur im laufenden Gespräch ans Ende springen; beim Start bleibt die Einrichtung oben sichtbar.
        val letzter = liste.layoutInfo.totalItemsCount - 1
        if (folgen && vm.gespraech.isNotEmpty() && letzter >= 0) {
            // Greift Frank in den Sprung, bricht er ab; das Abbrechen des ganzen Effekts (neue Nachricht) geht weiter durch.
            try { liste.animateScrollToItem(letzter) } catch (e: kotlinx.coroutines.CancellationException) { if (!isActive) throw e }
            // Rest bis ganz unten ohne Animation: die neue Zeile war beim Lesen der Anzahl evtl. noch nicht vermessen,
            // oder die letzte Blase ist höher als der Bildschirm (dann steht nach dem Sprung nur ihr Anfang oben).
            if (folgen && liste.canScrollForward) liste.scrollBy(100_000f)
        }
    }

    // Die Seitenleiste selbst liegt in JarvisApp: So reicht sie von ganz oben bis ganz unten, über die Reiter-Leiste.
    Column(Modifier.fillMaxSize()) {
        Kopf(tunnel)
        LaeuftGerade(vm)
        // Die Stoppuhr erscheint nur, wenn Frank sie über Jarvis startet, und bleibt bis zum Zurücksetzen.
        StoppuhrKarte(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        // Rechts unten über der Liste schweben Programmierer, im Programmiergespräch „+ Nachtrag“ und abgesetzt „Neue Session“;
        // die Liste bekommt unten so viel Luft, dass sich die letzte Nachricht darüber schieben lässt.
        val mitte = if (vm.gespraech.isEmpty()) 0.dp else with(dichte) { (listenHoehe / 2).toDp() }
        val vonUnten = vm.gespraech.isNotEmpty()
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { listenHoehe = it.height }) {
        LazyColumn(
            Modifier.fillMaxSize().nestedScroll(nachOben), state = liste,
            // Rechts endet der Text etwa einen Millimeter vor der Spalte der schwebenden Symbole, ein- wie ausgeklappt (der Pfeil steht dort immer).
            contentPadding = PaddingValues(start = 16.dp, end = if (vm.liveAktiv) 16.dp else SCHWEBE_FREI_RECHTS, top = 8.dp, bottom = maxOf(8.dp + schweber, mitte)),
            verticalArrangement = if (vonUnten) Arrangement.spacedBy(10.dp, Alignment.Bottom) else Arrangement.spacedBy(10.dp),
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
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("Tagesauswertung", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val neueste = auswertung.neueste
                            Text(
                                when {
                                    auswertung.laeuft -> auswertung.schritt.ifEmpty { "Wird erstellt" } + " …"
                                    neueste == null -> "Noch keine. Nächste: ${vm.naechsteAuswertung}"
                                    else -> "Stand " + zeitKurz(neueste.zeit) + " Uhr" +
                                        (if (neueste.profil.isNotEmpty()) " · " + de.frank.jarvis.data.Einstellungen.auswertungProfilName(neueste.profil) else "") +
                                        " · nächste ${vm.naechsteAuswertung}"
                                },
                                color = if (auswertung.laeuft) f.primaer else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        // Oben: Vorlesen links neben dem Zahnrad, darunter der Knopf für eine neue Auswertung.
                        Column(Modifier.offset(x = 4.dp, y = (-6).dp), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                auswertung.neueste?.let { VorleseSteuerung(vm, "auswertung", "Tagesauswertung", if (it.mitKi) it.text else it.text + "\n\n" + it.daten) }
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f).antippen { profilWahlOffen = true }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Tune, "Ausführlichkeit der Tagesauswertung", tint = f.textLeise, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Sperre gegen Doppeltippen: „läuft“ springt erst um, wenn der Dienst wirklich angefangen hat; bis dahin gilt „startet“.
                            var startet by remember { mutableStateOf(false) }
                            LaunchedEffect(startet, auswertung.laeuft) { if (startet) { if (auswertung.laeuft) startet = false else { kotlinx.coroutines.delay(15_000); startet = false } } }
                            val wertetAus = auswertung.laeuft || startet
                            Box(
                                Modifier.size(44.dp).glas(f, 22.dp, erhoeht = 0.5f, toenung = if (wertetAus) f.primaer else null).antippen {
                                    if (wertetAus) vm.meldung = "Die Tagesauswertung läuft schon."
                                    else { startet = true; vm.auswertungJetzt() }
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Refresh, if (wertetAus) "Tagesauswertung läuft gerade" else "Tagesauswertung aktualisieren", tint = if (wertetAus) f.primaer else f.textLeise, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    if (profilWahlOffen) AuswertungsProfilWahl(vm) { profilWahlOffen = false }
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
            if (vm.denktSichtbar) item(key = "denkt") {
                Row(Modifier.padding(start = 4.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Kern(30.dp, f.primaer, aktiv = true)
                    Text(vm.schritt.ifEmpty { "Jarvis denkt" } + " …", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
        if (!vm.liveAktiv) ProgrammierSchweber(vm, Modifier.align(Alignment.BottomEnd).padding(end = SCHWEBE_RECHTS, bottom = 2.dp))
        }
        // Die Eingabeblase wie in der ChatGPT-App: Text, Plus-Menü, Modell, Diktat und Live-Gespräch.
        EingabeBlase(vm)
    }
}

@Composable
private fun Kopf(tunnel: TunnelZustand) {
    val f = LocalFarben.current
    val (zeile, farbe) = when (tunnel.stufe) {
        TunnelStufe.ONLINE -> "online" to f.erfolg
        TunnelStufe.VERBINDET -> tunnel.meldung to f.primaer
        TunnelStufe.NICHT_EINGERICHTET -> "Server noch nicht eingerichtet" to f.textLeise
        TunnelStufe.FEHLER -> tunnel.meldung to f.gefahr
        TunnelStufe.AUS -> "Ausgeschaltet" to f.textLeise
    }
    // Oben steht nur noch der Verbindungsstand, damit der Chat höher beginnt. Gespräche öffnen per Wischen von links,
    // Aktualisieren und Hell/Dunkel stehen im Schwebestapel über „Neue Session“, das Design bleibt fest.
    Text(
        zeile, Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
        color = farbe, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
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

/**
 * Auswahl vor einem Programmierauftrag: Modell („Opus 5.5“ oder „Sol 6.1“), bei Sol das CLI („Codex CLI“ oder „OpenCode CLI“),
 * bei Codex das Review („Ja“/„Nein“). Hier steht nur die Frage; die Knöpfe stehen unten neben „Programmierer“, bei eingeklappten Symbolen allein über dem Pfeil (ProgrammierSchweber).
 * Nach einer Wahl, per Sprache oder nach einem Neustart der App steht darunter, was gilt.
 */
@Composable
private fun ModellWahlKarte(n: Nachricht) {
    val f = LocalFarben.current
    val id = remember(n.kontext) { de.frank.jarvis.data.Gespraeche.wahlId(n.kontext) }
    val optionen = remember(n.kontext) { wahlOptionen(n.kontext) }
    val stand by de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.wahlStand.collectAsState()
    val s = stand[id]
    Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp, top = 2.dp, bottom = 6.dp)) {
        Text(n.text, color = f.text, fontSize = 15.sp, lineHeight = 21.sp)
        if (s != de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.WAHL_OFFEN) {
            Text(when (s) {
                de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.WAHL_ERLEDIGT -> "Per Sprache gewählt."
                null -> "Diese Auswahl ist nicht mehr aktiv."
                else -> optionen.firstOrNull { it.first == s }?.let { "Gewählt: ${it.second}" } ?: "Diese Auswahl ist nicht mehr aktiv."
            }, Modifier.padding(top = 4.dp), color = f.textLeise, fontSize = 13.sp)
        }
    }
}

/**
 * Nachrichtentypen für kurze Statusmeldungen, die ein fremder Dienst zustellt (Fortschritt, kurzer Fertig- und Fehlerstand
 * einer Programmier-Sitzung über den Relay). Der ausführliche Abschlussbericht ([ProgrammierChat.ART_BERICHT]) gehört nicht
 * dazu: Er bekommt die volle Aktionsleiste. Rückfragen („rueckfrage“) haben ihre eigene Karte.
 */
private val EXTERNE_ARTEN = setOf(de.frank.jarvis.faehigkeit.ProgrammierChat.ART_STATUS)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Blase(vm: AppViewModel, nummer: Int, n: Nachricht) {
    if (n.art == "rueckfrage") { RueckfrageKarte(vm, n); return }
    if (n.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_MODELLWAHL) { ModellWahlKarte(n); return }
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (n.vonMir) Alignment.End else Alignment.Start) {
        if (n.vonMir && n.anhaenge.isNotEmpty()) FlowRow(Modifier.padding(start = 44.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            n.anhaenge.forEach { AnhangKachel(de.frank.jarvis.data.Anhang(it), 96.dp) }
        }
        if (n.vonMir) {
            // Franks Nachricht: dezente Blase rechts, wie in der ChatGPT-App; die Symbole darunter beginnen an ihrer linken Kante.
            Column(Modifier.padding(start = 48.dp)) {
                Box(Modifier.align(Alignment.End)) {
                    Markierbar(hell = false) {
                        Text(
                            n.text,
                            Modifier.widthIn(max = 460.dp)
                                .background(f.flaecheStark, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                            color = f.text, fontSize = 16.sp, lineHeight = 22.sp,
                        )
                    }
                }
                FrageLeiste(vm, nummer, n)
            }
        } else if (n.art in EXTERNE_ARTEN) {
            // Von außen zugestellte Meldung (z. B. Fortschritt einer Programmier-Sitzung am PC): keine Antwort von Jarvis,
            // daher mit Punkt davor und ohne Aktionsleiste (Wiederholen, Bewerten, Vorlesen ergeben hier keinen Sinn).
            Row(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)) {
                Aufzaehlungspunkt()
                Markierbar(hell = false) { ProgrammierText(n.text, Modifier.weight(1f), farbe = f.text) }
            }
        } else if (n.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_BILD) {
            // Ein Bild, das Jarvis im Gespräch erzeugt hat: groß im Chat, darunter „In Ablage verschieben“ (bzw. wo es schon liegt).
            Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp, top = 2.dp)) {
                Text(n.text, color = f.textLeise, fontSize = 14.sp)
                JarvisDateien(vm, n.anhaenge)
            }
        } else {
            // Jarvis' Antwort: Fließtext über die ganze Breite, ohne Blase, gut gegliedert.
            Markierbar(hell = false) {
                val form = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp, top = 2.dp)
                when (n.art) {
                    // Der Abschlussbericht wie jede Programmiermeldung mit Punkt vorn (Punkt, Symbol, Absender, Inhalt).
                    de.frank.jarvis.faehigkeit.ProgrammierChat.ART_BERICHT -> Row(form) {
                        Aufzaehlungspunkt()
                        ProgrammierText(n.text, Modifier.weight(1f), farbe = f.text)
                    }
                    "fehler", "abgebrochen" -> Text(n.text, form, color = f.gefahr, fontSize = 15.sp, lineHeight = 21.sp)
                    else -> AntwortText(n.text, form)
                }
            }
            // Bilder und PDFs zur Antwort (etwa vom Ergebnis eines Agenten) direkt darunter.
            if (n.anhaenge.isNotEmpty()) Box(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp)) { JarvisDateien(vm, n.anhaenge) }
            AppViewModel.berichtVorgang(n)?.let { BerichtAblageKnopf(vm, nummer, n, it) }
            AntwortLeiste(vm, nummer, n)
        }
    }
}

/**
 * Unter einem fertigen Agenten-Bericht (Recherche, Machbarkeit …): „Recherche in die Ablage kopieren“. Liegt der Bericht schon
 * in der Ablage, steht dort nur, unter welchem Titel; ein Fehler steht offen darunter, der Knopf bleibt für einen neuen Versuch.
 */
@Composable
private fun BerichtAblageKnopf(vm: AppViewModel, nummer: Int, n: Nachricht, vorgang: String) {
    val f = LocalFarben.current
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val stand by remember { de.frank.jarvis.ablage.AblageZentrale.speicher(context).stand }.collectAsState()
    val gemerkt = AppViewModel.berichtAblageTitel(n)
    // Nur, was wirklich in der Ablage liegt: Hat Frank den Eintrag dort gelöscht, lässt sich der Bericht erneut kopieren.
    val liegtDa by produceState<String?>(null, gemerkt, stand) {
        value = gemerkt?.let { t -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { de.frank.jarvis.ablage.AblageZentrale.speicher(context).findeGenau(t)?.titel } }
    }
    val zustand = vm.ablageUmzug["bericht:$vorgang"]
    val name = if (n.text.startsWith("**Recherche")) "Recherche" else "Bericht"
    Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 2.dp, end = 4.dp, top = 8.dp)) {
        val titel = liegtDa
        if (titel != null) Text("✓ In der Ablage: „$titel“", color = f.erfolg, fontSize = 14.sp)
        else Chip(
            if (zustand == AppViewModel.UMZUG_LAEUFT) "Wird gespeichert …" else "$name in die Ablage kopieren",
            aktiv = true, icon = androidx.compose.material.icons.Icons.Rounded.Inventory2,
        ) { vm.berichtInAblage(nummer) }
        zustand?.takeIf { it != AppViewModel.UMZUG_LAEUFT }?.let {
            Text("Nicht gespeichert: $it", color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * Auswahlfenster am Zahnrad der Tagesauswertung: Kurz, Normal, Gründlich (von oben). Ein Tipp speichert die Wahl dauerhaft
 * und schließt das Fenster; sie gilt ab der nächsten Auswertung, die vorhandene bleibt, wie sie ist.
 */
@Composable
private fun AuswertungsProfilWahl(vm: AppViewModel, schliessen: () -> Unit) {
    val f = LocalFarben.current
    val aktuell = vm.einstellungen.auswertungProfil
    androidx.compose.material3.AlertDialog(
        onDismissRequest = schliessen,
        containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Ausführlichkeit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wie ausführlich Jarvis die Tagesauswertung schreibt. Gilt ab der nächsten Auswertung.", color = f.textLeise, fontSize = 14.sp)
                de.frank.jarvis.data.Einstellungen.AUSWERTUNG_PROFILE.forEach { (id, name, beschreibung) ->
                    val gewaehlt = id == aktuell
                    Row(
                        Modifier.fillMaxWidth().glas(f, 14.dp, erhoeht = 0.3f, toenung = if (gewaehlt) f.primaer else null)
                            .antippen { vm.auswertungProfil(id); schliessen() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .semantics { stateDescription = if (gewaehlt) "ausgewählt" else "" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(beschreibung, color = f.textLeise, fontSize = 13.sp, lineHeight = 17.sp)
                        }
                        if (gewaehlt) Icon(Icons.Rounded.Check, null, tint = f.primaer, modifier = Modifier.padding(start = 8.dp).size(20.dp))
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = schliessen) { Text("Schließen") } },
    )
}

/** Der Punkt vor jeder Programmiermeldung (Statusmeldung und Abschlussbericht): Punkt, Symbol, Absender, Inhalt. */
@Composable
private fun Aufzaehlungspunkt() {
    val f = LocalFarben.current
    Text("•", color = if (f.dunkel) f.text else Color.Black, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
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
        // Ausgefüllt nur der tatsächlich gewählte Daumen; neutral sind beide nur umrandet
        // (Rounded.ThumbUpOffAlt ist im Rounded-Satz selbst ausgefüllt, daher Outlined).
        LeistenKnopf(if (n.bewertung > 0) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp, "Gute Antwort", if (n.bewertung > 0) f.primaer else f.textLeise) {
            vm.bewerten(nummer, if (n.bewertung > 0) 0 else 1)
        }
        LeistenKnopf(if (n.bewertung < 0) Icons.Rounded.ThumbDown else Icons.Outlined.ThumbDown, "Schlechte Antwort", if (n.bewertung < 0) f.primaer else f.textLeise) {
            if (n.bewertung < 0) vm.bewerten(nummer, 0) else grundFragen = true
        }
        Vorleseknopf(vm, "gespraech:$nummer", "Jarvis", n.text, groesse = 36.dp, schlicht = true)
        LeistenKnopf(Icons.Rounded.Share, "Teilen", f.textLeise) { teilen(vm, context, n.text, "Antwort teilen") }
        LeistenKnopf(Icons.AutoMirrored.Rounded.CallSplit, "In neuem Chat fortsetzen", f.textLeise) { vm.abzweigen(nummer) }
        LeistenKnopf(Icons.Rounded.Refresh, "Erneut versuchen", f.textLeise) { vm.erneutVersuchen(nummer) }
    }
    // Verbrauch dieser Antwort und aller Antworten des Gesprächs bis hierher.
    if (n.tokens > 0) {
        val gesamt = vm.gespraech.take(nummer + 1).sumOf { it.tokens }
        val zahl = { t: Long -> String.format(java.util.Locale.GERMAN, "%,d", t) }
        Text("${zahl(n.tokens)} Tokens · im Gespräch gesamt ${zahl(gesamt)}", color = f.textLeise, fontSize = 13.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
    }
    if (grundFragen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { grundFragen = false }, containerColor = menueFlaeche(f)) {
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

/**
 * Die Leiste unter Franks eigener Frage: Kopieren, Vorlesen, Teilen und in neuer Session weiterarbeiten,
 * jeweils mit genau dieser Nachricht. Bewerten und Wiederholen gibt es hier nicht.
 */
@Composable
private fun FrageLeiste(vm: AppViewModel, nummer: Int, n: Nachricht) {
    val f = LocalFarben.current
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Kopierknopf(vm, n.text, "Frage")
        Vorleseknopf(vm, "gespraech:$nummer", "Deine Frage", n.text, groesse = 36.dp, schlicht = true)
        LeistenKnopf(Icons.Rounded.Share, "Frage teilen", f.textLeise) { teilen(vm, context, n.text, "Frage teilen") }
        LeistenKnopf(Icons.AutoMirrored.Rounded.CallSplit, "In neuer Session weiterarbeiten", f.textLeise) { vm.mitFrageWeiterarbeiten(nummer) }
    }
}

/** Öffnet Androids Teilen-Dialog mit genau [text]; die Sperre wartet solange, damit Jarvis nicht dazwischen sperrt. */
internal fun teilen(vm: AppViewModel, context: android.content.Context, text: String, titel: String) {
    vm.sperrAufschubBis = System.currentTimeMillis() + 120_000
    runCatching {
        context.startActivity(android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text), titel,
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { vm.meldung = "Das Teilen-Menü ließ sich nicht öffnen." }
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
    if (!a.arbeitetJetzt()) return
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
            val farbe = if (a.lokal) PC_LILA else CLAUDE_ORANGE
            Text(SPINNER[bild], color = farbe, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
            Text("$wort…", color = farbe, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            if (a.start > 0) Text(" · " + laufzeit((jetzt - a.start) / 1_000), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "(${a.app}${if (a.lokal) " · gestartet auf dem PC" else ""} · letzte Aktivität vor $seit)",
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
/** Am PC direkt gestartete Sitzungen (nicht über Jarvis): dasselbe Arbeitssymbol in Lila statt Orange. */
internal val PC_LILA = Color(0xFF8B5CF6)
/** Die Bilder des Claude-Code-Arbeitssymbols, alle 120 ms eins weiter; auch die Seitenleiste zeigt es an arbeitenden Programmier-Gesprächen. */
internal val SPINNER = listOf("·", "✢", "✳", "✶", "✻", "✽", "✻", "✶", "✳", "✢")
private val SPINNER_WOERTER = listOf(
    "Drizzling", "Cooking", "Brewing", "Baking", "Clauding", "Cogitating", "Percolating", "Simmering", "Marinating", "Noodling",
    "Pondering", "Ruminating", "Conjuring", "Crafting", "Forging", "Hatching", "Mulling", "Musing", "Stewing", "Synthesizing",
    "Transmuting", "Vibing", "Puttering", "Schlepping", "Reticulating", "Finagling", "Moseying", "Churning", "Crunching", "Computing",
)

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
                    Text(l.schritt.ifEmpty { l.auftrag }, color = f.textLeise, fontSize = 13.sp, maxLines = if (l.wartet != null) 6 else 2, overflow = TextOverflow.Ellipsis)
                }
            }
            // Zeitlimit erreicht: Der Agent pausiert mit seinem Arbeitsstand, bis Frank entscheidet.
            l.wartet?.let { kennung ->
                Text(de.frank.jarvis.agent.Zeitablauf.frage(l.schritt), color = f.text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Knopf("Weiterarbeiten", Modifier.weight(1f)) {
                        vm.meldung = if (de.frank.jarvis.agent.Agenten.entscheide(kennung, true)) "Agent ${l.agent} arbeitet ${de.frank.jarvis.agent.Agenten.ZEITLIMIT_MINUTEN} Minuten weiter" else "Der Agent wartet nicht mehr"
                    }
                    Knopf("Abbrechen", Modifier.weight(1f), haupt = false) {
                        vm.meldung = if (de.frank.jarvis.agent.Agenten.entscheide(kennung, false)) "Agent ${l.agent} schreibt seinen Zwischenstand" else "Der Agent wartet nicht mehr"
                    }
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
                Knopf("Aus", Modifier.weight(1f), farbe = f.gefahr) { de.frank.jarvis.timer.Timer.klingelnAus(kontext, t.id, erledigt = true) }
                Knopf("Schlummern $schlummern min", Modifier.weight(1f), haupt = false) { de.frank.jarvis.timer.Timer.schlummern(kontext, t.id) }
            }
        }
    }
}
