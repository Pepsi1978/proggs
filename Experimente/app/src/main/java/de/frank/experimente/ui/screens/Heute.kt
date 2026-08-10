package de.frank.experimente.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.Suggestion
import de.frank.experimente.data.local.Task
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.TagZustand
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.Etikett
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.Kopfzeile
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.UntereLeiste
import de.frank.experimente.ui.components.Wartezustand
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Erscheinung
import de.frank.experimente.ui.theme.Formen
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * **B-01 — Heute.** Der Startbildschirm, in sechs Zuständen (Funktions-Spec §4):
 * `LEER` · `AUFNAHME` · `LAGE_STEHT` · `VORSCHLAEGE` · `LAEUFT` · `ABEND`,
 * dazu *lädt*, *Fehler* und *kein Netz*.
 */
@Composable
fun Heute(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    val zustand by modell.tagZustand.collectAsStateWithLifecycle()
    val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
    val wartet by modell.wartet.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val vorschlaege by modell.vorschlaege.collectAsStateWithLifecycle()
    val offene by modell.offene.collectAsStateWithLifecycle()
    val lageFeld by modell.lageFeld.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Kopfzeile(
            titel = "Heute",
            rechts = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // F-26: der Schnellschalter kündigt den nächsten Modus an.
                    Rundknopf(
                        symbol = symbolFuerErscheinung(erscheinung.naechste()),
                        beschriftung = beschriftungFuer(erscheinung.naechste()),
                        beiKlick = modell::naechsteErscheinung,
                        farbe = farben.gedaempft,
                    )
                    Rundknopf(
                        symbol = Symbole.Einstellungen,
                        beschriftung = "Einstellungen",
                        beiKlick = { modell.gehe(Ziel.EINSTELLUNGEN) },
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
            Text(
                text = modell.heute
                    .format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
                    .uppercase(),
                style = schriften.daten,
                color = farben.gedaempft,
            )

            meldung?.let { text ->
                Karte {
                    Text(text, style = schriften.fliesstext, color = farben.warnung)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                        Textknopf("Nochmal versuchen", modell::schliesseMeldung)
                    }
                }
            }

            wartet?.let { Karte { Wartezustand(it) } }

            when (zustand) {
                TagZustand.LEER, TagZustand.AUFNAHME -> LageErfragen(modell, nimmtAuf)

                TagZustand.LAGE_STEHT -> {
                    Karte {
                        Zwischenueberschrift("Deine Lage heute")
                        Box(Modifier.padding(top = 12.dp)) {
                            Eingabefeld(
                                text = lageFeld.text,
                                beiAenderung = { modell.setzeText(modell.lageFeldFluss(), it) },
                                platzhalter = "Was für ein Tag ist das? Was liegt vor dir?",
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Textknopf(
                            text = if (lageFeld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                            beiKlick = {
                                if (lageFeld.kannZurueck) modell.nimmZurueck(modell.lageFeldFluss())
                                else modell.verbessere(modell.lageFeldFluss())
                            },
                            modifier = Modifier.weight(1f),
                            betont = false,
                            aktiv = !lageFeld.laeuft,
                        )
                        Textknopf("Weiter", modell::weiter, Modifier.weight(1f))
                    }
                }

                TagZustand.VORSCHLAEGE -> {
                    Zwischenueberschrift("Fünf Vorschläge für heute")
                    vorschlaege.forEachIndexed { nr, vorschlag ->
                        // M-04: die Karten erscheinen gestaffelt, 240 ms.
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(dauer(Bewegung.LANG), delayMillis = nr * 60, easing = Bewegung.weich)) +
                                slideInVertically(
                                    tween(dauer(Bewegung.LANG), delayMillis = nr * 60, easing = Bewegung.weich),
                                ) { it / 6 },
                        ) {
                            Vorschlagskarte(vorschlag, modell)
                        }
                    }
                    Textknopf("Andere Vorschläge", modell::andereVorschlaege, Modifier.fillMaxWidth())
                }

                TagZustand.LAEUFT, TagZustand.ABEND -> {
                    Zwischenueberschrift(
                        if (offene.size > 1) "Läuft (${offene.size} von 3)" else "Läuft",
                    )
                    offene.forEach { Experimentkarte(it, modell) }
                    if (offene.size >= 3) {
                        Text(
                            "Drei Experimente laufen. Schließ eines ab, bevor du ein neues beginnst.",
                            style = schriften.fliesstextKlein,
                            color = farben.gedaempft,
                        )
                    }

                    val aufgaben = offene.flatMap { modell.heutigeAufgaben(it) }
                    if (aufgaben.isNotEmpty()) {
                        Zwischenueberschrift("Heute zu tun")
                        // F-07: EINE Liste für den Tag, nach Experimenten gruppiert —
                        // der Titel steht als Zwischenüberschrift darüber.
                        Karte {
                            offene.forEachIndexed { nr, experiment ->
                                val seine = modell.heutigeAufgaben(experiment)
                                if (seine.isEmpty()) return@forEachIndexed
                                Text(
                                    text = experiment.title,
                                    style = schriften.kartentitel,
                                    color = farben.text,
                                    modifier = Modifier.padding(top = if (nr == 0) 0.dp else 20.dp),
                                )
                                seine.forEach { Aufgabenzeile(it, modell) }
                            }
                        }
                    }

                    if (zustand == TagZustand.ABEND) {
                        Textknopf("Wie ist es gelaufen?", modell::oeffneAuswertung, Modifier.fillMaxWidth())
                    }
                }
            }

            Box(Modifier.padding(bottom = 12.dp))
        }

        UntereLeiste(jetzt = Ziel.HEUTE, beiWahl = modell::gehe)
    }
}

/** Der leere Zustand: Frage, Untertitel, Sprechknopf, „Lieber tippen“. */
@Composable
private fun LageErfragen(modell: AppViewModel, nimmtAuf: Boolean) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val lageFeld by modell.lageFeld.collectAsStateWithLifecycle()

    Karte {
        Text("Wie ist deine Lage heute?", style = schriften.abschnittstitel, color = farben.text)
        Text(
            "Was für ein Tag ist das? Was liegt vor dir?",
            style = schriften.fliesstextKlein,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Sprechknopf(nimmtAuf = nimmtAuf, beiKlick = { modell.sprechknopf(modell.lageFeldFluss()) })
        Textknopf(
            text = "Lieber tippen",
            beiKlick = { modell.setzeText(modell.lageFeldFluss(), lageFeld.text.ifBlank { " " }) },
        )
    }
    if (lageFeld.text.isNotBlank()) {
        Eingabefeld(
            text = lageFeld.text,
            beiAenderung = { modell.setzeText(modell.lageFeldFluss(), it) },
            platzhalter = "Was für ein Tag ist das? Was liegt vor dir?",
        )
        Textknopf("Weiter", modell::weiter, Modifier.fillMaxWidth())
    }
}

/** Eine Vorschlagskarte: Titel, Beschreibung, Stufe und Dauer, Merken-Symbol. */
@Composable
private fun Vorschlagskarte(vorschlag: Suggestion, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Karte(beiKlick = { modell.starte(vorschlag) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                text = vorschlag.title,
                style = schriften.kartentitel,
                color = farben.text,
                modifier = Modifier.weight(1f),
            )
            // F-05: das Merken-Symbol füllt sich (M-07, 180 ms).
            Icon(
                imageVector = Symbole.AufDieMerklisteLegen,
                contentDescription = "Auf die Merkliste legen",
                tint = farben.gedaempft,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { modell.merke(vorschlag) },
            )
        }
        Text(
            text = vorschlag.description,
            style = schriften.fliesstext,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Etikett(vorschlag.level.name.lowercase())
            Etikett(if (vorschlag.days > 1) "${vorschlag.days} Tage" else "1 Tag")
            if (vorschlag.fromWatchlist) Etikett("von deiner Merkliste")
        }
    }
}

/** Ein laufendes Experiment: Titel, Tag, Stufe, Gespräch, „Nicht umgesetzt“. */
@Composable
private fun Experimentkarte(experiment: Experiment, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Karte {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                text = experiment.title,
                style = schriften.kartentitel,
                color = farben.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Symbole.GespraechZumExperiment,
                contentDescription = "Gespräch zum Experiment",
                tint = farben.aktion,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { modell.oeffneGespraech(experiment) },
            )
        }
        Text(
            text = experiment.description,
            style = schriften.fliesstextKlein,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Etikett(modell.tagText(experiment))
            Etikett(modell.stufenwort(experiment))
            Box(Modifier.weight(1f))
            // F-13: „Nicht umgesetzt“ sitzt im Entwurf auf der Experimentkarte von B-01.
            Text(
                text = "Nicht umgesetzt",
                style = schriften.knopf,
                color = farben.aktion,
                modifier = Modifier.clickable { modell.nichtUmgesetzt(experiment) },
            )
        }
    }
}

/** Eine Aufgabe der Tagesliste: Kästchen, Text, gedämpft wenn abgehakt (F-08). */
@Composable
private fun Aufgabenzeile(aufgabe: Task, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val fertig = aufgabe.doneAt != null
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable { modell.hakenSchalten(aufgabe) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (fertig) farben.erledigtGedeckt else farben.flaeche)
                .border(1.dp, if (fertig) farben.erledigt else farben.rand, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // M-06: der Haken zeichnet sich.
            if (fertig) {
                Text("✓", style = schriften.fliesstextKlein, color = farben.erledigt)
            }
        }
        Text(
            text = aufgabe.text,
            style = schriften.fliesstext,
            color = if (fertig) farben.blass else farben.text,
            textDecoration = if (fertig) TextDecoration.LineThrough else null,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun symbolFuerErscheinung(naechste: Erscheinung) = when (naechste) {
    Erscheinung.HELL -> Symbole.HellmodusEinschalten
    Erscheinung.DUNKEL -> Symbole.HellmodusEinschalten2
    Erscheinung.SYSTEM -> Symbole.HellmodusEinschalten3
}

private fun beschriftungFuer(naechste: Erscheinung) = when (naechste) {
    Erscheinung.HELL -> "Hellmodus einschalten"
    Erscheinung.DUNKEL -> "Dunkelmodus einschalten"
    Erscheinung.SYSTEM -> "Der Systemdarstellung folgen"
}
