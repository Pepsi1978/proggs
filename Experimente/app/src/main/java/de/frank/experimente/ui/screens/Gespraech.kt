package de.frank.experimente.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.ChatTurn
import de.frank.experimente.data.local.Rolle
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.components.Karte
import de.frank.experimente.ui.components.Kopfzeile
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.components.Sprechknopf
import de.frank.experimente.ui.components.Wartezustand
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Formen
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.dauer

/**
 * **B-02 — Gespräch.** Der Faden gehört genau einem Experiment (F-09); es gibt kein freies,
 * themenloses Gespräch. Die Antwort wird sofort vorgelesen, ohne weiteren Druck.
 *
 * Kein Wischen (F-27 gilt nur für die fünf Hauptbildschirme) — zurück über den Pfeil.
 */
@Composable
fun Gespraech(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val faden by modell.gespraech.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val wartet by modell.wartet.collectAsStateWithLifecycle()
    val meldung by modell.meldung.collectAsStateWithLifecycle()
    var getippt by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Kopfzeile(
            titel = "Gespräch",
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

        LazyColumn(
            Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (faden.isEmpty()) {
                item {
                    Karte {
                        Text(
                            "Frag mich, wie du das angehen könntest.",
                            style = schriften.fliesstext,
                            color = farben.gedaempft,
                        )
                    }
                }
            }
            items(faden, key = { it.id }) { runde -> Blase(runde, modell) }
            meldung?.let { text ->
                item {
                    Karte { Text(text, style = schriften.fliesstext, color = farben.warnung) }
                }
            }
            wartet?.let { item { Karte { Wartezustand(it) } } }
            item { Box(Modifier.padding(bottom = 8.dp)) }
        }

        // Der Verfasser unten: Textfeld und Sprechknopf.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(Formen.eingabefeld)
                    .background(farben.erhoeht)
                    .border(1.dp, farben.rand, Formen.eingabefeld)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (getippt.isEmpty()) {
                    Text("Oder tippen …", style = schriften.fliesstext, color = farben.blass)
                }
                BasicTextField(
                    value = getippt,
                    onValueChange = { getippt = it },
                    textStyle = schriften.fliesstext.copy(color = farben.text),
                    cursorBrush = SolidColor(farben.aktion),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (getippt.isNotBlank()) {
                Icon(
                    imageVector = Symbole.GespraechAufnehmen,
                    contentDescription = "Abschicken",
                    tint = farben.aktion,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            modell.sprich(getippt)
                            getippt = ""
                        },
                )
            } else {
                Sprechknopf(
                    nimmtAuf = nimmtAuf,
                    beiKlick = modell::gespraechAufnehmen,
                    beschriftung = "Gespräch aufnehmen",
                    modifier = Modifier.size(72.dp),
                )
            }
        }
    }
}

/**
 * Eine Sprechblase. Franks Blase trägt `20 20 6 20`, die der KI `20 20 20 6` — so sind die
 * beiden Radien im Entwurf gemessen. **M-08**: die Antwort erscheint in 400 ms, `ease-out`.
 */
@Composable
private fun Blase(runde: ChatTurn, modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val meins = runde.role == Rolle.ICH

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(dauer(Bewegung.ANTWORT), easing = Bewegung.antwort)),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (meins) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.86f)
                    .clip(if (meins) Formen.blaseIch else Formen.blaseKi)
                    .background(if (meins) farben.aktionGedeckt else farben.flaeche)
                    .border(1.dp, farben.rand, if (meins) Formen.blaseIch else Formen.blaseKi)
                    .padding(16.dp),
            ) {
                Text(text = runde.text, style = schriften.fliesstext, color = farben.text)
                if (!meins) {
                    // F-09 Fehlerfall: ein Lautsprecher-Knopf erlaubt einen neuen Versuch,
                    // wenn das sofortige Vorlesen nicht geklappt hat.
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Icon(
                            imageVector = Symbole.AuswertungVorlesen,
                            contentDescription = "Vorlesen",
                            tint = farben.gedaempft,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { modell.lies(runde.text) },
                        )
                    }
                }
            }
        }
    }
}
