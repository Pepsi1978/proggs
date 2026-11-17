@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package de.frank.jarvis.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.delay
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
    /**
     * Wann eine Prüfung zuletzt die Proscenic-Cloud selbst nicht erreichte (ms; 0 = nie). Angezeigt nur, solange danach keine
     * Erreichbarkeit bestätigt wurde ([SaugerStand.erreichbarUm]), damit ein einzelner Aussetzer nicht rot stehen bleibt.
     */
    var cloudFehlerUm by mutableStateOf(0L)
    var geprueft by mutableStateOf(false)
    var laedt by mutableStateOf(false)
    var meldung by mutableStateOf<SaugerAusgang?>(null)
    /** Abschnitt, zu dem nach dem Aufklappen gescrollt wird: "saugstufe" oder "staub" (aus der Statuszeile). */
    var ziel by mutableStateOf<String?>(null)
    /** Wann der Zustand zuletzt still gelesen wurde (ms). */
    var zustandUm = 0L
    /** Live-Karte im Vollbild offen (Franks Wunsch vom 17.11.2026). */
    var liveOffen by mutableStateOf(false)
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
            if (cloud) aktualisieren(context, mitKarte = a.offen)
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

    // Zustand still lesen, damit Pause-Taste und Statuszeile auch zugeklappt stimmen: beim Erscheinen, während der Roboter
    // arbeitet alle 45 Sekunden, sonst alle 5 Minuten (nur solange die Karte zu sehen ist).
    // Nicht auf dem Tablet: Handy und Tablet teilen das Proscenic-Konto, jede Anmeldung macht die andere ungültig.
    if (cloud && !de.frank.jarvis.data.Geraet.istTablet) LaunchedEffect(Unit) {
        while (true) {
            val aktiv = ProscenicMehr.aktiv(a.live?.modus)
            // Die offene Live-Karte hält selbst die Gateway-Verbindung; eine zweite würde sie verdrängen.
            // Gilt der Roboter als nicht erreichbar, jede Minute nachsehen, damit die Anzeige nicht rot stehen bleibt.
            val abstand = if (aktiv) 45_000L else if (SaugerStand.erreichbar == false) 60_000L else 5 * 60_000L
            if (!de.frank.jarvis.staubsauger.SaugerLive.aktiv && System.currentTimeMillis() - a.zustandUm > abstand) zustandLesen(context)
            // Dazwischen alle 2 Minuten nur die Erreichbarkeit (schnelle Abfrage ohne Gateway, verdrängt die Proscenic-App nicht).
            else if (System.currentTimeMillis() - SaugerStand.erreichbarUm > 2 * 60_000L && !a.laedt) erreichbarPruefen(context)
            delay(15_000L)
        }
    }

    fun aufklappen(ziel: String? = null) {
        a.ziel = ziel
        if (!a.offen) {
            a.offen = true
            if (cloud) saugerBereich.launch { oeffnen(context) }
        }
    }

    Karte(modifier = griff) {
        // Kopf: Titel, Akku, Erreichbarkeit, dann Tiefenreinigung, Pause, Aktualisieren mit Abstand zueinander und zum Klapp-Pfeil.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().antippen {
            if (a.offen) { a.offen = false; a.ziel = null } else aufklappen()
        }) {
            Column(Modifier.weight(1f)) {
                Text("Staubsauger", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.sauger_laden), null, Modifier.size(18.dp))
                    Text(
                        (live?.akku ?: s.akku)?.let { "$it %" } ?: "–",
                        color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                    )
                }
                // Stand und Erreichbarkeit als eigene Zeile unter dem Akku, auf jedem Gerät gleich (daneben wurde es zugeklappt gequetscht).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val st = Modifier
                    when {
                        a.laedt || s.laeuft -> Text("wird gelesen …", color = f.primaer, fontSize = 13.sp, modifier = st)
                        a.cloudFehlerUm > s.erreichbarUm -> Text("Cloud nicht erreichbar", color = f.gefahr, fontSize = 13.sp, modifier = st)
                        s.erreichbar == false -> Text("nicht erreichbar", color = f.gefahr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = st)
                        s.erreichbar == true -> {
                            Text("erreichbar", color = f.erfolg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = st)
                            // Alter von Akku und Zustand weiter sichtbar, damit ein alter Wert nicht als aktuell gilt.
                            s.gelesenUm?.let { Text(" · Stand ${alterKurz(it)}", color = f.textLeise, fontSize = 13.sp) }
                        }
                        s.gelesenUm != null -> Text("Stand ${alterKurz(s.gelesenUm!!)}", color = f.textLeise, fontSize = 13.sp, modifier = st)
                    }
                }
            }
            KopfKnopf(R.drawable.sauger_tiefenreinigung, "Tiefenreinigung", s.laeuft) { los(Aktion.TIEFENREINIGUNG) }
            if (cloud) {
                // Pause/Weiter: läuft er, Pause-Symbol; pausiert, Play-Symbol; steht er, ausgegraut.
                val modus = live?.modus
                val pausiert = ProscenicMehr.pausiert(modus)
                val bereit = ProscenicMehr.aktiv(modus) && !a.laedt
                Spacer(Modifier.width(18.dp))
                Box(
                    Modifier.size(42.dp).glas(f, 21.dp, erhoeht = 0.5f).antippen { if (bereit) einstellen { ProscenicMehr.pauseOderWeiter(context, weiter = pausiert) } }
                        .alpha(if (bereit) 1f else 0.4f),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (pausiert) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, if (pausiert) "Weiter reinigen" else "Pause", tint = f.text, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.width(18.dp))
            Box(
                Modifier.size(42.dp).glas(f, 21.dp, erhoeht = 0.5f, toenung = if (a.laedt) f.primaer else null)
                    .antippen { if (cloud) saugerBereich.launch { aktualisieren(context, mitKarte = a.offen) } else los(Aktion.STATUS) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Refresh, "Aktualisieren", tint = if (a.laedt) f.primaer else f.textLeise, modifier = Modifier.size(20.dp)) }
            Icon(
                if (a.offen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, if (a.offen) "Zuklappen" else "Aufklappen",
                tint = f.textLeise, modifier = Modifier.padding(start = 22.dp).size(26.dp),
            )
        }
        // Statuszeile, solange der Roboter arbeitet: Saugstufe und automatische Staubsammlung, beides antippbar.
        if (!a.offen && cloud && live != null && ProscenicMehr.aktiv(live.modus)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Saugstufe:", color = f.textLeise, fontSize = 13.sp)
                StatusKnopf(stufeName(live.saugstufe)) { aufklappen("saugstufe") }
                Text("Staubsammlung:", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                StatusKnopf(when (live.staubFrequenz) { null -> "–"; 0 -> "aus"; else -> "an" }) { aufklappen("staub") }
            }
            // Während der Reinigung direkt zur Live-Karte.
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Fahrt verfolgen:", color = f.textLeise, fontSize = 13.sp)
                StatusKnopf("Live-Karte") { a.liveOffen = true }
            }
        }
        val meldung = a.meldung ?: s.meldung?.takeIf { !s.laeuft }
        meldung?.let { Text(it.text, color = if (it.fehler) f.gefahr else f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }

        if (a.offen) Aufgeklappt(cloud, ::los, ::einstellen)
    }
    // Verschwindet die Karte (anderer Bereich, Neuaufbau), schließt auch die Live-Karte: Sonst öffnete sie sich beim Zurückkommen von selbst.
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { a.liveOffen = false } }
    if (cloud && a.liveOffen) SaugerLiveDialog(a.live) {
        a.liveOffen = false
        // Danach Zustand (und aufgeklappt die Karte) wieder wie gewohnt lesen.
        saugerBereich.launch { aktualisieren(context, mitKarte = a.offen) }
    }
}

/** Antippbarer Wert in der Statuszeile. */
@Composable
private fun StatusKnopf(text: String, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.padding(start = 6.dp).height(34.dp).glas(f, 12.dp, erhoeht = 0.5f, toenung = f.primaer).antippen { aktion() }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Übernimmt einen frisch gelesenen Stand; ohne Karte bleibt die bisherige (gespeicherte) Karte stehen. */
private suspend fun uebernehmen(context: android.content.Context, l: ProscenicMehr.Live) {
    val a = SaugerAnsicht
    val alt = a.live
    a.live = if (l.karte == null && alt?.karte != null) alt.mitZustand(l) else l
    a.zustandUm = System.currentTimeMillis()
    withContext(Dispatchers.IO) { SaugerStand.merken(context, l.akku, l.modus?.let(::zustandName), stufeName(l.saugstufe)) }
}

/** Liest still nur den Zustand (ohne Karte, ohne „wird gelesen …“). */
private suspend fun zustandLesen(context: android.content.Context) {
    val a = SaugerAnsicht
    if (a.laedt) return
    a.zustandUm = System.currentTimeMillis()
    // Erst die Erreichbarkeit (schnell, ohne Gateway); nur ein verbundener Roboter schickt seinen Zustand.
    if (erreichbarPruefen(context) == false) return
    ProscenicMehr.live(context, mitKarte = false)?.let { uebernehmen(context, it) }
}

/**
 * Fragt nur ab, ob der Roboter mit der Cloud verbunden ist. Ist er es nicht, gilt sein letzter Modus nicht mehr: Pause-Taste und
 * schnelles Nachlesen sollen nicht weiter so tun, als reinige er noch.
 */
private suspend fun erreichbarPruefen(context: android.content.Context): Boolean? {
    val a = SaugerAnsicht
    val online = ProscenicMehr.erreichbar(context)
    if (online == null) a.cloudFehlerUm = System.currentTimeMillis()
    a.geprueft = true
    if (online == false) a.live = a.live?.copy(modus = null)
    return online
}

/**
 * Aufklappen: zuerst sofort die gespeicherte Karte zeigen, dann nur prüfen, ob es eine neuere gibt (der Roboter arbeitet gerade
 * oder im Verlauf endete eine Reinigung nach der gespeicherten Karte); nur dann wird die Karte neu geholt, sonst nur der Zustand.
 */
private suspend fun oeffnen(context: android.content.Context) {
    val a = SaugerAnsicht
    if (a.live?.karte == null) ProscenicMehr.ausSpeicher(context)?.let { c ->
        val l = a.live
        a.live = if (l == null) c else c.mitZustand(l)
    }
    // Zustand noch unbekannt (Karte direkt nach dem App-Start geöffnet): erst lesen, sonst bliebe eine laufende Reinigung unerkannt.
    if (a.live?.modus == null) zustandLesen(context)
    val neuer = a.live?.karte == null || ProscenicMehr.aktiv(a.live?.modus) ||
        (ProscenicMehr.letzteReinigungEnde(context)?.let { it > ProscenicMehr.kartenStand(context) } ?: false)
    aktualisieren(context, mitKarte = neuer)
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
    // Erst prüfen, dann lesen: Ein offline Roboter würde sonst eine 14 Sekunden lange Gateway-Sitzung für nichts öffnen.
    if (erreichbarPruefen(context) == false) return@coroutineScope
    ProscenicMehr.live(context, mitKarte)?.let { uebernehmen(context, it) }
}

/** Liest Karte, Zonen und Zeitpläne frisch (wartet einen laufenden Abruf ab). */
private suspend fun frischMitKarte(context: android.content.Context) {
    while (SaugerAnsicht.laedt) delay(100)
    aktualisieren(context, mitKarte = true)
}

/** Meldung, wenn Zeitpläne oder Zonen nicht frisch vom Roboter kamen und deshalb nicht bearbeitet werden dürfen. */
private fun nichtFrisch(was: String) = SaugerAusgang(
    if (SaugerStand.erreichbar == false) de.frank.jarvis.staubsauger.ProscenicCloud.OFFLINE_TEXT
    else "Die $was kamen gerade nicht frisch vom M7 Pro; damit nichts verloren geht, kannst du sie erst nach dem nächsten Abruf bearbeiten.", true)

internal fun zustandName(m: String) = when (m) {
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
    /** Frisch gelesener Stand, auf dem der Zonen-Entwurf bzw. der Zeitplan-Dialog beruht (Grundlage fürs Speichern). */
    var zonenBasis by remember { mutableStateOf<ProscenicMehr.Live?>(null) }
    var zeitplanBasis by remember { mutableStateOf<ProscenicMehr.Live?>(null) }
    val entwurf = remember { mutableStateListOf<Zone>() }
    var dialog by remember { mutableStateOf<String?>(null) }
    var vollbild by remember { mutableStateOf(false) }
    val saugZiel = remember { BringIntoViewRequester() }
    val staubZiel = remember { BringIntoViewRequester() }
    // Aus der Statuszeile: zum gewünschten Abschnitt scrollen. Einige Male nachfassen, weil die Karte darüber noch erscheinen kann.
    LaunchedEffect(a.ziel) {
        val z = a.ziel ?: return@LaunchedEffect
        val r = if (z == "staub") staubZiel else saugZiel
        repeat(6) { delay(250); runCatching { r.bringIntoView() } }
        a.ziel = null
    }

    live?.modus?.let { Text("Zustand: ${zustandName(it)} · Saugstufe: ${stufeName(live.saugstufe)}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }

    fun neueZone(x1: Int, y1: Int, x2: Int, y2: Int) {
        if (kotlin.math.abs(x2 - x1) < 150 || kotlin.math.abs(y2 - y1) < 150) return   // versehentliches Wischen, keine Zone
        val id = (entwurf.maxOfOrNull { it.json.optInt("id") } ?: 1000) + 1
        val nr = entwurf.count { it.sperre } + 1
        val j = JSONObject().put("active", "forbid").put("id", id).put("mode", "default").put("name", "扫地机禁区").put("tag", nr.toString())
            .put("forbidType", "all")
            .put("vertexs", JSONArray().put(JSONArray().put(minOf(x1, x2)).put(maxOf(y1, y2))).put(JSONArray().put(maxOf(x1, x2)).put(maxOf(y1, y2)))
                .put(JSONArray().put(maxOf(x1, x2)).put(minOf(y1, y2))).put(JSONArray().put(minOf(x1, x2)).put(minOf(y1, y2))))
        entwurf.add(Zone(j))
    }
    fun zonenSpeichern() {
        val basis = zonenBasis ?: return
        val liste = entwurf.toList(); zonenModus = false; zonenBasis = null
        einstellen { ProscenicMehr.zonenSpeichern(context, basis, liste) }
    }

    // Karte der Wohnung: Antippen öffnet sie im Vollbild (Zoom mit zwei Fingern, zurück per Wischgeste).
    val karte = live?.karte
    LaunchedEffect(karte == null) { if (karte == null) vollbild = false }
    if (karte != null) {
        KartenAnsicht(live, if (zonenModus) entwurf else live.zonen, zonenModus, vollbild = false,
            beiTipp = { vollbild = true },
            beiZone = { i -> if (zonenModus && entwurf[i].sperre) entwurf.removeAt(i) },
            beiNeu = ::neueZone)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Legende(Color(0xFF1565C0), "Roboter"); Legende(Color(0xFF43A047), "Station"); Legende(Color(0xFF1E88E5), "Fahrweg"); Legende(Color(0xFFE53935), "Sperrzone")
        }
        // Live-Karte: Position und gefahrene Strecke der laufenden Reinigung in Echtzeit (auch abrufbar, wenn er gerade steht).
        if (cloud) Knopf(if (ProscenicMehr.aktiv(live.modus)) "Live-Karte: Fahrt verfolgen" else "Live-Karte", Modifier.fillMaxWidth().padding(top = 6.dp)) { a.liveOffen = true }
        if (vollbild) Dialog(onDismissRequest = { vollbild = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(f.hgOben)) {
                KartenAnsicht(live, if (zonenModus) entwurf else live.zonen, zonenModus, vollbild = true, modifier = Modifier.fillMaxSize(),
                    beiZone = { i -> if (zonenModus && entwurf[i].sperre) entwurf.removeAt(i) },
                    beiNeu = ::neueZone)
                // Nur die Karte: Statusleiste und Navigationsleiste ausgeblendet, solange das Vollbild offen ist (Wischen vom Rand holt sie kurz zurück).
                val dialogFenster = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                LaunchedEffect(dialogFenster) {
                    dialogFenster?.let { w ->
                        androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }
                // Knöpfe halten den Platz der ausgeblendeten Leisten frei: Eine per Wischen geholte Leiste verdeckt sie nicht,
                // und unten liegen sie nicht in der Gestenzone.
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                val rand = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.systemBarsIgnoringVisibility)
                    .displayCutoutPadding().padding(12.dp)
                Column(Modifier.fillMaxWidth().then(rand)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hinweis nur beim Sperrzonen-Zeichnen; sonst bleibt nur die Karte (Franks Wunsch vom 15.11.2026).
                        if (zonenModus) Text("Rechteck ziehen = neue Sperrzone, rote Zone antippen = löschen, zwei Finger = zoomen",
                            color = f.textLeise, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        else Spacer(Modifier.weight(1f))
                        Box(Modifier.size(42.dp).glas(f, 21.dp, erhoeht = 0.5f).antippen { vollbild = false }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Schließen", tint = f.text, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                if (zonenModus) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().then(rand), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Knopf("Speichern", Modifier.weight(1f)) { zonenSpeichern(); vollbild = false }
                    Knopf("Abbrechen", Modifier.weight(1f)) { zonenModus = false }
                }
            }
        }
    } else {
        Text(if (a.laedt) "Karte wird geladen …" else if (cloud) "Keine Karte – tippe auf Aktualisieren." else "Die Karte gibt es nur mit der Proscenic-Anmeldung unter Konten.",
            color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
    }
    if (zonenModus) {
        Text("Ziehe auf der Karte ein Rechteck für eine neue Sperrzone; tippe die Karte an, um sie groß zu bearbeiten (rote Zone antippen = löschen).", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Knopf("Speichern", Modifier.weight(1f)) { zonenSpeichern() }
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
    Text("Saugstufe", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp).bringIntoViewRequester(saugZiel))
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
    Row(Modifier.fillMaxWidth().padding(top = 6.dp).bringIntoViewRequester(staubZiel), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to "Aus", 1 to "Jedes Mal", 2 to "Jedes 2.", 3 to "Jedes 3.").forEach { (w, t) ->
            Knopf(t, Modifier.weight(1f), aktiv = live?.staubFrequenz == w) { einstellen { ProscenicMehr.staubFrequenz(context, w) } }
        }
    }
    // Sperrzonen, Zeitpläne, Verlauf.
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Vor dem Bearbeiten Zonen und Zeitpläne frisch vom Roboter holen: Gespeichert wird immer die ganze Liste, ein alter
        // Stand aus dem Speicher würde Änderungen aus der Proscenic-App oder per Sprache überschreiben.
        Knopf("Sperrzonen", Modifier.weight(1f), aktiv = zonenModus) {
            if (zonenModus) return@Knopf
            saugerBereich.launch {
                frischMitKarte(context)
                val l = a.live ?: return@launch
                if (l.karte == null) return@launch
                // Nur auf frisch vom Roboter gelesenen Zonen bearbeiten, sonst löschte das Speichern Zonen aus der Proscenic-App.
                if (!l.zonenGelesen) { a.meldung = nichtFrisch("Sperrzonen"); return@launch }
                zonenBasis = l
                entwurf.clear(); entwurf.addAll(l.zonen); zonenModus = true
            }
        }
        Knopf("Zeitpläne", Modifier.weight(1f)) {
            saugerBereich.launch {
                frischMitKarte(context)
                val l = a.live
                if (l == null || !l.zeitplaeneGelesen) { a.meldung = nichtFrisch("Zeitpläne"); return@launch }
                zeitplanBasis = l; dialog = "zeitplaene"
            }
        }
        Knopf("Verlauf", Modifier.weight(1f)) { dialog = "verlauf" }
    }

    when (dialog) {
        "lautstaerke" -> LautstaerkeDialog(live?.lautstaerke ?: 5, { dialog = null }) { w -> dialog = null; einstellen { ProscenicMehr.lautstaerke(context, w) } }
        "zeitplaene" -> zeitplanBasis?.let { basis ->
            ZeitplanDialog(basis, { dialog = null; zeitplanBasis = null }) { liste -> dialog = null; zeitplanBasis = null; einstellen { ProscenicMehr.zeitplaeneSpeichern(context, basis, liste) } }
        }
        "verlauf" -> VerlaufDialog { dialog = null }
    }
}

/** Abbildung Kartenpixel ↔ Bildschirm: eingepasst (basis, bx, by), dazu Zoom und Verschiebung des Vollbilds. */
private class Sicht(val basis: Float, val bx: Float, val by: Float, val zoom: Float, val pan: Offset) {
    fun bild(px: Float, py: Float) = Offset(pan.x + zoom * (bx + basis * px), pan.y + zoom * (by + basis * py))
    fun karte(p: Offset) = Offset(((p.x - pan.x) / zoom - bx) / basis, ((p.y - pan.y) / zoom - by) / basis)
    companion object {
        fun von(w: Float, h: Float, k: ProscenicMehr.Karte, zoom: Float, pan: Offset): Sicht {
            val basis = minOf(w / k.breite, h / k.hoehe)
            return Sicht(basis, (w - k.breite * basis) / 2, (h - k.hoehe * basis) / 2, zoom, pan)
        }
    }
}

/** Verschiebung so begrenzen, dass die gezoomte Fläche den Bildschirm nie freigibt. */
private fun klemmen(pan: Offset, zoom: Float, w: Float, h: Float) = Offset(pan.x.coerceIn(w * (1 - zoom), 0f), pan.y.coerceIn(h * (1 - zoom), 0f))

/**
 * Wohnungskarte mit Fahrweg (blau), Roboter, Station (grün), Räumen und Sperrzonen (rot).
 * Klein: Antippen ruft [beiTipp] (Vollbild), im Sperrzonen-Modus zieht ein Finger ein Rechteck.
 * Vollbild: zwei Finger zoomen und verschieben; ein Finger verschiebt, im Sperrzonen-Modus zieht er ein Rechteck, Antippen löscht eine Zone.
 */
@Composable
private fun KartenAnsicht(
    live: ProscenicMehr.Live, zonen: List<Zone>, bearbeiten: Boolean, vollbild: Boolean, modifier: Modifier = Modifier,
    beiTipp: () -> Unit = {}, beiZone: (Int) -> Unit, beiNeu: (Int, Int, Int, Int) -> Unit,
) {
    val f = LocalFarben.current
    val k = live.karte ?: return
    val bild = remember(k) { k.bild.asImageBitmap() }
    var ziehStart by remember { mutableStateOf<Offset?>(null) }
    var ziehEnde by remember { mutableStateOf<Offset?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val flaeche = if (vollbild) Modifier.fillMaxSize()
    else Modifier.fillMaxWidth().aspectRatio(k.breite.toFloat() / k.hoehe, matchHeightConstraintsFirst = true).glas(f, 12.dp)
    Box(if (vollbild) modifier else Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(max = 520.dp), contentAlignment = Alignment.Center) {
        Canvas(
            flaeche.pointerInput(bearbeiten, vollbild, zonen, k) {
                awaitEachGesture {
                    val unten = awaitFirstDown(requireUnconsumed = false)
                    val start = unten.position
                    var ende = start; var bewegt = false; var mehr = false; var tippGilt = true
                    while (true) {
                        val ev = awaitPointerEvent()
                        val gedrueckt = ev.changes.filter { it.pressed }
                        // Langer Druck (Karte verschieben) oder ein Griff der Liste ist kein Tipp.
                        if (ev.changes.any { it.uptimeMillis - unten.uptimeMillis > viewConfiguration.longPressTimeoutMillis }) tippGilt = false
                        if (gedrueckt.isEmpty()) { if (ev.changes.any { it.isConsumed }) tippGilt = false; break }
                        if (gedrueckt.size >= 2) {
                            mehr = true; ziehStart = null; ziehEnde = null
                            if (!vollbild) continue
                            val c = ev.calculateCentroid(); val nz = (zoom * ev.calculateZoom()).coerceIn(1f, 8f)
                            pan = klemmen(c - (c - pan) * (nz / zoom) + ev.calculatePan(), nz, size.width.toFloat(), size.height.toFloat())
                            zoom = nz
                            ev.changes.forEach { it.consume() }
                        } else if (!mehr) {
                            val ch = gedrueckt[0]; ende = ch.position
                            if (!bewegt && (ende - start).getDistance() > viewConfiguration.touchSlop) bewegt = true
                            if (bewegt && bearbeiten) { ziehStart = start; ziehEnde = ende; ch.consume() }
                            else if (bewegt && vollbild) { pan = klemmen(pan + ch.positionChange(), zoom, size.width.toFloat(), size.height.toFloat()); ch.consume() }
                        }
                    }
                    if (!mehr) {
                        val sicht = Sicht.von(size.width.toFloat(), size.height.toFloat(), k, zoom, pan)
                        if (bewegt && bearbeiten) {
                            val a = sicht.karte(start); val b = sicht.karte(ende)
                            beiNeu(k.mmX(a.x), k.mmY(a.y), k.mmX(b.x), k.mmY(b.y))
                        } else if (!bewegt && !vollbild) { if (tippGilt) beiTipp() }
                        else if (!bewegt && bearbeiten && tippGilt) {
                            val p = sicht.karte(start); val mx = k.mmX(p.x); val my = k.mmY(p.y)
                            val i = zonen.indexOfFirst { z -> z.sperre && z.ecken.let { e -> e.isNotEmpty() && mx in e.minOf { it.first }..e.maxOf { it.first } && my in e.minOf { it.second }..e.maxOf { it.second } } }
                            if (i >= 0) beiZone(i)
                        }
                    }
                    ziehStart = null; ziehEnde = null
                }
            },
        ) {
            val sicht = Sicht.von(size.width, size.height, k, zoom, pan)
            fun pt(x: Int, y: Int) = sicht.bild(k.px(x), k.py(y))
            clipRect {
                val o = sicht.bild(0f, 0f)
                val sk = sicht.basis * sicht.zoom
                drawImage(bild, IntOffset.Zero, IntSize(k.breite, k.hoehe), IntOffset(o.x.toInt(), o.y.toInt()),
                    IntSize((k.breite * sk).toInt(), (k.hoehe * sk).toInt()), filterQuality = FilterQuality.None)
                // Räume und Sperrzonen.
                zonen.forEach { z ->
                    val e = z.ecken; if (e.isEmpty()) return@forEach
                    val a = pt(e.minOf { it.first }, e.maxOf { it.second }); val b = pt(e.maxOf { it.first }, e.minOf { it.second })
                    if (z.sperre) {
                        drawRect(Color(0x44E53935), a, Size(b.x - a.x, b.y - a.y))
                        drawRect(Color(0xFFE53935), a, Size(b.x - a.x, b.y - a.y), style = Stroke(2.dp.toPx()))
                    } else drawRect(Color(0x33607D8B), a, Size(b.x - a.x, b.y - a.y), style = Stroke(1.dp.toPx()))
                }
                // Fahrweg.
                if (live.pfad.size > 1) {
                    val p = Path()
                    live.pfad.forEachIndexed { i, (x, y) -> val q = pt(x, y); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
                    drawPath(p, Color(0xCC1E88E5), style = Stroke(1.6.dp.toPx()))
                }
                live.station?.let { (x, y) -> drawCircle(Color(0xFF43A047), 6.dp.toPx(), pt(x, y)) }
                live.roboter?.let { (x, y) ->
                    drawCircle(Color.White, 8.dp.toPx(), pt(x, y))
                    drawCircle(Color(0xFF1565C0), 6.dp.toPx(), pt(x, y))
                }
                val a = ziehStart; val b = ziehEnde
                if (a != null && b != null) {
                    drawRect(Color(0x44E53935), Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y)))
                }
            }
        }
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
