package de.frank.newskompass.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
                onDrop = { app.einstellungen.setzeThemen(reihenfolge.mapNotNull(nachSchluessel::get)) },
                reducedMotion = false,
            ),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "themen-titel") {
                Abschnitt(
                    "Deine Themen",
                    "Ein Stichwort, eine Frage oder ein ganzer Satz — jedes Feld wird ein eigener Nachrichtenblock. " +
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
            item(key = "zeitplan") { Breite { ZeitplanBereich(app, stand) } }
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
    aendere: (String) -> Unit,
    aendereBereich: (Int, Int) -> Unit,
    loesche: () -> Unit,
) {
    var text by remember(thema.id) { mutableStateOf(thema.text) }
    var bereichOffen by remember(thema.id) { mutableStateOf(false) }
    val fokus = remember { FocusRequester() }
    LaunchedEffect(fokussieren) { if (fokussieren) runCatching { fokus.requestFocus() } }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(if (gezogen) 14.dp else 0.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    griff.padding(top = 10.dp, start = 6.dp).size(width = 40.dp, height = 52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.DragIndicator, "Verschieben", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    Modifier.padding(top = 22.dp).size(28.dp).clip(RoundedCornerShape(9.dp)).background(blockVerlauf(nummer)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${nummer + 1}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
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
                if (loeschbar) {
                    IconButton(onClick = loesche, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Thema löschen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            AssistChip(
                onClick = { bereichOffen = true },
                label = { Text(meldungsBereich(thema.minMeldungen, thema.maxMeldungen)) },
                leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(AssistChipDefaults.IconSize)) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(start = 90.dp, bottom = 8.dp),
            )
        }
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
        Abschnitt("Ausführlichkeit", "Wie lang jede Meldung geschrieben und vorgelesen wird — für alle Themen und deine gesprochenen Fragen.")
        Kachel {
            Ausfuehrlichkeit.entries.forEach { stufe ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { app.einstellungen.setzeAusfuehrlichkeit(stufe) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = stand.ausfuehrlichkeit == stufe, onClick = { app.einstellungen.setzeAusfuehrlichkeit(stufe) })
                    Column {
                        Text(stufe.label, style = MaterialTheme.typography.titleMedium)
                        Text(stufe.erklaerung, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Gilt ab dem nächsten Lauf; ausführlichere Meldungen brauchen etwas länger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
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
private fun ZeitplanBereich(app: NewsApplication, stand: EinstellungenStand) {
    val kontext = LocalContext.current
    Column {
        Abschnitt("Zeitplan")
        Kachel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatisch um 5 und 17 Uhr", style = MaterialTheme.typography.titleMedium)
                    if (stand.zeitplanAktiv) {
                        val naechster = Zeitplan.naechsterTermin().format(DateTimeFormatter.ofPattern("EEEE, HH:mm 'Uhr'", Locale.GERMANY))
                        Text("Nächste Ausgabe: $naechster", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        }
    }
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
