package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.frank.experimente.data.local.GespraechsRunde
import de.frank.experimente.data.local.Sprecher
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.components.Fehlerkarte
import de.frank.experimente.ui.components.Lautsprecher
import de.frank.experimente.ui.components.ObereLeiste
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.SymbolKnopf
import de.frank.experimente.ui.theme.AppForm
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.LocalReduzierteBewegung
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.dauer

/** Abstand, den eine Blase zur gegenüberliegenden Kante hält — im Design `calc(100% - 48px)`. */
private val BLASEN_EINZUG = 48.dp

/**
 * **B-02 — Gespräch** (F-09).
 *
 * Der Faden gehört genau einem Experiment. Frank spricht, die Antwort erscheint und wird
 * sofort vorgelesen (das übernimmt das ViewModel). Der Lautsprecher an der Antwort ist der
 * zweite Versuch, wenn das Vorlesen scheitert.
 */
@Composable
fun GespraechBildschirm(
    vm: AppViewModel,
    experimentId: Long,
    aufZurueck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalAppColors.current
    val fluss = remember(experimentId) { vm.gespraech(experimentId) }
    val runden by fluss.collectAsState(initial = emptyList())
    val offene by vm.offeneExperimente.collectAsState()
    val heute by vm.heute.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()
    val laedt by vm.laedt.collectAsState()
    val liestVor by vm.liestVor.collectAsState()
    val fehler by vm.fehler.collectAsState()

    val experiment = offene.firstOrNull { it.id == experimentId }
    val wartet = laedt && runden.lastOrNull()?.sprecher == Sprecher.ICH
    var eingabe by remember { mutableStateOf("") }

    val listenStand = rememberLazyListState()
    val letzterIndex =
        (runden.size + (if (wartet) 1 else 0) + (if (fehler != null) 1 else 0) - 1).coerceAtLeast(0)
    LaunchedEffect(letzterIndex) { listenStand.animateScrollToItem(letzterIndex) }

    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        ObereLeiste(
            titel = "",
            links = {
                SymbolKnopf(
                    symbol = Icons.AutoMirrored.Filled.ArrowBack,
                    beschreibung = "Zurück zu Heute",
                    onClick = aufZurueck,
                    tint = farben.text,
                )
            },
        )

        if (experiment != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Mass.seitenrand)
                    .padding(top = 20.dp),
            ) {
                Text(experiment.titel, style = AppTypo.abschnittstitel, color = farben.text)
                Text(
                    text = if (experiment.tage > 1) {
                        "Tag ${vm.ablage.tagNummer(experiment, heute)} von ${experiment.tage}"
                    } else {
                        experiment.stufe.anzeige
                    },
                    style = AppTypo.daten,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        LazyColumn(
            state = listenStand,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = Mass.seitenrand,
                end = Mass.seitenrand,
                top = Mass.abschnittAbstand,
                bottom = Mass.seitenrand,
            ),
            verticalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
        ) {
            if (runden.isEmpty() && !wartet) {
                item {
                    Text(
                        text = "Frag mich, wie du das angehen könntest.",
                        style = AppTypo.fliesstext,
                        color = farben.gedaempft,
                    )
                }
            }

            items(runden, key = { it.id }) { runde ->
                Blase(
                    runde = runde,
                    liestVor = liestVor,
                    onVorlesen = { vm.vorlesen(runde.text) },
                )
            }

            if (wartet) {
                item(key = "wartet") { WarteBlase() }
            }

            if (fehler != null) {
                item(key = "fehler") {
                    Fehlerkarte(text = fehler!!, onNochmal = vm::fehlerVerwerfen)
                }
            }
        }

        Verfasserleiste(
            eingabe = eingabe,
            onEingabe = { eingabe = it },
            onSenden = {
                val text = eingabe.trim()
                if (text.isNotEmpty()) {
                    vm.gespraechSenden(experimentId, text)
                    eingabe = ""
                }
            },
            nimmtAuf = nimmtAuf,
            onSprechen = { vm.gespraechSprechen(experimentId) },
        )
    }
}

/**
 * Eine Runde des Fadens. „Ich“ steht rechts mit abgeflachter Ecke unten rechts, die KI links
 * mit abgeflachter Ecke unten links und 1 dp Rand.
 *
 * **M-08** — die KI-Antwort blendet in 400 ms `antwort` auf und gibt sich dabei von oben nach
 * unten frei (im Design `clip-path: inset(0 0 100% 0)` → `inset(0 0 0 0)`).
 */
@Composable
private fun Blase(runde: GespraechsRunde, liestVor: Boolean, onVorlesen: () -> Unit) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current

    if (runde.sprecher == Sprecher.ICH) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = BLASEN_EINZUG),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                Modifier
                    .clip(AppForm.blaseIch)
                    .background(farben.erhoeht)
                    .padding(Mass.karteInnen),
            ) {
                Text(runde.text, style = AppTypo.fliesstext, color = farben.text)
            }
        }
        return
    }

    var sichtbar by remember(runde.id) { mutableStateOf(false) }
    LaunchedEffect(runde.id) { sichtbar = true }
    val anteil by animateFloatAsState(
        targetValue = if (sichtbar) 1f else 0f,
        animationSpec = tween(dauer(Bewegung.ANTWORT_MS, reduziert), easing = Bewegung.antwort),
        label = "M-08",
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(end = BLASEN_EINZUG),
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            Modifier
                .alpha(anteil)
                .drawWithContent {
                    clipRect(bottom = size.height * anteil) { this@drawWithContent.drawContent() }
                }
                .clip(AppForm.blaseKi)
                .background(farben.flaeche)
                .border(Mass.randstaerke, farben.rand, AppForm.blaseKi)
                .padding(Mass.karteInnen),
        ) {
            Text(runde.text, style = AppTypo.fliesstext, color = farben.text)
            Lautsprecher(
                laeuft = liestVor,
                onClick = onVorlesen,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** Die Antwort ist unterwegs — drei ruhige Punkte in der Form der KI-Blase. */
@Composable
private fun WarteBlase() {
    val farben = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = BLASEN_EINZUG),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            Modifier
                .clip(AppForm.blaseKi)
                .background(farben.flaeche)
                .border(Mass.randstaerke, farben.rand, AppForm.blaseKi)
                .padding(Mass.karteInnen),
        ) {
            Row(
                modifier = Modifier.width(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(3) {
                    Text("•", style = AppTypo.fliesstext, color = farben.gedaempft)
                }
            }
        }
    }
}

/** Die Leiste unten: Textfeld und Sprechknopf, 96 dp hoch, beide 56 dp. */
@Composable
private fun Verfasserleiste(
    eingabe: String,
    onEingabe: (String) -> Unit,
    onSenden: () -> Unit,
    nimmtAuf: Boolean,
    onSprechen: () -> Unit,
) {
    val farben = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(farben.grund)
            .padding(Mass.seitenrand),
        horizontalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(Mass.sprechknopfKlein)
                .clip(AppForm.eingabefeld)
                .background(farben.erhoeht)
                .border(
                    width = Mass.randstaerke,
                    color = if (farben.istDunkel) Color.Transparent else farben.rand,
                    shape = AppForm.eingabefeld,
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (eingabe.isEmpty()) {
                Text("Oder tippen …", style = AppTypo.fliesstext, color = farben.blass)
            }
            BasicTextField(
                value = eingabe,
                onValueChange = onEingabe,
                singleLine = true,
                textStyle = AppTypo.fliesstext.copy(color = farben.text),
                cursorBrush = SolidColor(farben.aktion),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSenden() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Sprechknopf(laeuftAufnahme = nimmtAuf, onKlick = onSprechen, gross = false)
    }
}
