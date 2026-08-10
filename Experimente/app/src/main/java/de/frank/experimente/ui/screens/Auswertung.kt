package de.frank.experimente.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.Etikett
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.Kopfzeile
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Wartezustand
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer

/**
 * **B-03 — Auswertung.** Zeigt **alle offenen Experimente der Reihe nach** (F-10 Schritt 1),
 * je mit Titel, Tag und dem heutigen Haken-Stand. Zu jedem spricht Frank ein, danach läuft
 * die KI-Auswertung (F-11) und erscheint darunter mit Lautsprecher-Knopf (F-12).
 */
@Composable
fun Auswertung(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    val offene by modell.offene.collectAsStateWithLifecycle()
    val wertetAus by modell.wertetAus.collectAsStateWithLifecycle()
    val feld by modell.auswertungsFeld.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val wartet by modell.wartet.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    val auswertungen by modell.auswertungenHeute.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Kopfzeile(
            titel = "Wie ist es gelaufen?",
            links = {
                Box(Modifier.padding(end = 12.dp)) {
                    Rundknopf(
                        symbol = Symbole.ZurueckZuHeute,
                        beschriftung = "Zurück zu Heute",
                        beiKlick = modell::zurueck,
                    )
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (offene.isEmpty()) {
                Karte {
                    Text("Heute läuft kein Experiment.", style = schriften.fliesstext, color = farben.gedaempft)
                }
            }

            offene.forEach { experiment ->
                val dran = experiment.id == wertetAus
                val heutige = modell.heutigeAufgaben(experiment)
                val fertig = heutige.count { it.doneAt != null }
                val fertigeAuswertung = auswertungen.firstOrNull { it.experimentId == experiment.id }

                Karte(beiKlick = if (dran) null else ({ modell.waehleAuswertung(experiment) })) {
                    Text(experiment.title, style = schriften.kartentitel, color = farben.text)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Etikett(modell.tagText(experiment))
                        if (heutige.isNotEmpty()) Etikett("$fertig von ${heutige.size} erledigt")
                    }

                    if (fertigeAuswertung?.aiText != null) {
                        // M-10/M-08: die Auswertung erscheint unter Franks Text.
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(dauer(Bewegung.ANTWORT), easing = Bewegung.antwort)),
                        ) {
                            Column(Modifier.padding(top = 20.dp)) {
                                Zwischenueberschrift("Deine Worte")
                                Text(
                                    fertigeAuswertung.ownText,
                                    style = schriften.fliesstext,
                                    color = farben.gedaempft,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Zwischenueberschrift("Einschätzung", Modifier.weight(1f))
                                    Icon(
                                        imageVector = Symbole.AuswertungVorlesen,
                                        contentDescription = "Auswertung vorlesen",
                                        tint = farben.aktion,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { modell.lies(fertigeAuswertung.aiText) },
                                    )
                                }
                                Text(
                                    fertigeAuswertung.aiText,
                                    style = schriften.fliesstext,
                                    color = farben.text,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    } else if (dran) {
                        Column(Modifier.padding(top = 20.dp)) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Sprechknopf(
                                    nimmtAuf = nimmtAuf,
                                    beiKlick = { modell.sprechknopf(modell.auswertungsFeldFluss()) },
                                    beschriftung = "Auswertung einsprechen",
                                )
                            }
                            if (feld.text.isNotBlank()) {
                                Box(Modifier.padding(top = 20.dp)) {
                                    Eingabefeld(
                                        text = feld.text,
                                        beiAenderung = { modell.setzeText(modell.auswertungsFeldFluss(), it) },
                                        platzhalter = "Was ist daraus geworden?",
                                        zeilen = 4,
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Textknopf(
                                        text = if (feld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                                        beiKlick = {
                                            if (feld.kannZurueck) modell.nimmZurueck(modell.auswertungsFeldFluss())
                                            else modell.verbessere(modell.auswertungsFeldFluss())
                                        },
                                        modifier = Modifier.weight(1f),
                                        betont = false,
                                        aktiv = !feld.laeuft,
                                    )
                                    Textknopf("Fertig", modell::werteAus, Modifier.weight(1f))
                                }
                            }
                            // F-10: Frank darf einzelne Experimente überspringen; sie bleiben
                            // offen und erscheinen am nächsten Abend erneut.
                            Row(
                                Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Textknopf(
                                    text = "Überspringen",
                                    beiKlick = {
                                        offene.firstOrNull { it.id != experiment.id }
                                            ?.let(modell::waehleAuswertung)
                                    },
                                    betont = false,
                                )
                            }
                        }
                    }
                }
            }

            meldung?.let { text ->
                Karte { Text(text, style = schriften.fliesstext, color = farben.warnung) }
            }
            wartet?.let { Karte { Wartezustand(it) } }
            Box(Modifier.padding(bottom = 20.dp))
        }
    }
}
