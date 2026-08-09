package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.Vorschlag
import de.frank.experimente.data.repo.Ablage
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.Sprechziel
import de.frank.experimente.ui.TagesStand
import de.frank.experimente.ui.components.AktionsKnopf
import de.frank.experimente.ui.components.AufgabenZeile
import de.frank.experimente.ui.components.Etikett
import de.frank.experimente.ui.components.Fehlerkarte
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.MerkenSymbol
import de.frank.experimente.ui.components.ObereLeiste
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.SymbolKnopf
import de.frank.experimente.ui.components.TextKnopf
import de.frank.experimente.ui.components.Wartezustand
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.AppForm
import de.frank.experimente.ui.theme.AppTypo
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.LocalReduzierteBewegung
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.staffel
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATUM = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

/**
 * **B-01 — Heute.** Der Startbildschirm und das Rückgrat der App.
 *
 * Wechselt mit dem Tagesstand: Lage einsprechen → fünf Vorschläge → laufende Experimente
 * mit der To-Do-Liste → abends die Auswertung.
 */
@Composable
fun HeuteBildschirm(
    vm: AppViewModel,
    aufEinstellungen: () -> Unit,
    aufGespraech: (Long) -> Unit,
    aufAuswertung: () -> Unit,
    aufNichtUmgesetzt: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val farben = LocalAppColors.current
    val stand by vm.stand.collectAsState()
    val laedt by vm.laedt.collectAsState()
    val fehler by vm.fehler.collectAsState()
    val nimmtAuf by vm.nimmtAuf.collectAsState()
    val heute by vm.heute.collectAsState()
    val erscheinung by vm.erscheinung.collectAsState()
    val vorschlaege by vm.vorschlaege.collectAsState()
    val aufgabenGruppen by vm.tagesaufgaben.collectAsState()
    val lageText by vm.lageText.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(farben.grund)) {
        ObereLeiste(
            titel = "Heute",
            rechts = {
                Row {
                    // F-26 — Schnellschalter: Hell → Dunkel → Wie das System
                    SymbolKnopf(
                        symbol = when (erscheinung) {
                            Erscheinung.HELL -> Icons.Filled.LightMode
                            Erscheinung.DUNKEL -> Icons.Filled.Bedtime
                            Erscheinung.SYSTEM -> Icons.Filled.Brightness6
                        },
                        beschreibung = "Erscheinung umschalten",
                        onClick = vm::erscheinungWeiterschalten,
                    )
                    SymbolKnopf(
                        symbol = Icons.Outlined.Settings,
                        beschreibung = "Einstellungen",
                        onClick = aufEinstellungen,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Mass.seitenrand, end = Mass.seitenrand, bottom = 32.dp,
            ),
        ) {
            item {
                Text(
                    text = heute.format(DATUM),
                    style = AppTypo.daten,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }

            if (fehler != null) {
                item {
                    Fehlerkarte(
                        text = fehler!!,
                        onNochmal = { vm.fehlerVerwerfen() },
                        modifier = Modifier.padding(bottom = Mass.kartenAbstand),
                    )
                }
            }

            if (laedt) {
                item { Wartezustand(modifier = Modifier.padding(bottom = 24.dp)) }
            }

            when (stand) {
                TagesStand.LEER, TagesStand.AUFNAHME -> item {
                    LageEinsprechen(vm, nimmtAuf)
                }

                TagesStand.LAGE_STEHT -> item {
                    LageBearbeiten(vm, lageText)
                }

                TagesStand.VORSCHLAEGE -> {
                    item {
                        Zwischenueberschrift(
                            "Fünf Vorschläge für heute",
                            Modifier.padding(bottom = 12.dp),
                        )
                    }
                    itemsIndexed(vorschlaege, key = { _, v -> v.id }) { index, vorschlag ->
                        VorschlagsKarte(
                            vorschlag = vorschlag,
                            index = index,
                            onWaehlen = { vm.experimentStarten(vorschlag) },
                            onMerken = { vm.merken(vorschlag) },
                        )
                    }
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextKnopf(
                                text = "Andere Vorschläge",
                                onClick = vm::vorschlaegeAktualisieren,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                TagesStand.LAEUFT -> {
                    item {
                        Zwischenueberschrift(
                            if (aufgabenGruppen.size >= Ablage.MAX_OFFEN) {
                                "Läuft (${aufgabenGruppen.size} von ${Ablage.MAX_OFFEN})"
                            } else {
                                "Läuft"
                            },
                            Modifier.padding(bottom = 12.dp),
                        )
                    }
                    items(aufgabenGruppen, key = { it.first.id }) { (experiment, _) ->
                        LaufendesExperiment(
                            experiment = experiment,
                            tagNummer = vm.ablage.tagNummer(experiment, heute),
                            onGespraech = { aufGespraech(experiment.id) },
                            onNichtUmgesetzt = { aufNichtUmgesetzt(experiment.id) },
                        )
                    }
                    if (aufgabenGruppen.size >= Ablage.MAX_OFFEN) {
                        item {
                            Text(
                                text = "Drei Experimente laufen. Schließ eines ab, bevor du ein neues beginnst.",
                                style = AppTypo.fliesstextKlein,
                                color = farben.gedaempft,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }

                    item {
                        Zwischenueberschrift(
                            "Heute zu tun",
                            Modifier.padding(top = Mass.abschnittAbstand, bottom = 8.dp),
                        )
                    }
                    // EINE Liste für den Tag, nach Experimenten gruppiert (F-07)
                    aufgabenGruppen.forEach { (experiment, aufgaben) ->
                        item(key = "kopf-${experiment.id}") {
                            Text(
                                text = experiment.titel,
                                style = AppTypo.fliesstextKlein,
                                color = farben.gedaempft,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        items(aufgaben, key = { "a-${it.id}" }) { a ->
                            AufgabenZeile(
                                text = a.text,
                                erledigt = a.erledigtAm != null,
                                onClick = { vm.hakenUmschalten(a) },
                            )
                        }
                    }

                    item {
                        AktionsKnopf(
                            text = "Wie ist es gelaufen?",
                            onClick = aufAuswertung,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Mass.abschnittAbstand),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LageEinsprechen(vm: AppViewModel, nimmtAuf: Boolean) {
    val farben = LocalAppColors.current
    var tippen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Wie ist deine Lage heute?",
            style = AppTypo.abschnittstitel,
            color = farben.text,
        )
        Text(
            text = "Was für ein Tag ist das? Was liegt vor dir?",
            style = AppTypo.fliesstextKlein,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )

        if (tippen) {
            Textfeld(
                wert = vm.lageText.collectAsState().value,
                platzhalter = "Sprich einfach drauflos — oder tipp es hier.",
                onWert = vm::lageSetzen,
            )
            AktionsKnopf(
                text = "Weiter",
                onClick = vm::lageBestaetigen,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        } else {
            Sprechknopf(laeuftAufnahme = nimmtAuf, onKlick = vm::lageSprechen)
            TextKnopf(
                text = "Lieber tippen",
                onClick = { tippen = true },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun LageBearbeiten(vm: AppViewModel, text: String) {
    Column(Modifier.fillMaxWidth()) {
        Textfeld(wert = text, platzhalter = "", onWert = vm::lageSetzen)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hatFassung = vm.hatVerbesserung(Sprechziel.LAGE)
            TextKnopf(
                text = if (hatFassung) "Zurücknehmen" else "Text mit KI verbessern",
                onClick = {
                    if (hatFassung) {
                        vm.verbesserungZuruecknehmen(Sprechziel.LAGE, vm::lageSetzen)
                    } else {
                        vm.textVerbessern(Sprechziel.LAGE, text, vm::lageSetzen)
                    }
                },
            )
            AktionsKnopf(text = "Weiter", onClick = vm::lageBestaetigen)
        }
    }
}

@Composable
fun Textfeld(
    wert: String,
    platzhalter: String,
    onWert: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHoehe: androidx.compose.ui.unit.Dp = 120.dp,
) {
    val farben = LocalAppColors.current
    var feld by remember(wert) { mutableStateOf(TextFieldValue(wert)) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(minHoehe)
            .clip(AppForm.eingabefeld)
            .background(farben.erhoeht)
            .border(Mass.randstaerke, farben.rand, AppForm.eingabefeld)
            .padding(16.dp),
    ) {
        if (feld.text.isEmpty() && platzhalter.isNotEmpty()) {
            Text(platzhalter, style = AppTypo.fliesstext, color = farben.blass)
        }
        BasicTextField(
            value = feld,
            onValueChange = { feld = it; onWert(it.text) },
            textStyle = AppTypo.fliesstext.copy(color = farben.text),
            cursorBrush = SolidColor(farben.aktion),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** **M-04** — die Karte erscheint mit 40 ms Versatz je Position, 240 ms `ruhig`, aus 8 dp unten. */
@Composable
private fun VorschlagsKarte(
    vorschlag: Vorschlag,
    index: Int,
    onWaehlen: () -> Unit,
    onMerken: () -> Unit,
) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    var sichtbar by remember { mutableStateOf(false) }
    LaunchedEffect(vorschlag.id) { sichtbar = true }

    val anteil by animateFloatAsState(
        targetValue = if (sichtbar) 1f else 0f,
        animationSpec = tween(
            durationMillis = dauer(Bewegung.RUHIG_MS, reduziert),
            delayMillis = staffel(index, reduziert),
            easing = Bewegung.ruhig,
        ),
        label = "M-04",
    )

    Box(
        Modifier
            .alpha(anteil)
            .padding(bottom = Mass.kartenAbstand),
    ) {
        Karte(onClick = onWaehlen) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(vorschlag.titel, style = AppTypo.kartentitel, color = farben.text)
                    Text(
                        text = vorschlag.beschreibung,
                        style = AppTypo.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Etikett(vorschlag.stufe.anzeige)
                        Etikett(if (vorschlag.tage == 1) "1 Tag" else "${vorschlag.tage} Tage")
                        if (vorschlag.vonMerkliste) {
                            Etikett("von deiner Merkliste", hervorgehoben = true)
                        }
                    }
                }
                MerkenSymbol(gemerkt = false, onClick = onMerken)
            }
        }
    }
}

@Composable
private fun LaufendesExperiment(
    experiment: Experiment,
    tagNummer: Int,
    onGespraech: () -> Unit,
    onNichtUmgesetzt: () -> Unit,
) {
    val farben = LocalAppColors.current
    Box(Modifier.padding(bottom = Mass.kartenAbstand)) {
        Karte {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(experiment.titel, style = AppTypo.kartentitel, color = farben.text)
                    Text(
                        text = if (experiment.tage > 1) "Tag $tagNummer von ${experiment.tage}" else experiment.stufe.anzeige,
                        style = AppTypo.daten,
                        color = farben.gedaempft,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                SymbolKnopf(
                    symbol = Icons.Outlined.ChatBubbleOutline,
                    beschreibung = "Gespräch",
                    onClick = onGespraech,
                )
            }
            // F-13 — der manuelle Weg, ein Experiment als nicht umgesetzt abzuschließen.
            TextKnopf(text = "Nicht umgesetzt", onClick = onNichtUmgesetzt)
        }
    }
}
