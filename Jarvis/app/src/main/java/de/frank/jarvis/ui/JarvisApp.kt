package de.frank.jarvis.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DesktopMac
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.tunnel.Tunnel
import de.frank.jarvis.ui.theme.JarvisTheme
import de.frank.jarvis.ui.theme.LocalBewegung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun JarvisApp(vm: AppViewModel, activity: ComponentActivity) {
    JarvisTheme(vm.einstellungen.modus, vm.einstellungen.design) {
        val f = LocalFarben.current
        val tunnel by Tunnel.zustand.collectAsState()
        var fensterBreite by remember { mutableIntStateOf(0) }
        Box(Modifier.fillMaxSize().onSizeChanged { fensterBreite = it.width }.background(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten)))) {
            Hintergrund()
            // Zurück-Geste auf Ablage, Aktivität oder Einstellungen: vorheriger Reiter, sonst Jarvis-Start, nie App schließen.
            // Steht vor dem Inhalt, damit die Zurück-Behandlung innerer Ansichten (Ablage, Merkliste, Gedächtnis) Vorrang hat.
            androidx.activity.compose.BackHandler(enabled = vm.reiter != Reiter.JARVIS && !vm.gesperrt) { vm.reiterZurueck() }
            // Zurück-Geste in einer Session: zum Startbildschirm mit der Tagesauswertung; erst dort beendet die nächste Geste die App.
            // Die Session bleibt in der Seitenleiste; während eines Live-Gesprächs bleibt die Geste wie bisher.
            androidx.activity.compose.BackHandler(enabled = vm.reiter == Reiter.JARVIS && !vm.gesperrt && !vm.liveAktiv && (vm.gespraech.isNotEmpty() || vm.sitzungId != null)) { vm.neuesGespraech() }
            val schublade = rememberGespraecheSchublade(fensterBreite)
            // Gesprächsleiste über die ganze Höhe wie in Geniale Ideen; wischen öffnet sie nur auf dem Jarvis-Reiter.
            val wischen = vm.reiter == Reiter.JARVIS && !vm.gesperrt && vm.merkOffen == null && !GedaechtnisAnsicht.offen && !FunktionenAnsicht.offen
            GespraecheSchublade(vm, schublade, wischen) {
            Box(Modifier.fillMaxSize()) {
            // Auf dem aufgeklappten Fold bleibt der Inhalt eine gut lesbare Spalte in der Mitte.
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                    .widthIn(max = 640.dp).align(Alignment.TopCenter),
            ) {
                if (vm.gesperrt) {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Kern(120.dp, f.primaer, aktiv = false)
                        Text("Jarvis ist gesperrt", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                        // Nach „Hey Jarvis“ über dem Sperrbildschirm: Jarvis hört und antwortet, zeigt aber nichts vom Gespräch.
                        val vorlesen by vm.vorleser.stand.collectAsState()
                        val status = vm.sprachStatus(vorlesen.zustand != de.frank.jarvis.speech.VorleseZustand.AUS)
                        if (status != null) Text(status, color = f.primaer, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                        Text("Im Hintergrund arbeitet Jarvis weiter und bleibt für ChatGPT erreichbar.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 22.dp))
                        // Ein klingelnder Timer lässt sich auch ohne Entsperren ausschalten.
                        KlingelndeTimer(Modifier.padding(bottom = 18.dp))
                        Knopf("Entsperren") { vm.entsperrenAnfragen() }
                    }
                    return@Column
                }
                if (FunktionenAnsicht.offen) {
                    // Vollbild über allem: geöffnet aus dem Live-Gedächtnis oder den Einstellungen, zurück landet wieder dort.
                    FunktionenBildschirm(Modifier.weight(1f))
                    return@Column
                }
                vm.merkOffen?.let { liste ->
                    // Vollbild: ohne die Reiter-Leiste, zurück mit dem Pfeil oder der Zurück-Geste.
                    MerkBildschirm(vm, liste, Modifier.weight(1f))
                    return@Column
                }
                if (GedaechtnisAnsicht.offen) {
                    GedaechtnisBildschirm(Modifier.weight(1f))
                    return@Column
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(vm.reiter, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "reiter") { reiter ->
                        when (reiter) {
                            Reiter.JARVIS -> StartBildschirm(vm, tunnel, activity, schublade)
                            Reiter.ABLAGE -> AblageBildschirm(vm, activity)
                            Reiter.AKTIVITAET -> AktivitaetBildschirm(vm)
                            Reiter.EINSTELLUNGEN -> EinstellungenBildschirm(vm, tunnel, activity)
                        }
                    }
                }
                Leiste(vm)
            }
            }
            }
            if (!vm.gesperrt) PromptAnsichtEbene(vm)
            if (!vm.gesperrt) PromptEbene(vm)
            if (!vm.gesperrt) PromptLoeschenFrage(vm)
            // Rückfrage einer Programmier-Sitzung im Vollbild, über allem außer der kurzen Meldung.
            if (!vm.gesperrt) RueckfrageEbene(vm)
            if (!vm.gesperrt) TokenlimitFenster(vm)
            Meldung(vm, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
    }
}

/** Ruhig treibende Lichtflecken hinter dem Glas. */
@Composable
private fun Hintergrund() {
    val f = LocalFarben.current
    if (LocalBewegung.current) {
        val lauf = rememberInfiniteTransition(label = "hg")
        val t = lauf.animateFloat(0f, 1f, infiniteRepeatable(tween(46_000, easing = LinearEasing)), label = "t")
        // Der Wert wird erst beim Zeichnen gelesen: Jedes Bild wird nur neu gezeichnet, nicht neu zusammengesetzt.
        // Eigene Ebene, damit das Glas darüber nicht in jedem Bild mitgezeichnet wird.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) { lichtflecken(f, t.value) }
    } else {
        // Ohne Bewegung gar keine Endlos-Animation: Sie würde sonst weiter Bilder anfordern.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) { lichtflecken(f, 0.2f) }
    }
}

private fun DrawScope.lichtflecken(f: de.frank.jarvis.ui.theme.Farben, phase: Float) {
    val w = (phase * 2 * PI).toFloat()
    fun fleck(farbe: Color, x: Float, y: Float, r: Float, alpha: Float) {
        val mitte = Offset(size.width * x, size.height * y)
        drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = alpha), Color.Transparent), mitte, size.minDimension * r), size.minDimension * r, mitte)
    }
    val staerke = if (f.dunkel) 0.34f else 0.55f
    fleck(f.blob1, 0.18f + 0.06f * cos(w), 0.12f + 0.04f * sin(w), 0.85f, staerke)
    fleck(f.blob2, 0.88f + 0.05f * sin(w), 0.42f + 0.06f * cos(w), 0.70f, staerke * 0.7f)
    fleck(f.blob3, 0.30f + 0.07f * sin(w + 1.3f), 0.92f + 0.03f * cos(w), 0.95f, staerke)
}

@Composable
private fun Leiste(vm: AppViewModel) {
    val f = LocalFarben.current
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(62.dp).glas(f, 31.dp, erhoeht = 1.2f, fuellung = f.flaecheStark).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Reihenfolge: Reiter, dann Fernwartung, Einstellungen ganz rechts.
        @Composable fun RowScope.reiterKnopf(r: Reiter) {
            val aktiv = vm.reiter == r
            val icon = when (r) { Reiter.JARVIS -> Icons.Rounded.AutoAwesome; Reiter.ABLAGE -> Icons.Rounded.Folder; Reiter.AKTIVITAET -> Icons.Rounded.History; Reiter.EINSTELLUNGEN -> Icons.Rounded.Tune }
            val grund = if (aktiv) Modifier.knopf3d(f.primaer, f.tertiaer, 25.dp, f.dunkel) else Modifier
            Row(
                Modifier.weight(if (aktiv) 1.9f else 1f).height(50.dp).then(grund).antippen { vm.reiter = r },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (aktiv) f.aufPrimaer else f.textLeise, modifier = Modifier.size(20.dp))
                if (aktiv) Text(r.anzeige, Modifier.padding(start = 7.dp), color = f.aufPrimaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Reiter.entries.filter { it != Reiter.EINSTELLUNGEN }.forEach { reiterKnopf(it) }
        // Kein Reiter: öffnet einen Rechner-Bildschirm zum Fernbedienen. Grün = mindestens ein Rechner hat in der letzten
        // Minute geantwortet (Fernwartungs-Dienst im Tunnel), rot = keiner. Geprüft alle 10 s, solange Jarvis vorn ist.
        // Beim Antippen zählt nur, wer gerade wirklich antwortet (RechnerStatus.pruefen liefert das ungeglättet).
        val context = androidx.compose.ui.platform.LocalContext.current
        val lebenszyklus = androidx.lifecycle.compose.LocalLifecycleOwner.current
        androidx.compose.runtime.LaunchedEffect(lebenszyklus) {
            lebenszyklus.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                while (true) { de.frank.jarvis.fernsteuerung.RechnerStatus.pruefen(); kotlinx.coroutines.delay(10_000) }
            }
        }
        val rechnerDa = de.frank.jarvis.fernsteuerung.RechnerStatus.erreichbar.value?.isNotEmpty()
        // Grün heißt weiter: Ein Bildschirm ist erreichbar. Nur die immer laufende Server-Konsole färbt das Symbol in der Akzentfarbe.
        val nurKonsole = de.frank.jarvis.fernsteuerung.RechnerStatus.erreichbar.value?.let { da -> da.isNotEmpty() && da.all { it.konsole } } == true
        // Antworten Mac und Windows-PC beide, fragt Jarvis, welcher; sonst direkt zum erreichbaren. Keiner: nicht blind
        // verbinden (früher der Mac, dann 6 s schwarzer Bildschirm), sondern sagen, dass keiner erreichbar ist.
        val bereich = androidx.compose.runtime.rememberCoroutineScope()
        var auswahl by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        // Läuft ein Start, bricht erneutes Tippen ihn ab: Die Anzeige verschwindet sofort, der abgebrochene Start öffnet nichts mehr.
        var start by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }
        val sucht = start != null
        val oeffnen = { z: de.frank.jarvis.fernsteuerung.Fernziel ->
            if (z.konsole) context.startActivity(android.content.Intent(context, de.frank.jarvis.fernsteuerung.ServerKonsoleActivity::class.java))
            else context.startActivity(android.content.Intent(context, de.frank.jarvis.fernsteuerung.FernsteuerungActivity::class.java)
                .putExtra(de.frank.jarvis.fernsteuerung.FernsteuerungActivity.EXTRA_ZIEL, z.name))
        }
        if (auswahl) RechnerAuswahl(de.frank.jarvis.fernsteuerung.RechnerStatus.erreichbar.value.orEmpty(), { auswahl = false }) { auswahl = false; oeffnen(it) }
        Row(
            Modifier.weight(1f).height(50.dp).antippen {
                start?.let { it.cancel(); start = null; return@antippen }
                start = bereich.launch {
                    val da = de.frank.jarvis.fernsteuerung.RechnerStatus.pruefen()
                    coroutineContext.ensureActive()
                    start = null
                    when {
                        da.size > 1 -> auswahl = true
                        da.size == 1 -> oeffnen(da.first())
                        else -> android.widget.Toast.makeText(context, "Kein Rechner für die Fernwartung erreichbar. Mac, Windows-PC und Server antworten im Tunnel nicht.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }.semantics { contentDescription = if (sucht) "Fernwartung wird vorbereitet, erneut tippen bricht ab" else "Fernwartung Mac, Windows oder Server, " + when (rechnerDa) { true -> if (nurKonsole) "nur die Server-Konsole erreichbar" else "Fernwartung möglich"; false -> "kein Rechner für die Fernwartung erreichbar"; null -> "Verbindung wird geprüft" } },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Immer derselbe ausgefüllte Monitor, nur die Farbe zeigt den Zustand; grün leuchtet zusätzlich.
            // Während Jarvis prüft, welche Rechner antworten (bis etwa 13 s mit zweitem Versuch), grau mit drehendem Ring: das Tippen ist angekommen.
            val farbe = if (sucht) f.textLeise else when (rechnerDa) { true -> if (nurKonsole) f.primaer else f.erfolg; false -> f.gefahr; null -> f.textLeise }
            // Gleiche Größe wie die Reitersymbole (20 dp).
            Box(contentAlignment = Alignment.Center) {
                if (sucht) androidx.compose.material3.CircularProgressIndicator(Modifier.size(36.dp), color = f.primaer, strokeWidth = 2.5.dp)
                Icon(Icons.Rounded.DesktopMac, null, tint = farbe, modifier = Modifier.size(20.dp).drawBehind {
                    if (rechnerDa == true && !sucht) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(farbe.copy(alpha = 0.5f), androidx.compose.ui.graphics.Color.Transparent)), radius = size.minDimension * 1.1f)
                })
            }
        }
        reiterKnopf(Reiter.EINSTELLUNGEN)
    }
}

/** Mehrere Rechner sind erreichbar: Frank wählt, wessen Bildschirm oder Konsole er sehen will. */
@Composable
private fun RechnerAuswahl(da: List<de.frank.jarvis.fernsteuerung.Fernziel>, schliessen: () -> Unit, gewaehlt: (de.frank.jarvis.fernsteuerung.Fernziel) -> Unit) {
    val f = LocalFarben.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = schliessen,
        containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Fernwartung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Was möchtest du sehen?")
                da.forEach { z ->
                    androidx.compose.material3.Button(onClick = { gewaehlt(z) }, Modifier.fillMaxWidth()) { Text(z.titel) }
                }
            }
        },
        confirmButton = {},
    )
}

/** Rückfrage, wenn eine Chat-Aufgabe Franks Tokenlimit ausgeschöpft hat. Tippen daneben entscheidet nichts. */
@Composable
private fun TokenlimitFenster(vm: AppViewModel) {
    val f = LocalFarben.current
    val frage = vm.tokenlimitFrage ?: return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {},
        containerColor = menueFlaeche(f), titleContentColor = f.text, textContentColor = f.text,
        title = { Text("Aufgabe weitermachen?") },
        text = { Text(frage.text) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { vm.tokenlimitEntscheiden(true) }) { Text("Weiter") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { vm.tokenlimitEntscheiden(false) }) { Text("Abbrechen", color = f.gefahr) } },
    )
}

/** Kurze Rückmeldung oben, verschwindet von selbst. */
@Composable
private fun Meldung(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val text = vm.meldung
    LaunchedEffect(text) {
        // Längere Meldungen (etwa die Rückfrage, welche Session gemeint ist) bleiben länger stehen.
        if (text != null) { delay(maxOf(2_800L, text.length * 60L)); if (vm.meldung == text) vm.meldung = null }
    }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        Text(
            text.orEmpty(),
            Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp).glas(f, 18.dp, erhoeht = 1.4f, fuellung = menueFlaeche(f).copy(alpha = 0.96f)).padding(horizontal = 18.dp, vertical = 12.dp),
            color = f.text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        )
    }
}

// ---------------------------------------------------------------- Bausteine

/** Der Leuchtkern: zwei gegenläufige Ringe um einen pulsierenden Mittelpunkt. [aktiv] = schneller und heller. */
@Composable
fun Kern(groesse: Dp, farbe: Color, aktiv: Boolean, modifier: Modifier = Modifier) {
    // key: InfiniteTransition übernimmt ein geändertes Tempo nicht, deshalb wird sie beim Wechsel von [aktiv] neu angelegt.
    if (LocalBewegung.current) androidx.compose.runtime.key(aktiv) {
        val lauf = rememberInfiniteTransition(label = "kern")
        val dreh = lauf.animateFloat(0f, 360f, infiniteRepeatable(tween(if (aktiv) 2_600 else 9_000, easing = LinearEasing)), label = "dreh")
        val puls = lauf.animateFloat(0.78f, 1f, infiniteRepeatable(tween(if (aktiv) 620 else 1_900), RepeatMode.Reverse), label = "puls")
        // Drehung und Puls erst beim Zeichnen lesen, auf eigener Ebene: kein Neuaufbau der Umgebung in jedem Bild.
        Canvas(modifier.size(groesse).graphicsLayer()) { kernZeichnen(farbe, dreh.value, puls.value) }
    } else {
        Canvas(modifier.size(groesse)) { kernZeichnen(farbe, 30f, 0.9f) }
    }
}

private fun DrawScope.kernZeichnen(farbe: Color, winkel: Float, p: Float) {
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = 0.34f * p), Color.Transparent), center, r), r)
    val strich = r * 0.085f
    val aussen = r * 0.74f
    drawCircle(farbe.copy(alpha = 0.22f), aussen, style = Stroke(strich * 0.5f))
    for (i in 0 until 3) {
        drawArc(farbe.copy(alpha = 0.95f), winkel + i * 120f, 62f, false, Offset(center.x - aussen, center.y - aussen), androidx.compose.ui.geometry.Size(aussen * 2, aussen * 2), style = Stroke(strich, cap = StrokeCap.Round))
    }
    val innen = r * 0.50f
    for (i in 0 until 6) {
        drawArc(farbe.copy(alpha = 0.6f), -winkel * 1.6f + i * 60f, 26f, false, Offset(center.x - innen, center.y - innen), androidx.compose.ui.geometry.Size(innen * 2, innen * 2), style = Stroke(strich * 0.8f, cap = StrokeCap.Round))
    }
    drawCircle(Brush.radialGradient(listOf(Color.White, farbe, farbe.copy(alpha = 0f)), center, r * 0.34f * p), r * 0.34f * p)
}

@Composable
fun Karte(modifier: Modifier = Modifier, toenung: Color? = null, inhalt: @Composable ColumnScope.() -> Unit) {
    val f = LocalFarben.current
    Column(modifier.fillMaxWidth().glas(f, toenung = toenung).padding(18.dp), content = inhalt)
}

/** Aufklappbarer Einstellungsbereich: Kopfzeile antippen klappt den Inhalt auf und zu. Startet zugeklappt; mit [aufklappbar] = false immer offen. */
@Composable
fun Bereich(titel: String, aufklappbar: Boolean = true, inhalt: @Composable ColumnScope.() -> Unit) {
    val f = LocalFarben.current
    var offen by androidx.compose.runtime.saveable.rememberSaveable(titel) { mutableStateOf(!aufklappbar) }
    val drehung by androidx.compose.animation.core.animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")
    Column(Modifier.fillMaxWidth().glas(f)) {
        Row(
            Modifier.fillMaxWidth().then(if (aufklappbar) Modifier.antippen { offen = !offen } else Modifier).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(titel, Modifier.weight(1f), color = f.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (aufklappbar) Icon(Icons.Rounded.KeyboardArrowDown, if (offen) "Zuklappen" else "Aufklappen", Modifier.size(26.dp).graphicsLayer { rotationZ = drehung }, tint = f.textLeise)
        }
        AnimatedVisibility(offen, enter = androidx.compose.animation.expandVertically() + fadeIn(), exit = androidx.compose.animation.shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp), content = inhalt)
        }
    }
}

@Composable
fun Abschnitt(titel: String, modifier: Modifier = Modifier) {
    Text(titel.uppercase(), modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp), color = LocalFarben.current.textSchwach, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

/** Hauptknopf (plastisch, gefüllt) oder Nebenknopf (Glas). */
@Composable
fun Knopf(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, haupt: Boolean = true, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    val c = farbe ?: f.primaer
    val grund = if (haupt) Modifier.knopf3d(c, if (farbe == null) f.tertiaer else c.copy(alpha = 0.8f), 16.dp, f.dunkel) else Modifier.glas(f, 16.dp, erhoeht = 0.5f)
    Row(
        // Mindesthöhe statt fester Höhe: Bei großer Schrift wächst der Knopf mit, statt dass Text und Nachbarn überlappen.
        modifier.heightIn(min = 48.dp).then(grund).antippen(aktion = aktion).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = if (haupt) f.aufPrimaer else c, modifier = Modifier.padding(end = 8.dp).size(18.dp))
        Text(text, color = if (haupt) f.aufPrimaer else f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Eingabefeld. Mit [geheim] ist der Inhalt verdeckt; das Auge rechts zeigt ihn. */
@Composable
fun Eingabe(
    wert: String,
    beiAenderung: (String) -> Unit,
    platzhalter: String,
    modifier: Modifier = Modifier,
    geheim: Boolean = false,
    einzeilig: Boolean = true,
    senden: (() -> Unit)? = null,
    tastatur: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    val f = LocalFarben.current
    var sichtbar by remember { mutableStateOf(false) }
    Row(modifier.glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(start = 16.dp, end = if (geheim) 6.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            if (wert.isEmpty()) Text(platzhalter, color = f.textSchwach, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                wert, beiAenderung, Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = f.text, fontSize = 16.sp),
                cursorBrush = SolidColor(f.primaer),
                singleLine = einzeilig,
                maxLines = if (einzeilig) 1 else 5,
                visualTransformation = if (geheim && !sichtbar && wert.isNotEmpty()) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = tastatur, imeAction = if (senden != null) ImeAction.Send else ImeAction.Default),
                keyboardActions = KeyboardActions(onSend = { senden?.invoke() }),
            )
        }
        if (geheim) Box(Modifier.size(40.dp).antippen(haptik = false) { sichtbar = !sichtbar }, contentAlignment = Alignment.Center) {
            Icon(if (sichtbar) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (sichtbar) "Verbergen" else "Anzeigen", tint = f.textLeise, modifier = Modifier.size(20.dp))
        }
    }
}

/** Uhrzeit oder Datum eines Protokolleintrags, kurz. */
fun zeitKurz(millis: Long): String {
    val zeit = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
    val heute = java.time.LocalDate.now()
    val muster = if (zeit.toLocalDate() == heute) "HH:mm" else "d. MMM, HH:mm"
    return zeit.format(java.time.format.DateTimeFormatter.ofPattern(muster, java.util.Locale.GERMAN))
}
