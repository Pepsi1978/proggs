package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.ablage.Freigabe
import de.frank.jarvis.ablage.Weitergabe
import de.frank.jarvis.gedaechtnis.Bereich
import de.frank.jarvis.gedaechtnis.Gespraechsgedaechtnis
import de.frank.jarvis.gedaechtnis.Gespraechsnotiz
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.gedaechtnis.Langzeitkontext
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ob der Bildschirm des Live-Gedächtnisses offen ist (Vollbild über den Reitern, wie die Merklisten). */
object GedaechtnisAnsicht {
    var offen by mutableStateOf(false)
}

/**
 * Das Live-Gedächtnis zum Ansehen: was Jarvis bei jeder Frage vor Augen hat, je Bereich mit Stand und Anlass der
 * letzten Änderung, die Neuigkeiten der letzten 24 Stunden, was Jarvis sich aus Gesprächen gemerkt hat, und das
 * Logbuch zum Herunterladen.
 */
@Composable
fun GedaechtnisBildschirm(modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val app = context.applicationContext
    val bereich = rememberCoroutineScope()
    BackHandler { GedaechtnisAnsicht.offen = false }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Lagebild.lade(app) } }
    val stand by Lagebild.stand.collectAsState()
    // Die Altersangaben („vor 3 Minuten“) laufen mit, ohne dass sich der Stand ändert.
    var takt by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); takt++ } }
    var offenerBereich by remember { mutableStateOf<Bereich?>(null) }
    var notizen by remember { mutableStateOf(emptyList<Gespraechsnotiz>()) }
    var logbuch by remember { mutableStateOf("") }
    var meldung by remember { mutableStateOf("") }
    // Die Notizen nur, wenn sich die Gespräche inhaltlich geändert haben (nicht bei jeder Lesung ohne Änderung).
    LaunchedEffect(stand.abschnitte[Bereich.GESPRAECHE]?.geaendert) {
        notizen = withContext(Dispatchers.IO) { Gespraechsgedaechtnis.notizen(app) }
    }
    // Die Logbuch-Zusammenfassung liest die ganze Tagesdatei: einmal beim Öffnen, danach alle fünf Minuten.
    LaunchedEffect(Unit) {
        while (true) {
            logbuch = withContext(Dispatchers.IO) { runCatching { Logbuch.auszug(tage = 1, hoechstens = 0).lines().take(4).joinToString("\n") }.getOrDefault("") }
            delay(300_000L)
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).antippen { GedaechtnisAnsicht.offen = false }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück", tint = f.text, modifier = Modifier.size(24.dp))
            }
            Text("Live-Gedächtnis", Modifier.weight(1f).padding(start = 2.dp), color = f.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (stand.laeuft.isNotEmpty()) Kern(34.dp, f.primaer, aktiv = true)
            else Rundknopf(Icons.Rounded.Refresh, "Alles neu lesen", farbe = f.primaer) { Lagebild.anstossen(app, Bereich.entries, "von Hand aufgefrischt") }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "kopf") {
                Text(
                    "Das weiß Jarvis bei jeder Frage, im eigenen Chat, bei „Hey Jarvis“ und in ChatGPT. Ändert sich etwas in einer deiner Apps, steht es nach wenigen Sekunden hier. " +
                        "Die letzten 10 Tage sind vollständig darin, Älteres verdichtet; gezielt sucht Jarvis im ganzen Gedächtnis.",
                    color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item(key = "ueberblick") {
                takt.let { }
                Karte(toenung = f.primaer) {
                    val fehler = stand.abschnitte.count { it.value.fehler.isNotEmpty() }
                    Text("${stand.abschnitte.size} von ${Bereich.entries.size} Bereichen bereit", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Etwa ${tausend(stand.zeichen)} Zeichen (rund ${tausend(stand.zeichen / 4)} Token)" +
                            (Lagebild.letzteAenderung.takeIf { it > 0 }?.let { " · zuletzt geändert ${alter(it)}" } ?: "") +
                            (if (fehler > 0) " · $fehler gerade nicht lesbar" else ""),
                        color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                    if (meldung.isNotEmpty()) Text(meldung, color = f.primaer, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            item(key = "langzeitkontext") { LangzeitkontextKarte() }

            item(key = "funktionen") {
                Karte(Modifier.antippen { FunktionenAnsicht.offen = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Funktionsübersicht", color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Was Jarvis kann, nur bei Bedarf gelesen, nicht Teil des Live-Gedächtnisses. Ansehen und bearbeiten.", color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text("Öffnen", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            val neu = stand.neuigkeiten.filter { System.currentTimeMillis() - it.zeit < 24 * 3_600_000L }.take(15)
            if (neu.isNotEmpty()) {
                item(key = "neu-titel") { Abschnitt("Neu in den letzten 24 Stunden") }
                item(key = "neu") {
                    Karte {
                        neu.forEachIndexed { i, n ->
                            Text(n.text, color = f.text, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = if (i == 0) 0.dp else 8.dp))
                            Text(alter(n.zeit) + " · " + n.anlass, color = f.textSchwach, fontSize = 12.sp)
                        }
                    }
                }
            }

            item(key = "bereiche-titel") { Abschnitt("Bereiche") }
            items(Bereich.entries.toList(), key = { "b-" + it.kennung }) { b ->
                val a = stand.abschnitte[b]
                val auf = offenerBereich == b
                Karte(Modifier.animateContentSize().antippen { offenerBereich = if (auf) null else b }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.titel, Modifier.weight(1f), color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        if (b in stand.laeuft) Kern(22.dp, f.primaer, aktiv = true)
                    }
                    takt.let { }
                    Text(
                        if (a == null) "Noch nicht gelesen" else "Gelesen ${alter(a.stand)}" +
                            (if (a.geaendert > 0 && a.geaendert < a.stand) " · geändert ${alter(a.geaendert)}" else "") +
                            (if (a.anlass.isNotEmpty()) " · ${a.anlass}" else "") + " · ${tausend(a.text.length)} Zeichen",
                        color = f.textSchwach, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                    if (a != null && a.fehler.isNotEmpty()) Text("Gerade nicht lesbar: ${a.fehler}", color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    if (auf && a != null) {
                        Text(
                            a.text.let { if (it.length > 4_000) it.take(4_000) + "\n…" else it },
                            color = f.textLeise, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp),
                        )
                        Knopf("Diesen Bereich neu lesen", Modifier.padding(top = 8.dp).fillMaxWidth(), Icons.Rounded.Refresh, haupt = false) { Lagebild.anstossen(app, listOf(b), "von Hand aufgefrischt") }
                    }
                }
            }

            item(key = "notizen-titel") { Abschnitt("Aus Gesprächen gemerkt") }
            item(key = "notizen") {
                Karte {
                    if (notizen.isEmpty()) Text("Noch keine kurzlebigen Notizen. Jarvis merkt sich hier Vorhaben, Zusagen und offene Themen aus euren Gesprächen; dauerhafte Dinge landen bei Regeln, Zielen und dem Wissen über dich.", color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp)
                    notizen.forEach { n ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(n.text, color = f.text, fontSize = 15.sp, lineHeight = 21.sp)
                                Text("notiert ${alter(n.erstellt)}" + (n.bis?.let { " · gilt bis $it" } ?: ""), color = f.textSchwach, fontSize = 12.sp)
                            }
                            Box(Modifier.size(40.dp).antippen {
                                bereich.launch {
                                    withContext(Dispatchers.IO) { Gespraechsgedaechtnis.loescheNotiz(app, n.id) }
                                    notizen = withContext(Dispatchers.IO) { Gespraechsgedaechtnis.notizen(app) }
                                    Lagebild.anstossen(app, listOf(Bereich.GESPRAECHE), "Notiz gelöscht")
                                }
                            }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Delete, "Notiz löschen", tint = f.textLeise, modifier = Modifier.size(20.dp)) }
                        }
                    }
                    LernlaufBereich { bereich.launch { notizen = withContext(Dispatchers.IO) { Gespraechsgedaechtnis.notizen(app) } } }
                }
            }

            item(key = "logbuch-titel") { Abschnitt("Logbuch") }
            item(key = "logbuch") {
                Karte {
                    Text(
                        "Jarvis schreibt alles mit, was er tut: Aktionen, Fehler, Abstürze, Dauer von Antworten und Abläufen. Damit lässt sich die App gezielt verbessern. " +
                            "Am PC holt `Jarvis/werkzeuge/logbuch-holen.ps1` es übers WLAN ab; Jarvis selbst liest es mit logbuch_lesen.",
                        color = f.textLeise, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                    if (logbuch.isNotEmpty()) Text(logbuch, color = f.text, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    Knopf("Logbuch in „Download/Jarvis“ speichern", Modifier.padding(top = 10.dp).fillMaxWidth(), Icons.Rounded.Download, haupt = false) {
                        bereich.launch {
                            meldung = withContext(Dispatchers.IO) {
                                runCatching {
                                    val datei = Logbuch.export(app)
                                    "Gespeichert als Download/Jarvis/" + Weitergabe.herunterladen(app, Freigabe(datei, datei.name, "application/json"))
                                }.getOrElse { "Speichern ging nicht: ${it.message ?: it.javaClass.simpleName}" }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tausend(n: Int): String = "%,d".format(java.util.Locale.GERMAN, n)

private fun alter(millis: Long): String {
    val minuten = (System.currentTimeMillis() - millis) / 60_000L
    return when {
        minuten < 1 -> "gerade eben"
        minuten < 60 -> "vor $minuten Min."
        minuten < 24 * 60 -> "vor ${minuten / 60} Std."
        else -> "vor ${minuten / (24 * 60)} Tagen"
    }
}

/**
 * Franks Langzeitkontext zum Bearbeiten, eingeklappt, damit die Liste klein bleibt. Dieselbe Datei bearbeitet er auch am Mac:
 * Solange er hier nichts ändert, kommt ein neuerer Stand von selbst an.
 */
@Composable
private fun LangzeitkontextKarte() {
    val f = LocalFarben.current
    val app = LocalContext.current.applicationContext
    val bereich = rememberCoroutineScope()
    var offen by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var gelesen by remember { mutableStateOf("") }
    var stand by remember { mutableStateOf(0L) }
    var meldung by remember { mutableStateOf("") }
    LaunchedEffect(offen) {
        while (offen) {
            val neu = withContext(Dispatchers.IO) { Langzeitkontext.stand(app) to Langzeitkontext.text(app) }
            if (neu.first != stand && text == gelesen) { stand = neu.first; text = neu.second; gelesen = neu.second }
            delay(5_000)
        }
    }
    Karte(toenung = f.primaer) {
        Row(Modifier.fillMaxWidth().antippen { offen = !offen }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Langzeitkontext", color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Deine Textdatei für Jarvis, auf Handy und Mac gleich. Füg zum Beispiel ein, was ChatGPT sich über dich gemerkt hat.", color = f.textLeise, fontSize = 13.sp)
            }
            Text(if (offen) "Zuklappen" else "Öffnen", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (offen) {
            OutlinedTextField(
                value = text, onValueChange = { text = it; meldung = "" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 420.dp).padding(top = 10.dp),
                textStyle = TextStyle(color = f.text, fontSize = 15.sp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${text.length} Zeichen", Modifier.weight(1f), color = f.textLeise, fontSize = 13.sp)
                if (text != gelesen) Knopf("Speichern") {
                    val neu = text
                    bereich.launch {
                        val aktuell = withContext(Dispatchers.IO) { Langzeitkontext.stand(app) }
                        if (aktuell != stand) { meldung = "Nicht gespeichert: Der Text wurde inzwischen am Mac geändert. Sichere deine Fassung und öffne die Karte neu."; return@launch }
                        stand = withContext(Dispatchers.IO) { Langzeitkontext.ersetze(app, neu).also { Lagebild.anstossen(app, listOf(Bereich.GEMERKT), "Langzeitkontext geändert") } }
                        gelesen = neu
                        meldung = "Gespeichert, auf Handy und Mac gleich."
                    }
                }
            }
            if (meldung.isNotEmpty()) Text(meldung, color = f.primaer, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
