package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Semantisch

/**
 * Der Bildschirm, auf den es ankommt: unterwegs schnell eine Idee einsprechen oder eintippen,
 * bevor sie weg ist.
 */
@Composable
fun ErfassenScreen(
    viewModel: IdeenViewModel,
    mikrofonErlaubt: Boolean,
    aufMikrofonFragen: () -> Unit,
    aufZurueck: () -> Unit,
    aufEinstellungen: () -> Unit,
) {
    val gold = LocalGold.current
    val theme by viewModel.theme.collectAsState()
    val aufnahme by viewModel.aufnahme.collectAsState()
    val ki by viewModel.ki.collectAsState()

    var titel by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var original by remember { mutableStateOf<String?>(null) }

    // Beim Öffnen läuft die Aufnahme sofort los — dafür ist die App da.
    LaunchedEffect(mikrofonErlaubt) {
        if (mikrofonErlaubt && !aufnahme.laeuft && text.isBlank()) viewModel.starteAufnahme()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gold.hintergrund)
            .imePadding(),
    ) {
        IdeenKopfleiste(
            titel = "Neue Idee",
            themeWahl = theme,
            aufEinstellungen = aufEinstellungen,
            voran = {
                Box(
                    modifier = Modifier.size(38.dp).druckEffekt {
                        viewModel.brichAufnahmeAb()
                        aufZurueck()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück ohne Speichern",
                        tint = gold.primaer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!mikrofonErlaubt) {
                GoldKarte(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Zum Einsprechen braucht die App das Mikrofon",
                            style = MaterialTheme.typography.titleSmall,
                            color = gold.textPrimaer,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Es wird ausschliesslich für deine Diktate benutzt und nichts davon " +
                                "verlässt das Gerät ausser dem Text an den Erkennungsdienst.",
                            style = MaterialTheme.typography.bodySmall,
                            color = gold.textGedaempft,
                        )
                        Spacer(Modifier.height(12.dp))
                        GoldKnopf(text = "Mikrofon freigeben", aufTipp = aufMikrofonFragen)
                    }
                }
            }

            EingabeFeld(
                beschriftung = "Titel",
                platzhalter = "Kurz und wiedererkennbar",
                wert = titel,
                aufWert = { titel = it },
                einzeilig = true,
            )

            EingabeFeld(
                beschriftung = "Die Idee",
                platzhalter = "Sprich sie ein oder tipp sie hier",
                wert = text,
                aufWert = { text = it },
                minHoehe = 160.dp,
            )

            AnimatedVisibility(visible = original != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Der Text wurde geglättet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .druckEffekt {
                                original?.let { text = it }
                                original = null
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(gold.flaecheErhoeht)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("Original anzeigen", style = MaterialTheme.typography.labelSmall, color = gold.primaer)
                    }
                }
            }

            if (aufnahme.wirdUebertragen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Das Gesprochene wird gerade zu Text …",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gold.flaeche)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GrosserMikrofonKnopf(
                    laeuft = aufnahme.laeuft,
                    uebertraegt = aufnahme.wirdUebertragen,
                    aufTipp = {
                        if (aufnahme.laeuft) {
                            viewModel.beendeAufnahme { erkannt ->
                                text = if (text.isBlank()) erkannt else "$text $erkannt"
                                if (titel.isBlank()) titel = erkannt.take(60)
                            }
                        } else if (mikrofonErlaubt) {
                            viewModel.starteAufnahme()
                        } else {
                            aufMikrofonFragen()
                        }
                    },
                )
                if (text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .druckEffekt {
                                original = text
                                viewModel.glaetteText(text) { geglaettet -> text = geglaettet }
                            }
                            .clip(CircleShape)
                            .border(1.dp, gold.rahmen, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Text glätten",
                            tint = if (ki.antwortet) gold.primaer else gold.textGedaempft,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                GoldKnopf(
                    text = "Speichern",
                    aktiviert = text.isNotBlank() || titel.isNotBlank(),
                    laedt = aufnahme.wirdUebertragen,
                    aufTipp = {
                        viewModel.legeAn(titel, text, originalText = original)
                        aufZurueck()
                    },
                )
            }
        }
    }
}

@Composable
private fun EingabeFeld(
    beschriftung: String,
    platzhalter: String,
    wert: String,
    aufWert: (String) -> Unit,
    einzeilig: Boolean = false,
    minHoehe: androidx.compose.ui.unit.Dp = 46.dp,
) {
    val gold = LocalGold.current
    Column {
        Text(
            beschriftung,
            style = MaterialTheme.typography.labelSmall,
            color = gold.textGedaempft,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHoehe)
                .clip(RoundedCornerShape(16.dp))
                .background(gold.eingabefeld)
                .border(1.dp, gold.rahmen, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            if (wert.isEmpty()) {
                Text(platzhalter, style = MaterialTheme.typography.bodyMedium, color = gold.textGedaempft)
            }
            BasicTextField(
                value = wert,
                onValueChange = aufWert,
                singleLine = einzeilig,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = gold.textPrimaer),
                cursorBrush = SolidColor(gold.primaer),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GrosserMikrofonKnopf(laeuft: Boolean, uebertraegt: Boolean, aufTipp: () -> Unit) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "aufnahme")
    val puls by uebergang.animateFloat(
        initialValue = 1f,
        targetValue = if (laeuft && !reduziert) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulswert",
    )
    Box(
        modifier = Modifier
            .scale(puls)
            .size(60.dp)
            .druckEffekt(aufTipp)
            .goldSchein(
                if (laeuft) Semantisch.fehler else gold.primaer,
                hoehe = 16.dp,
                radius = 30.dp,
            )
            .clip(CircleShape)
            .background(
                if (laeuft) {
                    Brush.radialGradient(listOf(Semantisch.fehler, Semantisch.fehler.copy(alpha = 0.7f)))
                } else {
                    Brush.radialGradient(listOf(gold.primaer, gold.primaerGedaempft))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (laeuft) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (laeuft) "Aufnahme beenden" else "Aufnahme starten",
            tint = gold.aufPrimaer,
            modifier = Modifier.size(26.dp),
        )
    }
}
