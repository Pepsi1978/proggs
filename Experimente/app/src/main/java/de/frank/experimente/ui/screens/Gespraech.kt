package de.frank.experimente.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.data.local.Rolle
import de.frank.experimente.ui.AppViewModel
import de.frank.experimente.ui.components.Kopfzeile
import de.frank.experimente.ui.components.Rundknopf
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.LocalSchriften
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.filmkorn
import de.frank.experimente.ui.theme.glas
import de.frank.experimente.ui.theme.lichtgrund

/**
 * **B-02 — Gespräch** (F-09). Der Faden gehört genau einem Experiment; es gibt kein freies,
 * themenloses Gespräch.
 *
 * Der Entwurf setzt die Blasen auf `width: fit-content` mit höchstens `100% − 28 dp`:
 * eigene rechts mit Radius `20 20 6 20` auf *Erhöht*, die der KI links mit `20 20 20 6` auf
 * *Fläche* und 1 dp Rand. Unten liegt eine Glasleiste (96 dp) mit Eingabefeld (56 dp),
 * Mikrofon und rundem Sende-Knopf (56 dp).
 */
@Composable
fun Gespraech(modell: AppViewModel) {
    val farben = LocalFarben.current
    val schriften = LocalSchriften.current
    val stufe = LocalEffektstufe.current

    val nachrichten by modell.gespraech.collectAsStateWithLifecycle()
    val denkt by modell.wartet.collectAsStateWithLifecycle()
    val nimmtAuf by modell.nimmtAuf.collectAsStateWithLifecycle()
    val laufende by modell.laufende.collectAsStateWithLifecycle()
    val gespraechZu by modell.gespraechZu.collectAsStateWithLifecycle()

    val experiment = laufende.firstOrNull { it.id == gespraechZu }
    var entwurf by remember { mutableStateOf("") }
    val liste = rememberLazyListState()

    LaunchedEffect(nachrichten.size) {
        if (nachrichten.isNotEmpty()) liste.animateScrollToItem(nachrichten.lastIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .lichtgrund(farben, stufe)
            .filmkorn(stufe)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.ime)),
    ) {
        Kopfzeile(
            innenAbstand = 8.dp,
            links = {
                Rundknopf(Symbole.Zurueck, "Zurück", modell::zurueckTippen)
            },
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = experiment?.title ?: "Experiment",
                    style = schriften.gespraechstitel,
                    color = farben.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = experiment?.let(modell::tagText) ?: "Tag 1",
                    style = schriften.stufe,
                    color = farben.gedaempft,
                )
            }
        }

        LazyColumn(
            state = liste,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(nachrichten, key = { it.id }) { runde ->
                val eigen = runde.role == Rolle.ICH
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (eigen) Arrangement.End else Arrangement.Start,
                ) {
                    Box(
                        Modifier
                            .widthIn(max = 322.dp)
                            .clip(
                                if (eigen) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                                else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                            )
                            .background(if (eigen) farben.erhoeht else farben.flaeche)
                            .then(
                                if (eigen) Modifier
                                else Modifier.border(
                                    1.dp, farben.rand,
                                    RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
                                )
                            )
                            .padding(16.dp),
                    ) {
                        Text(
                            text = runde.text,
                            style = schriften.fliesstext.copy(lineHeight = schriften.fliesstext.lineHeight),
                            color = farben.text,
                        )
                    }
                }
            }
            if (denkt != null) {
                item("denkt") { Denkpunkte() }
            }
        }

        // Die Eingabeleiste: Glasfläche, 96 dp hoch.
        Row(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .glas(farben, stufe)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(farben.erhoeht)
                    .border(1.dp, farben.rand, RoundedCornerShape(14.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(start = 16.dp, end = 6.dp)) {
                    if (entwurf.isEmpty()) {
                        Text("Nachricht eingeben", style = schriften.fliesstext, color = farben.blass)
                    }
                    BasicTextField(
                        value = entwurf,
                        onValueChange = { entwurf = it },
                        textStyle = schriften.fliesstext.copy(color = farben.text),
                        cursorBrush = SolidColor(farben.aktion),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(
                    Modifier
                        .padding(end = 4.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { modell.gespraechAufnehmen() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (nimmtAuf) Symbole.Stopp else Symbole.Mikrofon,
                        contentDescription = "Gespräch aufnehmen",
                        tint = if (nimmtAuf) farben.aktion else farben.gedaempft,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(farben.aktion)
                    .clickable {
                        val text = entwurf.trim()
                        if (text.isNotEmpty()) {
                            entwurf = ""
                            modell.sprich(text)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Symbole.Senden,
                    contentDescription = "Senden",
                    tint = farben.aufAktion,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

/**
 * Der Wartezustand im Gespräch: drei Punkte à 6 dp, `dot` über 1400 ms linear endlos mit
 * 0 / 200 / 400 ms Versatz — `0%, 60%, 100% → opacity .25`, `30% → opacity 1`.
 */
@Composable
private fun Denkpunkte() {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val takt = rememberInfiniteTransition(label = "denkt")
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .background(farben.flaeche)
            .border(1.dp, farben.rand, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(0, 200, 400).forEach { versatz ->
            val deckkraft by takt.animateFloat(
                initialValue = 0.25f,
                targetValue = if (stufe.dauerbewegung) 1f else 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, delayMillis = versatz, easing = Bewegung.gerade),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "punkt$versatz",
            )
            Box(
                Modifier
                    .size(6.dp)
                    .alpha(deckkraft)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(farben.gedaempft),
            )
        }
    }
}
