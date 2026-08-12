package de.frank.experimente.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.Suggestion
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.TagZustand
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.components.Bildschirmgeruest
import de.frank.experimente.ui.components.Einflug
import de.frank.experimente.ui.components.Eingabefeld
import de.frank.experimente.ui.components.Etikett
import de.frank.experimente.ui.components.Etikettart
import de.frank.experimente.ui.components.KnopfBetont
import de.frank.experimente.ui.components.KnopfUmrandet
import de.frank.experimente.ui.components.Meldungen
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Textknopf
import de.frank.experimente.ui.components.Titel
import de.frank.experimente.ui.components.Wartekarte
import de.frank.experimente.ui.components.Wellenform
import de.frank.experimente.ui.components.Zwischenueberschrift
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.lichtsaum
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * **B-01 — Heute.** Hier wird die Lage eingesprochen und werden die fünf Vorschläge erzeugt.
 *
 * Seit dieser Fassung ist B-01 **nicht mehr der Startbildschirm**, und die laufenden
 * Experimente samt To-Do-Liste sind auf den Monitor (B-10) gewandert. Geblieben sind fünf
 * Zustände: `LEER` · `AUFNAHME` · `LAGE_STEHT` · `WARTET` · `VORSCHLAEGE` · `ABEND`.
 *
 * Neu auf jeder Vorschlagskarte: **„In den Monitor"** (F-36, der übliche Weg) neben
 * **„Jetzt starten"** (F-06, der Abkürzungsweg).
 */
@Composable
fun Heute(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current

    val zustand by modell.tagZustand.collectAsStateWithLifecycle()
    val erscheinung by modell.erscheinung.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val vorschlaege by modell.vorschlaege.collectAsStateWithLifecycle()
    val laufende by modell.laufende.collectAsStateWithLifecycle()
    val lageFeld by modell.lageFeld.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    val istVoll by modell.istVoll.collectAsStateWithLifecycle()

    Bildschirmgeruest(
        kopf = {
            Titel("Heute")
            Box(Modifier.weight(1f))
            Rundknopf(
                symbol = symbolFuerErscheinung(erscheinung.naechste()),
                beschriftung = beschriftungFuer(erscheinung.naechste()),
                beiKlick = modell::naechsteErscheinung,
            )
        },
        leiste = Ziel.HEUTE,
        beiLeistenwahl = modell::gehe,
        ueberlagerung = {
            Meldungen(stoerung = null, beiNochmal = modell::schliesseMeldung, hinweis = meldung)
        },
    ) {
        // Datum links, der Morgen/Abend-Umschalter rechts.
        item("datum") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = modell.heute
                        .format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
                        .uppercase(),
                    style = schriften.daten,
                    color = farben.gedaempft,
                )
                TagUmschalter(
                    abend = zustand == TagZustand.ABEND,
                    beiMorgen = modell::zeigeMorgen,
                    beiAbend = modell::zeigeAbend,
                )
            }
        }

        when (zustand) {
            TagZustand.LEER, TagZustand.AUFNAHME -> item("lage") {
                LageErfragen(modell, nimmtAuf)
            }

            TagZustand.LAGE_STEHT -> {
                item("feld") {
                    Eingabefeld(
                        text = lageFeld.text,
                        beiAenderung = { modell.setzeText(modell.lageFeldFluss(), it) },
                        platzhalter = "Was für ein Tag ist das? Was liegt vor dir?",
                    )
                }
                item("feld-knoepfe") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Textknopf(
                            text = if (lageFeld.kannZurueck) "Zurücknehmen" else "Text mit KI verbessern",
                            beiKlick = {
                                if (lageFeld.kannZurueck) modell.nimmZurueck(modell.lageFeldFluss())
                                else modell.verbessere(modell.lageFeldFluss())
                            },
                            aktiv = !lageFeld.laeuft,
                        )
                        KnopfBetont("Weiter", modell::weiter, gross = true)
                    }
                }
                if (istVoll) {
                    item("voll") {
                        Text(
                            text = "Drei Experimente laufen schon. Schließ eines ab, bevor du ein " +
                                "neues beginnst. Im Monitor siehst du sie.",
                            style = schriften.fliesstextKlein,
                            color = farben.gedaempft,
                        )
                    }
                }
            }

            TagZustand.WARTET -> item("wartet") {
                Wartekarte("Ich sehe mir an, was ich über dich weiß …")
            }

            TagZustand.VORSCHLAEGE -> {
                item("kopf-vorschlaege") { Zwischenueberschrift("Fünf Vorschläge für heute") }
                items(vorschlaege, key = { "v${it.id}" }) { vorschlag ->
                    Einflug(vorschlaege.indexOf(vorschlag)) {
                        Vorschlagskarte(vorschlag, modell, istVoll)
                    }
                }
                item("andere") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Textknopf(
                            text = "Andere Vorschläge",
                            beiKlick = modell::andereVorschlaege,
                            symbol = Symbole.Auffrischen,
                        )
                    }
                }
            }

            TagZustand.ABEND -> {
                item("kopf-abend") { Zwischenueberschrift("Abend") }
                item("abendzeile") {
                    Text(
                        text = if (laufende.size == 1) {
                            "Ein Experiment wartet auf deine Auswertung."
                        } else {
                            "${laufende.size} Experimente warten auf deine Auswertung."
                        },
                        style = schriften.fliesstext,
                        color = farben.gedaempft,
                    )
                }
                items(laufende, key = { "ab${it.id}" }) { experiment ->
                    Abendkarte(experiment, modell)
                }
            }
        }
    }
}

/** Der Umschalter Morgen/Abend: Pillen in einer Spur `color-mix(Text 8%)`, je 36 dp hoch. */
@Composable
private fun TagUmschalter(abend: Boolean, beiMorgen: () -> Unit, beiAbend: () -> Unit) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(farben.text.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("Morgen" to !abend, "Abend" to abend).forEach { (name, aktiv) ->
            Box(
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (aktiv) farben.flaeche else Color.Transparent)
                    .clickable { if (name == "Morgen") beiMorgen() else beiAbend() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name,
                    style = schriften.umschalter,
                    color = if (aktiv) farben.aktion else farben.gedaempft,
                )
            }
        }
    }
}

/**
 * Der leere Zustand: die Frage, der Untertitel, der große Sprechknopf mit der Wellenform
 * (`E-18`) und darunter „Lieber tippen" (F-28).
 */
@Composable
private fun LageErfragen(modell: AppViewModel, nimmtAuf: Boolean) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val sekunden by modell.aufnahmeSekunden.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth()) {
        Text("Wie ist deine Lage heute?", style = schriften.abschnittstitel, color = farben.text)
        Text(
            text = "Was für ein Tag ist das? Was liegt vor dir?",
            style = schriften.fliesstextKlein,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Sprechknopf(nimmtAuf = nimmtAuf, beiKlick = { modell.sprechknopf(modell.lageFeldFluss()) })
            if (nimmtAuf) {
                Wellenform(Modifier.padding(top = 2.dp))
                Text(
                    text = "00:%02d".format(sekunden),
                    style = schriften.daten,
                    color = farben.gedaempft,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Textknopf("Lieber tippen", modell::lieberTippen, Modifier.padding(top = 14.dp))
        }
    }
}

/**
 * Eine **Vorschlagskarte**. Sie lässt sich wischen (`E-22`): nach rechts in den Monitor
 * (F-36), nach links verworfen. Beide Wege stehen zusätzlich als Knopf da, damit kein
 * Effekt Information allein trägt.
 */
@Composable
private fun Vorschlagskarte(vorschlag: Suggestion, modell: AppViewModel, istVoll: Boolean) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current
    val dichte = LocalDensity.current
    var wischweg by remember { mutableFloatStateOf(0f) }
    var gemerkt by remember { mutableStateOf(false) }
    val schwelle = with(dichte) { 100.dp.toPx() }

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = wischweg
                // `rotate(versatz * 0.02deg)` aus dem Entwurf.
                rotationZ = wischweg * 0.02f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
            .lichtsaum(farben.text, 0.12f)
            .then(
                if (!stufe.parallaxe) Modifier else Modifier.pointerInput(vorschlag.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (wischweg > schwelle) modell.uebernimm(vorschlag)
                            wischweg = 0f
                        },
                        onDragCancel = { wischweg = 0f },
                    ) { _, weite -> wischweg += weite }
                }
            ),
    ) {
        if (wischweg != 0f) {
            val anteil = (kotlin.math.abs(wischweg) / schwelle).coerceIn(0f, 1f)
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(anteil)
                    .background(
                        if (wischweg >= 0) farben.aktion.copy(alpha = 0.18f)
                        else farben.warnung.copy(alpha = 0.16f),
                    ),
            )
            Text(
                text = if (wischweg >= 0) "IN DEN MONITOR" else "VERWORFEN",
                style = schriften.zwischenueberschrift,
                color = if (wischweg >= 0) farben.aktion else farben.warnung,
                textAlign = if (wischweg >= 0) TextAlign.End else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(anteil)
                    .padding(top = 18.dp, start = 18.dp, end = 18.dp),
            )
        }
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = vorschlag.title,
                    style = schriften.kartentitel,
                    color = farben.text,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                // F-05 — merken; das Symbol füllt sich, wenn der Vorschlag liegt.
                Icon(
                    imageVector = if (gemerkt) Symbole.Gemerkt else Symbole.Merken,
                    contentDescription = "Auf die Merkliste legen",
                    tint = if (gemerkt) farben.aktion else farben.blass,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { modell.merke(vorschlag); gemerkt = true }
                        .padding(9.dp),
                )
            }
            Text(
                text = vorschlag.description,
                style = schriften.kartentext,
                color = farben.text,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = modell.vorschlagsMeta(vorschlag),
                style = schriften.stufe,
                color = farben.gedaempft,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (vorschlag.fromWatchlist) {
                Box(Modifier.padding(top = 10.dp)) {
                    Etikett("von deiner Merkliste", Etikettart.STUFE)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KnopfUmrandet(
                    text = "In den Monitor",
                    beiKlick = { modell.uebernimm(vorschlag) },
                    modifier = Modifier.weight(1f),
                    farbe = farben.aktion,
                    randfarbe = farben.aktion,
                )
                KnopfBetont(
                    text = "Jetzt starten",
                    beiKlick = { modell.starteSofort(vorschlag) },
                    modifier = Modifier.weight(1f),
                    aktiv = !istVoll,
                )
            }
        }
    }
}

/**
 * Eine **Abendkarte**: Titel, Tag und Haken-Stand, darunter die heutigen Aufgaben zum
 * Abhaken und die drei Wege „Gespräch", „Nicht umgesetzt" und „Wie ist es gelaufen?".
 */
@Composable
private fun Abendkarte(experiment: de.frank.experimente.data.local.Experiment, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val heutige = modell.heutigeAufgaben(experiment)
    val fertig = heutige.count { it.doneAt != null }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp))
            .lichtsaum(farben.text, 0.12f)
            .padding(18.dp),
    ) {
        Text(experiment.title, style = schriften.kartentitel, color = farben.text)
        Text(
            text = "${modell.tagText(experiment)} · $fertig von ${heutige.size} erledigt",
            style = schriften.daten,
            color = farben.gedaempft,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(farben.randWeich))
        heutige.forEach { aufgabe ->
            val erledigt = aufgabe.doneAt != null
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { modell.hakenSchalten(aufgabe) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (erledigt) Symbole.KastenVoll else Symbole.KastenLeer,
                    contentDescription = if (erledigt) "Erledigt" else "Offen",
                    tint = if (erledigt) farben.erledigt else farben.text,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = aufgabe.text,
                    style = schriften.aufgabenzeile,
                    color = if (erledigt) farben.blass else farben.text,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(farben.randWeich))
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KnopfUmrandet(
                text = "Gespräch",
                beiKlick = { modell.oeffneGespraech(experiment) },
                modifier = Modifier.weight(1f),
            )
            KnopfUmrandet(
                text = "Nicht umgesetzt",
                beiKlick = { modell.nichtUmgesetzt(experiment) },
                modifier = Modifier.weight(1f),
                farbe = farben.gedaempft,
            )
        }
        KnopfBetont(
            text = "Wie ist es gelaufen?",
            beiKlick = { modell.oeffneAuswertung(experiment) },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            gross = true,
        )
    }
}
