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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.speech.VorleseZustand
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Motion
import de.frank.genialeideen.ui.theme.Semantisch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.launch

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

    val kategorien by viewModel.kategorien.collectAsState()
    val gewaehlteKategorie by viewModel.gewaehlteKategorie.collectAsState()

    var bereich by remember { mutableStateOf(ListenBereich.OFFEN) }
    var suchOffen by remember { mutableStateOf(false) }

    val schublade = rememberDrawerState(DrawerValue.Closed)
    val bereichsraum = rememberCoroutineScope()
    val kategorieName = kategorien.firstOrNull { it.id == gewaehlteKategorie }?.name

    // Zurückwischen hebt zuerst die Kategorie auf, erst danach verlässt man die Liste.
    BackHandler(enabled = gewaehlteKategorie != null) { viewModel.waehleKategorie(null) }

    val roheListe = if (bereich == ListenBereich.OFFEN) offene else umgesetzte
    val liste = if (gewaehlteKategorie == null) {
        roheListe
    } else {
        roheListe.filter { it.kategorieId == gewaehlteKategorie }
    }
    // Die gezogene Reihenfolge lebt lokal, bis der Finger losgelassen wird.
    var reihenfolge by remember(liste) { mutableStateOf(liste.map(IdeeEntity::id)) }
    LaunchedEffect(liste) { reihenfolge = liste.map(IdeeEntity::id) }
    val nachId = liste.associateBy(IdeeEntity::id)
    val sortiert = reihenfolge.mapNotNull(nachId::get)

    val zustand = rememberReorderState()
    val listState = rememberLazyListState()
    ReorderAutoScroll(zustand, listState)

    ModalNavigationDrawer(
        drawerState = schublade,
        drawerContent = {
            KategorienLeiste(
                kategorien = kategorien,
                gewaehlt = gewaehlteKategorie,
                anzahlJeKategorie = alleZaehlung(offene + umgesetzte),
                gesamt = offene.size + umgesetzte.size,
                aufWahl = { id ->
                    viewModel.waehleKategorie(id)
                    bereichsraum.launch { schublade.close() }
                },
                aufNeueKategorie = { name -> viewModel.legeKategorieAn(name) },
                aufLoeschen = { id -> viewModel.loescheKategorie(id) },
            )
        },
    ) {
    Box(Modifier.fillMaxSize().background(hintergrundVerlauf())) {
    BewegterHintergrund()
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        IdeenKopfleiste(
            titel = kategorieName ?: "Geniale Ideen",
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
                sortiert.isEmpty() && gewaehlteKategorie != null -> Leerzustand(
                    symbol = "🗂",
                    ueberschrift = "Hier liegt noch nichts",
                    satz = "In dieser Kategorie steht in dieser Liste noch keine Idee.",
                    knopfText = "Alle Ideen zeigen",
                    aufKnopf = { viewModel.waehleKategorie(null) },
                    modifier = Modifier.align(Alignment.Center),
                )
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
                            )
                        }
                    }
                }
            }

            if (!suchOffen) {
                AufnahmeKnopfMitPegel(
                    laeuft = aufnahme.laeuft,
                    pegel = aufnahme.pegel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                        .navigationBarsPadding(),
                    groesse = 68.dp,
                    aufTipp = aufNeueIdee,
                )
            }

            // Der Seitenschalter: ein schmaler Griff am linken Rand holt die Kategorien herein.
            SeitenGriff(
                modifier = Modifier.align(Alignment.CenterStart),
                aufTipp = { bereichsraum.launch { schublade.open() } },
            )
        }
    }
    }
    }
}

/** Zählt, wie viele Ideen in jeder Kategorie liegen. */
private fun alleZaehlung(ideen: List<IdeeEntity>): Map<Long, Int> =
    ideen.mapNotNull { it.kategorieId }.groupingBy { it }.eachCount()

/** Der Griff am linken Rand, der die Seitenleiste hereinholt (Baustein P). */
@Composable
private fun SeitenGriff(modifier: Modifier = Modifier, aufTipp: () -> Unit) {
    val gold = LocalGold.current
    Box(
        modifier = modifier
            .padding(start = 0.dp)
            .size(width = 22.dp, height = 82.dp)
            .druckEffekt(aufTipp)
            .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(gold.primaer.copy(alpha = 0.85f), gold.primaerGedaempft.copy(alpha = 0.55f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Kategorien einblenden",
            tint = gold.aufPrimaer,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Die Seitenleiste mit allen Kategorien (Baustein P). */
@Composable
private fun KategorienLeiste(
    kategorien: List<KategorieEntity>,
    gewaehlt: Long?,
    anzahlJeKategorie: Map<Long, Int>,
    gesamt: Int,
    aufWahl: (Long?) -> Unit,
    aufNeueKategorie: (String) -> Unit,
    aufLoeschen: (Long) -> Unit,
) {
    val gold = LocalGold.current
    var neuOffen by remember { mutableStateOf(false) }
    var neuerName by remember { mutableStateOf("") }

    ModalDrawerSheet(
        drawerContainerColor = gold.hintergrund,
        drawerContentColor = gold.textPrimaer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                "Kategorien",
                modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = gold.primaer,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                item {
                    KategorieZeile(
                        name = "Alle Ideen",
                        anzahl = gesamt,
                        gewaehlt = gewaehlt == null,
                        aufTipp = { aufWahl(null) },
                        aufLoeschen = null,
                    )
                }
                items(kategorien, key = KategorieEntity::id) { kategorie ->
                    KategorieZeile(
                        name = kategorie.name,
                        anzahl = anzahlJeKategorie[kategorie.id] ?: 0,
                        gewaehlt = gewaehlt == kategorie.id,
                        aufTipp = { aufWahl(kategorie.id) },
                        aufLoeschen = { aufLoeschen(kategorie.id) },
                    )
                }
            }

            if (neuOffen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gold.eingabefeld)
                        .border(1.dp, gold.primaer, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (neuerName.isBlank()) {
                            Text(
                                "Name der Kategorie",
                                style = MaterialTheme.typography.bodyMedium,
                                color = gold.textGedaempft,
                            )
                        }
                        BasicTextField(
                            value = neuerName,
                            onValueChange = { neuerName = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = gold.textPrimaer),
                            cursorBrush = SolidColor(gold.primaer),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        modifier = Modifier.size(30.dp).druckEffekt {
                            if (neuerName.isNotBlank()) aufNeueKategorie(neuerName.trim())
                            neuerName = ""
                            neuOffen = false
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Kategorie anlegen",
                            tint = gold.primaer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .druckEffekt { neuOffen = !neuOffen }
                    .clip(RoundedCornerShape(14.dp))
                    .background(gold.flaecheErhoeht)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = gold.primaer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Neue Kategorie",
                    style = MaterialTheme.typography.labelLarge,
                    color = gold.primaer,
                )
            }
        }
    }
}

@Composable
private fun KategorieZeile(
    name: String,
    anzahl: Int,
    gewaehlt: Boolean,
    aufTipp: () -> Unit,
    aufLoeschen: (() -> Unit)?,
) {
    val gold = LocalGold.current
    val farbe by animateColorAsState(
        if (gewaehlt) gold.primaer.copy(alpha = 0.18f) else Color.Transparent,
        label = "kategoriezeile",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .druckEffekt(aufTipp)
            .clip(RoundedCornerShape(12.dp))
            .background(farbe)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Label,
            contentDescription = null,
            tint = if (gewaehlt) gold.primaer else gold.textGedaempft,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (gewaehlt) gold.primaer else gold.textPrimaer,
            maxLines = 1,
        )
        Text(
            anzahl.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = gold.textGedaempft,
        )
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
) {
    val gold = LocalGold.current
    val umgesetzt = idee.status == IdeenStatus.UMGESETZT.name
    GoldKarte(modifier = modifier.fillMaxWidth(), kippbar = true) {
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
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
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                            ),
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
