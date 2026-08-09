package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import de.frank.experimente.data.local.Aufgabe
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Sprechziel
import de.frank.experimente.ui.components.AktionsKnopf
import de.frank.experimente.ui.components.Fehlerkarte
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.Lautsprecher
import de.frank.experimente.ui.components.ObereLeiste
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.SymbolKnopf
import de.frank.experimente.ui.components.TextKnopf
import de.frank.experimente.ui.components.Wartezustand
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.LocalReduzierteBewegung
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.dauer
import kotlinx.coroutines.flow.flowOf

/**
 * **B-03 — Auswertung** (F-10, F-11, F-12, F-13).
 *
 * Zeigt die offenen Experimente **der Reihe nach**: einsprechen, Text bearbeiten, „Weiter“,
 * darunter die Sicht der KI. „Überspringen“ geht zum nächsten, „Fertig“ zurück auf B-01.
 *
 * Kommt der Bildschirm über „Nicht umgesetzt“ (F-13) von B-01, steht genau **ein**
 * Experiment an und die Auswertung wird als *nicht umgesetzt* gespeichert.
 */
@Composable
fun AuswertungBildschirm(
    vm: AppViewModel,
    aufZurueck: () -> Unit,
    modifier: Modifier = Modifier,
    nurExperimentId: Long? = null,
    nichtUmgesetzt: Boolean = false,
) {
    val farben = LocalAppColors.current
    val offene by vm.offeneExperimente.collectAsState()
    val gruppen by vm.tagesaufgaben.collectAsState()
    val heute by vm.heute.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()
    val laedt by vm.laedt.collectAsState()
    val liestVor by vm.liestVor.collectAsState()
    val fehler by vm.fehler.collectAsState()

    val reihe = if (nurExperimentId == null) offene else offene.filter { it.id == nurExperimentId }
    var index by remember { mutableStateOf(0) }
    val experiment = reihe.getOrNull(index)

    val auswertungen by remember(experiment?.id) {
        experiment?.let { vm.auswertungen(it.id) } ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        ObereLeiste(
            titel = "Wie ist es gelaufen?",
            links = {
                SymbolKnopf(
                    symbol = Icons.AutoMirrored.Filled.ArrowBack,
                    beschreibung = "Zurück",
                    onClick = aufZurueck,
                    tint = farben.text,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Mass.seitenrand, vertical = Mass.abschnittAbstand),
        ) {
            if (experiment == null) {
                AktionsKnopf(text = "Fertig", onClick = aufZurueck)
                return@Column
            }

            EinExperiment(
                vm = vm,
                experiment = experiment,
                aufgaben = gruppen.firstOrNull { it.first.id == experiment.id }?.second.orEmpty(),
                tagNummer = vm.ablage.tagNummer(experiment, heute),
                kiText = auswertungen.firstOrNull { it.datum == heute }?.kiText,
                nimmtAuf = nimmtAuf,
                laedt = laedt,
                liestVor = liestVor,
                fehler = fehler,
                nichtUmgesetzt = nichtUmgesetzt,
            )

            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                TextKnopf(text = "Überspringen", onClick = { index += 1 })
            }
        }
    }
}

/** Ein Durchgang: einsprechen → Text → „Weiter“ → die Sicht der KI. */
@Composable
private fun EinExperiment(
    vm: AppViewModel,
    experiment: Experiment,
    aufgaben: List<Aufgabe>,
    tagNummer: Int,
    kiText: String?,
    nimmtAuf: Boolean,
    laedt: Boolean,
    liestVor: Boolean,
    fehler: String?,
    nichtUmgesetzt: Boolean,
) {
    val farben = LocalAppColors.current
    var text by remember(experiment.id) { mutableStateOf("") }
    var zeigtText by remember(experiment.id) { mutableStateOf(false) }
    var gesendet by remember(experiment.id) { mutableStateOf(false) }

    Text(experiment.titel, style = AppTypo.kartentitel, color = farben.text)

    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(Mass.seitenrand),
    ) {
        if (experiment.tage > 1) {
            Text("Tag $tagNummer von ${experiment.tage}", style = AppTypo.daten, color = farben.gedaempft)
        }
        if (aufgaben.isNotEmpty()) {
            Text(
                text = "${aufgaben.count { it.erledigtAm != null }} von ${aufgaben.size} erledigt",
                style = AppTypo.daten,
                color = farben.gedaempft,
            )
        }
    }

    if (!zeigtText) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = Mass.abschnittAbstand),
            contentAlignment = Alignment.Center,
        ) {
            Sprechknopf(
                laeuftAufnahme = nimmtAuf,
                onKlick = {
                    vm.auswertungSprechen(experiment) { neu ->
                        text = neu
                        zeigtText = true
                    }
                },
            )
        }
    } else {
        Textfeld(
            wert = text,
            platzhalter = "",
            onWert = { text = it },
            modifier = Modifier.padding(top = Mass.abschnittAbstand),
            minHoehe = 132.dp,
        )
        Row(
            modifier = Modifier.padding(top = Mass.kartenAbstand),
            horizontalArrangement = Arrangement.spacedBy(Mass.kartenAbstand),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hatFassung = vm.hatVerbesserung(Sprechziel.AUSWERTUNG)
            TextKnopf(
                text = if (hatFassung) "Zurücknehmen" else "Text mit KI verbessern",
                onClick = {
                    if (hatFassung) {
                        vm.verbesserungZuruecknehmen(Sprechziel.AUSWERTUNG) { text = it }
                    } else {
                        vm.textVerbessern(Sprechziel.AUSWERTUNG, text) { text = it }
                    }
                },
            )
            AktionsKnopf(
                text = "Weiter",
                onClick = {
                    gesendet = true
                    vm.auswerten(experiment, text, nichtUmgesetzt)
                },
            )
        }
    }

    if (!gesendet) return

    Column(Modifier.padding(top = Mass.abschnittAbstand)) {
        when {
            fehler != null -> Fehlerkarte(
                text = fehler,
                onNochmal = {
                    vm.fehlerVerwerfen()
                    gesendet = false
                    zeigtText = false
                    text = ""
                },
            )

            kiText != null -> Antwortkarte(kiText, liestVor) { vm.vorlesen(kiText) }

            laedt -> Karte { Wartezustand() }
        }
    }
}

/**
 * Die Sicht der KI. **M-08** — die Karte blendet in 400 ms `antwort` auf und gibt sich dabei
 * von oben nach unten frei.
 */
@Composable
private fun Antwortkarte(text: String, liestVor: Boolean, onVorlesen: () -> Unit) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    var sichtbar by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text) { sichtbar = true }
    val anteil by animateFloatAsState(
        targetValue = if (sichtbar) 1f else 0f,
        animationSpec = tween(dauer(Bewegung.ANTWORT_MS, reduziert), easing = Bewegung.antwort),
        label = "M-08",
    )

    Box(
        Modifier
            .alpha(anteil)
            .drawWithContent {
                clipRect(bottom = size.height * anteil) { this@drawWithContent.drawContent() }
            },
    ) {
        Karte {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = 10.dp, y = (-10).dp)
                    .defaultMinSize(minHeight = Mass.tippflaeche),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = farben.aktion,
                    modifier = Modifier.size(Mass.symbolKarte),
                )
                Lautsprecher(laeuft = liestVor, onClick = onVorlesen)
            }
            Text(
                text = text,
                style = AppTypo.fliesstext,
                color = farben.text,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
