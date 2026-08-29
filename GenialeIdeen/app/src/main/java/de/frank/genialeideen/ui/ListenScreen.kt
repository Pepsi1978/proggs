package de.frank.genialeideen.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.speech.VorleseZustand
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.Semantisch

enum class ListenBereich(val titel: String) {
    OFFEN("Offene Ideen"),
    UMGESETZT("Umgesetzt"),
}

@Composable
fun ListenScreen(
    viewModel: IdeenViewModel,
    aufIdee: (IdeeEntity) -> Unit,
    aufNeueIdee: () -> Unit,
    aufEinstellungen: () -> Unit,
) {
    val gold = LocalGold.current
    val theme by viewModel.theme.collectAsState()
    val offene by viewModel.offeneIdeen.collectAsState()
    val umgesetzte by viewModel.umgesetzteIdeen.collectAsState()
    val laedt by viewModel.laedt.collectAsState()
    val suchtext by viewModel.suchtext.collectAsState()
    val treffer by viewModel.suchtreffer.collectAsState()
    val letzteAnfragen by viewModel.letzteSuchanfragen.collectAsState()
    val vorlese by viewModel.vorleseStand.collectAsState()
    val aufnahme by viewModel.aufnahme.collectAsState()

    var bereich by remember { mutableStateOf(ListenBereich.OFFEN) }
    var suchOffen by remember { mutableStateOf(false) }

    val liste = if (bereich == ListenBereich.OFFEN) offene else umgesetzte
    // Die gezogene Reihenfolge lebt lokal, bis der Finger losgelassen wird.
    var reihenfolge by remember(liste) { mutableStateOf(liste.map(IdeeEntity::id)) }
    LaunchedEffect(liste) { reihenfolge = liste.map(IdeeEntity::id) }
    val nachId = liste.associateBy(IdeeEntity::id)
    val sortiert = reihenfolge.mapNotNull(nachId::get)

    val zustand = rememberReorderState()
    val listState = rememberLazyListState()
    ReorderAutoScroll(zustand, listState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(hintergrundVerlauf()),
    ) {
        IdeenKopfleiste(
            titel = "Geniale Ideen",
            themeWahl = theme,
            aufThemeTipp = viewModel::themeWeiterschalten,
            aufEinstellungen = aufEinstellungen,
        )

        SuchZeile(
            offen = suchOffen,
            text = suchtext,
            letzteAnfragen = letzteAnfragen,
            aufOeffnen = { suchOffen = true },
            aufText = viewModel::suche,
            aufSchliessen = {
                suchOffen = false
                viewModel.leereSuche()
            },
            aufVerlaufLeeren = viewModel::leereSuchverlauf,
        )

        if (!suchOffen) {
            BereichsWaehler(bereich, offene.size, umgesetzte.size) { bereich = it }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                suchOffen -> SuchErgebnisse(
                    anfrage = suchtext,
                    treffer = treffer,
                    aufIdee = aufIdee,
                    aufLeeren = viewModel::leereSuche,
                )
                laedt -> SchimmerGeruest(zeilen = 4, modifier = Modifier.padding(16.dp))
                sortiert.isEmpty() && bereich == ListenBereich.OFFEN -> Leerzustand(
                    symbol = "💡",
                    ueberschrift = "Noch keine Idee festgehalten",
                    satz = "Sprich sie einfach ein, bevor sie wieder weg ist. " +
                        "Der Knopf unten nimmt sofort auf.",
                    knopfText = "Erste Idee festhalten",
                    aufKnopf = aufNeueIdee,
                    modifier = Modifier.align(Alignment.Center),
                )
                sortiert.isEmpty() -> Leerzustand(
                    symbol = "✅",
                    ueberschrift = "Noch nichts umgesetzt",
                    satz = "Sobald du in einer Idee auf „Umgesetzt“ tippst, steht sie hier.",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().reorderViewport(zustand),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(sortiert, key = IdeeEntity::id) { idee ->
                        val index = sortiert.indexOf(idee)
                        GestaffeltEinblenden(sichtbar = true, index = index.coerceAtMost(8)) {
                            IdeenKarte(
                                idee = idee,
                                modifier = Modifier.reorderRow(zustand, idee.id),
                                griff = reorderHandle(
                                    state = zustand,
                                    id = idee.id,
                                    order = { reihenfolge },
                                    onMove = { von, nach ->
                                        reihenfolge = reihenfolge.toMutableList().apply {
                                            add(nach, removeAt(von))
                                        }
                                    },
                                    onDrop = { viewModel.schreibeReihenfolge(reihenfolge) },
                                ),
                                spricht = vorlese.quelle == "idee-${idee.id}",
                                vorleseZustand = vorlese.zustand,
                                aufTipp = { aufIdee(idee) },
                                aufVorlesen = {
                                    viewModel.lies("idee-${idee.id}", idee.titel, "${idee.titel}.\n\n${idee.text}")
                                },
                                aufUmgesetzt = {
                                    if (idee.status == IdeenStatus.OFFEN.name) {
                                        viewModel.setzeUmgesetzt(idee)
                                    } else {
                                        viewModel.zurueckZuOffen(idee)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (!suchOffen) {
                AufnahmeKnopf(
                    laeuft = aufnahme.laeuft,
                    uebertraegt = aufnahme.wirdUebertragen,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                        .navigationBarsPadding(),
                    aufTipp = aufNeueIdee,
                )
            }
        }
    }
}

@Composable
private fun hintergrundVerlauf(): Brush {
    val gold = LocalGold.current
    return Brush.verticalGradient(
        colors = listOf(
            gold.hintergrund,
            gold.hintergrund,
            if (gold.istDunkel) Color(0xFF171308) else Color(0xFFF5EEDF),
        ),
    )
}

@Composable
private fun BereichsWaehler(
    aktuell: ListenBereich,
    offene: Int,
    umgesetzte: Int,
    aufWahl: (ListenBereich) -> Unit,
) {
    val gold = LocalGold.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gold.flaeche)
            .border(1.dp, gold.rahmen, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ListenBereich.entries.forEach { eintrag ->
            val gewaehlt = eintrag == aktuell
            val farbe by animateColorAsState(
                if (gewaehlt) gold.primaer else Color.Transparent,
                label = "bereich",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .druckEffekt { aufWahl(eintrag) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(farbe)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val anzahl = if (eintrag == ListenBereich.OFFEN) offene else umgesetzte
                Text(
                    text = "${eintrag.titel} ($anzahl)",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (gewaehlt) gold.aufPrimaer else gold.textGedaempft,
                )
            }
        }
    }
}

@Composable
private fun IdeenKarte(
    idee: IdeeEntity,
    modifier: Modifier,
    griff: Modifier,
    spricht: Boolean,
    vorleseZustand: VorleseZustand,
    aufTipp: () -> Unit,
    aufVorlesen: () -> Unit,
    aufUmgesetzt: () -> Unit,
) {
    val gold = LocalGold.current
    val umgesetzt = idee.status == IdeenStatus.UMGESETZT.name
    GoldKarte(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = griff.padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = "Zum Sortieren lang drücken und ziehen",
                    tint = gold.textGedaempft,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .druckEffekt(aufTipp)
                    .padding(end = 8.dp),
            ) {
                Text(
                    idee.titel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (umgesetzt) gold.textGedaempft else gold.textPrimaer,
                    maxLines = 2,
                )
                if (idee.text.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        idee.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = gold.textGedaempft,
                        maxLines = 2,
                    )
                }
            }
            LautsprecherKnopf(
                spricht = spricht,
                zustand = vorleseZustand,
                aufTipp = aufVorlesen,
            )
            Spacer(Modifier.width(4.dp))
            UmgesetztKnopf(umgesetzt = umgesetzt, aufTipp = aufUmgesetzt)
        }
    }
}

/** Zeigt „lädt", „spricht" oder „aus" und stoppt bei erneutem Tipp sofort (Baustein D 4.4). */
@Composable
fun LautsprecherKnopf(
    spricht: Boolean,
    zustand: VorleseZustand,
    aufTipp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "atem")
    val atem by uebergang.animateFloat(
        initialValue = 1f,
        targetValue = if (spricht && zustand == VorleseZustand.SPRICHT && !reduziert) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "atemwert",
    )
    Box(
        modifier = modifier
            .size(38.dp)
            .scale(if (spricht) atem else 1f)
            .druckEffekt(aufTipp)
            .clip(CircleShape)
            .background(if (spricht) gold.primaer.copy(alpha = 0.20f) else Color.Transparent)
            .border(
                1.dp,
                if (spricht) gold.primaer else gold.rahmen,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = spricht to zustand,
            transitionSpec = { fadeIn(tween(Motion.MIKRO_MS)) togetherWith fadeOut(tween(Motion.MIKRO_MS)) },
            label = "lautsprecher",
        ) { (aktiv, stand) ->
            when {
                aktiv && stand == VorleseZustand.LAEDT -> Text("…", color = gold.primaer, fontSize = 16.sp)
                aktiv -> Icon(
                    Icons.Default.Stop,
                    contentDescription = "Vorlesen anhalten",
                    tint = gold.primaer,
                    modifier = Modifier.size(18.dp),
                )
                else -> Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Vorlesen",
                    tint = gold.textGedaempft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UmgesetztKnopf(umgesetzt: Boolean, aufTipp: () -> Unit) {
    val gold = LocalGold.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .druckEffekt(aufTipp)
            .clip(CircleShape)
            .background(if (umgesetzt) Semantisch.erfolg.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, if (umgesetzt) Semantisch.erfolg else gold.rahmen, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (umgesetzt) Icons.Default.Undo else Icons.Default.Check,
            contentDescription = if (umgesetzt) "Zurück zu den offenen Ideen" else "Als umgesetzt markieren",
            tint = if (umgesetzt) Semantisch.erfolg else gold.textGedaempft,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AufnahmeKnopf(
    laeuft: Boolean,
    uebertraegt: Boolean,
    modifier: Modifier,
    aufTipp: () -> Unit,
) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val uebergang = rememberInfiniteTransition(label = "puls")
    val puls by uebergang.animateFloat(
        initialValue = 1f,
        targetValue = if ((laeuft || uebertraegt) && !reduziert) 1.10f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulswert",
    )
    Box(
        modifier = modifier
            .scale(puls)
            .size(68.dp)
            .druckEffekt(aufTipp)
            .goldSchein(gold.primaer, hoehe = 18.dp, radius = 34.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(gold.primaer, gold.primaerGedaempft))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Neue Idee einsprechen oder eintippen",
            tint = gold.aufPrimaer,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun SuchZeile(
    offen: Boolean,
    text: String,
    letzteAnfragen: List<String>,
    aufOeffnen: () -> Unit,
    aufText: (String) -> Unit,
    aufSchliessen: () -> Unit,
    aufVerlaufLeeren: () -> Unit,
) {
    val gold = LocalGold.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gold.eingabefeld)
            .border(1.dp, gold.rahmen, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Suchen",
            tint = gold.textGedaempft,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (!offen) {
                Text(
                    "Alle Ideen durchsuchen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = gold.textGedaempft,
                    modifier = Modifier.druckEffekt(aufOeffnen).fillMaxWidth(),
                )
            } else {
                if (text.isEmpty()) {
                    Text(
                        "Suchwort eingeben",
                        style = MaterialTheme.typography.bodyMedium,
                        color = gold.textGedaempft,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = aufText,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = gold.textPrimaer),
                    cursorBrush = SolidColor(gold.primaer),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (offen) {
            Box(
                modifier = Modifier.size(24.dp).druckEffekt(aufSchliessen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Suche schliessen",
                    tint = gold.textGedaempft,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
    if (offen && text.isBlank() && letzteAnfragen.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            letzteAnfragen.take(3).forEach { anfrage ->
                Box(
                    modifier = Modifier
                        .druckEffekt { aufText(anfrage) }
                        .clip(RoundedCornerShape(10.dp))
                        .background(gold.flaecheErhoeht)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(anfrage, style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft)
                }
            }
            Box(modifier = Modifier.druckEffekt(aufVerlaufLeeren).padding(6.dp)) {
                Text("leeren", style = MaterialTheme.typography.labelSmall, color = gold.primaer)
            }
        }
    }
}

@Composable
private fun SuchErgebnisse(
    anfrage: String,
    treffer: List<IdeeEntity>,
    aufIdee: (IdeeEntity) -> Unit,
    aufLeeren: () -> Unit,
) {
    val gold = LocalGold.current
    when {
        anfrage.isBlank() -> Leerzustand(
            symbol = "🔍",
            ueberschrift = "Wonach suchst du?",
            satz = "Die Suche findet jedes Wort aus Titel und Text — Umlaute sind ihr egal.",
        )
        treffer.isEmpty() -> Leerzustand(
            symbol = "🫙",
            ueberschrift = "Nichts gefunden für „$anfrage“",
            satz = "Vielleicht steckt die Idee unter einem anderen Wort.",
            knopfText = "Suche leeren",
            aufKnopf = aufLeeren,
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(treffer, key = IdeeEntity::id) { idee ->
                GoldKarte(modifier = Modifier.fillMaxWidth().druckEffekt { aufIdee(idee) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            hervorgehoben(idee.titel, anfrage, gold.primaer),
                            style = MaterialTheme.typography.titleSmall,
                            color = gold.textPrimaer,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            hervorgehoben(fundstelle(idee.text, anfrage), anfrage, gold.primaer),
                            style = MaterialTheme.typography.bodySmall,
                            color = gold.textGedaempft,
                            maxLines = 3,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (idee.status == IdeenStatus.UMGESETZT.name) "Umgesetzt" else "Offen",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idee.status == IdeenStatus.UMGESETZT.name) {
                                Semantisch.erfolg
                            } else {
                                gold.primaer
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Schneidet den Text um die Fundstelle herum zu, damit der Treffer sichtbar ist. */
private fun fundstelle(text: String, anfrage: String): String {
    val stelle = text.indexOf(anfrage.trim(), ignoreCase = true)
    if (stelle < 0) return text.take(160)
    val start = (stelle - 50).coerceAtLeast(0)
    val ende = (stelle + anfrage.length + 90).coerceAtMost(text.length)
    return (if (start > 0) "… " else "") + text.substring(start, ende) + (if (ende < text.length) " …" else "")
}

private fun hervorgehoben(text: String, anfrage: String, farbe: Color) = buildAnnotatedString {
    val wort = anfrage.trim()
    if (wort.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var index = 0
    while (index < text.length) {
        val treffer = text.indexOf(wort, index, ignoreCase = true)
        if (treffer < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, treffer))
        withStyle(SpanStyle(color = farbe, fontWeight = FontWeight.SemiBold)) {
            append(text.substring(treffer, treffer + wort.length))
        }
        index = treffer + wort.length
    }
}
