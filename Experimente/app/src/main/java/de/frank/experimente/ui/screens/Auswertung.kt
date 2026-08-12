package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.AuswertungZustand
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Titel
import de.frank.experimente.ui.components.Wartekarte
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Lichtbluete
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.lichtsaum

/**
 * **B-03 — Auswertung** (F-10 bis F-13).
 *
 * Vier Zustände: einsprechen · Text bearbeiten · warten · Antwort. Die KI-Einschätzung
 * bekommt beim Vorlesen den Mitlese-Streifen (`E-21`), und „Fertig" schließt das Experiment
 * mit der Lichtblüte ab (`E-16` · M-93).
 */
@Composable
fun Auswertung(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    val zustand by modell.auswertungsZustand.collectAsStateWithLifecycle()
    val feld by modell.auswertungsFeld.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val laufende by modell.laufende.collectAsStateWithLifecycle()
    val wertetAus by modell.wertetAus.collectAsStateWithLifecycle()
    val abschnitte by modell.einschaetzung.collectAsStateWithLifecycle()
    val mitlese by modell.mitlese.collectAsStateWithLifecycle()
    val liestVor by modell.liestVor.collectAsStateWithLifecycle()
    val bluete by modell.bluete.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()

    val experiment = laufende.firstOrNull { it.id == wertetAus }

    Bildschirmgeruest(
        kopfInnen = 8.dp,
        kopfLinks = { Rundknopf(Symbole.Zurueck, "Zurück", modell::zurueck) },
        kopf = { Titel("Auswertung", klein = true) },
        ueberlagerung = {
            Meldungen(stoerung = null, beiNochmal = modell::schliesseMeldung, hinweis = meldung)
            if (bluete) {
                val stufe = LocalEffektstufe.current
                val weg by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(Bewegung.BLUETE, easing = Bewegung.ruhig),
                    label = "bluete",
                )
                Lichtbluete(farben, stufe, weg, Modifier)
            }
        },
    ) {
        item("titel") {
            Column {
                Text(
                    text = experiment?.title ?: "Experiment",
                    style = schriften.kartentitel,
                    color = farben.text,
                )
                Text(
                    text = experiment?.let { "${modell.tagText(it)} · ${modell.stufenwort(it)}" }.orEmpty(),
                    style = schriften.daten,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        when (zustand) {
            AuswertungZustand.AUFNAHME -> item("aufnahme") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Sprechknopf(
                        nimmtAuf = nimmtAuf,
                        beschriftung = "Auswertung einsprechen",
                        beiKlick = {
                            modell.sprechknopf(modell.auswertungsFeldFluss()) { modell.auswertungTippen() }
                        },
                    )
                    Text(
                        text = "Erzähl, was daraus geworden ist.",
                        style = schriften.fliesstext,
                        color = farben.gedaempft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Textknopf("Lieber tippen", modell::auswertungTippen, Modifier.padding(top = 14.dp))
                }
            }

            AuswertungZustand.TEXT -> {
                item("feld") {
                    Eingabefeld(
                        text = feld.text,
                        beiAenderung = { modell.setzeText(modell.auswertungsFeldFluss(), it) },
                        platzhalter = "Was ist daraus geworden?",
                        mindesthoehe = 140.dp,
                        innen = 16.dp,
                    )
                }
                item("knoepfe") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Textknopf(
                            text = if (feld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                            beiKlick = {
                                if (feld.kannZurueck) modell.nimmZurueck(modell.auswertungsFeldFluss())
                                else modell.verbessere(modell.auswertungsFeldFluss())
                            },
                            aktiv = !feld.laeuft,
                        )
                        KnopfBetont("Weiter", modell::werteAus, Modifier.weight(1f), gross = true)
                    }
                }
            }

            AuswertungZustand.WARTET -> item("wartet") {
                Wartekarte("Ich denke darüber nach …")
            }

            AuswertungZustand.ANTWORT -> item("antwort") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(farben.flaeche)
                        .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
                        .lichtsaum(farben.text, 0.12f)
                        .padding(18.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Einschätzung".uppercase(),
                            style = schriften.zwischenueberschrift,
                            color = farben.aktion,
                        )
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { modell.lies(abschnitte.joinToString("")) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Symbole.Vorlesen,
                                contentDescription = "Auswertung vorlesen",
                                tint = if (liestVor) farben.aktion else farben.text,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    // E-21 — der gerade gesprochene Abschnitt wird hervorgehoben.
                    Text(
                        text = buildAnnotatedString {
                            abschnitte.forEachIndexed { nr, teil ->
                                if (nr == mitlese) {
                                    withStyle(
                                        SpanStyle(background = farben.aktion.copy(alpha = 0.26f)),
                                    ) { append(teil) }
                                } else {
                                    append(teil)
                                }
                            }
                        },
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    Textknopf(
                        text = "Nochmal versuchen",
                        beiKlick = modell::nochmalVersuchen,
                        symbol = Symbole.Auffrischen,
                        modifier = Modifier.padding(top = 14.dp),
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KnopfUmrandet(
                            text = "Überspringen",
                            beiKlick = modell::ueberspringeAuswertung,
                            modifier = Modifier.weight(1f),
                        )
                        KnopfBetont(
                            text = "Fertig",
                            beiKlick = modell::schliesseAb,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
