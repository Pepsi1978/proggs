package de.frank.newskompass.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.frank.newskompass.news.DiktatZustand
import de.frank.newskompass.news.SprachStufe
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.heightIn
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.frank.module.draganddrop.ReorderAutoScroll
import de.frank.module.draganddrop.reorderHandle
import de.frank.module.draganddrop.reorderItem
import de.frank.module.draganddrop.reorderViewport
import de.frank.module.draganddrop.rememberReorderState
import de.frank.newskompass.AppProfil
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.ai.GeraeteAnmeldung
import de.frank.newskompass.ai.geraeteCodeGruppen
import de.frank.newskompass.data.EinstellungenStand
import de.frank.newskompass.data.model.Ausfuehrlichkeit
import de.frank.newskompass.data.model.BildModus
import de.frank.newskompass.data.model.Denkstufen
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.data.model.Rhythmus
import de.frank.newskompass.data.model.RhythmusArt
import de.frank.newskompass.data.model.Geschlecht
import de.frank.newskompass.data.model.Stimme
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.data.model.TtsAnbieter
import de.frank.newskompass.data.ArchivSicherung
import de.frank.newskompass.news.SicherungWorker
import de.frank.newskompass.news.Zeitplan
import de.frank.newskompass.tts.GeklonteStimme
import de.frank.newskompass.tts.QwenStimmVerwaltung
import de.frank.newskompass.tts.TtsCatalog
import de.frank.newskompass.ui.theme.blockFarbe
import de.frank.newskompass.ui.theme.blockVerlauf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Die Themen stehen hinter zwei festen Kopfzeilen; deren Plätze füllt dieser Platzhalter. */
private const val PLATZHALTER = Long.MIN_VALUE
private const val KOPFZEILEN = 2
private val TERMIN_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)

private fun schluessel(thema: Thema): Long =
    runCatching { UUID.fromString(thema.id).let { it.mostSignificantBits xor it.leastSignificantBits } }
        .getOrElse { thema.id.hashCode().toLong() }
        .let { if (it == PLATZHALTER) it + 1 else it }

@Composable
fun EinstellungenScreen(app: NewsApplication, activity: ComponentActivity, zurueck: () -> Unit) {
    val stand by app.einstellungen.stand.collectAsStateWithLifecycle()
    val liste = rememberLazyListState()
    val zustand = rememberReorderState(liste, "themen")
    ReorderAutoScroll(zustand)

    // Die gezogene Reihenfolge lebt lokal, damit ein Schreibvorgang die Geste nicht zurücksetzt.
    var reihenfolge by remember { mutableStateOf(stand.themen.map(::schluessel)) }
    LaunchedEffect(stand.themen) {
        val ids = stand.themen.map(::schluessel)
        reihenfolge = if (zustand.draggedId == null) ids else {
            val da = ids.toSet()
            val lokal = reihenfolge.toSet()
            reihenfolge.filter { it in da } + ids.filter { it !in lokal }
        }
    }
    val nachSchluessel = remember(stand.themen) { stand.themen.associateBy(::schluessel) }
    val sortiert = remember(reihenfolge, nachSchluessel) { reihenfolge.mapNotNull(nachSchluessel::get) }
    var neuesThema by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { app.ueberschriften.pruefe() }
    val kontext = LocalContext.current
    val diktat by app.themenDiktat.zustand.collectAsStateWithLifecycle()
    val mikrofonErlaubnis = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erteilt ->
        app.themenDiktat.erlaubnisErhalten(erteilt)
    }
    // Im Hintergrund schaltet Android das Mikrofon stumm — eine offene Aufnahme wird verworfen.
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis == Lifecycle.Event.ON_STOP) app.themenDiktat.brichAufnahmeAb()
        }
        lebenszyklus.addObserver(beobachter)
        onDispose {
            lebenszyklus.removeObserver(beobachter)
            app.themenDiktat.brichAufnahmeAb()
        }
    }
    val tippeMikrofon = { themaId: String ->
        val erlaubt = ContextCompat.checkSelfPermission(kontext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        app.themenDiktat.tippe(themaId, erlaubt) { mikrofonErlaubnis.launch(Manifest.permission.RECORD_AUDIO) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = zurueck) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück") }
            Text("Einstellungen", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            state = liste,
            modifier = Modifier.fillMaxSize().reorderViewport(
                state = zustand,
                order = { List(KOPFZEILEN) { PLATZHALTER } + reihenfolge },
                onMove = { von, nach ->
                    val a = von - KOPFZEILEN
                    val b = nach - KOPFZEILEN
                    if (a in reihenfolge.indices && b in reihenfolge.indices) {
                        reihenfolge = reihenfolge.toMutableList().apply { add(b, removeAt(a)) }
                    }
                },
                onDrop = {
                    // Aus dem aktuellen Stand abbilden, nicht aus dem beim Greifen: Sonst gingen Änderungen während
                    // der Geste verloren, etwa eine inzwischen erzeugte Überschrift.
                    val aktuell = app.einstellungen.stand.value.themen
                    val jetzt = aktuell.associateBy(::schluessel)
                    val geordnet = reihenfolge.mapNotNull(jetzt::get)
                    app.einstellungen.setzeThemen(geordnet + aktuell.filter { schluessel(it) !in reihenfolge })
                },
                reducedMotion = false,
            ),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "themen-titel") {
                Abschnitt(
                    "Deine Themen",
                    "Ein Stichwort, eine Frage oder ein ganzer Satz — jedes Feld wird ein eigener Nachrichtenblock. " +
                        "Die KI gibt jedem Thema eine kurze Überschrift; tippe darauf, um Text und Einstellungen aufzuklappen. " +
                        "Mit dem Mikrofon sprichst du ein Thema ein, der Zauberstab lässt die KI den Text sauber formulieren, " +
                        "und „Zurück“ holt den vorherigen Text wieder. " +
                        "Halte den Griff gedrückt und schieb die Themen in die gewünschte Reihenfolge.",
                )
            }
            item(key = "themen-abstand") { Spacer(Modifier.height(4.dp)) }
            items(sortiert, key = ::schluessel) { thema ->
                val position = sortiert.indexOf(thema)
                Box(reorderItem(zustand, schluessel(thema), false).widthIn(max = 760.dp).fillMaxWidth()) {
                    ThemenKarte(
                        thema = thema,
                        nummer = position,
                        gezogen = zustand.isDragging(schluessel(thema)),
                        griff = reorderHandle(zustand, schluessel(thema)),
                        fokussieren = neuesThema == thema.id,
                        loeschbar = sortiert.size > 1,
                        app = app,
                        diktat = diktat,
                        tippeMikrofon = { tippeMikrofon(thema.id) },
                        aendere = { text ->
                            app.einstellungen.setzeThemen(app.einstellungen.stand.value.themen.map { if (it.id == thema.id) it.copy(text = text) else it })
                        },
                        aendereBereich = { min, max ->
                            app.einstellungen.setzeThemen(
                                app.einstellungen.stand.value.themen.map {
                                    if (it.id == thema.id) it.copy(minMeldungen = min, maxMeldungen = max) else it
                                },
                            )
                        },
                        aendereAusfuehrlichkeit = { stufe ->
                            app.einstellungen.setzeThemen(
                                app.einstellungen.stand.value.themen.map {
                                    if (it.id == thema.id) it.copy(ausfuehrlichkeit = stufe) else it
                                },
                            )
                        },
                        zeitplanAktiv = stand.zeitplanAktiv,
                        aenderePlan = { zeiten, rhythmus ->
                            app.einstellungen.setzeThemen(
                                app.einstellungen.stand.value.themen.map {
                                    if (it.id == thema.id) it.copy(uhrzeiten = zeiten, rhythmus = rhythmus) else it
                                },
                            )
                        },
                        loesche = { app.einstellungen.setzeThemen(app.einstellungen.stand.value.themen.filterNot { it.id == thema.id }) },
                    )
                }
            }
            item(key = "themen-plus") {
                Breite {
                    OutlinedButton(
                        onClick = {
                            val neu = Thema(UUID.randomUUID().toString(), "")
                            neuesThema = neu.id
                            app.einstellungen.setzeThemen(app.einstellungen.stand.value.themen + neu)
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Thema hinzufügen")
                    }
                }
            }
            item(key = "ausfuehrlichkeit") { Breite { AusfuehrlichkeitBereich(app, stand) } }
            item(key = "codex") { Breite { CodexBereich(app, activity, stand) } }
            item(key = "bilder") { Breite { BilderBereich(app, stand) } }
            item(key = "vorlesen") { Breite { VorleseBereich(app, stand) } }
            item(key = "sprache") { Breite { SpracheingabeBereich(app, stand) } }
            item(key = "zeitplan") { Breite { ZeitplanBereich(app, activity, stand) } }
            item(key = "sicherung") { Breite { SicherungBereich(app, stand) } }
            item(key = "design") { Breite { DesignBereich(app, stand) } }
            item(key = "version") {
                Text(
                    "News Kompass ${AppProfil.VERSION_NAME} · Stand ${AppProfil.VERSION_BUMPED_AT}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun Breite(inhalt: @Composable () -> Unit) {
    Box(Modifier.widthIn(max = 760.dp).fillMaxWidth()) { inhalt() }
}

@Composable
private fun Abschnitt(titel: String, text: String? = null) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(top = 26.dp, bottom = 10.dp)) {
        Text(titel, style = MaterialTheme.typography.titleLarge)
        if (text != null) {
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Kachel(inhalt: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp).animateContentSize()) { inhalt() }
    }
}

// --- Themen ------------------------------------------------------------------------------

@Composable
private fun ThemenKarte(
    thema: Thema,
    nummer: Int,
    gezogen: Boolean,
    griff: Modifier,
    fokussieren: Boolean,
    loeschbar: Boolean,
    app: NewsApplication,
    diktat: DiktatZustand,
    tippeMikrofon: () -> Unit,
    aendere: (String) -> Unit,
    aendereBereich: (Int, Int) -> Unit,
    aendereAusfuehrlichkeit: (Ausfuehrlichkeit) -> Unit,
    zeitplanAktiv: Boolean,
    aenderePlan: (List<Int>, Rhythmus) -> Unit,
    loesche: () -> Unit,
) {
    var text by remember(thema.id) { mutableStateOf(thema.text) }
    var bereichOffen by remember(thema.id) { mutableStateOf(false) }
    // Wie in Perfect Moment: „Zurück“ holt den Text vor dem letzten Einsprechen oder der KI-Fassung zurück;
    // jeder KI-Druck arbeitet vom eigenen Original aus und liefert eine neue Formulierung.
    var rueckgaengig by remember(thema.id) { mutableStateOf<String?>(null) }
    var kiOriginal by remember(thema.id) { mutableStateOf<String?>(null) }
    var kiFassungen by remember(thema.id) { mutableStateOf(emptyList<String>()) }
    var verbessert by remember(thema.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fertig = diktat.fertig
    LaunchedEffect(fertig) {
        if (fertig != null && fertig.themaId == thema.id) {
            rueckgaengig = fertig.vorher.takeIf { it.isNotBlank() && it != fertig.text }
            kiOriginal = null
            kiFassungen = emptyList()
            text = fertig.text
            app.themenDiktat.quittiere(fertig)
        }
    }
    val verbessere = {
        val quelle = kiOriginal ?: text
        if (quelle.isBlank()) {
            app.themenDiktat.melde(thema.id, "Sprich oder schreibe zuerst dein Thema.")
        } else if (!app.codex.istVerbunden) {
            app.themenDiktat.melde(thema.id, "Bitte zuerst unten bei Codex anmelden.")
        } else if (!verbessert) {
            verbessert = true
            app.themenDiktat.loescheMeldung()
            scope.launch {
                try {
                    val neu = app.themenDiktat.verbessere(quelle, kiFassungen)
                    if (neu.isBlank()) {
                        app.themenDiktat.melde(thema.id, "Die KI hat keine Fassung geliefert.")
                    } else {
                        rueckgaengig = quelle
                        kiOriginal = quelle
                        kiFassungen = kiFassungen + neu
                        text = neu
                        aendere(neu)
                    }
                } catch (abbruch: CancellationException) {
                    throw abbruch
                } catch (fehler: Exception) {
                    app.themenDiktat.melde(thema.id, fehler.message ?: "Der Text konnte nicht verbessert werden.")
                } finally {
                    verbessert = false
                }
            }
        }
    }
    // Zugeklappt zeigt die Karte nur die kurze Überschrift; neue und leere Themen stehen gleich offen.
    var offen by rememberSaveable(thema.id) { mutableStateOf(fokussieren || thema.text.isBlank()) }
    var loeschenFragen by remember { mutableStateOf(false) }
    val drehung by animateFloatAsState(if (offen) 180f else 0f, label = "pfeil")
    val fokus = remember { FocusRequester() }
    LaunchedEffect(fokussieren) {
        if (fokussieren) {
            offen = true
            runCatching { fokus.requestFocus() }
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(if (gezogen) 14.dp else 0.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    griff.padding(start = 6.dp).size(width = 40.dp, height = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.DragIndicator, "Verschieben", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClickLabel = if (offen) "Zuklappen" else "Aufklappen") { offen = !offen },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(blockVerlauf(nummer)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${nummer + 1}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        thema.kopfzeile(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp).rotate(drehung),
                    )
                }
            }
            AnimatedVisibility(
                visible = offen,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(120)),
            ) {
            Column(Modifier.animateContentSize(animationSpec = tween(180))) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 76.dp)) {
                    TextField(
                        value = text,
                        onValueChange = {
                            text = it
                            // Selbst getippt ist ein neuer Ausgangspunkt: Zurück und alte KI-Fassungen gelten nicht mehr.
                            rueckgaengig = null
                            kiOriginal = null
                            kiFassungen = emptyList()
                            aendere(it)
                        },
                        placeholder = { Text("Worüber willst du informiert werden? Zum Beispiel: Fußball-Bundesliga, oder: Was gibt es Neues in der Raumfahrt?") },
                        modifier = Modifier.weight(1f).focusRequester(fokus),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
                val hierAktiv = diktat.themaId == thema.id
                val nimmtAuf = hierAktiv && diktat.stufe == SprachStufe.NIMMT_AUF
                val versteht = hierAktiv && diktat.stufe == SprachStufe.VERSTEHT
                val mikrofonFrei = diktat.stufe == SprachStufe.BEREIT || hierAktiv
                Row(
                    Modifier.padding(start = 84.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = tippeMikrofon,
                        enabled = mikrofonFrei && !versteht,
                        colors = if (nimmtAuf) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        },
                    ) {
                        when {
                            versteht -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            nimmtAuf -> Icon(Icons.Rounded.Stop, "Aufnahme beenden und übernehmen")
                            else -> Icon(Icons.Rounded.Mic, "Thema einsprechen")
                        }
                    }
                    FilledTonalIconButton(onClick = verbessere, enabled = !verbessert && !nimmtAuf && !versteht) {
                        if (verbessert) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.AutoFixHigh, "Text mit KI verbessern")
                        }
                    }
                    val alt = rueckgaengig
                    if (alt != null) {
                        FilledTonalIconButton(
                            onClick = {
                                text = alt
                                rueckgaengig = null
                                aendere(alt)
                            },
                            enabled = !verbessert && !nimmtAuf && !versteht,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, "Vorherigen Text wiederherstellen")
                        }
                    }
                    if (loeschbar) {
                        // Steht bewusst unten in der Knopfreihe, weit weg vom Zuklapp-Pfeil, und fragt vorher nach.
                        FilledTonalIconButton(onClick = { loeschenFragen = true }, enabled = !nimmtAuf && !versteht) {
                            Icon(Icons.Rounded.DeleteOutline, "Thema löschen")
                        }
                    }
                    Text(
                        when {
                            nimmtAuf -> "Ich höre zu … tippen zum Übernehmen"
                            versteht -> "Ich verstehe …"
                            verbessert -> "KI formuliert …"
                            diktat.meldungFuer == thema.id -> diktat.meldung
                            text.isBlank() -> "Tippe aufs Mikrofon und sprich dein Thema"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (diktat.meldungFuer == thema.id && !nimmtAuf && !versteht && !verbessert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                AssistChip(
                    onClick = { bereichOffen = true },
                    label = { Text(meldungsBereich(thema.minMeldungen, thema.maxMeldungen)) },
                    leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(AssistChipDefaults.IconSize)) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(start = 90.dp),
                )
                AusfuehrlichkeitRegler(
                    stufe = thema.ausfuehrlichkeit,
                    aendere = aendereAusfuehrlichkeit,
                    hinweis = "Gilt für dieses Thema ab der nächsten Aktualisierung – automatisch und per Hand.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
                AktualisierungsBereich(
                    thema = thema,
                    zeitplanAktiv = zeitplanAktiv,
                    aendere = aenderePlan,
                    modifier = Modifier.padding(start = 90.dp, end = 12.dp, bottom = 8.dp),
                )
            }
            }
        }
    }
    if (loeschenFragen) {
        AlertDialog(
            onDismissRequest = { loeschenFragen = false },
            title = { Text("Thema löschen?") },
            text = { Text("Willst du den Thementext wirklich löschen?\n\n„${thema.kopfzeile()}“") },
            confirmButton = {
                TextButton(onClick = {
                    loeschenFragen = false
                    loesche()
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { loeschenFragen = false }) { Text("Abbrechen") } },
        )
    }
    if (bereichOffen) {
        MeldungsBereichDialog(
            min = thema.minMeldungen,
            max = thema.maxMeldungen,
            schliessen = { bereichOffen = false },
            uebernehmen = { min, max ->
                bereichOffen = false
                aendereBereich(min, max)
            },
        )
    }
}

/**
 * Wann dieses Thema automatisch aktualisiert wird: erst der Rhythmus (täglich, alle x Tage,
 * wöchentlich, monatlich, jährlich) mit seinen passenden Feldern, dann die Uhrzeiten — antippen
 * ändert eine Zeit, Minus streicht sie, Plus fügt eine hinzu. Unten fasst ein Satz alles zusammen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AktualisierungsBereich(
    thema: Thema,
    zeitplanAktiv: Boolean,
    aendere: (List<Int>, Rhythmus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uhrzeiten = thema.uhrzeiten
    val rhythmus = thema.rhythmus
    val setzeZeiten = { zeiten: List<Int> -> aendere(zeiten, rhythmus) }
    val setzeRhythmus = { neu: Rhythmus -> aendere(uhrzeiten, neu) }
    // null = keine Bearbeitung, -1 = neue Uhrzeit, sonst die bearbeitete Uhrzeit.
    var bearbeitet by remember { mutableStateOf<Int?>(null) }
    var abWaehlen by remember { mutableStateOf(false) }
    var monatstagWaehlen by remember { mutableStateOf(false) }
    var jahrestagWaehlen by remember { mutableStateOf(false) }
    val grau = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp), tint = grau)
            Spacer(Modifier.width(6.dp))
            Text("Automatisch aktualisieren", style = MaterialTheme.typography.labelLarge, color = grau)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RhythmusArt.entries.forEach { art ->
                FilterChip(
                    selected = rhythmus.art == art,
                    onClick = { if (rhythmus.art != art) setzeRhythmus(Rhythmus.neu(art, LocalDate.now())) },
                    label = { Text(art.label) },
                    shape = RoundedCornerShape(50),
                )
            }
        }
        when (rhythmus.art) {
            RhythmusArt.TAEGLICH -> Unit
            RhythmusArt.ALLE_X_TAGE -> FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZahlWaehler("Alle", rhythmus.intervall, if (rhythmus.intervall == 1) "Tag" else "Tage", Rhythmus.maxIntervall(rhythmus.art)) {
                    setzeRhythmus(rhythmus.copy(intervall = it))
                }
                AbChip(rhythmus.ab) { abWaehlen = true }
            }
            RhythmusArt.WOECHENTLICH -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { tag ->
                        FilterChip(
                            selected = tag in rhythmus.wochentage,
                            onClick = {
                                val tage = if (tag in rhythmus.wochentage) rhythmus.wochentage - tag else rhythmus.wochentage + tag
                                setzeRhythmus(rhythmus.copy(wochentage = tage))
                            },
                            label = { Text(Rhythmus.wochentagKurz(tag)) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZahlWaehler(
                        if (rhythmus.intervall == 1) "Jede" else "Alle",
                        rhythmus.intervall,
                        if (rhythmus.intervall == 1) "Woche" else "Wochen",
                        Rhythmus.maxIntervall(rhythmus.art),
                    ) { setzeRhythmus(rhythmus.copy(intervall = it)) }
                    if (rhythmus.intervall > 1) AbChip(rhythmus.ab) { abWaehlen = true }
                }
            }
            RhythmusArt.MONATLICH -> AssistChip(
                onClick = { monatstagWaehlen = true },
                label = { Text("Jeden Monat am ${rhythmus.tag}.") },
                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(AssistChipDefaults.IconSize)) },
                shape = RoundedCornerShape(50),
            )
            RhythmusArt.JAEHRLICH -> AssistChip(
                onClick = { jahrestagWaehlen = true },
                label = { Text("Jedes Jahr am ${Rhythmus.jahrestagText(rhythmus.tag, rhythmus.monat)}") },
                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(AssistChipDefaults.IconSize)) },
                shape = RoundedCornerShape(50),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            uhrzeiten.forEach { minute ->
                InputChip(
                    selected = false,
                    onClick = { bearbeitet = minute },
                    label = { Text("${Thema.uhrzeitText(minute)} Uhr") },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.RemoveCircleOutline,
                            "Uhrzeit ${Thema.uhrzeitText(minute)} entfernen",
                            Modifier.size(InputChipDefaults.IconSize).clip(CircleShape).clickable { setzeZeiten(uhrzeiten - minute) },
                        )
                    },
                    shape = RoundedCornerShape(50),
                )
            }
            AssistChip(
                onClick = { bearbeitet = -1 },
                label = { Text("Uhrzeit") },
                leadingIcon = { Icon(Icons.Rounded.Add, "Uhrzeit hinzufügen", Modifier.size(AssistChipDefaults.IconSize)) },
                shape = RoundedCornerShape(50),
            )
        }
        val zusammenfassung = remember(thema, zeitplanAktiv) { planZusammenfassung(thema, zeitplanAktiv) }
        Text(
            zusammenfassung,
            style = MaterialTheme.typography.bodySmall,
            color = grau,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
    bearbeitet?.let { alt ->
        UhrzeitDialog(
            titel = if (alt < 0) "Neue Uhrzeit" else "Uhrzeit ändern",
            start = if (alt < 0) 12 * 60 else alt,
            schliessen = { bearbeitet = null },
            uebernehmen = { neu ->
                bearbeitet = null
                setzeZeiten(((if (alt < 0) uhrzeiten else uhrzeiten - alt) + neu).distinct().sorted())
            },
        )
    }
    if (abWaehlen) {
        DatumDialog(rhythmus.ab, schliessen = { abWaehlen = false }) {
            abWaehlen = false
            setzeRhythmus(rhythmus.copy(ab = it))
        }
    }
    if (jahrestagWaehlen) {
        val jahr = LocalDate.now().year
        val start = LocalDate.of(jahr, rhythmus.monat, 1).let { it.withDayOfMonth(minOf(rhythmus.tag, it.lengthOfMonth())) }
        DatumDialog(start, schliessen = { jahrestagWaehlen = false }) {
            jahrestagWaehlen = false
            setzeRhythmus(rhythmus.copy(tag = it.dayOfMonth, monat = it.monthValue))
        }
    }
    if (monatstagWaehlen) {
        MonatstagDialog(rhythmus.tag, schliessen = { monatstagWaehlen = false }) {
            monatstagWaehlen = false
            setzeRhythmus(rhythmus.copy(tag = it))
        }
    }
}

/** Der kleine Satz unter den Feldern: wann dieses Thema aktualisiert wird und wann das nächste Mal. */
private fun planZusammenfassung(thema: Thema, zeitplanAktiv: Boolean): String {
    if (thema.uhrzeiten.isEmpty()) return "Keine Uhrzeit — dieses Thema wird nur per Hand aktualisiert."
    val zeiten = Rhythmus.aufzaehlung(thema.uhrzeiten.map(Thema::uhrzeitText))
    val satz = "${thema.rhythmus.beschreibung()} um $zeiten Uhr."
    if (!zeitplanAktiv) return "$satz Der Zeitplan ist unten ausgeschaltet."
    val naechster = Zeitplan.naechsterTermin(thema)
        ?: return "$satz Es gibt keinen passenden Termin."
    val wann = naechster.format(TERMIN_FORMAT)
    return "$satz Nächstes Mal: $wann."
}

@Composable
private fun ZahlWaehler(vorher: String, wert: Int, nachher: String, max: Int, aendere: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(vorher, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { aendere((wert - 1).coerceAtLeast(1)) }, enabled = wert > 1) {
            Icon(Icons.Rounded.Remove, "Weniger")
        }
        Text("$wert", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { aendere((wert + 1).coerceAtMost(max)) }, enabled = wert < max) {
            Icon(Icons.Rounded.Add, "Mehr")
        }
        Text(nachher, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AbChip(ab: LocalDate, waehle: () -> Unit) {
    AssistChip(
        onClick = waehle,
        label = { Text("ab ${Rhythmus.datumText(ab)}") },
        leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(AssistChipDefaults.IconSize)) },
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(top = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatumDialog(start: LocalDate, schliessen: () -> Unit, uebernehmen: (LocalDate) -> Unit) {
    // Der DatePicker rechnet in UTC-Mitternacht.
    val zustand = rememberDatePickerState(initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = schliessen,
        confirmButton = {
            TextButton(onClick = {
                val ms = zustand.selectedDateMillis
                if (ms == null) schliessen() else uebernehmen(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = schliessen) { Text("Abbrechen") } },
    ) {
        DatePicker(state = zustand)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonatstagDialog(tag: Int, schliessen: () -> Unit, uebernehmen: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Tag im Monat") },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..31).forEach { n ->
                        val gewaehlt = n == tag
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (gewaehlt) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { uebernehmen(n) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$n",
                                color = if (gewaehlt) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Hat ein Monat diesen Tag nicht, etwa den 31., wird am letzten Tag des Monats aktualisiert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = schliessen) { Text("Schließen") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UhrzeitDialog(titel: String, start: Int, schliessen: () -> Unit, uebernehmen: (Int) -> Unit) {
    val zeit = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text(titel) },
        text = { TimePicker(state = zeit) },
        confirmButton = { TextButton(onClick = { uebernehmen(zeit.hour * 60 + zeit.minute) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = schliessen) { Text("Abbrechen") } },
    )
}

private fun meldungsBereich(min: Int, max: Int): String =
    if (min == max) "Genau $max ${if (max == 1) "Meldung" else "Meldungen"}" else "$min–$max Meldungen"

@Composable
private fun MeldungsBereichDialog(min: Int, max: Int, schliessen: () -> Unit, uebernehmen: (Int, Int) -> Unit) {
    var bereich by remember { mutableStateOf(min.toFloat()..max.toFloat()) }
    val neuMin = bereich.start.roundToInt()
    val neuMax = bereich.endInclusive.roundToInt()
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Meldungen in diesem Block") },
        text = {
            Column {
                Text(meldungsBereich(neuMin, neuMax), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                RangeSlider(
                    value = bereich,
                    onValueChange = { bereich = it },
                    valueRange = Thema.GRENZE_MIN.toFloat()..Thema.GRENZE_MAX.toFloat(),
                    steps = Thema.GRENZE_MAX - Thema.GRENZE_MIN - 1,
                )
                Text(
                    "Der Höchstwert gilt fest. Der Mindestwert ist ein Ziel: Gibt es in 48 Stunden nicht genug Neues, " +
                        "füllt Codex nur mit belegten Meldungen der letzten sieben Tage auf und sagt, wann sie passiert sind — sonst bleibt der Block kürzer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { uebernehmen(neuMin, neuMax) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = schliessen) { Text("Abbrechen") } },
    )
}

// --- Codex -------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodexBereich(app: NewsApplication, activity: ComponentActivity, stand: EinstellungenStand) {
    val bereich = rememberCoroutineScope()
    val kontext = LocalContext.current
    var verbunden by remember { mutableStateOf(app.codex.istVerbunden) }
    var email by remember { mutableStateOf(app.codex.email) }
    var anmeldung by remember { mutableStateOf<GeraeteAnmeldung?>(null) }
    var laeuft by remember { mutableStateOf(false) }
    var meldung by remember { mutableStateOf<String?>(null) }
    var ladeModelle by remember { mutableStateOf(false) }

    fun holeModelle() {
        ladeModelle = true
        bereich.launch {
            runCatching { app.codex.ladeModelle() }
                .onSuccess { app.einstellungen.setzeModelle(it); meldung = "${it.size} Modelle geladen." }
                .onFailure { meldung = "Modelle nicht abrufbar: ${it.message}" }
            ladeModelle = false
        }
    }
    LaunchedEffect(verbunden) { if (verbunden) holeModelle() }

    Column {
        Abschnitt("Codex", "Recherchiert mit Websuche und malt die Illustrationen.")
        Kachel {
            if (verbunden) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF16A34A))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Angemeldet", style = MaterialTheme.typography.titleMedium)
                        email?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    TextButton(onClick = {
                        app.codex.meldeAb()
                        verbunden = false
                        email = null
                    }) { Text("Abmelden") }
                }
            } else if (anmeldung != null) {
                val code = anmeldung!!.benutzerCode
                Text("Tippe diesen Code auf der geöffneten Seite ein:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    geraeteCodeGruppen(code).forEach { gruppe ->
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                            Text(
                                gruppe,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = 3.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                    IconButton(onClick = { kopiere(kontext, code) }) { Icon(Icons.Rounded.ContentCopy, "Code kopieren") }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Warte auf die Bestätigung …", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { app.codex.brichAnmeldungAb() }) { Text("Abbrechen") }
                }
            } else {
                Text(
                    "Mit deinem ChatGPT-Konto anmelden. Die App zeigt dir einen kurzen Code aus vier und fünf Zeichen, den du auf der Anmeldeseite eintippst.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    enabled = !laeuft,
                    shape = RoundedCornerShape(50),
                    onClick = {
                        laeuft = true
                        meldung = null
                        bereich.launch {
                            try {
                                val ergebnis = app.codex.melde(activity) { anmeldung = it }
                                email = ergebnis.email
                                verbunden = true
                                app.ueberschriften.pruefe()
                                if (app.speicher.index.value.isEmpty()) Zeitplan.starteLauf(kontext, manuell = true)
                            } catch (abbruch: CancellationException) {
                                meldung = "Anmeldung abgebrochen."
                            } catch (fehler: Exception) {
                                meldung = fehler.message
                            } finally {
                                anmeldung = null
                                laeuft = false
                            }
                        }
                    },
                ) { Text("Bei Codex anmelden") }
            }
            meldung?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Modell", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (verbunden) {
                    IconButton(onClick = ::holeModelle, enabled = !ladeModelle) {
                        if (ladeModelle) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.Refresh, "Modelle neu laden")
                    }
                }
            }
            val modelle = stand.modelle
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                modelle.forEach { m ->
                    FilterChip(
                        selected = m.id == stand.modellId,
                        onClick = { app.einstellungen.setzeModell(m.id) },
                        label = { Text(m.name) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }
            val gewaehlt = modelle.firstOrNull { it.id == stand.modellId }
            Spacer(Modifier.height(14.dp))
            Text("Denktiefe (Effort)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (gewaehlt?.stufen ?: listOf("low", "medium", "high", "xhigh", "max")).forEach { s ->
                    FilterChip(
                        selected = s == stand.denktiefe,
                        onClick = { app.einstellungen.setzeDenktiefe(s) },
                        label = { Text(Denkstufen.label(s)) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Höhere Denktiefe recherchiert gründlicher, dauert aber länger und verbraucht mehr Kontingent.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun kopiere(kontext: Context, text: String) {
    kontext.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Code", text))
    Toast.makeText(kontext, "Code kopiert", Toast.LENGTH_SHORT).show()
}

// --- Bilder ------------------------------------------------------------------------------

@Composable
private fun AusfuehrlichkeitBereich(app: NewsApplication, stand: EinstellungenStand) {
    Column {
        Abschnitt(
            "Ausführlichkeit für freie Themen",
            "Für Fragen und Themen, die du über den Mikrofonbutton unten rechts auf dem Startbildschirm einsprichst.",
        )
        Kachel {
            AusfuehrlichkeitRegler(
                stufe = stand.ausfuehrlichkeit,
                aendere = app.einstellungen::setzeAusfuehrlichkeit,
                hinweis = "Gilt ab der nächsten Mikrofon-Frage. Gespeicherte Themen haben ihren eigenen Regler.",
            )
        }
    }
}

@Composable
private fun AusfuehrlichkeitRegler(
    stufe: Ausfuehrlichkeit,
    aendere: (Ausfuehrlichkeit) -> Unit,
    hinweis: String,
    modifier: Modifier = Modifier,
) {
    // Während der Geste nur diesen Regler aktualisieren, nicht die gesamte Themenliste speichern.
    var wert by remember(stufe) { mutableStateOf(stufe.maxAbsaetze.toFloat()) }
    val gewaehlt = Ausfuehrlichkeit.fromAbsaetze(wert.roundToInt())
    val farben = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = farben.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = farben.primaryContainer, contentColor = farben.onPrimaryContainer) {
                    Icon(Icons.Rounded.Tune, null, Modifier.padding(10.dp).size(20.dp))
                }
                Text("Ausführlichkeit", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Slider(
                value = wert,
                onValueChange = { wert = it },
                onValueChangeFinished = { if (gewaehlt != stufe) aendere(gewaehlt) },
                valueRange = 2f..10f,
                steps = 7,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Ausführlichkeit pro Meldung"
                    stateDescription = gewaehlt.umfang
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kurz", style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant)
                Text("10 Absätze", style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = farben.primaryContainer, contentColor = farben.onPrimaryContainer) {
                Text(
                    gewaehlt.umfang,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Text(gewaehlt.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            Text(
                gewaehlt.erklaerung,
                style = MaterialTheme.typography.bodyMedium,
                color = farben.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(hinweis, style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun BilderBereich(app: NewsApplication, stand: EinstellungenStand) {
    Column {
        Abschnitt("Bilder")
        Kachel {
            BildModus.entries.forEach { modus ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { app.einstellungen.setzeBildModus(modus) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = stand.bildModus == modus, onClick = { app.einstellungen.setzeBildModus(modus) })
                    Column {
                        Text(modus.label, style = MaterialTheme.typography.titleMedium)
                        Text(modus.erklaerung, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (stand.bildModus == BildModus.FOTO_SONST_KI || stand.bildModus == BildModus.NUR_KI) {
                Spacer(Modifier.height(12.dp))
                Text("Höchstens ${stand.maxKiBilder} KI-Bilder pro Ausgabe", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = stand.maxKiBilder.toFloat(),
                    onValueChange = { app.einstellungen.setzeMaxKiBilder(it.toInt()) },
                    valueRange = 0f..30f,
                    steps = 29,
                )
                if (!stand.bilderUnterstuetzt) {
                    Text(
                        "Codex hat die Bilderzeugung zuletzt abgelehnt; deshalb gibt es gerade nur Fotos der Quellen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    TextButton(onClick = { app.einstellungen.setzeBilderUnterstuetzt(true) }) { Text("Beim nächsten Lauf erneut versuchen") }
                }
            }
        }
    }
}

// --- Vorlesen ----------------------------------------------------------------------------

@Composable
private fun VorleseBereich(app: NewsApplication, stand: EinstellungenStand) {
    val bereich = rememberCoroutineScope()
    val kontext = LocalContext.current
    val eigeneStimmen = remember { mutableStateListOf<GeklonteStimme>() }
    var ladeStimmen by remember { mutableStateOf(false) }
    var stimmMeldung by remember { mutableStateOf<String?>(null) }

    fun holeStimmen() {
        ladeStimmen = true
        bereich.launch {
            runCatching { QwenStimmVerwaltung { app.einstellungen.alibabaSchluessel }.liste() }
                .onSuccess {
                    eigeneStimmen.clear()
                    eigeneStimmen.addAll(it)
                    stimmMeldung = if (it.isEmpty()) "Im Alibaba-Konto ist noch keine eigene Stimme angelegt." else null
                    if (stand.qwenStimmeId.isBlank() && it.isNotEmpty()) app.einstellungen.setzeQwenStimme(it.first().id)
                }
                .onFailure { stimmMeldung = it.message }
            ladeStimmen = false
        }
    }
    LaunchedEffect(stand.ttsAnbieter, stand.hatAlibabaSchluessel) {
        if (stand.ttsAnbieter == TtsAnbieter.QWEN && stand.hatAlibabaSchluessel) holeStimmen()
    }
    val probe: (TtsAnbieter, String) -> Unit = { anbieter, stimme ->
        app.vorleser.probiere(anbieter, stimme) { fehler -> Toast.makeText(kontext, fehler, Toast.LENGTH_LONG).show() }
    }

    Column {
        Abschnitt("Vorlesen", "Jede Meldung hat einen Lautsprecher. Gelesen wird Absatz für Absatz; der nächste wird schon vorbereitet, während der aktuelle läuft.")
        Kachel {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TtsAnbieter.entries.forEachIndexed { i, a ->
                    SegmentedButton(
                        selected = stand.ttsAnbieter == a,
                        onClick = { app.einstellungen.setzeTtsAnbieter(a) },
                        shape = SegmentedButtonDefaults.itemShape(i, TtsAnbieter.entries.size),
                    ) {
                        Text(
                            when (a) {
                                TtsAnbieter.GOOGLE -> "Google HD"
                                TtsAnbieter.EDGE -> "Edge"
                                TtsAnbieter.QWEN -> "Alibaba"
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            when (stand.ttsAnbieter) {
                TtsAnbieter.GOOGLE -> {
                    SchluesselFeld("Google-Cloud-Schlüssel", stand.hatGoogleSchluessel) { app.einstellungen.setzeGoogleSchluessel(it) }
                    Spacer(Modifier.height(12.dp))
                    StimmenWahl(TtsCatalog.googleStimmen, stand.googleStimme, { app.einstellungen.setzeGoogleStimme(it) }) {
                        probe(TtsAnbieter.GOOGLE, it)
                    }
                }
                TtsAnbieter.EDGE -> {
                    Text("Kostenlos und ohne Schlüssel.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    StimmenWahl(TtsCatalog.edgeStimmen, stand.edgeStimme, { app.einstellungen.setzeEdgeStimme(it) }) {
                        probe(TtsAnbieter.EDGE, it)
                    }
                }
                TtsAnbieter.QWEN -> {
                    SchluesselFeld("Alibaba-Model-Studio-Schlüssel", stand.hatAlibabaSchluessel) { app.einstellungen.setzeAlibabaSchluessel(it) }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Deine Stimmen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = ::holeStimmen, enabled = stand.hatAlibabaSchluessel && !ladeStimmen) {
                            if (ladeStimmen) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Refresh, "Stimmen laden")
                        }
                    }
                    eigeneStimmen.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { app.einstellungen.setzeQwenStimme(s.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.id == stand.qwenStimmeId, onClick = { app.einstellungen.setzeQwenStimme(s.id) })
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.titleMedium)
                                Text("angelegt ${s.angelegtAm}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { probe(TtsAnbieter.QWEN, s.id) }) { Icon(Icons.Rounded.PlayArrow, "Probe hören") }
                        }
                    }
                    stimmMeldung?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Tempo ${"%.2f".format(Locale.GERMANY, stand.sprechtempo)}×", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = stand.sprechtempo,
                onValueChange = { app.einstellungen.setzeTempo((it * 20).toInt() / 20f) },
                valueRange = 0.6f..1.6f,
            )
        }
    }
}

// --- Spracheingabe ------------------------------------------------------------------------

@Composable
private fun SpracheingabeBereich(app: NewsApplication, stand: EinstellungenStand) {
    Column {
        Abschnitt(
            "Spracheingabe",
            "Der Mikrofon-Knopf unten rechts: tippen, Frage sprechen, noch einmal tippen. Erkannt wird mit Groq Whisper Large V3 Turbo, " +
                "geschützt durch vier Schichten gegen Wörter, die Whisper in die Stille hineindichtet. Die Frage wirkt wie ein Thema nur für diesen Moment: " +
                "Die Antwort erscheint als eigener Block unten in der aktuellen Ausgabe und wird vorgelesen. Deine Themenliste bleibt unverändert.",
        )
        Kachel {
            SchluesselFeld("Groq-Schlüssel", stand.hatGroqSchluessel) { app.einstellungen.setzeGroqSchluessel(it) }
            Spacer(Modifier.height(8.dp))
            Text(
                "Den Schlüssel bekommst du unter console.groq.com.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SchluesselFeld(titel: String, vorhanden: Boolean, speichere: (String) -> Unit) {
    var wert by remember { mutableStateOf("") }
    OutlinedTextField(
        value = wert,
        onValueChange = { wert = it },
        label = { Text(if (vorhanden) "$titel (gespeichert)" else titel) },
        placeholder = { Text(if (vorhanden) "•••••••• — zum Ersetzen neu eintragen" else "Schlüssel einfügen") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            if (wert.isNotBlank()) {
                TextButton(onClick = {
                    speichere(wert)
                    wert = ""
                }) { Text("Speichern") }
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StimmenWahl(stimmen: List<Stimme>, gewaehlt: String, waehle: (String) -> Unit, probe: (String) -> Unit) {
    var offen by remember { mutableStateOf(false) }
    val aktuell = stimmen.firstOrNull { it.id == gewaehlt } ?: stimmen.first()
    Box {
        Surface(
            onClick = { offen = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Stimme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${aktuell.name} · ${if (aktuell.geschlecht == Geschlecht.WEIBLICH) "weiblich" else "männlich"}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = { probe(aktuell.id) }) { Icon(Icons.Rounded.PlayArrow, "Probe hören") }
                Icon(Icons.Rounded.UnfoldMore, null)
            }
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            stimmen.forEach { s ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${s.name}  ·  ${if (s.geschlecht == Geschlecht.WEIBLICH) "w" else "m"}",
                            fontWeight = if (s.id == gewaehlt) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        waehle(s.id)
                        offen = false
                    },
                    trailingIcon = {
                        IconButton(onClick = { probe(s.id) }) { Icon(Icons.Rounded.PlayArrow, "Probe hören") }
                    },
                )
            }
        }
    }
}

// --- Zeitplan und Design -----------------------------------------------------------------

/**
 * Archivsicherung: ZIP mit allen Ausgaben und Bildern an einen frei gewählten Ort, und die
 * Prüfung einer vorhandenen Sicherung als Trockenlauf. Erfolg zeigt die App erst nach dem
 * vollständigen Zurücklesen; das Hochladen in eine Cloud bestätigt sie nicht.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SicherungBereich(app: NewsApplication, stand: EinstellungenStand) {
    val kontext = LocalContext.current
    val index by app.speicher.index.collectAsStateWithLifecycle()
    val auftraege by remember { WorkManager.getInstance(kontext).getWorkInfosForUniqueWorkFlow(Zeitplan.SICHERUNG) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val auftrag = auftraege.firstOrNull()
    val laeuft = auftrag != null && !auftrag.state.isFinished
    var startFehler by remember { mutableStateOf<String?>(null) }
    // Beiseitegelegtes aus Importen — neu gezählt, sobald ein Auftrag endet.
    var beiseite by remember { mutableStateOf<ArchivSicherung.Beiseite?>(null) }
    LaunchedEffect(auftrag?.state) {
        beiseite = withContext(Dispatchers.IO) { runCatching { ArchivSicherung.zaehleBeiseite(app.speicher.importWurzel) }.getOrNull() }
    }

    fun starte(uri: Uri?, art: String, schreiben: Boolean) {
        if (uri == null) return
        // Die Freigabe muss den Auftrag überdauern, auch wenn die Oberfläche geschlossen wird.
        // Gibt der Anbieter sie nicht dauerhaft her, startet kein Auftrag, der später nicht lesen könnte.
        val rechte = Intent.FLAG_GRANT_READ_URI_PERMISSION or (if (schreiben) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        val erteilt = runCatching { kontext.contentResolver.takePersistableUriPermission(uri, rechte) }.isSuccess
        if (!erteilt) {
            if (schreiben) runCatching { DocumentsContract.deleteDocument(kontext.contentResolver, uri) }
            startFehler = "Der gewählte Speicherort gibt der App keinen dauerhaften Zugriff. Bitte einen anderen Ort wählen, etwa den internen Speicher oder Google Drive."
            return
        }
        startFehler = null
        SicherungWorker.starte(kontext, uri, art)
    }
    val sichern = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        starte(uri, SicherungWorker.ART_EXPORT, schreiben = true)
    }
    val pruefen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        starte(uri, SicherungWorker.ART_PRUEFEN, schreiben = false)
    }
    val importieren = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        starte(uri, SicherungWorker.ART_IMPORT, schreiben = false)
    }
    val zipTypen = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

    Column {
        Abschnitt(
            "Archivsicherung",
            "Sichert alle gespeicherten Ausgaben und ihre Bilder als ZIP-Datei an einen Ort deiner Wahl, auch in Google Drive. " +
                "Einstellungen, Schlüssel und Anmeldung sind nicht enthalten. Ein Import fügt nur fehlende Ausgaben hinzu und überschreibt oder löscht nichts.",
        )
        Kachel {
            if (stand.letzteSicherungUm > 0) {
                Text("Letzte geprüfte Sicherung: ${SicherungWorker.datum(stand.letzteSicherungUm)}", style = MaterialTheme.typography.titleMedium)
                Text(stand.letzteSicherungText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val neu = index.count { it.stand > stand.letzteSicherungUm }
                if (neu > 0) {
                    Text(
                        if (neu == 1) "1 Ausgabe ist seitdem neu oder geändert." else "$neu Ausgaben sind seitdem neu oder geändert.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else {
                Text("Noch keine geprüfte Sicherung.", style = MaterialTheme.typography.titleMedium)
            }
            beiseite?.takeIf { !it.leer }?.let { b ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Beiseitegelegt aus Importen: ${b.konflikte} Konfliktfassungen (mit ${b.bilder} Bildern) und ${b.beschaedigt} beschädigte Rohdateien. " +
                        "Sie liegen getrennt vom Archiv im App-Speicher und werden mit jeder Sicherung mitgesichert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (laeuft) {
                Text(auftrag?.progress?.getString(SicherungWorker.K_TEXT) ?: "Wird vorbereitet …", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (auftrag?.progress?.getFloat(SicherungWorker.K_ANTEIL, 0f) ?: 0f).coerceIn(0.02f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { WorkManager.getInstance(kontext).cancelUniqueWork(Zeitplan.SICHERUNG) }) { Text("Abbrechen") }
            } else {
                val ergebnis = when (auftrag?.state) {
                    WorkInfo.State.SUCCEEDED -> auftrag.outputData.getString(SicherungWorker.K_TEXT)
                    WorkInfo.State.FAILED -> auftrag.outputData.getString(SicherungWorker.K_FEHLER) ?: "Die Sicherung ist unerwartet gescheitert; lokal wurde nichts verändert."
                    WorkInfo.State.CANCELLED -> when {
                        SicherungWorker.ETIKETT + SicherungWorker.ART_PRUEFEN in auftrag.tags -> "Prüfung abgebrochen. Es wurde nichts verändert."
                        SicherungWorker.ETIKETT + SicherungWorker.ART_IMPORT in auftrag.tags ->
                            "Import abgebrochen. Bereits übernommene Ausgaben sind vollständig; ein erneuter Import setzt fort. Lokal wurde nichts überschrieben oder gelöscht."
                        else -> "Sicherung abgebrochen. Die unvollständige Datei wurde nach Möglichkeit entfernt; lokal wurde nichts verändert."
                    }
                    else -> null
                }
                val vollstaendig = auftrag?.state == WorkInfo.State.SUCCEEDED && auftrag.outputData.getBoolean(SicherungWorker.K_VOLLSTAENDIG, false)
                val meldung = startFehler ?: ergebnis
                if (meldung != null) {
                    Text(
                        meldung,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            startFehler != null -> MaterialTheme.colorScheme.error
                            vollstaendig -> MaterialTheme.colorScheme.primary
                            auftrag?.state == WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { runCatching { sichern.launch(sicherungsName()) } }, shape = RoundedCornerShape(50)) { Text("Archiv sichern") }
                    OutlinedButton(onClick = { runCatching { pruefen.launch(zipTypen) } }, shape = RoundedCornerShape(50)) { Text("Sicherung prüfen") }
                    OutlinedButton(onClick = { runCatching { importieren.launch(zipTypen) } }, shape = RoundedCornerShape(50)) { Text("Archiv importieren") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Die App bestätigt nur die geschriebene und vollständig zurückgelesene Datei. Das Hochladen in eine Cloud übernimmt der gewählte Anbieter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun sicherungsName(): String =
    "NewsKompass-Archiv-" + java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.GERMANY).format(java.util.Date()) + ".zip"

@Composable
private fun ZeitplanBereich(app: NewsApplication, activity: ComponentActivity, stand: EinstellungenStand) {
    val kontext = LocalContext.current
    Column {
        Abschnitt("Zeitplan")
        Kachel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatisch aktualisieren", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Rhythmus und Uhrzeiten stellst du oben bei jedem Thema ein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val naechster = if (stand.zeitplanAktiv) Zeitplan.naechsterTermin(stand.themen) else null
                    if (naechster != null) {
                        val wann = naechster.zeit.format(DateTimeFormatter.ofPattern("EEEE, dd.MM., HH:mm 'Uhr'", Locale.GERMANY))
                        Text("Nächste Ausgabe: $wann", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = stand.zeitplanAktiv, onCheckedChange = {
                    app.einstellungen.setzeZeitplan(it)
                    Zeitplan.plane(kontext)
                })
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { Zeitplan.starteLauf(kontext, manuell = true) }, shape = RoundedCornerShape(50)) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Jetzt eine Ausgabe erstellen")
            }
            if (stand.zeitplanAktiv) {
                Spacer(Modifier.height(16.dp))
                BereitschaftsKarte(activity)
            }
        }
    }
}

/**
 * Bereitschaft wie beim Genialen Wecker: jede Freigabe mit Häkchen oder Kreis, daneben der eine
 * Knopf, der direkt zur passenden Systemseite führt. Ohne Akku-Ausnahme darf der Lauf nach dem
 * Wecker oft keinen Vordergrunddienst mehr starten und stirbt nach 10 Minuten.
 */
@Composable
private fun BereitschaftsKarte(activity: ComponentActivity) {
    val freigaben = rememberBereitschaft(activity)
    val benachrichtigungen = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erteilt ->
        Bereitschaft.benachrichtigungsAntwort(activity, erteilt)
    }
    val fehlt = freigaben.count { !it.second }
    Text("Bereitschaft", style = MaterialTheme.typography.titleMedium)
    Text(
        if (fehlt == 0) {
            "Alles bereit: Die Ausgaben kommen pünktlich, auch wenn die App geschlossen ist."
        } else {
            "$fehlt ${if (fehlt == 1) "Freigabe fehlt" else "Freigaben fehlen"}. Tippe jeweils auf „Erlauben“."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (fehlt == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
    )
    freigaben.forEach { (name, bereit) ->
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (bereit) "✓" else "○",
                color = if (bereit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium)
                // Vorher lesen, was gleich zu tun ist — nie suchen müssen.
                if (!bereit) {
                    Text(Bereitschaft.anleitung(activity, name), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (bereit) {
                Text("erteilt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                TextButton(onClick = {
                    Toast.makeText(activity, Bereitschaft.anleitung(activity, name), Toast.LENGTH_LONG).show()
                    if (!Bereitschaft.behebe(activity, name) { benachrichtigungen.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Toast.makeText(activity, "Diese Einstellungsseite gibt es auf dem Gerät nicht. Öffne die App-Info.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Erlauben") }
            }
        }
    }
    OutlinedButton(onClick = { Bereitschaft.oeffneAppInfo(activity) }, shape = RoundedCornerShape(50)) { Text("App-Info öffnen") }
    Text(
        "Samsung: Zusätzlich unter Einstellungen › Akku › Hintergrundnutzung begrenzen › Nie schlafende Apps News Kompass eintragen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun DesignBereich(app: NewsApplication, stand: EinstellungenStand) {
    Column {
        Abschnitt("Darstellung")
        Kachel {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DesignModus.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = stand.design == m,
                        onClick = { app.einstellungen.setzeDesign(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, DesignModus.entries.size),
                    ) { Text(m.label) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i -> Box(Modifier.size(22.dp).clip(CircleShape).background(blockFarbe(i))) }
            }
        }
    }
}
