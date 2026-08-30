package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.NachrichtEntity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.Hoehe
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.tiefenSchatten
import de.frank.genialeideen.ui.theme.Semantisch

@Composable
fun DetailScreen(
    viewModel: IdeenViewModel,
    aufZurueck: () -> Unit,
    aufEinstellungen: () -> Unit,
) {
    val gold = LocalGold.current
    val idee by viewModel.aktuelleIdee.collectAsState()
    val nachrichten by viewModel.nachrichten.collectAsState()
    val ki by viewModel.ki.collectAsState()
    val vorlese by viewModel.vorleseStand.collectAsState()
    val aufnahme by viewModel.aufnahme.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val korrektur by viewModel.korrektur.collectAsState()
    val kategorien by viewModel.kategorien.collectAsState()

    var eingabe by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Auf welche Nachricht wurde lang gedrückt? Solange sie steht, liegt das Löschblatt oben.
    var gedrueckt by remember { mutableStateOf<NachrichtEntity?>(null) }

    LaunchedEffect(nachrichten.size, ki.teilAntwort) {
        if (nachrichten.isNotEmpty()) listState.animateScrollToItem(nachrichten.size)
    }

    // Wechselt man die Idee, steht die Karte wieder ganz oben.
    LaunchedEffect(idee?.id) { listState.scrollToItem(0) }

    val aktuelle = idee
    if (aktuelle == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SchimmerGeruest(zeilen = 2, modifier = Modifier.padding(24.dp))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gold.hintergrund)
            .imePadding(),
    ) {
        IdeenKopfleiste(
            titel = aktuelle.titel,
            themeWahl = theme,
            aufEinstellungen = aufEinstellungen,
            voran = {
                Box(
                    modifier = Modifier.size(38.dp).druckEffekt(aufZurueck),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück zur Liste",
                        tint = gold.primaer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
            },
        )

        // Die Ideenkarte liegt als erstes Stück *in* der Liste: Bei einer langen Idee schiebt
        // sie sich beim Hochwischen nach oben weg und gibt den Chat frei.
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp, 4.dp, 0.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "ideenkopf") {
                    IdeenKopf(
                        idee = aktuelle,
                        kategorien = kategorien,
                        aufKategorie = { id -> viewModel.setzeKategorie(aktuelle.id, id) },
                        aufNeueKategorie = { name, art, fertig ->
                            viewModel.legeKategorieAn(name, art, fertig)
                        },
                        spricht = vorlese.quelle == "detail-${aktuelle.id}",
                        vorleseZustand = vorlese.zustand,
                        aufVorlesen = {
                            viewModel.lies(
                                "detail-${aktuelle.id}",
                                aktuelle.titel,
                                "${aktuelle.titel}.\n\n${aktuelle.text}",
                            )
                        },
                        aufUmgesetzt = {
                            if (aktuelle.status == IdeenStatus.OFFEN.name) {
                                viewModel.setzeUmgesetzt(aktuelle)
                            } else {
                                viewModel.zurueckZuOffen(aktuelle)
                            }
                        },
                        aufLoeschen = {
                            viewModel.loesche(aktuelle)
                            aufZurueck()
                        },
                    )
                }

                if (nachrichten.isEmpty() && ki.teilAntwort.isBlank() && !ki.antwortet) {
                    item(key = "leer") {
                        Leerzustand(
                            symbol = "💬",
                            ueberschrift = "Denk laut über die Idee nach",
                            satz = "Frag das Modell nach dem ersten Schritt, nach Risiken oder " +
                                "danach, was die Idee wirklich braucht.",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                items(nachrichten, key = NachrichtEntity::id) { nachricht ->
                    NachrichtenBlase(
                        nachricht = nachricht,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        spricht = vorlese.quelle == "nachricht-${nachricht.id}",
                        vorleseZustand = vorlese.zustand,
                        laufenderAbsatz = if (vorlese.quelle == "nachricht-${nachricht.id}") {
                            vorlese.absatzText
                        } else {
                            ""
                        },
                        aufVorlesen = {
                            viewModel.lies(
                                "nachricht-${nachricht.id}",
                                aktuelle.titel,
                                nachricht.text,
                            )
                        },
                        aufLangdruck = { gedrueckt = nachricht },
                    )
                }
                if (ki.teilAntwort.isNotBlank()) {
                    item(key = "stroemend") {
                        StroemendeAntwort(
                            text = ki.teilAntwort,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                if (ki.antwortet && ki.teilAntwort.isBlank()) {
                    item(key = "denkt") {
                        DenktNach(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        gedrueckt?.let { gewaehlte ->
            NachrichtLoeschBlatt(
                nachricht = gewaehlte,
                aufSchliessen = { gedrueckt = null },
                aufNachrichtLoeschen = {
                    viewModel.loescheNachricht(gewaehlte)
                    gedrueckt = null
                },
                aufPaarLoeschen = {
                    viewModel.loescheFrageUndAntwort(gewaehlte)
                    gedrueckt = null
                },
                aufKonversationLoeschen = {
                    viewModel.loescheKonversation(aktuelle.id)
                    gedrueckt = null
                },
            )
        }

        EingabeZeile(
            text = eingabe,
            aufText = {
                eingabe = it
                if (korrektur != null && it != korrektur?.korrigiert) viewModel.korrekturVergessen()
            },
            antwortet = ki.antwortet,
            nimmtAuf = aufnahme.laeuft,
            uebertraegt = aufnahme.wirdUebertragen,
            aufSenden = {
                viewModel.frage(aktuelle, eingabe)
                eingabe = ""
            },
            aufAbbrechen = viewModel::brichKiAb,
            aufMikrofon = {
                if (aufnahme.laeuft) {
                    viewModel.beendeAufnahme { text ->
                        eingabe = if (eingabe.isBlank()) text else "$eingabe $text"
                    }
                } else {
                    viewModel.starteAufnahme()
                }
            },
            korrigiert = korrektur != null,
            aufKorrigieren = {
                viewModel.korrigiereText(eingabe) { neu -> eingabe = neu }
            },
            aufZuruecknehmen = {
                viewModel.korrekturZuruecknehmen { alt -> eingabe = alt }
            },
        )
    }
}

@Composable
private fun IdeenKopf(
    idee: IdeeEntity,
    kategorien: List<de.frank.genialeideen.data.local.KategorieEntity>,
    aufKategorie: (Long?) -> Unit,
    aufNeueKategorie: (String, de.frank.genialeideen.data.local.Kategorieart, (Long?) -> Unit) -> Unit,
    spricht: Boolean,
    vorleseZustand: de.frank.genialeideen.speech.VorleseZustand,
    aufVorlesen: () -> Unit,
    aufUmgesetzt: () -> Unit,
    aufLoeschen: () -> Unit,
) {
    val gold = LocalGold.current
    val umgesetzt = idee.status == IdeenStatus.UMGESETZT.name
    GoldKarte(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        erhoeht = true,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    idee.text.ifBlank { "Ohne weiteren Text." },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = gold.textPrimaer,
                )
                Spacer(Modifier.width(8.dp))
                LautsprecherKnopf(spricht = spricht, zustand = vorleseZustand, aufTipp = aufVorlesen)
            }
            Spacer(Modifier.height(12.dp))
            KategorieWahl(
                kategorien = kategorien,
                gewaehlt = idee.kategorieId,
                aufWahl = aufKategorie,
                aufNeueKategorie = { name, art, fertig ->
                    aufNeueKategorie(name, art) { id ->
                        if (id != null) aufKategorie(id)
                        fertig(id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UmsetzungsKnopf(umgesetzt = umgesetzt, aufTipp = aufUmgesetzt)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(38.dp).druckEffekt(aufLoeschen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Idee löschen",
                        tint = gold.textGedaempft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Der grosse Knopf „Umgesetzt" mit kleiner Feier beim Umlegen (Baustein N.2). */
@Composable
private fun UmsetzungsKnopf(umgesetzt: Boolean, aufTipp: () -> Unit) {
    val gold = LocalGold.current
    val farbe = if (umgesetzt) Semantisch.erfolg else gold.primaer
    Row(
        modifier = Modifier
            .druckEffekt(aufTipp)
            .goldSchein(farbe.copy(alpha = 0.55f), hoehe = 10.dp, radius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (umgesetzt) {
                    Brush.horizontalGradient(listOf(Semantisch.erfolg, Semantisch.erfolg.copy(alpha = 0.75f)))
                } else {
                    Brush.horizontalGradient(listOf(gold.primaer, gold.primaerGedaempft))
                },
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (umgesetzt) Icons.Default.Undo else Icons.Default.Check,
            contentDescription = null,
            tint = gold.aufPrimaer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (umgesetzt) "Wieder offen" else "Umgesetzt",
            color = gold.aufPrimaer,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NachrichtenBlase(
    nachricht: NachrichtEntity,
    modifier: Modifier = Modifier,
    spricht: Boolean,
    vorleseZustand: de.frank.genialeideen.speech.VorleseZustand,
    aufVorlesen: () -> Unit,
    aufLangdruck: () -> Unit = {},
    laufenderAbsatz: String = "",
) {
    val gold = LocalGold.current
    val vonMir = nachricht.rolle == "user"
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (vonMir) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = aufLangdruck,
                    onClick = {},
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (vonMir) 18.dp else 4.dp,
                        bottomEnd = if (vonMir) 4.dp else 18.dp,
                    ),
                )
                .background(if (vonMir) gold.primaer.copy(alpha = 0.16f) else gold.flaeche)
                .border(
                    1.dp,
                    if (vonMir) gold.primaer.copy(alpha = 0.35f) else gold.rahmen,
                    RoundedCornerShape(18.dp),
                )
                .padding(14.dp),
        ) {
            // Der gerade gesprochene Absatz ist hervorgehoben (N.7).
            Text(
                text = hervorgehobenerAbsatz(nachricht.text, laufenderAbsatz, gold.primaer),
                style = MaterialTheme.typography.bodyMedium,
                color = gold.textPrimaer,
            )
            if (nachricht.unvollstaendig) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Diese Antwort ist unvollständig — die Verbindung brach ab.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Semantisch.warnung,
                )
            }
            if (!vonMir) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LautsprecherKnopf(
                        spricht = spricht,
                        zustand = vorleseZustand,
                        aufTipp = aufVorlesen,
                    )
                }
            }
        }
    }
}

/** Der einlaufende Text mit blinkendem Schreib-Cursor (N.7). */
@Composable
private fun StroemendeAntwort(text: String, modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "cursor")
    val blinken by uebergang.animateFloat(
        initialValue = 0f,
        targetValue = if (reduziert) 1f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "blinkwert",
    )
    Column(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .tiefenSchatten(gold.primaer, Hoehe.karte, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(gold.flaeche)
            .border(1.dp, gold.primaer.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = gold.textPrimaer,
            )
            Box(
                modifier = Modifier
                    .padding(start = 3.dp, bottom = 3.dp)
                    .size(width = 2.dp, height = 16.dp)
                    .background(gold.primaer.copy(alpha = if (reduziert) 1f else blinken)),
            )
        }
    }
}

@Composable
private fun DenktNach(modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "denkt")
    val takt by uebergang.animateFloat(
        initialValue = 0.4f,
        targetValue = if (reduziert) 0.4f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "taktwert",
    )
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(8.dp)
                    .scale(if (index == 1) takt else 1f)
                    .clip(CircleShape)
                    .background(gold.primaer.copy(alpha = takt)),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("denkt nach …", style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft)
    }
}

@Composable
private fun EingabeZeile(
    text: String,
    aufText: (String) -> Unit,
    antwortet: Boolean,
    nimmtAuf: Boolean,
    uebertraegt: Boolean,
    aufSenden: () -> Unit,
    aufAbbrechen: () -> Unit,
    aufMikrofon: () -> Unit,
    korrigiert: Boolean,
    aufKorrigieren: () -> Unit,
    aufZuruecknehmen: () -> Unit,
) {
    val gold = LocalGold.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gold.flaeche)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        AnimatedVisibility(visible = antwortet) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Antwort läuft ein",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.textGedaempft,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .druckEffekt(aufAbbrechen)
                        .clip(RoundedCornerShape(10.dp))
                        .background(gold.flaecheErhoeht)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("Abbrechen", style = MaterialTheme.typography.labelSmall, color = gold.primaer)
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp, max = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gold.eingabefeld)
                    .border(1.dp, gold.rahmen, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        if (nimmtAuf) "Sprich einfach los …" else "Frag etwas zu dieser Idee",
                        style = MaterialTheme.typography.bodyMedium,
                        color = gold.textGedaempft,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = aufText,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = gold.textPrimaer),
                    cursorBrush = SolidColor(gold.primaer),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (text.isNotBlank()) {
                KorrekturKnopf(
                    korrigiert = korrigiert,
                    laeuft = antwortet,
                    aufKorrigieren = aufKorrigieren,
                    aufZuruecknehmen = aufZuruecknehmen,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            RundKnopf(
                beschreibung = if (nimmtAuf) "Aufnahme beenden" else "Antwort einsprechen",
                farbe = if (nimmtAuf) Semantisch.fehler else gold.primaer,
                aufTipp = aufMikrofon,
            ) {
                Icon(
                    imageVector = if (nimmtAuf) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (nimmtAuf) Semantisch.fehler else gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            RundKnopf(
                beschreibung = "Frage senden",
                farbe = gold.primaer,
                gefuellt = true,
                aktiviert = text.isNotBlank() && !antwortet && !uebertraegt,
                aufTipp = aufSenden,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = gold.aufPrimaer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun RundKnopf(
    beschreibung: String,
    farbe: Color,
    aufTipp: () -> Unit,
    gefuellt: Boolean = false,
    aktiviert: Boolean = true,
    inhalt: @Composable () -> Unit,
) {
    val gold = LocalGold.current
    Box(
        modifier = Modifier
            .size(46.dp)
            .then(if (aktiviert) Modifier.druckEffekt(aufTipp) else Modifier)
            .clip(CircleShape)
            .background(
                when {
                    gefuellt && aktiviert -> Brush.linearGradient(listOf(gold.primaer, gold.primaerGedaempft))
                    gefuellt -> Brush.linearGradient(listOf(gold.rahmen, gold.rahmen))
                    else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                },
            )
            .border(if (gefuellt) 0.dp else 1.dp, if (gefuellt) Color.Transparent else farbe.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { inhalt() }
}

/**
 * Hebt den Absatz hervor, der gerade vorgelesen wird. Der Vergleich läuft über die ersten
 * Wörter, weil der gesprochene Text vorher durch die TTS-Aufbereitung gelaufen ist und
 * deshalb nicht mehr Zeichen für Zeichen mit dem angezeigten übereinstimmt.
 */
private fun hervorgehobenerAbsatz(
    text: String,
    laufend: String,
    farbe: androidx.compose.ui.graphics.Color,
) = androidx.compose.ui.text.buildAnnotatedString {
    if (laufend.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    val anker = laufend.take(24).trim()
    val absaetze = text.split(ZEILENUMBRUCH)
    absaetze.forEachIndexed { index, absatz ->
        val passt = anker.isNotBlank() && absatz.take(28).contains(anker.take(12), ignoreCase = true)
        if (passt) {
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = farbe,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                ),
            ) { append(absatz) }
        } else {
            append(absatz)
        }
        if (index < absaetze.lastIndex) append(ZEILENUMBRUCH)
    }
}

/**
 * Was bei Langdruck auf eine Nachricht erscheint: Frage, Antwort, das Paar oder die ganze
 * Unterhalfung fliegen raus. Nichts davon fragt zweimal nach — die Meldung unten sagt, was weg ist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NachrichtLoeschBlatt(
    nachricht: NachrichtEntity,
    aufSchliessen: () -> Unit,
    aufNachrichtLoeschen: () -> Unit,
    aufPaarLoeschen: () -> Unit,
    aufKonversationLoeschen: () -> Unit,
) {
    val gold = LocalGold.current
    val vonMir = nachricht.rolle == "user"
    val blatt = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = aufSchliessen,
        sheetState = blatt,
        containerColor = gold.flaecheErhoeht,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                if (vonMir) "Diese Frage" else "Diese Antwort",
                style = MaterialTheme.typography.labelSmall,
                color = gold.textGedaempft,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                nachricht.text.trim().take(120),
                style = MaterialTheme.typography.bodySmall,
                color = gold.textPrimaer,
            )
            Spacer(Modifier.height(14.dp))
            LoeschZeile(
                symbol = Icons.Default.Delete,
                text = if (vonMir) "Frage löschen" else "Antwort löschen",
                aufTipp = aufNachrichtLoeschen,
            )
            LoeschZeile(
                symbol = Icons.Default.QuestionAnswer,
                text = "Frage und Antwort löschen",
                aufTipp = aufPaarLoeschen,
            )
            LoeschZeile(
                symbol = Icons.Default.DeleteSweep,
                text = "Ganze Unterhaltung löschen",
                warnend = true,
                aufTipp = aufKonversationLoeschen,
            )
        }
    }
}

@Composable
private fun LoeschZeile(
    symbol: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    aufTipp: () -> Unit,
    warnend: Boolean = false,
) {
    val gold = LocalGold.current
    val farbe = if (warnend) Semantisch.fehler else gold.textPrimaer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .druckEffekt(aufTipp)
            .clip(RoundedCornerShape(14.dp))
            .background(gold.flaeche)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            symbol,
            contentDescription = null,
            tint = farbe,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = farbe)
    }
}

private const val ZEILENUMBRUCH = "\n"
