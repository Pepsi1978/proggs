package de.frank.newskompass.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.data.Archiv
import de.frank.newskompass.data.AusgabenEintrag
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.DesignModus
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.news.SprachStufe
import de.frank.newskompass.news.Zeitplan
import de.frank.newskompass.tts.VorleseStufe
import de.frank.newskompass.tts.VorleseZustand
import de.frank.newskompass.ui.theme.LocalIstDunkel
import de.frank.newskompass.ui.theme.blockFarbe
import de.frank.newskompass.ui.theme.blockVerlauf
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NachrichtenScreen(app: NewsApplication, oeffneEinstellungen: () -> Unit) {
    val kontext = LocalContext.current
    val index by app.speicher.index.collectAsStateWithLifecycle()
    val beschaedigt by app.speicher.beschaedigt.collectAsStateWithLifecycle()
    val stand by app.einstellungen.stand.collectAsStateWithLifecycle()
    val vorlesen by app.vorleser.zustand.collectAsStateWithLifecycle()
    val arbeit by remember { WorkManager.getInstance(kontext).getWorkInfosForUniqueWorkFlow(Zeitplan.LAUF) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val lauf = arbeit.firstOrNull()
    val sprache by app.sprachFrage.zustand.collectAsStateWithLifecycle()
    val fragen by remember { WorkManager.getInstance(kontext).getWorkInfosForUniqueWorkFlow(Zeitplan.FRAGE) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val offeneFragen = fragen.filter { !it.state.isFinished }
    val gescheiterteFragen = fragen.filter { it.id in sprache.vorlesen && it.state.isFinished && it.outputData.getString("fehler") != null }
    val mikrofonErlaubnis = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erteilt ->
        app.sprachFrage.erlaubnisErhalten(erteilt)
    }

    // Im Hintergrund schaltet Android das Mikrofon stumm — eine offene Aufnahme wird verworfen.
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lebenszyklus) {
        val beobachter = LifecycleEventObserver { _, ereignis ->
            if (ereignis == Lifecycle.Event.ON_STOP) app.sprachFrage.brichAufnahmeAb()
        }
        lebenszyklus.addObserver(beobachter)
        onDispose {
            lebenszyklus.removeObserver(beobachter)
            app.sprachFrage.brichAufnahmeAb()
        }
    }

    // Aktuell folgt der neuesten Ausgabe; ein bewusst gewählter Tag oder Monat bleibt stehen,
    // auch wenn im Hintergrund eine neue Ausgabe fertig wird.
    var auswahlText by rememberSaveable { mutableStateOf(Ansicht.Aktuell.text) }
    val auswahl = remember(auswahlText) { Ansicht.aus(auswahlText) }
    fun zeige(ansicht: Ansicht) {
        auswahlText = ansicht.text
    }

    val heute = rememberHeute()
    val archivTage = remember(index, beschaedigt, heute) { Archiv.tage(index, beschaedigt, heute) }
    val schadenJeTag = remember(beschaedigt) { Archiv.beschaedigteTage(beschaedigt) }
    val archivMonate = remember(index, heute) { Archiv.monate(index, heute) }
    val gewaehlterEintrag: AusgabenEintrag? = when (auswahl) {
        Ansicht.Aktuell -> index.firstOrNull()
        is Ansicht.Tag -> index.filter { it.tag == auswahl.datum }.let { tag -> tag.firstOrNull { it.id == auswahl.ausgabeId } ?: tag.firstOrNull() }
        is Ansicht.Monat -> null
    }
    val monat = (auswahl as? Ansicht.Monat)?.let { a -> archivMonate.firstOrNull { it.monat == a.monat } }
    // Der gezeigte Kalendertag — auch dann, wenn an ihm nur beschädigte Ausgaben liegen.
    val angezeigterTag = (auswahl as? Ansicht.Tag)?.datum ?: if (monat == null) gewaehlterEintrag?.tag else null
    val schadenAmTag = angezeigterTag?.let { schadenJeTag[it] } ?: 0
    // Im Monatsrückblick fehlen beschädigte Ausgaben — das soll dort sichtbar sein.
    val schadenImMonat = monat?.let { m -> schadenJeTag.filterKeys { YearMonth.from(it) == m.monat }.values.sum() } ?: 0
    // Gibt es den gewählten Tag oder Monat nicht mehr, zurück zu Aktuell.
    LaunchedEffect(auswahl, index, schadenJeTag) {
        val weg = (auswahl is Ansicht.Tag && gewaehlterEintrag == null && schadenAmTag == 0) || (auswahl is Ansicht.Monat && monat == null)
        if (index.isNotEmpty() && weg) zeige(Ansicht.Aktuell)
    }

    // Volle Ausgaben kommen erst beim Anschauen aus dem Speicher; der Rückblick wird aus dem Archiv gebaut.
    val ansichtsId = monat?.let { Archiv.RUECKBLICK_PRAEFIX + it.monat } ?: gewaehlterEintrag?.id
    var geladen by remember { mutableStateOf<Geladen?>(null) }
    LaunchedEffect(ansichtsId, gewaehlterEintrag?.stand, monat) {
        val id = ansichtsId ?: return@LaunchedEffect
        geladen = when {
            monat != null -> Archiv.rueckblick(app.speicher, monat).let { Geladen(id, it.ausgabe, it.hinweise) }
            gewaehlterEintrag != null -> Geladen(id, app.speicher.ausgabe(gewaehlterEintrag.id), emptyList())
            else -> null
        }
    }
    val passend = geladen?.takeIf { it.id == ansichtsId }
    val ausgabe = passend?.ausgabe
    val archivHinweise = passend?.hinweise.orEmpty()
    val laedt = ansichtsId != null && passend == null
    // Geladen, aber leer: Die Datei ließ sich nicht lesen. Sie bleibt, wie sie ist.
    val ladeFehler = passend != null && passend.ausgabe == null
    val tagesAusgaben = if (monat != null) emptyList() else gewaehlterEintrag?.let { e -> index.filter { it.tag == e.tag } }.orEmpty()

    // Welche Bilder der gezeigten Ausgabe vorhanden sind: einmal im Hintergrund geprüft statt je Karte im
    // Hauptthread. Neu geprüft, sobald sich Ausgabe oder Archiv ändern — etwa wenn ein Import fehlende
    // Bilder nachliefert. Nur die gezeigte Ausgabe wird gehalten; dekodierte Bilder hält Coil selbst.
    var bildDateien by remember { mutableStateOf<Map<String, java.io.File>>(emptyMap()) }
    LaunchedEffect(ausgabe, index) {
        val namen = ausgabe?.bloecke.orEmpty().flatMap { b -> b.meldungen.mapNotNull { it.bildDatei } }.toSet()
        val gefunden = withContext(Dispatchers.IO) { namen.mapNotNull { n -> app.speicher.bildDatei(n)?.let { n to it } }.toMap() }
        bildDateien = gefunden
    }

    // Reihenfolge der Blöcke folgt immer der aktuellen Themenliste der Einstellungen.
    val bloecke = remember(ausgabe, stand.themen) {
        val rang = stand.themen.mapIndexed { i, t -> t.id to i }.toMap()
        ausgabe?.bloecke.orEmpty().sortedBy { rang[it.themaId] ?: Int.MAX_VALUE }
    }
    val dunkel = LocalIstDunkel.current
    val liste = rememberLazyListState()
    val bildschirm = rememberCoroutineScope()
    val schublade = rememberDrawerState(DrawerValue.Closed)

    // Zurück führt aus dem Archiv nach Aktuell; ist die Seitenleiste offen, schließt Zurück zuerst sie.
    BackHandler(auswahl != Ansicht.Aktuell) { zeige(Ansicht.Aktuell) }
    BackHandler(schublade.isOpen) { bildschirm.launch { schublade.close() } }

    // Block, zu dem die Ansicht springen soll, sobald er in der gewählten Ausgabe steht.
    var springeZu by remember { mutableStateOf<String?>(null) }

    // Eine gesprochene Frage bekommt eine gesprochene Antwort: Ist ihr Block da, springt die
    // Ansicht hin und er wird vorgelesen.
    LaunchedEffect(fragen, sprache.vorlesen, index) {
        fragen.filter { it.id in sprache.vorlesen && it.state.isFinished }.forEach { info ->
            if (info.outputData.getString("fehler") != null) return@forEach
            val ausgabeId = info.outputData.getString("ausgabeId")
            val themaId = info.outputData.getString("themaId")
            val eintrag = index.firstOrNull { it.id == ausgabeId }
            val antwort = eintrag?.let { app.speicher.ausgabe(it.id) }
            val block = antwort?.bloecke?.firstOrNull { it.themaId == themaId }
            if (eintrag == null || block == null) {
                // Noch nicht im Speicher angekommen — der nächste Durchlauf findet ihn.
                if (info.state != WorkInfo.State.SUCCEEDED || ausgabeId == null) app.sprachFrage.erledigt(info.id)
                return@forEach
            }
            app.sprachFrage.erledigt(info.id)
            zeige(if (eintrag.id == index.firstOrNull()?.id) Ansicht.Aktuell else Ansicht.Tag(eintrag.tag, eintrag.id))
            springeZu = block.themaId
            if (block.meldungen.isNotEmpty()) app.vorleser.lies("block-${block.themaId}", blockText(block))
        }
    }

    // Muss der Reihenfolge der Einträge in der Liste unten folgen.
    val vorspann = 1 + (if (auswahl != Ansicht.Aktuell) 1 else 0) + (if (tagesAusgaben.size > 1) 1 else 0) + 1 +
        offeneFragen.size + gescheiterteFragen.size + (if (monat != null) 1 else 0) + (if (archivHinweise.isNotEmpty()) 1 else 0) +
        (if (schadenAmTag > 0 || schadenImMonat > 0) 1 else 0) + (if (ladeFehler) 1 else 0) + (if (laedt) 1 else 0) + (if (index.isEmpty()) 1 else 0)
    LaunchedEffect(springeZu, bloecke, vorspann) {
        val ziel = springeZu ?: return@LaunchedEffect
        val position = bloecke.indexOfFirst { it.themaId == ziel }
        if (position < 0) return@LaunchedEffect
        var index = vorspann
        bloecke.take(position).forEach { index += 1 + (if (it.fehler != null) 1 else 0) + it.meldungen.size }
        springeZu = null
        // Eigener Bereich: Das Zurücksetzen von springeZu bricht diesen Effekt beim Aussetzen ab.
        bildschirm.launch { liste.animateScrollToItem(index) }
    }

    ModalNavigationDrawer(
        drawerState = schublade,
        drawerContent = {
            ArchivSeitenleiste(auswahl, heute, archivTage, archivMonate) { ansicht ->
                zeige(ansicht)
                bildschirm.launch {
                    schublade.close()
                    liste.scrollToItem(0)
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                state = liste,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "kopf", contentType = "kopf") {
                    val (titel, untertitel) = when {
                        monat != null -> "Rückblick" to Archiv.monatsName(monat.monat)
                        auswahl is Ansicht.Tag && angezeigterTag != null -> {
                            val datum = angezeigterTag
                            val gross = when (datum) {
                                heute, heute.minusDays(1) -> tagName(datum, heute)
                                else -> datum.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMANY))
                            }
                            gross to datum.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMANY))
                        }
                        else -> gruss() to heute.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMANY))
                    }
                    Kopf(
                        ausgabe = ausgabe,
                        titel = titel,
                        untertitel = untertitel,
                        dunkel = dunkel,
                        laeuft = lauf?.state == WorkInfo.State.RUNNING,
                        oeffneArchiv = { bildschirm.launch { schublade.open() } },
                        aktualisiere = { Zeitplan.starteLauf(kontext, manuell = true) },
                        wechsleDesign = { app.einstellungen.setzeDesign(if (dunkel) DesignModus.HELL else DesignModus.DUNKEL) },
                        oeffneEinstellungen = oeffneEinstellungen,
                    )
                }
                if (auswahl != Ansicht.Aktuell) {
                    item(key = "archiv", contentType = "archivleiste") {
                        Spalte {
                            ArchivHinweis(
                                text = when {
                                    monat != null -> "Monatsrückblick · ${Archiv.monatsName(monat.monat)}"
                                    angezeigterTag != null -> "Archiv · " + angezeigterTag.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMANY))
                                    else -> "Archiv"
                                },
                                zuAktuell = { zeige(Ansicht.Aktuell) },
                            )
                        }
                    }
                }
                if (tagesAusgaben.size > 1) {
                    item(key = "ausgaben", contentType = "ausgabenwahl") {
                        AusgabenWahl(tagesAusgaben, gewaehlterEintrag?.id) { id ->
                            gewaehlterEintrag?.let { zeige(Ansicht.Tag(it.tag, id)) }
                        }
                    }
                }
                item(key = "status", contentType = "status") {
                    Spalte { LaufStatus(lauf, app.codex.istVerbunden, oeffneEinstellungen) }
                }
                items(offeneFragen, key = { "frage-${it.id}" }, contentType = { "frage" }) { info ->
                    Spalte {
                        FrageLaeuft(
                            frage = info.tags.firstOrNull { it.startsWith(Zeitplan.FRAGE_ETIKETT) }?.removePrefix(Zeitplan.FRAGE_ETIKETT).orEmpty(),
                            text = info.progress.getString("text")
                                ?: if (info.state == WorkInfo.State.RUNNING) "Deine Frage wird recherchiert …" else "Wartet auf die Recherche …",
                            anteil = info.progress.getFloat("anteil", 0f),
                            laeuft = info.state == WorkInfo.State.RUNNING,
                            verwerfen = { WorkManager.getInstance(kontext).cancelWorkById(info.id) },
                        )
                    }
                }
                items(gescheiterteFragen, key = { "frage-fehler-${it.id}" }, contentType = { "hinweis" }) { info ->
                    Spalte { Hinweis(info.outputData.getString("fehler").orEmpty(), "OK") { app.sprachFrage.erledigt(info.id) } }
                }
                if (monat != null) {
                    item(key = "rueckblick-hinweis", contentType = "hinweis") {
                        Spalte {
                            Hinweis(
                                "Heuristische Auswahl aus den gespeicherten Meldungen (${Archiv.zeitraum(monat)}): je Thema die ${Archiv.TOP_JE_THEMA} Geschichten, " +
                                    "die an den meisten Tagen, am weitesten oben und mit den meisten Quellen berichtet wurden. Gleiche Geschichten sind zusammengefasst, es wurde nichts neu recherchiert.",
                            )
                        }
                    }
                }
                if (archivHinweise.isNotEmpty()) {
                    item(key = "archiv-hinweise", contentType = "hinweise") {
                        Spalte { Column { archivHinweise.forEach { Hinweis(it) } } }
                    }
                }
                if (schadenImMonat > 0) {
                    item(key = "schaden", contentType = "hinweis") {
                        Spalte {
                            Hinweis(
                                if (schadenImMonat == 1) {
                                    "Eine gespeicherte Ausgabe dieses Monats ließ sich nicht lesen und fehlt im Rückblick. Die Datei bleibt unverändert erhalten."
                                } else {
                                    "$schadenImMonat gespeicherte Ausgaben dieses Monats ließen sich nicht lesen und fehlen im Rückblick. Die Dateien bleiben unverändert erhalten."
                                },
                            )
                        }
                    }
                } else if (schadenAmTag > 0) {
                    item(key = "schaden", contentType = "hinweis") {
                        Spalte {
                            Hinweis(
                                if (schadenAmTag == 1) {
                                    "Eine Ausgabe dieses Tages ließ sich nicht lesen. Die Datei bleibt unverändert erhalten, es wurde nichts gelöscht."
                                } else {
                                    "$schadenAmTag Ausgaben dieses Tages ließen sich nicht lesen. Die Dateien bleiben unverändert erhalten, es wurde nichts gelöscht."
                                },
                            )
                        }
                    }
                }
                if (ladeFehler) {
                    item(key = "ladefehler", contentType = "hinweis") {
                        Spalte { Hinweis("Diese Ausgabe ließ sich nicht laden. Die Datei bleibt unverändert erhalten.") }
                    }
                }
                if (laedt) {
                    item(key = "laedt", contentType = "laden") {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                }
                if (index.isEmpty()) {
                    item(key = "leer", contentType = "leer") {
                        Spalte {
                            LeerZustand(
                                angemeldet = app.codex.istVerbunden,
                                laeuft = lauf?.state == WorkInfo.State.RUNNING,
                                laden = { Zeitplan.starteLauf(kontext, manuell = true) },
                                oeffneEinstellungen = oeffneEinstellungen,
                            )
                        }
                    }
                }
                bloecke.forEachIndexed { index, block ->
                    item(key = "b-${ausgabe?.id}-${block.themaId}", contentType = "blockkopf") {
                        Spalte {
                            BlockKopf(
                                index = index,
                                block = block,
                                zustand = vorlesen,
                                vorlesen = { app.vorleser.schalteUm("block-${block.themaId}", blockText(block)) },
                                entfernen = if (block.frage != null && ausgabe != null) {
                                    {
                                        if (vorlesen.quelleId == "block-${block.themaId}" || block.meldungen.any { it.id == vorlesen.quelleId }) app.vorleser.stoppe()
                                        app.bereich.launch { app.speicher.entferneBlock(ausgabe.id, block.themaId) }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                    block.fehler?.let { fehler ->
                        item(key = "f-${ausgabe?.id}-${block.themaId}", contentType = "hinweis") { Spalte { Hinweis(fehler) } }
                    }
                    items(block.meldungen, key = { it.id }, contentType = { "meldung" }) { meldung ->
                        Spalte {
                            MeldungsKarte(
                                meldung = meldung,
                                blockIndex = index,
                                bild = meldung.bildDatei?.let(bildDateien::get),
                                zustand = vorlesen,
                                vorlesen = { app.vorleser.schalteUm(meldung.id, meldung.vorleseText) },
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = vorlesen.fehler.isNotBlank(),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(start = 16.dp, end = 96.dp, top = 16.dp, bottom = 16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 8.dp,
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            vorlesen.fehler,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false).widthIn(max = 520.dp),
                        )
                        TextButton(onClick = app.vorleser::loescheFehler) { Text("OK") }
                    }
                }
            }

            Column(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.End,
            ) {
                AnimatedVisibility(visible = sprache.meldung.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(bottom = 12.dp).widthIn(max = 360.dp),
                    ) {
                        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sprache.meldung,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (sprache.zuEinstellungen) {
                                TextButton(onClick = {
                                    app.sprachFrage.loescheMeldung()
                                    oeffneEinstellungen()
                                }) { Text("Einstellungen") }
                            } else {
                                TextButton(onClick = app.sprachFrage::loescheMeldung) { Text("OK") }
                            }
                        }
                    }
                }
                MikrofonKnopf(
                    stufe = sprache.stufe,
                    tippe = {
                        val erlaubt = ContextCompat.checkSelfPermission(kontext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        app.sprachFrage.tippe(erlaubt) { mikrofonErlaubnis.launch(Manifest.permission.RECORD_AUDIO) }
                    },
                )
            }
        }
    }
}

/**
 * Der schwebende Mikrofon-Knopf unten rechts: tippen, Frage sprechen, noch einmal tippen.
 * Während der Aufnahme pulsiert er rot, während Whisper zuhört, dreht sich ein Kreis.
 */
@Composable
private fun MikrofonKnopf(stufe: SprachStufe, tippe: () -> Unit) {
    val flaeche = when (stufe) {
        SprachStufe.NIMMT_AUF -> Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
        else -> Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = stufe != SprachStufe.BEREIT) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(end = 12.dp),
            ) {
                Text(
                    if (stufe == SprachStufe.NIMMT_AUF) "Ich höre zu … tippen zum Senden" else "Ich verstehe …",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Surface(
            onClick = tippe,
            enabled = stufe != SprachStufe.VERSTEHT,
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 10.dp,
            modifier = Modifier.size(64.dp).pulsWenn(stufe == SprachStufe.NIMMT_AUF, bis = 1.08f, dauerMs = 700),
        ) {
            Box(Modifier.fillMaxSize().background(flaeche), contentAlignment = Alignment.Center) {
                when (stufe) {
                    SprachStufe.BEREIT -> Icon(Icons.Rounded.Mic, "Frage einsprechen", tint = Color.White, modifier = Modifier.size(30.dp))
                    SprachStufe.NIMMT_AUF -> Icon(Icons.Rounded.Stop, "Aufnahme beenden und Frage senden", tint = Color.White, modifier = Modifier.size(30.dp))
                    SprachStufe.VERSTEHT -> CircularProgressIndicator(Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
                }
            }
        }
    }
}

@Composable
private fun FrageLaeuft(frage: String, text: String, anteil: Float, laeuft: Boolean, verwerfen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Deine Frage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (frage.isNotBlank()) {
                        Text(
                            "„$frage“",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = verwerfen) { Icon(Icons.Rounded.Close, "Frage verwerfen", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(end = 12.dp))
            Spacer(Modifier.height(10.dp))
            val balken = Modifier.fillMaxWidth().padding(end = 12.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
            if (laeuft) {
                LinearProgressIndicator(progress = { anteil.coerceIn(0.03f, 1f) }, modifier = balken)
            } else {
                LinearProgressIndicator(modifier = balken)
            }
        }
    }
}


/** Auf dem aufgeklappten Fold bleibt der Text in angenehmer Zeilenlänge. */
@Composable
private fun Spalte(inhalt: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 16.dp)) { inhalt() }
    }
}

private fun blockText(block: Block): String =
    (listOf("${block.titel}.") + block.meldungen.map { it.vorleseText }).joinToString("\n\n")

@Composable
private fun Kopf(
    ausgabe: Ausgabe?,
    /** Große Zeile und Datumszeile — im Archiv der gewählte Tag oder Monat statt Gruß und heute. */
    titel: String,
    untertitel: String,
    dunkel: Boolean,
    laeuft: Boolean,
    oeffneArchiv: () -> Unit,
    aktualisiere: () -> Unit,
    wechsleDesign: () -> Unit,
    oeffneEinstellungen: () -> Unit,
) {
    val verlauf = Brush.linearGradient(
        if (dunkel) listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF831843))
        else listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777)),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(verlauf),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = oeffneArchiv) {
                    Icon(Icons.Rounded.CalendarMonth, "Archiv öffnen", tint = Color.White)
                }
                Text(
                    "NEWS KOMPASS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = aktualisiere, enabled = !laeuft) {
                    if (laeuft) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, "Jetzt aktualisieren", tint = Color.White)
                    }
                }
                IconButton(onClick = wechsleDesign) {
                    Icon(if (dunkel) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "Hell oder dunkel", tint = Color.White)
                }
                IconButton(onClick = oeffneEinstellungen) {
                    Icon(Icons.Rounded.Settings, "Einstellungen", tint = Color.White)
                }
            }
            Column(Modifier.padding(start = 14.dp)) {
                Spacer(Modifier.height(18.dp))
                Text(titel, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    untertitel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                if (ausgabe != null) {
                    Spacer(Modifier.height(14.dp))
                    val anzahl = ausgabe.bloecke.sumOf { it.meldungen.size }
                    val zeile = if (ausgabe.id.startsWith(Archiv.RUECKBLICK_PRAEFIX)) {
                        "${ausgabe.slot} · $anzahl Meldungen"
                    } else {
                        val zeit = SimpleDateFormat("EEEE, HH:mm", Locale.GERMANY).format(Date(ausgabe.erstelltUm))
                        "${ausgabe.slot} · $zeit Uhr · $anzahl Meldungen"
                    }
                    Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
                        Text(
                            zeile,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun gruss(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> "Guten Morgen"
    in 11..17 -> "Guten Tag"
    else -> "Guten Abend"
}

/** Was für die gewählte Ansicht geladen wurde; [ausgabe] null heißt: ließ sich nicht lesen. */
private data class Geladen(val id: String, val ausgabe: Ausgabe?, val hinweise: List<String>)

/** Oben im Archiv: was gerade gezeigt wird, und der Weg zurück zu Aktuell. */
@Composable
private fun ArchivHinweis(text: String, zuAktuell: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = zuAktuell) { Text("Zu Aktuell") }
        }
    }
}

/** Die Ausgaben des gezeigten Tages, etwa Morgen und Abend. */
@Composable
private fun AusgabenWahl(ausgaben: List<AusgabenEintrag>, gewaehlt: String?, waehle: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.widthIn(max = 792.dp).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ausgaben, key = { it.id }) { a ->
            FilterChip(
                selected = a.id == gewaehlt,
                onClick = { waehle(a.id) },
                label = { Text(ausgabeName(a)) },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

private fun ausgabeName(a: AusgabenEintrag): String {
    val teil = a.slot.removeSuffix("ausgabe").ifBlank { "Ausgabe" }
    return "$teil · " + SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(a.erstelltUm))
}

@Composable
private fun LaufStatus(lauf: WorkInfo?, angemeldet: Boolean, oeffneEinstellungen: () -> Unit) {
    when {
        !angemeldet -> Hinweis("Melde dich in den Einstellungen bei Codex an, damit die Nachrichten recherchiert werden können.", "Zur Anmeldung", oeffneEinstellungen)
        lauf?.state == WorkInfo.State.RUNNING -> {
            val text = lauf.progress.getString("text") ?: "Die Nachrichten werden zusammengestellt …"
            val anteil = lauf.progress.getFloat("anteil", 0f)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { anteil.coerceIn(0.03f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
        lauf?.state == WorkInfo.State.ENQUEUED && lauf.runAttemptCount > 0 ->
            Hinweis("Der letzte Versuch hat nicht geklappt. Es wird gleich automatisch erneut versucht.")
        lauf?.state == WorkInfo.State.FAILED -> {
            val fehler = lauf.outputData.getString("fehler") ?: "Der letzte Lauf ist gescheitert."
            Hinweis(fehler)
        }
    }
}

@Composable
private fun Hinweis(text: String, knopf: String? = null, aktion: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (knopf != null) TextButton(onClick = aktion) { Text(knopf) }
        }
    }
}

@Composable
private fun LeerZustand(angemeldet: Boolean, laeuft: Boolean, laden: () -> Unit, oeffneEinstellungen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(blockVerlauf(0)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Noch keine Ausgabe", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Jeden Tag um 5 und um 17 Uhr stellt News Kompass die Neuigkeiten zu deinen Themen zusammen — mit Bildern und zum Vorlesen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(22.dp))
        if (angemeldet) {
            Button(onClick = laden, enabled = !laeuft, shape = RoundedCornerShape(50)) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (laeuft) "Wird geladen …" else "Jetzt Nachrichten laden")
            }
        } else {
            Button(onClick = oeffneEinstellungen, shape = RoundedCornerShape(50)) { Text("Bei Codex anmelden") }
        }
    }
}

/**
 * Kopf eines Blocks. Ein Frage-Block trägt statt der Nummer ein Mikrofon, darunter steht die
 * Frage so, wie Whisper sie verstanden hat, und er lässt sich wieder entfernen.
 */
@Composable
private fun BlockKopf(index: Int, block: Block, zustand: VorleseZustand, vorlesen: () -> Unit, entfernen: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(blockVerlauf(index)),
                contentAlignment = Alignment.Center,
            ) {
                if (block.frage != null) {
                    Icon(Icons.Rounded.Mic, "Deine Frage", tint = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(block.titel, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (block.frage != null) "Deine Frage · ${block.meldungen.size} Meldungen" else "${block.meldungen.size} Meldungen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (block.meldungen.isNotEmpty()) {
                LautsprecherKnopf(zustand, "block-${block.themaId}", blockFarbe(index), vorlesen, rahmen = true)
            }
        }
        if (block.frage != null) {
            Row(Modifier.fillMaxWidth().padding(start = 50.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "„${block.frage}“",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (entfernen != null) {
                    TextButton(onClick = entfernen) { Text("Entfernen", color = blockFarbe(index)) }
                }
            }
        }
    }
}

@Composable
private fun MeldungsKarte(
    meldung: Meldung,
    blockIndex: Int,
    bild: java.io.File?,
    zustand: VorleseZustand,
    vorlesen: () -> Unit,
) {
    val kontext = LocalContext.current
    var offen by rememberSaveable(meldung.id) { mutableStateOf(false) }
    var grossesBild by rememberSaveable(meldung.id) { mutableStateOf(false) }
    if (grossesBild && bild != null) {
        BildAnsicht(
            bild = bild,
            titel = meldung.titel,
            istKi = meldung.bildIstKi,
            schliessen = { grossesBild = false },
            teilen = { Teilen.mitBild(kontext, meldung, bild) },
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        val antippbar = if (bild != null) Modifier.clickable(onClickLabel = "Bild vergrößern") { grossesBild = true } else Modifier
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).then(antippbar)) {
            if (bild != null) {
                AsyncImage(
                    model = bild,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(blockVerlauf(blockIndex)),
                )
            } else {
                Box(Modifier.fillMaxSize().background(blockVerlauf(blockIndex)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Newspaper, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(72.dp))
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f)),
                ),
            )
            if (meldung.bildIstKi) {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("KI-Illustration", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TeilenKnopf(meldung, bild)
                LautsprecherKnopf(zustand, meldung.id, blockFarbe(blockIndex), vorlesen, rahmen = false)
            }
            Text(
                meldung.titel,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            )
        }
        Column(Modifier.clickable { offen = !offen }.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
            if (meldung.istUpdate || meldung.wann.isNotBlank()) {
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (meldung.istUpdate) {
                        Surface(color = blockFarbe(blockIndex), shape = RoundedCornerShape(50)) {
                            Text(
                                "UPDATE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (meldung.wann.isNotBlank()) {
                        Text(
                            meldung.wann.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val sichtbar = if (offen) meldung.absaetze else meldung.absaetze.take(1)
            sichtbar.forEachIndexed { i, absatz ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                Text(
                    absatz,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (offen) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (offen && meldung.quellen.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(meldung.quellen.distinct().take(6)) { adresse ->
                        AssistChip(
                            onClick = {
                                runCatching { kontext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse))) }
                            },
                            label = { Text(domain(adresse), maxLines = 1) },
                            leadingIcon = { Icon(Icons.Rounded.Link, null, Modifier.size(AssistChipDefaults.IconSize)) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (offen) "Weniger" else "Weiterlesen",
                    style = MaterialTheme.typography.labelLarge,
                    color = blockFarbe(blockIndex),
                )
                Icon(if (offen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = blockFarbe(blockIndex))
            }
        }
    }
}

/**
 * Teilen oben rechts im Bild. Mit Bild fragt ein kleines Menü, ob Bild samt Kurztext oder der
 * ganze Beitrag als Text weitergegeben wird; ohne Bild geht es direkt als Text.
 */
@Composable
private fun TeilenKnopf(meldung: Meldung, bild: java.io.File?) {
    val kontext = LocalContext.current
    var menue by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { if (bild != null) menue = true else Teilen.alsText(kontext, meldung) },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.42f),
            modifier = Modifier.size(46.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Share, "Teilen", tint = Color.White) }
        }
        DropdownMenu(expanded = menue, onDismissRequest = { menue = false }) {
            DropdownMenuItem(
                text = { Text("Mit Bild teilen") },
                leadingIcon = { Icon(Icons.Rounded.Image, null) },
                onClick = {
                    menue = false
                    if (bild != null) Teilen.mitBild(kontext, meldung, bild)
                },
            )
            DropdownMenuItem(
                text = { Text("Text teilen") },
                leadingIcon = { Icon(Icons.Rounded.ShortText, null) },
                onClick = {
                    menue = false
                    Teilen.alsText(kontext, meldung)
                },
            )
        }
    }
}

private fun domain(adresse: String): String =
    runCatching { Uri.parse(adresse).host.orEmpty().removePrefix("www.") }.getOrDefault(adresse).ifBlank { adresse }

/**
 * Pulsierendes Vergrößern, aber nur solange [aktiv] ist — sonst läuft keine Dauer-Animation. Der Wert
 * wird erst beim Zeichnen gelesen, damit nicht jede Karte in jedem Frame neu zusammengesetzt wird.
 */
@Composable
private fun Modifier.pulsWenn(aktiv: Boolean, bis: Float, dauerMs: Int): Modifier {
    if (!aktiv) return this
    val puls = rememberInfiniteTransition(label = "puls")
    val skala = puls.animateFloat(1f, bis, infiniteRepeatable(tween(dauerMs), RepeatMode.Reverse), label = "skala")
    return graphicsLayer {
        scaleX = skala.value
        scaleY = skala.value
    }
}

@Composable
private fun LautsprecherKnopf(zustand: VorleseZustand, quelle: String, farbe: Color, aktion: () -> Unit, rahmen: Boolean) {
    val meins = zustand.quelleId == quelle
    val stufe = if (meins) zustand.stufe else VorleseStufe.AUS
    val hintergrund = when {
        stufe != VorleseStufe.AUS -> farbe
        rahmen -> farbe.copy(alpha = 0.14f)
        else -> Color.Black.copy(alpha = 0.42f)
    }
    Surface(
        onClick = aktion,
        shape = CircleShape,
        color = hintergrund,
        modifier = Modifier.size(46.dp).pulsWenn(stufe == VorleseStufe.SPRICHT, bis = 1.12f, dauerMs = 650),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (stufe) {
                VorleseStufe.LAEDT -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                VorleseStufe.SPRICHT -> Icon(Icons.Rounded.GraphicEq, "Vorlesen anhalten", tint = Color.White)
                VorleseStufe.AUS -> Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    "Vorlesen",
                    tint = if (rahmen) farbe else Color.White,
                )
            }
        }
    }
    if (meins && stufe == VorleseStufe.SPRICHT && zustand.absatzAnzahl > 1 && rahmen) {
        Text(
            " ${zustand.absatzNummer}/${zustand.absatzAnzahl}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = farbe,
        )
    }
}
