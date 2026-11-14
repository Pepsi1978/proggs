package de.frank.aufgaben.ui

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.rounded.Checklist
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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.data.erledigtGrenze
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
class Bereiche(alle: List<Aufgabe>, heute: Long, suche: String, archivTage: Set<Long> = emptySet()) {
    private val gefiltert = if (suche.isBlank()) alle else alle.filter {
        it.titel.contains(suche, true) || it.text.contains(suche, true)
    }
    private val tagesOrdnung = compareBy<Aufgabe>({ it.prio.rang }, { -it.erstellt })
    private val aufHeute = gefiltert.map { it.aufHeute(heute) }
    private val aufMorgen = gefiltert.mapNotNull { it.amTag(heute + 1, heute) }
    val heuteOffen = aufHeute.filter { !it.erledigt && it.tag != null && it.tag <= heute && (it.minuten == null || it.tag < heute) }.sortedWith(tagesOrdnung)
    val heuteTermine = aufHeute.filter { it.tag == heute && it.minuten != null }
    val heuteErledigt = gefiltert.filter { it.erledigt && it.tag == heute && it.minuten == null }
    val morgenOffen = aufMorgen.filter { it.minuten == null && !it.erledigt }.sortedWith(tagesOrdnung)
    val morgenTermine = aufMorgen.filter { it.minuten != null }
    // Aufgaben aus der App „Ziele“ ohne Tag stehen unter „Ziele“ bei ihrem Ziel, nicht in den Prioritäten; tägliche stehen an den Tagen.
    val nachPrio = Prioritaet.entries.associateWith { p -> gefiltert.filter { !it.erledigt && it.tag == null && it.zielId == null && !it.laeuftTaeglich && it.prio == p }.sortedByDescending { it.erstellt } }
    /** Offene Aufgaben je Ziel (auch mit Tag), in der Reihenfolge des Übernehmens. */
    val nachZiel: List<List<Aufgabe>> = gefiltert.filter { !it.erledigt && it.zielId != null }
        .sortedBy { it.erstellt }.groupBy { it.zielId }.values.toList()
    val demnaechst = gefiltert.filter { !it.erledigt && it.tag != null && it.tag > heute + 1 }.sortedWith(compareBy({ it.tag }, { it.minuten ?: -1 }))
    // Nur die letzten 30 Tage; ältere löscht das Aufräumen beim Start und um Mitternacht endgültig.
    private val grenze = erledigtGrenze()
    val erledigt = gefiltert.filter { it.erledigt && it.tag != heute && (it.erledigtAm ?: Long.MAX_VALUE) >= grenze }.sortedByDescending { it.erledigtAm ?: 0 }
    val heuteGesamt = alle.map { it.aufHeute(heute) }.count { (it.tag == heute) || (!it.erledigt && it.tag != null && it.tag < heute) }
    val heuteFertig = alle.count { it.tag == heute && it.erledigt }

    /** Tage in Folge (bis heute oder gestern) mit mindestens einer erledigten Aufgabe. */
    val serie: Int = run {
        val zone = ZoneId.systemDefault()
        // Dazu die Tage bereits gelöschter erledigter Aufgaben, sonst endete die Serie nach 30 Tagen.
        val tage = alle.mapNotNull { it.erledigtAm }.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay() }.toSet() + archivTage
        var d = if (heute in tage) heute else heute - 1
        var n = 0
        while (d in tage) { n++; d-- }
        n
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ListeBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.aufgaben.collectAsState()
    val heute = vm.heute
    var suche by rememberSaveable { mutableStateOf("") }
    var sucheOffen by rememberSaveable { mutableStateOf(false) }
    // Auf-/Zuklapp-Wahl für „Erledigt“ hier gehalten, weil der Bereich bei leerer Liste ganz verschwindet.
    val erledigtWahl = rememberSaveable { mutableIntStateOf(0) }
    val b = remember(alle, heute, suche) { Bereiche(alle, heute, suche, vm.archivierteSerieTage()) }
    val scroll = rememberScrollState()
    // Erledigt zeigt anfangs nur die neuesten Einträge; nahe am Listenende lädt er automatisch weitere nach.
    var erledigtZeigen by rememberSaveable { mutableIntStateOf(ERLEDIGT_STUECK) }
    // Offen wie in Sektion (startZu): selbst aufgeklappt oder während der Suche (mit Treffern, sonst fehlt der Bereich).
    val erledigtOffen = erledigtWahl.intValue == 1 || (erledigtWahl.intValue == 0 && suche.isNotBlank())
    val erledigtAnzahl by rememberUpdatedState(b.erledigt.size)
    val erledigtOffenAktuell by rememberUpdatedState(erledigtOffen)
    LaunchedEffect(erledigtOffen) { if (!erledigtOffen) erledigtZeigen = ERLEDIGT_STUECK }
    LaunchedEffect(scroll) {
        androidx.compose.runtime.snapshotFlow { Triple(scroll.maxValue - scroll.value, erledigtZeigen, erledigtOffenAktuell) }.collect { (rest, zeigen, offen) ->
            // Passt der Rest schon auf den Bildschirm, kommt gleich das nächste Stück (maxValue ändert sich mit dem neuen Inhalt).
            if (offen && rest < NACHLADEN_PX && zeigen < erledigtAnzahl) erledigtZeigen = zeigen + ERLEDIGT_STUECK
        }
    }
    // Von Jarvis gewünscht: Bereich Heute bzw. Morgen oben unter der Statusleiste. Erst scrollen, wenn die Liste steht
    // (Daten kommen beim Kaltstart erst nach ein paar Frames, Bereiche blenden ein); sonst landet man daneben.
    val bereichY = remember { HashMap<String, Float>() }
    val statusHoehe = WindowInsets.statusBarsIgnoringVisibility.getTop(LocalDensity.current)
    LaunchedEffect(vm.bereichZiel) {
        val (bereich, _) = vm.bereichZiel ?: return@LaunchedEffect
        var letzte = -1f
        var ruhig = 0
        val bis = System.currentTimeMillis() + 1_500
        while (ruhig < 8 && System.currentTimeMillis() < bis) {
            withFrameNanos { }
            val y = bereichY[bereich] ?: continue
            if (y == letzte) ruhig++ else { ruhig = 0; letzte = y }
        }
        bereichY[bereich]?.let { scroll.animateScrollTo((it - statusHoehe).toInt().coerceAtLeast(0)) }
        vm.bereichZiel = null
    }
    // Ganz aus dem Bild gescrollt: Die Szene hält an, statt unsichtbar weiterzuzeichnen.
    val szeneWeg by remember { androidx.compose.runtime.derivedStateOf { scroll.value > 900 } }
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
    val statusOben = WindowInsets.statusBarsIgnoringVisibility.getTop(dichte)
    val navUnten = WindowInsets.navigationBarsIgnoringVisibility.getBottom(dichte)
    LaunchedEffect(zieht) {
        if (!zieht) return@LaunchedEffect
        // Zone innerhalb der Ablageleisten, in der das Scrollen sanft beginnt; zur Bildschirmkante hin wird es stetig
        // schneller bis zum Höchsttempo direkt an der Kante (auch über den Leisten, dort nie Stillstand).
        val zoneMax = with(dichte) { 120.dp.toPx() }
        // Ruhige Mitte, die auch bei kleinem Fenster (Querformat, geteilter Bildschirm) frei vom Scrollen bleibt.
        val mitteMin = with(dichte) { 80.dp.toPx() }
        val tempo = with(dichte) { 1000.dp.toPx() }
        // Kleiner Abstand zur Kante (über Status- und Navigationsleiste), damit das Höchsttempo erreichbar ist.
        val kantenRand = with(dichte) { 14.dp.toPx() }
        var letzte = 0L
        var v = 0f // geglättetes Tempo in px/s
        while (true) {
            withFrameNanos { n ->
                // Nach dem Loslassen keinen Rest-Schritt mehr scrollen, bevor der Effekt beendet wird.
                if (zustand.aufgabe == null) { v = 0f; return@withFrameNanos }
                val dt = if (letzte == 0L) 0f else ((n - letzte) / 1e9f).coerceAtMost(0.032f)
                letzte = n
                if (dt <= 0f) return@withFrameNanos
                val zone = min(zoneMax, ((zustand.scrollUnten - zustand.scrollOben - mitteMin) / 2f).coerceAtLeast(0f))
                val s = staerkeAmRand(
                    zustand.finger.value.y,
                    zustand.kanteOben + kantenRand, zustand.scrollOben + zone,
                    zustand.scrollUnten - zone, zustand.kanteUnten - kantenRand,
                )
                val ziel = s * tempo
                // Weich beschleunigen (~90 ms), aber schnell abbremsen und umkehren (~35 ms): Bleibt der Finger stehen,
                // läuft die Liste nicht unter ihm weiter.
                val bremst = ziel * v < 0f || abs(ziel) < abs(v)
                v += (ziel - v) * (1f - exp(-dt / if (bremst) 0.035f else 0.09f))
                if (s == 0f && abs(v) < 20f) v = 0f
                // Finger in der Randzone: Das automatische Aufklappen eines Bereichs wartet, bis er wieder ruhig steht.
                zustand.randScrollt = s != 0f
                if (v != 0f) {
                    val bewegt = scroll.dispatchRawDelta(v * dt)
                    if (bewegt == 0f) v = 0f // Listenende erreicht: kein Leerlauf bei jedem Frame
                    else zustand.aktualisiere()
                }
            }
        }
    }

    // Nach dem Ablegen auf einer Zeitleiste: Der Termin bleibt genau dort auf dem Bildschirm, wo man ihn hat fallen lassen.
    // Die Leisten rücken danach wieder zusammen und die Datenbank liefert den Termin erst ein paar Frames später an seinem
    // neuen Platz; solange gleicht die Liste jede Verschiebung aus, bis alles ruht (oder man selbst scrollt).
    LaunchedEffect(zustand.ablageNr) {
        val z = zustand.ablage ?: return@LaunchedEffect
        val start = System.currentTimeMillis()
        var letzteKorrektur = 0L
        while (zustand.ablage === z && zustand.aufgabe == null && !scroll.isScrollInProgress) {
            withFrameNanos { }
            val nun = System.currentTimeMillis()
            val y = z.istY
            if (y != null) {
                z.istY = null
                val d = y - z.zielY
                if (kotlin.math.abs(d) > 0.5f) { scroll.dispatchRawDelta(d); letzteKorrektur = nun }
                else if (letzteKorrektur == 0L) letzteKorrektur = nun
            }
            if (nun - start > 3000 || (letzteKorrektur != 0L && nun - letzteKorrektur > 450)) break
        }
        if (zustand.ablage === z) zustand.ablage = null
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned {
        val r = it.boundsInRoot()
        zustand.ursprung = it.positionInRoot()
        if (!zieht) { zustand.kanteOben = r.top + statusOben; zustand.kanteUnten = r.bottom - navUnten; zustand.scrollOben = r.top + with(dichte) { 150.dp.toPx() }; zustand.scrollUnten = r.bottom - with(dichte) { 110.dp.toPx() } }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll, enabled = !zieht)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBarsIgnoringVisibility))
            Kopf(vm, b, sucheOffen, { sucheOffen = !sucheOffen; if (!sucheOffen) suche = "" })
            AnimatedVisibility(sucheOffen, enter = expandVertically(clip = false) + fadeIn(), exit = shrinkVertically(clip = false) + fadeOut()) { Suchfeld(suche) { suche = it } }
            if (szeneZeigen && !sucheOffen) {
                AufgabenSzene(
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp).einblenden(dauer = 750)
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
                    pausiert = szeneWeg,
                )
            }

            // ---- Heute ----
            Spacer(Modifier.onGloballyPositioned { bereichY["heute"] = it.positionInParent().y })
            Sektion(
                "heute", "Heute", Icons.Rounded.WbSunny, f.primaer, b.heuteOffen.size + b.heuteTermine.count { !it.erledigt }, zustand,
                Ziel.Tag(heute), untertitel = Tage.langesDatum(heute), verzoegerung = 40, einblendDauer = 750, leuchten = true, dunklesBand = true,
                // Wie alle Bereiche von Anfang an offen, auch leer (dann mit Hinweis und Zeitleiste);
                // nur ein leeres Suchergebnis klappt ihn zu.
                leer = suche.isNotBlank() && b.heuteOffen.isEmpty() && b.heuteTermine.isEmpty() && b.heuteErledigt.isEmpty(), automatik = suche.isBlank(),
                aktionen = { KalenderKnopf(vm); VorlesenKnopf(vm, "tag_$heute") { vm.tagVorlesen(heute, b.heuteOffen + b.heuteTermine) } },
            ) {
                if (b.heuteOffen.isEmpty() && b.heuteTermine.isEmpty()) LeerHinweis(Icons.Rounded.WbSunny, "Noch nichts für heute. Halte eine Aufgabe gedrückt und zieh sie hierher – oder direkt auf eine Uhrzeit.")
                b.heuteOffen.forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand) } }
                b.heuteErledigt.forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand) } }
                Zeitleiste(
                    heute, b.heuteTermine, true, zustand, leisteVon, leisteBis, leisteAuto, leisteLuecken, heuteGanz, { heuteGanz = it },
                    { vm.oeffne(it.id) }, { vm.erledigen(it, !it.erledigt) },
                )
            }
            // ---- Morgen ----
            Spacer(Modifier.onGloballyPositioned { bereichY["morgen"] = it.positionInParent().y })
            Sektion(
                "morgen", "Morgen", Icons.Rounded.WbTwilight, f.sekundaer, b.morgenOffen.size + b.morgenTermine.size, zustand,
                Ziel.Tag(heute + 1), untertitel = Tage.langesDatum(heute + 1), verzoegerung = 80, leuchten = true,
                leer = b.morgenOffen.isEmpty() && b.morgenTermine.isEmpty(), automatik = suche.isBlank(),
                aktionen = { KalenderKnopf(vm); VorlesenKnopf(vm, "tag_${heute + 1}") { vm.tagVorlesen(heute + 1, b.morgenOffen + b.morgenTermine) } },
            ) {
                if (b.morgenOffen.isEmpty() && b.morgenTermine.isEmpty()) LeerHinweis(Icons.Rounded.EventNote, "Plane schon für morgen: Aufgaben hierher ziehen oder auf die Zeitleiste fallen lassen.")
                b.morgenOffen.forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand) } }
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
                val farbe = if (p == Prioritaet.SPAETER) f.textLeise else f.prio(p)
                Sektion(
                    p.name, p.anzeige, info[p]!!.first, farbe, liste.size, zustand, Ziel.Prio(p),
                    untertitel = info[p]!!.second, verzoegerung = 120 + i * 40, leer = liste.isEmpty(), automatik = suche.isBlank(),
                    kraeftig = p != Prioritaet.SPAETER,
                    aktionen = { KalenderKnopf(vm); VorlesenKnopf(vm, "bereich_${p.name}") { vm.bereichVorlesen(p.name, p.anzeige, liste) } },
                ) {
                    if (liste.isEmpty()) LeerHinweis(if (p == Prioritaet.SPAETER) Icons.Rounded.Mic else Icons.Rounded.SwipeDown, if (p == Prioritaet.SPAETER) "Leer. Tipp aufs Plus und sprich deine nächste Aufgabe ein." else "Nichts hier. Zieh Aufgaben aus „Später“ hierher.")
                    liste.forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand) } }
                }
            }
            // ---- Ziele: je Ziel ein Knopf, darunter eingerückt seine Aufgaben ----
            val zielAufgaben = b.nachZiel.flatten()
            Sektion(
                "ziele", "Ziele", Icons.Rounded.Flag, ZIELE_BLAU, b.nachZiel.size, zustand, Ziel.Prio(Prioritaet.SPAETER),
                untertitel = "Aufgaben aus deinen Zielen", verzoegerung = 280, leer = b.nachZiel.isEmpty(), automatik = suche.isBlank(),
                aktionen = { VorlesenKnopf(vm, "bereich_ziele") { vm.bereichVorlesen("ziele", "Ziele", zielAufgaben, mitDatum = true) } },
            ) {
                if (b.nachZiel.isEmpty()) LeerHinweis(Icons.Rounded.Flag, "Noch keine Aufgaben aus der App Ziele. Übernimm dort eine Aufgabe mit dem Plus.")
                b.nachZiel.forEach { liste -> androidx.compose.runtime.key(liste.first().zielId) { ZielGruppe(vm, liste, heute, zustand) } }
            }
            if (b.demnaechst.isNotEmpty()) Sektion(
                "demnaechst", "Demnächst", Icons.Rounded.Event, f.tertiaer, b.demnaechst.size, zustand, Ziel.Prio(Prioritaet.SPAETER),
                untertitel = "Nach morgen geplant", verzoegerung = 300, automatik = suche.isBlank(),
                aktionen = { KalenderKnopf(vm); VorlesenKnopf(vm, "bereich_demnaechst") { vm.bereichVorlesen("demnaechst", "Demnächst", b.demnaechst, mitDatum = true) } },
            ) { b.demnaechst.forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand, zeigeDatum = true) } } }
            if (b.erledigt.isNotEmpty()) Sektion(
                "erledigt", "Erledigt", Icons.Rounded.TaskAlt, f.erfolg, b.erledigt.size, zustand, Ziel.Erledigt,
                untertitel = if (b.serie > 1) "${b.serie} Tage in Folge etwas geschafft" else "Schon geschafft", verzoegerung = 340, automatik = suche.isBlank(), startZu = true, wahlZustand = erledigtWahl,
                aktionen = { KalenderKnopf(vm); VorlesenKnopf(vm, "bereich_erledigt") { vm.bereichVorlesen("erledigt", "Erledigt", b.erledigt) } },
            ) {
                // Stückweise: Erst die neuesten Einträge, weitere kommen beim Scrollen zum Listenende von selbst dazu.
                // So bleibt der Bereich auch mit Hunderten erledigter Aufgaben beim Aufklappen und Scrollen flüssig.
                b.erledigt.take(erledigtZeigen).forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand, zeigeDatum = true) } }
            }
            Spacer(Modifier.height(140.dp))
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility))
        }

        // Keine Kopfleiste beim Scrollen: Die Aufgaben nutzen die ganze Höhe (Hell/Dunkel und Vorlesen stehen oben in der Liste).

        // Schwebendes Plus
        // Links daneben der Fokus-Knopf (früher oben in der Kopfzeile), ohne Leuchtring.
        Row(
            Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility).padding(end = 22.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            FokusUnten(sichtbar = !zieht) { vm.fokusStarten(null) }
            Plus(Modifier, sichtbar = !zieht) { vm.neueAufgabe(mitMikro = true) }
        }

        // Zeitlinie und Geisterblock: in derselben Ebene und aus derselben Höhe wie die gezogene Karte (siehe unten).
        if (zieht) ZeitVorschau(zustand)

        // Ablageleisten während des Ziehens
        AnimatedVisibility(zieht, Modifier.align(Alignment.TopCenter), enter = fadeIn(tween(120)) + slideInVertically { -it / 2 }, exit = fadeOut(tween(120))) {
            AblageOben(zustand, heute)
        }
        AnimatedVisibility(zieht, Modifier.align(Alignment.BottomCenter), enter = fadeIn(tween(120)) + slideInVertically { it / 2 }, exit = fadeOut(tween(120))) {
            AblageUnten(zustand)
        }
        SchwebendeKarte(zustand, heute)
    }
}

/**
 * Die gezogene Karte folgt dem Finger — gerade, in Originalgröße, gelesen nur in der Zeichenebene. Eigene Composable,
 * damit der Wechsel Karte/Blase an der oberen Leiste nur diesen Teil neu aufbaut, nicht den ganzen Listenbildschirm.
 */
@Composable
private fun SchwebendeKarte(zustand: ZiehZustand, heute: Long) {
    val f = LocalFarben.current
    val dichte = LocalDensity.current
        zustand.aufgabe?.let { a ->
            val breite = with(dichte) { zustand.groesse.width.toDp() }
            // In der oberen Leiste: Statt der großen Karte folgt eine kleine Blase mit Aufgabensymbol dem Daumen.
            if (zustand.inObenLeiste) {
                val farbe = if (a.prio == Prioritaet.SPAETER) f.primaer else f.prio(a.prio)
                val schirmBreite = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
                Box(
                    Modifier.size(BLASE).graphicsLayer {
                        // Gut 0,5 cm neben dem Daumenrand, schräg links oben (rechter Daumen kommt von rechts unten), damit man
                        // sieht, dass die Aufgabe noch gehalten wird. Am linken Rand weicht sie nach rechts oben aus, oben zur Seite.
                        val r = BLASE.toPx() / 2
                        val abstand = BLASE_ABSTAND.toPx()
                        val rand = 4.dp.toPx()
                        val fx = zustand.finger.value.x - zustand.ursprung.x
                        val fy = zustand.finger.value.y - zustand.ursprung.y
                        val schraeg = abstand * 0.7071f
                        val dy = maxOf(-schraeg, -(fy - r - rand))
                        val dxBetrag = kotlin.math.sqrt(maxOf(schraeg * schraeg, abstand * abstand - dy * dy))
                        val links = fx - dxBetrag
                        val x = if (links - r >= rand) links else fx + dxBetrag
                        translationX = x.coerceIn(r + rand, schirmBreite.toPx() - r - rand) - r
                        translationY = fy + dy - r
                    }.knopf3d(farbe, farbe.copy(alpha = 0.8f), 99.dp, f.dunkel),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Checklist, a.titel, tint = aufFarbe(farbe), modifier = Modifier.size(22.dp)) }
            } else Box(
                Modifier.width(breite).graphicsLayer {
                    translationX = zustand.finger.value.x - zustand.griff.x - zustand.ursprung.x
                    // Gleiche Quelle wie die Zeitlinie: linieY() + linieAbstand = Kartenoberkante, Linie also genau 1 mm darüber.
                    translationY = zustand.linieY() + zustand.linieAbstand - zustand.ursprung.y
                },
            ) {
                AufgabeKarte(a.copy(minuten = zustand.hoverZeit?.second ?: a.minuten), heute, zustand, schwebend = true, onTipp = {}, onErledigt = {})
            }
            if (!zustand.inObenLeiste) ZeitMarke(zustand)
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

/** Ein Ziel als Knopf; antippen klappt seine Aufgaben eingerückt darunter auf. Ziehen und Bearbeiten wie überall. */
@Composable
private fun ZielGruppe(vm: AppViewModel, liste: List<Aufgabe>, heute: Long, zustand: ZiehZustand) {
    val f = LocalFarben.current
    val erste = liste.first()
    var offen by rememberSaveable(erste.zielId) { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().glas(f, 16.dp, 0.9f, ZIELE_BLAU.copy(alpha = 0.14f)).antippen(haptik = false) { offen = !offen }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(erste.zielSymbol.ifBlank { "🎯" }, fontSize = 20.sp)
            Text(
                erste.zielTitel.ifBlank { "Ziel" }, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Box(Modifier.glas(f, 99.dp, 0f, ZIELE_BLAU.copy(alpha = 0.18f), rand = false).padding(horizontal = 8.dp, vertical = 1.dp)) {
                Text("${liste.size}", color = ZIELE_BLAU, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (offen) "Zuklappen" else "Aufklappen", tint = f.textLeise, modifier = Modifier.padding(start = 6.dp))
        }
        if (offen) Column(Modifier.fillMaxWidth().padding(start = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            liste.map { it.aufHeute(heute) }.forEach { a -> androidx.compose.runtime.key(a.id) { Karte(vm, a, heute, zustand, zeigeDatum = a.tag != null) } }
        }
    }
}

/** Öffnet den Vier-Wochen-Kalender; sitzt in jedem Bereich direkt vor dem Lautsprecher. */
@Composable
private fun KalenderKnopf(vm: AppViewModel) {
    val f = LocalFarben.current
    // Gleiches Grau wie der Lautsprecher und ohne Umrandung, damit beide Knöpfe in jedem Design einheitlich wirken.
    Box(Modifier.size(40.dp).antippen { vm.kalenderOeffnen() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.CalendarMonth, "Kalender öffnen", tint = f.textLeise)
    }
}

@Composable
internal fun VorlesenKnopf(vm: AppViewModel, schluessel: String, aktion: () -> Unit) {
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
    // Ganz oben in einer Zeile: links der Titel in zwei Zeilen („Geniale“ / „Aufgaben“), dahinter alle sechs Knöpfe
    // gleich groß, rechtsbündig zur rechten Kante der Szene (14 dp Rand). Auf schmalen Bildschirmen schrumpfen die Knöpfe.
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp)) {
        val titelBreite = 98.dp
        val g = ((maxWidth - titelBreite - 6.dp * 6) / 6).coerceIn(28.dp, 44.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            // „Geniale“ klein, „Aufgaben“ groß: Das Hauptwort der App trägt den Titel.
            Column(Modifier.weight(1f).padding(start = 2.dp)) {
                Text("Geniale", color = f.textLeise, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                Text(
                    "Aufgaben", color = f.text, fontSize = 20.sp, lineHeight = 22.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(brush = Brush.linearGradient(listOf(f.text, f.primaer))),
                )
            }
            Fortschritt(b.heuteFertig, b.heuteGesamt, g)
            RundKnopf(if (sucheOffen) Icons.Rounded.Close else Icons.Rounded.Search, "Suchen", g, sucheUmschalten)
            RundKnopf(Icons.Rounded.Palette, "Design wechseln", g) {
                val alle = Design.entries
                val neu = alle[(alle.indexOf(Design.von(e.design)) + 1) % alle.size]
                e.design = neu.id
                vm.melde("Design: ${neu.anzeige}")
            }
            ModusKnopf(vm, g)
            // Liest alle offenen Aufgaben nacheinander mit allem vor, was in ihnen steht.
            RundKnopf(if (vm.sprichtId == "alle") Icons.Rounded.StopCircle else Icons.Rounded.VolumeUp, if (vm.sprichtId == "alle") "Vorlesen stoppen" else "Alle Aufgaben vorlesen", g) { vm.alleVorlesen(b) }
            RundKnopf(Icons.Rounded.Settings, "Einstellungen", g) { vm.zeige(Bildschirm.Einstellungen) }
        }
    }
    // Darunter rechtsbündig die Serie bzw. das Datum.
    Text(
        if (b.serie > 1) "${b.serie} Tage in Folge etwas geschafft" else Tage.langesDatum(vm.heute),
        color = f.textLeise, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, textAlign = TextAlign.End,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
    )
}

/** Hell → Automatisch (nach der Sonne) → Dunkel → Hell. Das Symbol zeigt den aktuellen Modus. Sitzt oben auf jedem Bildschirm. */
@Composable
fun ModusKnopf(vm: AppViewModel, groesse: Dp? = null) {
    val e = vm.einstellungen
    val modus = e.modus
    val icon = when (modus) { "hell" -> Icons.Rounded.LightMode; "dunkel" -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto }
    val text = when (modus) { "hell" -> "Hell (weiter zu Automatisch)"; "dunkel" -> "Dunkel (weiter zu Hell)"; else -> "Automatisch (weiter zu Dunkel)" }
    val aktion = {
        val neu = when (modus) { "hell" -> "system"; "system" -> "dunkel"; else -> "hell" }
        e.modus = neu
        vm.melde(when (neu) { "hell" -> "Hell"; "dunkel" -> "Dunkel"; else -> "Automatisch nach der Sonne" })
    }
    // Ohne Größe der Standardknopf der Unterseiten; mit Größe genau so groß wie die übrigen Kopfknöpfe (auch bei 44 dp).
    if (groesse == null) RundKnopf(icon, text, aktion) else RundKnopf(icon, text, groesse, aktion)
}

/** Runder Glas-Knopf der Kopfzeile in frei wählbarer Größe; das Symbol wächst mit. */
@Composable
private fun RundKnopf(icon: ImageVector, beschreibung: String, groesse: Dp, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(Modifier.size(groesse).glas(f, 99.dp, 1.2f).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = f.text, modifier = Modifier.size(groesse * 0.46f))
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
private fun Fortschritt(fertig: Int, gesamt: Int, groesse: Dp) {
    val f = LocalFarben.current
    val ziel = if (gesamt == 0) 0f else fertig / gesamt.toFloat()
    val anteil by animateFloatAsState(ziel, spring(dampingRatio = 0.7f, stiffness = 120f), label = "fortschritt")
    Box(Modifier.size(groesse).glas(f, 99.dp, 1.2f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(groesse * 0.81f)) {
            val w = (groesse * 0.095f).toPx()
            drawArc(f.textSchwach.copy(alpha = 0.25f), 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
            drawArc(Brush.sweepGradient(listOf(f.primaer, f.sekundaer, f.primaer)), -90f, 360f * anteil, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(if (gesamt == 0) "☀️" else "$fertig/$gesamt", color = f.text, fontSize = if (gesamt == 0) 18.sp else if (groesse < 56.dp) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Plus(modifier: Modifier, sichtbar: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    val puls = rememberInfiniteTransition(label = "puls")
    val p by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Restart), label = "p")
    val skala by animateFloatAsState(if (sichtbar) 1f else 0f, spring(dampingRatio = 0.6f), label = "plus")
    Box(
        modifier.size(60.dp).graphicsLayer { scaleX = skala; scaleY = skala; alpha = skala; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(f.primaer.copy(alpha = 0.35f * (1f - p)), r * (1f + 0.45f * p))
            }
            .knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel)
            .antippen(aktion = aktion),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, "Neue Aufgabe einsprechen", tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

/** Fokus-Timer unten links neben dem Plus: farbig gefüllt wie bisher, aber ohne Leuchtring. */
@Composable
private fun FokusUnten(sichtbar: Boolean, aktion: () -> Unit) {
    val f = LocalFarben.current
    val skala by animateFloatAsState(if (sichtbar) 1f else 0f, spring(dampingRatio = 0.6f), label = "fokus")
    Box(
        Modifier.size(52.dp).graphicsLayer { scaleX = skala; scaleY = skala; alpha = skala; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel)
            .antippen(aktion = aktion),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.SelfImprovement, "Fokus-Timer", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AblageOben(zustand: ZiehZustand, heute: Long) {
    val f = LocalFarben.current
    // Vollständig deckend: Darunter scheint die Liste nicht mehr durch.
    // Gleicher deckender Grund wie Heute und Morgen (zentral in Farben); das Glas liegt wie bisher darüber.
    Column(
        Modifier.fillMaxWidth()
            .onGloballyPositioned { zustand.meldeObenLeiste(it.boundsInRoot()) }
            .background(f.flaecheDeckend)
            .glas(f, radius = 0.dp, erhoeht = 2f, fuellung = f.flaecheStark).windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility).padding(horizontal = 10.dp, vertical = 10.dp),
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AblageUnten(zustand: ZiehZustand) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().glas(f, radius = 0.dp, erhoeht = 2f, fuellung = f.flaecheStark).windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility).padding(10.dp),
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
    // Beim Eintritt einmal kurz vibrieren, nicht fortlaufend während des Verweilens.
    val haptik = LocalHapticFeedback.current
    LaunchedEffect(aktiv) { if (aktiv && zustand.aufgabe != null) haptik.performHapticFeedback(HapticFeedbackType.LongPress) }
    val skala by animateFloatAsState(if (aktiv) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "chip")
    Row(
        modifier.height(hoch.dp).ablageZiel(zustand, schluessel, ziel)
            .graphicsLayer { scaleX = skala; scaleY = skala }
            .then(if (aktiv) Modifier.knopf3d(farbe, farbe.copy(alpha = 0.75f), 14.dp, f.dunkel) else Modifier.glas(f, 14.dp, 0.5f, farbe.copy(alpha = 0.12f))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Auf hellen Farben (Gelb) dunkle Schrift, sonst weiße.
        val auf = aufFarbe(farbe)
        Icon(icon, null, tint = if (aktiv) auf else farbe, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = if (aktiv) auf else f.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

internal fun farbeFuerPrio(f: Farben, p: Prioritaet) = f.prio(p)

/** Klares Blau für den Bereich „Ziele“, unabhängig vom Design. */
private val ZIELE_BLAU = Color(0xFF2F7DF6)

/** Größe der Blase, die in der oberen Ablageleiste statt der Karte dem Daumen folgt. */
private val BLASE = 44.dp

/** Abstand Blasenmitte zum Berührungspunkt: halbe Daumenbreite (~0,8 cm) + 0,5 cm Luft + halbe Blase. */
private val BLASE_ABSTAND = 108.dp

/** So viele erledigte Aufgaben kommen je Stück dazu. */
private const val ERLEDIGT_STUECK = 30

/** Ab diesem Abstand zum Listenende (in Pixeln) wird das nächste Stück erledigter Aufgaben gezeigt. */
private const val NACHLADEN_PX = 2500
