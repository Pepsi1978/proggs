package de.frank.jarvis.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.mentalitaet.MAX_PAUSE_SEKUNDEN
import de.frank.jarvis.mentalitaet.MAX_WIEDERHOLUNGEN
import de.frank.jarvis.mentalitaet.MIN_PAUSE_SEKUNDEN
import de.frank.jarvis.mentalitaet.MIN_WIEDERHOLUNGEN
import de.frank.jarvis.mentalitaet.MentalPhase
import de.frank.jarvis.mentalitaet.MentalSatz
import de.frank.jarvis.mentalitaet.MentalitaetSpeicher
import de.frank.jarvis.mentalitaet.MentalitaetSpieler
import de.frank.jarvis.mentalitaet.STANDARD_PAUSE_SEKUNDEN
import de.frank.jarvis.mentalitaet.ordneNachIds
import de.frank.jarvis.ui.theme.LocalBewegung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import de.frank.module.draganddrop.ReorderAutoScroll
import de.frank.module.draganddrop.rememberReorderState
import de.frank.module.draganddrop.reorderHandle
import de.frank.module.draganddrop.reorderItem
import de.frank.module.draganddrop.reorderViewport
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MENTAL_AKZENT = Color(0xFF7A8CFF)
private val MENTAL_AKZENT_HELL = Color(0xFFB98CFF)

/**
 * Vollbildansicht „Mentalität“ (auf Handy und Tablet, ohne untere Leiste):
 * - Oben eine scrollbare, per Drag-and-Drop sortierbare Liste persönlicher mentaler Sätze.
 * - Rechts neben jedem Satz nur seine Wiederholungszahl (1–10); Tipp auf die Zahl öffnet den Schieberegler.
 * - Unten rechts Plus zum Einsprechen eines neuen Satzes (mit Jarvis' Transkriptionsmodell),
 *   links daneben der Lautsprecher zum Starten und sofortigen Stoppen der Endlos-Vorleseschleife
 *   und direkt links neben dem Lautsprecher das Zahnrad für die Einstellungen (Pause zwischen Sätzen).
 */
@Composable
fun MentalitaetBildschirm(vm: AppViewModel, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val context = LocalContext.current
    val appContext = context.applicationContext
    val haptik = LocalHapticFeedback.current
    val reduziert = !LocalBewegung.current

    val einstellungen = remember(appContext) { Einstellungen.get(appContext) }
    val pauseSekunden = einstellungen.mentalitaetPauseSekunden
    val saetze by MentalitaetSpeicher.stand.collectAsState()
    val speicherHinweis by MentalitaetSpeicher.hinweis.collectAsState()
    val spielStand by MentalitaetSpieler.stand.collectAsState()

    var reglerSatzId by remember { mutableStateOf<Long?>(null) }
    var bearbeiteSatzId by remember { mutableStateOf<Long?>(null) }
    var einstellungenOffen by remember { mutableStateOf(false) }
    var ziehReihe by remember { mutableStateOf<List<Long>?>(null) }

    LaunchedEffect(Unit) { MentalitaetSpeicher.sicher(appContext) }
    LaunchedEffect(spielStand.fehler) {
        val fehler = spielStand.fehler ?: return@LaunchedEffect
        vm.meldung = fehler
        MentalitaetSpieler.fehlerGelesen()
    }
    DisposableEffect(Unit) {
        onDispose { vm.mentalitaetAufnahmeBeenden() }
    }

    val gezeigt = remember(saetze, ziehReihe) {
        ziehReihe?.let { ordneNachIds(saetze, it) } ?: saetze
    }
    val alleIds = remember(gezeigt) { gezeigt.map { it.id } }
    fun aktuelleIds(): List<Long> = (ziehReihe?.let { ordneNachIds(saetze, it) } ?: saetze).map { it.id }
    val reglerSatz = remember(saetze, reglerSatzId) {
        reglerSatzId?.let { id -> saetze.firstOrNull { it.id == id } }
    }
    val bearbeiteSatz = remember(saetze, bearbeiteSatzId) {
        bearbeiteSatzId?.let { id -> saetze.firstOrNull { it.id == id } }
    }
    LaunchedEffect(reglerSatzId, reglerSatz) {
        if (reglerSatzId != null && reglerSatz == null) reglerSatzId = null
    }
    LaunchedEffect(bearbeiteSatzId, bearbeiteSatz) {
        if (bearbeiteSatzId != null && bearbeiteSatz == null) bearbeiteSatzId = null
    }

    fun schliessen() {
        vm.mentalitaetAufnahmeBeenden()
        ToolsAnsicht.tool = null
    }

    fun speichereReihenfolge(ids: List<Long>) {
        ziehReihe = ids
        ToolsAnsicht.hintergrund.launch {
            MentalitaetSpeicher.reihenfolge(appContext, ids)
            if (ziehReihe === ids) ziehReihe = null
        }
    }

    BackHandler {
        when {
            einstellungenOffen -> einstellungenOffen = false
            reglerSatzId != null -> reglerSatzId = null
            bearbeiteSatzId != null -> bearbeiteSatzId = null
            else -> schliessen()
        }
    }

    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState, "mentalitaet")
    ReorderAutoScroll(reorder)
    LaunchedEffect(reorder.dragging) {
        if (reorder.dragging) haptik.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    var vorherAnzahl by remember { mutableIntStateOf(saetze.size) }
    LaunchedEffect(saetze.size) {
        if (vorherAnzahl > 0 && saetze.size > vorherAnzahl) {
            listState.animateScrollToItem(saetze.lastIndex)
        }
        vorherAnzahl = saetze.size
    }

    val nimmtAuf = vm.mentalitaetNimmtAuf
    val schreibtMit = vm.mentalitaetSchreibtMit
    val holtMikro = vm.mentalitaetHoltMikro
    val diktatAktiv = nimmtAuf || schreibtMit || holtMikro

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // ---- Kopfzeile
                Row(
                    Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).antippen { schliessen() }.semantics { contentDescription = "Zurück" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = f.text, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 2.dp)) {
                        Text("Mentalität", color = f.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        val untertitel = when {
                            spielStand.laeuft && spielStand.pausiert -> "Pausiert · Weiter setzt genau hier fort"
                            spielStand.laeuft && spielStand.internetProbleme -> "Internetprobleme · Satz wird erneut angefragt …"
                            else -> null
                        } ?: when (spielStand.phase) {
                            MentalPhase.LAEDT -> "Stimme wird frisch erzeugt …"
                            MentalPhase.SPRICHT -> "Spricht · Wiederholung ${spielStand.wiederholung} von ${spielStand.wiederholungenGesamt}"
                            MentalPhase.PAUSE -> "Pause (${spielStand.pauseSekunden} s) bis zur nächsten Ausgabe …"
                            MentalPhase.AUS -> when (gezeigt.size) {
                                0 -> "Noch keine Sätze"
                                1 -> "1 Satz · gedrückt halten zum Sortieren"
                                else -> "${gezeigt.size} Sätze · gedrückt halten zum Sortieren"
                            }
                        }
                        Text(
                            untertitel,
                            color = when {
                                spielStand.laeuft && !spielStand.pausiert && spielStand.internetProbleme -> f.gefahr
                                spielStand.laeuft -> MENTAL_AKZENT
                                else -> f.textLeise
                            },
                            fontWeight = if (spielStand.laeuft && !spielStand.pausiert && spielStand.internetProbleme) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                speicherHinweis?.let {
                    Text(it, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = f.gefahr, fontSize = 13.sp)
                }

                // ---- Scrollbare Satzliste mit Drag-and-Drop
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .reorderViewport(
                            state = reorder,
                            // Live aus dem State lesen: Das Modul behält diese Lambdas die ganze Geste über.
                            // Ein eingefangenes `alleIds` bliebe auf dem Stand beim Greifen stehen, dann gäbe es
                            // nur einen einzigen Platzwechsel pro Geste und die Lücke folgte dem Finger nicht.
                            order = { aktuelleIds() },
                            onMove = { von, nach ->
                                val l = aktuelleIds().toMutableList()
                                if (von != nach && von in l.indices && nach in l.indices) {
                                    l.add(nach, l.removeAt(von))
                                    ziehReihe = l
                                    haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDrop = {
                                ziehReihe?.let { speichereReihenfolge(it) }
                            },
                            reducedMotion = reduziert,
                        ),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 128.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (gezeigt.isEmpty()) {
                        item(key = -1L) {
                            Karte(Modifier.padding(top = 6.dp)) {
                                Text("Deine mentalen Sätze", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Tippe unten rechts auf Plus, um deinen ersten Satz einzusprechen. Rechts daneben legst du später fest, wie oft er wiederholt wird.",
                                    color = f.textLeise,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                    itemsIndexed(gezeigt, key = { _, it -> it.id }) { index, satz ->
                        val zieht = reorder.isDragging(satz.id)
                        val istAktiv = spielStand.laeuft && spielStand.aktiverSatzId == satz.id
                        fun schiebe(nach: Int) = alleIds.toMutableList().apply { add(nach, removeAt(index)) }

                        MentalSatzKarte(
                            satz = satz,
                            zieht = zieht,
                            istAktiv = istAktiv,
                            spielPhase = if (istAktiv) spielStand.phase else MentalPhase.AUS,
                            aktuelleWiederholung = if (istAktiv) spielStand.wiederholung else 0,
                            gesamteWiederholungen = if (istAktiv) spielStand.wiederholungenGesamt else satz.wiederholungen,
                            pauseSekunden = spielStand.pauseSekunden,
                            modifier = reorderItem(reorder, satz.id, reduziert).semantics {
                                customActions = listOfNotNull(
                                    CustomAccessibilityAction("Nach oben verschieben") { speichereReihenfolge(schiebe(index - 1)); true }.takeIf { index > 0 },
                                    CustomAccessibilityAction("Nach unten verschieben") { speichereReihenfolge(schiebe(index + 1)); true }.takeIf { index < alleIds.lastIndex },
                                )
                            },
                            griffModifier = reorderHandle(reorder, satz.id),
                            unterdrueckeKlick = { reorder.dragging || reorder.draggedId != null },
                            beiSatzTippen = { einstellungenOffen = false; reglerSatzId = null; bearbeiteSatzId = satz.id },
                            beiZahlTippen = { einstellungenOffen = false; bearbeiteSatzId = null; reglerSatzId = satz.id },
                        )
                    }
                }
            }

            // ---- Status-Hinweis beim Einsprechen direkt über den Knöpfen
            AnimatedVisibility(
                visible = diktatAktiv,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Row(
                    Modifier
                        .glas(f, 20.dp, erhoeht = 1.4f, fuellung = menueFlaeche(f).copy(alpha = 0.96f))
                        .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (nimmtAuf) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(f.gefahr))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        when {
                            nimmtAuf -> "Ich höre zu … tippe zum Speichern"
                            schreibtMit -> "Satz wird erkannt …"
                            else -> "Mikrofon startet …"
                        },
                        color = if (nimmtAuf) f.gefahr else f.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (nimmtAuf || holtMikro) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Abbrechen",
                            color = f.textLeise,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .antippen { vm.mentalitaetAufnahmeAbbrechen() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // ---- Untere Knöpfe rechts: Zahnrad (Einstellungen), Pause/Weiter, Lautsprecher (Start/Stopp), Plus (Einsprechen)
            Row(
                Modifier.align(Alignment.BottomEnd).padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(62.dp)
                        .glas(
                            f,
                            31.dp,
                            erhoeht = if (einstellungenOffen) 1.3f else 1.0f,
                            fuellung = f.flaecheStark,
                            toenung = if (einstellungenOffen) MENTAL_AKZENT else null,
                        )
                        .antippen {
                            reglerSatzId = null
                            bearbeiteSatzId = null
                            einstellungenOffen = true
                        }
                        .semantics { contentDescription = "Einstellungen für Mentalität" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        null,
                        tint = if (einstellungenOffen) MENTAL_AKZENT else f.text,
                        modifier = Modifier.size(26.dp),
                    )
                }

                val kannVorlesen = gezeigt.isNotEmpty() || spielStand.laeuft
                val laeuft = spielStand.laeuft

                // Pause/Weiter: nur während einer Session aktiv; hält genau an der aktuellen Stelle an und setzt dort fort.
                val pausiert = laeuft && spielStand.pausiert
                Box(
                    Modifier
                        .size(62.dp)
                        .then(
                            when {
                                pausiert -> Modifier.knopf3d(MENTAL_AKZENT, MENTAL_AKZENT_HELL, 31.dp, f.dunkel)
                                laeuft -> Modifier.glas(f, 31.dp, erhoeht = 1.2f, fuellung = f.flaecheStark, toenung = MENTAL_AKZENT)
                                else -> Modifier.glas(f, 31.dp, erhoeht = 0.3f, fuellung = f.flaeche).alpha(0.45f)
                            },
                        )
                        .antippen(haptik = laeuft) { if (laeuft) MentalitaetSpieler.pauseUmschalten() }
                        .semantics {
                            contentDescription = when {
                                pausiert -> "Vorlesen fortsetzen"
                                laeuft -> "Vorlesen pausieren"
                                else -> "Pause, erst nach dem Start verfügbar"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (pausiert) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        null,
                        tint = when {
                            pausiert -> Color.White
                            laeuft -> f.text
                            else -> f.textSchwach
                        },
                        modifier = Modifier.size(30.dp),
                    )
                }
                Box(
                    Modifier
                        .size(62.dp)
                        .then(
                            when {
                                laeuft -> Modifier.knopf3d(f.gefahr, f.gefahr.copy(alpha = 0.82f), 31.dp, f.dunkel)
                                kannVorlesen -> Modifier.glas(f, 31.dp, erhoeht = 1.2f, fuellung = f.flaecheStark, toenung = MENTAL_AKZENT)
                                else -> Modifier.glas(f, 31.dp, erhoeht = 0.3f, fuellung = f.flaeche).alpha(0.45f)
                            },
                        )
                        .antippen(haptik = kannVorlesen) {
                            if (laeuft) {
                                MentalitaetSpieler.stopp()
                            } else if (gezeigt.isNotEmpty()) {
                                if (diktatAktiv) {
                                    vm.meldung = "Bitte erst das Einsprechen beenden."
                                } else {
                                    MentalitaetSpieler.starten(appContext, gezeigt)
                                }
                            }
                        }
                        .semantics { contentDescription = if (laeuft) "Vorlesen stoppen" else "Vorlesen starten" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (laeuft) {
                        Icon(Icons.Rounded.Stop, null, tint = Color.White, modifier = Modifier.size(30.dp))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.VolumeUp,
                            null,
                            tint = if (kannVorlesen) f.text else f.textSchwach,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Box(
                    Modifier
                        .size(62.dp)
                        .then(
                            if (nimmtAuf) {
                                Modifier.knopf3d(f.gefahr, f.gefahr.copy(alpha = 0.82f), 31.dp, f.dunkel)
                            } else {
                                Modifier.knopf3d(MENTAL_AKZENT, MENTAL_AKZENT_HELL, 31.dp, f.dunkel)
                            },
                        )
                        .antippen {
                            einstellungenOffen = false
                            reglerSatzId = null
                            bearbeiteSatzId = null
                            vm.mentalitaetMikrofonTippen()
                        }
                        .semantics {
                            contentDescription = when {
                                nimmtAuf -> "Aufnahme beenden und Satz speichern"
                                schreibtMit -> "Satz wird erkannt"
                                else -> "Neuen Satz einsprechen"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        schreibtMit -> Kern(34.dp, Color.White, aktiv = true)
                        nimmtAuf -> Icon(Icons.Rounded.Stop, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        else -> Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        // ---- Einstellungen für Mentalität (aktuell: Pause zwischen Sätzen)
        if (einstellungenOffen) {
            MentalitaetEinstellungenEbene(
                pauseSekunden = pauseSekunden,
                beiPauseAendern = { einstellungen.mentalitaetPauseSekunden = it },
                schliessen = { einstellungenOffen = false },
            )
        }

        // ---- Wiederholungszahl-Schieberegler (1 bis 10), bequem unten am Daumen
        reglerSatz?.let { satz ->
            WiederholungsReglerEbene(
                satz = satz,
                beiWert = { neuerWert ->
                    ToolsAnsicht.hintergrund.launch {
                        MentalitaetSpeicher.wiederholungenSetzen(appContext, satz.id, neuerWert)
                    }
                },
                schliessen = { reglerSatzId = null },
            )
        }

        // ---- Satz bearbeiten oder löschen (Wurzelebene über der Tastatur statt Dialog-Fenster)
        bearbeiteSatz?.let { satz ->
            SatzBearbeitenEbene(
                satz = satz,
                beiSpeichern = { neu ->
                    bearbeiteSatzId = null
                    ToolsAnsicht.hintergrund.launch { MentalitaetSpeicher.textAendern(appContext, satz.id, neu) }
                },
                beiLoeschen = {
                    bearbeiteSatzId = null
                    if (reglerSatzId == satz.id) reglerSatzId = null
                    ToolsAnsicht.hintergrund.launch { MentalitaetSpeicher.loeschen(appContext, satz.id) }
                },
                beiLeerHinweis = { vm.meldung = "Der Satz darf nicht leer sein. Zum Entfernen tippe auf Löschen." },
                schliessen = { bearbeiteSatzId = null },
            )
        }
    }
}

@Composable
private fun MentalSatzKarte(
    satz: MentalSatz,
    zieht: Boolean,
    istAktiv: Boolean,
    spielPhase: MentalPhase,
    aktuelleWiederholung: Int,
    gesamteWiederholungen: Int,
    pauseSekunden: Int,
    modifier: Modifier = Modifier,
    griffModifier: Modifier,
    unterdrueckeKlick: () -> Boolean,
    beiSatzTippen: () -> Unit,
    beiZahlTippen: () -> Unit,
) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .glas(
                f,
                20.dp,
                erhoeht = when {
                    zieht -> 1.7f
                    istAktiv -> 1.1f
                    else -> 0.7f
                },
                toenung = if (istAktiv) MENTAL_AKZENT else null,
            )
            .then(
                if (istAktiv) Modifier.border(1.5.dp, MENTAL_AKZENT.copy(alpha = 0.75f), form)
                else Modifier,
            )
            .padding(start = 6.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Griff + Satztext: langes Drücken sortiert per Drag-and-Drop, kurzer Tipp bearbeitet/löscht.
        Row(
            griffModifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .antippen(haptik = false, unterdruecken = unterdrueckeKlick, aktion = beiSatzTippen)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.DragIndicator,
                    contentDescription = "Gedrückt halten und ziehen",
                    tint = if (zieht) MENTAL_AKZENT else f.textSchwach,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Text(
                    satz.text,
                    color = f.text,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = if (istAktiv) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (istAktiv) {
                    val status = when (spielPhase) {
                        MentalPhase.LAEDT -> "Wird frisch erzeugt … ($aktuelleWiederholung/$gesamteWiederholungen)"
                        MentalPhase.SPRICHT -> "Spricht · $aktuelleWiederholung von $gesamteWiederholungen"
                        MentalPhase.PAUSE -> "Pause (${pauseSekunden} s) · nach $aktuelleWiederholung von $gesamteWiederholungen"
                        MentalPhase.AUS -> ""
                    }
                    if (status.isNotEmpty()) {
                        Text(
                            status,
                            color = MENTAL_AKZENT,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        // Rechts neben jedem Satz: NUR die Wiederholungszahl (kein Lautsprechersymbol).
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (istAktiv) MENTAL_AKZENT.copy(alpha = 0.24f)
                    else f.flaecheStark,
                )
                .border(
                    1.dp,
                    if (istAktiv) MENTAL_AKZENT.copy(alpha = 0.7f) else f.rand,
                    CircleShape,
                )
                .antippen(aktion = beiZahlTippen)
                .semantics { contentDescription = "Wiederholungen: ${satz.wiederholungen}. Tippen zum Ändern" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                satz.wiederholungen.toString(),
                color = if (istAktiv) MENTAL_AKZENT else f.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Einstellungsmenü für „Mentalität“ als eigene Wurzelebene am unteren Rand (bequem auf Handy und Tablet).
 * Erste Einstellung: Pause zwischen Sätzen (3 bis 30 Sekunden, Standard 5 Sekunden).
 */
@Composable
private fun MentalitaetEinstellungenEbene(
    pauseSekunden: Int,
    beiPauseAendern: (Int) -> Unit,
    schliessen: () -> Unit,
) {
    val f = LocalFarben.current
    val haptik = LocalHapticFeedback.current
    val wert = pauseSekunden.coerceIn(MIN_PAUSE_SEKUNDEN, MAX_PAUSE_SEKUNDEN)

    fun setze(neu: Int) {
        val sauber = neu.coerceIn(MIN_PAUSE_SEKUNDEN, MAX_PAUSE_SEKUNDEN)
        if (sauber != wert) {
            haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            beiPauseAendern(sauber)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (f.dunkel) 0.58f else 0.34f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = schliessen,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .glas(f, 26.dp, erhoeht = 1.6f, fuellung = menueFlaeche(f).copy(alpha = 0.98f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text("Mentalität", color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Einstellungen",
                color = f.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pause zwischen Sätzen",
                    color = f.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(MENTAL_AKZENT, MENTAL_AKZENT_HELL)))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$wert Sekunden",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                "Nach dem Vorlesen eines Satzes bleibt diese Zeit zum eigenen Lesen, bevor die Wiedergabe fortgesetzt wird.",
                color = f.textLeise,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Slider(
                value = wert.toFloat(),
                onValueChange = { setze(it.roundToInt()) },
                valueRange = MIN_PAUSE_SEKUNDEN.toFloat()..MAX_PAUSE_SEKUNDEN.toFloat(),
                steps = MAX_PAUSE_SEKUNDEN - MIN_PAUSE_SEKUNDEN - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .semantics {
                        contentDescription = "Pause zwischen Sätzen: $wert Sekunden, von $MIN_PAUSE_SEKUNDEN bis $MAX_PAUSE_SEKUNDEN Sekunden"
                    },
                colors = SliderDefaults.colors(
                    thumbColor = MENTAL_AKZENT,
                    activeTrackColor = MENTAL_AKZENT,
                    inactiveTrackColor = f.flaecheStark,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )

            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$MIN_PAUSE_SEKUNDEN s",
                    color = if (wert == MIN_PAUSE_SEKUNDEN) MENTAL_AKZENT else f.textLeise,
                    fontSize = 12.sp,
                    fontWeight = if (wert == MIN_PAUSE_SEKUNDEN) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .antippen(haptik = false) { setze(MIN_PAUSE_SEKUNDEN) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
                Text(
                    "Standard: $STANDARD_PAUSE_SEKUNDEN s",
                    color = if (wert == STANDARD_PAUSE_SEKUNDEN) MENTAL_AKZENT else f.textLeise,
                    fontSize = 12.sp,
                    fontWeight = if (wert == STANDARD_PAUSE_SEKUNDEN) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .antippen(haptik = false) { setze(STANDARD_PAUSE_SEKUNDEN) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
                Text(
                    "$MAX_PAUSE_SEKUNDEN s",
                    color = if (wert == MAX_PAUSE_SEKUNDEN) MENTAL_AKZENT else f.textLeise,
                    fontSize = 12.sp,
                    fontWeight = if (wert == MAX_PAUSE_SEKUNDEN) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .antippen(haptik = false) { setze(MAX_PAUSE_SEKUNDEN) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }

            Knopf("Fertig", Modifier.fillMaxWidth(), farbe = MENTAL_AKZENT, aktion = schliessen)
        }
    }
}

/**
 * Schieberegler für die Wiederholungszahl (1 bis 10), als eigene Wurzelebene am unteren Rand,
 * damit er auf dem Handy bequem mit dem Daumen erreichbar bleibt.
 */
@Composable
private fun WiederholungsReglerEbene(
    satz: MentalSatz,
    beiWert: (Int) -> Unit,
    schliessen: () -> Unit,
) {
    val f = LocalFarben.current
    val haptik = LocalHapticFeedback.current
    var wert by remember(satz.id) { mutableIntStateOf(satz.wiederholungen.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN)) }

    fun setze(neu: Int) {
        val sauber = neu.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN)
        if (sauber != wert) {
            wert = sauber
            haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            beiWert(sauber)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (f.dunkel) 0.58f else 0.34f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = schliessen,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .glas(f, 26.dp, erhoeht = 1.6f, fuellung = menueFlaeche(f).copy(alpha = 0.98f))
                // Klicks auf der Karte selbst schließen das Fenster nicht.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Wiederholungen", color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        satz.text,
                        color = f.text,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MENTAL_AKZENT, MENTAL_AKZENT_HELL))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(wert.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Slider(
                value = wert.toFloat(),
                onValueChange = { setze(it.roundToInt()) },
                valueRange = MIN_WIEDERHOLUNGEN.toFloat()..MAX_WIEDERHOLUNGEN.toFloat(),
                steps = MAX_WIEDERHOLUNGEN - MIN_WIEDERHOLUNGEN - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .semantics { contentDescription = "Wiederholungen von 1 bis 10" },
                colors = SliderDefaults.colors(
                    thumbColor = MENTAL_AKZENT,
                    activeTrackColor = MENTAL_AKZENT,
                    inactiveTrackColor = f.flaecheStark,
                    activeTickColor = Color.White.copy(alpha = 0.65f),
                    inactiveTickColor = f.textSchwach.copy(alpha = 0.5f),
                ),
            )

            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (MIN_WIEDERHOLUNGEN..MAX_WIEDERHOLUNGEN).forEach { zahl ->
                    val gewaehlt = zahl == wert
                    Text(
                        zahl.toString(),
                        color = if (gewaehlt) MENTAL_AKZENT else f.textLeise,
                        fontSize = if (gewaehlt) 14.sp else 12.sp,
                        fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .antippen(haptik = false) { setze(zahl) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }

            Knopf("Fertig", Modifier.fillMaxWidth(), farbe = MENTAL_AKZENT, aktion = schliessen)
        }
    }
}

/**
 * Bearbeiten-Ebene direkt in der Wurzel (mit `imePadding()`), damit das mehrzeilige Textfeld sofort
 * den Fokus bekommt, die Tastatur öffnet und die Knöpfe immer vollständig erreichbar bleiben.
 */
@Composable
private fun SatzBearbeitenEbene(
    satz: MentalSatz,
    beiSpeichern: (String) -> Unit,
    beiLoeschen: () -> Unit,
    beiLeerHinweis: () -> Unit,
    schliessen: () -> Unit,
) {
    val f = LocalFarben.current
    val fokus = remember { FocusRequester() }
    val tastatur = LocalSoftwareKeyboardController.current
    var feld by remember(satz.id) {
        mutableStateOf(TextFieldValue(satz.text, selection = TextRange(satz.text.length)))
    }
    var loeschenBestaetigen by remember(satz.id) { mutableStateOf(false) }

    LaunchedEffect(satz.id) {
        // Erst nach dem ersten Layout fokussieren, damit das Textfeld sicher an die Tastatur gebunden ist.
        withFrameNanos { }
        runCatching { fokus.requestFocus() }
        tastatur?.show()
    }
    DisposableEffect(satz.id) {
        onDispose { tastatur?.hide() }
    }

    fun zu() {
        tastatur?.hide()
        schliessen()
    }

    fun sichern() {
        val sauber = feld.text.trim()
        if (sauber.isEmpty()) {
            beiLeerHinweis()
            return
        }
        tastatur?.hide()
        beiSpeichern(sauber)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (f.dunkel) 0.58f else 0.34f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::zu,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .glas(f, 26.dp, erhoeht = 1.6f, fuellung = menueFlaeche(f).copy(alpha = 0.98f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Satz bearbeiten",
                    Modifier.weight(1f),
                    color = f.text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (loeschenBestaetigen) f.gefahr.copy(alpha = 0.18f) else Color.Transparent)
                        .antippen {
                            if (loeschenBestaetigen) {
                                tastatur?.hide()
                                beiLoeschen()
                            } else {
                                loeschenBestaetigen = true
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = f.gefahr, modifier = Modifier.size(18.dp))
                    Text(
                        if (loeschenBestaetigen) " Wirklich löschen?" else " Löschen",
                        color = f.gefahr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            OutlinedTextField(
                value = feld,
                onValueChange = { feld = it; loeschenBestaetigen = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .focusRequester(fokus),
                placeholder = { Text("Mentaler Satz …", color = f.textSchwach) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = TextStyle(color = f.text, fontSize = 16.sp, lineHeight = 22.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = f.text,
                    unfocusedTextColor = f.text,
                    cursorColor = MENTAL_AKZENT,
                    focusedBorderColor = MENTAL_AKZENT,
                    unfocusedBorderColor = f.rand,
                    focusedContainerColor = f.flaecheStark,
                    unfocusedContainerColor = f.flaecheStark,
                ),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Knopf("Abbrechen", Modifier.weight(1f), haupt = false, aktion = ::zu)
                Knopf("Speichern", Modifier.weight(1f), icon = Icons.Rounded.Check, farbe = MENTAL_AKZENT, aktion = ::sichern)
            }
        }
    }
}
