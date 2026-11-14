package de.frank.jarvis.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.R
import de.frank.jarvis.staubsauger.ProscenicCloud
import de.frank.jarvis.staubsauger.ProscenicMehr
import de.frank.jarvis.staubsauger.ProscenicMehr.Zone
import de.frank.jarvis.staubsauger.ProscenicMehr.Zeitplan
import de.frank.jarvis.staubsauger.ProscenicSteuerung
import de.frank.jarvis.staubsauger.ProscenicSteuerung.Aktion
import de.frank.jarvis.staubsauger.SaugerAusgang
import de.frank.jarvis.staubsauger.SaugerStand
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val saugerBereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/** Zustand der aufklappbaren Staubsauger-Karte, über Neuaufbauten des Startbildschirms hinweg. */
private object SaugerAnsicht {
    var offen by mutableStateOf(false)
    var live by mutableStateOf<ProscenicMehr.Live?>(null)
    var erreichbar by mutableStateOf<Boolean?>(null)
    var geprueft by mutableStateOf(false)
    var laedt by mutableStateOf(false)
    var meldung by mutableStateOf<SaugerAusgang?>(null)
}

private fun stufeName(s: String?) = when (s) { "quiet" -> "Leise"; "auto" -> "Normal"; "strong" -> "Leistungsstark"; null, "" -> "–"; else -> s }
private val TAGE = listOf(1 to "Mo", 2 to "Di", 3 to "Mi", 4 to "Do", 5 to "Fr", 6 to "Sa", 0 to "So")

/**
 * Startbereich „Staubsauger“ (Proscenic M7 Pro). Zugeklappt: Akku, Erreichbarkeit (nach Aktualisieren grün oder rot),
 * Tiefenreinigung und Aktualisieren. Aufgeklappt: die ganze Wohnungskarte mit Fahrweg, Roboter, Station und Sperrzonen,
 * darunter alle Befehle und Einstellungen. Alles läuft unsichtbar über die Proscenic-Cloud (Konten, Proscenic).
 */
@Composable
fun StaubsaugerStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val cloud = remember(context) { ProscenicCloud.konfiguriert(context) }
    val installiert = remember(context) { context.packageManager.getLaunchIntentForPackage(ProscenicSteuerung.PAKET) != null }
    if (!cloud && !installiert) return
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { SaugerStand.laden(context) } }
    val s = SaugerStand
    val a = SaugerAnsicht
    val live = a.live

    fun los(aktion: Aktion) {
        if (s.laeuft) return
        s.laeuft = true
        saugerBereich.launch {
            ProscenicSteuerung.ausfuehren(context, aktion)
            if (a.offen && cloud) aktualisieren(context, mitKarte = true)
        }
    }
    fun einstellen(block: suspend () -> SaugerAusgang) {
        if (a.laedt) return
        a.laedt = true
        saugerBereich.launch {
            a.meldung = block()
            a.laedt = false
            aktualisieren(context, mitKarte = a.offen)
        }
    }

    Karte(modifier = griff) {
        // Kopf: Titel, Akku, Erreichbarkeit, Tiefenreinigung, Aktualisieren, Auf-/Zuklappen.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().antippen {
            a.offen = !a.offen
            if (a.offen && cloud && (a.live?.karte == null)) saugerBereich.launch { aktualisieren(context, mitKarte = true) }
        }) {
            Column(Modifier.weight(1f)) {
                Text("Staubsauger", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.sauger_laden), null, Modifier.size(18.dp))
                    Text(
                        (live?.akku ?: s.akku)?.let { "$it %" } ?: "–",
                        color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, end = 10.dp),
                    )
                    when {
                        a.laedt || s.laeuft -> Text("wird gelesen …", color = f.primaer, fontSize = 13.sp)
                        a.geprueft && a.erreichbar == true -> Text("erreichbar", color = f.erfolg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        a.geprueft && a.erreichbar == false -> Text("nicht erreichbar", color = f.gefahr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        a.geprueft -> Text("Cloud nicht erreichbar", color = f.gefahr, fontSize = 13.sp)
                        s.gelesenUm != null -> Text("Stand ${alterKurz(s.gelesenUm!!)}", color = f.textLeise, fontSize = 13.sp)
                    }
                }
            }
            KopfKnopf(R.drawable.sauger_tiefenreinigung, "Tiefenreinigung", s.laeuft) { los(Aktion.TIEFENREINIGUNG) }
            Box(
                Modifier.padding(start = 6.dp).size(42.dp).glas(f, 21.dp, erhoeht = 0.5f, toenung = if (a.laedt) f.primaer else null)
                    .antippen { if (cloud) saugerBereich.launch { aktualisieren(context, mitKarte = a.offen) } else los(Aktion.STATUS) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Refresh, "Aktualisieren", tint = if (a.laedt) f.primaer else f.textLeise, modifier = Modifier.size(20.dp)) }
            Icon(
                if (a.offen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, if (a.offen) "Zuklappen" else "Aufklappen",
                tint = f.textLeise, modifier = Modifier.padding(start = 4.dp).size(26.dp),
            )
        }
        val meldung = a.meldung ?: s.meldung?.takeIf { !s.laeuft }
        meldung?.let { Text(it.text, color = if (it.fehler) f.gefahr else f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }

        if (a.offen) Aufgeklappt(cloud, ::los, ::einstellen)
    }
}

/** Liest Erreichbarkeit und Stand neu (mit Karte, wenn aufgeklappt). */
private suspend fun aktualisieren(context: android.content.Context, mitKarte: Boolean) {
    val a = SaugerAnsicht
    if (a.laedt) return
    a.laedt = true
    try {
        coroutineScopeAsync(context, mitKarte)
    } finally { a.laedt = false }
}

private suspend fun coroutineScopeAsync(context: android.content.Context, mitKarte: Boolean) = kotlinx.coroutines.coroutineScope {
    val a = SaugerAnsicht
    val online = async { ProscenicMehr.erreichbar(context) }
    val neu = async { ProscenicMehr.live(context, mitKarte) }
    a.erreichbar = online.await(); a.geprueft = true
    neu.await()?.let { l ->
        // Karte nur ersetzen, wenn eine neue kam (Statusabfrage ohne Karte behält die alte).
        a.live = if (l.karte == null && a.live?.karte != null) a.live!!.copy(modus = l.modus, akku = l.akku, saugstufe = l.saugstufe, lautstaerke = l.lautstaerke, led = l.led, staubFrequenz = l.staubFrequenz, roboter = l.roboter ?: a.live!!.roboter) else l
        withContext(Dispatchers.IO) { SaugerStand.merken(context, l.akku, l.modus?.let(::zustandName), stufeName(l.saugstufe)) }
    }
}

private fun zustandName(m: String) = when (m) {
    "charge" -> "lädt in der Station"; "fullcharge" -> "voll geladen in der Station"; "sweep" -> "reinigt"; "pause" -> "pausiert"
    "backcharge" -> "fährt zur Ladestation"; "findchargerpause" -> "Rückfahrt pausiert"; "DustCenterWorking" -> "Staubsammlung läuft"
    "idle" -> "bereit"; "fault" -> "Störung"; else -> m
}

@Composable
private fun Aufgeklappt(cloud: Boolean, los: (Aktion) -> Unit, einstellen: (suspend () -> SaugerAusgang) -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    val s = SaugerStand
    val a = SaugerAnsicht
    val live = a.live
    var zonenModus by remember { mutableStateOf(false) }
    val entwurf = remember { mutableStateListOf<Zone>() }
    var dialog by remember { mutableStateOf<String?>(null) }

    live?.modus?.let { Text("Zustand: ${zustandName(it)} · Saugstufe: ${stufeName(live.saugstufe)}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }

    // Karte der Wohnung.
    val karte = live?.karte
    if (karte != null) {
        KartenAnsicht(live, if (zonenModus) entwurf else live.zonen, zonenModus,
            beiZone = { i -> if (zonenModus && entwurf[i].sperre) entwurf.removeAt(i) },
            beiNeu = { x1, y1, x2, y2 ->
                val id = (entwurf.maxOfOrNull { it.json.optInt("id") } ?: 1000) + 1
                val nr = entwurf.count { it.sperre } + 1
                val j = JSONObject().put("active", "forbid").put("id", id).put("mode", "default").put("name", "扫地机禁区").put("tag", nr.toString())
                    .put("forbidType", "all")
                    .put("vertexs", JSONArray().put(JSONArray().put(minOf(x1, x2)).put(maxOf(y1, y2))).put(JSONArray().put(maxOf(x1, x2)).put(maxOf(y1, y2)))
                        .put(JSONArray().put(maxOf(x1, x2)).put(minOf(y1, y2))).put(JSONArray().put(minOf(x1, x2)).put(minOf(y1, y2))))
                entwurf.add(Zone(j))
            })
    } else {
        Text(if (a.laedt) "Karte wird geladen …" else if (cloud) "Keine Karte – tippe auf Aktualisieren." else "Die Karte gibt es nur mit der Proscenic-Anmeldung unter Konten.",
            color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
    }
    if (zonenModus) {
        Text("Ziehe auf der Karte ein Rechteck für eine neue Sperrzone, tippe eine rote Zone an, um sie zu löschen.", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Knopf("Speichern", Modifier.weight(1f)) {
                val mapId = live?.mapId ?: return@Knopf
                val liste = entwurf.toList(); zonenModus = false
                einstellen { ProscenicMehr.zonenSpeichern(context, mapId, liste) }
            }
            Knopf("Abbrechen", Modifier.weight(1f)) { zonenModus = false }
        }
    }

    // Befehle wie bisher, dazu Pause/Fortsetzen.
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SaugerKachel("Reinigen", R.drawable.sauger_reinigen, s.laeuft, Modifier.weight(1f)) { los(Aktion.REINIGEN) }
        SaugerKachel("Tiefenreinigung", R.drawable.sauger_tiefenreinigung, s.laeuft, Modifier.weight(1f)) { los(Aktion.TIEFENREINIGUNG) }
        SaugerKachel("Y-förmiges Wischen", R.drawable.sauger_wischen, s.laeuft, Modifier.weight(1f)) { los(Aktion.WISCHEN) }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SaugerKachel("Gerät positionieren", R.drawable.sauger_positionieren, s.laeuft, Modifier.weight(1f)) { los(Aktion.POSITIONIEREN) }
        SaugerKachel("Staubsammlung", R.drawable.sauger_staubsammlung, s.laeuft, Modifier.weight(1f)) { los(Aktion.STAUBSAMMLUNG) }
        SaugerKachel("Zur Station", R.drawable.sauger_laden, s.laeuft, Modifier.weight(1f)) { los(Aktion.STATION) }
    }
    if (!cloud) return
    val pausiert = live?.modus == "pause" || live?.modus == "findchargerpause"
    Knopf(if (pausiert) "Fortsetzen" else "Pause", Modifier.fillMaxWidth().padding(top = 8.dp)) {
        einstellen { ProscenicMehr.pauseOderWeiter(context, weiter = pausiert) }
    }

    // Saugstufe.
    Text("Saugstufe", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("quiet", "auto", "strong").forEach { st ->
            Knopf(stufeName(st), Modifier.weight(1f), aktiv = live?.saugstufe == st) { einstellen { ProscenicMehr.saugstufe(context, st) } }
        }
    }
    // Lautstärke, Leuchte.
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Knopf("Lautstärke ${live?.lautstaerke?.let { "${it * 10} %" } ?: ""}".trim(), Modifier.weight(1f)) { dialog = "lautstaerke" }
        Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Leuchte (LED)", color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(checked = live?.led == true, onCheckedChange = { an -> einstellen { ProscenicMehr.led(context, an) } })
        }
    }
    // Automatische Staubsammlung.
    Text("Automatische Staubsammlung nach dem Andocken", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to "Aus", 1 to "Jedes Mal", 2 to "Jedes 2.", 3 to "Jedes 3.").forEach { (w, t) ->
            Knopf(t, Modifier.weight(1f), aktiv = live?.staubFrequenz == w) { einstellen { ProscenicMehr.staubFrequenz(context, w) } }
        }
    }
    // Sperrzonen, Zeitpläne, Verlauf.
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Knopf("Sperrzonen", Modifier.weight(1f), aktiv = zonenModus) {
            if (live?.karte == null) return@Knopf
            entwurf.clear(); entwurf.addAll(live.zonen); zonenModus = true
        }
        Knopf("Zeitpläne", Modifier.weight(1f)) { dialog = "zeitplaene" }
        Knopf("Verlauf", Modifier.weight(1f)) { dialog = "verlauf" }
    }

    when (dialog) {
        "lautstaerke" -> LautstaerkeDialog(live?.lautstaerke ?: 5, { dialog = null }) { w -> dialog = null; einstellen { ProscenicMehr.lautstaerke(context, w) } }
        "zeitplaene" -> ZeitplanDialog(live, { dialog = null }) { liste -> dialog = null; einstellen { ProscenicMehr.zeitplaeneSpeichern(context, live?.zeitZone ?: 1, live?.zeitZoneSek ?: 3600, liste) } }
        "verlauf" -> VerlaufDialog { dialog = null }
    }
}

/** Wohnungskarte mit Fahrweg (blau), Roboter, Station (grün), Räumen und Sperrzonen (rot). */
@Composable
private fun KartenAnsicht(
    live: ProscenicMehr.Live, zonen: List<Zone>, bearbeiten: Boolean,
    beiZone: (Int) -> Unit, beiNeu: (Int, Int, Int, Int) -> Unit,
) {
    val f = LocalFarben.current
    val k = live.karte ?: return
    val bild = remember(k) { k.bild.asImageBitmap() }
    var ziehStart by remember { mutableStateOf<Offset?>(null) }
    var ziehEnde by remember { mutableStateOf<Offset?>(null) }
    val maxHoehe = 520.dp
    Box(Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(max = maxHoehe), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(k.breite.toFloat() / k.hoehe, matchHeightConstraintsFirst = true)
                .glas(f, 12.dp)
                .pointerInput(bearbeiten, zonen.size) {
                    if (!bearbeiten) return@pointerInput
                    detectTapGestures { p ->
                        val sk = size.width.toFloat() / k.breite
                        val mx = k.mmX(p.x / sk); val my = k.mmY(p.y / sk)
                        val i = zonen.indexOfFirst { z -> z.sperre && z.ecken.let { e -> e.isNotEmpty() && mx in e.minOf { it.first }..e.maxOf { it.first } && my in e.minOf { it.second }..e.maxOf { it.second } } }
                        if (i >= 0) beiZone(i)
                    }
                }
                .pointerInput(bearbeiten) {
                    if (!bearbeiten) return@pointerInput
                    detectDragGestures(
                        onDragStart = { ziehStart = it; ziehEnde = it },
                        onDrag = { c, d -> c.consume(); ziehEnde = (ziehEnde ?: Offset.Zero) + d },
                        onDragEnd = {
                            val a = ziehStart; val b = ziehEnde
                            if (a != null && b != null) {
                                val sk = size.width.toFloat() / k.breite
                                beiNeu(k.mmX(a.x / sk), k.mmY(a.y / sk), k.mmX(b.x / sk), k.mmY(b.y / sk))
                            }
                            ziehStart = null; ziehEnde = null
                        },
                        onDragCancel = { ziehStart = null; ziehEnde = null },
                    )
                },
        ) {
            val sk = size.width / k.breite
            drawImage(bild, IntOffset.Zero, IntSize(k.breite, k.hoehe), IntOffset.Zero, IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.None)
            // Räume und Sperrzonen.
            zonen.forEach { z ->
                val e = z.ecken; if (e.isEmpty()) return@forEach
                val x1 = k.px(e.minOf { it.first }) * sk; val x2 = k.px(e.maxOf { it.first }) * sk
                val y1 = k.py(e.maxOf { it.second }) * sk; val y2 = k.py(e.minOf { it.second }) * sk
                if (z.sperre) {
                    drawRect(Color(0x44E53935), Offset(x1, y1), Size(x2 - x1, y2 - y1))
                    drawRect(Color(0xFFE53935), Offset(x1, y1), Size(x2 - x1, y2 - y1), style = Stroke(2.dp.toPx()))
                } else drawRect(Color(0x33607D8B), Offset(x1, y1), Size(x2 - x1, y2 - y1), style = Stroke(1.dp.toPx()))
            }
            // Fahrweg.
            if (live.pfad.size > 1) {
                val p = Path()
                live.pfad.forEachIndexed { i, (x, y) -> val px = k.px(x) * sk; val py = k.py(y) * sk; if (i == 0) p.moveTo(px, py) else p.lineTo(px, py) }
                drawPath(p, Color(0xCC1E88E5), style = Stroke(1.6.dp.toPx()))
            }
            live.station?.let { (x, y) -> drawCircle(Color(0xFF43A047), 6.dp.toPx(), Offset(k.px(x) * sk, k.py(y) * sk)) }
            live.roboter?.let { (x, y) ->
                drawCircle(Color.White, 8.dp.toPx(), Offset(k.px(x) * sk, k.py(y) * sk))
                drawCircle(Color(0xFF1565C0), 6.dp.toPx(), Offset(k.px(x) * sk, k.py(y) * sk))
            }
            val a = ziehStart; val b = ziehEnde
            if (a != null && b != null) {
                drawRect(Color(0x44E53935), Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y)))
            }
        }
    }
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Legende(Color(0xFF1565C0), "Roboter"); Legende(Color(0xFF43A047), "Station"); Legende(Color(0xFF1E88E5), "Fahrweg"); Legende(Color(0xFFE53935), "Sperrzone")
    }
}

@Composable
private fun Legende(farbe: Color, text: String) {
    val f = LocalFarben.current
    Canvas(Modifier.size(10.dp)) { drawCircle(farbe) }
    Text(text, color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 10.dp))
}

@Composable
private fun LautstaerkeDialog(start: Int, schliessen: () -> Unit, setzen: (Int) -> Unit) {
    var wert by remember { mutableStateOf(start.toFloat()) }
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Lautstärke") },
        text = {
            Column {
                Text("${(wert.toInt()) * 10} %")
                Slider(value = wert, onValueChange = { wert = it }, valueRange = 0f..10f, steps = 9)
            }
        },
        confirmButton = { TextButton({ setzen(wert.toInt()) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(schliessen) { Text("Abbrechen") } },
    )
}

@Composable
private fun ZeitplanDialog(live: ProscenicMehr.Live?, schliessen: () -> Unit, speichern: (List<Zeitplan>) -> Unit) {
    val f = LocalFarben.current
    val liste = remember { mutableStateListOf<Zeitplan>().apply { addAll(live?.zeitplaene.orEmpty()) } }
    var neu by remember { mutableStateOf(false) }
    var stunde by remember { mutableStateOf(10) }
    var minute by remember { mutableStateOf(0) }
    val tage = remember { mutableStateListOf(1, 2, 3, 4, 5) }
    var stufe by remember { mutableStateOf("auto") }
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Zeitpläne") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (live == null) Text("Erst aktualisieren, dann sind die Zeitpläne da.", color = f.textLeise)
                liste.forEachIndexed { i, z ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("%02d:%02d Uhr".format(z.startSek / 3600, z.startSek % 3600 / 60), color = f.text, fontWeight = FontWeight.SemiBold)
                            Text(TAGE.filter { it.first in z.tage }.joinToString(" ") { it.second }.ifBlank { "einmalig" } + " · " + stufeName(z.stufe), color = f.textLeise, fontSize = 13.sp)
                        }
                        Switch(checked = z.aktiv, onCheckedChange = { liste[i] = z.copy(aktiv = it) })
                        Icon(Icons.Rounded.Delete, "Löschen", tint = f.gefahr, modifier = Modifier.padding(start = 6.dp).size(24.dp).antippen { liste.removeAt(i) })
                    }
                }
                if (!neu) {
                    if (live != null) Knopf("Neuer Zeitplan", Modifier.fillMaxWidth().padding(top = 8.dp)) { neu = true }
                } else {
                    Text("Neuer Zeitplan", color = f.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Knopf("−", Modifier.size(40.dp)) { stunde = (stunde + 23) % 24 }
                        Text("%02d".format(stunde), color = f.text, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        Knopf("+", Modifier.size(40.dp)) { stunde = (stunde + 1) % 24 }
                        Text(":", color = f.text, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
                        Knopf("−", Modifier.size(40.dp)) { minute = (minute + 55) % 60 }
                        Text("%02d".format(minute), color = f.text, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        Knopf("+", Modifier.size(40.dp)) { minute = (minute + 5) % 60 }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TAGE.forEach { (n, t) -> Knopf(t, Modifier.weight(1f), aktiv = n in tage) { if (n in tage) tage.remove(n) else tage.add(n) } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("quiet", "auto", "strong").forEach { st -> Knopf(stufeName(st), Modifier.weight(1f), aktiv = stufe == st) { stufe = st } }
                    }
                    Knopf("Hinzufügen", Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        liste.add(Zeitplan(true, stunde * 3600 + minute * 60, tage.sorted(), stufe, JSONObject())); neu = false
                    }
                }
            }
        },
        confirmButton = { if (live != null) TextButton({ speichern(liste.toList()) }) { Text("Speichern") } },
        dismissButton = { TextButton(schliessen) { Text("Abbrechen") } },
    )
}

@Composable
private fun VerlaufDialog(schliessen: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    var verlauf by remember { mutableStateOf<ProscenicMehr.Verlauf?>(null) }
    var fertig by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { verlauf = ProscenicMehr.verlauf(context); fertig = true }
    val format = remember { SimpleDateFormat("EE dd.MM. HH:mm", Locale.GERMANY) }
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Reinigungsverlauf") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val v = verlauf
                when {
                    !fertig -> Text("Wird geladen …", color = f.textLeise)
                    v == null -> Text("Der Verlauf ließ sich gerade nicht laden.", color = f.gefahr)
                    else -> {
                        Text(
                            listOfNotNull(v.anzahl?.let { "$it Reinigungen" }, v.gesamtFlaeche?.let { "$it m² gesamt" }, v.gesamtMin?.let { "${it / 60} h ${it % 60} min gesamt" }).joinToString(" · "),
                            color = f.text, fontWeight = FontWeight.SemiBold,
                        )
                        v.laeufe.forEach { l ->
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                Text(format.format(Date(l.start * 1000)), color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("${l.dauerSek / 60} min · ${l.flaeche} m²", color = f.textLeise, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(schliessen) { Text("Schließen") } },
    )
}

/** Kleiner Glasknopf mit Text; [aktiv] hebt die gewählte Option hervor. */
@Composable
private fun Knopf(text: String, modifier: Modifier = Modifier, aktiv: Boolean = false, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        modifier.height(40.dp).glas(f, 12.dp, erhoeht = 0.5f, toenung = if (aktiv) f.primaer else null).antippen { aktion() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (aktiv) f.primaer else f.text, fontSize = 13.sp, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1) }
}

/** Runder Knopf mit Proscenic-Symbol für die Kopfzeile (zugeklappt). */
@Composable
private fun KopfKnopf(@DrawableRes symbol: Int, beschreibung: String, gesperrt: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        Modifier.size(42.dp).glas(f, 21.dp, erhoeht = 0.5f).antippen { if (!gesperrt) aktion() }.alpha(if (gesperrt) 0.45f else 1f),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(symbol), beschreibung, Modifier.size(28.dp)) }
}

/** Knopf wie im Mehr-Menü der Proscenic-App: Symbol oben, Beschriftung darunter. */
@Composable
private fun SaugerKachel(titel: String, @DrawableRes symbol: Int, gesperrt: Boolean, modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    Column(
        modifier.height(104.dp).glas(f, 14.dp, erhoeht = 0.5f).antippen { if (!gesperrt) aktion() }
            .padding(start = 4.dp, end = 4.dp, top = 13.dp, bottom = 7.dp).alpha(if (gesperrt) 0.45f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Image(painterResource(symbol), null, Modifier.size(40.dp))
        Text(titel, color = f.text, fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
    }
}
