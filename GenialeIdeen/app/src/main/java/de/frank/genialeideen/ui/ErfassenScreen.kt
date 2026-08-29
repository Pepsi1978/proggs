package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.Semantisch
import de.frank.genialeideen.ui.theme.lichtKante
import de.frank.genialeideen.ui.theme.schwebend
import de.frank.genialeideen.ui.theme.tiefenSchatten
import kotlin.math.sin

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
    val korrektur by viewModel.korrektur.collectAsState()

    var titel by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    // Beim Öffnen läuft die Aufnahme sofort los — dafür ist die App da.
    LaunchedEffect(mikrofonErlaubt) {
        if (mikrofonErlaubt && !aufnahme.laeuft && text.isBlank()) viewModel.starteAufnahme()
    }

    Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
        BewegterHintergrund()
        Column(
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            IdeenKopfleiste(
                titel = "Neue Idee",
                themeWahl = theme,
                aufEinstellungen = aufEinstellungen,
                voran = {
                    KopfKnopf(beschreibung = "Zurück ohne Speichern", aufTipp = {
                        viewModel.brichAufnahmeAb()
                        viewModel.korrekturVergessen()
                        aufZurueck()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = gold.primaer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
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
                    GestaffeltEinblenden(sichtbar = true, index = 0) {
                        GoldKarte(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Zum Einsprechen braucht die App das Mikrofon",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = gold.textPrimaer,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Es wird ausschliesslich für deine Diktate benutzt. Ausser dem " +
                                        "Text an den Erkennungsdienst verlässt nichts das Gerät.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = gold.textGedaempft,
                                )
                                Spacer(Modifier.height(12.dp))
                                GoldKnopf(text = "Mikrofon freigeben", aufTipp = aufMikrofonFragen)
                            }
                        }
                    }
                }

                GestaffeltEinblenden(sichtbar = true, index = 1) {
                    EingabeFeld(
                        beschriftung = "Titel",
                        platzhalter = "Kurz und wiedererkennbar",
                        wert = titel,
                        aufWert = { titel = it },
                        einzeilig = true,
                    )
                }

                GestaffeltEinblenden(sichtbar = true, index = 2) {
                    EingabeFeld(
                        beschriftung = "Die Idee",
                        platzhalter = "Sprich sie ein oder tipp sie hier",
                        wert = text,
                        aufWert = {
                            text = it
                            // Von Hand geändert heisst: Die Korrektur ist nicht mehr rücknehmbar.
                            if (korrektur != null && it != korrektur?.korrigiert) {
                                viewModel.korrekturVergessen()
                            }
                        },
                        minHoehe = 160.dp,
                    )
                }

                AnimatedVisibility(visible = korrektur != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "In gutes Deutsch gebracht — dein Original liegt bereit.",
                            style = MaterialTheme.typography.labelSmall,
                            color = gold.textGedaempft,
                        )
                    }
                }

                AnimatedVisibility(visible = aufnahme.wirdUebertragen) {
                    Text(
                        "Das Gesprochene wird gerade zu Text …",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.textGedaempft,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tiefenSchatten(gold.primaer, Hoehe.schwebendeLeiste, RoundedCornerShape(0.dp))
                    .background(
                        Brush.verticalGradient(listOf(gold.flaeche, gold.flaecheErhoeht)),
                    )
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AufnahmeKnopfMitPegel(
                        laeuft = aufnahme.laeuft,
                        pegel = aufnahme.pegel,
                        aufTipp = {
                            if (aufnahme.laeuft) {
                                viewModel.beendeAufnahme { erkannt ->
                                    text = if (text.isBlank()) erkannt else "$text $erkannt"
                                    if (titel.isBlank()) titel = erkannt.take(60)
                                    viewModel.korrekturVergessen()
                                }
                            } else if (mikrofonErlaubt) {
                                viewModel.starteAufnahme()
                            } else {
                                aufMikrofonFragen()
                            }
                        },
                    )

                    // Aus dem Korrektur-Knopf wird nach der Korrektur der Rückgängig-Knopf.
                    if (text.isNotBlank()) {
                        KorrekturKnopf(
                            korrigiert = korrektur != null,
                            laeuft = ki.antwortet,
                            aufKorrigieren = {
                                viewModel.korrigiereText(text) { neu -> text = neu }
                            },
                            aufZuruecknehmen = {
                                viewModel.korrekturZuruecknehmen { alt -> text = alt }
                            },
                        )
                    }

                    Spacer(Modifier.weight(1f))
                    GoldKnopf(
                        text = "Speichern",
                        aktiviert = text.isNotBlank() || titel.isNotBlank(),
                        laedt = aufnahme.wirdUebertragen,
                        hauptKnopf = true,
                        aufTipp = {
                            viewModel.legeAn(titel, text, originalText = korrektur?.original)
                            viewModel.korrekturVergessen()
                            aufZurueck()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Der Knopf, der aus dem Diktat gutes Deutsch macht — und danach zum Rückgängig-Knopf wird
 * (Baustein O.4). Der Wechsel läuft als Formwandel, nicht als harter Symboltausch (N.5).
 */
@Composable
fun KorrekturKnopf(
    korrigiert: Boolean,
    laeuft: Boolean,
    aufKorrigieren: () -> Unit,
    aufZuruecknehmen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    RundKnopf3D(
        beschreibung = if (korrigiert) {
            "Korrektur rückgängig machen und das Original wiederherstellen"
        } else {
            "Text in gutes Deutsch bringen"
        },
        aufTipp = { if (korrigiert) aufZuruecknehmen() else aufKorrigieren() },
        modifier = modifier,
        groesse = 52.dp,
        grundfarbe = if (korrigiert) gold.akzentWarm else gold.flaecheErhoeht,
        aktiviert = !laeuft,
    ) {
        AnimatedContent(
            targetState = korrigiert,
            transitionSpec = {
                fadeIn(tween(Motion.MIKRO_MS)) togetherWith fadeOut(tween(Motion.MIKRO_MS))
            },
            label = "korrektur",
        ) { istKorrigiert ->
            Icon(
                imageVector = if (istKorrigiert) Icons.Default.Undo else Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = if (istKorrigiert) gold.aufPrimaer else gold.primaer,
                modifier = Modifier.size(22.dp),
            )
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
    minHoehe: Dp = 46.dp,
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
                // Eingabefelder sind Vertiefungen: oben dunkler, unten heller (N.4).
                .background(
                    Brush.verticalGradient(
                        listOf(gold.eingabefeld, gold.eingabefeld, gold.flaeche),
                    ),
                )
                .border(1.dp, lichtKante(gedrueckt = true, staerke = 0.22f), RoundedCornerShape(16.dp))
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

/**
 * Der Aufnahmeknopf mit echter Pegel-Anzeige ringsum (N.7): Die Balken folgen der
 * tatsächlichen Lautstärke, nichts ist simuliert.
 */
@Composable
fun AufnahmeKnopfMitPegel(
    laeuft: Boolean,
    pegel: Float,
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
    groesse: Dp = 60.dp,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "pegel")
    val phase by uebergang.animateFloat(
        initialValue = 0f,
        targetValue = if (laeuft && !reduziert) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "phase",
    )

    Box(modifier = modifier.size(groesse + 22.dp), contentAlignment = Alignment.Center) {
        if (laeuft) {
            Canvas(Modifier.size(groesse + 22.dp)) {
                val mitte = Offset(size.width / 2f, size.height / 2f)
                val innen = size.minDimension / 2f - 11.dp.toPx()
                val balken = 28
                repeat(balken) { index ->
                    val winkel = (index.toFloat() / balken) * 2f * Math.PI.toFloat()
                    // Der Pegel bestimmt die Grundlänge, die Welle verteilt sie ringsum.
                    val welle = 0.55f + 0.45f * sin(phase + index * 0.6f)
                    val laenge = (4f + pegel.coerceIn(0f, 1f) * 16f * welle).dp.toPx()
                    val start = Offset(
                        mitte.x + innen * kotlin.math.cos(winkel),
                        mitte.y + innen * kotlin.math.sin(winkel),
                    )
                    val ende = Offset(
                        mitte.x + (innen + laenge) * kotlin.math.cos(winkel),
                        mitte.y + (innen + laenge) * kotlin.math.sin(winkel),
                    )
                    drawLine(
                        color = gold.primaer.copy(alpha = 0.35f + 0.5f * pegel.coerceIn(0f, 1f)),
                        start = start,
                        end = ende,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        RundKnopf3D(
            beschreibung = if (laeuft) "Aufnahme beenden" else "Aufnahme starten",
            aufTipp = aufTipp,
            modifier = Modifier.schwebend(aktiv = !laeuft),
            groesse = groesse,
            grundfarbe = if (laeuft) Semantisch.fehler else gold.primaer,
            hauptKnopf = true,
        ) {
            Icon(
                imageVector = if (laeuft) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                tint = gold.aufPrimaer,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
