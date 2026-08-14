package de.frank.stacklabor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.frank.stacklabor.StackLaborApp
import de.frank.stacklabor.logik.Ampel
import de.frank.stacklabor.logik.Laufzustand
import de.frank.stacklabor.ui.bausteine.AmpelBalken
import de.frank.stacklabor.ui.bausteine.AuswertungsAuszug
import de.frank.stacklabor.ui.bausteine.HinweisSchnipsel
import de.frank.stacklabor.ui.bausteine.MittelKontextmenue
import de.frank.stacklabor.ui.bausteine.RueckgaengigLeiste
import de.frank.stacklabor.ui.blaetter.AufschluesselungsBlatt
import de.frank.stacklabor.ui.blaetter.EigeneFragenBlatt
import de.frank.stacklabor.ui.blaetter.HistorieBlatt
import de.frank.stacklabor.ui.blaetter.MittelBearbeitenBlatt
import de.frank.stacklabor.ui.blaetter.Richtung
import de.frank.stacklabor.ui.blaetter.StackBearbeitenBlatt
import de.frank.stacklabor.ui.bausteine.zeitpunktKurz
import de.frank.stacklabor.ui.bausteine.Chip
import de.frank.stacklabor.ui.bausteine.MittelEintrag
import de.frank.stacklabor.ui.bausteine.SymbolKnopf
import de.frank.stacklabor.ui.bausteine.VerlaufsKante
import de.frank.stacklabor.ui.bausteine.Zaehlpunkte
import de.frank.stacklabor.ui.bausteine.ampelFarbe
import de.frank.stacklabor.ui.bausteine.glasflaeche
import de.frank.stacklabor.ui.theme.Dauer
import de.frank.stacklabor.ui.theme.Farben
import de.frank.stacklabor.ui.theme.Formen
import de.frank.stacklabor.ui.theme.Kurven
import de.frank.stacklabor.ui.theme.LocalBewegungReduziert
import de.frank.stacklabor.ui.theme.Masse
import de.frank.stacklabor.ui.theme.Schrift
import kotlinx.coroutines.launch

/**
 * **B-02 — Stack-Detail.** Der Arbeitsplatz.
 *
 * Aufbau exakt aus `messung/hell/B-02.json`, Höhen zusammen genau 421 dp:
 * Kopfleiste 57 · Ziel-Streifen 41 · Sortier-/Suchleiste 37 · Liste 233 · Sockel 53.
 * Kopfleiste und Sockel sind Glasflächen mit 24 dp Weichzeichner — die einzigen Stellen,
 * an denen der Weichzeichner liegt, denn über einer scrollenden Liste würde er stocken.
 */
@Composable
fun StackBildschirm(app: StackLaborApp, steuerung: NavHostController, stackId: String) {
    val bereich = rememberCoroutineScope()
    val stack by app.repository.alleStacks
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val zeilen by app.repository.mittelZeilen(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val zielZeilen by app.repository.zielZeilen(stackId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val ansicht by app.einstellungen.sortieransicht.collectAsStateWithLifecycle(initialValue = "LOESLICHKEIT")
    val angemeldet by app.codexAnmeldung.istAngemeldet.collectAsStateWithLifecycle(initialValue = false)
    val laufZustaende by app.auswertung.zustand.collectAsStateWithLifecycle(initialValue = emptyMap())
    val lauf = laufZustaende[stackId] ?: Laufzustand.Ruhe
    val laeuft = lauf is Laufzustand.Laeuft

    var zieleOffen by remember { mutableStateOf(false) }
    var sucheOffen by remember { mutableStateOf(false) }
    var kontextFuer by remember { mutableStateOf<String?>(null) }
    var zuletztEntfernt by remember { mutableStateOf<String?>(null) }
    var bearbeiteEintrag by remember { mutableStateOf<String?>(null) }
    var menueOffen by remember { mutableStateOf(false) }
    var fragenOffen by remember { mutableStateOf(false) }
    var historieOffen by remember { mutableStateOf(false) }
    var stackBearbeiten by remember { mutableStateOf(false) }
    var aufschluesselung by remember { mutableStateOf<Pair<Richtung, String>?>(null) }

    val derStack = stack.firstOrNull { it.id == stackId }
    val obenInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val untenInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Höchstens drei Auren gleichzeitig (M-21) — die ranghöchsten roten.
    val mitAura = zeilen.filter { it.ampel == Ampel.ROT && it.eintrag.aktiv }
        .take(3).map { it.eintrag.id }.toSet()

    val bewertung by app.datenbank.bewertungDao().juengsteBewertung(stackId)
        .collectAsStateWithLifecycle(initialValue = null)
    var veraltet by remember { mutableStateOf(false) }
    // F-23: Die gespeicherte Prüfsumme gegen den jetzigen Inhalt halten. Häkchen und
    // Priorität gehen bewusst NICHT ein — sonst wäre nach jedem Probieren alles „veraltet".
    LaunchedEffect(bewertung, zeilen, zielZeilen) {
        val b = bewertung
        veraltet = b != null && b.pruefsumme != app.repository.pruefsummeFuer(stackId)
    }

    val schlechtesteZielAmpel = zielZeilen.maxByOrNull { it.ampel.gewicht }?.ampel ?: Ampel.GRAU
    val zaehlung = zielZeilen.groupingBy { it.ampel }.eachCount()

    Box(Modifier.fillMaxSize().background(Farben.grund)) {
        Column(Modifier.fillMaxSize()) {

            // ── Kopfleiste 57 dp, Glasfläche ────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .glasflaeche()
                    .padding(top = obenInset)
                    .height(Masse.kopfleiste)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SymbolKnopf(Icons.Rounded.ArrowBack, "Zurück") { steuerung.popBackStack() }
                Column(Modifier.weight(1f)) {
                    Text(
                        derStack?.name.orEmpty(),
                        style = Schrift.kopfzeile,
                        color = Farben.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(derStack?.zeitpunkt, derStack?.einnahmeHinweis)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        style = Schrift.klein,
                        color = Farben.textSchwach,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SymbolKnopf(Icons.Rounded.MoreVert, "Mehr") { menueOffen = true }
            }

            // ── Ziel-Streifen 41 dp ─────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Masse.zielStreifen)
                    .background(Farben.flaeche)
                    .clickable { zieleOffen = !zieleOffen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(Masse.ampelBalken)
                        .height(Masse.zielStreifen)
                        .background(ampelFarbe(schlechtesteZielAmpel), Formen.ampelBalken),
                )
                Spacer(Modifier.width(10.dp))
                Text("Ziele ${zielZeilen.size}", style = Schrift.name, color = Farben.text)
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Zaehlpunkte(
                        zaehlung[Ampel.GRUEN] ?: 0, zaehlung[Ampel.GELB] ?: 0,
                        zaehlung[Ampel.ROT] ?: 0, zaehlung[Ampel.GRAU] ?: 0,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = if (zieleOffen) "Ziele zuklappen" else "Ziele aufklappen",
                    tint = Farben.textSchwach,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (zieleOffen) 180f else 0f),
                )
                Spacer(Modifier.width(12.dp))
            }

            // ── Sortier- und Suchleiste 37 dp ───────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Masse.sortierleiste)
                    .background(Farben.erhoeht)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Chip("Löslichkeit", ansicht == "LOESLICHKEIT") {
                    bereich.launch { app.einstellungen.setzeSortieransicht("LOESLICHKEIT") }
                }
                Chip("Einnahme", ansicht == "EINNAHME") {
                    bereich.launch { app.einstellungen.setzeSortieransicht("EINNAHME") }
                }
                Spacer(Modifier.weight(1f))
                SymbolKnopf(Icons.Rounded.Search, "Suchen", groesse = 20.dp) { sucheOffen = !sucheOffen }
            }

            // ── Liste + Ziel-Überlagerung ───────────────────────────────────────
            Box(Modifier.fillMaxWidth().weight(1f)) {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Masse.randSeitlich, end = Masse.randSeitlich, bottom = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (zeilen.isEmpty()) {
                        item {
                            Text(
                                "Noch kein Mittel in diesem Stack.",
                                style = Schrift.grund,
                                color = Farben.textSchwach,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                    items(zeilen, key = { it.eintrag.id }) { zeile ->
                        MittelEintrag(
                            zeile = zeile,
                            mitAura = zeile.eintrag.id in mitAura,
                            aufHaekchen = {
                                bereich.launch {
                                    app.repository.setzeAktiv(zeile.eintrag.id, !zeile.eintrag.aktiv)
                                }
                            },
                            aufKlick = { bearbeiteEintrag = zeile.eintrag.id },
                            aufAmpel = {
                                aufschluesselung = Richtung.MITTEL_ZU_ZIELEN to zeile.eintrag.mittelId
                            },
                            // In der Ansicht „Löslichkeit" kann nicht gezogen werden — dort
                            // öffnet langes Drücken das Kontextmenü (F-31), damit kein toter
                            // Zustand entsteht. In „Einnahme" nimmt es den Eintrag auf (F-07).
                            aufLangesDruecken = {
                                if (ansicht == "LOESLICHKEIT") kontextFuer = zeile.eintrag.id
                            },
                        )
                    }
                }

                // Die Auswertungs-Karte: dreizeiliger Auszug, tippen führt ins Vollbild B-07.
                if (bewertung != null || laeuft) {
                    AuswertungsAuszug(
                        text = when {
                            laeuft -> (lauf as Laufzustand.Laeuft).text
                            else -> bewertung?.gesamt.orEmpty()
                        },
                        laeuft = laeuft,
                        veraltet = veraltet,
                        zeitstempel = bewertung?.let { zeitpunktKurz(it.zeitpunkt) }.orEmpty(),
                        modell = bewertung?.modell.orEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = Masse.randSeitlich, vertical = 8.dp),
                        aufKlick = { steuerung.navigate(Ziele.auswertung(stackId)) },
                    )
                }

                // Die Ziel-Überlagerung legt sich ÜBER die Liste — sie verdrängt sie nicht.
                // Würde sie verdrängen, blieben 77 dp und damit 1,3 Mittel übrig.
                Column(Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = zieleOffen,
                    enter = expandVertically(
                        tween(if (LocalBewegungReduziert.current) 0 else Dauer.AUFKLAPPEN, easing = Kurven.leit),
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        tween(if (LocalBewegungReduziert.current) 0 else Dauer.AUFKLAPPEN, easing = Kurven.leit),
                    ) + fadeOut(),
                ) {
                    ZielUeberlagerung(
                        zeilen = zielZeilen,
                        aufOrdnen = { steuerung.navigate(Ziele.zieleOrdnen(stackId)) },
                        aufFertig = { zieleOffen = false },
                    )
                }
                }
            }

            // ── Auswerten-Sockel 53 dp, fest, Glasfläche ────────────────────────
            Column(Modifier.fillMaxWidth()) {
                VerlaufsKante()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glasflaeche()
                        .height(Masse.sockel)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Der Knopf trägt alle Zustände aus dem Spec: bereit · läuft (dann ist er
                    // ein Abbruch-Knopf) · veraltet · nicht angemeldet · Fehler.
                    val knopfFarbe = when {
                        laeuft -> Farben.gelb
                        !angemeldet -> Farben.grau
                        lauf is Laufzustand.Fehler -> Farben.rot
                        veraltet -> Farben.gelbText
                        else -> Farben.akzent
                    }
                    val knopfText = when {
                        laeuft -> "Auswertung läuft — Abbrechen"
                        !angemeldet -> "Erst bei Codex anmelden"
                        lauf is Laufzustand.Fehler -> (lauf as Laufzustand.Fehler).meldung + " Erneut?"
                        veraltet -> "Stand veraltet — neu auswerten"
                        else -> "Diesen Stack auswerten"
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(Formen.knopf)
                            .background(knopfFarbe)
                            .clickable {
                                if (!angemeldet) steuerung.navigate(Ziele.ANMELDUNG)
                                else app.auswertung.werteStackAus(stackId)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (Farben.istHell) Color.White else Farben.grund,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            knopfText,
                            style = Schrift.grundMittel,
                            color = if (Farben.istHell) Color.White else Farben.grund,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(Formen.knopf)
                            .background(Farben.erhoeht)
                            // F-02: Der Weg zum Katalog. Das gewählte Mittel steht danach
                            // sofort in der Liste; die Konkurrenzprüfung läuft nach.
                            .clickable { steuerung.navigate(Ziele.KATALOG) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Mittel hinzufügen",
                            tint = Farben.akzent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(untenInset))
            }
        }

        // F-31 — Kontextmenü am Mittel (nur in der Ansicht „Löslichkeit")
        val kontextZeile = zeilen.firstOrNull { it.eintrag.id == kontextFuer }
        MittelKontextmenue(
            sichtbar = kontextZeile != null,
            mittelName = kontextZeile?.mittel?.name.orEmpty(),
            aufBearbeiten = { bearbeiteEintrag = kontextFuer; kontextFuer = null },
            aufGruppeBilden = { kontextFuer = null },
            aufEntfernen = {
                val id = kontextFuer
                kontextFuer = null
                if (id != null) {
                    bereich.launch { app.repository.entferneEintrag(id) }
                    zuletztEntfernt = id
                }
            },
            aufSchliessen = { kontextFuer = null },
        )

        // F-02 — der Hinweis nach der Konkurrenzprüfung, über dem Sockel
        val mitHinweis = zeilen.firstOrNull { it.eintrag.offenerHinweis.isNotBlank() }
        HinweisSchnipsel(
            sichtbar = mitHinweis != null,
            mittelName = mitHinweis?.mittel?.name.orEmpty(),
            text = mitHinweis?.eintrag?.offenerHinweis.orEmpty(),
            aufBehalten = {
                mitHinweis?.let { bereich.launch { app.datenbank.stackDao().setzeOffenenHinweis(it.eintrag.id, "") } }
            },
            aufEntfernen = {
                mitHinweis?.let { bereich.launch { app.repository.entferneEintrag(it.eintrag.id) } }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Masse.randSeitlich)
                .padding(bottom = Masse.sockel + 8.dp + untenInset),
        )

        // M-24 — die Rückgängig-Leiste, sechs Sekunden lang
        RueckgaengigLeiste(
            sichtbar = zuletztEntfernt != null,
            aufRueckgaengig = { zuletztEntfernt = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Masse.randSeitlich)
                .padding(bottom = Masse.sockel + 8.dp + untenInset),
        )
        // ── Die Blätter ─────────────────────────────────────────────────────────
        bearbeiteEintrag?.let { id ->
            MittelBearbeitenBlatt(app, id) { bearbeiteEintrag = null }
        }
        if (fragenOffen) EigeneFragenBlatt(app, stackId) { fragenOffen = false }
        if (historieOffen) {
            HistorieBlatt(
                app = app, stackId = stackId,
                aufOeffnen = { historieOffen = false; steuerung.navigate(Ziele.auswertung(stackId)) },
                aufSchliessen = { historieOffen = false },
            )
        }
        if (stackBearbeiten) StackBearbeitenBlatt(app, stackId) { stackBearbeiten = false }
        aufschluesselung?.let { (richtung, id) ->
            AufschluesselungsBlatt(app, stackId, richtung, id) { aufschluesselung = null }
        }

        // Das Überlaufmenü des Kopfes
        MittelKontextmenue(
            sichtbar = menueOffen,
            mittelName = "Stack",
            aufBearbeiten = { menueOffen = false; stackBearbeiten = true },
            aufGruppeBilden = { menueOffen = false; fragenOffen = true },
            aufEntfernen = { menueOffen = false; historieOffen = true },
            aufSchliessen = { menueOffen = false },
        )

        LaunchedEffect(zuletztEntfernt) {
            if (zuletztEntfernt != null) {
                kotlinx.coroutines.delay(Dauer.RUECKGAENGIG_MS)
                zuletztEntfernt = null
            }
        }
    }
}

/**
 * **B-04 — Ziele dieses Stacks**, als Überlagerung über der Liste (höchstens 281 dp).
 *
 * Tippen auf eine Zeile klappt die Begründung als **eigene Zeile** auf — keine Sprechblase:
 * die würde die Nachbarn verdecken und mit dem Ziehen kollidieren.
 */
@Composable
private fun ZielUeberlagerung(
    zeilen: List<de.frank.stacklabor.daten.StackRepository.ZielZeile>,
    aufOrdnen: () -> Unit,
    aufFertig: () -> Unit,
) {
    var aufgeklappt by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = Masse.zielUeberlagerungMax)
            .background(Farben.flaeche),
    ) {
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(zeilen, key = { it.ziel.id }) { z ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            aufgeklappt = if (aufgeklappt == z.ziel.id) null else z.ziel.id
                        },
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(Masse.zielEintrag),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AmpelBalken(z.ampel)
                        Spacer(Modifier.width(10.dp))
                        NummernKreis(z.rang)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            z.ziel.text,
                            style = Schrift.grundMittel,
                            color = Farben.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    // Die Begründung bei Rot oder Gelb — eigene Zeile, klappt auf.
                    AnimatedVisibility(visible = aufgeklappt == z.ziel.id && z.begruendung.isNotBlank()) {
                        Text(
                            z.begruendung,
                            style = Schrift.klein,
                            color = Farben.textSchwach,
                            modifier = Modifier.padding(start = 41.dp, end = 12.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip("Ordnen", false) { aufOrdnen() }
            Spacer(Modifier.weight(1f))
            Chip("Fertig", true) { aufFertig() }
        }
    }
}

/** Der Nummernkreis am Ziel: 20 dp, Rand 1 dp, Zahl 11 sp. */
@Composable
fun NummernKreis(nummer: Int) {
    Box(
        Modifier
            .size(Masse.nummernKreis)
            .clip(CircleShape)
            .background(Farben.flaeche)
            .border(1.dp, Farben.rand, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(nummer.toString(), style = Schrift.winzigFett, color = Farben.textSchwach)
    }
}
