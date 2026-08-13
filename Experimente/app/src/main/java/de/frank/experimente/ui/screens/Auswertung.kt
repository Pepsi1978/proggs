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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.frank.experimente.ui.KENNUNG_EINSCHAETZUNG
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Tagewahl
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Vorleseknopf
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
    val abschnitte by modell.einschaetzung.collectAsStateWithLifecycle()
    val mitlese by modell.mitlese.collectAsStateWithLifecycle()
    val liestVor by modell.liestVor.collectAsStateWithLifecycle()
    val bluete by modell.bluete.collectAsStateWithLifecycle()
    val hinweis by modell.hinweis.collectAsStateWithLifecycle()
    val stoerung by modell.stoerung.collectAsStateWithLifecycle()
    val frueher by modell.bisherigeAuswertungen.collectAsStateWithLifecycle()
    val liest by modell.liestKennung.collectAsStateWithLifecycle()

    val letzterTag by modell.letzterTagErreicht.collectAsStateWithLifecycle()

    // Nicht aus `laufende` gefischt: sobald die letzte Auswertung steht, ist das Experiment
    // abgeschlossen und fällt dort heraus — der Titel wurde dann zu „Experiment".
    val experiment by modell.ausgewertetes.collectAsStateWithLifecycle()

    var verlaengernOffen by remember { mutableStateOf(false) }
    if (verlaengernOffen) {
        Verlaengern(
            jetzigeTage = experiment?.days ?: 1,
            beiSchliessen = { verlaengernOffen = false },
            beiWahl = { zusaetzlich ->
                verlaengernOffen = false
                modell.fuehreFort(zusaetzlich)
            },
        )
    }

    Bildschirmgeruest(
        kopfInnen = 8.dp,
        kopfLinks = { Rundknopf(Symbole.Zurueck, "Zurück", modell::zurueckTippen) },
        kopf = { Titel("Auswertung", klein = true) },
        ueberlagerung = {
            Meldungen(stoerung = stoerung, beiNochmal = modell::schliesseMeldung, hinweis = hinweis)
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
                        Vorleseknopf(
                            spricht = liestVor && liest == KENNUNG_EINSCHAETZUNG,
                            beiKlick = { modell.lies(abschnitte.joinToString("")) },
                            beschriftung = "Einschätzung vorlesen",
                        )
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

                    // --- Wie geht es weiter? ------------------------------------------
                    //
                    // **Die App beendet nichts von selbst.** Vorher schloss die Auswertung am
                    // letzten Tag das Experiment stillschweigend ab — wer bei „Tag 2 von 2"
                    // erzählte, wie es lief, hatte es damit beendet, auch wenn er es
                    // fortführen wollte. Hier wird gefragt.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .height(1.dp)
                            .background(farben.randWeich),
                    )
                    Text(
                        text = if (letzterTag) {
                            "Der letzte geplante Tag ist erreicht. Wie soll es weitergehen?"
                        } else {
                            "Wie soll es weitergehen?"
                        },
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KnopfUmrandet(
                            text = "Weiterführen",
                            beiKlick = { verlaengernOffen = true },
                            modifier = Modifier.weight(1f),
                            farbe = farben.aktion,
                        )
                        KnopfBetont(
                            text = "Abschließen",
                            beiKlick = modell::schliesseAb,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Textknopf(
                            text = if (letzterTag) "Später entscheiden" else "Zwischenstand",
                            beiKlick = modell::behalteOffen,
                        )
                        Box(Modifier.weight(1f))
                        // F-13, zweiter Weg. Er war im Modell fertig gebaut, aber von keinem
                        // Knopf aus erreichbar — hier ist er.
                        experiment?.let { offenes ->
                            Textknopf(
                                text = "Nicht umgesetzt",
                                beiKlick = { modell.nichtUmgesetzt(offenes) },
                                farbe = farben.gedaempft,
                            )
                        }
                    }
                }
            }
        }

        // --- Was bisher gesagt wurde -------------------------------------------------
        //
        // Der eingesprochene Text war nach „Weiter" nicht mehr auffindbar: er lag in der
        // Ablage, aber kein Bildschirm zeigte ihn. Hier steht er, Tag für Tag.
        val vergangene = frueher.filter { it.ownText.isNotBlank() }
        if (vergangene.isNotEmpty()) {
            item("kopf-bisher") {
                Text(
                    text = "Bisher gesagt".uppercase(),
                    style = schriften.zwischenueberschrift,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            items(vergangene, key = { "aus${it.id}" }) { eintrag ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(farben.flaeche)
                        .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = TAGESFORM_AUSWERTUNG.format(eintrag.date),
                            style = schriften.stufe,
                            color = farben.aktion,
                        )
                        Vorleseknopf(
                            spricht = liest == "frueher-${eintrag.id}",
                            beiKlick = {
                                modell.liesVor(
                                    "frueher-${eintrag.id}",
                                    buildString {
                                        append(eintrag.ownText)
                                        eintrag.aiText?.takeIf { it.isNotBlank() }?.let {
                                            append("\n\nEinschätzung: ")
                                            append(it)
                                        }
                                    },
                                )
                            },
                            beschriftung = "Diese Auswertung vorlesen",
                        )
                    }
                    Text(
                        text = eintrag.ownText,
                        style = schriften.fliesstext,
                        color = farben.text,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    eintrag.aiText?.takeIf { it.isNotBlank() }?.let { einschaetzung ->
                        Text(
                            text = "Einschätzung".uppercase(),
                            style = schriften.zwischenueberschrift,
                            color = farben.gedaempft,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            text = einschaetzung,
                            style = schriften.kartentext,
                            color = farben.gedaempft,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private val TAGESFORM_AUSWERTUNG: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", java.util.Locale.GERMAN)

/**
 * „Weiterführen" — um wie viele Tage soll es weitergehen?
 *
 * Gefragt wird nach den **zusätzlichen** Tagen, nicht nach der neuen Gesamtdauer: aus Franks
 * Sicht geht es weiter, und „noch drei Tage" ist die Zahl, die er im Kopf hat.
 */
@Composable
private fun Verlaengern(jetzigeTage: Int, beiWahl: (Int) -> Unit, beiSchliessen: () -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    var zusaetzlich by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = beiSchliessen,
        containerColor = farben.flaeche,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Weiterführen", style = schriften.kartentitel, color = farben.text) },
        text = {
            Column {
                Text(
                    text = "Bisher $jetzigeTage ${if (jetzigeTage == 1) "Tag" else "Tage"} — " +
                        "wie viele sollen dazukommen?",
                    style = schriften.fliesstextKlein,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Tagewahl(
                    tage = zusaetzlich,
                    beiAenderung = { zusaetzlich = it },
                    beschriftung = "Zusätzliche Tage",
                    groesstes = 60 - jetzigeTage.coerceAtMost(59),
                )
                Text(
                    text = "Danach läuft es ${jetzigeTage + zusaetzlich} Tage. " +
                        "Die Aufgaben für die neuen Tage kommen dazu.",
                    style = schriften.stufe,
                    color = farben.blass,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { beiWahl(zusaetzlich) }) {
                Text("Weiterführen", style = schriften.knopf, color = farben.aktion)
            }
        },
        dismissButton = {
            TextButton(onClick = beiSchliessen) {
                Text("Abbrechen", style = schriften.knopf, color = farben.gedaempft)
            }
        },
    )
}
