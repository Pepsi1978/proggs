package de.frank.aufgaben.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.Farben
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay

/** Alle Bereiche aus der Gesamtliste, datumsbasiert berechnet. */
class Bereiche(alle: List<Aufgabe>, heute: Long, suche: String) {
    private val gefiltert = if (suche.isBlank()) alle else alle.filter {
        it.titel.contains(suche, true) || it.text.contains(suche, true)
    }
    private val tagesOrdnung = compareBy<Aufgabe>({ it.prio.rang }, { -it.erstellt })
    val heuteOffen = gefiltert.filter { !it.erledigt && it.tag != null && it.tag <= heute && (it.minuten == null || it.tag < heute) }.sortedWith(tagesOrdnung)
    val heuteTermine = gefiltert.filter { it.tag == heute && it.minuten != null }
    val heuteErledigt = gefiltert.filter { it.erledigt && it.tag == heute && it.minuten == null }
    val morgenOffen = gefiltert.filter { it.tag == heute + 1 && it.minuten == null && !it.erledigt }.sortedWith(tagesOrdnung)
    val morgenTermine = gefiltert.filter { it.tag == heute + 1 && it.minuten != null }
    val nachPrio = Prioritaet.entries.associateWith { p -> gefiltert.filter { !it.erledigt && it.tag == null && it.prio == p }.sortedByDescending { it.erstellt } }
    val demnaechst = gefiltert.filter { !it.erledigt && it.tag != null && it.tag > heute + 1 }.sortedWith(compareBy({ it.tag }, { it.minuten ?: -1 }))
    val erledigt = gefiltert.filter { it.erledigt && it.tag != heute }.sortedByDescending { it.erledigtAm ?: 0 }.take(40)
    val heuteGesamt = alle.count { (it.tag == heute) || (!it.erledigt && it.tag != null && it.tag < heute) }
    val heuteFertig = alle.count { it.tag == heute && it.erledigt }

    /** Tage in Folge (bis heute oder gestern) mit mindestens einer erledigten Aufgabe. */
    val serie: Int = run {
        val zone = ZoneId.systemDefault()
        val tage = alle.mapNotNull { it.erledigtAm }.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay() }.toSet()
        var d = if (heute in tage) heute else heute - 1
        var n = 0
        while (d in tage) { n++; d-- }
        n
    }
}

@Composable
fun ListeBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.aufgaben.collectAsState()
    val heute = vm.heute
    var suche by rememberSaveable { mutableStateOf("") }
    var sucheOffen by rememberSaveable { mutableStateOf(false) }
    val b = remember(alle, heute, suche) { Bereiche(alle, heute, suche) }
    val scroll = rememberScrollState()
    val zustand = remember { ZiehZustand() }
    zustand.beiAblage = { a, z -> vm.verschiebe(a, z) }
    val dichte = LocalDensity.current
    zustand.scrollWert = { scroll.value.toFloat() }
    zustand.linieAbstand = with(dichte) { LINIE_UEBER_KARTE.toPx() }
    zustand.scrolleUm = { scroll.dispatchRawDelta(it) }
    val einstellungenStand by vm.einstellungen.stand.collectAsState()
    val szeneZeigen = remember(einstellungenStand) { vm.einstellungen.szeneZeigen }
    val leisteVon = remember(einstellungenStand) { vm.einstellungen.zeitleisteVon }
    val leisteBis = remember(einstellungenStand) { vm.einstellungen.zeitleisteBis.coerceAtLeast(leisteVon + 60) }
    val leisteAuto = remember(einstellungenStand) { vm.einstellungen.zeitleisteAuto }
    val leisteLuecken = remember(einstellungenStand) { vm.einstellungen.zeitleisteLuecken }
    // Kompakt ⇄ Ganzer Tag, je Tag getrennt; gilt bis zum Neustart der App (danach wieder kompakt).
    var heuteGanz by rememberSaveable(heute) { mutableStateOf(false) }
    var morgenGanz by rememberSaveable(heute) { mutableStateOf(false) }
    zustand.ziehSpanne = leisteVon to leisteBis

    // Randscrollen während des Ziehens, auch bei stillstehendem Finger.
    val zieht = zustand.aufgabe != null
    LaunchedEffect(zieht) {
        if (!zieht) return@LaunchedEffect
        val zone = with(dichte) { 90.dp.toPx() }
        val tempo = with(dichte) { 620.dp.toPx() }
        var letzte = 0L
        while (true) {
            withFrameNanos { n ->
                val dt = if (letzte == 0L) 0f else ((n - letzte) / 1e9f).coerceAtMost(0.032f)
                letzte = n
                val s = staerkeAmRand(zustand.finger.value.y, zustand.scrollOben, zustand.scrollUnten, zone)
                if (s != 0f && dt > 0f) {
                    scroll.dispatchRawDelta(s * tempo * dt)
                    zustand.aktualisiere()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned {
        val r = it.boundsInRoot()
        zustand.ursprung = it.positionInRoot()
        if (!zieht) { zustand.scrollOben = r.top + with(dichte) { 150.dp.toPx() }; zustand.scrollUnten = r.bottom - with(dichte) { 110.dp.toPx() } }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = !zieht)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Kopf(vm, b, sucheOffen, { sucheOffen = !sucheOffen; if (!sucheOffen) suche = "" })
            AnimatedVisibility(sucheOffen, enter = expandVertically(clip = false) + fadeIn(), exit = shrinkVertically(clip = false) + fadeOut()) { Suchfeld(suche) { suche = it } }
            if (szeneZeigen && !sucheOffen) {
                AufgabenSzene(
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp).einblenden()
                        .graphicsLayer {
                            val s = scroll.value.toFloat()
                            translationY = s * 0.35f
                            alpha = (1f - s / 900f).coerceIn(0f, 1f)
                            val k = (1f - s / 3000f).coerceIn(0.9f, 1f)
                            scaleX = k; scaleY = k
                            // Without an offscreen buffer: otherwise the layer clips the soft shadow at the card edge (square grey borders).
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        },
                    erledigtAnteil = if (b.heuteGesamt == 0) 0f else b.heuteFertig / b.heuteGesamt.toFloat(),
                )
            }

            // ---- Heute ----
            Sektion(
                "heute", "Heute", Icons.Rounded.WbSunny, f.primaer, b.heuteOffen.size + b.heuteTermine.count { !it.erledigt }, zustand,
                Ziel.Tag(heute), untertitel = Tage.langesDatum(heute), verzoegerung = 40,
                leer = b.heuteOffen.isEmpty() && b.heuteTermine.isEmpty() && b.heuteErledigt.isEmpty(), automatik = suche.isBlank(),
                aktionen = { VorlesenKnopf(vm, "tag_$heute") { vm.tagVorlesen(heute, b.heuteOffen + b.heuteTermine) } },
            ) {
                if (b.heuteOffen.isEmpty() && b.heuteTermine.isEmpty()) LeerHinweis(Icons.Rounded.WbSunny, "Noch nichts für heute. Halte eine Aufgabe gedrückt und zieh sie hierher – oder direkt auf eine Uhrzeit.")
                b.heuteOffen.forEach { a -> Karte(vm, a, heute, zustand) }
                b.heuteErledigt.forEach { a -> Karte(vm, a, heute, zustand) }
                Zeitleiste(
                    heute, b.heuteTermine, true, zustand, leisteVon, leisteBis, leisteAuto, leisteLuecken, heuteGanz, { heuteGanz = it },
                    { vm.oeffne(it.id) }, { vm.erledigen(it, !it.erledigt) },
                )
            }
            // ---- Morgen ----
            Sektion(
                "morgen", "Morgen", Icons.Rounded.WbTwilight, f.sekundaer, b.morgenOffen.size + b.morgenTermine.size, zustand,
                Ziel.Tag(heute + 1), untertitel = Tage.langesDatum(heute + 1), verzoegerung = 80,
                leer = b.morgenOffen.isEmpty() && b.morgenTermine.isEmpty(), automatik = suche.isBlank(),
                aktionen = { VorlesenKnopf(vm, "tag_${heute + 1}") { vm.tagVorlesen(heute + 1, b.morgenOffen + b.morgenTermine) } },
            ) {
                if (b.morgenOffen.isEmpty() && b.morgenTermine.isEmpty()) LeerHinweis(Icons.Rounded.EventNote, "Plane schon für morgen: Aufgaben hierher ziehen oder auf die Zeitleiste fallen lassen.")
                b.morgenOffen.forEach { a -> Karte(vm, a, heute, zustand) }
                Zeitleiste(
                    heute + 1, b.morgenTermine, false, zustand, leisteVon, leisteBis, leisteAuto, leisteLuecken, morgenGanz, { morgenGanz = it },
                    { vm.oeffne(it.id) }, { vm.erledigen(it, !it.erledigt) },
                )
            }
            // ---- Prioritäten ----
            val info = mapOf(
                Prioritaet.HOCH to (Icons.Rounded.LocalFireDepartment to "Wichtig und dringend"),
                Prioritaet.MITTEL to (Icons.Rounded.Star to "Bald erledigen"),
                Prioritaet.GERING to (Icons.Rounded.Spa to "Wenn Zeit ist"),
                Prioritaet.SPAETER to (Icons.Rounded.Inbox to "Eingang – neue Aufgaben landen hier"),
            )
            Prioritaet.entries.forEachIndexed { i, p ->
                val liste = b.nachPrio[p].orEmpty()
                Sektion(
                    p.name, p.anzeige, info[p]!!.first, if (p == Prioritaet.SPAETER) f.textLeise else f.prio(p), liste.size, zustand, Ziel.Prio(p),
                    untertitel = info[p]!!.second, verzoegerung = 120 + i * 40, leer = liste.isEmpty(), automatik = suche.isBlank(),
                ) {
                    if (liste.isEmpty()) LeerHinweis(if (p == Prioritaet.SPAETER) Icons.Rounded.Mic else Icons.Rounded.SwipeDown, if (p == Prioritaet.SPAETER) "Leer. Tipp aufs Plus und sprich deine nächste Aufgabe ein." else "Nichts hier. Zieh Aufgaben aus „Später“ hierher.")
                    liste.forEach { a -> Karte(vm, a, heute, zustand) }
                }
            }
            if (b.demnaechst.isNotEmpty()) Sektion(
                "demnaechst", "Demnächst", Icons.Rounded.Event, f.tertiaer, b.demnaechst.size, zustand, Ziel.Prio(Prioritaet.SPAETER),
                untertitel = "Nach morgen geplant", verzoegerung = 300,
            ) { b.demnaechst.forEach { a -> Karte(vm, a, heute, zustand, zeigeDatum = true) } }
            if (b.erledigt.isNotEmpty()) Sektion(
                "erledigt", "Erledigt", Icons.Rounded.TaskAlt, f.erfolg, b.erledigt.size, zustand, Ziel.Erledigt,
                untertitel = if (b.serie > 1) "${b.serie} Tage in Folge etwas geschafft" else "Schon geschafft", startOffen = false, verzoegerung = 340,
            ) { b.erledigt.forEach { a -> Karte(vm, a, heute, zustand, zeigeDatum = true) } }
            Spacer(Modifier.height(140.dp))
            Spacer(Modifier.navigationBarsPadding())
        }

        // Kopfleiste, die beim Scrollen zu Glas wird
        val kopfSichtbar by remember { androidx.compose.runtime.derivedStateOf { scroll.value > with(dichte) { 180.dp.toPx() } } }
        AnimatedVisibility(kopfSichtbar && !zieht, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
            Row(
                Modifier.fillMaxWidth().glas(f, radius = 0.dp, erhoeht = 1.2f, fuellung = f.flaecheStark).statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Aufgaben", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${b.heuteFertig}/${b.heuteGesamt} heute", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Schwebendes Plus
        Plus(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 24.dp), sichtbar = !zieht) { vm.neueAufgabe(mitMikro = true) }

        // Zeitlinie und Geisterblock: in derselben Ebene und aus derselben Höhe wie die gezogene Karte (siehe unten).
        if (zieht) ZeitVorschau(zustand)

        // Ablageleisten während des Ziehens
        AnimatedVisibility(zieht, Modifier.align(Alignment.TopCenter), enter = fadeIn(tween(120)) + slideInVertically { -it / 2 }, exit = fadeOut(tween(120))) {
            AblageOben(zustand, heute)
        }
        AnimatedVisibility(zieht, Modifier.align(Alignment.BottomCenter), enter = fadeIn(tween(120)) + slideInVertically { it / 2 }, exit = fadeOut(tween(120))) {
            AblageUnten(zustand)
        }
        // Die gezogene Karte folgt dem Finger — gerade, in Originalgröße, gelesen nur in der Zeichenebene.
        zustand.aufgabe?.let { a ->
            val breite = with(dichte) { zustand.groesse.width.toDp() }
            Box(
                Modifier.width(breite).graphicsLayer {
                    translationX = zustand.finger.value.x - zustand.griff.x - zustand.ursprung.x
                    // Gleiche Quelle wie die Zeitlinie: linieY() + linieAbstand = Kartenoberkante, Linie also genau 1 mm darüber.
                    translationY = zustand.linieY() + zustand.linieAbstand - zustand.ursprung.y
                },
            ) {
                AufgabeKarte(a.copy(minuten = zustand.hoverZeit?.second ?: a.minuten), heute, zustand, schwebend = true, onTipp = {}, onErledigt = {})
            }
            ZeitMarke(zustand)
        }
    }
}

@Composable
private fun Karte(vm: AppViewModel, a: Aufgabe, heute: Long, zustand: ZiehZustand, zeigeDatum: Boolean = false) {
    val hervor = vm.hervorgehoben == a.id
    LaunchedEffect(hervor) { if (hervor) { delay(2200); vm.hervorgehoben = null } }
    androidx.compose.runtime.key(a.id) {
        Box(Modifier.einblenden()) {
            AufgabeKarte(a, heute, zustand, zeigeDatum = zeigeDatum, hervorheben = hervor, onTipp = { vm.oeffne(a.id) }, onErledigt = { vm.erledigen(a, !a.erledigt) })
        }
    }
}

@Composable
private fun VorlesenKnopf(vm: AppViewModel, schluessel: String, aktion: () -> Unit) {
    val f = LocalFarben.current
    val spricht = vm.sprichtId == schluessel
    Box(Modifier.size(40.dp).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(if (spricht) Icons.Rounded.StopCircle else Icons.Rounded.VolumeUp, if (spricht) "Vorlesen stoppen" else "Vorlesen", tint = if (spricht) f.sekundaer else f.textLeise)
    }
}

@Composable
private fun Kopf(vm: AppViewModel, b: Bereiche, sucheOffen: Boolean, sucheUmschalten: () -> Unit) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp), horizontalArrangement = Arrangement.End) {
        RundKnopf(if (sucheOffen) Icons.Rounded.Close else Icons.Rounded.Search, "Suchen", sucheUmschalten)
        RundKnopf(Icons.Rounded.SelfImprovement, "Fokus-Timer") { vm.fokusStarten(null) }
        RundKnopf(Icons.Rounded.Palette, "Design wechseln") {
            val alle = Design.entries
            val neu = alle[(alle.indexOf(Design.von(e.design)) + 1) % alle.size]
            e.design = neu.id
            vm.melde("Design: ${neu.anzeige}")
        }
        // Hell → Automatisch (wie das System) → Dunkel → Hell. Das Symbol zeigt den aktuellen Modus.
        val modus = e.modus
        RundKnopf(
            when (modus) { "hell" -> Icons.Rounded.LightMode; "dunkel" -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto },
            when (modus) { "hell" -> "Hell (weiter zu Automatisch)"; "dunkel" -> "Dunkel (weiter zu Hell)"; else -> "Automatisch (weiter zu Dunkel)" },
        ) {
            val neu = when (modus) { "hell" -> "system"; "system" -> "dunkel"; else -> "hell" }
            e.modus = neu
            vm.melde(when (neu) { "hell" -> "Hell"; "dunkel" -> "Dunkel"; else -> "Automatisch wie das System" })
        }
        RundKnopf(Icons.Rounded.Settings, "Einstellungen") { vm.zeige(Bildschirm.Einstellungen) }
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Fortschritt(b.heuteFertig, b.heuteGesamt)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("Sinnvolle Aufgaben", color = f.text, fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(brush = Brush.linearGradient(listOf(f.text, f.primaer))))
            Text(
                if (b.serie > 1) "${b.serie} Tage in Folge etwas geschafft" else Tage.langesDatum(vm.heute),
                color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun RundKnopf(icon: ImageVector, beschreibung: String, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.padding(4.dp).size(44.dp).glas(f, 99.dp, 0.8f).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = f.text, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Suchfeld(wert: String, aendern: (String) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).glas(f, 99.dp, 0.6f, f.flaecheStark).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Search, null, tint = f.textLeise, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (wert.isEmpty()) Text("Aufgaben durchsuchen…", color = f.textSchwach, fontSize = 16.sp)
            BasicTextField(wert, aendern, singleLine = true, textStyle = TextStyle(color = f.text, fontSize = 16.sp), cursorBrush = SolidColor(f.primaer), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Fortschritt(fertig: Int, gesamt: Int) {
    val f = LocalFarben.current
    val ziel = if (gesamt == 0) 0f else fertig / gesamt.toFloat()
    val anteil by animateFloatAsState(ziel, spring(dampingRatio = 0.7f, stiffness = 120f), label = "fortschritt")
    Box(Modifier.size(62.dp).glas(f, 99.dp, 1.2f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(50.dp)) {
            val w = 6.dp.toPx()
            drawArc(f.textSchwach.copy(alpha = 0.25f), 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
            drawArc(Brush.sweepGradient(listOf(f.primaer, f.sekundaer, f.primaer)), -90f, 360f * anteil, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(if (gesamt == 0) "☀️" else "$fertig/$gesamt", color = f.text, fontSize = if (gesamt == 0) 18.sp else 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Plus(modifier: Modifier, sichtbar: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    val puls = rememberInfiniteTransition(label = "puls")
    val p by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Restart), label = "p")
    val skala by animateFloatAsState(if (sichtbar) 1f else 0f, spring(dampingRatio = 0.6f), label = "plus")
    Box(
        modifier.size(72.dp).graphicsLayer { scaleX = skala; scaleY = skala; alpha = skala; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(f.primaer.copy(alpha = 0.35f * (1f - p)), r * (1f + 0.45f * p))
            }
            .knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel)
            .antippen(aktion = aktion),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, "Neue Aufgabe einsprechen", tint = Color.White, modifier = Modifier.size(38.dp))
    }
}

@Composable
private fun AblageOben(zustand: ZiehZustand, heute: Long) {
    val f = LocalFarben.current
    Column(
        Modifier.fillMaxWidth().glas(f, radius = 0.dp, erhoeht = 2f, fuellung = f.flaecheStark).statusBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = f.textLeise, modifier = Modifier.size(15.dp))
            Text(
                zustand.hoverZeit?.let { "Termin um ${Tage.zeit(it.second)} Uhr · ${Tage.datum(it.first)}" } ?: "Loslassen über einem Ziel · auf der Zeitleiste für eine Uhrzeit",
                color = f.textLeise, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AblageChip(zustand, "chip_heute", Icons.Rounded.WbSunny, "Heute", f.primaer, Ziel.Tag(heute, behalteZeit = true), Modifier.weight(1f))
            AblageChip(zustand, "chip_morgen", Icons.Rounded.WbTwilight, "Morgen", f.primaer, Ziel.Tag(heute + 1, behalteZeit = true), Modifier.weight(1f))
            AblageChip(zustand, "chip_uebermorgen", Icons.Rounded.Event, "Übermorgen", f.primaer, Ziel.Tag(heute + 2, behalteZeit = true), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AblageChip(zustand, "chip_hoch", Icons.Rounded.LocalFireDepartment, "Hoch", f.prio(Prioritaet.HOCH), Ziel.Prio(Prioritaet.HOCH), Modifier.weight(1f))
            AblageChip(zustand, "chip_mittel", Icons.Rounded.Star, "Mittel", f.prio(Prioritaet.MITTEL), Ziel.Prio(Prioritaet.MITTEL), Modifier.weight(1f))
            AblageChip(zustand, "chip_gering", Icons.Rounded.Spa, "Gering", f.prio(Prioritaet.GERING), Ziel.Prio(Prioritaet.GERING), Modifier.weight(1f))
            AblageChip(zustand, "chip_spaeter", Icons.Rounded.Inbox, "Später", f.prio(Prioritaet.SPAETER), Ziel.Prio(Prioritaet.SPAETER), Modifier.weight(1f))
        }
    }
}

@Composable
private fun AblageUnten(zustand: ZiehZustand) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().glas(f, radius = 0.dp, erhoeht = 2f, fuellung = f.flaecheStark).navigationBarsPadding().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AblageChip(zustand, "chip_erledigt", Icons.Rounded.TaskAlt, "Erledigt", f.erfolg, Ziel.Erledigt, Modifier.weight(1f), hoch = 50)
        AblageChip(zustand, "chip_loeschen", Icons.Rounded.DeleteOutline, "Löschen", f.gefahr, Ziel.Loeschen, Modifier.weight(1f), hoch = 50)
    }
}

@Composable
private fun AblageChip(zustand: ZiehZustand, schluessel: String, icon: ImageVector, text: String, farbe: Color, ziel: Ziel, modifier: Modifier, hoch: Int = 46) {
    val f = LocalFarben.current
    val aktiv = zustand.hoverZiel == schluessel
    val skala by animateFloatAsState(if (aktiv) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "chip")
    Row(
        modifier.height(hoch.dp).ablageZiel(zustand, schluessel, ziel)
            .graphicsLayer { scaleX = skala; scaleY = skala }
            .then(if (aktiv) Modifier.knopf3d(farbe, farbe.copy(alpha = 0.75f), 14.dp, f.dunkel) else Modifier.glas(f, 14.dp, 0.5f, farbe.copy(alpha = 0.12f))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (aktiv) Color.White else farbe, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = if (aktiv) Color.White else f.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

internal fun farbeFuerPrio(f: Farben, p: Prioritaet) = f.prio(p)
